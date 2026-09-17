import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { transformSync } from "esbuild";
import { createElement, Fragment, isValidElement, type ComponentProps, type ReactElement, type ReactNode } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import type { ProductSession } from "../components/product/use-product-session";
import type { ProductUserProfile } from "../lib/product/profile";

const testRunner = process.env.VITEST ? "vitest" : "node:test";
const { describe, it } = await import(testRunner) as Pick<typeof import("node:test"), "describe" | "it">;
const require = createRequire(import.meta.url);

function loadModule<Module>(relativePath: string, mocks: Record<string, unknown>, globals: Record<string, unknown> = {}): Module {
  const filename = fileURLToPath(new URL(relativePath, import.meta.url));
  const { code } = transformSync(readFileSync(filename, "utf8"), {
    format: "cjs",
    jsx: "automatic",
    loader: filename.endsWith(".tsx") ? "tsx" : "ts",
    target: "node22"
  });
  const loaded = { exports: {} };
  const loadDependency = (specifier: string) => Object.hasOwn(mocks, specifier)
    ? mocks[specifier]
    : require(specifier);
  new Function("require", "module", "exports", ...Object.keys(globals), code)(loadDependency, loaded, loaded.exports, ...Object.values(globals));
  return loaded.exports as Module;
}

function SharedProvider({ children }: { children: ReactNode }) {
  return createElement(Fragment, null, children);
}

function controlledHooks() {
  type Slot = { value?: unknown; dependencies?: unknown[]; cleanup?: () => void };
  const slots: Slot[] = [];
  const effects: (() => void)[] = [];
  let cursor = 0;
  function nextSlot() {
    const index = cursor++;
    return slots[index] ??= {};
  }
  function changed(slot: Slot, dependencies?: unknown[]) {
    return !dependencies || !slot.dependencies || dependencies.some((value, index) => value !== slot.dependencies?.[index]);
  }
  function useMemo(factory: () => unknown, dependencies?: unknown[]) {
    const slot = nextSlot();
    if (changed(slot, dependencies)) {
      slot.value = factory();
      slot.dependencies = dependencies;
    }
    return slot.value;
  }
  return {
    react: {
      ...require("react"),
      useState(initial: unknown) {
        const slot = nextSlot();
        if (!("value" in slot)) slot.value = typeof initial === "function" ? initial() : initial;
        return [slot.value, (next: unknown) => { slot.value = typeof next === "function" ? next(slot.value) : next; }];
      },
      useRef: (initial: unknown) => useMemo(() => ({ current: initial }), []),
      useMemo,
      useCallback: (callback: unknown, dependencies?: unknown[]) => useMemo(() => callback, dependencies),
      useEffect(effect: () => (() => void) | void, dependencies?: unknown[]) {
        const slot = nextSlot();
        if (changed(slot, dependencies)) {
          slot.dependencies = dependencies;
          effects.push(() => {
            slot.cleanup?.();
            slot.cleanup = effect() ?? undefined;
          });
        }
      }
    },
    render<Result>(component: () => Result): Result {
      cursor = 0;
      return component();
    },
    flushEffects() {
      for (const effect of effects.splice(0)) effect();
    },
    dispose() {
      for (const slot of slots) slot.cleanup?.();
    }
  };
}

const productProfile = loadModule<typeof import("../lib/product/profile")>("../lib/product/profile.ts", {});

