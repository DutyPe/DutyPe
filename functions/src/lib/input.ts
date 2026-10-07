/**
 * Callable input readers. Each throws `invalid-argument` with a user-readable message,
 * so handlers read their payload top to bottom without branching.
 */
import * as functions from "firebase-functions";

export function fail(code: functions.https.FunctionsErrorCode, message: string): never {
  throw new functions.https.HttpsError(code, message);
}

type Obj = Record<string, unknown>;

export function obj(value: unknown): Obj {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Obj : {};
}

export function str(
  data: Obj, key: string, opts: { min?: number; max: number; optional?: boolean; pattern?: RegExp },
): string {
  const raw = data[key];
  const value = typeof raw === "string" ? raw.trim().replace(/\s+/g, " ") : "";
  if (!value) {
    if (opts.optional) return "";
    fail("invalid-argument", `${key} is required`);
  }
  if (value.length < (opts.min ?? 1)) fail("invalid-argument", `${key} is too short`);
  if (value.length > opts.max) fail("invalid-argument", `${key} is too long`);
  if (opts.pattern && !opts.pattern.test(value)) fail("invalid-argument", `${key} is invalid`);
  return value;
}

/** Multi-line free text: keeps line breaks, collapses runs of spaces. */
export function text(data: Obj, key: string, opts: { min?: number; max: number; optional?: boolean }): string {
  const raw = data[key];
  const value = typeof raw === "string" ? raw.replace(/[ \t]+/g, " ").replace(/\n{3,}/g, "\n\n").trim() : "";
  if (!value) {
    if (opts.optional) return "";
    fail("invalid-argument", `${key} is required`);
  }
  if (value.length < (opts.min ?? 1)) fail("invalid-argument", `${key} is too short`);
  if (value.length > opts.max) fail("invalid-argument", `${key} is too long`);
  return value;
}

export function int(data: Obj, key: string, opts: { min: number; max: number; optional?: boolean }): number {
  const raw = data[key];
  if (raw === undefined || raw === null || raw === "") {
    if (opts.optional) return opts.min;
    fail("invalid-argument", `${key} is required`);
  }
  const value = Number(raw);
  if (!Number.isInteger(value)) fail("invalid-argument", `${key} must be a whole number`);
  if (value < opts.min || value > opts.max) fail("invalid-argument", `${key} must be ${opts.min}–${opts.max}`);
  return value;
}

export function oneOf<T extends string>(data: Obj, key: string, allowed: readonly T[], fallback?: T): T {
  const raw = typeof data[key] === "string" ? (data[key] as string).trim().toUpperCase() : "";
  const match = allowed.find((v) => v.toUpperCase() === raw);
  if (match) return match;
  if (fallback !== undefined && !raw) return fallback;
  return fail("invalid-argument", `${key} is invalid`);
}

export function stringList(data: Obj, key: string, opts: { maxItems: number; maxLength: number }): string[] {
  const raw = data[key];
  if (raw === undefined || raw === null) return [];
  if (!Array.isArray(raw)) fail("invalid-argument", `${key} must be a list`);
  const values = raw
    .filter((v): v is string => typeof v === "string")
    .map((v) => v.trim())
    .filter(Boolean)
    .map((v) => (v.length > opts.maxLength ? v.slice(0, opts.maxLength).trim() : v))
    .slice(0, opts.maxItems);
  return Array.from(new Set(values));
}

export function latLng(data: Obj): { lat: number; lng: number } {
  const lat = Number(data.lat);
  const lng = Number(data.lng);
  const valid = Number.isFinite(lat) && Number.isFinite(lng) &&
    lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180 && !(lat === 0 && lng === 0);
  if (!valid) fail("invalid-argument", "A valid location is required");
  return { lat, lng };
}

/** Indian mobile number → 10 digits, or throws. */
export function mobile(data: Obj, key: string): string {
  const digits = String(data[key] ?? "").replace(/\D/g, "").replace(/^(91|0)(?=\d{10}$)/, "");
  if (!/^[6-9]\d{9}$/.test(digits)) fail("invalid-argument", `${key} must be a valid 10-digit mobile number`);
  return digits;
}

/** A download URL from this project's Storage bucket (never an arbitrary URL). */
export function storageUrl(data: Obj, key: string): string {
  const value = str(data, key, { max: 1000, optional: true });
  if (!value) return "";
  if (!/^https:\/\/firebasestorage\.googleapis\.com\//.test(value)) fail("invalid-argument", `${key} is invalid`);
  return value;
}

/** Client-generated request id used as the idempotency key. */
export function requestId(data: Obj): string {
  return str(data, "requestId", { min: 8, max: 64, pattern: /^[A-Za-z0-9_-]+$/ });
}
