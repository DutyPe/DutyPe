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
 * - referrals/{referralId}/audit_logs: Event sourcing for audit and replay
 * - users/{userId}/withdrawals: Withdrawal tracking with admin approval
 * - referral_stats: Per-user referral statistics
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
  REWARD_PER_REFERRAL: 25,           // â‚¹25 per successful referral
  SIGNUP_BONUS: 25,                   // â‚¹25 for new user who uses code
  MIN_WITHDRAWAL: 50,                 // Minimum â‚¹50 to withdraw
  MAX_WITHDRAWAL_PER_DAY: 1000,       // Max â‚¹1000 per day
  
  // Milestone bonuses
  MILESTONES: {
    5: 50,    // 5 referrals = â‚¹50 bonus
    10: 100,  // 10 referrals = â‚¹100 bonus
    15: 150,  // 15 referrals = â‚¹150 bonus
    25: 250,  // 25 referrals = â‚¹250 bonus
    50: 500,  // 50 referrals = â‚¹500 bonus
    100: 1000 // 100 referrals = â‚¹1000 bonus
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
const CANONICAL_REFERRAL_PREFIX = "DUTY";
const CANONICAL_REFERRAL_LENGTH = 8;
const REFERRAL_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const USER_WITHDRAWALS_SUBCOLLECTION = "withdrawals";
const REFERRAL_AUDIT_SUBCOLLECTION = "audit_logs";
const DEFAULT_REFERRAL_STATS: { [key: string]: any } = {
  totalReferrals: 0,
  successfulReferrals: 0,
  pendingReferrals: 0,
  expiredReferrals: 0,
  rejectedReferrals: 0,
  totalEarnings: 0,
  pendingEarnings: 0,
  withdrawnAmount: 0,
  availableBalance: 0,
  canWithdraw: false,
  nextMilestone: 5,
  currentTier: "BRONZE",
  freeJobPostings: 0,
  freeJobPostingsExpiry: null,
  signupBonusReceived: false,
  signupBonusAmount: 0,
  totalWithdrawals: 0,
  isBlocked: false,
  blockReason: null
};

type ReferralFraudResult = {
  allowed: boolean;
  fraudScore: number;
  signals: string[];
  needsReview: boolean;
  rejectionReason?: string;
  errorMessage?: string;
};


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

function normalizeReferralCodeInput(code: string): string {
  return (code || "").replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
}

function getNumberValue(value: any, fallback = 0): number {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

function getBooleanValue(value: any, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

function getStringValue(value: any, fallback = ""): string {
  return typeof value === "string" && value.trim() ? value : fallback;
}

/**
 * Compute worker profile completion percentage (0-100)
 * Uses only target schema fields from users + worker_profiles.
 */
function computeWorkerProfileCompletion(user: any = {}): number {
  let score = 0;
  if (getStringValue(user.fullName)) score += 30;
  if (getStringValue(user.phone)) score += 30;
  if (getStringValue(user.profileImageUrl)) score += 20;
  if (user.location?.lat && user.location?.lng) score += 20;
  return Math.min(score, 100);
}

/**
 * Compute employer profile completion percentage (0-100)
 * Uses only target schema fields from users + employer_profiles.
 */
function computeEmployerProfileCompletion(user: any = {}): number {
  let score = 0;
  if (getStringValue(user.fullName)) score += 30;
  if (getStringValue(user.phone)) score += 30;
  if (getStringValue(user.profileImageUrl)) score += 20;
  if (user.location?.lat && user.location?.lng) score += 20;
  return Math.min(score, 100);
}

/**
 * Check if profile is considered complete (>= 80%)
 */
function isProfileComplete(user: any = {}): boolean {
  const role = getStringValue(user.activeRole, "WORKER").toUpperCase();
  const completion = role === "EMPLOYER"
    ? computeEmployerProfileCompletion(user)
    : computeWorkerProfileCompletion(user);
  return completion >= 80;
}

function getCombinedReferralStats(userData: any = {}, legacyStats: any = {}) {
  return {
    ...legacyStats,
    ...(userData?.referralStats || {})
  };
}

function referralAuditDocRef(referralId: string) {
  return db.collection("referrals")
    .doc(referralId)
    .collection(REFERRAL_AUDIT_SUBCOLLECTION)
    .doc();
}

function userReferralAuditDocRef(userId: string) {
  return db.collection("users")
    .doc(userId)
    .collection(REFERRAL_AUDIT_SUBCOLLECTION)
    .doc();
}

function userWithdrawalsCollection(userId: string) {
  return db.collection("users")
    .doc(userId)
    .collection(USER_WITHDRAWALS_SUBCOLLECTION);
}

function buildMissingReferralStatsUpdates(existingStats: any = {}) {
  const updates: { [key: string]: any } = {};

  for (const [field, defaultValue] of Object.entries(DEFAULT_REFERRAL_STATS)) {
    if (existingStats[field] === undefined) {
      updates[`referralStats.${field}`] = defaultValue;
    }
  }

  return updates;
}

async function getReferralCodeLookup(rawCode: string) {
  const normalizedCode = normalizeReferralCodeInput(rawCode);
  const candidates = Array.from(new Set([
    normalizedCode,
    normalizedCode.toLowerCase()
  ])).filter(Boolean);

  for (const candidate of candidates) {
    const doc = await db.collection("referrals").doc(candidate).get();
    if (doc.exists) {
      return {
        doc,
        codeId: candidate,
        data: doc.data()!
      };
    }
  }

  return null;
}

async function evaluateReferralFraud(params: {
  referrerUserId: string;
  deviceFingerprint?: string | null;
  ipAddress?: string | null;
}): Promise<ReferralFraudResult> {
  const { referrerUserId, deviceFingerprint, ipAddress } = params;
  let fraudScore = 0;
  const signals: string[] = [];
  const now = Date.now();
  const oneDayAgo = new Date(now - ONE_DAY_MS);
  const oneHourAgo = new Date(now - ONE_HOUR_MS);

  const recentReferrals = await db.collection("referrals")
    .where("referrerId", "==", referrerUserId)
    .where("createdAt", ">", oneHourAgo)
    .get();

  if (recentReferrals.size > 10) {
    fraudScore += 40;
    signals.push("HIGH_VELOCITY");
  }

  if (deviceFingerprint) {
    const sameDeviceReferrals = await db.collection("referrals")
      .where("deviceFingerprint", "==", deviceFingerprint)
      .where("createdAt", ">", oneDayAgo)
      .get();

    if (sameDeviceReferrals.size > 2) {
      fraudScore += 50;
      signals.push("SAME_DEVICE_MULTIPLE_REFERRALS");
    }
  }

  if (ipAddress && ipAddress !== "unknown") {
    const sameIpReferrals = await db.collection("referrals")
      .where("ipAddress", "==", ipAddress)
      .where("createdAt", ">", oneDayAgo)
      .get();

    if (sameIpReferrals.size >= REFERRAL_CONFIG.SAME_IP_MAX_REFERRALS) {
      return {
        allowed: false,
        fraudScore,
        signals: [...signals, "SAME_IP_LIMIT"],
        needsReview: false,
        rejectionReason: "SAME_IP_LIMIT",
        errorMessage: "Too many referrals from this network"
      };
    }

    if (sameIpReferrals.size > 2) {
      fraudScore += 30;
      signals.push("SAME_IP_MULTIPLE_REFERRALS");
    }
  }

  const [referrerUserDoc, referrerStatsDoc] = await Promise.all([
    db.collection("users").doc(referrerUserId).get(),
    db.collection("users").doc(referrerUserId).get()
  ]);
  const referrerStats = getCombinedReferralStats(referrerUserDoc.data() || {}, referrerStatsDoc.data() || {});
  const totalReferrals = getNumberValue(referrerStats.totalReferrals);
  const rejectedReferrals = getNumberValue(referrerStats.rejectedReferrals);

  if (getBooleanValue(referrerStats.isBlocked)) {
    return {
      allowed: false,
      fraudScore: 100,
      signals: ["REFERRER_BLOCKED"],
      needsReview: false,
      rejectionReason: "REFERRER_BLOCKED",
      errorMessage: "Referral account is restricted"
    };
  }

  if (totalReferrals > 10 && rejectedReferrals / totalReferrals > 0.3) {
    fraudScore += 25;
    signals.push("HIGH_REJECTION_RATE");
  }

  if (fraudScore >= 70) {
    return {
      allowed: false,
      fraudScore,
      signals,
      needsReview: false,
      rejectionReason: signals.join(", "),
      errorMessage: "Referral could not be processed"
    };
  }

  return {
    allowed: true,
    fraudScore,
    signals,
    needsReview: fraudScore >= 40
  };
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
    // Check if profile is now complete (was previously incomplete)
    const afterIsComplete = isProfileComplete(after);
    const beforeIsComplete = isProfileComplete(before);
    if (!afterIsComplete || beforeIsComplete) {
      return null;
    }
    if (before.referralCode || after.referralCode) {
      return null;
    }
    const userRole = after.activeRole || "WORKER";
    const userName = after.fullName || after.companyName || "";
    if (!userName) {
      functions.logger.warn("REFERRAL: User " + userId + " has no name, cannot generate referral code");
      return null;
    }
    try {
      const userRef = db.collection("users").doc(userId);
      for (let attempt = 0; attempt < 10; attempt++) {
        const referralCode = await generateUniqueReferralCode();
        try {
          const resolvedCode = await db.runTransaction(async (transaction) => {
            const latestUserDoc = await transaction.get(userRef);
            if (!latestUserDoc.exists) {
              return null;
            }
            const latestUser = latestUserDoc.data() || {};
            if (latestUser.referralCode) {
              return latestUser.referralCode as string;
            }
            const codeRef = db.collection("referrals").doc(referralCode);
            const codeDoc = await transaction.get(codeRef);
            if (codeDoc.exists) {
              throw new Error("REFERRAL_CODE_COLLISION");
            }
            const existingStats = latestUser.referralStats || {};
            transaction.update(userRef, {
              referralCode,
              referralCodeCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
              "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp(),
              ...buildMissingReferralStatsUpdates(existingStats)
            });
            transaction.set(db.collection("users").doc(userId), {
              userId,
              userRole,
              ...DEFAULT_REFERRAL_STATS,
              ...existingStats,
              lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }, { merge: true });
            transaction.set(codeRef, {
              code: referralCode,
              userId,
              userRole,
              userName,
              isActive: true,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              totalUsed: 0,
              successfulReferrals: 0
            });
            return referralCode;
          });
          if (resolvedCode) {
            functions.logger.info("REFERRAL: Ensured code " + resolvedCode + " for user " + userId);
            return { success: true, referralCode: resolvedCode };
          }
        } catch (error: any) {
          if (error?.message !== "REFERRAL_CODE_COLLISION") {
            throw error;
          }
        }
      }
      throw new Error("Failed to reserve canonical referral code");
    } catch (error) {
      functions.logger.error("REFERRAL: Error creating referral code for " + userId + ":", error);
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
 * - vamsi9843 (Vamsi Banoth â†’ vamsi + 9843)
 * - sai9827 (Sai Kumar â†’ sai + 9827)
 * - ravi8734 (Ravi â†’ ravi + 8734)
 * - priya3921 (Priya â†’ priya + 3921)
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
async function generateUniqueReferralCode(): Promise<string> {
  const maxRetries = 10;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    let suffix = "";
    while (suffix.length < CANONICAL_REFERRAL_LENGTH - CANONICAL_REFERRAL_PREFIX.length) {
      const index = Math.floor(Math.random() * REFERRAL_CODE_ALPHABET.length);
      suffix += REFERRAL_CODE_ALPHABET.charAt(index);
    }

    const code = `${CANONICAL_REFERRAL_PREFIX}${suffix}`;
    const [exactMatch, legacyCaseMatch] = await Promise.all([
      db.collection("referrals").doc(code).get(),
      db.collection("referrals").doc(code.toLowerCase()).get()
    ]);

    if (!exactMatch.exists && !legacyCaseMatch.exists) {
      return code;
    }
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

  try {
    validateUserId(newUserId, true);
    validateString(data.referralCode || "", "referralCode", { minLength: 7, maxLength: 10 });
    validateEnum(data.userRole || "WORKER", "userRole", ["WORKER", "EMPLOYER"]);

    if (data.userName) {
      validateString(data.userName, "userName", { minLength: 1, maxLength: 120 });
    }

    if (data.userPhone) {
      validateString(data.userPhone, "userPhone", { minLength: 4, maxLength: 30 });
    }
  } catch (error: any) {
    throw new functions.https.HttpsError("invalid-argument", error.message);
  }

  const requestedCode = normalizeReferralCodeInput(data.referralCode || "");
  const newUserRole = getStringValue(data.userRole, "WORKER").toUpperCase();
  const newUserName = getStringValue(data.userName, "");
  const newUserPhone = getStringValue(data.userPhone, "");
  const deviceFingerprint = getStringValue(data.deviceFingerprint, "") || null;

  if (!requestedCode || !/^[A-Z0-9]{7,10}$/.test(requestedCode)) {
    return { success: false, error: "Invalid referral code format" };
  }

  try {
    const ipAddress = context.rawRequest.ip ||
      context.rawRequest.headers["x-forwarded-for"]?.toString().split(",")[0] ||
      "unknown";

    const codeLookup = await getReferralCodeLookup(requestedCode);
    if (!codeLookup) {
      return { success: false, error: "Referral code not found" };
    }

    const codeData = codeLookup.data;
    if (!codeData.isActive) {
      return { success: false, error: "This referral code is no longer active" };
    }

    const referrerUserId = getStringValue(codeData.userId);
    if (!referrerUserId) {
      return { success: false, error: "Invalid referral code" };
    }

    if (referrerUserId === newUserId) {
      return { success: false, error: "Cannot use your own referral code" };
    }

    const fraudResult = await evaluateReferralFraud({
      referrerUserId,
      deviceFingerprint,
      ipAddress
    });

    if (!fraudResult.allowed) {
      return {
        success: false,
        error: fraudResult.errorMessage || "Referral could not be processed"
      };
    }

    const codeRef = codeLookup.doc.ref;
    const referrerUserRef = db.collection("users").doc(referrerUserId);
    const newUserRef = db.collection("users").doc(newUserId);
    const referrerStatsRef = db.collection("users").doc(referrerUserId);
    const newUserStatsRef = db.collection("users").doc(newUserId);

    const result = await db.runTransaction(async (transaction) => {
      const existingReferralQuery = db.collection("referrals")
        .where("referredUserId", "==", newUserId)
        .limit(1);

      const [latestCodeDoc, referrerUserDoc, newUserDoc, referrerLegacyStatsDoc, newUserLegacyStatsDoc, existingReferralSnapshot] = await Promise.all([
        transaction.get(codeRef),
        transaction.get(referrerUserRef),
        transaction.get(newUserRef),
        transaction.get(referrerStatsRef),
        transaction.get(newUserStatsRef),
        transaction.get(existingReferralQuery)
      ]);

      if (!latestCodeDoc.exists) {
        return { success: false, error: "Referral code not found" };
      }

      const latestCodeData = latestCodeDoc.data() || {};
      if (!latestCodeData.isActive) {
        return { success: false, error: "This referral code is no longer active" };
      }

      const latestReferrerUserId = getStringValue(latestCodeData.userId);
      if (!latestReferrerUserId || latestReferrerUserId === newUserId) {
        return { success: false, error: "Cannot use your own referral code" };
      }

      if (!referrerUserDoc.exists || !newUserDoc.exists) {
        return { success: false, error: "User account not ready yet" };
      }

      const referralCode = getStringValue(latestCodeData.code, latestCodeDoc.id);
      const referrerUserData = referrerUserDoc.data() || {};
      const newUserData = newUserDoc.data() || {};
      const referrerStats = getCombinedReferralStats(referrerUserData, referrerLegacyStatsDoc.data() || {});
      const newUserStats = getCombinedReferralStats(newUserData, newUserLegacyStatsDoc.data() || {});
      const existingReferredByCode = getStringValue(newUserStats.referredByCode);

      if (existingReferredByCode) {
        if (normalizeReferralCodeInput(existingReferredByCode) === requestedCode) {
          return { success: true, message: "Referral already applied" };
        }
        return { success: false, error: "You have already used a referral code" };
      }

      if (!existingReferralSnapshot.empty) {
        const existingReferral = existingReferralSnapshot.docs[0].data() || {};
        const existingCode = normalizeReferralCodeInput(getStringValue(existingReferral.referralCode));
        if (existingCode === requestedCode) {
          return { success: true, message: "Referral already applied" };
        }
        return { success: false, error: "You have already used a referral code" };
      }

      const currentSuccessful = getNumberValue(referrerStats.successfulReferrals);
      const newSuccessfulCount = currentSuccessful + 1;
      const referrerReward = REFERRAL_CONFIG.REWARD_PER_REFERRAL;
      const referredUserReward = REFERRAL_CONFIG.SIGNUP_BONUS;
      const milestoneBonus = getMilestoneBonus(newSuccessfulCount);
      const totalReferrerReward = referrerReward + milestoneBonus;
      const newTier = calculateTier(newSuccessfulCount);
      const newCanWithdraw = canWithdraw(newSuccessfulCount);
      const newNextMilestone = getNextMilestone(newSuccessfulCount);
      const referrerRole = getStringValue(referrerUserData.activeRole, "WORKER");
      const referrerName = getStringValue(
        referrerUserData.fullName || referrerUserData.companyName || latestCodeData.userName,
        "DutyPe User"
      );
      let freePostings = getNumberValue(referrerStats.freeJobPostings);
      let freePostingsExpiry = referrerStats.freeJobPostingsExpiry || null;

      if (referrerRole === "EMPLOYER") {
        const postingReward = REFERRAL_CONFIG.EMPLOYER_FREE_POSTINGS[newSuccessfulCount];
        if (postingReward) {
          freePostings = postingReward.count;
          freePostingsExpiry = new Date(Date.now() + postingReward.days * ONE_DAY_MS);
        }
      }

      const referralId = db.collection("referrals").doc().id;
      const maskedPhone = newUserPhone ? "****" + newUserPhone.slice(-4) : "";
      const idempotencyKey = generateIdempotencyKey(latestReferrerUserId, newUserId);
      const referralRef = db.collection("referrals").doc(referralId);

      transaction.set(referralRef, {
        id: referralId,
        idempotencyKey,
        referrerId: latestReferrerUserId,
        referredUserId: newUserId,
        status: "COMPLETED",
        reward: totalReferrerReward,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });

      const referrerUserUpdate: { [key: string]: any } = {
        ...buildMissingReferralStatsUpdates(referrerUserData.referralStats || {}),
        "referralStats.totalReferrals": admin.firestore.FieldValue.increment(1),
        "referralStats.successfulReferrals": newSuccessfulCount,
        "referralStats.totalEarnings": admin.firestore.FieldValue.increment(totalReferrerReward),
        "referralStats.availableBalance": admin.firestore.FieldValue.increment(totalReferrerReward),
        "referralStats.canWithdraw": newCanWithdraw,
        "referralStats.currentTier": newTier,
        "referralStats.nextMilestone": newNextMilestone,
        "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
      };

      if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
        referrerUserUpdate["referralStats.freeJobPostings"] = freePostings;
        referrerUserUpdate["referralStats.freeJobPostingsExpiry"] = freePostingsExpiry;
      }

      transaction.update(referrerUserRef, referrerUserUpdate);
      transaction.set(referrerStatsRef, {
        userId: latestReferrerUserId,
        userRole: referrerRole,
        totalReferrals: admin.firestore.FieldValue.increment(1),
        successfulReferrals: newSuccessfulCount,
        totalEarnings: admin.firestore.FieldValue.increment(totalReferrerReward),
        availableBalance: admin.firestore.FieldValue.increment(totalReferrerReward),
        canWithdraw: newCanWithdraw,
        currentTier: newTier,
        nextMilestone: newNextMilestone,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
        ...(referrerRole === "EMPLOYER" && freePostingsExpiry ? {
          freeJobPostings: freePostings,
          freeJobPostingsExpiry: freePostingsExpiry
        } : {})
      }, { merge: true });

      transaction.update(newUserRef, {
        ...buildMissingReferralStatsUpdates(newUserData.referralStats || {}),
        "referralStats.totalEarnings": admin.firestore.FieldValue.increment(referredUserReward),
        "referralStats.availableBalance": admin.firestore.FieldValue.increment(referredUserReward),
        "referralStats.signupBonusReceived": true,
        "referralStats.signupBonusAmount": referredUserReward,
        "referralStats.referredByCode": referralCode,
        "referralStats.referredByUserId": latestReferrerUserId,
        "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
      });
      transaction.set(newUserStatsRef, {
        userId: newUserId,
        userRole: newUserRole,
        referredByCode: referralCode,
        referredByUserId: latestReferrerUserId,
        totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
        availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
        signupBonusReceived: true,
        signupBonusAmount: referredUserReward,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      transaction.update(codeRef, {
        totalUsed: admin.firestore.FieldValue.increment(1),
        successfulReferrals: admin.firestore.FieldValue.increment(1),
        userRole: referrerRole,
        userName: referrerName
      });

      const referrerEventRef = referralAuditDocRef(referralId);
      transaction.set(referrerEventRef, {
        eventType: "REWARD_CREDITED",
        userId: latestReferrerUserId,
        userRole: getStringValue(referrerUserData.activeRole, "WORKER"),
        amount: totalReferrerReward,
        bonusAmount: milestoneBonus,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });

      const referredEventRef = referralAuditDocRef(referralId);
      transaction.set(referredEventRef, {
        eventType: "SIGNUP_BONUS_CREDITED",
        userId: newUserId,
        referralId,
        amount: referredUserReward,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });

      const referrerNotifRef = db.collection("notifications").doc();
      transaction.set(referrerNotifRef, {
        recipientId: latestReferrerUserId,
        title: "Referral Successful",
        message: (newUserName || "Someone") + " joined using your code. You earned Rs." + totalReferrerReward + (milestoneBonus > 0 ? " including Rs." + milestoneBonus + " milestone bonus" : "") + ".",
        type: "REFERRAL_REWARD",
        data: { referralId, amount: totalReferrerReward },
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false
      });

      const referredNotifRef = db.collection("notifications").doc();
      transaction.set(referredNotifRef, {
        recipientId: newUserId,
        title: "Welcome Bonus",
        message: "You earned Rs." + referredUserReward + " for joining with a referral code.",
        type: "SIGNUP_BONUS",
        data: { amount: referredUserReward },
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false
      });

      return {
        success: true,
        referralId,
        referrerName,
        referrerRole,
        referrerReward: totalReferrerReward,
        referredUserReward,
        milestoneBonus,
        message: fraudResult.needsReview ? "Referral applied and flagged for review" : "Referral applied successfully"
      };
    });

    return result;
  } catch (error) {
    functions.logger.error("REFERRAL: Error applying code:", error);
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
    const afterIsComplete = isProfileComplete(after);
    const beforeIsComplete = isProfileComplete(before);
    if (beforeIsComplete || !afterIsComplete) {
      return null;
    }

    functions.logger.info(`ðŸŽ REFERRAL: Checking pending referral for user ${referredUserId}`);

    try {
      // Find pending referral for this user
      const pendingReferrals = await db.collection("referrals")
        .where("referredUserId", "==", referredUserId)
        .where("status", "==", "PENDING")
        .limit(1)
        .get();

      if (pendingReferrals.empty) {
        functions.logger.info(`ðŸŽ REFERRAL: No pending referral for user ${referredUserId}`);
        return null;
      }

      const referralDoc = pendingReferrals.docs[0];
      const referral = referralDoc.data();
      const referralId = referralDoc.id;
      const referrerUserId = referral.referrerId;

      // Check if expired
      const expiresAt = referral.expiresAt?.toMillis() || 0;
      if (Date.now() > expiresAt) {
        functions.logger.info(`ðŸŽ REFERRAL: Referral ${referralId} expired`);
        
        // Mark as expired
        await db.collection("referrals").doc(referralId).update({
          status: "EXPIRED",
          completedAt: admin.firestore.FieldValue.serverTimestamp()
        });

        const referrerUserRef = db.collection("users").doc(referrerUserId);
        await db.runTransaction(async (transaction) => {
          transaction.set(db.collection("users").doc(referrerUserId), {
            pendingReferrals: admin.firestore.FieldValue.increment(-1),
            expiredReferrals: admin.firestore.FieldValue.increment(1),
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });
          transaction.update(referrerUserRef, {
            "referralStats.pendingReferrals": admin.firestore.FieldValue.increment(-1),
            "referralStats.expiredReferrals": admin.firestore.FieldValue.increment(1),
            "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
          });
        });

        return { status: "EXPIRED" };
      }

      // Get referrer's current stats for milestone calculation
      const [referrerUserDoc, referrerStatsDoc] = await Promise.all([
        db.collection("users").doc(referrerUserId).get(),
        db.collection("users").doc(referrerUserId).get()
      ]);
      const referrerUserData = referrerUserDoc.data() || {};
      const referrerStats = getCombinedReferralStats(referrerUserData, referrerStatsDoc.data() || {});
      const currentSuccessful = getNumberValue(referrerStats.successfulReferrals);
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
      const referrerRole = getStringValue(referrerUserData.activeRole, "WORKER");
      let freePostings = getNumberValue(referrerStats.freeJobPostings);
      let freePostingsExpiry = referrerStats.freeJobPostingsExpiry;
      
      if (referrerRole === "EMPLOYER") {
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
        reward: totalReferrerReward
      });

      // 2. Update referrer's stats in BOTH locations
      const referrerStatsRef = db.collection("users").doc(referrerUserId);
      const referrerStatsUpdate: { [key: string]: any } = {
        userId: referrerUserId,
        userRole: referrerRole,
        successfulReferrals: newSuccessfulCount,
        pendingReferrals: admin.firestore.FieldValue.increment(-1),
        totalEarnings: admin.firestore.FieldValue.increment(totalReferrerReward),
        availableBalance: admin.firestore.FieldValue.increment(totalReferrerReward),
        canWithdraw: newCanWithdraw,
        currentTier: newTier,
        nextMilestone: newNextMilestone,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      };

      if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
        referrerStatsUpdate.freeJobPostings = freePostings;
        referrerStatsUpdate.freeJobPostingsExpiry = freePostingsExpiry;
      }

      batch.set(referrerStatsRef, referrerStatsUpdate, { merge: true });

      // Also update users.referralStats
      const referrerUserRef = db.collection("users").doc(referrerUserId);
      const userStatsUpdate: { [key: string]: any } = {
        ...buildMissingReferralStatsUpdates(referrerUserData.referralStats || {}),
        "referralStats.successfulReferrals": newSuccessfulCount,
        "referralStats.pendingReferrals": admin.firestore.FieldValue.increment(-1),
        "referralStats.totalEarnings": admin.firestore.FieldValue.increment(totalReferrerReward),
        "referralStats.availableBalance": admin.firestore.FieldValue.increment(totalReferrerReward),
        "referralStats.canWithdraw": newCanWithdraw,
        "referralStats.currentTier": newTier,
        "referralStats.nextMilestone": newNextMilestone,
        "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
      };

      if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
        userStatsUpdate["referralStats.freeJobPostings"] = freePostings;
        userStatsUpdate["referralStats.freeJobPostingsExpiry"] = freePostingsExpiry;
      }

      batch.update(referrerUserRef, userStatsUpdate);

      // 3. Update referral code stats
      const codeRef = db.collection("referrals").doc(referral.referralCode);
      batch.update(codeRef, {
        successfulReferrals: admin.firestore.FieldValue.increment(1)
      });

      // 4. Credit referred user's signup bonus in BOTH locations
      const referredStatsRef = db.collection("users").doc(referredUserId);
      batch.set(referredStatsRef, {
        userId: referredUserId,
        userRole: getStringValue(after.activeRole, "WORKER"),
        totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
        availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
        signupBonusReceived: true,
        signupBonusAmount: referredUserReward,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      // Also update users.referralStats
      const referredUserRef = db.collection("users").doc(referredUserId);
      batch.update(referredUserRef, {
        ...buildMissingReferralStatsUpdates(after.referralStats || {}),
        "referralStats.totalEarnings": admin.firestore.FieldValue.increment(referredUserReward),
        "referralStats.availableBalance": admin.firestore.FieldValue.increment(referredUserReward),
        "referralStats.signupBonusReceived": true,
        "referralStats.signupBonusAmount": referredUserReward,
        "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
      });

      // 5. Log events
      const referrerEventRef = referralAuditDocRef(referralId);
      batch.set(referrerEventRef, {
        eventType: "REWARD_CREDITED",
        userId: referrerUserId,
        referralId: referralId,
        amount: totalReferrerReward,
        bonusAmount: milestoneBonus,
        newTier: newTier,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });

      const referredEventRef = referralAuditDocRef(referralId);
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
          title: "ðŸŽ‰ Referral Successful!",
          message: `${referral.referredUserName || "Someone"} joined using your code! You earned â‚¹${totalReferrerReward}${milestoneBonus > 0 ? ` (includes â‚¹${milestoneBonus} milestone bonus!)` : ""}`,
          type: "REFERRAL_REWARD",
          data: { referralId, amount: totalReferrerReward },
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false
        });

        const referredNotifRef = db.collection("notifications").doc();
        batch.set(referredNotifRef, {
          recipientId: referredUserId,
          title: "ðŸŽ Welcome Bonus!",
          message: `You earned â‚¹${referredUserReward} for joining with a referral code!`,
          type: "SIGNUP_BONUS",
          data: { amount: referredUserReward },
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false
        });
        
        functions.logger.info(`ðŸŽ REFERRAL: Creating notifications for referral ${referralId}`);
      } else {
        functions.logger.info(`ðŸŽ REFERRAL: Notifications already exist for referral ${referralId}, skipping`);
      }

      await batch.commit();

      functions.logger.info(`ðŸŽ REFERRAL: âœ… Completed! Referrer ${referrerUserId} earned â‚¹${totalReferrerReward}, Referred ${referredUserId} earned â‚¹${referredUserReward}`);

      return {
        status: "COMPLETED",
        referrerReward: totalReferrerReward,
        referredUserReward: referredUserReward,
        milestoneBonus: milestoneBonus,
        newTier: newTier
      };

    } catch (error) {
      functions.logger.error(`ðŸŽ REFERRAL: Error completing referral:`, error);
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
    functions.logger.info("ðŸŽ REFERRAL: Running expiry cleanup");

    try {
      const now = new Date();
      
      // Find expired pending referrals
      const expiredReferrals = await db.collection("referrals")
        .where("status", "==", "PENDING")
        .where("expiresAt", "<", now)
        .limit(500) // Process in batches
        .get();

      if (expiredReferrals.empty) {
        functions.logger.info("ðŸŽ REFERRAL: No expired referrals found");
        return null;
      }

      functions.logger.info(`ðŸŽ REFERRAL: Found ${expiredReferrals.size} expired referrals`);

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
        const referrerId = referral.referrerId;
        referrerUpdates[referrerId] = (referrerUpdates[referrerId] || 0) + 1;

        // Log event
        const eventRef = referralAuditDocRef(doc.id);
        batch.set(eventRef, {
          eventType: "REFERRAL_EXPIRED",
          referralId: doc.id,
          referrerId: referrerId,
          referredUserId: referral.referredUserId,
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
      }

      // Update referrer stats
      for (const [referrerId, expiredCount] of Object.entries(referrerUpdates)) {
        const statsRef = db.collection("users").doc(referrerId);
        batch.set(statsRef, {
          pendingReferrals: admin.firestore.FieldValue.increment(-expiredCount),
          expiredReferrals: admin.firestore.FieldValue.increment(expiredCount),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        batch.update(db.collection("users").doc(referrerId), {
          "referralStats.pendingReferrals": admin.firestore.FieldValue.increment(-expiredCount),
          "referralStats.expiredReferrals": admin.firestore.FieldValue.increment(expiredCount),
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        });
      }

      await batch.commit();

      functions.logger.info(`ðŸŽ REFERRAL: âœ… Expired ${expiredReferrals.size} referrals`);
      return { expiredCount: expiredReferrals.size };

    } catch (error) {
      functions.logger.error("ðŸŽ REFERRAL: Error in expiry cleanup:", error);
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

  await checkRateLimit(userId, "withdrawals", 5, 24 * 60 * 60 * 1000);

  const amount = parseFloat(data.amount);
  const paymentMethod = data.paymentMethod || "UPI";
  const upiId = data.upiId;
  const bankDetails = data.bankDetails;

  try {
    const userRef = db.collection("users").doc(userId);
    const legacyStatsRef = db.collection("users").doc(userId);
    const [userDoc, legacyStatsDoc] = await Promise.all([
      userRef.get(),
      legacyStatsRef.get()
    ]);

    if (!userDoc.exists) {
      return { success: false, error: "No referral stats found" };
    }

    const userData = userDoc.data() || {};
    const stats = getCombinedReferralStats(userData, legacyStatsDoc.data() || {});
    const availableBalance = getNumberValue(stats.availableBalance);
    const successfulReferrals = getNumberValue(stats.successfulReferrals);
    const canUserWithdraw = getBooleanValue(stats.canWithdraw, canWithdraw(successfulReferrals));

    if (getBooleanValue(stats.isBlocked)) {
      return { success: false, error: "Your account is blocked from withdrawals" };
    }

    if (!canUserWithdraw) {
      return { success: false, error: "You need at least 5 successful referrals to withdraw" };
    }

    if (amount < REFERRAL_CONFIG.MIN_WITHDRAWAL) {
      return { success: false, error: "Minimum withdrawal is Rs." + REFERRAL_CONFIG.MIN_WITHDRAWAL };
    }

    if (amount > availableBalance) {
      return { success: false, error: "Insufficient balance. Available: Rs." + availableBalance };
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayWithdrawals = await userWithdrawalsCollection(userId)
      .where("createdAt", ">=", today)
      .get();

    const todayTotal = todayWithdrawals.docs.reduce((sum, doc) => sum + getNumberValue(doc.data().amount), 0);
    if (todayTotal + amount > REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY) {
      return { success: false, error: "Daily limit is Rs." + REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY };
    }

    if (paymentMethod === "UPI" && !upiId) {
      return { success: false, error: "UPI ID is required" };
    }

    if (paymentMethod === "BANK_TRANSFER" && (!bankDetails?.accountNumber || !bankDetails?.ifscCode)) {
      return { success: false, error: "Bank account details are required" };
    }

    const userRole = getStringValue(userData.activeRole, "WORKER");
    const withdrawalId = userWithdrawalsCollection(userId).doc().id;
    const batch = db.batch();

    batch.set(userWithdrawalsCollection(userId).doc(withdrawalId), {
      id: withdrawalId,
      userId,
      userRole,
      amount,
      status: "PENDING",
      paymentMethod,
      upiId: upiId || null,
      bankAccountNumber: bankDetails?.accountNumber || null,
      ifscCode: bankDetails?.ifscCode || null,
      accountHolderName: bankDetails?.accountHolderName || null,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    batch.update(userRef, {
      "referralStats.availableBalance": admin.firestore.FieldValue.increment(-amount),
      "referralStats.withdrawnAmount": admin.firestore.FieldValue.increment(amount),
      "referralStats.lastWithdrawalAt": admin.firestore.FieldValue.serverTimestamp(),
      "referralStats.totalWithdrawals": admin.firestore.FieldValue.increment(1),
      "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
    });
    batch.set(legacyStatsRef, {
      userId,
      userRole,
      availableBalance: admin.firestore.FieldValue.increment(-amount),
      withdrawnAmount: admin.firestore.FieldValue.increment(amount),
      lastWithdrawalAt: admin.firestore.FieldValue.serverTimestamp(),
      totalWithdrawals: admin.firestore.FieldValue.increment(1),
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    batch.set(userReferralAuditDocRef(userId), {
      eventType: "WITHDRAWAL_REQUESTED",
      userId,
      withdrawalId,
      amount,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    await batch.commit();
    return { success: true, withdrawalId };
  } catch (error) {
    functions.logger.error("REFERRAL: Error creating withdrawal:", error);
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
    const referrerUserId = referral.referrerId;

    if (referral.fraudCheckedAt || referral.status !== "PENDING") {
      return null;
    }

    try {
      const fraudResult = await evaluateReferralFraud({
        referrerUserId,
        deviceFingerprint: referral.deviceFingerprint || null,
        ipAddress: referral.ipAddress || null
      });

      if (!fraudResult.allowed) {
        const batch = db.batch();
        batch.update(snapshot.ref, {
          status: "REJECTED",
          rejectionReason: fraudResult.rejectionReason || fraudResult.signals.join(", "),
          fraudScore: fraudResult.fraudScore,
          fraudSignals: fraudResult.signals,
          fraudCheckedAt: admin.firestore.FieldValue.serverTimestamp(),
          completedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        batch.set(db.collection("users").doc(referrerUserId), {
          pendingReferrals: admin.firestore.FieldValue.increment(-1),
          rejectedReferrals: admin.firestore.FieldValue.increment(1),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        batch.update(db.collection("users").doc(referrerUserId), {
          "referralStats.pendingReferrals": admin.firestore.FieldValue.increment(-1),
          "referralStats.rejectedReferrals": admin.firestore.FieldValue.increment(1),
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        });
        await batch.commit();
        return { status: "REJECTED", fraudScore: fraudResult.fraudScore, signals: fraudResult.signals };
      }

      await snapshot.ref.update({
        fraudScore: fraudResult.fraudScore,
        fraudSignals: fraudResult.signals,
        needsReview: fraudResult.needsReview,
        fraudCheckedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      return {
        status: fraudResult.needsReview ? "FLAGGED" : "PASSED",
        fraudScore: fraudResult.fraudScore,
        signals: fraudResult.signals
      };
    } catch (error) {
      functions.logger.error("REFERRAL FRAUD: Error analyzing referral " + referralId + ":", error);
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
    const [userDoc, legacyStatsDoc] = await Promise.all([
      db.collection("users").doc(userId).get(),
      db.collection("users").doc(userId).get()
    ]);

    if (!userDoc.exists && !legacyStatsDoc.exists) {
      return { exists: false };
    }

    const userData = userDoc.data() || {};
    const legacyStats = legacyStatsDoc.data() || {};
    const stats = {
      userId,
      userRole: getStringValue(userData.activeRole, "WORKER"),
      referralCode: getStringValue(userData.referralCode),
      ...DEFAULT_REFERRAL_STATS,
      ...legacyStats,
      ...(userData.referralStats || {})
    };

    return { exists: true, stats };
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
      .where("referrerId", "==", userId)
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
    let query = db.collection("users")
      .where("isBlocked", "==", false)
      .orderBy("successfulReferrals", "desc")
      .limit(limit);

    if (role) {
      query = db.collection("users")
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




