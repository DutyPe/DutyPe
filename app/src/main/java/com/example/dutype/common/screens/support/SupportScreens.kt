package com.example.dutype.common.screens.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dutype.app.R
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contact_us)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.contact_us_need_help),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.contact_us_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@dutypeapp.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_support_request))
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(R.string.email_support), fontWeight = FontWeight.SemiBold)
                        Text("support@dutypeapp.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            ElevatedCard(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(stringResource(R.string.whatsapp_support), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.whatsapp_support_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onStatusBarColorChange(androidx.compose.ui.graphics.Color.White)
    }
    var expandedGuideIndex by remember { mutableStateOf(-1) }
    var expandedFaqIndex by remember { mutableStateOf(-1) }
    var searchQuery by remember { mutableStateOf("") }

    val accent = androidx.compose.ui.graphics.Color(0xFF2563EB)
    val accentSoft = androidx.compose.ui.graphics.Color(0xFFDBEAFE)
    val ink = androidx.compose.ui.graphics.Color(0xFF0F172A)
    val muted = androidx.compose.ui.graphics.Color(0xFF64748B)

    val guideItems = listOf(
        HelpExpandableItem(
            icon = Icons.Default.RocketLaunch,
            title = stringResource(R.string.getting_started),
            content = stringResource(R.string.guide_getting_started_content)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Search,
            title = stringResource(R.string.finding_jobs_faster),
            content = stringResource(R.string.guide_finding_jobs_content)
        ),
        HelpExpandableItem(
            icon = Icons.Default.WorkOutline,
            title = stringResource(R.string.applying_work_start),
            content = stringResource(R.string.guide_applying_work_content)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Star,
            title = stringResource(R.string.building_reputation),
            content = stringResource(R.string.guide_building_reputation_content)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Payments,
            title = stringResource(R.string.getting_paid_safely),
            content = stringResource(R.string.guide_getting_paid_content)
        )
    )

    val faqItems = listOf(
        HelpExpandableItem(
            icon = Icons.Default.HelpOutline,
            title = stringResource(R.string.faq_why_not_seeing_jobs),
            content = stringResource(R.string.faq_not_seeing_jobs_answer)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Assignment,
            title = stringResource(R.string.faq_track_application),
            content = stringResource(R.string.faq_track_application_answer)
        ),
        HelpExpandableItem(
            icon = Icons.Default.SupportAgent,
            title = stringResource(R.string.faq_contact_support),
            content = stringResource(R.string.faq_contact_support_answer)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Verified,
            title = stringResource(R.string.faq_improve_trust),
            content = stringResource(R.string.faq_improve_trust_answer)
        ),
        HelpExpandableItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.faq_personal_data_safe),
            content = stringResource(R.string.faq_personal_data_safe_answer)
        ),
        HelpExpandableItem(
            icon = Icons.Default.MoneyOff,
            title = stringResource(R.string.faq_cant_apply),
            content = stringResource(R.string.faq_cant_apply_answer)
        )
    )

    val q = searchQuery.trim().lowercase()
    val filteredGuides = if (q.isEmpty()) guideItems else guideItems.filter {
        it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
    }
    val filteredFaqs = if (q.isEmpty()) faqItems else faqItems.filter {
        it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFF8FAFC))
    ) {
        CommonHeader(
            title = stringResource(R.string.help_faqs),
            navController = navController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = androidx.compose.ui.graphics.Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.12f), accent.copy(alpha = 0.02f))
                            )
                        )
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(accent, androidx.compose.foundation.shape.RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.help_hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ink
                        )
                        Text(
                            stringResource(R.string.help_hero_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted
                        )
                    }
                }
            }

            // Search
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.White
                )
            )

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Phone,
                    label = stringResource(R.string.whatsapp_label),
                    bg = androidx.compose.ui.graphics.Color(0xFFDCFCE7),
                    tint = androidx.compose.ui.graphics.Color(0xFF16A34A)
                ) {
                    val msg = context.getString(R.string.whatsapp_worker_message)
                    val url = "https://wa.me/919121706236?text=" + java.net.URLEncoder.encode(msg, "UTF-8")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.email_label),
                    bg = accentSoft,
                    tint = accent
                ) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dutypein@gmail.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_worker_support))
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                }
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BugReport,
                    label = stringResource(R.string.report),
                    bg = androidx.compose.ui.graphics.Color(0xFFFFE4E6),
                    tint = androidx.compose.ui.graphics.Color(0xFFE11D48)
                ) {
                    navController.navigate(com.example.dutype.navigation.Routes.REPORT)
                }
            }

            // Worker Guide
            if (filteredGuides.isNotEmpty()) {
                SectionLabel(title = stringResource(R.string.worker_guide), count = filteredGuides.size, accent = accent)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        filteredGuides.forEachIndexed { index, item ->
                            ExpandableHelpRow(
                                item = item,
                                expanded = expandedGuideIndex == index,
                                accent = accent,
                                onClick = {
                                    expandedGuideIndex = if (expandedGuideIndex == index) -1 else index
                                }
                            )
                            if (index < filteredGuides.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            // FAQs
            if (filteredFaqs.isNotEmpty()) {
                SectionLabel(title = stringResource(R.string.frequently_asked), count = filteredFaqs.size, accent = accent)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        filteredFaqs.forEachIndexed { index, item ->
                            ExpandableHelpRow(
                                item = item,
                                expanded = expandedFaqIndex == index,
                                accent = accent,
                                onClick = {
                                    expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                                }
                            )
                            if (index < filteredFaqs.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            if (filteredGuides.isEmpty() && filteredFaqs.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White
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
                            tint = muted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_matching_topics),
                            fontWeight = FontWeight.SemiBold,
                            color = ink
                        )
                        Text(
                            stringResource(R.string.no_matching_topics_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = muted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = androidx.compose.ui.graphics.Color.White,
        shadowElevation = 2.dp
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
                    .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: Int, accent: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
            color = accent.copy(alpha = 0.12f)
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpandableHelpRow(
    item: HelpExpandableItem,
    expanded: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color(0xFF0F172A),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF64748B)
            )
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color(0xFF475569),
                lineHeight = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (item.icon != null) 46.dp else 0.dp)
            )
        }
    }
}

private data class HelpExpandableItem(
    val title: String,
    val content: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    var reportText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.report_a_problem)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (submitted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                Text(stringResource(R.string.report_submitted), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(stringResource(R.string.report_submitted_thanks), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.go_back)) }
            } else {
                Text(stringResource(R.string.report_a_problem), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.describe_issue_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    label = { Text(stringResource(R.string.describe_problem)) },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
                Button(
                    onClick = { submitted = true },
                    enabled = reportText.length >= 10,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.submit_report)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tutorial)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.tutorial_getting_started_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TutorialStep("1", stringResource(R.string.tutorial_step1_title), stringResource(R.string.tutorial_step1_desc))
            TutorialStep("2", stringResource(R.string.tutorial_step2_title), stringResource(R.string.tutorial_step2_desc))
            TutorialStep("3", stringResource(R.string.tutorial_step3_title), stringResource(R.string.tutorial_step3_desc))
            TutorialStep("4", stringResource(R.string.tutorial_step4_title), stringResource(R.string.tutorial_step4_desc))
            TutorialStep("5", stringResource(R.string.tutorial_step5_title), stringResource(R.string.tutorial_step5_desc))
        }
    }
}

@Composable
private fun TutorialStep(number: String, title: String, description: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(number, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
