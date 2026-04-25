"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitApplication = exports.getWorkerProfileForEmployer = exports.lookupPhoneRole = exports.switchActiveRole = exports.addRole = exports.completeRegistration = void 0;
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
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const secure_callable_1 = require("./secure-callable");
const idempotency_1 = require("./idempotency");
const validation_1 = require("./validation");
const db = () => admin.firestore();
const VALID_ROLES = ["WORKER", "EMPLOYER"];
// ────────────────────────────────────────────────────────────────────────
// Helpers
// ────────────────────────────────────────────────────────────────────────
function normalizePhoneE164(raw) {
    if (!raw)
        return null;
    const trimmed = String(raw).trim();
    if (!/^\+[1-9][0-9]{6,14}$/.test(trimmed))
        return null;
    return trimmed;
}
function normalizeReferralCode(raw) {
    return String(raw !== null && raw !== void 0 ? raw : "")
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]/g, "");
}
async function logUserEvent(uid, type, role, payload = {}) {
    try {
        await db().collection("user_events").add({
            uid,
            type,
            role,
            payload,
            at: admin.firestore.FieldValue.serverTimestamp(),
        });
    }
    catch (e) {
        functions.logger.warn(`user_events write failed: ${type}`, e);
    }
}
async function fireAndForgetReferral(uid, role, fullName, referralCode) {
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
    }
    catch (e) {
        functions.logger.warn("queue pending_referral_applications failed", e);
    }
}
function alreadyExistedToEvent(alreadyExisted, _role) {
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
exports.completeRegistration = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a;
    const uid = context.auth.uid;
    const phoneE164 = normalizePhoneE164((_a = context.auth.token) === null || _a === void 0 ? void 0 : _a.phone_number);
    const fullName = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.fullName, "fullName", {
        required: true,
        minLength: 1,
        maxLength: 80,
    });
    const role = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.role, "role", VALID_ROLES);
    const referralCode = (data === null || data === void 0 ? void 0 : data.referralCode) ? normalizeReferralCode(data.referralCode) : "";
    if (!phoneE164) {
        throw new functions.https.HttpsError("failed-precondition", "Phone number on auth token is missing or not in E.164 format");
    }
    const idem = await (0, idempotency_1.withIdempotency)(uid, "completeRegistration", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    const userRef = db().collection("users").doc(uid);
    const phoneIndexRef = db().collection("phone_index").doc(phoneE164);
    const result = await db().runTransaction(async (tx) => {
        var _a, _b;
        const [userSnap, phoneSnap] = await Promise.all([tx.get(userRef), tx.get(phoneIndexRef)]);
        // Phone uniqueness: if phone_index already maps to another uid, refuse.
        if (phoneSnap.exists) {
            const phoneData = phoneSnap.data() || {};
            const owner = phoneData.uid;
            if (owner && owner !== uid) {
                throw new functions.https.HttpsError("already-exists", "Phone number is already linked to another account");
            }
            // Single-role-per-phone (#9 / #20): block role change on same phone.
            const existingRole = String(phoneData.role || "").toUpperCase();
            if (existingRole && existingRole !== role) {
                throw new functions.https.HttpsError("failed-precondition", `phone-already-registered-as:${existingRole}`);
            }
        }
        const now = admin.firestore.Timestamp.now();
        const existing = (userSnap.data() || {});
        const existingRoles = Array.isArray(existing.roles)
            ? existing.roles.map((r) => String(r).toUpperCase()).filter(Boolean)
            : [];
        // Single-role enforcement (#20). If the user already has a role and it
        // differs from the request, refuse — even if the phone_index doc was
        // somehow missing (defence in depth).
        if (existingRoles.length > 0 && !existingRoles.includes(role)) {
            throw new functions.https.HttpsError("failed-precondition", `phone-already-registered-as:${existingRoles[0]}`);
        }
        const mergedRoles = Array.from(new Set([...existingRoles, role]));
        const alreadyExisted = userSnap.exists && existingRoles.length > 0;
        const userData = {
            userId: uid,
            phone: phoneE164,
            fullName: ((_a = existing.fullName) === null || _a === void 0 ? void 0 : _a.trim()) || fullName,
            roles: mergedRoles,
            activeRole: role,
            createdAt: existing.createdAt || now,
            lastActiveAt: now,
        };
        if (existing.referralCode)
            userData.referralCode = existing.referralCode;
        if (referralCode && !existing.referredByCode) {
            userData.referredByCode = referralCode;
        }
        else if (existing.referredByCode) {
            userData.referredByCode = existing.referredByCode;
        }
        if (existing.referredByUserId)
            userData.referredByUserId = existing.referredByUserId;
        if (existing.profileImageUrl)
            userData.profileImageUrl = existing.profileImageUrl;
        if (existing.fcmToken)
            userData.fcmToken = existing.fcmToken;
        tx.set(userRef, userData, { merge: true });
        // Extended phone_index payload (#20). Stored with `merge: true` so
        // existing legacy docs get backfilled with role/name/phone on the next
        // registration touch.
        tx.set(phoneIndexRef, {
            phone: phoneE164,
            uid,
            role,
            name: userData.fullName,
            createdAt: phoneSnap.exists ? ((_b = phoneSnap.data()) === null || _b === void 0 ? void 0 : _b.createdAt) || now : now,
            updatedAt: now,
        }, { merge: true });
        return { alreadyExisted, mergedRoles };
    });
    await logUserEvent(uid, alreadyExistedToEvent(result.alreadyExisted, role), role, {
        role,
        rolesCount: result.mergedRoles.length,
        hasReferral: !!referralCode,
    });
    if (referralCode) {
        await fireAndForgetReferral(uid, role, fullName, referralCode);
    }
    const out = {
        success: true,
        userId: uid,
        activeRole: role,
        alreadyExisted: result.alreadyExisted,
    };
    await idem.record(out);
    return out;
});
// ────────────────────────────────────────────────────────────────────────
// addRole — DISABLED (#20: dual-role not supported)
// ────────────────────────────────────────────────────────────────────────
// Kept exported for binary compatibility with deployed clients; always
// rejects so existing apps surface a clear error instead of silently
// granting a second role.
exports.addRole = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    const uid = context.auth.uid;
    const newRole = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.newRole, "newRole", VALID_ROLES);
    // Look up existing role for the toast message.
    const userSnap = await db().collection("users").doc(uid).get();
    const existing = (userSnap.data() || {});
    const existingRoles = Array.isArray(existing.roles)
        ? existing.roles.map((r) => String(r).toUpperCase()).filter(Boolean)
        : [];
    if (existingRoles.includes(newRole)) {
        return { success: true, roles: existingRoles, activeRole: newRole, noop: true };
    }
    const existingRole = existingRoles[0] || "";
    throw new functions.https.HttpsError("failed-precondition", existingRole
        ? `phone-already-registered-as:${existingRole}`
        : "dual-role-not-supported");
});
// ────────────────────────────────────────────────────────────────────────
// switchActiveRole — DISABLED (#20: dual-role not supported)
// ────────────────────────────────────────────────────────────────────────
// If the requested role matches the user's existing role, this is a no-op
// (idempotent for clients calling on every cold start). Anything else is
// rejected.
exports.switchActiveRole = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    const uid = context.auth.uid;
    const newRole = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.newRole, "newRole", VALID_ROLES);
    const userSnap = await db().collection("users").doc(uid).get();
    if (!userSnap.exists) {
        throw new functions.https.HttpsError("failed-precondition", "User profile not found");
    }
    const existing = (userSnap.data() || {});
    const activeRole = String(existing.activeRole || "").toUpperCase();
    if (activeRole === newRole) {
        return { success: true, activeRole: newRole, profileExists: true, noop: true };
    }
    const existingRole = activeRole || "";
    throw new functions.https.HttpsError("failed-precondition", existingRole
        ? `phone-already-registered-as:${existingRole}`
        : "dual-role-not-supported");
});
// ────────────────────────────────────────────────────────────────────────
// lookupPhoneRole — single-doc, role-aware phone lookup (#11 / #20)
// ────────────────────────────────────────────────────────────────────────
// Public (App-Check enforced, auth NOT required) so the LOGIN screen can
// pre-check before triggering OTP. Returns only the existing role/name —
// never the uid — to keep the surface privacy-safe.
exports.lookupPhoneRole = (0, secure_callable_1.onCallSecured)({ requireAuth: false }, async (data, _context) => {
    const phoneE164 = normalizePhoneE164(data === null || data === void 0 ? void 0 : data.phone);
    if (!phoneE164) {
        throw new functions.https.HttpsError("invalid-argument", "phone must be in E.164 format (e.g. +919876543210)");
    }
    const requestedRoleRaw = String((data === null || data === void 0 ? void 0 : data.requestedRole) || "").toUpperCase();
    const requestedRole = requestedRoleRaw === "WORKER" || requestedRoleRaw === "EMPLOYER"
        ? requestedRoleRaw
        : "";
    const snap = await db().collection("phone_index").doc(phoneE164).get();
    let d = snap.data() || {};
    if (!snap.exists) {
        const digits = phoneE164.replace(/\D/g, "");
        const last10 = digits.length >= 10 ? digits.slice(-10) : "";
        const variants = Array.from(new Set([
            phoneE164,
            digits,
            last10 ? `+91${last10}` : "",
            last10 ? `91${last10}` : "",
            last10,
        ].filter(Boolean)));
        let userSnap = await db()
            .collection("users")
            .where("phone", "in", variants.slice(0, 10))
            .limit(1)
            .get();
        if (userSnap.empty) {
            userSnap = await db()
                .collection("users")
                .where("phoneNumber", "in", variants.slice(0, 10))
                .limit(1)
                .get();
        }
        if (userSnap.empty) {
            return { exists: false, roleConflict: false };
        }
        d = userSnap.docs[0].data() || {};
    }
    const existingRole = String(d.role || "").toUpperCase();
    const name = String(d.name || "");
    const roleConflict = !!requestedRole && !!existingRole && requestedRole !== existingRole;
    return {
        exists: true,
        existingRole: existingRole || null,
        name: name || null,
        roleConflict,
    };
});
// ────────────────────────────────────────────────────────────────────────
// getWorkerProfileForEmployer (#19)
// ────────────────────────────────────────────────────────────────────────
// Firestore rules block direct employer reads of `worker_profiles` because
// rules cannot iterate `applications` to verify the relationship. This
// callable bridges that gap: returns the merged user + worker_profile data
// only if the caller has at least one application from this worker
// (optionally scoped to a specific jobId).
exports.getWorkerProfileForEmployer = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a;
    const uid = context.auth.uid;
    const workerId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.workerId, "workerId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const jobId = (data === null || data === void 0 ? void 0 : data.jobId)
        ? (0, validation_1.validateString)(data.jobId, "jobId", { minLength: 4, maxLength: 128 })
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
        if (!direct.exists || ((_a = direct.data()) === null || _a === void 0 ? void 0 : _a.employerId) !== uid) {
            throw new functions.https.HttpsError("permission-denied", "Caller is not the employer of this application");
        }
    }
    else {
        const appSnap = await appQuery.get();
        if (appSnap.empty) {
            throw new functions.https.HttpsError("permission-denied", "Caller does not employ this worker");
        }
    }
    const [userSnap, workerSnap] = await Promise.all([
        db().collection("users").doc(workerId).get(),
        db().collection("worker_profiles").doc(workerId).get(),
    ]);
    if (!userSnap.exists && !workerSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Worker profile not found");
    }
    const user = (userSnap.data() || {});
    const worker = (workerSnap.data() || {});
    // Strip sensitive fields before returning.
    const safeUser = {};
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
        if (user[k] !== undefined)
            safeUser[k] = user[k];
    }
    const safeWorker = {};
    for (const k of [
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
        if (worker[k] !== undefined)
            safeWorker[k] = worker[k];
    }
    // Merge with worker_profiles taking precedence on overlapping keys.
    const merged = Object.assign(Object.assign(Object.assign({}, safeUser), safeWorker), { workerId });
    return { success: true, profile: merged };
});
// ────────────────────────────────────────────────────────────────────────
// submitApplication
// ────────────────────────────────────────────────────────────────────────
const MIN_WORKER_PROFILE_SCORE = 80;
const MAX_APPLICATIONS_PER_HOUR = 10;
exports.submitApplication = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r, _s, _t, _u, _v;
    const uid = context.auth.uid;
    const jobId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.jobId, "jobId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const idem = await (0, idempotency_1.withIdempotency)(uid, "submitApplication", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    // Rate limit: count applications by this worker in the last hour.
    const oneHourAgo = admin.firestore.Timestamp.fromMillis(Date.now() - 60 * 60 * 1000);
    const recentSnap = await db()
        .collection("applications")
        .where("workerId", "==", uid)
        .where("createdAt", ">", oneHourAgo)
        .limit(MAX_APPLICATIONS_PER_HOUR + 1)
        .get();
    if (recentSnap.size >= MAX_APPLICATIONS_PER_HOUR) {
        throw new functions.https.HttpsError("resource-exhausted", `Application limit reached (${MAX_APPLICATIONS_PER_HOUR}/hour)`);
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
    const user = (userSnap.data() || {});
    const roles = Array.isArray(user.roles)
        ? user.roles.map((r) => String(r).toUpperCase())
        : [];
    if (!roles.includes("WORKER")) {
        throw new functions.https.HttpsError("failed-precondition", "WORKER role required");
    }
    if (!workerProfileSnap.exists) {
        throw new functions.https.HttpsError("failed-precondition", "Worker profile not set up");
    }
    const workerScoreRaw = (_c = (_b = (_a = workerProfileSnap.data()) === null || _a === void 0 ? void 0 : _a.profileScore) !== null && _b !== void 0 ? _b : user.workerProfileScore) !== null && _c !== void 0 ? _c : 0;
    const workerScore = Number(workerScoreRaw);
    if (!Number.isFinite(workerScore) || workerScore < MIN_WORKER_PROFILE_SCORE) {
        throw new functions.https.HttpsError("failed-precondition", `Profile must be at least ${MIN_WORKER_PROFILE_SCORE}% complete to apply`);
    }
    if (!jobSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Job not found");
    }
    const job = (jobSnap.data() || {});
    const jobStatus = String(job.status || "").toLowerCase();
    if (jobStatus !== "active" && jobStatus !== "open") {
        throw new functions.https.HttpsError("failed-precondition", `Job is not accepting applications (status=${jobStatus || "unknown"})`);
    }
    const employerId = String(job.employerId || "");
    if (!employerId) {
        throw new functions.https.HttpsError("failed-precondition", "Job is missing employerId");
    }
    if (employerId === uid) {
        throw new functions.https.HttpsError("failed-precondition", "You cannot apply to your own job");
    }
    const docId = `${jobId}_${uid}`;
    const appRef = db().collection("applications").doc(docId);
    // Build snapshots at create time so list screens render with zero fan-out.
    const worker = (workerProfileSnap.data() || {});
    const jobTypes = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
    const workerSnapshot = {
        workerId: uid,
        fullName: String((_d = user.fullName) !== null && _d !== void 0 ? _d : ""),
        profileImageUrl: String((_e = user.profileImageUrl) !== null && _e !== void 0 ? _e : ""),
        primarySkill: jobTypes.length > 0 ? String(jobTypes[0]) : "",
        experience: String((_f = worker.experience) !== null && _f !== void 0 ? _f : ""),
        city: String((_j = (_h = (_g = user.location) === null || _g === void 0 ? void 0 : _g.city) !== null && _h !== void 0 ? _h : user.city) !== null && _j !== void 0 ? _j : ""),
        ratingAvg: Number((_l = (_k = worker.rating) !== null && _k !== void 0 ? _k : worker.ratingAvg) !== null && _l !== void 0 ? _l : 0),
        totalJobs: Number((_m = worker.totalJobs) !== null && _m !== void 0 ? _m : 0),
    };
    const jobSnapshot = {
        jobId,
        title: String((_o = job.title) !== null && _o !== void 0 ? _o : ""),
        companyName: String((_p = job.companyName) !== null && _p !== void 0 ? _p : ""),
        jobType: String((_q = job.jobType) !== null && _q !== void 0 ? _q : ""),
        salary: String((_r = job.salary) !== null && _r !== void 0 ? _r : ""),
        salaryType: String((_s = job.salaryType) !== null && _s !== void 0 ? _s : ""),
        addressText: String((_t = job.addressText) !== null && _t !== void 0 ? _t : ""),
        status: String((_u = job.status) !== null && _u !== void 0 ? _u : ""),
        geohash: String((_v = job.geohash) !== null && _v !== void 0 ? _v : ""),
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
    await idem.record(out);
    return out;
});
//# sourceMappingURL=auth-callables.js.map