package com.example.dutype.common.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes

private val TosInk = Color(0xFF0F172A)
private val TosBody = Color(0xFF475569)
private val TosMuted = Color(0xFF64748B)
private val TosBorder = Color(0xFFE2E8F0)
private val TosGreen = Color(0xFF16A34A)
private val TosCheck = Color(0xFF10B981)
private val TosLink = Color(0xFF2563EB)

private val TosSectionTitleRes = listOf(
    R.string.tos_section_1,
    R.string.tos_section_2,
    R.string.tos_section_3,
    R.string.tos_section_4,
    R.string.tos_section_5
)

/**
 * Terms of Service screen: highlights card + accordion of the real terms text.
 */
@Composable
internal fun TermsAccordionScreen(navController: NavController) {
    var expandedIndex by rememberSaveable { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        TosHeader(onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 32.dp)
        ) {
            TosHighlightsCard()
            Spacer(modifier = Modifier.height(16.dp))
            TosSectionTitleRes.forEachIndexed { index, titleRes ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                TosSectionCard(
                    title = stringResource(titleRes),
                    expanded = expandedIndex == index,
                    onToggle = { expandedIndex = if (expandedIndex == index) -1 else index }
                ) {
                    TosSectionBody(index = index, navController = navController)
                }
            }
        }
    }
}

@Composable
private fun TosHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = TosInk,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.terms_of_service),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TosInk,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun TosHighlightsCard() {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0FDF4), shape)
            .border(1.dp, Color(0xFFA7F3D0), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.tos_summary_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TosGreen
        )
        TosHighlightRow(stringResource(R.string.tos_summary_1))
        TosHighlightRow(stringResource(R.string.tos_summary_2))
        TosHighlightRow(stringResource(R.string.tos_summary_3))
    }
}

@Composable
private fun TosHighlightRow(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TosCheck,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TosInk
        )
    }
}

@Composable
private fun TosSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White, shape)
            .border(1.dp, TosBorder, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable { onToggle() }
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F0F0F),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TosMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TosBodyText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = TosBody
    )
}

@Composable
private fun TosSubHeading(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = TosInk
    )
}

@Composable
private fun TosTitledPara(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TosSubHeading(title)
        TosBodyText(content)
    }
}

@Composable
private fun TosSectionBody(index: Int, navController: NavController) {
    when (index) {
        0 -> TosEligibilityBody()
        1 -> TosWorkerBody()
        2 -> TosEmployerBody()
        3 -> TosPaymentsBody()
        else -> TosPrivacyBody(navController)
    }
}

@Composable
private fun TosEligibilityBody() {
    TosTitledPara(
        stringResource(R.string.policy_terms_s1_title),
        stringResource(R.string.policy_terms_s1_content)
    )
    TosTitledPara(
        stringResource(R.string.policy_terms_s2_title),
        stringResource(R.string.policy_terms_s2_content)
    )
    TosTitledPara(
        stringResource(R.string.policy_terms_s7_title),
        stringResource(R.string.policy_terms_s7_content)
    )
}

@Composable
private fun TosWorkerBody() {
    TosTitledPara(
        stringResource(R.string.policy_terms_s5_title),
        stringResource(R.string.policy_terms_s5_content)
    )
}

@Composable
private fun TosEmployerBody() {
    TosTitledPara(
        stringResource(R.string.policy_terms_s4_title),
        stringResource(R.string.policy_terms_s4_content)
    )
}

@Composable
private fun TosPaymentsBody() {
    TosTitledPara(
        stringResource(R.string.policy_terms_s3_title),
        stringResource(R.string.policy_terms_s3_content)
    )
    TosTitledPara(
        stringResource(R.string.policy_terms_s6_title),
        stringResource(R.string.policy_terms_s6_content)
    )
}

@Composable
private fun TosPrivacyBody(navController: NavController) {
    TosBodyText(stringResource(R.string.policy_grievance_contact))
    TosEmailRow("dutypein@gmail.com", "DutyPe Support & Policy Query")
    TosEmailRow("dutypefeedback@gmail.com", "DutyPe Feedback")
    Text(
        text = stringResource(R.string.privacy_policy),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TosLink,
        modifier = Modifier.clickable { navController.navigate(Routes.PRIVACY_POLICY) }
    )
    TosBodyText(stringResource(R.string.policy_last_updated))
    TosBodyText(stringResource(R.string.policy_compliance))
}

@Composable
private fun TosEmailRow(email: String, subject: String) {
    val context = LocalContext.current
    Text(
        text = email,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TosLink,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            try { context.startActivity(intent) } catch (_: Exception) {}
        }
    )
}
