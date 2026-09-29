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
import { getReferralConfig, isCallerAdmin, ReferralConfig } from "./app-config";

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
 * Get next milestone for user (from the admin-editable config).
 */
function getNextMilestone(successfulReferrals: number, config: ReferralConfig): number {
  for (const milestone of milestoneList(config)) {
    if (successfulReferrals < milestone.count) return milestone.count;
  }
  return successfulReferrals + 10;
}

/**
 * Withdrawal eligibility. Single rule shared with the app and requestWithdrawal:
 * the available balance must reach the configured minimum.
 */
function canWithdraw(availableBalance: number, minWithdrawal: number): boolean {
  return availableBalance >= minWithdrawal;
}

type MilestoneEntry = { count: number; bonus: number };

/** Milestones from /app_config/referral, sorted by referral count. */
function milestoneList(config: ReferralConfig): MilestoneEntry[] {
  return Object.entries(config.milestones || {})
    .map(([count, bonus]) => ({ count: Number(count), bonus: Number(bonus) }))
    .filter((m) => Number.isFinite(m.count) && m.count > 0 && Number.isFinite(m.bonus) && m.bonus >= 0)
    .sort((a, b) => a.count - b.count);
}

interface MilestoneAuditResult {
  toCredit: number;
  newAwardedMilestones: number[];
  awardedNow: number[];
}

/**
 * Works out which milestone bonuses are still owed, never crediting one twice.
 *
 * - When the stats doc has an `awardedMilestones` array, it is the source of truth:
 *   every reached milestone that is not in it is owed.
 * - Legacy docs without that field fall back to inferring already-paid bonuses from
 *   `totalEarnings` (the old behaviour), so historic users are not paid again.
 *
 * Pass the RAW stats-doc value for `existingAwarded` (undefined when the field is
 * missing), not a defaults-merged object.
 */
