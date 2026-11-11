package com.example.dutype.common.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.example.dutype.components.cascadeEnter
import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import android.app.Activity
import android.content.Intent
import com.example.dutype.MainActivity
import com.dutype.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
private val ScreenBg = Color.White
private val InkColor = Color(0xFF0F0F0F)
private val SubtitleGray = Color(0xFF64748B)
private val BorderNeutral = Color(0xFFE2E8F0)
private val SelectedTint = Color(0xFFF8FAFC)
private val RadioRing = Color(0xFFCBD5E1)
private val WorkerIconBg = Color(0xFFECFDF5)
private val WorkerIconTint = Color(0xFF10B981)
private val EmployerIconBg = Color(0xFFEFF6FF)
private val EmployerIconTint = Color(0xFF2563EB)
private val ServicesIconBg = Color(0xFFF5F3FF)
private val ServicesIconTint = Color(0xFF7C3AED)

@Composable
fun SelectRoleScreen(
    navController: NavHostController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    run { // follows the app theme (dark mode too)
        val context = LocalContext.current
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.DisposableEffect(view) {
            val window = (view.context as? android.app.Activity)?.window
            val insetsController = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
            val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
            insetsController?.isAppearanceLightStatusBars = true

            onDispose {
                insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
            }
        }
        val scope = rememberCoroutineScope()
        val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel =
            androidx.hilt.navigation.compose.hiltViewModel()

        var selectedRole by remember { mutableStateOf("WORKER") }
        var showLanguageBottomSheet by remember { mutableStateOf(false) }

        val isTelugu = LocaleHelper.getLanguage(context) == LocaleHelper.LANGUAGE_TELUGU
        val activeLangName = when (LocaleHelper.getLanguage(context)) {
            LocaleHelper.LANGUAGE_TELUGU -> "తెలుగు"
            LocaleHelper.LANGUAGE_HINDI -> "हिन्दी"
            else -> "English"
        }

        fun applyRole(role: String, asGuest: Boolean = true) {
            Timber.d("🔍 Role selected: $role, asGuest: $asGuest")
            if (role == "SERVICES") {
                navController.navigate(Routes.SERVICES) {
                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                    launchSingleTop = true
                }
                return
            }
            val targetRole = if (role == "EMPLOYER")
                com.example.dutype.models.UserRole.EMPLOYER
            else
                com.example.dutype.models.UserRole.WORKER
            scope.launch {
                profileCompletionViewModel.saveUserInfoToLocalStorage("", "", targetRole)
                runCatching {
                    val topic = if (targetRole == com.example.dutype.models.UserRole.EMPLOYER)
                        com.example.dutype.services.FCMTokenManager.TOPIC_EMPLOYERS
                    else
                        com.example.dutype.services.FCMTokenManager.TOPIC_WORKERS
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
                }
            }
            if (!asGuest) {
                navController.navigate("${Routes.ENHANCED_LOGIN}?role=$role")
            } else if (onRoleSelected != null) {
                onRoleSelected.invoke(role)
            } else {
                val dest = if (targetRole == com.example.dutype.models.UserRole.EMPLOYER)
                    Routes.EMPLOYER_HOME
                else
                    Routes.WORKER_HOME
                navController.navigate(dest) {
                    popUpTo(Routes.SELECT_ROLE) { inclusive = true }
                    launchSingleTop = true
                }
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
            // Top Bar with PhonePe style compact language selector pill on top right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Surface(
                    onClick = { showLanguageBottomSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Language",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeLangName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.35f))

            HeadlineBlock()

            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.cascadeEnter(index = 1)) {
                RoleSelectCard(
                    icon = Icons.Default.Engineering,
                    title = stringResource(R.string.select_role_looking_for_work),
                    subtitle = stringResource(R.string.select_role_worker_desc),
                    iconBg = WorkerIconBg.bg(),
                    iconTint = WorkerIconTint.fg(),
                    selected = selectedRole == "WORKER",
                    accentColor = Color(0xFF10B981),
                    onClick = { selectedRole = "WORKER" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(modifier = Modifier.cascadeEnter(index = 2)) {
                RoleSelectCard(
                    icon = Icons.Default.Business,
                    title = stringResource(R.string.select_role_want_to_hire),
                    subtitle = stringResource(R.string.select_role_employer_desc),
                    iconBg = EmployerIconBg.bg(),
                    iconTint = EmployerIconTint.fg(),
                    selected = selectedRole == "EMPLOYER",
                    accentColor = Color(0xFF2563EB),
                    onClick = { selectedRole = "EMPLOYER" }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(modifier = Modifier.cascadeEnter(index = 3)) {
                RoleSelectCard(
                    icon = Icons.Default.HomeRepairService,
                    title = stringResource(R.string.select_role_home_services),
                    subtitle = stringResource(R.string.select_role_home_services_desc),
                    iconBg = ServicesIconBg.bg(),
                    iconTint = ServicesIconTint.fg(),
                    selected = selectedRole == "SERVICES",
                    accentColor = Color(0xFF8B5CF6),
                    onClick = { selectedRole = "SERVICES" }
                )
            }

            Spacer(modifier = Modifier.weight(0.85f))

            val buttonText = when (selectedRole) {
                "WORKER" -> if (isTelugu) "ఉద్యోగాలను అన్వేషించండి" else "Explore Jobs"
                "EMPLOYER" -> if (isTelugu) "అభ్యర్థులను అన్వేషించండి" else "Explore Candidates"
                else -> if (isTelugu) "ఇంటి సర్వీసులను చూడండి" else "Explore Home Services"
            }

            Box(modifier = Modifier.cascadeEnter(index = 4)) {
                ContinueButton(
                    onClick = { applyRole(selectedRole, asGuest = true) },
                    accentColor = when (selectedRole) {
                        "WORKER" -> Color(0xFF10B981)
                        "EMPLOYER" -> Color(0xFF2563EB)
                        else -> Color(0xFF8B5CF6)
                    },
                    buttonText = buttonText
                )
            }



            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showLanguageBottomSheet) {
            com.example.dutype.components.LanguageSelectionBottomSheet(
                onDismiss = { showLanguageBottomSheet = false }
            )
        }
    }
}

@Composable
private fun HeadlineBlock() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val titleOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -10f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "heroTitleY"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "heroTitleAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = titleOffsetY
                alpha = titleAlpha
            }
    ) {
        Text(
            text = stringResource(R.string.select_role_headline),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = InkColor.fg()
            )
        )
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
    buttonText: String = stringResource(R.string.continue_text),
    accentColor: Color = Color(0xFF0F172A)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btnBounce"
    )

    // Swiggy-style continuous subtle shimmer beam passing across the surface every 3 seconds
    val infiniteTransition = rememberInfiniteTransition(label = "swiggyCtaShimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val width = size.width
                    val height = size.height
                    val x = shimmerTranslate * width
                    val brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = Offset(x - 80f, 0f),
                        end = Offset(x + 80f, height)
                    )
                    drawRect(brush = brush)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
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
}

@Composable
private fun RoleSelectCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring Bounce on Touch: Subtle scale down to 0.97f on press and bounce back with Spring.StiffnessMediumLow
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "roleCardSpringScale"
    )

    // Dynamic Selection State: Selected card expands border with glowing accent color
    val targetBorderColor = if (selected) accentColor else BorderNeutral.bd()
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 250),
        label = "roleCardBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "roleBorderWidth"
    )
    val cardBg by animateColorAsState(
        targetValue = if (selected) accentColor.copy(alpha = 0.06f) else Color.White.bg(),
        animationSpec = tween(durationMillis = 250),
        label = "roleCardBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 108.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(cardBg)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
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

            SelectionIndicator(selected = selected, accentColor = accentColor)
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(1.5.dp, if (selected) accentColor else RadioRing.bd(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectRoleScreen() {
    MaterialTheme {
        SelectRoleScreen(navController = rememberNavController())
    }
}
