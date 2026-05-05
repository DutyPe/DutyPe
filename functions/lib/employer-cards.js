"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onApplicationWriteSyncEmployerCard = exports.onJobMetadataWriteSyncEmployerCard = void 0;
/**
 * Employer-card aggregate.
 *
 * Bug #4 fix (April 2026): the employer home screen was showing
 * `applicationsReceived = 0` on every card because the existing
 * `jobmetadata` document is shared with the worker side and only carries
 * the slim public payload — application counts were being written to
 * `job_details.applicationCount` (private, employer-only via callable)
 * and never reached the card list.
 *
 * Solution: a dedicated `employer_job_cards/{jobId}` document maintained
 * by Cloud Functions.
 *
 * Current client reality (May 2026): Android reads ONLY
 * `employerId` (query filter) + `applicationCount` (card badge).
 * Keeping extra denormalized fields here increases write size and drift
 * without adding value. This trigger now stores only fields that are read.
 *
 * Schema:
 *   employer_job_cards/{jobId} = {
 *     jobId,
 *     employerId,
 *     applicationCount,
 *     updatedAt
 *   }
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;
const CARDS = "employer_job_cards";
const UNUSED_CARD_FIELDS = [
    "title",
    "jobType",
    "status",
    "salary",
    "salaryType",
    "location",
    "addressText",
    "geohash",
    "jobImageUrl",
    "vacancies",
    "shiftTiming",
    "companyName",
    "contactNumber",
    "shortlistedCount",
    "hiredCount",
    "completedCount",
    "rejectedCount",
    "withdrawnCount",
    "appliedCount",
    "lastApplicationAt",
    "createdAt",
];
async function buildDenormFromJob(jobId) {
    var _a, _b;
    const [metaSnap, detailsSnap] = await Promise.all([
        db.collection("jobmetadata").doc(jobId).get(),
        db.collection("job_details").doc(jobId).get(),
    ]);
    if (!metaSnap.exists)
        return null;
    const meta = metaSnap.data() || {};
    const details = (detailsSnap.exists ? detailsSnap.data() : {}) || {};
    const employerId = String((_b = (_a = meta.employerId) !== null && _a !== void 0 ? _a : details.employerId) !== null && _b !== void 0 ? _b : "");
    if (!employerId) {
        functions.logger.warn("buildDenormFromJob: missing employerId", { jobId });
        return null;
    }
    return {
        jobId,
        employerId,
    };
}
/**
 * On every jobmetadata write, refresh the matching employer_job_cards
 * doc with the latest denormalized snapshot. We deliberately do not
 * touch the per-status counters here — those are owned by the
 * applications trigger below.
 */
exports.onJobMetadataWriteSyncEmployerCard = functions.firestore
    .document("jobmetadata/{jobId}")
    .onWrite(async (change, context) => {
    const jobId = context.params.jobId;
    const cardRef = db.collection(CARDS).doc(jobId);
    if (!change.after.exists) {
        // jobmetadata deleted — drop the card too.
        await cardRef.delete().catch((e) => {
            functions.logger.warn("delete employer_job_card failed", { jobId, err: e === null || e === void 0 ? void 0 : e.message });
        });
        return;
    }
    const denorm = await buildDenormFromJob(jobId);
    if (!denorm)
        return;
    try {
        if (change.after.get("searchKeywords") !== undefined) {
            await change.after.ref.update({ searchKeywords: FIELD.delete() });
        }
        const removeUnused = {};
        for (const key of UNUSED_CARD_FIELDS) {
            removeUnused[key] = FIELD.delete();
        }
        await cardRef.set(Object.assign(Object.assign(Object.assign({}, denorm), removeUnused), { updatedAt: FIELD.serverTimestamp() }), { merge: true });
    }
    catch (e) {
        functions.logger.error("upsert employer_job_card failed", { jobId, err: e === null || e === void 0 ? void 0 : e.message });
    }
});
/**
 * Maintain per-status counters on employer_job_cards from the
 * applications collection. We use `onWrite` so creates, status updates
 * and deletes all flow through one place.
 */
exports.onApplicationWriteSyncEmployerCard = functions.firestore
    .document("applications/{applicationId}")
    .onWrite(async (change) => {
    var _a, _b;
    const before = change.before.exists ? change.before.data() || {} : null;
    const after = change.after.exists ? change.after.data() || {} : null;
    const jobId = String((_b = ((_a = after === null || after === void 0 ? void 0 : after.jobId) !== null && _a !== void 0 ? _a : before === null || before === void 0 ? void 0 : before.jobId)) !== null && _b !== void 0 ? _b : "");
    if (!jobId)
        return;
    // applicationCount is the only counter consumed by current clients.
    const deltas = {};
    const bump = (by) => {
        if (by === 0)
            return;
        deltas["applicationCount"] = FIELD.increment(by);
    };
    if (!before && after) {
        // Created.
        bump(1);
    }
    else if (before && !after) {
        // Deleted.
        bump(-1);
    }
    if (Object.keys(deltas).length === 0)
        return;
    const cardRef = db.collection(CARDS).doc(jobId);
    try {
        // Use set+merge so the doc is auto-created if the metadata trigger has not run yet.
        await cardRef.set(deltas, { merge: true });
    }
    catch (e) {
        functions.logger.error("update employer_job_card counters failed", {
            jobId,
            err: e === null || e === void 0 ? void 0 : e.message,
        });
    }
});
//# sourceMappingURL=employer-cards.js.map