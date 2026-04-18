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
 * Notifications are short-lived (auto-cleaned by scheduled-notifications
 * cleanup), so we keep the payload minimal.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;
async function createNotification(n) {
    var _a;
    const ref = db.collection("notifications").doc();
    await ref.set({
        id: ref.id,
        recipientId: n.recipientId,
        title: n.title,
        body: n.body,
        type: n.type,
        relatedId: (_a = n.relatedId) !== null && _a !== void 0 ? _a : null,
        isRead: false,
        createdAt: FIELD.serverTimestamp(),
    });
}
async function sendFcmToUser(userId, title, body, data) {
    var _a;
    try {
        const userSnap = await db.doc(`users/${userId}`).get();
        const token = userSnap.exists ? String((_a = userSnap.get("fcmToken")) !== null && _a !== void 0 ? _a : "") : "";
        if (!token)
            return;
        await admin.messaging().send({
            token,
            notification: { title, body },
            data,
            android: { priority: "high" },
        });
    }
    catch (e) {
        functions.logger.warn("sendFcmToUser failed", { userId, err: e === null || e === void 0 ? void 0 : e.message });
    }
}
/**
 * Notify the worker whenever their application changes status.
 */
exports.onApplicationStatusChanged = functions.firestore
    .document("applications/{applicationId}")
    .onUpdate(async (change) => {
    var _a, _b, _c, _d;
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String((_a = before.status) !== null && _a !== void 0 ? _a : "");
    const next = String((_b = after.status) !== null && _b !== void 0 ? _b : "");
    if (prev === next)
        return;
    const workerId = String((_c = after.workerId) !== null && _c !== void 0 ? _c : "");
    const jobId = String((_d = after.jobId) !== null && _d !== void 0 ? _d : "");
    if (!workerId)
        return;
    const title = next === "hired" ? "You're hired! 🎉" :
        next === "shortlisted" ? "You've been shortlisted" :
            next === "rejected" ? "Application update" :
                "Application update";
    const body = next === "hired" ? "An employer has accepted your application." :
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
exports.onApplicationCreated = functions.firestore
    .document("applications/{applicationId}")
    .onCreate(async (snap) => {
    var _a, _b;
    const data = snap.data() || {};
    const employerId = String((_a = data.employerId) !== null && _a !== void 0 ? _a : "");
    const jobId = String((_b = data.jobId) !== null && _b !== void 0 ? _b : "");
    if (!employerId)
        return;
    const title = "New application received";
    const body = "A worker has applied to your job posting.";
    await Promise.all([
        createNotification({ recipientId: employerId, title, body, type: "NEW_APPLICATION", relatedId: jobId }),
        sendFcmToUser(employerId, title, body, { type: "NEW_APPLICATION", jobId, applicationId: snap.id }),
    ]);
});
//# sourceMappingURL=notification-fanout.js.map