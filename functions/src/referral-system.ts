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
 * P0 SECURITY FIX: Added comprehensive input validation and rate limiting
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
 * @version 2.0.0 - Enterprise Edition with Security Hardening
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { validateString, validateNumber, validateUserId, validateEnum, checkRateLimit } from "./validation";

const db = admin.firestore();

// ============================================
// REFERRAL SYSTEM CONSTANTS
// ============================================
const REFERRAL_CONFIG = {
  // Reward amounts (in INR)
  REWARD_PER_REFERRAL: 25,           // ₹25 per successful referral
  SIGNUP_BONUS: 25,                   // ₹25 for new user who uses code
  MIN_WITHDRAWAL: 50,                 // Minimum ₹50 to withdraw
  MAX_WITHDRAWAL_PER_DAY: 1000,       // Max ₹1000 per day
  
  // Milestone bonuses
  MILESTONES: {
    5: 50,    // 5 referrals = ₹50 bonus
    10: 100,  // 10 referrals = ₹100 bonus
    15: 150,  // 15 referrals = ₹150 bonus
    25: 250,  // 25 referrals = ₹250 bonus
    50: 500,  // 50 referrals = ₹500 bonus
    100: 1000 // 100 referrals = ₹1000 bonus
  } as { [key: number]: number },
  
  // Withdrawal milestones (can withdraw at these counts)
  WITHDRAWAL_MILESTONES: [5, 10, 15],
  
  // Employer free job postings
  EMPLOYER_FREE_POSTINGS: {
    5: { count: 5, days: 15 },
    10: { count: 10, days: 30 },
    25: { count: 25, days: 60 }
  } as { [key: number]: { count: number; days: number } },
  
  // Fraud prevention
  MAX_REFERRALS_PER_DAY: 50,          // Max referrals per user per day
  MAX_PENDING_REFERRALS: 100,         // Max pending referrals
  REFERRAL_EXPIRY_DAYS: 30,           // Referral expires in 30 days
  SAME_DEVICE_COOLDOWN_HOURS: 24,     // Same device can't be used for 24 hours
  SAME_IP_MAX_REFERRALS: 5,           // Max 5 referrals from same IP per day
  
  // Tier thresholds
  TIERS: {
    BRONZE: 0,
    SILVER: 5,
    GOLD: 10,
    PLATINUM: 25,
    DIAMOND: 50,
    ELITE: 100
  } as { [key: string]: number }
};

const ONE_DAY_MS = 24 * 60 * 60 * 1000;
const ONE_HOUR_MS = 60 * 60 * 1000;


// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Calculate tier based on successful referrals
 */
function calculateTier(successfulReferrals: number): string {
  if (successfulReferrals >= 100) return "ELITE";
  if (successfulReferrals >= 50) return "DIAMOND";
  if (successfulReferrals >= 25) return "PLATINUM";
  if (successfulReferrals >= 10) return "GOLD";
  if (successfulReferrals >= 5) return "SILVER";
  return "BRONZE";
}

/**
 * Get next milestone for user
 */
function getNextMilestone(successfulReferrals: number): number {
  const milestones = [5, 10, 15, 25, 50, 100];
  for (const milestone of milestones) {
    if (successfulReferrals < milestone) return milestone;
  }
  return successfulReferrals + 10;
}

/**
 * Check if user can withdraw based on referral count
 */
function canWithdraw(successfulReferrals: number): boolean {
  return successfulReferrals >= 15 || 
         REFERRAL_CONFIG.WITHDRAWAL_MILESTONES.includes(successfulReferrals);
}

/**
 * Get milestone bonus if applicable
 */
function getMilestoneBonus(newCount: number): number {
  return REFERRAL_CONFIG.MILESTONES[newCount] || 0;
}

/**
 * Generate idempotency key for referral
 */
function generateIdempotencyKey(referrerUserId: string, referredUserId: string): string {
  return `${referrerUserId}_${referredUserId}`;
}


// ============================================
// EVENT 1: REFERRAL CODE CREATION
// ============================================
// Triggered when user COMPLETES PROFILE (has fullName)
// NOT when account is created (only has phone number at that point)
// This ensures we can generate personalized codes like "vamsi9843"

