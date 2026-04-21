import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  const unauthorized = await requireAuthorizedAdminRequest(request);
  if (unauthorized) {
    return unauthorized;
  }

  try {
    const db = getFirebaseAdminDb();
    const snapshot = await db.collection("worker_profiles").limit(2000).get();

    const profiles = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...(doc.data() as Record<string, unknown>)
    }));

    return NextResponse.json({ profiles });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load worker profiles.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
