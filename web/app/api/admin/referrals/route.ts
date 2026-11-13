import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { e164, findUserIdsForPhone } from "@/lib/firebase/admin-account-deletion";
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
 *   ?lookup=CODE|PHONE|UID               one user's wallet, ledger, referrals, referrer cluster and withdrawals
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

  for (let i = 0; i < unique.length; i += 100) {
    const chunk = unique.slice(i, i + 100);
    const [workers, employers, users] = await Promise.all([
      db.getAll(...chunk.map((u) => db.collection(WorkerProfiles.COLLECTION).doc(u)), {
        fieldMask: [WorkerProfiles.NAME, WorkerProfiles.PHONE]
      }),
      db.getAll(...chunk.map((u) => db.collection(EmployerProfiles.COLLECTION).doc(u)), {
        fieldMask: [EmployerProfiles.OWNER_NAME, EmployerProfiles.BUSINESS_NAME, EmployerProfiles.PHONE]
      }),
      db.getAll(...chunk.map((u) => db.collection("users").doc(u)), {
        fieldMask: ["name", "displayName", "phoneNumber", "phone"]
      })
    ]);

    workers.forEach((d) => {
      if (d.exists) {
        const name = d.get(WorkerProfiles.NAME) || "";
        const phone = d.get(WorkerProfiles.PHONE) || "";
        if (name || phone) out.set(d.id, `${name} · ${phone} (worker)`.trim().replace(/^·\s*|\s*·$/g, ""));
      }
    });

    employers.forEach((d) => {
      if (d.exists && !out.has(d.id)) {
        const name = d.get(EmployerProfiles.BUSINESS_NAME) || d.get(EmployerProfiles.OWNER_NAME) || "";
        const phone = d.get(EmployerProfiles.PHONE) || "";
        if (name || phone) out.set(d.id, `${name} · ${phone} (employer)`.trim().replace(/^·\s*|\s*·$/g, ""));
      }
    });

    users.forEach((d) => {
      if (d.exists && !out.has(d.id)) {
        const name = d.get("name") || d.get("displayName") || "";
        const phone = d.get("phoneNumber") || d.get("phone") || "";
        if (name || phone) out.set(d.id, `${name} · ${phone}`.trim().replace(/^·\s*|\s*·$/g, "") || d.id);
      }
    });
  }

  return out;
}

async function lookupUid(query: string): Promise<string | null> {
  const db = getFirebaseAdminDb();
  const trimmed = query.trim();
  if (!trimmed) return null;

  // 1. Try phone resolution across all formats
  const digits = trimmed.replace(/\D/g, "");
  if (digits.length >= 10) {
    const { userIds } = await findUserIdsForPhone(trimmed);
    if (userIds.length > 0) return userIds[0];
  }

  // 2. Try referral code candidates
  const codeCandidate = trimmed.toUpperCase();
  const codeSnap = await db.collection(ReferralCodes.COLLECTION).doc(codeCandidate).get();
  if (codeSnap.exists) {
    const uid = String(codeSnap.get(ReferralCodes.UID) ?? "");
    if (uid) return uid;
  }

  const walletCodeSnap = await db.collection(Wallets.COLLECTION).where(Wallets.REFERRAL_CODE, "==", codeCandidate).limit(1).get();
  if (!walletCodeSnap.empty) return walletCodeSnap.docs[0].id;

  const userCodeSnap = await db.collection("users").where("referralCode", "==", codeCandidate).limit(1).get();
  if (!userCodeSnap.empty) return userCodeSnap.docs[0].id;

  // 3. Check if trimmed is already a direct doc ID
  const [uCheck, wCheck, eCheck, wallCheck] = await Promise.all([
    db.collection("users").doc(trimmed).get(),
    db.collection(WorkerProfiles.COLLECTION).doc(trimmed).get(),
    db.collection(EmployerProfiles.COLLECTION).doc(trimmed).get(),
    db.collection(Wallets.COLLECTION).doc(trimmed).get()
  ]);
  if (uCheck.exists || wCheck.exists || eCheck.exists || wallCheck.exists) {
    return trimmed;
  }

  return null;
}

