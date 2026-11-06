import { NextRequest, NextResponse } from "next/server";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { getAdminActor, logAdminAudit, requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { EmployerProfiles, UserTokens, Wallets, WalletLedger } from "@/lib/firebase/schema";
import { randomInt } from "crypto";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request, { sensitive: true });
  if (unauthorized) return unauthorized;

  const actor = await getAdminActor(request);
  let body: Record<string, unknown>;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const action = String(body.action || "").trim();
  const userId = String(body.userId || "").trim();
  const reason = String(body.reason || "Admin support goodwill").trim();

  if (!userId) {
    return NextResponse.json({ error: "Missing userId" }, { status: 400 });
  }

  const db = getFirebaseAdminDb();

  try {
    switch (action) {
      // 1. Worker Bonus / Credit (Added directly to referral_stats wallet)
      case "CREDIT_WALLET": {
        const amountRupees = Number(body.amountRupees);
        if (!Number.isFinite(amountRupees) || amountRupees <= 0) {
          return NextResponse.json({ error: "Enter a valid positive amount in Rupees" }, { status: 400 });
        }
        const amountPaise = Math.round(amountRupees * 100);
        const eventId = `admin_bonus_${userId}_${Date.now()}_${randomInt(1000, 9999)}`;

        let newBalanceRupees = 0;

        await db.runTransaction(async (tx) => {
          const wRef = db.collection(Wallets.COLLECTION).doc(userId);
          const wSnap = await tx.get(wRef);
          const wData = wSnap.data() || {};
          const currentBalancePaise = Number(wData[Wallets.BALANCE_PAISE] || 0);
          const currentLifetimePaise = Number(wData[Wallets.LIFETIME_EARNED_PAISE] || 0);
          const afterPaise = currentBalancePaise + amountPaise;
          newBalanceRupees = afterPaise / 100;

          const lRef = db.collection(WalletLedger.COLLECTION).doc(eventId);
          tx.create(lRef, {
            [WalletLedger.UID]: userId,
            [WalletLedger.TYPE]: "ADMIN_BONUS",
            [WalletLedger.AMOUNT_PAISE]: amountPaise,
            [WalletLedger.REF_ID]: reason,
            [WalletLedger.BALANCE_AFTER_PAISE]: afterPaise,
            [WalletLedger.CREATED_AT]: Timestamp.now()
          });

          tx.set(
            wRef,
            {
              [Wallets.BALANCE_PAISE]: afterPaise,
              [Wallets.LIFETIME_EARNED_PAISE]: currentLifetimePaise + amountPaise,
              [Wallets.UPDATED_AT]: Timestamp.now()
            },
            { merge: true }
          );
        });

        // Notify user via in-app notification document
        await db.collection("notifications").add({
          userId,
          title: "🎁 Bonus Credited!",
          body: `₹${amountRupees} has been credited to your DutyPe wallet: ${reason}`,
          type: "WALLET_BONUS",
          createdAt: Timestamp.now(),
          read: false
        }).catch(() => undefined);

        if (actor) {
          void logAdminAudit("ADMIN_CREDIT_WALLET", actor, { targetUserId: userId, amountRupees, reason }, request);
        }

        return NextResponse.json({
          ok: true,
          message: `Successfully credited ₹${amountRupees} to user wallet.`,
          balanceRupees: newBalanceRupees
        });
      }

      // 2. Employer Free Job Posts
      case "GRANT_JOB_POSTS": {
        const count = Math.max(1, Number(body.count || 1));
        const empRef = db.collection(EmployerProfiles.COLLECTION).doc(userId);
        const empSnap = await empRef.get();
        if (!empSnap.exists) {
          return NextResponse.json({ error: "Employer profile not found." }, { status: 404 });
        }

        await empRef.update({
          grantedFreeJobPosts: FieldValue.increment(count),
          "subscription.credits.normal": FieldValue.increment(count),
          updatedAt: Timestamp.now()
        });

        await db.collection("notifications").add({
          userId,
          title: "🎉 Free Job Posts Granted!",
          body: `You received ${count} free job posting credits from DutyPe Support: ${reason}`,
          type: "PERK_JOB_POSTS",
          createdAt: Timestamp.now(),
          read: false
        }).catch(() => undefined);

        if (actor) {
          void logAdminAudit("ADMIN_GRANT_JOB_POSTS", actor, { targetUserId: userId, count, reason }, request);
        }

        return NextResponse.json({
          ok: true,
          message: `Successfully granted ${count} free job posts to employer.`
        });
      }

      // 3. Employer Contact Unlocks
      case "GRANT_CONTACT_UNLOCKS": {
        const workerId = String(body.workerId || "").trim();
        const count = Math.max(1, Number(body.count || 1));
        const empRef = db.collection(EmployerProfiles.COLLECTION).doc(userId);

        if (workerId) {
          // Unlock a specific worker's contact directly
          await empRef.collection(EmployerProfiles.Unlocks.COLLECTION).doc(workerId).set({
            workerId,
            grantedByAdmin: true,
            reason,
            createdAt: Timestamp.now()
          }, { merge: true });
        } else {
          // Or add generic contact unlock quota
          await empRef.update({
            freeContactUnlocks: FieldValue.increment(count),
            "subscription.credits.unlocks": FieldValue.increment(count),
            updatedAt: Timestamp.now()
          });
        }

        await db.collection("notifications").add({
          userId,
          title: "🔓 Worker Contact Unlocked!",
          body: workerId
            ? "Worker phone number unlocked directly by DutyPe Support."
            : `${count} contact unlocks added to your employer account.`,
          type: "PERK_CONTACT_UNLOCK",
          createdAt: Timestamp.now(),
          read: false
        }).catch(() => undefined);

        if (actor) {
          void logAdminAudit("ADMIN_GRANT_CONTACT_UNLOCK", actor, { targetUserId: userId, workerId, count, reason }, request);
        }

        return NextResponse.json({
          ok: true,
          message: workerId
            ? `Successfully unlocked worker contact for employer.`
            : `Successfully granted ${count} contact unlocks to employer.`
        });
      }

      // 4. Customer Free Platform Booking Waiver
      case "GRANT_FREE_PLATFORM_BOOKING": {
        const enabled = body.enabled !== false;
        const userRef = db.collection("users").doc(userId);
        await userRef.set(
          {
            freePlatformBooking: enabled,
            vipFreeBooking: enabled,
            freePlatformBookingReason: reason,
            freePlatformBookingUpdatedAt: Timestamp.now()
          },
          { merge: true }
        );

        await db.collection("notifications").add({
          userId,
          title: enabled ? "✨ Free Platform Booking Activated!" : "Platform Booking Fee Restored",
          body: enabled
            ? `You have 100% Free Platform Fee on all DutyPe Home Services: ${reason}`
            : "Standard platform fee restored.",
          type: "FREE_PLATFORM_FEE",
          createdAt: Timestamp.now(),
          read: false
        }).catch(() => undefined);

        if (actor) {
          void logAdminAudit("ADMIN_GRANT_FREE_PLATFORM_FEE", actor, { targetUserId: userId, enabled, reason }, request);
        }

        return NextResponse.json({
          ok: true,
          message: enabled
            ? "Customer now has 100% Free Platform Fee on all service bookings."
            : "Platform booking fee restored for customer."
        });
      }

      // 5. Dedicated Customer / Exclusive Coupon
      case "CREATE_CUSTOMER_COUPON": {
        const code = String(body.code || "").trim().toUpperCase();
        const discountRupees = Math.max(1, Number(body.discountRupees || 50));
        const title = String(body.title || `Special Offer: ₹${discountRupees} OFF`).trim();
        const targetPhone = String(body.targetPhone || "").trim();

        if (!code || code.length < 3) {
          return NextResponse.json({ error: "Coupon code must be at least 3 characters." }, { status: 400 });
        }

        const configRef = db.collection("app_config").doc("services");
        const snap = await configRef.get();
        const existing = snap.data() || {};
        const coupons: Array<Record<string, unknown>> = Array.isArray(existing.coupons) ? [...existing.coupons] : [];

        // Remove if duplicate code
        const filtered = coupons.filter((c) => String(c.code).toUpperCase() !== code);

        const newCoupon: Record<string, unknown> = {
          code,
          title,
          description: reason || `Exclusive customer reward of ₹${discountRupees}`,
          type: "FLAT",
          value: discountRupees,
          minPrice: Math.max(49, discountRupees),
          active: true,
          firstBookingOnly: false,
          ...(targetPhone ? { targetPhone } : {}),
          ...(userId ? { targetUid: userId } : {}),
          createdAt: Timestamp.now(),
          expiresAt: Timestamp.fromMillis(Date.now() + 60 * 24 * 60 * 60 * 1000) // 60 days
        };

        filtered.unshift(newCoupon);

        await configRef.set(
          {
            coupons: filtered,
            updatedAt: Timestamp.now()
          },
          { merge: true }
        );

        if (actor) {
          void logAdminAudit("ADMIN_CREATE_COUPON", actor, { code, discountRupees, targetPhone, userId }, request);
        }

        return NextResponse.json({
          ok: true,
          message: `Dedicated coupon ${code} (₹${discountRupees} off) created successfully.`
        });
      }

      default:
        return NextResponse.json({ error: `Unknown action: ${action}` }, { status: 400 });
    }
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to execute support grant action." },
      { status: 500 }
    );
  }
}
