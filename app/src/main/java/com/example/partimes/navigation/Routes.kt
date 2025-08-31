package com.example.partimes.navigation

object Routes {
    const val ONBOARDING_ROUTE = "onboarding_route" 
    const val HOME_ROUTE = "home_route"
    const val JOB_DETAIL_ROUTE = "job_detail_route/{jobId}"
    const val PROFILE_ROUTE = "profile_route"
    const val POST_JOB_ROUTE = "post_job_route"
    const val JOB_APPLICATIONS_ROUTE = "job_applications_route"
    const val LOGIN_SIGNUP_ROUTE = "login_signup_route" // This is used for LoginScreen
    const val EMPLOYER_MAIN_ROUTE = "employer_main_route"
    const val JOBSEEKER_MAIN_ROUTE = "jobseeker_main_route"
    const val SPLASH_ROUTE = "splash_route"
    const val SELECT_ROLE_ROUTE = "select_role_route"
    // LOGIN_BOTTOM_SHEET_ROUTE has been removed as its functionality is merged into LoginScreen

    // PHONE_NUMBER_SCREEN_ROUTE has been removed as its functionality is merged into LoginScreen
    const val LOCATION_SERVICE_SCREEN_ROUTE = "location_service_screen_route"

    fun jobDetailRoute(jobId: String): String {
        return "job_detail_route/$jobId"
    }
}
