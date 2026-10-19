/**
 * Referrals and the wallet — money works like a ledger.
 *
 *  • referral_stats/{uid}      the wallet: balance / lifetime / withdrawn in whole paise (server-only)
 *  • wallet_ledger/{eventId}   append-only entries; the id IS the event (referral_{uid}_referrer,
 *                              withdrawal_{id}, ...) so the same event can never be credited twice.
 *                              Every balance change writes its ledger entry in the same transaction.
 *  • referrals/{refereeUid}    one referral per new user (the id enforces it)
 *  • referral_codes/{CODE}     code → owner
 *
 *   registerReferral            at signup, with the code the new user typed (called by profiles.ts)
 *   completeReferral            when the new user's profile becomes complete → rewards
 *   ensureWallet / ensureWalletCallable   wallet + own code for every user
 *   requestWithdrawal           the worker/employer cashes out the full balance to UPI
 *   settleWithdrawal            admin marks a withdrawal paid or failed (failed = refund entry)
 *   getReferralLeaderboard      top referrers (first name + initial only)
 *   expirePendingReferrals      daily: pending referrals past expiresAt → EXPIRED
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { createHash, randomInt } from "crypto";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import { notify } from "./lib/notify";
import { getReferralConfig, isCallerAdmin } from "./app-config";
import { REFERRAL_POST_VALID_MS, referralPostsLeft } from "./lib/referral-posts";
import {
  EmployerProfiles, ReferralCodes, Referrals, Values, WalletLedger, Wallets, WithdrawalDaily, Withdrawals,
  WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
type Tx = admin.firestore.Transaction;

const DAY_MS = 24 * 60 * 60 * 1000;
const REFERRAL_EXPIRY_MS = 30 * DAY_MS;
const MAX_REFERRALS_PER_HOUR = 10;
const MAX_WITHDRAWALS_PER_DAY = 5;
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
/** Job-post credits an employer earns at these successful-referral counts. */
const EMPLOYER_CREDIT_MILESTONES: Record<number, number> = { 5: 5, 10: 10, 25: 25 };

export const walletRef = (uid: string) => db.collection(Wallets.COLLECTION).doc(uid);
export const ledgerRef = (eventId: string) => db.collection(WalletLedger.COLLECTION).doc(eventId);

// ───────────────────────────── ledger core ─────────────────────────────

export interface WalletState { ref: admin.firestore.DocumentReference; balancePaise: number; data: admin.firestore.DocumentData }

export async function readWallet(tx: Tx, uid: string): Promise<WalletState> {
  const snap = await tx.get(walletRef(uid));
  const data = snap.data() || {};
  return { ref: snap.ref, balancePaise: Number(data[Wallets.BALANCE_PAISE] || 0), data };
}

/**
 * Appends one ledger entry and moves the wallet balance by the same amount. The caller must have
 * checked (inside the same transaction) that the ledger id does not exist yet.
 */
