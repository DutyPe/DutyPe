import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# City Launch Kit

> Repeatable playbook for opening city N+1. Do **not** open a new city until the day-90 expansion gates from \`growth/outputs/master_growth_strategy.md\` are met.

## Pre-launch (week -4 to week 0)

- [ ] Confirm all 3 expansion gates met in city-1.
- [ ] Pick city-2 from candidate list (Vijayawada → Visakhapatnam → Bangalore preferred order).
- [ ] Write a city-specific brief (mirror of [\`growth/research/hyderabad_market_brief.md\`](../../growth/research/hyderabad_market_brief.md)).
- [ ] Pick 3 wedge pincodes in city-2.
- [ ] Translate / localise marketing assets:
  - Pamphlet (city-name swapped).
  - Society poster.
  - WhatsApp templates.
  - Push copy (city-name + local language if different).
- [ ] Update Play Store long-description to include city-2 in city list.
- [ ] Add city-2 to \`/<city>/<pincode>/<category>\` programmatic pages.
- [ ] Identify 30 RWA admins per pincode in city-2.
- [ ] Identify 200 SMB prospects per pincode (PG, cloud kitchen, salon, tiffin).
- [ ] Hire / assign one city operator (founder for first 2 weeks; full-time ops by week 4).

## Week 1 of city-2

- [ ] Founder spends 5 days in city-2 personally.
- [ ] Naka pamphlet drops × 4 mornings (one per pincode + one repeat).
- [ ] 30 RWA admins messaged.
- [ ] 50 SMBs cold-messaged.
- [ ] Telugu / local press soft launch (1 journalist coffee, no formal pitch yet).

## Week 2–4 of city-2

- [ ] First RWA partnership signed.
- [ ] First NGO partnership conversation opened.
- [ ] 200+ city-2 active workers.
- [ ] 25+ city-2 active employers.
- [ ] First Telugu Short filmed in city-2 (different geography signals authenticity).

## Day 30 city-2 checkpoint

If city-2 hits ≥ 50 % of city-1's day-30 metrics → keep going.
If less → diagnose (wrong pincode? wrong message? wrong founder presence?). Do not push city-3.

## Day 90 city-2 checkpoint

Mirror of city-1 day-90 success criteria.

## What this kit does NOT include

- A "national rollout plan" (we don't have one).
- A press launch with a national publication (don't pitch press for city expansion until city-2 hits day-90 milestones).
- Paid media spend (still OFF — see [\`marketing/channels/paid_media.md\`](../channels/paid_media.md)).
- A new product feature (city expansion is a distribution motion, not a product motion).

## Owner

- Founder for weeks -4 through 4.
- City operator from week 4 onward.
- Engineer for SEO / push / WhatsApp infra (one-time setup).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
