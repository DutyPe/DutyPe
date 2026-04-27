"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";

type JobRow = {
  id: string;
  title?: string;
  companyName?: string;
  // Stored geo on the card doc is { lat, lng } — NEVER render it as a child.
  location?: { lat?: number; lng?: number } | string;
  addressText?: string;
  companyCity?: string;
  payAmount?: number | string;
  salary?: number | string;
  payType?: string;
  salaryType?: string;
  vacancies?: number;
  status?: string;
  isActive?: boolean;
  applicationCount?: number;
  description?: string;
  category?: string;
  jobType?: string;
  shift?: string;
  shiftTiming?: string;
};

function renderLocation(job: JobRow): string {
  if (job.addressText && job.addressText.trim()) return job.addressText;
  if (job.companyCity && job.companyCity.trim()) return job.companyCity;
  if (typeof job.location === "string" && job.location.trim()) return job.location;
  if (job.location && typeof job.location === "object") {
    const { lat, lng } = job.location;
    if (typeof lat === "number" && typeof lng === "number") {
      return `${lat.toFixed(4)}, ${lng.toFixed(4)}`;
    }
  }
  return "N/A";
}

type EditingJob = {
  id: string;
  title: string;
  companyName: string;
  location: string;
  payAmount: string;
  vacancies: string;
  description: string;
  category: string;
  shift: string;
  gender: string;
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
  }, []);

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

  function startEdit(job: JobRow) {
    setEditing({
      id: job.id,
      title: job.title ?? "",
      companyName: job.companyName ?? "",
      location: renderLocation(job) === "N/A" ? "" : renderLocation(job),
      payAmount: String(job.payAmount ?? job.salary ?? ""),
      vacancies: String(job.vacancies ?? ""),
      description: job.description ?? "",
      category: job.category ?? job.jobType ?? "",
      shift: job.shift ?? job.shiftTiming ?? "",
      gender: (job as JobRow & { gender?: string }).gender ?? "Any",
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
          location: editing.location,
          salary: editing.payAmount,
          vacancies: Number(editing.vacancies) || 0,
          description: editing.description,
          category: editing.category,
          jobType: editing.category,
          shift: editing.shift,
          shiftTiming: editing.shift,
          gender: editing.gender,
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

  const filteredJobs = searchTerm
    ? jobs.filter(
        (j) =>
          (j.title ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
          (j.companyName ?? "").toLowerCase().includes(searchTerm.toLowerCase()) ||
          renderLocation(j).toLowerCase().includes(searchTerm.toLowerCase())
      )
    : jobs;

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
                  <span>Location</span>
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
                <label className="admin-field">
                  <span>Category</span>
                  <input
                    value={editing.category}
                    onChange={(e) => setEditing({ ...editing, category: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Shift</span>
                  <input
                    value={editing.shift}
                    onChange={(e) => setEditing({ ...editing, shift: e.target.value })}
                  />
                </label>
                <label className="admin-field">
                  <span>Gender</span>
                  <select
                    value={editing.gender}
                    onChange={(e) => setEditing({ ...editing, gender: e.target.value })}
                  >
                    <option value="Any">Any</option>
                    <option value="Male">Male</option>
                    <option value="Female">Female</option>
                  </select>
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

      {/* Toolbar */}
      <div className="admin-toolbar">
        <input
          type="text"
          className="admin-search"
          placeholder="Search jobs by title, company, or location..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <span className="admin-count">{filteredJobs.length} jobs</span>
      </div>

      {error && <div className="admin-error">{error}</div>}

      {filteredJobs.length === 0 ? (
        <div className="admin-empty"><p>No jobs found.</p></div>
      ) : (
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
                <th>Apps</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredJobs.map((job) => (
                <tr key={job.id}>
                  <td><strong>{job.title ?? "N/A"}</strong></td>
                  <td>{job.companyName ?? "N/A"}</td>
                  <td>{renderLocation(job)}</td>
                  <td>{formatCurrencyRange(job.payAmount ?? job.salary, job.payType ?? job.salaryType)}</td>
                  <td>{job.vacancies ?? 0}</td>
                  <td>
                    <button
                      className={`status-pill clickable ${job.isActive ? "success" : "danger"}`}
                      onClick={() => handleToggleActive(job.id, !!job.isActive)}
                      title="Click to toggle"
                    >
                      {job.isActive ? "Active" : "Inactive"}
                    </button>
                  </td>
                  <td>{job.applicationCount ?? 0}</td>
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
      )}
    </>
  );
}
