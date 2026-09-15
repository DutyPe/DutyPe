# Kotlin Android App Review

Date: 2026-09-15. Scope: the Kotlin Android app and the Firebase rules/functions that enforce its data access. The website was excluded. This is a targeted, flow-based review, not a certification that every line or dependency is safe.

## Migration and Device Rehearsal

Follow-up on 2026-09-16: the requested reconciliation/backfill and device checks were **prepared and rehearsed locally**, not run against customer data. No remote staging ID, backup approval, or reviewed real-data plan was available. The complete operator commands, older-client cutover order, safety boundaries, and outstanding approvals are in [docs/ANDROID_MIGRATION_RUNBOOK.md](ANDROID_MIGRATION_RUNBOOK.md).

- Added a bounded, resumable, dry-run-first migration command. Apply requires the reviewed plan hash, exact target, maintenance acknowledgement, and unchanged source versions. Audited repairs never alter authoritative balances; invalid/duplicate records, fraud conflicts, and overbooked jobs block repair.
- Rehearsed the real CLI with saved plans and per-item receipts. Two synthetic profile mirrors changed from 999 to the authoritative 500; job capacity changed from 9 to 1. Existing private fields and authoritative ledger values were preserved. Public-profile create/replace/delete/resume/idempotence were exercised in regression tests; device fixture projections were also verified under the live sync trigger.
- Added a separate `com.dutype.app.staging` APK using a demo-only Firebase configuration and emulator endpoints. Four instrumented tests passed on a newly created API 36 AVD through real Auth/Functions emulator HTTP handling: startup, privacy rules, contact authorization, withdrawal idempotence/post-logout denial, rating idempotence, and rejection of legacy direct notification writes.
- Independently verified one withdrawal/debit and one review/aggregate after repeated device calls. Captured the rendered language-selection screen, and exported local Firestore/Auth/Storage fixtures. A System UI ANR dialog and Bluetooth boot crash occurred in the disposable emulator; they are recorded in the runbook as environment caveats.
- Device testing exposed a Functions emulator interop failure in the withdrawal timestamp call. The handler now imports modular Firestore `FieldValue`; the exact failing test and the complete four-test class passed after the fix.
- Final checks: **52 security/migration tests passed**, **23 JVM tests passed**, backend typecheck/build passed, normal debug APK and release Kotlin/resources compiled, staging app/test APKs built, and staging lint passed with **0 errors, 557 warnings, 57 informational findings**. The original debug lint count below is from the previous gate and is not the staging warning count.
- Local evidence is preserved in ignored [device instrumentation output](../app/build/reports/migration/device-staging/instrumentation-verified-output.txt), [staging screenshot](../app/build/reports/migration/device-staging/staging-visible-screen.png), [security regression output](../app/build/reports/migration/security-regression-output.txt), and [staging lint report](../app/build/reports/lint-results-staging.html). No live FCM delivery, SMS OTP, settlement, Play release, signed old-client upgrade, or remote index readiness was tested.
- Production and remote-staging execution remain blocked on target/backup/data-review approvals and client coordination. The migration tool refuses the known production project; the runbook does not authorize weakening that guard. The dedicated AVD and local Firebase services were shut down after validation.

## Remaining Work Plan

Requested 2026-09-16. Work is local to Android and its shared backend; no deployment, production-data mutation, website-source edits, or TLS-policy changes are authorized by this plan. The completion evidence below is required before marking each item fixed.

**Local implementation completed for steps 1-6.** The final evidence, coverage limits, and deployment gates are below. This does not mean the shared migration has been deployed or verified with live users.

1. **Restore backend validation.** Resolve the declared Firebase/TypeScript dependencies using the normal registry or verified local cache. Run full TypeScript checking, not only syntax transpilation. If network access remains blocked, report the exact package/host and keep this gate open.
2. **Atomic, retry-safe withdrawals.** Read eligibility, authoritative balance, daily usage, and a stable request ID inside a transaction; reserve funds and create the withdrawal atomically. Android must reuse the same request ID after uncertain failures. Tests must cover concurrent overspend, duplicate retries, different payloads using one ID, daily limits, blocked users, and no partial writes. Existing financial data needs reconciliation before rollout; a code change is not a repair of historical balances.
3. **Server-owned ratings.** Validate completed work, the actual participants, allowed role/direction, star range, and review size on the server. Deduplicate per application/rater and update protected aggregates in the same transaction. Tests must reject fabricated work/self-ratings/duplicates and prove concurrent aggregate updates retain both ratings.
4. **Private profile/application reads.** Define and project an explicit public-profile allowlist, migrate Android's other-user reads to it, and preserve owner-only private reads. Remove unauthenticated queries against private users and restrict applications to their participants. Tests must cover anonymous users, unrelated users, both participants, owner updates, and exposure of phone/address/token/payment fields. Public-profile backfill and website compatibility are deployment gates, not permission to edit website source.
5. **Authorized notifications.** Derive recipients and allowed content from authenticated business events on the server, then deny arbitrary client notification creation. Keep recipient-bound data-only push delivery. Tests must reject spoofed recipients/types/content and verify legitimate Android workflows still produce notifications exactly once where a stable event ID is available.
6. **Lint and integration.** Align the supported Android toolchain without disabling extra checks, fix meaningful resulting errors in scope, run all Kotlin/backend/rules tests, and rebuild debug and release compile/resources. Device, R8/signing, mixed-version staging, index readiness, admin claims, and deployment approval remain explicit external gates.
7. **Update this report.** Record exact passing/failing commands, remaining risks, schema/index prerequisites, migration order, and any test coverage limits. Do not describe the app as production-ready while a required gate remains open.

