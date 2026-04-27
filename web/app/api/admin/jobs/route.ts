import { NextRequest, NextResponse } from "next/server";
import { FieldValue, Timestamp } from "firebase-admin/firestore";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { encodeGeohash, hasValidCoordinates } from "@/lib/firebase/geohash";

export const runtime = "nodejs";

const VALID_URGENCY = new Set(["LOW", "MEDIUM", "HIGH"]);
const VALID_SALARY_TYPE = new Set(["HOURLY", "DAILY", "WEEKLY", "MONTHLY", "FIXED"]);
const VALID_GENDER = new Set(["Any", "Male", "Female"]);

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function extractCityFromAddress(address: string): string {
  if (!address) return "";
  const parts = address.split(",").map((p) => p.trim()).filter(Boolean);
  if (parts.length === 0) return "";
  if (parts.length >= 2) return parts[parts.length - 2];
  return parts[0];
}

function parseBenefits(input: unknown): string[] {
  if (Array.isArray(input)) {
    return input.map((i) => String(i).trim()).filter(Boolean).slice(0, 20);
  }
  if (typeof input === "string") {
    return input.split(/[,\n]/).map((p) => p.trim()).filter(Boolean).slice(0, 20);
  }
  return [];
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db
      .collection("jobmetadata")
      .orderBy("createdAt", "desc")
      .limit(500)
      .get();

    const ids = snapshot.docs.map((d) => d.id);
    const detailsById = new Map<string, Record<string, unknown>>();

    for (let i = 0; i < ids.length; i += 30) {
      const chunk = ids.slice(i, i + 30);
      if (chunk.length === 0) continue;
      const refs = chunk.map((id) => db.collection("job_details").doc(id));
      const detailsSnap = await db.getAll(...refs);
      detailsSnap.forEach((doc) => {
        if (doc.exists) detailsById.set(doc.id, asRecord(doc.data()));
      });
    }

    const jobs = snapshot.docs.map((item) => {
      const data = asRecord(item.data());
      const status = typeof data.status === "string" && data.status.trim() ? data.status : "open";
      const details = detailsById.get(item.id) ?? {};
      return {
        id: item.id,
        ...data,
        ...details,
        status,
        isActive: status.toLowerCase() === "open"
      };
    });

    return NextResponse.json({ jobs });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load jobs.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type CreateJobBody = {
  title?: string;
  companyName?: string;
  jobType?: string;
  jobImageUrl?: string;
  salary?: number | string;
  salaryType?: string;
  addressText?: string;
  latitude?: number | string;
  longitude?: number | string;
  urgency?: string;
  description?: string;
  contactNumber?: string;
  gender?: string;
  experienceRequired?: string;
  educationRequired?: string;
  workingHours?: string;
  vacancies?: number | string;
  benefits?: string[] | string;
  expiresInDays?: number;
  employerId?: string;
};

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  let body: CreateJobBody;
  try {
    body = (await request.json()) as CreateJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const title = body.title?.trim() ?? "";
  const companyName = body.companyName?.trim() ?? "";
  const addressText = body.addressText?.trim() ?? "";
  const description = body.description?.trim() ?? "";
  const contactNumber = body.contactNumber?.trim() ?? "";
  const workingHours = body.workingHours?.trim() || undefined;
  const experienceRequired = body.experienceRequired?.trim() || "No Experience Required";
  const educationRequired = body.educationRequired?.trim() || "No qualification required";
  const jobImageUrl = body.jobImageUrl?.trim() || undefined;

  const salary = String(body.salary ?? "").trim();
  const salaryTypeRaw = (body.salaryType?.trim() || "DAILY").toUpperCase();
  const salaryType = VALID_SALARY_TYPE.has(salaryTypeRaw) ? salaryTypeRaw : "DAILY";

  const urgencyRaw = (body.urgency?.trim() || "MEDIUM").toUpperCase();
  const urgency = VALID_URGENCY.has(urgencyRaw) ? urgencyRaw : "MEDIUM";

  const genderRaw = body.gender?.trim() || "Any";
  const gender = VALID_GENDER.has(genderRaw) ? genderRaw : "Any";

  const latitude = Number(body.latitude);
  const longitude = Number(body.longitude);

  const vacancies = Math.max(1, Math.min(1000, Number(body.vacancies) || 1));
  const benefits = parseBenefits(body.benefits);
  const expiresInDays = Math.max(1, Math.min(60, Number(body.expiresInDays) || 15));
  const employerId = body.employerId?.trim() || "admin";

  if (!title || title.length < 3 || title.length > 120) {
    return NextResponse.json({ error: "Title must be 3-120 characters." }, { status: 400 });
  }
  if (!companyName) return NextResponse.json({ error: "Company name is required." }, { status: 400 });
  if (!description) return NextResponse.json({ error: "Description is required." }, { status: 400 });
  if (!contactNumber) return NextResponse.json({ error: "Contact number is required." }, { status: 400 });
  if (!addressText) return NextResponse.json({ error: "Address is required." }, { status: 400 });
  if (!salary || salary.length > 60) {
    return NextResponse.json({ error: "Salary is required and must be 60 characters or less." }, { status: 400 });
  }
  if (jobImageUrl && jobImageUrl.length > 2000) {
    return NextResponse.json({ error: "Job image URL is too long." }, { status: 400 });
  }
  if (!hasValidCoordinates(latitude, longitude)) {
    return NextResponse.json(
      { error: "Latitude and longitude are required (use the geocode helper or paste real coordinates)." },
      { status: 400 }
    );
  }

  try {
    const db = getFirebaseAdminDb();
    const jobRef = db.collection("jobmetadata").doc();
    const now = new Date();
    const expiresAt = new Date(now.getTime() + expiresInDays * 24 * 60 * 60 * 1000);
    const geohash = encodeGeohash(latitude, longitude, 6);
    const companyCity = extractCityFromAddress(addressText);

    // Ensure the employer_profiles/{employerId} doc exists so worker-side
    // EmployerProfileCache + JobListing display companyName/isVerified properly.
    // Idempotent: merge:true only writes the doc if absent or fills missing fields.
    await db.collection("employer_profiles").doc(employerId).set(
      {
        userId: employerId,
        companyName,
        isVerified: true,
        lastActiveAt: Timestamp.fromDate(now)
      },
      { merge: true }
    );

    const cardData: Record<string, unknown> = {
      employerId,
      companyName,
      title,
      salary,
      salaryType,
      location: { lat: latitude, lng: longitude },
      geohash,
      addressText,
      urgency,
      status: "open",
      createdAt: Timestamp.fromDate(now),
      vacancies
    };
    if (jobImageUrl) {
      cardData.jobImageUrl = jobImageUrl;
    }

    const detailsData: Record<string, unknown> = {
      employerId,
      expiresAt: Timestamp.fromDate(expiresAt),
      description,
      contactNumber,
      gender,
      experienceRequired,
      educationRequired,
      companyCity,
      benefits,
      applicationCount: 0
    };
    if (workingHours) detailsData.workingHours = workingHours;

    const batch = db.batch();
    batch.set(jobRef, cardData);
    batch.set(db.collection("job_details").doc(jobRef.id), detailsData);
    await batch.commit();

    return NextResponse.json({ ok: true, jobId: jobRef.id });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateJobBody = {
  jobId?: string;
  title?: string;
  companyName?: string;
  jobType?: string;
  jobImageUrl?: string;
  salary?: number | string;
  salaryType?: string;
  addressText?: string;
  latitude?: number | string;
  longitude?: number | string;
  urgency?: string;
  status?: string;
  description?: string;
  contactNumber?: string;
  gender?: string;
  experienceRequired?: string;
  educationRequired?: string;
  workingHours?: string;
  vacancies?: number | string;
  benefits?: string[] | string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  let body: UpdateJobBody;
  try {
    body = (await request.json()) as UpdateJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const jobId = body.jobId?.trim();
  if (!jobId) return NextResponse.json({ error: "Missing jobId." }, { status: 400 });

  const cardPayload: Record<string, unknown> = {};
  const detailsPayload: Record<string, unknown> = {};
  const legacyMetadataFields = [
    "jobType",
    "description",
    "benefits",
    "gender",
    "experienceRequired",
    "educationRequired",
    "companyCity",
    "isVerified"
  ];

  legacyMetadataFields.forEach((field) => {
    cardPayload[field] = FieldValue.delete();
  });

  if (body.title !== undefined) cardPayload.title = String(body.title).trim();
  if (body.companyName !== undefined) cardPayload.companyName = String(body.companyName).trim();
  if (body.jobImageUrl !== undefined) {
    const imageUrl = String(body.jobImageUrl).trim();
    cardPayload.jobImageUrl = imageUrl || FieldValue.delete();
  }
  if (body.salary !== undefined) {
    const v = String(body.salary).trim();
    if (v.length > 0 && v.length <= 60) cardPayload.salary = v;
  }
  if (body.salaryType !== undefined) {
    const v = String(body.salaryType).trim().toUpperCase();
    if (VALID_SALARY_TYPE.has(v)) cardPayload.salaryType = v;
  }
  if (body.addressText !== undefined) {
    const a = String(body.addressText).trim();
    cardPayload.addressText = a;
    detailsPayload.companyCity = extractCityFromAddress(a);
  }
  if (body.latitude !== undefined && body.longitude !== undefined) {
    const lat = Number(body.latitude);
    const lng = Number(body.longitude);
    if (hasValidCoordinates(lat, lng)) {
      cardPayload.location = { lat, lng };
      cardPayload.geohash = encodeGeohash(lat, lng, 6);
    }
  }
  if (body.urgency !== undefined) {
    const v = String(body.urgency).trim().toUpperCase();
    if (VALID_URGENCY.has(v)) cardPayload.urgency = v;
  }
  if (body.status !== undefined) {
    const v = String(body.status).trim().toLowerCase();
    cardPayload.status = ["open", "closed", "expired"].includes(v) ? v : "open";
  }

  if (body.description !== undefined) detailsPayload.description = String(body.description).trim();
  if (body.contactNumber !== undefined) detailsPayload.contactNumber = String(body.contactNumber).trim();
  if (body.workingHours !== undefined) detailsPayload.workingHours = String(body.workingHours).trim();
  if (body.experienceRequired !== undefined) detailsPayload.experienceRequired = String(body.experienceRequired).trim();
  if (body.educationRequired !== undefined) detailsPayload.educationRequired = String(body.educationRequired).trim();
  if (body.gender !== undefined) {
    const v = String(body.gender).trim();
    detailsPayload.gender = VALID_GENDER.has(v) ? v : "Any";
  }
  if (body.vacancies !== undefined) {
    const vacancies = Math.max(1, Math.min(1000, Number(body.vacancies) || 1));
    cardPayload.vacancies = vacancies;
  }
  if (body.benefits !== undefined) {
    detailsPayload.benefits = parseBenefits(body.benefits);
  }

  try {
    const db = getFirebaseAdminDb();
    const batch = db.batch();
    if (Object.keys(cardPayload).length > 0) {
      batch.set(db.collection("jobmetadata").doc(jobId), cardPayload, { merge: true });
    }
    if (Object.keys(detailsPayload).length > 0) {
      batch.set(db.collection("job_details").doc(jobId), detailsPayload, { merge: true });
    }
    await batch.commit();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type DeleteJobBody = { jobId?: string };

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  let body: DeleteJobBody;
  try {
    body = (await request.json()) as DeleteJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const jobId = body.jobId?.trim();
  if (!jobId) return NextResponse.json({ error: "Missing jobId." }, { status: 400 });

  try {
    const db = getFirebaseAdminDb();
    const batch = db.batch();
    batch.delete(db.collection("jobmetadata").doc(jobId));
    batch.delete(db.collection("job_details").doc(jobId));
    await batch.commit();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
