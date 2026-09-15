"use client";

import Image from "next/image";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { signOut } from "firebase/auth";

import { SiteIcon } from "@/components/site-icon";
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
  { href: "/app/worker", label: "Home", icon: "layout-dashboard" },
  { href: "/app/worker/jobs", label: "All jobs", icon: "search" },
  { href: "/app/worker/map", label: "Map", icon: "map" },
  { href: "/app/worker/location", label: "Location", icon: "map-pin" },
  { href: "/app/worker/my-jobs", label: "My jobs", icon: "bookmark" },
  { href: "/app/worker/messages", label: "Messages", icon: "messages-square" },
  { href: "/app/worker/notifications", label: "Notifications", icon: "bell" },
  { href: "/app/worker/profile", label: "Profile", icon: "user-round" }
];

const employerLinks = [
  { href: "/app/employer", label: "Dashboard", icon: "layout-dashboard" },
  { href: "/app/employer/post-job", label: "Post job", icon: "plus" },
  { href: "/app/employer/locations", label: "Locations", icon: "map-pin" },
  { href: "/app/employer/jobs", label: "My jobs", icon: "briefcase-business" },
  { href: "/app/employer/applications", label: "Applications", icon: "clipboard-list" },
  { href: "/app/employer/messages", label: "Messages", icon: "messages-square" },
  { href: "/app/employer/notifications", label: "Notifications", icon: "bell" }
];

