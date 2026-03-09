package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.WorkerColors

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        CommonHeader(
            title = "About Us",
            navController = navController,
            backgroundColor = WorkerColors.CardBackground
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Welcome to DutyPe",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            )
            
            Spacer(modifier = Modifier.height(5.dp))
            
            Text(
                text = buildAnnotatedString {
                    append("Your gateway to local job opportunities! DutyPe connects you with employers looking for reliable workers like you. Whether you're seeking part-time work, gig jobs, or full-time employment, we've got you covered.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Our Mission\n")
                    }
                    append("To empower workers by providing easy access to local job opportunities. We believe everyone deserves a chance to earn, grow, and succeed — without complicated applications or lengthy processes.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Our Vision\n")
                    }
                    append("To become India's most trusted platform for local employment, where every worker can find meaningful work that fits their skills, schedule, and location preferences.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Key Features\n")
                    }
                    append("Quick one-tap job applications • Jobs near your location • Real-time job notifications • Save jobs for later • Track your applications • Build your work profile\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Job Categories\n")
                    }
                    append("Delivery & Logistics • Food Service & Cooking • Housekeeping & Cleaning • Shop & Retail Help • Childcare & Eldercare • Maintenance & Repairs\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Why Choose DutyPe?\n")
                    }
                    append("No resume required • Verified employers • Transparent pay information • Flexible work options • Safe & secure platform")
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF4B5563),
                    lineHeight = 26.sp
                )
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Footer
            Text(
                text = "Made With Love in Bharat 💙",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(5.dp))
            
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
