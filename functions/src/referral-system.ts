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
 * - referral_stats/{userId}/withdrawals: Withdrawal tracking with admin approval
 * - referral_stats: Per-user referral statistics
 * 
 * @author DutyPe Engineering Team
 * @version 2.0.0 - Enterprise Edition with Security Hardening
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { validateString, validateNumber, validateUserId, validateEnum, assertAppCheck } from "./validation";
import { getReferralConfig, ReferralConfig } from "./app-config";
import { getUserLanguage, tTitle, tBody } from "./notification-i18n";
import {
  calculateTier,
  canWithdraw,
  evaluateWithdrawal,
  generateIdempotencyKey,
  getBooleanValue,
  getConfiguredMilestoneBonus,
  getNumberValue,
  getStringValue,
  normalizeReferralCodeInput,
  scoreReferralFraud,
  timestampToMillis,
  welcomeBonusAmountForRole as welcomeBonusForRole,
} from "./referral-rules";

const db = admin.firestore();

// ============================================
// REFERRAL SYSTEM CONSTANTS
// ============================================
const REFERRAL_CONFIG = {
  // Reward amounts (in INR)
  REWARD_PER_REFERRAL: 25,           // ₹25 per successful referral
  SIGNUP_BONUS: 25,                   // ₹25 for new user who uses code
  MIN_WITHDRAWAL: 100,                // Minimum ₹100 to withdraw
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
const WELCOME_BONUS_ROLLOUT_AT_MS = Date.UTC(2026, 4, 5, 0, 0, 0);
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
  withdrawnAmount: 0,
  availableBalance: 0,
  currentTier: "BRONZE",
  signupBonusReceived: false,
  signupBonusAmount: 0,
  welcomeBonusReceived: false,
  welcomeBonusAmount: 0,
  welcomeBonusCampaignId: "",
  unlimitedJobPostingGranted: false,
  totalWithdrawals: 0
};

