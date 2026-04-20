import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Pamphlet — Telugu (A5, double-sided)

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
