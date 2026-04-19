# DutyPe – Growth Experiments Backlog

> Every experiment must have a **kill metric**. If you cannot define a number that would make you stop, you're not experimenting; you're hoping. Hope is not a strategy.
>
> Each experiment is timeboxed. Founder reviews backlog every Friday and decides: KEEP / KILL / SCALE.

---

## Backlog format

```
EXP-XX  Name
  Hypothesis        : Concrete, falsifiable.
  Channel           : Where it runs.
  Audience          : Specific ICP segment.
  Asset needed      : What we ship before we run.
  Cost              : ₹ + hours.
  Effort            : H / M / L.
  Run duration      : weeks.
  Success metric    : The number that says "scale."
  Kill metric       : The number that says "stop."
  Decision if works : Concrete next step.
  Decision if fails : Concrete next step.
```

---

# QUICK WINS — 7 days each

## EXP-01  Naka pamphlet drop in 1 pincode
- **Hypothesis:** A Telugu pamphlet with QR + ₹250 incentive at one naka point produces ≥ 30% install rate among recipients.
- **Channel:** Offline, naka labour point in pincode 500032 (Gachibowli) or 500081 (Madhapur).
- **Audience:** Female household workers, 22–45.
- **Asset:** 500 pamphlets (₹1.2 K), 1 promoter (founder), 1 banner.
- **Cost:** ₹2 K + 8 hours founder time.
- **Effort:** M.
- **Run:** 4 mornings, 2 hours each.
- **Success metric:** ≥ 60 installs (≥ 30% of 200 hands handed); ≥ 25 profile-completions.
- **Kill metric:** < 20 installs after 4 mornings.
- **If works:** Scale to 3 nakas, 4 mornings/week each.
- **If fails:** Test the *message* (commission-fear vs ₹250 incentive headline) before killing channel.

## EXP-02  RWA partnership — 1 society
- **Hypothesis:** A founder-personal pitch to one RWA admin can generate ≥ 10 employer signups in 7 days.
- **Channel:** In-person + WhatsApp follow-up.
- **Audience:** RWA admin of a 200+ unit complex in wedge pincode.
- **Asset:** RWA pitch one-pager + co-branded WhatsApp message + Telugu poster.
- **Cost:** ₹500 + 4 hrs founder.
- **Effort:** L.
- **Run:** 7 days from first meeting.
- **Success metric:** ≥ 10 resident installs; ≥ 3 jobs posted.
- **Kill metric:** < 3 installs after 7 days.
- **If works:** Repeat with 5 RWAs in next 14 days.
- **If fails:** Diagnose — was message wrong, or is the channel wrong? RWA admins are the right channel; if message is wrong, iterate copy.

## EXP-03  Play Store ASO rewrite
- **Hypothesis:** Wedge-specific ASO rewrite (title + screenshots + description) increases store-listing-visitor → install conversion by ≥ 30 %.
- **Channel:** Play Console.
- **Audience:** Organic Play Store visitors.
- **Asset:** New title, description, 8 screenshots, feature graphic, Telugu localisation.
- **Cost:** ₹0 (founder + designer time, ~10 hours).
- **Effort:** M.
- **Run:** 14 days post-publish.
- **Success metric:** Conversion rate (visitors → installs) up by ≥ 30 % WoW.
- **Kill metric:** Conversion rate flat or down after 14 days.
- **If works:** Continue iterating screenshots A/B.
- **If fails:** Founder writes 5 more variants of headlines and screenshots. Re-test.

## EXP-04  Reactivation push to lapsed installers
- **Hypothesis:** A targeted notification + WhatsApp + email to workers who installed but never completed profile, offering ₹50 to complete, can reactivate ≥ 10 % of them.
- **Channel:** FCM push + WhatsApp + email (via Cloud Function).
- **Audience:** Workers with `profile_complete < 80%` for ≥ 7 days.
- **Asset:** Push copy (Telugu + English), Cloud Function script, ₹50 credit issuance flow.
- **Cost:** Engineering 4 hrs + ₹X reward budget.
- **Effort:** M.
- **Run:** 7 days.
- **Success metric:** ≥ 10 % of segment completes profile within 7 days.
- **Kill metric:** < 3 % completion lift over baseline.
- **If works:** Make this an automated lifecycle journey (D7, D14, D30).
- **If fails:** Test (a) higher reward, (b) different copy, (c) WhatsApp first instead of push.

## EXP-05  Founder WhatsApp outbound to 50 PG owners
- **Hypothesis:** Personalised cold WhatsApp to 50 PG owners in wedge pincode produces ≥ 8 employer accounts and ≥ 3 paid-equivalent posts.
- **Channel:** Founder's business WhatsApp (warmed up).
- **Audience:** PG owners in 500032 / 500081, scraped from Google Maps.
- **Asset:** Warmed WhatsApp number, prospect list (CSV), opening message + 2 follow-ups (see `outbound_sequences.md` B2).
- **Cost:** ₹0 + 6 hrs founder.
- **Effort:** M.
- **Run:** 7 days.
- **Success metric:** ≥ 8 employer accounts created.
- **Kill metric:** < 3 accounts after 50 messages.
- **If works:** Scale to 200 prospects in next 14 days.
- **If fails:** Re-write opener, test new variants on next 50.

