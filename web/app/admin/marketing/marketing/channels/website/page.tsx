import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Channel — Website (dutype.in)

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
