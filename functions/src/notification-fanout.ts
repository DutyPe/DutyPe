/**
 * Notification fan-out triggers.
 *
 * Client rules forbid direct notifications/* creates, so every transactional
 * notification must come from a CF. This module fires notifications for:
 *   • application status transitions (shortlisted / hired / rejected) → notify worker
 *   • new application created → notify employer
 *
 * Localisation: every notification respects `users/{uid}.language`. When the
 * recipient has no stored language, English is used.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { getUserLanguage, getUserDisplayName, tTitle, tBody, SupportedLocale } from "./notification-i18n";

const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;

interface NotificationPayload {
  recipientId: string;
  title: string;
  body: string;
  type: string;
  data?: Record<string, string>;
  locale: SupportedLocale;
}

async function createNotification(n: NotificationPayload): Promise<void> {
  const ref = db.collection("notifications").doc();
  await ref.set({
    recipientId: n.recipientId,
    title: n.title,
    message: n.body,
    type: n.type,
    ...(n.data ? { data: n.data } : {}),
    isRead: false,
    createdAt: FIELD.serverTimestamp(),
  });
}

async function sendFcmToUser(
  userId: string,
  title: string,
  body: string,
  data: Record<string, string>
): Promise<void> {
  try {
    const userSnap = await db.doc(`users/${userId}`).get();
    const token = userSnap.exists ? String(userSnap.get("fcmToken") ?? "") : "";
    if (!token) return;
    await admin.messaging().send({
      token,
      notification: { title, body },
      data,
      android: { priority: "high" },
    });
  } catch (e: any) {
    functions.logger.warn("sendFcmToUser failed", { userId, err: e?.message });
  }
}

export const onApplicationStatusChanged = functions.firestore
  .document("applications/{applicationId}")
  .onUpdate(async (change) => {
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String(before.status ?? "");
    const next = String(after.status ?? "");
    if (prev === next) return;

    const workerId = String(after.workerId ?? "");
    const jobId = String(after.jobId ?? "");
    if (!workerId) return;

    const locale = await getUserLanguage(db, workerId);
    const recipient = await getUserDisplayName(db, workerId);

    const templateId =
      next === "hired" ? "APPLICATION_HIRED" :
      next === "shortlisted" ? "APPLICATION_SHORTLISTED" :
      next === "rejected" ? "APPLICATION_REJECTED" :
      "APPLICATION_STATUS_OTHER";

    const params: Record<string, string | number> = { recipient };
    if (templateId === "APPLICATION_STATUS_OTHER") params.status = next;
    const title = tTitle(templateId, locale, params);
    const body = tBody(templateId, locale, params);

    const dataMap: Record<string, string> = {
      type: "APPLICATION_STATUS",
      jobId,
      applicationId: change.after.id,
    };
    await Promise.all([
      createNotification({ recipientId: workerId, title, body, type: "APPLICATION_STATUS", data: dataMap, locale }),
      sendFcmToUser(workerId, title, body, { ...dataMap, locale }),
    ]);
  });

export const onApplicationCreated = functions.firestore
  .document("applications/{applicationId}")
  .onCreate(async (snap) => {
    const data = snap.data() || {};
    const employerId = String(data.employerId ?? "");
    const jobId = String(data.jobId ?? "");
    const workerId = String(data.workerId ?? "");
    if (!employerId) return;

    const locale = await getUserLanguage(db, employerId);
    const [recipient, workerName, jobSnap] = await Promise.all([
      getUserDisplayName(db, employerId),
      getUserDisplayName(db, workerId, "A worker"),
      jobId ? db.doc(`jobmetadata/${jobId}`).get().catch(() => null) : Promise.resolve(null),
    ]);
    const jobTitle = jobSnap && jobSnap.exists ? String(jobSnap.get("title") ?? "") : "";

    const params: Record<string, string | number> = {
      recipient,
      workerName,
      jobTitle: jobTitle ? `"${jobTitle}"` : "",
    };
    const title = tTitle("NEW_APPLICATION_RECEIVED", locale, params);
    const body = tBody("NEW_APPLICATION_RECEIVED", locale, params);

    const dataMap: Record<string, string> = {
      type: "NEW_APPLICATION",
      jobId,
      applicationId: snap.id,
    };
    await Promise.all([
      createNotification({ recipientId: employerId, title, body, type: "NEW_APPLICATION", data: dataMap, locale }),
      sendFcmToUser(employerId, title, body, { ...dataMap, locale }),
    ]);
  });
