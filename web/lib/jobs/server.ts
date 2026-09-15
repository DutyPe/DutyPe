import "server-only";

import { FieldPath, getFirestore } from "firebase-admin/firestore";
import { unstable_cache } from "next/cache";

import { getFirebaseAdminApp, isFirebaseAdminConfigured } from "@/lib/firebase/admin-server";
import { readTimestamp } from "@/lib/firebase/firestore-helpers";
import { emptyJobSearch, JobSearchError, publicJobSummary, queryLiveJobPage, validJobId, type JobRecord, type JobSearch, type PublicJobPage, type PublicJobSummary } from "./public-listings";

export const unavailableJobPage: PublicJobPage = { status: "unavailable", unavailableReason: "temporary", jobs: [], nextCursor: null, scanned: 0 };

function normalizeRecord(id: string, data: Record<string, unknown>): JobRecord {
  const values = { ...data };
  for (const field of ["createdAt", "postedAt", "updatedAt", "expiresAt"]) {
    if (data[field]) values[field] = readTimestamp(data[field])?.getTime() ?? data[field];
  }
  return { id, data: values };
}

const readBatch = unstable_cache(async (after: string | null, limit: number): Promise<JobRecord[]> => {
  const db = getFirestore(getFirebaseAdminApp());
  let query = db.collection("jobs").where("isActive", "==", true).orderBy(FieldPath.documentId()).limit(limit);
  if (after) query = query.startAfter(after);
  const snapshot = await query.select(
    "title", "companyName", "category", "city", "area", "locality", "location", "payAmount", "payType", "jobType",
    "isActive", "isFilled", "vacancyStatus", "vacancies", "acceptedCount", "expiresAt", "postedAt", "createdAt", "updatedAt", "latitude", "longitude"
  ).get();
  return snapshot.docs.map((document) => normalizeRecord(document.id, document.data()));
}, ["public-job-batch-v1"], { revalidate: 60 });

async function withDeadline<Value>(operation: Promise<Value>): Promise<Value> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    return await Promise.race([operation, new Promise<never>((_, reject) => {
      timer = setTimeout(() => reject(new Error("Job service timeout")), 6000);
    })]);
  } finally { clearTimeout(timer); }
}

export async function getLiveJobPage(search: JobSearch = emptyJobSearch, cursor: string | null = null): Promise<PublicJobPage> {
  if (cursor !== null && !validJobId(cursor)) throw new JobSearchError("Invalid jobs page.");
  if (!isFirebaseAdminConfigured()) return { ...unavailableJobPage, unavailableReason: "setup-required" };
  try {
    return await withDeadline(queryLiveJobPage(readBatch, search, cursor));
  } catch {
    return unavailableJobPage;
  }
}

export async function getJobRecord(id: string): Promise<JobRecord | null> {
  if (!validJobId(id)) return null;
  const db = getFirestore(getFirebaseAdminApp());
  return withDeadline((async () => {
    const direct = await db.collection("jobs").doc(id).get();
    if (direct.exists) return normalizeRecord(direct.id, direct.data() ?? {});
    const legacy = await db.collection("jobs").where("jobId", "==", id).limit(1).get();
    const match = legacy.docs[0];
    return match ? normalizeRecord(match.id, match.data()) : null;
  })());
}

export async function getPublicJob(id: string): Promise<{ status: "ready" | "unavailable"; job: PublicJobSummary | null }> {
  if (!isFirebaseAdminConfigured()) return { status: "unavailable", job: null };
  try {
    const record = await getJobRecord(id);
    return { status: "ready", job: record ? publicJobSummary(record) : null };
  } catch {
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
        cursor = records.at(-1)!.id;
      }
      return jobs;
    })());
  } catch { return []; }
}