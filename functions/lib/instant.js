"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.expireInstantRequests = exports.onInstantRequestCreated = exports.respondInstantRequest = exports.getInstantWorkerContact = exports.deleteInstantRequest = exports.setInstantRequestStatus = exports.setInstantResponseStatus = exports.postInstantRequest = void 0;
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
 *   onInstantRequestCreated   notifies available workers nearby (worker_cards, geohash cells — no scans)
 *   expireInstantRequests     open needs past expiresAt → expired
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto_1 = require("crypto");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const notify_1 = require("./lib/notify");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const R = schema_1.InstantRequests.Responses;
const ST = schema_1.Values.InstantStatus;
const RS = schema_1.Values.InstantResponseStatus;
const HOUR_MS = 60 * 60 * 1000;
const FREE_URGENT_POSTS = 3;
const MAX_RADIUS_KM = 10;
const MAX_SCHEDULE_MS = 48 * HOUR_MS;
const DELETE_WINDOW_MS = 30 * 60 * 1000;
const NOTIFY_LIMIT = 100;
const WINDOWS = {
    right_now: 4 * HOUR_MS,
    within_1_hour: 6 * HOUR_MS,
    today: 24 * HOUR_MS,
    tomorrow: 48 * HOUR_MS,
};
function requestRef(id) {
    return db.collection(schema_1.InstantRequests.COLLECTION).doc(id);
}
function chargeRef(id) {
    return db.collection(schema_1.Idempotency.COLLECTION).doc(`postInstant_${id}`);
}
function validRequestId(data) {
    return (0, input_1.str)(data, "requestId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
}
function millis(value) {
    return value instanceof Timestamp ? value.toMillis() : 0;
}
// ─────────────────────────────── employer ────────────────────────────────
exports.postInstantRequest = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const id = (0, crypto_1.createHash)("sha256").update(`${uid}:${(0, input_1.requestId)(data)}`).digest("base64url").slice(0, 20);
    const nowMs = Date.now();
    const title = (0, input_1.str)(data, "title", { min: 3, max: 80 });
    const category = (0, input_1.oneOf)(data, "category", schema_1.CATEGORY_KEYS, "OTHER");
    const workersNeeded = (0, input_1.int)(data, "workersNeeded", { min: 1, max: 20 });
    const payPerPerson = (0, input_1.int)(data, "payPerPerson", { min: 0, max: 100000 });
    const durationText = (0, input_1.str)(data, "durationText", { max: 40, optional: true });
    const addressText = (0, input_1.str)(data, "addressText", { min: 3, max: 200 });
    const area = (0, input_1.str)(data, "area", { min: 2, max: 60 });
    const contactNumber = (0, input_1.mobile)(data, "contactNumber");
    const radiusKm = Math.min(MAX_RADIUS_KM, Math.max(2, Number(data.radiusKm) || 5));
    const { lat, lng } = (0, input_1.latLng)(data);
    const windowKey = (0, input_1.oneOf)(data, "window", [...Object.keys(WINDOWS), "custom"], "right_now");
    let scheduledAtMs = 0;
    if (windowKey === "custom") {
        scheduledAtMs = Number(data.scheduledAt) || 0;
        if (scheduledAtMs <= nowMs || scheduledAtMs > nowMs + MAX_SCHEDULE_MS) {
            (0, input_1.fail)("invalid-argument", "Pick a time within the next 48 hours");
        }
    }
    const expiresAtMs = windowKey === "custom" ? scheduledAtMs : nowMs + WINDOWS[windowKey];
    const charge = await db.runTransaction(async (tx) => {
        const ref = requestRef(id);
        const employerRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid);
        const [existing, employer] = await Promise.all([tx.get(ref), tx.get(employerRef)]);
        if (existing.exists)
            return "duplicate";
        if (!employer.exists)
            (0, input_1.fail)("failed-precondition", "Complete your employer profile first");
        if (employer.get(schema_1.EmployerProfiles.BLOCKED) === true)
            (0, input_1.fail)("permission-denied", "Your account is blocked");
        const S = schema_1.EmployerProfiles.Subscription;
        const sub = (0, input_1.obj)(employer.get(schema_1.EmployerProfiles.SUBSCRIPTION));
        const subExpires = millis(sub[S.EXPIRES_AT]);
        const active = sub[S.STATUS] === "ACTIVE" && (subExpires === 0 || subExpires > nowMs);
        const credits = (0, input_1.obj)(sub[S.CREDITS]);
        const freeUsed = Number(employer.get(schema_1.EmployerProfiles.FREE_URGENT_POSTS_USED) || 0);
        let paid;
        if (active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN") {
            paid = "campaign";
        }
        else if (freeUsed < FREE_URGENT_POSTS) {
            paid = "free";
            tx.update(employerRef, schema_1.EmployerProfiles.FREE_URGENT_POSTS_USED, FieldValue.increment(1));
        }
        else if (active && Number(credits[S.CREDITS_INSTANT] || 0) > 0) {
            paid = "instant";
            tx.update(employerRef, `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_INSTANT}`, FieldValue.increment(-1));
        }
        else if (active && Number(credits[S.CREDITS_NORMAL] || 0) > 0) {
            paid = "normal";
            tx.update(employerRef, `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(-1));
        }
        else {
            (0, input_1.fail)("resource-exhausted", "You have used your 3 free urgent posts. Buy a plan to post more.");
        }
        tx.create(ref, Object.assign(Object.assign({ [schema_1.InstantRequests.EMPLOYER_ID]: uid, [schema_1.InstantRequests.TITLE]: title, [schema_1.InstantRequests.CATEGORY]: category, [schema_1.InstantRequests.WORKERS_NEEDED]: workersNeeded, [schema_1.InstantRequests.PAY_PER_PERSON]: payPerPerson, [schema_1.InstantRequests.DURATION_TEXT]: durationText }, (scheduledAtMs ? { [schema_1.InstantRequests.SCHEDULED_AT]: Timestamp.fromMillis(scheduledAtMs) } : {})), { [schema_1.InstantRequests.AREA]: area, [schema_1.InstantRequests.ADDRESS_TEXT]: addressText, [schema_1.InstantRequests.LAT]: lat, [schema_1.InstantRequests.LNG]: lng, [schema_1.InstantRequests.GEOHASH]: (0, geo_1.encodeGeohash)(lat, lng), [schema_1.InstantRequests.RADIUS_KM]: radiusKm, [schema_1.InstantRequests.CONTACT_NUMBER]: contactNumber, [schema_1.InstantRequests.STATUS]: ST.OPEN, [schema_1.InstantRequests.SELECTED_WORKER_IDS]: [], [schema_1.InstantRequests.RESPONSE_COUNT]: 0, [schema_1.InstantRequests.CREATED_AT]: Timestamp.fromMillis(nowMs), [schema_1.InstantRequests.EXPIRES_AT]: Timestamp.fromMillis(expiresAtMs) }));
        tx.set(chargeRef(id), {
            [schema_1.Idempotency.RESULT]: paid,
            [schema_1.Idempotency.EXPIRE_AT]: Timestamp.fromMillis(expiresAtMs + 7 * 24 * HOUR_MS),
        });
        return paid;
    });
    return { id, charge };
});
async function ownedRequest(tx, id, uid) {
    const snap = await tx.get(requestRef(id));
    if (!snap.exists || snap.get(schema_1.InstantRequests.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your urgent need");
    return snap;
}
exports.setInstantResponseStatus = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const id = validRequestId(data);
    const workerId = (0, input_1.str)(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
    const status = (0, input_1.oneOf)(data, "status", [RS.ACCEPTED, RS.REJECTED, RS.COMPLETED, RS.NO_SHOW]);
    const outcome = await db.runTransaction(async (tx) => {
        const request = await ownedRequest(tx, id, uid);
        const responseRef = request.ref.collection(R.COLLECTION).doc(workerId);
        const response = await tx.get(responseRef);
        if (!response.exists)
            (0, input_1.fail)("not-found", "This worker has not responded");
        const requestStatus = String(request.get(schema_1.InstantRequests.STATUS));
        if (requestStatus === ST.CANCELLED || requestStatus === ST.EXPIRED)
            (0, input_1.fail)("failed-precondition", "This urgent need is closed");
        const needed = Number(request.get(schema_1.InstantRequests.WORKERS_NEEDED) || 1);
        const selected = new Set((request.get(schema_1.InstantRequests.SELECTED_WORKER_IDS) || []));
        if (status === RS.ACCEPTED && !selected.has(workerId) && selected.size >= needed) {
            (0, input_1.fail)("failed-precondition", "You already selected the workers you need");
        }
        let completedCount = 0;
        if (status === RS.COMPLETED) {
            const done = await tx.get(request.ref.collection(R.COLLECTION).where(R.STATUS, "==", RS.COMPLETED));
            completedCount = done.docs.filter((d) => d.id !== workerId).length + 1;
        }
        if (status === RS.ACCEPTED || status === RS.COMPLETED)
            selected.add(workerId);
        else
            selected.delete(workerId);
        const nextStatus = status === RS.COMPLETED && completedCount >= needed ? ST.COMPLETED :
            selected.size >= needed ? ST.FILLED : ST.OPEN;
        tx.update(responseRef, { [R.STATUS]: status });
        tx.update(request.ref, {
            [schema_1.InstantRequests.SELECTED_WORKER_IDS]: Array.from(selected),
            [schema_1.InstantRequests.STATUS]: requestStatus === ST.COMPLETED && status !== RS.COMPLETED ? ST.COMPLETED : nextStatus,
        });
        return { title: String(request.get(schema_1.InstantRequests.TITLE) || "") };
    });
    if (status === RS.ACCEPTED) {
        await (0, notify_1.notify)(workerId, {
            type: "WORKER_HIRED",
            templateId: "INSTANT_SELECTED",
            params: { title: outcome.title },
            data: { requestId: id, action: "view_urgent_work" },
            role: schema_1.Values.Role.WORKER,
        });
    }
    return { ok: true };
});
exports.setInstantRequestStatus = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const id = validRequestId(data);
    const status = (0, input_1.oneOf)(data, "status", [ST.FILLED, ST.CANCELLED]);
    await db.runTransaction(async (tx) => {
        const request = await ownedRequest(tx, id, uid);
        const current = String(request.get(schema_1.InstantRequests.STATUS));
        if (current === ST.CANCELLED || current === ST.EXPIRED || current === ST.COMPLETED) {
            (0, input_1.fail)("failed-precondition", "This urgent need is already closed");
        }
        const charge = status === ST.CANCELLED ? await tx.get(chargeRef(id)) : null;
        tx.update(request.ref, { [schema_1.InstantRequests.STATUS]: status });
        // Nobody was selected yet: give the post back.
        const nobodySelected = (request.get(schema_1.InstantRequests.SELECTED_WORKER_IDS) || []).length === 0;
        if (status === ST.CANCELLED && nobodySelected && (charge === null || charge === void 0 ? void 0 : charge.exists)) {
            const S = schema_1.EmployerProfiles.Subscription;
            const employerRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid);
            const paid = String(charge.get(schema_1.Idempotency.RESULT));
            if (paid === "free")
                tx.update(employerRef, schema_1.EmployerProfiles.FREE_URGENT_POSTS_USED, FieldValue.increment(-1));
            if (paid === "instant")
                tx.update(employerRef, `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_INSTANT}`, FieldValue.increment(1));
            if (paid === "normal")
                tx.update(employerRef, `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(1));
            tx.delete(charge.ref);
        }
    });
    return { ok: true };
});
exports.deleteInstantRequest = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const id = validRequestId((0, input_1.obj)(raw));
    const snap = await requestRef(id).get();
    if (!snap.exists)
        return { ok: true };
    if (snap.get(schema_1.InstantRequests.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your urgent need");
    const status = String(snap.get(schema_1.InstantRequests.STATUS));
    const young = Date.now() - millis(snap.get(schema_1.InstantRequests.CREATED_AT)) < DELETE_WINDOW_MS;
    if ((status === ST.OPEN || status === ST.FILLED) && !young) {
        (0, input_1.fail)("failed-precondition", "Cancel the urgent need first");
    }
    await db.recursiveDelete(snap.ref);
    return { ok: true };
});
exports.getInstantWorkerContact = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const id = validRequestId(data);
    const workerId = (0, input_1.str)(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
    const ref = requestRef(id);
    const [request, response, worker] = await Promise.all([
        ref.get(),
        ref.collection(R.COLLECTION).doc(workerId).get(),
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(workerId).get(),
    ]);
    if (!request.exists || request.get(schema_1.InstantRequests.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your urgent need");
    if (!response.exists)
        (0, input_1.fail)("permission-denied", "This worker has not responded");
    const phone = String(worker.get(schema_1.WorkerProfiles.PHONE) || "");
    if (!phone)
        (0, input_1.fail)("not-found", "This worker has no phone number");
    return { phone };
});
// ─────────────────────────────── worker ────────────────────────────────
exports.respondInstantRequest = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const id = validRequestId(data);
    const action = (0, input_1.oneOf)(data, "action", [RS.APPLIED, RS.CALLED], RS.APPLIED);
    const created = await db.runTransaction(async (tx) => {
        const ref = requestRef(id);
        const responseRef = ref.collection(R.COLLECTION).doc(uid);
        const workerRef = db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid);
        const [request, response, worker] = await Promise.all([tx.get(ref), tx.get(responseRef), tx.get(workerRef)]);
        if (!request.exists)
            (0, input_1.fail)("not-found", "This urgent need was removed");
        if (!worker.exists)
            (0, input_1.fail)("failed-precondition", "Complete your worker profile first");
        if (worker.get(schema_1.WorkerProfiles.BLOCKED) === true)
            (0, input_1.fail)("permission-denied", "Your account is blocked");
        if (request.get(schema_1.InstantRequests.EMPLOYER_ID) === uid)
            (0, input_1.fail)("failed-precondition", "This is your own urgent need");
        const open = request.get(schema_1.InstantRequests.STATUS) === ST.OPEN &&
            millis(request.get(schema_1.InstantRequests.EXPIRES_AT)) > Date.now();
        if (!open)
            (0, input_1.fail)("failed-precondition", "This urgent need is already filled or closed");
        if (response.exists) {
            if (action === RS.CALLED && response.get(R.STATUS) === RS.APPLIED) {
                tx.update(responseRef, { [R.STATUS]: RS.CALLED });
            }
            return null;
        }
        const workerName = String(worker.get(schema_1.WorkerProfiles.NAME) || "A worker");
        tx.create(responseRef, {
            [R.WORKER_ID]: uid,
            [R.STATUS]: action,
            [R.WORKER_NAME]: workerName,
            [R.CREATED_AT]: Timestamp.now(),
        });
        tx.update(ref, { [schema_1.InstantRequests.RESPONSE_COUNT]: FieldValue.increment(1) });
        return {
            employerId: String(request.get(schema_1.InstantRequests.EMPLOYER_ID)),
            title: String(request.get(schema_1.InstantRequests.TITLE) || ""),
            workerName,
        };
    });
    if (created) {
        await (0, notify_1.notify)(created.employerId, {
            type: "NEW_APPLICATION",
            templateId: "INSTANT_RESPONSE_RECEIVED",
            params: { workerName: created.workerName, title: created.title },
            data: { requestId: id, action: "view_urgent_needs" },
            role: schema_1.Values.Role.EMPLOYER,
        });
    }
    return { ok: true };
});
// ─────────────────────────────── triggers ────────────────────────────────
exports.onInstantRequestCreated = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.InstantRequests.COLLECTION}/{requestId}`)
    .onCreate(async (snap, context) => {
    const request = snap.data();
    const lat = Number(request[schema_1.InstantRequests.LAT]);
    const lng = Number(request[schema_1.InstantRequests.LNG]);
    const radius = Number(request[schema_1.InstantRequests.RADIUS_KM]) || 5;
    const employerId = String(request[schema_1.InstantRequests.EMPLOYER_ID]);
    const category = String(request[schema_1.InstantRequests.CATEGORY]);
    // Precision-5 cells (~4.9 km) covering the radius; one single-field range query each.
    // Every nearby worker is eligible (there is no availability gate); skill matches go first.
    const cells = (0, geo_1.coveringCells)(lat, lng, radius, 5);
    const snaps = await Promise.all(cells.map((cell) => db.collection(schema_1.WorkerCards.COLLECTION)
        .where(schema_1.WorkerCards.GEOHASH, ">=", cell)
        .where(schema_1.WorkerCards.GEOHASH, "<", `${cell}~`)
        .limit(200)
        .get()));
    const candidates = new Map();
    for (const s of snaps) {
        for (const doc of s.docs) {
            if (doc.id === employerId || candidates.has(doc.id))
                continue;
            const d = (0, geo_1.distanceKm)(lat, lng, Number(doc.get(schema_1.WorkerCards.LAT)), Number(doc.get(schema_1.WorkerCards.LNG)));
            if (!(d <= radius))
                continue;
            const skills = (doc.get(schema_1.WorkerCards.SKILLS) || []);
            candidates.set(doc.id, { distance: d, skilled: skills.includes(category) });
        }
    }
    const chosen = Array.from(candidates.entries())
        .sort((a, b) => Number(b[1].skilled) - Number(a[1].skilled) || a[1].distance - b[1].distance)
        .slice(0, NOTIFY_LIMIT);
    const params = {
        title: String(request[schema_1.InstantRequests.TITLE] || ""),
        area: String(request[schema_1.InstantRequests.AREA] || ""),
        pay: Number(request[schema_1.InstantRequests.PAY_PER_PERSON] || 0),
    };
    for (let i = 0; i < chosen.length; i += 20) {
        await Promise.all(chosen.slice(i, i + 20).map(([workerId]) => (0, notify_1.notify)(workerId, {
            type: "NEW_JOB_ALERT",
            templateId: "INSTANT_REQUEST_NEARBY",
            params,
            data: { requestId: context.params.requestId, action: "view_urgent_work" },
            role: schema_1.Values.Role.WORKER,
        })));
    }
    functions.logger.info(`instant ${context.params.requestId}: notified ${chosen.length} of ${candidates.size} nearby workers`);
    return null;
});
exports.expireInstantRequests = functions
    .region("asia-south1")
    .pubsub.schedule("every 30 minutes")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const now = Timestamp.now();
    for (let round = 0; round < 10; round++) {
        const snap = await db.collection(schema_1.InstantRequests.COLLECTION)
            .where(schema_1.InstantRequests.STATUS, "==", ST.OPEN)
            .where(schema_1.InstantRequests.EXPIRES_AT, "<=", now)
            .limit(400)
            .get();
        if (snap.empty)
            break;
        const batch = db.batch();
        snap.docs.forEach((doc) => batch.update(doc.ref, { [schema_1.InstantRequests.STATUS]: ST.EXPIRED }));
        await batch.commit();
        if (snap.size < 400)
            break;
    }
    return null;
});
//# sourceMappingURL=instant.js.map