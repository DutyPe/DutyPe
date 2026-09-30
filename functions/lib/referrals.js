"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.expirePendingReferrals = exports.getReferralLeaderboard = exports.settleWithdrawal = exports.requestWithdrawal = exports.completeReferral = exports.isEmployerProfileComplete = exports.isWorkerProfileComplete = exports.applyReferralCode = exports.registerReferral = exports.ensureWalletCallable = exports.ensureWallet = void 0;
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
const ledgerRef = (eventId) => db.collection(schema_1.WalletLedger.COLLECTION).doc(eventId);
async function readWallet(tx, uid) {
    const snap = await tx.get(walletRef(uid));
    const data = snap.data() || {};
    return { ref: snap.ref, balancePaise: Number(data[schema_1.Wallets.BALANCE_PAISE] || 0), data };
}
/**
 * Appends one ledger entry and moves the wallet balance by the same amount. The caller must have
 * checked (inside the same transaction) that the ledger id does not exist yet.
 */
function postEntry(tx, uid, wallet, eventId, type, amountPaise, refId) {
    const after = wallet.balancePaise + amountPaise;
    tx.create(ledgerRef(eventId), {
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
    if (amountPaise > 0 && type !== schema_1.Values.LedgerType.REFUND) {
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
function randomCode() {
    return Array.from({ length: 8 }, () => CODE_ALPHABET[(0, crypto_1.randomInt)(CODE_ALPHABET.length)]).join("");
}
/** Creates the wallet and the user's own referral code if missing. Returns the code. */
async function ensureWallet(uid, role) {
    const existing = await walletRef(uid).get();
    const code = String(existing.get(schema_1.Wallets.REFERRAL_CODE) || "");
    if (code)
        return code;
    for (let attempt = 0; attempt < 8; attempt++) {
        const candidate = randomCode();
        try {
            return await db.runTransaction(async (tx) => {
                const [wallet, codeDoc] = await Promise.all([
                    tx.get(walletRef(uid)), tx.get(db.collection(schema_1.ReferralCodes.COLLECTION).doc(candidate)),
                ]);
                const current = String(wallet.get(schema_1.Wallets.REFERRAL_CODE) || "");
                if (current)
                    return current;
                if (codeDoc.exists)
                    throw new Error("CODE_TAKEN");
                tx.create(codeDoc.ref, {
                    [schema_1.ReferralCodes.UID]: uid,
                    [schema_1.ReferralCodes.ROLE]: role,
                    [schema_1.ReferralCodes.ACTIVE]: true,
                });
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
exports.ensureWallet = ensureWallet;
/** The app asks for its own code (older accounts get one on first call). */
exports.ensureWalletCallable = (0, secure_callable_1.onCallSecured)({}, async (_raw, context) => {
    const uid = context.auth.uid;
    const role = String(context.auth.token.role || "");
    const code = await ensureWallet(uid, role === schema_1.Values.Role.EMPLOYER ? role : schema_1.Values.Role.WORKER);
    return { referralCode: code };
});
// ───────────────────────────── referrals ─────────────────────────────
function normalizeCode(raw) {
    return String(raw !== null && raw !== void 0 ? raw : "").toUpperCase().replace(/[^A-Z0-9]/g, "");
}
/**
 * Records that [uid] joined with [rawCode]. Never throws: a bad code must not fail registration.
 * The referral stays PENDING until the new user's profile is complete.
 */
const LATE_REFERRAL_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
async function registerReferral(uid, rawCode) {
    const code = normalizeCode(rawCode);
    if (!/^[A-Z0-9]{6,10}$/.test(code))
        return "Invalid referral code";
    try {
        const codeDoc = await db.collection(schema_1.ReferralCodes.COLLECTION).doc(code).get();
        const referrerUid = String(codeDoc.get(schema_1.ReferralCodes.UID) || "");
        if (!codeDoc.exists || codeDoc.get(schema_1.ReferralCodes.ACTIVE) !== true || !referrerUid)
            return "Referral code not found";
        if (referrerUid === uid)
            return "You cannot use your own code";
        const hourAgo = Timestamp.fromMillis(Date.now() - 60 * 60 * 1000);
        const recent = await db.collection(schema_1.Referrals.COLLECTION)
            .where(schema_1.Referrals.REFERRER_UID, "==", referrerUid)
            .where(schema_1.Referrals.CREATED_AT, ">=", hourAgo)
            .count().get();
        const fraudScore = recent.data().count >= MAX_REFERRALS_PER_HOUR ? 60 : 0;
        return await db.runTransaction(async (tx) => {
            const ref = db.collection(schema_1.Referrals.COLLECTION).doc(uid);
            const [existing, referrerWallet] = await Promise.all([tx.get(ref), tx.get(walletRef(referrerUid))]);
            if (existing.exists)
                return "A referral code was already used";
            if (referrerWallet.get(schema_1.Wallets.BLOCKED) === true)
                return "This referral code is not active";
            const now = Date.now();
            tx.create(ref, {
                [schema_1.Referrals.REFERRER_UID]: referrerUid,
                [schema_1.Referrals.CODE]: code,
                [schema_1.Referrals.STATUS]: fraudScore > 0 ? schema_1.Values.ReferralStatus.REJECTED : schema_1.Values.ReferralStatus.PENDING,
                [schema_1.Referrals.FRAUD_SCORE]: fraudScore,
                [schema_1.Referrals.CREATED_AT]: Timestamp.fromMillis(now),
                [schema_1.Referrals.EXPIRES_AT]: Timestamp.fromMillis(now + REFERRAL_EXPIRY_MS),
            });
            return null;
        });
    }
    catch (error) {
        functions.logger.warn(`registerReferral(${uid}) failed`, error);
        return "Referral could not be applied";
    }
}
exports.registerReferral = registerReferral;
/** A code entered after sign-up (profile setup). Allowed for 7 days after the account was created. */
exports.applyReferralCode = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const role = String(context.auth.token.role || "");
    if (role !== schema_1.Values.Role.WORKER && role !== schema_1.Values.Role.EMPLOYER)
        (0, input_1.fail)("failed-precondition", "Finish registration first");
    const code = (0, input_1.str)((0, input_1.obj)(raw), "code", { min: 6, max: 20 });
    const created = Date.parse((await admin.auth().getUser(uid)).metadata.creationTime);
    if (Date.now() - created > LATE_REFERRAL_WINDOW_MS)
        (0, input_1.fail)("failed-precondition", "Referral codes can only be used in your first 7 days");
    const error = await registerReferral(uid, code);
    if (error)
        (0, input_1.fail)("invalid-argument", error);
    const profile = await db.collection(role === schema_1.Values.Role.EMPLOYER ? schema_1.EmployerProfiles.COLLECTION : schema_1.WorkerProfiles.COLLECTION).doc(uid).get();
    const complete = role === schema_1.Values.Role.EMPLOYER ? isEmployerProfileComplete(profile.data()) : isWorkerProfileComplete(profile.data());
    if (complete)
        await completeReferral(uid, role);
    return { ok: true };
});
function isWorkerProfileComplete(p) {
    if (!p)
        return false;
    const skills = p[schema_1.WorkerProfiles.SKILLS];
    return !!String(p[schema_1.WorkerProfiles.NAME] || "").trim() && !!String(p[schema_1.WorkerProfiles.PHONE] || "").trim() &&
        Array.isArray(skills) && skills.length > 0;
}
exports.isWorkerProfileComplete = isWorkerProfileComplete;
function isEmployerProfileComplete(p) {
    if (!p)
        return false;
    const name = p[schema_1.EmployerProfiles.EMPLOYER_TYPE] === schema_1.Values.EmployerType.COMPANY ?
        p[schema_1.EmployerProfiles.BUSINESS_NAME] : p[schema_1.EmployerProfiles.OWNER_NAME];
    return !!String(name || "").trim() && !!String(p[schema_1.EmployerProfiles.PHONE] || "").trim();
}
exports.isEmployerProfileComplete = isEmployerProfileComplete;
/**
 * The referred user completed their profile: pay the referrer (plus any milestone bonus), give the
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
        const referrerCodeSnap = await tx.get(db.collection(schema_1.ReferralCodes.COLLECTION).doc(String(referral.get(schema_1.Referrals.CODE))));
        const referrerRole = String(referrerCodeSnap.get(schema_1.ReferralCodes.ROLE) || schema_1.Values.Role.WORKER);
        const rewardId = `referral_${refereeUid}_referrer`;
        const bonusId = `referral_${refereeUid}_referee`;
        const [referrer, referee, rewardEntry, bonusEntry] = await Promise.all([
            readWallet(tx, referrerUid), readWallet(tx, refereeUid), tx.get(ledgerRef(rewardId)), tx.get(ledgerRef(bonusId)),
        ]);
        const employerRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(referrerUid);
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
        let refereePaise = 0;
        if (refereeRole === schema_1.Values.Role.WORKER && !bonusEntry.exists && config.signupBonus > 0) {
            refereePaise = Math.round(config.signupBonus * 100);
            postEntry(tx, refereeUid, referee, bonusId, schema_1.Values.LedgerType.SIGNUP_BONUS, refereePaise, referrerUid);
        }
        return { referrerUid, referrerRole, referrerPaise, refereePaise };
    });
    if (!paid)
        return;
    if (paid.referrerPaise > 0) {
        await (0, notify_1.notify)(paid.referrerUid, {
            type: "PAYMENT", templateId: "REFERRAL_REWARD_BASIC", role: paid.referrerRole,
            params: { amount: paid.referrerPaise / 100 }, data: { refId: refereeUid },
        });
    }
    if (paid.refereePaise > 0) {
        await (0, notify_1.notify)(refereeUid, {
            type: "PAYMENT", templateId: "SIGNUP_BONUS", role: schema_1.Values.Role.WORKER,
            params: { amount: paid.refereePaise / 100 }, data: {},
        });
    }
}
exports.completeReferral = completeReferral;
// ───────────────────────────── withdrawals ─────────────────────────────
const UPI_PATTERN = /^[a-zA-Z0-9._-]{2,256}@[a-zA-Z][a-zA-Z0-9.-]{1,64}$/;
/** Cash out the wallet to UPI (whole balance, capped per day). requestId makes a retry harmless. */
exports.requestWithdrawal = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
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
    const result = await db.runTransaction(async (tx) => {
        const [existing, wallet, daily] = await Promise.all([tx.get(withdrawalRef), readWallet(tx, uid), tx.get(dailyRef)]);
        if (existing.exists)
            return { withdrawalId, amountPaise: Number(existing.get(schema_1.Withdrawals.AMOUNT_PAISE)), replay: true };
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
        const [wallet, refund] = await Promise.all([readWallet(tx, uid), tx.get(ledgerRef(refundId))]);
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
exports.getReferralLeaderboard = (0, secure_callable_1.onCallSecured)({}, async () => {
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