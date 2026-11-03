"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.fetchTelanganaEmployers = exports.sendWhatsappEmployerBroadcast = exports.sendWhatsappPromo = exports.whatsappPromos = void 0;
exports.whatsappServiceAssigned = whatsappServiceAssigned;
exports.stopWhatsappPromos = stopWhatsappPromos;
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
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const secure_callable_1 = require("./secure-callable");
const app_config_1 = require("./app-config");
const input_1 = require("./lib/input");
const phone_otp_1 = require("./lib/phone-otp");
const whatsapp_1 = require("./lib/whatsapp");
const schema_1 = require("./schema");
const db = admin.firestore();
const { FieldValue, Timestamp } = admin.firestore;
const DAY_MS = 24 * 60 * 60 * 1000;
const AUDIENCES = ["ALL", schema_1.Values.Role.WORKER, schema_1.Values.Role.EMPLOYER, "PARTNER"];
const ms = (v) => { var _a, _b; return (_b = (_a = v === null || v === void 0 ? void 0 : v.toMillis) === null || _a === void 0 ? void 0 : _a.call(v)) !== null && _b !== void 0 ? _b : 0; };
async function languageOf(uid) {
    const t = await db.collection(schema_1.UserTokens.COLLECTION).doc(uid).get();
    return String(t.get(schema_1.UserTokens.LANGUAGE) || "en").slice(0, 2).toLowerCase();
}
/** Reserves [n] messages of today's [field] budget; false when that would pass [cap]. */
async function reserveDaily(field, n, cap) {
    const ref = db.collection(schema_1.OtpDaily.COLLECTION).doc((0, phone_otp_1.istDayKey)(Date.now()));
    return db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        const used = Number(snap.get(field) || 0);
        if (used + n > cap)
            return false;
        tx.set(ref, { [field]: used + n, [schema_1.OtpDaily.EXPIRE_AT]: Timestamp.fromMillis(Date.now() + 3 * DAY_MS) }, { merge: true });
        return true;
    });
}
// ─────────────────────────── service updates (UTILITY) ───────────────────────────
/**
 * "Partner found" to the customer ({{1}} partner, {{2}} service, {{3}} partner phone) and "Job
 * confirmed" to the partner ({{1}} service, {{2}} customer, {{3}} customer phone, {{4}} address).
 * Best effort: a failure never affects the booking (the app and push already carry the same info).
 */
