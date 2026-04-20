"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";

type AnnouncementRow = {
  id: string;
  title?: string;
  message?: string;
  type?: string;
  targetRole?: string;
  isActive?: boolean;
  imageUrl?: string;
  deepLink?: string;
  expiresAt?: unknown;
  createdAt?: unknown;
};

const initialForm = {
  title: "",
  message: "",
  type: "INFO",
  priority: "NORMAL",
  targetRole: "ALL",
  imageUrl: "",
  deepLink: "",
  actionText: "",
  expiresInDays: "30"
};

export function AdminAnnouncementsClient() {
  const [announcements, setAnnouncements] = useState<AnnouncementRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(initialForm);

  async function loadAnnouncements() {
    try {
      setLoading(true);
      const response = await adminApiFetch("/api/admin/announcements", {
        cache: "no-store"
      });

      const payload = (await response.json()) as {
        announcements?: AnnouncementRow[];
        error?: string;
      };

      if (!response.ok) {
        throw new Error(payload.error || "Failed to load announcements.");
      }

      setAnnouncements(payload.announcements ?? []);
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
        const response = await adminApiFetch("/api/admin/announcements", {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            title: form.title.trim(),
            message: form.message.trim(),
            type: form.type,
            priority: form.priority,
            targetRole: form.targetRole,
            imageUrl: form.imageUrl.trim() || undefined,
            deepLink: form.deepLink.trim() || undefined,
            actionText: form.actionText.trim() || undefined,
            expiresInDays: Number(form.expiresInDays) || 30
          })
        });

        const payload = (await response.json()) as { error?: string };
        if (!response.ok) {
          throw new Error(payload.error || "Failed to create announcement.");
        }

        setForm(initialForm);
        await loadAnnouncements();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to create announcement.");
    } finally {
      setSubmitting(false);
    }
  }

    async function handleToggle(row: AnnouncementRow) {
      try {
        const response = await adminApiFetch("/api/admin/announcements", {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            announcementId: row.id,
            isActive: !row.isActive
          })
        });

        const payload = (await response.json()) as { error?: string };
        if (!response.ok) {
          throw new Error(payload.error || "Failed to update announcement.");
        }

        await loadAnnouncements();
      } catch (toggleError) {
        setError(toggleError instanceof Error ? toggleError.message : "Failed to update announcement.");
      }
    }

    async function handleDelete(id: string) {
      const shouldDelete = window.confirm("Delete this announcement?");
      if (!shouldDelete) {
        return;
      }

      try {
        const response = await adminApiFetch("/api/admin/announcements", {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ announcementId: id })
        });

        const payload = (await response.json()) as { error?: string };
        if (!response.ok) {
          throw new Error(payload.error || "Failed to delete announcement.");
        }

        await loadAnnouncements();
      } catch (deleteError) {
        setError(deleteError instanceof Error ? deleteError.message : "Failed to delete announcement.");
      }
    }

  useEffect(() => {
    void loadAnnouncements();
  }, []);

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
              <option value="SUCCESS">SUCCESS</option>
              <option value="WARNING">WARNING</option>
              <option value="ERROR">ERROR</option>
              <option value="FEATURE">FEATURE</option>
              <option value="PROMOTION">PROMOTION</option>
            </select>
          </label>
          <label>
            <span>Priority</span>
            <select
              value={form.priority}
              onChange={(event) => setForm((current) => ({ ...current, priority: event.target.value }))}
            >
              <option value="LOW">LOW</option>
              <option value="NORMAL">NORMAL</option>
              <option value="HIGH">HIGH</option>
              <option value="URGENT">URGENT</option>
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
          <label>
            <span>Image URL (optional)</span>
            <input
              value={form.imageUrl}
              onChange={(event) => setForm((current) => ({ ...current, imageUrl: event.target.value }))}
              placeholder="https://…"
            />
          </label>
          <label>
            <span>Deep link (optional)</span>
            <input
              value={form.deepLink}
              onChange={(event) => setForm((current) => ({ ...current, deepLink: event.target.value }))}
              placeholder="dutype://referrals"
            />
          </label>
          <label>
            <span>Action button text (optional)</span>
            <input
              value={form.actionText}
              onChange={(event) => setForm((current) => ({ ...current, actionText: event.target.value }))}
              placeholder="View"
            />
          </label>
          <label>
            <span>Expires in (days)</span>
            <input
              type="number"
              min={1}
              max={365}
              value={form.expiresInDays}
              onChange={(event) => setForm((current) => ({ ...current, expiresInDays: event.target.value }))}
            />
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
