import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { e164 } from "@/lib/firebase/admin-account-deletion";
import {
  EmployerProfiles, PhoneRoles, ReferralCodes, Referrals, WalletLedger, Wallets, Withdrawals, WorkerProfiles
} from "@/lib/firebase/schema";

export const runtime = "nodejs";

/**
 * Read-only referral / wallet views for the admin panel. Money moves only through Cloud Functions
 * (settleWithdrawal is called from the page with the admin's own sign-in).
 *
 *   ?view=withdrawals[&status=PENDING]   newest withdrawal requests (one page, ?after=)
 *   ?view=referrals                      newest referrals (one page, ?after=)
 *   ?lookup=CODE|PHONE|UID               one user's wallet, ledger, referrals and withdrawals
 */
const PAGE = 100;

function iso(value: unknown) {
  return value instanceof Timestamp ? value.toDate().toISOString() : null;
}

async function names(uids: string[]): Promise<Map<string, string>> {
  const db = getFirebaseAdminDb();
  const unique = Array.from(new Set(uids.filter(Boolean)));
  const out = new Map<string, string>();
  if (!unique.length) return out;
  const [workers, employers] = await Promise.all([
    db.getAll(...unique.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u)), { fieldMask: [WorkerProfiles.NAME, WorkerProfiles.PHONE] }),
    db.getAll(...unique.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u)), { fieldMask: [EmployerProfiles.OWNER_NAME, EmployerProfiles.BUSINESS_NAME, EmployerProfiles.PHONE] })
  ]);
  workers.forEach((d) => { if (d.exists) out.set(d.id, `${d.get(WorkerProfiles.NAME) ?? ""} · ${d.get(WorkerProfiles.PHONE) ?? ""} (worker)`); });
  employers.forEach((d) => {
    if (d.exists) out.set(d.id, `${d.get(EmployerProfiles.BUSINESS_NAME) || d.get(EmployerProfiles.OWNER_NAME) || ""} · ${d.get(EmployerProfiles.PHONE) ?? ""} (employer)`);
  });
  return out;
}

async function lookupUid(query: string): Promise<string | null> {
  const db = getFirebaseAdminDb();
  const phone = e164(query);
  if (phone) return String((await db.collection(PhoneRoles.COLLECTION).doc(phone).get()).get(PhoneRoles.UID) ?? "") || null;
  const code = await db.collection(ReferralCodes.COLLECTION).doc(query.toUpperCase()).get();
  if (code.exists) return String(code.get(ReferralCodes.UID) ?? "") || null;
  return query;
}

