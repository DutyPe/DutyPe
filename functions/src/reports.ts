/**
 * Job reports — crowd moderation.
 *
 *   onJobReportCreated  counts reports for the job (count aggregation); at AUTO_HIDE_THRESHOLD the job
 *                       is closed and the employer told. NON_PAYMENT reports also notify the employer.
 *   getReportStats      admin dashboard counters (count aggregations, no document reads)
 *
 * Reports are created by the app at job_reports/{jobId}_{reporterId} (one per person per job).
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail } from "./lib/input";
import { notify } from "./lib/notify";
import { isCallerAdmin } from "./app-config";
import { JobReports, Jobs, Values } from "./schema";

const db = admin.firestore();
const AUTO_HIDE_THRESHOLD = 3;

export const onJobReportCreated = functions
  .region("asia-south1")
  .firestore.document(`${JobReports.COLLECTION}/{reportId}`)
  .onCreate(async (snap) => {
    const report = snap.data();
    const jobId = String(report[JobReports.JOB_ID] || "");
    if (!jobId) return null;
    const jobRef = db.collection(Jobs.COLLECTION).doc(jobId);
    const [job, count] = await Promise.all([
      jobRef.get(),
      db.collection(JobReports.COLLECTION).where(JobReports.JOB_ID, "==", jobId).count().get(),
    ]);
    if (!job.exists) return null;
    const employerId = String(job.get(Jobs.EMPLOYER_ID) || "");
    const title = String(job.get(Jobs.TITLE) || "");
    const reports = count.data().count;

    if (reports >= AUTO_HIDE_THRESHOLD && job.get(Jobs.STATUS) === Values.JobStatus.OPEN) {
      await jobRef.update({ [Jobs.STATUS]: Values.JobStatus.CLOSED });
      functions.logger.warn(`job ${jobId} closed after ${reports} reports`);
      if (employerId) {
        await notify(employerId, {
          type: "GENERAL",
          templateId: "JOB_HIDDEN_REPORTS",
          params: { title },
          data: { jobId },
          role: Values.Role.EMPLOYER,
        });
      }
    }
    if (report[JobReports.REASON] === "NON_PAYMENT" && employerId) {
      await notify(employerId, {
        type: "GENERAL",
        templateId: "NON_PAYMENT_REPORTED",
        params: { title },
        data: { jobId },
        role: Values.Role.EMPLOYER,
      });
    }
    return null;
  });

export const getReportStats = onCallSecured({ enforceAppCheck: false }, async (_raw: unknown, context) => {
  if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
  const since = (ms: number) => admin.firestore.Timestamp.fromMillis(Date.now() - ms);
  const col = db.collection(JobReports.COLLECTION);
  const [daily, weekly, open] = await Promise.all([
    col.where(JobReports.CREATED_AT, ">", since(24 * 60 * 60 * 1000)).count().get(),
    col.where(JobReports.CREATED_AT, ">", since(7 * 24 * 60 * 60 * 1000)).count().get(),
    col.where(JobReports.STATUS, "==", "open").count().get(),
  ]);
  return {
    dailyReports: daily.data().count,
    weeklyReports: weekly.data().count,
    pendingModeration: open.data().count,
  };
});
