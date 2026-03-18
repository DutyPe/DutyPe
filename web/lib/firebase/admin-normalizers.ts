import { readTimestamp } from "@/lib/firebase/firestore-helpers";

type AnyRecord = Record<string, unknown>;

function pickFirstNonEmptyString(...values: unknown[]) {
  for (const value of values) {
    if (typeof value === "string" && value.trim()) {
      return value.trim();
    }
  }

  return "";
}

function normalizeRoleText(value: string) {
  const upper = value.trim().toUpperCase();

  if (upper === "WORKER" || upper === "EMPLOYER" || upper === "ADMIN") {
    return upper;
  }

  return "";
}

export type NormalizedUser = {
  id: string;
  fullName: string;
  phone: string;
  email: string;
  referralCode: string;
  role: string;
  activeRole: string;
  roles: string[];
  joinedAt: unknown;
  source: {
    name: string;
    phone: string;
    email: string;
    referralCode: string;
    joinedAt: string;
  };
};

export function normalizeUserRecord(
  id: string,
  user: AnyRecord,
  extras?: {
    workerProfile?: AnyRecord | null;
    employerProfile?: AnyRecord | null;
    referralCodeByUserId?: string;
  }
): NormalizedUser {
  const workerProfile = extras?.workerProfile ?? null;
  const employerProfile = extras?.employerProfile ?? null;

  const fullName = pickFirstNonEmptyString(
    user.fullName,
    user.name,
    user.displayName,
    workerProfile?.fullName,
    workerProfile?.name,
    employerProfile?.fullName,
    employerProfile?.name,
    user.email,
    user.phone
  );

  const phone = pickFirstNonEmptyString(
    user.phone,
    user.phoneNumber,
    user.contactPhone,
    workerProfile?.phone,
    workerProfile?.phoneNumber,
    workerProfile?.contactPhone,
    employerProfile?.phone,
    employerProfile?.phoneNumber,
    employerProfile?.contactPhone
  );

  const email = pickFirstNonEmptyString(
    user.email,
    user.contactEmail,
    workerProfile?.email,
    workerProfile?.contactEmail,
    employerProfile?.email,
    employerProfile?.contactEmail
  );

  const referralCode = pickFirstNonEmptyString(
    user.referralCode,
    extras?.referralCodeByUserId,
    user.referral_code
  );

  const collectedRoles = [
    normalizeRoleText(String(user.activeRole ?? "")),
    normalizeRoleText(String(user.role ?? "")),
    ...(Array.isArray(user.roles) ? user.roles.map((entry) => normalizeRoleText(String(entry))) : [])
  ].filter(Boolean);

  const roles = [...new Set(collectedRoles)];

  const activeRole =
    normalizeRoleText(String(user.activeRole ?? "")) ||
    normalizeRoleText(String(user.role ?? "")) ||
    roles[0] ||
    "WORKER";

  const role = normalizeRoleText(String(user.role ?? "")) || activeRole;

  const joinedCandidates: Array<[string, unknown]> = [
    ["createdAt", user.createdAt],
    ["joinedAt", user.joinedAt],
    ["registeredAt", user.registeredAt],
    ["workerProfile.createdAt", workerProfile?.createdAt],
    ["employerProfile.createdAt", employerProfile?.createdAt],
    ["updatedAt", user.updatedAt],
    ["lastLoginAt", user.lastLoginAt]
  ];

  let joinedAt: unknown = null;
  let joinedSource = "";

  for (const [source, value] of joinedCandidates) {
    if (readTimestamp(value)) {
      joinedAt = value;
      joinedSource = source;
      break;
    }
  }

  return {
    id,
    fullName,
    phone,
    email,
    referralCode,
    role,
    activeRole,
    roles: roles.length > 0 ? roles : [activeRole],
    joinedAt,
    source: {
      name: fullName ? "resolved" : "missing",
      phone: phone ? "resolved" : "missing",
      email: email ? "resolved" : "missing",
      referralCode: referralCode ? "resolved" : "missing",
      joinedAt: joinedSource || "missing"
    }
  };
}

export type NormalizedApplication = {
  id: string;
  workerId: string;
  workerName: string;
  workerPhone: string;
  workerEmail: string;
  jobTitle: string;
  status: string;
  appliedAt: unknown;
};

function normalizeApplicationStatusText(value: unknown): string {
  const candidate = pickFirstNonEmptyString(value).toLowerCase();

  switch (candidate) {
    case "applied":
    case "pending":
      return "PENDING";
    case "under_review":
    case "under review":
      return "UNDER_REVIEW";
    case "accepted":
    case "shortlisted":
      return "ACCEPTED";
    case "in_progress":
    case "in progress":
      return "IN_PROGRESS";
    case "rejected":
      return "REJECTED";
    case "completed":
    case "hired":
      return "COMPLETED";
    case "withdrawn":
      return "WITHDRAWN";
    default:
      return "PENDING";
  }
}

export function toCanonicalApplicationStatus(value: string): string {
  const normalized = normalizeApplicationStatusText(value);

  switch (normalized) {
    case "PENDING":
      return "applied";
    case "UNDER_REVIEW":
      return "under_review";
    case "ACCEPTED":
      return "accepted";
    case "IN_PROGRESS":
      return "in_progress";
    case "REJECTED":
      return "rejected";
    case "COMPLETED":
      return "completed";
    case "WITHDRAWN":
      return "withdrawn";
    default:
      return "applied";
  }
}

export function normalizeApplicationRecord(id: string, application: AnyRecord, worker?: NormalizedUser) {
  const workerName = pickFirstNonEmptyString(
    application.workerName,
    application.applicantName,
    worker?.fullName,
    application.workerId
  );

  const workerPhone = pickFirstNonEmptyString(
    application.workerPhone,
    worker?.phone
  );

  const workerEmail = pickFirstNonEmptyString(
    application.workerEmail,
    worker?.email
  );

  const jobTitle = pickFirstNonEmptyString(application.jobTitle, application.title);

  const status = normalizeApplicationStatusText(application.status);

  return {
    id,
    workerId: pickFirstNonEmptyString(application.workerId),
    workerName,
    workerPhone,
    workerEmail,
    jobTitle,
    status,
    appliedAt: application.appliedAt ?? application.createdAt ?? application.updatedAt
  } satisfies NormalizedApplication;
}
