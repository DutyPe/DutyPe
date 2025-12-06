package com.example.dutype.navigation

object Routes {
    // Main Navigation Routes
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val ENHANCED_LOGIN = "enhanced_login"
    const val LOCATION_SERVICE = "location_service"
    const val MANUAL_LOCATION_ROUTE = "manual_location_route"
    const val SELECT_ROLE = "select_role"
    const val WORKER_HOME = "worker_home"
    const val EMPLOYER_HOME = "employer_home"
    const val EMPLOYER_COMPANY_DETAILS = "employer_company_details"
    const val LOGIN_SIGNUP_ROUTE = "login_signup_route"
    const val REGISTER = "register"
    
    // Worker Routes
    const val WORKER_HOME_TAB = "home"
    const val WORKER_MY_JOBS = "myjobs"
    const val WORKER_PROFILE = "profile"
    const val WORKER_PROFILE_DETAILS = "worker_profile_details"
    const val JOB_DETAIL = "job_detail_route/{jobId}"
    const val JOB_APPLICATION = "job_application/{jobId}"
    const val SMART_JOB_APPLICATION = "smart_job_application/{jobId}"
    const val PROFILE_SETUP = "profile_setup"
    const val EMPLOYER_NOTIFICATIONS = "employer_notifications"
    const val WORKER_NOTIFICATIONS = "worker_notifications"
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
    const val EMPLOYER_PROFILE_SETUP = "employer_profile_setup"
    const val EMPLOYER_MY_JOBS = "employer_my_jobs"
    const val EDIT_JOB = "edit_job/{jobId}"
    const val VIEW_APPLICANTS = "view_applicants/{jobId}"
    const val EMPLOYER_ABOUT = "employer_about"
    const val EMPLOYER_HELP = "employer_help"
    const val EMPLOYER_MANAGE_ADDRESSES = "employer_manage_addresses"
    const val EMPLOYER_REFER_EARN = "employer_refer_earn"
    const val COMPANY_DETAILS = "company_details"
    const val ANALYTICS = "analytics"
    const val WORKER_PROFILE_VIEW = "worker_profile_view/{workerId}"
    const val EMPLOYER_APPLICATIONS = "employer_applications"
    const val EMPLOYER_APPLICATIONS_JOB = "employer_applications_job/{jobId}"
    const val EMPLOYER_APPLICATION_DETAIL = "employer_application_detail/{applicationId}"
    
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
    
    fun editJobRoute(jobId: String): String {
        return "edit_job/$jobId"
    }
    
    fun workerProfileViewRoute(workerId: String): String {
        return "worker_profile_view/$workerId"
    }
    
    fun messageWorkerRoute(workerId: String): String {
        return "message/$workerId"
    }
}
