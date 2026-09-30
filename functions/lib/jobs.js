"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.expireJobs = exports.onJobWritten = exports.deleteJob = exports.renewJob = exports.setJobStatus = exports.updateJob = exports.postJob = void 0;
/**
 * Jobs — the only writers of jobmetadata (card) and job_details (details).
 *
 *   postJob        create card + details atomically, charge the employer (credit / free quota / campaign)
 *   updateJob      edit within 48 h of posting
 *   setJobStatus   open ⇄ filled / closed (no re-open after expiry — use renewJob)
 *   renewJob       expired/closed job → open for another 30 days (charged like a new post)
 *   deleteJob      within 30 min of posting; refunds the credit it used
 *   onJobWritten   keeps job_cells counts and cleans up after deletes
 *   expireJobs     flips open jobs past expiresAt to expired
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto_1 = require("crypto");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const geo_1 = require("./lib/geo");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
const JOB_LIFETIME_MS = 30 * DAY_MS;
const EDIT_WINDOW_MS = 48 * 60 * 60 * 1000;
const DELETE_WINDOW_MS = 30 * 60 * 1000;
const FREE_POSTS_PER_DAY = 3;
const IST_OFFSET_MS = 330 * 60 * 1000;
const EMPLOYMENT_TYPES = Object.values(schema_1.Values.EmploymentType);
const PAY_TYPES = Object.values(schema_1.Values.PayType);
const SHIFTS = Object.values(schema_1.Values.Shift);
const URGENCIES = Object.values(schema_1.Values.Urgency);
const GENDERS = ["ANY", "MALE", "FEMALE"];
/** Reads and validates the editable job fields. Every field is required on post. */
function readJobInput(data) {
    const payType = (0, input_1.oneOf)(data, "payType", PAY_TYPES);
    const payAmount = (0, input_1.int)(data, "payAmount", { min: 0, max: 10000000 });
    if (payType !== schema_1.Values.PayType.NEGOTIABLE && payAmount < 1)
        (0, input_1.fail)("invalid-argument", "Enter the pay amount");
    const { lat, lng } = (0, input_1.latLng)(data);
    return {
        card: {
            [schema_1.Jobs.TITLE]: (0, input_1.str)(data, "title", { min: 3, max: 80 }),
            [schema_1.Jobs.CATEGORY]: (0, input_1.oneOf)(data, "category", schema_1.CATEGORY_KEYS),
            [schema_1.Jobs.EMPLOYMENT_TYPE]: (0, input_1.oneOf)(data, "employmentType", EMPLOYMENT_TYPES),
            [schema_1.Jobs.PAY_AMOUNT]: payType === schema_1.Values.PayType.NEGOTIABLE ? 0 : payAmount,
            [schema_1.Jobs.PAY_TYPE]: payType,
            [schema_1.Jobs.VACANCIES]: (0, input_1.int)(data, "vacancies", { min: 1, max: 50 }),
            [schema_1.Jobs.SHIFT]: (0, input_1.oneOf)(data, "shift", SHIFTS, schema_1.Values.Shift.ANY),
            [schema_1.Jobs.AREA]: (0, input_1.str)(data, "area", { min: 2, max: 60 }),
            [schema_1.Jobs.LAT]: lat,
            [schema_1.Jobs.LNG]: lng,
            [schema_1.Jobs.GEOHASH]: (0, geo_1.encodeGeohash)(lat, lng),
            [schema_1.Jobs.PHOTO_URL]: (0, input_1.storageUrl)(data, "photoUrl") || FieldValue.delete(),
        },
        details: {
            [schema_1.JobDetails.DESCRIPTION]: (0, input_1.text)(data, "description", { min: 10, max: 2000 }),
            [schema_1.JobDetails.ADDRESS_TEXT]: (0, input_1.str)(data, "addressText", { min: 3, max: 200 }),
            [schema_1.JobDetails.CONTACT_NUMBER]: (0, input_1.mobile)(data, "contactNumber"),
            [schema_1.JobDetails.GENDER]: (0, input_1.oneOf)(data, "gender", GENDERS, "ANY"),
            [schema_1.JobDetails.EXPERIENCE_REQUIRED]: (0, input_1.str)(data, "experienceRequired", { max: 60, optional: true }),
            [schema_1.JobDetails.EDUCATION_REQUIRED]: (0, input_1.str)(data, "educationRequired", { max: 60, optional: true }),
            [schema_1.JobDetails.BENEFITS]: (0, input_1.stringList)(data, "benefits", { maxItems: 8, maxLength: 30 }),
        },
    };
}
/** Removes FieldValue.delete() sentinels, which set() on a new document does not accept. */
function withoutDeletes(doc) {
    return Object.fromEntries(Object.entries(doc).filter(([, v]) => !(v instanceof FieldValue)));
}
function jobIdFor(uid, reqId) {
    return (0, crypto_1.createHash)("sha256").update(`${uid}:${reqId}`).digest("base64url").slice(0, 20);
}
function chargeRef(jobId) {
    return db.collection(schema_1.Idempotency.COLLECTION).doc(`postJob_${jobId}`);
}
function startOfTodayIst(nowMs) {
    const istMidnight = Math.floor((nowMs + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
    return Timestamp.fromMillis(istMidnight);
}
async function loadEmployer(tx, uid) {
    const ref = db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid);
    const snap = await tx.get(ref);
    if (!snap.exists)
        (0, input_1.fail)("failed-precondition", "Complete your employer profile first");
    const data = snap.data() || {};
    if (data[schema_1.EmployerProfiles.BLOCKED] === true)
        (0, input_1.fail)("permission-denied", "Your account is blocked");
    return { ref, data };
}
function companyNameOf(employer) {
    const business = String(employer[schema_1.EmployerProfiles.BUSINESS_NAME] || "").trim();
    const owner = String(employer[schema_1.EmployerProfiles.OWNER_NAME] || "").trim();
    return (employer[schema_1.EmployerProfiles.EMPLOYER_TYPE] === schema_1.Values.EmployerType.COMPANY && business) ?
        business : (owner || business || "Employer");
}
/**
 * Decides how a post is paid for and applies the credit change inside `tx`.
 * Active campaign grant → free; active plan with credits → 1 credit; otherwise the daily free quota.
 */
function chargeForPost(tx, employer, postsToday, nowMs) {
    const S = schema_1.EmployerProfiles.Subscription;
    const sub = (0, input_1.obj)(employer.data[schema_1.EmployerProfiles.SUBSCRIPTION]);
    const expiresAt = sub[S.EXPIRES_AT] instanceof Timestamp ? sub[S.EXPIRES_AT].toMillis() : 0;
    const active = sub[S.STATUS] === "ACTIVE" && (expiresAt === 0 || expiresAt > nowMs);
    if (active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN")
        return "campaign";
    const normalCredits = Number((0, input_1.obj)(sub[S.CREDITS])[S.CREDITS_NORMAL] || 0);
    if (active && normalCredits > 0) {
        tx.update(employer.ref, `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(-1));
        return "credit";
    }
    if (postsToday >= FREE_POSTS_PER_DAY) {
        (0, input_1.fail)("resource-exhausted", `You have used today's ${FREE_POSTS_PER_DAY} free job posts. Buy a plan to post more today.`);
    }
    return "free";
}
/** The employer's posts since midnight IST — the free-quota count and the duplicate check. */
async function postsToday(uid, nowMs) {
    const snap = await db.collection(schema_1.Jobs.COLLECTION)
        .where(schema_1.Jobs.EMPLOYER_ID, "==", uid)
        .where(schema_1.Jobs.CREATED_AT, ">=", startOfTodayIst(nowMs))
        .limit(50)
        .get();
    return snap.docs.map((d) => ({
        title: String(d.get(schema_1.Jobs.TITLE) || "").toLowerCase(),
        category: String(d.get(schema_1.Jobs.CATEGORY) || ""),
        lat: Number(d.get(schema_1.Jobs.LAT) || 0),
        lng: Number(d.get(schema_1.Jobs.LNG) || 0),
        status: String(d.get(schema_1.Jobs.STATUS) || ""),
    }));
}
/** Same title + category within ~1 km, still open, posted today → a repost of the same job. */
function assertNotDuplicate(today, card) {
    const title = String(card[schema_1.Jobs.TITLE]).toLowerCase();
    const lat = Number(card[schema_1.Jobs.LAT]);
    const lng = Number(card[schema_1.Jobs.LNG]);
    const duplicate = today.some((p) => p.status === schema_1.Values.JobStatus.OPEN &&
        p.title === title && p.category === card[schema_1.Jobs.CATEGORY] &&
        Math.abs(p.lat - lat) < 0.01 && Math.abs(p.lng - lng) < 0.01);
    if (duplicate)
        (0, input_1.fail)("already-exists", "You already posted this job today. Edit it or increase vacancies instead.");
}
async function ownedJob(tx, uid, jobId) {
    if (!/^[A-Za-z0-9_-]{6,64}$/.test(jobId))
        (0, input_1.fail)("invalid-argument", "jobId is invalid");
    const ref = db.collection(schema_1.Jobs.COLLECTION).doc(jobId);
    const snap = await tx.get(ref);
    if (!snap.exists)
        (0, input_1.fail)("not-found", "Job not found");
    if (snap.get(schema_1.Jobs.EMPLOYER_ID) !== uid)
        (0, input_1.fail)("permission-denied", "Not your job");
    return { ref, snap };
}
// ───────────────────────────────── callables ─────────────────────────────────
exports.postJob = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const reqId = (0, input_1.requestId)(data);
    const input = readJobInput(data);
    const urgency = (0, input_1.oneOf)(data, "urgency", URGENCIES, schema_1.Values.Urgency.NORMAL);
    const jobId = jobIdFor(uid, reqId);
    const jobRef = db.collection(schema_1.Jobs.COLLECTION).doc(jobId);
    const nowMs = Date.now();
    const today = await postsToday(uid, nowMs);
    return db.runTransaction(async (tx) => {
        const existing = await tx.get(jobRef);
        if (existing.exists)
            return { jobId, replay: true };
        assertNotDuplicate(today, input.card);
        const employer = await loadEmployer(tx, uid);
        const charge = chargeForPost(tx, employer, today.length, nowMs);
        const createdAt = Timestamp.fromMillis(nowMs);
        tx.create(jobRef, withoutDeletes(Object.assign(Object.assign({}, input.card), { [schema_1.Jobs.EMPLOYER_ID]: uid, [schema_1.Jobs.COMPANY_NAME]: companyNameOf(employer.data), [schema_1.Jobs.URGENCY]: urgency, [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.OPEN, [schema_1.Jobs.APPLICATION_COUNT]: 0, [schema_1.Jobs.CREATED_AT]: createdAt, [schema_1.Jobs.EXPIRES_AT]: Timestamp.fromMillis(nowMs + JOB_LIFETIME_MS) })));
        tx.create(db.collection(schema_1.JobDetails.COLLECTION).doc(jobId), Object.assign(Object.assign({}, input.details), { [schema_1.JobDetails.EMPLOYER_ID]: uid }));
        tx.set(chargeRef(jobId), {
            [schema_1.Idempotency.RESULT]: { charge },
            [schema_1.Idempotency.EXPIRE_AT]: Timestamp.fromMillis(nowMs + DELETE_WINDOW_MS + DAY_MS),
        });
        return { jobId, replay: false, charge };
    });
});
exports.updateJob = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const jobId = (0, input_1.str)(data, "jobId", { max: 64 });
    const input = readJobInput(data);
    await db.runTransaction(async (tx) => {
        var _a, _b;
        const { ref, snap } = await ownedJob(tx, uid, jobId);
        const createdAt = (_b = (_a = snap.get(schema_1.Jobs.CREATED_AT)) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        if (Date.now() - createdAt > EDIT_WINDOW_MS) {
            (0, input_1.fail)("failed-precondition", "Jobs can only be edited within 48 hours of posting");
        }
        tx.update(ref, input.card);
        tx.set(db.collection(schema_1.JobDetails.COLLECTION).doc(jobId), input.details, { merge: true });
    });
    return { jobId };
});
exports.setJobStatus = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const jobId = (0, input_1.str)(data, "jobId", { max: 64 });
    const status = (0, input_1.oneOf)(data, "status", [schema_1.Values.JobStatus.OPEN, schema_1.Values.JobStatus.FILLED, schema_1.Values.JobStatus.CLOSED].map((s) => s.toUpperCase()));
    const next = status.toLowerCase();
    await db.runTransaction(async (tx) => {
        const { ref, snap } = await ownedJob(tx, uid, jobId);
        const current = snap.get(schema_1.Jobs.STATUS);
        if (current === next)
            return;
        if (current === schema_1.Values.JobStatus.EXPIRED)
            (0, input_1.fail)("failed-precondition", "This job has expired — renew it instead");
        tx.update(ref, { [schema_1.Jobs.STATUS]: next });
    });
    return { jobId, status: next };
});
exports.renewJob = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const jobId = (0, input_1.str)((0, input_1.obj)(raw), "jobId", { max: 64 });
    const nowMs = Date.now();
    const today = await postsToday(uid, nowMs);
    return db.runTransaction(async (tx) => {
        var _a, _b;
        const { ref, snap } = await ownedJob(tx, uid, jobId);
        const expiresAt = (_b = (_a = snap.get(schema_1.Jobs.EXPIRES_AT)) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        if (snap.get(schema_1.Jobs.STATUS) === schema_1.Values.JobStatus.OPEN && expiresAt > nowMs) {
            (0, input_1.fail)("failed-precondition", "This job is still live");
        }
        const employer = await loadEmployer(tx, uid);
        const charge = chargeForPost(tx, employer, today.length, nowMs);
        tx.update(ref, {
            [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.OPEN,
            [schema_1.Jobs.EXPIRES_AT]: Timestamp.fromMillis(nowMs + JOB_LIFETIME_MS),
        });
        return { jobId, charge };
    });
});
exports.deleteJob = (0, secure_callable_1.onCallSecured)({}, async (raw, context) => {
    const uid = context.auth.uid;
    const jobId = (0, input_1.str)((0, input_1.obj)(raw), "jobId", { max: 64 });
    await db.runTransaction(async (tx) => {
        var _a, _b;
        const { ref, snap } = await ownedJob(tx, uid, jobId);
        const createdAt = (_b = (_a = snap.get(schema_1.Jobs.CREATED_AT)) === null || _a === void 0 ? void 0 : _a.toMillis()) !== null && _b !== void 0 ? _b : 0;
        if (Date.now() - createdAt > DELETE_WINDOW_MS) {
            (0, input_1.fail)("failed-precondition", "Jobs can only be deleted within 30 minutes of posting. Close it instead.");
        }
        const chargeSnap = await tx.get(chargeRef(jobId));
        const charge = (0, input_1.obj)(chargeSnap.get(schema_1.Idempotency.RESULT)).charge;
        if (charge === "credit") {
            const S = schema_1.EmployerProfiles.Subscription;
            tx.update(db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid), `${schema_1.EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(1));
        }
        tx.delete(ref);
        tx.delete(db.collection(schema_1.JobDetails.COLLECTION).doc(jobId));
        tx.delete(chargeRef(jobId));
    });
    return { jobId };
});
function openCell(doc) {
    if (!doc || doc[schema_1.Jobs.STATUS] !== schema_1.Values.JobStatus.OPEN)
        return null;
    const geohash = String(doc[schema_1.Jobs.GEOHASH] || "");
    if (geohash.length < schema_1.JobCells.CELL_PRECISION)
        return null;
    return {
        region: geohash.slice(0, schema_1.JobCells.REGION_PRECISION),
        cell: geohash.slice(0, schema_1.JobCells.CELL_PRECISION),
        category: String(doc[schema_1.Jobs.CATEGORY] || "OTHER"),
        urgent: doc[schema_1.Jobs.URGENCY] === schema_1.Values.Urgency.HIGH,
    };
}
function sameCell(a, b) {
    return !!a && !!b && a.cell === b.cell && a.category === b.category && a.urgent === b.urgent;
}
function cellDelta(key, delta) {
    const counts = {
        [schema_1.JobCells.TOTAL]: FieldValue.increment(delta),
        [key.category]: FieldValue.increment(delta),
    };
    if (key.urgent)
        counts[schema_1.JobCells.URGENT] = FieldValue.increment(delta);
    return db.collection(schema_1.JobCells.COLLECTION).doc(key.region).set({
        [schema_1.JobCells.CELLS]: { [key.cell]: counts },
        [schema_1.JobCells.UPDATED_AT]: FieldValue.serverTimestamp(),
    }, { merge: true });
}
exports.onJobWritten = functions
    .region("asia-south1")
    .firestore.document(`${schema_1.Jobs.COLLECTION}/{jobId}`)
    .onWrite(async (change, context) => {
    const before = openCell(change.before.data());
    const after = openCell(change.after.data());
    const work = [];
    if (!sameCell(before, after)) {
        if (before)
            work.push(cellDelta(before, -1));
        if (after)
            work.push(cellDelta(after, 1));
    }
    if (!change.after.exists) {
        // Job deleted: its applications have nothing left to point at.
        const apps = await db.collection(schema_1.Applications.COLLECTION)
            .where(schema_1.Applications.JOB_ID, "==", context.params.jobId).limit(500).get();
        if (!apps.empty) {
            const batch = db.batch();
            apps.docs.forEach((d) => batch.delete(d.ref));
            work.push(batch.commit());
        }
    }
    await Promise.all(work);
});
exports.expireJobs = functions
    .region("asia-south1")
    .pubsub.schedule("every 30 minutes")
    .timeZone("Asia/Kolkata")
    .onRun(async () => {
    const now = Timestamp.now();
    let expired = 0;
    for (;;) {
        const snap = await db.collection(schema_1.Jobs.COLLECTION)
            .where(schema_1.Jobs.STATUS, "==", schema_1.Values.JobStatus.OPEN)
            .where(schema_1.Jobs.EXPIRES_AT, "<=", now)
            .limit(400)
            .get();
        if (snap.empty)
            break;
        const batch = db.batch();
        snap.docs.forEach((d) => batch.update(d.ref, { [schema_1.Jobs.STATUS]: schema_1.Values.JobStatus.EXPIRED }));
        await batch.commit();
        expired += snap.size;
        if (snap.size < 400)
            break;
    }
    functions.logger.info(`expireJobs: ${expired} jobs expired`);
    return null;
});
//# sourceMappingURL=jobs.js.map