function auditUserMilestones(
  successfulReferrals: number,
  totalEarnings: number,
  signupBonusAmount: number,
  existingAwarded: unknown,
  config: ReferralConfig
): MilestoneAuditResult {
  const trustAwarded = Array.isArray(existingAwarded);
  const awarded = new Set<number>(
    trustAwarded ? (existingAwarded as unknown[]).map((v) => Number(v)).filter((v) => Number.isFinite(v)) : []
  );
  let toCredit = 0;
  const awardedNow: number[] = [];

  const baseEarnings = (successfulReferrals * config.rewardPerReferral) + signupBonusAmount;
  let extraEarnings = trustAwarded ? 0 : Math.max(0, totalEarnings - baseEarnings);

  for (const m of milestoneList(config)) {
    if (successfulReferrals < m.count || awarded.has(m.count)) continue;
    if (!trustAwarded && extraEarnings >= m.bonus) {
      // Legacy: bonus already included in past totalEarnings.
      extraEarnings -= m.bonus;
      awarded.add(m.count);
    } else {
      toCredit += m.bonus;
      awarded.add(m.count);
      awardedNow.push(m.count);
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

/**
 * Money-relevant referral stats.
 *
 * `referral_stats/{uid}` is server-only and authoritative. `users/{uid}.referralStats`
 * is only a display mirror and used to be client-writable, so it must never decide a
 * balance, a withdrawal or a milestone (it used to override referral_stats here, which
 * let a user write their own balance and withdraw it). Only a block flag is honoured
 * from the mirror, because it can only ever restrict.
 */
function getCombinedReferralStats(userData: any = {}, statsData: any = {}) {
  const mirror = userData?.referralStats || {};
  return {
    ...statsData,
    isBlocked: getBooleanValue(statsData?.isBlocked) || getBooleanValue(mirror.isBlocked)
  };
}

/**
 * Defaults for stats fields that are still missing on referral_stats, as plain keys.
 * Never overwrites existing values (the old `set({...DEFAULTS, ...})` reset balances).
 */
function missingStatsDefaults(existingStats: any = {}) {
  const updates: { [key: string]: any } = {};
  for (const [field, defaultValue] of Object.entries(DEFAULT_REFERRAL_STATS)) {
    if (existingStats?.[field] === undefined) updates[field] = defaultValue;
  }
  return updates;
}

/** Missing mirror defaults as dotted paths — ONLY valid with update(), never set(). */
function buildMissingReferralStatsUpdates(existingStats: any = {}) {
  const updates: { [key: string]: any } = {};
  for (const [field, defaultValue] of Object.entries(DEFAULT_REFERRAL_STATS)) {
    if (existingStats?.[field] === undefined) updates[`referralStats.${field}`] = defaultValue;
  }
  return updates;
}

/**
 * Keeps the users/{uid}.referralStats display mirror in sync.
 * Uses a nested object: `set()` does NOT treat "referralStats.x" keys as paths (it
 * wrote literal top-level fields before, so the mirror silently went stale).
 */
function mirrorUserReferralStats(
  writer: FirebaseFirestore.Transaction | FirebaseFirestore.WriteBatch,
  userId: string,
  fields: { [key: string]: any }
) {
  const ref = db.collection("users").doc(userId);
  (writer as any).set(ref, { referralStats: fields }, { merge: true });
}

/** Referral codes may only be applied by recently created accounts (anti code-swapping). */
const REFERRAL_APPLY_WINDOW_DAYS = 14;

async function isAccountNewEnoughForReferral(userId: string): Promise<boolean> {
  try {
    const user = await admin.auth().getUser(userId);
    const createdMs = Date.parse(user.metadata.creationTime || "");
    if (!Number.isFinite(createdMs)) return true;
    return Date.now() - createdMs <= REFERRAL_APPLY_WINDOW_DAYS * ONE_DAY_MS;
  } catch (error) {
    functions.logger.warn("REFERRAL: account age check failed for " + userId, error);
    return false;
  }
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
    // Only the server-only referral_stats doc is trusted here. users.referralCode is
    // client-writable and let a user "claim" any unused code.
    const userQuery = { empty: true, docs: [] as FirebaseFirestore.QueryDocumentSnapshot[] };
    const statsQuery = await db.collection("referral_stats").where("referralCode", "==", normalizedCode).limit(1).get();

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
            const statsRef = db.collection("referral_stats").doc(userId);
            const statsDoc = await transaction.get(statsRef);
            transaction.update(userRef, {
              referralCode,
              referralCodeCreatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            // Fill only missing defaults: the old DEFAULTS spread reset an already
            // credited signup bonus / pending counts to 0 when the code was created late.
            transaction.set(statsRef, {
              userId,
              userRole,
              ...missingStatsDefaults(statsDoc.data() || {}),
              referralCode,
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

    let existingCode = normalizeReferralCodeInput(
      statsData.referralCode || userData.referralCode || ""
    );
    if (existingCode) {
      const ownerDoc = await db.collection("referral_codes").doc(existingCode).get();
      if (ownerDoc.exists && getStringValue(ownerDoc.get("userId")) !== userId) {
        // users.referralCode pointed at another user's code: ignore it.
        existingCode = "";
      }
    }

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
          ...missingStatsDefaults(statsData),
          referralCode: existingCode,
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
          ...missingStatsDefaults(statsData),
          referralCode: foundCode,
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
          const freshStatsDoc = await transaction.get(statsRef);

          transaction.set(userRef, {
            referralCode: newCode,
            referralCodeCreatedAt: admin.firestore.FieldValue.serverTimestamp()
          }, { merge: true });

          // Never seed money fields from users.referralStats (client-writable) and never
          // overwrite existing stats with defaults.
          transaction.set(statsRef, {
            userId,
            userRole: resolvedRole,
            ...missingStatsDefaults(freshStatsDoc.data() || {}),
            referralCode: newCode,
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

    // Only new accounts may apply a code; stops existing users swapping codes for money.
    if (!(await isAccountNewEnoughForReferral(newUserId))) {
      return { success: false, error: "Referral codes can only be used when you first join DutyPe" };
    }

    const config = await getReferralConfig();

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

      if (getBooleanValue(referrerStats.isBlocked)) {
        return { success: false, error: "Referral account is restricted" };
      }
      const currentSuccessful = getNumberValue(referrerStats.successfulReferrals);
      const newSuccessfulCount = currentSuccessful + 1;
      const currentEarnings = getNumberValue(referrerStats.totalEarnings);
      const signupBonus = getNumberValue(referrerStats.signupBonusReceived ? referrerStats.signupBonusAmount : 0);
      const audit = auditUserMilestones(
        newSuccessfulCount, currentEarnings, signupBonus,
        (referrerLegacyStatsDoc.data() || {}).awardedMilestones, config
      );
      const referrerReward = config.rewardPerReferral;
      const referredUserReward = config.signupBonus;
      const milestoneBonus = audit.toCredit;
      const totalReferrerReward = referrerReward + milestoneBonus;
      const currentBalance = getNumberValue(referrerStats.availableBalance);
      const newTier = calculateTier(newSuccessfulCount);
      const newCanWithdraw = canWithdraw(currentBalance + totalReferrerReward, config.minWithdrawal);
      const newNextMilestone = getNextMilestone(newSuccessfulCount, config);
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
        expiresAt: isProfileCompleted ? null : admin.firestore.Timestamp.fromMillis(Date.now() + REFERRAL_CONFIG.REFERRAL_EXPIRY_DAYS * ONE_DAY_MS),
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
          ...missingStatsDefaults(referrerLegacyStatsDoc.data() || {}),
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
          ...missingStatsDefaults(newUserLegacyStatsDoc.data() || {}),
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
    const config = await getReferralConfig();
    const pendingQuery = db.collection("referrals")
      .where("referredUserId", "==", referredUserId)
      .where("status", "==", "PENDING")
      .limit(1);

    // Cheap pre-check outside the transaction: nothing pending → maybe heal, else stop.
    const pendingPeek = await pendingQuery.get();
    if (pendingPeek.empty) {
      return await healMissingReferral(referredUserId, afterData);
    }

    // Everything that credits money happens in ONE transaction that re-reads the referral.
    // Profile saves fire up to three triggers (users / worker_profiles / employer_profiles)
    // at once; the losers of the race see status != PENDING and credit nothing.
    const outcome = await db.runTransaction(async (transaction) => {
      const pendingSnap = await transaction.get(pendingQuery);
      if (pendingSnap.empty) return { status: "COMPLETED" };

      const referralDoc = pendingSnap.docs[0];
      const referral = referralDoc.data() || {};
      const referralId = referralDoc.id;
      const referrerUserId = getStringValue(referral.referrerUserId || referral.referrerId);
      if (!referrerUserId || referrerUserId === referredUserId) {
        transaction.update(referralDoc.ref, { status: "REJECTED", rejectionReason: "INVALID_REFERRER" });
        return { status: "REJECTED" };
      }

      const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
      const referredStatsRef = db.collection("referral_stats").doc(referredUserId);
      const referrerUserRef = db.collection("users").doc(referrerUserId);
      const [referrerStatsDoc, referredStatsDoc, referrerUserDoc] = await transaction.getAll(
        referrerStatsRef, referredStatsRef, referrerUserRef
      );

      const expiresAtMs = referral.expiresAt?.toMillis ? referral.expiresAt.toMillis() : 0;
      if (expiresAtMs > 0 && Date.now() > expiresAtMs) {
        transaction.update(referralDoc.ref, {
          status: "EXPIRED",
          completedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        const expiredFields = {
          pendingReferrals: admin.firestore.FieldValue.increment(-1),
          expiredReferrals: admin.firestore.FieldValue.increment(1),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
        };
        transaction.set(referrerStatsRef, expiredFields, { merge: true });
        mirrorUserReferralStats(transaction, referrerUserId, expiredFields);
        return { status: "EXPIRED" };
      }

      const referrerRaw = referrerStatsDoc.data() || {};
      const referrerStats = getCombinedReferralStats(referrerUserDoc.data() || {}, referrerRaw);
      if (getBooleanValue(referrerStats.isBlocked)) {
        transaction.update(referralDoc.ref, { status: "REJECTED", rejectionReason: "REFERRER_BLOCKED" });
        return { status: "REJECTED" };
      }

      const newSuccessfulCount = getNumberValue(referrerStats.successfulReferrals) + 1;
      const audit = auditUserMilestones(
        newSuccessfulCount,
        getNumberValue(referrerStats.totalEarnings),
        getNumberValue(referrerStats.signupBonusReceived ? referrerStats.signupBonusAmount : 0),
        referrerRaw.awardedMilestones,
        config
      );
      const referrerReward = config.rewardPerReferral;
      const referredUserReward = config.signupBonus;
      const milestoneBonus = audit.toCredit;
      const totalReferrerReward = referrerReward + milestoneBonus;
      const newTier = calculateTier(newSuccessfulCount);
      const newCanWithdraw = canWithdraw(
        getNumberValue(referrerStats.availableBalance) + totalReferrerReward,
        config.minWithdrawal
      );
      const newNextMilestone = getNextMilestone(newSuccessfulCount, config);

      const referrerRole = getStringValue(
        referrerUserDoc.get("activeRole") || referrerUserDoc.get("role") || referrerStats.userRole,
        "WORKER"
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

      const now = admin.firestore.FieldValue.serverTimestamp();

      // 1. Referral → COMPLETED
      transaction.update(referralDoc.ref, {
        status: "COMPLETED",
        profileCompleted: true,
        rewardAmount: referrerReward,
        bonusAmount: milestoneBonus,
        referredUserReward,
        completedAt: now
      });

      // 2. Referrer stats (authoritative) + display mirror
      const referrerFields: { [key: string]: any } = {
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
        lastUpdated: now
      };
      if (referrerRole === "EMPLOYER" && freePostingsExpiry) {
        referrerFields.freeJobPostings = freePostings;
        referrerFields.freeJobPostingsExpiry = freePostingsExpiry;
      }
      transaction.set(referrerStatsRef, { ...missingStatsDefaults(referrerRaw), ...referrerFields }, { merge: true });
      mirrorUserReferralStats(transaction, referrerUserId, referrerFields);

      // 3. Referred user's signup bonus (only once)
      const referredRaw = referredStatsDoc.data() || {};
      const creditReferred = !getBooleanValue(referredRaw.signupBonusReceived);
      if (creditReferred) {
        const referredFields = {
          userId: referredUserId,
          userRole: getStringValue(afterData?.activeRole || afterData?.role || referral.referredRole, "WORKER"),
          totalEarnings: admin.firestore.FieldValue.increment(referredUserReward),
          availableBalance: admin.firestore.FieldValue.increment(referredUserReward),
          signupBonusReceived: true,
          signupBonusAmount: referredUserReward,
          lastUpdated: now
        };
        transaction.set(referredStatsRef, { ...missingStatsDefaults(referredRaw), ...referredFields }, { merge: true });
        mirrorUserReferralStats(transaction, referredUserId, referredFields);
      }

      // 4. Code usage counter
      const codeId = normalizeReferralCodeInput(getStringValue(referral.referralCode));
      if (codeId) {
        transaction.set(db.collection("referral_codes").doc(codeId), {
          successfulReferrals: admin.firestore.FieldValue.increment(1)
        }, { merge: true });
      }

      // 5. Events + notifications (inside the transaction, so they are written exactly once)
      transaction.set(db.collection("referral_events").doc(), {
        eventType: "REWARD_CREDITED",
        userId: referrerUserId,
        referralId,
        amount: totalReferrerReward,
        bonusAmount: milestoneBonus,
        newTier,
        timestamp: now
      });
      transaction.set(db.collection("notifications").doc(), {
        recipientId: referrerUserId,
        title: "🎉 Referral Successful!",
        message: `${referral.referredUserName || "Someone"} joined using your code! You earned ₹${totalReferrerReward}${milestoneBonus > 0 ? ` (includes ₹${milestoneBonus} milestone bonus!)` : ""}`,
        type: "REFERRAL_REWARD",
        data: { referralId, amount: totalReferrerReward },
        createdAt: now,
        isRead: false
      });
      if (audit.awardedNow.length > 0) {
        transaction.set(db.collection("referral_events").doc(), {
          eventType: "MILESTONE_REACHED",
          userId: referrerUserId,
          milestones: audit.awardedNow,
          bonusAmount: milestoneBonus,
          newSuccessfulCount,
          timestamp: now
        });
      }
      if (creditReferred) {
        transaction.set(db.collection("referral_events").doc(), {
          eventType: "SIGNUP_BONUS_CREDITED",
          userId: referredUserId,
          referralId,
          amount: referredUserReward,
          timestamp: now
        });
        transaction.set(db.collection("notifications").doc(), {
          recipientId: referredUserId,
          title: "🎁 Welcome Bonus!",
          message: `You earned ₹${referredUserReward} for joining with a referral code!`,
          type: "SIGNUP_BONUS",
          data: { amount: referredUserReward },
          createdAt: now,
          isRead: false
        });
      }

      return {
        status: "COMPLETED",
        referrerReward: totalReferrerReward,
        referredUserReward: creditReferred ? referredUserReward : 0,
        milestoneBonus,
        newTier
      };
    });

    functions.logger.info(`🎁 REFERRAL: processPending for ${referredUserId} → ${outcome.status}`);
    return outcome;
  } catch (error) {
    functions.logger.error(`🎁 REFERRAL: Error completing referral:`, error);
    return null;
  }
}

/**
 * The user signed up with a code but no referral record exists (e.g. the apply call
 * failed). Re-run the normal, fully guarded application (code lookup, self-referral,
 * fraud checks, account-age window) using the code stored at registration. The old
 * heal trusted a client-writable referredByUserId and bypassed every check.
 */
async function healMissingReferral(referredUserId: string, afterData?: any) {
  const anyReferral = await db.collection("referrals")
    .where("referredUserId", "==", referredUserId)
    .limit(1)
    .get();
  if (!anyReferral.empty) {
    return { status: getStringValue(anyReferral.docs[0].get("status"), "COMPLETED") };
  }

  const [uDoc, wDoc, eDoc] = await Promise.all([
    db.collection("users").doc(referredUserId).get(),
    db.collection("worker_profiles").doc(referredUserId).get(),
    db.collection("employer_profiles").doc(referredUserId).get()
  ]);
  const merged = { ...(uDoc.data() || {}), ...(wDoc.data() || {}), ...(eDoc.data() || {}), ...(afterData || {}) };
  const refCode = normalizeReferralCodeInput(merged.referredByCode || "");
  if (!refCode) return null;

  const result = await executeReferralApplication({
    newUserId: referredUserId,
    referralCode: refCode,
    newUserRole: getStringValue(merged.activeRole || merged.role, "WORKER"),
    newUserName: getStringValue(merged.fullName || merged.name || merged.companyName, ""),
    newUserPhone: getStringValue(merged.phone || merged.phoneNumber, "")
  });
  functions.logger.info(`🎁 REFERRAL: heal for ${referredUserId} with ${refCode} → ${result.success ? "applied" : result.error}`);
  return result.success ? { status: "COMPLETED" } : null;
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

    const before = change.before.exists ? change.before.data() : null;
    if (isWorkerProfileComplete(after) && !(before && isWorkerProfileComplete(before))) {
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

    const before = change.before.exists ? change.before.data() : null;
    if (isEmployerProfileComplete(after) && !(before && isEmployerProfileComplete(before))) {
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
  // Maintenance job that credits money across ALL users: admins only
  // (any signed-in user could trigger it before).
  if (!(await isCallerAdmin(context))) {
    throw new functions.https.HttpsError("permission-denied", "admin only");
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

    const [workerDoc, employerDoc] = await Promise.all([
      db.collection("worker_profiles").doc(referredUserId).get(),
      db.collection("employer_profiles").doc(referredUserId).get()
    ]);
    const workerData = workerDoc.exists ? workerDoc.data() : null;
    const employerData = employerDoc.exists ? employerDoc.data() : null;
    const isComplete = Boolean(
      (workerData && isWorkerProfileComplete(workerData)) ||
      (employerData && isEmployerProfileComplete(employerData))
    );

    if (isComplete) {
      // Same transactional path as the triggers: can never double-credit.
      const outcome = await processPendingReferralOnProfileComplete(referredUserId, workerData || employerData);
      results.push({ referralId: doc.id, referredUserId, status: outcome?.status || "ERROR" });
    } else {
      results.push({ referralId: doc.id, referredUserId, status: "PENDING", reason: "Profile incomplete" });
    }
  }

  // Milestone reconciliation for active referrers (transactional per user).
  let milestonesFixed = 0;
  try {
    const statsSnapshot = await db.collection("referral_stats")
      .where("successfulReferrals", ">=", 1)
      .limit(200)
      .get();
    for (const statDoc of statsSnapshot.docs) {
      const before = statDoc.get("awardedMilestones");
      const after = await reconcileMilestones(statDoc.id);
      if (JSON.stringify(before) !== JSON.stringify(after.awardedMilestones)) milestonesFixed++;
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
        mirrorUserReferralStats(batch, referrerId, {
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
  if (!data || typeof data !== "object" || typeof data.amount !== "number") {
    throw new functions.https.HttpsError("invalid-argument", "A numeric amount and request details are required");
  }
  if (data.userId !== undefined && data.userId !== userId) {
    throw new functions.https.HttpsError("permission-denied", "Account changed before withdrawal");
  }
  const config = await getReferralConfig();
  const minWithdrawal = config.minWithdrawal;
  const maxPerDay = config.maxWithdrawalPerDay;
  if (typeof data.amount === "number" && data.amount > maxPerDay) {
    return { success: false, error: `You can withdraw up to ₹${maxPerDay} per day` };
  }
  const amount = validateNumber(data.amount, "amount", {
    required: true, min: minWithdrawal, max: maxPerDay
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
      // Balance comes ONLY from the server-owned referral_stats doc.
      const availablePaise = Math.round(getNumberValue(stats.availableBalance) * 100);
      if (availablePaise < minWithdrawal * 100) {
        return { success: false, error: `Minimum balance of ₹${minWithdrawal} required to withdraw` };
      }
      if (!Number.isSafeInteger(availablePaise) || amountPaise > availablePaise) {
        return { success: false, error: "Insufficient referral balance" };
      }
      if (amount < minWithdrawal) {
        return { success: false, error: `Minimum withdrawal amount is ₹${minWithdrawal}` };
      }
      let dailyPaise = getNumberValue(dailyDoc.get("amountPaise"));
      let dailyCount = getNumberValue(dailyDoc.get("requestCount"));
      if (!dailyDoc.exists) {
        const previousRequests = await transaction.get(db.collection("withdrawal_requests")
          .where("userId", "==", userId).where("createdAt", ">=", today));
        dailyPaise = previousRequests.docs.reduce((total, document) => total + Math.round(getNumberValue(document.get("amount")) * 100), 0);
        dailyCount = previousRequests.size;
      }
      if (dailyPaise + amountPaise > maxPerDay * 100 || dailyCount >= 5) {
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
      const withdrawalStats = {
        availableBalance,
        withdrawnAmount,
        totalWithdrawals,
        canWithdraw: canWithdraw(availableBalance, minWithdrawal),
        lastWithdrawalStatus: "PENDING",
        lastWithdrawalAt: timestamp,
        lastUpdated: timestamp
      };
      transaction.set(statsRef, { userId, userRole, ...withdrawalStats }, { merge: true });
      // Nested object: the old "referralStats.x" keys in set() wrote literal fields, so the
      // mirror kept the pre-withdrawal balance (and used to be read back as the balance).
      transaction.set(userRef, { referralStats: withdrawalStats }, { merge: true });
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
        mirrorUserReferralStats(batch, referrerUserId, {
          pendingReferrals: admin.firestore.FieldValue.increment(-1),
          rejectedReferrals: admin.firestore.FieldValue.increment(1),
          lastUpdated: admin.firestore.FieldValue.serverTimestamp()
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
/**
 * Credits any milestone bonus that is owed, atomically (safe against concurrent calls).
 * Returns the fresh authoritative stats doc data.
 */
async function reconcileMilestones(userId: string): Promise<{ [key: string]: any }> {
  const config = await getReferralConfig();
  const statsRef = db.collection("referral_stats").doc(userId);
  return await db.runTransaction(async (transaction) => {
    const statsDoc = await transaction.get(statsRef);
    const raw = statsDoc.data() || {};
    const successful = getNumberValue(raw.successfulReferrals);
    const firstMilestone = milestoneList(config)[0]?.count ?? Number.MAX_SAFE_INTEGER;
    if (!statsDoc.exists || successful < firstMilestone) return raw;

    const audit = auditUserMilestones(
      successful,
      getNumberValue(raw.totalEarnings),
      getNumberValue(raw.signupBonusReceived ? raw.signupBonusAmount : 0),
      raw.awardedMilestones,
      config
    );
    const awardedChanged = !Array.isArray(raw.awardedMilestones) ||
      audit.newAwardedMilestones.length !== raw.awardedMilestones.length;
    if (audit.toCredit <= 0 && !awardedChanged) return raw;

    const now = admin.firestore.FieldValue.serverTimestamp();
    const patch: { [key: string]: any } = {
      awardedMilestones: audit.newAwardedMilestones,
      nextMilestone: getNextMilestone(successful, config),
      lastUpdated: now
    };
    const next: { [key: string]: any } = { ...raw, awardedMilestones: audit.newAwardedMilestones };
    if (audit.toCredit > 0) {
      const newBalance = getNumberValue(raw.availableBalance) + audit.toCredit;
      patch.totalEarnings = admin.firestore.FieldValue.increment(audit.toCredit);
      patch.availableBalance = admin.firestore.FieldValue.increment(audit.toCredit);
      patch.canWithdraw = canWithdraw(newBalance, config.minWithdrawal);
      next.totalEarnings = getNumberValue(raw.totalEarnings) + audit.toCredit;
      next.availableBalance = newBalance;
      transaction.set(db.collection("notifications").doc(), {
        recipientId: userId,
        title: "🎉 Milestone Bonus Credited!",
        message: `Your ₹${audit.toCredit} milestone bonus for reaching ${successful} referrals has been credited to your balance!`,
        type: "MILESTONE_REWARD",
        data: { milestones: audit.awardedNow, amount: audit.toCredit },
        createdAt: now,
        isRead: false
      });
      transaction.set(db.collection("referral_events").doc(), {
        eventType: "MILESTONE_REACHED",
        userId,
        milestones: audit.awardedNow,
        bonusAmount: audit.toCredit,
        newSuccessfulCount: successful,
        timestamp: now
      });
    }
    transaction.set(statsRef, patch, { merge: true });
    mirrorUserReferralStats(transaction, userId, patch);
    return next;
  });
}

export const getReferralStats = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  // Only admins may look at someone else's stats (it used to accept any userId).
  const requestedUserId = getStringValue(data?.userId, context.auth.uid);
  if (requestedUserId !== context.auth.uid && !(await isCallerAdmin(context))) {
    throw new functions.https.HttpsError("permission-denied", "You can only view your own referral stats");
  }
  const userId = requestedUserId;

  try {
    const config = await getReferralConfig();
    const [userDoc, statsDoc] = await Promise.all([
      db.collection("users").doc(userId).get(),
      db.collection("referral_stats").doc(userId).get()
    ]);

    if (!userDoc.exists && !statsDoc.exists) {
      return { exists: false };
    }

    const userData = userDoc.data() || {};
    const reconciled = statsDoc.exists ? await reconcileMilestones(userId) : {};
    const stats: { [key: string]: any } = {
      userId,
      userRole: getStringValue(userData.activeRole || userData.role || reconciled.userRole, "WORKER"),
      referralCode: getStringValue(reconciled.referralCode || userData.referralCode),
      ...DEFAULT_REFERRAL_STATS,
      ...getCombinedReferralStats(userData, reconciled)
    };
    stats.canWithdraw = canWithdraw(getNumberValue(stats.availableBalance), config.minWithdrawal);
    stats.minWithdrawal = config.minWithdrawal;

    return { exists: true, stats };
  } catch (error) {
    if (error instanceof functions.https.HttpsError) throw error;
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
  const limit = Math.min(Math.max(Number(data?.limit) || 20, 1), 100);

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
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }
  const role = data?.role === "WORKER" || data?.role === "EMPLOYER" ? data.role : undefined;
  const limit = Math.min(Math.max(Number(data?.limit) || 10, 1), 50);

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
      // Public fields only: this used to return whole stats docs (balances, withdrawals).
      leaderboard: leaderboard.docs.map((doc, index) => ({
        rank: index + 1,
        userId: doc.id,
        userRole: getStringValue(doc.get("userRole"), "WORKER"),
        successfulReferrals: getNumberValue(doc.get("successfulReferrals")),
        totalEarnings: getNumberValue(doc.get("totalEarnings")),
        currentTier: getStringValue(doc.get("currentTier"), "BRONZE")
      }))
    };

  } catch (error) {
    functions.logger.error("Error getting leaderboard:", error);
    throw new functions.https.HttpsError("internal", "Failed to get leaderboard");
  }
});


