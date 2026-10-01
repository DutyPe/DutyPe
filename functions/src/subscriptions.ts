/**
 * Employer subscriptions. The employer pays by UPI and uploads proof
 * (subscription_payment_requests, client-created as PENDING); an admin verifies it here and the
 * plan's credits are added to employer_profiles/{uid}.subscription in the same transaction.
 */
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import { notify } from "./lib/notify";
import { isCallerAdmin } from "./app-config";
import { AppConfig, EmployerProfiles, SubscriptionPayments, Values } from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;

export interface PlanRow {
  id: string; jobs?: number; instantUnlocks?: number; validityDays?: number; pricePaise?: number;
  /** Includes DutyPe AI (talk-to-post, AI top picks, voice assistant). */
  ai?: boolean;
  /** DutyPe AI actions per day. */
  aiPerDay?: number;
  /** Not offered any more; still valid for verifying payments made earlier. */
  legacy?: boolean;
}

/**
 * Built-in plans, used when app_config/subscription_plans is missing (the app has the same list in
 * SubscriptionRepository). ₹99 has no AI; ₹199 and ₹299 include DutyPe AI.
 */
export const DEFAULT_PLANS: PlanRow[] = [
  { id: "basic_99", pricePaise: 9_900, jobs: 3, instantUnlocks: 5, validityDays: 30, ai: false },
  { id: "pro_ai_199", pricePaise: 19_900, jobs: 6, instantUnlocks: 15, validityDays: 30, ai: true, aiPerDay: 100 },
  { id: "max_ai_299", pricePaise: 29_900, jobs: 12, instantUnlocks: 40, validityDays: 30, ai: true, aiPerDay: 300 },
  // Earlier plans, kept so pending payments for them can still be verified.
  { id: "single_49", pricePaise: 4_900, jobs: 1, instantUnlocks: 0, validityDays: 30, legacy: true },
  { id: "starter_99", pricePaise: 9_900, jobs: 3, instantUnlocks: 5, validityDays: 30, legacy: true },
  { id: "growth_149", pricePaise: 14_900, jobs: 6, instantUnlocks: 15, validityDays: 30, legacy: true },
  { id: "premium_299", pricePaise: 29_900, jobs: 12, instantUnlocks: 40, validityDays: 30, ai: true, aiPerDay: 300, legacy: true },
];

async function planById(planId: string): Promise<PlanRow | null> {
  const doc = await db.collection(AppConfig.COLLECTION).doc(AppConfig.DOC_SUBSCRIPTION_PLANS).get();
  const configured = (doc.get("plans") as PlanRow[] | undefined) || [];
  return configured.find((p) => p.id === planId) || DEFAULT_PLANS.find((p) => p.id === planId) || null;
}

export const verifySubscriptionPayment = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const data = obj(raw);
  const requestId = str(data, "requestId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const approve = data.approve === true;
  const reason = str(data, "reason", { max: 300, optional: true });
  const ref = db.collection(SubscriptionPayments.COLLECTION).doc(requestId);

  const pre = await ref.get();
  if (!pre.exists) fail("not-found", "Payment request not found");
  const plan = approve ? await planById(String(pre.get(SubscriptionPayments.PLAN_ID))) : null;
  if (approve && !plan) fail("failed-precondition", "The plan in this request no longer exists");

  const employerId = await db.runTransaction(async (tx) => {
    const req = await tx.get(ref);
    if (req.get(SubscriptionPayments.STATUS) !== "PENDING") fail("failed-precondition", "Already processed");
    const uid = String(req.get(SubscriptionPayments.EMPLOYER_ID));
    const now = Date.now();
    tx.update(ref, {
      [SubscriptionPayments.STATUS]: approve ? "VERIFIED" : "REJECTED",
      [SubscriptionPayments.VERIFIED_AT]: Timestamp.fromMillis(now),
      ...(reason ? { [SubscriptionPayments.REJECTION_REASON]: reason } : {}),
    });
    if (approve && plan) {
      const S = EmployerProfiles.Subscription;
      tx.set(db.collection(EmployerProfiles.COLLECTION).doc(uid), {
        [EmployerProfiles.SUBSCRIPTION]: {
          [S.PLAN_ID]: plan.id,
          [S.STATUS]: "ACTIVE",
          [S.START_AT]: Timestamp.fromMillis(now),
          [S.EXPIRES_AT]: Timestamp.fromMillis(now + (plan.validityDays || 30) * DAY_MS),
          [S.CREDITS]: {
            [S.CREDITS_NORMAL]: FieldValue.increment(plan.jobs || 0),
            [S.CREDITS_INSTANT]: FieldValue.increment(plan.instantUnlocks || 0),
          },
          [S.AI]: plan.ai === true,
          [S.AI_PER_DAY]: plan.ai === true ? plan.aiPerDay || 100 : 0,
        },
      }, { merge: true });
    }
    return uid;
  });

  await notify(employerId, {
    type: "PAYMENT",
    templateId: approve ? "SUBSCRIPTION_ACTIVATED" : "SUBSCRIPTION_REJECTED",
    role: Values.Role.EMPLOYER,
    params: { plan: plan?.id || "", reason: reason || "" },
    data: { requestId },
  });
  return { requestId, status: approve ? "VERIFIED" : "REJECTED" };
});
