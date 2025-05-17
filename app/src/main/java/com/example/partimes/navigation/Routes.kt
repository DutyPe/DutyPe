package com.example.partimes.navigation

object Routes {
    const val JOB_DETAILS_BASE = "jobDetails"
    fun jobDetailRoute(jobId: String) = "$JOB_DETAILS_BASE/$jobId"
    const val JOB_DETAILS_WITH_ARG = "$JOB_DETAILS_BASE/{jobId}"
//
//    const val JobDetails = "jobDetails/{jobId}"
//    const val ChatDetail = "chat_detail/{name}"

    // Optional: If you want, add others too:
    // const val Profile = "profile"
    // const val HelpMain = "help"
    // etc.
}
