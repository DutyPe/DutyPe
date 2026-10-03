/**
 * WhatsApp beyond login (see lib/whatsapp.ts for the rules):
 *
 *   whatsappPromos({ enabled? })   the Settings switch "WhatsApp offers": reads it, or turns it on/off.
 *   sendWhatsappPromo({ ... })     admin: one approved MARKETING template to everyone who switched
 *                                  offers on (optionally only workers / employers / partners); dryRun
 *                                  counts first. At most one offer per person every 7 days.
 *   whatsappServiceAssigned(b)     called by acceptServiceBooking: UTILITY messages to the customer
 *                                  and the partner. Never used for urgent work or vacancies.
 *   stopWhatsappPromos(phones)     called by the webhook when someone replies STOP.
 */
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { onCallSecured } from "./secure-callable";
import { isCallerAdmin } from "./app-config";
import { fail, obj, str } from "./lib/input";
import { istDayKey } from "./lib/phone-otp";
import {
  PROMO_GAP_DAYS_DEFAULT, PROMO_MAX_PER_RUN, WHATSAPP_TOKEN_SECRET, langList, promoDue, sendWaTemplate, serviceTemplates, waLang, waReady,
} from "./lib/whatsapp";
import {
  EmployerProfiles, OtpDaily, ServiceBookings as BK, ServicePartners, UserTokens, Values, WhatsappCampaigns as WC,
  WhatsappPrefs as WP, WorkerProfiles,
} from "./schema";

const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
const AUDIENCES = ["ALL", Values.Role.WORKER, Values.Role.EMPLOYER, "PARTNER"];

const ms = (v: unknown) => (v as admin.firestore.Timestamp | undefined)?.toMillis?.() ?? 0;

async function languageOf(uid: string): Promise<string> {
  const t = await db.collection(UserTokens.COLLECTION).doc(uid).get();
  return String(t.get(UserTokens.LANGUAGE) || "en").slice(0, 2).toLowerCase();
}

/** Reserves [n] messages of today's [field] budget; false when that would pass [cap]. */
async function reserveDaily(field: string, n: number, cap: number): Promise<boolean> {
  const ref = db.collection(OtpDaily.COLLECTION).doc(istDayKey(Date.now()));
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const used = Number(snap.get(field) || 0);
    if (used + n > cap) return false;
    tx.set(ref, { [field]: used + n, [OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 3 * DAY_MS) }, { merge: true });
    return true;
  });
}

// ─────────────────────────── service updates (UTILITY) ───────────────────────────

/**
 * "Partner found" to the customer ({{1}} partner, {{2}} service, {{3}} partner phone) and "Job
 * confirmed" to the partner ({{1}} service, {{2}} customer, {{3}} customer phone, {{4}} address).
 * Best effort: a failure never affects the booking (the app and push already carry the same info).
 */
export async function whatsappServiceAssigned(b: FirebaseFirestore.DocumentData): Promise<number> {
  const t = serviceTemplates();
  if (!t) return 0;
  const jobs: Array<{ uid: string; phone: string; template: string; params: unknown[] }> = [];
  if (t.customer && b[BK.CUSTOMER_PHONE]) {
    jobs.push({ uid: String(b[BK.CUSTOMER_ID]), phone: String(b[BK.CUSTOMER_PHONE]), template: t.customer,
      params: [b[BK.PARTNER_NAME], b[BK.SERVICE_NAME], b[BK.PARTNER_PHONE]] });
  }
  if (t.partner && b[BK.PARTNER_PHONE]) {
    const where = [b[BK.ADDRESS_TEXT], b[BK.AREA]].filter((x) => x).join(", ");
    jobs.push({ uid: String(b[BK.PARTNER_ID]), phone: String(b[BK.PARTNER_PHONE]), template: t.partner,
      params: [b[BK.SERVICE_NAME], b[BK.CUSTOMER_NAME], b[BK.CUSTOMER_PHONE], where] });
  }
  if (!jobs.length) return 0;
  try {
    if (!(await reserveDaily(OtpDaily.UTILITY_COUNT, jobs.length, Number(process.env.WHATSAPP_UTILITY_DAILY_CAP || 500)))) {
      functions.logger.warn("WhatsApp service updates: daily cap reached");
      return 0;
    }
    const results = await Promise.all(jobs.map(async (j) => {
      const r = await sendWaTemplate(j.phone, j.template, waLang(await languageOf(j.uid), t.langs), j.params);
      if (!r.ok) functions.logger.warn(`WhatsApp service update ${j.template} failed`, r.error);
      return r.ok;
    }));
    return results.filter(Boolean).length;
  } catch (e) {
    functions.logger.warn("WhatsApp service updates failed", e);
    return 0;
  }
}

