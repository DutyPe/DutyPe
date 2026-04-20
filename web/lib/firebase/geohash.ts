/**
 * Geohash encoder used by the admin job-post API to mirror the Android app's
 * `GeoUtils.encodeGeohash` (firebase-geohash, precision 6, ~1.2 km cells).
 *
 * Implementation follows the standard geohash base32 algorithm — output
 * matches Firebase GeoFire / com.firebase.geofire.GeoFireUtils for the same
 * coordinates and precision.
 */
const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

export function encodeGeohash(latitude: number, longitude: number, precision = 6): string {
  let latLow = -90.0;
  let latHigh = 90.0;
  let lngLow = -180.0;
  let lngHigh = 180.0;

  let bit = 0;
  let evenBit = true;
  let charIndex = 0;
  let geohash = "";

  while (geohash.length < precision) {
    if (evenBit) {
      const lngMid = (lngLow + lngHigh) / 2;
      if (longitude >= lngMid) {
        charIndex = (charIndex << 1) + 1;
        lngLow = lngMid;
      } else {
        charIndex = charIndex << 1;
        lngHigh = lngMid;
      }
    } else {
      const latMid = (latLow + latHigh) / 2;
      if (latitude >= latMid) {
        charIndex = (charIndex << 1) + 1;
        latLow = latMid;
      } else {
        charIndex = charIndex << 1;
        latHigh = latMid;
      }
    }

    evenBit = !evenBit;
    bit += 1;

    if (bit === 5) {
      geohash += BASE32[charIndex];
      bit = 0;
      charIndex = 0;
    }
  }

  return geohash;
}

export function hasValidCoordinates(lat: number | null | undefined, lng: number | null | undefined): boolean {
  if (typeof lat !== "number" || typeof lng !== "number") return false;
  if (Number.isNaN(lat) || Number.isNaN(lng)) return false;
  if (lat < -90 || lat > 90) return false;
  if (lng < -180 || lng > 180) return false;
  if (lat === 0 && lng === 0) return false;
  return true;
}
