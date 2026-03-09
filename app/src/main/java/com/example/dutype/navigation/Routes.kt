package com.example.dutype.navigation

object Routes {
    // Main Navigation Routes
    const val ONBOARDING = "onboarding"
    const val ENHANCED_LOGIN = "enhanced_login"
    const val MANUAL_LOCATION_ROUTE = "manual_location_route"
    const val SELECT_ROLE = "select_role"
    const val WORKER_HOME = "worker_home"
    const val EMPLOYER_HOME = "employer_home"
    const val EMPLOYER_COMPANY_DETAILS = "employer_company_details"
    
    // Worker Routes
    const val WORKER_HOME_TAB = "home"
    const val WORKER_MY_JOBS = "myjobs"
    const val WORKER_PROFILE = "profile"
    const val WORKER_PROFILE_DETAILS = "worker_profile_details"
    const val WORKER_VISITING_CARD = "worker_visiting_card"
    const val EMPLOYER_VISITING_CARD = "employer_visiting_card"
    const val WORKER_ALL_JOBS = "worker_all_jobs"
    const val WORKER_ALL_JOBS_FILTERED = "worker_all_jobs/{filter}"
    const val WORKER_CATEGORIES = "worker_categories"
    const val WORKER_CATEGORIES_FILTERED = "worker_categories/{category}"
    const val JOB_DETAIL = "job_detail_route/{jobId}"
    const val JOB_APPLICATION = "job_application/{jobId}"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_SETUP_WITH_RETURN = "profile_setup?returnRoute={returnRoute}"
    const val EMPLOYER_NOTIFICATIONS = "employer_notifications"
    const val WORKER_NOTIFICATIONS = "worker_notifications"
    const val LOGOUT = "logout"
    const val HELP = "help"
    const val CHAT_SUPPORT = "chat_support"
    const val CALL_SUPPORT = "call_support"
    const val REPORT = "report"
    const val TUTORIAL = "tutorial"
    const val FAQ = "faq" 
    const val ABOUT_US = "aboutUs"
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
    const val EMPLOYER_PROFILE_VIEW = "employer_profile_view/{employerId}"
    const val EMPLOYER_APPLICATIONS = "employer_applications"
    const val EMPLOYER_APPLICATIONS_JOB = "employer_applications_job/{jobId}"
    const val EMPLOYER_APPLICATION_DETAIL = "employer_application_detail/{applicationId}"
    const val EMPLOYER_MORE_SETTINGS = "employer_more_settings"
    const val EMPLOYER_MY_RATINGS = "employer_my_ratings"
    const val EMPLOYER_TRUST_BADGES = "employer_trust_badges"
    const val CONTACT_US = "contact_us"
    
    // History Routes
    const val WORKER_HISTORY = "worker_history"
    const val EMPLOYER_HISTORY = "employer_history"
    
    // Map-First Interface Route (Accessibility Feature)
    const val WORKER_JOB_MAP = "worker_job_map"
    
    // Language Selection
    const val LANGUAGE_SELECTION = "language_selection"
    
    // Chat Routes
    const val CHAT_CONVERSATIONS = "chat_conversations"
    const val CHAT_CONVERSATION_DETAIL = "chat_conversation/{conversationId}"
    
    // Earnings Dashboard (Worker Financial Clarity)
    const val WORKER_EARNINGS = "worker_earnings"
    
    // Worker Refer & Earn
    const val WORKER_REFER_EARN = "worker_refer_earn"
    
    // Typography Showcase (Dev Tool) - REMOVED FOR PRODUCTION
    // const val TYPOGRAPHY_SHOWCASE = "typography_showcase"
    
    // AI Chatbot Routes
    const val WORKER_AI_CHAT = "worker_ai_chat"
    const val EMPLOYER_AI_CHAT = "employer_ai_chat"
    
    // AI-Enhanced Job Posting
    const val EMPLOYER_AI_POST_JOB = "employer_ai_post_job"
    
    // Voice Job Posting
    const val EMPLOYER_VOICE_POST_JOB = "employer_voice_post_job"
    
    // Work Start Verification Routes
    const val WORKER_WORK_START_QR = "worker_work_start_qr/{jobId}"
    const val EMPLOYER_VERIFY_WORK = "employer_verify_work/{jobId}/{applicationId}"
    
    // Utility functions
    fun jobDetailRoute(jobId: String): String {
        return "job_detail_route/$jobId"
    }
    
    fun jobApplicationRoute(jobId: String): String {
        return "job_application/$jobId"
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
    
    fun employerProfileViewRoute(employerId: String): String {
        return "employer_profile_view/$employerId"
    }
    
    fun messageWorkerRoute(workerId: String): String {
        return "message/$workerId"
    }
    
    fun allJobsRoute(filter: String = "All Jobs"): String {
        return "worker_all_jobs/$filter"
    }
    
    fun categoriesRoute(category: String? = null): String {
        return if (category != null) "worker_categories/$category" else "worker_categories"
    }
    
    fun workerWorkStartQRRoute(jobId: String): String {
        return "worker_work_start_qr/$jobId"
    }
    
    fun employerVerifyWorkRoute(jobId: String, applicationId: String): String {
        return "employer_verify_work/$jobId/$applicationId"
    }
    
    fun chatConversationDetailRoute(conversationId: String): String {
        return "chat_conversation/$conversationId"
    }
    
    fun profileSetupWithReturnRoute(returnRoute: String): String {
        return "profile_setup?returnRoute=${java.net.URLEncoder.encode(returnRoute, "UTF-8")}"
    }
    
    fun employerProfileSetupWithReturnRoute(returnRoute: String): String {
        return "employer_profile_setup?returnRoute=${java.net.URLEncoder.encode(returnRoute, "UTF-8")}"
    }
}
