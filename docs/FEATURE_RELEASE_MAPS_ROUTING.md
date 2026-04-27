# Feature Release: Maps Routing And API Key Setup

## Purpose

This note records what Google Maps Platform APIs DutyPe should use for current location features and the future worker-to-job route overlay feature.

## Play Update Size: Why It Is Still ~18.8 MB

Observed in Play Console:

- New install size: about 24.6 MB
- Update size: about 18.8 MB
- Difference vs previous release: only about -46.6 KB

Meaning:

- The package is slightly smaller, but Play patch size is still large because most of the base split changed between versions.
- In this app, a small UI change still touches the base module build graph, so many bytes in DEX/resources are different.

Already fixed:

- Removed JNI keepDebugSymbols packaging from release in app module.

Why that did not fully solve it:

- The app still has heavy SDKs in the base module (Ads, Maps/Places, multiple Firebase modules, SQLCipher).
- R8/resource shrink output churn causes larger binary diff for small code changes.

One-by-one action order (practical):

1. Keep current fix (done): no keepDebugSymbols in packaged release JNI libs.
2. Run a no-code-change experiment:
    - Build a release from the same commit with only versionCode/versionName change.
    - Upload to Internal testing and check update size.
    - If update is still large, this confirms binary churn (not feature size) is the dominant issue.
3. Reuse R8 mapping from previous release during obfuscation:
    - This reduces symbol/name churn between versions.
    - Expected: smaller patch size for minor changes.
    - **Now wired in `app/build.gradle.kts`.** Workflow per release:
        1. Build the release once (`./gradlew :app:bundleRelease`).
        2. The `archiveReleaseMapping` task auto-copies `app/build/outputs/mapping/release/mapping.txt` into `app/mapping/release-mapping.txt`.
        3. Bump `versionCode` + `versionName` and **commit `app/mapping/release-mapping.txt` together with the version bump.**
        4. Next release builds will read that mapping via a generated `-applymapping` rule, so R8 keeps the same obfuscated names. Patch size for small UI changes (e.g. `WorkerHomeScreen` background colour) should drop dramatically.
    - First-ever release after this change: no previous mapping exists, so the build silently skips applyMapping. Starting from the *next* release, patch sizes shrink.
4. Remove or isolate unused heavy dependencies from base:
    - SafetyNet appears to have no direct source usage; validate in QA, then remove if safe.
    - Keep Ads, Maps/Places, Firebase modules only where actually needed.
5. Move heavy optional features out of base module:
    - Candidate first: Ads feature.
    - Next: map-heavy screens if not core for all users.
6. Re-measure every release:
    - Track both New install size and Size for updates in Play Console.
    - Compare only against immediately previous production release.

## Current Location APIs

Keep these enabled for the Android app:

- Maps SDK for Android: shows interactive maps, markers, and route polylines in the Android app.
- Places API / Places API New: powers location search, address autocomplete, and place selection.
- Geocoding API: converts typed addresses to latitude/longitude and converts GPS latitude/longitude back to readable addresses.

Do not enable these unless a feature specifically needs them:

- Maps JavaScript API: only for web maps.
- Maps Static API: only for static map images.
- Street View API: only for Street View experiences.
- Aerial View API: only for special aerial video views.
- Roads API: only for snapping GPS traces to roads.
- Navigation SDK: only for full turn-by-turn navigation inside the app.

## Future Route Overlay Feature

For an Uber, Rapido, or Ola-style route from a worker's current location to an employer job location, enable:

- Routes API

Use Routes API when the app needs:

- Real road route from worker current location to job location.
- Actual travel distance, not straight-line distance.
- Estimated duration or ETA.
- Encoded route polyline to draw on Google Maps.
- Route overlay animation or pulsing route effect.

Distance Matrix API is not needed for one selected job route. Enable Distance Matrix API only if the app needs distance or ETA for many jobs at once.

## Route Overlay Implementation Plan

1. Get the worker current latitude/longitude using Android location services.
2. Read the employer job latitude/longitude from Firestore.
3. Call Routes API `computeRoutes`.
4. Read `distanceMeters`, `duration`, and the encoded polyline.
5. Decode the polyline in Android.
6. Draw the route using Google Maps Compose `Polyline`.
7. Animate the route width, color, or alpha to create a pulsing route effect.

For a simple label like `2.3 km away`, no extra API is needed. The app can calculate straight-line distance locally. For real road distance and ETA, use Routes API.

## API Key Restriction Steps

In Google Cloud Console:

1. Go to APIs & Services.
2. Click Credentials.
3. Click the Maps API key used by the Android app.
4. Under Application restrictions, select Android apps.
5. Click Add an item.
6. Package name: `com.dutype.app`.
7. Add the SHA-1 certificate fingerprint.
8. For debug SHA-1, run `./gradlew.bat :app:signingReport` and copy the SHA1 under the debug variant.
9. For release SHA-1, use Google Play Console > App integrity > App signing key certificate.
10. Under API restrictions, select Restrict key.
11. Allow only the APIs the app uses:
    - Maps SDK for Android
    - Places API / Places API New
    - Geocoding API
    - Routes API, only when the route overlay feature is enabled
12. Click Save.

Important: use package name `com.dutype.app`, not `com.example.dutype`, because Gradle `applicationId` is `com.dutype.app`.