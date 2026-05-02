package com.example.dutype.navigation

object Routes {
    // Main Navigation Routes
    const val ONBOARDING = "onboarding"
    const val ENHANCED_LOGIN = "enhanced_login"
    const val REGISTER = "register"
    const val MANUAL_LOCATION_ROUTE = "manual_location_route"
    const val SELECT_ROLE = "select_role"
    const val WORKER_HOME = "worker_home"
    const val EMPLOYER_HOME = "employer_home"
    const val EMPLOYER_COMPANY_DETAILS = "employer_company_details"
    
    // Worker Routes
    const val WORKER_PROFILE_DETAILS = "worker_profile_details"
    const val WORKER_ALL_JOBS = "worker_all_jobs"
    const val WORKER_CATEGORIES = "worker_categories"
    const val WORKER_CATEGORIES_FILTERED = "worker_categories/{category}"
    const val JOB_DETAIL = "job_detail_route/{jobId}"
    const val JOB_APPLICATION = "job_application/{jobId}"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_SETUP_WITH_RETURN = "profile_setup?returnRoute={returnRoute}"
    const val EMPLOYER_NOTIFICATIONS = "employer_notifications"
    const val WORKER_NOTIFICATIONS = "worker_notifications"
    const val HELP = "help"
    const val REPORT = "report"
    const val ABOUT_US = "aboutUs"
    
    
    // Employer Routes
    const val EMPLOYER_DASHBOARD = "dashboard"
    const val EMPLOYER_PROFILE = "employer_profile"
    const val EMPLOYER_POST_JOB = "employer_post_job"
    const val EMPLOYER_POST_URGENT_NEED = "employer_post_urgent_need"
    const val EMPLOYER_PROFILE_SETUP = "employer_profile_setup"
    const val EMPLOYER_MY_JOBS = "employer_my_jobs"
    const val EDIT_JOB = "edit_job/{jobId}"
    const val EMPLOYER_JOB_PREVIEW = "employer_job_preview/{jobId}"
    const val ANALYTICS = "analytics"
    const val WORKER_PROFILE_VIEW = "worker_profile_view/{workerId}?applicationId={applicationId}"
    const val EMPLOYER_PROFILE_VIEW = "employer_profile_view/{employerId}"
    const val EMPLOYER_APPLICATIONS = "employer_applications"
    const val EMPLOYER_APPLICATIONS_JOB = "employer_applications_job/{jobId}"
    const val EMPLOYER_ABOUT = "employer_about"
    const val EMPLOYER_HELP = "employer_help"
    const val EMPLOYER_MANAGE_ADDRESSES = "employer_manage_addresses"
    const val EMPLOYER_REFER_EARN = "employer_refer_earn"
    const val EMPLOYER_HISTORY = "employer_history"
    const val CONTACT_US = "contact_us"
    
    // History Routes
    const val WORKER_HISTORY = "worker_history"
    
    // Map-First Interface Route (Accessibility Feature)
    const val WORKER_JOB_MAP = "worker_job_map"
    
    // Earnings Dashboard (Worker Financial Clarity)
    const val WORKER_EARNINGS = "worker_earnings"
    
    // Worker Refer & Earn
    const val WORKER_REFER_EARN = "worker_refer_earn"
    
    // Utility functions
    fun jobDetailRoute(jobId: String): String {
        return "job_detail_route/$jobId"
    }
    
    fun jobApplicationRoute(jobId: String): String {
        return "job_application/$jobId"
    }
    
    fun employerApplicationsJobRoute(jobId: String): String {
        return "employer_applications_job/$jobId"
    }
    
    fun editJobRoute(jobId: String): String {
        return "edit_job/$jobId"
    }

    fun employerJobPreviewRoute(jobId: String): String {
        return "employer_job_preview/$jobId"
    }
    
    fun workerProfileViewRoute(workerId: String, applicationId: String? = null): String {
        return if (applicationId.isNullOrBlank()) {
            "worker_profile_view/$workerId"
        } else {
            "worker_profile_view/$workerId?applicationId=${java.net.URLEncoder.encode(applicationId, "UTF-8")}"
        }
    }
    
    fun employerProfileViewRoute(employerId: String): String {
        return "employer_profile_view/$employerId"
    }
    
    fun categoriesRoute(category: String? = null): String {
        return if (category != null) "worker_categories/${android.net.Uri.encode(category)}" else "worker_categories"
    }
    
    fun profileSetupWithReturnRoute(returnRoute: String): String {
        return "profile_setup?returnRoute=${java.net.URLEncoder.encode(returnRoute, "UTF-8")}"
    }
    
    fun employerProfileSetupWithReturnRoute(returnRoute: String): String {
        return "employer_profile_setup?returnRoute=${java.net.URLEncoder.encode(returnRoute, "UTF-8")}"
    }
}
