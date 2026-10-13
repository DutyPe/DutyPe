import { NextRequest, NextResponse } from "next/server";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { e164 } from "@/lib/firebase/admin-account-deletion";
import {
  EmployerProfiles,
  PhoneRoles,
  ReferralCodes,
  Wallets,
  WorkerProfiles
} from "@/lib/firebase/schema";

export const runtime = "nodejs";

const PAGE_SIZE = 100;

export type ReferralCodeRow = {
  id: string; // for compatibility with collection viewer
  code: string;
  uid: string;
  phone: string;
  name: string;
  businessName?: string;
  area: string;
  role: "WORKER" | "EMPLOYER";
  active: boolean;
  balancePaise: number;
  balanceRupees: number;
  successfulReferrals: number;
  lifetimeEarnedRupees: number;
  withdrawnRupees: number;
  shareUrl: string;
};

async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    const query = (params.get("q") || params.get("phone") || params.get("code") || "").trim();
    const roleParam = params.get("role")?.toUpperCase();
    const activeParam = params.get("active");
    const after = params.get("after");
    const limit = Math.min(200, Math.max(1, Number(params.get("limit")) || PAGE_SIZE));

    // Fast-path: Search by exact phone number or referral code
    if (query) {
      const cleanDigits = query.replace(/\D/g, "");
      const isLikelyPhone = cleanDigits.length >= 10;
      let matchedUids: string[] = [];
      let directCodeDoc: FirebaseFirestore.DocumentSnapshot | null = null;

      if (isLikelyPhone) {
        const formattedPhone = e164(query) || (cleanDigits.length === 10 ? `+91${cleanDigits}` : `+${cleanDigits}`);
        // 1. Check phoneRoles
        const phoneRoleSnap = await db.collection(PhoneRoles.COLLECTION).doc(formattedPhone).get();
        if (phoneRoleSnap.exists) {
          const uid = String(phoneRoleSnap.get(PhoneRoles.UID) || "");
          if (uid) matchedUids.push(uid);
        }

        // 2. Fallback: Search worker_profiles and employer_profiles by phone
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
        directCodeDoc = codeSnap;
        const codeUid = String(codeSnap.get(ReferralCodes.UID) || "");
        if (codeUid && !matchedUids.includes(codeUid)) matchedUids.push(codeUid);
      }

      // If we matched UIDs through phone or direct code
      if (matchedUids.length > 0 || directCodeDoc) {
        const uids = matchedUids;
        const [workers, employers, wallets] = await Promise.all([
          uids.length ? db.getAll(...uids.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u))) : [],
          uids.length ? db.getAll(...uids.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u))) : [],
          uids.length ? db.getAll(...uids.map((u) => db.collection(Wallets.COLLECTION).doc(u))) : []
        ]);

        const workerMap = new Map(workers.filter((w) => w.exists).map((w) => [w.id, w.data()!]));
        const employerMap = new Map(employers.filter((e) => e.exists).map((e) => [e.id, e.data()!]));
        const walletMap = new Map(wallets.filter((w) => w.exists).map((w) => [w.id, w.data()!]));

        const results: ReferralCodeRow[] = [];

        // Add direct code doc if found
        if (directCodeDoc && directCodeDoc.exists) {
          const cCode = directCodeDoc.id;
          const cUid = String(directCodeDoc.get(ReferralCodes.UID) || "");
          const cRole = (String(directCodeDoc.get(ReferralCodes.ROLE) || "WORKER").toUpperCase() === "EMPLOYER" ? "EMPLOYER" : "WORKER") as "WORKER" | "EMPLOYER";
          const wProf = workerMap.get(cUid);
          const eProf = employerMap.get(cUid);
          const wData = walletMap.get(cUid);

          const phone = String(wProf?.phone || eProf?.phone || directCodeDoc.get("phone") || "");
          const name = String(wProf?.name || eProf?.ownerName || eProf?.businessName || directCodeDoc.get("name") || "");
          const business = eProf?.businessName ? String(eProf.businessName) : undefined;
          const area = String(wProf?.area || eProf?.area || "");
          const balancePaise = Number(wData?.balancePaise ?? 0);
          const lifetimeEarned = Number(wData?.lifetimeEarnedPaise ?? 0);
          const withdrawn = Number(wData?.withdrawnPaise ?? 0);
          const successfulReferrals = Number(wData?.successfulReferrals ?? 0);

          results.push({
            id: cCode,
            code: cCode,
            uid: cUid,
            phone,
            name,
            businessName: business,
            area,
            role: cRole,
            active: directCodeDoc.get(ReferralCodes.ACTIVE) !== false,
            balancePaise,
            balanceRupees: balancePaise / 100,
            successfulReferrals,
            lifetimeEarnedRupees: lifetimeEarned / 100,
            withdrawnRupees: withdrawn / 100,
            shareUrl: `https://dutype.in/refer/${cCode}`
          });
        }

        // Add from matched UIDs if not already added
        for (const uid of uids) {
          const wData = walletMap.get(uid);
          const codeFromWallet = String(wData?.referralCode || "");
          if (codeFromWallet && !results.some((r) => r.code === codeFromWallet)) {
            const wProf = workerMap.get(uid);
            const eProf = employerMap.get(uid);
            const role = (eProf ? "EMPLOYER" : "WORKER") as "WORKER" | "EMPLOYER";
            const phone = String(wProf?.phone || eProf?.phone || "");
            const name = String(wProf?.name || eProf?.ownerName || eProf?.businessName || "");
            const business = eProf?.businessName ? String(eProf.businessName) : undefined;
            const area = String(wProf?.area || eProf?.area || "");
            const balancePaise = Number(wData?.balancePaise ?? 0);
            const lifetimeEarned = Number(wData?.lifetimeEarnedPaise ?? 0);
            const withdrawn = Number(wData?.withdrawnPaise ?? 0);
            const successfulReferrals = Number(wData?.successfulReferrals ?? 0);

            results.push({
              id: codeFromWallet,
              code: codeFromWallet,
              uid,
              phone,
              name,
              businessName: business,
              area,
              role,
              active: wData?.blocked !== true,
              balancePaise,
              balanceRupees: balancePaise / 100,
              successfulReferrals,
              lifetimeEarnedRupees: lifetimeEarned / 100,
              withdrawnRupees: withdrawn / 100,
              shareUrl: `https://dutype.in/refer/${codeFromWallet}`
            });
          }
        }

        return NextResponse.json({
          codes: results,
          items: results,
          counts: { total: results.length, active: results.filter((r) => r.active).length },
          nextCursor: null
        });
      }
    }

    // Default: Browse referral_codes collection with pagination & filters
    const col = db.collection(ReferralCodes.COLLECTION);
    let q: FirebaseFirestore.Query = col;

    if (roleParam === "WORKER" || roleParam === "EMPLOYER") {
      q = q.where(ReferralCodes.ROLE, "==", roleParam);
    }
    if (activeParam === "true" || activeParam === "false") {
      q = q.where(ReferralCodes.ACTIVE, "==", activeParam === "true");
    }

    q = q.orderBy(FirebaseFirestore.FieldPath.documentId());

    if (after) {
      const cursor = await col.doc(after).get();
      if (cursor.exists) q = q.startAfter(cursor);
    }

    const [snap, totalCountSnap, activeCountSnap] = await Promise.all([
      q.limit(limit).get(),
      col.count().get(),
      col.where(ReferralCodes.ACTIVE, "==", true).count().get()
    ]);

    const codeDocs = snap.docs;
    const uids = Array.from(new Set(codeDocs.map((d) => String(d.get(ReferralCodes.UID) || "")).filter(Boolean)));

    // Batch resolve worker profiles, employer profiles, and wallets
    const [workers, employers, wallets] = await Promise.all([
      uids.length ? db.getAll(...uids.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u))) : [],
      uids.length ? db.getAll(...uids.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u))) : [],
      uids.length ? db.getAll(...uids.map((u) => db.collection(Wallets.COLLECTION).doc(u))) : []
    ]);

    const workerMap = new Map(workers.filter((w) => w.exists).map((w) => [w.id, w.data()!]));
    const employerMap = new Map(employers.filter((e) => e.exists).map((e) => [e.id, e.data()!]));
    const walletMap = new Map(wallets.filter((w) => w.exists).map((w) => [w.id, w.data()!]));

    const codes: ReferralCodeRow[] = codeDocs.map((doc) => {
      const code = doc.id;
      const uid = String(doc.get(ReferralCodes.UID) || "");
      const docRole = String(doc.get(ReferralCodes.ROLE) || "WORKER").toUpperCase();
      const role = (docRole === "EMPLOYER" ? "EMPLOYER" : "WORKER") as "WORKER" | "EMPLOYER";
      const wProf = workerMap.get(uid);
      const eProf = employerMap.get(uid);
      const wData = walletMap.get(uid);

      const phone = String(wProf?.phone || eProf?.phone || doc.get("phone") || "");
      const name = String(wProf?.name || eProf?.ownerName || eProf?.businessName || doc.get("name") || "");
      const business = eProf?.businessName ? String(eProf.businessName) : undefined;
      const area = String(wProf?.area || eProf?.area || "");
      const balancePaise = Number(wData?.balancePaise ?? 0);
      const lifetimeEarned = Number(wData?.lifetimeEarnedPaise ?? 0);
      const withdrawn = Number(wData?.withdrawnPaise ?? 0);
      const successfulReferrals = Number(wData?.successfulReferrals ?? 0);

      return {
        id: code,
        code,
        uid,
        phone,
        name,
        businessName: business,
        area,
        role,
        active: doc.get(ReferralCodes.ACTIVE) !== false,
        balancePaise,
        balanceRupees: balancePaise / 100,
        successfulReferrals,
        lifetimeEarnedRupees: lifetimeEarned / 100,
        withdrawnRupees: withdrawn / 100,
        shareUrl: `https://dutype.in/refer/${code}`
      };
    });

    const nextCursor = snap.size === limit ? snap.docs.at(-1)!.id : null;

    return NextResponse.json({
      codes,
      items: codes,
      counts: {
        total: totalCountSnap.data().count,
        active: activeCountSnap.data().count
      },
      nextCursor
    });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to load referral codes." },
      { status: 500 }
    );
  }
}

export const GET = cachedAdminGet(getUncached);

/**
 * PATCH /api/admin/referral-codes
 * Toggle active status of a referral code: { code: string, active: boolean }
 */
export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const body = (await request.json()) as { code?: string; active?: boolean };
    const code = String(body.code || "").trim().toUpperCase();
    if (!code) return NextResponse.json({ error: "Missing code parameter." }, { status: 400 });
    if (typeof body.active !== "boolean") return NextResponse.json({ error: "Missing active boolean." }, { status: 400 });

    const db = getFirebaseAdminDb();
    const docRef = db.collection(ReferralCodes.COLLECTION).doc(code);
    const snap = await docRef.get();
    if (!snap.exists) return NextResponse.json({ error: "Referral code not found." }, { status: 404 });

    await docRef.set({ [ReferralCodes.ACTIVE]: body.active }, { merge: true });

    return NextResponse.json({ success: true, code, active: body.active });
  } catch (error) {
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to update referral code." },
      { status: 500 }
    );
  }
}
