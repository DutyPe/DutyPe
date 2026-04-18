/**
 * P0 SECURITY FIX: Input Validation for Cloud Functions
 * 
 * Prevents injection attacks, XSS, and malformed data from reaching the database.
 * All Cloud Functions MUST validate inputs using these utilities.
 * 
 * @author DutyPe Security Team
 * @since 2.4.1
 */

import * as functions from "firebase-functions";

/**
 * Validate string input
 */
export function validateString(
  value: any,
  fieldName: string,
  options: {
    required?: boolean;
    minLength?: number;
    maxLength?: number;
    pattern?: RegExp;
  } = {}
): string {
  // Check if required
  if (options.required && (value === null || value === undefined || value === "")) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} is required`
    );
  }

  // If not required and empty, return empty string
  if (!value) {
    return "";
  }

  // Check type
  if (typeof value !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be a string`
    );
  }

  // Trim whitespace
  const trimmed = value.trim();

  // Check min length
  if (options.minLength && trimmed.length < options.minLength) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be at least ${options.minLength} characters`
    );
  }

  // Check max length
  if (options.maxLength && trimmed.length > options.maxLength) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be at most ${options.maxLength} characters`
    );
  }

  // Check pattern
  if (options.pattern && !options.pattern.test(trimmed)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} has invalid format`
    );
  }

  return trimmed;
}

/**
 * Validate number input
 */
export function validateNumber(
  value: any,
  fieldName: string,
  options: {
    required?: boolean;
    min?: number;
    max?: number;
    integer?: boolean;
  } = {}
): number {
  // Check if required
  if (options.required && (value === null || value === undefined)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} is required`
    );
  }

  // If not required and empty, return 0
  if (value === null || value === undefined) {
    return 0;
  }

  // Convert to number
  const num = Number(value);

  // Check if valid number
  if (isNaN(num)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be a valid number`
    );
  }

  // Check if integer required
  if (options.integer && !Number.isInteger(num)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be an integer`
    );
  }

  // Check min
  if (options.min !== undefined && num < options.min) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be at least ${options.min}`
    );
  }

  // Check max
  if (options.max !== undefined && num > options.max) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be at most ${options.max}`
    );
  }

  return num;
}

/**
 * Validate boolean input
 */
export function validateBoolean(
  value: any,
  fieldName: string,
  required: boolean = false
): boolean {
  if (required && (value === null || value === undefined)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} is required`
    );
  }

  if (value === null || value === undefined) {
    return false;
  }

  if (typeof value !== "boolean") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be a boolean`
    );
  }

  return value;
}

/**
 * Validate email format
 */
export function validateEmail(email: any, required: boolean = false): string {
  const emailStr = validateString(email, "email", {
    required,
    maxLength: 255,
    pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  });

  return emailStr.toLowerCase();
}

/**
 * Validate phone number format (Indian format)
 */
export function validatePhone(phone: any, required: boolean = false): string {
  return validateString(phone, "phone", {
    required,
    minLength: 10,
    maxLength: 15,
    pattern: /^\+?[1-9]\d{9,14}$/,
  });
}

/**
 * Validate user ID format
 */
export function validateUserId(userId: any, required: boolean = true): string {
  return validateString(userId, "userId", {
    required,
    minLength: 10,
    maxLength: 128,
    pattern: /^[a-zA-Z0-9_-]+$/,
  });
}

/**
 * Validate document ID format
 */
export function validateDocumentId(
  docId: any,
  fieldName: string = "documentId",
  required: boolean = true
): string {
  return validateString(docId, fieldName, {
    required,
    minLength: 10,
    maxLength: 128,
    pattern: /^[a-zA-Z0-9_-]+$/,
  });
}

/**
 * Sanitize text to prevent XSS
 * Removes HTML tags and dangerous characters
 */
export function sanitizeText(text: string): string {
  if (!text) return "";

  return text
    .replace(/<[^>]*>/g, "") // Remove HTML tags
    .replace(/[<>'"]/g, "") // Remove dangerous characters
    .trim();
}

/**
 * Validate and sanitize message text
 */
export function validateMessage(
  message: any,
  maxLength: number = 1000
): string {
  const messageStr = validateString(message, "message", {
    required: true,
    minLength: 1,
    maxLength,
  });

  return sanitizeText(messageStr);
}

/**
 * Validate array input
 */
export function validateArray(
  value: any,
  fieldName: string,
  options: {
    required?: boolean;
    minLength?: number;
    maxLength?: number;
  } = {}
): any[] {
  if (options.required && (!value || !Array.isArray(value))) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} is required and must be an array`
    );
  }

  if (!value) {
    return [];
  }

  if (!Array.isArray(value)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be an array`
    );
  }

  if (options.minLength && value.length < options.minLength) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must have at least ${options.minLength} items`
    );
  }

  if (options.maxLength && value.length > options.maxLength) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must have at most ${options.maxLength} items`
    );
  }

  return value;
}

