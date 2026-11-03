import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, int } from "./lib/input";
import { planById, DEFAULT_PLANS } from "./subscriptions";
import { EmployerProfiles, SubscriptionPayments, Values, Wallets } from "./schema";
import { notify } from "./lib/notify";
import { ensureWallet, ledgerRef, postEntry, readWallet } from "./referrals";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;

export const RAZORPAY_KEY_ID = process.env.RAZORPAY_KEY_ID || "rzp_live_TvEYRD4s7DgLuj";
export const RAZORPAY_KEY_SECRET = process.env.RAZORPAY_KEY_SECRET || "rN8NyKBPiL5rifYV4TXr2VXu";
export const RAZORPAY_WEBHOOK_SECRET = process.env.RAZORPAY_WEBHOOK_SECRET || "rN8NyKBPiL5rifYV4TXr2VXu";

/**
 * Timing-safe string comparison to prevent timing analysis attacks on signatures.
 */
function timingSafeEqual(a: string, b: string): boolean {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const bufA = Buffer.from(a, "utf8");
  const bufB = Buffer.from(b, "utf8");
  if (bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * Fulfills the subscription activation or wallet deposit atomically inside a single Firestore transaction.
 * Strictly idempotent: safe to call from both verifyRazorpayPayment callable and razorpayWebhook concurrently.
 * All amounts and plan information are read strictly from the server-validated order record to prevent parameter tampering.
 */
async function fulfillPayment(
  uid: string,
  orderId: string,
  paymentId: string
) {
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
      const plan = await planById(planId);
      if (!plan) throw new Error("Plan not found: " + planId);

      const S = EmployerProfiles.Subscription;
      const empRef = db.collection(EmployerProfiles.COLLECTION).doc(uid);
      tx.set(empRef, {
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
          [S.AI_PER_DAY]: plan.ai === true ? (plan.aiPerDay || 100) : 0,
        },
      }, { merge: true });

      const paymentRecordRef = db.collection(SubscriptionPayments.COLLECTION).doc(paymentId);
      tx.set(paymentRecordRef, {
        [SubscriptionPayments.EMPLOYER_ID]: uid,
        [SubscriptionPayments.PLAN_ID]: plan.id,
        [SubscriptionPayments.AMOUNT_PAISE]: plan.pricePaise,
        [SubscriptionPayments.UPI_ID_USED]: "RAZORPAY_GATEWAY",
        [SubscriptionPayments.UTR_NUMBER]: paymentId,
        [SubscriptionPayments.STATUS]: "VERIFIED",
        [SubscriptionPayments.CREATED_AT]: Timestamp.fromMillis(now),
        [SubscriptionPayments.VERIFIED_AT]: Timestamp.fromMillis(now),
        paymentMethod: "RAZORPAY",
        orderId,
      });

      return { ok: true, status: "ACTIVE", planId: plan.id, orderId, paymentId };
    } else {
      // WALLET DEPOSIT
      const amountRupees = Math.round(amountPaise / 100);
      const eventId = `razorpay_${orderId}`;

      const ledgerCheck = await tx.get(ledgerRef(eventId));
      if (!ledgerCheck.exists) {
        await ensureWallet(uid);
        const w = await readWallet(tx, uid);
        if (w.data[Wallets.BLOCKED] === true) {
          throw new Error("User wallet is blocked");
        }
        postEntry(tx, uid, w, eventId, Values.LedgerType.WALLET_DEPOSIT, amountPaise, orderId);
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
    await notify(uid, {
      type: "PAYMENT",
      templateId: "SUBSCRIPTION_ACTIVATED",
      role: Values.Role.EMPLOYER,
      params: { plan: (result as { planId?: string }).planId || "" },
      data: { orderId, paymentId },
    }).catch(() => undefined);
  } else if (result.status === "COMPLETED") {
    await notify(uid, {
      type: "PAYMENT",
      templateId: "WALLET_CREDITED",
      role: Values.Role.EMPLOYER,
      params: { amount: String((result as { amountRupees?: number }).amountRupees || 0) },
      data: { orderId, paymentId },
    }).catch(() => undefined);
  }

  return result;
}

/**
 * Creates an authorized Razorpay Order for a subscription plan or wallet recharge.
 */
export const createRazorpayOrder = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth?.uid;
  if (!uid) fail("unauthenticated", "Authentication required");

  const data = obj(raw);
  const type = str(data, "type", { max: 30, optional: true }) || "SUBSCRIPTION"; // SUBSCRIPTION | WALLET
  const planId = str(data, "planId", { optional: true, max: 64 });
  const amountRupees = int(data, "amountRupees", { optional: true, min: 1, max: 100000 });

  let amountPaise = 0;
  let description = "DutyPe Service Payment";

  if (type === "SUBSCRIPTION") {
    if (!planId) fail("invalid-argument", "planId is required for subscription");
    const plan = await planById(planId);
    if (!plan) fail("not-found", "Plan not found: " + planId);
    amountPaise = plan.pricePaise || 0;
    description = `DutyPe Subscription: ${plan.id}`;
  } else {
    // Wallet recharge (Closed-loop PPI compliant: min ₹50, max ₹2,000)
    if (!amountRupees || amountRupees < 50 || amountRupees > 2000) {
      fail("invalid-argument", "Wallet deposit amount must be between ₹50 and ₹2,000");
    }
    amountPaise = amountRupees * 100;
    description = `DutyPe Wallet Deposit: ₹${amountRupees}`;
  }

  const receipt = `rcpt_${uid.slice(0, 8)}_${Date.now()}`;
  const authHeader = "Basic " + Buffer.from(`${RAZORPAY_KEY_ID}:${RAZORPAY_KEY_SECRET}`).toString("base64");

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
      fail("internal", "Failed to create Razorpay order: " + errText);
    }

    const orderData = (await res.json()) as { id: string; amount: number; currency: string };

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
      keyId: RAZORPAY_KEY_ID,
      name: "DutyPe",
      description,
    };
  } catch (err: unknown) {
    functions.logger.error("Error in createRazorpayOrder", err);
    fail("internal", err instanceof Error ? err.message : "Razorpay communication error");
  }
});

