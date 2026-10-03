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
import { whatsappConfigured } from "./phone-otp";

export const WHATSAPP_TOKEN_SECRET = "WHATSAPP_TOKEN";

/** Test seam: replaced in tests so no network call is made. */
export const waNet = {
  fetch: (url: string, init?: RequestInit): Promise<Response> => fetch(url, init),
};

/** A template parameter: Meta rejects new lines, tabs and 4+ spaces in a row, and empty text. */
export function waParam(v: unknown, max = 120): string {
  const s = String(v ?? "").replace(/[\r\n\t]+/g, " ").replace(/ {2,}/g, " ").trim();
  return (s.length > max ? s.slice(0, max - 1) + "…" : s) || "-";
}

/** The template language for a user: their app language when that translation is approved, else the first. */
export function waLang(locale: unknown, approved: string[]): string {
  const want = String(locale || "").toLowerCase().slice(0, 2);
  return approved.includes(want) ? want : (approved[0] || "en");
}

export function langList(raw: unknown, fallback = "en"): string[] {
  const out = String(raw || fallback).split(",").map((s) => s.trim().toLowerCase()).filter((s) => /^[a-z]{2}(_[A-Z]{2})?$/i.test(s));
  return out.length ? out : [fallback];
}

/** Meta Cloud API body for a template with plain text body parameters. */
export function waTextTemplateBody(toE164: string, template: string, lang: string, params: unknown[]) {
  return {
    messaging_product: "whatsapp",
    to: toE164.replace(/^\+/, ""),
    type: "template",
    template: {
      name: template,
      language: { code: lang },
      ...(params.length ? {
        components: [{ type: "body", parameters: params.map((p) => ({ type: "text", text: waParam(p) })) }],
      } : {}),
    },
  };
}

/** Sends one template message. True when Meta accepted it. */
export async function sendWaTemplate(toE164: string, template: string, lang: string, params: unknown[],
  env: NodeJS.ProcessEnv = process.env): Promise<{ ok: boolean; error?: string }> {
  if (!/^\+91[6-9]\d{9}$/.test(toE164)) return { ok: false, error: "bad_phone" };
  const version = env.WHATSAPP_API_VERSION || "v21.0";
  const url = `https://graph.facebook.com/${version}/${env.WHATSAPP_PHONE_NUMBER_ID}/messages`;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 8_000);
  try {
    const res = await waNet.fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${(env.WHATSAPP_TOKEN || "").trim()}` },
      body: JSON.stringify(waTextTemplateBody(toE164, template, lang, params)),
      signal: controller.signal,
    });
    if (res.ok) return { ok: true };
    return { ok: false, error: `${res.status} ${(await res.text()).slice(0, 200)}` };
  } catch (e) {
    return { ok: false, error: String((e as Error)?.message || e).slice(0, 200) };
  } finally {
    clearTimeout(timer);
  }
}

/** WhatsApp can send business messages (token + phone number id present). */
export function waReady(env: NodeJS.ProcessEnv = process.env): boolean {
  // whatsappConfigured also needs the login template; business messages only need the number.
  return whatsappConfigured({ ...env, WHATSAPP_TEMPLATE: env.WHATSAPP_TEMPLATE || "x" });
}

export interface ServiceTemplates {
  customer: string;
  partner: string;
  langs: string[];
}

/** Service-update templates, or null when WhatsApp or the templates are not set up. */
export function serviceTemplates(env: NodeJS.ProcessEnv = process.env): ServiceTemplates | null {
  const customer = (env.WHATSAPP_SERVICE_CUSTOMER_TEMPLATE || "").trim();
  const partner = (env.WHATSAPP_SERVICE_PARTNER_TEMPLATE || "").trim();
  if (!waReady(env) || (!customer && !partner)) return null;
  return { customer, partner, langs: langList(env.WHATSAPP_SERVICE_LANGS) };
}

export const PROMO_GAP_DAYS_DEFAULT = 7;
/** Meta's starting limit is 1,000 people a day for business-started messages (login codes count too). */
export const PROMO_MAX_PER_RUN = 1000;

/** True when this person may get another offer now (at most one every [gapDays] days). */
export function promoDue(lastPromoMs: number, nowMs: number, gapDays: number): boolean {
  return !lastPromoMs || nowMs - lastPromoMs >= gapDays * 24 * 60 * 60 * 1000;
}

/** Words that switch offers off when someone replies on WhatsApp (English, Telugu, Hindi). */
const STOP_WORDS = ["stop", "unsubscribe", "stop promotions", "ఆపు", "ఆపండి", "बंद", "रोकें", "रुको"];

/**
 * Numbers (E.164) that asked to stop offers in a webhook payload: a text reply such as "STOP" or
 * the template's "Stop promotions" quick-reply button.
 */
export function optOutNumbers(body: unknown): string[] {
  const out = new Set<string>();
  const entries = (body as { entry?: unknown[] } | null)?.entry;
  if (!Array.isArray(entries)) return [];
  for (const entry of entries) {
    for (const change of ((entry as { changes?: unknown[] })?.changes || [])) {
      const messages = (change as { value?: { messages?: unknown[] } })?.value?.messages;
      if (!Array.isArray(messages)) continue;
      for (const m of messages as Array<Record<string, any>>) {
        const said = String(m?.text?.body ?? m?.button?.text ?? m?.button?.payload ??
          m?.interactive?.button_reply?.title ?? "").trim().toLowerCase();
        const from = String(m?.from || "").replace(/\D/g, "");
        if (!said || !/^91[6-9]\d{9}$/.test(from)) continue;
        if (STOP_WORDS.includes(said)) out.add(`+${from}`);
      }
    }
  }
  return [...out];
}
