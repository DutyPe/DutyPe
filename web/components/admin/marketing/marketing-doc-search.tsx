"use client";

import { useEffect, useState } from "react";

/**
 * Filters the rendered doc list by toggling `.marketing-full-file` elements.
 * Works against the DOM because the doc blocks are rendered by MarketingFullView,
 * which owns its own markup.
 */
export function MarketingDocSearch({ totalDocs }: { totalDocs: number }) {
  const [search, setSearch] = useState("");
  const [matchCount, setMatchCount] = useState<number | null>(null);

  useEffect(() => {
    if (typeof document === "undefined") return;
    const term = search.trim().toLowerCase();
    const items = document.querySelectorAll<HTMLDetailsElement>(
      ".marketing-full-file"
    );
    let visible = 0;
    items.forEach((el) => {
      if (!term) {
        el.style.display = "";
        el.open = false;
        visible += 1;
        return;
      }
      const text = (el.textContent ?? "").toLowerCase();
      const isMatch = text.includes(term);
      el.style.display = isMatch ? "" : "none";
      el.open = isMatch;
      if (isMatch) visible += 1;
    });
    setMatchCount(term ? visible : null);
  }, [search]);

  return (
    <div className="marketing-search-bar">
      <span className="search-icon">🔍</span>
      <input
        type="search"
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        placeholder="Search across pamphlets, playbooks, research, channels…"
      />
      <span className="search-count">
        {matchCount !== null
          ? `${matchCount} match${matchCount === 1 ? "" : "es"}`
          : `${totalDocs} docs`}
      </span>
    </div>
  );
}
