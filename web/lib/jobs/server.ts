import "server-only";

import { FieldPath, getFirestore } from "firebase-admin/firestore";
import { unstable_cache } from "next/cache";

import { getFirebaseAdminApp, isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";
import { readTimestamp } from "@/lib/firebase/firestore-helpers";
import { emptyJobSearch, JobSearchError, publicJobSummary, queryLiveJobPage, validJobId, type JobRecord, type JobSearch, type PublicJobPage, type PublicJobSummary } from "./public-listings";

export const unavailableJobPage: PublicJobPage = { status: "unavailable", unavailableReason: "temporary", jobs: [], nextCursor: null, scanned: 0 };
const reportedFailures = new Map<string, number>();

function reportFailure(operation: string, error: unknown) {
  const candidate = error && typeof error === "object" && "code" in error ? error.code : null;
  const code = typeof candidate === "number" ? String(candidate) : typeof candidate === "string" && /^[a-z0-9_/-]{1,60}$/i.test(candidate) ? candidate : "unknown";
  const key = `${operation}:${code}`;
  const now = Date.now();
  if (now - (reportedFailures.get(key) ?? 0) < 60_000) return;
  reportedFailures.set(key, now);
  console.error("live_jobs_service_failure", { operation, code });
}

const metadataFields = [
  "title", "companyName", "category", "city", "companyCity", "area", "locality", "location", "addressText",
  "salary", "salaryType", "payAmount", "payType", "jobType", "status", "isActive", "isFilled", "vacancyStatus",
  "vacancies", "acceptedCount", "expiresAt", "postedAt", "createdAt", "updatedAt", "latitude", "longitude"
];
type JobCursor = { collection: "jobmetadata" | "jobs"; id: string | null };

function decodeCursor(value: string | null): JobCursor {
  if (value === null) return { collection: "jobmetadata", id: null };
  if (!value.startsWith("v2:")) return { collection: "jobs", id: value };
  try {
    const cursor = JSON.parse(Buffer.from(value.slice(3), "base64url").toString("utf8"));
    if (!["jobmetadata", "jobs"].includes(cursor.collection) || !validJobId(cursor.id)) throw new Error("Invalid cursor");
    return cursor;
  } catch { throw new JobSearchError("Invalid jobs page. Start a new search."); }
}

function encodeCursor(collection: JobCursor["collection"], id: string) {
  return `v2:${Buffer.from(JSON.stringify({ collection, id })).toString("base64url")}`;
}

function normalizeRecord(id: string, data: Record<string, unknown>): JobRecord {
  const values = { ...data };
  for (const field of ["createdAt", "postedAt", "updatedAt", "expiresAt"]) {
    if (data[field]) values[field] = readTimestamp(data[field])?.getTime() ?? data[field];
  }
  return { id, data: values };
}

const readBatch = unstable_cache(async (after: string | null, limit: number): Promise<JobRecord[]> => {
  const db = getFirestore(getFirebaseAdminApp());
  const cursor = decodeCursor(after);
  const records: JobRecord[] = [];
  if (cursor.collection === "jobmetadata") {
    let query = db.collection("jobmetadata").where("status", "==", "open").orderBy(FieldPath.documentId()).limit(limit);
    if (cursor.id) query = query.startAfter(cursor.id);
    const snapshot = await query.select(...metadataFields).get();
    const details = snapshot.docs.length ? await db.getAll(
      ...snapshot.docs.map((document) => db.collection("job_details").doc(document.id)),
      { fieldMask: ["companyCity", "expiresAt"] }
    ) : [];
    snapshot.docs.forEach((document, index) => {
      records.push({
        ...normalizeRecord(document.id, { ...details[index]?.data(), ...document.data() }),
        cursor: encodeCursor("jobmetadata", document.id)
      });
    });
    if (records.length === limit) return records;
  }
  let legacyQuery = db.collection("jobs").where("isActive", "==", true).orderBy(FieldPath.documentId()).limit(limit - records.length);
  if (cursor.collection === "jobs" && cursor.id) legacyQuery = legacyQuery.startAfter(cursor.id);
  const legacy = await legacyQuery.select(...metadataFields).get();
  const canonical = legacy.docs.length ? await db.getAll(
    ...legacy.docs.map((document) => db.collection("jobmetadata").doc(document.id)),
    { fieldMask: ["status"] }
  ) : [];
  legacy.docs.forEach((document, index) => records.push({
    ...normalizeRecord(document.id, canonical[index]?.exists ? { title: "", isActive: false } : document.data()),
    cursor: encodeCursor("jobs", document.id)
  }));
  return records;
}, ["public-job-batch-v2-canonical"], { revalidate: 60 });

async function withDeadline<Value>(operation: Promise<Value>): Promise<Value> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    return await Promise.race([operation, new Promise<never>((_, reject) => {
      timer = setTimeout(() => reject(Object.assign(new Error("Job service timeout"), { code: "deadline-exceeded" })), 6000);
    })]);
  } finally { clearTimeout(timer); }
}

