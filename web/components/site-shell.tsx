import Link from "next/link";
import { ReactNode } from "react";

import { PLAY_STORE_URL, footerGroups, primaryNav, siteMeta } from "@/lib/public-site";

export function SiteShell({ children }: { children: ReactNode }) {
  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />
      <div className="page-ambient ambient-c" />

      <div className="page-wrap">
        <header className="topbar">
          <div className="topbar-main">
            <Link href="/" className="brand" aria-label="DutyPe home">
              <span className="brand-mark">DP</span>
              <span>
                <strong>{siteMeta.name}</strong>
                <small>{siteMeta.strapline}</small>
              </span>
            </Link>

            <Link href="/jobs" className="topbar-search" aria-label="Browse DutyPe jobs">
              <span>Search jobs</span>
              <strong>Role, city, category</strong>
            </Link>

            <div className="topbar-actions">
              <span className="topbar-note">Free for workers. Direct hiring for employers.</span>
              <Link href="/app/employer/post-job" className="button ghost topbar-button">
                Post Job
              </Link>
              <a
                href={PLAY_STORE_URL}
                className="button topbar-button"
                target="_blank"
                rel="noopener noreferrer"
              >
                Download App
              </a>
            </div>
          </div>

          <nav className="nav-links" aria-label="Primary">
            {primaryNav.map((link) => (
              <Link key={link.href} href={link.href} className="nav-link-pill">
                {link.label}
              </Link>
            ))}
          </nav>
        </header>

        <main className="site-main">{children}</main>

        <footer className="site-footer footer-grid">
          <div className="footer-top-row">
            <span className="brand-mark footer-mark">DP</span>
            <div className="footer-brand-copy">
              <p className="footer-title">DutyPe</p>
              <p>
                Find local jobs near you. Connect workers with employers instantly. No
                middlemen, no fees.
              </p>
              <div className="footer-meta-grid">
                <div className="footer-meta-card">
                  <span className="footer-meta-label">Registered company</span>
                  <strong>{siteMeta.companyName}</strong>
                </div>
                <div className="footer-meta-card">
                  <span className="footer-meta-label">Support</span>
                  <a href={`mailto:${siteMeta.supportEmail}`}>{siteMeta.supportEmail}</a>
                </div>
              </div>
            </div>
          </div>

          <div className="footer-group-grid">
            {footerGroups.map((group) => (
              <div key={group.title} className="footer-group">
              <p className="footer-title">{group.title}</p>
              <div className="footer-link-list">
                {group.links.map((link) => (
                  <Link key={link.href} href={link.href}>
                    {link.label}
                  </Link>
                ))}
              </div>
              </div>
            ))}
          </div>

          <div className="footer-bottom-line">
            <span>© 2026 DutyPe. Operated by {siteMeta.companyName}.</span>
            <span>Local hiring across India.</span>
          </div>
        </footer>
      </div>
    </div>
  );
}
