import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { validateString, validateMessage, validateEnum, validateUserId, assertAppCheck } from "./validation";
import { getUserLanguage, getUserDisplayName, tTitle, tBody, SUPPORTED_LOCALES, normalizeLocale, localizedTopic } from "./notification-i18n";

// Initialize Firebase Admin SDK
admin.initializeApp();

// ============================================
// SCHEDULED NOTIFICATIONS (Enterprise Grade)
// ============================================
export * from './scheduled-notifications';
export { cleanupExpiredNotifications } from './scheduled-notifications';
export * from './referral-system';
export * from './job-landing';
export * from './worker-landing';
export * from './employer-landing';
export * from './auth-callables';

const db = admin.firestore();
const messaging = admin.messaging();

const ONE_DAY_MS = 24 * 60 * 60 * 1000;

// Topic constants (must match Android app)
const TOPIC_ALL_USERS = "all_users";
const TOPIC_WORKERS = "workers";
const TOPIC_EMPLOYERS = "employers";
const TOPIC_APP_UPDATES = "app_updates";
const SELF_NOTIFICATION_RETENTION_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_SELF_NOTIFICATION_DATA_KEYS = 25;
const ALLOWED_SELF_NOTIFICATION_TYPES = new Set([
  "PROFILE_COMPLETE",
  "WELCOME",
  "JOB_POSTED",
  "WORKER_HIRED",
  "SYSTEM_UPDATE",
  "GENERAL",
]);
const INSTANT_WORKER_SCAN_LIMIT = 80;
const INSTANT_WORKER_NOTIFY_LIMIT = 25;
const MAX_INSTANT_WORK_DISTANCE_KM = 10;

const CATEGORY_KEYWORDS: Array<{ name: string; display: string; words: string[] }> = [
  { name: "DELIVERY", display: "Delivery", words: ["delivery", "courier", "rider", "parcel"] },
  { name: "DRIVER", display: "Driver", words: ["driver", "cab", "taxi", "truck"] },
  { name: "COOK", display: "Cook", words: ["cook", "chef", "kitchen", "catering"] },
  { name: "MAID", display: "Housekeeping", words: ["maid", "housekeep", "cleaner", "domestic", "house help"] },
  { name: "SECURITY", display: "Security", words: ["security", "guard", "watchman"] },
  { name: "HELPER", display: "Helper", words: ["helper", "assistant", "labour", "labor", "loader", "loading", "unloading"] },
  { name: "ELECTRICIAN", display: "Electrician", words: ["electric", "wiring"] },
  { name: "PLUMBER", display: "Plumber", words: ["plumb", "pipe"] },
  { name: "WAITER", display: "Restaurant Staff", words: ["waiter", "server", "restaurant", "hotel", "steward"] },
  { name: "SALES", display: "Sales", words: ["sales", "retail", "store"] },
  { name: "TELECALLER", display: "Telecaller", words: ["telecaller", "calling", "call center", "bpo"] },
  { name: "OFFICE_STAFF", display: "Office Staff", words: ["office", "admin", "clerk", "peon"] },
  { name: "PACKER", display: "Packer", words: ["packer", "packing", "warehouse"] },
  { name: "MECHANIC", display: "Mechanic", words: ["mechanic", "garage", "technician"] },
];

function timestampMillis(value: unknown): number {
  if (value instanceof admin.firestore.Timestamp) {
    return value.toMillis();
  }
  if (typeof value === "number") {
    return value;
  }
  if (typeof value === "object" && value !== null && "toMillis" in value) {
    const maybeTimestamp = value as { toMillis?: () => number };
    if (typeof maybeTimestamp.toMillis === "function") {
      return maybeTimestamp.toMillis();
    }
  }
  return 0;
}

function distanceKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRadians = (degrees: number) => degrees * Math.PI / 180;
  const earthRadiusKm = 6371;
  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2);
  return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function validCoordinates(lat: unknown, lng: unknown): boolean {
  return typeof lat === "number" && typeof lng === "number" &&
    lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 &&
    (lat !== 0 || lng !== 0);
}

function normalizeMatchToken(value: unknown): string {
  return String(value || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function stringList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .map((item) => normalizeMatchToken(item))
      .filter(Boolean);
  }
  if (typeof value === "string") {
    return value
      .split(",")
      .map((item) => normalizeMatchToken(item))
      .filter(Boolean);
  }
  return [];
}

function inferInstantRequestCategory(request: Record<string, unknown>): { name: string; display: string } {
  const explicit = normalizeMatchToken(request.category).replace(/ /g, "_").toUpperCase();
  const explicitMatch = CATEGORY_KEYWORDS.find((category) => category.name === explicit);
  if (explicitMatch) return { name: explicitMatch.name, display: explicitMatch.display };

  const requestText = normalizeMatchToken(`${request.title || ""} ${request.description || ""} ${request.category || ""}`);
  const inferred = CATEGORY_KEYWORDS.find((category) =>
    category.words.some((word) => requestText.includes(word))
  );
  return inferred ? { name: inferred.name, display: inferred.display } : { name: "OTHER", display: "Other" };
}

function scoreInstantRoleFit(
  request: Record<string, unknown>,
  worker: Record<string, unknown>,
): number {
  const requestCategory = inferInstantRequestCategory(request);
  if (requestCategory.name === "OTHER") return 0;

  const workerSkills = stringList(worker.skills || worker.jobTypes || worker.categories || worker.primaryJobType);
  if (workerSkills.length === 0) return 0;

  const requestText = normalizeMatchToken(`${request.title || ""} ${request.description || ""} ${request.category || ""}`);
  const categoryRule = CATEGORY_KEYWORDS.find((item) => item.name === requestCategory.name);
  const categoryWords = categoryRule?.words || [requestCategory.display.toLowerCase()];
  const skillText = workerSkills.join(" ");
  const categoryMatch = categoryWords.some((word) => skillText.includes(word)) ||
    workerSkills.some((skill) => skill.length >= 3 && requestText.includes(skill));

  return categoryMatch ? 18 : -14;
}

function chunked<T>(items: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
}

function sanitizeNotificationDataMap(rawData: unknown): Record<string, string> {
  if (!rawData || typeof rawData !== "object") {
    return {};
  }

  const dataObject = rawData as Record<string, unknown>;
  const cleanData: Record<string, string> = {};

  for (const [rawKey, rawValue] of Object.entries(dataObject).slice(0, MAX_SELF_NOTIFICATION_DATA_KEYS)) {
    const key = String(rawKey).trim().slice(0, 64);
    if (!key) continue;

    const value = String(rawValue ?? "").trim().slice(0, 500);
    cleanData[key] = value;
  }

  return cleanData;
}

