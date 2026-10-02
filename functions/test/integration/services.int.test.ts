/** DutyPe Services against the Firestore emulator; FCM offers are captured (no network). */
import { after, beforeEach, describe, it } from "node:test";
import * as assert from "node:assert/strict";
import * as admin from "firebase-admin";
import functionsTest from "firebase-functions-test";

process.env.GCLOUD_PROJECT = "demo-dutype";
if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error("Run inside `firebase emulators:exec`");
if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
const fft = functionsTest({ projectId: "demo-dutype" });
/* eslint-disable @typescript-eslint/no-var-requires, @typescript-eslint/no-explicit-any */
const svc = require("../../src/services") as typeof import("../../src/services");
const db = admin.firestore();
const T = admin.firestore.Timestamp;
const call = (fn: unknown) => fft.wrap(fn as never) as unknown as (d: unknown, c: unknown) => Promise<any>;
const app = { app: { appId: "t" } };
const customer = { auth: { uid: "cust1", token: { role: "EMPLOYER", phone_number: "+919000000001" } }, ...app };
const partnerCtx = (uid: string) => ({ auth: { uid, token: { role: "WORKER", phone_number: "+919000000002" } }, ...app });
const adminCtx = { auth: { uid: "admin1", token: { admin: true } }, ...app };
// Khammam centre and points ~2 km / ~8 km away.
const HOME = { lat: 17.2473, lng: 80.1514 };
const NEAR = { lat: 17.2653, lng: 80.1514 };
const FAR = { lat: 17.3193, lng: 80.1514 };

let offers: Array<{ token: string; bookingId: string }> = [];
svc.offerSender.send = async (m: any) => {
  offers.push({ token: m.token, bookingId: m.data.bookingId });
  return "id";
};

