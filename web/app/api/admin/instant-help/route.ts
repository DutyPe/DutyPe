import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

type FirestoreValue = { toDate?: () => Date } | Date | number | string | null | undefined;

function readMillis(value: FirestoreValue): number {
  if (!value) return 0;
  if (value instanceof Date) return value.getTime();
  if (typeof value === "number") return value;
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  if (typeof value.toDate === "function") return value.toDate().getTime();
  return 0;
}

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

function minutesBetween(startMs: number, endMs: number) {
  if (!startMs || !endMs || endMs < startMs) return null;
  return Math.round((endMs - startMs) / 60000);
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    const [requestsSnap, responsesSnap] = await Promise.all([
      db.collection("instant_requests").orderBy("createdAt", "desc").limit(300).get(),
      db.collection("instant_responses").orderBy("createdAt", "desc").limit(500).get()
    ]);

    const requests = requestsSnap.docs.map((doc) => {
      const data = asRecord(doc.data());
      const createdAt = readMillis(data.createdAt as FirestoreValue);
      const firstResponseAt = readMillis(data.firstResponseAt as FirestoreValue);
      const completedAt = readMillis(data.completedAt as FirestoreValue);
      const expiresAt = readMillis(data.expiresAt as FirestoreValue);
      return {
        id: doc.id,
        title: String(data.title ?? "Urgent request"),
        category: String(data.category ?? ""),
        status: String(data.status ?? "open").toLowerCase(),
        employerName: String(data.employerName ?? ""),
        responseCount: Number(data.responseCount ?? 0),
        callCount: Number(data.callCount ?? 0),
        notifiedWorkerCount: Number(data.notifiedWorkerCount ?? 0),
        createdAt,
        expiresAt,
        firstResponseAt,
        completedAt,
        timeToFirstResponseMinutes: minutesBetween(createdAt, firstResponseAt)
      };
    });

    const responses = responsesSnap.docs.map((doc) => {
      const data = asRecord(doc.data());
      return {
        id: doc.id,
        requestId: String(data.requestId ?? ""),
        workerId: String(data.workerId ?? ""),
        employerId: String(data.employerId ?? ""),
        workerName: String(data.workerName ?? "Worker"),
        status: String(data.status ?? "viewed").toLowerCase(),
        createdAt: readMillis(data.createdAt as FirestoreValue),
        updatedAt: readMillis(data.updatedAt as FirestoreValue)
      };
    });

    const totalRequests = requests.length;
    const openRequests = requests.filter((item) => item.status === "open").length;
    const filledRequests = requests.filter((item) => item.status === "filled" || item.status === "completed").length;
    const completedRequests = requests.filter((item) => item.status === "completed").length;
    const failedRequests = requests.filter((item) => item.status === "failed" || item.status === "cancelled").length;
    const expiredRequests = requests.filter((item) => item.status === "expired").length;
    const firstResponseTimes = requests
      .map((item) => item.timeToFirstResponseMinutes)
      .filter((value): value is number => typeof value === "number");
    const avgTimeToFirstResponseMinutes = firstResponseTimes.length
      ? Math.round(firstResponseTimes.reduce((sum, value) => sum + value, 0) / firstResponseTimes.length)
      : null;

    return NextResponse.json({
      metrics: {
        totalRequests,
        openRequests,
        filledRequests,
        completedRequests,
        failedRequests,
        expiredRequests,
        responseCount: responses.length,
        avgResponsesPerRequest: totalRequests ? Number((responses.length / totalRequests).toFixed(1)) : 0,
        filledRate: totalRequests ? Number(((filledRequests / totalRequests) * 100).toFixed(1)) : 0,
        expiredRate: totalRequests ? Number(((expiredRequests / totalRequests) * 100).toFixed(1)) : 0,
        avgTimeToFirstResponseMinutes
      },
      recentRequests: requests.slice(0, 50),
      recentResponses: responses.slice(0, 50)
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load instant-help metrics.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
