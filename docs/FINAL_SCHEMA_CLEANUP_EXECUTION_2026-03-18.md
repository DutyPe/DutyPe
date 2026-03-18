# Firestore Strict Cleanup Execution (2026-03-18)

## BRUTAL TRUTH

Current state before this pass:
- Users/jobs/application paths still had legacy fields and toggles in critical runtime services.
- Job state was mixed (`isActive`/`isFilled` + `status`).
- Job creation payloads still emitted legacy fields from some employer flows.
- Schema direction was clear, but runtime still had drift points.

If left unchanged at 5L+ users, read cost, index churn, and logic inconsistency would continue to increase.

## What Was Fixed In This Pass

### 1) UserMetadata strict cleanup
- Removed legacy employer stats logic using `isActive`/`isFilled`.
- Uses `status == "open"` for active jobs.
- Filled jobs now derived from max of:
  - closed jobs from `jobs.status`, and
  - distinct hired job IDs from `applications.status == "hired"`.
- Added robust timestamp parsing helper for Firestore `Timestamp`/`Long` compatibility.

### 2) Employer job toggle strict cleanup
- Replaced job toggle write from legacy `isActive` to strict `status` (`open`/`closed`).

### 3) Strict employer job creation payloads
- Voice posting flow now writes strict payload keys:
  - `salary`, `salaryType`, `jobType`, `location {lat,lng}`, `status`, `createdAt`.
- Post job screen payload now writes strict keys and removes legacy state flags from payload.

### 4) JobFirestoreService strict query + mapping cleanup
- Replaced runtime filters based on `isActive`/`isFilled` with strict `status == "open"`.
- Updated category routing to strict `jobType` query filtering.
- Updated salary filtering to use strict `salary`/`salaryType` with safe parsing.
- Hardened timestamp handling for `Timestamp` and numeric millis.
- Preserved response compatibility fields (`payAmount`, `payType`, `category`) to avoid immediate UI breakage while migrating.

### 5) Web + admin strict applications cutover
- Replaced active web source reads/writes from legacy `job_applications` to strict `applications`.
- Updated worker and employer product flows, employer review flows, admin dashboard, and admin applications API route.
- Updated work-verification uniqueness checks to query `applications`.
- Added `createdAt` on web application creation for strict ordering.

## Final Architecture Target (Clean)

Allowed runtime collections:
- `users`
- `worker_profiles`
- `employer_profiles`
- `jobs`
- `job_details`
- `applications`
- `saved_jobs`
- `ratings`
- `referrals`
- `notifications`

Everything else should be backend/admin/analytics only.

## Done vs Not Done

### Done now
- `UserMetadata` strict employer stats cleanup complete.
- Employer status toggle moved to strict `status` writes.
- Main employer posting payloads moved to strict core schema.
- `JobFirestoreService` critical runtime filters and mappings moved to strict status + jobType + salary logic.
- Android app runtime no longer references `job_applications` collection.
- Active web/admin source no longer references `job_applications` collection.

### Still pending for full zero-drift end state
- Some non-job domains still intentionally use `isActive` (example: announcements), which is not job-schema drift but should be documented as domain-specific.
- Final hard-delete of legacy docs/fields in production data depends on migration rollout completion.

## Geo + Matching Architecture (Operational)

Current practical flow:
1. Query open jobs using strict schema and indexed fields.
2. Compute exact distance client-side.
3. Keep jobs within 10 km.
4. Fallback to 15 km if no results.
5. Cap result set to 30 for UX and read-cost control.

Required indexes:
- jobs: `geohash ASC`, `jobType ASC`, `status ASC`, `createdAt DESC`
- jobs: `geohash ASC`, `status ASC`, `createdAt DESC`
- applications: `workerId ASC`, `createdAt DESC`
- applications: `jobId ASC`, `createdAt DESC`
- saved_jobs: `userId ASC`, `createdAt DESC`

## Worker History + Saved Jobs (Correct Pattern)

### Worker history
- Source of truth: `applications`
- Query: `where workerId == currentUserId`, ordered by `createdAt DESC`.

### Saved jobs
- Source of truth: `saved_jobs`
- Schema: `{ userId, jobId, createdAt }`
- Never store large saved-jobs arrays on `users`.

## Migration Strategy (Safe at Scale)

### Phase 1: Dual-write (short window)
- Write strict fields now.
- Keep compatibility reads where unavoidable.

### Phase 2: Backfill
- Migrate legacy to strict:
  - `phoneNumber -> phone`
  - `name -> fullName`
  - `geoHash -> geohash`
  - `payAmount -> salary`
  - `payType -> salaryType`
  - legacy application docs -> `applications`

### Phase 3: Read cutover
- App/web reads strict fields only.
- Remove fallback logic in runtime paths.

### Phase 4: Hard cleanup
- Remove legacy fields and legacy collections from production data.
- Freeze rules and indexes to strict schema only.

## Final Verdict

- Runtime source cleanup target is now complete for Android + active web/admin paths for the `applications` collection cutover.
- Full production completion still requires migration execution (backfill + strict-only reads + hard cleanup of legacy stored data).
- Code-path drift is now low; data-plane migration remains the final blocker to declare absolute end-state completion.
