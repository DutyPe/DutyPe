import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Research

> Outside-in market data, competitor teardowns, and ICP interview notes that ground the strategy in observable reality.

## Files

- [competitor_teardown.md](competitor_teardown.md) — feature, pricing, and positioning teardown of Apna, WorkIndia, Job Hai, Vahan, Quikr, OLX, WhatsApp groups.
- [hyderabad_market_brief.md](hyderabad_market_brief.md) — pincode demographics, household density, demand-side hypotheses for the wedge.
- [interview_notes_template.md](interview_notes_template.md) — interview note template (use for every worker / employer / partner conversation).
- [interviews/](interviews/) — one folder per ICP. Drop dated notes inside.

## Cadence

- Founder writes 1 long-form interview note per week minimum (≥ 30 minutes of conversation).
- Competitor teardown is refreshed every 90 days (or on a major launch / pricing change you observe).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
