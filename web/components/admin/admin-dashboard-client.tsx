"use client";

import { useEffect, useMemo, useState } from "react";
import { collection, getDocs, limit, query } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";

type DashboardSnapshot = {
  totalUsers: number;
  workers: number;
  employers: number;
  totalJobs: number;
  activeJobs: number;
  totalApplications: number;
  pendingApplications: number;
  totalReferrals: number;
  completedReferrals: number;
};

export function AdminDashboardClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const [state, setState] = useState<{
    loading: boolean;
    error: string | null;
    snapshot: DashboardSnapshot | null;
  }>({
    loading: true,
    error: null,
    snapshot: null
  });

  useEffect(() => {
    async function load() {
      if (!services) {
        setState({
          loading: false,
          error: "Firebase is not configured.",
          snapshot: null
        });
        return;
      }

      try {
        const [jobsSnap, applicationsSnap, referralsSnap, usersSnap] = await Promise.all([
          getDocs(query(collection(services.db, "jobs"), limit(1000))),
          getDocs(query(collection(services.db, "job_applications"), limit(1000))),
          getDocs(query(collection(services.db, "referrals"), limit(1000))),
          getDocs(query(collection(services.db, "users"), limit(1000)))
        ]);

        const users = usersSnap.docs.map((doc) => doc.data());
        const jobs = jobsSnap.docs.map((doc) => doc.data());
        const applications = applicationsSnap.docs.map((doc) => doc.data());
        const referrals = referralsSnap.docs.map((doc) => doc.data());

        setState({
          loading: false,
          error: null,
          snapshot: {
            totalUsers: users.length,
            workers: users.filter((item) => {
              const roles = Array.isArray(item.roles) ? item.roles : [];
              return item.role === "WORKER" || item.activeRole === "WORKER" || roles.includes("WORKER");
            }).length,
            employers: users.filter((item) => {
              const roles = Array.isArray(item.roles) ? item.roles : [];
              return item.role === "EMPLOYER" || item.activeRole === "EMPLOYER" || roles.includes("EMPLOYER");
            }).length,
            totalJobs: jobs.length,
            activeJobs: jobs.filter((item) => item.isActive).length,
            totalApplications: applications.length,
            pendingApplications: applications.filter((item) => item.status === "PENDING").length,
            totalReferrals: referrals.length,
            completedReferrals: referrals.filter((item) => item.status === "COMPLETED").length
          }
        });
      } catch (loadError) {
        setState({
          loading: false,
          error: loadError instanceof Error ? loadError.message : "Failed to load dashboard.",
          snapshot: null
        });
      }
    }

    void load();
  }, [services]);

  if (state.loading) {
    return <div className="empty-state">Loading live admin dashboard data from Firestore.</div>;
  }

  if (state.error || !state.snapshot) {
    return <div className="empty-state">Unable to load dashboard metrics. {state.error ?? ""}</div>;
  }

  return (
    <div className="stat-strip">
      <div className="stat-card">
        <strong>{state.snapshot.totalUsers}</strong>
        <span>
          Users total. Workers: {state.snapshot.workers}. Employers: {state.snapshot.employers}.
        </span>
      </div>
      <div className="stat-card">
        <strong>{state.snapshot.totalJobs}</strong>
        <span>Jobs total. Active: {state.snapshot.activeJobs}.</span>
      </div>
      <div className="stat-card">
        <strong>{state.snapshot.totalApplications}</strong>
        <span>Applications total. Pending: {state.snapshot.pendingApplications}.</span>
      </div>
      <div className="stat-card">
        <strong>{state.snapshot.totalReferrals}</strong>
        <span>Referrals total. Completed: {state.snapshot.completedReferrals}.</span>
      </div>
    </div>
  );
}
