"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useRef, useState } from "react";

import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

// Mirrors the Android app's PostJobScreen enums (jobType / salaryType / urgency / shift / experience / gender).
const JOB_TYPES: { value: string; label: string }[] = [
  { value: "DELIVERY", label: "Delivery" },
  { value: "DRIVER", label: "Driver" },
  { value: "COOK", label: "Cook" },
  { value: "MAID", label: "Maid / House Help" },
  { value: "WAREHOUSE", label: "Warehouse" },
  { value: "SECURITY", label: "Security Guard" },
  { value: "ELECTRICIAN", label: "Electrician" },
  { value: "PLUMBER", label: "Plumber" },
  { value: "CARPENTER", label: "Carpenter" },
  { value: "PAINTER", label: "Painter" },
  { value: "GARDENER", label: "Gardener" },
  { value: "CLEANER", label: "Cleaner" },
  { value: "HELPER", label: "Helper" },
  { value: "OFFICE_BOY", label: "Office Boy" },
  { value: "RECEPTIONIST", label: "Receptionist" },
  { value: "DATA_ENTRY", label: "Data Entry" },
  { value: "TELECALLER", label: "Telecaller" },
  { value: "SALES", label: "Sales" },
  { value: "OTHER", label: "Other" }
];

// Keyword → category. First match wins. Ordering matters (specific before generic).
const CATEGORY_KEYWORDS: Array<{ category: string; words: string[] }> = [
  { category: "DELIVERY", words: ["delivery", "courier", "rider", "swiggy", "zomato", "dunzo", "shipment", "parcel"] },
  { category: "DRIVER", words: ["driver", "chauffeur", "uber", "ola", "cab", "taxi", "auto driver", "truck"] },
  { category: "COOK", words: ["cook", "chef", "kitchen", "tandoor", "biryani", "cuisine"] },
  { category: "MAID", words: ["maid", "house help", "housekeep", "babysit", "nanny", "ayah"] },
  { category: "WAREHOUSE", words: ["warehouse", "stock", "loader", "packer", "godown", "inventory"] },
  { category: "SECURITY", words: ["security", "guard", "watchman", "bouncer"] },
  { category: "ELECTRICIAN", words: ["electrician", "wiring", "electrical"] },
  { category: "PLUMBER", words: ["plumber", "plumbing", "pipe"] },
  { category: "CARPENTER", words: ["carpenter", "woodwork", "furniture maker"] },
  { category: "PAINTER", words: ["painter", "painting"] },
  { category: "GARDENER", words: ["gardener", "garden", "landscap", "horticult"] },
  { category: "CLEANER", words: ["cleaner", "cleaning", "janitor", "sweeper", "housekeeper"] },
  { category: "OFFICE_BOY", words: ["office boy", "office assistant", "peon"] },
  { category: "RECEPTIONIST", words: ["receptionist", "front desk"] },
  { category: "DATA_ENTRY", words: ["data entry", "typing", "computer operator"] },
  { category: "TELECALLER", words: ["telecaller", "tele caller", "call center", "callcenter", "bpo"] },
  { category: "SALES", words: ["sales", "salesman", "sales executive", "marketing", "field sales"] },
  { category: "HELPER", words: ["helper", "assistant", "labour", "labor", "worker"] }
];

function detectCategoryFromTitle(title: string): string | null {
  const t = title.toLowerCase();
  for (const { category, words } of CATEGORY_KEYWORDS) {
    for (const w of words) {
      if (t.includes(w)) return category;
    }
  }
  return null;
}

type LocationSuggestion = {
  display_name: string;
  lat: string;
  lon: string;
};

const SALARY_TYPES = ["HOURLY", "DAILY", "WEEKLY", "MONTHLY", "FIXED"];
const SHIFTS = ["Flexible", "Day Shift", "Night Shift", "Rotational", "Morning", "Evening"];
// BUG #5 FIX: Stored values stay LOW/MEDIUM/HIGH (Firestore rules whitelist
// these), but the user-facing labels now match the Android app's JobUrgency
// enum (`Today` / `Within 3 days` / `Within 1 week`).
const URGENCY: Array<{ value: string; label: string }> = [
  { value: "HIGH", label: "🔴 Today" },
  { value: "MEDIUM", label: "🟡 Within 3 days" },
  { value: "LOW", label: "🟢 Within 1 week" }
];
const GENDERS = ["Any", "Male", "Female"];
const EXPERIENCE_LEVELS = [
  "No Experience Required",
  "Fresher",
  "1-3 years",
  "3-5 years",
  "5+ years"
];
const COMMON_BENEFITS = [
  "Food",
  "Transport",
  "Accommodation",
  "Insurance",
  "Healthcare",
  "Phone Allowance",
  "Loan",
  "Bonus",
  "Provident Fund"
];

