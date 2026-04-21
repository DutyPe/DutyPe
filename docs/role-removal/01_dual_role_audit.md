# Dual-Role Audit (snapshot pre-refactor)

Found via exhaustive `grep_search` over `app/`, `functions/src/`, `web/`, `firestore.rules`.

## Core managers / orchestrators

| File | Purpose |
|---|---|
| `app/.../managers/RoleSwitchManager.kt` | Orchestrator: writes activeRole, refreshes token, clears caches, navigates |
| `app/.../managers/RoleSwitchManagerEntryPoint.kt` | Hilt entry point exposing the manager |
| `app/.../viewmodels/RoleManagementViewModel.kt` | `toggleRole`, `switchActiveRole`, `switchRoleWithCleanup` |
| `app/.../cache/RoleCacheManager.kt` | Per-role cache split, cleared on switch |
| `app/.../components/DualRoleManager.kt` | Legacy helper, no live consumers |
| `app/.../components/RoleSwitchDialog.kt` | Compose dialog for switch confirmation |
| `app/.../state/ProfileSetupStateManager.kt` | Tracks per-role profile setup state in DataStore |
| `app/.../state/AppStateManager.kt` | App-wide session state, also clears on switch |

## Models

| File | Dual-role surface |
|---|---|
| `app/.../models/User.kt` | `roles: List<String>`, `activeRole: UserRole`, `hasRole`, `isDualRole`, `getEnabledRoles` |
| `app/.../models/UserRole.kt` | Enum WORKER/EMPLOYER/ADMIN |

## Persistence / validation

| File | Surface |
|---|---|
| `firestore.rules` | `validRoles()`, enforces `activeRole ∈ roles`, size ≤ 2 |
| `app/.../services/ProfileCompletionService.kt` | `saveUserInfo`, `updateUserRole` mutate `roles[]` and `activeRole`; `isProfileComplete(userId, role)` |
| `app/.../services/AuthFlowService.kt` | Writes `roles`, `activeRole` on signup |
| `app/.../utils/FirestoreUtils.kt` | `ensureMinimalUserDocument` writes `roles[]`, `activeRole` |

## Navigation / routing

| File | Surface |
|---|---|
| `app/.../navigation/Routes.kt` | `SELECT_ROLE`, `WORKER_HOME`, `EMPLOYER_HOME` |
| `app/.../navigation/MainNavGraph.kt` | Reads activeRole from Firestore + DataStore + roles[] fallback to choose start destination |
| `app/.../navigation/StartDestinationCache.kt` | Caches resolved start route |
| `app/.../common/screens/SelectRoleScreen.kt` | Initial role chooser (kept; one-time) |

## UI components

| File | Surface |
|---|---|
| `app/.../worker/screens/profile/WorkerProfile.kt` | "Switch Role" button when `isDualRole()` |
| `app/.../employer/screens/profilescreen/EmployerProfileScreen.kt` | Same |
| `app/.../components/RoleSwitchDialog.kt` | Switch confirmation dialog |

## Profile completion

| File | Surface |
|---|---|
| `app/.../viewmodels/ProfileCompletionViewModel.kt` | `isProfileComplete(role)`, `markProfileComplete(role)`, `markProfileSetupAsShown(role)`, `saveWorkerProfileData()`, `saveEmployerProfileData()` |
| `app/.../services/ProfileCompletionService.kt` | Per-role completion %; mutates roles[]+activeRole |
| `app/.../components/LoginBottomSheet.kt` | Reads existing user activeRole; calls `markProfileComplete(role)` |
| `app/.../auth/EnhancedLoginScreen.kt` | Same patterns |

## FCM / topics

| File | Surface |
|---|---|
| `app/.../services/FCMTokenManager.kt` | `subscribeToRoleTopics(role)` subs+unsubs per role on switch |
| `app/.../utils/LocaleHelper.kt` | Re-subs on language change |
| `app/.../auth/AuthManager.kt` | Logout clears token + re-subs guest topic |

## Cloud Functions

| File | Surface |
|---|---|
| `functions/src/referral-system.ts` | Reads `userData.activeRole` |
| `functions/src/scheduled-notifications.ts` | `userActiveRoleMatches(user, 'WORKER'/'EMPLOYER')` filter |
| `functions/src/index.ts` | Validates `targetRole` for self-notifications |

## Web admin

| File | Surface |
|---|---|
| `web/lib/firebase/admin-normalizers.ts` | `normalizeRole`, `normalizeRoles`; coalesces `activeRole`+`roles[]` |
| `web/lib/firebase/admin-access.ts` | Admin claim from `activeRole`/`roles[]`/`role` |
| `web/components/product/use-product-session.ts` | `extractProductRoles`, `getActiveProductRole`, `setActiveRole` |
| `web/components/product/product-shell.tsx` | Role-gating via `currentRole === requiredRole` |

## String resources

| Key | Action |
|---|---|
| `select_role` | Keep (initial selection) |
| `switch_role_title`, `switch_to_role_title`, `switch_role_body`, `tap_to_switch_role` | DELETE |
