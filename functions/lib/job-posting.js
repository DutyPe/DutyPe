"use strict";
/**
 * Job Posting Cloud Functions
 *
 * P0 FIX: Idempotency validation to prevent duplicate job postings
 *
 * Features:
 * - Validates idempotency key before creating job
 * - Returns existing job if duplicate detected
 * - Prevents double-posting when user clicks submit multiple times
 *
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.batchUpdateVacancyStatus = exports.createJobWithIdempotency = void 0;
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const db = admin.firestore();
/**
 * Create job with idempotency validation
 *
 * This prevents duplicate job postings by checking the idempotency key
 * before creating a new job. If a job with the same key exists, returns
 * the existing job ID instead of creating a duplicate.
 *
 * Usage from Android:
 * ```kotlin
 * val createJob = functions.getHttpsCallable("createJobWithIdempotency")
 * val result = createJob.call(jobData).await()
 * ```
 */
exports.createJobWithIdempotency = functions.https.onCall(async (data, context) => {
    // Verify authentication
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated to create jobs');
    }
    const { idempotencyKey } = data, jobData = __rest(data, ["idempotencyKey"]);
    // Validate idempotency key
    if (!idempotencyKey || typeof idempotencyKey !== 'string') {
        throw new functions.https.HttpsError('invalid-argument', 'Idempotency key is required');
    }
    try {
        // Check if job with this idempotency key already exists
        const existingJobsSnapshot = await db.collection('jobs')
            .where('idempotencyKey', '==', idempotencyKey)
            .limit(1)
            .get();
        if (!existingJobsSnapshot.empty) {
            const existingJob = existingJobsSnapshot.docs[0];
            functions.logger.info(`Duplicate job detected with idempotency key: ${idempotencyKey}`);
            return {
                jobId: existingJob.id,
                duplicate: true,
                message: 'Job already exists with this idempotency key'
            };
        }
        // Validate required fields
        if (!jobData.title || !jobData.category || !jobData.employerId) {
            throw new functions.https.HttpsError('invalid-argument', 'Missing required fields: title, category, employerId');
        }
        // Create new job with idempotency key
        const jobRef = await db.collection('jobs').add(Object.assign(Object.assign({}, jobData), { idempotencyKey, createdAt: admin.firestore.FieldValue.serverTimestamp(), updatedAt: admin.firestore.FieldValue.serverTimestamp(), isActive: true, isFilled: false }));
        functions.logger.info(`New job created: ${jobRef.id} with idempotency key: ${idempotencyKey}`);
        return {
            jobId: jobRef.id,
            duplicate: false,
            message: 'Job created successfully'
        };
    }
    catch (error) {
        functions.logger.error('Error creating job:', error);
        throw new functions.https.HttpsError('internal', 'Failed to create job', error.message);
    }
});
/**
 * Batch update job vacancy status
 *
 * P1 FIX: Reduces API calls by updating multiple jobs in one request
 * Instead of N calls for N jobs, makes 1 call for all jobs
 *
 * Usage from Android:
 * ```kotlin
 * val batchUpdate = functions.getHttpsCallable("batchUpdateVacancyStatus")
 * val result = batchUpdate.call(mapOf("jobIds" to listOf("id1", "id2"))).await()
 * ```
 */
exports.batchUpdateVacancyStatus = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'Authentication required');
    }
    const { jobIds } = data;
    if (!Array.isArray(jobIds) || jobIds.length === 0) {
        throw new functions.https.HttpsError('invalid-argument', 'jobIds must be a non-empty array');
    }
    // Firestore limit: 10 documents per batch
    if (jobIds.length > 10) {
        throw new functions.https.HttpsError('invalid-argument', 'Maximum 10 jobs per batch');
    }
    try {
        const batch = db.batch();
        const results = {};
        for (const jobId of jobIds) {
            const jobRef = db.collection('jobs').doc(jobId);
            const jobDoc = await jobRef.get();
            if (!jobDoc.exists) {
                results[jobId] = { error: 'Job not found' };
                continue;
            }
            const jobData = jobDoc.data();
            const vacancies = (jobData === null || jobData === void 0 ? void 0 : jobData.vacancies) || 0;
            const applicationsCount = (jobData === null || jobData === void 0 ? void 0 : jobData.applicationsCount) || 0;
            // Calculate vacancy status
            const isFilled = applicationsCount >= vacancies;
            results[jobId] = {
                vacancies,
                applicationsCount,
                isFilled,
                status: isFilled ? 'FILLED' : 'AVAILABLE'
            };
            // Update if status changed
            if ((jobData === null || jobData === void 0 ? void 0 : jobData.isFilled) !== isFilled) {
                batch.update(jobRef, { isFilled });
            }
        }
        await batch.commit();
        return { success: true, results };
    }
    catch (error) {
        functions.logger.error('Batch update error:', error);
        throw new functions.https.HttpsError('internal', 'Batch update failed', error.message);
    }
});
//# sourceMappingURL=job-posting.js.map