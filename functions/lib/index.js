"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __exportStar = (this && this.__exportStar) || function(m, exports) {
    for (var p in m) if (p !== "default" && !Object.prototype.hasOwnProperty.call(exports, p)) __createBinding(exports, m, p);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.getReferralConfigCallable = exports.updateReferralConfig = exports.getReferralLeaderboard = exports.getReferralHistory = exports.getReferralStats = exports.detectReferralFraud = exports.requestWithdrawal = exports.expirePendingReferrals = exports.applyReferralCode = exports.ensureUserReferralCode = exports.onEmployerProfileReferralReady = exports.onWorkerProfileReferralReady = exports.updateMetadataOnJobDelete = exports.updateMetadataOnJobCreate = exports.updatePlatformMetadata = exports.getReportStats = exports.processJobReport = exports.processModerationDecision = exports.logUserActivity = exports.detectDuplicateJob = exports.expireStaleInstantRequests = exports.syncInstantResponseMetrics = exports.notifyAvailableWorkersForInstantRequest = exports.persistSelfNotification = exports.sendPushNotification = exports.sendBroadcastNotification = exports.cleanupExpiredNotifications = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const validation_1 = require("./validation");
const notification_i18n_1 = require("./notification-i18n");
// Initialize Firebase Admin SDK
admin.initializeApp();
// ============================================
// SCHEDULED NOTIFICATIONS (Enterprise Grade)
// ============================================
__exportStar(require("./scheduled-notifications"), exports);
var scheduled_notifications_1 = require("./scheduled-notifications");
Object.defineProperty(exports, "cleanupExpiredNotifications", { enumerable: true, get: function () { return scheduled_notifications_1.cleanupExpiredNotifications; } });
__exportStar(require("./referral-system"), exports);
__exportStar(require("./job-landing"), exports);
__exportStar(require("./worker-landing"), exports);
__exportStar(require("./employer-landing"), exports);
__exportStar(require("./auth-callables"), exports);
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
function timestampMillis(value) {
    if (value instanceof admin.firestore.Timestamp) {
        return value.toMillis();
    }
    if (typeof value === "number") {
        return value;
    }
    if (typeof value === "object" && value !== null && "toMillis" in value) {
        const maybeTimestamp = value;
        if (typeof maybeTimestamp.toMillis === "function") {
            return maybeTimestamp.toMillis();
        }
    }
    return 0;
}
function distanceKm(lat1, lng1, lat2, lng2) {
    const toRadians = (degrees) => degrees * Math.PI / 180;
    const earthRadiusKm = 6371;
    const dLat = toRadians(lat2 - lat1);
    const dLng = toRadians(lng2 - lng1);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2);
    return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
