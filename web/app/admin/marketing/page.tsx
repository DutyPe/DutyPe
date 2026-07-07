"use client";

import { useEffect, useMemo, useState } from "react";
import { CsvTable, MarkdownView } from "@/components/marketing-shell";

type CallFeedbackRow = {
  id: string;
  jobId?: string;
  workerId?: string;
  jobTitle?: string;
  companyName?: string;
  spokeWithEmployer?: boolean;
  jobAvailability?: string;
  jobOfferAccepted?: boolean;
  createdAt?: { toDate?: () => Date } | string | number;
};

function CallStatsDisplay() {
  const [totalCalls, setTotalCalls] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function fetchStats() {
      try {
        setIsLoading(true);
        const response = await fetch('/api/stats');
        if (response.ok) {
          const data = await response.json();
          setTotalCalls(data.totalCalls);
        }
      } catch (error) {
        console.error('Failed to fetch call stats', error);
        setTotalCalls(0); // Default to 0 on error
      } finally {
        setIsLoading(false);
      }
    }
    fetchStats();
  }, []);

  return (
    <div className="marketing-hero-stat">
      {isLoading ? (
        <strong>...</strong>
      ) : (
        <strong>
          {totalCalls !== null ? new Intl.NumberFormat('en-IN').format(totalCalls) : 'N/A'}
        </strong>
      )}
      <span>Total Calls Made</span>
    </div>
  );
}

