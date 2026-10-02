"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.acceptUrgentOffer = exports.dispatchUrgentWaves = exports.onInstantRequestCreated = exports.sender = exports.WAVE_INTERVAL_MS = exports.WAVES_KM = void 0;
exports.urgentTopic = urgentTopic;
exports.nextWaveKm = nextWaveKm;
exports.waveCells = waveCells;
exports.waveConditions = waveConditions;
exports.sendWave = sendWave;
exports.advanceWave = advanceWave;
exports.readActiveWorkers = readActiveWorkers;
exports.releaseWorkers = releaseWorkers;
/**
 * Urgent job dispatch — like a ride request: offers go out to online workers nearby in widening
 * waves, and the first workers to accept get the job until every vacancy is filled.
 *
 *   onInstantRequestCreated  wave 1: every online worker within 5 km gets a ringing offer
 *   dispatchUrgentWaves      every minute: needs still open after ~2 min go out to 10, then 15,
 *                            then 20 km (each wave reaches only the newly covered areas)
 *   acceptUrgentOffer        worker taps Accept: they get the job if a place is left (transaction),
 *                            and the employer is told "2 of 3 coming"
 *
 * Offers are FCM topic pushes (urgent_{geohash5}_{lang}); a worker's phone is subscribed to its
 * current ~5 km cell only while they are Online. So a wave costs no Firestore reads or writes per
 * worker, however many workers there are. The employer removing a worker (instant.ts) reopens the
 * place and restarts the waves.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const notify_1 = require("./lib/notify");
const notification_i18n_1 = require("./notification-i18n");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const R = schema_1.InstantRequests.Responses;
const ST = schema_1.Values.InstantStatus;
const RS = schema_1.Values.InstantResponseStatus;
/** Offer waves, km from the job. */
exports.WAVES_KM = [5, 10, 15, 20];
/** Time a wave gets before the next, wider one (the minute scheduler adds up to a minute). */
exports.WAVE_INTERVAL_MS = 2 * 60 * 1000;
/** FCM allows at most 5 topics in one condition. */
const TOPICS_PER_SEND = 5;
const CELL_PRECISION = 5;
/** Topic for online workers currently in one ~5 km cell, in one app language. Mirrors the app. */
function urgentTopic(cell, locale) {
    return `urgent_${cell}_${locale}`;
}
/** The wave after one that reached [fromKm] (0 = none yet), or null after the last. */
function nextWaveKm(fromKm) {
    var _a;
    return (_a = exports.WAVES_KM.find((km) => km > fromKm)) !== null && _a !== void 0 ? _a : null;
}
/** Cells the wave to [toKm] must reach: those touching the new circle but not the previous one. */
function waveCells(lat, lng, fromKm, toKm) {
    const reached = new Set(fromKm > 0 ? (0, geo_1.coveringCells)(lat, lng, fromKm, CELL_PRECISION) : []);
    return (0, geo_1.coveringCells)(lat, lng, toKm, CELL_PRECISION).filter((c) => !reached.has(c));
}
/** FCM conditions ("'a' in topics || 'b' in topics …") covering [cells] for one language. */
function waveConditions(cells, locale) {
    const out = [];
    for (let i = 0; i < cells.length; i += TOPICS_PER_SEND) {
        out.push(cells.slice(i, i + TOPICS_PER_SEND).map((c) => `'${urgentTopic(c, locale)}' in topics`).join(" || "));
    }
    return out;
}
/** Test seam: the function that actually sends. */
exports.sender = {
    send: (message) => admin.messaging().send(message),
};
/** Pushes the offer for one wave, in every app language. Returns how many sends succeeded. */
async function sendWave(requestId, request, fromKm, toKm) {
    const lat = Number(request[schema_1.InstantRequests.LAT]);
    const lng = Number(request[schema_1.InstantRequests.LNG]);
    const cells = waveCells(lat, lng, fromKm, toKm);
    const needed = Number(request[schema_1.InstantRequests.WORKERS_NEEDED] || 1);
    const taken = (request[schema_1.InstantRequests.SELECTED_WORKER_IDS] || []).length;
    const params = {
        km: toKm,
        title: String(request[schema_1.InstantRequests.TITLE] || ""),
        area: String(request[schema_1.InstantRequests.AREA] || ""),
        pay: Number(request[schema_1.InstantRequests.PAY_PER_PERSON] || 0),
        needed: Math.max(1, needed - taken),
    };
    const expiresAt = request[schema_1.InstantRequests.EXPIRES_AT] instanceof Timestamp ?
        request[schema_1.InstantRequests.EXPIRES_AT].toMillis() : 0;
    const sends = [];
    for (const locale of notification_i18n_1.SUPPORTED_LOCALES) {
        const data = {
            type: "URGENT_OFFER",
            requestId,
            title: (0, notification_i18n_1.tTitle)("INSTANT_REQUEST_NEARBY", locale, params),
            body: (0, notification_i18n_1.tBody)("INSTANT_REQUEST_NEARBY", locale, params),
            jobTitle: params.title,
            area: params.area,
            pay: String(params.pay),
            needed: String(params.needed),
            lat: String(lat),
            lng: String(lng),
            waveKm: String(toKm),
            expiresAt: String(expiresAt),
            channel: "urgent_offers",
            locale,
        };
        for (const condition of waveConditions(cells, locale)) {
            // Offers are useless once the job is gone: FCM drops undelivered ones after 10 minutes.
            sends.push(exports.sender.send({ condition, data, android: { priority: "high", ttl: 10 * 60 * 1000 } }));
        }
    }
    const results = await Promise.allSettled(sends);
    const failed = results.filter((r) => r.status === "rejected").length;
    return { cells: cells.length, sent: results.length - failed, failed };
}
/**
 * Sends the next wave for one open need, if it is due. The wave is claimed in a transaction first,
 * so two scheduler runs (or a retry) never send the same wave twice.
 */
