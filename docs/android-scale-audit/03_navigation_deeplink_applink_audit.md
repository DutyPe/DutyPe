# 03 — Navigation, Deep Link & App Link Audit

---

## A. Routes inventory

- **Source of truth:** [`navigation/Routes.kt`](../../app/src/main/java/com/example/dutype/navigation/Routes.kt) — 61 `const val` route IDs + 6 helper builder functions.
- **NavGraph files:**
  - `MainNavGraph.kt` — root graph (auth, onboarding, role selection, profile setup, deep-link broadcast receiver).
  - `WorkerNavGraph.kt` — worker tab graph.
  - `WorkerMainScreen.kt` — bottom-bar host for worker.
  - `EmployerMainScreen.kt` — bottom-bar host for employer.
- **Pattern:** All `composable("...")` calls use `Routes.*` constants — **no inline string literals as route declarations** (verified by grep).

## B. Drift / inconsistency findings

| # | Finding | Evidence | Severity |
|---|---------|----------|----------|
| 1 | Inline string in `popUpTo()` instead of `Routes.*` | [PostJobScreen.kt L559](../../app/src/main/java/com/example/dutype/employer/screens/PostJobScreen.kt#L559) `popUpTo("employer_home")` | P1 |
| 2 | `popUpTo(0) { inclusive = true }` in auth flow — opaque clear | [RegisterScreen.kt L248](../../app/src/main/java/com/example/dutype/auth/RegisterScreen.kt#L248) | P3 |
| 3 | Three NavGraph "owners" (MainNavGraph + WorkerMainScreen + EmployerMainScreen) instead of one root + child graphs | All three files | P2 |
| 4 | Stringly typed routes — no Kotlin Serialization type-safe destinations | All of `Routes.kt` | P2 |
| 5 | Start destination computed asynchronously across DataStore + Firestore | [MainNavGraph.kt L73-180](../../app/src/main/java/com/example/dutype/navigation/MainNavGraph.kt#L73) | P1 |
| 6 | `popUpTo` uses mixed inclusive flags without convention | Throughout | P3 |

## C. Deep link inventory

### Manifest declarations — [AndroidManifest.xml L100-135](../../app/src/main/AndroidManifest.xml#L100)

```
scheme="dutype"  hosts: job, worker, refer, application, employer, chat, profile, notifications, home
scheme="https"   host:  dutype.in   (autoVerify="true")
```

### Handler — [`utils/DeepLinkHandler.kt`](../../app/src/main/java/com/example/dutype/utils/DeepLinkHandler.kt)

| URI Pattern | Routes to |
|-------------|-----------|
| `dutype://job/{jobId}` | Job detail |
| `dutype://worker/{workerId}` | Worker profile |
| `dutype://worker/applications/{appId}` | Application detail |
| `dutype://worker/applications` | Applications list |
| `dutype://worker/jobs` | Jobs list |
| `dutype://worker/profile` | Worker profile |
| `dutype://refer/{code}` | Referral entry |
| `dutype://employer/...` | Employer flows |
| `dutype://chat/...` | (Chat removed — orphan?) |
| `dutype://profile/...` | Profile |
| `dutype://notifications` | Notifications |
| `dutype://home` | Home |
| `https://dutype.in/jobs/{jobId}` | Job detail (web) |
| `https://dutype.in/refer/{code}` | Referral (web) |
| `https://dutype.in/worker/{workerId}` | Worker profile (web) |

### Manifest ↔ handler mapping

| Status | Notes |
|--------|-------|
| ✅ All 9 dutype hosts have routing logic | — |
| ⚠️ `chat` host exists in manifest but chat feature was removed | Orphan host — safe but noisy |
| ⚠️ Web URL coverage limited (jobs, refer, worker only) — manifest accepts ALL `dutype.in` paths | Untested paths fall through; verify desired behavior |
| ✅ Path params parsed defensively (most cases) | One or two missing `null` checks (audit per call site) |

## D. App Link verification — Critical gap

| Check | Status |
|-------|--------|
| `android:autoVerify="true"` set | ✅ |
| HTTPS only | ✅ |
| `assetlinks.json` published at `https://dutype.in/.well-known/assetlinks.json` | ❌ **Missing** |
| Release SHA-256 fingerprint declared | ❌ **N/A — file missing** |
| Debug fingerprint declared (for QA) | ❌ |
| `adb shell pm verify-app-links` documented | ❌ |

**Impact:** Android verifier silently fails → links open in chooser, never auto-launching the app. **All marketing share / referral SMS / push deep-link flows degrade.**

## E. Deep-link plumbing complexity

Current path of a single deep link from FCM tap → screen:

```
FCM payload → DutyPeMessagingService.onMessageReceived
  → Notification builder with PendingIntent → MainActivity.intent extras
    → MainActivity.onNewIntent → broadcast Intent (notificationId)
      → MainNavGraph.LaunchedEffect (BroadcastReceiver registered)
        → DeepLinkHandler.handleDeepLinkUri(uri)
          → NavController.navigate(route)
```

**6 indirections** for one navigation event.

**Recommended path:**
```
FCM payload → PendingIntent with deep-link URI
  → MainActivity (singleTask) → NavController auto-handles via composable() deepLinks block
    → screen rendered
```

**3 indirections** with built-in Compose Navigation primitives.

## F. Recommended canonical routing model

1. **Type-safe destinations** (Compose Navigation 2.8 + Kotlin Serialization):
   ```kotlin
   @Serializable data object WorkerHome
   @Serializable data class JobDetail(val jobId: String)
   @Serializable data class WorkerProfile(val workerId: String)
   ```
2. **Single `NavHost`** in MainActivity; nested graphs by feature.
3. **`deepLinks = listOf(navDeepLink { uriPattern = ... })`** declared inline on each `composable<Destination>`.
4. **`DeepLinkHandler` retired** in favor of Navigation's built-in matching; FCM service constructs PendingIntent with deep-link URI directly.
5. **Single `StartDestinationResolver`** that returns synchronously from cached state and reconciles asynchronously.

## G. Action checklist (in order)

1. ✅ Publish `web/.well-known/assetlinks.json` with release fingerprint (P0).
2. ✅ Replace `popUpTo("employer_home")` with `Routes.EMPLOYER_HOME` (P1, 5 min).
3. ✅ Verify `chat` deep-link host is intentional or remove (P2).
4. ✅ Remove broadcast indirection — use `composable(deepLinks = ...)` (P2).
5. ✅ Migrate routes to type-safe (P2, phased).
6. ✅ Cache start destination synchronously (P1).
7. ✅ Document deep-link contract in repo `README.md` so feature owners can register new ones consistently.
