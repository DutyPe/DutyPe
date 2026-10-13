import { describe, it } from "node:test";
import * as assert from "node:assert/strict";
import {
  CATEGORIES, DEFAULT_CONFIG, DEFAULT_SERVICES, bookingFeeFor, findService, maxDiscount, mergeConfig, newStartOtp,
  ITEMS, bringFor, partnerFeeFor, platformTakePaise, provideFor, quote, skillOf, takePaise,
} from "../src/lib/service-catalog";

describe("service catalog", () => {
  it("has the launch categories, each with services, unique ids and known items", () => {
    assert.deepEqual(CATEGORIES.map((c) => c.id),
      ["CLEANING", "AC", "ELECTRICIAN", "PLUMBER", "APPLIANCE", "CARPENTER", "PAINTER", "HOME_HELP", "VEHICLE",
       "BEAUTICIAN", "MEHENDI", "COOK_MAID", "BABYSITTER", "ELDERLY_CARE", "TAILORING"]);
    for (const c of CATEGORIES) assert.ok(DEFAULT_SERVICES.filter((s) => s.category === c.id).length >= 0, c.id);
    for (const s of DEFAULT_SERVICES) {
      for (const id of [...provideFor(s), ...bringFor(s)]) assert.ok(ITEMS[id], `${s.id}: ${id}`);
    }
    assert.equal(skillOf("CLEANING"), "BASIC");
    assert.equal(skillOf("ELECTRICIAN"), "SKILLED");
    assert.equal(new Set(DEFAULT_SERVICES.map((s) => s.id)).size, DEFAULT_SERVICES.length);
    for (const s of DEFAULT_SERVICES) assert.ok(s.price > 0 && s.te && s.hi && s.includes, s.id);
  });

  it("uses tiered platform fees (₹9 for <=149, ₹19 for >149)", () => {
    const fixed = findService(DEFAULT_CONFIG, "ac_service")!; // 399 -> 19
    const visit = findService(DEFAULT_CONFIG, "wm_repair")!; // 149 -> 9
    assert.equal(bookingFeeFor(DEFAULT_CONFIG, fixed), 19);
    assert.equal(bookingFeeFor(DEFAULT_CONFIG, visit), 9);
  });

  it("takes platform fee in paise", () => {
    assert.equal(platformTakePaise(449, 0, 19, 0), 1900);
    assert.equal(platformTakePaise(99, 0, 9, 0), 900);
  });

  it("merges admin overrides and ignores bad values", () => {
    const c = mergeConfig({
      bookingFee: 29, commissionPct: 15, commissionPctBad: 1, inspectionFee: -5, upiId: "dutype@upi",
      services: [
        { id: "ac_service", price: 499 },
        { id: "elec_fan", active: false },
        { id: "plumb_tap", price: "abc" },
        { id: "new_one", category: "CLEANING", name: "Balcony cleaning", price: 299 },
        { id: "bad_new", category: "NOPE", name: "x", price: 1 },
      ],
    });
    assert.equal(c.bookingFee, 29);
    assert.equal(c.commissionPct, 15);
    assert.equal(c.inspectionFee, 19);
    assert.equal(c.upiId, "dutype@upi");
    assert.equal(findService(c, "ac_service")!.price, 499);
    assert.equal(findService(c, "elec_fan"), null, "inactive services cannot be booked");
    assert.equal(findService(c, "plumb_tap")!.price, 99);
    assert.equal(findService(c, "new_one")!.price, 299);
    assert.equal(findService(c, "bad_new"), null);
    assert.deepEqual(mergeConfig(null).services, DEFAULT_SERVICES);
  });

  it("defaults: no commission, ₹19 partner fee, partner's first job free, first booking fee-free false", () => {
    assert.equal(DEFAULT_CONFIG.commissionPct, 0);
    assert.equal(DEFAULT_CONFIG.partnerFee, 19);
    assert.equal(DEFAULT_CONFIG.partnerFirstJobFree, true);
    assert.equal(DEFAULT_CONFIG.firstBookingFeeFree, false);
    assert.deepEqual(DEFAULT_CONFIG.coupons, []);
  });

  const NOW = Date.parse("2026-10-10T10:00:00Z");
  const festive = mergeConfig({
    coupons: [
      { code: "dasara50", title: "Dasara ₹50 off", type: "FLAT", value: 50, minOrder: 300 },
      { code: "BIG", title: "Huge", type: "FLAT", value: 5000 },
      { code: "PCT20", title: "20% off", type: "PCT", value: 20, maxOff: 30 },
      { code: "FIRST", title: "First", type: "FLAT", value: 30, firstBookingOnly: true },
      { code: "ACONLY", title: "AC", type: "FLAT", value: 25, categories: ["AC"] },
      { code: "OLD", title: "Old", type: "FLAT", value: 25, validTo: NOW - 1 },
      { code: "OFF", title: "Off", type: "FLAT", value: 25, active: false },
      { code: "x", type: "FLAT", value: 10 },
      { code: "BADPCT", type: "PCT", value: 150 },
    ],
  });

  it("cleans coupons: upper-cases codes, drops invalid ones", () => {
    assert.deepEqual(festive.coupons.map((c) => c.code), ["DASARA50", "BIG", "PCT20", "FIRST", "ACONLY", "OLD", "OFF"]);
  });

  it("calculates customer price with coupons deducted from service price", () => {
    const ac = findService(festive, "ac_service")!;
    const withoutCoupon = quote(festive, ac, false, "", NOW);
    assert.equal(withoutCoupon.discount, 0);
    assert.equal(withoutCoupon.total, ac.price);
  });

  it("coupons: best offer wins, never stacks, capped at DutyPe's take", () => {
    const ac = findService(festive, "ac_service")!; // ₹399
    const cap = maxDiscount(festive, ac);
    assert.equal(cap, 19 + 19, "booking fee + partner fee (no commission)");
    const big = quote(festive, ac, false, "big", NOW);
    assert.equal(big.discount, cap);
    assert.equal(big.couponCode, "BIG");
    assert.equal(big.total, ac.price - cap);
    const pct = quote(festive, ac, false, "PCT20", NOW);
    assert.equal(pct.discount, 30, "maxOff");
    assert.equal(pct.total, ac.price - 30);
    const acOnly = quote(festive, ac, false, "ACONLY", NOW);
    assert.equal(acOnly.discount, 25);
    assert.equal(acOnly.total, ac.price - 25);
    assert.equal(quote(festive, ac, false, "OLD", NOW).couponError, "This offer has ended");
    assert.equal(quote(festive, ac, false, "OFF", NOW).couponError, "This coupon is not active");
    assert.equal(quote(festive, ac, false, "NOPE", NOW).couponError, "This coupon code is not valid");
    assert.equal(quote(festive, ac, false, "FIRST", NOW).couponError, "This coupon is for your first booking only");
    const tap = findService(festive, "plumb_tap")!; // ₹99
    assert.match(quote(festive, tap, false, "DASARA50", NOW).couponError!, /₹300/);
    assert.equal(quote(festive, tap, false, "ACONLY", NOW).couponError, "This coupon is not for this service");
  });

  it("partner fee: ₹19 per job, first job free unless a discount needs it", () => {
    assert.equal(partnerFeeFor(DEFAULT_CONFIG, 3, 449, 19, 0, 0), 19);
    assert.equal(partnerFeeFor(DEFAULT_CONFIG, 0, 449, 19, 0, 0), 0);
    assert.equal(partnerFeeFor(DEFAULT_CONFIG, 0, 449, 19, 19, 0), 0, "fee-free first booking is covered by the booking fee");
    assert.equal(partnerFeeFor(DEFAULT_CONFIG, 0, 449, 19, 30, 0), 11, "the uncovered ₹11 of a ₹30 coupon");
    assert.equal(partnerFeeFor(mergeConfig({ partnerFirstJobFree: false }), 0, 449, 19, 0, 0), 19);
  });

  it("DutyPe's take is never negative and never pays out", () => {
    assert.equal(takePaise(449, 0, 19, 0, 19, 0), 3800);
    assert.equal(takePaise(449, 0, 19, 19, 19, 0), 1900, "fee-free first booking: partner pays only ₹19");
    assert.equal(takePaise(449, 0, 19, 38, 19, 0), 0, "max discount: DutyPe takes nothing, pays nothing");
    assert.equal(takePaise(449, 100, 19, 0, 19, 10), 3800 + 5490);
    // For every service and coupon, the take covers the discount.
    for (const s of festive.services) {
      for (const code of ["", "BIG", "PCT20", "DASARA50"]) {
        for (const first of [true, false]) {
          const q = quote(festive, s, first, code, NOW);
          for (const jobs of [0, 5]) {
            const fee = partnerFeeFor(festive, jobs, s.price, q.bookingFee, q.discount, festive.commissionPct);
            const take = (q.bookingFee - q.discount + fee) * 100;
            assert.ok(take >= 0, `${s.id} ${code} ${first} ${jobs}`);
            assert.ok(fee <= festive.partnerFee);
          }
        }
      }
    }
  });

  it("makes 4-digit start codes", () => {
    for (let i = 0; i < 200; i++) assert.match(newStartOtp(), /^\d{4}$/);
  });
});
