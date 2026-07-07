import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

function asRecord(value: unknown) {
  return (value ?? {}) as Record<string, unknown>;
}

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) return unauthorized;

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db
      .collection("job_call_sessions")
      .orderBy("createdAt", "desc")
      .limit(50)
      .get();

    const callFeedback = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...asRecord(doc.data())
    }));

    return NextResponse.json({ callFeedback });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load call feedback.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}