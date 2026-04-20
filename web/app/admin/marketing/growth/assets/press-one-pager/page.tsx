import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Press One-Pager (use ONLY after eligibility gate in \`playbooks/press_pitch.md\`)

> Single A4 page. PDF version sent as attachment when a journalist requests detail. Do NOT paste this into the first cold pitch (that's the 6-line message in the playbook).

\`\`\`
DutyPe — Hyperlocal hiring for blue-collar India
Press one-pager · {{month YYYY}} · For media use

CONTACT
{{Founder Name}}, Founder
+91 91217 06236 (WhatsApp / call)
dutypein@gmail.com
KGPV INNOVATION SOLUTIONS PRIVATE LIMITED, Hyderabad

THE STORY IN ONE LINE
DutyPe is a Telugu-first, hyperlocal hiring app that lets blue-collar workers
in Hyderabad find verified jobs within 5 km of home — without paying any
agency commission.

THE NUMBER THAT MATTERS
{{N}} workers across {{pincodes}} have been hired in the last {{period}},
moving ₹{{amount}} from agency middlemen back to the workers themselves.

THREE THINGS THAT MAKE US DIFFERENT
1. Phone-OTP verified employers + a server-side fraud-score system that auto-rejects
   suspicious "earn ₹50,000 from home" job posts.
2. A QR-based "work-start" scan that creates an attendance record for both worker
   and employer — useful when payment disputes arise.
3. Telugu-first interface and Telugu-language WhatsApp support — for a city
   where most blue-collar hiring conversations happen in Telugu.

THE PEOPLE BEHIND IT
{{Founder Name}}, {{title / background, 1 line}}.
{{Co-founder / team, 1 line each.}}

THREE INTERVIEWS WE CAN ARRANGE
- A cook from {{pincode}} who switched from a ₹4,000-commission agency to DutyPe.
- A {{NGO}} community-leader who runs onboarding camps with us.
- A PG owner in {{area}} who fills cleaning roles in 6 hours instead of 3 days.

THINGS WE WILL NOT CLAIM
- We are not the largest. We are not pan-India. We are not background-verified.
- Our worker LTV is small. Our team is small. We are bootstrapped.
- We are not solving "blue-collar India" — we are solving 3 pincodes in Hyderabad,
  on purpose.

ASSETS AVAILABLE ON REQUEST
- Founder photo (high-res, square + landscape).
- App screenshots (Play Store grade).
- Worker / employer testimonial videos (Telugu + English subtitles).
- One redacted dashboard screenshot (no PII).

KEY DATES
- Founded: {{date}}.
- App live: {{date}}.
- First 1 000 hires milestone: {{date}}.
- Telugu localisation shipped: {{date}}.

EMBARGO POLICY
We don't operate embargoes. Numbers shared in this one-pager are good for
publication on receipt.
\`\`\`

## Update cadence

- Refresh the numbers at the start of every month.
- Refresh the interview list quarterly (or as availability changes).
- Re-confirm consent for each interview subject before sharing their name with a journalist.

## Distribution

- Sent ONLY in reply to a journalist who asked for more detail after the initial 6-line cold pitch (template in \`playbooks/press_pitch.md\`).
- Never posted publicly on dutype.in (that's marketing copy, this is press copy).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
