"use client";

import { useEffect, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { type NormalizedApplication } from "@/lib/firebase/admin-normalizers";
import { formatDate } from "@/lib/firebase/firestore-helpers";

type ApplicationRow = {
  id: string;
  workerId?: string;
  workerName?: string;
  workerPhone?: string;
  workerEmail?: string;
  applicantName?: string;
  jobTitle?: string;
  status?: string;
  appliedAt?: unknown;
};

export function AdminApplicationsClient() {
  const [applications, setApplications] = useState<NormalizedApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  useEffect(() => {
    async function loadApplications() {
      try {
        const response = await adminApiFetch("/api/admin/applications", {
          cache: "no-store"
        });

        const payload = (await response.json()) as {
          applications?: NormalizedApplication[];
          error?: string;
        };

        if (!response.ok) {
          throw new Error(payload.error || "Failed to load applications.");
        }

        setApplications(payload.applications ?? []);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load applications.");
      } finally {
        setLoading(false);
      }
    }

    void loadApplications();
  }, []);

  if (loading) {
    return <div className="empty-state">Loading applications from Firestore.</div>;
  }

  if (error) {
    return <div className="empty-state">Unable to load applications. {error}</div>;
  }

  if (applications.length === 0) {
    return <div className="empty-state">No applications were returned from Firestore.</div>;
  }

  async function handleStatusChange(applicationId: string, status: string) {
    if (updatingId) {
      return;
    }

    setUpdatingId(applicationId);
    setError(null);

    try {
      const response = await adminApiFetch("/api/admin/applications", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ applicationId, status })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to update status.");
      }

      setApplications((current) =>
        current.map((item) =>
          item.id === applicationId
            ? {
                ...item,
                status
              }
            : item
        )
      );
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : "Failed to update status.");
    } finally {
      setUpdatingId(null);
    }
  }

  return (
    <>
      {error ? <div className="admin-error">{error}</div> : null}

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
                <td>{application.workerName || shortId(application.workerId)}</td>
                <td>{application.workerPhone || shortId(application.workerId)}</td>
                <td>{application.jobTitle || "Untitled job"}</td>
                <td>
                  <div className="admin-status-cell">
                    <span className={`status-pill ${statusTone(application.status)}`}>
                      {application.status || "PENDING"}
                    </span>
                    <select
                      className="admin-inline-select"
                      value={application.status || "PENDING"}
                      onChange={(event) => {
                        void handleStatusChange(application.id, event.target.value);
                      }}
                      disabled={updatingId === application.id}
                    >
                      {APPLICATION_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                </td>
                <td>{formatDate(application.appliedAt) !== "N/A" ? formatDate(application.appliedAt) : "Recently"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

const APPLICATION_STATUSES = ["PENDING", "SHORTLISTED", "ACCEPTED", "REJECTED", "WITHDRAWN"];

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

function shortId(value: string | undefined) {
  if (!value) {
    return "N/A";
  }

  return value.length > 12 ? `${value.slice(0, 8)}...` : value;
}
