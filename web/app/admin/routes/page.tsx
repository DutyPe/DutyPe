import Link from "next/link";

import { AdminAuthGate } from "@/components/admin/admin-auth-gate";
import { AdminShell } from "@/components/admin-shell";

const adminRoutes = [
  { href: "/admin", label: "Dashboard", description: "Platform overview and recent activity." },
  { href: "/admin/login", label: "Admin Login", description: "Firebase-backed admin sign in screen." },
  { href: "/admin/routes", label: "Routes", description: "Full navigation map of admin routes." },
  { href: "/admin/users", label: "Users", description: "Browse and manage workers/employers." },
  { href: "/admin/worker-profiles", label: "Worker Profiles", description: "Inspect worker_profiles collection data." },
  { href: "/admin/employer-profiles", label: "Employer Profiles", description: "Inspect employer_profiles collection data." },
  { href: "/admin/jobs", label: "Jobs", description: "Review jobs and moderation controls." },
  { href: "/admin/post-job", label: "Post Job", description: "Create and publish a new job posting." },
  { href: "/admin/posters", label: "Posters", description: "Print posters for posted jobs, workers, and employers." },
  { href: "/admin/applications", label: "Applications", description: "Track applications and update status." },
  { href: "/admin/instant-help", label: "Instant Help", description: "Monitor urgent hiring requests and response speed." },
  { href: "/admin/saved-jobs", label: "Saved Jobs", description: "Review saved-job collection records." },
  { href: "/admin/ratings", label: "Ratings", description: "Inspect ratings and trust signals." },
  { href: "/admin/job-reports", label: "Job Reports", description: "Moderate reported jobs and abuse signals." },
  { href: "/admin/referrals", label: "Referrals", description: "Monitor referrals and payouts." },
  { href: "/admin/referral-config", label: "Referral Config", description: "Edit referral program configuration values." },
  { href: "/admin/referral-stats", label: "Referral Stats", description: "Inspect referral stats documents and rewards." },
  { href: "/admin/referral-codes", label: "Referral Codes", description: "Inspect referral code registry and ownership." },
  { href: "/admin/marketing", label: "Marketing", description: "Growth and marketing content browser (including /admin/marketing/... routes)." },
  { href: "/admin/app-update", label: "App Update", description: "Control the Android update prompt for worker and employer homes." },
  { href: "/admin/notifications", label: "Notifications", description: "Broadcast app alerts to all users or one role." },
  { href: "/admin/announcements", label: "Announcements", description: "Send notices and updates to users." },
  { href: "/admin/check-and-create-code", label: "Check/Create Code", description: "Referral utility tools." },
  { href: "/admin/create-test-referral", label: "Create Test Referral", description: "Generate test referral entries." },
  { href: "/admin/test-referral", label: "Test Referral", description: "Validate referral behavior and logs." }
];

export const metadata = {
  title: "Admin Routes",
  description: "Quick navigation map for all DutyPe admin routes."
};

export default function AdminRoutesPage() {

  return (
    <AdminShell title="Routes" description="Navigate every admin route from one place.">
      <AdminAuthGate>
        <section className="admin-section">
          <h2 className="admin-section-title">Admin Route Map</h2>
          <div className="admin-route-grid">
            {adminRoutes.map((route) => (
              <Link key={route.href} href={route.href} className="admin-route-card">
                <strong>{route.label}</strong>
                <span className="admin-route-path">{route.href}</span>
                <p>{route.description}</p>
              </Link>
            ))}
          </div>
        </section>
      </AdminAuthGate>
    </AdminShell>
  );
}
