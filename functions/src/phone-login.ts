/**
 * Cheaper ways to log in with a phone number than Firebase SMS (~₹6.7 per SMS):
 *
 *   sendWhatsappOtp({ phone, role })       sends a 6-digit code on WhatsApp (Meta Cloud API,
 *                                          authentication template). { sent: false } means "use SMS":
 *                                          WhatsApp not set up, a send error, or the daily cap reached.
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
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import {
  CODE_TTL_MS, MAX_VERIFY_ATTEMPTS, codeMatches, decideSend, hashCode, indianE164, istDayKey, newCode,
  parseTruecallerUserInfo, whatsappConfigured, whatsappTemplateBody,
} from "./lib/phone-otp";
import { EmployerProfiles, OtpCodes, OtpDaily, PhoneRoles, TruecallerProfiles, Values, WorkerProfiles } from "./schema";

const db = admin.firestore();
const { Timestamp } = admin.firestore;
export const WHATSAPP_SECRET = "WHATSAPP_TOKEN";
const DAY_MS = 24 * 60 * 60 * 1000;
const ROLES: string[] = [Values.Role.WORKER, Values.Role.EMPLOYER];

/** Test seam: replaced in tests so no network call is made. */
export const net = {
  fetch: (url: string, init?: RequestInit): Promise<Response> => fetch(url, init),
};

function requestedRole(data: Record<string, unknown>): string {
  const role = String(data.role || "").toUpperCase();
  return ROLES.includes(role) ? role : "";
}

async function registeredRole(phone: string): Promise<string | null> {
  const doc = await db.collection(PhoneRoles.COLLECTION).doc(phone).get();
  return doc.exists ? String(doc.get(PhoneRoles.ROLE) || "") || null : null;
}

/** The Firebase Auth account for this number: the one SMS login uses, or a new one. */
export async function uidForPhone(phone: string): Promise<string> {
  const find = async () => {
    try {
      return await admin.auth().getUserByPhoneNumber(phone);
    } catch (e) {
      if ((e as { code?: string }).code === "auth/user-not-found") return null;
      throw e;
    }
  };
  let user = await find();
  if (!user) {
    try {
      user = await admin.auth().createUser({ phoneNumber: phone });
    } catch (e) {
      // Created at the same moment by another request.
      if ((e as { code?: string }).code !== "auth/phone-number-already-exists") throw e;
      user = await find();
    }
  }
  if (!user) fail("internal", "Could not sign in");
  if (user.disabled) fail("permission-denied", "This account is blocked. Contact DutyPe support.");
  return user.uid;
}

// ─────────────────────────────── WhatsApp ───────────────────────────────

