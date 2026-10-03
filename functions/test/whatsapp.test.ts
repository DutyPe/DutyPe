import { describe, it } from "node:test";
import * as assert from "node:assert/strict";
import { langList, optOutNumbers, promoDue, serviceTemplates, waLang, waParam, waTextTemplateBody } from "../src/lib/whatsapp";

describe("WhatsApp business messages", () => {
  it("cleans template parameters the way Meta needs", () => {
    assert.equal(waParam("Plot 4,\nWyra Road\t  Khammam"), "Plot 4, Wyra Road Khammam");
    assert.equal(waParam(""), "-");
    assert.equal(waParam("x".repeat(200)).length, 120);
  });

  it("picks the user's language only when that translation is approved", () => {
    assert.equal(waLang("te", ["en", "te"]), "te");
    assert.equal(waLang("hi", ["en", "te"]), "en");
    assert.equal(waLang(undefined, ["te"]), "te");
    assert.deepEqual(langList(" te, en ,bad!"), ["te", "en"]);
    assert.deepEqual(langList(""), ["en"]);
  });

  it("builds a text template body", () => {
    const b = waTextTemplateBody("+919876543210", "service_partner_found", "te", ["Ravi", "AC service", "+919000000001"]);
    assert.equal(b.to, "919876543210");
    assert.equal(b.template.language.code, "te");
    assert.deepEqual(b.template.components![0].parameters.map((p) => p.text), ["Ravi", "AC service", "+919000000001"]);
    assert.equal(waTextTemplateBody("+919876543210", "t", "en", []).template.components, undefined);
  });

  it("service templates stay off until WhatsApp and a template are set", () => {
    const base = { WHATSAPP_TOKEN: "tok", WHATSAPP_PHONE_NUMBER_ID: "1" } as NodeJS.ProcessEnv;
    assert.equal(serviceTemplates(base), null);
    assert.equal(serviceTemplates({ ...base, WHATSAPP_TOKEN: "unset", WHATSAPP_SERVICE_CUSTOMER_TEMPLATE: "c" }), null);
    assert.deepEqual(serviceTemplates({ ...base, WHATSAPP_SERVICE_CUSTOMER_TEMPLATE: "c", WHATSAPP_SERVICE_LANGS: "te,en" }),
      { customer: "c", partner: "", langs: ["te", "en"] });
  });

  it("allows one offer per person every 7 days", () => {
    const day = 24 * 60 * 60 * 1000;
    assert.equal(promoDue(0, 10 * day, 7), true);
    assert.equal(promoDue(5 * day, 10 * day, 7), false);
    assert.equal(promoDue(3 * day, 10 * day, 7), true);
  });

  it("finds STOP replies in a webhook payload", () => {
    const body = { entry: [{ changes: [{ value: { messages: [
      { from: "919876543210", type: "text", text: { body: " STOP " } },
      { from: "919876543211", type: "button", button: { text: "Stop promotions", payload: "Stop promotions" } },
      { from: "919876543212", type: "text", text: { body: "ఆపండి" } },
      { from: "919876543213", type: "text", text: { body: "please stop calling" } },
      { from: "15550001111", type: "text", text: { body: "stop" } },
    ] } }] }] };
    assert.deepEqual(optOutNumbers(body), ["+919876543210", "+919876543211", "+919876543212"]);
    assert.deepEqual(optOutNumbers(null), []);
  });
});
