package com.example.dutype.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.components.LanguageOptionCard
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
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
 * 
 * @author DutyPe Engineering Team
 * @since 2.1.0
 */

// Define color palette for consistency
private val PrimaryOrange = Color(0xFFFF8C32)
private val TextDark = Color(0xFF1A1C1E)
private val TextGray = Color(0xFF6B7280)
private val BackgroundLight = Color(0xFFFAFAFA)

private val onboardingPages = listOf(
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen1,
        title = "Fast Trusted Service",
        description = "Connect instantly with verified professionals for all your service needs."
    ),
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen2,
        title = "Real-time Tracking",
        description = "Monitor your job status and worker location in real-time for peace of mind."
    ),
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen3,
        title = "Hyper-local Jobs",
        description = "Find opportunities nearby or hire local talent quickly and efficiently."
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
                LocaleHelper.saveLanguage(context, language)
                markLanguageAsSelected(context)
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
 * Uses shared LanguageOptionCard component with orange accent color
 * Shows all text in the SELECTED language (not mixed)
 */
@Composable
private fun FirstTimeLanguageSelection(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Language icon with animation
            val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer { 
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimaryOrange.copy(alpha = 0.15f),
                                Color(0xFF3B82F6).copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background globe
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = PrimaryOrange.copy(alpha = 0.3f)
                )
                // Foreground emoji
                Text(
                    text = if (isTeluguSelected) "🇮🇳" else "🇬🇧",
                    fontSize = 48.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title - shows in selected language
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextGray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Language options - using shared LanguageOptionCard component
            LanguageOptionCard(
                emoji = "🇬🇧",
                name = "English",
                nativeName = "English",
                isSelected = currentSelection == LocaleHelper.LANGUAGE_ENGLISH,
                accentColor = PrimaryOrange, // Orange for onboarding
                onClick = { currentSelection = LocaleHelper.LANGUAGE_ENGLISH }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LanguageOptionCard(
                emoji = "🇮🇳",
                name = "Telugu",
                nativeName = "తెలుగు",
                isSelected = currentSelection == LocaleHelper.LANGUAGE_TELUGU,
                accentColor = PrimaryOrange, // Orange for onboarding
                onClick = { currentSelection = LocaleHelper.LANGUAGE_TELUGU }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue button - text in selected language
            Button(
                onClick = { onLanguageSelected(currentSelection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryOrange
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = continueText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text - in selected language only
            Text(
                text = changeAnytimeText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            )
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
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Animate background elements based on page
    val animatedOffsetX by animateFloatAsState(
        targetValue = pagerState.currentPage * 100f,
        animationSpec = tween(1000, easing = LinearOutSlowInEasing), 
        label = "bg_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Decorative background elements
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Top circle (moves slightly with page)
            drawCircle(
                color = PrimaryOrange.copy(alpha = 0.05f),
                center = Offset(x = canvasWidth * 0.8f - animatedOffsetX * 0.2f, y = canvasHeight * 0.15f),
                radius = canvasWidth * 0.4f
            )
            
            // Bottom circle
            drawCircle(
                color = Color(0xFF2196F3).copy(alpha = 0.03f),
                center = Offset(x = canvasWidth * 0.1f + animatedOffsetX * 0.2f, y = canvasHeight * 0.85f),
                radius = canvasWidth * 0.5f
            )
        }
        
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
                onSkip = {
                    navController.navigate(Routes.SELECT_ROLE) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
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
                
                OnboardingPage(
                    content = onboardingPages[page],
                    pageOffset = pageOffset
                )
            }

            // Bottom controls
            BottomControls(
                pagerState = pagerState,
                onNext = {
                    if (pagerState.currentPage == onboardingPages.lastIndex) {
                        Timber.d("🎯 OnboardingScreen - Completed! Navigating to SELECT_ROLE")
                        navController.navigate(Routes.SELECT_ROLE) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
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
    onSkip: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { 
                    Timber.d("🎯 OnboardingScreen - Skip clicked")
                    onSkip() 
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Skip",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    content: OnboardingPageContent,
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
        // Image Container with Parallax
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(16.dp)
                .graphicsLayer {
                    // Move image slightly slower than swipe
                    translationX = pageOffset * 100f
                    scaleX = 1f - (absOffset * 0.1f)
                    scaleY = scaleX
                },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = content.imageRes),
                contentDescription = content.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Text Content with different parallax
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    // Move text slightly faster/more delay
                    translationX = pageOffset * 50f
                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                }
        ) {
            Text( 
                text = content.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            )
        }
    }
}

@Composable
private fun BottomControls(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button (Hide on first page)
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = pagerState.currentPage > 0,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                // Using renamed OnboardingIconButton to avoid shadowing Material3 IconButton
                OnboardingIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFEEEEEE), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextGray
                    )
                }
            }
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
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "button_pulse"
            )

            // Using renamed OnboardingIconButton to avoid shadowing Material3 IconButton
            OnboardingIconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryOrange, Color(0xFFFFB74D))
                        ),
                        shape = CircleShape
                    )
                    .shadow(12.dp, CircleShape, spotColor = PrimaryOrange.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Custom IconButton for onboarding navigation
 * Renamed from IconButton to avoid shadowing Material3 IconButton
 */
@Composable
private fun OnboardingIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class OnboardingPageContent(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(navController = rememberNavController())
}
