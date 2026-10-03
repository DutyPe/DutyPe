/**
 * Pure helpers for WhatsApp login codes and Truecaller profiles (no Firebase, unit tested).
 */
import { createCipheriv, createDecipheriv, createHash, randomBytes, randomInt, timingSafeEqual } from "crypto";

/**
 * Login-code rules (like Twilio Verify / bank OTPs):
 *  - A code is valid for 10 minutes. "Resend" within that time sends the SAME code again (so a user
 *    who left the app for a few minutes can still type the first code) and gives it 10 more
 *    minutes, but a code never lives longer than 30 minutes from when it was made.
 *  - Waits between sends grow: 30 s, 60 s, 2 min, then 5 min. At most 5 codes an hour, 10 a day per number.
 *  - 5 wrong tries lock the code (a resend does not reset the tries); the next request makes a new code.
 *  - One internet address (IP) can request at most 20 codes an hour (stops SMS-pumping bots).
 */
export const CODE_TTL_MS = 10 * 60 * 1000;
export const CODE_MAX_LIFE_MS = 30 * 60 * 1000;
export const MAX_VERIFY_ATTEMPTS = 5;
export const RESEND_GAP_MS = 30 * 1000;
export const MAX_RESEND_GAP_MS = 5 * 60 * 1000;
export const MAX_PER_HOUR = 5;
export const MAX_PER_DAY = 10;
export const MAX_PER_IP_HOUR = 20;
const HOUR_MS = 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;

/** "+91XXXXXXXXXX" for an Indian mobile in any common form, else null. */
export function indianE164(raw: unknown): string | null {
  const digits = String(raw ?? "").replace(/\D/g, "");
  if (/^[6-9]\d{9}$/.test(digits)) return `+91${digits}`;
  if (/^91[6-9]\d{9}$/.test(digits)) return `+${digits}`;
  if (/^0[6-9]\d{9}$/.test(digits)) return `+91${digits.slice(1)}`;
  return null;
}

export function newCode(): string {
  return String(randomInt(0, 1_000_000)).padStart(6, "0");
}

export function hashCode(phone: string, code: string): string {
  return createHash("sha256").update(`${phone}:${code}`).digest("hex");
}

export function codeMatches(phone: string, code: string, storedHash: string): boolean {
  const a = Buffer.from(hashCode(phone, code), "hex");
  const b = Buffer.from(String(storedHash || ""), "hex");
  return a.length === b.length && timingSafeEqual(a, b);
}

/** IST calendar day, e.g. "2026-10-02". */
export function istDayKey(nowMs: number): string {
  return new Date(nowMs + IST_OFFSET_MS).toISOString().slice(0, 10);
}

export interface SendCounters {
  lastSentAt: number;
  hourStart: number;
  hourCount: number;
  dayKey: string;
  dayCount: number;
}

export type SendDecision =
  | { ok: true; next: SendCounters }
  | { ok: false; reason: "wait" | "hour" | "day"; retryAfterSec: number };

/** Wait before the next send, growing with the codes sent this hour: 30 s, 60 s, 2 min, 4→5 min. */
export function resendGapMs(sentThisHour: number): number {
  return Math.min(RESEND_GAP_MS * 2 ** Math.max(0, sentThisHour - 1), MAX_RESEND_GAP_MS);
}

/** Per-number limits: growing wait between codes, 5 an hour, 10 a day. */
export function decideSend(prev: Partial<SendCounters> | null, nowMs: number): SendDecision {
  const last = Number(prev?.lastSentAt || 0);
  const sameHour = prev?.hourStart && nowMs - Number(prev.hourStart) < HOUR_MS;
  const gap = resendGapMs(sameHour ? Number(prev?.hourCount || 0) : 0);
  if (last && nowMs - last < gap) {
    return { ok: false, reason: "wait", retryAfterSec: Math.ceil((gap - (nowMs - last)) / 1000) };
  }
  const day = istDayKey(nowMs);
  const dayCount = prev?.dayKey === day ? Number(prev?.dayCount || 0) : 0;
  if (dayCount >= MAX_PER_DAY) return { ok: false, reason: "day", retryAfterSec: 6 * 60 * 60 };
  const hourStart = prev?.hourStart && nowMs - Number(prev.hourStart) < HOUR_MS ? Number(prev.hourStart) : nowMs;
  const hourCount = hourStart === Number(prev?.hourStart) ? Number(prev?.hourCount || 0) : 0;
  if (hourCount >= MAX_PER_HOUR) {
    return { ok: false, reason: "hour", retryAfterSec: Math.ceil((hourStart + HOUR_MS - nowMs) / 1000) };
  }
  return { ok: true, next: { lastSentAt: nowMs, hourStart, hourCount: hourCount + 1, dayKey: day, dayCount: dayCount + 1 } };
}

/**
 * Resend within the window: reuse the code (true) unless it expired, was used, got locked by wrong
 * tries, or is older than [CODE_MAX_LIFE_MS].
 */
export function reuseCode(prev: { hasCode: boolean; expiresAt: number; issuedAt: number; attempts: number } | null,
  nowMs: number): boolean {
  if (!prev || !prev.hasCode) return false;
  return prev.expiresAt > nowMs && nowMs - prev.issuedAt < CODE_MAX_LIFE_MS && prev.attempts < MAX_VERIFY_ATTEMPTS;
}

