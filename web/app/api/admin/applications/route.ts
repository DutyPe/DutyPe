import { NextRequest, NextResponse } from "next/server";

import {
  normalizeApplicationRecord,
  normalizeUserRecord,
  toCanonicalApplicationStatus,
  type NormalizedUser
} from "@/lib/firebase/admin-normalizers";
import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type RawApplication = {
  id: string;
  workerId?: unknown;
  jobId?: unknown;
  jobTitle?: unknown;
  title?: unknown;
  workerName?: unknown;
  applicantName?: unknown;
  workerPhone?: unknown;
  appliedAt?: unknown;
  createdAt?: unknown;
  timestamp?: unknown;
  updatedAt?: unknown;
  created_at?: unknown;
  date?: unknown;
  [key: string]: unknown;
};

function asRecord(value: unknown): Record<string, unknown> {
  return (value ?? {}) as Record<string, unknown>;
}

function parseTimestampMs(value: unknown): number {
  if (!value) return 0;
  if (typeof value === "number") return value;
  if (value instanceof Date) return value.getTime();
  if (typeof value === "object" && value !== null) {
    if ("toMillis" in value && typeof (value as { toMillis?: () => number }).toMillis === "function") {
      return (value as { toMillis: () => number }).toMillis();
    }
    if ("_seconds" in value && typeof (value as { _seconds?: number })._seconds === "number") {
      return (value as { _seconds: number })._seconds * 1000;
    }
    if ("seconds" in value && typeof (value as { seconds?: number }).seconds === "number") {
      return (value as { seconds: number }).seconds * 1000;
    }
  }
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    if (!isNaN(parsed)) return parsed;
  }
  return 0;
}

