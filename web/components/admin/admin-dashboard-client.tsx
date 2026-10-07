"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { onAuthStateChanged, type User } from "firebase/auth";
import {
  Users,
  Briefcase,
  FileText,
  Gift,
  PlusCircle,
  Zap,
  Bell,
  Megaphone,
  Compass,
  MapPin,
  Building2,
  HardHat,
  ArrowRight,
  Sparkles,
  TrendingUp
} from "lucide-react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import { getFirebaseServices } from "@/lib/firebase/client";
import { readTimestamp } from "@/lib/firebase/firestore-helpers";

type DashboardSnapshot = {
  totalUsers: number;
  workers: number;
  employers: number;
  totalJobs: number;
  activeJobs: number;
  totalApplications: number;
  pendingApplications: number;
  totalReferrals: number;
  completedReferrals: number;
  stateStats: Record<string, { workers: number; employers: number; total: number }>;
  recentJobs: {
    id: string;
    title: string;
    companyName: string;
    isActive: boolean;
    createdAt: string;
  }[];
  recentUsers: {
    id: string;
    name: string;
    role: string;
    joinedAt: string;
  }[];
  recentApplications: {
    id: string;
    workerName: string;
    jobTitle: string;
    status: string;
    appliedAt: string;
  }[];
};

type DashboardApiResult<T> = {
  data: T;
  error: string | null;
};

let sessionRefreshInFlight: Promise<void> | null = null;

async function ensureAdminServerSession() {
  const services = getFirebaseServices();

  if (!services) {
    throw new Error("Firebase is not configured for the web app.");
  }

  const currentUser = await (async (): Promise<User | null> => {
    if (services.auth.currentUser) {
      return services.auth.currentUser;
    }

    return new Promise((resolve) => {
      const timeout = setTimeout(() => {
        unsubscribe();
        resolve(services.auth.currentUser);
      }, 5000);

      const unsubscribe = onAuthStateChanged(
        services.auth,
        (user) => {
          clearTimeout(timeout);
          unsubscribe();
          resolve(user);
        },
        () => {
          clearTimeout(timeout);
          unsubscribe();
          resolve(null);
        }
      );
    });
  })();

  if (!currentUser) {
    throw new Error("Unauthorized");
  }

  const idToken = await currentUser.getIdToken(true);
  const response = await fetch("/api/admin/session", {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ idToken })
  });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as { error?: string } | null;
    throw new Error(payload?.error ?? "Unable to refresh admin session.");
  }
}

async function refreshAdminServerSessionOnce() {
  if (!sessionRefreshInFlight) {
    sessionRefreshInFlight = ensureAdminServerSession().finally(() => {
      sessionRefreshInFlight = null;
    });
  }

  await sessionRefreshInFlight;
}

async function fetchAdminJson<T>(url: string, fallback: T): Promise<DashboardApiResult<T>> {
  async function request() {
    const response = await adminApiFetch(url, {
      cache: "no-store"
    });

    const payload = (await response.json().catch(() => null)) as
      | ({ error?: string } & Partial<Record<string, unknown>>)
      | null;

    return { response, payload };
  }

  try {
    let { response, payload } = await request();

    if (response.status === 401) {
      try {
        await refreshAdminServerSessionOnce();
        await new Promise((resolve) => setTimeout(resolve, 120));
        ({ response, payload } = await request());
      } catch (sessionError) {
        return {
          data: fallback,
          error: `${url}: ${sessionError instanceof Error ? sessionError.message : "Unauthorized"}`
        };
      }
    }

    if (!response.ok) {
      return {
        data: fallback,
        error: `${url}: ${payload?.error || `Failed to load ${url}`}`
      };
    }

    return {
      data: (payload as unknown as T) ?? fallback,
      error: null
    };
  } catch (error) {
    return {
      data: fallback,
      error: `${url}: ${error instanceof Error ? error.message : `Failed to load ${url}`}`
    };
  }
}

