"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onJobCallFeedbackWritten = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
/**
 * 3-Worker Consensus Auto-Closer
 *
 * When workers call an employer and submit post-call feedback ("FILLED"),
 * this function tracks distinct worker reports. If 3 or more distinct workers
 * report that the job is already filled:
 * 1. Soft-closes the job ("filled" status) and pauses incoming phone calls (callsStopped = true).
 * 2. Alerts the employer via FCM and In-App notification with an instant 1-tap reopen option.
 * 3. Prevents dead/filled jobs from cluttering the worker home feed.
 */
exports.onJobCallFeedbackWritten = functions.firestore
    .document("job_call_feedback/{feedbackId}")
    .onWrite(async (change, context) => {
    var _a;
    // Ignore deletions
    if (!change.after.exists) {
        return;
    }
    const feedbackData = change.after.data();
    if (!feedbackData)
        return;
    // Only trigger when availability is reported as FILLED
    if (feedbackData.jobAvailability !== "FILLED") {
        return;
    }
    const jobId = feedbackData.jobId;
    if (!jobId || typeof jobId !== "string") {
        return;
    }
    try {
        // Query all feedback for this job where jobAvailability == "FILLED"
        const snap = await db
            .collection("job_call_feedback")
            .where("jobId", "==", jobId)
            .where("jobAvailability", "==", "FILLED")
            .get();
        // Collect distinct worker IDs
        const distinctWorkers = new Set();
        snap.forEach((doc) => {
            const d = doc.data();
            if (d.workerId) {
                distinctWorkers.add(d.workerId);
            }
        });
        functions.logger.info(`Job ${jobId} has ${distinctWorkers.size} distinct worker reports of FILLED.`);
        if (distinctWorkers.size >= 3) {
            // Fetch the job
            const jobDoc = await db.collection("jobs").doc(jobId).get();
            if (!jobDoc.exists)
                return;
            const job = jobDoc.data() || {};
            const currentStatus = (job.status || "").toLowerCase();
            // If already filled or closed, skip
            if (currentStatus === "filled" || currentStatus === "closed" || currentStatus === "completed") {
                functions.logger.info(`Job ${jobId} is already filled/closed.`);
                return;
            }
            const employerId = job.employerId || feedbackData.employerId;
            const jobTitle = job.title || feedbackData.jobTitle || "Job Position";
            functions.logger.info(`Consensus reached (>=3 reports)! Auto-closing job ${jobId} to prevent spam calls.`);
            const now = admin.firestore.FieldValue.serverTimestamp();
            const closePayload = {
                status: "filled",
                callsStopped: true,
                autoClosedReason: "WORKER_CONSENSUS_3_REPORTS",
                autoClosedAt: now,
                updatedAt: now,
            };
            // Update all job collections for consistency
            await Promise.all([
                db.collection("jobs").doc(jobId).set(closePayload, { merge: true }),
                db.collection("jobmetadata").doc(jobId).set(Object.assign(Object.assign({}, closePayload), { status: "FILLED" }), { merge: true }),
                db.collection("job_details").doc(jobId).set(closePayload, { merge: true }),
            ]);
            // Create audit log entry
            await db.collection("job_audit_logs").add({
                jobId,
                action: "AUTO_CLOSED_CONSENSUS",
                reason: "3 distinct workers reported job as filled",
                workerCount: distinctWorkers.size,
                timestamp: now,
            });
            // Notify Employer via in-app notification
            if (employerId) {
                await db
                    .collection("users")
                    .doc(employerId)
                    .collection("notifications")
                    .add({
                    title: "🛑 Calls Paused: Job Marked Filled",
                    body: `3 workers reported that your job "${jobTitle}" is already filled. We paused incoming calls to protect you from spam calls. Tap here if you need to reopen.`,
                    type: "JOB_CONSENSUS_AUTO_CLOSED",
                    jobId,
                    read: false,
                    createdAt: now,
                });
                // Send FCM push notification if employer device token exists
                try {
                    const userDoc = await db.collection("users").doc(employerId).get();
                    const fcmToken = (_a = userDoc.data()) === null || _a === void 0 ? void 0 : _a.fcmToken;
                    if (fcmToken) {
                        await admin.messaging().send({
                            token: fcmToken,
                            notification: {
                                title: "🛑 Calls Paused: Job Marked Filled",
                                body: `3 workers reported that your job "${jobTitle}" is filled. Incoming calls are now stopped.`,
                            },
                            data: {
                                type: "JOB_CONSENSUS_AUTO_CLOSED",
                                jobId,
                            },
                        });
                    }
                }
                catch (fcmErr) {
                    functions.logger.warn("Failed to dispatch FCM push to employer:", fcmErr);
                }
            }
        }
    }
    catch (err) {
        functions.logger.error(`Error during consensus auto-closer for job ${jobId}:`, err);
    }
});
//# sourceMappingURL=job-consensus-closer.js.map