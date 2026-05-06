import "server-only";

import type { Auth, UserRecord } from "firebase-admin/auth";
import type { DocumentData, QuerySnapshot } from "firebase-admin/firestore";

type AnyRecord = Record<string, unknown>;

type ProfileKind = "WORKER" | "EMPLOYER";

type PhoneRoleRecord = AnyRecord & { docId: string };

export function asRecord(value: unknown) {
  return (value ?? {}) as AnyRecord;
}

export function firstNonEmptyString(...values: unknown[]) {
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

function hasValue(value: unknown) {
  if (value === null || value === undefined) return false;
  if (typeof value === "string") return Boolean(value.trim());
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "object") return Object.keys(value as AnyRecord).length > 0;
  return true;
}

function hasLatLng(value: unknown) {
  if (!value || typeof value !== "object") return false;
  const location = value as AnyRecord;
  return typeof location.lat === "number" && typeof location.lng === "number";
}

function normalizeRole(value: unknown) {
  const role = firstNonEmptyString(value).toUpperCase();
  return role === "WORKER" || role === "EMPLOYER" || role === "ADMIN" ? role : "";
}

export async function listAuthUsersById(auth: Auth) {
  const authUsersById = new Map<string, UserRecord>();
  let pageToken: string | undefined;

  do {
    const page = await auth.listUsers(1000, pageToken);
    page.users.forEach((user) => authUsersById.set(user.uid, user));
    pageToken = page.pageToken;
  } while (pageToken);

  return authUsersById;
}

export function buildRecordMap(snapshot: QuerySnapshot<DocumentData>) {
  const map = new Map<string, AnyRecord>();
  snapshot.forEach((item) => map.set(item.id, asRecord(item.data())));
  return map;
}

export function buildPhoneRoleMap(snapshot: QuerySnapshot<DocumentData>) {
  const map = new Map<string, PhoneRoleRecord>();
  snapshot.forEach((item) => {
    const data = asRecord(item.data());
    const uid = firstNonEmptyString(data.uid);
    if (uid) {
      map.set(uid, { ...data, docId: item.id });
    }
  });
  return map;
}

export function buildReferralCodeByUserId(snapshot: QuerySnapshot<DocumentData>) {
  const map = new Map<string, string>();
  snapshot.forEach((item) => {
    const data = asRecord(item.data());
    const userId = firstNonEmptyString(data.userId);
    const code = firstNonEmptyString(data.code, item.id);
    if (userId && code) {
      map.set(userId, code);
    }
  });
  return map;
}

function missingWorkerFields(profile: AnyRecord) {
  const missing: string[] = [];
  if (!hasValue(profile.fullName)) missing.push("fullName");
  if (!hasValue(profile.phone)) missing.push("phone");
  if (normalizeRole(profile.role) !== "WORKER") missing.push("role=WORKER");
  if (!hasValue(profile.skills)) missing.push("skills");
  if (!hasValue(profile.dateOfBirth)) missing.push("dateOfBirth");
  if (!hasValue(profile.gender)) missing.push("gender");
  if (!hasValue(profile.experience)) missing.push("experience");
  if (!hasLatLng(profile.location)) missing.push("location.lat/lng");
  if (!hasValue(profile.geohash)) missing.push("geohash");
  return missing;
}

function missingEmployerFields(profile: AnyRecord) {
  const missing: string[] = [];
  if (!hasValue(profile.fullName)) missing.push("fullName");
  if (!hasValue(profile.phone)) missing.push("phone");
  if (normalizeRole(profile.role) !== "EMPLOYER") missing.push("role=EMPLOYER");
  if (!hasValue(profile.companyName)) missing.push("companyName");
  if (!hasValue(profile.industry)) missing.push("industry");
  if (!hasValue(profile.businessAddress)) missing.push("businessAddress");
  if (!hasLatLng(profile.businessLocation)) missing.push("businessLocation.lat/lng");
  if (!hasValue(profile.geohash)) missing.push("geohash");
  return missing;
}

