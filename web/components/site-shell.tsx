import Image from "next/image";
import Link from "next/link";
import { ReactNode } from "react";

import { footerGroups } from "@/lib/public-site";

import { SiteNavigation } from "./site-navigation";

export function SiteShell({ children }: { children: ReactNode }) {
  return (
    <div className="page-shell public-site">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <SiteNavigation />
      <div className="page-wrap">
        <main className="site-main" id="main-content" tabIndex={-1}>{children}</main>
      </div>
      <footer className="site-footer">
        <div className="footer-inner">
          <div className="footer-brand-copy">
            <Link href="/" className="brand site-brand" aria-label="DutyPe home">
              <Image src="/dutype-logo.webp" alt="" width={36} height={36} className="brand-logo" />
              <strong>Duty<span>Pe</span></strong>
            </Link>
            <p>Local work. Local people.<br />A little closer to home.</p>
            <span className="footer-origin">Made in India, for India.</span>
          </div>
          <div className="footer-group-grid">
            {footerGroups.map((group) => (
              <div key={group.title} className="footer-group">
                <h2 className="footer-title">{group.title}</h2>
                <div className="footer-link-list">
                  {group.links.map((link) => <Link key={link.href} href={link.href}>{link.label}</Link>)}
                </div>
              </div>
            ))}
          </div>
          <div className="footer-bottom-line">
            <span>&copy; {new Date().getFullYear()} DutyPe. All rights reserved.</span>
            <span>Good work starts nearby.</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
