"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.expirePendingReferrals = exports.getReferralLeaderboard = exports.settleWithdrawal = exports.requestWithdrawal = exports.applyReferralCode = exports.refreshReferralCode = exports.ensureWalletCallable = exports.ledgerRef = exports.walletRef = void 0;
exports.readWallet = readWallet;
exports.postEntry = postEntry;
exports.ensureWallet = ensureWallet;
exports.normalizeCode = normalizeCode;
exports.registerReferral = registerReferral;
exports.completeReferral = completeReferral;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto_1 = require("crypto");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const notify_1 = require("./lib/notify");
const app_config_1 = require("./app-config");
const referral_posts_1 = require("./lib/referral-posts");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
const REFERRAL_EXPIRY_MS = 30 * DAY_MS;
const MAX_REFERRALS_PER_HOUR = 10;
const MAX_WITHDRAWALS_PER_DAY = 5;
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
/** Job-post credits an employer earns at these successful-referral counts. */
const EMPLOYER_CREDIT_MILESTONES = { 5: 5, 10: 10, 25: 25 };
const walletRef = (uid) => db.collection(schema_1.Wallets.COLLECTION).doc(uid);
exports.walletRef = walletRef;
const ledgerRef = (eventId) => db.collection(schema_1.WalletLedger.COLLECTION).doc(eventId);
exports.ledgerRef = ledgerRef;
async function readWallet(tx, uid) {
    const snap = await tx.get((0, exports.walletRef)(uid));
    const data = snap.data() || {};
    return { ref: snap.ref, balancePaise: Number(data[schema_1.Wallets.BALANCE_PAISE] || 0), data };
}
/**
 * Appends one ledger entry and moves the wallet balance by the same amount. The caller must have
 * checked (inside the same transaction) that the ledger id does not exist yet.
 */
