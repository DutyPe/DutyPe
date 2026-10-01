/**
 * Worker cards, place, matching and the employer's view of a worker.
 *
 *   onWorkerProfileWritten       worker_profiles → worker_cards (public: no phone, home rounded to ~1 km)
 *   resolvePlace                 district / state for a point; for a worker, also records where they are now
 *   matchWorkersForJob           suitable workers for an employer's job: 5 → 10 → 20 km, then the district
 *   getWorkerProfileForEmployer  the private profile, only for an employer with a relationship
 *
 * Matching keys: a card carries "{SKILL}_{cell}" and "ANY_{cell}" for the worker's home cell and
 * current cell (geohash5, ~4.9 km), and the same per district. "Drivers within 10 km of this job"
 * is then one array-contains-any query over the ~30 cells around the job — only matching workers
 * are read, most recently active first.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str, latLng } from "./lib/input";
import { coarse, coveringCells, decodeGeohash, distanceKm, encodeGeohash } from "./lib/geo";
import { placeOf } from "./lib/places";
import {
  Applications, EmployerProfiles, Idempotency, InstantRequests, Jobs, Values, WorkerCards, WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;

const MATCH_BANDS_KM = [5, 10, 20];
const MAX_MATCHES = 30;
const QUERY_LIMIT = 40;
const MATCH_CACHE_MS = 30 * 60 * 1000;
const ACTIVE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
/** lastActiveAt is refreshed at most this often (it ranks workers in matching). */
const ACTIVE_REFRESH_MS = 20 * 60 * 60 * 1000;
const ANY = "ANY";

// ─────────────────────────────── card sync ───────────────────────────────

const CARD_FIELDS: Array<[string, string]> = [
  [WorkerProfiles.NAME, WorkerCards.NAME],
  [WorkerProfiles.PHOTO_URL, WorkerCards.PHOTO_URL],
  [WorkerProfiles.SKILLS, WorkerCards.SKILLS],
  [WorkerProfiles.EXPERIENCE_YEARS, WorkerCards.EXPERIENCE_YEARS],
  [WorkerProfiles.AREA, WorkerCards.AREA],
  [WorkerProfiles.AVAILABLE, WorkerCards.AVAILABLE],
];
const WATCHED = [...CARD_FIELDS.map(([p]) => p), WorkerProfiles.LAT, WorkerProfiles.LNG];

function skillsOf(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}

/** The matching keys for a set of skills over the home + current cell and district. */
function matchKeys(skills: string[], cells: unknown[], districts: unknown[]): Record<string, string[]> {
  const tags = [...skills, ANY];
  const keys = (places: unknown[]) => Array.from(new Set(
    places.filter((v) => v !== undefined && v !== null && v !== "").flatMap((v) => tags.map((t) => `${t}_${v}`)),
  )).slice(0, 30);
  return { [WorkerCards.SKILL_CELLS]: keys(cells), [WorkerCards.SKILL_DISTRICTS]: keys(districts) };
}

