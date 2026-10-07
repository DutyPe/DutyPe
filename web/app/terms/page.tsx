import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Terms of Service | DutyPe",
  description: "Terms and conditions of use for DutyPe job marketplace and on-demand home services platform.",
};

export default function TermsOfServicePage() {
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
            <Link href="/privacy" className="hover:text-blue-600 transition-colors">Privacy Policy</Link>
            <Link href="/accountdeletion" className="hover:text-red-600 transition-colors">Delete Account</Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 py-10">
        <div className="bg-white rounded-2xl border border-slate-200 p-6 md:p-10 shadow-sm space-y-8">
          <div>
            <span className="text-xs font-bold uppercase tracking-wider text-blue-600 bg-blue-50 px-2.5 py-1 rounded-md border border-blue-100">
              Intermediary Guidelines & Terms
            </span>
            <h1 className="text-3xl md:text-4xl font-extrabold text-slate-900 mt-3 tracking-tight">
              DutyPe Terms of Service
            </h1>
            <p className="text-sm text-slate-500 mt-2">
              Last Updated: October 5, 2026 | Effective Date: Immediately
            </p>
          </div>

          <p className="text-slate-600 leading-relaxed">
            These Terms of Service (&ldquo;Terms&rdquo;) govern your access and use of the DutyPe mobile application, website, 
            and associated technology platforms operated by DutyPe Technologies (&ldquo;DutyPe&rdquo;, &ldquo;we&rdquo;, &ldquo;us&rdquo;). 
            By downloading, registering, or browsing DutyPe, you agree to be bound by these Terms.
          </p>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              1. Platform Nature & Intermediary Status
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              DutyPe operates strictly as an intermediary technology platform under Section 79 of the Information Technology Act, 2000. 
              DutyPe does not act as an employer, placement agency, contractor, or master of any worker or service provider. 
              The platform connects independent employers with job seekers and consumers with home service technicians. 
              All employment agreements, wages, work conditions, or direct agreements are established exclusively between the respective users.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              2. Eligibility & Account Responsibilities
            </h2>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>You must be at least 18 years of age to register or transact on DutyPe.</li>
              <li>You agree to provide true, current, and complete information during phone OTP verification and profile creation.</li>
              <li>You are solely responsible for all activities occurring under your authenticated credentials.</li>
              <li>Impersonation, posting false job listings, or submitting deceptive credentials is strictly prohibited and results in immediate account termination.</li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              3. Employer & Service Consumer Obligations
            </h2>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>Employers must not solicit any registration fees, security deposits, or advance training charges from job seekers. Asking candidates for money is grounds for immediate ban.</li>
              <li>Job listings must adhere to fair labor standards and Indian statutory regulations.</li>
              <li>Contact details obtained via DutyPe must be used solely for hiring or service delivery and must not be distributed to third-party telemarketers.</li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              4. Worker & Service Partner Conduct
            </h2>
            <ul className="list-disc pl-5 space-y-2 text-sm text-slate-600">
              <li>Workers and partners agree to fulfill agreed work commitments honestly, safely, and professionally.</li>
              <li>Workers must present valid identity verification when required by employers or clients.</li>
              <li>Any harassment, abusive behavior, or misconduct on or off duty will lead to permanent de-platforming and potential legal reporting.</li>
            </ul>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              5. Referral Program & Credits
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              DutyPe provides promotional referral rewards and subscription credits. Users may refer unlimited friends to earn incentives set by the platform administration. 
              Any automated bot referrals, self-referral rings, SIM cloning, or exploitation will result in forfeiture of balances and account suspension. Referral rewards are subject to administrative audit prior to UPI disbursement.
            </p>
          </section>

          <section className="space-y-3">
            <h2 className="text-xl font-bold text-slate-900 border-b border-slate-100 pb-2">
              6. Limitation of Liability
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              DutyPe Technologies shall not be held liable for any disputes regarding wages, work quality, personal injury, property damage, or disagreements arising outside our technology communications. Both parties are encouraged to verify references, work scope, and credentials before commencing work.
            </p>
          </section>

          <section className="space-y-3 bg-slate-50 rounded-xl p-5 border border-slate-200">
            <h2 className="text-lg font-bold text-slate-900">
              7. Dispute Resolution & Governing Law
            </h2>
            <p className="text-slate-600 leading-relaxed text-sm">
              These Terms shall be governed by and construed in accordance with the laws of India. Any legal dispute, arbitration, or proceedings shall be subject to the exclusive jurisdiction of the competent courts in Andhra Pradesh, India.
            </p>
            <div className="mt-4 pt-3 border-t border-slate-200 text-sm text-slate-700">
              <p>For questions or legal notifications regarding these terms, reach out to:</p>
              <p className="mt-1">
                <strong>Official Email:</strong>{" "}
                <a href="mailto:support@dutype.in" className="text-blue-600 font-semibold hover:underline">
                  support@dutype.in
                </a>
              </p>
              <p><strong>Platform:</strong> DutyPe Technologies, Andhra Pradesh, India</p>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
