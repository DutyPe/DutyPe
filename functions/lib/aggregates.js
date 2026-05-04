"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onApplicationCreatedIncrementApplicationCount = exports.onApplicationHired = exports.onRatingCreated = void 0;
/**
 * Aggregate maintainers — Cloud Functions triggers that keep the
 * client-forbidden aggregate fields (rating, totalRatings, totalJobs,
 * totalHires) correct on worker_profiles / employer_profiles.
 *
 * Design notes:
 *   • Every aggregate mutation runs inside a Firestore transaction so concurrent
 *     events don't step on each other.
 *   • The rating averages are computed incrementally using the running mean
 *     formula:  new_avg = old_avg + (new_value - old_avg) / new_count
 *     This avoids a full table scan on every rating.
 *   • Counters are bumped exactly once per state transition (onUpdate checks
 *     the previous value to avoid double-counting on no-op writes).
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;
/**
 * When a rating is created, bump the target user's running average and
 * total count on whichever profile collection matches their role.
 */
exports.onRatingCreated = functions.firestore
    .document("ratings/{ratingId}")
    .onCreate(async (snap) => {
    var _a, _b;
    const rating = snap.data();
    const toUserId = String((_a = rating === null || rating === void 0 ? void 0 : rating.toUserId) !== null && _a !== void 0 ? _a : "");
    const value = Number((_b = rating === null || rating === void 0 ? void 0 : rating.rating) !== null && _b !== void 0 ? _b : 0);
    if (!toUserId || !Number.isFinite(value) || value < 1 || value > 5) {
        functions.logger.warn("onRatingCreated: invalid payload", { ratingId: snap.id });
        return;
    }
    const targetRef = await resolveRatingTargetProfileRef(rating, snap.id);
    if (!targetRef)
        return;
    await updateRollingAverage(targetRef, value);
});
async function resolveRatingTargetProfileRef(rating, ratingId) {
    var _a, _b, _c;
    const toUserId = String((_a = rating === null || rating === void 0 ? void 0 : rating.toUserId) !== null && _a !== void 0 ? _a : "");
    const explicitRole = normalizeRatingTargetRole((_c = (_b = rating === null || rating === void 0 ? void 0 : rating.targetRole) !== null && _b !== void 0 ? _b : rating === null || rating === void 0 ? void 0 : rating.toRole) !== null && _c !== void 0 ? _c : rating === null || rating === void 0 ? void 0 : rating.ratedRole);
    if (explicitRole) {
        return profileRefForRole(toUserId, explicitRole);
    }
    const inferredRole = await inferRatingTargetRole(rating);
    if (inferredRole) {
        return profileRefForRole(toUserId, inferredRole);
    }
    const workerRef = db.doc(`worker_profiles/${toUserId}`);
    const employerRef = db.doc(`employer_profiles/${toUserId}`);
    const [workerSnap, employerSnap] = await Promise.all([
        workerRef.get(),
        employerRef.get(),
    ]);
    if (workerSnap.exists && !employerSnap.exists)
        return workerRef;
    if (employerSnap.exists && !workerSnap.exists)
        return employerRef;
    functions.logger.warn("onRatingCreated: ambiguous target role, skipped aggregate", {
        ratingId,
        toUserId,
        workerProfileExists: workerSnap.exists,
        employerProfileExists: employerSnap.exists,
    });
    return null;
}
function normalizeRatingTargetRole(value) {
    const normalized = String(value !== null && value !== void 0 ? value : "").trim().toUpperCase();
    if (normalized === "WORKER")
        return "WORKER";
    if (normalized === "EMPLOYER")
        return "EMPLOYER";
    return null;
}
function profileRefForRole(toUserId, role) {
    const collection = role === "WORKER" ? "worker_profiles" : "employer_profiles";
    return db.doc(`${collection}/${toUserId}`);
}
async function inferRatingTargetRole(rating) {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k;
    const jobId = String((_a = rating === null || rating === void 0 ? void 0 : rating.jobId) !== null && _a !== void 0 ? _a : "");
    const fromUserId = String((_b = rating === null || rating === void 0 ? void 0 : rating.fromUserId) !== null && _b !== void 0 ? _b : "");
    const toUserId = String((_c = rating === null || rating === void 0 ? void 0 : rating.toUserId) !== null && _c !== void 0 ? _c : "");
    if (!jobId || !fromUserId || !toUserId)
        return null;
    const fromSideApplication = await db.doc(`applications/${jobId}_${fromUserId}`).get();
    if (applicationIsCompleted(fromSideApplication.data()) &&
        String((_d = fromSideApplication.get("employerId")) !== null && _d !== void 0 ? _d : "") === toUserId) {
        return "EMPLOYER";
    }
    const toSideApplication = await db.doc(`applications/${jobId}_${toUserId}`).get();
    if (applicationIsCompleted(toSideApplication.data()) &&
        String((_e = toSideApplication.get("workerId")) !== null && _e !== void 0 ? _e : "") === toUserId &&
        String((_f = toSideApplication.get("employerId")) !== null && _f !== void 0 ? _f : "") === fromUserId) {
        return "WORKER";
    }
    const fromSideInstant = await db.doc(`instant_responses/${jobId}_${fromUserId}`).get();
    if (instantResponseIsCompleted(fromSideInstant.data()) &&
        String((_g = fromSideInstant.get("workerId")) !== null && _g !== void 0 ? _g : "") === fromUserId &&
        String((_h = fromSideInstant.get("employerId")) !== null && _h !== void 0 ? _h : "") === toUserId) {
        return "EMPLOYER";
    }
    const toSideInstant = await db.doc(`instant_responses/${jobId}_${toUserId}`).get();
    if (instantResponseIsCompleted(toSideInstant.data()) &&
        String((_j = toSideInstant.get("workerId")) !== null && _j !== void 0 ? _j : "") === toUserId &&
        String((_k = toSideInstant.get("employerId")) !== null && _k !== void 0 ? _k : "") === fromUserId) {
        return "WORKER";
    }
    return null;
}
function applicationIsCompleted(data) {
    var _a;
    const status = String((_a = data === null || data === void 0 ? void 0 : data.status) !== null && _a !== void 0 ? _a : "").toLowerCase();
    return status === "hired" || status === "completed";
}
function instantResponseIsCompleted(data) {
    var _a;
    return String((_a = data === null || data === void 0 ? void 0 : data.status) !== null && _a !== void 0 ? _a : "").toLowerCase() === "completed";
}
async function updateRollingAverage(ref, newValue) {
    await db.runTransaction(async (tx) => {
        var _a, _b;
        const snap = await tx.get(ref);
        if (!snap.exists)
            return; // profile doesn't exist for this role — skip
        const data = snap.data() || {};
        const prevCount = Number((_a = data.totalRatings) !== null && _a !== void 0 ? _a : 0);
        const prevAvg = Number((_b = data.rating) !== null && _b !== void 0 ? _b : 0);
        const newCount = prevCount + 1;
        const newAvg = prevAvg + (newValue - prevAvg) / newCount;
        tx.update(ref, {
            rating: Math.round(newAvg * 100) / 100,
            totalRatings: newCount,
        });
    });
}
/**
 * When an application transitions to status='hired' (and wasn't hired
 * before), increment:
 *   • worker_profiles/{workerId}.totalJobs
 *   • employer_profiles/{employerId}.totalHires
 *
 * onUpdate is used rather than onWrite so we don't re-fire on deletes, and
 * we check the previous state so re-writes of the same status are no-ops.
 */
