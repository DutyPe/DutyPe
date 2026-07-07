package com.example.dutype.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.example.dutype.navigation.Routes
import timber.log.Timber

/**
 * Deep Link Handler - Comprehensive deep linking following Google's best practices
 * 
 * Supported deep links:
 * - dutype://job/{jobId} - Opens job detail screen
 * - dutype://worker/{workerId} - Opens worker profile
 * - dutype://refer/{referralCode} - Opens referral screen with code
 * - dutype://application/{applicationId} - Opens application detail
 * - https://dutype.in/jobs/{jobId} - Web link to job
 * - https://dutype.in/refer/{code} - Web link to referral
 * - https://dutype.in/worker/{workerId} - Web link to worker profile
 * 
 * Usage:
 * ```
 * DeepLinkHandler.handleDeepLink(intent, navController)
 * ```
 */
object DeepLinkHandler {
    
    private const val SCHEME = "dutype"
    private const val HOST_JOB = "job"
    private const val HOST_WORKER = "worker"
    private const val HOST_REFER = "refer"
    private const val HOST_APPLICATION = "application"
    private const val HOST_EMPLOYER = "employer"
    private const val HOST_PROFILE = "profile"
    private const val HOST_NOTIFICATIONS = "notifications"
    private const val HOST_HOME = "home"
    private const val HOST_JOBS = "jobs"
    private const val HOST_APPLICATIONS = "applications"
    private const val HOST_POST_JOB = "post-job"
    private const val HOST_REFERRALS = "referrals"
    private const val HOST_HELP = "help"
    private const val HOST_SUPPORT = "support"
    private const val HOST_CONTACT = "contact"
    
    // Web URLs - Android App Links (opens Android app directly)
    private const val WEB_DOMAIN = "dutype.in"  // Custom domain
    private const val WEB_JOBS_PATH = "jobs"
    private const val WEB_REFER_PATH = "refer"
    
