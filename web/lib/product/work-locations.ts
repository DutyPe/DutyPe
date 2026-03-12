import type { ProductWorkLocation } from "./profile";

const MAX_WORK_LOCATIONS = 10;

function numberValue(value: unknown, fallback = 0): number {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim()) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  return fallback;
}

function stringValue(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

export function normalizeWorkLocation(value: unknown): ProductWorkLocation | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const payload = value as Record<string, unknown>;
  const id = stringValue(payload.id);
  const address = stringValue(payload.address);

  if (!id || !address) {
    return null;
  }

  return {
    addedAt: numberValue(payload.addedAt, Date.now()),
    address,
    id,
    label: stringValue(payload.label, address),
    latitude: numberValue(payload.latitude),
    longitude: numberValue(payload.longitude),
    usageCount: numberValue(payload.usageCount, 1)
  };
}

export function normalizeWorkLocations(value: unknown): ProductWorkLocation[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((item) => normalizeWorkLocation(item))
    .filter((item): item is ProductWorkLocation => item !== null)
    .sort((left, right) => {
      const usageDelta = (right.usageCount ?? 0) - (left.usageCount ?? 0);
      if (usageDelta !== 0) {
        return usageDelta;
      }

      return (right.addedAt ?? 0) - (left.addedAt ?? 0);
    });
}

export function upsertWorkLocation(
  existingLocations: ProductWorkLocation[],
  nextLocation: ProductWorkLocation
): ProductWorkLocation[] {
  const normalized = normalizeWorkLocations(existingLocations);
  const duplicateByAddress = normalized.find(
    (item) => item.address?.trim().toLowerCase() === nextLocation.address?.trim().toLowerCase()
  );

  if (duplicateByAddress) {
    return normalized
      .map((item) =>
        item.id === duplicateByAddress.id
          ? {
              ...item,
              address: nextLocation.address,
              label: nextLocation.label,
              latitude: nextLocation.latitude,
              longitude: nextLocation.longitude,
              usageCount: Math.max(item.usageCount ?? 1, nextLocation.usageCount ?? 1)
            }
          : item
      )
      .sort((left, right) => (right.usageCount ?? 0) - (left.usageCount ?? 0));
  }

  const nextLocations = [nextLocation, ...normalized];
  return nextLocations.slice(0, MAX_WORK_LOCATIONS);
}

export function removeWorkLocation(
  existingLocations: ProductWorkLocation[],
  locationId: string
): ProductWorkLocation[] {
  return normalizeWorkLocations(existingLocations).filter((item) => item.id !== locationId);
}

export function incrementWorkLocationUsage(
  existingLocations: ProductWorkLocation[],
  locationId: string
): ProductWorkLocation[] {
  return normalizeWorkLocations(existingLocations)
    .map((item) =>
      item.id === locationId
        ? {
            ...item,
            usageCount: (item.usageCount ?? 0) + 1
          }
        : item
    )
    .sort((left, right) => (right.usageCount ?? 0) - (left.usageCount ?? 0));
}
