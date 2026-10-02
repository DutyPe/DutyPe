/** DutyPe AI and AI plans against the Firestore emulator; the AI is a stub (no network). */
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
const dutypeAi = require("../../src/dutype-ai") as typeof import("../../src/dutype-ai");
const subscriptions = require("../../src/subscriptions") as typeof import("../../src/subscriptions");
const db = admin.firestore();
const T = admin.firestore.Timestamp;
const asEmployer = { auth: { uid: "emp1", token: { role: "EMPLOYER" } }, app: { appId: "t" } };
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
let prompts: string[] = [];
let reply: unknown = null;
hiring.ai.json = async (p: string) => {
  prompts.push(p);
  return reply;
};

async function clear() {
  const r = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  assert.ok(r.ok);
}

/** emp1 with two jobs, applicants (2 today), one hire; emp2 owns another job and applicant. */
async function seed(plan: "ai" | "none") {
  await db.doc("employer_profiles/emp1").set({
    ownerName: "Sita", businessName: "Sai Comforts", aiTrialUsed: plan === "none" ? 10 : 0,
    subscription: plan === "ai" ? { planId: "pro_ai_199", status: "ACTIVE", ai: true, aiPerDay: 100, credits: { normal: 6, instant: 15 } } :
      { planId: "", status: "NONE", credits: { normal: 0, instant: 0 } },
  });
  const now = Date.now();
  await db.doc("jobmetadata/cook").set({ employerId: "emp1", title: "Cook", status: "open", category: "COOK", vacancies: 2, applicationCount: 3, createdAt: T.fromMillis(now - 864e5) });
  await db.doc("jobmetadata/old").set({ employerId: "emp1", title: "Driver", status: "expired", category: "DRIVER", vacancies: 1, applicationCount: 0, createdAt: T.fromMillis(now - 40 * 864e5) });
  await db.doc("jobmetadata/other").set({ employerId: "emp2", title: "Guard", status: "open", category: "SECURITY", createdAt: T.fromMillis(now) });
  const app = (id: string, jobId: string, name: string, status: string, ageMs: number, employerId = "emp1") =>
    db.doc(`applications/${id}`).set({ jobId, workerId: id, employerId, status, workerName: name, createdAt: T.fromMillis(now - ageMs) });
  await app("a1", "cook", "Ravi", "applied", 60_000);
  await app("a2", "cook", "Suresh", "applied", 120_000);
  await app("a3", "cook", "Lakshmi", "hired", 3 * 864e5);
  await app("b1", "other", "Stranger", "applied", 60_000, "emp2");
}

