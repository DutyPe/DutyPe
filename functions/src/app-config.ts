/**
 * Dynamic referral configuration — source of truth is
 * /app_config/referral. Cached in-process for 60s to keep Firestore reads
 * bounded under heavy CF fan-out.
 *
 * Shape (all numbers in INR unless stated):
 *   rewardPerReferral:  number   // default 25
 *   signupBonus:        number   // default 25
 *   minWithdrawal:      number   // default 50
 *   maxWithdrawalPerDay:number   // default 1000
 *   milestones:         { [count:string]: number }
 *   withdrawalMilestones: number[]
 *   updatedAt:          Timestamp
 *   updatedBy:          string  (admin uid)
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export interface ReferralConfig {
  rewardPerReferral: number;
  signupBonus: number;
  minWithdrawal: number;
  maxWithdrawalPerDay: number;
  milestones: Record<string, number>;
  withdrawalMilestones: number[];
}

export const DEFAULT_REFERRAL_CONFIG: ReferralConfig = {
  rewardPerReferral: 25,
  signupBonus: 25,
  minWithdrawal: 50,
  maxWithdrawalPerDay: 1000,
  milestones: { "5": 50, "10": 100, "15": 150, "25": 250, "50": 500, "100": 1000 },
  withdrawalMilestones: [5, 10, 15],
};

const TTL_MS = 60 * 1000;
let cache: { value: ReferralConfig; expiresAt: number } | null = null;

export async function getReferralConfig(): Promise<ReferralConfig> {
  const now = Date.now();
  if (cache && cache.expiresAt > now) return cache.value;

  try {
    const snap = await admin.firestore().doc("app_config/referral").get();
    const data = snap.exists ? (snap.data() || {}) : {};
    const merged: ReferralConfig = {
      rewardPerReferral: num(data.rewardPerReferral, DEFAULT_REFERRAL_CONFIG.rewardPerReferral),
      signupBonus: num(data.signupBonus, DEFAULT_REFERRAL_CONFIG.signupBonus),
      minWithdrawal: num(data.minWithdrawal, DEFAULT_REFERRAL_CONFIG.minWithdrawal),
      maxWithdrawalPerDay: num(data.maxWithdrawalPerDay, DEFAULT_REFERRAL_CONFIG.maxWithdrawalPerDay),
      milestones: (data.milestones && typeof data.milestones === "object")
        ? data.milestones as Record<string, number>
        : DEFAULT_REFERRAL_CONFIG.milestones,
      withdrawalMilestones: Array.isArray(data.withdrawalMilestones)
        ? data.withdrawalMilestones.map((v: unknown) => Number(v)).filter((v) => Number.isFinite(v))
        : DEFAULT_REFERRAL_CONFIG.withdrawalMilestones,
    };
    cache = { value: merged, expiresAt: now + TTL_MS };
    return merged;
  } catch (e: any) {
    functions.logger.warn("getReferralConfig failed — falling back to defaults", { err: e?.message });
    return DEFAULT_REFERRAL_CONFIG;
  }
}

function num(v: unknown, d: number): number {
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : d;
}

/**
 * Admin-only callable to update /app_config/referral.
 * Authorised via the `admin` custom claim on the caller. Set the claim with:
 *   admin.auth().setCustomUserClaims(uid, { admin: true });
 */
export const updateReferralConfig = functions
  .region("asia-south1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError("unauthenticated", "login required");
    }
    const claims = (context.auth.token || {}) as Record<string, unknown>;
    if (!claims.admin) {
      throw new functions.https.HttpsError("permission-denied", "admin only");
    }

    const patch: Partial<ReferralConfig> = {};
    if (data?.rewardPerReferral !== undefined) patch.rewardPerReferral = numStrict(data.rewardPerReferral, "rewardPerReferral", 0, 10_000);
    if (data?.signupBonus !== undefined) patch.signupBonus = numStrict(data.signupBonus, "signupBonus", 0, 10_000);
    if (data?.minWithdrawal !== undefined) patch.minWithdrawal = numStrict(data.minWithdrawal, "minWithdrawal", 1, 100_000);
    if (data?.maxWithdrawalPerDay !== undefined) patch.maxWithdrawalPerDay = numStrict(data.maxWithdrawalPerDay, "maxWithdrawalPerDay", 1, 10_000_000);
    if (data?.milestones !== undefined) {
      if (!data.milestones || typeof data.milestones !== "object") {
        throw new functions.https.HttpsError("invalid-argument", "milestones must be object");
      }
      const out: Record<string, number> = {};
      for (const [k, v] of Object.entries(data.milestones)) {
        out[String(k)] = numStrict(v, `milestones.${k}`, 0, 100_000);
      }
      patch.milestones = out;
    }
    if (data?.withdrawalMilestones !== undefined) {
      if (!Array.isArray(data.withdrawalMilestones)) {
        throw new functions.https.HttpsError("invalid-argument", "withdrawalMilestones must be array");
      }
      patch.withdrawalMilestones = data.withdrawalMilestones
        .map((v: unknown) => numStrict(v, "withdrawalMilestones[]", 0, 10_000));
    }

    if (Object.keys(patch).length === 0) {
      throw new functions.https.HttpsError("invalid-argument", "no valid fields to update");
    }

    await admin.firestore().doc("app_config/referral").set({
      ...patch,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedBy: context.auth.uid,
    }, { merge: true });

    // Invalidate in-process cache so subsequent CF calls see the new value.
    cache = null;
    functions.logger.info("updateReferralConfig: admin=" + context.auth.uid + " keys=" + Object.keys(patch).join(","));
    return { success: true };
  });

function numStrict(v: unknown, field: string, min: number, max: number): number {
  const n = Number(v);
  if (!Number.isFinite(n) || n < min || n > max) {
    throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
  }
  return n;
}

export const getReferralConfigCallable = functions
  .region("asia-south1")
  .https.onCall(async () => {
    return await getReferralConfig();
  });
