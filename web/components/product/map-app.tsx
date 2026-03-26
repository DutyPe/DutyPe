"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { collection, getDocs, limit, orderBy, query } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import { formatCurrencyRange } from "@/lib/firebase/firestore-helpers";
import {
  attachJobDistances,
  directionsUrl,
  filterJobsByRadius,
  formatDistanceLabel,
  hasValidCoordinates,
  workerLocationFromProfile,
  type LocatedProductJob
} from "@/lib/product/location";
import {
  isLiveJob,
  normalizeProductJob,
  type ProductJob
} from "@/lib/product/marketplace";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

const mapDistanceOptions = [
  { label: "500 m", value: 0.5 },
  { label: "1 km", value: 1 },
  { label: "2 km", value: 2 },
  { label: "5 km", value: 5 },
  { label: "10 km", value: 10 }
] as const;

function offsetFromUserKm(
  userLatitude: number,
  userLongitude: number,
  jobLatitude: number,
  jobLongitude: number
) {
  const kmPerLatDegree = 111.32;
  const kmPerLonDegree = 111.32 * Math.cos((userLatitude * Math.PI) / 180);

  return {
    xKm: (jobLongitude - userLongitude) * kmPerLonDegree,
    yKm: (jobLatitude - userLatitude) * kmPerLatDegree
  };
}

function markerPosition(
  locatedJob: LocatedProductJob,
  userLatitude: number,
  userLongitude: number,
  radiusKm: number
) {
  const latitude = locatedJob.job.latitude;
  const longitude = locatedJob.job.longitude;

  if (!hasValidCoordinates(latitude, longitude)) {
    return { left: 50, top: 50 };
  }

  const safeLatitude = Number(latitude);
  const safeLongitude = Number(longitude);

  const offset = offsetFromUserKm(
    userLatitude,
    userLongitude,
    safeLatitude,
    safeLongitude
  );
  const usableRadius = 38;

  const left = 50 + Math.max(-usableRadius, Math.min(usableRadius, (offset.xKm / radiusKm) * usableRadius));
  const top = 50 - Math.max(-usableRadius, Math.min(usableRadius, (offset.yKm / radiusKm) * usableRadius));

  return {
    left,
    top
  };
}

function markerSize(job: ProductJob) {
  return 14 + Math.min(job.vacancies ?? 1, 6) * 2;
}

