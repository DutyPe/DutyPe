import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# RWA Admin — One-Pager

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
