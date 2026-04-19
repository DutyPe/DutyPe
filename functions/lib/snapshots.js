"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onJobMetadataUpdate = exports.onEmployerProfileWrite = exports.onUserIdentityUpdate = exports.onWorkerProfileWrite = void 0;
/**
 * Snapshot-refresh Cloud Functions.
 *
 * Maintains denormalized copies of user / job data on related documents so
 * that list screens render with zero N+1 fan-out:
 *
 *   applications.workerSnapshot   ← worker_profiles/{uid} + users/{uid}
 *   applications.jobSnapshot      ← jobmetadata/{jobId}
 *   jobmetadata.employerSnapshot  ← employer_profiles/{uid} + users/{uid}
 *
 * Only "active" related docs are refreshed (applications in applied/shortlisted
 * status, jobmetadata in open status) so the fan-out is bounded.
 *
 * Also recomputes a server-owned `profileScore` on worker_profiles/{uid} and
 * mirrors it to users/{uid}.workerProfileScore. This is the apply-gate
 * signal for submitApplication and closes the PERMISSION_DENIED → 80% hole.
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const db = () => admin.firestore();
const REGION = "asia-south1";
const BATCH_SIZE = 400;
const ACTIVE_APP_STATUSES = ["applied", "shortlisted"];
function str(v, fallback = "") {
    if (typeof v === "string")
        return v;
    if (v == null)
        return fallback;
    return String(v);
}
function num(v, fallback = 0) {
    const n = Number(v);
    return Number.isFinite(n) ? n : fallback;
}
// ────────────────────────────────────────────────────────────────────────
// Batched updater
// ────────────────────────────────────────────────────────────────────────
async function batchUpdate(query, mutate) {
    let updated = 0;
    let last;
    // eslint-disable-next-line no-constant-condition
    while (true) {
        let q = query.limit(BATCH_SIZE);
        if (last)
            q = q.startAfter(last);
        const snap = await q.get();
        if (snap.empty)
            break;
        const batch = db().batch();
        let writes = 0;
        for (const doc of snap.docs) {
            const patch = mutate(doc);
            if (patch && Object.keys(patch).length > 0) {
                batch.update(doc.ref, patch);
                writes++;
            }
        }
        if (writes > 0)
            await batch.commit();
        updated += writes;
        if (snap.size < BATCH_SIZE)
            break;
        last = snap.docs[snap.docs.length - 1];
    }
    return updated;
}
// ────────────────────────────────────────────────────────────────────────
// Worker profile → applications.workerSnapshot + users.workerProfileScore
// ────────────────────────────────────────────────────────────────────────
function buildWorkerSnapshot(uid, worker, user) {
    var _a, _b;
    const jobTypes = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
    return {
        workerId: uid,
        fullName: str(user.fullName),
        profileImageUrl: str(user.profileImageUrl),
        primarySkill: jobTypes.length > 0 ? str(jobTypes[0]) : "",
        experience: str(worker.experience),
        city: str((_a = user.location) === null || _a === void 0 ? void 0 : _a.city) || str(user.city),
        ratingAvg: num((_b = worker.rating) !== null && _b !== void 0 ? _b : worker.ratingAvg),
        totalJobs: num(worker.totalJobs),
    };
}
/**
 * Profile score is a coarse 0–100 gauge. Keep the formula server-side so that
 * clients cannot spoof themselves past the apply gate.
 *   • identity     : 30  (fullName + phone)
 *   • role-basics  : 30  (jobTypes present + isAvailable true)
 *   • experience   : 30  (experience string non-empty)
 *   • location     : 10  (user has location OR geohash)
 */
function computeWorkerProfileScore(worker, user) {
    let score = 0;
    if (str(user.fullName).trim().length >= 2)
        score += 20;
    if (str(user.phone).trim().length >= 10)
        score += 10;
    const jobTypes = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
    if (jobTypes.length > 0)
        score += 20;
    if (worker.isAvailable === true)
        score += 10;
    if (str(worker.experience).trim().length >= 3)
        score += 30;
    if (user.location || str(user.geohash).length > 0)
        score += 10;
    return Math.min(100, Math.max(0, score));
}
exports.onWorkerProfileWrite = functions
    .region(REGION)
    .firestore.document("worker_profiles/{uid}")
    .onWrite(async (change, context) => {
    const uid = context.params.uid;
    if (!change.after.exists) {
        functions.logger.info(`worker_profiles/${uid} deleted; skipping snapshot refresh`);
        return null;
    }
    const worker = change.after.data() || {};
    const userDoc = await db().collection("users").doc(uid).get();
    const user = userDoc.data() || {};
    const score = computeWorkerProfileScore(worker, user);
    const snapshot = buildWorkerSnapshot(uid, worker, user);
    // 1. Persist score back to worker_profiles (via Admin SDK, bypasses rules)
    //    and mirror to users/{uid}.workerProfileScore.
    const writes = [];
    if (num(worker.profileScore, -1) !== score) {
        writes.push(change.after.ref.set({ profileScore: score, profileScoreUpdatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true }));
    }
    if (num(user.workerProfileScore, -1) !== score) {
        writes.push(db().collection("users").doc(uid).set({ workerProfileScore: score, lastActiveAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true }));
    }
    await Promise.all(writes);
    // 2. Refresh workerSnapshot on active applications.
    const q = db()
        .collection("applications")
        .where("workerId", "==", uid)
        .where("status", "in", ACTIVE_APP_STATUSES);
    const updated = await batchUpdate(q, () => ({
        workerSnapshot: snapshot,
        workerSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }));
    functions.logger.info(`onWorkerProfileWrite: uid=${uid} score=${score} apps_updated=${updated}`);
    return null;
});
/**
 * When users/{uid} updates identity fields used in the snapshot (fullName,
 * profileImageUrl), re-push them to active applications.
 */
