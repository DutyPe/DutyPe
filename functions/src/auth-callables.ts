/**
 * Auth, role, and application callables for DutyPe.
 *
 * All callables:
 *   • require Firebase Auth (uid from context.auth.uid)
 *   • enforce App Check via onCallSecured
 *   • require an `idempotencyKey` from the client (replay-safe)
 *   • run in asia-south1
 *
 * Identity now lives in phoneRoles/{phoneE164}; role-owned profile data lives
 * in worker_profiles/{uid} or employer_profiles/{uid}.
 *
 * #9 / #20 fix: phoneRoles/{phoneE164} stores
 *   { phoneNumber, role, uid, name, createdAt, updatedAt }
 * so any phone-aware lookup is a single doc read AND the registration path
 * hard-blocks dual roles (the same phone cannot register as both worker and
 * employer).
 */
import * as admin from "firebase-admin";
import * as functions from "firebase-functions";
import { onCallSecured } from "./secure-callable";
import { withIdempotency } from "./idempotency";
import { validateString, validateEnum } from "./validation";

const db = () => admin.firestore();

const VALID_ROLES = ["WORKER", "EMPLOYER"] as const;
type Role = typeof VALID_ROLES[number];

interface UserEventPayload {
  [key: string]: unknown;
}

// ────────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────────

function normalizePhoneE164(raw: unknown): string | null {
  if (!raw) return null;
  const trimmed = String(raw).trim();
  if (!/^\+[1-9][0-9]{6,14}$/.test(trimmed)) return null;
  return trimmed;
}

function normalizeReferralCode(raw: unknown): string {
  return String(raw ?? "")
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
}

async function logUserEvent(
  uid: string,
  type: string,
  role: string,
  payload: UserEventPayload = {}
): Promise<void> {
  try {
    await db().collection("user_events").add({
      uid,
      type,
      role,
      payload,
      at: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (e) {
    functions.logger.warn(`user_events write failed: ${type}`, e);
  }
}

async function fireAndForgetReferral(
  uid: string,
  role: Role,
  fullName: string,
  referralCode: string
): Promise<void> {
  // Best-effort post-registration referral application. Failure here must
  // not break registration; the existing applyReferralCode callable will
  // also be invoked by the client as a fallback.
  try {
    await db()
      .collection("pending_referral_applications")
      .doc(uid)
      .set({
        uid,
        role,
        fullName,
        referralCode,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        status: "queued",
      });
  } catch (e) {
    functions.logger.warn("queue pending_referral_applications failed", e);
  }
}

function alreadyExistedToEvent(alreadyExisted: boolean, _role: string): string {
  return alreadyExisted ? "role_added" : "signup";
}

// ────────────────────────────────────────────────────────────────────────
// completeRegistration
// ────────────────────────────────────────────────────────────────────────
// Single-role-per-phone enforcement (#9 / #20):
//   • If phoneRoles/{phoneE164} already exists for a DIFFERENT uid → block.
//   • If it exists for the SAME uid but with a DIFFERENT role → block with
//     `failed-precondition` so the client can show "phone is registered as
//     EMPLOYER, please log in as employer" toast instead of silently merging.
//   • On success, write the extended snapshot {phone, uid, role, name, createdAt}.

export const completeRegistration = onCallSecured(
  {},
  async (data: any, context) => {
    const uid = context.auth!.uid;
    const phoneE164 = normalizePhoneE164(context.auth!.token?.phone_number);
    const fullName = validateString(data?.fullName, "fullName", {
      required: true,
      minLength: 1,
      maxLength: 80,
    });
    const role = validateEnum(data?.role, "role", VALID_ROLES as unknown as string[]) as Role;
    const referralCode = data?.referralCode ? normalizeReferralCode(data.referralCode) : "";

    if (!phoneE164) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Phone number on auth token is missing or not in E.164 format"
      );
    }

    const idem = await withIdempotency(uid, "completeRegistration", data?.idempotencyKey);
    if (idem.hit) return idem.result;

    const phoneRoleRef = db().collection("phoneRoles").doc(phoneE164);
    const profileRef = db()
      .collection(role === "WORKER" ? "worker_profiles" : "employer_profiles")
      .doc(uid);

    const result = await db().runTransaction(async (tx) => {
      const [phoneSnap, profileSnap] = await Promise.all([tx.get(phoneRoleRef), tx.get(profileRef)]);

      // Phone uniqueness: if phoneRoles already maps to another uid, refuse.
      if (phoneSnap.exists) {
        const phoneData = phoneSnap.data() || {};
        const owner = phoneData.uid as string | undefined;
        if (owner && owner !== uid) {
          throw new functions.https.HttpsError(
            "already-exists",
            "Phone number is already linked to another account"
          );
        }
        // Single-role-per-phone (#9 / #20): block role change on same phone.
        const existingRole = String(phoneData.role || "").toUpperCase();
        if (existingRole && existingRole !== role) {
          throw new functions.https.HttpsError(
            "failed-precondition",
            `phone-already-registered-as:${existingRole}`
          );
        }
      }

      const now = admin.firestore.Timestamp.now();
      const existing = (profileSnap.data() || {}) as Record<string, any>;
      const existingRole = String(existing.role || "").toUpperCase();

      // Single-role enforcement (#20). If the user already has a role and it
      // differs from the request, refuse — even if the phoneRoles doc was
      // somehow missing (defence in depth).
      if (existingRole && existingRole !== role) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `phone-already-registered-as:${existingRole}`
        );
      }

      const alreadyExisted = phoneSnap.exists || profileSnap.exists;

      const profileData: Record<string, any> = {
        userId: uid,
        phone: phoneE164,
        fullName: (existing.fullName as string | undefined)?.trim() || fullName,
        role,
        createdAt: existing.createdAt || now,
        updatedAt: now,
      };
      if (role === "EMPLOYER") profileData.companyName = existing.companyName || fullName;
      if (existing.referralCode) profileData.referralCode = existing.referralCode;
      if (referralCode && !existing.referredByCode) {
        profileData.referredByCode = referralCode;
      } else if (existing.referredByCode) {
        profileData.referredByCode = existing.referredByCode;
      }
      if (existing.referredByUserId) profileData.referredByUserId = existing.referredByUserId;
      if (existing.profileImageUrl) profileData.profileImageUrl = existing.profileImageUrl;

      tx.set(profileRef, profileData, { merge: true });

      tx.set(
        phoneRoleRef,
        {
          phoneNumber: phoneE164,
          uid,
          role,
          name: profileData.fullName,
          createdAt: phoneSnap.exists ? phoneSnap.data()?.createdAt || now : now,
          updatedAt: now,
        }
      );

      return { alreadyExisted };
    });

    await logUserEvent(
      uid,
      alreadyExistedToEvent(result.alreadyExisted, role),
      role,
      {
        role,
        rolesCount: 1,
        hasReferral: !!referralCode,
      }
    );

    if (referralCode) {
      await fireAndForgetReferral(uid, role, fullName, referralCode);
    }

    const out = {
      success: true,
      userId: uid,
      role,
      alreadyExisted: result.alreadyExisted,
    };
    await idem.record!(out);
    return out;
  }
);

