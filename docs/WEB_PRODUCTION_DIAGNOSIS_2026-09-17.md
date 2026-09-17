# Website Login And Live-Job Diagnosis

Date: 2026-09-17

Status: local fixes and regression checks completed; deployment and real production authentication/data checks are not certified by these results.

## Confirmed Causes

| Symptom | Reproduced or source-verified cause | Change |
| --- | --- | --- |
| Android has jobs, website does not | Android writes `jobmetadata` plus `job_details`; the website queried only legacy `jobs` with `isActive == true` | Canonical reader with masked summary joins and legacy compatibility |
| An old job link revives a closed Android job | Legacy `jobId` alias lookup returned the legacy document without checking canonical metadata at the resolved document ID | Resolve aliases through the same canonical-first lookup as direct links |
| City/nearby queries miss app records | Android uses `salary`, `salaryType`, `addressText`, nested `location.lat/lng`, and `status: open` | Normalize these fields, preserve expiry, and use nested coordinates internally |
| Hyderabad query misses valid addresses | Some Android-derived `companyCity` values contain the state instead of the city | Prefer explicit city, then a recognized address city, then the derived value |
| Next page looks signed out | Public navigation always displayed Sign in; posting confused profile errors or missing roles with guest state | One root session provider, session-aware header, explicit loading/profile/access states |
| Email login succeeds but workspace fails | Email completion did not initialize/refresh the shared canonical account like provider login | Verify canonical profile initialization and hydration before routing |
| Provider login silently stops | Auth events could unmount the Google/OTP component before onboarding completed; its local error could disappear | Retain active provider flow and keep setup errors in the account screen |
| Navigating away returns to login's destination | Delayed profile reads could finish and route after leaving auth | Page-lifetime, request-version and current-account guards |
| Details page says session expired during server failure | All Admin token-verification errors became 401; detail UI erased its local account | Rejected tokens remain 401, operational failures become 503; one forced token refresh, no product sign-out |
| Visiting admin login loses product account | Admin permission/network failure signed out the shared browser Auth instance | Deny admin access without logging the product user out |
| Retrying publishing resets a live job | A committed write with a lost response was retried with a replacing batch, resetting counts, expiry and closed state | Read-before-create transaction; matching retries return the saved job without further writes |
| Returning to the posting page creates a duplicate | The pending job ID existed only in component memory, while its draft survived remount | Persist the owner-bound pending ID with the draft before writing |
| Posting drops newer saved locations | The writer replaced locations using the cached session profile | Read and update the latest saved locations inside the transaction |
| An old publish redirects a different account | Completion cleared draft storage and navigated after the original page unmounted | Guard completion by mounted page and current UID; remove only the matching request's draft |
| Robots returns 500 | `public/robots.txt` duplicated `app/robots.ts` again | Keep the App Router metadata route; regression prevents duplicate owner |
| Standalone packaging command fails | Declared `build:standalone` referenced a missing script | Restore build script and copy required `.next/static` and `public` assets |
| Images fail after the standalone server reports Ready | `sharp` was not declared as a production dependency; server startup did not exercise image optimization | Install `sharp` 0.34.5 and verify the packaged Next.js optimizer before declaring the build ready |

These findings do not prove every reported production failure has the same cause. They identify independently reproduced defects in the current checkout.

## Data And Session Boundaries

- Public city/area/current-location search requires no browser login. Server-side Firebase access is still required for the reader.
- Canonical metadata is authoritative over same-ID legacy records, including closed records. Public queries fetch only `companyCity` and `expiresAt` from detail documents; no full description or contact number is emitted in summaries, HTML or list structured data.
- Legacy `jobId` aliases now check canonical metadata at the resolved legacy document ID before using legacy content. The regression reproduced a closed job appearing open through its old link, then verified closed suppression, canonical open-job content, genuine legacy-only compatibility and unchanged public detail-field masking. All 40 data/API contract tests passed after this correction.
- Source-aware continuation cursors cross from canonical metadata to legacy jobs without duplicating a migrated record. The existing 500-candidate/request and 20-match/page bounds remain; this is not a globally sorted full-text or nearest-neighbour index. A sparse city's first scanned batch can still require **Search more listings**. Indexed city/geo projection and partitioned sitemap work remain necessary at larger scale.
- Canonical numeric timestamp representations in seconds, milliseconds and microseconds are normalized. Closed or expired records are not made active merely to match Android's cached counts.
- Current registered Web App defaults match the checked-in Android and Firebase deployment project. Explicit overrides are trimmed, and required blank fields disable account initialization. Do not replace the current project with an older key/configuration or mix environments; public defaults do not prove remote key validity.
- A project ID alone is not evidence of usable server credentials. Server failures emit a rate-limited `live_jobs_service_failure` diagnostic containing only operation and sanitized error code, never token, credential contents, raw query or job content.
- Failed account checks retain authentication while clearing stale role/profile authority. Publishing does not reactivate blocked users or strip existing admin/employer roles. Phone sign-in no longer queries account names/roles before OTP verification and requires explicit SMS consent.
- Firebase browser persistence is origin-scoped. Production verification must confirm that navigation does not switch between bare/www, preview and production origins. No user browser storage was cleared to make tests pass.

