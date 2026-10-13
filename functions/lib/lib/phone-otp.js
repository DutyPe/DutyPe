"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MAX_PER_IP_HOUR = exports.MAX_PER_DAY = exports.MAX_PER_HOUR = exports.MAX_RESEND_GAP_MS = exports.RESEND_GAP_MS = exports.MAX_VERIFY_ATTEMPTS = exports.CODE_MAX_LIFE_MS = exports.CODE_TTL_MS = void 0;
exports.indianE164 = indianE164;
exports.newCode = newCode;
exports.hashCode = hashCode;
exports.codeMatches = codeMatches;
exports.istDayKey = istDayKey;
exports.resendGapMs = resendGapMs;
exports.decideSend = decideSend;
exports.reuseCode = reuseCode;
exports.resentExpiry = resentExpiry;
exports.sealCode = sealCode;
exports.openCode = openCode;
exports.ipKey = ipKey;
exports.whatsappConfigured = whatsappConfigured;
exports.whatsappTemplateBody = whatsappTemplateBody;
exports.parseTruecallerUserInfo = parseTruecallerUserInfo;
/**
 * Pure helpers for WhatsApp login codes (no Firebase, unit tested).
 */
const crypto_1 = require("crypto");
/**
 * Login-code rules (like Twilio Verify / bank OTPs):
 *  - A code is valid for 10 minutes. "Resend" within that time sends the SAME code again (so a user
 *    who left the app for a few minutes can still type the first code) and gives it 10 more
 *    minutes, but a code never lives longer than 30 minutes from when it was made.
 *  - Waits between sends grow: 30 s, 60 s, 2 min, then 5 min. At most 5 codes an hour, 10 a day per number.
 *  - 5 wrong tries lock the code (a resend does not reset the tries); the next request makes a new code.
 *  - One internet address (IP) can request at most 20 codes an hour (stops SMS-pumping bots).
 */