/**
 * Send broadcast notification to all users or specific role
 * Triggered when a document is created in "broadcast_notifications" collection
 * 
 * Document fields:
 * - title: string (required)
 * - message: string (required)
 * - topic: string (optional) - "all_users", "workers", "employers", "app_updates"
 *                              defaults to "all_users"
 * - type: string (optional) - notification type for app handling
 */
export const sendBroadcastNotification = functions.firestore
  .document("broadcast_notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const notification = snapshot.data();
    const notificationId = context.params.notificationId;

    functions.logger.info(`Processing broadcast notification: ${notificationId}`, notification);

    const fallbackTitle = notification.title || "DutyPe";
    const fallbackMessage = notification.message || "";
    const topic = notification.topic || TOPIC_ALL_USERS;
    const type = notification.type || "broadcast";

    // Validate topic
    const validTopics = [TOPIC_ALL_USERS, TOPIC_WORKERS, TOPIC_EMPLOYERS, TOPIC_APP_UPDATES];
    if (!validTopics.includes(topic)) {
      functions.logger.error(`Invalid topic: ${topic}`);
      await snapshot.ref.update({
        error: `Invalid topic: ${topic}. Valid topics: ${validTopics.join(", ")}`,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      return null;
    }

    // Translations map: { en: { title, message }, te: { title, message } }.
    // When provided, fan out to per-language topics so each device receives its locale.
    const translations = (notification.translations && typeof notification.translations === "object")
      ? notification.translations as Record<string, { title?: string; message?: string }>
      : null;

    try {
      const sendToOne = async (sendTopic: string, sendTitle: string, sendMessage: string) => {
        const topicMessage: admin.messaging.Message = {
          topic: sendTopic,
          data: {
            notificationId: notificationId,
            title: sendTitle,
            message: sendMessage,
            body: sendMessage,
            type: type,
            click_action: "FLUTTER_NOTIFICATION_CLICK",
          },
          android: {
            priority: "high",
            notification: {
              title: sendTitle,
              body: sendMessage,
              icon: "ic_notification",
              color: "#3B82F6",
              sound: "default",
              clickAction: "OPEN_ACTIVITY",
            },
          },
        };
        return messaging.send(topicMessage);
      };

      const responses: Record<string, string> = {};

      if (translations) {
        for (const lang of SUPPORTED_LOCALES) {
          const t = translations[lang];
          const localizedTitle = (t?.title && t.title.trim()) || fallbackTitle;
          const localizedMessage = (t?.message && t.message.trim()) || fallbackMessage;
          const langTopic = localizedTopic(topic, lang);
          const resp = await sendToOne(langTopic, localizedTitle, localizedMessage);
          responses[lang] = resp;
          functions.logger.info(`Broadcast (${lang}) sent to ${langTopic}: ${resp}`);
        }
      } else {
        const resp = await sendToOne(topic, fallbackTitle, fallbackMessage);
        responses["default"] = resp;
        functions.logger.info(`Broadcast notification sent to topic ${topic}: ${resp}`);
      }

      // Update document with sent status
      await snapshot.ref.update({
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        fcmMessageIds: responses,
        status: "sent",
      });

      return responses;
    } catch (error) {
      functions.logger.error("Error sending broadcast notification:", error);
      
      await snapshot.ref.update({
        error: String(error),
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        status: "failed",
      });
      
      return null;
    }
  });

/**
 * Triggered when a new notification document is created in Firestore
 * Sends push notification to the recipient's device
 * 
 * DEDUPLICATION STRATEGY:
 * 1. Check if sentAt exists (already processed)
 * 2. Use transaction to atomically check + mark as processing
 * 3. Check for duplicate notifications in last 5 seconds (same title + recipient)
 */
export const sendPushNotification = functions.firestore
  .document("notifications/{notificationId}")
  .onCreate(async (snapshot, context) => {
    const notification = snapshot.data();
    const notificationId = context.params.notificationId;

    functions.logger.info(`📬 FCM: Processing notification ${notificationId}`, {
      title: notification.title,
      type: notification.type,
      recipientId: notification.recipientId
    });

    // DEDUPLICATION CHECK 1: Already sent
    if (notification.sentAt) {
      functions.logger.warn(`📬 FCM: ⚠️ Notification ${notificationId} already sent, skipping duplicate`);
      return null;
    }

    const recipientId = notification.recipientId;
    if (!recipientId) {
      functions.logger.warn("📬 FCM: No recipientId in notification, skipping");
      return null;
    }

    if (notification.skipPush === true) {
      functions.logger.info(`📬 FCM: Skipping push for self-notification ${notificationId}`);
      await snapshot.ref.update({
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        skipped: true,
        skipReason: "SKIP_PUSH",
      });
      return null;
    }

    try {
      // DEDUPLICATION CHECK 2: Transaction-based lock to prevent race conditions
      const lockResult = await db.runTransaction(async (transaction) => {
        const notifRef = snapshot.ref;
        const freshDoc = await transaction.get(notifRef);
        
        if (!freshDoc.exists) {
          functions.logger.warn(`📬 FCM: Notification ${notificationId} deleted before processing`);
          return { locked: false, reason: "deleted" };
        }
        
        const freshData = freshDoc.data();
        
        // Check if already marked as processing or sent
        if (freshData?.sentAt || freshData?.processing) {
          functions.logger.warn(`📬 FCM: ⚠️ Notification ${notificationId} already processing/sent`);
          return { locked: false, reason: "already_processing" };
        }
        
        // Mark as processing to prevent duplicate execution
        transaction.update(notifRef, {
          processing: true,
          processingStartedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        return { locked: true };
      });
      
      if (!lockResult.locked) {
        functions.logger.info(`📬 FCM: Skipping notification ${notificationId} - ${lockResult.reason}`);
        return null;
      }

      // DEDUPLICATION CHECK 3: Best-effort duplicate detection in last 5 seconds.
      // Use single-field query to avoid composite index failures in production.
      const fiveSecondsAgoMs = Date.now() - 5000;
      const candidateNotifications = await db.collection("notifications")
        .where("recipientId", "==", recipientId)
        .limit(30)
        .get();

      const getCreatedAtMs = (rawCreatedAt: unknown): number => {
        if (rawCreatedAt instanceof admin.firestore.Timestamp) {
          return rawCreatedAt.toMillis();
        }

        if (typeof rawCreatedAt === "number") {
          return rawCreatedAt;
        }

        if (typeof rawCreatedAt === "object" && rawCreatedAt !== null && "toMillis" in rawCreatedAt) {
          const maybeTimestamp = rawCreatedAt as { toMillis?: () => number };
          if (typeof maybeTimestamp.toMillis === "function") {
            return maybeTimestamp.toMillis();
          }
        }

        return 0;
      };

      const matchingNotifications = candidateNotifications.docs.filter((doc) => {
        const data = doc.data();

        if (data.title !== notification.title) return false;
        if (data.type !== notification.type) return false;

        const createdAtMs = getCreatedAtMs(data.createdAt);
        return createdAtMs >= fiveSecondsAgoMs;
      });

      if (matchingNotifications.length > 1) {
        // Found duplicates - only process the first one (oldest)
        const sortedDocs = matchingNotifications.sort((a, b) => {
          const aTime = getCreatedAtMs(a.data().createdAt);
          const bTime = getCreatedAtMs(b.data().createdAt);
          return aTime - bTime;
        });

        const firstDocId = sortedDocs[0].id;
        if (notificationId !== firstDocId) {
          functions.logger.warn(`📬 FCM: ⚠️ Duplicate notification detected! Skipping ${notificationId}, keeping ${firstDocId}`);

          // Mark this as duplicate and skip
          await snapshot.ref.update({
            processing: false,
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            skipped: true,
            skipReason: "DUPLICATE_NOTIFICATION",
            duplicateOf: firstDocId
          });

          return null;
        }
      }

      // Get recipient's FCM token from the canonical token collection.
      const tokenDoc = await db.collection("user_tokens").doc(recipientId).get();
      
      if (!tokenDoc.exists) {
        functions.logger.warn(`📬 FCM: No token document found for: ${recipientId}`);
        await snapshot.ref.update({
          processing: false,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          error: "NO_FCM_TOKEN"
        });
        return null;
      }

      const tokenData = tokenDoc.data();
      if (!tokenData?.fcmToken) {
        functions.logger.warn(`📬 FCM: No FCM token for user: ${recipientId}`);
        await snapshot.ref.update({
          processing: false,
          sentAt: admin.firestore.FieldValue.serverTimestamp(),
          error: "INACTIVE_FCM_TOKEN"
        });
        return null;
      }

      const fcmToken = tokenData.fcmToken;

      // Localization: if the doc carries a templateId + params, render in the
      // recipient's preferred language. Otherwise fall back to the literal
      // title/message that producers wrote (which themselves should already be
      // localized — see notification-fanout.ts and referral-system.ts).
      let effectiveTitle = notification.title || "DutyPe";
      let effectiveMessage = notification.message || "";
      let effectiveLocale = normalizeLocale(notification.locale);
      const templateId = typeof notification.templateId === "string" ? notification.templateId : "";
      if (templateId) {
        effectiveLocale = await getUserLanguage(db, recipientId);
        const params = (notification.params && typeof notification.params === "object")
          ? notification.params as Record<string, string | number>
          : undefined;
        effectiveTitle = tTitle(templateId, effectiveLocale, params);
        effectiveMessage = tBody(templateId, effectiveLocale, params);
      }

      // Extract deep link from notification data
      const deepLink = notification.data?.deepLink || "";
      
      // Map notification type to Android channel ID
      const notificationType = notification.type || "general";
      const highPriorityTypes = ["BIRTHDAY", "JOB_EXPIRY", "APPLICATION_STATUS", "JOB_ALERT", "NEW_JOB_ALERT", "NEW_APPLICATION", "APPLICATION_WITHDRAWN", "WORKER_HIRED", "PROFILE_COMPLETE", "JOB_POSTED", "SHORTLISTED", "REJECTED", "APPLICATION_STATUS_UPDATE", "WELCOME"];
      const mediumPriorityTypes = ["PENDING_APPLICATIONS", "JOB_RECOMMENDATION", "REMINDER", "INTERVIEW_SCHEDULED"];
      let channelId = "low_priority";
      if (highPriorityTypes.includes(notificationType)) {
        channelId = "high_priority";
      } else if (mediumPriorityTypes.includes(notificationType)) {
        channelId = "medium_priority";
      }

      // Build the FCM message.
      // DATA-ONLY message (no android.notification block) so that onMessageReceived()
      // is ALWAYS called by DutyPeMessagingService regardless of whether the app is
      // in foreground, background, or killed. This gives the app full control over
      // how the notification is displayed and ensures deep-links work correctly.
      const message: admin.messaging.Message = {
        token: fcmToken,
        data: {
          notificationId: notificationId,
          title: effectiveTitle,
          message: effectiveMessage,
          body: effectiveMessage,
          type: notificationType,
          deepLink: deepLink,
          channel: channelId,
          locale: effectiveLocale,
        },
        android: {
          priority: "high",
        },
      };

      // Send the notification
      const response = await messaging.send(message);
      functions.logger.info(`📬 FCM: ✅ Notification sent successfully: ${response}`);

      // Update notification document with sent status (and resolved copy for inbox)
      const updatePayload: Record<string, unknown> = {
        processing: false,
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
        fcmMessageId: response,
      };
      if (templateId) {
        updatePayload.title = effectiveTitle;
        updatePayload.message = effectiveMessage;
      }
      await snapshot.ref.update(updatePayload);

      return response;
    } catch (error) {
      functions.logger.error(`📬 FCM: ❌ Error sending notification ${notificationId}:`, error);
      
      // Update notification with error status
      await snapshot.ref.update({
        processing: false,
        error: String(error),
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      
      return null;
    }
  });

/**
 * Persists self-targeted notifications for inbox visibility.
 * Recipient is always the authenticated user; push is skipped to avoid duplicates
 * because the app already shows an immediate local notification.
 */
export const persistSelfNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth?.uid) {
    throw new functions.https.HttpsError("unauthenticated", "Authentication required");
  }

  const userId = context.auth.uid;

  const title = validateString(data?.title, "title", {
    required: true,
    minLength: 1,
    maxLength: 120,
  });
  const message = validateMessage(data?.message, 700);

  const notificationType = validateString(data?.type, "type", {
    required: true,
    minLength: 1,
    maxLength: 64,
  }).toUpperCase();

  if (!ALLOWED_SELF_NOTIFICATION_TYPES.has(notificationType)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `Unsupported self notification type: ${notificationType}`
    );
  }

  const targetRoleRaw = validateString(data?.targetRole, "targetRole", {
    required: false,
    maxLength: 16,
  }).toUpperCase();
  const targetRole = targetRoleRaw === "WORKER" || targetRoleRaw === "EMPLOYER"
    ? targetRoleRaw
    : "";

  const payloadData = sanitizeNotificationDataMap(data?.data);

  const requestedNotificationId = typeof data?.notificationId === "string"
    ? data.notificationId.trim()
    : "";
  const notificationId = /^[A-Za-z0-9_-]{8,128}$/.test(requestedNotificationId)
    ? requestedNotificationId
    : db.collection("notifications").doc().id;

  const nowMs = Date.now();
  const requestedExpiresAt = Number(data?.expiresAt);
  const maxAllowedExpiry = nowMs + SELF_NOTIFICATION_RETENTION_MS;
  const resolvedExpiryMs = Number.isFinite(requestedExpiresAt) && requestedExpiresAt > nowMs
    ? Math.min(requestedExpiresAt, maxAllowedExpiry)
    : maxAllowedExpiry;

  const notificationRef = db.collection("notifications").doc(notificationId);
  await notificationRef.set({
    recipientId: userId,
    title,
    message,
    type: notificationType,
    ...(targetRole ? { targetRole } : {}),
    data: payloadData,
    isRead: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: admin.firestore.Timestamp.fromMillis(resolvedExpiryMs),
    skipPush: true,
  });

  return {
    success: true,
    notificationId: notificationRef.id,
  };
});

export const notifyAvailableWorkersForInstantRequest = functions.firestore
  .document("instant_requests/{requestId}")
  .onCreate(async (snapshot, context) => {
    const request = snapshot.data() || {};
    const requestId = context.params.requestId;
    const status = String(request.status || "open").toLowerCase();
    if (status !== "open") return null;

    const requestLat = Number(request.lat);
    const requestLng = Number(request.lng);
    if (!validCoordinates(requestLat, requestLng)) {
      functions.logger.warn(`INSTANT: request ${requestId} has no valid coordinates`);
      return null;
    }

    const nowMs = Date.now();
    const expiresAtMs = timestampMillis(request.expiresAt);
    if (expiresAtMs > 0 && expiresAtMs <= nowMs) {
      functions.logger.info(`INSTANT: request ${requestId} already expired, no fanout`);
      return null;
    }

    const requestRadius = Math.max(
      1,
      Math.min(MAX_INSTANT_WORK_DISTANCE_KM, Number(request.radiusKm) || MAX_INSTANT_WORK_DISTANCE_KM),
    );
    const availabilitySnap = await db.collection("worker_availability")
      .where("isAvailable", "==", true)
      .limit(INSTANT_WORKER_SCAN_LIMIT)
      .get();

    const baseCandidates = availabilitySnap.docs
      .map((doc) => {
        const availability = doc.data() || {};
        const workerLat = Number(availability.lat);
        const workerLng = Number(availability.lng);
        if (!validCoordinates(workerLat, workerLng)) return null;

        const availableUntilMs = timestampMillis(availability.availableUntil);
        if (availableUntilMs > 0 && availableUntilMs <= nowMs) return null;

        const distance = distanceKm(requestLat, requestLng, workerLat, workerLng);
        if (distance > MAX_INSTANT_WORK_DISTANCE_KM || distance > requestRadius) return null;

        const workerId = String(availability.workerId || doc.id);
        if (!workerId || workerId === String(request.employerId || "")) return null;

        const freshnessMs = Math.max(
          timestampMillis(availability.lastActiveAt),
          timestampMillis(availability.lastSeenAt),
          timestampMillis(availability.updatedAt)
        );
        const hoursSinceFreshness = freshnessMs > 0
          ? (nowMs - freshnessMs) / (1000 * 60 * 60)
          : Number.POSITIVE_INFINITY;

        const accepted = Number(availability.instantAcceptedCount ?? availability.acceptedRequests ?? 0);
        const declined = Number(availability.instantDeclinedCount ?? availability.declinedRequests ?? 0);
        const noShow = Number(availability.instantNoShowCount ?? availability.noShowCount ?? 0);
        const decisionCount = Math.max(0, accepted) + Math.max(0, declined) + Math.max(0, noShow);
        const acceptanceRate = decisionCount > 0 ? accepted / decisionCount : 0.5;

        const freshnessScore =
          hoursSinceFreshness <= 6 ? 16 :
          hoursSinceFreshness <= 24 ? 10 :
          hoursSinceFreshness <= 72 ? 5 : 0;
        const reliabilityScore =
          decisionCount >= 3
            ? Math.max(-8, Math.min(12, Math.round((acceptanceRate - 0.5) * 24)))
            : 0;
        const distanceScore = Math.max(0, 30 - distance * 2.5);

        return {
          workerId,
          distance,
          updatedAt: timestampMillis(availability.updatedAt),
          baseRankScore: Math.round(distanceScore + freshnessScore + reliabilityScore),
          availability,
        };
      })
      .filter((item): item is {
        workerId: string;
        distance: number;
        updatedAt: number;
        baseRankScore: number;
        availability: Record<string, unknown>;
      } => item !== null);

    const workerProfiles = new Map<string, Record<string, unknown>>();
    for (const chunk of chunked(baseCandidates.map((candidate) => candidate.workerId), 10)) {
      if (chunk.length === 0) continue;
      const profileSnap = await db.collection("worker_profiles")
        .where(admin.firestore.FieldPath.documentId(), "in", chunk)
        .get();
      profileSnap.docs.forEach((doc) => {
        workerProfiles.set(doc.id, doc.data() || {});
      });
    }

    const requestCategory = inferInstantRequestCategory(request);
    const candidates = baseCandidates
      .map((candidate) => {
        const workerProfile = workerProfiles.get(candidate.workerId) || {};
        const roleFitScore = scoreInstantRoleFit(request, {
          ...candidate.availability,
          ...workerProfile,
        });
        if (requestCategory.name !== "OTHER" && roleFitScore < 0) {
          return null;
        }

        return {
          workerId: candidate.workerId,
          distance: candidate.distance,
          updatedAt: candidate.updatedAt,
          rankScore: candidate.baseRankScore + roleFitScore,
        };
      })
      .filter((item): item is { workerId: string; distance: number; updatedAt: number; rankScore: number } => item !== null)
      .sort((a, b) => b.rankScore - a.rankScore || a.distance - b.distance || b.updatedAt - a.updatedAt)
      .slice(0, INSTANT_WORKER_NOTIFY_LIMIT);

    const batch = db.batch();
    const expiresAt = expiresAtMs > nowMs
      ? admin.firestore.Timestamp.fromMillis(expiresAtMs)
      : admin.firestore.Timestamp.fromMillis(nowMs + 2 * 60 * 60 * 1000);

    candidates.forEach((candidate) => {
      const notificationRef = db.collection("notifications").doc(`instant_${requestId}_${candidate.workerId}`);
      batch.set(notificationRef, {
        recipientId: candidate.workerId,
        title: "Urgent work nearby",
        message: `${String(request.title || "Urgent help needed")} near you`,
        type: "NEW_JOB_ALERT",
        targetRole: "WORKER",
        data: {
          requestId,
          employerId: String(request.employerId || ""),
          category: String(request.category || ""),
          distanceKm: candidate.distance.toFixed(1),
          deepLink: "dutype://worker/home",
        },
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt,
      }, { merge: false });
    });

    batch.set(snapshot.ref, {
      notifiedWorkerCount: candidates.length,
      notificationFanoutAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    await batch.commit();

    functions.logger.info(`INSTANT: notified ${candidates.length} workers for ${requestId}`);
    return { notifiedWorkerCount: candidates.length };
  });

export const syncInstantResponseMetrics = functions.firestore
  .document("instant_responses/{responseId}")
  .onWrite(async (change, context) => {
    if (!change.after.exists) return null;
    const before = change.before.exists ? (change.before.data() || {}) : null;
    const after = change.after.data() || {};
    const responseId = context.params.responseId;
    const requestId = String(after.requestId || "");
    const employerId = String(after.employerId || "");
    const workerId = String(after.workerId || "");
    if (!requestId || !employerId || !workerId) return null;

    const afterStatus = String(after.status || "").toLowerCase();
    const beforeStatus = String(before?.status || "").toLowerCase();
    const requestRef = db.collection("instant_requests").doc(requestId);

    await db.runTransaction(async (tx) => {
      const requestSnap = await tx.get(requestRef);
      if (!requestSnap.exists) return;
      const request = requestSnap.data() || {};
      const updates: Record<string, unknown> = {
        lastResponseAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      if (!before) {
        updates.responseCount = admin.firestore.FieldValue.increment(1);
        if (!request.firstResponseAt) {
          updates.firstResponseAt = admin.firestore.FieldValue.serverTimestamp();
        }
      }

      if (afterStatus === "called" && beforeStatus !== "called") {
        updates.callCount = admin.firestore.FieldValue.increment(1);
      }

      const workersNeededRaw = Number(request.workersNeeded || 1);
      const workersNeeded = Number.isFinite(workersNeededRaw) ? Math.max(1, Math.floor(workersNeededRaw)) : 1;
      const selectedWorkerIds = Array.isArray(request.selectedWorkerIds)
        ? request.selectedWorkerIds.map((value) => String(value || "").trim()).filter(Boolean)
        : [];
      const currentSelectedWorkerId = String(request.selectedWorkerId || "").trim();
      if (currentSelectedWorkerId && !selectedWorkerIds.includes(currentSelectedWorkerId)) {
        selectedWorkerIds.push(currentSelectedWorkerId);
      }
      const completedWorkerIds = Array.isArray(request.completedWorkerIds)
        ? request.completedWorkerIds.map((value) => String(value || "").trim()).filter(Boolean)
        : [];
      const requestStatus = String(request.status || "").toLowerCase();
      const terminalStatus = ["cancelled", "expired", "completed"].includes(requestStatus);

      if (afterStatus === "accepted") {
        const selectedAfter = Array.from(new Set([...selectedWorkerIds, workerId]));
        updates.selectedWorkerId = workerId;
        updates.selectedWorkerIds = selectedAfter;
        if (!terminalStatus) {
          updates.status = selectedAfter.length >= workersNeeded || requestStatus === "filled" ? "filled" : "open";
          if (selectedAfter.length >= workersNeeded) updates.filledAt = admin.firestore.FieldValue.serverTimestamp();
          updates.failureReason = admin.firestore.FieldValue.delete();
        }
      } else if (afterStatus === "completed") {
        const selectedAfter = Array.from(new Set([...selectedWorkerIds, workerId]));
        const completedAfter = Array.from(new Set([...completedWorkerIds, workerId]));
        updates.selectedWorkerId = workerId;
        updates.selectedWorkerIds = selectedAfter;
        updates.completedWorkerIds = completedAfter;
        if (!terminalStatus) {
          if (completedAfter.length >= workersNeeded) {
            updates.status = "completed";
            updates.completedAt = admin.firestore.FieldValue.serverTimestamp();
          } else {
            updates.status = selectedAfter.length >= workersNeeded || requestStatus === "filled" ? "filled" : "open";
          }
          updates.failureReason = admin.firestore.FieldValue.delete();
        }
        if (after.completionProof) updates.completionProof = String(after.completionProof).slice(0, 300);
      } else if (afterStatus === "no_show") {
        const selectedAfter = selectedWorkerIds.filter((id) => id !== workerId);
        const completedAfter = completedWorkerIds.filter((id) => id !== workerId);
        updates.selectedWorkerId = selectedAfter[selectedAfter.length - 1] || "";
        updates.selectedWorkerIds = selectedAfter;
        updates.completedWorkerIds = completedAfter;
        if (!terminalStatus) {
          updates.status = selectedAfter.length >= workersNeeded ? "filled" : "open";
          updates.failureReason = admin.firestore.FieldValue.delete();
        }
      }

      tx.set(requestRef, updates, { merge: true });
    });

    if (!before && ["applied", "interested", "called"].includes(afterStatus)) {
      await db.collection("notifications").doc(`instant_response_${responseId}`).set({
        recipientId: employerId,
        title: "Worker responded",
        message: `${String(after.workerName || "A worker")} responded to your urgent request`,
        type: "NEW_APPLICATION",
        targetRole: "EMPLOYER",
        data: {
          requestId,
          workerId,
          responseId,
          deepLink: `dutype://employer/urgent/${requestId}`,
        },
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      }, { merge: false });
    }

    return null;
  });

export const expireStaleInstantRequests = functions.pubsub
  .schedule("every 15 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = admin.firestore.Timestamp.now();
    const snapshot = await db.collection("instant_requests")
      .where("status", "==", "open")
      .where("expiresAt", "<=", now)
      .limit(200)
      .get();

    if (snapshot.empty) {
      return { expired: 0 };
    }

    const batch = db.batch();
    snapshot.docs.forEach((doc) => {
      batch.set(doc.ref, {
        status: "expired",
        expiredAt: admin.firestore.FieldValue.serverTimestamp(),
        failureReason: "Expired without selection",
      }, { merge: true });
    });
    await batch.commit();

    functions.logger.info(`INSTANT: expired ${snapshot.size} stale urgent requests`);
    return { expired: snapshot.size };
  });


// ============================================
// P1 FIX #6: DUPLICATE JOB DETECTION
// ============================================
// Detects and flags duplicate/similar job postings
// Uses contact number matching + text similarity

/**
 * Calculate text similarity using Jaccard index
 */
function calculateTextSimilarity(text1: string, text2: string): number {
  if (!text1 || !text2) return 0;
  
  const words1 = new Set(text1.toLowerCase().split(/\s+/).filter(w => w.length > 3));
  const words2 = new Set(text2.toLowerCase().split(/\s+/).filter(w => w.length > 3));
  
  if (words1.size === 0 || words2.size === 0) return 0;
  
  const intersection = new Set([...words1].filter(x => words2.has(x)));
  const union = new Set([...words1, ...words2]);
  
  return intersection.size / union.size;
}

/**
 * Duplicate Job Detection - Triggered on job creation
 * Checks for:
 * 1. Same contact number from different user (suspicious)
 * 2. Similar description text (>70% match)
 * 3. Same title + location combination
 */
export const detectDuplicateJob = functions.firestore
  .document("jobmetadata/{jobId}")
  .onCreate(async (snapshot, context) => {
    const job = snapshot.data();
    const jobId = context.params.jobId;

    // employerId now lives in job_details (slim jobmetadata schema).
    const detailsSnap = await db.collection("job_details").doc(jobId).get();
    const employerId = (detailsSnap.exists ? detailsSnap.get("employerId") : null) as string | null;
    if (!employerId) {
      functions.logger.warn(`🔍 DUPLICATE CHECK: No employerId found in job_details for ${jobId}`);
      return;
    }

    functions.logger.info(`🔍 DUPLICATE CHECK: Analyzing job ${jobId}`);

    // Helper: look up employerId for a set of jobIds via job_details in parallel.
    const resolveEmployerIds = async (ids: string[]): Promise<Map<string, string>> => {
      if (ids.length === 0) return new Map();
      const refs = ids.map((id) => db.collection("job_details").doc(id));
      const snaps = await db.getAll(...refs);
      const result = new Map<string, string>();
      for (const snap of snaps) {
        const eid = snap.exists ? (snap.get("employerId") as string | undefined) : undefined;
        if (eid) result.set(snap.id, eid);
      }
      return result;
    };

    try {
      let fraudScore = 0;
      const signals: string[] = [];
      const now = Date.now();
      const twentyFourHoursAgo = now - ONE_DAY_MS;
      const twentyFourHoursAgoTs = admin.firestore.Timestamp.fromMillis(twentyFourHoursAgo);

      // CHECK 1: Same contact number from DIFFERENT user (HIGH SUSPICION)
      if (job.contactNumber) {
        const sameContactJobs = await db.collection("jobmetadata")
          .where("contactNumber", "==", job.contactNumber)
          .where("createdAt", ">", twentyFourHoursAgoTs)
          .limit(10)
          .get();

        const candidateIds = sameContactJobs.docs.map((d) => d.id).filter((id) => id !== jobId);
        const employerMap = await resolveEmployerIds(candidateIds);
        const differentUserSameContact = candidateIds.filter((id) => {
          const eid = employerMap.get(id);
          return eid && eid !== employerId;
        });

        if (differentUserSameContact.length > 0) {
          fraudScore += 50;
          signals.push("SAME_CONTACT_DIFFERENT_USER");
          functions.logger.warn(`🔍 DUPLICATE: Same contact ${job.contactNumber} used by different user!`);
        }
      }

      // CHECK 2: Similar description (>70% match)
      const recentJobsSnap = await db.collection("jobmetadata")
        .where("createdAt", ">", twentyFourHoursAgoTs)
        .limit(50)
        .get();

      const recentIds = recentJobsSnap.docs.map((d) => d.id).filter((id) => id !== jobId);
      const recentEmployerMap = await resolveEmployerIds(recentIds);
      const recentDetailsMap = new Map<string, FirebaseFirestore.DocumentData>();
      {
        const detailRefs = recentIds.map((id) => db.collection("job_details").doc(id));
        if (detailRefs.length > 0) {
          const detailSnaps = await db.getAll(...detailRefs);
          for (const ds of detailSnaps) {
            if (ds.exists) recentDetailsMap.set(ds.id, ds.data() || {});
          }
        }
      }

      for (const recentJob of recentJobsSnap.docs) {
        if (recentJob.id === jobId) continue;
        const otherEmployerId = recentEmployerMap.get(recentJob.id);
        if (!otherEmployerId || otherEmployerId === employerId) continue;

        const otherDescription = recentDetailsMap.get(recentJob.id)?.description || "";
        const similarity = calculateTextSimilarity(
          job.description || "",
          otherDescription
        );

        if (similarity > 0.7) {
          fraudScore += 40;
          signals.push(`SIMILAR_DESCRIPTION_${Math.round(similarity * 100)}%`);
          functions.logger.warn(`🔍 DUPLICATE: ${Math.round(similarity * 100)}% similar to job ${recentJob.id}`);
          break; // One match is enough
        }
      }

      // CHECK 3: Same title + same area (within 1km)
      const jobLat = typeof job.latitude === "number"
        ? job.latitude
        : typeof job.location?.lat === "number"
          ? job.location.lat
          : null;
      const jobLng = typeof job.longitude === "number"
        ? job.longitude
        : typeof job.location?.lng === "number"
          ? job.location.lng
          : null;

      if (job.title && jobLat !== null && jobLng !== null) {
        const sameTitleJobs = await db.collection("jobmetadata")
          .where("title", "==", job.title)
          .where("createdAt", ">", twentyFourHoursAgoTs)
          .limit(20)
          .get();

        const titleCandidateIds = sameTitleJobs.docs.map((d) => d.id).filter((id) => id !== jobId);
        const titleEmployerMap = await resolveEmployerIds(titleCandidateIds);

        for (const sameTitleJob of sameTitleJobs.docs) {
          if (sameTitleJob.id === jobId) continue;
          const otherJob = sameTitleJob.data();
          const otherEmployerId = titleEmployerMap.get(sameTitleJob.id);
          if (!otherEmployerId) continue;

          const otherLat = typeof otherJob.latitude === "number"
            ? otherJob.latitude
            : typeof otherJob.location?.lat === "number"
              ? otherJob.location.lat
              : null;
          const otherLng = typeof otherJob.longitude === "number"
            ? otherJob.longitude
            : typeof otherJob.location?.lng === "number"
              ? otherJob.location.lng
              : null;

          if (otherLat !== null && otherLng !== null) {
            // Simple distance check (approximate)
            const latDiff = Math.abs(jobLat - otherLat);
            const lngDiff = Math.abs(jobLng - otherLng);
            const isNearby = latDiff < 0.01 && lngDiff < 0.01; // ~1km

            if (isNearby && otherEmployerId !== employerId) {
              fraudScore += 30;
              signals.push("SAME_TITLE_SAME_AREA");
              functions.logger.warn(`🔍 DUPLICATE: Same title "${job.title}" in same area`);
              break;
            }
          }
        }
      }

      // DETERMINE ACTION based on fraud score
      let moderationStatus = "AUTO_APPROVED";
      
      if (fraudScore >= 70) {
        // HIGH RISK: Auto-reject and hide
        moderationStatus = "AUTO_REJECTED";
        await snapshot.ref.update({
          status: "closed",
          moderationStatus: "AUTO_REJECTED",
          moderationReason: signals.join(", "),
          fraudScore: fraudScore,
        });
        
        functions.logger.warn(`🔍 DUPLICATE: ⛔ Job ${jobId} AUTO-REJECTED (score: ${fraudScore})`);
        
      } else if (fraudScore >= 40) {
        // MEDIUM RISK: Send to moderation queue
        moderationStatus = "PENDING_REVIEW";
        await snapshot.ref.update({
          moderationStatus: "PENDING_REVIEW",
          moderationReason: signals.join(", "),
          fraudScore: fraudScore,
        });
        
        // Create an in-app notification for employer review.
        const modLocale = await getUserLanguage(db, employerId);
        const modRecipient = await getUserDisplayName(db, employerId);
        await db.collection("notifications").add({
          recipientId: employerId,
          title: tTitle("JOB_UNDER_REVIEW", modLocale, { title: job.title, recipient: modRecipient }),
          message: tBody("JOB_UNDER_REVIEW", modLocale, { title: job.title, recipient: modRecipient }),
          type: "MODERATION_REVIEW_REQUIRED",
          data: {
            jobId: jobId,
            fraudScore: fraudScore,
          },
          isRead: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        
        functions.logger.info(`🔍 DUPLICATE: ⚠️ Job ${jobId} sent to moderation (score: ${fraudScore})`);
        
      } else {
        // LOW RISK: Auto-approve
        await snapshot.ref.update({
          moderationStatus: "AUTO_APPROVED",
          fraudScore: fraudScore,
        });
        functions.logger.info(`🔍 DUPLICATE: ✅ Job ${jobId} passed duplicate check (score: ${fraudScore})`);
      }

      return { fraudScore, signals, moderationStatus };

    } catch (error) {
      functions.logger.error(`🔍 DUPLICATE: Error checking job ${jobId}:`, error);
      return null;
    }
  });


// ============================================
// P1 FIX #7: IP ADDRESS TRACKING & ANALYSIS
// ============================================
// Tracks IP addresses for fraud detection
// Detects scam networks operating from same location

/**
 * Log user activity with IP address
 * Called from Android app via HTTPS callable function
 */
export const logUserActivity = functions.https.onCall(async (data, context) => {
  // Activity logging removed - Firebase Analytics handles this
  return { success: true };
});

// ============================================
// P1 FIX #10: MODERATION QUEUE SYSTEM
// ============================================
// Jobs with medium fraud score go to moderation queue
// Admin approves/rejects before job goes live

/**
 * Process moderation decision
 * Called when admin approves or rejects a job in moderation queue
 */
export const processModerationDecision = functions.firestore
  .document("jobmetadata/{jobId}")
  .onUpdate(async () => {
    // Moderation decisions are handled by the current job status workflow.
    return null;
  });




// ============================================
// P1: COMMUNITY REPORTING SYSTEM
// ============================================
// 3 reports = auto-hide job
// Crowd-sourced moderation for clean platform

const AUTO_HIDE_THRESHOLD = 3;

/**
 * Process job report and auto-hide if threshold reached
 * Triggered when a new report is created
 */
export const processJobReport = functions.firestore
  .document("job_reports/{reportId}")
  .onCreate(async (snapshot, context) => {
    const report = snapshot.data();
    const reportId = context.params.reportId;
    const jobId = report.jobId;

    if (!jobId) {
      functions.logger.warn(`Report ${reportId} has no jobId, skipping`);
      return null;
    }

    functions.logger.info(`🚨 REPORT: Processing report ${reportId} for job ${jobId}`);

    try {
      // Get job document
      const jobRef = db.collection("jobmetadata").doc(jobId);
      const jobDoc = await jobRef.get();

      if (!jobDoc.exists) {
        functions.logger.warn(`Job ${jobId} not found`);
        return null;
      }

      const jobData = jobDoc.data();

      // Count reports from job_reports collection
      const reportsSnapshot = await db.collection("job_reports")
        .where("jobId", "==", jobId)
        .get();
      const currentReportCount = reportsSnapshot.size;

      const reportTypeCounts: Record<string, number> = {};
      const sortedReports = reportsSnapshot.docs
        .map((doc) => {
          const data = doc.data() as Record<string, unknown>;
          const rawCreatedAt = data.createdAt;
          const createdAtMs = rawCreatedAt instanceof admin.firestore.Timestamp
            ? rawCreatedAt.toMillis()
            : (typeof rawCreatedAt === "number" ? rawCreatedAt : 0);
          return { data, createdAtMs };
        })
        .sort((a, b) => b.createdAtMs - a.createdAtMs);

      for (const item of sortedReports) {
        const type = typeof item.data.reportType === "string" && item.data.reportType.trim().length > 0
          ? item.data.reportType
          : "OTHER";
        reportTypeCounts[type] = (reportTypeCounts[type] || 0) + 1;
      }

      const reportSamples = sortedReports.slice(0, 5).map((item) => {
        const type = typeof item.data.reportType === "string" && item.data.reportType.trim().length > 0
          ? item.data.reportType
          : "OTHER";
        const rawDescription = typeof item.data.description === "string"
          ? item.data.description.trim()
          : "";
        const description = (rawDescription.length > 220
          ? `${rawDescription.slice(0, 220)}...`
          : rawDescription) || "No details provided";
        const createdAt = item.data.createdAt instanceof admin.firestore.Timestamp
          ? item.data.createdAt
          : null;
        return {
          reportType: type,
          description,
          createdAt,
        };
      });

      const jobAggregateUpdate: Record<string, unknown> = {
        reportCount: currentReportCount,
        reportTypeCounts,
        reportSamples,
        lastReportedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      // Check if threshold reached
      if (currentReportCount >= AUTO_HIDE_THRESHOLD) {
        functions.logger.warn(`🚨 REPORT: Job ${jobId} reached ${currentReportCount} reports - AUTO-HIDING`);

        // Deactivate the job
        jobAggregateUpdate.status = "closed";
        jobAggregateUpdate.moderationStatus = "HIDDEN_BY_REPORTS";

        // Notify employer through the current inbox workflow.
        const detailsSnapForReport = await db.collection("job_details").doc(jobId).get();
        const reportEmployerId = detailsSnapForReport.exists
          ? (detailsSnapForReport.get("employerId") as string | undefined)
          : undefined;
        if (reportEmployerId) {
          const hideLocale = await getUserLanguage(db, reportEmployerId);
          const hideRecipient = await getUserDisplayName(db, reportEmployerId);
          await db.collection("notifications").add({
            recipientId: reportEmployerId,
            title: tTitle("JOB_HIDDEN_REPORTS", hideLocale, { title: jobData?.title ?? "", recipient: hideRecipient }),
            message: tBody("JOB_HIDDEN_REPORTS", hideLocale, { title: jobData?.title ?? "", recipient: hideRecipient }),
            type: "JOB_HIDDEN",
            data: {
              jobId: jobId,
              reportCount: currentReportCount,
            },
            isRead: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });
        }

        
      }

      await jobRef.set(jobAggregateUpdate, { merge: true });

      const jobDetailsRef = db.collection("job_details").doc(jobId);
      const jobDetailsDoc = await jobDetailsRef.get();
      if (jobDetailsDoc.exists) {
        await jobDetailsRef.set(
          {
            reportCount: currentReportCount,
            reportTypeCounts,
            reportSamples,
            lastReportedAt: admin.firestore.FieldValue.serverTimestamp(),
          },
          { merge: true }
        );
      }

      functions.logger.info(`🚨 REPORT: Job ${jobId} now has ${currentReportCount} reports`);
      return { success: true, reportCount: currentReportCount };

    } catch (error) {
      functions.logger.error(`🚨 REPORT: Error processing report:`, error);
      return null;
    }
  });

/**
 * Get report statistics for admin dashboard
 */
export const getReportStats = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  try {
    const now = Date.now();
    const oneDayAgo = now - (24 * 60 * 60 * 1000);
    const oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000);
    const oneDayAgoTs = admin.firestore.Timestamp.fromMillis(oneDayAgo);
    const oneWeekAgoTs = admin.firestore.Timestamp.fromMillis(oneWeekAgo);

    // Get reports from last 24 hours
    const dailyReports = await db.collection("job_reports")
      .where("createdAt", ">", oneDayAgoTs)
      .get();

    // Get reports from last week
    const weeklyReports = await db.collection("job_reports")
      .where("createdAt", ">", oneWeekAgoTs)
      .get();

    return {
      dailyReports: dailyReports.size,
      weeklyReports: weeklyReports.size,
      pendingModeration: 0,
    };

  } catch (error) {
    functions.logger.error("Error getting report stats:", error);
    throw new functions.https.HttpsError("internal", "Failed to get report stats");
  }
});


// ============================================
// METADATA UPDATE FUNCTIONS
// ============================================

/**
 * Scheduled function to update platform metadata
 * Runs every hour to update job counts, category stats, etc.
 */
export const updatePlatformMetadata = functions.pubsub
  .schedule("every 1 hours")
  .onRun(async (context) => {
    functions.logger.info("📊 METADATA: disabled (metadata collection removed in final schema)");
    return null;
  });

/**
 * Trigger to update metadata when a new job is created
 */
export const updateMetadataOnJobCreate = functions.firestore
  .document("jobmetadata/{jobId}")
  .onCreate(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job create skipped (metadata removed)");
    return null;
  });

/**
 * Trigger to update metadata when a job is deleted
 */
export const updateMetadataOnJobDelete = functions.firestore
  .document("jobmetadata/{jobId}")
  .onDelete(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job delete skipped (metadata removed)");
    return null;
  });

// ============================================
// ENTERPRISE REFERRAL SYSTEM EXPORTS
// ============================================
// Import and re-export referral system functions
export {
  onWorkerProfileReferralReady,
  onEmployerProfileReferralReady,
  ensureUserReferralCode,
  applyReferralCode,
  expirePendingReferrals,
  requestWithdrawal,
  detectReferralFraud,
  getReferralStats,
  getReferralHistory,
  getReferralLeaderboard
} from "./referral-system";




// ============================================
// EXPORT JOB POSTING FUNCTIONS
// ============================================
export * from "./job-posting";

// ============================================
// EXPORT COVER LETTER SIGNED URL
// ============================================
export * from "./cover-letter";

// ============================================
// EXPORT AGGREGATE MAINTAINERS + EXPIRY SWEEP
// ============================================
export * from "./aggregates";
export * from "./employer-cards";
export * from "./job-expiry";

// ============================================
// EXPORT NOTIFICATION FAN-OUT
// ============================================
export * from "./notification-fanout";

// ============================================
// EXPORT APP CONFIG (admin-editable referral rewards)
// ============================================
export { updateReferralConfig, getReferralConfigCallable } from "./app-config";
