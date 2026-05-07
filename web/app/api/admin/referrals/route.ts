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

function timestampMillis(value: unknown) {
  if (!value) return 0;
  if (value instanceof Date) return value.getTime();
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = new Date(value).getTime();
    return Number.isNaN(parsed) ? 0 : parsed;
  }
  if (typeof value === "object") {
    const candidate = value as { seconds?: number; _seconds?: number; toDate?: () => Date };
    if (typeof candidate.toDate === "function") return candidate.toDate().getTime();
    const seconds = typeof candidate.seconds === "number" ? candidate.seconds : candidate._seconds;
    if (typeof seconds === "number") return seconds * 1000;
  }
  return 0;
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

function buildReferralCodeMaps(snapshot: FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>) {
  const byCode = new Map<string, Record<string, unknown> & { id: string; code: string }>();
  const byUserId = new Map<string, Array<Record<string, unknown> & { id: string; code: string }>>();

  snapshot.forEach((item) => {
    const data = asRecord(item.data());
    const code = firstNonEmptyString(data.code, item.id);
    if (!code) return;
    const record = { id: item.id, ...data, code };
    byCode.set(code.toUpperCase(), record);

    const userId = firstNonEmptyString(data.userId, data.uid);
    if (userId) {
      const records = byUserId.get(userId) ?? [];
      records.push(record);
      byUserId.set(userId, records);
    }
  });

  return { byCode, byUserId };
}

type IdentityMaps = {
  phoneRoles: Map<string, Record<string, unknown> & { docId: string }>;
  workerProfiles: Map<string, Record<string, unknown>>;
  employerProfiles: Map<string, Record<string, unknown>>;
  referralStats: Map<string, Record<string, unknown>>;
};

type AdminReferralRow = Record<string, unknown> & {
  id: string;
  referrerId: string;
  referredUserId: string;
  referrerUserName: string;
  referrerPhone: string;
  referrerRole: string;
  referrerReferralCode: string;
  referredUserName: string;
  referredUserPhone: string;
  referredUserRole: string;
  referredByCode: string;
  currentReferralCode: string;
};

type AdminGenericRow = Record<string, unknown> & { id: string };

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
  maps: IdentityMaps
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

function enrichReferralDoc(
  item: FirebaseFirestore.QueryDocumentSnapshot<FirebaseFirestore.DocumentData>,
  maps: IdentityMaps
): AdminReferralRow {
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
}

async function loadIdentityMaps(db: FirebaseFirestore.Firestore) {
  const [
    phoneRolesSnapshot,
    workerProfilesSnapshot,
    employerProfilesSnapshot,
    referralStatsSnapshot,
    referralCodesSnapshot
  ] = await Promise.all([
    db.collection("phoneRoles").limit(5000).get(),
    db.collection("worker_profiles").limit(5000).get(),
    db.collection("employer_profiles").limit(5000).get(),
    db.collection("referral_stats").limit(5000).get(),
    db.collection("referral_codes").limit(5000).get()
  ]);

  return {
    maps: {
      phoneRoles: buildPhoneRoleMap(phoneRolesSnapshot),
      workerProfiles: buildRecordMap(workerProfilesSnapshot),
      employerProfiles: buildRecordMap(employerProfilesSnapshot),
      referralStats: buildRecordMap(referralStatsSnapshot)
    },
    referralCodes: buildReferralCodeMaps(referralCodesSnapshot)
  };
}

