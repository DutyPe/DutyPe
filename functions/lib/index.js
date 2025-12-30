"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendPushNotification = exports.sendBroadcastNotification = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();
// Topic constants (must match Android app)
const TOPIC_ALL_USERS = "all_users";
const TOPIC_WORKERS = "workers";
const TOPIC_EMPLOYERS = "employers";
const TOPIC_APP_UPDATES = "app_updates";
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
    const title = notification.title || "DutyPe";
    const message = notification.message || "";
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
    try {
        // Build the FCM message for topic
        const topicMessage = {
            topic: topic,
            data: {
                notificationId: notificationId,
                title: title,
                message: message,
                body: message,
                type: type,
                click_action: "FLUTTER_NOTIFICATION_CLICK",
            },
            android: {
                priority: "high",
                notification: {
                    title: title,
                    body: message,
                    icon: "ic_notification",
                    color: "#3B82F6",
                    sound: "default",
                    clickAction: "OPEN_ACTIVITY",
                },
            },
        };
        // Send to topic
        const response = await messaging.send(topicMessage);
        functions.logger.info(`Broadcast notification sent to topic ${topic}: ${response}`);
        // Update document with sent status
        await snapshot.ref.update({
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            fcmMessageId: response,
            status: "sent",
        });
        return response;
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
 */
exports.sendPushNotification = functions.firestore
    .document("notifications/{notificationId}")
    .onCreate(async (snapshot, context) => {
    const notification = snapshot.data();
    const notificationId = context.params.notificationId;
    functions.logger.info(`Processing notification: ${notificationId}`, notification);
    const recipientId = notification.recipientId;
    if (!recipientId) {
        functions.logger.warn("No recipientId in notification, skipping");
        return null;
    }
    try {
        // Get recipient's FCM token
        const tokenDoc = await db.collection("fcm_tokens").doc(recipientId).get();
        if (!tokenDoc.exists) {
            functions.logger.warn(`No FCM token found for user: ${recipientId}`);
            return null;
        }
        const tokenData = tokenDoc.data();
        if (!(tokenData === null || tokenData === void 0 ? void 0 : tokenData.isActive) || !(tokenData === null || tokenData === void 0 ? void 0 : tokenData.token)) {
            functions.logger.warn(`FCM token inactive or missing for user: ${recipientId}`);
            return null;
        }
        const fcmToken = tokenData.token;
        // Build the FCM message
        const message = {
            token: fcmToken,
            data: {
                notificationId: notificationId,
                title: notification.title || "DutyPe",
                message: notification.message || "",
                body: notification.message || "",
                type: notification.type || "general",
                action: notification.action || "",
                jobId: notification.jobId || "",
                applicationId: notification.applicationId || "",
                click_action: "FLUTTER_NOTIFICATION_CLICK",
            },
            android: {
                priority: "high",
                notification: {
                    title: notification.title || "DutyPe",
                    body: notification.message || "",
                    icon: "ic_notification",
                    color: "#3B82F6",
                    sound: "default",
                    clickAction: "OPEN_ACTIVITY",
                },
            },
        };
        // Send the notification
        const response = await messaging.send(message);
        functions.logger.info(`Notification sent successfully: ${response}`);
        // Update notification document with sent status
        await snapshot.ref.update({
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
            fcmMessageId: response,
        });
        return response;
    }
    catch (error) {
        functions.logger.error("Error sending notification:", error);
        // Update notification with error status
        await snapshot.ref.update({
            error: String(error),
            sentAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return null;
    }
});
//# sourceMappingURL=index.js.map