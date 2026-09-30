"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitRating = void 0;
/**
 * Ratings — ratings/{jobOrRequestId}_{workerId}_{raterRole}, one per side of a finished piece of work.
 *
 *   submitRating  checks the work was completed (application or instant response), creates the rating
 *                 and updates the target's running average in the same transaction:
 *                 workers → worker_cards.rating/ratingCount, employers → employer_profiles.rating/ratingCount.
 */
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const schema_1 = require("./schema");
const db = admin.firestore();
const { Timestamp } = admin.firestore;
const ID = /^[A-Za-z0-9_-]+$/;
exports.submitRating = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const source = (0, input_1.oneOf)(data, "source", ["job", "instant"], "job");
    const workId = (0, input_1.str)(data, "id", { max: 128, pattern: ID });
    const targetId = (0, input_1.str)(data, "targetId", { max: 128, pattern: ID });
    const stars = (0, input_1.int)(data, "stars", { min: 1, max: 5 });
    const review = (0, input_1.text)(data, "review", { max: 1000, optional: true });
    const tags = (0, input_1.stringList)(data, "tags", { maxItems: 10, maxLength: 40 });
    if (targetId === uid)
        (0, input_1.fail)("invalid-argument", "You cannot rate yourself");
    // Who is the worker in this pair, and did the work finish?
    let workerId = "";
    let employerId = "";
    if (source === "job") {
        for (const candidate of [uid, targetId]) {
            const app = await db.collection(schema_1.Applications.COLLECTION).doc(`${workId}_${candidate}`).get();
            if (app.exists) {
                workerId = String(app.get(schema_1.Applications.WORKER_ID));
                employerId = String(app.get(schema_1.Applications.EMPLOYER_ID));
                const status = app.get(schema_1.Applications.STATUS);
                if (status !== schema_1.Values.ApplicationStatus.COMPLETED && status !== schema_1.Values.ApplicationStatus.HIRED) {
                    (0, input_1.fail)("failed-precondition", "You can rate only after the work is done");
                }
                break;
            }
        }
    }
    else {
        const requestRef = db.collection(schema_1.InstantRequests.COLLECTION).doc(workId);
        const request = await requestRef.get();
        employerId = String(request.get(schema_1.InstantRequests.EMPLOYER_ID) || "");
        workerId = employerId === uid ? targetId : uid;
        const response = await requestRef.collection(schema_1.InstantRequests.Responses.COLLECTION).doc(workerId).get();
        if (!request.exists || !response.exists)
            workerId = "";
        else if (response.get(schema_1.InstantRequests.Responses.STATUS) !== schema_1.Values.InstantResponseStatus.COMPLETED &&
            response.get(schema_1.InstantRequests.Responses.STATUS) !== schema_1.Values.InstantResponseStatus.ACCEPTED) {
            (0, input_1.fail)("failed-precondition", "You can rate only after the work is done");
        }
    }
    if (!workerId || !employerId || ![workerId, employerId].includes(uid) || ![workerId, employerId].includes(targetId)) {
        (0, input_1.fail)("permission-denied", "Only the two people who worked together can rate each other");
    }
    const raterRole = uid === workerId ? schema_1.Values.Role.WORKER : schema_1.Values.Role.EMPLOYER;
    const ratingRef = db.collection(schema_1.Ratings.COLLECTION).doc(`${workId}_${workerId}_${raterRole}`);
    const targetRef = raterRole === schema_1.Values.Role.WORKER ?
        db.collection(schema_1.EmployerProfiles.COLLECTION).doc(employerId) :
        db.collection(schema_1.WorkerCards.COLLECTION).doc(workerId);
    const ratingKey = raterRole === schema_1.Values.Role.WORKER ? schema_1.EmployerProfiles.RATING : schema_1.WorkerCards.RATING;
    const countKey = raterRole === schema_1.Values.Role.WORKER ? schema_1.EmployerProfiles.RATING_COUNT : schema_1.WorkerCards.RATING_COUNT;
    return db.runTransaction(async (tx) => {
        const [existing, target] = await Promise.all([tx.get(ratingRef), tx.get(targetRef)]);
        if (existing.exists)
            return { ok: true, duplicate: true };
        tx.create(ratingRef, {
            [schema_1.Ratings.RATER_ID]: uid,
            [schema_1.Ratings.TARGET_ID]: targetId,
            [schema_1.Ratings.STARS]: stars,
            [schema_1.Ratings.REVIEW]: review,
            [schema_1.Ratings.TAGS]: tags,
            [schema_1.Ratings.CREATED_AT]: Timestamp.now(),
        });
        if (target.exists) {
            const count = Number(target.get(countKey) || 0);
            const avg = Number(target.get(ratingKey) || 0);
            tx.update(targetRef, {
                [ratingKey]: Math.round(((avg * count + stars) / (count + 1)) * 100) / 100,
                [countKey]: count + 1,
            });
        }
        return { ok: true, duplicate: false };
    });
});
//# sourceMappingURL=ratings.js.map