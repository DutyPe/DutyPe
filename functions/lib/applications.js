"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.autoCompleteHired = exports.getWorkerContact = exports.setApplicationStatus = exports.withdrawApplication = exports.applyToJob = void 0;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const notify_1 = require("./lib/notify");
const auto_complete_rules_1 = require("./auto-complete-rules");
const referrals_1 = require("./referrals");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const S = schema_1.Values.ApplicationStatus;
const MAX_APPLICATIONS_PER_DAY = 40;
const DAY_MS = 24 * 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
function applicationId(jobId, workerId) {
    return `${jobId}_${workerId}`;
}
function validJobId(data) {
    return (0, input_1.str)(data, "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
}
async function appliedToday(uid) {
    const since = Math.floor((Date.now() + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
    const agg = await db.collection(schema_1.Applications.COLLECTION)
        .where(schema_1.Applications.WORKER_ID, "==", uid)
        .where(schema_1.Applications.CREATED_AT, ">=", Timestamp.fromMillis(since))
        .count().get();
    return agg.data().count;
}
// ───────────────────────────────── worker ─────────────────────────────────
exports.applyToJob = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const jobId = validJobId(data);
    const viaCall = data.viaCall === true;
    const appRef = db.collection(schema_1.Applications.COLLECTION).doc(applicationId(jobId, uid));
    const jobRef = db.collection(schema_1.Jobs.COLLECTION).doc(jobId);
    const workerRef = db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid);
    const [existingBefore, job] = await Promise.all([appRef.get(), jobRef.get()]);
    if (!existingBefore.exists && await appliedToday(uid) >= MAX_APPLICATIONS_PER_DAY) {
        (0, input_1.fail)("resource-exhausted", "You have applied to many jobs today. Try again tomorrow.");
    }
    const result = await db.runTransaction(async (tx) => {
        var _a, _b;
        const [app, worker] = await Promise.all([tx.get(appRef), tx.get(workerRef)]);
        const now = Timestamp.now();
        if (app.exists) {
            const status = app.get(schema_1.Applications.STATUS);
            const update = { [schema_1.Applications.UPDATED_AT]: now };
            if (viaCall) {
                update[schema_1.Applications.CALL_COUNT] = FieldValue.increment(1);
                update[schema_1.Applications.LAST_CALLED_AT] = now;
            }
            if (!viaCall && status === S.WITHDRAWN)
                update[schema_1.Applications.STATUS] = S.APPLIED;
            tx.update(appRef, update);
            return { created: false, employerId: String(app.get(schema_1.Applications.EMPLOYER_ID) || ""), status };
        }
        if (!job.exists)
            (0, input_1.fail)("not-found", "This job is no longer available");
        const employerId = String(job.get(schema_1.Jobs.EMPLOYER_ID) || "");
        if (employerId === uid)
            (0, input_1.fail)("failed-precondition", "You cannot apply to your own job");
        const expiresAt = (_b = (_a = job.get(schema_1.Jobs.EXPIRES_AT)) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        if (job.get(schema_1.Jobs.STATUS) !== schema_1.Values.JobStatus.OPEN || (expiresAt > 0 && expiresAt < now.toMillis())) {
            (0, input_1.fail)("failed-precondition", "This job is no longer accepting applications");
        }
        if (!worker.exists)
            (0, input_1.fail)("failed-precondition", "Complete your profile to apply");
        if (worker.get(schema_1.WorkerProfiles.BLOCKED) === true)
            (0, input_1.fail)("permission-denied", "Your account is blocked");
        const workerName = String(worker.get(schema_1.WorkerProfiles.NAME) || "").trim();
        if (!workerName)
            (0, input_1.fail)("failed-precondition", "Add your name to your profile to apply");
        const skills = worker.get(schema_1.WorkerProfiles.SKILLS);
        tx.create(appRef, Object.assign(Object.assign({ [schema_1.Applications.JOB_ID]: jobId, [schema_1.Applications.WORKER_ID]: uid, [schema_1.Applications.EMPLOYER_ID]: employerId, [schema_1.Applications.STATUS]: S.APPLIED, [schema_1.Applications.CREATED_AT]: now, [schema_1.Applications.UPDATED_AT]: now, [schema_1.Applications.CALL_COUNT]: viaCall ? 1 : 0 }, (viaCall ? { [schema_1.Applications.LAST_CALLED_AT]: now } : {})), { [schema_1.Applications.WORKER_NAME]: workerName, [schema_1.Applications.WORKER_PHOTO]: String(worker.get(schema_1.WorkerProfiles.PHOTO_URL) || ""), [schema_1.Applications.WORKER_SKILL]: Array.isArray(skills) && skills.length ? String(skills[0]) : "" }));
        return { created: true, employerId, status: S.APPLIED, workerName, jobTitle: String(job.get(schema_1.Jobs.TITLE) || "") };
    });
    // The number is only handed out with a recorded call, so every reveal is an application.
    const contact = viaCall ? await db.collection(schema_1.JobContacts.COLLECTION).doc(jobId).get() : null;
    const contactNumber = contact ? String(contact.get(schema_1.JobContacts.CONTACT_NUMBER) || "") : "";
    if (result.created) {
        await jobRef.update({ [schema_1.Jobs.APPLICATION_COUNT]: FieldValue.increment(1) })
            .catch((e) => functions.logger.warn(`applicationCount ${jobId}`, e));
        // A referred worker's reward is earned by their first real application.
        await (0, referrals_1.completeReferral)(uid, schema_1.Values.Role.WORKER).catch((e) => functions.logger.warn("referral", e));
        await (0, notify_1.notify)(result.employerId, {
            type: "NEW_APPLICATION",
            templateId: "NEW_APPLICATION_RECEIVED",
            role: schema_1.Values.Role.EMPLOYER,
            params: { workerName: result.workerName || "", jobTitle: result.jobTitle || "" },
            data: { jobId, applicationId: appRef.id },
        });
    }
    return Object.assign({ applicationId: appRef.id, created: result.created, status: result.status }, (viaCall ? { contactNumber } : {}));
});
exports.withdrawApplication = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const jobId = validJobId((0, input_1.obj)(raw));
    const ref = db.collection(schema_1.Applications.COLLECTION).doc(applicationId(jobId, uid));
    await db.runTransaction(async (tx) => {
        const app = await tx.get(ref);
        if (!app.exists)
            (0, input_1.fail)("not-found", "Application not found");
        if (app.get(schema_1.Applications.STATUS) !== S.APPLIED)
            (0, input_1.fail)("failed-precondition", "Only applications under review can be withdrawn");
        tx.update(ref, { [schema_1.Applications.STATUS]: S.WITHDRAWN, [schema_1.Applications.UPDATED_AT]: Timestamp.now() });
    });
    return { applicationId: ref.id };
});
// ──────────────────────────────── employer ────────────────────────────────
const TRANSITIONS = {
    [S.APPLIED]: [S.HIRED, S.REJECTED],
    [S.HIRED]: [S.COMPLETED, S.REJECTED],
};
exports.setApplicationStatus = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const appId = (0, input_1.str)(data, "applicationId", { max: 160, pattern: /^[A-Za-z0-9_-]+$/ });
    const next = (0, input_1.oneOf)(data, "status", [S.HIRED, S.REJECTED, S.COMPLETED].map((s) => s.toUpperCase())).toLowerCase();
    const ref = db.collection(schema_1.Applications.COLLECTION).doc(appId);
    const outcome = await db.runTransaction(async (tx) => {
        const app = await tx.get(ref);
        if (!app.exists)
            (0, input_1.fail)("not-found", "Application not found");
        if (app.get(schema_1.Applications.EMPLOYER_ID) !== uid)
            (0, input_1.fail)("permission-denied", "Not your applicant");
        const current = String(app.get(schema_1.Applications.STATUS));
        if (current === next)
            return null;
        if (!(TRANSITIONS[current] || []).includes(next))
            (0, input_1.fail)("failed-precondition", `Cannot change ${current} to ${next}`);
        const now = Timestamp.now();
        const workerId = String(app.get(schema_1.Applications.WORKER_ID));
        const jobId = String(app.get(schema_1.Applications.JOB_ID));
        if (next === S.HIRED) {
            // Vacancies cap: the hired query is part of the transaction, so two parallel hires for the
            // last seat cannot both succeed.
            const jobRef = db.collection(schema_1.Jobs.COLLECTION).doc(jobId);
            const [job, hired] = await Promise.all([
                tx.get(jobRef),
                tx.get(db.collection(schema_1.Applications.COLLECTION)
                    .where(schema_1.Applications.JOB_ID, "==", jobId)
                    .where(schema_1.Applications.STATUS, "in", [S.HIRED, S.COMPLETED])
                    .limit(51)),
            ]);
            if (job.exists) {
                const vacancies = Number(job.get(schema_1.Jobs.VACANCIES) || 1);
                if (hired.size >= vacancies) {
                    (0, input_1.fail)("failed-precondition", `All ${vacancies} position(s) for this job are already filled`);
                }
                if (hired.size + 1 >= vacancies && job.get(schema_1.Jobs.STATUS) === schema_1.Values.JobStatus.OPEN) {
                    tx.update(jobRef, { [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.FILLED });
                }
            }
        }
        tx.update(ref, Object.assign(Object.assign({ [schema_1.Applications.STATUS]: next, [schema_1.Applications.UPDATED_AT]: now }, (next === S.HIRED ? { [schema_1.Applications.HIRED_AT]: now } : {})), (next === S.COMPLETED ? { [schema_1.Applications.COMPLETED_AT]: now } : {})));
        if (next === S.HIRED) {
            tx.update(db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid), { [schema_1.EmployerProfiles.TOTAL_HIRES]: FieldValue.increment(1) });
        }
        if (next === S.COMPLETED) {
            tx.set(db.collection(schema_1.WorkerCards.COLLECTION).doc(workerId), { [schema_1.WorkerCards.JOBS_COMPLETED]: FieldValue.increment(1) }, { merge: true });
        }
        return { workerId, jobId };
    });
    if (outcome && next !== S.COMPLETED) {
        const job = await db.collection(schema_1.Jobs.COLLECTION).doc(outcome.jobId).get();
        await (0, notify_1.notify)(outcome.workerId, {
            type: next === S.HIRED ? "WORKER_HIRED" : "REJECTED",
            templateId: next === S.HIRED ? "APPLICATION_HIRED" : "APPLICATION_REJECTED",
            role: schema_1.Values.Role.WORKER,
            params: { jobTitle: String(job.get(schema_1.Jobs.TITLE) || ""), companyName: String(job.get(schema_1.Jobs.COMPANY_NAME) || "") },
            data: { jobId: outcome.jobId, applicationId: appId },
        });
    }
    return { applicationId: appId, status: next };
});
/**
 * A worker's phone for the employer of one of their jobs. Phones live only in the private
 * worker profile; every reveal is recorded in employer_profiles/{uid}/unlocks/{workerId}.
 */
