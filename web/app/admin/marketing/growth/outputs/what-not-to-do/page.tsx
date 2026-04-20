import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – What NOT To Do

> The most expensive mistakes for an unfunded marketplace are not commission errors; they are channels that look like growth and aren't. This document is the discipline file.
>
> Read this **before** every Monday standup.

---

## 1. Do NOT run paid Meta / Google ads to acquire workers

- Worker LTV today is a few rupees of AdMob revenue + indirect contribution to employer experience.
- CAC for a blue-collar worker via Meta in tier-1 India is ₹40–₹150 per *install*, not per activation.
- You will burn 6 months of runway in 6 weeks and still not know if PMF exists.
- Reopen this conversation only when (a) a paid monetisation tier exists, (b) it converts ≥ 5 % of active employers, (c) per-pincode unit economics are positive.

## 2. Do NOT run paid ads to acquire employers either, until pricing is proven

- Even the cheapest small-employer SMB has a per-lead cost of ₹150–₹500 on Meta lead-gen.
- Without a paid feature to offset CAC, every paying employer loses money.
- Open after EXP-10 and EXP-14 prove paying willingness.

## 3. Do NOT launch in a 4th pincode while pincodes 1–3 have < 500 active workers each

- Density is destiny in a local marketplace.
- Spreading thin = supply shortage everywhere = bad employer experience everywhere = uniform churn.
- Focus is the only weapon a small marketplace has against a big one.

## 4. Do NOT launch a 2nd city before city-1 hits the 3 gates

Gates (from \`weekly_execution_plan.md\`):
1. ≥ 500 active workers across 3 wedge pincodes.
2. ≥ 20 % employer 30-day repeat-post rate.
3. ≥ 5 % paying employers (or equivalent monetisation signal).

Launching city-2 prematurely doubles support cost and halves operator focus.

## 5. Do NOT add a 3rd category before cook + maid hit ≥ 60 matches/week

- 17 categories is already too many for a 2-person team to operate.
- Hide non-wedge categories in the Play Store description until the wedge proves out.
- The temptation to "open everything to everyone" is the #1 marketplace failure mode.

## 6. Do NOT build a recruiter / staffing-agency dashboard

- It contradicts the entire "no commission, direct" positioning.
- It makes you a small player in a market controlled by larger HR-Tech players.
- It diverts engineering from the mobile-first hyper-local proposition.

## 7. Do NOT build iOS until Android Hyderabad pincode density is real

- ~95 % of your target ICP is on Android budget devices.
- iOS users skew higher-income — a different ICP, different unit economics, different positioning.
- 6 engineering months on iOS = 6 months of losing the local-density race.

## 8. Do NOT chase influencer marketing on Instagram for workers

- Audience overlap with cook / maid / helper segment is near zero.
- Spend is high (₹15–50 K per micro-creator post for unverifiable installs).
- Telugu YouTube *worker testimonial* is a different beast and is allowed (it's content, not influencer-paid).

## 9. Do NOT pay for "PR placement" or paid press

- Paid press in Indian tech outlets converts ~zero installs in this segment.
- Earned press from a real story (X livelihoods, Y rupees redistributed) is worth 100× more.
- Defer all press until 1 000 jobs filled milestone.

## 10. Do NOT outsource founder LinkedIn or X to an agency

- Investors and partners can spot ghost-written content in 2 posts.
- Founder authenticity is the entire moat of the LinkedIn channel for an unfunded company.
- Either the founder posts personally or no one posts.

## 11. Do NOT add cosmetic features to the app instead of fixing acquisition

- Every "small UI polish" sprint is a sprint not spent in the field.
- The product is good enough to test. The market is the unknown, not the UI.
- Open feature work only when a measured friction blocks ≥ 10 % of activations.

## 12. Do NOT translate or expand to Hindi UI before Telugu wedge proves

- Telugu localisation is already shipped — use the asymmetric advantage.
- Hindi is a much bigger market with much bigger competition (Apna, WorkIndia, Job Hai).
- Expand to Hindi in city-3 onwards, not earlier.

## 13. Do NOT spam WhatsApp at scale without warm-up

- Cold-spammed numbers get reported in 24 hours.
- A banned business WhatsApp number takes weeks to recover.
- Stay under 50 messages / day on a new number for the first 3 weeks.

## 14. Do NOT promise things you cannot prove

Avoid in marketing copy:
- "Background-verified workers" (you do phone OTP, not BGV).
- "Insurance covered" (you don't).
- "Salary guarantee" (you don't).
- "Most trusted hiring app" (you can't measure this).
- "Used by lakhs" until lakhs is true.

Promising features you don't have = 1-star reviews + Play Store policy violation risk + erosion of the trust moat you're building.

## 15. Do NOT confuse referral payouts with traction

- Referrals work because both sides win. They are not "growth" if the referred user doesn't activate.
- Watch *referred-user 30-day retention*, not *referrals issued*.
- Cap individual referral earnings (you already do at tiers) — anyone bypassing cap = fraud.

## 16. Do NOT add a chat feature inside the app

- Worker ↔ employer chat opens the door to fraud, abuse, and scope creep.
- WhatsApp already exists. Lean into it (deep-link to WhatsApp from the application screen).
- Codebase notes that chat was already removed (\`docs/CHAT_REMOVAL_COMPLETE.md\`); don't re-introduce it.

## 17. Do NOT accept enterprise deals that compromise the core product

- A staffing agency wanting "white-label DutyPe" is not a customer — it's a contradiction.
- A corporate HR head wanting "post 200 jobs at 50% off" makes your supply-side dynamic worse.
- Keep enterprise out for the first 12 months. Re-evaluate when you have leverage.

## 18. Do NOT build a referral leaderboard before fixing the referral persistence bug

- A leaderboard with broken payouts = public proof that you don't pay reliably.
- Fix bug first, leaderboard second.

## 19. Do NOT spend founder time at startup events / conferences

- Zero of your ICP attends startup events.
- Time at conferences = time not at nakas / RWAs / outbound.
- Allow yourself one event per quarter, *only* if there is a specific introduction or fundraising agenda.

## 20. Do NOT confuse "we got 1 000 installs from a YouTube short" with PMF

- Spike installs from viral content have ~10–30 % activation, ~5–10 % retention.
- A spike masks the underlying activation problem.
- Always read **activated cohort retention**, not absolute install counts.

---

## Anti-pattern detector

Every Friday review, ask out loud:

- Did we add a feature to avoid a hard sales conversation?
- Did we open a new channel to avoid fixing an old one?
- Did we ship a "rebrand" because we couldn't ship a metric?
- Did we send a press email because the cohort retention chart looked bad?
- Did we open a 4th experiment because the 3rd one wasn't working and we were afraid to kill it?

If yes to any → that's the only thing to fix the next week.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