export function postEntry(tx: Tx, uid: string, wallet: WalletState, eventId: string, type: string, amountPaise: number, refId: string) {
  const after = wallet.balancePaise + amountPaise;
  tx.create(ledgerRef(eventId), {
    [WalletLedger.UID]: uid,
    [WalletLedger.TYPE]: type,
    [WalletLedger.AMOUNT_PAISE]: amountPaise,
    [WalletLedger.REF_ID]: refId,
    [WalletLedger.BALANCE_AFTER_PAISE]: after,
    [WalletLedger.CREATED_AT]: Timestamp.now(),
  });
  const update: Record<string, unknown> = {
    [Wallets.BALANCE_PAISE]: after,
    [Wallets.UPDATED_AT]: Timestamp.now(),
  };
  if (amountPaise > 0 && type !== Values.LedgerType.REFUND && type !== Values.LedgerType.WALLET_DEPOSIT) {
    update[Wallets.LIFETIME_EARNED_PAISE] = FieldValue.increment(amountPaise);
  }
  if (type === Values.LedgerType.WITHDRAWAL) update[Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
  if (type === Values.LedgerType.REFUND) update[Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
  tx.set(wallet.ref, update, { merge: true });
  wallet.balancePaise = after;
}

// ───────────────────────────── wallet + code ─────────────────────────────

function generateNameCode(rawName?: string): string {
  const clean = (rawName || "").replace(/[^A-Za-z]/g, "").toUpperCase();
  const isGeneric = !clean || clean === "USER" || clean.startsWith("USER") || clean === "DUTY" || clean === "TEST" || clean === "ADMIN";
  const prefix = !isGeneric && clean.length >= 3 ? clean.slice(0, 5) : "DUTY";
  const num = randomInt(1000, 10000); // 1000 - 9999
  return `${prefix}${num}`;
}

/** Creates the wallet and the user's own referral code if missing, or upgrades generic codes to real name. Returns the code. */
export async function ensureWallet(
  uid: string,
  role: string = Values.Role.EMPLOYER,
  preferredName?: string,
  forceRefreshName?: boolean
): Promise<string> {
  const existing = await walletRef(uid).get();
  const currentCode = String(existing.get(Wallets.REFERRAL_CODE) || "");

  let baseName = preferredName?.trim() || "";
  if (!baseName) {
    const [wProf, eProf, uDoc] = await Promise.all([
      db.collection(WorkerProfiles.COLLECTION).doc(uid).get(),
      db.collection(EmployerProfiles.COLLECTION).doc(uid).get(),
      db.collection("users").doc(uid).get(),
    ]);
    baseName = String(
      wProf.get("name") ||
      eProf.get("ownerName") ||
      eProf.get("businessName") ||
      uDoc.get("name") ||
      uDoc.get("displayName") ||
      ""
    );
    if (!baseName) {
      try {
        const authUser = await admin.auth().getUser(uid);
        baseName = authUser.displayName || "";
      } catch {}
    }
  }

  const cleanBase = baseName.replace(/[^A-Za-z]/g, "").toUpperCase();
  const hasRealName = cleanBase.length >= 2 && cleanBase !== "USER" && !cleanBase.startsWith("USER");
  const isCurrentGeneric = !currentCode || currentCode.startsWith("USER") || currentCode.startsWith("DUTY");

  // If user already has a valid personalized code and we're not forcing refresh, return it
  if (currentCode && !isCurrentGeneric && !forceRefreshName) {
    return currentCode;
  }

  // If user already has a generic code (e.g. USER5769) but we don't have a real name yet, keep the generic code for now
  if (currentCode && !hasRealName && !forceRefreshName) {
    return currentCode;
  }

  // Generate a new code derived from the real name
  for (let attempt = 0; attempt < 8; attempt++) {
    const candidate = generateNameCode(hasRealName ? baseName : "");
    try {
      return await db.runTransaction(async (tx) => {
        const profileRef = role === Values.Role.EMPLOYER
          ? db.collection(EmployerProfiles.COLLECTION).doc(uid)
          : db.collection(WorkerProfiles.COLLECTION).doc(uid);
        const [wallet, codeDoc, profileSnap] = await Promise.all([
          tx.get(walletRef(uid)),
          tx.get(db.collection(ReferralCodes.COLLECTION).doc(candidate)),
          tx.get(profileRef)
        ]);
        const currentInTx = String(wallet.get(Wallets.REFERRAL_CODE) || "");
        if (currentInTx && !currentInTx.startsWith("USER") && !currentInTx.startsWith("DUTY") && !forceRefreshName) {
          return currentInTx;
        }
        if (codeDoc.exists) throw new Error("CODE_TAKEN");
        const phone = profileSnap.exists ? String(profileSnap.get("phone") || "") : "";
        const name = profileSnap.exists
          ? String(profileSnap.get(role === Values.Role.EMPLOYER ? "ownerName" : "name") || profileSnap.get("businessName") || "")
          : baseName;
        tx.create(codeDoc.ref, {
          [ReferralCodes.UID]: uid,
          [ReferralCodes.ROLE]: role,
          [ReferralCodes.ACTIVE]: true,
          phone,
          name: name || "DutyPe Member",
          createdAt: Timestamp.now(),
        });
        // If there was an old code, update its name and keep active as alias
        if (currentInTx && currentInTx !== candidate) {
          const oldCodeRef = db.collection(ReferralCodes.COLLECTION).doc(currentInTx);
          tx.set(oldCodeRef, {
            name: name || "DutyPe Member",
            aliasFor: candidate,
            updatedAt: Timestamp.now()
          }, { merge: true });
        }
        tx.set(wallet.ref, {
          [Wallets.REFERRAL_CODE]: candidate,
          [Wallets.BALANCE_PAISE]: Number(wallet.get(Wallets.BALANCE_PAISE) || 0),
          [Wallets.LIFETIME_EARNED_PAISE]: Number(wallet.get(Wallets.LIFETIME_EARNED_PAISE) || 0),
          [Wallets.WITHDRAWN_PAISE]: Number(wallet.get(Wallets.WITHDRAWN_PAISE) || 0),
          [Wallets.SUCCESSFUL_REFERRALS]: Number(wallet.get(Wallets.SUCCESSFUL_REFERRALS) || 0),
          [Wallets.AWARDED_MILESTONES]: wallet.get(Wallets.AWARDED_MILESTONES) || [],
          [Wallets.BLOCKED]: wallet.get(Wallets.BLOCKED) === true,
          [Wallets.UPDATED_AT]: Timestamp.now(),
        }, { merge: true });

        // Sync to authoritative users collection
        tx.set(db.collection("users").doc(uid), {
          referralCode: candidate,
          ...(name ? { name } : {}),
          updatedAt: Timestamp.now()
        }, { merge: true });

        return candidate;
      });
    } catch (error) {
      if ((error as Error).message !== "CODE_TAKEN") throw error;
    }
  }
  throw new functions.https.HttpsError("internal", "Could not create a referral code");
}

/** The app asks for its own code (older accounts get one on first call, upgraded with preferredName). */
export const ensureWalletCallable = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const preferredName = str(data, "name", { min: 0, max: 100, optional: true });
  let role = String(context.auth!.token.role || "");
  if (!role) {
    const userDoc = await db.collection("users").doc(uid).get();
    role = String(userDoc.get("role") || "");
  }
  const code = await ensureWallet(uid, role === Values.Role.EMPLOYER ? role : Values.Role.WORKER, preferredName);
  return { referralCode: code };
});