## Local Fix Status

Updated after the second implementation pass on 2026-09-16. Nothing was deployed, no real accounts or payments were exercised, and website source was not edited by this work. **This is tested local remediation, not a production-ready security sign-off.** The original review below is historical evidence; its original line anchors and defect-reproduction artifacts refer to the pre-fix source state.

| Finding | Current Local Status |
| --- | --- |
| 1. Private reads | Private users are owner/admin-only; applications and referral history are participant-scoped, and withdrawal history is owner/admin-only. Anonymous profile queries are denied. Android uses server-maintained `public_profiles` for browsing and the `getApplicationContact` callable for the other participant's phone/email. Contact access is allowed for pending/reviewed/accepted/in-progress/completed applications but denied after rejection/withdrawal or `active=false`; it is not an ad-payment entitlement check. Profile backfill and mixed-client rollout remain staging gates. |
| 2. Ownership/state forgery | Existing job/application ownership is immutable to clients. Applications must be created pending against the real job employer. Workers cannot self-verify/complete, and backward employer status transitions are rejected. Employer-controlled workflow integrity still needs authoritative server operations. |
| 3. Financial fields | Client balance, earnings, eligibility, and fraud-block mutations are denied on create/update; referral codes cannot be assigned, removed, or changed. Withdrawals now read server-only `referral_stats`, never the client-facing mirror as authority. Only the generation timestamp signal and non-increasing free-posting consumption remain client-writable within referral stats. Historical balance/reward fraud is not automatically repaired. |
| 4. Admin authorization | Replaced email matching with the trusted `admin: true` custom claim. Requires provisioning legitimate administrators before rollout. |
| 5. Forged notifications | Direct client creation is denied; recipients can only mark read or delete. `requestNotification` validates the stored event/participants and generates templates and deep links, ignoring client title/body/status. `notifyApplicationEvent` creates status notifications from the current stored application. Server-only `notification_events` receipts deduplicate retries even after notification deletion. This is not a guarantee of exactly-once FCM delivery. |
| 6. Missing started state | Added `IN_PROGRESS`, display/notification branches, and real Firebase mapper regression tests. |
| 7. Verification loss | Application mutations now update only intended fields; notes use `arrayUnion`. Status changes preserve verification and ownership fields rather than replacing the whole document. |
| 8. Logout privacy | Logout clears local login state, attempts token unlink before Firebase sign-out with a three-second bound, and clears displayed notifications. Token removal compares this device's token. Private pushes require the current locally logged-in UID to match `recipientId`; both server senders now generate recipient-bound data-only messages. On-device delivery and mixed-version rollout remain unverified. |
| 9. AI endpoint | Uses validated build configuration. Release requires HTTPS and has no local cleartext exceptions. Missing/invalid configuration disables AI requests without crashing application initialization. A real production HTTPS endpoint still must be configured. |
| 10. Acceptance | Both service entry points and the generic Accept action use a transaction with state/owner/capacity checks. Repeat acceptance does not allocate another vacancy; rejecting accepted work releases capacity. Work verification now rechecks the current pending code and updates job/application atomically. Multi-device and old-client coexistence tests remain required. |
| 11. Withdrawal race | The handler atomically reads/debits authoritative stats, reserves UTC-day amount/count, and creates a deterministic withdrawal/event ID. Same-ID retries return the prior result; changed payment details are rejected. Android persists an opaque payload/account-bound request ID through uncertain failures and restarts. Five new requests/day and Rs.1000/day are enforced in server-only `withdrawal_daily`. Existing same-day requests are counted when initializing the daily record. Missing authoritative stats fail closed. Actual payout/settlement remains outside these tests. |
| 12. Pagination | The ordinary/category summary query scans additional raw pages until the visible page is full or the query is exhausted. Other filtered/search query paths were not redesigned. |
| 13. Real-time applications | Main worker/employer/job application lists use account-scoped snapshot listeners with cleanup and replacement of old subscriptions. Worker status-filter views use the same live stream. The existing 200-record cap remains; device-level lifecycle/reconnection testing is outstanding. |
| 14. Ratings | `submitRating` derives participants/roles from completed work, rejects self/unrelated/incomplete/fabricated reviews and invalid stars, checks legacy reviews, and atomically creates a canonical review plus aggregate update. Same-payload retries are idempotent. Direct rating writes and client aggregate inflation are denied. Existing aggregates and reviews must be reconciled before rollout; completed status is still employer-controlled under the current workflow policy. |

