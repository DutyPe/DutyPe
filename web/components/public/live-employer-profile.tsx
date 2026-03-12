"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, doc, getDoc, getDocs, limit, query, where } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";

type EmployerDoc = {
  companyName?: string;
  fullName?: string;
  city?: string;
  businessAddress?: string;
  trustTier?: string;
  companyRating?: number;
  isVerified?: boolean;
};

type EmployerSnapshot = {
  profile: EmployerDoc;
  activeJobs: number;
};

export function LiveEmployerProfile({ employerId }: { employerId: string }) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [snapshot, setSnapshot] = useState<EmployerSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      if (!services) {
        setError("Firebase is not configured.");
        setLoading(false);
        return;
      }

      try {
        const [profileDoc, jobsSnap] = await Promise.all([
          getDoc(doc(services.db, "users", employerId)),
          getDocs(
            query(
              collection(services.db, "jobs"),
              where("employerId", "==", employerId),
              limit(20)
            )
          )
        ]);

        if (!profileDoc.exists()) {
          setSnapshot(null);
          setError("No employer profile was found for this route.");
        } else {
          setSnapshot({
            profile: profileDoc.data() as EmployerDoc,
            activeJobs: jobsSnap.docs.filter((item) => item.data().isActive).length
          });
          setError(null);
        }
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load employer profile.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, [services, employerId]);

  if (loading) {
    return <div className="empty-state">Loading employer profile from Firestore.</div>;
  }

  if (error || !snapshot) {
    return <div className="empty-state">{error ?? "No employer profile found."}</div>;
  }

  const profile = snapshot.profile;

  return (
    <div className="detail-grid">
      <article className="detail-panel">
        <span className="card-kicker">Live employer snapshot</span>
        <h3>{profile.companyName ?? profile.fullName ?? "Unnamed employer"}</h3>
        <p>
          This route can pull a real profile document plus a small active-jobs count
          from Firestore.
        </p>
      </article>
      <article className="detail-panel">
        <span className="card-kicker">Current employer fields</span>
        <ul className="detail-list">
          <li>
            <strong>City or address</strong>
            <span>{profile.city ?? profile.businessAddress ?? "N/A"}</span>
          </li>
          <li>
            <strong>Trust tier</strong>
            <span>{profile.trustTier ?? "N/A"}</span>
          </li>
          <li>
            <strong>Company rating</strong>
            <span>{profile.companyRating ?? "N/A"}</span>
          </li>
          <li>
            <strong>Verified</strong>
            <span>{profile.isVerified ? "Yes" : "No"}</span>
          </li>
          <li>
            <strong>Active jobs found</strong>
            <span>{snapshot.activeJobs}</span>
          </li>
        </ul>
      </article>
    </div>
  );
}
