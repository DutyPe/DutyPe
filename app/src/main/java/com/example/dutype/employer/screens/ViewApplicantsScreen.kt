package com.example.dutype.employer.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun ViewApplicantsScreen(navController: NavController, jobId: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Applicants for Job ID: $jobId",
            style = MaterialTheme.typography.headlineMedium
        )

        // Show list of applicants who applied for this job
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewApplicantsScreenPreview() {
    ViewApplicantsScreen(navController = rememberNavController(), jobId = "sample_job_id")
}
