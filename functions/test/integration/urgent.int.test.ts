/**
 * Urgent dispatch, end to end against the Firestore emulator. Run from the repo root:
 *   npx firebase emulators:exec --only firestore --project demo-dutype "cd functions && npx tsx --test test/integration/*.test.ts"
 * Pushes are captured (sender.send is replaced), so each wave's areas and texts can be checked.
 */
import { after, before, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec` (FIRESTORE_EMULATOR_HOST unset)");
admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });

/* eslint-disable @typescript-eslint/no-var-requires */
const urgent = require("../../src/urgent") as typeof import("../../src/urgent");
const instant = require("../../src/instant") as typeof import("../../src/instant");
const { coveringCells } = require("../../src/lib/geo") as typeof import("../../src/lib/geo");
const { InstantRequests: IR, WorkerProfiles: WP, WorkerCards: WC, EmployerProfiles: EP } =
  require("../../src/schema") as typeof import("../../src/schema");

const db = admin.firestore();
const KHAMMAM = { lat: 17.2477, lng: 80.1437 };
const EMPLOYER = "emp1";
const sent: admin.messaging.Message[] = [];
urgent.sender.send = async (m) => {
  sent.push(m);
  return "ok";
};

const asUser = (uid: string, role: string) => ({ auth: { uid, token: { role } }, app: { appId: "test" } });
const accept = fft.wrap(urgent.acceptUrgentOffer as never) as unknown as (d: unknown, c: unknown) => Promise<{ result: string; contactNumber?: string }>;
const post = fft.wrap(instant.postInstantRequest as never) as unknown as (d: unknown, c: unknown) => Promise<{ id: string }>;
const setResponse = fft.wrap(instant.setInstantResponseStatus as never) as unknown as (d: unknown, c: unknown) => Promise<unknown>;
const setRequest = fft.wrap(instant.setInstantRequestStatus as never) as unknown as (d: unknown, c: unknown) => Promise<unknown>;
const runWaves = fft.wrap(urgent.dispatchUrgentWaves as never) as unknown as (d?: unknown) => Promise<unknown>;
const runExpiry = fft.wrap(instant.expireInstantRequests as never) as unknown as (d?: unknown) => Promise<unknown>;

async function clearAll() {
  const res = await fetch(
    `http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`,
    { method: "DELETE" });
  assert.ok(res.ok, "could not clear the emulator");
}

async function seedPeople(workers: string[]) {
  await db.collection(EP.COLLECTION).doc(EMPLOYER).set({ [EP.OWNER_NAME]: "Sita", [EP.PHONE]: "+919876543210" });
  await Promise.all(workers.map((w) => db.collection(WP.COLLECTION).doc(w).set({ [WP.NAME]: `Worker ${w}`, [WP.AVAILABLE]: true })));
}

/** Posts through the real callable, then does what the onCreate trigger does (wave 1). */
async function postNeed(workersNeeded: number, reqId: string): Promise<string> {
  const { id } = await post({
    requestId: `req-${reqId}-0123456789abcdef`, title: "Loading helpers", category: "HELPER", workersNeeded, payPerPerson: 800,
    addressText: "Wyra Road, Khammam", area: "Wyra Road", contactNumber: "9876543210",
    lat: KHAMMAM.lat, lng: KHAMMAM.lng, window: "today",
  }, asUser(EMPLOYER, "EMPLOYER"));
  await db.collection(IR.COLLECTION).doc(id).update({ [IR.DISPATCH_RADIUS_KM]: 0, [IR.NEXT_WAVE_AT]: admin.firestore.Timestamp.fromMillis(0) });
  await urgent.advanceWave(id, Date.now());
  return id;
}

async function need(id: string) {
  return (await db.collection(IR.COLLECTION).doc(id).get()).data() || {};
}

async function makeWaveDue(id: string) {
  await db.collection(IR.COLLECTION).doc(id).update({ [IR.NEXT_WAVE_AT]: admin.firestore.Timestamp.fromMillis(Date.now() - 1000) });
}

function topicsIn(messages: admin.messaging.Message[], locale: string): Set<string> {
  const out = new Set<string>();
  for (const m of messages) {
    const condition = (m as { condition?: string }).condition || "";
    for (const match of condition.matchAll(/'urgent_([0-9a-z]+)_([a-z]+)' in topics/g)) {
      if (match[2] === locale) out.add(match[1]);
    }
  }
  return out;
}

describe("urgent dispatch (emulator)", () => {
  before(clearAll);
  beforeEach(async () => {
    sent.length = 0;
    await clearAll();
  });
  after(() => fft.cleanup());

  it("sends offers in 5 → 10 → 15 → 20 km waves, each only to new areas, then stops", async () => {
    await seedPeople([]);
    const id = await postNeed(2, "waves");
    let reached = new Set<string>();
    for (const km of [5, 10, 15, 20]) {
      if (km > 5) {
        await makeWaveDue(id);
        sent.length = 0;
        await runWaves();
      }
      const cells = topicsIn(sent, "te");
      const expected = new Set(coveringCells(KHAMMAM.lat, KHAMMAM.lng, km, 5).filter((c) => !reached.has(c)));
      assert.deepEqual([...cells].sort(), [...expected].sort(), `wave ${km} km`);
      assert.equal(topicsIn(sent, "en").size, cells.size, "every language gets the same areas");
      for (const m of sent) {
        const condition = (m as { condition?: string }).condition || "";
        assert.ok(condition.split("||").length <= 5, "at most 5 topics per send");
        assert.equal(m.data?.type, "URGENT_OFFER");
        assert.equal(m.data?.waveKm, String(km));
      }
      assert.equal((await need(id))[IR.DISPATCH_RADIUS_KM], km);
      reached = new Set([...reached, ...cells]);
    }
    assert.equal((await need(id))[IR.NEXT_WAVE_AT], undefined, "no wave after 20 km");
    sent.length = 0;
    await runWaves();
    assert.equal(sent.length, 0);
  });

  it("a wave is not sent before it is due", async () => {
    await seedPeople([]);
    await postNeed(1, "notdue");
    sent.length = 0;
    await runWaves();
    assert.equal(sent.length, 0);
  });

  it("first workers to accept get the places; the rest see 'filled'; waves stop", async () => {
    await seedPeople(["w1", "w2", "w3"]);
    const id = await postNeed(2, "fill");
    const a = await accept({ requestId: id }, asUser("w1", "WORKER"));
    assert.equal(a.result, "accepted");
    assert.equal(a.contactNumber, "9876543210");
    assert.equal((await need(id))[IR.STATUS], "open");
    assert.equal((await accept({ requestId: id }, asUser("w2", "WORKER"))).result, "accepted");
    const r = await need(id);
    assert.equal(r[IR.STATUS], "filled");
    assert.deepEqual([...r[IR.SELECTED_WORKER_IDS]].sort(), ["w1", "w2"]);
    assert.equal(r[IR.NEXT_WAVE_AT], undefined);
    assert.equal((await accept({ requestId: id }, asUser("w3", "WORKER"))).result, "filled");
    // Accepting twice is harmless.
    assert.equal((await accept({ requestId: id }, asUser("w1", "WORKER"))).result, "accepted");
    assert.equal((await need(id))[IR.RESPONSE_COUNT], 2);
    const w1 = (await db.collection(WP.COLLECTION).doc("w1").get()).data() || {};
    assert.equal(w1[WP.ACTIVE_URGENT_ID], id);
    // Employer inbox got "Worker w1 accepted … 1 of 2".
    const inbox = await db.collection("notifications").where("recipientId", "==", EMPLOYER).get();
    assert.equal(inbox.size, 2);
  });

  it("8 workers tapping Accept at the same moment fill exactly 3 places", async () => {
    const workers = ["a", "b", "c", "d", "e", "f", "g", "h"];
    await seedPeople(workers);
    const id = await postNeed(3, "race");
    const results = await Promise.all(workers.map((w) => accept({ requestId: id }, asUser(w, "WORKER"))));
    assert.equal(results.filter((r) => r.result === "accepted").length, 3);
    assert.equal(results.filter((r) => r.result === "filled").length, 5);
    const r = await need(id);
    assert.equal(r[IR.SELECTED_WORKER_IDS].length, 3);
    assert.equal(r[IR.STATUS], "filled");
  });

  it("a worker holds one urgent job at a time", async () => {
    await seedPeople(["w1"]);
    const first = await postNeed(2, "busy1");
    const second = await postNeed(2, "busy2");
    assert.equal((await accept({ requestId: first }, asUser("w1", "WORKER"))).result, "accepted");
    assert.equal((await accept({ requestId: second }, asUser("w1", "WORKER"))).result, "busy");
    await setResponse({ requestId: first, workerId: "w1", status: "COMPLETED" }, asUser(EMPLOYER, "EMPLOYER"));
    assert.equal((await accept({ requestId: second }, asUser("w1", "WORKER"))).result, "accepted");
    const card = (await db.collection(WC.COLLECTION).doc("w1").get()).data() || {};
    assert.equal(card[WC.JOBS_COMPLETED], 1);
  });

  it("removing a worker reopens the place, restarts offers from 5 km and frees the worker", async () => {
    await seedPeople(["w1", "w2", "w3"]);
    const id = await postNeed(2, "remove");
    await accept({ requestId: id }, asUser("w1", "WORKER"));
    await accept({ requestId: id }, asUser("w2", "WORKER"));
    await setResponse({ requestId: id, workerId: "w2", status: "NO_SHOW" }, asUser(EMPLOYER, "EMPLOYER"));
    const r = await need(id);
    assert.equal(r[IR.STATUS], "open");
    assert.deepEqual(r[IR.SELECTED_WORKER_IDS], ["w1"]);
    assert.equal(r[IR.DISPATCH_RADIUS_KM], 0);
    const w2 = (await db.collection(WP.COLLECTION).doc("w2").get()).data() || {};
    assert.equal(w2[WP.ACTIVE_URGENT_ID], undefined);
    assert.equal(((await db.collection(WC.COLLECTION).doc("w2").get()).data() || {})[WC.NO_SHOWS], 1);
    // The removed worker cannot take it again; someone else can.
    assert.equal((await accept({ requestId: id }, asUser("w2", "WORKER"))).result, "removed");
    sent.length = 0;
    await runWaves();
    assert.deepEqual([...topicsIn(sent, "hi")].sort(), coveringCells(KHAMMAM.lat, KHAMMAM.lng, 5, 5).sort(), "offers restart at 5 km");
    assert.equal((await accept({ requestId: id }, asUser("w3", "WORKER"))).result, "accepted");
    assert.equal((await need(id))[IR.STATUS], "filled");
  });

  it("cancelling frees the workers, stops offers and tells them", async () => {
    await seedPeople(["w1"]);
    const id = await postNeed(3, "cancel");
    await accept({ requestId: id }, asUser("w1", "WORKER"));
    await setRequest({ requestId: id, status: "CANCELLED" }, asUser(EMPLOYER, "EMPLOYER"));
    const r = await need(id);
    assert.equal(r[IR.STATUS], "cancelled");
    assert.equal(r[IR.NEXT_WAVE_AT], undefined);
    assert.equal(((await db.collection(WP.COLLECTION).doc("w1").get()).data() || {})[WP.ACTIVE_URGENT_ID], undefined);
    const told = await db.collection("notifications").where("recipientId", "==", "w1").get();
    assert.equal(told.size, 1);
    assert.equal((await accept({ requestId: id }, asUser("w1", "WORKER"))).result, "accepted",
      "their earlier acceptance stays on record");
  });

  it("expiry closes open and filled needs and frees their workers", async () => {
    await seedPeople(["w1"]);
    const id = await postNeed(1, "expire");
    await accept({ requestId: id }, asUser("w1", "WORKER"));
    assert.equal((await need(id))[IR.STATUS], "filled");
    await db.collection(IR.COLLECTION).doc(id).update({ [IR.EXPIRES_AT]: admin.firestore.Timestamp.fromMillis(Date.now() - 1000) });
    await runExpiry();
    assert.equal((await need(id))[IR.STATUS], "expired");
    assert.equal(((await db.collection(WP.COLLECTION).doc("w1").get()).data() || {})[WP.ACTIVE_URGENT_ID], undefined);
    const other = await postNeed(1, "after-expiry");
    assert.equal((await accept({ requestId: other }, asUser("w1", "WORKER"))).result, "accepted");
  });

  it("an employer cannot take their own job; a closed job cannot be taken", async () => {
    await seedPeople(["w1"]);
    await db.collection(WP.COLLECTION).doc(EMPLOYER).set({ [WP.NAME]: "Sita" });
    const id = await postNeed(1, "own");
    await assert.rejects(accept({ requestId: id }, asUser(EMPLOYER, "WORKER")), /own urgent job/);
    assert.equal((await accept({ requestId: "doesnotexist" }, asUser("w1", "WORKER"))).result, "closed");
  });
});
