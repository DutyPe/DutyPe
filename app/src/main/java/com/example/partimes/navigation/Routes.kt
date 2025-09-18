package com.example.partimes.navigation

object Routes {
    // Main Navigation Routes
    const val SPLASH = "splash"
    const val LOGIN_BOTTOM_SHEET = "login_bottom_sheet"
    const val LOCATION_SERVICE = "location_service"
    const val LOCATION_SERVICE_SCREEN_ROUTE = "location_service_screen_route"
    const val MANUAL_LOCATION_ROUTE = "manual_location_route"
    const val SELECT_ROLE = "select_role"
    const val JOBSEEKER_HOME = "jobseeker_home"
    const val EMPLOYER_HOME = "employer_home"
    const val LOGIN_SIGNUP_ROUTE = "login_signup_route"
    
    // Job Seeker Routes
    const val JOBSEEKER_HOME_TAB = "home"
    const val JOBSEEKER_MY_JOBS = "myjobs"
    const val JOBSEEKER_PROFILE = "profile"
    const val JOB_DETAIL = "job_detail_route/{jobId}"
    const val SECURITY = "security"
    const val LOGOUT = "logout"
    const val HELP = "help"
    const val CHAT_SUPPORT = "chat_support"
    const val CALL_SUPPORT = "call_support"
    const val REPORT = "report"
    const val TUTORIAL = "tutorial"
    const val FAQ = "faq"
    const val ABOUT_US = "aboutUs"
    const val PRIVACY = "privacy"
    const val TERMS = "terms"
    const val CHAT_DETAIL = "chat_detail/{name}"
    
    // Employer Routes
    const val EMPLOYER_DASHBOARD = "dashboard"
    const val EMPLOYER_PROFILE = "employer_profile"
    const val EMPLOYER_POST_JOB = "employer_post_job"
    const val EMPLOYER_MY_JOBS = "employer_my_jobs"
    const val VIEW_APPLICANTS = "view_applicants/{jobId}"
    const val EMPLOYER_ABOUT = "employer_about"
    const val EMPLOYER_HELP = "employer_help"
    const val EMPLOYER_FAQ = "employer_faq"
    const val EMPLOYER_CHAT_SUPPORT = "employer_chat_support"
    const val EMPLOYER_CALL_SUPPORT = "employer_call_support"
    const val EMPLOYER_REPORT = "employer_report"
    const val EMPLOYER_TUTORIAL = "employer_tutorial"
    const val EMPLOYER_NOTIFICATIONS = "employer_notifications"
    const val EMPLOYER_MANAGE_ADDRESSES = "employer_manage_addresses"
    const val EMPLOYER_REVIEWS = "employer_reviews"
    const val EMPLOYER_REFER_EARN = "employer_refer_earn"
    
    // Utility functions
    fun jobDetailRoute(jobId: String): String {
        return "job_detail_route/$jobId"
    }
    
    fun chatDetailRoute(name: String): String {
        return "chat_detail/$name"
    }
    
    fun viewApplicantsRoute(jobId: String): String {
        return "view_applicants/$jobId"
    }
}
