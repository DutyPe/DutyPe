import { describe, it } from "node:test";
import * as assert from "node:assert/strict";
import {
  CATEGORIES, DEFAULT_CONFIG, DEFAULT_SERVICES, bookingFeeFor, findService, mergeConfig, newStartOtp, platformTakePaise,
} from "../src/lib/service-catalog";

describe("service catalog", () => {
  it("has the 5 launch categories, each with services and unique ids", () => {
    assert.deepEqual(CATEGORIES.map((c) => c.id), ["AC", "CLEANING", "ELECTRICIAN", "PLUMBER", "APPLIANCE"]);
    for (const c of CATEGORIES) assert.ok(DEFAULT_SERVICES.filter((s) => s.category === c.id).length >= 4, c.id);
    assert.equal(new Set(DEFAULT_SERVICES.map((s) => s.id)).size, DEFAULT_SERVICES.length);
    for (const s of DEFAULT_SERVICES) assert.ok(s.price > 0 && s.te && s.hi && s.includes, s.id);
  });

  it("uses ₹19 for fixed jobs and ₹49 for inspection visits", () => {
    const fixed = findService(DEFAULT_CONFIG, "ac_service")!;
    const visit = findService(DEFAULT_CONFIG, "wm_repair")!;
    assert.equal(bookingFeeFor(DEFAULT_CONFIG, fixed), 19);
    assert.equal(bookingFeeFor(DEFAULT_CONFIG, visit), 49);
  });

  it("takes booking fee + commission on price and extras, in paise", () => {
    assert.equal(platformTakePaise(449, 0, 19, 10), 1900 + 4490);
    assert.equal(platformTakePaise(449, 300, 19, 15), 1900 + Math.round(749 * 15));
    assert.equal(platformTakePaise(99, 0, 19, 0), 1900);
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
    assert.equal(c.inspectionFee, 49);
    assert.equal(c.upiId, "dutype@upi");
    assert.equal(findService(c, "ac_service")!.price, 499);
    assert.equal(findService(c, "elec_fan"), null, "inactive services cannot be booked");
    assert.equal(findService(c, "plumb_tap")!.price, 129);
    assert.equal(findService(c, "new_one")!.price, 299);
    assert.equal(findService(c, "bad_new"), null);
    assert.deepEqual(mergeConfig(null).services, DEFAULT_SERVICES);
  });

  it("makes 4-digit start codes", () => {
    for (let i = 0; i < 200; i++) assert.match(newStartOtp(), /^\d{4}$/);
  });
});
