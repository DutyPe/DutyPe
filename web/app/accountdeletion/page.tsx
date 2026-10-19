import type { Metadata } from "next";
import Link from "next/link";
import { 
  ShieldAlert, 
  Trash2, 
  CheckCircle2, 
  Clock, 
  Smartphone, 
  Mail, 
  FileText,
  AlertTriangle,
  HelpCircle
} from "lucide-react";

import { AccountDeletionForm } from "@/components/account-deletion-form";
import { SITE_URL, SUPPORT_EMAIL } from "@/lib/public-site";

export const dynamic = "force-static";

export const metadata: Metadata = {
  title: "Request Account & Data Deletion | DutyPe",
  description:
    "Official Account and Data Deletion page for DutyPe (com.dutype.app). Submit a web request to permanently delete your account, personal data, and profile history.",
  alternates: { canonical: `${SITE_URL}/accountdeletion` },
  robots: { index: true, follow: true },
};

export default function AccountDeletionPage() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 antialiased">
      {/* Top Header */}
      <header className="border-b border-slate-200 bg-white sticky top-0 z-20 shadow-sm">
        <div className="max-w-4xl mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2">
            <span className="text-xl font-black tracking-tight text-blue-600">DutyPe</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-red-50 text-red-700 font-semibold border border-red-200">
              Account Deletion
            </span>
          </Link>
          <div className="flex items-center gap-4 text-sm font-medium text-slate-600">
            <Link href="/privacy" className="hover:text-blue-600 transition-colors">Privacy Policy</Link>
            <Link href="/terms" className="hover:text-blue-600 transition-colors">Terms</Link>
            <Link href="/contact" className="hover:text-blue-600 transition-colors">Support</Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 py-8 md:py-12">
        <div className="space-y-8">
          {/* Hero Banner */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-10 shadow-sm space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-red-700 bg-red-50 px-2.5 py-1 rounded-md border border-red-100 flex items-center gap-1.5">
                <ShieldAlert className="w-3.5 h-3.5" />
                Google Play Policy & DPDP Act Compliant
              </span>
              <span className="text-xs font-medium text-slate-500 bg-slate-100 px-2.5 py-1 rounded-md">
                Package: com.dutype.app
              </span>
            </div>

            <h1 className="text-3xl md:text-4xl font-extrabold text-slate-900 tracking-tight">
              Request DutyPe Account & Data Deletion
            </h1>

            <p className="text-slate-600 leading-relaxed text-base">
              Under Google Play&apos;s User Data Policy and India&apos;s Digital Personal Data Protection (DPDP) Act, 
              users of <strong>DutyPe: Local Jobs &amp; Services</strong> (operated by <strong>KGPV INNOVATION SOLUTIONS PRIVATE LIMITED</strong>) 
              have the permanent right to delete their account, profile data, and all associated personal information.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
              <div className="flex items-center gap-2.5 bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs font-medium text-slate-700">
                <Clock className="w-4 h-4 text-blue-600 shrink-0" />
                <span>Processed within 7 business days</span>
              </div>
              <div className="flex items-center gap-2.5 bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs font-medium text-slate-700">
                <Trash2 className="w-4 h-4 text-red-600 shrink-0" />
                <span>Permanent profile &amp; history erasure</span>
              </div>
              <div className="flex items-center gap-2.5 bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs font-medium text-slate-700">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Web or In-App submission</span>
              </div>
            </div>
          </div>

          {/* Interactive Web Submission Form */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-slate-900">
                Method 1: Online Web Request Form
              </h2>
              <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                Instant • No App Needed
              </span>
            </div>
            <AccountDeletionForm />
          </div>

          {/* Method 2: In-App Deletion Steps */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm space-y-4">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <Smartphone className="w-5 h-5 text-blue-600" />
              Method 2: Directly Inside the DutyPe Android App
            </h2>
            <p className="text-sm text-slate-600">
              If you have the DutyPe app installed on your Android device, you can trigger instant account deletion directly from your account settings:
            </p>
            <ol className="list-decimal pl-5 space-y-2 text-sm text-slate-700">
              <li>Open the <strong>DutyPe</strong> app on your Android device.</li>
              <li>Tap the <strong>Profile</strong> tab in the bottom navigation bar.</li>
              <li>Scroll down to <strong>Account &amp; Security</strong> or <strong>Guidelines &amp; Safety</strong>.</li>
              <li>Tap <strong>Delete Account</strong> and confirm your request.</li>
              <li>Your session will terminate immediately and your account will be queued for permanent deletion.</li>
            </ol>
          </div>

          {/* Method 3: Email Support */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm space-y-3">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <Mail className="w-5 h-5 text-indigo-600" />
              Method 3: Direct Email to Support
            </h2>
            <p className="text-sm text-slate-600">
              You can also email our dedicated compliance team directly from your email client:
            </p>
            <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-sm text-slate-700 space-y-2">
              <p>
                <strong>Recipient:</strong>{" "}
                <a href={`mailto:${SUPPORT_EMAIL}`} className="text-blue-600 underline font-semibold">
                  {SUPPORT_EMAIL}
                </a>
              </p>
              <p>
                <strong>Subject line:</strong> Account Deletion Request - [Your Registered Phone Number]
              </p>
              <p>
                <strong>Body:</strong> Please include your registered 10-digit mobile number and mention whether you are registered as a Worker, Employer, or Customer.
              </p>
            </div>
          </div>

          {/* Data Disclosure: What is Deleted vs Retained */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm space-y-6">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <FileText className="w-5 h-5 text-slate-700" />
              Data Retention &amp; Permanent Deletion Policy
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Deleted Data */}
              <div className="bg-red-50/50 border border-red-200 rounded-xl p-5 space-y-3">
                <h3 className="font-bold text-red-950 flex items-center gap-2 text-base">
                  <Trash2 className="w-4 h-4 text-red-600" />
                  What Data is Permanently Deleted
                </h3>
                <ul className="list-disc pl-5 space-y-1.5 text-xs text-red-900/90 leading-relaxed">
                  <li><strong>Personal Profile:</strong> Full name, profile photo, bio, phone number, and account credentials.</li>
                  <li><strong>Work Profile &amp; Skills:</strong> Categories, skills, experience, certifications, and portfolio details.</li>
                  <li><strong>Location History:</strong> GPS coordinates, saved home/work addresses, and geohashes.</li>
                  <li><strong>Job Postings &amp; Applications:</strong> All employer job listings, job applications submitted by workers, and hiring records.</li>
                  <li><strong>Chat &amp; Communications:</strong> Chat messages, direct inquiries, and notification records.</li>
                  <li><strong>Referral Records:</strong> Referral codes, invite relationships, and promotional credits.</li>
                </ul>
              </div>

              {/* Retained Data */}
              <div className="bg-amber-50/50 border border-amber-200 rounded-xl p-5 space-y-3">
                <h3 className="font-bold text-amber-950 flex items-center gap-2 text-base">
                  <AlertTriangle className="w-4 h-4 text-amber-600" />
                  What Data is Retained &amp; Statutory Retention Period
                </h3>
                <ul className="list-disc pl-5 space-y-1.5 text-xs text-amber-900/90 leading-relaxed">
                  <li><strong>Financial &amp; Tax Records:</strong> Invoices, payment receipts, GST filing details, and bank payout logs are retained strictly to comply with Indian statutory accounting, GST laws, and Section 128 of the Companies Act, 2013.</li>
                  <li><strong>Retention Period:</strong> Preserved securely for the minimum legal period required under Indian financial regulations, after which all records are permanently shredded.</li>
                  <li><strong>Fraud &amp; Abuse Prevention:</strong> Hashes of verified fraud or safety violations may be retained solely to protect community safety and prevent repeat abuse.</li>
                </ul>
              </div>
            </div>
          </div>

          {/* Grievance Officer & Contact */}
          <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-8 shadow-sm space-y-3 text-sm text-slate-600">
            <h2 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <HelpCircle className="w-4 h-4 text-blue-600" />
              Grievance Officer &amp; Support Contact
            </h2>
            <p>
              In accordance with the Information Technology Act, 2000 and the Digital Personal Data Protection (DPDP) Act, 
              if you have any grievances or questions regarding account deletion, please contact:
            </p>
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 text-xs space-y-1 text-slate-700">
              <p><strong>Company:</strong> KGPV INNOVATION SOLUTIONS PRIVATE LIMITED</p>
              <p><strong>Brand / App:</strong> DutyPe: Local Jobs &amp; Services (com.dutype.app)</p>
              <p><strong>Grievance Email:</strong> <a href={`mailto:${SUPPORT_EMAIL}`} className="text-blue-600 underline">{SUPPORT_EMAIL}</a></p>
              <p><strong>Registered Address:</strong> Khammam, Telangana, India - 507001</p>
              <p><strong>Response Turnaround:</strong> Grievance tickets are acknowledged within 24 hours and resolved within 7 business days.</p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-6 text-center text-xs text-slate-500">
        <div className="max-w-4xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>&copy; {new Date().getFullYear()} DutyPe • KGPV INNOVATION SOLUTIONS PRIVATE LIMITED. All rights reserved.</span>
          <div className="flex items-center gap-4">
            <Link href="/privacy" className="hover:text-blue-600">Privacy Policy</Link>
            <Link href="/terms" className="hover:text-blue-600">Terms of Service</Link>
            <Link href="/accountdeletion" className="hover:text-blue-600 font-semibold text-slate-700">Account Deletion</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
