"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getReferralConfigCallable = exports.updateReferralConfig = exports.getReferralConfig = exports.DEFAULT_REFERRAL_CONFIG = void 0;
/**
 * Dynamic referral configuration — source of truth is
 * /app_config/referral. Cached in-process for 60s to keep Firestore reads
 * bounded under heavy CF fan-out.
 *
 * Shape (all numbers in INR unless stated):
 *   rewardPerReferral:  number   // default 25
 *   signupBonus:        number   // default 25
 *   minWithdrawal:      number   // default 50
 *   maxWithdrawalPerDay:number   // default 1000
 *   milestones:         { [count:string]: number }
 *   withdrawalMilestones: number[]
 *   updatedAt:          Timestamp
 *   updatedBy:          string  (admin uid)
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
exports.DEFAULT_REFERRAL_CONFIG = {
    rewardPerReferral: 25,
    signupBonus: 25,
    minWithdrawal: 50,
    maxWithdrawalPerDay: 1000,
    milestones: { "5": 50, "10": 100, "15": 150, "25": 250, "50": 500, "100": 1000 },
    withdrawalMilestones: [5, 10, 15],
};
const TTL_MS = 60 * 1000;
let cache = null;
async function getReferralConfig() {
    const now = Date.now();
    if (cache && cache.expiresAt > now)
        return cache.value;
    try {
        const snap = await admin.firestore().doc("app_config/referral").get();
        const data = snap.exists ? (snap.data() || {}) : {};
        const merged = {
            rewardPerReferral: num(data.rewardPerReferral, exports.DEFAULT_REFERRAL_CONFIG.rewardPerReferral),
            signupBonus: num(data.signupBonus, exports.DEFAULT_REFERRAL_CONFIG.signupBonus),
            minWithdrawal: num(data.minWithdrawal, exports.DEFAULT_REFERRAL_CONFIG.minWithdrawal),
            maxWithdrawalPerDay: num(data.maxWithdrawalPerDay, exports.DEFAULT_REFERRAL_CONFIG.maxWithdrawalPerDay),
            milestones: (data.milestones && typeof data.milestones === "object")
                ? data.milestones
                : exports.DEFAULT_REFERRAL_CONFIG.milestones,
            withdrawalMilestones: Array.isArray(data.withdrawalMilestones)
                ? data.withdrawalMilestones.map((v) => Number(v)).filter((v) => Number.isFinite(v))
                : exports.DEFAULT_REFERRAL_CONFIG.withdrawalMilestones,
        };
        cache = { value: merged, expiresAt: now + TTL_MS };
        return merged;
    }
    catch (e) {
        functions.logger.warn("getReferralConfig failed — falling back to defaults", { err: e === null || e === void 0 ? void 0 : e.message });
        return exports.DEFAULT_REFERRAL_CONFIG;
    }
}
exports.getReferralConfig = getReferralConfig;
function num(v, d) {
    const n = Number(v);
    return Number.isFinite(n) && n >= 0 ? n : d;
}
/**
 * Admin-only callable to update /app_config/referral.
 * Authorised via the `admin` custom claim on the caller. Set the claim with:
 *   admin.auth().setCustomUserClaims(uid, { admin: true });
 */
exports.updateReferralConfig = functions
    .region("asia-south1")
    .https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "login required");
    }
    const claims = (context.auth.token || {});
    if (!claims.admin) {
        throw new functions.https.HttpsError("permission-denied", "admin only");
    }
    const patch = {};
    if ((data === null || data === void 0 ? void 0 : data.rewardPerReferral) !== undefined)
        patch.rewardPerReferral = numStrict(data.rewardPerReferral, "rewardPerReferral", 0, 10000);
    if ((data === null || data === void 0 ? void 0 : data.signupBonus) !== undefined)
        patch.signupBonus = numStrict(data.signupBonus, "signupBonus", 0, 10000);
    if ((data === null || data === void 0 ? void 0 : data.minWithdrawal) !== undefined)
        patch.minWithdrawal = numStrict(data.minWithdrawal, "minWithdrawal", 1, 100000);
    if ((data === null || data === void 0 ? void 0 : data.maxWithdrawalPerDay) !== undefined)
        patch.maxWithdrawalPerDay = numStrict(data.maxWithdrawalPerDay, "maxWithdrawalPerDay", 1, 10000000);
    if ((data === null || data === void 0 ? void 0 : data.milestones) !== undefined) {
        if (!data.milestones || typeof data.milestones !== "object") {
            throw new functions.https.HttpsError("invalid-argument", "milestones must be object");
        }
        const out = {};
        for (const [k, v] of Object.entries(data.milestones)) {
            out[String(k)] = numStrict(v, `milestones.${k}`, 0, 100000);
        }
        patch.milestones = out;
    }
    if ((data === null || data === void 0 ? void 0 : data.withdrawalMilestones) !== undefined) {
        if (!Array.isArray(data.withdrawalMilestones)) {
            throw new functions.https.HttpsError("invalid-argument", "withdrawalMilestones must be array");
        }
        patch.withdrawalMilestones = data.withdrawalMilestones
            .map((v) => numStrict(v, "withdrawalMilestones[]", 0, 10000));
    }
    if (Object.keys(patch).length === 0) {
        throw new functions.https.HttpsError("invalid-argument", "no valid fields to update");
    }
    await admin.firestore().doc("app_config/referral").set(Object.assign(Object.assign({}, patch), { updatedAt: admin.firestore.FieldValue.serverTimestamp(), updatedBy: context.auth.uid }), { merge: true });
    // Invalidate in-process cache so subsequent CF calls see the new value.
    cache = null;
    functions.logger.info("updateReferralConfig: admin=" + context.auth.uid + " keys=" + Object.keys(patch).join(","));
    return { success: true };
});
function numStrict(v, field, min, max) {
    const n = Number(v);
    if (!Number.isFinite(n) || n < min || n > max) {
        throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
    }
    return n;
}
exports.getReferralConfigCallable = functions
    .region("asia-south1")
    .https.onCall(async () => {
    return await getReferralConfig();
});
//# sourceMappingURL=app-config.js.map