function sessionFixture(setupFailure: Error | null = null) {
  const hooks = controlledHooks();
  const account = { uid: "signed-in-user", email: "user@example.test", displayName: "Test user" };
  const services = { auth: { currentUser: account as typeof account | null }, db: {} };
  const reads: string[][] = [];
  let profile: ProductUserProfile | null = { role: "EMPLOYER", roles: ["EMPLOYER"] };
  let failure: Error | null = null;
  let authListener: (user: typeof account | null) => void = () => {};
  let subscriptions = 0;
  const { ProductSessionProvider } = loadModule<{ ProductSessionProvider: (props: { children: ReactNode }) => ReactElement }>("../components/product/use-product-session.ts", {
    react: hooks.react,
    "firebase/auth": {
      onAuthStateChanged: (_auth: unknown, listener: typeof authListener) => {
        ++subscriptions;
        authListener = listener;
        return () => {};
      }
    },
    "firebase/firestore": {
      doc: (_db: unknown, ...segments: string[]) => segments,
      getDoc: async (segments: string[]) => {
        reads.push(segments);
        if (failure) throw failure;
        return { exists: () => profile !== null, data: () => profile, id: "untrusted-document-id" };
      }
    },
    "@/lib/firebase/client": { getFirebaseServices: () => {
      if (setupFailure) throw setupFailure;
      return services;
    } },
    "@/lib/firebase/auth-errors": { FIREBASE_SETUP_ERROR: "Configuration unavailable", firebaseAuthErrorMessage: (error: Error) => error.message },
    "@/lib/product/profile": productProfile
  }, { window: {} });
  const session = () => hooks.render(() => ProductSessionProvider({ children: null }).props.value as ProductSession);
  session();
  hooks.flushEffects();
  return {
    account,
    reads,
    session,
    hooks,
    subscriptions: () => subscriptions,
    emitAuth(user: typeof account | null) {
      services.auth.currentUser = user;
      authListener(user);
    },
    setProfile(next: ProductUserProfile | null) { profile = next; },
    fail(error: Error) { failure = error; }
  };
}

function findElement(node: ReactNode, matches: (element: ReactElement) => boolean): ReactElement | null {
  if (Array.isArray(node)) {
    for (const child of node) {
      const found = findElement(child, matches);
      if (found) return found;
    }
  } else if (isValidElement(node)) {
    if (matches(node)) return node;
    return findElement(node.props.children, matches);
  }
  return null;
}

function authFixture(existing: ProductUserProfile | null = null) {
  const hooks = controlledHooks();
  const events: string[] = [];
  const writes: Record<string, unknown>[] = [];
  const account = { uid: "signed-in-user", email: "user@example.test", displayName: "Test user" };
  const services = { db: {}, auth: { currentUser: null as typeof account | null } };
  let profile = existing ? { ...existing, id: account.uid } : null;
  let refreshCount = 0;
  let failedRefresh = 0;
  const session = {
    user: null as typeof account | null,
    loading: false,
    error: null as string | null,
    profile: null as ProductUserProfile | null,
    async refreshProfile() {
      const count = ++refreshCount;
      await Promise.resolve();
      events.push("refresh");
      if (count === failedRefresh) return null;
      session.profile = profile ?? { id: account.uid, roles: [] };
      return session.profile;
    }
  };
  const { ensureProductAccount } = loadModule<typeof import("../lib/firebase/account-actions")>("../lib/firebase/account-actions.ts", {
    "@/lib/product/profile": productProfile,
    "firebase/firestore": {
      doc: (_db: unknown, ...segments: string[]) => {
        assert.deepEqual(segments, ["users", account.uid]);
        return segments;
      },
      runTransaction: async (_db: unknown, operation: (transaction: unknown) => Promise<void>) => {
        await operation({
          get: async () => ({ exists: () => profile !== null, data: () => profile }),
          update: (_reference: unknown, fields: Record<string, unknown>) => {
            writes.push(fields);
            profile = { ...profile, ...fields, id: account.uid };
          },
          set: (_reference: unknown, fields: Record<string, unknown>) => {
            writes.push(fields);
            profile = { ...fields, id: account.uid };
          }
        });
        events.push("initialized");
      }
    }
  });
  const authenticate = async () => {
    events.push("authenticate");
    services.auth.currentUser = account;
    session.user = account;
    return { user: account };
  };
  const { ProductAuthClient } = loadModule<{ ProductAuthClient: () => ReactElement }>("../components/product/product-auth-client.tsx", {
    react: hooks.react,
    "next/image": () => null,
    "next/link": "a",
    "next/navigation": {
      useRouter: () => ({ replace: (path: string) => events.push(`route:${path}`) }),
      useSearchParams: () => new URLSearchParams("method=email&role=EMPLOYER&next=/app/employer/post-job")
    },
    "firebase/auth": {
      signInWithEmailAndPassword: authenticate,
      createUserWithEmailAndPassword: authenticate,
      updateProfile: async () => {}
    },
    "@/components/site-icon": { SiteIcon: () => null },
    "@/lib/firebase/client": { getFirebaseServices: () => services },
    "@/lib/firebase/auth-errors": { FIREBASE_SETUP_ERROR: "Configuration unavailable", firebaseAuthErrorMessage: (error: Error) => error.message },
    "@/lib/firebase/account-actions": { ensureProductAccount },
    "@/lib/product/profile": productProfile,
    "./use-product-session": { useProductSession: () => session },
    "./provider-sign-in": { ProviderSignIn: () => null }
  });
  const render = () => hooks.render(ProductAuthClient);
  return {
    events,
    writes,
    render,
    profile: () => profile,
    session,
    failRefresh(count: number) { failedRefresh = count; },
    async submit() {
      const form = findElement(render(), (element) => element.type === "form");
      assert.ok(form);
      await form.props.onSubmit({ preventDefault() {} });
    },
    signInAlready() {
      services.auth.currentUser = account;
      session.user = account;
    }
  };
}

