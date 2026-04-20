import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Category Launch Kit

> Repeatable playbook for opening category N+1 inside a city. Do **not** add a category until cook + maid combined hit ≥ 60 fills/week and median TFA ≤ 6 h.

## Recommended sequence

1. Cook + Maid (current wedge).
2. **Driver** (highest LTV potential — personal, school, small fleet).
3. Helper / Delivery (existing supply, lower LTV).
4. Skilled trades (electrician, plumber, painter, carpenter) — defer until 12-month mark.

## Pre-launch (week -2 to week 0)

- [ ] Verify wedge gate met (60 fills/week, ≤ 6 h TFA).
- [ ] Audit existing category-N supply already on the platform (sometimes you have it without realising).
- [ ] Update Play Store screenshots to feature the new category prominently in slot 2 (worker side).
- [ ] Write category-specific Telugu pamphlet variant.
- [ ] Write category-specific WhatsApp opener for employer outbound.
- [ ] Identify category-specific channel:
  - Driver: school WhatsApp groups, PG networks, small fleet operators on JustDial.
  - Delivery: gig-economy facebook/WhatsApp groups, Quikr competitors.
  - Skilled trades: Justdial partner network, tools / hardware shops, Sulekha alternatives.
- [ ] Add category to \`/<city>/<pincode>/<category>\` SEO templates.

## Week 1 of category-N

- [ ] First 50 pamphlets distributed at category-N's natural supply gather-points (e.g., for drivers — auto stand at metro stations).
- [ ] First 30 employer WhatsApp opens specific to the new category.
- [ ] Cloud Function: ensure FCM topic \`<category>_workers\` is firing.
- [ ] Push to existing workers in adjacent categories: "Did you know we have <new category> jobs too?"

## Week 2–4

- [ ] First 50 active category-N workers.
- [ ] First 10 active category-N employers.
- [ ] First 5 category-N fills.

## Day 30 checkpoint

If category-N hits ≥ 50 active workers + first 5 fills → keep.
If less → category-N is wrong for this city wedge OR our channel mix doesn't reach its supply. Pause; do not add a third category.

## What this kit does NOT include

- Adding all 17 categories at once.
- Building category-specific app screens (the existing post-job stepper handles all categories).
- A separate marketing brand for the new category (one DutyPe brand, always).
- Paid acquisition for the new category supply (still OFF).

## Owner

- Founder for first 2 weeks of any new category.
- Ops + engineer for routine supply outreach + Cloud Function topic setup.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
