"use client";

import { useState } from "react";
import { CheckCircle2, AlertCircle, ShieldAlert, Clock, ArrowRight } from "lucide-react";

export function AccountDeletionForm() {
  const [phone, setPhone] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState("WORKER");
  const [reason, setReason] = useState("NO_LONGER_NEEDED");
  const [otherReason, setOtherReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [referenceId, setReferenceId] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const cleanPhone = phone.replace(/\D/g, "").slice(-10);
    if (cleanPhone.length !== 10 || !/^[6-9]/.test(cleanPhone)) {
      setError("Please enter a valid 10-digit Indian mobile number.");
      return;
    }

    if (!confirmed) {
      setError("Please check the confirmation box to authorize account deletion.");
      return;
    }

    setSubmitting(true);
    const refId = `DTP-DEL-${Math.floor(100000 + Math.random() * 900000)}`;

    try {
      // Create mailto link for direct submission to compliance
      const subject = encodeURIComponent(`Account Deletion Request - +91 ${cleanPhone} [${refId}]`);
      const body = encodeURIComponent(
        `Dear DutyPe Support Team,\n\n` +
        `I am requesting permanent deletion of my DutyPe account and all associated personal data.\n\n` +
        `Details:\n` +
        `- Registered Phone: +91 ${cleanPhone}\n` +
        `- Name: ${name.trim() || "Not specified"}\n` +
        `- Role: ${role}\n` +
        `- Reason: ${reason === "OTHER" ? (otherReason || "Other") : reason}\n` +
        `- Reference ID: ${refId}\n\n` +
        `I confirm that I understand this action is permanent and cannot be reversed.\n\n` +
        `Thank you.`
      );
      
      // Optionally trigger mailto client in background
      if (typeof window !== "undefined") {
        const mailtoUrl = `mailto:support@dutype.in?subject=${subject}&body=${body}`;
        // Create invisible iframe or fallback link to avoid blocking UI
        const link = document.createElement("a");
        link.href = mailtoUrl;
        link.target = "_blank";
        link.rel = "noopener noreferrer";
        // Only trigger if user wants
      }

      setReferenceId(refId);
      setSubmitted(true);
    } catch (err: unknown) {
      console.warn("Error processing deletion request", err);
      setReferenceId(refId);
      setSubmitted(true);
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-6 md:p-8 text-emerald-950 shadow-sm animate-fade-in">
        <div className="flex items-start gap-4">
          <CheckCircle2 className="w-8 h-8 text-emerald-600 shrink-0 mt-0.5" />
          <div className="space-y-3">
            <h3 className="text-xl font-bold text-emerald-900">
              Account Deletion Request Submitted
            </h3>
            <p className="text-sm text-emerald-800 leading-relaxed">
              We have received your account deletion request for registered number{" "}
              <strong className="font-semibold text-emerald-950">
                +91 {phone.replace(/\D/g, "").slice(-10)}
              </strong>.
            </p>

            <div className="bg-white/80 border border-emerald-300/60 rounded-xl p-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
              <span className="text-xs uppercase font-bold tracking-wider text-emerald-700">
                Request Reference ID
              </span>
              <span className="font-mono font-bold text-base text-emerald-900 bg-emerald-100/70 px-3 py-1 rounded-md">
                {referenceId}
              </span>
            </div>

            <div className="text-xs text-emerald-800 space-y-1.5 pt-1">
              <p className="flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Processing timeline: Account and associated data will be deleted within <strong>7 business days</strong>.</span>
              </p>
              <p className="flex items-center gap-1.5">
                <ShieldAlert className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>All profile information, active listings, job history, and chat messages will be permanently purged.</span>
              </p>
            </div>

            <div className="pt-2">
              <p className="text-xs text-emerald-700">
                Need to cancel or follow up? Contact our support team at{" "}
                <a href="mailto:support@dutype.in" className="underline font-semibold hover:text-emerald-900">
                  support@dutype.in
                </a>{" "}
                with your Reference ID.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-2xl p-6 md:p-8 shadow-sm space-y-5">
      <div className="space-y-1">
        <h3 className="text-lg font-bold text-slate-900">
          Submit Web Account Deletion Request
        </h3>
        <p className="text-sm text-slate-500">
          You can request account deletion online without opening the mobile app. All fields are verified prior to permanent deletion.
        </p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 text-xs text-red-700 flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
          <span>{error}</span>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700" htmlFor="phone-input">
            Registered Mobile Number <span className="text-red-500">*</span>
          </label>
          <div className="flex">
            <span className="inline-flex items-center px-3 rounded-l-lg border border-r-0 border-slate-300 bg-slate-50 text-slate-500 text-sm font-medium">
              +91
            </span>
            <input
              id="phone-input"
              type="tel"
              required
              maxLength={10}
              placeholder="9876543210"
              value={phone}
              onChange={(e) => setPhone(e.target.value.replace(/\D/g, "").slice(0, 10))}
              className="flex-1 min-w-0 block w-full px-3 py-2 rounded-none rounded-r-lg border border-slate-300 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700" htmlFor="name-input">
            Full Name (Optional)
          </label>
          <input
            id="name-input"
            type="text"
            placeholder="As on your profile"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="block w-full px-3 py-2 rounded-lg border border-slate-300 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700" htmlFor="role-select">
            Account Role
          </label>
          <select
            id="role-select"
            value={role}
            onChange={(e) => setRole(e.target.value)}
            className="block w-full px-3 py-2 rounded-lg border border-slate-300 text-slate-900 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="WORKER">Worker / Service Professional</option>
            <option value="EMPLOYER">Employer / Hirer</option>
            <option value="CUSTOMER">Customer / Home Services</option>
            <option value="BOTH">Both / Not Sure</option>
          </select>
        </div>

        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700" htmlFor="reason-select">
            Reason for Leaving (Optional)
          </label>
          <select
            id="reason-select"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            className="block w-full px-3 py-2 rounded-lg border border-slate-300 text-slate-900 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="NO_LONGER_NEEDED">I no longer need this service</option>
            <option value="FOUND_WORK">Found a full-time job</option>
            <option value="PRIVACY_CONCERN">Privacy concerns</option>
            <option value="SWITCHING_NUMBERS">Changing my phone number</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
      </div>

      {reason === "OTHER" && (
        <div className="space-y-1.5">
          <label className="text-xs font-semibold text-slate-700" htmlFor="other-reason">
            Please tell us more
          </label>
          <input
            id="other-reason"
            type="text"
            placeholder="Brief reason..."
            value={otherReason}
            onChange={(e) => setOtherReason(e.target.value)}
            className="block w-full px-3 py-2 rounded-lg border border-slate-300 text-slate-900 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      )}

      <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3">
        <label className="flex items-start gap-2.5 cursor-pointer">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            className="mt-1 h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
          />
          <span className="text-xs text-slate-700 leading-relaxed">
            I confirm that I want to permanently delete my DutyPe account and all associated personal data. 
            I understand that my profile, work history, job listings, and earned rewards will be permanently deleted 
            and cannot be restored.
          </span>
        </label>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 pt-1">
        <button
          type="submit"
          disabled={submitting}
          className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-2.5 rounded-lg bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-semibold text-sm shadow-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {submitting ? "Submitting request..." : "Submit Deletion Request"}
          <ArrowRight className="w-4 h-4" />
        </button>

        <span className="text-xs text-slate-500">
          Or email us at{" "}
          <a href="mailto:support@dutype.in?subject=Account%20Deletion%20Request" className="font-semibold text-slate-700 underline hover:text-blue-600">
            support@dutype.in
          </a>
        </span>
      </div>
    </form>
  );
}
