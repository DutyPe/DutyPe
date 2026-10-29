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
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { fail, obj, str } from "./lib/input";
import {
  CODE_TTL_MS, MAX_PER_IP_HOUR, MAX_VERIFY_ATTEMPTS, codeMatches, decideSend, hashCode, indianE164, ipKey, istDayKey,
  newCode, openCode, parseTruecallerUserInfo, resentExpiry, reuseCode, sealCode, whatsappConfigured, whatsappTemplateBody,
} from "./lib/phone-otp";
import { EmployerProfiles, OtpCodes, OtpDaily, OtpIp, PhoneRoles, TruecallerProfiles, Values, WorkerProfiles } from "./schema";

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

/**
 * WhatsApp login code (Meta Cloud API, authentication template).
 * Returns { sent: true, channel: "whatsapp" } on success.
 * Enforces per-number rate limits, IP rate limits, and daily cap (WHATSAPP_DAILY_CAP = 5000).
 */
export const sendWhatsappOtp = onCallSecured(
  { requireAuth: false, enforceAppCheck: false, timeoutSeconds: 25, secrets: [WHATSAPP_SECRET] },
  async (raw: unknown, context) => {
    const data = obj(raw);
    const phone = indianE164(data.phone);
    if (!phone) fail("invalid-argument", "Enter a valid 10-digit mobile number");
    const ip = String((context as { rawRequest?: { ip?: string } } | undefined)?.rawRequest?.ip || "");
    if (!whatsappConfigured()) {
      fail("unavailable", "WhatsApp OTP service is currently not configured.");
    }

    // Never spend on a number registered with the other role (the app checks too).
    const role = requestedRole(data);
    const existing = await registeredRole(phone);
    if (role && existing && existing !== role) fail("failed-precondition", `phone-already-registered-as:${existing}`);
    const mode = String(data.mode || "");
    if (mode === "login" && !existing) fail("not-found", "No account found with this number. Please register first.");
    if (mode === "register" && existing) fail("already-exists", "An account already exists with this number. Please log in.");

    const now = Date.now();
    const waCap = Number(process.env.WHATSAPP_DAILY_CAP || 5000);
    let code = newCode();
    const codeRef = db.collection(OtpCodes.COLLECTION).doc(phone);
    const dayRef = db.collection(OtpDaily.COLLECTION).doc(istDayKey(now));
    const ipRef = ip ? db.collection(OtpIp.COLLECTION).doc(ipKey(ip, now)) : null;

    const decision = await db.runTransaction(async (tx) => {
      const [prev, day, ipDoc] = await Promise.all([
        tx.get(codeRef),
        tx.get(dayRef),
        ipRef ? tx.get(ipRef) : Promise.resolve(null),
      ]);
      if (ipDoc && Number(ipDoc.get(OtpIp.COUNT) || 0) >= MAX_PER_IP_HOUR) {
        return { ok: false as const, reason: "ip" as const, retryAfterSec: 3600 };
      }
      const waCount = Number(day.get(OtpDaily.COUNT) || 0);
      if (waCount >= waCap) {
        return { ok: false as const, reason: "cap" as const, retryAfterSec: 0 };
      }
      const d = decideSend(prev.exists ? {
        lastSentAt: Number(prev.get(OtpCodes.LAST_SENT_AT) || 0),
        hourStart: Number(prev.get(OtpCodes.HOUR_START) || 0),
        hourCount: Number(prev.get(OtpCodes.HOUR_COUNT) || 0),
        dayKey: String(prev.get(OtpCodes.DAY_KEY) || ""),
        dayCount: Number(prev.get(OtpCodes.DAY_COUNT) || 0),
      } : null, now);
      if (!d.ok) return d;

      // Resend within the window: the same digits, 10 more minutes (max 30 from the first send).
      const issuedAt = Number(prev.get(OtpCodes.ISSUED_AT) || 0);
      const previous = prev.exists && reuseCode({
        hasCode: Boolean(prev.get(OtpCodes.HASH)) && Boolean(prev.get(OtpCodes.SEALED)),
        expiresAt: Number(prev.get(OtpCodes.EXPIRES_AT) || 0),
        issuedAt,
        attempts: Number(prev.get(OtpCodes.ATTEMPTS) || 0),
      }, now) ? openCode(String(prev.get(OtpCodes.SEALED))) : null;
      if (previous) code = previous;

      tx.set(codeRef, {
        [OtpCodes.HASH]: hashCode(phone, code),
        [OtpCodes.SEALED]: previous ? prev.get(OtpCodes.SEALED) : sealCode(code),
        [OtpCodes.ISSUED_AT]: previous ? issuedAt : now,
        [OtpCodes.EXPIRES_AT]: previous ? resentExpiry(issuedAt, now) : now + CODE_TTL_MS,
        [OtpCodes.ATTEMPTS]: previous ? Number(prev.get(OtpCodes.ATTEMPTS) || 0) : 0,
        [OtpCodes.CHANNEL]: "whatsapp",
        [OtpCodes.LAST_SENT_AT]: d.next.lastSentAt,
        [OtpCodes.HOUR_START]: d.next.hourStart,
        [OtpCodes.HOUR_COUNT]: d.next.hourCount,
        [OtpCodes.DAY_KEY]: d.next.dayKey,
        [OtpCodes.DAY_COUNT]: d.next.dayCount,
        [OtpCodes.EXPIRE_AT]: Timestamp.fromMillis(now + DAY_MS),
      });
      tx.set(dayRef, {
        [OtpDaily.COUNT]: admin.firestore.FieldValue.increment(1),
        [OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(now + 7 * DAY_MS),
      }, { merge: true });
      if (ipRef) {
        tx.set(ipRef, {
          [OtpIp.COUNT]: admin.firestore.FieldValue.increment(1),
          [OtpIp.EXPIRE_AT]: Timestamp.fromMillis(now + 2 * 60 * 60 * 1000),
        }, { merge: true });
      }
      return { ...d, reused: Boolean(previous) };
    });

    if (!decision.ok) {
      if (decision.reason === "cap") {
        fail("resource-exhausted", "Daily WhatsApp OTP limit reached. Please try again tomorrow or contact support.");
      }
      if (decision.reason === "wait") {
        return { sent: false, channel: "whatsapp", retryAfterSec: decision.retryAfterSec };
      }
      if (decision.reason === "ip") {
        fail("resource-exhausted", "Too many login requests from this network. Please try again in an hour.");
      }
      fail("resource-exhausted", "Too many codes for this number. Please try again later.");
    }

    const waOk = await sendOnWhatsapp(phone, code);
    if (waOk) {
      return { sent: true, channel: "whatsapp", expiresInSec: CODE_TTL_MS / 1000, sameCode: decision.reused };
    }

    // A fresh code nobody received is cancelled; a resent one may still be on the phone, so it stays.
    if (!decision.reused) {
      await codeRef.update({ [OtpCodes.HASH]: "", [OtpCodes.SEALED]: "" }).catch(() => undefined);
    }
    fail("unavailable", "Could not deliver WhatsApp OTP. Please ensure your mobile number is active on WhatsApp.");
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
      tx.update(codeRef, { [OtpCodes.HASH]: "", [OtpCodes.SEALED]: "", [OtpCodes.ATTEMPTS]: 0 });
      return "ok";
    });
    if (result === "expired") fail("deadline-exceeded", "This code has expired. Please request a new one.");
    if (result === "locked") fail("resource-exhausted", "Too many wrong codes. Please request a new one.");
    if (result === "wrong") fail("invalid-argument", "The code is incorrect. Please check and try again.");

    const [uid, existingRole] = await Promise.all([
      uidForPhone(phone),
      registeredRole(phone)
    ]);
    const claims: Record<string, unknown> = {
      phone_number: phone,
      ...(existingRole ? { role: existingRole } : {}),
    };
    admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));

    const tokenPromise = admin.auth().createCustomToken(uid, claims);
    let name = "";
    let isProfileComplete = false;
    let photoUrl = "";
    if (existingRole) {
      const collection = existingRole === Values.Role.EMPLOYER ? EmployerProfiles.COLLECTION : WorkerProfiles.COLLECTION;
      const profileSnap = await db.collection(collection).doc(uid).get().catch(() => null);
      if (profileSnap && profileSnap.exists) {
        const pData = profileSnap.data() || {};
        if (existingRole === Values.Role.EMPLOYER) {
          name = String(pData[EmployerProfiles.OWNER_NAME] || "");
          isProfileComplete = Boolean(pData[EmployerProfiles.OWNER_NAME] && String(pData[EmployerProfiles.OWNER_NAME]).trim().length >= 2);
          photoUrl = String(pData[EmployerProfiles.PHOTO_URL] || "");
        } else {
          name = String(pData[WorkerProfiles.NAME] || "");
          const skills = pData[WorkerProfiles.SKILLS];
          isProfileComplete = Array.isArray(skills) && skills.length > 0;
          photoUrl = String(pData[WorkerProfiles.PHOTO_URL] || "");
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
  },
);

// ─────────────────────────────── Truecaller ───────────────────────────────

async function truecallerProfile(authorizationCode: string, codeVerifier: string) {
  const clientId = process.env.TRUECALLER_CLIENT_ID || "-kqtniuuyzghbxcn_q52rjxhipc_8tuttzryunaupwu";
  if (!clientId) fail("unavailable", "Truecaller login is not set up");
  const host = process.env.TRUECALLER_OAUTH_HOST || "https://oauth-account-noneu.truecaller.com";
  let tokenRes: Response;
  try {
    tokenRes = await net.fetch(`${host}/v1/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        client_id: clientId,
        code: authorizationCode,
        code_verifier: codeVerifier,
      }).toString(),
    });
  } catch (err: unknown) {
    functions.logger.error("Truecaller token fetch network error:", err);
    fail("unavailable", "Unable to contact Truecaller servers. Please try again or use OTP.");
  }
  if (!tokenRes.ok) {
    const errText = await tokenRes.text().catch(() => "");
    functions.logger.warn(`Truecaller token ${tokenRes.status}: ${errText.slice(0, 300)}`);
    fail("unauthenticated", "Truecaller verification failed or expired. Please try again or use OTP.");
  }
  const tokenJson = (await tokenRes.json().catch(() => ({}))) as { access_token?: string };
  const accessToken = String(tokenJson.access_token || "");
  if (!accessToken) fail("unauthenticated", "Truecaller verification failed. Please try again or use OTP.");
  
  let infoRes: Response;
  try {
    infoRes = await net.fetch(`${host}/v1/userinfo`, { headers: { Authorization: `Bearer ${accessToken}` } });
  } catch (err: unknown) {
    functions.logger.error("Truecaller userinfo fetch network error:", err);
    fail("unavailable", "Unable to contact Truecaller servers. Please try again or use OTP.");
  }
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

      const tc = await truecallerProfile(authorizationCode, codeVerifier);
      if (!tc.phone) fail("failed-precondition", "DutyPe works with Indian mobile numbers only. Please use OTP.");
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
        return { ...profile, allowed: false, roleConflict: true };
      }

      const isNewUser = !existingRole;
      const uid = await uidForPhone(phone);

      // Save/update truecaller_profiles doc
      await db.collection(TruecallerProfiles.COLLECTION).doc(uid).set({
        [TruecallerProfiles.PHONE]: phone,
        [TruecallerProfiles.NAME]: tc.name,
        [TruecallerProfiles.EMAIL]: tc.email,
        [TruecallerProfiles.PICTURE]: tc.picture,
        [TruecallerProfiles.GENDER]: tc.gender,
        [TruecallerProfiles.CITY]: tc.city,
        [TruecallerProfiles.UPDATED_AT]: Timestamp.now(),
      }, { merge: true });

      let isProfileComplete = false;
      let finalName = tc.name;
      let finalPhotoUrl = tc.picture;

      if (existingRole) {
        const collection = existingRole === Values.Role.EMPLOYER ? EmployerProfiles.COLLECTION : WorkerProfiles.COLLECTION;
        const profileSnap = await db.collection(collection).doc(uid).get().catch(() => null);
        if (profileSnap && profileSnap.exists) {
          const pData = profileSnap.data() || {};
          if (existingRole === Values.Role.EMPLOYER) {
            finalName = String(pData[EmployerProfiles.OWNER_NAME] || tc.name);
            isProfileComplete = Boolean(pData[EmployerProfiles.OWNER_NAME] && String(pData[EmployerProfiles.OWNER_NAME]).trim().length >= 2);
            finalPhotoUrl = String(pData[EmployerProfiles.PHOTO_URL] || tc.picture);
            const updates: Record<string, unknown> = {};
            if (!pData[EmployerProfiles.EMAIL] && tc.email) updates[EmployerProfiles.EMAIL] = tc.email;
            if (!pData[EmployerProfiles.PHOTO_URL] && tc.picture) updates[EmployerProfiles.PHOTO_URL] = tc.picture;
            if ((!pData[EmployerProfiles.OWNER_NAME] || pData[EmployerProfiles.OWNER_NAME] === "User") && tc.name) updates[EmployerProfiles.OWNER_NAME] = tc.name;
            if ((!pData[EmployerProfiles.BUSINESS_NAME] || pData[EmployerProfiles.BUSINESS_NAME] === "Employer") && tc.name) updates[EmployerProfiles.BUSINESS_NAME] = tc.name;
            if (!pData[EmployerProfiles.AREA] && tc.city) updates[EmployerProfiles.AREA] = tc.city;
            if (Object.keys(updates).length > 0) {
              await db.collection(collection).doc(uid).update(updates).catch(() => undefined);
            }
          } else {
            finalName = String(pData[WorkerProfiles.NAME] || tc.name);
            const skills = pData[WorkerProfiles.SKILLS];
            isProfileComplete = Array.isArray(skills) && skills.length > 0;
            finalPhotoUrl = String(pData[WorkerProfiles.PHOTO_URL] || tc.picture);
            const updates: Record<string, unknown> = {};
            if (!pData[WorkerProfiles.EMAIL] && tc.email) updates[WorkerProfiles.EMAIL] = tc.email;
            if (!pData[WorkerProfiles.PHOTO_URL] && tc.picture) updates[WorkerProfiles.PHOTO_URL] = tc.picture;
            if ((!pData[WorkerProfiles.NAME] || pData[WorkerProfiles.NAME] === "User") && tc.name) updates[WorkerProfiles.NAME] = tc.name;
            if (!pData[WorkerProfiles.AREA] && tc.city) updates[WorkerProfiles.AREA] = tc.city;
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
          fullName: tc.name || "User",
          email: tc.email || null,
          profileImageUrl: tc.picture || null,
          photoUrl: tc.picture || null,
          city: tc.city || null,
          area: tc.city || null,
          role: existingRole || role,
          authMethod: "TRUECALLER",
          createdAt: Timestamp.now(),
          updatedAt: Timestamp.now(),
        }, { merge: true }).catch(() => undefined);
      } else {
        const uData = userSnap.data() || {};
        const userUpdates: Record<string, unknown> = { updatedAt: Timestamp.now() };
        if (!uData.profileImageUrl && tc.picture) userUpdates.profileImageUrl = tc.picture;
        if (!uData.photoUrl && tc.picture) userUpdates.photoUrl = tc.picture;
        if (!uData.city && tc.city) { userUpdates.city = tc.city; userUpdates.area = tc.city; }
        if (!uData.email && tc.email) userUpdates.email = tc.email;
        if ((!uData.name || uData.name === "User") && tc.name) {
          userUpdates.name = tc.name;
          userUpdates.fullName = tc.name;
        }
        if (Object.keys(userUpdates).length > 1) {
          await userDocRef.update(userUpdates).catch(() => undefined);
        }
      }

      const claims: Record<string, unknown> = {
        phone_number: phone,
        ...(existingRole ? { role: existingRole } : {}),
      };
      await admin.auth().setCustomUserClaims(uid, claims).catch((e) => functions.logger.warn("setCustomUserClaims failed", e));

      return {
        ...profile,
        allowed: true,
        isNewUser,
        role: existingRole || null,
        name: finalName,
        photoUrl: finalPhotoUrl,
        isProfileComplete,
        token: await admin.auth().createCustomToken(uid, claims),
      };
    } catch (e: unknown) {
      functions.logger.error("truecallerSignIn error:", e);
      if (e instanceof functions.https.HttpsError) throw e;
      const err = e as { code?: string; message?: string };
      if (err && typeof err.code === "string" && typeof err.message === "string") {
        throw new functions.https.HttpsError(err.code as functions.https.FunctionsErrorCode, err.message);
      }
      throw new functions.https.HttpsError("internal", (e as Error)?.message || "Truecaller verification failed. Please try OTP.");
    }
  },
);

