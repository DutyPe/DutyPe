/**
 * The one way the server tells a user something: an inbox document (notifications, TTL on
 * expireAt) plus an FCM data push, written/sent directly — no trigger, no lock, no duplicate
 * scan. Cost per notification: 1 token read (+1 profile read only when the template greets by
 * name) and 1 write.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { Notifications, UserTokens, WorkerProfiles, EmployerProfiles, Values } from "../schema";
import { normalizeLocale, tBody, tTitle, NOTIFICATION_TEMPLATES } from "../notification-i18n";

const db = admin.firestore();
const INBOX_TTL_MS = 30 * 24 * 60 * 60 * 1000;

const HIGH_PRIORITY = new Set([
  "NEW_APPLICATION", "APPLICATION_STATUS", "WORKER_HIRED", "REJECTED", "JOB_EXPIRY_REMINDER",
  "NEW_JOB_ALERT", "PAYMENT", "BIRTHDAY", "WELCOME", "SERVICE_BOOKING",
]);

export interface Notice {
  /** App NotificationType name (drives the channel and the deep link). */
  type: string;
  /** Key in NOTIFICATION_TEMPLATES. */
  templateId: keyof typeof NOTIFICATION_TEMPLATES;
  params?: Record<string, string | number>;
  /** String key/values the app needs to route the tap (jobId, applicationId, ...). */
  data?: Record<string, string>;
  /** The recipient's role, so a "{recipient}" greeting reads the right profile. */
  role?: string;
}

async function recipientName(uid: string, role?: string): Promise<string> {
  const ref = role === Values.Role.EMPLOYER ?
    db.collection(EmployerProfiles.COLLECTION).doc(uid) :
    db.collection(WorkerProfiles.COLLECTION).doc(uid);
  const snap = await ref.get();
  const name = String(snap.get(role === Values.Role.EMPLOYER ? EmployerProfiles.OWNER_NAME : WorkerProfiles.NAME) || "");
  return name.trim().split(/\s+/)[0] || "";
}

function usesRecipient(templateId: string): boolean {
  const t = NOTIFICATION_TEMPLATES[templateId];
  return !!t && JSON.stringify(t).includes("{recipient}");
}

/** Sends one notice to one user. Never throws: a failed push must not fail the caller's action. */
export async function notify(uid: string, notice: Notice): Promise<void> {
  try {
    const tokenSnap = await db.collection(UserTokens.COLLECTION).doc(uid).get();
    const locale = normalizeLocale(tokenSnap.get(UserTokens.LANGUAGE));
    const params: Record<string, string | number> = { ...(notice.params || {}) };
    if (params.recipient === undefined && usesRecipient(notice.templateId)) {
      params.recipient = await recipientName(uid, notice.role);
    }
    const title = tTitle(notice.templateId, locale, params).replace(/\s+,/, ",").replace(/^(\S+) ,/, "$1,");
    const body = tBody(notice.templateId, locale, params);
    const data = notice.data || {};
    const now = Date.now();

    const ref = db.collection(Notifications.COLLECTION).doc();
    await ref.set({
      [Notifications.RECIPIENT_ID]: uid,
      [Notifications.TITLE]: title,
      [Notifications.BODY]: body,
      [Notifications.TYPE]: notice.type,
      [Notifications.DATA]: data,
      [Notifications.READ]: false,
      [Notifications.CREATED_AT]: admin.firestore.Timestamp.fromMillis(now),
      [Notifications.EXPIRE_AT]: admin.firestore.Timestamp.fromMillis(now + INBOX_TTL_MS),
    });

    const token = tokenSnap.get(UserTokens.FCM_TOKEN);
    if (typeof token !== "string" || !token) return;
    try {
      await admin.messaging().send({
        token,
        data: {
          ...data,
          notificationId: ref.id,
          title,
          body,
          type: notice.type,
          channel: HIGH_PRIORITY.has(notice.type) ? "high_priority" : "low_priority",
          locale,
        },
        android: { priority: "high" },
      });
    } catch (error) {
      const code = (error as { code?: string }).code || "";
      if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
        await tokenSnap.ref.update({ [UserTokens.FCM_TOKEN]: admin.firestore.FieldValue.delete() });
      } else {
        throw error;
      }
    }
  } catch (error) {
    functions.logger.warn(`notify(${uid}, ${notice.type}) failed`, error);
  }
}
