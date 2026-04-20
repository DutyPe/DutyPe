import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Current Landing Page Copy (input file)

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
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
