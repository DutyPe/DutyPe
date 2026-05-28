"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.mirrorAuthUserIdentity = exports.mirrorEmployerProfileIdentity = exports.mirrorWorkerProfileIdentity = exports.mirrorPhoneRoleIdentity = void 0;
const admin = require("firebase-admin");
const functions = require("firebase-functions");
const db = admin.firestore();
function cleanString(value) {
    return typeof value === "string" ? value.trim() : "";
}
function firstNonEmptyString(...values) {
    for (const value of values) {
        const text = cleanString(value);
        if (text)
            return text;
    }
    return "";
}
function normalizeRole(value) {
    const role = cleanString(value).toUpperCase();
    return role === "WORKER" || role === "EMPLOYER" ? role : "";
}
function normalizePhone(value) {
    const text = cleanString(value);
    if (!text)
        return "";
    if (/^\+[1-9]\d{6,14}$/.test(text))
        return text;
    const digits = text.replace(/\D/g, "");
    if (digits.length === 10)
        return `+91${digits}`;
    if (digits.length >= 7 && digits.length <= 15)
        return `+${digits}`;
    return "";
}
function profileCollectionForRole(role) {
    return role === "EMPLOYER" ? "employer_profiles" : "worker_profiles";
}
function copyIfPresent(target, targetKey, source, ...sourceKeys) {
    for (const key of sourceKeys) {
        const value = source[key];
        if (value !== undefined && value !== null && value !== "") {
            target[targetKey] = value;
            return;
        }
    }
}
async function mirrorToUserDoc(userId, role, source, mirrorSource, extras = {}) {
    if (!userId)
        return;
    const fullName = firstNonEmptyString(source.fullName, source.name, source.companyName, extras.fullName, extras.name, extras.displayName);
    const phone = normalizePhone(firstNonEmptyString(source.phone, source.phoneNumber, extras.phone, extras.phoneNumber));
    const email = firstNonEmptyString(source.email, extras.email);
    const payload = {
        userId,
        uid: userId,
        identityMirrorSource: mirrorSource,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    if (role) {
        payload.role = role;
        payload.activeRole = role;
    }
    if (fullName) {
        payload.fullName = fullName;
        payload.name = fullName;
    }
    if (phone) {
        payload.phone = phone;
        payload.phoneNumber = phone;
    }
    if (email) {
        payload.email = email;
    }
    copyIfPresent(payload, "companyName", source, "companyName");
    copyIfPresent(payload, "profileImageUrl", source, "profileImageUrl");
    copyIfPresent(payload, "referralCode", source, "referralCode");
    copyIfPresent(payload, "referredByCode", source, "referredByCode");
    copyIfPresent(payload, "referredByUserId", source, "referredByUserId");
    copyIfPresent(payload, "createdAt", source, "createdAt");
    await db.collection("users").doc(userId).set(payload, { merge: true });
}
async function ensureMissingProfileFromPhoneRole(userId, phone, role, data) {
    const profileRef = db.collection(profileCollectionForRole(role)).doc(userId);
    const profileSnap = await profileRef.get();
    if (profileSnap.exists)
        return;
    const name = firstNonEmptyString(data.fullName, data.name);
    const payload = {
        userId,
        role,
        phone,
        createdAt: data.createdAt || admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    if (name)
        payload.fullName = name;
    copyIfPresent(payload, "referralCode", data, "referralCode");
    copyIfPresent(payload, "referredByCode", data, "referredByCode");
    copyIfPresent(payload, "referredByUserId", data, "referredByUserId");
    await profileRef.set(payload, { merge: true });
}
async function mirrorProfileToPhoneRole(userId, role, data) {
    const phone = normalizePhone(firstNonEmptyString(data.phone, data.phoneNumber));
    if (!phone)
        return;
    const name = firstNonEmptyString(data.fullName, data.name, data.companyName);
    const payload = {
        phoneNumber: phone,
        uid: userId,
        role,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    if (name)
        payload.name = name;
    copyIfPresent(payload, "createdAt", data, "createdAt");
    copyIfPresent(payload, "referralCode", data, "referralCode");
    copyIfPresent(payload, "referredByCode", data, "referredByCode");
    copyIfPresent(payload, "referredByUserId", data, "referredByUserId");
    await db.collection("phoneRoles").doc(phone).set(payload, { merge: true });
}
exports.mirrorPhoneRoleIdentity = functions.firestore
    .document("phoneRoles/{phone}")
    .onWrite(async (change, context) => {
    if (!change.after.exists)
        return null;
    const data = (change.after.data() || {});
    const userId = firstNonEmptyString(data.uid, data.userId);
    const role = normalizeRole(data.role);
    const phone = normalizePhone(firstNonEmptyString(data.phoneNumber, data.phone, context.params.phone));
    if (!userId)
        return null;
    await Promise.all([
        mirrorToUserDoc(userId, role, Object.assign(Object.assign({}, data), { phoneNumber: phone }), "phoneRoles"),
        role && phone ? ensureMissingProfileFromPhoneRole(userId, phone, role, data) : Promise.resolve(),
    ]);
    return null;
});
exports.mirrorWorkerProfileIdentity = functions.firestore
    .document("worker_profiles/{userId}")
    .onWrite(async (change, context) => {
    if (!change.after.exists)
        return null;
    const data = (change.after.data() || {});
    const userId = context.params.userId;
    await Promise.all([
        mirrorToUserDoc(userId, "WORKER", data, "worker_profiles"),
        mirrorProfileToPhoneRole(userId, "WORKER", data),
    ]);
    return null;
});
exports.mirrorEmployerProfileIdentity = functions.firestore
    .document("employer_profiles/{userId}")
    .onWrite(async (change, context) => {
    if (!change.after.exists)
        return null;
    const data = (change.after.data() || {});
    const userId = context.params.userId;
    await Promise.all([
        mirrorToUserDoc(userId, "EMPLOYER", data, "employer_profiles"),
        mirrorProfileToPhoneRole(userId, "EMPLOYER", data),
    ]);
    return null;
});
exports.mirrorAuthUserIdentity = functions.auth.user().onCreate(async (user) => {
    await mirrorToUserDoc(user.uid, "", {
        phoneNumber: user.phoneNumber || "",
        email: user.email || "",
        fullName: user.displayName || "",
        createdAt: user.metadata.creationTime
            ? admin.firestore.Timestamp.fromDate(new Date(user.metadata.creationTime))
            : admin.firestore.FieldValue.serverTimestamp(),
    }, "auth", {
        displayName: user.displayName || "",
        phoneNumber: user.phoneNumber || "",
        email: user.email || "",
    });
});
//# sourceMappingURL=identity-mirror.js.map