/** Explicitly refresh/upgrade the referral code with the user's real verified name. */
export const refreshReferralCode = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const name = str(data, "name", { min: 2, max: 100 });
  let role = String(context.auth!.token.role || "");
  if (!role) {
    const userDoc = await db.collection("users").doc(uid).get();
    role = String(userDoc.get("role") || Values.Role.WORKER);
  }
  const code = await ensureWallet(uid, role === Values.Role.EMPLOYER ? role : Values.Role.WORKER, name, true);
  return { referralCode: code };
});

// ───────────────────────────── referrals ─────────────────────────────

export function normalizeCode(raw: unknown): string {
  if (typeof raw !== "string" && typeof raw !== "number") return "";
  return String(raw)
    .trim()
    .replace(/[\u200B-\u200D\uFEFF\u00A0\s]/g, "") // Strip zero-width, non-breaking, and regular whitespace
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
}

/**
 * Records that [uid] joined with [rawCode]. Never throws: returns null on success or user-facing error string.
 * The referral is processed with atomic ACID guarantees for ledger and wallets.
 */
const LATE_REFERRAL_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;

async function resolveReferralCodeDoc(code: string): Promise<FirebaseFirestore.DocumentSnapshot | null> {
  // 1. Direct uppercase lookup
  let snap = await db.collection(ReferralCodes.COLLECTION).doc(code).get();
  if (snap.exists) return snap;

  // 2. Direct lowercase lookup
  snap = await db.collection(ReferralCodes.COLLECTION).doc(code.toLowerCase()).get();
  if (snap.exists) return snap;

  // 3. Search by field in referral_codes (in case document ID was auto-generated or UID)
  const qSnap = await db.collection(ReferralCodes.COLLECTION)
    .where("referralCode", "in", [code, code.toUpperCase(), code.toLowerCase()])
    .limit(1)
    .get();
  if (!qSnap.empty) return qSnap.docs[0];

  // 4. Search in referral_stats (Wallets collection)
  const wSnap = await db.collection(Wallets.COLLECTION)
    .where(Wallets.REFERRAL_CODE, "in", [code, code.toUpperCase(), code.toLowerCase()])
    .limit(1)
    .get();
  if (!wSnap.empty) {
    const wDoc = wSnap.docs[0];
    const referrerUid = wDoc.id;
    const [wProf, eProf, uDoc] = await Promise.all([
      db.collection(WorkerProfiles.COLLECTION).doc(referrerUid).get(),
      db.collection(EmployerProfiles.COLLECTION).doc(referrerUid).get(),
      db.collection("users").doc(referrerUid).get(),
    ]);
    const name = String(
      wProf.get("name") ||
      eProf.get("ownerName") ||
      eProf.get("businessName") ||
      uDoc.get("name") ||
      uDoc.get("displayName") ||
      "DutyPe Member"
    );
    const phone = String(wProf.get("phone") || eProf.get("phone") || uDoc.get("phone") || "");
    const role = eProf.exists ? Values.Role.EMPLOYER : Values.Role.WORKER;
    const refDoc = db.collection(ReferralCodes.COLLECTION).doc(code);
    await refDoc.set({
      [ReferralCodes.UID]: referrerUid,
      [ReferralCodes.ROLE]: role,
      [ReferralCodes.ACTIVE]: true,
      phone,
      name,
      createdAt: Timestamp.now(),
    }, { merge: true });
    return await refDoc.get();
  }

  // 5. Search in users collection
  const uSnap = await db.collection("users")
    .where("referralCode", "in", [code, code.toUpperCase(), code.toLowerCase()])
    .limit(1)
    .get();
  if (!uSnap.empty) {
    const uDoc = uSnap.docs[0];
    const referrerUid = uDoc.id;
    const name = String(uDoc.get("name") || uDoc.get("displayName") || "DutyPe Member");
    const phone = String(uDoc.get("phone") || "");
    const role = String(uDoc.get("role") || Values.Role.WORKER) === Values.Role.EMPLOYER
      ? Values.Role.EMPLOYER
      : Values.Role.WORKER;
    const refDoc = db.collection(ReferralCodes.COLLECTION).doc(code);
    await refDoc.set({
      [ReferralCodes.UID]: referrerUid,
      [ReferralCodes.ROLE]: role,
      [ReferralCodes.ACTIVE]: true,
      phone,
      name,
      createdAt: Timestamp.now(),
    }, { merge: true });
    return await refDoc.get();
  }

  return null;
}

