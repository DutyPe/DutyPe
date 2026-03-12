import { readTimestamp } from "@/lib/firebase/firestore-helpers";

import { displayProfileName, type ProductUserProfile } from "./profile";

export type ProductApplicationStatus =
  | "PENDING"
  | "UNDER_REVIEW"
  | "ACCEPTED"
  | "IN_PROGRESS"
  | "REJECTED"
  | "COMPLETED"
  | "WITHDRAWN";

export type ProductStatusHistoryEntry = {
  notes?: string;
  status: ProductApplicationStatus;
  systemUpdate?: boolean;
  timestamp: unknown;
  updatedAt?: unknown;
  updatedBy?: string;
};

export type ProductJob = {
  acceptedCount: number;
  applicationCount: number;
  category: string;
  companyName: string;
  contactNumber: string;
  createdAt: unknown;
  description: string;
  employerId: string;
  employerTrustTier: string;
  expiresAt: unknown;
  gender: string;
  id: string;
  isActive: boolean;
  isFilled: boolean;
  jobId: string;
  jobType: string;
  latitude: number;
  location: string;
  longitude: number;
  payAmount: string;
  payType: string;
  postedAt: unknown;
  shiftTiming: string;
  title: string;
  updatedAt: unknown;
  vacancies: number;
  vacancyStatus: string;
};

export type ProductApplication = {
  active: boolean;
  appliedAt: unknown;
  companyName: string;
  coverLetter: string;
  employerId: string;
  id: string;
  jobId: string;
  jobLocation: string;
  jobTitle: string;
  source: string;
  status: ProductApplicationStatus;
  statusHistory: ProductStatusHistoryEntry[];
  updatedAt: unknown;
  workerId: string;
  workerName: string;
};

const applicationStatuses: ProductApplicationStatus[] = [
  "PENDING",
  "UNDER_REVIEW",
  "ACCEPTED",
  "IN_PROGRESS",
  "REJECTED",
  "COMPLETED",
  "WITHDRAWN"
];

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function numberValue(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  return fallback;
}

