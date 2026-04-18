"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.onCallSecured = void 0;
/**
 * Hardened wrapper around functions.https.onCall:
 *   • runtime options pinned (region asia-south1, memory, timeout)
 *   • consumeAppCheckToken rejects tokens already used (replay protection)
 *   • reject unverified callers — App Check must be present
 *   • auth required unless explicitly opted out
 */
const functions = require("firebase-functions");
const DEFAULT_RUNTIME = {
    memory: "256MB",
    timeoutSeconds: 60,
    // Enforce App Check at the platform layer. Requests without a valid App
    // Check token are rejected before our handler runs.
    enforceAppCheck: true,
    consumeAppCheckToken: true,
};
function onCallSecured(opts, handler) {
    var _a, _b, _c, _d;
    const runtime = Object.assign(Object.assign({}, DEFAULT_RUNTIME), { memory: (_a = opts.memory) !== null && _a !== void 0 ? _a : DEFAULT_RUNTIME.memory, timeoutSeconds: (_b = opts.timeoutSeconds) !== null && _b !== void 0 ? _b : DEFAULT_RUNTIME.timeoutSeconds, enforceAppCheck: (_c = opts.enforceAppCheck) !== null && _c !== void 0 ? _c : DEFAULT_RUNTIME.enforceAppCheck, consumeAppCheckToken: (_d = opts.enforceAppCheck) !== null && _d !== void 0 ? _d : DEFAULT_RUNTIME.consumeAppCheckToken });
    return functions
        .region("asia-south1")
        .runWith(runtime)
        .https.onCall(async (data, context) => {
        var _a, _b;
        // Belt-and-suspenders: even though enforceAppCheck=true blocks at the
        // platform, re-assert here so older-runtime deployments still fail closed.
        if (((_a = runtime.enforceAppCheck) !== null && _a !== void 0 ? _a : true) && !context.app) {
            throw new functions.https.HttpsError("failed-precondition", "app check required");
        }
        if (((_b = opts.requireAuth) !== null && _b !== void 0 ? _b : true) && !context.auth) {
            throw new functions.https.HttpsError("unauthenticated", "login required");
        }
        return handler(data, context);
    });
}
exports.onCallSecured = onCallSecured;
//# sourceMappingURL=secure-callable.js.map