describe("DutyPe AI (emulator)", () => {
  beforeEach(async () => {
    await clear();
    prompts = [];
    reply = null;
  });
  after(() => fft.cleanup());

  it("collects the employer's own facts only", async () => {
    await seed("ai");
    const f = await dutypeAi.employerFacts("emp1");
    assert.equal(f.jobs.length, 2);
    assert.equal(f.stats.openJobs, 1);
    assert.equal(f.stats.appliedToday, 2);
    assert.equal(f.stats.waiting, 2);
    assert.deepEqual(f.stats.hiredNames, ["Lakshmi"]);
    assert.ok(!f.applicants.some((a) => a.name === "Stranger"));
    assert.equal(f.firstName, "Sita");
    assert.equal(f.plan.active, true);
    assert.equal(f.plan.freeJobPostsLeftToday, 3);
    assert.equal(f.plan.freeUrgentPostsLeft, 3);
    assert.equal(f.plan.referralFreePosts, 0);
    assert.ok(f.homeServices.catalog.some((x) => x.id === "ac_service"));
    assert.deepEqual(f.homeServices.bookings, []);
  });

  it("knows home-service bookings and can propose booking one", async () => {
    await seed("ai");
    await db.doc("service_bookings/bk1").set({
      customerId: "emp1", serviceName: "AC service", status: "ON_THE_WAY", total: 449, partnerName: "Kiran", createdAt: T.now(),
    });
    await db.doc("service_bookings/bk2").set({ customerId: "emp2", serviceName: "Tap repair", status: "SEARCHING", createdAt: T.now() });
    const f = await dutypeAi.employerFacts("emp1");
    assert.deepEqual(f.homeServices.bookings.map((b) => b.id), ["bk1"]);
    assert.equal(dutypeAi.validateAction({ type: "book_service", args: { serviceId: "ac_service" } }, f)?.args.serviceId, "ac_service");
    assert.equal(dutypeAi.validateAction({ type: "book_service", args: { serviceId: "nope" } }, f), null);
    assert.equal(dutypeAi.validateAction({ type: "open_service_booking", args: { bookingId: "bk2" } }, f), null, "not their booking");
    assert.match(dutypeAi.quickAnswer("what about my AC service booking?", f, "en")!, /AC service.*on the way.*Kiran/);
    assert.match(dutypeAi.quickAnswer("how many free posts do I have?", f, "en")!, /3 more jobs free today/);
  });

  it("answers from the data and proposes a validated action", async () => {
    await seed("ai");
    reply = { reply: "Ravi applied a minute ago. Shall I hire him?", action: { type: "hire", args: { applicationId: "a1" } } };
    const r = await call(dutypeAi.dutypeAi)({ message: "who applied for cook?", lang: "en" }, asEmployer);
    assert.equal(r.action.type, "hire");
    assert.equal(r.action.summary, "Hire Ravi for Cook");
    assert.ok(prompts[0].includes("Ravi") && !prompts[0].includes("Stranger"), "the AI sees only this employer's data");
  });

  it("drops actions on other employers' data or that make no sense", async () => {
    await seed("ai");
    const f = await dutypeAi.employerFacts("emp1");
    assert.equal(dutypeAi.validateAction({ type: "hire", args: { applicationId: "b1" } }, f), null, "someone else's applicant");
    assert.equal(dutypeAi.validateAction({ type: "hire", args: { applicationId: "a3" } }, f), null, "already hired");
    assert.equal(dutypeAi.validateAction({ type: "close_job", args: { jobId: "old" } }, f), null, "expired job cannot be closed");
    assert.equal(dutypeAi.validateAction({ type: "renew_job", args: { jobId: "old" } }, f)?.summary, "Renew: Driver");
    assert.equal(dutypeAi.validateAction({ type: "post_job", args: { title: "Cook" } }, f), null, "no category / pay yet");
    assert.equal(dutypeAi.validateAction({ type: "post_job", args: { title: "Cook", category: "COOK", payAmount: 12000, payType: "MONTHLY", vacancies: 2 } }, f),
      null, "no line about the work yet");
    assert.equal(dutypeAi.validateAction({ type: "post_job", args: { title: "Cook", category: "COOK", payAmount: 90000, payType: "MONTHLY", vacancies: 2,
      description: "Cook for our hotel." } }, f), null, "above the ₹50,000 limit");
    const post = dutypeAi.validateAction({ type: "post_job", args: { title: "Cook", category: "COOK", payAmount: 12000, payType: "MONTHLY", vacancies: 2,
      description: "Cook breakfast and lunch for our hotel staff." } }, f);
    assert.equal(post?.args.category, "COOK");
    const urgent = dutypeAi.validateAction({ type: "post_urgent", args: { title: "Loaders", category: "PACKER", workersNeeded: 3, payPerPerson: 700, window: "today" } }, f);
    assert.equal(urgent?.summary, "Urgent: 3 × Loaders, ₹700 each");
    assert.equal(dutypeAi.validateAction({ type: "post_urgent", args: { title: "Loaders", workersNeeded: 99, payPerPerson: 700 } }, f), null);
    assert.equal(dutypeAi.validateAction({ type: "delete_everything", args: {} }, f), null);
  });

  it("when the AI does not answer, the free try is given back and data questions still work", async () => {
    await seed("none");
    await db.doc("employer_profiles/emp1").update({ aiTrialUsed: 3 });
    reply = null;
    const r = await call(dutypeAi.dutypeAi)({ message: "how many applied today", lang: "en" }, asEmployer);
    assert.equal(r.aiDown, true);
    assert.match(r.reply, /2 applied today/);
    const busy = await call(dutypeAi.dutypeAi)({ message: "post a cook job", lang: "en" }, asEmployer);
    assert.match(busy.reply, /busy/);
    assert.equal((await db.doc("employer_profiles/emp1").get()).get("aiTrialUsed"), 3, "both tries refunded");
  });

  it("without AI access it still answers counts and hires from the data", async () => {
    await seed("none");
    const r = await call(dutypeAi.dutypeAi)({ message: "ఈరోజు ఎంత మంది దరఖాస్తు చేశారు?", lang: "te" }, asEmployer);
    assert.equal(prompts.length, 0);
    assert.equal(r.locked, "upgrade");
    assert.match(r.reply, /2 మంది/);
    const h = await call(dutypeAi.dutypeAi)({ message: "who did I hire", lang: "en" }, asEmployer);
    assert.match(h.reply, /Lakshmi/);
    const other = await call(dutypeAi.dutypeAi)({ message: "post a cook job", lang: "en" }, asEmployer);
    assert.match(other.reply, /₹199/);
  });

  it("approving a ₹199 payment switches DutyPe AI on; ₹99 does not", async () => {
    const asAdmin = { auth: { uid: "admin1", token: { admin: true } }, app: { appId: "t" } };
    for (const [id, planId] of [["p1", "pro_ai_199"], ["p2", "basic_99"]]) {
      const uid = `e_${planId}`;
      await db.doc(`employer_profiles/${uid}`).set({ ownerName: "X" });
      await db.doc(`subscription_payment_requests/${id}`).set({ employerId: uid, planId, status: "PENDING", amountPaise: 1 });
      await call(subscriptions.verifySubscriptionPayment)({ requestId: id, approve: true }, asAdmin);
    }
    const pro = (await db.doc("employer_profiles/e_pro_ai_199").get()).get("subscription");
    const basic = (await db.doc("employer_profiles/e_basic_99").get()).get("subscription");
    assert.equal(pro.ai, true);
    assert.equal(pro.aiPerDay, 100);
    assert.equal(pro.credits.normal, 6);
    assert.equal(basic.ai, false);
    assert.equal(basic.credits.normal, 3);
  });
});
