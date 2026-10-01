package com.example.dutype.common.screens.support

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.AttachFile
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

private val CuInk = Color(0xFF0F172A)
private val CuBlack = Color(0xFF0F0F0F)
private val CuBorder = Color(0xFFE2E8F0)
private val CuMuted = Color(0xFF64748B)
private val CuHint = Color(0xFF94A3B8)
private val CuGreen = Color(0xFF16A34A)

@Composable
fun ContactUsScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf<Uri?>(null) }
    val pickLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) attachedUri = uri }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        ContactBackRow(onBack = { navController.popBackStack() })
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.contact_get_in_touch), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CuInk.fg())
        Spacer(Modifier.height(16.dp))
        ContactReplyBadge()
        Spacer(Modifier.height(16.dp))
        ContactChannelCard(
            icon = Icons.Outlined.Chat,
            circleColor = Color(0xFFF0FDF4).fg(),
            tint = CuGreen.fg(),
            title = stringResource(R.string.contact_whatsapp_title),
            subtitle = stringResource(R.string.contact_whatsapp_subtitle),
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210"))
                context.startActivity(intent)
            }
        )
        Spacer(Modifier.height(12.dp))
        ContactChannelCard(
            icon = Icons.Outlined.Phone,
            circleColor = Color(0xFFEFF6FF).fg(),
            tint = Color(0xFF2563EB).fg(),
            title = stringResource(R.string.contact_helpline_title),
            subtitle = stringResource(R.string.contact_helpline_subtitle),
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+918500717800")))
            }
        )
        Spacer(Modifier.height(12.dp))
        ContactChannelCard(
            icon = Icons.Outlined.Email,
            circleColor = Color(0xFFFEF3C7).fg(),
            tint = Color(0xFFD97706).fg(),
            title = stringResource(R.string.contact_email_title),
            subtitle = stringResource(R.string.contact_email_subtitle),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@dutypeapp.com"))
                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_support_request))
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)))
            }
        )
        Spacer(Modifier.height(16.dp))
        ContactFormCard(
            message = message,
            onMessageChange = { message = it },
            attached = attachedUri != null,
            onAttach = { pickLauncher.launch("image/*") },
            onSend = {
                val body = message.trim()
                val shot = attachedUri
                val subject = context.getString(R.string.email_subject_support_request)
                val intent = if (shot != null) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@dutypeapp.com"))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        putExtra(Intent.EXTRA_STREAM, shot)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@dutypeapp.com")).apply {
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }
                }
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.contact_send_message)))
            }
        )
    }
}

@Composable
private fun ContactBackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = CuInk.fg(),
            modifier = Modifier.size(24.dp).clickable { onBack() }
        )
    }
}

@Composable
private fun ContactReplyBadge() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0FDF4).bg())
            .border(1.dp, Color(0xFFA7F3D0).bd(), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.contact_reply_time),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CuGreen.fg()
        )
    }
}

@Composable
private fun ContactChannelCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    circleColor: Color,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg())
            .border(1.dp, CuBorder.bd(), shape)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = CuBlack.fg())
            Text(subtitle, fontSize = 13.sp, color = CuMuted.fg())
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = CuHint.fg(),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ContactMessageField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = CuBlack.fg()),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(CuInk.fg()),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(shape)
            .border(1.dp, if (focused) CuInk.bd() else CuBorder.bd(), shape)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.help_hero_title), fontSize = 14.sp, color = CuHint.fg())
                }
                inner()
            }
        }
    )
}

