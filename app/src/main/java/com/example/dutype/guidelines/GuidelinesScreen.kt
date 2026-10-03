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
import androidx.compose.material.icons.filled.CheckCircle
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
private val Muted = Color(0xFF64748B)
private val Green = Color(0xFF16A34A)
private val Red = Color(0xFFDC2626)
private val Line = Color(0xFFE2E8F0)

/** The rules as a column of sections (used by the screen and the accept dialog). */
@Composable
fun GuidelinesContent(role: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(titleFor(role)), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink.fg())
        Text(stringResource(R.string.guide_intro), fontSize = 13.sp, color = Muted.fg(), lineHeight = 19.sp)
        sectionsFor(role).forEach { GuideCard(it) }
        GuideCard(GuideSection(R.string.guide_sec_help, R.array.guide_help, Icons.Filled.Call))
    }
}

@Composable
private fun GuideCard(section: GuideSection) {
    val accent = if (section.warn) Red else Color(0xFF2563EB)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (section.warn) Color(0xFFFEF2F2).bg() else Color.White.bg())
            .border(1.dp, if (section.warn) Color(0xFFFECACA) else Line.bg(), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(section.icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(stringResource(section.title), fontWeight = FontWeight.Bold, color = if (section.warn) Red else Ink.fg(), fontSize = 15.sp)
        }
        stringArrayResource(section.items).forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (section.warn) Icons.Filled.Block else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (section.warn) Red else Green,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(line, fontSize = 14.sp, color = Ink.fg(), lineHeight = 20.sp)
            }
        }
    }
}

@Composable
fun GuidelinesScreen(role: String, navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC).bg()
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item { GuidelinesContent(role) }
            item {
                Spacer(Modifier.size(12.dp))
                TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) } }) {
                    Icon(Icons.Filled.Call, contentDescription = null, tint = Red)
                    Spacer(Modifier.width(6.dp))
                    Text("112", color = Red, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Row for profile / home menus that opens the rules. */
@Composable
fun GuidelinesEntryRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF0FDF4).bg())
            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Green.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = Green)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.guide_entry), fontWeight = FontWeight.Bold, color = Ink.fg())
            Text(stringResource(R.string.guide_entry_sub), fontSize = 12.sp, color = Muted.fg())
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Green)
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