async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;

    const lookup = params.get("lookup")?.trim();
    if (lookup) {
      const uid = await lookupUid(lookup);
      if (!uid) return NextResponse.json({ error: "No account found." }, { status: 404 });
      const [wallet, ledger, made, own, withdrawals, who] = await Promise.all([
        db.collection(Wallets.COLLECTION).doc(uid).get(),
        db.collection(WalletLedger.COLLECTION).where(WalletLedger.UID, "==", uid).orderBy(WalletLedger.CREATED_AT, "desc").limit(50).get(),
        db.collection(Referrals.COLLECTION).where(Referrals.REFERRER_UID, "==", uid).orderBy(Referrals.CREATED_AT, "desc").limit(50).get(),
        db.collection(Referrals.COLLECTION).doc(uid).get(),
        db.collection(Withdrawals.COLLECTION).where(Withdrawals.UID, "==", uid).orderBy(Withdrawals.CREATED_AT, "desc").limit(20).get(),
        names([uid])
      ]);
      return NextResponse.json({
        uid,
        name: who.get(uid) ?? "",
        wallet: wallet.exists ? {
          referralCode: wallet.get(Wallets.REFERRAL_CODE) ?? "",
          balancePaise: wallet.get(Wallets.BALANCE_PAISE) ?? 0,
          lifetimeEarnedPaise: wallet.get(Wallets.LIFETIME_EARNED_PAISE) ?? 0,
          withdrawnPaise: wallet.get(Wallets.WITHDRAWN_PAISE) ?? 0,
          successfulReferrals: wallet.get(Wallets.SUCCESSFUL_REFERRALS) ?? 0,
          blocked: wallet.get(Wallets.BLOCKED) === true
        } : null,
        ledger: ledger.docs.map((d) => ({
          id: d.id,
          type: d.get(WalletLedger.TYPE),
          amountPaise: d.get(WalletLedger.AMOUNT_PAISE),
          balanceAfterPaise: d.get(WalletLedger.BALANCE_AFTER_PAISE),
          createdAt: iso(d.get(WalletLedger.CREATED_AT))
        })),
        referredBy: own.exists ? { referrerUid: own.get(Referrals.REFERRER_UID), code: own.get(Referrals.CODE), status: own.get(Referrals.STATUS) } : null,
        referrals: made.docs.map((d) => ({ refereeUid: d.id, status: d.get(Referrals.STATUS), createdAt: iso(d.get(Referrals.CREATED_AT)) })),
        withdrawals: withdrawals.docs.map((d) => ({
          id: d.id, amountPaise: d.get(Withdrawals.AMOUNT_PAISE), status: d.get(Withdrawals.STATUS), createdAt: iso(d.get(Withdrawals.CREATED_AT))
        }))
      });
    }

    const after = params.get("after");
    if (params.get("view") === "referrals") {
      let query = db.collection(Referrals.COLLECTION).orderBy(Referrals.CREATED_AT, "desc");
      if (after) {
        const cursor = await db.collection(Referrals.COLLECTION).doc(after).get();
        if (cursor.exists) query = query.startAfter(cursor);
      }
      const snap = await query.limit(PAGE).get();
      const who = await names(snap.docs.flatMap((d) => [d.id, String(d.get(Referrals.REFERRER_UID) ?? "")]));
      return NextResponse.json({
        referrals: snap.docs.map((d) => ({
          id: d.id,
          referee: who.get(d.id) ?? d.id,
          referrerUid: d.get(Referrals.REFERRER_UID),
          referrer: who.get(String(d.get(Referrals.REFERRER_UID))) ?? d.get(Referrals.REFERRER_UID),
          code: d.get(Referrals.CODE),
          status: d.get(Referrals.STATUS),
          fraudScore: d.get(Referrals.FRAUD_SCORE) ?? 0,
          createdAt: iso(d.get(Referrals.CREATED_AT)),
          completedAt: iso(d.get(Referrals.COMPLETED_AT))
        })),
        nextCursor: snap.size === PAGE ? snap.docs.at(-1)!.id : null
      });
    }

    const status = params.get("status");
    let query = status
      ? db.collection(Withdrawals.COLLECTION).where(Withdrawals.STATUS, "==", status).orderBy(Withdrawals.CREATED_AT, "desc")
      : db.collection(Withdrawals.COLLECTION).orderBy(Withdrawals.CREATED_AT, "desc");
    if (after) {
      const cursor = await db.collection(Withdrawals.COLLECTION).doc(after).get();
      if (cursor.exists) query = query.startAfter(cursor);
    }
    const snap = await query.limit(PAGE).get();
    const who = await names(snap.docs.map((d) => String(d.get(Withdrawals.UID) ?? "")));
    return NextResponse.json({
      withdrawals: snap.docs.map((d) => ({
        id: d.id,
        uid: d.get(Withdrawals.UID),
        user: who.get(String(d.get(Withdrawals.UID))) ?? d.get(Withdrawals.UID),
        amountPaise: d.get(Withdrawals.AMOUNT_PAISE),
        upiId: d.get(Withdrawals.UPI_ID),
        status: d.get(Withdrawals.STATUS),
        txnRef: d.get(Withdrawals.TXN_REF) ?? "",
        failureReason: d.get(Withdrawals.FAILURE_REASON) ?? "",
        createdAt: iso(d.get(Withdrawals.CREATED_AT)),
        processedAt: iso(d.get(Withdrawals.PROCESSED_AT))
      })),
      nextCursor: snap.size === PAGE ? snap.docs.at(-1)!.id : null
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load referrals." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