const quickActions = [
  { href: "/admin/post-job", label: "Post a Job", icon: PlusCircle, color: "#087f68", bg: "#e8f5ef" },
  { href: "/admin/users", label: "Manage Users", icon: Users, color: "#0284c7", bg: "#e0f2fe" },
  { href: "/admin/jobs", label: "Moderate Jobs", icon: Briefcase, color: "#7c3aed", bg: "#f3e8ff" },
  { href: "/admin/applications", label: "Applications", icon: FileText, color: "#d97706", bg: "#fef3c7" },
  { href: "/admin/instant-help", label: "Instant Help", icon: Zap, color: "#ea580c", bg: "#ffedd5" },
  { href: "/admin/referrals", label: "Referrals", icon: Gift, color: "#e11d48", bg: "#ffe4e6" },
  { href: "/admin/notifications", label: "Notifications", icon: Bell, color: "#dc2626", bg: "#fee2e2" },
  { href: "/admin/announcements", label: "Announcements", icon: Megaphone, color: "#2563eb", bg: "#dbeafe" },
  { href: "/admin/routes", label: "Routes & Tools", icon: Compass, color: "#475569", bg: "#f1f5f9" }
];

export function AdminDashboardClient() {
  const [state, setState] = useState<{
    loading: boolean;
    error: string | null;
    snapshot: DashboardSnapshot | null;
  }>({
    loading: true,
    error: null,
    snapshot: null
  });

  useEffect(() => {
    async function load() {
      try {
        const result = await fetchAdminJson<DashboardSnapshot | null>("/api/admin/dashboard", null);
        setState({
          loading: false,
          error: result.error,
          snapshot: result.data
        });
      } catch (loadError) {
        setState({
          loading: false,
          error: loadError instanceof Error ? loadError.message : "Failed to load dashboard.",
          snapshot: null
        });
      }
    }

    void load();
  }, []);

  if (state.loading) {
    return (
      <div className="admin-loading">
        <div className="admin-loading-spinner" />
        <p>Loading dashboard data...</p>
      </div>
    );
  }

  if (state.error || !state.snapshot) {
    return (
      <div className="admin-empty">
        <p>Unable to load dashboard. {state.error ?? ""}</p>
      </div>
    );
  }

  const s = state.snapshot;

  return (
    <>
      {/* Primary KPI Stats Row */}
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-icon-wrap" style={{ background: "#e8f5ef", color: "#087f68" }}>
            <Users size={22} strokeWidth={2.2} />
          </div>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Total Users</span>
            <strong className="admin-stat-value">{s.totalUsers.toLocaleString()}</strong>
            <div className="admin-stat-chips">
              <span className="admin-stat-chip worker">👷 {s.workers} workers</span>
              <span className="admin-stat-chip employer">🏢 {s.employers} employers</span>
            </div>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="admin-stat-icon-wrap" style={{ background: "#e0f2fe", color: "#0284c7" }}>
            <Briefcase size={22} strokeWidth={2.2} />
          </div>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Total Jobs</span>
            <strong className="admin-stat-value">{s.totalJobs.toLocaleString()}</strong>
            <div className="admin-stat-chips">
              <span className="admin-stat-chip success">🟢 {s.activeJobs} active</span>
              <span className="admin-stat-chip neutral">{s.totalJobs - s.activeJobs} closed</span>
            </div>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="admin-stat-icon-wrap" style={{ background: "#fef3c7", color: "#d97706" }}>
            <FileText size={22} strokeWidth={2.2} />
          </div>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Applications</span>
            <strong className="admin-stat-value">{s.totalApplications.toLocaleString()}</strong>
            <div className="admin-stat-chips">
              <span className="admin-stat-chip warning">⏳ {s.pendingApplications} pending</span>
            </div>
          </div>
        </div>

        <div className="admin-stat-card">
          <div className="admin-stat-icon-wrap" style={{ background: "#fce7f3", color: "#db2777" }}>
            <Gift size={22} strokeWidth={2.2} />
          </div>
          <div className="admin-stat-info">
            <span className="admin-stat-label">Referrals</span>
            <strong className="admin-stat-value">{s.totalReferrals.toLocaleString()}</strong>
            <div className="admin-stat-chips">
              <span className="admin-stat-chip accent">✨ {s.completedReferrals} completed</span>
            </div>
          </div>
        </div>
      </div>

      {/* State-wise Distribution */}
      <div className="admin-section">
        <div className="admin-section-header">
          <div className="admin-section-title-wrap">
            <MapPin size={18} className="admin-section-icon-inline" />
            <h2 className="admin-section-title">Regional Distribution (Telangana & Andhra Pradesh)</h2>
          </div>
          <Link href="/admin/users" className="admin-view-all" prefetch={false}>
            <span>View User Directory</span>
            <ArrowRight size={14} />
          </Link>
        </div>
        
        {/* Highlight Cards for Top Focus States */}
        <div className="admin-stats-grid small" style={{ marginBottom: "1rem" }}>
          {["Telangana", "Andhra Pradesh"].map((st) => {
            const counts = s.stateStats[st] || { workers: 0, employers: 0, total: 0 };
            return (
              <div key={st} className="admin-stat-card state-focus-card">
                <div className="admin-stat-icon-wrap" style={{ background: "#e8f5ef", color: "#087f68" }}>
                  <Building2 size={20} />
                </div>
                <div className="admin-stat-info">
                  <div className="admin-state-header">
                    <strong>{st}</strong>
                    <span className="admin-badge-primary">Core Market</span>
                  </div>
                  <span className="admin-state-total">{counts.total.toLocaleString()} users registered</span>
                  <div className="admin-stat-chips" style={{ marginTop: "4px" }}>
                    <span className="admin-stat-chip worker">👷 {counts.workers} workers</span>
                    <span className="admin-stat-chip employer">🏢 {counts.employers} employers</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* State Breakdown Table */}
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>State / Region</th>
                <th style={{ textAlign: "right" }}>👷 Workers</th>
                <th style={{ textAlign: "right" }}>🏢 Employers</th>
                <th style={{ textAlign: "right" }}>Total Users</th>
                <th style={{ minWidth: "160px" }}>Share</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(s.stateStats).length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", color: "#64748b", padding: "1.5rem" }}>
                    No location data available yet
                  </td>
                </tr>
              ) : (
                Object.entries(s.stateStats)
                  .sort((a, b) => b[1].total - a[1].total)
                  .map(([stateName, counts]) => {
                    const pct = s.totalUsers > 0 ? Math.round((counts.total / s.totalUsers) * 100) : 0;
                    const isPrimary = stateName === "Telangana" || stateName === "Andhra Pradesh";
                    return (
                      <tr key={stateName} className={isPrimary ? "row-highlight" : ""}>
                        <td>
                          <div className="admin-table-cell-title">
                            <strong>{stateName}</strong>
                            {isPrimary && (
                              <span className="status-pill success" style={{ fontSize: "11px" }}>Primary Market</span>
                            )}
                          </div>
                        </td>
                        <td style={{ textAlign: "right", fontWeight: 600, color: "#0284c7" }}>{counts.workers.toLocaleString()}</td>
                        <td style={{ textAlign: "right", fontWeight: 600, color: "#7c3aed" }}>{counts.employers.toLocaleString()}</td>
                        <td style={{ textAlign: "right", fontWeight: 700, color: "#0f172a" }}>{counts.total.toLocaleString()}</td>
                        <td>
                          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                            <div className="admin-progress-track">
                              <div
                                className="admin-progress-fill"
                                style={{
                                  width: `${Math.min(pct, 100)}%`,
                                  background: isPrimary ? "linear-gradient(90deg, #087f68, #10b981)" : "#94a3b8"
                                }}
                              />
                            </div>
                            <span style={{ fontSize: "12px", fontWeight: 600, color: "#475569", minWidth: "34px", textAlign: "right" }}>{pct}%</span>
                          </div>
                        </td>
                      </tr>
                    );
                  })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Quick actions */}
      <div className="admin-section">
        <div className="admin-section-header">
          <div className="admin-section-title-wrap">
            <Sparkles size={18} className="admin-section-icon-inline" />
            <h2 className="admin-section-title">Quick Operations & Actions</h2>
          </div>
        </div>
        <div className="admin-actions-grid">
          {quickActions.map((action) => {
            const Icon = action.icon;
            return (
              <Link key={action.href} href={action.href} className="admin-action-card" prefetch={false}>
                <span className="admin-action-icon-wrap" style={{ background: action.bg, color: action.color }}>
                  <Icon size={20} strokeWidth={2.2} />
                </span>
                <span className="admin-action-label">{action.label}</span>
              </Link>
            );
          })}
        </div>
      </div>

      {/* Two-column: Recent Jobs + Recent Applications */}
      <div className="admin-two-col">
        <div className="admin-section">
          <div className="admin-section-header">
            <div className="admin-section-title-wrap">
              <Briefcase size={17} className="admin-section-icon-inline" />
              <h2 className="admin-section-title">Recent Job Listings</h2>
            </div>
            <Link href="/admin/jobs" className="admin-view-all" prefetch={false}>
              <span>View all</span>
              <ArrowRight size={13} />
            </Link>
          </div>
          <div className="admin-list-card">
            {s.recentJobs.length === 0 ? (
              <p className="admin-empty-text">No jobs posted yet</p>
            ) : (
              s.recentJobs.map((job) => (
                <div key={job.id} className="admin-list-item">
                  <div className="admin-list-item-info">
                    <strong>{job.title}</strong>
                    <span>{job.companyName || "Direct Employer"} · Posted {job.createdAt}</span>
                  </div>
                  <span className={`status-pill ${job.isActive ? "success" : "danger"}`}>
                    {job.isActive ? "Active" : "Closed"}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="admin-section">
          <div className="admin-section-header">
            <div className="admin-section-title-wrap">
              <FileText size={17} className="admin-section-icon-inline" />
              <h2 className="admin-section-title">Recent Applications</h2>
            </div>
            <Link href="/admin/applications" className="admin-view-all" prefetch={false}>
              <span>View all</span>
              <ArrowRight size={13} />
            </Link>
          </div>
          <div className="admin-list-card">
            {s.recentApplications.length === 0 ? (
              <p className="admin-empty-text">No applications received yet</p>
            ) : (
              s.recentApplications.map((app) => (
                <div key={app.id} className="admin-list-item">
                  <div className="admin-list-item-info">
                    <strong>{app.workerName || "Applicant"}</strong>
                    <span>{app.jobTitle}</span>
                  </div>
                  <div className="admin-list-item-meta">
                    <span
                      className={`status-pill ${
                        app.status === "PENDING"
                          ? "warning"
                          : app.status === "ACCEPTED" || app.status === "SHORTLISTED"
                            ? "success"
                            : app.status === "REJECTED"
                              ? "danger"
                              : "neutral"
                      }`}
                    >
                      {app.status}
                    </span>
                    <small>{app.appliedAt}</small>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Recently Joined Users */}
      <div className="admin-section">
        <div className="admin-section-header">
          <div className="admin-section-title-wrap">
            <Users size={17} className="admin-section-icon-inline" />
            <h2 className="admin-section-title">Recently Joined Accounts</h2>
          </div>
          <Link href="/admin/users" className="admin-view-all" prefetch={false}>
            <span>View all</span>
            <ArrowRight size={13} />
          </Link>
        </div>
        <div className="admin-list-card">
          {s.recentUsers.length === 0 ? (
            <p className="admin-empty-text">No registered users yet</p>
          ) : (
            s.recentUsers.map((user) => (
              <div key={user.id} className="admin-list-item">
                <div className="admin-list-item-info">
                  <div className="admin-user-row">
                    <span className="admin-user-avatar">
                      {(user.name || "U").substring(0, 1).toUpperCase()}
                    </span>
                    <div>
                      <strong>{user.name || "Unnamed User"}</strong>
                      <span>Joined {user.joinedAt}</span>
                    </div>
                  </div>
                </div>
                <div className="admin-list-item-meta">
                  <span className={`status-pill ${user.role === "EMPLOYER" ? "employer-pill" : "worker-pill"}`}>
                    {user.role}
                  </span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
}
