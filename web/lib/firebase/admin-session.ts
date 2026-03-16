import "server-only";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import {
  ADMIN_POLICY_SUMMARY,
  formatAdminAuthorizationSource,
  getAdminAuthorization
} from "@/lib/firebase/admin-access";
import { getFirebaseAdminAuth, isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";

export const ADMIN_SESSION_COOKIE_NAME = "dutype_admin_session";
export const ADMIN_SESSION_MAX_AGE_MS = 1000 * 60 * 60 * 24 * 5;

export type AdminSession = {
  uid: string;
  email: string;
  matchedBy: string;
};

export function getAdminSessionCookieOptions(maxAgeSeconds = ADMIN_SESSION_MAX_AGE_MS / 1000) {
  return {
    httpOnly: true,
    maxAge: maxAgeSeconds,
    path: "/",
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production"
  };
}

export async function createAdminSessionCookie(idToken: string) {
  const auth = getFirebaseAdminAuth();
  const decodedToken = await auth.verifyIdToken(idToken, true);
  const authorization = getAdminAuthorization(decodedToken);

  if (!authorization.isAuthorized) {
    throw new Error(
      `This Firebase account is not allowed to use DutyPe admin routes. Allowed policy: ${ADMIN_POLICY_SUMMARY}.`
    );
  }

  return auth.createSessionCookie(idToken, {
    expiresIn: ADMIN_SESSION_MAX_AGE_MS
  });
}

export async function verifyAdminSessionCookie(sessionCookie: string): Promise<AdminSession | null> {
  const decodedCookie = await getFirebaseAdminAuth().verifySessionCookie(sessionCookie, true);
  const authorization = getAdminAuthorization(decodedCookie);

  if (!authorization.isAuthorized || !authorization.matchedBy) {
    return null;
  }

  return {
    uid: decodedCookie.uid,
    email: decodedCookie.email ?? "",
    matchedBy: formatAdminAuthorizationSource(authorization.matchedBy)
  };
}

export async function getAdminSession(): Promise<AdminSession | null> {
  const sessionCookie = cookies().get(ADMIN_SESSION_COOKIE_NAME)?.value;

  if (!sessionCookie) {
    return null;
  }

  try {
    return await verifyAdminSessionCookie(sessionCookie);
  } catch {
    return null;
  }
}

export async function requireAdminSession() {
  if (!isFirebaseAdminConfigured()) {
    // Fallback for environments where admin credentials are not provisioned.
    return {
      uid: "client-only-admin",
      email: "",
      matchedBy: "Client auth fallback"
    };
  }

  const session = await getAdminSession();

  if (!session) {
    redirect("/admin/login");
  }

  return session;
}
