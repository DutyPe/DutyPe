import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { FaqContent } from "@/components/public/faq-content";
import { CityJobGuide, JobCategoryGuide } from "@/components/public/job-discovery";
import { LiveJobsSection } from "@/components/jobs/live-jobs-section";
import { emptyJobSearch } from "@/lib/jobs/public-listings";
import { SiteIcon } from "@/components/site-icon";
import { SiteShell } from "@/components/site-shell";
import {
  SUPPORT_EMAIL,
  getKnownLegacySlugs,
  getPublicPageMetadata,
  getPublicPageStructuredData,
  resolveLegacyPage,
  type LegacyPageBlock
} from "@/lib/public-site";

type Props = {
  params: {
    slug: string;
  };
};

export const revalidate = 60;

const supportPages = [
  { href: "/safety", label: "Safety", icon: "shield-check" },
  { href: "/contact", label: "Contact us", icon: "messages-square" },
  { href: "/faq", label: "FAQ", icon: "clipboard-list" }
];

const legalPages = [
  { href: "/privacy", label: "Privacy", icon: "shield-check" },
  { href: "/terms", label: "Terms", icon: "clipboard-list" },
  { href: "/refund", label: "Refunds", icon: "wallet" }
];

const contactIcons: Record<string, string> = {
  "General support": "messages-square",
  "Privacy concerns": "shield-check",
  "Refunds and billing": "wallet",
  Legal: "clipboard-list",
  "Report abuse": "shield-check",
  Feedback: "sparkles"
};