// ────────────────────────────────────────────────────────────────────────
// lookupPhoneRole — single-doc, role-aware phone lookup (#11 / #20)
// ────────────────────────────────────────────────────────────────────────
// Public (auth/App Check NOT required) so the LOGIN screen can
// pre-check before triggering OTP. Returns only the existing role/name —
// never the uid — to keep the surface privacy-safe.
export const lookupPhoneRole = onCallSecured(
  { requireAuth: false, enforceAppCheck: false },
  async (data: any, _context) => {
    const phoneE164 = normalizePhoneE164(data?.phone);
    if (!phoneE164) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "phone must be in E.164 format (e.g. +919876543210)"
      );
    }

    const requestedRoleRaw = String(data?.requestedRole || "").toUpperCase();
    const requestedRole =
      requestedRoleRaw === "WORKER" || requestedRoleRaw === "EMPLOYER"
        ? requestedRoleRaw
        : "";

    const snap = await db().collection("phoneRoles").doc(phoneE164).get();
    if (!snap.exists) {
      return { exists: false, roleConflict: false };
    }
    const d = snap.data() || {};

    const existingRole = String(d.role || "").toUpperCase();
    const name = String(d.name || "");
    const roleConflict =
      !!requestedRole && !!existingRole && requestedRole !== existingRole;

    return {
      exists: true,
      existingRole: existingRole || null,
      name: name || null,
      roleConflict,
    };
  }
);