// ─────────────────────────────── offers (MARKETING) ───────────────────────────────

async function phoneOf(uid: string, token: Record<string, unknown>): Promise<string> {
  const fromToken = String(token.phone_number || "");
  if (fromToken) return fromToken;
  const u = await admin.auth().getUser(uid).catch(() => null);
  return u?.phoneNumber || "";
}

async function rolesOf(uid: string): Promise<string[]> {
  const [w, e, p] = await Promise.all([
    db.collection(WorkerProfiles.COLLECTION).doc(uid).get(),
    db.collection(EmployerProfiles.COLLECTION).doc(uid).get(),
    db.collection(ServicePartners.COLLECTION).doc(uid).get(),
  ]);
  return [
    ...(w.exists ? [Values.Role.WORKER] : []),
    ...(e.exists ? [Values.Role.EMPLOYER] : []),
    ...(p.exists && p.get(ServicePartners.STATUS) === "APPROVED" ? ["PARTNER"] : []),
  ];
}

/** Settings → "WhatsApp offers". Without `enabled` it only reads the current choice. */
export const whatsappPromos = onCallSecured({ timeoutSeconds: 15 }, async (raw: unknown, context) => {
  const uid = context.auth!.uid;
  const data = obj(raw);
  const ref = db.collection(WP.COLLECTION).doc(uid);
  if (typeof data.enabled !== "boolean") {
    const snap = await ref.get();
    return { enabled: snap.get(WP.PROMOS) === true };
  }
  const now = Timestamp.now();
  if (!data.enabled) {
    await ref.set({ [WP.PROMOS]: false, [WP.OPTED_OUT_AT]: now, [WP.SOURCE]: "app", [WP.UPDATED_AT]: now }, { merge: true });
    return { enabled: false };
  }
  const phone = await phoneOf(uid, (context.auth!.token || {}) as Record<string, unknown>);
  if (!/^\+91[6-9]\d{9}$/.test(phone)) fail("failed-precondition", "Log in with your mobile number to get WhatsApp offers");
  const [roles, language] = await Promise.all([rolesOf(uid), languageOf(uid)]);
  await ref.set({
    [WP.PROMOS]: true,
    [WP.PHONE]: phone,
    [WP.LANGUAGE]: language,
    [WP.ROLES]: roles,
    [WP.CONSENT_AT]: now,
    [WP.OPTED_OUT_AT]: FieldValue.delete(),
    [WP.SOURCE]: "app",
    [WP.UPDATED_AT]: now,
  }, { merge: true });
  return { enabled: true };
});

/** Switches offers off for numbers that replied STOP. Returns how many accounts changed. */
export async function stopWhatsappPromos(phones: string[]): Promise<number> {
  let n = 0;
  for (const phone of phones) {
    const snap = await db.collection(WP.COLLECTION).where(WP.PHONE, "==", phone).limit(5).get();
    const now = Timestamp.now();
    for (const d of snap.docs) {
      if (d.get(WP.PROMOS) !== true) continue;
      await d.ref.update({ [WP.PROMOS]: false, [WP.OPTED_OUT_AT]: now, [WP.SOURCE]: "whatsapp_stop", [WP.UPDATED_AT]: now });
      n++;
    }
  }
  return n;
}

