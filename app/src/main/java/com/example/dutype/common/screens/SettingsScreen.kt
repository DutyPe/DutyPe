package com.example.dutype.common.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

// Airbnb style clean color palette
private val AirbnbTitleColor = Color(0xFF222222)
private val AirbnbSubtitleColor = Color(0xFF717171)
private val AirbnbDividerColor = Color(0xFFF0F0F0)
private val AirbnbSwitchActiveColor = Color(0xFF222222)
private val AirbnbSwitchTrackInactive = Color(0xFFB0B0B0)
private val AirbnbDangerColor = Color(0xFFE11D48)

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

    val scrollState = rememberScrollState()
    val compactTitleProgress by remember {
        derivedStateOf {
            ((scrollState.value - 30f) / 60f).coerceIn(0f, 1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.bg())
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Pinned sticky top bar with Back Button and scroll-linked compact title
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
                    tint = AirbnbTitleColor.fg(),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.settings),
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AirbnbTitleColor.fg(),
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
            color = AirbnbDividerColor.bd().copy(alpha = compactTitleProgress)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 40.dp)
        ) {
            // Large Title + Subtitle inside the scrollable column so it scrolls up
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = AirbnbTitleColor.fg(),
                        letterSpacing = (-0.5).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Control your account, preferences, and privacy settings on DutyPe.",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = AirbnbSubtitleColor.fg(),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Section 1: Account
            AirbnbSectionHeader(title = stringResource(R.string.settings_account))
            AirbnbActionRow(
                iconRes = R.drawable.ic_profile_person,
                title = stringResource(R.string.edit_profile),
                onClick = openEditProfile
            )
            AirbnbHairlineDivider()
            AirbnbInfoRow(
                iconRes = R.drawable.ic_profile_person,
                title = stringResource(R.string.phone_number),
                value = if (phoneNumber.isBlank()) "—" else phoneNumber,
                onClick = openEditProfile
            )

            // Section 2: Preferences
            AirbnbSectionHeader(title = stringResource(R.string.settings_preferences))
            val themeViewModel: ThemeModeViewModel = hiltViewModel()
            val themeMode = LocalThemeMode.current
            val systemDark = isSystemInDarkTheme()
            val isDark = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && systemDark)

            AirbnbSwitchRow(
                iconRes = R.drawable.ic_profile_settings,
                title = stringResource(R.string.dark_mode),
                checked = isDark,
                onCheckedChange = { on ->
                    themeViewModel.setMode(if (on) ThemeMode.DARK else ThemeMode.LIGHT)
                }
            )
            AirbnbHairlineDivider()
            AirbnbLanguageRow()

            // Section 3: Notifications
            AirbnbSectionHeader(title = stringResource(R.string.notifications))
            var pushEnabled by remember { mutableStateOf(readSettingsFlag(context, KEY_PUSH_ENABLED)) }
            var whatsappEnabled by remember { mutableStateOf(false) }
            var whatsappBusy by remember { mutableStateOf(false) }
            val signedIn = currentUser != null

            LaunchedEffect(signedIn) {
                if (!signedIn) return@LaunchedEffect
                runCatching { callWhatsappPromos(null) }.onSuccess { whatsappEnabled = it }
            }

            AirbnbSwitchRow(
                iconRes = R.drawable.ic_bell_outline,
                title = stringResource(R.string.push_notifications),
                checked = pushEnabled,
                onCheckedChange = { on ->
                    pushEnabled = on
                    writeSettingsFlag(context, KEY_PUSH_ENABLED, on)
                }
            )
            AirbnbHairlineDivider()
            AirbnbSwitchRow(
                iconRes = R.drawable.ic_profile_message,
                title = stringResource(R.string.whatsapp_alerts),
                checked = whatsappEnabled,
                onCheckedChange = { on ->
                    if (!signedIn || whatsappBusy) return@AirbnbSwitchRow
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

            // Section 4: Legal & Policies
            AirbnbSectionHeader(title = stringResource(R.string.settings_legal))
            AirbnbActionRow(
                iconRes = R.drawable.ic_profile_hand_privacy,
                title = stringResource(R.string.privacy_policy),
                onClick = { navController.navigate(Routes.PRIVACY_POLICY) }
            )
            AirbnbHairlineDivider()
            AirbnbActionRow(
                iconRes = R.drawable.ic_profile_legal_book,
                title = stringResource(R.string.terms_of_service),
                onClick = { navController.navigate(Routes.TERMS_OF_SERVICE) }
            )

            // Section 5: Account Actions (Logout & Delete)
            if (currentUser != null) {
                AirbnbSectionHeader(title = "Account Management")
                AirbnbActionRow(
                    iconRes = R.drawable.ic_profile_logout_door,
                    title = stringResource(R.string.log_out),
                    onClick = { showLogoutDialog = true }
                )
                AirbnbHairlineDivider()
                AirbnbActionRow(
                    iconRes = R.drawable.ic_profile_logout_door,
                    title = stringResource(R.string.delete_account),
                    isDestructive = true,
                    onClick = { showAccountDeletionDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            SettingsAirbnbFooter(appVersion = appVersion)
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

/**
 * Top Header matching Airbnb Privacy Screen screenshot:
 * Clean back arrow, bold 30sp Title, lightweight 15sp subtitle text.
 */
@Composable
private fun AirbnbSettingsHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .offset(x = (-8).dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = AirbnbTitleColor.fg(),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = AirbnbTitleColor.fg(),
                letterSpacing = (-0.5).sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbSubtitleColor.fg(),
                lineHeight = 22.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Airbnb Section Header: Clean 18sp semi-bold black label, unboxed.
 */
@Composable
private fun AirbnbSectionHeader(title: String) {
    Text(
        text = title,
        style = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = AirbnbTitleColor.fg(),
            letterSpacing = (-0.1).sp
        ),
        modifier = Modifier.padding(start = 20.dp, top = 26.dp, end = 20.dp, bottom = 10.dp)
    )
}

/**
 * Subtle hairline divider between flat rows
 */
@Composable
private fun AirbnbHairlineDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        thickness = 0.8.dp,
        color = AirbnbDividerColor.bd()
    )
}

/**
 * Airbnb Action Row: Sleek outline icon, title, optional subtitle, and subtle chevron right.
 */
@Composable
private fun AirbnbActionRow(
    iconRes: Int? = null,
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (isDestructive) AirbnbDangerColor.fg() else AirbnbTitleColor.fg(),
                modifier = Modifier.size(25.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) AirbnbDangerColor.fg() else AirbnbTitleColor.fg(),
                modifier = Modifier.size(25.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDestructive) AirbnbDangerColor.fg() else AirbnbTitleColor.fg()
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = AirbnbSubtitleColor.fg(),
                        lineHeight = 18.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_airbnb_chevron_right),
            contentDescription = null,
            tint = Color(0xFF717171).fg(),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Airbnb Info Row: displays current value on the right
 */
@Composable
private fun AirbnbInfoRow(
    iconRes: Int,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = AirbnbTitleColor.fg(),
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = TextStyle(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbTitleColor.fg()
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbSubtitleColor.fg()
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_airbnb_chevron_right),
            contentDescription = null,
            tint = Color(0xFF717171).fg(),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Airbnb Switch Row: exactly matching the switch in image 1 (black rounded pill with white checkmark when on)
 */
@Composable
private fun AirbnbSwitchRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = AirbnbTitleColor.fg(),
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = AirbnbTitleColor.fg()
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = AirbnbSubtitleColor.fg(),
                        lineHeight = 18.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        AirbnbModernSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Custom Airbnb Pill Switch:
 * Exactly matches Airbnb's native switch (image 1 & 2):
 * - Checked: Solid black pill track (#222222) with white thumb featuring a crisp black checkmark.
 * - Unchecked: Medium grey pill track (#B0B0B0) with clean white circle thumb.
 * - Standard proportional height (32.dp) and width (52.dp).
 */
@Composable
internal fun AirbnbModernSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = Color(0xFF222222)
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) activeTrackColor else Color(0xFFB0B0B0),
        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (19.5).dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "switchThumb"
    )

    Box(
        modifier = modifier
            .width((51.5).dp)
            .height((31.5).dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg.bg())
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size((27.5).dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = activeTrackColor,
                    modifier = Modifier.size((15.5).dp)
                )
            }
        }
    }
}

@Composable
private fun AirbnbLanguageRow() {
    val context = LocalContext.current
    val current = LocaleHelper.getLanguage(context)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.translate_indic_24),
            contentDescription = null,
            tint = AirbnbTitleColor.fg(),
            modifier = Modifier.size(25.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = stringResource(R.string.language),
            style = TextStyle(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbTitleColor.fg()
            ),
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AirbnbLanguageChip("EN", current == LocaleHelper.LANGUAGE_ENGLISH) {
                applyLanguageChange(context, LocaleHelper.LANGUAGE_ENGLISH)
            }
            AirbnbLanguageChip("हिन्दी", current == LocaleHelper.LANGUAGE_HINDI) {
                applyLanguageChange(context, LocaleHelper.LANGUAGE_HINDI)
            }
            AirbnbLanguageChip("తెలుగు", current == LocaleHelper.LANGUAGE_TELUGU) {
                applyLanguageChange(context, LocaleHelper.LANGUAGE_TELUGU)
            }
        }
    }
}

@Composable
private fun AirbnbLanguageChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (active) AirbnbTitleColor.bg() else Color(0xFFF7F7F7).bg())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                color = if (active) Color.White else AirbnbTitleColor.fg()
            )
        )
    }
}

private suspend fun callWhatsappPromos(enabled: Boolean?): Boolean {
    val payload = if (enabled == null) emptyMap<String, Any>() else mapOf("enabled" to enabled)
    val data = FirebaseFunctions.getInstance("asia-south1")
        .getHttpsCallable("whatsappPromos").call(payload).await().data as? Map<*, *>
    return data?.get("enabled") == true
}

@Composable
private fun SettingsAirbnbFooter(appVersion: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "DutyPe v$appVersion",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbSubtitleColor.fg()
            )
        )
        Text(
            text = stringResource(R.string.about_made_in_bharat),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = AirbnbSubtitleColor.fg()
            )
        )
    }
}
