# Launch promo and app icon config

One Firestore document controls both features: `app_config/launch`.
The app never reads it on the startup path. A background WorkManager job
(every 12 h, network connected, battery not low) plus an opportunistic refresh at
most once per 12 h when the app is foregrounded fetches it (1 document read),
validates it, caches it locally and pre-downloads the promo image into Coil's disk cache.
The UI only reads the cached copy.

## Publish in the Firebase console

Firestore Database -> collection `app_config` -> document `launch` -> add a **string** field named
`config` whose value is the JSON below (or add native fields with the same names).
Security rules must allow clients to `read` `app_config/launch` (same as the other `app_config` docs).
Deleting the document (or setting `"enabled": false`) switches everything off and reverts the icon.

## Schema

```json
{
  "version": 12,
  "enabled": true,
  "promos": [
    {
      "id": "independence26",
      "type": "fullscreen",
      "startAt": 1794700800000,
      "endAt": 1795305600000,
      "roles": ["worker", "employer"],
      "languages": ["en", "hi", "te"],
      "minAppVersion": 821,
      "maxAppVersion": 0,
      "imageUrl": "https://example.com/promos/independence26.webp",
      "title": { "en": "Happy Independence Day", "hi": "स्वतंत्रता दिवस की शुभकामनाएं", "te": "స్వాతంత్ర్య దినోత్సవ శుభాకాంక్షలు" },
      "body":  { "en": "New jobs every day", "hi": "रोज़ नई नौकरियां", "te": "ప్రతిరోజూ కొత్త ఉద్యోగాలు" },
      "ctaLabel": { "en": "See jobs", "hi": "जॉब देखें", "te": "ఉద్యోగాలు చూడండి" },
      "deepLink": "dutype://home",
      "priority": 10,
      "frequency": { "maxShows": 3, "minHoursBetween": 24 },
      "dismissible": true
    },
    {
      "id": "refer_banner",
      "type": "banner",
      "roles": ["worker"],
      "imageUrl": "",
      "title": { "en": "Refer and earn" },
      "body": { "en": "Invite friends and earn rewards" },
      "ctaLabel": { "en": "Refer now" },
      "deepLink": "refer_earn",
      "priority": 1,
      "frequency": { "maxShows": 2, "minHoursBetween": 72 },
      "dismissible": true
    }
  ],
  "appIcon": {
    "enabled": true,
    "activeIconId": "independence",
    "startAt": 1794700800000,
    "endAt": 1795305600000,
    "minAppVersion": 821
  }
}
```

Field notes:

- Times are epoch milliseconds (UTC). `0` or missing = no start / no end.
- `version`: bump on every edit (used for logs and debugging; an invalid payload never replaces the last good one).
- `enabled` (top level) is the kill switch: false = no promo shown and icon reverts to default.
- `type`: `fullscreen` (needs `imageUrl`, https only) or `banner` (image optional, flat card at the top).
- `priority`: the highest number wins among eligible promos. Only one promo is shown per app launch.
- `roles` / `languages`: empty or missing = everyone. Roles: `worker`, `employer`. Languages: app language codes (`en`, `hi`, `te`).
- `minAppVersion` / `maxAppVersion`: compared with the app **versionCode** (current release 821). `0` = no limit.
- `deepLink`: a `dutype://...` or `https://dutype.in/...` link (handled by the existing deep-link path) or an in-app route name.
- Text may be a plain string (treated as English) or a per-language map; it falls back to `en`.
- `frequency`: `maxShows` total impressions and `minHoursBetween` impressions, tracked locally per promo id.
  A promo the user closes with the X button is never shown again (if `dismissible` is true).
  Use a new `id` to show a new campaign to everybody again.
- `appIcon.activeIconId` must be an alias known to the installed app. Currently: `default`, `birthday`, `independence`.
  Unknown ids are ignored (the current icon stays).

## When a promo is shown

Only from the cache, only on the worker or employer home screen (never onboarding, login, OTP, role selection or profile setup),
about 800 ms after the home screen appears, once per app launch, and only if its image is already in the disk cache.
If the image is not cached yet the promo is skipped for that launch and appears on a later one.

## When the icon changes

Only while the app is in the background (after the user leaves the app, or from the background job when the app is closed),
at most once per 24 h for a switch to a non-default icon. Reverting to the default icon is never throttled:
it happens when the window ends, the config is disabled or removed, or the app version is out of range.
Some launchers take a few seconds (or a launcher restart) to show the new icon.

## Adding a new icon variant

1. Add the icon resources (`mipmap-*`, adaptive icon XML) to the app.
2. Copy an `<activity-alias>` in `AndroidManifest.xml` (`enabled="false"`, own `icon`/`roundIcon`).
3. Add one line to `AppIconManager.ALIASES` (`"independence" to "com.example.dutype.IndependenceAlias"`).
4. Ship the release, then set `activeIconId` in the config.

## Testing

- Debug builds refresh on every foreground (no 12 h wait); release uses the 12 h rule.
- Filter logcat by tag `PromoConfig` to see fetch, selection and icon decisions.
- To see a promo again, clear the app data, or call `LaunchConfigStore.clearHistory(context)` from a debug hook.
- To test quickly, publish a promo with `startAt` in the past, `endAt` in the future and `frequency.maxShows` high, open the app on Wi-Fi (image is prefetched),
  close the app fully, open it again: the promo appears on the home screen.
- Icon test: set `appIcon.activeIconId` to `birthday`, open the app, press Home, wait a few seconds.
  Set `enabled` to false and repeat to check the revert.

## Safety rules

- Kill switch: `enabled: false` (or delete the doc). Takes effect at the next refresh (up to 12 h).
- Invalid JSON or a failed fetch never changes anything: the last good config keeps working.
- Expired promos and icon windows revert automatically, even offline, because the check uses the cached window.
- Use `minAppVersion` / `maxAppVersion` to target builds that have the needed icon aliases.
- Keep promo images small (<= 500 KB WebP, portrait about 1080x1920 for fullscreen). Images above 4 MB (1 MB on metered networks) are not prefetched.
