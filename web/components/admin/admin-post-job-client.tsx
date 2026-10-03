"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";
import {
  searchIndianLocations,
  type WebLocationSuggestion
} from "@/lib/geo/location-search";

// Values are the Firestore schema keys (category / payType / shift / employmentType / gender).
const JOB_CATEGORIES: { value: string; label: string; icon: string }[] = [
  { value: "COOK", label: "Cook", icon: "👨\u200d🍳" },
  { value: "MAID", label: "Maid", icon: "🧹" },
  { value: "DRIVER", label: "Driver", icon: "🚗" },
  { value: "HELPER", label: "Helper", icon: "🤝" },
  { value: "SECURITY", label: "Security", icon: "🛡️" },
  { value: "GARDENER", label: "Gardener", icon: "🌱" },
  { value: "CARETAKER", label: "Caretaker", icon: "👥" },
  { value: "DELIVERY", label: "Delivery", icon: "📦" },
  { value: "WAITER", label: "Waiter", icon: "🍽️" },
  { value: "ELECTRICIAN", label: "Electrician", icon: "⚡" },
  { value: "PLUMBER", label: "Plumber", icon: "🔧" },
  { value: "PAINTER", label: "Painter", icon: "🎨" },
  { value: "CARPENTER", label: "Carpenter", icon: "🪚" },
  { value: "RECEPTIONIST", label: "Receptionist", icon: "💼" },
  { value: "CASHIER", label: "Cashier", icon: "💵" },
  { value: "PACKER", label: "Packer", icon: "📦" },
  { value: "SALES", label: "Sales", icon: "🛍️" },
  { value: "TELECALLER", label: "Telecaller", icon: "📞" },
  { value: "TEACHER", label: "Teacher", icon: "📚" },
  { value: "OFFICE_STAFF", label: "Office Staff", icon: "🗂️" },
  { value: "CUSTOMER_SUPPORT", label: "Customer Support", icon: "🎧" },
  { value: "FIELD_EXECUTIVE", label: "Field Work", icon: "🧭" },
  { value: "MARKETING", label: "Marketing", icon: "📣" },
  { value: "FINANCE", label: "Finance", icon: "🏦" },
  { value: "HEALTHCARE", label: "Healthcare", icon: "⚕️" },
  { value: "BEAUTICIAN", label: "Beautician", icon: "💇" },
  { value: "TAILOR", label: "Tailor", icon: "🧵" },
  { value: "MECHANIC", label: "Mechanic", icon: "🔩" },
  { value: "DATA_ENTRY", label: "Data Entry", icon: "⌨️" },
  { value: "LEGAL", label: "Legal", icon: "⚖️" },
  { value: "TUTOR", label: "Home Tutor", icon: "📖" },
  { value: "EVENT_STAFF", label: "Event Staff", icon: "🎉" },
  { value: "PROMOTER", label: "Store Promoter", icon: "🏷️" },
  { value: "PHARMACY", label: "Pharmacy Assistant", icon: "💊" },
  { value: "CONSTRUCTION", label: "Construction", icon: "🏗️" },
  { value: "OTHER", label: "Other", icon: "📋" }
];

// Keyword → category. First match wins. Ordering matters (specific before generic).
// Mirrors inferCategoryFromTitle() in PostJobScreen.kt.
const CATEGORY_KEYWORDS: Array<{ category: string; words: string[] }> = [
  { category: "DELIVERY", words: ["delivery", "courier", "rider", "swiggy", "zomato", "dunzo", "parcel"] },
  { category: "DRIVER", words: ["driver", "chauffeur", "uber", "ola", "cab", "taxi", "truck"] },
  { category: "COOK", words: ["cook", "chef", "kitchen", "tandoor", "biryani"] },
  { category: "WAITER", words: ["waiter", "server", "steward"] },
  { category: "MAID", words: ["maid", "house help", "housekeep", "babysit", "nanny", "ayah"] },
  { category: "SECURITY", words: ["security", "guard", "watchman", "bouncer"] },
  { category: "ELECTRICIAN", words: ["electrician", "wiring", "electrical"] },
  { category: "PLUMBER", words: ["plumber", "plumbing", "pipe"] },
  { category: "PAINTER", words: ["painter", "painting"] },
  { category: "CARPENTER", words: ["carpenter", "woodwork"] },
  { category: "GARDENER", words: ["gardener", "garden", "landscap", "horticult"] },
  { category: "CARETAKER", words: ["caretaker", "care taker", "caregiver"] },
  { category: "RECEPTIONIST", words: ["receptionist", "front desk"] },
  { category: "CASHIER", words: ["cashier", "billing"] },
  { category: "PACKER", words: ["packer", "packing", "loader"] },
  { category: "MAID", words: ["cleaner", "cleaning", "janitor", "sweeper"] },
  { category: "HELPER", words: ["helper", "assistant", "labour", "labor"] }
];