export async function registerReferral(
  uid: string,
  rawCode: string,
  providedPhone?: string,
  providedName?: string
): Promise<string | null> {
  const code = normalizeCode(rawCode);
  if (code.length < 3 || code.length > 20) return "Invalid referral code format";
  try {
    const codeDoc = await resolveReferralCodeDoc(code);
    if (!codeDoc || !codeDoc.exists) return "Referral code not found";
    if (codeDoc.get(ReferralCodes.ACTIVE) !== true) return "This referral code is disabled";

    const referrerUid = String(codeDoc.get(ReferralCodes.UID) || "");
    if (!referrerUid) return "Referral code has no active owner";
    if (referrerUid === uid) return "You cannot use your own referral code";

    const hourAgo = Timestamp.fromMillis(Date.now() - 60 * 60 * 1000);
    const recent = await db.collection(Referrals.COLLECTION)
      .where(Referrals.REFERRER_UID, "==", referrerUid)
      .where(Referrals.CREATED_AT, ">=", hourAgo)
      .count().get();
    const fraudScore = recent.data().count >= MAX_REFERRALS_PER_HOUR ? 60 : 0;

    const config = await getReferralConfig();

    // Fetch referee details (name and phone) to store in referral record
    let refereePhone = (providedPhone || "").trim();
    let refereeName = (providedName || "").trim();
    if (!refereePhone || !refereeName) {
      const [wProf, eProf, uDoc] = await Promise.all([
        db.collection(WorkerProfiles.COLLECTION).doc(uid).get(),
        db.collection(EmployerProfiles.COLLECTION).doc(uid).get(),
        db.collection("users").doc(uid).get()
      ]);
      if (!refereePhone) {
        refereePhone = String(wProf.get("phone") || eProf.get("phone") || uDoc.get("phone") || "");
      }
      if (!refereeName) {
        refereeName = String(
          wProf.get("name") ||
          eProf.get("ownerName") ||
          eProf.get("businessName") ||
          uDoc.get("name") ||
          uDoc.get("displayName") ||
          ""
        );
      }
      if (!refereePhone || !refereeName) {
        try {
          const authUser = await admin.auth().getUser(uid);
          if (!refereePhone) refereePhone = authUser.phoneNumber || "";
          if (!refereeName) refereeName = authUser.displayName || "";
        } catch {}
      }
    }

    const referrerPhone = String(codeDoc.get("phone") || "");
    const cleanReferrerPhone = referrerPhone.replace(/[^0-9]/g, "").slice(-10);
    const cleanRefereePhone = refereePhone.replace(/[^0-9]/g, "").slice(-10);

    // Self-referral guard: Same phone number
    if (cleanReferrerPhone && cleanRefereePhone && cleanReferrerPhone === cleanRefereePhone) {
      return "You cannot refer an account with the same phone number";
    }

    const bonusEventId = `referral_${uid}_referee`;
    const rewardEventId = `referral_${uid}_referrer`;

    const txResult = await db.runTransaction(async (tx) => {
      const ref = db.collection(Referrals.COLLECTION).doc(uid);
      const [existing, referrerWallet, refereeWallet, referrerRefDoc, bonusEntry, rewardEntry] = await Promise.all([
        tx.get(ref),
        readWallet(tx, referrerUid),
        readWallet(tx, uid),
        tx.get(db.collection(Referrals.COLLECTION).doc(referrerUid)),
        tx.get(ledgerRef(bonusEventId)),
        tx.get(ledgerRef(rewardEventId)),
      ]);

      if (existing.exists) {
        if (existing.get(Referrals.CODE) === code) {
          // Idempotent success: Already linked to this exact referral code
          return null;
        }
        return "A referral code was already used for this account";
      }
      if (referrerWallet.data[Wallets.BLOCKED] === true) return "This referral code belongs to an inactive account";

      // Circular referral guard: Prevent A -> B -> A loops
      if (referrerRefDoc.exists && referrerRefDoc.get(Referrals.REFERRER_UID) === uid) {
        return "Circular referrals are not permitted";
      }

      const now = Date.now();
      const isClean = fraudScore === 0;

      // Ensure referee wallet structure exists
      if (!refereeWallet.data || Object.keys(refereeWallet.data).length === 0) {
        tx.set(refereeWallet.ref, {
          [Wallets.BALANCE_PAISE]: 0,
          [Wallets.LIFETIME_EARNED_PAISE]: 0,
          [Wallets.WITHDRAWN_PAISE]: 0,
          [Wallets.SUCCESSFUL_REFERRALS]: 0,
          [Wallets.AWARDED_MILESTONES]: [],
          [Wallets.BLOCKED]: false,
          [Wallets.UPDATED_AT]: Timestamp.now(),
        }, { merge: true });
      }

      tx.create(ref, {
        [Referrals.REFERRER_UID]: referrerUid,
        [Referrals.CODE]: code,
        [Referrals.STATUS]: isClean ? Values.ReferralStatus.COMPLETED : Values.ReferralStatus.REJECTED,
        [Referrals.FRAUD_SCORE]: fraudScore,
        refereeName: refereeName || "DutyPe Friend",
        refereePhone,
        referrerName: String(codeDoc.get("name") || "DutyPe Member"),
        [Referrals.CREATED_AT]: Timestamp.fromMillis(now),
        [Referrals.COMPLETED_AT]: isClean ? Timestamp.fromMillis(now) : null,
        [Referrals.EXPIRES_AT]: Timestamp.fromMillis(now + REFERRAL_EXPIRY_MS),
      });

      if (isClean) {
        // 1. Instant credit: referee receives their signup bonus immediately upon entering code
        const bonusPaise = Math.round(config.signupBonus * 100);
        if (!bonusEntry.exists && bonusPaise > 0) {
          postEntry(tx, uid, refereeWallet, bonusEventId, Values.LedgerType.SIGNUP_BONUS, bonusPaise, code);
        }

        // 2. Instant credit: referrer receives their referral reward immediately upon friend joining
        const rewardPaise = Math.round(config.rewardPerReferral * 100);
        if (!rewardEntry.exists && rewardPaise > 0) {
          postEntry(tx, referrerUid, referrerWallet, rewardEventId, Values.LedgerType.REFERRAL_REWARD, rewardPaise, uid);
        }

        // Increment successful referrals count on referrer wallet
        const successful = Number(referrerWallet.data[Wallets.SUCCESSFUL_REFERRALS] || 0) + 1;
        const awarded: number[] = Array.isArray(referrerWallet.data[Wallets.AWARDED_MILESTONES]) ?
          referrerWallet.data[Wallets.AWARDED_MILESTONES].map(Number) : [];
        const milestoneRupees = Number(config.milestones?.[String(successful)] || 0);
        if (milestoneRupees > 0 && !awarded.includes(successful)) {
          const milestonePaise = Math.round(milestoneRupees * 100);
          postEntry(tx, referrerUid, referrerWallet, `milestone_${referrerUid}_${successful}`, Values.LedgerType.MILESTONE, milestonePaise, String(successful));
          awarded.push(successful);
        }
        tx.set(referrerWallet.ref, {
          [Wallets.SUCCESSFUL_REFERRALS]: successful,
          [Wallets.AWARDED_MILESTONES]: awarded,
          [Wallets.UPDATED_AT]: Timestamp.now()
        }, { merge: true });
      }

      // Mark user record atomically
      tx.set(db.collection("users").doc(uid), {
        hasUsedReferralCode: true,
        referredBy: referrerUid,
        referredByCode: code,
        referredByName: String(codeDoc.get("name") || "DutyPe Member"),
        referralLinkedAt: Timestamp.now(),
        updatedAt: Timestamp.now()
      }, { merge: true });

      return null;
    });

    if (txResult === null && fraudScore === 0) {
      // Notify referee of instant credit
      notify(uid, {
        type: "SIGNUP_BONUS",
        templateId: "SIGNUP_BONUS",
        params: { amount: config.signupBonus }
      }).catch(() => {});
      // Notify referrer of new friend joining with friend's name
      notify(referrerUid, {
        type: "REFERRAL",
        templateId: "REFERRAL_REWARD_BASIC",
        params: { name: refereeName || "Friend", amount: config.rewardPerReferral }
      }).catch(() => {});
    }

    return txResult;
  } catch (error) {
    functions.logger.warn(`registerReferral(${uid}) failed`, error);
    return "Referral could not be applied. Please try again.";
  }
}

