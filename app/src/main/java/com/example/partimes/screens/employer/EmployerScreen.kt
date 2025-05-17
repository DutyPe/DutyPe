package com.example.partimes.screens.employer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

sealed class EmployerScreen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : EmployerScreen("employer_dashboard", "Home", Icons.Default.Home)
    object PostJob : EmployerScreen("employer_post_job", "Post", Icons.Default.AddCircle)
    object MyJobs : EmployerScreen("employer_my_jobs", "My Jobs", Icons.Default.Work)
    object Profile : EmployerScreen("employer_profile", "Profile", Icons.Default.Person)
//    object Support : EmployerScreen("employer_support", "Support", Icons.Default.Help)

    object ViewApplicants : EmployerScreen("employer_view_applicants/{jobId}", "Applicants", Icons.Default.People)
}