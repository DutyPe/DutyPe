# DUAL ROLE BUG FIX - CRITICAL

## THE BUG (Found by Deep Code Analysis)

### Problem Statement
When a user switches from Worker to Employer (or vice versa), their original role was being OVERWRITTEN instead of ADDED to the roles array.

### Root Cause Analysis

#### In `MandatoryEmployerProfileSetupScreen.kt` (Line 328):
```kotlin
val employerProfileData = mutableMapOf(
    "companyName" to companyName,
    "fullName" to companyName,
    "contactEmail" to contactEmail,
    "contactPhone" to contactPhone,
    "phone" to contactPhone,
    "businessAddress" to businessAddress,
    "industry" to industry,
    "companySize" to companySize,
    "gender" to gender,
    "dateOfBirth" to dateOfBirth,
    "role" to "EMPLOYER",  // ← BUG: This overwrites the single "role" field
    "profileCompleted" to true,
    ...
)
```

#### In `MandatoryWorkerProfileSetupScreen.kt` (Line 741):
```kotlin
val workerProfileData = mutableMapOf(
    "fullName" to fullName,
    "email" to email,
    "phone" to phoneNumber,
    "address" to address,
    "dateOfBirth" to dateOfBirth,
    "gender" to gender,
    "skills" to skills,
    "experience" to experience,
    "role" to "WORKER",  // ← BUG: This overwrites the single "role" field
    "profileCompleted" to true,
    ...
)
```

### The Exact Sequence of Events (What Was Happening):

1. **Initial State**: User is Worker
   ```json
   {
     "role": "WORKER",
     "roles": ["WORKER"],
     "activeRole": "WORKER",
     "skills": "Plumber"
   }
   ```

2. **User switches to Employer** and completes profile setup

3. **Line 328 creates profile data** with `"role" to "EMPLOYER"`

4. **Line 353 saves this data** using `SetOptions.merge()`
   - This OVERWRITES the `role` field to "EMPLOYER"
   - Result after save:
   ```json
   {
     "role": "EMPLOYER",  // ← OVERWRITTEN!
     "roles": ["WORKER"],  // ← Still has old value
     "activeRole": "WORKER",
     "skills": "Plumber"
   }
   ```

5. **Lines 357-380 try to fix it** by updating the `roles` array
   - Reads current roles: `["WORKER"]`
   - Adds "EMPLOYER": `["WORKER", "EMPLOYER"]`
   - Updates `roles` and `activeRole`
   - Final result:
   ```json
   {
     "role": "EMPLOYER",  // ← WRONG! Should not exist or should be array
     "roles": ["WORKER", "EMPLOYER"],  // ← Correct
     "activeRole": "EMPLOYER",  // ← Correct
     "skills": "Plumber",
     "companyName": "ABC Company"
   }
   ```

6. **The Problem**: The single `role` field is now inconsistent with the `roles` array
   - `role` says "EMPLOYER" (singular)
   - `roles` says ["WORKER", "EMPLOYER"] (array)
   - This causes confusion in the app logic

### Why This Was Hard to Detect

1. The `roles` array update code (lines 357-380) was working correctly
2. The bug was in the profile data map creation (line 328)
3. The two operations happened in sequence, making it look like it was working
4. The `role` field (singular) and `roles` field (plural) are TWO DIFFERENT FIELDS in Firestore

## THE FIX

### Solution
Remove the `"role"` field from BOTH profile data maps. The roles array update code is already in place and working correctly.

### Changes Made

#### 1. `MandatoryWorkerProfileSetupScreen.kt` (Line 741)
**BEFORE:**
```kotlin
val workerProfileData = mutableMapOf(
    "fullName" to fullName,
    "email" to email,
    "phone" to phoneNumber,
    "address" to address,
    "dateOfBirth" to dateOfBirth,
    "gender" to gender,
    "skills" to skills,
    "experience" to experience,
    "role" to "WORKER",  // ← REMOVED
    "profileCompleted" to true,
    "completedAt" to System.currentTimeMillis()
)
```

**AFTER:**
```kotlin
val workerProfileData = mutableMapOf(
    "fullName" to fullName,
    "email" to email,
    "phone" to phoneNumber,
    "address" to address,
    "dateOfBirth" to dateOfBirth,
    "gender" to gender,
    "skills" to skills,
    "experience" to experience,
    // REMOVED: "role" to "WORKER" - this was overwriting the single role field
    // The roles array is updated separately below (lines 753-776)
    "profileCompleted" to true,
    "completedAt" to System.currentTimeMillis()
)
```

#### 2. `MandatoryEmployerProfileSetupScreen.kt` (Line 328)
**BEFORE:**
```kotlin
val employerProfileData = mutableMapOf(
    "companyName" to companyName,
    "fullName" to companyName,
    "contactEmail" to contactEmail,
    "contactPhone" to contactPhone,
    "phone" to contactPhone,
    "businessAddress" to businessAddress,
    "industry" to industry,
    "companySize" to companySize,
    "gender" to gender,
    "dateOfBirth" to dateOfBirth,
    "role" to "EMPLOYER",  // ← REMOVED
    "profileCompleted" to true,
    "completedAt" to System.currentTimeMillis(),
    "isSelfieVerified" to (uploadedSelfieUrl != null),
    "completedJobsCount" to 0
)
```