/** A code entered after sign-up (profile setup or Refer & Earn screen). One referral per user. */
export const applyReferralCode = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;

  // Rate limiting: Max 5 code attempts per 10 minutes to prevent dictionary guessing
  const attemptRef = db.collection("referral_attempts").doc(uid);
  const attemptSnap = await attemptRef.get();
  const nowMs = Date.now();
  const attemptData = attemptSnap.data() || {};
  const windowStart = Number(attemptData.windowStart || 0);
  const count = Number(attemptData.count || 0);

  if (nowMs - windowStart < 10 * 60 * 1000) {
    if (count >= 5) {
      fail("resource-exhausted", "Too many referral code attempts. Please wait 10 minutes before trying again.");
    }
    await attemptRef.set({ count: count + 1, windowStart, lastAttemptAt: Timestamp.now() }, { merge: true });
  } else {
    await attemptRef.set({ count: 1, windowStart: nowMs, lastAttemptAt: Timestamp.now() }, { merge: true });
  }

  const data = obj(raw);
  const rawCode = str(data, "code", { min: 3, max: 30 });
  const code = normalizeCode(rawCode);
  const phone = str(data, "phone", { optional: true, max: 20 });
  const name = str(data, "name", { optional: true, max: 100 });

  const error = await registerReferral(uid, code, phone, name);
  if (error) fail("invalid-argument", error);

  // Reset attempt count upon successful application
  await attemptRef.delete().catch(() => {});

  const codeDoc = await resolveReferralCodeDoc(code);
  return {
    ok: true,
    referrerName: String(codeDoc?.get("name") || "DutyPe Member"),
    code
  };
});

