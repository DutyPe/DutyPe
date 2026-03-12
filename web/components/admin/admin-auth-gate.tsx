"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ReactNode, useEffect, useMemo, useState } from "react";
import { User, onAuthStateChanged, signOut } from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  ADMIN_POLICY_SUMMARY,
  formatAdminAuthorizationSource,
  getAdminAuthorization
} from "@/lib/firebase/admin-access";

type AdminAuthGateProps = {
  children: ReactNode;
};

export function AdminAuthGate({ children }: AdminAuthGateProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [matchedBy, setMatchedBy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!services) {
      setError("Firebase is not configured for the web app.");
      setIsLoading(false);
      return;
    }

    const unsubscribe = onAuthStateChanged(
      services.auth,
      async (currentUser) => {
        setUser(currentUser);

        if (!currentUser) {
          setMatchedBy(null);
          setIsLoading(false);
          return;
        }

        try {
          const tokenResult = await currentUser.getIdTokenResult(true);
          const authorization = getAdminAuthorization({
            email: currentUser.email,
            ...tokenResult.claims
          });

          setMatchedBy(formatAdminAuthorizationSource(authorization.matchedBy));
        } catch (tokenError) {
          setMatchedBy(null);
          setError(
            tokenError instanceof Error
              ? tokenError.message
              : "Failed to read admin authorization claims."
          );
        } finally {
          setIsLoading(false);
        }
      },
      (authError) => {
        setError(authError.message);
        setIsLoading(false);
      }
    );

    return () => unsubscribe();
  }, [services]);

  async function handleSignOut() {
    if (!services) {
      return;
    }

    try {
      await fetch("/api/admin/session", {
        method: "DELETE"
      });

      await signOut(services.auth);
      router.replace("/admin/login");
      router.refresh();
    } catch (signOutError) {
      setError(signOutError instanceof Error ? signOutError.message : "Failed to sign out.");
    }
  }

  if (isLoading) {
    return (
      <section className="section">
        <div className="empty-state">
          Checking Firebase admin session for this browser.
        </div>
      </section>
    );
  }

  if (!user) {
    return (
      <section className="section">
        <div className="detail-panel">
          <span className="card-kicker">Admin sign-in required</span>
          <h3>Open the new login route before using admin tools.</h3>
          <p>
            These admin pages now expect both a Firebase Auth session in the Next app
            and the server-backed admin session cookie created during login.
          </p>
          <div className="button-row">
            <Link href="/admin/login" className="button">
              Go to admin login
            </Link>
            <Link href="/" className="button ghost">
              Back to public site
            </Link>
          </div>
          {error ? <p className="callout">Auth error: {error}</p> : null}
        </div>
      </section>
    );
  }

  if (!matchedBy) {
    return (
      <section className="section">
        <div className="detail-panel">
          <span className="card-kicker">Access denied</span>
          <h3>{user.email ?? "Signed-in account"} is not allowed to open DutyPe admin routes.</h3>
          <p>
            The React admin guard now mirrors the Firestore policy: {ADMIN_POLICY_SUMMARY}.
          </p>
          <div className="button-row">
            <button type="button" className="button" onClick={handleSignOut}>
              Sign out
            </button>
            <Link href="/" className="button ghost">
              Back to public site
            </Link>
          </div>
          {error ? <p className="callout">Auth error: {error}</p> : null}
        </div>
      </section>
    );
  }

  return (
    <>
      <section className="section">
        <div className="auth-banner">
          <div>
            <span className="card-kicker">Authorized admin</span>
            <h3>{user.email ?? "Signed-in admin"}</h3>
            <p>
              This Firebase session matches the verified server session for this request.
              Authorization source: {matchedBy}.
            </p>
          </div>

          <button type="button" className="button ghost" onClick={handleSignOut}>
            Sign out
          </button>
        </div>
        {error ? <div className="callout">Auth error: {error}</div> : null}
      </section>

      {children}
    </>
  );
}
