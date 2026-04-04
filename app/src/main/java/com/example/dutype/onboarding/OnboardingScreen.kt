package com.example.dutype.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

/**
 * OnboardingScreen - First-time user experience
 * 
 * REFACTORED (January 2026):
 * - Uses shared LanguageOptionCard component from components/
 * - Renamed custom IconButton to OnboardingIconButton to avoid shadowing Material3
 * - Improved code organization and documentation
 * - Updated to use Meesho-style colors and typography
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */

// Define color palette for consistency - Meesho style
private val PrimaryOrange = Color(0xFFFF8C32)
private val SecondaryOrange = Color(0xFFFFB25B)
private val AccentTeal = Color(0xFF0EA5A3)
private val TextDark = WorkerColors.TextPrimary
private val TextGray = WorkerColors.TextSecondary
private val BackgroundLight = Color.White
private val SurfaceWarm = Color(0xFFFFFBF6)

@Composable
private fun OnboardingBackdrop(
    modifier: Modifier = Modifier,
    pageFactor: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_orbit")
    val drift by infiniteTransition.animateFloat(
        initialValue = -26f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_drift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFCF8),
                    Color(0xFFF7FBFF)
                )
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PrimaryOrange.copy(alpha = 0.16f), Color.Transparent)
            ),
            center = Offset(
                x = size.width * 0.86f - (pageFactor * 90f) + drift,
                y = size.height * 0.16f
            ),
            radius = size.minDimension * 0.44f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentTeal.copy(alpha = 0.14f), Color.Transparent)
            ),
            center = Offset(
                x = size.width * 0.13f + (pageFactor * 70f) - drift,
                y = size.height * 0.83f
            ),
            radius = size.minDimension * 0.5f
        )
    }
}

// Onboarding page data - uses string resource IDs for localization
private data class OnboardingPageData(
    @DrawableRes val imageRes: Int,
    @androidx.annotation.StringRes val titleRes: Int,
    @androidx.annotation.StringRes val descriptionRes: Int
)

private val onboardingPagesData = listOf(
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
    
    // Check if language has been selected before (first-time users need to select)
    var showLanguageSelection by remember { 
        mutableStateOf(!hasLanguageBeenSelected(context)) 
    }
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.getLanguage(context)) }
    
    if (showLanguageSelection) {
        // Show language selection first for first-time users
        FirstTimeLanguageSelection(
            selectedLanguage = selectedLanguage,
            onLanguageSelected = { language ->
                selectedLanguage = language
                // Language is already saved in FirstTimeLanguageSelection
                // Just hide language selection and show onboarding (no restart needed)
                showLanguageSelection = false
            }
        )
    } else {
        // Show regular onboarding
        OnboardingContent(navController = navController)
    }
}

/**
 * First-time language selection screen shown before onboarding
 * Uses simple language cards matching the bottom sheet style
 * Shows all text in the SELECTED language (not mixed)
 * 
 * UPDATED (January 2026):
 * - Simplified to match LanguageSelectionBottomSheet style
 * - No flags, just script characters (అ, A)
 * - No longer restarts the app - just saves language and continues to onboarding
 */
