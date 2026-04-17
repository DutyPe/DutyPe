import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db.collection("jobmetadata").orderBy("createdAt", "desc").limit(500).get();

    const jobs = snapshot.docs.map((item) => {
      const data = asRecord(item.data());
      const status = typeof data.status === "string" && data.status.trim() ? data.status : "open";

      return {
        id: item.id,
        ...data,
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
  location?: string;
  description?: string;
  category?: string;
  payAmount?: string;
  payType?: string;
  shift?: string;
  vacancies?: number;
  requirements?: string;
  contactPhone?: string;
  contactEmail?: string;
};

export async function POST(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: CreateJobBody;

  try {
    body = (await request.json()) as CreateJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const title = body.title?.trim() ?? "";
  const companyName = body.companyName?.trim() ?? "";
  const location = body.location?.trim() ?? "";

  if (!title || !companyName || !location) {
    return NextResponse.json(
      { error: "Title, companyName and location are required." },
      { status: 400 }
    );
  }

  try {
    const db = getFirebaseAdminDb();
    const created = await db.collection("jobmetadata").add({
      title,
      companyName,
      location,
      description: body.description?.trim() ?? "",
      category: body.category?.trim() || "Other",
      payAmount: body.payAmount?.trim() ?? "",
      payType: body.payType?.trim() || "Monthly",
      shift: body.shift?.trim() || "Day Shift",
      vacancies: Number(body.vacancies) || 1,
      requirements: body.requirements?.trim() ?? "",
      contactPhone: body.contactPhone?.trim() ?? "",
      contactEmail: body.contactEmail?.trim() ?? "",
      status: "open",
      isActive: true,
      applicationCount: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
      postedBy: "admin",
      source: "admin-console"
    });

    return NextResponse.json({ ok: true, jobId: created.id });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to create job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateJobBody = {
  jobId?: string;
  title?: string;
  companyName?: string;
  location?: string;
  payAmount?: string;
  vacancies?: number;
  description?: string;
  category?: string;
  shift?: string;
  status?: string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateJobBody;

  try {
    body = (await request.json()) as UpdateJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const jobId = body.jobId?.trim();
  if (!jobId) {
    return NextResponse.json({ error: "Missing jobId." }, { status: 400 });
  }

  const payload: Record<string, unknown> = {
    updatedAt: new Date()
  };

  if (body.title !== undefined) payload.title = String(body.title).trim();
  if (body.companyName !== undefined) payload.companyName = String(body.companyName).trim();
  if (body.location !== undefined) payload.location = String(body.location).trim();
  if (body.payAmount !== undefined) payload.payAmount = String(body.payAmount).trim();
  if (body.vacancies !== undefined) payload.vacancies = Number(body.vacancies) || 0;
  if (body.description !== undefined) payload.description = String(body.description).trim();
  if (body.category !== undefined) payload.category = String(body.category).trim();
  if (body.shift !== undefined) payload.shift = String(body.shift).trim();
  if (body.status !== undefined) {
    const normalizedStatus = String(body.status).trim().toLowerCase() === "open" ? "open" : "closed";
    payload.status = normalizedStatus;
    payload.isActive = normalizedStatus === "open";
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("jobmetadata").doc(jobId).set(payload, { merge: true });
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type DeleteJobBody = {
  jobId?: string;
};

export async function DELETE(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: DeleteJobBody;

  try {
    body = (await request.json()) as DeleteJobBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const jobId = body.jobId?.trim();
  if (!jobId) {
    return NextResponse.json({ error: "Missing jobId." }, { status: 400 });
  }

  try {
    const db = getFirebaseAdminDb();
    await db.collection("jobmetadata").doc(jobId).delete();
    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to delete job.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