---

# MEANINGFUL TESTS — 30 days each

## EXP-06  Programmatic SEO pages — 250 pages launch
- **Hypothesis:** 250 city × pincode × category SEO pages will produce ≥ 100 organic installs/month within 60 days of indexing.
- **Channel:** Organic search.
- **Audience:** Long-tail searchers (e.g. "cook job in gachibowli").
- **Asset:** Page template, Cloud Function for live counts, sitemap.xml entry, JobPosting schema.
- **Cost:** Engineering 1 week.
- **Effort:** H.
- **Run:** 30 days post-publish (60 days for full SEO read).
- **Success metric:** ≥ 5 K monthly impressions in Search Console; ≥ 100 attributable installs.
- **Kill metric:** < 500 impressions after 30 days.
- **If works:** Add 750 more pages (other cities) over 60 days.
- **If fails:** Audit page quality (duplicate content? thin pages? `noindex` accidentally on?). Fix before adding more.

## EXP-07  Telugu YouTube Shorts series — 4 reels
- **Hypothesis:** 4 Telugu testimonial Shorts will produce ≥ 1 K views each and ≥ 50 attributable installs in 30 days.
- **Channel:** YouTube Shorts (DutyPe Telugu channel).
- **Audience:** Telugu-speaking blue-collar workers in Telangana / AP.
- **Asset:** 4 raw 30-sec testimonials filmed at naka or society, edited.
- **Cost:** ₹0 (founder + worker subjects).
- **Effort:** M.
- **Run:** 30 days.
- **Success metric:** ≥ 4 K total views, ≥ 50 installs attributed.
- **Kill metric:** < 500 views per video, < 5 installs.
- **If works:** Move to weekly cadence + add Instagram cross-post.
- **If fails:** Diagnose — bad subject, bad hook, wrong channel? Test Instagram-first format.

## EXP-08  NGO / SHG cohort onboarding pilot
- **Hypothesis:** Partnering with one NGO for a 50-woman cohort produces ≥ 30 activations and ≥ 10 successful first hires in 30 days.
- **Channel:** Field event + WhatsApp.
- **Audience:** Cohort women referred by NGO.
- **Asset:** Onboarding camp script, ₹100 sign-up incentive × 50, photographer for testimonials.
- **Cost:** ₹6 K incentives + ₹2 K event + 12 hrs founder.
- **Effort:** H.
- **Run:** 30 days.
- **Success metric:** 30 activated workers, 10 confirmed hires, 1 case study.
- **Kill metric:** < 10 activations after 30 days.
- **If works:** Replicate with 3 more NGOs in next 60 days; raise to a recurring monthly cadence.
- **If fails:** Was it the NGO selection or our onboarding? Try one different NGO before killing the channel.

## EXP-09  Referral loop amplification post-bug-fix
- **Hypothesis:** Fixing the dual-write referral persistence bug + adding a Telugu video + a Sunday push will lift weekly referrals/active-worker by ≥ 2×.
- **Channel:** In-app + push + WhatsApp share.
- **Audience:** Active workers with ≥ 1 friend in their phonebook.
- **Asset:** Bug fix, Telugu video, push schedule, share-link template.
- **Cost:** Engineering 3 days + ₹0.
- **Effort:** M.
- **Run:** 30 days.
- **Success metric:** Referrals/active-worker/week ≥ 2× pre-launch baseline.
- **Kill metric:** No movement after 30 days.
- **If works:** Add referral leaderboard + monthly bonus winner.
- **If fails:** Audit funnel — share clicked but install didn't happen? Install but didn't redeem? Fix the blocker.

## EXP-10  Pricing test — promoted listing for employers
- **Hypothesis:** ₹99 / ₹199 / ₹499 promoted-listing options will get ≥ 5 % of active employers to pay something.
- **Channel:** In-app upsell on the post-job confirmation screen.
- **Audience:** Employers who just posted a free job.
- **Asset:** Pricing page, Razorpay integration, 3 price-point screens for A/B.
- **Cost:** Engineering 1 week (Razorpay integration), ₹0 marketing.
- **Effort:** H.
- **Run:** 30 days.
- **Success metric:** ≥ 5 % conversion at any price point; ARPU contribution ≥ 1 % of total monthly cost.
- **Kill metric:** < 1 % conversion across all 3 price points.
- **If works:** Decide which price point + iterate; this becomes the unit-economics unlock for paid acquisition later.
- **If fails:** **Critical learning** — paid features are not perceived as valuable yet. Don't push paid acquisition at all.

