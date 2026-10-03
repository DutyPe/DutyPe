"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.sendWhatsappPromo = exports.whatsappPromos = void 0;
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
//# sourceMappingURL=whatsapp-messages.js.map