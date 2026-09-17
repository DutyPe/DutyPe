"use client";

import { createContext, createElement, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { User, onAuthStateChanged } from "firebase/auth";
import { doc, getDoc, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { FIREBASE_SETUP_ERROR, firebaseAuthErrorMessage } from "@/lib/firebase/auth-errors";
import {
  extractProductRoles,
  getActiveProductRole,
  type ProductRole,
  type ProductUserProfile
} from "@/lib/product/profile";

export type ProductSession = {
  availableRoles: ProductRole[];
  currentRole: ProductRole | null;
  error: string | null;
  loading: boolean;
  profile: ProductUserProfile | null;
  refreshProfile: () => Promise<ProductUserProfile | null>;
  setActiveRole: (role: ProductRole) => Promise<void>;
  user: User | null;
};

const ProductSessionContext = createContext<ProductSession | null>(null);

export function ProductSessionProvider({ children }: { children: ReactNode }) {
  const session = useProductSessionState();
  return createElement(ProductSessionContext.Provider, { value: session }, children);
}

export function useProductSession(): ProductSession {
  const session = useContext(ProductSessionContext);
  if (!session) throw new Error("ProductSessionProvider is required for product pages.");
  return session;
}

function useProductSessionState(): ProductSession {
  const { services, setupError } = useMemo(() => {
    if (typeof window === "undefined") return { services: null, setupError: null };
    try {
      return { services: getFirebaseServices(), setupError: null };
    } catch (initializationError) {
      return { services: null, setupError: firebaseAuthErrorMessage(initializationError) };
    }
  }, []);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<ProductUserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);
  const currentUser = useRef<User | null>(null);
  const profileRequest = useRef(0);

  const loadProfile = useCallback(async (userToLoad: User | null, showLoading = true) => {
    const requestId = ++profileRequest.current;
    if (!services || !userToLoad) {
      setProfile(null);
      setError(services ? null : setupError ?? FIREBASE_SETUP_ERROR);
      setLoading(false);
      return null;
    }

    if (showLoading) setLoading(true);
    setError(null);
    let timeout: ReturnType<typeof setTimeout> | undefined;
    const isCurrentRequest = () => requestId === profileRequest.current &&
      currentUser.current?.uid === userToLoad.uid;

    try {
      const snapshot = await Promise.race([
        getDoc(doc(services.db, "users", userToLoad.uid)),
        new Promise<never>((_, reject) => {
          timeout = setTimeout(() => reject(new Error("Account loading timed out. Check your connection and try again.")), 10_000);
        })
      ]);

      if (!isCurrentRequest()) return null;

      const loadedProfile: ProductUserProfile = snapshot.exists()
        ? { ...(snapshot.data() as ProductUserProfile), id: userToLoad.uid }
        : {
            email: userToLoad.email,
            fullName: userToLoad.displayName ?? "",
            id: userToLoad.uid,
            roles: []
          };
      setProfile(loadedProfile);
      setError(null);
      return loadedProfile;
    } catch (loadError) {
      if (isCurrentRequest()) {
        setProfile(null);
        setError(loadError instanceof Error ? loadError.message : "Unable to load your account. Please try again.");
      }
      return null;
    } finally {
      clearTimeout(timeout);
      if (isCurrentRequest()) setLoading(false);
    }
  }, [services, setupError]);

  useEffect(() => {
    if (!services) {
      setError(setupError ?? FIREBASE_SETUP_ERROR);
      setLoading(false);
      return;
    }

    let disposed = false;
    const authTimeout = setTimeout(() => {
      setError("Account loading timed out. Check your connection and try again.");
      setLoading(false);
    }, 10_000);

    const unsubscribe = onAuthStateChanged(
      services.auth,
      (nextUser) => {
        if (disposed) return;
        clearTimeout(authTimeout);
        if (currentUser.current?.uid !== nextUser?.uid) setProfile(null);
        currentUser.current = nextUser;
        setUser(nextUser);
        void loadProfile(nextUser);
      },
      (authError) => {
        if (disposed) return;
        clearTimeout(authTimeout);
        ++profileRequest.current;
        currentUser.current = null;
        setUser(null);
        setProfile(null);
        setError(firebaseAuthErrorMessage(authError));
        setLoading(false);
      }
    );

    return () => {
      disposed = true;
      currentUser.current = null;
      clearTimeout(authTimeout);
      unsubscribe();
    };
  }, [services, loadProfile, setupError]);

  const availableRoles = extractProductRoles(profile);
  const currentRole = getActiveProductRole(profile);

  const refreshProfile = useCallback(async () => {
    const authenticatedUser = services?.auth.currentUser ?? null;
    const sameAccount = currentUser.current?.uid === authenticatedUser?.uid;
    if (!sameAccount) setProfile(null);
    currentUser.current = authenticatedUser;
    setUser(authenticatedUser);
    return loadProfile(authenticatedUser, !sameAccount || !profile);
  }, [services, loadProfile, profile]);

  const setActiveRole = useCallback(async (role: ProductRole) => {
    const targetUser = currentUser.current;
    if (!services || !targetUser) {
      return;
    }

    await updateDoc(doc(services.db, "users", targetUser.uid), {
      activeRole: role,
      role
    });

    if (currentUser.current?.uid !== targetUser.uid) return;
    setProfile((current) =>
      current
        ? {
            ...current,
            activeRole: role,
            role
          }
        : current
    );
  }, [services]);

  return {
    availableRoles,
    currentRole,
    error,
    loading,
    profile,
    refreshProfile,
    setActiveRole,
    user
  };
}