Additional cross-check: [job callables](../functions/src/job-posting.ts) could previously bypass owner rules through Admin SDK writes. Job creation now requires the authenticated employer, scopes retry IDs by employer, and creates the deterministic job document transactionally. Batch vacancy updates reject foreign jobs before any writes. These fixes were included because server notification authorization depends on genuine job ownership.

Post-OTP changes remove phone enumeration before sign-in, preserve existing account data when a registered user enters the registration flow, merge profile initialization safely, and reject late private/contact responses after an account switch. The public profile projection is a field allowlist, not automatic redaction of sensitive information a user places inside an otherwise public bio/skills field.

### Follow-up Verification

- `:app:testDebugUnitTest`: **23 passed**, zero failures/errors/skips. Includes 17 new regression tests for workflow mapping/update payloads, transition policy, listener delivery/cleanup, filtered-page scanning, notification recipients, AI configuration, and durable withdrawal request identity. [Test results](../app/build/reports/tests/testDebugUnitTest/index.html).
- `:app:assembleDebug`: **passed**; [app/build/outputs/apk/debug/app-universal-debug.apk](../app/build/outputs/apk/debug/app-universal-debug.apk) was rebuilt from the final Kotlin source.
- `:app:compileReleaseKotlin`, `:app:processReleaseResources`, and `:app:compileDebugAndroidTestKotlin`: **passed**. This is not a signed release APK/AAB, R8 validation, or execution on a device.
- `npm run test:security --prefix functions`: **43 passed**, zero skips, against loopback Firestore emulator 1.20.4. Run serially; both emulator suites reset the same isolated `demo-dutype-android-fixes` database.
- [scripts/android-firestore-rules.test.cjs](../scripts/android-firestore-rules.test.cjs): **19 passed**, including owner/participant/anonymous access, actual Android OR-participant queries, phone-claim ownership, financial fields, direct ratings, and notification writes. The test uploads current rules and uses only synthetic accounts.
- [scripts/android-backend-security.test.cjs](../scripts/android-backend-security.test.cjs): **21 passed**, invoking actual TypeScript callable/trigger implementations against the real local Firestore SDK. Covers concurrent withdrawals and ratings, duplicates, rollback/no-write conditions, legacy records, public projection/contact privacy, notification templates/receipts, job ownership, and trigger retries. Direct handler invocation does not test Firebase's deployed HTTP authentication/App Check middleware.
- [scripts/android-push-contract.test.cjs](../scripts/android-push-contract.test.cjs): **3 passed**. Tests production payload expressions/functions with synthetic stubs and TypeScript syntax checking; no FCM send occurs. It resolves an already installed TypeScript compiler from the backend, root, or website dependency directory without changing website files. This is not a full backend typecheck or deployment test.
- `npm run typecheck --prefix functions` and `npm run build --prefix functions`: **passed** with real SDK types. The tracked backend JavaScript/source maps were regenerated by the compiler, including unchanged backend modules; website source was not modified.
- `npm ci --prefix functions --offline --ignore-scripts --no-audit --no-fund`: **passed** from the [backend lockfile](../functions/package-lock.json). `registry.npmjs.org` was confirmed policy-blocked, so no further network workaround or TLS weakening was used. Compatible cached development packages were selected; Firestore 6.8.0 is now required rather than silently omitted as an optional Admin dependency. Missing generated lockfile download/integrity fields were restored from content-verified cached registry tarballs before the clean-install gate. Host Node 24 versus declared Node 20 remains a deployment-runtime verification caveat.
- `:app:lintDebug`: **passed with `abortOnError=true`**, zero error/fatal issues, **627 warnings and 57 informational findings remain**. Foundation now follows the existing Compose BOM instead of forcing 1.10.1. Fixed the exposed API-26 channel usage on minSdk24, remembered Compose state, indentation, debug-network attributes, and sample instrumentation package ID. No additional lint checks were suppressed. [Lint report](../app/build/reports/lint-results-debug.html).
- Running lint concurrently with release/test compilation exposed a different Kotlin FIR analysis failure. The final build/test command and lint command were run separately with `--no-parallel --max-workers=1`, and both passed. Concurrency root cause is not established; use the serial gate commands below rather than claiming all combined task schedules work.
- Final index JSON validation and scoped `git diff --check`: **passed**. Index definitions in [firestore.indexes.json](../firestore.indexes.json) cover the new profile, participant/verification, rating, daily-withdrawal, and scoped-job queries; production index readiness is unverified.
- No real OTP, FCM delivery, payment, production data access, or on-device tests were performed. The isolated emulator is stopped after validation.