## Posting Retry Guarantees

- Website publishing still writes legacy `jobs` and the employer's `users` profile. These two writes now share a transaction; existing job records are never replaced by a create retry. Ownership or changed-content conflicts stop without writes.
- The pending job ID is saved with its owner and form before submission. A valid, unexpired draft reuses that ID after remount. If tab storage cannot save the request, publication stops before any database write and remains retryable after storage recovers.
- This is recovery for the same retained request, not a global server-side idempotency receipt. Clearing tab storage, expiry of the one-hour draft, or starting in another browser does not preserve the request identity.
- Saved-location usage is calculated from the transaction's profile snapshot, not from the older rendered profile. A completed retry does not apply that usage increment again.
- Late completion cannot clear another request's draft or redirect after a page/account change. This UI guard does not cancel or roll back a write that has already been submitted.
- Seven additional browser regressions cover lost acknowledgements, changed content, foreign ownership, newer saved locations, remount recovery, an account switch during held completion, and unavailable tab storage. All 13 focused posting checks pass. The fixture simulates committed/failed/held responses; it does not establish actual Firestore callback-retry, authentication-rule or cross-client contention behavior.

## Verification

Commands from repository root:

```powershell
npm --prefix web test
npm --prefix web run test:contracts
npm --prefix web run test:browser
npm --prefix web run test:browser:layout
npm --prefix web run type-check
npm --prefix web run build:standalone
```

- Astra was explicitly requested for the investigation, implementation delegation and independent review using the available `GPT-6 Astra` model selector.
- Completed suite runs passed 38 Vitest tests, 40 server/API contract tests and 83 browser behavior tests, with zero failures or cancellations. The full contract suite was rerun after the legacy-link correction; the unit and browser suites passed during the preceding posting fixes. Browser regressions use the real session hook for the reported navigation/provider races and isolated Firebase responses. They cover email -> Jobs -> Post job, profile denial, token-service outage, Google/OTP completion, leaving auth mid-request, persistent setup errors, guest Android-shaped city/nearby search, connection recovery and the posting cases above.
- Earlier in this investigation, all 24 mobile/desktop checks passed for auth, OTP, signup, Jobs, details and employer posting at 320, 390, 768 and 1440 pixels. The continuation did not change layout or styles. Screenshots are local ignored artifacts under `web/test-results/`.
- The legacy-link `build:standalone` completed with exit code 0 and 378 generated pages, including TypeScript and lint checks. Build IDs matched, and 20 browser assets plus three job-server assets matched their packaged copies byte-for-byte. The generated reader was inspected for canonical alias resolution, and the duplicate public robots asset was absent. Eight existing admin-image/effect lint warnings remained, with no build errors.
- The subsequent missing-`sharp` fix also passed the complete standalone build. Native `sharp` 0.34.5/libvips 8.17.3 processed the real logo on Windows x64. The build script now runs the packaged Next.js `optimizeImage` on that logo, checks the resulting 64px WebP, and rejects dependencies resolved outside the standalone directory. This uses the real native optimizer, not a mock; an HTTP image-route/browser retest remains unverified under network policy.
- The rebuilt standalone preview reported ready at `http://127.0.0.1:3000`. This verifies server startup, not HTTP page timing or live Firebase access.
- Local Node is 24.14.0; the package declares Node 22. Run the release gates on Node 22 in the deployment environment as well. Dependencies were recovered from local npm caches without disabling TLS or changing network policy.

