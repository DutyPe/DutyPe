"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.reEngageInactiveWorkers = exports.remindPendingApplications = exports.remindExpiringJobs = exports.engagementEvening = exports.engagementAfternoon = exports.engagementMorning = void 0;
/**
 * Scheduled reminders, built to cost the same at 1k or 1M users:
 *
 *   • Engagement nudges go to FCM topics (workers_{lang}, employers_{lang}, guest_users_{lang}):
 *     one send per language, zero document reads.
 *   • Personal reminders read only the documents that crossed a threshold since the previous run
 *     (a time window as wide as the schedule interval), so each job/application is picked exactly
 *     once — no per-user dedupe documents, no collection scans.
 *   • Inbox cleanup is a Firestore TTL policy on notifications.expireAt (no function).
 *   • Birthday wishes are shown on the device (BirthdayService), not scanned for here.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const notify_1 = require("./lib/notify");
const notification_i18n_1 = require("./notification-i18n");
const schema_1 = require("./schema");
const db = admin.firestore();
const { Timestamp } = admin.firestore;
const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;
const IST_OFFSET_MS = 330 * 60 * 1000;
function schedule(cron) {
    return functions.region("asia-south1").pubsub.schedule(cron).timeZone("Asia/Kolkata");
}
/** 22:00–08:00 IST. */
function isQuietHours(nowMs = Date.now()) {
    const hour = new Date(nowMs + IST_OFFSET_MS).getUTCHours();
    return hour >= 22 || hour < 8;
}
function pick(pool, slot) {
    const fitting = pool.filter((p) => !p.timeOfDay || p.timeOfDay === slot);
    const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
    return fitting[day % fitting.length];
}
async function sendToTopics(baseTopic, templateId, deepLink) {
    await Promise.all(notification_i18n_1.SUPPORTED_LOCALES.map((locale) => admin.messaging().send({
        topic: (0, notification_i18n_1.localizedTopic)(baseTopic, locale),
        data: {
            type: "SMART_ENGAGEMENT",
            title: (0, notification_i18n_1.tTitle)(templateId, locale, { recipient: "" }).replace(/\s+,/, ",").replace(/^(\S+) ,/, "$1,"),
            body: (0, notification_i18n_1.tBody)(templateId, locale, {}),
            deepLink,
            channel: "medium_priority",
        },
        android: { priority: "normal" },
    }).catch((e) => functions.logger.warn(`topic ${baseTopic}_${locale} failed`, e))));
}
async function engagement(slot) {
    if (isQuietHours())
        return null;
    const worker = pick(notification_i18n_1.SE_WORKER_POOL, slot);
    const employer = pick(notification_i18n_1.SE_EMPLOYER_POOL, slot);
    const guest = pick(notification_i18n_1.SE_GUEST_POOL, slot);
    await Promise.all([
        sendToTopics("workers", worker.id, worker.deepLink),
        sendToTopics("employers", employer.id, employer.deepLink),
        sendToTopics("guest_users", guest.id, "dutype://login"),
    ]);
    return null;
}
exports.engagementMorning = schedule("0 9 * * *").onRun(() => engagement("morning"));
exports.engagementAfternoon = schedule("0 14 * * *").onRun(() => engagement("afternoon"));
exports.engagementEvening = schedule("0 19 * * *").onRun(() => engagement("evening"));
// ───────────────────────────── personal reminders ─────────────────────────────
/** Jobs that expire 23–24 h from now (hourly run ⇒ each job once). */
exports.remindExpiringJobs = schedule("0 * * * *").onRun(async () => {
    const now = Date.now();
    const snap = await db.collection(schema_1.Jobs.COLLECTION)
        .where(schema_1.Jobs.STATUS, "==", schema_1.Values.JobStatus.OPEN)
        .where(schema_1.Jobs.EXPIRES_AT, ">=", Timestamp.fromMillis(now + 23 * HOUR_MS))
        .where(schema_1.Jobs.EXPIRES_AT, "<", Timestamp.fromMillis(now + 24 * HOUR_MS))
        .limit(1000)
        .get();
    for (let i = 0; i < snap.docs.length; i += 25) {
        await Promise.all(snap.docs.slice(i, i + 25).map((job) => (0, notify_1.notify)(String(job.get(schema_1.Jobs.EMPLOYER_ID)), {
            type: "JOB_EXPIRY_REMINDER",
            templateId: "JOB_EXPIRY_SOON",
            params: { jobTitle: String(job.get(schema_1.Jobs.TITLE) || ""), hoursLeft: 24 },
            data: { jobId: job.id },
            role: schema_1.Values.Role.EMPLOYER,
        })));
    }
    return null;
});
/**
 * Applications that became 2 days old without a decision in the last 2 hours: one reminder per
 * employer (with the count) and one per worker.
 */
