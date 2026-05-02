# Instant Help Production Deploy

Use this after the Android, web, and Functions builds pass.

## Scope

This deploy publishes the instant-help backend behavior:

- Cloud Functions for urgent worker notification fanout.
- Cloud Functions for instant response metrics sync.
- Scheduled expiry for stale urgent requests.
- Firestore rules for urgent lifecycle, no-show, cancellation, completion proof, and ratings.
- Firestore index for expiring open urgent requests.

## Verify First

```powershell
Push-Location functions
npm run build
Pop-Location

Push-Location web
npm run type-check
Pop-Location

.\gradlew.bat :app:assembleDebug
```

## Deploy

```powershell
firebase deploy --only functions,firestore:rules,firestore:indexes
```

## Post-Deploy Smoke Check

1. Employer posts one urgent request.
2. A nearby worker with Available now turned on receives the urgent notification.
3. Worker taps Interested or Call.
4. Employer sees the response in urgent detail and history.
5. Employer selects worker, marks complete, and submits rating.
6. Admin opens `/admin/instant-help` and checks response count, fill rate, expiry count, and time to first response.

Do not run this deploy from automation without confirming the target Firebase project first.