Final Android gate commands (with Android Studio JBR and the local SDK configured as shown in the original Verification section):

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr" :app:testDebugUnitTest :app:assembleDebug :app:compileReleaseKotlin :app:processReleaseResources :app:compileDebugAndroidTestKotlin --offline --no-parallel --max-workers=1 --console=plain
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr" :app:lintDebug --offline --no-parallel --max-workers=1 --console=plain
```

### Rollout Requirements

1. Keep the migration local until shared-client compatibility is approved. The rules/functions serve Android and other clients; website source remains untouched, but the old website and old Android clients can fail under the new rules. Do not deploy the stricter rules alone or enable a new authoritative handler while old unprotected writers remain active.
2. Reconcile authoritative `referral_stats` balances/eligibility, rating summaries/legacy reviews, and accepted counts before release. The code does not undo prior fraud. Missing authoritative financial records deliberately block withdrawals; the code never copies an untrusted profile balance into the ledger as a fallback.
3. Provision admin custom claims, deploy the required indexes in staging, and wait for readiness. Backfill `public_profiles` with the same tested allowlist. `getPublicProfile` repairs an individually viewed missing profile, but does not populate complete role/search lists; a controlled paged backfill remains required.
4. Stage `submitRating`, `getPublicProfile`, `getApplicationContact`, `requestNotification`, `notifyApplicationEvent`, `syncPublicProfile`, the new withdrawal protocol, and both recipient-bound data-only push senders with the new Android client. Withdrawals require a stable `requestId`; old clients must be updated or gated. Direct review/notification writes and unauthenticated profile queries are intentionally no longer supported.
5. Establish a coordinated maintenance/version-gated cutover so old insecure notification/rating writers cannot race the new server implementations. Deploy the stricter rules together with the compatible clients/backend, not piecemeal. Check existing backend landing/profile consumers separately before cutover; their source was outside this task.
6. Use Node 20 for staging/runtime tests, supply a real HTTPS `AI_BACKEND_URL`, and perform device tests for login/register/role switching, profile/contact visibility, acceptance/start/completion, ratings, delayed pushes during logout, retries/process death, and offline recovery. Test remaining 627 lint warnings by risk; do not treat zero errors as zero quality debt.
7. Run signed release/R8 and platform/native-library compatibility checks, confirm App Check/auth-provider/key policies, and review outstanding storage/backup exposure, workflow abuse/rate limits, and operational alerting. These were not converted into a production-security certification by this local migration. App Check enforcement and real push/payment delivery were not tested.

## Findings

P1 means a high-priority release blocker; P2 means an important correctness, privacy, or reliability issue. Emulator evidence applies to the checked-in rules, not necessarily the rules currently deployed to production.

### 1. [P1] Private profiles, applications, and payment details are readable by unrelated users

- Locations: [profile read rules](../firestore.rules#L29), [anonymous query rule](../firestore.rules#L35), [application reads](../firestore.rules#L117), [withdrawal listing](../firestore.rules#L297).
- Reproduced: an unauthenticated `limit(2)` query returned complete synthetic profiles, including a private FCM token. An unrelated authenticated user could read an application's verification code and list another user's withdrawal bank-account data.
- Firestore reads return whole documents. A comment saying "PUBLIC fields only" does not hide private fields, and limiting a query to two documents neither restricts it to phone lookup nor prevents repeated queries.
- Fix: separate genuinely public profile data from private records; enforce participant/owner authorization for applications and withdrawals. Remove public queries against private user documents. Keep any necessary pre-login lookup minimal and server-controlled.

### 2. [P1] Clients can take over other users' jobs and applications

- Locations: [job write authorization](../firestore.rules#L103), [application write authorization](../firestore.rules#L120), [client verification transition](../app/src/main/java/com/example/dutype/services/WorkVerificationService.kt#L325).
- Reproduced: an unrelated user was denied a title-only job edit, but changing `employerId` to their own UID in the same request allowed it. Replacing an application's `workerId` likewise allowed takeover. An existing worker could directly set their application's status to `COMPLETED` and verification status to `VERIFIED`.
- Authorization trusts ownership in the proposed document, rather than requiring ownership of the existing document and preserving immutable IDs. It also allows participants to change every workflow field.
- Fix: distinguish create/update/delete authorization, preserve owner and participant IDs, allowlist mutable fields, and enforce role-specific state transitions. Verification and financially meaningful transitions need authoritative server validation.

### 3. [P1] Users can forge the balances and eligibility used for withdrawals

- Locations: [user creation/update rules](../firestore.rules#L43), [Android withdrawal call](../app/src/main/java/com/example/dutype/services/ReferralService.kt#L647), [stats precedence](../functions/src/referral-system.ts#L190), [withdrawal balance validation](../functions/src/referral-system.ts#L1189).
- Reproduced in rules: a normal user could set their own `referralStats.availableBalance`, `successfulReferrals`, and `canWithdraw`, and clear `isBlocked`. These fields are not protected by the user-update rule.
- Source cross-check: the withdrawal function gives `users.referralStats` precedence over legacy stats and uses those values to authorize a withdrawal request. It is not sufficient that request creation itself is restricted to a Cloud Function.
- The same probe removed and then replaced the supposedly immutable `referralCode`; the rule permits removal, then treats the replacement as an initial assignment.
- Fix: make balances, referral eligibility, fraud controls, and code assignment server-owned on both create and update. Validate withdrawals against an authoritative ledger.
- Limit: no withdrawal handler or payment was executed. The client-write bypass is reproduced; the callable trust path is source-traced. A pending request is not proof of an actual payout.

### 4. [P1] Administrative access trusts an unverified email claim

- Location: [isAdmin](../firestore.rules#L8).
- Reproduced: an emulator-authenticated identity with `email_verified: false` and an email under `dutype.com` could write an administrator-only announcement.
- The rule checks an email string, not a server-issued admin role or verified membership. Exploitability in production depends on the enabled sign-up/email-change flows and token issuance configuration, which were not inspected.
- Fix: use tightly administered custom claims for administrative authorization. Do not derive admin access from an unverified address.
- The unsigned tokens used by the probe are emulator fixtures only. This is not a claim that production Firebase accepts forged JWT signatures.

### 5. [P1] Any authenticated user can create app-branded messages for other users

- Locations: [notification creation rule](../firestore.rules#L139), [push trigger](../functions/src/index.ts#L228), [Android display path](../app/src/main/java/com/example/dutype/services/DutyPeMessagingService.kt#L59).
- Reproduced: a normal user could create a notification with another user's `recipientId` and a system-style type/title.
- Source cross-check: the trigger looks up that recipient's FCM token and constructs an app push. Duplicate suppression does not authenticate the sender or authorize the content. The Android receiver displays the supplied title/body.
- Fix: create privileged notifications server-side from authorized business events; validate sender/recipient relationships, message types, templates, and rate limits for any client-submitted messaging.
- Limit: actual FCM delivery was not invoked during the audit.

### 6. [P1] Successfully starting work produces a status the Kotlin app cannot deserialize

- Locations: [work-start write](../app/src/main/java/com/example/dutype/services/WorkVerificationService.kt#L325), [ApplicationStatus](../app/src/main/java/com/example/dutype/models/JobApplicationModels.kt#L141), [worker parsing](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L503), [employer parsing](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L661).
- Reproduced using the compiled app and actual Firebase mapper: `ACCEPTED` deserialized successfully, while `IN_PROGRESS` threw `Could not find enum value ... for value "IN_PROGRESS"`.
- The worker's manual fallback maps unknown statuses to `PENDING`; employer list parsing catches the error and omits the record. Verified work can therefore appear pending to the worker and disappear from the employer's list.
- Fix: align persisted states, Kotlin enums, all status-dependent UI branches, queries, and transition validation. Add coverage for work start through completion and existing stored records.

### 7. [P1] Normal application edits erase persisted verification data

- Locations: [application model](../app/src/main/java/com/example/dutype/models/JobApplicationModels.kt#L15), [status replacement](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L1089), [notes replacement](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L1151).
- Reproduced using the actual mapper: a model round-trip dropped `verification`, `verificationCode`, `verificationStatus`, and `workStartedAt` because these fields are absent from `JobApplication`.
- These service methods call `set(updatedApplication)` without merge. For example, adding notes to an accepted application that already has a verification code replaces its document without that code.
- Fix: update only the intended fields, using a transaction where state/history must remain consistent. Keep persisted and display-enriched models distinct; do not replace server-owned records with partial UI models.

### 8. [P1] Logout leaves the previous account's push token registered

- Locations: [sign-out ordering](../app/src/main/java/com/example/dutype/auth/AuthManager.kt#L116), [later token removal](../app/src/main/java/com/example/dutype/auth/AuthManager.kt#L137), [token cleanup guard](../app/src/main/java/com/example/dutype/services/FCMTokenManager.kt#L169), [message receiver](../app/src/main/java/com/example/dutype/services/DutyPeMessagingService.kt#L59).
- Source cross-check: `signOut()` runs before asynchronous `removeToken()`. Cleanup obtains `auth.currentUser?.uid`, which is normally null by then, and returns without removing the old user's token.
- Notifications for the old account can continue reaching the device after logout or another login. The receiver does not check a recipient UID against the current account before displaying the body. A fast subsequent login can also make delayed cleanup operate on the new UID.
- Fix: capture account/device identity and coordinate token unlinking with logout, while ensuring network failure cannot prevent local sign-out. Bind tokens to account/device registrations and reject mismatched private notifications on receipt.
- Limit: delivery after logout was source-traced, not tested on a device.

### 9. [P1] The injected AI client always uses a private cleartext address

- Locations: [hardcoded endpoint](../app/src/main/java/com/example/dutype/di/AIModule.kt#L27), [unused configured URL](../app/build.gradle.kts#L46), [cleartext exception](../app/src/main/res/xml/network_security_config.xml#L4).
- Reproduced using the actual injected Retrofit factory: the base URL is `http://10.91.59.173:8000/`, not the configured `BuildConfig.AI_BACKEND_URL`. The constant is not restricted to debug builds.
- Released clients using this module cannot reach a public production backend through that address. On a reachable LAN, the requests are unencrypted and can reach a service on that private address.
- Fix: use variant-appropriate configuration, require HTTPS in release, and move development cleartext exceptions into debug-only resources. Do not treat an embedded API key as a secret or substitute for user authorization.

