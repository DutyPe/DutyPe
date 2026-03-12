import Link from "next/link";
import { ReactNode } from "react";

import { footerGroups, primaryNav, siteMeta } from "@/lib/public-site";

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

            <div className="topbar-actions">
              <span className="topbar-note">No middlemen, no fees</span>
              <Link href="/app" className="button topbar-button">
                Open app
              </Link>
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
          <div className="footer-brand">
            <div className="logo-orb">DutyPe</div>
            <p className="footer-title">DutyPe</p>
            <p>
              Find local jobs near you. Connect workers with employers instantly. No
              middlemen, no fees.
            </p>
          </div>

          {footerGroups.map((group) => (
            <div key={group.title}>
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

          <div className="footer-bottom-line">
            <span>Built for hyperlocal hiring in India.</span>
            <span>React website, app-first product flow.</span>
          </div>
        </footer>
      </div>
    </div>
  );
}
