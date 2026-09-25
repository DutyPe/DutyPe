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
 * - referral_stats: Per-user referral statistics
 * 
 * @author DutyPe Engineering Team
 * @version 2.0.0 - Enterprise Edition with Security Hardening
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { createHash } from "crypto";
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
const CANONICAL_REFERRAL_PREFIX = "DUTY";
const CANONICAL_REFERRAL_LENGTH = 8;
const REFERRAL_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
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
  awardedMilestones: [],
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
 * Check if user can withdraw based on referral count OR minimum balance
 * Users can withdraw if they reached minimum balance (₹100) OR have at least 5 successful referrals.
 */
function canWithdraw(successfulReferrals: number, availableBalance: number = 0, minWithdrawal: number = 100): boolean {
  return availableBalance >= minWithdrawal || successfulReferrals >= 5;
}

/**
 * Get milestone bonus if applicable for an exact count
 */
function getMilestoneBonus(newCount: number): number {
  return REFERRAL_CONFIG.MILESTONES[newCount] || 0;
}

interface MilestoneAuditResult {
  toCredit: number;
  newAwardedMilestones: number[];
  awardedNow: number[];
}

/**
 * Audits a user's milestone bonuses to ensure no milestone bonuses are skipped or missed,
 * while preventing any double crediting.
 */
