# Release Validation — DutyPe v2.6.5+

This is the handoff document for the R8 / ProGuard rewrite that landed in commit
`c1197b6` (and the follow-up `BuildConfig` reflection fix). Everything you need
to verify the next signed release is safe — and the recipe to fix any specific
crash that shows up — lives in this file.

> Audience: future you, or another AI on a different machine. No prior context
> assumed.

---

## 0. Production-correctness fixes shipped on top of the R8 rewrite

After the proguard rewrite landed, an audit against official Android / Firebase
guidance turned up three production gaps. All three are now fixed:

1. **Mobile Ads SDK was never initialized.** `AdManager.initialize()` existed
   but had zero call sites — `MobileAds` was relying on lazy auto-init on the
   first ad load, delaying the first ad and skipping proper SDK setup. Now
   `AdManager` is `@Inject`ed into `DutyPeApplication` and initialized from
   `initializeNonCriticalComponents()` on a background coroutine.
   - **NOT yet fixed**: Google UMP (User Messaging Platform) consent. EU/UK/
     Brazil traffic will receive non-personalized ads until a UMP `ConsentForm`
     is wired into `MainActivity.onCreate`. AdMob TOS technically requires
     this for that traffic. Tracked as a separate UI change.
2. **Crashlytics mapping-file upload was disabled** in `app/build.gradle.kts`
   via a `tasks.findByName("uploadCrashlyticsMappingFileRelease")?.enabled = false`
   workaround for a DNS issue. That meant every release stack trace in the
   Crashlytics console was obfuscated. The disable line is removed; the build
   now uploads mappings normally. If your local network blocks
   `firebasecrashlyticssymbols.googleapis.com`, the build still succeeds — run
   `./gradlew :app:uploadCrashlyticsMappingFileRelease` later from a network
   that works, or run release builds from CI.
3. **Firebase Analytics + Performance SDKs shipped but never used.** Zero
   `logEvent(...)` calls, zero `Trace.start(...)` calls. Now there's a tiny
   `com.example.dutype.analytics.Analytics` helper that wraps
   `FirebaseAnalytics`, initialized once from `initializeNonCriticalComponents()`,
   with four typed events instrumented at the highest-leverage business
   actions:
   - `job_apply` — `JobApplicationService.submitApplication` success path.
   - `job_post` — `JobFirestoreService.createJob` success path.
   - `otp_verified` — `OtpViewModel.signInWithPhoneAuthCredential` success path.
   - `rewarded_ad_completed` — both employer and worker rewarded-ad earned
     callbacks in `AdManager`.

   Auto-collected events (`first_open`, `session_start`, `screen_view`) start
   flowing as soon as `Analytics.init(this)` runs. Performance SDK is left on
   the classpath because it auto-records app-start, screen-rendering and HTTP
   metrics with no code; remove it from `app/build.gradle.kts` if nobody opens
   the Firebase Performance dashboard.

---

## 1. What changed and why

### 1.1 The rewrite (commit `c1197b6`)

`app/proguard-rules.pro` went from **386 lines / 151 nuclear `-keep` rules** to
**~129 lines, surgical only**.

The old file had blanket keeps like:

```
-keep class androidx.compose.** { *; }
-keep class com.google.firebase.** { *; }
-keep class androidx.lifecycle.** { *; }
... (and ~140 more)
```

These defeated R8 entirely. R8 was retaining ~830,000 mapping entries from
`material-icons-extended` alone (every icon class survived, even though only
~220 are used).

The rewrite trusts each library's own bundled `consumer-rules.pro` (which is
how AndroidX / Firebase / Hilt / Coil / Room / Work / Datastore / Maps ship
their rules) and keeps only what the app itself needs reflectively.

### 1.2 The follow-up fix (`DutyPeApplication.initializeGoogleMapsServices`)

The old code was:

```kotlin
val mapsKey = try {
    BuildConfig::class.java.getField("MAPS_API_KEY").get(null) as? String ?: ""
} catch (e: Exception) { "" }
```

R8 inlines `BuildConfig` `String` constants into call sites — **the field does
not exist in the release dex**, so `getField("MAPS_API_KEY")` always threw
`NoSuchFieldException`, the catch swallowed it, and Places SDK silently never
initialized in production. Now it reads `BuildConfig.MAPS_API_KEY` directly.

### 1.3 Local size impact (debug-comparable measurements)

