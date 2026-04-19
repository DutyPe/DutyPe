"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.setNotificationBuckets = exports.unregisterFcmToken = exports.registerFcmToken = void 0;
/**
 * Multi-device FCM token management + role-bucketed notification subscriptions.
 *
 * Today the app stores a single `users.fcmToken` field, which means a user with
 * two devices loses pushes to one of them whenever they open the other. The
 * legacy mega-topics (`workers`, `employers`, `all_users`) cannot slice by city,
 * pincode, or job type, so any future targeted campaign is impossible.
 *
 * This module gives the server two callables:
 *
 *   • registerFcmToken({token, platform, deviceId, role})
 *       — appends to users/{uid}.fcmTokens (cap at 5 most-recent),
 *         removes stale entries (>60 days), updates lastSeenAt.
 *
 *   • setNotificationBuckets({city, pincode, jobTypes[], role})
 *       — records the active bucket topics on users/{uid}.notificationBuckets.
 *         The Android client should subscribe locally to every topic in this
 *         list and unsubscribe from anything else.
 *
 * The push-fanout function (in index.ts) should be migrated to read
 * `users.fcmTokens[].token` and call messaging.sendEach(...) instead of
 * messaging.send(token). That migration is intentionally NOT done here so
 * the legacy single-token push path keeps working until both clients are
 * on the new schema.
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const secure_callable_1 = require("./secure-callable");
const validation_1 = require("./validation");
const db = () => admin.firestore();
const MAX_TOKENS_PER_USER = 5;
const TOKEN_STALE_MS = 60 * 24 * 60 * 60 * 1000; // 60 days
const VALID_ROLES = ["WORKER", "EMPLOYER"];
const VALID_PLATFORMS = ["android", "ios", "web"];
const MAX_BUCKETS = 8;
exports.registerFcmToken = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a;
    const uid = context.auth.uid;
    const token = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.token, "token", {
        required: true,
        minLength: 32,
        maxLength: 4096,
    });
    const platform = (0, validation_1.validateEnum)(String((_a = data === null || data === void 0 ? void 0 : data.platform) !== null && _a !== void 0 ? _a : "").toLowerCase(), "platform", VALID_PLATFORMS);
    const deviceId = (0, validation_1.validateString)(data === null || data === void 0 ? void 0 : data.deviceId, "deviceId", {
        required: true,
        minLength: 4,
        maxLength: 128,
    });
    const role = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.role, "role", VALID_ROLES);
    const userRef = db().collection("users").doc(uid);
    const result = await db().runTransaction(async (tx) => {
        var _a;
        const snap = await tx.get(userRef);
        if (!snap.exists) {
            throw new functions.https.HttpsError("failed-precondition", "User not found");
        }
        const existing = Array.isArray((_a = snap.data()) === null || _a === void 0 ? void 0 : _a.fcmTokens)
            ? snap.data().fcmTokens
            : [];
        const now = admin.firestore.Timestamp.now();
        const stale = now.toMillis() - TOKEN_STALE_MS;
        // Deduplicate by deviceId OR token, drop stale, then insert/refresh ours.
        const filtered = existing.filter((t) => {
            var _a, _b, _c;
            return t.deviceId !== deviceId &&
                t.token !== token &&
                ((_c = (_b = (_a = t.lastSeenAt) === null || _a === void 0 ? void 0 : _a.toMillis) === null || _b === void 0 ? void 0 : _b.call(_a)) !== null && _c !== void 0 ? _c : 0) >= stale;
        });
        filtered.push({
            token,
            platform,
            deviceId,
            role,
            addedAt: now,
            lastSeenAt: now,
        });
        // Keep most recent N (sorted by lastSeenAt desc).
        filtered.sort((a, b) => b.lastSeenAt.toMillis() - a.lastSeenAt.toMillis());
        const trimmed = filtered.slice(0, MAX_TOKENS_PER_USER);
        tx.update(userRef, {
            fcmTokens: trimmed,
            // Keep legacy single-token field in sync for backward compatibility.
            fcmToken: token,
            lastActiveAt: now,
        });
        return { tokenCount: trimmed.length };
    });
    return { success: true, tokenCount: result.tokenCount };
});
exports.unregisterFcmToken = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    const uid = context.auth.uid;
    const token = ((data === null || data === void 0 ? void 0 : data.token) || "").trim();
    const deviceId = ((data === null || data === void 0 ? void 0 : data.deviceId) || "").trim();
    if (!token && !deviceId) {
        throw new functions.https.HttpsError("invalid-argument", "Provide token or deviceId");
    }
    const userRef = db().collection("users").doc(uid);
    let removed = 0;
    await db().runTransaction(async (tx) => {
        var _a;
        const snap = await tx.get(userRef);
        if (!snap.exists)
            return;
        const existing = Array.isArray((_a = snap.data()) === null || _a === void 0 ? void 0 : _a.fcmTokens)
            ? snap.data().fcmTokens
            : [];
        const filtered = existing.filter((t) => {
            const matches = (token && t.token === token) || (deviceId && t.deviceId === deviceId);
            if (matches)
                removed++;
            return !matches;
        });
        if (removed === 0)
            return;
        tx.update(userRef, { fcmTokens: filtered });
    });
    return { success: true, removed };
});
/**
 * Bucket topic format:  worker_<city>_<pincode>_<jobType>
 * All segments are normalized to [a-z0-9] and lowercased so they are valid
 * FCM topic names. Empty segments collapse to "any".
 */
function buildBucketTopic(role, city, pincode, jobType) {
    const norm = (s) => (s || "any").toLowerCase().replace(/[^a-z0-9]/g, "").slice(0, 32) || "any";
    return `${role.toLowerCase()}_${norm(city)}_${norm(pincode)}_${norm(jobType)}`;
}
exports.setNotificationBuckets = (0, secure_callable_1.onCallSecured)({}, async (data, context) => {
    var _a, _b;
    const uid = context.auth.uid;
    const role = (0, validation_1.validateEnum)(data === null || data === void 0 ? void 0 : data.role, "role", VALID_ROLES);
    const city = String((_a = data === null || data === void 0 ? void 0 : data.city) !== null && _a !== void 0 ? _a : "").trim();
    const pincode = String((_b = data === null || data === void 0 ? void 0 : data.pincode) !== null && _b !== void 0 ? _b : "").trim();
    const jobTypesIn = Array.isArray(data === null || data === void 0 ? void 0 : data.jobTypes)
        ? data.jobTypes.map((j) => String(j).trim()).filter(Boolean)
        : [];
    // At least one bucket; cap at MAX_BUCKETS.
    const jobTypes = jobTypesIn.slice(0, MAX_BUCKETS);
    const topics = [];
    if (jobTypes.length === 0) {
        topics.push(buildBucketTopic(role, city, pincode, ""));
    }
    else {
        for (const jt of jobTypes) {
            topics.push(buildBucketTopic(role, city, pincode, jt));
        }
    }
    const dedup = Array.from(new Set(topics)).slice(0, MAX_BUCKETS);
    await db().collection("users").doc(uid).set({
        notificationBuckets: {
            role,
            city,
            pincode,
            jobTypes,
            topics: dedup,
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
    }, { merge: true });
    return { success: true, topics: dedup };
});
//# sourceMappingURL=fcm-tokens.js.map