**AFTER:**
```kotlin
val employerProfileData = mutableMapOf(
    "companyName" to companyName,
    "fullName" to companyName,
    "contactEmail" to contactEmail,
    "contactPhone" to contactPhone,
    "phone" to contactPhone,
    "businessAddress" to businessAddress,
    "industry" to industry,
    "companySize" to companySize,
    "gender" to gender,
    "dateOfBirth" to dateOfBirth,
    // REMOVED: "role" to "EMPLOYER" - this was overwriting the single role field
    // The roles array is updated separately below (lines 357-380)
    "profileCompleted" to true,
    "completedAt" to System.currentTimeMillis(),
    "isSelfieVerified" to (uploadedSelfieUrl != null),
    "completedJobsCount" to 0
)
```

## EXPECTED BEHAVIOR AFTER FIX

### Scenario 1: New User Signs Up as Worker
1. User completes Worker profile
2. Firestore document:
   ```json
   {
     "fullName": "John Doe",
     "phone": "+919876543210",
     "roles": ["WORKER"],
     "activeRole": "WORKER",
     "skills": "Plumber",
     "experience": "5 years"
   }
   ```
3. ✅ No `role` field (singular) - only `roles` array

### Scenario 2: Worker Enables Employer Role
1. Worker switches to Employer and completes profile
2. Profile data saved WITHOUT `"role"` field
3. Roles array updated separately
4. Firestore document:
   ```json
   {
     "fullName": "John Doe",
     "phone": "+919876543210",
     "roles": ["WORKER", "EMPLOYER"],
     "activeRole": "EMPLOYER",
     "skills": "Plumber",
     "experience": "5 years",
     "companyName": "ABC Company",
     "industry": "Construction"
   }
   ```
5. ✅ Both roles preserved, no overwriting

### Scenario 3: Employer Enables Worker Role
1. Employer switches to Worker and completes profile
2. Profile data saved WITHOUT `"role"` field
3. Roles array updated separately
4. Firestore document:
   ```json
   {
     "fullName": "ABC Company",
     "phone": "+919876543210",
     "roles": ["EMPLOYER", "WORKER"],
     "activeRole": "WORKER",
     "companyName": "ABC Company",
     "industry": "Construction",
     "skills": "Plumber",
     "experience": "5 years"
   }
   ```
5. ✅ Both roles preserved, no overwriting

### Scenario 4: Switching Between Roles
1. User has both roles: `["WORKER", "EMPLOYER"]`
2. User switches from Worker to Employer
3. Only `activeRole` is updated to "EMPLOYER"
4. Firestore document:
   ```json
   {
     "roles": ["WORKER", "EMPLOYER"],
     "activeRole": "EMPLOYER"  // ← Only this changes
   }
   ```
5. ✅ No data loss, instant switch

## VERIFICATION STEPS

### 1. Test New Worker Registration
```bash
# Expected: roles: ["WORKER"], activeRole: "WORKER"
# No "role" field should exist
```

### 2. Test Worker → Employer Switch
```bash
# Before: roles: ["WORKER"]
# After: roles: ["WORKER", "EMPLOYER"], activeRole: "EMPLOYER"
# Worker data (skills, experience) should be preserved
```

### 3. Test Employer → Worker Switch
```bash
# Before: roles: ["EMPLOYER"]
# After: roles: ["EMPLOYER", "WORKER"], activeRole: "WORKER"
# Employer data (companyName, industry) should be preserved
```

### 4. Test Role Switching
```bash
# User with roles: ["WORKER", "EMPLOYER"]
# Switch to Worker: activeRole: "WORKER"
# Switch to Employer: activeRole: "EMPLOYER"
# All data should persist
```

## RELATED FILES

- `app/src/main/java/com/example/dutype/worker/screens/MandatoryWorkerProfileSetupScreen.kt` (Line 741)
- `app/src/main/java/com/example/dutype/employer/screens/MandatoryEmployerProfileSetupScreen.kt` (Line 328)
- `app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt`
- `app/src/main/java/com/example/dutype/models/User.kt`
- `firestore.rules`

## FIRESTORE RULES

The Firestore rules already support dual roles correctly:

```javascript
// Dual-role support: roles is an array
match /users/{userId} {
  allow read: if request.auth != null;
  allow create: if request.auth != null && request.auth.uid == userId;
  allow update: if request.auth != null && request.auth.uid == userId
    && (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['roles']))
    || (request.resource.data.roles is list 
        && request.resource.data.roles.hasAll(resource.data.roles));
}
```

## STATUS

✅ **FIXED** - The bug has been identified and resolved by removing the `"role"` field from profile data maps.

The roles array update code was already working correctly. The issue was that the profile data map was overwriting the single `role` field before the roles array could be updated.

## NEXT STEPS

1. ✅ Remove `"role"` field from Worker profile data map
2. ✅ Remove `"role"` field from Employer profile data map
3. ⏳ Test dual role switching in the app
4. ⏳ Verify Firestore documents have correct structure
5. ⏳ Deploy and monitor for any issues

---

**Date**: 2026-03-10
**Fixed By**: Kiro AI Assistant
**Severity**: CRITICAL
**Impact**: Dual role functionality was broken - users could not maintain both roles simultaneously
