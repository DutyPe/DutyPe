import { MarkdownView } from "@/components/marketing-shell";

export default function Page() {
  const content = `# DutyPe – Feature Inventory (input file)

> Pulled from codebase. This is what we can *truthfully* claim in marketing. Anything not listed here = do not claim.

## Worker side
- Onboarding: language pick → phone OTP → role pick → profile (skills, location, experience).
- Job feed: distance-sorted (1/5/10 km), filter by category, pay type, urgency.
- Apply in 1 tap once profile ≥80% complete.
- "My Jobs" / Applied / Saved tracking.
- QR scan to mark work start + work complete.
- Earnings dashboard.
- Refer & Earn: ₹25/referral + tier bonuses, withdraw at ₹50 minimum.
- Notifications (FCM + local WorkManager): new nearby jobs, application status, smart re-engagement (time-of-day tagged).
- Digital visiting card (shareable profile).
- WhatsApp support (role-aware message: "I am a worker on DutyPe…").

## Employer side
- Onboarding: phone OTP → role → company profile → 80% completion gate.
- Post Job (4-step wizard): Role → Pay & Place → People → Review.
- Anti-fraud at post time: structured titles only, scam-keyword warnings, pay-rate suggested range.
- Rate limit: 2 posts/hr, 5/day (free); 10/hr, 50/day (paid).
- View applications, shortlist, contact applicant directly.
- QR generation to verify work start/finish + rate worker.
- Refer & Earn: tiered free posts unlocked at 5/10/25 referrals.
- Analytics (basic): views, applications per job.
- WhatsApp support (role-aware: "I am an employer on DutyPe…").

## Trust / safety we can talk about
- Phone-verified accounts on both sides.
- Scam keyword detection at job-post time.
- Fraud score (server-side) auto-rejects bad postings.
- Profile completion gating before posting/applying.
- QR-based work-start so neither side can dispute attendance.

## Things NOT yet built (don't claim)
- No background check / police verification of workers.
- No insurance coverage.
- No escrow / in-app payment to worker.
- No staffing-agency / recruiter dashboard.
- No iOS app.
- Languages beyond English & Telugu not live.
`;

  return (
    <div className="marketing-page-single">
      <MarkdownView source={content} />
    </div>
  );
}
