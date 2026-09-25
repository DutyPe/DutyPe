# DutyPe Project Rules & Guidelines

## Firebase Deployment Rules
- **Active Project ID**: Always `dutype-860ac`.
- **DO NOT** run `firebase projects:list`, `firebase use`, or any project inspection command prior to deployment. The default project is permanently pinned in `.firebaserc`.
- **Deployment Commands**:
  - Deploy single function: `firebase deploy --only functions:<functionName>`
  - Deploy multiple functions (always quote comma-separated list in PowerShell): `firebase deploy --only "functions:fn1,functions:fn2"`
  - Deploy firestore rules: `firebase deploy --only firestore:rules`
  - Deploy firestore indexes: `firebase deploy --only firestore:indexes`
  - Deploy webapp (hosting): `firebase deploy --only hosting`

## Architecture Pointers
- Android app package: `com.example.dutype`
- Functions source: `functions/` (TypeScript, built with `npm run build` to `functions/lib/`)
- Webapp source: `web/` (Next.js 14 App Router, built with `npm run build`)
- Firestore Collections:
  - `users`: Authoritative user records and `referralStats` map
  - `referral_stats`: Real-time referral earnings, balances, and subcollection `withdrawals`
  - `referrals`: Individual referral documents tracking who referred whom
  - `withdrawal_requests`: Global withdrawal collection
  - `jobs`, `jobmetadata`, `job_details`: Standard job listings
  - `instant_requests`: Urgent/instant hiring requests
  - `worker_profiles`: Worker profile information including coordinates and `geohash`