/**
 * The referred user did the real thing (a worker's first application, an employer's first job
 * post; called from applyToJob / postJob, one read when nothing is pending): pay the referrer (plus any milestone bonus), give the
 * new worker their signup bonus, and give employer referrers job-post credits at milestones.
 * Idempotent: the referral flips PENDING → COMPLETED inside the transaction and every credit's
 * ledger id is the event.
 */
export async function completeReferral(refereeUid: string, refereeRole: string): Promise<void> {
  const refDoc = db.collection(Referrals.COLLECTION).doc(refereeUid);
  const peek = await refDoc.get();
  if (!peek.exists || peek.get(Referrals.STATUS) !== Values.ReferralStatus.PENDING) return;
  const config = await getReferralConfig();

  const paid = await db.runTransaction(async (tx) => {
    const referral = await tx.get(refDoc);
    if (referral.get(Referrals.STATUS) !== Values.ReferralStatus.PENDING) return null;
    const referrerUid = String(referral.get(Referrals.REFERRER_UID));
    const expiresAt = (referral.get(Referrals.EXPIRES_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    const refCode = String(referral.get(Referrals.CODE));
    let referrerCodeSnap = await tx.get(db.collection(ReferralCodes.COLLECTION).doc(refCode));
    if (!referrerCodeSnap.exists) {
      referrerCodeSnap = await tx.get(db.collection(ReferralCodes.COLLECTION).doc(refCode.toLowerCase()));
    }
    const isEmployer = (await tx.get(db.collection(EmployerProfiles.COLLECTION).doc(referrerUid))).exists;
    const referrerRole = String(
      (referrerCodeSnap.exists ? referrerCodeSnap.get(ReferralCodes.ROLE) : null) ||
      (isEmployer ? Values.Role.EMPLOYER : Values.Role.WORKER)
    );
    const rewardId = `referral_${refereeUid}_referrer`;
    const bonusId = `referral_${refereeUid}_referee`;
    const [referrer, referee, rewardEntry, bonusEntry] = await Promise.all([
      readWallet(tx, referrerUid), readWallet(tx, refereeUid), tx.get(ledgerRef(rewardId)), tx.get(ledgerRef(bonusId)),
    ]);
    const employerRef = db.collection(EmployerProfiles.COLLECTION).doc(referrerUid);
    const employerSnap = referrerRole === Values.Role.EMPLOYER ? await tx.get(employerRef) : null;

    if (expiresAt > 0 && Date.now() > expiresAt) {
      tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.EXPIRED });
      return null;
    }
    if (referrer.data[Wallets.BLOCKED] === true) {
      tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.REJECTED });
      return null;
    }

    const successful = Number(referrer.data[Wallets.SUCCESSFUL_REFERRALS] || 0) + 1;
    const awarded: number[] = Array.isArray(referrer.data[Wallets.AWARDED_MILESTONES]) ?
      referrer.data[Wallets.AWARDED_MILESTONES].map(Number) : [];
    tx.update(refDoc, { [Referrals.STATUS]: Values.ReferralStatus.COMPLETED, [Referrals.COMPLETED_AT]: Timestamp.now() });
    tx.set(referrer.ref, { [Wallets.SUCCESSFUL_REFERRALS]: successful }, { merge: true });

    let referrerPaise = 0;
    if (!rewardEntry.exists) {
      referrerPaise = Math.round(config.rewardPerReferral * 100);
      postEntry(tx, referrerUid, referrer, rewardId, Values.LedgerType.REFERRAL_REWARD, referrerPaise, refereeUid);
    }
    const milestoneRupees = Number(config.milestones?.[String(successful)] || 0);
    if (milestoneRupees > 0 && !awarded.includes(successful)) {
      const milestonePaise = Math.round(milestoneRupees * 100);
      postEntry(tx, referrerUid, referrer, `milestone_${referrerUid}_${successful}`, Values.LedgerType.MILESTONE, milestonePaise, String(successful));
      tx.set(referrer.ref, { [Wallets.AWARDED_MILESTONES]: FieldValue.arrayUnion(successful) }, { merge: true });
      referrerPaise += milestonePaise;
    }
    if (referrerRole === Values.Role.EMPLOYER && EMPLOYER_CREDIT_MILESTONES[successful]) {
      const S = EmployerProfiles.Subscription;
      tx.set(employerRef, {
        [EmployerProfiles.SUBSCRIPTION]: {
          [S.CREDITS]: { [S.CREDITS_NORMAL]: FieldValue.increment(EMPLOYER_CREDIT_MILESTONES[successful]) },
        },
      }, { merge: true });
    }

    // Every 2 successful referrals: grant one free job post (normal or urgent) valid for 30 days.
    let freePost = false;
    if (employerSnap?.exists && successful % 2 === 0) {
      const nowMs = Date.now();
      tx.update(employerRef, {
        [EmployerProfiles.REFERRAL_FREE_POSTS]: referralPostsLeft(employerSnap.data(), nowMs) + 1,
        [EmployerProfiles.REFERRAL_FREE_POSTS_UNTIL]: Timestamp.fromMillis(nowMs + REFERRAL_POST_VALID_MS),
      });
      freePost = true;
    }

    let refereePaise = 0;
    if (refereeRole === Values.Role.WORKER && !bonusEntry.exists && config.signupBonus > 0) {
      refereePaise = Math.round(config.signupBonus * 100);
      postEntry(tx, refereeUid, referee, bonusId, Values.LedgerType.SIGNUP_BONUS, refereePaise, referrerUid);
    }
    return { referrerUid, referrerRole, referrerPaise, refereePaise, freePost };
  });

  if (!paid) return;
  if (paid.referrerPaise > 0) {
    await notify(paid.referrerUid, {
      type: "PAYMENT", templateId: "REFERRAL_REWARD_BASIC", role: paid.referrerRole,
      params: { amount: paid.referrerPaise / 100 }, data: { refId: refereeUid },
    });
  }
  if (paid.freePost) {
    await notify(paid.referrerUid, {
      type: "PAYMENT", templateId: "REFERRAL_FREE_POST", role: Values.Role.EMPLOYER,
      params: {}, data: { action: "post_job" },
    });
  }
  if (paid.refereePaise > 0) {
    await notify(refereeUid, {
      type: "PAYMENT", templateId: "SIGNUP_BONUS", role: Values.Role.WORKER,
      params: { amount: paid.refereePaise / 100 }, data: {},
    });
  }
}