export function WorkerMapClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [jobs, setJobs] = useState<ProductJob[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [radiusKm, setRadiusKm] = useState<number>(5);
  const [category, setCategory] = useState("ALL");
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const workerLocation = useMemo(
    () => workerLocationFromProfile(session.profile),
    [session.profile]
  );

  useEffect(() => {
    if (!services) {
      setLoading(false);
      return;
    }

    const activeServices = services;
    let cancelled = false;

    async function loadJobs() {
      try {
        setLoading(true);
        setError(null);

        const snapshot = await getDocs(
          query(collection(activeServices.db, "jobs"), orderBy("createdAt", "desc"), limit(96))
        );

        if (cancelled) {
          return;
        }

        const liveJobs = snapshot.docs
          .map((item) => normalizeProductJob(item.id, item.data() as Record<string, unknown>))
          .filter((job) => isLiveJob(job) && hasValidCoordinates(job.latitude, job.longitude));

        setJobs(liveJobs);
      } catch (loadError) {
        if (!cancelled) {
          setError(
            loadError instanceof Error ? loadError.message : "Failed to load map jobs."
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadJobs();

    return () => {
      cancelled = true;
    };
  }, [services]);

  const categories = useMemo(
    () => [
      "ALL",
      ...new Set(
        jobs
          .map((job) => job.category)
          .filter((value): value is string => typeof value === "string" && value.trim().length > 0)
      )
    ],
    [jobs]
  );

  const visibleJobs = useMemo(() => {
    const categoryFiltered =
      category === "ALL" ? jobs : jobs.filter((job) => job.category === category);

    return filterJobsByRadius(attachJobDistances(categoryFiltered, workerLocation), radiusKm);
  }, [category, jobs, radiusKm, workerLocation]);

  useEffect(() => {
    if (selectedJobId && visibleJobs.some((item) => item.job.id === selectedJobId)) {
      return;
    }

    setSelectedJobId(visibleJobs[0]?.job.id ?? null);
  }, [selectedJobId, visibleJobs]);

  const selectedJob =
    visibleJobs.find((item) => item.job.id === selectedJobId) ?? visibleJobs[0] ?? null;

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Discovery base</span>
            <strong>{workerLocation?.label || "Set location first"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Jobs on radar</span>
            <strong>{visibleJobs.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Radius</span>
            <strong>{radiusKm} km</strong>
          </div>
          <div className="product-summary-card">
            <span>Category</span>
            <strong>{category === "ALL" ? "All jobs" : category}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Map mode</span>
            <strong>Hyperlocal radar</strong>
            <p>
              This mirrors the Android nearby-jobs mindset with a live coordinate-based
              worker view, marker selection, and radius-based discovery.
            </p>
          </article>

          <article className="product-summary-card">
            <span>How to use it</span>
            <strong>Tap markers, then compare</strong>
            <p>
              Use radius chips to tighten the search, tap a marker to inspect the opening,
              then jump into the full job flow or directions.
            </p>
          </article>
        </div>

        <div className="button-row">
          <Link href="/app/worker/location" className="button">
            Update worker location
          </Link>
          <Link href="/app/worker/jobs" className="button ghost">
            Open list view
          </Link>
        </div>

        {error ? <div className="callout">Map error: {error}</div> : null}
        {!workerLocation ? (
          <div className="callout">
            Save a worker location first so the radar can center around your real work area.
          </div>
        ) : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Nearby jobs map</span>
            <h2>See openings around your saved work area</h2>
          </div>
          <p>
            Jobs with coordinates are projected around your saved worker location so you can
            evaluate which openings are truly nearby before opening the full detail flow.
          </p>
        </div>

        {!workerLocation ? (
          <div className="empty-state">
            Set a worker location first to unlock the nearby jobs radar.
          </div>
        ) : loading ? (
          <div className="empty-state">Loading map jobs from Firestore.</div>
        ) : visibleJobs.length === 0 ? (
          <div className="empty-state">
            No live jobs with coordinates matched the current radius and category filters.
          </div>
        ) : (
          <div className="map-layout">
            <div className="map-panel">
              <div className="map-toolbar">
                <div className="map-filter-stack">
                  <span className="card-kicker">Radius</span>
                  <div className="map-chip-row">
                    {mapDistanceOptions.map((option) => (
                      <button
                        key={option.value}
                        type="button"
                        className={`map-chip ${radiusKm === option.value ? "active" : ""}`}
                        onClick={() => setRadiusKm(option.value)}
                      >
                        {option.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="map-filter-stack">
                  <span className="card-kicker">Category</span>
                  <div className="map-chip-row">
                    {categories.map((item) => (
                      <button
                        key={item}
                        type="button"
                        className={`map-chip ${category === item ? "active" : ""}`}
                        onClick={() => setCategory(item)}
                      >
                        {item === "ALL" ? "All jobs" : item}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <div className="radar-stage">
                <div className="radar-ring ring-a" />
                <div className="radar-ring ring-b" />
                <div className="radar-ring ring-c" />
                <div className="radar-crosshair crosshair-x" />
                <div className="radar-crosshair crosshair-y" />

                <div className="radar-user-core">
                  <span className="radar-user-dot" />
                  <strong>You</strong>
                  <small>{workerLocation.label}</small>
                </div>

                {visibleJobs.map((item) => {
                  const position = markerPosition(
                    item,
                    workerLocation.latitude,
                    workerLocation.longitude,
                    radiusKm
                  );
                  const active = selectedJob?.job.id === item.job.id;

                  return (
                    <button
                      key={item.job.id}
                      type="button"
                      className={`radar-marker ${active ? "active" : ""}`}
                      style={{
                        left: `${position.left}%`,
                        top: `${position.top}%`,
                        width: `${markerSize(item.job)}px`,
                        height: `${markerSize(item.job)}px`
                      }}
                      aria-label={`${item.job.title} ${formatDistanceLabel(item.distanceKm) ?? ""}`}
                      onClick={() => setSelectedJobId(item.job.id)}
                    >
                      <span />
                    </button>
                  );
                })}

                <div className="radar-legend">
                  <span>{radiusKm} km radar</span>
                  <span>{visibleJobs.length} jobs visible</span>
                </div>
              </div>
            </div>

            <aside className="map-sidebar">
              {selectedJob ? (
                <article className="detail-panel map-selected-card">
                  <span className="card-kicker">{selectedJob.job.category || "LOCAL JOB"}</span>
                  <h3>{selectedJob.job.title}</h3>
                  <p>{selectedJob.job.companyName || "DutyPe employer"}</p>

                  <div className="market-card-meta">
                    <div className="market-meta-item">
                      <span>Distance</span>
                      <strong>{formatDistanceLabel(selectedJob.distanceKm) || "Nearby"}</strong>
                    </div>
                    <div className="market-meta-item">
                      <span>Pay</span>
                      <strong>
                        {formatCurrencyRange(
                          selectedJob.job.payAmount,
                          selectedJob.job.payType
                        )}
                      </strong>
                    </div>
                    <div className="market-meta-item">
                      <span>Shift</span>
                      <strong>{selectedJob.job.shiftTiming || "Shift timing pending"}</strong>
                    </div>
                  </div>

                  <p className="market-card-copy">
                    {selectedJob.job.addressText ||
                      `${selectedJob.job.location.lat}, ${selectedJob.job.location.lng}`}
                  </p>

                  <div className="button-row compact">
                    <Link
                      href={`/app/worker/jobs/${selectedJob.job.id}`}
                      className="button"
                    >
                      Open job
                    </Link>
                    {directionsUrl(selectedJob.job) ? (
                      <a
                        href={directionsUrl(selectedJob.job) ?? "#"}
                        target="_blank"
                        rel="noreferrer"
                        className="button ghost"
                      >
                        Directions
                      </a>
                    ) : null}
                  </div>
                </article>
              ) : null}

              <div className="map-list">
                {visibleJobs.slice(0, 8).map((item) => (
                  <button
                    key={item.job.id}
                    type="button"
                    className={`map-list-item ${selectedJob?.job.id === item.job.id ? "active" : ""}`}
                    onClick={() => setSelectedJobId(item.job.id)}
                  >
                    <div className="map-list-item-top">
                      <strong>{item.job.title}</strong>
                      <span>{formatDistanceLabel(item.distanceKm) || "Nearby"}</span>
                    </div>
                    <p>{item.job.companyName || "DutyPe employer"}</p>
                          <small>
                            {item.job.addressText || `${item.job.location.lat}, ${item.job.location.lng}`}
                          </small>
                  </button>
                ))}
              </div>
            </aside>
          </div>
        )}
      </section>
    </div>
  );
}
