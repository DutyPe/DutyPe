"use strict";
/**
 * Closes finished work automatically.
 *
 * A hired worker used to depend on the employer pressing "mark work done" before the job
 * counted as completed and rating unlocked. When the employer never did, the job sat in
 * hired forever. This closes it after a grace period: 6 hours for regular vacancy jobs,
 * 3 hours for instant/urgent work, which is same-day by design.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.autoCompleteFinishedWork = exports.stampApplicationHiredAt = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const auto_complete_rules_1 = require("./auto-complete-rules");
const notification_i18n_1 = require("./notification-i18n");
const db = admin.firestore();
/** Cap per run so a backlog cannot blow up a single invocation. */
const MAX_PER_RUN = 200;
function toMillis(value) {
    if (!value)
        return 0;
    if (value instanceof admin.firestore.Timestamp)
        return value.toMillis();
    if (value instanceof Date)
        return value.getTime();
    if (typeof value === "number")
        return value < 100000000000 ? value * 1000 : value;
    return 0;
}
/**
 * Stamps hiredAt the first time an application reaches hired, which is what the
 * scheduled sweep measures the grace period from.
 */
exports.stampApplicationHiredAt = functions.firestore
    .document("applications/{applicationId}")
    .onUpdate(async (change) => {
    var _a, _b;
    const after = change.after.data() || {};
    const before = change.before.data() || {};
    const nextStatus = String((_a = after.status) !== null && _a !== void 0 ? _a : "").toLowerCase();
    const prevStatus = String((_b = before.status) !== null && _b !== void 0 ? _b : "").toLowerCase();
    const becameHired = ["hired", "accepted", "in_progress"].includes(nextStatus) &&
        !["hired", "accepted", "in_progress"].includes(prevStatus);
    if (!becameHired || after.hiredAt)
        return null;
    await change.after.ref.set({ hiredAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    return null;
});
async function notifyAutoCompleted(recipientId, jobTitle, jobId) {
    if (!recipientId)
        return;
    const locale = await (0, notification_i18n_1.getUserLanguage)(db, recipientId);
    await db.collection("notifications").add({
        recipientId,
        title: (0, notification_i18n_1.tTitle)("WORK_AUTO_COMPLETED", locale, { title: jobTitle, recipient: "" }),
        message: (0, notification_i18n_1.tBody)("WORK_AUTO_COMPLETED", locale, { title: jobTitle, recipient: "" }),
        type: "APPLICATION_STATUS",
        data: { jobId, status: "completed", autoCompleted: "true" },
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
}
async function autoCompleteApplications(nowMs) {
    var _a, _b, _c, _d;
    const cutoff = admin.firestore.Timestamp.fromMillis((0, auto_complete_rules_1.autoCompleteCutoffMs)(nowMs, "standard"));
    const floor = admin.firestore.Timestamp.fromMillis((0, auto_complete_rules_1.autoCompleteFloorMs)(nowMs));
    const snapshot = await db.collection("applications")
        // Matches every status the decision function treats as active, including the legacy
        // spellings, so the query and the rule can never disagree about what is in scope.
        .where("status", "in", ["hired", "accepted", "in_progress"])
        .where("hiredAt", ">=", floor)
        .where("hiredAt", "<=", cutoff)
        .limit(MAX_PER_RUN)
        .get();
    if (snapshot.empty)
        return 0;
    const batch = db.batch();
    const notifications = [];
    let silentCount = 0;
    for (const doc of snapshot.docs) {
        const data = doc.data();
        const decision = (0, auto_complete_rules_1.shouldAutoComplete)({
            status: String((_a = data.status) !== null && _a !== void 0 ? _a : ""),
            startedAtMs: toMillis(data.hiredAt),
            nowMs,
            kind: "standard",
        });
        if (!decision.complete)
            continue;
        batch.set(doc.ref, {
            status: "completed",
            completedAt: admin.firestore.FieldValue.serverTimestamp(),
            autoCompleted: true,
        }, { merge: true });
        // Backfilled records are closed without telling the worker: the job is months old
        // and a notification about it would only be confusing.
        if (data.autoCompleteSilent === true) {
            silentCount++;
            continue;
        }
        notifications.push({
            workerId: String((_b = data.workerId) !== null && _b !== void 0 ? _b : ""),
            jobId: String((_c = data.jobId) !== null && _c !== void 0 ? _c : ""),
        });
    }
    const total = notifications.length + silentCount;
    if (total === 0)
        return 0;
    await batch.commit();
    for (const item of notifications) {
        const jobSnap = await db.collection("jobmetadata").doc(item.jobId).get();
        const jobTitle = String((_d = jobSnap.get("title")) !== null && _d !== void 0 ? _d : "");
        await notifyAutoCompleted(item.workerId, jobTitle, item.jobId);
    }
    return total;
}
async function autoCompleteInstantResponses(nowMs) {
    var _a;
    const cutoff = admin.firestore.Timestamp.fromMillis((0, auto_complete_rules_1.autoCompleteCutoffMs)(nowMs, "instant"));
    const floor = admin.firestore.Timestamp.fromMillis((0, auto_complete_rules_1.autoCompleteFloorMs)(nowMs));
    const snapshot = await db.collection("instant_responses")
        .where("status", "==", "accepted")
        .where("acceptedAt", ">=", floor)
        .where("acceptedAt", "<=", cutoff)
        .limit(MAX_PER_RUN)
        .get();
    if (snapshot.empty)
        return 0;
    let completed = 0;
    // Written one at a time so the existing instant_responses onWrite trigger can roll each
    // completion up into its parent request.
    for (const doc of snapshot.docs) {
        const data = doc.data();
        const decision = (0, auto_complete_rules_1.shouldAutoComplete)({
            status: String((_a = data.status) !== null && _a !== void 0 ? _a : ""),
            startedAtMs: toMillis(data.acceptedAt),
            nowMs,
            kind: "instant",
        });
        if (!decision.complete)
            continue;
        await doc.ref.set({
            status: "completed",
            completedAt: admin.firestore.FieldValue.serverTimestamp(),
            autoCompleted: true,
        }, { merge: true });
        completed++;
    }
    return completed;
}
exports.autoCompleteFinishedWork = functions.pubsub
    .schedule("every 30 minutes")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const nowMs = Date.now();
    try {
        const [standard, instant] = await Promise.all([
            autoCompleteApplications(nowMs),
            autoCompleteInstantResponses(nowMs),
        ]);
        functions.logger.info(`AUTO-COMPLETE: closed ${standard} application(s) after ` +
            `${auto_complete_rules_1.AUTO_COMPLETE_RULES.STANDARD_GRACE_MS / 3600000}h and ${instant} instant response(s) after ` +
            `${auto_complete_rules_1.AUTO_COMPLETE_RULES.INSTANT_GRACE_MS / 3600000}h`);
    }
    catch (error) {
        functions.logger.error("AUTO-COMPLETE: sweep failed", error);
    }
    return null;
});
//# sourceMappingURL=auto-complete.js.map