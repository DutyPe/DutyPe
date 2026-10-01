"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.verifySubscriptionPayment = exports.DEFAULT_PLANS = void 0;
/**
 * Employer subscriptions. The employer pays by UPI and uploads proof
 * (subscription_payment_requests, client-created as PENDING); an admin verifies it here and the
 * plan's credits are added to employer_profiles/{uid}.subscription in the same transaction.
 */
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const notify_1 = require("./lib/notify");
const app_config_1 = require("./app-config");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
/**
 * Built-in plans, used when app_config/subscription_plans is missing (the app has the same list in
 * SubscriptionRepository). ₹99 has no AI; ₹199 and ₹299 include DutyPe AI.
 */
exports.DEFAULT_PLANS = [
    { id: "basic_99", pricePaise: 9900, jobs: 3, instantUnlocks: 5, validityDays: 30, ai: false },
    { id: "pro_ai_199", pricePaise: 19900, jobs: 6, instantUnlocks: 15, validityDays: 30, ai: true, aiPerDay: 100 },
    { id: "max_ai_299", pricePaise: 29900, jobs: 12, instantUnlocks: 40, validityDays: 30, ai: true, aiPerDay: 300 },
    // Earlier plans, kept so pending payments for them can still be verified.
    { id: "single_49", pricePaise: 4900, jobs: 1, instantUnlocks: 0, validityDays: 30, legacy: true },
    { id: "starter_99", pricePaise: 9900, jobs: 3, instantUnlocks: 5, validityDays: 30, legacy: true },
    { id: "growth_149", pricePaise: 14900, jobs: 6, instantUnlocks: 15, validityDays: 30, legacy: true },
    { id: "premium_299", pricePaise: 29900, jobs: 12, instantUnlocks: 40, validityDays: 30, ai: true, aiPerDay: 300, legacy: true },
];
async function planById(planId) {
    const doc = await db.collection(schema_1.AppConfig.COLLECTION).doc(schema_1.AppConfig.DOC_SUBSCRIPTION_PLANS).get();
    const configured = doc.get("plans") || [];
    return configured.find((p) => p.id === planId) || exports.DEFAULT_PLANS.find((p) => p.id === planId) || null;
}
exports.verifySubscriptionPayment = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const requestId = (0, input_1.str)(data, "requestId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    const approve = data.approve === true;
    const reason = (0, input_1.str)(data, "reason", { max: 300, optional: true });
    const ref = db.collection(schema_1.SubscriptionPayments.COLLECTION).doc(requestId);
    const pre = await ref.get();
    if (!pre.exists)
        (0, input_1.fail)("not-found", "Payment request not found");
    const plan = approve ? await planById(String(pre.get(schema_1.SubscriptionPayments.PLAN_ID))) : null;
    if (approve && !plan)
        (0, input_1.fail)("failed-precondition", "The plan in this request no longer exists");
    const employerId = await db.runTransaction(async (tx) => {
        const req = await tx.get(ref);
        if (req.get(schema_1.SubscriptionPayments.STATUS) !== "PENDING")
            (0, input_1.fail)("failed-precondition", "Already processed");
        const uid = String(req.get(schema_1.SubscriptionPayments.EMPLOYER_ID));
        const now = Date.now();
        tx.update(ref, Object.assign({ [schema_1.SubscriptionPayments.STATUS]: approve ? "VERIFIED" : "REJECTED", [schema_1.SubscriptionPayments.VERIFIED_AT]: Timestamp.fromMillis(now) }, (reason ? { [schema_1.SubscriptionPayments.REJECTION_REASON]: reason } : {})));
        if (approve && plan) {
            const S = schema_1.EmployerProfiles.Subscription;
            tx.set(db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid), {
                [schema_1.EmployerProfiles.SUBSCRIPTION]: {
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
    await (0, notify_1.notify)(employerId, {
        type: "PAYMENT",
        templateId: approve ? "SUBSCRIPTION_ACTIVATED" : "SUBSCRIPTION_REJECTED",
        role: schema_1.Values.Role.EMPLOYER,
        params: { plan: (plan === null || plan === void 0 ? void 0 : plan.id) || "", reason: reason || "" },
        data: { requestId },
    });
    return { requestId, status: approve ? "VERIFIED" : "REJECTED" };
});
//# sourceMappingURL=subscriptions.js.map