exports.getWorkerContact = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const workerId = (0, input_1.str)(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
    const jobId = validJobId(data);
    const [job, app, worker] = await Promise.all([
        db.collection(schema_1.Jobs.COLLECTION).doc(jobId).get(),
        db.collection(schema_1.Applications.COLLECTION).doc(applicationId(jobId, workerId)).get(),
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(workerId).get(),
    ]);
    if (!job.exists || job.get(schema_1.Jobs.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your job");
    const isApplicant = app.exists && app.get(schema_1.Applications.EMPLOYER_ID) === uid;
    if (!isApplicant && job.get(schema_1.Jobs.STATUS) !== schema_1.Values.JobStatus.OPEN) {
        (0, input_1.fail)("failed-precondition", "This job is closed");
    }
    const phone = String(worker.get(schema_1.WorkerProfiles.PHONE) || "");
    if (!phone)
        (0, input_1.fail)("not-found", "This worker has no phone number");
    await db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid)
        .collection(schema_1.EmployerProfiles.Unlocks.COLLECTION).doc(workerId)
        .set({
        [schema_1.EmployerProfiles.Unlocks.JOB_ID]: jobId,
        [schema_1.EmployerProfiles.Unlocks.CREATED_AT]: Timestamp.now(),
    });
    return { phone };
});
// ─────────────────────────────── schedules ────────────────────────────────
exports.autoCompleteHired = functions
    .region("asia-south1")
    .pubsub.schedule("every 60 minutes")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const nowMs = Date.now();
    const snap = await db.collection(schema_1.Applications.COLLECTION)
        .where(schema_1.Applications.STATUS, "==", S.HIRED)
        .where(schema_1.Applications.HIRED_AT, "<=", Timestamp.fromMillis(nowMs - auto_complete_rules_1.AUTO_COMPLETE_RULES.STANDARD_GRACE_MS))
        .where(schema_1.Applications.HIRED_AT, ">=", Timestamp.fromMillis(nowMs - auto_complete_rules_1.AUTO_COMPLETE_RULES.MAX_LOOKBACK_MS))
        .limit(400)
        .get();
    if (snap.empty)
        return null;
    const batch = db.batch();
    const now = Timestamp.fromMillis(nowMs);
    snap.docs.forEach((d) => {
        batch.update(d.ref, { [schema_1.Applications.STATUS]: S.COMPLETED, [schema_1.Applications.COMPLETED_AT]: now, [schema_1.Applications.UPDATED_AT]: now });
        batch.set(db.collection(schema_1.WorkerCards.COLLECTION).doc(String(d.get(schema_1.Applications.WORKER_ID))), { [schema_1.WorkerCards.JOBS_COMPLETED]: FieldValue.increment(1) }, { merge: true });
    });
    await batch.commit();
    const jobs = await db.getAll(...Array.from(new Set(snap.docs.map((d) => String(d.get(schema_1.Applications.JOB_ID)))))
        .map((id) => db.collection(schema_1.Jobs.COLLECTION).doc(id)));
    const titles = new Map(jobs.map((j) => [j.id, String(j.get(schema_1.Jobs.TITLE) || "")]));
    await Promise.all(snap.docs.map((d) => (0, notify_1.notify)(String(d.get(schema_1.Applications.WORKER_ID)), {
        type: "APPLICATION_STATUS",
        templateId: "WORK_AUTO_COMPLETED",
        role: schema_1.Values.Role.WORKER,
        params: { title: titles.get(String(d.get(schema_1.Applications.JOB_ID))) || "" },
        data: { jobId: String(d.get(schema_1.Applications.JOB_ID)), applicationId: d.id },
    })));
    functions.logger.info(`autoCompleteHired: ${snap.size} completed`);
    return null;
});
//# sourceMappingURL=applications.js.map