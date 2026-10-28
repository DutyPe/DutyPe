package com.example.dutype.guidelines

import android.content.Intent
import android.net.Uri
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg

/**
 * Safety & code of conduct for each side of DutyPe: service partners, workers (regular jobs and
 * urgent work) and employers / service customers. The text lives in res/values(-te, -hi)/guidelines.xml
 * (English, Telugu, Hindi). Partners accept it when they apply; everyone can open it any time.
 */
object GuidelineRole {
    const val PARTNER = "partner"
    const val WORKER = "worker"
    const val EMPLOYER = "employer"
}

private data class GuideSection(@StringRes val title: Int, @ArrayRes val items: Int, val icon: ImageVector, val warn: Boolean = false)

private fun sectionsFor(role: String): List<GuideSection> = when (role) {
    GuidelineRole.PARTNER -> listOf(
        GuideSection(R.string.guide_sec_before, R.array.guide_partner_before, Icons.Filled.Route),
        GuideSection(R.string.guide_sec_at_work, R.array.guide_partner_at, Icons.Filled.HomeRepairService),
        GuideSection(R.string.guide_sec_safety, R.array.guide_partner_safety, Icons.Filled.HealthAndSafety),
        GuideSection(R.string.guide_sec_money, R.array.guide_partner_money, Icons.Filled.Payments),
        GuideSection(R.string.guide_sec_after, R.array.guide_partner_after, Icons.Filled.TaskAlt),
        GuideSection(R.string.guide_sec_never, R.array.guide_partner_never, Icons.Filled.Block, warn = true),
    )
    GuidelineRole.EMPLOYER -> listOf(
        GuideSection(R.string.guide_sec_posting, R.array.guide_employer_posting, Icons.Filled.PostAdd),
        GuideSection(R.string.guide_sec_workers, R.array.guide_employer_workers, Icons.Filled.Groups),
        GuideSection(R.string.guide_sec_services, R.array.guide_employer_services, Icons.Filled.HomeRepairService),
        GuideSection(R.string.guide_sec_never, R.array.guide_employer_never, Icons.Filled.Block, warn = true),
    )
    else -> listOf(
        GuideSection(R.string.guide_sec_before, R.array.guide_worker_before, Icons.Filled.Route),
        GuideSection(R.string.guide_sec_at_work, R.array.guide_worker_at, Icons.Filled.Verified),
        GuideSection(R.string.guide_sec_safety, R.array.guide_worker_safety, Icons.Filled.HealthAndSafety),
        GuideSection(R.string.guide_sec_money, R.array.guide_worker_money, Icons.Filled.Payments),
        GuideSection(R.string.guide_sec_never, R.array.guide_worker_never, Icons.Filled.Block, warn = true),
    )
}

@StringRes
private fun titleFor(role: String): Int = when (role) {
    GuidelineRole.PARTNER -> R.string.guide_partner_title
    GuidelineRole.EMPLOYER -> R.string.guide_employer_title
    else -> R.string.guide_worker_title
}

private val Ink = Color(0xFF0F172A)
private val InkRow = Color(0xFF0F0F0F)
private val Muted = Color(0xFF64748B)
private val Line = Color(0xFFE2E8F0)
private val Red = Color(0xFFDC2626)
private val Green = Color(0xFF10B981)

/** The rules as a column of unboxed sections matching Settings/Airbnb styling */
@Composable
fun GuidelinesContent(role: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        sectionsFor(role).forEach { GuideSectionItem(it) }
        GuideSectionItem(GuideSection(R.string.guide_sec_help, R.array.guide_help, Icons.Filled.Call))
    }
}

@Composable
private fun GuideSectionItem(section: GuideSection) {
    val titleColor = if (section.warn) Red else Color(0xFF222222).fg()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (section.warn) "Never Allowed" else stringResource(section.title),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    letterSpacing = (-0.2).sp
                ),
                modifier = Modifier.weight(1f)
            )
            if (section.warn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF2F2).bg())
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ZERO TOLERANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red
                    )
                }
            }
        }

        if (section.warn) {
            Text(
                text = "Permanent account ban + police complaint for violations.",
                style = TextStyle(
                    fontSize = 13.5.sp,
                    color = Red.copy(alpha = 0.85f),
                    lineHeight = 18.sp
                )
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            stringArrayResource(section.items).forEach { line ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (section.warn) Color(0xFFFEE2E2).bg() else Color(0xFFDCFCE7).bg()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (section.warn) Icons.Filled.Close else Icons.Filled.Check,
                            contentDescription = null,
                            tint = if (section.warn) Red else Green,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = line,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF475569).fg(),
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 0.8.dp,
            color = Color(0xFFF0F0F0).bd()
        )
    }
}

@Composable
fun GuidelinesScreen(role: String, navController: NavController) {
    val scrollState = rememberScrollState()
    val compactTitleProgress by remember {
        derivedStateOf {
            ((scrollState.value - 30f) / 60f).coerceIn(0f, 1f)
        }
    }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .statusBarsPadding()
    ) {
        // Sticky Top Row with Back Button and Collapsible Compact Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color(0xFF222222).fg(),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.guide_title),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222).fg(),
                    letterSpacing = (-0.2).sp
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = compactTitleProgress
                    translationY = (1f - compactTitleProgress) * 12f
                }
            )
        }
        HorizontalDivider(
            thickness = 0.8.dp,
            color = Color(0xFFF0F0F0).bd().copy(alpha = compactTitleProgress)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Large Title + Subtitle
            Column {
                Text(
                    text = stringResource(R.string.guide_title),
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222).fg(),
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(titleFor(role)) + " — " + stringResource(R.string.guide_intro),
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF717171).fg(),
                        lineHeight = 22.sp
                    )
                )
            }

            HorizontalDivider(
                thickness = 0.8.dp,
                color = Color(0xFFF0F0F0).bd()
            )

            GuidelinesContent(role)

            Spacer(Modifier.height(4.dp))

            // Clean Emergency 112 Section (Airbnb styled)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFEF2F2).bg())
                    .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) } }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Emergency Police & Medical: 112",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Red
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Tap to call immediately in case of distress",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = Color(0xFF991B1B)
                        )
                    )
                }
            }
        }
    }
}
/** Row for profile / home menus that opens the rules (Pronto / UC B&W matching). */
@Composable
fun GuidelinesEntryRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(1.dp, Line.bd(), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9).bg()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = Ink.fg(), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.guide_entry), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink.fg())
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.guide_entry_sub), fontSize = 12.sp, color = Muted.fg())
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Scrollable rules in a dialog, for "read before you accept". */
@Composable
fun GuidelinesDialog(role: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.guide_ok)) } },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                GuidelinesContent(role)
            }
        }
    )
}
