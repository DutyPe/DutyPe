# Android Data and Client Migration

Status: local rehearsal completed on 2026-09-16. **No remote staging project was supplied and no production data, deployment, Play release, or real payment was changed.** Do not interpret local synthetic repairs as reconciliation of existing customer records.

## Required Approvals

Before remote execution, identify the staging project, the release owner, the data reviewer, the Android/website client owners, and the maintenance-window owner. Confirm an export/backup and a recovery procedure before granting the operator write access. Use normal local ADC authentication; do not put keys or credentials in scripts, reports, or chat.

The CLI deliberately refuses the production project found in [app/google-services.json](../app/google-services.json). Production execution is a separate, explicitly approved operation after staging evidence is reviewed. Do not bypass that guard just to reuse a command.

## Tools and Guarantees

- [scripts/android-data-migration.cjs](../scripts/android-data-migration.cjs) defaults to the fixed local demo project. It has only explicit `plan` and `apply` modes and refuses overwriting an output file.
- `plan` reads one bounded page, writes a local review report, and writes nothing to Firestore. Pages default to 50 and are capped at 100. Follow `nextCursor` using `--after`; do not assume one page covers the collection.
- `apply` requires the saved plan, its displayed `reviewSha`, the exact project confirmation, and `--maintenance-ack`. The acknowledgement is an operator assertion, **not an automatic writer freeze**.
- Each item is re-evaluated in a Firestore transaction. Changed source versions or a changed proposed patch reject the stale plan. Stop, review what already applied, and generate a fresh plan; never edit a plan to force it through.
- Apply is atomic per item, not for an entire page/collection. New-only JSONL output is flushed after each completed item. Server-only `migration_audit` receipts retain before/after fields and the input digest. A failure can leave earlier reviewed items applied; use the receipts and a new dry run to resume.
- User reconciliation copies validated authoritative `referral_stats` fields to the user-facing mirror and derives withdrawal eligibility from the referral milestone. It **never credits/debits/reconstructs the authoritative financial ledger**, changes payout records, or clears a conflicting fraud block.
- Rating summaries are recomputed only from reviews with completed applications, matching jobs/participants, existing raters, valid roles/stars, and no duplicate application/rater pairs. Invalid or duplicate records block that user; they are not silently deleted or chosen as winners. Application completion and legacy review provenance still need human fraud review.
- Jobs count accepted, in-progress, and completed applications as occupied. Closed/expired vacancy states are preserved. Invalid participants/statuses, duplicate hires, invalid vacancies, or overbooking block the repair.
- More than 200 ratings or 500 applications for an item blocks automatic reconciliation. These are explicit safety ceilings, not silent truncation. Handle larger records with a separately reviewed plan.
- Public-profile backfill uses the existing [allowlist](../functions/src/profiles.ts). A second pass over `public_profiles` removes orphan/disabled-user projections and obsolete fields. It never modifies private source profiles. Public text fields may themselves contain user-entered sensitive text; the projection is not a content-redaction service.

Migration reports contain user/document IDs, aggregate values, and issue references. Audit receipts can contain previously exposed profile fields. Store both with restricted access and a deliberate retention period. The example output directory is ignored build output and is not durable storage.

## Local Rehearsal

Install the locked backend dependencies, build, and start the full emulator stack from the workspace root:

```powershell
npm ci --prefix functions --offline --ignore-scripts --no-audit --no-fund
npm run build --prefix functions
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
firebase emulators:start --config firebase.local-staging.json --project demo-dutype-android-fixes --only 'auth,firestore,functions,storage' --non-interactive
```

The [root staging configuration](../firebase.local-staging.json) must load the real rules. A missing-rules warning or a permissive fallback is a failed gate. Stop rather than testing an allow-all database. Auth is on 9199, Firestore 8185, Functions 5101, Storage 9299, and the hub 4401; all bind to loopback.

In a separate terminal:

```powershell
node scripts/android-local-staging.cjs seed
node scripts/android-data-migration.cjs plan --kind users --after device- --page-size 2 --out app/build/reports/migration/run-01/users-plan.json
node scripts/android-data-migration.cjs plan --kind jobs --after device- --page-size 1 --out app/build/reports/migration/run-01/jobs-plan.json
```

