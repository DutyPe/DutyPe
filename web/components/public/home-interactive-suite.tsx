"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";

type LinkItem = {
  href: string;
  label: string;
};

type HomeInteractiveSuiteProps = {
  categories: LinkItem[];
  cities: LinkItem[];
};

type ExploreGroup = "All" | "Category" | "City";

const trendFeed = [
  {
    title: "Delivery demand is rising in Hyderabad",
    copy: "Evening and weekend delivery shifts are opening faster than average this week.",
    delta: "+22%",
    eta: "Updated 2m ago"
  },
  {
    title: "Retail hiring momentum in Bangalore",
    copy: "Store assistant and cashier roles are seeing stronger conversion after 6 PM.",
    delta: "+17%",
    eta: "Updated 5m ago"
  },
  {
    title: "Helper jobs accelerating in Delhi NCR",
    copy: "Small business hiring is trending up around packing, dispatch, and floor support.",
    delta: "+19%",
    eta: "Updated 8m ago"
  }
];

const quickSearchTags = ["jobs near me", "driver jobs", "part-time", "hyderabad", "warangal"];

function getRadiusAdvice(radius: number) {
  if (radius <= 3) {
    return "Hyper-local mode: best for same-day joining and low-commute worker matching.";
  }
  if (radius <= 8) {
    return "Balanced mode: wider quality pool while keeping travel practical for workers.";
  }
  return "Expansion mode: broadest hiring inventory for urgent or hard-to-fill roles.";
}

export function HomeInteractiveSuite({ categories, cities }: HomeInteractiveSuiteProps) {
  const searchRef = useRef<HTMLInputElement | null>(null);
  const [group, setGroup] = useState<ExploreGroup>("All");
  const [query, setQuery] = useState("");
  const [radiusKm, setRadiusKm] = useState(5);
  const [trendIndex, setTrendIndex] = useState(0);
  const [isTrendPlaying, setIsTrendPlaying] = useState(true);

  const allLinks = useMemo(
    () => [
      ...categories.map((item) => ({ ...item, group: "Category" as const })),
      ...cities.map((item) => ({ ...item, group: "City" as const }))
    ],
    [categories, cities]
  );

  const filteredLinks = useMemo(() => {
    const needle = query.trim().toLowerCase();

    return allLinks
      .filter((item) => {
        if (group !== "All" && item.group !== group) {
          return false;
        }

        if (!needle) {
          return true;
        }

        return item.label.toLowerCase().includes(needle);
      })
      .slice(0, 10);
  }, [allLinks, group, query]);

  const estimatedMatches = useMemo(() => {
    const groupBoost = group === "Category" ? 42 : group === "City" ? 28 : 18;
    return Math.round(140 + radiusKm * radiusKm * 14 + groupBoost);
  }, [group, radiusKm]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const isTypingField =
        target && ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);

      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchRef.current?.focus();
        searchRef.current?.select();
        return;
      }

      if (event.key === "/" && !isTypingField) {
        event.preventDefault();
        searchRef.current?.focus();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    if (!isTrendPlaying) {
      return;
    }

    const timer = window.setInterval(() => {
      setTrendIndex((prev) => (prev + 1) % trendFeed.length);
    }, 3400);

    return () => window.clearInterval(timer);
  }, [isTrendPlaying]);

  const trend = trendFeed[trendIndex];
  const trendProgress = ((trendIndex + 1) / trendFeed.length) * 100;

  return (
    <section className="section interactive-suite" aria-labelledby="interactive-suite-title">
      <div className="section-header">
        <div>
          <span className="tag">Interactive mode</span>
          <h2 id="interactive-suite-title">Smart job discovery command center</h2>
        </div>
        <p>
          Use live filters, trend pulses, and radius simulation to discover what is hiring
          now in your preferred market.
        </p>
      </div>

      <div className="interactive-grid">
        <article className="interactive-panel">
          <span className="card-kicker">Command search</span>
          <h3>Search categories and cities instantly</h3>
          <p>
            Press <strong>Ctrl/Cmd + K</strong> to jump into search. Use <strong>/</strong>
            from anywhere on this page.
          </p>

          <div className="command-row">
            <input
              ref={searchRef}
              type="text"
              className="command-input"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Type: driver, part-time, Hyderabad..."
              aria-label="Search categories and cities"
            />
            <span className="command-hint">Ctrl/Cmd + K</span>
          </div>

          <div className="filter-chip-row" role="tablist" aria-label="Filter group">
            {(["All", "Category", "City"] as ExploreGroup[]).map((option) => (
              <button
                key={option}
                type="button"
                className={`filter-chip${group === option ? " active" : ""}`}
                onClick={() => setGroup(option)}
                role="tab"
                aria-selected={group === option}
              >
                {option}
              </button>
            ))}
          </div>

          <div className="quick-tag-row">
            {quickSearchTags.map((tag) => (
              <button
                key={tag}
                type="button"
                className="quick-tag"
                onClick={() => setQuery(tag)}
              >
                #{tag}
              </button>
            ))}
          </div>

          <div className="interactive-results">
            {filteredLinks.length > 0 ? (
              filteredLinks.map((item) => (
                <Link key={item.href} href={item.href} className="result-link">
                  <strong>{item.label}</strong>
                  <span>Open route</span>
                </Link>
              ))
            ) : (
              <div className="result-empty">
                No routes match this filter yet. Try a broader keyword.
              </div>
            )}
          </div>
        </article>

        <article className="interactive-panel">
          <span className="card-kicker">Radius simulator</span>
          <h3>Preview hiring volume by distance</h3>
          <p>
            Adjust your discovery radius to estimate how many active jobs you can unlock.
          </p>

          <div className="radius-meta">
            <strong>{radiusKm} km</strong>
            <span>{estimatedMatches} estimated active matches</span>
          </div>

          <input
            type="range"
            min={1}
            max={15}
            step={1}
            value={radiusKm}
            onChange={(event) => setRadiusKm(Number(event.target.value))}
            className="radius-range"
            aria-label="Set search radius in kilometers"
          />

          <div className="radius-scale" aria-hidden="true">
            <span>1 km</span>
            <span>15 km</span>
          </div>

          <p className="radius-tip">{getRadiusAdvice(radiusKm)}</p>

          <Link href="/jobs-near-me" className="button ghost radius-cta">
            Explore nearby jobs
          </Link>
        </article>

        <article
          className="interactive-panel trend-panel"
          onMouseEnter={() => setIsTrendPlaying(false)}
          onMouseLeave={() => setIsTrendPlaying(true)}
        >
          <div className="trend-head">
            <span className="card-kicker">Live pulse</span>
            <button
              type="button"
              className="trend-toggle"
              onClick={() => setIsTrendPlaying((prev) => !prev)}
            >
              {isTrendPlaying ? "Pause" : "Play"}
            </button>
          </div>

          <h3>{trend.title}</h3>
          <p>{trend.copy}</p>

          <div className="trend-badges">
            <span className="pill">{trend.delta} weekly trend</span>
            <span className="pill">{trend.eta}</span>
          </div>

          <div className="trend-progress" aria-hidden="true">
            <span style={{ width: `${trendProgress}%` }} />
          </div>

          <div className="trend-list">
            {trendFeed.map((item, index) => (
              <button
                key={item.title}
                type="button"
                className={`trend-item${index === trendIndex ? " active" : ""}`}
                onClick={() => setTrendIndex(index)}
              >
                <strong>{item.title}</strong>
                <span>{item.delta}</span>
              </button>
            ))}
          </div>
        </article>
      </div>
    </section>
  );
}
