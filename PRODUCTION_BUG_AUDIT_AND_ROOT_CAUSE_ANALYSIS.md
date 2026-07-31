# DutyPe Mobile Application — Comprehensive Production Bug Audit & System Architecture Report

**Document Date:** July 31, 2026  
**Target Application:** DutyPe (Native Android / Kotlin)  
**Primary Tech Stack:** Android SDK (Target API 36 / Min API 24), Kotlin, Jetpack Compose, Dagger Hilt, Firebase Auth & Firestore, Room DB (SQLCipher Encrypted), Kotlin Coroutines & Flow, WorkManager, Google Play Services (Location & Maps).

---

## Executive Overview
This document provides a deep-dive architectural analysis and root-cause breakdown for the 7 critical production bugs identified in DutyPe. Each section outlines the structural flaws causing the behavior, recommended production-grade Kotlin fixes, and regression safety assessments.

---

# Bug Reports & System Diagnostics

## Bug 1 & 4: Deep Lifecycle & Authentication Launch Crashes

### 1. Symptom & Problem Statement
* **Symptom A:** App crashes instantly when opened directly from the Google Play Store installer screen immediately after installation.
* **Symptom B:** App crashes when launched or re-opened immediately following a user logout session.

### 2. Deep-Dive Root Cause Analysis
* **Play Store Installer Intent Anomaly:** When an application is launched directly from the Google Play Store installer context, the root launch Intent carries specific installer flags (`FLAG_ACTIVITY_NEW_TASK` combined with installer extras). If `MainActivity` is launched with these flags while an existing task stack exists or when cold-starting without pre-initialized Hilt/Firebase singletons, intent parameter parsing or splash screen state resolution can fail with `NullPointerException` or `IllegalStateException`.
* **Logout Teardown & Lifecycle Race:** Upon logout, the application clears authentication credentials (Firebase Auth / DataStore). However, active background Firestore snapshot listeners, Room DB flow collectors, and singleton state managers (`UserSessionManager`, `JobViewModel`) continue executing in active `CoroutineScope`s. When `MainActivity` re-initializes, these listeners attempt to fetch profile/job data with a `null` or unauthenticated User ID (`uid`), triggering unhandled security exceptions or null-pointer crashes.

### 3. Recommended Architectural Fix
1. **Sanitize Launch Intents in `MainActivity`:** Implement defensive intent flag filtering in `onCreate` and `onNewIntent` to ignore installer-specific intent extras and force a clean root activity launch.
2. **Global Session Reset Protocol:** Implement a central `UserSessionManager` / `AuthRepository.logout()` teardown workflow that:
   - Cancels all active Firestore snapshot listeners.
   - Clears DataStore tokens and local Room session tables cleanly on `Dispatchers.IO`.
   - Clears in-memory ViewModel caches.
   - Navigates cleanly to the Authentication route using `popUpTo(0) { inclusive = true }`.

### 4. Regressional Safety Assessment
* Isolates authentication state teardown to the data/session layer.
* Does not alter UI rendering logic; prevents background coroutines from attempting network calls with invalid credentials.

---

## Bug 2: Memory Degradation, Session Crashing & Auth Failures

### 1. Symptom & Problem Statement
* **Symptom A:** The app runs fine initially but experiences performance degradation, lag, and crashes during extended usage sessions.
* **Symptom B:** Login and account creation flows periodically fail, freeze, or force application crashes.

### 2. Deep-Dive Root Cause Analysis
* **Memory & Listener Leaks:** ViewModels or Repository singletons registering long-lived Firestore snapshot listeners (`addSnapshotListener`) without detaching them in `onCleared()` or upon user logout. Memory accumulates as uncollected callbacks retain references to destroyed UI components and ViewModels.
* **Auth Pipeline Race Conditions:** Authentication asynchronous callbacks (Firebase Auth OTP, Google Credential Manager, Phone Auth) mutating UI state directly without thread isolation or missing comprehensive `runCatching` / `try-catch` exception handling around network/token operations.

### 3. Recommended Architectural Fix
1. **Lifecycle-Aware Listener Binding:** Ensure all Firestore snapshot listeners are wrapped inside `callbackFlow` and automatically detached when `awaitClose` is triggered or when ViewModel scope cancels.
2. **Thread Isolation & Safe Auth Pipeline:** Wrap all auth operations inside `withContext(Dispatchers.IO)` using Kotlin `Result<T>` wrappers, ensuring main-thread non-blocking execution and explicit exception handling for all network/credential failures.

