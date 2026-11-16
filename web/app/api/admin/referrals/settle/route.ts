import { NextRequest, NextResponse } from "next/server";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import adminApp from "firebase-admin";

import { getAdminActor, logAdminAudit, requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { invalidateAdminResponseCache } from "@/lib/firebase/admin-response-cache";
import { Values, WalletLedger, Wallets, Withdrawals } from "@/lib/firebase/schema";
import { RateLimitTiers } from "@/lib/security/admin-rate-limiter";

export const runtime = "nodejs";

/**
 * Dedicated server-side admin endpoint to settle withdrawals immediately.
 * POST /api/admin/referrals/settle
 * Body: { withdrawalId: string, status: "COMPLETED" | "PROCESSING" | "FAILED" | "ADJUST", txnRef?: string, failureReason?: string, newAmount?: number, adjustReason?: string }
 */
export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, RateLimitTiers.FINANCIAL);
  if (unauthorized) return unauthorized;

  const actor = await getAdminActor(request);

  let body: { withdrawalId?: string; status?: string; txnRef?: string; failureReason?: string; newAmount?: number; adjustReason?: string };
  try {
    body = (await request.json()) as { withdrawalId?: string; status?: string; txnRef?: string; failureReason?: string; newAmount?: number; adjustReason?: string };
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const withdrawalId = body.withdrawalId?.trim();
  const nextStatus = (body.status || "").toUpperCase();
  const txnRef = body.txnRef?.trim() || "";
  const failureReason = body.failureReason?.trim() || body.adjustReason?.trim() || "";
  const newAmountRupees = body.newAmount !== undefined ? Number(body.newAmount) : undefined;

  if (!withdrawalId) {
    return NextResponse.json({ error: "withdrawalId is required" }, { status: 400 });
  }

  if (nextStatus !== "COMPLETED" && nextStatus !== "PROCESSING" && nextStatus !== "FAILED" && nextStatus !== "ADJUST") {
    return NextResponse.json({ error: "Invalid status. Must be COMPLETED, PROCESSING, FAILED, or ADJUST" }, { status: 400 });
  }

  if (nextStatus === "COMPLETED" && !txnRef) {
    return NextResponse.json({ error: "UPI transaction reference is required for completed payouts" }, { status: 400 });
  }

  if (nextStatus === "FAILED" && !failureReason) {
    return NextResponse.json({ error: "Failure reason is required when marking a withdrawal as failed" }, { status: 400 });
  }

  if (nextStatus === "ADJUST" && (newAmountRupees === undefined || Number.isNaN(newAmountRupees) || newAmountRupees <= 0 || !failureReason)) {
    return NextResponse.json({ error: "Valid newAmount (in ₹) and adjustment explanation note are required" }, { status: 400 });
  }

  const db = getFirebaseAdminDb();
  const withdrawalRef = db.collection(Withdrawals.COLLECTION).doc(withdrawalId);

  try {
    const outcome = await db.runTransaction(async (tx) => {
      // 1. Initial read: check withdrawal document
      const wSnap = await tx.get(withdrawalRef);
      if (!wSnap.exists) {
        throw new Error("Withdrawal request not found");
      }

      const currentStatus = String(wSnap.get(Withdrawals.STATUS) || "").toUpperCase();
      if (currentStatus === "COMPLETED" || currentStatus === "FAILED") {
        // Idempotent return if already settled
        return {
          uid: String(wSnap.get(Withdrawals.UID) || ""),
          amountPaise: Number(wSnap.get(Withdrawals.AMOUNT_PAISE) || 0),
          alreadySettled: true,
          status: currentStatus,
        };
      }

      const uid = String(wSnap.get(Withdrawals.UID) || "");
      const amountPaise = Number(wSnap.get(Withdrawals.AMOUNT_PAISE) || 0);

      // --- STRICT FIRESTORE INVARIANT: ALL READS MUST OCCUR BEFORE ALL WRITES ---

      // CASE A: ADJUST AMOUNT
      if (nextStatus === "ADJUST") {
        const newPaise = Math.round((newAmountRupees ?? 0) * 100);
        if (newPaise <= 0 || newPaise > amountPaise) {
          throw new Error("Adjusted amount must be greater than 0 and less than or equal to current amount.");
        }
        const diffPaise = amountPaise - newPaise;

        const withLedgerRef = db.collection(WalletLedger.COLLECTION).doc(`withdrawal_${withdrawalId}`);
        // READ BEFORE ANY WRITES
        const withLedgerSnap = await tx.get(withLedgerRef);

        // NOW EXECUTE WRITES
        tx.update(withdrawalRef, {
          [Withdrawals.AMOUNT_PAISE]: newPaise,
          [Withdrawals.FAILURE_REASON]: failureReason,
          adjustedAt: Timestamp.now(),
        });

        if (withLedgerSnap.exists) {
          tx.update(withLedgerRef, {
            [WalletLedger.AMOUNT_PAISE]: -newPaise,
          });
        }

        const walletRef = db.collection(Wallets.COLLECTION).doc(uid);
        tx.set(
          walletRef,
          {
            [Wallets.WITHDRAWN_PAISE]: FieldValue.increment(-diffPaise),
            [Wallets.LIFETIME_EARNED_PAISE]: FieldValue.increment(-diffPaise),
            [Wallets.UPDATED_AT]: Timestamp.now(),
          },
          { merge: true }
        );

        return { uid, amountPaise: newPaise, alreadySettled: false, status: "PENDING", note: failureReason };
      }

      // CASE B: FAILED WITHDRAWAL (REFUND BACK TO WALLET)
      if (nextStatus === "FAILED") {
        const refundEventId = `refund_${withdrawalId}`;
        const walletRef = db.collection(Wallets.COLLECTION).doc(uid);
        const ledgerRef = db.collection(WalletLedger.COLLECTION).doc(refundEventId);

        // READ ALL DOCUMENTS BEFORE ANY WRITES
        const [walletSnap, ledgerSnap] = await Promise.all([
          tx.get(walletRef),
          tx.get(ledgerRef),
        ]);

        // NOW EXECUTE WRITES
        const updateData: Record<string, unknown> = {
          [Withdrawals.STATUS]: nextStatus,
          ...(txnRef ? { [Withdrawals.TXN_REF]: txnRef } : {}),
          ...(failureReason ? { [Withdrawals.FAILURE_REASON]: failureReason } : {}),
          [Withdrawals.PROCESSED_AT]: Timestamp.now(),
        };

        tx.update(withdrawalRef, updateData);

        if (!ledgerSnap.exists) {
          const currentBalance = Number(walletSnap.get(Wallets.BALANCE_PAISE) || 0);
          const newBalance = currentBalance + amountPaise;

          tx.set(
            walletRef,
            {
              [Wallets.BALANCE_PAISE]: newBalance,
              [Wallets.WITHDRAWN_PAISE]: FieldValue.increment(-amountPaise),
              [Wallets.UPDATED_AT]: Timestamp.now(),
            },
            { merge: true }
          );

          tx.create(ledgerRef, {
            [WalletLedger.UID]: uid,
            [WalletLedger.TYPE]: Values.LedgerType.REFUND,
            [WalletLedger.AMOUNT_PAISE]: amountPaise,
            [WalletLedger.REF_ID]: withdrawalId,
            [WalletLedger.BALANCE_AFTER_PAISE]: newBalance,
            [WalletLedger.CREATED_AT]: Timestamp.now(),
          });
        }

        return { uid, amountPaise, alreadySettled: false, status: nextStatus, note: failureReason };
      }

      // CASE C: COMPLETED OR PROCESSING (NO EXTRA READS REQUIRED)
      const updateData: Record<string, unknown> = {
        [Withdrawals.STATUS]: nextStatus,
        ...(txnRef ? { [Withdrawals.TXN_REF]: txnRef } : {}),
        ...(failureReason ? { [Withdrawals.FAILURE_REASON]: failureReason } : {}),
        ...(nextStatus !== "PROCESSING" ? { [Withdrawals.PROCESSED_AT]: Timestamp.now() } : {}),
      };

      tx.update(withdrawalRef, updateData);

      return { uid, amountPaise, alreadySettled: false, status: nextStatus, note: failureReason };
    });

    // Wipe cached responses so GET /api/admin/referrals immediately returns fresh data
    invalidateAdminResponseCache();

    // Log admin audit
    if (actor) {
      await logAdminAudit("WITHDRAWAL_SETTLE", actor, { withdrawalId, status: nextStatus, txnRef, failureReason }, request);
    }

    // Opportunistically notify the user in the background
    if (!outcome.alreadySettled && outcome.uid && nextStatus !== "PROCESSING") {
      const notifCol = db.collection("notifications");
      const title =
        nextStatus === "COMPLETED"
          ? `₹${outcome.amountPaise / 100} Sent to Your UPI!`
          : nextStatus === "ADJUST"
          ? `Withdrawal Request Adjusted to ₹${outcome.amountPaise / 100}`
          : "Withdrawal Failed";
      const message =
        nextStatus === "COMPLETED"
          ? `Your referral earnings withdrawal of ₹${outcome.amountPaise / 100} has been paid via UPI. Ref: ${txnRef}`
          : nextStatus === "ADJUST"
          ? `Your withdrawal request was adjusted to ₹${outcome.amountPaise / 100}. Reason: ${failureReason}`
          : `Your withdrawal of ₹${outcome.amountPaise / 100} could not be processed (${failureReason}). Please check your UPI ID and request again.`;

      await notifCol.add({
        userId: outcome.uid,
        recipientId: outcome.uid,
        title,
        body: message,
        message,
        type: "PAYMENT",
        data: { withdrawalId, status: nextStatus, txnRef, note: failureReason },
        read: false,
        createdAt: Timestamp.now(),
      }).catch(() => {});

      // Send push notification if token available
      try {
        const tokenDoc = await db.collection("user_tokens").doc(outcome.uid).get();
        const token = tokenDoc.get("fcmToken");
        if (token && typeof token === "string") {
          await adminApp.messaging().send({
            token,
            notification: {
              title,
              body: message,
            },
            data: {
              type: "PAYMENT",
              withdrawalId,
              status: nextStatus,
            },
          });
        }
      } catch {
        // Push notification is best-effort
      }
    }

    return NextResponse.json({
      success: true,
      withdrawalId,
      status: nextStatus,
      txnRef: txnRef || undefined,
      processedAt: new Date().toISOString(),
    });
  } catch (error) {
    const msg = error instanceof Error ? error.message : "Failed to settle withdrawal";
    return NextResponse.json({ error: msg }, { status: 500 });
  }
}
