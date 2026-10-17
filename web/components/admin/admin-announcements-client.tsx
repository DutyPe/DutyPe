"use client";

import { FormEvent, useEffect, useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { formatDateTime } from "@/lib/firebase/firestore-helpers";
import { AdminTablePagination, paginateRows } from "./admin-table-pagination";
import {
  Megaphone,
  Bell,
  Search,
  Filter,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
  Smartphone,
  Trash2,
  Power,
  ExternalLink,
  Users,
  ShieldAlert,
  ArrowRight,
  Info,
  Clock
} from "lucide-react";

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

const ANNOUNCEMENT_TEMPLATES: AnnouncementTemplate[] = [
  {
    id: "custom",
    label: "✏️ Custom (Write your own)",
    title: "",
    message: "",
    type: "INFO",
    priority: "NORMAL",
    targetRole: "ALL",
    deepLink: ""
  },
  {
    id: "promote_business",
    label: "🏪 Promote Your Shop or Brand (Partner With Us)",
    title: "Promote Your Business on DutyPe",
    message: "Reach 5,000+ local customers & workers across your district. Tap to advertise your shop or brand with us.",
    type: "PROMOTION",
    priority: "HIGH",
    targetRole: "ALL",
    deepLink: "dutype://promote-business"
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
  }
];

export function AdminAnnouncementsClient() {
  const [announcements, setAnnouncements] = useState<AnnouncementRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 8;

  const [form, setForm] = useState({
    templateId: "custom",
    title: "",
    message: "",
    type: "INFO",
    priority: "NORMAL",
    targetRole: "ALL",
    deepLink: "",
    expiresInDays: 7
  });

  async function loadAnnouncements() {
    setLoading(true);
    setError(null);
    try {
      const response = await adminApiFetch("/api/admin/announcements");
      const payload = (await response.json()) as { announcements?: AnnouncementRow[]; error?: string };
      if (!response.ok) {
        throw new Error(payload.error || "Failed to load announcements.");
      }
      const rawRows = payload.announcements ?? [];
      const sorted = [...rawRows].sort((first, second) => {
        const firstPriority = priorityRank[String(first.priority ?? "NORMAL").toUpperCase()] ?? 2;
        const secondPriority = priorityRank[String(second.priority ?? "NORMAL").toUpperCase()] ?? 2;
        return secondPriority - firstPriority;
      });
      setAnnouncements(sorted);
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
    if (!form.title.trim() || !form.message.trim()) {
      setError("Please provide a title and message.");
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const expiresAt = new Date();
      expiresAt.setDate(expiresAt.getDate() + (Number(form.expiresInDays) || 7));

      const response = await adminApiFetch("/api/admin/announcements", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          message: form.message.trim(),
          type: form.type,
          priority: form.priority,
          targetRole: form.targetRole,
          actionRoute: form.deepLink.trim() || undefined,
          expiresAt: expiresAt.toISOString(),
          isActive: true
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to publish announcement.");

      setSuccess("Announcement broadcasted successfully!");
      setForm({
        templateId: "custom",
        title: "",
        message: "",
        type: "INFO",
        priority: "NORMAL",
        targetRole: "ALL",
        deepLink: "",
        expiresInDays: 7
      });
      setTimeout(() => setSuccess(null), 3000);
      await loadAnnouncements();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to publish announcement.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggle(announcement: AnnouncementRow) {
    try {
      const response = await adminApiFetch("/api/admin/announcements", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          announcementId: announcement.id,
          isActive: !announcement.isActive
        })
      });

      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to update announcement.");

      await loadAnnouncements();
    } catch (toggleError) {
      setError(toggleError instanceof Error ? toggleError.message : "Failed to update announcement.");
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm("Are you sure you want to delete this announcement?")) return;
    try {
      const response = await adminApiFetch("/api/admin/announcements", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ announcementId: id })
      });
      const payload = (await response.json()) as { error?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to delete announcement.");
      await loadAnnouncements();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete announcement.");
    }
  }

  const activeCount = announcements.filter((a) => a.isActive).length;
  const urgentCount = announcements.filter((a) => String(a.priority ?? "").toUpperCase() === "URGENT").length;
  const promoCount = announcements.filter((a) => String(a.type ?? "").toUpperCase() === "PROMOTION").length;

  const filteredAnnouncements = announcements.filter((a) => {
    const search = searchTerm.toLowerCase();
    const targetRole = String(a.targetRole ?? "ALL").toUpperCase();
    const isActive = !!a.isActive;
    const matchesSearch =
      !searchTerm ||
      String(a.title ?? "").toLowerCase().includes(search) ||
      String(a.message ?? "").toLowerCase().includes(search) ||
      String(a.type ?? "").toLowerCase().includes(search) ||
      String(a.priority ?? "").toLowerCase().includes(search);
    const matchesRole = roleFilter === "ALL" || targetRole === roleFilter;
    const matchesStatus = statusFilter === "ALL" || (statusFilter === "ACTIVE" ? isActive : !isActive);
    return matchesSearch && matchesRole && matchesStatus;
  });

  const {
    pageRows: visibleAnnouncements,
    safePage: visibleAnnouncementsPage
  } = paginateRows(filteredAnnouncements, currentPage, pageSize);

  const getTypeBadge = (type?: string) => {
    const t = String(type ?? "INFO").toUpperCase();
    if (t === "PROMOTION") return "adm-badge-purple";
    if (t === "WARNING" || t === "ERROR") return "adm-badge-rose";
    if (t === "SUCCESS") return "adm-badge-emerald";
    if (t === "FEATURE") return "adm-badge-blue";
    return "adm-badge-neutral";
  };

  const getPriorityBadge = (priority?: string) => {
    const p = String(priority ?? "NORMAL").toUpperCase();
    if (p === "URGENT") return "adm-badge-rose";
    if (p === "HIGH") return "adm-badge-amber";
    return "adm-badge-neutral";
  };

  return (
    <div className="adm-stack">
      {/* Toast Alert */}
      {success && (
        <div className="adm-card" style={{ borderLeft: "4px solid #10b981", background: "#ecfdf5", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#065f46", fontSize: "0.875rem", fontWeight: 600 }}>
            <CheckCircle2 style={{ width: 18, height: 18, color: "#10b981", flexShrink: 0 }} />
            <span>{success}</span>
          </div>
        </div>
      )}

      {error && (
        <div className="adm-card" style={{ borderLeft: "4px solid #ef4444", background: "#fef2f2", padding: "12px 18px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, color: "#b91c1c", fontSize: "0.875rem", fontWeight: 600 }}>
            <AlertTriangle style={{ width: 18, height: 18, color: "#ef4444", flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        </div>
      )}

      {/* Header Row */}
      <div className="adm-header-row">
        <div>
          <h1 className="adm-page-title">In-App Announcements</h1>
          <p className="adm-page-subtitle">
            Broadcast targeted cards and alerts across Worker and Employer app home screens.
          </p>
        </div>
        <div style={{ display: "flex", gap: 10 }}>
          <button
            type="button"
            className="adm-btn adm-btn-secondary"
            onClick={() => void loadAnnouncements()}
            disabled={loading}
          >
            Refresh Feed
          </button>
        </div>
      </div>

      {/* Top Metrics Grid */}
      <div className="adm-metrics-grid">
        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Total Broadcasts</span>
            <div className="adm-metric-icon">
              <Megaphone style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{announcements.length}</div>
          <div className="adm-metric-sub">
            <span>all-time notices in database</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Live Active Banners</span>
            <div className="adm-metric-icon" style={{ background: "#ecfdf5", color: "#10b981" }}>
              <CheckCircle2 style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: "#059669" }}>{activeCount}</div>
          <div className="adm-metric-sub">
            <span>currently displayed in app</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Urgent Alerts</span>
            <div className="adm-metric-icon" style={{ background: "#fef2f2", color: "#ef4444" }}>
              <ShieldAlert style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value" style={{ color: urgentCount > 0 ? "#dc2626" : "var(--adm-text-main)" }}>
            {urgentCount}
          </div>
          <div className="adm-metric-sub">
            <span>pinned at highest priority</span>
          </div>
        </div>

        <div className="adm-metric-card">
          <div className="adm-metric-top">
            <span className="adm-metric-label">Promotional & Offers</span>
            <div className="adm-metric-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
              <Sparkles style={{ width: 18, height: 18 }} />
            </div>
          </div>
          <div className="adm-metric-value">{promoCount}</div>
          <div className="adm-metric-sub">
            <span>marketing & partner highlights</span>
          </div>
        </div>
      </div>

      {/* Split Studio: Composer & Live Preview on Top */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: 20 }}>
        {/* Left: Broadcast Form */}
        <div className="adm-card">
          <div className="adm-card-header">
            <h2 className="adm-card-title">
              <Megaphone style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
              Broadcast Studio
            </h2>
            <span className="adm-badge adm-badge-emerald">Live Push</span>
          </div>
          <form onSubmit={handleSubmit} className="adm-card-body" style={{ display: "flex", flexDirection: "column", gap: 14 }}>
            <div className="adm-form-group">
              <label className="adm-form-label">Quick Template Preset</label>
              <select
                className="adm-select"
                value={form.templateId}
                onChange={(e) => {
                  const tid = e.target.value;
                  const t = ANNOUNCEMENT_TEMPLATES.find((x) => x.id === tid);
                  if (!t || t.id === "custom") {
                    setForm((cur) => ({ ...cur, templateId: tid }));
                    return;
                  }
                  setForm((cur) => ({
                    ...cur,
                    templateId: tid,
                    title: t.title,
                    message: t.message,
                    type: t.type,
                    priority: t.priority,
                    targetRole: t.targetRole,
                    deepLink: t.deepLink
                  }));
                }}
              >
                {ANNOUNCEMENT_TEMPLATES.map((t) => (
                  <option key={t.id} value={t.id}>{t.label}</option>
                ))}
              </select>
            </div>

            <div className="adm-form-group">
              <label className="adm-form-label">Announcement Title *</label>
              <input
                type="text"
                className="adm-input"
                placeholder="e.g. New jobs available in your area!"
                value={form.title}
                onChange={(e) => setForm((cur) => ({ ...cur, templateId: "custom", title: e.target.value }))}
                required
              />
            </div>

            <div className="adm-form-group">
              <label className="adm-form-label">Message Copy *</label>
              <textarea
                className="adm-textarea"
                rows={3}
                placeholder="Tell workers or employers what changed or what action to take."
                value={form.message}
                onChange={(e) => setForm((cur) => ({ ...cur, message: e.target.value }))}
                required
              />
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <div className="adm-form-group">
                <label className="adm-form-label">Target Audience</label>
                <select
                  className="adm-select"
                  value={form.targetRole}
                  onChange={(e) => setForm((cur) => ({ ...cur, targetRole: e.target.value }))}
                >
                  <option value="ALL">Everyone (Workers & Employers)</option>
                  <option value="WORKER">Workers Only</option>
                  <option value="EMPLOYER">Employers Only</option>
                </select>
              </div>

              <div className="adm-form-group">
                <label className="adm-form-label">Visual Type</label>
                <select
                  className="adm-select"
                  value={form.type}
                  onChange={(e) => setForm((cur) => ({ ...cur, type: e.target.value }))}
                >
                  <option value="INFO">INFO (Neutral Blue)</option>
                  <option value="SUCCESS">SUCCESS (Green)</option>
                  <option value="WARNING">WARNING (Amber)</option>
                  <option value="ERROR">ERROR (Urgent Red)</option>
                  <option value="PROMOTION">PROMOTION (Purple Sparkle)</option>
                  <option value="FEATURE">FEATURE (Cyan)</option>
                </select>
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
              <div className="adm-form-group">
                <label className="adm-form-label">Priority Order</label>
                <select
                  className="adm-select"
                  value={form.priority}
                  onChange={(e) => setForm((cur) => ({ ...cur, priority: e.target.value }))}
                >
                  <option value="URGENT">URGENT (Top Pinned)</option>
                  <option value="HIGH">HIGH Priority</option>
                  <option value="NORMAL">NORMAL Priority</option>
                  <option value="LOW">LOW Priority</option>
                </select>
              </div>

              <div className="adm-form-group">
                <label className="adm-form-label">Expires In (Days)</label>
                <input
                  type="number"
                  min="1"
                  max="90"
                  className="adm-input"
                  value={form.expiresInDays}
                  onChange={(e) => setForm((cur) => ({ ...cur, expiresInDays: Number(e.target.value) || 7 }))}
                />
              </div>
            </div>

            <div className="adm-form-group">
              <label className="adm-form-label">Action Deeplink (Optional)</label>
              <input
                type="text"
                className="adm-input"
                placeholder="dutype://jobs or dutype://post-job"
                value={form.deepLink}
                onChange={(e) => setForm((cur) => ({ ...cur, deepLink: e.target.value }))}
              />
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 4 }}>
              <button
                type="submit"
                className="adm-btn adm-btn-primary"
                disabled={submitting}
                style={{ width: "100%" }}
              >
                {submitting ? "Publishing..." : "Publish Announcement"}
              </button>
            </div>
          </form>
        </div>

        {/* Right: Live Mobile App Banner Simulation Preview */}
        <div className="adm-card" style={{ display: "flex", flexDirection: "column" }}>
          <div className="adm-card-header">
            <h2 className="adm-card-title">
              <Smartphone style={{ width: 18, height: 18, color: "var(--adm-primary)" }} />
              Live Mobile In-App Preview
            </h2>
            <span className="adm-badge adm-badge-neutral">Real-time Simulation</span>
          </div>
          <div className="adm-card-body" style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center", alignItems: "center", background: "#f8fafc", padding: 24 }}>
            <div style={{ width: "100%", maxWidth: 360, background: "#ffffff", borderRadius: 16, border: "1px solid var(--adm-border)", boxShadow: "0 8px 16px rgba(0,0,0,0.06)", overflow: "hidden" }}>
              <div style={{ background: "#0f172a", padding: "10px 14px", display: "flex", alignItems: "center", justifyContent: "space-between", color: "#ffffff", fontSize: "0.75rem", fontWeight: 600 }}>
                <span>DutyPe Mobile App</span>
                <span className="adm-badge adm-badge-neutral" style={{ fontSize: "0.7rem", padding: "1px 6px" }}>
                  {form.targetRole === "WORKER" ? "Worker Home" : form.targetRole === "EMPLOYER" ? "Employer Home" : "All Users"}
                </span>
              </div>
              <div style={{ padding: 16 }}>
                <div
                  style={{
                    borderRadius: 12,
                    padding: "14px 16px",
                    background: form.type === "PROMOTION" ? "#f5f3ff" : form.type === "WARNING" ? "#fffbeb" : form.type === "ERROR" ? "#fef2f2" : "#eff6ff",
                    border: `1px solid ${form.type === "PROMOTION" ? "#ddd6fe" : form.type === "WARNING" ? "#fde68a" : form.type === "ERROR" ? "#fecdd3" : "#bfdbfe"}`,
                    display: "flex",
                    flexDirection: "column",
                    gap: 6
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span className={`adm-badge ${getTypeBadge(form.type)}`}>
                      {form.type}
                    </span>
                    {form.priority === "URGENT" && (
                      <span className="adm-badge adm-badge-rose" style={{ fontSize: "0.68rem" }}>
                        ⚡ Urgent
                      </span>
                    )}
                  </div>
                  <h4 style={{ margin: "2px 0 0", fontSize: "0.95rem", fontWeight: 700, color: "var(--adm-text-main)" }}>
                    {form.title.trim() || "Announcement Title Preview"}
                  </h4>
                  <p style={{ margin: 0, fontSize: "0.825rem", color: "var(--adm-text-muted)", lineHeight: 1.45 }}>
                    {form.message.trim() || "Type a message above to see how it renders in the user's mobile feed."}
                  </p>
                  {form.deepLink.trim() && (
                    <div style={{ display: "flex", alignItems: "center", gap: 4, marginTop: 4, fontSize: "0.75rem", color: "var(--adm-primary)", fontWeight: 600 }}>
                      <span>Tap to open action</span>
                      <ArrowRight style={{ width: 12, height: 12 }} />
                    </div>
                  )}
                </div>
              </div>
            </div>
            <p style={{ fontSize: "0.75rem", color: "var(--adm-text-muted)", marginTop: 14, textAlign: "center", maxWidth: 280 }}>
              Users see this announcement prominently above job listings on their app home screen until dismissed or expired.
            </p>
          </div>
        </div>
      </div>

      {/* Announcements Feed Toolbar */}
      <div className="adm-toolbar">
        <div className="adm-search-wrap">
          <Search className="adm-search-icon" />
          <input
            type="text"
            className="adm-input-search"
            placeholder="Search announcements by title, copy, type, or priority..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        <select
          className="adm-select"
          value={roleFilter}
          onChange={(e) => setRoleFilter(e.target.value)}
        >
          <option value="ALL">All Roles</option>
          <option value="WORKER">Workers</option>
          <option value="EMPLOYER">Employers</option>
        </select>
        <select
          className="adm-select"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
        >
          <option value="ALL">All Statuses</option>
          <option value="ACTIVE">Active Banners</option>
          <option value="INACTIVE">Deactivated</option>
        </select>
        <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", marginLeft: "auto", fontWeight: 500 }}>
          {filteredAnnouncements.length} results
        </span>
      </div>

      {/* Announcements List Table */}
      {loading ? (
        <div className="adm-card" style={{ padding: 32, display: "flex", flexDirection: "column", gap: 12 }}>
          <div className="adm-skeleton" style={{ height: 28, width: "30%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
          <div className="adm-skeleton" style={{ height: 44, width: "100%" }} />
        </div>
      ) : filteredAnnouncements.length === 0 ? (
        <div className="adm-card" style={{ padding: "48px 24px", textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
          <Megaphone style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
          <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No announcements match filters</h3>
          <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)", maxWidth: 360 }}>
            Try resetting your search query or role filter to view existing broadcasts.
          </p>
        </div>
      ) : (
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Announcement Content</th>
                <th>Target Role</th>
                <th>Type</th>
                <th>Priority</th>
                <th>Action Route</th>
                <th>Created / Expires</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibleAnnouncements.map((ann) => (
                <tr key={ann.id}>
                  <td>
                    <span className={`adm-badge ${ann.isActive ? "adm-badge-emerald" : "adm-badge-neutral"}`}>
                      {ann.isActive ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td style={{ maxWidth: 320 }}>
                    <div style={{ fontWeight: 600, color: "var(--adm-text-main)" }}>
                      {ann.title || "Untitled Announcement"}
                    </div>
                    <div style={{ fontSize: "0.775rem", color: "var(--adm-text-muted)", marginTop: 2, lineClamp: 2 }}>
                      {ann.message || "—"}
                    </div>
                  </td>
                  <td>
                    <span className="adm-badge adm-badge-neutral">
                      <Users style={{ width: 12, height: 12 }} />
                      {ann.targetRole ?? "ALL"}
                    </span>
                  </td>
                  <td>
                    <span className={`adm-badge ${getTypeBadge(ann.type)}`}>
                      {ann.type ?? "INFO"}
                    </span>
                  </td>
                  <td>
                    <span className={`adm-badge ${getPriorityBadge(ann.priority)}`}>
                      {ann.priority ?? "NORMAL"}
                    </span>
                  </td>
                  <td>
                    {ann.actionRoute ? (
                      <code style={{ fontSize: "0.75rem", background: "var(--adm-surface-subtle)", padding: "2px 6px", borderRadius: 4, color: "var(--adm-accent-blue)" }}>
                        {ann.actionRoute}
                      </code>
                    ) : (
                      <span style={{ color: "var(--adm-text-subtle)", fontSize: "0.8rem" }}>None</span>
                    )}
                  </td>
                  <td style={{ fontSize: "0.775rem", color: "var(--adm-text-muted)", whiteSpace: "nowrap" }}>
                    <div>{formatDateTime(ann.createdAt)}</div>
                    <div style={{ color: "var(--adm-text-subtle)" }}>Exp: {formatDateTime(ann.expiresAt)}</div>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <div style={{ display: "inline-flex", gap: 6 }}>
                      <button
                        type="button"
                        className="adm-btn adm-btn-secondary adm-btn-sm"
                        onClick={() => void handleToggle(ann)}
                        title={ann.isActive ? "Deactivate" : "Activate"}
                      >
                        <Power style={{ width: 13, height: 13, color: ann.isActive ? "#eab308" : "#10b981" }} />
                        {ann.isActive ? "Deactivate" : "Activate"}
                      </button>
                      <button
                        type="button"
                        className="adm-btn-icon"
                        style={{ color: "#ef4444" }}
                        title="Delete Announcement"
                        onClick={() => void handleDelete(ann.id)}
                      >
                        <Trash2 style={{ width: 14, height: 14 }} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ padding: "12px 18px", borderTop: "1px solid var(--adm-border)" }}>
            <AdminTablePagination
              itemLabel="announcements"
              page={visibleAnnouncementsPage}
              pageSize={pageSize}
              totalItems={filteredAnnouncements.length}
              onPageChange={setCurrentPage}
            />
          </div>
        </div>
      )}
    </div>
  );
}