| Artifact                | Before     | After      | Delta            |
| ----------------------- | ---------- | ---------- | ---------------- |
| `classes*.dex` (total)  | 49.4 MB    | 17.0 MB    | **−32.4 MB / −65 %** |
| Number of dex files     | 5          | 2          | −3               |
| `mapping.txt`           | 250 MB     | 128 MB     | −122 MB          |
| `resources.pb`          | 21 MB      | 19 MB      | −2 MB            |
| Native libs (per ABI)   | unchanged  | unchanged  | 0                |

Play Console "Size for updates" should drop materially once Play has both the
old and new release on its diff baseline.

---

## 2. Verification checklist (already passed locally)

- [x] **R8 actually ran.** `bundleRelease` produced
      `app/build/intermediates/intermediary_bundle/release/.../intermediary-bundle.aab`,
      `mapping.txt` is fresh, dex shrank to 17 MB.
- [x] **Baseline Profile bundled.** AAB contains `assets/dexopt/baseline.prof`
      (7741 B) and `assets/dexopt/baseline.profm` (788 B).
- [x] **Native libs unchanged.** `libsqlcipher.so`, `libandroidx.graphics.path.so`,
      `libdatastore_shared_counter.so` present per ABI; sqlcipher dominates
      (~2 MB / ABI) but is unaffected by R8.
- [x] **No Firestore reflective deserialization.** Zero `.toObject()` /
      `.toObjects()` calls in the codebase — manual parsing throughout, so model
      class member keeps are not load-bearing for Firestore.
- [x] **Reflection audit clean.** 11 reflective callsites reviewed; only the
      `BuildConfig.MAPS_API_KEY` one was broken (now fixed). The other notable
      ones:
      - `Class.forName("...DebugAppCheckProviderFactory")` → DEBUG-gated, falls
        back to Play Integrity if missing. Safe.
      - `auth.firebaseAuthSettings.javaClass.getMethod("forceRecaptchaFlowForTesting", ...)`
        → DEBUG-gated. Safe.
      - `gson.fromJson(...)` calls in Converters / AuthManager / DataStoreExt /
        JobPostingWorker → safe because `com.example.dutype.models.**` is kept.
- [x] **HiltWorkers + FCM service intact in manifest.** `DutyPeMessagingService`
      declared at line 115; WorkManager default initializer correctly removed
      via `androidx.startup.InitializationProvider` `tools:node="remove"` (Hilt
      WorkerFactory is wired through `Configuration.Provider`).

---

## 3. Pre-publish smoke test (signed release on real device)

Build a signed release AAB, install via `bundletool` or push to Internal
testing, then walk this list:

1. **Cold launch** from launcher icon. Splash → Main. No ANR, no crash. (Time
   it; baseline profile should make this materially faster than the previous
   release on Pixel 4a / Galaxy A series.)
2. **Login (worker)** with phone OTP. Confirm OTP delivery + token acquisition
   (App Check Play Integrity should silently succeed).
3. **Login (employer)**. Same flow.
4. **Post a job** (employer). Save → list refresh → details screen.
5. **Apply to a job** (worker). Triggers `JobPostingWorker` /
   `JobSyncWorker`-adjacent flows; confirm WorkManager runs without
   `ClassNotFoundException`.
6. **Profile → Edit profile** (both roles). Image upload to Firebase Storage.
7. **Refer & Earn** screen. Deep link share (`dutype://refer`).
8. **Support** screen.
9. **Background**: kill app from recents, relaunch from a notification or
   deep link (`https://dutype.in/...` web link).
10. **FCM push receipt**: trigger a server push, confirm `DutyPeMessagingService`
    receives it (foreground + killed state).
11. **Places autocomplete** in any "select location" field — this was previously
    silently broken and is now expected to work.
12. **Background WorkManager jobs**: leave the app for ~6 h (or force-trigger via
    `adb shell cmd jobscheduler run -f com.dutype.app <jobId>`) and confirm
    `PendingApplicationNotificationWorker` fires.
13. **Maps tile loading** on the job-details map preview.
14. **Rewarded ads** (if surfaced in your test account) — confirm `AdManager`
    serves an ad.
15. **Size regression check.** After any `proguard-rules.pro` edit, run
    `./gradlew :app:bundleRelease` and confirm the new AAB's total dex is still
    around 17 MB. PowerShell:

    ```powershell
    $aab = "app\build\outputs\bundle\release\app-release.aab"
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::OpenRead((Resolve-Path $aab)).Entries |
      Where-Object { $_.FullName -like "*.dex" } |
      Select-Object FullName, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}
    ```

    If total dex jumped by more than ~1 MB versus the c1197b6 baseline, your
    last `-keep` rule is too broad. Revert it and reread §4.0.

If any step hangs, crashes, or silently no-ops, jump to §4 to find the surgical
fix.

