"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type CaseMetric = {
  label: string;
  value: string;
};

type CaseStudy = {
  index: string;
  status: string;
  org: string;
  period: string;
  title: string;
  summary: string;
  tags: string[];
  metrics: CaseMetric[];
  href: string;
  cta: string;
};

const caseStudies: CaseStudy[] = [
  {
    index: "01",
    status: "Live now",
    org: "DutyPe Worker Flow",
    period: "2026",
    title: "Turning search visitors into worker applications",
    summary:
      "Location-first pages, cleaner category routing, and stronger trust framing lifted job-route engagement while reducing dead-end sessions.",
    tags: ["Acquisition", "SEO UX", "Worker Funnel"],
    metrics: [
      { label: "Jobs route opens", value: "+42%" },
      { label: "Bounce reduction", value: "-31%" },
      { label: "Avg pages/session", value: "+1.8x" }
    ],
    href: "/jobs-near-me",
    cta: "Explore worker routes"
  },
  {
    index: "02",
    status: "In progress",
    org: "DutyPe Employer Flow",
    period: "2026",
    title: "Shortening time-to-hire for local businesses",
    summary:
      "Employer pathways now emphasize posting clarity, applicant review visibility, and high-confidence trust cues before conversion drop-offs happen.",
    tags: ["Hiring Ops", "Conversion", "Employer UX"],
    metrics: [
      { label: "Post-job completion", value: "+27%" },
      { label: "Qualified applicant rate", value: "+19%" },
      { label: "First response speed", value: "2.3x" }
    ],
    href: "/employer",
    cta: "Open employer flow"
  },
  {
    index: "03",
    status: "Experimenting",
    org: "DutyPe Trust Layer",
    period: "2026",
    title: "Building high-confidence job discovery at first glance",
    summary:
      "Policy visibility, no-fee messaging, and referral-aware growth surfaces were redesigned to improve credibility before app install.",
    tags: ["Trust", "Retention", "Growth"],
    metrics: [
      { label: "Trust panel interactions", value: "+58%" },
      { label: "Policy route CTR", value: "+33%" },
      { label: "Referral section engagement", value: "+2.1x" }
    ],
    href: "/refer",
    cta: "View referral flow"
  }
];

export function WorkCaseDeck() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    if (isPaused) {
      return;
    }

    const timer = window.setInterval(() => {
      setActiveIndex((prev) => (prev + 1) % caseStudies.length);
    }, 4200);

    return () => window.clearInterval(timer);
  }, [isPaused]);

  const activeCase = caseStudies[activeIndex];

  return (
    <section className="section work-deck-section" aria-labelledby="work-deck-title">
      <div className="section-header">
        <div>
          <span className="tag">Selected product work</span>
          <h2 id="work-deck-title">Interactive case stream</h2>
        </div>
        <p>
          Explore active growth initiatives across worker discovery, employer conversion,
          and trust architecture. Hover a case or let the deck auto-rotate.
        </p>
      </div>

      <div
        className="work-deck-grid"
        onMouseEnter={() => setIsPaused(true)}
        onMouseLeave={() => setIsPaused(false)}
        onTouchStart={() => setIsPaused(true)}
        onTouchEnd={() => setIsPaused(false)}
      >
        <div className="work-list" role="tablist" aria-label="Case selection list">
          {caseStudies.map((item, index) => {
            const isActive = index === activeIndex;

            return (
              <button
                key={item.index}
                type="button"
                className={`work-list-item${isActive ? " active" : ""}`}
                onClick={() => setActiveIndex(index)}
                role="tab"
                aria-selected={isActive}
              >
                <span className="work-list-number">{item.index}</span>
                <div className="work-list-body">
                  <span className="work-list-kicker">
                    {item.status} · {item.org}
                  </span>
                  <strong>{item.title}</strong>
                </div>
              </button>
            );
          })}
        </div>

        <article className="work-preview">
          <span className="work-preview-meta">
            {activeCase.org} / {activeCase.period}
          </span>
          <h3>{activeCase.title}</h3>
          <p>{activeCase.summary}</p>

          <div className="work-preview-tags">
            {activeCase.tags.map((tag) => (
              <span key={tag} className="work-tag">
                {tag}
              </span>
            ))}
          </div>

          <div className="work-metric-grid">
            {activeCase.metrics.map((metric) => (
              <article key={metric.label} className="work-metric-card">
                <span>{metric.label}</span>
                <strong>{metric.value}</strong>
              </article>
            ))}
          </div>

          <div className="button-row compact">
            <Link href={activeCase.href} className="button ghost">
              {activeCase.cta}
            </Link>
            <span className="work-auto-state">{isPaused ? "Paused" : "Auto rotating"}</span>
          </div>
        </article>
      </div>
    </section>
  );
}
