"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getWorkerProfileForEmployer = exports.matchWorkersForJob = exports.onWorkerProfileWritten = void 0;
/**
 * Worker cards, matching and the employer's view of a worker.
 *
 *   onWorkerProfileWritten       worker_profiles → worker_cards (public, no phone). Server-only card.
 *   matchWorkersForJob           nearest suitable workers for an employer's job, from worker_cards
 *   getWorkerProfileForEmployer  the private profile, only for an employer with a relationship
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const schema_1 = require("./schema");
const referrals_1 = require("./referrals");
const db = admin.firestore();
const MATCH_RADIUS_KM = 15;
const MATCH_CELL_PRECISION = 5;
const PER_CELL_LIMIT = 40;
const MAX_MATCHES = 30;
const ACTIVE_WINDOW_MS = 7 * 24 * 60 * 60 * 1000;
// ─────────────────────────────── card sync ───────────────────────────────
const CARD_FIELDS = [
    [schema_1.WorkerProfiles.NAME, schema_1.WorkerCards.NAME],
    [schema_1.WorkerProfiles.PHOTO_URL, schema_1.WorkerCards.PHOTO_URL],
    [schema_1.WorkerProfiles.SKILLS, schema_1.WorkerCards.SKILLS],
    [schema_1.WorkerProfiles.EXPERIENCE_YEARS, schema_1.WorkerCards.EXPERIENCE_YEARS],
    [schema_1.WorkerProfiles.AREA, schema_1.WorkerCards.AREA],
    [schema_1.WorkerProfiles.LAT, schema_1.WorkerCards.LAT],
    [schema_1.WorkerProfiles.LNG, schema_1.WorkerCards.LNG],
    [schema_1.WorkerProfiles.GEOHASH, schema_1.WorkerCards.GEOHASH],
    [schema_1.WorkerProfiles.AVAILABLE, schema_1.WorkerCards.AVAILABLE],
];
exports.onWorkerProfileWritten = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.WorkerProfiles.COLLECTION}/{uid}`)
    .onWrite(async (change, context) => {
    const cardRef = db.collection(schema_1.WorkerCards.COLLECTION).doc(context.params.uid);
    const after = change.after.data();
    if (!after || after[schema_1.WorkerProfiles.BLOCKED] === true) {
        await cardRef.delete();
        return;
    }
    const before = change.before.data() || {};
    if (!(0, referrals_1.isWorkerProfileComplete)(change.before.data()) && (0, referrals_1.isWorkerProfileComplete)(after)) {
        await (0, referrals_1.completeReferral)(context.params.uid, schema_1.Values.Role.WORKER);
    }
    const changed = CARD_FIELDS.some(([p]) => JSON.stringify(before[p]) !== JSON.stringify(after[p]));
    if (!changed && change.before.exists)
        return;
    const card = { [schema_1.WorkerCards.LAST_ACTIVE_AT]: admin.firestore.FieldValue.serverTimestamp() };
    CARD_FIELDS.forEach(([p, c]) => {
        var _a;
        card[c] = (_a = after[p]) !== null && _a !== void 0 ? _a : admin.firestore.FieldValue.delete();
    });
    await cardRef.set(card, { merge: true });
});
// ─────────────────────────────── matching ───────────────────────────────
exports.matchWorkersForJob = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 30 }, async (raw, context) => {
    const uid = context.auth.uid;
    const jobId = (0, input_1.str)((0, input_1.obj)(raw), "jobId", { max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
    // A regular job or an instant (urgent) request — both carry employerId, lat, lng, category.
    let job = await db.collection(schema_1.Jobs.COLLECTION).doc(jobId).get();
    if (!job.exists)
        job = await db.collection(schema_1.InstantRequests.COLLECTION).doc(jobId).get();
    if (!job.exists || job.get(schema_1.Jobs.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your job");
    const lat = Number(job.get(schema_1.Jobs.LAT));
    const lng = Number(job.get(schema_1.Jobs.LNG));
    const category = String(job.get(schema_1.Jobs.CATEGORY) || "");
    const cells = (0, geo_1.coveringCells)(lat, lng, MATCH_RADIUS_KM, MATCH_CELL_PRECISION);
    const snaps = await Promise.all(cells.map((cell) => db.collection(schema_1.WorkerCards.COLLECTION)
        .orderBy(schema_1.WorkerCards.GEOHASH).startAt(cell).endAt(cell + "")
        .limit(PER_CELL_LIMIT).get()));
    const nowMs = Date.now();
    const workers = snaps.flatMap((s) => s.docs)
        .filter((d) => d.id !== uid)
        .map((d) => {
        var _a, _b;
        const w = d.data();
        const km = (0, geo_1.distanceKm)(lat, lng, Number(w[schema_1.WorkerCards.LAT]), Number(w[schema_1.WorkerCards.LNG]));
        const skills = Array.isArray(w[schema_1.WorkerCards.SKILLS]) ? w[schema_1.WorkerCards.SKILLS].map(String) : [];
        const available = w[schema_1.WorkerCards.AVAILABLE] === true;
        const lastActive = (_b = (_a = w[schema_1.WorkerCards.LAST_ACTIVE_AT]) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        const rating = Number(w[schema_1.WorkerCards.RATING] || 0);
        const reasons = [];
        let score = Math.max(0, 40 - km * 2);
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
        reasons.unshift(km < 1 ? "Less than 1 km away" : `${km.toFixed(1)} km away`);
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
            distanceKm: Number(km.toFixed(2)),
            matchScore: Math.round(score),
            matchReasons: reasons,
        };
    })
        .filter((w) => w.distanceKm <= MATCH_RADIUS_KM)
        .sort((a, b) => b.matchScore - a.matchScore)
        .slice(0, MAX_MATCHES);
    return { workers };
});
// ─────────────────────────── employer's worker view ───────────────────────────
exports.getWorkerProfileForEmployer = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o;
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const workerId = (0, input_1.str)(data, "workerId", { max: 128, pattern: /^[A-Za-z0-9_-]+$/ });
    const jobId = (0, input_1.str)(data, "jobId", { max: 64, optional: true, pattern: /^[A-Za-z0-9_-]+$/ });
    const unlockRef = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid)
        .collection(schema_1.EmployerProfiles.Unlocks.COLLECTION).doc(workerId);
    const [unlock, app, profile, card] = await Promise.all([
        unlockRef.get(),
        jobId ? db.collection(schema_1.Applications.COLLECTION).doc(`${jobId}_${workerId}`).get() : Promise.resolve(null),
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(workerId).get(),
        db.collection(schema_1.WorkerCards.COLLECTION).doc(workerId).get(),
    ]);
    let related = unlock.exists || (!!app && app.exists && app.get(schema_1.Applications.EMPLOYER_ID) === uid);
    if (!related) {
        const any = await db.collection(schema_1.Applications.COLLECTION)
            .where(schema_1.Applications.EMPLOYER_ID, "==", uid).where(schema_1.Applications.WORKER_ID, "==", workerId).limit(1).get();
        related = !any.empty;
    }
    if (!related)
        (0, input_1.fail)("permission-denied", "You can view workers who applied to your jobs");
    if (!profile.exists)
        (0, input_1.fail)("not-found", "Worker profile not found");
    const P = schema_1.WorkerProfiles;
    const p = profile.data() || {};
    const c = card.data() || {};
    return {
        profile: {
            workerId,
            [P.NAME]: (_a = p[P.NAME]) !== null && _a !== void 0 ? _a : "",
            [P.PHOTO_URL]: (_b = p[P.PHOTO_URL]) !== null && _b !== void 0 ? _b : "",
            [P.GENDER]: (_c = p[P.GENDER]) !== null && _c !== void 0 ? _c : "",
            [P.DATE_OF_BIRTH]: (_d = p[P.DATE_OF_BIRTH]) !== null && _d !== void 0 ? _d : "",
            [P.EDUCATION]: (_e = p[P.EDUCATION]) !== null && _e !== void 0 ? _e : "",
            [P.EXPERIENCE_YEARS]: (_f = p[P.EXPERIENCE_YEARS]) !== null && _f !== void 0 ? _f : 0,
            [P.SKILLS]: (_g = p[P.SKILLS]) !== null && _g !== void 0 ? _g : [],
            [P.BIO]: (_h = p[P.BIO]) !== null && _h !== void 0 ? _h : "",
            [P.AREA]: (_j = p[P.AREA]) !== null && _j !== void 0 ? _j : "",
            [P.AVAILABLE]: p[P.AVAILABLE] === true,
            [schema_1.WorkerCards.RATING]: (_k = c[schema_1.WorkerCards.RATING]) !== null && _k !== void 0 ? _k : 0,
            [schema_1.WorkerCards.RATING_COUNT]: (_l = c[schema_1.WorkerCards.RATING_COUNT]) !== null && _l !== void 0 ? _l : 0,
            [schema_1.WorkerCards.JOBS_COMPLETED]: (_m = c[schema_1.WorkerCards.JOBS_COMPLETED]) !== null && _m !== void 0 ? _m : 0,
            // The phone is shown only after the employer revealed it (getWorkerContact).
            [P.PHONE]: unlock.exists ? (_o = p[P.PHONE]) !== null && _o !== void 0 ? _o : "" : "",
        },
    };
});
//# sourceMappingURL=workers.js.map