---

## 4. Per-crash recipes (paste-and-go)

> All edits below go into `app/proguard-rules.pro`. After editing, rerun
> `./gradlew :app:bundleRelease` and re-test.

### 4.0 SIZE DISCIPLINE — read this before adding any rule

The whole point of commit `c1197b6` was that **broad `-keep` rules silently
undo R8** and bring the 17–18 MB Play update size back. Every recipe in this
section is written to keep R8 effective. Apply them as written.

**Hard rules — never break these:**

1. **No package wildcards on third-party libraries.** Never write
   `-keep class androidx.**`, `com.google.firebase.**`, `com.google.android.gms.**`,
   `androidx.compose.**`, `androidx.lifecycle.**`, `androidx.navigation.**`,
   `coil.**`, `com.airbnb.lottie.**`, `com.google.accompanist.**`,
   `com.google.android.libraries.places.**`, `com.google.android.gms.maps.**`,
   or `kotlin.**`. These libraries already ship their own `consumer-rules.pro`.
   Adding a wildcard on top is what created the original 32 MB of dead dex.
2. **`-keep class com.example.dutype.** { *; }` is forbidden.** Keep only the
   specific model / entry-point / worker that fails.
3. **Always prefer `-keepclassmembers` over `-keep`.** `-keep` keeps the class
   AND its members AND blocks renaming. `-keepclassmembers` only keeps members
   on classes R8 already kept for other reasons — much cheaper.
4. **Never add `-dontobfuscate` or `-dontshrink` or `-dontoptimize`.** These
   are size killers. They're not in the current file; keep it that way.
5. **One rule per crash, not preemptive.** Don't add rules "just in case".
   Wait for an actual stack trace, then add the narrowest rule that fixes it.
6. **After adding any rule, re-check size.** Run `./gradlew :app:bundleRelease`
   and confirm `app/build/outputs/bundle/release/app-release.aab` total dex
   (`unzip -l app-release.aab | grep dex`) is still ~17 MB, not 30 MB+.
   If it grew by more than ~500 KB, your rule is too broad — narrow it.

If your fix needs more than 5 lines added to `proguard-rules.pro`, you are
almost certainly doing it wrong. Stop and reread §4.0.

### 4.1 `ClassNotFoundException: com.foo.bar.Baz` at app start

R8 stripped a class loaded by reflection, JNI, the manifest, or a service
loader. Always add the **single class**, never the package:

```pro
-keep class com.foo.bar.Baz { *; }
```

**Forbidden** (will re-bloat dex):

```pro
# DO NOT DO THIS
-keep class com.foo.bar.** { *; }
```

If the SDK genuinely needs many of its own classes kept, that means it forgot
to ship a `consumer-rules.pro`. File a bug against the SDK, then keep ONLY
the specific subpackage that the stack trace points at — never the SDK root.

### 4.2 Firestore warning: `No setter/field for X found on class Y`

