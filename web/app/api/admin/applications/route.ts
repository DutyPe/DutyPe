import { NextRequest, NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { Applications, Jobs, Values, WorkerProfiles } from "@/lib/firebase/schema";

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
    const workerIds = Array.from(new Set(snap.docs.map((d) => String(d.get(Applications.WORKER_ID) ?? "")))).filter(Boolean);
    const titles = new Map<string, string>();
    const workerPhones = new Map<string, string>();
    const workerEmails = new Map<string, string>();
    if (jobIds.length) {
      const cards = await db.getAll(...jobIds.map((id) => db.collection(Jobs.COLLECTION).doc(id)), { fieldMask: [Jobs.TITLE] });
      cards.forEach((c) => titles.set(c.id, String(c.get(Jobs.TITLE) ?? "")));
    }
    if (workerIds.length) {
      const workers = await db.getAll(...workerIds.map((id) => db.collection(WorkerProfiles.COLLECTION).doc(id)), { fieldMask: [WorkerProfiles.PHONE, WorkerProfiles.EMAIL] });
      workers.forEach((w) => {
        if (w.exists) {
          workerPhones.set(w.id, String(w.get(WorkerProfiles.PHONE) ?? ""));
          workerEmails.set(w.id, String(w.get(WorkerProfiles.EMAIL) ?? ""));
        }
      });
    }
    const applications = snap.docs.map((d) => {
      const created = d.get(Applications.CREATED_AT);
      const wId = String(d.get(Applications.WORKER_ID) ?? "");
      return {
        id: d.id,
        sourceType: "STANDARD",
        jobId: String(d.get(Applications.JOB_ID) ?? ""),
        employerId: String(d.get(Applications.EMPLOYER_ID) ?? ""),
        workerId: wId,
        workerName: String(d.get(Applications.WORKER_NAME) ?? ""),
        workerPhone: workerPhones.get(wId) || "",
        workerEmail: workerEmails.get(wId) || "",
        jobTitle: titles.get(String(d.get(Applications.JOB_ID))) || "(job removed)",
        status: String(d.get(Applications.STATUS) ?? "").toUpperCase(),
        callCount: Number(d.get(Applications.CALL_COUNT) ?? 0),
        appliedAt: created instanceof Timestamp ? created.toDate().toISOString() : null
      };
    });

    // Also fetch instant job requests & applicant responses
    const instantSnap = await db.collection("instant_requests")
      .orderBy("createdAt", "desc")
      .limit(60)
      .get()
      .catch(() => null);

    const instantItems: typeof applications = [];
    if (instantSnap && !instantSnap.empty) {
      for (const d of instantSnap.docs) {
        const created = d.get("createdAt");
        const title = String(d.get("title") || d.get("category") || "Instant Work");
        const pay = d.get("payPerPerson") ? `₹${d.get("payPerPerson")}` : "";
        const workersNeeded = d.get("workersNeeded") ? `(${d.get("workersNeeded")} needed)` : "";
        const area = String(d.get("area") || d.get("addressText") || "");

        const responsesSnap = await d.ref.collection("responses").limit(5).get().catch(() => null);
        if (responsesSnap && !responsesSnap.empty) {
          for (const resp of responsesSnap.docs) {
            const respCreated = resp.get("createdAt") || created;
            instantItems.push({
              id: `${d.id}_${resp.id}`,
              sourceType: "INSTANT",
              jobId: d.id,
              employerId: String(d.get("employerId") ?? ""),
              workerId: resp.id,
              workerName: String(resp.get("workerName") || "Worker Applicant"),
              workerPhone: String(resp.get("workerPhone") || d.get("contactNumber") || ""),
              workerEmail: "",
              jobTitle: `[⚡ Instant] ${title} ${pay} ${area ? `• ${area}` : ""}`.trim(),
              status: String(resp.get("status") || d.get("status") || "APPLIED").toUpperCase(),
              callCount: Number(d.get("responseCount") ?? 0),
              appliedAt: respCreated instanceof Timestamp ? respCreated.toDate().toISOString() : null
            });
          }
        } else {
          instantItems.push({
            id: d.id,
            sourceType: "INSTANT",
            jobId: d.id,
            employerId: String(d.get("employerId") ?? ""),
            workerId: "",
            workerName: `Instant Hiring ${workersNeeded}`.trim(),
            workerPhone: String(d.get("contactNumber") || ""),
            workerEmail: "",
            jobTitle: `[⚡ Instant] ${title} ${pay} ${area ? `• ${area}` : ""}`.trim(),
            status: String(d.get("status") || "OPEN").toUpperCase(),
            callCount: Number(d.get("responseCount") ?? 0),
            appliedAt: created instanceof Timestamp ? created.toDate().toISOString() : null
          });
        }
      }
    }

    const allApplications = [...instantItems, ...applications].sort((a, b) => {
      const timeA = a.appliedAt ? new Date(a.appliedAt).getTime() : 0;
      const timeB = b.appliedAt ? new Date(b.appliedAt).getTime() : 0;
      return timeB - timeA;
    });

    return NextResponse.json({ applications: allApplications, nextCursor: snap.size === PAGE ? snap.docs.at(-1)!.id : null });
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
    if (applicationId.includes("_")) {
      const [reqId, respId] = applicationId.split("_");
      await getFirebaseAdminDb().collection("instant_requests").doc(reqId).collection("responses").doc(respId).update({
        status,
        updatedAt: Timestamp.now()
      }).catch(() => null);
    } else {
      const appRef = getFirebaseAdminDb().collection(Applications.COLLECTION).doc(applicationId);
      const appDoc = await appRef.get();
      if (appDoc.exists) {
        await appRef.update({
          [Applications.STATUS]: status,
          [Applications.UPDATED_AT]: Timestamp.now()
        });
      } else {
        await getFirebaseAdminDb().collection("instant_requests").doc(applicationId).update({
          status,
          updatedAt: Timestamp.now()
        }).catch(() => null);
      }
    }
    return NextResponse.json({ ok: true });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to update status." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
