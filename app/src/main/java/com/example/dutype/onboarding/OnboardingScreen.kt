package com.example.dutype.onboarding

import androidx.annotation.DrawableRes
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    @DrawableRes val imageRes: Int,
    val titleRes: Int,
    val descriptionRes: Int
)

val onboardingPagesData = listOf(
    OnboardingPageData(
        imageRes = R.drawable.onboardscreen1,
        titleRes = R.string.onboarding_title_1,
        descriptionRes = R.string.onboarding_desc_1
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboardscreen2,
        titleRes = R.string.onboarding_title_2,
        descriptionRes = R.string.onboarding_desc_2
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboardscreen3,
        titleRes = R.string.onboarding_title_3,
        descriptionRes = R.string.onboarding_desc_3
    )
)

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    var showLanguageSelection by remember { mutableStateOf(!hasLanguageBeenSelected(context)) }
    
    if (showLanguageSelection) {
        FirstTimeLanguageSelection(
            selectedLanguage = LocaleHelper.getLanguage(context),
            onLanguageSelected = { lang ->
                // Persist the selection so SharedPreferences-backed lookups
                // (LocaleHelper.getLanguage) and downstream FCM topic
                // subscriptions both reflect the new choice.
                LocaleHelper.saveLanguage(context, lang)
                LocaleHelper.setLocale(context, lang)
                markLanguageAsSelected(context)

                // Compose only re-resolves stringResource() values when the
                // configuration changes. attachBaseContext applied the prior
                // locale at activity creation, so the simplest reliable way
                // to re-render every screen (including the upcoming
                // onboarding pages) in the freshly selected language is to
                // restart MainActivity. This mirrors the behaviour used by
                // LanguageSelectionBottomSheet for in-app changes.
                val activity = context as? android.app.Activity
                if (activity != null) {
                    val intent = android.content.Intent(context, com.example.dutype.MainActivity::class.java)
                    intent.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    context.startActivity(intent)
                    activity.finish()
                } else {
                    showLanguageSelection = false
                }
            }
        )
    } else {
        OnboardingContent(navController = navController)
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
    Box(modifier = Modifier.fillMaxSize()) {
        LanguageSelectionBackdrop()
        LanguageScreenDecor(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFEEF2FF))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isTeluguSelected) "భాష" else "Language",
                    style = AppTypography.labelMedium.copy(
                        color = Color(0xFF4338CA),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = AppTypography.displayTitle.copy(
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    textAlign = TextAlign.Start
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = subtitle,
                style = AppTypography.bodyLarge.copy(
                    color = Color(0xFF475569),
                    lineHeight = 24.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SimpleLanguageCard(
                    scriptChar = "A",
                    nativeName = "English",
                    supportingText = "Simple and familiar across the app",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_ENGLISH,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_ENGLISH },
                    iconBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED))),
                    accentTint = Color(0xFF3B82F6)
                )

                SimpleLanguageCard(
                    scriptChar = "అ",
                    nativeName = "తెలుగు",
                    supportingText = "తెలుగులో జాబ్స్, సూచనలు, బటన్స్",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_TELUGU },
                    iconBrush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
                    accentTint = Color(0xFFF59E0B)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onLanguageSelected(currentSelection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
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
 * Screenshot-style glass language card for first-time selection
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
            .height(120.dp)
            .shadow(
                elevation = if (isSelected) 14.dp else 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) accentTint.copy(alpha = 0.65f) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isSelected) 0.36f else 0.24f),
                                accentTint.copy(alpha = if (isSelected) 0.12f else 0.05f),
                                Color.White.copy(alpha = if (isSelected) 0.14f else 0.08f)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (-24).dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(accentTint.copy(alpha = if (isSelected) 0.18f else 0.08f))
                    .blur(18.dp)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-18).dp, y = 22.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isSelected) 0.22f else 0.12f))
                    .blur(14.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
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
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 12.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF111827),
                                    Color(0xFF334155)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                pageLabel = "${pagerState.currentPage + 1}/${pagerState.pageCount}",
                onSkip = {
                    Timber.d("🎯 OnboardingScreen - Skip clicked")
                    completeOnboardingAndNavigate()
                }
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
                    imageRes = pageData.imageRes,
                    title = stringResource(pageData.titleRes),
                    description = stringResource(pageData.descriptionRes),
                    pageIndex = page,
                    pageOffset = pageOffset
                )
            }

            // Bottom controls
            BottomControls(
                pagerState = pagerState,
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
                },
                onBack = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
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
    modifier: Modifier = Modifier,
    pageLabel: String,
    onSkip: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(PrimaryOrange)
            )
            Text(
                text = pageLabel,
                style = AppTypography.caption.copy(
                    color = GlassTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .clickable { onSkip() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.skip),
                style = AppTypography.buttonMedium.copy(
                    color = GlassTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    @DrawableRes imageRes: Int,
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
        Spacer(modifier = Modifier.height(6.dp))

        // Image Container with layered card and parallax
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    // Move image slightly slower than swipe
                    translationX = pageOffset * 100f
                    scaleX = 1f - (absOffset * 0.1f)
                    scaleY = scaleX
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, RoundedCornerShape(32.dp), ambientColor = Color(0xFF94A3B8).copy(alpha = 0.12f), spotColor = Color(0xFF94A3B8).copy(alpha = 0.12f)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.96f)
                ),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Text Content with different parallax
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    // Move text slightly faster/more delay
                    translationX = pageOffset * 50f
                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                }
        ) {
            Text(
                text = title,
                style = AppTypography.displayTitle.copy(
                    fontWeight = FontWeight.Bold,
                    color = GlassTextPrimary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = AppTypography.bodyLarge.copy(
                    color = GlassTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            )
        }
    }
}

@Composable
fun OnboardingBackdrop(pageFactor: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Clean white base matching language selection
        drawRect(color = Color(0xFFF8FAFC))

        // Warm amber glow top-left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF59E0B).copy(alpha = 0.10f),
                    Color.Transparent
                )
            ),
            center = Offset(
                x = size.width * (0.1f + pageFactor * 0.08f),
                y = size.height * (0.08f - pageFactor * 0.02f)
            ),
            radius = size.width * 0.5f
        )

        // Soft blue glow top-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF60A5FA).copy(alpha = 0.10f),
                    Color.Transparent
                )
            ),
            center = Offset(
                x = size.width * (0.88f - pageFactor * 0.06f),
                y = size.height * (0.16f)
            ),
            radius = size.width * 0.4f
        )

        // Subtle violet bottom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA78BFA).copy(alpha = 0.06f),
                    Color.Transparent
                )
            ),
            center = Offset(
                x = size.width * (0.5f + pageFactor * 0.05f),
                y = size.height * 0.92f
            ),
            radius = size.width * 0.5f
        )
    }
}

@Composable
private fun BottomControls(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNext: () -> Unit,
    onBack: () -> Unit
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
        AnimatedVisibility(
            visible = pagerState.currentPage > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.size(width = 56.dp, height = 56.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.95f)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = if (isTelugu) "వెనక్కి" else "Back",
                    tint = GlassTextPrimary
                )
            }
        }

        if (pagerState.currentPage == 0) {
            Spacer(modifier = Modifier.width(56.dp))
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