// ───────────────────────────── withdrawals ─────────────────────────────

const UPI_PATTERN = /^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.-]{1,64}$/;

/** Cash out the wallet to UPI (whole balance, capped per day). requestId makes a retry harmless. */
export const requestWithdrawal = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const upiId = str(data, "upiId", { min: 5, max: 100, pattern: UPI_PATTERN });
  const reqId = str(data, "requestId", { min: 8, max: 80, pattern: /^[A-Za-z0-9_-]+$/ });
  const config = await getReferralConfig();
  const minPaise = Math.round(config.minWithdrawal * 100);
  const dailyCapPaise = Math.round(config.maxWithdrawalPerDay * 100);
  const withdrawalId = createHash("sha256").update(`${uid}:${reqId}`).digest("hex").slice(0, 32);
  const day = new Date(Date.now() + 330 * 60 * 1000).toISOString().slice(0, 10);
  const withdrawalRef = db.collection(Withdrawals.COLLECTION).doc(withdrawalId);
  const dailyRef = db.collection(WithdrawalDaily.COLLECTION).doc(`${uid}_${day}`);
  const burstRef = db.collection("withdrawal_burst_limits").doc(uid);

  const result = await db.runTransaction(async (tx) => {
    const [existing, wallet, daily, burst] = await Promise.all([
      tx.get(withdrawalRef),
      readWallet(tx, uid),
      tx.get(dailyRef),
      tx.get(burstRef)
    ]);
    if (existing.exists) return { withdrawalId, amountPaise: Number(existing.get(Withdrawals.AMOUNT_PAISE)), replay: true };

    const lastRequestMs = Number(burst.get("lastRequestMs") || 0);
    if (Date.now() - lastRequestMs < 30 * 1000) {
      fail("resource-exhausted", "Please wait 30 seconds before submitting another withdrawal request.");
    }
    tx.set(burstRef, { lastRequestMs: Date.now() }, { merge: true });

    if (wallet.data[Wallets.BLOCKED] === true) fail("permission-denied", "Your wallet is blocked. Contact support.");
    const dailyPaise = Number(daily.get(WithdrawalDaily.AMOUNT_PAISE) || 0);
    const dailyCount = Number(daily.get(WithdrawalDaily.REQUEST_COUNT) || 0);
    if (dailyCount >= MAX_WITHDRAWALS_PER_DAY) fail("resource-exhausted", "Daily withdrawal limit reached");
    // The whole balance, up to what is left of today's cap; the rest can be withdrawn tomorrow.
    const amountPaise = Math.min(wallet.balancePaise, dailyCapPaise - dailyPaise);
    if (wallet.balancePaise < minPaise) fail("failed-precondition", `You need at least ₹${config.minWithdrawal} to withdraw`);
    if (amountPaise < minPaise) fail("resource-exhausted", `You can withdraw up to ₹${config.maxWithdrawalPerDay} per day`);

    const now = Timestamp.now();
    tx.create(withdrawalRef, {
      [Withdrawals.UID]: uid,
      [Withdrawals.AMOUNT_PAISE]: amountPaise,
      [Withdrawals.UPI_ID]: upiId,
      [Withdrawals.STATUS]: Values.WithdrawalStatus.PENDING,
      [Withdrawals.CREATED_AT]: now,
    });
    postEntry(tx, uid, wallet, `withdrawal_${withdrawalId}`, Values.LedgerType.WITHDRAWAL, -amountPaise, withdrawalId);
    tx.set(dailyRef, {
      [WithdrawalDaily.AMOUNT_PAISE]: dailyPaise + amountPaise,
      [WithdrawalDaily.REQUEST_COUNT]: dailyCount + 1,
      [WithdrawalDaily.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 2 * DAY_MS),
    });
    return { withdrawalId, amountPaise, replay: false };
  });
  return result;
});

