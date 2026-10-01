import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

import { InstantRequests, Values } from "@/lib/firebase/schema";

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

async function getUncached(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    const col = db.collection(InstantRequests.COLLECTION);
    const S = Values.InstantStatus;
    // Totals are count aggregations (1 read per 1,000 docs), not document downloads.
    const count = async (status?: string) =>
      (await (status ? col.where(InstantRequests.STATUS, "==", status) : col).count().get()).data().count;
    const [recentSnap, totalRequests, openRequests, filled, completedRequests, cancelled, expiredRequests] = await Promise.all([
      col.orderBy(InstantRequests.CREATED_AT, "desc").limit(50).get(),
      count(), count(S.OPEN), count(S.FILLED), count(S.COMPLETED), count(S.CANCELLED), count(S.EXPIRED)
    ]);

    const recentRequests = recentSnap.docs.map((doc) => {
      const data = asRecord(doc.data());
      const pay = Number(data[InstantRequests.PAY_PER_PERSON] ?? 0);
      const needed = Number(data[InstantRequests.WORKERS_NEEDED] ?? 1);
      return {
        id: doc.id,
        title: String(data[InstantRequests.TITLE] ?? "Urgent request"),
        category: String(data[InstantRequests.CATEGORY] ?? ""),
        status: String(data[InstantRequests.STATUS] ?? "open"),
        employerName: String(data[InstantRequests.EMPLOYER_ID] ?? ""),
        employerPhone: String(data[InstantRequests.CONTACT_NUMBER] ?? ""),
        workersNeeded: needed,
        perPersonPayment: pay,
        totalPayment: pay * needed,
        durationText: String(data[InstantRequests.DURATION_TEXT] ?? ""),
        addressText: String(data[InstantRequests.ADDRESS_TEXT] ?? data[InstantRequests.AREA] ?? ""),
        scheduledAt: readMillis(data[InstantRequests.SCHEDULED_AT] as FirestoreValue),
        scheduleLabel: "",
        urgencyType: "",
        responseCount: Number(data[InstantRequests.RESPONSE_COUNT] ?? 0),
        callCount: 0,
        notifiedWorkerCount: 0,
        createdAt: readMillis(data[InstantRequests.CREATED_AT] as FirestoreValue),
        expiresAt: readMillis(data[InstantRequests.EXPIRES_AT] as FirestoreValue),
        firstResponseAt: null,
        completedAt: null,
        timeToFirstResponseMinutes: null
      };
    });
    const recentResponses = recentRequests.reduce((sum, r) => sum + r.responseCount, 0);
    const filledRequests = filled + completedRequests;

    return NextResponse.json({
      metrics: {
        totalRequests,
        openRequests,
        filledRequests,
        completedRequests,
        failedRequests: cancelled,
        expiredRequests,
        responseCount: recentResponses,
        avgResponsesPerRequest: recentRequests.length ? Number((recentResponses / recentRequests.length).toFixed(1)) : 0,
        filledRate: totalRequests ? Number(((filledRequests / totalRequests) * 100).toFixed(1)) : 0,
        expiredRate: totalRequests ? Number(((expiredRequests / totalRequests) * 100).toFixed(1)) : 0,
        avgTimeToFirstResponseMinutes: null
      },
      recentRequests,
      recentResponses: []
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load instant-help metrics.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}

// Read guard: served from a short-lived server cache; cleared on any admin write.
export const GET = cachedAdminGet(getUncached);