export async function getLiveJobPage(search: JobSearch = emptyJobSearch, cursor: string | null = null): Promise<PublicJobPage> {
  if (cursor !== null && !validJobId(cursor)) throw new JobSearchError("Invalid jobs page.");
  decodeCursor(cursor);
  if (!isFirebaseAdminConfigured()) return { ...unavailableJobPage, unavailableReason: "setup-required" };
  try {
    return await withDeadline(queryLiveJobPage(readBatch, search, cursor));
  } catch (error) {
    reportFailure("search", error);
    return unavailableJobPage;
  }
}

export async function getJobRecord(id: string, includeDetails = true): Promise<JobRecord | null> {
  if (!validJobId(id)) return null;
  const db = getFirestore(getFirebaseAdminApp());
  async function readCanonicalRecord(recordId: string): Promise<JobRecord | null> {
    const canonical = await db.collection("jobmetadata").doc(recordId).get();
    if (!canonical.exists) return null;
    const detailsRef = db.collection("job_details").doc(canonical.id);
    const details = includeDetails ? await detailsRef.get() : (await db.getAll(detailsRef, { fieldMask: ["companyCity", "expiresAt"] }))[0];
    return normalizeRecord(canonical.id, { ...details?.data(), ...canonical.data() });
  }
  return withDeadline((async () => {
    const canonical = await readCanonicalRecord(id);
    if (canonical) return canonical;
    const direct = await db.collection("jobs").doc(id).get();
    if (direct.exists) return normalizeRecord(direct.id, direct.data() ?? {});
    const legacy = await db.collection("jobs").where("jobId", "==", id).limit(1).get();
    const match = legacy.docs[0];
    if (!match) return null;
    return (await readCanonicalRecord(match.id)) ?? normalizeRecord(match.id, match.data());
  })());
}

export async function getPublicJob(id: string): Promise<{ status: "ready" | "unavailable"; job: PublicJobSummary | null }> {
  if (!isFirebaseAdminConfigured()) return { status: "unavailable", job: null };
  try {
    const record = await getJobRecord(id, false);
    return { status: "ready", job: record ? publicJobSummary(record) : null };
  } catch (error) {
    reportFailure("summary", error);
    return { status: "unavailable", job: null };
  }
}

export async function getDiscoverableJobs(): Promise<PublicJobSummary[]> {
  if (!isFirebaseAdminConfigured()) return [];
  try {
    return await withDeadline((async () => {
      const jobs: PublicJobSummary[] = [];
      let cursor: string | null = null;
      for (let page = 0; page < 50; page += 1) {
        const records = await readBatch(cursor, 100);
        for (const record of records) {
          const job = publicJobSummary(record);
          if (job) jobs.push(job);
        }
        if (records.length < 100) break;
        cursor = records.at(-1)!.cursor ?? records.at(-1)!.id;
      }
      return jobs;
    })());
  } catch (error) { reportFailure("sitemap", error); return []; }
}