function RecentCallFeedback() {
  const [rows, setRows] = useState<CallFeedbackRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        setIsLoading(true);
        const response = await fetch('/api/admin/call-feedback');
        if (response.ok) {
          const data = await response.json();
          setRows(Array.isArray(data.callFeedback) ? data.callFeedback : []);
        }
      } catch (error) {
        console.error('Failed to fetch recent call feedback', error);
        setRows([]);
      } finally {
        setIsLoading(false);
      }
    }

    load();
  }, []);

  const formatDate = (value: CallFeedbackRow['createdAt']) => {
    if (value && typeof value === 'object' && 'toDate' in value && typeof value.toDate === 'function') {
      return value.toDate().toLocaleString();
    }
    if (typeof value === 'string' || typeof value === 'number') {
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
    }
    return '—';
  };

  return (
    <section className="marketing-panel" style={{ marginTop: 24 }}>
      <div className="marketing-panel-header">
        <h2>Recent call sessions</h2>
        <p>Latest call taps and worker feedback stored in Firestore.</p>
      </div>
      {isLoading ? (
        <div style={{ padding: 16 }}>Loading call sessions…</div>
      ) : rows.length === 0 ? (
        <div style={{ padding: 16 }}>No call sessions yet.</div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table className="marketing-data-table">
            <thead>
              <tr>
                <th>Job</th>
                <th>Worker</th>
                <th>Spoke</th>
                <th>Availability</th>
                <th>Hired</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id}>
                  <td>
                    <strong>{row.jobTitle || row.jobId || '—'}</strong>
                    <div style={{ color: '#64748b', fontSize: 12 }}>{row.companyName || '—'}</div>
                  </td>
                  <td>{row.workerId || '—'}</td>
                  <td>{row.spokeWithEmployer === true ? 'Yes' : row.spokeWithEmployer === false ? 'No' : '—'}</td>
                  <td>{row.jobAvailability || '—'}</td>
                  <td>{row.jobOfferAccepted === true ? 'Yes' : row.jobOfferAccepted === false ? 'No' : '—'}</td>
                  <td>{formatDate(row.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

const MARKETING_SECTIONS = [
  { id: "growth-assets", icon: "🎨", title: "Assets", count: 11, desc: "Pamphlets, posters, one-pagers, screenshots." },
  { id: "growth-campaigns", icon: "📣", title: "Campaigns", count: 7, desc: "Field visits, partnerships, reactivation runs." },
  { id: "growth-inputs", icon: "📥", title: "Inputs", count: 12, desc: "ICP, pricing, competitors, founder notes." },
  { id: "growth-outputs", icon: "📤", title: "Outputs", count: 11, desc: "Strategy decks, KPI specs, channel plans." },
  { id: "growth-playbooks", icon: "📘", title: "Playbooks", count: 11, desc: "RWA, naka, onboarding, press, partnerships." },
  { id: "growth-research", icon: "🔬", title: "Research", count: 5, desc: "Market briefs, competitor teardowns, interviews." },
  { id: "marketing-brand", icon: "🪣", title: "Brand", count: 4, desc: "Foundation, voice, visual identity, taglines." },
  { id: "marketing-channels", icon: "📡", title: "Channels", count: 7, desc: "WhatsApp, Play Store, LinkedIn, press, paid." },
  { id: "marketing-go-to-market", icon: "🚀", title: "Go To Market", count: 6, desc: "Positioning, ICP, city launch, seasonal calendar." }
];

export default function MarketingLandingPage() {
  const [search, setSearch] = useState("");
  const [matchCount, setMatchCount] = useState<number | null>(null);

  // Lightweight client-side search: hide non-matching <details> blocks.
  useEffect(() => {
    if (typeof document === "undefined") return;
    const term = search.trim().toLowerCase();
    const items = document.querySelectorAll<HTMLDetailsElement>(".marketing-full-file");
    let visible = 0;
    items.forEach((el) => {
      if (!term) {
        el.style.display = "";
        el.open = false;
        visible += 1;
        return;
      }
      const text = (el.textContent ?? "").toLowerCase();
      if (text.includes(term)) {
        el.style.display = "";
        el.open = true;
        visible += 1;
      } else {
        el.style.display = "none";
      }
    });
    setMatchCount(term ? visible : null);
  }, [search]);

  const totalDocs = useMemo(
    () => MARKETING_SECTIONS.reduce((sum, s) => sum + s.count, 0),
    []
  );

  return (
    <div className="marketing-landing">
      <header className="marketing-hero">
        <div className="marketing-hero-content">
          <h1>Marketing & Growth Hub</h1>
          <p>
            Every campaign, asset, input, output, playbook, channel, brand,
            go-to-market and research document — all in one searchable place.
          </p>
          <div className="marketing-hero-stats">
            <div className="marketing-hero-stat">
              <strong>{totalDocs}</strong>
              <span>Documents</span>
            </div>
            <CallStatsDisplay />
            <div className="marketing-hero-stat">
              <strong>9</strong>
              <span>Categories</span>
            </div>
            <div className="marketing-hero-stat">
              <strong>2</strong>
              <span>Top-level groups</span>
            </div>
          </div>
        </div>
      </header>

      <RecentCallFeedback />

      <div className="marketing-search-bar">
        <span className="search-icon">🔍</span>
        <input
          type="search"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search across pamphlets, playbooks, research, channels…"
        />
        {matchCount !== null ? (
          <span className="search-count">{matchCount} match{matchCount === 1 ? "" : "es"}</span>
        ) : (
          <span className="search-count">{totalDocs} docs</span>
        )}
      </div>

      <div className="marketing-section-cards">
        {MARKETING_SECTIONS.map((section) => (
          <a key={section.id} href={`#${section.id}`} className="marketing-section-card">
            <div className="icon">{section.icon}</div>
            <div className="meta">
              <strong>{section.title} <span style={{ color: "#94a3b8", fontWeight: 500 }}>· {section.count}</span></strong>
              <span>{section.desc}</span>
            </div>
          </a>
        ))}
      </div>

      <div className="marketing-full">
        <aside className="marketing-full-toc">
          <h3>Jump to section</h3>
          <small>{totalDocs} documents · 9 sections</small>
          <ul>
            {MARKETING_SECTIONS.map((s) => (
              <li key={s.id}>
                <a href={`#${s.id}`}>
                  <span>{s.icon}</span>
                  <span>{s.title}</span>
                </a>
              </li>
            ))}
          </ul>
        </aside>

        <div className="marketing-full-stream">
          <section className="marketing-full-section marketing-full-depth-0">
            <h2 className="marketing-full-heading">
              <span>🌱</span>
              <span>Growth</span>
              <small>56 docs</small>
            </h2>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>🎨</span>
            <span>Assets</span>
            <small>11 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Assets

> Production-ready creative + copy assets. All assets here should be **executable** — pamphlets ready to print, copy ready to paste into WhatsApp, screenshots ready to upload to Play Console.

## Files

- [pamphlet_telugu.md](pamphlet_telugu.md) — Telugu naka pamphlet copy (A5, double-sided).
- [rwa_one_pager.md](rwa_one_pager.md) — RWA admin pitch one-pager (PDF source).
- [society_poster.md](society_poster.md) — A4 society notice-board poster copy + spec.
- [whatsapp_templates.md](whatsapp_templates.md) — verified WhatsApp message templates (worker + employer + partner).
- [push_copy.md](push_copy.md) — push notification copy (English + Telugu) for each lifecycle trigger.
- [play_store_listing.md](play_store_listing.md) — finalised Play Store title / short / long descriptions in EN + TE + HI.
- [play_store_screenshots/](play_store_screenshots/) — screenshot exports.
- [investor_one_pager.md](investor_one_pager.md) — single-page investor / partner one-pager template.
- [pg_owner_one_pager.md](pg_owner_one_pager.md) — PG / cloud-kitchen owner one-pager.
- [press_one_pager.md](press_one_pager.md) — journalist-facing one-pager (use only after eligibility gate).

## Production rules

1. **Telugu localisation:** Telugu-speaking founder must approve every Telugu asset. Do not use Google Translate.
2. **No stock photos** of "smiling diverse workers." Use real photos with consent or use illustrations.
3. **No pricing claims** that aren't true today (e.g., "free for life").
4. **Brand colour:** \`#2563EB\` (DutyPe blue). Secondary: \`#0F172A\` (deep navy text). Accent: \`#F59E0B\` (amber for incentives).
5. **Typography:** Inter (web/screens) + Hind Telugu (Telugu copy).
6. **Logo lockup:** wordmark only on print < A4; wordmark + tagline on A4 and above.
7. **Every asset must include:** WhatsApp number \`+91 91217 06236\` and email \`dutypein@gmail.com\`.
8. **Legal entity** ("KGPV INNOVATION SOLUTIONS PRIVATE LIMITED") on all printed assets.

## Assets to add (designer queue)

- [ ] Pamphlet PDF (designer to create from copy).
- [ ] RWA one-pager PDF.
- [ ] Society poster PDF.
- [ ] 8 Play Store screenshots (English + Telugu localised).
- [ ] 1024×500 feature graphic.
- [ ] Investor one-pager PDF.
- [ ] PG owner one-pager PDF.
- [ ] Press one-pager PDF (only after eligibility gate).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/investor-one-pager">Investor One Pager</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Investor / Partner One-Pager

> Single page. Update monthly with verified numbers. Hand to: angels, micro-VCs, accelerator partners, ecosystem operators.

---

## Header

\`\`\`
DutyPe — Hyper-local hiring app for blue-collar India
Hyderabad · Telugu + English · Android · Live since {{launch_year}}
\`\`\`

## What we do

> One-paragraph plain-English description.

DutyPe matches blue-collar workers (cooks, maids, helpers, drivers) with households and small businesses **within 5 km of where they live**. Phone-OTP verified. QR-based work-start. Telugu-first. Zero agency commission for both sides.

## Why now

- 100M+ informal-sector workers in India; 90 % find jobs through word-of-mouth + agency middlemen.
- Indian blue-collar households now smartphone-default (Android share ≥ 95 %).
- Existing apps (Apna, WorkIndia, Job Hai) are national-generic — they cannot match a Madhapur-resident cook to a Madhapur-resident household.

## Numbers (as of {{month YYYY}})

| Metric | Number |
|---|---|
| Active workers (Hyderabad wedge pincodes) | {{N}} |
| Active employers (same) | {{N}} |
| Jobs filled in last 30 days | {{N}} |
| Median time-to-first-applicant | {{X}} hours |
| Worker D7 retention | {{P}} % |
| Employer 30-day repeat-post | {{P}} % |
| Cumulative referral payouts | ₹{{A}} |
| Play Store rating | {{R}} ★ ({{count}} reviews) |
| Burn / month | ₹{{B}} K |
| Runway | {{M}} months |

## Why we win in our wedge

1. **Telugu-first product** in Telugu-dominant Hyderabad. Asymmetric to nationally-positioned apps.
2. **QR work-start**. Unique trust artifact; documented attendance + payment proof.
3. **RWA partnership channel.** No competitor systematically partners with society admins.
4. **Hyper-local pincode SEO.** 250+ programmatic pages target long-tail searches Apna doesn't bother with.
5. **Founder-led offline supply.** Not scalable on day 1 — but unbeatable for first 3 pincodes.

## Roadmap (next 6 months)

- City-1 (Hyderabad): hit 1 000 active workers across 3 pincodes; ≥ 5 % paying employers.
- Open city-2 (Vijayawada or Bangalore — decided post-day-90).
- Driver / delivery vertical opened in city-1 after cook+maid hits 60 fills/week.
- Verified-employer paid badge live; first ₹50 K/mo revenue line.

## What we are NOT doing

- Not running paid Meta / Google ads to acquire workers (unit-economic-negative today).
- Not adding chat (deliberately removed).
- Not building iOS until Android density is real.
- Not expanding to more than 2 cities in the first 12 months.

## What we want from you (specify per recipient)

- ☐ Intro to {{specific person profile}}.
- ☐ Capital — ₹{{amount}} at ₹{{valuation}} cap.
- ☐ Domain expertise — {{topic}}.
- ☐ Hiring help — {{role}}.

## Contact

- Founder: {{name}}
- Phone / WhatsApp: +91 91217 06236
- Email: dutypein@gmail.com
- Website: https://dutype.in
- Legal entity: KGPV INNOVATION SOLUTIONS PRIVATE LIMITED, Hyderabad.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/pamphlet-telugu">Pamphlet Telugu</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Pamphlet — Telugu (A5, double-sided)

> Print spec: A5 (148 × 210 mm), 130 gsm matte, single colour preferred (cost ≤ ₹2.50/piece in 500-qty run). Double-sided.

## FRONT

\`\`\`
[Logo top-left: DutyPe]
[Headline, large, Telugu:]
మీ ఇంటి దగ్గర
ఉద్యోగాలు

[Sub-headline:]
కుక్, మెయిడ్, హెల్పర్, డ్రైవర్ —
కమిషన్ లేదు. ఏజెన్సీ లేదు.

[Bullet list — large icons:]
✓ ఫోన్ OTP తో సురక్షిత employers
✓ 5 km లో జాబ్స్
✓ Telugu support
✓ Free download

[QR code, large — links to Play Store with utm_source=naka_pamphlet]

[Below QR:]
ఈ QR scan చేయండి →
Free గా install చేయండి
\`\`\`

## BACK

\`\`\`
[Headline:]
DutyPe ఎలా పని చేస్తుంది?

[3 numbered steps:]

1. ఫోన్ నంబర్ OTP తో login
   (10 సెకన్లు)

2. Profile complete చేయండి
   (3 నిమిషాలు)

3. మీ area లో jobs apply చేయండి
   (1 tap)

[Trust line:]
Workers ఎప్పుడూ ఏ payment చేయకూడదు.
DutyPe complete free for workers.

[Refer & Earn block:]
ఒక్క friend ని join చేస్తే ₹25.
100 friends → ₹1,000 bonus.

[Bottom strip:]
WhatsApp Help: +91 91217 06236
Email: dutypein@gmail.com
KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

## Variants for A/B test

- **Variant A — commission-led:** front headline becomes "ఏజెన్సీ commission ఎందుకు pay చేయాలి? Free గా DutyPe లో job పొందండి."
- **Variant B — incentive-led:** front headline becomes "DutyPe install చేయండి. Profile complete చేయండి. ₹50 పొందండి."
- **Variant C — proximity-led (default above).**

Print 200 of each variant in week-1. Tally installs by variant in \`growth/campaigns/field_visits.csv\`.

## Design rules

- All Telugu text in **Hind Telugu** font weight 600 (or system Telugu equivalent).
- QR ≥ 3 cm × 3 cm. Test scannability from 30 cm.
- High contrast — single brand colour \`#2563EB\` on white background.
- No stock images. Optional: a small line-illustration of a phone in hand.
- Bottom 5 mm reserved for printer crop marks; do not put text there.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/pg-owner-one-pager">Pg Owner One Pager</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# PG / Cloud-Kitchen Owner One-Pager (A4)

> Hand-out for in-person / WhatsApp follow-up to small employer prospects.

\`\`\`
[Logo top-left: DutyPe]    [Top right: "For PG / Tiffin / Salon owners · Hyderabad"]

[Headline:]
Hire kitchen helpers, cleaners, cooks
in 24 hours. Replace overnight if they quit.

₹0 commission. Phone-verified. Hyper-local.

[Trust strip:]
✓ Phone OTP verified workers
✓ QR work-start every shift
✓ Workers from 5 km of your kitchen / PG
✓ Telugu support on WhatsApp

[Stats block — pulled live from Cloud Function:]
Active workers in your pincode ({{pincode}}): {{N}}
Median time to first applicant: {{X}} hours.

[Comparison block:]
                       DutyPe       Local agency       WhatsApp groups
Commission             ₹0           ₹2-5K              ₹0
24h replacement        ✅            ❌                  ❌
Phone-verified         ✅            Mixed              ❌
QR attendance          ✅            ❌                  ❌
Telugu support         ✅            Mixed              n/a

[Headline:]
How it works
1. Post a job free in 2 minutes.
2. Get applications today.
3. Pick a worker, scan their QR when they arrive.

[Big CTA:]
Post your first job free → install Play Store
[QR linking to /post-job deep link with utm_source=pg_one_pager]

[Bottom:]
WhatsApp founder direct: +91 91217 06236
Email: dutypein@gmail.com
DutyPe · dutype.in · KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

## Variants by sub-segment

- **PG / co-living owner:** lead with "24-hour replacement" + "no agency commission."
- **Cloud kitchen / tiffin:** lead with "verified kitchen helpers, 5 km radius" + "Telugu support."
- **Salon / beauty parlour:** lead with "experienced helpers nearby" + "no recruiter fee."
- **Small F&B (cafe, dhaba, juice shop):** lead with "fill same day" + "₹0 commission."

## Production

- A4, 130 gsm, full colour.
- Cost: ~₹15/piece in 100-qty.
- Designer creates one master + 4 sub-segment headlines.
- Founder hand-delivers first 50 personally (paired with cold WhatsApp playbook).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/play-store-listing">Play Store Listing</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Play Store Listing — Final Copy

> Final copy for Play Console. EN is default; TE and HI are localised listings.
>
> All numbers must be **server-truthful** (pulled from Cloud Function before each republish).

---

## English (default)

### Title (≤ 30)
\`DutyPe Cook Maid Helper Jobs\`

### Short description (≤ 80)
\`Hyper-local jobs near you. Cooks, maids, helpers, drivers. No agency fee.\`

### Full description
See [growth/outputs/app_store_growth_plan.md](../outputs/app_store_growth_plan.md) section "Full description". Paste verbatim into Play Console.

---

## Telugu (localised listing)

### Title (≤ 30)
\`DutyPe — Cook, Maid, Helper Jobs\`

### Short description (≤ 80)
\`మీ ఇంటి దగ్గర ఉద్యోగాలు. కుక్, మెయిడ్, హెల్పర్. కమిషన్ లేదు.\`

### Full description (translate the EN long-description, do not Google Translate)

\`\`\`
మీ ఇంటి దగ్గర ఉద్యోగం పొందండి — లేదా మీ ఇంటి / దుకాణం కోసం worker hire చేయండి. Agency commission లేకుండా.

DutyPe అనేది India లో blue-collar మరియు household work కోసం hyper-local hiring app. మేము cooks, maids, helpers, drivers, security guards, gardeners, electricians, plumbers, painters, carpenters లాంటి roles ని మీ ఇంటి near 5 km లో match చేస్తాము.

ఎందుకు DutyPe?
- 1 km / 5 km / 10 km radius — మీ area లోనే jobs.
- Phone-verified employers. Scam-keyword filter — fake "earn ₹50,000 from home" posts auto-block.
- QR-based work start / finish proof.
- Telugu interface + Telugu WhatsApp support.
- Workers, employers ఇద్దరికీ commission ZERO.
- Refer & Earn — ₹25 per friend + bonuses up to ₹1,000.

Job categories:
Cook, Maid, House Helper, Driver, Security, Gardener, Caretaker, Delivery, Waiter, Electrician, Plumber, Painter, Carpenter, Receptionist, Cashier, Packer.

Pay types: Daily, Hourly, Monthly, per-task.

Cities:
Hyderabad, Secunderabad — Gachibowli, Madhapur, Kondapur, Kukatpally, Miyapur, Hitech City, Begumpet, Banjara Hills, Jubilee Hills, Ameerpet, Mehdipatnam, LB Nagar, Uppal, Chandanagar, KPHB, Manikonda.

(Coming soon: Bangalore, Vijayawada, Visakhapatnam, Pune.)

Employers కోసం:
- Job post cheyandi 2 minutes lo, free ga.
- Today applications పొందండి, 3 days kosam wait cheyaali ledhu.
- Worker profiles చూడండి — phone OTP verified.
- Work attendance verify cheyandi QR tho.

Safety & Privacy:
- Workers నుండి ఎప్పుడూ payment ask cheyamu.
- Phone numbers masked, మీరు share cheste tappa.
- Profile photo optional.
- Data India lo store ayinaadi.

Refer & Earn:
₹25 per friend join. Bonus tiers: ₹50 (5), ₹100 (10), ₹250 (25), ₹500 (50), ₹1,000 (100). ₹50 nundi withdraw cheyochu.

Support:
WhatsApp: +91 91217 06236
Email: dutypein@gmail.com

KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

---

## Hindi (localised listing — UI is NOT yet Hindi, listing only)

### Title (≤ 30)
\`DutyPe Cook Maid Helper Jobs\`

### Short description (≤ 80)
\`आपके घर के पास नौकरी। कुक, मेड, हेल्पर। कोई कमीशन नहीं।\`

### Full description (note app UI is currently Telugu + English)

\`\`\`
अपने घर के पास नौकरी पाएं — या अपने घर / दुकान के लिए वेरिफाइड वर्कर हायर करें। बिना किसी एजेंसी कमीशन के।

DutyPe भारत में ब्लू-कॉलर और घरेलू काम के लिए हाइपर-लोकल हायरिंग ऐप है। हम कुक, मेड, हेल्पर, ड्राइवर, सिक्योरिटी गार्ड, इलेक्ट्रिशियन, प्लम्बर, पेंटर, कारपेंटर जैसी भूमिकाओं को आपके घर के 5 किमी के दायरे में मैच करते हैं।

ध्यान दें: DutyPe ऐप का इंटरफेस अभी अंग्रेज़ी और तेलुगु में है। हिंदी UI जल्द आ रहा है।

क्यों DutyPe?
- 1 km / 5 km / 10 km दायरा — सिर्फ आपके पास की नौकरियां।
- फोन OTP वेरिफाइड एम्प्लॉयर। नकली "घर बैठे ₹50,000 कमाएं" पोस्ट ऑटो-ब्लॉक।
- QR-आधारित वर्क-स्टार्ट प्रूफ।
- WhatsApp पर सहायता।
- वर्कर और एम्प्लॉयर दोनों के लिए कमीशन शून्य।
- रेफर एंड अर्न — हर दोस्त पर ₹25 + ₹1,000 तक बोनस।

(रेस्ट of long-description in हिंदी, mirroring EN structure.)

सपोर्ट:
WhatsApp: +91 91217 06236
Email: dutypein@gmail.com

KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

---

## What's-new text (per release)

Keep ≤ 500 chars. Always release-note specific. Avoid generic "bug fixes & improvements" — wastes ASO real estate.

Example template:
\`\`\`
- New: <feature> in {{city/category}}.
- Fixed: {{specific bug visible to users}}.
- Telugu translations updated for {{screens}}.

Reviews / requests → dutypein@gmail.com
WhatsApp +91 91217 06236
\`\`\`

---

## Update checklist

- [ ] All listings reflect current product (don't promise unreleased features).
- [ ] All numbers verified server-truthful in last 24 h.
- [ ] Telugu copy reviewed by a native speaker (NOT Google Translate).
- [ ] Hindi copy reviewed by native speaker.
- [ ] Screenshots refreshed if any UI changed in this release.
- [ ] Privacy policy + Data Safety form re-verified.
- [ ] What's-new release notes written per release.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/press-one-pager">Press One Pager</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Press One-Pager (use ONLY after eligibility gate in \`playbooks/press_pitch.md\`)

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/push-copy">Push Copy</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Push Notification Copy

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/rwa-one-pager">Rwa One Pager</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# RWA Admin — One-Pager

> One A4 page. Print double-sided if you want to fold it into A5. Hand to RWA admin in person; attach as PDF on follow-up WhatsApp.

## FRONT

\`\`\`
[Logo top-left: DutyPe]    [Top right: "For RWA Admins · Hyderabad"]

[Headline:]
Your residents are paying ₹2,000–5,000
in commission to maid agencies.
DutyPe makes it free, in your society.

[Sub:]
A free hyper-local hiring app for cooks, maids, helpers,
drivers — verified by phone OTP, with QR work-start.

[3-column trust block:]
Phone OTP        QR work-start       Telugu support
Verified         every shift          on WhatsApp
employers

[Stats block — pulled from live Cloud Function:]
Active in your pincode: <N> workers · <M> employers
Median time-to-first-applicant: <X> hours.

[Comparison block:]
                       DutyPe       Agency       WhatsApp group
Commission             ₹0           ₹2-5K        ₹0
Verified phone         ✅            ❌            ❌
Hyper-local 5 km       ✅            ❌            ✅ society
Replacement support    ✅            Sometimes    ❌
Telugu support         ✅            Mixed        n/a

[Big CTA:]
"Forward this WhatsApp to your residents once."
We'll handle the rest.
\`\`\`

## BACK

\`\`\`
[Headline:]
What we ask of you (5 minutes a month):

1. Forward our co-branded WhatsApp message to your society
   group — once a month.
2. Pin the DutyPe poster on your notice board for 30 days.
3. Tell us if any resident has a complaint about a hire.

[Headline:]
What you get:

✓ A custom installs report for your society every month.
✓ A small contribution (₹500) to your society event fund per
  quarter (refreshments, banners — never cash kickback).
✓ First access to new features in your pincode.
✓ Listed as a "Founding Society Partner" inside the app.

[Headline:]
Who runs this?

DutyPe is built by KGPV INNOVATION SOLUTIONS PRIVATE LIMITED.
Founder: <Name>. Phone: +91 91217 06236.
We are based in Hyderabad and you can meet us in person.

[Bottom:]
WhatsApp: +91 91217 06236 · dutypein@gmail.com · dutype.in
KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

## Production notes

- A4, 130 gsm, full colour.
- Cost: ~₹15/piece in 100-qty.
- Designer creates two locked-in language variants: **English** (default) and **Telugu**.
- Co-branded version: leave a 80×80 mm space top-right for the society logo.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/society-poster">Society Poster</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Society Notice-Board Poster (A4)

> Single page. Designed for the society notice board / lift mirror. Print spec: A4, 200 gsm, full colour, laminated. ~₹50/piece.

## Layout

\`\`\`
[Top 1/4: bold headline in Telugu + English]
మీ society లో maid / cook కావాలా?
Need a maid or cook in your society?

[Middle 1/2: phone mockup with 3 sample job cards visible]
[Right side overlay text:]
✓ Phone OTP verified
✓ 5 km radius
✓ ₹0 commission
✓ Telugu support

[Below mockup:]
QR (large, 5×5 cm) — links to Play Store with
utm_source=society_poster&utm_campaign=<society_name>

[Bottom strip:]
Society partner: <Society Name>
WhatsApp help: +91 91217 06236
DutyPe · dutype.in · KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

## Production rules

- Society name printed per-society (variable data field). Designer creates one master + per-society overlay.
- QR must be unique per society (utm_campaign). Cloud Function tracks attribution.
- Lamination is mandatory (poster lives on board for 30+ days, exposed to humidity).
- Replacement schedule: every 60 days OR when worker / employer count changes ≥ 25 % (whichever sooner).

## Distribution

- Founder hand-delivers first 30 posters personally (ride-along during RWA partnership pitch).
- Each delivery logged in \`growth/campaigns/partnerships.csv\` with \`units_or_members\` filled.
- Photo of mounted poster requested from RWA admin within 7 days.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/whatsapp-templates">Whatsapp Templates</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# WhatsApp Templates

> Production-grade WhatsApp Business message templates. Submit to Meta for "Utility" or "Marketing" template approval before high-volume use.
>
> All templates have variables in \`{{double_braces}}\`. Cloud Function fills variables before send.

---

## Worker — welcome (Utility, Telugu)

\`\`\`
Namaste {{1}} 🙏

DutyPe ki welcome.

Mee profile complete cheyyandi 7 days lo →
mee area lo verified jobs apply cheyochu.

Help kavalantey reply cheyandi.

— DutyPe team
\`\`\`

## Worker — D7 incomplete profile (Marketing, Telugu)

\`\`\`
{{1}}, mee profile inkaa complete kaaledhu.

Profile complete chesthe ₹50 unlock + jobs apply cheyochu.

Tap → {{deep_link}}

Sahaayam kavalantey reply cheyandi. Telugu lo support unnaadi.
\`\`\`

## Worker — D30 inactive (Marketing, Telugu)

\`\`\`
{{1}}, mee area lo {{N}} jobs add ayinayi past week lo.

Apply cheyandi free ga →
{{deep_link}}

Notification off cheyalantey reply "STOP".
\`\`\`

## Employer — welcome (Utility, English)

\`\`\`
Hi {{1}},

Welcome to DutyPe. I'm {{founder_name}}, founder.

Post your first job in 2 minutes →
{{deep_link_post_job}}

Reply with the role you're hiring for and I'll personally help you fill it within 48 hours.
\`\`\`

## Employer — D1 no post (Marketing, English)

\`\`\`
{{1}}, your DutyPe account is active but you haven't posted a job yet.

Your area ({{pincode}}) has {{N}} verified workers waiting.

Post free here → {{deep_link_post_job}}

Reply with any question — I'll help you set up.
\`\`\`

## Employer — post-fill NPS (Utility, English)

\`\`\`
{{1}}, did {{worker_name}} show up and complete the work?

Reply with one number:
5 — perfect
4 — good
3 — okay
2 — issues
1 — didn't show up

Anything else you'd want to fix?
\`\`\`

## Employer — D30 repeat-post nudge (Marketing, English)

\`\`\`
{{1}}, you hired {{worker_name}} 30 days ago.

Need to hire again? Post free → {{deep_link_post_job}}

Or refer another household — both you and they get ₹50.
\`\`\`

## RWA admin — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

I noticed you admin the {{society}} community. 

We help apartment residents hire maids and cooks directly — phone-verified, no agency commission, hyper-local.

Active workers near {{pincode}}: {{N_workers}}.

Open to a 15-min meet this week?

(Not selling anything — we're free for residents and for you.)

— {{founder_name}} · DutyPe
\`\`\`

## RWA admin — co-branded broadcast (Utility, English + Telugu)

\`\`\`
Dear {{society}} residents,

Hiring a maid, cook, helper or driver?

We've partnered with DutyPe — a free hyper-local hiring app, phone-verified, no agency commission.

Telugu support. 5 km radius from our society.

Install free → {{deep_link_society_attribution}}

— {{rwa_admin_name}}, {{society}} RWA
Powered by DutyPe (KGPV Innovation Solutions Pvt Ltd)
\`\`\`

## PG owner — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

Saw you run {{pg_name}} on {{road / area}}. 

We help PG / co-living owners hire cleaning + cooking staff directly — phone-verified, ₹0 commission, replaceable in 24h if someone quits.

Active workers in {{pincode}}: {{N_workers}}.

Want me to walk you through it in 5 minutes? Or post a free trial role here → {{deep_link}}.

— {{founder_name}} · DutyPe
\`\`\`

## NGO partner — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

I read about your work with {{cohort_description}} in {{area}}. 

We're a hyper-local hiring app for blue-collar work in Hyderabad — Telugu, phone-verified, ₹0 commission, women-first.

Would your cohort women benefit from a 1-day onboarding camp? We bring the install team and we cover refreshments + a ₹100 sign-up incentive per attendee.

Open to a 30-min meet to discuss?

— {{founder_name}} · DutyPe
\`\`\`

## Reactivation — lapsed employer 60 days (Marketing, English)

\`\`\`
{{1}}, it's been 60 days since you last posted on DutyPe.

Your area ({{pincode}}) has grown — now {{N_workers}} active workers (up from {{N_old}}).

Post free → {{deep_link_post_job}}

If something went wrong last time, reply and I'll personally fix it.

— {{founder_name}}
\`\`\`

## Send rules

- All marketing templates only sent **with prior consent** (account creation = consent for own-account messaging; partner WhatsApp lists need explicit opt-in or partner is the sender).
- Never include short URLs (Meta penalises bit.ly).
- Always include opt-out line for marketing.
- Never send between 22:00–08:00 IST.
- Cap: 2 marketing messages per user per week.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/assets/play-store-screenshots/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Play Store Screenshots — Export Specs

Drop the final exported PNG files here, sized **1080 × 1920** (portrait, 9:16).

## Naming convention

\`\`\`
en_01_worker_hero.png
en_02_worker_local.png
en_03_worker_apply.png
en_04_worker_refer.png
en_05_employer_post.png
en_06_employer_applications.png
en_07_employer_qr.png
en_08_employer_support.png

te_01_worker_hero.png
te_02_worker_local.png
… (same sequence in Telugu)
\`\`\`

## Master Figma source

Keep the editable source in \`growth/assets/play_store_screenshots/source/\` (Figma file URL noted in the README of that subfolder once created).

## Headlines per slot

See [growth/outputs/app_store_growth_plan.md](../../outputs/app_store_growth_plan.md) section "Screenshots (8 slots — sequence is the message)" for the exact overlay copy.

## Compression

- PNG, ≤ 8 MB each (Play Store limit).
- Use \`pngquant --quality=70-85 --strip\` to compress before upload.

## Localisation rule

- Telugu listing must use Telugu-overlay screenshots.
- Hindi listing should use English-overlay screenshots (since the app UI itself is not Hindi yet) plus a Hindi caption inside the listing description.
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📣</span>
            <span>Campaigns</span>
            <small>7 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Campaigns — operational tracking

> All live and historical campaigns are tracked here. Each campaign = one CSV. Update at end of each working day. Friday review reads from these files.

## Files

- [outbound.csv](outbound.csv) — every outbound message sent (WhatsApp, email, LinkedIn DM, in-person).
- [content_log.csv](content_log.csv) — every published content asset.
- [partnerships.csv](partnerships.csv) — RWA / NGO / contractor / SMB partnership pipeline.
- [reactivation.csv](reactivation.csv) — lifecycle / reactivation pushes sent.
- [field_visits.csv](field_visits.csv) — every naka / society / field session.
- [experiments.csv](experiments.csv) — active and historical experiments (linked to \`growth/outputs/growth_experiments_backlog.md\`).

## Update cadence

| File | When updated | Owner |
|---|---|---|
| outbound.csv | End of day, every day messages were sent | Founder / SDR |
| content_log.csv | Within 24 h of publishing | Founder |
| partnerships.csv | When status changes | Founder |
| reactivation.csv | Auto-export from Cloud Function weekly | Engineer |
| field_visits.csv | End of each session | Founder |
| experiments.csv | Friday review | Founder |

## Rules

- **No backfilling 2 weeks later.** Either log it that day or it didn't happen.
- **Use ISO dates** (\`YYYY-MM-DD\`) only.
- **Verdict columns** must be one of: \`pending\`, \`replied\`, \`meeting\`, \`signed\`, \`dead\`, \`keep\`, \`kill\`, \`scale\`.
- **Founder hours** is per-row, not cumulative.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/content-log">Content Log</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`date,channel,format,audience,headline,link,impressions_7d,replies,shares,clicks_to_install,attributable_installs_30d,verdict,notes
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/experiments">Experiments</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`exp_id,name,start_date,end_date,channel,hypothesis,success_metric,kill_metric,actual_result,decision,next_action,total_cost_inr,founder_hours,notes
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/field-visits">Field Visits</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`date,session_type,location,pincode,founder_hours,pamphlets_distributed,conversations,installs_same_day,profile_completes_7d,best_quote,top_objection,verdict,notes
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/outbound">Outbound</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`date,channel,sequence_id,prospect_name,prospect_org,prospect_role,city,pincode,opener_variant,follow_up_count,reply,verdict,founder_hours,notes
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/partnerships">Partnerships</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`date_first_contact,partner_type,partner_name,society_or_org,city,pincode,units_or_members,owner,status,signed_date,first_install_date,installs_attributed,jobs_posted,jobs_filled,monthly_check_in,notes
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📊</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/campaigns/reactivation">Reactivation</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <CsvTable csv={`date,cohort,trigger,channel,recipients,delivered,opened,clicked,activated,reward_paid_inr,verdict,notes
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📥</span>
            <span>Inputs</span>
            <small>11 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/app-features">App Features</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Feature Inventory (input file)

> Pulled from codebase. This is what we can *truthfully* claim in marketing. Anything not listed here = do not claim.

## Worker side
- Onboarding: language pick → phone OTP → role pick → profile (skills, location, experience).
- Job feed: distance-sorted (1/5/10 km), filter by category, pay type, urgency.
- Apply in 1 tap once profile ≥80% complete.
- "My Jobs" / Applied / Saved tracking.
- QR scan to mark work start + work complete.
- Earnings dashboard.
- Refer & Earn: ₹25/referral + tier bonuses, withdraw at ₹50 minimum.
- Notifications (FCM + local WorkManager): new nearby jobs, application status, smart re-engagement (time-of-day tagged).
- Digital visiting card (shareable profile).
- WhatsApp support (role-aware message: "I am a worker on DutyPe…").

## Employer side
- Onboarding: phone OTP → role → company profile → 80% completion gate.
- Post Job (4-step wizard): Role → Pay & Place → People → Review.
- Anti-fraud at post time: structured titles only, scam-keyword warnings, pay-rate suggested range.
- Rate limit: 2 posts/hr, 5/day (free); 10/hr, 50/day (paid).
- View applications, shortlist, contact applicant directly.
- QR generation to verify work start/finish + rate worker.
- Refer & Earn: tiered free posts unlocked at 5/10/25 referrals.
- Analytics (basic): views, applications per job.
- WhatsApp support (role-aware: "I am an employer on DutyPe…").

## Trust / safety we can talk about
- Phone-verified accounts on both sides.
- Scam keyword detection at job-post time.
- Fraud score (server-side) auto-rejects bad postings.
- Profile completion gating before posting/applying.
- QR-based work-start so neither side can dispute attendance.

## Things NOT yet built (don't claim)
- No background check / police verification of workers.
- No insurance coverage.
- No escrow / in-app payment to worker.
- No staffing-agency / recruiter dashboard.
- No iOS app.
- Languages beyond English & Telugu not live.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/app-store-links">App Store Links</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – App Store / Web Links (input file)

## Live links
- Play Store: https://play.google.com/store/apps/details?id=com.dutype.app
- Website: https://dutype.in
- Support email: dutypein@gmail.com
- Feedback email: dutypefeedback@gmail.com
- WhatsApp support: https://wa.me/919121706236
- Legal entity: KGPV INNOVATION SOLUTIONS PRIVATE LIMITED

## Not live yet
- iOS App Store: not built.
- Hindi UI: not localized (only English + Telugu).

## Founder to fill
- UTM-tracked landing page URLs for each campaign: ____
- Branch.io / Firebase Dynamic Link for "install + auto-open job" deep link: ____
- Play Console internal-testing track URL (for waitlists): ____
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/competitors">Competitors</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Competitive Landscape (input file)

> Replace approximate notes with founder's real comparison. Be honest.

## Direct competitors (blue-collar, India, app-based)
- **Apna** — large network, weak hiring conversion, broad categories, urban focus, no QR, no hyper-local radius.
- **WorkIndia** — older, B2B sales motion, recruiter-skewed, expensive for small employers.
- **Job Hai (Info Edge)** — Naukri's blue-collar app, small-employer aware, scam infested.
- **Vahan / Frontier Markets** — driver/delivery vertical, B2B contracts with Swiggy/Zomato, not direct.
- **Babajob** — defunct/absorbed; useful as a cautionary tale.

## Indirect competitors (where workers actually find work today)
- **WhatsApp groups** (the real #1 competitor; free, trusted, but no proof / no payment trail).
- **Naka labour points** (street corners where day-workers gather at dawn).
- **Local contractors / middlemen** (charge 10–30% cut).
- **Quikr / OLX Jobs** (declining but still used for cooks/maids).
- **Apartment society RWA + facility managers** (especially for cook/maid).
- **Newspaper classifieds** (declining).
- **Facebook groups + Telegram channels** for city-specific hiring.

## DutyPe's defensible edges (today, verifiable)
- Hyper-local radius (1/5/10 km) and geohash backend — Apna does not match this granularity.
- QR-based work-start verification — neither WhatsApp nor Apna has this.
- Scam-keyword + fraud-score firewall at post time — quantifiably better than Quikr/OLX/Apna for trust.
- Telugu localization — under-served vs. Hindi-only competitors.
- Direct worker↔employer (no recruiter cut) — compelling for small employers.

## DutyPe's weaknesses (be honest)
- Smaller worker pool per pincode than Apna in any tier-1 city.
- No background verification (Apna started doing some).
- No insurance / escrow.
- No iOS.
- Brand awareness ≈ zero.

## What this means for positioning
- Don't fight Apna on "biggest network." You'll lose.
- Win on **trust + hyper-local + verified employers**. The only people who care: households + micro-employers who got burned on Quikr/Apna.
- Don't try to compete with WhatsApp groups directly — *augment* them: invite the group admins to be DutyPe partners.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/current-metrics">Current Metrics</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Current Metrics (input file – founder to fill)

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/founder-notes">Founder Notes</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Founder Notes (input file)

> A free-form journal. Things only the founder knows. Update weekly.

## Why this product exists
- ____

## What I've personally seen workers say (verbatim)
- ____

## What I've personally seen employers say (verbatim)
- ____

## What's working (3 things)
- ____
- ____
- ____

## What's broken (3 things)
- ____
- ____
- ____

## What I'm afraid to admit publicly
- ____

## What I refuse to do
- e.g. "I will not pay influencers."
- e.g. "I will not become a staffing agency."
- ____

## Constraints
- Team size: ____
- Cash in bank: ____
- Months of runway at current burn: ____
- Hours per week I personally have for growth: ____
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/icp">Icp</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Ideal Customer Profile inputs (founder to confirm)

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/landing-page-copy">Landing Page Copy</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Current Landing Page Copy (input file)

> Snapshot the live \`https://dutype.in\` homepage hero, "How it works", FAQ, and footer here so we have a baseline to rewrite from. Update whenever the site changes.

## Current hero (snapshot)
- Title: ____
- Subtitle: ____
- Primary CTA: ____
- Secondary CTA: ____

## Current "How it works" steps
1. ____
2. ____
3. ____
4. ____

## Current trust block / proof
- ____

## Current FAQ topics
- ____

## What's missing today (to be addressed by \`outputs/landing_page_recommendations.md\`)
- Specific city / pincode landing pages.
- Category-specific pages (e.g. /hyderabad/cook-jobs).
- Real testimonials (verbatim, with first name + area).
- Concrete numbers (# workers in Hyderabad, # jobs posted last week, median time-to-first-application).
- Comparison block vs. Apna / Quikr / WhatsApp groups.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/pricing">Pricing</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Pricing & Monetization (input file)

## Today
- Free for both workers and employers.
- Workers see Google ads (AdMob).
- Workers must watch a rewarded ad to "unlock employer contact" on certain jobs.
- Employer rate limit: 2 posts/hour, 5/day on free; 10/hour, 50/day on "paid" — but no actual paid tier exists yet (the paid quota is wired but not sold). **Confirm**.
- Referral program: ₹25 per successful referral, tier bonuses, withdraw at ₹50.

## What's not built
- No subscription tier.
- No paid placement / promoted job listing.
- No verified-employer badge for sale.
- No staffing-agency seat.
- No payment gateway / escrow.

## Founder decision needed (before scaling marketing)
1. Are we monetizing employers (post fees / promoted listings) or workers (premium discovery / ads) — or both?
2. If employer-monetized: at what price would a small business *gladly* pay? (Test: ₹99 / ₹199 / ₹499 per post; ₹999/mo unlimited.)
3. If we don't monetize for 12 more months, what funds the ad spend? (Honest answer — if "nothing", then **no paid ads**, period.)
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/product-brief">Product Brief</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Product Brief (input file)

> Pre-filled from codebase recon on 19 Apr 2026. Replace \`[TBD]\` rows with founder truth before reusing in outbound.

## What it is
- Hyper-local, two-sided **blue-collar job marketplace** for India.
- Direct worker ↔ employer connection with no middleman fees.
- Available as **Android app** (\`com.dutype.app\`, v2.6.1) + Next.js website at \`https://dutype.in\`.
- Languages live: **English, Telugu** (\`res/values-te/\`).

## What it does
- Workers find local jobs within 1 / 5 / 10 km radius.
- Employers post jobs in 17 categories: COOK, MAID, DRIVER, HELPER, SECURITY, GARDENER, CARETAKER, DELIVERY, WAITER, ELECTRICIAN, PLUMBER, PAINTER, CARPENTER, RECEPTIONIST, CASHIER, PACKER, OTHER.
- Pay types: DAILY, HOURLY, MONTHLY, TASK.
- Trust stack: phone OTP signup, structured (not free-text) job titles, scam-keyword filter, fraud-score auto-reject ≥70, employer rate-limit (2/hr free / 10/hr paid), QR code work-start verification.

## Who it serves
- **Workers**: blue-collar, semi-formal, ages ~18–45, smartphone-first, often Hindi/Telugu/local-language preference.
- **Employers**: small / micro businesses, households, contractors. NOT large staffing agencies.

## Current monetization
- In-app Google ads (AdMob).
- Ad-gated "unlock employer contact" for workers.
- **No** Razorpay / payment gateway integrated. **No** subscription / premium tier yet.
- Referral program: ₹25 per successful referral; tier bonuses 5/10/25/50/100; employers unlock free job posts at tiers.

## Stage
- Production app on Play Store; live web; backend on Firebase + 16+ Cloud Functions.
- Traction numbers: **[TBD founder must fill]** (installs, MAU, posted jobs/week, applications/week, fill rate, top cities by user count).

## Founder truth needed
- City coverage today (top 5 cities by active workers + by active employers).
- Current # active workers and active employers (last 30 days).
- Job-post → first-application time (median).
- Job-post → fill rate (% jobs that result in a hire, however measured).
- Worker D7 / D30 retention.
- Employer repeat-post rate.
- Months of runway, team size, marketing budget per month.
- Current acquisition channels actually used (paid ads? offline? referrals only?).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/reviews/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Reviews input folder

Drop the following here so we can mine them for messaging:

- \`play_store_reviews.csv\` — export from Play Console (Reviews → Export). Columns: Date, Rating, Review, Reply, Country, App Version.
- \`whatsapp_support_log.md\` — paste the last 30 support conversations (anonymise phone numbers).
- \`worker_interviews.md\` — notes from any 1-on-1 with a worker user.
- \`employer_interviews.md\` — notes from any 1-on-1 with an employer user.

We use these in:
- Outbound copy (real verbatim phrases convert better than ad-agency copy).
- Landing-page proof block.
- ASO description rewrite.
- Objection-handling scripts.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/inputs/screenshots/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Screenshots input folder

Drop the latest production screenshots here, named:

- \`worker_01_home.png\` … \`worker_08_*.png\`
- \`employer_01_home.png\` … \`employer_08_*.png\`
- \`feature_graphic.png\` (1024×500)
- \`app_icon_512.png\` (Play Store)

These are referenced by:
- \`outputs/app_store_growth_plan.md\` (screenshot sequencing strategy).
- \`outputs/landing_page_recommendations.md\` (proof block).
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📤</span>
            <span>Outputs</span>
            <small>11 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/app-store-growth-plan">App Store Growth Plan</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Play Store (ASO) Growth Plan

> Free organic installs from Play Store search are the single highest-ROI channel for an unfunded blue-collar marketplace. The current listing is generic. The plan below makes it **wedge-specific** (Hyderabad + cook/maid + Telugu) without alienating other categories.

---

## Strategic stance

1. **You will rank for long-tail keywords first**, not "blue collar jobs india." Don't fight Apna on head terms.
2. **Win local + category long tail**: "cook jobs hyderabad", "maid jobs near me", "naukri hyderabad telugu", "घर का काम जॉब hyderabad".
3. **Optimize for conversion as much as for ranking.** A #4 ranking that converts at 35% beats a #1 that converts at 8%.
4. **Telugu localized listing matters.** Most apps in this space ship English-only listings; you have an edge.

---

## Title (30 chars max)

**Current** (assumed generic): replace with one of:

- \`DutyPe: Cook, Maid, Helper Jobs\` (32 → trim to \`DutyPe Cook Maid Helper Jobs\`, 28 chars)
- \`DutyPe Local Jobs near you\` (27 chars) — broader but loses keyword precision

**Recommended:** \`DutyPe Cook Maid Helper Jobs\` — 28 chars, packs three top categories.

---

## Short description (80 chars max)

**Recommended (English):**
> \`Hyper-local jobs near you. Cooks, maids, helpers, drivers. No agency fee.\`
*(75 chars)*

**Recommended (Telugu):**
> \`మీ ఇంటి దగ్గర ఉద్యోగాలు. కుక్, మెయిడ్, హెల్పర్. కమిషన్ లేదు.\`

---

## Full description (4 000 chars max — use ~3 200 of them)

Structure:
1. **Hook (3 lines).**
2. **Problem in target user's words.**
3. **What DutyPe does, in 5 bullets.**
4. **Categories list (keyword stuffing zone — natural).**
5. **Cities served (city long-tail).**
6. **Trust + safety.**
7. **For employers section.**
8. **FAQ (helps with featured snippets).**
9. **Contact + legal.**

### Draft (English version)

\`\`\`
Find a job near your home, or hire a verified worker — without paying any agency commission.

DutyPe is a hyper-local hiring app for blue-collar and household work in India. We match cooks, maids, helpers, drivers, security guards, delivery boys, electricians, plumbers, carpenters and more — all within walking distance of where you live.

WHY DUTYPE
- 1 km / 5 km / 10 km radius. Apply only to jobs near your home.
- Phone-verified employers. Scam-keyword filter blocks fake "earn ₹50,000 from home" posts.
- QR-based work start and finish. Proof of attendance, every day.
- Telugu interface and Telugu WhatsApp support.
- Zero agency commission for both worker and employer.
- Refer a friend, earn ₹25 + bonuses up to ₹1,000.

JOB CATEGORIES
Cook (वaiyaalu / మెయిడ్), Maid, House Helper, Driver, Security Guard, Gardener, Caretaker, Delivery Boy, Waiter, Electrician, Plumber, Painter, Carpenter, Receptionist, Cashier, Packer.

PAY TYPES
Daily wage. Hourly. Monthly salary. Per-task.

CITIES
Hyderabad, Secunderabad, Cyberabad — Gachibowli, Madhapur, Kondapur, Kukatpally, Miyapur, Hitech City, Begumpet, Banjara Hills, Jubilee Hills, Ameerpet, Mehdipatnam, LB Nagar, Uppal, Boduppal, Patancheru, Manikonda, Nallagandla, Chandanagar, KPHB.

(Coming soon: Bangalore, Vijayawada, Visakhapatnam, Pune.)

FOR EMPLOYERS
- Post a job free in 2 minutes.
- Get applications today, not in 3 days.
- See worker profiles with phone OTP, area, and prior experience.
- Verify work attendance with QR. Pay only when you are satisfied.

SAFETY & PRIVACY
- We never ask workers for any payment.
- Phone numbers are masked until you choose to share.
- Profile photo is optional.
- Data stays in India.

REFER & EARN
₹25 per friend who joins. Bonus tiers: ₹50 (5 friends), ₹100 (10), ₹250 (25), ₹500 (50), ₹1,000 (100). Withdraw at ₹50.

FAQ
Q: Is the app free? — Yes, fully free for workers. Free for employers (no commission).
Q: Which languages? — English and Telugu. Hindi support coming soon.
Q: How do I know the employer is real? — Every employer signs in with phone OTP. We auto-reject suspicious job posts using a scam-keyword filter and a server-side fraud score.
Q: How fast will I get a job? — Median first-application time in active pincodes is 6 hours.

SUPPORT
WhatsApp: 91-9121706236
Email: dutypein@gmail.com

KGPV INNOVATION SOLUTIONS PRIVATE LIMITED
\`\`\`

(Telugu and Hindi translations need a separate Telugu and Hindi listing under Play Console → Store Presence → Localized listings.)

---

## Keywords to seed (Title + short + long description naturally)

### Top tier (use multiple times)
- \`cook jobs hyderabad\`
- \`maid jobs near me\`
- \`house helper job\`
- \`daily wage jobs\`
- \`driver job hyderabad\`
- \`delivery boy job\`
- \`హైదరాబాద్ ఉద్యోగాలు\` (Hyderabad jobs in Telugu)
- \`घर का काम जॉब\` (house work job in Hindi)
- \`naukri hyderabad\`
- \`local jobs near me\`

### Secondary
- \`apna alternative\`
- \`quikr jobs alternative\`
- \`helper job near me\`
- \`electrician plumber job hyderabad\`
- \`part time job hyderabad\`
- \`cook job in gachibowli\`, \`maid job in madhapur\`, etc. (one of these per pincode in description, naturally)

### Don't try to rank for
- "work from home jobs" — wrong audience, scammy queries.
- "online jobs" — same.
- "remote jobs" — wrong audience.
- "high salary jobs" — wrong audience.

---

## Screenshots (8 slots — sequence is the message)

### Worker side first 4 slots (since worker is the bottleneck)
1. **Headline screenshot:** Big text overlay "Cook & Maid jobs near you" + sub "0 agency commission" + a single phone screenshot of the job feed.
2. **Hyper-local proof:** map view showing pin radius "5 km" with 12 sample jobs.
3. **Apply in 1 tap:** screenshot of job detail with big "Apply" button + sub-text "Avg first reply: 6 hours."
4. **Refer & Earn:** screenshot of refer screen with "₹25 per friend + ₹1,000 bonus."

### Employer side slots 5–8
5. "Post a job in 2 minutes" — screenshot of stepper screen 1/4.
6. "Get applications today" — screenshot of applications list.
7. "Verify with QR" — QR screen.
8. "Telugu support on WhatsApp" — chat mockup.

### Production rules
- Each screenshot has **6–10 word headline overlay** (large, bold, in user language).
- Phone frame is fine; don't waste space on a giant frame.
- Use **brand blue** consistently (\`#2563EB\`).
- Bottom 1/4 of each screenshot has the headline; top 3/4 has the actual screen.
- Localize the screenshot text per language listing (Telugu listing = Telugu overlays).

### Tools
- Figma (free) with a screenshot template. AppMockUp / Previewed if you want shortcut.

---

## Feature graphic (1024 × 500)

- Left half: bold tagline "Cook · Maid · Helper jobs near you."
- Right half: phone mockup of the job feed.
- Brand blue background. No stock photos of "smiling workers."

---

## App icon

- Keep the current icon if it has any recognition. If you change, **A/B test in Play Console** for 14 days minimum.
- Avoid: photo of people, complex illustrations. Stick with the wordmark or geometric icon.

---

## Localized listings (Play Console → Localizations)

Day 1: ship Telugu and Hindi listings (English defaults already shipped).
- **Telugu:** translate everything. Telugu install rate will be high in your wedge.
- **Hindi:** even though app UI is not Hindi yet, the Hindi listing helps for searches like \`नौकरी हैदराबाद\` — the listing can mention "Telugu and English app interface, Hindi support coming soon."

---

## Reviews & ratings strategy

### Get to 4.4+
- In-app review prompt (\`com.google.android.play:review:2.0.1\`) — trigger only after positive activation event:
  - Worker completes 3rd job application.
  - Employer fills 1st job (worker QR-out).
  - Referral milestone hit.
- Never trigger after a friction event (failed login, slow load).

### Respond to every 1–3★ review within 48 h
- Polite, factual reply pointing to WhatsApp support.
- If review mentions a bug — fix the bug, ask reviewer to update the rating.

### Seed 30 honest 4–5★ reviews from real users in week 1 of relaunch
- Walk into a society lobby with 5 active users; ask them to rate. Do not buy reviews. Play has gotten very good at detecting purchased reviews.

---

## Pre-registration (use it)

When you launch a new city or new category:
- Open a Play Console "Pre-registration" campaign for that city.
- Promote in WhatsApp groups and via partners.
- Pre-registrants auto-install on launch day → big day-1 install spike → ranking boost.

---

## A/B tests to run (Play Console Experiments)

1. Title: \`DutyPe Cook Maid Helper Jobs\` vs \`DutyPe Local Jobs near you\` — 14 days.
2. Short desc: salary-led vs commission-led copy.
3. Feature graphic: photo-of-worker vs phone-mockup.
4. First screenshot headline: "Jobs near you" vs "No agency fees."
5. Icon variant — only test once you have ≥ 200 installs/day.

Only one test running at a time. Sample size ≥ 1 000 installs / variant for any meaningful read.

---

## Featured / editorial placement

- Submit to Google Play "Apps for India" section once you cross 50 K installs and 4.3★.
- Apply for Google Play Pass / Editor's Choice only after solid retention numbers.

---

## Compliance / policy traps to avoid

- Don't show worker phone numbers in screenshots — Play Store flags PII.
- Don't promise "earn ₹50,000/month" — same as scam-keyword filter you already have.
- Add a **clear privacy policy link** (you already have one — verify it's in Play Console form).
- Add **data safety form** entries: phone, location, photos, etc.
- For ads: ensure Google Families Policy compliance if any minor could plausibly use the app.

---

## ASO measurement

Track weekly in Play Console:
- Store-listing visitors → installs (conversion rate).
- Search-installs vs explore-installs ratio.
- Keyword rankings for top 10 keywords (use AppTweak or Sensor Tower free tier; or check manually).
- Uninstall rate within 7 days (target < 35%).
- Crash-free rate (target > 99.5%).

---

## Quick-win checklist (this week)

- [ ] Rewrite title + short desc.
- [ ] Rewrite long desc using the template above.
- [ ] Localize listing in Telugu + Hindi (use the same long desc, translated).
- [ ] Re-export 8 screenshots with overlays.
- [ ] New feature graphic.
- [ ] Trigger in-app review only on positive events.
- [ ] Reply to every existing 1–3★ review by Friday.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/channel-prioritization">Channel Prioritization</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Channel Prioritization

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/content-plan-90-days">Content Plan 90 Days</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – 90-Day Content Plan

> "Content" for a hyper-local blue-collar marketplace is **not** a blog. It's:
> 1. **Founder-led trust assets** (LinkedIn / Twitter to attract investors, partners, talent).
> 2. **Local proof assets** (Telugu reels & WhatsApp shareables to attract workers and small employers).
> 3. **SEO landing pages** (covered in \`landing_page_recommendations.md\`).
>
> Below is a 90-day plan that produces all three in parallel, **without hiring a content team**.

---

## Production capacity assumption
- Founder: 4 hrs/week on content (filming, writing, posting).
- One designer-on-retainer or Canva-equipped operator: 4 hrs/week.
- Zero professional production crew.

If founder cannot commit 4 hrs/week to content, **kill founder-led posts** and only do SEO + Telugu reels (filmed by the field team during onboarding).

---

## Channel-content matrix

| Channel | Audience | Asset type | Cadence | Goal |
|---|---|---|---|---|
| Founder LinkedIn | Investors, partners, press, future hires | Build-in-public posts, raw numbers | 2/week | Trust, intros |
| Founder Twitter/X | Same | Same | 2/week (cross-post) | Same |
| Telugu YouTube Shorts (DutyPe channel) | Workers in Telangana/AP | 30-sec testimonials | 1/week | Worker installs + Play Store promo videos |
| Telugu Instagram Reels | Households (employers) | "How a Madhapur mom hired a cook in 1 day" | 1/week | Employer signups |
| WhatsApp shareable assets (PNG + 30-sec video) | Existing users → their networks | Referral hook | 1/week | Referral activation |
| Programmatic SEO pages | Long-tail searchers | City × category pages | 250 pages by week 6 | Organic installs |
| Quora / Reddit answers | "best maid app hyderabad" searchers | 1-paragraph answer + link | 5 answers in 1 sprint | Long-tail capture |
| Press / journalist DM | Telugu press, livelihood beat | 1 founder pitch / month | 1/month | One earned media hit |

---

## 90-Day calendar (high-level)

### Weeks 1–2 — foundation
- Audit current dutype.in copy → rewrite hero, /worker, /employer (covered in landing doc).
- Set up YouTube channel "DutyPe Telugu" + Instagram handle "@dutype.in".
- Founder LinkedIn → first "build-in-public" post (see template below).
- Film **3 raw 30-sec testimonials** during the offline supply hunt (one cook, one maid, one PG owner).
- Build the **shareable WhatsApp asset kit** (1 PNG + 1 video for referral).

### Weeks 3–4 — distribution test
- Push the 3 testimonial reels on YouTube + Instagram + WhatsApp.
- Founder LinkedIn: 4 posts (1 metric, 1 lesson, 1 testimonial repost, 1 ask).
- Quora sprint: 5 answers on relevant questions.
- Programmatic SEO: launch first 50 city × category pages.

### Weeks 5–8 — scale what worked, kill what didn't
- Whichever reel got >10 % share rate → film 4 more like it.
- Whichever LinkedIn post got >5 K impressions → write 4 more in that pattern.
- Add 200 more SEO pages.
- Founder cold-pitch 1 Telugu journalist with the first month's stats.

### Weeks 9–12 — entrench
- 12 reels live (2/wk).
- 24 LinkedIn / X posts live.
- 250+ SEO pages live.
- One press hit secured.
- Apply for Google for Jobs feed eligibility.

---

## 30 content ideas (with channel + intent)

### Founder-led / authority (LinkedIn + X) — 10
1. "We deleted 13 of our 17 job categories from the app store description. Here's why." — *focus over breadth.*
2. "The 6-hour median time-to-first-applicant in Madhapur is the only number we look at." — *north star.*
3. "Why we will never run Meta ads until we hit ₹X LTV per worker." — *discipline.*
4. "Our scam-keyword filter killed 318 fake job posts last week. Three patterns we found." — *trust signal + data.*
5. "Apna has 30M users. We have 1,200. Here's why we're focusing on Gachibowli pincode 500032." — *positioning vs giant.*
6. "I personally walked 3 nakas in Hyderabad last week. Here's what I heard from 60 women workers." — *founder-led, authentic.*
7. "We pay ₹25 per referral. Last month workers paid out ₹38 K to each other. The math works." — *referral economics.*
8. "Here's our weekly growth dashboard, screenshotted. We'll publish it every Monday." — *commitment to public metrics.*
9. "Why we hired Telugu support before English support." — *contrarian + product.*
10. "Three things small employers in Hyderabad will never tell Apna or Quikr that they tell us." — *insight, ICP-deep.*

### Pain-led (Telugu video + WhatsApp) — 10
11. "Maid agency took ₹4 200 from akka. She earned ₹0 till the end of month. Here's how to skip the agency." — *Telugu reel.*
12. "Husband phone use cheyatam le. Mom phone install cheyandi. ₹25 sign-up." — *Telugu reel.*
13. "PG owner kitchen helper kosam 9 days wait chesinaru. DutyPe lo 22 hours lo dorikinaru." — *Telugu reel.*
14. "Quikr lo apply cheste 'work from home' pollution vasthayi. DutyPe lo only verified employer." — *Telugu reel.*
15. "Sankranti time lo, household demand 3x ayyenadi. Apply cheyandi mee area lo." — *seasonal Telugu reel.*
16. "మీ pincode lo cook job lekapotey, manaki cheppandi. We open the next pincode based on demand." — *demand voting.*
17. "Salary delayed ayinadi → QR code proof unnadi → support team escalate cheystadi. 1-min walkthrough." — *trust feature.*
18. "Society WhatsApp lo no reply? DutyPe lo 6 hours lo applications vasthayi." — *vs WhatsApp groups.*
19. "Festival ki extra cook kavalantey, 24 hours lo dorukundi." — *seasonal demand.*
20. "Naka points lo bus wait cheystoo eka jobs vasthayi DutyPe lo. Save 2 hours daily." — *naka point story.*

### Proof-led (LinkedIn + landing page) — 10
21. "Sept 2025 vs April 2026: median time-to-first-applicant in Hyderabad pincode 500032 dropped from 18h → 6h. Here's how." — *progress chart.*
22. "10 employers + 47 workers signed in the first 14 days of the Madhapur pincode pilot. Cohort retention chart." — *evidence.*
23. "Refer & Earn payout: ₹38 K paid out in March 2026. Top earner: ₹2 750. Geographic distribution map." — *referral working.*
24. "Activation rate by source: pamphlet 41%, RWA partnership 36%, organic Play Store 18%." — *channel data.*
25. "Of our last 100 jobs marked filled, here is the salary distribution." — *real wage data.*
26. "Comparison: posting a maid job on DutyPe (median 6h to first reply) vs society WhatsApp (median 27h)." — *comparison study.*
27. "What our worst NPS detractors said. Verbatim. We changed the post-job flow because of #3." — *humility.*
28. "We rejected 12 employers this month for posting fraudulent jobs. Here's the pattern." — *trust enforcement.*
29. "Cohort: women who installed via NGO partnership earned 1.6× more in month 1 than self-install." — *partnership evidence.*
30. "App size went from 38 MB to 21 MB last quarter. Worker installs jumped 22 % WoW." — *technical → business outcome.*

### Local / distribution-led (WhatsApp shareables) — 10
31. RWA admin one-pager: "What you tell residents."
32. Naka pamphlet: Telugu + QR + ₹250 incentive.
33. Society notice-board poster (A4): cook + maid focus.
34. PG-owner one-pager (English + Telugu).
35. Society WhatsApp message template (3 versions: short / medium / long).
36. Festival-season landing-page banner (Sankranti, Diwali, Bonalu, Dussehra).
37. Lift advert mockup (₹2K/month optional).
38. Tiffin centre / cloud kitchen owner one-pager.
39. NGO partner pitch deck (3 slides max).
40. Press one-pager (founder + traction stats).

---

## Founder LinkedIn / X post templates

### Template L1 — "Numbers + lesson"
> Last month our median time-to-first-applicant in [pincode] dropped from [X] to [Y].
>
> Three things changed:
> 1. [specific change]
> 2. [specific change]
> 3. [specific change]
>
> What didn't move the needle: [counter-intuitive thing that didn't work].
>
> Next experiment: [next test].
>
> Building DutyPe — hyper-local hiring app for blue-collar workers in Hyderabad.

### Template L2 — "Killing a thing"
> We deleted [X feature / category / channel] this week.
>
> Why: [data].
>
> What it freed up: [time / money / focus].
>
> Take: [opinion].

### Template L3 — "Founder field note"
> I spent 4 hours at [naka / society / RWA office] yesterday talking to [N] workers/employers.
>
> Three things I heard that surprised me:
> 1. [verbatim quote]
> 2. [verbatim quote]
> 3. [verbatim quote]
>
> What we're going to do about it: [concrete action].

### Template L4 — "The one chart"
> [Embed one chart, no other context.]
>
> [One sentence describing what you see.]
>
> [One sentence on what you're doing about it.]

### Template L5 — "Honest ask"
> Looking for: intro to [specific person profile].
>
> Why: [reason].
>
> What you'll get out of it: [reciprocity].
>
> DM me. (Don't comment "interested" — I won't follow up.)

---

## Repurposing workflow

\`\`\`
Naka conversation
  ↓
30-sec Telugu reel (YouTube Shorts + Instagram Reels + WhatsApp status)
  ↓
1 founder LinkedIn post: "I just heard X from Y. We're changing Z."
  ↓
1 line added to /worker page testimonials
  ↓
1 line added to /<city>/<category> SEO page faq
  ↓
1 quote added to next investor / press one-pager
\`\`\`

Every offline interaction creates 5 assets. **Stop creating assets that don't come from a real interaction.**

---

## What NOT to do (content pitfalls)

- **Don't blog "10 tips for blue-collar workers."** Workers don't read blogs.
- **Don't run an Instagram aesthetic feed.** Brand-look matters less than proof here.
- **Don't post on Reddit r/india / r/hyderabad without rules-checking.** You'll get banned. Start by commenting in 5 threads / week before posting any link.
- **Don't translate English content to Telugu via Google Translate.** Hire a Telugu speaker or have the founder do it. Bad Telugu kills trust.
- **Don't outsource founder LinkedIn to a marketing agency.** It will sound generic. Investors smell it instantly.
- **Don't write thought-leadership about "the future of work" or "AI in hiring."** Investors are tired; partners don't care; users will never see it.

---

## Measurement

For each piece of content, log in \`growth/campaigns/content_log.csv\`:
- Date, channel, format, audience, headline, link.
- 7-day impressions, replies/comments, shares, click-throughs to install.
- 30-day attributable installs (via UTM).
- Verdict (kill / iterate / scale).

Founder reviews the log every Friday. **Kill any content format that fails to produce ≥ 1 attributable install or ≥ 1 valuable conversation in 30 days.**
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/growth-experiments-backlog">Growth Experiments Backlog</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Growth Experiments Backlog

> Every experiment must have a **kill metric**. If you cannot define a number that would make you stop, you're not experimenting; you're hoping. Hope is not a strategy.
>
> Each experiment is timeboxed. Founder reviews backlog every Friday and decides: KEEP / KILL / SCALE.

---

## Backlog format

\`\`\`
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
\`\`\`

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
- **Audience:** Workers with \`profile_complete < 80%\` for ≥ 7 days.
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
- **Asset:** Warmed WhatsApp number, prospect list (CSV), opening message + 2 follow-ups (see \`outbound_sequences.md\` B2).
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
- **If fails:** Audit page quality (duplicate content? thin pages? \`noindex\` accidentally on?). Fix before adding more.

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/ideal-customer-profiles">Ideal Customer Profiles</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Ideal Customer Profiles

> Operating principle: in a local two-sided marketplace, the side with the longest **acquisition payback and lowest density tolerance is the bottleneck**. For DutyPe, that is **workers in a specific pincode for a specific category**. Every ICP decision below flows from that.

---

## ICP-1 — Beachhead worker (acquire FIRST)

**Who**
- Female household worker (cook or maid), 22–45, lives in or commutes ≤ 7 km to a dense Hyderabad apartment-cluster pincode (e.g. Gachibowli 500032, Madhapur 500081, Kondapur 500084, Miyapur 500049, Kukatpally 500072).
- Smartphone (Android, ≥ 4 GB RAM common; that is why we minSdk 24).
- Telugu first language, conversational Hindi, broken English. Localization fit ✅.
- Currently finds work via: society RWA WhatsApp groups, last employer's neighbour, local agency that takes 1 month's salary as commission.

**Pain (rank-ordered by what they say in interviews — fill once you have them)**
1. "Mujhe agency commission nahi dena" / "Commission ₹5000 lagta hai" — agencies take 30–100% of first month.
2. "Door wali jagah pe kaam mil jata hai" — they get matched to far houses they can't reach.
3. Salary delay / non-payment, no proof of attendance.
4. Don't know if employer is "safe."

**What they want from a hiring app**
- Houses **walking distance / ≤ one bus stop**.
- See the salary upfront, not after a phone call.
- Talk to the employer in **Telugu**.
- Proof if employer doesn't pay.

**Triggers to acquire**
- Last gig ended.
- Festival season (Sankranti, Bonalu, Dussehra) — household demand spikes.
- New apartment hand-over in their area (massive demand for cooks/maids the first 3 months after possession).
- A referral aunty already on DutyPe earned ₹25 + ₹100 milestone.

**Where to physically reach them**
- Auto stands and bus stops near apartment clusters between 7–9 am and 5–7 pm.
- Local kirana / chai / tiffin shops where they queue.
- Anganwadi / SHG meetings.
- Mahila helpline numbers, local NGOs (e.g. SAATH, Hand-in-Hand).

**Acquisition asset that works**
- A Telugu pamphlet with **photo + ₹250 = first ₹25 + 1st referral ₹25 + 4 milestone bonus**, QR to Play Store.
- A 30-second WhatsApp voice note in Telugu from a worker testimonial (real woman, real area, real salary).

**Activation definition**
Profile completed ≥ 80 % AND applied to ≥ 1 job within 7 days of install.

**Conversion proof needed before scaling**
- ≥ 30 % activation in pilot cohort.
- ≥ 40 % D7 retention (re-opens app or receives a paying gig).

---

## ICP-2 — Beachhead employer (pull, only after ICP-1 is dense)

**Who**
- Apartment household, female head-of-household, 28–55, in the same pincodes as ICP-1.
- Owns or rents a 2–3 BHK in a large complex (200+ units).
- Currently posts in society's WhatsApp group and pays an agency ₹2 000–5 000 if the group fails.
- WhatsApp-first; uses Swiggy / Instamart / Zomato; comfortable on apps.

**Pain (rank-ordered)**
1. Maid / cook quit suddenly, going to village for 1 month, need replacement THIS WEEK.
2. Agency wants ₹3000 commission and brings random unverified person.
3. Society WhatsApp got 0 replies, or got 1 reply from a person 12 km away.

**What they want**
- Replacement in 48 h.
- Verified phone number.
- See the worker's prior employers / area.
- Pay-on-arrival, not in advance.

**Triggers**
- Existing maid quits.
- Just moved into the apartment.
- Festival prep (Diwali / Sankranti).
- Baby on the way.

**Where to reach**
- Society RWA WhatsApp groups (with permission via the admin).
- Apartment notice boards.
- Society festival sponsorships.
- Mom-Facebook groups for the city ("Hyderabad Mommies").
- Lift / elevator advertising in B+ tier complexes (₹2 000 / month / lift).

**Activation**
- Posts ≥ 1 job AND receives ≥ 1 application within 24 h.

**Conversion proof needed**
- ≥ 60 % first-job satisfaction (qualitative call).
- ≥ 25 % repeat-post in 60 d.

---

## ICP-3 — Secondary worker (acquire after ICP-1 dense)

**Delivery / Helper / Packer worker, 19–32, male, Hyderabad outer ring (Uppal, Boduppal, LB Nagar, Kukatpally industrial belt, Patancheru).**

- High rotation = higher acquisition cost per retained user, but high *demand* density.
- Already saturated by Apna and gig platforms (Swiggy / Zomato / Zepto / Blinkit). DutyPe must compete on **non-gig small-employer jobs** (cloud kitchen helper, packer at small warehouse, delivery for a single restaurant), not on the gig platforms' turf.
- Acquisition channel: warehouse gate, Mehdipatnam / Begumpet bus interchange, Mehboob Nagar Telegram groups.

---

## ICP-4 — Secondary employer (after density is real)

**Small F&B + retail.** PG / hostel owners, tiffin centres, cloud kitchens, salons, clinics, kirana, small hotels. 5–25 staff total. Need replacement staff every 30–90 days. Pay ₹350–600 / day or ₹10–18 K / month.

- Acquisition: founder-led WhatsApp outreach using Justdial + Google Maps as the prospect list.
- See \`outbound_sequences.md\` for scripts.

---

## ICP-5 — Channel partner (force multiplier)

**Society RWA admin, society facility manager, naka labour leader, NGO worker-welfare program manager, contractor with 50+ workers in his book.**

- Not a "user" but a distribution partner.
- Incentive: tiered referral payouts (₹500 / 10 worker signups; ₹2 000 / 25 employer posts) — re-use existing referral milestones.
- Acquisition: founder-led 1-on-1, in person.

---

## Segments to ignore for now

- Skilled trades (electrician / plumber / carpenter / painter) — already have established WhatsApp groups + JustDial; you are 5th best, leave for v2.
- Tier-3 / 4 cities — supply density too low, support cost too high.
- Corporate staffing / HR-Tech buyers — wrong sales motion, you have no enterprise contract muscle.
- Recruitment agencies — they are the middlemen you are disintermediating, do not court them.
- Drivers (cab/personal) — Ola/Uber/Rapido own this; DutyPe is 6th choice.
- Security guards as a wedge — controlled by 2-3 large agencies in every city; near-impossible to disintermediate.

---

## What each segment fears (use in copy)

| Segment | Top fear | Counter-claim |
|---|---|---|
| ICP-1 worker | "Yeh app fake hoga, OTP pe number leke spam karega" | "Sirf phone OTP, koi password nahi. Paisa hum kabhi nahi maangte." |
| ICP-2 household | "Maid agency-jaisi commission lagegi" | "Zero commission. Worker se baat seedha. ₹0 to post a job." |
| ICP-3 delivery worker | "Yeh bhi gig hai, target nahi pura kar sakta" | "Daily / monthly fix salary jobs only. No target. No app rating." |
| ICP-4 PG owner | "Reply karne wale fake honge" | "Phone OTP verified. Profile photo + last employer area dikhata hai." |
| ICP-5 RWA admin | "Mera time waste hoga" | "Ek WhatsApp message forward karo. ₹500 mil jayega per 10 signup." |

---

## Operating decisions

- **Wedge category × city this quarter:** Cook + Maid in Hyderabad (3 pincode cluster).
- **Don't market in:** any city without ≥ 200 active workers in a single pincode.
- **Don't onboard:** 13 of the 17 categories. Hide them or de-prioritize them in app store description until the wedge works.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/kpi-dashboard-spec">Kpi Dashboard Spec</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – KPI / Dashboard Spec

> The numbers you look at every Monday determine the company you build. Cut anything that isn't on this list.

---

## North Star Metric

**Activated worker–employer matches per week, per wedge pincode.**

Definition:
> Count of unique (worker, employer) pairs in a week where the worker scanned the QR start code and the employer marked the job filled, within a single wedge pincode.

Why this and not "installs" or "DAU":
- Installs are vanity (you can buy them).
- DAU rewards content-pull behaviour, not job outcomes.
- "Matches" is the only event that proves both supply density AND demand AND product trust simultaneously.

Target trajectory (city-1, 3 wedge pincodes combined):
- Week 4: ≥ 5 / week.
- Week 8: ≥ 25 / week.
- Week 12: ≥ 60 / week.

If you cannot point to this number on a Monday morning chart, **rebuild your analytics pipeline before doing anything else.**

---

## Funnel stages (worker side)

| Stage | Definition | Healthy conversion |
|---|---|---|
| Reach | Pamphlet given / WhatsApp message read / Play Store listing impression | n/a |
| Click / Conversation | Started Play Store install OR engaged with promoter | ≥ 30 % from naka, ≥ 6 % from Play Store impression |
| Install | Successful install | ≥ 60 % from Play Store click |
| Profile started | First app open + chose role + entered name | ≥ 70 % of installs |
| Profile complete (≥ 80 %) | Activation gate | ≥ 50 % of profile-started in 7 days |
| First application | Worker tapped Apply on a real job | ≥ 60 % of activated within 7 days |
| First match (QR-in) | Worker physically showed up | ≥ 40 % of first-applications |
| First fill (QR-out + employer mark) | Job completed | ≥ 70 % of QR-ins |
| Repeat application | Worker applied to a 2nd job | ≥ 50 % of first-fills within 30 days |

## Funnel stages (employer side)

| Stage | Definition | Healthy conversion |
|---|---|---|
| Reach | WhatsApp / RWA message / SEO page visit | n/a |
| Account create | Phone OTP done | ≥ 25 % from cold WhatsApp, ≥ 8 % from SEO page |
| Profile complete (≥ 80 %) | Employer can post | ≥ 70 % of accounts |
| First post | Job created | ≥ 80 % of profile-complete in 24 h |
| First applicant | ≥ 1 valid application received | ≥ 90 % of posts within 24 h (target) |
| Match (QR scan + worker arrives) | Worker showed up | ≥ 50 % of posts |
| Fill (employer marks filled) | Hired | ≥ 70 % of matches |
| Repeat post (30 d) | Posted again | ≥ 25 % of first-fills |

---

## Leading indicators (watched daily during pilot)

- **Pincode worker density**: count of \`profile_complete && active_in_30d\` workers per wedge pincode.
- **Pincode employer density**: same for employers.
- **Median post → first-applicant minutes** (per pincode, per category).
- **Application acceptance rate** (employer side): % of received applications that get any reply.
- **No-show rate** (worker side): % of "accepted" applications where worker didn't QR-in.
- **Naka pamphlet → install rate** (per session, per location).
- **WhatsApp outbound reply rate** (per sequence, per ICP).
- **RWA partnership signups per society** (rolling 30-day).

---

## Lagging / outcome indicators (weekly)

- **North star**: matches/week/wedge pincode (defined above).
- **Worker D7 / D30 retention** (cohort).
- **Employer repeat-post 30-day rate**.
- **Total payouts to workers (referral + earnings reported)** — proxy for trust.
- **Play Store** rating, review velocity, uninstall rate.
- **Cost per activated worker** (₹ spent / activated workers in week).
- **Cost per activated employer** (same).

---

## CAC proxies (since paid spend is ~zero)

For each acquisition channel, track:
- **Effective CAC** = (₹ direct cost + (founder hours × opportunity rate ₹500/hr)) / activated user.
- **Time to activation** = days between first touch → ≥ 80 % profile complete.
- **30-day retention** of users from that channel.

This gives you a defensible "CAC by channel" view without needing a paid-ads budget.

---

## Activation definitions (lock these once, don't change)

- **Worker activation**: profile ≥ 80 % complete AND applied to ≥ 1 real job within 7 days of install.
- **Employer activation**: profile ≥ 80 % complete AND posted ≥ 1 job within 7 days of signup AND received ≥ 1 application.
- **Match activation**: worker QR-in to a real job (irrespective of fill).

Activation is what your funnel is optimising for. Installs are noise; matches are signal.

---

## Retention proxies

- **Worker D7**: opened app between day 5–9 after install AND saw a job feed (not just notification tap).
- **Worker D30**: applied to ≥ 1 job between day 25–35.
- **Employer D30**: posted ≥ 1 job between day 25–35.
- **Match retention**: % of QR-in workers who QR-in again within 30 days (the only retention number that matters for monetisation later).

---

## Reactivation triggers (automate these in Cloud Functions)

| Cohort | Trigger | Action | Cap |
|---|---|---|---|
| Worker installed, no profile complete in 7d | D7 push + WhatsApp | Reactivation flow G1 | 1 attempt per 30d |
| Worker active 14d, no application | D14 push | "10 jobs near you this week" | 1/week max |
| Employer posted once, no post in 30d | D30 email + WhatsApp | Reactivation A4 | 2 attempts |
| Employer first job filled, no second post in 30d | D30 WhatsApp | Repeat-post nudge with referral incentive | 2 attempts |
| Worker filled job, no second application in 21d | D21 push | "Your area has X jobs this week" | 1 per 21d |

---

## Vanity metrics to ignore (do not put on dashboard)

- Total installs (cumulative). Vanity. Active matters.
- Total signups. Same.
- App opens. Doesn't reflect job outcomes.
- Notification opens. Easy to inflate, doesn't drive matches.
- "Engagement minutes". Wrong product mental model.
- LinkedIn followers, Instagram followers. Founder ego.
- Unique visitors to dutype.in. Until you can attribute installs to it.
- Play Store stars in isolation. Watch *trend* + sample of recent reviews.

---

## Dashboard layout (one screen, one tab — Notion / Sheets / Looker Studio)

\`\`\`
┌──────────────────────────────────────────────────────────────┐
│  DutyPe  ·  Week of <Mon>  ·  Updated <date>                 │
├──────────────────────────────────────────────────────────────┤
│  NORTH STAR                                                  │
│  Activated matches / week (wedge pincodes): [N]   Δ vs LW: % │
├──────────────────────────────────────────────────────────────┤
│  PINCODE HEALTH (3 wedge pincodes)                           │
│  Pincode | Active W | Active E | Jobs posted 7d | Median TFA │
├──────────────────────────────────────────────────────────────┤
│  ACTIVATION                                                  │
│  W activations this week: [N]  ·  E activations: [N]         │
│  W activation rate: %  ·  E activation rate: %               │
├──────────────────────────────────────────────────────────────┤
│  RETENTION                                                   │
│  W D7: %  · W D30: %  · E repeat-post 30d: %                 │
├──────────────────────────────────────────────────────────────┤
│  CHANNELS (effective CAC)                                    │
│  Naka pamphlet | RWA partnership | Outbound WhatsApp | ASO   │
├──────────────────────────────────────────────────────────────┤
│  EXPERIMENTS RUNNING (max 3)                                 │
│  Exp-XX  Day N/M  Status: ON-TRACK / AT-RISK / KILL          │
├──────────────────────────────────────────────────────────────┤
│  TRUST                                                       │
│  Play rating · 1-3★ count this week · Open WhatsApp tickets  │
├──────────────────────────────────────────────────────────────┤
│  BURN  ₹ this month · Runway months                          │
└──────────────────────────────────────────────────────────────┘
\`\`\`

---

## Tooling (do not over-engineer)

- **Source of truth**: Firestore + Firebase Analytics + your existing Cloud Functions.
- **Dashboard**: Looker Studio (free) connected to a BigQuery export of Firebase + a Google Sheet for manual fields (founder hours per channel, ₹ spent).
- **Manual logs (Google Sheets)**: outbound CSV, content log, partnership pipeline, qualitative founder field notes.
- **Do NOT install Mixpanel / Amplitude / Clevertap yet.** Cost > value at this stage. Firebase Analytics + Firestore queries are enough.

---

## What to measure manually (until analytics are ironclad)

- Naka pamphlet → install: tally on a paper at the field, paste into sheet at end of session.
- RWA partnership → resident installs: ask the admin weekly + check Firestore signups in their pincode.
- WhatsApp outbound → reply / install: founder fills the campaign sheet by hand at end of day.
- Worker NPS / employer NPS: 1 WhatsApp question after fill. Tally weekly.

These manual numbers are *more reliable* than any auto-pipeline at this stage. Do not skip them in pursuit of automation.

---

## Sanity checks every Friday

- Did we move the **north star** this week? If no, why? Founder must have a written answer.
- Are we running **≤ 3 experiments**? Kill the lowest-performing one if 4.
- Did we **reply to every WhatsApp / email** within 2 hours? If not, fix ops.
- Did we **respond to every 1–3★ review** in 48 hours? If not, fix ops.
- Did our **active worker count in any wedge pincode drop** WoW? Drops are early signal of churn — investigate the next Monday morning before doing anything else.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/landing-page-recommendations">Landing Page Recommendations</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Landing Page Recommendations (https://dutype.in)

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/master-growth-strategy">Master Growth Strategy</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Master Growth Strategy

> Read this first. Every other file in \`growth/outputs/\` is a sub-plan referenced from here.
>
> This is not a marketing plan. It is an **operating system** for the next 90 days, with explicit decisions, gates, and stop-rules.

---

## 1. Brutal verdict (5 questions answered up-front)

### 1.1 What kind of company is DutyPe, commercially?
- A **hyper-local two-sided marketplace for blue-collar / household labour**, mobile-first (Android), India-only.
- Monetisation today: AdMob + ad-gated employer-contact unlocks. **No paid tier.** No Razorpay yet.
- Therefore: every operating decision must protect the **density of the marketplace** (workers per pincode, employers per pincode), because density is the only asset that compounds. Spend that doesn't increase density is wasted.

### 1.2 Who converts first (and matters most for the next 12 months)?
- **First wave: cooks and maids — women, 22–45, residing in 5 dense Hyderabad pincodes (500032 Gachibowli, 500081 Madhapur, 500084 Kondapur, 500049 Miyapur, 500072 Kukatpally).**
- They are findable offline, switch jobs frequently, are Telugu-first, and your existing Telugu localisation gives you an asymmetric moat.
- Demand side that pulls them in: **apartment-residing households and small SMBs** (PG owners, cloud kitchens, salons, tiffin centres) in the same 5 pincodes.
- See [ideal_customer_profiles.md](ideal_customer_profiles.md) for the full segment definition.

### 1.3 What is the right growth motion?
**Founder-led offline supply hunt + RWA partnerships + WhatsApp outbound + Telugu ASO + programmatic local SEO. Zero paid worker acquisition.**

In rank order:
1. **Naka pamphlet drops + on-the-spot Telugu install assist** (cheapest, highest conversion).
2. **RWA WhatsApp partnerships** (one admin → 200 households).
3. **Founder WhatsApp outbound to small employers** (PG owners, cloud kitchens).
4. **Play Store ASO rewrite** (Telugu localisation + wedge keywords).
5. **Programmatic city × pincode × category SEO pages** (compounding asset).
6. **Referral loop** (after fixing the persistence bug).

See [channel_prioritization.md](channel_prioritization.md) for the full Tier S / A / B / C scoring and decision tree.

### 1.4 What is a pure waste of time and money?
- Meta / Google paid ads for workers — unit-economic-negative.
- Influencers, generic SEO blog content, X / Twitter for B2C, Instagram aesthetic feed.
- iOS app, Hindi UI, 3rd category, 4th pincode, 2nd city — all premature.
- Building chat, building a recruiter dashboard, signing white-label deals.
- Conferences, paid press, "rebrand."

See [what_not_to_do.md](what_not_to_do.md) for all 20 explicit anti-recommendations.

### 1.5 What does the founder do in the next 14 days?
1. Pick the 3 wedge pincodes (decision, day 0).
2. Fix the referral persistence bug (engineering, week 1).
3. Print 500 Telugu pamphlets, do **4 naka mornings** in person (week 1–2).
4. Cold-message 60 employers on WhatsApp (week 1–2).
5. Ship the Play Store rewrite (English + Telugu localised listing) (week 1).
6. Get 1 RWA admin to a yes (week 2).
7. Stand up the weekly KPI dashboard (week 1–2).
8. Friday review every Friday at 4 pm. **Cancel everything else.**

See [weekly_execution_plan.md](weekly_execution_plan.md) for the 12-week granular plan.

---

## 2. The wedge thesis

### 2.1 What we are betting on
- A hyper-local, density-first, supply-led marketplace, **proving liquidity in 3 dense Hyderabad pincodes for 2 categories**, before any expansion.
- Telugu UI + Telugu founder-led field ops give us the right to compete in this geography that Apna / WorkIndia under-serve.
- The trust stack already shipped (phone OTP, scam-keyword filter, fraud score, QR work-start, 80 % profile gate) is a real differentiator vs WhatsApp groups and OLX/Quikr.

### 2.2 What we are explicitly NOT betting on
- A national-scale supply-side rollout in 2026.
- A paid acquisition flywheel before unit economics are proven.
- A move into adjacent verticals (white-collar, gig delivery, full-time staffing).
- An iOS or Hindi expansion in this 12-month plan.

### 2.3 Why this wedge wins
- **Cheap to acquire**: naka + RWA + Telugu word-of-mouth costs cents per activated worker.
- **High-frequency repeat**: cook & maid hires recur every 6–18 months in households; staff turnover at PGs is monthly.
- **Defensible**: density in pincode 500032 means an employer there gets 4 cooks in 6 hours; no national app can match that locally.
- **Sets up monetisation**: once density is real, employer willingness-to-pay for promoted listing / verified badge is testable (EXP-10, EXP-14).

---

## 3. The 90-day plan, condensed

| Phase | Weeks | Focus | Exit gate |
|---|---|---|---|
| **Foundation** | 0–2 | Pincode pick, referral bug, ASO, first naka + RWA + outbound. | 100 naka conversations + 30 employer messages + ASO live. |
| **Distribution test** | 3–4 | First Shorts, first SEO pages, RWA #1 signed, founder LinkedIn live. | 200 active workers + 25 active employers in wedge pincodes. |
| **Compound** | 5–8 | NGO pilot, ASO A/B, referral amplification, 250 SEO pages, lifecycle journeys. | 30+ jobs/week, median TFA ≤ 12 h. |
| **Pricing reality** | 7–8 | EXP-10 promoted listing live, first press DM. | ≥ 5 % paying employers OR a written learning pivot. |
| **Decision** | 9–10 | Read EXP-10. Decide on city-2 vs deepen city-1. | All 3 expansion gates checked or not. |
| **Execute decision** | 11–12 | Either Vijayawada/Bangalore setup OR week of pure field + product fixes. | One concrete next-90-day plan written. |

Detail in [weekly_execution_plan.md](weekly_execution_plan.md).

---

## 4. What success looks like at day 90

Numerically (city-1, 3 wedge pincodes):
- Active workers: ≥ 800.
- Active employers: ≥ 120.
- Jobs filled per week: ≥ 60.
- Median time-to-first-applicant: ≤ 6 hours.
- Worker D7 retention: ≥ 40 %.
- Employer 30-day repeat-post: ≥ 25 %.
- Play Store rating: ≥ 4.3 with ≥ 200 new reviews in 90 days.
- Cost per activated worker: ≤ ₹40 (founder hours included).
- Cost per activated employer: ≤ ₹150 (founder hours included).
- ≥ 1 RWA partnership signed per pincode (≥ 3 total).
- ≥ 1 NGO partnership signed.
- ≥ 5 % paying employers from EXP-10 OR a clear learning that pricing isn't ready yet.

Qualitatively:
- A founder who can describe the wedge in 30 seconds without slides.
- A weekly dashboard the team trusts.
- 5 testimonial videos that are real, not actors.
- A 1-pager the team can send to investors with all numbers above.

---

## 5. What failure looks like (and the kill-rules)

Trigger an off-cycle re-plan if any of these are true at week 6:
- Active workers in wedge pincodes < 200 → naka & RWA aren't working; rebuild before scaling.
- Worker D7 retention < 25 % → product friction is the issue, not acquisition. Founder week in field, no laptop.
- Employer 30-day repeat-post rate < 10 % → match quality is bad. Audit fill rate by category.
- Founder LinkedIn or content posts have eaten > 15 % of weekly time without producing 1 inbound conversation worth having → kill the content channel for 4 weeks.
- 3+ experiments running, none with a Friday verdict → enforce 3-experiment cap and kill the oldest.

Explicit "stop the company forever" trigger (founder must agree to look at this honestly):
- After 6 months of focused effort in the wedge with no paid spend, if **per-pincode density still isn't compounding** AND **employers won't pay for any feature at any price**, then the marketplace dynamics in this segment may not support a venture business — investigate non-marketplace pivots (e.g. SaaS for staffing agencies) before raising money.

---

## 6. Decision triggers (when to expand what)

| Decision | Trigger |
|---|---|
| Add a 4th wedge pincode | All 3 current pincodes have ≥ 500 active workers AND ≥ 50 active employers. |
| Add a 3rd category (driver) | Cook + maid combined ≥ 60 matches/week AND median TFA ≤ 6 h. |
| Open city-2 | All 3 city-1 expansion gates met (workers, repeat-post, paying employers). |
| Hire a city operator (not founder) | Founder can no longer run a wedge pincode personally without dropping outbound or ASO. |
| Start any paid Meta / Google spend | EXP-10 or EXP-14 has proven ≥ ₹100 ARPU per active employer for 2 consecutive months. |
| Build iOS | Worker installs from non-Android devices > 5 % of total demand-side traffic. |
| Add Hindi UI | Cohort installs from non-Telugu states ≥ 20 % of new installs. |
| Approach press / journalists | ≥ 1 000 jobs filled cumulatively. |
| Raise institutional funding | All day-90 success numbers met, plus a written quarter-2 plan. |

---

## 7. Founder weekly cadence (locked)

- **Mon 9 am — Standup** (15 min). Numbers vs. plan. One number we move this week.
- **Mon–Wed mornings — Field** (2 hrs each, naka or RWA visit). No meetings.
- **Tue/Thu afternoons — Outbound** (1 hr each). 30 messages / day cap.
- **Wed 4 pm — Field readout** (30 min). One concrete change.
- **Thu — Founder LinkedIn post** (30 min). One of templates L1–L5 in [content_plan_90_days.md](content_plan_90_days.md).
- **Fri 4 pm — Friday review** (60 min). KEEP / KILL / SCALE every active experiment. Backlog tidy. Next week locked.
- **Weekend — off** unless a planned NGO event or society on-ground activation.

---

## 8. The 11 documents in this plan

| File | Purpose |
|---|---|
| [master_growth_strategy.md](master_growth_strategy.md) | This document. Read first. |
| [ideal_customer_profiles.md](ideal_customer_profiles.md) | 5 ICP segments, who to ignore, fears table. |
| [channel_prioritization.md](channel_prioritization.md) | Tier S / A / B / C channels, decision tree, this-week list. |
| [outbound_sequences.md](outbound_sequences.md) | 35+ scripts: cold WhatsApp / email / LinkedIn / partnership / objection / reactivation. |
| [landing_page_recommendations.md](landing_page_recommendations.md) | dutype.in rebuild + programmatic SEO template. |
| [app_store_growth_plan.md](app_store_growth_plan.md) | Play Store wedge ASO + Telugu listing + screenshots + reviews strategy. |
| [content_plan_90_days.md](content_plan_90_days.md) | 30 content ideas across founder LinkedIn, Telugu Shorts, WhatsApp shareables. |
| [growth_experiments_backlog.md](growth_experiments_backlog.md) | 15 experiments with hypotheses, success / kill metrics. |
| [weekly_execution_plan.md](weekly_execution_plan.md) | Week-by-week 12-week plan. |
| [kpi_dashboard_spec.md](kpi_dashboard_spec.md) | North star, funnel, leading / lagging indicators, dashboard layout. |
| [what_not_to_do.md](what_not_to_do.md) | 20 explicit anti-recommendations. |

---

## 9. The single sentence

> **The next 90 days are about proving that one founder, one engineer, ₹X K of pamphlets, and Telugu localisation can produce 60 weekly job fills in three Hyderabad pincodes — without any paid acquisition. Everything that doesn't serve that sentence gets cut.**
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/outbound-sequences">Outbound Sequences</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Outbound Sequences (Ready-to-Send Scripts)

> Channel rules first. Skip these and you will get banned, ghosted, or look like a scammer.

## Anti-cringe / anti-spam rules
1. **No "Hope you are doing well"** opener. Ever.
2. **No emoji walls.** One emoji max in any message.
3. **No fake personalization** ("loved your work in space X" when you didn't read X). Real specificity or none.
4. **No PDFs in cold WhatsApp.** Link only. (You'll get reported.)
5. **No bulk WhatsApp without warm-up.** New number = 20 messages/day max for first week, 50/day week 2, 100/day week 3, with 3-min gaps.
6. **One ask per message.** Never two CTAs.
7. **Telugu / Hindi for ICP-1 and ICP-2.** English for ICP-4 SMB owner ↔ test which converts.
8. **No "let me know if interested."** Always a calendar / Play Store link / yes-no question.
9. **Reply within 2 hours during 9 am–9 pm IST.** Outbound dies if first reply is delayed.
10. **Mark sent / replied / installed / activated in a sheet.** Untracked outbound = hobby, not channel.

---

# Section A — 5 EMAIL SEQUENCES

## Email Sequence A1 — Cold employer (PG / hostel / cloud kitchen owner) in wedge city

**Audience:** owners of small F&B / accommodation businesses with 5–25 staff, scraped from Justdial + Google Maps in target pincodes.

**Channel:** business email from \`founder@dutype.in\` (NOT a marketing tool — too easy to land in spam).

### A1-Email-1 (Day 0)
> **Subject:** 14 cooks available within 3 km of [BUSINESS NAME]
>
> Hi [first name],
>
> I run DutyPe, a local-only hiring app for blue-collar staff in Hyderabad.
>
> We currently have 14 cooks and 22 helpers in [Pincode] who marked themselves available this week. Most are women, ages 25–40, Telugu/Hindi speaking, ₹450–600/day expected pay.
>
> If you are short-staffed for [festival / weekend / next month], you can post a job free in 2 minutes and get applications today: https://dutype.in/post-job?utm=cold-email-pg
>
> Worth 2 minutes?
>
> – [Founder name], [phone]

### A1-Email-2 (Day 3, only if no reply)
> **Subject:** Re: 14 cooks available within 3 km of [BUSINESS NAME]
>
> [first name], quick nudge — last week 9 PG owners in [area] hired through DutyPe with median 6 hours from posting to first applicant. No commission, no agency fees.
>
> Want me to set up your first post? Reply YES and I'll do it for you.

### A1-Email-3 (Day 7, last touch)
> **Subject:** Closing your file
>
> Last note. If hiring isn't a problem this month, all good — I'll close your file and stop emailing.
>
> If it is, here's a 30-second video of how it works: [Loom link]. App: https://play.google.com/store/apps/details?id=com.dutype.app
>
> Either way, no more emails from me.

**Stop conditions:** any reply (positive or negative) → human handoff. 3 emails sent → mark "no_response" → re-attempt in 90 days.

---

## Email Sequence A2 — Apartment RWA / Facility Manager (B2B-like)

### A2-Email-1
> **Subject:** Free service for [Society Name] residents — verified maids/cooks in [Pincode]
>
> Hi [name],
>
> I'm [founder] from DutyPe. We help residents in apartment complexes find verified maids, cooks, drivers without paying agency commission.
>
> For [Society Name] specifically: we already have 47 women workers who live within 2 km. They are phone-verified and most have prior society experience.
>
> What we'd like: 5 minutes on your community WhatsApp group to share one message + a Telugu pamphlet on the notice board. We will pay [Society Welfare Fund / RWA account] **₹500 per 10 residents who use us in the first month**.
>
> Open to a 10-minute call this week?

### A2-Email-2 (Day 4)
> **Subject:** Re: free service for [Society Name] residents
>
> Quick question — would it help if I came on-site for one Saturday morning and did a desk in the lobby? Residents see the workers in person, sign up if interested. Zero cost to RWA.

### A2-Email-3 (Day 10)
> **Subject:** Closing your file
>
> [Same as A1-3 pattern.]

---

## Email Sequence A3 — NGO / SHG partnership

### A3-Email-1
> **Subject:** Worker-livelihood pilot in Hyderabad — partnership idea
>
> Dear [name],
>
> I lead DutyPe, a hyper-local hiring app focused on connecting blue-collar workers in Hyderabad directly to households and small employers — no agencies, no commission cuts, Telugu-first interface.
>
> I'd like to propose a 90-day pilot with [NGO Name]:
> - We provide free onboarding + a ₹100 sign-up incentive for 100 women in your network.
> - You provide trust + introduction.
> - Joint reporting on # placed, average wages, retention.
>
> Outcome we're targeting: 30+ women earning ≥ ₹8 000/month within 60 days.
>
> 30-minute call this week?

(Two follow-ups, same pattern as above.)

---

## Email Sequence A4 — Reactivation: lapsed employer who posted once

### A4-Email-1 (sent 30 days after their last post if no new post)
> **Subject:** Did your last post on DutyPe work out?
>
> Hi [name],
>
> You posted [Job Title] on DutyPe 30 days back. Quick check-in — did you fill the role?
>
> - **Yes →** great, what made the difference? (1-line reply helps us improve.)
> - **No →** I want to know why. I'll personally help if you re-post; founder hand-on-keyboard.
>
> [Founder name]

### A4-Email-2 (Day 5)
> **Subject:** Want me to post for you?
>
> If you tell me the job and salary, I'll write the post myself in 5 minutes and you just review. Reply with the role.

### A4-Email-3 (Day 12)
> **Subject:** Honest ask
>
> If DutyPe didn't work for you, what would have to change? One sentence is enough — I read every reply.

---

## Email Sequence A5 — Investor / partner / press warm intro response template (NOT cold)

> Subject: Quick read — DutyPe traction, what we're testing this quarter
>
> Hi [name],
>
> Thanks for the intro from [referrer]. Two minutes:
>
> - **What:** Hyper-local blue-collar hiring app in Hyderabad. Worker side is the focus this quarter.
> - **Why now:** [insert one factual stat e.g. "12 % WoW worker activation in Gachibowli pincode last 4 weeks"].
> - **What we're not:** Apna v2. We're a 3-pincode wedge with QR work-verification, scam-keyword firewall, Telugu-first.
> - **Ask:** [specific ask — intro to MyGate, journalist at Eenadu, etc.]
>
> Open to a 15-min call. Calendly: [link].

---

# Section B — 5 WHATSAPP OUTREACH FLOWS

## WhatsApp Flow B1 — Worker pamphlet QR install (the workhorse)

> Founder/promoter at auto stand, 7 am.
>
> **In Telugu (script):**
> "Hello aunty / akka, mee meeting Hyderabad lo evarikina maid / cook / helper job kavalantey, ee app lo free ga apply cheyi. Agency commission ledu. ₹250 first earn cheyandi — sign-up ₹25, oka friend ni refer cheste ₹25, 5 friends ki ₹100 bonus. QR scan cheyandi."
>
> Hand them a pamphlet with QR → Play Store link with \`utm_source=naka&utm_campaign=hyd_cook_maid_apr26\`.
>
> **Same-day WhatsApp follow-up message** (auto-sent if they share number):
> > Namaste [Name] akka, today metro stop near [area] lo kalisinaru. DutyPe app install link: [link]. Help kavalantey ee number ki call cheyandi: [support number]. — DutyPe team

**Anti-spam rule:** never message a worker who didn't share their number willingly.

---

## WhatsApp Flow B2 — Employer (small business) cold outreach

**Setup:** business WhatsApp account warmed up over 3 weeks. Number printed on pamphlets / Justdial profile / website. Outbound capped at 50/day.

### B2-msg-1 (Day 0, ~10 am or 4 pm)
> Hi, this is [Founder name], founder of DutyPe (Play Store app for hyper-local hiring in Hyderabad).
>
> Saw your [restaurant / PG / clinic] on Google Maps in [area]. We have 14 cooks + 22 helpers available within 3 km of your place this week.
>
> If you are short on staff, you can post a job free in 2 mins. Want me to share a 30-sec video of how it works?

### B2-msg-2 (Day 2 if no reply)
> Quick nudge — here's the video: [link, 30 sec, real PG owner saying it worked].
>
> Free to try. Reply YES and I'll send the link.

### B2-msg-3 (Day 6, last)
> Last note from me. If hiring is sorted, all good. If you ever need staff in the next 3 months, save my number — DutyPe.

**Stop conditions:** "STOP", "DO NOT MESSAGE", any complaint → mark do_not_contact in sheet, never message again.

---

## WhatsApp Flow B3 — RWA admin partnership

### B3-msg-1
> Namaste [Name] sir/madam, I'm [Founder] from DutyPe — Hyderabad-based hyper-local hiring app.
>
> Quick context: we already have 47 verified women workers (cooks/maids) within 2 km of [Society Name]. Many residents in your society spend ₹2–5K agency commission for the same hires.
>
> Idea: we donate **₹500 to RWA welfare fund per 10 residents who hire through DutyPe in the first month**. We provide: (1) one Telugu+English message for your residents group (you decide if it's appropriate), (2) a poster for the lift / notice board.
>
> No cost to society. 10-minute call this week?

### B3-msg-2 (Day 3)
> Just to make it easier — here is the message I would propose, you can edit anything: [paste 4-line message, with Play Store link + RWA's name in it].

### B3-msg-3 (Day 7, last)
> No worries if not the right fit. Saved your number; I'll share once we have 100+ workers in your pincode in case it becomes useful later.

---

## WhatsApp Flow B4 — Worker referral activation (already-installed but inactive)

Triggered by Cloud Function 7 days after install if no application made.

> [First name], DutyPe app lo profile complete cheyandi (80% only) → friend ni refer cheste ₹25 mil-thundi. Mee area lo ee week 9 jobs unnayi. Profile complete link: [deep link to profile completion screen].

(One-shot only. Re-trigger 30 days later if still inactive.)

---

## WhatsApp Flow B5 — Employer post-fill follow-up (NPS + repeat-post + referral)

Triggered 5 days after employer marks job "filled" via QR.

### B5-msg-1
> Hi [name], hope [Worker first name] is working out for you. Two quick asks:
>
> 1. Reply with a number 1–10 — how likely are you to recommend DutyPe to another household / business in [area]?
> 2. If 8+, will you forward this 1-line message to your society group? "[paste message]" — you'll get a free promoted post next time.

### B5-msg-2 (5 days after, if no reply)
> No pressure on the rating — but if you ever need another hire, post link is here: [deep link]. We're also opening up a verified-employer badge soon, free for our first 100 active employers — let me know if interested.

---

# Section C — 5 LINKEDIN DM FLOWS

> LinkedIn is for **ICP-4 (small business owners)** and **ICP-5 (RWA / NGO / partner)** only. Workers are not on LinkedIn. Skip the worker side entirely.

## LinkedIn Flow C1 — Apartment community manager (MyGate, NoBroker, Apartment Adda employee in Hyderabad cluster)

### C1-msg-1 (connection request note, ≤ 300 chars)
> Hi [first name] — running DutyPe, a hyper-local hiring app for blue-collar workers in Hyderabad. Would love your perspective on what residents in your communities ask about hiring maids/cooks. Quick 10-min chat?

### C1-msg-2 (after acceptance, Day 0)
> Thanks for connecting! Two questions, no agenda:
> 1. In the residents' WhatsApp groups you see, how often does someone ask "any maid / cook available?"
> 2. If a vetted, hyper-local app existed *as a complement* (not competing with MyGate), would it be of interest?
>
> Happy to share what we're seeing in [pincode] — 47 verified workers within 2 km of typical complex.

### C1-msg-3 (Day 5, last)
> No worries if busy — saved your contact. We're piloting with 5 societies this quarter; if you'd like to be looped in on results, say "yes" and I'll DM in 30 days.

---

## LinkedIn Flow C2 — Founder / GM of a small Hyderabad chain (5–15 outlet salons / clinics / cafes)

### C2-msg-1 (connection note)
> Hi [name] — saw [chain name] has [X] outlets in Hyderabad. Curious how you handle staff churn at the outlet level. I run DutyPe (hyper-local hiring app); we're seeing strong supply density in [pincode]. 10-min compare notes?

### C2-msg-2 (after accept)
> Specifically: how long does an outlet usually wait between staff quitting and hiring a replacement? Industry average I see is 9–14 days. Curious to know your benchmark.
>
> If useful, here's what we did for a 4-outlet PG chain in Madhapur — median fill time 22 hours: [1-page case study link].

(Follow up once at Day 6.)

---

## LinkedIn Flow C3 — Government / municipal worker-livelihood program officer

### C3-msg-1 (connection note)
> Hi [name], saw your work at [NULM / Skill India / DDU-GKY]. Building DutyPe — Telugu-first hyper-local job app for blue-collar workers. Would value 15 min on what's blocking placement in Hyderabad cohorts.

### C3-msg-2 (after accept)
> Thanks! One concrete proposal: pilot with one cohort of 50 women — we onboard them for free, give a ₹100 sign-up incentive, share placement data with you monthly. Zero cost to your program.

---

## LinkedIn Flow C4 — HR head of a 50–200 staff Hyderabad SMB (manufacturing / logistics warehouse)

### C4-msg-1 (connection note)
> Hi [name] — quick note, I run DutyPe, a hyper-local hiring app focused on Hyderabad blue-collar roles (helpers, packers, security, drivers). We have 200+ workers active near [area]. Curious if your hiring funnel is feeling tight on these roles.

### C4-msg-2 (after accept)
> Direct question: what's your current cost per hire for ground-floor staff? Industry says ₹2–8K all-in. We're seeing ₹0–500 on DutyPe (mostly time, no agency fees). Worth a 15-min compare?

---

## LinkedIn Flow C5 — Journalist covering tech/labour/livelihood beat at Telugu/English publication

### C5-msg-1 (connection note)
> Hi [name] — read your piece on [specific article]. Building DutyPe (Telugu-first hiring app for blue-collar workers in Hyderabad). Happy to share what we're seeing in pincode-level worker supply trends if useful for future stories.

### C5-msg-2 (after accept)
> No PR pitch. Sharing one chart: in [pincode], cook/maid demand is up 38% MoM but agency placement fees average ₹3 200 (we surveyed 80 households). Useful angle?

---

# Section D — 3 FOUNDER INTRO SCRIPTS (in-person / phone)

## D1 — At an auto-stand, to a worker (30 sec)
> "Akka / chinnamma — meeku Hyderabad lo cook / maid / helper job kavalantey, free app. Agency commission ledu. Naa pere [name], DutyPe team. Inka, oka friend ni refer cheste ₹25 mistundi. Ee QR scan cheyandi, app install ayipothundi."
>
> *(Hand pamphlet. Don't push. Smile. Move on.)*

## D2 — At a society lobby desk, to a resident (60 sec)
> "Hi, I'm [name] from DutyPe — a Hyderabad app to find verified maids and cooks. Most residents here spend ₹2 000–₹5 000 in agency commission. We don't take any commission. We currently have [X] women workers within 2 km of this complex. If you ever need a replacement, scan this QR, post for free. The first call is on the worker, not on you. Any questions?"

## D3 — Phone call to a small business owner (90 sec)
> "Hi [name], [Founder] here, founder of DutyPe — the Hyderabad app for blue-collar hiring. I won't pitch. One question: in the last 6 months, how many days has your [PG / cafe / clinic] been short-staffed because someone quit? … OK. We can usually fill in 24 hours, free to post, no commission. If you want, I'll set up your account in 5 minutes, no obligation. Worth it?"

---

# Section E — 3 PARTNERSHIP OUTREACH SCRIPTS

## E1 — Apartment RWA admin (in person, 2 min)
> "Sir / madam, I run DutyPe. We solve one specific problem — residents asking 'any maid available' in your group and getting no replies. We donate ₹500 to RWA welfare fund for every 10 residents who hire through us. No cost to society. We provide one message + one poster, you stay in control of your group. Can I show you how it works in 60 seconds on my phone?"

## E2 — NGO / SHG director (15-min meeting)
> "Three numbers for your time: (1) we have 47 women workers in [pincode] who joined DutyPe in 30 days, average earning ₹6 800 in first month. (2) Agency commission they would have paid: ₹4 200 average. (3) We are willing to put up ₹100 per woman from your cohort as a sign-up incentive, no strings. What we want from you: an introduction at the next [meeting / camp]. May I share a one-pager?"

## E3 — Local labour contractor (in his office)
> "Bhai, mein dushman nahi hoon. App tere kaam ko kam nahi karega. Ulta — tere paas jab ek extra worker bachta hai jise tu place nahi kar paaya, woh DutyPe pe daal de. Har worker placement pe humse ₹100 milta hai. Tere existing kaam mein zero loss. App mein tera naam '[contractor name]' likha rahega, har worker ke profile mein. Ek try karta hai?"

---

# Section F — 3 LOCAL BUSINESS OUTREACH SCRIPTS

## F1 — Tiffin centre / cloud kitchen owner (WhatsApp B2 + walk-in)
> "Saw your tiffin centre on [road]. Quick offer: if you need a kitchen helper this week, I'll post the job for you free, and you'll get applications today (we have 22 helpers within 3 km). If it doesn't work, I'll personally apologise and never message again. Fair?"

## F2 — PG / hostel owner (in person)
> "Sir, [Founder] from DutyPe. PG owners in Madhapur are losing 4–7 days every staff change. We close the gap to 1 day. ₹0 to try. I'll set up your account in 5 minutes right now if you have a phone with you."

## F3 — Salon / clinic chain operator (LinkedIn + email)
> "Hi [name], your [chain] has [X] outlets in Hyderabad. Salon receptionists and clinic helpers churn ~25%/quarter. We have ready supply in [pincodes] and can pre-screen applicants by area + experience. One free hire on us. Worth 10 minutes?"

---

# Section G — 3 REACTIVATION SEQUENCES

## G1 — Worker installed but never completed profile (Day 7)

> **Notification (push, Telugu):** "Akka, mee profile inka 30% only. 10 minute lo complete cheyandi → ₹50 bonus + first job apply. [Open profile]"
>
> **WhatsApp (Day 9 if still inactive):** "Hi [name], DutyPe team. App install thakshanam profile complete cheste ₹50 mistundi. Need help? Reply YES, mein call cheystha."

## G2 — Employer posted 1 job, never returned (Day 30)
- See A4 above (Email + WhatsApp chain).

## G3 — Worker completed 1 job, no second application in 21 days
> **Push:** "Mee area lo ee week [N] new jobs unnayi. Mee skill ki match ayyenavi 3. Apply now → [deep link]"
>
> **WhatsApp Day 23:** "Hi [name], last job [employer area] lo bagunda? Reply 1-10 number. Inka, mee area lo job lekapothey, eppudu vasthayi notify cheyatam — opt in cheyali?"

---

# Section H — 3 NO-RESPONSE FOLLOW-UP PATTERNS

**Pattern H1 — The honest closer**
> Last note. No reply = no problem; I'll close your file and stop messaging. If [problem] ever becomes urgent, save my number: [name], DutyPe.

**Pattern H2 — The pattern interrupt**
> [Name], one direct question — is the problem (1) not the right time, (2) not the right product, or (3) the message got lost? A single number reply is enough.

**Pattern H3 — The proof-drop**
> Not a follow-up — just sharing because it's relevant. Last week a [PG owner / mom / NGO director] in [pincode/area] hired through DutyPe in [X hours]. 1-min video here: [link]. No reply needed.

---

# Section I — 3 OBJECTION-HANDLING SCRIPTS

## I1 — "Will I get paid? Past apps cheated us."
> Genuinely fair question. Three things: (1) DutyPe never asks for any money from workers — ever. (2) Salary is paid by the employer, not the app. (3) We use a QR code: when you start work, you scan; when you finish, you scan. If the employer doesn't pay, you have proof, and we follow up on your behalf. Inka, mee phone OTP verified. Naa number [phone].

## I2 — "I posted on Apna / Quikr already, why DutyPe?"
> Good — that means you already see the problem. Three differences: (1) we filter by 1–10 km radius, not by city. The applicants you see actually live near you. (2) We use phone OTP + scam-keyword filter at post-time, so you don't get the "earn ₹50,000/day from home" replies. (3) Zero commission. If we don't help you fill it in 48 hours, I'll personally apologize and refund any time you spent.

## I3 — "We don't have budget for marketing tools / job platforms."
> Zero budget needed. The post is free. The first 5 hires are free. If we ever charge in the future, you'll be grandfathered in for 12 months. Worth 2 minutes to test?

---

## Operational mechanics

- **Track every send in \`growth/campaigns/<channel>_<date>.csv\`** with: prospect, channel, sent_at, opened, replied, installed, activated, posted_job, filled_job, do_not_contact.
- **Founder reviews the sheet every Friday 4 pm.** Kills any sequence with < 5 % reply rate after 100 sends.
- **Reply within 2 hours.** Replies > 4 hours = 50 % conversion drop.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/weekly-execution-plan">Weekly Execution Plan</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – Weekly Execution Plan (12 weeks)

> One operator, one founder, ~12 hours / week founder time on growth + 1 engineer for limited tickets. If your team is smaller, cut the *width* of each week, not the *depth*.

> Every Friday is **review + cull day**. Don't add a new bet without killing an old one.

---

## Cadence

- **Mon 9 am — Standup (15 min):** Last week's numbers vs. plan. This week's one number we move.
- **Wed 4 pm — Field readout (30 min):** What did the offline team / founder see in nakas / RWAs / outbound? One concrete change.
- **Fri 4 pm — Friday review (60 min):** Every active experiment → KEEP / KILL / SCALE. Backlog tidy. Next-week plan locked.

---

## Week 0 — Setup (do before Week 1)

- [ ] Pick 1 city + 3 pincodes (founder, day 0).
- [ ] Pick 2 wedge categories (cook + maid recommended).
- [ ] Fix the referral persistence bug (engineering).
- [ ] Open business WhatsApp number; warm up (send 5 / day to friends for 7 days).
- [ ] Create \`growth/campaigns/{outbound,reactivation,content,partnerships}.csv\` tracking sheets.
- [ ] Print 500 Telugu pamphlets (₹1 200).
- [ ] Pull JustDial + Google Maps prospect lists for the 3 pincodes (200 PG owners, 100 cloud kitchens, 80 salons, 40 tiffin centres).
- [ ] Build 30-name RWA admin list using MyGate + Facebook.
- [ ] Set up Firebase Dynamic Links for UTM-attributed install tracking.
- [ ] Audit current Play Store listing — screenshot baseline.

---

## Week 1 — Foundation

| Day | Owner | Task |
|---|---|---|
| Mon | Founder | Naka day 1: 50 conversations at Madhapur. |
| Tue | Engineer | Cloud Function for \`/api/stats?pincode=\` (active workers, jobs last 7d). |
| Tue | Founder | RWA outreach: 10 admins messaged via WhatsApp. |
| Wed | Founder | Naka day 2: 50 conversations at Gachibowli. |
| Wed | Designer | New Play Store screenshots × 8 (English + Telugu). |
| Thu | Founder | Employer WhatsApp outbound: 30 PG owners messaged. |
| Thu | Engineer | Play Store listing rewrite published (English + Telugu). |
| Fri | Founder | Field readout. Friday review. Cull dead bets. |

**Week 1 numbers we move:**
- 100 naka conversations.
- 30 employer messages.
- 10 RWA messages.
- New ASO listing live.

**Week 1 kill metrics:**
- < 30 worker installs from naka → debug pamphlet copy.
- 0 RWA replies → re-write opening message.

---

## Week 2 — Iterate, double down on what stuck

| Day | Owner | Task |
|---|---|---|
| Mon | Founder | Naka day 3: best-performing pincode from week 1. |
| Mon | Engineer | Reactivation Cloud Function (worker D7 incomplete-profile). |
| Tue | Founder | Film 3 raw Telugu testimonials at field. |
| Wed | Founder | Naka day 4. |
| Wed | Founder | RWA follow-ups: meet the 3 most engaged admins in person. |
| Thu | Founder | Employer outreach: 30 more PG owners + 20 cloud kitchens. |
| Fri | Founder | Friday review. |

---

## Week 3 — Distribution test

- [ ] Publish 3 Telugu Shorts on YouTube + Instagram.
- [ ] First founder LinkedIn post (use template L3 — naka field note).
- [ ] First 50 SEO programmatic pages live (\`/hyderabad/<pincode>/cook-jobs\`, \`…/maid-jobs\`).
- [ ] Start signing partnership MoUs with first 2 RWAs (donation language).
- [ ] Continue outbound: 50 employers / week.
- [ ] First Quora sprint: answer 5 questions in long form.

---

## Week 4 — First scale checkpoint

**End-of-week-4 numbers (target):**
- Active workers in wedge pincodes: **≥ 200**.
- Active employers in wedge pincodes: **≥ 25**.
- Jobs posted past 7 days: **≥ 30**.
- Median time-to-first-applicant: **≤ 12 hrs**.
- Play Store listing-visitor → install rate: **≥ +25 %** vs week 0.

**If hitting:**
- Open second pincode for naka activation.
- Add 2 more RWA partnerships.
- Open EXP-08 (NGO pilot).

**If missing:**
- Stop adding new motions. Diagnose:
  - Low installs from naka → message issue, location issue, or incentive issue?
  - Low employer signups → pincode supply too thin? Outbound message wrong?
  - Long first-applicant time → mismatch on category vs supply?
- Spend week 5 fixing, not expanding.

---

## Weeks 5–6 — Compound

- 4 more Telugu Shorts.
- 200 more SEO programmatic pages.
- 5 more RWA partnerships.
- 100 employer outbound messages.
- Start NGO pilot (EXP-08): 50-woman cohort.
- First A/B test on Play Store screenshots (Experiments tab).

---

## Weeks 7–8 — Pricing reality check

- [ ] Engineering: Razorpay integration.
- [ ] Launch EXP-10 (₹99 / ₹199 / ₹499 promoted listing).
- [ ] First press DM to 1 Telugu / English journalist.
- [ ] Add lifecycle journey: D7 incomplete-profile, D30 inactive worker, D5 post-fill NPS.
- [ ] Add referral leaderboard.

---

## Weeks 9–10 — Decide on city-2

- Read EXP-10 results.
- Check 3 gates:
  1. Active workers in city-1 wedge ≥ 500.
  2. Employer repeat-post rate ≥ 20 %.
  3. ≥ 5 % of employers paying *something* in EXP-10.

If all 3 → start city-2 setup (week 11).
If any miss → spend weeks 9–10 fixing the gap.

---

## Weeks 11–12 — Either city-2 or deepen city-1

**Path A (city-1 strong):**
- Week 11: Pick city-2, repeat Week 0 setup there.
- Week 12: Naka day 1 + RWA outreach in city-2.

**Path B (city-1 weak):**
- Week 11: Founder-only week. Spend 5 days in field, no laptop work.
- Week 12: Re-write top 3 friction points in product based on field notes.

---

## Weekly hourly budget (founder)

| Activity | Hours/week |
|---|---|
| Field (naka, RWA, in-person) | 6 |
| Outbound writing & sending | 3 |
| Friday review + planning | 2 |
| LinkedIn / X post + journalist DM | 1 |
| **Total** | **12** |

If less time available → cut LinkedIn first, then outbound, never field.

---

## Weekly hourly budget (engineer, ~6 hrs/week)

| Activity | Hours/week |
|---|---|
| Cloud Functions for stats / reactivation / SEO pages | 3 |
| ASO + screenshot tweaks | 1 |
| Razorpay / paid features (later weeks) | 2 |

---

## Stop-doing list (active rules for the next 12 weeks)

- **No paid Meta / Google ads.** Period.
- **No new feature work** unless it removes a measured friction in the funnel.
- **No new city** before week 11.
- **No new category** beyond cook + maid for first 8 weeks (then driver test).
- **No press push** before 1 000 jobs filled.
- **No conferences / events** for the founder.
- **No "rebrand."** Spend that time in field.

---

## Weekly dashboard (one screen, one tab)

| Metric | Last week | This week | Target | Trend |
|---|---|---|---|---|
| Active workers (3 wedge pincodes) | — | — | +50/wk | — |
| Active employers (3 wedge pincodes) | — | — | +10/wk | — |
| Jobs posted last 7d | — | — | ≥ 30 by week 4 | — |
| Median time-to-first-applicant | — | — | ≤ 6 hrs by week 8 | — |
| Worker D7 retention | — | — | ≥ 40 % | — |
| Employer repeat-post 30d | — | — | ≥ 20 % by week 8 | — |
| Play Store install rate | — | — | +30 % by week 4 | — |
| Refer & Earn payouts | — | — | up WoW | — |
| Total monthly burn | — | — | ≤ planned | — |

(Spec for this dashboard in \`kpi_dashboard_spec.md\`.)
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/outputs/what-not-to-do">What Not To Do</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe – What NOT To Do

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
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📘</span>
            <span>Playbooks</span>
            <small>11 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbooks

> Repeatable, written, hand-able-to-an-operator processes for every motion in the 90-day plan. If a playbook isn't written, it can't be repeated and can't be delegated.

## Files

- [naka.md](naka.md) — naka pamphlet drop + on-the-spot install assistance.
- [rwa.md](rwa.md) — RWA admin pitch + onboarding + monthly engagement.
- [outbound_whatsapp.md](outbound_whatsapp.md) — cold WhatsApp outbound (employer + RWA + NGO).
- [employer_onboarding.md](employer_onboarding.md) — first-post hand-holding for SMBs.
- [worker_onboarding.md](worker_onboarding.md) — same-day install → activation field flow.
- [ngo_pilot.md](ngo_pilot.md) — 50-woman cohort pilot operating manual.
- [aso_release.md](aso_release.md) — Play Store listing update + screenshot release process.
- [friday_review.md](friday_review.md) — weekly review meeting agenda + decision rules.
- [press_pitch.md](press_pitch.md) — when and how to pitch a journalist (later phase).
- [partnership_legal.md](partnership_legal.md) — what an RWA / NGO MoU needs to contain.

## Operating principles for every playbook

1. **One person, one playbook, one outcome.** No "team" tasks.
2. **Time-boxed.** Every playbook says "this takes N hours" — if it takes more, the playbook is broken.
3. **Stop-rule.** Every playbook has an explicit "abandon if X" clause.
4. **Logged.** Every execution drops a row in the relevant \`/growth/campaigns/*.csv\`.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/aso-release">Aso Release</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Play Store Listing Release

> Goal: ship an ASO update with zero conversion regression.

## Pre-flight checklist

- [ ] Title within 30 chars.
- [ ] Short description within 80 chars.
- [ ] Long description ≤ 4 000 chars.
- [ ] All 8 screenshots exported at correct resolution.
- [ ] Telugu listing complete (do not publish English-only update if Telugu localised listing exists — Play will warn).
- [ ] Hindi listing reviewed (text only; no UI).
- [ ] Feature graphic 1024×500 ready.
- [ ] Privacy policy URL alive (https://dutype.in/privacy).
- [ ] Data safety form re-confirmed.
- [ ] App version number bumped if any APK / AAB change is co-released.

## Pre-publish A/B test setup

- If only listing copy changing → use Play Console **Store Listing Experiments** (no APK release needed).
- If feature graphic / icon → run as separate experiment.
- One concurrent experiment max.

## Publish

- Publish during India morning hours (9–11 am) — fastest review turnaround.
- Wait 6–24 hours for Play review approval. **Do not refresh other listings during review.**

## Post-publish (first 7 days)

- Day 1: snapshot conversion baseline (visitor→install rate).
- Day 3: check Crashlytics for spike (sometimes a listing-only update bumps installs and exposes a latent crash).
- Day 7: read experiment significance. Decide rollout or revert.

## Rollout

- If experiment winner is significant (≥ 95 % confidence and ≥ 5 % uplift) → roll to 100 %.
- If inconclusive → keep collecting up to 28 days, then decide.
- If losing → revert.

## Rollback procedure

- Play Console → Store Listing Experiments → Revert.
- Update reverts within 30–60 min.
- Document outcome in \`growth/campaigns/experiments.csv\`.

## Cadence

- Maximum 1 listing change per 14 days (Play penalises high-frequency changes).
- Plan ASO sprints quarterly.
- Localised listings reviewed monthly for stale numbers (e.g., "active in 5 cities" should match reality).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/employer-onboarding">Employer Onboarding</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Employer First-Post Hand-Holding

> Goal: every newly signed employer posts their first job within 24 hours of account creation, with high-quality fields, and gets at least one application within 24 hours of the post.

## Trigger

- New employer account created (Cloud Function fires).
- WhatsApp template message (B5 in [outbound_sequences.md](../outputs/outbound_sequences.md)) sent within 30 minutes.

## Step 1 — Within 30 min of signup
- WhatsApp: "Welcome to DutyPe. I'm <name>, founder. I'll personally make sure you fill your first hire within 48 hours. Reply with the role you're hiring for."
- (For first 100 employers in city-1, this WhatsApp is from the founder personally — NOT a Cloud Function template. After 100, hand off to ops.)

## Step 2 — Within 24 h
- If they haven't posted: send a **prefilled job link** (deep link with category + pay-type + radius pre-set based on their account profile pincode).
- Offer: "I can take 2 minutes on a call to walk through it." (Most won't take it; 10 % will, and they convert at 3×.)

## Step 3 — Once posted
- Audit the post within 1 hour. If structured fields are weak (vague title, missing salary, missing timing), WhatsApp the employer with a one-line suggestion.
- Push the job to the right worker cohort (Cloud Function — currently in \`functions/src/notifications.ts\`). Verify it fired.

## Step 4 — 24 h after post
- If no applications: WhatsApp employer with one of:
  - "Salary range nelaki ₹X is below our area median; raise to ₹Y to triple applications."
  - "Add a Telugu line in description; cooks read it 3× more."
  - "Your radius is set to 1 km; expand to 5 km for this category."
- If applications received but employer hasn't replied: WhatsApp nudge — "3 applications waiting, 22 hours since first one."

## Step 5 — Worker selected
- Walk the employer through QR work-start (this is the trust artifact; it's free leverage).
- WhatsApp confirmation when the worker QR-ins on day 1.

## Step 6 — 7 days post-fill
- WhatsApp: "Is <worker name> still showing up? 1-tap reply: yes / not great / left. Anything I can fix?"
- This single question saves you weeks of wondering why repeat-post rate is low.

## Step 7 — 30 days post-fill (repeat-post nudge)
- "Need to hire again? Free to post. Refer another household — both get ₹50."

## Stop rules

- If an employer ghosts after step 4 with no reply for 7 days → stop nudging. Re-engage at D30 only.
- If an employer posts a clearly fraudulent job (already auto-flagged by your fraud-score system, score ≥ 70) → confirm reject and *do not* re-engage.

## Quality bar

- ≥ 70 % of new employers post within 24 hours.
- ≥ 50 % of first posts get an application within 24 hours.
- ≥ 30 % of first posts result in a fill within 7 days.
- ≥ 25 % of first-fills lead to a repeat post within 30 days.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/friday-review">Friday Review</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Friday Review

> 60 minutes, every Friday at 4 pm IST. Founder + engineer + ops (if any). No exceptions.

## Pre-meeting (founder, 30 min before)

- Pull all numbers from \`kpi_dashboard_spec.md\` dashboard.
- Update \`growth/campaigns/experiments.csv\` with the week's reads.
- Have one-pager open with the 9 metrics from the dashboard.

## Agenda (60 min)

### Block 1 — Numbers (15 min)
- Walk the dashboard, top to bottom.
- Founder calls out the **one** number they're most worried about.
- Engineer calls out one technical leading indicator (crash rate, uptime, queue lag).
- Ops (if any) calls out one field signal (worker quotes, employer complaints).

### Block 2 — Experiments (20 min)
- Each active experiment gets a 3-minute slot:
  - Owner reads success metric vs kill metric.
  - Decision: **KEEP / KILL / SCALE**.
  - One sentence on next action.
- Maximum 3 active experiments. If 4, the oldest underperformer is killed by default.

### Block 3 — Field readouts (10 min)
- Founder reads top 3 verbatim quotes from the week (workers + employers + partners).
- One concrete product / message / channel change derived from quotes.

### Block 4 — Anti-pattern check (5 min)
- Founder asks aloud (from \`what_not_to_do.md\` ¶ "Anti-pattern detector"):
  - Did we add a feature to avoid a hard sales conversation?
  - Did we open a new channel to avoid fixing an old one?
  - Did we ship a "rebrand" because we couldn't ship a metric?
  - Did we send a press email because the cohort retention chart looked bad?
- If any "yes" → fix that thing next week. No new bets.

### Block 5 — Next week locked (10 min)
- Founder writes the **one number** the team will move next week.
- Each owner writes their week's top 3 outputs in the shared plan.
- Meeting ends with a single Slack/WhatsApp message: "<DATE> review. Top number: X. Owners' top 3 each below."

## Discipline rules

- No slides. The dashboard is the slide.
- No "let's discuss next week." Every issue gets a decision in the meeting.
- No new initiatives proposed without naming what gets dropped.
- No retroactive scope changes to running experiments.

## Output artifact

- One Markdown note dropped in \`growth/playbooks/reviews/YYYY-MM-DD.md\` summarising decisions made + the week's "one number."
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/naka">Naka</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Naka Pamphlet Drop

> Goal: convert a 2-hour morning at one labour stand into ≥ 15 same-day worker installs.

## Pre-requisites

- 200 Telugu pamphlets (see \`growth/assets/pamphlet_telugu.md\`).
- 1 weather-proof banner (laminated A2, ₹400).
- 1 promoter (founder for first 8 sessions; field ops thereafter).
- Phone with hotspot enabled (some workers don't have data).
- ₹500 in ₹50/₹100 notes for the ₹250 referral incentive payouts (issued only when the worker completes profile and applies).
- Field log sheet (paper) + pen.
- Scheduling: pick days **Mon / Tue / Wed**. Skip Thu (worker pay-day, sparse). Skip festivals.

## On-site flow (per worker, ~3 minutes)

1. **Hand pamphlet, smile, in Telugu:** "Mee ki pani kavalanaa? Ee phone lo install cheste mee area lo kuda jobs vasthayi. Free ee. Commission ledhu."
2. **Open Play Store on their phone.** Use the QR on the pamphlet. If they don't have data → tether via your hotspot.
3. **Walk through phone OTP.** This is the single biggest install drop-off; do not skip.
4. **Help them choose role + enter name.** This unblocks their first profile screen.
5. **Tell them: "Profile complete cheyi 7 days lo, we send ₹50 to your account."** (Issued via the activation flow — do not pay cash on the spot.)
6. **Note their first name + last 4 of phone in field log.** For attribution.
7. **Move to the next worker.** Do not over-sell — 2 minutes per person, not 15.

## Stop rules

- **If hands-given hits 50 and installs are < 5** → message is wrong. Pause. Test a different opener tomorrow:
  - Variant A: "Commission lekunda" (no commission).
  - Variant B: "Mee illu daggara jobs" (jobs near your home).
  - Variant C: "₹250 sign-up" (incentive-led).
- **If a worker is hostile or suspicious** → say "namaskaram" and move on. Do not argue.
- **If police / local hawker authority objects** → leave immediately. Find a 50-metre-away spot, repeat.

## Post-session (within 1 hour)

1. Update \`growth/campaigns/field_visits.csv\`.
2. Capture 1 verbatim quote and one objection in the row.
3. WhatsApp the field photo to the founder group.
4. If ≥ 1 testimonial-ready person met, schedule a 30-min interview within 5 days.

## Time + cost

- 2 hours per session.
- ₹0 marginal cost beyond pamphlets and incentives.
- ~₹6 per pamphlet handed (₹1 200 / 200).
- Target effective CAC ≤ ₹40 per activated worker.

## Scaling rule

- Founder runs the first 8 sessions personally — non-negotiable.
- Hand off to ops only after founder can articulate the top 3 patterns observed in writing.
- One ops person can run 3 sessions/week sustainably; do not push to 5 (burnout + sloppy logging).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/ngo-pilot">Ngo Pilot</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — NGO 50-Woman Cohort Pilot

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/outbound-whatsapp">Outbound Whatsapp</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Cold WhatsApp Outbound

> Goal: 30 outbound messages → ≥ 5 replies → ≥ 2 meaningful conversations → ≥ 1 activation per day.

## Pre-requisites

- A **business WhatsApp** number, separate from personal.
- **Warm-up period:** new numbers send max 20 / day for first 14 days. Send to friends + existing users + warm contacts. Do not cold-blast a fresh number.
- A **prospect list** in \`growth/campaigns/outbound.csv\` with columns filled.
- Personalisation source: visible Google Maps photo, Instagram bio, JustDial listing, society website. Each row needs **one personalisation hook** before the message goes out.

## Daily flow (60 minutes)

1. **0–10 min:** open the prospect list. Pick today's 30 prospects (sorted by pincode then partner type).
2. **10–40 min:** send 30 messages, each personalised with one specific detail. Use sequence templates from [outbound_sequences.md](../outputs/outbound_sequences.md).
3. **40–55 min:** triage replies from previous days. Move forward only those who showed real interest. Polite-decline the rest.
4. **55–60 min:** log all sends + replies into \`outbound.csv\`.

## Personalisation rules (the difference between 5 % and 25 % reply rate)

- Mention their **business name** in the first sentence.
- Mention something **specific you noticed** (their menu, their society's naming convention, an Instagram photo).
- **One ask per message.** Do not stack "interested? open to call? share with team?"
- Keep the first message **under 6 sentences**. Long messages get screenshot-and-deleted.
- **No emoji storms.** One, max.
- **No "Hope you're doing well."** Get to the point.

## Follow-up cadence

| Day | Action | Outcome if no reply |
|---|---|---|
| 0 | First message | wait |
| 3 | One-line follow-up + one new value angle | wait |
| 7 | Final short follow-up + permission-to-close | mark \`dead\` in CSV |
| 21 | Optional re-engage with a new hook (e.g., "festival rush, free promoted post") | mark \`dead\` again if no reply |

## Hard stop rules

- If WhatsApp marks the number "spam-suspected" → stop all sends from that number for 7 days. Switch to email.
- If you are reported by 2 unique recipients in a week → you are doing something wrong. Audit message + opener.
- Maximum **50 sends/day** even for a warm number.
- Never cold-message a personal phone number scraped from a residential WhatsApp group.

## Quality bar (review weekly)

- Reply rate: ≥ 12 % across the week.
- Meeting / call rate from replies: ≥ 25 %.
- Activations from meetings / calls: ≥ 30 %.
- If any of the above are below threshold → message-iterate before increasing volume.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/partnership-legal">Partnership Legal</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Partnership MoU Essentials

> What every RWA / NGO / contractor MoU must contain. Use this as a checklist before signing anything. Lawyer-review any contract beyond a 1-page MoU.

## 1-page MoU structure (default for non-monetary partnerships)

\`\`\`
MEMORANDUM OF UNDERSTANDING

Between: KGPV INNOVATION SOLUTIONS PRIVATE LIMITED ("DutyPe")
And:     <Partner legal name>     ("Partner")
Date:    YYYY-MM-DD
Term:    12 months, auto-renew unless cancelled with 30 days notice.

1. Purpose
   Partner will help DutyPe reach <segment> in <geography> for the purpose of
   connecting workers and employers via the DutyPe platform.

2. Partner will:
   - Share the agreed DutyPe message / poster with their members / residents
     no fewer than once per month.
   - Provide a single named coordinator for monthly check-ins.
   - Notify DutyPe if any member raises a complaint about the platform.

3. DutyPe will:
   - Provide co-branded creative assets at no cost.
   - Provide a Partner-specific install link for attribution.
   - Share monthly metrics on Partner-attributed installs and outcomes.
   - Not contact Partner's members for unrelated commercial purposes.

4. Compensation
   - This is a non-commercial partnership. No money changes hands.
   - DutyPe may, at its sole discretion, contribute to Partner's events
     (refreshments, banners) up to ₹X per quarter.

5. Data
   - DutyPe is the data controller for all user data captured via Partner's link.
   - Partner does not receive personally identifying information about members
     who install the app.
   - DutyPe complies with the DPDP Act 2023 and its own published privacy policy
     at https://dutype.in/privacy.

6. Termination
   - Either party may terminate with 30 days written notice.
   - Both parties stop using each other's name in marketing within 7 days of
     termination.

7. Disputes
   - Subject to courts in Hyderabad, Telangana.

Signatures:
DutyPe ____________________     Partner ____________________
\`\`\`

## Red flags — do NOT sign

- Partner asks for revenue share / per-install kickback in cash.
- Partner asks for exclusivity in their geography.
- Partner asks DutyPe to whitelist their members for premium features.
- Partner wants to control message wording such that scam-language flags would not catch their members.
- Anyone asks for upfront payment to "introduce" you to a community.

## Special-case MoUs (require lawyer review)

- NGO partnerships involving sub-grants or named-fund contributions.
- Contracts with government bodies (NSDC, state labour department).
- Apartment-management platform integrations (MyGate / NoBroker / Apartment Adda).
- Any contract above ₹50 K total annual value.
- Any contract giving the partner access to user data.

## Storage

- Signed MoUs (PDF) → \`growth/playbooks/mou_signed/<partner>_YYYY-MM-DD.pdf\`.
- Update \`growth/campaigns/partnerships.csv\` with \`signed_date\`.
- Diary the 30-day renewal-decision date.

## Operating reminder

- A signed MoU is a starting point, not an outcome. The outcome is **monthly resident installs** from that partner.
- Review every active partnership every 90 days. Cull partners with < 5 attributable installs in a quarter.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/press-pitch">Press Pitch</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Press Pitch (do NOT execute before 1 000 jobs filled)

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
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/rwa">Rwa</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — RWA Partnership

> Goal: each signed RWA admin → 200+ household reach + ≥ 10 employer signups + ≥ 3 jobs in first 30 days.

## Stage 1 — Identify (target: 30 admins / wedge pincode)

Sources, in priority order:
1. MyGate society directory (free, public). Filter by pincode.
2. Facebook groups: "Hyderabad Apartment Owners," "<area> RWA," etc.
3. Apartment Adda directory.
4. Walk-in: physical society reception desk during weekday morning.

Capture in \`growth/campaigns/partnerships.csv\` with \`partner_type=RWA\`.

## Stage 2 — First contact (WhatsApp script B3 from \`outbound_sequences.md\`)

- Always introduce yourself with full name + role.
- Say: "I'm not selling anything. I want to help your residents save the agency commission for their household help."
- Attach the [RWA one-pager](../assets/rwa_one_pager.md).
- Ask for a 15-min meet, not a 1-hour pitch.

## Stage 3 — In-person meet (≤ 20 min)

- Bring printed RWA one-pager.
- Show the live availability widget on dutype.in for *their* pincode.
- Ask three questions, listen:
  1. "How do residents currently find help?"
  2. "What's the biggest complaint you hear about it?"
  3. "If I gave you a single message to forward to residents, would you do it once a month?"
- Offer **co-branded poster** (their society name + DutyPe logo). This costs ₹500/society and converts.

## Stage 4 — Activation (within 7 days of yes)

- Send admin a co-branded WhatsApp message + poster PNG.
- Admin posts in society WhatsApp + lift notice board.
- Track installs from that pincode in the next 30 days.
- Founder follows up admin in 14 days with a thank-you + the count of installs from their society.

## Stage 5 — Monthly engagement

- Send admin a 1-line WhatsApp on the 1st of each month: "<Society> residents posted X jobs and hired Y workers via DutyPe last month. Thank you."
- Annually: a small physical thank-you (sweets / a society plaque). Do not turn this into a kickback.

## Stop rules

- If ≥ 5 admins messaged with 0 yeses → message is wrong. Iterate.
- If a yes never converts to ≥ 3 resident installs in 30 days → that admin is a polite-no in disguise. Don't waste more cycles.
- If an admin asks for personal monetary kickback → walk away. Note in CSV. Don't blacklist publicly, just deprioritise.

## Time + cost per admin

- Identification: 5 min.
- WhatsApp + follow-ups: 15 min.
- In-person meet: 30 min including travel.
- Co-branded asset: ₹500 + 30 min designer.
- Monthly engagement: 5 min/month.

## Scaling rule

- Founder personally signs the first 5 RWA admins per pincode.
- After 5 signed, hand off identification + first-contact to ops; founder still attends the in-person meet personally.
- Cap: 30 active partner admins per pincode (beyond that, monthly check-ins consume too much time).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/playbooks/worker-onboarding">Worker Onboarding</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Playbook — Worker Onboarding (field-led)

> Goal: same-day install → 80 % profile complete in 3 days → first application in 7 days.

## Trigger

- Worker installs the app, either organically or via naka / RWA / NGO field touch.

## Day 0 — Install moment

If field-led (naka or NGO event):
- Promoter walks through OTP on the worker's phone.
- Promoter helps choose role + enters first name.
- Worker is told: "Complete cheyi profile 7 days lo. ₹50 vasthayi."
- Promoter logs first name + last 4 of phone in field log → uploaded to \`growth/campaigns/field_visits.csv\` end of day.

If organic (Play Store install):
- Cloud Function fires welcome WhatsApp in Telugu within 5 min: "Welcome to DutyPe. Profile complete cheyandi to apply jobs near you."

## Day 1 — In-app activation

- App home screen: prominent "Complete profile (3 min)" card with progress bar.
- 80 % gate is enforced before "Apply" button is enabled (existing product).

## Day 2 — Push (if profile < 80 %)

- Push (Telugu): "Mee profile 60 % complete. Apply chesthe job dorukutadi. Tap to finish."

## Day 3 — WhatsApp nudge (if profile < 80 %)

- 1:1 WhatsApp from a Telugu-speaking ops person: "Em ayinadi? Help kavalaa?"
- This step has the highest activation lift in low-literacy segments.

## Day 7 — Reactivation flow G1 (if profile still < 80 %)

- Push + WhatsApp + (if email captured) email.
- Offer: "Profile complete cheyi today, ₹50 unlock."
- After reward issuance, worker activation rate jumps materially in our experience (logged in EXP-04).

## Day 7 — If profile ≥ 80 % but 0 applications

- Push: "Mee area lo 12 jobs unnayi. Tap to see."
- WhatsApp from ops: "Application cheyatam lo help kavala?"

## Day 14 — If still inactive

- Final WhatsApp + push burst.
- After this, do not contact for 30 days. Avoid spam reputation.

## Day 30 — Reactivation cohort

- One-shot reactivation: "Your area added X jobs this week. Free to apply."
- Stop after this. Don't haunt people who don't want to be users.

## Stop rules

- If worker explicitly says "remove me" → mark account opted-out, no further messages.
- If worker reports a bad employer experience → escalate to support, do not push job-recommendations until the case is closed.
- Never call workers on phone unless they explicitly opt in for a callback.

## Quality bar

- 80 % profile-complete rate within 7 days of install: ≥ 50 %.
- First-application rate within 7 days of activation: ≥ 60 %.
- D7 retention (any session in days 5–9): ≥ 40 %.
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>🔬</span>
            <span>Research</span>
            <small>5 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/research/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Research

> Outside-in market data, competitor teardowns, and ICP interview notes that ground the strategy in observable reality.

## Files

- [competitor_teardown.md](competitor_teardown.md) — feature, pricing, and positioning teardown of Apna, WorkIndia, Job Hai, Vahan, Quikr, OLX, WhatsApp groups.
- [hyderabad_market_brief.md](hyderabad_market_brief.md) — pincode demographics, household density, demand-side hypotheses for the wedge.
- [interview_notes_template.md](interview_notes_template.md) — interview note template (use for every worker / employer / partner conversation).
- [interviews/](interviews/) — one folder per ICP. Drop dated notes inside.

## Cadence

- Founder writes 1 long-form interview note per week minimum (≥ 30 minutes of conversation).
- Competitor teardown is refreshed every 90 days (or on a major launch / pricing change you observe).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/research/competitor-teardown">Competitor Teardown</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Competitor Teardown — Indian Blue-Collar Hiring (April 2026)

> Public-information snapshot. Verify quarterly. The point of this doc is **what we're up against** and **where the gaps are**, not "let's copy them."

---

## Direct competitors

### Apna (apna.co)
- **Positioning:** "India's largest professional community for blue & grey collar."
- **Scale (publicly claimed):** 50M+ users, 200+ cities (claim, not verified).
- **Geography:** Pan-India, Hindi-first.
- **Categories:** 70+, very wide; field sales, telecaller, drivers, delivery, factory, BPO heavy.
- **Monetisation:** Employer-paid job posts (₹500–₹3 000 per post, ranges).
- **Strengths:** Brand, supply liquidity in Hindi-belt cities, active feed/chat.
- **Weaknesses for our wedge:**
  - Hindi-first; Telugu UX is a second-class citizen.
  - National generic feed — does not surface "5 km from your house" for a Madhapur cook.
  - Scam / spam volume is high in low-SES segments — we hear this in field interviews.
  - Strong on white-collar-adjacent roles (telecaller, BPO), weaker on household (cook/maid).

### WorkIndia
- **Positioning:** "Find local jobs near you."
- **Scale:** 30M+ users (claim).
- **Geography:** Pan-India.
- **Categories:** Wide, blue-collar-heavy.
- **Monetisation:** Employer subscription (₹1 500–₹5 000/month).
- **Strengths:** Strong SEO presence on city × category long-tail.
- **Weaknesses:** Subscription model gates SMBs out; unverified employer concerns; weaker in Telugu states.

### Job Hai (Info Edge / Naukri group)
- **Positioning:** "Jobs near you." Powered by Naukri's brand.
- **Scale:** Strong recent growth.
- **Geography:** Pan-India.
- **Strengths:** Naukri brand recall; deep recruiter relationships.
- **Weaknesses:** Same as Apna — scale solves discovery but not local trust.

### Vahan
- **Positioning:** Specialised — gig delivery / cab driver staffing for fleet aggregators (Swiggy, Zomato, Uber, Rapido).
- **Geography:** Pan-India through fleet partners.
- **Categories:** Delivery / driver only.
- **Strengths:** Deep B2B fleet integrations; not really a worker-app brand.
- **Relevance to us:** Not a direct competitor for cook/maid wedge. Becomes relevant only if/when we add driver+delivery seriously.

---

## Indirect competitors (the real fight)

### WhatsApp groups (society + naka + RWA)
- **Cost:** ₹0.
- **Time-to-first-applicant:** 1–3 days (unverified, anecdotal).
- **Trust:** "I know the admin." High among existing residents.
- **Weakness:** Spam, unsorted, no verification, no QR, no record. Admins burn out.
- **Our edge:** Verified, hyper-local, structured, free for both sides, QR proof.

### Local agencies / contractors
- **Cost:** ₹2–5 K commission per worker placed.
- **Trust:** Mixed. Older households still default to them.
- **Weakness:** Worker grievance: "agency keeps half my month-1 salary." Employer grievance: "no replacement for 3 weeks if person quits."
- **Our edge:** Zero commission. Worker chooses directly. Replacement is one tap away.

### Quikr / OLX Jobs
- **Cost:** Free.
- **Trust:** Low. Scam-laden.
- **Weakness:** Largely a deprecated / abandoned product surface.
- **Our edge:** Active product with trust stack.

### Naka / labour-stand spot hiring
- **Cost:** ₹0.
- **Daily-only. No record. No retention.**
- **Our edge:** We can layer DIGITAL on top of an existing ritual, not replace it. (See \`playbooks/naka.md\`.)

### Society notice boards / RWA newsletters
- **Cost:** ₹0.
- **Reach:** Per-society.
- **Our edge:** Partner with the RWA admin instead of competing.

### MyGate / NoBroker / Apartment Adda (apartment apps)
- **Not direct competitors yet.** They have built-in classifieds / "find a maid" features but are not their core product.
- **Risk:** They could enter. **Mitigation:** partner with them before they build (EXP-13 in \`growth_experiments_backlog.md\`).

---

## Feature comparison vs DutyPe

| Feature | DutyPe | Apna | WorkIndia | Job Hai | Quikr | WhatsApp groups |
|---|---|---|---|---|---|---|
| Phone OTP | ✅ | ✅ | ✅ | ✅ | Partial | ❌ |
| Hyper-local 1/5/10 km | ✅ | ❌ | ✅ city-level | ✅ city-level | ❌ | ✅ society-level |
| Telugu UI | ✅ | Partial | Partial | ❌ | ❌ | n/a |
| QR work-start | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Scam-keyword filter | ✅ | Partial | Partial | Partial | ❌ | ❌ |
| Employer commission | ₹0 | Per-post fee | Subscription | Per-post fee | ₹0 | ₹0 |
| Worker fee | ₹0 | ₹0 | ₹0 | ₹0 | ₹0 | ₹0 |
| Refer & earn | ✅ ₹25 + tiers | Limited | Limited | ❌ | ❌ | ❌ |
| Voice / chat in app | ❌ (deliberate) | ✅ | ✅ | ✅ | ❌ | n/a |
| Verified-employer paid badge | ❌ (planned EXP-14) | ✅ | ✅ | ✅ | ❌ | ❌ |
| Cook + Maid focus | ✅ wedge | Diluted | Diluted | Diluted | ❌ | ✅ default |

---

## Where we deliberately *won't* compete

- **Field sales / telecaller / BPO** roles → Apna's stronghold. Different ICP, different employer type.
- **Factory / industrial labour at scale** → WorkIndia / Vahan. Different supply chain, different unit economics.
- **National Hindi-first launch** → playing on Apna's home turf.

---

## Where the gap exists for DutyPe

1. **Hyper-local cook + maid in Telugu metros** — under-served by every national app.
2. **RWA / society partnership channel** — no competitor systematically does this.
3. **QR work-start trust artifact** — unique in the segment as far as we can see.
4. **Pincode-level SEO** — Apna ranks for city, not pincode. Long-tail is wide open.

---

## What to monitor (refresh quarterly)

- New Apna product launches (rumour: a chat / community surface upgrade).
- WorkIndia pricing changes (subscription tier movement).
- Any apartment-management app launching a "hire help" feature.
- Vahan moving into household / non-fleet hiring.
- Government livelihood programmes (SHG, NSDC) launching aggregator apps.

Add findings to this doc dated; do not overwrite history.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/research/hyderabad-market-brief">Hyderabad Market Brief</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Hyderabad Market Brief — DutyPe wedge

> Outside-in snapshot for the founding city. Drives pincode picks, partnership lists, and demand-side targeting. Refresh every 90 days.

---

## Why Hyderabad first

- Telugu-dominant metro → asymmetric advantage with Telugu-localised UI already shipped.
- Dense apartment-cluster geography (Cyberabad, IT Corridor) → high household-employer concentration in 5–10 pincodes.
- Active migrant labour supply from coastal AP and Telangana districts → strong cook/maid/helper supply pool.
- Cheaper field operations than Bangalore or Mumbai (founder cost-per-naka-hour is half).
- Less price-sensitive employer base than tier-2 cities (PG owners, IT-sector households, F&B SMBs).

---

## Pincode shortlist (founder picks 3 of these 5 for week-1)

| Pincode | Area | Demand drivers | Supply drivers | Why for wedge |
|---|---|---|---|---|
| 500032 | Gachibowli | IT employees, high-rise residential, large apartment complexes | Migrant women workers from coastal AP, Telangana | Apartment density = household demand spike; nakas exist near IIT/UoH |
| 500081 | Madhapur / HITEC City | IT, OYO/PG concentration, F&B SMBs | Same as above; daily-wage commute | Highest employer concentration per square km |
| 500084 | Kondapur | Mixed residential + IT support | Settled local + migrant blend | Repeat-hire cycles strongest (long-tenure households) |
| 500049 | Miyapur | Lower-mid-income residential, Metro stop | Strong daily-wage supply, low employer per capita | Test: supply-rich, demand-light pincode |
| 500072 | Kukatpally | Dense residential, schools, salons | Strong supply, multiple nakas | Salon / tiffin SMB density high |

**Recommended start: 500032 + 500081 + 500084.** Two demand-rich (Gachibowli, Madhapur) + one repeat-hire-rich (Kondapur). Avoid starting in Miyapur until supply→demand pull is proven elsewhere.

---

## Demand-side estimate (rough order of magnitude)

> These are working hypotheses, not census data. Validate with field within first 14 days.

| Segment | Estimated count in 3 wedge pincodes | Hire frequency | Annual hire events |
|---|---|---|---|
| Households needing maid/cook | ~25 000 | 1.5×/yr | ~37 500 |
| PG / co-living operators | ~400 | 6×/yr | ~2 400 |
| Cloud kitchens / tiffin centres | ~200 | 8×/yr | ~1 600 |
| Small F&B (cafes, juice shops, dhabas) | ~600 | 4×/yr | ~2 400 |
| Salons / beauty parlours | ~300 | 3×/yr | ~900 |
| **TOTAL annual hire events** | | | **~44 800** |

Even capturing 1 % of this (≈ 450 hires/yr in 3 pincodes) = 9 hires/week — on track for the 60-fills-per-week day-90 goal once efficiency compounds.

---

## Supply-side estimate

| Segment | Estimated active in 3 wedge pincodes | Switch frequency | Annual switch events |
|---|---|---|---|
| Active cooks (women, 22–45) | ~5 000 | 1.2×/yr | ~6 000 |
| Active maids | ~12 000 | 1.5×/yr | ~18 000 |
| Helpers (male, 18–40) | ~6 000 | 2×/yr | ~12 000 |
| Drivers | ~3 500 | 1×/yr | ~3 500 |
| Delivery (gig, eligible to switch) | ~5 000 | High but locked into platforms | ~unclear |

**Wedge target: cook + maid → ~17 000 active workers, ~24 000 switch events/year.** Capture rate target: 5 % activation in 90 days = 850 active workers in app.

---

## Operating notes

- **Naka points:** Madhapur Police Station chowrasta, Gachibowli Stadium signal, Kondapur signal at Botanical Gardens, Miyapur Metro stop, Kukatpally Y-junction. Best 6–9 am.
- **RWA admin sources:** MyGate society directory (free public listing), Facebook "Hyderabad Apartment Owners" groups, Apartment Adda directory. Aim for ≥ 30 admin contacts per wedge pincode.
- **PG owner sources:** Google Maps "PG near <area>" + JustDial scrape. Cap 200 prospects per pincode.
- **Tiffin / cloud kitchen sources:** Swiggy / Zomato listings filtered by area + Instagram local food pages.
- **Government:** Telangana Department of Labour has monthly labour-fairs. Worth one founder visit/quarter.
- **NGOs to approach (initial list):** SAATH, Pratham, Mahita Society, ASMITA Resource Centre for Women, MV Foundation. (Verify each before outreach.)

---

## Competitive ground reality (refresh from field, not from press releases)

- Apna posters are visible at one Madhapur naka but not consistently maintained.
- WorkIndia presence is marginal in Hyderabad nakas (more visible in Bangalore).
- Local agencies dominate cook/maid hiring for older households — this is the segment to *not* fight over yet; focus on younger / IT-sector households who already use apps.
- WhatsApp groups dominate apartment-resident hiring — this is the segment to *win* via RWA partnerships.

---

## Risks specific to the city

- **Monsoon (Jun–Sep)**: naka activity drops. Plan to lean harder on RWA + outbound during these months.
- **Sankranti (Jan)** and **Bonalu (Jul–Aug)**: workers travel to native villages for 1–3 weeks; supply tightens. Pre-festival demand spikes for cooks and maids — exploitable.
- **Rapidly changing IT layoff cycles**: household budgets compress → maid switching frequency increases. Watch quarterly.

---

## Next-city candidates (do NOT pursue until day-90 gates are met)

| City | Pros | Cons |
|---|---|---|
| Vijayawada | Telugu-first, strong NRI / household demand, lower competitive intensity | Smaller absolute market |
| Bangalore | Largest BLW market, dense apartments | Hindi+Kannada UI needed, expensive ops, brutal competition |
| Visakhapatnam | Telugu-first, untapped, port-city demand | Smaller, less alpha |
| Pune | Apartment-dense, IT corridor | Marathi/Hindi UI needed |

Recommended order if all gates met: Vijayawada → Visakhapatnam → Bangalore.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/research/interview-notes-template">Interview Notes Template</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Interview Notes Template

> One file per interview. File in the appropriate \`/growth/research/interviews/<segment>/\` folder. ISO-date filename. Don't summarise; capture verbatim where possible.

\`\`\`
# Interview — <First Name> <Initial>, <ICP segment>

- **Date:** YYYY-MM-DD
- **Location:** <pincode + naka / society / phone>
- **Duration:** <minutes>
- **Conducted by:** <founder / ops>
- **Recording consent:** <yes/no>

## 1. Who they are
- Age, family situation, languages spoken.
- Years in current line of work.
- Current monthly income (range).

## 2. How they currently find work / hire
- Step-by-step: where do they start, who do they ask, how long does it take.
- What does "good" look like? What does "bad" look like?

## 3. Last hire / last job-search story
- Walk through the most recent one in detail.
- What went right. What went wrong.
- What would they have paid to avoid?

## 4. Verbatim quotes (3 minimum)
- "..."
- "..."
- "..."

## 5. Trust signals that mattered to them
- What made them trust the employer/worker.
- What made them distrust.

## 6. Phone / app behaviour
- Phone model (note brand for installability).
- Apps they use daily.
- WhatsApp groups they're in.
- Comfort with installing new apps (1–5).

## 7. Reaction to DutyPe (if shown)
- First impression.
- First objection.
- Specific feature that landed.
- Specific feature they didn't understand.

## 8. Money detail
- For workers: lowest acceptable wage, current commute cost, agency fees they've paid.
- For employers: budget per role, switching cost when worker leaves.

## 9. Action items from this conversation
- One product change to consider.
- One marketing-message change to consider.
- One person to introduce me to.

## 10. Follow-up
- Date for follow-up: YYYY-MM-DD
- What I will send them between now and then.
\`\`\`

## Quality bar

- ≥ 30 minutes of actual conversation.
- ≥ 3 verbatim quotes captured (paraphrasing kills the data).
- One concrete action item written, not a generic insight.
- File committed within 24 hours of the interview.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/growth/research/interviews/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Interviews

Drop one markdown file per interview here, organized by segment:

\`\`\`
interviews/
  workers/         ← cooks, maids, helpers, drivers
  households/      ← apartment-residing employers
  smb/             ← PG / cloud kitchen / salon / tiffin owners
  rwa/             ← RWA admins
  ngo/             ← NGO/SHG partners
  internal/        ← founder retros, team learnings
\`\`\`

Filename: \`YYYY-MM-DD_firstname-pincode.md\` (e.g. \`2026-04-22_lakshmi-500081.md\`).

Use [interview_notes_template.md](../interview_notes_template.md).

**Quality bar:** 1 interview / week minimum from the founder during the 90-day plan. No exceptions.
`} />
              </div>
            </details>
          </div>
        </section>

          </section>

          <section className="marketing-full-section marketing-full-depth-0">
            <h2 className="marketing-full-heading">
              <span>📢</span>
              <span>Marketing</span>
              <small>20 docs</small>
            </h2>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📢</span>
            <span>Marketing</span>
            <small>2 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe Marketing

> \`/growth/\` is the operating system (what to do, when, how, kill metrics).
> \`/marketing/\` is the **brand, message, and creative source-of-truth** (what we say, how we sound, what we look like).

## Subfolders

- [brand/](brand/) — name, tone, voice, visual identity, taglines, do's and don'ts.
- [channels/](channels/) — channel-specific message frameworks (Play Store, dutype.in, WhatsApp, Telugu Shorts, LinkedIn, press).
- [go-to-market/](go-to-market/) — positioning, ICP messaging, launch / city-expansion message kits.

## Relationship to /growth/

| Need | Look in |
|---|---|
| What to ship this week | \`/growth/outputs/weekly_execution_plan.md\` |
| Which channels to invest in | \`/growth/outputs/channel_prioritization.md\` |
| How a campaign should sound | \`/marketing/brand/voice_and_tone.md\` |
| What our position is vs. Apna | \`/marketing/go-to-market/positioning.md\` |
| What our Play Store should say | \`/growth/assets/play_store_listing.md\` (final copy) + \`/marketing/channels/play_store.md\` (rationale) |
| Logo / colour spec | \`/marketing/brand/visual_identity.md\` |

## Rules

1. **Do not put creative drafts here.** Final, ready-to-use creative goes to \`/growth/assets/\`.
2. **Do not put strategy docs here.** Strategy lives in \`/growth/outputs/\`.
3. This folder is for **principles and references** — short, durable, infrequently updated.
4. Anything updated more than once a month belongs in \`/growth/\`, not here.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/seo-quick-start">Seo Quick Start</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# 🚀 Quick Start: SEO & Security Fixes

## ⚡ Deploy Now (5 minutes)

\`\`\`bash
# Deploy to Firebase
firebase deploy --only hosting
\`\`\`

## 🚨 Fix Google Warning (10 minutes)

### Step 1: Check the Issue
1. Go to https://search.google.com/search-console
2. Click "Security & Manual Actions" → "Security Issues"
3. See which pages are flagged

### Step 2: Request Review
1. After deploying the security fixes above
2. Click "Request Review" in Search Console
3. Say: "Added security headers, blocked admin pages from indexing, and clarified redirect pages"

## 📊 Submit Sitemap (2 minutes)

1. Go to https://search.google.com/search-console
2. Click "Sitemaps" in left menu
3. Enter: \`sitemap.xml\`
4. Click "Submit"

## ✅ What You Got

### 5 New SEO Pages
- \`/jobs-near-me.html\` - Main jobs landing page
- \`/driver-jobs.html\` - Driver jobs
- \`/maid-jobs.html\` - Maid jobs  
- \`/delivery-jobs.html\` - Delivery jobs
- \`/driver-jobs-hyderabad.html\` - City-specific example

### Security Fixes
- ✅ Security headers added
- ✅ Admin pages blocked from Google
- ✅ Sitemap created
- ✅ Robots.txt updated

## 📈 Next Week

Create 10 more city pages:
- driver-jobs-vijayawada.html
- driver-jobs-bangalore.html
- maid-jobs-hyderabad.html
- delivery-jobs-bangalore.html
- cook-jobs-hyderabad.html

Copy the template from \`driver-jobs-hyderabad.html\` and change:
1. City name
2. Job category
3. Meta tagserw2

## 🎯 Expected Results

- **Week 1**: Google warning resolved, pages indexed
- **Month 1**: Start appearing in "jobs near me" searches
- **Month 3**: Top 10 rankings for city-specific searches
- **Month 6**: 50% of app installs from organic search

## 📚 Full Documentation

- \`docs/SEO_AND_SECURITY_SUMMARY.md\` - Complete overview
- \`docs/GOOGLE_HARMFUL_CONTENT_FIX.md\` - Security fix guide
- \`docs/SEO_IMPLEMENTATION.md\` - SEO strategy

## 🆘 Need Help?

Check Google Search Console daily for:
- Security issues
- Indexing status
- Search performance
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>🪣</span>
            <span>Brand</span>
            <small>4 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/brand/foundation">Foundation</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# DutyPe — Brand Foundation

> The non-negotiables. Read once. Remember always.

## What DutyPe is

A hyperlocal hiring app for blue-collar and household work in India. Telugu-first, Hyderabad-first.

## What DutyPe is not

- Not a national job board.
- Not a recruiter / staffing agency.
- Not a gig-economy aggregator.
- Not a chat / community app.
- Not a white-collar / corporate hiring product.

## Our promise to workers

> "Find a job near your home, in your language, without paying a paisa to anyone."

## Our promise to employers

> "Hire a phone-verified worker who lives within 5 km of you, in 24 hours, without paying any agency commission."

## Our promise to society

> "Move the agency commission back to the worker. Make hiring transparent. Document attendance with QR. Zero spam."

## What we will never do (brand red lines)

- Charge workers any fee, ever.
- Allow unverified employer phone numbers on the platform.
- Promise income or salary outcomes we can't guarantee.
- Use stock-photo "smiling diverse workers" in marketing.
- Buy fake reviews / installs.
- Translate Telugu copy via Google Translate.
- Send marketing messages between 22:00–08:00 IST.
- Run influencer marketing without first earning real testimonials.
- Sell, share, or rent worker phone numbers to third parties.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/brand/taglines">Taglines</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Taglines & Boilerplate

## Primary tagline

> **Hyperlocal hiring.** (English)
> **మీ ఇంటి దగ్గర ఉద్యోగాలు.** (Telugu)

Use as the locked-in lockup with the wordmark on A4+ collateral.

## Secondary taglines (per audience)

| Audience | Tagline (EN) | Tagline (TE) |
|---|---|---|
| Worker | Find work near your home. No commission. | మీ ఇంటి దగ్గర పని. కమిషన్ లేదు. |
| Household employer | Hire verified help in 24 hours. ₹0 commission. | 24 గంటల్లో verified help. కమిషన్ ₹0. |
| SMB employer | Fill blue-collar roles in your locality. ₹0 agency fee. | మీ area లో blue-collar staff. ₹0 fee. |
| RWA admin | Free, verified hiring for your residents. | మీ residents కోసం free verified hiring. |
| NGO partner | Move the commission back to the worker. | Commission ని workers ki return chesthamu. |

## One-line elevator pitches

- **For users (10 sec):** "DutyPe matches workers like cooks and maids with households and small businesses within 5 km of where they live, with phone-OTP verification and zero agency commission. Telugu-first."
- **For investors (15 sec):** "We're a hyperlocal blue-collar hiring app, density-first in Hyderabad, Telugu-localised. We do what Apna can't — match a Madhapur cook to a Madhapur household in 6 hours, with QR-verified attendance, at ₹0 commission. Bootstrapped, focused on 3 pincodes, currently {{N}} active workers."
- **For journalists (30 sec):** "{{N}} blue-collar workers in Hyderabad have skipped agency middlemen and earned ₹{{X}} directly through DutyPe in the last {{period}}. We're Telugu-first, hyperlocal (5 km radius), and use a QR-based work-start scan as a trust artifact for both worker and employer."

## Boilerplate (use at the bottom of press, partner emails, MoUs)

> DutyPe is a hyperlocal hiring app for blue-collar and household work in India, built by KGPV INNOVATION SOLUTIONS PRIVATE LIMITED, Hyderabad. We match workers and employers within a 5-km radius, in Telugu and English, with phone-OTP verification, scam-keyword filters, and QR-based work attendance. We charge zero commission to both workers and employers. dutype.in · WhatsApp +91 91217 06236 · dutypein@gmail.com.

## What we will not say (banned tagline patterns)

- "India's #1 …"
- "Trusted by lakhs / millions" (until literally true).
- "Find your dream job."
- "Empowering Bharat."
- "Future of work."
- "AI-powered hiring."
- "Match made in heaven."
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/brand/visual-identity">Visual Identity</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Visual Identity

> Lightweight system. Designed for a 2-person team to apply consistently. Avoid maintaining a 60-page brand book.

## Logo

- **Wordmark only** for sizes ≤ 32 dp / ≤ 24 mm print.
- **Wordmark + tagline** ("Hyperlocal hiring") for A4 print and above.
- Clear-space margin = height of the "D" on all sides.
- Never:
  - Place on busy photographic background.
  - Stretch / squish.
  - Add drop-shadow / outer glow.
  - Translate the wordmark itself ("DutyPe" stays Latin script in all locales).

## Colour

| Role | Hex | Use |
|---|---|---|
| Primary | \`#2563EB\` | Brand blue. Buttons, CTAs, headlines. |
| Primary deep | \`#1E40AF\` | Pressed states, hero overlays. |
| Text dark | \`#0F172A\` | Body text on light. |
| Text muted | \`#475569\` | Secondary text, captions. |
| Surface | \`#FFFFFF\` | Page background. |
| Surface alt | \`#F8FAFC\` | Card backgrounds. |
| Success | \`#16A34A\` | "Verified", positive states. |
| Warning | \`#F59E0B\` | Incentives, ₹ amounts. |
| Danger | \`#DC2626\` | Errors, scam-flag warnings. |

Do not use any other colours in marketing assets without designer + founder sign-off.

## Typography

- **Latin:** Inter (400 / 500 / 600 / 700). Free, web + Android friendly.
- **Telugu:** Hind Telugu (400 / 600). Pairs visually with Inter.
- **Hindi:** Noto Sans Devanagari (400 / 600).
- Headline weight: 700.
- Body weight: 400.
- Number tabular figures on for any data display.

## Iconography

- Use Phosphor or Lucide icon sets — pick one, stick to it.
- 1.5 px stroke for outline icons.
- No mixed icon styles (do not blend filled + outline).

## Photography rules

- **Real over stock.** Use only photos with consent.
- **Hands + tools** > faces (privacy-respectful and emotionally cleaner).
- **Telugu signage in frame** is a feature for Hyderabad locality, not a bug.
- **No "model release" Indian-stock-photo shots.** They look fake to the actual ICP.

## Illustration

- If used, single-line outline style, brand-blue stroke on white.
- No 3D / isometric / rounded-pop "Notion-style" illustrations.
- Reserve illustrations for empty states and onboarding screens.

## Layout rules

- 8 px grid for app and web.
- 16 px / 24 px / 32 px / 48 px scale.
- Generous whitespace. If in doubt, leave more space.

## Asset storage

- Source files (Figma) — link in \`marketing/brand/assets_index.md\` (TBD when designer is onboarded).
- Exported PNG / PDF — \`growth/assets/<asset>/\`.
- Logo files (SVG + PNG) — \`marketing/brand/logo/\` (drop here when delivered).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/brand/voice-and-tone">Voice And Tone</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Voice & Tone

> One voice across founder, ops, push, WhatsApp, Play Store, and PR. If a piece of copy can't pass these rules, it doesn't ship.

## Voice (always)

- **Plain.** No HR jargon, no startup-isms, no "synergy / leverage / scale."
- **Direct.** Get to the point in the first sentence.
- **Humble.** We are 1 200 users in 3 pincodes, not "India's leading platform."
- **Bilingual without code-switching gymnastics.** Telugu copy is full Telugu. English copy is full English. Code-switching only in WhatsApp where natural ("apply cheyandi → tap here").
- **Numbers over adjectives.** "47 cooks active in your pincode" beats "lots of nearby workers."

## Tone by audience

| Audience | Tone |
|---|---|
| Worker (in app + push) | Warm, respectful, action-oriented. Telugu by default. Address as \`మీరు\`. |
| Worker (WhatsApp from ops) | Like a friendly older sibling explaining a thing. Voice notes welcome. |
| Employer SMB | Confident, time-saving, ROI-led. English by default. |
| Employer household | Polite, trust-led. Telugu for Hyd locals, English fallback. |
| RWA admin | Peer-to-peer, never salesy. "I'm not selling anything" early. |
| NGO partner | Mission-aligned, women-first language, never patronising. |
| Investor / partner | Numbers-led, brutally honest, no hype. |
| Journalist | Story-led, on-record, never embargoed. |
| 1-star reviewer | Calm, factual, fix-the-thing, don't argue. |

## Words we use

- "Worker" (not "labourer," not "domestic help" — too patronising).
- "Employer" (not "boss," not "client").
- "Hire" (not "recruit").
- "Verified" (only when we mean phone-OTP — never imply BGV).
- "Nearby" (5 km radius, not "city-wide").
- "Free" (only where it's actually free, today).

## Words we don't use

- "Lakhs of users" (until it's true).
- "AI-powered" (we are not).
- "Disruptive" (overused, meaningless).
- "Background-verified" (we are not — we are phone-OTP-verified).
- "Salary guarantee" (we don't guarantee).
- "Highest paid" (unfalsifiable).
- "Game-changing," "revolutionary," "transformative" — banned.

## Sentence length

- Headlines ≤ 6 words.
- WhatsApp opening ≤ 6 sentences.
- Push body ≤ 110 chars.
- Press one-pager: every section ≤ 60 words.

## Calls-to-action — preferred verbs

- "Install free"
- "Post a job"
- "Apply now"
- "Open the app"
- "Get the WhatsApp link"

Never use:
- "Learn more"
- "Sign up today!" (with the exclamation)
- "Click here"
- "Don't miss out"
- "Limited time" (unless literally true)

## Emoji policy

- Maximum **one** per message / push.
- Allowed: 🙏 (thank you / namaste), ✓ (proof point), ✅ (verified), 🎉 (celebration on real wins), 📍 (location).
- Banned: 🔥 💪 🚀 ✨ 💯 (founder bro speak).

## Founder LinkedIn / X voice

- First-person.
- One observation, one number, one decision per post.
- No hashtags except in rare special cases (#opentowork, #hiring as data).
- Never repost startup-influencer content.
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>📡</span>
            <span>Channels</span>
            <small>8 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel Frameworks

> Per-channel principles. **What to publish, what to skip, what good looks like.** Tactical execution lives in \`/growth/outputs/\` and \`/growth/playbooks/\`.

## Files

- [play_store.md](play_store.md) — listing principles, ASO posture, screenshots framework.
- [website.md](website.md) — dutype.in principles + IA + page templates.
- [whatsapp.md](whatsapp.md) — WhatsApp marketing principles (consent, cadence, copy rules).
- [telugu_shorts.md](telugu_shorts.md) — YouTube Shorts + Instagram Reels framework.
- [founder_linkedin.md](founder_linkedin.md) — what to post, what not to post, cadence.
- [press.md](press.md) — when to talk to press, what to share.
- [paid_media.md](paid_media.md) — paid-media doctrine (≈ "don't, until X").

## Channel investment posture (snapshot)

| Channel | Posture | Owner |
|---|---|---|
| Play Store | High investment, refresh quarterly. | Founder + designer |
| dutype.in | Continuous build (250 SEO pages → 1000 over Q3). | Engineer |
| WhatsApp (1:1 + broadcast) | Founder-led for first 100 prospects, then ops. | Founder → ops |
| Telugu YouTube Shorts | 1/week minimum. | Founder + intern |
| Instagram Reels | Cross-post Telugu shorts only; no native Reel investment. | Auto |
| LinkedIn (founder) | 2/week. | Founder |
| X / Twitter | Cross-post LinkedIn. No native X investment. | Auto |
| Press | Reactive only until 1 000-fills milestone. | Founder |
| Meta paid ads | OFF until pricing tier proven. | n/a |
| Google Search ads | OFF until CAC payback < 60 days. | n/a |
| Quora / Reddit | 1 sprint per quarter. | Founder |
| Podcast appearances | Accept inbound only. Don't seek. | Founder |
| Conferences / events | Skip. | n/a |
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/founder-linkedin">Founder Linkedin</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Founder LinkedIn (cross-post X)

> Goal: attract investors, partners, journalists, and senior hires. **Not** for worker / employer acquisition.

## Strategic posture

- 2 posts / week, founder personally writing, no agency.
- One post = one observation + one number + one decision.
- Cross-post to X verbatim. No native X investment.

## What good looks like

- First-person narrative.
- A specific number (or a specific quote).
- A counter-intuitive or honest take.
- A clear ask if relevant ("intro to X").
- A photo from the field where possible (consent obtained).

## What bad looks like

- "Excited to announce …" intros.
- Multi-paragraph thought-leadership without numbers.
- Carousel posts about "Top 10 lessons from being a founder."
- Re-posting other startup-influencer takes.
- Hashtag walls.
- Posts written by an agency (investors smell it instantly).

## Templates

[\`growth/outputs/content_plan_90_days.md\`](../../growth/outputs/content_plan_90_days.md) section "Founder LinkedIn / X post templates" (L1–L5).

## Cadence

- Mon — numbers post (template L1).
- Thu — field note OR ask (template L3 or L5).
- Cancel weeks where you don't have a real number or a real story. Do not post fluff to maintain cadence.

## Profile setup

- Headline: "Founder, DutyPe — hyperlocal hiring for blue-collar India. Hyderabad."
- Featured: pinned link to dutype.in + one investor one-pager.
- About: one-paragraph description, founder photo, contact email.

## Measurement

- Quality > quantity. Watch:
  - Number of valuable conversations / week (intros, partner calls, journalist DMs).
  - Number of high-signal followers (operators / investors / press) added / month.
- Ignore: total views, likes, follower count.

## Stop rules

- If 4 weeks in a row produce 0 valuable inbound conversations → reduce cadence to 1 / week.
- If founder is spending > 2 hrs / week here → cut.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/paid-media">Paid Media</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Paid Media (current posture: OFF)

> The only marketing doc whose primary message is **"don't"**. Update when conditions change.

## Current posture

| Channel | Status | Re-evaluate when |
|---|---|---|
| Meta ads — workers | OFF | Worker LTV ≥ ₹100 (currently negligible). Not in the next 12 months unless something fundamental changes. |
| Meta ads — employers | OFF | EXP-10 (\`growth/outputs/growth_experiments_backlog.md\`) proves ≥ 5 % paying conversion + ≥ ₹100 ARPU. |
| Google Search ads | OFF | CAC payback < 60 days demonstrated on a small organic test. |
| Google Display | OFF | Never a fit for hyperlocal blue-collar; do not re-evaluate. |
| YouTube ads | OFF | After 50 K organic Telugu Shorts views with strong attribution. |
| ASA (Apple Search Ads) | n/a | iOS not built. |
| Programmatic / DSP | OFF | Never relevant at our scale. |
| Influencer (paid) | OFF | After 5 organic testimonials documented + first earned press placement. |
| OTT / TV | OFF | Re-evaluate when MAU > 100 K and burn budget allows ₹50 L+ campaigns. |
| OOH / hoardings | OFF | Same. |

## Why "OFF" is the strategy

- Meta CAC for our segment is ₹40–150 / install (per public benchmarks for blue-collar India).
- AdMob ARPU is < ₹5 / worker / month today.
- Until we ship a paying tier (EXP-10 / EXP-14) with proven willingness, every paid rupee on workers is permanently negative.
- Founder hours invested in field + RWA + outbound deliver 5–10× the activation rate per rupee.

## Pre-flight (when we DO turn paid on)

- Targeted single experiment ≤ ₹15 K budget, 14-day cap.
- Hypothesis written before launch.
- Single creative variant first.
- Pixel-validated event flow (install → activation → first match).
- Daily kill threshold: if CAC > 1.5× target after 5 days → pause.
- Weekly review at Friday standup.

## What changes the calculus

- A paying employer tier hitting ≥ 5 % conversion at ≥ ₹200 ARPU/month.
- A B2B contract (e.g., apartment management partner) covering CAC.
- A government / NSDC partnership covering NGO-cohort acquisition.
- An LTV-positive referral loop where worker LTV > acquisition cost.

Until any of those is true: **OFF**.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/play-store">Play Store</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Play Store

> The single highest-ROI free channel for an unfunded blue-collar marketplace. Treat the listing as the homepage.

## Strategic posture

- Win **long-tail** ("cook jobs hyderabad", "maid jobs near me") first; do not fight Apna on head terms.
- Optimise for **conversion** (visitor → install), not just for ranking.
- Ship **localised listings** in Telugu and Hindi; we have asymmetric advantage in Telugu.

## What good looks like

- Title carries 2–3 keywords + brand.
- Short description carries the wedge promise + commission line.
- 8 screenshots — first 4 worker, next 4 employer.
- Each screenshot has a 6–10 word headline overlay.
- Long description ≥ 1 800 chars, ≤ 4 000 chars, with category list and city list inside (helps long-tail).
- One Play Console A/B running at any time.
- Crash-free rate ≥ 99.5 %.
- 4-day max response time on every 1–3 ★ review.

## What bad looks like (do not do)

- Generic title ("DutyPe — Local Jobs").
- Stock-photo screenshots.
- "Bug fixes & improvements" in what's-new.
- Listing that promises features that aren't shipped.
- > 1 listing change per 14 days.
- Screenshots showing user phone numbers (Play policy violation).

## Production source-of-truth

- Final copy: [\`growth/assets/play_store_listing.md\`](../../growth/assets/play_store_listing.md).
- Screenshot specs: [\`growth/assets/play_store_screenshots/README.md\`](../../growth/assets/play_store_screenshots/README.md).
- Release process: [\`growth/playbooks/aso_release.md\`](../../growth/playbooks/aso_release.md).
- Strategy doc: [\`growth/outputs/app_store_growth_plan.md\`](../../growth/outputs/app_store_growth_plan.md).

## Measurement

- Play Console: visitors → installs (target +30 % vs baseline within 14 days of every refresh).
- Search-installs vs explore-installs ratio (we want search ratio rising).
- Top-keyword rankings (manual check + AppTweak / Sensor Tower free tier).
- 7-day uninstall rate (target < 35 %).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/press">Press</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Press

> Earned media is leverage when there is a real story. Until then, founder time is more valuable in the field.

## Strategic posture

- **Reactive only** until eligibility gate is met.
- Eligibility gate: ≥ 1 000 cumulative jobs filled, ≥ 1 city density milestone, ≥ 1 case study.
- Telugu-language press first (Eenadu, Sakshi, V6) before national tech press.

## What good looks like

- A pitch with a specific number ("workers earned ₹X without paying agency commission").
- 3 named, consenting interview subjects ready to go.
- A founder who can speak fluently in Telugu + English on-record.
- A 1-page press one-pager with verified numbers.

## What bad looks like

- Paid PR placements.
- Embargoes (we don't have leverage to embargo).
- "As featured in" startup directories.
- Pitching by tagging on X.
- Sending the same pitch to 5 journalists same day.
- Pitching before milestone is real (kills future credibility).

## Source-of-truth

- Pitch playbook: [\`growth/playbooks/press_pitch.md\`](../../growth/playbooks/press_pitch.md).
- Press one-pager: [\`growth/assets/press_one_pager.md\`](../../growth/assets/press_one_pager.md).

## Measurement

- Number of earned placements / quarter (target: 1 by end of Q3 in city-1).
- Inbound from press placement (intro emails, partner DMs).
- Do NOT measure "PR value in ad-equivalent ₹" — vanity.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/telugu-shorts">Telugu Shorts</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Telugu YouTube Shorts (+ cross-post Instagram Reels)

> Our content channel for workers and households in Telangana / AP. Not a vanity feed; an acquisition channel.

## Strategic posture

- 1 Short / week, minimum, 12 weeks straight. Compounding requires consistency.
- Cross-post to Instagram Reels (auto) — do not invest separately in Instagram.
- Telugu-only audio. Subtitles in Telugu and English.
- Source content from real field interactions — not actors, not stock.

## What good looks like

- Hook in first 1 second (face + a tension question, NOT a logo intro).
- 25–35 second runtime.
- One quote per video, captioned in Telugu and English.
- One CTA at the end ("Install free → DutyPe app").
- Vertical 9:16, 1080×1920.
- Thumbnail with Telugu headline overlay.

## What bad looks like

- Founder talking-head only (do this max 1 in 5 Shorts).
- Stock-photo b-roll.
- Generic "tips for blue-collar workers" content.
- More than one CTA per video.
- Background music louder than dialogue.
- Hindi or English-only audio (kills relevance for Telugu wedge).

## Format playbook (rotate)

| Format | Source | Goal |
|---|---|---|
| Worker testimonial (30 s) | Field interview | Trust + worker installs |
| Employer testimonial (30 s) | Field interview | Trust + employer signups |
| Founder field note (60 s vertical, used as a Long video too) | Founder | Authority + partner inbound |
| App walkthrough (45 s, screen-recording with voiceover) | Founder | Reduce install-to-activation drop |
| RWA partner spotlight (30 s, with admin's permission) | RWA | Co-marketing + new RWA inbound |

## Production rules

- Phone-shot is fine. iPhone or recent Android.
- Natural light. No fancy rigs.
- Edit in CapCut or InShot. No 3rd-party agency edit.
- Caption every spoken word (Telugu + English).
- Brand colour \`#2563EB\` for any text overlay.

## Cadence + cost

- 1 Short / week. ~2 hours founder time per Short (filming + editing).
- ₹0 marginal cost.

## Strategy doc

[\`growth/outputs/content_plan_90_days.md\`](../../growth/outputs/content_plan_90_days.md).

## Measurement

- 7-day views / Short.
- Click-through on bio link.
- Attributable installs via UTM (\`utm_source=yt_shorts&utm_campaign=<short_id>\`).
- Kill any format that fails to produce ≥ 500 views in 30 days.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/website">Website</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — Website (dutype.in)

> The website is **not** a brochure. It is a conversion + SEO surface.

## Three jobs of the site

1. Convert worker visitors → app install (\`/worker\` + sticky mobile CTA).
2. Convert employer visitors → app install or web post-job (\`/employer\`).
3. Capture long-tail SEO traffic via programmatic city × pincode × category pages.

## What good looks like

- Homepage forces a **role decision** in the hero (worker vs employer).
- /worker and /employer are **separate flows**, not equal-weight tabs.
- Live counts (\`/api/stats?pincode=…\`) are **server-rendered from Firestore**.
- LCP < 1.5s on 3G.
- One CTA per viewport.
- Programmatic SEO pages auto-\`noindex\` when count < 5.
- JobPosting schema on every programmatic page.
- WhatsApp number + email in the footer of every page.

## What bad looks like

- Carousels in the hero.
- Hard-coded ("totally fake") stats.
- A blog (don't build it unless committing to weekly cadence for 6 months).
- A chatbot widget.
- AdSense.
- Multi-language toggles for languages we don't support.

## IA (locked)

\`\`\`
/                                 ← homepage (decision splitter)
/worker                           ← worker flow
/employer                         ← employer flow
/post-job                         ← web-side post-job (deep-link to app)
/hyderabad                        ← city hub
/hyderabad/<pincode>/<category>   ← programmatic SEO
/safety                           ← trust hub
/refer                            ← referral
/help                             ← FAQ + WhatsApp
/legal/privacy /legal/terms /legal/refund /legal/account-deletion
\`\`\`

Don't add new top-level routes without founder + engineer agreement.

## Strategy doc

[\`growth/outputs/landing_page_recommendations.md\`](../../growth/outputs/landing_page_recommendations.md).

## Owner

- Engineer owns implementation.
- Founder owns hero copy + CTA wording.
- Designer owns hero visuals.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/channels/whatsapp">Whatsapp</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Channel — WhatsApp

> WhatsApp is our most personal channel. Treat it like a phone call from a friend, not a marketing blast.

## Strategic posture

- 1:1 founder-led for the first 100 prospects per ICP segment.
- Templates only after we know what works.
- Always *consent first*. Account creation = consent for own-account transactional messaging only.

## What good looks like

- Personalised first sentence with a real detail.
- ≤ 6 sentences in the opener.
- One ask per message.
- Reply within 2 hours during business hours.
- 48 h SLA on every 1–3 ★ Play review reply that came in via WhatsApp.

## What bad looks like

- Cold-blasting a fresh business number (it gets banned in days).
- Long opening pitch with multiple links.
- Sending between 22:00–08:00 IST.
- Sending the same message to 200 people in one day from a new number.
- Using bit.ly or other URL shorteners (Meta penalises).
- Pretending to be a "team" when you're a solo operator.

## Templates source-of-truth

[\`growth/assets/whatsapp_templates.md\`](../../growth/assets/whatsapp_templates.md).

## Operating playbook

[\`growth/playbooks/outbound_whatsapp.md\`](../../growth/playbooks/outbound_whatsapp.md).

## Cadence cap

- 2 marketing messages per user per week. Maximum.
- 50 outbound sends per business number per day. Maximum.
- 14-day warm-up for any new business number.

## Reputation safety

- If WhatsApp flags the number "spam-suspected" → stop all marketing for 7 days, switch to email.
- If 2 unique recipients report in a week → audit message + opener before resuming.
- Maintain 2 backup business numbers (warmed) so a ban on one doesn't halt operations.

## Measurement

- Reply rate (target ≥ 12 % across the week).
- Meeting / call rate from replies (target ≥ 25 %).
- Activations from meetings (target ≥ 30 %).
- Number of WhatsApp-attributed installs / week (UTM in deep link).
`} />
              </div>
            </details>
          </div>
        </section>

        <section className="marketing-full-section marketing-full-depth-1">
          <h3 className="marketing-full-heading">
            <span>🚀</span>
            <span>Go To Market</span>
            <small>6 docs</small>
          </h3>
          <div className="marketing-full-files">
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/readme">Readme</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Go-To-Market

> Per-audience GTM messaging, positioning, and launch kits. Reused across channels.

## Files

- [positioning.md](positioning.md) — competitive positioning + differentiation.
- [icp_messaging.md](icp_messaging.md) — message frameworks per ICP segment.
- [city_launch_kit.md](city_launch_kit.md) — repeatable kit for opening any new city.
- [category_launch_kit.md](category_launch_kit.md) — repeatable kit for opening any new category.
- [seasonal_calendar.md](seasonal_calendar.md) — festival / wedding / academic-year demand spikes.

## Cadence

- Refresh positioning + ICP messaging every 90 days based on Friday-review learnings.
- City + category launch kits are reference; refresh after each launch with what was learned.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/category-launch-kit">Category Launch Kit</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Category Launch Kit

> Repeatable playbook for opening category N+1 inside a city. Do **not** add a category until cook + maid combined hit ≥ 60 fills/week and median TFA ≤ 6 h.

## Recommended sequence

1. Cook + Maid (current wedge).
2. **Driver** (highest LTV potential — personal, school, small fleet).
3. Helper / Delivery (existing supply, lower LTV).
4. Skilled trades (electrician, plumber, painter, carpenter) — defer until 12-month mark.

## Pre-launch (week -2 to week 0)

- [ ] Verify wedge gate met (60 fills/week, ≤ 6 h TFA).
- [ ] Audit existing category-N supply already on the platform (sometimes you have it without realising).
- [ ] Update Play Store screenshots to feature the new category prominently in slot 2 (worker side).
- [ ] Write category-specific Telugu pamphlet variant.
- [ ] Write category-specific WhatsApp opener for employer outbound.
- [ ] Identify category-specific channel:
  - Driver: school WhatsApp groups, PG networks, small fleet operators on JustDial.
  - Delivery: gig-economy facebook/WhatsApp groups, Quikr competitors.
  - Skilled trades: Justdial partner network, tools / hardware shops, Sulekha alternatives.
- [ ] Add category to \`/<city>/<pincode>/<category>\` SEO templates.

## Week 1 of category-N

- [ ] First 50 pamphlets distributed at category-N's natural supply gather-points (e.g., for drivers — auto stand at metro stations).
- [ ] First 30 employer WhatsApp opens specific to the new category.
- [ ] Cloud Function: ensure FCM topic \`<category>_workers\` is firing.
- [ ] Push to existing workers in adjacent categories: "Did you know we have <new category> jobs too?"

## Week 2–4

- [ ] First 50 active category-N workers.
- [ ] First 10 active category-N employers.
- [ ] First 5 category-N fills.

## Day 30 checkpoint

If category-N hits ≥ 50 active workers + first 5 fills → keep.
If less → category-N is wrong for this city wedge OR our channel mix doesn't reach its supply. Pause; do not add a third category.

## What this kit does NOT include

- Adding all 17 categories at once.
- Building category-specific app screens (the existing post-job stepper handles all categories).
- A separate marketing brand for the new category (one DutyPe brand, always).
- Paid acquisition for the new category supply (still OFF).

## Owner

- Founder for first 2 weeks of any new category.
- Ops + engineer for routine supply outreach + Cloud Function topic setup.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/city-launch-kit">City Launch Kit</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# City Launch Kit

> Repeatable playbook for opening city N+1. Do **not** open a new city until the day-90 expansion gates from \`growth/outputs/master_growth_strategy.md\` are met.

## Pre-launch (week -4 to week 0)

- [ ] Confirm all 3 expansion gates met in city-1.
- [ ] Pick city-2 from candidate list (Vijayawada → Visakhapatnam → Bangalore preferred order).
- [ ] Write a city-specific brief (mirror of [\`growth/research/hyderabad_market_brief.md\`](../../growth/research/hyderabad_market_brief.md)).
- [ ] Pick 3 wedge pincodes in city-2.
- [ ] Translate / localise marketing assets:
  - Pamphlet (city-name swapped).
  - Society poster.
  - WhatsApp templates.
  - Push copy (city-name + local language if different).
- [ ] Update Play Store long-description to include city-2 in city list.
- [ ] Add city-2 to \`/<city>/<pincode>/<category>\` programmatic pages.
- [ ] Identify 30 RWA admins per pincode in city-2.
- [ ] Identify 200 SMB prospects per pincode (PG, cloud kitchen, salon, tiffin).
- [ ] Hire / assign one city operator (founder for first 2 weeks; full-time ops by week 4).

## Week 1 of city-2

- [ ] Founder spends 5 days in city-2 personally.
- [ ] Naka pamphlet drops × 4 mornings (one per pincode + one repeat).
- [ ] 30 RWA admins messaged.
- [ ] 50 SMBs cold-messaged.
- [ ] Telugu / local press soft launch (1 journalist coffee, no formal pitch yet).

## Week 2–4 of city-2

- [ ] First RWA partnership signed.
- [ ] First NGO partnership conversation opened.
- [ ] 200+ city-2 active workers.
- [ ] 25+ city-2 active employers.
- [ ] First Telugu Short filmed in city-2 (different geography signals authenticity).

## Day 30 city-2 checkpoint

If city-2 hits ≥ 50 % of city-1's day-30 metrics → keep going.
If less → diagnose (wrong pincode? wrong message? wrong founder presence?). Do not push city-3.

## Day 90 city-2 checkpoint

Mirror of city-1 day-90 success criteria.

## What this kit does NOT include

- A "national rollout plan" (we don't have one).
- A press launch with a national publication (don't pitch press for city expansion until city-2 hits day-90 milestones).
- Paid media spend (still OFF — see [\`marketing/channels/paid_media.md\`](../channels/paid_media.md)).
- A new product feature (city expansion is a distribution motion, not a product motion).

## Owner

- Founder for weeks -4 through 4.
- City operator from week 4 onward.
- Engineer for SEO / push / WhatsApp infra (one-time setup).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/icp-messaging">Icp Messaging</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# ICP Messaging

> One framework per ICP segment from \`growth/outputs/ideal_customer_profiles.md\`. Use the message blocks below to ground every ad / push / WhatsApp / pitch.

---

## ICP-1 — Worker (Female cook / maid, 22–45, Hyderabad wedge pincode)

| Element | Message |
|---|---|
| Their pain | "Agency took ₹4 000 last time and I never got my second month's salary." |
| Our promise | "Find a job near your home. No commission. Telugu support." |
| Trust hook | "Phone OTP verified employers. QR proof of every shift." |
| Action | "Install free. Profile complete chesthe ₹50 unlock." |
| What NOT to say | "Earn ₹X / month" (sets expectations we can't guarantee). "App" in English-only contexts (use "DutyPe" + Telugu verbs). |

Voice: warm, respectful, action-oriented. Telugu by default.

---

## ICP-2 — Worker (Male helper / delivery / driver, 18–40, outer-ring pincodes)

| Element | Message |
|---|---|
| Their pain | "Naka pe khade-khade din nikal jata hai. Pucha tha tho aaj kal kaam nahi." |
| Our promise | "Daily / hourly work near you. Zero commission. Same-day reply." |
| Trust hook | "Phone-verified employers. Pay tracked with QR." |
| Action | "Install free. 12 jobs in your area today." |
| What NOT to say | "Earn ₹50,000 from home" (scam pattern). "Online jobs" (wrong audience). |

Voice: blunt, opportunity-led. Telugu primary, with Hindi fallback.

---

## ICP-3 — Apartment household (Working couple, 28–45, IT corridor pincode)

| Element | Message |
|---|---|
| Their pain | "Agency took ₹4K, the maid quit in 10 days, no replacement." |
| Our promise | "Verified maid / cook hired in 24 hours, 5 km from your apartment, ₹0 commission." |
| Trust hook | "Phone-OTP. QR work-start. Replaceable in 24 h if they leave." |
| Action | "Post a job free in 2 minutes." |
| What NOT to say | "Background-verified" (we are not). "Premium service" (free is the position). |

Voice: time-saving, ROI-led. English by default; Telugu line for warmth.

---

## ICP-4 — SMB owner (PG / cloud kitchen / salon / tiffin, 30–55, single pincode)

| Element | Message |
|---|---|
| Their pain | "Worker quit Sunday night, kitchen has no helper Monday morning." |
| Our promise | "Fill blue-collar roles in your locality, in 24 hours. ₹0 agency fee. Replacement guaranteed within 24 h." |
| Trust hook | "Phone-verified workers. QR-based attendance. Telugu support." |
| Action | "Post free → first applications today. 5-min onboarding call from founder." |
| What NOT to say | "Subscription" (free is the position). Generic "blue-collar India" pitch — use the segment's specific pain (tiffin, salon, etc.). |

Voice: confident, time-saving, peer-to-peer (founder ↔ founder).

---

## ICP-5 — Channel partner (RWA admin / NGO leader / contractor)

| Element | Message |
|---|---|
| Their interest | Their members / residents save commission and time. |
| Our promise | "Free, verified hiring for your community. Co-branded posters at no cost. Monthly impact report." |
| Trust hook | "Founder-led; meet me in person. KGPV INNOVATION SOLUTIONS PRIVATE LIMITED, registered in Hyderabad." |
| Action | "Forward our co-branded WhatsApp to your community once a month." |
| What NOT to say | Anything that suggests cash kickback. "Affiliate program" (wrong frame). |

Voice: peer-to-peer, mission-aligned, never salesy.

---

## Universal rules across all ICPs

- Always include WhatsApp number \`+91 91217 06236\` and email \`dutypein@gmail.com\`.
- Always include legal entity on print + PDF assets.
- Always lead with the pincode-specific number when one is available ("47 cooks active in your pincode").
- Never include screenshot images that show real PII.
- Telugu copy reviewed by a native speaker, not Google Translate.
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/positioning">Positioning</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Positioning

> Where DutyPe sits in the market, in plain language. The reference doc for any pitch / press / partner conversation.

## One-line positioning

> **For Telugu-speaking households and SMBs in Hyderabad who need to hire blue-collar help nearby, DutyPe is the only hyperlocal hiring app that matches workers within 5 km, in Telugu, with phone-OTP verification, QR-based work-start, and ₹0 commission for both sides.**

## Positioning statement (long form)

\`\`\`
For:                 Households + SMBs in Telugu metros (start: Hyderabad).
Who need:            To hire cooks, maids, helpers, drivers — fast, locally,
                     without paying agency commission.
DutyPe is:           A hyperlocal hiring app.
That:                Matches workers and employers within 5 km, with phone-OTP
                     verification + QR-based work-start trust artifacts.
Unlike:              Apna / WorkIndia / Job Hai (national-generic, Hindi-first,
                     city-level radius), local agencies (₹2-5K commission),
                     WhatsApp groups (no verification, no record).
We have:             Telugu-first product, RWA-partnership distribution,
                     Hyderabad-based founder presence, server-side fraud-score
                     filter, zero-commission economics for both sides.
\`\`\`

## Competitive matrix

| | DutyPe | Apna | WorkIndia | Job Hai | Local agency | WhatsApp groups |
|---|---|---|---|---|---|---|
| Hyperlocal radius | ✅ 1/5/10 km | ❌ city | ✅ city | ✅ city | ✅ society | ✅ society |
| Telugu-first | ✅ | Partial | Partial | ❌ | Mixed | n/a |
| Phone-OTP verified | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| QR work-start | ✅ unique | ❌ | ❌ | ❌ | ❌ | ❌ |
| Commission to worker / employer | ₹0 / ₹0 | ₹0 / ₹500-3K | ₹0 / sub | ₹0 / per-post | ~25-50% / ₹2-5K | ₹0 / ₹0 |
| Fraud-score auto-filter | ✅ | Partial | Partial | Partial | n/a | ❌ |
| Telugu support | ✅ | ❌ | ❌ | ❌ | Mixed | n/a |

## What we are deliberately NOT positioning as

- A national job board.
- A white-collar / corporate hiring product.
- A staffing agency / recruiter aggregator.
- A gig-economy aggregator (we don't take a delivery / cab cut).
- A community / chat / social product.
- An "AI-powered matching" product (we're not, and pretending we are erodes trust).

## Defending against "But Apna also does this"

Standard response (memorise):
> "Apna is excellent at scale across Hindi-belt cities for telecaller, BPO, and field-sales roles. We're built for one specific job: matching a household in Madhapur to a cook who lives 3 km away, in Telugu, with QR-verified attendance, in 6 hours. Apna doesn't try to do that — and shouldn't, given their scale. We're not in their fight."

## Defending against "But you're tiny"

> "Yes — by design. We're betting that owning 3 Hyderabad pincodes deeply produces a marketplace that scales city-by-city, not category-by-category. Density is the only moat in a hyperlocal marketplace. National generic-supply doesn't have it; we will."

## Re-positioning triggers (when to update this doc)

- A new direct competitor enters Telugu-first hyperlocal hiring.
- We add a 3rd category and the messaging needs to flex.
- We add a 2nd city and the "Hyderabad" anchor needs to broaden.
- Our QR-work-start feature is matched by a competitor (likely > 12 months out).
`} />
              </div>
            </details>
            <details className="marketing-full-file">
              <summary>
                <span className="marketing-full-file-icon">📄</span>
                <span className="marketing-full-file-title"><a href="/admin/marketing/marketing/go-to-market/seasonal-calendar">Seasonal Calendar</a></span>
              </summary>
              <div className="marketing-full-file-body">
                <MarkdownView source={`# Seasonal Calendar — Hyderabad

> Demand spikes / supply tightenings to plan around. Copy this template for new cities; adjust for local festivals.

## Annual rhythm

| Month | Event | Demand impact | Supply impact | Marketing action |
|---|---|---|---|---|
| Jan | Sankranti (mid-Jan) | High household demand for cooks, helpers, painters (pre-festival cleaning + big meals). | Workers travel to native villages 1–3 weeks. | Pre-festival push (week -2): "Hire your Sankranti cook now." Pamphlet emphasis at nakas in week -3. |
| Feb | Wedding season starts | Spike for cooks, waiters, decorators (helper-tagged). | Stable. | Push "wedding-season hire" to households + small banquet halls. |
| Mar | Holi + financial year-end | Mild household demand. | Stable. | Standard. |
| Apr | Ramadan / Ugadi | Iftar tiffin demand spike. | Stable. | Push to PG owners + cloud kitchens for iftar prep. |
| May | School holidays | Demand: caretaker / part-time helpers (kids at home). | Migrant workers travel home. | Caretaker / part-time category push. |
| Jun | Monsoon onset | Naka activity ↓ 30 %. | Some supply migration. | Lean on RWA + outbound; reduce naka days; double WhatsApp outbound. |
| Jul | Bonalu (Hyderabad/Telangana festival) | Festival cleaning + cook spike. | Workers travel for puja. | Festival-specific push 2 weeks ahead. |
| Aug | Independence Day + Raksha Bandhan | Mild demand. | Stable. | Standard. |
| Sep | Onam (Kerala migrants) + Ganesh Chaturthi | Demand spike especially in Kerala-migrant pockets + festival cleaning. | Some supply migration to Kerala. | Push to apartment communities; Kerala-migrant cohort if any. |
| Oct | Dussehra / Bathukamma | Telangana festival. Cook + maid demand spike. | Telangana workers travel home. | Pre-festival hire push. Bathukamma-week WhatsApp campaign. |
| Nov | Diwali + post-Diwali wedding season | Massive cleaning + cook + helper + painter demand. | Workers travel for Diwali. | Pre-Diwali campaign starts week -4. Pamphlet drops doubled in 2 weeks before. |
| Dec | Year-end + New Year + Christmas | Steady household demand; F&B / catering demand spike. | Stable. | Push to F&B SMBs. |

## Monsoon special (Jun–Sep)

- Naka activity drops materially.
- Plan to lean harder on RWA + WhatsApp outbound + SEO during these 4 months.
- Double the SEO programmatic page rollout in this window (low-cost, monsoon-immune).

## Festival operating principles

- Pre-festival demand spike windows are 14–21 days before the festival.
- Worker supply migration windows are 7 days before to 14 days after the festival.
- Push notifications for festival-specific roles must be scheduled 14 + 7 days before.
- Do NOT send marketing push during the festival day itself (low open rates + poor optics).

## Wedding season

- October–March is peak wedding season in Telangana / AP.
- Demand for waiters, helpers, cooks, decorators (helper-tagged) is high.
- Banquet halls and tiffin centres are the SMB target.

## Competitive seasonality

- Apna typically runs "Diwali bonus" worker-acquisition campaigns in Oct.
- Watch competitor Play Store listing changes in Sep–Oct each year and respond if needed.

## Maintenance

- Update this file each January with the prior year's lessons.
- Each new city gets its own \`seasonal_calendar.md\` (Vijayawada festival list differs from Bangalore).
`} />
              </div>
            </details>
          </div>
        </section>

          </section>
        </div>
      </div>
    </div>
  );
}
