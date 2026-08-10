"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onApplicationCreated = exports.onApplicationStatusChanged = void 0;
/**
 * Notification fan-out triggers.
 *
 * Client rules forbid direct notifications/* creates, so every transactional
 * notification must come from a CF. This module fires notifications for:
 *   • application status transitions (shortlisted / hired / rejected) → notify worker
 *   • new application created → notify employer
 *
 * Localisation: every notification respects `user_tokens/{uid}.language`. When the
 * recipient has no stored language, English is used.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const notification_i18n_1 = require("./notification-i18n");
const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;
async function createNotification(n) {
    const ref = db.collection("notifications").doc(n.id);
    let created = false;
    await db.runTransaction(async (transaction) => {
        var _a;
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
            data: Object.assign(Object.assign({}, ((_a = n.data) !== null && _a !== void 0 ? _a : {})), { targetRole: n.targetRole }),
            locale: n.locale,
            isRead: false,
            createdAt: FIELD.serverTimestamp(),
            expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 30 * 24 * 60 * 60 * 1000),
        });
        created = true;
    });
    return created;
}
exports.onApplicationStatusChanged = functions.firestore
    .document("applications/{applicationId}")
    .onUpdate(async (change) => {
    var _a, _b, _c, _d, _e;
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String((_a = before.status) !== null && _a !== void 0 ? _a : "");
    const next = String((_b = after.status) !== null && _b !== void 0 ? _b : "");
    const previousStatus = prev.toLowerCase();
    const nextStatus = next.toLowerCase() === "accepted" ? "hired" : next.toLowerCase();
    if (previousStatus === nextStatus)
        return null;
    const workerId = String((_c = after.workerId) !== null && _c !== void 0 ? _c : "");
    const jobId = String((_d = after.jobId) !== null && _d !== void 0 ? _d : "");
    if (!workerId)
        return null;
    // The worker triggered this one, so the employer is the side that needs to act.
    if (nextStatus === "work_submitted") {
        const employerId = String((_e = after.employerId) !== null && _e !== void 0 ? _e : "");
        if (!employerId)
            return null;
        const employerLocale = await (0, notification_i18n_1.getUserLanguage)(db, employerId);
        const employerName = await (0, notification_i18n_1.getUserDisplayName)(db, employerId);
        const workerName = await (0, notification_i18n_1.getUserDisplayName)(db, workerId);
        await createNotification({
            id: `app_status_${change.after.id}_work_submitted`,
            recipientId: employerId,
            targetRole: "EMPLOYER",
            title: (0, notification_i18n_1.tTitle)("WORK_SUBMITTED_CONFIRM", employerLocale, { recipient: employerName, worker: workerName }),
            body: (0, notification_i18n_1.tBody)("WORK_SUBMITTED_CONFIRM", employerLocale, { recipient: employerName, worker: workerName }),
            type: "APPLICATION_STATUS",
            data: {
                type: "APPLICATION_STATUS",
                jobId,
                applicationId: change.after.id,
                status: nextStatus,
                deepLink: `dutype://employer/applications/${change.after.id}`,
            },
            locale: employerLocale,
        });
        return null;
    }
    const locale = await (0, notification_i18n_1.getUserLanguage)(db, workerId);
    const recipient = await (0, notification_i18n_1.getUserDisplayName)(db, workerId);
    const templateId = nextStatus === "hired" ? "APPLICATION_HIRED" :
        nextStatus === "shortlisted" ? "APPLICATION_SHORTLISTED" :
            nextStatus === "rejected" ? "APPLICATION_REJECTED" :
                nextStatus === "withdrawn" ? "APPLICATION_WITHDRAWN" :
                    "APPLICATION_STATUS_OTHER";
    const params = { recipient };
    if (templateId === "APPLICATION_STATUS_OTHER")
        params.status = nextStatus;
    const title = (0, notification_i18n_1.tTitle)(templateId, locale, params);
    const body = (0, notification_i18n_1.tBody)(templateId, locale, params);
    const dataMap = {
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
exports.onApplicationCreated = functions.firestore
    .document("applications/{applicationId}")
    .onCreate(async (snap) => {
    var _a, _b, _c, _d;
    const data = snap.data() || {};
    const employerId = String((_a = data.employerId) !== null && _a !== void 0 ? _a : "");
    const jobId = String((_b = data.jobId) !== null && _b !== void 0 ? _b : "");
    const workerId = String((_c = data.workerId) !== null && _c !== void 0 ? _c : "");
    if (!employerId)
        return null;
    const locale = await (0, notification_i18n_1.getUserLanguage)(db, employerId);
    const [recipient, workerName, jobSnap] = await Promise.all([
        (0, notification_i18n_1.getUserDisplayName)(db, employerId),
        (0, notification_i18n_1.getUserDisplayName)(db, workerId, "A worker"),
        jobId ? db.doc(`jobmetadata/${jobId}`).get().catch(() => null) : Promise.resolve(null),
    ]);
    const jobTitle = jobSnap && jobSnap.exists ? String((_d = jobSnap.get("title")) !== null && _d !== void 0 ? _d : "") : "";
    const params = {
        recipient,
        workerName,
        jobTitle: jobTitle ? `"${jobTitle}"` : "",
    };
    const title = (0, notification_i18n_1.tTitle)("NEW_APPLICATION_RECEIVED", locale, params);
    const body = (0, notification_i18n_1.tBody)("NEW_APPLICATION_RECEIVED", locale, params);
    const dataMap = {
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
//# sourceMappingURL=notification-fanout.js.map