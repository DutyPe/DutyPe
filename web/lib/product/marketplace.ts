// ============================================================
// STRICT SCHEMA WITH UI COMPATIBILITY LAYER
// Database: ONLY exact fields specified
// UI: Can use computed fields for backward compat
// ============================================================

import { readTimestamp } from "@/lib/firebase/firestore-helpers";

// ============================================================
// STRICT TYPES
// ============================================================

export type ProductApplicationStatus =
  | "applied"
  | "PENDING"
  | "under_review"
  | "UNDER_REVIEW"
  | "accepted"
  | "ACCEPTED"
  | "in_progress"
  | "IN_PROGRESS"
  | "rejected"
  | "REJECTED"
  | "completed"
  | "COMPLETED"
  | "withdrawn"
  | "WITHDRAWN";

export type ProductJob = {
  id: string;
  jobId: string;
  employerId: string;
  title: string;
  jobType: string;
  salary: number;
  salaryType: string;
  location: { lat: number; lng: number };
  geohash: string;
  status: "open" | "closed" | "expired";
  createdAt: unknown;
  expiresAt: unknown;
  // Computed/optional UI fields
  description?: string;
  contactNumber?: string;
  addressText?: string;
  payAmount?: string;
  payType?: string;
  category?: string;
  isActive?: boolean;
  isFilled?: boolean;
  latitude?: number;
  longitude?: number;
  companyName?: string;
  shiftTiming?: string;
  vacancies?: number;
  applicationCount?: number;
  employerTrustTier?: string;
  acceptedCount?: number;
  gender?: string;
  postedAt?: unknown;
  updatedAt?: unknown;
  vacancyStatus?: string;
};

export type ProductApplication = {
  id: string;
  applicationId: string;
  jobId: string;
  workerId: string;
  employerId: string;
  status: ProductApplicationStatus;
  createdAt: unknown;
  // Optional UI fields
  workerName?: string;
  companyName?: string;
  jobTitle?: string;
  jobLocation?: string;
  coverLetter?: string;
  appliedAt?: unknown;
  source?: string;
  statusHistory?: ProductApplicationStatusHistoryEntry[];
};

export type ProductApplicationStatusHistoryEntry = {
  notes?: string;
  status: ProductApplicationStatus | string;
  systemUpdate?: boolean;
  timestamp?: unknown;
  updatedAt?: unknown;
  updatedBy?: string;
  [key: string]: unknown;
};

// ============================================================
// CONVERSION FUNCTIONS
// ============================================================

export function normalizeProductJob(
  id: string,
  data: Record<string, unknown>
): ProductJob {
  const location = data.location as Record<string, unknown> | undefined;
  const lat = Number(location?.lat ?? 0);
  const lng = Number(location?.lng ?? 0);
  const salary = Number(data.salary ?? 0);
  const salaryType = String(data.salaryType ?? "MONTHLY");
  const jobType = String(data.jobType ?? "");
  const status = String(data.status ?? "open") as "open" | "closed" | "expired";

  return {
    id,
    jobId: String(data.jobId ?? id),
    employerId: String(data.employerId ?? ""),
    title: String(data.title ?? ""),
    jobType,
    salary,
    salaryType,
    location: { lat, lng },
    geohash: String(data.geohash ?? ""),
    status,
    createdAt: data.createdAt,
    expiresAt: data.expiresAt,
    // Computed fields for UI
    description: String(data.description ?? ""),
    contactNumber: String(data.contactNumber ?? ""),
    addressText: String(data.addressText ?? ""),
    payAmount: String(salary),
    payType: salaryType,
    category: jobType,
    isActive: status === "open",
    isFilled: status === "closed",
    latitude: lat,
    longitude: lng,
  };
}

export function normalizeProductApplication(
  id: string,
  data: Record<string, unknown>
): ProductApplication {
  const statusHistory = Array.isArray(data.statusHistory)
    ? data.statusHistory
        .filter((entry): entry is Record<string, unknown> => entry !== null && typeof entry === "object")
        .map((entry) => ({
          ...entry,
          notes: typeof entry.notes === "string" ? entry.notes : "",
          status: String(entry.status ?? "PENDING"),
        }))
    : [];

  return {
    id,
    applicationId: String(data.applicationId ?? id),
    jobId: String(data.jobId ?? ""),
    workerId: String(data.workerId ?? ""),
    employerId: String(data.employerId ?? ""),
    status: (String(data.status ?? "applied") as ProductApplicationStatus),
    createdAt: data.createdAt,
    workerName: String(data.workerName ?? ""),
    companyName: String(data.companyName ?? ""),
    jobTitle: String(data.jobTitle ?? ""),
    jobLocation: String(data.jobLocation ?? ""),
    coverLetter: String(data.coverLetter ?? ""),
    appliedAt: data.appliedAt ?? data.createdAt,
    source: String(data.source ?? "WEB_PORTAL"),
    statusHistory,
  };
}

// ============================================================
// QUERY HELPERS
// ============================================================

export function isJobAvailable(job: ProductJob): boolean {
  if (job.status !== "open") return false;
  const expiry = readTimestamp(job.expiresAt);
  return !expiry || expiry.getTime() > Date.now();
}

export function isApplicationActive(app: ProductApplication): boolean {
  const normalized = String(app.status).toLowerCase();
  return (
    normalized !== "rejected" &&
    normalized !== "withdrawn" &&
    normalized !== "completed"
  );
}

export function sortByTimestampDesc<T extends Record<string, unknown>>(
  items: T[],
  timestampKey: keyof T = "createdAt" as keyof T
): T[] {
  return [...items].sort((a, b) => {
    const aTime = readTimestamp(a[timestampKey])?.getTime() ?? 0;
    const bTime = readTimestamp(b[timestampKey])?.getTime() ?? 0;
    return bTime - aTime;
  });
}

