import { NextRequest, NextResponse } from "next/server";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import adminApp from "firebase-admin";

import { getAdminActor, logAdminAudit, requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { findUserIdsForPhone } from "@/lib/firebase/admin-account-deletion";
import { Values, WalletLedger, Wallets } from "@/lib/firebase/schema";
import { RateLimitTiers } from "@/lib/security/admin-rate-limiter";

export const runtime = "nodejs";

const MAX_SINGLE_ADMIN_CREDIT_RUPEES = 1000;

/**
 * Admin manual credit endpoint:
 * POST /api/admin/referrals/credit
 * Body: { uidOrPhone: string, amountRupees: number, reason: string }
 */
export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, RateLimitTiers.FINANCIAL);
  if (unauthorized) return unauthorized;

  const actor = await getAdminActor(request);

  let body: { uidOrPhone?: string; amountRupees?: number; reason?: string };
  try {
    body = (await request.json()) as { uidOrPhone?: string; amountRupees?: number; reason?: string };
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const query = body.uidOrPhone?.trim();
  const amountRupees = Number(body.amountRupees);
  const reason = body.reason?.trim() || "Admin Reward / Adjustment";

  if (!query) {
    return NextResponse.json({ error: "User ID or phone number is required" }, { status: 400 });
  }

  if (isNaN(amountRupees) || amountRupees <= 0) {
    return NextResponse.json({ error: "Enter a valid positive amount in Rupees" }, { status: 400 });
  }

  if (amountRupees > MAX_SINGLE_ADMIN_CREDIT_RUPEES) {
    return NextResponse.json(
      { error: `Single manual credit cannot exceed ₹${MAX_SINGLE_ADMIN_CREDIT_RUPEES}. For higher disbursements, please use batch payout.` },
      { status: 400 }
    );
  }

  const amountPaise = Math.round(amountRupees * 100);
  const db = getFirebaseAdminDb();

  // Resolve UID if query is a phone number or referral code
  let uid = query;
  if (/^(\+91|\d{10})/.test(query)) {
    const { userIds } = await findUserIdsForPhone(query);
    if (userIds.length === 0) {
      return NextResponse.json({ error: `No user found for phone: ${query}` }, { status: 404 });
    }
    uid = userIds[0];
  } else if (query.length <= 10 && !query.includes("-") && query.toUpperCase() === query) {
    const codeSnap = await db.collection("referral_codes").doc(query).get();
    if (codeSnap.exists) {
      uid = String(codeSnap.get("uid") || query);
    }
  }

  try {
    const walletRef = db.collection(Wallets.COLLECTION).doc(uid);
    const eventId = `admin_credit_${uid}_${Date.now()}`;
    const ledgerRef = db.collection(WalletLedger.COLLECTION).doc(eventId);

    const result = await db.runTransaction(async (tx) => {
      const walletSnap = await tx.get(walletRef);
      const currentBalance = Number(walletSnap.get(Wallets.BALANCE_PAISE) || 0);
      const newBalance = currentBalance + amountPaise;

      tx.set(
        walletRef,
        {
          [Wallets.BALANCE_PAISE]: newBalance,
          [Wallets.LIFETIME_EARNED_PAISE]: FieldValue.increment(amountPaise),
          [Wallets.UPDATED_AT]: Timestamp.now()
        },
        { merge: true }
      );

      tx.create(ledgerRef, {
        [WalletLedger.UID]: uid,
        [WalletLedger.TYPE]: Values.LedgerType.ADJUSTMENT,
        [WalletLedger.AMOUNT_PAISE]: amountPaise,
        [WalletLedger.REF_ID]: reason,
        [WalletLedger.BALANCE_AFTER_PAISE]: newBalance,
        [WalletLedger.CREATED_AT]: Timestamp.now()
      });

      return { newBalancePaise: newBalance };
    });

    // Push notification to user
    try {
      const tokenDoc = await db.collection("user_tokens").doc(uid).get();
      const token = tokenDoc.get("fcmToken");
      if (token && typeof token === "string") {
        await adminApp.messaging().send({
          token,
          notification: {
            title: "🎁 Money Credited by Admin!",
            body: `₹${amountRupees} has been added to your DutyPe wallet. Note: ${reason}`
          },
          data: {
            type: "WALLET",
            amount: String(amountRupees)
          }
        });
      }
    } catch {
      // Notification is best-effort
    }

    if (actor) {
      await logAdminAudit(
        "ADMIN_MANUAL_CREDIT",
        actor,
        { uid, query, amountRupees, amountPaise, reason, newBalancePaise: result.newBalancePaise },
        request
      );
    }

    return NextResponse.json({
      ok: true,
      uid,
      amountRupees,
      amountPaise,
      newBalancePaise: result.newBalancePaise,
      message: `Successfully credited ₹${amountRupees} to user ${uid}`
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : "Failed to credit wallet";
    return NextResponse.json({ error: msg }, { status: 500 });
  }
}
