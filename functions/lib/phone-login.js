"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.truecallerSignIn = exports.verifyWhatsappOtp = exports.sendWhatsappOtp = exports.net = exports.WHATSAPP_SECRET = void 0;
exports.uidForPhone = uidForPhone;
/**
 * WhatsApp phone authentication (Meta WhatsApp Cloud API, authentication template):
 *
 *   sendWhatsappOtp({ phone, role, mode? }) sends a 6-digit code on WhatsApp.
 *   verifyWhatsappOtp({ phone, code })      checks the code; returns a Firebase custom token for the
 *                                           phone's account.
 *
 * Settings: functions/.env.dutype-860ac (WHATSAPP_PHONE_NUMBER_ID, WHATSAPP_TEMPLATE,
 * WHATSAPP_TEMPLATE_LANG, WHATSAPP_DAILY_CAP=5000); secret WHATSAPP_TOKEN.
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const input_1 = require("./lib/input");
const phone_otp_1 = require("./lib/phone-otp");
const schema_1 = require("./schema");
const db = admin.firestore();
const { Timestamp } = admin.firestore;
exports.WHATSAPP_SECRET = "WHATSAPP_TOKEN";
const DAY_MS = 24 * 60 * 60 * 1000;
const ROLES = [schema_1.Values.Role.WORKER, schema_1.Values.Role.EMPLOYER];
/** Test seam: replaced in tests so no network call is made. */
exports.net = {
    fetch: (url, init) => fetch(url, init),
};
function requestedRole(data) {
    const role = String(data.role || "").toUpperCase();
    return ROLES.includes(role) ? role : "";
}
async function registeredRole(phone) {
    const doc = await db.collection(schema_1.PhoneRoles.COLLECTION).doc(phone).get();
    return doc.exists ? String(doc.get(schema_1.PhoneRoles.ROLE) || "") || null : null;
}
/** The Firebase Auth account for this number: the one SMS login uses, or a new one. */
async function uidForPhone(phone) {
    const find = async () => {
        try {
            return await admin.auth().getUserByPhoneNumber(phone);
        }
        catch (e) {
            if (e.code === "auth/user-not-found")
                return null;
            throw e;
        }
    };
    let user = await find();
    if (!user) {
        try {
            user = await admin.auth().createUser({ phoneNumber: phone });
        }
        catch (e) {
            // Created at the same moment by another request.
            if (e.code !== "auth/phone-number-already-exists")
                throw e;
            user = await find();
        }
    }
    if (!user)
        (0, input_1.fail)("internal", "Could not sign in");
    if (user.disabled)
        (0, input_1.fail)("permission-denied", "This account is blocked. Contact DutyPe support.");
    return user.uid;
}
// ─────────────────────────────── WhatsApp ───────────────────────────────
async function sendOnWhatsapp(phone, code) {
    const version = process.env.WHATSAPP_API_VERSION || "v21.0";
    const url = `https://graph.facebook.com/${version}/${process.env.WHATSAPP_PHONE_NUMBER_ID}/messages`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 10000);
    try {
        const res = await exports.net.fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${(process.env.WHATSAPP_TOKEN || "").trim()}` },
            body: JSON.stringify((0, phone_otp_1.whatsappTemplateBody)(phone, code, process.env.WHATSAPP_TEMPLATE || "", process.env.WHATSAPP_TEMPLATE_LANG || "en")),
            signal: controller.signal,
        });
        if (!res.ok) {
            functions.logger.warn(`WhatsApp send failed ${res.status}`, (await res.text()).slice(0, 300));
            return false;
        }
        return true;
    }
    catch (e) {
        functions.logger.warn("WhatsApp send error", e);
        return false;
    }
    finally {
        clearTimeout(timer);
    }
}
/**
 * WhatsApp login code (Meta Cloud API, authentication template).
 * Returns { sent: true, channel: "whatsapp" } on success.
 * Enforces per-number rate limits, IP rate limits, and daily cap (WHATSAPP_DAILY_CAP = 5000).
 */
exports.sendWhatsappOtp = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 25, secrets: [exports.WHATSAPP_SECRET] }, async (raw, context) => {
    var _a;
    const data = (0, input_1.obj)(raw);
    const phone = (0, phone_otp_1.indianE164)(data.phone);
    if (!phone)
        (0, input_1.fail)("invalid-argument", "Enter a valid 10-digit mobile number");
    const ip = String(((_a = context === null || context === void 0 ? void 0 : context.rawRequest) === null || _a === void 0 ? void 0 : _a.ip) || "");
    if (!(0, phone_otp_1.whatsappConfigured)()) {
        (0, input_1.fail)("unavailable", "WhatsApp OTP service is currently not configured.");
    }
    // Never spend on a number registered with the other role (the app checks too).
    const role = requestedRole(data);
    const existing = await registeredRole(phone);
    if (role && existing && existing !== role)
        (0, input_1.fail)("failed-precondition", `phone-already-registered-as:${existing}`);
    const mode = String(data.mode || "");
    if (mode === "login" && !existing)
        (0, input_1.fail)("not-found", "No account found with this number. Please register first.");
    if (mode === "register" && existing)
        (0, input_1.fail)("already-exists", "An account already exists with this number. Please log in.");
    const now = Date.now();
    const waCap = Number(process.env.WHATSAPP_DAILY_CAP || 5000);
    let code = (0, phone_otp_1.newCode)();
    const codeRef = db.collection(schema_1.OtpCodes.COLLECTION).doc(phone);
    const dayRef = db.collection(schema_1.OtpDaily.COLLECTION).doc((0, phone_otp_1.istDayKey)(now));
    const ipRef = ip ? db.collection(schema_1.OtpIp.COLLECTION).doc((0, phone_otp_1.ipKey)(ip, now)) : null;
    const decision = await db.runTransaction(async (tx) => {
        const [prev, day, ipDoc] = await Promise.all([
            tx.get(codeRef),
            tx.get(dayRef),
            ipRef ? tx.get(ipRef) : Promise.resolve(null),
        ]);
        if (ipDoc && Number(ipDoc.get(schema_1.OtpIp.COUNT) || 0) >= phone_otp_1.MAX_PER_IP_HOUR) {
            return { ok: false, reason: "ip", retryAfterSec: 3600 };
        }
        const waCount = Number(day.get(schema_1.OtpDaily.COUNT) || 0);
        if (waCount >= waCap) {
            return { ok: false, reason: "cap", retryAfterSec: 0 };
        }
        const d = (0, phone_otp_1.decideSend)(prev.exists ? {
            lastSentAt: Number(prev.get(schema_1.OtpCodes.LAST_SENT_AT) || 0),
            hourStart: Number(prev.get(schema_1.OtpCodes.HOUR_START) || 0),
            hourCount: Number(prev.get(schema_1.OtpCodes.HOUR_COUNT) || 0),
            dayKey: String(prev.get(schema_1.OtpCodes.DAY_KEY) || ""),
            dayCount: Number(prev.get(schema_1.OtpCodes.DAY_COUNT) || 0),
        } : null, now);
        if (!d.ok)
            return d;
        // Resend within the window: the same digits, 10 more minutes (max 30 from the first send).
        const issuedAt = Number(prev.get(schema_1.OtpCodes.ISSUED_AT) || 0);
        const previous = prev.exists && (0, phone_otp_1.reuseCode)({
            hasCode: Boolean(prev.get(schema_1.OtpCodes.HASH)) && Boolean(prev.get(schema_1.OtpCodes.SEALED)),
            expiresAt: Number(prev.get(schema_1.OtpCodes.EXPIRES_AT) || 0),
            issuedAt,
            attempts: Number(prev.get(schema_1.OtpCodes.ATTEMPTS) || 0),
        }, now) ? (0, phone_otp_1.openCode)(String(prev.get(schema_1.OtpCodes.SEALED))) : null;
        if (previous)
            code = previous;
        tx.set(codeRef, {
            [schema_1.OtpCodes.HASH]: (0, phone_otp_1.hashCode)(phone, code),
            [schema_1.OtpCodes.SEALED]: previous ? prev.get(schema_1.OtpCodes.SEALED) : (0, phone_otp_1.sealCode)(code),
            [schema_1.OtpCodes.ISSUED_AT]: previous ? issuedAt : now,
            [schema_1.OtpCodes.EXPIRES_AT]: previous ? (0, phone_otp_1.resentExpiry)(issuedAt, now) : now + phone_otp_1.CODE_TTL_MS,
            [schema_1.OtpCodes.ATTEMPTS]: previous ? Number(prev.get(schema_1.OtpCodes.ATTEMPTS) || 0) : 0,
            [schema_1.OtpCodes.CHANNEL]: "whatsapp",
            [schema_1.OtpCodes.LAST_SENT_AT]: d.next.lastSentAt,
            [schema_1.OtpCodes.HOUR_START]: d.next.hourStart,
            [schema_1.OtpCodes.HOUR_COUNT]: d.next.hourCount,
            [schema_1.OtpCodes.DAY_KEY]: d.next.dayKey,
            [schema_1.OtpCodes.DAY_COUNT]: d.next.dayCount,
            [schema_1.OtpCodes.EXPIRE_AT]: Timestamp.fromMillis(now + DAY_MS),
        });
        tx.set(dayRef, {
            [schema_1.OtpDaily.COUNT]: admin.firestore.FieldValue.increment(1),
            [schema_1.OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(now + 7 * DAY_MS),
        }, { merge: true });
        if (ipRef) {
            tx.set(ipRef, {
                [schema_1.OtpIp.COUNT]: admin.firestore.FieldValue.increment(1),
                [schema_1.OtpIp.EXPIRE_AT]: Timestamp.fromMillis(now + 2 * 60 * 60 * 1000),
            }, { merge: true });
        }
        return Object.assign(Object.assign({}, d), { reused: Boolean(previous) });
    });
    if (!decision.ok) {
        if (decision.reason === "cap") {
            (0, input_1.fail)("resource-exhausted", "Daily WhatsApp OTP limit reached. Please try again tomorrow or contact support.");
        }
        if (decision.reason === "wait") {
            return { sent: false, channel: "whatsapp", retryAfterSec: decision.retryAfterSec };
        }
        if (decision.reason === "ip") {
            (0, input_1.fail)("resource-exhausted", "Too many login requests from this network. Please try again in an hour.");
        }
        (0, input_1.fail)("resource-exhausted", "Too many codes for this number. Please try again later.");
    }
    const waOk = await sendOnWhatsapp(phone, code);
    if (waOk) {
        return { sent: true, channel: "whatsapp", expiresInSec: phone_otp_1.CODE_TTL_MS / 1000, sameCode: decision.reused };
    }
    // A fresh code nobody received is cancelled; a resent one may still be on the phone, so it stays.
    if (!decision.reused) {
        await codeRef.update({ [schema_1.OtpCodes.HASH]: "", [schema_1.OtpCodes.SEALED]: "" }).catch(() => undefined);
    }
    (0, input_1.fail)("unavailable", "Could not deliver WhatsApp OTP. Please ensure your mobile number is active on WhatsApp.");
});
exports.verifyWhatsappOtp = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 20 }, async (raw) => {
    const data = (0, input_1.obj)(raw);
    const phone = (0, phone_otp_1.indianE164)(data.phone);
    if (!phone)
        (0, input_1.fail)("invalid-argument", "Enter a valid 10-digit mobile number");
    const code = (0, input_1.str)(data, "code", { min: 6, max: 6, pattern: /^\d{6}$/ });
    const codeRef = db.collection(schema_1.OtpCodes.COLLECTION).doc(phone);
    const result = await db.runTransaction(async (tx) => {
        const doc = await tx.get(codeRef);
        const hash = String(doc.get(schema_1.OtpCodes.HASH) || "");
        if (!doc.exists || !hash || Number(doc.get(schema_1.OtpCodes.EXPIRES_AT) || 0) < Date.now())
            return "expired";
        const attempts = Number(doc.get(schema_1.OtpCodes.ATTEMPTS) || 0);
        if (attempts >= phone_otp_1.MAX_VERIFY_ATTEMPTS)
            return "locked";
        if (!(0, phone_otp_1.codeMatches)(phone, code, hash)) {
            tx.update(codeRef, { [schema_1.OtpCodes.ATTEMPTS]: attempts + 1 });
            return attempts + 1 >= phone_otp_1.MAX_VERIFY_ATTEMPTS ? "locked" : "wrong";
        }
        // One use only; keep the send counters for the rate limits.
        tx.update(codeRef, { [schema_1.OtpCodes.HASH]: "", [schema_1.OtpCodes.SEALED]: "", [schema_1.OtpCodes.ATTEMPTS]: 0 });
        return "ok";
    });
    if (result === "expired")
        (0, input_1.fail)("deadline-exceeded", "This code has expired. Please request a new one.");
    if (result === "locked")
        (0, input_1.fail)("resource-exhausted", "Too many wrong codes. Please request a new one.");
    if (result === "wrong")
        (0, input_1.fail)("invalid-argument", "The code is incorrect. Please check and try again.");
    const [uid, existingRole] = await Promise.all([
        uidForPhone(phone),
        registeredRole(phone)
    ]);
    const claims = Object.assign({ phone_number: phone }, (existingRole ? { role: existingRole } : {}));
    admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));
    const tokenPromise = admin.auth().createCustomToken(uid, claims);
    let name = "";
    let isProfileComplete = false;
    let photoUrl = "";
    if (existingRole) {
        const collection = existingRole === schema_1.Values.Role.EMPLOYER ? schema_1.EmployerProfiles.COLLECTION : schema_1.WorkerProfiles.COLLECTION;
        const profileSnap = await db.collection(collection).doc(uid).get().catch(() => null);
        if (profileSnap && profileSnap.exists) {
            const pData = profileSnap.data() || {};
            if (existingRole === schema_1.Values.Role.EMPLOYER) {
                name = String(pData[schema_1.EmployerProfiles.OWNER_NAME] || "");
                isProfileComplete = Boolean(pData[schema_1.EmployerProfiles.OWNER_NAME] && String(pData[schema_1.EmployerProfiles.OWNER_NAME]).trim().length >= 2);
                photoUrl = String(pData[schema_1.EmployerProfiles.PHOTO_URL] || "");
            }
            else {
                name = String(pData[schema_1.WorkerProfiles.NAME] || "");
                const skills = pData[schema_1.WorkerProfiles.SKILLS];
                isProfileComplete = Array.isArray(skills) && skills.length > 0;
                photoUrl = String(pData[schema_1.WorkerProfiles.PHOTO_URL] || "");
            }
        }
    }
    const token = await tokenPromise;
    return {
        token,
        role: existingRole || null,
        isNewUser: !existingRole,
        name,
        isProfileComplete,
        photoUrl
    };
});
// ─────────────────────────────── Truecaller ───────────────────────────────
async function truecallerProfile(authorizationCode, codeVerifier) {
    const clientId = process.env.TRUECALLER_CLIENT_ID || "";
    if (!clientId)
        (0, input_1.fail)("unavailable", "Truecaller login is not set up");
    const host = process.env.TRUECALLER_OAUTH_HOST || "https://oauth-account-noneu.truecaller.com";
    const tokenRes = await exports.net.fetch(`${host}/v1/token`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
            grant_type: "authorization_code",
            client_id: clientId,
            code: authorizationCode,
            code_verifier: codeVerifier,
        }).toString(),
    });
    if (!tokenRes.ok) {
        functions.logger.warn(`Truecaller token ${tokenRes.status}`, (await tokenRes.text()).slice(0, 300));
        (0, input_1.fail)("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
    }
    const accessToken = String((await tokenRes.json()).access_token || "");
    if (!accessToken)
        (0, input_1.fail)("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
    const infoRes = await exports.net.fetch(`${host}/v1/userinfo`, { headers: { Authorization: `Bearer ${accessToken}` } });
    if (!infoRes.ok) {
        functions.logger.warn(`Truecaller userinfo ${infoRes.status}`);
        (0, input_1.fail)("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
    }
    return (0, phone_otp_1.parseTruecallerUserInfo)(await infoRes.json());
}
exports.truecallerSignIn = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 20 }, async (raw) => {
    try {
        const data = (0, input_1.obj)(raw);
        const authorizationCode = (0, input_1.str)(data, "authorizationCode", { max: 2000 });
        const codeVerifier = (0, input_1.str)(data, "codeVerifier", { min: 43, max: 128 });
        const role = requestedRole(data);
        if (!role)
            (0, input_1.fail)("invalid-argument", "role is required");
        const tc = await truecallerProfile(authorizationCode, codeVerifier);
        if (!tc.phone)
            (0, input_1.fail)("failed-precondition", "DutyPe works with Indian mobile numbers only. Please use OTP.");
        const phone = tc.phone;
        const existingRole = await registeredRole(phone);
        const profile = {
            phone,
            name: tc.name,
            email: tc.email,
            photoUrl: tc.picture,
            gender: tc.gender,
            city: tc.city,
            existingRole,
        };
        // Prevent cross-role collisions (a number cannot be both a Worker and an Employer)
        if (existingRole && existingRole !== role) {
            return Object.assign(Object.assign({}, profile), { allowed: false, roleConflict: true });
        }
        const isNewUser = !existingRole;
        const uid = await uidForPhone(phone);
        // Save/update truecaller_profiles doc
        await db.collection(schema_1.TruecallerProfiles.COLLECTION).doc(uid).set({
            [schema_1.TruecallerProfiles.PHONE]: phone,
            [schema_1.TruecallerProfiles.NAME]: tc.name,
            [schema_1.TruecallerProfiles.EMAIL]: tc.email,
            [schema_1.TruecallerProfiles.PICTURE]: tc.picture,
            [schema_1.TruecallerProfiles.GENDER]: tc.gender,
            [schema_1.TruecallerProfiles.CITY]: tc.city,
            [schema_1.TruecallerProfiles.UPDATED_AT]: Timestamp.now(),
        }, { merge: true });
        let isProfileComplete = false;
        let finalName = tc.name;
        let finalPhotoUrl = tc.picture;
        if (existingRole) {
            const collection = existingRole === schema_1.Values.Role.EMPLOYER ? schema_1.EmployerProfiles.COLLECTION : schema_1.WorkerProfiles.COLLECTION;
            const profileSnap = await db.collection(collection).doc(uid).get().catch(() => null);
            if (profileSnap && profileSnap.exists) {
                const pData = profileSnap.data() || {};
                if (existingRole === schema_1.Values.Role.EMPLOYER) {
                    finalName = String(pData[schema_1.EmployerProfiles.OWNER_NAME] || tc.name);
                    isProfileComplete = Boolean(pData[schema_1.EmployerProfiles.OWNER_NAME] && String(pData[schema_1.EmployerProfiles.OWNER_NAME]).trim().length >= 2);
                    finalPhotoUrl = String(pData[schema_1.EmployerProfiles.PHOTO_URL] || tc.picture);
                    const updates = {};
                    if (!pData[schema_1.EmployerProfiles.EMAIL] && tc.email)
                        updates[schema_1.EmployerProfiles.EMAIL] = tc.email;
                    if (!pData[schema_1.EmployerProfiles.PHOTO_URL] && tc.picture)
                        updates[schema_1.EmployerProfiles.PHOTO_URL] = tc.picture;
                    if (Object.keys(updates).length > 0) {
                        await db.collection(collection).doc(uid).update(updates).catch(() => undefined);
                    }
                }
                else {
                    finalName = String(pData[schema_1.WorkerProfiles.NAME] || tc.name);
                    const skills = pData[schema_1.WorkerProfiles.SKILLS];
                    isProfileComplete = Array.isArray(skills) && skills.length > 0;
                    finalPhotoUrl = String(pData[schema_1.WorkerProfiles.PHOTO_URL] || tc.picture);
                    const updates = {};
                    if (!pData[schema_1.WorkerProfiles.EMAIL] && tc.email)
                        updates[schema_1.WorkerProfiles.EMAIL] = tc.email;
                    if (!pData[schema_1.WorkerProfiles.PHOTO_URL] && tc.picture)
                        updates[schema_1.WorkerProfiles.PHOTO_URL] = tc.picture;
                    if (Object.keys(updates).length > 0) {
                        await db.collection(collection).doc(uid).update(updates).catch(() => undefined);
                    }
                }
            }
        }
        // Ensure authoritative users/{uid} document has base information
        const userDocRef = db.collection("users").doc(uid);
        const userSnap = await userDocRef.get().catch(() => null);
        if (!userSnap || !userSnap.exists) {
            await userDocRef.set({
                uid,
                phoneNumber: phone,
                name: tc.name || "User",
                email: tc.email || null,
                profileImageUrl: tc.picture || null,
                role: existingRole || role,
                authMethod: "TRUECALLER",
                createdAt: Timestamp.now(),
                updatedAt: Timestamp.now(),
            }, { merge: true }).catch(() => undefined);
        }
        else {
            const uData = userSnap.data() || {};
            const userUpdates = { updatedAt: Timestamp.now() };
            if (!uData.profileImageUrl && tc.picture)
                userUpdates.profileImageUrl = tc.picture;
            if (!uData.email && tc.email)
                userUpdates.email = tc.email;
            if ((!uData.name || uData.name === "User") && tc.name)
                userUpdates.name = tc.name;
            if (Object.keys(userUpdates).length > 1) {
                await userDocRef.update(userUpdates).catch(() => undefined);
            }
        }
        const claims = Object.assign({ phone_number: phone }, (existingRole ? { role: existingRole } : {}));
        await admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));
        return Object.assign(Object.assign({}, profile), { allowed: true, isNewUser, role: existingRole || null, name: finalName, photoUrl: finalPhotoUrl, isProfileComplete, token: await admin.auth().createCustomToken(uid, claims) });
    }
    catch (e) {
        functions.logger.error("truecallerSignIn error:", e);
        if (e instanceof functions.https.HttpsError)
            throw e;
        throw new functions.https.HttpsError("internal", (e === null || e === void 0 ? void 0 : e.message) || "Truecaller sign-in failed");
    }
});
//# sourceMappingURL=phone-login.js.map