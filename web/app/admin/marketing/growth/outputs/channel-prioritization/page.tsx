import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Channel Prioritization

> Brutal scoring. Effort and cost are calibrated to a **2-person founding team with < ₹50 K / month marketing budget**. Adjust if the founder confirms more.

## Scoring legend
- **Fit (1–5)**: how well the channel matches a hyper-local blue-collar marketplace at this stage.
- **Effort (1–5)**: 1 = an hour a week, 5 = full-time hire required.
- **Cost (1–5)**: 1 = free, 5 = ₹1 L+ / month.
- **Time-to-signal (TTS)**: how fast you'll know if it works.
- **Verdict**: NOW / LATER (after wedge proven) / NEVER (for current stage).

---

## Tier S — Do NOW (next 30 days)

### 1. Founder-led offline supply hunt in 3 pincodes
- **Fit 5 / Effort 4 / Cost 1 / TTS: 1–2 weeks.**
- *Why it works:* The bottleneck is supply density per pincode. Nothing acquires sticky workers faster than a person at the auto-stand at 7 am with a Telugu pamphlet and 200 ₹250 incentives.
- *Why it can fail:* founder gives up after week 2; falls in love with a screen instead. Discipline matters.
- *Tactic:* 4 mornings/week × 2 hours, 50 conversations / morning, target 60% install + 30% activation.
- *Verdict:* **NOW.** This is the only thing that matters in weeks 1–6.

### 2. RWA / society WhatsApp partnership program
- **Fit 5 / Effort 3 / Cost 1 / TTS: 2–3 weeks.**
- *Why it works:* Demand pre-aggregated. One admin = 200–500 households.
- *Why it fails:* Admins protective of their groups; will not allow spam. Approach as partner, not advertiser.
- *Tactic:* Identify 30 society admins in wedge pincodes (use MyGate / NoBroker city pages, Facebook). Founder-led 1-on-1 message offering ₹500 per 10-signup tier. Provide them a co-branded message template.
- *Verdict:* **NOW.**

### 3. Referral loop polish + amplification
- **Fit 5 / Effort 2 / Cost 2 / TTS: 1–2 weeks.**
- *Why it works:* Already built (₹25 + tier bonuses). Workers in this segment trust people more than ads.
- *Why it can fail:* Existing dual-write referral persistence bug (noted in your docs) — fix before pushing volume, or you'll burn trust paying late.
- *Tactic:*
  - Fix referral-code persistence bug.
  - Add Telugu video of a real worker who earned ₹250 + 5 milestones.
  - Pre-seeded WhatsApp share template in worker's phonebook flow.
  - Push 1 notification per Sunday: "Your friend ₹X earned this week. Share now."
- *Verdict:* **NOW.**