function adminFixture({ signedIn = true, failure = null, allowed = false }: { signedIn?: boolean; failure?: Error | null; allowed?: boolean } = {}) {
  const hooks = controlledHooks();
  const events: string[] = [];
  const account = { uid: "signed-in-user", getIdToken: async () => "synthetic-test-token" };
  const services = { auth: { currentUser: signedIn ? account : null } };
  const session = { user: services.auth.currentUser, loading: false, error: null };
  const router = { replace: (path: string) => events.push(`route:${path}`) };
  const { AdminLoginClient } = loadModule<{ AdminLoginClient: () => ReactElement }>("../components/admin/admin-login-client.tsx", {
    react: hooks.react,
    "next/link": "a",
    "next/navigation": { useRouter: () => router },
    "@/components/product/use-product-session": { useProductSession: () => session },
    "@/lib/firebase/client": { getFirebaseServices: () => services },
    "firebase/auth": {
      onAuthStateChanged: () => { throw new Error("Admin must use the shared auth subscription"); },
      signOut: async () => { events.push("signOut"); services.auth.currentUser = null; },
      signInWithEmailAndPassword: async () => {
        events.push("authenticate");
        services.auth.currentUser = account;
        session.user = account;
        render();
        hooks.flushEffects();
        return { user: account };
      }
    }
  }, {
    fetch: async (path: string, options: RequestInit) => {
      assert.equal(path, "/api/admin/session");
      assert.equal(options.method, "POST");
      events.push("authorize");
      if (failure) throw failure;
      return { ok: allowed, json: async () => ({ error: "Admin permission denied" }) };
    }
  });
  const render = () => hooks.render(AdminLoginClient);
  return {
    events,
    render,
    hooks,
    currentUser: () => services.auth.currentUser,
    async resume() {
      render();
      hooks.flushEffects();
      await new Promise<void>((resolve) => setImmediate(resolve));
    },
    async submit() {
      const form = findElement(render(), (element) => element.type === "form");
      assert.ok(form);
      await form.props.onSubmit({ preventDefault() {} });
    }
  };
}

function navigationFixture(session: Record<string, unknown>) {
  let pathname = "/";
  const { SiteNavigation } = loadModule<{ SiteNavigation: () => ReactElement }>("../components/site-navigation.tsx", {
    "next/image": () => null,
    "next/link": ({ children, ...props }: ComponentProps<"a">) => createElement("a", props, children),
    "next/navigation": { usePathname: () => pathname },
    "@/lib/public-site": { primaryNav: [{ href: "/", label: "Home" }, { href: "/jobs", label: "Jobs" }] },
    "./product/use-product-session": { useProductSession: () => session },
    "./site-icon": { SiteIcon: () => null }
  });
  return (nextPath: string) => {
    pathname = nextPath;
    return renderToStaticMarkup(createElement(SiteNavigation));
  };
}

