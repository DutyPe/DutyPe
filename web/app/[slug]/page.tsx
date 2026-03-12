import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { SiteShell } from "@/components/site-shell";
import {
  getKnownLegacySlugs,
  resolveLegacyPage,
  type LegacyPageBlock
} from "@/lib/public-site";

type Props = {
  params: {
    slug: string;
  };
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
        <ul className="detail-list">
          {block.items.map((item) => (
            <li key={item}>
              <strong>{item}</strong>
              <span>Relevant to this public route and the in-app follow-through.</span>
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
            <div key={item.question} className="faq-item">
              <strong>{item.question}</strong>
              <p>{item.answer}</p>
            </div>
          ))}
        </div>
      </article>
    );
  }

  if (block.kind === "contact") {
    return (
      <article key={block.title} className={`detail-panel tone-${block.tone ?? "default"}`}>
        <span className="card-kicker">{block.title}</span>
        <h3>{block.title}</h3>
        <div className="contact-grid">
          {block.items.map((item) => (
            <div key={item.label} className="contact-card">
              <strong>{item.label}</strong>
              <p>{item.note}</p>
              {item.href ? <Link href={item.href}>{item.value}</Link> : <span>{item.value}</span>}
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
    description: page.description
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

  return (
    <SiteShell>
      <section className="hero">
        <div className="hero-grid">
          <div>
            <span className="eyebrow">{page.eyebrow}</span>
            <h1 className="headline">{page.title}</h1>
            <p className="lede">{page.intro}</p>
            <div className="button-row">
              {page.ctaHref && page.ctaLabel ? (
                <Link href={page.ctaHref} className="button">
                  {page.ctaLabel}
                </Link>
              ) : null}
              <Link href="/jobs" className="button ghost">
                Browse jobs
              </Link>
            </div>
          </div>

          <aside className="hero-panel">
            <span className="card-kicker">Page focus</span>
            <h3>What this route preserves</h3>
            <ul className="detail-list">
              {page.highlights.map((highlight) => (
                <li key={highlight}>
                  <strong>{highlight}</strong>
                  <span>Legacy content, cleaner route structure, better UI.</span>
                </li>
              ))}
            </ul>
          </aside>
        </div>
      </section>

      <section className="section">
        <div className="section-header">
          <div>
            <span className="tag">Legacy content</span>
            <h2>Moved from the old public site into Next.js</h2>
          </div>
          <p>{page.description}</p>
        </div>

        <div className="section-grid legacy-grid">{page.blocks.map((block) => renderBlock(block))}</div>

        {page.ctaTitle && page.ctaCopy ? (
          <div className="callout">
            <strong>{page.ctaTitle}</strong>
            <span>{page.ctaCopy}</span>
          </div>
        ) : null}
      </section>
    </SiteShell>
  );
}
