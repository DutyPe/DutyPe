import { readTimestamp } from "@/lib/firebase/firestore-helpers";

import type { ProductRole } from "./profile";

export type ProductConversationParticipant = {
  name: string;
  profileImage: string | null;
  role: string;
};

export type ProductConversation = {
  createdAt: unknown;
  id: string;
  jobId: string;
  lastMessage: string;
  lastMessageAt: unknown;
  lastMessageBy: string;
  participantDetails: Record<string, ProductConversationParticipant>;
  participants: string[];
  unreadCount: Record<string, number>;
  updatedAt: unknown;
};

export type ProductMessage = {
  conversationId: string;
  createdAt: unknown;
  id: string;
  isRead: boolean;
  message: string;
  recipientId: string;
  senderId: string;
  type: string;
};

export type ProductNotification = {
  applicationId: string;
  conversationId: string;
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

function stringArrayValue(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.map((item) => String(item).trim()).filter(Boolean);
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

function participantDetailsValue(
  value: unknown
): Record<string, ProductConversationParticipant> {
  if (!value || typeof value !== "object") {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, entry]) => {
      const payload = entry && typeof entry === "object"
        ? (entry as Record<string, unknown>)
        : {};

      return [
        key,
        {
          name: stringValue(payload.name, "DutyPe user"),
          profileImage: stringValue(payload.profileImage) || null,
          role: stringValue(payload.role, "UNKNOWN")
        }
      ];
    })
  );
}

function unreadCountValue(value: unknown): Record<string, number> {
  if (!value || typeof value !== "object") {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, entry]) => [
      key,
      numberValue(entry)
    ])
  );
}

export function normalizeConversation(
  id: string,
  value: Record<string, unknown> | undefined
): ProductConversation {
  const payload = value ?? {};

  return {
    createdAt: payload.createdAt ?? payload.updatedAt ?? Date.now(),
    id,
    jobId: stringValue(payload.jobId),
    lastMessage: stringValue(payload.lastMessage),
    lastMessageAt: payload.lastMessageAt ?? payload.updatedAt ?? payload.createdAt ?? Date.now(),
    lastMessageBy: stringValue(payload.lastMessageBy),
    participantDetails: participantDetailsValue(payload.participantDetails),
    participants: stringArrayValue(payload.participants),
    unreadCount: unreadCountValue(payload.unreadCount),
    updatedAt: payload.updatedAt ?? payload.lastMessageAt ?? payload.createdAt ?? Date.now()
  };
}

export function normalizeMessage(
  id: string,
  value: Record<string, unknown> | undefined
): ProductMessage {
  const payload = value ?? {};

  return {
    conversationId: stringValue(payload.conversationId),
    createdAt: payload.createdAt ?? Date.now(),
    id,
    isRead: booleanValue(payload.isRead),
    message: stringValue(payload.message),
    recipientId: stringValue(payload.recipientId),
    senderId: stringValue(payload.senderId),
    type: stringValue(payload.type, "TEXT")
  };
}

export function normalizeNotification(
  id: string,
  value: Record<string, unknown> | undefined
): ProductNotification {
  const payload = value ?? {};

  return {
    applicationId:
      stringValue(payload.applicationId) || stringValue(payload.data && (payload.data as Record<string, unknown>).applicationId),
    conversationId:
      stringValue(payload.conversationId) || stringValue(payload.data && (payload.data as Record<string, unknown>).conversationId),
    createdAt: payload.createdAt ?? payload.updatedAt ?? Date.now(),
    data: recordOfStrings(payload.data),
    id,
    isRead: booleanValue(payload.isRead),
    jobId: stringValue(payload.jobId) || stringValue(payload.data && (payload.data as Record<string, unknown>).jobId),
    message: stringValue(payload.message),
    recipientId: stringValue(payload.recipientId),
    title: stringValue(payload.title, "DutyPe update"),
    type: stringValue(payload.type, "GENERAL").toUpperCase()
  };
}

export function sortByTimestampAsc<T extends object>(items: T[], field: keyof T): T[] {
  return [...items].sort((left, right) => {
    const leftDate = readTimestamp(left[field]);
    const rightDate = readTimestamp(right[field]);

    return (leftDate?.getTime() ?? 0) - (rightDate?.getTime() ?? 0);
  });
}

export function sortByTimestampDesc<T extends object>(items: T[], field: keyof T): T[] {
  return [...items].sort((left, right) => {
    const leftDate = readTimestamp(left[field]);
    const rightDate = readTimestamp(right[field]);

    return (rightDate?.getTime() ?? 0) - (leftDate?.getTime() ?? 0);
  });
}

export function conversationPeer(
  conversation: ProductConversation,
  userId: string
): ProductConversationParticipant {
  const peerId = conversation.participants.find((participantId) => participantId !== userId);

  if (!peerId) {
    return {
      name: "DutyPe contact",
      profileImage: null,
      role: "UNKNOWN"
    };
  }

  return (
    conversation.participantDetails[peerId] ?? {
      name: "DutyPe contact",
      profileImage: null,
      role: "UNKNOWN"
    }
  );
}

export function unreadConversationCount(
  conversations: ProductConversation[],
  userId: string
): number {
  return conversations.reduce(
    (count, conversation) => count + (conversation.unreadCount[userId] ?? 0),
    0
  );
}

const workerNotificationTypes = new Set([
  "APPLICATION_STATUS",
  "APPLICATION_STATUS_UPDATE",
  "CHAT_MESSAGE",
  "GENERAL",
  "INTERVIEW_SCHEDULED",
  "PROFILE_COMPLETE",
  "WELCOME",
  "SYSTEM_UPDATE",
  "WORKER_HIRED"
]);

const employerNotificationTypes = new Set([
  "CHAT_MESSAGE",
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
    .map((chunk) => chunk[0]?.toUpperCase() + chunk.slice(1))
    .join(" ");
}
