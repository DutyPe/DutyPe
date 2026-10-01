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

type AdminActor = { uid: string; email: string };

async function adminFromBearerToken(request: NextRequest): Promise<AdminActor | null> {
  const token = readBearerToken(request);

  if (!token) {
    return null;
  }

  try {
    const decodedToken = await getFirebaseAdminAuth().verifyIdToken(token, true);
    return getAdminAuthorization(decodedToken).isAuthorized
      ? { uid: decodedToken.uid, email: decodedToken.email ?? "" }
      : null;
  } catch {
    return null;
  }
}

/** Every admin write is recorded in Azure Cosmos DB (container "admin_activity"): who, what, when. */
async function logAdminActivity(request: NextRequest, actor: AdminActor) {
  const url = new URL(request.url);
  const { CosmosContainers, cosmosAdd } = await import("@/lib/azure/cosmos");
  await cosmosAdd(CosmosContainers.ADMIN_ACTIVITY, {
    uid: actor.uid,
    email: actor.email,
    method: request.method,
    path: url.pathname,
    query: url.search.slice(0, 300)
  });
}

export async function requireAuthorizedAdminRequest(request: NextRequest) {
  const session = await getAdminSession();
  const actor: AdminActor | null = session ? { uid: session.uid, email: session.email } : await adminFromBearerToken(request);
  if (actor) {
    // Any admin write makes cached admin reads stale: drop them so the next screen is fresh.
    if (request.method !== "GET" && request.method !== "HEAD") {
      const [{ invalidateAdminResponseCache }, { invalidateCollectionCache }] = await Promise.all([
        import("@/lib/firebase/admin-response-cache"),
        import("@/lib/firebase/admin-collection-cache")
      ]);
      invalidateAdminResponseCache();
      invalidateCollectionCache();
      await logAdminActivity(request, actor);
    }
    return null;
  }

  return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
}
