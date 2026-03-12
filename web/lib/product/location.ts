import type { ProductJob } from "./marketplace";
import type { ProductUserProfile } from "./profile";

export type ProductCoordinates = {
  label: string;
  latitude: number;
  longitude: number;
};

export type LocatedProductJob = {
  distanceKm: number | null;
  job: ProductJob;
};

function normalizeNumber(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  return 0;
}

export function hasValidCoordinates(latitude: unknown, longitude: unknown): boolean {
  const normalizedLatitude = normalizeNumber(latitude);
  const normalizedLongitude = normalizeNumber(longitude);

  if (!normalizedLatitude && !normalizedLongitude) {
    return false;
  }

  return (
    normalizedLatitude >= -90 &&
    normalizedLatitude <= 90 &&
    normalizedLongitude >= -180 &&
    normalizedLongitude <= 180
  );
}

export function workerLocationFromProfile(
  profile: ProductUserProfile | null | undefined
): ProductCoordinates | null {
  if (!hasValidCoordinates(profile?.latitude, profile?.longitude)) {
    return null;
  }

  return {
    label:
      profile?.currentLocationLabel?.trim() ||
      profile?.currentLocationAddress?.trim() ||
      "Saved location",
    latitude: normalizeNumber(profile?.latitude),
    longitude: normalizeNumber(profile?.longitude)
  };
}

export function employerBaseLocationFromProfile(
  profile: ProductUserProfile | null | undefined
): ProductCoordinates | null {
  if (!hasValidCoordinates(profile?.businessLatitude, profile?.businessLongitude)) {
    return null;
  }

  return {
    label: profile?.businessAddress?.trim() || "Business location",
    latitude: normalizeNumber(profile?.businessLatitude),
    longitude: normalizeNumber(profile?.businessLongitude)
  };
}

export function distanceBetweenKm(
  fromLatitude: number,
  fromLongitude: number,
  toLatitude: number,
  toLongitude: number
): number {
  const earthRadiusKm = 6371;
  const latitudeDelta = toRadians(toLatitude - fromLatitude);
  const longitudeDelta = toRadians(toLongitude - fromLongitude);

  const a =
    Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2) +
    Math.cos(toRadians(fromLatitude)) *
      Math.cos(toRadians(toLatitude)) *
      Math.sin(longitudeDelta / 2) *
      Math.sin(longitudeDelta / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return earthRadiusKm * c;
}

function toRadians(value: number): number {
  return (value * Math.PI) / 180;
}

export function jobDistanceKm(
  job: ProductJob,
  location: ProductCoordinates | null
): number | null {
  if (!location || !hasValidCoordinates(job.latitude, job.longitude)) {
    return null;
  }

  return distanceBetweenKm(
    location.latitude,
    location.longitude,
    normalizeNumber(job.latitude),
    normalizeNumber(job.longitude)
  );
}

export function attachJobDistances(
  jobs: ProductJob[],
  location: ProductCoordinates | null
): LocatedProductJob[] {
  return [...jobs]
    .map((job) => ({
      distanceKm: jobDistanceKm(job, location),
      job
    }))
    .sort((left, right) => {
      if (left.distanceKm === null && right.distanceKm === null) {
        return 0;
      }

      if (left.distanceKm === null) {
        return 1;
      }

      if (right.distanceKm === null) {
        return -1;
      }

      return left.distanceKm - right.distanceKm;
    });
}

export function filterJobsByRadius(
  jobs: LocatedProductJob[],
  radiusKm: number | null
): LocatedProductJob[] {
  if (radiusKm === null) {
    return jobs;
  }

  return jobs.filter((item) => item.distanceKm !== null && item.distanceKm <= radiusKm);
}

export function formatDistanceLabel(distanceKm: number | null): string | null {
  if (distanceKm === null) {
    return null;
  }

  if (distanceKm < 1) {
    return `${Math.round(distanceKm * 1000)} m away`;
  }

  return `${distanceKm.toFixed(distanceKm < 10 ? 1 : 0)} km away`;
}

export function directionsUrl(job: ProductJob): string | null {
  if (!hasValidCoordinates(job.latitude, job.longitude)) {
    return null;
  }

  return `https://www.google.com/maps/search/?api=1&query=${normalizeNumber(job.latitude)},${normalizeNumber(job.longitude)}`;
}
