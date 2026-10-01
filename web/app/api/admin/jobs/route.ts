import { NextRequest, NextResponse } from "next/server";
import { FieldValue, Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { encodeGeohash, hasValidCoordinates } from "@/lib/firebase/geohash";
import { CATEGORY_KEYS, JobContacts, JobDetails, Jobs, Values } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const DAY_MS = 24 * 60 * 60 * 1000;
const JOB_LIFETIME_MS = 30 * DAY_MS;
const PAY_TYPES = Object.values(Values.PayType) as string[];
const EMPLOYMENT_TYPES = Object.values(Values.EmploymentType) as string[];
const SHIFTS = Object.values(Values.Shift) as string[];
const GENDERS = ["ANY", "MALE", "FEMALE"];
const STATUSES = Object.values(Values.JobStatus) as string[];

type Body = Record<string, unknown>;

function str(body: Body, key: string, max: number): string {
  const value = body[key];
  return typeof value === "string" ? value.trim().slice(0, max) : "";
}

function oneOf(body: Body, key: string, allowed: readonly string[], fallback: string): string {
  const value = String(body[key] ?? "").toUpperCase();
  return allowed.includes(value) ? value : fallback;
}

/** First comma part of an address, e.g. "Madhapur, Hyderabad" → "Madhapur". */
function areaOf(address: string): string {
  return address.split(",").map((p) => p.trim()).filter(Boolean)[0]?.slice(0, 60) ?? "";
}

function toJson(id: string, card: Record<string, unknown>, details: Record<string, unknown> = {}) {
  const plain = (v: unknown) => (v instanceof Timestamp ? v.toDate().toISOString() : v);
  return {
    id,
    ...Object.fromEntries(Object.entries(details).map(([k, v]) => [k, plain(v)])),
    ...Object.fromEntries(Object.entries(card).map(([k, v]) => [k, plain(v)]))
  };
}

/** Newest jobs, one page (≤ 100) with their details; ?status= filters, ?after= pages. */
async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    const status = params.get("status");
    let query = db.collection(Jobs.COLLECTION).orderBy(Jobs.CREATED_AT, "desc");
    if (status && STATUSES.includes(status)) query = db.collection(Jobs.COLLECTION).where(Jobs.STATUS, "==", status).orderBy(Jobs.CREATED_AT, "desc");
    const after = params.get("after");
    if (after) {
      const cursor = await db.collection(Jobs.COLLECTION).doc(after).get();
      if (cursor.exists) query = query.startAfter(cursor);
    }
    const snap = await query.limit(100).get();
    const details = snap.empty ? [] : await db.getAll(...snap.docs.map((d) => db.collection(JobDetails.COLLECTION).doc(d.id)));
    const jobs = snap.docs.map((d, i) => toJson(d.id, d.data(), details[i]?.data()));
    return NextResponse.json({ jobs, nextCursor: snap.size === 100 ? snap.docs.at(-1)!.id : null });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load jobs." }, { status: 500 });
  }
}

/** Admin-posted job: same card + details shape as the app's postJob (the cell index follows via trigger). */
export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: Body;
  try { body = (await request.json()) as Body; } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }

  const title = str(body, "title", 80);
  const companyName = str(body, "companyName", 100);
  const description = str(body, "description", 2000);
  const addressText = str(body, "addressText", 200);
  const contactNumber = String(body.contactNumber ?? "").replace(/\D/g, "").slice(-10);
  const payType = oneOf(body, "payType", PAY_TYPES, "MONTHLY");
  const payAmount = payType === "NEGOTIABLE" ? 0 : Math.round(Number(body.payAmount) || 0);
  const lat = Number(body.lat);
  const lng = Number(body.lng);
  const photoUrl = str(body, "photoUrl", 2000);

  if (title.length < 3) return NextResponse.json({ error: "Title must be at least 3 characters." }, { status: 400 });
  if (!companyName) return NextResponse.json({ error: "Company name is required." }, { status: 400 });
  if (description.length < 10) return NextResponse.json({ error: "Description must be at least 10 characters." }, { status: 400 });
  if (!/^[6-9]\d{9}$/.test(contactNumber)) return NextResponse.json({ error: "Enter a valid 10-digit mobile number." }, { status: 400 });
  if (addressText.length < 3) return NextResponse.json({ error: "Address is required." }, { status: 400 });
  if (payType !== "NEGOTIABLE" && payAmount < 1) return NextResponse.json({ error: "Enter the pay amount." }, { status: 400 });
  if (!hasValidCoordinates(lat, lng)) return NextResponse.json({ error: "Pick the job location on the map." }, { status: 400 });

  try {
    const db = getFirebaseAdminDb();
    const ref = db.collection(Jobs.COLLECTION).doc();
    const now = Date.now();
    const card: Record<string, unknown> = {
      [Jobs.EMPLOYER_ID]: "admin",
      [Jobs.TITLE]: title,
      [Jobs.CATEGORY]: oneOf(body, "category", CATEGORY_KEYS, "OTHER"),
      [Jobs.EMPLOYMENT_TYPE]: oneOf(body, "employmentType", EMPLOYMENT_TYPES, "FULL_TIME"),
      [Jobs.COMPANY_NAME]: companyName,
      [Jobs.PAY_AMOUNT]: payAmount,
      [Jobs.PAY_TYPE]: payType,
      [Jobs.VACANCIES]: Math.max(1, Math.min(50, Number(body.vacancies) || 1)),
      [Jobs.URGENCY]: Values.Urgency.NORMAL,
      [Jobs.SHIFT]: oneOf(body, "shift", SHIFTS, "ANY"),
      [Jobs.AREA]: areaOf(addressText),
      [Jobs.LAT]: lat,
      [Jobs.LNG]: lng,
      [Jobs.GEOHASH]: encodeGeohash(lat, lng, 9),
      // The feed's km bands query this ~5 km cell; district / state are added by the job trigger.
      [Jobs.CELL]: encodeGeohash(lat, lng, 5),
      [Jobs.STATUS]: Values.JobStatus.OPEN,
      [Jobs.APPLICATION_COUNT]: 0,
      [Jobs.CREATED_AT]: Timestamp.fromMillis(now),
      [Jobs.EXPIRES_AT]: Timestamp.fromMillis(now + JOB_LIFETIME_MS)
    };
    if (photoUrl) card[Jobs.PHOTO_URL] = photoUrl;
    const batch = db.batch();
    batch.set(ref, card);
    batch.set(db.collection(JobDetails.COLLECTION).doc(ref.id), {
      [JobDetails.EMPLOYER_ID]: "admin",
      [JobDetails.DESCRIPTION]: description,
      [JobDetails.ADDRESS_TEXT]: addressText,
      [JobDetails.GENDER]: oneOf(body, "gender", GENDERS, "ANY"),
      [JobDetails.EXPERIENCE_REQUIRED]: str(body, "experienceRequired", 60),
      [JobDetails.EDUCATION_REQUIRED]: str(body, "educationRequired", 60),
      [JobDetails.BENEFITS]: []
    });
    // The number is never public: workers get it from applyToJob(viaCall).
    batch.set(db.collection(JobContacts.COLLECTION).doc(ref.id), {
      [JobContacts.EMPLOYER_ID]: "admin",
      [JobContacts.CONTACT_NUMBER]: contactNumber
    });
    await batch.commit();
    return NextResponse.json({ ok: true, jobId: ref.id });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to create job." }, { status: 500 });
  }
}

