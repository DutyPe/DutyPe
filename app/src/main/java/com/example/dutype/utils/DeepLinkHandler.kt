package com.example.dutype.utils

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
                Routes.WORKER_PROFILE,
                Routes.WORKER_NOTIFICATIONS,
                Routes.WORKER_ALL_JOBS,
                Routes.WORKER_MY_JOBS -> Routes.WORKER_HOME
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
            val uri = Uri.parse(deepLinkUrl)
            handleDeepLinkUri(uri, navController)
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to parse deep link: $deepLinkUrl")
            false
        }
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
                    else -> false
                }
            }
            
            // App scheme: dutype://refer/ABC123
            data.scheme == SCHEME && data.host == HOST_REFER -> {
                val referralCode = data.lastPathSegment
                if (referralCode != null) {
                    navigateToReferral(navController, referralCode)
                    true
                } else false
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
    
    private fun navigateToApplication(navController: NavController, applicationId: String) {
        // Fallback to worker applications list since there is no standalone application_detail route.
        safeNavigate(navController, Routes.WORKER_MY_JOBS, "application:$applicationId")
    }
    
    private fun navigateToWorkerApplication(navController: NavController, applicationId: String) {
        safeNavigate(navController, "${Routes.WORKER_MY_JOBS}?applicationId=$applicationId", "worker-application:$applicationId")
    }

    private fun navigateToWorkerApplications(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_MY_JOBS, "worker-applications")
    }

    private fun navigateToWorkerJobs(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_ALL_JOBS, "worker-jobs")
    }
    
    private fun navigateToEmployerApplication(navController: NavController, applicationId: String) {
        safeNavigate(navController, Routes.EMPLOYER_APPLICATIONS, "employer-application:$applicationId")
    }

    private fun navigateToEmployerApplications(navController: NavController) {
        safeNavigate(navController, Routes.EMPLOYER_APPLICATIONS, "employer-applications")
    }
    
    private fun navigateToEmployerJob(navController: NavController, jobId: String) {
        safeNavigate(navController, "${Routes.EMPLOYER_MY_JOBS}?jobId=$jobId", "employer-job:$jobId")
    }

    private fun navigateToEmployerJobs(navController: NavController) {
        safeNavigate(navController, Routes.EMPLOYER_MY_JOBS, "employer-jobs")
    }
    
    private fun navigateToProfile(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_PROFILE, "profile")
    }
    
    private fun navigateToNotifications(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_NOTIFICATIONS, "notifications")
    }
    
    private fun navigateToWorkerNotifications(navController: NavController) {
        safeNavigate(navController, Routes.WORKER_NOTIFICATIONS, "worker-notifications")
    }
    
    private fun navigateToEmployerNotifications(navController: NavController) {
        safeNavigate(navController, Routes.EMPLOYER_NOTIFICATIONS, "employer-notifications")
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
        safeNavigate(navController, Routes.EMPLOYER_PROFILE, "employer-profile")
    }
    
    private fun navigateToEmployerPostJob(navController: NavController) {
        safeNavigate(navController, Routes.EMPLOYER_POST_JOB, "employer-post-job")
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