function validCoordinates(lat, lng) {
    return typeof lat === "number" && typeof lng === "number" &&
        lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 &&
        (lat !== 0 || lng !== 0);
}
function sanitizeNotificationDataMap(rawData) {
    if (!rawData || typeof rawData !== "object") {
        return {};
    }
    const dataObject = rawData;
    const cleanData = {};
    for (const [rawKey, rawValue] of Object.entries(dataObject).slice(0, MAX_SELF_NOTIFICATION_DATA_KEYS)) {
        const key = String(rawKey).trim().slice(0, 64);
        if (!key)
            continue;
        const value = String(rawValue !== null && rawValue !== void 0 ? rawValue : "").trim().slice(0, 500);
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
exports.sendBroadcastNotification = functions.firestore
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
        ? notification.translations
        : null;
    try {
        const sendToOne = async (sendTopic, sendTitle, sendMessage) => {
            const topicMessage = {
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
        const responses = {};
        if (translations) {
            for (const lang of notification_i18n_1.SUPPORTED_LOCALES) {
                const t = translations[lang];
                const localizedTitle = ((t === null || t === void 0 ? void 0 : t.title) && t.title.trim()) || fallbackTitle;
                const localizedMessage = ((t === null || t === void 0 ? void 0 : t.message) && t.message.trim()) || fallbackMessage;
                const langTopic = (0, notification_i18n_1.localizedTopic)(topic, lang);
                const resp = await sendToOne(langTopic, localizedTitle, localizedMessage);
                responses[lang] = resp;
                functions.logger.info(`Broadcast (${lang}) sent to ${langTopic}: ${resp}`);
            }
        }
        else {
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
    }
    catch (error) {
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
exports.sendPushNotification = functions.firestore
    .document("notifications/{notificationId}")
    .onCreate(async (snapshot, context) => {
    var _a;
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
            if ((freshData === null || freshData === void 0 ? void 0 : freshData.sentAt) || (freshData === null || freshData === void 0 ? void 0 : freshData.processing)) {
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
        const getCreatedAtMs = (rawCreatedAt) => {
            if (rawCreatedAt instanceof admin.firestore.Timestamp) {
                return rawCreatedAt.toMillis();
            }
            if (typeof rawCreatedAt === "number") {
                return rawCreatedAt;
            }
            if (typeof rawCreatedAt === "object" && rawCreatedAt !== null && "toMillis" in rawCreatedAt) {
                const maybeTimestamp = rawCreatedAt;
                if (typeof maybeTimestamp.toMillis === "function") {
                    return maybeTimestamp.toMillis();
                }
            }
            return 0;
        };
        const matchingNotifications = candidateNotifications.docs.filter((doc) => {
            const data = doc.data();
            if (data.title !== notification.title)
                return false;
            if (data.type !== notification.type)
                return false;
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
        if (!(tokenData === null || tokenData === void 0 ? void 0 : tokenData.fcmToken)) {
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
        let effectiveLocale = (0, notification_i18n_1.normalizeLocale)(notification.locale);
        const templateId = typeof notification.templateId === "string" ? notification.templateId : "";
        if (templateId) {
            effectiveLocale = await (0, notification_i18n_1.getUserLanguage)(db, recipientId);
            const params = (notification.params && typeof notification.params === "object")
                ? notification.params
                : undefined;
            effectiveTitle = (0, notification_i18n_1.tTitle)(templateId, effectiveLocale, params);
            effectiveMessage = (0, notification_i18n_1.tBody)(templateId, effectiveLocale, params);
        }
        // Extract deep link from notification data
        const deepLink = ((_a = notification.data) === null || _a === void 0 ? void 0 : _a.deepLink) || "";
        // Map notification type to Android channel ID
        const notificationType = notification.type || "general";
        const highPriorityTypes = ["BIRTHDAY", "JOB_EXPIRY", "APPLICATION_STATUS", "JOB_ALERT", "NEW_JOB_ALERT", "NEW_APPLICATION", "APPLICATION_WITHDRAWN", "WORKER_HIRED", "PROFILE_COMPLETE", "JOB_POSTED", "SHORTLISTED", "REJECTED", "APPLICATION_STATUS_UPDATE", "WELCOME"];
        const mediumPriorityTypes = ["PENDING_APPLICATIONS", "JOB_RECOMMENDATION", "REMINDER", "INTERVIEW_SCHEDULED"];
        let channelId = "low_priority";
        if (highPriorityTypes.includes(notificationType)) {
            channelId = "high_priority";
        }
        else if (mediumPriorityTypes.includes(notificationType)) {
            channelId = "medium_priority";
        }
        // Build the FCM message.
        // DATA-ONLY message (no android.notification block) so that onMessageReceived()
        // is ALWAYS called by DutyPeMessagingService regardless of whether the app is
        // in foreground, background, or killed. This gives the app full control over
        // how the notification is displayed and ensures deep-links work correctly.
        const message = {
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
        const updatePayload = {
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
    }
    catch (error) {
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
exports.persistSelfNotification = functions.https.onCall(async (data, context) => {
    var _a;
    if (!((_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid)) {
        throw new functions.https.HttpsError("unauthenticated", "Authentication required");
    }
    const userId = context.auth.uid;
    const title = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.title, "title", {
        required: true,
        minLength: 1,
        maxLength: 120,
    });
    const message = (0, validation_1.validateMessage)(data === null || data === void 0 ? void 0 : data.message, 700);
    const notificationType = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.type, "type", {
        required: true,
        minLength: 1,
        maxLength: 64,
    }).toUpperCase();
    if (!ALLOWED_SELF_NOTIFICATION_TYPES.has(notificationType)) {
        throw new functions.https.HttpsError("invalid-argument", `Unsupported self notification type: ${notificationType}`);
    }
    const targetRoleRaw = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.targetRole, "targetRole", {
        required: false,
        maxLength: 16,
    }).toUpperCase();
    const targetRole = targetRoleRaw === "WORKER" || targetRoleRaw === "EMPLOYER"
        ? targetRoleRaw
        : "";
    const payloadData = sanitizeNotificationDataMap(data === null || data === void 0 ? void 0 : data.data);
    const requestedNotificationId = typeof (data === null || data === void 0 ? void 0 : data.notificationId) === "string"
        ? data.notificationId.trim()
        : "";
    const notificationId = /^[A-Za-z0-9_-]{8,128}$/.test(requestedNotificationId)
        ? requestedNotificationId
        : db.collection("notifications").doc().id;
    const nowMs = Date.now();
    const requestedExpiresAt = Number(data === null || data === void 0 ? void 0 : data.expiresAt);
    const maxAllowedExpiry = nowMs + SELF_NOTIFICATION_RETENTION_MS;
    const resolvedExpiryMs = Number.isFinite(requestedExpiresAt) && requestedExpiresAt > nowMs
        ? Math.min(requestedExpiresAt, maxAllowedExpiry)
        : maxAllowedExpiry;
    const notificationRef = db.collection("notifications").doc(notificationId);
    await notificationRef.set(Object.assign(Object.assign({ recipientId: userId, title,
        message, type: notificationType }, (targetRole ? { targetRole } : {})), { data: payloadData, isRead: false, createdAt: admin.firestore.FieldValue.serverTimestamp(), expiresAt: admin.firestore.Timestamp.fromMillis(resolvedExpiryMs), skipPush: true }));
    return {
        success: true,
        notificationId: notificationRef.id,
    };
});
exports.notifyAvailableWorkersForInstantRequest = functions.firestore
    .document("instant_requests/{requestId}")
    .onCreate(async (snapshot, context) => {
    const request = snapshot.data() || {};
    const requestId = context.params.requestId;
    const status = String(request.status || "open").toLowerCase();
    if (status !== "open")
        return null;
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
    const requestRadius = Math.max(1, Math.min(MAX_INSTANT_WORK_DISTANCE_KM, Number(request.radiusKm) || MAX_INSTANT_WORK_DISTANCE_KM));
    const availabilitySnap = await db.collection("worker_availability")
        .where("isAvailable", "==", true)
        .limit(INSTANT_WORKER_SCAN_LIMIT)
        .get();
    const candidates = availabilitySnap.docs
        .map((doc) => {
        const availability = doc.data() || {};
        const workerLat = Number(availability.lat);
        const workerLng = Number(availability.lng);
        if (!validCoordinates(workerLat, workerLng))
            return null;
        const availableUntilMs = timestampMillis(availability.availableUntil);
        if (availableUntilMs > 0 && availableUntilMs <= nowMs)
            return null;
        const distance = distanceKm(requestLat, requestLng, workerLat, workerLng);
        if (distance > MAX_INSTANT_WORK_DISTANCE_KM || distance > requestRadius)
            return null;
        const workerId = String(availability.workerId || doc.id);
        if (!workerId || workerId === String(request.employerId || ""))
            return null;
        return {
            workerId,
            distance,
            updatedAt: timestampMillis(availability.updatedAt),
        };
    })
        .filter((item) => item !== null)
        .sort((a, b) => a.distance - b.distance || b.updatedAt - a.updatedAt)
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
exports.syncInstantResponseMetrics = functions.firestore
    .document("instant_responses/{responseId}")
    .onWrite(async (change, context) => {
    if (!change.after.exists)
        return null;
    const before = change.before.exists ? (change.before.data() || {}) : null;
    const after = change.after.data() || {};
    const responseId = context.params.responseId;
    const requestId = String(after.requestId || "");
    const employerId = String(after.employerId || "");
    const workerId = String(after.workerId || "");
    if (!requestId || !employerId || !workerId)
        return null;
    const afterStatus = String(after.status || "").toLowerCase();
    const beforeStatus = String((before === null || before === void 0 ? void 0 : before.status) || "").toLowerCase();
    const requestRef = db.collection("instant_requests").doc(requestId);
    await db.runTransaction(async (tx) => {
        const requestSnap = await tx.get(requestRef);
        if (!requestSnap.exists)
            return;
        const request = requestSnap.data() || {};
        const updates = {
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
        if (afterStatus === "accepted") {
            updates.status = "filled";
            updates.selectedWorkerId = workerId;
        }
        else if (afterStatus === "completed") {
            updates.status = "completed";
            updates.selectedWorkerId = workerId;
            updates.completedAt = admin.firestore.FieldValue.serverTimestamp();
            if (after.completionProof)
                updates.completionProof = String(after.completionProof).slice(0, 300);
        }
        else if (afterStatus === "no_show") {
            updates.status = "failed";
            updates.selectedWorkerId = workerId;
            updates.failureReason = String(after.failureReason || "Worker did not show up").slice(0, 300);
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
exports.expireStaleInstantRequests = functions.pubsub
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
function calculateTextSimilarity(text1, text2) {
    if (!text1 || !text2)
        return 0;
    const words1 = new Set(text1.toLowerCase().split(/\s+/).filter(w => w.length > 3));
    const words2 = new Set(text2.toLowerCase().split(/\s+/).filter(w => w.length > 3));
    if (words1.size === 0 || words2.size === 0)
        return 0;
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
exports.detectDuplicateJob = functions.firestore
    .document("jobmetadata/{jobId}")
    .onCreate(async (snapshot, context) => {
    var _a, _b, _c, _d, _e;
    const job = snapshot.data();
    const jobId = context.params.jobId;
    // employerId now lives in job_details (slim jobmetadata schema).
    const detailsSnap = await db.collection("job_details").doc(jobId).get();
    const employerId = (detailsSnap.exists ? detailsSnap.get("employerId") : null);
    if (!employerId) {
        functions.logger.warn(`🔍 DUPLICATE CHECK: No employerId found in job_details for ${jobId}`);
        return;
    }
    functions.logger.info(`🔍 DUPLICATE CHECK: Analyzing job ${jobId}`);
    // Helper: look up employerId for a set of jobIds via job_details in parallel.
    const resolveEmployerIds = async (ids) => {
        if (ids.length === 0)
            return new Map();
        const refs = ids.map((id) => db.collection("job_details").doc(id));
        const snaps = await db.getAll(...refs);
        const result = new Map();
        for (const snap of snaps) {
            const eid = snap.exists ? snap.get("employerId") : undefined;
            if (eid)
                result.set(snap.id, eid);
        }
        return result;
    };
    try {
        let fraudScore = 0;
        const signals = [];
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
        const recentDetailsMap = new Map();
        {
            const detailRefs = recentIds.map((id) => db.collection("job_details").doc(id));
            if (detailRefs.length > 0) {
                const detailSnaps = await db.getAll(...detailRefs);
                for (const ds of detailSnaps) {
                    if (ds.exists)
                        recentDetailsMap.set(ds.id, ds.data() || {});
                }
            }
        }
        for (const recentJob of recentJobsSnap.docs) {
            if (recentJob.id === jobId)
                continue;
            const otherEmployerId = recentEmployerMap.get(recentJob.id);
            if (!otherEmployerId || otherEmployerId === employerId)
                continue;
            const otherDescription = ((_a = recentDetailsMap.get(recentJob.id)) === null || _a === void 0 ? void 0 : _a.description) || "";
            const similarity = calculateTextSimilarity(job.description || "", otherDescription);
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
            : typeof ((_b = job.location) === null || _b === void 0 ? void 0 : _b.lat) === "number"
                ? job.location.lat
                : null;
        const jobLng = typeof job.longitude === "number"
            ? job.longitude
            : typeof ((_c = job.location) === null || _c === void 0 ? void 0 : _c.lng) === "number"
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
                if (sameTitleJob.id === jobId)
                    continue;
                const otherJob = sameTitleJob.data();
                const otherEmployerId = titleEmployerMap.get(sameTitleJob.id);
                if (!otherEmployerId)
                    continue;
                const otherLat = typeof otherJob.latitude === "number"
                    ? otherJob.latitude
                    : typeof ((_d = otherJob.location) === null || _d === void 0 ? void 0 : _d.lat) === "number"
                        ? otherJob.location.lat
                        : null;
                const otherLng = typeof otherJob.longitude === "number"
                    ? otherJob.longitude
                    : typeof ((_e = otherJob.location) === null || _e === void 0 ? void 0 : _e.lng) === "number"
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
        }
        else if (fraudScore >= 40) {
            // MEDIUM RISK: Send to moderation queue
            moderationStatus = "PENDING_REVIEW";
            await snapshot.ref.update({
                moderationStatus: "PENDING_REVIEW",
                moderationReason: signals.join(", "),
                fraudScore: fraudScore,
            });
            // Create an in-app notification for employer review.
            const modLocale = await (0, notification_i18n_1.getUserLanguage)(db, employerId);
            const modRecipient = await (0, notification_i18n_1.getUserDisplayName)(db, employerId);
            await db.collection("notifications").add({
                recipientId: employerId,
                title: (0, notification_i18n_1.tTitle)("JOB_UNDER_REVIEW", modLocale, { title: job.title, recipient: modRecipient }),
                message: (0, notification_i18n_1.tBody)("JOB_UNDER_REVIEW", modLocale, { title: job.title, recipient: modRecipient }),
                type: "MODERATION_REVIEW_REQUIRED",
                data: {
                    jobId: jobId,
                    fraudScore: fraudScore,
                },
                isRead: false,
                createdAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            functions.logger.info(`🔍 DUPLICATE: ⚠️ Job ${jobId} sent to moderation (score: ${fraudScore})`);
        }
        else {
            // LOW RISK: Auto-approve
            await snapshot.ref.update({
                moderationStatus: "AUTO_APPROVED",
                fraudScore: fraudScore,
            });
            functions.logger.info(`🔍 DUPLICATE: ✅ Job ${jobId} passed duplicate check (score: ${fraudScore})`);
        }
        return { fraudScore, signals, moderationStatus };
    }
    catch (error) {
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
exports.logUserActivity = functions.https.onCall(async (data, context) => {
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
exports.processModerationDecision = functions.firestore
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
exports.processJobReport = functions.firestore
    .document("job_reports/{reportId}")
    .onCreate(async (snapshot, context) => {
    var _a, _b;
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
        const reportTypeCounts = {};
        const sortedReports = reportsSnapshot.docs
            .map((doc) => {
            const data = doc.data();
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
        const jobAggregateUpdate = {
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
                ? detailsSnapForReport.get("employerId")
                : undefined;
            if (reportEmployerId) {
                const hideLocale = await (0, notification_i18n_1.getUserLanguage)(db, reportEmployerId);
                const hideRecipient = await (0, notification_i18n_1.getUserDisplayName)(db, reportEmployerId);
                await db.collection("notifications").add({
                    recipientId: reportEmployerId,
                    title: (0, notification_i18n_1.tTitle)("JOB_HIDDEN_REPORTS", hideLocale, { title: (_a = jobData === null || jobData === void 0 ? void 0 : jobData.title) !== null && _a !== void 0 ? _a : "", recipient: hideRecipient }),
                    message: (0, notification_i18n_1.tBody)("JOB_HIDDEN_REPORTS", hideLocale, { title: (_b = jobData === null || jobData === void 0 ? void 0 : jobData.title) !== null && _b !== void 0 ? _b : "", recipient: hideRecipient }),
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
            await jobDetailsRef.set({
                reportCount: currentReportCount,
                reportTypeCounts,
                reportSamples,
                lastReportedAt: admin.firestore.FieldValue.serverTimestamp(),
            }, { merge: true });
        }
        functions.logger.info(`🚨 REPORT: Job ${jobId} now has ${currentReportCount} reports`);
        return { success: true, reportCount: currentReportCount };
    }
    catch (error) {
        functions.logger.error(`🚨 REPORT: Error processing report:`, error);
        return null;
    }
});
/**
 * Get report statistics for admin dashboard
 */
exports.getReportStats = functions.https.onCall(async (data, context) => {
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
    }
    catch (error) {
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
exports.updatePlatformMetadata = functions.pubsub
    .schedule("every 1 hours")
    .onRun(async (context) => {
    functions.logger.info("📊 METADATA: disabled (metadata collection removed in final schema)");
    return null;
});
/**
 * Trigger to update metadata when a new job is created
 */
exports.updateMetadataOnJobCreate = functions.firestore
    .document("jobmetadata/{jobId}")
    .onCreate(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job create skipped (metadata removed)");
    return null;
});
/**
 * Trigger to update metadata when a job is deleted
 */
exports.updateMetadataOnJobDelete = functions.firestore
    .document("jobmetadata/{jobId}")
    .onDelete(async (snapshot, context) => {
    functions.logger.info("📊 METADATA: update on job delete skipped (metadata removed)");
    return null;
});
// ============================================
// ENTERPRISE REFERRAL SYSTEM EXPORTS
// ============================================
// Import and re-export referral system functions
var referral_system_1 = require("./referral-system");
Object.defineProperty(exports, "onWorkerProfileReferralReady", { enumerable: true, get: function () { return referral_system_1.onWorkerProfileReferralReady; } });
Object.defineProperty(exports, "onEmployerProfileReferralReady", { enumerable: true, get: function () { return referral_system_1.onEmployerProfileReferralReady; } });
Object.defineProperty(exports, "ensureUserReferralCode", { enumerable: true, get: function () { return referral_system_1.ensureUserReferralCode; } });
Object.defineProperty(exports, "applyReferralCode", { enumerable: true, get: function () { return referral_system_1.applyReferralCode; } });
Object.defineProperty(exports, "expirePendingReferrals", { enumerable: true, get: function () { return referral_system_1.expirePendingReferrals; } });
Object.defineProperty(exports, "requestWithdrawal", { enumerable: true, get: function () { return referral_system_1.requestWithdrawal; } });
Object.defineProperty(exports, "detectReferralFraud", { enumerable: true, get: function () { return referral_system_1.detectReferralFraud; } });
Object.defineProperty(exports, "getReferralStats", { enumerable: true, get: function () { return referral_system_1.getReferralStats; } });
Object.defineProperty(exports, "getReferralHistory", { enumerable: true, get: function () { return referral_system_1.getReferralHistory; } });
Object.defineProperty(exports, "getReferralLeaderboard", { enumerable: true, get: function () { return referral_system_1.getReferralLeaderboard; } });
// ============================================
// EXPORT JOB POSTING FUNCTIONS
// ============================================
__exportStar(require("./job-posting"), exports);
// ============================================
// EXPORT COVER LETTER SIGNED URL
// ============================================
__exportStar(require("./cover-letter"), exports);
// ============================================
// EXPORT AGGREGATE MAINTAINERS + EXPIRY SWEEP
// ============================================
__exportStar(require("./aggregates"), exports);
__exportStar(require("./employer-cards"), exports);
__exportStar(require("./job-expiry"), exports);
// ============================================
// EXPORT NOTIFICATION FAN-OUT
// ============================================
__exportStar(require("./notification-fanout"), exports);
// ============================================
// EXPORT APP CONFIG (admin-editable referral rewards)
// ============================================
var app_config_1 = require("./app-config");
Object.defineProperty(exports, "updateReferralConfig", { enumerable: true, get: function () { return app_config_1.updateReferralConfig; } });
Object.defineProperty(exports, "getReferralConfigCallable", { enumerable: true, get: function () { return app_config_1.getReferralConfigCallable; } });
//# sourceMappingURL=index.js.map