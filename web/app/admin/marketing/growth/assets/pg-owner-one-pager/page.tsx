import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# PG / Cloud-Kitchen Owner One-Pager (A4)

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