function resolveLookupUserId(
  lookup: string,
  maps: IdentityMaps,
  referralCodes: ReturnType<typeof buildReferralCodeMaps>
) {
  const normalized = lookup.trim();
  const upper = normalized.toUpperCase();
  const codeRecord = referralCodes.byCode.get(upper);
  if (codeRecord) {
    return {
      userId: firstNonEmptyString(codeRecord.userId, codeRecord.uid),
      resolvedBy: "referralCode",
      referralCode: codeRecord.code
    };
  }

  if (maps.referralStats.has(normalized) || maps.workerProfiles.has(normalized) || maps.employerProfiles.has(normalized) || maps.phoneRoles.has(normalized)) {
    return { userId: normalized, resolvedBy: "uid", referralCode: "" };
  }

  for (const [userId, phoneRole] of maps.phoneRoles.entries()) {
    if (
      firstNonEmptyString(phoneRole.docId) === normalized ||
      firstNonEmptyString(phoneRole.phoneNumber, phoneRole.phone) === normalized
    ) {
      return { userId, resolvedBy: "phone", referralCode: "" };
    }
  }

  for (const [userId, profile] of [...maps.workerProfiles.entries(), ...maps.employerProfiles.entries()]) {
    if (firstNonEmptyString(profile.phone, profile.phoneNumber, profile.contactPhone) === normalized) {
      return { userId, resolvedBy: "phone", referralCode: "" };
    }
  }

  return { userId: "", resolvedBy: "unknown", referralCode: "" };
}

async function buildReferralLookup(db: FirebaseFirestore.Firestore, lookup: string) {
  const { maps, referralCodes } = await loadIdentityMaps(db);
  const resolved = resolveLookupUserId(lookup, maps, referralCodes);
  const userId = resolved.userId;

  if (!userId) {
    return {
      query: lookup,
      found: false,
      message: "No user matched that referral code, phone number, or uid."
    };
  }

  const identity = identityForUser(userId, maps);
  const codeRecords = referralCodes.byUserId.get(userId) ?? [];
  const referralCode = firstNonEmptyString(
    resolved.referralCode,
    ...codeRecords.map((record) => record.code),
    identity.referralCode
  );

  const referrerQueries: Array<Promise<FirebaseFirestore.QuerySnapshot<FirebaseFirestore.DocumentData>>> = [
    db.collection("referrals").where("referrerUserId", "==", userId).limit(500).get(),
    db.collection("referrals").where("referrerId", "==", userId).limit(500).get()
  ];
  codeRecords.forEach((record) => {
    referrerQueries.push(db.collection("referrals").where("referralCode", "==", record.code).limit(500).get());
  });
  if (referralCode && !codeRecords.some((record) => record.code === referralCode)) {
    referrerQueries.push(db.collection("referrals").where("referralCode", "==", referralCode).limit(500).get());
  }

  const [referrerSnapshots, referredSnapshot, withdrawalsSnapshot, auditLogsSnapshot] = await Promise.all([
    Promise.all(referrerQueries),
    db.collection("referrals").where("referredUserId", "==", userId).limit(500).get(),
    db.collection("referral_stats").doc(userId).collection("withdrawals").limit(200).get(),
    db.collection("referral_stats").doc(userId).collection("audit_logs").limit(200).get()
  ]);

  const referralsById = new Map<string, FirebaseFirestore.QueryDocumentSnapshot<FirebaseFirestore.DocumentData>>();
  referrerSnapshots.forEach((snapshot) => snapshot.docs.forEach((doc) => referralsById.set(doc.id, doc)));

  const referralsAsReferrer = Array.from(referralsById.values())
    .map((doc) => enrichReferralDoc(doc, maps))
    .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));
  const referralsAsReferred = referredSnapshot.docs
    .map((doc) => enrichReferralDoc(doc, maps))
    .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));
  const withdrawals = withdrawalsSnapshot.docs
    .map((doc): AdminGenericRow => ({ id: doc.id, ...asRecord(doc.data()) }))
    .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));
  const auditLogs = auditLogsSnapshot.docs
    .map((doc): AdminGenericRow => ({ id: doc.id, ...asRecord(doc.data()) }))
    .sort((a, b) => timestampMillis(b.timestamp) - timestampMillis(a.timestamp));

  return {
    query: lookup,
    found: true,
    resolvedBy: resolved.resolvedBy,
    userId,
    referralCode,
    identity,
    referralStats: maps.referralStats.get(userId) ?? null,
    referralCodes: codeRecords,
    referralsAsReferrer,
    referralsAsReferred,
    withdrawals,
    auditLogs
  };
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const lookup = new URL(request.url).searchParams.get("lookup")?.trim();

    if (lookup) {
      const result = await buildReferralLookup(db, lookup);
      return NextResponse.json({ lookup: result });
    }

    const [
      referralsSnapshot,
      withdrawalsSnapshot,
      identityData
    ] = await Promise.all([
      db.collection("referrals").limit(1000).get(),
      db.collectionGroup("withdrawals").limit(500).get(),
      loadIdentityMaps(db)
    ]);

    const maps = identityData.maps;

    const referrals = referralsSnapshot.docs
      .map((item) => enrichReferralDoc(item, maps))
      .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));

    const withdrawals = withdrawalsSnapshot.docs
      .map((item): AdminGenericRow => {
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
      })
      .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));

    return NextResponse.json({ referrals, withdrawals });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load referrals.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}


