"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onApplicationHired = exports.onRatingCreated = void 0;
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
    // Update both worker_profiles and employer_profiles if they exist for the user.
    await Promise.all([
        updateRollingAverage(db.doc(`worker_profiles/${toUserId}`), value),
        updateRollingAverage(db.doc(`employer_profiles/${toUserId}`), value),
    ]);
});
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