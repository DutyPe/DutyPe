"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, deleteDoc, doc, getDocs, limit, orderBy, query } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";

type JobRow = {
  id: string;
  title?: string;
  companyName?: string;
  location?: string;
  payAmount?: number | string;
  payType?: string;
  vacancies?: number;
  isActive?: boolean;
  applicationCount?: number;
};

export function AdminJobsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<JobRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);

  async function loadJobs() {
    if (!services) {
      setError("Firebase is not configured.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const snapshot = await getDocs(
        query(collection(services.db, "jobs"), orderBy("createdAt", "desc"), limit(100))
      );

      setJobs(
        snapshot.docs.map((item) => ({
          id: item.id,
          ...(item.data() as Omit<JobRow, "id">)
        }))
      );
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load jobs.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadJobs();
  }, []);

  async function handleDelete(jobId: string) {
    if (!services) {
      return;
    }

    const shouldDelete = window.confirm("Delete this job?");
    if (!shouldDelete) {
      return;
    }

    try {
      setPendingDeleteId(jobId);
      await deleteDoc(doc(services.db, "jobs", jobId));
      await loadJobs();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete job.");
    } finally {
      setPendingDeleteId(null);
    }
  }

  if (loading) {
    return <div className="empty-state">Loading jobs from Firestore.</div>;
  }

  if (error) {
    return <div className="empty-state">Unable to load jobs. {error}</div>;
  }

  if (jobs.length === 0) {
    return <div className="empty-state">No jobs were returned from Firestore.</div>;
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Company</th>
            <th>Location</th>
            <th>Salary</th>
            <th>Vacancies</th>
            <th>Status</th>
            <th>Applications</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map((job) => (
            <tr key={job.id}>
              <td>{job.title ?? "N/A"}</td>
              <td>{job.companyName ?? "N/A"}</td>
              <td>{job.location ?? "N/A"}</td>
              <td>{formatCurrencyRange(job.payAmount, job.payType)}</td>
              <td>{job.vacancies ?? 0}</td>
              <td>
                <span className={`status-pill ${job.isActive ? "success" : "danger"}`}>
                  {job.isActive ? "Active" : "Inactive"}
                </span>
              </td>
              <td>{job.applicationCount ?? 0}</td>
              <td>
                <button
                  type="button"
                  className="table-action danger"
                  onClick={() => void handleDelete(job.id)}
                  disabled={pendingDeleteId === job.id}
                >
                  {pendingDeleteId === job.id ? "Deleting..." : "Delete"}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
