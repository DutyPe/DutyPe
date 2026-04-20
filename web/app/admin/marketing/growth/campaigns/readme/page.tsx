import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Campaigns — operational tracking

> All live and historical campaigns are tracked here. Each campaign = one CSV. Update at end of each working day. Friday review reads from these files.

## Files

- [outbound.csv](outbound.csv) — every outbound message sent (WhatsApp, email, LinkedIn DM, in-person).
- [content_log.csv](content_log.csv) — every published content asset.
- [partnerships.csv](partnerships.csv) — RWA / NGO / contractor / SMB partnership pipeline.
- [reactivation.csv](reactivation.csv) — lifecycle / reactivation pushes sent.
- [field_visits.csv](field_visits.csv) — every naka / society / field session.
- [experiments.csv](experiments.csv) — active and historical experiments (linked to \`growth/outputs/growth_experiments_backlog.md\`).

## Update cadence

| File | When updated | Owner |
|---|---|---|
| outbound.csv | End of day, every day messages were sent | Founder / SDR |
| content_log.csv | Within 24 h of publishing | Founder |
| partnerships.csv | When status changes | Founder |
| reactivation.csv | Auto-export from Cloud Function weekly | Engineer |
| field_visits.csv | End of each session | Founder |
| experiments.csv | Friday review | Founder |

## Rules

- **No backfilling 2 weeks later.** Either log it that day or it didn't happen.
- **Use ISO dates** (\`YYYY-MM-DD\`) only.
- **Verdict columns** must be one of: \`pending\`, \`replied\`, \`meeting\`, \`signed\`, \`dead\`, \`keep\`, \`kill\`, \`scale\`.
- **Founder hours** is per-row, not cumulative.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
