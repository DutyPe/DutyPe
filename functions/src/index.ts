import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

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
 * Triggered when a new notification document is created in Firestore
 * Sends push notification to the recipient's device
 */
export const sendPushNotification = functions.firestore
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
      if (!tokenData?.isActive || !tokenData?.token) {
        functions.logger.warn(`FCM token inactive or missing for user: ${recipientId}`);
        return null;
      }

      const fcmToken = tokenData.token;

      // Build the FCM message
      const message: admin.messaging.Message = {
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
    } catch (error) {
      functions.logger.error("Error sending notification:", error);
      
      // Update notification with error status
      await snapshot.ref.update({
        error: String(error),
        sentAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      
      return null;
    }
  });
