"use strict";
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.requestNotification = exports.notifyApplicationEvent = exports.emitApplicationNotifications = void 0;
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const crypto_1 = require("crypto");
const validation_1 = require("./validation");
const db = admin.firestore();
const retention = 45 * 24 * 60 * 60 * 1000;
async function persistEvents(transaction, events) {
    if (events.length === 0)
        return 0;
    const refs = events.map(event => db.collection("notification_events").doc((0, crypto_1.createHash)("sha256").update(event.key).digest("hex")));
    const receipts = await transaction.getAll(...refs);
    let created = 0;
    const now = Date.now();
    events.forEach((event, index) => {
        if (receipts[index].exists)
            return;
        const { key } = event, notification = __rest(event, ["key"]);
        const id = refs[index].id;
        transaction.create(refs[index], { eventKey: key, notificationId: id, createdAt: now });
        transaction.create(db.collection("notifications").doc(id), Object.assign(Object.assign({}, notification), { id, createdAt: now, expiresAt: now + retention, isRead: false }));
        created += 1;
    });
    return created;
}
async function emitApplicationNotifications(applicationId, requesterId, requestedRecipient, reminder = false) {
    return db.runTransaction(async (transaction) => {
        const applicationDoc = await transaction.get(db.collection("job_applications").doc(applicationId));
        if (!applicationDoc.exists)
            throw new functions.https.HttpsError("not-found", "Application not found");
        const application = applicationDoc.data();
        const participants = [application.workerId, application.employerId];
        if ((requesterId && !participants.includes(requesterId)) ||
            (requestedRecipient && !participants.includes(requestedRecipient))) {
            throw new functions.https.HttpsError("permission-denied", "Notification is not for this application");
        }
        const job = await transaction.get(db.collection("jobs").doc(application.jobId));
        if (!job.exists || job.get("employerId") !== application.employerId) {
            throw new functions.https.HttpsError("failed-precondition", "Job ownership does not match the application");
        }
        const jobTitle = String(job.get("title") || "this job").slice(0, 150);
        const status = application.status;
        const statusTitles = {
            PENDING: "Application Submitted", UNDER_REVIEW: "Application Under Review", ACCEPTED: "Application Accepted",
            IN_PROGRESS: "Work Started", COMPLETED: "Work Completed", REJECTED: "Application Update", WITHDRAWN: "Application Withdrawn"
        };
        if (!statusTitles[status])
            throw new functions.https.HttpsError("failed-precondition", "Unknown application state");
        if (reminder && (status !== "PENDING" || application.active === false ||
            typeof application.appliedAt !== "number" || Date.now() - application.appliedAt < 24 * 60 * 60 * 1000)) {
            return 0;
        }
        const suffix = reminder ? `reminder:${new Date().toISOString().slice(0, 10)}` : status;
        const event = (recipientId, targetRole, type, title) => ({
            key: `application:${applicationId}:${suffix}:${recipientId}`,
            recipientId, targetRole, type, title,
            message: reminder ? `Your application for ${jobTitle} is still pending.` : `${jobTitle}: ${statusTitles[status]}.`,
            data: { applicationId, jobId: application.jobId, status,
                deepLink: targetRole === "EMPLOYER" ? `dutype://employer/applications/${applicationId}` : `dutype://worker/applications/${applicationId}` }
        });
        const events = [event(application.workerId, "WORKER", "APPLICATION_STATUS", reminder ? "Application Still Pending" : statusTitles[status])];
        if (!reminder && ["PENDING", "ACCEPTED", "WITHDRAWN"].includes(status)) {
            events.push(event(application.employerId, "EMPLOYER", status === "PENDING" ? "NEW_APPLICATION" : "APPLICATION_STATUS", status === "PENDING" ? "New Application Received" : statusTitles[status]));
        }
        return persistEvents(transaction, events);
    });
}
exports.emitApplicationNotifications = emitApplicationNotifications;
exports.notifyApplicationEvent = functions.firestore.document("job_applications/{applicationId}").onWrite(async (change, context) => {
    if (!change.after.exists || (change.before.exists && change.before.get("status") === change.after.get("status")))
        return;
    await emitApplicationNotifications(context.params.applicationId);
});
exports.requestNotification = functions.https.onCall(async (data, context) => {
    var _a;
    const userId = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!userId)
        throw new functions.https.HttpsError("unauthenticated", "Sign in to request a notification");
    if ((data === null || data === void 0 ? void 0 : data.userId) !== undefined && data.userId !== userId)
        throw new functions.https.HttpsError("permission-denied", "Account changed");
    const type = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.type, "type", { required: true, maxLength: 50 });
    const details = (data === null || data === void 0 ? void 0 : data.data) && typeof data.data === "object" ? data.data : {};
    const recipientId = (0, validation_1.validateString)(data.recipientId, "recipientId", { required: true, maxLength: 128, pattern: /^[a-zA-Z0-9_-]+$/ });
    const applicationTypes = ["APPLICATION_STATUS", "APPLICATION_STATUS_UPDATE", "NEW_APPLICATION", "APPLICATION_REMINDER"];
    if (applicationTypes.includes(type)) {
        const applicationId = (0, validation_1.validateString)(details.applicationId, "applicationId", { required: true, maxLength: 256, pattern: /^[a-zA-Z0-9_-]+$/ });
        const created = await emitApplicationNotifications(applicationId, userId, recipientId, details.notificationType === "PENDING_APPLICATION_REMINDER" || type === "APPLICATION_REMINDER");
        return { success: true, created };
    }
    if (type === "WORKER_HIRED") {
        const jobId = (0, validation_1.validateString)(details.jobId, "jobId", { required: true, maxLength: 256, pattern: /^[a-zA-Z0-9_-]+$/ });
        let query = db.collection("job_applications")
            .where("employerId", "==", userId).where("jobId", "==", jobId).where("status", "in", ["ACCEPTED", "IN_PROGRESS", "COMPLETED"]);
        if (recipientId !== userId)
            query = query.where("workerId", "==", recipientId);
        const applications = await query.limit(100).get();
        if (applications.empty)
            throw new functions.https.HttpsError("permission-denied", "No matching hire belongs to this employer");
        let created = 0;
        for (const application of applications.docs)
            created += await emitApplicationNotifications(application.id, userId, recipientId);
        return { success: true, created };
    }
    if (recipientId !== userId)
        throw new functions.https.HttpsError("permission-denied", "This notification can only target your own account");
    if (!["JOB_POSTED", "JOB_PAUSED", "PROFILE_COMPLETE", "PROFILE_MILESTONE", "REFERRAL_MILESTONE", "BIRTHDAY"].includes(type)) {
        throw new functions.https.HttpsError("invalid-argument", "Unsupported notification event");
    }
    const created = await db.runTransaction(async (transaction) => {
        const user = await transaction.get(db.collection("users").doc(userId));
        if (!user.exists)
            throw new functions.https.HttpsError("not-found", "Profile not found");
        const targetRole = user.get("activeRole") === "EMPLOYER" ? "EMPLOYER" : "WORKER";
        let key = `${type}:${userId}`;
        let title = "Profile Complete";
        let message = "Your profile is complete.";
        const eventData = { deepLink: targetRole === "EMPLOYER" ? "dutype://employer/home" : "dutype://worker/home" };
        if (type === "JOB_POSTED" || type === "JOB_PAUSED") {
            const jobTitle = (0, validation_1.validateString)(details.jobTitle, "jobTitle", { required: true, maxLength: 500 });
            const jobs = await transaction.get(db.collection("jobs").where("employerId", "==", userId).where("title", "==", jobTitle).limit(1));
            if (jobs.empty)
                throw new functions.https.HttpsError("permission-denied", "Job does not belong to your account");
            const job = jobs.docs[0];
            const active = job.get("isActive") !== false;
            title = type === "JOB_POSTED" ? "Job Posted" : active ? "Job Activated" : "Job Paused";
            message = `${String(job.get("title")).slice(0, 150)}: ${title}.`;
            key = `${type}:${job.id}:${active}`;
            eventData.jobId = job.id;
            eventData.deepLink = `dutype://employer/jobs/${job.id}`;
        }
        else if (type === "REFERRAL_MILESTONE") {
            const stats = await transaction.get(db.collection("referral_stats").doc(userId));
            const count = stats.get("successfulReferrals");
            if (!Number.isSafeInteger(count) || count < 1)
                return 0;
            key += `:${count}`;
            title = "Referral Progress";
            message = `You have ${count} successful referrals.`;
        }
        else if (type === "BIRTHDAY") {
            const birthday = String(user.get("dateOfBirth") || "");
            const today = new Date();
            const date = /^\d{4}-(\d{2})-(\d{2})$/.exec(birthday);
            if (!date || Number(date[1]) !== today.getUTCMonth() + 1 || Number(date[2]) !== today.getUTCDate())
                return 0;
            key += `:${today.getUTCFullYear()}`;
            title = "Happy Birthday";
            message = "Wishing you a happy birthday from DutyPe.";
        }
        else if (user.get("profileCompleted") !== true && user.get("isProfileComplete") !== true) {
            return 0;
        }
        return persistEvents(transaction, [{ key, recipientId: userId, targetRole, type, title, message, data: eventData }]);
    });
    return { success: true, created };
});
//# sourceMappingURL=notification-events.js.map