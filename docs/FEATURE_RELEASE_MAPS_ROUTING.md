# Feature Release: Maps Routing And API Key Setup

## Purpose

This note records what Google Maps Platform APIs DutyPe should use for current location features and the future worker-to-job route overlay feature.

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