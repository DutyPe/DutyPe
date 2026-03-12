"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  getDocs,
  limit,
  orderBy,
  query,
  updateDoc
} from "firebase/firestore";

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
  description?: string;
  category?: string;
  shift?: string;
};

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
  isActive: boolean;
};

export function AdminJobsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<JobRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [editing, setEditing] = useState<EditingJob | null>(null);
  const [saving, setSaving] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  async function loadJobs() {
    if (!services) {
      setError("Firebase is not configured.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const snapshot = await getDocs(
        query(collection(services.db, "jobs"), orderBy("createdAt", "desc"), limit(200))
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
    if (!services) return;
    if (!window.confirm("Are you sure you want to delete this job? This cannot be undone.")) return;

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

  async function handleToggleActive(jobId: string, currentActive: boolean) {
    if (!services) return;

    try {
      await updateDoc(doc(services.db, "jobs", jobId), { isActive: !currentActive });
      setJobs((prev) =>
        prev.map((j) => (j.id === jobId ? { ...j, isActive: !currentActive } : j))
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
      location: job.location ?? "",
      payAmount: String(job.payAmount ?? ""),
      vacancies: String(job.vacancies ?? ""),
      description: job.description ?? "",
      category: job.category ?? "",
      shift: job.shift ?? "",
      isActive: job.isActive ?? false
    });
  }

  async function handleSaveEdit() {
    if (!services || !editing) return;

    try {
      setSaving(true);
      await updateDoc(doc(services.db, "jobs", editing.id), {
        title: editing.title,
        companyName: editing.companyName,
        location: editing.location,
        payAmount: editing.payAmount,
        vacancies: Number(editing.vacancies) || 0,
        description: editing.description,
        category: editing.category,
        shift: editing.shift,
        isActive: editing.isActive
      });
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
          (j.location ?? "").toLowerCase().includes(searchTerm.toLowerCase())
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
                  <td>{job.location ?? "N/A"}</td>
                  <td>{formatCurrencyRange(job.payAmount, job.payType)}</td>
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
