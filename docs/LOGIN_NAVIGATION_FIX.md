# Login Navigation Fix - Role-Based Redirection

## Problem Statement

When users tried to login from the Worker profile screen, they were being redirected to the Employer home screen (if they were existing Employer users), instead of the Worker home screen. This violated the expected behavior where users should be redirected to the home screen corresponding to the role they selected when clicking login.

## Root Cause Analysis

The issue had multiple contributing factors:

### 1. Missing Role Parameter in Navigation (ALREADY FIXED)
- **Location**: `WorkerProfile.kt` and `EmployerProfileScreen.kt`
- **Issue**: Login buttons were navigating to `Routes.ENHANCED_LOGIN` without passing the role parameter
- **Status**: ✅ Already fixed - both screens now pass the role parameter explicitly

### 2. Using Database Role Instead of Intended Role (ALREADY FIXED)
- **Location**: `EnhancedLoginScreen.kt` (lines 185-210)
- **Issue**: After OTP verification, the code was using `parsedRole` (from Firestore) instead of `role` (from navigation parameter)
- **Status**: ✅ Already fixed - code now uses the `role` parameter consistently

### 3. Inconsistent Role Field Updates (FIXED IN THIS SESSION)
- **Location**: `ProfileCompletionService.kt` and `FirestoreUtils.kt`
- **Issue**: When updating user role, only the `role` field was updated, not the `activeRole` field
- **Impact**: On app restart, MainNavGraph reads `activeRole` from Firestore, which was outdated
- **Status**: ✅ FIXED - Now updates both `role` and `activeRole` fields

## Changes Made

### 1. ProfileCompletionService.kt

#### updateUserRole() Method
```kotlin
// BEFORE
firestore.collection("users").document(currentUser.uid)
    .update("role", newRole)
    .await()

// AFTER
val updates = mapOf(
    "role" to newRole,
    "activeRole" to newRole
)
firestore.collection("users").document(currentUser.uid)
    .update(updates)
    .await()
```

#### saveUserInfo() Method
```kotlin
// BEFORE
val userData = mapOf(
    "email" to email,
    "name" to name,
    "role" to role,
    "createdAt" to System.currentTimeMillis()
)

// AFTER
val userData = mapOf(
    "email" to email,
    "name" to name,
    "role" to role,
    "activeRole" to role,  // CRITICAL: Set activeRole for dual-role support
    "createdAt" to System.currentTimeMillis()
)
```

### 2. FirestoreUtils.kt

#### updateUserRole() Method
```kotlin
// BEFORE
firestore.collection("users")
    .document(userId)
    .update("role", role)
    .await()

// AFTER
val updates = mapOf(
    "role" to role,
    "activeRole" to role
)
firestore.collection("users")
    .document(userId)
    .update(updates)
    .await()
```

## Flow After Fix

### Scenario: Existing Employer User Logs in from Worker Profile

1. User is on Worker Home (not logged in)
2. User clicks "Login" button on Worker Profile screen
3. Navigation: `rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")`
4. EnhancedLoginScreen receives `initialRole = "WORKER"`
5. User enters phone number and OTP
6. After OTP verification:
   - Fetches user data from Firestore
   - Finds existing user with `role = "EMPLOYER"` and `activeRole = "EMPLOYER"`
   - **CRITICAL**: Uses `role` parameter (WORKER) instead of database role (EMPLOYER)
   - Calls `profileCompletionViewModel.updateUserRole(role)` with WORKER
   - This updates both `role` and `activeRole` to "WORKER" in Firestore
   - Navigates to `Routes.WORKER_HOME`
7. User is now on Worker Home screen ✅

### Scenario: New User Registers from Worker Profile

1. User is on Worker Home (not logged in)
2. User clicks "Login" button on Worker Profile screen
3. Navigation: `rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")`
4. EnhancedLoginScreen receives `initialRole = "WORKER"`
5. User enters phone number and OTP
6. After OTP verification:
   - No existing user data found
   - Calls `profileCompletionViewModel.updateUserRole(role)` with WORKER
   - This creates user document with both `role` and `activeRole` set to "WORKER"
   - Navigates to `Routes.PROFILE_SETUP` (Worker profile setup)
7. After profile setup, user is on Worker Home screen ✅

## Testing Checklist

- [ ] Test login from Worker profile screen as new user
- [ ] Test login from Worker profile screen as existing Worker user
- [ ] Test login from Worker profile screen as existing Employer user
- [ ] Test login from Employer profile screen as new user
- [ ] Test login from Employer profile screen as existing Employer user
- [ ] Test login from Employer profile screen as existing Worker user
- [ ] Verify app restart after login maintains correct role
- [ ] Verify role switching works correctly

## Related Files

- `app/src/main/java/com/example/dutype/auth/EnhancedLoginScreen.kt`
- `app/src/main/java/com/example/dutype/worker/screens/profile/WorkerProfile.kt`
- `app/src/main/java/com/example/dutype/employer/screens/profilescreen/EmployerProfileScreen.kt`
- `app/src/main/java/com/example/dutype/services/ProfileCompletionService.kt`
- `app/src/main/java/com/example/dutype/utils/FirestoreUtils.kt`
- `app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt`

## Notes

- The fix ensures dual-role support: users can have both Worker and Employer roles
- The `activeRole` field indicates which role the user is currently using
- The `role` field is kept for backward compatibility
- Both fields are now updated consistently across the codebase
