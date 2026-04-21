# Firestore Contract Changes

## `users/{uid}` — write-side change

**Before:**

```ts
{
  userId, phone, fullName, profileImageUrl,
  roles: ["WORKER", "EMPLOYER"],   // could grow if user added a second role
  activeRole: "WORKER" | "EMPLOYER",
  ...
}
```

**After (write-side):**

```ts
{
  userId, phone, fullName, profileImageUrl,
  role: "WORKER" | "EMPLOYER",
  // transient compat (one deploy cycle):
  roles: [role],
  activeRole: role,
  ...
}
```

The Android app no longer sends multi-element `roles` arrays and never sets
`activeRole` to a value different from `role`. After the next CF + admin web
release stops reading `activeRole`/`roles[]`, both fields can be dropped via a
one-shot script.

## `users/{uid}` — read-side change in app

```kotlin
val roleStr = (data["role"] as? String)
    ?: (data["activeRole"] as? String)              // legacy compat
    ?: (data["roles"] as? List<*>)?.firstOrNull()?.toString()  // legacy compat
```

The compat fallbacks live exclusively in `User` parsers and `MainNavGraph`'s
startup resolver. Nothing else consults `roles[]`.

## `firestore.rules`

- `validRoles()` helper retained transiently because some legacy clients in
  the wild still write `roles`/`activeRole`; rules continue to allow those
  fields but no longer require both. Rule constraints `roles.size() <= 2`
  and `activeRole in roles` remain valid because the new write shape
  (`roles: [role], activeRole: role`) trivially satisfies them.
- No rule edit is required for the Android refactor. (A future CF deploy may
  drop the helper.)

## Cloud Functions

`functions/src/scheduled-notifications.ts` and `functions/src/referral-system.ts`
already query the user document by `activeRole`. Because the Android app now
guarantees `activeRole === role`, no CF behaviour changes. When CFs are
updated to prefer `data.role`, the compat fields can be removed.

## Profile collections

`worker_profiles/{uid}` and `employer_profiles/{uid}` are unchanged in shape.
The product invariant is now: **at most one of them exists per `uid`**. No
schema change is required because the existing collections already key by
uid; the app simply stops writing to the off-role profile.

## DataStore (local) keys removed from product paths

| Key | Before | After |
|---|---|---|
| `user_role` | written by RoleManagementViewModel.switchActiveRole | only written once at registration |
| Per-role completion flags | both worker + employer flags possible per uid | only the user's single role's flag is ever set |
| `start_destination` cache | invalidated on every switch | invalidated only on logout |

No DataStore key was renamed or deleted; the schemas simply stop being used
for the second role.