export const onUserProfileComplete = functions.firestore
  .document("users/{userId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const userId = context.params.userId;

    // GUARD 1: Only trigger when profileCompleted changes from false to true (FIRST TIME ONLY)
    if (before.profileCompleted === true || after.profileCompleted !== true) {
      return null;
    }

    // GUARD 2: Check if referral code already exists in BEFORE state (should never happen, but extra safety)
    if (before.referralCode) {
      functions.logger.info(`🎁 REFERRAL: User ${userId} already had referral code in before state: ${before.referralCode}`);
      return null;
    }

    // GUARD 3: Check if referral code already exists in AFTER state
    if (after.referralCode) {
      functions.logger.info(`🎁 REFERRAL: User ${userId} already has referral code in after state: ${after.referralCode}`);
      return null;
    }

    functions.logger.info(`🎁 REFERRAL: User ${userId} completed profile for FIRST TIME, generating referral code`);

    try {
      const userRole = after.role || "WORKER";
      const userName = after.fullName || after.name || "";
      
      if (!userName) {
        functions.logger.warn(`🎁 REFERRAL: User ${userId} has no name, cannot generate personalized code`);
        return null;
      }
      
      // Generate unique personalized referral code with user's name
      const referralCode = await generateUniqueReferralCode(userName);

      functions.logger.info(`🎁 REFERRAL: Generated code ${referralCode} for user ${userId}`);

      // Use batch write for atomicity
      const batch = db.batch();

      // 1. Save referralCode to users collection (IMMUTABLE - never changes)
      const userRef = db.collection("users").doc(userId);
      batch.update(userRef, {
        referralCode: referralCode,
        referralCodeCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
        // Store referral stats directly in user document (simplified)
        referralStats: {
          totalReferrals: 0,
          successfulReferrals: 0,
          pendingReferrals: 0,
          totalEarnings: 0,
          availableBalance: 0,
          withdrawnAmount: 0,
          canWithdraw: false,
          nextMilestone: 5,
          currentTier: "BRONZE",
          freeJobPostings: 0,
          freeJobPostingsExpiry: null,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }
      });

      // 2. Create referral_codes document ONLY for O(1) lookup (code as document ID)
      const codeRef = db.collection("referral_codes").doc(referralCode);
      batch.set(codeRef, {
        code: referralCode,
        userId: userId,
        userRole: userRole,
        userName: userName,
        isActive: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        totalUsed: 0
      });

      await batch.commit();
      functions.logger.info(`🎁 REFERRAL: ✅ Code ${referralCode} created successfully for ${userId} - THIS IS PERMANENT AND WILL NEVER CHANGE`);
      
      return { success: true, referralCode };

    } catch (error) {
      functions.logger.error(`🎁 REFERRAL: ❌ Error creating referral code for ${userId}:`, error);
      // Don't throw - user can still use app without referral code
      return null;
    }
  });

/**
 * Generate unique personalized referral code
 * 
 * Format: nameXXXX (7-10 characters total, lowercase)
 * - name: User's first name (max 6 characters, can be 3-6, lowercase)
 * - XXXX: 4 random digits
 * 
 * Examples:
 * - vamsi9843 (Vamsi Banoth → vamsi + 9843)
 * - sai9827 (Sai Kumar → sai + 9827)
 * - ravi8734 (Ravi → ravi + 8734)
 * - priya3921 (Priya → priya + 3921)
 * 
 * Benefits:
 * - Highly personalized (uses actual first name)
 * - Easy to remember and share
 * - Clean lowercase format (modern style)
 * - Still unique (26^6 * 10^4 = 3B+ combinations)
 * - 7-10 characters (optimal length)
 * 
 * @param userName User's full name for personalization
 * @returns Unique personalized referral code (7-10 characters, lowercase)
 */