exports.onUserIdentityUpdate = functions
    .region(REGION)
    .firestore.document("users/{uid}")
    .onUpdate(async (change, context) => {
    const uid = context.params.uid;
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const fullNameChanged = str(before.fullName) !== str(after.fullName);
    const imageChanged = str(before.profileImageUrl) !== str(after.profileImageUrl);
    if (!fullNameChanged && !imageChanged)
        return null;
    const workerDoc = await db().collection("worker_profiles").doc(uid).get();
    if (!workerDoc.exists)
        return null;
    const snapshot = buildWorkerSnapshot(uid, workerDoc.data() || {}, after);
    const q = db()
        .collection("applications")
        .where("workerId", "==", uid)
        .where("status", "in", ACTIVE_APP_STATUSES);
    const updated = await batchUpdate(q, () => ({
        workerSnapshot: snapshot,
        workerSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }));
    functions.logger.info(`onUserIdentityUpdate: uid=${uid} apps_updated=${updated}`);
    return null;
});
// ────────────────────────────────────────────────────────────────────────
// Employer profile → jobmetadata.employerSnapshot
// ────────────────────────────────────────────────────────────────────────
function buildEmployerSnapshot(uid, employer, user) {
    var _a;
    return {
        employerId: uid,
        companyName: str(employer.companyName) || str(user.fullName),
        profileImageUrl: str(user.profileImageUrl),
        ratingAvg: num((_a = employer.rating) !== null && _a !== void 0 ? _a : employer.ratingAvg),
        totalHires: num(employer.totalHires),
        isVerified: employer.isVerified === true,
    };
}
exports.onEmployerProfileWrite = functions
    .region(REGION)
    .firestore.document("employer_profiles/{uid}")
    .onWrite(async (change, context) => {
    const uid = context.params.uid;
    if (!change.after.exists) {
        functions.logger.info(`employer_profiles/${uid} deleted; skipping snapshot refresh`);
        return null;
    }
    const employer = change.after.data() || {};
    const userDoc = await db().collection("users").doc(uid).get();
    const user = userDoc.data() || {};
    const snapshot = buildEmployerSnapshot(uid, employer, user);
    const q = db()
        .collection("jobmetadata")
        .where("employerId", "==", uid)
        .where("status", "==", "open");
    const updated = await batchUpdate(q, () => ({
        employerSnapshot: snapshot,
        employerSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }));
    functions.logger.info(`onEmployerProfileWrite: uid=${uid} jobs_updated=${updated}`);
    return null;
});
// ────────────────────────────────────────────────────────────────────────
// Job metadata → applications.jobSnapshot
// ────────────────────────────────────────────────────────────────────────
function buildJobSnapshot(jobId, job) {
    return {
        jobId,
        title: str(job.title),
        companyName: str(job.companyName),
        jobType: str(job.jobType),
        salary: num(job.salary),
        salaryType: str(job.salaryType),
        addressText: str(job.addressText),
        status: str(job.status),
        geohash: str(job.geohash),
    };
}
exports.onJobMetadataUpdate = functions
    .region(REGION)
    .firestore.document("jobmetadata/{jobId}")
    .onUpdate(async (change, context) => {
    const jobId = context.params.jobId;
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const fieldsToWatch = ["title", "companyName", "jobType", "salary", "salaryType", "addressText", "status"];
    const changed = fieldsToWatch.some((k) => str(before[k]) !== str(after[k]) || num(before[k]) !== num(after[k]));
    if (!changed)
        return null;
    const snapshot = buildJobSnapshot(jobId, after);
    const q = db()
        .collection("applications")
        .where("jobId", "==", jobId)
        .where("status", "in", ACTIVE_APP_STATUSES);
    const updated = await batchUpdate(q, () => ({
        jobSnapshot: snapshot,
        jobSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }));
    functions.logger.info(`onJobMetadataUpdate: jobId=${jobId} apps_updated=${updated}`);
    return null;
});
//# sourceMappingURL=snapshots.js.map