# DutyPe Product & Feature Roadmap

> **Mission**: Build India's most trusted, direct, and hyperlocal job marketplace for blue-collar, grey-collar, daily-wage, and micro-task workers — completely eliminating exploitative middlemen, agent cuts, and fraudulent postings.

---

## 🧭 Problem Statement & Core Proposition

| Problem Faced by Indian Workers & Employers | DutyPe Solution |
| :--- | :--- |
| **Exploitative Middlemen & Agency Cuts** | **Direct Worker-to-Employer Connection**: 100% free for workers with direct calling & WhatsApp links. |
| **High Commute Friction in Tier 2/3 & Metros** | **Hyperlocal Geohash Matching**: Jobs within 1 km, 5 km, and 10 km radius of the worker's home. |
| **Emergency Same-Day Staffing Shortages** | **Instant Work / Urgent Needs**: Real-time push notification fanout to available nearby workers within minutes. |
| **Low Literacy & Typing Hesitation** | **Vernacular & Voice-First UX**: Telugu, Hindi, and local language interfaces with audio-guided discovery. |
| **Unverified Workers & Distrust** | **Verified Community Badges & Proof of Work**: ID badges, digital attendance logs, and two-way ratings. |

---

## 🚀 Missing High-Impact Features (Ranked by Business Value)

### Phase 1: Growth & Conversion Boosters (Immediate)

#### 1. 📄 1-Tap WhatsApp "DutyPe Digital Job Card / Biodata"
* **Use Case**: Domestic helpers, drivers, electricians, and factory hands frequently get asked for a biodata/CV by housing society security, local shops, or contractors.
* **Feature**: Generates a visually crisp image/PDF containing photo, verified skills, experience, language proficiency, rating, and a DutyPe QR code profile link.
* **Growth Impact**: Workers share their DutyPe Job Card on **WhatsApp Status & Community Groups**, driving 100% organic, viral app installs.

#### 2. ⚡ "Ready to Work Today" / Live Radar (Instant Day-Gigs)
* **Use Case**: Catering helpers needed for an evening wedding, loading helpers needed for a 3-hour truck unload, or emergency maid replacements.
* **Feature**: A toggle on the worker home screen: **"Available for work today / Free right now"**.
* **Impact**: Nearby employers can view live available workers within 3–5 km on a radar map and send a 1-tap booking request for same-day hiring.

#### 3. 🎙️ 30-Second Audio Bio & Voice Search
* **Use Case**: Many blue-collar users struggle with complex search queries and keyboard typing.
* **Feature**:
  * **Voice Search**: Speak in Telugu/Hindi/English (*"Warangal lo driver job"* or *"Kukatpally cook job"*).
  * **30-Sec Voice Intro**: Worker records a brief self-introduction (*"Nenu 5 years nunchi automatic car driving chestunnanu..."*). Employers listen before calling to assess communication.

---

### Phase 2: Trust, Safety & Scheduling (Medium-Term)

#### 4. 📅 Walk-In Interview Scheduler & Google Maps Integration
* **Use Case**: Employers tell candidates over the phone to visit tomorrow, but workers forget the address or miss the time window.
* **Feature**: Employers configure open walk-in interview hours (e.g. *Monday 10 AM – 2 PM*). Workers receive a 1-tap "Get Directions (Google Maps)" navigation link and an automated reminder 2 hours prior.

#### 5. 🛡️ Trust & Safety: ID Verification Badges (Aadhaar / Driving License)
* **Use Case**: In-home hiring (maids, cooks, babysitters, private drivers) has high trust barriers.
* **Feature**: Voluntary badge verification (Driving License validation for drivers, masked Aadhaar badge). Badged profiles receive higher ranking and a "Verified by DutyPe" trust shield.

#### 6. 📝 Daily Work Log & Digital Pay Slip (Duty Khata)
* **Use Case**: Daily-wage workers face wage disputes, unpaid overtime, and lack proof of past earnings.
* **Feature**: Employers mark "Duty Completed" and log payout amount. Generates a verified digital receipt that builds a credit/employment track record on the worker's DutyPe profile.

---

### Phase 3: Engagement & Scaling (Long-Term)

#### 7. 💬 In-App Direct Chat with Multilingual Quick-Action Chips
* **Use Case**: Women workers and domestic staff prefer not exposing personal phone numbers immediately; employers miss phone calls during working hours.
* **Feature**: Lightweight real-time chat with pre-translated Telugu/Hindi/English quick response chips (*"What is the shift timing?"*, *"Is food provided?"*, *"I can join from tomorrow"*).

#### 8. ⭐ 2-Way Rating System (Workers Rating Employers)
* **Use Case**: Hold abusive or non-paying employers accountable.
* **Feature**: Workers rate employers on timely payment and accurate job descriptions. Good employers receive a **"Top Trusted Employer"** badge.

#### 9. 📲 Hyperlocal WhatsApp Job Alerts Bot
* **Use Case**: Blue-collar users check WhatsApp 10x more than opening apps.
* **Feature**: Automated broadcast whenever a new job opens within 3 km (*"Namaste Suresh! A Delivery job opened 2 km from your home in Madhapur paying ₹18,000/mo. Tap to apply on DutyPe."*).

#### 10. 🏢 Gated Society & Factory Bulk Hiring Portal
* **Use Case**: Gated communities and factories hire 10–50 workers simultaneously (security guards, housekeeping, gardeners).
* **Feature**: Bulk posting wizard with grouped applicant review for society associations and HR managers.

---

## 🗺️ Empty-State & Geographic Expansion Strategy

When a worker opens DutyPe in a city/state with no active vacancies:
1. **Never Show a Dead End**: Replace plain "No jobs found" with a welcoming, promising expansion card: *"DutyPe is expanding to [City/State] soon! 🚀"*.
2. **Capture Worker Demand**: A 1-tap **"Notify Me First"** button registers user location demand in Firestore (`location_demand_leads`), giving the sales team data on where to acquire employers next.
3. **Viral Employer Referrals**: 1-tap **"Invite Local Employers"** WhatsApp button lets workers invite neighborhood shops/contractors to post jobs for free.
4. **Hub Discovery**: Quick-switch chips for active hubs (Hyderabad, Vijayawada, Warangal, Visakhapatnam, etc.).
