"use client";

import { useEffect, useMemo, useState } from "react";
import { doc, getDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatTextList } from "@/lib/firebase/firestore-helpers";

type WorkerDoc = {
  fullName?: string;
  name?: string;
  city?: string;
  address?: string;
  skills?: string[] | string;
  experience?: string;
  isVerified?: boolean;
  profileCompleted?: boolean;
};

export function LiveWorkerProfile({ workerId }: { workerId: string }) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [worker, setWorker] = useState<WorkerDoc | null>(null);
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
        const snapshot = await getDoc(doc(services.db, "users", workerId));
        if (!snapshot.exists()) {
          setError("No worker profile was found for this route.");
          setWorker(null);
        } else {
          setWorker(snapshot.data() as WorkerDoc);
          setError(null);
        }
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Failed to load worker profile.");
      } finally {
        setLoading(false);
      }
    }

    void load();
  }, [services, workerId]);

  if (loading) {
    return <div className="empty-state">Loading worker profile from Firestore.</div>;
  }

  if (error || !worker) {
    return <div className="empty-state">{error ?? "No worker profile found."}</div>;
  }

  const skills = formatTextList(worker.skills);

  return (
    <div className="detail-grid">
      <article className="detail-panel">
        <span className="card-kicker">Live worker snapshot</span>
        <h3>{worker.fullName ?? worker.name ?? "Unnamed worker"}</h3>
        <p>
          {worker.experience ?? "Experience not set."} Public routes should only show
          approved profile fields here.
        </p>
      </article>
      <article className="detail-panel">
        <span className="card-kicker">Current profile fields</span>
        <ul className="detail-list">
          <li>
            <strong>City or address</strong>
            <span>{worker.city ?? worker.address ?? "N/A"}</span>
          </li>
          <li>
            <strong>Verified</strong>
            <span>{worker.isVerified ? "Yes" : "No"}</span>
          </li>
          <li>
            <strong>Profile complete</strong>
            <span>{worker.profileCompleted ? "Yes" : "No or not set"}</span>
          </li>
          <li>
            <strong>Skills</strong>
            <span>{skills.length > 0 ? skills.join(", ") : "N/A"}</span>
          </li>
        </ul>
      </article>
    </div>
  );
}