function booleanValue(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

export function normalizeApplicationStatus(value: unknown): ProductApplicationStatus {
  const candidate = typeof value === "string" ? value.trim().toUpperCase() : "";
  return applicationStatuses.includes(candidate as ProductApplicationStatus)
    ? (candidate as ProductApplicationStatus)
    : "PENDING";
}

export function normalizeProductJob(
  id: string,
  value: Record<string, unknown> | undefined
): ProductJob {
  const payload = value ?? {};

  return {
    acceptedCount: numberValue(payload.acceptedCount),
    applicationCount: numberValue(payload.applicationCount),
    category: stringValue(payload.category).toUpperCase(),
    companyName: stringValue(payload.companyName),
    contactNumber: stringValue(payload.contactNumber),
    createdAt: payload.createdAt ?? payload.postedAt ?? Date.now(),
    description: stringValue(payload.description),
    employerId: stringValue(payload.employerId),
    employerTrustTier: stringValue(payload.employerTrustTier, "NEW"),
    expiresAt: payload.expiresAt ?? 0,
    gender: stringValue(payload.gender, "ANY"),
    id,
    isActive: booleanValue(payload.isActive, true),
    isFilled: booleanValue(payload.isFilled),
    jobId: stringValue(payload.jobId, id),
    jobType: stringValue(payload.jobType, "FULL_TIME"),
    latitude: numberValue(payload.latitude),
    location: stringValue(payload.location),
    longitude: numberValue(payload.longitude),
    payAmount: stringValue(payload.payAmount),
    payType: stringValue(payload.payType, "MONTHLY"),
    postedAt: payload.postedAt ?? payload.createdAt ?? Date.now(),
    shiftTiming: stringValue(payload.shiftTiming),
    title: stringValue(payload.title),
    updatedAt: payload.updatedAt ?? payload.createdAt ?? Date.now(),
    vacancies: Math.max(1, numberValue(payload.vacancies, 1)),
    vacancyStatus: stringValue(payload.vacancyStatus, "OPEN")
  };
}

export function normalizeProductApplication(
  id: string,
  value: Record<string, unknown> | undefined
): ProductApplication {
  const payload = value ?? {};
  const statusHistoryValue = Array.isArray(payload.statusHistory) ? payload.statusHistory : [];

  return {
    active: booleanValue(payload.active, true),
    appliedAt: payload.appliedAt ?? payload.updatedAt ?? Date.now(),
    companyName: stringValue(payload.companyName),
    coverLetter: stringValue(payload.coverLetter),
    employerId: stringValue(payload.employerId),
    id,
    jobId: stringValue(payload.jobId),
    jobLocation: stringValue(payload.jobLocation),
    jobTitle: stringValue(payload.jobTitle),
    source: stringValue(payload.source, "WEB_PORTAL"),
    status: normalizeApplicationStatus(payload.status),
    statusHistory: statusHistoryValue
      .filter((item): item is Record<string, unknown> => typeof item === "object" && item !== null)
      .map((item) => ({
        notes: stringValue(item.notes),
        status: normalizeApplicationStatus(item.status),
        systemUpdate: booleanValue(item.systemUpdate),
        timestamp: item.timestamp ?? item.updatedAt ?? Date.now(),
        updatedAt: item.updatedAt ?? item.timestamp ?? Date.now(),
        updatedBy: stringValue(item.updatedBy)
      })),
    updatedAt: payload.updatedAt ?? payload.appliedAt ?? Date.now(),
    workerId: stringValue(payload.workerId),
    workerName: stringValue(payload.workerName)
  };
}

export function sortByTimestampDesc<T extends object>(
  items: T[],
  field: keyof T
): T[] {
  return [...items].sort((left, right) => {
    const leftDate = readTimestamp(left[field]);
    const rightDate = readTimestamp(right[field]);

    return (rightDate?.getTime() ?? 0) - (leftDate?.getTime() ?? 0);
  });
}

export function isLiveJob(job: ProductJob): boolean {
  if (!job.isActive || job.isFilled || job.vacancyStatus === "FILLED") {
    return false;
  }

  const expiry = readTimestamp(job.expiresAt);
  return !expiry || expiry.getTime() > Date.now();
}

const employerJobEditWindowMs = 7 * 24 * 60 * 60 * 1000;

export function canEditEmployerJob(job: ProductJob, now = Date.now()): boolean {
  const postedAt = readTimestamp(job.postedAt)?.getTime() ?? 0;

  if (!postedAt) {
    return false;
  }

  return now - postedAt <= employerJobEditWindowMs;
}

export function employerJobEditRestrictionMessage(
  job: ProductJob,
  now = Date.now()
): string {
  if (canEditEmployerJob(job, now)) {
    return "";
  }

  const postedAt = readTimestamp(job.postedAt)?.getTime() ?? 0;
  if (!postedAt) {
    return "This job cannot be edited because the original post time is unavailable.";
  }

  const elapsedDays = Math.max(
    1,
    Math.floor((now - postedAt) / (24 * 60 * 60 * 1000))
  );

  return `Jobs can only be edited within 7 days of posting. This job was posted ${elapsedDays} days ago.`;
}

export function workerProfileCompletion(profile: ProductUserProfile | null | undefined): number {
  let score = 0;

  if (profile?.fullName?.trim() || profile?.name?.trim()) score += 15;
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

export function employerProfileCompletion(profile: ProductUserProfile | null | undefined): number {
  let score = 0;

  if (profile?.companyName?.trim()) score += 20;
  if (profile?.industry?.trim()) score += 15;
  if (profile?.contactPhone?.trim() || profile?.phone?.trim()) score += 15;
  if (profile?.businessAddress?.trim() || profile?.address?.trim()) score += 20;
  if (profile?.gender?.trim()) score += 10;
  if (profile?.dateOfBirth?.trim()) score += 10;
  if (profile?.contactEmail?.trim() || profile?.email?.trim()) score += 5;
  if (profile?.companySize?.trim()) score += 5;

  return Math.min(score, 100);
}

export function missingWorkerFields(profile: ProductUserProfile | null | undefined): string[] {
  const missing: string[] = [];

  if (!(profile?.fullName?.trim() || profile?.name?.trim())) missing.push("Full name");
  if (!profile?.phone?.trim()) missing.push("Phone");
  if (!profile?.address?.trim()) missing.push("Address");
  if (!profile?.skills?.trim()) missing.push("Skills");
  if (!profile?.experience?.trim()) missing.push("Experience");

  return missing;
}

export function missingEmployerFields(profile: ProductUserProfile | null | undefined): string[] {
  const missing: string[] = [];

  if (!profile?.companyName?.trim()) missing.push("Company name");
  if (!profile?.industry?.trim()) missing.push("Industry");
  if (!(profile?.contactPhone?.trim() || profile?.phone?.trim())) missing.push("Contact phone");
  if (!(profile?.businessAddress?.trim() || profile?.address?.trim())) missing.push("Business address");

  return missing;
}

export function productStatusLabel(status: ProductApplicationStatus): string {
  return status
    .toLowerCase()
    .split("_")
    .map((chunk) => chunk[0]?.toUpperCase() + chunk.slice(1))
    .join(" ");
}

export function productStatusTone(status: ProductApplicationStatus): string {
  switch (status) {
    case "ACCEPTED":
    case "IN_PROGRESS":
    case "COMPLETED":
      return "success";
    case "REJECTED":
      return "danger";
    case "PENDING":
    case "UNDER_REVIEW":
      return "warning";
    default:
      return "neutral";
  }
}

export function canEmployerMoveToUnderReview(status: ProductApplicationStatus): boolean {
  return status === "PENDING";
}

export function canEmployerAcceptOrReject(status: ProductApplicationStatus): boolean {
  return status === "PENDING" || status === "UNDER_REVIEW";
}

export function canEmployerVerifyWork(status: ProductApplicationStatus): boolean {
  return status === "ACCEPTED";
}

export function canEmployerMarkWorkComplete(status: ProductApplicationStatus): boolean {
  return status === "ACCEPTED" || status === "IN_PROGRESS";
}

export function canEmployerRateWorker(status: ProductApplicationStatus): boolean {
  return status === "COMPLETED";
}

export function defaultWorkerName(profile: ProductUserProfile | null | undefined): string {
  return displayProfileName(profile);
}

export function defaultCompanyName(profile: ProductUserProfile | null | undefined): string {
  return profile?.companyName?.trim() || displayProfileName(profile);
}

export function deriveJobCategory(title: string, description: string): string {
  const corpus = `${title} ${description}`.toLowerCase();

  if (corpus.includes("deliver")) return "DELIVERY";
  if (corpus.includes("cook") || corpus.includes("chef")) return "COOKING";
  if (corpus.includes("clean") || corpus.includes("housekeep")) return "HOUSEKEEPING";
  if (corpus.includes("driver")) return "DRIVING";
  if (corpus.includes("retail") || corpus.includes("cashier")) return "RETAIL";

  return "HELPER";
}
