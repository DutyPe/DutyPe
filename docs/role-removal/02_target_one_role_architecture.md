# Target One-Role Architecture

## Account model

**One Firebase auth identity = one product role for the lifetime of the account.**

- Role is chosen once at registration via `SelectRoleScreen`.
- After registration the role is **immutable in product code**. Admin tools may
  change it manually as a support action; runtime app code never does.
- No "currently active role" concept exists. There is just `role`.

## Kotlin model

```kotlin
data class User(
    val id: String,
    val phone: String,
    val fullName: String,
    val profileImageUrl: String?,
    val role: UserRole,         // single, immutable in product code
    val lat: Double, val lng: Double, val geohash: String,
    val fcmToken: String?,
    val createdAt: Long, val lastActiveAt: Long
)
```

`isDualRole()`, `hasRole()`, `getEnabledRoles()`, `roles: List<String>`,
`activeRole` are removed from Kotlin code.

## Firestore document shape (`users/{uid}`)

| Field | Status | Notes |
|---|---|---|
| `userId` | keep | doc id |
| `phone` | keep | E.164 |
| `fullName` | keep | display name |
| `profileImageUrl` | keep | optional |
| **`role`** | **canonical** | `"WORKER" | "EMPLOYER"` (single string) |
| `roles` | **transient compat** | one-element array `[role]`, written so legacy CFs/admin still parse |
| `activeRole` | **transient compat** | mirrors `role`, ditto |
| `location.{lat,lng}`, `geohash` | keep | as-is |
| `fcmToken` | keep | as-is |
| `language` | keep | for notification i18n |
| `createdAt`, `lastActiveAt` | keep | |
| `referralCode`, `referredByCode`, `referredByUserId` | keep | |

> The compat fields `roles` and `activeRole` will be deleted after the next CF
> deploy + admin web update reads `role` directly. Tracked in
> `03_firestore_changes.md`.

## Profile collections

- `worker_profiles/{uid}` — only exists if `users/{uid}.role == WORKER`.
- `employer_profiles/{uid}` — only exists if `users/{uid}.role == EMPLOYER`.

A user account never owns both. No reconciliation logic.

## Startup / navigation model

`MainNavGraph` resolution:

```
hasOnboardingBeenCompleted == false  →  ONBOARDING
auth user == null                     →  SELECT_ROLE
users/{uid}.role == WORKER            →  PROFILE_SETUP   if !workerComplete else WORKER_HOME
users/{uid}.role == EMPLOYER          →  EMPLOYER_PROFILE_SETUP if !employerComplete else EMPLOYER_HOME
```

No `activeRole` lookup. No DataStore secondary role state. No "role switch home"
branch. `StartDestinationCache` is only reset at logout.

## Profile completion model

- `ProfileCompletionViewModel.isProfileComplete(role)` is collapsed to
  `isProfileComplete()` — operates on the user's single role internally.
- `markProfileComplete()` / `markProfileSetupAsShown()` lose the `role`
  parameter; they record completion against the only role this account has.

## FCM / topics

- On login (or first FCM token registration), `FCMTokenManager.registerTokenWithRole(role)`
  subscribes to **only** the topics of that role:
  - `all_users`, `all_users_<lang>`, `app_updates`, `app_updates_<lang>`
  - **either** `workers` + `workers_<lang>` **or** `employers` + `employers_<lang>`
  - Unsubscribes from `guest_users` topics.
- No `switchActiveRole`, no resubscribe-on-switch flow.
- On logout, all topics are cleared and `guest_users[+lang]` is resubscribed.

## Notification routing

CFs continue to read `users/{uid}.role` (preferring the new field, falling
back to `activeRole` for unmigrated docs). Per-recipient locale lookup is
already in place from the Phase 4 i18n work.

## Removed concepts (never to reappear)

- runtime role switching
- `activeRole` reads in app code
- `roles[]` array reads in app code
- per-role profile completion tracking on a single account
- per-role cache split (`RoleCacheManager`)
- `RoleSwitchManager`, `RoleManagementViewModel`, `RoleSwitchDialog`,
  `DualRoleManager`