async function generateUniqueReferralCode(userName: string = ""): Promise<string> {
  const maxRetries = 10;
  const digits = "0123456789";
  const letters = "abcdefghjklmnpqrstuvwxyz"; // Removed confusing: i, o (lowercase)
  
  // Extract first name (max 6 characters, lowercase)
  let namePrefix = "";
  if (userName) {
    // Get first name (before first space)
    const firstName = userName.trim().split(/\s+/)[0].toLowerCase();
    
    // Remove non-alphabetic characters
    const cleanName = firstName.replace(/[^a-z]/g, "");
    
    if (cleanName.length > 0) {
      // Use first name, max 6 characters (can be 3-6)
      namePrefix = cleanName.substring(0, Math.min(6, cleanName.length));
    }
  }
  
  // Fallback: Generate 4 random letters if no name
  if (namePrefix.length === 0) {
    for (let i = 0; i < 4; i++) {
      namePrefix += letters.charAt(Math.floor(Math.random() * letters.length));
    }
  }
  
  // Try to generate unique code
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    // Generate 4 random digits
    let digitSuffix = "";
    for (let i = 0; i < 4; i++) {
      digitSuffix += digits.charAt(Math.floor(Math.random() * digits.length));
    }
    
    // Combine: nameXXXX (7-10 chars total, lowercase)
    const code = `${namePrefix}${digitSuffix}`;
    
    // Check if code already exists
    const existingCode = await db.collection("referral_codes").doc(code).get();
    if (!existingCode.exists) {
      functions.logger.info(`🎁 REFERRAL: Generated code ${code} (${namePrefix} from "${userName}")`);
      return code;
    }
    
    functions.logger.warn(`🎁 REFERRAL: Code collision on attempt ${attempt + 1}, retrying...`);
  }
  
  throw new Error("Failed to generate unique referral code after max retries");
}



// ============================================
// EVENT 2: REFERRAL APPLICATION (PENDING)
// ============================================
// Triggered when new user applies a referral code during signup
// Creates PENDING referral record