/** Moderation edits: status, text fields, pay, vacancies, photo. */
export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: Body;
  try { body = (await request.json()) as Body; } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }
  const jobId = str(body, "jobId", 64);
  if (!jobId) return NextResponse.json({ error: "Missing jobId." }, { status: 400 });

  const card: Record<string, unknown> = {};
  const details: Record<string, unknown> = {};
  let contactNumber: string | null = null;
  if (body.title !== undefined) card[Jobs.TITLE] = str(body, "title", 80);
  if (body.companyName !== undefined) card[Jobs.COMPANY_NAME] = str(body, "companyName", 100);
  if (body.status !== undefined) {
    const status = String(body.status).toLowerCase();
    if (!STATUSES.includes(status)) return NextResponse.json({ error: "Invalid status." }, { status: 400 });
    card[Jobs.STATUS] = status;
    if (status === Values.JobStatus.OPEN) card[Jobs.EXPIRES_AT] = Timestamp.fromMillis(Date.now() + JOB_LIFETIME_MS);
  }
  if (body.payAmount !== undefined) card[Jobs.PAY_AMOUNT] = Math.max(0, Math.round(Number(body.payAmount) || 0));
  if (body.payType !== undefined) card[Jobs.PAY_TYPE] = oneOf(body, "payType", PAY_TYPES, "MONTHLY");
  if (body.vacancies !== undefined) card[Jobs.VACANCIES] = Math.max(1, Math.min(50, Number(body.vacancies) || 1));
  if (body.photoUrl !== undefined) card[Jobs.PHOTO_URL] = str(body, "photoUrl", 2000) || FieldValue.delete();
  if (body.description !== undefined) details[JobDetails.DESCRIPTION] = str(body, "description", 2000);
  if (body.contactNumber !== undefined) {
    contactNumber = String(body.contactNumber).replace(/\D/g, "").slice(-10);
    if (!/^[6-9]\d{9}$/.test(contactNumber)) return NextResponse.json({ error: "Enter a valid 10-digit mobile number." }, { status: 400 });
  }
  if (body.addressText !== undefined) {
    const address = str(body, "addressText", 200);
    details[JobDetails.ADDRESS_TEXT] = address;
    card[Jobs.AREA] = areaOf(address);
  }
  if (body.lat !== undefined && body.lng !== undefined) {
    const lat = Number(body.lat);
    const lng = Number(body.lng);
    if (hasValidCoordinates(lat, lng)) Object.assign(card, { [Jobs.LAT]: lat, [Jobs.LNG]: lng, [Jobs.GEOHASH]: encodeGeohash(lat, lng, 9), [Jobs.CELL]: encodeGeohash(lat, lng, 5) });
  }
  try {
    const db = getFirebaseAdminDb();
    const batch = db.batch();
    if (Object.keys(card).length) batch.update(db.collection(Jobs.COLLECTION).doc(jobId), card);
    if (Object.keys(details).length) batch.update(db.collection(JobDetails.COLLECTION).doc(jobId), details);
    if (contactNumber !== null) {
      batch.set(db.collection(JobContacts.COLLECTION).doc(jobId), { [JobContacts.CONTACT_NUMBER]: contactNumber }, { merge: true });
    }
    await batch.commit();
    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to update job." }, { status: 500 });
  }
}

/** Deletes card + details; the job trigger removes its applications and cell counts. */
export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: Body;
  try { body = (await request.json()) as Body; } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }
  const jobId = str(body, "jobId", 64);
  if (!jobId) return NextResponse.json({ error: "Missing jobId." }, { status: 400 });
  try {
    const db = getFirebaseAdminDb();
    const batch = db.batch();
    batch.delete(db.collection(Jobs.COLLECTION).doc(jobId));
    batch.delete(db.collection(JobDetails.COLLECTION).doc(jobId));
    await batch.commit();
    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to delete job." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