exports.remindPendingApplications = schedule("0 */2 * * *").onRun(async () => {
    if (isQuietHours())
        return null;
    const now = Date.now();
    const snap = await db.collection(schema_1.Applications.COLLECTION)
        .where(schema_1.Applications.STATUS, "==", schema_1.Values.ApplicationStatus.APPLIED)
        .where(schema_1.Applications.CREATED_AT, ">=", Timestamp.fromMillis(now - 50 * HOUR_MS))
        .where(schema_1.Applications.CREATED_AT, "<", Timestamp.fromMillis(now - 48 * HOUR_MS))
        .limit(2000)
        .get();
    if (snap.empty)
        return null;
    const perEmployer = new Map();
    snap.docs.forEach((d) => {
        const employerId = String(d.get(schema_1.Applications.EMPLOYER_ID));
        perEmployer.set(employerId, (perEmployer.get(employerId) || 0) + 1);
    });
    const jobIds = Array.from(new Set(snap.docs.map((d) => String(d.get(schema_1.Applications.JOB_ID)))));
    const titles = new Map();
    for (let i = 0; i < jobIds.length; i += 100) {
        const refs = jobIds.slice(i, i + 100).map((id) => db.collection(schema_1.Jobs.COLLECTION).doc(id));
        (await db.getAll(...refs)).forEach((j) => titles.set(j.id, String(j.get(schema_1.Jobs.TITLE) || "")));
    }
    const tasks = [];
    perEmployer.forEach((count, employerId) => tasks.push(() => (0, notify_1.notify)(employerId, {
        type: "NEW_APPLICATION",
        templateId: "EMPLOYER_PENDING_APPLICATIONS",
        params: { count },
        data: { action: "view_applications" },
        role: schema_1.Values.Role.EMPLOYER,
    })));
    snap.docs.forEach((d) => tasks.push(() => (0, notify_1.notify)(String(d.get(schema_1.Applications.WORKER_ID)), {
        type: "APPLICATION_REMINDER",
        templateId: "WORKER_PENDING_APPLICATION",
        params: { jobTitle: titles.get(String(d.get(schema_1.Applications.JOB_ID))) || "", daysPending: 2 },
        data: { jobId: String(d.get(schema_1.Applications.JOB_ID)) },
        role: schema_1.Values.Role.WORKER,
    })));
    for (let i = 0; i < tasks.length; i += 25)
        await Promise.all(tasks.slice(i, i + 25).map((t) => t()));
    return null;
});
/** Workers whose card went quiet exactly 7 days ago (6-hour window ⇒ once per inactive spell). */
exports.reEngageInactiveWorkers = schedule("0 */6 * * *").onRun(async () => {
    if (isQuietHours())
        return null;
    const now = Date.now();
    const snap = await db.collection(schema_1.WorkerCards.COLLECTION)
        .where(schema_1.WorkerCards.LAST_ACTIVE_AT, ">=", Timestamp.fromMillis(now - 7 * DAY_MS - 6 * HOUR_MS))
        .where(schema_1.WorkerCards.LAST_ACTIVE_AT, "<", Timestamp.fromMillis(now - 7 * DAY_MS))
        .limit(2000)
        .get();
    for (let i = 0; i < snap.docs.length; i += 25) {
        await Promise.all(snap.docs.slice(i, i + 25).map((card) => (0, notify_1.notify)(card.id, {
            type: "NEW_JOB_ALERT",
            templateId: "WORKER_RE_ENGAGEMENT",
            data: { action: "view_jobs" },
            role: schema_1.Values.Role.WORKER,
        })));
    }
    return null;
});
//# sourceMappingURL=scheduled-notifications.js.map