exports.CODE_TTL_MS = 10 * 60 * 1000;
exports.CODE_MAX_LIFE_MS = 30 * 60 * 1000;
exports.MAX_VERIFY_ATTEMPTS = 5;
exports.RESEND_GAP_MS = 30 * 1000;
exports.MAX_RESEND_GAP_MS = 5 * 60 * 1000;
exports.MAX_PER_HOUR = 5;
exports.MAX_PER_DAY = 10;
exports.MAX_PER_IP_HOUR = 20;
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
/** Wait before the next send, growing with the codes sent this hour: 30 s, 60 s, 2 min, 4→5 min. */
function resendGapMs(sentThisHour) {
    return Math.min(exports.RESEND_GAP_MS * 2 ** Math.max(0, sentThisHour - 1), exports.MAX_RESEND_GAP_MS);
}
/** Per-number limits: growing wait between codes, 5 an hour, 10 a day. */
function decideSend(prev, nowMs) {
    const last = Number((prev === null || prev === void 0 ? void 0 : prev.lastSentAt) || 0);
    const sameHour = (prev === null || prev === void 0 ? void 0 : prev.hourStart) && nowMs - Number(prev.hourStart) < HOUR_MS;
    const gap = resendGapMs(sameHour ? Number((prev === null || prev === void 0 ? void 0 : prev.hourCount) || 0) : 0);
    if (last && nowMs - last < gap) {
        return { ok: false, reason: "wait", retryAfterSec: Math.ceil((gap - (nowMs - last)) / 1000) };
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
/**
 * Resend within the window: reuse the code (true) unless it expired, was used, got locked by wrong
 * tries, or is older than [CODE_MAX_LIFE_MS].
 */
function reuseCode(prev, nowMs) {
    if (!prev || !prev.hasCode)
        return false;
    return prev.expiresAt > nowMs && nowMs - prev.issuedAt < exports.CODE_MAX_LIFE_MS && prev.attempts < exports.MAX_VERIFY_ATTEMPTS;
}
/** New expiry for a resent code: 10 more minutes, never past 30 minutes from issue. */
function resentExpiry(issuedAt, nowMs) {
    return Math.min(nowMs + exports.CODE_TTL_MS, issuedAt + exports.CODE_MAX_LIFE_MS);
}
/**
 * The code itself is kept encrypted (AES-256-GCM) only so a resend can send the same digits; the
 * check uses the hash. Key: OTP_SECRET (functions env), else the WhatsApp token, else the project.
 */
function otpKey(env) {
    const material = (env.OTP_SECRET || env.WHATSAPP_TOKEN || env.GCLOUD_PROJECT || "dutype").trim();
    return (0, crypto_1.createHash)("sha256").update(`dutype-otp:${material}`).digest();
}
function sealCode(code, env = process.env) {
    const iv = (0, crypto_1.randomBytes)(12);
    const c = (0, crypto_1.createCipheriv)("aes-256-gcm", otpKey(env), iv);
    const enc = Buffer.concat([c.update(code, "utf8"), c.final()]);
    return [iv, c.getAuthTag(), enc].map((b) => b.toString("base64")).join(".");
}
function openCode(sealed, env = process.env) {
    try {
        const [iv, tag, enc] = String(sealed || "").split(".").map((x) => Buffer.from(x, "base64"));
        const d = (0, crypto_1.createDecipheriv)("aes-256-gcm", otpKey(env), iv);
        d.setAuthTag(tag);
        const code = Buffer.concat([d.update(enc), d.final()]).toString("utf8");
        return /^\d{6}$/.test(code) ? code : null;
    }
    catch (_a) {
        return null;
    }
}
/** Short, non-reversible key for an IP address (we never store raw IPs). */
function ipKey(ip, nowMs) {
    const hour = Math.floor(nowMs / HOUR_MS);
    return (0, crypto_1.createHash)("sha256").update(`ip:${ip}`).digest("hex").slice(0, 20) + `_${hour}`;
}
/** WhatsApp is set up when the token secret holds a real value (a placeholder like "unset" is not). */
function whatsappConfigured(env = process.env) {
    const token = (env.WHATSAPP_TOKEN || "").trim();
    return Boolean(token && token.toLowerCase() !== "unset" && env.WHATSAPP_PHONE_NUMBER_ID && env.WHATSAPP_TEMPLATE);
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
/** The fields DutyPe uses from Truecaller's userinfo response. */
function parseTruecallerUserInfo(raw) {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j, _k;
    const u = (raw && typeof raw === "object" ? raw : {});
    const phone = indianE164((_a = u.phone_number) !== null && _a !== void 0 ? _a : u.phoneNumber) || "";
    const name = [u.given_name, u.family_name].map((v) => String(v !== null && v !== void 0 ? v : "").trim()).filter(Boolean).join(" ") ||
        String((_b = u.name) !== null && _b !== void 0 ? _b : "").trim();
    const email = String((_c = u.email) !== null && _c !== void 0 ? _c : "").trim().toLowerCase();
    const rawPic = String((_f = (_e = (_d = u.picture) !== null && _d !== void 0 ? _d : u.avatar_url) !== null && _e !== void 0 ? _e : u.picture_url) !== null && _f !== void 0 ? _f : "").trim();
    const picture = /^https?:\/\//i.test(rawPic) ? rawPic : "";
    const genderRaw = String((_g = u.gender) !== null && _g !== void 0 ? _g : "").trim().toUpperCase();
    const gender = ["MALE", "FEMALE", "OTHER"].includes(genderRaw) ? genderRaw : "";
    const city = String((_k = (_h = u.city) !== null && _h !== void 0 ? _h : (_j = u.address) === null || _j === void 0 ? void 0 : _j.city) !== null && _k !== void 0 ? _k : "").trim().slice(0, 50);
    return {
        phone,
        name: name.replace(/\s+/g, " ").slice(0, 80),
        email: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email) && email.length <= 120 ? email : "",
        picture,
        gender,
        city,
    };
}
//# sourceMappingURL=phone-otp.js.map