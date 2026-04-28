# Release Validation — DutyPe v2.6.5+

This is the handoff document for the R8 / ProGuard rewrite that landed in commit
`c1197b6` (and the follow-up `BuildConfig` reflection fix). Everything you need
to verify the next signed release is safe — and the recipe to fix any specific
crash that shows up — lives in this file.

> Audience: future you, or another AI on a different machine. No prior context
> assumed.

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

If any step hangs, crashes, or silently no-ops, jump to §4 to find the surgical
fix.

---

## 4. Per-crash recipes (paste-and-go)

> All edits below go into `app/proguard-rules.pro`. After editing, rerun
> `./gradlew :app:bundleRelease` and re-test.

### 4.1 `ClassNotFoundException: com.foo.bar.Baz` at app start

R8 stripped a class loaded by reflection, JNI, the manifest, or a service
loader. Add the most surgical keep that covers it:

```pro
-keep class com.foo.bar.Baz { *; }
```

If it's a whole package needed by a third-party SDK that ships no consumer
rules:

```pro
-keep class com.foo.bar.** { *; }
```

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
fails. Safety net:

```pro
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @androidx.hilt.work.HiltWorker class *
```

(Workers in this app: `PendingApplicationNotificationWorker`,
`GuestEngagementWorker`, `JobSyncWorker`, `JobPostingWorker`.)

### 4.5 FCM push not delivered

Confirm `DutyPeMessagingService` is still in the manifest (it is, line 115).
If R8 ever renames it (would only happen if you change manifest to use a class
literal), add:

```pro
-keep class com.example.dutype.services.DutyPeMessagingService { *; }
```

### 4.6 `RuntimeException: Unable to get provider ...MobileAdsInitProvider`

Already kept. If you ever swap ads SDK versions and break it again:

```pro
-keep class com.google.android.gms.ads.MobileAdsInitProvider { *; }
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient { *; }
-keep class com.google.android.gms.common.internal.safeparcel.** { *; }
```

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