async function sendOnWhatsapp(phone: string, code: string): Promise<boolean> {
  const version = process.env.WHATSAPP_API_VERSION || "v21.0";
  const url = `https://graph.facebook.com/${version}/${process.env.WHATSAPP_PHONE_NUMBER_ID}/messages`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10_000);
  try {
    const res = await net.fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${(process.env.WHATSAPP_TOKEN || "").trim()}` },
      body: JSON.stringify(whatsappTemplateBody(phone, code, process.env.WHATSAPP_TEMPLATE || "",
        process.env.WHATSAPP_TEMPLATE_LANG || "en")),
      signal: controller.signal,
    });
    if (!res.ok) {
      functions.logger.warn(`WhatsApp send failed ${res.status}`, (await res.text()).slice(0, 300));
      return false;
    }
    return true;
  } catch (e) {
    functions.logger.warn("WhatsApp send error", e);
    return false;
  } finally {
    clearTimeout(timer);
  }
}

export const sendWhatsappOtp = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 20, secrets: [WHATSAPP_SECRET] },
  async (raw: unknown) => {
    const data = obj(raw);
    const phone = indianE164(data.phone);
    if (!phone) fail("invalid-argument", "Enter a valid 10-digit mobile number");
    if (!whatsappConfigured()) return { sent: false, channel: "sms" };

    // Never spend on a number registered with the other role (the app checks too).
    const role = requestedRole(data);
    const existing = await registeredRole(phone);
    if (role && existing && existing !== role) fail("failed-precondition", `phone-already-registered-as:${existing}`);

    const now = Date.now();
    const cap = Number(process.env.WHATSAPP_DAILY_CAP || 3000);
    const code = newCode();
    const codeRef = db.collection(OtpCodes.COLLECTION).doc(phone);
    const dayRef = db.collection(OtpDaily.COLLECTION).doc(istDayKey(now));
    const decision = await db.runTransaction(async (tx) => {
      const [prev, day] = await Promise.all([tx.get(codeRef), tx.get(dayRef)]);
      if (Number(day.get(OtpDaily.COUNT) || 0) >= cap) return { ok: false as const, reason: "cap" as const, retryAfterSec: 0 };
      const d = decideSend(prev.exists ? {
        lastSentAt: Number(prev.get(OtpCodes.LAST_SENT_AT) || 0),
        hourStart: Number(prev.get(OtpCodes.HOUR_START) || 0),
        hourCount: Number(prev.get(OtpCodes.HOUR_COUNT) || 0),
        dayKey: String(prev.get(OtpCodes.DAY_KEY) || ""),
        dayCount: Number(prev.get(OtpCodes.DAY_COUNT) || 0),
      } : null, now);
      if (!d.ok) return d;
      tx.set(codeRef, {
        [OtpCodes.HASH]: hashCode(phone, code),
        [OtpCodes.EXPIRES_AT]: now + CODE_TTL_MS,
        [OtpCodes.ATTEMPTS]: 0,
        [OtpCodes.LAST_SENT_AT]: d.next.lastSentAt,
        [OtpCodes.HOUR_START]: d.next.hourStart,
        [OtpCodes.HOUR_COUNT]: d.next.hourCount,
        [OtpCodes.DAY_KEY]: d.next.dayKey,
        [OtpCodes.DAY_COUNT]: d.next.dayCount,
        [OtpCodes.EXPIRE_AT]: Timestamp.fromMillis(now + DAY_MS),
      });
      tx.set(dayRef, { [OtpDaily.COUNT]: admin.firestore.FieldValue.increment(1),
        [OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(now + 7 * DAY_MS) }, { merge: true });
      return d;
    });

    if (!decision.ok) {
      // The cap only switches people to SMS; the per-number limits stop the request.
      if (decision.reason === "cap") return { sent: false, channel: "sms" };
      if (decision.reason === "wait") return { sent: false, channel: "whatsapp", retryAfterSec: decision.retryAfterSec };
      fail("resource-exhausted", "Too many codes for this number. Please try again later.");
    }
    if (!(await sendOnWhatsapp(phone, code))) {
      await codeRef.update({ [OtpCodes.HASH]: "" }).catch(() => undefined);
      return { sent: false, channel: "sms" };
    }
    return { sent: true, channel: "whatsapp", expiresInSec: CODE_TTL_MS / 1000 };
  },
);

export const verifyWhatsappOtp = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 20 },
  async (raw: unknown) => {
    const data = obj(raw);
    const phone = indianE164(data.phone);
    if (!phone) fail("invalid-argument", "Enter a valid 10-digit mobile number");
    const code = str(data, "code", { min: 6, max: 6, pattern: /^\d{6}$/ });
    const codeRef = db.collection(OtpCodes.COLLECTION).doc(phone);

    const result = await db.runTransaction(async (tx) => {
      const doc = await tx.get(codeRef);
      const hash = String(doc.get(OtpCodes.HASH) || "");
      if (!doc.exists || !hash || Number(doc.get(OtpCodes.EXPIRES_AT) || 0) < Date.now()) return "expired";
      const attempts = Number(doc.get(OtpCodes.ATTEMPTS) || 0);
      if (attempts >= MAX_VERIFY_ATTEMPTS) return "locked";
      if (!codeMatches(phone, code, hash)) {
        tx.update(codeRef, { [OtpCodes.ATTEMPTS]: attempts + 1 });
        return attempts + 1 >= MAX_VERIFY_ATTEMPTS ? "locked" : "wrong";
      }
      // One use only; keep the send counters for the rate limits.
      tx.update(codeRef, { [OtpCodes.HASH]: "", [OtpCodes.ATTEMPTS]: 0 });
      return "ok";
    });
    if (result === "expired") fail("deadline-exceeded", "This code has expired. Please request a new one.");
    if (result === "locked") fail("resource-exhausted", "Too many wrong codes. Please request a new one.");
    if (result === "wrong") fail("invalid-argument", "The code is incorrect. Please check and try again.");

    const uid = await uidForPhone(phone);
    return { token: await admin.auth().createCustomToken(uid) };
  },
);

// ─────────────────────────────── Truecaller ───────────────────────────────

async function truecallerProfile(authorizationCode: string, codeVerifier: string) {
  const clientId = process.env.TRUECALLER_CLIENT_ID || "";
  if (!clientId) fail("unavailable", "Truecaller login is not set up");
  const host = process.env.TRUECALLER_OAUTH_HOST || "https://oauth-account-noneu.truecaller.com";
  const tokenRes = await net.fetch(`${host}/v1/token`, {
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
    fail("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
  }
  const accessToken = String(((await tokenRes.json()) as { access_token?: string }).access_token || "");
  if (!accessToken) fail("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
  const infoRes = await net.fetch(`${host}/v1/userinfo`, { headers: { Authorization: `Bearer ${accessToken}` } });
  if (!infoRes.ok) {
    functions.logger.warn(`Truecaller userinfo ${infoRes.status}`);
    fail("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
  }
  return parseTruecallerUserInfo(await infoRes.json());
}

export const truecallerSignIn = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 20 },
  async (raw: unknown) => {
    try {
      const data = obj(raw);
      const authorizationCode = str(data, "authorizationCode", { max: 2000 });
      const codeVerifier = str(data, "codeVerifier", { min: 43, max: 128 });
      const role = requestedRole(data);
      if (!role) fail("invalid-argument", "role is required");
      const mode = data.mode === "register" ? "register" : data.mode === "login" ? "login" : "unified";

      const tc = await truecallerProfile(authorizationCode, codeVerifier);
      if (!tc.phone) fail("failed-precondition", "DutyPe works with Indian mobile numbers only. Please use OTP.");
      const phone = tc.phone;
      const existingRole = await registeredRole(phone);
      const profile = { phone, name: tc.name, email: tc.email, existingRole };

      // Prevent cross-role collisions (a number cannot be both a Worker and an Employer)
      if (existingRole && existingRole !== role) return { ...profile, allowed: false, roleConflict: true };

      // Legacy strict mode checks (if explicitly requested)
      if (mode === "login" && !existingRole) return { ...profile, allowed: false };
      if (mode === "register" && existingRole) return { ...profile, allowed: false };

      const isNewUser = !existingRole;
      const uid = await uidForPhone(phone);
      await db.collection(TruecallerProfiles.COLLECTION).doc(uid).set({
        [TruecallerProfiles.PHONE]: phone,
        [TruecallerProfiles.NAME]: tc.name,
        [TruecallerProfiles.EMAIL]: tc.email,
        [TruecallerProfiles.UPDATED_AT]: Timestamp.now(),
      });
      // Existing accounts: keep the email Truecaller shared if the profile has none yet.
      if (existingRole && tc.email) {
        const ref = db.collection(existingRole === Values.Role.EMPLOYER ? EmployerProfiles.COLLECTION : WorkerProfiles.COLLECTION).doc(uid);
        await db.runTransaction(async (tx) => {
          const doc = await tx.get(ref);
          if (doc.exists && !doc.get(WorkerProfiles.EMAIL)) tx.update(ref, { [WorkerProfiles.EMAIL]: tc.email });
        });
      }
      return { ...profile, allowed: true, isNewUser, token: await admin.auth().createCustomToken(uid) };
    } catch (e) {
      functions.logger.error("truecallerSignIn error:", e);
      if (e instanceof functions.https.HttpsError) throw e;
      throw new functions.https.HttpsError("internal", (e as Error)?.message || "Truecaller sign-in failed");
    }
  },
);
