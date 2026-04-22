/**
 * Job Posting Cloud Functions — HARDENED
 *
 * Security:
 *   • Strict field whitelist mirroring firestore.rules /jobmetadata schema.
 *   • employerId is ALWAYS derived from context.auth.uid (never client-supplied).
 *   • Idempotency check + creation run inside one transaction — no race doubles.
 *   • batchUpdateVacancyStatus only returns status for jobs the caller owns.
 */

import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { assertAppCheck } from "./validation";

const db = admin.firestore();

const JOB_ALLOWED_FIELDS: ReadonlyArray<string> = [
  "companyName",
  "title",
  "jobType",
  "salary",
  "salaryType",
  "location",   // { lat, lng }
  "geohash",
  "addressText",
  "urgency",
  "expiresAt",  // ms since epoch or ISO — converted to Timestamp server-side
];

const URGENCY_ENUM = new Set(["LOW", "MEDIUM", "HIGH"]);

function assertString(v: unknown, field: string, min: number, max: number): string {
  if (typeof v !== "string" || v.length < min || v.length > max) {
    throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
  }
  return v;
}

function assertNumber(v: unknown, field: string, min: number, max: number): number {
  if (typeof v !== "number" || !Number.isFinite(v) || v < min || v > max) {
    throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
  }
  return v;
}

function assertLatLng(v: unknown): { lat: number; lng: number } {
  if (
    !v || typeof v !== "object" ||
    typeof (v as any).lat !== "number" || typeof (v as any).lng !== "number" ||
    (v as any).lat < -90 || (v as any).lat > 90 ||
    (v as any).lng < -180 || (v as any).lng > 180
  ) {
    throw new functions.https.HttpsError("invalid-argument", "location invalid");
  }
  return { lat: (v as any).lat, lng: (v as any).lng };
}

function toTimestamp(v: unknown, field: string): admin.firestore.Timestamp {
  if (typeof v === "number" && Number.isFinite(v) && v > 0) {
    return admin.firestore.Timestamp.fromMillis(v);
  }
  if (typeof v === "string" && v.length > 0) {
    const parsed = Date.parse(v);
    if (!Number.isNaN(parsed)) return admin.firestore.Timestamp.fromMillis(parsed);
  }
  throw new functions.https.HttpsError("invalid-argument", `${field} invalid`);
}

export const createJobWithIdempotency = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "login required");
  }
  assertAppCheck(context);
  const uid = context.auth.uid;

  const { idempotencyKey, ...rest } = (data ?? {}) as Record<string, unknown>;

  if (typeof idempotencyKey !== "string" || idempotencyKey.length < 8 || idempotencyKey.length > 64) {
    throw new functions.https.HttpsError("invalid-argument", "idempotencyKey required (8..64 chars)");
  }

  // Strict whitelist — reject any client-supplied field outside the allowlist.
  for (const k of Object.keys(rest)) {
    if (!JOB_ALLOWED_FIELDS.includes(k)) {
      throw new functions.https.HttpsError("invalid-argument", `field not allowed: ${k}`);
    }
  }

  const companyName = assertString(rest.companyName, "companyName", 1, 120);
  const title = assertString(rest.title, "title", 3, 120);
  const jobType = assertString(rest.jobType, "jobType", 1, 80);
  const salary = assertString(rest.salary, "salary", 1, 60);
  const salaryType = assertString(rest.salaryType, "salaryType", 1, 40);
  const geohash = assertString(rest.geohash, "geohash", 1, 20);
  const addressText = assertString(rest.addressText, "addressText", 1, 300);
  const urgencyRaw = assertString(rest.urgency, "urgency", 1, 20);
  if (!URGENCY_ENUM.has(urgencyRaw)) {
    throw new functions.https.HttpsError("invalid-argument", "urgency invalid");
  }
  const location = assertLatLng(rest.location);
  const expiresAt = toTimestamp(rest.expiresAt, "expiresAt");

  try {
    // Idempotency + creation atomically inside one transaction.
    // employerId + expiresAt + idempotencyKey now live in job_details (slim jobmetadata).
    const result = await db.runTransaction(async (tx) => {
      const dup = await tx.get(
        db.collection("job_details")
          .where("employerId", "==", uid)
          .where("idempotencyKey", "==", idempotencyKey)
          .limit(1)
      );
      if (!dup.empty) {
        return { jobId: dup.docs[0].id, duplicate: true as const };
      }

      const metaRef = db.collection("jobmetadata").doc();
      const detailsRef = db.collection("job_details").doc(metaRef.id);
      const createdAt = admin.firestore.FieldValue.serverTimestamp();
      tx.set(metaRef, {
        companyName,
        title,
        jobType,
        salary,
        salaryType,
        location,
        geohash,
        addressText,
        urgency: urgencyRaw,
        status: "open",
        createdAt,
      });
      tx.set(detailsRef, {
        employerId: uid,                       // server-set — never trust client
        createdAt,
        expiresAt,
        idempotencyKey,
      });
      return { jobId: metaRef.id, duplicate: false as const };
    });

    functions.logger.info(`job-posting: ${result.duplicate ? "duplicate" : "created"} ${result.jobId} by ${uid}`);
    return {
      jobId: result.jobId,
      duplicate: result.duplicate,
      message: result.duplicate ? "Job already exists with this idempotency key" : "Job created successfully",
    };
  } catch (err: any) {
    if (err instanceof functions.https.HttpsError) throw err;
    functions.logger.error("createJobWithIdempotency failed:", err);
    throw new functions.https.HttpsError("internal", "Failed to create job");
  }
});

/**
 * Batch get job status — only for jobs the caller OWNS.
 */
export const batchUpdateVacancyStatus = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "login required");
  }
  const uid = context.auth.uid;

  const { jobIds } = (data ?? {}) as { jobIds?: unknown };
  if (!Array.isArray(jobIds) || jobIds.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "jobIds must be a non-empty array");
  }
  if (jobIds.length > 10) {
    throw new functions.https.HttpsError("invalid-argument", "Maximum 10 jobs per batch");
  }

  try {
    const results: { [k: string]: { status?: string; error?: string } } = {};
    for (const idRaw of jobIds) {
      const jobId = String(idRaw);
      const metaSnap = await db.collection("jobmetadata").doc(jobId).get();
      if (!metaSnap.exists) {
        results[jobId] = { error: "Job not found" };
        continue;
      }
      // Ownership lives in job_details (slim jobmetadata schema).
      const detailsSnap = await db.collection("job_details").doc(jobId).get();
      const ownerId = detailsSnap.exists ? (detailsSnap.get("employerId") as string | undefined) : undefined;
      if (ownerId !== uid) {
        // Ownership check — do NOT leak status of jobs you don't own.
        results[jobId] = { error: "Forbidden" };
        continue;
      }
      results[jobId] = { status: (metaSnap.get("status") as string | undefined) ?? "open" };
    }
    return { success: true, results };
  } catch (err: any) {
    functions.logger.error("batchUpdateVacancyStatus failed:", err);
    throw new functions.https.HttpsError("internal", "Batch status failed");
  }
});
