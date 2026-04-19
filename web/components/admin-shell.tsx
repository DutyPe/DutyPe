"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo, useState } from "react";
import { signOut } from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";

const adminLinks = [
  { href: "/admin", label: "Dashboard", icon: "📊" },
  { href: "/admin/users", label: "Users", icon: "👥" },
  { href: "/admin/jobs", label: "Jobs", icon: "💼" },
  { href: "/admin/post-job", label: "Post Job", icon: "➕" },
  { href: "/admin/applications", label: "Applications", icon: "📋" },
  { href: "/admin/referrals", label: "Referrals", icon: "🎁" },
  { href: "/admin/notifications", label: "Notifications", icon: "🔔" },
  { href: "/admin/announcements", label: "Announcements", icon: "📢" },
  { href: "/admin/marketing", label: "Marketing & Growth", icon: "🚀" },
  { href: "/admin/routes", label: "Routes", icon: "🧭" }
];

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

        <nav className="admin-sidebar-nav">
          {adminLinks.map((link) => {
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
              >
                <span className="admin-sidebar-icon">{link.icon}</span>
                <span>{link.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="admin-sidebar-footer">
          <button
            type="button"
            className="admin-sidebar-link admin-sidebar-link-button danger"
            onClick={handleSidebarLogout}
          >
            <span className="admin-sidebar-icon">🚪</span>
            <span>Logout</span>
          </button>

          <Link href="/" className="admin-sidebar-link">
            <span className="admin-sidebar-icon">🌐</span>
            <span>Public Site</span>
          </Link>
        </div>
      </aside>

      {/* Main content */}
      <main className="admin-content">
        <header className="admin-topbar">
          <button
            className="admin-menu-btn"
            onClick={() => setSidebarOpen(true)}
            aria-label="Open menu"
          >
            <span /><span /><span />
          </button>
          <div className="admin-topbar-info">
            <h1>{title}</h1>
            {description && <p>{description}</p>}
          </div>
        </header>

        <div className="admin-body">
          {children}
        </div>
      </main>
    </div>
  );
}
