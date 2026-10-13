import { NextRequest, NextResponse } from "next/server";
import { FieldPath, Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { e164 } from "@/lib/firebase/admin-account-deletion";
import { EmployerProfiles, PhoneRoles, ReferralCodes, Wallets, WorkerProfiles } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const MAX_LIMIT = 200;

function jsonSafe(value: unknown): unknown {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(jsonSafe);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([k, v]) => [k, jsonSafe(v)]));
  }
  return value;
}

export type ReferralStatRow = {
  id: string; // user uid
  uid: string;
  phone: string;
  resolvedPhone: string;
  name: string;
  resolvedName: string;
  role: "WORKER" | "EMPLOYER";
  referralCode: string;
  balancePaise: number;
  balanceRupees: number;
  lifetimeEarnedPaise: number;
  lifetimeEarnedRupees: number;
  withdrawnPaise: number;
  withdrawnRupees: number;
  successfulReferrals: number;
  awardedMilestones: number[];
  blocked: boolean;
  updatedAt?: string;
};

async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    const col = db.collection(Wallets.COLLECTION);

    const query = (params.get("q") || params.get("phone") || params.get("code") || "").trim();
    const roleParam = params.get("role")?.toUpperCase();
    const blockedParam = params.get("blocked");
    const limit = Math.min(MAX_LIMIT, Math.max(1, Number(params.get("limit")) || 100));

    // Fast-path: Search by query (Phone, Referral code, UID)
    if (query) {
      const cleanDigits = query.replace(/\D/g, "");
      const isLikelyPhone = cleanDigits.length >= 10;
      let matchedUids: string[] = [];

      if (isLikelyPhone) {
        const formattedPhone = e164(query) || (cleanDigits.length === 10 ? `+91${cleanDigits}` : `+${cleanDigits}`);
        const phoneRoleSnap = await db.collection(PhoneRoles.COLLECTION).doc(formattedPhone).get();
        if (phoneRoleSnap.exists) {
          const uid = String(phoneRoleSnap.get(PhoneRoles.UID) || "");
          if (uid) matchedUids.push(uid);
        }
        if (!matchedUids.length) {
          const [wSnap, eSnap] = await Promise.all([
            db.collection(WorkerProfiles.COLLECTION).where(WorkerProfiles.PHONE, "==", formattedPhone).limit(1).get(),
            db.collection(EmployerProfiles.COLLECTION).where(EmployerProfiles.PHONE, "==", formattedPhone).limit(1).get()
          ]);
          if (!wSnap.empty) matchedUids.push(wSnap.docs[0].id);
          else if (!eSnap.empty) matchedUids.push(eSnap.docs[0].id);
        }
      }

      // Check if query is a referral code
      const codeCandidate = query.toUpperCase();
      const codeSnap = await db.collection(ReferralCodes.COLLECTION).doc(codeCandidate).get();
      if (codeSnap.exists) {
        const codeUid = String(codeSnap.get(ReferralCodes.UID) || "");
        if (codeUid && !matchedUids.includes(codeUid)) matchedUids.push(codeUid);
      }

      // Check if query matches a direct wallet doc ID
      if (!matchedUids.length) {
        const directDoc = await col.doc(query).get();
        if (directDoc.exists) matchedUids.push(directDoc.id);
      }

      if (matchedUids.length > 0) {
        const uids = matchedUids;
        const [wallets, workers, employers] = await Promise.all([
          db.getAll(...uids.map((u) => col.doc(u))),
          db.getAll(...uids.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u))),
          db.getAll(...uids.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u)))
        ]);

        const workerMap = new Map(workers.filter((w) => w.exists).map((w) => [w.id, w.data()!]));
        const employerMap = new Map(employers.filter((e) => e.exists).map((e) => [e.id, e.data()!]));

        const items: ReferralStatRow[] = [];
        wallets.filter((w) => w.exists).forEach((w) => {
          const uid = w.id;
          const data = w.data() || {};
          const wProf = workerMap.get(uid);
          const eProf = employerMap.get(uid);
          const phone = String(wProf?.phone || eProf?.phone || "");
          const name = String(wProf?.name || eProf?.ownerName || eProf?.businessName || "");
          const role = eProf ? "EMPLOYER" : "WORKER";

          const balancePaise = Number(data[Wallets.BALANCE_PAISE] || 0);
          const lifetimeEarnedPaise = Number(data[Wallets.LIFETIME_EARNED_PAISE] || 0);
          const withdrawnPaise = Number(data[Wallets.WITHDRAWN_PAISE] || 0);

          items.push({
            id: uid,
            uid,
            phone,
            resolvedPhone: phone,
            name,
            resolvedName: name,
            role,
            referralCode: String(data[Wallets.REFERRAL_CODE] || ""),
            balancePaise,
            balanceRupees: balancePaise / 100,
            lifetimeEarnedPaise,
            lifetimeEarnedRupees: lifetimeEarnedPaise / 100,
            withdrawnPaise,
            withdrawnRupees: withdrawnPaise / 100,
            successfulReferrals: Number(data[Wallets.SUCCESSFUL_REFERRALS] || 0),
            awardedMilestones: Array.isArray(data[Wallets.AWARDED_MILESTONES]) ? data[Wallets.AWARDED_MILESTONES].map(Number) : [],
            blocked: data[Wallets.BLOCKED] === true,
            updatedAt: data[Wallets.UPDATED_AT] instanceof Timestamp ? data[Wallets.UPDATED_AT].toDate().toISOString() : undefined
          });
        });

        return NextResponse.json({
          items,
          nextCursor: null,
          summary: {
            totalWallets: items.length,
            totalBalanceRupees: items.reduce((s, i) => s + i.balanceRupees, 0),
            totalEarnedRupees: items.reduce((s, i) => s + i.lifetimeEarnedRupees, 0),
            totalWithdrawnRupees: items.reduce((s, i) => s + i.withdrawnRupees, 0),
            totalReferrals: items.reduce((s, i) => s + i.successfulReferrals, 0)
          }
        });
      }
    }

    let firestoreQuery: FirebaseFirestore.Query = col.orderBy(FieldPath.documentId());

    const after = params.get("after");
    if (after) {
      const cursor = await col.doc(after).get();
      if (cursor.exists) firestoreQuery = firestoreQuery.startAfter(cursor);
    }

    const snap = await firestoreQuery.limit(limit).get();
    const uids = snap.docs.map((d) => d.id);

    const [workers, employers] = await Promise.all([
      uids.length ? db.getAll(...uids.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u))) : [],
      uids.length ? db.getAll(...uids.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u))) : []
    ]);

    const workerMap = new Map(workers.filter((w) => w.exists).map((w) => [w.id, w.data()!]));
    const employerMap = new Map(employers.filter((e) => e.exists).map((e) => [e.id, e.data()!]));

    let items: ReferralStatRow[] = snap.docs.map((d) => {
      const uid = d.id;
      const data = d.data() || {};
      const wProf = workerMap.get(uid);
      const eProf = employerMap.get(uid);
      const phone = String(wProf?.phone || eProf?.phone || "");
      const name = String(wProf?.name || eProf?.ownerName || eProf?.businessName || "");
      const role: "WORKER" | "EMPLOYER" = eProf ? "EMPLOYER" : "WORKER";

      const balancePaise = Number(data[Wallets.BALANCE_PAISE] || 0);
      const lifetimeEarnedPaise = Number(data[Wallets.LIFETIME_EARNED_PAISE] || 0);
      const withdrawnPaise = Number(data[Wallets.WITHDRAWN_PAISE] || 0);

      return {
        id: uid,
        uid,
        phone,
        resolvedPhone: phone,
        name,
        resolvedName: name,
        role,
        referralCode: String(data[Wallets.REFERRAL_CODE] || ""),
        balancePaise,
        balanceRupees: balancePaise / 100,
        lifetimeEarnedPaise,
        lifetimeEarnedRupees: lifetimeEarnedPaise / 100,
        withdrawnPaise,
        withdrawnRupees: withdrawnPaise / 100,
        successfulReferrals: Number(data[Wallets.SUCCESSFUL_REFERRALS] || 0),
        awardedMilestones: Array.isArray(data[Wallets.AWARDED_MILESTONES]) ? data[Wallets.AWARDED_MILESTONES].map(Number) : [],
        blocked: data[Wallets.BLOCKED] === true,
        updatedAt: data[Wallets.UPDATED_AT] instanceof Timestamp ? data[Wallets.UPDATED_AT].toDate().toISOString() : undefined
      };
    });

    if (roleParam === "WORKER" || roleParam === "EMPLOYER") {
      items = items.filter((i) => i.role === roleParam);
    }
    if (blockedParam === "true") {
      items = items.filter((i) => i.blocked === true);
    } else if (blockedParam === "false") {
      items = items.filter((i) => i.blocked === false);
    }

    const summary = {
      totalWallets: items.length,
      totalBalanceRupees: items.reduce((s, i) => s + i.balanceRupees, 0),
      totalEarnedRupees: items.reduce((s, i) => s + i.lifetimeEarnedRupees, 0),
      totalWithdrawnRupees: items.reduce((s, i) => s + i.withdrawnRupees, 0),
      totalReferrals: items.reduce((s, i) => s + i.successfulReferrals, 0)
    };

    return NextResponse.json({
      items,
      nextCursor: snap.size === limit ? snap.docs.at(-1)!.id : null,
      summary
    });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to load referral stats." },
      { status: 500 }
    );
  }
}

export const GET = cachedAdminGet(getUncached);

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    const body = (await request.json()) as { uid?: string; blocked?: boolean };
    const uid = body.uid?.trim();
    if (!uid) return NextResponse.json({ error: "User UID is required." }, { status: 400 });

    const walletRef = db.collection(Wallets.COLLECTION).doc(uid);
    const snap = await walletRef.get();
    if (!snap.exists) return NextResponse.json({ error: "Wallet not found." }, { status: 404 });

    const blocked = body.blocked === true;
    await walletRef.set({ [Wallets.BLOCKED]: blocked, [Wallets.UPDATED_AT]: Timestamp.now() }, { merge: true });

    return NextResponse.json({ ok: true, uid, blocked });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to update wallet status." },
      { status: 500 }
    );
  }
}
