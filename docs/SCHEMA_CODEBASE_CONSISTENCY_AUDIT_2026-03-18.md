# Schema vs Codebase Consistency Audit (2026-03-18)

## Scope
- Live Firestore schema snapshot (admin credentials)
- Android runtime (`app/src/main/java`)
- Cloud Functions runtime (`functions/src`)
- Web runtime (`web`)

## Live Collections (Current)
Snapshot source: `docs/LIVE_SCHEMA_AUDIT_2026-03-18.json`

1. `jobs` (441 docs)
- Strict fields present: `employerId`, `title`, `jobType`, `salary`, `salaryType`, `location.lat`, `location.lng`, `geohash`, `status`, `createdAt`, `expiresAt`
- Legacy fields still present in many docs: `isActive`, `isFilled`, `payAmount`, `payType`, `latitude`, `longitude`, etc.

2. `users` (79 docs)
- Active role model fields present: `roles`, `activeRole`, `phone`, `fullName`
- Legacy/alternate profile fields still present: flat `latitude`/`longitude`, `role`, etc.

3. `applications` (35 docs)
- Strict app fields present: `applicationId`, `jobId`, `workerId`, `employerId`, `status`, `createdAt`, `updatedAt`

4. `job_applications` (37 docs)
- Legacy collection still present (contains historical docs)

5. Other collections observed
- `notifications` (35), `referral_codes` (61), `referral_events` (10), `fcm_tokens` (6), `announcements` (5), `ratings` (4), `metadata` (3), `phone_roles` (2), `job_reports` (1)

## Mismatches Found
1. Collection split: `applications` vs `job_applications`
- Runtime targets strict `applications`, but live still had active data in `job_applications`.

2. Cloud Functions duplicate/rate logic used legacy job fields
- `functions/src/index.ts` depended on `jobs.postedAt` and `jobs.latitude/longitude` in parts of duplicate/rate checks.
- Strict jobs write path uses `createdAt` and canonical `location.{lat,lng}`.

3. Legacy data remains in live documents
- `jobs` includes many old fields beyond strict schema.
- `users` still includes legacy flat location and old aliases.

## Updates Applied
1. Migrated legacy applications into strict collection
- Added and ran: `scripts/migrate-job-applications-to-applications.js`
- Result: 37 legacy docs processed, 35 unique docs upserted into `applications`.

2. Android runtime fallback safety
- Updated: `app/src/main/java/com/example/dutype/services/firestore/ApplicationFirestoreService.kt`
- Behavior: reads `applications` first, falls back to `job_applications` only if needed.

3. Cloud Functions schema alignment
- Updated: `functions/src/index.ts`
- Replaced `postedAt` window queries with `createdAt` timestamp queries.
- Duplicate-location logic now supports canonical `location.lat/lng` in addition to legacy `latitude/longitude`.

4. Verification script schema alignment
- Updated: `scripts/verify-final-schema.js`
- `applications` is treated as expected; `job_applications` moved to deprecated list.

5. Live schema snapshot refreshed
- `docs/LIVE_SCHEMA_AUDIT_2026-03-18.json` regenerated after migration.

## Build Validation
1. Android: `:app:compileDebugKotlin` -> SUCCESS
2. Functions: `npm run build` (`tsc`) -> SUCCESS

## Remaining Optional Cleanup (Data)
These are data hygiene steps, not runtime blockers:

1. Decommission legacy `job_applications`
- After confidence window, archive/delete or mark as legacy-only.

2. Normalize legacy fields from `jobs`
- Keep strict canonical set for new reads/writes.
- Legacy aliases can remain for transitional compatibility.

3. Normalize `users` location shape (optional)
- Move fully to canonical map + geohash strategy if desired.

## Net Status
- Runtime now aligns with strict schema usage and is resilient to remaining legacy data.
- Critical collection mismatch (`job_applications` vs `applications`) has been addressed with migration + fallback.