function auditUserMilestones(
  successfulReferrals: number,
  totalEarnings: number,
  signupBonusAmount: number = 0,
  existingAwardedMilestones: number[] = []
): MilestoneAuditResult {
  const milestoneList = [
    { count: 5, bonus: 50 },
    { count: 10, bonus: 100 },
    { count: 15, bonus: 150 },
    { count: 25, bonus: 250 },
    { count: 50, bonus: 500 },
    { count: 100, bonus: 1000 },
  ];

  const awarded = new Set<number>(existingAwardedMilestones || []);
  let toCredit = 0;
  const awardedNow: number[] = [];

  // Calculate base referral earnings without milestone bonuses
  const baseEarnings = (successfulReferrals * REFERRAL_CONFIG.REWARD_PER_REFERRAL) + signupBonusAmount;
  let extraEarnings = Math.max(0, totalEarnings - baseEarnings);

  for (const m of milestoneList) {
    if (successfulReferrals >= m.count) {
      if (awarded.has(m.count)) {
        continue;
      }
      if (extraEarnings >= m.bonus) {
        // Milestone bonus was already included in past totalEarnings
        extraEarnings -= m.bonus;
        awarded.add(m.count);
      } else {
        // Milestone bonus was NOT yet credited
        toCredit += m.bonus;
        awarded.add(m.count);
        awardedNow.push(m.count);
      }
    }
  }

  return {
    toCredit,
    newAwardedMilestones: Array.from(awarded).sort((a, b) => a - b),
    awardedNow
  };
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

function getCombinedReferralStats(userData: any = {}, legacyStats: any = {}) {
  return {
    ...legacyStats,
    ...(userData?.referralStats || {})
  };
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
    const doc = await db.collection("referral_codes").doc(candidate).get();
    if (doc.exists) {
      return {
        doc,
        codeId: candidate,
        data: doc.data()!
      };
    }
  }

  // Fallback: check if the referral code exists on a user record or referral_stats
  try {
    const [userQuery, statsQuery] = await Promise.all([
      db.collection("users").where("referralCode", "==", normalizedCode).limit(1).get(),
      db.collection("referral_stats").where("referralCode", "==", normalizedCode).limit(1).get()
    ]);

    let foundUserId = "";
    let foundUserName = "";
    let foundUserRole = "WORKER";

    if (!userQuery.empty) {
      const uDoc = userQuery.docs[0];
      const uData = uDoc.data();
      foundUserId = uDoc.id;
      foundUserName = uData.fullName || uData.name || uData.companyName || "DutyPe User";
      foundUserRole = uData.activeRole || uData.role || "WORKER";
    } else if (!statsQuery.empty) {
      const sDoc = statsQuery.docs[0];
      const sData = sDoc.data();
      foundUserId = sDoc.id;
      foundUserRole = sData.userRole || "WORKER";
      const uDoc = await db.collection("users").doc(foundUserId).get();
      const uData = uDoc.data() || {};
      foundUserName = uData.fullName || uData.name || uData.companyName || "DutyPe User";
    }

    if (foundUserId) {
      const codeRef = db.collection("referral_codes").doc(normalizedCode);
      const codeData = {
        code: normalizedCode,
        userId: foundUserId,
        userRole: foundUserRole,
        userName: foundUserName,
        isActive: true,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        totalUsed: 0,
        successfulReferrals: 0
      };
      await codeRef.set(codeData, { merge: true });
      const healedDoc = await codeRef.get();
      return {
        doc: healedDoc,
        codeId: normalizedCode,
        data: healedDoc.data()!
      };
    }
  } catch (lookupErr) {
    functions.logger.warn("REFERRAL: Fallback lookup error for code " + normalizedCode, lookupErr);
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
  const oneHourAgoMs = now - ONE_HOUR_MS;
  const oneDayAgoMs = now - ONE_DAY_MS;

  const recentReferralsSnap = await db.collection("referrals")
    .where("referrerUserId", "==", referrerUserId)
    .limit(50)
    .get();

  const recentReferralsCount = recentReferralsSnap.docs.filter((d) => {
    const ca = d.data().createdAt;
    const ms = ca?.toMillis ? ca.toMillis() : ca ? new Date(ca).getTime() : 0;
    return ms > oneHourAgoMs;
  }).length;

  if (recentReferralsCount > 10) {
    fraudScore += 40;
    signals.push("HIGH_VELOCITY");
  }

  if (deviceFingerprint) {
    const sameDeviceSnap = await db.collection("referrals")
      .where("deviceFingerprint", "==", deviceFingerprint)
      .limit(10)
      .get();

    const sameDeviceCount = sameDeviceSnap.docs.filter((d) => {
      const ca = d.data().createdAt;
      const ms = ca?.toMillis ? ca.toMillis() : ca ? new Date(ca).getTime() : 0;
      return ms > oneDayAgoMs;
    }).length;

    if (sameDeviceCount > 2) {
      fraudScore += 50;
      signals.push("SAME_DEVICE_MULTIPLE_REFERRALS");
    }
  }

  if (ipAddress && ipAddress !== "unknown") {
    const sameIpSnap = await db.collection("referrals")
      .where("ipAddress", "==", ipAddress)
      .limit(10)
      .get();

    const sameIpCount = sameIpSnap.docs.filter((d) => {
      const ca = d.data().createdAt;
      const ms = ca?.toMillis ? ca.toMillis() : ca ? new Date(ca).getTime() : 0;
      return ms > oneDayAgoMs;
    }).length;

    if (sameIpCount > 25) {
      fraudScore += 25;
      signals.push("SHARED_NETWORK_HIGH_VOLUME");
    } else if (sameIpCount > 5) {
      fraudScore += 10;
      signals.push("SHARED_NETWORK_MULTI");
    }
  }

  const [referrerUserDoc, referrerStatsDoc] = await Promise.all([
    db.collection("users").doc(referrerUserId).get(),
    db.collection("referral_stats").doc(referrerUserId).get()
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

export function isWorkerProfileComplete(data: any): boolean {
  if (!data) return false;
  if (data.profileCompleted === true || data.isProfileComplete === true) return true;
  const fullName = getStringValue(data.fullName || data.name).trim();
  const phone = getStringValue(data.phone || data.phoneNumber).trim();
  const skills = data.skills;
  const hasSkills = Array.isArray(skills)
    ? skills.filter((s: any) => typeof s === "string" && s.trim().length > 0).length > 0
    : Boolean(typeof skills === "string" && skills.trim().length > 0);
  return Boolean(fullName && phone && hasSkills);
}

export function isEmployerProfileComplete(data: any): boolean {
  if (!data) return false;
  if (data.profileCompleted === true || data.isProfileComplete === true) return true;
  const name = getStringValue(data.companyName || data.fullName || data.name).trim();
  const phone = getStringValue(data.phone || data.phoneNumber).trim();
  return Boolean(name && phone);
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
    if (before.referralCode || after.referralCode) {
      return null;
    }
    const userRole = after.activeRole || after.role || "WORKER";
    const userName = after.fullName || after.name || after.companyName || "DutyPe User";
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
            const codeRef = db.collection("referral_codes").doc(referralCode);
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
            transaction.set(db.collection("referral_stats").doc(userId), {
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
      db.collection("referral_codes").doc(code).get(),
      db.collection("referral_codes").doc(code.toLowerCase()).get()
    ]);

    if (!exactMatch.exists && !legacyCaseMatch.exists) {
      return code;
    }
  }

  throw new Error("Failed to generate unique referral code after max retries");
}



// ============================================
// ENSURE USER REFERRAL CODE (Callable)
// ============================================
// Guaranteed idempotent lookup or generation of a referral code for a user.
// Called by ReferralService when displaying Refer & Earn screen.

export const ensureUserReferralCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const userRole = getStringValue(data?.userRole, "WORKER").toUpperCase();
  const userName = getStringValue(data?.userName, "");

  try {
    const userRef = db.collection("users").doc(userId);
    const statsRef = db.collection("referral_stats").doc(userId);

    const [userDoc, statsDoc] = await Promise.all([
      userRef.get(),
      statsRef.get()
    ]);

    const userData = userDoc.data() || {};
    const statsData = statsDoc.data() || {};

    const existingCode = normalizeReferralCodeInput(
      userData.referralCode || statsData.referralCode || ""
    );

    // If existingCode found, verify/heal referral_codes doc
    if (existingCode) {
      const codeRef = db.collection("referral_codes").doc(existingCode);
      const codeDoc = await codeRef.get();
      const resolvedName = userName || userData.fullName || userData.name || userData.companyName || "DutyPe User";
      const resolvedRole = userRole || userData.activeRole || userData.role || "WORKER";

      if (!codeDoc.exists) {
        await codeRef.set({
          code: existingCode,
          userId,
          userRole: resolvedRole,
          userName: resolvedName,
          isActive: true,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          totalUsed: statsData.totalReferrals || 0,
          successfulReferrals: statsData.successfulReferrals || 0
        }, { merge: true });
      }

      if (!statsData.referralCode) {
        await statsRef.set({
          userId,
          userRole: resolvedRole,
          referralCode: existingCode,
          ...DEFAULT_REFERRAL_STATS,
          ...statsData,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
      }

      return { success: true, referralCode: existingCode };
    }

    // Check if code was already assigned in referral_codes
    const existingCodeQuery = await db.collection("referral_codes")
      .where("userId", "==", userId)
      .limit(1)
      .get();

    if (!existingCodeQuery.empty) {
      const foundCode = normalizeReferralCodeInput(existingCodeQuery.docs[0].id);
      const resolvedRole = userRole || userData.activeRole || userData.role || "WORKER";

      await Promise.all([
        userRef.set({ referralCode: foundCode }, { merge: true }),
        statsRef.set({
          userId,
          userRole: resolvedRole,
          referralCode: foundCode,
          ...DEFAULT_REFERRAL_STATS,
          ...statsData,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true })
      ]);

      return { success: true, referralCode: foundCode };
    }

    // No existing code: generate a new canonical code
    for (let attempt = 0; attempt < 10; attempt++) {
      const newCode = await generateUniqueReferralCode();
      try {
        const resolvedCode = await db.runTransaction(async (transaction) => {
          const freshUserDoc = await transaction.get(userRef);
          if (freshUserDoc.exists && freshUserDoc.data()?.referralCode) {
            return freshUserDoc.data()!.referralCode as string;
          }

          const codeRef = db.collection("referral_codes").doc(newCode);
          const codeDoc = await transaction.get(codeRef);
          if (codeDoc.exists) {
            throw new Error("REFERRAL_CODE_COLLISION");
          }

          const freshUserData = freshUserDoc.data() || {};
          const resolvedName = userName || freshUserData.fullName || freshUserData.name || freshUserData.companyName || "DutyPe User";
          const resolvedRole = userRole || freshUserData.activeRole || freshUserData.role || "WORKER";
          const existingStats = freshUserData.referralStats || {};

          transaction.set(userRef, {
            referralCode: newCode,
            referralCodeCreatedAt: admin.firestore.FieldValue.serverTimestamp(),
            "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp(),
            ...buildMissingReferralStatsUpdates(existingStats)
          }, { merge: true });

          transaction.set(statsRef, {
            userId,
            userRole: resolvedRole,
            referralCode: newCode,
            ...DEFAULT_REFERRAL_STATS,
            ...existingStats,
            lastUpdated: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });

          transaction.set(codeRef, {
            code: newCode,
            userId,
            userRole: resolvedRole,
            userName: resolvedName,
            isActive: true,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            totalUsed: 0,
            successfulReferrals: 0
          });

          return newCode;
        });

        if (resolvedCode) {
          functions.logger.info(`REFERRAL: ensureUserReferralCode resolved ${resolvedCode} for ${userId}`);
          return { success: true, referralCode: resolvedCode };
        }
      } catch (err: any) {
        if (err?.message !== "REFERRAL_CODE_COLLISION") {
          throw err;
        }
      }
    }

    throw new Error("Failed to reserve canonical referral code");
  } catch (error: any) {
    functions.logger.error(`REFERRAL: Error ensuring referral code for ${userId}:`, error);
    throw new functions.https.HttpsError("internal", error.message || "Failed to ensure referral code");
  }
});

// ============================================
// CLAIM WELCOME BONUS (Callable)
// ============================================
// Safe check/no-op for welcome bonus eligibility to prevent 404 from ReferralService.

export const claimWelcomeBonus = functions.https.onCall(async (_data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }
  return { success: true, credited: false, reason: "checked" };
});

// ============================================
// EVENT 2: REFERRAL APPLICATION (PENDING)
// ============================================
// Triggered when new user applies a referral code during signup
// Creates PENDING referral record

export interface ExecuteReferralParams {
  newUserId: string;
  referralCode: string;
  newUserRole?: string;
  newUserName?: string;
  newUserPhone?: string;
  deviceFingerprint?: string | null;
  ipAddress?: string;
}

export async function executeReferralApplication(params: ExecuteReferralParams): Promise<{
  success: boolean;
  referralId?: string;
  referrerName?: string;
  referrerRole?: string;
  referrerReward?: number;
  referredUserReward?: number;
  milestoneBonus?: number;
  message?: string;
  error?: string;
}> {
  const { newUserId } = params;
  const requestedCode = normalizeReferralCodeInput(params.referralCode || "");
  const newUserRole = getStringValue(params.newUserRole, "WORKER").toUpperCase();
  const newUserName = getStringValue(params.newUserName, "");
  const newUserPhone = getStringValue(params.newUserPhone, "");
  const deviceFingerprint = getStringValue(params.deviceFingerprint, "") || null;
  const ipAddress = params.ipAddress || "unknown";

  if (!requestedCode || !/^[A-Z0-9]{7,10}$/.test(requestedCode)) {
    return { success: false, error: "Invalid referral code format" };
  }

  try {
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
    const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
    const newUserStatsRef = db.collection("referral_stats").doc(newUserId);

    const result = await db.runTransaction(async (transaction) => {
      const existingReferralQuery = db.collection("referrals")
        .where("referredUserId", "==", newUserId)
        .limit(1);

      const workerProfileRef = db.collection("worker_profiles").doc(newUserId);
      const employerProfileRef = db.collection("employer_profiles").doc(newUserId);

      const [
        latestCodeDoc,
        referrerUserDoc,
        newUserDoc,
        referrerLegacyStatsDoc,
        newUserLegacyStatsDoc,
        existingReferralSnapshot,
        newWorkerProfileDoc,
        newEmployerProfileDoc
      ] = await Promise.all([
        transaction.get(codeRef),
        transaction.get(referrerUserRef),
        transaction.get(newUserRef),
        transaction.get(referrerStatsRef),
        transaction.get(newUserStatsRef),
        transaction.get(existingReferralQuery),
        transaction.get(workerProfileRef),
        transaction.get(employerProfileRef)
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

      const referrerUserData = referrerUserDoc.exists
        ? (referrerUserDoc.data() || {})
        : {
            userId: latestReferrerUserId,
            name: getStringValue(latestCodeData.userName, "DutyPe User"),
            role: getStringValue(latestCodeData.userRole, "WORKER"),
            activeRole: getStringValue(latestCodeData.userRole, "WORKER")
          };
      const newUserData = newUserDoc.exists
        ? (newUserDoc.data() || {})
        : {
            userId: newUserId,
            name: newUserName,
            fullName: newUserName,
            phone: newUserPhone,
            role: newUserRole,
            activeRole: newUserRole
          };

      const referralCode = getStringValue(latestCodeData.code, latestCodeDoc.id);
      const referrerStats = getCombinedReferralStats(referrerUserData, referrerLegacyStatsDoc.data() || {});
      const newUserStats = getCombinedReferralStats(newUserData, newUserLegacyStatsDoc.data() || {});
      const existingReferredByCode = getStringValue(
        newUserStats.referredByCode || newUserData.referredByCode || newUserLegacyStatsDoc.data()?.referredByCode
      );

      const isAlreadyCredited = !existingReferralSnapshot.empty;

      if (isAlreadyCredited) {
        const existingReferral = existingReferralSnapshot.docs[0].data() || {};
        const existingCode = normalizeReferralCodeInput(getStringValue(existingReferral.referralCode || existingReferredByCode));
        if (existingCode === requestedCode) {
          return { success: true, message: "Referral already applied" };
        }
        return { success: false, error: "You have already used a referral code" };
      }

      const currentSuccessful = getNumberValue(referrerStats.successfulReferrals);
      const newSuccessfulCount = currentSuccessful + 1;
      const currentEarnings = getNumberValue(referrerStats.totalEarnings);
      const signupBonus = getNumberValue(referrerStats.signupBonusAmount);
      const existingAwarded = Array.isArray(referrerStats.awardedMilestones) ? referrerStats.awardedMilestones : [];
      const audit = auditUserMilestones(newSuccessfulCount, currentEarnings, signupBonus, existingAwarded);
      const referrerReward = REFERRAL_CONFIG.REWARD_PER_REFERRAL;
      const referredUserReward = REFERRAL_CONFIG.SIGNUP_BONUS;
      const milestoneBonus = audit.toCredit;
      const totalReferrerReward = referrerReward + milestoneBonus;
      const currentBalance = getNumberValue(referrerStats.availableBalance);
      const newTier = calculateTier(newSuccessfulCount);
      const newCanWithdraw = canWithdraw(newSuccessfulCount, currentBalance + totalReferrerReward);
      const newNextMilestone = getNextMilestone(newSuccessfulCount);
      const referrerRole = getStringValue(
        referrerUserData.activeRole || referrerUserData.role || latestCodeData.userRole,
        getStringValue(latestCodeData.userRole, "WORKER")
      );
      const referrerName = getStringValue(
        referrerUserData.fullName || referrerUserData.name || referrerUserData.companyName || latestCodeData.userName,
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
      const isProfileCompleted = Boolean(
        newUserData.profileCompleted ||
        newUserData.isProfileComplete ||
        (newWorkerProfileDoc.exists && isWorkerProfileComplete(newWorkerProfileDoc.data())) ||
        (newEmployerProfileDoc.exists && isEmployerProfileComplete(newEmployerProfileDoc.data()))
      );
      const referralStatus = isProfileCompleted ? "COMPLETED" : "PENDING";
      const referralRef = db.collection("referrals").doc(referralId);

      transaction.set(referralRef, {
        id: referralId,
        idempotencyKey,
        referrerUserId: latestReferrerUserId,
        referrerId: latestReferrerUserId,
        referrerRole,
        referredUserId: newUserId,
        referredId: newUserId,
        referredRole: newUserRole,
        referralCode,
        status: referralStatus,
        profileCompleted: isProfileCompleted,
        rewardAmount: referrerReward,
        bonusAmount: milestoneBonus,
        referredUserReward,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        completedAt: isProfileCompleted ? admin.firestore.FieldValue.serverTimestamp() : null,
        referredUserName: newUserName,
        referredUserPhone: maskedPhone,
        deviceFingerprint,
        ipAddress,
        fraudScore: fraudResult.fraudScore,
        fraudSignals: fraudResult.signals,
        needsReview: fraudResult.needsReview,
        fraudCheckedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      if (isProfileCompleted) {
        // Immediate crediting when profile is already complete
        const referrerUserUpdate: { [key: string]: any } = {
          ...buildMissingReferralStatsUpdates(referrerUserData.referralStats || {}),
          "referralStats.totalReferrals": admin.firestore.FieldValue.increment(1),
          "referralStats.successfulReferrals": newSuccessfulCount,
          "referralStats.totalEarnings": admin.firestore.FieldValue.increment(totalReferrerReward),
          "referralStats.availableBalance": admin.firestore.FieldValue.increment(totalReferrerReward),
          "referralStats.canWithdraw": newCanWithdraw,
          "referralStats.currentTier": newTier,
          "referralStats.nextMilestone": newNextMilestone,
          "referralStats.awardedMilestones": audit.newAwardedMilestones,
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        };

        if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
          referrerUserUpdate["referralStats.freeJobPostings"] = freePostings;
          referrerUserUpdate["referralStats.freeJobPostingsExpiry"] = freePostingsExpiry;
        }

        if (referrerUserDoc.exists) {
          transaction.update(referrerUserRef, referrerUserUpdate);
        } else {
          transaction.set(referrerUserRef, {
            userId: latestReferrerUserId,
            name: referrerName,
            role: referrerRole,
            activeRole: referrerRole,
            referralStats: {
              ...DEFAULT_REFERRAL_STATS,
              totalReferrals: 1,
              successfulReferrals: newSuccessfulCount,
              totalEarnings: totalReferrerReward,
              availableBalance: totalReferrerReward,
              canWithdraw: newCanWithdraw,
              currentTier: newTier,
              nextMilestone: newNextMilestone,
              awardedMilestones: audit.newAwardedMilestones,
              lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }
          }, { merge: true });
        }

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
          awardedMilestones: audit.newAwardedMilestones,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
          ...(referrerRole === "EMPLOYER" && freePostingsExpiry ? {
            freeJobPostings: freePostings,
            freeJobPostingsExpiry: freePostingsExpiry
          } : {})
        }, { merge: true });

        const newUserUpdate: { [key: string]: any } = {
          ...buildMissingReferralStatsUpdates(newUserData.referralStats || {}),
          "referralStats.totalEarnings": admin.firestore.FieldValue.increment(referredUserReward),
          "referralStats.availableBalance": admin.firestore.FieldValue.increment(referredUserReward),
          "referralStats.signupBonusReceived": true,
          "referralStats.signupBonusAmount": referredUserReward,
          "referralStats.referredByCode": referralCode,
          "referralStats.referredByUserId": latestReferrerUserId,
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        };

        if (newUserDoc.exists) {
          transaction.update(newUserRef, newUserUpdate);
        } else {
          transaction.set(newUserRef, {
            userId: newUserId,
            name: newUserName,
            fullName: newUserName,
            phone: newUserPhone,
            role: newUserRole,
            activeRole: newUserRole,
            referralStats: {
              ...DEFAULT_REFERRAL_STATS,
              totalEarnings: referredUserReward,
              availableBalance: referredUserReward,
              signupBonusReceived: true,
              signupBonusAmount: referredUserReward,
              referredByCode: referralCode,
              referredByUserId: latestReferrerUserId,
              lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }
          }, { merge: true });
        }

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

        const referrerEventRef = db.collection("referral_events").doc();
        transaction.set(referrerEventRef, {
          eventType: "REWARD_CREDITED",
          userId: latestReferrerUserId,
          referralId,
          amount: totalReferrerReward,
          bonusAmount: milestoneBonus,
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        });

        const referredEventRef = db.collection("referral_events").doc();
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
      } else {
        // Pending state: user signed up with code, reward unlocks upon profile completion
        const referrerUserPendingUpdate: { [key: string]: any } = {
          ...buildMissingReferralStatsUpdates(referrerUserData.referralStats || {}),
          "referralStats.totalReferrals": admin.firestore.FieldValue.increment(1),
          "referralStats.pendingReferrals": admin.firestore.FieldValue.increment(1),
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        };

        if (referrerUserDoc.exists) {
          transaction.update(referrerUserRef, referrerUserPendingUpdate);
        } else {
          transaction.set(referrerUserRef, {
            userId: latestReferrerUserId,
            name: referrerName,
            role: referrerRole,
            activeRole: referrerRole,
            referralStats: {
              ...DEFAULT_REFERRAL_STATS,
              totalReferrals: 1,
              pendingReferrals: 1,
              lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }
          }, { merge: true });
        }

        transaction.set(referrerStatsRef, {
          userId: latestReferrerUserId,
          userRole: referrerRole,
          totalReferrals: admin.firestore.FieldValue.increment(1),
          pendingReferrals: admin.firestore.FieldValue.increment(1),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        const newUserPendingUpdate: { [key: string]: any } = {
          ...buildMissingReferralStatsUpdates(newUserData.referralStats || {}),
          "referralStats.referredByCode": referralCode,
          "referralStats.referredByUserId": latestReferrerUserId,
          "referralStats.signupBonusReceived": false,
          "referralStats.signupBonusAmount": referredUserReward,
          "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
        };

        if (newUserDoc.exists) {
          transaction.update(newUserRef, newUserPendingUpdate);
        } else {
          transaction.set(newUserRef, {
            userId: newUserId,
            name: newUserName,
            fullName: newUserName,
            phone: newUserPhone,
            role: newUserRole,
            activeRole: newUserRole,
            referralStats: {
              ...DEFAULT_REFERRAL_STATS,
              referredByCode: referralCode,
              referredByUserId: latestReferrerUserId,
              signupBonusReceived: false,
              signupBonusAmount: referredUserReward,
              lastUpdated: admin.firestore.FieldValue.serverTimestamp()
            }
          }, { merge: true });
        }

        transaction.set(newUserStatsRef, {
          userId: newUserId,
          userRole: newUserRole,
          referredByCode: referralCode,
          referredByUserId: latestReferrerUserId,
          signupBonusReceived: false,
          signupBonusAmount: referredUserReward,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        transaction.update(codeRef, {
          totalUsed: admin.firestore.FieldValue.increment(1),
          userRole: referrerRole,
          userName: referrerName
        });

        const pendingEventRef = db.collection("referral_events").doc();
        transaction.set(pendingEventRef, {
          eventType: "REFERRAL_PENDING",
          userId: latestReferrerUserId,
          referredUserId: newUserId,
          referralId,
          amount: totalReferrerReward,
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        });

        const referrerPendingNotifRef = db.collection("notifications").doc();
        transaction.set(referrerPendingNotifRef, {
          recipientId: latestReferrerUserId,
          title: "Friend Joined with Your Code",
          message: (newUserName || "A new friend") + " joined using your code. You will earn Rs." + totalReferrerReward + " once they complete their profile.",
          type: "REFERRAL_PENDING",
          data: { referralId },
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false
        });

        const referredPendingNotifRef = db.collection("notifications").doc();
        transaction.set(referredPendingNotifRef, {
          recipientId: newUserId,
          title: "Referral Code Applied",
          message: "Complete your profile now to claim your Rs." + referredUserReward + " welcome bonus.",
          type: "SIGNUP_BONUS_PENDING",
          data: { referralId },
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          isRead: false
        });
      }

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
    return { success: false, error: "Failed to apply referral code" };
  }
}

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

  const ipAddress = context.rawRequest.ip ||
    context.rawRequest.headers["x-forwarded-for"]?.toString().split(",")[0] ||
    "unknown";

  const result = await executeReferralApplication({
    newUserId,
    referralCode: data.referralCode || "",
    newUserRole: data.userRole || "WORKER",
    newUserName: data.userName || "",
    newUserPhone: data.userPhone || "",
    deviceFingerprint: data.deviceFingerprint || null,
    ipAddress
  });

  return result;
});


// ============================================
// EVENT 3: REFERRAL COMPLETION (LEGACY - KEPT FOR BACKWARD COMPATIBILITY)
// ============================================
// This function is now only for users who applied codes before the system update
// New users get rewards immediately when applying code

export async function processPendingReferralOnProfileComplete(
  referredUserId: string,
  afterData?: any
): Promise<{
  status: string;
  referrerReward?: number;
  referredUserReward?: number;
  milestoneBonus?: number;
  newTier?: string;
} | null> {
  functions.logger.info(`🎁 REFERRAL: Checking pending referral for user ${referredUserId}`);

  try {
    // Find pending referral for this user
    let pendingReferrals = await db.collection("referrals")
      .where("referredUserId", "==", referredUserId)
      .where("status", "==", "PENDING")
      .limit(1)
      .get();

    let referralDoc = !pendingReferrals.empty ? pendingReferrals.docs[0] : null;
    let referral: any = referralDoc ? referralDoc.data() : null;
    let referralId = referralDoc ? referralDoc.id : "";
    let referrerUserId = referral ? (referral.referrerUserId || referral.referrerId) : "";

    // If no pending referral doc exists, check if user has already completed referral or has referrer info on profile
    if (!referral) {
      const completedQuery = await db.collection("referrals")
        .where("referredUserId", "==", referredUserId)
        .where("status", "==", "COMPLETED")
        .limit(1)
        .get();

      if (!completedQuery.empty) {
        functions.logger.info(`🎁 REFERRAL: User ${referredUserId} already has completed referral.`);
        return { status: "COMPLETED" };
      }

      // Check users, worker_profiles, and employer_profiles for referrer info
      const [uDoc, wDoc, eDoc] = await Promise.all([
        db.collection("users").doc(referredUserId).get(),
        db.collection("worker_profiles").doc(referredUserId).get(),
        db.collection("employer_profiles").doc(referredUserId).get()
      ]);
      const uData = uDoc.data() || {};
      const wData = wDoc.data() || {};
      const eData = eDoc.data() || {};
      const merged = { ...uData, ...wData, ...eData, ...(afterData || {}) };

      const refCode = normalizeReferralCodeInput(
        merged.referredByCode || merged.referralStats?.referredByCode || ""
      );
      let resolvedReferrerId = getStringValue(
        merged.referredByUserId || merged.referralStats?.referredByUserId || ""
      );

      if (!resolvedReferrerId && refCode) {
        const lookup = await getReferralCodeLookup(refCode);
        if (lookup) {
          resolvedReferrerId = getStringValue(lookup.data.userId);
        }
      }

      if (resolvedReferrerId && resolvedReferrerId !== referredUserId) {
        referrerUserId = resolvedReferrerId;
        referralId = db.collection("referrals").doc().id;
        referral = {
          id: referralId,
          referrerUserId,
          referrerId: referrerUserId,
          referredUserId,
          referredId: referredUserId,
          referralCode: refCode,
          status: "PENDING",
          referredUserName: getStringValue(merged.fullName || merged.name || merged.companyName, "DutyPe User"),
          referredUserPhone: getStringValue(merged.phone || merged.phoneNumber, ""),
          referredRole: getStringValue(merged.role || merged.activeRole, "WORKER"),
          createdAt: merged.createdAt || admin.firestore.FieldValue.serverTimestamp()
        };
        functions.logger.info(`🎁 REFERRAL: Auto-healed missing referral record ${referralId} for referrer ${referrerUserId} and user ${referredUserId}`);
      } else {
        functions.logger.info(`🎁 REFERRAL: No pending referral or referrer info for user ${referredUserId}`);
        return null;
      }
    }

    // Fetch user doc if not passed
    const after = afterData || (await db.collection("users").doc(referredUserId).get()).data() || {};

    // Check if expired
    const expiresAt = referral.expiresAt?.toMillis() || 0;
    if (expiresAt > 0 && Date.now() > expiresAt) {
      functions.logger.info(`🎁 REFERRAL: Referral ${referralId} expired`);
      
      // Mark as expired
      await db.collection("referrals").doc(referralId).update({
        status: "EXPIRED",
        completedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      const referrerUserRef = db.collection("users").doc(referrerUserId);
      await db.runTransaction(async (transaction) => {
        transaction.set(db.collection("referral_stats").doc(referrerUserId), {
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
      db.collection("referral_stats").doc(referrerUserId).get()
    ]);
    const referrerUserData = referrerUserDoc.data() || {};
    const referrerStats = getCombinedReferralStats(referrerUserData, referrerStatsDoc.data() || {});
    const currentSuccessful = getNumberValue(referrerStats.successfulReferrals);
    const newSuccessfulCount = currentSuccessful + 1;
    const currentEarnings = getNumberValue(referrerStats.totalEarnings);
    const signupBonus = getNumberValue(referrerStats.signupBonusAmount);
    const existingAwarded = Array.isArray(referrerStats.awardedMilestones) ? referrerStats.awardedMilestones : [];
    const audit = auditUserMilestones(newSuccessfulCount, currentEarnings, signupBonus, existingAwarded);

    // Calculate rewards
    const referrerReward = REFERRAL_CONFIG.REWARD_PER_REFERRAL;
    const referredUserReward = REFERRAL_CONFIG.SIGNUP_BONUS;
    const milestoneBonus = audit.toCredit;
    const totalReferrerReward = referrerReward + milestoneBonus;

    // Calculate new tier and withdrawal eligibility
    const newTier = calculateTier(newSuccessfulCount);
    const currentBalance = getNumberValue(referrerStats.availableBalance);
    const newCanWithdraw = canWithdraw(newSuccessfulCount, currentBalance + totalReferrerReward);
    const newNextMilestone = getNextMilestone(newSuccessfulCount);

    // Calculate employer free postings
    const referrerRole = getStringValue(
      referrerUserData.activeRole || referrerUserData.role || referrerStats.userRole,
      "WORKER"
    );
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
    batch.set(referralRef, {
      id: referralId,
      status: "COMPLETED",
      profileCompleted: true,
      referrerId: referrerUserId,
      referrerUserId: referrerUserId,
      referredId: referredUserId,
      referredUserId: referredUserId,
      rewardAmount: referrerReward,
      bonusAmount: milestoneBonus,
      referredUserReward: referredUserReward,
      completedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // 2. Update referrer's stats in BOTH locations
    const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
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
      awardedMilestones: audit.newAwardedMilestones,
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
      "referralStats.awardedMilestones": audit.newAwardedMilestones,
      "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
    };

    if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
      userStatsUpdate["referralStats.freeJobPostings"] = freePostings;
      userStatsUpdate["referralStats.freeJobPostingsExpiry"] = freePostingsExpiry;
    }

    batch.set(referrerUserRef, userStatsUpdate, { merge: true });

    if (audit.awardedNow.length > 0) {
      const milestoneEventRef = db.collection("referral_events").doc();
      batch.set(milestoneEventRef, {
        eventType: "MILESTONE_REACHED",
        userId: referrerUserId,
        milestones: audit.awardedNow,
        bonusAmount: milestoneBonus,
        newSuccessfulCount,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });

      const milestoneNotifRef = db.collection("notifications").doc();
      batch.set(milestoneNotifRef, {
        recipientId: referrerUserId,
        title: "🎉 Milestone Bonus Unlocked!",
        message: `Congratulations! You unlocked a ₹${milestoneBonus} milestone bonus for reaching ${newSuccessfulCount} referrals!`,
        type: "MILESTONE_REWARD",
        data: { milestones: audit.awardedNow, amount: milestoneBonus },
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false
      });
    }

    // 3. Update referral code stats
    if (referral.referralCode) {
      const codeRef = db.collection("referral_codes").doc(referral.referralCode);
      batch.set(codeRef, {
        successfulReferrals: admin.firestore.FieldValue.increment(1)
      }, { merge: true });
    }

    // 4. Credit referred user's signup bonus in BOTH locations
    const referredStatsRef = db.collection("referral_stats").doc(referredUserId);
    batch.set(referredStatsRef, {
      userId: referredUserId,
      userRole: getStringValue(after.activeRole || after.role, "WORKER"),
      totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
      availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
      signupBonusReceived: true,
      signupBonusAmount: referredUserReward,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    // Also update users.referralStats
    const referredUserRef = db.collection("users").doc(referredUserId);
    batch.set(referredUserRef, {
      ...buildMissingReferralStatsUpdates(after.referralStats || {}),
      "referralStats.totalEarnings": admin.firestore.FieldValue.increment(referredUserReward),
      "referralStats.availableBalance": admin.firestore.FieldValue.increment(referredUserReward),
      "referralStats.signupBonusReceived": true,
      "referralStats.signupBonusAmount": referredUserReward,
      "referralStats.lastUpdated": admin.firestore.FieldValue.serverTimestamp()
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
    const existingNotifications = await db.collection("notifications")
      .where("recipientId", "==", referrerUserId)
      .where("type", "==", "REFERRAL_REWARD")
      .limit(5)
      .get();
    
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
}

export const onReferredUserProfileComplete = functions.firestore
  .document("users/{userId}")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : null;
    if (!after) return null;
    const before = change.before.exists ? change.before.data() : null;
    const referredUserId = context.params.userId;

    const wasCompleted = Boolean(before?.profileCompleted || before?.isProfileComplete);
    const isCompleted = Boolean(after?.profileCompleted || after?.isProfileComplete) ||
      isWorkerProfileComplete(after) || isEmployerProfileComplete(after);

    if (!wasCompleted && isCompleted) {
      return await processPendingReferralOnProfileComplete(referredUserId, after);
    }
    return null;
  });

export const onWorkerProfileCompleteReferral = functions.firestore
  .document("worker_profiles/{userId}")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : null;
    if (!after) return null;
    const userId = context.params.userId;

    if (isWorkerProfileComplete(after)) {
      await db.collection("users").doc(userId).set({
        profileCompleted: true,
        isProfileComplete: true,
        role: "WORKER",
        activeRole: "WORKER",
        fullName: after.fullName || after.name || "",
        phone: after.phone || "",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      return await processPendingReferralOnProfileComplete(userId, after);
    }
    return null;
  });

export const onEmployerProfileCompleteReferral = functions.firestore
  .document("employer_profiles/{userId}")
  .onWrite(async (change, context) => {
    const after = change.after.exists ? change.after.data() : null;
    if (!after) return null;
    const userId = context.params.userId;

    if (isEmployerProfileComplete(after)) {
      await db.collection("users").doc(userId).set({
        profileCompleted: true,
        isProfileComplete: true,
        role: "EMPLOYER",
        activeRole: "EMPLOYER",
        fullName: after.fullName || after.companyName || "",
        phone: after.phone || "",
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      return await processPendingReferralOnProfileComplete(userId, after);
    }
    return null;
  });

export const syncPendingReferrals = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const pendingSnapshot = await db.collection("referrals")
    .where("status", "==", "PENDING")
    .limit(200)
    .get();

  functions.logger.info(`🎁 REFERRAL: Syncing ${pendingSnapshot.size} pending referrals`);

  const results: Array<{ referralId: string; referredUserId: string; status: string; reason?: string }> = [];

  for (const doc of pendingSnapshot.docs) {
    const refData = doc.data();
    const referredUserId = refData.referredUserId || refData.referredId;
    if (!referredUserId) continue;

    const [userDoc, workerDoc, employerDoc] = await Promise.all([
      db.collection("users").doc(referredUserId).get(),
      db.collection("worker_profiles").doc(referredUserId).get(),
      db.collection("employer_profiles").doc(referredUserId).get()
    ]);

    const workerData = workerDoc.exists ? workerDoc.data() : null;
    const employerData = employerDoc.exists ? employerDoc.data() : null;
    const userData = userDoc.exists ? userDoc.data() : null;

    const isWorkerComplete = workerData && isWorkerProfileComplete(workerData);
    const isEmployerComplete = employerData && isEmployerProfileComplete(employerData);
    const isUserDocComplete = Boolean(userData?.profileCompleted || userData?.isProfileComplete);

    if (isWorkerComplete || isEmployerComplete || isUserDocComplete) {
      await db.collection("users").doc(referredUserId).set({
        profileCompleted: true,
        isProfileComplete: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      const outcome = await processPendingReferralOnProfileComplete(
        referredUserId,
        userData || workerData || employerData
      );

      results.push({
        referralId: doc.id,
        referredUserId,
        status: outcome?.status || "COMPLETED"
      });
    } else {
      results.push({
        referralId: doc.id,
        referredUserId,
        status: "PENDING",
        reason: "Profile incomplete"
      });
    }
  }

  // Auto-heal users with referrer info who have completed profiles but missing referral doc
  try {
    const usersWithReferrer = await db.collection("users")
      .where("referredByUserId", ">", "")
      .limit(100)
      .get();

    for (const uDoc of usersWithReferrer.docs) {
      const uId = uDoc.id;
      const uData = uDoc.data();

      // Check if user already has completed referral
      const completedCheck = await db.collection("referrals")
        .where("referredUserId", "==", uId)
        .where("status", "==", "COMPLETED")
        .limit(1)
        .get();

      if (completedCheck.empty) {
        const [wDoc, eDoc] = await Promise.all([
          db.collection("worker_profiles").doc(uId).get(),
          db.collection("employer_profiles").doc(uId).get()
        ]);
        const wData = wDoc.exists ? wDoc.data() : null;
        const eData = eDoc.exists ? eDoc.data() : null;

        const isWorkerComplete = wData && isWorkerProfileComplete(wData);
        const isEmployerComplete = eData && isEmployerProfileComplete(eData);
        const isUserDocComplete = Boolean(uData.profileCompleted || uData.isProfileComplete);

        if (isWorkerComplete || isEmployerComplete || isUserDocComplete) {
          const outcome = await processPendingReferralOnProfileComplete(
            uId,
            { ...uData, ...wData, ...eData }
          );
          if (outcome) {
            results.push({
              referralId: "healed_" + uId,
              referredUserId: uId,
              status: outcome.status
            });
          }
        }
      }
    }
  } catch (healErr) {
    functions.logger.warn("🎁 REFERRAL: Auto-heal scan error", healErr);
  }

  // Milestone Audit for all active referrers with 5+ referrals
  let milestonesFixed = 0;
  try {
    const statsSnapshot = await db.collection("referral_stats")
      .where("successfulReferrals", ">=", 5)
      .limit(200)
      .get();

    for (const statDoc of statsSnapshot.docs) {
      const sData = statDoc.data();
      const sUserId = statDoc.id;
      const sSuccessful = getNumberValue(sData.successfulReferrals);
      const sEarnings = getNumberValue(sData.totalEarnings);
      const sSignup = getNumberValue(sData.signupBonusAmount);
      const sAwarded = Array.isArray(sData.awardedMilestones) ? sData.awardedMilestones : [];
      const audit = auditUserMilestones(sSuccessful, sEarnings, sSignup, sAwarded);

      if (audit.toCredit > 0 || audit.newAwardedMilestones.length !== sAwarded.length) {
        milestonesFixed++;
        const bonusToCredit = audit.toCredit;
        const now = admin.firestore.FieldValue.serverTimestamp();
        const sRef = db.collection("referral_stats").doc(sUserId);
        const uRef = db.collection("users").doc(sUserId);
        const b = db.batch();

        const sPatch: any = {
          awardedMilestones: audit.newAwardedMilestones,
          lastUpdated: now
        };
        const uPatch: any = {
          "referralStats.awardedMilestones": audit.newAwardedMilestones,
          "referralStats.lastUpdated": now
        };

        if (bonusToCredit > 0) {
          sPatch.totalEarnings = admin.firestore.FieldValue.increment(bonusToCredit);
          sPatch.availableBalance = admin.firestore.FieldValue.increment(bonusToCredit);
          sPatch.canWithdraw = true;
          uPatch["referralStats.totalEarnings"] = admin.firestore.FieldValue.increment(bonusToCredit);
          uPatch["referralStats.availableBalance"] = admin.firestore.FieldValue.increment(bonusToCredit);
          uPatch["referralStats.canWithdraw"] = true;

          const notifRef = db.collection("notifications").doc();
          b.set(notifRef, {
            recipientId: sUserId,
            title: "🎉 Milestone Bonus Credited!",
            message: `Your ₹${bonusToCredit} milestone bonus for reaching ${sSuccessful} referrals has been credited to your balance!`,
            type: "MILESTONE_REWARD",
            data: { milestones: audit.awardedNow, amount: bonusToCredit },
            createdAt: now,
            isRead: false
          });

          const evRef = db.collection("referral_events").doc();
          b.set(evRef, {
            eventType: "MILESTONE_REACHED",
            userId: sUserId,
            milestones: audit.awardedNow,
            bonusAmount: bonusToCredit,
            newSuccessfulCount: sSuccessful,
            timestamp: now
          });
        }

        b.set(sRef, sPatch, { merge: true });
        b.set(uRef, uPatch, { merge: true });
        await b.commit();
      }
    }
  } catch (milestoneAuditErr) {
    functions.logger.error("Error during milestone audit in syncPendingReferrals:", milestoneAuditErr);
  }

  return { success: true, count: results.length, results, milestonesFixed };
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
  if (!data || typeof data !== "object" || typeof data.amount !== "number") {
    throw new functions.https.HttpsError("invalid-argument", "A numeric amount and request details are required");
  }
  if (data.userId !== undefined && data.userId !== userId) {
    throw new functions.https.HttpsError("permission-denied", "Account changed before withdrawal");
  }
  const amount = validateNumber(data.amount, "amount", {
    required: true, min: REFERRAL_CONFIG.MIN_WITHDRAWAL, max: REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY
  });
  const amountPaise = Math.round(amount * 100);
  if (!Number.isSafeInteger(amountPaise) || Math.abs(amount * 100 - amountPaise) > 0.000001) {
    throw new functions.https.HttpsError("invalid-argument", "Amount must have at most two decimal places");
  }
  const rawRequestId = (typeof data.requestId === "string" && data.requestId.trim())
    ? data.requestId.trim()
    : `req_${Date.now()}_${Math.random().toString(36).substring(2, 10)}`;
  const requestId = validateString(rawRequestId, "requestId", {
    required: true, minLength: 8, maxLength: 80, pattern: /^[a-zA-Z0-9_-]+$/
  });
  const paymentMethod = validateEnum(data.paymentMethod || "UPI", "paymentMethod", ["UPI", "BANK_TRANSFER"]);
  const payment = {
    paymentMethod,
    upiId: paymentMethod === "UPI" ? validateString(data.upiId, "upiId", {
      required: true, minLength: 5, maxLength: 100, pattern: /^[a-zA-Z0-9._-]+@[a-zA-Z][a-zA-Z0-9.-]+$/
    }) : null,
    bankAccountNumber: paymentMethod === "BANK_TRANSFER" ? validateString(data.bankDetails?.accountNumber, "accountNumber", {
      required: true, minLength: 8, maxLength: 20, pattern: /^[0-9]+$/
    }) : null,
    ifscCode: paymentMethod === "BANK_TRANSFER" ? validateString(data.bankDetails?.ifscCode, "ifscCode", {
      required: true, pattern: /^[A-Z]{4}0[A-Z0-9]{6}$/
    }) : null,
    accountHolderName: paymentMethod === "BANK_TRANSFER" ? validateString(data.bankDetails?.accountHolderName, "accountHolderName", {
      required: true, minLength: 2, maxLength: 100
    }) : null
  };
  const withdrawalId = createHash("sha256").update(`${userId}\0${requestId}`).digest("hex");
  const today = new Date();
  today.setUTCHours(0, 0, 0, 0);
  const day = today.toISOString().slice(0, 10);

  try {
    const userRef = db.collection("users").doc(userId);
    const statsRef = db.collection("referral_stats").doc(userId);
    const withdrawalRef = db.collection("withdrawal_requests").doc(withdrawalId);
    const userWithdrawalRef = statsRef.collection("withdrawals").doc(withdrawalId);
    const dailyRef = db.collection("withdrawal_daily").doc(`${userId}_${day}`);
    return await db.runTransaction(async transaction => {
      const [withdrawalDoc, userDoc, statsDoc, dailyDoc] = await transaction.getAll(withdrawalRef, userRef, statsRef, dailyRef);
      if (withdrawalDoc.exists) {
        const previous = withdrawalDoc.data()!;
        if (previous.userId !== userId || previous.amount !== amount ||
            Object.entries(payment).some(([key, value]) => previous[key] !== value)) {
          throw new functions.https.HttpsError("already-exists", "Request ID was already used for different withdrawal details");
        }
        return { success: true, withdrawalId };
      }
      if (!userDoc.exists && !statsDoc.exists) {
        return { success: false, error: "An authoritative referral balance is not available. Contact support." };
      }
      const userData = userDoc.exists ? (userDoc.data() || {}) : {};
      const rawStats = statsDoc.exists ? (statsDoc.data() || {}) : {};
      const stats = getCombinedReferralStats(userData, rawStats);
      if (getBooleanValue(stats.isBlocked) || getBooleanValue(userData.referralStats?.isBlocked)) {
        return { success: false, error: "Your account is blocked from withdrawals" };
      }
      const availablePaise = Math.round(getNumberValue(stats.availableBalance) * 100);
      const successfulCount = getNumberValue(stats.successfulReferrals);
      if (availablePaise < REFERRAL_CONFIG.MIN_WITHDRAWAL * 100 && successfulCount < 5) {
        return { success: false, error: `Minimum balance of ₹${REFERRAL_CONFIG.MIN_WITHDRAWAL} required to withdraw` };
      }
      if (!Number.isSafeInteger(availablePaise) || amountPaise > availablePaise) {
        return { success: false, error: "Insufficient referral balance" };
      }
      if (amount < REFERRAL_CONFIG.MIN_WITHDRAWAL) {
        return { success: false, error: `Minimum withdrawal amount is ₹${REFERRAL_CONFIG.MIN_WITHDRAWAL}` };
      }
      let dailyPaise = getNumberValue(dailyDoc.get("amountPaise"));
      let dailyCount = getNumberValue(dailyDoc.get("requestCount"));
      if (!dailyDoc.exists) {
        const previousRequests = await transaction.get(db.collection("withdrawal_requests")
          .where("userId", "==", userId).where("createdAt", ">=", today));
        dailyPaise = previousRequests.docs.reduce((total, document) => total + Math.round(getNumberValue(document.get("amount")) * 100), 0);
        dailyCount = previousRequests.size;
      }
      if (dailyPaise + amountPaise > REFERRAL_CONFIG.MAX_WITHDRAWAL_PER_DAY * 100 || dailyCount >= 5) {
        return { success: false, error: "Daily withdrawal limit reached" };
      }
      const userRole = getStringValue(userData.activeRole || userData.role || stats.userRole, "WORKER");
      const userName = getStringValue(
        userData.fullName || userData.name || userData.displayName || stats.userName,
        "DutyPe User"
      );
      const phone = getStringValue(
        userData.phone || userData.phoneNumber || stats.phone,
        ""
      );
      const referralCode = getStringValue(
        stats.referralCode || userData.referralStats?.referralCode || userData.referralCode,
        ""
      );
      const availableBalance = (availablePaise - amountPaise) / 100;
      const withdrawnAmount = (Math.round(getNumberValue(stats.withdrawnAmount) * 100) + amountPaise) / 100;
      const totalWithdrawals = getNumberValue(stats.totalWithdrawals) + 1;
      const timestamp = FieldValue.serverTimestamp();
      const withdrawalPayload = {
        id: withdrawalId,
        requestId,
        userId,
        userName,
        phone,
        referralCode,
        userRole,
        amount,
        status: "PENDING",
        balanceDeductedAtRequest: true,
        ...payment,
        createdAt: timestamp,
        updatedAt: timestamp
      };
      transaction.create(withdrawalRef, withdrawalPayload);
      transaction.set(userWithdrawalRef, withdrawalPayload, { merge: true });
      transaction.set(dailyRef, { userId, day, amountPaise: dailyPaise + amountPaise, requestCount: dailyCount + 1 });
      transaction.set(statsRef, {
        userId,
        userRole,
        availableBalance,
        withdrawnAmount,
        totalWithdrawals,
        canWithdraw: availableBalance >= 100 || successfulCount >= 5,
        lastWithdrawalStatus: "PENDING",
        lastWithdrawalAt: timestamp,
        lastUpdated: timestamp
      }, { merge: true });
      transaction.set(userRef, {
        "referralStats.availableBalance": availableBalance,
        "referralStats.withdrawnAmount": withdrawnAmount,
        "referralStats.totalWithdrawals": totalWithdrawals,
        "referralStats.canWithdraw": availableBalance >= 100 || successfulCount >= 5,
        "referralStats.lastWithdrawalStatus": "PENDING",
        "referralStats.lastWithdrawalAt": timestamp,
        "referralStats.lastUpdated": timestamp
      }, { merge: true });
      transaction.create(db.collection("referral_events").doc(`withdrawal_${withdrawalId}`), {
        eventType: "WITHDRAWAL_REQUESTED", userId, withdrawalId, amount, timestamp
      });
      transaction.create(db.collection("notifications").doc(), {
        recipientId: userId,
        title: "Withdrawal Requested",
        message: `Your withdrawal request of ₹${amount} has been received. Our team will review and process the payout soon.`,
        type: "WITHDRAWAL_PENDING",
        data: { withdrawalId, amount },
        isRead: false,
        createdAt: timestamp
      });
      return { success: true, withdrawalId };
    });
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
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
    const referrerUserId = referral.referrerUserId;

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
        batch.set(db.collection("referral_stats").doc(referrerUserId), {
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
      db.collection("referral_stats").doc(userId).get()
    ]);

    if (!userDoc.exists && !legacyStatsDoc.exists) {
      return { exists: false };
    }

    const userData = userDoc.data() || {};
    const legacyStats = legacyStatsDoc.data() || {};
    const stats = {
      userId,
      userRole: getStringValue(userData.activeRole || userData.role || legacyStats.userRole, "WORKER"),
      referralCode: getStringValue(userData.referralCode),
      ...DEFAULT_REFERRAL_STATS,
      ...legacyStats,
      ...(userData.referralStats || {})
    };

    const currentSuccessful = getNumberValue(stats.successfulReferrals);
    const currentEarnings = getNumberValue(stats.totalEarnings);
    const signupBonus = getNumberValue(stats.signupBonusAmount);
    const existingAwarded = Array.isArray(stats.awardedMilestones) ? stats.awardedMilestones : [];

    if (currentSuccessful >= 5) {
      const audit = auditUserMilestones(currentSuccessful, currentEarnings, signupBonus, existingAwarded);
      if (audit.toCredit > 0 || audit.newAwardedMilestones.length !== existingAwarded.length) {
        const bonusToCredit = audit.toCredit;
        const now = admin.firestore.FieldValue.serverTimestamp();
        const sRef = db.collection("referral_stats").doc(userId);
        const uRef = db.collection("users").doc(userId);
        const b = db.batch();

        const sPatch: any = {
          awardedMilestones: audit.newAwardedMilestones,
          lastUpdated: now
        };
        const uPatch: any = {
          "referralStats.awardedMilestones": audit.newAwardedMilestones,
          "referralStats.lastUpdated": now
        };

        if (bonusToCredit > 0) {
          sPatch.totalEarnings = admin.firestore.FieldValue.increment(bonusToCredit);
          sPatch.availableBalance = admin.firestore.FieldValue.increment(bonusToCredit);
          sPatch.canWithdraw = true;
          uPatch["referralStats.totalEarnings"] = admin.firestore.FieldValue.increment(bonusToCredit);
          uPatch["referralStats.availableBalance"] = admin.firestore.FieldValue.increment(bonusToCredit);
          uPatch["referralStats.canWithdraw"] = true;

          const notifRef = db.collection("notifications").doc();
          b.set(notifRef, {
            recipientId: userId,
            title: "🎉 Milestone Bonus Credited!",
            message: `Your ₹${bonusToCredit} milestone bonus for reaching ${currentSuccessful} referrals has been credited to your balance!`,
            type: "MILESTONE_REWARD",
            data: { milestones: audit.awardedNow, amount: bonusToCredit },
            createdAt: now,
            isRead: false
          });

          const evRef = db.collection("referral_events").doc();
          b.set(evRef, {
            eventType: "MILESTONE_REACHED",
            userId,
            milestones: audit.awardedNow,
            bonusAmount: bonusToCredit,
            newSuccessfulCount: currentSuccessful,
            timestamp: now
          });

          stats.totalEarnings = currentEarnings + bonusToCredit;
          stats.availableBalance = getNumberValue(stats.availableBalance) + bonusToCredit;
        }

        stats.awardedMilestones = audit.newAwardedMilestones;
        b.set(sRef, sPatch, { merge: true });
        b.set(uRef, uPatch, { merge: true });
        await b.commit();
      }
    }

    stats.canWithdraw = canWithdraw(currentSuccessful, getNumberValue(stats.availableBalance));

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