export function canEditEmployerJob(job: ProductJob, now = Date.now()): boolean {
  const postedAt = readTimestamp(job.createdAt)?.getTime() ?? 0;
  const EDIT_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
  return postedAt > 0 && now - postedAt <= EDIT_WINDOW_MS;
}

export function employerJobEditRestrictionMessage(
  job: ProductJob,
  now = Date.now()
): string {
  if (canEditEmployerJob(job, now)) {
    return "";
  }

  const postedAt = readTimestamp(job.createdAt)?.getTime() ?? 0;
  if (!postedAt) {
    return "This job cannot be edited because the original post time is unavailable.";
  }

  const elapsedDays = Math.max(1, Math.floor((now - postedAt) / (24 * 60 * 60 * 1000)));
  return `Jobs can only be edited within 7 days of posting. This job was posted ${elapsedDays} days ago.`;
}

// ============================================================
// STATUS HELPERS
// ============================================================

export function toStorageApplicationStatus(status: ProductApplicationStatus): string {
  return status;
}

export function normalizeApplicationStatus(value: unknown): ProductApplicationStatus {
  const statusStr = String(value ?? "applied").toLowerCase();
  const validStatuses: ProductApplicationStatus[] = [
    "applied",
    "under_review",
    "accepted",
    "in_progress",
    "rejected",
    "completed",
    "withdrawn",
  ];
  return validStatuses.includes(statusStr as ProductApplicationStatus)
    ? (statusStr as ProductApplicationStatus)
    : "applied";
}

export function productStatusLabel(status: string): string {
  const labels: Record<string, string> = {
    applied: "Applied",
    under_review: "Under Review",
    accepted: "Accepted",
    in_progress: "In Progress",
    rejected: "Rejected",
    completed: "Completed",
    withdrawn: "Withdrawn",
  };
  return labels[status] ?? status;
}

export function productStatusTone(status: string): string {
  const tones: Record<string, string> = {
    applied: "default",
    under_review: "info",
    accepted: "success",
    in_progress: "processing",
    rejected: "critical",
    completed: "success",
    withdrawn: "critical",
  };
  return tones[status] ?? "default";
}

export function canEmployerAcceptOrReject(appOrStatus: ProductApplication | string): boolean {
  const status = typeof appOrStatus === "string" ? appOrStatus : appOrStatus.status;
  return String(status).toLowerCase() === "under_review";
}

export function canEmployerMoveToUnderReview(appOrStatus: ProductApplication | string): boolean {
  const status = typeof appOrStatus === "string" ? appOrStatus : appOrStatus.status;
  return String(status).toLowerCase() === "applied";
}

export function canEmployerVerifyWork(status: string): boolean {
  return status === "accepted" || status === "in_progress";
}

export function canEmployerMarkWorkComplete(status: string): boolean {
  return status === "in_progress" || status === "accepted";
}

export function canEmployerRateWorker(status: string): boolean {
  return status === "completed";
}

export function defaultWorkerName(profile: { fullName?: string; name?: string; phone?: string } | null | undefined): string {
  const name = profile?.fullName?.trim() || profile?.name?.trim();
  if (name) {
    return name;
  }

  const phone = profile?.phone?.trim();
  if (phone) {
    return phone;
  }

  return "Worker";
}

// ============================================================
// PROFILE HELPERS
// ============================================================

export function workerProfileCompletion(profile: any): number {
  let score = 0;
  if (profile?.fullName?.trim()) score += 15;
  if (profile?.email?.trim()) score += 10;
  if (profile?.phone?.trim()) score += 10;
  if (profile?.address?.trim()) score += 20;
  if (profile?.skills?.trim()) score += 15;
  if (profile?.experience?.trim()) score += 15;
  if (profile?.gender?.trim()) score += 5;
  if (profile?.dateOfBirth?.trim()) score += 5;
  if (profile?.profileImageUrl?.trim()) score += 5;
  return Math.min(score, 100);
}

export function employerProfileCompletion(profile: any): number {
  let score = 0;
  if (profile?.companyName?.trim()) score += 20;
  if (profile?.industry?.trim()) score += 15;
  if (profile?.phone?.trim()) score += 15;
  if (profile?.address?.trim()) score += 20;
  if (profile?.gender?.trim()) score += 10;
  if (profile?.dateOfBirth?.trim()) score += 10;
  if (profile?.email?.trim()) score += 5;
  if (profile?.companySize?.trim()) score += 5;
  return Math.min(score, 100);
}

export function missingWorkerFields(profile: any): string[] {
  const missing: string[] = [];
  if (!profile?.fullName?.trim()) missing.push("Full name");
  if (!profile?.phone?.trim()) missing.push("Phone");
  if (!profile?.address?.trim()) missing.push("Address");
  if (!profile?.skills?.trim()) missing.push("Skills");
  if (!profile?.experience?.trim()) missing.push("Experience");
  return missing;
}

export function missingEmployerFields(profile: any): string[] {
  const missing: string[] = [];
  if (!profile?.companyName?.trim()) missing.push("Company name");
  if (!profile?.industry?.trim()) missing.push("Industry");
  if (!profile?.phone?.trim()) missing.push("Contact phone");
  if (!profile?.address?.trim()) missing.push("Business address");
  return missing;
}

// ============================================================
// DEFAULTS
// ============================================================

export const defaultCompanyName = "DutyPe Employer";

// ============================================================
// LEGACY ALIASES (For migration)
// ============================================================

export { isJobAvailable as isLiveJob };