describe("shared product session navigation", () => {
  it("keeps signed-in presentation across Home, Jobs, and Post job routes", () => {
    const renderNavigation = navigationFixture({
      user: { uid: "signed-in-user" },
      profile: { id: "signed-in-user", roles: ["EMPLOYER"] },
      loading: false,
      error: null
    });
    for (const route of ["/", "/jobs", "/app/employer/post-job"]) {
      const markup = renderNavigation(route);
      assert.match(markup, /href="\/app" class="header-signin">My account/);
      assert.doesNotMatch(markup, />Sign in</);
    }
  });

  it("does not call an authenticated user a guest when their profile is loading, missing, or failed", () => {
    for (const state of [
      { loading: true, error: null, profile: null },
      { loading: false, error: null, profile: { id: "signed-in-user", roles: [] } },
      { loading: false, error: "Permission denied", profile: null }
    ]) {
      const markup = navigationFixture({ user: { uid: "signed-in-user" }, ...state })("/jobs");
      assert.match(markup, />My account</);
      assert.doesNotMatch(markup, />Sign in</);
    }
  });

  it("shows Sign in only for a resolved, error-free guest", () => {
    const loading = navigationFixture({ user: null, loading: true, error: null })("/");
    const failed = navigationFixture({ user: null, loading: false, error: "Auth unavailable" })("/");
    const guest = navigationFixture({ user: null, loading: false, error: null })("/");
    assert.match(loading, /role="status">Loading account/);
    assert.doesNotMatch(loading, />Sign in</);
    assert.match(failed, />Account unavailable</);
    assert.doesNotMatch(failed, />Sign in</);
    assert.match(guest, />Sign in</);
  });

  it("mounts the shared provider at the root and not again at the product layout", () => {
    const { default: RootLayout } = loadModule<{ default: (props: { children: ReactNode }) => ReactElement }>("../app/layout.tsx", {
      "next/font/google": { IBM_Plex_Sans: () => ({ variable: "body" }), Sora: () => ({ variable: "display" }) },
      "@/components/product/use-product-session": { ProductSessionProvider: SharedProvider },
      "@/lib/public-site": { SITE_URL: "https://example.test", cityLandingTargets: [], coreSeoKeywords: [], siteMeta: {} },
      "./globals.css": {}
    });
    const { default: ProductLayout } = loadModule<{ default: (props: { children: ReactNode }) => ReactElement }>("../app/app/layout.tsx", {});
    const root = RootLayout({ children: "public content" });
    assert.equal(root.props.children.props.children.type, SharedProvider);
    assert.equal(root.props.children.props.children.props.children, "public content");
    const product = ProductLayout({ children: "product content" });
    assert.equal(product.type, Fragment);
    assert.equal(product.props.children, "product content");
  });
});