@Composable
private fun FirstTimeLanguageSelection(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var currentSelection by remember { mutableStateOf(selectedLanguage) }
    
    // Get translations based on selected language
    val isTeluguSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU
    
    // Translations
    val title = if (isTeluguSelected) "మీ భాషను ఎంచుకోండి" else "Choose Your Language"
    val subtitle = if (isTeluguSelected) "మీకు ఇష్టమైన భాషను ఎంచుకోండి" else "Select your preferred language"
    val continueText = if (isTeluguSelected) "కొనసాగించు" else "Continue"
    val changeAnytimeText = if (isTeluguSelected) 
        "మీరు దీన్ని తర్వాత సెట్టింగ్స్‌లో మార్చవచ్చు" 
        else "You can change this later in Settings"
    
    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.88f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "DutyPe",
                    style = AppTypography.buttonMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = AppTypography.displayTitle.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                    )

                    Text(
                        text = subtitle,
                        style = AppTypography.bodyLarge.copy(
                            color = TextGray,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Language options - 2 column grid matching bottom sheet style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Telugu option
                SimpleLanguageCard(
                    scriptChar = "అ",
                    nativeName = "తెలుగు",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_TELUGU },
                    modifier = Modifier.weight(1f)
                )
                
                // English option
                SimpleLanguageCard(
                    scriptChar = "A",
                    nativeName = "English",
                    isSelected = currentSelection == LocaleHelper.LANGUAGE_ENGLISH,
                    onClick = { currentSelection = LocaleHelper.LANGUAGE_ENGLISH },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue button - text in selected language
            Button(
                onClick = { 
                    // Save language and mark as selected, then continue to onboarding
                    LocaleHelper.saveLanguage(context, currentSelection)
                    markLanguageAsSelected(context)
                    
                    // Apply locale immediately without restart
                    LocaleHelper.setLocale(context, currentSelection)
                    
                    // Continue to onboarding (no restart needed)
                    onLanguageSelected(currentSelection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryOrange, SecondaryOrange)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = continueText,
                        style = AppTypography.buttonLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text - in selected language only
            Text(
                text = changeAnytimeText,
                style = AppTypography.caption.copy(
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Simple language card matching the bottom sheet style
 */
@Composable
private fun SimpleLanguageCard(
    scriptChar: String,
    nativeName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryOrange else WorkerColors.Border,
        animationSpec = tween(200),
        label = "border_color"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "border_width"
    )
    
    Card(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 10.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = PrimaryOrange.copy(alpha = 0.22f),
                spotColor = PrimaryOrange.copy(alpha = 0.22f)
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceWarm else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Script character box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) {
                                Brush.horizontalGradient(
                                    colors = listOf(PrimaryOrange.copy(alpha = 0.2f), AccentTeal.copy(alpha = 0.15f))
                                )
                            } else {
                                Brush.horizontalGradient(colors = listOf(PrimaryOrange.copy(alpha = 0.1f), PrimaryOrange.copy(alpha = 0.08f)))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scriptChar,
                        style = AppTypography.pageTitle.copy(
                            color = PrimaryOrange,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Language name
                Text(
                    text = nativeName,
                    style = AppTypography.cardTitle.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            // Selection checkmark - top right corner
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PrimaryOrange),
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
                .background(Color.White.copy(alpha = 0.86f))
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
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .clickable { onSkip() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.skip),
                style = AppTypography.buttonMedium.copy(
                    color = TextGray
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
        val badgeColor = when (pageIndex) {
            0 -> PrimaryOrange
            1 -> AccentTeal
            else -> Color(0xFF7C3AED)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = "Step ${pageIndex + 1}",
                style = AppTypography.caption.copy(
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    .shadow(20.dp, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.92f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SurfaceWarm,
                                    Color.White
                                )
                            )
                        )
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .graphicsLayer {
                    // Move text slightly faster/more delay
                    translationX = pageOffset * 50f
                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Text(
                    text = title,
                    style = AppTypography.displayTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = AppTypography.bodyLarge.copy(
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun BottomControls(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val isLastPage = pagerState.currentPage == pagerState.pageCount - 1

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
                border = BorderStroke(1.dp, Color(0xFFE6E9EE)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.88f)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextGray
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
                            if (isSelected) PrimaryOrange else Color(0xFFE0E0E0)
                        )
                )
            }
        }

        // Next/Get Started Button with pulsing effect
        Button(
            onClick = onNext,
            modifier = Modifier
                .height(56.dp)
                .width(if (isLastPage) 164.dp else 148.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryOrange, SecondaryOrange)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.continue_text),
                    style = AppTypography.buttonMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = if (isLastPage) "Finish" else "Next",
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