function detectCategoryFromTitle(title: string): string | null {
  const t = title.toLowerCase().trim();
  if (t.length < 3) return null;
  for (const { category, words } of CATEGORY_KEYWORDS) {
    for (const w of words) {
      if (t.includes(w)) return category;
    }
  }
  return null;
}

type LocationSuggestion = WebLocationSuggestion;

// PayType enum from Android (JobEnums.kt).
const SALARY_TYPES: { value: string; label: string }[] = [
  // Regular jobs are weekly or monthly only (₹3,000–40,000 / month, ₹1,000–10,000 / week;
  // full-time at least ₹8,000 / ₹2,000). Daily work is an urgent post. Enforced by postJob.
  { value: "MONTHLY", label: "Per month" },
  { value: "WEEKLY", label: "Per week" }
];

// Shift chips shown in the Android WorkScheduleSection. The persisted value
// is the displayName, e.g. "Day shift" / "Any shift". "Custom" reveals
// start/end inputs and is persisted as "start - end".
const SHIFT_OPTIONS: { value: string; label: string; icon: string }[] = [
  { value: "DAY", label: "Day shift", icon: "☀️" },
  { value: "NIGHT", label: "Night shift", icon: "🌙" },
  { value: "ANY", label: "Any shift", icon: "⏳" }
];

// Gender option list matches PostJobScreen.kt ("Male" / "Female" / "Both").
const GENDERS: { value: string; label: string }[] = [
  { value: "ANY", label: "Any" },
  { value: "MALE", label: "Male" },
  { value: "FEMALE", label: "Female" }
];

// Experience options used by the admin post-job form.
const EXPERIENCE_LEVELS = [
  "No Experience Required",
  "Less than 1 year",
  "1-3 years",
  "3-5 years",
  "5+ years"
];

// Education options used by the admin post-job form.
const EDUCATION_REQUIREMENTS = [
  "No qualification required",
  "Below 10th",
  "10th pass",
  "12th pass",
  "ITI",
  "Diploma",
  "Graduate",
  "Any qualification"
];

// Work type bucket from PostJobScreen.kt (workTypes list). Persisted on
// the `jobType` Firestore field as Full-time / Part-time / etc.
const WORK_TYPES: { value: string; label: string }[] = [
  { value: "FULL_TIME", label: "Full-time" },
  { value: "PART_TIME", label: "Part-time" }
];

const initialForm = {
  title: "",
  companyName: "",
  // Stored as the JobCategory.displayName ("Cook" / "Driver" / …) — matches
  // what PostJobScreen.kt writes to the `jobType` field on jobmetadata.
  jobType: "COOK",
  salary: "",
  salaryType: "MONTHLY",
  addressText: "",
  latitude: "",
  longitude: "",
  description: "",
  contactNumber: "",
  gender: "ANY",
  experienceRequired: "No Experience Required",
  educationRequired: "No qualification required",
  shiftTiming: "DAY",
  workType: "FULL_TIME",
  vacancies: "1",
  jobImageUrl: ""
};

const MAX_JOB_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
const SUPPORTED_JOB_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

function mapPreviewUrl(latitude: number, longitude: number): string {
  const delta = 0.006;
  const bbox = [
    longitude - delta,
    latitude - delta,
    longitude + delta,
    latitude + delta
  ].join(",");
  return `https://www.openstreetmap.org/export/embed.html?bbox=${bbox}&layer=mapnik&marker=${latitude},${longitude}`;
}

