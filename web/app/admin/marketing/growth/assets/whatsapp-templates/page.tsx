import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# WhatsApp Templates

> Production-grade WhatsApp Business message templates. Submit to Meta for "Utility" or "Marketing" template approval before high-volume use.
>
> All templates have variables in \`{{double_braces}}\`. Cloud Function fills variables before send.

---

## Worker — welcome (Utility, Telugu)

\`\`\`
Namaste {{1}} 🙏

DutyPe ki welcome.

Mee profile complete cheyyandi 7 days lo →
mee area lo verified jobs apply cheyochu.

Help kavalantey reply cheyandi.

— DutyPe team
\`\`\`

## Worker — D7 incomplete profile (Marketing, Telugu)

\`\`\`
{{1}}, mee profile inkaa complete kaaledhu.

Profile complete chesthe ₹50 unlock + jobs apply cheyochu.

Tap → {{deep_link}}

Sahaayam kavalantey reply cheyandi. Telugu lo support unnaadi.
\`\`\`

## Worker — D30 inactive (Marketing, Telugu)

\`\`\`
{{1}}, mee area lo {{N}} jobs add ayinayi past week lo.

Apply cheyandi free ga →
{{deep_link}}

Notification off cheyalantey reply "STOP".
\`\`\`

## Employer — welcome (Utility, English)

\`\`\`
Hi {{1}},

Welcome to DutyPe. I'm {{founder_name}}, founder.

Post your first job in 2 minutes →
{{deep_link_post_job}}

Reply with the role you're hiring for and I'll personally help you fill it within 48 hours.
\`\`\`

## Employer — D1 no post (Marketing, English)

\`\`\`
{{1}}, your DutyPe account is active but you haven't posted a job yet.

Your area ({{pincode}}) has {{N}} verified workers waiting.

Post free here → {{deep_link_post_job}}

Reply with any question — I'll help you set up.
\`\`\`

## Employer — post-fill NPS (Utility, English)

\`\`\`
{{1}}, did {{worker_name}} show up and complete the work?

Reply with one number:
5 — perfect
4 — good
3 — okay
2 — issues
1 — didn't show up

Anything else you'd want to fix?
\`\`\`

## Employer — D30 repeat-post nudge (Marketing, English)

\`\`\`
{{1}}, you hired {{worker_name}} 30 days ago.

Need to hire again? Post free → {{deep_link_post_job}}

Or refer another household — both you and they get ₹50.
\`\`\`

## RWA admin — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

I noticed you admin the {{society}} community. 

We help apartment residents hire maids and cooks directly — phone-verified, no agency commission, hyper-local.

Active workers near {{pincode}}: {{N_workers}}.

Open to a 15-min meet this week?

(Not selling anything — we're free for residents and for you.)

— {{founder_name}} · DutyPe
\`\`\`

## RWA admin — co-branded broadcast (Utility, English + Telugu)

\`\`\`
Dear {{society}} residents,

Hiring a maid, cook, helper or driver?

We've partnered with DutyPe — a free hyper-local hiring app, phone-verified, no agency commission.

Telugu support. 5 km radius from our society.

Install free → {{deep_link_society_attribution}}

— {{rwa_admin_name}}, {{society}} RWA
Powered by DutyPe (KGPV Innovation Solutions Pvt Ltd)
\`\`\`

## PG owner — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

Saw you run {{pg_name}} on {{road / area}}. 

We help PG / co-living owners hire cleaning + cooking staff directly — phone-verified, ₹0 commission, replaceable in 24h if someone quits.

Active workers in {{pincode}}: {{N_workers}}.

Want me to walk you through it in 5 minutes? Or post a free trial role here → {{deep_link}}.

— {{founder_name}} · DutyPe
\`\`\`

## NGO partner — first contact (Marketing, English)

\`\`\`
Hi {{1}}, I'm {{founder_name}} from DutyPe.

I read about your work with {{cohort_description}} in {{area}}. 

We're a hyper-local hiring app for blue-collar work in Hyderabad — Telugu, phone-verified, ₹0 commission, women-first.

Would your cohort women benefit from a 1-day onboarding camp? We bring the install team and we cover refreshments + a ₹100 sign-up incentive per attendee.

Open to a 30-min meet to discuss?

— {{founder_name}} · DutyPe
\`\`\`

## Reactivation — lapsed employer 60 days (Marketing, English)

\`\`\`
{{1}}, it's been 60 days since you last posted on DutyPe.

Your area ({{pincode}}) has grown — now {{N_workers}} active workers (up from {{N_old}}).

Post free → {{deep_link_post_job}}

If something went wrong last time, reply and I'll personally fix it.

— {{founder_name}}
\`\`\`

## Send rules

- All marketing templates only sent **with prior consent** (account creation = consent for own-account messaging; partner WhatsApp lists need explicit opt-in or partner is the sender).
- Never include short URLs (Meta penalises bit.ly).
- Always include opt-out line for marketing.
- Never send between 22:00–08:00 IST.
- Cap: 2 marketing messages per user per week.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