async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;

    const lookup = params.get("lookup")?.trim();
    if (lookup) {
      const uid = await lookupUid(lookup);
      if (!uid) return NextResponse.json({ error: "No account found." }, { status: 404 });

      // 1. Parallel fetch base documents
      const [wallet, userDoc, ledger, own, withdrawals, who, userCodesSnap] = await Promise.all([
        db.collection(Wallets.COLLECTION).doc(uid).get(),
        db.collection("users").doc(uid).get(),
        db.collection(WalletLedger.COLLECTION).where(WalletLedger.UID, "==", uid).orderBy(WalletLedger.CREATED_AT, "desc").limit(50).get(),
        db.collection(Referrals.COLLECTION).doc(uid).get(),
        db.collection(Withdrawals.COLLECTION).where(Withdrawals.UID, "==", uid).orderBy(Withdrawals.CREATED_AT, "desc").limit(20).get(),
        names([uid]),
        db.collection(ReferralCodes.COLLECTION).where(ReferralCodes.UID, "==", uid).get()
      ]);

      // 2. Discover all codes & aliases
      const allCodes = new Set<string>();
      userCodesSnap.docs.forEach((d) => {
        allCodes.add(d.id.toUpperCase());
        const alias = d.get("aliasFor");
        if (alias && typeof alias === "string") allCodes.add(alias.toUpperCase());
      });
      const walletCode = wallet.get(Wallets.REFERRAL_CODE);
      if (walletCode && typeof walletCode === "string") allCodes.add(walletCode.toUpperCase());
      const userDocCode = userDoc.get("referralCode");
      if (userDocCode && typeof userDocCode === "string") allCodes.add(userDocCode.toUpperCase());

      const activeCode = (walletCode || userDocCode || userCodesSnap.docs.find((d) => d.get("active"))?.id || Array.from(allCodes)[0] || "").toUpperCase();
      const codeList = Array.from(allCodes);

      // 3. Fetch all made referrals across all identifiers
      const madeByUid = await db.collection(Referrals.COLLECTION).where(Referrals.REFERRER_UID, "==", uid).orderBy(Referrals.CREATED_AT, "desc").limit(100).get();

      const madeByCodePromises = [];
      if (codeList.length > 0) {
        for (let i = 0; i < codeList.length; i += 10) {
          const chunk = codeList.slice(i, i + 10);
          madeByCodePromises.push(
            db.collection(Referrals.COLLECTION).where(Referrals.CODE, "in", chunk).limit(100).get(),
            db.collection("users").where("referredByCode", "in", chunk).limit(100).get()
          );
        }
      }
      madeByCodePromises.push(db.collection("users").where("referredBy", "==", uid).limit(100).get());
      const extraSnaps = await Promise.all(madeByCodePromises);

      type RefereeItem = {
        refereeUid: string;
        refereeName?: string;
        status: string;
        createdAt: string | null;
      };

      const refereeMap = new Map<string, RefereeItem>();

      madeByUid.docs.forEach((d) => {
        if (d.id !== uid) {
          refereeMap.set(d.id, {
            refereeUid: d.id,
            status: d.get(Referrals.STATUS) || "COMPLETED",
            createdAt: iso(d.get(Referrals.CREATED_AT))
          });
        }
      });

      extraSnaps.forEach((snap) => {
        snap.docs.forEach((d) => {
          if (d.id !== uid && !refereeMap.has(d.id)) {
            refereeMap.set(d.id, {
              refereeUid: d.id,
              status: d.get("status") || "COMPLETED",
              createdAt: iso(d.get("createdAt"))
            });
          }
        });
      });

      const refereeUids = Array.from(refereeMap.keys());
      const refereeNames = refereeUids.length > 0 ? await names(refereeUids) : new Map<string, string>();
      const directReferrals: RefereeItem[] = refereeUids.map((rUid) => {
        const item = refereeMap.get(rUid)!;
        return {
          ...item,
          refereeName: refereeNames.get(rUid) || rUid
        };
      });

      // 4. Resolve referrer info & co-referee cluster
      let referrerUid: string | null = own.exists ? String(own.get(Referrals.REFERRER_UID) || "") || null : null;
      let refCode: string | null = own.exists ? String(own.get(Referrals.CODE) || "") || null : null;
      let refStatus: string = own.exists ? String(own.get(Referrals.STATUS) || "COMPLETED") : "COMPLETED";

      if (!referrerUid && userDoc.exists) {
        referrerUid = String(userDoc.get("referredBy") || "") || null;
        if (!refCode) refCode = String(userDoc.get("referredByCode") || "") || null;
      }

      if (!referrerUid && refCode) {
        const codeDoc = await db.collection(ReferralCodes.COLLECTION).doc(refCode.toUpperCase()).get();
        if (codeDoc.exists) {
          referrerUid = String(codeDoc.get(ReferralCodes.UID) || "") || null;
        }
      }

      let referredByData = null;
      if (referrerUid) {
        const [refWho, refWallet, clusterByUidSnap] = await Promise.all([
          names([referrerUid]),
          db.collection(Wallets.COLLECTION).doc(referrerUid).get(),
          db.collection(Referrals.COLLECTION).where(Referrals.REFERRER_UID, "==", referrerUid).orderBy(Referrals.CREATED_AT, "desc").limit(100).get()
        ]);

        const clusterExtraSnap = refCode
          ? await db.collection(Referrals.COLLECTION).where(Referrals.CODE, "==", refCode.toUpperCase()).limit(100).get()
          : null;

        const clusterDocsMap = new Map<string, { id: string; status: string; createdAt: unknown; phone?: string; name?: string }>();
        clusterByUidSnap.docs.forEach((d) => {
          clusterDocsMap.set(d.id, {
            id: d.id,
            status: String(d.get(Referrals.STATUS) || "COMPLETED"),
            createdAt: d.get(Referrals.CREATED_AT),
            phone: String(d.get("refereePhone") || ""),
            name: String(d.get("refereeName") || "")
          });
        });
        if (clusterExtraSnap) {
          clusterExtraSnap.docs.forEach((d) => {
            if (!clusterDocsMap.has(d.id)) {
              clusterDocsMap.set(d.id, {
                id: d.id,
                status: String(d.get(Referrals.STATUS) || "COMPLETED"),
                createdAt: d.get(Referrals.CREATED_AT),
                phone: String(d.get("refereePhone") || ""),
                name: String(d.get("refereeName") || "")
              });
            }
          });
        }

        const clusterUids = Array.from(clusterDocsMap.keys());
        const clusterNames = clusterUids.length > 0 ? await names(clusterUids) : new Map<string, string>();

        const coReferees = clusterUids.map((cUid) => {
          const item = clusterDocsMap.get(cUid)!;
          const isCurrent = cUid === uid;
          const resolved = isCurrent
            ? `${who.get(uid) || uid}`
            : (clusterNames.get(cUid) || item.name || cUid);

          return {
            uid: cUid,
            name: resolved,
            phone: item.phone || "",
            status: item.status,
            isCurrentUser: isCurrent,
            createdAt: iso(item.createdAt)
          };
        });

        referredByData = {
          referrerUid,
          name: refWho.get(referrerUid) || referrerUid,
          code: refCode || "",
          status: refStatus,
          totalSuccessfulReferrals: refWallet.exists ? Number(refWallet.get(Wallets.SUCCESSFUL_REFERRALS) || 0) : 0,
          totalReferralsCount: coReferees.length,
          coReferees
        };
      }

      return NextResponse.json({
        uid,
        name: who.get(uid) ?? "",
        activeCode,
        codes: codeList,
        wallet: wallet.exists ? {
          referralCode: wallet.get(Wallets.REFERRAL_CODE) ?? activeCode,
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
        referredBy: referredByData,
        referrals: directReferrals,
        withdrawals: withdrawals.docs.map((d) => ({
          id: d.id,
          amountPaise: d.get(Withdrawals.AMOUNT_PAISE),
          status: d.get(Withdrawals.STATUS),
          txnRef: d.get(Withdrawals.TXN_REF) ?? "",
          failureReason: d.get(Withdrawals.FAILURE_REASON) ?? "",
          createdAt: iso(d.get(Withdrawals.CREATED_AT))
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

const cachedGet = cachedAdminGet(getUncached);

export async function GET(request: NextRequest) {
  const url = new URL(request.url);
  // Withdrawals and single-user lookups must bypass cache to reflect instantly
  if (url.searchParams.get("view") === "withdrawals" || url.searchParams.has("nocache") || url.searchParams.has("lookup")) {
    return getUncached(request);
  }
  return cachedGet(request);
}
