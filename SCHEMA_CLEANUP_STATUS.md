# Schema Cleanup — Session Status

Canonical schema now lives in [firestore.rules](firestore.rules). No backward-compat shims.

## What shipped this session

### Rules (deployed as authoritative schema)
- `firestore.rules` strict `hasOnly`/`hasAll` validation across every allowed collection.
- `applications.status` vocabulary locked to `applied | shortlisted | rejected | hired`.
- `saved_jobs` locked to `{ userId, jobId, createdAt }`.
- `chat / conversations / messages / work_verifications` fall through to the default deny-all.

### Client (Android, `app/src/main/java/com/example/dutype`)
- [models/User.kt](app/src/main/java/com/example/dutype/models/User.kt) — dropped `email`, `companyName`, `isVerified`, `isActive`.
- [services/firestore/UserFirestoreService.kt](app/src/main/java/com/example/dutype/services/firestore/UserFirestoreService.kt) — user write map no longer sends `isVerified` / `isActive`.
- [services/firestore/JobFirestoreService.kt](app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt) — `createJob` writes 11-field details doc; `updateJob` no longer mirrors card fields.
- [services/firestore/ApplicationFirestoreService.kt](app/src/main/java/com/example/dutype/services/firestore/ApplicationFirestoreService.kt) — `saveJob` writes only `userId/jobId/createdAt`; `getSavedJobs` no longer merges legacy `workerId` docs.
- [models/JobApplicationModels.kt](app/src/main/java/com/example/dutype/models/JobApplicationModels.kt) — `ApplicationStatus.toFirestoreValue()` now strictly emits `applied | shortlisted | rejected | hired` (UI-only `COMPLETED/WITHDRAWN/UNDER_REVIEW/ACCEPTED` states map onto canonical values, they never hit the wire).
- [metadata/UserMetadata.kt](app/src/main/java/com/example/dutype/metadata/UserMetadata.kt) — allowed set updated; strict doc shape aligned.
- Dead code purged: `WorkVerificationModels.kt`, `WorkVerificationService.kt`, `WorkVerificationViewModel.kt`, `WorkStartQRScreen.kt`, `EmployerVerifyWorkScreen.kt`, plus every DI/nav/UI reference (AppModule, JobApplicationService, ApplicationManagementService, Routes, WorkerNavGraph, EmployerNavGraph, EmployerMainScreen, MyJobsScreen).
- `./gradlew :app:compileDebugKotlin` is green.

### Ops
- [scripts/migrate-saved-jobs-canonical.js](scripts/migrate-saved-jobs-canonical.js) — dry-run/commit migration to drop legacy fields (`id`, `workerId`, snapshots…) from existing `saved_jobs` docs.

## Remaining ops tasks (require console/CLI access)

1. **Run the saved_jobs migration**
   ```
   cd scripts
   node migrate-saved-jobs-canonical.js            # preview
   node migrate-saved-jobs-canonical.js --commit   # apply
   ```

2. **Enable Firestore TTL on `notifications.createdAt`**
   - Console → Firestore → TTL → *Create policy* on `notifications.createdAt` with desired retention (e.g. 45d).
   - Or via gcloud:
     ```
     gcloud firestore fields ttls update createdAt \
       --collection-group=notifications --enable-ttl
     ```

3. **Index review** — [firestore.indexes.json](firestore.indexes.json)
   - Audit composite indexes for removed fields (`isVerified`, `isActive`, `workerId` on saved_jobs, etc.) and delete unused entries before the next `firebase deploy --only firestore:indexes`.

4. **Cloud Functions (denormalized reward fields)** — `functions/src/referral-system.ts`
   - CF runs with admin privileges, so `rewardAmount / bonusAmount / referredUserReward / referredUserName / referredUserRole` written on `referrals/*` docs bypass rules. They are denormalized display data, not a security concern. Only trim when the dashboard/history read paths are refactored to read from `referral_stats` aggregates.

## Enforcement model

Client models still contain display-only runtime fields (worker enrichment on `JobApplication`, denorm fields on `Referral/NotificationData/Announcement`). That is intentional:
- Writes go through strict `toFirestoreMap()` helpers or explicit field maps.
- Reads use `@IgnoreExtraProperties` so any stale fields on legacy docs are silently dropped.
- Firestore rules are the hard boundary — any non-canonical write is rejected server-side.