export function AdminPostJobClient() {
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [geocoding, setGeocoding] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [form, setForm] = useState(initialForm);
  const [jobTypeManuallySet, setJobTypeManuallySet] = useState(false);
  const [showCoordOverride, setShowCoordOverride] = useState(false);
  const [suggestions, setSuggestions] = useState<LocationSuggestion[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [searchingLocation, setSearchingLocation] = useState(false);
  const [selectedImageFile, setSelectedImageFile] = useState<File | null>(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);
  const [uploadingImage, setUploadingImage] = useState(false);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);

  function update<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  // Auto-detect category from title (only until the admin manually picks a category).
  useEffect(() => {
    if (jobTypeManuallySet) return;
    const detected = detectCategoryFromTitle(form.title);
    if (detected && detected !== form.jobType) {
      setForm((prev) => ({ ...prev, jobType: detected }));
    }
  }, [form.title, form.jobType, jobTypeManuallySet]);

  // Debounced address autocomplete via Nominatim. Picking a suggestion
  // fills lat/lng instantly so admins don't have to wait for a separate geocode.
  useEffect(() => {
    const query = form.addressText.trim();
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    if (query.length < 3) {
      setSuggestions([]);
      setSearchingLocation(false);
      return;
    }
    debounceTimer.current = setTimeout(async () => {
      try {
        if (abortRef.current) abortRef.current.abort();
        const controller = new AbortController();
        abortRef.current = controller;
        setSearchingLocation(true);
        const results = await searchIndianLocations(query, {
          limit: 8,
          signal: controller.signal
        });
        setSuggestions(results);
      } catch {
        // Aborted or failed — keep silent; admin can still type lat/lng manually.
      } finally {
        setSearchingLocation(false);
      }
    }, 350);
    return () => {
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [form.addressText]);

  function pickSuggestion(item: LocationSuggestion) {
    setForm((prev) => ({
      ...prev,
      addressText: item.fullAddress,
      latitude: item.latitude,
      longitude: item.longitude
    }));
    setSuggestions([]);
    setShowSuggestions(false);
  }

  function nudgePin(latitudeDelta: number, longitudeDelta: number) {
    const latitude = Number(form.latitude);
    const longitude = Number(form.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return;
    setForm((prev) => ({
      ...prev,
      latitude: (latitude + latitudeDelta).toFixed(6),
      longitude: (longitude + longitudeDelta).toFixed(6)
    }));
  }

  function updatePreviewUrl(nextUrl: string | null) {
    setImagePreviewUrl((previousUrl) => {
      if (previousUrl && previousUrl.startsWith("blob:") && previousUrl !== nextUrl) {
        URL.revokeObjectURL(previousUrl);
      }
      return nextUrl;
    });
  }

  useEffect(() => {
    return () => {
      if (imagePreviewUrl && imagePreviewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(imagePreviewUrl);
      }
    };
  }, [imagePreviewUrl]);

  function clearJobImage() {
    setSelectedImageFile(null);
    updatePreviewUrl(null);
    update("jobImageUrl", "");
    if (imageInputRef.current) {
      imageInputRef.current.value = "";
    }
  }

  function handleImageFileSelect(file: File | null) {
    if (!file) {
      return;
    }

    if (!SUPPORTED_JOB_IMAGE_TYPES.has(file.type)) {
      setError("Only JPG, PNG, and WEBP images are supported.");
      return;
    }

    if (file.size > MAX_JOB_IMAGE_SIZE_BYTES) {
      setError("Image must be 5 MB or smaller.");
      return;
    }

    setError(null);
    setSelectedImageFile(file);
    update("jobImageUrl", "");
    updatePreviewUrl(URL.createObjectURL(file));
  }

  async function uploadJobImageIfNeeded() {
    if (!selectedImageFile) {
      return form.jobImageUrl.trim() || undefined;
    }

    setUploadingImage(true);
    try {
      const payload = new FormData();
      payload.append("image", selectedImageFile);
      payload.append("employerId", "admin");

      const response = await adminApiFetch("/api/admin/jobs/upload-image", {
        method: "POST",
        body: payload
      });

      const result = (await response.json()) as { error?: string; url?: string };
      if (!response.ok || !result.url) {
        throw new Error(result.error || "Failed to upload job image.");
      }

      update("jobImageUrl", result.url);
      setSelectedImageFile(null);
      updatePreviewUrl(result.url);
      if (imageInputRef.current) {
        imageInputRef.current.value = "";
      }

      return result.url;
    } finally {
      setUploadingImage(false);
    }
  }

  async function handleGeocode() {
    if (!form.addressText.trim()) {
      setError("Enter an address before geocoding.");
      return;
    }
    try {
      setGeocoding(true);
      setError(null);
      const results = await searchIndianLocations(form.addressText.trim(), { limit: 1 });
      if (!results.length) {
        setError("No coordinates found for that address. Paste lat/lng manually.");
        return;
      }
      const location = results[0];
      setForm((prev) => ({
        ...prev,
        addressText: location.fullAddress,
        latitude: location.latitude,
        longitude: location.longitude
      }));
    } catch {
      setError("Geocoding failed. Paste latitude and longitude manually.");
    } finally {
      setGeocoding(false);
    }
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!form.title.trim() || form.title.trim().length < 3) {
      setError("Job title must be at least 3 characters.");
      return;
    }
    if (!form.companyName.trim()) {
      setError("Company name is required.");
      return;
    }
    if (!form.description.trim()) {
      setError("Description is required.");
      return;
    }
    if (!form.contactNumber.trim()) {
      setError("Contact number is required.");
      return;
    }
    if (!form.addressText.trim()) {
      setError("Address is required.");
      return;
    }
    const payAmount = form.salaryType === "NEGOTIABLE" ? 0 : Number(form.salary.replace(/[^0-9]/g, ""));
    if (form.salaryType !== "NEGOTIABLE" && (!Number.isFinite(payAmount) || payAmount < 1)) {
      setError("Enter the pay amount in rupees (numbers only).");
      return;
    }
    if (payAmount > 50000) {
      setError("Pay can be at most ₹50,000.");
      return;
    }
    const lat = Number(form.latitude);
    const lng = Number(form.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
      setError("Latitude and longitude are required. Click 'Geocode address' or paste coordinates.");
      return;
    }

    const vacancyCount = Number(form.vacancies) || 1;
    if (vacancyCount < 1 || vacancyCount > 50) {
      setError("Vacancies must be between 1 and 50.");
      return;
    }

    try {
      setSubmitting(true);
      const jobImageUrl = await uploadJobImageIfNeeded();
      const response = await adminApiFetch("/api/admin/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          companyName: form.companyName.trim(),
          category: form.jobType,
          employmentType: form.workType,
          payAmount,
          payType: form.salaryType,
          shift: form.shiftTiming,
          addressText: form.addressText.trim(),
          lat,
          lng,
          description: form.description.trim(),
          contactNumber: form.contactNumber.trim(),
          gender: form.gender,
          experienceRequired: form.experienceRequired,
          educationRequired: form.educationRequired,
          vacancies: vacancyCount,
          photoUrl: jobImageUrl
        })
      });

      const payload = (await response.json()) as { error?: string; jobId?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to post job.");

      setSuccess(payload.jobId ?? "Job posted.");
      setForm(initialForm);
      setJobTypeManuallySet(false);
      setShowCoordOverride(false);
      setSelectedImageFile(null);
      updatePreviewUrl(null);
      if (imageInputRef.current) {
        imageInputRef.current.value = "";
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to post job.");
    } finally {
      setSubmitting(false);
    }
  }

  const selectedLatitude = Number(form.latitude);
  const selectedLongitude = Number(form.longitude);
  const hasSelectedCoordinates =
    Number.isFinite(selectedLatitude) &&
    Number.isFinite(selectedLongitude) &&
    (selectedLatitude !== 0 || selectedLongitude !== 0);

  if (success) {
    return (
      <div className="admin-success-card">
        <div className="admin-success-icon">✓</div>
        <h2>Job Posted Successfully!</h2>
        <p>The job is live and appears in nearby searches in the app within seconds.</p>
        <p style={{ opacity: 0.7, fontSize: 13 }}>Job ID: <code>{success}</code></p>
        <div className="admin-success-actions">
          <button className="button" onClick={() => setSuccess(null)}>Post another job</button>
          <button className="button ghost" onClick={() => router.push("/admin/jobs")}>View all jobs</button>
        </div>
      </div>
    );
  }

  return (
    <form className="admin-post-form" onSubmit={handleSubmit}>
      {error && <div className="admin-error">{error}</div>}

      <div className="admin-form-section">
        <h3>1. Basics</h3>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Job title *</span>
            <input value={form.title} onChange={(e) => update("title", e.target.value)} placeholder="e.g. Cook for Madhapur PG" required />
          </label>
          <label className="admin-field">
            <span>Company / Employer name *</span>
            <input value={form.companyName} onChange={(e) => update("companyName", e.target.value)} placeholder="e.g. Sai Comforts PG" required />
          </label>
          <label className="admin-field">
            <span>Job category * (auto-detected from title — override anytime)</span>
            <select
              value={form.jobType}
              onChange={(e) => {
                setJobTypeManuallySet(true);
                update("jobType", e.target.value);
              }}
            >
              {JOB_CATEGORIES.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.icon} {t.label}
                </option>
              ))}
            </select>
          </label>
          <label className="admin-field">
            <span>Vacancies *</span>
            {/* BUG #8 FIX: avoid wheel-scroll mutating the number. */}
            <input
              type="number"
              min={1}
              max={50}
              value={form.vacancies}
              onChange={(e) => update("vacancies", e.target.value)}
              onWheel={(e) => (e.currentTarget as HTMLInputElement).blur()}
            />
          </label>
        </div>
      </div>

      <div className="admin-form-section">
        <h3>2. Compensation & schedule</h3>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Pay (₹) *</span>
            {/* Mirrors PostJobScreen.kt: free-form text so admins can enter
                a number, range (10000-15000), or text ("Negotiable"). */}
            <input
              value={form.salary}
              onChange={(e) => update("salary", e.target.value)}
              placeholder="e.g. 18000"
              inputMode="numeric"
              maxLength={9}
              disabled={form.salaryType === "NEGOTIABLE"}
            />
          </label>
          <label className="admin-field">
            <span>Pay per *</span>
            <select value={form.salaryType} onChange={(e) => update("salaryType", e.target.value)}>
              {SALARY_TYPES.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </label>
          <label className="admin-field">
            <span>Job type *</span>
            <select value={form.workType} onChange={(e) => update("workType", e.target.value)}>
              {WORK_TYPES.map((w) => <option key={w.value} value={w.value}>{w.label}</option>)}
            </select>
          </label>
        </div>

        <div className="admin-field">
          <span>Shift</span>
          <div className="admin-chip-row">
            {SHIFT_OPTIONS.map((s) => (
              <button
                type="button"
                key={s.value}
                className={`admin-chip ${form.shiftTiming === s.value ? "active" : ""}`}
                onClick={() => update("shiftTiming", s.value)}
              >
                {s.icon} {s.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="admin-form-section">
        <h3>3. Requirements</h3>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Gender</span>
            <select value={form.gender} onChange={(e) => update("gender", e.target.value)}>
              {GENDERS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Experience required</span>
            <select value={form.experienceRequired} onChange={(e) => update("experienceRequired", e.target.value)}>
              {EXPERIENCE_LEVELS.map((e) => <option key={e} value={e}>{e}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Education required</span>
            <select value={form.educationRequired} onChange={(e) => update("educationRequired", e.target.value)}>
              {EDUCATION_REQUIREMENTS.map((item) => <option key={item} value={item}>{item}</option>)}
            </select>
          </label>
        </div>
      </div>

      <div className="admin-form-section">
        <h3>4. Job image (optional)</h3>
        <label className="admin-field">
          <span>Upload job image (like Android post-job)</span>
          <input
            ref={imageInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => handleImageFileSelect(event.target.files?.[0] ?? null)}
          />
          <small style={{ marginTop: 6, opacity: 0.75 }}>
            Supported: JPG, PNG, WEBP. Max size: 5 MB.
          </small>
        </label>

        {(imagePreviewUrl || form.jobImageUrl) && (
          <div className="admin-job-image-preview-wrap">
            <img
              src={imagePreviewUrl || form.jobImageUrl}
              alt="Job preview"
              className="admin-job-image-preview"
            />
            <div className="admin-form-actions" style={{ gap: 8 }}>
              <button type="button" className="button ghost" onClick={clearJobImage}>
                Remove image
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="admin-form-section">
        <h3>5. Location *</h3>
        <label className="admin-field" style={{ position: "relative" }}>
          <span>Address (start typing — we’ll find the spot for you)</span>
          <input
            value={form.addressText}
            onChange={(e) => {
              update("addressText", e.target.value);
              setShowSuggestions(true);
            }}
            onFocus={() => setShowSuggestions(true)}
            onBlur={() => {
              // Delay so a click on a suggestion still registers.
              setTimeout(() => setShowSuggestions(false), 200);
            }}
            placeholder="e.g. Plot 14, Hitech City Main Rd, Madhapur, Hyderabad"
            autoComplete="off"
            required
          />
          {showSuggestions && (suggestions.length > 0 || searchingLocation) && (
            <ul className="admin-autocomplete-list">
              {searchingLocation && suggestions.length === 0 && (
                <li className="admin-autocomplete-empty">Searching…</li>
              )}
              {suggestions.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    className="admin-autocomplete-item"
                    onMouseDown={(event) => {
                      event.preventDefault();
                      pickSuggestion(item);
                    }}
                  >
                    <strong>{item.label}</strong>
                    <small>{item.fullAddress}</small>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {form.latitude && form.longitude && !showCoordOverride && (
            <small style={{ marginTop: 6, opacity: 0.7 }}>
              ✓ Coordinates resolved: {Number(form.latitude).toFixed(5)}, {Number(form.longitude).toFixed(5)}
            </small>
          )}
        </label>
        {hasSelectedCoordinates && (
          <div className="admin-location-pin-panel">
            <iframe
              className="admin-location-map-preview"
              title="Selected job location map preview"
              src={mapPreviewUrl(selectedLatitude, selectedLongitude)}
              loading="lazy"
            />
            <div className="admin-pin-controls" aria-label="Adjust selected job coordinates">
              <button type="button" title="Move pin north" aria-label="Move pin north" onClick={() => nudgePin(0.0001, 0)}>
                ↑
              </button>
              <button type="button" title="Move pin west" aria-label="Move pin west" onClick={() => nudgePin(0, -0.0001)}>
                ←
              </button>
              <button type="button" title="Move pin east" aria-label="Move pin east" onClick={() => nudgePin(0, 0.0001)}>
                →
              </button>
              <button type="button" title="Move pin south" aria-label="Move pin south" onClick={() => nudgePin(-0.0001, 0)}>
                ↓
              </button>
            </div>
            <small>
              Exact pin: {selectedLatitude.toFixed(6)}, {selectedLongitude.toFixed(6)}
            </small>
          </div>
        )}
        <div className="admin-form-actions" style={{ gap: 8 }}>
          <button
            type="button"
            className="button ghost"
            onClick={() => setShowCoordOverride((v) => !v)}
          >
            {showCoordOverride ? "Hide manual coordinates" : "Override coordinates manually"}
          </button>
        </div>
        {showCoordOverride && (
          <div className="admin-form-grid">
            <label className="admin-field">
              <span>Latitude *</span>
              <input value={form.latitude} onChange={(e) => update("latitude", e.target.value)} placeholder="17.4504" required />
            </label>
            <label className="admin-field">
              <span>Longitude *</span>
              <input value={form.longitude} onChange={(e) => update("longitude", e.target.value)} placeholder="78.3823" required />
            </label>
            <div className="admin-field">
              <span>&nbsp;</span>
              <button type="button" className="button ghost" onClick={handleGeocode} disabled={geocoding}>
                {geocoding ? "Geocoding…" : "📍 Re-geocode address"}
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="admin-form-section">
        <h3>6. Description & contact</h3>
        <label className="admin-field">
          <span>Description *</span>
          <textarea
            rows={5}
            value={form.description}
            onChange={(e) => update("description", e.target.value)}
            placeholder="Describe the role, responsibilities, and any onsite specifics..."
            required
          />
        </label>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Contact phone *</span>
            <input type="tel" value={form.contactNumber} onChange={(e) => update("contactNumber", e.target.value)} placeholder="+91 9876543210" required />
          </label>
        </div>
      </div>

      <div className="admin-form-actions">
        <button type="submit" className="button" disabled={submitting || uploadingImage}>
          {uploadingImage ? "Uploading image..." : submitting ? "Posting..." : "Publish job"}
        </button>
        <button type="button" className="button ghost" onClick={() => router.push("/admin/jobs")}>Cancel</button>
      </div>
    </form>
  );
}
