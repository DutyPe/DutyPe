import { NextResponse } from "next/server";
import { Timestamp } from "firebase-admin/firestore";

import { cachedAdminGet } from "@/lib/firebase/admin-response-cache";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";
import { Applications, EmployerProfiles, Jobs, Referrals, Values, WorkerProfiles } from "@/lib/firebase/schema";

export const runtime = "nodejs";

const day = (v: unknown) => (v instanceof Timestamp ? v.toDate().toLocaleDateString("en-IN") : "N/A");

/** Dashboard numbers: count aggregations (≈1 read per 1,000 docs) + the latest 5 of each list. */
async function getUncached() {
  try {
    const db = getFirebaseAdminDb();
    const count = async (q: FirebaseFirestore.Query) => (await q.count().get()).data().count;
    const jobs = db.collection(Jobs.COLLECTION);
    const apps = db.collection(Applications.COLLECTION);
    const refs = db.collection(Referrals.COLLECTION);
    const [
      workers, employers, totalJobs, activeJobs, totalApplications, pendingApplications, totalReferrals, completedReferrals,
      recentJobs, recentApps, recentWorkers, recentEmployers
    ] = await Promise.all([
      count(db.collection(WorkerProfiles.COLLECTION)),
      count(db.collection(EmployerProfiles.COLLECTION)),
      count(jobs),
      count(jobs.where(Jobs.STATUS, "==", Values.JobStatus.OPEN)),
      count(apps),
      count(apps.where(Applications.STATUS, "==", Values.ApplicationStatus.APPLIED)),
      count(refs),
      count(refs.where(Referrals.STATUS, "==", Values.ReferralStatus.COMPLETED)),
      jobs.orderBy(Jobs.CREATED_AT, "desc").limit(5).get(),
      apps.orderBy(Applications.CREATED_AT, "desc").limit(5).get(),
      db.collection(WorkerProfiles.COLLECTION).orderBy(WorkerProfiles.CREATED_AT, "desc").limit(3).get(),
      db.collection(EmployerProfiles.COLLECTION).orderBy(EmployerProfiles.CREATED_AT, "desc").limit(3).get()
    ]);

    const titles = new Map<string, string>();
    const jobIds = Array.from(new Set(recentApps.docs.map((d) => String(d.get(Applications.JOB_ID) ?? "")))).filter(Boolean);
    if (jobIds.length) {
      (await db.getAll(...jobIds.map((id) => jobs.doc(id)), { fieldMask: [Jobs.TITLE] }))
        .forEach((c) => titles.set(c.id, String(c.get(Jobs.TITLE) ?? "")));
    }

    return NextResponse.json({
      totalUsers: workers + employers,
      workers,
      employers,
      totalJobs,
      activeJobs,
      totalApplications,
      pendingApplications,
      totalReferrals,
      completedReferrals,
      stateStats: {},
      recentJobs: recentJobs.docs.map((d) => ({
        id: d.id,
        title: String(d.get(Jobs.TITLE) ?? "Untitled"),
        companyName: String(d.get(Jobs.COMPANY_NAME) ?? ""),
        isActive: d.get(Jobs.STATUS) === Values.JobStatus.OPEN,
        createdAt: day(d.get(Jobs.CREATED_AT))
      })),
      recentUsers: [
        ...recentWorkers.docs.map((d) => ({ id: d.id, name: String(d.get(WorkerProfiles.NAME) || d.get(WorkerProfiles.PHONE) || d.id), role: "WORKER", at: d.get(WorkerProfiles.CREATED_AT) })),
        ...recentEmployers.docs.map((d) => ({ id: d.id, name: String(d.get(EmployerProfiles.BUSINESS_NAME) || d.get(EmployerProfiles.OWNER_NAME) || d.id), role: "EMPLOYER", at: d.get(EmployerProfiles.CREATED_AT) }))
      ]
        .sort((a, b) => (b.at instanceof Timestamp ? b.at.toMillis() : 0) - (a.at instanceof Timestamp ? a.at.toMillis() : 0))
        .map(({ at, ...u }) => ({ ...u, joinedAt: day(at) })),
      recentApplications: recentApps.docs.map((d) => ({
        id: d.id,
        workerName: String(d.get(Applications.WORKER_NAME) || d.get(Applications.WORKER_ID) || "Unknown"),
        jobTitle: titles.get(String(d.get(Applications.JOB_ID))) || "(job removed)",
        status: String(d.get(Applications.STATUS) ?? "").toUpperCase(),
        appliedAt: day(d.get(Applications.CREATED_AT))
      }))
    });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Failed to load dashboard." }, { status: 500 });
  }
}

export const GET = cachedAdminGet(getUncached);