/**
 * Admin: sends one approved MARKETING template to opted-in users.
 * { template, langs: "te,en" (approved translations; each person gets theirs, else the first),
 *   params: string[] (same for everyone), audience: ALL | WORKER | EMPLOYER | PARTNER,
 *   limit (≤ 1000), dryRun }.
 */
export const sendWhatsappPromo = onCallSecured(
  { enforceAppCheck: false, timeoutSeconds: 540, memory: "512MB", secrets: [WHATSAPP_TOKEN_SECRET] },
  async (raw: unknown, context) => {
    if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
    const data = obj(raw);
    const template = str(data, "template", { max: 120, pattern: /^[a-z0-9_]+$/ });
    const langs = langList(data.langs);
    const params = Array.isArray(data.params) ? (data.params as unknown[]).slice(0, 5).map((p) => String(p ?? "").slice(0, 200)) : [];
    const audience = String(data.audience || "ALL").toUpperCase();
    if (!AUDIENCES.includes(audience)) fail("invalid-argument", "audience must be ALL, WORKER, EMPLOYER or PARTNER");
    const limit = Math.max(1, Math.min(PROMO_MAX_PER_RUN, Number(data.limit) || PROMO_MAX_PER_RUN));
    const dryRun = data.dryRun !== false;
    const gapDays = Number(process.env.WHATSAPP_PROMO_GAP_DAYS || PROMO_GAP_DAYS_DEFAULT);
    const now = Date.now();

    let q: FirebaseFirestore.Query = db.collection(WP.COLLECTION).where(WP.PROMOS, "==", true);
    if (audience !== "ALL") q = q.where(WP.ROLES, "array-contains", audience);
    const snap = await q.limit(5000).get();
    const due = snap.docs.filter((d) => promoDue(ms(d.get(WP.LAST_PROMO_AT)), now, gapDays));
    const targets = due.slice(0, limit);
    const summary = { matched: snap.size, due: due.length, willSend: targets.length, skippedRecent: snap.size - due.length };
    if (dryRun) return { dryRun: true, ...summary, ready: waReady() };
    if (!waReady()) fail("failed-precondition", "WhatsApp is not set up on the server");
    if (!targets.length) return { dryRun: false, ...summary, sent: 0, failed: 0 };
    if (!(await reserveDaily(OtpDaily.PROMO_COUNT, targets.length, Number(process.env.WHATSAPP_PROMO_DAILY_CAP || 800)))) {
      fail("resource-exhausted", "Today's WhatsApp offer limit is reached. Send fewer, or try tomorrow.");
    }

    let sent = 0;
    let failed = 0;
    const errors: string[] = [];
    // 10 at a time keeps well under Meta's 80 messages/second.
    for (let i = 0; i < targets.length; i += 10) {
      await Promise.all(targets.slice(i, i + 10).map(async (d) => {
        const r = await sendWaTemplate(String(d.get(WP.PHONE) || ""), template, waLang(d.get(WP.LANGUAGE), langs), params);
        if (r.ok) {
          sent++;
          await d.ref.update({ [WP.LAST_PROMO_AT]: Timestamp.now() });
        } else {
          failed++;
          if (errors.length < 3 && r.error) errors.push(r.error);
        }
      }));
      // A template that Meta rejects fails for everyone: stop after the first batch.
      if (i === 0 && sent === 0) break;
    }
    // Give back the budget for messages that did not go out.
    if (sent < targets.length) {
      await db.collection(OtpDaily.COLLECTION).doc(istDayKey(now))
        .update({ [OtpDaily.PROMO_COUNT]: FieldValue.increment(sent - targets.length) }).catch(() => undefined);
    }
    await db.collection(WC.COLLECTION).add({
      [WC.TEMPLATE]: template, [WC.LANGS]: langs, [WC.PARAMS]: params, [WC.AUDIENCE]: audience,
      [WC.MATCHED]: snap.size, [WC.SENT]: sent, [WC.FAILED]: failed, [WC.SKIPPED_RECENT]: summary.skippedRecent,
      [WC.BY]: context.auth!.uid, [WC.CREATED_AT]: Timestamp.now(),
    });
    return { dryRun: false, ...summary, sent, failed, errors };
  });
