package com.example.partimes.navigation

object Routes {
    // Main Navigation Routes
    const val SPLASH = "splash"
    const val ENHANCED_LOGIN = "enhanced_login"
    const val ROLE_SELECTION = "role_selection"
    const val LOGIN_BOTTOM_SHEET = "login_bottom_sheet"
    const val LOCATION_SERVICE = "location_service"
    const val LOCATION_SERVICE_SCREEN_ROUTE = "location_service_screen_route"
    const val MANUAL_LOCATION_ROUTE = "manual_location_route"
    const val SELECT_ROLE = "select_role"
    const val WORKER_ONBOARDING = "worker_onboarding"
    const val EMPLOYER_ONBOARDING = "employer_onboarding"
    const val WORKER_HOME = "worker_home"
    const val EMPLOYER_HOME = "employer_home"
    const val LOGIN_SIGNUP_ROUTE = "login_signup_route"
    const val REGISTER = "register"
    
    // Worker Routes
    const val WORKER_HOME_TAB = "home"
    const val WORKER_MY_JOBS = "myjobs"
    const val WORKER_PROFILE = "profile"
    const val JOB_DETAIL = "job_detail_route/{jobId}"
    const val PROFILE_SETUP = "profile_setup"
    const val NOTIFICATION_CENTER = "notification_center"
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
    
    // Advanced Profile Routes
    const val ADVANCED_PROFILE = "advanced_profile"
    const val SKILLS_MANAGEMENT = "skills_management"
    const val RESUME_UPLOAD = "resume_upload"
    const val VERIFICATION = "verification"
    const val PERSONAL_INFO = "personal_info"
    const val WORK_EXPERIENCE = "work_experience"
    const val EDUCATION = "education"
    const val WORK_PREFERENCES = "work_preferences"
    
    // Employer Routes
    const val EMPLOYER_DASHBOARD = "dashboard"
    const val EMPLOYER_PROFILE = "employer_profile"
    const val EMPLOYER_POST_JOB = "employer_post_job"
    const val EMPLOYER_PROFILE_SETUP = "employer_profile_setup"
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
    const val TALENT_SEARCH = "talent_search"
    const val EMPLOYER_TERMS = "employer_terms"
    const val COMPANY_DETAILS = "company_details"
    const val ANALYTICS = "analytics"
    
    // Utility functions
    fun jobDetailRoute(jobId: String): String {
        return "job_detail_route/$jobId"
    }
    
    fun applicationFormRoute(jobId: String): String {
        return "application_form/$jobId"
    }
    
    fun chatDetailRoute(name: String): String {
        return "chat_detail/$name"
    }
    
    fun viewApplicantsRoute(jobId: String): String {
        return "view_applicants/$jobId"
    }
}