type RoleProfile = {
  role: string;
  data: { [key: string]: any };
  exists: boolean;
  createTime?: admin.firestore.Timestamp;
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

/**
 * Check if user can withdraw based on available balance
 */

/**
 * Get milestone bonus if applicable
 */

function welcomeBonusAmountForRole(config: ReferralConfig, role: string): number {
  return welcomeBonusForRole(config.signupBonus, role);
}

async function creditWelcomeBonusForNewProfile(
  userId: string,
  role: "WORKER" | "EMPLOYER",
  profileData: Record<string, any>
): Promise<void> {
  const config = await getReferralConfig();
  const statsRef = db.collection("referral_stats").doc(userId);
  const profileRef = db.collection(profileCollectionForRole(role)).doc(userId);
  const amount = welcomeBonusAmountForRole(config, role);
  const unlimitedPostingEnabled = false; // Forced false to remove unlimited job posting feature
  const campaignId = getStringValue(config.welcomeBonusCampaignId, "welcome_bonus_v1");

  if (amount <= 0 && !unlimitedPostingEnabled) {
    functions.logger.info("WELCOME_BONUS: no active reward for role", { userId, role, campaignId });
    return;
  }

  const notificationPayload = await db.runTransaction(async (transaction) => {
    const statsDoc = await transaction.get(statsRef);
    const stats = statsDoc.data() || {};
    const alreadyReceivedSignupBonus = getBooleanValue(stats.signupBonusReceived) || getBooleanValue(stats.welcomeBonusReceived);
    const alreadyGrantedUnlimitedPosting = getBooleanValue(stats.unlimitedJobPostingGranted) || getBooleanValue(profileData.unlimitedJobPostingGranted);
    const currentBalance = getNumberValue(stats.availableBalance);
    const shouldCreditCash = amount > 0 && !alreadyReceivedSignupBonus;
    const shouldGrantUnlimitedPosting = unlimitedPostingEnabled && !alreadyGrantedUnlimitedPosting;
    const baseStats = statsDoc.exists ? {} : DEFAULT_REFERRAL_STATS;
    const statsUpdate: Record<string, any> = {
      ...baseStats,
      userRole: role,
      lastUpdated: admin.firestore.FieldValue.serverTimestamp()
    };

    if (shouldCreditCash) {
      const newBalance = currentBalance + amount;
      statsUpdate.totalEarnings = admin.firestore.FieldValue.increment(amount);
      statsUpdate.availableBalance = admin.firestore.FieldValue.increment(amount);
      statsUpdate.canWithdraw = canWithdraw(newBalance, config.minWithdrawal);
      statsUpdate.signupBonusReceived = true;
      statsUpdate.signupBonusAmount = amount;
      statsUpdate.signupBonusSource = "WELCOME";
      statsUpdate.signupBonusCampaignId = campaignId;
      statsUpdate.signupBonusRole = role;
      statsUpdate.signupBonusCreditedAt = admin.firestore.FieldValue.serverTimestamp();
      statsUpdate.welcomeBonusReceived = true;
      statsUpdate.welcomeBonusAmount = amount;
      statsUpdate.welcomeBonusCampaignId = campaignId;
      statsUpdate.welcomeBonusCreditedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (shouldGrantUnlimitedPosting) {
      statsUpdate.unlimitedJobPostingGranted = true;
      statsUpdate.unlimitedJobPostingCampaignId = campaignId;
      statsUpdate.unlimitedJobPostingGrantedAt = admin.firestore.FieldValue.serverTimestamp();
      transaction.set(profileRef, {
        unlimitedJobPostingGranted: true,
        unlimitedJobPostingCampaignId: campaignId,
        unlimitedJobPostingGrantedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    }

    transaction.set(statsRef, statsUpdate, { merge: true });

    if (shouldCreditCash) {
      const auditRef = userReferralAuditDocRef(userId);
      transaction.set(auditRef, {
        eventType: "WELCOME_BONUS_CREDITED",
        userId,
        userRole: role,
        amount,
        campaignId,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    return {
      shouldCreditCash,
      grantsUnlimitedPosting: shouldGrantUnlimitedPosting,
      amount,
      campaignId
    };
  });

  if (!notificationPayload.shouldCreditCash && !notificationPayload.grantsUnlimitedPosting) {
    return;
  }

  const locale = await getUserLanguage(db, userId);
  const userName = getStringValue(profileData.fullName || profileData.companyName, "");
  const templateId = role === "EMPLOYER" && notificationPayload.grantsUnlimitedPosting
    ? (notificationPayload.shouldCreditCash ? "EMPLOYER_WELCOME_BONUS" : "EMPLOYER_WELCOME_BENEFIT")
    : "SIGNUP_BONUS";
  const title = tTitle(templateId, locale, { amount: notificationPayload.amount });
  const message = tBody(templateId, locale, { amount: notificationPayload.amount });

  await db.collection("notifications").add({
    recipientId: userId,
    title,
    message,
    type: "SIGNUP_BONUS",
    data: {
      amount: notificationPayload.shouldCreditCash ? notificationPayload.amount : 0,
      role,
      campaignId: notificationPayload.campaignId,
      source: "WELCOME",
      userName,
      unlimitedJobPostingGranted: notificationPayload.grantsUnlimitedPosting
    },
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    isRead: false
  });

  functions.logger.info("WELCOME_BONUS: credited", {
    userId,
    role,
    amount: notificationPayload.shouldCreditCash ? notificationPayload.amount : 0,
    unlimitedJobPostingGranted: notificationPayload.grantsUnlimitedPosting,
    campaignId: notificationPayload.campaignId
  });
}

export const creditWorkerWelcomeBonusOnCreate = functions
  .region("asia-south1")
  .firestore.document("worker_profiles/{userId}")
  .onCreate(async (snapshot, context) => {
    await creditWelcomeBonusForNewProfile(context.params.userId, "WORKER", snapshot.data() || {});
  });

export const creditEmployerWelcomeBonusOnCreate = functions
  .region("asia-south1")
  .firestore.document("employer_profiles/{userId}")
  .onCreate(async (snapshot, context) => {
    await creditWelcomeBonusForNewProfile(context.params.userId, "EMPLOYER", snapshot.data() || {});
  });

function profileCollectionForRole(role: string): string {
  return getStringValue(role, "WORKER").toUpperCase() === "EMPLOYER"
    ? "employer_profiles"
    : "worker_profiles";
}

async function getRoleProfile(userId: string, preferredRole = "WORKER"): Promise<RoleProfile> {
  const normalizedRole = getStringValue(preferredRole, "WORKER").toUpperCase();
  const roles = normalizedRole === "EMPLOYER" ? ["EMPLOYER", "WORKER"] : ["WORKER", "EMPLOYER"];

  for (const role of roles) {
    const doc = await db.collection(profileCollectionForRole(role)).doc(userId).get();
    if (doc.exists) {
      return { role, data: doc.data() || {}, exists: true, createTime: doc.createTime };
    }
  }

  return { role: normalizedRole, data: {}, exists: false };
}

function roleProfileCreatedAtMillis(profile: RoleProfile): number {
  return timestampToMillis(profile.data.createdAt) || timestampToMillis(profile.createTime);
}

function isRecentWelcomeBonusProfile(profile: RoleProfile): boolean {
  const createdAtMillis = roleProfileCreatedAtMillis(profile);
  return profile.exists && createdAtMillis >= WELCOME_BONUS_ROLLOUT_AT_MS;
}

function buildReferralStatsResponse(userId: string, profile: RoleProfile, statsData: { [key: string]: any }) {
  return {
    userId,
    userRole: getStringValue(statsData.userRole || profile.data.role, profile.role).toUpperCase(),
    referralCode: getStringValue(statsData.referralCode || profile.data.referralCode),
    ...DEFAULT_REFERRAL_STATS,
    ...statsData
  };
}

export const claimWelcomeBonus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  try {
    validateUserId(userId, true);
    if (data?.userRole) {
      validateEnum(data.userRole, "userRole", ["WORKER", "EMPLOYER"]);
    }
  } catch (error: any) {
    throw new functions.https.HttpsError("invalid-argument", error.message);
  }

  try {
    const preferredRole = getStringValue(data?.userRole, "WORKER").toUpperCase();
    const profile = await getRoleProfile(userId, preferredRole);
    const statsRef = db.collection("referral_stats").doc(userId);
    const beforeStatsDoc = await statsRef.get();
    const beforeStats = beforeStatsDoc.data() || {};

    if (!profile.exists) {
      return { success: false, claimed: false, reason: "PROFILE_NOT_FOUND" };
    }

    if (!isRecentWelcomeBonusProfile(profile)) {
      return {
        success: true,
        claimed: false,
        reason: "NOT_ELIGIBLE",
        stats: buildReferralStatsResponse(userId, profile, beforeStats)
      };
    }

    const role = profile.role === "EMPLOYER" ? "EMPLOYER" : "WORKER";
    const config = await getReferralConfig();
    const cashRewardActive = welcomeBonusAmountForRole(config, role) > 0;
    const cashAlreadySettled = getBooleanValue(beforeStats.signupBonusReceived) ||
      getBooleanValue(beforeStats.welcomeBonusReceived);
    const unlimitedPostingActive = false; // Forced false to remove unlimited job posting feature
    const unlimitedPostingAlreadySettled = getBooleanValue(beforeStats.unlimitedJobPostingGranted) ||
      getBooleanValue(profile.data.unlimitedJobPostingGranted);
    const hasMissingEligibleReward = (cashRewardActive && !cashAlreadySettled) ||
      (unlimitedPostingActive && !unlimitedPostingAlreadySettled);

    if (!hasMissingEligibleReward) {
      return {
        success: true,
        claimed: false,
        reason: "ALREADY_CLAIMED",
        stats: buildReferralStatsResponse(userId, profile, beforeStats)
      };
    }

    await creditWelcomeBonusForNewProfile(userId, role, profile.data);

    const afterStatsDoc = await statsRef.get();
    const afterStats = afterStatsDoc.data() || {};

    return {
      success: true,
      claimed: getBooleanValue(afterStats.signupBonusReceived) ||
        getBooleanValue(afterStats.welcomeBonusReceived) ||
        getBooleanValue(afterStats.unlimitedJobPostingGranted),
      stats: buildReferralStatsResponse(userId, profile, afterStats)
    };
  } catch (error) {
    functions.logger.error("WELCOME_BONUS: claim failed", { userId, error });
    throw new functions.https.HttpsError("internal", "Failed to claim welcome bonus");
  }
});

function isReferralCodeReady(user: any = {}): boolean {
  const hasName = !!getStringValue(user.fullName || user.companyName);
  const hasPhone = !!getStringValue(user.phone);
  return hasName && hasPhone;
}

function referralAuditDocRef(referralId: string) {
  return db.collection("referrals")
    .doc(referralId)
    .collection(REFERRAL_AUDIT_SUBCOLLECTION)
    .doc();
}

function userReferralAuditDocRef(userId: string) {
  return db.collection("referral_stats")
    .doc(userId)
    .collection(REFERRAL_AUDIT_SUBCOLLECTION)
    .doc();
}

function userWithdrawalsCollection(userId: string) {
  return db.collection("referral_stats")
    .doc(userId)
    .collection(USER_WITHDRAWALS_SUBCOLLECTION);
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

  return null;
}

async function ensureCanonicalReferralCodeForUser(
  userId: string,
  userRole: string,
  userName: string,
  existingCode = ""
): Promise<string> {
  const resolvedUserRole = getStringValue(userRole, "WORKER").toUpperCase();
  const resolvedUserName = getStringValue(userName, "DutyPe User");
  const normalizedExistingCode = normalizeReferralCodeInput(existingCode);
  const statsRef = db.collection("referral_stats").doc(userId);
  const profileRef = db.collection(profileCollectionForRole(resolvedUserRole)).doc(userId);
  const userRef = db.collection("users").doc(userId);

  const currentStatsDoc = await statsRef.get();
  const currentStatsCode = currentStatsDoc.exists
    ? normalizeReferralCodeInput(getStringValue(currentStatsDoc.get("referralCode")))
    : "";
  if (currentStatsCode) {
    return currentStatsCode;
  }

  const codeCandidates: string[] = [];
  if (normalizedExistingCode) {
    codeCandidates.push(normalizedExistingCode);
  }

  for (let attempt = 0; attempt < 10; attempt++) {
    const candidate = codeCandidates.length > 0
      ? codeCandidates.shift()!
      : await generateUniqueReferralCode(resolvedUserName);

    try {
      const resolvedCode = await db.runTransaction(async (transaction) => {
        const latestStatsDoc = await transaction.get(statsRef);
        const latestStatsCode = latestStatsDoc.exists
          ? normalizeReferralCodeInput(getStringValue(latestStatsDoc.get("referralCode")))
          : "";
        if (latestStatsCode) {
          return latestStatsCode;
        }

        const latestProfileDoc = await transaction.get(profileRef);
        const latestProfileCode = latestProfileDoc.exists
          ? normalizeReferralCodeInput(getStringValue(latestProfileDoc.get("referralCode")))
          : "";

        const codeToUse = latestProfileCode || candidate;
        if (!codeToUse) {
          throw new Error("INVALID_REFERRAL_CODE");
        }

        const codeRef = db.collection("referral_codes").doc(codeToUse);
        const codeDoc = await transaction.get(codeRef);
        if (codeDoc.exists) {
          const ownerUserId = getStringValue(codeDoc.get("userId"));
          if (ownerUserId && ownerUserId !== userId) {
            throw new Error("REFERRAL_CODE_COLLISION");
          }
        }

        transaction.set(statsRef, {
          userRole: resolvedUserRole,
          userName: resolvedUserName,
          referralCode: codeToUse,
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        transaction.set(codeRef, {
          userId,
          userRole: resolvedUserRole,
          userName: resolvedUserName,
          isActive: true,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          totalUsed: 0,
          successfulReferrals: 0
        }, { merge: true });

        transaction.set(profileRef, {
          referralCode: codeToUse
        }, { merge: true });

        transaction.set(userRef, {
          userId,
          uid: userId,
          role: resolvedUserRole,
          activeRole: resolvedUserRole,
          fullName: resolvedUserName,
          name: resolvedUserName,
          referralCode: codeToUse,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        return codeToUse;
      });

      if (resolvedCode) {
        return resolvedCode;
      }
    } catch (error: any) {
      if (error?.message !== "REFERRAL_CODE_COLLISION") {
        throw error;
      }
    }
  }

  throw new Error("Failed to reserve canonical referral code");
}

async function evaluateReferralFraud(params: {
  referrerUserId: string;
  deviceFingerprint?: string | null;
  ipAddress?: string | null;
}): Promise<ReferralFraudResult> {
  const { referrerUserId, deviceFingerprint, ipAddress } = params;
  const now = Date.now();
  const oneDayAgo = new Date(now - ONE_DAY_MS);
  const oneHourAgo = new Date(now - ONE_HOUR_MS);

  const toMillis = timestampToMillis;

  // Avoid composite-index dependency on (referrerId, createdAt) by filtering in memory.
  const recentReferralsRaw = await db.collection("referrals")
    .where("referrerId", "==", referrerUserId)
    .limit(250)
    .get();

  const referralsInLastHour = recentReferralsRaw.docs.filter((doc) => {
    const createdAtMillis = toMillis(doc.data().createdAt);
    return createdAtMillis > oneHourAgo.getTime();
  }).length;

  let sameDeviceReferralsCount = 0;
  if (deviceFingerprint) {
    // Avoid composite-index dependency on (deviceFingerprint, createdAt).
    const sameDeviceReferralsRaw = await db.collection("referrals")
      .where("deviceFingerprint", "==", deviceFingerprint)
      .limit(250)
      .get();

    sameDeviceReferralsCount = sameDeviceReferralsRaw.docs.filter((doc) => {
      const createdAtMillis = toMillis(doc.data().createdAt);
      return createdAtMillis > oneDayAgo.getTime();
    }).length;
  }

  const hasIpAddress = Boolean(ipAddress && ipAddress !== "unknown");
  let sameIpReferralsCount = 0;
  if (hasIpAddress) {
    // Avoid composite-index dependency on (ipAddress, createdAt).
    const sameIpReferralsRaw = await db.collection("referrals")
      .where("ipAddress", "==", ipAddress)
      .limit(250)
      .get();

    sameIpReferralsCount = sameIpReferralsRaw.docs.filter((doc) => {
      const createdAtMillis = toMillis(doc.data().createdAt);
      return createdAtMillis > oneDayAgo.getTime();
    }).length;
  }

  const referrerStatsDoc = await db.collection("referral_stats").doc(referrerUserId).get();
  const referrerStats = referrerStatsDoc.data() || {};

  return scoreReferralFraud({
    referralsInLastHour,
    sameDeviceReferralsToday: sameDeviceReferralsCount,
    sameIpReferralsToday: sameIpReferralsCount,
    hasIpAddress,
    totalReferrals: getNumberValue(referrerStats.totalReferrals),
    rejectedReferrals: getNumberValue(referrerStats.rejectedReferrals),
  });
}


// ============================================
// EVENT 1: REFERRAL CODE CREATION
// ============================================

async function ensureReferralForReadyProfile(
  userId: string,
  before: any,
  after: any,
  fallbackRole: string
) {
  const afterReady = isReferralCodeReady(after);
  const beforeReady = isReferralCodeReady(before);
  if (!afterReady || beforeReady) {
    return null;
  }

  const userRole = getStringValue(after.role || fallbackRole, fallbackRole).toUpperCase();
  const userName = getStringValue(after.fullName || after.companyName, "");
  if (!userName) {
    functions.logger.warn("REFERRAL: Profile " + userId + " has no name, cannot generate referral code");
    return null;
  }

  try {
    const resolvedCode = await ensureCanonicalReferralCodeForUser(
      userId,
      userRole,
      userName,
      getStringValue(after.referralCode)
    );
    functions.logger.info("REFERRAL: Ensured code " + resolvedCode + " for profile " + userId);
    return { success: true, referralCode: resolvedCode };
  } catch (error) {
    functions.logger.error("REFERRAL: Error creating referral code for profile " + userId + ":", error);
    return null;
  }
}

export const onWorkerProfileReferralReady = functions.firestore
  .document("worker_profiles/{userId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;
    return ensureReferralForReadyProfile(
      context.params.userId,
      change.before.exists ? change.before.data() : {},
      change.after.data() || {},
      "WORKER"
    );
  });

export const onEmployerProfileReferralReady = functions.firestore
  .document("employer_profiles/{userId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;
    return ensureReferralForReadyProfile(
      context.params.userId,
      change.before.exists ? change.before.data() : {},
      change.after.data() || {},
      "EMPLOYER"
    );
  });

export const ensureUserReferralCode = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  try {
    validateUserId(userId, true);
  } catch (error: any) {
    throw new functions.https.HttpsError("invalid-argument", error.message);
  }

  try {
    const roleFromPayload = getStringValue(data?.userRole, "WORKER").toUpperCase();
    const profileDoc = await db.collection(profileCollectionForRole(roleFromPayload)).doc(userId).get();
    if (!profileDoc.exists) {
      throw new functions.https.HttpsError("failed-precondition", "User profile not found");
    }

    const userData = profileDoc.data() || {};
    const nameFromPayload = getStringValue(data?.userName, "");
    const userRole = getStringValue(userData.role, roleFromPayload).toUpperCase();
    const userName = getStringValue(userData.fullName || userData.companyName, nameFromPayload || "DutyPe User");
    const existingCode = getStringValue(userData.referralCode);

    const referralCode = await ensureCanonicalReferralCodeForUser(
      userId,
      userRole,
      userName,
      existingCode
    );

    return {
      success: true,
      referralCode
    };
  } catch (error) {
    functions.logger.error("REFERRAL: ensureUserReferralCode failed for " + userId + ":", error);
    throw new functions.https.HttpsError("internal", "Failed to ensure referral code");
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
async function generateUniqueReferralCode(userName: string): Promise<string> {
  const maxRetries = 10;
  const baseName = (userName || "DUTY")
    .split(/\s+/)
    .filter(Boolean)[0]
    .replace(/[^A-Za-z0-9]/g, "")
    .toUpperCase()
    .slice(0, 6)
    .padEnd(3, "D");

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    const randomNum = Math.floor(1000 + Math.random() * 9000).toString();
    const code = `${baseName}${randomNum}`.slice(0, 10);
    const [exactMatch, lowercaseMatch] = await Promise.all([
      db.collection("referral_codes").doc(code).get(),
      db.collection("referral_codes").doc(code.toLowerCase()).get()
    ]);

    if (!exactMatch.exists && !lowercaseMatch.exists) {
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
    const referrerRoleFromCode = getStringValue(codeData.userRole, "WORKER").toUpperCase();
    const referrerProfileRef = db.collection(profileCollectionForRole(referrerRoleFromCode)).doc(referrerUserId);
    const newUserProfileRef = db.collection(profileCollectionForRole(newUserRole)).doc(newUserId);
    const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
    const newUserStatsRef = db.collection("referral_stats").doc(newUserId);

    const cfg = await getReferralConfig();

    const result = await db.runTransaction(async (transaction) => {
      const existingReferralQuery = db.collection("referrals")
        .where("referredUserId", "==", newUserId)
        .limit(1);

      const [latestCodeDoc, referrerProfileDoc, newUserProfileDoc, referrerStatsDoc, newUserStatsDoc, existingReferralSnapshot] = await Promise.all([
        transaction.get(codeRef),
        transaction.get(referrerProfileRef),
        transaction.get(newUserProfileRef),
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

      if (!referrerProfileDoc.exists || !newUserProfileDoc.exists) {
        return { success: false, error: "User account not ready yet" };
      }

      const referralCode = getStringValue(latestCodeData.code, latestCodeDoc.id);
      const referrerUserData = referrerProfileDoc.data() || {};
      const newUserData = newUserProfileDoc.data() || {};
      const referrerStats = referrerStatsDoc.data() || {};
      const newUserStats = newUserStatsDoc.data() || {};
      const newUserRoles = [getStringValue(newUserData.role || newUserRole, newUserRole).toUpperCase()].filter(Boolean);
      const existingReferredByCode = getStringValue(newUserStats.referredByCode || newUserData.referredByCode);

      // BUG #11 FIX: The previous guard `newUserRoles.length > 1 || isProfileComplete(newUserData)`
      // rejected legitimate fallback apply attempts that fire after the user
      // finishes profile setup (which happens whenever the in-registration
      // attempt failed for any reason - timing, fraud false-positive, etc.).
      // Dedup is already enforced below by `existingReferredByCode` and the
      // existing-referrals query, so we do not need a profile-completion gate
      // here. We still block multi-role accounts defensively.
      if (newUserRoles.length > 1) {
        return { success: false, error: "Referral code can only be used on your first registration" };
      }

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
      const alreadyReceivedSignupBonus = getBooleanValue(newUserStats.signupBonusReceived) || getBooleanValue(newUserStats.welcomeBonusReceived);
      const signupBonusForNewUser = welcomeBonusAmountForRole(cfg, newUserRole);
      const shouldCreditReferredSignupBonus = !alreadyReceivedSignupBonus && signupBonusForNewUser > 0;
      const referrerReward = cfg.rewardPerReferral;
      const referredUserReward = shouldCreditReferredSignupBonus ? signupBonusForNewUser : 0;
      const milestoneBonus = getConfiguredMilestoneBonus(newSuccessfulCount, cfg.milestones);
      const totalReferrerReward = referrerReward + milestoneBonus;
      const newTier = calculateTier(newSuccessfulCount);
      const referrerRole = getStringValue(
        referrerUserData.role,
        "WORKER"
      ).toUpperCase();
      const referrerName = getStringValue(
        referrerUserData.fullName || referrerUserData.companyName || latestCodeData.userName,
        "DutyPe User"
      );
      const referrerOwnReferralCode = getStringValue(
        referrerStats.referralCode || referrerUserData.referralCode || referralCode,
        referralCode
      );
      const newUserOwnReferralCode = getStringValue(
        newUserStats.referralCode || newUserData.referralCode
      );

      const referralId = db.collection("referrals").doc().id;
      const maskedPhone = newUserPhone ? "****" + newUserPhone.slice(-4) : "";
      const idempotencyKey = generateIdempotencyKey(latestReferrerUserId, newUserId);
      const referralRef = db.collection("referrals").doc(referralId);
      const referredDisplayName = getStringValue(
        newUserName || newUserData.fullName || newUserData.companyName,
        maskedPhone || "User"
      );

      transaction.set(referralRef, {
        idempotencyKey,
        referrerId: latestReferrerUserId,
        referredUserId: newUserId,
        referralCode,
        status: "COMPLETED",
        rewardAmount: referrerReward,
        bonusAmount: milestoneBonus,
        referredUserReward,
        referredUserName: referredDisplayName,
        referredUserRole: newUserRole,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        completedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      transaction.set(referrerStatsRef, {
        userRole: referrerRole,
        referralCode: referrerOwnReferralCode,
        totalReferrals: admin.firestore.FieldValue.increment(1),
        successfulReferrals: newSuccessfulCount,
        totalEarnings: admin.firestore.FieldValue.increment(totalReferrerReward),
        availableBalance: admin.firestore.FieldValue.increment(totalReferrerReward),
        currentTier: newTier,
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      transaction.set(newUserStatsRef, {
        userRole: newUserRole,
        ...(newUserOwnReferralCode ? { referralCode: newUserOwnReferralCode } : {}),
        referredByCode: referralCode,
        referredByUserId: latestReferrerUserId,
        totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
        availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
        ...(shouldCreditReferredSignupBonus ? {
          signupBonusReceived: true,
          signupBonusAmount: referredUserReward,
          signupBonusSource: "REFERRAL",
          signupBonusCampaignId: getStringValue(cfg.welcomeBonusCampaignId, "welcome_bonus_v1"),
          signupBonusRole: newUserRole,
          signupBonusCreditedAt: admin.firestore.FieldValue.serverTimestamp()
        } : {}),
        lastUpdated: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });

      transaction.set(referrerProfileRef, {
        referralCode: referrerOwnReferralCode
      }, { merge: true });

      transaction.set(newUserProfileRef, {
        ...(newUserOwnReferralCode ? { referralCode: newUserOwnReferralCode } : {}),
        referredByCode: referralCode,
        referredByUserId: latestReferrerUserId
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
        userRole: getStringValue(referrerUserData.role, "WORKER").toUpperCase(),
        amount: totalReferrerReward,
        bonusAmount: milestoneBonus,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });

      if (shouldCreditReferredSignupBonus) {
        const referredEventRef = referralAuditDocRef(referralId);
        transaction.set(referredEventRef, {
          eventType: "SIGNUP_BONUS_CREDITED",
          userId: newUserId,
          referralId,
          amount: referredUserReward,
          timestamp: admin.firestore.FieldValue.serverTimestamp()
        });
      }

      const referrerLocale = await getUserLanguage(db, latestReferrerUserId);
      const referredLocale = await getUserLanguage(db, newUserId);
      const referrerTemplateId = milestoneBonus > 0 ? "REFERRAL_REWARD_WITH_BONUS" : "REFERRAL_REWARD_BASIC";
      const referrerName2 = newUserName || "Someone";

      const referrerNotifRef = db.collection("notifications").doc();
      transaction.set(referrerNotifRef, {
        recipientId: latestReferrerUserId,
        title: tTitle(referrerTemplateId, referrerLocale, { name: referrerName2, amount: totalReferrerReward, bonus: milestoneBonus }),
        message: tBody(referrerTemplateId, referrerLocale, { name: referrerName2, amount: totalReferrerReward, bonus: milestoneBonus }),
        type: "REFERRAL_REWARD",
        data: { referralId, amount: totalReferrerReward },
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        isRead: false
      });

      if (shouldCreditReferredSignupBonus) {
        const referredNotifRef = db.collection("notifications").doc();
        transaction.set(referredNotifRef, {
          recipientId: newUserId,
          title: tTitle("SIGNUP_BONUS", referredLocale, { amount: referredUserReward }),
          message: tBody("SIGNUP_BONUS", referredLocale, { amount: referredUserReward }),
          type: "SIGNUP_BONUS",
          data: { amount: referredUserReward, source: "REFERRAL" },
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
    throw new functions.https.HttpsError("internal", "Failed to apply referral code");
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

      // Update referrer stats (canonical: referral_stats/{uid})
      for (const [referrerId, expiredCount] of Object.entries(referrerUpdates)) {
        const statsRef = db.collection("referral_stats").doc(referrerId);
        batch.set(statsRef, {
          pendingReferrals: admin.firestore.FieldValue.increment(-expiredCount),
          expiredReferrals: admin.firestore.FieldValue.increment(expiredCount),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
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
  assertAppCheck(context);

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

  const cfg = await getReferralConfig();
  const minWithdrawal = Math.max(cfg.minWithdrawal, 100);
  const amount = parseFloat(data.amount);
  const paymentMethod = data.paymentMethod || "UPI";
  const upiId = data.upiId;
  const bankDetails = data.bankDetails;

  try {
    const statsRef = db.collection("referral_stats").doc(userId);
    const withdrawalRef = userWithdrawalsCollection(userId).doc();
    const auditRef = userReferralAuditDocRef(userId);
    const withdrawalId = withdrawalRef.id;

    const result = await db.runTransaction(async (tx) => {
      const statsDoc = await tx.get(statsRef);

      if (!statsDoc.exists) {
        return { success: false as const, error: "No referral stats found" };
      }

      const stats = statsDoc.data() || {};
      const availableBalance = getNumberValue(stats.availableBalance);
      const canUserWithdraw = getBooleanValue(stats.canWithdraw, canWithdraw(availableBalance, minWithdrawal));

      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const todayWithdrawals = await tx.get(
        userWithdrawalsCollection(userId).where("createdAt", ">=", today)
      );
      const todayTotal = todayWithdrawals.docs.reduce(
        (sum, doc) => sum + getNumberValue(doc.data().amount), 0
      );

      const decision = evaluateWithdrawal({
        amount,
        availableBalance,
        minWithdrawal,
        maxWithdrawalPerDay: cfg.maxWithdrawalPerDay,
        alreadyWithdrawnToday: todayTotal,
        canWithdrawFlag: canUserWithdraw,
        paymentMethod,
        upiId,
        bankDetails,
      });

      if (!decision.ok) {
        return { success: false as const, error: decision.error };
      }

      const userRole = getStringValue(stats.userRole, "WORKER");

      tx.set(withdrawalRef, {
        userRole,
        amount,
        status: "PENDING",
        balanceBeforeRequest: availableBalance,
        balanceAfterRequest: 0,
        balanceDeductedAtRequest: true,
        refundApplied: false,
        paymentMethod,
        upiId: upiId || null,
        bankAccountNumber: bankDetails?.accountNumber || null,
        ifscCode: bankDetails?.ifscCode || null,
        accountHolderName: bankDetails?.accountHolderName || null,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      tx.set(statsRef, {
        userRole,
        availableBalance: admin.firestore.FieldValue.increment(-amount),
        withdrawnAmount: admin.firestore.FieldValue.increment(amount),
        canWithdraw: false,
        lastWithdrawalAt: admin.firestore.FieldValue.serverTimestamp(),
        totalWithdrawals: admin.firestore.FieldValue.increment(1),
        lastUpdated: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: true });

      tx.set(auditRef, {
        eventType: "WITHDRAWAL_REQUESTED",
        userId,
        withdrawalId,
        amount,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { success: true as const, withdrawalId };
    });

    if (!result.success) return result;
    return { success: true, withdrawalId: result.withdrawalId };
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
        batch.set(db.collection("referral_stats").doc(referrerUserId), {
          pendingReferrals: admin.firestore.FieldValue.increment(-1),
          rejectedReferrals: admin.firestore.FieldValue.increment(1),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
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


/**
 * Get referral stats for a user
 */
export const getReferralStats = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  // SECURITY: reject cross-user reads. A user may only query their own stats.
  if (data?.userId && data.userId !== context.auth.uid) {
    throw new functions.https.HttpsError("permission-denied", "cannot read another user's stats");
  }
  const userId = context.auth.uid;

  try {
    const referralStatsDoc = await db.collection("referral_stats").doc(userId).get();

    if (!referralStatsDoc.exists) {
      return { exists: false };
    }

    const referralStats = referralStatsDoc.data() || {};
    const combinedStats = referralStats;
    const profile = await getRoleProfile(userId, getStringValue(combinedStats.userRole, "WORKER"));
    const profileData = profile.data;
    const stats = {
      userId,
      userRole: getStringValue(combinedStats.userRole || profileData.role, profile.role).toUpperCase(),
      referralCode: getStringValue(combinedStats.referralCode || profileData.referralCode),
      ...DEFAULT_REFERRAL_STATS,
      ...referralStats,
      ...combinedStats
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
  const role = getStringValue(data.role).toUpperCase(); // Optional filter by role
  const limit = Math.max(1, Math.min(getNumberValue(data.limit, 10), 50));

  try {
    const snapshot = await db.collection("referral_stats")
      .orderBy("successfulReferrals", "desc")
      .limit(Math.max(limit * 5, 25))
      .get();

    const profileEntries = await Promise.all(
      snapshot.docs.map(async (doc) => {
        const statsData = doc.data() || {};
        const profile = await getRoleProfile(doc.id, getStringValue(statsData.userRole, "WORKER"));
        return [doc.id, profile] as const;
      })
    );
    const profileById = new Map(profileEntries);

    const leaderboard = snapshot.docs
      .map(doc => {
        const statsData = doc.data() || {};
        const combinedStats = statsData;
        const profile = profileById.get(doc.id);
        const profileData = profile?.data || {};
        return {
          userId: doc.id,
          userRole: getStringValue(combinedStats.userRole || profileData.role, profile?.role || "WORKER").toUpperCase(),
          userName: getStringValue(
            profileData.fullName ||
            profileData.companyName ||
            combinedStats.userName,
            "DutyPe User"
          ),
          profileImageUrl: getStringValue(profileData.profileImageUrl),
          referralCode: getStringValue(profileData.referralCode || combinedStats.referralCode),
          successfulReferrals: getNumberValue(combinedStats.successfulReferrals),
          totalEarnings: getNumberValue(combinedStats.totalEarnings),
          availableBalance: getNumberValue(combinedStats.availableBalance),
          currentTier: getStringValue(combinedStats.currentTier, "BRONZE")
        };
      })
      .filter(entry => !role || entry.userRole === role)
      .slice(0, limit);

    return {
      leaderboard: leaderboard.map((entry, index) => ({
        rank: index + 1,
        ...entry
      }))
    };

  } catch (error) {
    functions.logger.error("Error getting leaderboard:", error);
    throw new functions.https.HttpsError("internal", "Failed to get leaderboard");
  }
});




