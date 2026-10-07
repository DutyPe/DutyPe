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

/** The rules as a column of sections (used by the screen and the accept dialog). */
@Composable
fun GuidelinesContent(role: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Clean Intro Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.bg())
                .border(1.dp, Line.bd(), RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Text(
                text = stringResource(titleFor(role)),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Ink.fg()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.guide_intro),
                fontSize = 13.sp,
                color = Muted.fg(),
                lineHeight = 19.sp
            )
        }

        sectionsFor(role).forEach { GuideCard(it) }
        GuideCard(GuideSection(R.string.guide_sec_help, R.array.guide_help, Icons.Filled.Call))
    }
}

@Composable
private fun GuideCard(section: GuideSection) {
    val iconTint = if (section.warn) Red else Ink.fg()
    val cardBorder = if (section.warn) Color(0xFFFCA5A5).bd() else Line.bd()
    val cardBg = Color.White.bg()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (section.warn) Color(0xFFFEF2F2).bg() else Color(0xFFF1F5F9).bg()),
                contentAlignment = Alignment.Center
            ) {
                Icon(section.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            if (section.warn) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Never Allowed",
                            fontWeight = FontWeight.Bold,
                            color = Red,
                            fontSize = 15.5.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2).bg())
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ZERO TOLERANCE",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Permanent account ban + police complaint for violations",
                        fontSize = 12.sp,
                        color = Red.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            } else {
                Text(
                    stringResource(section.title),
                    fontWeight = FontWeight.SemiBold,
                    color = Ink.fg(),
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        stringArrayResource(section.items).forEach { line ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (section.warn) Color(0xFFFEE2E2).bg() else Color(0xFFDCFCE7).bg()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (section.warn) Icons.Filled.Close else Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (section.warn) Red else Green,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = line,
                    fontSize = 13.5.sp,
                    color = if (section.warn) Ink.fg() else Color(0xFF334155).fg(),
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun GuidelinesScreen(role: String, navController: NavController) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
    ) {
        com.example.dutype.components.CommonHeader(
            title = stringResource(R.string.guide_title),
            navController = navController,
            backgroundColor = Color.White.bg()
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { GuidelinesContent(role) }
            item {
                Spacer(Modifier.height(4.dp))
                // Clean Emergency Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.bg())
                        .border(1.dp, Line.bd(), RoundedCornerShape(14.dp))
                        .clickable { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) } }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2).bg()),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, tint = Red, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency Police & Medical: 112",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink.fg()
                        )
                        Text(
                            text = "Tap to call immediately in case of distress",
                            fontSize = 12.sp,
                            color = Muted.fg()
                        )
                    }
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
