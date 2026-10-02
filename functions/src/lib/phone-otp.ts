/**
 * Pure helpers for WhatsApp login codes and Truecaller profiles (no Firebase, unit tested).
 */
import { createHash, randomInt, timingSafeEqual } from "crypto";

export const CODE_TTL_MS = 10 * 60 * 1000;
export const MAX_VERIFY_ATTEMPTS = 5;
export const RESEND_GAP_MS = 30 * 1000;
export const MAX_PER_HOUR = 5;
export const MAX_PER_DAY = 10;
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

/** Per-number limits: one code every 30 s, 5 an hour, 10 a day. */
export function decideSend(prev: Partial<SendCounters> | null, nowMs: number): SendDecision {
  const last = Number(prev?.lastSentAt || 0);
  if (last && nowMs - last < RESEND_GAP_MS) {
    return { ok: false, reason: "wait", retryAfterSec: Math.ceil((RESEND_GAP_MS - (nowMs - last)) / 1000) };
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
