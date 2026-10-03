/**
 * WhatsApp offers switch, admin offer send, STOP replies and service confirmations against the
 * Firestore + Auth emulators. Meta is stubbed (no network).
 */
import { after, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec`");
if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });
/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
const wa = require("../../src/whatsapp-messages") as typeof import("../../src/whatsapp-messages");
const lib = require("../../src/lib/whatsapp") as typeof import("../../src/lib/whatsapp");
const db = admin.firestore();
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
const as = (uid: string, token: Record<string, unknown> = {}) => ({ auth: { uid, token }, app: { appId: "t" } });

let sent: Array<{ to: string; name: string; lang: string; params: string[] }> = [];
let ok = true;
lib.waNet.fetch = async (_url: string, init?: RequestInit) => {
  const b = JSON.parse(String(init?.body));
  sent.push({ to: b.to, name: b.template.name, lang: b.template.language.code,
    params: (b.template.components?.[0]?.parameters || []).map((p: any) => p.text) });
  return new Response(ok ? "{}" : "bad template", { status: ok ? 200 : 400 });
};

async function clear() {
  const fs = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  assert.ok(fs.ok);
}

describe("WhatsApp messages (emulator)", () => {
  beforeEach(async () => {
    await clear();
    sent = [];
    ok = true;
    Object.assign(process.env, { WHATSAPP_TOKEN: "tok", WHATSAPP_PHONE_NUMBER_ID: "123" });
    delete process.env.WHATSAPP_SERVICE_CUSTOMER_TEMPLATE;
    delete process.env.WHATSAPP_SERVICE_PARTNER_TEMPLATE;
    delete process.env.WHATSAPP_PROMO_DAILY_CAP;
  });
  after(() => fft.cleanup());

  it("offers are off until the user switches them on, and STOP switches them off", async () => {
    assert.deepEqual(await call(wa.whatsappPromos)({}, as("u1", { phone_number: "+919876543210" })), { enabled: false });
    await db.collection("worker_profiles").doc("u1").set({ name: "Ravi" });
    await db.collection("user_tokens").doc("u1").set({ language: "te" });
    assert.deepEqual(await call(wa.whatsappPromos)({ enabled: true }, as("u1", { phone_number: "+919876543210" })), { enabled: true });
    const p = (await db.collection("whatsapp_prefs").doc("u1").get()).data()!;
    assert.deepEqual([p.promos, p.phone, p.language, p.roles], [true, "+919876543210", "te", ["WORKER"]]);
    assert.ok(p.consentAt);

    await assert.rejects(call(wa.whatsappPromos)({ enabled: true }, as("u2")), (e: any) => e.code === "failed-precondition");

    assert.equal(await wa.stopWhatsappPromos(["+919876543210"]), 1);
    const after = (await db.collection("whatsapp_prefs").doc("u1").get()).data()!;
    assert.deepEqual([after.promos, after.source], [false, "whatsapp_stop"]);
    assert.deepEqual(await call(wa.whatsappPromos)({}, as("u1")), { enabled: false });
  });

  it("admin offer: dry run counts, send goes once per 7 days, in each person's language", async () => {
    const recent = admin.firestore.Timestamp.fromMillis(Date.now() - 2 * 86400_000);
    await db.collection("whatsapp_prefs").doc("a").set({ promos: true, phone: "+919000000001", language: "te", roles: ["WORKER"] });
    await db.collection("whatsapp_prefs").doc("b").set({ promos: true, phone: "+919000000002", language: "hi", roles: ["EMPLOYER"] });
    await db.collection("whatsapp_prefs").doc("c").set({ promos: true, phone: "+919000000003", language: "te", roles: ["WORKER"], lastPromoAt: recent });
    await db.collection("whatsapp_prefs").doc("d").set({ promos: false, phone: "+919000000004", roles: ["WORKER"] });
    const adminCtx = as("boss", { admin: true });

    await assert.rejects(call(wa.sendWhatsappPromo)({ template: "dasara_offer" }, as("x")), (e: any) => e.code === "permission-denied");
    const dry = await call(wa.sendWhatsappPromo)({ template: "dasara_offer", langs: "te,en" }, adminCtx);
    assert.deepEqual([dry.dryRun, dry.matched, dry.willSend, dry.skippedRecent], [true, 3, 2, 1]);
    assert.equal(sent.length, 0);

    const workers = await call(wa.sendWhatsappPromo)({ template: "dasara_offer", langs: "te,en", audience: "WORKER", dryRun: false, params: ["DASARA50"] }, adminCtx);
    assert.deepEqual([workers.sent, workers.skippedRecent], [1, 1]);
    assert.deepEqual(sent, [{ to: "919000000001", name: "dasara_offer", lang: "te", params: ["DASARA50"] }]);

    const all = await call(wa.sendWhatsappPromo)({ template: "dasara_offer", langs: "te,en", dryRun: false }, adminCtx);
    assert.equal(all.sent, 1, "a got one already today; only b now");
    assert.equal(sent.at(-1)!.lang, "te", "Hindi is not approved for this template, so the first language");
    const campaigns = await db.collection("whatsapp_campaigns").get();
    assert.equal(campaigns.size, 2);

    // A rejected template stops after the first batch and gives the budget back.
    await db.collection("whatsapp_prefs").doc("e").set({ promos: true, phone: "+919000000005", roles: ["WORKER"] });
    ok = false;
    const bad = await call(wa.sendWhatsappPromo)({ template: "not_approved", dryRun: false }, adminCtx);
    assert.deepEqual([bad.sent, bad.failed], [0, 1]);
    const day = (await db.collection("otp_daily").get()).docs[0].data();
    assert.equal(day.promoCount, 2);
  });

  it("booking confirmed: WhatsApp to the customer and the partner, only when templates are set", async () => {
    const b = {
      customerId: "c1", customerName: "Ramesh", customerPhone: "+919876500001", serviceName: "AC service",
      partnerId: "p1", partnerName: "Ravi", partnerPhone: "+919876500002", addressText: "H.No 2-14,\nWyra Road", area: "Khammam",
    };
    assert.equal(await wa.whatsappServiceAssigned(b), 0);
    assert.equal(sent.length, 0);

    Object.assign(process.env, { WHATSAPP_SERVICE_CUSTOMER_TEMPLATE: "service_partner_found",
      WHATSAPP_SERVICE_PARTNER_TEMPLATE: "service_job_confirmed", WHATSAPP_SERVICE_LANGS: "en,te" });
    await db.collection("user_tokens").doc("p1").set({ language: "te" });
    assert.equal(await wa.whatsappServiceAssigned(b), 2);
    const byName = Object.fromEntries(sent.map((s) => [s.name, s]));
    assert.deepEqual(byName.service_partner_found, { to: "919876500001", name: "service_partner_found", lang: "en",
      params: ["Ravi", "AC service", "+919876500002"] });
    assert.deepEqual(byName.service_job_confirmed, { to: "919876500002", name: "service_job_confirmed", lang: "te",
      params: ["AC service", "Ramesh", "+919876500001", "H.No 2-14, Wyra Road, Khammam"] });

    // Meta down: the booking is unaffected (0 delivered, no throw).
    ok = false;
    assert.equal(await wa.whatsappServiceAssigned(b), 0);
  });
});
