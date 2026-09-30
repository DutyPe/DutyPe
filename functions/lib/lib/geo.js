"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.coveringCells = exports.distanceKm = exports.encodeGeohash = void 0;
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
function distanceKm(lat1, lng1, lat2, lng2) {
    const rad = (d) => d * Math.PI / 180;
    const dLat = rad(lat2 - lat1);
    const dLng = rad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
exports.distanceKm = distanceKm;
/** Geohash prefixes (at `precision`) whose cells cover a circle — used for "workers near a job" queries. */
function coveringCells(lat, lng, radiusKm, precision) {
    const latStep = radiusKm / 111;
    const lngStep = radiusKm / (111 * Math.max(0.2, Math.cos(lat * Math.PI / 180)));
    const cells = new Set();
    const steps = 4;
    for (let i = -steps; i <= steps; i++) {
        for (let j = -steps; j <= steps; j++) {
            cells.add(encodeGeohash(lat + (latStep * i) / steps, lng + (lngStep * j) / steps, precision));
        }
    }
    return Array.from(cells);
}
exports.coveringCells = coveringCells;
//# sourceMappingURL=geo.js.map