### 10. [P1] Accepting from the applications list bypasses vacancy checks

- Locations: [Accept button](../app/src/main/java/com/example/dutype/employer/screens/applications/EmployerApplicationManagementScreen.kt#L588), [ViewModel delegation](../app/src/main/java/com/example/dutype/viewmodels/EmployerApplicationViewModel.kt#L382), [generic transition](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L1057), [separate hire method](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L1253).
- Source cross-check: the list's Accept button calls the generic status update, which does not check remaining vacancies or run the hire method's verification setup. This permits normal UI actions to bypass the intended capacity policy.
- The separate hire method also checks capacity and writes acceptance as separate operations; routing every caller there alone would not prevent concurrent overbooking.
- Fix: consolidate all acceptance paths behind one authorized, idempotent transaction that validates current state and reserves capacity. Count workers already in progress as occupying capacity too.
- Limit: concurrent Android acceptance was not exercised on devices.

### 11. [P1] Withdrawal authorization and balance deduction are not atomic

- Locations: [balance check](../functions/src/referral-system.ts#L1206), [daily-total query](../functions/src/referral-system.ts#L1213), [new withdrawal and batch](../functions/src/referral-system.ts#L1232), [commit](../functions/src/referral-system.ts#L1274).
- Source cross-check: both the balance and daily-total checks happen before a separate write batch. A batch makes its writes atomic, but does not protect the earlier reads from other requests.
- Concrete untested interleaving: two requests for 600 can both read balance 1000 and daily total zero, then both commit deductions. That permits 1200 in pending requests and a balance of -200. A new random withdrawal ID does not deduplicate retries.
- Fix: reserve funds and a per-user daily total in a transaction against server-owned data, with a stable idempotency key and explicit payment-state transitions. Instance-local rate limiting does not solve this race.
- Limit: the optional handler race probe was not executed because its declared backend dependencies were unavailable. The numeric example is an interleaving derived from source, not an observed result.

### 12. [P2] Client-side filtering prematurely stops job pagination

- Locations: [server page limit and subsequent filtering](../app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt#L185), [initial hasMore calculation](../app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt#L526), [load-more guard](../app/src/main/java/com/example/dutype/viewmodels/AllJobsViewModel.kt#L616).
- Source cross-check: the service limits the raw Firestore page and then removes inactive, expired, or filled jobs. The ViewModel sets `hasMore` from the filtered count and retains only the last visible document as its cursor.
- With a 15-document page containing one inactive job, 14 visible jobs set `hasMore` to false even when older matching jobs remain. An entirely filtered page also loses the cursor needed to reach valid results beyond it.
- Fix: return raw-page cursor/exhaustion metadata separately from visible items, or filter on the server with appropriate indexes. Continue scanning filtered pages until exhausted or a visible page is filled.

### 13. [P2] Application lists do not receive real-time remote status changes

- Locations: [worker application flow](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L478), [job application flow](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L603), [employer application flow](../app/src/main/java/com/example/dutype/services/JobApplicationService.kt#L636), [worker load guard](../app/src/main/java/com/example/dutype/viewmodels/SmartJobApplicationViewModel.kt#L117).
- Source cross-check: these flows perform `get().await()`, emit a result, and finish. Their ViewModels do not attach a replacement remote listener. The shared application state manager is in-memory state, not a Firestore subscription.
- If an employer changes a status while the worker leaves the list open, the list can remain stale until an explicit refresh/navigation reload. The already-loaded guard can also skip a repeated load call.
- Fix: use lifecycle-scoped snapshot listeners with `awaitClose` cleanup, or an explicit refresh contract covering remote notifications and foreground/resume. Existing listener-based referral/job services provide nearby patterns to reuse.
- Limit: two-device latency, lifecycle, and reconnection behavior remain untested.

### 14. [P2] Ratings allow fabricated reviews and silently fail to update summaries

- Locations: [rating rules](../firestore.rules#L278), [review write and summary call](../app/src/main/java/com/example/dutype/services/RatingService.kt#L104), [summary update](../app/src/main/java/com/example/dutype/services/RatingService.kt#L122), [target profile ownership rule](../firestore.rules#L45).
- Reproduced in rules: a user could create a rating of 999 for nonexistent work. No completed application, real participant relationship, rating range, or deterministic uniqueness is enforced there.
- A denied control also showed that an ordinary user cannot update another user's rating summary. The Kotlin service attempts that update after saving the review, catches the failure inside `updateUserRatingSummary`, and still reports submission success.
- Fix: validate and deduplicate reviews against completed work server-side, and update aggregates transactionally. Protect aggregate fields from self-editing and distinguish a saved review from failed dependent work.

## Code Quality

- **Broken lint gate:** `:app:lintDebug` reproducibly crashes in `RememberInCompositionDetector` with a `KaSimpleVariableAccessCall` class/interface incompatibility. The resolved Compose artifacts include 1.10.1 despite an older BOM; align the supported AGP/Kotlin/Compose/lint toolchain rather than disabling additional checks. [Version catalog](../gradle/libs.versions.toml#L1), [lint configuration](../app/build.gradle.kts#L153).
- **Weak release/test gates:** `abortOnError = false` permits ordinary lint errors without failing the build. The six existing JVM tests consist of five address-validation tests and one arithmetic sample. The sole instrumentation sample expects `com.example.dutype`, but the application ID is `com.dutype.app`; it would fail that assertion if run against this app. [Instrumentation assertion](../app/src/androidTest/java/com/example/dutype/ExampleInstrumentedTest.kt#L22), [application ID](../app/build.gradle.kts#L34).
- **Incomplete offline posting:** the current posting screen calls `createJob`, not `createJobWithOfflineSupport`. The separate queue restores numeric map values as `Double`, confirmed by the Gson probe; acceptance code casts vacancies to `Long` and would fall back to 1 for those values. Its idempotency key controls the WorkManager name, but job creation still allocates a new document on each invocation. These are defects in a currently unwired path, not proof that an on-device offline retry was reproduced. [Posting call](../app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt#L683), [offline entry point](../app/src/main/java/com/example/dutype/viewmodels/FirestoreEmployerJobViewModel.kt#L269), [queue decoding](../app/src/main/java/com/example/dutype/employer/sync/JobPostingWorker.kt#L140), [job creation](../app/src/main/java/com/example/dutype/services/firestore/JobFirestoreService.kt#L41).
- **Competing workflow owners:** `ApplicationManagementService` and `JobApplicationService` implement overlapping transitions with different transaction and notification behavior. The live Accept path does not use the transactional implementation. Consolidate behavior before further local fixes. [Alternative transaction](../app/src/main/java/com/example/dutype/services/ApplicationManagementService.kt#L141).
- **Session controls are not an enforcement boundary:** the inactivity update method has no external call sites, auth-error logout events have no consumers in the searched Kotlin sources, and session expiration does not itself sign out Firebase. The session timeout also implements seven hours while its comment says seven days. Define the intended policy and wire it to lifecycle/auth transitions, or remove misleading security claims. [Session policy](../app/src/main/java/com/example/dutype/auth/SessionManager.kt#L56), [logout event](../app/src/main/java/com/example/dutype/core/error/ErrorHandler.kt#L193).
- **Sensitive diagnostics:** OTP sending logs the full phone number through the Crashlytics breadcrumb helper, not just a debug-only logger. Minimize or mask this before recording it; production uploads were not inspected. [Phone breadcrumb](../app/src/main/java/com/example/dutype/viewmodels/OtpViewModel.kt#L209), [Crashlytics sink](../app/src/main/java/com/example/dutype/core/error/ErrorHandler.kt#L224).
- **Additional privacy hardening:** cover-letter storage permits reads by any authenticated user; upload rules do not restrict file size/type. Backup configuration remains template defaults despite locally stored user data. Storage contents and backup/restore behavior were not inspected, so actual stored-document exposure and backup requirements remain open. [Storage rules](../storage.rules#L16), [backup policy](../app/src/main/res/xml/backup_rules.xml#L8), [local user data](../app/src/main/java/com/example/dutype/auth/AuthManager.kt#L51).

## Verification

| Check | Result | What It Establishes |
| --- | --- | --- |
| Debug Kotlin/Java compilation through unit-test task | Passed | The checked-in Android code compiles in the configured local environment. |
| Existing JVM tests | 6 passed, 0 failures/skips | Address validation and the arithmetic sample only. |
| Focused compiled-app JVM probes | 4 passed, 0 failures/skips | Reproduced the enum, verification round-trip, Gson numeric-type, and AI endpoint defects. Passing here confirms bugs exist, not that they are fixed. |
| Local Firestore rules probes | 17 completed | 13 unsafe allows reproduced and 4 controls denied. These are not 17 independent vulnerabilities. |
| Android lint | Failed | Reproduced a detector/toolchain failure; no complete lint assessment is available. |
| Android device/UI tests | Not run | `adb devices -l` showed no connected devices during this review. |
| Actual withdrawal callable/race probe | Not run | Backend test dependencies unavailable; financial handler findings are source-traced. |

Local artifacts, generated under ignored build outputs:

- [Firestore probe](../app/build/reports/android-review/firestore-audit.cjs#L1) and [recorded rules results](../app/build/reports/android-review/firestore-results.txt#L1).
- [Compiled-app probes](../app/build/reports/android-review/AndroidReviewProbe.java#L1), [Gradle probe task](../app/build/reports/android-review/java-probe.init.gradle#L1), and [four-test results](../app/build/test-results/testAndroidReviewProbe/TEST-AndroidReviewProbe.xml#L2).
- [Five validation-test results](../app/build/test-results/testDebugUnitTest/TEST-com.example.dutype.ValidationUtilsTest.xml#L2), [sample-test result](../app/build/test-results/testDebugUnitTest/TEST-com.example.dutype.ExampleUnitTest.xml#L2), and [captured lint output](../app/build/reports/android-review/lint-output.txt#L1).

These local artifacts are not durable regression coverage in source control and will be removed by a clean build. Promote relevant probes into ordinary tests when fixing the findings.

The default Java 25 runtime initially blocked Gradle. Verification used Android Studio's bundled JDK and the installed Android SDK via process-local configuration; project configuration was not changed. The unit-test command was:

```powershell
& {
    $previousAndroidHome = $env:ANDROID_HOME
    try {
        $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
        .\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr" :app:testDebugUnitTest --offline --console=plain
    }
    finally {
        $env:ANDROID_HOME = $previousAndroidHome
    }
}
```

The rules run used Firestore emulator 1.20.4 on `127.0.0.1:8185` with project `demo-dutype-android-audit`, synthetic records, and the checked-in rules. It did not query or mutate production data. The emulator was stopped after the final run. No real OTPs, payments, push messages, or account changes were initiated.

## Remaining Gaps

- Confirm which rules/functions are actually deployed, enabled Firebase auth providers, App Check enforcement, and key restrictions before making claims about live exploitability. Do not test unsafe writes against real user records.
- Exercise login, role switching, job posting/applying, acceptance, verification, completion, rating, logout/account switching, offline recovery, and deep links on Android devices using test accounts.
- Add two-device real-time and concurrent-action tests, process-death/restoration tests, permission-denial cases, and retry/cancellation tests.
- Run a release APK/AAB build and inspect R8 behavior, native-library 16 KB page alignment, exported/merged components, and signing configuration. These were not validated by the debug compile.
- Complete an advisory-based dependency vulnerability scan and measured startup/scroll/network/battery profiling. Old versions, large files, or "optimized" comments alone do not establish a CVE or a measured performance improvement.
- No vulnerability-free or exhaustive end-to-end production claim is justified by the available checks.

## Repair Order

1. Close the backend authorization and financial-field gaps, with positive and negative emulator tests for every supported Android action. Coordinate changes with clients so legitimate profile, rating, and verification flows do not simply become permission errors.
2. Align application states and replace destructive document writes; route acceptance/verification/completion through authoritative transitions.
3. Correct logout/token ownership and release AI configuration.
4. Repair pagination, live updates, ratings, and offline behavior; add focused regression tests.
5. Restore a functioning lint/release gate, then perform device and staging end-to-end verification before release.

The original review above was read-only. The follow-up changes and still-open findings are tracked in Local Fix Status at the top of this document.