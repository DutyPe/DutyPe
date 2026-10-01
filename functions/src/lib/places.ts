/**
 * District and state for a point in India, from current LGD boundaries bundled in
 * data/districts.json (built by scripts/build-places.py, ~100 m simplification). No network call,
 * no paid API; the file is parsed once per instance (~3 MB, ~150 ms) on first use.
 *
 * A point that falls in no polygon (a sliver left by simplification, or just off the coast) takes
 * the district whose boundary is nearest, if within 5 km. Anything else is outside India → null.
 */
import { readFileSync } from "fs";
import { join } from "path";
import { distanceKm } from "./geo";

export interface Place {
  districtId: number;
  district: string;
  stateId: number;
  state: string;
}

interface District extends Place {
  box: [number, number, number, number]; // minLng, minLat, maxLng, maxLat
  rings: Float64Array[];                 // flat [lng, lat, lng, lat, ...]
}

const NEAREST_MAX_KM = 5;
const SCALE = 10_000;
let districts: District[] | null = null;

function load(): District[] {
  if (districts) return districts;
  const raw = JSON.parse(readFileSync(join(__dirname, "../../data/districts.json"), "utf8")) as {
    d: Array<[number, string, number, string, [number, number, number, number], number[][]]>;
  };
  districts = raw.d.map(([districtId, district, stateId, state, box, encoded]) => ({
    districtId, district, stateId, state, box,
    rings: encoded.map((ring) => {
      const out = new Float64Array(ring.length);
      let x = 0;
      let y = 0;
      for (let i = 0; i < ring.length; i += 2) {
        x += ring[i];
        y += ring[i + 1];
        out[i] = x / SCALE;
        out[i + 1] = y / SCALE;
      }
      return out;
    }),
  }));
  return districts;
}

function inRing(ring: Float64Array, lng: number, lat: number): boolean {
  let inside = false;
  for (let i = 0, j = ring.length - 2; i < ring.length; j = i, i += 2) {
    const xi = ring[i]; const yi = ring[i + 1];
    const xj = ring[j]; const yj = ring[j + 1];
    if ((yi > lat) !== (yj > lat) && lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

/** Distance (km) from a point to the nearest vertex of a district's outline — fine at ~100 m spacing. */
function nearestVertexKm(d: District, lng: number, lat: number): number {
  let best = Infinity;
  for (const ring of d.rings) {
    for (let i = 0; i < ring.length; i += 2) {
      if (Math.abs(ring[i + 1] - lat) > 0.1 || Math.abs(ring[i] - lng) > 0.1) continue;
      best = Math.min(best, distanceKm(lat, lng, ring[i + 1], ring[i]));
    }
  }
  return best;
}

function toPlace(d: District): Place {
  return { districtId: d.districtId, district: d.district, stateId: d.stateId, state: d.state };
}

export function placeOf(lat: number, lng: number): Place | null {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null;
  const all = load();
  const near: District[] = [];
  for (const d of all) {
    const [minLng, minLat, maxLng, maxLat] = d.box;
    if (lng < minLng - 0.05 || lng > maxLng + 0.05 || lat < minLat - 0.05 || lat > maxLat + 0.05) continue;
    near.push(d);
    if (lng >= minLng && lng <= maxLng && lat >= minLat && lat <= maxLat && d.rings.some((r) => inRing(r, lng, lat))) {
      return toPlace(d);
    }
  }
  let best: District | null = null;
  let bestKm = NEAREST_MAX_KM;
  for (const d of near) {
    const km = nearestVertexKm(d, lng, lat);
    if (km < bestKm) { bestKm = km; best = d; }
  }
  return best ? toPlace(best) : null;
}
