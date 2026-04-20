import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Competitive Landscape (input file)

> Replace approximate notes with founder's real comparison. Be honest.

## Direct competitors (blue-collar, India, app-based)
- **Apna** — large network, weak hiring conversion, broad categories, urban focus, no QR, no hyper-local radius.
- **WorkIndia** — older, B2B sales motion, recruiter-skewed, expensive for small employers.
- **Job Hai (Info Edge)** — Naukri's blue-collar app, small-employer aware, scam infested.
- **Vahan / Frontier Markets** — driver/delivery vertical, B2B contracts with Swiggy/Zomato, not direct.
- **Babajob** — defunct/absorbed; useful as a cautionary tale.

## Indirect competitors (where workers actually find work today)
- **WhatsApp groups** (the real #1 competitor; free, trusted, but no proof / no payment trail).
- **Naka labour points** (street corners where day-workers gather at dawn).
- **Local contractors / middlemen** (charge 10–30% cut).
- **Quikr / OLX Jobs** (declining but still used for cooks/maids).
- **Apartment society RWA + facility managers** (especially for cook/maid).
- **Newspaper classifieds** (declining).
- **Facebook groups + Telegram channels** for city-specific hiring.

## DutyPe's defensible edges (today, verifiable)
- Hyper-local radius (1/5/10 km) and geohash backend — Apna does not match this granularity.
- QR-based work-start verification — neither WhatsApp nor Apna has this.
- Scam-keyword + fraud-score firewall at post time — quantifiably better than Quikr/OLX/Apna for trust.
- Telugu localization — under-served vs. Hindi-only competitors.
- Direct worker↔employer (no recruiter cut) — compelling for small employers.

## DutyPe's weaknesses (be honest)
- Smaller worker pool per pincode than Apna in any tier-1 city.
- No background verification (Apna started doing some).
- No insurance / escrow.
- No iOS.
- Brand awareness ≈ zero.

## What this means for positioning
- Don't fight Apna on "biggest network." You'll lose.
- Win on **trust + hyper-local + verified employers**. The only people who care: households + micro-employers who got burned on Quikr/Apna.
- Don't try to compete with WhatsApp groups directly — *augment* them: invite the group admins to be DutyPe partners.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
