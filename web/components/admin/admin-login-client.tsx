"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";
import { signInWithEmailAndPassword } from "firebase/auth";

import { useProductSession } from "@/components/product/use-product-session";
import { getFirebaseServices } from "@/lib/firebase/client";

export function AdminLoginClient() {
  const router = useRouter();
  const session = useProductSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [redirecting, setRedirecting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const servicesRef = useRef(getFirebaseServices());
  const submitInFlight = useRef(false);

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
    const user = session.user;
    if (!user || submitInFlight.current) return;
    let disposed = false;

    async function resumeAdminSession() {
      try {
        const idToken = await user!.getIdToken();
        if (disposed) return;
        await createServerSession(idToken);
        if (disposed || servicesRef.current?.auth.currentUser?.uid !== user!.uid) return;
        setRedirecting(true);
        router.replace("/admin");
      } catch (sessionError) {
        if (disposed) return;
        setError(
          sessionError instanceof Error
            ? sessionError.message
            : "Unable to create admin session."
        );
      }
    }
    void resumeAdminSession();

    return () => { disposed = true; };
  }, [session.user, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitInFlight.current) return;
    const services = servicesRef.current;

    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    try {
      submitInFlight.current = true;
      setSubmitting(true);
      setError(null);
      const credential = await signInWithEmailAndPassword(
        services.auth,
        email.trim(),
        password
      );
      const idToken = await credential.user.getIdToken();
      await createServerSession(idToken);
      if (services.auth.currentUser?.uid !== credential.user.uid) return;
      setRedirecting(true);
      router.replace("/admin");
    } catch (signInError) {
      setError(signInError instanceof Error ? signInError.message : "Unable to sign in.");
    } finally {
      submitInFlight.current = false;
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
        <section className="hero admin-login-hero">
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
              <button type="submit" className="button" disabled={submitting || (session.loading && !session.user)}>
                {submitting ? "Signing in..." : "Sign in"}
              </button>
              <Link href="/" className="button ghost">
                Back to public site
              </Link>
            </div>
          </form>

          {session.loading && !session.user ? <div className="callout" role="status">Loading your account...</div> : null}
          {error || session.error ? <div className="callout" role="alert">Sign-in error: {error || session.error}</div> : null}
        </section>
      </div>
    </div>
  );
}
