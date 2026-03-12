"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, getDocs, limit, orderBy, query } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatDate } from "@/lib/firebase/firestore-helpers";

type ApplicationRow = {
  id: string;
  workerName?: string;
  workerPhone?: string;
  jobTitle?: string;
  status?: string;
  appliedAt?: unknown;
};

export function AdminApplicationsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [applications, setApplications] = useState<ApplicationRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadApplications() {
      if (!services) {
        setError("Firebase is not configured.");
        setLoading(false);
        return;
      }

      try {
        const snapshot = await getDocs(
          query(collection(services.db, "job_applications"), orderBy("appliedAt", "desc"), limit(100))
        );

        setApplications(
          snapshot.docs.map((item) => ({
            id: item.id,
            ...(item.data() as Omit<ApplicationRow, "id">)
          }))
        );
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load applications.");
      } finally {
        setLoading(false);
      }
    }

    void loadApplications();
  }, [services]);

  if (loading) {
    return <div className="empty-state">Loading applications from Firestore.</div>;
  }

  if (error) {
    return <div className="empty-state">Unable to load applications. {error}</div>;
  }

  if (applications.length === 0) {
    return <div className="empty-state">No applications were returned from Firestore.</div>;
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Worker</th>
            <th>Phone</th>
            <th>Job</th>
            <th>Status</th>
            <th>Applied</th>
          </tr>
        </thead>
        <tbody>
          {applications.map((application) => (
            <tr key={application.id}>
              <td>{application.workerName ?? "N/A"}</td>
              <td>{application.workerPhone ?? "N/A"}</td>
              <td>{application.jobTitle ?? "N/A"}</td>
              <td>
                <span className={`status-pill ${statusTone(application.status)}`}>
                  {application.status ?? "PENDING"}
                </span>
              </td>
              <td>{formatDate(application.appliedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function statusTone(status: string | undefined) {
  switch (status) {
    case "ACCEPTED":
      return "success";
    case "REJECTED":
      return "danger";
    case "PENDING":
      return "warning";
    default:
      return "neutral";
  }
}
