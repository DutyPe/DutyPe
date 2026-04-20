import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Push Notification Copy

> Telugu + English variants for every lifecycle trigger. Keep titles ≤ 40 chars, body ≤ 110 chars (Android collapses past this on most launchers).

| Trigger | Lang | Title | Body | Deep link |
|---|---|---|---|---|
| Worker D1 install | TE | DutyPe ki welcome 🙏 | Profile complete cheyyandi → mee area lo jobs apply cheyochu. | \`dutype://profile\` |
| Worker D1 install | EN | Welcome to DutyPe | Complete your profile to apply for verified jobs near you. | \`dutype://profile\` |
| Worker D2 incomplete profile | TE | Profile 60% complete | 3 nimishaalu lo finish cheyandi → ₹50 unlock. | \`dutype://profile\` |
| Worker D7 incomplete profile | TE | ₹50 unlock cheyandi | Profile complete cheyandi → ₹50 + jobs near you. | \`dutype://profile?reward=50\` |
| Worker D7 first job | TE | Mee area lo {{N}} jobs | Tap chesi apply cheyandi. Free ee. | \`dutype://feed?pincode={{p}}\` |
| Worker new job in pincode | TE | New job 2 km lo | {{title}} — ₹{{salary}}/day. Apply now. | \`dutype://job/{{id}}\` |
| Worker application accepted | TE | Application accepted! | {{employer}} mee application accept chesaru. Tap to chat. | \`dutype://application/{{id}}\` |
| Worker D30 inactive | TE | {{N}} new jobs this week | Mee pincode lo new jobs. Apply chesi {{income}} earn cheyandi. | \`dutype://feed\` |
| Employer D1 no post | EN | Post your first job | {{N}} workers active in {{pincode}}. Post free in 2 minutes. | \`dutype://post-job\` |
| Employer D1 no post | TE | First job post cheyandi | {{N}} workers mee area lo. 2 nimishaalu lo post cheyandi. | \`dutype://post-job\` |
| Employer post-no-applications-24h | EN | Want more applications? | Increase salary by ₹{{X}} or expand radius to 5 km. | \`dutype://post/{{id}}/edit\` |
| Employer applications received | EN | {{N}} workers applied | Tap to review and shortlist. | \`dutype://post/{{id}}/applications\` |
| Employer post-fill D7 | EN | How was {{worker_name}}? | 1-tap rating. Helps us find you better matches. | \`dutype://post/{{id}}/rate\` |
| Employer D30 repeat-post | EN | Need to hire again? | Post free in 2 minutes. Same area, fresh applications. | \`dutype://post-job\` |
| Referral milestone hit | TE | ₹{{X}} unlock! | {{N}} friends joined → bonus unlock. Withdraw at ₹50. | \`dutype://wallet\` |
| Worker first hire | TE | Congrats! Mee first job 🎉 | Tomorrow {{employer}} dagara start cheyandi. QR scan cheyandi. | \`dutype://hire/{{id}}\` |
| Worker QR-out | TE | Job complete! | {{employer}} payment confirm cheyali. WhatsApp lo follow up cheyandi. | \`dutype://hire/{{id}}\` |

## Frequency caps

- Maximum 1 push per user per 24h.
- Maximum 4 push per user per week.
- Quiet hours: 22:00–08:00 IST.
- Do not push to a user who has had ≥ 3 consecutive un-opened pushes (auto-suppress for 14 days).

## A/B opportunities

- Reward-led vs job-led title for D7 incomplete-profile workers (current top hypothesis).
- Telugu-only vs Telugu+English mixed copy (test which converts higher per cohort).
- Number-led ("{{N}} jobs") vs benefit-led ("Earn this week") for inactive workers.

## Source-of-truth file in code

Notification strings should mirror this file in \`app/src/main/res/values-te/strings.xml\` and \`values/strings.xml\`. Any change here → corresponding XML PR. Do not let the two drift.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
