# Dual-Role Removal — Master Status

**Branch:** `latest-code-backup-20260319`
**Started:** 2026-04-21
**Driver:** Single-account = single-role. No runtime switching.

## Phase Tracker

| Phase | Status | Notes |
|---|---|---|
| 1. Discovery audit | ✅ done | See `01_dual_role_audit.md` |
| 2. Target architecture design | ✅ done | See `02_target_one_role_architecture.md` |
| 3. Code refactor (delete switching, simplify nav, FCM, profile) | ✅ done | See `05_execution_log.md` |
| 4. Firestore contract cleanup | ✅ done (write-side) | See `03_firestore_changes.md` |
| 5. Dead-code purge | ✅ done | See `05_execution_log.md` |
| 6. Build + validation | ✅ done | `./gradlew :app:assembleDebug` BUILD SUCCESSFUL |

## Key Outcomes

- **Deleted (8 files):** `RoleSwitchManager.kt`, `RoleSwitchManagerEntryPoint.kt`,
  `RoleManagementViewModel.kt`, `RoleSwitchDialog.kt`, `DualRoleManager.kt`,
  legacy `state/AppStateManager.kt` & `state/ProfileSetupStateManager.kt` are
  retained only as logout/session helpers; switching APIs gone.
- **`User` model collapsed** to single `role: UserRole`. `roles[]` and
  `activeRole` are no longer exposed in Kotlin code; Firestore documents
  continue to write `role` (canonical) plus a one-element `roles` array and
  `activeRole = role` (transient compat for CFs/admin during migration).
- **Navigation simplified:** `MainNavGraph` resolves user → single
  `users.role` → single home/profile-setup destination.
- **FCM:** users subscribe to exactly one role topic
  (`workers`/`workers_te` OR `employers`/`employers_te`); switching no longer
  triggers re-subscription.
- **Profile completion:** `ProfileCompletionViewModel` now keys completion
  state by the user's single role.
- **UI:** "Switch Role" buttons removed from `WorkerProfile.kt` and
  `EmployerProfileScreen.kt`. Role-switch strings deleted.

See `06_validation_report.md` for the final audit.
