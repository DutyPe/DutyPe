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
  pendingReason?: string;
  isProfileComplete?: boolean;
  canFix?: boolean;
  missingFields?: string[];
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

function evaluatePendingReason(referredUserId: string, maps: IdentityMaps) {
  const workerProfile = maps.workerProfiles.get(referredUserId);
  const employerProfile = maps.employerProfiles.get(referredUserId);
  const phoneRole = maps.phoneRoles.get(referredUserId);

  if (workerProfile) {
    const fullName = firstNonEmptyString(workerProfile.fullName, workerProfile.name, phoneRole?.name);
    const phone = firstNonEmptyString(workerProfile.phone, workerProfile.phoneNumber, phoneRole?.phoneNumber);
    const rawSkills = workerProfile.skills;
    const skills = Array.isArray(rawSkills) ? rawSkills.filter(Boolean) : (typeof rawSkills === "string" && rawSkills.trim() ? [rawSkills.trim()] : []);

    const missing: string[] = [];
    if (!fullName) missing.push("Full Name");
    if (!phone) missing.push("Phone");
    if (skills.length === 0) missing.push("Skills");

    const isComplete = Boolean(
      workerProfile.profileCompleted === true ||
      workerProfile.isProfileComplete === true ||
      (fullName && phone && skills.length > 0)
    );

    return {
      isProfileComplete: isComplete,
      canFix: isComplete,
      missingFields: missing,
      pendingReason: isComplete
        ? "Worker Profile 100% complete (Ready to credit)"
        : `Worker Profile incomplete: missing ${missing.join(", ")}`
    };
  }

  if (employerProfile) {
    const name = firstNonEmptyString(employerProfile.companyName, employerProfile.fullName, employerProfile.name, phoneRole?.name);
    const phone = firstNonEmptyString(employerProfile.phone, employerProfile.phoneNumber, employerProfile.contactPhone, phoneRole?.phoneNumber);

    const missing: string[] = [];
    if (!name) missing.push("Company/Employer Name");
    if (!phone) missing.push("Contact Phone");

    const isComplete = Boolean(
      employerProfile.profileCompleted === true ||
      employerProfile.isProfileComplete === true ||
      (name && phone)
    );

    return {
      isProfileComplete: isComplete,
      canFix: isComplete,
      missingFields: missing,
      pendingReason: isComplete
        ? "Employer Profile complete (Ready to credit)"
        : `Employer Profile incomplete: missing ${missing.join(", ")}`
    };
  }

  return {
    isProfileComplete: false,
    canFix: false,
    missingFields: ["Profile not started"],
    pendingReason: "Referred user has not started profile setup"
  };
}

function enrichReferralDoc(
  item: FirebaseFirestore.QueryDocumentSnapshot<FirebaseFirestore.DocumentData>,
  maps: IdentityMaps
): AdminReferralRow {
  const raw = asRecord(item.data());
  const referrerId = firstNonEmptyString(raw.referrerId, raw.referrerUserId);
  const referredUserId = firstNonEmptyString(raw.referredUserId, raw.referredId);
  const referrer = identityForUser(referrerId, maps);
  const referred = identityForUser(referredUserId, maps);
  const referredStats = maps.referralStats.get(referredUserId) ?? {};
  const status = firstNonEmptyString(raw.status, "PENDING").toUpperCase();

  const pendingEval = status === "PENDING"
    ? evaluatePendingReason(referredUserId, maps)
    : { pendingReason: "", isProfileComplete: true, canFix: false, missingFields: [] };

  return {
    id: item.id,
    ...raw,
    status,
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
    currentReferralCode: referred.referralCode,
    pendingReason: pendingEval.pendingReason,
    isProfileComplete: pendingEval.isProfileComplete,
    canFix: pendingEval.canFix,
    missingFields: pendingEval.missingFields
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
      rootWithdrawalsSnapshot,
      identityData
    ] = await Promise.all([
      db.collection("referrals").limit(1000).get(),
      db.collectionGroup("withdrawals").limit(500).get(),
      db.collection("withdrawal_requests").limit(500).get(),
      loadIdentityMaps(db)
    ]);

    const maps = identityData.maps;

    const referrals = referralsSnapshot.docs
      .map((item) => enrichReferralDoc(item, maps))
      .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));

    const withdrawalMap = new Map<string, AdminGenericRow>();

    // 1. Process root withdrawal_requests
    rootWithdrawalsSnapshot.docs.forEach((item) => {
      const raw = asRecord(item.data());
      const userId = firstNonEmptyString(raw.userId);
      const identity = identityForUser(userId, maps);
      const stats = maps.referralStats.get(userId) ?? {};
      withdrawalMap.set(item.id, {
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
      });
    });

    // 2. Process subcollection withdrawals
    withdrawalsSnapshot.docs.forEach((item) => {
      const raw = asRecord(item.data());
      const userId = firstNonEmptyString(raw.userId, item.ref.parent.parent?.id);
      const identity = identityForUser(userId, maps);
      const stats = maps.referralStats.get(userId) ?? {};
      const existing = withdrawalMap.get(item.id) ?? {};
      withdrawalMap.set(item.id, {
        ...existing,
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
      });
    });

    const withdrawals = Array.from(withdrawalMap.values())
      .sort((a, b) => timestampMillis(b.createdAt) - timestampMillis(a.createdAt));

    return NextResponse.json({ referrals, withdrawals });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load referrals.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}