function sectionId(title: string) {
  return title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

function renderBlock(block: LegacyPageBlock) {
  if (block.kind === "copy") {
    return (
      <section key={block.title} id={sectionId(block.title)} tabIndex={-1} className={`resource-section resource-tone-${block.tone ?? "default"}`}>
        <h2>{block.title}</h2>
        {block.paragraphs.map((paragraph) => (
          <p key={paragraph}>{paragraph}</p>
        ))}
      </section>
    );
  }

  if (block.kind === "list") {
    return (
      <section key={block.title} id={sectionId(block.title)} tabIndex={-1} className={`resource-section resource-tone-${block.tone ?? "default"}`}>
        <h2>{block.title}</h2>
        {block.intro ? <p>{block.intro}</p> : null}
        <ul className="resource-list">
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </section>
    );
  }

  if (block.kind === "faq") {
    return (
      <section key={block.title} id={sectionId(block.title)} tabIndex={-1} className="resource-section">
        <h2>{block.title}</h2>
        <div className="resource-faq-list">
          {block.items.map((item) => (
            <details key={item.question} className="resource-faq-item">
              <summary><span>{item.question}</span><SiteIcon name="chevron-down" /></summary>
              <p>{item.answer}</p>
            </details>
          ))}
        </div>
      </section>
    );
  }

  if (block.kind === "contact") {
    return (
      <section key={block.title} id={sectionId(block.title)} tabIndex={-1} className="resource-section">
        <h2>{block.title}</h2>
        <div className="resource-contact-list">
          {block.items.map((item) => {
            const isPhone = /^\+?\d[\d\s-]*$/.test(item.value);
            const href = item.href ?? (isPhone ? `tel:${item.value.replace(/[\s-]/g, "")}` : undefined);

            return (
              <article key={item.label} className="resource-contact-row">
                <span className="resource-contact-icon"><SiteIcon name={contactIcons[item.label] ?? (isPhone ? "smartphone" : "messages-square")} /></span>
                <div>
                  <h3>{item.label}</h3>
                  <p>{item.note}</p>
                  {href ? (
                    <a href={href} className="resource-contact-action" aria-label={`${item.label}: ${item.value}`}>
                      {item.value}<SiteIcon name="arrow-up-right" />
                    </a>
                  ) : <span className="resource-contact-value">{item.value}</span>}
                </div>
              </article>
            );
          })}
        </div>
      </section>
    );
  }

  return (
    <section key={block.title} id={sectionId(block.title)} tabIndex={-1} className="resource-section">
      <h2>{block.title}</h2>
      {block.intro ? <p>{block.intro}</p> : null}
      <div className="resource-table-wrap">
        <table className="resource-table">
          <thead>
            <tr>
              <th scope="col">{block.columns[0]}</th>
              <th scope="col">{block.columns[1]}</th>
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
  return getPublicPageMetadata(params.slug);
}

export function generateStaticParams() {
  return getKnownLegacySlugs().map((slug) => ({ slug }));
}

export default function LegacyContentPage({ params }: Props) {
  const page = resolveLegacyPage(params.slug);

  if (!page) {
    notFound();
  }

  const structuredData = getPublicPageStructuredData(params.slug);
  const schema = structuredData ? <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData).replace(/</g, "\\u003c") }} /> : null;
  const jobSearch = { ...emptyJobSearch, city: page.city || page.category?.city || "", category: page.category?.slug || "" };
  const liveJobs = <LiveJobsSection search={jobSearch} heading={jobSearch.city ? `Live jobs in ${jobSearch.city}` : "Live jobs"} />;

  if (page.category || page.city) {
    return <SiteShell>{schema}{page.city ? <CityJobGuide page={page} liveJobs={liveJobs} /> : <JobCategoryGuide page={page} liveJobs={liveJobs} />}</SiteShell>;
  }

  const isLegal = page.eyebrow === "Legal";
  const isSafety = page.slug === "safety";
  const isFaq = page.slug === "faq";
  const isSupport = isSafety || page.eyebrow === "Support";
  const pageGroup = isLegal ? "Legal" : isSupport ? "Help centre" : "Browse jobs";
  const relatedPages = isLegal ? legalPages : isSupport ? supportPages : [];
  const icon = relatedPages.find((item) => item.href === `/${page.slug}`)?.icon ?? "briefcase-business";
  const contents = (
    <nav className="resource-toc" aria-label="On this page">
      {page.blocks.map((block, index) => (
        <a key={block.title} href={`#${sectionId(block.title)}`}>
          <span>{String(index + 1).padStart(2, "0")}</span>{block.title}
        </a>
      ))}
    </nav>
  );

  return (
    <SiteShell>
      {schema}
      <div className={`resource-page resource-${isLegal ? "legal" : isSupport ? "support" : "browse"}`}>
        <nav className="resource-breadcrumb" aria-label="Breadcrumb">
          <Link href="/">Home</Link><SiteIcon name="arrow-right" />
          <span>{pageGroup}</span><SiteIcon name="arrow-right" />
          <span aria-current="page">{page.title}</span>
        </nav>

        <header className="resource-header">
          <div className="resource-header-label">
            <span className="section-label"><SiteIcon name={icon} /> DUTYPE {pageGroup.toUpperCase()}</span>
            {isLegal ? <span className="resource-updated">Last updated: January 11, 2026</span> : null}
          </div>
          <h1>{page.title}</h1>
          <p>{page.intro}</p>
          {isSafety ? (
            <div className="resource-header-actions">
              <a href={page.ctaHref} className="button"><SiteIcon name="shield-check" /> {page.ctaLabel}</a>
              <a href="#emergency-contacts" className="text-link"><SiteIcon name="smartphone" /> Emergency contacts</a>
            </div>
          ) : null}
        </header>
        {page.slug === "jobs-near-me" ? liveJobs : null}

        {relatedPages.length ? (
          <nav className="resource-topic-nav" aria-label={`${pageGroup} pages`}>
            {relatedPages.map((item) => (
              <Link key={item.href} href={item.href} aria-current={item.href === `/${page.slug}` ? "page" : undefined}>
                <SiteIcon name={item.icon} />{item.label}
              </Link>
            ))}
          </nav>
        ) : null}

        {isLegal || isSafety ? (
          <div className="resource-overview" aria-label="At a glance">
            <span className="section-label">AT A GLANCE</span>
            <ul>
              {page.highlights.map((highlight) => (
                <li key={highlight}><SiteIcon name="check" /><span>{highlight}</span></li>
              ))}
            </ul>
          </div>
        ) : null}

        {!isFaq ? (
          <details className="resource-mobile-index">
            <summary>On this page<SiteIcon name="chevron-down" /></summary>
            {contents}
          </details>
        ) : null}

        <div className="resource-layout">
          <aside className="resource-sidebar">
            <span className="section-label">{isFaq ? "HELP & SUPPORT" : "ON THIS PAGE"}</span>
            {isFaq ? (
              <nav className="resource-toc" aria-label="More help">
                <Link href="/contact"><SiteIcon name="messages-square" />Contact support</Link>
                <Link href="/safety"><SiteIcon name="shield-check" />Stay safe on DutyPe</Link>
                <Link href="/refund"><SiteIcon name="wallet" />Refunds & billing</Link>
              </nav>
            ) : contents}
            <div className="resource-sidebar-help">
              <SiteIcon name="messages-square" />
              <strong>{isSafety ? "Something doesn't feel right?" : "Need a hand?"}</strong>
              <Link href={isSafety || page.slug === "contact" ? `mailto:${SUPPORT_EMAIL}` : "/contact"} className="text-link">
                {isSafety ? "Report an issue" : page.slug === "contact" ? "Email support" : "Contact support"}<SiteIcon name="arrow-up-right" />
              </Link>
            </div>
          </aside>

          <div className="resource-body">
            {isFaq ? (
              <FaqContent groups={page.blocks.flatMap((block) => block.kind === "faq" ? [{ ...block, id: sectionId(block.title) }] : [])} />
            ) : page.blocks.map((block) => renderBlock(block))}

            {page.ctaTitle && page.ctaCopy ? (
              <section className="resource-next-step" aria-label={page.ctaTitle}>
                <span className="resource-contact-icon"><SiteIcon name={isLegal ? "clipboard-list" : "messages-square"} /></span>
                <div><h2>{page.ctaTitle}</h2><p>{page.ctaCopy}</p></div>
                {page.ctaHref && page.ctaLabel ? (
                  <Link href={page.ctaHref} className="button ghost">{page.ctaLabel}<SiteIcon name="arrow-up-right" /></Link>
                ) : null}
              </section>
            ) : null}
          </div>
        </div>
      </div>
    </SiteShell>
  );
}
