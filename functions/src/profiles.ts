/**
 * Identity and profiles.
 *
 *   completeRegistration     after OTP: phoneRoles/{+91..} {uid, role} + the role profile + wallet
 *                            (+ the referral the user typed). One phone = one role, forever.
 *   lookupPhoneRole          pre-OTP check (one document read); never returns names or uids
 *   onEmployerProfileWritten keeps employer_cards/{uid} (the public name/photo/rating) in sync
 *   deleteAccount            removes the user's personal data and sign-in; money records are kept
 *
 * Worker profiles are handled in workers.ts (card sync). Referrals complete on the first
 * application / job post, not on profile completion.
 * There is no users/{uid} collection: role = phoneRoles, identity = the role profile,
 * push = user_tokens, money = referral_stats.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, oneOf } from "./lib/input";
import { getReferralConfig } from "./app-config";
import { ensureWallet, registerReferral } from "./referrals";
import {
  Applications, EmployerCards, EmployerProfiles, Jobs, OtpCodes, PhoneRoles, ReferralCodes, SavedJobs, TruecallerProfiles, UserTokens, Values,
  Wallets, WorkerProfiles,
} from "./schema";

import { markFieldLeadJoined } from "./field-leads";

const db = admin.firestore();
const { Timestamp } = admin.firestore;
const ROLES = [Values.Role.WORKER, Values.Role.EMPLOYER] as const;

function e164(raw: unknown): string | null {
  const digits = String(raw ?? "").replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(digits)) return `+91${digits}`;
  if (/^91[6-9]\d{9}$/.test(digits)) return `+${digits}`;
  return null;
}

export const completeRegistration = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 25 },
  async (raw: unknown, context) => {
    const data = obj(raw);
    const uid = context.auth?.uid || (typeof data.uid === "string" && data.uid.trim() ? data.uid.trim() : null);
    if (!uid) fail("unauthenticated", "Sign in with your mobile number first");
    if (!context.auth) {
      const verifiedUser = await admin.auth().getUser(uid).catch(() => null);
      if (!verifiedUser) fail("unauthenticated", "User account not recognized");
    }

    let phone = e164(data.phone) || e164(context.auth?.token?.phone_number);
    if (!phone) {
      const userRec = await admin.auth().getUser(uid).catch(() => null);
      phone = e164(userRec?.phoneNumber);
    }
  if (!phone) {
    const tcDoc = await db.collection(TruecallerProfiles.COLLECTION).doc(uid).get();
    if (tcDoc.exists) phone = e164(tcDoc.get(TruecallerProfiles.PHONE));
  }
  if (!phone) fail("failed-precondition", "Sign in with your mobile number first");
  const role = oneOf(data, "role", ROLES);
  const name = str(data, "name", { min: 2, max: 80 });
  const referralCode = str(data, "referralCode", { max: 20, optional: true });
  // Employers choose at registration whether the name is a person or a business.
  const employerType = data.employerType === Values.EmployerType.COMPANY ?
    Values.EmployerType.COMPANY : Values.EmployerType.INDIVIDUAL;

  const phoneRef = db.collection(PhoneRoles.COLLECTION).doc(phone);
  const profileRef = db.collection(role === Values.Role.WORKER ? WorkerProfiles.COLLECTION : EmployerProfiles.COLLECTION).doc(uid);
  const config = role === Values.Role.EMPLOYER ? await getReferralConfig() : null;

  const created = await db.runTransaction(async (tx) => {
    const [phoneDoc, profile, truecaller] = await Promise.all([
      tx.get(phoneRef), tx.get(profileRef), tx.get(db.collection(TruecallerProfiles.COLLECTION).doc(uid)),
    ]);
    // Email shared through Truecaller one-tap sign-up (same number only).
    const tcEmail = truecaller.get(TruecallerProfiles.PHONE) === phone ? String(truecaller.get(TruecallerProfiles.EMAIL) || "") : "";
    if (phoneDoc.exists) {
      if (phoneDoc.get(PhoneRoles.UID) !== uid) fail("already-exists", "This number is linked to another account");
      if (phoneDoc.get(PhoneRoles.ROLE) !== role) {
        fail("failed-precondition", `phone-already-registered-as:${phoneDoc.get(PhoneRoles.ROLE)}`);
      }
    } else {
      tx.create(phoneRef, { [PhoneRoles.UID]: uid, [PhoneRoles.ROLE]: role });
    }
    if (profile.exists) return false;

    const now = Timestamp.now();
    if (role === Values.Role.WORKER) {
      tx.create(profileRef, {
        [WorkerProfiles.NAME]: name,
        [WorkerProfiles.PHONE]: phone,
        ...(tcEmail ? { [WorkerProfiles.EMAIL]: tcEmail } : {}),
        [WorkerProfiles.SKILLS]: [],
        [WorkerProfiles.AVAILABLE]: false,
        [WorkerProfiles.BLOCKED]: false,
        [WorkerProfiles.CREATED_AT]: now,
        [WorkerProfiles.UPDATED_AT]: now,
      });
    } else {
      const S = EmployerProfiles.Subscription;
      const campaign = config?.employerUnlimitedJobPostingEnabled === true;
      tx.create(profileRef, {
        [EmployerProfiles.EMPLOYER_TYPE]: employerType,
        ...(employerType === Values.EmployerType.COMPANY ?
          { [EmployerProfiles.BUSINESS_NAME]: name, [EmployerProfiles.OWNER_NAME]: "" } :
          { [EmployerProfiles.OWNER_NAME]: name }),
        [EmployerProfiles.PHONE]: phone,
        ...(tcEmail ? { [EmployerProfiles.EMAIL]: tcEmail } : {}),
        [EmployerProfiles.SUBSCRIPTION]: {
          [S.PLAN_ID]: campaign ? "UNLIMITED_CAMPAIGN" : "",
          [S.STATUS]: campaign ? "ACTIVE" : "NONE",
          ...(campaign ? { [S.START_AT]: now } : {}),
          [S.CREDITS]: { [S.CREDITS_NORMAL]: 0, [S.CREDITS_INSTANT]: 0 },
        },
        [EmployerProfiles.FREE_URGENT_POSTS_USED]: 0,
        [EmployerProfiles.VERIFIED]: false,
        [EmployerProfiles.TOTAL_HIRES]: 0,
        [EmployerProfiles.BLOCKED]: false,
        [EmployerProfiles.CREATED_AT]: now,
        [EmployerProfiles.UPDATED_AT]: now,
      });
    }
    return true;
  });

  const customClaims: Record<string, any> = { phone_number: phone, role };
  if (context.auth?.token?.admin) {
    customClaims.admin = true;
  }
  await admin.auth().setCustomUserClaims(uid, customClaims);
  const code = await ensureWallet(uid, role);
  const referralError = created && referralCode ? await registerReferral(uid, referralCode) : null;
  // Registered earlier at a DutyPe help desk? Credit that field agent.
  if (created) await markFieldLeadJoined(phone, uid);
  return { role, created, referralCode: code, referralError };
});

/** Pre-OTP: does this number exist, and with which role? One read; rules keep phoneRoles private. */
export const lookupPhoneRole = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 10 },
  async (raw: unknown) => {
    const data = obj(raw);
    const phone = e164(data.phone);
    if (!phone) fail("invalid-argument", "Enter a valid 10-digit mobile number");
    const requested = String(data.requestedRole || "").toUpperCase();
    const doc = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
    if (!doc.exists) return { exists: false, roleConflict: false };
    const existingRole = String(doc.get(PhoneRoles.ROLE) || "");
    return {
      exists: true,
      existingRole,
      roleConflict: !!requested && requested !== existingRole,
    };
  },
);