/**
 * Verifies Razorpay payment signature and activates the subscription or credits wallet immediately.
 * Validates HMAC-SHA256 signature and uses server-recorded order data for fulfillment.
 */
export const verifyRazorpayPayment = onCallSecured({ enforceAppCheck: false }, async (raw: unknown, context) => {
  const uid = context.auth?.uid;
  if (!uid) fail("unauthenticated", "Authentication required");

  const data = obj(raw);
  const orderId = str(data, "orderId", { max: 100 });
  const paymentId = str(data, "paymentId", { max: 100 });
  const signature = str(data, "signature", { max: 150 });

  // 1. Validate signature using HMAC-SHA256 with timing-safe comparison
  const expectedSignature = crypto
    .createHmac("sha256", RAZORPAY_KEY_SECRET)
    .update(`${orderId}|${paymentId}`)
    .digest("hex");

  if (!timingSafeEqual(expectedSignature, signature)) {
    functions.logger.warn("Razorpay signature mismatch", { orderId, paymentId });
    fail("permission-denied", "Payment verification failed: invalid signature");
  }

  // 2. Fulfill atomically inside Firestore transaction (order metadata is read from Firestore)
  return await fulfillPayment(uid, orderId, paymentId);
});

/**
 * Razorpay Webhook for server-to-server asynchronous payment capture confirmation.
 * Registered URL: https://asia-south1-dutype-860ac.cloudfunctions.net/razorpayWebhook
 */
export const razorpayWebhook = functions
  .region("asia-south1")
  .https.onRequest(async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const signature = req.headers["x-razorpay-signature"] as string;
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || RAZORPAY_KEY_SECRET;

    // MANDATORY signature check: Reject immediately if signature header is missing
    if (!signature) {
      functions.logger.warn("Razorpay webhook rejected: missing signature header");
      res.status(400).send("Missing signature header");
      return;
    }

    const rawBody = (req as unknown as { rawBody?: Buffer }).rawBody
      ? (req as unknown as { rawBody: Buffer }).rawBody.toString()
      : JSON.stringify(req.body);

    const expected = crypto.createHmac("sha256", webhookSecret).update(rawBody).digest("hex");
    if (!timingSafeEqual(expected, signature)) {
      functions.logger.warn("Razorpay webhook signature mismatch");
      res.status(400).send("Invalid signature");
      return;
    }

    const event = req.body?.event;
    functions.logger.info("Razorpay webhook verified and received", { event });

    try {
      if (event === "order.paid" || event === "payment.captured") {
        const payload = req.body.payload;
        const orderEntity = payload?.order?.entity;
        const paymentEntity = payload?.payment?.entity;
        const orderId = orderEntity?.id || paymentEntity?.order_id;
        const paymentId = paymentEntity?.id;
        const notes = orderEntity?.notes || paymentEntity?.notes || {};
        const uid = notes.uid;

        if (orderId && uid) {
          await fulfillPayment(uid, orderId, paymentId || `pay_${Date.now()}`);
          functions.logger.info("Razorpay webhook fulfilled successfully", { orderId, uid });
        }
      }
      res.status(200).json({ status: "ok" });
    } catch (err: unknown) {
      functions.logger.error("Razorpay webhook processing error", err);
      res.status(500).send("Webhook processing error");
    }
  });
