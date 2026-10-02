/**
 * Applications — the only writers of applications/{jobId_workerId}.
 *
 *   applyToJob            worker applies, or taps "Call" (viaCall: counts the call; a first call
 *                         creates the application so the employer sees who called)
 *   withdrawApplication   worker withdraws an application still under review
 *   setApplicationStatus  employer hires / rejects / marks work completed
 *   autoCompleteHired     hired work completes itself after the grace period
 *
 * The document id makes a second application for the same job impossible; the transaction makes
 * a double tap or retry harmless. The job card is read before the transaction and its
 * applicationCount bumped after it, so a popular job's card is not a lock every applicant queues
 * on. Hires are capped at the job's vacancies; the last one marks the job filled.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, oneOf } from "./lib/input";
import { notify } from "./lib/notify";
import { AUTO_COMPLETE_RULES } from "./auto-complete-rules";
import { completeReferral } from "./referrals";
import { BUSY_MESSAGE, busyWith } from "./lib/busy";
import {
  Applications, EmployerProfiles, JobContacts, Jobs, Values, WorkerCards, WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const S = Values.ApplicationStatus;

const MAX_APPLICATIONS_PER_DAY = 40;
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;

function applicationId(jobId: string, workerId: string): string {
  return `${jobId}_${workerId}`;
}

function validJobId(data: Record<string, unknown>): string {
  return str(data, "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
}

async function appliedToday(uid: string): Promise<number> {
  const since = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
  const agg = await db.collection(Applications.COLLECTION)
    .where(Applications.WORKER_ID, "==", uid)
    .where(Applications.CREATED_AT, ">=", Timestamp.fromMillis(since))
    .count().get();
  return agg.data().count;
}

// ───────────────────────────────── worker ─────────────────────────────────

export const applyToJob = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const jobId = validJobId(data);
  const viaCall = data.viaCall === true;
  const appRef = db.collection(Applications.COLLECTION).doc(applicationId(jobId, uid));
  const jobRef = db.collection(Jobs.COLLECTION).doc(jobId);
  const workerRef = db.collection(WorkerProfiles.COLLECTION).doc(uid);

  const [existingBefore, job] = await Promise.all([appRef.get(), jobRef.get()]);
  if (!existingBefore.exists && await appliedToday(uid) >= MAX_APPLICATIONS_PER_DAY) {
    fail("resource-exhausted", "You have applied to many jobs today. Try again tomorrow.");
  }

  const result = await db.runTransaction(async (tx) => {
    const [app, worker] = await Promise.all([tx.get(appRef), tx.get(workerRef)]);
    const now = Timestamp.now();

    if (app.exists) {
      const status = app.get(Applications.STATUS);
      const update: Record<string, unknown> = { [Applications.UPDATED_AT]: now };
      if (viaCall) {
        update[Applications.CALL_COUNT] = FieldValue.increment(1);
        update[Applications.LAST_CALLED_AT] = now;
      }
      if (!viaCall && status === S.WITHDRAWN) update[Applications.STATUS] = S.APPLIED;
      tx.update(appRef, update as admin.firestore.DocumentData);
      return { created: false, employerId: String(app.get(Applications.EMPLOYER_ID) || ""), status };
    }

    if (!job.exists) fail("not-found", "This job is no longer available");
    const employerId = String(job.get(Jobs.EMPLOYER_ID) || "");
    if (employerId === uid) fail("failed-precondition", "You cannot apply to your own job");
    const expiresAt = (job.get(Jobs.EXPIRES_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    if (job.get(Jobs.STATUS) !== Values.JobStatus.OPEN || (expiresAt > 0 && expiresAt < now.toMillis())) {
      fail("failed-precondition", "This job is no longer accepting applications");
    }
    if (!worker.exists) fail("failed-precondition", "Complete your profile to apply");
    if (worker.get(WorkerProfiles.BLOCKED) === true) fail("permission-denied", "Your account is blocked");
    // One job at a time: finish the current urgent / service / hired job before applying again.
    const busy = await busyWith(uid, worker, tx);
    if (busy) fail("failed-precondition", BUSY_MESSAGE[busy]);
    const workerName = String(worker.get(WorkerProfiles.NAME) || "").trim();
    if (!workerName) fail("failed-precondition", "Add your name to your profile to apply");
    const skills = worker.get(WorkerProfiles.SKILLS);

    tx.create(appRef, {
      [Applications.JOB_ID]: jobId,
      [Applications.WORKER_ID]: uid,
      [Applications.EMPLOYER_ID]: employerId,
      [Applications.STATUS]: S.APPLIED,
      [Applications.CREATED_AT]: now,
      [Applications.UPDATED_AT]: now,
      [Applications.CALL_COUNT]: viaCall ? 1 : 0,
      ...(viaCall ? { [Applications.LAST_CALLED_AT]: now } : {}),
      [Applications.WORKER_NAME]: workerName,
      [Applications.WORKER_PHOTO]: String(worker.get(WorkerProfiles.PHOTO_URL) || ""),
      [Applications.WORKER_SKILL]: Array.isArray(skills) && skills.length ? String(skills[0]) : "",
    });
    return { created: true, employerId, status: S.APPLIED, workerName, jobTitle: String(job.get(Jobs.TITLE) || "") };
  });

  // The number is only handed out with a recorded call, so every reveal is an application.
  const contact = viaCall ? await db.collection(JobContacts.COLLECTION).doc(jobId).get() : null;
  const contactNumber = contact ? String(contact.get(JobContacts.CONTACT_NUMBER) || "") : "";

  if (result.created) {
    await jobRef.update({ [Jobs.APPLICATION_COUNT]: FieldValue.increment(1) })
      .catch((e) => functions.logger.warn(`applicationCount ${jobId}`, e));
    // A referred worker's reward is earned by their first real application.
    await completeReferral(uid, Values.Role.WORKER).catch((e) => functions.logger.warn("referral", e));
    await notify(result.employerId, {
      type: "NEW_APPLICATION",
      templateId: "NEW_APPLICATION_RECEIVED",
      role: Values.Role.EMPLOYER,
      params: { workerName: result.workerName || "", jobTitle: result.jobTitle || "" },
      data: { jobId, applicationId: appRef.id },
    });
  }
  return {
    applicationId: appRef.id, created: result.created, status: result.status,
    ...(viaCall ? { contactNumber } : {}),
  };
});

export const withdrawApplication = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const jobId = validJobId(obj(raw));
  const ref = db.collection(Applications.COLLECTION).doc(applicationId(jobId, uid));
  await db.runTransaction(async (tx) => {
    const app = await tx.get(ref);
    if (!app.exists) fail("not-found", "Application not found");
    if (app.get(Applications.STATUS) !== S.APPLIED) fail("failed-precondition", "Only applications under review can be withdrawn");
    tx.update(ref, { [Applications.STATUS]: S.WITHDRAWN, [Applications.UPDATED_AT]: Timestamp.now() });
  });
  return { applicationId: ref.id };
});

// ──────────────────────────────── employer ────────────────────────────────

const TRANSITIONS: Record<string, string[]> = {
  [S.APPLIED]: [S.HIRED, S.REJECTED],
  [S.HIRED]: [S.COMPLETED, S.REJECTED],
};

export const setApplicationStatus = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const appId = str(data, "applicationId", { max: 160, pattern: /^[A-Za-z0-9_-]+$/ });
  const next = oneOf(data, "status", [S.HIRED, S.REJECTED, S.COMPLETED].map((s) => s.toUpperCase())).toLowerCase();
  const ref = db.collection(Applications.COLLECTION).doc(appId);

  const outcome = await db.runTransaction(async (tx) => {
    const app = await tx.get(ref);
    if (!app.exists) fail("not-found", "Application not found");
    if (app.get(Applications.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your applicant");
    const current = String(app.get(Applications.STATUS));
    if (current === next) return null;
    if (!(TRANSITIONS[current] || []).includes(next)) fail("failed-precondition", `Cannot change ${current} to ${next}`);
    const now = Timestamp.now();
    const workerId = String(app.get(Applications.WORKER_ID));
    const jobId = String(app.get(Applications.JOB_ID));

    if (next === S.HIRED) {
      // Vacancies cap: the hired query is part of the transaction, so two parallel hires for the
      // last seat cannot both succeed.
      const jobRef = db.collection(Jobs.COLLECTION).doc(jobId);
      const [job, hired] = await Promise.all([
        tx.get(jobRef),
        tx.get(db.collection(Applications.COLLECTION)
          .where(Applications.JOB_ID, "==", jobId)
          .where(Applications.STATUS, "in", [S.HIRED, S.COMPLETED])
          .limit(51)),
      ]);
      if (job.exists) {
        const vacancies = Number(job.get(Jobs.VACANCIES) || 1);
        if (hired.size >= vacancies) {
          fail("failed-precondition", `All ${vacancies} position(s) for this job are already filled`);
        }
        if (hired.size + 1 >= vacancies && job.get(Jobs.STATUS) === Values.JobStatus.OPEN) {
          tx.update(jobRef, { [Jobs.STATUS]: Values.JobStatus.FILLED });
        }
      }
    }
    tx.update(ref, {
      [Applications.STATUS]: next,
      [Applications.UPDATED_AT]: now,
      ...(next === S.HIRED ? { [Applications.HIRED_AT]: now } : {}),
      ...(next === S.COMPLETED ? { [Applications.COMPLETED_AT]: now } : {}),
    });
    if (next === S.HIRED) {
      tx.update(db.collection(EmployerProfiles.COLLECTION).doc(uid), { [EmployerProfiles.TOTAL_HIRES]: FieldValue.increment(1) });
    }
    if (next === S.COMPLETED) {
      tx.set(db.collection(WorkerCards.COLLECTION).doc(workerId),
        { [WorkerCards.JOBS_COMPLETED]: FieldValue.increment(1) }, { merge: true });
    }
    return { workerId, jobId };
  });

  if (outcome && next !== S.COMPLETED) {
    const job = await db.collection(Jobs.COLLECTION).doc(outcome.jobId).get();
    await notify(outcome.workerId, {
      type: next === S.HIRED ? "WORKER_HIRED" : "REJECTED",
      templateId: next === S.HIRED ? "APPLICATION_HIRED" : "APPLICATION_REJECTED",
      role: Values.Role.WORKER,
      params: { jobTitle: String(job.get(Jobs.TITLE) || ""), companyName: String(job.get(Jobs.COMPANY_NAME) || "") },
      data: { jobId: outcome.jobId, applicationId: appId },
    });
  }
  return { applicationId: appId, status: next };
});

/**
 * A worker's phone for the employer of one of their jobs. Phones live only in the private
 * worker profile; every reveal is recorded in employer_profiles/{uid}/unlocks/{workerId}.
 */
