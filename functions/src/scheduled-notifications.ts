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
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { notify } from "./lib/notify";
import {
  NOTIFICATION_TEMPLATES, SE_EMPLOYER_POOL, SE_GUEST_POOL, SE_WORKER_POOL, SUPPORTED_LOCALES,
  localizedTopic, tBody, tTitle,
} from "./notification-i18n";
import { Applications, Jobs, Values, WorkerCards } from "./schema";

const db = admin.firestore();
const { Timestamp } = admin.firestore;
const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;
const IST_OFFSET_MS = 330 * 60 * 1000;

function schedule(cron: string) {
  return functions.region("asia-south1").pubsub.schedule(cron).timeZone("Asia/Kolkata");
}

/** 22:00–08:00 IST. */
function isQuietHours(nowMs = Date.now()): boolean {
  const hour = new Date(nowMs + IST_OFFSET_MS).getUTCHours();
  return hour >= 22 || hour < 8;
}

// ───────────────────────────── topic nudges ─────────────────────────────

type Slot = "morning" | "afternoon" | "evening";

function pick<T extends { timeOfDay?: Slot }>(pool: T[], slot: Slot): T {
  const fitting = pool.filter((p) => !p.timeOfDay || p.timeOfDay === slot);
  const day = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS);
  return fitting[day % fitting.length];
}

async function sendToTopics(
  baseTopic: string,
  templateId: keyof typeof NOTIFICATION_TEMPLATES,
  deepLink: string,
): Promise<void> {
  await Promise.all(SUPPORTED_LOCALES.map((locale) => admin.messaging().send({
    topic: localizedTopic(baseTopic, locale),
    data: {
      type: "SMART_ENGAGEMENT",
      title: tTitle(templateId, locale, { recipient: "" }).replace(/\s+,/, ",").replace(/^(\S+) ,/, "$1,"),
      body: tBody(templateId, locale, {}),
      deepLink,
      channel: "medium_priority",
    },
    android: { priority: "normal" },
  }).catch((e) => functions.logger.warn(`topic ${baseTopic}_${locale} failed`, e))));
}

async function engagement(slot: Slot): Promise<null> {
  if (isQuietHours()) return null;
  const worker = pick(SE_WORKER_POOL, slot);
  const employer = pick(SE_EMPLOYER_POOL, slot);
  const guest = pick(SE_GUEST_POOL, slot);
  await Promise.all([
    sendToTopics("workers", worker.id, worker.deepLink),
    sendToTopics("employers", employer.id, employer.deepLink),
    sendToTopics("guest_users", guest.id, "dutype://login"),
  ]);
  return null;
}

export const engagementMorning = schedule("0 9 * * *").onRun(() => engagement("morning"));
export const engagementAfternoon = schedule("0 14 * * *").onRun(() => engagement("afternoon"));
export const engagementEvening = schedule("0 19 * * *").onRun(() => engagement("evening"));

// ───────────────────────────── personal reminders ─────────────────────────────

/** Jobs that expire 23–24 h from now (hourly run ⇒ each job once). */
export const remindExpiringJobs = schedule("0 * * * *").onRun(async () => {
  const now = Date.now();
  const snap = await db.collection(Jobs.COLLECTION)
    .where(Jobs.STATUS, "==", Values.JobStatus.OPEN)
    .where(Jobs.EXPIRES_AT, ">=", Timestamp.fromMillis(now + 23 * HOUR_MS))
    .where(Jobs.EXPIRES_AT, "<", Timestamp.fromMillis(now + 24 * HOUR_MS))
    .limit(1000)
    .get();
  for (let i = 0; i < snap.docs.length; i += 25) {
    await Promise.all(snap.docs.slice(i, i + 25).map((job) => notify(String(job.get(Jobs.EMPLOYER_ID)), {
      type: "JOB_EXPIRY_REMINDER",
      templateId: "JOB_EXPIRY_SOON",
      params: { jobTitle: String(job.get(Jobs.TITLE) || ""), hoursLeft: 24 },
      data: { jobId: job.id },
      role: Values.Role.EMPLOYER,
    })));
  }
  return null;
});

/**
 * Applications that became 2 days old without a decision in the last 2 hours: one reminder per
 * employer (with the count) and one per worker.
 */
export const remindPendingApplications = schedule("0 */2 * * *").onRun(async () => {
  if (isQuietHours()) return null;
  const now = Date.now();
  const snap = await db.collection(Applications.COLLECTION)
    .where(Applications.STATUS, "==", Values.ApplicationStatus.APPLIED)
    .where(Applications.CREATED_AT, ">=", Timestamp.fromMillis(now - 50 * HOUR_MS))
    .where(Applications.CREATED_AT, "<", Timestamp.fromMillis(now - 48 * HOUR_MS))
    .limit(2000)
    .get();
  if (snap.empty) return null;

  const perEmployer = new Map<string, number>();
  snap.docs.forEach((d) => {
    const employerId = String(d.get(Applications.EMPLOYER_ID));
    perEmployer.set(employerId, (perEmployer.get(employerId) || 0) + 1);
  });
  const jobIds = Array.from(new Set(snap.docs.map((d) => String(d.get(Applications.JOB_ID)))));
  const titles = new Map<string, string>();
  for (let i = 0; i < jobIds.length; i += 100) {
    const refs = jobIds.slice(i, i + 100).map((id) => db.collection(Jobs.COLLECTION).doc(id));
    (await db.getAll(...refs)).forEach((j) => titles.set(j.id, String(j.get(Jobs.TITLE) || "")));
  }

  const tasks: Array<() => Promise<void>> = [];
  perEmployer.forEach((count, employerId) => tasks.push(() => notify(employerId, {
    type: "NEW_APPLICATION",
    templateId: "EMPLOYER_PENDING_APPLICATIONS",
    params: { count },
    data: { action: "view_applications" },
    role: Values.Role.EMPLOYER,
  })));
  snap.docs.forEach((d) => tasks.push(() => notify(String(d.get(Applications.WORKER_ID)), {
    type: "APPLICATION_REMINDER",
    templateId: "WORKER_PENDING_APPLICATION",
    params: { jobTitle: titles.get(String(d.get(Applications.JOB_ID))) || "", daysPending: 2 },
    data: { jobId: String(d.get(Applications.JOB_ID)) },
    role: Values.Role.WORKER,
  })));
  for (let i = 0; i < tasks.length; i += 25) await Promise.all(tasks.slice(i, i + 25).map((t) => t()));
  return null;
});

/** Workers whose card went quiet exactly 7 days ago (6-hour window ⇒ once per inactive spell). */
export const reEngageInactiveWorkers = schedule("0 */6 * * *").onRun(async () => {
  if (isQuietHours()) return null;
  const now = Date.now();
  const snap = await db.collection(WorkerCards.COLLECTION)
    .where(WorkerCards.LAST_ACTIVE_AT, ">=", Timestamp.fromMillis(now - 7 * DAY_MS - 6 * HOUR_MS))
    .where(WorkerCards.LAST_ACTIVE_AT, "<", Timestamp.fromMillis(now - 7 * DAY_MS))
    .limit(2000)
    .get();
  for (let i = 0; i < snap.docs.length; i += 25) {
    await Promise.all(snap.docs.slice(i, i + 25).map((card) => notify(card.id, {
      type: "NEW_JOB_ALERT",
      templateId: "WORKER_RE_ENGAGEMENT",
      data: { action: "view_jobs" },
      role: Values.Role.WORKER,
    })));
  }
  return null;
});
