"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut
} from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";
import { ADMIN_POLICY_SUMMARY } from "@/lib/firebase/admin-access";

export function AdminLoginClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function createServerSession(user: Awaited<ReturnType<typeof signInWithEmailAndPassword>>["user"]) {
    const idToken = await user.getIdToken(true);
    const response = await fetch("/api/admin/session", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ idToken })
    });

    const payload = (await response.json().catch(() => null)) as { error?: string } | null;

    if (!response.ok) {
      throw new Error(payload?.error ?? "Unable to create a server-backed admin session.");
    }
  }

  useEffect(() => {
    if (!services) {
      return;
    }

    const unsubscribe = onAuthStateChanged(services.auth, async (user) => {
      if (!user) {
        return;
      }

      try {
        await createServerSession(user);
        router.replace("/admin");
      } catch (sessionError) {
        setError(
          sessionError instanceof Error
            ? sessionError.message
            : "Unable to create a server-backed admin session."
        );
        void signOut(services.auth);
      }
    });

    return () => unsubscribe();
  }, [router, services]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const credential = await signInWithEmailAndPassword(services.auth, email.trim(), password);
      await createServerSession(credential.user);
      router.replace("/admin");
    } catch (signInError) {
      if (services.auth.currentUser) {
        await signOut(services.auth);
      }

      setError(signInError instanceof Error ? signInError.message : "Unable to sign in.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />

      <div className="page-wrap">
        <section className="hero">
          <span className="eyebrow">Admin login</span>
          <h1 className="headline">Sign in with Firebase Auth to use the new React admin routes.</h1>
          <p className="lede">
            This replaces the legacy static login page for the new admin workspace.
            Use the same admin account that already exists in Firebase Authentication.
          </p>

          <div className="callout">
            Current admin policy matches Firestore rules: {ADMIN_POLICY_SUMMARY}. A
            server session cookie is created when Firebase Admin credentials are available.
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label>
              <span>Email</span>
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="admin@dutype.com"
                required
              />
            </label>
            <label>
              <span>Password</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter password"
                required
              />
            </label>

            <div className="button-row">
              <button type="submit" className="button" disabled={submitting}>
                {submitting ? "Signing in..." : "Sign in"}
              </button>
              <Link href="/" className="button ghost">
                Back to public site
              </Link>
            </div>
          </form>

          {error ? <div className="callout">Sign-in error: {error}</div> : null}
        </section>
      </div>
    </div>
  );
}
