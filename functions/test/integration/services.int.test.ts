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
// Bhadradri Kothagudem district (next to Khammam): outside the service area.
const KOTHAGUDEM = { lat: 17.55, lng: 80.62 };

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

async function partner(
  uid: string, at: { lat: number; lng: number }, creditsPaise = 50_000, categories = ["AC", "ELECTRICIAN"], jobsCompleted = 5,
) {
  await db.doc(`service_partners/${uid}`).set({
    status: "APPROVED", name: `P ${uid}`, phone: "+919000000002", categories, online: true, lat: at.lat, lng: at.lng,
    lastSeenAt: T.now(), creditsPaise, ratingSum: 0, ratingCount: 0, jobsCompleted,
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
    assert.equal(c.inArea, undefined, "no location sent");
    assert.equal((await call(svc.getServiceCatalog)({ ...HOME }, app)).inArea, true);
    assert.equal((await call(svc.getServiceCatalog)({ ...KOTHAGUDEM }, app)).inArea, false);
    assert.equal(c.bookingFee, 19);
    assert.equal(c.commissionPct, 0);
    assert.ok(c.services.some((s: any) => s.id === "ac_service"));
  });

  it("runs a booking end to end and takes the ₹19 partner fee from credits", async () => {
    await partner("p1", NEAR);
    await partner("p2", FAR);
    const r = await book();
    assert.equal(r.discount, 19, "first booking: no booking fee");
    assert.equal(r.total, 449);
    assert.match(r.startOtp, /^\d{4}$/);
    // Wave 1 (3 km) reaches only the near partner.
    assert.deepEqual(offers.map((o) => o.token), ["tok_p1"]);
    const secret = await db.doc(`service_booking_secrets/${r.bookingId}`).get();
    assert.equal(secret.get("customerId"), "cust1");

    const offer = await call(svc.getServiceOffer)({ bookingId: r.bookingId }, partnerCtx("p1"));
    assert.equal(offer.available, true);
    assert.equal(offer.earning, 449 - 19);
    assert.equal(offer.requiredCreditsPaise, 1900);
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
    // Booking fee ₹19 - first-booking discount ₹19 + partner fee ₹19.
    const take = 1900;
    assert.equal(done.total, 449 + 300);
    assert.equal(done.platformTakePaise, take);
    assert.equal(done.creditsPaise, 50_000 - take);

    const p1 = await db.doc("service_partners/p1").get();
    assert.equal(p1.get("creditsPaise"), 50_000 - take);
    assert.equal(p1.get("jobsCompleted"), 6);
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
    // While on the service job the worker cannot apply for regular jobs either.
    const apps = require("../../src/applications") as typeof import("../../src/applications");
    await db.doc("worker_profiles/p1").set({ name: "Ravi" });
    await db.doc("jobmetadata/job1").set({ employerId: "emp9", title: "Helper", status: "open" });
    await assert.rejects(call(apps.applyToJob)({ jobId: "job1" }, partnerCtx("p1")),
      (e) => code(e) === "failed-precondition" && /Finish your current DutyPe Services job/.test(String((e as Error).message)));
    await call(svc.cancelServiceBooking)({ bookingId: a.bookingId }, customer);
    assert.equal((await db.doc("service_partners/p1").get()).get("activeBookingId"), undefined);
    assert.equal((await call(apps.applyToJob)({ jobId: "job1" }, partnerCtx("p1"))).created, true, "free again after it ends");
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: b.bookingId }, partnerCtx("p1"))).result, "accepted");
  });

  it("checks role, city, open-booking limit and schedule hours", async () => {
    await assert.rejects(call(svc.createServiceBooking)({ serviceId: "ac_service", addressText: "xxxxx", ...HOME }, partnerCtx("w")),
      (e) => code(e) === "permission-denied");
    await assert.rejects(book("ac_service", { lat: 17.385, lng: 78.4867 }), (e) => code(e) === "failed-precondition", "Hyderabad is out of range");
    await assert.rejects(book("ac_service", KOTHAGUDEM), (e) => code(e) === "failed-precondition", "next district is out of range");
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

  it("first-booking offer, coupons (once each, given back on cancel) and the partner's free first job", async () => {
    const now = Date.now();
    await db.doc("app_config/services").set({
      coupons: [
        { code: "DASARA30", title: "Dasara ₹30 off", type: "FLAT", value: 30, validFrom: now - 864e5, validTo: now + 864e5 },
        { code: "HIDDEN", title: "Secret", type: "FLAT", value: 10, visible: false },
        { code: "ENDED", title: "Old", type: "FLAT", value: 10, validTo: now - 1000 },
      ],
    });
    svc.clearConfigCache();
    await partner("rookie", NEAR, 0, ["AC"], 0);

    const pre = await call(svc.previewServiceQuote)({ serviceId: "ac_service" }, customer);
    assert.equal(pre.firstBooking, true);
    assert.equal(pre.discount, 19);
    assert.equal(pre.total, 449);
    assert.deepEqual(pre.offers.map((o: any) => o.code), ["DASARA30"], "hidden and ended offers are not listed");
    const withCode = await call(svc.previewServiceQuote)({ serviceId: "ac_service", couponCode: "dasara30" }, customer);
    assert.equal(withCode.discount, 30);
    assert.equal(withCode.total, 449 + 19 - 30);
    assert.equal((await call(svc.previewServiceQuote)({ serviceId: "ac_service", couponCode: "ENDED" }, customer)).couponError,
      "This offer has ended");
    await assert.rejects(book("ac_service", { couponCode: "NOPE" }), (e) => code(e) === "failed-precondition");

    const r = await book("ac_service", { couponCode: "DASARA30" });
    assert.equal(r.discount, 30);
    assert.equal(r.total, 438);
    // Rookie partner with no credits: first job free except the ₹11 the booking fee cannot cover.
    let offer = await call(svc.getServiceOffer)({ bookingId: r.bookingId }, partnerCtx("rookie"));
    assert.equal(offer.requiredCreditsPaise, 0, "₹19 fee - ₹30 discount + ₹11 partner fee");
    assert.equal(offer.earning, 449 - 11);
    assert.deepEqual(offers.map((o) => o.token), ["tok_rookie"], "zero credits is enough for the free first job");

    // The same coupon cannot be used twice while the first booking holds it.
    assert.equal((await call(svc.previewServiceQuote)({ serviceId: "ac_service", couponCode: "DASARA30" }, customer)).couponError,
      "You have already used this coupon");
    await assert.rejects(book("ac_service", { couponCode: "DASARA30" }), (e) => code(e) === "already-exists");
    // Cancelling gives the coupon back.
    await call(svc.cancelServiceBooking)({ bookingId: r.bookingId }, customer);
    const again = await book("ac_service", { couponCode: "DASARA30" });
    assert.equal(again.discount, 30);

    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: again.bookingId }, partnerCtx("rookie"))).result, "accepted");
    offer = await call(svc.getServiceOffer)({ bookingId: again.bookingId }, partnerCtx("rookie"));
    assert.equal(offer.earning, 449 - 11);
    await db.doc("service_partners/rookie").update({ creditsPaise: 0 });
    await call(svc.updateServiceBooking)({ bookingId: again.bookingId, action: "start", otp: again.startOtp }, partnerCtx("rookie"));
    const done = await call(svc.updateServiceBooking)({ bookingId: again.bookingId, action: "complete" }, partnerCtx("rookie"));
    assert.equal(done.total, 438);
    assert.equal(done.platformTakePaise, 0, "DutyPe never pays out: discount = booking fee + partner fee");
    assert.equal(done.creditsPaise, 0);

    // Second job: the full ₹19 partner fee (+ ₹19 booking fee, no first-booking offer any more).
    const next = await book("ac_service");
    assert.equal(next.discount, 0);
    assert.equal(next.total, 449 + 19);
    offer = await call(svc.getServiceOffer)({ bookingId: next.bookingId }, partnerCtx("rookie"));
    assert.equal(offer.requiredCreditsPaise, 3800);
    assert.equal((await call(svc.acceptServiceBooking)({ bookingId: next.bookingId }, partnerCtx("rookie"))).result, "low_credits");
  });

  it("partner applies, admin approves, top-up is verified into credits", async () => {
    await db.doc("worker_profiles/w1").set({ name: "Ravi", phone: "+919000000003" });
    await assert.rejects(call(svc.applyServicePartner)({ categories: ["AC"], ...KOTHAGUDEM }, partnerCtx("w1")),
      (e) => code(e) === "failed-precondition", "partners must be in Khammam district");
    await call(svc.applyServicePartner)({ categories: ["ac", "PLUMBER", "bogus"], experienceYears: 5, ...NEAR }, partnerCtx("w1"));
    let p = await db.doc("service_partners/w1").get();
    assert.equal(p.get("status"), "PENDING");
    assert.deepEqual(p.get("categories"), ["AC", "PLUMBER"]);
    await assert.rejects(call(svc.setPartnerOnline)({ online: true, ...NEAR }, partnerCtx("w1")), (e) => code(e) === "permission-denied");
    await assert.rejects(call(svc.reviewServicePartner)({ partnerId: "w1", action: "approve" }, partnerCtx("w1")),
      (e) => code(e) === "permission-denied");
    await call(svc.reviewServicePartner)({ partnerId: "w1", action: "approve" }, adminCtx);
    await assert.rejects(call(svc.setPartnerOnline)({ online: true, ...KOTHAGUDEM }, partnerCtx("w1")),
      (e) => code(e) === "failed-precondition", "cannot go online outside the district");
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
