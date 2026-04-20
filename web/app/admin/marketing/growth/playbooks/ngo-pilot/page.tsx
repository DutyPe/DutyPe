import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Playbook — NGO 50-Woman Cohort Pilot

> Goal: a single NGO partnership produces 30 activated workers + 10 confirmed hires + 1 documented case study in 30 days.

## Pre-requisites

- NGO partner identified (see [hyderabad_market_brief.md](../research/hyderabad_market_brief.md) for shortlist).
- MoU signed (template in [partnership_legal.md](partnership_legal.md)).
- ₹6 K incentive budget (₹100 × 50 women on profile-completion + first-application).
- ₹2 K event budget (venue, refreshments, banner).
- Photographer (founder phone is fine).
- 50 pamphlets + 50 stickers.

## Step 1 — Pre-event (1 week before)

- NGO sends list of 50 candidate women.
- Founder + NGO co-host runs a WhatsApp broadcast: date, time, venue, what to bring (Aadhaar for OTP, phone).
- Confirm 35+ RSVPs to expect 50 walk-ins.

## Step 2 — Event day (3 hours)

- 0:00–0:20: NGO leader speaks (community trust).
- 0:20–0:40: Founder speaks in Telugu — what DutyPe is, why no commission, why women-first.
- 0:40–2:00: One-on-one install with 5 promoters (founder + 4 ops/volunteers). Each promoter handles 10 women.
- 2:00–2:30: Q&A. Capture every objection in writing.
- 2:30–3:00: Photos with consent. Snacks. Capture 3 video testimonials.

## Step 3 — Day 1–7 follow-up

- Day 2: WhatsApp every attendee personally. "Profile pending? Help cheyala?"
- Day 3: Push notification: "Apply mee area lo jobs ki."
- Day 7: Issue ₹100 to each woman who completed profile + ≥ 1 application. Use UPI direct payout (Razorpay payouts API).

## Step 4 — Day 8–30 follow-up

- Track activations weekly in \`growth/campaigns/field_visits.csv\`.
- Weekly 1:1 WhatsApp from founder to each cohort member: "Anything I can fix?"
- Document best testimonial in writing + video → use as content asset.

## Step 5 — Case study output (day 30)

- Single-page case study: "X women from <NGO> earned ₹Y in 30 days via DutyPe."
- Share with NGO leadership + investor pipeline + journalist if relevant.

## Stop rules

- If event RSVPs < 25 → reschedule, don't half-fire.
- If activations < 10 by day 14 → diagnose: is it the cohort (wrong segment), the install flow (friction), or the job supply (insufficient demand in their area)?
- Never run two NGO cohorts in same 30-day window. One at a time, one founder, deep attention.

## Quality bar

- ≥ 30 activated workers (out of 50).
- ≥ 10 first-job fills.
- ≥ 1 written case study.
- ≥ 3 video testimonials.

## Scaling rule

- Run one NGO pilot in week 6 of the 90-day plan.
- After the first one, replicate with 1 NGO/month for 3 more months.
- Cap at 4 NGO partnerships in city-1 for the first 12 months.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