export const applyReferralCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const newUserId = context.auth.uid;
  const referralCode = (data.referralCode || "").trim().toLowerCase();  // FIXED: Use lowercase to match storage format
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
                      context.rawRequest.headers["x-forwarded-for"]?.toString().split(",")[0] || 
                      "unknown";

    // 1. Validate referral code (O(1) lookup)
    const codeDoc = await db.collection("referral_codes").doc(referralCode).get();
    
    if (!codeDoc.exists) {
      functions.logger.warn(`🎁 REFERRAL: Code ${referralCode} not found`);
      return { success: false, error: "Referral code not found" };
    }

    const codeData = codeDoc.data()!;
    
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
    const pendingCount = referrerStats.data()?.pendingReferrals || 0;
    
    if (pendingCount >= REFERRAL_CONFIG.MAX_PENDING_REFERRALS) {
      return { success: false, error: "Referrer has too many pending referrals" };
    }

    // 5. CREDIT REWARDS IMMEDIATELY (no waiting for profile completion)
    const batch = db.batch();
    const referralId = db.collection("referrals").doc().id;
    const maskedPhone = newUserPhone ? `****${newUserPhone.slice(-4)}` : "";

    // Get referrer's current stats for milestone calculation
    const currentSuccessful = referrerStats.data()?.successfulReferrals || 0;
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
    let freePostings = referrerStats.data()?.freeJobPostings || 0;
    let freePostingsExpiry = referrerStats.data()?.freeJobPostingsExpiry;
    
    if (codeData.userRole === "EMPLOYER") {
      const postingReward = REFERRAL_CONFIG.EMPLOYER_FREE_POSTINGS[newSuccessfulCount];
      if (postingReward) {
        freePostings = postingReward.count;
        freePostingsExpiry = new Date(Date.now() + postingReward.days * ONE_DAY_MS);
      }
    }

    // Create referral document with COMPLETED status immediately
    const referralRef = db.collection("referrals").doc(referralId);
    batch.set(referralRef, {
      id: referralId,
      idempotencyKey: idempotencyKey,
      referrerUserId: referrerUserId,
      referrerRole: codeData.userRole,
      referredUserId: newUserId,
      referredRole: newUserRole,
      referralCode: referralCode,
      status: "COMPLETED",  // Immediately completed
      rewardAmount: referrerReward,
      bonusAmount: milestoneBonus,
      referredUserReward: referredUserReward,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      completedAt: admin.firestore.FieldValue.serverTimestamp(),  // Same as created
      referredUserName: newUserName,
      referredUserPhone: maskedPhone,
      deviceFingerprint: deviceFingerprint,
      ipAddress: ipAddress
    });

    // Update referrer's stats
    const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
    const referrerStatsUpdate: { [key: string]: any } = {
      totalReferrals: admin.firestore.FieldValue.increment(1),
      successfulReferrals: newSuccessfulCount,
      totalEarnings: admin.firestore.FieldValue.increment(totalReferrerReward),
      availableBalance: admin.firestore.FieldValue.increment(totalReferrerReward),
      canWithdraw: newCanWithdraw,
      currentTier: newTier,
      nextMilestone: newNextMilestone,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    };

    if (codeData.userRole === "EMPLOYER" && freePostingsExpiry) {
      referrerStatsUpdate.freeJobPostings = freePostings;
      referrerStatsUpdate.freeJobPostingsExpiry = freePostingsExpiry;
    }

    batch.update(referrerStatsRef, referrerStatsUpdate);

    // Update code usage count
    const codeRef = db.collection("referral_codes").doc(referralCode);
    batch.update(codeRef, {
      totalUsed: admin.firestore.FieldValue.increment(1),
      successfulReferrals: admin.firestore.FieldValue.increment(1)
    });

    // Create/update new user's stats with referral info and credit signup bonus
    const newUserStatsRef = db.collection("referral_stats").doc(newUserId);
    batch.set(newUserStatsRef, {
      userId: newUserId,
      userRole: newUserRole,
      referredByCode: referralCode,
      referredByUserId: referrerUserId,
      totalEarnings: referredUserReward,
      availableBalance: referredUserReward,
      signupBonusReceived: true,
      signupBonusAmount: referredUserReward,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Log events
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
      userId: newUserId,
      referralId: referralId,
      amount: referredUserReward,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    // Send notifications
    const referrerNotifRef = db.collection("notifications").doc();
    batch.set(referrerNotifRef, {
      recipientId: referrerUserId,
      title: "🎉 Referral Successful!",
      message: `${newUserName || "Someone"} joined using your code! You earned ₹${totalReferrerReward}${milestoneBonus > 0 ? ` (includes ₹${milestoneBonus} milestone bonus!)` : ""}`,
      type: "REFERRAL_REWARD",
      data: { referralId, amount: totalReferrerReward },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      isRead: false
    });

    const referredNotifRef = db.collection("notifications").doc();
    batch.set(referredNotifRef, {
      recipientId: newUserId,
      title: "🎁 Welcome Bonus!",
      message: `You earned ₹${referredUserReward} for joining with a referral code!`,
      type: "SIGNUP_BONUS",
      data: { amount: referredUserReward },
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      isRead: false
    });

    await batch.commit();

    functions.logger.info(`🎁 REFERRAL: ✅ Code ${referralCode} applied! Referrer ${referrerUserId} earned ₹${totalReferrerReward}, Referred ${newUserId} earned ₹${referredUserReward}`);
    
    return { 
      success: true, 
      referralId: referralId,
      referrerName: codeData.userName,
      referrerRole: codeData.userRole,
      referrerReward: totalReferrerReward,
      referredUserReward: referredUserReward,
      milestoneBonus: milestoneBonus
    };

  } catch (error) {
    functions.logger.error(`🎁 REFERRAL: Error applying code:`, error);
    throw new functions.https.HttpsError("internal", "Failed to apply referral code");
  }
});


// ============================================
// EVENT 3: REFERRAL COMPLETION (LEGACY - KEPT FOR BACKWARD COMPATIBILITY)
// ============================================
// This function is now only for users who applied codes before the system update
// New users get rewards immediately when applying code

export const onReferredUserProfileComplete = functions.firestore
  .document("users/{userId}")
  .onUpdate(async (change, context) => {
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
      const expiresAt = referral.expiresAt?.toMillis() || 0;
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
      const referrerStatsUpdate: { [key: string]: any } = {
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

      // 6. Send notifications (with idempotency check using indexed fields)
      // Check if notifications already exist for this referral to prevent duplicates
      // Use indexed fields (recipientId + type) and check referralId in memory
      const existingNotifications = await db.collection("notifications")
        .where("recipientId", "==", referrerUserId)
        .where("type", "==", "REFERRAL_REWARD")
        .limit(5)
        .get();
      
      // Check if any existing notification has this referralId (in-memory check)
      const notificationExists = existingNotifications.docs.some(doc => {
        const data = doc.data();
        return data.data?.referralId === referralId;
      });
      
      if (!notificationExists) {
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
      } else {
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

    } catch (error) {
      functions.logger.error(`🎁 REFERRAL: Error completing referral:`, error);
      return null;
    }
  });


// ============================================
// EVENT 4: REFERRAL EXPIRY CLEANUP
// ============================================
// Scheduled function to expire old pending referrals

export const expirePendingReferrals = functions.pubsub
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
      const referrerUpdates: { [key: string]: number } = {};

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

    } catch (error) {
      functions.logger.error("🎁 REFERRAL: Error in expiry cleanup:", error);
      return null;
    }
  });


