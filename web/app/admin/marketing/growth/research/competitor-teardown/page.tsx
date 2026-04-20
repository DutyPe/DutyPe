import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Competitor Teardown — Indian Blue-Collar Hiring (April 2026)

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
