"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.razorpayWebhook = exports.verifyRazorpayPayment = exports.createRazorpayOrder = exports.RAZORPAY_WEBHOOK_SECRET = exports.RAZORPAY_KEY_SECRET = exports.RAZORPAY_KEY_ID = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const subscriptions_1 = require("./subscriptions");
const schema_1 = require("./schema");
const notify_1 = require("./lib/notify");
const referrals_1 = require("./referrals");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
exports.RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID || "rzp_live_TvEYRD4s7DgLuj";
exports.RAZORPAY_KEY_SECRET = process.env.RAZORPAY_KEY_SECRET || "rN8NyKBPiL5rifYV4TXr2VXu";
exports.RAZORPAY_WEBHOOK_SECRET = process.env.RAZORPAY_WEBHOOK_SECRET || "rN8NyKBPiL5rifYV4TXr2VXu";
/**
 * Timing-safe string comparison to prevent timing analysis attacks on signatures.
 */
function timingSafeEqual(a, b) {
    if (typeof a !== "string" || typeof b !== "string")
        return false;
    const bufA = Buffer.from(a, "utf8");
    const bufB = Buffer.from(b, "utf8");
    if (bufA.length !== bufB.length)
        return false;
    return crypto.timingSafeEqual(bufA, bufB);
}
/**
 * Fulfills the subscription activation or wallet deposit atomically inside a single Firestore transaction.
 * Strictly idempotent: safe to call from both verifyRazorpayPayment callable and razorpayWebhook concurrently.
 * All amounts and plan information are read strictly from the server-validated order record to prevent parameter tampering.
 */
async function fulfillPayment(uid, orderId, paymentId) {
    const orderRef = db.collection("razorpay_orders").doc(orderId);
    const now = Date.now();
    const result = await db.runTransaction(async (tx) => {
        const orderSnap = await tx.get(orderRef);
        if (!orderSnap.exists) {
            throw new Error("Order not found: " + orderId);
        }
        // Edge Case: Order already processed by previous call or webhook
        if (orderSnap.get("status") === "PAID") {
            functions.logger.info("Order already fulfilled", { orderId });
            return { ok: true, status: "ALREADY_PROCESSED", orderId, paymentId };
        }
        // Security Check: Enforce matching UID so users cannot claim someone else's order
        const recordedUid = String(orderSnap.get("uid") || "");
        if (recordedUid && recordedUid !== uid) {
            throw new Error("Order ownership mismatch");
        }
        // Authoritative type, planId and amount read strictly from the order record in Firestore
        const type = String(orderSnap.get("type") || "SUBSCRIPTION");
        const planId = String(orderSnap.get("planId") || "");
        const amountPaise = Number(orderSnap.get("amountPaise") || 0);
        if (amountPaise <= 0) {
            throw new Error("Invalid order amount");
        }
        // Mark PAID in this transaction immediately to block any concurrent fulfillment attempts
        tx.update(orderRef, {
            paymentId,
            status: "PAID",
            verifiedAt: Timestamp.now(),
        });
        if (type === "SUBSCRIPTION") {
            const plan = await (0, subscriptions_1.planById)(planId);
            if (!plan)
                throw new Error("Plan not found: " + planId);
            const S = schema_1.EmployerProfiles.Subscription;
            const empRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid);
            tx.set(empRef, {
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
                    [S.AI_PER_DAY]: plan.ai === true ? (plan.aiPerDay || 100) : 0,
                },
            }, { merge: true });
            const paymentRecordRef = db.collection(schema_1.SubscriptionPayments.COLLECTION).doc(paymentId);
            tx.set(paymentRecordRef, {
                [schema_1.SubscriptionPayments.EMPLOYER_ID]: uid,
                [schema_1.SubscriptionPayments.PLAN_ID]: plan.id,
                [schema_1.SubscriptionPayments.AMOUNT_PAISE]: plan.pricePaise,
                [schema_1.SubscriptionPayments.UPI_ID_USED]: "RAZORPAY_GATEWAY",
                [schema_1.SubscriptionPayments.UTR_NUMBER]: paymentId,
                [schema_1.SubscriptionPayments.STATUS]: "VERIFIED",
                [schema_1.SubscriptionPayments.CREATED_AT]: Timestamp.fromMillis(now),
                [schema_1.SubscriptionPayments.VERIFIED_AT]: Timestamp.fromMillis(now),
                paymentMethod: "RAZORPAY",
                orderId,
            });
            return { ok: true, status: "ACTIVE", planId: plan.id, orderId, paymentId };
        }
        else {
            // WALLET DEPOSIT
            const amountRupees = Math.round(amountPaise / 100);
            const eventId = `razorpay_${orderId}`;
            const ledgerCheck = await tx.get((0, referrals_1.ledgerRef)(eventId));
            if (!ledgerCheck.exists) {
                await (0, referrals_1.ensureWallet)(uid);
                const w = await (0, referrals_1.readWallet)(tx, uid);
                if (w.data[schema_1.Wallets.BLOCKED] === true) {
                    throw new Error("User wallet is blocked");
                }
                (0, referrals_1.postEntry)(tx, uid, w, eventId, schema_1.Values.LedgerType.WALLET_DEPOSIT, amountPaise, orderId);
            }
            const depRef = db.collection("wallet_deposits").doc(orderId);
            tx.set(depRef, {
                uid,
                amountRupees,
                amountPaise,
                utr: paymentId,
                method: "RAZORPAY",
                orderId,
                status: "COMPLETED",
                createdAt: Timestamp.now(),
                verifiedAt: Timestamp.now(),
            }, { merge: true });
            return { ok: true, status: "COMPLETED", amountRupees, orderId, paymentId };
        }
    });
    // Post-transaction notifications
    if (result.status === "ACTIVE") {
        await (0, notify_1.notify)(uid, {
            type: "PAYMENT",
            templateId: "SUBSCRIPTION_ACTIVATED",
            role: schema_1.Values.Role.EMPLOYER,
            params: { plan: result.planId || "" },
            data: { orderId, paymentId },
        }).catch(() => undefined);
    }
    else if (result.status === "COMPLETED") {
        await (0, notify_1.notify)(uid, {
            type: "PAYMENT",
            templateId: "WALLET_CREDITED",
            role: schema_1.Values.Role.EMPLOYER,
            params: { amount: String(result.amountRupees || 0) },
            data: { orderId, paymentId },
        }).catch(() => undefined);
    }
    return result;
}
/**
 * Creates an authorized Razorpay Order for a subscription plan or wallet recharge.
 */
