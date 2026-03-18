"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useMemo, useState } from "react";
import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  updateProfile
} from "firebase/auth";
import { doc, getDoc, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  getActiveProductRole,
  productRoleLabel,
  productRolePath,
  PRODUCT_ROLES,
  type ProductRole
} from "@/lib/product/profile";

type AuthMode = "signin" | "signup";

const initialState = {
  companyName: "",
  email: "",
  fullName: "",
  password: ""
};

const roleHighlights = {
  WORKER: [
    "Open nearby jobs fast with saved and applied tracking.",
    "Complete the worker profile fields used by the Android app.",
    "Keep job actions inside the product shell instead of the public site."
  ],
  EMPLOYER: [
    "Create live jobs in Firestore with the Android-compatible shape.",
    "Review worker applications and update status from the employer shell.",
    "Keep hiring operations inside one product workspace."
  ]
} as const;

export function ProductAuthClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedRole = searchParams.get("role");
  const defaultRole = requestedRole === "EMPLOYER" ? "EMPLOYER" : "WORKER";

  const [mode, setMode] = useState<AuthMode>("signin");
  const [selectedRole, setSelectedRole] = useState<ProductRole>(defaultRole);
  const [form, setForm] = useState(initialState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const selectedRoleHighlights = roleHighlights[selectedRole];

  async function redirectToRoleHome(userId: string, fallbackRole: ProductRole) {
    if (!services) {
      return;
    }

    const snapshot = await getDoc(doc(services.db, "users", userId));

    if (!snapshot.exists()) {
      router.replace(productRolePath(fallbackRole));
      return;
    }

    const role = getActiveProductRole(snapshot.data()) ?? fallbackRole;
    router.replace(productRolePath(role));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!services) {
      setError("Firebase is not configured for the product app.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      if (mode === "signin") {
        const credential = await signInWithEmailAndPassword(
          services.auth,
          form.email.trim(),
          form.password
        );

        await updateDoc(doc(services.db, "users", credential.user.uid), {
          isActive: true,
          lastActiveAt: serverTimestamp()
        }).catch(() => undefined);

        await redirectToRoleHome(credential.user.uid, selectedRole);
        return;
      }

      const credential = await createUserWithEmailAndPassword(
        services.auth,
        form.email.trim(),
        form.password
      );

      if (form.fullName.trim()) {
        await updateProfile(credential.user, {
          displayName: form.fullName.trim()
        });
      }

      const currentTime = serverTimestamp();
      const userProfile = {
        phone: "",
        fullName: form.fullName.trim(),
        profileImageUrl: "",
        roles: [selectedRole],
        activeRole: selectedRole,
        location: {
          lat: 0,
          lng: 0
        },
        geohash: "s0000000000",
        isVerified: false,
        isActive: true,
        createdAt: currentTime,
        lastActiveAt: currentTime
      };

      await setDoc(doc(services.db, "users", credential.user.uid), userProfile);

      if (selectedRole === "WORKER") {
        await setDoc(
          doc(services.db, "worker_profiles", credential.user.uid),
          {
            userId: credential.user.uid,
            jobTypes: [],
            isAvailable: true,
            rating: 0,
            totalRatings: 0,
            totalJobs: 0,
            lastActiveAt: currentTime
          },
          { merge: true }
        );
      } else {
        await setDoc(
          doc(services.db, "employer_profiles", credential.user.uid),
          {
            userId: credential.user.uid,
            companyName: form.companyName.trim(),
            rating: 0,
            totalRatings: 0,
            totalHires: 0
          },
          { merge: true }
        );
      }

      router.replace(productRolePath(selectedRole));
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Unable to continue.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />
      <div className="page-ambient ambient-c" />

      <div className="page-wrap">
        <section className="hero product-hero">
          <div className="auth-shell-grid">
            <div className="hero-copy">
              <span className="eyebrow">DutyPe app auth</span>
              <h1 className="headline">Open the worker or employer product flow, not just the public site.</h1>
              <p className="lede">
                This is the first Android-parity auth slice in web. It creates or reuses a
                Firebase user and routes into the worker or employer app shell.
              </p>

              <div className="product-tab-row">
                <button
                  type="button"
                  className={`product-tab ${mode === "signin" ? "active" : ""}`}
                  onClick={() => setMode("signin")}
                >
                  Sign in
                </button>
                <button
                  type="button"
                  className={`product-tab ${mode === "signup" ? "active" : ""}`}
                  onClick={() => setMode("signup")}
                >
                  Create account
                </button>
              </div>

              <article className="detail-panel">
                <span className="card-kicker">{productRoleLabel(selectedRole)} route</span>
                <h3>{selectedRole === "WORKER" ? "Worker-first flow" : "Employer operations flow"}</h3>
                <ul className="helper-list">
                  {selectedRoleHighlights.map((highlight) => (
                    <li key={highlight}>{highlight}</li>
                  ))}
                </ul>
              </article>
            </div>

            <div className="auth-panel">
              <form className="product-form" onSubmit={handleSubmit}>
                <label>
                  <span>Role</span>
                  <select
                    value={selectedRole}
                    onChange={(event) => setSelectedRole(event.target.value as ProductRole)}
                  >
                    {PRODUCT_ROLES.map((role) => (
                      <option key={role} value={role}>
                        {productRoleLabel(role)}
                      </option>
                    ))}
                  </select>
                </label>

                {mode === "signup" ? (
                  <label>
                    <span>Full name</span>
                    <input
                      value={form.fullName}
                      onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))}
                      placeholder="Rani Kumari"
                      required
                    />
                  </label>
                ) : null}

                {mode === "signup" && selectedRole === "EMPLOYER" ? (
                  <label>
                    <span>Company name</span>
                    <input
                      value={form.companyName}
                      onChange={(event) =>
                        setForm((current) => ({ ...current, companyName: event.target.value }))
                      }
                      placeholder="GreenKart Retail"
                      required
                    />
                  </label>
                ) : null}

                <label>
                  <span>Email</span>
                  <input
                    type="email"
                    value={form.email}
                    onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                    placeholder="name@example.com"
                    required
                  />
                </label>

                <label>
                  <span>Password</span>
                  <input
                    type="password"
                    value={form.password}
                    onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                    placeholder="Minimum 6 characters"
                    required
                  />
                </label>

                <div className="product-form-wide callout">
                  First parity slice note: this web auth currently uses Firebase email/password.
                  Phone auth, Google auth, and richer onboarding are still separate follow-up work.
                </div>

                <div className="product-form-wide button-row">
                  <button type="submit" className="button" disabled={submitting}>
                    {submitting
                      ? "Working..."
                      : mode === "signin"
                        ? `Open ${productRoleLabel(selectedRole)} app`
                        : `Create ${productRoleLabel(selectedRole)} account`}
                  </button>
                  <Link href="/app" className="button ghost">
                    Back to app entry
                  </Link>
                </div>
              </form>

              {error ? <div className="callout">Auth error: {error}</div> : null}
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}
