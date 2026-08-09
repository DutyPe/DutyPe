import { MarketingFullView } from "@/components/marketing-shell";
import { CallStatsDisplay } from "@/components/admin/marketing/call-stats-display";
import { MarketingDocSearch } from "@/components/admin/marketing/marketing-doc-search";
import { RecentCallFeedback } from "@/components/admin/marketing/recent-call-feedback";
import {
  getMarketingTreeWithContent,
  type MarketingLoadedNode,
} from "@/lib/marketing-content";

/** Icon + blurb per section. Counts are derived from disk, never hardcoded. */
const SECTION_META: Record<string, { icon: string; desc: string }> = {
  "growth/assets": { icon: "🎨", desc: "Pamphlets, posters, one-pagers, screenshots." },
  "growth/campaigns": { icon: "📣", desc: "Field visits, partnerships, reactivation runs." },
  "growth/inputs": { icon: "📥", desc: "ICP, pricing, competitors, founder notes." },
  "growth/outputs": { icon: "📤", desc: "Strategy decks, KPI specs, channel plans." },
  "growth/playbooks": { icon: "📘", desc: "RWA, naka, onboarding, press, partnerships." },
  "growth/research": { icon: "🔬", desc: "Market briefs, competitor teardowns, interviews." },
  "marketing/brand": { icon: "🪣", desc: "Foundation, voice, visual identity, taglines." },
  "marketing/channels": { icon: "📡", desc: "WhatsApp, Play Store, LinkedIn, press, paid." },
  "marketing/go-to-market": { icon: "🚀", desc: "Positioning, ICP, city launch, seasonal calendar." },
};

function countFiles(node: MarketingLoadedNode): number {
  return node.kind === "file"
    ? 1
    : node.children.reduce((sum, child) => sum + countFiles(child), 0);
}

function anchorId(slug: string[]): string {
  return slug.join("-").toLowerCase().replace(/[^a-z0-9-]+/g, "-");
}

type Section = {
  id: string;
  title: string;
  count: number;
  icon: string;
  desc: string;
};

function collectSections(tree: MarketingLoadedNode[]): Section[] {
  const sections: Section[] = [];
  for (const group of tree) {
    if (group.kind !== "dir") continue;
    for (const child of group.children) {
      if (child.kind !== "dir") continue;
      const meta = SECTION_META[child.slug.join("/")];
      if (!meta) continue;
      sections.push({
        id: anchorId(child.slug),
        title: child.name.replace(/-/g, " ").replace(/\b\w/g, (c) => c.toUpperCase()),
        count: countFiles(child),
        icon: meta.icon,
        desc: meta.desc,
      });
    }
  }
  return sections;
}

export default function MarketingLandingPage() {
  const tree = getMarketingTreeWithContent();
  const sections = collectSections(tree);
  const totalDocs = tree.reduce((sum, node) => sum + countFiles(node), 0);
  const topLevelGroups = tree.filter((node) => node.kind === "dir").length;

  return (
    <div className="marketing-landing">
      <header className="marketing-hero">
        <div className="marketing-hero-content">
          <h1>Marketing &amp; Growth Hub</h1>
          <p>
            Every campaign, asset, input, output, playbook, channel, brand,
            go-to-market and research document — all in one searchable place.
          </p>
          <div className="marketing-hero-stats">
            <div className="marketing-hero-stat">
              <strong>{totalDocs}</strong>
              <span>Documents</span>
            </div>
            <CallStatsDisplay />
            <div className="marketing-hero-stat">
              <strong>{sections.length}</strong>
              <span>Categories</span>
            </div>
            <div className="marketing-hero-stat">
              <strong>{topLevelGroups}</strong>
              <span>Top-level groups</span>
            </div>
          </div>
        </div>
      </header>

      <RecentCallFeedback />

      <MarketingDocSearch totalDocs={totalDocs} />

      <div className="marketing-section-cards">
        {sections.map((section) => (
          <a key={section.id} href={`#${section.id}`} className="marketing-section-card">
            <div className="icon">{section.icon}</div>
            <div className="meta">
              <strong>
                {section.title}{" "}
                <span className="marketing-section-count">· {section.count}</span>
              </strong>
              <span>{section.desc}</span>
            </div>
          </a>
        ))}
      </div>

      <MarketingFullView tree={tree} />
    </div>
  );
}
