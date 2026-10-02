/**
 * Instant help — urgent same-day needs. The only writers of instant_requests and their
 * responses/{workerId} subcollection.
 *
 *   postInstantRequest        employer posts a need (3 free, then instant/normal credits; campaign = free)
 *   respondInstantRequest     worker says "I can come" / records a call (one response per worker)
 *   setInstantResponseStatus  employer accepts / rejects / completes / marks no-show
 *   setInstantRequestStatus   employer marks the need filled or cancels it (refund if nobody was selected)
 *   deleteInstantRequest      employer removes a closed need (or an open one within 30 min)
 *   getInstantWorkerContact   employer gets the phone of a worker who responded
 *   (dispatch)                offers in 5/10/15/20 km waves and first-to-accept: see urgent.ts
 *   expireInstantRequests     open needs past expiresAt → expired
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { createHash } from "crypto";
import { referralPostsLeft, refundReferralPost, useReferralPost } from "./lib/referral-posts";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, int, oneOf, latLng, mobile, requestId } from "./lib/input";
import { encodeGeohash } from "./lib/geo";
import { notify } from "./lib/notify";
import { readActiveWorkers, releaseWorkers } from "./urgent";
import {
  CATEGORY_KEYS, EmployerProfiles, Idempotency, InstantRequests, Values, WorkerCards, WorkerProfiles, MAX_PAY_RUPEES } from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;

const R = InstantRequests.Responses;
const ST = Values.InstantStatus;
const RS = Values.InstantResponseStatus;
const HOUR_MS = 60 * 60 * 1000;
const FREE_URGENT_POSTS = 3;
const MAX_RADIUS_KM = 10;
const MAX_SCHEDULE_MS = 48 * HOUR_MS;
const DELETE_WINDOW_MS = 30 * 60 * 1000;
const WINDOWS: Record<string, number> = {
  right_now: 4 * HOUR_MS,
  within_1_hour: 6 * HOUR_MS,
  today: 24 * HOUR_MS,
  tomorrow: 48 * HOUR_MS,
};

type Charge = "campaign" | "free" | "instant" | "normal" | "referral";

function requestRef(id: string) {
  return db.collection(InstantRequests.COLLECTION).doc(id);
}

function chargeRef(id: string) {
  return db.collection(Idempotency.COLLECTION).doc(`postInstant_${id}`);
}

function validRequestId(data: Record<string, unknown>): string {
  return str(data, "requestId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
}

function millis(value: unknown): number {
  return value instanceof Timestamp ? value.toMillis() : 0;
}

// ─────────────────────────────── employer ────────────────────────────────

export const postInstantRequest = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const id = createHash("sha256").update(`${uid}:${requestId(data)}`).digest("base64url").slice(0, 20);
  const nowMs = Date.now();

  const title = str(data, "title", { min: 3, max: 80 });
  const category = oneOf(data, "category", CATEGORY_KEYS, "OTHER");
  const workersNeeded = int(data, "workersNeeded", { min: 1, max: 20 });
  const payPerPerson = int(data, "payPerPerson", { min: 0, max: 10_000_000 });
  if (payPerPerson > MAX_PAY_RUPEES) fail("invalid-argument", "Pay can be at most ₹50,000");
  const durationText = str(data, "durationText", { max: 40, optional: true });
  const addressText = str(data, "addressText", { min: 3, max: 200 });
  const area = str(data, "area", { min: 2, max: 60 });
  const contactNumber = mobile(data, "contactNumber");
  const radiusKm = Math.min(MAX_RADIUS_KM, Math.max(2, Number(data.radiusKm) || 5));
  const { lat, lng } = latLng(data);
  const windowKey = oneOf(data, "window", [...Object.keys(WINDOWS), "custom"] as const, "right_now");
  let scheduledAtMs = 0;
  if (windowKey === "custom") {
    scheduledAtMs = Number(data.scheduledAt) || 0;
    if (scheduledAtMs <= nowMs || scheduledAtMs > nowMs + MAX_SCHEDULE_MS) {
      fail("invalid-argument", "Pick a time within the next 48 hours");
    }
  }
  const expiresAtMs = windowKey === "custom" ? scheduledAtMs : nowMs + WINDOWS[windowKey];

  const charge = await db.runTransaction(async (tx) => {
    const ref = requestRef(id);
    const employerRef = db.collection(EmployerProfiles.COLLECTION).doc(uid);
    const [existing, employer] = await Promise.all([tx.get(ref), tx.get(employerRef)]);
    if (existing.exists) return "duplicate";
    if (!employer.exists) fail("failed-precondition", "Complete your employer profile first");
    if (employer.get(EmployerProfiles.BLOCKED) === true) fail("permission-denied", "Your account is blocked");

    const S = EmployerProfiles.Subscription;
    const sub = obj(employer.get(EmployerProfiles.SUBSCRIPTION));
    const subExpires = millis(sub[S.EXPIRES_AT]);
    const active = sub[S.STATUS] === "ACTIVE" && (subExpires === 0 || subExpires > nowMs);
    const credits = obj(sub[S.CREDITS]);
    const freeUsed = Number(employer.get(EmployerProfiles.FREE_URGENT_POSTS_USED) || 0);
    let paid: Charge;
    if (active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN") {
      paid = "campaign";
    } else if (freeUsed < FREE_URGENT_POSTS) {
      paid = "free";
      tx.update(employerRef, EmployerProfiles.FREE_URGENT_POSTS_USED, FieldValue.increment(1));
    } else if (referralPostsLeft(employer.data(), nowMs) > 0) {
      paid = "referral";
      useReferralPost(tx, employerRef);
    } else if (active && Number(credits[S.CREDITS_INSTANT] || 0) > 0) {
      paid = "instant";
      tx.update(employerRef, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_INSTANT}`, FieldValue.increment(-1));
    } else if (active && Number(credits[S.CREDITS_NORMAL] || 0) > 0) {
      paid = "normal";
      tx.update(employerRef, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(-1));
    } else {
      fail("resource-exhausted", "You have used your 3 free urgent posts. Buy a plan to post more.");
    }

    tx.create(ref, {
      [InstantRequests.EMPLOYER_ID]: uid,
      [InstantRequests.TITLE]: title,
      [InstantRequests.CATEGORY]: category,
      [InstantRequests.WORKERS_NEEDED]: workersNeeded,
      [InstantRequests.PAY_PER_PERSON]: payPerPerson,
      [InstantRequests.DURATION_TEXT]: durationText,
      ...(scheduledAtMs ? { [InstantRequests.SCHEDULED_AT]: Timestamp.fromMillis(scheduledAtMs) } : {}),
      [InstantRequests.AREA]: area,
      [InstantRequests.ADDRESS_TEXT]: addressText,
      [InstantRequests.LAT]: lat,
      [InstantRequests.LNG]: lng,
      [InstantRequests.GEOHASH]: encodeGeohash(lat, lng),
      [InstantRequests.CELL]: encodeGeohash(lat, lng, 5),
      [InstantRequests.RADIUS_KM]: radiusKm,
      [InstantRequests.CONTACT_NUMBER]: contactNumber,
      [InstantRequests.STATUS]: ST.OPEN,
      [InstantRequests.SELECTED_WORKER_IDS]: [],
      [InstantRequests.RESPONSE_COUNT]: 0,
      [InstantRequests.CREATED_AT]: Timestamp.fromMillis(nowMs),
      [InstantRequests.EXPIRES_AT]: Timestamp.fromMillis(expiresAtMs),
    });
    tx.set(chargeRef(id), {
      [Idempotency.RESULT]: paid,
      [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(expiresAtMs + 7 * 24 * HOUR_MS),
    });
    return paid;
  });
  return { id, charge };
});

async function ownedRequest(tx: admin.firestore.Transaction, id: string, uid: string) {
  const snap = await tx.get(requestRef(id));
  if (!snap.exists || snap.get(InstantRequests.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your urgent need");
  return snap;
}

export const setInstantResponseStatus = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const id = validRequestId(data);
  const workerId = str(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
  const status = oneOf(data, "status", [RS.ACCEPTED, RS.REJECTED, RS.COMPLETED, RS.NO_SHOW] as const);

  const outcome = await db.runTransaction(async (tx) => {
    const request = await ownedRequest(tx, id, uid);
    const responseRef = request.ref.collection(R.COLLECTION).doc(workerId);
    const response = await tx.get(responseRef);
    if (!response.exists) fail("not-found", "This worker has not responded");
    const requestStatus = String(request.get(InstantRequests.STATUS));
    if (requestStatus === ST.CANCELLED || requestStatus === ST.EXPIRED) fail("failed-precondition", "This urgent need is closed");

    const needed = Number(request.get(InstantRequests.WORKERS_NEEDED) || 1);
    const selected = new Set<string>((request.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[]);
    if (status === RS.ACCEPTED && !selected.has(workerId) && selected.size >= needed) {
      fail("failed-precondition", "You already selected the workers you need");
    }
    let completedCount = 0;
    if (status === RS.COMPLETED) {
      const done = await tx.get(request.ref.collection(R.COLLECTION).where(R.STATUS, "==", RS.COMPLETED));
      completedCount = done.docs.filter((d) => d.id !== workerId).length + 1;
    }
    const previous = String(response.get(R.STATUS));
    // Removed, no-show or finished: the worker may take another urgent job.
    const release = status === RS.ACCEPTED ? [] : await readActiveWorkers(tx, id, [workerId]);

    if (status === RS.ACCEPTED || status === RS.COMPLETED) selected.add(workerId);
    else selected.delete(workerId);
    const nextStatus = status === RS.COMPLETED && completedCount >= needed ? ST.COMPLETED :
      selected.size >= needed ? ST.FILLED : ST.OPEN;
    const finalStatus = requestStatus === ST.COMPLETED && status !== RS.COMPLETED ? ST.COMPLETED : nextStatus;
    // A place opened up again: offers restart from 5 km.
    const reopened = finalStatus === ST.OPEN && (status === RS.REJECTED || status === RS.NO_SHOW) &&
      (previous === RS.ACCEPTED || requestStatus === ST.FILLED);

    tx.update(responseRef, { [R.STATUS]: status });
    tx.update(request.ref, {
      [InstantRequests.SELECTED_WORKER_IDS]: Array.from(selected),
      [InstantRequests.STATUS]: finalStatus,
      ...(reopened ? {
        [InstantRequests.DISPATCH_RADIUS_KM]: 0,
        [InstantRequests.NEXT_WAVE_AT]: Timestamp.now(),
      } : {}),
      ...(finalStatus !== ST.OPEN ? { [InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() } : {}),
    });
    releaseWorkers(tx, release);
    if (status === RS.ACCEPTED) {
      tx.update(db.collection(WorkerProfiles.COLLECTION).doc(workerId), { [WorkerProfiles.ACTIVE_URGENT_ID]: id });
    }
    if (status === RS.NO_SHOW && previous !== RS.NO_SHOW) {
      tx.set(db.collection(WorkerCards.COLLECTION).doc(workerId), { [WorkerCards.NO_SHOWS]: FieldValue.increment(1) }, { merge: true });
    }
    if (status === RS.COMPLETED && previous !== RS.COMPLETED) {
      tx.set(db.collection(WorkerCards.COLLECTION).doc(workerId), { [WorkerCards.JOBS_COMPLETED]: FieldValue.increment(1) }, { merge: true });
    }
    return { title: String(request.get(InstantRequests.TITLE) || ""), previous };
  });

  if (status === RS.ACCEPTED && outcome.previous !== RS.ACCEPTED) {
    await notify(workerId, {
      type: "WORKER_HIRED",
      templateId: "INSTANT_SELECTED",
      params: { title: outcome.title },
      data: { requestId: id, action: "view_urgent_work" },
      role: Values.Role.WORKER,
    });
  }
  if ((status === RS.REJECTED || status === RS.NO_SHOW) && outcome.previous === RS.ACCEPTED) {
    await notify(workerId, {
      type: "REJECTED",
      templateId: "URGENT_REMOVED",
      params: { title: outcome.title },
      data: { requestId: id, action: "view_urgent_work" },
      role: Values.Role.WORKER,
    });
  }
  return { ok: true };
});

export const setInstantRequestStatus = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const id = validRequestId(data);
  const status = oneOf(data, "status", [ST.FILLED, ST.CANCELLED] as const);

  const closed = await db.runTransaction(async (tx) => {
    const request = await ownedRequest(tx, id, uid);
    const current = String(request.get(InstantRequests.STATUS));
    if (current === ST.CANCELLED || current === ST.EXPIRED || current === ST.COMPLETED) {
      fail("failed-precondition", "This urgent need is already closed");
    }
    const charge = status === ST.CANCELLED ? await tx.get(chargeRef(id)) : null;
    const selectedIds = (request.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[];
    const release = status === ST.CANCELLED ? await readActiveWorkers(tx, id, selectedIds) : [];
    tx.update(request.ref, { [InstantRequests.STATUS]: status, [InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() });
    releaseWorkers(tx, release);

    // Nobody was selected yet: give the post back.
    const nobodySelected = ((request.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[]).length === 0;
    if (status === ST.CANCELLED && nobodySelected && charge?.exists) {
      const S = EmployerProfiles.Subscription;
      const employerRef = db.collection(EmployerProfiles.COLLECTION).doc(uid);
      const paid = String(charge.get(Idempotency.RESULT));
      if (paid === "free") tx.update(employerRef, EmployerProfiles.FREE_URGENT_POSTS_USED, FieldValue.increment(-1));
      if (paid === "referral") refundReferralPost(tx, employerRef);
      if (paid === "instant") tx.update(employerRef, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_INSTANT}`, FieldValue.increment(1));
      if (paid === "normal") tx.update(employerRef, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(1));
      tx.delete(charge.ref);
    }
    return { title: String(request.get(InstantRequests.TITLE) || ""), workers: status === ST.CANCELLED ? selectedIds : [] };
  });
  // Workers who accepted are told the job is off, so they don't travel for nothing.
  await Promise.all(closed.workers.map((workerId) => notify(workerId, {
    type: "REJECTED",
    templateId: "URGENT_CANCELLED",
    params: { title: closed.title },
    data: { requestId: id, action: "view_urgent_work" },
    role: Values.Role.WORKER,
  })));
  return { ok: true };
});

export const deleteInstantRequest = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const id = validRequestId(obj(raw));
  const snap = await requestRef(id).get();
  if (!snap.exists) return { ok: true };
  if (snap.get(InstantRequests.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your urgent need");
  const status = String(snap.get(InstantRequests.STATUS));
  const young = Date.now() - millis(snap.get(InstantRequests.CREATED_AT)) < DELETE_WINDOW_MS;
  if ((status === ST.OPEN || status === ST.FILLED) && !young) {
    fail("failed-precondition", "Cancel the urgent need first");
  }
  await db.recursiveDelete(snap.ref);
  return { ok: true };
});

export const getInstantWorkerContact = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const id = validRequestId(data);
  const workerId = str(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
  const ref = requestRef(id);
  const [request, response, worker] = await Promise.all([
    ref.get(),
    ref.collection(R.COLLECTION).doc(workerId).get(),
    db.collection(WorkerProfiles.COLLECTION).doc(workerId).get(),
  ]);
  if (!request.exists || request.get(InstantRequests.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your urgent need");
  if (!response.exists) fail("permission-denied", "This worker has not responded");
  const phone = String(worker.get(WorkerProfiles.PHONE) || "");
  if (!phone) fail("not-found", "This worker has no phone number");
  return { phone };
});

// ─────────────────────────────── worker ────────────────────────────────

export const respondInstantRequest = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const id = validRequestId(data);
  const action = oneOf(data, "action", [RS.APPLIED, RS.CALLED] as const, RS.APPLIED);

  const created = await db.runTransaction(async (tx) => {
    const ref = requestRef(id);
    const responseRef = ref.collection(R.COLLECTION).doc(uid);
    const workerRef = db.collection(WorkerProfiles.COLLECTION).doc(uid);
    const [request, response, worker] = await Promise.all([tx.get(ref), tx.get(responseRef), tx.get(workerRef)]);
    if (!request.exists) fail("not-found", "This urgent need was removed");
    if (!worker.exists) fail("failed-precondition", "Complete your worker profile first");
    if (worker.get(WorkerProfiles.BLOCKED) === true) fail("permission-denied", "Your account is blocked");
    if (request.get(InstantRequests.EMPLOYER_ID) === uid) fail("failed-precondition", "This is your own urgent need");
    const open = request.get(InstantRequests.STATUS) === ST.OPEN &&
      millis(request.get(InstantRequests.EXPIRES_AT)) > Date.now();
    if (!open) fail("failed-precondition", "This urgent need is already filled or closed");

    if (response.exists) {
      if (action === RS.CALLED && response.get(R.STATUS) === RS.APPLIED) {
        tx.update(responseRef, { [R.STATUS]: RS.CALLED });
      }
      return null;
    }
    const workerName = String(worker.get(WorkerProfiles.NAME) || "A worker");
    tx.create(responseRef, {
      [R.WORKER_ID]: uid,
      [R.STATUS]: action,
      [R.WORKER_NAME]: workerName,
      [R.CREATED_AT]: Timestamp.now(),
    });
    tx.update(ref, { [InstantRequests.RESPONSE_COUNT]: FieldValue.increment(1) });
    return {
      employerId: String(request.get(InstantRequests.EMPLOYER_ID)),
      title: String(request.get(InstantRequests.TITLE) || ""),
      workerName,
    };
  });

  if (created) {
    await notify(created.employerId, {
      type: "NEW_APPLICATION",
      templateId: "INSTANT_RESPONSE_RECEIVED",
      params: { workerName: created.workerName, title: created.title },
      data: { requestId: id, action: "view_urgent_needs" },
      role: Values.Role.EMPLOYER,
    });
  }
  return { ok: true };
});

// ─────────────────────────────── expiry ────────────────────────────────

/**
 * Needs past their window → expired (open ones and filled ones), and the workers who took them are
 * free to accept other urgent jobs.
 */
export const expireInstantRequests = functions
  .region("asia-south1")
  .pubsub.schedule("every 30 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Timestamp.now();
    let expired = 0;
    for (let round = 0; round < 10; round++) {
      const snap = await db.collection(InstantRequests.COLLECTION)
        .where(InstantRequests.STATUS, "in", [ST.OPEN, ST.FILLED])
        .where(InstantRequests.EXPIRES_AT, "<=", now)
        .limit(200)
        .get();
      if (snap.empty) break;
      for (const doc of snap.docs) {
        await db.runTransaction(async (tx) => {
          const fresh = await tx.get(doc.ref);
          if (![ST.OPEN, ST.FILLED].includes(fresh.get(InstantRequests.STATUS))) return;
          const release = await readActiveWorkers(tx, doc.id, (fresh.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[]);
          tx.update(doc.ref, { [InstantRequests.STATUS]: ST.EXPIRED, [InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() });
          releaseWorkers(tx, release);
        });
      }
      expired += snap.size;
      if (snap.size < 200) break;
    }
    if (expired) functions.logger.info(`expireInstantRequests: ${expired} expired`);
    return null;
  });
