package com.example.dutype.employer.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.CommonHeader
@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = "About Us",
            navController = navController
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
                    color = Color(0xFF1F2937),
                    fontSize = 22.sp
                )
            )
            
            Spacer(modifier = Modifier.height(5.dp))
            
            Text(
                text = buildAnnotatedString {
                    append("Your all-in-one solution for finding the best local talent, right when you need it. We connect businesses with a pool of qualified, on-demand workers for gig-based and full-time roles.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Our Mission\n")
                    }
                    append("To empower businesses by providing a seamless, efficient, and reliable platform to connect with a flexible workforce. We aim to simplify the hiring process, so you can focus on growing your business.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Our Vision\n")
                    }
                    append("To become the leading platform for on-demand employment in India, creating a dynamic ecosystem where businesses can thrive with the right talent and workers can find meaningful opportunities.\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Key Features for Employers\n")
                    }
                    append("Post jobs in minutes • Access a large talent pool • GPS-based attendance tracking • Verified worker profiles • Flexible hiring options • Real-time application alerts • Manage multiple job postings • Track worker performance\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Hire For Any Role\n")
                    }
                    append("Delivery Personnel • Kitchen & Cooking Staff • Housekeeping & Cleaning • Shop Assistants & Retail • Childcare & Eldercare • Maintenance Workers • Event & Catering Staff • And many more...\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Why Choose DutyPe?\n")
                    }
                    append("Quick hiring process • Verified worker database • Cost-effective solutions • 24/7 platform access • Dedicated support team\n\n")
                    
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF111827))) {
                        append("Our Core Values\n")
                    }
                    append("Efficiency in hiring • Reliability you can trust • Transparency in all dealings • Empowerment for businesses")
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
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(5.dp))
            
            // Version info
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmployerAboutScreenPreview() {
    EmployerAboutScreen(
        navController = rememberNavController(),
        onStatusBarColorChange = {}
    )
}