/** New expiry for a resent code: 10 more minutes, never past 30 minutes from issue. */
export function resentExpiry(issuedAt: number, nowMs: number): number {
  return Math.min(nowMs + CODE_TTL_MS, issuedAt + CODE_MAX_LIFE_MS);
}

/**
 * The code itself is kept encrypted (AES-256-GCM) only so a resend can send the same digits; the
 * check uses the hash. Key: OTP_SECRET (functions env), else the WhatsApp token, else the project.
 */
function otpKey(env: NodeJS.ProcessEnv): Buffer {
  const material = (env.OTP_SECRET || env.WHATSAPP_TOKEN || env.GCLOUD_PROJECT || "dutype").trim();
  return createHash("sha256").update(`dutype-otp:${material}`).digest();
}

export function sealCode(code: string, env: NodeJS.ProcessEnv = process.env): string {
  const iv = randomBytes(12);
  const c = createCipheriv("aes-256-gcm", otpKey(env), iv);
  const enc = Buffer.concat([c.update(code, "utf8"), c.final()]);
  return [iv, c.getAuthTag(), enc].map((b) => b.toString("base64")).join(".");
}

export function openCode(sealed: string, env: NodeJS.ProcessEnv = process.env): string | null {
  try {
    const [iv, tag, enc] = String(sealed || "").split(".").map((x) => Buffer.from(x, "base64"));
    const d = createDecipheriv("aes-256-gcm", otpKey(env), iv);
    d.setAuthTag(tag);
    const code = Buffer.concat([d.update(enc), d.final()]).toString("utf8");
    return /^\d{6}$/.test(code) ? code : null;
  } catch {
    return null;
  }
}

/** Short, non-reversible key for an IP address (we never store raw IPs). */
export function ipKey(ip: string, nowMs: number): string {
  const hour = Math.floor(nowMs / HOUR_MS);
  return createHash("sha256").update(`ip:${ip}`).digest("hex").slice(0, 20) + `_${hour}`;
}

export interface TruecallerUser {
  phone: string | null;
  name: string;
  email: string;
}

/** The fields DutyPe uses from Truecaller's userinfo response. */
export function parseTruecallerUserInfo(raw: unknown): TruecallerUser {
  const u = (raw && typeof raw === "object" ? raw : {}) as Record<string, unknown>;
  const phone = indianE164(u.phone_number ?? u.phoneNumber);
  const name = [u.given_name, u.family_name].map((v) => String(v ?? "").trim()).filter(Boolean).join(" ") ||
    String(u.name ?? "").trim();
  const email = String(u.email ?? "").trim().toLowerCase();
  return {
    phone,
    name: name.replace(/\s+/g, " ").slice(0, 80),
    email: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email) && email.length <= 120 ? email : "",
  };
}

/** WhatsApp is set up when the token secret holds a real value (a placeholder like "unset" is not). */
export function whatsappConfigured(env: NodeJS.ProcessEnv = process.env): boolean {
  const token = (env.WHATSAPP_TOKEN || "").trim();
  return Boolean(token && token.toLowerCase() !== "unset" && env.WHATSAPP_PHONE_NUMBER_ID && env.WHATSAPP_TEMPLATE);
}

/**
 * Our own SMS gateway (DLT-registered OTP template), ~₹0.15–0.25 per SMS instead of Firebase's
 * ~₹6.7. SMS_PROVIDER = "2factor" (SMS_API_KEY, SMS_TEMPLATE = the 2Factor template name) or
 * "msg91" (SMS_API_KEY = authkey, SMS_TEMPLATE = MSG91 template id).
 */
export function smsGatewayConfigured(env: NodeJS.ProcessEnv = process.env): boolean {
  const provider = (env.SMS_PROVIDER || "").toLowerCase();
  return (provider === "2factor" || provider === "msg91") && Boolean((env.SMS_API_KEY || "").trim()) &&
    Boolean((env.SMS_TEMPLATE || "").trim());
}

/** The HTTP request for the configured gateway (pure, so it can be tested). */
export function smsGatewayRequest(toE164: string, code: string, env: NodeJS.ProcessEnv = process.env):
  { url: string; init: { method: string; headers?: Record<string, string> } } {
  const ten = toE164.replace(/^\+91/, "");
  const key = (env.SMS_API_KEY || "").trim();
  const template = (env.SMS_TEMPLATE || "").trim();
  if ((env.SMS_PROVIDER || "").toLowerCase() === "msg91") {
    const q = new URLSearchParams({ template_id: template, mobile: `91${ten}`, otp: code });
    return { url: `https://control.msg91.com/api/v5/otp?${q}`, init: { method: "POST", headers: { authkey: key, "Content-Type": "application/json" } } };
  }
  return {
    url: `https://2factor.in/API/V1/${encodeURIComponent(key)}/SMS/${ten}/${code}/${encodeURIComponent(template)}`,
    init: { method: "GET" },
  };
}

/** Meta Cloud API body for an authentication template (body code + copy-code button). */
export function whatsappTemplateBody(toE164: string, code: string, template: string, lang: string) {
  return {
    messaging_product: "whatsapp",
    to: toE164.replace(/^\+/, ""),
    type: "template",
    template: {
      name: template,
      language: { code: lang },
      components: [
        { type: "body", parameters: [{ type: "text", text: code }] },
        { type: "button", sub_type: "url", index: "0", parameters: [{ type: "text", text: code }] },
      ],
    },
  };
}
