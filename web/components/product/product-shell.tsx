"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ReactNode, useEffect, useMemo, useState } from "react";
import { signOut } from "firebase/auth";

import {
  displayProfileName,
  productRoleLabel,
  productRolePath,
  type ProductRole
} from "@/lib/product/profile";
import { getFirebaseServices } from "@/lib/firebase/client";

import { type ProductSession, useProductSession } from "./use-product-session";

type ProductRoleBoundaryProps = {
  children: (session: ProductSession) => ReactNode;
  currentPath: string;
  description: string;
  requiredRole: ProductRole;
  title: string;
};

type ProductAppShellProps = {
  children: ReactNode;
  currentPath: string;
  description: string;
  role: ProductRole;
  session: ProductSession;
  title: string;
};

const workerLinks = [
  { href: "/app/worker", label: "Home" },
  { href: "/app/worker/jobs", label: "All jobs" },
  { href: "/app/worker/map", label: "Map" },
  { href: "/app/worker/location", label: "Location" },
  { href: "/app/worker/my-jobs", label: "My jobs" },
  { href: "/app/worker/messages", label: "Messages" },
  { href: "/app/worker/notifications", label: "Notifications" },
  { href: "/app/worker/profile", label: "Profile" }
];

const employerLinks = [
  { href: "/app/employer", label: "Dashboard" },
  { href: "/app/employer/post-job", label: "Post job" },
  { href: "/app/employer/locations", label: "Locations" },
  { href: "/app/employer/jobs", label: "My jobs" },
  { href: "/app/employer/applications", label: "Applications" },
  { href: "/app/employer/messages", label: "Messages" },
  { href: "/app/employer/notifications", label: "Notifications" }
];

export function ProductRoleBoundary({
  children,
  currentPath,
  description,
  requiredRole,
  title
}: ProductRoleBoundaryProps) {
  const session = useProductSession();
  const [syncingRole, setSyncingRole] = useState(false);
  const router = useRouter();

  useEffect(() => {
    if (!session.user || session.loading) {
      return;
    }

    if (!session.availableRoles.includes(requiredRole)) {
      return;
    }

    if (session.currentRole === requiredRole) {
      return;
    }

    setSyncingRole(true);
    void session
      .setActiveRole(requiredRole)
      .catch(() => undefined)
      .finally(() => setSyncingRole(false));
  }, [requiredRole, session]);

  if (session.loading || syncingRole) {
    return (
      <div className="page-shell">
        <div className="page-ambient ambient-a" />
        <div className="page-ambient ambient-b" />
        <div className="page-wrap">
          <section className="section">
            <div className="empty-state">
              {syncingRole
                ? `Switching into ${productRoleLabel(requiredRole).toLowerCase()} mode.`
                : "Loading DutyPe product session."}
            </div>
          </section>
        </div>
      </div>
    );
  }

  if (!session.user) {
    return (
      <div className="page-shell">
        <div className="page-ambient ambient-a" />
        <div className="page-ambient ambient-b" />
        <div className="page-wrap">
          <section className="hero product-hero">
            <span className="eyebrow">Product sign-in required</span>
            <h1 className="headline">Open the app routes with a Firebase product session.</h1>
            <p className="lede">
              Android parity routes are gated because apply, save, post-job, and profile
              flows need a real authenticated user.
            </p>
            <div className="button-row">
              <Link href={`/app/auth?role=${requiredRole}`} className="button">
                Sign in
              </Link>
              <Link href="/app" className="button ghost">
                Back to app entry
              </Link>
            </div>
          </section>
        </div>
      </div>
    );
  }

  if (!session.availableRoles.includes(requiredRole)) {
    return (
      <div className="page-shell">
        <div className="page-ambient ambient-a" />
        <div className="page-ambient ambient-b" />
        <div className="page-wrap">
          <section className="section">
            <div className="detail-panel">
              <span className="card-kicker">Role mismatch</span>
              <h3>Your current Firebase user is not configured for {productRoleLabel(requiredRole)} mode.</h3>
              <p>
                The web product shell follows the Android role split. This account only
                has access to: {session.availableRoles.map(productRoleLabel).join(", ") || "no roles"}.
              </p>
              <div className="button-row">
                <button type="button" className="button" onClick={() => router.push("/app")}>
                  Go to app entry
                </button>
                <Link href="/app/auth" className="button ghost">
                  Use another account
                </Link>
              </div>
            </div>
          </section>
        </div>
      </div>
    );
  }

  return (
    <ProductAppShell
      currentPath={currentPath}
      description={description}
      role={requiredRole}
      session={session}
      title={title}
    >
      {children(session)}
    </ProductAppShell>
  );
}

