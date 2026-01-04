package com.example.dutype.employer.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.components.CommonHeader
import com.example.dutype.models.SubscriptionPlan
import com.example.dutype.models.SubscriptionPlanType
import com.example.dutype.models. SubscriptionPlans
import com.example.dutype.models.UserSubscription
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.MeeshoFontFamily
import com.example.dutype.ui.theme.WorkerColors

// Theme colors matching Meesho-style design
private val PrimaryBlue = WorkerColors.Info
private val DarkBlue = Color(0xFF1E3A8A)
private val PurpleAccent = WorkerColors.Primary
private val GoldAccent = WorkerColors.Warning
private val SuccessGreen = WorkerColors.Success
private val BackgroundColor = WorkerColors.ScreenBackground
private val CardColor = WorkerColors.CardBackground
private val TextPrimary = WorkerColors.TextPrimary
private val TextSecondary = WorkerColors.TextSecondary

@Composable
fun SubscriptionScreen(
    currentSubscription: UserSubscription?,
    onBackClick: () -> Unit,
    onSelectPlan: (SubscriptionPlan, Boolean) -> Unit,
    isLoading: Boolean = false
) {
    var isYearly by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    
    val plans = SubscriptionPlans.ALL_PLANS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // CommonHeader
        CommonHeader(
            title = "Subscription Plans",
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Premium Hero Card
            PremiumHeroCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Current Plan (if subscribed)
            if (currentSubscription != null) {
                ActiveSubscriptionCard(subscription = currentSubscription)
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Billing Toggle
            BillingCycleCard(
                isYearly = isYearly,
                onToggle = { isYearly = it }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Plan Cards
            plans.forEach { plan ->
                SubscriptionPlanCard(
                    plan = plan,
                    isYearly = isYearly,
                    isCurrentPlan = currentSubscription?.planId == plan.id,
                    isSelected = selectedPlan?.id == plan.id,
                    onSelect = { selectedPlan = plan }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subscribe Button
            SubscribeActionButton(
                selectedPlan = selectedPlan,
                currentSubscription = currentSubscription,
                isYearly = isYearly,
                isLoading = isLoading,
                onSubscribe = onSelectPlan
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Trust & Security Section
            TrustSecurityCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Terms
            Text(
                text = "By subscribing, you agree to our Terms of Service and Privacy Policy. " +
                       "Subscriptions auto-renew unless cancelled 24 hours before renewal.",
                style = AppTypography.bodySmall.copy(
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PremiumHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(DarkBlue, PrimaryBlue, PurpleAccent)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Animated Crown
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Upgrade to Premium",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Unlock unlimited job posts, verified badge\n& priority support for your business",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Quick benefits row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickBenefit(icon = Icons.Default.AllInclusive, text = "Unlimited\nPosts")
                    QuickBenefit(icon = Icons.Default.Verified, text = "Verified\nBadge")
                    QuickBenefit(icon = Icons.Default.Speed, text = "Priority\nSupport")
                }
            }
        }
    }
}

@Composable
private fun QuickBenefit(icon: ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun ActiveSubscriptionCard(subscription: UserSubscription) {
    val planName = subscription.planType.lowercase().replaceFirstChar { it.uppercase() }
    val daysRemaining = ((subscription.endDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(SuccessGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Active: $planName Plan",
                    style = AppTypography.sectionHeader.copy(
                        color = Color(0xFF065F46),
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (daysRemaining > 0) "$daysRemaining days remaining" else "Renewal due",
                    style = AppTypography.bodySmall.copy(color = TextSecondary)
                )
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SuccessGreen.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "ACTIVE",
                    style = AppTypography.labelSmall.copy(
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BillingCycleCard(
    isYearly: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose Billing Cycle",
                style = AppTypography.sectionHeader.copy(color = TextPrimary)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(4.dp)
            ) {
                // Monthly Option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isYearly) CardColor else Color.Transparent)
                        .clickable { onToggle(false) }
                        .padding(horizontal = 32.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Monthly",
                        fontWeight = if (!isYearly) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (!isYearly) PrimaryBlue else TextSecondary
                    )
                }
                
                // Yearly Option with Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isYearly) CardColor else Color.Transparent)
                        .clickable { onToggle(true) }
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Yearly",
                            fontWeight = if (isYearly) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isYearly) PrimaryBlue else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SuccessGreen
                        ) {
                            Text(
                                text = "SAVE 17%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isYearly: Boolean,
    isCurrentPlan: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "scale"
    )
    
    // Shimmer animation for selected cards
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    val planGradient = getPlanGradient(plan.type)
    val planColor = getPlanColor(plan.type)
    val planCardBackground = getPlanCardBackground(plan.type)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 20.dp else 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = planColor.copy(alpha = 0.4f),
                spotColor = planColor.copy(alpha = 0.5f)
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isSelected) BorderStroke(3.dp, Brush.linearGradient(planGradient)) 
                 else BorderStroke(1.dp, planColor.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = planCardBackground,
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
            // Multiple decorative elements for rich background
            // Large gradient circle top-left
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .offset(x = (-60).dp, y = (-60).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                planColor.copy(alpha = 0.15f),
                                planColor.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Medium circle top-right
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-30).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                planGradient[1].copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Small accent circle bottom-right
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                planGradient.last().copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            // Diagonal shimmer stripe for selected cards
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                start = androidx.compose.ui.geometry.Offset(
                                    x = shimmerOffset * 1000f - 200f,
                                    y = 0f
                                ),
                                end = androidx.compose.ui.geometry.Offset(
                                    x = shimmerOffset * 1000f + 200f,
                                    y = 600f
                                )
                            )
                        )
                )
            }
            
            Column {
                // Colorful Top Banner
                if (plan.isPopular || isCurrentPlan || plan.type != SubscriptionPlanType.FREE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(planGradient)
                            )
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    isCurrentPlan -> Icons.Default.CheckCircle
                                    plan.isPopular -> Icons.Default.AutoAwesome
                                    else -> getPlanIcon(plan.type)
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isCurrentPlan -> "✓ YOUR CURRENT PLAN"
                                    plan.isPopular -> "⭐ MOST POPULAR CHOICE"
                                    else -> getPlanBannerText(plan.type)
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                
                Column(modifier = Modifier.padding(24.dp)) {
                    // Plan Header with colorful icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Gradient Icon Box
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        brush = Brush.linearGradient(planGradient),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = planColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getPlanIcon(plan.type),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = plan.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = planColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = getPlanTagline(plan.type),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        // Colorful Selection Indicator
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (isSelected) Brush.linearGradient(planGradient)
                                    else Brush.linearGradient(listOf(Color(0xFFE5E7EB), Color(0xFFE5E7EB))),
                                    CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    brush = if (isSelected) Brush.linearGradient(planGradient) 
                                           else Brush.linearGradient(listOf(Color(0xFFD1D5DB), Color(0xFFD1D5DB))),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Price with gradient text effect
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (isYearly) plan.displayPriceYearly else plan.displayPriceMonthly,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = planColor
                        )
                        if (plan.priceMonthly > 0) {
                            Text(
                                text = "/${if (isYearly) "year" else "mo"}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                            )
                        }
                    }
                    
                    // Yearly Savings Badge
                    if (isYearly && plan.priceMonthly > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${plan.priceMonthly / 100 * 12}",
                                fontSize = 16.sp,
                                color = TextSecondary.copy(alpha = 0.6f),
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(SuccessGreen, Color(0xFF34D399))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🎉 Save ₹${((plan.priceMonthly * 12) - plan.priceYearly) / 100}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Colorful divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        planColor.copy(alpha = 0.1f),
                                        planColor.copy(alpha = 0.3f),
                                        planColor.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Features with colorful icons
                    plan.features.forEachIndexed { index, feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        brush = Brush.linearGradient(planGradient),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = feature,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Select Button for each card
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onSelect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) planColor else Color.Transparent
                        ),
                        border = if (!isSelected) BorderStroke(2.dp, planColor) else null,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isSelected) "✓ Selected" else "Select ${plan.name}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isSelected) Color.White else planColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscribeActionButton(
    selectedPlan: SubscriptionPlan?,
    currentSubscription: UserSubscription?,
    isYearly: Boolean,
    isLoading: Boolean,
    onSubscribe: (SubscriptionPlan, Boolean) -> Unit
) {
    val isEnabled = selectedPlan != null && 
                    selectedPlan.id != currentSubscription?.planId &&
                    selectedPlan.priceMonthly > 0 &&
                    !isLoading
    
    Button(
        onClick = {
            selectedPlan?.let { plan ->
                if (plan.priceMonthly > 0) {
                    onSubscribe(plan, isYearly)
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryBlue,
            disabledContainerColor = Color(0xFFE5E7EB)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = when {
                    selectedPlan == null -> Icons.Default.TouchApp
                    selectedPlan.id == currentSubscription?.planId -> Icons.Default.CheckCircle
                    selectedPlan.priceMonthly == 0 -> Icons.Default.Star
                    else -> Icons.Default.CreditCard
                },
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = when {
                    selectedPlan == null -> "Select a Plan to Continue"
                    selectedPlan.id == currentSubscription?.planId -> "This is Your Current Plan"
                    selectedPlan.priceMonthly == 0 -> "Continue with Free Plan"
                    else -> "Subscribe Now • ${if (isYearly) selectedPlan.displayPriceYearly else selectedPlan.displayPriceMonthly}"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TrustSecurityCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Why Choose DutyPe Premium?",
                style = AppTypography.sectionHeader.copy(color = PrimaryBlue)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrustItem(icon = Icons.Default.Security, text = "Secure\nPayments")
                TrustItem(icon = Icons.Default.Autorenew, text = "Cancel\nAnytime")
                TrustItem(icon = Icons.Default.Support, text = "24/7\nSupport")
                TrustItem(icon = Icons.Default.Verified, text = "Trusted by\n1000+")
            }
        }
    }
}

@Composable
private fun TrustItem(icon: ImageVector, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = AppTypography.labelSmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center
            ),
            lineHeight = 14.sp
        )
    }
}

// Helper functions
private fun getPlanColor(type: SubscriptionPlanType): Color {
    return when (type) {
        SubscriptionPlanType.FREE -> Color(0xFF64748B)
        SubscriptionPlanType.BASIC -> Color(0xFF3B82F6)
        SubscriptionPlanType.PRO -> Color(0xFF8B5CF6)
        SubscriptionPlanType.ENTERPRISE -> Color(0xFFEA580C)
    }
}

private fun getPlanGradient(type: SubscriptionPlanType): List<Color> {
    return when (type) {
        SubscriptionPlanType.FREE -> listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFCBD5E1))
        SubscriptionPlanType.BASIC -> listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6), Color(0xFF60A5FA))
        SubscriptionPlanType.PRO -> listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6), Color(0xFFA78BFA))
        SubscriptionPlanType.ENTERPRISE -> listOf(Color(0xFFDC2626), Color(0xFFEA580C), Color(0xFFF97316))
    }
}

