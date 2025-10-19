package com.example.dutype.common.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import com.example.dutype.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.res.painterResource
import com.example.dutype.navigation.Routes

@Composable
fun SelectRoleScreen(
    navController: NavController,
    onRoleSelected: ((String) -> Unit)? = null
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Animated Header Section
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -50 },
                    animationSpec = tween(600)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App title
                    Text(
                        text = "DuTyPe",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 33.sp
                        ),
                        color = Color(0xFF212529),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose Your Role",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp
                        ),
                        color = Color(0xFF212529),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Find work or hire workers in one tap",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(33.dp))

            // Animated Role Cards
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { 100 },
                    animationSpec = tween(800, delayMillis = 300)
                ) + fadeIn(animationSpec = tween(800, delayMillis = 300))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Worker Card with staggered animation
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInHorizontally(
                            initialOffsetX = { -200 },
                            animationSpec = tween(600, delayMillis = 500)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
                    ) {
                        RoleCard(
                            animationRes = R.raw.waiter, // Use waiter.json animation
                            title = "Worker",
                            subtitle = "I'm looking for work",
                            gradientColors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                                Color(0xFF4CAF50).copy(alpha = 0.05f)
                            ),
                            accentColor = Color(0xFF4CAF50),
                            onClick = { 
                                println("🔍 Worker role selected, onRoleSelected callback: ${onRoleSelected != null}")
                                onRoleSelected?.invoke("WORKER") ?: navController.navigate(Routes.WORKER_ONBOARDING)
                            }
                        )
                    }

                    // Employer Card with staggered animation
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInHorizontally(
                            initialOffsetX = { 200 },
                            animationSpec = tween(600, delayMillis = 700)
                        ) + fadeIn(animationSpec = tween(600, delayMillis = 700))
                    ) {
                        RoleCard(
                            animationRes = R.raw.delivery, // Fixed: Use animationRes instead of imageRes
                            title = "Employer",
                            subtitle = "I want to hire workers",
                            gradientColors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.1f),
                                Color(0xFF2196F3).copy(alpha = 0.05f)
                            ),
                            accentColor = Color(0xFF2196F3),
                            onClick = { 
                                println("🔍 Employer role selected, onRoleSelected callback: ${onRoleSelected != null}")
                                onRoleSelected?.invoke("EMPLOYER") ?: navController.navigate(Routes.EMPLOYER_PROFILE_SETUP)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun RoleCard(
    animationRes: Int,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    accentColor: Color,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    // Lottie animation composition
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.8f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .clickable {
                isPressed = true
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = gradientColors,
                            radius = 400f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = Color(0xFF212529)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = "Arrow",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = Color(0xFF6C757D)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right side - Lottie Animation with NO background, NO clip, NO shadow
                Box(
                    modifier = Modifier
                        .size(123.dp), // Increased size for larger animation
                    contentAlignment = Alignment.Center
                ) {
                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier.size(153.dp) // Increased animation size
                    )
                }
            }

            // Accent border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
                    .align(Alignment.BottomCenter)
            )
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
