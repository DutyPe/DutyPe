package com.example.dutype.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import kotlinx.coroutines.launch

private val onboardingPages = listOf(
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen1,
        title = "Fast trusted service",
        description = "simply dummy text of the printing and\ntypesetting industry."
    ),
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen2,
        title = "Tracking online",
        description = "simply dummy text of the printing and\ntypesetting industry."
    ),
    OnboardingPageContent(
        imageRes = R.drawable.onboardscreen3,
        title = "Hyper-local part-time jobs",
        description = "Workers can find jobs and employers\ncan hire workers in a single app."
    )
)

@Composable
fun OnboardingScreen(navController: NavController) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F6))
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.large.copy(all = CornerSize(32.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar
                TopBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 16.dp),
                    onBack = { navController.popBackStack() },
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
                        .weight(1f)               // <-- this centers content better
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) { page ->
                    OnboardingPage(content = onboardingPages[page])
                }

                // Bottom button with page indicators
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button on the left (show only if not on first page)
                        if (pagerState.currentPage > 0) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF8C32))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(52.dp))
                        }

                        // Page indicator dots in the center
                        Row(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index) 
                                                Color(0xFFFF8C32) 
                                            else 
                                                Color(0xFFDDDDDD)
                                        )
                                )
                            }
                        }

                        // Next button on the right
                        NextCircleButton(
                            onClick = {
                                if (pagerState.currentPage == onboardingPages.lastIndex) {
                                    navController.navigate(Routes.SELECT_ROLE) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                } else {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Skip",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333),
            modifier = Modifier.clickable { onSkip() }
        )
    }
}

@Composable
private fun OnboardingPage(content: OnboardingPageContent) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Illustration – bigger and centered
        androidx.compose.foundation.Image(
            painter = painterResource(id = content.imageRes),
            contentDescription = content.title,
            modifier = Modifier
                .fillMaxWidth(1.0f)
                .aspectRatio(1f),   // keeps nice ratio
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text( 
            text = content.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF000000),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = content.description,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF9B9B9B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun NextCircleButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color(0xFFFF8C32))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Next",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
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
