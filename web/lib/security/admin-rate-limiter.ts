import { NextRequest } from "next/server";

export interface RateLimitOptions {
  windowMs?: number; // Duration of window in ms (default: 60,000ms = 1 minute)
  maxRequests?: number; // Max requests allowed per window (default: 60)
  sensitive?: boolean; // If true, applies strict threshold (default: 25)
  keyPrefix?: string; // Optional namespace prefix to isolate rate limit buckets
}

export interface RateLimitResult {
  allowed: boolean;
  limit: number;
  remaining: number;
  resetSeconds: number;
}

export const RateLimitTiers = {
  AUTH_SESSION: { windowMs: 60 * 1000, maxRequests: 10, keyPrefix: "auth_session" },
  GENERAL: { windowMs: 60 * 1000, maxRequests: 60 },
  SENSITIVE: { windowMs: 60 * 1000, maxRequests: 25, sensitive: true },
  FINANCIAL: { windowMs: 60 * 60 * 1000, maxRequests: 10, sensitive: true, keyPrefix: "financial" },
  DESTRUCTIVE: { windowMs: 60 * 60 * 1000, maxRequests: 5, sensitive: true, keyPrefix: "destructive" },
  MEDIA_UPLOAD: { windowMs: 60 * 1000, maxRequests: 10, keyPrefix: "media_upload" }
} as const;

interface RequestRecord {
  timestamps: number[];
  lastCleanup: number;
}

// In-memory sliding window cache keyed by client identifier
const rateLimitMap = new Map<string, RequestRecord>();

// Periodic garbage collection every 5 minutes to prevent memory leaks
const GC_INTERVAL_MS = 5 * 60 * 1000;
let lastGlobalGc = Date.now();

function performGarbageCollection(now: number, maxWindowMs: number) {
  if (now - lastGlobalGc < GC_INTERVAL_MS) return;
  lastGlobalGc = now;

  for (const [key, record] of rateLimitMap.entries()) {
    record.timestamps = record.timestamps.filter((ts) => now - ts < maxWindowMs);
    if (record.timestamps.length === 0) {
      rateLimitMap.delete(key);
    }
  }
}

/**
 * Extracts client IP with Cloudflare and reverse-proxy precedence.
 * Prioritizes cf-connecting-ip to prevent x-forwarded-for spoofing.
 */
export function extractClientIp(request: NextRequest): string {
  const cfIp = request.headers.get("cf-connecting-ip");
  if (cfIp) return cfIp.trim();

  const realIp = request.headers.get("x-real-ip");
  if (realIp) return realIp.trim();

  const forwarded = request.headers.get("x-forwarded-for");
  if (forwarded) {
    const firstIp = forwarded.split(",")[0]?.trim();
    if (firstIp) return firstIp;
  }

  return "127.0.0.1";
}

/**
 * IP-level sliding window rate limit (used before authentication).
 */
export function checkIpRateLimit(
  request: NextRequest,
  options?: RateLimitOptions
): RateLimitResult {
  const ip = extractClientIp(request);
  const prefix = options?.keyPrefix || "ip_tier";
  return evaluateRateLimit(`ip:${prefix}:${ip}`, options);
}

/**
 * Evaluates sliding-window rate limit for admin operations.
 * Protects against scrapers, brute force inspection, and compromised admin credentials.
 */
export function checkAdminRateLimit(
  request: NextRequest,
  actor?: { uid: string; email: string } | null,
  options?: RateLimitOptions
): RateLimitResult {
  const ip = extractClientIp(request);
  const actorId = actor?.uid || "anon";
  const prefix = options?.keyPrefix ? `:${options.keyPrefix}` : "";
  const sensitiveTag = options?.sensitive ? ":sensitive" : "";
  const key = `${ip}:${actorId}${prefix}${sensitiveTag}`;

  return evaluateRateLimit(key, options);
}

function evaluateRateLimit(key: string, options?: RateLimitOptions): RateLimitResult {
  const now = Date.now();
  const windowMs = options?.windowMs ?? 60 * 1000;
  const defaultLimit = options?.sensitive ? 25 : 60;
  const limit = options?.maxRequests ?? defaultLimit;

  performGarbageCollection(now, windowMs);

  let record = rateLimitMap.get(key);
  if (!record) {
    record = { timestamps: [], lastCleanup: now };
    rateLimitMap.set(key, record);
  }

  // Filter timestamps outside current sliding window
  record.timestamps = record.timestamps.filter((ts) => now - ts < windowMs);

  const count = record.timestamps.length;
  if (count >= limit) {
    const oldest = record.timestamps[0] || now;
    const resetMs = Math.max(0, windowMs - (now - oldest));
    const resetSeconds = Math.max(1, Math.ceil(resetMs / 1000));
    return {
      allowed: false,
      limit,
      remaining: 0,
      resetSeconds
    };
  }

  record.timestamps.push(now);
  const remaining = Math.max(0, limit - record.timestamps.length);
  return {
    allowed: true,
    limit,
    remaining,
    resetSeconds: Math.ceil(windowMs / 1000)
  };
}