    /** Safe navigate helper — wraps all navigation in try-catch + Timber logging. */
    private fun safeNavigate(navController: NavController, route: String, label: String) {
        try {
            navController.navigate(route) { launchSingleTop = true }
            Timber.i("🔗 DEEP LINK: ✅ $label navigation successful")
        } catch (e: IllegalArgumentException) {
            // Some links target nested worker routes that may not exist on the root NavController.
            val fallbackRoute = when (route) {
                com.example.dutype.navigation.WorkerBottomRoutes.PROFILE,
                Routes.WORKER_NOTIFICATIONS,
                Routes.WORKER_ALL_JOBS,
                com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS -> Routes.WORKER_HOME
                else -> null
            }

            if (fallbackRoute != null) {
                runCatching {
                    navController.navigate(fallbackRoute) { launchSingleTop = true }
                }.onSuccess {
                    Timber.w(e, "🔗 DEEP LINK: ⚠️ $label not found on current graph, fallback to $fallbackRoute")
                }.onFailure { fallbackError ->
                    Timber.e(fallbackError, "🔗 DEEP LINK: ❌ Failed fallback navigation for $label")
                }
            } else {
                Timber.e(e, "🔗 DEEP LINK: ❌ Failed to navigate ($label)")
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 DEEP LINK: ❌ Failed to navigate ($label)")
        }
    }
    
    /**
     * Handle deep link from string URL
     * Used for in-app navigation from announcements, notifications, etc.
     */
    fun handleDeepLink(deepLinkUrl: String, navController: NavController): Boolean {
        return try {
            val normalizedUrl = deepLinkUrl.trim()
            if (normalizedUrl.isBlank()) {
                return false
            }

            if (handleInternalRouteString(normalizedUrl, navController)) {
                return true
            }

            val uri = Uri.parse(normalizedUrl)
            handleDeepLinkUri(uri, navController)
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to parse deep link: $deepLinkUrl")
            false
        }
    }

    /**
     * Announcement actions can be app routes, app links, or external update links.
     */
    fun handleAnnouncementAction(
        deepLinkUrl: String,
        navController: NavController,
        context: Context
    ): Boolean {
        if (handleDeepLink(deepLinkUrl, navController)) {
            return true
        }

        return openExternalAction(deepLinkUrl, context)
    }
    
    /**
     * Handle deep link from intent
     */
    fun handleDeepLink(intent: Intent?, navController: NavController): Boolean {
        val data = intent?.data ?: return false
        return handleDeepLinkUri(data, navController)
    }
    
    /**
     * Handle deep link from Uri (public for broadcast receiver)
     */
    fun handleDeepLinkUri(data: Uri, navController: NavController): Boolean {
        Timber.d("🔗 DEEP LINK: Deep link received: $data")
        Timber.d("🔗 DEEP LINK: Scheme: ${data.scheme}, Host: ${data.host}, Path: ${data.path}")
        Timber.d("🔗 DEEP LINK: Path segments: ${data.pathSegments}")
        
        return when {
            // App scheme: dutype://job/123
            data.scheme == SCHEME && data.host == HOST_JOB -> {
                val jobId = data.lastPathSegment
                Timber.d("🔗 DEEP LINK: Detected job deep link - jobId: $jobId")
                if (jobId != null) {
                    navigateToJob(navController, jobId)
                    true
                } else {
                    Timber.w("🔗 DEEP LINK: ⚠️ Job ID is null")
                    false
                }
            }
            
            // App scheme: dutype://worker/456
            data.scheme == SCHEME && data.host == HOST_WORKER -> {
                val pathSegments = data.pathSegments
                when {
                    // dutype://worker/applications/123
                    pathSegments.size >= 2 && pathSegments[0] == "applications" -> {
                        val applicationId = pathSegments[1]
                        navigateToWorkerApplication(navController, applicationId)
                        true
                    }
                    // dutype://worker/applications
                    pathSegments.firstOrNull() == "applications" -> {
                        navigateToWorkerApplications(navController)
                        true
                    }
                    // dutype://worker/jobs
                    pathSegments.firstOrNull() == "jobs" -> {
                        navigateToWorkerJobs(navController)
                        true
                    }
                    // dutype://worker/profile
                    pathSegments.firstOrNull() == "profile" -> {
                        navigateToWorkerProfile(navController)
                        true
                    }
                    // dutype://worker/refer or dutype://worker/referrals
                    pathSegments.firstOrNull() in setOf("refer", "referrals") -> {
                        navigateToReferralHome(navController)
                        true
                    }
                    // dutype://worker/456 (profile by ID)
                    pathSegments.size == 1 -> {
                        val workerId = pathSegments[0]
                        navigateToWorkerProfileById(navController, workerId)
                        true
                    }
                    // dutype://worker/home
                    pathSegments.firstOrNull() == "home" -> {
                        navigateToWorkerHome(navController)
                        true
                    }
                    // dutype://worker/notifications
                    pathSegments.firstOrNull() == "notifications" -> {
                        navigateToWorkerNotifications(navController)
                        true
                    }
                    else -> false
                }
            }
            
            // App scheme: dutype://employer/*
            data.scheme == SCHEME && data.host == HOST_EMPLOYER -> {
                val pathSegments = data.pathSegments
                when {
                    // dutype://employer/applications/123
                    pathSegments.size >= 2 && pathSegments[0] == "applications" -> {
                        val applicationId = pathSegments[1]
                        navigateToEmployerApplication(navController, applicationId)
                        true
                    }
                    // dutype://employer/applications
                    pathSegments.firstOrNull() == "applications" -> {
                        navigateToEmployerApplications(navController)
                        true
                    }
                    // dutype://employer/jobs/123
                    pathSegments.size >= 2 && pathSegments[0] == "jobs" -> {
                        val jobId = pathSegments[1]
                        navigateToEmployerJob(navController, jobId)
                        true
                    }
                    // dutype://employer/jobs
                    pathSegments.firstOrNull() == "jobs" -> {
                        navigateToEmployerJobs(navController)
                        true
                    }
                    // dutype://employer/urgent/REQUEST_ID
                    pathSegments.size >= 2 && pathSegments[0] == "urgent" -> {
                        navigateToEmployerUrgent(navController, pathSegments[1])
                        true
                    }
                    // dutype://employer/dashboard
                    pathSegments.firstOrNull() == "dashboard" -> {
                        navigateToEmployerHome(navController)
                        true
                    }
                    // dutype://employer/post-job
                    pathSegments.firstOrNull() == "post-job" -> {
                        navigateToEmployerPostJob(navController)
                        true
                    }
                    // dutype://employer/home
                    pathSegments.firstOrNull() == "home" -> {
                        navigateToEmployerHome(navController)
                        true
                    }
                    // dutype://employer/notifications
                    pathSegments.firstOrNull() == "notifications" -> {
                        navigateToEmployerNotifications(navController)
                        true
                    }
                    // dutype://employer/profile
                    pathSegments.firstOrNull() == "profile" -> {
                        navigateToEmployerProfile(navController)
                        true
                    }
                    // dutype://employer/verification or dutype://employer/company-details
                    pathSegments.firstOrNull() in setOf("verification", "company", "company-details") -> {
                        navigateToEmployerCompanyDetails(navController)
                        true
                    }
                    // dutype://employer/refer or dutype://employer/referrals
                    pathSegments.firstOrNull() in setOf("refer", "referrals") -> {
                        navigateToEmployerReferralHome(navController)
                        true
                    }
                    else -> false
                }
            }
            
            // App scheme: dutype://refer/ABC123
            data.scheme == SCHEME && data.host == HOST_REFER -> {
                val referralCode = data.lastPathSegment
                if (referralCode != null) {
                    navigateToReferral(navController, referralCode)
                    true
                } else {
                    navigateToReferralHome(navController)
                    true
                }
            }

            // Legacy app scheme: dutype://referrals
            data.scheme == SCHEME && data.host == HOST_REFERRALS -> {
                navigateToReferralHome(navController)
                true
            }
            
            // App scheme: dutype://application/789
            data.scheme == SCHEME && data.host == HOST_APPLICATION -> {
                val applicationId = data.lastPathSegment
                if (applicationId != null) {
                    navigateToApplication(navController, applicationId)
                    true
                } else false
            }

            // App scheme: dutype://applications
            data.scheme == SCHEME && data.host == HOST_APPLICATIONS -> {
                navigateToWorkerApplications(navController)
                true
            }

            // Legacy app scheme: dutype://post-job
            data.scheme == SCHEME && data.host == HOST_POST_JOB -> {
                navigateToEmployerPostJob(navController)
                true
            }

            // App scheme: dutype://help or dutype://support
            data.scheme == SCHEME && data.host in setOf(HOST_HELP, HOST_SUPPORT) -> {
                navigateToHelp(navController)
                true
            }

            // App scheme: dutype://contact
            data.scheme == SCHEME && data.host == HOST_CONTACT -> {
                navigateToContact(navController)
                true
            }

            // App scheme: dutype://jobs
            data.scheme == SCHEME && data.host == HOST_JOBS -> {
                navigateToWorkerJobs(navController)
                true
            }
            
            // App scheme: dutype://profile
            data.scheme == SCHEME && data.host == HOST_PROFILE -> {
                navigateToProfile(navController)
                true
            }
            
            // App scheme: dutype://notifications
            data.scheme == SCHEME && data.host == HOST_NOTIFICATIONS -> {
                navigateToNotifications(navController)
                true
            }
            
            // App scheme: dutype://home
            data.scheme == SCHEME && data.host == HOST_HOME -> {
                navigateToHome(navController)
                true
            }
            
            // Android App Link: https://dutype.in/jobs/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == WEB_JOBS_PATH -> {
                val jobId = data.pathSegments.getOrNull(1)
                Timber.d("🔗 DEEP LINK: Detected web job link - jobId: $jobId")
                Timber.d("🔗 DEEP LINK: Full path segments: ${data.pathSegments}")
                if (jobId != null) {
                    navigateToJob(navController, jobId)
                    true
                } else {
                    Timber.w("🔗 DEEP LINK: ⚠️ Job ID is null in web link")
                    false
                }
            }
            
            // Android App Link: https://dutype.in/refer/vamsi9843
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == WEB_REFER_PATH -> {
                val referralCode = data.pathSegments.getOrNull(1)
                if (referralCode != null) {
                    navigateToReferral(navController, referralCode)
                    true
                } else false
            }
            
            // Android App Link: https://dutype.in/worker/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "worker" -> {
                val workerId = data.pathSegments.getOrNull(1)
                if (workerId != null) {
                    navigateToWorkerProfileById(navController, workerId)
                    true
                } else false
            }
            
            // Android App Link: https://dutype.in/application/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "application" -> {
                val applicationId = data.pathSegments.getOrNull(1)
                if (applicationId != null) {
                    navigateToApplication(navController, applicationId)
                    true
                } else false
            }
            
