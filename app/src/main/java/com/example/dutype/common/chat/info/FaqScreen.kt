package com.example.dutype.common.chat.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import kotlinx.coroutines.delay

data class FaqItem(
    val question: String,
    val answer: String,
    val icon: ImageVector = Icons.AutoMirrored.Filled.Help
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White) // White theme color to match worker screens
    var isVisible by remember { mutableStateOf(false) }

    val faqItems = remember {
        listOf(
            FaqItem(
                question = "How do I find jobs near me?",
                answer = "Simply enable location services and browse jobs in your area. You can also use voice search to find specific job types like 'delivery jobs near me' or 'part-time work in retail'.",
                icon = Icons.Default.LocationOn
            ),
            FaqItem(
                question = "Do I need a resume to apply?",
                answer = "No! DutyPe is designed to be simple. Most jobs just require basic information and you can apply with a quick voice message or simple form.",
                icon = Icons.Default.Description
            ),
            FaqItem(
                question = "How does voice application work?",
                answer = "Tap the voice button and tell us about yourself and why you're interested in the job. Our AI will help format your application professionally.",
                icon = Icons.Default.Mic
            ),
            FaqItem(
                question = "When will I hear back from employers?",
                answer = "Most employers respond within 24-48 hours. You'll get notifications directly in the app when there's an update on your applications.",
                icon = Icons.Default.Schedule
            ),
            FaqItem(
                question = "Is the app free to use?",
                answer = "Yes! DutyPe is completely free for workers. There are no hidden fees or subscription costs.",
                icon = Icons.Default.MonetizationOn
            ),
            FaqItem(
                question = "What types of jobs are available?",
                answer = "We focus on local, flexible work including delivery, retail, food service, cleaning, tutoring, event staff, and many other part-time opportunities.",
                icon = Icons.Default.Work
            ),
            FaqItem(
                question = "How do I get paid?",
                answer = "Payment terms vary by employer. Most jobs offer daily or weekly payments through bank transfer, UPI, or cash. Payment details are clearly mentioned in each job posting.",
                icon = Icons.Default.Payment
            ),
            FaqItem(
                question = "Can I work multiple jobs?",
                answer = "Absolutely! Our platform is designed for flexibility. You can apply to multiple jobs and work according to your schedule and availability.",
                icon = Icons.Default.Schedule
            ),
            FaqItem(
                question = "How do I contact support?",
                answer = "You can reach our support team through the Help section in the app, call our support line, or use the in-app chat feature.",
                icon = Icons.Default.Support
            ),
            FaqItem(
                question = "Is my personal information safe?",
                answer = "Yes, we take privacy seriously. Your data is encrypted and we never share personal information with third parties without your consent.",
                icon = Icons.Default.Security
            )
        )
    }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    // Clean Header - matching the about us page style
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Common header - used across all info screens
        CommonHeader(
            title = "FAQs",
            navController = navController
        )
        
        // Content Area
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 0.dp)
                    .padding(start = 8.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // FAQ Items - simplified like About Us
                faqItems.forEachIndexed { index, faqItem ->
                    ExpandableFaqCard(
                        faqItem = faqItem,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(23.dp))

                // Contact Support Info - simple text like About Us
                Text(
                    text = "Still have questions?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Contact our support team anytime through the Help section in the app.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Black,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.padding(bottom = 40.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandableFaqCard(
    faqItem: FaqItem,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Question Row - simple like About Us
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = faqItem.icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = faqItem.question,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }

        // Answer - simple text like About Us
        if (isExpanded) {
            Text(
                text = faqItem.answer,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)
            )
        }
    }
}
