"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.truecallerSignIn = exports.verifyWhatsappOtp = exports.sendWhatsappOtp = exports.net = exports.WHATSAPP_SECRET = void 0;
exports.uidForPhone = uidForPhone;
/**
 * Cheaper ways to log in with a phone number than Firebase SMS (~₹6.7 per SMS):
 *
 *   sendWhatsappOtp({ phone, role, channel? }) sends a 6-digit code on WhatsApp (Meta Cloud API,
 *                                          authentication template), or by our SMS gateway when WhatsApp
 *                                          fails or channel = "sms" (~₹0.2). { sent: false } means "use
 *                                          Firebase SMS": nothing set up, send errors, or daily caps.
 *   verifyWhatsappOtp({ phone, code })     checks the code; returns a Firebase custom token for the
 *                                          phone's account (same uid as SMS login, created if new).
 *   truecallerSignIn({ authorizationCode, codeVerifier, role, mode })
 *                                          Truecaller one-tap: exchanges the code for the verified
 *                                          number, name and email. Returns a custom token only when
 *                                          the login / sign-up is allowed for that number and role.
 *
 * The app signs in with signInWithCustomToken(), so auth.currentUser.phoneNumber and the ID token's
 * phone_number are the same as after SMS login, and everything after login is unchanged.
 *
 * Settings: functions/.env.dutype-860ac (WHATSAPP_PHONE_NUMBER_ID, WHATSAPP_TEMPLATE,
 * WHATSAPP_TEMPLATE_LANG, TRUECALLER_CLIENT_ID); secret WHATSAPP_TOKEN (Meta permanent token; the
 * value "unset" keeps WhatsApp off). Custom tokens need the functions' service account to have the
 * "Service Account Token Creator" role.
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
/** Sends [code] through our SMS gateway. True when the gateway accepted it. */
async function sendOnSmsGateway(phone, code) {
    const { url, init } = (0, phone_otp_1.smsGatewayRequest)(phone, code);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 10000);
    try {
        const res = await exports.net.fetch(url, Object.assign(Object.assign({}, init), { signal: controller.signal }));
        const text = (await res.text()).slice(0, 300);
        // 2Factor: {"Status":"Success"}; MSG91: {"type":"success"}.
        const ok = res.ok && /"(Status|type)"\s*:\s*"success"/i.test(text);
        if (!ok)
            functions.logger.warn(`SMS gateway send failed ${res.status}`, text);
        return ok;
    }
    catch (e) {
        functions.logger.warn("SMS gateway error", e);
        return false;
    }
    finally {
        clearTimeout(timer);
    }
}
/**
 * Login code, cheapest channel first:
 *   1. WhatsApp (unless the app asks for SMS: "Didn't get it? Send by SMS"),
 *   2. our SMS gateway (same code, verified by verifyWhatsappOtp),
 *   3. { sent: false, channel: "sms" } → the app falls back to Firebase SMS.
 * Both server channels share one stored code and the per-number rate limits.
 */