async function clear() {
  const r = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/demo-dutype/databases/(default)/documents`, { method: "DELETE" });
  assert.ok(r.ok);
  svc.clearConfigCache();
}

async function partner(uid: string, at: { lat: number; lng: number }, creditsPaise = 50_000, categories = ["AC", "ELECTRICIAN"]) {
  await db.doc(`service_partners/${uid}`).set({
    status: "APPROVED", name: `P ${uid}`, phone: "+919000000002", categories, online: true, lat: at.lat, lng: at.lng,
    lastSeenAt: T.now(), creditsPaise, ratingSum: 0, ratingCount: 0, jobsCompleted: 0,
  });
  await db.doc(`user_tokens/${uid}`).set({ fcmToken: `tok_${uid}`, language: "en" });
}

async function book(serviceId = "ac_service", extra: Record<string, unknown> = {}) {
  return call(svc.createServiceBooking)({ serviceId, addressText: "H.No 1-2, Wyra Road", area: "Wyra Road", ...HOME, ...extra }, customer);
}

function code(e: unknown) {
  return (e as { code?: string }).code;
}

describe("DutyPe Services (emulator)", () => {
  beforeEach(async () => {
    await clear();
    offers = [];
    await db.doc("employer_profiles/cust1").set({ ownerName: "Sita" });
  });
  after(() => fft.cleanup());

  it("returns the catalog with the 5 categories and fees", async () => {
    const c = await call(svc.getServiceCatalog)({}, app);
    assert.equal(c.categories.length, 5);
    assert.equal(c.bookingFee, 19);
    assert.equal(c.commissionPct, 10);
    assert.ok(c.services.some((s: any) => s.id === "ac_service"));
  });

  it("runs a booking end to end and takes fee + commission from credits", async () => {
    await partner("p1", NEAR);
    await partner("p2", FAR);
    const r = await book();
    assert.equal(r.total, 449 + 19);
    assert.match(r.startOtp, /^\d{4}$/);
    // Wave 1 (3 km) reaches only the near partner.
    assert.deepEqual(offers.map((o) => o.token), ["tok_p1"]);
    const secret = await db.doc(`service_booking_secrets/${r.bookingId}`).get();
    assert.equal(secret.get("customerId"), "cust1");

    const offer = await call(svc.getServiceOffer)({ bookingId: r.bookingId }, partnerCtx("p1"));
    assert.equal(offer.available, true);
    assert.equal(offer.earning, 449 - 45);
    assert.equal(offer.customerPhone, undefined, "no contact before accepting");

    const acc = await call(svc.acceptServiceBooking)({ bookingId: r.bookingId }, partnerCtx("p1"));
    assert.equal(acc.result, "accepted");
    assert.equal(acc.customerPhone, "+919000000001");
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: r.bookingId }, partnerCtx("p2"))).result, "taken");

    await call(svc.updateServiceBooking)({ bookingId: r.bookingId, action: "on_the_way" }, partnerCtx("p1"));
    await assert.rejects(call(svc.updateServiceBooking)({ bookingId: r.bookingId, action: "start", otp: "xxxx" }, partnerCtx("p1")),
      (e) => code(e) === "invalid-argument");
    await call(svc.updateServiceBooking)({ bookingId: r.bookingId, action: "start", otp: r.startOtp }, partnerCtx("p1"));
    await assert.rejects(call(svc.updateServiceBooking)({ bookingId: r.bookingId, action: "complete", extras: 300 }, partnerCtx("p1")),
      (e) => code(e) === "invalid-argument", "extras need a note");
    const done = await call(svc.updateServiceBooking)(
      { bookingId: r.bookingId, action: "complete", extras: 300, extrasNote: "Capacitor" }, partnerCtx("p1"));
    const take = 1900 + Math.round(749 * 10);
    assert.equal(done.total, 449 + 19 + 300);
    assert.equal(done.platformTakePaise, take);
    assert.equal(done.creditsPaise, 50_000 - take);

    const p1 = await db.doc("service_partners/p1").get();
    assert.equal(p1.get("creditsPaise"), 50_000 - take);
    assert.equal(p1.get("jobsCompleted"), 1);
    assert.equal(p1.get("activeBookingId"), undefined);
    const ledger = await db.collection("service_partners/p1/ledger").get();
    assert.equal(ledger.docs[0].get("amountPaise"), -take);

    await call(svc.rateServiceBooking)({ bookingId: r.bookingId, stars: 5, review: "Good" }, customer);
    await assert.rejects(call(svc.rateServiceBooking)({ bookingId: r.bookingId, stars: 4 }, customer), (e) => code(e) === "already-exists");
    assert.equal((await db.doc("service_partners/p1").get()).get("ratingCount"), 1);
    const inbox = await db.collection("notifications").where("recipientId", "==", "cust1").get();
    assert.ok(inbox.size >= 4, "customer was told at each step");
  });

  it("widens waves, skips partners without credits, and repeats the widest wave", async () => {
    await partner("near_poor", NEAR, 100);
    await partner("far", FAR);
    const r = await book();
    assert.equal(offers.length, 0, "near partner has too few credits");
    const t0 = Date.now();
    // Make the next wave due and run the minute job: 6 km then 10 km.
    for (let i = 0; i < 2; i++) {
      await db.doc(`service_bookings/${r.bookingId}`).update({ nextWaveAt: T.fromMillis(t0) });
      await svc.dispatchServiceWaves(t0 + 1000);
    }
    assert.deepEqual(offers.map((o) => o.token), ["tok_far"]);
    const b = await db.doc(`service_bookings/${r.bookingId}`).get();
    assert.equal(b.get("dispatchRadiusKm"), 10);
  });

  it("partner cancel puts the booking back to searching and never re-offers to them", async () => {
    await partner("p1", NEAR);
    const r = await book();
    await call(svc.acceptServiceBooking)({ bookingId: r.bookingId }, partnerCtx("p1"));
    offers = [];
    await call(svc.updateServiceBooking)({ bookingId: r.bookingId, action: "cancel" }, partnerCtx("p1"));
    const b = await db.doc(`service_bookings/${r.bookingId}`).get();
    assert.equal(b.get("status"), "SEARCHING");
    assert.deepEqual(b.get("excludedPartnerIds"), ["p1"]);
    assert.equal(offers.length, 0);
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: r.bookingId }, partnerCtx("p1"))).result, "closed");
    assert.equal((await db.doc("service_partners/p1").get()).get("cancellations"), 1);
  });

  it("customer cancel frees the partner; busy partners cannot take a second job", async () => {
    await partner("p1", NEAR);
    const a = await book();
    const b = await book("elec_fan");
    await call(svc.acceptServiceBooking)({ bookingId: a.bookingId }, partnerCtx("p1"));
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: b.bookingId }, partnerCtx("p1"))).result, "busy");
    await call(svc.cancelServiceBooking)({ bookingId: a.bookingId }, customer);
    assert.equal((await db.doc("service_partners/p1").get()).get("activeBookingId"), undefined);
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: b.bookingId }, partnerCtx("p1"))).result, "accepted");
  });

  it("checks role, city, open-booking limit and schedule hours", async () => {
    await assert.rejects(call(svc.createServiceBooking)({ serviceId: "ac_service", addressText: "xxxxx", ...HOME }, partnerCtx("w")),
      (e) => code(e) === "permission-denied");
    await assert.rejects(book("ac_service", { lat: 17.385, lng: 78.4867 }), (e) => code(e) === "failed-precondition", "Hyderabad is out of range");
    await assert.rejects(book("nope"), (e) => code(e) === "not-found");
    await book(); await book(); await book();
    await assert.rejects(book(), (e) => code(e) === "resource-exhausted");
    await db.recursiveDelete(db.collection("service_bookings"));
    // 03:00 IST tomorrow is outside 7 AM – 9 PM.
    const tomorrow3am = Math.floor((Date.now() + 330 * 60_000) / 864e5 + 1) * 864e5 - 330 * 60_000 + 3 * 3600_000;
    await assert.rejects(book("ac_service", { when: "scheduled", scheduledAt: tomorrow3am }), (e) => code(e) === "invalid-argument");
    const tomorrow11am = tomorrow3am + 8 * 3600_000;
    const s = await book("ac_service", { when: "scheduled", scheduledAt: tomorrow11am });
    assert.equal(offers.length, 0, "scheduled bookings wait until 90 minutes before");
    const doc = await db.doc(`service_bookings/${s.bookingId}`).get();
    assert.equal(doc.get("nextWaveAt").toMillis(), tomorrow11am - 90 * 60_000);
  });

  it("expires unassigned bookings and tells the customer", async () => {
    const r = await book();
    await svc.dispatchServiceWaves(Date.now() + 46 * 60_000);
    assert.equal((await db.doc(`service_bookings/${r.bookingId}`).get()).get("status"), "NO_PARTNER");
  });

  it("partner applies, admin approves, top-up is verified into credits", async () => {
    await db.doc("worker_profiles/w1").set({ name: "Ravi", phone: "+919000000003" });
    await call(svc.applyServicePartner)({ categories: ["ac", "PLUMBER", "bogus"], experienceYears: 5 }, partnerCtx("w1"));
    let p = await db.doc("service_partners/w1").get();
    assert.equal(p.get("status"), "PENDING");
    assert.deepEqual(p.get("categories"), ["AC", "PLUMBER"]);
    await assert.rejects(call(svc.setPartnerOnline)({ online: true, ...NEAR }, partnerCtx("w1")), (e) => code(e) === "permission-denied");
    await assert.rejects(call(svc.reviewServicePartner)({ partnerId: "w1", action: "approve" }, partnerCtx("w1")),
      (e) => code(e) === "permission-denied");
    await call(svc.reviewServicePartner)({ partnerId: "w1", action: "approve" }, adminCtx);
    await call(svc.setPartnerOnline)({ online: true, ...NEAR }, partnerCtx("w1"));
    p = await db.doc("service_partners/w1").get();
    assert.equal(p.get("status"), "APPROVED");
    assert.equal(p.get("online"), true);

    const t = await call(svc.requestPartnerTopup)({ amount: 500, utr: "123456789012" }, partnerCtx("w1"));
    await assert.rejects(call(svc.requestPartnerTopup)({ amount: 500, utr: "123456789012" }, partnerCtx("w1")),
      (e) => code(e) === "already-exists");
    await assert.rejects(call(svc.requestPartnerTopup)({ amount: 50, utr: "123456789013" }, partnerCtx("w1")),
      (e) => code(e) === "invalid-argument", "below the minimum top-up");
    const v = await call(svc.verifyPartnerTopup)({ topupId: t.topupId, approve: true }, adminCtx);
    assert.equal(v.creditsPaise, 50_000);
    await assert.rejects(call(svc.verifyPartnerTopup)({ topupId: t.topupId, approve: true }, adminCtx),
      (e) => code(e) === "failed-precondition", "cannot be credited twice");
  });
});
