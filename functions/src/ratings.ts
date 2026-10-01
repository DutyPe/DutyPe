/**
 * Ratings — ratings/{jobOrRequestId}_{workerId}_{raterRole}, one per side of a finished piece of work.
 *
 *   submitRating  checks the work was completed (application or instant response), creates the rating
 *                 and updates the target's running average in the same transaction:
 *                 workers → worker_cards.rating/ratingCount, employers → employer_profiles.rating/ratingCount.
 */
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, int, text, stringList, oneOf } from "./lib/input";
import {
  Applications, EmployerProfiles, InstantRequests, Ratings, Values, WorkerCards,
} from "./schema";

const db = admin.firestore();
const { Timestamp } = admin.firestore;
const ID = /^[A-Za-z0-9_-]+$/;

export const submitRating = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const source = oneOf(data, "source", ["job", "instant"] as const, "job");
  const workId = str(data, "id", { max: 128, pattern: ID });
  const targetId = str(data, "targetId", { max: 128, pattern: ID });
  const stars = int(data, "stars", { min: 1, max: 5 });
  const review = text(data, "review", { max: 1000, optional: true });
  const tags = stringList(data, "tags", { maxItems: 10, maxLength: 40 });
  if (targetId === uid) fail("invalid-argument", "You cannot rate yourself");

  // Who is the worker in this pair, and did the work finish?
  let workerId = "";
  let employerId = "";
  if (source === "job") {
    for (const candidate of [uid, targetId]) {
      const app = await db.collection(Applications.COLLECTION).doc(`${workId}_${candidate}`).get();
      if (app.exists) {
        workerId = String(app.get(Applications.WORKER_ID));
        employerId = String(app.get(Applications.EMPLOYER_ID));
        const status = app.get(Applications.STATUS);
        if (status !== Values.ApplicationStatus.COMPLETED && status !== Values.ApplicationStatus.HIRED) {
          fail("failed-precondition", "You can rate only after the work is done");
        }
        break;
      }
    }
  } else {
    const requestRef = db.collection(InstantRequests.COLLECTION).doc(workId);
    const request = await requestRef.get();
    employerId = String(request.get(InstantRequests.EMPLOYER_ID) || "");
    workerId = employerId === uid ? targetId : uid;
    const response = await requestRef.collection(InstantRequests.Responses.COLLECTION).doc(workerId).get();
    if (!request.exists || !response.exists) workerId = "";
    else if (response.get(InstantRequests.Responses.STATUS) !== Values.InstantResponseStatus.COMPLETED &&
      response.get(InstantRequests.Responses.STATUS) !== Values.InstantResponseStatus.ACCEPTED) {
      fail("failed-precondition", "You can rate only after the work is done");
    }
  }
  if (!workerId || !employerId || ![workerId, employerId].includes(uid) || ![workerId, employerId].includes(targetId)) {
    fail("permission-denied", "Only the two people who worked together can rate each other");
  }

  const raterRole = uid === workerId ? Values.Role.WORKER : Values.Role.EMPLOYER;
  const ratingRef = db.collection(Ratings.COLLECTION).doc(`${workId}_${workerId}_${raterRole}`);
  const targetRef = raterRole === Values.Role.WORKER ?
    db.collection(EmployerProfiles.COLLECTION).doc(employerId) :
    db.collection(WorkerCards.COLLECTION).doc(workerId);
  const ratingKey = raterRole === Values.Role.WORKER ? EmployerProfiles.RATING : WorkerCards.RATING;
  const countKey = raterRole === Values.Role.WORKER ? EmployerProfiles.RATING_COUNT : WorkerCards.RATING_COUNT;

  return db.runTransaction(async (tx) => {
    const [existing, target] = await Promise.all([tx.get(ratingRef), tx.get(targetRef)]);
    if (existing.exists) return { ok: true, duplicate: true };
    tx.create(ratingRef, {
      [Ratings.RATER_ID]: uid,
      [Ratings.TARGET_ID]: targetId,
      [Ratings.STARS]: stars,
      [Ratings.REVIEW]: review,
      [Ratings.TAGS]: tags,
      [Ratings.CREATED_AT]: Timestamp.now(),
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
