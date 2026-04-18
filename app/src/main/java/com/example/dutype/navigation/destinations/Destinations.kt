package com.example.dutype.navigation.destinations

import kotlinx.serialization.Serializable

/**
 * P2-3 — Type-safe navigation destinations.
 *
 * Defines a single sealed-interface hierarchy that mirrors every route in
 * [com.example.dutype.navigation.Routes]. Each destination is `@Serializable`
 * so it can be used directly as a Compose Navigation 2.8 type-safe route OR as
 * a [androidx.navigation3.runtime.NavKey] when this module migrates to
 * Jetpack Navigation 3 (stable as of November 2025).
 *
 * ## Migration plan
 *
 * **Today (this scaffold):** Destinations are pure Kotlin `@Serializable`
 * objects/classes. Existing call sites still navigate via the `Routes`
 * `const val` strings; this hierarchy is the *target*, not yet wired in.
 *
 * **Next (Compose Navigation 2.8 type-safe path, low risk):** Replace
 * `composable("route")` with `composable<Destination.X>` and replace
 * `navController.navigate("route/$id")` with
 * `navController.navigate(Destination.JobDetail(id))`. Routes.kt can be
 * deleted once every call site is migrated.
 *
 * **Eventually (Navigation 3 path, larger scope):** Have every destination
 * implement `androidx.navigation3.runtime.NavKey`; replace `NavHost` with
 * `NavDisplay(backStack = ..., entryProvider = entryProvider { entry<X> { } })`;
 * replace `NavController` with a `Navigator` wrapping a
 * `mutableStateListOf<NavKey>()` back stack. The shape of `Destination`
 * defined here is deliberately compatible with that move — only the marker
 * interface changes.
 *
 * Migration playbook lives in `docs/android-scale-audit/02_brutal_issue_list.md`
 * under P2-3.
 */
sealed interface Destination {

    // region Auth & onboarding
    @Serializable data object Onboarding : Destination
    @Serializable data class EnhancedLogin(val role: String? = null) : Destination
    @Serializable data object Register : Destination
    @Serializable data object SelectRole : Destination
    @Serializable data object ManualLocation : Destination
    // endregion

    // region Top-level shells
    @Serializable data object WorkerHome : Destination
    @Serializable data object EmployerHome : Destination
    @Serializable data object EmployerCompanyDetails : Destination
    // endregion

    // region Worker bottom-tab destinations
    @Serializable data object WorkerHomeTab : Destination
    @Serializable data object WorkerMyJobs : Destination
    @Serializable data object WorkerProfile : Destination
    // endregion

    // region Worker feature destinations
    @Serializable data object WorkerProfileDetails : Destination
    @Serializable data object WorkerVisitingCard : Destination
    @Serializable data object EmployerVisitingCard : Destination
    @Serializable data object WorkerAllJobs : Destination
    @Serializable data object WorkerCategories : Destination
    @Serializable data class WorkerCategoriesFiltered(val category: String) : Destination
    @Serializable data class JobDetail(val jobId: String) : Destination
    @Serializable data class JobApplication(val jobId: String) : Destination
    @Serializable data object ProfileSetup : Destination
    @Serializable data class ProfileSetupWithReturn(val returnRoute: String) : Destination
    @Serializable data object WorkerNotifications : Destination
    @Serializable data object WorkerHistory : Destination
    @Serializable data object WorkerJobMap : Destination
    @Serializable data object WorkerEarnings : Destination
    @Serializable data object WorkerReferEarn : Destination
    // endregion

    // region Shared support destinations
    @Serializable data object Help : Destination
    @Serializable data object Report : Destination
    @Serializable data object Tutorial : Destination
    @Serializable data object Faq : Destination
    @Serializable data object AboutUs : Destination
    @Serializable data object ContactUs : Destination
    // endregion

    // region Employer destinations
    @Serializable data object EmployerDashboard : Destination
    @Serializable data object EmployerProfile : Destination
    @Serializable data object EmployerPostJob : Destination
    @Serializable data object EmployerProfileSetup : Destination
    @Serializable data class EmployerProfileSetupWithReturn(val returnRoute: String) : Destination
    @Serializable data object EmployerMyJobs : Destination
    @Serializable data class EditJob(val jobId: String) : Destination
    @Serializable data class ViewApplicants(val jobId: String) : Destination
    @Serializable data object EmployerAbout : Destination
    @Serializable data object EmployerHelp : Destination
    @Serializable data object EmployerNotifications : Destination
    @Serializable data object EmployerManageAddresses : Destination
    @Serializable data object EmployerReferEarn : Destination
    @Serializable data object EmployerHistory : Destination
    @Serializable data object EmployerVoicePostJob : Destination
    @Serializable data object Analytics : Destination
    @Serializable data class WorkerProfileView(val workerId: String) : Destination
    @Serializable data class EmployerProfileView(val employerId: String) : Destination
    @Serializable data object EmployerApplications : Destination
    @Serializable data class EmployerApplicationsJob(val jobId: String) : Destination
    @Serializable data object EmployerMoreSettings : Destination
    @Serializable data object EmployerMyRatings : Destination
    @Serializable data object EmployerTrustBadges : Destination
    // endregion
}
