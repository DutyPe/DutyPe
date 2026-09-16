"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { primaryNav } from "@/lib/public-site";

import { SiteIcon } from "./site-icon";

export function SiteNavigation() {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMenuOpen(false);
        menuButton.current?.focus();
      }
    }
    document.addEventListener("keydown", closeOnEscape);
    return () => document.removeEventListener("keydown", closeOnEscape);
  }, [menuOpen]);

  return (
    <header className="site-header" onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) setMenuOpen(false);
    }}>
      <div className="site-header-inner">
        <Link href="/" className="brand site-brand" aria-label="DutyPe home" onClick={() => setMenuOpen(false)}>
          <Image src="/dutype-logo.webp" alt="" width={40} height={40} className="brand-logo" priority />
          <strong>Duty<span>Pe</span></strong>
        </Link>
        <nav id="primary-navigation" className={`site-navigation${menuOpen ? " is-open" : ""}`} aria-label="Primary">
          {primaryNav.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              aria-current={pathname === link.href ? "page" : undefined}
              onClick={() => setMenuOpen(false)}
            >
              {link.label}
            </Link>
          ))}
          <Link href="/app/employer/post-job" className="mobile-hiring-link" onClick={() => setMenuOpen(false)}>
            Post a job <SiteIcon name="arrow-up-right" />
          </Link>
        </nav>
        <div className="site-header-actions">
          <Link href="/app/auth?role=EMPLOYER" className="header-signin">Sign in</Link>
          <Link href="/app/employer/post-job" className="button header-hiring">
            Post a job <SiteIcon name="plus" />
          </Link>
          <button
            ref={menuButton}
            className="icon-button menu-toggle"
            type="button"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            title={menuOpen ? "Close menu" : "Open menu"}
            aria-expanded={menuOpen}
            aria-controls="primary-navigation"
            onClick={() => setMenuOpen((current) => !current)}
          >
            <SiteIcon name={menuOpen ? "x" : "menu"} />
          </button>
        </div>
      </div>
    </header>
  );
}