function getApplicationSortTime(entry: RawApplication): number {
  return (
    parseTimestampMs(entry.appliedAt) ||
    parseTimestampMs(entry.createdAt) ||
    parseTimestampMs(entry.timestamp) ||
    parseTimestampMs(entry.updatedAt) ||
    parseTimestampMs(entry.created_at) ||
    parseTimestampMs(entry.date) ||
    0
  );
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const url = new URL(request.url);
    const requestedLimit = parseInt(url.searchParams.get("limit") || "5000", 10);
    const fetchLimit = Number.isFinite(requestedLimit) && requestedLimit > 0 ? Math.min(requestedLimit, 10000) : 5000;

    // Fetch from both 'applications' and 'job_applications' collections without
    // restrictive orderBy clauses that omit documents missing a specific timestamp key.
    const [appsSnapshot, jobAppsSnapshot] = await Promise.all([
      db.collection("applications").limit(fetchLimit).get().catch(() => ({ docs: [] })),
      db.collection("job_applications").limit(fetchLimit).get().catch(() => ({ docs: [] }))
    ]);

    // Merge by document ID so applications that exist in either or both collections are unified
    const appMap = new Map<string, RawApplication>();

    for (const doc of jobAppsSnapshot.docs) {
      appMap.set(doc.id, {
        id: doc.id,
        ...asRecord(doc.data())
      });
    }

    for (const doc of appsSnapshot.docs) {
      const existing = appMap.get(doc.id) ?? { id: doc.id };
      appMap.set(doc.id, {
        ...existing,
        ...asRecord(doc.data())
      });
    }

    const rawApplications: RawApplication[] = Array.from(appMap.values());

    // Sort descending by date (past, current, everything)
    rawApplications.sort((a, b) => getApplicationSortTime(b) - getApplicationSortTime(a));

    // Efficiently enrich missing worker details in batches
    const missingWorkerIds = [
      ...new Set(
        rawApplications
          .filter((entry) => {
            const hasName = typeof entry.workerName === "string" && entry.workerName.trim().length > 0;
            const hasApplicantName = typeof entry.applicantName === "string" && entry.applicantName.trim().length > 0;
            const hasPhone = typeof entry.workerPhone === "string" && entry.workerPhone.trim().length > 0;
            return (!hasName && !hasApplicantName) || !hasPhone;
          })
          .map((entry) => (typeof entry.workerId === "string" ? entry.workerId.trim() : ""))
          .filter(Boolean)
      )
    ];

    const userById = new Map<string, NormalizedUser>();

    // Batch load up to 600 missing user profiles in chunks of 30 using db.getAll
    const userIdsToFetch = missingWorkerIds.slice(0, 600);
    for (let i = 0; i < userIdsToFetch.length; i += 30) {
      const chunk = userIdsToFetch.slice(i, i + 30);
      if (chunk.length === 0) continue;
      const refs = chunk.map((id) => db.collection("users").doc(id));
      try {
        const snapshots = await db.getAll(...refs);
        snapshots.forEach((docSnap) => {
          if (docSnap.exists) {
            userById.set(docSnap.id, normalizeUserRecord(docSnap.id, asRecord(docSnap.data())));
          }
        });
      } catch {
        // Continue if a batch lookup encounters an issue
      }
    }

    // Enrich missing job titles if jobId exists
    const missingJobIds = [
      ...new Set(
        rawApplications
          .filter((entry) => {
            const hasJobTitle = typeof entry.jobTitle === "string" && entry.jobTitle.trim().length > 0;
            const hasTitle = typeof entry.title === "string" && entry.title.trim().length > 0;
            return !hasJobTitle && !hasTitle && typeof entry.jobId === "string" && entry.jobId.trim().length > 0;
          })
          .map((entry) => String(entry.jobId).trim())
          .filter(Boolean)
      )
    ];

    const jobTitleById = new Map<string, string>();
    const jobIdsToFetch = missingJobIds.slice(0, 300);
    for (let i = 0; i < jobIdsToFetch.length; i += 30) {
      const chunk = jobIdsToFetch.slice(i, i + 30);
      if (chunk.length === 0) continue;
      const refs = chunk.map((id) => db.collection("jobmetadata").doc(id));
      try {
        const snapshots = await db.getAll(...refs);
        snapshots.forEach((docSnap) => {
          if (docSnap.exists) {
            const data = asRecord(docSnap.data());
            const title = typeof data.title === "string" ? data.title.trim() : "";
            if (title) jobTitleById.set(docSnap.id, title);
          }
        });
      } catch {
        // Continue if jobmetadata lookup fails
      }
    }

    const applications = rawApplications.map((entry) => {
      const worker = typeof entry.workerId === "string" ? userById.get(entry.workerId.trim()) : undefined;
      const enrichedEntry = { ...entry };
      if (
        !enrichedEntry.jobTitle &&
        !enrichedEntry.title &&
        typeof enrichedEntry.jobId === "string" &&
        jobTitleById.has(enrichedEntry.jobId.trim())
      ) {
        enrichedEntry.jobTitle = jobTitleById.get(enrichedEntry.jobId.trim());
      }
      return normalizeApplicationRecord(entry.id, enrichedEntry, worker);
    });

    return NextResponse.json({
      applications,
      total: applications.length
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load applications.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

type UpdateStatusBody = {
  applicationId?: string;
  status?: string;
};

export async function PATCH(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  let body: UpdateStatusBody;

  try {
    body = (await request.json()) as UpdateStatusBody;
  } catch {
    return NextResponse.json({ error: "Invalid request body." }, { status: 400 });
  }

  const applicationId = body.applicationId?.trim();
  const status = body.status?.trim();

  if (!applicationId || !status) {
    return NextResponse.json({ error: "Missing applicationId or status." }, { status: 400 });
  }

  const canonicalStatus = toCanonicalApplicationStatus(status);

  try {
    const db = getFirebaseAdminDb();
    const updatePayload = {
      status: canonicalStatus,
      updatedAt: new Date()
    };

    const appRef = db.collection("applications").doc(applicationId);
    const jobAppRef = db.collection("job_applications").doc(applicationId);

    const [appDoc, jobAppDoc] = await Promise.all([
      appRef.get().catch(() => null),
      jobAppRef.get().catch(() => null)
    ]);

    const updateOps: Promise<unknown>[] = [];

    if (appDoc && appDoc.exists) {
      updateOps.push(appRef.set(updatePayload, { merge: true }));
    }
    if (jobAppDoc && jobAppDoc.exists) {
      updateOps.push(jobAppRef.set(updatePayload, { merge: true }));
    }
    if ((!appDoc || !appDoc.exists) && (!jobAppDoc || !jobAppDoc.exists)) {
      updateOps.push(appRef.set(updatePayload, { merge: true }));
    }

    await Promise.all(updateOps);

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update application status.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
