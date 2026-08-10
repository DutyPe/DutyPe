package com.example.dutype.onboarding

import com.dutype.app.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

// =============================================================================
// Onboarding Data Models (Immutable)
// =============================================================================

@Immutable
data class OnboardingPageData(
    @DrawableRes val imageRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

val onboardingPages = listOf(
    OnboardingPageData(
        imageRes = R.drawable.onboarding1,
        titleRes = R.string.onboarding_title_1,
        descriptionRes = R.string.onboarding_desc_1
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding2,
        titleRes = R.string.onboarding_title_2,
        descriptionRes = R.string.onboarding_desc_2
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding3,
        titleRes = R.string.onboarding_title_3,
        descriptionRes = R.string.onboarding_desc_3
    ),
    OnboardingPageData(
        imageRes = R.drawable.onboarding4,
        titleRes = R.string.onboarding_title_4,
        descriptionRes = R.string.onboarding_desc_4
    )
)

// =============================================================================
// Main Onboarding Screen
// =============================================================================

@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: ProfileCompletionViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    val isLastPage by remember {
        derivedStateOf { pagerState.currentPage == onboardingPages.lastIndex }
    }

    fun completeAndNavigate() {
        coroutineScope.launch {
            Timber.d("🎯 OnboardingScreen - Completing onboarding flow")
            viewModel.markOnboardingCompleted()
            viewModel.markAppAsOpened()
            navController.navigate(Routes.SELECT_ROLE) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar with Skip Button
            OnboardingHeader(
                isLastPage = isLastPage,
                onSkipClick = { completeAndNavigate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Pager Section (Occupies ~55-60% height for illustration + text)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val pageData = onboardingPages[pageIndex]

                // Page offset calculation for subtle scale & fade transition
                val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction).absoluteValue
                val imageScale = 1f - (pageOffset * 0.05f).coerceIn(0f, 0.05f)
                val imageAlpha = 1f - (pageOffset * 0.3f).coerceIn(0f, 0.3f)

                OnboardingPageContent(
                    pageData = pageData,
                    imageScale = imageScale,
                    imageAlpha = imageAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Footer Controls (Indicators + Navigation Button)
            OnboardingFooter(
                pageCount = onboardingPages.size,
                currentPage = pagerState.currentPage,
                isLastPage = isLastPage,
                onNextClick = {
                    if (isLastPage) {
                        completeAndNavigate()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            )
        }
    }
}

// =============================================================================
// Header Component (Top Right Skip Button)
// =============================================================================

@Composable
private fun OnboardingHeader(
    isLastPage: Boolean,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLanguageBottomSheet by remember { mutableStateOf(false) }

    val activeLangName = when (com.example.dutype.utils.LocaleHelper.getLanguage(context)) {
        com.example.dutype.utils.LocaleHelper.LANGUAGE_TELUGU -> "తెలుగు"
        com.example.dutype.utils.LocaleHelper.LANGUAGE_HINDI -> "हिन्दी"
        else -> "English"
    }

    Row(
        modifier = modifier.height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Left Language Selector Chip (matching SelectRoleScreen)
        Surface(
            onClick = { showLanguageBottomSheet = true },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Translate,
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
                    imageVector = androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Top Right Skip Button
        AnimatedVisibility(
            visible = !isLastPage,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            TextButton(
                onClick = onSkipClick,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF64748B)
                )
            }
        }
    }

    if (showLanguageBottomSheet) {
        com.example.dutype.components.LanguageSelectionBottomSheet(
            onDismiss = { showLanguageBottomSheet = false }
        )
    }
}

// =============================================================================
// Page Content Component (55-60% Image Height + M3 Typography Text)
// =============================================================================

@Composable
private fun OnboardingPageContent(
    pageData: OnboardingPageData,
    imageScale: Float,
    imageAlpha: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Image Container (Full width without heavy side padding so illustration is larger and clear)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = pageData.imageRes),
                contentDescription = stringResource(id = pageData.titleRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                        alpha = imageAlpha
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Text Section (Positioned higher up, directly under image with clean 24.dp side margins)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = pageData.titleRes),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 36.sp
                ),
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = pageData.descriptionRes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    fontSize = 16.sp
                ),
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// =============================================================================
// Footer Component (Animated Pill Indicators + Action Buttons)
// =============================================================================

@Composable
private fun OnboardingFooter(
    pageCount: Int,
    currentPage: Int,
    isLastPage: Boolean,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated Pill Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { index ->
                val isSelected = index == currentPage
                val targetWidth = if (isSelected) 24.dp else 8.dp
                val targetColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                }

                val animatedWidth by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = tween(durationMillis = 300),
                    label = "indicatorWidth"
                )
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 300),
                    label = "indicatorColor"
                )

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(animatedWidth)
                        .clip(CircleShape)
                        .background(animatedColor)
                )
            }
        }

        // Action Button (Next Floating Icon Button vs Get Started Button)
        androidx.compose.animation.AnimatedContent(
            targetState = isLastPage,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f))
                    .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f))
            },
            label = "onboardingButtonTransition"
        ) { targetIsLastPage ->
            if (targetIsLastPage) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier
                        .height(45.0.dp)
                        .padding(start = 16.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_get_started),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 13.dp)
                    )
                }
            } else {
                FilledIconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(45.0.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.onboarding_next),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(name = "Light Theme", showBackground = true)
@Composable
private fun OnboardingPreviewLight() {
    MaterialTheme {
        Surface {
            OnboardingScreen(navController = rememberNavController())
        }
    }
}
