"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getWorkerProfileForEmployer = exports.matchWorkersForJob = exports.resolvePlace = exports.onWorkerProfileWritten = void 0;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const places_1 = require("./lib/places");
const schema_1 = require("./schema");
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
const CARD_FIELDS = [
    [schema_1.WorkerProfiles.NAME, schema_1.WorkerCards.NAME],
    [schema_1.WorkerProfiles.PHOTO_URL, schema_1.WorkerCards.PHOTO_URL],
    [schema_1.WorkerProfiles.SKILLS, schema_1.WorkerCards.SKILLS],
    [schema_1.WorkerProfiles.EXPERIENCE_YEARS, schema_1.WorkerCards.EXPERIENCE_YEARS],
    [schema_1.WorkerProfiles.AREA, schema_1.WorkerCards.AREA],
    [schema_1.WorkerProfiles.AVAILABLE, schema_1.WorkerCards.AVAILABLE],
];
const WATCHED = [...CARD_FIELDS.map(([p]) => p), schema_1.WorkerProfiles.LAT, schema_1.WorkerProfiles.LNG];
function skillsOf(value) {
    return Array.isArray(value) ? value.map(String).filter(Boolean) : [];
}
/** The matching keys for a set of skills over the home + current cell and district. */
function matchKeys(skills, cells, districts) {
    const tags = [...skills, ANY];
    const keys = (places) => Array.from(new Set(places.filter((v) => v !== undefined && v !== null && v !== "").flatMap((v) => tags.map((t) => `${t}_${v}`)))).slice(0, 30);
    return { [schema_1.WorkerCards.SKILL_CELLS]: keys(cells), [schema_1.WorkerCards.SKILL_DISTRICTS]: keys(districts) };
}
exports.onWorkerProfileWritten = functions
    .region("asia-south1")
    .runWith({ memory: "512MB" })
    .firestore.document(`${schema_1.WorkerProfiles.COLLECTION}/{uid}`)
    .onWrite(async (change, context) => {
    const cardRef = db.collection(schema_1.WorkerCards.COLLECTION).doc(context.params.uid);
    const after = change.after.data();
    if (!after || after[schema_1.WorkerProfiles.BLOCKED] === true) {
        await cardRef.delete();
        return;
    }
    const before = change.before.data() || {};
    const changed = WATCHED.some((p) => JSON.stringify(before[p]) !== JSON.stringify(after[p]));
    if (!changed && change.before.exists)
        return;
    const lat = Number(after[schema_1.WorkerProfiles.LAT]);
    const lng = Number(after[schema_1.WorkerProfiles.LNG]);
    const hasPoint = Number.isFinite(lat) && Number.isFinite(lng) && !(lat === 0 && lng === 0);
    const place = hasPoint ? (0, places_1.placeOf)(lat, lng) : null;
    const cell = hasPoint ? (0, geo_1.encodeGeohash)(lat, lng, 5) : null;
    const skills = skillsOf(after[schema_1.WorkerProfiles.SKILLS]);
    await db.runTransaction(async (tx) => {
        var _a, _b;
        const current = (await tx.get(cardRef)).data() || {};
        const card = { [schema_1.WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp() };
        CARD_FIELDS.forEach(([p, c]) => {
            var _a;
            card[c] = (_a = after[p]) !== null && _a !== void 0 ? _a : FieldValue.delete();
        });
        card[schema_1.WorkerCards.LAT] = hasPoint ? (0, geo_1.coarse)(lat) : FieldValue.delete();
        card[schema_1.WorkerCards.LNG] = hasPoint ? (0, geo_1.coarse)(lng) : FieldValue.delete();
        card[schema_1.WorkerCards.CELL] = cell !== null && cell !== void 0 ? cell : FieldValue.delete();
        card[schema_1.WorkerCards.DISTRICT_ID] = (_a = place === null || place === void 0 ? void 0 : place.districtId) !== null && _a !== void 0 ? _a : FieldValue.delete();
        card[schema_1.WorkerCards.STATE_ID] = (_b = place === null || place === void 0 ? void 0 : place.stateId) !== null && _b !== void 0 ? _b : FieldValue.delete();
        Object.assign(card, matchKeys(skills, [cell, current[schema_1.WorkerCards.CURRENT_CELL]], [place === null || place === void 0 ? void 0 : place.districtId, current[schema_1.WorkerCards.CURRENT_DISTRICT_ID]]));
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
exports.resolvePlace = (0, secure_callable_1.onCallSecured)({ requireAuth: false, memory: "512MB" }, async (raw, context) => {
    var _a, _b;
    const data = (0, input_1.obj)(raw);
    const { lat, lng } = (0, input_1.latLng)(data);
    const place = (0, places_1.placeOf)(lat, lng);
    const cell = (0, geo_1.encodeGeohash)(lat, lng, 5);
    const uid = (_a = context.auth) === null || _a === void 0 ? void 0 : _a.uid;
    if (data.track === true && uid && ((_b = context.auth) === null || _b === void 0 ? void 0 : _b.token.role) === schema_1.Values.Role.WORKER) {
        const cardRef = db.collection(schema_1.WorkerCards.COLLECTION).doc(uid);
        await db.runTransaction(async (tx) => {
            var _a, _b, _c;
            const snap = await tx.get(cardRef);
            if (!snap.exists)
                return;
            const card = snap.data() || {};
            if (card[schema_1.WorkerCards.CURRENT_CELL] === cell) {
                // Same area: only refresh "recently active" (the app calls at most once a day).
                const last = (_b = (_a = card[schema_1.WorkerCards.LAST_ACTIVE_AT]) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
                if (Date.now() - last > ACTIVE_REFRESH_MS)
                    tx.update(cardRef, { [schema_1.WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp() });
                return;
            }
            tx.update(cardRef, Object.assign({ [schema_1.WorkerCards.CURRENT_CELL]: cell, [schema_1.WorkerCards.CURRENT_DISTRICT_ID]: (_c = place === null || place === void 0 ? void 0 : place.districtId) !== null && _c !== void 0 ? _c : FieldValue.delete(), [schema_1.WorkerCards.LAST_ACTIVE_AT]: FieldValue.serverTimestamp() }, matchKeys(skillsOf(card[schema_1.WorkerCards.SKILLS]), [card[schema_1.WorkerCards.CELL], cell], [card[schema_1.WorkerCards.DISTRICT_ID], place === null || place === void 0 ? void 0 : place.districtId])));
        });
    }
    return Object.assign({ cell }, (place !== null && place !== void 0 ? place : {}));
});
/** Approximate km to a worker: their rounded home point, or the centre of the cell they are in now. */
function approxKm(lat, lng, w) {
    const options = [];
    if (Number.isFinite(Number(w[schema_1.WorkerCards.LAT])) && w[schema_1.WorkerCards.LAT] !== undefined) {
        options.push((0, geo_1.distanceKm)(lat, lng, Number(w[schema_1.WorkerCards.LAT]), Number(w[schema_1.WorkerCards.LNG])));
    }
    if (typeof w[schema_1.WorkerCards.CURRENT_CELL] === "string") {
        const box = (0, geo_1.decodeGeohash)(w[schema_1.WorkerCards.CURRENT_CELL]);
        options.push((0, geo_1.distanceKm)(lat, lng, (box.minLat + box.maxLat) / 2, (box.minLng + box.maxLng) / 2));
    }
    return options.length ? Math.min(...options) : Number.POSITIVE_INFINITY;
}
function toMatch(lat, lng, category, nowMs, d) {
    var _a, _b;
    const w = d.data();
    const km = approxKm(lat, lng, w);
    const skills = skillsOf(w[schema_1.WorkerCards.SKILLS]);
    const available = w[schema_1.WorkerCards.AVAILABLE] === true;
    const lastActive = (_b = (_a = w[schema_1.WorkerCards.LAST_ACTIVE_AT]) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
    const rating = Number(w[schema_1.WorkerCards.RATING] || 0);
    const reasons = [];
    let score = Number.isFinite(km) ? Math.max(0, 40 - km * 2) : 0;
    if (category && skills.includes(category)) {
        score += 30;
        reasons.push("Skill matches this job");
    }
    if (available) {
        score += 15;
        reasons.push("Available now");
    }
    if (nowMs - lastActive < ACTIVE_WINDOW_MS) {
        score += 10;
        reasons.push("Active this week");
    }
    if (rating >= 4) {
        score += 5;
        reasons.push(`Rated ${rating.toFixed(1)}`);
    }
    if (Number.isFinite(km))
        reasons.unshift(km < 1.5 ? "About 1 km away" : `About ${Math.round(km)} km away`);
    return {
        workerId: d.id,
        fullName: String(w[schema_1.WorkerCards.NAME] || "Worker"),
        phone: "",
        profileImageUrl: String(w[schema_1.WorkerCards.PHOTO_URL] || ""),
        skills: skills.slice(0, 8),
        experience: w[schema_1.WorkerCards.EXPERIENCE_YEARS] ? `${w[schema_1.WorkerCards.EXPERIENCE_YEARS]} yrs` : "",
        rating,
        ratingCount: Number(w[schema_1.WorkerCards.RATING_COUNT] || 0),
        completedJobs: Number(w[schema_1.WorkerCards.JOBS_COMPLETED] || 0),
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
exports.matchWorkersForJob = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30, memory: "512MB" }, async (raw, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j;
    const uid = context.auth.uid;
    const jobId = (0, input_1.str)((0, input_1.obj)(raw), "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    const cacheRef = db.collection(schema_1.Idempotency.COLLECTION).doc(`match_${jobId}`);
    const [cached, jobSnap] = await Promise.all([cacheRef.get(), db.collection(schema_1.Jobs.COLLECTION).doc(jobId).get()]);
    // A regular job or an instant (urgent) request — both carry employerId, lat, lng, category.
    let job = jobSnap.exists ? jobSnap : await db.collection(schema_1.InstantRequests.COLLECTION).doc(jobId).get();
    if (!job.exists) {
        job = await db.collection("jobs").doc(jobId).get();
    }
    const employerId = job.get(schema_1.Jobs.EMPLOYER_ID) || job.get("employerId") || job.get("employer_id") || job.get("userId") || job.get("postedBy");
    if (!job.exists || employerId !== uid)
        (0, input_1.fail)("permission-denied", "Not your job");
    const cachedAt = (_b = (_a = cached.get("at")) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
    if (cached.exists && Date.now() - cachedAt < MATCH_CACHE_MS)
        return { workers: (_c = cached.get(schema_1.Idempotency.RESULT)) !== null && _c !== void 0 ? _c : [] };
    const rawLat = Number((_e = (_d = job.get(schema_1.Jobs.LAT)) !== null && _d !== void 0 ? _d : job.get("lat")) !== null && _e !== void 0 ? _e : job.get("latitude"));
    const rawLng = Number((_g = (_f = job.get(schema_1.Jobs.LNG)) !== null && _f !== void 0 ? _f : job.get("lng")) !== null && _g !== void 0 ? _g : job.get("longitude"));
    const lat = Number.isFinite(rawLat) && rawLat !== 0 ? rawLat : 17.2473;
    const lng = Number.isFinite(rawLng) && rawLng !== 0 ? rawLng : 80.1514;
    const rawCat = String(job.get(schema_1.Jobs.CATEGORY) || job.get("category") || job.get("workType") || "").trim();
    const catUpper = rawCat.toUpperCase();
    const catTitle = rawCat ? rawCat.charAt(0).toUpperCase() + rawCat.slice(1).toLowerCase() : "";
    const skillTags = Array.from(new Set([catUpper, catTitle, rawCat].filter(t => t && t !== "OTHER")));
    const nowMs = Date.now();
    const found = new Map();
    const take = (docs) => docs.forEach((d) => {
        if (d.id !== uid && !found.has(d.id))
            found.set(d.id, toMatch(lat, lng, rawCat, nowMs, d));
    });
    const queryTagCells = async (tags, cellsList) => {
        for (const tag of tags) {
            if (found.size >= MAX_MATCHES)
                break;
            for (let i = 0; i < cellsList.length && found.size < MAX_MATCHES; i += 30) {
                const batch = cellsList.slice(i, i + 30).map((c) => `${tag}_${c}`);
                try {
                    const snap = await db.collection(schema_1.WorkerCards.COLLECTION)
                        .where(schema_1.WorkerCards.SKILL_CELLS, "array-contains-any", batch)
                        .orderBy(schema_1.WorkerCards.LAST_ACTIVE_AT, "desc")
                        .limit(QUERY_LIMIT)
                        .get();
                    take(snap.docs);
                }
                catch (_a) {
                    try {
                        const snap = await db.collection(schema_1.WorkerCards.COLLECTION)
                            .where(schema_1.WorkerCards.SKILL_CELLS, "array-contains-any", batch)
                            .limit(QUERY_LIMIT)
                            .get();
                        take(snap.docs);
                    }
                    catch (_b) {
                        // graceful ignore
                    }
                }
            }
        }
    };
    const queried = new Set();
    const bandsCells = [];
    for (const radius of MATCH_BANDS_KM) {
        const cells = (0, geo_1.coveringCells)(lat, lng, radius, 5).filter((c) => !queried.has(c));
        cells.forEach((c) => queried.add(c));
        bandsCells.push(cells);
    }
    // Phase 1: Query exact skill tags in expanding bands
    for (const cells of bandsCells) {
        if (found.size >= MAX_MATCHES)
            break;
        if (skillTags.length > 0) {
            await queryTagCells(skillTags, cells);
        }
    }
    // Phase 2: If still fewer than MAX_MATCHES, query ANY tag in expanding bands
    if (found.size < MAX_MATCHES) {
        for (const cells of bandsCells) {
            if (found.size >= MAX_MATCHES)
                break;
            await queryTagCells([ANY], cells);
        }
    }
    // Phase 3: District fallback
    if (found.size < MAX_MATCHES) {
        const districtId = (_h = job.get(schema_1.Jobs.DISTRICT_ID)) !== null && _h !== void 0 ? _h : (_j = (0, places_1.placeOf)(lat, lng)) === null || _j === void 0 ? void 0 : _j.districtId;
        if (districtId !== undefined) {
            const allTags = [...skillTags, ANY];
            for (const tag of allTags) {
                if (found.size >= MAX_MATCHES)
                    break;
                try {
                    const snap = await db.collection(schema_1.WorkerCards.COLLECTION)
                        .where(schema_1.WorkerCards.SKILL_DISTRICTS, "array-contains", `${tag}_${districtId}`)
                        .orderBy(schema_1.WorkerCards.LAST_ACTIVE_AT, "desc")
                        .limit(QUERY_LIMIT)
                        .get();
                    take(snap.docs);
                }
                catch (_k) {
                    try {
                        const snap = await db.collection(schema_1.WorkerCards.COLLECTION)
                            .where(schema_1.WorkerCards.SKILL_DISTRICTS, "array-contains", `${tag}_${districtId}`)
                            .limit(QUERY_LIMIT)
                            .get();
                        take(snap.docs);
                    }
                    catch (_l) {
                        // graceful ignore
                    }
                }
            }
        }
    }
    const workers = Array.from(found.values())
        .sort((a, b) => b.matchScore - a.matchScore)
        .slice(0, MAX_MATCHES);
    await cacheRef.set({
        [schema_1.Idempotency.RESULT]: workers,
        at: Timestamp.fromMillis(nowMs),
        [schema_1.Idempotency.EXPIRE_AT]: Timestamp.fromMillis(nowMs + 24 * 60 * 60 * 1000),
    });
    return { workers };
});
// ─────────────────────────── employer's worker view ───────────────────────────
exports.getWorkerProfileForEmployer = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j;
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const workerId = (0, input_1.str)(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
    const jobId = (0, input_1.str)(data, "jobId", { max: 64, optional: true, pattern: /^[A-Za-z0-9_-]+$/ });
    const unlockRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid)
        .collection(schema_1.EmployerProfiles.Unlocks.COLLECTION).doc(workerId);
    const [unlock, app, profile, card, userDoc] = await Promise.all([
        unlockRef.get(),
        jobId ? db.collection(schema_1.Applications.COLLECTION).doc(`${jobId}_${workerId}`).get() : Promise.resolve(null),
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(workerId).get(),
        db.collection(schema_1.WorkerCards.COLLECTION).doc(workerId).get(),
        db.collection("users").doc(workerId).get(),
    ]);
    let related = unlock.exists || (!!app && app.exists && app.get(schema_1.Applications.EMPLOYER_ID) === uid);
    if (!related) {
        const any = await db.collection(schema_1.Applications.COLLECTION)
            .where(schema_1.Applications.EMPLOYER_ID, "==", uid).where(schema_1.Applications.WORKER_ID, "==", workerId).limit(1).get();
        related = !any.empty;
    }
    if (!related) {
        const emp = await db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get();
        related = emp.exists;
    }
    if (!related)
        (0, input_1.fail)("permission-denied", "You can view workers who applied to your jobs");
    if (!profile.exists && !card.exists && !userDoc.exists)
        (0, input_1.fail)("not-found", "Worker profile not found");
    const P = schema_1.WorkerProfiles;
    const p = profile.data() || {};
    const c = card.data() || {};
    const u = userDoc.data() || {};
    const resolvedPhone = unlock.exists ? (p[P.PHONE] || u["phoneNumber"] || u["phone"] || "") : "";
    const resolvedName = p[P.NAME] || c[schema_1.WorkerCards.NAME] || u["name"] || u["displayName"] || "Worker";
    const resolvedPhoto = p[P.PHOTO_URL] || c[schema_1.WorkerCards.PHOTO_URL] || u["photoUrl"] || u["profileImageUrl"] || "";
    const resolvedArea = p[P.AREA] || c[schema_1.WorkerCards.AREA] || "";
    const resolvedSkills = (Array.isArray(p[P.SKILLS]) && p[P.SKILLS].length > 0) ? p[P.SKILLS] : (Array.isArray(c[schema_1.WorkerCards.SKILLS]) ? c[schema_1.WorkerCards.SKILLS] : []);
    const resolvedYears = (_b = (_a = p[P.EXPERIENCE_YEARS]) !== null && _a !== void 0 ? _a : c[schema_1.WorkerCards.EXPERIENCE_YEARS]) !== null && _b !== void 0 ? _b : 0;
    return {
        profile: {
            workerId,
            [P.NAME]: resolvedName,
            [P.PHOTO_URL]: resolvedPhoto,
            [P.GENDER]: (_c = p[P.GENDER]) !== null && _c !== void 0 ? _c : "",
            [P.DATE_OF_BIRTH]: (_d = p[P.DATE_OF_BIRTH]) !== null && _d !== void 0 ? _d : "",
            [P.EDUCATION]: (_e = p[P.EDUCATION]) !== null && _e !== void 0 ? _e : "",
            [P.EXPERIENCE_YEARS]: resolvedYears,
            [P.SKILLS]: resolvedSkills,
            [P.BIO]: (_f = p[P.BIO]) !== null && _f !== void 0 ? _f : "",
            [P.AREA]: resolvedArea,
            [P.AVAILABLE]: p[P.AVAILABLE] === true || c[schema_1.WorkerCards.AVAILABLE] === true,
            [schema_1.WorkerCards.RATING]: (_g = c[schema_1.WorkerCards.RATING]) !== null && _g !== void 0 ? _g : 0,
            [schema_1.WorkerCards.RATING_COUNT]: (_h = c[schema_1.WorkerCards.RATING_COUNT]) !== null && _h !== void 0 ? _h : 0,
            [schema_1.WorkerCards.JOBS_COMPLETED]: (_j = c[schema_1.WorkerCards.JOBS_COMPLETED]) !== null && _j !== void 0 ? _j : 0,
            // The phone is shown only after the employer revealed it (getWorkerContact).
            [P.PHONE]: resolvedPhone,
        },
    };
});
//# sourceMappingURL=workers.js.map