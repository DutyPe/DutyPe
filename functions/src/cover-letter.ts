/**
 * Cover letter signed-URL service — HARDENED
 *
 * Issues a short-lived V4 signed URL so an employer can read a worker's
 * cover letter ONLY when:
 *   • caller is authenticated
 *   • an `applications/{jobId}_{workerId}` doc exists
 *   • that application's employerId === caller's uid
 *
 * This replaces the old rule that let any signed-in user read every cover letter.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { requirePerUserRateLimit, assertAppCheck } from "./validation";

const db = admin.firestore();
const SIGNED_URL_TTL_MS = 10 * 60 * 1000; // 10 minutes

export const getCoverLetterSignedUrl = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "login required");
  }
  assertAppCheck(context);
  const employerUid = context.auth.uid;

  await requirePerUserRateLimit(employerUid, "getCoverLetterSignedUrl", {
    perMinute: 20, perHour: 200, perDay: 1000,
  });

  const applicationId = String(data?.applicationId ?? "").trim();
  if (!applicationId || applicationId.length > 200 || !applicationId.includes("_")) {
    throw new functions.https.HttpsError("invalid-argument", "applicationId invalid");
  }

  const appSnap = await db.collection("applications").doc(applicationId).get();
  if (!appSnap.exists) {
    throw new functions.https.HttpsError("not-found", "application not found");
  }
  const app = appSnap.data() || {};
  if (app.employerId !== employerUid) {
    throw new functions.https.HttpsError("permission-denied", "not your application");
  }
  const workerId = String(app.workerId ?? "");
  if (!workerId) {
    throw new functions.https.HttpsError("failed-precondition", "application missing workerId");
  }

  // Optional explicit file path from client; default to canonical path.
  const explicitPath = data?.objectPath ? String(data.objectPath) : "";
  const objectPath = explicitPath || `cover_letters/${workerId}/cover_letter.pdf`;

  // Enforce that the path is under the worker's own folder.
  if (!objectPath.startsWith(`cover_letters/${workerId}/`)) {
    throw new functions.https.HttpsError("permission-denied", "path not allowed");
  }

  const bucket = admin.storage().bucket();
  const file = bucket.file(objectPath);
  const [exists] = await file.exists();
  if (!exists) {
    throw new functions.https.HttpsError("not-found", "cover letter not uploaded");
  }

  const expiresAt = Date.now() + SIGNED_URL_TTL_MS;
  const [url] = await file.getSignedUrl({
    version: "v4",
    action: "read",
    expires: expiresAt,
  });

  functions.logger.info(`cover-letter: signed url issued employer=${employerUid} application=${applicationId}`);
  return { url, expiresAt };
});
