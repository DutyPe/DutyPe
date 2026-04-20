import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Playbook — Employer First-Post Hand-Holding

> Goal: every newly signed employer posts their first job within 24 hours of account creation, with high-quality fields, and gets at least one application within 24 hours of the post.

## Trigger

- New employer account created (Cloud Function fires).
- WhatsApp template message (B5 in [outbound_sequences.md](../outputs/outbound_sequences.md)) sent within 30 minutes.

## Step 1 — Within 30 min of signup
- WhatsApp: "Welcome to DutyPe. I'm <name>, founder. I'll personally make sure you fill your first hire within 48 hours. Reply with the role you're hiring for."
- (For first 100 employers in city-1, this WhatsApp is from the founder personally — NOT a Cloud Function template. After 100, hand off to ops.)

## Step 2 — Within 24 h
- If they haven't posted: send a **prefilled job link** (deep link with category + pay-type + radius pre-set based on their account profile pincode).
- Offer: "I can take 2 minutes on a call to walk through it." (Most won't take it; 10 % will, and they convert at 3×.)

## Step 3 — Once posted
- Audit the post within 1 hour. If structured fields are weak (vague title, missing salary, missing timing), WhatsApp the employer with a one-line suggestion.
- Push the job to the right worker cohort (Cloud Function — currently in \`functions/src/notifications.ts\`). Verify it fired.

## Step 4 — 24 h after post
- If no applications: WhatsApp employer with one of:
  - "Salary range nelaki ₹X is below our area median; raise to ₹Y to triple applications."
  - "Add a Telugu line in description; cooks read it 3× more."
  - "Your radius is set to 1 km; expand to 5 km for this category."
- If applications received but employer hasn't replied: WhatsApp nudge — "3 applications waiting, 22 hours since first one."

## Step 5 — Worker selected
- Walk the employer through QR work-start (this is the trust artifact; it's free leverage).
- WhatsApp confirmation when the worker QR-ins on day 1.

## Step 6 — 7 days post-fill
- WhatsApp: "Is <worker name> still showing up? 1-tap reply: yes / not great / left. Anything I can fix?"
- This single question saves you weeks of wondering why repeat-post rate is low.

## Step 7 — 30 days post-fill (repeat-post nudge)
- "Need to hire again? Free to post. Refer another household — both get ₹50."

## Stop rules

- If an employer ghosts after step 4 with no reply for 7 days → stop nudging. Re-engage at D30 only.
- If an employer posts a clearly fraudulent job (already auto-flagged by your fraud-score system, score ≥ 70) → confirm reject and *do not* re-engage.

## Quality bar

- ≥ 70 % of new employers post within 24 hours.
- ≥ 50 % of first posts get an application within 24 hours.
- ≥ 30 % of first posts result in a fill within 7 days.
- ≥ 25 % of first-fills lead to a repeat post within 30 days.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