export function ProductRoleBoundary({
  children,
  currentPath,
  description,
  requiredRole,
  title
}: ProductRoleBoundaryProps) {
  const session = useProductSession();
  const { setActiveRole } = session;
  const roleAttempt = useRef<string | null>(null);
  const [roleFailure, setRoleFailure] = useState<{ key: string; message: string } | null>(null);
  const router = useRouter();
  const pathname = usePathname();
  const roleKey = `${session.user?.uid ?? "guest"}:${requiredRole}`;
  const syncingRole = Boolean(
    session.user && !session.loading && session.availableRoles.includes(requiredRole) &&
    session.currentRole !== requiredRole
  );
  const roleError = roleFailure?.key === roleKey ? roleFailure.message : null;

  useEffect(() => {
    if (!syncingRole) {
      roleAttempt.current = null;
      return;
    }

    if (roleAttempt.current === roleKey) {
      return;
    }

    roleAttempt.current = roleKey;
    setRoleFailure(null);
    void setActiveRole(requiredRole).catch(() => {
      if (roleAttempt.current === roleKey) {
        setRoleFailure({ key: roleKey, message: "We couldn't switch your account. Please try again." });
      }
    });
  }, [requiredRole, roleKey, syncingRole, roleError, setActiveRole]);

  if (session.loading || (syncingRole && !roleError)) {
    return (
      <div className="page-shell">
        <div className="page-ambient ambient-a" />
        <div className="page-ambient ambient-b" />
        <div className="page-wrap">
          <section className="section">
            <div className="empty-state" role="status">
              {syncingRole
                ? `Switching into ${productRoleLabel(requiredRole).toLowerCase()} mode.`
                : "Loading your account."}
            </div>
          </section>
        </div>
      </div>
    );
  }

  if (syncingRole && roleError) {
    return (
      <div className="page-shell">
        <main className="page-wrap">
          <section className="section">
            <h1>Unable to switch role</h1>
            <p role="alert">{roleError}</p>
            <div className="button-row">
              <button type="button" className="button" onClick={() => {
                roleAttempt.current = null;
                setRoleFailure(null);
              }}>
                Try again
              </button>
              <Link href="/app" className="button ghost">Back to your account</Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  if (session.error && !session.profile) {
    return (
      <div className="page-shell">
        <main className="page-wrap">
          <section className="section">
            <h1>Unable to load your account</h1>
            <p role="alert">{session.error}</p>
            <div className="button-row">
              <button type="button" className="button" onClick={() => void session.refreshProfile()}>Try again</button>
              <Link href="/app/auth" className="button ghost">Sign in</Link>
            </div>
          </section>
        </main>
      </div>
    );
  }

  if (!session.user) {
    return (
      <div className="page-shell auth-page">
        <main className="auth-gate">
          <section>
            <Link href="/" className="brand site-brand" aria-label="DutyPe home">
              <Image src="/dutype-logo.webp" alt="" width={40} height={40} className="brand-logo" />
              <strong>Duty<span>Pe</span></strong>
            </Link>
            <h1 className="headline">Sign in to continue</h1>
            <p className="lede">
              Sign in with your {productRoleLabel(requiredRole).toLowerCase()} account.
            </p>
            <div className="button-row">
              <Link href={`/app/auth?role=${requiredRole}&next=${encodeURIComponent(pathname || currentPath)}`} className="button">
                Sign in
              </Link>
              <Link href="/app" className="button ghost">
                Back
              </Link>
            </div>
          </section>
        </main>
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
              <span className="card-kicker">Account access</span>
              <h3>This account doesn&apos;t have {productRoleLabel(requiredRole).toLowerCase()} access.</h3>
              <p>
                Available roles: {session.availableRoles.map(productRoleLabel).join(", ") || "None"}.
              </p>
              <div className="button-row">
                <button type="button" className="button" onClick={() => router.push("/app")}>
                  Back to your account
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
    <div className="page-shell product-page">
      <a href="#workspace-content" className="skip-link">Skip to content</a>
      <div className="page-wrap product-shell">
        <aside className="product-side">
          <Link href="/app" className="brand site-brand" aria-label="DutyPe account">
            <Image src="/dutype-logo.webp" alt="" width={40} height={40} className="brand-logo" />
            <span>
              <strong>Duty<span>Pe</span></strong>
              <small>{productRoleLabel(role)} workspace</small>
            </span>
          </Link>

          <div className="product-identity">
            <span className="product-account-avatar"><SiteIcon name="user-round" /></span>
            <div>
              <strong>{userName}</strong>
              <small>{session.user?.email ?? productRoleLabel(role)}</small>
            </div>
          </div>

          <nav className="product-nav" aria-label={`${productRoleLabel(role)} navigation`}>
            {navLinks.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={currentPath === item.href ? "active" : undefined}
                aria-current={currentPath === item.href ? "page" : undefined}
              >
                <SiteIcon name={item.icon} />
                {item.label}
              </Link>
            ))}
          </nav>

          {rolePeers.length > 0 ? (
            <div className="product-switcher">
              <span className="card-kicker">Switch workspace</span>
              <div className="product-chip-row">
                {rolePeers.map((item) => (
                  <Link key={item} href={productRolePath(item)} className="product-chip">
                    <SiteIcon name={item === "EMPLOYER" ? "briefcase-business" : "user-round"} />
                    {productRoleLabel(item)}
                  </Link>
                ))}
              </div>
            </div>
          ) : null}

          <div className="product-side-actions">
            <Link href="/" className="button ghost" aria-label="Back to website" title="Back to website">
              <SiteIcon name="external-link" /><span>Back to website</span>
            </Link>
            <button type="button" className="button ghost" aria-label="Sign out" title="Sign out" onClick={() => void handleSignOut()}>
              <SiteIcon name="log-out" /><span>Sign out</span>
            </button>
          </div>
        </aside>

        <main className="product-main" id="workspace-content" tabIndex={-1}>
          <section className="product-header">
            <div className="product-header-top">
              <span className="section-label">{productRoleLabel(role)} workspace</span>
              <span className="product-header-note">{currentSection}</span>
            </div>
            <h1>{title}</h1>
            {description ? <p>{description}</p> : null}
          </section>

          {session.error ? <div className="callout" role="alert">Session error: {session.error}</div> : null}
          {children}
        </main>
      </div>
    </div>
  );
}