@Composable
private fun ContactFormCard(
    message: String,
    onMessageChange: (String) -> Unit,
    attached: Boolean,
    onAttach: () -> Unit,
    onSend: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg())
            .border(1.dp, CuBorder.bd(), shape)
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.contact_or_send_message), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CuBlack.fg())
        Spacer(Modifier.height(12.dp))
        ContactMessageField(value = message, onValueChange = onMessageChange)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.clickable { onAttach() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = CuMuted.fg(), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (attached) stringResource(R.string.contact_screenshot_attached) else stringResource(R.string.contact_attach_screenshot),
                fontSize = 13.sp,
                color = CuMuted.fg()
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSend,
            enabled = message.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CuBlack.bg(),
                contentColor = Color.White,
                disabledContainerColor = CuBlack.bg().copy(alpha = 0.4f),
                disabledContentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.contact_send_message), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Help & FAQs — same look as the Terms / Privacy screens: white page, back arrow +
 * centered title, a green summary card, then accordion cards (one open at a time).
 */
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }
    // Keys like "guide_0" / "faq_2"; the first guide starts open like the Terms screen.
    var expandedKey by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("guide_0") }
    var searchQuery by remember { mutableStateOf("") }

    val guideItems = listOf(
        HelpExpandableItem(stringResource(R.string.getting_started), stringResource(R.string.guide_getting_started_content), Icons.Default.RocketLaunch),
        HelpExpandableItem(stringResource(R.string.finding_jobs_faster), stringResource(R.string.guide_finding_jobs_content), Icons.Default.Search),
        HelpExpandableItem(stringResource(R.string.applying_work_start), stringResource(R.string.guide_applying_work_content), Icons.Default.WorkOutline),
        HelpExpandableItem(stringResource(R.string.building_reputation), stringResource(R.string.guide_building_reputation_content), Icons.Default.Star),
        HelpExpandableItem(stringResource(R.string.getting_paid_safely), stringResource(R.string.guide_getting_paid_content), Icons.Default.Payments)
    )
    val faqItems = listOf(
        HelpExpandableItem(stringResource(R.string.faq_why_not_seeing_jobs), stringResource(R.string.faq_not_seeing_jobs_answer), Icons.Default.HelpOutline),
        HelpExpandableItem(stringResource(R.string.faq_track_application), stringResource(R.string.faq_track_application_answer), Icons.Default.Assignment),
        HelpExpandableItem(stringResource(R.string.faq_contact_support), stringResource(R.string.faq_contact_support_answer), Icons.Default.SupportAgent),
        HelpExpandableItem(stringResource(R.string.faq_improve_trust), stringResource(R.string.faq_improve_trust_answer), Icons.Default.Verified),
        HelpExpandableItem(stringResource(R.string.faq_personal_data_safe), stringResource(R.string.faq_personal_data_safe_answer), Icons.Default.Security),
        HelpExpandableItem(stringResource(R.string.faq_cant_apply), stringResource(R.string.faq_cant_apply_answer), Icons.Default.MoneyOff)
    )

    val q = searchQuery.trim().lowercase()
    val matches: (HelpExpandableItem) -> Boolean = {
        q.isEmpty() || it.title.lowercase().contains(q) || it.content.lowercase().contains(q)
    }
    val filteredGuides = guideItems.filter(matches)
    val filteredFaqs = faqItems.filter(matches)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .statusBarsPadding()
    ) {
        HelpHeader(title = stringResource(R.string.help_faqs), onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp)
        ) {
            HelpSummaryCard(
                onWhatsApp = {
                    val msg = context.getString(R.string.whatsapp_worker_message)
                    val url = "https://wa.me/918500717800?text=" + java.net.URLEncoder.encode(msg, "UTF-8")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                onEmail = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:dutypein@gmail.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_worker_support))
                    runCatching { context.startActivity(Intent.createChooser(intent, "Send Email")) }
                },
                onReport = { navController.navigate(com.example.dutype.navigation.Routes.REPORT) }
            )
            Spacer(Modifier.height(16.dp))
            HelpSearchField(value = searchQuery, onValueChange = { searchQuery = it })

            if (filteredGuides.isNotEmpty()) {
                HelpSectionLabel(stringResource(R.string.worker_guide))
                filteredGuides.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    val key = "guide_${guideItems.indexOf(item)}"
                    // While searching, every match is shown open.
                    HelpAccordionCard(item, expanded = q.isNotEmpty() || expandedKey == key) {
                        expandedKey = if (expandedKey == key) "" else key
                    }
                }
            }

            if (filteredFaqs.isNotEmpty()) {
                HelpSectionLabel(stringResource(R.string.frequently_asked))
                filteredFaqs.forEachIndexed { index, item ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    val key = "faq_${faqItems.indexOf(item)}"
                    HelpAccordionCard(item, expanded = q.isNotEmpty() || expandedKey == key) {
                        expandedKey = if (expandedKey == key) "" else key
                    }
                }
            }

            if (filteredGuides.isEmpty() && filteredFaqs.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.no_matching_topics), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = CuInk.fg())
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.no_matching_topics_hint), fontSize = 13.sp, color = CuMuted.fg())
            }
        }
    }
}

@Composable
private fun HelpHeader(title: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = CuInk.fg(), modifier = Modifier.size(24.dp))
        }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CuInk.fg(), modifier = Modifier.align(Alignment.Center))
    }
}

/** Green summary card (matches the Terms "Plain Language Summary") with the 3 quick contacts. */
@Composable
private fun HelpSummaryCard(onWhatsApp: () -> Unit, onEmail: () -> Unit, onReport: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0FDF4).bg(), shape)
            .border(1.dp, Color(0xFFA7F3D0).bd(), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.help_need_fast), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CuGreen.fg())
        Text(
            stringResource(R.string.help_need_fast_desc),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = CuInk.fg()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HelpContactPill(stringResource(R.string.whatsapp_label), Icons.Default.Phone, Modifier.weight(1f), onWhatsApp)
            HelpContactPill(stringResource(R.string.email_label), Icons.Default.Email, Modifier.weight(1f), onEmail)
            HelpContactPill(stringResource(R.string.report), Icons.Default.BugReport, Modifier.weight(1f), onReport)
        }
    }
}

@Composable
private fun HelpContactPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .background(Color.White.bg(), shape)
            .border(1.dp, Color(0xFFA7F3D0).bd(), shape)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = CuGreen.fg(), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = CuInk.fg(), maxLines = 1)
    }
}

@Composable
private fun HelpSearchField(value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = CuBlack.fg()),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(CuInk.fg()),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .border(1.dp, CuBorder.bd(), shape),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = CuMuted.fg(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(stringResource(R.string.search_help_topics), fontSize = 14.sp, color = CuHint.fg())
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear),
                        tint = CuMuted.fg(),
                        modifier = Modifier.size(18.dp).clickable { onValueChange("") }
                    )
                }
            }
        }
    )
}

@Composable
private fun HelpSectionLabel(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = CuMuted.fg(),
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
    )
}

/** Accordion card: same shape, height and chevrons as the Terms screen sections. */
@Composable
private fun HelpAccordionCard(item: HelpExpandableItem, expanded: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.bg(), shape)
            .border(1.dp, CuBorder.bd(), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.icon != null) {
                Icon(item.icon, contentDescription = null, tint = CuInk.fg(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CuBlack.fg(),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CuMuted.fg(),
                modifier = Modifier.size(18.dp)
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Text(
                text = item.content,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color(0xFF475569).fg(),
                modifier = Modifier.padding(
                    start = if (item.icon != null) 46.dp else 16.dp,
                    end = 16.dp,
                    bottom = 18.dp
                )
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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


