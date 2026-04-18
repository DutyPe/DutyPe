# 07 — Performance & Scale Checklist

> Every item has a target metric. Add these as Crashlytics custom keys / release-tracking dashboards.

---

## 7.1 Startup

- [ ] Cold start p50 **<900 ms** on Pixel 4a (debug), **<600 ms** (release).
- [ ] Nothing on the critical path before first frame except: Firebase init, App Check init, Splash screen, nav decision.
- [ ] `logFirebaseBinding`, AdMob (if re-enabled), WorkManager `enqueueUniquePeriodicWork` calls deferred to `applicationScope.launch`. _(already done for logFirebaseBinding)_
- [ ] `StartDestinationResolver` returns in **<200 ms** p95; uses `SessionStore` cached values, not Firestore reads.
- [ ] Baseline Profile generated and validated in release build.

## 7.2 Compose

- [ ] Every `LazyColumn`/`LazyRow` has stable `key = { it.id }` on items.
- [ ] `contentType = { ... }` set where list mixes item types.
- [ ] `derivedStateOf` used for any computed Compose state that reads >2 states.
- [ ] No `mutableStateOf` for values that survive config change — use `rememberSaveable` or VM state.
- [ ] No screen file over **300 lines**. Extract sections.
- [ ] Animations use `animate*AsState`, not raw `LaunchedEffect` loops.
- [ ] Image loading: Coil with `size(Size.ORIGINAL)` only on detail screens; cards use `crossfade = true` + fixed `.size(...)` dp.

## 7.3 Memory

- [ ] Firestore offline cache capped at 100 MB (set in `AppModule`; verified).
- [ ] Room uses SQLCipher; VACUUM scheduled weekly via WorkManager.
- [ ] No `List<HeavyItem>` held in `@Singleton` longer than a screen session.
- [ ] Coil memory cache ≤25% device memory (set in `AppModule`; verified).

## 7.4 Lists

- [ ] `jobmetadata` card doc **<1 KB** p95 (after Phase 3 strip).
- [ ] Home feed initial page = **30 items**. No >50-item one-shot queries.
- [ ] Pagination via `orderBy(createdAt, DESC).startAfter(...).limit(30)`.
- [ ] No N+1 joins on feed — employer `companyName` denormalized on `jobmetadata`.

## 7.5 Caching

- [ ] Repository returns from **memory → Room → Firestore**, in that order.
- [ ] `Firestore.persistentCacheSettings` on (default with offline enabled).
- [ ] DataStore holds only session/UI prefs; never acts as cache for server truth.
- [ ] `RequestDeduplicator` (if present) covers concurrent reads of the same key.

## 7.6 Listeners

- [ ] Simultaneous snapshot listeners per session: **≤3**.
- [ ] Allowlist: `app_config/referral`, own `users/{uid}`, own `referral_stats/{uid}`, active detail doc (temporary).
- [ ] All listeners use `callbackFlow { awaitClose { registration.remove() } }` + `stateIn(scope, WhileSubscribed(5_000), default)`. Template: [AppConfigRepository.kt](../../app/src/main/java/com/example/dutype/repositories/AppConfigRepository.kt).
- [ ] No listener in `@Singleton` that is not itself scoped to auth state.

## 7.7 Workers

- [ ] Worker count: **≤5**.
- [ ] All workers `@HiltWorker` with injected Firestore.
- [ ] Periodic workers have `Constraints.NetworkType.CONNECTED` + `setRequiresBatteryNotLow(true)`.
- [ ] No worker does >3 Firestore roundtrips per run.
- [ ] `GuestEngagementWorker` anti-spam guard verified.

## 7.8 Notifications

- [ ] FCM payload is self-contained (title, body, type, deepLink, entityId, channelId).
- [ ] `onMessageReceived` does **no Firestore reads**.
- [ ] Notification channel creation off the main thread.
- [ ] Badge count derived from paginated query, cached per session.
- [ ] Notification docs have `expiresAt`; CF sweeps nightly (or TTL policy).

## 7.9 Firebase reads / writes

- [ ] **Session budgets:** ≤50 reads, ≤10 writes per typical session. Measured via Crashlytics custom key.
- [ ] Every `.where(...)` chain ends in `.limit(...)`.
- [ ] No client-maintained counters.
- [ ] No dual-writes (one action → one doc); fan-out via CF trigger only.
- [ ] `applications` write is 1 doc (the apply doc). CF handles notification + aggregates.

## 7.10 Indexes

- [ ] Composite index count **≤30**.
- [ ] Every index has a matching Android or CF query grep hit.
- [ ] Quarterly index audit (add to team calendar).
- [ ] Orphan indexes from [firestore.indexes.json](../../firestore.indexes.json) removed (#6, #10, #15, #16; #13 renamed).

## 7.11 ANR risk

- [ ] `MainThreadChecker` + `ANRHandler` wired (already done).
- [ ] No `runBlocking`, `GlobalScope.launch`, `Thread.sleep` in app code.
- [ ] No `Tasks.await(...)` on main thread.
- [ ] Heavy startup init in `applicationScope.launch(Dispatchers.IO)`.
- [ ] Compose first-frame work **<16 ms** on measured devices.

## 7.12 Baseline Profiles

- [ ] Generated on CI via `androidx.baselineprofile` plugin.
- [ ] Covers: cold start + nav to Worker Home + scroll feed + open Refer & Earn + apply flow.
- [ ] Committed to repo; validated per release.

## 7.13 Release monitoring

- [ ] **Crashlytics custom keys:** `screenName`, `activeRole`, `sessionReads`, `sessionWrites`, `featureFlags`.
- [ ] **Performance Monitoring traces:** cold_start, home_feed_load, job_apply, referral_apply, withdraw_request.
- [ ] **Alert:** p95 session reads >100 for 1 hour → Slack page.
- [ ] **Alert:** App Check token failures >1% → Slack page.
- [ ] **Alert:** FCM delivery latency p95 >10 s → Slack page.

## 7.14 Security posture

- [ ] `firestore.rules` has exactly **one** `service cloud.firestore` block (CI guard).
- [ ] Rules enforce CF-only writes for every aggregate and verification field.
- [ ] App Check (Play Integrity) enforced on Firestore + Storage + Functions.
- [ ] Rules emulator tests cover: cross-user access denial, role escalation denial, aggregate write denial, idempotent application write.
- [ ] Storage rules pinned to `isSelf()` on profile paths; file-size caps enforced.

## 7.15 Legacy debt

- [ ] `fallbackToDestructiveMigration()` removed from Room.
- [ ] No `FirebaseFirestore.getInstance()` static access in app code (CI guard: `! git grep -q 'FirebaseFirestore.getInstance()' app/src`).
- [ ] No `*StateManager` singletons in `state/`.
- [ ] No field-name aliases in models (`referrerId` vs `referrerUserId`).
- [ ] No `@Singleton` ViewModel holders for features that can be plain ViewModels.
