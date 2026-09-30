"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getReportStats = exports.onJobReportCreated = void 0;
/**
 * Job reports — crowd moderation.
 *
 *   onJobReportCreated  counts reports for the job (count aggregation); at AUTO_HIDE_THRESHOLD the job
 *                       is closed and the employer told. NON_PAYMENT reports also notify the employer.
 *   getReportStats      admin dashboard counters (count aggregations, no document reads)
 *
 * Reports are created by the app at job_reports/{jobId}_{reporterId} (one per person per job).
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const notify_1 = require("./lib/notify");
const app_config_1 = require("./app-config");
const schema_1 = require("./schema");
const db = admin.firestore();
const AUTO_HIDE_THRESHOLD = 3;
exports.onJobReportCreated = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.JobReports.COLLECTION}/{reportId}`)
    .onCreate(async (snap) => {
    const report = snap.data();
    const jobId = String(report[schema_1.JobReports.JOB_ID] || "");
    if (!jobId)
        return null;
    const jobRef = db.collection(schema_1.Jobs.COLLECTION).doc(jobId);
    const [job, count] = await Promise.all([
        jobRef.get(),
        db.collection(schema_1.JobReports.COLLECTION).where(schema_1.JobReports.JOB_ID, "==", jobId).count().get(),
    ]);
    if (!job.exists)
        return null;
    const employerId = String(job.get(schema_1.Jobs.EMPLOYER_ID) || "");
    const title = String(job.get(schema_1.Jobs.TITLE) || "");
    const reports = count.data().count;
    if (reports >= AUTO_HIDE_THRESHOLD && job.get(schema_1.Jobs.STATUS) === schema_1.Values.JobStatus.OPEN) {
        await jobRef.update({ [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.CLOSED });
        functions.logger.warn(`job ${jobId} closed after ${reports} reports`);
        if (employerId) {
            await (0, notify_1.notify)(employerId, {
                type: "GENERAL",
                templateId: "JOB_HIDDEN_REPORTS",
                params: { title },
                data: { jobId },
                role: schema_1.Values.Role.EMPLOYER,
            });
        }
    }
    if (report[schema_1.JobReports.REASON] === "NON_PAYMENT" && employerId) {
        await (0, notify_1.notify)(employerId, {
            type: "GENERAL",
            templateId: "NON_PAYMENT_REPORTED",
            params: { title },
            data: { jobId },
            role: schema_1.Values.Role.EMPLOYER,
        });
    }
    return null;
});
exports.getReportStats = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (_raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const since = (ms) => admin.firestore.Timestamp.fromMillis(Date.now() - ms);
    const col = db.collection(schema_1.JobReports.COLLECTION);
    const [daily, weekly, open] = await Promise.all([
        col.where(schema_1.JobReports.CREATED_AT, ">", since(24 * 60 * 60 * 1000)).count().get(),
        col.where(schema_1.JobReports.CREATED_AT, ">", since(7 * 24 * 60 * 60 * 1000)).count().get(),
        col.where(schema_1.JobReports.STATUS, "==", "open").count().get(),
    ]);
    return {
        dailyReports: daily.data().count,
        weeklyReports: weekly.data().count,
        pendingModeration: open.data().count,
    };
});
//# sourceMappingURL=reports.js.map