// Rich gradient backgrounds for each plan card
private fun getPlanCardBackground(type: SubscriptionPlanType): List<Color> {
    return when (type) {
        SubscriptionPlanType.FREE -> listOf(
            Color(0xFFF8FAFC),
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC)
        )
        SubscriptionPlanType.BASIC -> listOf(
            Color(0xFFEFF6FF),  // Light blue top
            Color(0xFFDBEAFE),  // Soft blue
            Color(0xFFBFDBFE),  // Medium blue tint
            Color(0xFFEFF6FF)   // Light blue bottom
        )
        SubscriptionPlanType.PRO -> listOf(
            Color(0xFFF5F3FF),  // Light purple top
            Color(0xFFEDE9FE),  // Soft purple
            Color(0xFFDDD6FE),  // Medium purple tint
            Color(0xFFFDF4FF)   // Light pink-purple bottom
        )
        SubscriptionPlanType.ENTERPRISE -> listOf(
            Color(0xFFFFF7ED),  // Light orange top
            Color(0xFFFFEDD5),  // Soft orange
            Color(0xFFFED7AA),  // Medium orange tint
            Color(0xFFFEF3C7)   // Light yellow-orange bottom
        )
    }
}

private fun getPlanLightBackground(type: SubscriptionPlanType): Color {
    return when (type) {
        SubscriptionPlanType.FREE -> Color(0xFFF8FAFC)
        SubscriptionPlanType.BASIC -> Color(0xFFEFF6FF)
        SubscriptionPlanType.PRO -> Color(0xFFF5F3FF)
        SubscriptionPlanType.ENTERPRISE -> Color(0xFFFFF7ED)
    }
}

private fun getPlanBannerText(type: SubscriptionPlanType): String {
    return when (type) {
        SubscriptionPlanType.FREE -> "START FREE"
        SubscriptionPlanType.BASIC -> "💼 GREAT VALUE"
        SubscriptionPlanType.PRO -> "⭐ BEST FOR GROWTH"
        SubscriptionPlanType.ENTERPRISE -> "🏆 ULTIMATE POWER"
    }
}

private fun getPlanIcon(type: SubscriptionPlanType): ImageVector {
    return when (type) {
        SubscriptionPlanType.FREE -> Icons.Default.Person
        SubscriptionPlanType.BASIC -> Icons.Default.Bolt
        SubscriptionPlanType.PRO -> Icons.Default.Verified
        SubscriptionPlanType.ENTERPRISE -> Icons.Default.Business
    }
}

private fun getPlanTagline(type: SubscriptionPlanType): String {
    return when (type) {
        SubscriptionPlanType.FREE -> "Get started for free"
        SubscriptionPlanType.BASIC -> "Perfect for small businesses"
        SubscriptionPlanType.PRO -> "Best for growing teams"
        SubscriptionPlanType.ENTERPRISE -> "For large organizations"
    }
}
