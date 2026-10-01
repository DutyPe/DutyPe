"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ReactNode, useMemo, useState } from "react";
import { signOut } from "firebase/auth";
import {
  LayoutDashboard,
  Compass,
  Users,
  MessageSquare,
  PhoneCall,
  UserX,
  HardHat,
  Building2,
  Briefcase,
  PlusCircle,
  Printer,
  FileText,
  Zap,
  Bookmark,
  Star,
  Gift,
  BarChart3,
  Ticket,
  Sliders,
  Sparkles,
  History,
  Rocket,
  CreditCard,
  Smartphone,
  Bell,
  Megaphone,
  Settings,
  LogOut,
  ExternalLink,
  Menu,
  X,
  Search,
  ShieldCheck,
  LucideIcon
} from "lucide-react";

import { getFirebaseServices } from "@/lib/firebase/client";

type AdminLink = {
  href: string;
  label: string;
  icon: LucideIcon;
  summary: string;
};

const adminSections: Array<{ label: string; links: AdminLink[] }> = [
  {
    label: "Overview",
    links: [
      { href: "/admin", label: "Dashboard", icon: LayoutDashboard, summary: "Live platform health, metrics & activity." },
      { href: "/admin/routes", label: "Routes & Tools", icon: Compass, summary: "Directory of every admin route." }
    ]
  },
  {
    label: "People & Directory",
    links: [
      { href: "/admin/users", label: "Users", icon: Users, summary: "Accounts, roles, and verification status." },
      { href: "/admin/whatsapp-groups", label: "WhatsApp Broadcasts", icon: MessageSquare, summary: "Export Worker/Employer broadcast contacts." },
      { href: "/admin/phone-roles", label: "Phone Roles", icon: PhoneCall, summary: "Phone number role mapping records." },
      { href: "/admin/delete-user-by-phone", label: "Delete by Phone", icon: UserX, summary: "Emergency account data cleanup." },
      { href: "/admin/worker-profiles", label: "Worker Profiles", icon: HardHat, summary: "Detailed worker profile collection." },
      { href: "/admin/employer-profiles", label: "Employer Profiles", icon: Building2, summary: "Employer & company accounts." }
    ]
  },
  {
    label: "Hiring & Moderation",
    links: [
      { href: "/admin/jobs", label: "Jobs Moderation", icon: Briefcase, summary: "Moderate live job posts and hiring listings." },
      { href: "/admin/post-job", label: "Post a Job", icon: PlusCircle, summary: "Create verified admin job listings." },
      { href: "/admin/posters", label: "Job Posters", icon: Printer, summary: "Generate high-res printable job posters." },
      { href: "/admin/applications", label: "Applications", icon: FileText, summary: "Review worker application records." },
      { href: "/admin/instant-help", label: "Instant Help", icon: Zap, summary: "Urgent request speed and fill rate metrics." },
      { href: "/admin/saved-jobs", label: "Saved Jobs", icon: Bookmark, summary: "Worker saved jobs activity." },
      { href: "/admin/ratings", label: "Ratings & Trust", icon: Star, summary: "Worker and employer review signals." },
    ]
  },
  {
    label: "Growth & Marketing",
    links: [
      { href: "/admin/referrals", label: "Referral Operations", icon: Gift, summary: "Referral rewards and withdrawals." },
      { href: "/admin/referral-stats", label: "Referral Stats", icon: BarChart3, summary: "Referral analytics and reward audits." },
      { href: "/admin/referral-codes", label: "Referral Codes", icon: Ticket, summary: "Code management and usage." },
      { href: "/admin/referral-config", label: "Referral Config", icon: Sliders, summary: "Set reward amounts and limits." },
      { href: "/admin/marketing", label: "Marketing Hub", icon: Rocket, summary: "Campaign assets, SEO & playbooks." }
    ]
  },
  {
    label: "Payments & Revenue",
    links: [
      { href: "/admin/payments", label: "Payments Panel", icon: CreditCard, summary: "Verify employer UTRs, manage QR codes & extend jobs." }
    ]
  },
  {
    label: "Platform & App Control",
    links: [
      { href: "/admin/app-update", label: "App Update", icon: Smartphone, summary: "Control Android OTA update prompts." },
      { href: "/admin/notifications", label: "Push Notifications", icon: Bell, summary: "Campaign sends and delivery logs." },
      { href: "/admin/announcements", label: "Announcements", icon: Megaphone, summary: "Broadcast messages in-app." },
      { href: "/admin/dynamic-config", label: "Dynamic Config", icon: Settings, summary: "Feature flags, launcher icons & configs." },
      { href: "/admin/feedback", label: "Feedback", icon: MessageSquare, summary: "In-app feedback (Azure)." },
      { href: "/admin/ai-logs", label: "DutyPe AI Logs", icon: Sparkles, summary: "Employer questions and AI replies (Azure)." },
      { href: "/admin/admin-activity", label: "Admin Activity", icon: History, summary: "Who changed what in the admin panel (Azure)." }
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
  const [searchQuery, setSearchQuery] = useState("");

  const filteredSections = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return adminSections;
    return adminSections
      .map((section) => ({
        ...section,
        links: section.links.filter(
          (link) =>
            link.label.toLowerCase().includes(q) ||
            link.summary.toLowerCase().includes(q) ||
            link.href.toLowerCase().includes(q)
        )
      }))
      .filter((section) => section.links.length > 0);
  }, [searchQuery]);

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
      {/* Mobile overlay backdrop */}
      {sidebarOpen && (
        <div
          className="admin-overlay"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside className={`admin-sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="admin-sidebar-header">
          <Link href="/admin" className="admin-brand">
            <span className="admin-brand-mark">
              <span className="admin-brand-initials">DP</span>
              <span className="admin-brand-pulse" />
            </span>
            <div className="admin-brand-text">
              <div className="admin-brand-title">
                <strong>DutyPe</strong>
                <span className="admin-env-pill">OPS</span>
              </div>
              <small>Operations Console</small>
            </div>
          </Link>
          <button
            className="admin-sidebar-close"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close sidebar"
          >
            <X size={18} />
          </button>
        </div>

        {/* Sleek in-sidebar quick filter */}
        <div className="admin-sidebar-search-box">
          <Search size={14} className="admin-sidebar-search-icon" />
          <input
            type="text"
            placeholder="Quick find..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="admin-sidebar-search-input"
            aria-label="Filter admin navigation"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery("")}
              className="admin-sidebar-search-clear"
              aria-label="Clear search"
            >
              <X size={12} />
            </button>
          )}
        </div>

        <nav className="admin-sidebar-nav" aria-label="Admin navigation">
          {filteredSections.map((section) => (
            <div key={section.label} className="admin-sidebar-section">
              <span className="admin-sidebar-section-label">{section.label}</span>
              <div className="admin-sidebar-section-links">
                {section.links.map((link) => {
                  const Icon = link.icon;
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
                      <span className="admin-sidebar-icon-wrap" aria-hidden="true">
                        <Icon size={15} strokeWidth={isActive ? 2.2 : 1.9} />
                      </span>
                      <span className="admin-sidebar-link-title">{link.label}</span>
                      {isActive && <span className="admin-sidebar-active-dot" />}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
          {filteredSections.length === 0 && (
            <div className="admin-sidebar-no-results">
              <small>No routes matching &ldquo;{searchQuery}&rdquo;</small>
            </div>
          )}
        </nav>

        <div className="admin-sidebar-footer">
          <div className="admin-sidebar-footer-info">
            <ShieldCheck size={14} className="admin-security-icon" />
            <span>Secure Admin Session</span>
          </div>

          <div className="admin-sidebar-footer-actions">
            <Link href="/" className="admin-footer-btn" target="_blank" rel="noopener noreferrer">
              <ExternalLink size={15} />
              <span>Public Site</span>
            </Link>

            <button
              type="button"
              className="admin-footer-btn danger"
              onClick={handleSidebarLogout}
              title="Sign out of admin session"
            >
              <LogOut size={15} />
              <span>Logout</span>
            </button>
          </div>
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
              <Menu size={20} />
            </button>
            <div className="admin-topbar-info">
              <div className="admin-topbar-breadcrumb">
                <span className="admin-topbar-kicker">DutyPe Control Center</span>
                <span className="admin-topbar-divider">/</span>
                <span className="admin-topbar-current">{activeLink?.label ?? title}</span>
              </div>
              <h1>{title}</h1>
              {topbarDescription && <p>{topbarDescription}</p>}
            </div>
          </div>
          <div className="admin-topbar-actions" aria-label="Admin shortcuts">
            <Link href="/admin/post-job" className="admin-topbar-action primary">
              <PlusCircle size={15} />
              <span>Post Job</span>
            </Link>
            <Link href="/admin/jobs" className="admin-topbar-action">
              <Briefcase size={15} />
              <span>Jobs</span>
            </Link>
            <Link href="/admin/users" className="admin-topbar-action">
              <Users size={15} />
              <span>Users</span>
            </Link>
            <Link href="/" className="admin-topbar-action ghost" target="_blank" rel="noopener noreferrer">
              <ExternalLink size={15} />
              <span>Public</span>
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