exports.onApplicationHired = functions.firestore
    .document("applications/{applicationId}")
    .onUpdate(async (change) => {
    var _a, _b, _c, _d;
    const before = change.before.data() || {};
    const after = change.after.data() || {};
    const prev = String((_a = before.status) !== null && _a !== void 0 ? _a : "");
    const next = String((_b = after.status) !== null && _b !== void 0 ? _b : "");
    if (next !== "hired" || prev === "hired")
        return;
    const workerId = String((_c = after.workerId) !== null && _c !== void 0 ? _c : "");
    const employerId = String((_d = after.employerId) !== null && _d !== void 0 ? _d : "");
    if (!workerId || !employerId) {
        functions.logger.warn("onApplicationHired: missing ids", { id: change.after.id });
        return;
    }
    await Promise.all([
        safeIncrement(db.doc(`worker_profiles/${workerId}`), { totalJobs: FIELD.increment(1) }),
        safeIncrement(db.doc(`employer_profiles/${employerId}`), { totalHires: FIELD.increment(1) }),
    ]);
});
/**
 * Keep per-job application counters in sync for employer views.
 * This runs on application create and updates `job_details/{jobId}.applicationCount`.
 */
exports.onApplicationCreatedIncrementApplicationCount = functions.firestore
    .document("applications/{applicationId}")
    .onCreate(async (snap) => {
    var _a;
    const data = snap.data() || {};
    const jobId = String((_a = data.jobId) !== null && _a !== void 0 ? _a : "");
    if (!jobId) {
        functions.logger.warn("onApplicationCreatedIncrementApplicationCount: missing jobId", { id: snap.id });
        return;
    }
    try {
        await db.doc(`job_details/${jobId}`).set({ applicationCount: FIELD.increment(1) }, { merge: true });
    }
    catch (e) {
        functions.logger.error("onApplicationCreatedIncrementApplicationCount failed", {
            id: snap.id,
            jobId,
            err: e === null || e === void 0 ? void 0 : e.message,
        });
    }
});
async function safeIncrement(ref, updates) {
    try {
        const snap = await ref.get();
        if (!snap.exists)
            return;
        await ref.update(updates);
    }
    catch (e) {
        functions.logger.error("safeIncrement failed", { ref: ref.path, err: e === null || e === void 0 ? void 0 : e.message });
    }
}
//# sourceMappingURL=aggregates.js.map