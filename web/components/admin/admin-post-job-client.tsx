"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useMemo, useState } from "react";
import { addDoc, collection, serverTimestamp } from "firebase/firestore";

import { getFirebaseServices } from "@/lib/firebase/client";

const JOB_CATEGORIES = [
  "Delivery",
  "Driver",
  "Cook",
  "Maid",
  "Warehouse",
  "Security",
  "Electrician",
  "Plumber",
  "Carpenter",
  "Painter",
  "Gardener",
  "Cleaner",
  "Helper",
  "Office Boy",
  "Receptionist",
  "Data Entry",
  "Telecaller",
  "Sales",
  "Other"
];

const PAY_TYPES = ["Monthly", "Weekly", "Daily", "Hourly", "Fixed"];
const SHIFTS = ["Day Shift", "Night Shift", "Flexible", "Rotational", "Morning", "Evening"];

export function AdminPostJobClient() {
  const services = useMemo(() => getFirebaseServices(), []);
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [form, setForm] = useState({
    title: "",
    companyName: "",
    location: "",
    description: "",
    category: "",
    payAmount: "",
    payType: "Monthly",
    shift: "Day Shift",
    vacancies: "1",
    requirements: "",
    contactPhone: "",
    contactEmail: ""
  });

  function updateField(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!services) {
      setError("Firebase is not configured.");
      return;
    }

    if (!form.title.trim() || !form.companyName.trim() || !form.location.trim()) {
      setError("Title, company, and location are required.");
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      await addDoc(collection(services.db, "jobs"), {
        title: form.title.trim(),
        companyName: form.companyName.trim(),
        location: form.location.trim(),
        description: form.description.trim(),
        category: form.category || "Other",
        payAmount: form.payAmount.trim(),
        payType: form.payType,
        shift: form.shift,
        vacancies: Number(form.vacancies) || 1,
        requirements: form.requirements.trim(),
        contactPhone: form.contactPhone.trim(),
        contactEmail: form.contactEmail.trim(),
        isActive: true,
        applicationCount: 0,
        createdAt: serverTimestamp(),
        postedBy: "admin",
        source: "admin-console"
      });

      setSuccess(true);
      setForm({
        title: "",
        companyName: "",
        location: "",
        description: "",
        category: "",
        payAmount: "",
        payType: "Monthly",
        shift: "Day Shift",
        vacancies: "1",
        requirements: "",
        contactPhone: "",
        contactEmail: ""
      });
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Failed to post job.");
    } finally {
      setSubmitting(false);
    }
  }

  if (success) {
    return (
      <div className="admin-success-card">
        <div className="admin-success-icon">✓</div>
        <h2>Job Posted Successfully!</h2>
        <p>The job is now live and visible to workers.</p>
        <div className="admin-success-actions">
          <button className="button" onClick={() => setSuccess(false)}>
            Post Another Job
          </button>
          <button className="button ghost" onClick={() => router.push("/admin/jobs")}>
            View All Jobs
          </button>
        </div>
      </div>
    );
  }

  return (
    <form className="admin-post-form" onSubmit={handleSubmit}>
      {error && <div className="admin-error">{error}</div>}

      <div className="admin-form-section">
        <h3>Job Details</h3>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Job Title *</span>
            <input
              value={form.title}
              onChange={(e) => updateField("title", e.target.value)}
              placeholder="e.g. Delivery Partner"
              required
            />
          </label>
          <label className="admin-field">
            <span>Company Name *</span>
            <input
              value={form.companyName}
              onChange={(e) => updateField("companyName", e.target.value)}
              placeholder="e.g. QuickDrop Logistics"
              required
            />
          </label>
          <label className="admin-field">
            <span>Location *</span>
            <input
              value={form.location}
              onChange={(e) => updateField("location", e.target.value)}
              placeholder="e.g. Madhapur, Hyderabad"
              required
            />
          </label>
          <label className="admin-field">
            <span>Category</span>
            <select value={form.category} onChange={(e) => updateField("category", e.target.value)}>
              <option value="">Select category</option>
              {JOB_CATEGORIES.map((cat) => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="admin-form-section">
        <h3>Compensation & Schedule</h3>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Pay Amount</span>
            <input
              value={form.payAmount}
              onChange={(e) => updateField("payAmount", e.target.value)}
              placeholder="e.g. 18000-26000"
            />
          </label>
          <label className="admin-field">
            <span>Pay Type</span>
            <select value={form.payType} onChange={(e) => updateField("payType", e.target.value)}>
              {PAY_TYPES.map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </label>
          <label className="admin-field">
            <span>Shift</span>
            <select value={form.shift} onChange={(e) => updateField("shift", e.target.value)}>
              {SHIFTS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>
          <label className="admin-field">
            <span>Vacancies</span>
            <input
              type="number"
              min="1"
              value={form.vacancies}
              onChange={(e) => updateField("vacancies", e.target.value)}
            />
          </label>
        </div>
      </div>

      <div className="admin-form-section">
        <h3>Additional Info</h3>
        <label className="admin-field">
          <span>Job Description</span>
          <textarea
            rows={4}
            value={form.description}
            onChange={(e) => updateField("description", e.target.value)}
            placeholder="Describe the role, responsibilities, and benefits..."
          />
        </label>
        <label className="admin-field">
          <span>Requirements</span>
          <textarea
            rows={3}
            value={form.requirements}
            onChange={(e) => updateField("requirements", e.target.value)}
            placeholder="Skills, experience, or documents needed..."
          />
        </label>
        <div className="admin-form-grid">
          <label className="admin-field">
            <span>Contact Phone</span>
            <input
              type="tel"
              value={form.contactPhone}
              onChange={(e) => updateField("contactPhone", e.target.value)}
              placeholder="e.g. +91 9876543210"
            />
          </label>
          <label className="admin-field">
            <span>Contact Email</span>
            <input
              type="email"
              value={form.contactEmail}
              onChange={(e) => updateField("contactEmail", e.target.value)}
              placeholder="e.g. hr@company.com"
            />
          </label>
        </div>
      </div>

      <div className="admin-form-actions">
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? "Posting..." : "Post Job"}
        </button>
        <button type="button" className="button ghost" onClick={() => router.push("/admin/jobs")}>
          Cancel
        </button>
      </div>
    </form>
  );
}