describe("shared profile hydration", () => {
  it("renders public server content without initializing Firebase or reading a profile", () => {
    const { ProductSessionProvider } = loadModule<{ ProductSessionProvider: (props: { children: ReactNode }) => ReactElement }>("../components/product/use-product-session.ts", {
      "firebase/auth": {},
      "firebase/firestore": {},
      "@/lib/firebase/client": { getFirebaseServices: () => { throw new Error("Firebase must not initialize during public SSR"); } },
      "@/lib/firebase/auth-errors": { FIREBASE_SETUP_ERROR: "Configuration unavailable", firebaseAuthErrorMessage: () => { throw new Error("Unexpected server initialization"); } },
      "@/lib/product/profile": productProfile
    });
    const markup = renderToStaticMarkup(createElement(ProductSessionProvider, null, createElement("main", null, "Public jobs")));
    assert.equal(markup, "<main>Public jobs</main>");
  });

  it("reports browser Firebase setup failures as errors rather than resolved guests", () => {
    const fixture = sessionFixture(new Error("Firebase initialization failed"));
    try {
      const failed = fixture.session();
      assert.equal(failed.loading, false);
      assert.equal(failed.error, "Firebase initialization failed");
      assert.equal(fixture.subscriptions(), 0);
      assert.deepEqual(fixture.reads, []);
      assert.doesNotMatch(navigationFixture(failed)("/jobs"), />Sign in</);
    } finally {
      fixture.hooks.dispose();
    }
  });

  it("returns the canonical users/uid profile and keeps one auth subscription on rerender", async () => {
    const fixture = sessionFixture();
    try {
      fixture.emitAuth(fixture.account);
      const loaded = await fixture.session().refreshProfile();
      assert.equal(loaded?.id, fixture.account.uid);
      assert.equal(loaded?.role, "EMPLOYER");
      assert.equal(fixture.session().profile, loaded);
      fixture.hooks.flushEffects();
      assert.equal(fixture.subscriptions(), 1);
      assert.ok(fixture.reads.length > 0);
      for (const segments of fixture.reads) assert.deepEqual(segments, ["users", fixture.account.uid]);
    } finally {
      fixture.hooks.dispose();
    }
  });

  it("keeps an authenticated missing-profile account distinct from a guest", async () => {
    const fixture = sessionFixture();
    try {
      fixture.setProfile(null);
      fixture.emitAuth(fixture.account);
      const loaded = await fixture.session().refreshProfile();
      assert.equal(loaded?.id, fixture.account.uid);
      assert.deepEqual(loaded?.roles, []);
      assert.equal(fixture.session().user?.uid, fixture.account.uid);
      assert.equal(fixture.session().currentRole, null);
      assert.equal(fixture.session().loading, false);
      assert.equal(fixture.session().error, null);
    } finally {
      fixture.hooks.dispose();
    }
  });

  it("returns null and clears stale roles on profile failure without clearing auth", async () => {
    const fixture = sessionFixture();
    try {
      fixture.emitAuth(fixture.account);
      await fixture.session().refreshProfile();
      assert.equal(fixture.session().currentRole, "EMPLOYER");
      fixture.fail(new Error("Profile permission denied"));
      assert.equal(await fixture.session().refreshProfile(), null);
      const failed = fixture.session();
      assert.equal(failed.user?.uid, fixture.account.uid);
      assert.equal(failed.profile, null);
      assert.equal(failed.currentRole, null);
      assert.deepEqual(failed.availableRoles, []);
      assert.equal(failed.error, "Profile permission denied");
      assert.equal(failed.loading, false);
    } finally {
      fixture.hooks.dispose();
    }
  });
});