export const onEmployerProfileWritten = functions
  .region("asia-south1")
  .firestore.document(`${EmployerProfiles.COLLECTION}/{uid}`)
  .onWrite(async (change, context) => {
    const cardRef = db.collection(EmployerCards.COLLECTION).doc(context.params.uid);
    const after = change.after.data();
    if (!after || after[EmployerProfiles.BLOCKED] === true) {
      await cardRef.delete();
      return;
    }
    const card = employerCard(after);
    const before = change.before.data();
    if (before && before[EmployerProfiles.BLOCKED] !== true &&
      JSON.stringify(employerCard(before)) === JSON.stringify(card)) return;
    await cardRef.set(card);
  });

/** What anyone may see of an employer: never the phone, GSTIN, address or subscription. */
function employerCard(p: admin.firestore.DocumentData): Record<string, unknown> {
  const business = String(p[EmployerProfiles.BUSINESS_NAME] || "").trim();
  const owner = String(p[EmployerProfiles.OWNER_NAME] || "").trim();
  const name = p[EmployerProfiles.EMPLOYER_TYPE] === Values.EmployerType.COMPANY && business ?
    business : (owner || business || "Employer");
  return {
    [EmployerCards.NAME]: name,
    [EmployerCards.PHOTO_URL]: String(p[EmployerProfiles.PHOTO_URL] || ""),
    [EmployerCards.AREA]: String(p[EmployerProfiles.AREA] || ""),
    [EmployerCards.VERIFIED]: p[EmployerProfiles.VERIFIED] === true,
    [EmployerCards.RATING]: Number(p[EmployerProfiles.RATING] || 0),
    [EmployerCards.RATING_COUNT]: Number(p[EmployerProfiles.RATING_COUNT] || 0),
  };
}

