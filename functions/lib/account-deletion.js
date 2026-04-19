"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteAccount = void 0;
/**
 * Account deletion (Play Store + GDPR + DPDP Act compliance).
 *
 * Strategy: tombstone the user document and cascade-handle related entities
 * rather than hard-deleting everything (which would destroy referral audit
 * trails and other-side application history).
 *
 * Cascade outcomes when a user calls deleteAccount:
 *
 *   users/{uid}                — kept, fields stripped, status='deleted',
 *                                deletedAt set. Allows other-party reads
 *                                (e.g. an employer's view of a worker who
 *                                already worked for them) to keep working
 *                                without crashing.
 *   phone_index/{phoneE164}    — DELETED so the phone can be re-registered.
 *   worker_profiles/{uid}      — deleted (PII heavy).
 *   employer_profiles/{uid}    — deleted (PII heavy).
 *   applications (worker side) — status='withdrawn' if not terminal.
 *   jobmetadata (employer side)— status='closed' (preserves analytics).
 *   saved_jobs (worker side)   — deleted.
 *   notifications (recipientId)— deleted in batches.
 *   referral_codes/{code}      — set isActive=false (audit preserved).
 *   referral_stats/{uid}       — kept (audit trail).
 *   ratings                    — kept (the other side's reputation history).
 *   user_events                — kept + new event 'account_deleted' appended.
 *   fcmTokens                  — cleared.
 *
 * After the callable returns success, the client must:
 *   1. firebase.auth().currentUser.delete()   — removes the Auth identity
 *   2. local cache wipe + sign-out flow
 *
 * The callable is idempotent: a re-run on an already-tombstoned user is a
 * no-op that returns success.
 */
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const secure_callable_1 = require("./secure-callable");
const idempotency_1 = require("./idempotency");
const db = () => admin.firestore();
const BATCH_SIZE = 400;
async function deleteByQuery(query) {
    let total = 0;
    let cursor;
    // eslint-disable-next-line no-constant-condition
    while (true) {
        let q = query.limit(BATCH_SIZE);
        if (cursor)
            q = q.startAfter(cursor);
        const snap = await q.get();
        if (snap.empty)
            break;
        const batch = db().batch();
        snap.docs.forEach((d) => batch.delete(d.ref));
        await batch.commit();
        total += snap.size;
        if (snap.size < BATCH_SIZE)
            break;
        cursor = snap.docs[snap.docs.length - 1];
    }
    return total;
}
async function updateByQuery(query, patch) {
    let total = 0;
    let cursor;
    // eslint-disable-next-line no-constant-condition
    while (true) {
        let q = query.limit(BATCH_SIZE);
        if (cursor)
            q = q.startAfter(cursor);
        const snap = await q.get();
        if (snap.empty)
            break;
        const batch = db().batch();
        snap.docs.forEach((d) => batch.update(d.ref, patch));
        await batch.commit();
        total += snap.size;
        if (snap.size < BATCH_SIZE)
            break;
        cursor = snap.docs[snap.docs.length - 1];
    }
    return total;
}
exports.deleteAccount = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 540, memory: "512MB" }, async (data, context) => {
    var _a, _b, _c, _d;
    const uid = context.auth.uid;
    const reason = String((_a = data === null || data === void 0 ? void 0 : data.reason) !== null && _a !== void 0 ? _a : "user_requested").slice(0, 200);
    const idem = await (0, idempotency_1.withIdempotency)(uid, "deleteAccount", data === null || data === void 0 ? void 0 : data.idempotencyKey, 7 * 24 * 60 * 60 * 1000 // 7-day idempotency window
    );
    if (idem.hit)
        return idem.result;
    const userRef = db().collection("users").doc(uid);
    const userSnap = await userRef.get();
    if (!userSnap.exists) {
        const noop = {
            success: true,
            alreadyDeleted: true,
            closedJobs: 0,
            withdrawnApplications: 0,
            deletedSavedJobs: 0,
            deletedNotifications: 0,
        };
        await idem.record(noop);
        return noop;
    }
    const user = userSnap.data() || {};
    if (user.status === "deleted") {
        const noop = {
            success: true,
            alreadyDeleted: true,
            closedJobs: 0,
            withdrawnApplications: 0,
            deletedSavedJobs: 0,
            deletedNotifications: 0,
        };
        await idem.record(noop);
        return noop;
    }
    const phone = String((_c = (_b = user.phone) !== null && _b !== void 0 ? _b : user.phoneNumber) !== null && _c !== void 0 ? _c : "").trim();
    const referralCode = String((_d = user.referralCode) !== null && _d !== void 0 ? _d : "").trim();
    // ── 1. Close active jobs (employer side) ────────────────────────
    const closedJobs = await updateByQuery(db()
        .collection("jobmetadata")
        .where("employerId", "==", uid)
        .where("status", "in", ["open", "active"]), {
        status: "closed",
        closedReason: "account_deleted",
        closedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    // ── 2. Withdraw active applications (worker side) ───────────────
    const withdrawnApplications = await updateByQuery(db()
        .collection("applications")
        .where("workerId", "==", uid)
        .where("status", "in", ["applied", "shortlisted"]), {
        status: "withdrawn",
        withdrawReason: "account_deleted",
        lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    // ── 3. Delete saved_jobs and notifications ──────────────────────
    const deletedSavedJobs = await deleteByQuery(db().collection("saved_jobs").where("userId", "==", uid));
    const deletedNotifications = await deleteByQuery(db().collection("notifications").where("recipientId", "==", uid));
    // ── 4. Delete role profiles (PII heavy) ─────────────────────────
    await Promise.all([
        db()
            .collection("worker_profiles")
            .doc(uid)
            .delete()
            .catch(() => undefined),
        db()
            .collection("employer_profiles")
            .doc(uid)
            .delete()
            .catch(() => undefined),
    ]);
    // ── 5. Deactivate referral_codes/{code} ─────────────────────────
    if (referralCode) {
        await db()
            .collection("referral_codes")
            .doc(referralCode)
            .set({
            isActive: false,
            deactivatedAt: admin.firestore.FieldValue.serverTimestamp(),
            deactivatedReason: "owner_account_deleted",
        }, { merge: true })
            .catch((err) => functions.logger.warn("referral_codes deactivate failed", err));
    }
    // ── 6. Tombstone users/{uid} ────────────────────────────────────
    await userRef.set({
        userId: uid,
        status: "deleted",
        deletedAt: admin.firestore.FieldValue.serverTimestamp(),
        deletedReason: reason,
        // strip PII
        phone: admin.firestore.FieldValue.delete(),
        phoneNumber: admin.firestore.FieldValue.delete(),
        fullName: admin.firestore.FieldValue.delete(),
        profileImageUrl: admin.firestore.FieldValue.delete(),
        location: admin.firestore.FieldValue.delete(),
        geohash: admin.firestore.FieldValue.delete(),
        fcmToken: admin.firestore.FieldValue.delete(),
        fcmTokens: admin.firestore.FieldValue.delete(),
        notificationBuckets: admin.firestore.FieldValue.delete(),
        // keep referralCode + referredByUserId for audit linkage
    }, { merge: true });
    // ── 7. Free up phone for re-registration ────────────────────────
    if (phone && /^\+[1-9][0-9]{6,14}$/.test(phone)) {
        await db()
            .collection("phone_index")
            .doc(phone)
            .delete()
            .catch((err) => functions.logger.warn("phone_index delete failed", err));
    }
    // ── 8. Lifecycle event ──────────────────────────────────────────
    await db()
        .collection("user_events")
        .add({
        uid,
        type: "account_deleted",
        role: "",
        payload: {
            reason,
            closedJobs,
            withdrawnApplications,
            deletedSavedJobs,
            deletedNotifications,
        },
        at: admin.firestore.FieldValue.serverTimestamp(),
    })
        .catch((err) => functions.logger.warn("user_events log failed", err));
    const out = {
        success: true,
        alreadyDeleted: false,
        closedJobs,
        withdrawnApplications,
        deletedSavedJobs,
        deletedNotifications,
    };
    await idem.record(out);
    return out;
});
//# sourceMappingURL=account-deletion.js.map