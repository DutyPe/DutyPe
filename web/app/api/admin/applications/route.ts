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
  [key: string]: unknown;
};

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
    const snapshot = await db
      .collection("applications")
      .orderBy("createdAt", "desc")
      .limit(100)
      .get();

    const rawApplications: RawApplication[] = snapshot.docs.map((item) => ({
      id: item.id,
      ...asRecord(item.data())
    }));

    const workerIds = [
      ...new Set(
        rawApplications
          .map((entry) => (typeof entry.workerId === "string" ? entry.workerId : ""))
          .filter(Boolean)
      )
    ];

    const userById = new Map<string, NormalizedUser>();

    if (workerIds.length > 0) {
      const usersSnapshot = await db.collection("users").limit(1000).get();

      usersSnapshot.docs.forEach((userDoc) => {
        if (!workerIds.includes(userDoc.id)) {
          return;
        }

        const raw = asRecord(userDoc.data());
        userById.set(userDoc.id, normalizeUserRecord(userDoc.id, raw));
      });
    }

    const applications = rawApplications.map((entry) =>
      normalizeApplicationRecord(
        entry.id,
        entry,
        typeof entry.workerId === "string" ? userById.get(entry.workerId) : undefined
      )
    );

    return NextResponse.json({ applications });
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
    await db.collection("applications").doc(applicationId).set(
      {
        status: canonicalStatus,
        updatedAt: new Date()
      },
      { merge: true }
    );

    return NextResponse.json({ ok: true });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to update application status.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