function postEntry(tx, uid, wallet, eventId, type, amountPaise, refId) {
    const after = wallet.balancePaise + amountPaise;
    tx.create((0, exports.ledgerRef)(eventId), {
        [schema_1.WalletLedger.UID]: uid,
        [schema_1.WalletLedger.TYPE]: type,
        [schema_1.WalletLedger.AMOUNT_PAISE]: amountPaise,
        [schema_1.WalletLedger.REF_ID]: refId,
        [schema_1.WalletLedger.BALANCE_AFTER_PAISE]: after,
        [schema_1.WalletLedger.CREATED_AT]: Timestamp.now(),
    });
    const update = {
        [schema_1.Wallets.BALANCE_PAISE]: after,
        [schema_1.Wallets.UPDATED_AT]: Timestamp.now(),
    };
    if (amountPaise > 0 && type !== schema_1.Values.LedgerType.REFUND && type !== schema_1.Values.LedgerType.WALLET_DEPOSIT) {
        update[schema_1.Wallets.LIFETIME_EARNED_PAISE] = FieldValue.increment(amountPaise);
    }
    if (type === schema_1.Values.LedgerType.WITHDRAWAL)
        update[schema_1.Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
    if (type === schema_1.Values.LedgerType.REFUND)
        update[schema_1.Wallets.WITHDRAWN_PAISE] = FieldValue.increment(-amountPaise);
    tx.set(wallet.ref, update, { merge: true });
    wallet.balancePaise = after;
}
// ───────────────────────────── wallet + code ─────────────────────────────
function generateNameCode(rawName) {
    const clean = (rawName || "").replace(/[^A-Za-z]/g, "").toUpperCase();
    const isGeneric = !clean || clean === "USER" || clean.startsWith("USER") || clean === "DUTY" || clean === "TEST" || clean === "ADMIN";
    const prefix = !isGeneric && clean.length >= 3 ? clean.slice(0, 5) : "DUTY";
    const num = (0, crypto_1.randomInt)(1000, 10000); // 1000 - 9999
    return `${prefix}${num}`;
}
/** Creates the wallet and the user's own referral code if missing, or upgrades generic codes to real name. Returns the code. */
async function ensureWallet(uid, role = schema_1.Values.Role.EMPLOYER, preferredName, forceRefreshName) {
    const existing = await (0, exports.walletRef)(uid).get();
    const currentCode = String(existing.get(schema_1.Wallets.REFERRAL_CODE) || "");
    let baseName = (preferredName === null || preferredName === void 0 ? void 0 : preferredName.trim()) || "";
    if (!baseName) {
        const [wProf, eProf, uDoc] = await Promise.all([
            db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get(),
            db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get(),
            db.collection("users").doc(uid).get(),
        ]);
        baseName = String(wProf.get("name") ||
            eProf.get("ownerName") ||
            eProf.get("businessName") ||
            uDoc.get("name") ||
            uDoc.get("displayName") ||
            "");
        if (!baseName) {
            try {
                const authUser = await admin.auth().getUser(uid);
                baseName = authUser.displayName || "";
            }
            catch (_a) { }
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
                const profileRef = role === schema_1.Values.Role.EMPLOYER
                    ? db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid)
                    : db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid);
                const [wallet, codeDoc, profileSnap] = await Promise.all([
                    tx.get((0, exports.walletRef)(uid)),
                    tx.get(db.collection(schema_1.ReferralCodes.COLLECTION).doc(candidate)),
                    tx.get(profileRef)
                ]);
                const currentInTx = String(wallet.get(schema_1.Wallets.REFERRAL_CODE) || "");
                if (currentInTx && !currentInTx.startsWith("USER") && !currentInTx.startsWith("DUTY") && !forceRefreshName) {
                    return currentInTx;
                }
                if (codeDoc.exists)
                    throw new Error("CODE_TAKEN");
                const phone = profileSnap.exists ? String(profileSnap.get("phone") || "") : "";
                const name = profileSnap.exists
                    ? String(profileSnap.get(role === schema_1.Values.Role.EMPLOYER ? "ownerName" : "name") || profileSnap.get("businessName") || "")
                    : baseName;
                tx.create(codeDoc.ref, {
                    [schema_1.ReferralCodes.UID]: uid,
                    [schema_1.ReferralCodes.ROLE]: role,
                    [schema_1.ReferralCodes.ACTIVE]: true,
                    phone,
                    name: name || "DutyPe Member",
                    createdAt: Timestamp.now(),
                });
                // If there was an old code, update its name and keep active as alias
                if (currentInTx && currentInTx !== candidate) {
                    const oldCodeRef = db.collection(schema_1.ReferralCodes.COLLECTION).doc(currentInTx);
                    tx.set(oldCodeRef, {
                        name: name || "DutyPe Member",
                        aliasFor: candidate,
                        updatedAt: Timestamp.now()
                    }, { merge: true });
                }
                tx.set(wallet.ref, {
                    [schema_1.Wallets.REFERRAL_CODE]: candidate,
                    [schema_1.Wallets.BALANCE_PAISE]: Number(wallet.get(schema_1.Wallets.BALANCE_PAISE) || 0),
                    [schema_1.Wallets.LIFETIME_EARNED_PAISE]: Number(wallet.get(schema_1.Wallets.LIFETIME_EARNED_PAISE) || 0),
                    [schema_1.Wallets.WITHDRAWN_PAISE]: Number(wallet.get(schema_1.Wallets.WITHDRAWN_PAISE) || 0),
                    [schema_1.Wallets.SUCCESSFUL_REFERRALS]: Number(wallet.get(schema_1.Wallets.SUCCESSFUL_REFERRALS) || 0),
                    [schema_1.Wallets.AWARDED_MILESTONES]: wallet.get(schema_1.Wallets.AWARDED_MILESTONES) || [],
                    [schema_1.Wallets.BLOCKED]: wallet.get(schema_1.Wallets.BLOCKED) === true,
                    [schema_1.Wallets.UPDATED_AT]: Timestamp.now(),
                }, { merge: true });
                // Sync to authoritative users collection
                tx.set(db.collection("users").doc(uid), Object.assign(Object.assign({ referralCode: candidate }, (name ? { name } : {})), { updatedAt: Timestamp.now() }), { merge: true });
                return candidate;
            });
        }
        catch (error) {
            if (error.message !== "CODE_TAKEN")
                throw error;
        }
    }
    throw new functions.https.HttpsError("internal", "Could not create a referral code");
}
/** The app asks for its own code (older accounts get one on first call, upgraded with preferredName). */
exports.ensureWalletCallable = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const preferredName = (0, input_1.str)(data, "name", { min: 0, max: 100, optional: true });
    let role = String(context.auth.token.role || "");
    if (!role) {
        const userDoc = await db.collection("users").doc(uid).get();
        role = String(userDoc.get("role") || "");
    }
    const code = await ensureWallet(uid, role === schema_1.Values.Role.EMPLOYER ? role : schema_1.Values.Role.WORKER, preferredName);
    return { referralCode: code };
});
/** Explicitly refresh/upgrade the referral code with the user's real verified name. */
exports.refreshReferralCode = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const name = (0, input_1.str)(data, "name", { min: 2, max: 100 });
    let role = String(context.auth.token.role || "");
    if (!role) {
        const userDoc = await db.collection("users").doc(uid).get();
        role = String(userDoc.get("role") || schema_1.Values.Role.WORKER);
    }
    const code = await ensureWallet(uid, role === schema_1.Values.Role.EMPLOYER ? role : schema_1.Values.Role.WORKER, name, true);
    return { referralCode: code };
});
// ───────────────────────────── referrals ─────────────────────────────
function normalizeCode(raw) {
    if (typeof raw !== "string" && typeof raw !== "number")
        return "";
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
async function resolveReferralCodeDoc(code) {
    // 1. Direct uppercase lookup
    let snap = await db.collection(schema_1.ReferralCodes.COLLECTION).doc(code).get();
    if (snap.exists)
        return snap;
    // 2. Direct lowercase lookup
    snap = await db.collection(schema_1.ReferralCodes.COLLECTION).doc(code.toLowerCase()).get();
    if (snap.exists)
        return snap;
    // 3. Search by field in referral_codes (in case document ID was auto-generated or UID)
    const qSnap = await db.collection(schema_1.ReferralCodes.COLLECTION)
        .where("referralCode", "in", [code, code.toUpperCase(), code.toLowerCase()])
        .limit(1)
        .get();
    if (!qSnap.empty)
        return qSnap.docs[0];
    // 4. Search in referral_stats (Wallets collection)
    const wSnap = await db.collection(schema_1.Wallets.COLLECTION)
        .where(schema_1.Wallets.REFERRAL_CODE, "in", [code, code.toUpperCase(), code.toLowerCase()])
        .limit(1)
        .get();
    if (!wSnap.empty) {
        const wDoc = wSnap.docs[0];
        const referrerUid = wDoc.id;
        const [wProf, eProf, uDoc] = await Promise.all([
            db.collection(schema_1.WorkerProfiles.COLLECTION).doc(referrerUid).get(),
            db.collection(schema_1.EmployerProfiles.COLLECTION).doc(referrerUid).get(),
            db.collection("users").doc(referrerUid).get(),
        ]);
        const name = String(wProf.get("name") ||
            eProf.get("ownerName") ||
            eProf.get("businessName") ||
            uDoc.get("name") ||
            uDoc.get("displayName") ||
            "DutyPe Member");
        const phone = String(wProf.get("phone") || eProf.get("phone") || uDoc.get("phone") || "");
        const role = eProf.exists ? schema_1.Values.Role.EMPLOYER : schema_1.Values.Role.WORKER;
        const refDoc = db.collection(schema_1.ReferralCodes.COLLECTION).doc(code);
        await refDoc.set({
            [schema_1.ReferralCodes.UID]: referrerUid,
            [schema_1.ReferralCodes.ROLE]: role,
            [schema_1.ReferralCodes.ACTIVE]: true,
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
        const role = String(uDoc.get("role") || schema_1.Values.Role.WORKER) === schema_1.Values.Role.EMPLOYER
            ? schema_1.Values.Role.EMPLOYER
            : schema_1.Values.Role.WORKER;
        const refDoc = db.collection(schema_1.ReferralCodes.COLLECTION).doc(code);
        await refDoc.set({
            [schema_1.ReferralCodes.UID]: referrerUid,
            [schema_1.ReferralCodes.ROLE]: role,
            [schema_1.ReferralCodes.ACTIVE]: true,
            phone,
            name,
            createdAt: Timestamp.now(),
        }, { merge: true });
        return await refDoc.get();
    }
    return null;
}
async function registerReferral(uid, rawCode, providedPhone, providedName) {
    const code = normalizeCode(rawCode);
    if (code.length < 3 || code.length > 20)
        return "Invalid referral code format";
    try {
        const codeDoc = await resolveReferralCodeDoc(code);
        if (!codeDoc || !codeDoc.exists)
            return "Referral code not found";
        if (codeDoc.get(schema_1.ReferralCodes.ACTIVE) !== true)
            return "This referral code is disabled";
        const referrerUid = String(codeDoc.get(schema_1.ReferralCodes.UID) || "");
        if (!referrerUid)
            return "Referral code has no active owner";
        if (referrerUid === uid)
            return "You cannot use your own referral code";
        const hourAgo = Timestamp.fromMillis(Date.now() - 60 * 60 * 1000);
        const recent = await db.collection(schema_1.Referrals.COLLECTION)
            .where(schema_1.Referrals.REFERRER_UID, "==", referrerUid)
            .where(schema_1.Referrals.CREATED_AT, ">=", hourAgo)
            .count().get();
        const fraudScore = recent.data().count >= MAX_REFERRALS_PER_HOUR ? 60 : 0;
        const config = await (0, app_config_1.getReferralConfig)();
        // Fetch referee details (name and phone) to store in referral record
        let refereePhone = (providedPhone || "").trim();
        let refereeName = (providedName || "").trim();
        if (!refereePhone || !refereeName) {
            const [wProf, eProf, uDoc] = await Promise.all([
                db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get(),
                db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get(),
                db.collection("users").doc(uid).get()
            ]);
            if (!refereePhone) {
                refereePhone = String(wProf.get("phone") || eProf.get("phone") || uDoc.get("phone") || "");
            }
            if (!refereeName) {
                refereeName = String(wProf.get("name") ||
                    eProf.get("ownerName") ||
                    eProf.get("businessName") ||
                    uDoc.get("name") ||
                    uDoc.get("displayName") ||
                    "");
            }
            if (!refereePhone || !refereeName) {
                try {
                    const authUser = await admin.auth().getUser(uid);
                    if (!refereePhone)
                        refereePhone = authUser.phoneNumber || "";
                    if (!refereeName)
                        refereeName = authUser.displayName || "";
                }
                catch (_a) { }
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
            var _a;
            const ref = db.collection(schema_1.Referrals.COLLECTION).doc(uid);
            const [existing, referrerWallet, refereeWallet, referrerRefDoc, bonusEntry, rewardEntry] = await Promise.all([
                tx.get(ref),
                readWallet(tx, referrerUid),
                readWallet(tx, uid),
                tx.get(db.collection(schema_1.Referrals.COLLECTION).doc(referrerUid)),
                tx.get((0, exports.ledgerRef)(bonusEventId)),
                tx.get((0, exports.ledgerRef)(rewardEventId)),
            ]);
            if (existing.exists) {
                if (existing.get(schema_1.Referrals.CODE) === code) {
                    // Idempotent success: Already linked to this exact referral code
                    return null;
                }
                return "A referral code was already used for this account";
            }
            if (referrerWallet.data[schema_1.Wallets.BLOCKED] === true)
                return "This referral code belongs to an inactive account";
            // Circular referral guard: Prevent A -> B -> A loops
            if (referrerRefDoc.exists && referrerRefDoc.get(schema_1.Referrals.REFERRER_UID) === uid) {
                return "Circular referrals are not permitted";
            }
            const now = Date.now();
            const isClean = fraudScore === 0;
            // Ensure referee wallet structure exists
            if (!refereeWallet.data || Object.keys(refereeWallet.data).length === 0) {
                tx.set(refereeWallet.ref, {
                    [schema_1.Wallets.BALANCE_PAISE]: 0,
                    [schema_1.Wallets.LIFETIME_EARNED_PAISE]: 0,
                    [schema_1.Wallets.WITHDRAWN_PAISE]: 0,
                    [schema_1.Wallets.SUCCESSFUL_REFERRALS]: 0,
                    [schema_1.Wallets.AWARDED_MILESTONES]: [],
                    [schema_1.Wallets.BLOCKED]: false,
                    [schema_1.Wallets.UPDATED_AT]: Timestamp.now(),
                }, { merge: true });
            }
            tx.create(ref, {
                [schema_1.Referrals.REFERRER_UID]: referrerUid,
                [schema_1.Referrals.CODE]: code,
                [schema_1.Referrals.STATUS]: isClean ? schema_1.Values.ReferralStatus.COMPLETED : schema_1.Values.ReferralStatus.REJECTED,
                [schema_1.Referrals.FRAUD_SCORE]: fraudScore,
                refereeName: refereeName || "DutyPe Friend",
                refereePhone,
                referrerName: String(codeDoc.get("name") || "DutyPe Member"),
                [schema_1.Referrals.CREATED_AT]: Timestamp.fromMillis(now),
                [schema_1.Referrals.COMPLETED_AT]: isClean ? Timestamp.fromMillis(now) : null,
                [schema_1.Referrals.EXPIRES_AT]: Timestamp.fromMillis(now + REFERRAL_EXPIRY_MS),
            });
            if (isClean) {
                // 1. Instant credit: referee receives their signup bonus immediately upon entering code
                const bonusPaise = Math.round(config.signupBonus * 100);
                if (!bonusEntry.exists && bonusPaise > 0) {
                    postEntry(tx, uid, refereeWallet, bonusEventId, schema_1.Values.LedgerType.SIGNUP_BONUS, bonusPaise, code);
                }
                // 2. Instant credit: referrer receives their referral reward immediately upon friend joining
                const rewardPaise = Math.round(config.rewardPerReferral * 100);
                if (!rewardEntry.exists && rewardPaise > 0) {
                    postEntry(tx, referrerUid, referrerWallet, rewardEventId, schema_1.Values.LedgerType.REFERRAL_REWARD, rewardPaise, uid);
                }
                // Increment successful referrals count on referrer wallet
                const successful = Number(referrerWallet.data[schema_1.Wallets.SUCCESSFUL_REFERRALS] || 0) + 1;
                const awarded = Array.isArray(referrerWallet.data[schema_1.Wallets.AWARDED_MILESTONES]) ?
                    referrerWallet.data[schema_1.Wallets.AWARDED_MILESTONES].map(Number) : [];
                const milestoneRupees = Number(((_a = config.milestones) === null || _a === void 0 ? void 0 : _a[String(successful)]) || 0);
                if (milestoneRupees > 0 && !awarded.includes(successful)) {
                    const milestonePaise = Math.round(milestoneRupees * 100);
                    postEntry(tx, referrerUid, referrerWallet, `milestone_${referrerUid}_${successful}`, schema_1.Values.LedgerType.MILESTONE, milestonePaise, String(successful));
                    awarded.push(successful);
                }
                tx.set(referrerWallet.ref, {
                    [schema_1.Wallets.SUCCESSFUL_REFERRALS]: successful,
                    [schema_1.Wallets.AWARDED_MILESTONES]: awarded,
                    [schema_1.Wallets.UPDATED_AT]: Timestamp.now()
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
            (0, notify_1.notify)(uid, {
                type: "SIGNUP_BONUS",
                templateId: "SIGNUP_BONUS",
                params: { amount: config.signupBonus }
            }).catch(() => { });
            // Notify referrer of new friend joining with friend's name
            (0, notify_1.notify)(referrerUid, {
                type: "REFERRAL",
                templateId: "REFERRAL_REWARD_BASIC",
                params: { name: refereeName || "Friend", amount: config.rewardPerReferral }
            }).catch(() => { });
        }
        return txResult;
    }
    catch (error) {
        functions.logger.warn(`registerReferral(${uid}) failed`, error);
        return "Referral could not be applied. Please try again.";
    }
}
/** A code entered after sign-up (profile setup or Refer & Earn screen). One referral per user. */
exports.applyReferralCode = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    const uid = context.auth.uid;
    // Rate limiting: Max 5 code attempts per 10 minutes to prevent dictionary guessing
    const attemptRef = db.collection("referral_attempts").doc(uid);
    const attemptSnap = await attemptRef.get();
    const nowMs = Date.now();
    const attemptData = attemptSnap.data() || {};
    const windowStart = Number(attemptData.windowStart || 0);
    const count = Number(attemptData.count || 0);
    if (nowMs - windowStart < 10 * 60 * 1000) {
        if (count >= 5) {
            (0, input_1.fail)("resource-exhausted", "Too many referral code attempts. Please wait 10 minutes before trying again.");
        }
        await attemptRef.set({ count: count + 1, windowStart, lastAttemptAt: Timestamp.now() }, { merge: true });
    }
    else {
        await attemptRef.set({ count: 1, windowStart: nowMs, lastAttemptAt: Timestamp.now() }, { merge: true });
    }
    const data = (0, input_1.obj)(raw);
    const rawCode = (0, input_1.str)(data, "code", { min: 3, max: 30 });
    const code = normalizeCode(rawCode);
    const phone = (0, input_1.str)(data, "phone", { optional: true, max: 20 });
    const name = (0, input_1.str)(data, "name", { optional: true, max: 100 });
    const error = await registerReferral(uid, code, phone, name);
    if (error)
        (0, input_1.fail)("invalid-argument", error);
    // Reset attempt count upon successful application
    await attemptRef.delete().catch(() => { });
    const codeDoc = await resolveReferralCodeDoc(code);
    return {
        ok: true,
        referrerName: String((codeDoc === null || codeDoc === void 0 ? void 0 : codeDoc.get("name")) || "DutyPe Member"),
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
async function completeReferral(refereeUid, refereeRole) {
    const refDoc = db.collection(schema_1.Referrals.COLLECTION).doc(refereeUid);
    const peek = await refDoc.get();
    if (!peek.exists || peek.get(schema_1.Referrals.STATUS) !== schema_1.Values.ReferralStatus.PENDING)
        return;
    const config = await (0, app_config_1.getReferralConfig)();
    const paid = await db.runTransaction(async (tx) => {
        var _a, _b, _c;
        const referral = await tx.get(refDoc);
        if (referral.get(schema_1.Referrals.STATUS) !== schema_1.Values.ReferralStatus.PENDING)
            return null;
        const referrerUid = String(referral.get(schema_1.Referrals.REFERRER_UID));
        const expiresAt = (_b = (_a = referral.get(schema_1.Referrals.EXPIRES_AT)) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        const refCode = String(referral.get(schema_1.Referrals.CODE));
        let referrerCodeSnap = await tx.get(db.collection(schema_1.ReferralCodes.COLLECTION).doc(refCode));
        if (!referrerCodeSnap.exists) {
            referrerCodeSnap = await tx.get(db.collection(schema_1.ReferralCodes.COLLECTION).doc(refCode.toLowerCase()));
        }
        const isEmployer = (await tx.get(db.collection(schema_1.EmployerProfiles.COLLECTION).doc(referrerUid))).exists;
        const referrerRole = String((referrerCodeSnap.exists ? referrerCodeSnap.get(schema_1.ReferralCodes.ROLE) : null) ||
            (isEmployer ? schema_1.Values.Role.EMPLOYER : schema_1.Values.Role.WORKER));
        const rewardId = `referral_${refereeUid}_referrer`;
        const bonusId = `referral_${refereeUid}_referee`;
        const [referrer, referee, rewardEntry, bonusEntry] = await Promise.all([
            readWallet(tx, referrerUid), readWallet(tx, refereeUid), tx.get((0, exports.ledgerRef)(rewardId)), tx.get((0, exports.ledgerRef)(bonusId)),
        ]);
        const employerRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(referrerUid);
        const employerSnap = referrerRole === schema_1.Values.Role.EMPLOYER ? await tx.get(employerRef) : null;
        if (expiresAt > 0 && Date.now() > expiresAt) {
            tx.update(refDoc, { [schema_1.Referrals.STATUS]: schema_1.Values.ReferralStatus.EXPIRED });
            return null;
        }
        if (referrer.data[schema_1.Wallets.BLOCKED] === true) {
            tx.update(refDoc, { [schema_1.Referrals.STATUS]: schema_1.Values.ReferralStatus.REJECTED });
            return null;
        }
        const successful = Number(referrer.data[schema_1.Wallets.SUCCESSFUL_REFERRALS] || 0) + 1;
        const awarded = Array.isArray(referrer.data[schema_1.Wallets.AWARDED_MILESTONES]) ?
            referrer.data[schema_1.Wallets.AWARDED_MILESTONES].map(Number) : [];
        tx.update(refDoc, { [schema_1.Referrals.STATUS]: schema_1.Values.ReferralStatus.COMPLETED, [schema_1.Referrals.COMPLETED_AT]: Timestamp.now() });
        tx.set(referrer.ref, { [schema_1.Wallets.SUCCESSFUL_REFERRALS]: successful }, { merge: true });
        let referrerPaise = 0;
        if (!rewardEntry.exists) {
            referrerPaise = Math.round(config.rewardPerReferral * 100);
            postEntry(tx, referrerUid, referrer, rewardId, schema_1.Values.LedgerType.REFERRAL_REWARD, referrerPaise, refereeUid);
        }
        const milestoneRupees = Number(((_c = config.milestones) === null || _c === void 0 ? void 0 : _c[String(successful)]) || 0);
        if (milestoneRupees > 0 && !awarded.includes(successful)) {
            const milestonePaise = Math.round(milestoneRupees * 100);
            postEntry(tx, referrerUid, referrer, `milestone_${referrerUid}_${successful}`, schema_1.Values.LedgerType.MILESTONE, milestonePaise, String(successful));
            tx.set(referrer.ref, { [schema_1.Wallets.AWARDED_MILESTONES]: FieldValue.arrayUnion(successful) }, { merge: true });
            referrerPaise += milestonePaise;
        }
        if (referrerRole === schema_1.Values.Role.EMPLOYER && EMPLOYER_CREDIT_MILESTONES[successful]) {
            const S = schema_1.EmployerProfiles.Subscription;
            tx.set(employerRef, {
                [schema_1.EmployerProfiles.SUBSCRIPTION]: {
                    [S.CREDITS]: { [S.CREDITS_NORMAL]: FieldValue.increment(EMPLOYER_CREDIT_MILESTONES[successful]) },
                },
            }, { merge: true });
        }
        // Every 2 successful referrals: grant one free job post (normal or urgent) valid for 30 days.
        let freePost = false;
        if ((employerSnap === null || employerSnap === void 0 ? void 0 : employerSnap.exists) && successful % 2 === 0) {
            const nowMs = Date.now();
            tx.update(employerRef, {
                [schema_1.EmployerProfiles.REFERRAL_FREE_POSTS]: (0, referral_posts_1.referralPostsLeft)(employerSnap.data(), nowMs) + 1,
                [schema_1.EmployerProfiles.REFERRAL_FREE_POSTS_UNTIL]: Timestamp.fromMillis(nowMs + referral_posts_1.REFERRAL_POST_VALID_MS),
            });
            freePost = true;
        }
        let refereePaise = 0;
        if (refereeRole === schema_1.Values.Role.WORKER && !bonusEntry.exists && config.signupBonus > 0) {
            refereePaise = Math.round(config.signupBonus * 100);
            postEntry(tx, refereeUid, referee, bonusId, schema_1.Values.LedgerType.SIGNUP_BONUS, refereePaise, referrerUid);
        }
        return { referrerUid, referrerRole, referrerPaise, refereePaise, freePost };
    });
    if (!paid)
        return;
    if (paid.referrerPaise > 0) {
        await (0, notify_1.notify)(paid.referrerUid, {
            type: "PAYMENT", templateId: "REFERRAL_REWARD_BASIC", role: paid.referrerRole,
            params: { amount: paid.referrerPaise / 100 }, data: { refId: refereeUid },
        });
    }
    if (paid.freePost) {
        await (0, notify_1.notify)(paid.referrerUid, {
            type: "PAYMENT", templateId: "REFERRAL_FREE_POST", role: schema_1.Values.Role.EMPLOYER,
            params: {}, data: { action: "post_job" },
        });
    }
    if (paid.refereePaise > 0) {
        await (0, notify_1.notify)(refereeUid, {
            type: "PAYMENT", templateId: "SIGNUP_BONUS", role: schema_1.Values.Role.WORKER,
            params: { amount: paid.refereePaise / 100 }, data: {},
        });
    }
}
// ───────────────────────────── withdrawals ─────────────────────────────
const UPI_PATTERN = /^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.-]{1,64}$/;
/** Cash out the wallet to UPI (whole balance, capped per day). requestId makes a retry harmless. */
exports.requestWithdrawal = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const upiId = (0, input_1.str)(data, "upiId", { min: 5, max: 100, pattern: UPI_PATTERN });
    const reqId = (0, input_1.str)(data, "requestId", { min: 8, max: 80, pattern: /^[A-Za-z0-9_-]+$/ });
    const config = await (0, app_config_1.getReferralConfig)();
    const minPaise = Math.round(config.minWithdrawal * 100);
    const dailyCapPaise = Math.round(config.maxWithdrawalPerDay * 100);
    const withdrawalId = (0, crypto_1.createHash)("sha256").update(`${uid}:${reqId}`).digest("hex").slice(0, 32);
    const day = new Date(Date.now() + 330 * 60 * 1000).toISOString().slice(0, 10);
    const withdrawalRef = db.collection(schema_1.Withdrawals.COLLECTION).doc(withdrawalId);
    const dailyRef = db.collection(schema_1.WithdrawalDaily.COLLECTION).doc(`${uid}_${day}`);
    const burstRef = db.collection("withdrawal_burst_limits").doc(uid);
    const result = await db.runTransaction(async (tx) => {
        const [existing, wallet, daily, burst] = await Promise.all([
            tx.get(withdrawalRef),
            readWallet(tx, uid),
            tx.get(dailyRef),
            tx.get(burstRef)
        ]);
        if (existing.exists)
            return { withdrawalId, amountPaise: Number(existing.get(schema_1.Withdrawals.AMOUNT_PAISE)), replay: true };
        const lastRequestMs = Number(burst.get("lastRequestMs") || 0);
        if (Date.now() - lastRequestMs < 30 * 1000) {
            (0, input_1.fail)("resource-exhausted", "Please wait 30 seconds before submitting another withdrawal request.");
        }
        tx.set(burstRef, { lastRequestMs: Date.now() }, { merge: true });
        if (wallet.data[schema_1.Wallets.BLOCKED] === true)
            (0, input_1.fail)("permission-denied", "Your wallet is blocked. Contact support.");
        const dailyPaise = Number(daily.get(schema_1.WithdrawalDaily.AMOUNT_PAISE) || 0);
        const dailyCount = Number(daily.get(schema_1.WithdrawalDaily.REQUEST_COUNT) || 0);
        if (dailyCount >= MAX_WITHDRAWALS_PER_DAY)
            (0, input_1.fail)("resource-exhausted", "Daily withdrawal limit reached");
        // The whole balance, up to what is left of today's cap; the rest can be withdrawn tomorrow.
        const amountPaise = Math.min(wallet.balancePaise, dailyCapPaise - dailyPaise);
        if (wallet.balancePaise < minPaise)
            (0, input_1.fail)("failed-precondition", `You need at least ₹${config.minWithdrawal} to withdraw`);
        if (amountPaise < minPaise)
            (0, input_1.fail)("resource-exhausted", `You can withdraw up to ₹${config.maxWithdrawalPerDay} per day`);
        const now = Timestamp.now();
        tx.create(withdrawalRef, {
            [schema_1.Withdrawals.UID]: uid,
            [schema_1.Withdrawals.AMOUNT_PAISE]: amountPaise,
            [schema_1.Withdrawals.UPI_ID]: upiId,
            [schema_1.Withdrawals.STATUS]: schema_1.Values.WithdrawalStatus.PENDING,
            [schema_1.Withdrawals.CREATED_AT]: now,
        });
        postEntry(tx, uid, wallet, `withdrawal_${withdrawalId}`, schema_1.Values.LedgerType.WITHDRAWAL, -amountPaise, withdrawalId);
        tx.set(dailyRef, {
            [schema_1.WithdrawalDaily.AMOUNT_PAISE]: dailyPaise + amountPaise,
            [schema_1.WithdrawalDaily.REQUEST_COUNT]: dailyCount + 1,
            [schema_1.WithdrawalDaily.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 2 * DAY_MS),
        });
        return { withdrawalId, amountPaise, replay: false };
    });
    return result;
});
/**
 * Admin settles a withdrawal: COMPLETED (with the payout reference) or FAILED (money goes back to
 * the wallet as a REFUND entry). A settled withdrawal can never change again.
 */
exports.settleWithdrawal = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const withdrawalId = (0, input_1.str)(data, "withdrawalId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    const status = String(data.status || "").toUpperCase();
    if (status !== schema_1.Values.WithdrawalStatus.COMPLETED && status !== schema_1.Values.WithdrawalStatus.FAILED &&
        status !== schema_1.Values.WithdrawalStatus.PROCESSING)
        (0, input_1.fail)("invalid-argument", "status is invalid");
    const txnRef = (0, input_1.str)(data, "txnRef", { max: 100, optional: true });
    const failureReason = (0, input_1.str)(data, "failureReason", { max: 300, optional: true });
    if (status === schema_1.Values.WithdrawalStatus.COMPLETED && !txnRef)
        (0, input_1.fail)("invalid-argument", "txnRef is required");
    const ref = db.collection(schema_1.Withdrawals.COLLECTION).doc(withdrawalId);
    const outcome = await db.runTransaction(async (tx) => {
        const w = await tx.get(ref);
        if (!w.exists)
            (0, input_1.fail)("not-found", "Withdrawal not found");
        const current = String(w.get(schema_1.Withdrawals.STATUS));
        if (current === schema_1.Values.WithdrawalStatus.COMPLETED || current === schema_1.Values.WithdrawalStatus.FAILED) {
            (0, input_1.fail)("failed-precondition", `Withdrawal is already ${current}`);
        }
        const uid = String(w.get(schema_1.Withdrawals.UID));
        const amountPaise = Number(w.get(schema_1.Withdrawals.AMOUNT_PAISE));
        const refundId = `refund_${withdrawalId}`;
        const [wallet, refund] = await Promise.all([readWallet(tx, uid), tx.get((0, exports.ledgerRef)(refundId))]);
        tx.update(ref, Object.assign(Object.assign(Object.assign({ [schema_1.Withdrawals.STATUS]: status }, (txnRef ? { [schema_1.Withdrawals.TXN_REF]: txnRef } : {})), (failureReason ? { [schema_1.Withdrawals.FAILURE_REASON]: failureReason } : {})), (status !== schema_1.Values.WithdrawalStatus.PROCESSING ? { [schema_1.Withdrawals.PROCESSED_AT]: Timestamp.now() } : {})));
        if (status === schema_1.Values.WithdrawalStatus.FAILED && !refund.exists) {
            postEntry(tx, uid, wallet, refundId, schema_1.Values.LedgerType.REFUND, amountPaise, withdrawalId);
        }
        return { uid, amountPaise };
    });
    if (status !== schema_1.Values.WithdrawalStatus.PROCESSING) {
        await (0, notify_1.notify)(outcome.uid, {
            type: "PAYMENT",
            templateId: status === schema_1.Values.WithdrawalStatus.COMPLETED ? "WITHDRAWAL_COMPLETED" : "WITHDRAWAL_FAILED",
            params: { amount: outcome.amountPaise / 100 },
            data: { withdrawalId, status },
        });
    }
    return { withdrawalId, status };
});
// ───────────────────────────── leaderboard + expiry ─────────────────────────────
let leaderboardCache = null;
exports.getReferralLeaderboard = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async () => {
    if (leaderboardCache && Date.now() - leaderboardCache.at < 10 * 60 * 1000)
        return { leaders: leaderboardCache.rows };
    const top = await db.collection(schema_1.Wallets.COLLECTION)
        .where(schema_1.Wallets.BLOCKED, "==", false)
        .orderBy(schema_1.Wallets.SUCCESSFUL_REFERRALS, "desc")
        .limit(20).get();
    const profiles = top.empty ? [] : await db.getAll(...top.docs.map((d) => db.collection(schema_1.WorkerProfiles.COLLECTION).doc(d.id)));
    const rows = top.docs.map((d, i) => {
        var _a;
        const full = String(((_a = profiles[i]) === null || _a === void 0 ? void 0 : _a.get(schema_1.WorkerProfiles.NAME)) || "DutyPe user").trim().split(/\s+/);
        return {
            rank: i + 1,
            name: full.length > 1 ? `${full[0]} ${full[full.length - 1][0]}.` : full[0],
            successfulReferrals: Number(d.get(schema_1.Wallets.SUCCESSFUL_REFERRALS) || 0),
        };
    });
    leaderboardCache = { at: Date.now(), rows };
    return { leaders: rows };
});
exports.expirePendingReferrals = functions
    .region("asia-south1")
    .pubsub.schedule("every 24 hours")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const snap = await db.collection(schema_1.Referrals.COLLECTION)
        .where(schema_1.Referrals.STATUS, "==", schema_1.Values.ReferralStatus.PENDING)
        .where(schema_1.Referrals.EXPIRES_AT, "<=", Timestamp.now())
        .limit(500).get();
    if (snap.empty)
        return null;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.update(d.ref, { [schema_1.Referrals.STATUS]: schema_1.Values.ReferralStatus.EXPIRED }));
    await batch.commit();
    return null;
});
//# sourceMappingURL=referrals.js.map