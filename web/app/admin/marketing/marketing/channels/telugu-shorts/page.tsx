import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Channel — Telugu YouTube Shorts (+ cross-post Instagram Reels)

> Our content channel for workers and households in Telangana / AP. Not a vanity feed; an acquisition channel.

## Strategic posture

- 1 Short / week, minimum, 12 weeks straight. Compounding requires consistency.
- Cross-post to Instagram Reels (auto) — do not invest separately in Instagram.
- Telugu-only audio. Subtitles in Telugu and English.
- Source content from real field interactions — not actors, not stock.

## What good looks like

- Hook in first 1 second (face + a tension question, NOT a logo intro).
- 25–35 second runtime.
- One quote per video, captioned in Telugu and English.
- One CTA at the end ("Install free → DutyPe app").
- Vertical 9:16, 1080×1920.
- Thumbnail with Telugu headline overlay.

## What bad looks like

- Founder talking-head only (do this max 1 in 5 Shorts).
- Stock-photo b-roll.
- Generic "tips for blue-collar workers" content.
- More than one CTA per video.
- Background music louder than dialogue.
- Hindi or English-only audio (kills relevance for Telugu wedge).

## Format playbook (rotate)

| Format | Source | Goal |
|---|---|---|
| Worker testimonial (30 s) | Field interview | Trust + worker installs |
| Employer testimonial (30 s) | Field interview | Trust + employer signups |
| Founder field note (60 s vertical, used as a Long video too) | Founder | Authority + partner inbound |
| App walkthrough (45 s, screen-recording with voiceover) | Founder | Reduce install-to-activation drop |
| RWA partner spotlight (30 s, with admin's permission) | RWA | Co-marketing + new RWA inbound |

## Production rules

- Phone-shot is fine. iPhone or recent Android.
- Natural light. No fancy rigs.
- Edit in CapCut or InShot. No 3rd-party agency edit.
- Caption every spoken word (Telugu + English).
- Brand colour \`#2563EB\` for any text overlay.

## Cadence + cost

- 1 Short / week. ~2 hours founder time per Short (filming + editing).
- ₹0 marginal cost.

## Strategy doc

[\`growth/outputs/content_plan_90_days.md\`](../../growth/outputs/content_plan_90_days.md).

## Measurement

- 7-day views / Short.
- Click-through on bio link.
- Attributable installs via UTM (\`utm_source=yt_shorts&utm_campaign=<short_id>\`).
- Kill any format that fails to produce ≥ 500 views in 30 days.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
