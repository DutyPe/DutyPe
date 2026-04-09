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
    <SiteShell>
      {/* Hero */}
      <section className="hero">
        <div className="hero-grid">
          <div className="hero-copy">
            <span className="eyebrow">🗑️ Privacy</span>
            <h1 className="headline">Request Account Deletion</h1>
            <p className="lede">
              You have the right to request deletion of your DutyPe account and
              personal data at any time. Send us an email and we will process
              your request.
            </p>
            <div className="button-row">
              <a
                href={`mailto:${SUPPORT_EMAIL}?subject=Account%20Deletion%20Request`}
                className="button"
              >
                Request Deletion via Email
              </a>
            </div>
          </div>

          <aside className="hero-panel hero-panel-enhanced">
            <span className="card-kicker">Key points</span>
            <h3>What to expect</h3>
            <ul className="detail-list detail-list-enhanced">
              <li>
                <strong>Processed within 7 business days</strong>
              </li>
              <li>
                <strong>All personal data permanently removed</strong>
              </li>
              <li>
                <strong>This action cannot be undone</strong>
              </li>
            </ul>
          </aside>
        </div>
      </section>

      {/* Steps */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">How it works</span>
            <h2>Deletion process</h2>
          </div>
          <p>Follow these steps to delete your account and data.</p>
        </div>

        <div className="section-grid">
          {deletionSteps.map((s) => (
            <article key={s.step} className="detail-panel">
              <span className="card-kicker">Step {s.step}</span>
              <h3>{s.title}</h3>
              <p>{s.detail}</p>
            </article>
          ))}
        </div>
      </section>

      {/* What gets deleted */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Data scope</span>
            <h2>What gets deleted</h2>
          </div>
          <p>
            When your account is deleted, the following data is permanently
            removed.
          </p>
        </div>

        <div className="section-grid">
          <article className="detail-panel tone-highlight">
            <span className="card-kicker">Permanently removed</span>
            <h3>Your data</h3>
            <ul className="detail-list detail-list-enhanced">
              {deletedData.map((d) => (
                <li key={d}>
                  <strong>{d}</strong>
                </li>
              ))}
            </ul>
          </article>

          <article className="detail-panel tone-neutral">
            <span className="card-kicker">Legal retention</span>
            <h3>What we may retain</h3>
            <p>
              Certain data may be retained for legal and compliance purposes:
            </p>
            <ul className="detail-list detail-list-enhanced">
              {retainedData.map((d) => (
                <li key={d}>
                  <strong>{d}</strong>
                </li>
              ))}
            </ul>
          </article>
        </div>
      </section>

      {/* Important notes */}
      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Important</span>
            <h2>Before you proceed</h2>
          </div>
        </div>

        <div className="section-grid">
          <article className="detail-panel tone-warning">
            <span className="card-kicker">Warning</span>
            <h3>Permanent action</h3>
            <p>
              Account deletion is permanent and cannot be reversed. You will
              need to create a new account if you wish to use DutyPe again.
            </p>
          </article>

          <article className="detail-panel">
            <span className="card-kicker">Note</span>
            <h3>Pending items</h3>
            <p>
              If you have any pending payments or active disputes, those must be
              resolved before your account can be deleted. For employer accounts,
              all active job postings will be closed and applicants will be
              notified.
            </p>
          </article>
        </div>
      </section>

      {/* CTA */}
      <div className="callout">
        <strong>Ready to delete your account?</strong>
        <span>
          Send an email to {SUPPORT_EMAIL} with the subject &quot;Account
          Deletion Request&quot; and your registered phone number.
        </span>
        <a
          href={`mailto:${SUPPORT_EMAIL}?subject=Account%20Deletion%20Request`}
          className="callout-action"
        >
          Request deletion
        </a>
      </div>

      {/* Company footer note */}
      <p
        style={{
          textAlign: "center",
          fontSize: "0.85rem",
          color: "var(--text-muted)",
          padding: "1.5rem 1rem 0",
        }}
      >
        Operated by {siteMeta.companyName}
      </p>
    </SiteShell>
  );
}
