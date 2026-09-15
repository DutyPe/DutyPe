"use client";

import Image from "next/image";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import {
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  updateProfile
} from "firebase/auth";
import type { User } from "firebase/auth";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";

import { SiteIcon } from "@/components/site-icon";
import { FIREBASE_SETUP_ERROR, firebaseAuthErrorMessage } from "@/lib/firebase/auth-errors";
import { getFirebaseServices } from "@/lib/firebase/client";
import { ensureProductAccount } from "@/lib/firebase/account-actions";
import {
  extractProductRoles,
  getActiveProductRole,
  productReturnPath,
  productRoleLabel,
  PRODUCT_ROLES,
  type ProductRole
} from "@/lib/product/profile";

import { useProductSession } from "./use-product-session";
import { ProviderSignIn } from "./provider-sign-in";

type AuthMode = "signin" | "signup" | "reset";

const initialState = {
  companyName: "",
  email: "",
  fullName: "",
  password: ""
};

export function ProductAuthClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const { refreshProfile } = useProductSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedRole = searchParams.get("role");
  const returnPath = searchParams.get("next");
  const defaultRole = requestedRole === "EMPLOYER" ? "EMPLOYER" : "WORKER";

  const [mode, setMode] = useState<AuthMode>("signin");
  const [method, setMethod] = useState<"phone" | "email">(searchParams.get("method") === "email" || defaultRole === "WORKER" ? "email" : "phone");
  const [selectedRole, setSelectedRole] = useState<ProductRole>(defaultRole);
  const [form, setForm] = useState(initialState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);
  const [resetSent, setResetSent] = useState(false);
  const submitInFlight = useRef(false);
  const heading = useRef<HTMLHeadingElement>(null);

  useEffect(() => {
    if (resetSent) heading.current?.focus();
  }, [resetSent]);

  function changeMode(nextMode: AuthMode) {
    if (submitInFlight.current) return;
    setMode(nextMode);
    setError(null);
    setResetSent(false);
    setShowPassword(false);
    heading.current?.focus();
  }

  async function completeProviderSignIn(user: User) {
    if (!services) return;
    await ensureProductAccount(services.db, user, selectedRole);
    await refreshProfile();
    router.replace(productReturnPath(selectedRole, returnPath));
  }

  async function redirectToRoleHome(userId: string, fallbackRole: ProductRole) {
    if (!services) {
      return;
    }

    let timeout: ReturnType<typeof setTimeout> | undefined;
    try {
      const snapshot = await Promise.race([
        getDoc(doc(services.db, "users", userId)),
        new Promise<never>((_, reject) => {
          timeout = setTimeout(() => reject(Object.assign(new Error("Account loading timed out."), {
            code: "app/account-timeout"
          })), 10_000);
        })
      ]);

      if (!snapshot.exists()) {
        router.replace(productReturnPath(fallbackRole, returnPath));
        return;
      }

      const profile = snapshot.data();
      const role = extractProductRoles(profile).includes(fallbackRole)
        ? fallbackRole
        : getActiveProductRole(profile) ?? fallbackRole;
      router.replace(productReturnPath(role, returnPath));
    } finally {
      clearTimeout(timeout);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submitInFlight.current || (mode === "reset" && resetSent)) return;

    if (!services) {
      setError(FIREBASE_SETUP_ERROR);
      return;
    }

    try {
      submitInFlight.current = true;
      setSubmitting(true);
      setError(null);

      if (mode === "reset") {
        await sendPasswordResetEmail(services.auth, form.email.trim());
        setResetSent(true);
        return;
      }

      if (mode === "signin") {
        const credential = await signInWithEmailAndPassword(
          services.auth,
          form.email.trim(),
          form.password
        );

        void updateDoc(doc(services.db, "users", credential.user.uid), {
          isActive: true,
          lastLoginAt: Date.now(),
          updatedAt: Date.now()
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

      const currentTime = Date.now();
      const userProfile = {
        activeRole: selectedRole,
        address: "",
        bio: "",
        businessAddress: "",
        businessLatitude: 0,
        businessLongitude: 0,
        companyName: selectedRole === "EMPLOYER" ? form.companyName.trim() : "",
        companySize: "",
        contactEmail: form.email.trim(),
        contactPhone: "",
        createdAt: currentTime,
        currentLocationAddress: "",
        currentLocationLabel: "",
        dateOfBirth: "",
        email: form.email.trim(),
        experience: "",
        fullName: form.fullName.trim(),
        gender: "",
        id: credential.user.uid,
        industry: "",
        isActive: true,
        lastLoginAt: currentTime,
        latitude: 0,
        longitude: 0,
        name: form.fullName.trim(),
        phone: "",
        profileCompleted: false,
        profileImageUrl: "",
        role: selectedRole,
        roles: [selectedRole],
        savedJobs: [],
        skills: "",
        trustTier: "NEW",
        updatedAt: currentTime,
        website: "",
        workLocations: []
      };

      await setDoc(doc(services.db, "users", credential.user.uid), userProfile);

      await refreshProfile();
      router.replace(productReturnPath(selectedRole, returnPath));
    } catch (submitError) {
      if (mode === "reset") {
        const code = submitError && typeof submitError === "object" && "code" in submitError
          ? String(submitError.code)
          : "";

        if (code === "auth/user-not-found") {
          setResetSent(true);
        } else {
          setError(code === "auth/too-many-requests"
            ? "Too many reset requests. Please try again later."
            : firebaseAuthErrorMessage(submitError));
        }
      } else {
        setError(firebaseAuthErrorMessage(submitError));
      }
    } finally {
      submitInFlight.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="page-shell auth-page">
      <a href="#account-content" className="skip-link">Skip to content</a>
      <header className="auth-topbar">
        <Link href="/" className="brand site-brand" aria-label="DutyPe home">
          <Image src="/dutype-logo.webp" alt="" width={40} height={40} className="brand-logo" priority />
          <strong>Duty<span>Pe</span></strong>
        </Link>
        <Link href="/jobs" className="text-link">Browse jobs <SiteIcon name="arrow-up-right" /></Link>
      </header>

      <main className="auth-layout" id="account-content" tabIndex={-1}>
        <section className="auth-content">
          <span className="section-label">YOUR NEXT CHAPTER STARTS HERE</span>
          <h1 ref={heading} tabIndex={-1}>{mode === "reset" ? resetSent ? "Check your email" : "Reset your password" : mode === "signin" ? "Welcome back" : "Create your account"}</h1>
          <p className="auth-intro">{mode === "reset" ? "Get back to your DutyPe account." : mode === "signin" ? "Sign in to your DutyPe account." : "Join DutyPe as a worker or employer."}</p>

          {mode !== "reset" ? <>
            <div className="product-tab-row" role="group" aria-label="Sign-in method">
              <button type="button" className={`product-tab ${method === "phone" ? "active" : ""}`} aria-pressed={method === "phone"} disabled={submitting} onClick={() => { setMethod("phone"); setError(null); }}>Phone OTP</button>
              <button type="button" className={`product-tab ${method === "email" ? "active" : ""}`} aria-pressed={method === "email"} disabled={submitting} onClick={() => { setMethod("email"); setError(null); }}>Email</button>
            </div>
            {method === "phone" ? <label className="provider-role"><span>I am a</span><select value={selectedRole} disabled={submitting} onChange={(event) => setSelectedRole(event.target.value as ProductRole)}>{PRODUCT_ROLES.map((role) => <option key={role} value={role}>{productRoleLabel(role)}</option>)}</select></label> : null}
            <ProviderSignIn disabled={submitting || !services} showPhone={method === "phone"} onBusyChange={(busy) => { submitInFlight.current = busy; setSubmitting(busy); }} onAuthenticated={completeProviderSignIn} />
          </> : null}

          {mode !== "reset" && method === "email" ? (
          <div className="product-tab-row" role="group" aria-label="Account access">
            <button type="button" className={`product-tab ${mode === "signin" ? "active" : ""}`} aria-pressed={mode === "signin"} disabled={submitting} onClick={() => changeMode("signin")}>
              Sign in
            </button>
            <button type="button" className={`product-tab ${mode === "signup" ? "active" : ""}`} aria-pressed={mode === "signup"} disabled={submitting} onClick={() => changeMode("signup")}>
              Create account
            </button>
          </div>
          ) : null}

          {method === "email" || mode === "reset" ? (mode === "reset" && resetSent ? (
            <div className="auth-reset-confirmation" role="status">
              <span className="category-icon tone-mint"><SiteIcon name="check" /></span>
              <p>If an account exists for <strong>{form.email.trim()}</strong>, you&apos;ll receive a password reset link.</p>
            </div>
          ) : (
          <form className="product-form" onSubmit={handleSubmit} aria-busy={submitting}>
            {mode !== "reset" ? (
            <label>
              <span>I am a</span>
              <select value={selectedRole} disabled={submitting} onChange={(event) => setSelectedRole(event.target.value as ProductRole)}>
                {PRODUCT_ROLES.map((role) => <option key={role} value={role}>{productRoleLabel(role)}</option>)}
              </select>
            </label>
            ) : null}

            {mode === "signup" ? (
              <label>
                <span>Full name</span>
                <input autoComplete="name" value={form.fullName} onChange={(event) => setForm((current) => ({ ...current, fullName: event.target.value }))} placeholder="Your full name" required />
              </label>
            ) : null}

            {mode === "signup" && selectedRole === "EMPLOYER" ? (
              <label>
                <span>Company name</span>
                <input autoComplete="organization" value={form.companyName} onChange={(event) => setForm((current) => ({ ...current, companyName: event.target.value }))} placeholder="Your company or business" required />
              </label>
            ) : null}

            <label>
              <span>Email</span>
              <input type="email" autoComplete={mode === "reset" ? "email" : "username"} disabled={submitting} value={form.email} onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))} placeholder="name@example.com" required />
            </label>

            {mode !== "reset" ? (
            <div className="auth-password-control">
              <div className="auth-password-label">
                <label htmlFor="account-password">Password</label>
                {mode === "signin" ? <button type="button" className="auth-text-button" disabled={submitting} onClick={() => changeMode("reset")}>Forgot password?</button> : null}
              </div>
              <div className="password-field">
                <input
                  id="account-password"
                  type={showPassword ? "text" : "password"}
                  autoComplete={mode === "signup" ? "new-password" : "current-password"}
                  minLength={mode === "signup" ? 6 : undefined}
                  value={form.password}
                  onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                  placeholder={mode === "signup" ? "Minimum 6 characters" : "Your password"}
                  required
                />
                <button type="button" className="password-toggle" aria-label={showPassword ? "Hide password" : "Show password"} title={showPassword ? "Hide password" : "Show password"} aria-pressed={showPassword} onClick={() => setShowPassword((current) => !current)}>
                  <SiteIcon name={showPassword ? "eye-off" : "eye"} />
                </button>
              </div>
            </div>
            ) : null}

            <div className="product-form-wide auth-submit-row">
              <button type="submit" className="button" disabled={submitting || !services}>
                {submitting ? "Working..." : mode === "reset" ? "Send reset link" : mode === "signin" ? "Sign in" : `Create ${productRoleLabel(selectedRole)} account`}
                <SiteIcon name="arrow-right" />
              </button>
            </div>
          </form>
          )) : null}

          {mode === "reset" ? (
            <div className="auth-reset-actions">
              <button type="button" className={resetSent ? "button" : "auth-text-button"} disabled={submitting} onClick={() => changeMode("signin")}>Back to sign in<SiteIcon name="arrow-right" /></button>
              {resetSent ? <button type="button" className="auth-text-button" onClick={() => changeMode("reset")}>Use a different email</button> : null}
            </div>
          ) : null}

          {!services || error ? <div className="callout" role="alert">{!services ? FIREBASE_SETUP_ERROR : error}</div> : null}
          <div className="auth-legal"><Link href="/terms">Terms of service</Link><Link href="/privacy">Privacy policy</Link></div>
        </section>

        <aside className="auth-aside" aria-label="About DutyPe">
          <Image src="/dutype-logo.webp" alt="DutyPe" width={88} height={88} />
          <div>
            <span className="section-label">LOCAL WORK. REAL POSSIBILITIES.</span>
            <h2>{selectedRole === "EMPLOYER" ? <>Great teams.<br />Start nearby.</> : <>Good work.<br />Closer to home.</>}</h2>
            <p>{selectedRole === "EMPLOYER" ? "For the people who keep your business moving." : "For the people who keep our neighbourhoods moving."}</p>
          </div>
          <div className="auth-aside-bottom"><SiteIcon name="shield-check" /><span>Good opportunities. No recruitment fees.</span></div>
        </aside>
      </main>
    </div>
  );
}