describe("email account initialization and routing", () => {
  it("initializes a missing canonical account and finishes shared hydration before routing", async () => {
    const fixture = authFixture();
    await fixture.submit();
    assert.deepEqual(fixture.events, ["authenticate", "refresh", "initialized", "refresh", "route:/app/employer/post-job"]);
    assert.equal(fixture.profile()?.id, "signed-in-user");
    assert.deepEqual(fixture.profile()?.roles, ["EMPLOYER"]);
  });

  it("preserves existing worker roles and account fields when employer is explicitly selected", async () => {
    const fixture = authFixture({ roles: ["WORKER"], role: "WORKER", trustTier: "VERIFIED", savedJobs: ["saved-job"], fullName: "Existing name" });
    await fixture.submit();
    assert.deepEqual(fixture.profile()?.roles, ["WORKER", "EMPLOYER"]);
    assert.equal(fixture.profile()?.trustTier, "VERIFIED");
    assert.equal(fixture.profile()?.fullName, "Existing name");
    assert.deepEqual(fixture.profile()?.savedJobs, ["saved-job"]);
    assert.equal(fixture.writes.some((fields) => "isActive" in fields), false);
  });

  it("does not reactivate or route an inactive account", async () => {
    const fixture = authFixture({ roles: ["EMPLOYER"], isActive: false });
    await fixture.submit();
    assert.equal(fixture.profile()?.isActive, false);
    assert.deepEqual(fixture.writes, []);
    assert.equal(fixture.events.some((event) => event.startsWith("route:")), false);
    assert.ok(findElement(fixture.render(), (element) => element.props.role === "alert" && /inactive/.test(element.props.children)));
  });

  it("does not overwrite roles outside worker/employer self-enrollment", async () => {
    const fixture = authFixture({ roles: ["ADMIN"], role: "ADMIN", activeRole: "ADMIN" });
    await fixture.submit();
    assert.deepEqual(fixture.profile()?.roles, ["ADMIN"]);
    assert.equal(fixture.profile()?.role, "ADMIN");
    assert.deepEqual(fixture.writes, []);
    assert.equal(fixture.events.some((event) => event.startsWith("route:")), false);
  });

  it("can open an already-configured employer workspace without rewriting other roles", async () => {
    const fixture = authFixture({ roles: ["EMPLOYER", "ADMIN"], role: "ADMIN", activeRole: "EMPLOYER" });
    await fixture.submit();
    assert.deepEqual(fixture.profile()?.roles, ["EMPLOYER", "ADMIN"]);
    assert.equal(fixture.profile()?.role, "ADMIN");
    assert.deepEqual(fixture.writes, []);
    assert.equal(fixture.events.at(-1), "route:/app/employer/post-job");
  });

  for (const failedRefresh of [1, 2]) {
    it(`does not route when shared hydration fails at refresh ${failedRefresh}`, async () => {
      const fixture = authFixture();
      fixture.failRefresh(failedRefresh);
      await fixture.submit();
      assert.equal(fixture.events.some((event) => event.startsWith("route:")), false);
      assert.equal(fixture.session.user?.uid, "signed-in-user");
      assert.ok(findElement(fixture.render(), (element) => element.props.role === "alert"));
    });
  }

  it("continues a signed-in account only on explicit submit, without email reauthentication", async () => {
    const fixture = authFixture();
    fixture.signInAlready();
    fixture.render();
    assert.deepEqual(fixture.events, []);
    await fixture.submit();
    assert.deepEqual(fixture.events, ["refresh", "initialized", "refresh", "route:/app/employer/post-job"]);
  });
});

describe("admin authorization keeps product auth intact", () => {
  for (const failure of [null, new Error("Network unavailable")]) {
    const reason = failure ? "network failure" : "permission denial";
    it(`keeps the shared product user signed in after automatic admin ${reason}`, async () => {
      const fixture = adminFixture({ failure });
      try {
        await fixture.resume();
        assert.deepEqual(fixture.events, ["authorize"]);
        assert.equal(fixture.currentUser()?.uid, "signed-in-user");
        assert.match(renderToStaticMarkup(fixture.render()), failure ? /Network unavailable/ : /Admin permission denied/);
      } finally {
        fixture.hooks.dispose();
      }
    });

    it(`keeps product auth after explicit admin sign-in ends in ${reason}`, async () => {
      const fixture = adminFixture({ signedIn: false, failure });
      try {
        await fixture.submit();
        assert.deepEqual(fixture.events, ["authenticate", "authorize"]);
        assert.equal(fixture.currentUser()?.uid, "signed-in-user");
        assert.ok(findElement(fixture.render(), (element) => element.props.role === "alert"));
      } finally {
        fixture.hooks.dispose();
      }
    });
  }

  it("routes to admin only after the actual server gate accepts the session", async () => {
    const fixture = adminFixture({ allowed: true });
    try {
      await fixture.resume();
      assert.deepEqual(fixture.events, ["authorize", "route:/admin"]);
      assert.equal(fixture.currentUser()?.uid, "signed-in-user");
    } finally {
      fixture.hooks.dispose();
    }
  });
});