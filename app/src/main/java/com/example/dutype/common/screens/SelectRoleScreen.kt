package com.example.dutype.common.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import android.content.Intent
import com.example.dutype.MainActivity
import com.dutype.app.R
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch
import timber.log.Timber


import com.example.dutype.utils.LocaleHelper

// ── Palette ───────────────────────────────────────────────────────────────
private val ScreenBg = Color(0xFFFFFFFF)
private val InkColor = Color(0xFF0F0F0F)
private val SubtitleGray = Color(0xFF64748B)
private val BorderNeutral = Color(0xFFE2E8F0)
private val SelectedTint = Color(0xFFF8FAFC)
private val RadioRing = Color(0xFFCBD5E1)
private val WorkerIconBg = Color(0xFFECFDF5)
private val WorkerIconTint = Color(0xFF10B981)
private val EmployerIconBg = Color(0xFFEFF6FF)
private val EmployerIconTint = Color(0xFF2563EB)

@Composable
fun SelectRoleScreen(
    navController: NavHostController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    run { // follows the app theme (dark mode too)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel =
            androidx.hilt.navigation.compose.hiltViewModel()

        var selectedRole by remember { mutableStateOf("WORKER") }

        fun applyRole(role: String) {
            Timber.d("🔍 Role selected: $role")
            val targetRole = if (role == "EMPLOYER")
                com.example.dutype.models.UserRole.EMPLOYER
            else
                com.example.dutype.models.UserRole.WORKER
            scope.launch {
                profileCompletionViewModel.saveUserInfoToLocalStorage("", "", targetRole)
            }
            if (onRoleSelected != null) {
                onRoleSelected.invoke(role)
            } else {
                navController.navigate("${Routes.ENHANCED_LOGIN}?role=$role")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBg.bg())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Spacer(modifier = Modifier.weight(1.35f))

            HeadlineBlock()

            Spacer(modifier = Modifier.height(32.dp))

            RoleSelectCard(
                icon = Icons.Default.Engineering,
                title = stringResource(R.string.select_role_looking_for_work),
                subtitle = stringResource(R.string.select_role_worker_desc),
                iconBg = WorkerIconBg.bg(),
                iconTint = WorkerIconTint.fg(),
                selected = selectedRole == "WORKER",
                onClick = { selectedRole = "WORKER" }
            )

            Spacer(modifier = Modifier.height(14.dp))

            RoleSelectCard(
                icon = Icons.Default.Business,
                title = stringResource(R.string.select_role_want_to_hire),
                subtitle = stringResource(R.string.select_role_employer_desc),
                iconBg = EmployerIconBg.bg(),
                iconTint = EmployerIconTint.fg(),
                selected = selectedRole == "EMPLOYER",
                onClick = { selectedRole = "EMPLOYER" }
            )

            Spacer(modifier = Modifier.weight(0.85f))

            ContinueButton(onClick = { applyRole(selectedRole) })

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeadlineBlock() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.select_role_headline),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = InkColor.fg()
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.select_role_subheadline),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                color = SubtitleGray.fg()
            )
        )
    }
}

@Composable
private fun ContinueButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.continue_text),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RoleSelectCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor by animateColorAsState(
        // Selected: strong outline (white in dark mode); others: a quiet line.
        targetValue = if (selected) InkColor.fg() else BorderNeutral.bd(),
        animationSpec = tween(durationMillis = 200),
        label = "roleCardBorder"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clip(shape)
            .background(if (selected) SelectedTint.bg() else Color.White.bg())
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .selectable(
                selected = selected,
                onClick = onClick
            )
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor.fg()
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = SubtitleGray.fg(),
                        lineHeight = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            SelectionIndicator(selected = selected)
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    if (selected) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(InkColor.bg()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, RadioRing.bd(), CircleShape)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectRoleScreen() {
    MaterialTheme {
        SelectRoleScreen(navController = rememberNavController())
    }
}
