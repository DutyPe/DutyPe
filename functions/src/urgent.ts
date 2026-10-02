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
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import { coveringCells } from "./lib/geo";
import { notify } from "./lib/notify";
import { SUPPORTED_LOCALES, tBody, tTitle } from "./notification-i18n";
import { InstantRequests, Values, WorkerProfiles } from "./schema";
import { dispatchServiceWaves } from "./services";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;

const R = InstantRequests.Responses;
const ST = Values.InstantStatus;
const RS = Values.InstantResponseStatus;

/** Offer waves, km from the job. */
export const WAVES_KM = [5, 10, 15, 20] as const;
/** Time a wave gets before the next, wider one (the minute scheduler adds up to a minute). */
export const WAVE_INTERVAL_MS = 2 * 60 * 1000;
/** FCM allows at most 5 topics in one condition. */
const TOPICS_PER_SEND = 5;
const CELL_PRECISION = 5;

/** Topic for online workers currently in one ~5 km cell, in one app language. Mirrors the app. */
export function urgentTopic(cell: string, locale: string): string {
  return `urgent_${cell}_${locale}`;
}

/** The wave after one that reached [fromKm] (0 = none yet), or null after the last. */
export function nextWaveKm(fromKm: number): number | null {
  return WAVES_KM.find((km) => km > fromKm) ?? null;
}

/** Cells the wave to [toKm] must reach: those touching the new circle but not the previous one. */
export function waveCells(lat: number, lng: number, fromKm: number, toKm: number): string[] {
  const reached = new Set(fromKm > 0 ? coveringCells(lat, lng, fromKm, CELL_PRECISION) : []);
  return coveringCells(lat, lng, toKm, CELL_PRECISION).filter((c) => !reached.has(c));
}

/** FCM conditions ("'a' in topics || 'b' in topics …") covering [cells] for one language. */
export function waveConditions(cells: string[], locale: string): string[] {
  const out: string[] = [];
  for (let i = 0; i < cells.length; i += TOPICS_PER_SEND) {
    out.push(cells.slice(i, i + TOPICS_PER_SEND).map((c) => `'${urgentTopic(c, locale)}' in topics`).join(" || "));
  }
  return out;
}

/** Test seam: the function that actually sends. */
export const sender = {
  send: (message: admin.messaging.Message): Promise<string> => admin.messaging().send(message),
};

