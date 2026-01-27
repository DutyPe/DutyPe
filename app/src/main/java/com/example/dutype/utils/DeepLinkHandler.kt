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
 * - https://dutypeapp.web.app/jobs/{jobId} - Web link to job
 * - https://dutypeapp.web.app/refer/{code} - Web link to referral
 * - https://dutypeapp.web.app/worker/{workerId} - Web link to worker profile
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
    private const val HOST_CHAT = "chat"
    private const val HOST_PROFILE = "profile"
    private const val HOST_NOTIFICATIONS = "notifications"
    private const val HOST_HOME = "home"
    private const val HOST_JOBS = "jobs"
    
    // Web URLs
    private const val WEB_DOMAIN = "dutypeapp.web.app"
    private const val WEB_JOBS_PATH = "jobs"
    private const val WEB_REFER_PATH = "refer"
    
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
        Timber.d("🔗 Deep link received: $data")
        Timber.d("🔗 Scheme: ${data.scheme}, Host: ${data.host}, Path: ${data.path}")
        
        return when {
            // App scheme: dutype://job/123
            data.scheme == SCHEME && data.host == HOST_JOB -> {
                val jobId = data.lastPathSegment
                if (jobId != null) {
                    navigateToJob(navController, jobId)
                    true
                } else false
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
                    // dutype://employer/jobs/123
                    pathSegments.size >= 2 && pathSegments[0] == "jobs" -> {
                        val jobId = pathSegments[1]
                        navigateToEmployerJob(navController, jobId)
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
            
            // App scheme: dutype://chat/123
            data.scheme == SCHEME && data.host == HOST_CHAT -> {
                val conversationId = data.lastPathSegment
                if (conversationId != null) {
                    navigateToChat(navController, conversationId)
                    true
                } else {
                    navigateToChatList(navController)
                    true
                }
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
            
            // Web URL: https://dutype.app/jobs/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == WEB_JOBS_PATH -> {
                val jobId = data.pathSegments.getOrNull(1)
                if (jobId != null) {
                    navigateToJob(navController, jobId)
                    true
                } else false
            }
            
            // Web URL: https://dutype.app/refer/ABC123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == WEB_REFER_PATH -> {
                val referralCode = data.pathSegments.getOrNull(1)
                if (referralCode != null) {
                    navigateToReferral(navController, referralCode)
                    true
                } else false
            }
            
            // Web URL: https://dutypeapp.web.app/worker/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "worker" -> {
                val workerId = data.pathSegments.getOrNull(1)
                if (workerId != null) {
                    navigateToWorkerProfileById(navController, workerId)
                    true
                } else false
            }
            
            // Web URL: https://dutypeapp.web.app/application/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "application" -> {
                val applicationId = data.pathSegments.getOrNull(1)
                if (applicationId != null) {
                    navigateToApplication(navController, applicationId)
                    true
                } else false
            }
            
            // Web URL: https://dutypeapp.web.app/employer/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "employer" -> {
                val employerId = data.pathSegments.getOrNull(1)
                if (employerId != null) {
                    navigateToEmployerProfile(navController)
                    true
                } else false
            }
            
            // Web URL: https://dutypeapp.web.app/chat/123
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "chat" -> {
                val conversationId = data.pathSegments.getOrNull(1)
                if (conversationId != null) {
                    navigateToChat(navController, conversationId)
                    true
                } else {
                    navigateToChatList(navController)
                    true
                }
            }
            
            // Web URL: https://dutypeapp.web.app/profile
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "profile" -> {
                navigateToProfile(navController)
                true
            }
            
            // Web URL: https://dutypeapp.web.app/notifications
            data.host == WEB_DOMAIN && data.pathSegments.firstOrNull() == "notifications" -> {
                navigateToNotifications(navController)
                true
            }
            
            else -> {
                Timber.w("🔗 Unhandled deep link: $data")
                false
            }
        }
    }
    
    /**
     * Navigate to job detail screen
     */
    private fun navigateToJob(navController: NavController, jobId: String) {
        Timber.i("🔗 Navigating to job: $jobId")
        try {
            navController.navigate(Routes.jobDetailRoute(jobId)) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to job")
        }
    }
    
    /**
     * Navigate to referral screen with code
     */
    private fun navigateToReferral(navController: NavController, referralCode: String) {
        Timber.i("🔗 Navigating to referral with code: $referralCode")
        try {
            // Navigate to worker refer & earn screen with code
            navController.navigate("${Routes.WORKER_REFER_EARN}?code=$referralCode") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to referral")
        }
    }
    
    /**
     * Navigate to application detail
     */
    private fun navigateToApplication(navController: NavController, applicationId: String) {
        Timber.i("🔗 Navigating to application: $applicationId")
        try {
            navController.navigate("application_detail/$applicationId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to application")
        }
    }
    
    /**
     * Navigate to worker application detail
     */
    private fun navigateToWorkerApplication(navController: NavController, applicationId: String) {
        Timber.i("🔗 Navigating to worker application: $applicationId")
        try {
            // Navigate to worker's my jobs screen which shows applications
            navController.navigate("${Routes.WORKER_MY_JOBS}?applicationId=$applicationId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to worker application")
        }
    }
    
    /**
     * Navigate to employer application detail
     */
    private fun navigateToEmployerApplication(navController: NavController, applicationId: String) {
        Timber.i("🔗 Navigating to employer application: $applicationId")
        try {
            navController.navigate("${Routes.EMPLOYER_APPLICATION_DETAIL}/$applicationId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer application")
        }
    }
    
    /**
     * Navigate to employer job detail
     */
    private fun navigateToEmployerJob(navController: NavController, jobId: String) {
        Timber.i("🔗 Navigating to employer job: $jobId")
        try {
            navController.navigate("${Routes.EMPLOYER_MY_JOBS}?jobId=$jobId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer job")
        }
    }
    
    /**
     * Navigate to chat conversation
     */
    private fun navigateToChat(navController: NavController, conversationId: String) {
        Timber.i("🔗 Navigating to chat: $conversationId")
        try {
            navController.navigate("${Routes.CHAT_CONVERSATION_DETAIL.replace("{conversationId}", conversationId)}") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to chat")
        }
    }
    
    /**
     * Navigate to chat list
     */
    private fun navigateToChatList(navController: NavController) {
        Timber.i("🔗 Navigating to chat list")
        try {
            navController.navigate(Routes.CHAT_CONVERSATIONS) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to chat list")
        }
    }
    
    /**
     * Navigate to profile (role-specific)
     */
    private fun navigateToProfile(navController: NavController) {
        Timber.i("🔗 Navigating to profile")
        try {
            // Navigate to current user's profile based on role
            navController.navigate(Routes.WORKER_PROFILE) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to profile")
        }
    }
    
    /**
     * Navigate to notifications (role-specific)
     */
    private fun navigateToNotifications(navController: NavController) {
        Timber.i("🔗 Navigating to notifications")
        try {
            navController.navigate(Routes.WORKER_NOTIFICATIONS) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to notifications")
        }
    }
    
    /**
     * Navigate to worker notifications
     */
    private fun navigateToWorkerNotifications(navController: NavController) {
        Timber.i("🔗 Navigating to worker notifications")
        try {
            navController.navigate(Routes.WORKER_NOTIFICATIONS) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to worker notifications")
        }
    }
    
    /**
     * Navigate to employer notifications
     */
    private fun navigateToEmployerNotifications(navController: NavController) {
        Timber.i("🔗 Navigating to employer notifications")
        try {
            navController.navigate(Routes.EMPLOYER_NOTIFICATIONS) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer notifications")
        }
    }
    
    /**
     * Navigate to home (role-specific)
     */
    private fun navigateToHome(navController: NavController) {
        Timber.i("🔗 Navigating to home")
        try {
            navController.navigate(Routes.WORKER_HOME) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to home")
        }
    }
    
    /**
     * Navigate to worker home
     */
    private fun navigateToWorkerHome(navController: NavController) {
        Timber.i("🔗 Navigating to worker home")
        try {
            navController.navigate(Routes.WORKER_HOME) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to worker home")
        }
    }
    
    /**
     * Navigate to employer home
     */
    private fun navigateToEmployerHome(navController: NavController) {
        Timber.i("🔗 Navigating to employer home")
        try {
            navController.navigate(Routes.EMPLOYER_HOME) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer home")
        }
    }
    
    /**
     * Navigate to employer profile
     */
    private fun navigateToEmployerProfile(navController: NavController) {
        Timber.i("🔗 Navigating to employer profile")
        try {
            navController.navigate(Routes.EMPLOYER_PROFILE) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer profile")
        }
    }
    
    /**
     * Navigate to employer post job screen
     */
    private fun navigateToEmployerPostJob(navController: NavController) {
        Timber.i("🔗 Navigating to employer post job")
        try {
            navController.navigate(Routes.EMPLOYER_POST_JOB) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to employer post job")
        }
    }
    
    /**
     * Navigate to worker profile (current user)
     */
    private fun navigateToWorkerProfile(navController: NavController) {
        Timber.i("🔗 Navigating to worker profile")
        try {
            navController.navigate(Routes.WORKER_PROFILE) {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to worker profile")
        }
    }
    
    /**
     * Navigate to worker profile by ID
     */
    private fun navigateToWorkerProfileById(navController: NavController, workerId: String) {
        Timber.i("🔗 Navigating to worker profile by ID: $workerId")
        try {
            navController.navigate("worker_profile/$workerId") {
                launchSingleTop = true
            }
        } catch (e: Exception) {
            Timber.e(e, "🔗 Failed to navigate to worker profile by ID")
        }
    }
    
    // ==========================================
    // DEEP LINK GENERATION (For Sharing)
    // ==========================================
    
    /**
     * Generate shareable web link for job (Universal Link)
     * This is the primary link format - works on web and opens app if installed
     */
    fun generateJobWebLink(jobId: String): String {
        return "https://dutypeapp.web.app/jobs/$jobId"
    }
    
    /**
     * Generate app scheme deep link for job (Fallback)
     * Used when universal links are not supported
     */
    fun generateJobDeepLink(jobId: String): String {
        return "dutype://job/$jobId"
    }
    
    /**
     * Generate shareable web link for referral (Universal Link)
     * This is the primary link format - works on web and opens app if installed
     */
    fun generateReferralWebLink(referralCode: String): String {
        return "https://dutypeapp.web.app/refer/$referralCode"
    }
    
    /**
     * Generate app scheme deep link for referral (Fallback)
     */
    fun generateReferralDeepLink(referralCode: String): String {
        return "dutype://refer/$referralCode"
    }
    
    /**
     * Generate shareable link for worker profile
     */
    fun generateWorkerProfileLink(workerId: String): String {
        return "dutype://worker/$workerId"
    }
    
    /**
     * Generate shareable web link for worker profile
     */
    fun generateWorkerWebLink(workerId: String): String {
        return "https://dutypeapp.web.app/worker/$workerId"
    }
    
    /**
     * Generate shareable link for application
     */
    fun generateApplicationLink(applicationId: String): String {
        return "dutype://application/$applicationId"
    }
}
