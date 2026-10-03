"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BUSY_MESSAGE = void 0;
exports.busyWith = busyWith;
/**
 * "Busy" rule: a worker on an unfinished job cannot take another one.
 *
 * Busy means any of
 *   - an urgent/instant job they were selected for that is still open or filled,
 *   - a DutyPe Services booking assigned to them that is not finished,
 *   - a regular job they were hired for less than the auto-complete grace period ago.
 * Stale pointers (that job already ended) never block.
 */
const admin = require("firebase-admin");
const auto_complete_rules_1 = require("../auto-complete-rules");
const schema_1 = require("../schema");
const ACTIVE_SERVICE = ["ASSIGNED", "ON_THE_WAY", "STARTED"];
exports.BUSY_MESSAGE = {
    urgent: "Finish your current urgent job first. You can apply for new jobs after it is completed.",
    service: "Finish your current DutyPe Services job first. You can apply for new jobs after it is completed.",
    job: "You were just hired for a job. You can apply for new jobs after it is completed.",
};
/**
 * [worker] is the worker profile snapshot (already read). Reads go through [tx] when given so
 * the check can run inside a transaction (before any write).
 */
async function busyWith(uid, worker, tx, ignoreUrgentId = "") {
    const db = admin.firestore();
    const read = (ref) => tx ? tx.get(ref) : ref.get();
    const urgentId = String((worker === null || worker === void 0 ? void 0 : worker.get(schema_1.WorkerProfiles.ACTIVE_URGENT_ID)) || "");
    if (urgentId && urgentId !== ignoreUrgentId) {
        const r = await read(db.collection(schema_1.InstantRequests.COLLECTION).doc(urgentId));
        const S = schema_1.Values.InstantStatus;
        if (r.exists && [S.OPEN, S.FILLED].includes(r.get(schema_1.InstantRequests.STATUS)) &&
            (r.get(schema_1.InstantRequests.SELECTED_WORKER_IDS) || []).includes(uid))
            return "urgent";
    }
    const partner = await read(db.collection(schema_1.ServicePartners.COLLECTION).doc(uid));
    const bookingId = String(partner.get(schema_1.ServicePartners.ACTIVE_BOOKING_ID) || "");
    if (bookingId) {
        const b = await read(db.collection(schema_1.ServiceBookings.COLLECTION).doc(bookingId));
        if (b.exists && b.get(schema_1.ServiceBookings.PARTNER_ID) === uid &&
            ACTIVE_SERVICE.includes(String(b.get(schema_1.ServiceBookings.STATUS))))
            return "service";
    }
    const since = admin.firestore.Timestamp.fromMillis(Date.now() - auto_complete_rules_1.AUTO_COMPLETE_RULES.STANDARD_GRACE_MS);
    const q = db.collection(schema_1.Applications.COLLECTION)
        .where(schema_1.Applications.WORKER_ID, "==", uid)
        .where(schema_1.Applications.STATUS, "==", schema_1.Values.ApplicationStatus.HIRED)
        .where(schema_1.Applications.HIRED_AT, ">", since)
        .limit(1);
    const hired = tx ? await tx.get(q) : await q.get();
    if (!hired.empty)
        return "job";
    return null;
}
//# sourceMappingURL=busy.js.map