export const getWorkerContact = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const workerId = str(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
  const jobId = validJobId(data);
  const [job, app, worker] = await Promise.all([
    db.collection(Jobs.COLLECTION).doc(jobId).get(),
    db.collection(Applications.COLLECTION).doc(applicationId(jobId, workerId)).get(),
    db.collection(WorkerProfiles.COLLECTION).doc(workerId).get(),
  ]);
  if (!job.exists || job.get(Jobs.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your job");
  const isApplicant = app.exists && app.get(Applications.EMPLOYER_ID) === uid;
  if (!isApplicant && job.get(Jobs.STATUS) !== Values.JobStatus.OPEN) {
    fail("failed-precondition", "This job is closed");
  }
  const phone = String(worker.get(WorkerProfiles.PHONE) || "");
  if (!phone) fail("not-found", "This worker has no phone number");
  await db.collection(EmployerProfiles.COLLECTION).doc(uid)
    .collection(EmployerProfiles.Unlocks.COLLECTION).doc(workerId)
    .set({
      [EmployerProfiles.Unlocks.JOB_ID]: jobId,
      [EmployerProfiles.Unlocks.CREATED_AT]: Timestamp.now(),
    });
  return { phone };
});

// ─────────────────────────────── schedules ────────────────────────────────

export const autoCompleteHired = functions
  .region("asia-south1")
  .pubsub.schedule("every 60 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const nowMs = Date.now();
    const snap = await db.collection(Applications.COLLECTION)
      .where(Applications.STATUS, "==", S.HIRED)
      .where(Applications.HIRED_AT, "<=", Timestamp.fromMillis(nowMs - AUTO_COMPLETE_RULES.STANDARD_GRACE_MS))
      .where(Applications.HIRED_AT, ">=", Timestamp.fromMillis(nowMs - AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS))
      .limit(400)
      .get();
    if (snap.empty) return null;
    const batch = db.batch();
    const now = Timestamp.fromMillis(nowMs);
    snap.docs.forEach((d) => {
      batch.update(d.ref, { [Applications.STATUS]: S.COMPLETED, [Applications.COMPLETED_AT]: now, [Applications.UPDATED_AT]: now });
      batch.set(db.collection(WorkerCards.COLLECTION).doc(String(d.get(Applications.WORKER_ID))),
        { [WorkerCards.JOBS_COMPLETED]: FieldValue.increment(1) }, { merge: true });
    });
    await batch.commit();
    const jobs = await db.getAll(...Array.from(new Set(snap.docs.map((d) => String(d.get(Applications.JOB_ID)))))
      .map((id) => db.collection(Jobs.COLLECTION).doc(id)));
    const titles = new Map(jobs.map((j) => [j.id, String(j.get(Jobs.TITLE) || "")]));
    await Promise.all(snap.docs.map((d) => notify(String(d.get(Applications.WORKER_ID)), {
      type: "APPLICATION_STATUS",
      templateId: "WORK_AUTO_COMPLETED",
      role: Values.Role.WORKER,
      params: { title: titles.get(String(d.get(Applications.JOB_ID))) || "" },
      data: { jobId: String(d.get(Applications.JOB_ID)), applicationId: d.id },
    })));
    functions.logger.info(`autoCompleteHired: ${snap.size} completed`);
    return null;
  });
