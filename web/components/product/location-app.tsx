"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { doc, setDoc, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  hasValidCoordinates,
  workerLocationFromProfile
} from "@/lib/product/location";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type WorkerLocationForm = {
  currentLocationAddress: string;
  currentLocationLabel: string;
  latitude: string;
  longitude: string;
};

const initialForm: WorkerLocationForm = {
  currentLocationAddress: "",
  currentLocationLabel: "",
  latitude: "",
  longitude: ""
};

function toTextCoordinate(value: unknown): string {
  return typeof value === "number" && Number.isFinite(value) && value !== 0
    ? value.toFixed(6)
    : "";
}

function getBrowserPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      reject(new Error("Browser geolocation is not available on this device."));
      return;
    }

    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      maximumAge: 60_000,
      timeout: 15_000
    });
  });
}

export function WorkerLocationClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [form, setForm] = useState<WorkerLocationForm>(initialForm);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setForm({
      currentLocationAddress: session.profile?.currentLocationAddress ?? "",
      currentLocationLabel: session.profile?.currentLocationLabel ?? "",
      latitude: toTextCoordinate(session.profile?.latitude),
      longitude: toTextCoordinate(session.profile?.longitude)
    });
  }, [session.profile]);

  const savedLocation = workerLocationFromProfile(session.profile);

  async function handleUseCurrentLocation() {
    try {
      setLocating(true);
      setError(null);
      setMessage(null);

      const position = await getBrowserPosition();
      const nextLatitude = position.coords.latitude.toFixed(6);
      const nextLongitude = position.coords.longitude.toFixed(6);

      setForm((current) => ({
        ...current,
        currentLocationLabel: current.currentLocationLabel || "Current location",
        latitude: nextLatitude,
        longitude: nextLongitude
      }));
    } catch (locationError) {
      setError(
        locationError instanceof Error
          ? locationError.message
          : "Unable to get the device location."
      );
    } finally {
      setLocating(false);
    }
  }

  async function handleSaveLocation() {
    if (!services || !session.user) {
      return;
    }

    const latitude = Number(form.latitude);
    const longitude = Number(form.longitude);

    if (!hasValidCoordinates(latitude, longitude)) {
      setError("Add a valid location before saving. Use current location or enter coordinates manually.");
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setMessage(null);

      const currentTime = Date.now();
      const payload = {
        currentLocationAddress: form.currentLocationAddress.trim(),
        currentLocationLabel: form.currentLocationLabel.trim() || "Saved location",
        latitude,
        longitude,
        updatedAt: currentTime
      };

      await updateDoc(doc(services.db, "users", session.user.uid), payload);
      await setDoc(
        doc(services.db, "worker_profiles", session.user.uid),
        {
          ...payload,
          userId: session.user.uid
        },
        { merge: true }
      );

      await session.refreshProfile();
      setMessage("Worker location saved.");
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "Failed to save worker location."
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Saved area</span>
            <strong>{savedLocation?.label || "Not set"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Coordinates</span>
            <strong>
              {savedLocation
                ? `${savedLocation.latitude.toFixed(4)}, ${savedLocation.longitude.toFixed(4)}`
                : "Pending"}
            </strong>
          </div>
          <div className="product-summary-card">
            <span>Job discovery impact</span>
            <strong>{savedLocation ? "Nearby sorting enabled" : "Nearby sorting disabled"}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Why this matters</span>
            <strong>Hyperlocal job discovery</strong>
            <p>
              Android uses the worker location to make nearby openings feel immediate. The web
              app now follows the same direction by saving a dedicated worker location.
            </p>
          </article>

          <article className="product-summary-card">
            <span>Best practice</span>
            <strong>Save the area you can actually work in</strong>
            <p>
              Use your current location for fast setup, then confirm the area label so nearby
              jobs and route decisions stay realistic.
            </p>
          </article>
        </div>

        <div className="button-row">
          <button
            type="button"
            className="button"
            disabled={locating}
            onClick={() => void handleUseCurrentLocation()}
          >
            {locating ? "Detecting..." : "Use current location"}
          </button>
          <Link href="/app/worker/jobs" className="button ghost">
            Open nearby jobs
          </Link>
        </div>

        {message ? <div className="callout">{message}</div> : null}
        {error ? <div className="callout">Location error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Worker location</span>
            <h2>Save the area you want to work in</h2>
          </div>
          <p>
            This keeps worker location separate from profile identity fields and gives the
            job feed a stable base for nearby ranking and directions.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">Current saved state</span>
            <h3>{savedLocation?.label || "Location not saved yet"}</h3>
            <p>
              {session.profile?.currentLocationAddress?.trim()
                ? session.profile.currentLocationAddress
                : "Add a clear area or landmark so nearby jobs feel trustworthy and practical."}
            </p>
            <div className="pill-row">
              <span className="pill">
                {savedLocation ? "Nearby filters active" : "Nearby filters inactive"}
              </span>
              <span className="pill">
                {savedLocation ? "Directions enabled on jobs" : "Directions unavailable"}
              </span>
            </div>
          </article>

          <article className="detail-panel">
            <span className="card-kicker">How to use it</span>
            <h3>Keep both human and machine-readable location</h3>
            <ul className="detail-list">
              <li>
                <strong>Area label</strong>
                <span>Short readable label like Banjara Hills, Madhapur, or Begumpet.</span>
              </li>
              <li>
                <strong>Address details</strong>
                <span>Add landmarks or street context if the area alone is too broad.</span>
              </li>
              <li>
                <strong>Coordinates</strong>
                <span>Needed for nearby sorting and route links across the worker flow.</span>
              </li>
            </ul>
          </article>
        </div>

        <div className="editor-form">
          <label>
            <span>Area label</span>
            <input
              value={form.currentLocationLabel}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  currentLocationLabel: event.target.value
                }))
              }
              placeholder="Madhapur"
            />
          </label>

          <label>
            <span>Latitude</span>
            <input
              value={form.latitude}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  latitude: event.target.value
                }))
              }
              placeholder="17.448294"
            />
          </label>

          <label>
            <span>Longitude</span>
            <input
              value={form.longitude}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  longitude: event.target.value
                }))
              }
              placeholder="78.391487"
            />
          </label>

          <label className="editor-form-wide">
            <span>Address details</span>
            <input
              value={form.currentLocationAddress}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  currentLocationAddress: event.target.value
                }))
              }
              placeholder="Near metro, opposite main road, flat area landmark"
            />
          </label>

          <div className="editor-form-actions button-row">
            <button
              type="button"
              className="button"
              disabled={saving}
              onClick={() => void handleSaveLocation()}
            >
              {saving ? "Saving..." : "Save worker location"}
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
