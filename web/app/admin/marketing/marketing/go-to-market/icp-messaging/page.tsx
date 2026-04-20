import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# ICP Messaging

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
