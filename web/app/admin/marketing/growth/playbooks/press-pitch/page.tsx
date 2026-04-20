import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Playbook — Press Pitch (do NOT execute before 1 000 jobs filled)

> Earned media is leverage when you have a real story. Before then it is a vanity exercise that costs founder time.

## Eligibility gate

Do not pitch any journalist until **all** of these are true:
- ≥ 1 000 cumulative jobs filled.
- ≥ 1 city with provable density (≥ 500 active workers in 3 wedge pincodes).
- ≥ 1 written case study (NGO cohort or marquee employer).
- ≥ ₹X cumulative payouts to workers as referral / earnings (proof of redistribution).
- Founder is comfortable being on record with raw numbers.

## Target outlets (in order)

1. **Telugu print + digital:** Eenadu, Sakshi, V6 News.
2. **Local English:** Telangana Today, Deccan Chronicle.
3. **National tech-business:** YourStory, Inc42, Entrackr (warmer to bootstrapped traction).
4. **National mainstream business:** Mint, Business Standard, ET (only with a real numbers story).
5. **Long-form podcasts:** Founder Thesis, The Seen and the Unseen (only if you have a strong point of view).

Avoid: paid PR placements, "as featured in" startup directories, regional aggregators with no readership.

## Pitch one-pager (≤ 1 page)

\`\`\`
SUBJECT: <City> blue-collar workers earned ₹X without paying agency commission — DutyPe metrics

Hi <Name>,

I read your <specific recent story> on <topic>. <One sentence why DutyPe is relevant to that beat.>

What we've done in the last <N> months in <city>:
- N1 active blue-collar workers across <wedge pincodes>.
- N2 jobs filled.
- N3 employers (X% repeat-post rate at 30 days).
- N4 paid out as referrals + earnings tracked in-app.
- 0 agency commission. Phone OTP + QR work-start trust stack.

What's interesting:
- <One angle: e.g., "women cooks in Madhapur are switching from agency to direct hire." >

What I can offer:
- Numbers, screenshots of the dashboard, intros to 3 workers + 3 employers willing to be interviewed.
- 30-min call this week.

— <Founder name>
DutyPe · dutype.in · <phone>
\`\`\`

## Cadence

- Maximum 1 pitch / week.
- Maximum 3 outlets in flight at any time.
- Wait 7 days for reply before second outlet on same story.
- If 0 replies after 5 different journalists → story angle is wrong, not the journalists. Re-write.

## What NOT to do

- Do not pay for "press release distribution."
- Do not pitch a journalist by tagging them on Twitter/X.
- Do not send the same pitch to multiple journalists on the same day.
- Do not invite a journalist to a "company event" — they don't go.
- Do not embargo a story — bootstrapped startups don't have leverage to embargo.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
