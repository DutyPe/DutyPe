"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";

type AnnouncementRow = {
  id: string;
  title?: string;
  message?: string;
  type?: string;
  priority?: string;
  targetRole?: string;
  isActive?: boolean;
  actionRoute?: string;
  expiresAt?: unknown;
  createdAt?: unknown;
};

// Pre-built templates so admins don't have to retype standard announcement copy.
// Picking a template auto-fills title, message, type, targetRole, and the
// matching deeplink — every field stays editable afterwards.
type AnnouncementTemplate = {
  id: string;
  label: string;
  title: string;
  message: string;
  type: string;
  priority: string;
  targetRole: string;
  deepLink: string;
};

const priorityRank: Record<string, number> = {
  URGENT: 5,
  HIGH: 4,
  MEDIUM: 3,
  NORMAL: 2,
  LOW: 1
};

function sortAnnouncementsForAdminFeed(rows: AnnouncementRow[]) {
  return [...rows].sort((first, second) => {
    const firstPriority = priorityRank[String(first.priority ?? "NORMAL").toUpperCase()] ?? 2;
    const secondPriority = priorityRank[String(second.priority ?? "NORMAL").toUpperCase()] ?? 2;
    return secondPriority - firstPriority;
  });
}

const ANNOUNCEMENT_TEMPLATES: AnnouncementTemplate[] = [
  {
    id: "custom",
    label: "✏️ Custom (write your own)",
    title: "",
    message: "",
    type: "INFO",
    priority: "NORMAL",
    targetRole: "ALL",
    deepLink: ""
  },
  {
    id: "new_jobs_nearby",
    label: "📍 New jobs available nearby",
    title: "New jobs near you!",
    message: "We just added fresh jobs in your area. Check them out before they fill up.",
    type: "INFO",
    priority: "HIGH",
    targetRole: "WORKER",
    deepLink: "dutype://jobs"
  },
  {
    id: "complete_profile",
    label: "👤 Complete your profile",
    title: "Finish your profile to get hired",
    message: "Workers with complete profiles get 3× more job calls. Add your skills and photo now.",
    type: "WARNING",
    priority: "HIGH",
    targetRole: "WORKER",
    deepLink: "dutype://profile"
  },
  {
    id: "referral_boost",
    label: "🎁 Referral bonus week",
    title: "Earn ₹100 per referral this week",
    message: "Invite your friends to DutyPe and earn cash for every signup that gets verified.",
    type: "PROMOTION",
    priority: "NORMAL",
    targetRole: "ALL",
    deepLink: "dutype://refer"
  },
  {
    id: "post_job_reminder",
    label: "📝 Post your first job",
    title: "Hire workers in minutes",
    message: "Post your first job free and get applications from verified workers near you.",
    type: "FEATURE",
    priority: "HIGH",
    targetRole: "EMPLOYER",
    deepLink: "dutype://post-job"
  },
  {
    id: "verify_employer",
    label: "✅ Verify your business",
    title: "Get the verified badge",
    message: "Verified employers receive 2× more applications. Submit your business documents now.",
    type: "INFO",
    priority: "NORMAL",
    targetRole: "EMPLOYER",
    deepLink: "dutype://employer/company-details"
  },
  {
    id: "app_update",
    label: "🚀 New app version available",
    title: "Update DutyPe for the latest features",
    message: "We've shipped a faster experience and bug fixes. Update from the Play Store now.",
    type: "FEATURE",
    priority: "HIGH",
    targetRole: "ALL",
    deepLink: "https://play.google.com/store/apps/details?id=com.dutype.app"
  },
  {
    id: "maintenance",
    label: "🛠 Scheduled maintenance",
    title: "Scheduled maintenance tonight",
    message: "DutyPe will be briefly unavailable tonight from 1 AM to 2 AM IST while we ship upgrades.",
    type: "WARNING",
    priority: "URGENT",
    targetRole: "ALL",
    deepLink: ""
  },
  {
    id: "support",
    label: "💬 Need help? Contact support",
    title: "We're here to help",
    message: "Tap below to chat with our support team if you have any questions.",
    type: "INFO",
    priority: "NORMAL",
    targetRole: "ALL",
    deepLink: "dutype://help"
  }
];

const initialForm = {
  templateId: "custom",
  title: "",
  message: "",
  type: "INFO",
  priority: "NORMAL",
  targetRole: "ALL",
  deepLink: "",
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

      setAnnouncements(sortAnnouncementsForAdminFeed(payload.announcements ?? []));
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
            deepLink: form.deepLink.trim() || undefined,
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
          <label className="editor-form-wide">
            <span>Template (auto-fills the fields below)</span>
            <select
              value={form.templateId}
              onChange={(event) => {
                const templateId = event.target.value;
                const template = ANNOUNCEMENT_TEMPLATES.find((t) => t.id === templateId);
                if (!template) {
                  setForm((current) => ({ ...current, templateId }));
                  return;
                }
                if (template.id === "custom") {
                  setForm((current) => ({ ...current, templateId }));
                  return;
                }
                setForm((current) => ({
                  ...current,
                  templateId,
                  title: template.title,
                  message: template.message,
                  type: template.type,
                  priority: template.priority,
                  targetRole: template.targetRole,
                  deepLink: template.deepLink
                }));
              }}
            >
              {ANNOUNCEMENT_TEMPLATES.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Title</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, templateId: "custom", title: event.target.value }))}
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
              <option value="MEDIUM">MEDIUM</option>
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
            <span>Deep link (optional)</span>
            <input
              value={form.deepLink}
              onChange={(event) => setForm((current) => ({ ...current, deepLink: event.target.value }))}
              placeholder="dutype://refer"
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
                    <strong>Priority</strong>
                    <span>{announcement.priority ?? "NORMAL"}</span>
                  </li>
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
                  <li className="admin-item">
                    <strong>Expires</strong>
                    <span>{formatDateTime(announcement.expiresAt)}</span>
                  </li>
                  <li className="admin-item">
                    <strong>Action</strong>
                    <span>{announcement.actionRoute || "None"}</span>
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