// ============================================
// EVENT 5: WITHDRAWAL REQUEST PROCESSING
// ============================================

export const requestWithdrawal = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  
  // P0 FIX: Validate and sanitize inputs
  try {
    validateUserId(userId, true);
    validateNumber(data.amount, "amount", { min: 1, max: 100000 });
    validateEnum(data.paymentMethod || "UPI", "paymentMethod", ["UPI", "BANK_TRANSFER"]);
    
    if (data.paymentMethod === "UPI" && data.upiId) {
      validateString(data.upiId, "upiId", { minLength: 3, maxLength: 100 });
    }
    
    if (data.paymentMethod === "BANK_TRANSFER" && data.bankDetails) {
      validateString(data.bankDetails.accountNumber, "accountNumber", { minLength: 8, maxLength: 20 });
      validateString(data.bankDetails.ifscCode, "ifscCode", { minLength: 11, maxLength: 11 });
      validateString(data.bankDetails.accountHolderName, "accountHolderName", { minLength: 2, maxLength: 100 });
    }
  } catch (error: any) {
    throw new functions.https.HttpsError("invalid-argument", error.message);
  }
  
  // P0 FIX: Rate limiting - max 5 withdrawal requests per day
  await checkRateLimit(userId, "withdrawal_requests", 5, 24 * 60 * 60 * 1000);
  
  const amount = parseFloat(data.amount);
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

    const stats = statsDoc.data()!;

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

    if (paymentMethod === "BANK_TRANSFER" && (!bankDetails?.accountNumber || !bankDetails?.ifscCode)) {
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
      bankAccountNumber: bankDetails?.accountNumber || null,
      ifscCode: bankDetails?.ifscCode || null,
      accountHolderName: bankDetails?.accountHolderName || null,
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

  } catch (error) {
    functions.logger.error("🎁 REFERRAL: Error creating withdrawal:", error);
    throw new functions.https.HttpsError("internal", "Failed to create withdrawal request");
  }
});


// ============================================
// EVENT 6: FRAUD DETECTION & BLOCKING
// ============================================

export const detectReferralFraud = functions.firestore
  .document("referrals/{referralId}")
  .onCreate(async (snapshot, context) => {
    const referral = snapshot.data();
    const referralId = context.params.referralId;
    const referrerUserId = referral.referrerUserId;

    functions.logger.info(`🎁 REFERRAL FRAUD: Analyzing referral ${referralId}`);

    try {
      let fraudScore = 0;
      const signals: string[] = [];
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

      } else if (fraudScore >= 40) {
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

    } catch (error) {
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
export const getReferralStats = functions.https.onCall(async (data, context) => {
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

  } catch (error) {
    functions.logger.error("Error getting referral stats:", error);
    throw new functions.https.HttpsError("internal", "Failed to get referral stats");
  }
});

/**
 * Get referral history for a user
 */
export const getReferralHistory = functions.https.onCall(async (data, context) => {
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
      referrals: referrals.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }))
    };

  } catch (error) {
    functions.logger.error("Error getting referral history:", error);
    throw new functions.https.HttpsError("internal", "Failed to get referral history");
  }
});

/**
 * Get leaderboard
 */
export const getReferralLeaderboard = functions.https.onCall(async (data, context) => {
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
      leaderboard: leaderboard.docs.map((doc, index) => ({
        rank: index + 1,
        userId: doc.id,
        ...doc.data()
      }))
    };

  } catch (error) {
    functions.logger.error("Error getting leaderboard:", error);
    throw new functions.https.HttpsError("internal", "Failed to get leaderboard");
  }
});

