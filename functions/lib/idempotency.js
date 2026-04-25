"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.withIdempotency = void 0;
/**
 * Idempotency helper for callable Cloud Functions.
 *
 * Stores a small record at idempotency/{uid}_{op}_{key} containing the
 * cached result so that retries within the TTL window short-circuit and
 * return the original outcome instead of re-executing the side effect.
 *
 * The collection is intended to be pruned by Firestore TTL on `expireAt`.
 *   gcloud firestore fields ttls update expireAt --collection-group=idempotency
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const DEFAULT_TTL_MS = 24 * 60 * 60 * 1000; // 24h
const IDEMPOTENCY_COLLECTION = "idempotency";
const KEY_PATTERN = /^[A-Za-z0-9_-]{8,128}$/;
/**
 * Validate caller-supplied idempotency key and look up any cached result.
 *
 * Returns either a cached `result` (caller MUST short-circuit) or a `record`
 * callback that the caller MUST invoke after the side-effect succeeds.
 */
async function withIdempotency(uid, op, rawKey, ttlMs = DEFAULT_TTL_MS) {
    var _a;
    if (!rawKey) {
        throw new functions.https.HttpsError("invalid-argument", "idempotencyKey is required");
    }
    const key = String(rawKey).trim();
    if (!KEY_PATTERN.test(key)) {
        throw new functions.https.HttpsError("invalid-argument", "idempotencyKey must be 8-128 chars [A-Za-z0-9_-]");
    }
    const db = admin.firestore();
    const docId = `${uid}_${op}_${key}`;
    const ref = db.collection(IDEMPOTENCY_COLLECTION).doc(docId);
    const snap = await ref.get();
    if (snap.exists) {
        const data = snap.data() || {};
        return { hit: true, result: ((_a = data.result) !== null && _a !== void 0 ? _a : null) };
    }
    const record = async (result) => {
        await ref.set({
            uid,
            op,
            key,
            result: result !== null && result !== void 0 ? result : null,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
            expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + ttlMs),
        });
    };
    return { hit: false, record };
}
exports.withIdempotency = withIdempotency;
//# sourceMappingURL=idempotency.js.map