export function buildProfileAdminRow({
  collectionName,
  expectedRole,
  userId,
  profile,
  phoneRole,
  referralStats,
  referralCode,
  authUser
}: {
  collectionName: "worker_profiles" | "employer_profiles";
  expectedRole: ProfileKind;
  userId: string;
  profile: AnyRecord;
  phoneRole?: PhoneRoleRecord;
  referralStats?: AnyRecord;
  referralCode?: string;
  authUser?: UserRecord;
}) {
  const profileFieldNames = Object.keys(profile).sort((a, b) => a.localeCompare(b));
  const profileRole = normalizeRole(profile.role);
  const phoneRoleRole = normalizeRole(phoneRole?.role);
  const missingProfileFields = expectedRole === "WORKER"
    ? missingWorkerFields(profile)
    : missingEmployerFields(profile);
  const roleStatus = !phoneRoleRole
    ? "missing-phoneRoles"
    : phoneRoleRole === expectedRole && (!profileRole || profileRole === expectedRole)
      ? "aligned"
      : "role-mismatch";

  const resolvedName = expectedRole === "WORKER"
    ? firstNonEmptyString(profile.fullName, profile.name, phoneRole?.name, authUser?.displayName)
    : firstNonEmptyString(profile.companyName, profile.fullName, phoneRole?.name, authUser?.displayName);

  const resolvedPhone = firstNonEmptyString(
    profile.phone,
    profile.phoneNumber,
    phoneRole?.phoneNumber,
    authUser?.phoneNumber
  );

  return {
    id: userId,
    ...profile,
    firebaseCollection: collectionName,
    firebaseDocumentId: userId,
    firebasePath: `${collectionName}/${userId}`,
    expectedRole,
    resolvedName,
    resolvedPhone,
    profileRole: profileRole || "missing",
    phoneRoleRole: phoneRoleRole || "missing",
    roleStatus,
    profileHealth: missingProfileFields.length === 0 ? "complete" : "needs-review",
    missingProfileFields,
    profileFieldCount: profileFieldNames.length,
    profileFieldNames,
    hasAuthUser: Boolean(authUser),
    authPhoneNumber: authUser?.phoneNumber ?? "",
    authEmail: authUser?.email ?? "",
    authDisplayName: authUser?.displayName ?? "",
    authDisabled: authUser?.disabled ?? false,
    hasPhoneRole: Boolean(phoneRole),
    phoneRoleDocId: phoneRole?.docId ?? "",
    phoneRoleUid: phoneRole?.uid ?? "",
    phoneRoleName: phoneRole?.name ?? "",
    phoneRolePhoneNumber: phoneRole?.phoneNumber ?? "",
    hasReferralStats: Boolean(referralStats),
    referralCode: firstNonEmptyString(profile.referralCode, referralStats?.referralCode, referralCode),
    referredByCode: firstNonEmptyString(profile.referredByCode, referralStats?.referredByCode),
    referredByUserId: firstNonEmptyString(profile.referredByUserId, referralStats?.referredByUserId),
    referralStatsAvailableBalance: readNumber(referralStats?.availableBalance),
    referralStatsTotalEarnings: readNumber(referralStats?.totalEarnings),
    referralStatsSuccessfulReferrals: readNumber(referralStats?.successfulReferrals),
    __firebase: {
      sourcePath: `${collectionName}/${userId}`,
      exactProfileData: profile,
      phoneRolesData: phoneRole ?? null,
      referralStatsData: referralStats ?? null,
      authUserData: authUser
        ? {
            uid: authUser.uid,
            phoneNumber: authUser.phoneNumber ?? null,
            email: authUser.email ?? null,
            displayName: authUser.displayName ?? null,
            disabled: authUser.disabled,
            metadata: authUser.metadata
          }
        : null
    }
  };
}