Inspect each saved report. An example local apply after review is:

```powershell
node scripts/android-data-migration.cjs apply --plan app/build/reports/migration/run-01/users-plan.json --review-sha REVIEWED_SHA --confirm-project demo-dutype-android-fixes --maintenance-ack --out app/build/reports/migration/run-01/users-applied.jsonl
```

Replace `REVIEWED_SHA` with that report's actual displayed hash; it is not a bypass token. Apply the reviewed jobs page separately. Then generate fresh profile plans, because reconciliation changed user source versions:

```powershell
node scripts/android-data-migration.cjs plan --kind profiles --out app/build/reports/migration/run-01/profiles-plan.json
node scripts/android-data-migration.cjs plan --kind orphan-profiles --out app/build/reports/migration/run-01/orphans-plan.json
```

Apply each reviewed page with the same confirmation pattern. Run another dry pass and require zero unexpected changes/issues before proceeding. The live sync trigger may make backfill items `unchanged`; that is a verified no-op, not evidence that creation paths were untested. Creation, replacement, deletion, paging, and stale-plan rejection have separate emulator regression tests.

Remote staging uses the same CLI with **all** of `--environment staging --project STAGING_ID --confirm-staging STAGING_ID`. Apply also requires `--confirm-project STAGING_ID`, the reviewed SHA, and maintenance acknowledgement. Unset `FIRESTORE_EMULATOR_HOST` before a remote staging command. No remote commands were run during this task.

## Older-Client Cutover

1. **Inventory and decide the window.** Record the highest Play version code and active website/Android protocol versions. Current source remains versionCode 38; assign a higher release code only after checking Play Console. The `.staging` APK is debug-signed, has a separate application ID, and is not a release candidate.
2. **Prepare compatible clients first.** New Android calls `submitRating`, `requestNotification`, `getPublicProfile`, and `getApplicationContact`; withdrawal calls require a persistent `requestId`. Complete the equivalent website/client migration separately. Website source was not changed in this task.
3. **Freeze actual writers.** Deny/disable legacy financial, rating, job, and notification writers at the server/rules boundary, including old Admin SDK functions and scheduled jobs, before reconciliation. Stop competing new writers during each reviewed apply window as well. Existing Android code cannot reliably be retrofitted with a force-update check it never fetched; a Play immediate-update prompt is guidance, not authorization.
4. **Backup and rehearse staging.** Use an approved sanitized data copy, reconcile every user/job page, resolve blocked items with a reviewer, and backfill both profile passes. Do not initialize missing balances from the user mirror or infer that old fabricated ratings are legitimate simply because they parse.
5. **Deploy staged prerequisites.** Provision legitimate admin custom claims. Deploy the new indexes and wait for READY; the emulator does not enforce production index availability. Stage the compatible Functions build, projection triggers, notification senders, and strict rules while writers are controlled.
6. **Prove both sides of compatibility.** New clients must read their own private data, read other public profiles, query participant applications, submit/retry ratings and withdrawals once, and receive account-bound data-only notifications. Old unauthenticated user queries, unrelated application reads, direct review/notification writes, and withdrawal requests without IDs must fail without corrupting data. Denied old calls are expected; error handling/update UX on an actual old signed APK remains a staging gate.
7. **Release and monitor.** Roll out the higher-version signed Android build and the coordinated website release. Monitor permission errors, rejected protocol requests, duplicate-event rate, authoritative/mirror drift, migration blocked counts, notification errors, and withdrawal queue totals. Reopen writers only after the full checklist passes.

Do not roll back to permissive privacy rules or client-owned balances to keep an old client working. On failure, pause affected writers, preserve receipts and logs, and restore only reviewed data under a matching-version/precondition check. There is no automatic rollback command; audit receipts support a separately approved recovery operation and must not overwrite legitimate post-migration writes.

## Device and Staging Checks

The [staging variant](../app/build.gradle.kts) uses `com.dutype.app.staging`, the demo Firebase project, and a provider that configures emulator endpoints before app injection. Analytics/Crashlytics/performance and FCM auto-init are disabled for this build. The normal debug/release configuration is not switched to emulators. A synthetic format-valid API key is intentionally checked into staging resources; it is not a production credential.

