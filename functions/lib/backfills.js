"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.runBackfillSnapshots = exports.runBackfillWorkerProfileScores = exports.runBackfillPhoneIndex = void 0;
/**
 * Admin-only backfill callables. Gated to a hard-coded allowlist of admin UIDs
 * to keep them callable from an operator account without needing Cloud Run.
 *
 * Run via:
 *   firebase functions:shell
 *   runBackfillPhoneIndex({})
 *   runBackfillWorkerProfileScores({})
 *   runBackfillSnapshots({})
 *
 * Or from an admin Android build with App Check + auth + allowlisted UID.
 *
 * Each function is paginated, idempotent, and re-entrant.
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const secure_callable_1 = require("./secure-callable");
// Operator UIDs permitted to invoke backfills.
// TODO: move to Firestore /admin_allowlist/{uid} once the ops dashboard lands.
const ADMIN_ALLOWLIST = new Set([
// Add founder UIDs here, e.g. "kLo92abc...".
]);
function requireAdmin(uid) {
    if (!ADMIN_ALLOWLIST.has(uid)) {
        throw new functions.https.HttpsError("permission-denied", "admin allowlist only");
    }
}
const db = () => admin.firestore();
const BATCH_SIZE = 400;
// ────────────────────────────────────────────────────────────────────────
// 1. Backfill phone_index from existing users
// ────────────────────────────────────────────────────────────────────────
exports.runBackfillPhoneIndex = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 540, memory: "512MB" }, async (data, context) => {
    var _a, _b, _c;
    requireAdmin(context.auth.uid);
    const dryRun = (data === null || data === void 0 ? void 0 : data.dryRun) === true;
    let scanned = 0;
    let indexed = 0;
    let skipped = 0;
    let conflicts = 0;
    let lastUid = null;
    let cursor;
    if (data === null || data === void 0 ? void 0 : data.startAfter) {
        const startSnap = await db().collection("users").doc(data.startAfter).get();
        if (startSnap.exists) {
            cursor = startSnap;
        }
    }
    // eslint-disable-next-line no-constant-condition
    while (true) {
        let q = db().collection("users").orderBy("__name__").limit(BATCH_SIZE);
        if (cursor)
            q = q.startAfter(cursor);
        const snap = await q.get();
        if (snap.empty)
            break;
        const batch = db().batch();
        let writes = 0;
        for (const userDoc of snap.docs) {
            scanned++;
            lastUid = userDoc.id;
            const user = userDoc.data() || {};
            const phone = String((_b = (_a = user.phone) !== null && _a !== void 0 ? _a : user.phoneNumber) !== null && _b !== void 0 ? _b : "").trim();
            if (!phone || !/^\+[1-9][0-9]{6,14}$/.test(phone)) {
                skipped++;
                continue;
            }
            const idxRef = db().collection("phone_index").doc(phone);
            const existing = await idxRef.get();
            if (existing.exists) {
                const owner = (_c = existing.data()) === null || _c === void 0 ? void 0 : _c.uid;
                if (owner && owner !== userDoc.id) {
                    conflicts++;
                    functions.logger.warn(`phone_index conflict: ${phone} already → ${owner}, user ${userDoc.id} skipped`);
                    continue;
                }
                skipped++;
                continue;
            }
            if (!dryRun) {
                batch.set(idxRef, {
                    uid: userDoc.id,
                    createdAt: user.createdAt || admin.firestore.FieldValue.serverTimestamp(),
                    backfilledAt: admin.firestore.FieldValue.serverTimestamp(),
                });
                writes++;
            }
            indexed++;
        }
        if (writes > 0)
            await batch.commit();
        if (snap.size < BATCH_SIZE)
            break;
        cursor = snap.docs[snap.docs.length - 1];
    }
    return { scanned, indexed, skipped, conflicts, lastUid };
});
// ────────────────────────────────────────────────────────────────────────
// 2. Backfill workerProfileScore on users + profileScore on worker_profiles
// ────────────────────────────────────────────────────────────────────────
function computeWorkerProfileScore(worker, user) {
    const str = (v) => (typeof v === "string" ? v : v == null ? "" : String(v));
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
exports.runBackfillWorkerProfileScores = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 540, memory: "512MB" }, async (data, context) => {
    var _a, _b;
    requireAdmin(context.auth.uid);
    const dryRun = (data === null || data === void 0 ? void 0 : data.dryRun) === true;
    let scanned = 0;
    let updated = 0;
    let cursor;
    // eslint-disable-next-line no-constant-condition
    while (true) {
        let q = db()
            .collection("worker_profiles")
            .orderBy("__name__")
            .limit(BATCH_SIZE);
        if (cursor)
            q = q.startAfter(cursor);
        const snap = await q.get();
        if (snap.empty)
            break;
        const uids = snap.docs.map((d) => d.id);
        const userDocs = await db().getAll(...uids.map((u) => db().collection("users").doc(u)));
        const batch = db().batch();
        let writes = 0;
        for (let i = 0; i < snap.docs.length; i++) {
            scanned++;
            const workerDoc = snap.docs[i];
            const worker = workerDoc.data() || {};
            const user = userDocs[i].exists ? userDocs[i].data() || {} : {};
            const score = computeWorkerProfileScore(worker, user);
            const workerScoreCurrent = Number((_a = worker.profileScore) !== null && _a !== void 0 ? _a : -1);
            const userScoreCurrent = Number((_b = user.workerProfileScore) !== null && _b !== void 0 ? _b : -1);
            if (workerScoreCurrent === score && userScoreCurrent === score)
                continue;
            if (!dryRun) {
                if (workerScoreCurrent !== score) {
                    batch.set(workerDoc.ref, { profileScore: score, profileScoreUpdatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
                    writes++;
                }
                if (userScoreCurrent !== score) {
                    batch.set(db().collection("users").doc(workerDoc.id), { workerProfileScore: score }, { merge: true });
                    writes++;
                }
            }
            updated++;
        }
        if (writes > 0)
            await batch.commit();
        if (snap.size < BATCH_SIZE)
            break;
        cursor = snap.docs[snap.docs.length - 1];
    }
    return { scanned, updated };
});
// ────────────────────────────────────────────────────────────────────────
// 3. Backfill snapshots on active applications
// ────────────────────────────────────────────────────────────────────────
const ACTIVE_APP_STATUSES = ["applied", "shortlisted"];
exports.runBackfillSnapshots = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 540, memory: "512MB" }, async (data, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r, _s, _t;
    requireAdmin(context.auth.uid);
    const dryRun = (data === null || data === void 0 ? void 0 : data.dryRun) === true;
    const maxDocs = Math.min(Number((_a = data === null || data === void 0 ? void 0 : data.maxDocs) !== null && _a !== void 0 ? _a : 20000), 50000);
    let scanned = 0;
    let updated = 0;
    let missingJob = 0;
    let missingWorker = 0;
    let cursor;
    // eslint-disable-next-line no-constant-condition
    while (scanned < maxDocs) {
        let q = db()
            .collection("applications")
            .where("status", "in", ACTIVE_APP_STATUSES)
            .orderBy("__name__")
            .limit(BATCH_SIZE);
        if (cursor)
            q = q.startAfter(cursor);
        const snap = await q.get();
        if (snap.empty)
            break;
        const batch = db().batch();
        let writes = 0;
        // Gather unique worker/job ids for this batch
        const workerIds = Array.from(new Set(snap.docs.map((d) => String(d.data().workerId || ""))));
        const jobIds = Array.from(new Set(snap.docs.map((d) => String(d.data().jobId || ""))));
        const workerDocs = workerIds.length
            ? await db().getAll(...workerIds.map((u) => db().collection("worker_profiles").doc(u)))
            : [];
        const userDocs = workerIds.length
            ? await db().getAll(...workerIds.map((u) => db().collection("users").doc(u)))
            : [];
        const jobDocs = jobIds.length
            ? await db().getAll(...jobIds.map((j) => db().collection("jobmetadata").doc(j)))
            : [];
        const workerById = new Map(workerIds.map((u, i) => [u, workerDocs[i]]));
        const userById = new Map(workerIds.map((u, i) => [u, userDocs[i]]));
        const jobById = new Map(jobIds.map((j, i) => [j, jobDocs[i]]));
        for (const appDoc of snap.docs) {
            scanned++;
            const app = appDoc.data();
            const workerId = String(app.workerId || "");
            const jobId = String(app.jobId || "");
            const workerDoc = workerById.get(workerId);
            const userDoc = userById.get(workerId);
            const jobDoc = jobById.get(jobId);
            const hasWorker = (_b = workerDoc === null || workerDoc === void 0 ? void 0 : workerDoc.exists) !== null && _b !== void 0 ? _b : false;
            const hasJob = (_c = jobDoc === null || jobDoc === void 0 ? void 0 : jobDoc.exists) !== null && _c !== void 0 ? _c : false;
            if (!hasWorker)
                missingWorker++;
            if (!hasJob)
                missingJob++;
            if (!hasWorker || !hasJob)
                continue;
            const worker = workerDoc.data() || {};
            const user = (userDoc === null || userDoc === void 0 ? void 0 : userDoc.exists) ? userDoc.data() || {} : {};
            const job = jobDoc.data() || {};
            const jobTypes = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
            const workerSnapshot = {
                workerId,
                fullName: String((_d = user.fullName) !== null && _d !== void 0 ? _d : ""),
                profileImageUrl: String((_e = user.profileImageUrl) !== null && _e !== void 0 ? _e : ""),
                primarySkill: jobTypes.length > 0 ? String(jobTypes[0]) : "",
                experience: String((_f = worker.experience) !== null && _f !== void 0 ? _f : ""),
                city: String((_g = user.city) !== null && _g !== void 0 ? _g : ""),
                ratingAvg: Number((_j = (_h = worker.rating) !== null && _h !== void 0 ? _h : worker.ratingAvg) !== null && _j !== void 0 ? _j : 0),
                totalJobs: Number((_k = worker.totalJobs) !== null && _k !== void 0 ? _k : 0),
            };
            const jobSnapshot = {
                jobId,
                title: String((_l = job.title) !== null && _l !== void 0 ? _l : ""),
                companyName: String((_m = job.companyName) !== null && _m !== void 0 ? _m : ""),
                jobType: String((_o = job.jobType) !== null && _o !== void 0 ? _o : ""),
                salary: Number((_p = job.salary) !== null && _p !== void 0 ? _p : 0),
                salaryType: String((_q = job.salaryType) !== null && _q !== void 0 ? _q : ""),
                addressText: String((_r = job.addressText) !== null && _r !== void 0 ? _r : ""),
                status: String((_s = job.status) !== null && _s !== void 0 ? _s : ""),
                geohash: String((_t = job.geohash) !== null && _t !== void 0 ? _t : ""),
            };
            if (!dryRun) {
                batch.set(appDoc.ref, {
                    workerSnapshot,
                    jobSnapshot,
                    workerSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
                    jobSnapshotUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
                }, { merge: true });
                writes++;
            }
            updated++;
        }
        if (writes > 0)
            await batch.commit();
        if (snap.size < BATCH_SIZE)
            break;
        cursor = snap.docs[snap.docs.length - 1];
    }
    return { scanned, updated, missingJob, missingWorker };
});
//# sourceMappingURL=backfills.js.map