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
import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

const DEFAULT_TTL_MS = 24 * 60 * 60 * 1000; // 24h
const IDEMPOTENCY_COLLECTION = "idempotency";
const KEY_PATTERN = /^[A-Za-z0-9_-]{8,128}$/;

export interface IdempotencyResult<R = unknown> {
  hit: boolean;
  result?: R | null;
  record?: (result: R) => Promise<void>;
}

/**
 * Validate caller-supplied idempotency key and look up any cached result.
 *
 * Returns either a cached `result` (caller MUST short-circuit) or a `record`
 * callback that the caller MUST invoke after the side-effect succeeds.
 */
export async function withIdempotency<R = unknown>(
  uid: string,
  op: string,
  rawKey: unknown,
  ttlMs: number = DEFAULT_TTL_MS
): Promise<IdempotencyResult<R>> {
  if (!rawKey) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "idempotencyKey is required"
    );
  }
  const key = String(rawKey).trim();
  if (!KEY_PATTERN.test(key)) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "idempotencyKey must be 8-128 chars [A-Za-z0-9_-]"
    );
  }

  const db = admin.firestore();
  const docId = `${uid}_${op}_${key}`;
  const ref = db.collection(IDEMPOTENCY_COLLECTION).doc(docId);
  const snap = await ref.get();
  if (snap.exists) {
    const data = snap.data() || {};
    return { hit: true, result: (data.result ?? null) as R | null };
  }

  const record = async (result: R): Promise<void> => {
    await ref.set({
      uid,
      op,
      key,
      result: result ?? null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      expireAt: admin.firestore.Timestamp.fromMillis(Date.now() + ttlMs),
    });
  };

  return { hit: false, record };
}
