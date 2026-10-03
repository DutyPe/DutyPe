/**
 * Jobs — the only writers of jobmetadata (card), job_details (details) and job_contacts (phone).
 *
 *   postJob          create card + details + contact atomically, charge the employer
 *                    (campaign / credit / daily free quota); the first post completes a pending referral
 *   updateJob        edit within 48 h of posting
 *   setJobStatus     open ⇄ filled / closed (no re-open after expiry — use renewJob)
 *   renewJob         expired/closed job → open for another 30 days (charged like a new post)
 *   deleteJob        within 30 min of posting; gives back the credit or free post it used
 *   onJobWritten     labels the job with its district / state (from lat/lng) and search keywords;
 *                    cleans up after deletes
 *   expireJobs       flips open jobs past expiresAt to expired
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { createHash } from "crypto";
import { vacancyPayProblem } from "./lib/pay-rules";
import { referralPostsLeft, refundReferralPost, useReferralPost } from "./lib/referral-posts";
import { onCallSecured } from "./secure-callable";
import {
  fail, obj, str, text, int, oneOf, stringList, latLng, mobile, storageUrl, requestId,
} from "./lib/input";
import { encodeGeohash } from "./lib/geo";
import { placeOf } from "./lib/places";
import { keywordsOf } from "./lib/keywords";
import { completeReferral } from "./referrals";
import {
  Applications, CATEGORY_KEYS, EmployerProfiles, Idempotency, JobContacts, JobDetails, Jobs, Values, MAX_PAY_RUPEES } from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;

const DAY_MS = 24 * 60 * 60 * 1000;
const JOB_LIFETIME_MS = 30 * DAY_MS;
const EDIT_WINDOW_MS = 48 * 60 * 60 * 1000;
const DELETE_WINDOW_MS = 30 * 60 * 1000;
const FREE_POSTS_PER_DAY = 3;
const IST_OFFSET_MS = 330 * 60 * 1000;
/** A retried trigger event older than this is dropped. */
const EVENT_MAX_AGE_MS = 60 * 60 * 1000;

type Charge = "campaign" | "credit" | "free" | "referral";

const EMPLOYMENT_TYPES = Object.values(Values.EmploymentType);
const PAY_TYPES = Object.values(Values.PayType);
const SHIFTS = Object.values(Values.Shift);
const URGENCIES = Object.values(Values.Urgency);
const GENDERS = ["ANY", "MALE", "FEMALE"] as const;

// ───────────────────────────── payload → documents ─────────────────────────────

interface JobInput {
  card: Record<string, unknown>;
  details: Record<string, unknown>;
  contactNumber: string;
}

/** Reads and validates the editable job fields. Every field is required on post. */
function readJobInput(data: Record<string, unknown>): JobInput {
  const payType = oneOf(data, "payType", PAY_TYPES);
  const payAmount = int(data, "payAmount", { min: 0, max: 10_000_000 });
  if (payAmount < 1) fail("invalid-argument", "Enter the pay amount");
  if (payAmount > MAX_PAY_RUPEES) fail("invalid-argument", "Pay can be at most ₹50,000");
  // Regular vacancies: weekly or monthly pay within local limits (urgent posts are for daily work).
  const payProblem = vacancyPayProblem(payType, String(data.employmentType || ""), payAmount);
  if (payProblem) fail("invalid-argument", payProblem);
  const { lat, lng } = latLng(data);
  return {
    card: {
      [Jobs.TITLE]: str(data, "title", { min: 3, max: 80 }),
      [Jobs.CATEGORY]: oneOf(data, "category", CATEGORY_KEYS),
      [Jobs.EMPLOYMENT_TYPE]: oneOf(data, "employmentType", EMPLOYMENT_TYPES),
      [Jobs.PAY_AMOUNT]: payAmount,
      [Jobs.PAY_TYPE]: payType,
      [Jobs.VACANCIES]: int(data, "vacancies", { min: 1, max: 50 }),
      [Jobs.SHIFT]: oneOf(data, "shift", SHIFTS, Values.Shift.ANY),
      [Jobs.AREA]: str(data, "area", { min: 2, max: 60 }),
      [Jobs.LAT]: lat,
      [Jobs.LNG]: lng,
      [Jobs.GEOHASH]: encodeGeohash(lat, lng),
      [Jobs.CELL]: encodeGeohash(lat, lng, 5),
      [Jobs.PHOTO_URL]: storageUrl(data, "photoUrl") || FieldValue.delete(),
    },
    details: {
      [JobDetails.DESCRIPTION]: text(data, "description", { min: 10, max: 2000 }),
      [JobDetails.ADDRESS_TEXT]: str(data, "addressText", { min: 3, max: 200 }),
      [JobDetails.GENDER]: oneOf(data, "gender", GENDERS, "ANY"),
      [JobDetails.EXPERIENCE_REQUIRED]: str(data, "experienceRequired", { max: 60, optional: true }),
      [JobDetails.EDUCATION_REQUIRED]: str(data, "educationRequired", { max: 60, optional: true }),
      [JobDetails.BENEFITS]: stringList(data, "benefits", { maxItems: 8, maxLength: 30 }),
    },
    contactNumber: mobile(data, "contactNumber"),
  };
}