/**
 * Admin settles a withdrawal: COMPLETED (with the payout reference) or FAILED (money goes back to
 * the wallet as a REFUND entry). A settled withdrawal can never change again.
 */
export const settleWithdrawal = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const data = obj(raw);
  const withdrawalId = str(data, "withdrawalId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const status = String(data.status || "").toUpperCase();
  if (status !== Values.WithdrawalStatus.COMPLETED && status !== Values.WithdrawalStatus.FAILED &&
    status !== Values.WithdrawalStatus.PROCESSING) fail("invalid-argument", "status is invalid");
  const txnRef = str(data, "txnRef", { max: 100, optional: true });
  const failureReason = str(data, "failureReason", { max: 300, optional: true });
  if (status === Values.WithdrawalStatus.COMPLETED && !txnRef) fail("invalid-argument", "txnRef is required");

  const ref = db.collection(Withdrawals.COLLECTION).doc(withdrawalId);
  const outcome = await db.runTransaction(async (tx) => {
    const w = await tx.get(ref);
    if (!w.exists) fail("not-found", "Withdrawal not found");
    const current = String(w.get(Withdrawals.STATUS));
    if (current === Values.WithdrawalStatus.COMPLETED || current === Values.WithdrawalStatus.FAILED) {
      fail("failed-precondition", `Withdrawal is already ${current}`);
    }
    const uid = String(w.get(Withdrawals.UID));
    const amountPaise = Number(w.get(Withdrawals.AMOUNT_PAISE));
    const refundId = `refund_${withdrawalId}`;
    const [wallet, refund] = await Promise.all([readWallet(tx, uid), tx.get(ledgerRef(refundId))]);
    tx.update(ref, {
      [Withdrawals.STATUS]: status,
      ...(txnRef ? { [Withdrawals.TXN_REF]: txnRef } : {}),
      ...(failureReason ? { [Withdrawals.FAILURE_REASON]: failureReason } : {}),
      ...(status !== Values.WithdrawalStatus.PROCESSING ? { [Withdrawals.PROCESSED_AT]: Timestamp.now() } : {}),
    });
    if (status === Values.WithdrawalStatus.FAILED && !refund.exists) {
      postEntry(tx, uid, wallet, refundId, Values.LedgerType.REFUND, amountPaise, withdrawalId);
    }
    return { uid, amountPaise };
  });
  if (status !== Values.WithdrawalStatus.PROCESSING) {
    await notify(outcome.uid, {
      type: "PAYMENT",
      templateId: status === Values.WithdrawalStatus.COMPLETED ? "WITHDRAWAL_COMPLETED" : "WITHDRAWAL_FAILED",
      params: { amount: outcome.amountPaise / 100 },
      data: { withdrawalId, status },
    });
  }
  return { withdrawalId, status };
});

// ───────────────────────────── leaderboard + expiry ─────────────────────────────

let leaderboardCache: { at: number; rows: unknown[] } | null = null;

export const getReferralLeaderboard = onCallSecured({ enforceAppCheck: false }, async () => {
  if (leaderboardCache && Date.now() - leaderboardCache.at < 10 * 60 * 1000) return { leaders: leaderboardCache.rows };
  const top = await db.collection(Wallets.COLLECTION)
    .where(Wallets.BLOCKED, "==", false)
    .orderBy(Wallets.SUCCESSFUL_REFERRALS, "desc")
    .limit(20).get();
  const profiles = top.empty ? [] : await db.getAll(...top.docs.map((d) => db.collection(WorkerProfiles.COLLECTION).doc(d.id)));
  const rows = top.docs.map((d, i) => {
    const full = String(profiles[i]?.get(WorkerProfiles.NAME) || "DutyPe user").trim().split(/\s+/);
    return {
      rank: i + 1,
      name: full.length > 1 ? `${full[0]} ${full[full.length - 1][0]}.` : full[0],
      successfulReferrals: Number(d.get(Wallets.SUCCESSFUL_REFERRALS) || 0),
    };
  });
  leaderboardCache = { at: Date.now(), rows };
  return { leaders: rows };
});

export const expirePendingReferrals = functions
  .region("asia-south1")
  .pubsub.schedule("every 24 hours")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const snap = await db.collection(Referrals.COLLECTION)
      .where(Referrals.STATUS, "==", Values.ReferralStatus.PENDING)
      .where(Referrals.EXPIRES_AT, "<=", Timestamp.now())
      .limit(500).get();
    if (snap.empty) return null;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.update(d.ref, { [Referrals.STATUS]: Values.ReferralStatus.EXPIRED }));
    await batch.commit();
    return null;
  });
