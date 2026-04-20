import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Current Metrics (input file – founder to fill)

> Honesty here drives everything else. If a number is unknown, write \`unknown\` rather than a guess.

## Install / signup funnel
- Total Play Store installs to date: ____
- Active installs (Play Console): ____
- Last-30-day new signups (workers): ____
- Last-30-day new signups (employers): ____
- Worker profile-completion rate (≥80%): ____%
- Employer profile-completion rate (≥80%): ____%

## Marketplace health
- Jobs posted last 7 / 30 days: ____ / ____
- Median time from post → first application: ____ minutes
- % of jobs receiving ≥3 applications in 24h: ____%
- % of jobs reported "filled" (any signal): ____%
- Repeat-post rate among employers (≥2 posts in 30 days): ____%

## Geography
- Top 5 cities by active workers (last 30d): ____
- Top 5 pincodes by active workers: ____
- Cities where employers have posted but no workers exist: ____ (these are dead, don't market there)

## Retention proxies
- Worker D1 / D7 / D30: ____ / ____ / ____
- Employer post→re-post within 30d: ____%
- Notification opt-in rate: ____%

## Economics
- AdMob revenue / month: ₹____
- Contact-unlock revenue / month: ₹____
- Referral payouts / month: ₹____
- Implied "CAC" (any spend) / month: ₹____
- Months of runway: ____
- Marketing budget approved / month: ₹____

## Support load
- WhatsApp support tickets / week: ____
- Top 3 complaint themes: ____ / ____ / ____

## Honest self-rating (1–5)
- Product polish: ____
- Worker NPS (gut): ____
- Employer NPS (gut): ____
- Density in best pincode: ____
- Confidence we have product–market fit in that pincode: ____
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
