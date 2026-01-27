"use strict";
/**
 * ============================================
 * ENTERPRISE-GRADE REFERRAL SYSTEM
 * ============================================
 *
 * Architecture inspired by:
 * - Dropbox (two-sided rewards, viral growth)
 * - PayPal ($20 referral program that grew to 100M users)
 * - Uber (location-based fraud detection)
 * - Stripe (atomic transactions, idempotency)
 *
 * Key Features:
 * 1. Event-Driven Architecture - All referral events processed via Cloud Functions
 * 2. Atomic Transactions - Batch writes for consistency
 * 3. Fraud Prevention - Device fingerprinting, rate limiting, velocity checks
 * 4. Real-time Updates - Firestore listeners for instant UI updates
 * 5. Scalable Design - O(1) lookups, denormalized data, composite indexes
 * 6. Idempotency - Prevents duplicate rewards on retry
 *
 * Collections:
 * - referral_codes: O(1) code lookup (code as document ID)
 * - referral_stats: User's referral statistics (userId as document ID)
 * - referrals: Individual referral records with full audit trail
 * - referral_events: Event sourcing for audit and replay
 * - withdrawal_requests: Withdrawal tracking with admin approval
 * - fraud_signals: Fraud detection signals
 *
 * @author DutyPe Engineering Team
 * @version 2.0.0 - Enterprise Edition
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.getReferralLeaderboard = exports.getReferralHistory = exports.getReferralStats = exports.detectReferralFraud = exports.requestWithdrawal = exports.expirePendingReferrals = exports.onReferredUserProfileComplete = exports.applyReferralCode = exports.onUserProfileComplete = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
// ============================================
// REFERRAL SYSTEM CONSTANTS
// ============================================
const REFERRAL_CONFIG = {
    // Reward amounts (in INR)
    REWARD_PER_REFERRAL: 10,
    SIGNUP_BONUS: 10,
    MIN_WITHDRAWAL: 50,
    MAX_WITHDRAWAL_PER_DAY: 1000,
    // Milestone bonuses
    MILESTONES: {
        5: 50,
        10: 100,
        15: 150,
        25: 250,
        50: 500,
        100: 1000 // 100 referrals = ₹1000 bonus
    },
    // Withdrawal milestones (can withdraw at these counts)
    WITHDRAWAL_MILESTONES: [5, 10, 15],
    // Employer free job postings
    EMPLOYER_FREE_POSTINGS: {
        5: { count: 5, days: 15 },
        10: { count: 10, days: 30 },
        25: { count: 25, days: 60 }
    },
    // Fraud prevention
    MAX_REFERRALS_PER_DAY: 50,
    MAX_PENDING_REFERRALS: 100,
    REFERRAL_EXPIRY_DAYS: 30,
    SAME_DEVICE_COOLDOWN_HOURS: 24,
    SAME_IP_MAX_REFERRALS: 5,
    // Tier thresholds
    TIERS: {
        BRONZE: 0,
        SILVER: 5,
        GOLD: 10,
        PLATINUM: 25,
        DIAMOND: 50,
        ELITE: 100
    }
};
const ONE_DAY_MS = 24 * 60 * 60 * 1000;
const ONE_HOUR_MS = 60 * 60 * 1000;
// ============================================
// HELPER FUNCTIONS
// ============================================
/**
 * Calculate tier based on successful referrals
 */
function calculateTier(successfulReferrals) {
    if (successfulReferrals >= 100)
        return "ELITE";
    if (successfulReferrals >= 50)
        return "DIAMOND";
    if (successfulReferrals >= 25)
        return "PLATINUM";
    if (successfulReferrals >= 10)
        return "GOLD";
    if (successfulReferrals >= 5)
        return "SILVER";
    return "BRONZE";
}
/**
 * Get next milestone for user
 */
function getNextMilestone(successfulReferrals) {
    const milestones = [5, 10, 15, 25, 50, 100];
    for (const milestone of milestones) {
        if (successfulReferrals < milestone)
            return milestone;
    }
    return successfulReferrals + 10;
}
/**
 * Check if user can withdraw based on referral count
 */
function canWithdraw(successfulReferrals) {
    return successfulReferrals >= 15 ||
        REFERRAL_CONFIG.WITHDRAWAL_MILESTONES.includes(successfulReferrals);
}
/**
 * Get milestone bonus if applicable
 */
function getMilestoneBonus(newCount) {
    return REFERRAL_CONFIG.MILESTONES[newCount] || 0;
}
/**
 * Generate idempotency key for referral
 */
