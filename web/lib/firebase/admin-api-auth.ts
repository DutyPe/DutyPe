import { NextRequest, NextResponse } from "next/server";

import { getAdminAuthorization } from "@/lib/firebase/admin-access";
import { getAdminSession } from "@/lib/firebase/admin-session";
import { getFirebaseAdminAuth } from "@/lib/firebase/admin-server";
import { checkAdminRateLimit, extractClientIp, RateLimitOptions } from "@/lib/security/admin-rate-limiter";

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

export type AdminActor = { uid: string; email: string };

export async function getAdminActor(request: NextRequest): Promise<AdminActor | null> {
  const session = await getAdminSession();
  if (session) return { uid: session.uid, email: session.email };
  return adminFromBearerToken(request);
}

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
  try {
    const { CosmosContainers, cosmosAdd } = await import("@/lib/azure/cosmos");
    await cosmosAdd(CosmosContainers.ADMIN_ACTIVITY, {
      uid: actor.uid,
      email: actor.email,
      method: request.method,
      path: url.pathname,
      query: url.search.slice(0, 300)
    });
  } catch {
    // Non-blocking fallback
  }
}

/**
 * Statutory KYC & Security Audit Log in Firestore:
 * Immutable records of admin inspections, data access, and moderation actions.
 */
export async function logAdminAudit(
  action: string,
  actor: AdminActor,
  details: Record<string, unknown> = {},
  request?: NextRequest
) {
  try {
    const { getFirebaseAdminDb } = await import("@/lib/firebase/admin-server");
    const { Timestamp } = await import("firebase-admin/firestore");
    const ip = request ? extractClientIp(request) : "internal";
    const userAgent = request?.headers.get("user-agent") ?? "";

    await getFirebaseAdminDb().collection("admin_audit_logs").add({
      action,
      adminUid: actor.uid,
      adminEmail: actor.email,
      timestamp: Timestamp.now(),
      ip,
      userAgent,
      ...details
    });
  } catch (err) {
    console.warn("Failed to write to admin_audit_logs:", err);
  }
}

/**
 * Validates admin authorization, throttles scrapers/attacks via rate limiting,
 * and maintains write invalidation & activity logs.
 */
export async function requireAuthorizedAdminRequest(
  request: NextRequest,
  options?: RateLimitOptions
) {
  const actor = await getAdminActor(request);
  if (!actor) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Sliding-window rate limit check per IP / Actor
  const rateLimit = checkAdminRateLimit(request, actor, options);
  if (!rateLimit.allowed) {
    return NextResponse.json(
      {
        error: `Rate limit exceeded. For anti-scraping and KYC protection, maximum ${rateLimit.limit} requests/min allowed. Please wait ${rateLimit.resetSeconds}s.`,
        retryAfter: rateLimit.resetSeconds
      },
      {
        status: 429,
        headers: {
          "Retry-After": String(rateLimit.resetSeconds),
          "X-RateLimit-Limit": String(rateLimit.limit),
          "X-RateLimit-Remaining": "0"
        }
      }
    );
  }

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
