import { readTimestamp } from "@/lib/firebase/firestore-helpers";

import type { ProductRole } from "./profile";

export type ProductNotification = {
  applicationId: string;
  createdAt: unknown;
  data: Record<string, string>;
  id: string;
  isRead: boolean;
  jobId: string;
  message: string;
  recipientId: string;
  title: string;
  type: string;
};

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

function booleanValue(value: unknown, fallback = false): boolean {
  return typeof value === "boolean" ? value : fallback;
}

function recordOfStrings(value: unknown): Record<string, string> {
  if (!value || typeof value !== "object") {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>)
      .map(([key, entry]) => [key, stringValue(entry)])
      .filter(([, entry]) => Boolean(entry))
  );
}

export function normalizeNotification(
  id: string,
  value: Record<string, unknown> | undefined
): ProductNotification {
  const payload = value ?? {};
  const data = (payload.data && typeof payload.data === "object"
    ? (payload.data as Record<string, unknown>)
    : {}) as Record<string, unknown>;

  return {
    applicationId: stringValue(payload.applicationId) || stringValue(data.applicationId),
    createdAt: payload.createdAt ?? payload.updatedAt ?? Date.now(),
    data: recordOfStrings(payload.data),
    id,
    isRead: booleanValue(payload.isRead),
    jobId: stringValue(payload.jobId) || stringValue(data.jobId),
    message: stringValue(payload.message),
    recipientId: stringValue(payload.recipientId),
    title: stringValue(payload.title, "DutyPe update"),
    type: stringValue(payload.type, "GENERAL").toUpperCase()
  };
}

export function sortByTimestampDesc<T extends object>(items: T[], field: keyof T): T[] {
  return [...items].sort((left, right) => {
    const leftDate = readTimestamp(left[field]);
    const rightDate = readTimestamp(right[field]);

    return (rightDate?.getTime() ?? 0) - (leftDate?.getTime() ?? 0);
  });
}

const workerNotificationTypes = new Set([
  "APPLICATION_STATUS",
  "APPLICATION_STATUS_UPDATE",
  "GENERAL",
  "INTERVIEW_SCHEDULED",
  "PROFILE_COMPLETE",
  "WELCOME",
  "SYSTEM_UPDATE",
  "WORKER_HIRED"
]);

const employerNotificationTypes = new Set([
  "GENERAL",
  "JOB_PAUSED",
  "JOB_POSTED",
  "NEW_APPLICATION",
  "PROFILE_COMPLETE",
  "WELCOME",
  "SYSTEM_UPDATE",
  "WORKER_HIRED"
]);

export function isNotificationRelevant(
  notification: ProductNotification,
  role: ProductRole
): boolean {
  return role === "WORKER"
    ? workerNotificationTypes.has(notification.type)
    : employerNotificationTypes.has(notification.type);
}

export function notificationTypeLabel(type: string): string {
  return type
    .toLowerCase()
    .split("_")
    .map((chunk) => (chunk[0]?.toUpperCase() ?? "") + chunk.slice(1))
    .join(" ");
}