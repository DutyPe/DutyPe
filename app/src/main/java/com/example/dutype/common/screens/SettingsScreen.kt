package com.example.dutype.common.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.MainActivity
import com.example.dutype.components.AccountDeletionDialog
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ThemeModeViewModel
import com.example.dutype.data.ThemeMode
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.LocalThemeMode
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.appVersionName
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val SettingsBg = Color(0xFFF8FAFC)
private val SettingsInk = Color(0xFF0F172A)
private val SettingsRowText = Color(0xFF0F0F0F)
private val SettingsMuted = Color(0xFF64748B)
private val SettingsFaint = Color(0xFF94A3B8)
private val SettingsBorder = Color(0xFFE2E8F0)
private val SettingsDivider = Color(0xFFF1F5F9)
private val SettingsDanger = Color(0xFFEF4444)
private val SettingsOn = Color(0xFF10B981)

private const val SETTINGS_PREFS = "dutype_settings_prefs"
private const val KEY_PUSH_ENABLED = "push_notifications_enabled"

private fun readSettingsFlag(context: Context, key: String): Boolean {
    return runCatching {
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).getBoolean(key, true)
    }.getOrDefault(true)
}

private fun writeSettingsFlag(context: Context, key: String, value: Boolean) {
    runCatching {
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }
}

private fun findActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun applyLanguageChange(context: Context, code: String) {
    if (LocaleHelper.getLanguage(context) == code) return
    LocaleHelper.saveLanguage(context, code)
    LocaleHelper.setLocale(context.applicationContext, code)
    LocaleHelper.setLocale(context, code)
    val activity = findActivity(context)
    if (activity != null) {
        val intent = Intent(activity, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activity.startActivity(intent)
        activity.finish()
    }
}

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val authManager = profileCompletionViewModel.authManager
    val appVersion = remember(context) { context.appVersionName() }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }

    var userRoleStr by remember { mutableStateOf("WORKER") }
    var phoneNumber by remember { mutableStateOf(currentUser?.phoneNumber.orEmpty()) }
    LaunchedEffect(Unit) {
        val role = profileCompletionViewModel.getUserRole()
        if (role == com.example.dutype.models.UserRole.EMPLOYER) {
            userRoleStr = "EMPLOYER"
        }
        if (phoneNumber.isBlank()) {
            val saved = runCatching { profileCompletionViewModel.getPhoneNumber() }.getOrNull()
            if (!saved.isNullOrBlank()) phoneNumber = saved
        }
    }

    val roleForEdit = userRoleStr
    val openEditProfile: () -> Unit = {
        if (roleForEdit == "EMPLOYER") {
            navController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
        } else {
            navController.navigate(Routes.WORKER_PROFILE_DETAILS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg.bg())
    ) {
        com.example.dutype.components.CommonHeader(
            title = stringResource(R.string.settings),
            navController = navController,
            backgroundColor = SettingsBg.bg()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp)
        ) {
            SettingsSectionLabel(stringResource(R.string.settings_account))
            AccountCard(
                phoneNumber = phoneNumber,
                onEditProfile = openEditProfile
            )

            SettingsSectionLabel(stringResource(R.string.settings_preferences))
            PreferencesCard()

            SettingsSectionLabel(stringResource(R.string.notifications))
            NotificationsCard()

            SettingsSectionLabel(stringResource(R.string.settings_legal))
            LegalCard(
                navController = navController,
                userRole = roleForEdit
            )

            if (currentUser != null) {
                Spacer(modifier = Modifier.height(20.dp))
                DangerCard(
                    logoutText = stringResource(R.string.log_out),
                    deleteText = stringResource(R.string.delete_account),
                    onLogout = { showLogoutDialog = true },
                    onDelete = { showAccountDeletionDialog = true }
                )
            }

            SettingsFooter(appVersion = appVersion)
        }
    }

    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = navController,
            userRole = userRoleStr,
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }

    if (showAccountDeletionDialog) {
        AccountDeletionDialog(
            isVisible = showAccountDeletionDialog,
            onDismiss = { showAccountDeletionDialog = false },
            navController = navController,
            userRole = userRoleStr,
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = SettingsInk.fg(),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SettingsInk.fg()
            )
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SettingsFaint.fg(),
            letterSpacing = 0.6.sp
        ),
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, end = 0.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.bg())
            .border(BorderStroke(1.dp, SettingsBorder.bd()), RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsDividerLine() {
    HorizontalDivider(color = SettingsDivider.bd(), thickness = 1.dp)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val base = Modifier
        .fillMaxWidth()
        .height(52.dp)
    val clickModifier = if (onClick != null) base.clickable(onClick = onClick) else base
    Row(
        modifier = clickModifier.padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SettingsInk.fg(),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsRowText.fg()
            ),
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun SettingsChevron() {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = SettingsFaint.fg(),
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun NavRow(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    title: String,
    onClick: () -> Unit
) {
    val base = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .clickable(onClick = onClick)
    Row(
        modifier = base.padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = SettingsInk.fg(),
                modifier = Modifier.size(20.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SettingsInk.fg(),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsRowText.fg()
            ),
            modifier = Modifier.weight(1f)
        )
        SettingsChevron()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        onClick = { onCheckedChange(!checked) }
    ) {
        FlatToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FlatToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) SettingsOn.bg() else SettingsBorder.bg())
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) SettingsRowText.bg() else Color.White.bg())
        )
    }
}