Build the app and test APKs:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr" -PlocalStagingTests=true :app:assembleStaging :app:assembleStagingAndroidTest --offline --no-parallel --max-workers=1 --console=plain
```

Use a dedicated empty AVD only. The rehearsal created `DutyPe_Staging_20260916`, API 36 Google APIs x86_64, port 5580; it did not use the two existing AVDs or a personal phone. Verify `adb -s emulator-5580 emu avd name` before any install/input/capture:

```powershell
adb -s emulator-5580 install -r app/build/outputs/apk/staging/app-universal-staging.apk
adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/staging/app-staging-androidTest.apk
adb -s emulator-5580 shell am instrument -w -r -e class com.example.dutype.LocalStagingTest com.dutype.app.staging.test/androidx.test.runner.AndroidJUnitRunner
node scripts/android-local-staging.cjs verify
```

Require four explicit passing test statuses and `OK (4 tests)`. ADB exit zero or `INSTRUMENTATION_CODE: 0` is not a test pass. The fixture commands target only the fixed demo project; do not use the seed command to reseed after device withdrawals without first starting a clean demo dataset, since it intentionally initializes ledger values for the rehearsal.

### Verified Results

- Final standalone regression run: **52/52** security and migration tests passed; backend typecheck/build passed. Normal Android JVM tests **23/23**, debug APK, release Kotlin/resources, and instrumentation-source compilation passed. Staging app/test APKs built and staging lint passed with **0 errors, 557 warnings, 57 informational findings**. Normal build and staging lint were run serially as separate gates.
- Reviewed local plans repaired two synthetic profile mirrors from 999 to the authoritative 500, and a synthetic job's accepted count from 9 to 1. Authoritative balances were unchanged by reconciliation; receipts were recorded.
- Public projections were verified free of private phone/token fields. A live sync trigger had already generated the device fixtures; dedicated backfill tests also exercised actual creation/replacement, orphan deletion, resume cursors, and no-op reapplication.
- Four instrumented tests passed on the disposable Android emulator through the real Auth/Functions HTTP emulator middleware, not just direct handler calls: guest launch; public read/private denial; participant contacts plus idempotent withdrawal and post-signout denial; idempotent server ratings plus rejected legacy notification writes.
- Independent database verification found one withdrawal with one 100-unit deduction (500 to 400), one rating with one aggregate update, and the reconciled job count. Repeated device tests used the same request/review IDs and did not duplicate them.
- The language-selection screen was visually checked. A System UI ANR dialog from the emulator first obscured the screen; dismissing it on the disposable AVD revealed the correctly rendered app. A Bluetooth system-process crash also occurred during boot. These environment failures are retained as caveats, not hidden as app passes.
- Real device testing exposed a Functions-emulator legacy `admin.firestore.FieldValue` compatibility failure in withdrawal. The handler now uses the supported modular `FieldValue` import; the same failing device test and then all four tests passed.
- Evidence resides under ignored [app/build/reports/migration/device-staging](../app/build/reports/migration/device-staging/instrumentation-verified-output.txt): plans, apply JSONL, screenshots, and a Firebase emulator export. The initial failed tests are also retained. Promote required evidence into approved durable storage before cleaning build outputs.

### Still Blocked / Not Claimed

- Actual customer-data reconciliation or public-profile backfill: no approved remote target, backup, or reviewed real-data plan was supplied. The CLI refuses the known production project.
- Remote Firebase staging deployment, indexes READY, admin claims, old signed-client upgrade UX, release signing/R8, Play rollout, and website migration: not executed.
- Auth used synthetic emulator email/password accounts. Real SMS OTP, Play Integrity/App Check enforcement, FCM delivery, payment settlement, physical-device/OEM testing, and Android 7/other API-level behavior were not verified.
- Functions ran under host Node 24 because Node 20 was not installed for the CLI; deployed runtime compatibility still requires the intended Node version in remote staging.
- The local AVD and Firebase services are stopped after the final checks. The dedicated AVD remains available for reruns; its files are under ignored app build output, so a clean build may remove them. No existing AVD was modified.