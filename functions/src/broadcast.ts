/**
 * Admin broadcasts: a broadcast_notifications/{id} document (written by the web admin) is sent to an
 * FCM topic — per language when translations are given.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { SUPPORTED_LOCALES, localizedTopic } from "./notification-i18n";

const messaging = admin.messaging();

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
export const sendBroadcastNotification = functions
  .region("asia-south1")
  .firestore
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
            ...(typeof notification.deepLink === "string" && notification.deepLink ? { deepLink: notification.deepLink } : {}),
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