Should not happen in this codebase (we don't use `.toObject()`), but if a new
caller adds it, keep that model's members:

```pro
-keepclassmembers class com.example.dutype.models.<ModelName> {
    <init>();
    *;
}
```

### 4.3 Hilt: `IllegalStateException: Hilt Activity must be attached to ...`
or `Multiple entries with same key`

Already covered by `-keep class dagger.hilt.android.internal.** { *; }` in the
current rules. If a brand-new Hilt entry-point still fails, add it explicitly:

```pro
-keep class com.example.dutype.<package>.<EntryPointClass> { *; }
```

### 4.4 `IllegalArgumentException: Could not instantiate ...Worker`
(WorkManager / Hilt-Work)

The `@HiltWorker` class was renamed and Hilt's `WorkerAssistedFactory` lookup
fails. Keep ONLY the specific worker that crashed (the stack trace names it):

```pro
-keep class com.example.dutype.workers.PendingApplicationNotificationWorker { <init>(...); }
```

The four workers in this app are: `PendingApplicationNotificationWorker`,
`GuestEngagementWorker`, `JobSyncWorker`, `JobPostingWorker`. Add a rule for
each one ONLY if it actually crashes. Do **not** preemptively add the broad
`-keep class * extends androidx.work.ListenableWorker { <init>(...); }` —
that keeps every worker class in every transitively-pulled library and grows
dex unnecessarily.

### 4.5 FCM push not delivered

The service is declared by fully-qualified name in the manifest (line 115),
so R8 already keeps it via the manifest reference. If you ever switch to a
class-literal manifest reference and it breaks, add ONLY this one line:

```pro
-keep class com.example.dutype.services.DutyPeMessagingService { *; }
```

Do **not** add `-keep class com.example.dutype.services.** { *; }` — most
classes in that package don't need full member retention.

### 4.6 `RuntimeException: Unable to get provider ...MobileAdsInitProvider`

Already kept in the current rules — there should be nothing to do. If a future
ads SDK upgrade ever breaks it again, the existing three lines in
`proguard-rules.pro` (`MobileAdsInitProvider`, `AdvertisingIdClient`,
`com.google.android.gms.common.internal.safeparcel.**`) are the maximum that
should ever be there. **Do not add `-keep class com.google.android.gms.ads.** { *; }`
or `-keep class com.google.android.gms.** { *; }`** — the play-services-ads
library ships its own consumer rules and a wildcard here will add several MB
back to dex.

### 4.7 Maps: blank tiles / autocomplete returns empty

1. Confirm `BuildConfig.MAPS_API_KEY` is set in the release flavor (check
   `app/build.gradle.kts` `buildConfigField` and / or `local.properties`).
2. Confirm Places SDK init log line:
   `Google Maps and Places APIs configured` in logcat.
3. If still broken, the API key is restricted to the wrong SHA-1 or package —
   not an R8 issue.

### 4.8 Coil image loading fails silently

Modern Coil 2.x ships full consumer rules. Don't add anything. If a specific
image loader extension class is missing, keep it surgically — never blanket
`-keep class coil.** { *; }`.

### 4.9 Room: `Cannot find implementation for <Dao>. <Dao>_Impl does not exist`

Already covered by the `@Dao` / `@Database` keeps. If it still breaks for a
new DAO, ensure the DAO interface is annotated with `@Dao` and the class is in
the `com.example.dutype.**` package tree.

### 4.10 SQLCipher: `UnsatisfiedLinkError: libsqlcipher.so`

Not an R8 issue — it's an ABI / packaging issue. Check
`android.packagingOptions.jniLibs` and confirm the AAB still ships
`libsqlcipher.so` per ABI (it does, verified above).

### 4.11 Crash with obfuscated stack trace in Crashlytics

You forgot to upload the new mapping. Run §5.

---

## 5. Mapping-file discipline (must do after every Play upload)

The `applymapping` reuse logic in `app/build.gradle.kts` reads
`app/mapping/release-mapping.txt`. After Play accepts the new release, copy
the freshly-generated mapping into that path so the **next** release diffs
against it (this is what keeps "Size for updates" small).

PowerShell (Windows):

```powershell
Copy-Item app\build\outputs\mapping\release\mapping.txt app\mapping\release-mapping.txt -Force
git add app/mapping/release-mapping.txt
git commit -m "chore(release): refresh mapping for v2.6.X reuse"
git push origin <branch>
```

The build will warn at configure time if `app/mapping/release-mapping.txt` is
missing or older than 14 days.

### 5.1 Why a tiny UI change can still show a ~7 MB update

If Play Console shows a ~7 MB update after a color/text-only change, first check
the release mapping, not images. In the April 30, 2026 release artifact,
compressed dex is ~7.16 MB, while app resources are only ~1.2 MB. So a mapping
mistake makes Play patch the dex split and the update looks much larger than the
source edit.

Quick checks:

```powershell
git status --short app\mapping\release-mapping.txt app\build.gradle.kts
Get-Item app\mapping\release-mapping.txt
```

If `app/mapping/release-mapping.txt` is modified after `bundleRelease`, commit
it with the same release/versionCode change before starting the next hotfix:

```powershell
git add app\build.gradle.kts app\mapping\release-mapping.txt
git commit -m "chore(release): refresh mapping for v2.6.X"
git push origin <branch>
```

Do not expect Play updates to be literally the number of source-code bytes
changed. The goal is to avoid rewriting the whole dex split. With a fresh
committed mapping, no-code repeat builds keep `classes.dex` stable; without it,
small UI changes can still look like multi-MB updates.

---

## 6. Rollback

If the signed release misbehaves and you can't diagnose in time:

```powershell
git revert c1197b6
git push origin latest-code-backup-20260319
```

Then build a hotfix release. The old rules will be restored. Update size will
balloon back to ~17 MB but the app will be functionally identical to v2.6.4.

---

## 7. Files changed in this work

- `app/proguard-rules.pro` — full rewrite.
- `app/build.gradle.kts` — `-applymapping` wiring + 14-day staleness warning +
  `archiveReleaseMapping` task.
- `app/src/main/java/com/example/dutype/DutyPeApplication.kt` — Maps key
  reflection bug fix.
- `docs/release-validation.md` — this file.
