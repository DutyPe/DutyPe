/** AI hiring helpers against the Firestore emulator; the AI is replaced by a stub (no network). */
import { after, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec`");
if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });
/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
const hiring = require("../../src/ai-hiring") as typeof import("../../src/ai-hiring");
const { encodeGeohash } = require("../../src/lib/geo") as typeof import("../../src/lib/geo");
const db = admin.firestore();
const KHAMMAM = { lat: 17.2477, lng: 80.1437 };
const asEmployer = { auth: { uid: "emp1", token: { role: "EMPLOYER" } }, app: { appId: "t" } };
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
let aiPrompts: string[] = [];
let aiReply: unknown = null;
hiring.ai.json = async (p: string) => {
  aiPrompts.push(p);
  return aiReply;
};

async function clear() {
  const r = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  assert.ok(r.ok);
}

function card(skills: string[], km: number, extra: Record<string, unknown> = {}) {
  const lat = KHAMMAM.lat + km / 111;
  const cell = encodeGeohash(lat, KHAMMAM.lng, 5);
  return {
    skills, lat, lng: KHAMMAM.lng, cell, available: true,
    skillCells: [...skills, "ANY"].map((s) => `${s}_${cell}`),
    lastActiveAt: admin.firestore.Timestamp.now(), ...extra,
  };
}

const WORKERS = {
  good: card(["DELIVERY"], 2, { experienceYears: 3, jobsCompleted: 6, rating: 4.8, ratingCount: 5 }),
  far: card(["DELIVERY"], 18, { experienceYears: 3 }),
  noskill: card(["COOK"], 1, { experienceYears: 5 }),
  flaky: card(["DELIVERY"], 2, { experienceYears: 3, noShows: 3 }),
};

/** An employer: on the free trial by default, or on an AI plan. */
async function seedEmployer(plan: "trial" | "ai" | "none" = "trial", aiPerDay = 100) {
  const sub = plan === "ai" ?
    { planId: "pro_ai_199", status: "ACTIVE", ai: true, aiPerDay, credits: { normal: 6, instant: 15 } } :
    { planId: "", status: "NONE", credits: { normal: 0, instant: 0 } };
  await db.doc("employer_profiles/emp1").set({ ownerName: "Sita", subscription: sub, aiTrialUsed: plan === "none" ? 10 : 0 });
}

async function seedJob() {
  await seedEmployer();
  await db.doc("jobmetadata/job1").set({ employerId: "emp1", title: "Delivery Boy", category: "DELIVERY", lat: KHAMMAM.lat, lng: KHAMMAM.lng, payType: "MONTHLY", status: "open" });
  await db.doc("job_details/job1").set({ employerId: "emp1", description: "Bike needed", experienceRequired: "1-3 years" });
  let t = 1;
  for (const [w, c] of Object.entries(WORKERS)) {
    await db.doc(`worker_cards/${w}`).set(c);
    await db.doc(`applications/job1_${w}`).set({
      jobId: "job1", workerId: w, employerId: "emp1", status: "applied",
      workerName: `Name-${w}`, workerPhoto: "", createdAt: admin.firestore.Timestamp.fromMillis(t++ * 1000),
    });
  }
}

describe("AI hiring (emulator)", () => {
  beforeEach(async () => {
    await clear();
    aiPrompts = [];
    aiReply = null;
  });
  after(() => fft.cleanup());

  it("shortlist ranks skilled, near, reliable workers first and sends the AI no personal data", async () => {
    await seedJob();
    const res = await call(hiring.aiShortlist)({ jobId: "job1", lang: "te" }, asEmployer);
    assert.equal(res.considered, 4);
    assert.equal(res.byAi, false, "no AI reply: the fixed score decides");
    assert.equal(res.picks.length, 3);
    assert.equal(res.picks[0].workerId, "good");
    assert.ok(!res.picks.slice(0, 2).some((p: any) => p.workerId === "flaky"), "3 no-shows keep a worker out of the top 2");
    assert.match(res.picks[0].reason, /కి\.మీ/, "fallback reason is in Telugu");
    const prompt = aiPrompts[0];
    assert.ok(prompt, "the AI was asked");
    assert.ok(!/Name-|good|flaky|noskill/.test(prompt), "the AI prompt has no names or worker ids");
  });

  it("uses the AI's order and reasons, ignores invalid ids, and caches", async () => {
    await seedJob();
    aiReply = { picks: [{ id: "C2", reason: "close and experienced" }, { id: "C1", reason: "best rated" }, { id: "C99", reason: "bad id" }] };
    const res = await call(hiring.aiShortlist)({ jobId: "job1", lang: "en" }, asEmployer);
    assert.equal(res.byAi, true);
    assert.equal(res.picks.length, 2, "the invalid id is dropped");
    assert.equal(res.picks[0].reason, "close and experienced");
    aiPrompts = [];
    await call(hiring.aiShortlist)({ jobId: "job1", lang: "en" }, asEmployer);
    assert.equal(aiPrompts.length, 0, "unchanged applicants: served from cache");
  });

  it("only the job's employer can shortlist", async () => {
    await db.doc("jobmetadata/job2").set({ employerId: "someoneElse", category: "COOK", lat: 1, lng: 1 });
    await assert.rejects(call(hiring.aiShortlist)({ jobId: "job2" }, asEmployer), /Not your job/);
  });

  it("counts available skilled workers within 5 and 10 km", async () => {
    await db.doc("worker_cards/a").set(card(["DRIVER"], 1));
    await db.doc("worker_cards/b").set(card(["DRIVER"], 3));
    await db.doc("worker_cards/c").set(card(["DRIVER"], 8));
    await db.doc("worker_cards/d").set({ ...card(["DRIVER"], 2), available: false });
    await db.doc("worker_cards/e").set(card(["COOK"], 1));
    const res = await call(hiring.nearbyWorkerCount)({ ...KHAMMAM, category: "DRIVER" }, asEmployer);
    assert.equal(res.within5km, 2);
    assert.equal(res.within10km, 3);
  });

  it("talk-to-post keeps form-valid values and asks for what is missing", async () => {
    await seedEmployer("ai");
    aiReply = {
      title: "Delivery Boy", category: "delivery", payAmount: 15000, payType: "monthly", vacancies: 3,
      shift: "DAY", gender: "Unknown", experience: "lots", perks: ["Food Provided", "Free phone"], description: "Deliver groceries.",
    };
    const r = await call(hiring.aiJobAssistant)({ transcript: "3 delivery boys 15000 per month", lang: "en", draft: {} }, asEmployer);
    assert.equal(r.ready, true);
    assert.equal(r.draft.category, "DELIVERY");
    assert.equal(r.draft.payType, "MONTHLY");
    assert.equal(r.draft.gender, "Both", "unknown values fall back to the form defaults");
    assert.equal(r.draft.experience, "No Experience Required");
    assert.deepEqual(r.draft.perks, ["Food Provided"]);
    assert.match(r.summary, /3 × Delivery Boy/);

    aiReply = { title: "Cook", category: "COOK" };
    const r2 = await call(hiring.aiJobAssistant)({ transcript: "cook kavali", lang: "te", draft: {} }, asEmployer);
    assert.equal(r2.ready, false);
    assert.deepEqual(r2.missing, ["pay", "about"], "pay first, then one line about the work");
    assert.match(r2.question, /జీతం/);

    aiReply = { title: "Cook", category: "COOK", payAmount: 60000, payType: "MONTHLY", description: "Cook lunch for 20 staff, 9am to 3pm." };
    const r3 = await call(hiring.aiJobAssistant)({ transcript: "60000 per month", lang: "en", draft: r2.draft }, asEmployer);
    assert.equal(r3.draft.payAmount, 0, "above ₹50,000 counts as not given");
    assert.deepEqual(r3.missing, ["pay"]);
    assert.match(r3.question, /50,000/);

    aiReply = { payAmount: 15000, payType: "WEEKLY" };
    const r4 = await call(hiring.aiJobAssistant)({ transcript: "15000 per week", lang: "en", draft: r3.draft }, asEmployer);
    assert.equal(r4.draft.payType, "WEEKLY");
    assert.equal(r4.ready, true);
  });

  it("free trial: 10 AI actions, then the assistant asks to upgrade", async () => {
    await seedEmployer("trial");
    aiReply = { title: "Helper", category: "HELPER" };
    for (let i = 0; i < 10; i++) await call(hiring.aiJobAssistant)({ transcript: "helper", lang: "en" }, asEmployer);
    assert.equal(aiPrompts.length, 10);
    const locked = await call(hiring.aiJobAssistant)({ transcript: "helper", lang: "te" }, asEmployer);
    assert.equal(aiPrompts.length, 10, "the 11th trial call does not reach the AI");
    assert.equal(locked.locked, "upgrade");
    assert.match(locked.question, /₹199/);
  });

  it("AI plan: daily allowance, then a limit message", async () => {
    await seedEmployer("ai", 3);
    aiReply = { title: "Helper", category: "HELPER" };
    for (let i = 0; i < 3; i++) await call(hiring.aiJobAssistant)({ transcript: "helper", lang: "en" }, asEmployer);
    const limited = await call(hiring.aiJobAssistant)({ transcript: "helper", lang: "en" }, asEmployer);
    assert.equal(aiPrompts.length, 3);
    assert.equal(limited.locked, "limit");
    const trialLeft = (await db.doc("employer_profiles/emp1").get()).get("aiTrialUsed");
    assert.equal(trialLeft, 0, "plan users do not spend trial actions");
  });

  it("no AI access: top picks still come from the fixed score, without calling the AI", async () => {
    await seedJob();
    await seedEmployer("none");
    const res = await call(hiring.aiShortlist)({ jobId: "job1", lang: "en" }, asEmployer);
    assert.equal(aiPrompts.length, 0);
    assert.equal(res.aiLocked, true);
    assert.equal(res.picks[0].workerId, "good");
  });
});