exports.createRazorpayOrder = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    var _a;
    const uid = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!uid)
        (0, input_1.fail)("unauthenticated", "Authentication required");
    const data = (0, input_1.obj)(raw);
    const type = (0, input_1.str)(data, "type", { max: 30, optional: true }) || "SUBSCRIPTION"; // SUBSCRIPTION | WALLET
    const planId = (0, input_1.str)(data, "planId", { optional: true, max: 64 });
    const amountRupees = (0, input_1.int)(data, "amountRupees", { optional: true, min: 1, max: 100000 });
    let amountPaise = 0;
    let description = "DutyPe Service Payment";
    if (type === "SUBSCRIPTION") {
        if (!planId)
            (0, input_1.fail)("invalid-argument", "planId is required for subscription");
        const plan = await (0, subscriptions_1.planById)(planId);
        if (!plan)
            (0, input_1.fail)("not-found", "Plan not found: " + planId);
        amountPaise = plan.pricePaise || 0;
        description = `DutyPe Subscription: ${plan.id}`;
    }
    else {
        // Wallet recharge (Closed-loop PPI compliant: min ₹50, max ₹2,000)
        if (!amountRupees || amountRupees < 50 || amountRupees > 2000) {
            (0, input_1.fail)("invalid-argument", "Wallet deposit amount must be between ₹50 and ₹2,000");
        }
        amountPaise = amountRupees * 100;
        description = `DutyPe Wallet Deposit: ₹${amountRupees}`;
    }
    const receipt = `rcpt_${uid.slice(0, 8)}_${Date.now()}`;
    const authHeader = "Basic " + Buffer.from(`${exports.RAZORPAY_KEY_ID}:${exports.RAZORPAY_KEY_SECRET}`).toString("base64");
    try {
        const res = await fetch("https://api.razorpay.com/v1/orders", {
            method: "POST",
            headers: {
                "Authorization": authHeader,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                amount: amountPaise,
                currency: "INR",
                receipt,
                notes: {
                    uid,
                    type,
                    planId: planId || "",
                    amountPaise: String(amountPaise),
                },
            }),
        });
        if (!res.ok) {
            const errText = await res.text();
            functions.logger.error("Razorpay order creation failed", { status: res.status, body: errText });
            (0, input_1.fail)("internal", "Failed to create Razorpay order: " + errText);
        }
        const orderData = (await res.json());
        // Record order in Firestore for strict verification during fulfillment
        await db.collection("razorpay_orders").doc(orderData.id).set({
            orderId: orderData.id,
            uid,
            type,
            planId: planId || "",
            amountPaise,
            currency: "INR",
            receipt,
            status: "CREATED",
            createdAt: Timestamp.now(),
        });
        return {
            orderId: orderData.id,
            amountPaise: orderData.amount,
            currency: orderData.currency,
            keyId: exports.RAZORPAY_KEY_ID,
            name: "DutyPe",
            description,
        };
    }
    catch (err) {
        functions.logger.error("Error in createRazorpayOrder", err);
        (0, input_1.fail)("internal", err instanceof Error ? err.message : "Razorpay communication error");
    }
});
/**
 * Verifies Razorpay payment signature and activates the subscription or credits wallet immediately.
 * Validates HMAC-SHA256 signature and uses server-recorded order data for fulfillment.
 */
