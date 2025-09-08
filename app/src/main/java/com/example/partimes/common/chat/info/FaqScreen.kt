package com.example.partimes.common.chat.info

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.utils.BackNavigationTopBar
import kotlinx.coroutines.delay

data class FaqItem(
    val question: String,
    val answer: String,
    val icon: ImageVector = Icons.AutoMirrored.Filled.Help
)

@Composable
fun FaqScreen(navController: NavController) {
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
                answer = "No! Quick PartTimes is designed to be simple. Most jobs just require basic information and you can apply with a quick voice message or simple form.",
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
                answer = "Yes! Quick PartTimes is completely free for job seekers. There are no hidden fees or subscription costs.",
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

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Frequently Asked Questions", navController = navController)
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Have Questions?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "We've got answers! Browse our most common questions below.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // FAQ Items
                faqItems.forEachIndexed { index, faqItem ->
                    ExpandableFaqCard(
                        faqItem = faqItem,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Support Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Still have questions?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Contact our support team anytime!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
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

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = faqItem.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = faqItem.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faqItem.answer,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
}
