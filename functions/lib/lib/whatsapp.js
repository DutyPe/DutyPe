"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PROMO_MAX_PER_RUN = exports.PROMO_GAP_DAYS_DEFAULT = exports.waNet = exports.WHATSAPP_TOKEN_SECRET = void 0;
exports.waParam = waParam;
exports.waLang = waLang;
exports.langList = langList;
exports.waTextTemplateBody = waTextTemplateBody;
exports.sendWaTemplate = sendWaTemplate;
exports.waReady = waReady;
exports.serviceTemplates = serviceTemplates;
exports.promoDue = promoDue;
exports.optOutNumbers = optOutNumbers;
/**
 * WhatsApp business messages besides the login code (Meta Cloud API, same number and token):
 *
 *   • Service updates (UTILITY templates, ~₹0.13 each) — only for DutyPe Services: when a partner
 *     accepts a booking, the customer gets the partner's name and number and the partner gets the
 *     customer's details. Urgent work and vacancies stay on free push notifications (a WhatsApp blast
 *     to 100 nearby workers is a MARKETING message, ~₹1 each, and spam reports can block the number).
 *   • Offers (MARKETING templates) — only to people who switched on "WhatsApp offers" in Settings,
 *     at most one every 7 days, and replying STOP switches it off.
 *
 * Settings (functions/.env.dutype-860ac): WHATSAPP_SERVICE_CUSTOMER_TEMPLATE,
 * WHATSAPP_SERVICE_PARTNER_TEMPLATE, WHATSAPP_SERVICE_LANGS (e.g. "en,te"), WHATSAPP_UTILITY_DAILY_CAP,
 * WHATSAPP_PROMO_DAILY_CAP, WHATSAPP_PROMO_GAP_DAYS. Nothing is sent while a template is not set.
 */
