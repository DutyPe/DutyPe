import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Privacy Policy | DutyPe",
  description: "Official Privacy Policy for DutyPe - Hyperlocal Job Marketplace & Home Services Platform.",
};

export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 antialiased">
      {/* Top Header */}
      <header className="border-b border-slate-200 bg-white sticky top-0 z-20 shadow-sm">
        <div className="max-w-4xl mx-auto px-4 py-4 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2">
            <span className="text-xl font-black tracking-tight text-blue-600">DutyPe</span>
            <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 font-semibold border border-blue-200">Legal</span>
          </Link>
          <div className="flex items-center gap-4 text-sm font-medium text-slate-600">
            <Link href="/terms" className="hover:text-blue-600 transition-colors">Terms of Service</Link>
            <Link href="/accountdeletion" className="hover:text-red-600 transition-colors">Delete Account</Link>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-4xl mx-auto px-4 py-10">
        <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-10 shadow-sm space-y-8">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-1 rounded-md border border-blue-100">
              DPDP Act & IT Act Compliant
            </span>
            <h1 className="text-3xl md:text-4xl font-extrabold text-slate-900 mt-3 tracking-tight">
              DutyPe Privacy Policy
            </h1>
            <p className="text-sm text-slate-500 mt-2">
              Last Updated: October 5, 2026 | Effective Date: Immediately
            </p>
          </div>

          <p className="text-slate-600 leading-relaxed">
            Welcome to DutyPe (&ldquo;Platform&rdquo;, &ldquo;we&rdquo;, &ldquo;our&rdquo;, or &ldquo;us&rdquo;), operated by DutyPe Technologies. 
            DutyPe is a hyperlocal workforce hiring and on-demand home services marketplace connecting employers with workers, 
            and consumers with skilled home service professionals across India.
          </p>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              1. Information We Collect
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              We collect information to facilitate transparent job matching and prompt service delivery:
            </p>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>
                <strong>Identity & Profile Information:</strong> Full name, phone number, profile photo, job categories, skills, experience, and educational background provided voluntarily during profile creation.
              </li>
              <li>
                <strong>Geographical & Location Data:</strong> Precise or approximate location (GPS coordinates and geohash) to compute hyperlocal distance matrices (e.g. 5–15 km worker radii), calculate distance to job sites, and dispatch nearby service technicians.
              </li>
              <li>
                <strong>Voice Data:</strong> Temporary audio recordings when an employer or worker opts to use the voice-assisted job posting feature. Audio is converted to text via speech-to-text engines and is never used for biometric identification.
              </li>
              <li>
                <strong>Transaction & Wallet Information:</strong> Payment order IDs, subscription status, referral balances, and withdrawal UPI IDs. We do not store raw credit/debit card credentials or bank net banking passwords.
              </li>
              <li>
                <strong>Device & Diagnostic Data:</strong> IP address, device model, operating system version, Firebase Cloud Messaging (FCM) tokens for notifications, and anonymized crash analytics.
              </li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              2. How We Use Your Information
            </h2>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>Facilitating employer-to-worker matches and on-demand home service bookings.</li>
              <li>Enabling direct calling and contact between employers and applicants upon consent.</li>
              <li>Processing subscription credits, wallet referral incentives, and payout withdrawals.</li>
              <li>Sending transactional SMS, WhatsApp updates, and push notifications regarding job applications, interview calls, or service status.</li>
              <li>Preventing fraudulent spam postings, abuse, and illegal activities on our platform.</li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              3. Data Sharing & Third-Party Disclosure
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              <strong>We never sell, rent, or trade your personal information.</strong> Information is only shared under strict operational necessity:
            </p>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>
                <strong>Between Matched Parties:</strong> When an employer unlocks an applicant&apos;s contact details, the applicant&apos;s name and phone number are made visible to facilitate employment interviews.
              </li>
              <li>
                <strong>Cloud & Infrastructure Providers:</strong> Google Firebase (Authentication, Firestore, Cloud Storage, FCM), Razorpay/Cashfree (secure payments), and communication gateways (SMS/WhatsApp).
              </li>
              <li>
                <strong>Legal Requirements:</strong> When mandated by applicable Indian law, court order, or government investigative authority under the Information Technology Act 2000.
              </li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              4. Data Retention and Account Deletion
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              You maintain full ownership of your personal data. You may request immediate deletion of your account and associated records directly within the mobile application settings or via our dedicated web portal at{" "}
              <Link href="/accountdeletion" className="text-blue-600 font-semibold underline hover:text-blue-700">
                dutype.in/accountdeletion
              </Link>.
              Upon confirmation, your profile, resume details, and phone association are permanently purged within 48 hours, retaining only non-identifiable financial ledger entries as required by Indian taxation statutes.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              5. Security Measures
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              We implement industry-standard SSL/TLS end-to-end transport encryption, Firebase Security Rules enforcing least-privilege document access, and automated abuse detection to safeguard your data against unauthorized interception.
            </p>
          </section>

          <section className="space-y-3 bg-blue-50/60 rounded-xl p-5 border border-blue-100">
            <h2 className="text-lg font-bold text-slate-900">
              6. Grievance Officer & Contact Information
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              In accordance with the Information Technology (Intermediary Guidelines and Digital Media Ethics Code) Rules, 2021, and the Digital Personal Data Protection Act:
            </p>
            <div className="mt-3 text-sm text-slate-700 space-y-1">
              <p><strong>Grievance Officer:</strong> DutyPe Legal & Trust Team</p>
              <p>
                <strong>Official Email:</strong>{" "}
                <a href="mailto:support@dutype.in" className="text-blue-600 font-semibold hover:underline">
                  support@dutype.in
                </a>
              </p>
              <p><strong>Support Helpline:</strong> +91 85007 17800</p>
              <p><strong>Platform:</strong> DutyPe Technologies, Andhra Pradesh, India</p>
              <p className="text-xs text-slate-500 pt-2">
                All inquiries, concerns, or grievances are acknowledged within 24 hours and resolved within 48 to 72 business hours.
              </p>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
