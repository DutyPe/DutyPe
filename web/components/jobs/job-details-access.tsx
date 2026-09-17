"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { WorkerAppActions } from "@/components/public/job-discovery";
import { useProductSession } from "@/components/product/use-product-session";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import type { PublicJobSummary } from "@/lib/jobs/public-listings";

type Details = PublicJobSummary & { description: string; location: string; shiftTiming: string; vacancies: number | null };

export function JobDetailsAccess({ jobId }: { jobId: string }) {
  const session = useProductSession();
  const user = session.user;
  const [result, setResult] = useState<{ uid: string; id: string; job: Details } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [closed, setClosed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [retry, setRetry] = useState(0);
  const [reauthenticate, setReauthenticate] = useState(false);
  const nextPath = `/jobs/${encodeURIComponent(jobId)}`;
  const signInPath = `/app/auth?role=WORKER&next=${encodeURIComponent(nextPath)}`;

  useEffect(() => {
    setResult(null);
    setError(null);
    setReauthenticate(false);
    setClosed(false);
    if (!user) { setLoading(false); return; }
    let cancelled = false;
    const controller = new AbortController();
    let timeout: ReturnType<typeof setTimeout> | undefined;
    const deadline = new Promise<never>((_, reject) => {
      timeout = setTimeout(() => { controller.abort(); reject(new Error("Loading took too long. Please try again.")); }, 10_000);
    });
    setLoading(true);
    void (async () => {
      try {
        const response = await Promise.race([(async () => {
          const requestDetails = async (refresh: boolean) => {
            const token = await user.getIdToken(refresh);
            if (cancelled || controller.signal.aborted) throw new Error("Request cancelled");
            return fetch(`/api/jobs/${encodeURIComponent(jobId)}`, {
              headers: { Authorization: `Bearer ${token}` }, signal: controller.signal, cache: "no-store"
            });
          };
          const firstResponse = await requestDetails(false);
          return firstResponse.status === 401 ? requestDetails(true) : firstResponse;
        })(), deadline]);
        if (cancelled) return;
        if (response.status === 401) {
          setReauthenticate(true);
          setError("We couldn't verify your session for job details. Your account has not been signed out.");
          return;
        }
        if (response.status === 404 || response.status === 410) { setClosed(true); return; }
        if (!response.ok) throw new Error("Full job details are temporarily unavailable. Please try again.");
        const payload = await Promise.race([response.json(), deadline]);
        if (cancelled) return;
        if (!payload.job || typeof payload.job.description !== "string") throw new Error("Job details could not be loaded.");
        setResult({ uid: user.uid, id: jobId, job: payload.job });
      } catch (failure) {
        if (!cancelled) setError(controller.signal.aborted ? "Loading took too long. Please try again." : failure instanceof Error ? failure.message : "Unable to load job details.");
      } finally {
        clearTimeout(timeout);
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; clearTimeout(timeout); controller.abort(); };
  }, [jobId, user, retry]);

  if (session.loading && !user) return <p role="status">Checking your account...</p>;
  if (session.error && !user) return <div className="callout" role="alert"><p>Your account could not be checked. Please try again.</p><button type="button" className="button ghost" onClick={() => void session.refreshProfile()}>Retry account check</button><Link href={signInPath} className="text-link">Sign in</Link></div>;
  if (!user) return (
    <section className="job-detail-gate" aria-labelledby="job-signin-heading">
      <h2 id="job-signin-heading">Sign in for full job details</h2>
      <p>View the job description, work location and shift details with your DutyPe account.</p>
      <Link href={signInPath} className="button">Sign in to view details</Link>
      <Link href="/jobs" className="text-link">Keep browsing jobs</Link>
    </section>
  );
  if (loading) return <p role="status">Loading full job details...</p>;
  if (closed) return <section className="job-detail-gate"><h2>This job is no longer available</h2><Link className="button" href="/jobs">Find other live jobs</Link></section>;
  if (error) return <div className="callout" role="alert"><p>{error}</p><button type="button" className="button ghost" onClick={() => setRetry((current) => current + 1)}>Try again</button>{reauthenticate ? <Link href={signInPath} className="text-link">Sign in again</Link> : null}</div>;
  const job = result?.uid === user.uid && result.id === jobId ? result.job : null;
  if (!job) return null;
  return (
    <section className="job-full-details" aria-labelledby="full-job-heading">
      <h2 id="full-job-heading">About this job</h2>
      <p className="job-full-description">{job.description || "The employer has not added a description."}</p>
      <dl className="public-job-facts">
        <div><dt>Work location</dt><dd>{job.location || [job.area, job.city].filter(Boolean).join(", ") || "Not provided"}</dd></div>
        <div><dt>Pay</dt><dd>{formatCurrencyRange(job.payAmount, job.payType)}</dd></div>
        <div><dt>Shift</dt><dd>{job.shiftTiming || "Confirm with the employer"}</dd></div>
        {job.vacancies ? <div><dt>Vacancies</dt><dd>{job.vacancies}</dd></div> : null}
      </dl>
      <p>Applications and employer conversations continue in the DutyPe Android app.</p>
      <WorkerAppActions />
      <p className="location-feedback">Never pay a recruitment fee or deposit to get a job.</p>
    </section>
  );
}