const phone_otp_1 = require("./phone-otp");
exports.WHATSAPP_TOKEN_SECRET = "WHATSAPP_TOKEN";
/** Test seam: replaced in tests so no network call is made. */
exports.waNet = {
    fetch: (url, init) => fetch(url, init),
};
/** A template parameter: Meta rejects new lines, tabs and 4+ spaces in a row, and empty text. */
function waParam(v, max = 120) {
    const s = String(v !== null && v !== void 0 ? v : "").replace(/[\r\n\t]+/g, " ").replace(/ {2,}/g, " ").trim();
    return (s.length > max ? s.slice(0, max - 1) + "…" : s) || "-";
}
/** The template language for a user: their app language when that translation is approved, else the first. */
function waLang(locale, approved) {
    const want = String(locale || "").toLowerCase().slice(0, 2);
    return approved.includes(want) ? want : (approved[0] || "en");
}
function langList(raw, fallback = "en") {
    const out = String(raw || fallback).split(",").map((s) => s.trim().toLowerCase()).filter((s) => /^[a-z]{2}(_[A-Z]{2})?$/i.test(s));
    return out.length ? out : [fallback];
}
/** Meta Cloud API body for a template with plain text body parameters. */
function waTextTemplateBody(toE164, template, lang, params) {
    return {
        messaging_product: "whatsapp",
        to: toE164.replace(/^\+/, ""),
        type: "template",
        template: Object.assign({ name: template, language: { code: lang } }, (params.length ? {
            components: [{ type: "body", parameters: params.map((p) => ({ type: "text", text: waParam(p) })) }],
        } : {})),
    };
}
/** Sends one template message. True when Meta accepted it. */
async function sendWaTemplate(toE164, template, lang, params, env = process.env) {
    if (!/^\+91[6-9]\d{9}$/.test(toE164))
        return { ok: false, error: "bad_phone" };
    const version = env.WHATSAPP_API_VERSION || "v21.0";
    const url = `https://graph.facebook.com/${version}/${env.WHATSAPP_PHONE_NUMBER_ID}/messages`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 8000);
    try {
        const res = await exports.waNet.fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Authorization": `Bearer ${(env.WHATSAPP_TOKEN || "").trim()}` },
            body: JSON.stringify(waTextTemplateBody(toE164, template, lang, params)),
            signal: controller.signal,
        });
        if (res.ok)
            return { ok: true };
        return { ok: false, error: `${res.status} ${(await res.text()).slice(0, 200)}` };
    }
    catch (e) {
        return { ok: false, error: String((e === null || e === void 0 ? void 0 : e.message) || e).slice(0, 200) };
    }
    finally {
        clearTimeout(timer);
    }
}
/** WhatsApp can send business messages (token + phone number id present). */
function waReady(env = process.env) {
    // whatsappConfigured also needs the login template; business messages only need the number.
    return (0, phone_otp_1.whatsappConfigured)(Object.assign(Object.assign({}, env), { WHATSAPP_TEMPLATE: env.WHATSAPP_TEMPLATE || "x" }));
}
/** Service-update templates, or null when WhatsApp or the templates are not set up. */
function serviceTemplates(env = process.env) {
    const customer = (env.WHATSAPP_SERVICE_CUSTOMER_TEMPLATE || "").trim();
    const partner = (env.WHATSAPP_SERVICE_PARTNER_TEMPLATE || "").trim();
    if (!waReady(env) || (!customer && !partner))
        return null;
    return { customer, partner, langs: langList(env.WHATSAPP_SERVICE_LANGS) };
}
exports.PROMO_GAP_DAYS_DEFAULT = 7;
/** Meta's starting limit is 1,000 people a day for business-started messages (login codes count too). */
exports.PROMO_MAX_PER_RUN = 1000;
/** True when this person may get another offer now (at most one every [gapDays] days). */
function promoDue(lastPromoMs, nowMs, gapDays) {
    return !lastPromoMs || nowMs - lastPromoMs >= gapDays * 24 * 60 * 60 * 1000;
}
/** Words that switch offers off when someone replies on WhatsApp (English, Telugu, Hindi). */
const STOP_WORDS = ["stop", "unsubscribe", "stop promotions", "ఆపు", "ఆపండి", "बंद", "रोकें", "रुको"];
/**
 * Numbers (E.164) that asked to stop offers in a webhook payload: a text reply such as "STOP" or
 * the template's "Stop promotions" quick-reply button.
 */
function optOutNumbers(body) {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k;
    const out = new Set();
    const entries = body === null || body === void 0 ? void 0 : body.entry;
    if (!Array.isArray(entries))
        return [];
    for (const entry of entries) {
        for (const change of ((entry === null || entry === void 0 ? void 0 : entry.changes) || [])) {
            const messages = (_a = change === null || change === void 0 ? void 0 : change.value) === null || _a === void 0 ? void 0 : _a.messages;
            if (!Array.isArray(messages))
                continue;
            for (const m of messages) {
                const said = String((_k = (_g = (_e = (_c = (_b = m === null || m === void 0 ? void 0 : m.text) === null || _b === void 0 ? void 0 : _b.body) !== null && _c !== void 0 ? _c : (_d = m === null || m === void 0 ? void 0 : m.button) === null || _d === void 0 ? void 0 : _d.text) !== null && _e !== void 0 ? _e : (_f = m === null || m === void 0 ? void 0 : m.button) === null || _f === void 0 ? void 0 : _f.payload) !== null && _g !== void 0 ? _g : (_j = (_h = m === null || m === void 0 ? void 0 : m.interactive) === null || _h === void 0 ? void 0 : _h.button_reply) === null || _j === void 0 ? void 0 : _j.title) !== null && _k !== void 0 ? _k : "").trim().toLowerCase();
                const from = String((m === null || m === void 0 ? void 0 : m.from) || "").replace(/\D/g, "");
                if (!said || !/^91[6-9]\d{9}$/.test(from))
                    continue;
                if (STOP_WORDS.includes(said))
                    out.add(`+${from}`);
            }
        }
    }
    return [...out];
}
//# sourceMappingURL=whatsapp.js.map