            // Android App Link: https://dutype.in/employer/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "employer" -> {
                val employerId = data.pathSegments.getOrNull(1)
                if (employerId != null) {
                    navigateToEmployerProfileById(navController, employerId)
                    true
                } else false
            }
            
            // Android App Link: https://dutype.in/profile
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "profile" -> {
                navigateToProfile(navController)
                true
            }
            
            // Android App Link: https://dutype.in/notifications
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "notifications" -> {
                navigateToNotifications(navController)
                true
            }

            // Android App Link: https://dutype.in/help or /support
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() in setOf("help", "support") -> {
                navigateToHelp(navController)
                true
            }

            // Android App Link: https://dutype.in/contact
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "contact" -> {
                navigateToContact(navController)
                true
            }
            
            else -> {
                Timber.w("🔗 DEEP LINK: ⚠️ Unhandled deep link: $data")
                Timber.w("🔗 DEEP LINK: Scheme: ${data.scheme}, Host: ${data.host}")
                Timber.w("🔗 DEEP LINK: Path: ${data.path}, Segments: ${data.pathSegments}")
                false
            }
        }
    }
    
    /**
     * Navigate to job detail screen
     */
    private fun navigateToJob(navController: NavController, jobId: String) {
        safeNavigate(navController, Routes.jobDetailRoute(jobId), "job:$jobId")
    }
    
    private fun navigateToReferral(navController: NavController, referralCode: String) {
        safeNavigate(navController, "${Routes.WORKER_REFER_EARN}?code=$referralCode", "referral:$referralCode")
    }

    private fun navigateToReferralHome(navController: NavController) {
        if (tryNavigate(navController, Routes.WORKER_REFER_EARN, "worker-referrals")) return
        // if (tryNavigate(navController, Routes.EMPLOYER_REFER_EARN, "employer-referrals")) return
        safeNavigate(navController, Routes.WORKER_HOME, "referrals-fallback")
    }
    
    private fun navigateToApplication(navController: NavController, applicationId: String) {
        // Fallback to worker applications list since there is no standalone application_detail route.
        safeNavigate(navController, com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS, "application:$applicationId")
    }
    
    private fun navigateToWorkerApplication(navController: NavController, applicationId: String) {
        safeNavigate(navController, "${com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS}?applicationId=$applicationId", "worker-application:$applicationId")
    }

    private fun navigateToWorkerApplications(navController: NavController) {
        safeNavigate(navController, com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS, "worker-applications")
    }

    private fun navigateToWorkerJobs(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_ALL_JOBS, "worker-jobs")
    }
    
    private fun navigateToEmployerApplication(navController: NavController, applicationId: String) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_APPLICATIONS, "employer-application:$applicationId")
    }

    private fun navigateToEmployerApplications(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_APPLICATIONS, "employer-applications")
    }
    
    private fun navigateToEmployerJob(navController: NavController, jobId: String) {
        safeNavigateEmployerInner(navController, Routes.employerJobPreviewRoute(jobId), "employer-job:$jobId")
    }

    private fun navigateToEmployerJobs(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_MY_JOBS, "employer-jobs")
    }

    private fun navigateToEmployerUrgent(navController: NavController, requestId: String) {
        safeNavigateEmployerInner(navController, Routes.employerUrgentNeedDetailRoute(requestId), "employer-urgent:$requestId")
    }

    private fun navigateToEmployerCompanyDetails(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_COMPANY_DETAILS, "employer-company-details")
    }

    private fun navigateToEmployerReferralHome(navController: NavController) {
        // safeNavigateEmployerInner(navController, Routes.EMPLOYER_REFER_EARN, "employer-referrals")
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_HOME, "employer-home-fallback")
    }
    
    private fun navigateToProfile(navController: NavController) {
        safeNavigate(navController, com.example.dutype.navigation.WorkerBottomRoutes.PROFILE, "profile")
    }
    
    private fun navigateToNotifications(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_NOTIFICATIONS, "notifications")
    }

    private fun navigateToHelp(navController: NavController) {
        if (tryNavigate(navController, Routes.HELP, "help")) return
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_HELP, "employer-help")
    }

    private fun navigateToContact(navController: NavController) {
        if (tryNavigate(navController, Routes.CONTACT_US, "contact")) return
        safeNavigate(navController, Routes.WORKER_HOME, "contact-fallback")
    }
    
    private fun navigateToWorkerNotifications(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_NOTIFICATIONS, "worker-notifications")
    }
    
    private fun navigateToEmployerNotifications(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_NOTIFICATIONS, "employer-notifications")
    }
    
    private fun navigateToHome(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_HOME, "home")
    }
    
    private fun navigateToWorkerHome(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_HOME, "worker-home")
    }
    
    private fun navigateToEmployerHome(navController: NavController) {
        safeNavigate(navController, Routes.EMPLOYER_HOME, "employer-home")
    }
    
    private fun navigateToEmployerProfile(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_PROFILE, "employer-profile")
    }
    
    private fun navigateToEmployerPostJob(navController: NavController) {
        safeNavigateEmployerInner(navController, Routes.EMPLOYER_POST_JOB, "employer-post-job")
    }

    private fun safeNavigateEmployerInner(navController: NavController, route: String, label: String) {
        try {
            navController.navigate(route) { launchSingleTop = true }
            Timber.i("DEEP LINK: employer inner $label navigation successful")
        } catch (e: IllegalArgumentException) {
            com.example.dutype.navigation.EmployerInnerNavQueue.setPending(route)
            runCatching {
                navController.navigate(Routes.EMPLOYER_HOME) { launchSingleTop = true }
            }.onSuccess {
                Timber.w(e, "DEEP LINK: queued employer inner route $route via EMPLOYER_HOME")
            }.onFailure { fallback ->
                Timber.e(fallback, "DEEP LINK: failed employer inner fallback for $label")
            }
        } catch (e: Exception) {
            Timber.e(e, "DEEP LINK: failed employer inner navigation for $label")
        }
    }

    private fun tryNavigate(navController: NavController, route: String, label: String): Boolean {
        return try {
            navController.navigate(route) { launchSingleTop = true }
            Timber.i("DEEP LINK: $label navigation successful")
            true
        } catch (e: Exception) {
            Timber.d(e, "DEEP LINK: $label route unavailable on current graph")
            false
        }
    }

    private fun handleInternalRouteString(routeInput: String, navController: NavController): Boolean {
        val parsed = Uri.parse(routeInput)
        if (parsed.scheme != null) {
            return false
        }

        val route = routeInput.trim().trimStart('/').substringBefore('?')
        if (route.isBlank()) {
            return false
        }

        val routeKey = route.lowercase()
        val segments = route.split('/').filter { it.isNotBlank() }
        val lowerSegments = segments.map { it.lowercase() }

        return when {
            routeKey in setOf(Routes.WORKER_HOME, "worker", "worker/home", "app/worker", "app/worker/home") -> {
                navigateToWorkerHome(navController)
                true
            }
            routeKey in setOf(Routes.EMPLOYER_HOME, "employer/home", "app/employer", "app/employer/home") -> {
                navigateToEmployerHome(navController)
                true
            }
            routeKey in setOf(Routes.WORKER_NOTIFICATIONS, "notifications", "worker/notifications", "app/worker/notifications") -> {
                navigateToWorkerNotifications(navController)
                true
            }
            routeKey in setOf(Routes.EMPLOYER_NOTIFICATIONS, "employer/notifications", "app/employer/notifications") -> {
                navigateToEmployerNotifications(navController)
                true
            }
            routeKey in setOf(Routes.WORKER_ALL_JOBS, "jobs", "worker/jobs", "app/worker/jobs") -> {
                navigateToWorkerJobs(navController)
                true
            }
            routeKey in setOf(Routes.EMPLOYER_MY_JOBS, "employer/jobs", "app/employer/jobs") -> {
                navigateToEmployerJobs(navController)
                true
            }
            routeKey in setOf(Routes.EMPLOYER_POST_JOB, "post-job", "post_job", "employer/post-job", "app/employer/post-job") -> {
                navigateToEmployerPostJob(navController)
                true
            }
            routeKey in setOf(Routes.WORKER_PROFILE_DETAILS, "profile", "worker/profile", "app/worker/profile") -> {
                navigateToWorkerProfile(navController)
                true
            }
            routeKey in setOf(Routes.EMPLOYER_PROFILE, "employer/profile", "app/employer/profile") -> {
                navigateToEmployerProfile(navController)
                true
            }
            routeKey in setOf("refer", "referral", "referrals", "worker/refer", "worker/referrals", "app/worker/refer") -> {
                navigateToReferralHome(navController)
                true
            }
            routeKey in setOf("employer/refer", "employer/referrals", "app/employer/refer") -> {
                navigateToEmployerReferralHome(navController)
                true
            }
            routeKey in setOf("help", "support", "contact-support", "worker/help", "worker/support", "app/worker/help") -> {
                navigateToHelp(navController)
                true
            }
            routeKey in setOf("contact", "contact-us", "contact_us", "worker/contact", "app/worker/contact") -> {
                navigateToContact(navController)
                true
            }
            routeKey in setOf("employer/help", "employer/support", "app/employer/help") -> {
                safeNavigateEmployerInner(navController, Routes.EMPLOYER_HELP, "employer-help")
                true
            }
            routeKey in setOf("employer/verification", "employer/company", "employer/company-details", "app/employer/company-details") -> {
                navigateToEmployerCompanyDetails(navController)
                true
            }
            lowerSegments.size >= 2 && lowerSegments[0] in setOf("job", "jobs") -> {
                navigateToJob(navController, segments[1])
                true
            }
            lowerSegments.size >= 3 && lowerSegments[0] == "app" && lowerSegments[1] == "worker" && lowerSegments[2] == "jobs" -> {
                val jobId = segments.getOrNull(3)
                if (jobId != null) {
                    navigateToJob(navController, jobId)
                } else {
                    navigateToWorkerJobs(navController)
                }
                true
            }
            lowerSegments.size >= 3 && lowerSegments[0] == "employer" && lowerSegments[1] == "jobs" -> {
                navigateToEmployerJob(navController, segments[2])
                true
            }
            lowerSegments.size >= 4 && lowerSegments[0] == "app" && lowerSegments[1] == "employer" && lowerSegments[2] == "jobs" -> {
                navigateToEmployerJob(navController, segments[3])
                true
            }
            else -> false
        }
    }

    private fun openExternalAction(deepLinkUrl: String, context: Context): Boolean {
        return try {
            val uri = Uri.parse(deepLinkUrl.trim())
            val scheme = uri.scheme?.lowercase()
            if (scheme !in setOf("http", "https", "market")) {
                return false
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Timber.i("DEEP LINK: external action opened: $deepLinkUrl")
            true
        } catch (e: Exception) {
            Timber.e(e, "DEEP LINK: failed to open external action: $deepLinkUrl")
            false
        }
    }
    
    private fun navigateToWorkerProfile(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_PROFILE_DETAILS, "worker-profile")
    }
    
    private fun navigateToWorkerProfileById(navController: NavController, workerId: String) {
        safeNavigate(navController, Routes.workerProfileViewRoute(workerId), "worker-profile:$workerId")
    }
    
    private fun navigateToEmployerProfileById(navController: NavController, employerId: String) {
        safeNavigate(navController, Routes.employerProfileViewRoute(employerId), "employer-profile:$employerId")
    }
    
    // ==========================================
    // DEEP LINK GENERATION (For Sharing)
    // ==========================================
    
    /**
     * Generate Android App Link for referral (Opens Android app directly)
     * Format: https://dutype.in/refer/vamsi9843
     * 
     * This is an Android App Link that:
     * - Opens the Android app directly if installed
     * - Falls back to Play Store if app not installed
     * - Works with QR codes, WhatsApp, SMS, etc.
     * 
     * Domain: dutype.in
     */
    fun generateReferralWebLink(referralCode: String): String {
        return "https://dutype.in/refer/$referralCode"
    }
    
    /**
     * Generate Android App Link for job (Opens Android app directly)
     * Format: https://dutype.in/jobs/jobId123?v=timestamp
     * 
     * IMPORTANT: Adds timestamp parameter to bypass WhatsApp's 7-day link preview cache
     * This ensures users always see the latest "DutyPe - Find Hyperlocal Jobs" preview
     */
    fun generateJobWebLink(jobId: String): String {
        val timestamp = System.currentTimeMillis()
        return "https://dutype.in/jobs/$jobId?v=$timestamp"
    }
    
    /**
     * Generate Android App Link for worker profile
     * Format: https://dutype.in/worker/workerId123
     */
    fun generateWorkerWebLink(workerId: String): String {
        return "https://dutype.in/worker/$workerId"
    }
}
