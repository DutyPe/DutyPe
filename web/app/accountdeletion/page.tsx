import type { Metadata } from "next";

import { SiteShell } from "@/components/site-shell";
import { SITE_URL, SUPPORT_EMAIL, siteMeta } from "@/lib/public-site";

export const metadata: Metadata = {
  title: "Request Account Deletion – DutyPe",
  description:
    "Request deletion of your DutyPe account and all associated personal data. We process deletion requests within 7 business days.",
  alternates: { canonical: `${SITE_URL}/accountdeletion` },
  robots: { index: true, follow: true },
};

const deletionSteps = [
  {
    step: "1",
    title: "Send an email",
    detail: `Email ${SUPPORT_EMAIL} with the subject line "Account Deletion Request". Include the phone number registered with your DutyPe account.`,
  },
  {
    step: "2",
    title: "We verify your identity",
    detail:
      "Our team will verify the account ownership using the phone number provided. This takes 1–2 business days.",
  },
  {
    step: "3",
    title: "Data is deleted",
    detail:
      "Once verified, all personal data is permanently removed within 3–5 business days.",
  },
  {
    step: "4",
    title: "Confirmation sent",
    detail:
      "You will receive a final confirmation email once the deletion is complete.",
  },
];

const deletedData = [
  "Profile information (name, phone number, photo, skills, preferences)",
  "Job applications and application history",
  "Chat messages and communication history",
  "Referral data and earnings history",
  "Saved jobs and notification preferences",
  "Employer-posted jobs and associated applicant data (for employer accounts)",
];

const retainedData = [
  "Transaction records required by Indian tax and financial regulations",
  "Abuse and safety reports to protect the community",
  "Aggregated, anonymised analytics that cannot identify you",
];

export default function AccountDeletionPage() {
  return (
    <SiteShell plain>
      <article className="policy-page">
        <header className="policy-header">
          <p className="policy-eyebrow">Privacy</p>
          <h1>Request Account Deletion</h1>
          <p>
            You have the right to request deletion of your DutyPe account and personal data at any time.
            Send us an email and we will process your request.
          </p>
        </header>

        <div className="policy-content">
          <section className="policy-section">
            <h2>Deletion process</h2>
            <ol className="policy-list">
              {deletionSteps.map((s) => (
                <li key={s.step}>
                  <strong>{s.title}.</strong> {s.detail}
                </li>
              ))}
            </ol>
          </section>

          <section className="policy-section">
            <h2>What gets deleted</h2>
            <p>When your account is deleted, the following data is permanently removed.</p>
            <ul className="policy-list">
              {deletedData.map((d) => (
                <li key={d}>{d}</li>
              ))}
            </ul>
          </section>

          <section className="policy-section">
            <h2>What we may retain</h2>
            <p>Certain data may be retained for legal and compliance purposes.</p>
            <ul className="policy-list">
              {retainedData.map((d) => (
                <li key={d}>{d}</li>
              ))}
            </ul>
          </section>

          <section className="policy-section">
            <h2>Before you proceed</h2>
            <p>
              Account deletion is permanent and cannot be reversed. You will need to create a new account
              if you wish to use DutyPe again.
            </p>
            <p>
              If you have any pending payments or active disputes, those must be resolved before your
              account can be deleted. For employer accounts, all active job postings will be closed and
              applicants will be notified.
            </p>
          </section>

          <section className="policy-section policy-support-section">
            <h2>Request deletion</h2>
            <p>
              Send an email to {SUPPORT_EMAIL} with the subject &quot;Account Deletion Request&quot; and your
              registered phone number.
            </p>
            <a
              href={`mailto:${SUPPORT_EMAIL}?subject=Account%20Deletion%20Request`}
              className="policy-action"
            >
              Request deletion via email
            </a>
          </section>
        </div>

        <p className="policy-footer-note">Operated by {siteMeta.companyName}</p>
      </article>
    </SiteShell>
  );
}
