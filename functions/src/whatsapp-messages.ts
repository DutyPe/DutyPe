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
  EmployerProfiles, JobContacts, Jobs, OtpDaily, ServiceBookings as BK, ServicePartners, UserTokens, Values, WhatsappCampaigns as WC,
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

/**
 * Admin: Broadcast WhatsApp launch offers / templates directly to employers or workers.
 * Supports:
 * 1) Single test message to any phone number (`isSingleTest: true`, `testPhone`, `name`, `city`, `postJobUrl`)
 * 2) Bulk broadcast to registered employers (`EMPLOYERS`), registered workers (`WORKERS`), or `BOTH` in Firestore OR a custom list
 * 3) State filtering (`targetState`: e.g. "TELANGANA" to hyper-focus on Telangana employers)
 * 4) Exact city filtering (`targetCity`: e.g. "Khammam", "Hyderabad", "Vijayawada", "Visakhapatnam", or "ALL")
 * 5) Dynamic city extraction (`extractCleanCity`) so {{2}} is always hyper-local (e.g. Khammam, Hyderabad)
 * 6) `dryRun: true` to inspect recipients, variables, breakdown counts, and cost without triggering messages
 */
export const sendWhatsappEmployerBroadcast = onCallSecured(
  { enforceAppCheck: false, timeoutSeconds: 540, memory: "512MB", secrets: [WHATSAPP_TOKEN_SECRET] },
  async (raw: unknown, context) => {
    if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");
    const data = obj(raw);
    const template = str(data, "template", { max: 120, pattern: /^[a-z0-9_]+$/ });
    const englishTemplate = str(data, "englishTemplate", { max: 120, optional: true, pattern: /^[a-z0-9_]+$/ });
    const routingMode = str(data, "routingMode", { max: 30, optional: true }) || "AUTO";
    const audienceTarget = str(data, "audienceTarget", { max: 30, optional: true }) || "EMPLOYERS";
    const targetState = str(data, "targetState", { max: 30, optional: true }) || "ALL";
    const targetCity = str(data, "targetCity", { max: 60, optional: true }) || "ALL";
    const lang = str(data, "lang", { max: 10, optional: true }) || "en";
    const dryRun = data.dryRun !== false;
    const isSingleTest = data.isSingleTest === true;
    const testPhone = str(data, "testPhone", { max: 30, optional: true });
    const defaultName = str(data, "name", { max: 80, optional: true }) || "Sir/Madam";
    const defaultCity = str(data, "city", { max: 80, optional: true }) || "Khammam";
    const postJobUrl = str(data, "postJobUrl", { max: 200, optional: true }) || "https://dutype.in/employer/post-job";
    const headerType = str(data, "headerType", { max: 30, optional: true }) || "AUTO";
    const headerImageUrl = str(data, "headerImageUrl", { max: 500, optional: true });
    const customPhones = Array.isArray(data.customPhones)
      ? (data.customPhones as unknown[]).map((p) => String(p ?? "").trim()).filter(Boolean)
      : [];

    if (!waReady()) fail("failed-precondition", "WhatsApp is not configured on the server");

    // Single test mode
    if (isSingleTest) {
      if (!testPhone) fail("invalid-argument", "testPhone is required for single test");
      let cleaned = testPhone.replace(/\D/g, "");
      if (cleaned.length === 10) cleaned = "91" + cleaned;
      const formatted = "+" + cleaned;
      if (!/^\+91[6-9]\d{9}$/.test(formatted)) {
        fail("invalid-argument", "Invalid Indian phone number: " + testPhone);
      }
      let params = Array.isArray(data.params)
        ? (data.params as unknown[])
        : [defaultName, defaultCity];
      if (dryRun) {
        return { ok: true, dryRun: true, phone: formatted, template, params };
      }

      const shouldIncludeHeader = headerType === "IMAGE" ||
        (headerType === "AUTO" && (!!headerImageUrl || template === "dutype_employer_quick_hire" || template.includes("quick_hire")));

      const defaultBanner = lang === "te"
        ? "https://dutype-860ac.web.app/dutype_branded_marketing_banner_telugu.jpg"
        : "https://dutype-860ac.web.app/dutype_branded_marketing_banner.jpg";
      const activeHeaderUrl = headerImageUrl || defaultBanner;

      let sendOpts = shouldIncludeHeader ? { headerImageUrl: activeHeaderUrl } : undefined;
      let res = await sendWaTemplate(formatted, template, lang, params, process.env, sendOpts);

      // Auto-fallback if Meta reports 132012 (header format mismatch)
      if (!res.ok && res.error && (res.error.includes("132012") || res.error.includes("header"))) {
        if (sendOpts?.headerImageUrl) {
          functions.logger.warn("WhatsApp 132012 header mismatch, auto-retrying without header...");
          sendOpts = undefined;
          res = await sendWaTemplate(formatted, template, lang, params, process.env, undefined);
        } else {
          functions.logger.warn("WhatsApp 132012 header mismatch, auto-retrying with image header...");
          sendOpts = { headerImageUrl: activeHeaderUrl };
          res = await sendWaTemplate(formatted, template, lang, params, process.env, sendOpts);
        }
      }

      // Auto-fallback if Meta reports 132000 (parameter count mismatch)
      if (!res.ok && res.error && res.error.includes("132000")) {
        functions.logger.warn("WhatsApp 132000 parameter mismatch, auto-retrying with 0 parameters (static template)...");
        const retryRes0 = await sendWaTemplate(formatted, template, lang, [], process.env, sendOpts);
        if (retryRes0.ok) {
          res = retryRes0;
          params = [];
        } else if (params.length !== 1) {
          functions.logger.warn("WhatsApp 132000 parameter mismatch, auto-retrying with 1 parameter (name only)...");
          const retryRes1 = await sendWaTemplate(formatted, template, lang, [defaultName], process.env, sendOpts);
          if (retryRes1.ok) {
            res = retryRes1;
            params = [defaultName];
          }
        }
      }
      if (!res.ok) {
        fail("internal", "WhatsApp send failed: " + (res.error || "unknown error"));
      }
      return { ok: true, dryRun: false, phone: formatted, template, params };
    }

    // Helper to detect if employer/worker is in Telugu states (Telangana / Andhra Pradesh)
    const NON_TELUGU_IDENTIFIERS = [
      "bengaluru", "bangalore", "karnataka", "mumbai", "pune", "maharashtra",
      "delhi", "chennai", "tamil nadu", "kolkata", "west bengal", "kerala",
      "kochi", "gujarat", "ahmedabad", "jaipur", "rajasthan", "lucknow",
      "uttar pradesh", "bihar", "patna", "madhya pradesh", "indore", "bhopal",
      "punjab", "chandigarh", "haryana", "gurugram", "noida", "goa", "odisha"
    ];

    function isTeluguStateEmployer(stateText?: string, cityText?: string, addrText?: string): boolean {
      const combined = `${stateText || ""} ${cityText || ""} ${addrText || ""}`.toLowerCase();
      for (const nonTe of NON_TELUGU_IDENTIFIERS) {
        if (combined.includes(nonTe)) return false;
      }
      return true;
    }

    const TG_IDENTIFIERS = [
      "telangana", "khammam", "hyderabad", "secunderabad", "warangal", "hanumakonda", "kazipet",
      "karimnagar", "nizamabad", "nalgonda", "suryapet", "mahabubnagar", "siddipet", "mancherial",
      "kothagudem", "bhadradri", "medchal", "malkajgiri", "ranga reddy", "rangareddy", "sangareddy",
      "jagitial", "kamareddy", "peddapalli", "sircilla", "wanaparthy", "gadwal", "nagarkurnool",
      "vikarabad", "asifabad", "adilabad", "mulugu", "narayanpet", "abids", "tellapur", "yella reddy guda",
      "ameerpet", "madhapur", "gachibowli", "kukatpally", "dilsukhnagar", "lb nagar", "uppal",
      "begumpet", "kondapur", "hitec city", "jubilee hills", "banjara hills", "wyra", "sathupally", "madhira"
    ];

    function isTelanganaProfile(cityText?: string, addrText?: string): boolean {
      const combined = `${cityText || ""} ${addrText || ""}`.toLowerCase();
      return TG_IDENTIFIERS.some((k) => combined.includes(k));
    }

    // Dynamic clean city extraction to guarantee {{2}} has the real locality (e.g., Khammam, Hyderabad)
    const KNOWN_TELUGU_CITIES = [
      "Khammam", "Hyderabad", "Secunderabad", "Warangal", "Hanamkonda", "Kazipet",
      "Vijayawada", "Visakhapatnam", "Vizag", "Guntur", "Nellore", "Kurnool",
      "Tirupati", "Kadapa", "Kakinada", "Rajahmundry", "Karimnagar", "Nizamabad",
      "Nalgonda", "Mahabubnagar", "Suryapet", "Miryalaguda", "Ramagundam",
      "Mancherial", "Adilabad", "Siddipet", "Sangareddy", "Medak", "Eluru",
      "Ongole", "Anantapur", "Chittoor", "Vizianagaram", "Srikakulam",
      "Machilipatnam", "Bhimavaram", "Tenali", "Proddatur", "Hindupur",
      "Kothagudem", "Bhadrachalam", "Palvancha", "Sathupally", "Madhira", "Wyra",
      "Yellandu", "Bengaluru", "Bangalore", "Chennai", "Mumbai", "Pune", "Delhi"
    ];

    function extractCleanCity(area?: string, address?: string, fallback: string = "Khammam"): string {
      const combined = `${area || ""} ${address || ""}`.trim();
      if (!combined) return fallback;

      // 1. Direct keyword match for prominent cities
      for (const city of KNOWN_TELUGU_CITIES) {
        const regex = new RegExp(`\\b${city}\\b`, "i");
        if (regex.test(combined)) {
          return city.toLowerCase() === "vizag" ? "Visakhapatnam" : city;
        }
      }

      // 2. Use area if concise and non-empty (stripped of pincode)
      if (area && area.trim().length > 0) {
        const cleanArea = area.replace(/\b\d{6}\b/g, "").replace(/[,.-]/g, " ").trim();
        const parts = cleanArea.split(/\s+/).filter(Boolean);
        if (parts.length > 0 && parts.length <= 3) {
          return parts.join(" ");
        }
      }

      // 3. Fallback: parse address segments from end
      if (address) {
        const segments = address.split(",").map((s) => s.trim()).filter(Boolean);
        for (let i = segments.length - 1; i >= 0; i--) {
          const seg = segments[i].replace(/\b\d{6}\b/g, "").trim();
          if (seg.length >= 3 && seg.length <= 25 && !/\d/.test(seg)) {
            if (!/^(telangana|andhra pradesh|india|ap|tg|ts)$/i.test(seg)) {
              return seg;
            }
          }
        }
      }

      return fallback;
    }

    // Bulk broadcast mode
    type TargetRecipient = {
      phone: string;
      name: string;
      city: string;
      rawLocation: string;
      role: "EMPLOYER" | "WORKER";
      isTeluguState: boolean;
    };
    const targetMap = new Map<string, TargetRecipient>();

    if (customPhones.length > 0) {
      for (const rawPhone of customPhones) {
        let cleaned = rawPhone.replace(/\D/g, "");
        if (cleaned.length === 10) cleaned = "91" + cleaned;
        const formatted = "+" + cleaned;
        if (/^\+91[6-9]\d{9}$/.test(formatted)) {
          targetMap.set(formatted, {
            phone: formatted,
            name: defaultName,
            city: defaultCity,
            rawLocation: defaultCity,
            role: "EMPLOYER",
            isTeluguState: true
          });
        }
      }
    } else {
      // 1. Fetch employers if requested
      if (audienceTarget === "EMPLOYERS" || audienceTarget === "BOTH") {
        const empSnap = await db.collection(EmployerProfiles.COLLECTION).limit(1000).get();
        for (const d of empSnap.docs) {
          const phone = String(d.get(EmployerProfiles.PHONE) || "").trim();
          const empName = String(d.get(EmployerProfiles.OWNER_NAME) || d.get(EmployerProfiles.BUSINESS_NAME) || "").trim() || defaultName;
          const area = String(d.get(EmployerProfiles.AREA) || "").trim();
          const addr = String(d.get(EmployerProfiles.ADDRESS) || "").trim();
          const empCity = extractCleanCity(area, addr, defaultCity);
          const empState = String(d.get("state") || d.get("location.state") || "").trim();
          const isTe = isTeluguStateEmployer(empState, empCity, addr);

          let cleaned = phone.replace(/\D/g, "");
          if (cleaned.length === 10) cleaned = "91" + cleaned;
          const formatted = "+" + cleaned;
          if (/^\+91[6-9]\d{9}$/.test(formatted)) {
            targetMap.set(formatted, {
              phone: formatted,
              name: empName,
              city: empCity,
              rawLocation: `${area} ${addr}`.trim(),
              role: "EMPLOYER",
              isTeluguState: isTe
            });
          }
        }

        // 1b. If targeting Telangana, also aggregate active verified Telangana business job posters
        if (targetState === "TELANGANA") {
          const jobSnap = await db.collection(Jobs.COLLECTION).where(Jobs.STATE, "==", "Telangana").limit(400).get();
          const contactsSnap = await db.collection(JobContacts.COLLECTION).limit(500).get();
          const contactsMap = new Map<string, string>();
          contactsSnap.docs.forEach((cd) => {
            contactsMap.set(cd.id, String(cd.data()[JobContacts.CONTACT_NUMBER] || ""));
          });

          for (const jd of jobSnap.docs) {
            const jdata = jd.data();
            const rawPhone = contactsMap.get(jd.id);
            if (rawPhone) {
              let cleaned = String(rawPhone).replace(/\D/g, "");
              if (cleaned.length === 10) cleaned = "91" + cleaned;
              const formatted = "+" + cleaned;
              if (/^\+91[6-9]\d{9}$/.test(formatted)) {
                if (!targetMap.has(formatted)) {
                  const name = String(jdata[Jobs.COMPANY_NAME] || defaultName).trim();
                  const city = String(jdata[Jobs.DISTRICT] || jdata[Jobs.AREA] || "Telangana").trim();
                  targetMap.set(formatted, {
                    phone: formatted,
                    name,
                    city,
                    rawLocation: String(jdata[Jobs.AREA] || city).trim(),
                    role: "EMPLOYER",
                    isTeluguState: true
                  });
                }
              }
            }
          }
        }
      }

      // 2. Fetch workers if requested
      if (audienceTarget === "WORKERS" || audienceTarget === "BOTH") {
        const workerSnap = await db.collection(WorkerProfiles.COLLECTION).limit(1000).get();
        for (const d of workerSnap.docs) {
          const phone = String(d.get(WorkerProfiles.PHONE) || d.get("mobile") || "").trim();
          const workerName = String(d.get(WorkerProfiles.NAME) || "").trim() || "Candidate";
          const area = String(d.get(WorkerProfiles.AREA) || "").trim();
          const addr = String(d.get(WorkerProfiles.ADDRESS) || "").trim();
          const workerCity = extractCleanCity(area, addr, defaultCity);
          const workerState = String(d.get("state") || d.get("location.state") || "").trim();
          const isTe = isTeluguStateEmployer(workerState, workerCity, addr);

          let cleaned = phone.replace(/\D/g, "");
          if (cleaned.length === 10) cleaned = "91" + cleaned;
          const formatted = "+" + cleaned;
          if (/^\+91[6-9]\d{9}$/.test(formatted)) {
            if (!targetMap.has(formatted) || audienceTarget === "WORKERS") {
              targetMap.set(formatted, {
                phone: formatted,
                name: workerName,
                city: workerCity,
                rawLocation: `${area} ${addr}`.trim(),
                role: "WORKER",
                isTeluguState: isTe
              });
            }
          }
        }
      }
    }

    let allTargets = Array.from(targetMap.values());

    // Apply Target State Filtering if specified
    if (targetState === "TELANGANA") {
      allTargets = allTargets.filter((t) => isTelanganaProfile(t.city, t.rawLocation));
    }

    // Apply Exact City Filtering if specified
    if (targetCity && targetCity !== "ALL") {
      const needle = targetCity.trim().toLowerCase();
      allTargets = allTargets.filter(
        (t) => t.city.toLowerCase() === needle || t.rawLocation.toLowerCase().includes(needle)
      );
    }

    // Apply Region / Language filtering based on routingMode
    if (routingMode === "TELUGU_ONLY") {
      allTargets = allTargets.filter((t) => t.isTeluguState);
    } else if (routingMode === "ENGLISH_ONLY") {
      allTargets = allTargets.filter((t) => !t.isTeluguState);
    }

    const targets = allTargets;
    const summary = {
      matched: targets.length,
      willSend: targets.length,
      dryRun,
      teluguCount: targets.filter((t) => t.isTeluguState).length,
      englishCount: targets.filter((t) => !t.isTeluguState).length,
      employerCount: targets.filter((t) => t.role === "EMPLOYER").length,
      workerCount: targets.filter((t) => t.role === "WORKER").length,
    };

    if (dryRun) {
      return { ...summary, sample: targets.slice(0, 8) };
    }

    if (!targets.length) {
      return { dryRun: false, matched: 0, sent: 0, failed: 0, errors: [] };
    }

    let sent = 0;
    let failed = 0;
    const errors: string[] = [];

    // Send in batches of 10
    for (let i = 0; i < targets.length; i += 10) {
      await Promise.all(
        targets.slice(i, i + 10).map(async (t) => {
          // Determine language and template for this recipient
          let recipientTemplate = template;
          let recipientLang = lang;

          if (routingMode === "AUTO") {
            if (t.isTeluguState) {
              recipientTemplate = template;
              recipientLang = "te";
            } else {
              recipientTemplate = englishTemplate || template;
              recipientLang = "en";
            }
          } else if (routingMode === "ALL_TELUGU") {
            recipientTemplate = template;
            recipientLang = "te";
          } else if (routingMode === "ALL_ENGLISH") {
            recipientTemplate = englishTemplate || template;
            recipientLang = "en";
          }

          let params: unknown[];
          if (Array.isArray(data.params)) {
            if (data.params.length === 0) {
              params = [];
            } else if (data.params.length === 1) {
              params = [t.name];
            } else {
              params = [t.name, t.city, ...(data.params.slice(2))];
            }
          } else {
            params = [t.name, t.city];
          }

          const shouldIncludeHeader = headerType === "IMAGE" ||
            (headerType === "AUTO" && (!!headerImageUrl || recipientTemplate === "dutype_employer_quick_hire" || recipientTemplate.includes("quick_hire")));

          const defaultBanner = recipientLang === "te"
            ? "https://dutype-860ac.web.app/dutype_branded_marketing_banner_telugu.jpg"
            : "https://dutype-860ac.web.app/dutype_branded_marketing_banner.jpg";
          const activeHeaderUrl = headerImageUrl || defaultBanner;

          let sendOpts = shouldIncludeHeader ? { headerImageUrl: activeHeaderUrl } : undefined;
          let r = await sendWaTemplate(t.phone, recipientTemplate, recipientLang, params, process.env, sendOpts);

          // Auto-fallback if Meta reports 132012 (header format mismatch)
          if (!r.ok && r.error && (r.error.includes("132012") || r.error.includes("header"))) {
            if (sendOpts?.headerImageUrl) {
              const retryNoHeader = await sendWaTemplate(t.phone, recipientTemplate, recipientLang, params, process.env, undefined);
              if (retryNoHeader.ok) {
                r = retryNoHeader;
                sendOpts = undefined;
              }
            } else {
              const retryWithHeader = await sendWaTemplate(t.phone, recipientTemplate, recipientLang, params, process.env, { headerImageUrl: activeHeaderUrl });
              if (retryWithHeader.ok) {
                r = retryWithHeader;
                sendOpts = { headerImageUrl: activeHeaderUrl };
              }
            }
          }

          // Auto-fallback if Meta reports 132000 (parameter count mismatch)
          if (!r.ok && r.error && r.error.includes("132000")) {
            const retry0 = await sendWaTemplate(t.phone, recipientTemplate, recipientLang, [], process.env, sendOpts);
            if (retry0.ok) {
              r = retry0;
            } else {
              const retry1 = await sendWaTemplate(t.phone, recipientTemplate, recipientLang, [t.name], process.env, sendOpts);
              if (retry1.ok) r = retry1;
            }
          }
          if (r.ok) {
            sent++;
          } else {
            failed++;
            if (errors.length < 5 && r.error) errors.push(`${t.phone}: ${r.error}`);
          }
        })
      );
      // Abort early if the first batch fails completely (template error or bad auth)
      if (i === 0 && sent === 0 && failed > 0) break;
    }

    await db.collection(WC.COLLECTION).add({
      [WC.TEMPLATE]: template,
      [WC.LANGS]: [lang],
      [WC.PARAMS]: [defaultName, defaultCity],
      [WC.AUDIENCE]: "EMPLOYER_BROADCAST",
      [WC.MATCHED]: targets.length,
      [WC.SENT]: sent,
      [WC.FAILED]: failed,
      [WC.BY]: context.auth!.uid,
      [WC.CREATED_AT]: Timestamp.now()
    });

    return { dryRun: false, matched: targets.length, sent, failed, errors };
  }
);