/** Removes FieldValue.delete() sentinels, which set() on a new document does not accept. */
function withoutDeletes(doc: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(doc).filter(([, v]) => !(v instanceof FieldValue)));
}

function jobIdFor(uid: string, reqId: string): string {
  return createHash("sha256").update(`${uid}:${reqId}`).digest("base64url").slice(0, 20);
}

function chargeRef(jobId: string) {
  return db.collection(Idempotency.COLLECTION).doc(`postJob_${jobId}`);
}

function contactRef(jobId: string) {
  return db.collection(JobContacts.COLLECTION).doc(jobId);
}

function istDay(ms: number): string {
  return new Date(ms + IST_OFFSET_MS).toISOString().slice(0, 10);
}

function startOfTodayIst(nowMs: number): admin.firestore.Timestamp {
  const istMidnight = Math.floor((nowMs + IST_OFFSET_MS) / DAY_MS) * DAY_MS - IST_OFFSET_MS;
  return Timestamp.fromMillis(istMidnight);
}

/** Free posts + renewals used by an employer on one IST day (read and bumped inside the transaction). */
function quotaRef(uid: string, dayMs: number) {
  return db.collection(Idempotency.COLLECTION).doc(`postQuota_${uid}_${istDay(dayMs)}`);
}

// ───────────────────────────── employer + billing ─────────────────────────────

interface Employer {
  ref: admin.firestore.DocumentReference;
  data: admin.firestore.DocumentData;
}

async function loadEmployer(tx: admin.firestore.Transaction, uid: string): Promise<Employer> {
  const ref = db.collection(EmployerProfiles.COLLECTION).doc(uid);
  const snap = await tx.get(ref);
  if (!snap.exists) fail("failed-precondition", "Complete your employer profile first");
  const data = snap.data() || {};
  if (data[EmployerProfiles.BLOCKED] === true) fail("permission-denied", "Your account is blocked");
  return { ref, data };
}

function companyNameOf(employer: admin.firestore.DocumentData): string {
  const business = String(employer[EmployerProfiles.BUSINESS_NAME] || "").trim();
  const owner = String(employer[EmployerProfiles.OWNER_NAME] || "").trim();
  return (employer[EmployerProfiles.EMPLOYER_TYPE] === Values.EmployerType.COMPANY && business) ?
    business : (owner || business || "Employer");
}

/**
 * Decides how a post (or renewal) is paid for and applies the change inside `tx`.
 * Active campaign grant → free; active plan with credits → 1 credit; otherwise one of today's
 * free posts, counted in [quota] (posts and renewals share it). All reads happen before this call.
 */
