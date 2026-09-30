"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteAccount = exports.onEmployerProfileWritten = exports.lookupPhoneRole = exports.completeRegistration = void 0;
/**
 * Identity and profiles.
 *
 *   completeRegistration     after OTP: phoneRoles/{+91..} {uid, role} + the role profile + wallet
 *                            (+ the referral the user typed). One phone = one role, forever.
 *   lookupPhoneRole          pre-OTP check (one document read); never returns names or uids
 *   onEmployerProfileWritten pays a pending referral when the employer profile becomes complete
 *   deleteAccount            removes the user's personal data and sign-in; money records are kept
 *
 * Worker profiles are handled in workers.ts (card sync + the same referral hook).
 * There is no users/{uid} collection: role = phoneRoles, identity = the role profile,
 * push = user_tokens, money = referral_stats.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const app_config_1 = require("./app-config");
const referrals_1 = require("./referrals");
const schema_1 = require("./schema");
const db = admin.firestore();
const { Timestamp } = admin.firestore;
const ROLES = [schema_1.Values.Role.WORKER, schema_1.Values.Role.EMPLOYER];
function e164(raw) {
    const digits = String(raw !== null && raw !== void 0 ? raw : "").replace(/\D/g, "");
    if (/^[6-9]\d{9}$/.test(digits))
        return `+91${digits}`;
    if (/^91[6-9]\d{9}$/.test(digits))
        return `+${digits}`;
    return null;
}
exports.completeRegistration = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const phone = e164(context.auth.token.phone_number);
    if (!phone)
        (0, input_1.fail)("failed-precondition", "Sign in with your mobile number first");
    const role = (0, input_1.oneOf)(data, "role", ROLES);
    const name = (0, input_1.str)(data, "name", { min: 2, max: 80 });
    const referralCode = (0, input_1.str)(data, "referralCode", { max: 20, optional: true });
    const phoneRef = db.collection(schema_1.PhoneRoles.COLLECTION).doc(phone);
    const profileRef = db.collection(role === schema_1.Values.Role.WORKER ? schema_1.WorkerProfiles.COLLECTION : schema_1.EmployerProfiles.COLLECTION).doc(uid);
    const config = role === schema_1.Values.Role.EMPLOYER ? await (0, app_config_1.getReferralConfig)() : null;
    const created = await db.runTransaction(async (tx) => {
        const [phoneDoc, profile] = await Promise.all([tx.get(phoneRef), tx.get(profileRef)]);
        if (phoneDoc.exists) {
            if (phoneDoc.get(schema_1.PhoneRoles.UID) !== uid)
                (0, input_1.fail)("already-exists", "This number is linked to another account");
            if (phoneDoc.get(schema_1.PhoneRoles.ROLE) !== role) {
                (0, input_1.fail)("failed-precondition", `phone-already-registered-as:${phoneDoc.get(schema_1.PhoneRoles.ROLE)}`);
            }
        }
        else {
            tx.create(phoneRef, { [schema_1.PhoneRoles.UID]: uid, [schema_1.PhoneRoles.ROLE]: role });
        }
        if (profile.exists)
            return false;
        const now = Timestamp.now();
        if (role === schema_1.Values.Role.WORKER) {
            tx.create(profileRef, {
                [schema_1.WorkerProfiles.NAME]: name,
                [schema_1.WorkerProfiles.PHONE]: phone,
                [schema_1.WorkerProfiles.SKILLS]: [],
                [schema_1.WorkerProfiles.AVAILABLE]: false,
                [schema_1.WorkerProfiles.BLOCKED]: false,
                [schema_1.WorkerProfiles.CREATED_AT]: now,
                [schema_1.WorkerProfiles.UPDATED_AT]: now,
            });
        }
        else {
            const S = schema_1.EmployerProfiles.Subscription;
            const campaign = (config === null || config === void 0 ? void 0 : config.employerUnlimitedJobPostingEnabled) === true;
            tx.create(profileRef, {
                [schema_1.EmployerProfiles.EMPLOYER_TYPE]: schema_1.Values.EmployerType.INDIVIDUAL,
                [schema_1.EmployerProfiles.OWNER_NAME]: name,
                [schema_1.EmployerProfiles.PHONE]: phone,
                [schema_1.EmployerProfiles.SUBSCRIPTION]: Object.assign(Object.assign({ [S.PLAN_ID]: campaign ? "UNLIMITED_CAMPAIGN" : "", [S.STATUS]: campaign ? "ACTIVE" : "NONE" }, (campaign ? { [S.START_AT]: now } : {})), { [S.CREDITS]: { [S.CREDITS_NORMAL]: 0, [S.CREDITS_INSTANT]: 0 } }),
                [schema_1.EmployerProfiles.FREE_URGENT_POSTS_USED]: 0,
                [schema_1.EmployerProfiles.VERIFIED]: false,
                [schema_1.EmployerProfiles.TOTAL_HIRES]: 0,
                [schema_1.EmployerProfiles.BLOCKED]: false,
                [schema_1.EmployerProfiles.CREATED_AT]: now,
                [schema_1.EmployerProfiles.UPDATED_AT]: now,
            });
        }
        return true;
    });
    await admin.auth().setCustomUserClaims(uid, Object.assign(Object.assign({}, (context.auth.token.admin ? { admin: true } : {})), { role }));
    const code = await (0, referrals_1.ensureWallet)(uid, role);
    const referralError = created && referralCode ? await (0, referrals_1.registerReferral)(uid, referralCode) : null;
    // An employer profile is complete at creation, so the referral pays out now (the profile
    // trigger may have run before the referral existed). Workers complete later (skills).
    if (created && referralCode && !referralError && role === schema_1.Values.Role.EMPLOYER) {
        await (0, referrals_1.completeReferral)(uid, role);
    }
    return { role, created, referralCode: code, referralError };
});
/** Pre-OTP: does this number exist, and with which role? One read; rules keep phoneRoles private. */
exports.lookupPhoneRole = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: true, timeoutSeconds: 10 }, async (raw) => {
    const data = (0, input_1.obj)(raw);
    const phone = e164(data.phone);
    if (!phone)
        (0, input_1.fail)("invalid-argument", "Enter a valid 10-digit mobile number");
    const requested = String(data.requestedRole || "").toUpperCase();
    const doc = await db.collection(schema_1.PhoneRoles.COLLECTION).doc(phone).get();
    if (!doc.exists)
        return { exists: false, roleConflict: false };
    const existingRole = String(doc.get(schema_1.PhoneRoles.ROLE) || "");
    return {
        exists: true,
        existingRole,
        roleConflict: !!requested && requested !== existingRole,
    };
});
exports.onEmployerProfileWritten = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.EmployerProfiles.COLLECTION}/{uid}`)
    .onWrite(async (change, context) => {
    const after = change.after.data();
    if (!after || (0, referrals_1.isEmployerProfileComplete)(change.before.data()) || !(0, referrals_1.isEmployerProfileComplete)(after))
        return;
    await (0, referrals_1.completeReferral)(context.params.uid, schema_1.Values.Role.EMPLOYER);
});
/**
 * Deletes the caller's account: role profile (the worker card follows via its trigger), phone
 * registration, push token and saved jobs; closes open jobs / withdraws open applications; then the
 * Firebase Auth user. The wallet ledger and withdrawals are kept (blocked) for accounting.
 */
exports.deleteAccount = (0, secure_callable_1.onCallSecured)({}, async (_raw, context) => {
    const uid = context.auth.uid;
    const phone = e164(context.auth.token.phone_number);
    const role = String(context.auth.token.role || "");
    const batchUpdate = async (query, update) => {
        const snap = await query.limit(500).get();
        if (snap.empty)
            return;
        const batch = db.batch();
        snap.docs.forEach((d) => update(batch, d));
        await batch.commit();
    };
    if (role === schema_1.Values.Role.EMPLOYER) {
        await batchUpdate(db.collection(schema_1.Jobs.COLLECTION).where(schema_1.Jobs.EMPLOYER_ID, "==", uid).where(schema_1.Jobs.STATUS, "==", schema_1.Values.JobStatus.OPEN), (b, d) => b.update(d.ref, { [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.CLOSED }));
    }
    else {
        await batchUpdate(db.collection(schema_1.Applications.COLLECTION).where(schema_1.Applications.WORKER_ID, "==", uid)
            .where(schema_1.Applications.STATUS, "==", schema_1.Values.ApplicationStatus.APPLIED), (b, d) => b.update(d.ref, { [schema_1.Applications.STATUS]: schema_1.Values.ApplicationStatus.WITHDRAWN }));
    }
    await batchUpdate(db.collection(schema_1.SavedJobs.COLLECTION).where(schema_1.SavedJobs.USER_ID, "==", uid), (b, d) => b.delete(d.ref));
    const wallet = await db.collection(schema_1.Wallets.COLLECTION).doc(uid).get();
    const code = String(wallet.get(schema_1.Wallets.REFERRAL_CODE) || "");
    const batch = db.batch();
    batch.delete(db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid));
    batch.delete(db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid));
    batch.delete(db.collection(schema_1.UserTokens.COLLECTION).doc(uid));
    if (wallet.exists)
        batch.update(wallet.ref, { [schema_1.Wallets.BLOCKED]: true });
    if (code)
        batch.set(db.collection(schema_1.ReferralCodes.COLLECTION).doc(code), { [schema_1.ReferralCodes.ACTIVE]: false }, { merge: true });
    if (phone) {
        const phoneDoc = await db.collection(schema_1.PhoneRoles.COLLECTION).doc(phone).get();
        if (phoneDoc.get(schema_1.PhoneRoles.UID) === uid)
            batch.delete(phoneDoc.ref);
    }
    await batch.commit();
    await admin.auth().deleteUser(uid);
    functions.logger.info(`account ${uid} deleted`);
    return { ok: true };
});
//# sourceMappingURL=profiles.js.map