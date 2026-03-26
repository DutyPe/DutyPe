"use client";

import { useEffect, useMemo, useState } from "react";
import { User, onAuthStateChanged } from "firebase/auth";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import { serverTimestamp } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
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
  refreshProfile: () => Promise<void>;
  setActiveRole: (role: ProductRole) => Promise<void>;
  user: User | null;
};

export function useProductSession(): ProductSession {
  const services = useMemo(() => getFirebaseServices(), []);
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<ProductUserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function loadProfile(userToLoad: User | null) {
    if (!services || !userToLoad) {
      setProfile(null);
      return;
    }

    try {
      const snapshot = await getDoc(doc(services.db, "users", userToLoad.uid));

      if (!snapshot.exists()) {
        setProfile({
          email: userToLoad.email,
          fullName: userToLoad.displayName ?? "",
          id: userToLoad.uid,
          roles: []
        });
        return;
      }

      setProfile({
        id: snapshot.id,
        ...(snapshot.data() as ProductUserProfile)
      });
      setError(null);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Failed to load user profile.");
    }
  }

  useEffect(() => {
    if (!services) {
      setError("Firebase is not configured for the product app.");
      setLoading(false);
      return;
    }

    const unsubscribe = onAuthStateChanged(
      services.auth,
      async (currentUser) => {
        setUser(currentUser);
        setLoading(true);

        if (!currentUser) {
          setProfile(null);
          setLoading(false);
          return;
        }

        await loadProfile(currentUser);
        setLoading(false);
      },
      (authError) => {
        setError(authError.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [services]);

  const availableRoles = extractProductRoles(profile);
  const currentRole = getActiveProductRole(profile);

  async function refreshProfile() {
    if (!user) {
      setProfile(null);
      return;
    }

    await loadProfile(user);
  }

  async function setActiveRole(role: ProductRole) {
    if (!services || !user) {
      return;
    }

    await updateDoc(doc(services.db, "users", user.uid), {
      activeRole: role,
      lastActiveAt: serverTimestamp()
    });

    setProfile((current) =>
      current
        ? {
            ...current,
            activeRole: role,
            role
          }
        : current
    );
  }

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