### 4. Regressional Safety Assessment
* Standardizes asynchronous data streams into safe Kotlin Flows and `Result` types.
* Prevents main thread starvation and memory leakage during long user sessions.

---

## Bug 3: Storage Footprint & Storage Bloating

### 1. Symptom & Problem Statement
* The local storage footprint of the app increases continuously over time, reaching hundreds of megabytes without explicit user downloads.

### 2. Deep-Dive Root Cause Analysis
* **Uncapped Image & Lottie Caching:** Image loading libraries (Coil) and Lottie composition caches accumulating disk cache files in `app_flutter`, `cache/image_cache`, or `cache/code_cache` without strict size limits or TTL (Time-To-Live) eviction rules.
* **Firestore Offline Persistence & Room WAL Logs:** Firestore offline database cache growing indefinitely, combined with Room DB Write-Ahead Logging (`WAL`) files not being truncated during normal app operation.

### 3. Recommended Architectural Fix
1. **Configured Coil Disk Cache Budget:** Limit Coil image loader disk cache size explicitly (e.g. 50 MB) in `DutyPeApplication.kt`.
2. **Periodic Cache & Storage Pruning:** Implement a background `CacheManager` utility that auto-cleans stale temporary files and checkpoints Room DB WAL logs during app startup or background maintenance.

### 4. Regressional Safety Assessment
* Operating within defined disk budgets protects device storage without impacting image loading performance or database reliability.

---

## Bug 5: Navigation & State Loss on Employer "Hiring Room"

### 1. Symptom & Problem Statement
* When an employer posts a job and clicks on a job card to view matching applicants in the "Hiring Room", the screen displays an empty state or fails to load matching worker profiles.

### 2. Deep-Dive Root Cause Analysis
* **Navigation Parameter Loss:** The `jobId` passed via Compose Navigation route is blank, null, or improperly URL-decoded upon entering `HiringRoomScreen`.
* **Asynchronous Race Condition:** `HiringRoomViewModel` initiates the matching algorithm before validating the input `jobId` or employer location state, causing the query to execute with empty parameters and return 0 results.

### 3. Recommended Architectural Fix
1. **Defensive Navigation Guard:** Validate `jobId` parameters immediately upon route entry. Fallback gracefully to default states or log errors if parameters are invalid.
2. **UI State Machine Implementation:** Enforce a strict state flow (`Loading` -> `Success(workers)` -> `Empty` / `Error`) in `HiringRoomViewModel`, ensuring data fetch completes before UI renders.

### 4. Regressional Safety Assessment
* Fixes parameter validation at the view model level without changing the visual design of the Hiring Room screen.

---

## Bug 6 & 7: Location, Filtration & Geospatial Search Failures

### 1. Symptom & Problem Statement
* **Symptom A:** Nearby jobs fail to show on the Worker Home Screen or "All Jobs" screen despite valid location permissions.
* **Symptom B:** Category filtering, distance radius search, and keyword searching on the "All Jobs" screen do not return expected results or fail silently.

### 2. Deep-Dive Root Cause Analysis
* **Location Handling Edge Cases:** If GPS location lookup returns `null` or defaults to `0.0, 0.0`, distance calculation algorithms (Haversine formula) fail or calculate astronomically high distances, filtering out valid jobs.
* **Filter Payload Mismatch:** Search queries and category filter payloads passing empty strings or mismatched key structures into Firestore or in-memory job filters, resulting in query mismatch.

### 3. Recommended Architectural Fix
1. **Robust Spatial Location Fallback:** Provide fallback to employer/worker saved profile location if device GPS location is temporarily unavailable.
2. **Sanitized Geospatial Filtering Pipeline:** Cleanly compute distances using a verified Haversine distance algorithm, filtering jobs against normalized categories, search keywords, and distance thresholds.

### 4. Regressional Safety Assessment
* Enhances the accuracy of search and filtering algorithms while maintaining fallback safety for devices with disabled GPS.

---

## Verification & Build Checklist
- [x] R8 / ProGuard rules configured to preserve Lifecycle, Activity, Hilt, Room, and SQLCipher classes.
- [x] Native 64-bit 16 KB ELF segment alignment verified (`arm64-v8a`, `x86_64`).
- [x] Zero `-applymapping` baseline corruption in release build pipeline (`v804` / `3.9`).
