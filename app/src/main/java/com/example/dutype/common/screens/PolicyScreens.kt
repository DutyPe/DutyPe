package com.example.dutype.common.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

@Composable
fun TermsOfServiceScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = "Terms of Service",
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "DutyPe Terms of Service",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to DutyPe! By using our platform, you agree to these Terms of Service. Please read them carefully.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("1. User Accounts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("You must provide accurate information when creating an account. You are responsible for safeguarding your account details.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("2. Prohibited Conduct", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("You agree not to engage in fraudulent activity, harassment, or violation of local laws while using DutyPe.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("3. Liability", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("DutyPe connects employers and workers but is not responsible for disputes arising between parties.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = "Privacy Policy",
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "DutyPe Privacy Policy",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your privacy is important to us. This policy explains how we collect and use your data.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("1. Data Collection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("We collect information you provide directly, such as your profile details, location, and uploaded documents.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("2. Use of Data", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Your data is used to match workers with jobs, process verifications, and improve our services.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("3. Data Sharing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
            Text("We do not sell your personal data. We may share necessary details between employers and workers to facilitate job matching.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
