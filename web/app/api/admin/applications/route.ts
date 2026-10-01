import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { Applications, Jobs, Values } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const PAGE = 100;
const STATUSES = Object.values(Values.ApplicationStatus) as string[];

/** Newest applications (one page) with the job title joined from the job cards. */
async function getUncached(request: NextRequest) {
  try {
    const db = getFirebaseAdminDb();
    const params = new URL(request.url).searchParams;
    let query = db.collection(Applications.COLLECTION).orderBy(Applications.CREATED_AT, "desc");
    const after = params.get("after");
    if (after) {
      const cursor = await db.collection(Applications.COLLECTION).doc(after).get();
      if (cursor.exists) query = query.startAfter(cursor);
    }
    const snap = await query.limit(PAGE).get();
    const jobIds = Array.from(new Set(snap.docs.map((d) => String(d.get(Applications.JOB_ID) ?? "")))).filter(Boolean);
    const titles = new Map<string, string>();
    if (jobIds.length) {
      const cards = await db.getAll(...jobIds.map((id) => db.collection(Jobs.COLLECTION).doc(id)), { fieldMask: [Jobs.TITLE] });
      cards.forEach((c) => titles.set(c.id, String(c.get(Jobs.TITLE) ?? "")));
    }
    const applications = snap.docs.map((d) => {
      const created = d.get(Applications.CREATED_AT);
      return {
        id: d.id,
        jobId: String(d.get(Applications.JOB_ID) ?? ""),
        employerId: String(d.get(Applications.EMPLOYER_ID) ?? ""),
        workerId: String(d.get(Applications.WORKER_ID) ?? ""),
        workerName: String(d.get(Applications.WORKER_NAME) ?? ""),
        workerPhone: "",
        workerEmail: "",
        jobTitle: titles.get(String(d.get(Applications.JOB_ID))) || "(job removed)",
        status: String(d.get(Applications.STATUS) ?? "").toUpperCase(),
        callCount: Number(d.get(Applications.CALL_COUNT) ?? 0),
        appliedAt: created instanceof Timestamp ? created.toDate().toISOString() : null
      };
    });
    return NextResponse.json({ applications, nextCursor: snap.size === PAGE ? snap.docs.at(-1)!.id : null });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load applications." }, { status: 500 });
  }
}

/** Admin override of an application's status (support cases). Counters are not adjusted. */
export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;
  let body: { applicationId?: string; status?: string };
  try { body = await request.json(); } catch { return NextResponse.json({ error: "Invalid request body." }, { status: 400 }); }
  const applicationId = body.applicationId?.trim();
  const status = body.status?.trim().toLowerCase();
  if (!applicationId || !status || !STATUSES.includes(status)) {
    return NextResponse.json({ error: "Missing applicationId or invalid status." }, { status: 400 });
  }
  try {
    await getFirebaseAdminDb().collection(Applications.COLLECTION).doc(applicationId).update({
      [Applications.STATUS]: status,
      [Applications.UPDATED_AT]: Timestamp.now()
    });
    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to update status." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
