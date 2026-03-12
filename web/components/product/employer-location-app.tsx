"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { doc, setDoc, updateDoc } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";
import {
  employerBaseLocationFromProfile,
  hasValidCoordinates
} from "@/lib/product/location";
import type { ProductWorkLocation } from "@/lib/product/profile";
import {
  incrementWorkLocationUsage,
  normalizeWorkLocations,
  removeWorkLocation,
  upsertWorkLocation
} from "@/lib/product/work-locations";

import type { ProductSession } from "./use-product-session";

type SharedProps = {
  session: ProductSession;
};

type EmployerLocationForm = {
  address: string;
  label: string;
  latitude: string;
  longitude: string;
};

const initialForm: EmployerLocationForm = {
  address: "",
  label: "",
  latitude: "",
  longitude: ""
};

function toCoordinateText(value: unknown): string {
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

export function EmployerLocationsClient({ session }: SharedProps) {
  const services = useMemo(() => getFirebaseServices(), []);
  const [form, setForm] = useState<EmployerLocationForm>(initialForm);
  const [saving, setSaving] = useState(false);
  const [locating, setLocating] = useState(false);
  const [busyLocationId, setBusyLocationId] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const workLocations = useMemo(
    () => normalizeWorkLocations(session.profile?.workLocations),
    [session.profile?.workLocations]
  );
  const businessBase = useMemo(
    () => employerBaseLocationFromProfile(session.profile),
    [session.profile]
  );

  useEffect(() => {
    if (workLocations.length === 0 && businessBase) {
      setForm({
        address: session.profile?.businessAddress ?? "",
        label: session.profile?.companyName?.trim() || "Main office",
        latitude: toCoordinateText(session.profile?.businessLatitude),
        longitude: toCoordinateText(session.profile?.businessLongitude)
      });
    }
  }, [
    businessBase,
    session.profile?.businessAddress,
    session.profile?.businessLatitude,
    session.profile?.businessLongitude,
    session.profile?.companyName,
    workLocations.length
  ]);

  async function updateLocationState(nextLocations: ProductWorkLocation[]) {
    if (!services || !session.user) {
      return;
    }

    await updateDoc(doc(services.db, "users", session.user.uid), {
      updatedAt: Date.now(),
      workLocations: nextLocations
    });

    await setDoc(
      doc(services.db, "employer_profiles", session.user.uid),
      {
        updatedAt: Date.now(),
        userId: session.user.uid,
        workLocations: nextLocations
      },
      { merge: true }
    );
  }

  async function handleUseCurrentLocation() {
    try {
      setLocating(true);
      setError(null);
      setMessage(null);

      const position = await getBrowserPosition();
      setForm((current) => ({
        ...current,
        label: current.label || "Current work site",
        latitude: position.coords.latitude.toFixed(6),
        longitude: position.coords.longitude.toFixed(6)
      }));
    } catch (locationError) {
      setError(
        locationError instanceof Error
          ? locationError.message
          : "Unable to get current location."
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
    if (!form.address.trim() || !form.label.trim()) {
      setError("Add both a label and address before saving this work location.");
      return;
    }

    if (!hasValidCoordinates(latitude, longitude)) {
      setError("Add valid coordinates for this work location.");
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setMessage(null);

      const nextLocations = upsertWorkLocation(workLocations, {
        addedAt: Date.now(),
        address: form.address.trim(),
        id: crypto.randomUUID(),
        label: form.label.trim(),
        latitude,
        longitude,
        usageCount: 1
      });

      await updateLocationState(nextLocations);
      await session.refreshProfile();
      setForm(initialForm);
      setMessage("Employer work location saved.");
    } catch (saveError) {
      setError(
        saveError instanceof Error
          ? saveError.message
          : "Failed to save employer location."
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleSetBusinessBase(location: ProductWorkLocation) {
    if (!services || !session.user) {
      return;
    }

    try {
      setBusyLocationId(location.id ?? null);
      setError(null);
      setMessage(null);

      const nextLocations = incrementWorkLocationUsage(workLocations, location.id ?? "");

      await updateDoc(doc(services.db, "users", session.user.uid), {
        businessAddress: location.address ?? "",
        businessLatitude: location.latitude ?? 0,
        businessLongitude: location.longitude ?? 0,
        updatedAt: Date.now(),
        workLocations: nextLocations
      });

      await setDoc(
        doc(services.db, "employer_profiles", session.user.uid),
        {
          businessAddress: location.address ?? "",
          businessLatitude: location.latitude ?? 0,
          businessLongitude: location.longitude ?? 0,
          updatedAt: Date.now(),
          userId: session.user.uid,
          workLocations: nextLocations
        },
        { merge: true }
      );

      await session.refreshProfile();
      setMessage("Business base updated from saved location.");
    } catch (updateError) {
      setError(
        updateError instanceof Error
          ? updateError.message
          : "Failed to set business base."
      );
    } finally {
      setBusyLocationId(null);
    }
  }

  async function handleRemoveLocation(locationId: string) {
    try {
      setBusyLocationId(locationId);
      setError(null);
      setMessage(null);

      const nextLocations = removeWorkLocation(workLocations, locationId);
      await updateLocationState(nextLocations);
      await session.refreshProfile();
      setMessage("Saved work location removed.");
    } catch (removeError) {
      setError(
        removeError instanceof Error
          ? removeError.message
          : "Failed to remove work location."
      );
    } finally {
      setBusyLocationId(null);
    }
  }

  return (
    <div className="product-section-stack">
      <section className="detail-panel">
        <div className="product-summary-grid">
          <div className="product-summary-card">
            <span>Business base</span>
            <strong>{businessBase?.label || "Not set"}</strong>
          </div>
          <div className="product-summary-card">
            <span>Saved work locations</span>
            <strong>{workLocations.length}</strong>
          </div>
          <div className="product-summary-card">
            <span>Default for post-job</span>
            <strong>{session.profile?.businessAddress?.trim() || "No default address"}</strong>
          </div>
        </div>

        <div className="product-insight-grid">
          <article className="product-summary-card">
            <span>Why save locations</span>
            <strong>Faster job posting</strong>
            <p>
              Android keeps reusable work locations so employers can switch job sites quickly.
              The web app now follows the same pattern.
            </p>
          </article>

          <article className="product-summary-card">
            <span>Usage pattern</span>
            <strong>Most-used locations rise first</strong>
            <p>
              Saved locations are sorted by usage count so your common posting areas stay at
              the top when you return to create jobs.
            </p>
          </article>
        </div>

        <div className="button-row">
          <Link href="/app/employer/post-job" className="button">
            Open post job
          </Link>
          <button
            type="button"
            className="button ghost"
            disabled={locating}
            onClick={() => void handleUseCurrentLocation()}
          >
            {locating ? "Detecting..." : "Use current location"}
          </button>
        </div>

        {message ? <div className="callout">{message}</div> : null}
        {error ? <div className="callout">Employer location error: {error}</div> : null}
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Employer locations</span>
            <h2>Save work sites you post jobs from often</h2>
          </div>
          <p>
            Keep a reusable list of verified work locations so job creation can move faster
            without manually re-entering the same site every time.
          </p>
        </div>

        <div className="detail-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">Current business base</span>
            <h3>{session.profile?.companyName?.trim() || "Employer profile"}</h3>
            <p>{session.profile?.businessAddress?.trim() || "No business base has been saved yet."}</p>
            <div className="pill-row">
              {businessBase ? (
                <span className="pill">
                  {businessBase.latitude.toFixed(4)}, {businessBase.longitude.toFixed(4)}
                </span>
              ) : (
                <span className="pill">Coordinates pending</span>
              )}
            </div>
          </article>

          <article className="detail-panel">
            <span className="card-kicker">Save a new location</span>
            <h3>Create a reusable work-location shortcut</h3>
            <div className="editor-form">
              <label>
                <span>Label</span>
                <input
                  value={form.label}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, label: event.target.value }))
                  }
                  placeholder="Madhapur outlet"
                />
              </label>

              <label>
                <span>Latitude</span>
                <input
                  value={form.latitude}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, latitude: event.target.value }))
                  }
                  placeholder="17.448294"
                />
              </label>

              <label>
                <span>Longitude</span>
                <input
                  value={form.longitude}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, longitude: event.target.value }))
                  }
                  placeholder="78.391487"
                />
              </label>

              <label className="editor-form-wide">
                <span>Address</span>
                <input
                  value={form.address}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, address: event.target.value }))
                  }
                  placeholder="Road number, landmark, area, city"
                />
              </label>

              <div className="editor-form-actions button-row">
                <button
                  type="button"
                  className="button"
                  disabled={saving}
                  onClick={() => void handleSaveLocation()}
                >
                  {saving ? "Saving..." : "Save work location"}
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Saved locations</span>
            <h2>Quick access for repeat job sites</h2>
          </div>
          <p>
            Most-used work sites stay at the top. Set one as your business base or remove it
            if that location is no longer active.
          </p>
        </div>

        {workLocations.length === 0 ? (
          <div className="empty-state">No saved work locations yet.</div>
        ) : (
          <div className="application-card-grid">
            {workLocations.map((location) => (
              <article key={location.id} className="card application-card">
                <div className="market-card-head">
                  <div>
                    <span className="card-kicker">Saved work site</span>
                    <h3>{location.label || "Employer location"}</h3>
                  </div>
                  <span className="status-pill neutral">
                    {(location.usageCount ?? 0).toString()} uses
                  </span>
                </div>

                <p className="market-card-copy">{location.address || "Address missing"}</p>

                <div className="market-card-meta">
                  <div className="market-meta-item">
                    <span>Latitude</span>
                    <strong>{toCoordinateText(location.latitude) || "Pending"}</strong>
                  </div>
                  <div className="market-meta-item">
                    <span>Longitude</span>
                    <strong>{toCoordinateText(location.longitude) || "Pending"}</strong>
                  </div>
                  <div className="market-meta-item">
                    <span>Added</span>
                    <strong>
                      {location.addedAt ? new Date(location.addedAt).toLocaleDateString("en-IN") : "N/A"}
                    </strong>
                  </div>
                </div>

                <div className="button-row compact market-card-actions">
                  <button
                    type="button"
                    className="button"
                    disabled={busyLocationId === location.id}
                    onClick={() => void handleSetBusinessBase(location)}
                  >
                    {busyLocationId === location.id ? "Updating..." : "Set as business base"}
                  </button>
                  <button
                    type="button"
                    className="button ghost"
                    disabled={busyLocationId === location.id}
                    onClick={() => void handleRemoveLocation(location.id ?? "")}
                  >
                    {busyLocationId === location.id ? "Updating..." : "Remove"}
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
