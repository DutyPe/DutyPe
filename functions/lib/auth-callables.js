"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.submitApplication = exports.respondToWorkerJobRequest = exports.requestWorkerForJob = exports.matchWorkersForJob = exports.getWorkerProfileForEmployer = exports.lookupPhoneRole = exports.completeRegistration = void 0;
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
function normalizeMatchToken(value) {
    return String(value !== null && value !== void 0 ? value : "")
        .trim()
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, " ")
        .trim();
}
function stringList(value) {
    if (Array.isArray(value)) {
        return value
            .map((item) => normalizeMatchToken(item))
            .filter((item) => item.length > 0);
    }
    if (typeof value === "string") {
        return value
            .split(",")
            .map((item) => normalizeMatchToken(item))
            .filter((item) => item.length > 0);
    }
    return [];
}
function readLocation(value) {
    var _a, _b;
    if (!value || typeof value !== "object")
        return null;
    const raw = value;
    const lat = Number((_a = raw.lat) !== null && _a !== void 0 ? _a : raw.latitude);
    const lng = Number((_b = raw.lng) !== null && _b !== void 0 ? _b : raw.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng))
        return null;
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180)
        return null;
    return { lat, lng };
}
function readRecordLocation(value) {
    return readLocation(value.location) || readLocation(value);
}
function distanceKm(from, to) {
    if (!from || !to)
        return null;
    const radiusKm = 6371;
    const dLat = ((to.lat - from.lat) * Math.PI) / 180;
    const dLng = ((to.lng - from.lng) * Math.PI) / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos((from.lat * Math.PI) / 180) *
            Math.cos((to.lat * Math.PI) / 180) *
            Math.sin(dLng / 2) *
            Math.sin(dLng / 2);
    return radiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
const CATEGORY_KEYWORDS = [
    { name: "DELIVERY", display: "Delivery", words: ["delivery", "courier", "rider", "parcel"] },
    { name: "DRIVER", display: "Driver", words: ["driver", "cab", "taxi", "truck"] },
    { name: "COOK", display: "Cook", words: ["cook", "chef", "kitchen", "catering"] },
    { name: "MAID", display: "Housekeeping", words: ["maid", "housekeep", "cleaner", "domestic", "house help"] },
    { name: "SECURITY", display: "Security", words: ["security", "guard", "watchman"] },
    { name: "HELPER", display: "Helper", words: ["helper", "assistant", "labour", "labor", "loader", "loading", "unloading"] },
    { name: "ELECTRICIAN", display: "Electrician", words: ["electric", "wiring"] },
    { name: "PLUMBER", display: "Plumber", words: ["plumb", "pipe"] },
    { name: "WAITER", display: "Restaurant Staff", words: ["waiter", "server", "restaurant", "hotel", "steward"] },
    { name: "SALES", display: "Sales", words: ["sales", "retail", "store"] },
    { name: "TELECALLER", display: "Telecaller", words: ["telecaller", "calling", "call center", "bpo"] },
    { name: "OFFICE_STAFF", display: "Office Staff", words: ["office", "admin", "clerk", "peon"] },
    { name: "PACKER", display: "Packer", words: ["packer", "packing", "warehouse"] },
    { name: "MECHANIC", display: "Mechanic", words: ["mechanic", "garage", "technician"] },
];
function inferCategory(job, details) {
    const explicit = normalizeMatchToken(job.jobType || details.jobType).replace(/ /g, "_").toUpperCase();
    const explicitMatch = CATEGORY_KEYWORDS.find((category) => category.name === explicit);
    if (explicitMatch)
        return { name: explicitMatch.name, display: explicitMatch.display };
    const text = normalizeMatchToken(`${job.title || ""} ${details.description || ""}`);
    const inferred = CATEGORY_KEYWORDS.find((category) => category.words.some((word) => text.includes(word)));
    return inferred ? { name: inferred.name, display: inferred.display } : { name: "OTHER", display: "Other" };
}
function scoreWorkerForJob(workerId, worker, job, details) {
    var _a, _b, _c, _d;
    const category = inferCategory(job, details);
    const workerSkills = stringList(worker.skills || worker.jobTypes);
    const jobText = normalizeMatchToken(`${job.title || ""} ${details.description || ""}`);
    const skillText = workerSkills.join(" ");
    const reasons = [];
    let score = 0;
    const categoryRule = CATEGORY_KEYWORDS.find((item) => item.name === category.name);
    const categoryWords = (categoryRule === null || categoryRule === void 0 ? void 0 : categoryRule.words) || [category.display.toLowerCase()];
    const categoryMatch = categoryWords.some((word) => skillText.includes(word)) ||
        workerSkills.some((skill) => jobText.includes(skill) && skill.length >= 3);
    if (categoryMatch) {
        score += 45;
        reasons.push(`${category.display} skill match`);
    }
    const jobLocation = readRecordLocation(job);
    const workerLocation = readRecordLocation(worker);
    const dist = distanceKm(jobLocation, workerLocation);
    if (dist != null) {
        if (dist <= 3) {
            score += 30;
            reasons.push("Within 3 km");
        }
        else if (dist <= 5) {
            score += 24;
            reasons.push("Within 5 km");
        }
        else if (dist <= 10) {
            score += 16;
            reasons.push("Within 10 km");
        }
        else if (dist <= 20) {
            score += 8;
        }
    }
    const experience = normalizeMatchToken(worker.experience);
    if (experience && !experience.includes("no experience")) {
        score += 8;
        reasons.push("Has experience");
    }
    const rating = Number((_b = (_a = worker.ratingAvg) !== null && _a !== void 0 ? _a : worker.rating) !== null && _b !== void 0 ? _b : 0);
    if (Number.isFinite(rating) && rating >= 4) {
        score += 7;
        reasons.push("Strong rating");
    }
    const completedJobs = Number((_d = (_c = worker.completedJobs) !== null && _c !== void 0 ? _c : worker.totalJobs) !== null && _d !== void 0 ? _d : 0);
    if (Number.isFinite(completedJobs) && completedJobs > 0) {
        score += Math.min(8, completedJobs * 2);
        reasons.push("Completed DutyPe work");
    }
    if (worker.isAvailable !== false) {
        score += 5;
    }
    if (workerId === String(job.employerId || details.employerId || "")) {
        score = 0;
    }
    return { score: Math.min(100, Math.round(score)), reasons: reasons.slice(0, 4), distance: dist };
}
async function loadOwnedJob(uid, jobId) {
    const [jobSnap, detailSnap] = await Promise.all([
        db().collection("jobmetadata").doc(jobId).get(),
        db().collection("job_details").doc(jobId).get(),
    ]);
    if (!jobSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Job not found");
    }
    const job = (jobSnap.data() || {});
    const details = (detailSnap.data() || {});
    const employerId = String(job.employerId || details.employerId || "");
    if (employerId !== uid) {
        throw new functions.https.HttpsError("permission-denied", "Caller does not own this job");
    }
    return { jobId, job, details };
}
async function writeNotification(recipientId, title, message, type, targetRole, data) {
    const ref = db().collection("notifications").doc();
    await ref.set({
        recipientId,
        title,
        message,
        type,
        targetRole,
        data: Object.assign(Object.assign({}, data), { notificationId: ref.id }),
        isRead: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
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
//   • If phoneRoles/{phoneE164} already exists for a DIFFERENT uid → block.
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
    const phoneRoleRef = db().collection("phoneRoles").doc(phoneE164);
    const profileRef = db()
        .collection(role === "WORKER" ? "worker_profiles" : "employer_profiles")
        .doc(uid);
    const result = await db().runTransaction(async (tx) => {
        var _a, _b;
        const [phoneSnap, profileSnap] = await Promise.all([tx.get(phoneRoleRef), tx.get(profileRef)]);
        // Phone uniqueness: if phoneRoles already maps to another uid, refuse.
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
        const existing = (profileSnap.data() || {});
        const existingRole = String(existing.role || "").toUpperCase();
        // Single-role enforcement (#20). If the user already has a role and it
        // differs from the request, refuse — even if the phoneRoles doc was
        // somehow missing (defence in depth).
        if (existingRole && existingRole !== role) {
            throw new functions.https.HttpsError("failed-precondition", `phone-already-registered-as:${existingRole}`);
        }
        const alreadyExisted = phoneSnap.exists || profileSnap.exists;
        const profileData = {
            userId: uid,
            phone: phoneE164,
            fullName: ((_a = existing.fullName) === null || _a === void 0 ? void 0 : _a.trim()) || fullName,
            role,
            createdAt: existing.createdAt || now,
            updatedAt: now,
        };
        if (role === "EMPLOYER")
            profileData.companyName = existing.companyName || fullName;
        if (existing.referralCode)
            profileData.referralCode = existing.referralCode;
        if (referralCode && !existing.referredByCode) {
            profileData.referredByCode = referralCode;
        }
        else if (existing.referredByCode) {
            profileData.referredByCode = existing.referredByCode;
        }
        if (existing.referredByUserId)
            profileData.referredByUserId = existing.referredByUserId;
        if (existing.profileImageUrl)
            profileData.profileImageUrl = existing.profileImageUrl;
        tx.set(profileRef, profileData, { merge: true });
        tx.set(phoneRoleRef, {
            phoneNumber: phoneE164,
            uid,
            role,
            name: profileData.fullName,
            createdAt: phoneSnap.exists ? ((_b = phoneSnap.data()) === null || _b === void 0 ? void 0 : _b.createdAt) || now : now,
            updatedAt: now,
        });
        return { alreadyExisted };
    });
    await logUserEvent(uid, alreadyExistedToEvent(result.alreadyExisted, role), role, {
        role,
        rolesCount: 1,
        hasReferral: !!referralCode,
    });
    if (referralCode) {
        await fireAndForgetReferral(uid, role, fullName, referralCode);
    }
    const out = {
        success: true,
        userId: uid,
        role,
        alreadyExisted: result.alreadyExisted,
    };
    await idem.record(out);
    return out;
});
// ────────────────────────────────────────────────────────────────────────
// lookupPhoneRole — single-doc, role-aware phone lookup (#11 / #20)
// ────────────────────────────────────────────────────────────────────────
// Public (auth/App Check NOT required) so the LOGIN screen can
// pre-check before triggering OTP. Returns only the existing role/name —
// never the uid — to keep the surface privacy-safe.
exports.lookupPhoneRole = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false }, async (data, _context) => {
    const phoneE164 = normalizePhoneE164(data === null || data === void 0 ? void 0 : data.phone);
    if (!phoneE164) {
        throw new functions.https.HttpsError("invalid-argument", "phone must be in E.164 format (e.g. +919876543210)");
    }
    const requestedRoleRaw = String((data === null || data === void 0 ? void 0 : data.requestedRole) || "").toUpperCase();
    const requestedRole = requestedRoleRaw === "WORKER" || requestedRoleRaw === "EMPLOYER"
        ? requestedRoleRaw
        : "";
    const snap = await db().collection("phoneRoles").doc(phoneE164).get();
    if (!snap.exists) {
        return { exists: false, roleConflict: false };
    }
    const d = snap.data() || {};
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
// only if the caller has at least one application or instant response from
// this worker (optionally scoped to a specific jobId/requestId).
exports.getWorkerProfileForEmployer = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (data, context) => {
    const uid = context.auth.uid;
    const workerId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.workerId, "workerId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const jobId = (data === null || data === void 0 ? void 0 : data.jobId)
        ? (0, validation_1.validateString)(data.jobId, "jobId", { minLength: 4, maxLength: 128 })
        : "";
    // Authorise: caller must employ this worker via at least one application
    // or have an instant-help response from this worker.
    let appQuery = db()
        .collection("applications")
        .where("employerId", "==", uid)
        .where("workerId", "==", workerId)
        .limit(1);
    if (jobId) {
        // Tightest scope: docId is `${jobId}_${workerId}`.
        const docId = `${jobId}_${workerId}`;
        const [direct, instantDirect] = await Promise.all([
            db().collection("applications").doc(docId).get(),
            db().collection("instant_responses").doc(docId).get(),
        ]);
        const directData = (direct.data() || {});
        const instantDirectData = (instantDirect.data() || {});
        const hasApplicationAccess = direct.exists && directData.employerId === uid && directData.workerId === workerId;
        const hasInstantAccess = instantDirect.exists && instantDirectData.employerId === uid && instantDirectData.workerId === workerId;
        if (!hasApplicationAccess && !hasInstantAccess) {
            throw new functions.https.HttpsError("permission-denied", "Caller is not the employer for this worker relationship");
        }
    }
    else {
        const [appSnap, instantSnap] = await Promise.all([
            appQuery.get(),
            db()
                .collection("instant_responses")
                .where("workerId", "==", workerId)
                .limit(50)
                .get(),
        ]);
        const hasInstantAccess = instantSnap.docs.some((doc) => {
            const response = (doc.data() || {});
            return response.employerId === uid && response.workerId === workerId;
        });
        if (appSnap.empty && !hasInstantAccess) {
            throw new functions.https.HttpsError("permission-denied", "Caller does not have a worker relationship");
        }
    }
    const workerSnap = await db().collection("worker_profiles").doc(workerId).get();
    if (!workerSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Worker profile not found");
    }
    const worker = (workerSnap.data() || {});
    const safeWorker = {};
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
        if (worker[k] !== undefined)
            safeWorker[k] = worker[k];
    }
    const merged = Object.assign(Object.assign({}, safeWorker), { workerId });
    return { success: true, profile: merged };
});
exports.matchWorkersForJob = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 60, enforceAppCheck: false }, async (data, context) => {
    const uid = context.auth.uid;
    const jobId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.jobId, "jobId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const { job, details } = await loadOwnedJob(uid, jobId);
    const existingRequestsSnap = await db()
        .collection("worker_job_requests")
        .where("jobId", "==", jobId)
        .where("employerId", "==", uid)
        .limit(200)
        .get();
    const requestByWorker = new Map();
    existingRequestsSnap.docs.forEach((doc) => {
        const request = doc.data();
        requestByWorker.set(String(request.workerId || ""), Object.assign(Object.assign({}, request), { requestId: doc.id }));
    });
    let workersSnap;
    try {
        workersSnap = await db()
            .collection("worker_profiles")
            .where("role", "==", "WORKER")
            .limit(500)
            .get();
        if (workersSnap.empty) {
            workersSnap = await db().collection("worker_profiles").limit(500).get();
        }
    }
    catch (e) {
        functions.logger.warn("matchWorkersForJob role query failed, falling back", e);
        workersSnap = await db().collection("worker_profiles").limit(500).get();
    }
    const rankedWorkers = workersSnap.docs
        .map((doc) => {
        var _a, _b, _c, _d;
        const worker = (doc.data() || {});
        const scoring = scoreWorkerForJob(doc.id, worker, job, details);
        const request = requestByWorker.get(doc.id);
        const requestStatus = String((request === null || request === void 0 ? void 0 : request.status) || "");
        const safePhone = requestStatus === "accepted" ? String(worker.phone || "") : "";
        return {
            workerId: doc.id,
            fullName: String(worker.fullName || worker.name || "Worker"),
            phone: safePhone,
            profileImageUrl: String(worker.profileImageUrl || ""),
            skills: stringList(worker.skills || worker.jobTypes).slice(0, 8),
            experience: String(worker.experience || ""),
            rating: Number((_b = (_a = worker.ratingAvg) !== null && _a !== void 0 ? _a : worker.rating) !== null && _b !== void 0 ? _b : 0) || 0,
            completedJobs: Number((_d = (_c = worker.completedJobs) !== null && _c !== void 0 ? _c : worker.totalJobs) !== null && _d !== void 0 ? _d : 0) || 0,
            isAvailable: worker.isAvailable !== false,
            distanceKm: scoring.distance == null ? null : Number(scoring.distance.toFixed(2)),
            matchScore: scoring.score,
            matchReasons: scoring.reasons,
            requestId: String((request === null || request === void 0 ? void 0 : request.requestId) || ""),
            requestStatus,
        };
    })
        .sort((a, b) => {
        var _a, _b;
        if (b.matchScore !== a.matchScore)
            return b.matchScore - a.matchScore;
        const aDistance = (_a = a.distanceKm) !== null && _a !== void 0 ? _a : Number.MAX_SAFE_INTEGER;
        const bDistance = (_b = b.distanceKm) !== null && _b !== void 0 ? _b : Number.MAX_SAFE_INTEGER;
        return aDistance - bDistance;
    });
    const strongMatches = rankedWorkers.filter((worker) => worker.matchScore >= 20 || worker.requestStatus);
    const selectedWorkerIds = new Set(strongMatches.map((worker) => worker.workerId));
    const fallbackProfiles = rankedWorkers
        .filter((worker) => !selectedWorkerIds.has(worker.workerId))
        .filter((worker) => worker.workerId !== uid)
        .filter((worker) => worker.fullName !== "Worker" || worker.skills.length > 0 || worker.profileImageUrl)
        .slice(0, Math.max(0, 20 - strongMatches.length))
        .map((worker) => (Object.assign(Object.assign({}, worker), { matchScore: Math.max(worker.matchScore, 5), matchReasons: worker.matchReasons.length > 0 ? worker.matchReasons : ["Worker profile available"] })));
    const matchedWorkers = [...strongMatches, ...fallbackProfiles].slice(0, 50);
    return { success: true, workers: matchedWorkers };
});
exports.requestWorkerForJob = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (data, context) => {
    const uid = context.auth.uid;
    const jobId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.jobId, "jobId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const workerId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.workerId, "workerId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const idem = await (0, idempotency_1.withIdempotency)(uid, "requestWorkerForJob", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    const { job, details } = await loadOwnedJob(uid, jobId);
    if (String(job.status || "").toLowerCase() !== "open") {
        throw new functions.https.HttpsError("failed-precondition", "Job is not open");
    }
    const [workerSnap, employerSnap] = await Promise.all([
        db().collection("worker_profiles").doc(workerId).get(),
        db().collection("employer_profiles").doc(uid).get(),
    ]);
    if (!workerSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Worker profile not found");
    }
    const worker = (workerSnap.data() || {});
    const employer = (employerSnap.data() || {});
    const scoring = scoreWorkerForJob(workerId, worker, job, details);
    if (scoring.score < 5) {
        throw new functions.https.HttpsError("failed-precondition", "Worker is not a strong fit for this job");
    }
    const requestId = `${jobId}_${workerId}`;
    const requestRef = db().collection("worker_job_requests").doc(requestId);
    const requestPayload = {
        requestId,
        jobId,
        workerId,
        employerId: uid,
        status: "pending",
        jobTitle: String(job.title || "Job request"),
        companyName: String(job.companyName || employer.companyName || employer.fullName || "DutyPe employer"),
        jobLocation: String(job.addressText || details.companyCity || ""),
        salary: String(job.salary || ""),
        salaryType: String(job.salaryType || ""),
        jobType: String(job.jobType || ""),
        employerName: String(employer.companyName || employer.fullName || job.companyName || "Employer"),
        employerPhone: String(details.contactNumber || employer.phone || ""),
        workerName: String(worker.fullName || worker.name || "Worker"),
        workerSkills: stringList(worker.skills || worker.jobTypes).slice(0, 8),
        matchScore: scoring.score,
        matchReasons: scoring.reasons,
        distanceKm: scoring.distance == null ? null : Number(scoring.distance.toFixed(2)),
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: admin.firestore.Timestamp.fromMillis(Date.now() + 7 * 24 * 60 * 60 * 1000),
    };
    const txResult = await db().runTransaction(async (tx) => {
        const existing = await tx.get(requestRef);
        if (existing.exists) {
            const existingData = existing.data() || {};
            return { requestId, status: String(existingData.status || "pending"), alreadyExists: true };
        }
        tx.set(requestRef, requestPayload);
        return { requestId, status: "pending", alreadyExists: false };
    });
    if (!txResult.alreadyExists) {
        await writeNotification(workerId, "Employer requested you", `${requestPayload.employerName} requested you for ${requestPayload.jobTitle}`, "EMPLOYER_MESSAGE", "WORKER", {
            requestId,
            jobId,
            employerId: uid,
            deepLink: `dutype://job/${jobId}`,
        });
        await logUserEvent(uid, "worker_requested_for_job", "EMPLOYER", { jobId, workerId, requestId });
    }
    const out = Object.assign({ success: true }, txResult);
    await idem.record(out);
    return out;
});
exports.respondToWorkerJobRequest = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false }, async (data, context) => {
    const uid = context.auth.uid;
    const requestId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.requestId, "requestId", {
        required: true,
        minLength: 8,
        maxLength: 256,
    });
    const action = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.action, "action", ["ACCEPT", "REJECT"]);
    const idem = await (0, idempotency_1.withIdempotency)(uid, "respondToWorkerJobRequest", data === null || data === void 0 ? void 0 : data.idempotencyKey);
    if (idem.hit)
        return idem.result;
    const requestRef = db().collection("worker_job_requests").doc(requestId);
    const requestSnap = await requestRef.get();
    if (!requestSnap.exists) {
        throw new functions.https.HttpsError("not-found", "Request not found");
    }
    const request = (requestSnap.data() || {});
    if (String(request.workerId || "") !== uid) {
        throw new functions.https.HttpsError("permission-denied", "Request belongs to another worker");
    }
    const workerSnap = await db().collection("worker_profiles").doc(uid).get();
    const worker = (workerSnap.data() || {});
    const jobId = String(request.jobId || "");
    const employerId = String(request.employerId || "");
    const now = admin.firestore.FieldValue.serverTimestamp();
    if (action === "REJECT") {
        await requestRef.update({ status: "rejected", updatedAt: now, respondedAt: now });
        const out = { success: true, status: "rejected", jobId };
        await idem.record(out);
        return out;
    }
    const jobRef = db().collection("jobmetadata").doc(jobId);
    const detailRef = db().collection("job_details").doc(jobId);
    const applicationRef = db().collection("applications").doc(`${jobId}_${uid}`);
    const txResult = await db().runTransaction(async (tx) => {
        const [freshRequestSnap, jobSnap] = await Promise.all([tx.get(requestRef), tx.get(jobRef)]);
        const freshRequest = (freshRequestSnap.data() || {});
        const job = (jobSnap.data() || {});
        const status = String(freshRequest.status || "");
        if (status === "accepted")
            return { accepted: true, alreadyAccepted: true };
        if (status !== "pending")
            return { accepted: false, status };
        if (!jobSnap.exists || String(job.status || "").toLowerCase() !== "open") {
            tx.update(requestRef, { status: "expired", updatedAt: now, respondedAt: now });
            return { accepted: false, status: "expired" };
        }
        tx.set(applicationRef, {
            jobId,
            workerId: uid,
            employerId,
            status: "hired",
            createdAt: now,
            workerName: String(worker.fullName || worker.name || request.workerName || "Worker"),
        }, { merge: true });
        tx.update(requestRef, {
            status: "accepted",
            updatedAt: now,
            respondedAt: now,
            workerPhone: String(worker.phone || ""),
        });
        tx.update(jobRef, { status: "expired" });
        tx.update(detailRef, { expiresAt: admin.firestore.Timestamp.fromMillis(Date.now()) });
        return { accepted: true, alreadyAccepted: false };
    });
    if (txResult.accepted) {
        const siblingRequests = await db()
            .collection("worker_job_requests")
            .where("jobId", "==", jobId)
            .limit(200)
            .get();
        const batch = db().batch();
        let expiredCount = 0;
        siblingRequests.docs.forEach((doc) => {
            if (doc.id !== requestId && String(doc.data().status || "") === "pending") {
                batch.update(doc.ref, { status: "expired", updatedAt: admin.firestore.FieldValue.serverTimestamp() });
                expiredCount += 1;
            }
        });
        if (expiredCount > 0) {
            await batch.commit();
        }
        await writeNotification(employerId, "Worker accepted your request", `${String(worker.fullName || request.workerName || "Worker")} accepted ${String(request.jobTitle || "your job")}`, "WORKER_HIRED", "EMPLOYER", {
            requestId,
            jobId,
            workerId: uid,
            deepLink: `dutype://employer/applications/${jobId}`,
        });
        await logUserEvent(uid, "worker_job_request_accepted", "WORKER", { jobId, employerId, requestId });
    }
    const out = {
        success: true,
        status: txResult.accepted ? "accepted" : String(txResult.status || "expired"),
        jobId,
    };
    await idem.record(out);
    return out;
});
// ────────────────────────────────────────────────────────────────────────
// submitApplication
// ────────────────────────────────────────────────────────────────────────
const MIN_WORKER_PROFILE_SCORE = 80;
const MAX_APPLICATIONS_PER_HOUR = 10;
exports.submitApplication = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a;
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
    const [workerProfileSnap, jobSnap] = await Promise.all([
        db().collection("worker_profiles").doc(uid).get(),
        db().collection("jobmetadata").doc(jobId).get(),
    ]);
    if (!workerProfileSnap.exists) {
        throw new functions.https.HttpsError("failed-precondition", "Worker profile not set up");
    }
    const worker = (workerProfileSnap.data() || {});
    const profileRole = String(worker.role || "WORKER").toUpperCase();
    if (profileRole !== "WORKER") {
        throw new functions.https.HttpsError("failed-precondition", "WORKER role required");
    }
    const workerScoreRaw = (_a = worker.profileScore) !== null && _a !== void 0 ? _a : 0;
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
    const applicationData = {
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
    await idem.record(out);
    return out;
});
//# sourceMappingURL=auth-callables.js.map