function chargeForPost(
  tx: admin.firestore.Transaction, employer: Employer, quota: admin.firestore.DocumentSnapshot, nowMs: number,
): Charge {
  const S = EmployerProfiles.Subscription;
  const sub = obj(employer.data[EmployerProfiles.SUBSCRIPTION]);
  const expiresAt = sub[S.EXPIRES_AT] instanceof Timestamp ? (sub[S.EXPIRES_AT] as admin.firestore.Timestamp).toMillis() : 0;
  const active = sub[S.STATUS] === "ACTIVE" && (expiresAt === 0 || expiresAt > nowMs);
  if (active && sub[S.PLAN_ID] === "UNLIMITED_CAMPAIGN") return "campaign";
  const normalCredits = Number(obj(sub[S.CREDITS])[S.CREDITS_NORMAL] || 0);
  if (active && normalCredits > 0) {
    tx.update(employer.ref, `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(-1));
    return "credit";
  }
  const used = Number(quota.get(Idempotency.RESULT) || 0);
  if (used >= FREE_POSTS_PER_DAY && referralPostsLeft(employer.data, nowMs) > 0) {
    useReferralPost(tx, employer.ref);
    return "referral";
  }
  if (used >= FREE_POSTS_PER_DAY) {
    fail("resource-exhausted",
      `You have used today's ${FREE_POSTS_PER_DAY} free job posts. Buy a plan to post more today.`);
  }
  tx.set(quota.ref, {
    [Idempotency.RESULT]: used + 1,
    [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(nowMs + 2 * DAY_MS),
  });
  return "free";
}

interface TodayPost { title: string; category: string; lat: number; lng: number; status: string }

/** The employer's posts since midnight IST — for the duplicate check. */
async function postsToday(uid: string, nowMs: number): Promise<TodayPost[]> {
  const snap = await db.collection(Jobs.COLLECTION)
    .where(Jobs.EMPLOYER_ID, "==", uid)
    .where(Jobs.CREATED_AT, ">=", startOfTodayIst(nowMs))
    .limit(50)
    .get();
  return snap.docs.map((d) => ({
    title: String(d.get(Jobs.TITLE) || "").toLowerCase(),
    category: String(d.get(Jobs.CATEGORY) || ""),
    lat: Number(d.get(Jobs.LAT) || 0),
    lng: Number(d.get(Jobs.LNG) || 0),
    status: String(d.get(Jobs.STATUS) || ""),
  }));
}

/** Same title + category within ~1 km, still open, posted today → a repost of the same job. */
function assertNotDuplicate(today: TodayPost[], card: Record<string, unknown>): void {
  const title = String(card[Jobs.TITLE]).toLowerCase();
  const lat = Number(card[Jobs.LAT]);
  const lng = Number(card[Jobs.LNG]);
  const duplicate = today.some((p) => p.status === Values.JobStatus.OPEN &&
    p.title === title && p.category === card[Jobs.CATEGORY] &&
    Math.abs(p.lat - lat) < 0.01 && Math.abs(p.lng - lng) < 0.01);
  if (duplicate) fail("already-exists", "You already posted this job today. Edit it or increase vacancies instead.");
}

async function ownedJob(tx: admin.firestore.Transaction, uid: string, jobId: string) {
  if (!/^[A-Za-z0-9_-]{6,64}$/.test(jobId)) fail("invalid-argument", "jobId is invalid");
  const ref = db.collection(Jobs.COLLECTION).doc(jobId);
  const snap = await tx.get(ref);
  if (!snap.exists) fail("not-found", "Job not found");
  if (snap.get(Jobs.EMPLOYER_ID) !== uid) fail("permission-denied", "Not your job");
  return { ref, snap };
}

// ───────────────────────────────── callables ─────────────────────────────────

export const postJob = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const reqId = requestId(data);
  const input = readJobInput(data);
  const urgency = oneOf(data, "urgency", URGENCIES, Values.Urgency.NORMAL);
  const jobId = jobIdFor(uid, reqId);
  const jobRef = db.collection(Jobs.COLLECTION).doc(jobId);
  const nowMs = Date.now();
  const today = await postsToday(uid, nowMs);

  const result = await db.runTransaction(async (tx) => {
    const existing = await tx.get(jobRef);
    if (existing.exists) return { jobId, replay: true };
    assertNotDuplicate(today, input.card);
    const employer = await loadEmployer(tx, uid);
    const quota = await tx.get(quotaRef(uid, nowMs));
    const charge = chargeForPost(tx, employer, quota, nowMs);
    const createdAt = Timestamp.fromMillis(nowMs);

    // Simple setup: the shop / business name and place typed while posting the first job fill in an
    // empty employer profile (no separate company-setup step).
    const businessName = str(data, "businessName", { max: 80, optional: true });
    const profileFill: Record<string, unknown> = {};
    if (businessName && !String(employer.data[EmployerProfiles.BUSINESS_NAME] || "").trim()) {
      profileFill[EmployerProfiles.BUSINESS_NAME] = businessName;
      profileFill[EmployerProfiles.EMPLOYER_TYPE] = Values.EmployerType.COMPANY;
    }
    if (!String(employer.data[EmployerProfiles.ADDRESS] || "").trim() && input.details[JobDetails.ADDRESS_TEXT]) {
      profileFill[EmployerProfiles.ADDRESS] = input.details[JobDetails.ADDRESS_TEXT];
      profileFill[EmployerProfiles.AREA] = input.card[Jobs.AREA];
      profileFill[EmployerProfiles.LAT] = input.card[Jobs.LAT];
      profileFill[EmployerProfiles.LNG] = input.card[Jobs.LNG];
    }
    if (Object.keys(profileFill).length) tx.update(employer.ref, profileFill);

    tx.create(jobRef, withoutDeletes({
      ...input.card,
      [Jobs.EMPLOYER_ID]: uid,
      [Jobs.COMPANY_NAME]: businessName || companyNameOf(employer.data),
      [Jobs.URGENCY]: urgency,
      [Jobs.STATUS]: Values.JobStatus.OPEN,
      [Jobs.APPLICATION_COUNT]: 0,
      [Jobs.CREATED_AT]: createdAt,
      [Jobs.EXPIRES_AT]: Timestamp.fromMillis(nowMs + JOB_LIFETIME_MS),
    }));
    tx.create(db.collection(JobDetails.COLLECTION).doc(jobId), {
      ...input.details,
      [JobDetails.EMPLOYER_ID]: uid,
    });
    tx.create(contactRef(jobId), {
      [JobContacts.EMPLOYER_ID]: uid,
      [JobContacts.CONTACT_NUMBER]: input.contactNumber,
    });
    tx.set(chargeRef(jobId), {
      [Idempotency.RESULT]: { charge, day: istDay(nowMs) },
      [Idempotency.EXPIRE_AT]: Timestamp.fromMillis(nowMs + DELETE_WINDOW_MS + DAY_MS),
    });
    return { jobId, replay: false, charge };
  });
  // A referred employer's reward is earned by their first real job post (one read when none is pending).
  if (!result.replay) await completeReferral(uid, Values.Role.EMPLOYER).catch((e) => functions.logger.warn("referral", e));
  return result;
});