async function advanceWave(requestId, nowMs) {
    const ref = db.collection(schema_1.InstantRequests.COLLECTION).doc(requestId);
    const claimed = await db.runTransaction(async (tx) => {
        var _a, _b, _c;
        const snap = await tx.get(ref);
        const r = snap.data();
        if (!r || r[schema_1.InstantRequests.STATUS] !== ST.OPEN)
            return null;
        const due = (_a = r[schema_1.InstantRequests.NEXT_WAVE_AT]) === null || _a === void 0 ? void 0 : _a.toMillis();
        if (due === undefined || due > nowMs)
            return null;
        const expires = (_c = (_b = r[schema_1.InstantRequests.EXPIRES_AT]) === null || _b === void 0 ? void 0 : _b.toMillis()) !== null && _c !== void 0 ? _c : 0;
        const fromKm = Number(r[schema_1.InstantRequests.DISPATCH_RADIUS_KM] || 0);
        const toKm = nextWaveKm(fromKm);
        if (toKm === null || (expires > 0 && expires <= nowMs)) {
            tx.update(ref, { [schema_1.InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() });
            return null;
        }
        tx.update(ref, {
            [schema_1.InstantRequests.DISPATCH_RADIUS_KM]: toKm,
            [schema_1.InstantRequests.NEXT_WAVE_AT]: nextWaveKm(toKm) === null ?
                FieldValue.delete() : Timestamp.fromMillis(nowMs + exports.WAVE_INTERVAL_MS),
        });
        return { r, fromKm, toKm };
    });
    if (!claimed)
        return null;
    const result = await sendWave(requestId, claimed.r, claimed.fromKm, claimed.toKm);
    functions.logger.info(`urgent ${requestId}: wave to ${claimed.toKm} km, ${result.cells} areas, ` +
        `${result.sent} sent, ${result.failed} failed`);
    return claimed.toKm;
}
/** Wave 1 goes out as soon as the need is posted. */
exports.onInstantRequestCreated = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.InstantRequests.COLLECTION}/{requestId}`)
    .onCreate(async (snap, context) => {
    await snap.ref.update({
        [schema_1.InstantRequests.DISPATCH_RADIUS_KM]: 0,
        [schema_1.InstantRequests.NEXT_WAVE_AT]: Timestamp.fromMillis(0),
    });
    await advanceWave(context.params.requestId, Date.now());
    return null;
});
/** Every minute: the next, wider wave for each open need whose current wave has had its time. */
exports.dispatchUrgentWaves = functions
    .region("asia-south1")
    .pubsub.schedule("every 1 minutes")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const now = Date.now();
    const due = await db.collection(schema_1.InstantRequests.COLLECTION)
        .where(schema_1.InstantRequests.STATUS, "==", ST.OPEN)
        .where(schema_1.InstantRequests.NEXT_WAVE_AT, "<=", Timestamp.fromMillis(now))
        .limit(200)
        .get();
    await Promise.all(due.docs.map((d) => advanceWave(d.id, now).catch((e) => functions.logger.warn(`urgent ${d.id}: wave failed`, e))));
    return null;
});
/**
 * The worker tapped Accept. One transaction decides: a place is left → it is theirs (their
 * response is "accepted", they are in selectedWorkerIds, the need is "filled" when the last place
 * goes). A worker can hold one unfinished urgent job at a time. Accepting twice is harmless.
 */
exports.acceptUrgentOffer = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const id = (0, input_1.str)((0, input_1.obj)(raw), "requestId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
    const ref = db.collection(schema_1.InstantRequests.COLLECTION).doc(id);
    const workerRef = db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid);
    const outcome = await db.runTransaction(async (tx) => {
        var _a, _b;
        const responseRef = ref.collection(R.COLLECTION).doc(uid);
        const [request, response, worker] = await Promise.all([tx.get(ref), tx.get(responseRef), tx.get(workerRef)]);
        if (!request.exists)
            return { result: "closed" };
        if (!worker.exists)
            (0, input_1.fail)("failed-precondition", "Complete your worker profile first");
        if (worker.get(schema_1.WorkerProfiles.BLOCKED) === true)
            (0, input_1.fail)("permission-denied", "Your account is blocked");
        if (request.get(schema_1.InstantRequests.EMPLOYER_ID) === uid)
            (0, input_1.fail)("failed-precondition", "This is your own urgent job");
        const r = request.data() || {};
        const contact = {
            result: "accepted",
            contactNumber: String(r[schema_1.InstantRequests.CONTACT_NUMBER] || ""),
            addressText: String(r[schema_1.InstantRequests.ADDRESS_TEXT] || ""),
            lat: Number(r[schema_1.InstantRequests.LAT]),
            lng: Number(r[schema_1.InstantRequests.LNG]),
            title: String(r[schema_1.InstantRequests.TITLE] || ""),
        };
        const previous = response.get(R.STATUS);
        if (previous === RS.ACCEPTED || previous === RS.COMPLETED)
            return Object.assign(Object.assign({}, contact), { fresh: false });
        if (previous === RS.REJECTED || previous === RS.NO_SHOW)
            return { result: "removed" };
        const expires = (_b = (_a = r[schema_1.InstantRequests.EXPIRES_AT]) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        if (expires > 0 && expires <= Date.now())
            return { result: "closed" };
        if (r[schema_1.InstantRequests.STATUS] === ST.FILLED)
            return { result: "filled" };
        if (r[schema_1.InstantRequests.STATUS] !== ST.OPEN)
            return { result: "closed" };
        // One unfinished urgent job at a time; a stale pointer (that job ended) does not block.
        const activeId = String(worker.get(schema_1.WorkerProfiles.ACTIVE_URGENT_ID) || "");
        if (activeId && activeId !== id) {
            const active = await tx.get(db.collection(schema_1.InstantRequests.COLLECTION).doc(activeId));
            const stillOn = active.exists &&
                [ST.OPEN, ST.FILLED].includes(active.get(schema_1.InstantRequests.STATUS)) &&
                (active.get(schema_1.InstantRequests.SELECTED_WORKER_IDS) || []).includes(uid);
            if (stillOn)
                return { result: "busy" };
        }
        const needed = Number(r[schema_1.InstantRequests.WORKERS_NEEDED] || 1);
        const selected = new Set((r[schema_1.InstantRequests.SELECTED_WORKER_IDS] || []));
        if (selected.size >= needed)
            return { result: "filled" };
        selected.add(uid);
        const full = selected.size >= needed;
        const workerName = String(worker.get(schema_1.WorkerProfiles.NAME) || "Worker");
        tx.set(responseRef, {
            [R.WORKER_ID]: uid,
            [R.STATUS]: RS.ACCEPTED,
            [R.WORKER_NAME]: workerName,
            [R.CREATED_AT]: response.exists ? response.get(R.CREATED_AT) : Timestamp.now(),
        });
        tx.update(ref, Object.assign(Object.assign({ [schema_1.InstantRequests.SELECTED_WORKER_IDS]: Array.from(selected) }, (response.exists ? {} : { [schema_1.InstantRequests.RESPONSE_COUNT]: FieldValue.increment(1) })), (full ? { [schema_1.InstantRequests.STATUS]: ST.FILLED, [schema_1.InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() } : {})));
        tx.update(workerRef, { [schema_1.WorkerProfiles.ACTIVE_URGENT_ID]: id });
        return Object.assign(Object.assign({}, contact), { fresh: true, workerName, count: selected.size, needed, employerId: String(r[schema_1.InstantRequests.EMPLOYER_ID]) });
    });
    if (outcome.result === "accepted" && "fresh" in outcome && outcome.fresh && "employerId" in outcome) {
        await (0, notify_1.notify)(outcome.employerId, {
            type: "NEW_APPLICATION",
            templateId: "URGENT_ACCEPTED",
            params: { workerName: outcome.workerName, title: outcome.title, count: outcome.count, needed: outcome.needed },
            data: { requestId: id, action: "view_urgent_needs" },
            role: schema_1.Values.Role.EMPLOYER,
        });
    }
    if (outcome.result !== "accepted")
        return { result: outcome.result };
    return {
        result: "accepted",
        contactNumber: outcome.contactNumber,
        addressText: outcome.addressText,
        lat: outcome.lat,
        lng: outcome.lng,
        title: outcome.title,
    };
});
/**
 * Frees workers from an urgent job that ended or that they were taken off (activeUrgentId points at
 * it). Used inside the employer's and the expiry transactions; the reads must come first.
 */
async function readActiveWorkers(tx, requestId, workerIds) {
    if (!workerIds.length)
        return [];
    const refs = workerIds.map((w) => db.collection(schema_1.WorkerProfiles.COLLECTION).doc(w));
    const snaps = await tx.getAll(...refs);
    return snaps.filter((s) => s.exists && s.get(schema_1.WorkerProfiles.ACTIVE_URGENT_ID) === requestId).map((s) => s.ref);
}
function releaseWorkers(tx, refs) {
    refs.forEach((ref) => tx.update(ref, { [schema_1.WorkerProfiles.ACTIVE_URGENT_ID]: FieldValue.delete() }));
}
//# sourceMappingURL=urgent.js.map