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
  id: string;
  recipientId: string;
  targetRole: "WORKER" | "EMPLOYER";
  title: string;
  body: string;
  type: string;
  data?: Record<string, string>;
  locale: SupportedLocale;
}

async function createNotification(n: NotificationPayload): Promise<boolean> {
  const ref = db.collection("notifications").doc(n.id);
  let created = false;

  await db.runTransaction(async (transaction) => {
    const existing = await transaction.get(ref);
    if (existing.exists) {
      return;
    }

    transaction.set(ref, {
      recipientId: n.recipientId,
      targetRole: n.targetRole,
      title: n.title,
      message: n.body,
      type: n.type,
      data: {
        ...(n.data ?? {}),
        targetRole: n.targetRole,
      },
      locale: n.locale,
      isRead: false,
      createdAt: FIELD.serverTimestamp(),
      expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000),
    });
    created = true;
  });

  return created;
}

export const onApplicationStatusChanged = functions.firestore
  .document("applications/{applicationId}")
  .onUpdate(async (change) => {
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String(before.status ?? "");
    const next = String(after.status ?? "");
    const previousStatus = prev.toLowerCase();
    const nextStatus = next.toLowerCase() === "accepted" ? "hired" : next.toLowerCase();
    if (previousStatus === nextStatus) return null;

    const workerId = String(after.workerId ?? "");
    const jobId = String(after.jobId ?? "");
    if (!workerId) return null;

    const locale = await getUserLanguage(db, workerId);
    const recipient = await getUserDisplayName(db, workerId);

    const templateId =
      nextStatus === "hired" ? "APPLICATION_HIRED" :
      nextStatus === "shortlisted" ? "APPLICATION_SHORTLISTED" :
      nextStatus === "rejected" ? "APPLICATION_REJECTED" :
      nextStatus === "withdrawn" ? "APPLICATION_WITHDRAWN" :
      "APPLICATION_STATUS_OTHER";

    const params: Record<string, string | number> = { recipient };
    if (templateId === "APPLICATION_STATUS_OTHER") params.status = nextStatus;
    const title = tTitle(templateId, locale, params);
    const body = tBody(templateId, locale, params);

    const dataMap: Record<string, string> = {
      type: "APPLICATION_STATUS",
      jobId,
      applicationId: change.after.id,
      status: nextStatus,
      deepLink: `dutype://worker/applications/${change.after.id}`,
    };
    const id = `app_status_${change.after.id}_${nextStatus}`;
    await createNotification({ id, recipientId: workerId, targetRole: "WORKER", title, body, type: "APPLICATION_STATUS", data: dataMap, locale });
    return null;
  });

export const onApplicationCreated = functions.firestore
  .document("applications/{applicationId}")
  .onCreate(async (snap) => {
    const data = snap.data() || {};
    const employerId = String(data.employerId ?? "");
    const jobId = String(data.jobId ?? "");
    const workerId = String(data.workerId ?? "");
    if (!employerId) return null;

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
      workerId,
      deepLink: `dutype://employer/applications/${snap.id}`,
    };
    const id = `new_application_${snap.id}`;
    await createNotification({ id, recipientId: employerId, targetRole: "EMPLOYER", title, body, type: "NEW_APPLICATION", data: dataMap, locale });
    return null;
  });
