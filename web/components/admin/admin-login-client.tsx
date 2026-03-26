"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signOut
} from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";

export function AdminLoginClient() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [redirecting, setRedirecting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const servicesRef = useRef(getFirebaseServices());

  async function createServerSession(idToken: string) {
    const response = await fetch("/api/admin/session", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ idToken })
    });

    const payload = (await response.json().catch(() => null)) as { error?: string } | null;

    if (!response.ok) {
      throw new Error(payload?.error ?? "Unable to create admin session.");
    }
  }

  useEffect(() => {
    const services = servicesRef.current;
    if (!services) return;

    const unsubscribe = onAuthStateChanged(services.auth, async (user) => {
      if (!user) return;

      try {
        const idToken = await user.getIdToken(true);
        await createServerSession(idToken);
        setRedirecting(true);
        router.replace("/admin");
      } catch (sessionError) {
        setError(
          sessionError instanceof Error
            ? sessionError.message
            : "Unable to create admin session."
        );
        void signOut(services.auth);
      }
    });

    return () => unsubscribe();
  }, [router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const services = servicesRef.current;

    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const credential = await signInWithEmailAndPassword(
        services.auth,
        email.trim(),
        password
      );
      const idToken = await credential.user.getIdToken(true);
      await createServerSession(idToken);
      setRedirecting(true);
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

  if (redirecting) {
    return (
      <div className="page-shell">
        <div className="page-wrap">
          <section className="hero">
            <p className="lede">Signing in...</p>
          </section>
        </div>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />

      <div className="page-wrap">
        <section className="hero">
          <span className="eyebrow">Admin login</span>
          <h1 className="headline">DutyPe Admin</h1>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label>
              <span>Email</span>
              <input
                id="email"
                name="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@dutype.com"
                autoComplete="email"
                required
              />
            </label>
            <label>
              <span>Password</span>
              <input
                id="password"
                name="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                autoComplete="current-password"
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