exports.verifyRazorpayPayment = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (raw, context) => {
    var _a;
    const uid = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!uid)
        (0, input_1.fail)("unauthenticated", "Authentication required");
    const data = (0, input_1.obj)(raw);
    const orderId = (0, input_1.str)(data, "orderId", { max: 100 });
    const paymentId = (0, input_1.str)(data, "paymentId", { max: 100 });
    const signature = (0, input_1.str)(data, "signature", { max: 150 });
    // 1. Validate signature using HMAC-SHA256 with timing-safe comparison
    const expectedSignature = crypto
        .createHmac("sha256", exports.RAZORPAY_KEY_SECRET)
        .update(`${orderId}|${paymentId}`)
        .digest("hex");
    if (!timingSafeEqual(expectedSignature, signature)) {
        functions.logger.warn("Razorpay signature mismatch", { orderId, paymentId });
        (0, input_1.fail)("permission-denied", "Payment verification failed: invalid signature");
    }
    // 2. Fulfill atomically inside Firestore transaction (order metadata is read from Firestore)
    return await fulfillPayment(uid, orderId, paymentId);
});
/**
 * Razorpay Webhook for server-to-server asynchronous payment capture confirmation.
 * Registered URL: https://asia-south1-dutype-860ac.cloudfunctions.net/razorpayWebhook
 */
exports.razorpayWebhook = functions
    .region("asia-south1")
    .https.onRequest(async (req, res) => {
    var _a, _b, _c;
    if (req.method !== "POST") {
        res.status(405).send("Method Not Allowed");
        return;
    }
    const signature = req.headers["x-razorpay-signature"];
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || exports.RAZORPAY_KEY_SECRET;
    // MANDATORY signature check: Reject immediately if signature header is missing
    if (!signature) {
        functions.logger.warn("Razorpay webhook rejected: missing signature header");
        res.status(400).send("Missing signature header");
        return;
    }
    const rawBody = req.rawBody
        ? req.rawBody.toString()
        : JSON.stringify(req.body);
    const expected = crypto.createHmac("sha256", webhookSecret).update(rawBody).digest("hex");
    if (!timingSafeEqual(expected, signature)) {
        functions.logger.warn("Razorpay webhook signature mismatch");
        res.status(400).send("Invalid signature");
        return;
    }
    const event = (_a = req.body) === null || _a === void 0 ? void 0 : _a.event;
    functions.logger.info("Razorpay webhook verified and received", { event });
    try {
        if (event === "order.paid" || event === "payment.captured") {
            const payload = req.body.payload;
            const orderEntity = (_b = payload === null || payload === void 0 ? void 0 : payload.order) === null || _b === void 0 ? void 0 : _b.entity;
            const paymentEntity = (_c = payload === null || payload === void 0 ? void 0 : payload.payment) === null || _c === void 0 ? void 0 : _c.entity;
            const orderId = (orderEntity === null || orderEntity === void 0 ? void 0 : orderEntity.id) || (paymentEntity === null || paymentEntity === void 0 ? void 0 : paymentEntity.order_id);
            const paymentId = paymentEntity === null || paymentEntity === void 0 ? void 0 : paymentEntity.id;
            const notes = (orderEntity === null || orderEntity === void 0 ? void 0 : orderEntity.notes) || (paymentEntity === null || paymentEntity === void 0 ? void 0 : paymentEntity.notes) || {};
            const uid = notes.uid;
            if (orderId && uid) {
                await fulfillPayment(uid, orderId, paymentId || `pay_${Date.now()}`);
                functions.logger.info("Razorpay webhook fulfilled successfully", { orderId, uid });
            }
        }
        res.status(200).json({ status: "ok" });
    }
    catch (err) {
        functions.logger.error("Razorpay webhook processing error", err);
        res.status(500).send("Webhook processing error");
    }
});
//# sourceMappingURL=razorpay.js.map