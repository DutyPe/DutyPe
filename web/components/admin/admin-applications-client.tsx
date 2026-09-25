"use client";

import { useCallback, useEffect, useState } from "react";
import { RefreshCw, Search, CheckCircle2, Clock, XCircle, Briefcase, FileText } from "lucide-react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { type NormalizedApplication } from "@/lib/firebase/admin-normalizers";
import { formatDate } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";

export function AdminApplicationsClient() {
  const [applications, setApplications] = useState<NormalizedApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(50);

  const loadApplications = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }
    setError(null);

    try {
      // Fetch up to 10,000 past and present applications
      const response = await adminApiFetch("/api/admin/applications?limit=10000", {
        cache: "no-store"
      });

      const payload = (await response.json()) as {
        applications?: NormalizedApplication[];
        total?: number;
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
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void loadApplications(false);
  }, [loadApplications]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter, pageSize]);

  if (loading) {
    return <div className="empty-state">Loading all past and current applications from Firestore...</div>;
  }

  if (error && applications.length === 0) {
    return (
      <div className="empty-state">
        Unable to load applications. {error}
        <div style={{ marginTop: "1rem" }}>
          <button type="button" className="button" onClick={() => void loadApplications(true)}>
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (applications.length === 0) {
    return (
      <div className="empty-state">
        No application records found across past or current collections.
        <div style={{ marginTop: "1rem" }}>
          <button type="button" className="button" onClick={() => void loadApplications(true)}>
            Refresh
          </button>
        </div>
      </div>
    );
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
    const search = searchTerm.toLowerCase().trim();
    const matchesSearch =
      !search ||
      application.workerName.toLowerCase().includes(search) ||
      application.workerPhone.includes(search) ||
      application.workerEmail.toLowerCase().includes(search) ||
      application.jobTitle.toLowerCase().includes(search) ||
      application.workerId.toLowerCase().includes(search) ||
      application.id.toLowerCase().includes(search);
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
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <strong>{applications.length}</strong>
            <FileText size={18} style={{ opacity: 0.6, color: "var(--brand, #059669)" }} />
          </div>
          <span>Total (Past & Current)</span>
        </div>
        <div className="admin-stat-card compact">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <strong>{statusCounts.PENDING ?? 0}</strong>
            <Clock size={18} style={{ opacity: 0.6, color: "#f59e0b" }} />
          </div>
          <span>Pending</span>
        </div>
        <div className="admin-stat-card compact">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <strong>{(statusCounts.ACCEPTED ?? 0) + (statusCounts.SHORTLISTED ?? 0)}</strong>
            <CheckCircle2 size={18} style={{ opacity: 0.6, color: "#10b981" }} />
          </div>
          <span>Accepted / Shortlisted</span>
        </div>
        <div className="admin-stat-card compact">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <strong>{statusCounts.REJECTED ?? 0}</strong>
            <XCircle size={18} style={{ opacity: 0.6, color: "#ef4444" }} />
          </div>
          <span>Rejected</span>
        </div>
      </div>

      <div className="admin-toolbar" style={{ display: "flex", alignItems: "center", gap: "0.75rem", flexWrap: "wrap" }}>
        <div style={{ position: "relative", flex: "1 1 280px" }}>
          <input
            type="text"
            className="admin-search"
            placeholder="Search worker, phone, email, job, or ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ width: "100%", paddingLeft: "2.2rem" }}
          />
          <Search size={15} style={{ position: "absolute", left: "0.8rem", top: "50%", transform: "translateY(-50%)", opacity: 0.4 }} />
        </div>

        <select
          className="admin-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          style={{ minWidth: "140px" }}
        >
          {visibleStatuses.map((status) => (
            <option key={status} value={status}>
              {status === "ALL" ? "All statuses" : status}
            </option>
          ))}
        </select>

        <button
          type="button"
          onClick={() => void loadApplications(true)}
          disabled={refreshing}
          className="button secondary"
          style={{ display: "inline-flex", alignItems: "center", gap: "0.4rem", padding: "0.45rem 0.85rem", fontSize: "0.82rem" }}
          title="Refresh applications list"
        >
          <RefreshCw size={14} className={refreshing ? "spin-icon" : ""} />
          <span>{refreshing ? "Refreshing..." : "Refresh"}</span>
        </button>

        <span className="admin-count" style={{ marginLeft: "auto", fontSize: "0.85rem", color: "var(--muted, #64748b)" }}>
          Showing {filteredApplications.length} of {applications.length} applications
        </span>
      </div>

      {error ? <div className="admin-error">{error}</div> : null}

      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>Worker / Applicant</th>
              <th>Phone</th>
              <th>Job Applied</th>
              <th>Status</th>
              <th>Applied Date</th>
              <th>Application ID</th>
            </tr>
          </thead>
          <tbody>
            {visibleApplications.map((application) => (
              <tr key={application.id}>
                <td>
                  <div style={{ fontWeight: 600, color: "var(--text-main, #0f172a)" }}>
                    {application.workerName || shortId(application.workerId)}
                  </div>
                  {application.workerEmail ? (
                    <div style={{ fontSize: "0.74rem", color: "var(--muted, #64748b)" }}>{application.workerEmail}</div>
                  ) : null}
                </td>
                <td>
                  <span style={{ fontFamily: "monospace", fontSize: "0.86rem" }}>
                    {application.workerPhone || "—"}
                  </span>
                </td>
                <td>
                  <div style={{ display: "flex", alignItems: "center", gap: "0.35rem" }}>
                    <Briefcase size={14} style={{ opacity: 0.5, flexShrink: 0 }} />
                    <span style={{ fontWeight: 500 }}>{application.jobTitle || "Untitled job"}</span>
                  </div>
                </td>
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
                      title="Update status"
                    >
                      {APPLICATION_STATUSES.map((status) => (
                        <option key={status} value={status}>
                          {status}
                        </option>
                      ))}
                    </select>
                  </div>
                </td>
                <td>
                  <span style={{ fontSize: "0.84rem", whiteSpace: "nowrap" }}>
                    {formatDate(application.appliedAt) !== "N/A" ? formatDate(application.appliedAt) : "Past application"}
                  </span>
                </td>
                <td>
                  <code style={{ fontSize: "0.72rem", background: "rgba(0,0,0,0.04)", padding: "2px 5px", borderRadius: "4px" }} title={application.id}>
                    {shortId(application.id)}
                  </code>
                </td>
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

const APPLICATION_STATUSES = [
  "PENDING",
  "UNDER_REVIEW",
  "SHORTLISTED",
  "ACCEPTED",
  "IN_PROGRESS",
  "REJECTED",
  "COMPLETED",
  "WITHDRAWN"
];

function statusTone(status: string | undefined) {
  switch (status) {
    case "ACCEPTED":
    case "SHORTLISTED":
    case "COMPLETED":
      return "success";
    case "REJECTED":
    case "WITHDRAWN":
      return "danger";
    case "UNDER_REVIEW":
    case "IN_PROGRESS":
      return "accent";
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

  return value.length > 14 ? `${value.slice(0, 10)}...` : value;
}
