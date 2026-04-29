export type WebLocationSuggestion = {
  id: string;
  label: string;
  fullAddress: string;
  latitude: string;
  longitude: string;
  area: string;
  city: string;
  state: string;
  country: string;
};

type NominatimAddress = {
  neighbourhood?: string;
  suburb?: string;
  city_district?: string;
  quarter?: string;
  road?: string;
  village?: string;
  town?: string;
  city?: string;
  county?: string;
  state?: string;
  country?: string;
};

type NominatimPlace = {
  place_id?: number;
  osm_type?: string;
  osm_id?: number;
  display_name?: string;
  name?: string;
  lat?: string;
  lon?: string;
  address?: NominatimAddress;
};

type SearchOptions = {
  limit?: number;
  signal?: AbortSignal;
};

const NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org";

function firstText(...values: Array<string | undefined>): string {
  return values.find((value) => value?.trim())?.trim() ?? "";
}

function uniqueParts(parts: string[]): string[] {
  const seen = new Set<string>();
  return parts.filter((part) => {
    const normalized = part.trim();
    if (!normalized) return false;
    const key = normalized.toLowerCase();
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function normalizePlace(place: NominatimPlace, fallbackId: string): WebLocationSuggestion | null {
  if (!place.lat || !place.lon) return null;

  const address = place.address ?? {};
  const area = firstText(
    address.neighbourhood,
    address.suburb,
    address.city_district,
    address.quarter,
    address.road,
    address.village
  );
  const city = firstText(address.city, address.town, address.village, address.county);
  const state = firstText(address.state);
  const country = firstText(address.country) || "India";
  const fullAddress = firstText(place.display_name, [area, city, state, country].filter(Boolean).join(", "));
  const label = uniqueParts([firstText(place.name, area, fullAddress.split(",")[0]), city, state]).join(", ");

  return {
    id: String(place.place_id ?? `${place.osm_type ?? "place"}_${place.osm_id ?? fallbackId}`),
    label: label || fullAddress,
    fullAddress,
    latitude: place.lat,
    longitude: place.lon,
    area,
    city,
    state,
    country
  };
}

export async function searchIndianLocations(
  query: string,
  options: SearchOptions = {}
): Promise<WebLocationSuggestion[]> {
  const trimmedQuery = query.trim();
  if (trimmedQuery.length < 3) return [];

  const params = new URLSearchParams({
    format: "jsonv2",
    addressdetails: "1",
    namedetails: "1",
    extratags: "1",
    dedupe: "1",
    countrycodes: "in",
    limit: String(options.limit ?? 8),
    q: trimmedQuery
  });

  const response = await fetch(`${NOMINATIM_BASE_URL}/search?${params.toString()}`, {
    signal: options.signal,
    headers: { "Accept-Language": "en-IN,en" }
  });

  if (!response.ok) return [];

  const results = (await response.json()) as NominatimPlace[];
  return Array.isArray(results)
    ? results.map((place, index) => normalizePlace(place, String(index))).filter(Boolean) as WebLocationSuggestion[]
    : [];
}

export async function reverseGeocodeIndianLocation(
  latitude: number,
  longitude: number,
  options: Pick<SearchOptions, "signal"> = {}
): Promise<WebLocationSuggestion | null> {
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null;

  const params = new URLSearchParams({
    format: "jsonv2",
    addressdetails: "1",
    namedetails: "1",
    lat: String(latitude),
    lon: String(longitude)
  });

  const response = await fetch(`${NOMINATIM_BASE_URL}/reverse?${params.toString()}`, {
    signal: options.signal,
    headers: { "Accept-Language": "en-IN,en" }
  });

  if (!response.ok) return null;

  return normalizePlace((await response.json()) as NominatimPlace, `${latitude},${longitude}`);
}