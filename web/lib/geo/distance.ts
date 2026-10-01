/** Coordinates are valid when both are finite, in range, and not the (0, 0) "unset" pair. */
export function hasValidCoordinates(latitude: unknown, longitude: unknown): boolean {
  const lat = Number(latitude);
  const lng = Number(longitude);
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) return false;
  return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
}

/** Great-circle distance (haversine), km. */
export function distanceBetweenKm(fromLat: number, fromLng: number, toLat: number, toLng: number): number {
  const rad = (d: number) => (d * Math.PI) / 180;
  const dLat = rad(toLat - fromLat);
  const dLng = rad(toLng - fromLng);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(fromLat)) * Math.cos(rad(toLat)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
