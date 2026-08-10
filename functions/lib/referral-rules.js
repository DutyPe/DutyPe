"use strict";
/**
 * Pure decision logic for the referral money paths.
 *
 * Everything here is deliberately free of Firestore and firebase-functions so the
 * rules that move real money can be unit tested without an emulator. Callers in
 * referral-system.ts supply already-fetched counts and balances.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.scoreReferralFraud = exports.evaluateWithdrawal = exports.welcomeBonusAmountForRole = exports.normalizeReferralCodeInput = exports.generateIdempotencyKey = exports.getConfiguredMilestoneBonus = exports.getMilestoneBonus = exports.canWithdraw = exports.calculateTier = exports.timestampToMillis = exports.getStringValue = exports.getBooleanValue = exports.getNumberValue = exports.REFERRAL_RULES = void 0;
exports.REFERRAL_RULES = {
    MIN_WITHDRAWAL: 100,
    MAX_WITHDRAWAL_PER_DAY: 1000,
    SAME_IP_MAX_REFERRALS: 5,
    MILESTONES: {
        5: 50,
        10: 100,
        15: 150,
        25: 250,
        50: 500,
        100: 1000,
    },
    TIER_THRESHOLDS: [
        { tier: "ELITE", min: 100 },
        { tier: "DIAMOND", min: 50 },
        { tier: "PLATINUM", min: 25 },
        { tier: "GOLD", min: 10 },
        { tier: "SILVER", min: 5 },
        { tier: "BRONZE", min: 0 },
    ],
    FRAUD: {
        HIGH_VELOCITY_PER_HOUR: 10,
        SAME_DEVICE_PER_DAY: 2,
        SAME_IP_PER_DAY: 2,
        MIN_REFERRALS_FOR_REJECTION_RATE: 10,
        MAX_REJECTION_RATE: 0.3,
        BLOCK_AT_SCORE: 70,
        REVIEW_AT_SCORE: 40,
        SCORE_HIGH_VELOCITY: 40,
        SCORE_SAME_DEVICE: 50,
        SCORE_SAME_IP: 30,
        SCORE_HIGH_REJECTION_RATE: 25,
    },
};
function getNumberValue(value, fallback = 0) {
    return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}
exports.getNumberValue = getNumberValue;
function getBooleanValue(value, fallback = false) {
    return typeof value === "boolean" ? value : fallback;
}
exports.getBooleanValue = getBooleanValue;
function getStringValue(value, fallback = "") {
    return typeof value === "string" && value.trim() ? value : fallback;
}
exports.getStringValue = getStringValue;
/** Accepts Firestore Timestamps, Dates, and second- or millisecond-epoch numbers. */
function timestampToMillis(value) {
    if (!value)
        return 0;
    if (typeof (value === null || value === void 0 ? void 0 : value.toMillis) === "function") {
        return value.toMillis();
    }
    if (value instanceof Date) {
        return value.getTime();
    }
    if (typeof value === "number") {
        return value < 100000000000 ? value * 1000 : value;
    }
    return 0;
}
exports.timestampToMillis = timestampToMillis;
function calculateTier(successfulReferrals) {
    const count = getNumberValue(successfulReferrals);
    for (const { tier, min } of exports.REFERRAL_RULES.TIER_THRESHOLDS) {
        if (count >= min)
            return tier;
    }
    return "BRONZE";
}
exports.calculateTier = calculateTier;
function canWithdraw(availableBalance, minWithdrawal) {
    return availableBalance >= minWithdrawal;
}
exports.canWithdraw = canWithdraw;
function getMilestoneBonus(newCount) {
    return exports.REFERRAL_RULES.MILESTONES[newCount] || 0;
}
exports.getMilestoneBonus = getMilestoneBonus;
function getConfiguredMilestoneBonus(newCount, milestones) {
    var _a;
    const configured = Number((_a = milestones === null || milestones === void 0 ? void 0 : milestones[String(newCount)]) !== null && _a !== void 0 ? _a : milestones === null || milestones === void 0 ? void 0 : milestones[newCount]);
    return Number.isFinite(configured) && configured >= 0 ? configured : getMilestoneBonus(newCount);
}
exports.getConfiguredMilestoneBonus = getConfiguredMilestoneBonus;
function generateIdempotencyKey(referrerUserId, referredUserId) {
    return `${referrerUserId}_${referredUserId}`;
}
exports.generateIdempotencyKey = generateIdempotencyKey;
function normalizeReferralCodeInput(code) {
    return (code || "").replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
}
exports.normalizeReferralCodeInput = normalizeReferralCodeInput;
/** Employers currently receive no signup bonus; workers get the configured amount. */
function welcomeBonusAmountForRole(signupBonus, role) {
    const normalizedRole = getStringValue(role, "WORKER").toUpperCase();
    if (normalizedRole === "EMPLOYER")
        return 0;
    return Math.max(0, getNumberValue(signupBonus));
}
exports.welcomeBonusAmountForRole = welcomeBonusAmountForRole;
/**
 * The full guard chain for a withdrawal request. Order matters: the caller shows
 * the first failure to the user, so the most actionable message must win.
 */