async function whatsappServiceAssigned(b) {
    const t = (0, whatsapp_1.serviceTemplates)();
    if (!t)
        return 0;
    const jobs = [];
    if (t.customer && b[schema_1.ServiceBookings.CUSTOMER_PHONE]) {
        jobs.push({ uid: String(b[schema_1.ServiceBookings.CUSTOMER_ID]), phone: String(b[schema_1.ServiceBookings.CUSTOMER_PHONE]), template: t.customer,
            params: [b[schema_1.ServiceBookings.PARTNER_NAME], b[schema_1.ServiceBookings.SERVICE_NAME], b[schema_1.ServiceBookings.PARTNER_PHONE]] });
    }
    if (t.partner && b[schema_1.ServiceBookings.PARTNER_PHONE]) {
        const where = [b[schema_1.ServiceBookings.ADDRESS_TEXT], b[schema_1.ServiceBookings.AREA]].filter((x) => x).join(", ");
        jobs.push({ uid: String(b[schema_1.ServiceBookings.PARTNER_ID]), phone: String(b[schema_1.ServiceBookings.PARTNER_PHONE]), template: t.partner,
            params: [b[schema_1.ServiceBookings.SERVICE_NAME], b[schema_1.ServiceBookings.CUSTOMER_NAME], b[schema_1.ServiceBookings.CUSTOMER_PHONE], where] });
    }
    if (!jobs.length)
        return 0;
    try {
        if (!(await reserveDaily(schema_1.OtpDaily.UTILITY_COUNT, jobs.length, Number(process.env.WHATSAPP_UTILITY_DAILY_CAP || 500)))) {
            functions.logger.warn("WhatsApp service updates: daily cap reached");
            return 0;
        }
        const results = await Promise.all(jobs.map(async (j) => {
            const r = await (0, whatsapp_1.sendWaTemplate)(j.phone, j.template, (0, whatsapp_1.waLang)(await languageOf(j.uid), t.langs), j.params);
            if (!r.ok)
                functions.logger.warn(`WhatsApp service update ${j.template} failed`, r.error);
            return r.ok;
        }));
        return results.filter(Boolean).length;
    }
    catch (e) {
        functions.logger.warn("WhatsApp service updates failed", e);
        return 0;
    }
}
// ─────────────────────────────── offers (MARKETING) ───────────────────────────────
async function phoneOf(uid, token) {
    const fromToken = String(token.phone_number || "");
    if (fromToken)
        return fromToken;
    const u = await admin.auth().getUser(uid).catch(() => null);
    return (u === null || u === void 0 ? void 0 : u.phoneNumber) || "";
}
async function rolesOf(uid) {
    const [w, e, p] = await Promise.all([
        db.collection(schema_1.WorkerProfiles.COLLECTION).doc(uid).get(),
        db.collection(schema_1.EmployerProfiles.COLLECTION).doc(uid).get(),
        db.collection(schema_1.ServicePartners.COLLECTION).doc(uid).get(),
    ]);
    return [
        ...(w.exists ? [schema_1.Values.Role.WORKER] : []),
        ...(e.exists ? [schema_1.Values.Role.EMPLOYER] : []),
        ...(p.exists && p.get(schema_1.ServicePartners.STATUS) === "APPROVED" ? ["PARTNER"] : []),
    ];
}
/** Settings → "WhatsApp offers". Without `enabled` it only reads the current choice. */
exports.whatsappPromos = (0, secure_callable_1.onCallSecured)({ timeoutSeconds: 15 }, async (raw, context) => {
    const uid = context.auth.uid;
    const data = (0, input_1.obj)(raw);
    const ref = db.collection(schema_1.WhatsappPrefs.COLLECTION).doc(uid);
    if (typeof data.enabled !== "boolean") {
        const snap = await ref.get();
        return { enabled: snap.get(schema_1.WhatsappPrefs.PROMOS) === true };
    }
    const now = Timestamp.now();
    if (!data.enabled) {
        await ref.set({ [schema_1.WhatsappPrefs.PROMOS]: false, [schema_1.WhatsappPrefs.OPTED_OUT_AT]: now, [schema_1.WhatsappPrefs.SOURCE]: "app", [schema_1.WhatsappPrefs.UPDATED_AT]: now }, { merge: true });
        return { enabled: false };
    }
    const phone = await phoneOf(uid, (context.auth.token || {}));
    if (!/^\+91[6-9]\d{9}$/.test(phone))
        (0, input_1.fail)("failed-precondition", "Log in with your mobile number to get WhatsApp offers");
    const [roles, language] = await Promise.all([rolesOf(uid), languageOf(uid)]);
    await ref.set({
        [schema_1.WhatsappPrefs.PROMOS]: true,
        [schema_1.WhatsappPrefs.PHONE]: phone,
        [schema_1.WhatsappPrefs.LANGUAGE]: language,
        [schema_1.WhatsappPrefs.ROLES]: roles,
        [schema_1.WhatsappPrefs.CONSENT_AT]: now,
        [schema_1.WhatsappPrefs.OPTED_OUT_AT]: FieldValue.delete(),
        [schema_1.WhatsappPrefs.SOURCE]: "app",
        [schema_1.WhatsappPrefs.UPDATED_AT]: now,
    }, { merge: true });
    return { enabled: true };
});
/** Switches offers off for numbers that replied STOP. Returns how many accounts changed. */
async function stopWhatsappPromos(phones) {
    let n = 0;
    for (const phone of phones) {
        const snap = await db.collection(schema_1.WhatsappPrefs.COLLECTION).where(schema_1.WhatsappPrefs.PHONE, "==", phone).limit(5).get();
        const now = Timestamp.now();
        for (const d of snap.docs) {
            if (d.get(schema_1.WhatsappPrefs.PROMOS) !== true)
                continue;
            await d.ref.update({ [schema_1.WhatsappPrefs.PROMOS]: false, [schema_1.WhatsappPrefs.OPTED_OUT_AT]: now, [schema_1.WhatsappPrefs.SOURCE]: "whatsapp_stop", [schema_1.WhatsappPrefs.UPDATED_AT]: now });
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
exports.sendWhatsappPromo = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false, timeoutSeconds: 540, memory: "512MB", secrets: [whatsapp_1.WHATSAPP_TOKEN_SECRET] }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const template = (0, input_1.str)(data, "template", { max: 120, pattern: /^[a-z0-9_]+$/ });
    const langs = (0, whatsapp_1.langList)(data.langs);
    const params = Array.isArray(data.params) ? data.params.slice(0, 5).map((p) => String(p !== null && p !== void 0 ? p : "").slice(0, 200)) : [];
    const audience = String(data.audience || "ALL").toUpperCase();
    if (!AUDIENCES.includes(audience))
        (0, input_1.fail)("invalid-argument", "audience must be ALL, WORKER, EMPLOYER or PARTNER");
    const limit = Math.max(1, Math.min(whatsapp_1.PROMO_MAX_PER_RUN, Number(data.limit) || whatsapp_1.PROMO_MAX_PER_RUN));
    const dryRun = data.dryRun !== false;
    const gapDays = Number(process.env.WHATSAPP_PROMO_GAP_DAYS || whatsapp_1.PROMO_GAP_DAYS_DEFAULT);
    const now = Date.now();
    let q = db.collection(schema_1.WhatsappPrefs.COLLECTION).where(schema_1.WhatsappPrefs.PROMOS, "==", true);
    if (audience !== "ALL")
        q = q.where(schema_1.WhatsappPrefs.ROLES, "array-contains", audience);
    const snap = await q.limit(5000).get();
    const due = snap.docs.filter((d) => (0, whatsapp_1.promoDue)(ms(d.get(schema_1.WhatsappPrefs.LAST_PROMO_AT)), now, gapDays));
    const targets = due.slice(0, limit);
    const summary = { matched: snap.size, due: due.length, willSend: targets.length, skippedRecent: snap.size - due.length };
    if (dryRun)
        return Object.assign(Object.assign({ dryRun: true }, summary), { ready: (0, whatsapp_1.waReady)() });
    if (!(0, whatsapp_1.waReady)())
        (0, input_1.fail)("failed-precondition", "WhatsApp is not set up on the server");
    if (!targets.length)
        return Object.assign(Object.assign({ dryRun: false }, summary), { sent: 0, failed: 0 });
    if (!(await reserveDaily(schema_1.OtpDaily.PROMO_COUNT, targets.length, Number(process.env.WHATSAPP_PROMO_DAILY_CAP || 800)))) {
        (0, input_1.fail)("resource-exhausted", "Today's WhatsApp offer limit is reached. Send fewer, or try tomorrow.");
    }
    let sent = 0;
    let failed = 0;
    const errors = [];
    // 10 at a time keeps well under Meta's 80 messages/second.
    for (let i = 0; i < targets.length; i += 10) {
        await Promise.all(targets.slice(i, i + 10).map(async (d) => {
            const r = await (0, whatsapp_1.sendWaTemplate)(String(d.get(schema_1.WhatsappPrefs.PHONE) || ""), template, (0, whatsapp_1.waLang)(d.get(schema_1.WhatsappPrefs.LANGUAGE), langs), params);
            if (r.ok) {
                sent++;
                await d.ref.update({ [schema_1.WhatsappPrefs.LAST_PROMO_AT]: Timestamp.now() });
            }
            else {
                failed++;
                if (errors.length < 3 && r.error)
                    errors.push(r.error);
            }
        }));
        // A template that Meta rejects fails for everyone: stop after the first batch.
        if (i === 0 && sent === 0)
            break;
    }
    // Give back the budget for messages that did not go out.
    if (sent < targets.length) {
        await db.collection(schema_1.OtpDaily.COLLECTION).doc((0, phone_otp_1.istDayKey)(now))
            .update({ [schema_1.OtpDaily.PROMO_COUNT]: FieldValue.increment(sent - targets.length) }).catch(() => undefined);
    }
    await db.collection(schema_1.WhatsappCampaigns.COLLECTION).add({
        [schema_1.WhatsappCampaigns.TEMPLATE]: template, [schema_1.WhatsappCampaigns.LANGS]: langs, [schema_1.WhatsappCampaigns.PARAMS]: params, [schema_1.WhatsappCampaigns.AUDIENCE]: audience,
        [schema_1.WhatsappCampaigns.MATCHED]: snap.size, [schema_1.WhatsappCampaigns.SENT]: sent, [schema_1.WhatsappCampaigns.FAILED]: failed, [schema_1.WhatsappCampaigns.SKIPPED_RECENT]: summary.skippedRecent,
        [schema_1.WhatsappCampaigns.BY]: context.auth.uid, [schema_1.WhatsappCampaigns.CREATED_AT]: Timestamp.now(),
    });
    return Object.assign(Object.assign({ dryRun: false }, summary), { sent, failed, errors });
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
exports.sendWhatsappEmployerBroadcast = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false, timeoutSeconds: 540, memory: "512MB", secrets: [whatsapp_1.WHATSAPP_TOKEN_SECRET] }, async (raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const data = (0, input_1.obj)(raw);
    const template = (0, input_1.str)(data, "template", { max: 120, pattern: /^[a-z0-9_]+$/ });
    const englishTemplate = (0, input_1.str)(data, "englishTemplate", { max: 120, optional: true, pattern: /^[a-z0-9_]+$/ });
    const routingMode = (0, input_1.str)(data, "routingMode", { max: 30, optional: true }) || "AUTO";
    const audienceTarget = (0, input_1.str)(data, "audienceTarget", { max: 30, optional: true }) || "EMPLOYERS";
    const targetState = (0, input_1.str)(data, "targetState", { max: 30, optional: true }) || "ALL";
    const targetCity = (0, input_1.str)(data, "targetCity", { max: 60, optional: true }) || "ALL";
    const lang = (0, input_1.str)(data, "lang", { max: 10, optional: true }) || "en";
    const dryRun = data.dryRun !== false;
    const isSingleTest = data.isSingleTest === true;
    const testPhone = (0, input_1.str)(data, "testPhone", { max: 30, optional: true });
    const defaultName = (0, input_1.str)(data, "name", { max: 80, optional: true }) || "Sir/Madam";
    const defaultCity = (0, input_1.str)(data, "city", { max: 80, optional: true }) || "Khammam";
    const postJobUrl = (0, input_1.str)(data, "postJobUrl", { max: 200, optional: true }) || "https://dutype.in/employer/post-job";
    const customPhones = Array.isArray(data.customPhones)
        ? data.customPhones.map((p) => String(p !== null && p !== void 0 ? p : "").trim()).filter(Boolean)
        : [];
    if (!(0, whatsapp_1.waReady)())
        (0, input_1.fail)("failed-precondition", "WhatsApp is not configured on the server");
    // Single test mode
    if (isSingleTest) {
        if (!testPhone)
            (0, input_1.fail)("invalid-argument", "testPhone is required for single test");
        let cleaned = testPhone.replace(/\D/g, "");
        if (cleaned.length === 10)
            cleaned = "91" + cleaned;
        const formatted = "+" + cleaned;
        if (!/^\+91[6-9]\d{9}$/.test(formatted)) {
            (0, input_1.fail)("invalid-argument", "Invalid Indian phone number: " + testPhone);
        }
        let params = Array.isArray(data.params)
            ? data.params
            : [defaultName, defaultCity];
        if (dryRun) {
            return { ok: true, dryRun: true, phone: formatted, template, params };
        }
        let res = await (0, whatsapp_1.sendWaTemplate)(formatted, template, lang, params);
        // Auto-fallback if Meta reports 132000 (parameter count mismatch)
        if (!res.ok && res.error && res.error.includes("132000")) {
            functions.logger.warn("WhatsApp 132000 parameter mismatch, auto-retrying with 0 parameters (static template)...");
            const retryRes0 = await (0, whatsapp_1.sendWaTemplate)(formatted, template, lang, []);
            if (retryRes0.ok) {
                res = retryRes0;
                params = [];
            }
            else if (params.length !== 1) {
                functions.logger.warn("WhatsApp 132000 parameter mismatch, auto-retrying with 1 parameter (name only)...");
                const retryRes1 = await (0, whatsapp_1.sendWaTemplate)(formatted, template, lang, [defaultName]);
                if (retryRes1.ok) {
                    res = retryRes1;
                    params = [defaultName];
                }
            }
        }
        if (!res.ok) {
            (0, input_1.fail)("internal", "WhatsApp send failed: " + (res.error || "unknown error"));
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
    function isTeluguStateEmployer(stateText, cityText, addrText) {
        const combined = `${stateText || ""} ${cityText || ""} ${addrText || ""}`.toLowerCase();
        for (const nonTe of NON_TELUGU_IDENTIFIERS) {
            if (combined.includes(nonTe))
                return false;
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
    function isTelanganaProfile(cityText, addrText) {
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
    function extractCleanCity(area, address, fallback = "Khammam") {
        const combined = `${area || ""} ${address || ""}`.trim();
        if (!combined)
            return fallback;
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
    const targetMap = new Map();
    if (customPhones.length > 0) {
        for (const rawPhone of customPhones) {
            let cleaned = rawPhone.replace(/\D/g, "");
            if (cleaned.length === 10)
                cleaned = "91" + cleaned;
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
    }
    else {
        // 1. Fetch employers if requested
        if (audienceTarget === "EMPLOYERS" || audienceTarget === "BOTH") {
            const empSnap = await db.collection(schema_1.EmployerProfiles.COLLECTION).limit(1000).get();
            for (const d of empSnap.docs) {
                const phone = String(d.get(schema_1.EmployerProfiles.PHONE) || "").trim();
                const empName = String(d.get(schema_1.EmployerProfiles.OWNER_NAME) || d.get(schema_1.EmployerProfiles.BUSINESS_NAME) || "").trim() || defaultName;
                const area = String(d.get(schema_1.EmployerProfiles.AREA) || "").trim();
                const addr = String(d.get(schema_1.EmployerProfiles.ADDRESS) || "").trim();
                const empCity = extractCleanCity(area, addr, defaultCity);
                const empState = String(d.get("state") || d.get("location.state") || "").trim();
                const isTe = isTeluguStateEmployer(empState, empCity, addr);
                let cleaned = phone.replace(/\D/g, "");
                if (cleaned.length === 10)
                    cleaned = "91" + cleaned;
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
                const jobSnap = await db.collection(schema_1.Jobs.COLLECTION).where(schema_1.Jobs.STATE, "==", "Telangana").limit(400).get();
                const contactsSnap = await db.collection(schema_1.JobContacts.COLLECTION).limit(500).get();
                const contactsMap = new Map();
                contactsSnap.docs.forEach((cd) => {
                    contactsMap.set(cd.id, String(cd.data()[schema_1.JobContacts.CONTACT_NUMBER] || ""));
                });
                for (const jd of jobSnap.docs) {
                    const jdata = jd.data();
                    const rawPhone = contactsMap.get(jd.id);
                    if (rawPhone) {
                        let cleaned = String(rawPhone).replace(/\D/g, "");
                        if (cleaned.length === 10)
                            cleaned = "91" + cleaned;
                        const formatted = "+" + cleaned;
                        if (/^\+91[6-9]\d{9}$/.test(formatted)) {
                            if (!targetMap.has(formatted)) {
                                const name = String(jdata[schema_1.Jobs.COMPANY_NAME] || defaultName).trim();
                                const city = String(jdata[schema_1.Jobs.DISTRICT] || jdata[schema_1.Jobs.AREA] || "Telangana").trim();
                                targetMap.set(formatted, {
                                    phone: formatted,
                                    name,
                                    city,
                                    rawLocation: String(jdata[schema_1.Jobs.AREA] || city).trim(),
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
            const workerSnap = await db.collection(schema_1.WorkerProfiles.COLLECTION).limit(1000).get();
            for (const d of workerSnap.docs) {
                const phone = String(d.get(schema_1.WorkerProfiles.PHONE) || d.get("mobile") || "").trim();
                const workerName = String(d.get(schema_1.WorkerProfiles.NAME) || "").trim() || "Candidate";
                const area = String(d.get(schema_1.WorkerProfiles.AREA) || "").trim();
                const addr = String(d.get(schema_1.WorkerProfiles.ADDRESS) || "").trim();
                const workerCity = extractCleanCity(area, addr, defaultCity);
                const workerState = String(d.get("state") || d.get("location.state") || "").trim();
                const isTe = isTeluguStateEmployer(workerState, workerCity, addr);
                let cleaned = phone.replace(/\D/g, "");
                if (cleaned.length === 10)
                    cleaned = "91" + cleaned;
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
        allTargets = allTargets.filter((t) => t.city.toLowerCase() === needle || t.rawLocation.toLowerCase().includes(needle));
    }
    // Apply Region / Language filtering based on routingMode
    if (routingMode === "TELUGU_ONLY") {
        allTargets = allTargets.filter((t) => t.isTeluguState);
    }
    else if (routingMode === "ENGLISH_ONLY") {
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
        return Object.assign(Object.assign({}, summary), { sample: targets.slice(0, 8) });
    }
    if (!targets.length) {
        return { dryRun: false, matched: 0, sent: 0, failed: 0, errors: [] };
    }
    let sent = 0;
    let failed = 0;
    const errors = [];
    // Send in batches of 10
    for (let i = 0; i < targets.length; i += 10) {
        await Promise.all(targets.slice(i, i + 10).map(async (t) => {
            // Determine language and template for this recipient
            let recipientTemplate = template;
            let recipientLang = lang;
            if (routingMode === "AUTO") {
                if (t.isTeluguState) {
                    recipientTemplate = template;
                    recipientLang = "te";
                }
                else {
                    recipientTemplate = englishTemplate || template;
                    recipientLang = "en";
                }
            }
            else if (routingMode === "ALL_TELUGU") {
                recipientTemplate = template;
                recipientLang = "te";
            }
            else if (routingMode === "ALL_ENGLISH") {
                recipientTemplate = englishTemplate || template;
                recipientLang = "en";
            }
            let params;
            if (Array.isArray(data.params)) {
                if (data.params.length === 0) {
                    params = [];
                }
                else if (data.params.length === 1) {
                    params = [t.name];
                }
                else {
                    params = [t.name, t.city, ...(data.params.slice(2))];
                }
            }
            else {
                params = [t.name, t.city];
            }
            let r = await (0, whatsapp_1.sendWaTemplate)(t.phone, recipientTemplate, recipientLang, params);
            if (!r.ok && r.error && r.error.includes("132000")) {
                // Auto retry with 0 params or 1 param
                const retry0 = await (0, whatsapp_1.sendWaTemplate)(t.phone, recipientTemplate, recipientLang, []);
                if (retry0.ok) {
                    r = retry0;
                }
                else {
                    const retry1 = await (0, whatsapp_1.sendWaTemplate)(t.phone, recipientTemplate, recipientLang, [t.name]);
                    if (retry1.ok)
                        r = retry1;
                }
            }
            if (r.ok) {
                sent++;
            }
            else {
                failed++;
                if (errors.length < 5 && r.error)
                    errors.push(`${t.phone}: ${r.error}`);
            }
        }));
        // Abort early if the first batch fails completely (template error or bad auth)
        if (i === 0 && sent === 0 && failed > 0)
            break;
    }
    await db.collection(schema_1.WhatsappCampaigns.COLLECTION).add({
        [schema_1.WhatsappCampaigns.TEMPLATE]: template,
        [schema_1.WhatsappCampaigns.LANGS]: [lang],
        [schema_1.WhatsappCampaigns.PARAMS]: [defaultName, defaultCity],
        [schema_1.WhatsappCampaigns.AUDIENCE]: "EMPLOYER_BROADCAST",
        [schema_1.WhatsappCampaigns.MATCHED]: targets.length,
        [schema_1.WhatsappCampaigns.SENT]: sent,
        [schema_1.WhatsappCampaigns.FAILED]: failed,
        [schema_1.WhatsappCampaigns.BY]: context.auth.uid,
        [schema_1.WhatsappCampaigns.CREATED_AT]: Timestamp.now()
    });
    return { dryRun: false, matched: targets.length, sent, failed, errors };
});
/**
 * Admin: Aggregates verified Telangana employers from both registered employer profiles
 * and active verified Telangana job listings into a single de-duplicated audience.
 */
exports.fetchTelanganaEmployers = (0, secure_callable_1.onCallSecured)({ enforceAppCheck: false, timeoutSeconds: 60, memory: "256MB" }, async (_raw, context) => {
    if (!(await (0, app_config_1.isCallerAdmin)(context)))
        (0, input_1.fail)("permission-denied", "Admins only");
    const TG_IDENTIFIERS = [
        "telangana", "khammam", "hyderabad", "secunderabad", "warangal", "hanumakonda", "kazipet",
        "karimnagar", "nizamabad", "nalgonda", "suryapet", "mahabubnagar", "siddipet", "mancherial",
        "kothagudem", "bhadradri", "medchal", "malkajgiri", "ranga reddy", "rangareddy", "sangareddy",
        "jagitial", "kamareddy", "peddapalli", "sircilla", "wanaparthy", "gadwal", "nagarkurnool",
        "vikarabad", "asifabad", "adilabad", "mulugu", "narayanpet", "abids", "tellapur", "yella reddy guda",
        "ameerpet", "madhapur", "gachibowli", "kukatpally", "dilsukhnagar", "lb nagar", "uppal",
        "begumpet", "kondapur", "hitec city", "jubilee hills", "banjara hills", "wyra", "sathupally", "madhira"
    ];
    const employersMap = new Map();
    // 1. Registered employer_profiles
    const empSnap = await db.collection(schema_1.EmployerProfiles.COLLECTION).limit(1000).get();
    for (const d of empSnap.docs) {
        const data = d.data();
        const phone = String(data[schema_1.EmployerProfiles.PHONE] || "").trim();
        if (!phone)
            continue;
        const combined = `${data[schema_1.EmployerProfiles.AREA] || ""} ${data[schema_1.EmployerProfiles.ADDRESS] || ""} ${data.state || ""} ${data.city || ""} ${data.district || ""}`.toLowerCase();
        const isTg = TG_IDENTIFIERS.some((k) => combined.includes(k));
        if (isTg) {
            let cleaned = phone.replace(/\D/g, "");
            if (cleaned.length === 10)
                cleaned = "91" + cleaned;
            if (cleaned.length === 12 && cleaned.startsWith("91")) {
                const formatted = "+" + cleaned;
                const name = String(data[schema_1.EmployerProfiles.BUSINESS_NAME] || data[schema_1.EmployerProfiles.OWNER_NAME] || "Business Owner").trim();
                const city = String(data.city || data[schema_1.EmployerProfiles.AREA] || "Telangana").trim();
                employersMap.set(cleaned, {
                    phone: formatted,
                    name,
                    city,
                    area: String(data[schema_1.EmployerProfiles.AREA] || city).trim(),
                    source: "employer_profile"
                });
            }
        }
    }
    // 2. Active verified Telangana job posters
    const jobSnap = await db.collection(schema_1.Jobs.COLLECTION).where(schema_1.Jobs.STATE, "==", "Telangana").limit(400).get();
    const contactsSnap = await db.collection(schema_1.JobContacts.COLLECTION).limit(500).get();
    const contactsMap = new Map();
    contactsSnap.docs.forEach((cd) => {
        contactsMap.set(cd.id, String(cd.data()[schema_1.JobContacts.CONTACT_NUMBER] || ""));
    });
    for (const jd of jobSnap.docs) {
        const jdata = jd.data();
        const rawPhone = contactsMap.get(jd.id);
        if (rawPhone) {
            let cleaned = String(rawPhone).replace(/\D/g, "");
            if (cleaned.length === 10)
                cleaned = "91" + cleaned;
            if (cleaned.length === 12 && cleaned.startsWith("91")) {
                if (!employersMap.has(cleaned)) {
                    const formatted = "+" + cleaned;
                    const name = String(jdata[schema_1.Jobs.COMPANY_NAME] || "Business Owner").trim();
                    const city = String(jdata[schema_1.Jobs.DISTRICT] || jdata[schema_1.Jobs.AREA] || "Telangana").trim();
                    employersMap.set(cleaned, {
                        phone: formatted,
                        name,
                        city,
                        area: String(jdata[schema_1.Jobs.AREA] || city).trim(),
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
});
//# sourceMappingURL=whatsapp-messages.js.map