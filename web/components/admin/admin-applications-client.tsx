"use client";

import { useEffect, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { type NormalizedApplication } from "@/lib/firebase/admin-normalizers";
import { formatDate } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";

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
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

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

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter, pageSize]);

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

  const visibleStatuses = [
    "ALL",
    ...new Set([...APPLICATION_STATUSES, ...applications.map((application) => application.status || "PENDING")])
  ];
  const statusCounts = applications.reduce<Record<string, number>>((acc, application) => {
    const status = application.status || "PENDING";
    acc[status] = (acc[status] ?? 0) + 1;
    return acc;
  }, {});
  const filteredApplications = applications.filter((application) => {
    const search = searchTerm.toLowerCase();
    const matchesSearch =
      !searchTerm ||
      application.workerName.toLowerCase().includes(search) ||
      application.workerPhone.includes(searchTerm) ||
      application.workerEmail.toLowerCase().includes(search) ||
      application.jobTitle.toLowerCase().includes(search) ||
      application.workerId.toLowerCase().includes(search);
    const matchesStatus = statusFilter === "ALL" || application.status === statusFilter;

    return matchesSearch && matchesStatus;
  });
  const {
    pageRows: visibleApplications,
    safePage: visibleApplicationsPage
  } = paginateRows(filteredApplications, currentPage, pageSize);

  return (
    <>
      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{applications.length}</strong>
          <span>Total applications</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{statusCounts.PENDING ?? 0}</strong>
          <span>Pending</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{statusCounts.ACCEPTED ?? 0}</strong>
          <span>Accepted</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{statusCounts.REJECTED ?? 0}</strong>
          <span>Rejected</span>
        </div>
      </div>

      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder="Search worker, phone, email, job, or ID..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <select
          className="admin-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          {visibleStatuses.map((status) => (
            <option key={status} value={status}>
              {status === "ALL" ? "All statuses" : status}
            </option>
          ))}
        </select>
        <span className="admin-count">{filteredApplications.length} applications</span>
      </div>

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
            {visibleApplications.map((application) => (
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
      <AdminTablePagination
        itemLabel="applications"
        page={visibleApplicationsPage}
        pageSize={pageSize}
        totalItems={filteredApplications.length}
        onPageChange={setCurrentPage}
        onPageSizeChange={setPageSize}
      />
    </>
  );
}

const APPLICATION_STATUSES = ["PENDING", "UNDER_REVIEW", "SHORTLISTED", "ACCEPTED", "IN_PROGRESS", "REJECTED", "COMPLETED", "WITHDRAWN"];

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
