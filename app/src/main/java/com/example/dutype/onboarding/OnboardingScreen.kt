package com.example.dutype.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.*
import com.example.dutype.utils.LocaleHelper
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

private val PrimaryOrange = Color(0xFF111827)
private val PrimaryDark = Color(0xFF111827)
private val GlassTextPrimary = Color(0xFF0F172A)
private val GlassTextSecondary = Color(0xFF64748B)

data class OnboardingPageData(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val accentStart: Color,
    val accentEnd: Color
)

val onboardingPagesData = listOf(
    OnboardingPageData(
        icon = Icons.Filled.Bolt,
        titleRes = R.string.onboarding_title_1,
        descriptionRes = R.string.onboarding_desc_1,
        accentStart = Color(0xFFF59E0B),
        accentEnd = Color(0xFFEF4444)
    ),
    OnboardingPageData(
        icon = Icons.Filled.VerifiedUser,
        titleRes = R.string.onboarding_title_2,
        descriptionRes = R.string.onboarding_desc_2,
        accentStart = Color(0xFF2563EB),
        accentEnd = Color(0xFF7C3AED)
    ),
    OnboardingPageData(
        icon = Icons.Filled.LocationOn,
        titleRes = R.string.onboarding_title_3,
        descriptionRes = R.string.onboarding_desc_3,
        accentStart = Color(0xFF059669),
        accentEnd = Color(0xFF0EA5E9)
    )
)

@Composable
fun OnboardingScreen(navController: NavController) {
    com.example.dutype.ui.theme.ForceLightTheme {
    val context = LocalContext.current
    var showLanguageSelection by remember { mutableStateOf(!hasLanguageBeenSelected(context)) }
    
    if (showLanguageSelection) {
        FirstTimeLanguageSelection(
            selectedLanguage = LocaleHelper.getLanguage(context),
            onLanguageSelected = { lang ->
                // Persist + propagate locale to all activities. setLocale alone
                // only mutates the local Context — it does not survive activity
                // recreation, so subsequent onboarding/role-select screens render
                // in the previous language. Persist via saveLanguage and recreate
                // the host activity so AppCompat re-wraps every Composable.
                LocaleHelper.saveLanguage(context, lang)
                LocaleHelper.setLocale(context, lang)
                markLanguageAsSelected(context)
                showLanguageSelection = false
                (context as? android.app.Activity)?.recreate()
            }
        )
    } else {
        OnboardingContent(navController = navController)
    }
    }
}

@Composable
private fun FirstTimeLanguageSelection(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val effectiveInitialLanguage = if (selectedLanguage == "hi") {
        LocaleHelper.LANGUAGE_ENGLISH
    } else {
        selectedLanguage
    }
    var currentSelection by remember { mutableStateOf(effectiveInitialLanguage) }

    val isTeluguSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU

    val title = if (isTeluguSelected) "మీ భాషను ఎంచుకోండి" else "Choose Your Language"
    val subtitle = if (isTeluguSelected) {
        "జాబ్ వివరాలు, బటన్స్, సూచనలు మీకు సౌకర్యంగా కనిపించే భాషను ఎంచుకోండి"
    } else {
        "Pick the language that should appear across job details, buttons, and guidance"
    }
    val continueText = if (isTeluguSelected) "కొనసాగించు" else "Continue"
    val changeAnytimeText = if (isTeluguSelected) {
        "తర్వాత Settings లో కూడా భాషను మార్చవచ్చు"
    } else {
        "You can change this later from Settings"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFAEF),
                        Color(0xFFF8FAFC),
                        Color(0xFFEFF6FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = if (isTeluguSelected) "DutyPe లో మీ భాష" else "Your Language in DutyPe",
                        style = AppTypography.labelLarge.copy(
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = title,
                        style = AppTypography.displayTitle.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFFCBD5E1),
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SimpleLanguageCard(
                    scriptChar = "A",
                    nativeName = "English",
                    supportingText = "Simple and familiar across the app",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_ENGLISH,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_ENGLISH },
                    iconBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF0EA5E9))),
                    accentTint = Color(0xFF2563EB)
                )

                SimpleLanguageCard(
                    scriptChar = "అ",
                    nativeName = "తెలుగు",
                    supportingText = "తెలుగులో జాబ్స్, సూచనలు, బటన్స్",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_TELUGU },
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFDC2626))),
                    accentTint = Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onLanguageSelected(currentSelection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = continueText,
                    style = AppTypography.buttonLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = changeAnytimeText,
                style = AppTypography.caption.copy(
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LanguageSelectionBackdrop() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFEFF6FF),
                            Color(0xFFF8FAFC)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.14f),
                        Color.Transparent
                    )
                ),
                center = Offset(x = size.width * 0.1f, y = size.height * 0.1f),
                radius = size.width * 0.44f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                center = Offset(x = size.width * 0.88f, y = size.height * 0.18f),
                radius = size.width * 0.38f
            )
        }
    }
}