interface MilestoneAuditResult {
  toCredit: number;
  newAwardedMilestones: number[];
  awardedNow: number[];
}

function auditUserMilestones(
  successfulReferrals: number,
  totalEarnings: number,
  signupBonusAmount: number = 0,
  existingAwardedMilestones: number[] = []
): MilestoneAuditResult {
  const milestoneList = [
    { count: 5, bonus: 50 },
    { count: 10, bonus: 100 },
    { count: 15, bonus: 150 },
    { count: 25, bonus: 250 },
    { count: 50, bonus: 500 },
    { count: 100, bonus: 1000 },
  ];

  const awarded = new Set<number>(existingAwardedMilestones || []);
  let toCredit = 0;
  const awardedNow: number[] = [];

  const baseEarnings = (successfulReferrals * 25) + signupBonusAmount;
  let extraEarnings = Math.max(0, totalEarnings - baseEarnings);

  for (const m of milestoneList) {
    if (successfulReferrals >= m.count) {
      if (awarded.has(m.count)) {
        continue;
      }
      if (extraEarnings >= m.bonus) {
        extraEarnings -= m.bonus;
        awarded.add(m.count);
      } else {
        toCredit += m.bonus;
        awarded.add(m.count);
        awardedNow.push(m.count);
      }
    }
  }

  return {
    toCredit,
    newAwardedMilestones: Array.from(awarded).sort((a, b) => a - b),
    awardedNow
  };
}

