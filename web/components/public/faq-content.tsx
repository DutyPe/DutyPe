"use client";

import { useRef, useState } from "react";

import { SiteIcon } from "@/components/site-icon";
import type { LegacyPageBlock } from "@/lib/public-site";

type FaqGroup = Extract<LegacyPageBlock, { kind: "faq" }> & { id: string };

export function FaqContent({ groups }: { groups: FaqGroup[] }) {
  const searchInput = useRef<HTMLInputElement>(null);
  const [search, setSearch] = useState("");
  const [topic, setTopic] = useState("all");
  const terms = search.trim().toLowerCase().split(/\s+/).filter(Boolean);
  const matchingGroups = groups
    .filter((group) => topic === "all" || topic === group.id)
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => terms.every((term) => `${item.question} ${item.answer}`.toLowerCase().includes(term)))
    }))
    .filter((group) => group.items.length > 0);
  const answerCount = matchingGroups.reduce((total, group) => total + group.items.length, 0);

  return (
    <div className="resource-faq-content">
      <form className="resource-faq-search" role="search" onSubmit={(event) => event.preventDefault()}>
        <label htmlFor="faq-search">Search questions</label>
        <div>
          <SiteIcon name="search" />
          <input ref={searchInput} id="faq-search" type="search" value={search} maxLength={200} onChange={(event) => setSearch(event.target.value)} placeholder="Jobs, accounts, payments..." />
          {search ? (
            <button type="button" className="icon-button" aria-label="Clear search" title="Clear search" onClick={() => { setSearch(""); searchInput.current?.focus(); }}><SiteIcon name="x" /></button>
          ) : null}
        </div>
      </form>

      <div className="resource-faq-topics" role="group" aria-label="Question topics">
        {[{ id: "all", title: "All questions" }, ...groups].map((group) => (
          <button key={group.id} type="button" aria-pressed={topic === group.id} onClick={() => setTopic(group.id)}>{group.title}</button>
        ))}
      </div>
      <p className="resource-result-count" role="status">{answerCount} {answerCount === 1 ? "answer" : "answers"}{search.trim() ? " matching your search" : ""}</p>

      {matchingGroups.length ? matchingGroups.map((group) => (
        <section key={group.id} id={group.id} className="resource-section">
          <h2>{group.title}</h2>
          <div className="resource-faq-list">
            {group.items.map((item) => (
              <details key={item.question} className="resource-faq-item">
                <summary><span>{item.question}</span><SiteIcon name="chevron-down" /></summary>
                <p>{item.answer}</p>
              </details>
            ))}
          </div>
        </section>
      )) : (
        <div className="resource-empty">
          <SiteIcon name="search" />
          <h2>No matching answers</h2>
          <p>No questions matched your current search and topic.</p>
          <button type="button" className="button ghost" onClick={() => { setSearch(""); setTopic("all"); searchInput.current?.focus(); }}>Reset filters<SiteIcon name="arrow-right" /></button>
        </div>
      )}
    </div>
  );
}