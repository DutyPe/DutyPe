"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  addDoc,
  collection,
  deleteDoc,
  doc,
  getDocs,
  limit,
  orderBy,
  query,
  serverTimestamp,
  updateDoc
} from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";

type AnnouncementRow = {
  id: string;
  title?: string;
  message?: string;
  type?: string;
  targetRole?: string;
  isActive?: boolean;
  createdAt?: unknown;
};

const initialForm = {
  title: "",
  message: "",
  type: "INFO",
  targetRole: "ALL"
};

export function AdminAnnouncementsClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [announcements, setAnnouncements] = useState<AnnouncementRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(initialForm);

  async function loadAnnouncements() {
    if (!services) {
      setError("Firebase is not configured.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const snapshot = await getDocs(
        query(collection(services.db, "announcements"), orderBy("createdAt", "desc"), limit(50))
      );

      setAnnouncements(
        snapshot.docs.map((item) => ({
          id: item.id,
          ...(item.data() as Omit<AnnouncementRow, "id">)
        }))
      );
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadAnnouncements();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!services) {
      return;
    }

    try {
      setSubmitting(true);
      await addDoc(collection(services.db, "announcements"), {
        title: form.title.trim(),
        message: form.message.trim(),
        type: form.type,
        targetRole: form.targetRole,
        isActive: true,
        priority: 1,
        createdAt: serverTimestamp()
      });

      setForm(initialForm);
      await loadAnnouncements();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to create announcement.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggle(row: AnnouncementRow) {
    if (!services) {
      return;
    }

    try {
      await updateDoc(doc(services.db, "announcements", row.id), {
        isActive: !row.isActive
      });
      await loadAnnouncements();
    } catch (toggleError) {
      setError(toggleError instanceof Error ? toggleError.message : "Failed to update announcement.");
    }
  }

  async function handleDelete(id: string) {
    if (!services) {
      return;
    }

    const shouldDelete = window.confirm("Delete this announcement?");
    if (!shouldDelete) {
      return;
    }

    try {
      await deleteDoc(doc(services.db, "announcements", id));
      await loadAnnouncements();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete announcement.");
    }
  }

  return (
    <div className="admin-section-stack">
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Create announcement</span>
            <h2>Publish directly from the React admin route</h2>
          </div>
          <p>
            This replaces the prompt-based flow in the legacy HTML page with a typed
            form that can later support validation and preview states.
          </p>
        </div>

        <form className="editor-form" onSubmit={handleSubmit}>
          <label>
            <span>Title</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="Referral boost week"
              required
            />
          </label>
          <label className="editor-form-wide">
            <span>Message</span>
            <textarea
              value={form.message}
              onChange={(event) => setForm((current) => ({ ...current, message: event.target.value }))}
              placeholder="Tell workers or employers what changed."
              rows={4}
              required
            />
          </label>
          <label>
            <span>Type</span>
            <select
              value={form.type}
              onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}
            >
              <option value="INFO">INFO</option>
              <option value="WARNING">WARNING</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="ERROR">ERROR</option>
            </select>
          </label>
          <label>
            <span>Target role</span>
            <select
              value={form.targetRole}
              onChange={(event) => setForm((current) => ({ ...current, targetRole: event.target.value }))}
            >
              <option value="ALL">ALL</option>
              <option value="WORKER">WORKER</option>
              <option value="EMPLOYER">EMPLOYER</option>
            </select>
          </label>
          <div className="editor-form-actions">
            <button type="submit" className="button" disabled={submitting}>
              {submitting ? "Publishing..." : "Publish announcement"}
            </button>
          </div>
        </form>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Live announcements</span>
            <h2>Current announcement feed</h2>
          </div>
          <p>
            This route now reads the `announcements` collection directly instead of
            relying on a one-off static HTML tool.
          </p>
        </div>

        {loading ? <div className="empty-state">Loading announcements from Firestore.</div> : null}
        {!loading && error ? <div className="empty-state">Unable to load announcements. {error}</div> : null}
        {!loading && !error && announcements.length === 0 ? (
          <div className="empty-state">No announcements were returned from Firestore.</div>
        ) : null}

        {!loading && !error && announcements.length > 0 ? (
          <div className="admin-grid">
            {announcements.map((announcement) => (
              <article key={announcement.id} className="admin-panel">
                <span className="card-kicker">{announcement.type ?? "INFO"}</span>
                <h3>{announcement.title ?? "Untitled announcement"}</h3>
                <p>{announcement.message ?? "No message provided."}</p>
                <ul className="admin-list">
                  <li className="admin-item">
                    <strong>Target role</strong>
                    <span>{announcement.targetRole ?? "ALL"}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Status</strong>
                    <span>{announcement.isActive ? "Active" : "Inactive"}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Created</strong>
                    <span>{formatDateTime(announcement.createdAt)}</span>
                  </li>
                </ul>
                <div className="button-row">
                  <button type="button" className="button ghost" onClick={() => void handleToggle(announcement)}>
                    {announcement.isActive ? "Deactivate" : "Activate"}
                  </button>
                  <button type="button" className="table-action danger" onClick={() => void handleDelete(announcement.id)}>
                    Delete
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </section>
    </div>
  );
}
