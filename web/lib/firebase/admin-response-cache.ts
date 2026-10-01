import "server-only";

import { NextRequest, NextResponse } from "next/server";

import { requireAuthorizedAdminRequest } from "@/lib/firebase/admin-api-auth";

/**
 * Read guard for admin list endpoints. Admin pages (especially the Dashboard, which
 * loads users + jobs + applications + referrals at once) re-read whole collections on
 * every refresh / login, costing thousands of billed reads each time. A successful GET
 * is kept for TTL_MS per URL; any admin write clears it (see requireAuthorizedAdminRequest).
 */
const TTL_MS = 5 * 60 * 1000;
const responses = new Map<string, { at: number; status: number; body: string; contentType: string }>();
const inFlight = new Map<string, Promise<NextResponse | Response>>();

type GetHandler = (request: NextRequest) => Promise<NextResponse | Response>;

export function cachedAdminGet(handler: GetHandler): GetHandler {
  return async (request: NextRequest) => {
    // Auth first: cached data is only ever served to an authorised admin.
    const unauthorized = await requireAuthorizedAdminRequest(request);
    if (unauthorized) return unauthorized;

    const url = new URL(request.url);
    const key = url.pathname + url.search;
    const hit = responses.get(key);
    if (hit && Date.now() - hit.at < TTL_MS) {
      return new NextResponse(hit.body, {
        status: hit.status,
        headers: { "content-type": hit.contentType, "x-admin-cache": "HIT" }
      });
    }

    // Concurrent identical requests share one Firestore read.
    let pending = inFlight.get(key);
    if (!pending) {
      pending = handler(request).finally(() => inFlight.delete(key));
      inFlight.set(key, pending);
    }
    const response = await pending;
    const body = await response.clone().text();
    const contentType = response.headers.get("content-type") ?? "application/json";
    if (response.status === 200) {
      responses.set(key, { at: Date.now(), status: 200, body, contentType });
    }
    return new NextResponse(body, {
      status: response.status,
      headers: { "content-type": contentType, "x-admin-cache": "MISS" }
    });
  };
}

export function invalidateAdminResponseCache() {
  responses.clear();
}
