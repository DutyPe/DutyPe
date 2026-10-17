"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";
import {
  Compass,
  Search,
  LayoutDashboard,
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
  Star,
  Gift,
  Sliders,
  BarChart3,
  Ticket,
  Rocket,
  Smartphone,
  Bell,
  Megaphone,
  Settings,
  MessageCircle,
  Bot,
  History,
  Wrench,
  Sparkles,
  CreditCard,
  BookOpen,
  DollarSign,
  Copy,
  Check,
  ArrowRight,
  ExternalLink,
  LucideIcon
} from "lucide-react";

type RouteCategory =
  | "OVERVIEW"
  | "PEOPLE"
  | "HIRING"
  | "GROWTH"
  | "SERVICES"
  | "SYSTEM";

interface AdminRouteItem {
  href: string;
  label: string;
  description: string;
  category: RouteCategory;
  icon: LucideIcon;
  badge?: string;
}

const ADMIN_ROUTES: AdminRouteItem[] = [
  // Overview & Strategy
  { href: "/admin", label: "Dashboard", description: "Platform live health, key metrics and recent activity.", category: "OVERVIEW", icon: LayoutDashboard },
  { href: "/admin/routes", label: "Routes & Tools", description: "Comprehensive directory and quick navigation map of every admin route.", category: "OVERVIEW", icon: Compass },
  { href: "/admin/founder-playbook", label: "Founder Playbook", description: "Khammam GTM strategy, business checklist, unit economics & founder daily rhythm.", category: "OVERVIEW", icon: BookOpen, badge: "Strategy" },

  // People & Directory
  { href: "/admin/users", label: "Users Directory", description: "Browse and manage accounts, roles, verification status and referral codes.", category: "PEOPLE", icon: Users },
  { href: "/admin/whatsapp-groups", label: "WhatsApp Broadcasts", description: "Filter and export Worker & Employer phone numbers for emergency broadcast groups.", category: "PEOPLE", icon: MessageSquare },
  { href: "/admin/whatsapp-offers", label: "WhatsApp Offers & Promos", description: "Send bulk WhatsApp promotional messages to opted-in users.", category: "PEOPLE", icon: MessageCircle, badge: "Marketing" },
  { href: "/admin/phone-roles", label: "Phone Roles", description: "Inspect phoneRoles collection data exactly as stored in Firestore.", category: "PEOPLE", icon: PhoneCall },
  { href: "/admin/delete-user-by-phone", label: "Delete User by Phone", description: "Emergency account data cleanup for wrong-role registrations.", category: "PEOPLE", icon: UserX },
  { href: "/admin/worker-profiles", label: "Worker Profiles", description: "Inspect worker_profiles documents with skills, coordinates and KYC status.", category: "PEOPLE", icon: HardHat },
  { href: "/admin/employer-profiles", label: "Employer Profiles", description: "Inspect employer_profiles collection data and company accounts.", category: "PEOPLE", icon: Building2 },

  // Hiring & Moderation
  { href: "/admin/jobs", label: "Jobs Moderation", description: "Review live job listings, urgent hiring posts, and moderation controls.", category: "HIRING", icon: Briefcase },
  { href: "/admin/post-job", label: "Post a Job", description: "Create and publish official verified job listings on behalf of employers.", category: "HIRING", icon: PlusCircle },
  { href: "/admin/posters", label: "Marketing Kit & Posters", description: "Printable PDF job posters, Instagram story cards, and WhatsApp templates.", category: "HIRING", icon: Printer, badge: "Print Ready" },
  { href: "/admin/applications", label: "Applications", description: "Track worker job applications and review hiring statuses.", category: "HIRING", icon: FileText },
  { href: "/admin/instant-help", label: "Instant Help Monitor", description: "Monitor urgent hyper-local requests and worker dispatch speed.", category: "HIRING", icon: Zap },
  { href: "/admin/ratings", label: "Ratings & Trust", description: "Inspect employer-worker ratings and trust reputation signals.", category: "HIRING", icon: Star },

  // Growth & Marketing
  { href: "/admin/expenses", label: "Marketing Expenses & ROI", description: "Track offline marketing expenses, print runs, and unit CAC against real revenue.", category: "GROWTH", icon: DollarSign, badge: "ROI" },
  { href: "/admin/referrals", label: "Referrals Operations", description: "Monitor worker/employer referral rewards, claims and withdrawal payouts.", category: "GROWTH", icon: Gift },
  { href: "/admin/referral-stats", label: "Referral Stats", description: "Audit referral rewards, leaderboards, and top community ambassadors.", category: "GROWTH", icon: BarChart3 },
  { href: "/admin/referral-codes", label: "Referral Codes Registry", description: "Inspect referral code registry and code ownership across users.", category: "GROWTH", icon: Ticket },
  { href: "/admin/referral-config", label: "Referral Config", description: "Edit referral program payout amounts, minimum thresholds, and cooldowns.", category: "GROWTH", icon: Sliders },
  { href: "/admin/promotional-banners", label: "Promotional Banners & Showcases", description: "Manage carousel banners across customer, worker, and employer apps.", category: "GROWTH", icon: Sparkles },
  { href: "/admin/marketing", label: "Marketing Assets Hub", description: "Growth and marketing content browser, brochures, and investor one-pagers.", category: "GROWTH", icon: Rocket },
  { href: "/admin/field-leads", label: "Field Registrations & Desks", description: "Track offline umbrella-desk registrations, leads, and agent payouts.", category: "GROWTH", icon: Users },

  // Services
  { href: "/admin/services", label: "Home Services Dispatch", description: "Approve service partners, verify credit top-ups, bookings, and assign partners.", category: "SERVICES", icon: Wrench, badge: "Dispatch" },
  { href: "/admin/service-catalog", label: "3D Icons & Service Catalog", description: "Upload 3D clay renders, set Khammam visiting prices, and edit catalog.", category: "SERVICES", icon: Sparkles },
  { href: "/admin/payments", label: "Subscription Payments", description: "Verify employer UTRs, manage active QR codes, and extend job postings.", category: "SERVICES", icon: CreditCard },

  // System & Logs
  { href: "/admin/announcements", label: "In-App Announcements", description: "Broadcast live notices and update cards to users inside the mobile app.", category: "SYSTEM", icon: Megaphone },
  { href: "/admin/notifications", label: "Push Notifications", description: "Broadcast native mobile push notifications to all users or specific roles.", category: "SYSTEM", icon: Bell },
  { href: "/admin/app-update", label: "App Update Controls", description: "Trigger force or flexible update prompts for Android users remotely.", category: "SYSTEM", icon: Smartphone },
  { href: "/admin/dynamic-config", label: "Dynamic App Config", description: "Manage festival app icons, primary color themes, and remote feature flags.", category: "SYSTEM", icon: Settings },
  { href: "/admin/feedback", label: "User Feedback", description: "Inspect in-app feedback submitted by workers and employers.", category: "SYSTEM", icon: MessageSquare },
  { href: "/admin/ai-logs", label: "DutyPe AI Logs", description: "Employer questions asked to DutyPe AI assistant and AI reply transcripts.", category: "SYSTEM", icon: Bot },
  { href: "/admin/admin-activity", label: "Admin Activity Audit", description: "Tamper-evident log of every admin modification with timestamps.", category: "SYSTEM", icon: History }
];