// ────────────────────────────────────────────────────────────────────────
// getWorkerProfileForEmployer (#19)
// ────────────────────────────────────────────────────────────────────────
// Firestore rules block direct employer reads of `worker_profiles` because
// rules cannot iterate `applications` to verify the relationship. This
// callable bridges that gap: returns the merged user + worker_profile data
// only if the caller has at least one application from this worker
// (optionally scoped to a specific jobId).
export const getWorkerProfileForEmployer = onCallSecured(
  { enforceAppCheck: false },
  async (data: any, context) => {
    const uid = context.auth!.uid;
    const workerId = validateString(data?.workerId, "workerId", {
      required: true,
      minLength: 4,
      maxLength: 128,
    });
    const jobId = data?.jobId
      ? validateString(data.jobId, "jobId", { minLength: 4, maxLength: 128 })
      : "";

    // Authorise: caller must employ this worker via at least one application.
    let appQuery = db()
      .collection("applications")
      .where("employerId", "==", uid)
      .where("workerId", "==", workerId)
      .limit(1);
    if (jobId) {
      // Tightest scope: docId is `${jobId}_${workerId}`.
      const docId = `${jobId}_${workerId}`;
      const direct = await db().collection("applications").doc(docId).get();
      const directData = (direct.data() || {}) as Record<string, any>;
      if (!direct.exists || directData.employerId !== uid || directData.workerId !== workerId) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Caller is not the employer of this application"
        );
      }
    } else {
      const appSnap = await appQuery.get();
      if (appSnap.empty) {
        throw new functions.https.HttpsError(
          "permission-denied",
          "Caller does not employ this worker"
        );
      }
    }

    const workerSnap = await db().collection("worker_profiles").doc(workerId).get();

    if (!workerSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Worker profile not found");
    }

    const worker = (workerSnap.data() || {}) as Record<string, any>;

    const safeWorker: Record<string, any> = {};
    for (const k of [
      "userId",
      "fullName",
      "phone",
      "role",
      "location",
      "geohash",
      "skills",
      "jobTypes",
      "experience",
      "educationQualification",
      "bio",
      "gender",
      "dateOfBirth",
      "isAvailable",
      "rating",
      "ratingAvg",
      "totalRatings",
      "totalJobs",
      "completedJobs",
      "profileImageUrl",
      "email",
    ]) {
      if (worker[k] !== undefined) safeWorker[k] = worker[k];
    }

    const merged = { ...safeWorker, workerId };
    return { success: true, profile: merged };
  }
);

// ────────────────────────────────────────────────────────────────────────
// submitApplication
// ────────────────────────────────────────────────────────────────────────
const MIN_WORKER_PROFILE_SCORE = 80;
const MAX_APPLICATIONS_PER_HOUR = 10;

export const submitApplication = onCallSecured({}, async (data: any, context) => {
  const uid = context.auth!.uid;
  const jobId = validateString(data?.jobId, "jobId", {
    required: true,
    minLength: 4,
    maxLength: 128,
  });
  const idem = await withIdempotency(uid, "submitApplication", data?.idempotencyKey);
  if (idem.hit) return idem.result;

  // Rate limit: count applications by this worker in the last hour.
  const oneHourAgo = admin.firestore.Timestamp.fromMillis(
    Date.now() - 60 * 60 * 1000
  );
  const recentSnap = await db()
    .collection("applications")
    .where("workerId", "==", uid)
    .where("createdAt", ">", oneHourAgo)
    .limit(MAX_APPLICATIONS_PER_HOUR + 1)
    .get();
  if (recentSnap.size >= MAX_APPLICATIONS_PER_HOUR) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      `Application limit reached (${MAX_APPLICATIONS_PER_HOUR}/hour)`
    );
  }

  // Pre-checks outside the transaction (cheaper).
  const [workerProfileSnap, jobSnap] = await Promise.all([
    db().collection("worker_profiles").doc(uid).get(),
    db().collection("jobmetadata").doc(jobId).get(),
  ]);

  if (!workerProfileSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "Worker profile not set up");
  }
  const worker = (workerProfileSnap.data() || {}) as Record<string, any>;
  const profileRole = String(worker.role || "WORKER").toUpperCase();
  if (profileRole !== "WORKER") {
    throw new functions.https.HttpsError("failed-precondition", "WORKER role required");
  }
  const workerScoreRaw = worker.profileScore ?? 0;
  const workerScore = Number(workerScoreRaw);
  if (!Number.isFinite(workerScore) || workerScore < MIN_WORKER_PROFILE_SCORE) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Profile must be at least ${MIN_WORKER_PROFILE_SCORE}% complete to apply`
    );
  }
  if (!jobSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Job not found");
  }
  const job = (jobSnap.data() || {}) as Record<string, any>;
  const jobStatus = String(job.status || "").toLowerCase();
  if (jobStatus !== "active" && jobStatus !== "open") {
    throw new functions.https.HttpsError(
      "failed-precondition",
      `Job is not accepting applications (status=${jobStatus || "unknown"})`
    );
  }
  const employerId = String(job.employerId || "");
  if (!employerId) {
    throw new functions.https.HttpsError("failed-precondition", "Job is missing employerId");
  }
  if (employerId === uid) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "You cannot apply to your own job"
    );
  }

  const docId = `${jobId}_${uid}`;
  const appRef = db().collection("applications").doc(docId);
  const applicationData: Record<string, any> = {
    jobId,
    workerId: uid,
    employerId,
    status: "applied",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  };
  const workerName = String(worker.fullName || worker.name || "").trim();
  if (workerName) {
    applicationData.workerName = workerName;
  }

  const txResult = await db().runTransaction(async (tx) => {
    const existing = await tx.get(appRef);
    if (existing.exists) {
      return { alreadyApplied: true };
    }
    tx.set(appRef, applicationData);
    return { alreadyApplied: false };
  });

  if (!txResult.alreadyApplied) {
    await logUserEvent(uid, "application_submitted", "WORKER", { jobId, employerId });
  }

  const out = {
    success: true,
    applicationId: docId,
    alreadyApplied: txResult.alreadyApplied,
  };
  await idem.record!(out);
  return out;
});
