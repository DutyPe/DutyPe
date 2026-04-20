import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Landing Page Recommendations (https://dutype.in)

> Current site is a generic marketplace homepage. It is not built to convert *one specific* visitor — it tries to please everyone. Below is the rebuild plan, wedge-first.

---

## Strategic stance

1. The homepage is **not** the most important page. The **\`/<city>/<category>-jobs\`** programmatic page is. That's where SEO traffic will land.
2. Build for the **first city and first 2 categories only.** Add more pages only when those rank.
3. **Two distinct visitor flows** (worker vs. employer). Force a decision in the hero, don't show both as equal-weight options on a single scroll.
4. Every page must answer: *Why this app, why now, why this city/role, what's the next step*.

---

## Information architecture

\`\`\`
/                                    ← homepage (decision splitter)
/worker                              ← worker landing
/employer                            ← employer landing
/post-job                            ← employer single-page conversion (deep-link to app)
/hyderabad                           ← city hub
/hyderabad/cook-jobs                 ← programmatic SEO
/hyderabad/maid-jobs                 ← programmatic SEO
/hyderabad/<pincode>/<category>      ← long-tail SEO (e.g. /hyderabad/500032/cook-jobs)
/safety                              ← trust hub
/refer                               ← referral explainer (with QR/share)
/help                                ← FAQ + WhatsApp support
/legal/privacy /legal/terms /legal/refund /legal/account-deletion
\`\`\`

---

## Homepage rebuild

### Hero (above the fold, 1 viewport)
- **One question only:** "Are you looking for work, or hiring someone?"
- Two giant buttons:
  - **"I want a job"** → \`/worker\`
  - **"I want to hire"** → \`/employer\`
- **No carousels.** No "Download App" as the primary CTA at the very top.
- Below the fold, single proof line: "X workers active in Hyderabad. Y jobs posted last 7 days."
  - These numbers must be **server-rendered from a Cloud Function reading Firestore**, not hard-coded. Fake numbers will be obvious and kill trust.

### Below hero (one scroll)
- Trust strip: "Phone OTP verified · No agency commission · QR work-start · Telugu support"
- Real testimonial × 3 (real first name, area, headshot, salary outcome). If you don't have these → don't fake them; show "Stories coming soon" with a "Tell us your story" form.
- Footer: Play Store badge, WhatsApp number, support email, legal entity name, sitemap, language toggle.

### What to remove / kill
- Generic "Find your dream job" headline.
- Hero stock photo of "happy diverse workers."
- "How it works in 4 steps" if the steps are generic — replace with a 30-second auto-playing video of one real worker using the app.
- Testimonials without real names + areas.
- Counters that are not server-truthful.

---

## /worker page (hyper-local, conversion-first)

### Hero
- "Find a [cook / maid / helper / driver] job within 5 km of your home in Hyderabad."
- Sub: "Phone OTP signup. Zero agency fees. Telugu interface."
- CTA: **"Install the App – Free"** → Play Store with \`utm_source=site&utm_medium=worker_hero\`.
- Secondary CTA: "Get app link on WhatsApp" (collects phone, sends Play link via Cloud Function).

### Live availability snapshot
- "237 jobs posted this week in Hyderabad."
- "Median time to first reply: 6 hours."
- (Server-rendered. Hide the snapshot if numbers are too low to be impressive — never lie.)

### How it works (visual, 3 steps, screenshots from input/screenshots/)
1. Phone OTP signup (10 sec).
2. Complete profile (3 min) — unlocks applying.
3. Apply to nearby jobs in 1 tap.

### Income proof (when you have it)
- "Workers in Hyderabad earned a median ₹X in their first month on DutyPe."
- "Highest-earning category last month: [X], median ₹Y."

### Trust block
- Phone OTP + scam-keyword filter + QR work-start + Telugu support.
- Embed a 30-sec founder video in Telugu.

### FAQ (real worker objections)
- Will I have to pay anything? — No, never.
- Are these jobs verified? — Phone OTP for every employer; we auto-reject suspicious posts.
- Why ask for my profile photo? — Lets the employer see who's coming. You can hide it from feed.
- I don't speak English. — App is in Telugu. Support is in Telugu on WhatsApp.

### Footer
- Sticky CTA on mobile: "Install free"

---

## /employer page

### Hero
- "Hire a verified [maid / cook / helper] near your [home / business] in 24 hours. Zero agency commission."
- CTA: **"Post a Job — Free"** (deep-links into app post-job flow OR opens web post-job form that creates the post via Cloud Function).
- Secondary: "See workers near me" (lets them enter pincode → returns count + sample profile thumbnails, anonymized).

### Pre-conversion availability widget
- Pincode / area input → "32 cooks, 47 maids available within 5 km of [pincode]."
- This is the killer feature. Apna doesn't show this. WhatsApp groups can't.

### Comparison block
| | DutyPe | Local agency | WhatsApp group | Apna |
|---|---|---|---|---|
| Commission | ₹0 | ₹2-5 K | ₹0 | ₹0 |
| Verified phone | ✅ | ❌ | ❌ | Partial |
| Hyper-local (≤5 km) | ✅ | ❌ | ❌ | ❌ |
| QR work-start | ✅ | ❌ | ❌ | ❌ |
| Telugu support | ✅ | Mixed | – | ❌ |
| Median time to first applicant | 6 h | 3 d | 1–2 d | 18 h |

(Numbers must be defensible. Pull from Firestore weekly via Cloud Function.)

### Testimonials (small employers, named, area)
- "Filled my cook role in 14 hours, saved ₹3 200 in agency fees." — Priya M., Gachibowli
- (3 of these. With photo if consented.)

### How it works
1. Post job in 4 steps (~ 2 min).
2. Get applications today.
3. Pick worker, scan their QR when they start.

### Pricing
- Currently free. State it clearly. State the future plan honestly: "Free for first 100 employers in Hyderabad. After that we may introduce a paid promoted-listing tier."

### FAQ
- How do I know the worker is real? — Phone OTP, profile completion ≥ 80%, area match.
- Do you screen workers? — We verify identity (phone). Skill verification is your decision after interview.
- What if the worker doesn't show up? — Use the QR. If they didn't scan, they didn't show. Mark them no-show; they're penalised in their score.
- Can I post in Telugu? — Yes, the app supports Telugu posting. Web is English-first today.

---

## Programmatic city × category × pincode pages

### Why
- "cook jobs in Gachibowli" / "maid jobs near Madhapur" are searched. They have **buying intent**.
- Apna ranks for top-of-funnel city queries. You can win the **pincode-level long tail** they don't bother with.

### Page template
\`\`\`
URL: /hyderabad/<pincode>/<category>-jobs
Title: <N> <Category> Jobs in <Pincode> (<Area>) – DutyPe
Meta: Apply free for <N> <category> jobs in <area> Hyderabad. Phone-verified employers, no agency commission. Telugu support.

H1: <Category> Jobs in <Area> (<Pincode>), Hyderabad
Hero count: "<N> jobs posted in last 7 days · <M> employers actively hiring"
[3 sample job cards — Cloud Function pulls latest 3 jobs, renders address truncated to area, salary, type]
[Install CTA]
[Comparison: DutyPe vs agency vs WhatsApp groups for THIS area]
[FAQ specific to area: Average salary in <area>, top employers (anonymised), commute info]
[Internal links: other categories in this pincode, this category in nearby pincodes]
\`\`\`

### Generation
- Cloud Function \`landing-page-generate\` (you already have the related stubs in \`functions/src/job-landing.ts\` etc — extend them).
- Render at request time with Next.js \`generateStaticParams\` for top 50 pincodes × top 5 categories = 250 pages day 1.
- ISR: revalidate every 6 hours.

### SEO checklist per page
- Unique title, description, H1.
- Real-time count visible above the fold.
- ≥ 600 words of content (3 sample jobs + FAQ + internal links provide this naturally).
- \`application/ld+json\` JobPosting schema for each sample job (great for Google for Jobs eligibility).
- One H2 mentioning area + category combo.
- One H3 mentioning median salary in area.

### Risk
- Thin pages with 0 jobs hurt SEO. **Hide the page from sitemap and \`noindex\` it if \`<N>\` < 5.** Re-include when density grows.

---

## Conversion mechanics across all pages

### Sticky mobile CTA
- Floating button bottom-right: "Install Free" on worker pages, "Post Free" on employer pages.

### App install attribution
- Every CTA appends \`?utm_source=site&utm_medium=<page>&utm_campaign=<wedge>\`.
- Use Firebase Dynamic Links (or Branch.io free tier) so install → first-open carries UTM into the app, attributing the source.

### Reduce decision-paralysis
- One CTA per viewport. Never two same-weight buttons.
- Worker pages: zero employer copy. Employer pages: zero worker copy. Don't try to convert both on one page.

### Speed
- LCP target < 1.5s on 3G.
- No client-side carousels. SSR everything except the "live count."
- Lazy-load testimonials and FAQ.

### Forms
- Worker phone-collection ("get app link on WhatsApp") = single field, no name, no email. Drop-off after 1 field is brutal in this segment.

---

## Trust / safety page (\`/safety\`)

A dedicated page that the employer-side pre-launch sequence links to. It must answer:
- How are workers verified?
- How are employers verified?
- What happens if there is a dispute?
- How is salary handled? (Be honest: *not* through the app today.)
- Privacy policy in plain English (and Telugu summary).

---

## Referral explainer page (\`/refer\`)

- Big number: "₹25 per friend + tier bonuses up to ₹1 000."
- Live counter (server-rendered): "₹X paid out to workers / employers this month."
- Simple 3-step illustration.
- Big "Open app to share" CTA.

---

## Asset to-do list (in priority order)

1. **Real stat Cloud Function** that returns \`{ active_workers_in_pincode, jobs_last_7d, median_first_reply_minutes, payouts_last_30d_inr }\` for use across pages. (Engineering: 2 days.)
2. **Hero rebuild** — homepage, /worker, /employer, /post-job. (Design + copy: 3 days.)
3. **Programmatic city-category template** + first 250 pages. (Engineering: 1 week.)
4. **3 real testimonial videos** (one worker Telugu, one household woman, one PG owner). (Founder: 4 days, ₹0 if you film yourself.)
5. **Comparison table** asset. (Design: 1 day.)
6. **Install-via-WhatsApp-link Cloud Function** (Twilio not needed; can do via Cloud Function + send link manually for the first 100 to validate). (Engineering: 2 days.)
7. **Sticky mobile CTA** with UTM passthrough. (Engineering: half day.)
8. **Robots.txt + sitemap.xml** with \`noindex\` rule for thin pages. (Engineering: half day.)
9. **Schema.org JobPosting markup** on each programmatic page. (Engineering: 1 day.)

---

## What NOT to add

- A blog (don't unless you commit one weekly post for 6 months — likely a waste of founder time at this stage).
- A chatbot widget (it will distract from the install CTA).
- An AdSense banner (please no).
- Multiple language toggles for languages you don't support yet — only show English / Telugu.
- An "About Us" page with founder photos in suits — not needed yet.
- A press / media kit page — premature.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
