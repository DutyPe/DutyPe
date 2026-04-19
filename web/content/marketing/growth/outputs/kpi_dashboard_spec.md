# DutyPe – KPI / Dashboard Spec

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

- **Pincode worker density**: count of `profile_complete && active_in_30d` workers per wedge pincode.
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

```
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
```

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
