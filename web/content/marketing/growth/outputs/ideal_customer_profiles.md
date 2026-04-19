# DutyPe – Ideal Customer Profiles

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
- See `outbound_sequences.md` for scripts.

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