exports.sendWhatsappOtp = (0, secure_callable_1.onCallSecured)({ requireAuth: false, enforceAppCheck: false, timeoutSeconds: 25, secrets: [exports.WHATSAPP_SECRET] }, async (raw, context) => {
    var _a;
    const data = (0, input_1.obj)(raw);
    const phone = (0, phone_otp_1.indianE164)(data.phone);
    if (!phone)
        (0, input_1.fail)("invalid-argument", "Enter a valid 10-digit mobile number");
    const ip = String(((_a = context === null || context === void 0 ? void 0 : context.rawRequest) === null || _a === void 0 ? void 0 : _a.ip) || "");
    const wantSms = data.channel === "sms";
    const useWhatsapp = !wantSms && (0, phone_otp_1.whatsappConfigured)();
    const useSms = (0, phone_otp_1.smsGatewayConfigured)();
    if (!useWhatsapp && !useSms)
        return { sent: false, channel: "sms" };
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
    const waCap = Number(process.env.WHATSAPP_DAILY_CAP || 3000);
    const smsCap = Number(process.env.SMS_DAILY_CAP || 2000);
    let code = (0, phone_otp_1.newCode)();
    const codeRef = db.collection(schema_1.OtpCodes.COLLECTION).doc(phone);
    const dayRef = db.collection(schema_1.OtpDaily.COLLECTION).doc((0, phone_otp_1.istDayKey)(now));
    const ipRef = ip ? db.collection(schema_1.OtpIp.COLLECTION).doc((0, phone_otp_1.ipKey)(ip, now)) : null;
    const decision = await db.runTransaction(async (tx) => {
        const [prev, day, ipDoc] = await Promise.all([tx.get(codeRef), tx.get(dayRef), ipRef ? tx.get(ipRef) : Promise.resolve(null)]);
        if (ipDoc && Number(ipDoc.get(schema_1.OtpIp.COUNT) || 0) >= phone_otp_1.MAX_PER_IP_HOUR) {
            return { ok: false, reason: "ip", retryAfterSec: 3600 };
        }
        const waLeft = useWhatsapp && Number(day.get(schema_1.OtpDaily.COUNT) || 0) < waCap;
        const smsLeft = useSms && Number(day.get(schema_1.OtpDaily.SMS_COUNT) || 0) < smsCap;
        if (!waLeft && !smsLeft)
            return { ok: false, reason: "cap", retryAfterSec: 0 };
        // From WhatsApp the app may ask for SMS right after: let that through the "wait" limit once.
        const d = (0, phone_otp_1.decideSend)(prev.exists ? {
            lastSentAt: wantSms && prev.get(schema_1.OtpCodes.CHANNEL) === "whatsapp" ? 0 : Number(prev.get(schema_1.OtpCodes.LAST_SENT_AT) || 0),
            hourStart: Number(prev.get(schema_1.OtpCodes.HOUR_START) || 0),
            hourCount: Number(prev.get(schema_1.OtpCodes.HOUR_COUNT) || 0),
            dayKey: String(prev.get(schema_1.OtpCodes.DAY_KEY) || ""),
            dayCount: Number(prev.get(schema_1.OtpCodes.DAY_COUNT) || 0),
        } : null, now);
        if (!d.ok)
            return d;
        const channel = waLeft ? "whatsapp" : "sms";
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
            [schema_1.OtpCodes.CHANNEL]: channel,
            [schema_1.OtpCodes.LAST_SENT_AT]: d.next.lastSentAt,
            [schema_1.OtpCodes.HOUR_START]: d.next.hourStart,
            [schema_1.OtpCodes.HOUR_COUNT]: d.next.hourCount,
            [schema_1.OtpCodes.DAY_KEY]: d.next.dayKey,
            [schema_1.OtpCodes.DAY_COUNT]: d.next.dayCount,
            [schema_1.OtpCodes.EXPIRE_AT]: Timestamp.fromMillis(now + DAY_MS),
        });
        tx.set(dayRef, {
            [channel === "whatsapp" ? schema_1.OtpDaily.COUNT : schema_1.OtpDaily.SMS_COUNT]: admin.firestore.FieldValue.increment(1),
            [schema_1.OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(now + 7 * DAY_MS),
        }, { merge: true });
        if (ipRef) {
            tx.set(ipRef, { [schema_1.OtpIp.COUNT]: admin.firestore.FieldValue.increment(1),
                [schema_1.OtpIp.EXPIRE_AT]: Timestamp.fromMillis(now + 2 * 60 * 60 * 1000) }, { merge: true });
        }
        return Object.assign(Object.assign({}, d), { channel, smsLeft, reused: Boolean(previous) });
    });
    if (!decision.ok) {
        // The caps only switch people to Firebase SMS; the per-number limits stop the request.
        if (decision.reason === "cap")
            return { sent: false, channel: "sms" };
        if (decision.reason === "wait") {
            return { sent: false, channel: wantSms ? "sms_gateway" : "whatsapp", retryAfterSec: decision.retryAfterSec };
        }
        if (decision.reason === "ip")
            (0, input_1.fail)("resource-exhausted", "Too many login requests from this network. Please try again in an hour.");
        (0, input_1.fail)("resource-exhausted", "Too many codes for this number. Please try again later.");
    }
    if (decision.channel === "whatsapp" && await sendOnWhatsapp(phone, code)) {
        return { sent: true, channel: "whatsapp", expiresInSec: phone_otp_1.CODE_TTL_MS / 1000, sameCode: decision.reused };
    }
    // WhatsApp failed (or SMS was asked for): same code by our SMS gateway.
    if ((decision.channel === "sms" || decision.smsLeft) && await sendOnSmsGateway(phone, code)) {
        if (decision.channel === "whatsapp") {
            await db.collection(schema_1.OtpDaily.COLLECTION).doc((0, phone_otp_1.istDayKey)(now))
                .set({ [schema_1.OtpDaily.SMS_COUNT]: admin.firestore.FieldValue.increment(1) }, { merge: true }).catch(() => undefined);
        }
        await codeRef.update({ [schema_1.OtpCodes.CHANNEL]: "sms" }).catch(() => undefined);
        return { sent: true, channel: "sms_gateway", expiresInSec: phone_otp_1.CODE_TTL_MS / 1000, sameCode: decision.reused };
    }
    // A fresh code nobody received is cancelled; a resent one may still be on the phone, so it stays.
    if (!decision.reused)
        await codeRef.update({ [schema_1.OtpCodes.HASH]: "", [schema_1.OtpCodes.SEALED]: "" }).catch(() => undefined);
    return { sent: false, channel: "sms" };
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
    const uid = await uidForPhone(phone);
    const existingRole = await registeredRole(phone);
    const claims = Object.assign({ phone_number: phone }, (existingRole ? { role: existingRole } : {}));
    await admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));
    return { token: await admin.auth().createCustomToken(uid, claims) };
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
        const mode = data.mode === "register" ? "register" : data.mode === "login" ? "login" : "unified";
        const tc = await truecallerProfile(authorizationCode, codeVerifier);
        if (!tc.phone)
            (0, input_1.fail)("failed-precondition", "DutyPe works with Indian mobile numbers only. Please use OTP.");
        const phone = tc.phone;
        const existingRole = await registeredRole(phone);
        const profile = { phone, name: tc.name, email: tc.email, existingRole };
        // Prevent cross-role collisions (a number cannot be both a Worker and an Employer)
        if (existingRole && existingRole !== role)
            return Object.assign(Object.assign({}, profile), { allowed: false, roleConflict: true });
        // Legacy strict mode checks (if explicitly requested)
        if (mode === "login" && !existingRole)
            return Object.assign(Object.assign({}, profile), { allowed: false });
        if (mode === "register" && existingRole)
            return Object.assign(Object.assign({}, profile), { allowed: false });
        const isNewUser = !existingRole;
        const uid = await uidForPhone(phone);
        await db.collection(schema_1.TruecallerProfiles.COLLECTION).doc(uid).set({
            [schema_1.TruecallerProfiles.PHONE]: phone,
            [schema_1.TruecallerProfiles.NAME]: tc.name,
            [schema_1.TruecallerProfiles.EMAIL]: tc.email,
            [schema_1.TruecallerProfiles.UPDATED_AT]: Timestamp.now(),
        });
        // Existing accounts: keep the email Truecaller shared if the profile has none yet.
        if (existingRole && tc.email) {
            const ref = db.collection(existingRole === schema_1.Values.Role.EMPLOYER ? schema_1.EmployerProfiles.COLLECTION : schema_1.WorkerProfiles.COLLECTION).doc(uid);
            await db.runTransaction(async (tx) => {
                const doc = await tx.get(ref);
                if (doc.exists && !doc.get(schema_1.WorkerProfiles.EMAIL))
                    tx.update(ref, { [schema_1.WorkerProfiles.EMAIL]: tc.email });
            });
        }
        const claims = Object.assign({ phone_number: phone }, (existingRole ? { role: existingRole } : {}));
        await admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));
        return Object.assign(Object.assign({}, profile), { allowed: true, isNewUser, token: await admin.auth().createCustomToken(uid, claims) });
    }
    catch (e) {
        functions.logger.error("truecallerSignIn error:", e);
        if (e instanceof functions.https.HttpsError)
            throw e;
        throw new functions.https.HttpsError("internal", (e === null || e === void 0 ? void 0 : e.message) || "Truecaller sign-in failed");
    }
});
//# sourceMappingURL=phone-login.js.map