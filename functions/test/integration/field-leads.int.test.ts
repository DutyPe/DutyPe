/** Umbrella-desk field registration against the Firestore emulator. */
import { after, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec`");
if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });
/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
const leads = require("../../src/field-leads") as typeof import("../../src/field-leads");
const db = admin.firestore();
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
const anon = { app: { appId: "web" } };
const code = (e: unknown) => (e as { code?: string }).code;

async function clear() {
  const r = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  assert.ok(r.ok);
}

const lead = (extra: Record<string, unknown> = {}) => ({
  agentCode: "ravi01", name: "Suresh", phone: "98480 12345", role: "partner", skills: ["ELECTRICIAN"], area: "Wyra Road",
  consent: true, ...extra,
});

describe("field leads (emulator)", () => {
  beforeEach(async () => {
    await clear();
    await db.doc("field_agents/RAVI01").set({ name: "Ravi", active: true, leads: 0, joined: 0 });
    await db.doc("field_agents/OLD").set({ name: "Old", active: false });
  });
  after(() => fft.cleanup());

  it("registers a lead for an active agent, merges duplicates, and credits the agent when the person joins", async () => {
    const r = await call(leads.submitFieldLead)(lead(), anon);
    assert.equal(r.duplicate, false);
    assert.equal(r.agentToday, 1);
    const doc = await db.doc("field_leads/+919848012345").get();
    assert.equal(doc.get("role"), "PARTNER");
    assert.equal(doc.get("agentCode"), "RAVI01");
    assert.equal(doc.get("status"), "NEW");

    const again = await call(leads.submitFieldLead)(lead({ skills: ["PLUMBER"], agentCode: "RAVI01" }), anon);
    assert.equal(again.duplicate, true);
    assert.deepEqual((await db.doc("field_leads/+919848012345").get()).get("skills"), ["ELECTRICIAN", "PLUMBER"]);
    assert.equal((await db.doc("field_agents/RAVI01").get()).get("leads"), 1, "a duplicate is not counted twice");

    await leads.markFieldLeadJoined("+919848012345", "u1");
    await leads.markFieldLeadJoined("+919848012345", "u1");
    assert.equal((await db.doc("field_leads/+919848012345").get()).get("status"), "JOINED");
    assert.equal((await db.doc("field_agents/RAVI01").get()).get("joined"), 1);
    await leads.markFieldLeadJoined("+919000000000", "u2"); // no lead: nothing happens
  });

  it("rejects inactive codes, bad phones and missing consent", async () => {
    await assert.rejects(call(leads.submitFieldLead)(lead({ agentCode: "OLD" }), anon), (e) => code(e) === "permission-denied");
    await assert.rejects(call(leads.submitFieldLead)(lead({ agentCode: "NOPE1" }), anon), (e) => code(e) === "permission-denied");
    await assert.rejects(call(leads.submitFieldLead)(lead({ phone: "12345" }), anon), (e) => code(e) === "invalid-argument");
    await assert.rejects(call(leads.submitFieldLead)(lead({ consent: false }), anon), (e) => code(e) === "failed-precondition");
    await assert.rejects(call(leads.submitFieldLead)(lead({ role: "boss" }), anon), (e) => code(e) === "invalid-argument");
  });
});