const CATEGORY_TABS: Array<{ id: string; label: string }> = [
  { id: "ALL", label: "All Routes" },
  { id: "OVERVIEW", label: "Overview & Strategy" },
  { id: "PEOPLE", label: "People & Directory" },
  { id: "HIRING", label: "Hiring & Jobs" },
  { id: "GROWTH", label: "Growth & Marketing" },
  { id: "SERVICES", label: "Home Services" },
  { id: "SYSTEM", label: "System & Logs" }
];

export default function AdminRoutesPage() {
  const [search, setSearch] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [copiedPath, setCopiedPath] = useState<string | null>(null);

  const filteredRoutes = useMemo(() => {
    const q = search.trim().toLowerCase();
    return ADMIN_ROUTES.filter((route) => {
      const matchesCategory =
        selectedCategory === "ALL" || route.category === selectedCategory;
      const matchesSearch =
        !q ||
        route.label.toLowerCase().includes(q) ||
        route.href.toLowerCase().includes(q) ||
        route.description.toLowerCase().includes(q);
      return matchesCategory && matchesSearch;
    });
  }, [search, selectedCategory]);

  const handleCopy = (path: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    navigator.clipboard.writeText(window.location.origin + path);
    setCopiedPath(path);
    setTimeout(() => setCopiedPath(null), 2000);
  };

  return (
    <AdminShell
      title="Routes & Command Map"
      description="Interactive directory of all 35+ admin workflows, management screens, and diagnostic consoles."
    >
      <AdminAuthGate>
        <div className="adm-stack">
          {/* Top KPI Metrics */}
          <div className="adm-metrics-grid">
            <div className="adm-metric-card">
              <div className="adm-metric-top">
                <span className="adm-metric-label">Total Admin Consoles</span>
                <div className="adm-metric-icon">
                  <Compass style={{ width: 18, height: 18 }} />
                </div>
              </div>
              <div className="adm-metric-value">{ADMIN_ROUTES.length}</div>
              <div className="adm-metric-sub">
                <span>production routes indexed</span>
              </div>
            </div>

            <div className="adm-metric-card">
              <div className="adm-metric-top">
                <span className="adm-metric-label">Core Operations</span>
                <div className="adm-metric-icon" style={{ background: "#ecfdf5", color: "#10b981" }}>
                  <Briefcase style={{ width: 18, height: 18 }} />
                </div>
              </div>
              <div className="adm-metric-value">
                {ADMIN_ROUTES.filter((r) => r.category === "HIRING" || r.category === "SERVICES").length}
              </div>
              <div className="adm-metric-sub">
                <span>jobs, services & moderation</span>
              </div>
            </div>

            <div className="adm-metric-card">
              <div className="adm-metric-top">
                <span className="adm-metric-label">Growth & Marketing</span>
                <div className="adm-metric-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
                  <Rocket style={{ width: 18, height: 18 }} />
                </div>
              </div>
              <div className="adm-metric-value">
                {ADMIN_ROUTES.filter((r) => r.category === "GROWTH").length}
              </div>
              <div className="adm-metric-sub">
                <span>expenses, banners & referrals</span>
              </div>
            </div>

            <div className="adm-metric-card">
              <div className="adm-metric-top">
                <span className="adm-metric-label">System & AI</span>
                <div className="adm-metric-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
                  <Bot style={{ width: 18, height: 18 }} />
                </div>
              </div>
              <div className="adm-metric-value">
                {ADMIN_ROUTES.filter((r) => r.category === "SYSTEM").length}
              </div>
              <div className="adm-metric-sub">
                <span>push, logs, updates & telemetry</span>
              </div>
            </div>
          </div>

          {/* Search & Filter Toolbar */}
          <div className="adm-toolbar">
            <div className="adm-search-wrap">
              <Search className="adm-search-icon" />
              <input
                type="text"
                className="adm-input-search"
                placeholder="Search admin consoles by title, URL or purpose (e.g. expenses, users, referrals)..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <span style={{ fontSize: "0.825rem", color: "var(--adm-text-muted)", fontWeight: 500 }}>
              <strong>{filteredRoutes.length}</strong> of {ADMIN_ROUTES.length} routes
            </span>
          </div>

          {/* Category Tabs */}
          <div style={{ display: "flex", gap: 8, overflowX: "auto", paddingBottom: 4 }}>
            {CATEGORY_TABS.map((tab) => {
              const active = selectedCategory === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setSelectedCategory(tab.id)}
                  className={`adm-btn adm-btn-sm ${active ? "adm-btn-primary" : "adm-btn-secondary"}`}
                  style={{ borderRadius: 9999, padding: "7px 16px" }}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* Routes Grid */}
          {filteredRoutes.length === 0 ? (
            <div className="adm-card" style={{ padding: 48, textAlign: "center", display: "flex", flexDirection: "column", alignItems: "center", gap: 12 }}>
              <Compass style={{ width: 44, height: 44, color: "var(--adm-text-subtle)" }} />
              <h3 style={{ margin: 0, fontSize: "1.1rem", color: "var(--adm-text-main)" }}>No matching routes found</h3>
              <p style={{ margin: 0, fontSize: "0.875rem", color: "var(--adm-text-muted)" }}>
                No admin console matched your search query &quot;{search}&quot;.
              </p>
              <button
                type="button"
                className="adm-btn adm-btn-secondary adm-btn-sm"
                onClick={() => { setSearch(""); setSelectedCategory("ALL"); }}
              >
                Reset Search
              </button>
            </div>
          ) : (
            <div className="adm-routes-grid">
              {filteredRoutes.map((route) => {
                const IconComponent = route.icon;
                const isCopied = copiedPath === route.href;
                return (
                  <Link
                    key={route.href}
                    href={route.href}
                    prefetch={false}
                    className="adm-route-card"
                  >
                    <div className="adm-route-top">
                      <div className="adm-route-icon">
                        <IconComponent style={{ width: 18, height: 18 }} />
                      </div>
                      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                        {route.badge && (
                          <span className="adm-badge adm-badge-emerald" style={{ fontSize: "0.68rem" }}>
                            {route.badge}
                          </span>
                        )}
                        <button
                          type="button"
                          className="adm-btn-icon"
                          style={{ width: 26, height: 26 }}
                          title="Copy Full Link"
                          onClick={(e) => handleCopy(route.href, e)}
                        >
                          {isCopied ? (
                            <Check style={{ width: 12, height: 12, color: "#10b981" }} />
                          ) : (
                            <Copy style={{ width: 12, height: 12 }} />
                          )}
                        </button>
                      </div>
                    </div>

                    <h3 className="adm-route-title">{route.label}</h3>
                    <span className="adm-route-path">{route.href}</span>
                    <p className="adm-route-desc">{route.description}</p>

                    <div style={{ marginTop: "auto", paddingTop: 8, display: "flex", alignItems: "center", gap: 4, fontSize: "0.775rem", fontWeight: 600, color: "var(--adm-primary)" }}>
                      <span>Open console</span>
                      <ArrowRight style={{ width: 13, height: 13 }} />
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </AdminAuthGate>
    </AdminShell>
  );
}
