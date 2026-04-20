import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Ideal Customer Profile inputs (founder to confirm)

> Recommended starting point based on product reality. Validate / overwrite with what you've actually seen working.

## Worker ICP (supply side – the bottleneck)
- 19–40 year old, blue-collar / semi-skilled, smartphone literate.
- Currently finds work via: WhatsApp groups, naka labour points, local contractors, referral from a relative.
- Primary pain: irregular daily wage work, middlemen take cuts, no proof of work, no easy way to find next gig.
- Triggers: end of last gig, salary delayed, came back from village, just bought a smartphone.
- Trust gates: "will I actually get paid?", "is the employer real?", "is the address near me?"
- Wedge category (recommended starting): **Cook + Maid** in Hyderabad / Secunderabad / Cyberabad pincodes (Telugu localization is already shipped, so distribution is cheaper here than any other city).

## Employer ICP (demand side – pull, not push)
Tiered. Start with the easiest two.

1. **Households** in dense apartment complexes in Hyderabad (₹15–25K maids, ₹8–15K cooks, drivers).
   - Pain: agency fees ₹2–5K per replacement, no transparency, no QR proof.
   - Trigger: maid quit, going to village, society WhatsApp group has someone asking.
2. **Small F&B + retail** (PG owners, tiffin centres, cloud kitchens, salons, clinics, small kiranas).
   - Pain: high turnover, no time to interview, vendor staffing is expensive.
   - Trigger: festival rush, exam season, sudden quit.

## Segments to ignore for now
- Large enterprises / IT services / corporate staffing — wrong sales motion.
- Recruiters and consultancies — they are middlemen, your model breaks them.
- Tier-3/4 cities — supply density too thin, support cost too high.
- Skilled-trade contractors (electricians, plumbers) — already have established WhatsApp groups; harder to displace, leave for v2.

## Trigger events to monitor
- Society WhatsApp groups posting "any maid available?"
- "Help wanted" boards outside salons / clinics / PGs.
- Quikr / OLX / Apna postings for the same categories in your wedge city (these are people who *just* tried elsewhere and failed).
- Local Facebook / Telegram groups for area-specific hiring.

## Founder must answer
- Of the 17 categories, which 2 do you have *most evidence* of repeat usage on? (Use that as wedge.)
- Top 3 pincodes where workers are densest *today*. Marketing concentrates there.
- Worst job category to be in right now (where employers complain most). Pause it.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
