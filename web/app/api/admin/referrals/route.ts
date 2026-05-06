import { NextRequest, NextResponse } from "next/server";
import { FieldValue } from "firebase-admin/firestore";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function firstNonEmptyString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }

  return "";
}

function readNumber(value: unknown) {
  return typeof value === "number" ? value : Number(value ?? 0) || 0;
}

function buildRecordMap(snapshot: FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>) {
  const map = new Map<string, Record<string, unknown>>();
  snapshot.forEach((item) => map.set(item.id, asRecord(item.data())));
  return map;
}

function buildPhoneRoleMap(snapshot: FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>) {
  const map = new Map<string, Record<string, unknown> & { docId: string }>();
  snapshot.forEach((item) => {
    const data = asRecord(item.data());
    const uid = firstNonEmptyString(data.uid);
    if (uid) {
      map.set(uid, { ...data, docId: item.id });
    }
  });
  return map;
}

async function readReferralMinWithdrawal(db: FirebaseFirestore.Firestore) {
  try {
    const snapshot = await db.collection("app_config").doc("referral").get();
    const data = snapshot.data() ?? {};
    return Math.max(readNumber(data.minWithdrawal) || 100, 100);
  } catch {
    return 100;
  }
}

function identityForUser(
  userId: string,
  maps: {
    phoneRoles: Map<string, Record<string, unknown> & { docId: string }>;
    workerProfiles: Map<string, Record<string, unknown>>;
    employerProfiles: Map<string, Record<string, unknown>>;
    referralStats: Map<string, Record<string, unknown>>;
  }
) {
  const phoneRole = (maps.phoneRoles.get(userId) ?? {}) as Record<string, unknown> & { docId?: string };
  const workerProfile = maps.workerProfiles.get(userId) ?? {};
  const employerProfile = maps.employerProfiles.get(userId) ?? {};
  const referralStats = maps.referralStats.get(userId) ?? {};

  return {
    userName: firstNonEmptyString(
      phoneRole.name,
      workerProfile.fullName,
      workerProfile.name,
      employerProfile.companyName,
      employerProfile.fullName,
      referralStats.userName
    ),
    phone: firstNonEmptyString(
      phoneRole.phoneNumber,
      workerProfile.phone,
      workerProfile.phoneNumber,
      employerProfile.phone,
      employerProfile.phoneNumber
    ),
    role: firstNonEmptyString(
      phoneRole.role,
      referralStats.userRole,
      workerProfile.role,
      employerProfile.role
    ).toUpperCase(),
    referralCode: firstNonEmptyString(
      referralStats.referralCode,
      workerProfile.referralCode,
      employerProfile.referralCode
    ),
    phoneRoleDocId: firstNonEmptyString(phoneRole.docId)
  };
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();

    const [
      referralsSnapshot,
      withdrawalsSnapshot,
      phoneRolesSnapshot,
      workerProfilesSnapshot,
      employerProfilesSnapshot,
      referralStatsSnapshot
    ] = await Promise.all([
      db.collection("referrals").orderBy("createdAt", "desc").limit(500).get(),
      db.collectionGroup("withdrawals").orderBy("createdAt", "desc").limit(200).get(),
      db.collection("phoneRoles").limit(5000).get(),
      db.collection("worker_profiles").limit(5000).get(),
      db.collection("employer_profiles").limit(5000).get(),
      db.collection("referral_stats").limit(5000).get()
    ]);

    const maps = {
      phoneRoles: buildPhoneRoleMap(phoneRolesSnapshot),
      workerProfiles: buildRecordMap(workerProfilesSnapshot),
      employerProfiles: buildRecordMap(employerProfilesSnapshot),
      referralStats: buildRecordMap(referralStatsSnapshot)
    };

    const referrals = referralsSnapshot.docs.map((item) => {
      const raw = asRecord(item.data());
      const referrerId = firstNonEmptyString(raw.referrerId, raw.referrerUserId);
      const referredUserId = firstNonEmptyString(raw.referredUserId);
      const referrer = identityForUser(referrerId, maps);
      const referred = identityForUser(referredUserId, maps);
      const referredStats = maps.referralStats.get(referredUserId) ?? {};

      return {
        id: item.id,
        ...raw,
        referrerId,
        referredUserId,
        referrerUserName: firstNonEmptyString(raw.referrerUserName, referrer.userName),
        referrerPhone: referrer.phone,
        referrerRole: referrer.role,
        referrerReferralCode: referrer.referralCode,
        referredUserName: firstNonEmptyString(raw.referredUserName, referred.userName),
        referredUserPhone: referred.phone,
        referredUserRole: firstNonEmptyString(raw.referredUserRole, referred.role),
        referredByCode: firstNonEmptyString(referredStats.referredByCode, raw.referralCode),
        currentReferralCode: referred.referralCode
      };
    });

    const withdrawals = withdrawalsSnapshot.docs.map((item) => {
      // Path: referral_stats/{userId}/withdrawals/{id}
      const userId = item.ref.parent.parent?.id ?? "";
      const raw = asRecord(item.data());
      const identity = identityForUser(userId, maps);
      const stats = maps.referralStats.get(userId) ?? {};

      return {
        id: item.id,
        userId,
        ...raw,
        userName: identity.userName,
        phone: identity.phone,
        userRole: firstNonEmptyString(raw.userRole, identity.role),
        referralCode: identity.referralCode,
        availableBalance: readNumber(stats.availableBalance),
        totalEarnings: readNumber(stats.totalEarnings),
        withdrawnAmount: readNumber(stats.withdrawnAmount),
        totalWithdrawals: readNumber(stats.totalWithdrawals)
      };
    });

    return NextResponse.json({ referrals, withdrawals });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load referrals.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateWithdrawalBody = {
  withdrawalId?: string;
  userId?: string;
  status?: string;
  transactionId?: string;
  adminNote?: string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateWithdrawalBody;

  try {
    body = (await request.json()) as UpdateWithdrawalBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const withdrawalId = body.withdrawalId?.trim();
  const userId = body.userId?.trim();
  const status = body.status?.trim().toUpperCase();
  const transactionId = body.transactionId?.trim();
  const adminNote = body.adminNote?.trim();

  if (!withdrawalId || !userId || (status !== "PROCESSING" && status !== "COMPLETED" && status !== "FAILED")) {
    return NextResponse.json(
      { error: "Invalid withdrawalId, userId, or status." },
      { status: 400 }
    );
  }

  try {
    const db = getFirebaseAdminDb();
    const minWithdrawal = await readReferralMinWithdrawal(db);

    const statsRef = db.collection("referral_stats").doc(userId);
    const withdrawalRef = statsRef.collection("withdrawals").doc(withdrawalId);
    const auditRef = statsRef.collection("audit_logs").doc();

    await db.runTransaction(async (transaction) => {
      const withdrawalDoc = await transaction.get(withdrawalRef);
      const statsDoc = await transaction.get(statsRef);
      if (!withdrawalDoc.exists) {
        throw new Error(`Withdrawal ${withdrawalId} not found for user ${userId}.`);
      }

      const now = new Date();
      const current = withdrawalDoc.data() ?? {};
      const stats = statsDoc.data() ?? {};
      const currentStatus = firstNonEmptyString(current.status).toUpperCase();
      const amount = readNumber(current.amount);
      const currentAvailableBalance = readNumber(stats.availableBalance);
      const update: Record<string, unknown> = {
        status,
        updatedAt: now
      };

      if (status === "PROCESSING") {
        update.approvedAt = now;
      }

      if (status === "COMPLETED") {
        update.processedAt = now;
        update.paidAt = now;
        if (transactionId) update.transactionId = transactionId;

        const statsUpdate: Record<string, unknown> = {
          lastPaidWithdrawalAt: now,
          lastUpdated: now
        };
        if (current.balanceDeductedAtRequest === false && amount > 0) {
          const nextBalance = Math.max(0, currentAvailableBalance - amount);
          statsUpdate.availableBalance = nextBalance;
          statsUpdate.withdrawnAmount = FieldValue.increment(amount);
          statsUpdate.canWithdraw = nextBalance >= minWithdrawal;
          update.balanceDeductedAtCompletion = true;
          update.balanceBeforePayment = currentAvailableBalance;
          update.balanceAfterPayment = nextBalance;
        } else {
          statsUpdate.canWithdraw = currentAvailableBalance >= minWithdrawal;
        }
        transaction.set(statsRef, statsUpdate, { merge: true });
      }

      if (status === "FAILED") {
        update.processedAt = now;
        update.rejectedAt = now;
        if (adminNote) update.adminNote = adminNote;
        if (current.refundApplied !== true && currentStatus !== "FAILED" && amount > 0) {
          update.refundApplied = true;
          update.refundedAt = now;
          update.balanceBeforeRefund = currentAvailableBalance;
          update.balanceAfterRefund = currentAvailableBalance + amount;
          transaction.set(statsRef, {
            availableBalance: FieldValue.increment(amount),
            withdrawnAmount: FieldValue.increment(-amount),
            canWithdraw: currentAvailableBalance + amount >= minWithdrawal,
            lastFailedWithdrawalAt: now,
            lastUpdated: now
          }, { merge: true });
        }
      }

      if (adminNote && status !== "FAILED") {
        update.adminNote = adminNote;
      }

      transaction.set(withdrawalRef, update, { merge: true });
      transaction.set(auditRef, {
        eventType: `WITHDRAWAL_${status}`,
        userId,
        withdrawalId,
        amount,
        transactionId: transactionId || null,
        adminNote: adminNote || null,
        timestamp: now
      });
    });

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update withdrawal.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