## Production Release Checklist

1. Install dependencies including optional native `sharp` packages and rebuild on the deployment OS/architecture using Node 22, then deploy through the existing release process. Do not ship Windows native binaries to a Linux runtime. No production deployment or production account/job writes were performed during this diagnosis.
2. Confirm the runtime's server identity, project/database and Firestore/Auth permissions. Local server credentials were absent; that does not establish which credentials exist in production. A 503 with `setup-required` differs from `temporary`; inspect sanitized runtime logs for timeouts or permission/precondition codes.
3. On an authorized HTTPS staging or production session, verify guest `/api/jobs?city=Hyderabad` returns canonical summaries. Check a known current app job by ID and confirm nested coordinates work after an explicit Use my location click. Verify a closed job is absent and manual search works when location is denied.
4. Sign in with an approved test email account and navigate Home -> Jobs -> Post job -> account -> reload. Confirm Firebase retains the user, the header displays My account, profile errors do not become guest prompts, and roles are unchanged. Repeat with Google and a configured test-phone flow; do not send real SMS without an approved test.
5. Verify an unauthorized admin session does not sign out the product user, but still cannot access admin data. Check authenticated detail requests with valid, expired, revoked and rejected tokens, plus an operational Auth-service failure.
6. Validate actual canonical job pages/sitemap output and submit the updated sitemap in Search Console. Browser AI recommendations, indexing and ranking are not guaranteed. Do not expose login-gated descriptions solely in structured data to obtain rich results.

## Explicit Remaining Risks

- Network policy blocked production/local browser access and Firebase API/docs access. The user was unavailable to approve requested diagnostic domains. No policy was bypassed. Isolated tests do not prove actual Firebase persistence, deployed IAM, reCAPTCHA/SMS, production network timing or deployed query indexes.
- Website job creation still uses the legacy `jobs` writer, which the repaired public reader supports. This work does not migrate that writer into Android's canonical employer-profile/subscription/quota flow. Existing canonical Android profile setup requires verified phone/App Check and has its own limits; do not silently create entitlement-bearing profiles or bypass those controls. Bidirectional posting parity needs a separately tested canonical posting/onboarding rollout.
- The current shared rules publicly expose raw job collections/detail documents. The web API's summary allowlist and authenticated detail route are not database-wide privacy. Tightening those shared rules requires Android coordination; no rules were weakened or deployed here.
- Large-catalog city/geo indexing, server-enforced posting quotas, and an app-wide security audit are not established by these focused fixes. The current bugs were addressed without claiming that unrelated features are production-certified.

## Coordinated Posting Migration Plan

Changing only the collection name is unsafe. Source review found that canonical posting expects verified-phone onboarding and employer-profile/credit checks, while the current employer-profile rules allow owners to update subscription data. Android's free-post count is also outside its job-creation transaction. ID-token verification plus a website-only counter would not establish trusted entitlements or shared limits.

The user was unavailable when approval to expand into shared Firebase functions/rules and Android callers was requested. That response was not treated as authorization; those shared files remain unchanged by this continuation. A coordinated local migration, followed by a separately approved rollout, needs these gates:

1. Establish one server-controlled posting command with verified account/phone ownership and App Check. Use the existing secure onboarding contract, not client-assigned roles or invented employer profiles. Decide how Google/email employers verify a phone on the same UID without silently merging or switching accounts.
2. Make entitlements server-controlled and define one shared daily quota/timezone policy. Coordinate all Android and website writers; a new web counter cannot account atomically for unchanged Android clients.
3. Create same-ID `jobmetadata` and `job_details`, reserve quota or debit credit, and record a UID/payload-bound idempotency receipt in one transaction. Validate canonical location/geohash, text/pay limits, vacancies and expiry; reject conflicting retries without charges.
4. Move web and Android creation/editing and employer management to that contract. Preserve historical legacy reads and explicitly resolve existing edit-window/status differences. Avoid creation-only dual writes that drift when jobs are edited or expired.
5. Test authorization failures, App Check/replay handling, simultaneous last-credit/free-slot requests, lost responses, rollback, existing Android records and obsolete-client behavior in an isolated Firebase environment. Coordinate raw-detail privacy rules with Android readers before any deployment.