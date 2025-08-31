package com.example.partimes.common.chat.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.components.BackNavigationTopBar

@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Terms & Conditions", navController = navController)
        }
    ) { paddingValues -> // Scaffold automatically gives the padding values
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues) // Apply the dynamic padding for top bar
        ) {
            Text(
                text = "Terms & Conditions",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            BulletPoint("Provide accurate and truthful information on your profile.")
            BulletPoint("Use the app for legal, job-related purposes only.")
            BulletPoint("Do not share your account or misuse other users' information.")
            BulletPoint("Follow local employment laws and agreements made with employers.")
            Spacer(modifier = Modifier.height(16.dp))
            Paragraph("We reserve the right to suspend or block users who misuse the platform or provide fake details.")
            Paragraph("Quick PartTimes is not responsible for job contracts or payments – we simply connect people.")
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "•", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, fontSize = 16.sp)
    }
}

@Composable
fun Paragraph(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
