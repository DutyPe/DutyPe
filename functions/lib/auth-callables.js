"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitApplication = exports.switchActiveRole = exports.addRole = exports.completeRegistration = void 0;
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
            const owner = (_a = phoneSnap.data()) === null || _a === void 0 ? void 0 : _a.uid;
            if (owner && owner !== uid) {
                throw new functions.https.HttpsError("already-exists", "Phone number is already linked to another account");
            }
        }
        const now = admin.firestore.Timestamp.now();
        const existing = userSnap.data() || {};
        const existingRoles = Array.isArray(existing.roles)
            ? existing.roles.map((r) => String(r).toUpperCase()).filter(Boolean)
            : [];
        const mergedRoles = Array.from(new Set([...existingRoles, role]));
        const alreadyExisted = userSnap.exists && existingRoles.length > 0;
        const userData = {
            userId: uid,
            phone: phoneE164,
            fullName: ((_b = existing.fullName) === null || _b === void 0 ? void 0 : _b.trim()) || fullName,
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
        if (!phoneSnap.exists) {
            tx.set(phoneIndexRef, {
                uid,
                createdAt: now,
            });
        }
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
function alreadyExistedToEvent(alreadyExisted, _role) {
    return alreadyExisted ? "role_added" : "signup";
}
exports.addRole = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    const uid = context.auth.uid;
    const newRole = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.newRole, "newRole", VALID_ROLES);
    const idem = await (0, idempotency_1.withIdempotency)(uid, "addRole", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    const userRef = db().collection("users").doc(uid);
    const result = await db().runTransaction(async (tx) => {
        const snap = await tx.get(userRef);
        if (!snap.exists) {
            throw new functions.https.HttpsError("failed-precondition", "User profile not found");
        }
        const existing = snap.data() || {};
        const existingRoles = Array.isArray(existing.roles)
            ? existing.roles.map((r) => String(r).toUpperCase()).filter(Boolean)
            : [];
        const merged = Array.from(new Set([...existingRoles, newRole]));
        tx.update(userRef, {
            roles: merged,
            activeRole: newRole,
            lastActiveAt: admin.firestore.FieldValue.serverTimestamp(),
            lastRoleSwitchAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return { merged, alreadyHad: existingRoles.includes(newRole) };
    });
    await logUserEvent(uid, "role_added", newRole, {
        alreadyHad: result.alreadyHad,
        rolesCount: result.merged.length,
    });
    const out = { success: true, roles: result.merged, activeRole: newRole };
    await idem.record(out);
    return out;
});
exports.switchActiveRole = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    const uid = context.auth.uid;
    const newRole = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.newRole, "newRole", VALID_ROLES);
    const idem = await (0, idempotency_1.withIdempotency)(uid, "switchActiveRole", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    const userRef = db().collection("users").doc(uid);
    const profileCol = newRole === "WORKER" ? "worker_profiles" : "employer_profiles";
    const profileRef = db().collection(profileCol).doc(uid);
    const result = await db().runTransaction(async (tx) => {
        const [userSnap, profileSnap] = await Promise.all([tx.get(userRef), tx.get(profileRef)]);
        if (!userSnap.exists) {
            throw new functions.https.HttpsError("failed-precondition", "User profile not found");
        }
        const existing = userSnap.data() || {};
        const existingRoles = Array.isArray(existing.roles)
            ? existing.roles.map((r) => String(r).toUpperCase()).filter(Boolean)
            : [];
        if (!existingRoles.includes(newRole)) {
            throw new functions.https.HttpsError("failed-precondition", `Role ${newRole} not added to this account; call addRole first`);
        }
        const previousRole = String(existing.activeRole || "").toUpperCase();
        const profileExists = profileSnap.exists;
        tx.update(userRef, {
            activeRole: newRole,
            lastActiveAt: admin.firestore.FieldValue.serverTimestamp(),
            lastRoleSwitchAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        return { previousRole, profileExists };
    });
    await logUserEvent(uid, "role_switched", newRole, {
        fromRole: result.previousRole,
        toRole: newRole,
        profileExists: result.profileExists,
    });
    const out = { success: true, activeRole: newRole, profileExists: result.profileExists };
    await idem.record(out);
    return out;
});
const MIN_WORKER_PROFILE_SCORE = 80;
const MAX_APPLICATIONS_PER_HOUR = 10;
exports.submitApplication = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k, _l, _m, _o, _p, _q, _r, _s, _t, _u;
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
    const user = userSnap.data() || {};
    const roles = Array.isArray(user.roles)
        ? user.roles.map((r) => String(r).toUpperCase())
        : [];
    if (!roles.includes("WORKER")) {
        throw new functions.https.HttpsError("failed-precondition", "WORKER role required");
    }
    if (!workerProfileSnap.exists) {
        throw new functions.https.HttpsError("failed-precondition", "Worker profile not set up");
    }
    const workerScore = Number((_b = (_a = (workerProfileSnap.data() || {}).profileScore) !== null && _a !== void 0 ? _a : user.workerProfileScore) !== null && _b !== void 0 ? _b : 0);
    if (!Number.isFinite(workerScore) || workerScore < MIN_WORKER_PROFILE_SCORE) {
        throw new functions.https.HttpsError("failed-precondition", `Profile must be at least ${MIN_WORKER_PROFILE_SCORE}% complete to apply`);
    }
    if (!jobSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Job not found");
    }
    const job = jobSnap.data() || {};
    const jobStatus = String(job.status || "").toLowerCase();
    if (jobStatus !== "active") {
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
    const worker = workerProfileSnap.data() || {};
    const jobTypes = Array.isArray(worker.jobTypes) ? worker.jobTypes : [];
    const workerSnapshot = {
        workerId: uid,
        fullName: String((_c = user.fullName) !== null && _c !== void 0 ? _c : ""),
        profileImageUrl: String((_d = user.profileImageUrl) !== null && _d !== void 0 ? _d : ""),
        primarySkill: jobTypes.length > 0 ? String(jobTypes[0]) : "",
        experience: String((_e = worker.experience) !== null && _e !== void 0 ? _e : ""),
        city: String((_h = (_g = (_f = user.location) === null || _f === void 0 ? void 0 : _f.city) !== null && _g !== void 0 ? _g : user.city) !== null && _h !== void 0 ? _h : ""),
        ratingAvg: Number((_k = (_j = worker.rating) !== null && _j !== void 0 ? _j : worker.ratingAvg) !== null && _k !== void 0 ? _k : 0),
        totalJobs: Number((_l = worker.totalJobs) !== null && _l !== void 0 ? _l : 0),
    };
    const jobSnapshot = {
        jobId,
        title: String((_m = job.title) !== null && _m !== void 0 ? _m : ""),
        companyName: String((_o = job.companyName) !== null && _o !== void 0 ? _o : ""),
        jobType: String((_p = job.jobType) !== null && _p !== void 0 ? _p : ""),
        salary: Number((_q = job.salary) !== null && _q !== void 0 ? _q : 0),
        salaryType: String((_r = job.salaryType) !== null && _r !== void 0 ? _r : ""),
        addressText: String((_s = job.addressText) !== null && _s !== void 0 ? _s : ""),
        status: String((_t = job.status) !== null && _t !== void 0 ? _t : ""),
        geohash: String((_u = job.geohash) !== null && _u !== void 0 ? _u : ""),
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