function generateIdempotencyKey(referrerUserId, referredUserId) {
    return `${referrerUserId}_${referredUserId}`;
}
// ============================================
// EVENT 1: REFERRAL CODE CREATION
// ============================================
// Triggered when user completes profile setup
// Creates unique referral code and initializes stats
exports.onUserProfileComplete = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
    var _a;
    const before = change.before.data();
    const after = change.after.data();
    const userId = context.params.userId;
    // Only trigger when profileCompleted changes from false to true
    if (before.profileCompleted === true || after.profileCompleted !== true) {
        return null;
    }
    functions.logger.info(`🎁 REFERRAL: User ${userId} completed profile, creating referral code`);
    try {
        const userRole = after.role || "WORKER";
        const userName = after.fullName || after.name || "";
        // Check if referral stats already exist
        const existingStats = await db.collection("referral_stats").doc(userId).get();
        if (existingStats.exists && ((_a = existingStats.data()) === null || _a === void 0 ? void 0 : _a.referralCode)) {
            functions.logger.info(`🎁 REFERRAL: User ${userId} already has referral code`);
            return null;
        }
        // Generate unique referral code
        const prefix = userRole === "EMPLOYER" ? "EMP" : "WRK";
        const uniquePart = userId.substring(0, 6).toUpperCase();
        const referralCode = `${prefix}${uniquePart}`;
        // Use batch write for atomicity
        const batch = db.batch();
        // 1. Create referral_codes document (code as document ID for O(1) lookup)
        const codeRef = db.collection("referral_codes").doc(referralCode);
        batch.set(codeRef, {
            code: referralCode,
            userId: userId,
            userRole: userRole,
            userName: userName,
            isActive: true,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            totalUsed: 0,
            successfulReferrals: 0
        });
        // 2. Create referral_stats document (userId as document ID)
        const statsRef = db.collection("referral_stats").doc(userId);
        batch.set(statsRef, {
            userId: userId,
            userRole: userRole,
            referralCode: referralCode,
            totalReferrals: 0,
            successfulReferrals: 0,
            pendingReferrals: 0,
            expiredReferrals: 0,
            rejectedReferrals: 0,
            totalEarnings: 0,
            availableBalance: 0,
            withdrawnAmount: 0,
            canWithdraw: false,
            nextMilestone: 5,
            currentTier: "BRONZE",
            freeJobPostings: 0,
            freeJobPostingsExpiry: null,
            isBlocked: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        // 3. Log event for audit trail
        const eventRef = db.collection("referral_events").doc();
        batch.set(eventRef, {
            eventType: "CODE_CREATED",
            userId: userId,
            referralCode: referralCode,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await batch.commit();
        functions.logger.info(`🎁 REFERRAL: ✅ Created code ${referralCode} for user ${userId}`);
        return { success: true, referralCode };
    }
    catch (error) {
        functions.logger.error(`🎁 REFERRAL: Error creating code for ${userId}:`, error);
        return null;
    }
});
// ============================================
// EVENT 2: REFERRAL APPLICATION (PENDING)
// ============================================
// Triggered when new user applies a referral code during signup
// Creates PENDING referral record
exports.applyReferralCode = functions.https.onCall(async (data, context) => {
    var _a, _b;
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
    }
    const newUserId = context.auth.uid;
    const referralCode = (data.referralCode || "").trim().toUpperCase();
    const newUserRole = data.userRole || "WORKER";
    const newUserName = data.userName || "";
    const newUserPhone = data.userPhone || "";
    const deviceFingerprint = data.deviceFingerprint || null;
    if (!referralCode) {
        return { success: false, error: "Referral code is required" };
    }
    functions.logger.info(`🎁 REFERRAL: User ${newUserId} applying code ${referralCode}`);
    try {
        // Get IP address for fraud detection
        const ipAddress = context.rawRequest.ip ||
            ((_a = context.rawRequest.headers["x-forwarded-for"]) === null || _a === void 0 ? void 0 : _a.toString().split(",")[0]) ||
            "unknown";
        // 1. Validate referral code (O(1) lookup)
        const codeDoc = await db.collection("referral_codes").doc(referralCode).get();
        if (!codeDoc.exists) {
            functions.logger.warn(`🎁 REFERRAL: Code ${referralCode} not found`);
            return { success: false, error: "Referral code not found" };
        }
        const codeData = codeDoc.data();
        // Check if code is active
        if (!codeData.isActive) {
            return { success: false, error: "This referral code is no longer active" };
        }
        const referrerUserId = codeData.userId;
        // 2. Self-referral check
        if (referrerUserId === newUserId) {
            return { success: false, error: "Cannot use your own referral code" };
        }
        // 3. Check for existing referral (idempotency)
        const idempotencyKey = generateIdempotencyKey(referrerUserId, newUserId);
        const existingReferral = await db.collection("referrals")
            .where("idempotencyKey", "==", idempotencyKey)
            .limit(1)
            .get();
        if (!existingReferral.empty) {
            functions.logger.info(`🎁 REFERRAL: Duplicate referral detected for ${newUserId}`);
            return { success: true, message: "Referral already applied" };
        }
        // 4. Fraud checks
        const now = Date.now();
        const oneDayAgo = now - ONE_DAY_MS;
        // Check same device fingerprint
        if (deviceFingerprint) {
            const sameDeviceReferrals = await db.collection("referrals")
                .where("deviceFingerprint", "==", deviceFingerprint)
                .where("createdAt", ">", new Date(oneDayAgo))
                .limit(1)
                .get();
            if (!sameDeviceReferrals.empty) {
                functions.logger.warn(`🎁 REFERRAL: ⚠️ Same device used recently: ${deviceFingerprint}`);
                // Log fraud signal but don't block (soft fraud detection)
                await db.collection("fraud_signals").add({
                    userId: newUserId,
                    signalType: "SAME_DEVICE_REFERRAL",
                    severity: "MEDIUM",
                    details: { deviceFingerprint, referralCode },
                    timestamp: admin.firestore.FieldValue.serverTimestamp(),
                    resolved: false
                });
            }
        }
        // Check same IP rate limit
        const sameIpReferrals = await db.collection("referrals")
            .where("ipAddress", "==", ipAddress)
            .where("createdAt", ">", new Date(oneDayAgo))
            .get();
        if (sameIpReferrals.size >= REFERRAL_CONFIG.SAME_IP_MAX_REFERRALS) {
            functions.logger.warn(`🎁 REFERRAL: ⚠️ IP ${ipAddress} exceeded daily limit`);
            await db.collection("fraud_signals").add({
                userId: newUserId,
                signalType: "IP_RATE_LIMIT_EXCEEDED",
                severity: "HIGH",
                details: { ipAddress, count: sameIpReferrals.size },
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                resolved: false
            });
            return { success: false, error: "Too many referrals from this network" };
        }
        // Check referrer's pending count
        const referrerStats = await db.collection("referral_stats").doc(referrerUserId).get();
        const pendingCount = ((_b = referrerStats.data()) === null || _b === void 0 ? void 0 : _b.pendingReferrals) || 0;
        if (pendingCount >= REFERRAL_CONFIG.MAX_PENDING_REFERRALS) {
            return { success: false, error: "Referrer has too many pending referrals" };
        }
        // 5. Create referral record with batch write
        const batch = db.batch();
        const referralId = db.collection("referrals").doc().id;
        const expiresAt = now + (REFERRAL_CONFIG.REFERRAL_EXPIRY_DAYS * ONE_DAY_MS);
        const maskedPhone = newUserPhone ? `****${newUserPhone.slice(-4)}` : "";
        // Create referral document
        const referralRef = db.collection("referrals").doc(referralId);
        batch.set(referralRef, {
            id: referralId,
            idempotencyKey: idempotencyKey,
            referrerUserId: referrerUserId,
            referrerRole: codeData.userRole,
            referredUserId: newUserId,
            referredRole: newUserRole,
            referralCode: referralCode,
            status: "PENDING",
            rewardAmount: 0,
            bonusAmount: 0,
            referredUserReward: 0,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            expiresAt: new Date(expiresAt),
            referredUserName: newUserName,
            referredUserPhone: maskedPhone,
            deviceFingerprint: deviceFingerprint,
            ipAddress: ipAddress
        });
        // Update referrer's stats
        const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
        batch.set(referrerStatsRef, {
            totalReferrals: admin.firestore.FieldValue.increment(1),
            pendingReferrals: admin.firestore.FieldValue.increment(1),
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        // Update code usage count
        const codeRef = db.collection("referral_codes").doc(referralCode);
        batch.update(codeRef, {
            totalUsed: admin.firestore.FieldValue.increment(1)
        });
        // Create/update new user's stats with referral info
        const newUserStatsRef = db.collection("referral_stats").doc(newUserId);
        batch.set(newUserStatsRef, {
            userId: newUserId,
            userRole: newUserRole,
            referredByCode: referralCode,
            referredByUserId: referrerUserId,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        // Log event
        const eventRef = db.collection("referral_events").doc();
        batch.set(eventRef, {
            eventType: "REFERRAL_APPLIED",
            referralId: referralId,
            referrerUserId: referrerUserId,
            referredUserId: newUserId,
            referralCode: referralCode,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await batch.commit();
        functions.logger.info(`🎁 REFERRAL: ✅ Code ${referralCode} applied for user ${newUserId}`);
        return {
            success: true,
            referralId: referralId,
            referrerName: codeData.userName,
            referrerRole: codeData.userRole
        };
    }
    catch (error) {
        functions.logger.error(`🎁 REFERRAL: Error applying code:`, error);
        throw new functions.https.HttpsError("internal", "Failed to apply referral code");
    }
});
// ============================================
// EVENT 3: REFERRAL COMPLETION (REWARD CREDIT)
// ============================================
// Triggered when referred user completes their profile
// Credits rewards to BOTH referrer and referred user
exports.onReferredUserProfileComplete = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
    var _a;
    const before = change.before.data();
    const after = change.after.data();
    const referredUserId = context.params.userId;
    // Only trigger when profileCompleted changes from false to true
    if (before.profileCompleted === true || after.profileCompleted !== true) {
        return null;
    }
    functions.logger.info(`🎁 REFERRAL: Checking pending referral for user ${referredUserId}`);
    try {
        // Find pending referral for this user
        const pendingReferrals = await db.collection("referrals")
            .where("referredUserId", "==", referredUserId)
            .where("status", "==", "PENDING")
            .limit(1)
            .get();
        if (pendingReferrals.empty) {
            functions.logger.info(`🎁 REFERRAL: No pending referral for user ${referredUserId}`);
            return null;
        }
        const referralDoc = pendingReferrals.docs[0];
        const referral = referralDoc.data();
        const referralId = referralDoc.id;
        const referrerUserId = referral.referrerUserId;
        // Check if expired
        const expiresAt = ((_a = referral.expiresAt) === null || _a === void 0 ? void 0 : _a.toMillis()) || 0;
        if (Date.now() > expiresAt) {
            functions.logger.info(`🎁 REFERRAL: Referral ${referralId} expired`);
            // Mark as expired
            await db.collection("referrals").doc(referralId).update({
                status: "EXPIRED",
                completedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            // Update referrer stats
            await db.collection("referral_stats").doc(referrerUserId).update({
                pendingReferrals: admin.firestore.FieldValue.increment(-1),
                expiredReferrals: admin.firestore.FieldValue.increment(1),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });
            return { status: "EXPIRED" };
        }
        // Get referrer's current stats for milestone calculation
        const referrerStatsDoc = await db.collection("referral_stats").doc(referrerUserId).get();
        const referrerStats = referrerStatsDoc.data() || {};
        const currentSuccessful = referrerStats.successfulReferrals || 0;
        const newSuccessfulCount = currentSuccessful + 1;
        // Calculate rewards
        const referrerReward = REFERRAL_CONFIG.REWARD_PER_REFERRAL;
        const referredUserReward = REFERRAL_CONFIG.SIGNUP_BONUS;
        const milestoneBonus = getMilestoneBonus(newSuccessfulCount);
        const totalReferrerReward = referrerReward + milestoneBonus;
        // Calculate new tier and withdrawal eligibility
        const newTier = calculateTier(newSuccessfulCount);
        const newCanWithdraw = canWithdraw(newSuccessfulCount);
        const newNextMilestone = getNextMilestone(newSuccessfulCount);
        // Calculate employer free postings
        let freePostings = referrerStats.freeJobPostings || 0;
        let freePostingsExpiry = referrerStats.freeJobPostingsExpiry;
        if (referrerStats.userRole === "EMPLOYER") {
            const postingReward = REFERRAL_CONFIG.EMPLOYER_FREE_POSTINGS[newSuccessfulCount];
            if (postingReward) {
                freePostings = postingReward.count;
                freePostingsExpiry = new Date(Date.now() + postingReward.days * ONE_DAY_MS);
            }
        }
        // Use batch write for atomic update
        const batch = db.batch();
        // 1. Update referral status
        const referralRef = db.collection("referrals").doc(referralId);
        batch.update(referralRef, {
            status: "COMPLETED",
            rewardAmount: referrerReward,
            bonusAmount: milestoneBonus,
            referredUserReward: referredUserReward,
            completedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        // 2. Update referrer's stats
        const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
        const referrerStatsUpdate = {
            successfulReferrals: newSuccessfulCount,
            pendingReferrals: admin.firestore.FieldValue.increment(-1),
            totalEarnings: admin.firestore.FieldValue.increment(totalReferrerReward),
            availableBalance: admin.firestore.FieldValue.increment(totalReferrerReward),
            canWithdraw: newCanWithdraw,
            currentTier: newTier,
            nextMilestone: newNextMilestone,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        };
        if (referrerStats.userRole === "EMPLOYER" && freePostingsExpiry) {
            referrerStatsUpdate.freeJobPostings = freePostings;
            referrerStatsUpdate.freeJobPostingsExpiry = freePostingsExpiry;
        }
        batch.update(referrerStatsRef, referrerStatsUpdate);
        // 3. Update referral code stats
        const codeRef = db.collection("referral_codes").doc(referral.referralCode);
        batch.update(codeRef, {
            successfulReferrals: admin.firestore.FieldValue.increment(1)
        });
        // 4. Credit referred user's signup bonus
        const referredStatsRef = db.collection("referral_stats").doc(referredUserId);
        batch.set(referredStatsRef, {
            totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
            availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
            signupBonusReceived: true,
            signupBonusAmount: referredUserReward,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        // 5. Log events
        const referrerEventRef = db.collection("referral_events").doc();
        batch.set(referrerEventRef, {
            eventType: "REWARD_CREDITED",
            userId: referrerUserId,
            referralId: referralId,
            amount: totalReferrerReward,
            bonusAmount: milestoneBonus,
            newTier: newTier,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        const referredEventRef = db.collection("referral_events").doc();
        batch.set(referredEventRef, {
            eventType: "SIGNUP_BONUS_CREDITED",
            userId: referredUserId,
            referralId: referralId,
            amount: referredUserReward,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        // 6. Send notifications (with idempotency check)
        // Check if notifications already exist for this referral to prevent duplicates
        const existingNotifications = await db.collection("notifications")
            .where("data.referralId", "==", referralId)
            .limit(1)
            .get();
        if (existingNotifications.empty) {
            const referrerNotifRef = db.collection("notifications").doc();
            batch.set(referrerNotifRef, {
                recipientId: referrerUserId,
                title: "🎉 Referral Successful!",
                message: `${referral.referredUserName || "Someone"} joined using your code! You earned ₹${totalReferrerReward}${milestoneBonus > 0 ? ` (includes ₹${milestoneBonus} milestone bonus!)` : ""}`,
                type: "REFERRAL_REWARD",
                data: { referralId, amount: totalReferrerReward },
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                isRead: false
            });
            const referredNotifRef = db.collection("notifications").doc();
            batch.set(referredNotifRef, {
                recipientId: referredUserId,
                title: "🎁 Welcome Bonus!",
                message: `You earned ₹${referredUserReward} for joining with a referral code!`,
                type: "SIGNUP_BONUS",
                data: { amount: referredUserReward },
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
                isRead: false
            });
            functions.logger.info(`🎁 REFERRAL: Creating notifications for referral ${referralId}`);
        }
        else {
            functions.logger.info(`🎁 REFERRAL: Notifications already exist for referral ${referralId}, skipping`);
        }
        await batch.commit();
        functions.logger.info(`🎁 REFERRAL: ✅ Completed! Referrer ${referrerUserId} earned ₹${totalReferrerReward}, Referred ${referredUserId} earned ₹${referredUserReward}`);
        return {
            status: "COMPLETED",
            referrerReward: totalReferrerReward,
            referredUserReward: referredUserReward,
            milestoneBonus: milestoneBonus,
            newTier: newTier
        };
    }
    catch (error) {
        functions.logger.error(`🎁 REFERRAL: Error completing referral:`, error);
        return null;
    }
});
// ============================================
// EVENT 4: REFERRAL EXPIRY CLEANUP
// ============================================
// Scheduled function to expire old pending referrals
exports.expirePendingReferrals = functions.pubsub
    .schedule("every 6 hours")
    .onRun(async (context) => {
    functions.logger.info("🎁 REFERRAL: Running expiry cleanup");
    try {
        const now = new Date();
        // Find expired pending referrals
        const expiredReferrals = await db.collection("referrals")
            .where("status", "==", "PENDING")
            .where("expiresAt", "<", now)
            .limit(500) // Process in batches
            .get();
        if (expiredReferrals.empty) {
            functions.logger.info("🎁 REFERRAL: No expired referrals found");
            return null;
        }
        functions.logger.info(`🎁 REFERRAL: Found ${expiredReferrals.size} expired referrals`);
        // Group by referrer for batch updates
        const referrerUpdates = {};
        const batch = db.batch();
        for (const doc of expiredReferrals.docs) {
            const referral = doc.data();
            // Update referral status
            batch.update(doc.ref, {
                status: "EXPIRED",
                completedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            // Track referrer updates
            const referrerId = referral.referrerUserId;
            referrerUpdates[referrerId] = (referrerUpdates[referrerId] || 0) + 1;
            // Log event
            const eventRef = db.collection("referral_events").doc();
            batch.set(eventRef, {
                eventType: "REFERRAL_EXPIRED",
                referralId: doc.id,
                referrerUserId: referrerId,
                referredUserId: referral.referredUserId,
                timestamp: admin.firestore.FieldValue.serverTimestamp()
            });
        }
        // Update referrer stats
        for (const [referrerId, expiredCount] of Object.entries(referrerUpdates)) {
            const statsRef = db.collection("referral_stats").doc(referrerId);
            batch.update(statsRef, {
                pendingReferrals: admin.firestore.FieldValue.increment(-expiredCount),
                expiredReferrals: admin.firestore.FieldValue.increment(expiredCount),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });
        }
        await batch.commit();
        functions.logger.info(`🎁 REFERRAL: ✅ Expired ${expiredReferrals.size} referrals`);
        return { expiredCount: expiredReferrals.size };
    }
    catch (error) {
        functions.logger.error("🎁 REFERRAL: Error in expiry cleanup:", error);
        return null;
    }
});
// ============================================
// EVENT 5: WITHDRAWAL REQUEST PROCESSING
// ============================================
exports.requestWithdrawal = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
    }
    const userId = context.auth.uid;
    const amount = parseFloat(data.amount) || 0;
    const paymentMethod = data.paymentMethod || "UPI";
    const upiId = data.upiId;
    const bankDetails = data.bankDetails;
    functions.logger.info(`🎁 REFERRAL: Withdrawal request from ${userId} for ₹${amount}`);
    try {
        // Get user's referral stats
        const statsDoc = await db.collection("referral_stats").doc(userId).get();
        if (!statsDoc.exists) {
            return { success: false, error: "No referral stats found" };
        }
        const stats = statsDoc.data();
        // Validation checks
        if (stats.isBlocked) {
            return { success: false, error: "Your account is blocked from withdrawals" };
        }
        if (!stats.canWithdraw) {
            return { success: false, error: "You need at least 5 successful referrals to withdraw" };
        }
        if (amount < REFERRAL_CONFIG.MIN_WITHDRAWAL) {
            return { success: false, error: `Minimum withdrawal is ₹${REFERRAL_CONFIG.MIN_WITHDRAWAL}` };
        }
        if (amount > stats.availableBalance) {
            return { success: false, error: `Insufficient balance. Available: ₹${stats.availableBalance}` };
        }
        // Check daily withdrawal limit
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const todayWithdrawals = await db.collection("withdrawal_requests")
            .where("userId", "==", userId)
            .where("createdAt", ">=", today)
            .get();
        const todayTotal = todayWithdrawals.docs.reduce((sum, doc) => {
            return sum + (doc.data().amount || 0);
        }, 0);
        if (todayTotal + amount > REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY) {
            return { success: false, error: `Daily limit is ₹${REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY}` };
        }
        // Validate payment details
        if (paymentMethod === "UPI" && !upiId) {
            return { success: false, error: "UPI ID is required" };
        }
        if (paymentMethod === "BANK_TRANSFER" && (!(bankDetails === null || bankDetails === void 0 ? void 0 : bankDetails.accountNumber) || !(bankDetails === null || bankDetails === void 0 ? void 0 : bankDetails.ifscCode))) {
            return { success: false, error: "Bank account details are required" };
        }
        // Create withdrawal request
        const withdrawalId = db.collection("withdrawal_requests").doc().id;
        const batch = db.batch();
        // Create withdrawal document
        const withdrawalRef = db.collection("withdrawal_requests").doc(withdrawalId);
        batch.set(withdrawalRef, {
            id: withdrawalId,
            userId: userId,
            userRole: stats.userRole,
            amount: amount,
            status: "PENDING",
            paymentMethod: paymentMethod,
            upiId: upiId || null,
            bankAccountNumber: (bankDetails === null || bankDetails === void 0 ? void 0 : bankDetails.accountNumber) || null,
            ifscCode: (bankDetails === null || bankDetails === void 0 ? void 0 : bankDetails.ifscCode) || null,
            accountHolderName: (bankDetails === null || bankDetails === void 0 ? void 0 : bankDetails.accountHolderName) || null,
            createdAt: admin.firestore.FieldValue.serverTimestamp()
        });
        // Deduct from available balance
        const statsRef = db.collection("referral_stats").doc(userId);
        batch.update(statsRef, {
            availableBalance: admin.firestore.FieldValue.increment(-amount),
            lastWithdrawalAt: admin.firestore.FieldValue.serverTimestamp(),
            totalWithdrawals: admin.firestore.FieldValue.increment(1),
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        });
        // Log event
        const eventRef = db.collection("referral_events").doc();
        batch.set(eventRef, {
            eventType: "WITHDRAWAL_REQUESTED",
            userId: userId,
            withdrawalId: withdrawalId,
            amount: amount,
            timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
        await batch.commit();
        functions.logger.info(`🎁 REFERRAL: ✅ Withdrawal request ${withdrawalId} created for ₹${amount}`);
        return { success: true, withdrawalId: withdrawalId };
    }
    catch (error) {
        functions.logger.error("🎁 REFERRAL: Error creating withdrawal:", error);
        throw new functions.https.HttpsError("internal", "Failed to create withdrawal request");
    }
});
// ============================================
// EVENT 6: FRAUD DETECTION & BLOCKING
// ============================================
exports.detectReferralFraud = functions.firestore
    .document("referrals/{referralId}")
    .onCreate(async (snapshot, context) => {
    const referral = snapshot.data();
    const referralId = context.params.referralId;
    const referrerUserId = referral.referrerUserId;
    functions.logger.info(`🎁 REFERRAL FRAUD: Analyzing referral ${referralId}`);
    try {
        let fraudScore = 0;
        const signals = [];
        const now = Date.now();
        const oneDayAgo = now - ONE_DAY_MS;
        const oneHourAgo = now - ONE_HOUR_MS;
        // CHECK 1: Velocity - Too many referrals in short time
        const recentReferrals = await db.collection("referrals")
            .where("referrerUserId", "==", referrerUserId)
            .where("createdAt", ">", new Date(oneHourAgo))
            .get();
        if (recentReferrals.size > 10) {
            fraudScore += 40;
            signals.push("HIGH_VELOCITY");
            functions.logger.warn(`🎁 FRAUD: High velocity - ${recentReferrals.size} referrals in 1 hour`);
        }
        // CHECK 2: Same device fingerprint used multiple times
        if (referral.deviceFingerprint) {
            const sameDeviceReferrals = await db.collection("referrals")
                .where("deviceFingerprint", "==", referral.deviceFingerprint)
                .where("createdAt", ">", new Date(oneDayAgo))
                .get();
            if (sameDeviceReferrals.size > 2) {
                fraudScore += 50;
                signals.push("SAME_DEVICE_MULTIPLE_REFERRALS");
                functions.logger.warn(`🎁 FRAUD: Same device used ${sameDeviceReferrals.size} times`);
            }
        }
        // CHECK 3: Same IP used for multiple referrals
        if (referral.ipAddress && referral.ipAddress !== "unknown") {
            const sameIpReferrals = await db.collection("referrals")
                .where("ipAddress", "==", referral.ipAddress)
                .where("createdAt", ">", new Date(oneDayAgo))
                .get();
            if (sameIpReferrals.size > REFERRAL_CONFIG.SAME_IP_MAX_REFERRALS) {
                fraudScore += 30;
                signals.push("SAME_IP_MULTIPLE_REFERRALS");
                functions.logger.warn(`🎁 FRAUD: Same IP used ${sameIpReferrals.size} times`);
            }
        }
        // CHECK 4: Referrer has high rejection rate
        const referrerStats = await db.collection("referral_stats").doc(referrerUserId).get();
        const stats = referrerStats.data() || {};
        const totalReferrals = stats.totalReferrals || 0;
        const rejectedReferrals = stats.rejectedReferrals || 0;
        if (totalReferrals > 10 && rejectedReferrals / totalReferrals > 0.3) {
            fraudScore += 25;
            signals.push("HIGH_REJECTION_RATE");
            functions.logger.warn(`🎁 FRAUD: High rejection rate - ${rejectedReferrals}/${totalReferrals}`);
        }
        // DETERMINE ACTION
        if (fraudScore >= 70) {
            // HIGH RISK: Auto-reject referral
            functions.logger.warn(`🎁 FRAUD: ⛔ Referral ${referralId} AUTO-REJECTED (score: ${fraudScore})`);
            const batch = db.batch();
            // Reject referral
            batch.update(snapshot.ref, {
                status: "REJECTED",
                rejectionReason: signals.join(", "),
                fraudScore: fraudScore,
                completedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            // Update referrer stats
            const statsRef = db.collection("referral_stats").doc(referrerUserId);
            batch.update(statsRef, {
                pendingReferrals: admin.firestore.FieldValue.increment(-1),
                rejectedReferrals: admin.firestore.FieldValue.increment(1),
                lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            });
            // Create fraud signal
            const fraudRef = db.collection("fraud_signals").doc();
            batch.set(fraudRef, {
                userId: referrerUserId,
                signalType: "REFERRAL_FRAUD_DETECTED",
                severity: "HIGH",
                details: { referralId, fraudScore, signals },
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                resolved: false
            });
            // Check if user should be blocked
            const existingFraudSignals = await db.collection("fraud_signals")
                .where("userId", "==", referrerUserId)
                .where("severity", "==", "HIGH")
                .get();
            if (existingFraudSignals.size >= 3) {
                // Block user
                batch.update(statsRef, {
                    isBlocked: true,
                    blockReason: "Multiple fraud signals detected"
                });
                // Deactivate referral code
                if (stats.referralCode) {
                    const codeRef = db.collection("referral_codes").doc(stats.referralCode);
                    batch.update(codeRef, { isActive: false });
                }
                functions.logger.warn(`🎁 FRAUD: ⛔ User ${referrerUserId} BLOCKED`);
            }
            await batch.commit();
            return { status: "REJECTED", fraudScore, signals };
        }
        else if (fraudScore >= 40) {
            // MEDIUM RISK: Flag for review
            functions.logger.info(`🎁 FRAUD: ⚠️ Referral ${referralId} flagged for review (score: ${fraudScore})`);
            await snapshot.ref.update({
                fraudScore: fraudScore,
                fraudSignals: signals,
                needsReview: true
            });
            await db.collection("fraud_signals").add({
                userId: referrerUserId,
                signalType: "REFERRAL_SUSPICIOUS",
                severity: "MEDIUM",
                details: { referralId, fraudScore, signals },
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                resolved: false
            });
            return { status: "FLAGGED", fraudScore, signals };
        }
        // LOW RISK: Pass
        functions.logger.info(`🎁 FRAUD: ✅ Referral ${referralId} passed fraud check (score: ${fraudScore})`);
        return { status: "PASSED", fraudScore };
    }
    catch (error) {
        functions.logger.error(`🎁 FRAUD: Error analyzing referral:`, error);
        return null;
    }
});
// ============================================
// UTILITY FUNCTIONS
// ============================================
/**
 * Get referral stats for a user
 */
exports.getReferralStats = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
    }
    const userId = data.userId || context.auth.uid;
    try {
        const statsDoc = await db.collection("referral_stats").doc(userId).get();
        if (!statsDoc.exists) {
            return { exists: false };
        }
        return { exists: true, stats: statsDoc.data() };
    }
    catch (error) {
        functions.logger.error("Error getting referral stats:", error);
        throw new functions.https.HttpsError("internal", "Failed to get referral stats");
    }
});
/**
 * Get referral history for a user
 */
exports.getReferralHistory = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
    }
    const userId = context.auth.uid;
    const limit = data.limit || 20;
    try {
        const referrals = await db.collection("referrals")
            .where("referrerUserId", "==", userId)
            .orderBy("createdAt", "desc")
            .limit(limit)
            .get();
        return {
            referrals: referrals.docs.map(doc => (Object.assign({ id: doc.id }, doc.data())))
        };
    }
    catch (error) {
        functions.logger.error("Error getting referral history:", error);
        throw new functions.https.HttpsError("internal", "Failed to get referral history");
    }
});
/**
 * Get leaderboard
 */
exports.getReferralLeaderboard = functions.https.onCall(async (data, context) => {
    const role = data.role; // Optional filter by role
    const limit = data.limit || 10;
    try {
        let query = db.collection("referral_stats")
            .where("isBlocked", "==", false)
            .orderBy("successfulReferrals", "desc")
            .limit(limit);
        if (role) {
            query = db.collection("referral_stats")
                .where("userRole", "==", role)
                .where("isBlocked", "==", false)
                .orderBy("successfulReferrals", "desc")
                .limit(limit);
        }
        const leaderboard = await query.get();
        return {
            leaderboard: leaderboard.docs.map((doc, index) => (Object.assign({ rank: index + 1, userId: doc.id }, doc.data())))
        };
    }
    catch (error) {
        functions.logger.error("Error getting leaderboard:", error);
        throw new functions.https.HttpsError("internal", "Failed to get leaderboard");
    }
});
//# sourceMappingURL=referral-system.js.map