"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";

type JobDoc = {
  title?: string;
  description?: string;
  companyName?: string;
  location?: string;
  payAmount?: number | string;
  payType?: string;
  vacancies?: number;
  isActive?: boolean;
  jobType?: string;
};

export function LiveJobDetails({ jobId }: { jobId: string }) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [job, setJob] = useState<JobDoc | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      if (!services) {
        setError("Firebase is not configured.");
        setLoading(false);
        return;
      }

      try {
        const snapshot = await getDoc(doc(services.db, "jobs", jobId));
        if (!snapshot.exists()) {
          setJob(null);
          setError("No Firestore job found for this route yet.");
        } else {
          setJob(snapshot.data() as JobDoc);
          setError(null);
        }
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load job.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, [services, jobId]);

  if (loading) {
    return <div className="empty-state">Loading Firestore job data for `{jobId}`.</div>;
  }

  if (error || !job) {
    return <div className="empty-state">{error ?? "No Firestore job found."}</div>;
  }

  return (
    <div className="detail-grid">
      <article className="detail-panel">
        <span className="card-kicker">Live marketplace snapshot</span>
        <h3>{job.title ?? "Untitled job"}</h3>
        <p>{job.description ?? "No description provided."}</p>
      </article>
      <article className="detail-panel">
        <span className="card-kicker">Current listing fields</span>
        <ul className="detail-list">
          <li>
            <strong>Company</strong>
            <span>{job.companyName ?? "N/A"}</span>
          </li>
          <li>
            <strong>Location</strong>
            <span>{job.location ?? "N/A"}</span>
          </li>
          <li>
            <strong>Pay</strong>
            <span>{formatCurrencyRange(job.payAmount, job.payType)}</span>
          </li>
          <li>
            <strong>Vacancies</strong>
            <span>{job.vacancies ?? 0}</span>
          </li>
          <li>
            <strong>Job type</strong>
            <span>{job.jobType ?? "N/A"}</span>
          </li>
          <li>
            <strong>Status</strong>
            <span>{job.isActive ? "Active" : "Inactive or hidden"}</span>
          </li>
        </ul>
      </article>
    </div>
  );
}