@Composable
private fun AccountCard(phoneNumber: String, onEditProfile: () -> Unit) {
    SettingsCard {
        NavRow(
            iconRes = R.drawable.ic_profile_person,
            title = stringResource(R.string.edit_profile),
            onClick = onEditProfile
        )
        SettingsDividerLine()
        SettingsRow(
            icon = Icons.Outlined.Phone,
            title = stringResource(R.string.phone_number),
            onClick = onEditProfile
        ) {
            Text(
                text = if (phoneNumber.isBlank()) "—" else phoneNumber,
                style = TextStyle(fontSize = 14.sp, color = SettingsMuted.fg())
            )
            Spacer(modifier = Modifier.width(6.dp))
            SettingsChevron()
        }
    }
}

@Composable
private fun PreferencesCard() {
    val themeViewModel: ThemeModeViewModel = hiltViewModel()
    val themeMode = LocalThemeMode.current
    val systemDark = isSystemInDarkTheme()
    val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && systemDark)
    SettingsCard {
        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.language)
        ) {
            LanguageChips()
        }
        SettingsDividerLine()
        ToggleRow(
            icon = Icons.Outlined.DarkMode,
            title = stringResource(R.string.dark_mode),
            checked = isDark,
            onCheckedChange = { on ->
                themeViewModel.setMode(if (on) ThemeMode.DARK else ThemeMode.LIGHT)
            }
        )
    }
}

@Composable
private fun LanguageChips() {
    val context = LocalContext.current
    val current = LocaleHelper.getLanguage(context)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LanguageChip("EN", current == LocaleHelper.LANGUAGE_ENGLISH) {
            applyLanguageChange(context, LocaleHelper.LANGUAGE_ENGLISH)
        }
        LanguageChip("हिन्दी", current == LocaleHelper.LANGUAGE_HINDI) {
            applyLanguageChange(context, LocaleHelper.LANGUAGE_HINDI)
        }
        LanguageChip("తెలుగు", current == LocaleHelper.LANGUAGE_TELUGU) {
            applyLanguageChange(context, LocaleHelper.LANGUAGE_TELUGU)
        }
    }
}

@Composable
private fun LanguageChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) SettingsRowText.bg() else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = if (active) 12.dp else 10.dp, end = if (active) 12.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) Color.White else SettingsMuted.fg()
            )
        )
    }
}

