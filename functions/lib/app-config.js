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
 *   signupBonus:        number   // default 25 (worker welcome/referral signup bonus)
 *   employerSignupBonus:number   // default 10
 *   employerSignupBonusEnabled: boolean
 *   employerUnlimitedJobPostingEnabled: boolean
 *   welcomeBonusCampaignId: string
 *   minWithdrawal:      number   // default 100
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
    employerSignupBonus: 10,
    employerSignupBonusEnabled: false,
    employerUnlimitedJobPostingEnabled: false,
    welcomeBonusCampaignId: "welcome_bonus_v1",
    minWithdrawal: 100,
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
            employerSignupBonus: num(data.employerSignupBonus, exports.DEFAULT_REFERRAL_CONFIG.employerSignupBonus),
            employerSignupBonusEnabled: bool(data.employerSignupBonusEnabled, exports.DEFAULT_REFERRAL_CONFIG.employerSignupBonusEnabled),
            employerUnlimitedJobPostingEnabled: bool(data.employerUnlimitedJobPostingEnabled, exports.DEFAULT_REFERRAL_CONFIG.employerUnlimitedJobPostingEnabled),
            welcomeBonusCampaignId: text(data.welcomeBonusCampaignId, exports.DEFAULT_REFERRAL_CONFIG.welcomeBonusCampaignId, 64),
            minWithdrawal: Math.max(num(data.minWithdrawal, exports.DEFAULT_REFERRAL_CONFIG.minWithdrawal), 100),
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
function bool(v, d) {
    return typeof v === "boolean" ? v : d;
}
function text(v, d, maxLength) {
    const raw = typeof v === "string" ? v.trim() : "";
    return raw ? raw.slice(0, maxLength) : d;
}
const ADMIN_DOMAIN = "@dutype.com";
const ADMIN_ROLE = "ADMIN";
const LEGACY_ADMIN_EMAILS = new Set([
    "admin@dutype.com",
    "vamsi@dutype.com",
    "vamsib298@gmail.com",
    "dutypein@gmail.com",
    "dutpyein@gmail.com",
]);
async function isCallerAdmin(context) {
    if (!context.auth)
        return false;
    const claims = (context.auth.token || {});
    // 1. Check custom claim 'admin'
    if (claims.admin === true)
        return true;
    // 2. Check role / activeRole in claims
    const role = String(claims.role || "").trim().toUpperCase();
    const activeRole = String(claims.activeRole || "").trim().toUpperCase();
    if (role === ADMIN_ROLE || activeRole === ADMIN_ROLE)
        return true;
    // 3. Check email in claims (Firebase Auth ID token)
    const email = String(claims.email || "").trim().toLowerCase();
    if (email) {
        if (email.endsWith(ADMIN_DOMAIN) || LEGACY_ADMIN_EMAILS.has(email)) {
            // Opportunistically persist custom claim so future calls carry admin: true
            try {
                await admin.auth().setCustomUserClaims(context.auth.uid, Object.assign(Object.assign({}, claims), { admin: true }));
            }
            catch (err) {
                functions.logger.warn("isCallerAdmin: failed setting custom claim", { uid: context.auth.uid, err });
            }
            return true;
        }
    }
    // 4. Check Firestore users or admins document
    try {
        const db = admin.firestore();
        const uid = context.auth.uid;
        const userSnap = await db.collection("users").doc(uid).get();
        if (userSnap.exists) {
            const uData = userSnap.data() || {};
            const uRole = String(uData.role || "").trim().toUpperCase();
            const uActiveRole = String(uData.activeRole || "").trim().toUpperCase();
            const uEmail = String(uData.email || "").trim().toLowerCase();
            if (uData.isAdmin === true ||
                uRole === ADMIN_ROLE ||
                uActiveRole === ADMIN_ROLE ||
                (uEmail && (uEmail.endsWith(ADMIN_DOMAIN) || LEGACY_ADMIN_EMAILS.has(uEmail)))) {
                try {
                    await admin.auth().setCustomUserClaims(uid, Object.assign(Object.assign({}, claims), { admin: true }));
                }
                catch (_a) { }
                return true;
            }
        }
        const adminSnap = await db.collection("admins").doc(uid).get();
        if (adminSnap.exists) {
            try {
                await admin.auth().setCustomUserClaims(uid, Object.assign(Object.assign({}, claims), { admin: true }));
            }
            catch (_b) { }
            return true;
        }
    }
    catch (err) {
        functions.logger.warn("isCallerAdmin: error checking firestore", { err: err === null || err === void 0 ? void 0 : err.message });
    }
    return false;
}
/**
 * Admin-only callable to update /app_config/referral.
 * Authorised via admin claim, ADMIN role, or allowlisted admin email.
 */
exports.updateReferralConfig = functions
    .region("asia-south1")
    .https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "login required");
    }
    const authorized = await isCallerAdmin(context);
    if (!authorized) {
        throw new functions.https.HttpsError("permission-denied", "admin only");
    }
    const patch = {};
    if ((data === null || data === void 0 ? void 0 : data.rewardPerReferral) !== undefined)
        patch.rewardPerReferral = numStrict(data.rewardPerReferral, "rewardPerReferral", 0, 10000);
    if ((data === null || data === void 0 ? void 0 : data.signupBonus) !== undefined)
        patch.signupBonus = numStrict(data.signupBonus, "signupBonus", 0, 10000);
    if ((data === null || data === void 0 ? void 0 : data.employerSignupBonus) !== undefined)
        patch.employerSignupBonus = numStrict(data.employerSignupBonus, "employerSignupBonus", 0, 10000);
    if ((data === null || data === void 0 ? void 0 : data.employerSignupBonusEnabled) !== undefined)
        patch.employerSignupBonusEnabled = boolStrict(data.employerSignupBonusEnabled, "employerSignupBonusEnabled");
    if ((data === null || data === void 0 ? void 0 : data.employerUnlimitedJobPostingEnabled) !== undefined)
        patch.employerUnlimitedJobPostingEnabled = boolStrict(data.employerUnlimitedJobPostingEnabled, "employerUnlimitedJobPostingEnabled");
    if ((data === null || data === void 0 ? void 0 : data.welcomeBonusCampaignId) !== undefined)
        patch.welcomeBonusCampaignId = textStrict(data.welcomeBonusCampaignId, "welcomeBonusCampaignId", 1, 64);
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
function boolStrict(v, field) {
    if (typeof v !== "boolean") {
        throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
    }
    return v;
}
function textStrict(v, field, minLength, maxLength) {
    if (typeof v !== "string") {
        throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
    }
    const value = v.trim();
    if (value.length < minLength || value.length > maxLength || !/^[A-Za-z0-9_-]+$/.test(value)) {
        throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
    }
    return value;
}
exports.getReferralConfigCallable = functions
    .region("asia-south1")
    .https.onCall(async () => {
    return await getReferralConfig();
});
//# sourceMappingURL=app-config.js.map