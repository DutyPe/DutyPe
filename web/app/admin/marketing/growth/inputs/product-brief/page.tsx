import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Product Brief (input file)

> Pre-filled from codebase recon on 19 Apr 2026. Replace \`[TBD]\` rows with founder truth before reusing in outbound.

## What it is
- Hyper-local, two-sided **blue-collar job marketplace** for India.
- Direct worker ↔ employer connection with no middleman fees.
- Available as **Android app** (\`com.dutype.app\`, v2.6.1) + Next.js website at \`https://dutype.in\`.
- Languages live: **English, Telugu** (\`res/values-te/\`).

## What it does
- Workers find local jobs within 1 / 5 / 10 km radius.
- Employers post jobs in 17 categories: COOK, MAID, DRIVER, HELPER, SECURITY, GARDENER, CARETAKER, DELIVERY, WAITER, ELECTRICIAN, PLUMBER, PAINTER, CARPENTER, RECEPTIONIST, CASHIER, PACKER, OTHER.
- Pay types: DAILY, HOURLY, MONTHLY, TASK.
- Trust stack: phone OTP signup, structured (not free-text) job titles, scam-keyword filter, fraud-score auto-reject ≥70, employer rate-limit (2/hr free / 10/hr paid), QR code work-start verification.

## Who it serves
- **Workers**: blue-collar, semi-formal, ages ~18–45, smartphone-first, often Hindi/Telugu/local-language preference.
- **Employers**: small / micro businesses, households, contractors. NOT large staffing agencies.

## Current monetization
- In-app Google ads (AdMob).
- Ad-gated "unlock employer contact" for workers.
- **No** Razorpay / payment gateway integrated. **No** subscription / premium tier yet.
- Referral program: ₹25 per successful referral; tier bonuses 5/10/25/50/100; employers unlock free job posts at tiers.

## Stage
- Production app on Play Store; live web; backend on Firebase + 16+ Cloud Functions.
- Traction numbers: **[TBD founder must fill]** (installs, MAU, posted jobs/week, applications/week, fill rate, top cities by user count).

## Founder truth needed
- City coverage today (top 5 cities by active workers + by active employers).
- Current # active workers and active employers (last 30 days).
- Job-post → first-application time (median).
- Job-post → fill rate (% jobs that result in a hire, however measured).
- Worker D7 / D30 retention.
- Employer repeat-post rate.
- Months of runway, team size, marketing budget per month.
- Current acquisition channels actually used (paid ads? offline? referrals only?).
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