@Composable
private fun LanguageScreenDecor(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(width = 172.dp, height = 172.dp)
                .offset(x = 176.dp, y = 128.dp)
                .graphicsLayer {
                    rotationZ = 18f
                    alpha = 0.26f
                }
                .clip(RoundedCornerShape(42.dp))
                .background(Color.White.copy(alpha = 0.55f))
        )

        Box(
            modifier = Modifier
                .size(width = 126.dp, height = 88.dp)
                .offset(x = (-24).dp, y = 478.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFFFFEDD5).copy(alpha = 0.7f))
                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(30.dp))
        )

        Box(
            modifier = Modifier
                .size(210.dp)
                .offset(x = (-62).dp, y = 612.dp)
                .clip(CircleShape)
                .background(Color(0xFFA78BFA).copy(alpha = 0.1f))
                .blur(24.dp)
        )

        Box(
            modifier = Modifier
                .size(width = 188.dp, height = 116.dp)
                .offset(x = 104.dp, y = 554.dp)
                .clip(RoundedCornerShape(56.dp))
                .background(Color(0xFF93C5FD).copy(alpha = 0.12f))
                .blur(30.dp)
        )
    }
}

/**
 * Batch-n #3: Clean, flat language card. White surface, soft border,
 * one accent line + check chip when selected. No glassy gradients,
 * halos, or blurs — reads as a professional choice tile.
 */
