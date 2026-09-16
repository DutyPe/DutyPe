import { readTimestamp } from "@/lib/firebase/firestore-helpers";
import { jobDirectoryCities } from "@/lib/public-site";
import { distanceBetweenKm, hasValidCoordinates } from "@/lib/product/location";

export type JobRecord = { id: string; data: Record<string, unknown> };

export type PublicJobSummary = {
  id: string;
  title: string;
  companyName: string;
  category: string;
  city: string;
  area: string;
  payAmount: string;
  payType: string;
  jobType: string;
  postedAt: number | null;
  expiresAt: number | null;
  updatedAt: number | null;
  distanceKm?: number;
};

export type JobSearch = {
  query: string;
  city: string;
  area: string;
  category: string;
  location?: { latitude: number; longitude: number; radiusKm: number };
};

export const emptyJobSearch: JobSearch = { query: "", city: "", area: "", category: "" };

function text(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

function timestamp(value: unknown): number | null {
  const time = readTimestamp(value)?.getTime();
  return time && Number.isFinite(time) ? time : null;
}

function normalized(value: string): string {
  return value.toLowerCase().normalize("NFKC").replace(/\bbengaluru\b/g, "bangalore").replace(/[^\p{L}\p{N}]+/gu, " ").trim();
}

function containsTerms(value: string, search: string): boolean {
  const words = ` ${normalized(value)} `;
  return normalized(search).split(" ").filter(Boolean).every((term) => words.includes(` ${term} `));
}

export function publicJobSummary(record: JobRecord, now = Date.now()): PublicJobSummary | null {
  const job = record.data;
  const title = text(job.title);
  const expiresAt = timestamp(job.expiresAt);
  const status = text(job.vacancyStatus).toUpperCase();
  if (!title || job.isActive !== true || job.isFilled === true ||
    ["FILLED", "CLOSED", "EXPIRED", "CANCELLED"].includes(status) ||
    (expiresAt !== null && expiresAt <= now)) return null;
  if (job.expiresAt && expiresAt === null) return null;
  if (job.vacancies != null) {
    const vacancies = Number(job.vacancies);
    if (!Number.isFinite(vacancies) || vacancies <= 0 || Number(job.acceptedCount ?? 0) >= vacancies) return null;
  }
  const legacyLocation = text(job.location);
  const locationParts = legacyLocation.split(",").map(normalized);
  const city = text(job.city) || jobDirectoryCities.find((name) =>
    locationParts.includes(normalized(name)) || normalized(legacyLocation).endsWith(` ${normalized(name)}`)
  ) || "";
  return {
    id: record.id, title, companyName: text(job.companyName), category: text(job.category).toUpperCase(),
    city, area: text(job.area) || text(job.locality),
    payAmount: typeof job.payAmount === "number" && Number.isFinite(job.payAmount) ? String(job.payAmount) : text(job.payAmount),
    payType: text(job.payType), jobType: text(job.jobType),
    postedAt: timestamp(job.postedAt ?? job.createdAt), expiresAt,
    updatedAt: timestamp(job.updatedAt ?? job.postedAt ?? job.createdAt)
  };
}

export function matchingJobSummary(record: JobRecord, search: JobSearch, now = Date.now()): PublicJobSummary | null {
  const summary = publicJobSummary(record, now);
  if (!summary) return null;
  const locationText = `${summary.city} ${summary.area} ${text(record.data.location)}`;
  if (!containsTerms(summary.city, search.city) || !containsTerms(locationText, search.area)) return null;
  const keyword = search.query.replace(/\b(jobs?|vacancies|vacancy|work|in|near|me)\b/gi, " ").trim();
  const queryText = `${summary.title} ${summary.companyName} ${summary.category} ${normalizedCategory(summary.category)} ${summary.city} ${summary.area}`;
  if (keyword && !containsTerms(queryText, keyword) && !containsTerms(queryText, normalizedCategory(keyword))) return null;
  const category = normalizedCategory(search.category);
  if (category === "part time" ? normalized(summary.jobType) !== "part time"
    : category === "daily wage" ? normalized(summary.payType) !== "daily"
      : category && normalizedCategory(summary.category) !== category) return null;
  if (search.location) {
    if (!hasValidCoordinates(record.data.latitude, record.data.longitude)) return null;
    const distanceKm = distanceBetweenKm(search.location.latitude, search.location.longitude, Number(record.data.latitude), Number(record.data.longitude));
    if (distanceKm > search.location.radiusKm) return null;
    return { ...summary, distanceKm: Math.round(distanceKm * 10) / 10 };
  }
  return summary;
}

function normalizedCategory(value: string): string {
  const aliases: Record<string, string> = { driver: "driving", drivers: "driving", cook: "cooking", maid: "housekeeping", cleaner: "cleaning" };
  const category = normalized(value);
  return Object.hasOwn(aliases, category) ? aliases[category] : category;
}

export class JobSearchError extends Error {}

export function parseJobSearch(input: Record<string, unknown>): JobSearch {
  const field = (name: string, maxLength: number) => {
    const value = input[name] ?? "";
    if (typeof value !== "string" || value.length > maxLength) throw new JobSearchError(`Invalid ${name} filter.`);
    return value.trim();
  };
  const result: JobSearch = { query: field("q", 120), city: field("city", 80), area: field("area", 100), category: field("category", 40) };
  if (input.location !== undefined) {
    if (!input.location || typeof input.location !== "object") throw new JobSearchError("Invalid nearby location.");
    const location = input.location as Record<string, unknown>;
    if (typeof location.latitude !== "number" || typeof location.longitude !== "number" ||
      !hasValidCoordinates(location.latitude, location.longitude) ||
      ![0.5, 1, 2, 5, 10, 25, 50].includes(Number(location.radiusKm))) {
      throw new JobSearchError("Choose a valid location and search radius.");
    }
    result.location = { latitude: location.latitude, longitude: location.longitude, radiusKm: Number(location.radiusKm) };
  }
  return result;
}

export function jobSearchParams(search: JobSearch): URLSearchParams {
  const params = new URLSearchParams();
  for (const [name, value] of Object.entries({ q: search.query, city: search.city, area: search.area, category: search.category })) {
    if (value) params.set(name, value);
  }
  return params;
}

export function validJobId(value: unknown): value is string {
  return typeof value === "string" && value.length > 0 && value.length <= 1500 && !/[\u0000-\u001f/\\]/.test(value) && value !== "." && value !== "..";
}

export type PublicJobPage = {
  status: "ready" | "unavailable";
  unavailableReason?: "setup-required" | "temporary";
  jobs: PublicJobSummary[];
  nextCursor: string | null;
  scanned: number;
};

export type ReadJobBatch = (after: string | null, limit: number) => Promise<JobRecord[]>;

export async function queryLiveJobPage(readBatch: ReadJobBatch, search: JobSearch, after: string | null = null, now = Date.now()): Promise<PublicJobPage> {
  if (after !== null && !validJobId(after)) throw new JobSearchError("Invalid jobs page.");
  const jobs: PublicJobSummary[] = [];
  let cursor = after;
  let scanned = 0;
  const batchSize = 100;
  const scanLimit = 500;
  const pageSize = 20;
  while (scanned < scanLimit) {
    const records = await readBatch(cursor, batchSize);
    if (!records.length) return { status: "ready", jobs, nextCursor: null, scanned };
    for (const [index, record] of records.entries()) {
      cursor = record.id;
      scanned += 1;
      const summary = matchingJobSummary(record, search, now);
      if (summary) jobs.push(summary);
      if (jobs.length === pageSize) {
        return { status: "ready", jobs, nextCursor: index < records.length - 1 || records.length === batchSize ? cursor : null, scanned };
      }
    }
    if (records.length < batchSize) return { status: "ready", jobs, nextCursor: null, scanned };
  }
  return { status: "ready", jobs, nextCursor: cursor, scanned };
}