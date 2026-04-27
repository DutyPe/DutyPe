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
 * by Cloud Functions. It has every field the employer's job-list card
 * needs (title, salary text, location, status, hero image, per-status
 * application counts, last-application timestamp) so the client can
 * render without any JOIN against jobmetadata / job_details / applications.
 *
 * Schema:
 *   employer_job_cards/{jobId} = {
 *     jobId, employerId, title, jobType, status,
 *     salary, salaryType,
 *     location: { lat, lng },
 *     addressText, geohash, jobImageUrl?,
 *     vacancies, urgency, workingHours, shiftTiming,
 *     applicationCount, shortlistedCount, hiredCount,
 *     completedCount, rejectedCount,
 *     lastApplicationAt?,
 *     createdAt, updatedAt
 *   }
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
const FIELD = admin.firestore.FieldValue;
const CARDS = "employer_job_cards";
async function buildDenormFromJob(jobId) {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r, _s, _t;
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
        title: String((_c = meta.title) !== null && _c !== void 0 ? _c : ""),
        jobType: String((_d = meta.jobType) !== null && _d !== void 0 ? _d : ""),
        status: String((_e = meta.status) !== null && _e !== void 0 ? _e : "open"),
        salary: String((_f = meta.salary) !== null && _f !== void 0 ? _f : ""),
        salaryType: String((_g = meta.salaryType) !== null && _g !== void 0 ? _g : ""),
        location: meta.location && typeof meta.location === "object"
            ? {
                lat: Number((_h = meta.location.lat) !== null && _h !== void 0 ? _h : 0),
                lng: Number((_j = meta.location.lng) !== null && _j !== void 0 ? _j : 0),
            }
            : null,
        addressText: String((_k = meta.addressText) !== null && _k !== void 0 ? _k : ""),
        geohash: String((_l = meta.geohash) !== null && _l !== void 0 ? _l : ""),
        jobImageUrl: meta.jobImageUrl ? String(meta.jobImageUrl) : null,
        vacancies: Number((_o = (_m = meta.vacancies) !== null && _m !== void 0 ? _m : details.vacancies) !== null && _o !== void 0 ? _o : 1),
        shiftTiming: String((_q = (_p = details.shiftTiming) !== null && _p !== void 0 ? _p : meta.shiftTiming) !== null && _q !== void 0 ? _q : "Flexible"),
        companyName: String((_r = meta.companyName) !== null && _r !== void 0 ? _r : ""),
        contactNumber: String((_s = details.contactNumber) !== null && _s !== void 0 ? _s : ""),
        createdAt: (_t = meta.createdAt) !== null && _t !== void 0 ? _t : null,
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
        await cardRef.set(Object.assign(Object.assign({}, denorm), { updatedAt: FIELD.serverTimestamp() }), { merge: true });
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
    var _a, _b, _c, _d;
    const before = change.before.exists ? change.before.data() || {} : null;
    const after = change.after.exists ? change.after.data() || {} : null;
    const jobId = String((_b = ((_a = after === null || after === void 0 ? void 0 : after.jobId) !== null && _a !== void 0 ? _a : before === null || before === void 0 ? void 0 : before.jobId)) !== null && _b !== void 0 ? _b : "");
    if (!jobId)
        return;
    const prevStatus = String((_c = before === null || before === void 0 ? void 0 : before.status) !== null && _c !== void 0 ? _c : "");
    const nextStatus = String((_d = after === null || after === void 0 ? void 0 : after.status) !== null && _d !== void 0 ? _d : "");
    // Compute counter deltas for: applicationCount (total non-deleted),
    // shortlistedCount, hiredCount, completedCount, rejectedCount.
    const deltas = {};
    const bump = (field, by) => {
        if (by === 0)
            return;
        deltas[field] = FIELD.increment(by);
    };
    if (!before && after) {
        // Created.
        bump("applicationCount", 1);
        bump(statusCounterField(nextStatus), 1);
        deltas["lastApplicationAt"] = FIELD.serverTimestamp();
    }
    else if (before && !after) {
        // Deleted.
        bump("applicationCount", -1);
        bump(statusCounterField(prevStatus), -1);
    }
    else if (before && after && prevStatus !== nextStatus) {
        // Status transitioned.
        bump(statusCounterField(prevStatus), -1);
        bump(statusCounterField(nextStatus), 1);
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
function statusCounterField(status) {
    switch (status.toLowerCase()) {
        case "shortlisted":
            return "shortlistedCount";
        case "hired":
            return "hiredCount";
        case "completed":
            return "completedCount";
        case "rejected":
            return "rejectedCount";
        case "withdrawn":
            return "withdrawnCount";
        default:
            // applied / unknown -> only contributes to the total.
            return "appliedCount";
    }
}
//# sourceMappingURL=employer-cards.js.map