/** Pushes the offer for one wave, in every app language. Returns how many sends succeeded. */
export async function sendWave(
  requestId: string, request: admin.firestore.DocumentData, fromKm: number, toKm: number,
): Promise<{ cells: number; sent: number; failed: number }> {
  const lat = Number(request[InstantRequests.LAT]);
  const lng = Number(request[InstantRequests.LNG]);
  const cells = waveCells(lat, lng, fromKm, toKm);
  const needed = Number(request[InstantRequests.WORKERS_NEEDED] || 1);
  const taken = ((request[InstantRequests.SELECTED_WORKER_IDS] || []) as string[]).length;
  const params = {
    km: toKm,
    title: String(request[InstantRequests.TITLE] || ""),
    area: String(request[InstantRequests.AREA] || ""),
    pay: Number(request[InstantRequests.PAY_PER_PERSON] || 0),
    needed: Math.max(1, needed - taken),
  };
  const expiresAt = request[InstantRequests.EXPIRES_AT] instanceof Timestamp ?
    (request[InstantRequests.EXPIRES_AT] as admin.firestore.Timestamp).toMillis() : 0;
  const sends: Array<Promise<string>> = [];
  for (const locale of SUPPORTED_LOCALES) {
    const data: Record<string, string> = {
      type: "URGENT_OFFER",
      requestId,
      title: tTitle("INSTANT_REQUEST_NEARBY", locale, params),
      body: tBody("INSTANT_REQUEST_NEARBY", locale, params),
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
      sends.push(sender.send({ condition, data, android: { priority: "high", ttl: 10 * 60 * 1000 } }));
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
export async function advanceWave(requestId: string, nowMs: number): Promise<number | null> {
  const ref = db.collection(InstantRequests.COLLECTION).doc(requestId);
  const claimed = await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const r = snap.data();
    if (!r || r[InstantRequests.STATUS] !== ST.OPEN) return null;
    const due = (r[InstantRequests.NEXT_WAVE_AT] as admin.firestore.Timestamp | undefined)?.toMillis();
    if (due === undefined || due > nowMs) return null;
    const expires = (r[InstantRequests.EXPIRES_AT] as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    const fromKm = Number(r[InstantRequests.DISPATCH_RADIUS_KM] || 0);
    const toKm = nextWaveKm(fromKm);
    if (toKm === null || (expires > 0 && expires <= nowMs)) {
      tx.update(ref, { [InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() });
      return null;
    }
    tx.update(ref, {
      [InstantRequests.DISPATCH_RADIUS_KM]: toKm,
      [InstantRequests.NEXT_WAVE_AT]: nextWaveKm(toKm) === null ?
        FieldValue.delete() : Timestamp.fromMillis(nowMs + WAVE_INTERVAL_MS),
    });
    return { r, fromKm, toKm };
  });
  if (!claimed) return null;
  const result = await sendWave(requestId, claimed.r, claimed.fromKm, claimed.toKm);
  functions.logger.info(`urgent ${requestId}: wave to ${claimed.toKm} km, ${result.cells} areas, ` +
    `${result.sent} sent, ${result.failed} failed`);
  return claimed.toKm;
}

/** Wave 1 goes out as soon as the need is posted. */
export const onInstantRequestCreated = functions
  .region("asia-south1")
  .firestore.document(`${InstantRequests.COLLECTION}/{requestId}`)
  .onCreate(async (snap, context) => {
    await snap.ref.update({
      [InstantRequests.DISPATCH_RADIUS_KM]: 0,
      [InstantRequests.NEXT_WAVE_AT]: Timestamp.fromMillis(0),
    });
    await advanceWave(context.params.requestId, Date.now());
    return null;
  });

/** Every minute: the next, wider wave for each open need whose current wave has had its time. */
export const dispatchUrgentWaves = functions
  .region("asia-south1")
  .pubsub.schedule("every 1 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Date.now();
    const due = await db.collection(InstantRequests.COLLECTION)
      .where(InstantRequests.STATUS, "==", ST.OPEN)
      .where(InstantRequests.NEXT_WAVE_AT, "<=", Timestamp.fromMillis(now))
      .limit(200)
      .get();
    await Promise.all(due.docs.map((d) => advanceWave(d.id, now).catch((e) =>
      functions.logger.warn(`urgent ${d.id}: wave failed`, e))));
    // DutyPe Services bookings share this minute job (no extra Cloud Scheduler job to pay for).
    await dispatchServiceWaves(now).catch((e) => functions.logger.warn("service waves failed", e));
    return null;
  });

// ─────────────────────────────── accepting ───────────────────────────────

type AcceptResult =
  | { result: "accepted"; contactNumber: string; addressText: string; lat: number; lng: number; title: string }
  | { result: "filled" | "closed" | "removed" | "busy" };

/**
 * The worker tapped Accept. One transaction decides: a place is left → it is theirs (their
 * response is "accepted", they are in selectedWorkerIds, the need is "filled" when the last place
 * goes). A worker can hold one unfinished urgent job at a time. Accepting twice is harmless.
 */
export const acceptUrgentOffer = onCallSecured({}, async (raw: unknown, context): Promise<AcceptResult> => {
  const uid = context.auth!.uid;
  const id = str(obj(raw), "requestId", { max: 40, pattern: /^[A-Za-z0-9_-]+$/ });
  const ref = db.collection(InstantRequests.COLLECTION).doc(id);
  const workerRef = db.collection(WorkerProfiles.COLLECTION).doc(uid);

  const outcome = await db.runTransaction(async (tx) => {
    const responseRef = ref.collection(R.COLLECTION).doc(uid);
    const [request, response, worker] = await Promise.all([tx.get(ref), tx.get(responseRef), tx.get(workerRef)]);
    if (!request.exists) return { result: "closed" as const };
    if (!worker.exists) fail("failed-precondition", "Complete your worker profile first");
    if (worker.get(WorkerProfiles.BLOCKED) === true) fail("permission-denied", "Your account is blocked");
    if (request.get(InstantRequests.EMPLOYER_ID) === uid) fail("failed-precondition", "This is your own urgent job");

    const r = request.data() || {};
    const contact = {
      result: "accepted" as const,
      contactNumber: String(r[InstantRequests.CONTACT_NUMBER] || ""),
      addressText: String(r[InstantRequests.ADDRESS_TEXT] || ""),
      lat: Number(r[InstantRequests.LAT]),
      lng: Number(r[InstantRequests.LNG]),
      title: String(r[InstantRequests.TITLE] || ""),
    };
    const previous = response.get(R.STATUS);
    if (previous === RS.ACCEPTED || previous === RS.COMPLETED) return { ...contact, fresh: false };
    if (previous === RS.REJECTED || previous === RS.NO_SHOW) return { result: "removed" as const };

    const expires = (r[InstantRequests.EXPIRES_AT] as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    if (expires > 0 && expires <= Date.now()) return { result: "closed" as const };
    if (r[InstantRequests.STATUS] === ST.FILLED) return { result: "filled" as const };
    if (r[InstantRequests.STATUS] !== ST.OPEN) return { result: "closed" as const };

    // One unfinished urgent job at a time; a stale pointer (that job ended) does not block.
    const activeId = String(worker.get(WorkerProfiles.ACTIVE_URGENT_ID) || "");
    if (activeId && activeId !== id) {
      const active = await tx.get(db.collection(InstantRequests.COLLECTION).doc(activeId));
      const stillOn = active.exists &&
        [ST.OPEN, ST.FILLED].includes(active.get(InstantRequests.STATUS)) &&
        ((active.get(InstantRequests.SELECTED_WORKER_IDS) || []) as string[]).includes(uid);
      if (stillOn) return { result: "busy" as const };
    }

    const needed = Number(r[InstantRequests.WORKERS_NEEDED] || 1);
    const selected = new Set<string>((r[InstantRequests.SELECTED_WORKER_IDS] || []) as string[]);
    if (selected.size >= needed) return { result: "filled" as const };
    selected.add(uid);
    const full = selected.size >= needed;

    const workerName = String(worker.get(WorkerProfiles.NAME) || "Worker");
    tx.set(responseRef, {
      [R.WORKER_ID]: uid,
      [R.STATUS]: RS.ACCEPTED,
      [R.WORKER_NAME]: workerName,
      [R.CREATED_AT]: response.exists ? response.get(R.CREATED_AT) : Timestamp.now(),
    });
    tx.update(ref, {
      [InstantRequests.SELECTED_WORKER_IDS]: Array.from(selected),
      ...(response.exists ? {} : { [InstantRequests.RESPONSE_COUNT]: FieldValue.increment(1) }),
      ...(full ? { [InstantRequests.STATUS]: ST.FILLED, [InstantRequests.NEXT_WAVE_AT]: FieldValue.delete() } : {}),
    });
    tx.update(workerRef, { [WorkerProfiles.ACTIVE_URGENT_ID]: id });
    return {
      ...contact, fresh: true, workerName, count: selected.size, needed,
      employerId: String(r[InstantRequests.EMPLOYER_ID]),
    };
  });

  if (outcome.result === "accepted" && "fresh" in outcome && outcome.fresh && "employerId" in outcome) {
    await notify(outcome.employerId, {
      type: "NEW_APPLICATION",
      templateId: "URGENT_ACCEPTED",
      params: { workerName: outcome.workerName, title: outcome.title, count: outcome.count, needed: outcome.needed },
      data: { requestId: id, action: "view_urgent_needs" },
      role: Values.Role.EMPLOYER,
    });
  }
  if (outcome.result !== "accepted") return { result: outcome.result };
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
export async function readActiveWorkers(
  tx: admin.firestore.Transaction, requestId: string, workerIds: string[],
): Promise<admin.firestore.DocumentReference[]> {
  if (!workerIds.length) return [];
  const refs = workerIds.map((w) => db.collection(WorkerProfiles.COLLECTION).doc(w));
  const snaps = await tx.getAll(...refs);
  return snaps.filter((s) => s.exists && s.get(WorkerProfiles.ACTIVE_URGENT_ID) === requestId).map((s) => s.ref);
}

export function releaseWorkers(tx: admin.firestore.Transaction, refs: admin.firestore.DocumentReference[]): void {
  refs.forEach((ref) => tx.update(ref, { [WorkerProfiles.ACTIVE_URGENT_ID]: FieldValue.delete() }));
}
