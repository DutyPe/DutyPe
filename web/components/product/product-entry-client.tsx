"use client";

import Link from "next/link";

import {
  displayProfileName,
  productRoleLabel,
  productRolePath,
  PRODUCT_ROLES
} from "@/lib/product/profile";

import { useProductSession } from "./use-product-session";

const roleHighlights = {
  WORKER: [
    "Browse nearby jobs with saved and applied states.",
    "Track profile completion before you start applying.",
    "Keep job search, saves, and applications together."
  ],
  EMPLOYER: [
    "Post jobs with clear role, pay, and location details.",
    "Review workers and update application states live.",
    "Manage openings and hiring status from one workspace."
  ]
} as const;

export function ProductEntryClient() {
  const session = useProductSession();

  if (session.loading) {
    return (
      <div className="page-shell">
        <div className="page-ambient ambient-a" />
        <div className="page-ambient ambient-b" />
        <div className="page-ambient ambient-c" />
        <div className="page-wrap">
          <section className="section">
            <div className="empty-state">Loading DutyPe app entry.</div>
          </section>
        </div>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />
      <div className="page-ambient ambient-c" />

      <div className="page-wrap">
        <section className="hero product-hero">
          <div className="hero-grid product-hero-grid">
            <div className="hero-copy">
              <span className="eyebrow">DutyPe web app</span>
              <h1 className="headline">Open your worker or employer workspace.</h1>
              <p className="lede">
                Workers can browse, save, and apply to jobs. Employers can post openings,
                review applications, and keep hiring work organized.
              </p>

              <div className="button-row">
                {session.user ? (
                  <Link
                    href={productRolePath(session.currentRole ?? session.availableRoles[0] ?? "WORKER")}
                    className="button"
                  >
                    Continue as{" "}
                    {productRoleLabel(session.currentRole ?? session.availableRoles[0] ?? "WORKER")}
                  </Link>
                ) : (
                  <Link href="/app/auth" className="button">
                    Sign in or create account
                  </Link>
                )}
                <Link href="/" className="button ghost">
                  Back to public site
                </Link>
              </div>

              <div className="brand-stat-row">
                <div className="stat-card">
                  <strong>{session.user ? "Live" : "Ready"}</strong>
                  <span>Account-backed web workspace</span>
                </div>
                <div className="stat-card">
                  <strong>2 roles</strong>
                  <span>Worker and employer modes</span>
                </div>
                <div className="stat-card">
                  <strong>{session.user ? session.availableRoles.length : 1}</strong>
                  <span>Mode{session.user && session.availableRoles.length > 1 ? "s" : ""} available in this session</span>
                </div>
              </div>
            </div>

            <aside className="hero-panel">
              <span className="card-kicker">What this web app already does</span>
              <h3>What you can do here</h3>
              <ul className="detail-list">
                <li>
                  <strong>Worker path</strong>
                  <span>Browse jobs, save jobs, apply, and complete profile details.</span>
                </li>
                <li>
                  <strong>Employer path</strong>
                  <span>Post jobs, review applications, and manage hiring state.</span>
                </li>
                <li>
                  <strong>Role-aware entry</strong>
                  <span>Firebase auth sends each user into the correct product shell.</span>
                </li>
              </ul>
            </aside>
          </div>
        </section>

        {session.user ? (
          <section className="section">
            <div className="section-header">
              <div>
                <span className="tag">Current session</span>
                <h2>Pick a product mode</h2>
              </div>
              <p>
                Signed in as {displayProfileName(session.profile)}. The role cards below
                show the worker and employer modes available on this account.
              </p>
            </div>

            <div className="product-card-grid">
              {PRODUCT_ROLES.map((role) => {
                const enabled = session.availableRoles.includes(role);
                const highlights = roleHighlights[role];

                return (
                  <article key={role} className="product-card">
                    <span className="card-kicker">{enabled ? "Available" : "Not configured"}</span>
                    <h3>{productRoleLabel(role)} mode</h3>
                    <p>
                      {role === "WORKER"
                        ? "Browse jobs, save listings, apply, track your jobs, and manage your profile."
                        : "Post jobs, review applications, monitor your hiring pipeline, and manage employer identity."}
                    </p>
                    <ul className="helper-list">
                      {highlights.map((highlight) => (
                        <li key={highlight}>{highlight}</li>
                      ))}
                    </ul>
                    <div className="button-row">
                      {enabled ? (
                        <Link href={productRolePath(role)} className="button">
                          Open {productRoleLabel(role)}
                        </Link>
                      ) : (
                        <Link href="/app/auth" className="button ghost">
                          Use another account
                        </Link>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          </section>
        ) : (
          <section className="section">
            <div className="section-header">
              <div>
                <span className="tag">Role split</span>
                <h2>Enter as worker or employer</h2>
              </div>
              <p>
                Android already treats these as separate product modes. The web app will
                do the same instead of forcing one generic dashboard.
              </p>
            </div>

            <div className="product-card-grid">
              {PRODUCT_ROLES.map((role) => (
                <article key={role} className="product-card">
                  <span className="card-kicker">{productRoleLabel(role)}</span>
                  <h3>{role === "WORKER" ? "Find jobs fast" : "Hire locally fast"}</h3>
                  <p>
                    {role === "WORKER"
                      ? "All jobs, saved jobs, applications, profile completion, and location-aware job discovery."
                      : "Employer dashboard, post job, edit jobs, review applications, and hiring operations."}
                  </p>
                  <ul className="helper-list">
                    {roleHighlights[role].map((highlight) => (
                      <li key={highlight}>{highlight}</li>
                    ))}
                  </ul>
                  <div className="button-row">
                    <Link href={`/app/auth?role=${role}`} className="button">
                      Continue as {productRoleLabel(role)}
                    </Link>
                  </div>
                </article>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
