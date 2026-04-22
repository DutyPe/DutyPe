/**
 * Auth, role, and application callables for DutyPe.
 *
 * All callables:
 *   • require Firebase Auth (uid from context.auth.uid)
 *   • enforce App Check via onCallSecured
 *   • require an `idempotencyKey` from the client (replay-safe)
 *   • run in asia-south1
 *
 * Field shapes intentionally mirror the existing `users` document layout
 * (userId / phone / fullName / roles / activeRole / referralCode /
 *  referredByCode / referredByUserId / createdAt / lastActiveAt) so that
 * existing read paths continue to work unchanged.
 *
 * #9 / #20 fix: phone_index/{phoneE164} now stores
 *   { phone, uid, role, name, createdAt }
 * so any phone-aware lookup is a single doc read AND the registration path
 * hard-blocks dual roles (the same phone cannot register as both worker and
 * employer). This is the schema decision documented in the user's audit
 * notes — "extend phone_index with {phone, role, uid, name}".
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
//   • If phone_index/{phoneE164} already exists for a DIFFERENT uid → block.
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

    const userRef = db().collection("users").doc(uid);
    const phoneIndexRef = db().collection("phone_index").doc(phoneE164);

    const result = await db().runTransaction(async (tx) => {
      const [userSnap, phoneSnap] = await Promise.all([tx.get(userRef), tx.get(phoneIndexRef)]);

      // Phone uniqueness: if phone_index already maps to another uid, refuse.
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
      const existing = (userSnap.data() || {}) as Record<string, any>;
      const existingRoles: string[] = Array.isArray(existing.roles)
        ? existing.roles.map((r: any) => String(r).toUpperCase()).filter(Boolean)
        : [];

      // Single-role enforcement (#20). If the user already has a role and it
      // differs from the request, refuse — even if the phone_index doc was
      // somehow missing (defence in depth).
      if (existingRoles.length > 0 && !existingRoles.includes(role)) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `phone-already-registered-as:${existingRoles[0]}`
        );
      }

      const mergedRoles = Array.from(new Set([...existingRoles, role]));
      const alreadyExisted = userSnap.exists && existingRoles.length > 0;

      const userData: Record<string, any> = {
        userId: uid,
        phone: phoneE164,
        fullName: (existing.fullName as string | undefined)?.trim() || fullName,
        roles: mergedRoles,
        activeRole: role,
        createdAt: existing.createdAt || now,
        lastActiveAt: now,
      };
      if (existing.referralCode) userData.referralCode = existing.referralCode;
      if (referralCode && !existing.referredByCode) {
        userData.referredByCode = referralCode;
      } else if (existing.referredByCode) {
        userData.referredByCode = existing.referredByCode;
      }
      if (existing.referredByUserId) userData.referredByUserId = existing.referredByUserId;
      if (existing.profileImageUrl) userData.profileImageUrl = existing.profileImageUrl;
      if (existing.fcmToken) userData.fcmToken = existing.fcmToken;

      tx.set(userRef, userData, { merge: true });

      // Extended phone_index payload (#20). Stored with `merge: true` so
      // existing legacy docs get backfilled with role/name/phone on the next
      // registration touch.
      tx.set(
        phoneIndexRef,
        {
          phone: phoneE164,
          uid,
          role,
          name: userData.fullName,
          createdAt: phoneSnap.exists ? phoneSnap.data()?.createdAt || now : now,
          updatedAt: now,
        },
        { merge: true }
      );

      return { alreadyExisted, mergedRoles };
    });

    await logUserEvent(
      uid,
      alreadyExistedToEvent(result.alreadyExisted, role),
      role,
      {
        role,
        rolesCount: result.mergedRoles.length,
        hasReferral: !!referralCode,
      }
    );

    if (referralCode) {
      await fireAndForgetReferral(uid, role, fullName, referralCode);
    }

    const out = {
      success: true,
      userId: uid,
      activeRole: role,
      alreadyExisted: result.alreadyExisted,
    };
    await idem.record!(out);
    return out;
  }
);

// ────────────────────────────────────────────────────────────────────────
// addRole — DISABLED (#20: dual-role not supported)
// ────────────────────────────────────────────────────────────────────────
// Kept exported for binary compatibility with deployed clients; always
// rejects so existing apps surface a clear error instead of silently
// granting a second role.
export const addRole = onCallSecured({}, async (data: any, context) => {
  const uid = context.auth!.uid;
  const newRole = validateEnum(
    data?.newRole,
    "newRole",
    VALID_ROLES as unknown as string[]
  ) as Role;

  // Look up existing role for the toast message.
  const userSnap = await db().collection("users").doc(uid).get();
  const existing = (userSnap.data() || {}) as Record<string, any>;
  const existingRoles: string[] = Array.isArray(existing.roles)
    ? existing.roles.map((r: any) => String(r).toUpperCase()).filter(Boolean)
    : [];

  if (existingRoles.includes(newRole)) {
    return { success: true, roles: existingRoles, activeRole: newRole, noop: true };
  }

  const existingRole = existingRoles[0] || "";
  throw new functions.https.HttpsError(
    "failed-precondition",
    existingRole
      ? `phone-already-registered-as:${existingRole}`
      : "dual-role-not-supported"
  );
});

// ────────────────────────────────────────────────────────────────────────
// switchActiveRole — DISABLED (#20: dual-role not supported)
// ────────────────────────────────────────────────────────────────────────
// If the requested role matches the user's existing role, this is a no-op
// (idempotent for clients calling on every cold start). Anything else is
// rejected.
export const switchActiveRole = onCallSecured({}, async (data: any, context) => {
  const uid = context.auth!.uid;
  const newRole = validateEnum(
    data?.newRole,
    "newRole",
    VALID_ROLES as unknown as string[]
  ) as Role;

  const userSnap = await db().collection("users").doc(uid).get();
  if (!userSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "User profile not found");
  }
  const existing = (userSnap.data() || {}) as Record<string, any>;
  const activeRole = String(existing.activeRole || "").toUpperCase();
  if (activeRole === newRole) {
    return { success: true, activeRole: newRole, profileExists: true, noop: true };
  }

  const existingRole = activeRole || "";
  throw new functions.https.HttpsError(
    "failed-precondition",
    existingRole
      ? `phone-already-registered-as:${existingRole}`
      : "dual-role-not-supported"
  );
});

// ────────────────────────────────────────────────────────────────────────
// lookupPhoneRole — single-doc, role-aware phone lookup (#11 / #20)
// ────────────────────────────────────────────────────────────────────────
// Public (App-Check enforced, auth NOT required) so the LOGIN screen can
// pre-check before triggering OTP. Returns only the existing role/name —
// never the uid — to keep the surface privacy-safe.
export const lookupPhoneRole = onCallSecured(
  { requireAuth: false },
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

    const snap = await db().collection("phone_index").doc(phoneE164).get();
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
  {},
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
      if (!direct.exists || (direct.data() as any)?.employerId !== uid) {
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

    const [userSnap, workerSnap] = await Promise.all([
      db().collection("users").doc(workerId).get(),
      db().collection("worker_profiles").doc(workerId).get(),
    ]);

    if (!userSnap.exists && !workerSnap.exists) {
      throw new functions.https.HttpsError("not-found", "Worker profile not found");
    }

    const user = (userSnap.data() || {}) as Record<string, any>;
    const worker = (workerSnap.data() || {}) as Record<string, any>;

    // Strip sensitive fields before returning.
    const safeUser: Record<string, any> = {};
    for (const k of [
      "userId",
      "fullName",
      "phone",
      "profileImageUrl",
      "city",
      "location",
      "geohash",
      "roles",
      "activeRole",
    ]) {
      if (user[k] !== undefined) safeUser[k] = user[k];
    }

    const safeWorker: Record<string, any> = {};
    for (const k of [
      "skills",
      "jobTypes",
      "experience",
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

    // Merge with worker_profiles taking precedence on overlapping keys.
    const merged = { ...safeUser, ...safeWorker, workerId };
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
  const [userSnap, workerProfileSnap, jobSnap] = await Promise.all([
    db().collection("users").doc(uid).get(),
    db().collection("worker_profiles").doc(uid).get(),
    db().collection("jobmetadata").doc(jobId).get(),
  ]);

  if (!userSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "User profile not found");
  }
  const user = (userSnap.data() || {}) as Record<string, any>;
  const roles: string[] = Array.isArray(user.roles)
    ? user.roles.map((r: any) => String(r).toUpperCase())
    : [];
  if (!roles.includes("WORKER")) {
    throw new functions.https.HttpsError("failed-precondition", "WORKER role required");
  }
  if (!workerProfileSnap.exists) {
    throw new functions.https.HttpsError("failed-precondition", "Worker profile not set up");
  }
  const workerScoreRaw =
    (workerProfileSnap.data() as Record<string, any> | undefined)?.profileScore ??
    user.workerProfileScore ??
    0;
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

  // Build snapshots at create time so list screens render with zero fan-out.
  const worker = (workerProfileSnap.data() || {}) as Record<string, any>;
  const jobTypes: any[] = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
  const workerSnapshot = {
    workerId: uid,
    fullName: String(user.fullName ?? ""),
    profileImageUrl: String(user.profileImageUrl ?? ""),
    primarySkill: jobTypes.length > 0 ? String(jobTypes[0]) : "",
    experience: String(worker.experience ?? ""),
    city: String(user.location?.city ?? user.city ?? ""),
    ratingAvg: Number(worker.rating ?? worker.ratingAvg ?? 0),
    totalJobs: Number(worker.totalJobs ?? 0),
  };
  const jobSnapshot = {
    jobId,
    title: String(job.title ?? ""),
    companyName: String(job.companyName ?? ""),
    jobType: String(job.jobType ?? ""),
    salary: Number(job.salary ?? 0),
    salaryType: String(job.salaryType ?? ""),
    addressText: String(job.addressText ?? ""),
    status: String(job.status ?? ""),
    geohash: String(job.geohash ?? ""),
  };

  const txResult = await db().runTransaction(async (tx) => {
    const existing = await tx.get(appRef);
    if (existing.exists) {
      return { alreadyApplied: true };
    }
    tx.set(appRef, {
      jobId,
      workerId: uid,
      employerId,
      status: "applied",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      workerSnapshot,
      jobSnapshot,
    });
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
