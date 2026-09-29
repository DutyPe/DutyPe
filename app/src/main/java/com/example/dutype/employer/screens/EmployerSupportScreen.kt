package com.example.dutype.employer.screens

import com.dutype.app.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Employer Support design palette
private val SupportBg = Color(0xFFF8FAFC)
private val SupportBorder = Color(0xFFE2E8F0)
private val SupportDivider = Color(0xFFF1F5F9)
private val SupportBlue = Color(0xFF2563EB)
private val SupportHeroBg = Color(0xFFEFF6FF)
private val SupportHeroBorder = Color(0xFFBFDBFE)
private val SupportInk = Color(0xFF0F172A)
private val SupportTitleInk = Color(0xFF0F0F0F)
private val SupportMuted = Color(0xFF64748B)
private val SupportHint = Color(0xFF94A3B8)
private val SupportWhatsApp = Color(0xFF25D366)

private const val SUPPORT_PHONE = "+918500717800"
private const val SUPPORT_EMAIL = "support@dutype.in"

private data class SupportItem(
    val title: String,
    val content: String
)

private val SupportFaqItems = listOf(
    SupportItem(
        "Why can't I see applicants?",
        "Make sure your job is Open and visible. Applicants only appear on active jobs, so check the job status first. Also complete your company profile, and give it a little time for workers nearby to find and apply."
    ),
    SupportItem(
        "How do credits work?",
        "Credits are consumed based on your plan, for example when you post a job or unlock a worker's contact details. You can see your remaining balance and upgrade anytime from the Subscription screen."
    ),
    SupportItem(
        "How to mark job as filled?",
        "Open the job from your dashboard or job history and tap Mark as Filled. The job will stop receiving new applications."
    )
)

@Composable
fun EmployerSupportScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onStatusBarColorChange(SupportBg)
    }

    var searchQuery by remember { mutableStateOf("") }
    var expandedGuide by remember { mutableStateOf("") }
    var expandedFaq by remember { mutableStateOf("") }

    val guideItems = listOf(
        SupportItem(
            "How to Post Your First Job",
            stringResource(R.string.employer_guide_posting_job_content)
        )
    )
    val q = searchQuery.trim().lowercase()
    val filteredGuides = filterSupportItems(guideItems, q)
    val filteredFaqs = filterSupportItems(SupportFaqItems, q)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SupportBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp)
        ) {
            SupportBackRow(onBack = { navController.popBackStack() })
            SupportSearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            Spacer(modifier = Modifier.height(16.dp))
            SupportHeroCard(
                onWhatsApp = { openSupportWhatsApp(context) },
                onCall = { openSupportDialer(context) }
            )
            if (filteredGuides.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                SupportAccordionCard(
                    items = filteredGuides,
                    expandedTitle = expandedGuide,
                    onToggle = { expandedGuide = if (expandedGuide == it) "" else it }
                )
            }
            if (filteredFaqs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SupportAccordionCard(
                    items = filteredFaqs,
                    expandedTitle = expandedFaq,
                    onToggle = { expandedFaq = if (expandedFaq == it) "" else it }
                )
            }
            if (filteredGuides.isEmpty() && filteredFaqs.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.no_matching_topics),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SupportInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            SupportFooter(onEmail = { openSupportEmail(context) })
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun filterSupportItems(items: List<SupportItem>, query: String): List<SupportItem> {
    if (query.isEmpty()) return items
    return items.filter {
        it.title.lowercase().contains(query) || it.content.lowercase().contains(query)
    }
}

private fun openSupportWhatsApp(context: Context) {
    val msg = context.getString(R.string.whatsapp_employer_message)
    val url = "https://wa.me/918500717800?text=" + java.net.URLEncoder.encode(msg, "UTF-8")
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun openSupportDialer(context: Context) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_PHONE")))
}

private fun openSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
    intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject_employer_support))
    context.startActivity(Intent.createChooser(intent, "Send Email"))
}

@Composable
private fun SupportBackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SupportInk,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SupportSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White, shape)
            .border(1.dp, SupportBorder, shape)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = SupportHint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = SupportInk),
            cursorBrush = SolidColor(SupportBlue),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search help articles…",
                            fontSize = 14.sp,
                            color = SupportHint
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun SupportHeroCard(
    onWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SupportHeroBg, shape)
            .border(1.dp, SupportHeroBorder, shape)
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(SupportBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Talk to your DutyPe Account Manager",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SupportInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Avg response: 3 minutes",
            fontSize = 13.sp,
            color = SupportMuted
        )
        Spacer(modifier = Modifier.height(20.dp))
        SupportHeroButtons(onWhatsApp = onWhatsApp, onCall = onCall)
    }
}

@Composable
private fun SupportHeroButtons(
    onWhatsApp: () -> Unit,
    onCall: () -> Unit
) {
    val noElevation = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onWhatsApp,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SupportWhatsApp,
                contentColor = Color.White
            ),
            elevation = noElevation,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            SupportButtonContent(
                icon = Icons.Default.ChatBubble,
                label = "WhatsApp Support",
                color = Color.White,
                weight = FontWeight.Bold
            )
        }
        OutlinedButton(
            onClick = onCall,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, SupportInk),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = SupportInk
            ),
            elevation = noElevation,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            SupportButtonContent(
                icon = Icons.Default.Phone,
                label = "Call Helpline",
                color = SupportInk,
                weight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SupportButtonContent(
    icon: ImageVector,
    label: String,
    color: Color,
    weight: FontWeight
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = weight,
        color = color,
        maxLines = 1
    )
}

@Composable
private fun SupportAccordionCard(
    items: List<SupportItem>,
    expandedTitle: String,
    onToggle: (String) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, SupportBorder, shape)
            .clip(shape)
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SupportDivider)
                )
            }
            SupportAccordionRow(
                item = item,
                expanded = expandedTitle == item.title,
                onClick = { onToggle(item.title) }
            )
        }
    }
}

@Composable
private fun SupportAccordionRow(
    item: SupportItem,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = SupportTitleInk,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SupportMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            SupportAnswerBox(text = item.content)
        }
    }
}

@Composable
private fun SupportAnswerBox(text: String) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(SupportBg, shape)
            .padding(14.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = SupportMuted
        )
    }
}

@Composable
private fun SupportFooter(onEmail: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEmail() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            tint = SupportMuted,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = SUPPORT_EMAIL,
            fontSize = 13.sp,
            color = SupportMuted
        )
    }
}
