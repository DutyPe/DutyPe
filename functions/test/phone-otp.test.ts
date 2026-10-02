import { describe, it } from "node:test";
import * as assert from "node:assert/strict";
import {
  MAX_PER_DAY, MAX_PER_HOUR, RESEND_GAP_MS, codeMatches, decideSend, hashCode, indianE164, istDayKey, newCode,
  parseTruecallerUserInfo, whatsappConfigured, whatsappTemplateBody, type SendCounters,
} from "../src/lib/phone-otp";

describe("indianE164", () => {
  it("accepts the usual ways a number is written", () => {
    for (const raw of ["9876543210", "+919876543210", "919876543210", "09876543210", "+91 98765-43210"]) {
      assert.equal(indianE164(raw), "+919876543210", raw);
    }
  });
  it("rejects non-mobile and foreign numbers", () => {
    for (const raw of ["", "12345", "5876543210", "+14155552671", "4479460000000", null, undefined]) {
      assert.equal(indianE164(raw), null, String(raw));
    }
  });
});

describe("codes", () => {
  it("are 6 digits", () => {
    for (let i = 0; i < 200; i++) assert.match(newCode(), /^\d{6}$/);
  });
  it("match only the same number and code", () => {
    const h = hashCode("+919876543210", "123456");
    assert.ok(codeMatches("+919876543210", "123456", h));
    assert.ok(!codeMatches("+919876543210", "123457", h));
    assert.ok(!codeMatches("+919876543211", "123456", h));
    assert.ok(!codeMatches("+919876543210", "123456", ""));
    assert.ok(!codeMatches("+919876543210", "123456", "zz"));
  });
});

describe("decideSend", () => {
  const t0 = Date.UTC(2026, 9, 2, 6, 0, 0);
  it("allows the first code", () => {
    const d = decideSend(null, t0);
    assert.ok(d.ok);
    if (d.ok) assert.deepEqual(d.next, { lastSentAt: t0, hourStart: t0, hourCount: 1, dayKey: istDayKey(t0), dayCount: 1 });
  });
  it("makes the user wait 30 s between codes", () => {
    const first = decideSend(null, t0);
    assert.ok(first.ok);
    const again = decideSend(first.ok ? first.next : null, t0 + 10_000);
    assert.deepEqual(again, { ok: false, reason: "wait", retryAfterSec: 20 });
    assert.ok(decideSend(first.ok ? first.next : null, t0 + RESEND_GAP_MS).ok);
  });
  it("allows 5 an hour, then resets after the hour", () => {
    let c: SendCounters | null = null;
    let t = t0;
    for (let i = 0; i < MAX_PER_HOUR; i++) {
      const d = decideSend(c, t);
      assert.ok(d.ok, `send ${i + 1}`);
      if (d.ok) c = d.next;
      t += RESEND_GAP_MS;
    }
    const blocked = decideSend(c, t);
    assert.equal(blocked.ok, false);
    if (!blocked.ok) assert.equal(blocked.reason, "hour");
    assert.ok(decideSend(c, t0 + 60 * 60 * 1000).ok);
  });
  it("allows 10 a day (IST)", () => {
    const c: SendCounters = { lastSentAt: t0 - 3_600_000, hourStart: t0 - 3_600_000, hourCount: 1, dayKey: istDayKey(t0), dayCount: MAX_PER_DAY };
    const d = decideSend(c, t0);
    assert.equal(d.ok, false);
    if (!d.ok) assert.equal(d.reason, "day");
    // Next IST day is allowed again.
    assert.ok(decideSend(c, t0 + 24 * 60 * 60 * 1000).ok);
  });
  it("uses the Indian calendar day", () => {
    assert.equal(istDayKey(Date.UTC(2026, 9, 1, 19, 0)), "2026-10-02"); // 00:30 IST
    assert.equal(istDayKey(Date.UTC(2026, 9, 1, 18, 0)), "2026-10-01"); // 23:30 IST
  });
});

describe("parseTruecallerUserInfo", () => {
  it("reads number, full name and email", () => {
    const u = parseTruecallerUserInfo({
      sub: "x", given_name: "Ravi ", family_name: "Kumar", phone_number: "919876543210", email: "Ravi@Example.com",
    });
    assert.deepEqual(u, { phone: "+919876543210", name: "Ravi Kumar", email: "ravi@example.com" });
  });
  it("tolerates missing or bad fields", () => {
    assert.deepEqual(parseTruecallerUserInfo({ phone_number: "+14155552671", email: "not-an-email" }),
      { phone: null, name: "", email: "" });
    assert.deepEqual(parseTruecallerUserInfo(null), { phone: null, name: "", email: "" });
    assert.equal(parseTruecallerUserInfo({ name: "Sita", phone_number: "9876543210" }).name, "Sita");
  });
});

describe("WhatsApp settings", () => {
  it("is off until the token, number id and template are set", () => {
    assert.equal(whatsappConfigured({}), false);
    assert.equal(whatsappConfigured({ WHATSAPP_TOKEN: "unset", WHATSAPP_PHONE_NUMBER_ID: "1", WHATSAPP_TEMPLATE: "t" }), false);
    assert.equal(whatsappConfigured({ WHATSAPP_TOKEN: "abc", WHATSAPP_PHONE_NUMBER_ID: "", WHATSAPP_TEMPLATE: "t" }), false);
    assert.equal(whatsappConfigured({ WHATSAPP_TOKEN: "abc", WHATSAPP_PHONE_NUMBER_ID: "1", WHATSAPP_TEMPLATE: "t" }), true);
  });
  it("builds an authentication template message", () => {
    const b = whatsappTemplateBody("+919876543210", "042137", "dutype_login_otp", "en");
    assert.equal(b.to, "919876543210");
    assert.equal(b.template.name, "dutype_login_otp");
    assert.deepEqual(b.template.components[0].parameters, [{ type: "text", text: "042137" }]);
    assert.equal(b.template.components[1].sub_type, "url");
  });
});
