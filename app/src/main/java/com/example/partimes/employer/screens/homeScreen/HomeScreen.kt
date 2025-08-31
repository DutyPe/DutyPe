package com.example.partimes.employer.screens.homeScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Employer Home", style = MaterialTheme.typography.headlineMedium)

        // You can add some buttons here to navigate to PostJob, MyJobs, etc.
    }
}