function evaluateWithdrawal(input) {
    var _a, _b;
    const amount = getNumberValue(input.amount, NaN);
    const availableBalance = getNumberValue(input.availableBalance);
    const minWithdrawal = Math.max(getNumberValue(input.minWithdrawal), exports.REFERRAL_RULES.MIN_WITHDRAWAL);
    if (!Number.isFinite(amount) || amount <= 0) {
        return { ok: false, error: "Enter a valid amount" };
    }
    if (!input.canWithdrawFlag) {
        return { ok: false, error: "You need at least Rs." + minWithdrawal + " available to withdraw" };
    }
    if (amount < minWithdrawal) {
        return { ok: false, error: "Minimum withdrawal is Rs." + minWithdrawal };
    }
    if (amount > availableBalance) {
        return { ok: false, error: "Insufficient balance. Available: Rs." + availableBalance };
    }
    // Wallets are emptied in full so no dust balance can strand below the minimum.
    if (Math.abs(amount - availableBalance) > 0.01) {
        return { ok: false, error: "Withdraw the full available balance to empty your wallet" };
    }
    const dailyCap = getNumberValue(input.maxWithdrawalPerDay, exports.REFERRAL_RULES.MAX_WITHDRAWAL_PER_DAY);
    if (getNumberValue(input.alreadyWithdrawnToday) + amount > dailyCap) {
        return { ok: false, error: "Daily limit is Rs." + dailyCap };
    }
    if (input.paymentMethod === "UPI" && !getStringValue(input.upiId)) {
        return { ok: false, error: "UPI ID is required" };
    }
    if (input.paymentMethod === "BANK_TRANSFER" &&
        (!getStringValue((_a = input.bankDetails) === null || _a === void 0 ? void 0 : _a.accountNumber) || !getStringValue((_b = input.bankDetails) === null || _b === void 0 ? void 0 : _b.ifscCode))) {
        return { ok: false, error: "Bank account details are required" };
    }
    return { ok: true, amount };
}
exports.evaluateWithdrawal = evaluateWithdrawal;
/** Scores a referral from already-counted signals. Blocking beats scoring. */
function scoreReferralFraud(counts) {
    const f = exports.REFERRAL_RULES.FRAUD;
    let fraudScore = 0;
    const signals = [];
    if (getNumberValue(counts.referralsInLastHour) > f.HIGH_VELOCITY_PER_HOUR) {
        fraudScore += f.SCORE_HIGH_VELOCITY;
        signals.push("HIGH_VELOCITY");
    }
    if (getNumberValue(counts.sameDeviceReferralsToday) > f.SAME_DEVICE_PER_DAY) {
        fraudScore += f.SCORE_SAME_DEVICE;
        signals.push("SAME_DEVICE_MULTIPLE_REFERRALS");
    }
    if (counts.hasIpAddress) {
        const sameIp = getNumberValue(counts.sameIpReferralsToday);
        if (sameIp >= exports.REFERRAL_RULES.SAME_IP_MAX_REFERRALS) {
            return {
                allowed: false,
                fraudScore,
                signals: [...signals, "SAME_IP_LIMIT"],
                needsReview: false,
                rejectionReason: "SAME_IP_LIMIT",
                errorMessage: "Too many referrals from this network",
            };
        }
        if (sameIp > f.SAME_IP_PER_DAY) {
            fraudScore += f.SCORE_SAME_IP;
            signals.push("SAME_IP_MULTIPLE_REFERRALS");
        }
    }
    const totalReferrals = getNumberValue(counts.totalReferrals);
    const rejectedReferrals = getNumberValue(counts.rejectedReferrals);
    if (totalReferrals > f.MIN_REFERRALS_FOR_REJECTION_RATE &&
        rejectedReferrals / totalReferrals > f.MAX_REJECTION_RATE) {
        fraudScore += f.SCORE_HIGH_REJECTION_RATE;
        signals.push("HIGH_REJECTION_RATE");
    }
    if (fraudScore >= f.BLOCK_AT_SCORE) {
        return {
            allowed: false,
            fraudScore,
            signals,
            needsReview: false,
            rejectionReason: signals.join(", "),
            errorMessage: "Referral could not be processed",
        };
    }
    return {
        allowed: true,
        fraudScore,
        signals,
        needsReview: fraudScore >= f.REVIEW_AT_SCORE,
    };
}
exports.scoreReferralFraud = scoreReferralFraud;
//# sourceMappingURL=referral-rules.js.map