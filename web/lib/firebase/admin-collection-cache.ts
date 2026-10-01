import "server-only";

import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

/**
 * Admin pages used to read whole collections (users, phoneRoles, referral_codes,
 * worker_profiles, ...) on EVERY page load: ~7,000 billed reads each time. This keeps one
 * snapshot per collection for a few minutes, shared by all admin routes on the instance.
 */
const TTL_MS = 5 * 60 * 1000;
const cache = new Map<string, { at: number; promise: Promise<FirebaseFirestore.QuerySnapshot> }>();

export function cachedCollection(name: string, limit = 5000): Promise<FirebaseFirestore.QuerySnapshot> {
  const key = `${name}|${limit}`;
  const hit = cache.get(key);
  if (hit && Date.now() - hit.at < TTL_MS) return hit.promise;
  const promise = getFirebaseAdminDb().collection(name).limit(limit).get();
  cache.set(key, { at: Date.now(), promise });
  promise.catch(() => cache.delete(key));
  return promise;
}

/** Call after admin writes so the next page load shows fresh data. */
export function invalidateCollectionCache(...names: string[]) {
  for (const key of Array.from(cache.keys())) {
    if (names.length === 0 || names.some((n) => key.startsWith(`${n}|`))) cache.delete(key);
  }
}