### 4. Founder-led WhatsApp outbound to 200 small employers
- **Fit 4 / Effort 3 / Cost 1 / TTS: 2 weeks.**
- *Why it works:* You can show them a worker availability map *of their pincode* — that's a hook no other product offers.
- *Why it fails:* WhatsApp account ban risk if you spam without warmup; need 80 char personalized first messages, not templates.
- *Tactic:* See \`outbound_sequences.md\` "Employer WhatsApp Flow A".
- *Verdict:* **NOW.** Must wait until ICP-1 supply density is real (≥ 100 active workers in pincode), else you're selling vapor.

### 5. ASO (Play Store listing) overhaul
- **Fit 5 / Effort 2 / Cost 1 / TTS: 4–6 weeks.**
- *Why it works:* Free organic install volume is the ROI king when done right. Your current listing is generic; you have category + city long-tail to claim.
- *Tactic:* Rewrite title, short description, long description, and screenshots for "cook jobs Hyderabad" / "maid jobs Hyderabad" wedge first. See \`app_store_growth_plan.md\`.
- *Verdict:* **NOW.**

---

## Tier A — Do once Tier S is producing signal (weeks 5–12)

### 6. Hyper-local SEO / Programmatic city × category landing pages
- **Fit 5 / Effort 3 / Cost 1 / TTS: 8–12 weeks.**
- *Why it works:* "cook jobs near me Gachibowli" has searchers; ranking is achievable for long tail; you already have Next.js for SSR.
- *Tactic:* 17 categories × top 30 Hyderabad pincodes = 500 pages. Generate from Cloud Function with real availability count + last-week's posts. See \`landing_page_recommendations.md\`.
- *Verdict:* **NOW (technical setup) → traffic in week 6+.**

### 7. NGO / SHG (Self-Help Group) partnership
- **Fit 4 / Effort 3 / Cost 2 / TTS: 6–8 weeks.**
- *Why it works:* Trusted institutions in your supply demographic. SHGs literally exist to help women find dignified work.
- *Tactic:* Identify SAATH, Hand-in-Hand, Aajeevika Bureau Hyderabad, NULM (urban livelihood mission). Offer co-branded onboarding camp + revenue-share on hires.
- *Verdict:* **Tier A.**

### 8. Naka point physical activation
- **Fit 4 / Effort 4 / Cost 2 / TTS: 4 weeks.**
- *Tactic:* Sundays at the 5 biggest naka points in Hyderabad with a folding table, 2 promoters, 100 incentive vouchers.
- *Verdict:* **Tier A** (after pamphlet messaging is validated in Tier S item 1).

### 9. App-install-only smart referral via SMS to lapsed installers
- **Fit 3 / Effort 2 / Cost 2 / TTS: 2 weeks.**
- *Tactic:* Workers who installed but never completed profile (you can read this from Firestore). Send 1 SMS "₹50 reward for completing profile this week."
- *Verdict:* **Tier A.**

### 10. Apartment notice board posters / lift advertising in 30 societies
- **Fit 4 / Effort 2 / Cost 3 / TTS: 4 weeks.**
- *Cost:* ~₹2 000 / lift / month × 30 lifts = ₹60 K / month. Skip until budget exists; replace with RWA partnerships first (free).
- *Verdict:* **Tier A**, gated on budget.

---

## Tier B — Test small, scale if it works (months 3–6)

### 11. Local-language YouTube short-form (real workers, real testimonials)
- **Fit 3 / Effort 4 / Cost 2.**
- Telugu YouTube has the right audience. Production cost is low if you film real users.
- 1 video / week, distributed inside WhatsApp groups + as Play Store promo videos.
- *Verdict:* **B**, after first 5 organic testimonials exist.

### 12. Quora / Reddit answer capture for "best app for maid in Hyderabad"
- **Fit 2 / Effort 2 / Cost 1.** Limited search volume. Worth a 1-week sprint, not more.

### 13. PR placement in Telugu print + local TV
- **Fit 2 / Effort 4 / Cost 4.** PR converts only after a real story (1 000 households used it, x livelihoods earned). Defer until you have a story.

### 14. Influencer / micro-creator (regional Telugu)
- **Fit 2 / Effort 3 / Cost 4.** Expensive, hard to attribute, audience overlap with workers is weak; Telugu YouTube housewives may convert for the *employer* side. Test 1 small creator only after #1–10.

### 15. Partnerships with apartment management apps (MyGate / NoBroker / Apartment Adda)
- **Fit 4 / Effort 4 / Cost 3.** Strategically powerful but you'll need 6–12 months of sales work and you'll be a small fish. **Worth a single founder-to-founder reach-out now**, but no plan dependency.

---

## Tier C — DO NOT DO at this stage

### Meta / Google paid acquisition for workers
- **Verdict: NEVER, until LTV is proven.**
- *Why:* CAC for blue-collar workers in Tier-1 India is ₹40–₹150 per qualified install via Meta. With AdMob ARPU of < ₹5 per active user and no paid monetization, every install loses money. You cannot ad-spend your way to PMF in this category.

### Meta / Google paid acquisition for employers
- **Verdict: LATER**, only after a verified-employer paid tier exists at ≥ ₹299 / month and CAC payback < 60 days. Today: **NEVER.**

### LinkedIn outbound to workers
- Wrong demographic. Skip.

### Generic content marketing (blog SEO for "blue collar jobs in india")
- Will take 9–18 months for a brand-new domain. Founder time better spent.

### Twitter / X for B2C
- Audience overlap ≈ 0%. Skip entirely.

### Influencer marketing on Instagram for workers
- Workers are not on Insta in numbers that move the needle for *this* segment in *this* city.

### National TV / OOH (out-of-home billboards)
- Budget required ₹50 L+; brand-awareness ROI for an unfunded marketplace is ~zero.

---

## Channel decision tree (quick)

\`\`\`
Is supply density (workers/pincode) ≥ 100 in target pincode?
├─ NO  → Tier S items 1, 3, 5 only. Nothing else.
└─ YES → Add Tier S items 2 + 4. Open Tier A items 6 + 7.
        │
        ├─ Is employer post→fill rate ≥ 50%?
        │   ├─ NO  → Stop selling to more employers. Fix supply gaps in pincode.
        │   └─ YES → Open Tier B (paid + content amplifying real success).
        │
        └─ Is monetization unit-economic positive?
            ├─ NO  → No paid acquisition channels.
            └─ YES → Cautiously test Meta/Google for employer side ONLY.
\`\`\`

---

## What to do this week

1. Pick the 3 pincodes (founder decision today).
2. Print 500 Telugu pamphlets (cost ≈ ₹1 200).
3. Identify 30 RWA admins (1 founder day on Facebook + MyGate).
4. Fix referral persistence bug (engineering, 1–2 days).
5. Rewrite Play Store listing for the wedge (1 day, see \`app_store_growth_plan.md\`).
6. Build the 200-employer prospect list for outbound (1 founder day on Justdial + Google Maps).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