@Composable
private fun SimpleLanguageCard(
    scriptChar: String,
    nativeName: String,
    supportingText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    iconBrush: Brush,
    accentTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentTint else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentTint.copy(alpha = 0.08f) else com.example.dutype.ui.theme.WorkerColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = scriptChar,
                    style = AppTypography.pageTitle.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nativeName,
                    style = AppTypography.cardTitle.copy(
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supportingText,
                    style = AppTypography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Helper functions for tracking if language has been selected
private const val PREF_LANGUAGE_SELECTED = "language_selected_first_time"

private fun hasLanguageBeenSelected(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("dutype_language_prefs", android.content.Context.MODE_PRIVATE)
    return prefs.getBoolean(PREF_LANGUAGE_SELECTED, false)
}

private fun markLanguageAsSelected(context: android.content.Context) {
    val prefs = context.getSharedPreferences("dutype_language_prefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putBoolean(PREF_LANGUAGE_SELECTED, true).apply()
}

@Composable
private fun OnboardingContent(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPagesData.size })
    val coroutineScope = rememberCoroutineScope()
    val profileCompletionViewModel: com.example.dutype.viewmodels.ProfileCompletionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    // Helper function to mark onboarding complete and navigate
    fun completeOnboardingAndNavigate() {
        coroutineScope.launch {
            Timber.d("🎯 OnboardingScreen - Marking onboarding as completed")
            profileCompletionViewModel.markOnboardingCompleted()
            profileCompletionViewModel.markAppAsOpened()
            navController.navigate(Routes.SELECT_ROLE) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }
    
    val pageFactor by remember(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        mutableStateOf(pagerState.currentPage + pagerState.currentPageOffsetFraction)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        OnboardingBackdrop(pageFactor = pageFactor)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar with Skip
            TopBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )

            // Pager takes the remaining height
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) { page ->
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                )
                val pageData = onboardingPagesData[page]
                
                OnboardingPage(
                    icon = pageData.icon,
                    accentStart = pageData.accentStart,
                    accentEnd = pageData.accentEnd,
                    title = stringResource(pageData.titleRes),
                    description = stringResource(pageData.descriptionRes),
                    pageIndex = page,
                    pageOffset = pageOffset
                )
            }

            // Bottom controls
            BottomControls(
                pagerState = pagerState,
                onSkip = {
                    Timber.d("🎯 OnboardingScreen - Skip clicked")
                    completeOnboardingAndNavigate()
                },
                onNext = {
                    if (pagerState.currentPage == onboardingPagesData.lastIndex) {
                        Timber.d("🎯 OnboardingScreen - Completed! Navigating to SELECT_ROLE")
                        completeOnboardingAndNavigate()
                    } else {
                        Timber.d("🎯 OnboardingScreen - Moving to page ${pagerState.currentPage + 1}")
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    }
}

// Helper function for lerp animation
fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@Composable
private fun OnboardingPage(
    icon: ImageVector,
    accentStart: Color,
    accentEnd: Color,
    title: String,
    description: String,
    pageIndex: Int,
    pageOffset: Float,
    modifier: Modifier = Modifier
) {
    val absOffset = pageOffset.absoluteValue
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Fade out pages as they leave
                alpha = lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - absOffset.coerceIn(0f, 1f)
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.4f))

        // Hero gradient block — replaces the static illustration. Pure
        // Compose so it renders crisp at any density and stays visually
        // role-neutral (suitable for both worker and employer flows).
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .graphicsLayer {
                    translationX = pageOffset * 80f
                    val scale = 1f - (absOffset * 0.08f)
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            // Outer soft halo
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentStart.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Mid ring
            Box(
                modifier = Modifier
                    .fillMaxSize(0.78f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
                    .border(
                        width = 1.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = accentStart.copy(alpha = 0.18f),
                        spotColor = accentEnd.copy(alpha = 0.18f)
                    )
            )

            // Inner gradient disc
            Box(
                modifier = Modifier
                    .fillMaxSize(0.56f)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accentStart, accentEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }

            // Decorative pill — top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = 24.dp)
                    .size(width = 56.dp, height = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                    .shadow(2.dp, RoundedCornerShape(999.dp))
            )

            // Decorative dot — bottom-left
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 18.dp, y = (-22).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentEnd.copy(alpha = 0.85f))
                    .shadow(4.dp, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Text Content with different parallax
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    translationX = pageOffset * 50f
                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                }
        ) {
            // Bug #7 fix: Step badge ("01", "02", "03") removed per product
            // request — the page indicator dots at the bottom already convey
            // progress, so the duplicated number chip was visual noise.

            Text(
                text = title,
                style = AppTypography.displayTitle.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = description,
                style = AppTypography.bodyLarge.copy(
                    color = GlassTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            )
        }

        Spacer(modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun OnboardingBackdrop(pageFactor: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = Color(0xFFF8FAFC))
    }
}

@Composable
private fun BottomControls(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
    val isTelugu = LocaleHelper.getLanguage(LocalContext.current) == LocaleHelper.LANGUAGE_TELUGU

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground.copy(alpha = 0.95f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.skip),
                style = AppTypography.buttonMedium.copy(
                    color = GlassTextPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Page Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot_width"
                )
                
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF111827) else Color(0xFFE2E8F0)
                        )
                )
            }
        }

        // Next/Get Started Button with pulsing effect
        Button(
            onClick = onNext,
            modifier = Modifier
                .height(56.dp)
                .width(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
            border = BorderStroke(1.dp, Color(0xFF111827)),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF111827), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = if (isLastPage) {
                        if (isTelugu) "పూర్తి" else "Finish"
                    } else {
                        if (isTelugu) "తదుపరి" else "Next"
                    },
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(navController = rememberNavController())
}
