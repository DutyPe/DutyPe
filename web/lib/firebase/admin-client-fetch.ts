"use client";

import { onAuthStateChanged, type User } from "firebase/auth";

import { getFirebaseServices } from "@/lib/firebase/client";

async function waitForCurrentUser(timeoutMs = 5000): Promise<User | null> {
  const services = getFirebaseServices();
  if (!services) {
    return null;
  }

  if (services.auth.currentUser) {
    return services.auth.currentUser;
  }

  return new Promise((resolve) => {
    let settled = false;

    const timeout = setTimeout(() => {
      if (settled) {
        return;
      }

      settled = true;
      unsubscribe();
      resolve(services.auth.currentUser);
    }, timeoutMs);

    const unsubscribe = onAuthStateChanged(
      services.auth,
      (user) => {
        if (settled) {
          return;
        }

        settled = true;
        clearTimeout(timeout);
        unsubscribe();
        resolve(user);
      },
      () => {
        if (settled) {
          return;
        }

        settled = true;
        clearTimeout(timeout);
        unsubscribe();
        resolve(null);
      }
    );
  });
}

async function resolveAdminToken(forceRefresh = false) {
  const currentUser = await waitForCurrentUser();

  if (!currentUser) {
    return null;
  }

  try {
    return await currentUser.getIdToken(forceRefresh);
  } catch {
    return null;
  }
}

export async function adminApiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
  const headers = new Headers(init.headers ?? undefined);
  const firstToken = await resolveAdminToken(false);

  if (firstToken) {
    headers.set("Authorization", `Bearer ${firstToken}`);
  }

  const firstResponse = await fetch(input, {
    ...init,
    credentials: init.credentials ?? "include",
    headers
  });

  if (firstResponse.status !== 401) {
    return firstResponse;
  }

  const refreshToken = await resolveAdminToken(true);
  if (!refreshToken || refreshToken === firstToken) {
    return firstResponse;
  }

  const retryHeaders = new Headers(init.headers ?? undefined);
  retryHeaders.set("Authorization", `Bearer ${refreshToken}`);

  return fetch(input, {
    ...init,
    credentials: init.credentials ?? "include",
    headers: retryHeaders
  });
}