const initialForm = {
  title: "",
  companyName: "",
  jobType: "OTHER",
  salary: "",
  salaryType: "DAILY",
  addressText: "",
  latitude: "",
  longitude: "",
  urgency: "MEDIUM",
  description: "",
  contactNumber: "",
  whatsappNumber: "",
  gender: "Any",
  experienceRequired: "No Experience Required",
  shiftTiming: "Flexible",
  workingHours: "",
  vacancies: "1",
  benefits: [] as string[],
  customBenefits: "",
  expiresInDays: "30",
  whatsappSameAsPhone: true
};

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
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const abortRef = useRef<AbortController | null>(null);

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
        const url = `https://nominatim.openstreetmap.org/search?format=json&addressdetails=0&limit=5&countrycodes=in&q=${encodeURIComponent(query)}`;
        const response = await fetch(url, {
          signal: controller.signal,
          headers: { "Accept-Language": "en" }
        });
        const results = (await response.json()) as LocationSuggestion[];
        setSuggestions(Array.isArray(results) ? results.slice(0, 5) : []);
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
      addressText: item.display_name,
      latitude: item.lat,
      longitude: item.lon
    }));
    setSuggestions([]);
    setShowSuggestions(false);
  }

  function toggleBenefit(value: string) {
    setForm((prev) => ({
      ...prev,
      benefits: prev.benefits.includes(value)
        ? prev.benefits.filter((b) => b !== value)
        : [...prev.benefits, value]
    }));
  }

  async function handleGeocode() {
    if (!form.addressText.trim()) {
      setError("Enter an address before geocoding.");
      return;
    }
    try {
      setGeocoding(true);
      setError(null);
      // Free Nominatim geocoder — same provider many web apps use.
      const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(form.addressText.trim())}`;
      const response = await fetch(url, { headers: { "Accept-Language": "en" } });
      const results = (await response.json()) as Array<{ lat: string; lon: string; display_name: string }>;
      if (!results.length) {
        setError("No coordinates found for that address. Paste lat/lng manually.");
        return;
      }
      const { lat, lon } = results[0];
      setForm((prev) => ({ ...prev, latitude: lat, longitude: lon }));
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
    const salaryNumber = Number(form.salary);
    if (!Number.isFinite(salaryNumber) || salaryNumber <= 0) {
      setError("Salary must be a positive number.");
      return;
    }
    const lat = Number(form.latitude);
    const lng = Number(form.longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng) || (lat === 0 && lng === 0)) {
      setError("Latitude and longitude are required. Click 'Geocode address' or paste coordinates.");
      return;
    }

    const benefits = [
      ...form.benefits,
      ...form.customBenefits.split(/[,\n]/).map((s) => s.trim()).filter(Boolean)
    ];

    try {
      setSubmitting(true);
      const response = await adminApiFetch("/api/admin/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          companyName: form.companyName.trim(),
          jobType: form.jobType,
          salary: salaryNumber,
          salaryType: form.salaryType,
          addressText: form.addressText.trim(),
          latitude: lat,
          longitude: lng,
          urgency: form.urgency,
          description: form.description.trim(),
          contactNumber: form.contactNumber.trim(),
          whatsappNumber: (form.whatsappSameAsPhone ? form.contactNumber : form.whatsappNumber).trim() || undefined,
          gender: form.gender,
          experienceRequired: form.experienceRequired,
          shiftTiming: form.shiftTiming,
          workingHours: form.workingHours.trim() || undefined,
          vacancies: Number(form.vacancies) || 1,
          benefits,
          expiresInDays: Number(form.expiresInDays) || 30
        })
      });

      const payload = (await response.json()) as { error?: string; jobId?: string };
      if (!response.ok) throw new Error(payload.error || "Failed to post job.");

      setSuccess(payload.jobId ?? "Job posted.");
      setForm(initialForm);
      setJobTypeManuallySet(false);
      setShowCoordOverride(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to post job.");
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <div className="admin-success-card">
        <div className="admin-success-icon">✓</div>
        <h2>Job Posted Successfully!</h2>
        <p>The job is now live in <code>jobmetadata</code> + <code>job_details</code> and visible in the app.</p>
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
              {JOB_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Vacancies *</span>
            {/* BUG #8 FIX: avoid wheel-scroll mutating the number. */}
            <input
              type="number"
              min={1}
              max={1000}
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
            <span>Salary amount *</span>
            {/* BUG #8 FIX: <input type="number"> increments/decrements on
                mouse-wheel scroll when focused, which silently turned 18000
                into 17998 etc. Blur on wheel disables that behavior without
                losing the numeric keypad / validation. */}
            <input
              type="number"
              min={1}
              value={form.salary}
              onChange={(e) => update("salary", e.target.value)}
              onWheel={(e) => (e.currentTarget as HTMLInputElement).blur()}
              placeholder="e.g. 18000"
              required
            />
          </label>
          <label className="admin-field">
            <span>Salary type *</span>
            <select value={form.salaryType} onChange={(e) => update("salaryType", e.target.value)}>
              {SALARY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Shift timing</span>
            <select value={form.shiftTiming} onChange={(e) => update("shiftTiming", e.target.value)}>
              {SHIFTS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Working hours (optional)</span>
            <input value={form.workingHours} onChange={(e) => update("workingHours", e.target.value)} placeholder="e.g. 9 AM – 6 PM" />
          </label>
          <label className="admin-field">
            <span>Expires in (days)</span>
            {/* BUG #8 FIX: avoid wheel-scroll mutating the number. */}
            <input
              type="number"
              min={1}
              max={60}
              value={form.expiresInDays}
              onChange={(e) => update("expiresInDays", e.target.value)}
              onWheel={(e) => (e.currentTarget as HTMLInputElement).blur()}
            />
          </label>
        </div>
        <div className="admin-field">
          <span>Urgency</span>
          <div className="admin-chip-row">
            {URGENCY.map((u) => (
              <button
                type="button"
                key={u.value}
                className={`admin-chip ${form.urgency === u.value ? "active" : ""}`}
                onClick={() => update("urgency", u.value)}
              >
                {u.label}
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
              {GENDERS.map((g) => <option key={g} value={g}>{g}</option>)}
            </select>
          </label>
          <label className="admin-field">
            <span>Experience required</span>
            <select value={form.experienceRequired} onChange={(e) => update("experienceRequired", e.target.value)}>
              {EXPERIENCE_LEVELS.map((e) => <option key={e} value={e}>{e}</option>)}
            </select>
          </label>
        </div>

        <div className="admin-field">
          <span>Benefits / perks</span>
          <div className="admin-chip-row">
            {COMMON_BENEFITS.map((b) => (
              <button
                type="button"
                key={b}
                className={`admin-chip ${form.benefits.includes(b) ? "active" : ""}`}
                onClick={() => toggleBenefit(b)}
              >
                {b}
              </button>
            ))}
          </div>
          <input
            value={form.customBenefits}
            onChange={(e) => update("customBenefits", e.target.value)}
            placeholder="Add custom benefits (comma separated)"
          />
        </div>
      </div>

      <div className="admin-form-section">
        <h3>4. Location *</h3>
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
                <li key={`${item.lat},${item.lon}`}>
                  <button
                    type="button"
                    className="admin-autocomplete-item"
                    onMouseDown={(event) => {
                      event.preventDefault();
                      pickSuggestion(item);
                    }}
                  >
                    📍 {item.display_name}
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
        <h3>5. Description & contact</h3>
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
          <label className="admin-field">
            <span>WhatsApp {form.whatsappSameAsPhone ? "(same as phone)" : "(optional)"}</span>
            <input
              type="tel"
              value={form.whatsappSameAsPhone ? form.contactNumber : form.whatsappNumber}
              onChange={(e) => update("whatsappNumber", e.target.value)}
              placeholder="+91 9876543210"
              disabled={form.whatsappSameAsPhone}
            />
          </label>
        </div>
        <label className="admin-field" style={{ flexDirection: "row", alignItems: "center", gap: 8 }}>
          <input
            type="checkbox"
            checked={form.whatsappSameAsPhone}
            onChange={(e) => update("whatsappSameAsPhone", e.target.checked)}
          />
          <span>WhatsApp same as phone</span>
        </label>
      </div>

      <div className="admin-form-actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? "Posting..." : "Publish job"}
        </button>
        <button type="button" className="button ghost" onClick={() => router.push("/admin/jobs")}>Cancel</button>
      </div>
    </form>
  );
}