/**
 * Admin: Aggregates verified Telangana employers from both registered employer profiles
 * and active verified Telangana job listings into a single de-duplicated audience.
 */
export const fetchTelanganaEmployers = onCallSecured(
  { enforceAppCheck: false, timeoutSeconds: 60, memory: "256MB" },
  async (_raw: unknown, context) => {
    if (!(await isCallerAdmin(context))) fail("permission-denied", "Admins only");

    const TG_IDENTIFIERS = [
      "telangana", "khammam", "hyderabad", "secunderabad", "warangal", "hanumakonda", "kazipet",
      "karimnagar", "nizamabad", "nalgonda", "suryapet", "mahabubnagar", "siddipet", "mancherial",
      "kothagudem", "bhadradri", "medchal", "malkajgiri", "ranga reddy", "rangareddy", "sangareddy",
      "jagitial", "kamareddy", "peddapalli", "sircilla", "wanaparthy", "gadwal", "nagarkurnool",
      "vikarabad", "asifabad", "adilabad", "mulugu", "narayanpet", "abids", "tellapur", "yella reddy guda",
      "ameerpet", "madhapur", "gachibowli", "kukatpally", "dilsukhnagar", "lb nagar", "uppal",
      "begumpet", "kondapur", "hitec city", "jubilee hills", "banjara hills", "wyra", "sathupally", "madhira"
    ];

    const employersMap = new Map<string, { phone: string; name: string; city: string; area: string; source: string }>();

    // 1. Registered employer_profiles
    const empSnap = await db.collection(EmployerProfiles.COLLECTION).limit(1000).get();
    for (const d of empSnap.docs) {
      const data = d.data();
      const phone = String(data[EmployerProfiles.PHONE] || "").trim();
      if (!phone) continue;

      const combined = `${data[EmployerProfiles.AREA] || ""} ${data[EmployerProfiles.ADDRESS] || ""} ${data.state || ""} ${data.city || ""} ${data.district || ""}`.toLowerCase();
      const isTg = TG_IDENTIFIERS.some((k) => combined.includes(k));
      if (isTg) {
        let cleaned = phone.replace(/\D/g, "");
        if (cleaned.length === 10) cleaned = "91" + cleaned;
        if (cleaned.length === 12 && cleaned.startsWith("91")) {
          const formatted = "+" + cleaned;
          const name = String(data[EmployerProfiles.BUSINESS_NAME] || data[EmployerProfiles.OWNER_NAME] || "Business Owner").trim();
          const city = String(data.city || data[EmployerProfiles.AREA] || "Telangana").trim();
          employersMap.set(cleaned, {
            phone: formatted,
            name,
            city,
            area: String(data[EmployerProfiles.AREA] || city).trim(),
            source: "employer_profile"
          });
        }
      }
    }

    // 2. Active verified Telangana job posters
    const jobSnap = await db.collection(Jobs.COLLECTION).where(Jobs.STATE, "==", "Telangana").limit(400).get();
    const contactsSnap = await db.collection(JobContacts.COLLECTION).limit(500).get();
    const contactsMap = new Map<string, string>();
    contactsSnap.docs.forEach((cd) => {
      contactsMap.set(cd.id, String(cd.data()[JobContacts.CONTACT_NUMBER] || ""));
    });

    for (const jd of jobSnap.docs) {
      const jdata = jd.data();
      const rawPhone = contactsMap.get(jd.id);
      if (rawPhone) {
        let cleaned = String(rawPhone).replace(/\D/g, "");
        if (cleaned.length === 10) cleaned = "91" + cleaned;
        if (cleaned.length === 12 && cleaned.startsWith("91")) {
          if (!employersMap.has(cleaned)) {
            const formatted = "+" + cleaned;
            const name = String(jdata[Jobs.COMPANY_NAME] || "Business Owner").trim();
            const city = String(jdata[Jobs.DISTRICT] || jdata[Jobs.AREA] || "Telangana").trim();
            employersMap.set(cleaned, {
              phone: formatted,
              name,
              city,
              area: String(jdata[Jobs.AREA] || city).trim(),
              source: "telangana_job_poster"
            });
          }
        }
      }
    }

    const employers = Array.from(employersMap.values());
    return {
      ok: true,
      count: employers.length,
      employers
    };
  }
);