export const updateJob = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const jobId = str(data, "jobId", { max: 64 });
  const input = readJobInput(data);
  await db.runTransaction(async (tx) => {
    const { ref, snap } = await ownedJob(tx, uid, jobId);
    const createdAt = (snap.get(Jobs.CREATED_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    if (Date.now() - createdAt > EDIT_WINDOW_MS) {
      fail("failed-precondition", "Jobs can only be edited within 48 hours of posting");
    }
    tx.update(ref, input.card as admin.firestore.DocumentData);
    tx.set(db.collection(JobDetails.COLLECTION).doc(jobId), input.details, { merge: true });
    tx.set(contactRef(jobId), {
      [JobContacts.EMPLOYER_ID]: uid,
      [JobContacts.CONTACT_NUMBER]: input.contactNumber,
    });
  });
  return { jobId };
});

export const setJobStatus = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const jobId = str(data, "jobId", { max: 64 });
  const status = oneOf(data, "status",
    [Values.JobStatus.OPEN, Values.JobStatus.FILLED, Values.JobStatus.CLOSED].map((s) => s.toUpperCase()));
  const next = status.toLowerCase();
  await db.runTransaction(async (tx) => {
    const { ref, snap } = await ownedJob(tx, uid, jobId);
    const current = snap.get(Jobs.STATUS);
    if (current === next) return;
    if (current === Values.JobStatus.EXPIRED) fail("failed-precondition", "This job has expired — renew it instead");
    tx.update(ref, { [Jobs.STATUS]: next });
  });
  return { jobId, status: next };
});

export const renewJob = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const jobId = str(obj(raw), "jobId", { max: 64 });
  const nowMs = Date.now();
  return db.runTransaction(async (tx) => {
    const { ref, snap } = await ownedJob(tx, uid, jobId);
    const expiresAt = (snap.get(Jobs.EXPIRES_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    if (snap.get(Jobs.STATUS) === Values.JobStatus.OPEN && expiresAt > nowMs) {
      fail("failed-precondition", "This job is still live");
    }
    const employer = await loadEmployer(tx, uid);
    const quota = await tx.get(quotaRef(uid, nowMs));
    const charge = chargeForPost(tx, employer, quota, nowMs);
    tx.update(ref, {
      [Jobs.STATUS]: Values.JobStatus.OPEN,
      [Jobs.EXPIRES_AT]: Timestamp.fromMillis(nowMs + JOB_LIFETIME_MS),
    });
    return { jobId, charge };
  });
});

