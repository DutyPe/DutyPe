"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

import { SiteIcon } from "@/components/site-icon";
import {
  displayProfileName,
  productRoleLabel,
  productRolePath,
  PRODUCT_ROLES
} from "@/lib/product/profile";

import { useProductSession } from "./use-product-session";

export function ProductEntryClient() {
  const session = useProductSession();
  const [retrying, setRetrying] = useState(false);
  const [retryError, setRetryError] = useState(false);
  const availableRoles = PRODUCT_ROLES.filter((role) => session.availableRoles.includes(role));
  const currentRole = availableRoles.find((role) => role === session.currentRole) ?? availableRoles[0];

  async function retryProfile() {
    if (retrying) return;
    setRetrying(true);
    setRetryError(false);
    try {
      await session.refreshProfile();
    } catch {
      setRetryError(true);
    } finally {
      setRetrying(false);
    }
  }

  return (
    <div className="page-shell auth-page entry-page">
      <a href="#entry-content" className="skip-link">Skip to content</a>
      <header className="auth-topbar">
        <Link href="/" className="brand site-brand" aria-label="DutyPe home">
          <Image src="/dutype-logo.webp" alt="" width={40} height={40} className="brand-logo" priority />
          <strong>Duty<span>Pe</span></strong>
        </Link>
        <Link href="/jobs" className="text-link">Browse jobs<SiteIcon name="arrow-up-right" /></Link>
      </header>

      <main className="entry-main" id="entry-content" tabIndex={-1} aria-busy={session.loading || retrying}>
        <div className="entry-heading">
          <span className="section-label">YOUR DUTYPE ACCOUNT</span>
          <h1>{session.loading ? "Your DutyPe account" : session.user ? "Choose your workspace" : "What brings you to DutyPe?"}</h1>
          {!session.loading ? (
            <p>{session.user ? <>Welcome back, <span>{displayProfileName(session.profile)}</span>.</> : "Your next opportunity. Or your next great hire."}</p>
          ) : null}
        </div>

        {session.loading ? (
          <div className="entry-status" role="status"><SiteIcon name="clock-3" /><p>Loading your account...</p></div>
        ) : session.error || retryError ? (
          <section className="entry-status entry-error" role="alert">
            <SiteIcon name="shield-check" />
            <h2>Unable to load your account</h2>
            <p>Your account details are unavailable right now.</p>
            <div className="entry-status-actions">
              {session.user ? <button type="button" className="button" disabled={retrying} onClick={() => void retryProfile()}>{retrying ? "Trying again..." : "Try again"}</button> : <Link href="/app/auth" className="button">Sign in</Link>}
              <Link href="/contact" className="text-link">Contact support<SiteIcon name="arrow-up-right" /></Link>
            </div>
          </section>
        ) : session.user && availableRoles.length === 0 ? (
          <section className="entry-status">
            <SiteIcon name="user-round" />
            <h2>No workspace is available</h2>
            <p>This account doesn&apos;t have worker or employer access.</p>
            <Link href="/contact" className="text-link">Contact support<SiteIcon name="arrow-up-right" /></Link>
          </section>
        ) : (
          <section className="entry-role-grid" aria-label="Choose a role">
            {PRODUCT_ROLES.map((role) => {
              const enabled = availableRoles.includes(role);
              const isCurrent = Boolean(session.user && currentRole === role);
              const label = productRoleLabel(role);
              const requiresAnotherAccount = Boolean(session.user && !enabled);

              return (
                <article key={role} className={`entry-role${isCurrent ? " is-current" : ""}`} aria-labelledby={`entry-role-${role.toLowerCase()}`}>
                  <div className="entry-role-top">
                    <span className={`category-icon tone-${role === "WORKER" ? "mint" : "blue"}`}><SiteIcon name={role === "WORKER" ? "user-round" : "briefcase-business"} /></span>
                    {isCurrent ? <span className="entry-current"><SiteIcon name="check" />Current workspace</span> : <span className="section-label">{label}</span>}
                  </div>
                  <h2 id={`entry-role-${role.toLowerCase()}`}>{role === "WORKER" ? "Find local work" : "Hire local people"}</h2>
                  <p>{requiresAnotherAccount ? "A different account is needed for this role." : role === "WORKER" ? "A fresh start, closer to home." : "Good people for your home or business."}</p>
                  <Link
                    href={session.user && enabled ? productRolePath(role) : `/app/auth?role=${role}`}
                    className={`button${session.user && !isCurrent ? " ghost" : ""}`}
                  >
                    {requiresAnotherAccount ? `Sign in as ${label}` : !session.user || isCurrent ? `Continue as ${label}` : `Open ${label}`}
                    <SiteIcon name="arrow-right" />
                  </Link>
                </article>
              );
            })}
          </section>
        )}

        {!session.loading ? (
          <div className="entry-account-actions">
            <span>{session.user ? "Not your account?" : "Already part of DutyPe?"}</span>
            <Link href="/app/auth" className="text-link">{session.user ? "Use another account" : "Sign in"}<SiteIcon name="arrow-up-right" /></Link>
          </div>
        ) : null}

        <div className="entry-bottom">
          <Link href="/safety" className="text-link"><SiteIcon name="shield-check" />Your safety comes first</Link>
          <nav aria-label="Account policies"><Link href="/privacy">Privacy</Link><Link href="/terms">Terms</Link></nav>
        </div>
      </main>
    </div>
  );
}
