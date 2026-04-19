# Playbook — Friday Review

> 60 minutes, every Friday at 4 pm IST. Founder + engineer + ops (if any). No exceptions.

## Pre-meeting (founder, 30 min before)

- Pull all numbers from `kpi_dashboard_spec.md` dashboard.
- Update `growth/campaigns/experiments.csv` with the week's reads.
- Have one-pager open with the 9 metrics from the dashboard.

## Agenda (60 min)

### Block 1 — Numbers (15 min)
- Walk the dashboard, top to bottom.
- Founder calls out the **one** number they're most worried about.
- Engineer calls out one technical leading indicator (crash rate, uptime, queue lag).
- Ops (if any) calls out one field signal (worker quotes, employer complaints).

### Block 2 — Experiments (20 min)
- Each active experiment gets a 3-minute slot:
  - Owner reads success metric vs kill metric.
  - Decision: **KEEP / KILL / SCALE**.
  - One sentence on next action.
- Maximum 3 active experiments. If 4, the oldest underperformer is killed by default.

### Block 3 — Field readouts (10 min)
- Founder reads top 3 verbatim quotes from the week (workers + employers + partners).
- One concrete product / message / channel change derived from quotes.

### Block 4 — Anti-pattern check (5 min)
- Founder asks aloud (from `what_not_to_do.md` ¶ "Anti-pattern detector"):
  - Did we add a feature to avoid a hard sales conversation?
  - Did we open a new channel to avoid fixing an old one?
  - Did we ship a "rebrand" because we couldn't ship a metric?
  - Did we send a press email because the cohort retention chart looked bad?
- If any "yes" → fix that thing next week. No new bets.

### Block 5 — Next week locked (10 min)
- Founder writes the **one number** the team will move next week.
- Each owner writes their week's top 3 outputs in the shared plan.
- Meeting ends with a single Slack/WhatsApp message: "<DATE> review. Top number: X. Owners' top 3 each below."

## Discipline rules

- No slides. The dashboard is the slide.
- No "let's discuss next week." Every issue gets a decision in the meeting.
- No new initiatives proposed without naming what gets dropped.
- No retroactive scope changes to running experiments.

## Output artifact

- One Markdown note dropped in `growth/playbooks/reviews/YYYY-MM-DD.md` summarising decisions made + the week's "one number."
