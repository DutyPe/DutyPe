import { Metadata } from "next";
import { notFound } from "next/navigation";

import { SiteShell } from "@/components/site-shell";
import { PublicJobPageClient } from "@/components/public/public-job-page-client";
import { getFirebaseAdminDb } from "@/lib/firebase/admin-server";

type Props = {
  params: {
    jobId: string;
  };
};

export const revalidate = 0;
export const dynamic = "force-dynamic";

async function getJobData(jobId: string) {
  try {
    const db = getFirebaseAdminDb();
    const metaDoc = await db.collection("jobmetadata").doc(jobId).get();
    if (!metaDoc.exists) return null;

    const detailsDoc = await db.collection("job_details").doc(jobId).get();

    const meta = metaDoc.data() || {};
    const details = detailsDoc.exists ? (detailsDoc.data() || {}) : {};

    const serializeTimestamp = (ts: any) => {
      if (!ts) return null;
      if (typeof ts.toDate === "function") {
        return ts.toDate().getTime();
      }
      if (typeof ts.seconds === "number") {
        return ts.seconds * 1000;
      }
      if (typeof ts._seconds === "number") {
        return ts._seconds * 1000;
      }
      return null;
    };

    return {
      id: jobId,
      title: String(meta.title || ""),
      companyName: String(meta.companyName || ""),
      salary: meta.salary,
      salaryType: meta.salaryType,
      addressText: meta.addressText,
      vacancies: meta.vacancies,
      status: meta.status,
      jobType: meta.jobType,
      shiftTiming: meta.shiftTiming,
      createdAt: serializeTimestamp(meta.createdAt),
      expiresAt: serializeTimestamp(details.expiresAt),
      description: details.description,
      contactNumber: details.contactNumber,
      gender: details.gender,
      experienceRequired: details.experienceRequired,
      educationRequired: details.educationRequired,
    };
  } catch (err) {
    console.error("Error fetching job:", err);
    return null;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const job = await getJobData(params.jobId);
  if (!job) {
    return {
      title: "Job Not Found - DutyPe",
      description: "This job listing is no longer active or could not be found on DutyPe."
    };
  }

  const jobTitle = job.title || "Job opening";
  const company = job.companyName || "DutyPe Employer";
  const location = job.addressText || "India";
  const salaryText = job.salary ? `offering salary of ${job.salary}` : "best in industry salary";

  return {
    title: `${jobTitle} at ${company} - DutyPe`,
    description: `Apply for the ${jobTitle} vacancy at ${company} located in ${location}, ${salaryText}. Download the DutyPe app to apply now.`,
    keywords: [
      jobTitle,
      company,
      job.jobType || "job vacancy",
      "DutyPe jobs",
      "hiring near me",
      "jobs near me"
    ]
  };
}

export default async function JobDetailPage({ params }: Props) {
  const job = await getJobData(params.jobId);
  if (!job) {
    notFound();
  }

  return (
    <SiteShell>
      <PublicJobPageClient job={job} />
    </SiteShell>
  );
}
