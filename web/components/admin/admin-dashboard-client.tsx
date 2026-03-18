"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  collection,
  getDocs,
  limit,
  query
} from "firebase/firestore";

import {
  normalizeApplicationRecord,
  normalizeUserRecord
} from "@/lib/firebase/admin-normalizers";
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

const quickActions = [
  { href: "/admin/post-job", label: "Post a Job", icon: "➕", color: "#1a7f37" },
  { href: "/admin/users", label: "Manage Users", icon: "👥", color: "#0969da" },
  { href: "/admin/jobs", label: "Moderate Jobs", icon: "💼", color: "#8250df" },
  { href: "/admin/applications", label: "Applications", icon: "📋", color: "#bf8700" },
  { href: "/admin/referrals", label: "Referrals", icon: "🎁", color: "#cf222e" },
  { href: "/admin/announcements", label: "Announcements", icon: "📢", color: "#0550ae" },
  { href: "/admin/routes", label: "Routes", icon: "🧭", color: "#24292f" }
];

export function AdminDashboardClient() {
  const services = useMemo(() => getFirebaseServices(), []);
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
      if (!services) {
        setState({ loading: false, error: "Firebase is not configured.", snapshot: null });
        return;
      }

      try {
        const [jobsSnap, applicationsSnap, referralsSnap, usersSnap] = await Promise.all([
          getDocs(query(collection(services.db, "jobs"), limit(1000))),
          getDocs(query(collection(services.db, "applications"), limit(1000))),
          getDocs(query(collection(services.db, "referrals"), limit(1000))),
          getDocs(query(collection(services.db, "users"), limit(1000)))
        ]);

        const users = usersSnap.docs.map((doc) => ({ id: doc.id, ...(doc.data() as Record<string, unknown>) }));
        const jobs = jobsSnap.docs.map((doc) => ({ id: doc.id, ...doc.data() }));
        const applications = applicationsSnap.docs.map((doc) => ({
          id: doc.id,
          ...doc.data()
        }));
        const referrals = referralsSnap.docs.map((doc) => doc.data());
        const normalizedUsers = users.map((user) => normalizeUserRecord(String(user.id), user));
        const userById = new Map(normalizedUsers.map((user) => [user.id, user]));

        // Sort jobs by createdAt desc, take 5
        const sortedJobs = [...jobs]
          .sort((a: any, b: any) => {
            const ta = readTimestamp(a.createdAt);
            const tb = readTimestamp(b.createdAt);
            return (tb?.getTime() ?? 0) - (ta?.getTime() ?? 0);
          })
          .slice(0, 5);

        // Sort applications by appliedAt desc, take 5
        const sortedApps = [...applications]
          .sort((a: any, b: any) => {
            const ta = readTimestamp(a.appliedAt ?? a.createdAt);
            const tb = readTimestamp(b.appliedAt ?? b.createdAt);
            return (tb?.getTime() ?? 0) - (ta?.getTime() ?? 0);
          })
          .slice(0, 5);

        setState({
          loading: false,
          error: null,
          snapshot: {
            totalUsers: normalizedUsers.length,
            workers: normalizedUsers.filter((u) => u.roles.includes("WORKER") || u.activeRole === "WORKER").length,
            employers: normalizedUsers.filter((u) => u.roles.includes("EMPLOYER") || u.activeRole === "EMPLOYER").length,
            totalJobs: jobs.length,
            activeJobs: jobs.filter((j: any) => {
              const status = typeof j.status === "string" ? j.status.trim().toLowerCase() : "";
              return status ? status === "open" : !!j.isActive;
            }).length,
            totalApplications: applications.length,
            pendingApplications: applications.filter((a: any) => {
              const status = typeof a.status === "string" ? a.status.trim().toLowerCase() : "";
              return status === "applied" || status === "pending";
            }).length,
            totalReferrals: referrals.length,
            completedReferrals: referrals.filter((r: any) => String(r.status ?? "").trim().toUpperCase() === "COMPLETED").length,
            recentJobs: sortedJobs.map((j: any) => ({
              id: j.id,
              title: j.title ?? "Untitled",
              companyName: j.companyName ?? "Unknown",
              isActive: !!j.isActive,
              createdAt: readTimestamp(j.createdAt)?.toLocaleDateString("en-IN") ?? "N/A"
            })),
            recentUsers: [...normalizedUsers]
              .sort((a, b) => {
                const ta = readTimestamp(a.joinedAt);
                const tb = readTimestamp(b.joinedAt);
                return (tb?.getTime() ?? 0) - (ta?.getTime() ?? 0);
              })
              .slice(0, 6)
              .map((user) => {
                return {
                  id: user.id,
                  name: user.fullName || user.phone || user.id,
                  role: user.roles.length > 0 ? user.roles.join(", ") : user.activeRole,
                  joinedAt: readTimestamp(user.joinedAt)?.toLocaleDateString("en-IN") ?? "Recently active"
                };
              }),
            recentApplications: sortedApps.map((a: any) => {
              const normalized = normalizeApplicationRecord(a.id, a as Record<string, unknown>, userById.get(String(a.workerId ?? "")));

              return {
                id: normalized.id,
                workerName: normalized.workerName || normalized.workerId || "Unknown",
                jobTitle: normalized.jobTitle || "Untitled job",
                status: normalized.status,
                appliedAt: readTimestamp(normalized.appliedAt)?.toLocaleDateString("en-IN") ?? "Recently"
              };
            })
          }
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
  }, [services]);

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
      {/* Stats row */}
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#ddf4ff" }}>👥</div>
          <div className="admin-stat-info">
            <strong>{s.totalUsers}</strong>
            <span>Total Users</span>
            <small>{s.workers} workers · {s.employers} employers</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#dafbe1" }}>💼</div>
          <div className="admin-stat-info">
            <strong>{s.totalJobs}</strong>
            <span>Total Jobs</span>
            <small>{s.activeJobs} active</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#fff8c5" }}>📋</div>
          <div className="admin-stat-info">
            <strong>{s.totalApplications}</strong>
            <span>Applications</span>
            <small>{s.pendingApplications} pending</small>
          </div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-icon" style={{ background: "#ffebe9" }}>🎁</div>
          <div className="admin-stat-info">
            <strong>{s.totalReferrals}</strong>
            <span>Referrals</span>
            <small>{s.completedReferrals} completed</small>
          </div>
        </div>
      </div>

      {/* Quick actions */}
      <div className="admin-section">
        <h2 className="admin-section-title">Quick Actions</h2>
        <div className="admin-actions-grid">
          {quickActions.map((action) => (
            <Link key={action.href} href={action.href} className="admin-action-card">
              <span className="admin-action-icon">{action.icon}</span>
              <span className="admin-action-label">{action.label}</span>
            </Link>
          ))}
        </div>
      </div>

      {/* Two-column: Recent Jobs + Recent Applications */}
      <div className="admin-two-col">
        <div className="admin-section">
          <div className="admin-section-header">
            <h2 className="admin-section-title">Recent Jobs</h2>
            <Link href="/admin/jobs" className="admin-view-all">View all →</Link>
          </div>
          <div className="admin-list-card">
            {s.recentJobs.length === 0 ? (
              <p className="admin-empty-text">No jobs yet</p>
            ) : (
              s.recentJobs.map((job) => (
                <div key={job.id} className="admin-list-item">
                  <div className="admin-list-item-info">
                    <strong>{job.title}</strong>
                    <span>{job.companyName} · Posted {job.createdAt}</span>
                  </div>
                  <span className={`status-pill ${job.isActive ? "success" : "danger"}`}>
                    {job.isActive ? "Active" : "Inactive"}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        <div className="admin-section">
          <div className="admin-section-header">
            <h2 className="admin-section-title">Recent Applications</h2>
            <Link href="/admin/applications" className="admin-view-all">View all →</Link>
          </div>
          <div className="admin-list-card">
            {s.recentApplications.length === 0 ? (
              <p className="admin-empty-text">No applications yet</p>
            ) : (
              s.recentApplications.map((app) => (
                <div key={app.id} className="admin-list-item">
                  <div className="admin-list-item-info">
                    <strong>{app.workerName}</strong>
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

      <div className="admin-section">
        <div className="admin-section-header">
          <h2 className="admin-section-title">Recently Joined Users</h2>
          <Link href="/admin/users" className="admin-view-all">View all →</Link>
        </div>
        <div className="admin-list-card">
          {s.recentUsers.length === 0 ? (
            <p className="admin-empty-text">No users yet</p>
          ) : (
            s.recentUsers.map((user) => (
              <div key={user.id} className="admin-list-item">
                <div className="admin-list-item-info">
                  <strong>{user.name}</strong>
                  <span>{user.role}</span>
                </div>
                <div className="admin-list-item-meta">
                  <small>Joined {user.joinedAt}</small>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </>
  );
}
