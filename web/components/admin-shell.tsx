import Link from "next/link";
import { ReactNode } from "react";

const adminLinks = [
  { href: "/admin", label: "Overview" },
  { href: "/admin/users", label: "Users" },
  { href: "/admin/jobs", label: "Jobs" },
  { href: "/admin/post-job", label: "Post job" },
  { href: "/admin/applications", label: "Applications" },
  { href: "/admin/referrals", label: "Referrals" },
  { href: "/admin/announcements", label: "Announcements" }
];

export function AdminShell({
  title,
  description,
  children
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />

      <div className="page-wrap admin-shell">
        <aside className="admin-side">
          <span className="eyebrow">Admin workspace</span>
          <h2>DutyPe operations</h2>
          <p>
            React migration target for the current Firebase-powered admin HTML pages.
          </p>

          <nav className="admin-nav" aria-label="Admin">
            {adminLinks.map((link) => (
              <Link key={link.href} href={link.href}>
                {link.label}
              </Link>
            ))}
          </nav>
        </aside>

        <div className="admin-main">
          <section className="admin-header">
            <span className="eyebrow">Operations layer</span>
            <h1>{title}</h1>
            <p>{description}</p>
          </section>

          {children}
        </div>
      </div>
    </div>
  );
}
