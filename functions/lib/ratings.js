"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitRating = void 0;
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const crypto_1 = require("crypto");
const validation_1 = require("./validation");
const db = admin.firestore();
exports.submitRating = functions.https.onCall(async (data, context) => {
    var _a;
    const userId = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (!userId)
        throw new functions.https.HttpsError("unauthenticated", "Sign in to submit a review");
    const applicationId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.applicationId, "applicationId", {
        required: true, maxLength: 256, pattern: /^[a-zA-Z0-9_-]+$/
    });
    if (!Number.isInteger(data.rating) || data.rating < 1 || data.rating > 5) {
        throw new functions.https.HttpsError("invalid-argument", "Rating must be an integer from 1 to 5");
    }
    if (data.userId !== undefined && data.userId !== userId) {
        throw new functions.https.HttpsError("permission-denied", "Account changed before review submission");
    }
    const review = (0, validation_1.validateString)(data.review, "review", { maxLength: 2000 });
    const tags = Array.from(new Set((0, validation_1.validateArray)(data.tags, "tags", { maxLength: 10 })
        .map(tag => (0, validation_1.validateString)(tag, "tag", { required: true, maxLength: 50 }))));
    const ratingId = (0, crypto_1.createHash)("sha256").update(`${applicationId}\0${userId}`).digest("hex");
    const ratingRef = db.collection("ratings").doc(ratingId);
    const applicationRef = db.collection("job_applications").doc(applicationId);
    const legacyRatings = await db.collection("ratings")
        .where("applicationId", "==", applicationId).where("raterId", "==", userId).limit(1).get();
    return db.runTransaction(async (transaction) => {
        var _a, _b;
        const [applicationDoc, ratingDoc, raterDoc] = await transaction.getAll(applicationRef, ratingRef, db.collection("users").doc(userId));
        if (!applicationDoc.exists)
            throw new functions.https.HttpsError("not-found", "Application not found");
        const application = applicationDoc.data();
        if (application.workerId !== userId && application.employerId !== userId) {
            throw new functions.https.HttpsError("permission-denied", "Only participants can review this work");
        }
        if (application.status !== "COMPLETED" || application.workerId === application.employerId) {
            throw new functions.https.HttpsError("failed-precondition", "Only completed work with another participant can be reviewed");
        }
        const raterRole = application.workerId === userId ? "WORKER" : "EMPLOYER";
        const targetRole = raterRole === "WORKER" ? "EMPLOYER" : "WORKER";
        const targetUserId = raterRole === "WORKER" ? application.employerId : application.workerId;
        if (typeof targetUserId !== "string" || !targetUserId || !raterDoc.exists) {
            throw new functions.https.HttpsError("failed-precondition", "Participant profile is unavailable");
        }
        if (ratingDoc.exists) {
            const previous = ratingDoc.data();
            if (previous.rating !== data.rating || previous.review !== review || JSON.stringify(previous.tags) !== JSON.stringify(tags)) {
                throw new functions.https.HttpsError("already-exists", "This application has already been reviewed");
            }
            return { success: true, ratingId, duplicate: true };
        }
        const targetRef = db.collection("users").doc(targetUserId);
        const targetDoc = await transaction.get(targetRef);
        if (legacyRatings.docs.some(document => document.id !== ratingId)) {
            throw new functions.https.HttpsError("already-exists", "This application has already been reviewed");
        }
        if (!targetDoc.exists)
            throw new functions.https.HttpsError("failed-precondition", "Participant profile is unavailable");
        const averageField = targetRole === "WORKER" ? "workerAverageRating" : "averageRating";
        const countField = targetRole === "WORKER" ? "workerTotalRatings" : "totalRatings";
        const average = (_a = targetDoc.get(averageField)) !== null && _a !== void 0 ? _a : 0;
        const count = (_b = targetDoc.get(countField)) !== null && _b !== void 0 ? _b : 0;
        if (!Number.isSafeInteger(count) || count < 0 || typeof average !== "number" || !Number.isFinite(average) || average < 0 || average > 5) {
            throw new functions.https.HttpsError("failed-precondition", "Rating summary needs reconciliation");
        }
        transaction.create(ratingRef, {
            id: ratingId, applicationId, jobId: application.jobId, raterId: userId,
            raterName: raterDoc.get("fullName") || "", raterRole, targetRole, targetUserId,
            targetUserName: targetDoc.get("fullName") || "", rating: data.rating, review, tags, createdAt: Date.now()
        });
        transaction.update(targetRef, {
            [averageField]: (average * count + data.rating) / (count + 1),
            [countField]: count + 1
        });
        return { success: true, ratingId, duplicate: false };
    });
});
//# sourceMappingURL=ratings.js.map