/**
 * Deletes the caller's account: role profile (the worker card follows via its trigger), phone
 * registration, push token and saved jobs; closes open jobs / withdraws open applications; then the
 * Firebase Auth user. The wallet ledger and withdrawals are kept (blocked) for accounting.
 */
export const deleteAccount = onCallSecured({}, async (_raw: unknown, context) => {
  const uid = context.auth!.uid;
  const phone = e164(context.auth!.token.phone_number);
  const role = String(context.auth!.token.role || "");
  const batchUpdate = async (query: admin.firestore.Query, update: (b: admin.firestore.WriteBatch, d: admin.firestore.QueryDocumentSnapshot) => void) => {
    const snap = await query.limit(500).get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((d) => update(batch, d));
    await batch.commit();
  };

  if (role === Values.Role.EMPLOYER) {
    await batchUpdate(
      db.collection(Jobs.COLLECTION).where(Jobs.EMPLOYER_ID, "==", uid).where(Jobs.STATUS, "==", Values.JobStatus.OPEN),
      (b, d) => b.update(d.ref, { [Jobs.STATUS]: Values.JobStatus.CLOSED }),
    );
  } else {
    await batchUpdate(
      db.collection(Applications.COLLECTION).where(Applications.WORKER_ID, "==", uid)
        .where(Applications.STATUS, "==", Values.ApplicationStatus.APPLIED),
      (b, d) => b.update(d.ref, { [Applications.STATUS]: Values.ApplicationStatus.WITHDRAWN }),
    );
  }
  await batchUpdate(db.collection(SavedJobs.COLLECTION).where(SavedJobs.USER_ID, "==", uid), (b, d) => b.delete(d.ref));

  const wallet = await db.collection(Wallets.COLLECTION).doc(uid).get();
  const code = String(wallet.get(Wallets.REFERRAL_CODE) || "");
  const batch = db.batch();
  batch.delete(db.collection(WorkerProfiles.COLLECTION).doc(uid));
  batch.delete(db.collection(EmployerProfiles.COLLECTION).doc(uid));
  batch.delete(db.collection(UserTokens.COLLECTION).doc(uid));
  batch.delete(db.collection(TruecallerProfiles.COLLECTION).doc(uid));
  if (wallet.exists) batch.update(wallet.ref, { [Wallets.BLOCKED]: true });
  if (code) batch.set(db.collection(ReferralCodes.COLLECTION).doc(code), { [ReferralCodes.ACTIVE]: false }, { merge: true });
  if (phone) {
    const phoneDoc = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
    if (phoneDoc.get(PhoneRoles.UID) === uid) batch.delete(phoneDoc.ref);
    batch.delete(db.collection(OtpCodes.COLLECTION).doc(phone));
  }
  await batch.commit();
  await admin.auth().deleteUser(uid);
  functions.logger.info(`account ${uid} deleted`);
  return { ok: true };
});