export const onWorkerProfileWritten = functions
  .region("asia-south1")
  .runWith({ memory: "512MB" })
  .firestore.document(`${WorkerProfiles.COLLECTION}/{uid}`)
  .onWrite(async (change, context) => {
    const cardRef = db.collection(WorkerCards.COLLECTION).doc(context.params.uid);
    const after = change.after.data();
    if (!after || after[WorkerProfiles.BLOCKED] === true) {
      await cardRef.delete();
      return;
    }
    const before = change.before.data() || {};
    const changed = WATCHED.some((p) => JSON.stringify(before[p]) !== JSON.stringify(after[p]));
    if (!changed && change.before.exists) return;

    const lat = Number(after[WorkerProfiles.LAT]);
    const lng = Number(after[WorkerProfiles.LNG]);
    const hasPoint = Number.isFinite(lat) && Number.isFinite(lng) && !(lat === 0 && lng === 0);
    const place = hasPoint ? placeOf(lat, lng) : null;
    const cell = hasPoint ? encodeGeohash(lat, lng, 5) : null;
    const skills = skillsOf(after[WorkerProfiles.SKILLS]);

    await db.runTransaction(async (tx) => {
      const current = (await tx.get(cardRef)).data() || {};
      const card: Record<string, unknown> = { [WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp() };
      CARD_FIELDS.forEach(([p, c]) => {
        card[c] = after[p] ?? FieldValue.delete();
      });
      card[WorkerCards.LAT] = hasPoint ? coarse(lat) : FieldValue.delete();
      card[WorkerCards.LNG] = hasPoint ? coarse(lng) : FieldValue.delete();
      card[WorkerCards.CELL] = cell ?? FieldValue.delete();
      card[WorkerCards.DISTRICT_ID] = place?.districtId ?? FieldValue.delete();
      card[WorkerCards.STATE_ID] = place?.stateId ?? FieldValue.delete();
      Object.assign(card, matchKeys(skills,
        [cell, current[WorkerCards.CURRENT_CELL]],
        [place?.districtId, current[WorkerCards.CURRENT_DISTRICT_ID]]));
      tx.set(cardRef, card, { merge: true });
    });
  });

// ─────────────────────────────── place ───────────────────────────────

/**
 * District and state for a point (the app caches the answer per ~5 km cell, so this runs about
 * once per cell a user visits). With `track: true` from a worker (a new cell, or the first app
 * open of the day), the worker's card also records the current cell and district, so employers
 * there can find them, and refreshes lastActiveAt.
 */
export const resolvePlace = onCallSecured({ requireAuth: false, memory: "512MB" }, async (raw: unknown, context) => {
  const data = obj(raw);
  const { lat, lng } = latLng(data);
  const place = placeOf(lat, lng);
  const cell = encodeGeohash(lat, lng, 5);
  const uid = context.auth?.uid;
  if (data.track === true && uid && context.auth?.token.role === Values.Role.WORKER) {
    const cardRef = db.collection(WorkerCards.COLLECTION).doc(uid);
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(cardRef);
      if (!snap.exists) return;
      const card = snap.data() || {};
      if (card[WorkerCards.CURRENT_CELL] === cell) {
        // Same area: only refresh "recently active" (the app calls at most once a day).
        const last = (card[WorkerCards.LAST_ACTIVE_AT] as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
        if (Date.now() - last > ACTIVE_REFRESH_MS) tx.update(cardRef, { [WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp() });
        return;
      }
      tx.update(cardRef, {
        [WorkerCards.CURRENT_CELL]: cell,
        [WorkerCards.CURRENT_DISTRICT_ID]: place?.districtId ?? FieldValue.delete(),
        [WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp(),
        ...matchKeys(skillsOf(card[WorkerCards.SKILLS]),
          [card[WorkerCards.CELL], cell], [card[WorkerCards.DISTRICT_ID], place?.districtId]),
      });
    });
  }
  return { cell, ...(place ?? {}) };
});

// ─────────────────────────────── matching ───────────────────────────────

interface MatchedWorker {
  workerId: string; fullName: string; phone: string; profileImageUrl: string; skills: string[];
  experience: string; rating: number; ratingCount: number; completedJobs: number; isAvailable: boolean;
  distanceKm: number; matchScore: number; matchReasons: string[];
}

/** Approximate km to a worker: their rounded home point, or the centre of the cell they are in now. */
function approxKm(lat: number, lng: number, w: admin.firestore.DocumentData): number {
  const options: number[] = [];
  if (Number.isFinite(Number(w[WorkerCards.LAT])) && w[WorkerCards.LAT] !== undefined) {
    options.push(distanceKm(lat, lng, Number(w[WorkerCards.LAT]), Number(w[WorkerCards.LNG])));
  }
  if (typeof w[WorkerCards.CURRENT_CELL] === "string") {
    const box = decodeGeohash(w[WorkerCards.CURRENT_CELL]);
    options.push(distanceKm(lat, lng, (box.minLat + box.maxLat) / 2, (box.minLng + box.maxLng) / 2));
  }
  return options.length ? Math.min(...options) : Number.POSITIVE_INFINITY;
}

function toMatch(
  lat: number, lng: number, category: string, nowMs: number, d: admin.firestore.QueryDocumentSnapshot,
): MatchedWorker {
  const w = d.data();
  const km = approxKm(lat, lng, w);
  const skills = skillsOf(w[WorkerCards.SKILLS]);
  const available = w[WorkerCards.AVAILABLE] === true;
  const lastActive = (w[WorkerCards.LAST_ACTIVE_AT] as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
  const rating = Number(w[WorkerCards.RATING] || 0);
  const reasons: string[] = [];
  let score = Number.isFinite(km) ? Math.max(0, 40 - km * 2) : 0;
  if (category && skills.includes(category)) { score += 30; reasons.push("Skill matches this job"); }
  if (available) { score += 15; reasons.push("Available now"); }
  if (nowMs - lastActive < ACTIVE_WINDOW_MS) { score += 10; reasons.push("Active this week"); }
  if (rating >= 4) { score += 5; reasons.push(`Rated ${rating.toFixed(1)}`); }
  if (Number.isFinite(km)) reasons.unshift(km < 1.5 ? "About 1 km away" : `About ${Math.round(km)} km away`);
  return {
    workerId: d.id,
    fullName: String(w[WorkerCards.NAME] || "Worker"),
    phone: "",
    profileImageUrl: String(w[WorkerCards.PHOTO_URL] || ""),
    skills: skills.slice(0, 8),
    experience: w[WorkerCards.EXPERIENCE_YEARS] ? `${w[WorkerCards.EXPERIENCE_YEARS]} yrs` : "",
    rating,
    ratingCount: Number(w[WorkerCards.RATING_COUNT] || 0),
    completedJobs: Number(w[WorkerCards.JOBS_COMPLETED] || 0),
    isAvailable: available,
    distanceKm: Number.isFinite(km) ? Number(km.toFixed(1)) : -1,
    matchScore: Math.round(score),
    matchReasons: reasons,
  };
}

/**
 * Suitable workers for one of the caller's jobs (or urgent needs): the job's skill within 5 km,
 * widening to 10 and 20 km, then the job's district, until 30 are found. Only workers with the
 * skill are read. The answer is cached for 30 minutes, so reopening the list costs one read.
 */
export const matchWorkersForJob = onCallSecured({ timeoutSeconds: 30, memory: "512MB" }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const jobId = str(obj(raw), "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
  const cacheRef = db.collection(Idempotency.COLLECTION).doc(`match_${jobId}`);
  const [cached, jobSnap] = await Promise.all([cacheRef.get(), db.collection(Jobs.COLLECTION).doc(jobId).get()]);
  // A regular job or an instant (urgent) request — both carry employerId, lat, lng, category.
  const job = jobSnap.exists ? jobSnap : await db.collection(InstantRequests.COLLECTION).doc(jobId).get();
  if (!job.exists || job.get(Jobs.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your job");
  const cachedAt = (cached.get("at") as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
  if (cached.exists && Date.now() - cachedAt < MATCH_CACHE_MS) return { workers: cached.get(Idempotency.RESULT) ?? [] };

  const lat = Number(job.get(Jobs.LAT));
  const lng = Number(job.get(Jobs.LNG));
  const category = String(job.get(Jobs.CATEGORY) || "");
  const tag = category && category !== "OTHER" ? category : ANY;
  const nowMs = Date.now();
  const found = new Map<string, MatchedWorker>();
  const take = (docs: admin.firestore.QueryDocumentSnapshot[]) => docs.forEach((d) => {
    if (d.id !== uid && !found.has(d.id)) found.set(d.id, toMatch(lat, lng, category, nowMs, d));
  });

  const queried = new Set<string>();
  for (const radius of MATCH_BANDS_KM) {
    if (found.size >= MAX_MATCHES) break;
    const cells = coveringCells(lat, lng, radius, 5).filter((c) => !queried.has(c));
    cells.forEach((c) => queried.add(c));
    for (let i = 0; i < cells.length && found.size < MAX_MATCHES; i += 30) {
      const snap = await db.collection(WorkerCards.COLLECTION)
        .where(WorkerCards.SKILL_CELLS, "array-contains-any", cells.slice(i, i + 30).map((c) => `${tag}_${c}`))
        .orderBy(WorkerCards.LAST_ACTIVE_AT, "desc")
        .limit(QUERY_LIMIT)
        .get();
      take(snap.docs);
    }
  }
  if (found.size < MAX_MATCHES) {
    const districtId = job.get(Jobs.DISTRICT_ID) ?? placeOf(lat, lng)?.districtId;
    if (districtId !== undefined) {
      const snap = await db.collection(WorkerCards.COLLECTION)
        .where(WorkerCards.SKILL_DISTRICTS, "array-contains", `${tag}_${districtId}`)
        .orderBy(WorkerCards.LAST_ACTIVE_AT, "desc")
        .limit(QUERY_LIMIT)
        .get();
      take(snap.docs);
    }
  }

  const workers = Array.from(found.values())
    .sort((a, b) => b.matchScore - a.matchScore)
    .slice(0, MAX_MATCHES);
  await cacheRef.set({
    [Idempotency.RESULT]: workers,
    at: Timestamp.fromMillis(nowMs),
    [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(nowMs + 24 * 60 * 60 * 1000),
  });
  return { workers };
});

// ─────────────────────────── employer's worker view ───────────────────────────

export const getWorkerProfileForEmployer = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const workerId = str(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
  const jobId = str(data, "jobId", { max: 64, optional: true, pattern: /^[A-Za-z0-9_-]+$/ });

  const unlockRef = db.collection(EmployerProfiles.COLLECTION).doc(uid)
    .collection(EmployerProfiles.Unlocks.COLLECTION).doc(workerId);
  const [unlock, app, profile, card] = await Promise.all([
    unlockRef.get(),
    jobId ? db.collection(Applications.COLLECTION).doc(`${jobId}_${workerId}`).get() : Promise.resolve(null),
    db.collection(WorkerProfiles.COLLECTION).doc(workerId).get(),
    db.collection(WorkerCards.COLLECTION).doc(workerId).get(),
  ]);
  let related = unlock.exists || (!!app && app.exists && app.get(Applications.EMPLOYER_ID) === uid);
  if (!related) {
    const any = await db.collection(Applications.COLLECTION)
      .where(Applications.EMPLOYER_ID, "==", uid).where(Applications.WORKER_ID, "==", workerId).limit(1).get();
    related = !any.empty;
  }
  if (!related) fail("permission-denied", "You can view workers who applied to your jobs");
  if (!profile.exists) fail("not-found", "Worker profile not found");

  const P = WorkerProfiles;
  const p = profile.data() || {};
  const c = card.data() || {};
  return {
    profile: {
      workerId,
      [P.NAME]: p[P.NAME] ?? "",
      [P.PHOTO_URL]: p[P.PHOTO_URL] ?? "",
      [P.GENDER]: p[P.GENDER] ?? "",
      [P.DATE_OF_BIRTH]: p[P.DATE_OF_BIRTH] ?? "",
      [P.EDUCATION]: p[P.EDUCATION] ?? "",
      [P.EXPERIENCE_YEARS]: p[P.EXPERIENCE_YEARS] ?? 0,
      [P.SKILLS]: p[P.SKILLS] ?? [],
      [P.BIO]: p[P.BIO] ?? "",
      [P.AREA]: p[P.AREA] ?? "",
      [P.AVAILABLE]: p[P.AVAILABLE] === true,
      [WorkerCards.RATING]: c[WorkerCards.RATING] ?? 0,
      [WorkerCards.RATING_COUNT]: c[WorkerCards.RATING_COUNT] ?? 0,
      [WorkerCards.JOBS_COMPLETED]: c[WorkerCards.JOBS_COMPLETED] ?? 0,
      // The phone is shown only after the employer revealed it (getWorkerContact).
      [P.PHONE]: unlock.exists ? p[P.PHONE] ?? "" : "",
    },
  };
});
