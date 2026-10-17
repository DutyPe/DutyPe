import { NextRequest, NextResponse } from "next/server";

import {
  ADMIN_SESSION_COOKIE_NAME,
  createAdminSessionCookie,
  getAdminSessionCookieOptions
} from "@/lib/firebase/admin-session";
import { isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";
import { checkIpRateLimit, RateLimitTiers } from "@/lib/security/admin-rate-limiter";

export const runtime = "nodejs";

type SessionRequestBody = {
  idToken?: string;
};

export async function POST(request: NextRequest) {
  // IP-level rate limit to prevent token flood and CPU exhaustion
  const rateLimit = checkIpRateLimit(request, RateLimitTiers.AUTH_SESSION);
  if (!rateLimit.allowed) {
    return NextResponse.json(
      { error: `Too many login attempts. Please wait ${rateLimit.resetSeconds}s before trying again.` },
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

  let body: SessionRequestBody;

  try {
    body = (await request.json()) as SessionRequestBody;
  } catch {
    return NextResponse.json({ error: "Invalid session request body." }, { status: 400 });
  }

  if (!body.idToken) {
    return NextResponse.json({ error: "Missing Firebase ID token." }, { status: 400 });
  }

  if (!isFirebaseAdminConfigured()) {
    return NextResponse.json({ ok: true, mode: "client-only" });
  }

  try {
    const sessionCookie = await createAdminSessionCookie(body.idToken);
    const response = NextResponse.json({ ok: true });

    response.cookies.set(
      ADMIN_SESSION_COOKIE_NAME,
      sessionCookie,
      getAdminSessionCookieOptions()
    );

    return response;
  } catch (error) {
    const message = error instanceof Error ? error.message : "Unable to create admin session.";
    const status = message.includes("not configured") ? 500 : 401;

    return NextResponse.json({ error: message }, { status });
  }
}

export async function DELETE() {
  const response = NextResponse.json({ ok: true });

  response.cookies.set(ADMIN_SESSION_COOKIE_NAME, "", {
    ...getAdminSessionCookieOptions(0),
    expires: new Date(0)
  });

  return response;
}
