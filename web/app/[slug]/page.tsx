import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { SiteShell } from "@/components/site-shell";
import {
  PLAY_STORE_URL,
  SITE_URL,
  coreSeoKeywords,
  getKnownLegacySlugs,
  resolveLegacyPage,
  type LegacyPageBlock
} from "@/lib/public-site";

type Props = {
  params: {
    slug: string;
  };
};

const pageIcons: Record<string, string> = {
  privacy: "🔒",
  terms: "📜",
  refund: "💳",
  safety: "🛡️",
  contact: "📧",
  faq: "❓"
};

function renderBlock(block: LegacyPageBlock) {
  if (block.kind === "copy") {
    return (
      <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
        <span className="card-kicker">{block.title}</span>
        <h3>{block.title}</h3>
        {block.paragraphs.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
      </article>
    );
  }

  if (block.kind === "list") {
    return (
      <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
        <span className="card-kicker">{block.title}</span>
        <h3>{block.title}</h3>
        {block.intro ? <p>{block.intro}</p> : null}
        <ul className="detail-list detail-list-enhanced">
          {block.items.map((item) => (
            <li key={item}>
              <strong>{item}</strong>
            </li>
          ))}
        </ul>
      </article>
    );
  }

  if (block.kind === "faq") {
    return (
      <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
        <span className="card-kicker">{block.title}</span>
        <h3>{block.title}</h3>
        <div className="faq-stack">
          {block.items.map((item) => (
            <details key={item.question} className="faq-item faq-item-enhanced">
              <summary><strong>{item.question}</strong></summary>
              <p>{item.answer}</p>
            </details>
          ))}
        </div>
      </article>
    );
  }

  if (block.kind === "contact") {
    const contactIcons: Record<string, string> = {
      "General support": "💬",
      "Privacy concerns": "🔒",
      "Refunds and billing": "💳",
      "Legal": "⚖️",
      "Report abuse": "🚨",
      "Feedback": "💡"
    };

    return (
      <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
        <span className="card-kicker">{block.title}</span>
        <h3>{block.title}</h3>
        <div className="contact-grid contact-grid-enhanced">
          {block.items.map((item) => (
            <div key={item.label} className="contact-card contact-card-enhanced">
              <span className="contact-icon">{contactIcons[item.label] ?? "📧"}</span>
              <strong>{item.label}</strong>
              <p>{item.note}</p>
              {item.href ? (
                <a href={item.href} className="contact-link">{item.value}</a>
              ) : (
                <span className="contact-value">{item.value}</span>
              )}
            </div>
          ))}
        </div>
      </article>
    );
  }

  return (
    <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
      <span className="card-kicker">{block.title}</span>
      <h3>{block.title}</h3>
      {block.intro ? <p>{block.intro}</p> : null}
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th>{block.columns[0]}</th>
              <th>{block.columns[1]}</th>
            </tr>
          </thead>
          <tbody>
            {block.rows.map((row) => (
              <tr key={row[0]}>
                <td>{row[0]}</td>
                <td>{row[1]}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </article>
  );
}

export function generateMetadata({ params }: Props): Metadata {
  const page = resolveLegacyPage(params.slug);

  if (!page) {
    return {};
  }

  return {
    title: page.title,
    description: page.description,
    alternates: {
      canonical: `${SITE_URL}/${params.slug}`
    },
    robots: {
      index: true,
      follow: true
    },
    keywords: [
      ...coreSeoKeywords,
      ...params.slug
        .split("-")
        .filter(Boolean)
        .map((word) => `${word} jobs`),
      `${params.slug.replace(/-/g, " ")} near me`,
      "local hiring"
    ]
  };
}

export function generateStaticParams() {
  return getKnownLegacySlugs().map((slug) => ({ slug }));
}

export default function LegacyContentPage({ params }: Props) {
  const page = resolveLegacyPage(params.slug);

  if (!page) {
    notFound();
  }

  const icon = pageIcons[page.slug] ?? "📄";
  const isLegal = page.eyebrow === "Legal";
  const isSafety = page.eyebrow === "Safety";
  const isSupport = page.eyebrow === "Support";

  return (
    <SiteShell>
      <section className="hero">
        <div className="hero-grid">
          <div className="hero-copy">
            <div className="eyebrow-group">
              <span className="eyebrow">{icon} {page.eyebrow}</span>
              {isLegal && <span className="hero-note">Last updated: January 11, 2026</span>}
            </div>
            <h1 className="headline">{page.title}</h1>
            <p className="lede">{page.intro}</p>
            <div className="button-row">
              {page.ctaHref && page.ctaLabel ? (
                page.ctaHref.startsWith("mailto:") ? (
                  <a href={page.ctaHref} className="button">
                    {page.ctaLabel}
                  </a>
                ) : (
                  <Link href={page.ctaHref} className="button">
                    {page.ctaLabel}
                  </Link>
                )
              ) : null}
              {isSafety || isSupport ? (
                <a href={PLAY_STORE_URL} className="button ghost" target="_blank" rel="noopener noreferrer">
                  Download app
                </a>
              ) : (
                <Link href="/jobs" className="button ghost">
                  Browse jobs
                </Link>
              )}
            </div>
          </div>

          <aside className="hero-panel hero-panel-enhanced">
            <span className="card-kicker">Key highlights</span>
            <h3>{page.title}</h3>
            <ul className="detail-list detail-list-enhanced">
              {page.highlights.map((highlight) => (
                <li key={highlight}>
                  <strong>{highlight}</strong>
                </li>
              ))}
            </ul>
          </aside>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">{page.eyebrow}</span>
            <h2>{page.title}</h2>
          </div>
          <p>{page.description}</p>
        </div>

        <div className="section-grid legacy-grid">{page.blocks.map((block) => renderBlock(block))}</div>

        {page.ctaTitle && page.ctaCopy ? (
          <div className="callout" style={{ marginTop: "clamp(14px, 2vw, 18px)" }}>
            <strong>{page.ctaTitle}</strong>
            <span>{page.ctaCopy}</span>
            {page.ctaHref && page.ctaLabel ? (
              page.ctaHref.startsWith("mailto:") ? (
                <a href={page.ctaHref} className="callout-action">{page.ctaLabel}</a>
              ) : (
                <Link href={page.ctaHref} className="callout-action">{page.ctaLabel}</Link>
              )
            ) : null}
          </div>
        ) : null}
      </section>
    </SiteShell>
  );
}
