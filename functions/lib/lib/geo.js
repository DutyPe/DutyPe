"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.coarse = exports.coveringCells = exports.distanceToBoxKm = exports.distanceKm = exports.decodeGeohash = exports.encodeGeohash = void 0;
const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
/** Standard geohash; precision 9 (~5 m) is what documents store. */
function encodeGeohash(lat, lng, precision = 9) {
    let latMin = -90;
    let latMax = 90;
    let lngMin = -180;
    let lngMax = 180;
    let hash = "";
    let bit = 0;
    let ch = 0;
    let even = true;
    while (hash.length < precision) {
        if (even) {
            const mid = (lngMin + lngMax) / 2;
            if (lng >= mid) {
                ch = (ch << 1) | 1;
                lngMin = mid;
            }
            else {
                ch = ch << 1;
                lngMax = mid;
            }
        }
        else {
            const mid = (latMin + latMax) / 2;
            if (lat >= mid) {
                ch = (ch << 1) | 1;
                latMin = mid;
            }
            else {
                ch = ch << 1;
                latMax = mid;
            }
        }
        even = !even;
        if (++bit === 5) {
            hash += BASE32[ch];
            bit = 0;
            ch = 0;
        }
    }
    return hash;
}
exports.encodeGeohash = encodeGeohash;
function decodeGeohash(hash) {
    let minLat = -90;
    let maxLat = 90;
    let minLng = -180;
    let maxLng = 180;
    let even = true;
    for (const c of hash) {
        const value = BASE32.indexOf(c);
        if (value < 0)
            break;
        for (let shift = 4; shift >= 0; shift--) {
            const on = ((value >> shift) & 1) === 1;
            if (even) {
                const mid = (minLng + maxLng) / 2;
                if (on)
                    minLng = mid;
                else
                    maxLng = mid;
            }
            else {
                const mid = (minLat + maxLat) / 2;
                if (on)
                    minLat = mid;
                else
                    maxLat = mid;
            }
            even = !even;
        }
    }
    return { minLat, maxLat, minLng, maxLng };
}
exports.decodeGeohash = decodeGeohash;
function distanceKm(lat1, lng1, lat2, lng2) {
    const rad = (d) => d * Math.PI / 180;
    const dLat = rad(lat2 - lat1);
    const dLng = rad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
exports.distanceKm = distanceKm;
/** Shortest distance (km) from a point to a cell; 0 inside it. */
function distanceToBoxKm(lat, lng, box) {
    return distanceKm(lat, lng, Math.min(Math.max(lat, box.minLat), box.maxLat), Math.min(Math.max(lng, box.minLng), box.maxLng));
}
exports.distanceToBoxKm = distanceToBoxKm;
/**
 * Every geohash cell at `precision` that touches the circle, walked cell by cell over the
 * circle's bounding box (so no edge cell is ever missed). Same algorithm as the app's Geohash.
 */
function coveringCells(lat, lng, radiusKm, precision) {
    const home = decodeGeohash(encodeGeohash(lat, lng, precision));
    const latStep = home.maxLat - home.minLat;
    const lngStep = home.maxLng - home.minLng;
    const latSteps = Math.ceil(radiusKm / (latStep * 111)) + 1;
    const lngSteps = Math.ceil(radiusKm / (lngStep * 111 * Math.max(0.2, Math.cos(lat * Math.PI / 180)))) + 1;
    const out = new Set();
    for (let i = -latSteps; i <= latSteps; i++) {
        for (let j = -lngSteps; j <= lngSteps; j++) {
            const pLat = Math.min(89.999, Math.max(-89.999, (home.minLat + home.maxLat) / 2 + i * latStep));
            const pLng = (home.minLng + home.maxLng) / 2 + j * lngStep;
            const hash = encodeGeohash(pLat, pLng, precision);
            if (distanceToBoxKm(lat, lng, decodeGeohash(hash)) <= radiusKm)
                out.add(hash);
        }
    }
    return Array.from(out);
}
exports.coveringCells = coveringCells;
/** A point rounded to ~1.1 km — what public cards show instead of someone's home. */
function coarse(value) {
    return Math.round(value * 100) / 100;
}
exports.coarse = coarse;
//# sourceMappingURL=geo.js.map