---

# SCALABLE MOTIONS — 60–90 days each

## EXP-11  City expansion — Vijayawada or Bangalore?
- **Hypothesis:** A second city can be opened with 30 % less effort than city-1 if we use the playbook from city-1 and pick by Telugu-population density (Vijayawada) or job-search demand (Bangalore).
- **Channel:** Repeat of Tier S items 1–5 in new city.
- **Cost:** ₹50 K + 60 hrs founder/ops.
- **Effort:** H.
- **Run:** 90 days.
- **Success metric:** Match city-1's 90-day metrics on supply density and first-applicant time.
- **Kill metric:** Less than 50 % of city-1's metrics at day 60 → close city, regroup.
- **Decision dependency:** **Do not start until city-1 has ≥ 500 active workers in 3 pincodes AND ≥ 20% employer repeat-post rate.**

## EXP-12  Vertical expansion — add Driver category
- **Hypothesis:** Driver hiring (personal, school, small fleet) has stickier employer LTV than cook/maid in our wedge city.
- **Channel:** Same playbook, different category.
- **Asset:** Driver-specific pamphlet, RWA partnership re-pitched for driver, school WhatsApp groups outbound.
- **Cost:** ₹15 K + 30 hrs.
- **Run:** 60 days.
- **Success metric:** Driver category ≥ 30 % of employer posts in pincode within 60 days; first-applicant time < 12 hrs.
- **Kill metric:** Driver supply density < 50 in pincode after 30 days → de-prioritize, focus back on cook/maid.

## EXP-13  Apartment-management partnership (MyGate / NoBroker / Apartment Adda)
- **Hypothesis:** A formal integration or co-marketing with one apartment management platform will 5× employer signups.
- **Channel:** Founder-led B2B sales.
- **Audience:** MyGate / NoBroker partnerships team.
- **Asset:** Founder pitch deck (6 slides max), one-pager on user data we'd share back.
- **Cost:** 60 hrs founder over 90 days.
- **Run:** 90 days.
- **Success metric:** Signed pilot with at least one platform.
- **Kill metric:** No response to outreach + 3 follow-ups across 90 days.
- **Decision:** Even one signed pilot = transformative. Worth the bet *in parallel* with EXP-01–05, not at the cost of them.

## EXP-14  Verified-employer paid badge
- **Hypothesis:** A ₹299 one-time verified-employer badge will be bought by ≥ 10 % of employers with ≥ 2 hires.
- **Channel:** In-app + email post-second-fill.
- **Asset:** Badge design, verification flow (Aadhaar / GST), Razorpay integration.
- **Cost:** Engineering 2 weeks.
- **Run:** 60 days.
- **Success metric:** ≥ 10 % conversion of eligible employers; revenue ≥ ₹50 K / month within 60 days.
- **Kill metric:** < 3 % conversion → either price or value is wrong; iterate or kill.
- **Dependency:** Only run after EXP-10 shows pricing willingness exists.

## EXP-15  Local-language SMS retargeting via dropped installs
- **Hypothesis:** Workers who clicked the Play Store link but didn't install will install at ≥ 8 % if SMS-retargeted in Telugu within 24 h.
- **Channel:** SMS via Gupshup / MSG91.
- **Audience:** Phone numbers captured from "get app link" form on /worker, who didn't install.
- **Asset:** SMS template (Telugu + English), Cloud Function trigger.
- **Cost:** ₹0.05 / SMS × 5 K = ₹250 / month.
- **Run:** 30 days.
- **Success metric:** ≥ 8 % install rate within 7 days of SMS.
- **Kill metric:** < 2 % install lift.
- **If works:** Make automated; expand to "install but no profile" segment via SMS.
- **If fails:** SMS deliverability or content is the issue; test 3 more variants.

---

## Backlog parking lot (don't run yet, revisit Q3)

- Meta lead ads to households (only after EXP-10 + EXP-14 prove paid willingness).
- Google Search ads on "maid agency hyderabad" branded competitors (only after CAC payback < 60 days).
- Instagram influencer (Telugu housewife creator) — defer until 5 organic testimonial reels exist.
- Press / Eenadu story — defer until 1 000 jobs filled milestone.
- Aadhaar e-KYC for worker side — defer until employer demand for verified workers is provable.
- Worker-side training partnerships (NSDC / Pradhan Mantri Kaushal Vikas) — strategic, defer until Q3.

---

## Operating discipline

- **Maximum 3 experiments running at once.** Beyond that, attribution is impossible and founder context-switching kills execution.
- **Friday review = decision day.** Every experiment gets KEEP / KILL / SCALE.
- **No experiment runs > 30 days without a midpoint check.** Most failures are visible by day 14.
- **Kept experiments must produce a written 1-pager** (what we tested, what happened, what we learned, what we'll do next).
- **Killed experiments get logged** so we don't accidentally re-test the same dead idea in 6 months.
