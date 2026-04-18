/**
 * Notification fan-out triggers.
 *
 * Client rules forbid direct notifications/* creates, so every transactional
 * notification must come from a CF. This module fires notifications for:
 *   • application status transitions (shortlisted / hired / rejected) → notify worker
 *   • new application created → notify employer
 *
 * Notifications are short-lived (auto-cleaned by scheduled-notifications
 * cleanup), so we keep the payload minimal.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;

interface NotificationPayload {
  recipientId: string;
  title: string;
  body: string;
  type: string;
  relatedId?: string;
}

async function createNotification(n: NotificationPayload): Promise<void> {
  const ref = db.collection("notifications").doc();
  await ref.set({
    id: ref.id,
    recipientId: n.recipientId,
    title: n.title,
    body: n.body,
    type: n.type,
    relatedId: n.relatedId ?? null,
    isRead: false,
    createdAt: FIELD.serverTimestamp(),
  });
}

async function sendFcmToUser(userId: string, title: string, body: string, data: Record<string, string>): Promise<void> {
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

/**
 * Notify the worker whenever their application changes status.
 */
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

    const title =
      next === "hired" ? "You're hired! 🎉" :
      next === "shortlisted" ? "You've been shortlisted" :
      next === "rejected" ? "Application update" :
      "Application update";
    const body =
      next === "hired" ? "An employer has accepted your application." :
      next === "shortlisted" ? "An employer is reviewing your application." :
      next === "rejected" ? "Your application wasn't selected this time." :
      `Status: ${next}`;

    await Promise.all([
      createNotification({ recipientId: workerId, title, body, type: "APPLICATION_STATUS", relatedId: jobId }),
      sendFcmToUser(workerId, title, body, { type: "APPLICATION_STATUS", jobId, applicationId: change.after.id }),
    ]);
  });

/**
 * Notify the employer whenever a new application lands on their job.
 */
export const onApplicationCreated = functions.firestore
  .document("applications/{applicationId}")
  .onCreate(async (snap) => {
    const data = snap.data() || {};
    const employerId = String(data.employerId ?? "");
    const jobId = String(data.jobId ?? "");
    if (!employerId) return;

    const title = "New application received";
    const body = "A worker has applied to your job posting.";

    await Promise.all([
      createNotification({ recipientId: employerId, title, body, type: "NEW_APPLICATION", relatedId: jobId }),
      sendFcmToUser(employerId, title, body, { type: "NEW_APPLICATION", jobId, applicationId: snap.id }),
    ]);
  });
