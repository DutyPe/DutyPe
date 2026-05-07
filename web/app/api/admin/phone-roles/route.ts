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
    const snapshot = await db.collection("phoneRoles").limit(5000).get();

    const items = snapshot.docs.map((doc) => {
      const { updatedAt: _updatedAt, ...data } = doc.data() as Record<string, unknown>;
      return {
        id: doc.id,
        ...data
      };
    });

    return NextResponse.json({ items });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Failed to load phone roles.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}