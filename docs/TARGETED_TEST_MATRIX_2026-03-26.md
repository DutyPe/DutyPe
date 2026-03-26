# Targeted Test Matrix - Notifications, Deep Links, Role Switch

Date: 2026-03-26
Scope: Android notification/deep-link handling and dual-role switch paths.
Method: Code-path verification + compile/build health already green in prior run.

## Result Legend
- PASS: Route/action is wired and reachable in current code.
- FAIL: Confirmed mismatch or dead-end.
- NEEDS_DEVICE: Requires runtime tap/device validation (cannot be proven from static analysis alone).

## Notification and Deep Link Matrix

| ID | Scenario | Expected | Status | Evidence |
|---|---|---|---|---|
| N1 | FCM payload deep link opens app with `ACTION_VIEW` | `MainActivity` launched with deep link data | PASS | `DutyPeMessagingService.createDeepLinkIntent` sets `Intent.ACTION_VIEW` and class to `MainActivity` |
| N2 | Worker application action deep link | `dutype://worker/applications/{id}` resolves | PASS | Deep link created in `DutyPeMessagingService.addActionsForType`; handled in `DeepLinkHandler` worker/applications branch |
| N3 | Employer application action deep link | `dutype://employer/applications/{id}` resolves | PASS | Deep link created in `DutyPeMessagingService.addActionsForType`; handled in `DeepLinkHandler` employer/applications branch |
| N4 | Generic applications action from notification | `dutype://applications` resolves to worker applications list | PASS | Created in `DutyPeMessagingService`; handled by `HOST_APPLICATIONS` in `DeepLinkHandler` |
| N5 | Generic jobs action from notification | `dutype://jobs` resolves to worker jobs list | PASS | Created in `DutyPeMessagingService`; handled by `HOST_JOBS` in `DeepLinkHandler` |
| N6 | Employer dashboard action from welcome/profile notifications | `dutype://employer/dashboard` resolves | PASS | Created in `DutyPeMessagingService`; routed to employer home in `DeepLinkHandler` |
| N7 | Employer profile public deep link by ID | `dutype://employer/{employerId}` should show public employer profile view | FAIL | `EmployerNavGraph` route exists but TODO says dedicated worker-facing screen not implemented; currently opens `EmployerProfileScreen` |
| N8 | Message route helper usage | `Routes.messageWorkerRoute(workerId)` should resolve to registered composable | FAIL | Route helper exists in `Routes.kt`, no matching `composable("message/..." )` found in navigation graphs |
| N9 | Web app links for worker/job/application/employer | `https://dutype.in/...` should map to routes | PASS | Handled in `DeepLinkHandler` web-domain branches; deep links exist in `EmployerNavGraph` for worker/employer view routes |
| N10 | Notification tap end-to-end from tray on device | Tap notification should land on intended destination | NEEDS_DEVICE | Static path is wired; requires emulator/physical tap verification and auth-state permutations |

## Role Switch Matrix

| ID | Scenario | Expected | Status | Evidence |
|---|---|---|---|---|
| R1 | Switch Worker -> Employer from profile dialog | Firestore activeRole updated, caches/viewmodels cleaned, navigate to employer home | PASS | `RoleSwitchManager.switchRole` updates role, clears state, calls `navigateToRoleHome` |
| R2 | Switch Employer -> Worker from profile dialog | Same as above, navigate to worker home | PASS | Same manager path; route mapping uses `Routes.WORKER_HOME` |
| R3 | Back stack isolation after switch | Old role screens not reachable by back | PASS | `navigateToRoleHome` uses `popUpTo(0) { inclusive = true }` |
| R4 | Role switch entry points present in both profile screens | Dialog + manager invocation available for dual-role users | PASS | Present in `WorkerProfile.kt` and `EmployerProfileScreen.kt` with `RoleSwitchManagerEntryPoint` |
| R5 | Notification list role filtering after switch | Notifications filtered by active role | PASS | `NotificationService.getUserNotifications` filters by `activeRole`; worker screen reloads on `activeRole` change |
| R6 | Runtime UX under latency/failures during switch | Loader, success/failure toast correctness | NEEDS_DEVICE | Callback hooks exist, but user-visible timing/error UX needs device run |

## Immediate Follow-up Fixes Recommended
1. Implement dedicated worker-facing employer public profile screen for `Routes.EMPLOYER_PROFILE_VIEW`.
2. Remove or implement destination for `Routes.messageWorkerRoute(workerId)` to avoid dead route helper.
3. Run physical/emulator tap test for `N10` and `R6` with authenticated worker, authenticated employer, and logged-out states.
