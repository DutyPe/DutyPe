# Playbook — Cold WhatsApp Outbound

> Goal: 30 outbound messages → ≥ 5 replies → ≥ 2 meaningful conversations → ≥ 1 activation per day.

## Pre-requisites

- A **business WhatsApp** number, separate from personal.
- **Warm-up period:** new numbers send max 20 / day for first 14 days. Send to friends + existing users + warm contacts. Do not cold-blast a fresh number.
- A **prospect list** in `growth/campaigns/outbound.csv` with columns filled.
- Personalisation source: visible Google Maps photo, Instagram bio, JustDial listing, society website. Each row needs **one personalisation hook** before the message goes out.

## Daily flow (60 minutes)

1. **0–10 min:** open the prospect list. Pick today's 30 prospects (sorted by pincode then partner type).
2. **10–40 min:** send 30 messages, each personalised with one specific detail. Use sequence templates from [outbound_sequences.md](../outputs/outbound_sequences.md).
3. **40–55 min:** triage replies from previous days. Move forward only those who showed real interest. Polite-decline the rest.
4. **55–60 min:** log all sends + replies into `outbound.csv`.

## Personalisation rules (the difference between 5 % and 25 % reply rate)

- Mention their **business name** in the first sentence.
- Mention something **specific you noticed** (their menu, their society's naming convention, an Instagram photo).
- **One ask per message.** Do not stack "interested? open to call? share with team?"
- Keep the first message **under 6 sentences**. Long messages get screenshot-and-deleted.
- **No emoji storms.** One, max.
- **No "Hope you're doing well."** Get to the point.

## Follow-up cadence

| Day | Action | Outcome if no reply |
|---|---|---|
| 0 | First message | wait |
| 3 | One-line follow-up + one new value angle | wait |
| 7 | Final short follow-up + permission-to-close | mark `dead` in CSV |
| 21 | Optional re-engage with a new hook (e.g., "festival rush, free promoted post") | mark `dead` again if no reply |

## Hard stop rules

- If WhatsApp marks the number "spam-suspected" → stop all sends from that number for 7 days. Switch to email.
- If you are reported by 2 unique recipients in a week → you are doing something wrong. Audit message + opener.
- Maximum **50 sends/day** even for a warm number.
- Never cold-message a personal phone number scraped from a residential WhatsApp group.

## Quality bar (review weekly)

- Reply rate: ≥ 12 % across the week.
- Meeting / call rate from replies: ≥ 25 %.
- Activations from meetings / calls: ≥ 30 %.
- If any of the above are below threshold → message-iterate before increasing volume.
