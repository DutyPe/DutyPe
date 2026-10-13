"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { httpsCallable } from "firebase/functions";
import { getFirebaseServices } from "@/lib/firebase/client";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";
import { Gift, ShieldAlert, CheckCircle, RefreshCw, UserCheck } from "lucide-react";

type JobRow = {
  id: string;
  title?: string;
  companyName?: string;
  addressText?: string;
  area?: string;
  payAmount?: number | string;
  payType?: string;
  vacancies?: number;
  status?: string;
  isActive?: boolean;
  applicationCount?: number;
  description?: string;
  category?: string;
  shift?: string;
  createdAt?: { _seconds?: number; seconds?: number } | string | number | null;
};

function renderLocation(job: JobRow): string {
  return job.addressText?.trim() || job.area?.trim() || "N/A";
}

function getEpochMillis(createdAt: any): number {
  if (!createdAt) return 0;
  if (typeof createdAt === "number") return createdAt;
  if (typeof createdAt === "string") return new Date(createdAt).getTime();
  if (typeof createdAt === "object") {
    if (typeof createdAt._seconds === "number") return createdAt._seconds * 1000;
    if (typeof createdAt.seconds === "number") return createdAt.seconds * 1000;
  }
  return 0;
}

function formatDateLocal(createdAt: any): string {
  const millis = getEpochMillis(createdAt);
  if (millis === 0) return "N/A";
  const date = new Date(millis);
  return date.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

type EditingJob = {
  id: string;
  title: string;
  companyName: string;
  location: string;
  payAmount: string;
  vacancies: string;
  description: string;
  isActive: boolean;
};

export function AdminJobsClient() {
  const [jobs, setJobs] = useState<JobRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [editing, setEditing] = useState<EditingJob | null>(null);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [activatingExpired, setActivatingExpired] = useState(false);

  // Free Job Posting Policy Config & Specific Grant State
  const [freeConfigLoading, setFreeConfigLoading] = useState(false);
  const [freeConfigSaving, setFreeConfigSaving] = useState(false);
  const [freeConfigMsg, setFreeConfigMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [freeJobPostingEnabled, setFreeJobPostingEnabled] = useState(false);
  const [freeJobsCount, setFreeJobsCount] = useState(0);
  const [applicableTo, setApplicableTo] = useState<"NONE" | "NEW_USERS" | "ALL_USERS">("NONE");

  // User-specific free jobs grant state
  const [grantPhoneOrUid, setGrantPhoneOrUid] = useState("");
  const [grantCount, setGrantCount] = useState(1);
  const [granting, setGranting] = useState(false);
  const [grantMsg, setGrantMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  async function loadFreeJobConfig() {
    try {
      setFreeConfigLoading(true);
      const services = getFirebaseServices();
      if (!services) return;
      const getCfg = httpsCallable(services.functions, "getJobPostingConfig");
      const res: any = await getCfg();
      const cfg = res.data;
      if (cfg) {
        setFreeJobPostingEnabled(Boolean(cfg.freeJobPostingEnabled));
        setFreeJobsCount(Number(cfg.freeJobsCount) || 0);
        setApplicableTo(cfg.applicableTo || (cfg.freeJobPostingEnabled ? "NEW_USERS" : "NONE"));
      }
    } catch (e: any) {
      console.warn("Could not load job posting config:", e);
    } finally {
      setFreeConfigLoading(false);
    }
  }

  async function handleSaveFreeConfig() {
    try {
      setFreeConfigSaving(true);
      setFreeConfigMsg(null);
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase not initialized");
      const updateCfg = httpsCallable(services.functions, "updateJobPostingConfig");
      await updateCfg({
        freeJobPostingEnabled,
        freeJobsCount: Number(freeJobsCount) || 0,
        applicableTo
      });
      setFreeConfigMsg({ type: "success", text: "Free job posting policy saved successfully!" });
    } catch (e: any) {
      setFreeConfigMsg({ type: "error", text: e.message || "Failed to save policy." });
    } finally {
      setFreeConfigSaving(false);
    }
  }

  async function handleGrantUserFreeJobs() {
    if (!grantPhoneOrUid.trim()) {
      setGrantMsg({ type: "error", text: "Please enter employer phone number or UID." });
      return;
    }
    try {
      setGranting(true);
      setGrantMsg(null);
      const services = getFirebaseServices();
      if (!services) throw new Error("Firebase not initialized");
      const grantFn = httpsCallable(services.functions, "grantEmployerFreeJobs");
      const res: any = await grantFn({
        phoneOrUid: grantPhoneOrUid.trim(),
        freeJobPosts: Number(grantCount) || 1
      });
      setGrantMsg({
        type: "success",
        text: `Granted ${grantCount} free jobs to employer (${res.data?.name || res.data?.phone || "Employer"}). Total free jobs now: ${res.data?.grantedFreeJobPosts}`
      });
      setGrantPhoneOrUid("");
    } catch (e: any) {
      setGrantMsg({ type: "error", text: e.message || "Failed to grant free jobs." });
    } finally {
      setGranting(false);
    }
  }

  async function loadJobs() {
    try {
      setLoading(true);
      const response = await adminApiFetch("/api/admin/jobs", {
        cache: "no-store"
      });

      const payload = (await response.json()) as {
        jobs?: Array<{ id: string } & Omit<JobRow, "id">>;
        error?: string;
      };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load jobs.");
      }

      const rows = payload.jobs ?? [];

      setJobs(
        rows.map((item) => {
          const data = item as Omit<JobRow, "id"> & { id: string };
          const { id, ...rest } = data;
          const normalizedStatus = typeof data.status === "string" ? data.status : (data.isActive ? "open" : "closed");
          return {
            id,
            ...rest,
            status: normalizedStatus,
            isActive: normalizedStatus === "open"
          };
        })
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
    void loadFreeJobConfig();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter, pageSize]);

  async function handleDelete(jobId: string) {
    if (!window.confirm("Are you sure you want to delete this job? This cannot be undone.")) return;

    try {
      setPendingDeleteId(jobId);
      const response = await adminApiFetch("/api/admin/jobs", {
        method: "DELETE",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ jobId })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to delete job.");
      }

      await loadJobs();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete job.");
    } finally {
      setPendingDeleteId(null);
    }
  }

  async function handleToggleActive(jobId: string, currentActive: boolean) {
    try {
      const nextStatus = currentActive ? "closed" : "open";
      const response = await adminApiFetch("/api/admin/jobs", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          jobId,
          status: nextStatus
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to toggle job.");
      }

      setJobs((prev) =>
        prev.map((j) => (j.id === jobId ? { ...j, status: nextStatus, isActive: nextStatus === "open" } : j))
      );
    } catch (toggleError) {
      setError(toggleError instanceof Error ? toggleError.message : "Failed to toggle job.");
    }
  }

  async function handleActivateExpiredJobs() {
    if (!window.confirm("Are you sure you want to activate ALL expired jobs for 30 days?")) return;

    try {
      setActivatingExpired(true);
      setError(null);
      
      const response = await adminApiFetch("/api/admin/jobs/activate-expired", {
        method: "POST",
        headers: { "Content-Type": "application/json" }
      });

      const payload = await response.json();
      if (!response.ok) {
        throw new Error(payload.error || "Failed to activate expired jobs.");
      }

      alert(payload.message || `Activated jobs successfully.`);
      await loadJobs();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to activate jobs.");
    } finally {
      setActivatingExpired(false);
    }
  }

  function startEdit(job: JobRow) {
    setEditing({
      id: job.id,
      title: job.title ?? "",
      companyName: job.companyName ?? "",
      location: renderLocation(job) === "N/A" ? "" : renderLocation(job),
      payAmount: String(job.payAmount ?? ""),
      vacancies: String(job.vacancies ?? ""),
      description: job.description ?? "",
      isActive: (job.status ?? (job.isActive ? "open" : "closed")) === "open"
    });
  }

  async function handleSaveEdit() {
    if (!editing) return;

    try {
      setSaving(true);
      const response = await adminApiFetch("/api/admin/jobs", {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          jobId: editing.id,
          title: editing.title,
          companyName: editing.companyName,
          addressText: editing.location,
          payAmount: Number(editing.payAmount) || 0,
          vacancies: Number(editing.vacancies) || 1,
          description: editing.description,
          status: editing.isActive ? "open" : "closed"
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to save changes.");
      }

      setEditing(null);
      await loadJobs();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Failed to save changes.");
    } finally {
      setSaving(false);
    }
  }

  const openCount = jobs.filter((job) => job.isActive).length;
  const filteredJobs = jobs.filter((job) => {
    const search = searchTerm.toLowerCase();
    const normalizedStatus = String(job.status ?? (job.isActive ? "open" : "closed")).toLowerCase();
    const matchesSearch =
      !searchTerm ||
      (job.title ?? "").toLowerCase().includes(search) ||
      (job.companyName ?? "").toLowerCase().includes(search) ||
      renderLocation(job).toLowerCase().includes(search);
    const matchesStatus = statusFilter === "ALL" || normalizedStatus === statusFilter;

    return matchesSearch && matchesStatus;
  });
  const sortedJobs = [...filteredJobs].sort((a, b) => {
    return getEpochMillis(b.createdAt) - getEpochMillis(a.createdAt);
  });
  const {
    pageRows: visibleJobs,
    safePage: visibleJobsPage
  } = paginateRows(sortedJobs, currentPage, pageSize);

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="admin-loading-spinner" />
        <p>Loading jobs...</p>
      </div>
    );
  }

  if (error && jobs.length === 0) {
    return <div className="admin-empty"><p>Unable to load jobs. {error}</p></div>;
  }

  return (
    <>
      {/* Edit modal */}
      {editing && (
        <div className="admin-modal-overlay" onClick={() => setEditing(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2>Edit Job</h2>
              <button className="admin-modal-close" onClick={() => setEditing(null)}>✕</button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-grid">
                <label className="admin-field">
                  <span>Job Title</span>
                  <input
                    value={editing.title}
                    onChange={(e) => setEditing({ ...editing, title: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Company</span>
                  <input
                    value={editing.companyName}
                    onChange={(e) => setEditing({ ...editing, companyName: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Address</span>
                  <input
                    value={editing.location}
                    onChange={(e) => setEditing({ ...editing, location: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Pay Amount</span>
                  <input
                    value={editing.payAmount}
                    onChange={(e) => setEditing({ ...editing, payAmount: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Vacancies</span>
                  <input
                    type="number"
                    value={editing.vacancies}
                    onChange={(e) => setEditing({ ...editing, vacancies: e.target.value })}
                  />
                </label>
                <label className="admin-field admin-field-check">
                  <input
                    type="checkbox"
                    checked={editing.isActive}
                    onChange={(e) => setEditing({ ...editing, isActive: e.target.checked })}
                  />
                  <span>Active</span>
                </label>
              </div>
              <label className="admin-field">
                <span>Description</span>
                <textarea
                  rows={4}
                  value={editing.description}
                  onChange={(e) => setEditing({ ...editing, description: e.target.value })}
                />
              </label>
            </div>
            <div className="admin-modal-footer">
              <button className="button ghost" onClick={() => setEditing(null)}>Cancel</button>
              <button className="button" onClick={handleSaveEdit} disabled={saving}>
                {saving ? "Saving..." : "Save Changes"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* FREE JOB POSTING CONFIG & USER GRANT PANEL */}
      <div style={{ display: "grid", gridTemplateColumns: "1.2fr 1fr", gap: 16, marginBottom: 20 }}>
        {/* Global Policy Card */}
        <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 12, padding: 18, boxShadow: "0 1px 3px rgba(0,0,0,0.05)" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 14, fontWeight: 700, color: "#0f172a" }}>
              <ShieldAlert size={18} color="#e11d48" />
              <span>Free Job Posting Policy (Employer Side)</span>
            </div>
            {freeConfigLoading && <RefreshCw size={14} className="animate-spin" color="#64748b" />}
          </div>

          <div style={{ fontSize: 12, color: "#64748b", marginBottom: 14, lineHeight: "17px" }}>
            Control whether employers can post jobs without paying an active subscription. Default is <b>STOPPED (0 free jobs)</b>.
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, fontWeight: 600, cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={freeJobPostingEnabled}
                onChange={(e) => {
                  const checked = e.target.checked;
                  setFreeJobPostingEnabled(checked);
                  if (checked && applicableTo === "NONE") setApplicableTo("NEW_USERS");
                  if (checked && freeJobsCount === 0) setFreeJobsCount(3);
                  if (!checked) setApplicableTo("NONE");
                }}
              />
              <span>Enable Free Job Postings</span>
              <span style={{ fontSize: 11, padding: "2px 6px", borderRadius: 4, background: freeJobPostingEnabled ? "#dcfce7" : "#fee2e2", color: freeJobPostingEnabled ? "#15803d" : "#dc2626", fontWeight: 700 }}>
                {freeJobPostingEnabled ? "ACTIVE" : "COMPLETELY STOPPED"}
              </span>
            </label>

            {freeJobPostingEnabled && (
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, padding: 12, background: "#f8fafc", borderRadius: 8, border: "1px solid #e2e8f0" }}>
                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                    Free Jobs Count:
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={freeJobsCount}
                    onChange={(e) => setFreeJobsCount(parseInt(e.target.value) || 0)}
                    style={{ width: "100%", padding: "6px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                    Applies To:
                  </label>
                  <select
                    value={applicableTo}
                    onChange={(e) => setApplicableTo(e.target.value as any)}
                    style={{ width: "100%", padding: "6px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
                  >
                    <option value="NEW_USERS">New Users (from activation)</option>
                    <option value="ALL_USERS">All Users (New + Existing)</option>
                    <option value="NONE">Disabled (None)</option>
                  </select>
                </div>
              </div>
            )}

            {freeConfigMsg && (
              <div style={{ fontSize: 12, color: freeConfigMsg.type === "success" ? "#166534" : "#dc2626", fontWeight: 600 }}>
                {freeConfigMsg.text}
              </div>
            )}

            <button
              type="button"
              onClick={handleSaveFreeConfig}
              disabled={freeConfigSaving}
              style={{
                alignSelf: "flex-start",
                padding: "7px 16px",
                background: "#0f172a",
                color: "#ffffff",
                border: "none",
                borderRadius: 6,
                fontSize: 12,
                fontWeight: 700,
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                gap: 6
              }}
            >
              {freeConfigSaving ? "Saving Policy..." : "Save Policy"}
            </button>
          </div>
        </div>

        {/* User-specific Grant Card */}
        <div style={{ background: "#ffffff", border: "1px solid #e2e8f0", borderRadius: 12, padding: 18, boxShadow: "0 1px 3px rgba(0,0,0,0.05)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 14, fontWeight: 700, color: "#0f172a", marginBottom: 12 }}>
            <Gift size={18} color="#2563eb" />
            <span>Grant Free Jobs to Specific User</span>
          </div>

          <div style={{ fontSize: 12, color: "#64748b", marginBottom: 14, lineHeight: "17px" }}>
            Add free job post credits directly to any specific employer by phone number or UID.
          </div>

          <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                Employer Phone Number or UID:
              </label>
              <input
                type="text"
                placeholder="e.g. 9876543210 or user UID"
                value={grantPhoneOrUid}
                onChange={(e) => setGrantPhoneOrUid(e.target.value)}
                style={{ width: "100%", padding: "7px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
              />
            </div>

            <div>
              <label style={{ display: "block", fontSize: 12, fontWeight: 600, color: "#334155", marginBottom: 4 }}>
                Number of Free Jobs to Grant:
              </label>
              <input
                type="number"
                min="1"
                max="50"
                value={grantCount}
                onChange={(e) => setGrantCount(parseInt(e.target.value) || 1)}
                style={{ width: "100%", padding: "7px 10px", borderRadius: 6, border: "1px solid #cbd5e1", fontSize: 13 }}
              />
            </div>

            {grantMsg && (
              <div style={{ fontSize: 12, color: grantMsg.type === "success" ? "#166534" : "#dc2626", fontWeight: 600 }}>
                {grantMsg.text}
              </div>
            )}

            <button
              type="button"
              onClick={handleGrantUserFreeJobs}
              disabled={granting || !grantPhoneOrUid.trim()}
              style={{
                alignSelf: "flex-start",
                padding: "7px 16px",
                background: "#2563eb",
                color: "#ffffff",
                border: "none",
                borderRadius: 6,
                fontSize: 12,
                fontWeight: 700,
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                gap: 6
              }}
            >
              <UserCheck size={14} />
              {granting ? "Granting..." : "Grant Free Jobs"}
            </button>
          </div>
        </div>
      </div>

      <div className="admin-stats-grid small">
        <div className="admin-stat-card compact">
          <strong>{jobs.length}</strong>
          <span>Total jobs</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{openCount}</strong>
          <span>Open</span>
        </div>
        <div className="admin-stat-card compact">
          <strong>{jobs.length - openCount}</strong>
          <span>Closed</span>
        </div>
      </div>

      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder="Search jobs by title, company, or location..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <select
          className="admin-filter"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All statuses</option>
          <option value="open">Open</option>
          <option value="closed">Closed</option>
          <option value="expired">Expired</option>
          <option value="deleted">Deleted</option>
        </select>
        <span className="admin-count">{filteredJobs.length} jobs</span>
        <button 
          className="button small"
          onClick={handleActivateExpiredJobs}
          disabled={activatingExpired}
          style={{ marginLeft: 'auto' }}
        >
          {activatingExpired ? "Activating..." : "Activate Expired Jobs"}
        </button>
      </div>

      {error && <div className="admin-error">{error}</div>}

      {filteredJobs.length === 0 ? (
        <div className="admin-empty"><p>No jobs found.</p></div>
      ) : (
        <>
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Company</th>
                  <th>Location</th>
                  <th>Salary</th>
                  <th>Vacancies</th>
                  <th>Posted Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {visibleJobs.map((job) => (
                  <tr key={job.id}>
                    <td><strong>{job.title ?? "N/A"}</strong></td>
                    <td>{job.companyName ?? "N/A"}</td>
                    <td>{renderLocation(job)}</td>
                    <td>{job.payType === "NEGOTIABLE" ? "Negotiable" : formatCurrencyRange(job.payAmount, job.payType)}</td>
                    <td>{job.vacancies ?? 0}</td>
                    <td>{formatDateLocal(job.createdAt)}</td>
                    <td>
                      <button
                        className={`status-pill clickable ${job.isActive ? "success" : "danger"}`}
                        onClick={() => handleToggleActive(job.id, !!job.isActive)}
                        title="Click to toggle"
                      >
                        {job.isActive ? "Active" : "Inactive"}
                      </button>
                    </td>
                    <td>
                      <div className="admin-table-actions">
                        <button
                          type="button"
                          className="table-action"
                          onClick={() => startEdit(job)}
                        >
                          Edit
                        </button>
                        <Link
                          className="table-action"
                          href={`/admin/posters?jobId=${encodeURIComponent(job.id)}`}
                          prefetch={false}
                        >
                          Print Poster
                        </Link>
                        <button
                          type="button"
                          className="table-action danger"
                          onClick={() => void handleDelete(job.id)}
                          disabled={pendingDeleteId === job.id}
                        >
                          {pendingDeleteId === job.id ? "..." : "Delete"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <AdminTablePagination
            itemLabel="jobs"
            page={visibleJobsPage}
            pageSize={pageSize}
            totalItems={filteredJobs.length}
            onPageChange={setCurrentPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}
    </>
  );
}
