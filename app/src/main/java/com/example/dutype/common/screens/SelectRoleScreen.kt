package com.example.dutype.common.screens

import com.dutype.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun SelectRoleScreen(
    navController: NavHostController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    com.example.dutype.ui.theme.ForceLightTheme {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
        var isVisible by remember { mutableStateOf(false) }
        var showLanguageBottomSheet by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isVisible = true
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFF1F5F9),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Top Language Selector Chip Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeLangName = when (com.example.dutype.utils.LocaleHelper.getLanguage(context)) {
                        com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU -> "తెలుగు"
                        "hi" -> "हिन्दी"
                        else -> "English"
                    }

                    Surface(
                        onClick = { showLanguageBottomSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeLangName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(300))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.how_can_we_help_today),
                            style = AppTypography.displayTitle.copy(
                                fontSize = 30.sp,
                                lineHeight = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Choose your role to get a personalized experience",
                            style = AppTypography.bodyLarge.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = tween(400, delayMillis = 100, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(400, delayMillis = 100))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        // Worker Role Card (Emerald Theme)
                        RoleCard(
                            icon = Icons.Default.WorkOutline,
                            title = stringResource(R.string.worker),
                            subtitle = stringResource(R.string.find_jobs_earn),
                            accentColor = Color(0xFF059669),
                            tintContainerColor = Color(0xFFECFDF5),
                            delay = 50,
                            onClick = {
                                Timber.d("🔍 Worker role selected")
                                val targetRole = com.example.dutype.models.UserRole.WORKER
                                scope.launch {
                                    profileCompletionViewModel.saveUserInfoToLocalStorage("", "", targetRole)
                                }
                                if (onRoleSelected != null) {
                                    onRoleSelected.invoke("WORKER")
                                } else {
                                    navController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")
                                }
                            }
                        )

                        // Employer Role Card (Cobalt Theme)
                        RoleCard(
                            icon = Icons.Default.Business,
                            title = stringResource(R.string.employer),
                            subtitle = stringResource(R.string.hire_skilled_workers),
                            accentColor = Color(0xFF2563EB),
                            tintContainerColor = Color(0xFFEFF6FF),
                            delay = 120,
                            onClick = {
                                Timber.d("🔍 Employer role selected")
                                val targetRole = com.example.dutype.models.UserRole.EMPLOYER
                                scope.launch {
                                    profileCompletionViewModel.saveUserInfoToLocalStorage("", "", targetRole)
                                }
                                if (onRoleSelected != null) {
                                    onRoleSelected.invoke("EMPLOYER")
                                } else {
                                    navController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Safe & Secure Trust Badge
                        SafeSecurePill()
                    }
                }
            }

            if (showLanguageBottomSheet) {
                com.example.dutype.components.LanguageSelectionBottomSheet(
                    onDismiss = { showLanguageBottomSheet = false }
                )
            }
        }
    }
}

@Composable
fun RoleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    tintContainerColor: Color,
    delay: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) { if (title.contains("Worker", ignoreCase = true)) -60 else 60 } +
            fadeIn(animationSpec = tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 1.dp
            ),
            border = BorderStroke(
                width = 1.dp,
                color = Color(0xFFE2E8F0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactical Icon Container (Stitch 2.0 rounded squircle)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tintContainerColor)
                        .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTypography.displayTitle.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF64748B),
                            lineHeight = 19.sp,
                            fontSize = 13.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Modern Pill Arrow Action
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafeSecurePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Color(0xFF059669),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.safe_secure_trusted),
            style = AppTypography.bodyMedium.copy(
                color = Color(0xFF475569),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
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