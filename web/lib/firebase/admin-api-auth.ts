import { NextRequest, NextResponse } from "next/server";

import { getAdminAuthorization } from "@/lib/firebase/admin-access";
import { getAdminSession } from "@/lib/firebase/admin-session";
import { getFirebaseAdminAuth } from "@/lib/firebase/admin-server";

function readBearerToken(request: NextRequest) {
  const authHeader = request.headers.get("authorization")?.trim() ?? "";

  if (authHeader.toLowerCase().startsWith("bearer ")) {
    const token = authHeader.slice(7).trim();
    if (token) {
      return token;
    }
  }

  const fallback = request.headers.get("x-admin-id-token")?.trim() ?? "";
  return fallback || null;
}

async function isAuthorizedByBearerToken(request: NextRequest) {
  const token = readBearerToken(request);

  if (!token) {
    return false;
  }

  try {
    const decodedToken = await getFirebaseAdminAuth().verifyIdToken(token, true);
    return getAdminAuthorization(decodedToken).isAuthorized;
  } catch {
    return false;
  }
}

export async function requireAuthorizedAdminRequest(request: NextRequest) {
  const session = await getAdminSession();
  const authorized = Boolean(session) || await isAuthorizedByBearerToken(request);
  if (authorized) {
    // Any admin write makes cached admin reads stale: drop them so the next screen is fresh.
    if (request.method !== "GET" && request.method !== "HEAD") {
      const [{ invalidateAdminResponseCache }, { invalidateCollectionCache }] = await Promise.all([
        import("@/lib/firebase/admin-response-cache"),
        import("@/lib/firebase/admin-collection-cache")
      ]);
      invalidateAdminResponseCache();
      invalidateCollectionCache();
    }
    return null;
  }

  return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
}