@Composable
private fun NotificationsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pushEnabled by remember { mutableStateOf(readSettingsFlag(context, KEY_PUSH_ENABLED)) }
    // "WhatsApp offers" lives on the server (consent record, STOP replies); off until switched on.
    var whatsappEnabled by remember { mutableStateOf(false) }
    var whatsappBusy by remember { mutableStateOf(false) }
    val signedIn = FirebaseAuth.getInstance().currentUser != null
    LaunchedEffect(signedIn) {
        if (!signedIn) return@LaunchedEffect
        runCatching { callWhatsappPromos(null) }.onSuccess { whatsappEnabled = it }
    }
    SettingsCard {
        ToggleRow(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.push_notifications),
            checked = pushEnabled,
            onCheckedChange = { on ->
                pushEnabled = on
                writeSettingsFlag(context, KEY_PUSH_ENABLED, on)
            }
        )
        SettingsDividerLine()
        ToggleRow(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = stringResource(R.string.whatsapp_alerts),
            checked = whatsappEnabled,
            onCheckedChange = { on ->
                if (!signedIn || whatsappBusy) return@ToggleRow
                whatsappBusy = true
                whatsappEnabled = on
                scope.launch {
                    runCatching { callWhatsappPromos(on) }
                        .onSuccess { whatsappEnabled = it }
                        .onFailure {
                            whatsappEnabled = !on
                            Toast.makeText(context, R.string.whatsapp_offers_failed, Toast.LENGTH_SHORT).show()
                        }
                    whatsappBusy = false
                }
            }
        )
        Text(
            text = stringResource(R.string.whatsapp_offers_sub),
            style = TextStyle(fontSize = 12.sp, color = SettingsMuted.fg(), lineHeight = 17.sp),
            modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 12.dp)
        )
    }
}

/** Reads ([enabled] = null) or sets the server's "WhatsApp offers" switch; returns the saved value. */
private suspend fun callWhatsappPromos(enabled: Boolean?): Boolean {
    val payload = if (enabled == null) emptyMap<String, Any>() else mapOf("enabled" to enabled)
    val data = FirebaseFunctions.getInstance("asia-south1")
        .getHttpsCallable("whatsappPromos").call(payload).await().data as? Map<*, *>
    return data?.get("enabled") == true
}

@Composable
private fun LegalCard(navController: NavController, userRole: String = "WORKER") {
    val roleParam = if (userRole.equals("EMPLOYER", ignoreCase = true)) {
        com.example.dutype.guidelines.GuidelineRole.EMPLOYER
    } else {
        com.example.dutype.guidelines.GuidelineRole.WORKER
    }
    val safetyRoute = Routes.guidelinesRoute(roleParam)

    SettingsCard {
        NavRow(
            iconRes = R.drawable.ic_profile_help,
            title = stringResource(R.string.guide_entry),
            onClick = { navController.navigate(safetyRoute) }
        )
        SettingsDividerLine()
        NavRow(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.privacy_policy),
            onClick = { navController.navigate(Routes.PRIVACY_POLICY) }
        )
        SettingsDividerLine()
        NavRow(
            icon = Icons.Outlined.Description,
            title = stringResource(R.string.terms_of_service),
            onClick = { navController.navigate(Routes.TERMS_OF_SERVICE) }
        )
    }
}

@Composable
private fun DangerRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 15.sp, color = SettingsDanger.fg())
        )
    }
}

@Composable
private fun DangerCard(
    logoutText: String,
    deleteText: String,
    onLogout: () -> Unit,
    onDelete: () -> Unit
) {
    SettingsCard {
        DangerRow(text = logoutText, onClick = onLogout)
        SettingsDividerLine()
        DangerRow(text = deleteText, onClick = onDelete)
    }
}

@Composable
private fun SettingsFooter(appVersion: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DutyPe v$appVersion",
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SettingsFaint.fg()
            )
        )
        Text(
            text = stringResource(R.string.about_made_in_bharat),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SettingsFaint.fg()
            )
        )
    }
}
