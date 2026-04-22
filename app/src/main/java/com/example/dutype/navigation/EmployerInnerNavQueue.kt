package com.example.dutype.navigation

/**
 * One-shot, same-process handoff queue from the outer [MainNavGraph] to the
 * nested [EmployerMainScreen] NavHost.
 *
 * Why this exists:
 *  - `EmployerMainScreen` owns its own `NavController` (see
 *    [EmployerMainScreen]). Routes like `employer_post_job` only exist in
 *    that inner graph.
 *  - When the user finishes [com.example.dutype.employer.screens.MandatoryEmployerProfileSetupScreen]
 *    with `returnRoute = employer_post_job`, the outer controller cannot
 *    navigate there — that route isn't in the outer graph. Crash:
 *    `Navigation destination ... cannot be found in the navigation graph`.
 *
 * How this works:
 *  - The setup screen sets [pendingRoute] then navigates the outer controller
 *    to [Routes.EMPLOYER_HOME], which mounts `EmployerMainScreen`.
 *  - `EmployerMainScreen` checks [pendingRoute] on first composition and, if
 *    set, pushes it onto its inner controller and clears the queue.
 *
 * Single in-memory slot is intentional: this is a UI handoff, not a message
 * bus, so there's never more than one pending value.
 */
object EmployerInnerNavQueue {
    @Volatile
    private var pendingRoute: String? = null

    fun setPending(route: String) {
        pendingRoute = route
    }

    fun consume(): String? {
        val route = pendingRoute
        pendingRoute = null
        return route
    }
}
