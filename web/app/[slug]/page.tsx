import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { SiteShell } from "@/components/site-shell";
import { AppConversionCard } from "@/components/public/app-conversion-card";
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

const plainPolicySlugs = new Set(["privacy", "terms", "refund", "safety"]);

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

function renderPlainBlock(block: LegacyPageBlock) {
  if (block.kind === "copy") {
    return (
      <section key={block.title} className="policy-section">
        <h2>{block.title}</h2>
        {block.paragraphs.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
      </section>
    );
  }

  if (block.kind === "list") {
    return (
      <section key={block.title} className="policy-section">
        <h2>{block.title}</h2>
        {block.intro ? <p>{block.intro}</p> : null}
        <ul className="policy-list">
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
    );
  }

  if (block.kind === "faq") {
    return (
      <section key={block.title} className="policy-section">
        <h2>{block.title}</h2>
        <div className="policy-faq-list">
          {block.items.map((item) => (
            <details key={item.question} className="policy-faq-item">
              <summary>{item.question}</summary>
              <p>{item.answer}</p>
            </details>
          ))}
        </div>
      </section>
    );
  }

  if (block.kind === "contact") {
    return (
      <section key={block.title} className="policy-section">
        <h2>{block.title}</h2>
        <div className="policy-contact-list">
          {block.items.map((item) => (
            <p key={item.label}>
              <strong>{item.label}:</strong>{" "}
              {item.href ? <a href={item.href}>{item.value}</a> : item.value}
              {item.note ? <span> - {item.note}</span> : null}
            </p>
          ))}
        </div>
      </section>
    );
  }

  return (
    <section key={block.title} className="policy-section">
      <h2>{block.title}</h2>
      {block.intro ? <p>{block.intro}</p> : null}
      <div className="policy-table-wrap">
        <table className="policy-table">
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
    </section>
  );
}

export function generateMetadata({ params }: Props): Metadata {
  const page = resolveLegacyPage(params.slug);

  if (!page) {
    return {};
  }

  // Build keyword-rich terms from the slug
  const slugWords = params.slug.split("-").filter(Boolean);
  const slugPhrase = params.slug.replace(/-/g, " ");

  // Detect city pages and category pages for better keyword targeting
  const isCityPage = params.slug.startsWith("jobs-in-");
  const cityName = isCityPage ? slugWords.slice(2).join(" ") : null;

  const categoryMatch = slugWords.find((w) =>
    ["delivery", "driver", "maid", "cook", "helper", "cleaner", "security", "warehouse", "retail", "peon"].includes(w)
  );

  const locationKeywords = cityName
    ? [
        `jobs in ${cityName}`,
        `${cityName} jobs`,
        `jobs near me ${cityName}`,
        `part time jobs in ${cityName}`,
        `delivery jobs in ${cityName}`,
        `driver jobs in ${cityName}`,
        `maid jobs in ${cityName}`,
        `daily wage jobs ${cityName}`,
        `jobs in ${cityName} for freshers`,
        `jobs in ${cityName} 10th pass`,
        `night shift jobs ${cityName}`,
        `${cityName} local hiring`,
        `${cityName} job vacancy`
      ]
    : [];

  const categoryKeywords = categoryMatch
    ? [
        `${categoryMatch} jobs near me`,
        `${categoryMatch} jobs`,
        `${categoryMatch} jobs for freshers`,
        `part time ${categoryMatch} jobs`,
        `${categoryMatch} jobs no experience`,
        `${categoryMatch} salary`,
        `${categoryMatch} vacancy`
      ]
    : [];

  return {
    title: page.title,
    description: page.description,
    alternates: {
      canonical: `${SITE_URL}/${params.slug}`
    },
    robots: {
      index: true,
      follow: true,
      googleBot: {
        index: true,
        follow: true,
        "max-snippet": -1,
        "max-image-preview": "large"
      }
    },
    openGraph: {
      title: page.title,
      description: page.description,
      type: "website",
      url: `${SITE_URL}/${params.slug}`,
      siteName: "DutyPe",
      locale: "en_IN"
    },
    keywords: [
      ...coreSeoKeywords,
      ...locationKeywords,
      ...categoryKeywords,
      ...(page.seoKeywords ?? []),
      `${slugPhrase}`,
      `${slugPhrase} near me`,
      "local hiring",
      "apply free",
      "no middlemen",
      "verified employers"
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

  const isJobSeoPage =
    params.slug === "jobs-near-me" || params.slug.startsWith("jobs-in-") || page.eyebrow.includes("Jobs");
  const icon = pageIcons[page.slug] ?? (isJobSeoPage ? "💼" : "📄");
  const isLegal = page.eyebrow === "Legal";
  const isSafety = page.eyebrow === "Safety";
  const isSupport = page.eyebrow === "Support";

  // Build structured data for every page
  const faqBlocks = page.blocks.filter(
    (b): b is Extract<LegacyPageBlock, { kind: "faq" }> => b.kind === "faq"
  );
  const faqItems = faqBlocks.flatMap((b) => b.items);

  const structuredData: Record<string, unknown>[] = [
    {
      "@type": "BreadcrumbList",
      itemListElement: [
        { "@type": "ListItem", position: 1, name: "Home", item: SITE_URL },
        {
          "@type": "ListItem",
          position: 2,
          name: page.title,
          item: `${SITE_URL}/${params.slug}`
        }
      ]
    }
  ];

  if (faqItems.length > 0) {
    structuredData.push({
      "@type": "FAQPage",
      mainEntity: faqItems.map((faq) => ({
        "@type": "Question",
        name: faq.question,
        acceptedAnswer: { "@type": "Answer", text: faq.answer }
      }))
    });
  }

  const jsonLd = { "@context": "https://schema.org", "@graph": structuredData };

  if (plainPolicySlugs.has(page.slug)) {
    return (
      <SiteShell plain>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
        <article className="policy-page">
          <header className="policy-header">
            <p className="policy-eyebrow">{page.eyebrow}</p>
            <h1>{page.title}</h1>
            <p>{page.intro}</p>
            {isLegal ? <span>Last updated: January 11, 2026</span> : null}
          </header>

          <div className="policy-content">
            {page.blocks.map((block) => renderPlainBlock(block))}

            {page.ctaTitle && page.ctaCopy ? (
              <section className="policy-section policy-support-section">
                <h2>{page.ctaTitle}</h2>
                <p>{page.ctaCopy}</p>
                {page.ctaHref && page.ctaLabel ? (
                  page.ctaHref.startsWith("mailto:") ? (
                    <a href={page.ctaHref} className="policy-action">{page.ctaLabel}</a>
                  ) : (
                    <Link href={page.ctaHref} className="policy-action">{page.ctaLabel}</Link>
                  )
                ) : null}
              </section>
            ) : null}
          </div>
        </article>
      </SiteShell>
    );
  }

  return (
    <SiteShell>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <section className={`hero ${isJobSeoPage ? "local-seo-hero" : ""}`}>
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

      <section className={`section ${isJobSeoPage ? "local-seo-section" : ""}`}>
        <div className="section-header">
          <div>
            <span className="tag">{page.eyebrow}</span>
            <h2>{page.title}</h2>
          </div>
          <p>{page.description}</p>
        </div>

        <div className="section-grid legacy-grid">{page.blocks.map((block) => renderBlock(block))}</div>

        {isJobSeoPage ? (
          <AppConversionCard
            categoryOrCity={
              params.slug.startsWith("jobs-in-")
                ? params.slug.replace("jobs-in-", "").charAt(0).toUpperCase() + params.slug.replace("jobs-in-", "").slice(1)
                : page.title.replace("Jobs", "").trim()
            }
          />
        ) : null}

        {page.ctaTitle && page.ctaCopy ? (
          <div className="callout legacy-cta-callout">
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
