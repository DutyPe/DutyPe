"use client";

import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { GoogleAuthProvider, RecaptchaVerifier, signInWithPhoneNumber, signInWithPopup, type ConfirmationResult, type User } from "firebase/auth";

import { SiteIcon } from "@/components/site-icon";
import { normalizeSignInPhone } from "@/lib/firebase/account-actions";
import { firebaseAuthErrorMessage } from "@/lib/firebase/auth-errors";
import { getFirebaseServices } from "@/lib/firebase/client";

type Props = {
  disabled: boolean;
  showPhone: boolean;
  onBusyChange: (busy: boolean) => void;
  onAuthenticated: (user: User) => Promise<void>;
};

export function ProviderSignIn({ disabled, showPhone, onBusyChange, onAuthenticated }: Props) {
  const [phone, setPhone] = useState("");
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
    mounted.current = true;
    return () => { mounted.current = false; captcha.current?.clear(); captcha.current = null; };
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
      if (mounted.current) await onAuthenticated(result.user);
    });
  }

  async function sendCode(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    if (cooldown > 0) return;
    const normalized = normalizeSignInPhone(phone);
    if (!normalized) { setError("Enter a valid mobile number, including the country code for numbers outside India."); return; }
    if (!consent) { setError("Please agree to receive a verification SMS."); return; }
    const services = getFirebaseServices();
    if (!services || !captchaContainer.current) return;

    await perform("send", async () => {
      captcha.current?.clear();
      const verifier = new RecaptchaVerifier(services.auth, captchaContainer.current!, { size: "invisible" });
      captcha.current = verifier;
      try {
        const result = await signInWithPhoneNumber(services.auth, normalized, verifier);
        if (mounted.current) {
          verifiedUser.current = null;
          setConfirmation(result);
          setSentPhone(normalized);
          setCode("");
          resendAt.current = Date.now() + 60_000;
          setCooldown(60);
        }
      } finally {
        if (captcha.current === verifier) { captcha.current = null; verifier.clear(); }
      }
    });
  }

  async function verifyCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!confirmation || !/^\d{6}$/.test(code)) return;
    await perform("verify", async () => {
      const user = verifiedUser.current ?? (await confirmation.confirm(code)).user;
      verifiedUser.current = user;
      if (mounted.current) await onAuthenticated(user);
    });
  }

  return (
    <div className="provider-sign-in">
      <button type="button" className="button ghost google-signin" disabled={disabled || Boolean(busy)} onClick={() => void googleSignIn()}>
        {busy === "google" ? "Connecting to Google..." : "Continue with Google"}
      </button>
      {showPhone ? (
        <form className="product-form phone-signin" onSubmit={confirmation ? verifyCode : sendCode} aria-busy={Boolean(busy)}>
          {confirmation ? <>
            <p className="product-form-wide" role="status">Verification code sent to {sentPhone}.</p>
            <label className="product-form-wide">
              <span>Verification code</span>
              <input ref={codeInput} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))} type="text" inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" minLength={6} maxLength={6} required disabled={Boolean(busy)} />
            </label>
          </> : <>
            <label className="product-form-wide">
              <span>Mobile number</span>
              <input ref={phoneInput} type="tel" autoComplete="tel" inputMode="tel" placeholder="+91 90000 00000" value={phone} onChange={(event) => setPhone(event.target.value)} required maxLength={24} disabled={Boolean(busy)} />
            </label>
            <label className="phone-consent product-form-wide">
              <input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} required disabled={Boolean(busy)} />
              <span>I agree to receive a verification SMS. Standard rates may apply. Google processes this number to prevent abuse.</span>
            </label>
          </>}
          <div className="product-form-wide auth-submit-row">
            <button type="submit" className="button" disabled={disabled || Boolean(busy) || (!confirmation && cooldown > 0)}>
              {busy === "send" ? "Sending code..." : busy === "verify" ? "Verifying..." : confirmation ? "Verify and continue" : cooldown > 0 ? `Send code in ${cooldown}s` : "Send verification code"}<SiteIcon name="arrow-right" />
            </button>
          </div>
          {confirmation ? <div className="product-form-wide phone-actions">
            <button type="button" className="auth-text-button" disabled={disabled || Boolean(busy) || cooldown > 0} onClick={() => void sendCode()}>{cooldown > 0 ? `Resend in ${cooldown}s` : "Resend code"}</button>
            <button type="button" className="auth-text-button" disabled={Boolean(busy)} onClick={() => { setConfirmation(null); setCode(""); setError(null); verifiedUser.current = null; requestAnimationFrame(() => phoneInput.current?.focus()); }}>Change number</button>
          </div> : null}
        </form>
      ) : null}
      <div id={captchaId} ref={captchaContainer} />
      {error ? <p className="callout" role="alert">{error}</p> : null}
    </div>
  );
}