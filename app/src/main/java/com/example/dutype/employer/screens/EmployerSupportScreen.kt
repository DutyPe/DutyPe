package com.example.dutype.employer.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography

// Employer theme colors
private val EmployerPrimaryBlue = Color(0xFF1E3A8A)
private val EmployerSecondaryBlue = Color(0xFF3B82F6)
private val EmployerLightBlue = Color(0xFFE0F2FE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerSupportScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    onStatusBarColorChange(Color.White)
    
    // Clean white background for professional look
    val backgroundColor = Color.White
    
    // FAQ items
    var expandedFaqIndex by remember { mutableStateOf(-1) }
    var expandedGuideIndex by remember { mutableStateOf(-1) }
    
    val userGuideItems = listOf(
        GuideItem(
            title = "Getting Started",
            content = "1. Complete your employer profile with company details\n2. Add your company logo and description\n3. Verify your phone number for security\n4. You're ready to post jobs!"
        ),
        GuideItem(
            title = "Posting a Job",
            content = "1. Tap the '+' button on Home screen\n2. Fill in job title, category, and description\n3. Set salary range and work location\n4. Add job requirements and benefits\n5. Review and post your job"
        ),
        GuideItem(
            title = "Managing Applications",
            content = "1. Go to 'My Jobs' to see all your postings\n2. Tap on a job to view applications\n3. Review applicant profiles and experience\n4. Shortlist or reject candidates\n5. Contact selected candidates directly"
        ),
        GuideItem(
            title = "Verifying Work Completion",
            content = "1. Generate a QR code for the job\n2. Worker scans QR to start work\n3. After work completion, verify the work\n4. Worker scans QR again to mark complete\n5. Rate the worker's performance"
        ),
        GuideItem(
            title = "Building Trust Score",
            content = "• Complete your profile 100%\n• Post detailed job descriptions\n• Respond to applications promptly\n• Verify work completion properly\n• Maintain good ratings from workers"
        ),
        GuideItem(
            title = "Best Practices",
            content = "• Write clear job descriptions\n• Set realistic salary expectations\n• Respond to applicants within 24 hours\n• Provide accurate work location\n• Give fair ratings to workers\n• Keep your profile updated"
        )
    )
    
    val faqItems = listOf(
        FaqItem(
            question = "How do I post a job?",
            answer = "Go to the Home screen and tap the '+' button or navigate to 'Post Job' from the bottom navigation. Fill in the job details including title, description, salary, and location, then tap 'Post Job'."
        ),
        FaqItem(
            question = "How do I view applications?",
            answer = "From your dashboard, tap on any posted job to see its applications. You can also go to 'My Jobs' and select a job to view all applicants."
        ),
        FaqItem(
            question = "How do I contact an applicant?",
            answer = "Open the applicant's profile from the applications list. You can view their details and contact information to reach out directly."
        ),
        FaqItem(
            question = "How do I edit or delete a job posting?",
            answer = "Go to 'My Jobs', find the job you want to modify, and tap on it. You'll see options to edit the job details or pause/delete the posting."
        ),
        FaqItem(
            question = "What payment methods are accepted?",
            answer = "We currently support UPI, credit/debit cards, and net banking for any premium features or promoted job listings."
        ),
        FaqItem(
            question = "How do I switch to worker mode?",
            answer = "Go to your Profile screen and toggle the 'Switch to Worker' option. This allows you to browse and apply for jobs as a worker."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Common Header component
        CommonHeader(
            title = "Help & FAQs",
            navController = navController
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Support Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = EmployerSecondaryBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "How can we help you?",
                        style = AppTypography.pageTitle.copy(
                            color = Color(0xFF1F2937)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We're here to assist you with any questions",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // User Guide Section
            Text(
                text = "User Guide",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    userGuideItems.forEachIndexed { index, guide ->
                        GuideItemCard(
                            guide = guide,
                            isExpanded = expandedGuideIndex == index,
                            onClick = {
                                expandedGuideIndex = if (expandedGuideIndex == index) -1 else index
                            }
                        )
                        if (index < userGuideItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = Color(0xFFE5E7EB)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            
            // FAQ Section
            Text(
                text = "Frequently Asked Questions",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    faqItems.forEachIndexed { index, faq ->
                        FaqItemCard(
                            faq = faq,
                            isExpanded = expandedFaqIndex == index,
                            onClick = {
                                expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                            }
                        )
                        if (index < faqItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = Color(0xFFE5E7EB)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Links
            Text(
                text = "Quick Links",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    QuickLinkItem(
                        icon = Icons.Default.BugReport,
                        title = "Report a Problem",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypein@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Bug Report - Employer App")
                            }
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    QuickLinkItem(
                        icon = Icons.Default.Feedback,
                        title = "Send Feedback",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypein@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Feedback - Employer App")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legal & Policies
            Text(
                text = "Legal & Policies",
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    QuickLinkItem(
                        icon = Icons.Default.ContactSupport,
                        title = "Contact Us",
                        onClick = { navController.navigate(com.example.dutype.navigation.Routes.CONTACT_US) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Version
            Text(
                text = "App Version 1.0.5",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF9CA3AF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ContactOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = AppTypography.labelLarge.copy(
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = subtitle,
                style = AppTypography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun GuideItemCard(
    guide: GuideItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
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
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = EmployerSecondaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF6B7280)
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = EmployerLightBlue.copy(alpha = 0.3f))
            ) {
                Text(
                    text = guide.content,
                    style = AppTypography.bodySmall.copy(
                        color = Color(0xFF374151),
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun FaqItemCard(
    faq: FaqItem,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = faq.question,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF6B7280)
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = faq.answer,
                style = AppTypography.bodySmall.copy(
                    color = Color(0xFF6B7280)
                )
            )
        }
    }
}

@Composable
private fun QuickLinkItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = AppTypography.listItemTitle.copy(
                color = Color(0xFF1F2937)
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF)
        )
    }
}

private data class GuideItem(
    val title: String,
    val content: String
)

private data class FaqItem(
    val question: String,
    val answer: String
)
