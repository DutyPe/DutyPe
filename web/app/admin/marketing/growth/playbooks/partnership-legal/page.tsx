import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# Playbook — Partnership MoU Essentials

> What every RWA / NGO / contractor MoU must contain. Use this as a checklist before signing anything. Lawyer-review any contract beyond a 1-page MoU.

## 1-page MoU structure (default for non-monetary partnerships)

\`\`\`
MEMORANDUM OF UNDERSTANDING

Between: KGPV INNOVATION SOLUTIONS PRIVATE LIMITED ("DutyPe")
And:     <Partner legal name>     ("Partner")
Date:    YYYY-MM-DD
Term:    12 months, auto-renew unless cancelled with 30 days notice.

1. Purpose
   Partner will help DutyPe reach <segment> in <geography> for the purpose of
   connecting workers and employers via the DutyPe platform.

2. Partner will:
   - Share the agreed DutyPe message / poster with their members / residents
     no fewer than once per month.
   - Provide a single named coordinator for monthly check-ins.
   - Notify DutyPe if any member raises a complaint about the platform.

3. DutyPe will:
   - Provide co-branded creative assets at no cost.
   - Provide a Partner-specific install link for attribution.
   - Share monthly metrics on Partner-attributed installs and outcomes.
   - Not contact Partner's members for unrelated commercial purposes.

4. Compensation
   - This is a non-commercial partnership. No money changes hands.
   - DutyPe may, at its sole discretion, contribute to Partner's events
     (refreshments, banners) up to ₹X per quarter.

5. Data
   - DutyPe is the data controller for all user data captured via Partner's link.
   - Partner does not receive personally identifying information about members
     who install the app.
   - DutyPe complies with the DPDP Act 2023 and its own published privacy policy
     at https://dutype.in/privacy.

6. Termination
   - Either party may terminate with 30 days written notice.
   - Both parties stop using each other's name in marketing within 7 days of
     termination.

7. Disputes
   - Subject to courts in Hyderabad, Telangana.

Signatures:
DutyPe ____________________     Partner ____________________
\`\`\`

## Red flags — do NOT sign

- Partner asks for revenue share / per-install kickback in cash.
- Partner asks for exclusivity in their geography.
- Partner asks DutyPe to whitelist their members for premium features.
- Partner wants to control message wording such that scam-language flags would not catch their members.
- Anyone asks for upfront payment to "introduce" you to a community.

## Special-case MoUs (require lawyer review)

- NGO partnerships involving sub-grants or named-fund contributions.
- Contracts with government bodies (NSDC, state labour department).
- Apartment-management platform integrations (MyGate / NoBroker / Apartment Adda).
- Any contract above ₹50 K total annual value.
- Any contract giving the partner access to user data.

## Storage

- Signed MoUs (PDF) → \`growth/playbooks/mou_signed/<partner>_YYYY-MM-DD.pdf\`.
- Update \`growth/campaigns/partnerships.csv\` with \`signed_date\`.
- Diary the 30-day renewal-decision date.

## Operating reminder

- A signed MoU is a starting point, not an outcome. The outcome is **monthly resident installs** from that partner.
- Review every active partnership every 90 days. Cull partners with < 5 attributable installs in a quarter.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