/**
 * Validate enum value
 */
export function validateEnum<T>(
  value: any,
  fieldName: string,
  allowedValues: T[],
  required: boolean = true
): T {
  if (required && (value === null || value === undefined)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} is required`
    );
  }

  if (!allowedValues.includes(value)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${fieldName} must be one of: ${allowedValues.join(", ")}`
    );
  }

  return value;
}

/**
 * Rate limiting — Firestore-transaction backed. Survives cold starts and
 * scales across Cloud Functions instances. Writes to /_rate_limits/{uid__action}
 * which is denied to all clients (see firestore.rules).
 */
import * as admin from "firebase-admin";

export async function requirePerUserRateLimit(
  userId: string,
  action: string,
  opts: { perMinute?: number; perHour?: number; perDay?: number } = {}
): Promise<void> {  const perMinute = opts.perMinute ?? Number.MAX_SAFE_INTEGER;
  const perHour = opts.perHour ?? Number.MAX_SAFE_INTEGER;
  const perDay = opts.perDay ?? Number.MAX_SAFE_INTEGER;

  const ref = admin.firestore().doc(`_rate_limits/${userId}__${action}`);
  const now = Date.now();

  await admin.firestore().runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const d = (snap.data() as {
      minuteStart?: number; minuteCount?: number;
      hourStart?: number; hourCount?: number;
      dayStart?: number; dayCount?: number;
    }) ?? {};

    const minuteStart = (d.minuteStart ?? 0);
    const hourStart = (d.hourStart ?? 0);
    const dayStart = (d.dayStart ?? 0);
    const minuteCount = now - minuteStart > 60_000 ? 0 : (d.minuteCount ?? 0);
    const hourCount = now - hourStart > 3_600_000 ? 0 : (d.hourCount ?? 0);
    const dayCount = now - dayStart > 86_400_000 ? 0 : (d.dayCount ?? 0);

    if (minuteCount >= perMinute || hourCount >= perHour || dayCount >= perDay) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        `rate limit exceeded for ${action}`
      );
    }

    tx.set(ref, {
      minuteStart: now - minuteStart > 60_000 ? now : minuteStart,
      minuteCount: minuteCount + 1,
      hourStart: now - hourStart > 3_600_000 ? now : hourStart,
      hourCount: hourCount + 1,
      dayStart: now - dayStart > 86_400_000 ? now : dayStart,
      dayCount: dayCount + 1,
      lastActionAt: now,
    });
  });
}

/**
 * Require a valid App Check token on a callable request. Rejects the call
 * with failed-precondition if absent. Use on sensitive paths (money,
 * identity lookups, signed URL issuers).
 */
export function assertAppCheck(context: functions.https.CallableContext): void {
  if (!context.app) {
    throw new functions.https.HttpsError("failed-precondition", "app check required");
  }
}

/**
 * @deprecated In-memory rate limiter. Resets on cold start — do not use for
 * money/abuse paths. Kept only so unreferenced call-sites still compile;
 * all critical paths must use `requirePerUserRateLimit`.
 */
const rateLimitMap = new Map<string, { count: number; resetAt: number }>();

export function checkRateLimit(
  userId: string,
  action: string,
  maxRequests: number = 10,
  windowMs: number = 60000 // 1 minute
): void {
  const key = `${userId}:${action}`;
  const now = Date.now();
  const limit = rateLimitMap.get(key);

  if (!limit || now > limit.resetAt) {
    // Reset or create new limit
    rateLimitMap.set(key, {
      count: 1,
      resetAt: now + windowMs,
    });
    return;
  }

  if (limit.count >= maxRequests) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      `Too many requests. Please try again later.`
    );
  }

  limit.count++;
}

/**
 * Clean up expired rate limit entries (call periodically)
 */
export function cleanupRateLimits(): void {
  const now = Date.now();
  for (const [key, limit] of rateLimitMap.entries()) {
    if (now > limit.resetAt) {
      rateLimitMap.delete(key);
    }
  }
}
