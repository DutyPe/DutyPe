# Playbook — Worker Onboarding (field-led)

> Goal: same-day install → 80 % profile complete in 3 days → first application in 7 days.

## Trigger

- Worker installs the app, either organically or via naka / RWA / NGO field touch.

## Day 0 — Install moment

If field-led (naka or NGO event):
- Promoter walks through OTP on the worker's phone.
- Promoter helps choose role + enters first name.
- Worker is told: "Complete cheyi profile 7 days lo. ₹50 vasthayi."
- Promoter logs first name + last 4 of phone in field log → uploaded to `growth/campaigns/field_visits.csv` end of day.

If organic (Play Store install):
- Cloud Function fires welcome WhatsApp in Telugu within 5 min: "Welcome to DutyPe. Profile complete cheyandi to apply jobs near you."

## Day 1 — In-app activation

- App home screen: prominent "Complete profile (3 min)" card with progress bar.
- 80 % gate is enforced before "Apply" button is enabled (existing product).

## Day 2 — Push (if profile < 80 %)

- Push (Telugu): "Mee profile 60 % complete. Apply chesthe job dorukutadi. Tap to finish."

## Day 3 — WhatsApp nudge (if profile < 80 %)

- 1:1 WhatsApp from a Telugu-speaking ops person: "Em ayinadi? Help kavalaa?"
- This step has the highest activation lift in low-literacy segments.

## Day 7 — Reactivation flow G1 (if profile still < 80 %)

- Push + WhatsApp + (if email captured) email.
- Offer: "Profile complete cheyi today, ₹50 unlock."
- After reward issuance, worker activation rate jumps materially in our experience (logged in EXP-04).

## Day 7 — If profile ≥ 80 % but 0 applications

- Push: "Mee area lo 12 jobs unnayi. Tap to see."
- WhatsApp from ops: "Application cheyatam lo help kavala?"

## Day 14 — If still inactive

- Final WhatsApp + push burst.
- After this, do not contact for 30 days. Avoid spam reputation.

## Day 30 — Reactivation cohort

- One-shot reactivation: "Your area added X jobs this week. Free to apply."
- Stop after this. Don't haunt people who don't want to be users.

## Stop rules

- If worker explicitly says "remove me" → mark account opted-out, no further messages.
- If worker reports a bad employer experience → escalate to support, do not push job-recommendations until the case is closed.
- Never call workers on phone unless they explicitly opt in for a callback.

## Quality bar

- 80 % profile-complete rate within 7 days of install: ≥ 50 %.
- First-application rate within 7 days of activation: ≥ 60 %.
- D7 retention (any session in days 5–9): ≥ 40 %.
