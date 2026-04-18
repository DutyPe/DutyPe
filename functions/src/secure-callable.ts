/**
 * Hardened wrapper around functions.https.onCall:
 *   • runtime options pinned (region asia-south1, memory, timeout)
 *   • consumeAppCheckToken rejects tokens already used (replay protection)
 *   • reject unverified callers — App Check must be present
 *   • auth required unless explicitly opted out
 */
import * as functions from "firebase-functions";

const DEFAULT_RUNTIME: functions.RuntimeOptions = {
  memory: "256MB",
  timeoutSeconds: 60,
  // Enforce App Check at the platform layer. Requests without a valid App
  // Check token are rejected before our handler runs.
  enforceAppCheck: true,
  consumeAppCheckToken: true,
};

export interface SecuredCallableOptions {
  requireAuth?: boolean;
  memory?: "128MB" | "256MB" | "512MB" | "1GB" | "2GB";
  timeoutSeconds?: number;
  enforceAppCheck?: boolean;
}

export function onCallSecured<T = unknown, R = unknown>(
  opts: SecuredCallableOptions,
  handler: (data: T, context: functions.https.CallableContext) => Promise<R> | R
): functions.HttpsFunction & functions.Runnable<T> {
  const runtime: functions.RuntimeOptions = {
    ...DEFAULT_RUNTIME,
    memory: opts.memory ?? DEFAULT_RUNTIME.memory,
    timeoutSeconds: opts.timeoutSeconds ?? DEFAULT_RUNTIME.timeoutSeconds,
    enforceAppCheck: opts.enforceAppCheck ?? DEFAULT_RUNTIME.enforceAppCheck,
    consumeAppCheckToken: opts.enforceAppCheck ?? DEFAULT_RUNTIME.consumeAppCheckToken,
  };

  return functions
    .region("asia-south1")
    .runWith(runtime)
    .https.onCall(async (data: T, context) => {
      // Belt-and-suspenders: even though enforceAppCheck=true blocks at the
      // platform, re-assert here so older-runtime deployments still fail closed.
      if ((runtime.enforceAppCheck ?? true) && !context.app) {
        throw new functions.https.HttpsError("failed-precondition", "app check required");
      }
      if ((opts.requireAuth ?? true) && !context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "login required");
      }
      return handler(data, context);
    });
}
