import { describe, it } from "node:test";
import * as assert from "node:assert/strict";

import { placeOf } from "../src/lib/places";
import { coveringCells, decodeGeohash, distanceKm, distanceToBoxKm, encodeGeohash } from "../src/lib/geo";

describe("placeOf", () => {
  const cases: Array<[string, number, number, string, string]> = [
    ["Khammam town", 17.2477, 80.1437, "Khammam", "Telangana"],
    ["Kothagudem", 17.551, 80.618, "Bhadradri Kothagudem", "Telangana"],
    ["Charminar, Hyderabad", 17.3616, 78.4747, "Hyderabad", "Telangana"],
    ["Jaggaiahpet (AP, near the Khammam border)", 16.8927, 80.0976, "NTR", "Andhra Pradesh"],
    ["Vijayawada", 16.5062, 80.648, "NTR", "Andhra Pradesh"],
    ["Bengaluru MG Road", 12.9756, 77.6066, "Bengaluru Urban", "Karnataka"],
  ];
  for (const [label, lat, lng, district, state] of cases) {
    it(label, () => {
      const place = placeOf(lat, lng);
      assert.ok(place, `${label}: no place`);
      assert.equal(place.state, state);
      assert.equal(place.district, district);
    });
  }

  it("returns null outside India", () => {
    assert.equal(placeOf(15, 68), null);            // Arabian Sea
    assert.equal(placeOf(6.9271, 79.8612), null);   // Colombo
    assert.equal(placeOf(Number.NaN, 80), null);
  });

  it("is fast after the first call", () => {
    placeOf(17.2477, 80.1437);
    const start = Date.now();
    for (let i = 0; i < 200; i++) placeOf(17 + i / 400, 79 + i / 300);
    assert.ok(Date.now() - start < 1000, "200 lookups took over a second");
  });
});

describe("coveringCells", () => {
  const lat = 17.2477;
  const lng = 80.1437;

  it("finds every cell that touches the circle and nothing outside it", () => {
    for (const radius of [5, 10, 15, 20]) {
      const cells = new Set(coveringCells(lat, lng, radius, 5));
      assert.ok(cells.size <= 90, `${radius} km -> ${cells.size} cells`);
      for (const cell of cells) assert.ok(distanceToBoxKm(lat, lng, decodeGeohash(cell)) <= radius);
      // Points all around the circle, just inside it, must land in a covering cell.
      for (let deg = 0; deg < 360; deg += 7.5) {
        const r = radius * 0.999;
        const pLat = lat + (r / 111) * Math.cos(deg * Math.PI / 180);
        const pLng = lng + (r / (111 * Math.cos(lat * Math.PI / 180))) * Math.sin(deg * Math.PI / 180);
        assert.ok(distanceKm(lat, lng, pLat, pLng) <= radius + 0.05);
        assert.ok(cells.has(encodeGeohash(pLat, pLng, 5)), `${radius} km ring missed bearing ${deg}`);
      }
    }
  });
});

describe("urgent offer topics", () => {
  it("match the app's topic format", async () => {
    const admin = await import("firebase-admin");
    if (!admin.apps.length) admin.initializeApp({ projectId: "demo-dutype" });
    const { urgentTopic, nextWaveKm } = await import("../src/urgent");
    assert.equal(urgentTopic("tepg9", "te"), "urgent_tepg9_te");
    assert.deepEqual([0, 5, 10, 15, 20].map(nextWaveKm), [5, 10, 15, 20, null]);
  });
});
