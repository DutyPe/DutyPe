package com.example.dutype.common.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch
import timber.log.Timber


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
    com.example.dutype.ui.theme.ForceLightTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel =
            androidx.hilt.navigation.compose.hiltViewModel()

        var selectedRole by remember { mutableStateOf("WORKER") }
        var activeLanguage by remember {
            mutableStateOf(com.example.dutype.utils.LocaleHelper.getLanguage(context))
        }

        // Mirrors LanguageSelectionBottomSheet's switch: persist + apply the
        // locale, then restart MainActivity so every screen re-composes in
        // the new language (Compose won't re-read string resources otherwise).
        fun switchLanguage(code: String) {
            if (activeLanguage == code) return
            activeLanguage = code
            com.example.dutype.utils.LocaleHelper.saveLanguage(context, code)
            com.example.dutype.utils.LocaleHelper.setLocale(context.applicationContext, code)
            com.example.dutype.utils.LocaleHelper.setLocale(context, code)

            val activity = context as? Activity
            if (activity != null) {
                val intent = Intent(activity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                activity.startActivity(intent)
                activity.finish()
            }
        }

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
                .background(ScreenBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top row: wordmark + compact language switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DutyPe",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkColor
                    )
                )
                LanguagePill(
                    label = "EN",
                    selected = activeLanguage == com.example.dutype.utils.LocaleHelper.LANGUAGE_ENGLISH,
                    onClick = { switchLanguage(com.example.dutype.utils.LocaleHelper.LANGUAGE_ENGLISH) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                LanguagePill(
                    label = "हिन्दी",
                    selected = activeLanguage == com.example.dutype.utils.LocaleHelper.LANGUAGE_HINDI,
                    onClick = { switchLanguage(com.example.dutype.utils.LocaleHelper.LANGUAGE_HINDI) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                LanguagePill(
                    label = "తెలుగు",
                    selected = activeLanguage == com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU,
                    onClick = { switchLanguage(com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU) }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            HeadlineBlock()

            Spacer(modifier = Modifier.height(32.dp))

            RoleSelectCard(
                icon = Icons.Default.Engineering,
                title = "I'm Looking for Work",
                subtitle = "Find daily jobs and instant gigs, get paid fast",
                iconBg = WorkerIconBg,
                iconTint = WorkerIconTint,
                selected = selectedRole == "WORKER",
                onClick = { selectedRole = "WORKER" }
            )

            Spacer(modifier = Modifier.height(14.dp))

            RoleSelectCard(
                icon = Icons.Default.Business,
                title = "I Want to Hire",
                subtitle = "Post jobs, find workers nearby, manage hiring",
                iconBg = EmployerIconBg,
                iconTint = EmployerIconTint,
                selected = selectedRole == "EMPLOYER",
                onClick = { selectedRole = "EMPLOYER" }
            )

            Spacer(modifier = Modifier.weight(1f))

            ContinueButton(onClick = { applyRole(selectedRole) })

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeadlineBlock() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "How will you use DutyPe?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = InkColor
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose how you want to use the app",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                color = SubtitleGray
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
            .height(56.dp),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = InkColor,
            contentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Continue",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun LanguagePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) InkColor else Color.White,
        border = if (selected) null else BorderStroke(1.dp, BorderNeutral)
    ) {
        Box(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else SubtitleGray
                )
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
        targetValue = if (selected) InkColor else BorderNeutral,
        animationSpec = tween(durationMillis = 200),
        label = "roleCardBorder"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .clip(shape)
            .background(if (selected) SelectedTint else Color.White)
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
                        color = InkColor
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = SubtitleGray,
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
                .background(InkColor),
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
                .border(1.5.dp, RadioRing, CircleShape)
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
