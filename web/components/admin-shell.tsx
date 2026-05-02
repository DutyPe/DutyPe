"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo, useState } from "react";
import { signOut } from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";

type AdminLink = {
  href: string;
  label: string;
  icon: string;
  summary: string;
};

const adminSections: Array<{ label: string; links: AdminLink[] }> = [
  {
    label: "Overview",
    links: [
      { href: "/admin", label: "Dashboard", icon: "DB", summary: "Live platform health and recent activity." },
      { href: "/admin/routes", label: "Routes", icon: "RT", summary: "Every admin route in one place." }
    ]
  },
  {
    label: "People",
    links: [
      { href: "/admin/users", label: "Users", icon: "US", summary: "Accounts, roles, and activity." },
      { href: "/admin/worker-profiles", label: "Worker Profiles", icon: "WK", summary: "Worker profile collection review." },
      { href: "/admin/employer-profiles", label: "Employer Profiles", icon: "EM", summary: "Employer profile collection review." }
    ]
  },
  {
    label: "Hiring",
    links: [
      { href: "/admin/jobs", label: "Jobs", icon: "JB", summary: "Moderate live job posts and hiring data." },
      { href: "/admin/post-job", label: "Post Job", icon: "PJ", summary: "Create a verified job as admin." },
      { href: "/admin/posters", label: "Posters", icon: "PR", summary: "Generate printable job posters." },
      { href: "/admin/applications", label: "Applications", icon: "AP", summary: "Review worker applications." },
      { href: "/admin/instant-help", label: "Instant Help", icon: "IH", summary: "Urgent request speed, fill rate, and expiry metrics." },
      { href: "/admin/saved-jobs", label: "Saved Jobs", icon: "SV", summary: "Saved-job collection review." },
      { href: "/admin/ratings", label: "Ratings", icon: "RG", summary: "Ratings and trust signals." },
      { href: "/admin/job-reports", label: "Job Reports", icon: "RP", summary: "Reported jobs and moderation signals." }
    ]
  },
  {
    label: "Growth",
    links: [
      { href: "/admin/referrals", label: "Referrals", icon: "RF", summary: "Referral operations and withdrawals." },
      { href: "/admin/referral-stats", label: "Referral Stats", icon: "RS", summary: "Referral reward and audit data." },
      { href: "/admin/referral-codes", label: "Referral Codes", icon: "RC", summary: "Referral code collection review." },
      { href: "/admin/marketing", label: "Marketing & Growth", icon: "MK", summary: "Campaigns, assets, SEO, and launch playbooks." }
    ]
  },
  {
    label: "Messaging",
    links: [
      { href: "/admin/notifications", label: "Notifications", icon: "NT", summary: "Campaign sends and notification history." },
      { href: "/admin/announcements", label: "Announcements", icon: "AN", summary: "In-app announcements and message control." }
    ]
  }
];

const adminLinks = adminSections.flatMap((section) => section.links);

export function AdminShell({
  title,
  description,
  children
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const services = useMemo(() => getFirebaseServices(), []);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const activeLink = adminLinks.find((link) =>
    link.href === "/admin" ? pathname === "/admin" : pathname.startsWith(link.href)
  );
  const topbarDescription = description ?? activeLink?.summary;

  async function handleSidebarLogout() {
    const shouldLogout = window.confirm("Do you really want to log out from the admin console?");

    if (!shouldLogout) {
      return;
    }

    try {
      await fetch("/api/admin/session", {
        method: "DELETE"
      });

      if (services?.auth) {
        await signOut(services.auth);
      }

      window.location.href = "/admin/login";
    } catch {
      window.alert("Unable to log out right now. Please try again.");
    }
  }

  return (
    <div className="admin-layout">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="admin-overlay" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="admin-sidebar-header">
          <Link href="/admin" className="admin-brand">
            <span className="admin-brand-mark">DP</span>
            <div>
              <strong>DutyPe</strong>
              <small>Admin Console</small>
            </div>
          </Link>
          <button
            className="admin-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close sidebar"
          >
            ✕
          </button>
        </div>

        <nav className="admin-sidebar-nav" aria-label="Admin navigation">
          {adminSections.map((section) => (
            <div key={section.label} className="admin-sidebar-section">
              <span className="admin-sidebar-section-label">{section.label}</span>
              {section.links.map((link) => {
                const isActive =
                  link.href === "/admin"
                    ? pathname === "/admin"
                    : pathname.startsWith(link.href);
                return (
                  <Link
                    key={link.href}
                    href={link.href}
                    className={`admin-sidebar-link ${isActive ? "active" : ""}`}
                    onClick={() => setSidebarOpen(false)}
                    aria-current={isActive ? "page" : undefined}
                    title={link.summary}
                  >
                    <span className="admin-sidebar-icon" aria-hidden="true">{link.icon}</span>
                    <span className="admin-sidebar-link-text">
                      <span>{link.label}</span>
                      <small>{link.summary}</small>
                    </span>
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="admin-sidebar-footer">
          <button
            type="button"
            className="admin-sidebar-link admin-sidebar-link-button danger"
            onClick={handleSidebarLogout}
          >
            <span className="admin-sidebar-icon" aria-hidden="true">LO</span>
            <span>Logout</span>
          </button>

          <Link href="/" className="admin-sidebar-link">
            <span className="admin-sidebar-icon" aria-hidden="true">WB</span>
            <span>Public Site</span>
          </Link>
        </div>
      </aside>

      {/* Main content */}
      <main className="admin-content">
        <header className="admin-topbar">
          <div className="admin-topbar-main">
            <button
              className="admin-menu-btn"
              onClick={() => setSidebarOpen(true)}
              aria-label="Open menu"
            >
              <span /><span /><span />
            </button>
            <div className="admin-topbar-info">
              <span className="admin-topbar-kicker">DutyPe operations</span>
              <h1>{title}</h1>
              {topbarDescription && <p>{topbarDescription}</p>}
            </div>
          </div>
          <div className="admin-topbar-actions" aria-label="Admin shortcuts">
            <Link href="/admin/post-job" className="admin-topbar-action primary">
              Post job
            </Link>
            <Link href="/admin/jobs" className="admin-topbar-action">
              Jobs
            </Link>
            <Link href="/" className="admin-topbar-action">
              Public
            </Link>
          </div>
        </header>

        <div className="admin-body">
          {children}
        </div>
      </main>
    </div>
  );
}
