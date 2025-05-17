package com.example.partimes.screens.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.partimes.components.BackNavigationTopBar

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Privacy Policy", navController = navController)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Paragraph("Your privacy matters to us.")
            Paragraph("Quick PartTimes collects only the information needed to help you find and apply for jobs nearby. This includes your name, phone number, location, and any skills or work preferences you provide.")
            Paragraph("We do not sell or share your data with any third-party advertisers. All your information is protected and only used to match you with relevant jobs or employers.")
            Paragraph("You can delete your account and data at any time.")
        }
    }
}