export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const body = (await request.json()) as { action?: string; referralCode?: string };
    if (body.action && body.action !== "create-test-referral") {
      return NextResponse.json({ error: "Unsupported referral action." }, { status: 400 });
    }

    const referralCode = body.referralCode?.trim().toUpperCase();
    if (!referralCode) {
      return NextResponse.json({ error: "Referral code is required." }, { status: 400 });
    }

    const db = getFirebaseAdminDb();
    const lookup = await buildReferralLookup(db, referralCode);
    const referrerUserId = "userId" in lookup ? lookup.userId : "";
    const resolvedReferralCode = "referralCode" in lookup ? lookup.referralCode || referralCode : referralCode;

    if (!lookup.found || !referrerUserId) {
      return NextResponse.json({ error: lookup.message || "Referral code not found." }, { status: 404 });
    }

    const now = FieldValue.serverTimestamp();
    const bonusAmount = 50;
    const testUserId = `TEST_USER_${Date.now()}`;
    const referralId = `${referrerUserId}_${testUserId}`;
    const statsRef = db.collection("referral_stats").doc(referrerUserId);
    const referralRef = db.collection("referrals").doc(referralId);
    const auditRef = statsRef.collection("audit_logs").doc();
    const batch = db.batch();

    batch.set(referralRef, {
      referralCode: resolvedReferralCode,
      referrerUserId: referrerUserId,
      referrerId: referrerUserId,
      referredUserId: testUserId,
      referredUserName: "Test User",
      referredUserPhone: "+919999999999",
      referredUserRole: "WORKER",
      status: "COMPLETED",
      rewardAmount: bonusAmount,
      bonusAmount,
      createdAt: now,
      completedAt: now,
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      createdBy: "admin-test-referral-tool"
    });

    batch.set(statsRef, {
      userId: referrerUserId,
      referralCode: resolvedReferralCode,
      totalEarnings: FieldValue.increment(bonusAmount),
      availableBalance: FieldValue.increment(bonusAmount),
      successfulReferrals: FieldValue.increment(1),
      totalReferrals: FieldValue.increment(1),
      lastReferralAt: now,
      updatedAt: now
    }, { merge: true });

    batch.set(auditRef, {
      eventType: "ADMIN_TEST_REFERRAL_CREATED",
      referralCode: resolvedReferralCode,
      referralId,
      testUserId,
      amount: bonusAmount,
      timestamp: now,
      createdAt: now
    });

    await batch.commit();

    const updatedLookup = await buildReferralLookup(db, referralCode);

    return NextResponse.json({
      ok: true,
      referralId,
      testUserId,
      bonusAmount,
      lookup: updatedLookup
    });
  } catch (error) {
    console.error("Failed to create test referral", error);
    return NextResponse.json(
      { error: error instanceof Error ? error.message : "Failed to create test referral." },
      { status: 500 }
    );
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