export function ProductAppShell({
  children,
  currentPath,
  description,
  role,
  session,
  title
}: ProductAppShellProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const navLinks = role === "WORKER" ? workerLinks : employerLinks;
  const rolePeers = session.availableRoles.filter((item) => item !== role);
  const userName = displayProfileName(session.profile);
  const currentSection =
    navLinks.find((item) => item.href === currentPath)?.label ?? productRoleLabel(role);

  async function handleSignOut() {
    if (!services) {
      return;
    }

    await signOut(services.auth);
    router.push("/app");
    router.refresh();
  }

  return (
    <div className="page-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />
      <div className="page-ambient ambient-c" />

      <div className="page-wrap product-shell">
        <aside className="product-side">
          <Link href="/app" className="brand">
            <span className="brand-mark">D</span>
            <span>
              <strong>DutyPe App</strong>
              <small>{productRoleLabel(role)} mode</small>
            </span>
          </Link>

          <div className="product-identity">
            <span className="card-kicker">Current account</span>
            <h3>{userName}</h3>
            <p>{session.user?.email ?? "Firebase user"}</p>
            <div className="pill-row product-badge-row">
              <span className="pill">{productRoleLabel(role)} mode</span>
              <span className="pill">{session.availableRoles.length} role access</span>
              <span className="pill">Live Firebase session</span>
            </div>
          </div>

          <nav className="product-nav" aria-label={`${productRoleLabel(role)} routes`}>
            {navLinks.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={currentPath === item.href ? "active" : undefined}
              >
                {item.label}
              </Link>
            ))}
          </nav>

          {rolePeers.length > 0 ? (
            <div className="product-switcher">
              <span className="card-kicker">Switch role</span>
              <div className="product-chip-row">
                {rolePeers.map((item) => (
                  <Link key={item} href={productRolePath(item)} className="product-chip">
                    {productRoleLabel(item)}
                  </Link>
                ))}
              </div>
            </div>
          ) : null}

          <div className="product-side-note">
            <span className="card-kicker">Current route</span>
            <h3>{currentSection}</h3>
            <p>
              This web shell mirrors the Android role split while keeping public website
              and product actions clearly separated.
            </p>
          </div>

          <div className="product-side-actions">
            <Link href="/" className="button ghost">
              Public website
            </Link>
            <button type="button" className="button ghost" onClick={() => void handleSignOut()}>
              Sign out
            </button>
          </div>
        </aside>

        <div className="product-main">
          <section className="product-header">
            <div className="product-header-top">
              <span className="eyebrow">Android parity slice</span>
              <span className="product-header-note">App-backed web flow</span>
            </div>
            <h1>{title}</h1>
            <p>{description}</p>

            <div className="product-summary-grid">
              <div className="product-summary-card">
                <span>Mode</span>
                <strong>{productRoleLabel(role)}</strong>
              </div>
              <div className="product-summary-card">
                <span>Current section</span>
                <strong>{currentSection}</strong>
              </div>
              <div className="product-summary-card">
                <span>Account state</span>
                <strong>{session.user ? "Authenticated" : "Guest"}</strong>
              </div>
            </div>
          </section>

          {session.error ? <div className="callout">Session error: {session.error}</div> : null}
          {children}
        </div>
      </div>
    </div>
  );
}
