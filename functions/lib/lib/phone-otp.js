"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MAX_PER_DAY = exports.MAX_PER_HOUR = exports.RESEND_GAP_MS = exports.MAX_VERIFY_ATTEMPTS = exports.CODE_TTL_MS = void 0;
exports.indianE164 = indianE164;
exports.newCode = newCode;
exports.hashCode = hashCode;
exports.codeMatches = codeMatches;
exports.istDayKey = istDayKey;
exports.decideSend = decideSend;
exports.parseTruecallerUserInfo = parseTruecallerUserInfo;
exports.whatsappConfigured = whatsappConfigured;
exports.smsGatewayConfigured = smsGatewayConfigured;
exports.smsGatewayRequest = smsGatewayRequest;
exports.whatsappTemplateBody = whatsappTemplateBody;
/**
 * Pure helpers for WhatsApp login codes and Truecaller profiles (no Firebase, unit tested).
 */
const crypto_1 = require("crypto");
exports.CODE_TTL_MS = 10 * 60 * 1000;
exports.MAX_VERIFY_ATTEMPTS = 5;
exports.RESEND_GAP_MS = 30 * 1000;
exports.MAX_PER_HOUR = 5;
exports.MAX_PER_DAY = 10;
const HOUR_MS = 60 * 60 * 1000;
const IST_OFFSET_MS = 330 * 60 * 1000;
/** "+91XXXXXXXXXX" for an Indian mobile in any common form, else null. */
function indianE164(raw) {
    const digits = String(raw !== null && raw !== void 0 ? raw : "").replace(/\D/g, "");
    if (/^[6-9]\d{9}$/.test(digits))
        return `+91${digits}`;
    if (/^91[6-9]\d{9}$/.test(digits))
        return `+${digits}`;
    if (/^0[6-9]\d{9}$/.test(digits))
        return `+91${digits.slice(1)}`;
    return null;
}
function newCode() {
    return String((0, crypto_1.randomInt)(0, 1000000)).padStart(6, "0");
}
function hashCode(phone, code) {
    return (0, crypto_1.createHash)("sha256").update(`${phone}:${code}`).digest("hex");
}
function codeMatches(phone, code, storedHash) {
    const a = Buffer.from(hashCode(phone, code), "hex");
    const b = Buffer.from(String(storedHash || ""), "hex");
    return a.length === b.length && (0, crypto_1.timingSafeEqual)(a, b);
}
/** IST calendar day, e.g. "2026-10-02". */
function istDayKey(nowMs) {
    return new Date(nowMs + IST_OFFSET_MS).toISOString().slice(0, 10);
}
/** Per-number limits: one code every 30 s, 5 an hour, 10 a day. */
function decideSend(prev, nowMs) {
    const last = Number((prev === null || prev === void 0 ? void 0 : prev.lastSentAt) || 0);
    if (last && nowMs - last < exports.RESEND_GAP_MS) {
        return { ok: false, reason: "wait", retryAfterSec: Math.ceil((exports.RESEND_GAP_MS - (nowMs - last)) / 1000) };
    }
    const day = istDayKey(nowMs);
    const dayCount = (prev === null || prev === void 0 ? void 0 : prev.dayKey) === day ? Number((prev === null || prev === void 0 ? void 0 : prev.dayCount) || 0) : 0;
    if (dayCount >= exports.MAX_PER_DAY)
        return { ok: false, reason: "day", retryAfterSec: 6 * 60 * 60 };
    const hourStart = (prev === null || prev === void 0 ? void 0 : prev.hourStart) && nowMs - Number(prev.hourStart) < HOUR_MS ? Number(prev.hourStart) : nowMs;
    const hourCount = hourStart === Number(prev === null || prev === void 0 ? void 0 : prev.hourStart) ? Number((prev === null || prev === void 0 ? void 0 : prev.hourCount) || 0) : 0;
    if (hourCount >= exports.MAX_PER_HOUR) {
        return { ok: false, reason: "hour", retryAfterSec: Math.ceil((hourStart + HOUR_MS - nowMs) / 1000) };
    }
    return { ok: true, next: { lastSentAt: nowMs, hourStart, hourCount: hourCount + 1, dayKey: day, dayCount: dayCount + 1 } };
}
/** The fields DutyPe uses from Truecaller's userinfo response. */
function parseTruecallerUserInfo(raw) {
    var _a, _b, _c;
    const u = (raw && typeof raw === "object" ? raw : {});
    const phone = indianE164((_a = u.phone_number) !== null && _a !== void 0 ? _a : u.phoneNumber);
    const name = [u.given_name, u.family_name].map((v) => String(v !== null && v !== void 0 ? v : "").trim()).filter(Boolean).join(" ") ||
        String((_b = u.name) !== null && _b !== void 0 ? _b : "").trim();
    const email = String((_c = u.email) !== null && _c !== void 0 ? _c : "").trim().toLowerCase();
    return {
        phone,
        name: name.replace(/\s+/g, " ").slice(0, 80),
        email: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email) && email.length <= 120 ? email : "",
    };
}
/** WhatsApp is set up when the token secret holds a real value (a placeholder like "unset" is not). */
function whatsappConfigured(env = process.env) {
    const token = (env.WHATSAPP_TOKEN || "").trim();
    return Boolean(token && token.toLowerCase() !== "unset" && env.WHATSAPP_PHONE_NUMBER_ID && env.WHATSAPP_TEMPLATE);
}
/**
 * Our own SMS gateway (DLT-registered OTP template), ~₹0.15–0.25 per SMS instead of Firebase's
 * ~₹6.7. SMS_PROVIDER = "2factor" (SMS_API_KEY, SMS_TEMPLATE = the 2Factor template name) or
 * "msg91" (SMS_API_KEY = authkey, SMS_TEMPLATE = MSG91 template id).
 */
function smsGatewayConfigured(env = process.env) {
    const provider = (env.SMS_PROVIDER || "").toLowerCase();
    return (provider === "2factor" || provider === "msg91") && Boolean((env.SMS_API_KEY || "").trim()) &&
        Boolean((env.SMS_TEMPLATE || "").trim());
}
/** The HTTP request for the configured gateway (pure, so it can be tested). */
function smsGatewayRequest(toE164, code, env = process.env) {
    const ten = toE164.replace(/^\+91/, "");
    const key = (env.SMS_API_KEY || "").trim();
    const template = (env.SMS_TEMPLATE || "").trim();
    if ((env.SMS_PROVIDER || "").toLowerCase() === "msg91") {
        const q = new URLSearchParams({ template_id: template, mobile: `91${ten}`, otp: code });
        return { url: `https://control.msg91.com/api/v5/otp?${q}`, init: { method: "POST", headers: { authkey: key, "Content-Type": "application/json" } } };
    }
    return {
        url: `https://2factor.in/API/V1/${encodeURIComponent(key)}/SMS/${ten}/${code}/${encodeURIComponent(template)}`,
        init: { method: "GET" },
    };
}
/** Meta Cloud API body for an authentication template (body code + copy-code button). */
function whatsappTemplateBody(toE164, code, template, lang) {
    return {
        messaging_product: "whatsapp",
        to: toE164.replace(/^\+/, ""),
        type: "template",
        template: {
            name: template,
            language: { code: lang },
            components: [
                { type: "body", parameters: [{ type: "text", text: code }] },
                { type: "button", sub_type: "url", index: "0", parameters: [{ type: "text", text: code }] },
            ],
        },
    };
}
//# sourceMappingURL=phone-otp.js.map