export const deleteJob = onCallSecured({}, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const jobId = str(obj(raw), "jobId", { max: 64 });
  await db.runTransaction(async (tx) => {
    const { ref, snap } = await ownedJob(tx, uid, jobId);
    const createdAtMs = (snap.get(Jobs.CREATED_AT) as admin.firestore.Timestamp | undefined)?.toMillis() ?? 0;
    if (Date.now() - createdAtMs > DELETE_WINDOW_MS) {
      fail("failed-precondition", "Jobs can only be deleted within 30 minutes of posting. Close it instead.");
    }
    const chargeSnap = await tx.get(chargeRef(jobId));
    const paid = obj(chargeSnap.get(Idempotency.RESULT));
    if (paid.charge === "credit") {
      const S = EmployerProfiles.Subscription;
      tx.update(db.collection(EmployerProfiles.COLLECTION).doc(uid),
        `${EmployerProfiles.SUBSCRIPTION}.${S.CREDITS}.${S.CREDITS_NORMAL}`, FieldValue.increment(1));
    }
    if (paid.charge === "referral") refundReferralPost(tx, db.collection(EmployerProfiles.COLLECTION).doc(uid));
    if (paid.charge === "free") {
      tx.set(quotaRef(uid, createdAtMs), { [Idempotency.RESULT]: FieldValue.increment(-1) }, { merge: true });
    }
    tx.delete(ref);
    tx.delete(db.collection(JobDetails.COLLECTION).doc(jobId));
    tx.delete(contactRef(jobId));
    tx.delete(chargeRef(jobId));
  });
  return { jobId };
});

// ───────────────────────────── place + cleanup ─────────────────────────────

/**
 * Keeps every job's district / state in step with its point (posts, edits, admin-web jobs), so
 * the feed's district and state fallbacks can query by them. Writes only when a label is missing
 * or the point moved to another district; a status change writes nothing. Retried on failure.
 */
export const onJobWritten = functions
  .region("asia-south1")
  .runWith({ failurePolicy: true, memory: "512MB" })
  .firestore.document(`${Jobs.COLLECTION}/{jobId}`)
  .onWrite(async (change, context) => {
    if (Date.now() - Date.parse(context.timestamp) > EVENT_MAX_AGE_MS) return;
    const after = change.after.data();
    if (!after) {
      // Job deleted: its applications and contact have nothing left to point at.
      const jobId = context.params.jobId;
      const apps = await db.collection(Applications.COLLECTION).where(Applications.JOB_ID, "==", jobId).limit(500).get();
      const batch = db.batch();
      apps.docs.forEach((d) => batch.delete(d.ref));
      batch.delete(db.collection(JobContacts.COLLECTION).doc(jobId));
      await batch.commit();
      return;
    }
    const lat = Number(after[Jobs.LAT]);
    const lng = Number(after[Jobs.LNG]);
    const update: Record<string, unknown> = {};
    const cell = Number.isFinite(lat) && Number.isFinite(lng) ? encodeGeohash(lat, lng, 5) : "";
    if (cell && after[Jobs.CELL] !== cell) update[Jobs.CELL] = cell;
    const before = change.before.data();
    const moved = !before || before[Jobs.LAT] !== after[Jobs.LAT] || before[Jobs.LNG] !== after[Jobs.LNG];
    if (moved || after[Jobs.DISTRICT_ID] === undefined) {
      const place = placeOf(lat, lng);
      if (place && (after[Jobs.DISTRICT_ID] !== place.districtId || after[Jobs.STATE_ID] !== place.stateId)) {
        Object.assign(update, {
          [Jobs.DISTRICT_ID]: place.districtId,
          [Jobs.DISTRICT]: place.district,
          [Jobs.STATE_ID]: place.stateId,
          [Jobs.STATE]: place.state,
        });
      }
    }
    const district = (update[Jobs.DISTRICT] as string | undefined) ?? after[Jobs.DISTRICT];
    const keywords = keywordsOf(after[Jobs.TITLE], after[Jobs.COMPANY_NAME], after[Jobs.AREA], district,
      String(after[Jobs.CATEGORY] ?? "").replace(/_/g, " "));
    if (JSON.stringify(keywords) !== JSON.stringify(after[Jobs.KEYWORDS] ?? [])) update[Jobs.KEYWORDS] = keywords;
    if (Object.keys(update).length) await change.after.ref.update(update);
  });

export const expireJobs = functions
  .region("asia-south1")
  .pubsub.schedule("every 30 minutes")
  .timeZone("Asia/Kolkata")
  .onRun(async () => {
    const now = Timestamp.now();
    let expired = 0;
    for (;;) {
      const snap = await db.collection(Jobs.COLLECTION)
        .where(Jobs.STATUS, "==", Values.JobStatus.OPEN)
        .where(Jobs.EXPIRES_AT, "<=", now)
        .limit(400)
        .get();
      if (snap.empty) break;
      const batch = db.batch();
      snap.docs.forEach((d) => batch.update(d.ref, { [Jobs.STATUS]: Values.JobStatus.EXPIRED }));
      await batch.commit();
      expired += snap.size;
      if (snap.size < 400) break;
    }
    functions.logger.info(`expireJobs: ${expired} jobs expired`);
    return null;
  });
