# Firestore Field Audit - Old/Bloated Fields Still Being Written

**Audit Date:** March 18, 2026  
**Status:** Code Review Only - No Changes Made  
**Scope:** All Firestore write operations across Android, Web API, Components, Functions

---

## 1. USERS COLLECTION - Field Issues

### Issue: `phoneNumber` field being written (should be `phone`)

| File | Line | Collection | Operation | Field | Value/Context |
|------|------|-----------|-----------|-------|-------------|
| [app/src/.../services/ProfileCompletionService.kt](app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt#L500) | 500 | `users` | `.set()` merge | `phoneNumber` | normalized phone passed to ensureMinimalUserDocument |
| [app/src/.../services/ProfileCompletionService.kt](app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt#L607) | 607 | `users` | `.set()` merge | `phoneNumber` | from profileData["phone"] |
| [app/src/.../utils/FirestoreUtils.kt](app/src/main/java/com/example/dutype/utils/FirestoreUtils.kt#L40) | 40 | `users` | `.set()` | `phoneNumber` | normalized from parameter |
| [app/src/.../utils/FirestoreUtils.kt](app/src/main/java/com/example/dutype/utils/FirestoreUtils.kt#L79) | 79 | `users` | `.set()` | `phoneNumber` | normalized phone in strict document |

**Impact:** These writes bypass the schema migration - still writing old `phoneNumber` field instead of `phone`

---

### Issue: `profileCompleted` field (should be removed - computed at read time)

| File | Line | Collection | Operation | Field | Value/Context |
|------|------|-----------|-----------|-------|-------------|
| [web/components/product/worker-app.tsx](web/components/product/worker-app.tsx#L152) | 152 | `users` | calculation | `profileCompleted` | assigned from `workerProfileCompletion(mergedProfile) >= 80` |
| [web/components/product/worker-app.tsx](web/components/product/worker-app.tsx#L1249) | 1249 | `users` | `.set()` | `profileCompleted` | `payload.profileCompleted ?? false` |
| [web/components/product/product-auth-client.tsx](web/components/product/product-auth-client.tsx#L143) | 143 | `users` | `.set()` | `profileCompleted` | `false` on new user creation |
| [web/components/product/product-auth-client.tsx](web/components/product/product-auth-client.tsx#L168) | 168 | `users` | `.set()` | `profileCompleted` | `false` on new user creation |
| [web/components/product/product-auth-client.tsx](web/components/product/product-auth-client.tsx#L186) | 186 | `users` | `.set()` | `profileCompleted` | `false` on new user creation |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L366) | 366 | `users` | read-only | `profileCompleted` | check `after.profileCompleted !== true` |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L822) | 822 | `users` | read/trigger | `profileCompleted` | checks if changed from false to true |

**Impact:** Field is written on creation but should be computed from profile completion data instead

---

## 2. JOBS COLLECTION - Status Field Issues

### Issue: `isActive` and `isFilled` fields (should be replaced with single `status` field)

| File | Line | Collection | Operation | Fields | Value/Context |
|------|------|-----------|-----------|--------|-------------|
| [web/components/admin/admin-jobs-client.tsx](web/components/admin/admin-jobs-client.tsx#L106) | 106 | `jobs` | `.updateDoc()` | `isActive` | toggled with `!currentActive` |
| [web/components/admin/admin-jobs-client.tsx](web/components/admin/admin-jobs-client.tsx#L144) | 144 | `jobs` | `.setDoc()` merge | `isActive` | `editing.isActive` |
| [web/components/product/employer-app.tsx](web/components/product/employer-app.tsx#L1645) | 1645 | `jobs` | payload | `isActive` | `nextStatus === "open"` |
| [functions/src/index.ts](functions/src/index.ts#L529) | 529 | `jobs` | `.update()` | `isActive` | `false` when job is auto-rejected |
| [functions/src/index.ts](functions/src/index.ts#L666) | 666 | `jobs` | `.update()` | `isActive` | `false` when job removed from moderation |
| [functions/src/job-posting.ts](functions/src/job-posting.ts#L163) | 163 | `jobs` | batch `.update()` | `isFilled` | when `applicationsCount >= vacancies` |
| [web/components/product/employer-review.tsx](web/components/product/employer-review.tsx#L799) | 799 | `jobs` | `.updateDoc()` | `status` + `vacancyStatus` | `isFilled ? "FILLED" : "OPEN"` |

**Reading/Display (Legacy Compatibility):**
| File | Line | Context |
|------|------|---------|
| [web/components/product/employer-app.tsx](web/components/product/employer-app.tsx#L175) | 175 | displays `job.isFilled ? "Filled" : job.isActive ? "Live" : "Paused"` |
| [web/lib/product/marketplace.ts](web/lib/product/marketplace.ts#L196-197) | 196-197 | builds response with both `isActive` and `isFilled` for compatibility |

**Impact:** Mixed use of old `isActive`/`isFilled` and new `status`/`vacancyStatus` fields

---

## 3. IMAGE URL FIELD - photoUrl vs profileImageUrl

### Status: ✅ CORRECT - Already using `profileImageUrl`

| File | Line | Collection | Operation | Field | Value/Context |
|------|------|-----------|-----------|-------|-------------|
| [app/src/.../services/ProfileCompletionService.kt](app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt#L292) | 292 | `users` | `.set()` merge | `profileImageUrl` | download URL from image upload |
| [app/src/.../services/ProfileCompletionService.kt](app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt#L623) | 623 | `users` | `.set()` merge | `profileImageUrl` | from profile data |

**Status:** Already migrated correctly ✅

---

## 4. NAME FIELD - name vs fullName

### Issue: Some components still checking for `name` field

| File | Line | Context | Status |
|------|------|---------|--------|
| [web/components/product/worker-app.tsx](web/components/product/worker-app.tsx#L149) | 149 | reads `workerName` from profile | Using fullName in model, but variable named `workerName` for display |
| [app/src/.../worker/screens/profile/DigitalVisitingCardScreen.kt](app/src/main/java/com/example/dutype/worker/screens/profile/DigitalVisitingCardScreen.kt#L113) | 113 | reads `getString("fullName")` | ✅ Correct |

---

## 5. ROLE FIELD - Should Be Removed from Document Updates

### Issue: `role` field still present in old code paths

| File | Line | Collection | Operation | Issue |
|------|------|-----------|-----------|-------|
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L977) | 977 | `referral_events` | `.set()` | writes `userRole: getStringValue(after.activeRole \|\| after.role, "WORKER")` - fallback to old `role` |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L1397) | 1397 | `referral_events` | `.set()` | writes `userRole: getStringValue(userData.activeRole \|\| userData.role \|\| legacyStats.userRole, "WORKER")` |

**Impact:** Fallback logic to old `role` field for backward compatibility

---

## 6. FRAUD-RELATED FIELDS - Collection Issues

### Issue: `fraudSignals` written in referral-system

| File | Line | Collection | Operation | Field | Value/Context |
|------|------|-----------|-----------|-------|-------------|
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L678) | 678 | `referrals` | `.set()` | `fraudSignals` | `fraudResult.signals` array |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L1332) | 1332 | `referral_stats` | `.set()` | `fraudSignals` | `fraudResult.signals` |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L1352) | 1352 | `referrals` | `.set()` | `fraudSignals` | `fraudResult.signals` |

**Status:** Data exists in the new collections but field naming consistent

---

## 7. JOB APPLICATIONS COLLECTION NAME

### Status: ✅ CORRECT - Using `applications` collection

| File | Line | Collection | Context |
|------|------|-----------|---------|
| [app/src/.../workers/PendingApplicationNotificationWorker.kt](app/src/main/java/com/example/dutype/workers/PendingApplicationNotificationWorker.kt#L62) | 62 | `applications` | reads from correct collection |
| [web/components/product/employer-review.tsx](web/components/product/employer-review.tsx#L676) | 676 | `applications` | `.updateDoc()` on correct collection |
| [functions/src/scheduled-notifications.ts](functions/src/scheduled-notifications.ts#L568) | 568 | `applications` | reads from correct collection |
| [web/app/api/admin/applications/route.ts](web/app/api/admin/applications/route.ts#L128) | 128 | `applications` | `.set()` on correct collection |

**Status:** Already using new collection name ✅

---

## 8. REFERRAL_CODES COLLECTION - Structure OK

### Status: ✅ CORRECT - Exists and properly written

| File | Line | Operation | Fields | Status |
|------|------|-----------|--------|--------|
| [app/src/.../services/ProfileCompletionService.kt](app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt#L1026) | 1026 | const | `COLLECTION_REFERRAL_CODES = "referral_codes"` | ✅ Correct |
| [web/components/admin/admin-referral-tools-client.tsx](web/components/admin/admin-referral-tools-client.tsx#L151) | 151 | `.setDoc()` | Creates with proper structure | ✅ Correct |
| [functions/src/referral-system.ts](functions/src/referral-system.ts#L411) | 411 | `.set()` | Contains `code`, `userId`, `isActive`, `createdAt` | ✅ Correct |

---

## 9. MODERATION QUEUE REFERENCES - Deprecated Collection

### Status: ⚠️ MIGRATION IN PROGRESS

| File | Line | Old Behavior | New Behavior | Status |
|------|------|-------------|-------------|--------|
| [functions/src/index.ts](functions/src/index.ts#L547) | 547 | Would create in `moderation_queue` | Now creates in `notifications` | ✅ Migrated |
| [functions/src/index.ts](functions/src/index.ts#L672) | 672 | Would create in `moderation_queue` | Now creates in `notifications` | ✅ Migrated |

---

## 10. SAVED JOBS - Still as User Array

### Issue: Saved jobs stored as array inside users collection (should be separate collection)

| File | Line | Context | Issue |
|------|------|---------|-------|
| [app/src/.../cache/JobCacheManager.kt](app/src/main/java/com/example/dutype/cache/JobCacheManager.kt#L491) | 491 | `clearSavedJobsCache()` | References saved jobs cache but implementation not shown |
| [app/src/.../auth/AuthManager.kt](app/src/main/java/com/example/dutype/auth/AuthManager.kt#L106) | 106 | comment | "AppStateManager session (saved jobs, applications, profile state)" |

**Impact:** Saved jobs likely still stored as array in users document rather than separate collection

---

## SUMMARY OF FINDINGS

### ❌ CRITICAL ISSUES (Must Fix):

1. **`phoneNumber` field in users** (Lines 500, 607, 40, 79 in Android)
   - Still writing `phoneNumber` instead of `phone`
   - Multiple Firestore write points affected

2. **`profileCompleted` field** (7 write locations across web/functions)
   - Field written on user creation instead of computed
   - Triggers (referral-system.ts line 822) depend on this field

3. **`isActive` and `isFilled` in jobs** (7 write locations)
   - Mixed use of old and new status fields
   - Both fields written in different parts of code
   - Frontend reads both for backward compatibility

### ⚠️ MEDIUM ISSUES (Should Fix):

4. **`role` field fallback** (referral-system.ts lines 977, 1397)
   - Fallback to old `role` field for backward compatibility
   - Should remove once migration complete

5. **Saved jobs still as array**
   - Not yet seen actual write operations in audit
   - Likely stored as array in users document

### ✅ ALREADY CORRECT:

- `profileImageUrl` (was `photoUrl`)
- `applications` collection (was `job_applications`)
- `moderation_queue` references (now using `notifications`)
- `referral_codes` collection structure

---

## RECOMMENDATION - PHASED APPROACH

### Phase 1: Immediate Fixes (High Impact)
1. Fix `phoneNumber` → `phone` in Android ProfileCompletionService and FirestoreUtils
2. Stop writing `profileCompleted` field; compute on read
3. Unify job status handling (complete migration to `status` + `vacancyStatus`)

### Phase 2: Remove Fallbacks
1. Remove `role` field fallback in referral-system.ts
2. Verify no code reads from old `role` field anymore

### Phase 3: Verify/Migrate
1. Verify saved jobs storage mechanism
2. Plan migration if stored as array in users

---

## FILES REQUIRING CHANGES

**Android:**
- `app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt`
- `app/src/main/java/com/example/dutype/utils/FirestoreUtils.kt`

**Web Components:**
- `web/components/product/worker-app.tsx`
- `web/components/product/product-auth-client.tsx`
- `web/components/admin/admin-jobs-client.tsx`
- `web/components/product/employer-app.tsx`
- `web/components/product/employer-review.tsx`

**Cloud Functions:**
- `functions/src/referral-system.ts`
- `functions/src/index.ts` (job auto-rejection logic)
- `functions/src/job-posting.ts`

**Web API/Lib:**
- `web/app/api/admin/applications/route.ts`
- `web/lib/product/marketplace.ts` (normalization logic)

---

**Report Generated:** March 18, 2026
