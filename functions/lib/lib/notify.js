"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.notify = void 0;
/**
 * The one way the server tells a user something: an inbox document (notifications, TTL on
 * expireAt) plus an FCM data push, written/sent directly — no trigger, no lock, no duplicate
 * scan. Cost per notification: 1 token read (+1 profile read only when the template greets by
 * name) and 1 write.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const schema_1 = require("../schema");
const notification_i18n_1 = require("../notification-i18n");
const db = admin.firestore();
const INBOX_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const HIGH_PRIORITY = new Set([
    "NEW_APPLICATION", "APPLICATION_STATUS", "WORKER_HIRED", "REJECTED", "JOB_EXPIRY_REMINDER",
    "NEW_JOB_ALERT", "PAYMENT", "BIRTHDAY", "WELCOME",
]);
async function recipientName(uid, role) {
    const ref = role === schema_1.Values.Role.EMPLOYER ?
        db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid) :
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid);
    const snap = await ref.get();
    const name = String(snap.get(role === schema_1.Values.Role.EMPLOYER ? schema_1.EmployerProfiles.OWNER_NAME : schema_1.WorkerProfiles.NAME) || "");
    return name.trim().split(/\s+/)[0] || "";
}
function usesRecipient(templateId) {
    const t = notification_i18n_1.NOTIFICATION_TEMPLATES[templateId];
    return !!t && JSON.stringify(t).includes("{recipient}");
}
/** Sends one notice to one user. Never throws: a failed push must not fail the caller's action. */
async function notify(uid, notice) {
    try {
        const tokenSnap = await db.collection(schema_1.UserTokens.COLLECTION).doc(uid).get();
        const locale = (0, notification_i18n_1.normalizeLocale)(tokenSnap.get(schema_1.UserTokens.LANGUAGE));
        const params = Object.assign({}, (notice.params || {}));
        if (params.recipient === undefined && usesRecipient(notice.templateId)) {
            params.recipient = await recipientName(uid, notice.role);
        }
        const title = (0, notification_i18n_1.tTitle)(notice.templateId, locale, params).replace(/\s+,/, ",").replace(/^(\S+) ,/, "$1,");
        const body = (0, notification_i18n_1.tBody)(notice.templateId, locale, params);
        const data = notice.data || {};
        const now = Date.now();
        const ref = db.collection(schema_1.Notifications.COLLECTION).doc();
        await ref.set({
            [schema_1.Notifications.RECIPIENT_ID]: uid,
            [schema_1.Notifications.TITLE]: title,
            [schema_1.Notifications.BODY]: body,
            [schema_1.Notifications.TYPE]: notice.type,
            [schema_1.Notifications.DATA]: data,
            [schema_1.Notifications.READ]: false,
            [schema_1.Notifications.CREATED_AT]: admin.firestore.Timestamp.fromMillis(now),
            [schema_1.Notifications.EXPIRE_AT]: admin.firestore.Timestamp.fromMillis(now + INBOX_TTL_MS),
        });
        const token = tokenSnap.get(schema_1.UserTokens.FCM_TOKEN);
        if (typeof token !== "string" || !token)
            return;
        try {
            await admin.messaging().send({
                token,
                data: Object.assign(Object.assign({}, data), { notificationId: ref.id, title,
                    body, type: notice.type, channel: HIGH_PRIORITY.has(notice.type) ? "high_priority" : "low_priority", locale }),
                android: { priority: "high" },
            });
        }
        catch (error) {
            const code = error.code || "";
            if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
                await tokenSnap.ref.update({ [schema_1.UserTokens.FCM_TOKEN]: admin.firestore.FieldValue.delete() });
            }
            else {
                throw error;
            }
        }
    }
    catch (error) {
        functions.logger.warn(`notify(${uid}, ${notice.type}) failed`, error);
    }
}
exports.notify = notify;
//# sourceMappingURL=notify.js.map