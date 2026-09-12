import Link from "next/link";
import Image from "next/image";
import { ReactNode } from "react";

import { AdSenseBanner } from "@/components/public/adsense-banner";
import { SmartAppBanner } from "@/components/public/smart-app-banner";
import { PLAY_STORE_URL, footerGroups, primaryNav, siteMeta } from "@/lib/public-site";

export function SiteShell({
  children,
  plain = false,
  hideTopBar = false,
  hideAds = false,
}: {
  children: ReactNode;
  plain?: boolean;
  hideTopBar?: boolean;
  hideAds?: boolean;
}) {
  const downloadUrl = `${PLAY_STORE_URL}&referrer=utm_source%3Dweb_topbar%26utm_medium%3Dorganic_web`;

  return (
    <div className={plain ? "page-shell page-shell-plain" : "page-shell"}>
      <div className="page-wrap">
        <header className={hideTopBar ? "topbar topbar-slim" : "topbar"}>
          {!hideTopBar ? (
          <div className="topbar-main">
            <Link href="/" className="brand" aria-label="DutyPe home">
              <Image
                src="/icon.webp"
                alt=""
                width={38}
                height={38}
                className="brand-mark brand-mark-logo"
                priority
              />
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
                href={downloadUrl}
                className="button topbar-button download-highlight-btn"
                target="_blank"
                rel="noopener noreferrer"
              >
                📱 Download App
              </a>
            </div>
          </div>
          ) : null}

          <nav className="nav-links" aria-label="Primary">
            {primaryNav.map((link) => (
              <Link key={link.href} href={link.href} className="nav-link-pill">
                {link.label}
              </Link>
            ))}
          </nav>
        </header>

        {!plain && !hideAds ? <AdSenseBanner /> : null}

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
              {!plain ? (
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
              ) : null}
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

      <SmartAppBanner />
    </div>
  );
}
