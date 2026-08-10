package com.example.dutype.employer.screens

import com.dutype.app.R
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
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.example.dutype.ui.theme.EmployerColors
import androidx.compose.ui.res.stringResource

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
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }
    
    // Clean white background for professional look
    val backgroundColor = com.example.dutype.ui.theme.EmployerColors.ScreenBackground
    
    // FAQ items
    var expandedFaqIndex by remember { mutableStateOf(-1) }
    var expandedGuideIndex by remember { mutableStateOf(-1) }
    var searchQuery by remember { mutableStateOf("") }
    
    val userGuideItems = listOf(
        GuideItem(
            title = stringResource(R.string.getting_started),
            content = stringResource(R.string.employer_guide_getting_started_content)
        ),
        GuideItem(
            title = stringResource(R.string.posting_a_job),
            content = stringResource(R.string.employer_guide_posting_job_content)
        ),
        GuideItem(
            title = stringResource(R.string.managing_applications),
            content = stringResource(R.string.employer_guide_managing_apps_content)
        ),
        GuideItem(
            title = stringResource(R.string.verifying_work_completion),
            content = stringResource(R.string.employer_guide_verifying_work_content)
        ),
        GuideItem(
            title = stringResource(R.string.building_trust_score),
            content = stringResource(R.string.employer_guide_trust_score_content)
        ),
        GuideItem(
            title = stringResource(R.string.best_practices),
            content = stringResource(R.string.employer_guide_best_practices_content)
        )
    )
    
    val faqItems = listOf(
        FaqItem(
            question = stringResource(R.string.employer_faq_post_job_question),
            answer = stringResource(R.string.employer_faq_post_job_answer)
        ),
        FaqItem(
            question = stringResource(R.string.employer_faq_view_apps_question),
            answer = stringResource(R.string.employer_faq_view_apps_answer)
        ),
        FaqItem(
            question = stringResource(R.string.employer_faq_contact_applicant_question),
            answer = stringResource(R.string.employer_faq_contact_applicant_answer)
        ),
        FaqItem(
            question = stringResource(R.string.employer_faq_edit_job_question),
            answer = stringResource(R.string.employer_faq_edit_job_answer)
        ),
        FaqItem(
            question = stringResource(R.string.employer_faq_payment_methods_question),
            answer = stringResource(R.string.employer_faq_payment_methods_answer)
        ),
        FaqItem(
            question = stringResource(R.string.employer_faq_switch_worker_question),
            answer = stringResource(R.string.employer_faq_switch_worker_answer)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Common Header component
        CommonHeader(
            title = stringResource(R.string.help_faqs),
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
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
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
                        text = stringResource(R.string.employer_help_title),
                        style = AppTypography.pageTitle.copy(
                            color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.employer_help_subtitle),
                        style = AppTypography.bodyMedium.copy(
                            color = EmployerColors.TextSecondary
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // Quick action tiles: WhatsApp / Email / Report
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmployerQuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Phone,
                    label = stringResource(R.string.whatsapp_label),
                    bg = EmployerColors.SuccessLight,
                    tint = Color(0xFF16A34A)
                ) {
                    val msg = context.getString(R.string.whatsapp_employer_message)
                    val url = "https://wa.me/919121706236?text=" + java.net.URLEncoder.encode(msg, "UTF-8")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                EmployerQuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.email_label),
                    bg = EmployerLightBlue,
                    tint = EmployerSecondaryBlue
                ) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dutypein@gmail.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_employer_support))
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                }
                EmployerQuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BugReport,
                    label = stringResource(R.string.report),
                    bg = Color(0xFFFFE4E6),
                    tint = Color(0xFFE11D48)
                ) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:dutypein@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_bug_report_employer))
                    }
                    context.startActivity(intent)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search across guide + faq
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_help_topics)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmployerSecondaryBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground,
                    unfocusedContainerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            val q = searchQuery.trim().lowercase()
            val filteredGuides = if (q.isEmpty()) userGuideItems else userGuideItems.filter {
                it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
            }
            val filteredFaqs = if (q.isEmpty()) faqItems else faqItems.filter {
                it.question.lowercase().contains(q) || it.answer.lowercase().contains(q)
            }

            // Employer Guide (Single Page Layout - No Accordion)
            if (filteredGuides.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.employer_user_guide_header),
                    style = AppTypography.sectionHeader.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredGuides.forEach { guide ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = guide.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = guide.content,
                                style = AppTypography.bodyMedium.copy(
                                    color = Color(0xFF475569),
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // FAQ Section (Single Page Layout - No Accordion)
            if (filteredFaqs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.employer_faq_header),
                    style = AppTypography.sectionHeader.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredFaqs.forEach { faq ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = faq.answer,
                                style = AppTypography.bodyMedium.copy(
                                    color = Color(0xFF475569),
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (filteredGuides.isEmpty() && filteredFaqs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = EmployerColors.TextTertiary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_matching_topics),
                            fontWeight = FontWeight.SemiBold,
                            color = EmployerColors.TextPrimary
                        )
                        Text(
                            stringResource(R.string.no_matching_topics_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmployerColors.TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Quick Links
            Text(
                text = stringResource(R.string.employer_quick_links_header),
                style = AppTypography.sectionHeader.copy(
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    QuickLinkItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.report_a_problem),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypein@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_bug_report_employer))
                            }
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(color = EmployerColors.Border)
                    QuickLinkItem(
                        icon = Icons.Default.Feedback,
                        title = stringResource(R.string.send_feedback),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypein@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_feedback_employer))
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legal & Policies
            Text(
                text = stringResource(R.string.employer_legal_header),
                style = AppTypography.sectionHeader.copy(
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    QuickLinkItem(
                        icon = Icons.AutoMirrored.Filled.ContactSupport,
                        title = stringResource(R.string.contact_us),
                        onClick = { navController.navigate(com.example.dutype.navigation.Routes.CONTACT_US) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Version
            Text(
                text = stringResource(R.string.auto_app_version_1_0_5),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EmployerColors.TextTertiary
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
private fun EmployerQuickActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    bg: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = EmployerColors.TextPrimary
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
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
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
                    .background(EmployerColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmployerColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = AppTypography.labelLarge.copy(
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = AppTypography.bodySmall.copy(
                    color = EmployerColors.TextSecondary
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
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = EmployerSecondaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = guide.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                    )
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = EmployerColors.TextSecondary
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
                        color = EmployerColors.TextSecondary,
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
                    color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = EmployerColors.TextSecondary
            )
        }
        
        if (isExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = faq.answer,
                style = AppTypography.bodySmall.copy(
                    color = EmployerColors.TextSecondary
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
            tint = EmployerColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = AppTypography.listItemTitle.copy(
                color = com.example.dutype.ui.theme.EmployerColors.TextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = EmployerColors.TextTertiary
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
