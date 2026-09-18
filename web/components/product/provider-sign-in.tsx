"use client";

import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { GoogleAuthProvider, RecaptchaVerifier, signInWithPhoneNumber, signInWithPopup, type ConfirmationResult, type User } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";

import { SiteIcon } from "@/components/site-icon";
import { normalizeSignInPhone } from "@/lib/firebase/account-actions";
import { firebaseAuthErrorMessage } from "@/lib/firebase/auth-errors";
import { getFirebaseServices } from "@/lib/firebase/client";
import type { EmployerType, ProductRole } from "@/lib/product/profile";

type Props = {
  disabled: boolean;
  showPhone: boolean;
  role: ProductRole;
  returnPath?: string | null;
  onBusyChange: (busy: boolean) => void;
  onAuthenticated: (user: User, extraDetails?: { fullName?: string; companyName?: string; employerType?: EmployerType }) => Promise<void>;
};

export function ProviderSignIn({ disabled, showPhone, role, onBusyChange, onAuthenticated }: Props) {
  const [phone, setPhone] = useState("");
  const [fullName, setFullName] = useState("");
  const [companyName, setCompanyName] = useState("");
  const [employerType, setEmployerType] = useState<EmployerType>("INDIVIDUAL");
  const [sentPhone, setSentPhone] = useState("");
  const [code, setCode] = useState("");
  const [confirmation, setConfirmation] = useState<ConfirmationResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<"google" | "send" | "verify" | null>(null);
  const [cooldown, setCooldown] = useState(0);
  const resendAt = useRef(0);
  const [consent, setConsent] = useState(false);
  const captchaId = useId();
  const captchaContainer = useRef<HTMLDivElement>(null);
  const captcha = useRef<RecaptchaVerifier | null>(null);
  const pending = useRef(false);
  const mounted = useRef(true);
  const verifiedUser = useRef<User | null>(null);
  const codeInput = useRef<HTMLInputElement>(null);
  const phoneInput = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (role !== "EMPLOYER") return;
    try {
      const raw = sessionStorage.getItem("dutype:employer-job-draft:v1");
      if (!raw) return;
      const draft = JSON.parse(raw);
      if (draft.version !== 1 || typeof draft.savedAt !== "number" || draft.savedAt > Date.now() || Date.now() - draft.savedAt > 3_600_000) return;
      if (draft.ownerId && draft.ownerId !== getFirebaseServices()?.auth.currentUser?.uid) return;
      if (draft.form) {
        if (typeof draft.form.companyName === "string" && draft.form.companyName.trim()) {
          setCompanyName((current) => current || draft.form.companyName.slice(0, 120));
          setEmployerType("COMPANY");
        }
        if (typeof draft.form.contactNumber === "string") {
          setPhone((current) => current || draft.form.contactNumber.slice(0, 24));
        }
      }
    } catch {
      // ignore
    }
  }, [role]);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      try {
        captcha.current?.clear();
      } catch {
        // ignore
      }
      captcha.current = null;
    };
  }, []);

  useEffect(() => {
    if (!confirmation) return;
    codeInput.current?.focus();
  }, [confirmation]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setTimeout(() => setCooldown(Math.max(0, Math.ceil((resendAt.current - Date.now()) / 1000))), 1000);
    return () => clearTimeout(timer);
  }, [cooldown]);

  async function perform(action: "google" | "send" | "verify", operation: () => Promise<void>) {
    if (pending.current || disabled) return;
    pending.current = true;
    setBusy(action);
    setError(null);
    onBusyChange(true);
    try {
      await operation();
    } catch (failure) {
      if (mounted.current) setError(firebaseAuthErrorMessage(failure));
    } finally {
      pending.current = false;
      if (mounted.current) { setBusy(null); onBusyChange(false); }
    }
  }

  async function googleSignIn() {
    const services = getFirebaseServices();
    if (!services) return;
    await perform("google", async () => {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: "select_account" });
      const result = await signInWithPopup(services.auth, provider);
      const extraDetails = {
        fullName: fullName.trim() || result.user.displayName || undefined,
        companyName: role === "EMPLOYER" && employerType === "COMPANY" ? companyName.trim() || undefined : undefined,
        employerType: role === "EMPLOYER" ? employerType : undefined
      };
      if (mounted.current) await onAuthenticated(result.user, extraDetails);
    });
  }

  async function sendCode(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    if (cooldown > 0) return;
    const normalized = normalizeSignInPhone(phone);
    if (!normalized) {
      setError("Enter a valid mobile number, including country code for numbers outside India.");
      return;
    }
    if (!consent) {
      setError("Please agree to receive a verification SMS.");
      return;
    }

    const services = getFirebaseServices();
    if (!services || !captchaContainer.current) return;

    // Pre-OTP role conflict check (single-role-per-phone defense)
    try {
      const snap = await getDoc(doc(services.db, "phoneRoles", normalized));
      if (snap.exists()) {
        const data = snap.data() as Record<string, unknown> | undefined;
        const existingRole = String(data?.role || "").toUpperCase();
        if (existingRole && existingRole !== role) {
          const roleLabel = existingRole.toLowerCase();
          setError(`This number is already registered as a ${roleLabel}. Please log in as a ${roleLabel}.`);
          return;
        }
      }
    } catch (checkErr) {
      console.warn("Pre-OTP phone check warning:", checkErr);
    }

    await perform("send", async () => {
      try {
        captcha.current?.clear();
        captcha.current = new RecaptchaVerifier(services.auth, captchaContainer.current!, {
            size: "invisible"
          });
        const result = await signInWithPhoneNumber(services.auth, normalized, captcha.current!);
        if (mounted.current) {
          verifiedUser.current = null;
          setConfirmation(result);
          setSentPhone(normalized);
          setCode("");
          resendAt.current = Date.now() + 60_000;
          setCooldown(60);
        }
      } finally {
        try {
          captcha.current?.clear();
        } catch {
          // ignore
        }
        captcha.current = null;
      }
    });
  }

  async function verifyCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!confirmation || !/^\d{6}$/.test(code)) return;
    await perform("verify", async () => {
      const user = verifiedUser.current ?? (await confirmation.confirm(code)).user;
      verifiedUser.current = user;
      const extraDetails = {
        fullName: fullName.trim() || undefined,
        companyName: role === "EMPLOYER" && employerType === "COMPANY" ? companyName.trim() || undefined : undefined,
        employerType: role === "EMPLOYER" ? employerType : undefined
      };
      if (mounted.current) await onAuthenticated(user, extraDetails);
    });
  }

  return (
    <div className="provider-sign-in">
      <button
        type="button"
        className="google-signin-btn"
        disabled={disabled || Boolean(busy)}
        onClick={() => void googleSignIn()}
      >
        <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true">
          <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
          <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
          <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
          <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
        </svg>
        <span>{busy === "google" ? "Connecting to Google..." : "Continue with Google"}</span>
      </button>

      <div className="auth-divider">
        <span>or use mobile number with OTP</span>
      </div>

      {showPhone ? (
        <form className="product-form phone-signin" onSubmit={confirmation ? verifyCode : sendCode} aria-busy={Boolean(busy)}>
          {confirmation ? (
            <>
              <p className="product-form-wide" role="status">Verification code sent to <strong>{sentPhone}</strong>.</p>
              <label className="product-form-wide">
                <span>6-digit OTP code</span>
                <input
                  ref={codeInput}
                  aria-label="Verification code"
                  value={code}
                  onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
                  type="text"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="[0-9]{6}"
                  minLength={6}
                  maxLength={6}
                  placeholder="123456"
                  required
                  disabled={Boolean(busy)}
                />
              </label>
            </>
          ) : (
            <>
              <label className="product-form-wide">
                <span>Mobile number</span>
                <input
                  ref={phoneInput}
                  type="tel"
                  autoComplete="tel"
                  inputMode="tel"
                  placeholder="98765 43210 or +91 98765 43210"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  required
                  maxLength={24}
                  disabled={Boolean(busy)}
                />
              </label>

              <details className="product-form-wide">
                <summary>Account details (optional)</summary>
                {role === "EMPLOYER" ? (
                  <div className="employer-type-selector product-form-wide" style={{ marginBottom: "0.75rem" }}>
                    <span style={{ fontSize: "0.85rem", fontWeight: 600, display: "block", marginBottom: "0.25rem" }}>
                      Employer account type
                    </span>
                    <div className="product-tab-row compact" role="group" aria-label="Employer account type">
                      <button
                        type="button"
                        className={`product-tab ${employerType === "INDIVIDUAL" ? "active" : ""}`}
                        onClick={() => setEmployerType("INDIVIDUAL")}
                      >
                        👤 Personal / Individual
                      </button>
                      <button
                        type="button"
                        className={`product-tab ${employerType === "COMPANY" ? "active" : ""}`}
                        onClick={() => setEmployerType("COMPANY")}
                      >
                        🏢 Company / Business
                      </button>
                    </div>
                    <small style={{ color: "#64748b", display: "block", marginTop: "0.25rem" }}>
                      {employerType === "INDIVIDUAL"
                        ? "Personal profile for household needs, private help, or instant chores."
                        : "Company profile for shops, restaurants, commercial firms, or multiple vacancies."}
                    </small>
                  </div>
                ) : null}

                <label className="product-form-wide">
                  <span>{role === "EMPLOYER" && employerType === "INDIVIDUAL" ? "Your full name (Personal)" : "Full name"}</span>
                  <input
                    type="text"
                    autoComplete="name"
                    placeholder="Your full name"
                    value={fullName}
                    onChange={(event) => setFullName(event.target.value)}
                    disabled={Boolean(busy)}
                  />
                </label>

                {role === "EMPLOYER" && employerType === "COMPANY" ? (
                  <label className="product-form-wide">
                    <span>Company or Business name</span>
                    <input
                      type="text"
                      autoComplete="organization"
                      placeholder="e.g. Acme Enterprises or Shop Name"
                      value={companyName}
                      onChange={(event) => setCompanyName(event.target.value)}
                      disabled={Boolean(busy)}
                    />
                  </label>
                ) : null}
              </details>

              <label className="phone-consent product-form-wide">
                <input
                  type="checkbox"
                  checked={consent}
                  onChange={(event) => setConsent(event.target.checked)}
                  required
                  disabled={Boolean(busy)}
                />
                <span>I agree to receive a verification SMS. Standard rates may apply. Google verifies this number to protect your account.</span>
              </label>
            </>
          )}

          <div className="product-form-wide auth-submit-row">
            <button
              type="submit"
              className="button"
              disabled={disabled || Boolean(busy) || (!confirmation && cooldown > 0)}
            >
              {busy === "send"
                ? "Sending code..."
                : busy === "verify"
                ? "Verifying..."
                : confirmation
                ? "Verify and continue"
                : cooldown > 0
                ? `Send code in ${cooldown}s`
                : "Send verification code"}
              <SiteIcon name="arrow-right" />
            </button>
          </div>

          {confirmation ? (
            <div className="product-form-wide phone-actions">
              <button
                type="button"
                className="auth-text-button"
                disabled={disabled || Boolean(busy) || cooldown > 0}
                onClick={() => void sendCode()}
              >
                {cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}
              </button>
              <button
                type="button"
                className="auth-text-button"
                disabled={Boolean(busy)}
                onClick={() => {
                  setConfirmation(null);
                  setCode("");
                  setError(null);
                  verifiedUser.current = null;
                  requestAnimationFrame(() => phoneInput.current?.focus());
                }}
              >
                Change number
              </button>
            </div>
          ) : null}
        </form>
      ) : null}

      <div id={captchaId} ref={captchaContainer} />
      {error ? <p className="callout" role="alert">{error}</p> : null}
    </div>
  );
}