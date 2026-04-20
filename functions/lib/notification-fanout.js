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
 * Localisation: every notification respects `users/{uid}.language`. When the
 * recipient has no stored language, English is used.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const notification_i18n_1 = require("./notification-i18n");
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
        message: n.body,
        type: n.type,
        relatedId: (_a = n.relatedId) !== null && _a !== void 0 ? _a : null,
        locale: n.locale,
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
    const locale = await (0, notification_i18n_1.getUserLanguage)(db, workerId);
    const templateId = next === "hired" ? "APPLICATION_HIRED" :
        next === "shortlisted" ? "APPLICATION_SHORTLISTED" :
            next === "rejected" ? "APPLICATION_REJECTED" :
                "APPLICATION_STATUS_OTHER";
    const params = templateId === "APPLICATION_STATUS_OTHER" ? { status: next } : undefined;
    const title = (0, notification_i18n_1.tTitle)(templateId, locale, params);
    const body = (0, notification_i18n_1.tBody)(templateId, locale, params);
    await Promise.all([
        createNotification({ recipientId: workerId, title, body, type: "APPLICATION_STATUS", relatedId: jobId, locale }),
        sendFcmToUser(workerId, title, body, {
            type: "APPLICATION_STATUS",
            jobId,
            applicationId: change.after.id,
            locale,
        }),
    ]);
});
exports.onApplicationCreated = functions.firestore
    .document("applications/{applicationId}")
    .onCreate(async (snap) => {
    var _a, _b;
    const data = snap.data() || {};
    const employerId = String((_a = data.employerId) !== null && _a !== void 0 ? _a : "");
    const jobId = String((_b = data.jobId) !== null && _b !== void 0 ? _b : "");
    if (!employerId)
        return;
    const locale = await (0, notification_i18n_1.getUserLanguage)(db, employerId);
    const title = (0, notification_i18n_1.tTitle)("NEW_APPLICATION_RECEIVED", locale);
    const body = (0, notification_i18n_1.tBody)("NEW_APPLICATION_RECEIVED", locale);
    await Promise.all([
        createNotification({ recipientId: employerId, title, body, type: "NEW_APPLICATION", relatedId: jobId, locale }),
        sendFcmToUser(employerId, title, body, {
            type: "NEW_APPLICATION",
            jobId,
            applicationId: snap.id,
            locale,
        }),
    ]);
});
//# sourceMappingURL=notification-fanout.js.map