async function completePendingReferral(
  db: FirebaseFirestore.Firestore,
  referralDoc: FirebaseFirestore.DocumentSnapshot<FirebaseFirestore.DocumentData>
) {
  const referralData = asRecord(referralDoc.data());
  const referralId = referralDoc.id;
  const referrerUserId = firstNonEmptyString(referralData.referrerUserId, referralData.referrerId);
  const referredUserId = firstNonEmptyString(referralData.referredUserId, referralData.referredId);
  const referralCode = firstNonEmptyString(referralData.referralCode);

  if (!referrerUserId || !referredUserId) {
    throw new Error(`Referral ${referralId} is missing referrer or referred user ID.`);
  }

  const [referrerUserDoc, referrerStatsDoc, codeDoc] = await Promise.all([
    db.collection("users").doc(referrerUserId).get(),
    db.collection("referral_stats").doc(referrerUserId).get(),
    referralCode ? db.collection("referral_codes").doc(referralCode).get() : null
  ]);

  const referrerUserData = referrerUserDoc.data() ?? {};
  const referrerStats = referrerStatsDoc.data() ?? {};
  const currentSuccessful = readNumber(referrerStats.successfulReferrals || referrerUserData.referralStats?.successfulReferrals);
  const newSuccessfulCount = currentSuccessful + 1;
  const referrerReward = 25;
  const referredUserReward = 25;
  const currentEarnings = readNumber(referrerStats.totalEarnings || referrerUserData.referralStats?.totalEarnings);
  const signupBonus = readNumber(referrerStats.signupBonusAmount || referrerUserData.referralStats?.signupBonusAmount);
  const existingAwarded = Array.isArray(referrerStats.awardedMilestones)
    ? (referrerStats.awardedMilestones as number[])
    : Array.isArray(referrerUserData.referralStats?.awardedMilestones)
    ? (referrerUserData.referralStats.awardedMilestones as number[])
    : [];

  const audit = auditUserMilestones(newSuccessfulCount, currentEarnings, signupBonus, existingAwarded);
  const milestoneBonus = audit.toCredit;
  const totalReferrerReward = referrerReward + milestoneBonus;
  const currentBalance = readNumber(referrerStats.availableBalance || referrerUserData.referralStats?.availableBalance);
  const nextBalance = currentBalance + totalReferrerReward;
  const canWithdraw = nextBalance >= 100 || newSuccessfulCount >= 5;

  const now = FieldValue.serverTimestamp();
  const batch = db.batch();

  // 1. Mark referral as COMPLETED
  batch.update(referralDoc.ref, {
    status: "COMPLETED",
    profileCompleted: true,
    rewardAmount: referrerReward,
    bonusAmount: milestoneBonus,
    referredUserReward,
    completedAt: now,
    updatedAt: now,
    fixedByAdmin: true
  });

  // 2. Referrer stats
  const referrerStatsRef = db.collection("referral_stats").doc(referrerUserId);
  batch.set(referrerStatsRef, {
    userId: referrerUserId,
    successfulReferrals: newSuccessfulCount,
    pendingReferrals: FieldValue.increment(-1),
    totalEarnings: FieldValue.increment(totalReferrerReward),
    availableBalance: FieldValue.increment(totalReferrerReward),
    canWithdraw,
    awardedMilestones: audit.newAwardedMilestones,
    lastUpdated: now
  }, { merge: true });

  const referrerUserRef = db.collection("users").doc(referrerUserId);
  batch.set(referrerUserRef, {
    "referralStats.successfulReferrals": newSuccessfulCount,
    "referralStats.pendingReferrals": FieldValue.increment(-1),
    "referralStats.totalEarnings": FieldValue.increment(totalReferrerReward),
    "referralStats.availableBalance": FieldValue.increment(totalReferrerReward),
    "referralStats.canWithdraw": canWithdraw,
    "referralStats.awardedMilestones": audit.newAwardedMilestones,
    "referralStats.lastUpdated": now
  }, { merge: true });

  // 3. Referred user stats & profileCompleted flag
  const referredStatsRef = db.collection("referral_stats").doc(referredUserId);
  batch.set(referredStatsRef, {
    userId: referredUserId,
    totalEarnings: FieldValue.increment(referredUserReward),
    availableBalance: FieldValue.increment(referredUserReward),
    signupBonusReceived: true,
    signupBonusAmount: referredUserReward,
    lastUpdated: now
  }, { merge: true });

  const referredUserRef = db.collection("users").doc(referredUserId);
  batch.set(referredUserRef, {
    profileCompleted: true,
    isProfileComplete: true,
    "referralStats.totalEarnings": FieldValue.increment(referredUserReward),
    "referralStats.availableBalance": FieldValue.increment(referredUserReward),
    "referralStats.signupBonusReceived": true,
    "referralStats.signupBonusAmount": referredUserReward,
    "referralStats.lastUpdated": now
  }, { merge: true });

  // 4. Update referral code count if exists
  if (codeDoc && codeDoc.exists) {
    batch.update(codeDoc.ref, {
      successfulReferrals: FieldValue.increment(1)
    });
  }

  // 5. Audit log
  const auditRef = db.collection("referral_events").doc();
  batch.set(auditRef, {
    eventType: "ADMIN_FIXED_PENDING_REFERRAL",
    referralId,
    referrerUserId,
    referredUserId,
    referrerReward: totalReferrerReward,
    referredReward: referredUserReward,
    timestamp: now
  });

  await batch.commit();

  return {
    referralId,
    referrerUserId,
    referredUserId,
    totalReferrerReward,
    referredUserReward
  };
}

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const body = (await request.json()) as { action?: string; referralCode?: string; referralId?: string };

    if (body.action === "fix-pending-referral") {
      const referralId = body.referralId?.trim();
      if (!referralId) {
        return NextResponse.json({ error: "referralId is required." }, { status: 400 });
      }

      const db = getFirebaseAdminDb();
      const referralDoc = await db.collection("referrals").doc(referralId).get();
      if (!referralDoc.exists) {
        return NextResponse.json({ error: "Referral not found." }, { status: 404 });
      }

      const result = await completePendingReferral(db, referralDoc);
      return NextResponse.json({ ok: true, message: "Referral fixed and credited successfully.", result });
    }

    if (body.action === "sync-all-pending") {
      const db = getFirebaseAdminDb();
      const pendingSnapshot = await db.collection("referrals")
        .where("status", "==", "PENDING")
        .limit(200)
        .get();

      const identityData = await loadIdentityMaps(db);
      const maps = identityData.maps;

      const fixed: Array<any> = [];
      const skipped: Array<any> = [];

      for (const doc of pendingSnapshot.docs) {
        const refData = doc.data();
        const referredUserId = firstNonEmptyString(refData.referredUserId, refData.referredId);
        const evalResult = evaluatePendingReason(referredUserId, maps);

        if (evalResult.isProfileComplete) {
          try {
            const outcome = await completePendingReferral(db, doc);
            fixed.push(outcome);
          } catch (e) {
            skipped.push({ referralId: doc.id, error: e instanceof Error ? e.message : "Failed" });
          }
        } else {
          skipped.push({ referralId: doc.id, reason: evalResult.pendingReason });
        }
      }

      let milestonesFixed = 0;
      try {
        const statsSnapshot = await db.collection("referral_stats")
          .where("successfulReferrals", ">=", 5)
          .limit(200)
          .get();

        for (const statDoc of statsSnapshot.docs) {
          const sData = statDoc.data();
          const sUserId = statDoc.id;
          const sSuccessful = readNumber(sData.successfulReferrals);
          const sEarnings = readNumber(sData.totalEarnings);
          const sSignup = readNumber(sData.signupBonusAmount);
          const sAwarded = Array.isArray(sData.awardedMilestones) ? (sData.awardedMilestones as number[]) : [];
          const audit = auditUserMilestones(sSuccessful, sEarnings, sSignup, sAwarded);

          if (audit.toCredit > 0 || audit.newAwardedMilestones.length !== sAwarded.length) {
            milestonesFixed++;
            const bonusToCredit = audit.toCredit;
            const now = FieldValue.serverTimestamp();
            const sRef = db.collection("referral_stats").doc(sUserId);
            const uRef = db.collection("users").doc(sUserId);
            const b = db.batch();

            const sPatch: Record<string, any> = {
              awardedMilestones: audit.newAwardedMilestones,
              lastUpdated: now
            };
            const uPatch: Record<string, any> = {
              "referralStats.awardedMilestones": audit.newAwardedMilestones,
              "referralStats.lastUpdated": now
            };

            if (bonusToCredit > 0) {
              sPatch.totalEarnings = FieldValue.increment(bonusToCredit);
              sPatch.availableBalance = FieldValue.increment(bonusToCredit);
              sPatch.canWithdraw = true;
              uPatch["referralStats.totalEarnings"] = FieldValue.increment(bonusToCredit);
              uPatch["referralStats.availableBalance"] = FieldValue.increment(bonusToCredit);
              uPatch["referralStats.canWithdraw"] = true;

              const notifRef = db.collection("notifications").doc();
              b.set(notifRef, {
                recipientId: sUserId,
                title: "🎉 Milestone Bonus Credited!",
                message: `Your ₹${bonusToCredit} milestone bonus for reaching ${sSuccessful} referrals has been credited to your balance!`,
                type: "MILESTONE_REWARD",
                data: { milestones: audit.awardedNow, amount: bonusToCredit },
                createdAt: now,
                isRead: false
              });

              const evRef = db.collection("referral_events").doc();
              b.set(evRef, {
                eventType: "MILESTONE_REACHED",
                userId: sUserId,
                milestones: audit.awardedNow,
                bonusAmount: bonusToCredit,
                newSuccessfulCount: sSuccessful,
                timestamp: now
              });
            }

            b.set(sRef, sPatch, { merge: true });
            b.set(uRef, uPatch, { merge: true });
            await b.commit();
          }
        }
      } catch (err) {
        console.error("Error in milestone audit during sync-all-pending:", err);
      }

      return NextResponse.json({
        ok: true,
        message: `Processed ${pendingSnapshot.size} pending referrals: ${fixed.length} fixed, ${skipped.length} incomplete. Also audited milestones (${milestonesFixed} users credited).`,
        fixedCount: fixed.length,
        skippedCount: skipped.length,
        milestonesFixed,
        fixed,
        skipped
      });
    }

    if (body.action === "audit-and-credit-milestones") {
      const db = getFirebaseAdminDb();
      const statsSnapshot = await db.collection("referral_stats")
        .where("successfulReferrals", ">=", 5)
        .limit(200)
        .get();

      let creditedCount = 0;
      let totalBonusCredited = 0;
      const creditedUsers: Array<{ userId: string; bonusCredited: number; milestones: number[] }> = [];

      for (const statDoc of statsSnapshot.docs) {
        const sData = statDoc.data();
        const sUserId = statDoc.id;
        const sSuccessful = readNumber(sData.successfulReferrals);
        const sEarnings = readNumber(sData.totalEarnings);
        const sSignup = readNumber(sData.signupBonusAmount);
        const sAwarded = Array.isArray(sData.awardedMilestones) ? (sData.awardedMilestones as number[]) : [];
        const audit = auditUserMilestones(sSuccessful, sEarnings, sSignup, sAwarded);

        if (audit.toCredit > 0 || audit.newAwardedMilestones.length !== sAwarded.length) {
          const bonusToCredit = audit.toCredit;
          const now = FieldValue.serverTimestamp();
          const sRef = db.collection("referral_stats").doc(sUserId);
          const uRef = db.collection("users").doc(sUserId);
          const b = db.batch();

          const sPatch: Record<string, any> = {
            awardedMilestones: audit.newAwardedMilestones,
            lastUpdated: now
          };
          const uPatch: Record<string, any> = {
            "referralStats.awardedMilestones": audit.newAwardedMilestones,
            "referralStats.lastUpdated": now
          };

          if (bonusToCredit > 0) {
            creditedCount++;
            totalBonusCredited += bonusToCredit;
            creditedUsers.push({ userId: sUserId, bonusCredited: bonusToCredit, milestones: audit.awardedNow });

            sPatch.totalEarnings = FieldValue.increment(bonusToCredit);
            sPatch.availableBalance = FieldValue.increment(bonusToCredit);
            sPatch.canWithdraw = true;
            uPatch["referralStats.totalEarnings"] = FieldValue.increment(bonusToCredit);
            uPatch["referralStats.availableBalance"] = FieldValue.increment(bonusToCredit);
            uPatch["referralStats.canWithdraw"] = true;

            const notifRef = db.collection("notifications").doc();
            b.set(notifRef, {
              recipientId: sUserId,
              title: "🎉 Milestone Bonus Credited!",
              message: `Your ₹${bonusToCredit} milestone bonus for reaching ${sSuccessful} referrals has been credited to your balance!`,
              type: "MILESTONE_REWARD",
              data: { milestones: audit.awardedNow, amount: bonusToCredit },
              createdAt: now,
              isRead: false
            });

            const evRef = db.collection("referral_events").doc();
            b.set(evRef, {
              eventType: "MILESTONE_REACHED",
              userId: sUserId,
              milestones: audit.awardedNow,
              bonusAmount: bonusToCredit,
              newSuccessfulCount: sSuccessful,
              timestamp: now
            });
          }

          b.set(sRef, sPatch, { merge: true });
          b.set(uRef, uPatch, { merge: true });
          await b.commit();
        }
      }

      return NextResponse.json({
        ok: true,
        message: `Audited ${statsSnapshot.size} referrers: credited missing milestone bonuses to ${creditedCount} users (Total: ₹${totalBonusCredited}).`,
        totalAudited: statsSnapshot.size,
        creditedCount,
        totalBonusCredited,
        creditedUsers
      });
    }

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
    const rootWithdrawalRef = db.collection("withdrawal_requests").doc(withdrawalId);
    const auditRef = statsRef.collection("audit_logs").doc();
    const userRef = db.collection("users").doc(userId);

    await db.runTransaction(async (transaction) => {
      const [withdrawalDoc, rootWithdrawalDoc, statsDoc, userDoc] = await Promise.all([
        transaction.get(withdrawalRef),
        transaction.get(rootWithdrawalRef),
        transaction.get(statsRef),
        transaction.get(userRef)
      ]);

      if (!withdrawalDoc.exists && !rootWithdrawalDoc.exists) {
        throw new Error(`Withdrawal ${withdrawalId} not found for user ${userId}.`);
      }

      const now = new Date();
      const current = withdrawalDoc.exists ? (withdrawalDoc.data() ?? {}) : (rootWithdrawalDoc.data() ?? {});
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
        transaction.set(statsRef, {
          lastWithdrawalStatus: "PROCESSING",
          lastUpdated: now
        }, { merge: true });
        if (userDoc.exists) {
          transaction.set(userRef, {
            referralStats: {
              lastWithdrawalStatus: "PROCESSING",
              lastUpdated: now
            }
          }, { merge: true });
        }
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

        if (userDoc.exists) {
          transaction.set(userRef, {
            referralStats: {
              ...statsUpdate,
              lastWithdrawalStatus: "COMPLETED"
            }
          }, { merge: true });
        }
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
          const statsRefundUpdate = {
            availableBalance: FieldValue.increment(amount),
            withdrawnAmount: FieldValue.increment(-amount),
            canWithdraw: currentAvailableBalance + amount >= minWithdrawal,
            lastFailedWithdrawalAt: now,
            lastUpdated: now
          };
          transaction.set(statsRef, statsRefundUpdate, { merge: true });

          if (userDoc.exists) {
            transaction.set(userRef, {
              referralStats: {
                ...statsRefundUpdate,
                lastWithdrawalStatus: "FAILED"
              }
            }, { merge: true });
          }
        }
      }

      if (adminNote && status !== "FAILED") {
        update.adminNote = adminNote;
      }

      transaction.set(withdrawalRef, update, { merge: true });
      transaction.set(rootWithdrawalRef, update, { merge: true });
      transaction.set(auditRef, {
        eventType: `WITHDRAWAL_${status}`,
        userId,
        withdrawalId,
        amount,
        transactionId: transactionId || null,
        adminNote: adminNote || null,
        timestamp: now
      });

      // Send in-app notification to user
      const notifRef = db.collection("notifications").doc();
      const notifTitle = status === "COMPLETED"
        ? `₹${amount} Withdrawal Paid!`
        : status === "PROCESSING"
          ? "Withdrawal Approved"
          : "Withdrawal Refunded";
      const notifMessage = status === "COMPLETED"
        ? `Your withdrawal of ₹${amount} has been paid successfully.${transactionId ? ` Ref: ${transactionId}` : ""}`
        : status === "PROCESSING"
          ? `Your withdrawal request of ₹${amount} has been approved and is being transferred.`
          : `Your withdrawal of ₹${amount} failed.${adminNote ? ` Reason: ${adminNote}.` : ""} Amount has been refunded to your wallet.`;

      transaction.set(notifRef, {
        recipientId: userId,
        title: notifTitle,
        message: notifMessage,
        type: `WITHDRAWAL_${status}`,
        isRead: false,
        createdAt: now
      });
    });

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update withdrawal.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
