package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.models.EmployerTrustTier
import com.example.dutype.models.getDisplayInfo
import com.example.dutype.models.parseTrustTier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustBadgesScreen(
    navController: NavController,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val screenBackgroundColor = Color.White
    LaunchedEffect(Unit) {
        onStatusBarColorChange?.invoke(screenBackgroundColor)
    }
    
    // Load current user's trust tier
    var currentTrustTier by remember { mutableStateOf("VERIFIED") }
    var isGstVerified by remember { mutableStateOf(false) }
    var completedJobsCount by remember { mutableStateOf(0) }
    var averageRating by remember { mutableStateOf(0f) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val currentUser = com.example.dutype.di.authFromHilt(context).currentUser
        if (currentUser != null) {
            val firestore = com.example.dutype.di.firestoreFromHilt(context)
            try {
                // P1 FIX: Use cached Firestore data first to avoid blocking on network
                val userDoc = firestore
                    .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                    .document(currentUser.uid)
                    .get(com.google.firebase.firestore.Source.CACHE)
                    .await()
                
                if (userDoc.exists()) {
                    currentTrustTier = userDoc.getString("trustTier") ?: "VERIFIED"
                    isGstVerified = userDoc.getBoolean("isGstVerified") ?: false
                    completedJobsCount = (userDoc.getLong("completedJobsCount") ?: 0L).toInt()
                    averageRating = (userDoc.getDouble("averageRating") ?: 0.0).toFloat()
                }
            } catch (e: Exception) {
                // Cache miss — fallback to server
                try {
                    val userDoc = firestore
                        .collection(com.example.dutype.firestore.FirestoreCollections.USERS)
                        .document(currentUser.uid)
                        .get()
                        .await()
                    
                    if (userDoc.exists()) {
                        currentTrustTier = userDoc.getString("trustTier") ?: "VERIFIED"
                        isGstVerified = userDoc.getBoolean("isGstVerified") ?: false
                        completedJobsCount = (userDoc.getLong("completedJobsCount") ?: 0L).toInt()
                        averageRating = (userDoc.getDouble("averageRating") ?: 0.0).toFloat()
                    }
                } catch (_: Exception) {
                    // Use defaults
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Trust Badges",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = screenBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Current Badge Section
            item {
                CurrentBadgeCard(
                    trustTier = currentTrustTier,
                    completedJobsCount = completedJobsCount,
                    averageRating = averageRating,
                    isGstVerified = isGstVerified
                )
            }
            
            // How Badges Work Section
            item {
                Text(
                    text = "How Trust Badges Work",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
            
            // Badge Explanation Cards
            item {
                BadgeExplanationCard(
                    tier = EmployerTrustTier.VERIFIED,
                    isCurrentTier = currentTrustTier == "VERIFIED",
                    requirements = listOf(
                        "Phone number verified via OTP",
                        "Selfie uploaded during profile setup",
                        "Profile information completed"
                    ),
                    benefits = listOf(
                        "Post unlimited jobs",
                        "Contact workers directly",
                        "Basic trust indicator for workers"
                    )
                )
            }
            
            item {
                BadgeExplanationCard(
                    tier = EmployerTrustTier.TRUSTED,
                    isCurrentTier = currentTrustTier == "TRUSTED",
                    requirements = listOf(
                        "Complete 10+ jobs successfully",
                        "Maintain 4.0+ average rating",
                        "Receive at least 5 ratings from workers"
                    ),
                    benefits = listOf(
                        "Higher visibility in job listings",
                        "Workers trust you more",
                        "Priority support from DutyPe"
                    ),
                    progress = if (currentTrustTier == "VERIFIED") {
                        "Progress: $completedJobsCount/10 jobs, ${String.format("%.1f", averageRating)}/4.0 rating"
                    } else null
                )
            }
            
            item {
                BadgeExplanationCard(
                    tier = EmployerTrustTier.BUSINESS,
                    isCurrentTier = currentTrustTier == "BUSINESS",
                    requirements = listOf(
                        "Valid GST number provided",
                        "GST format verified",
                        "Registered business entity"
                    ),
                    benefits = listOf(
                        "Highest trust level",
                        "Business verification badge",
                        "Premium visibility for workers",
                        "Access to bulk hiring features"
                    ),
                    actionText = if (!isGstVerified && currentTrustTier != "BUSINESS") "Add GST Number" else null,
                    onActionClick = {
                        // Navigate to company details to add GST
                        navController.navigate("employer_company_details")
                    }
                )
            }
            
            // Trust Tips Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TrustTipsCard()
            }
            
            // How Workers See Your Badge Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                WorkerPerspectiveCard()
            }
        }
    }
}

@Composable
private fun WorkerPerspectiveCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "How Workers See Your Badge",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Badge visibility explanation
            val badgeEffects = listOf(
                BadgeEffect(
                    badge = "✅ Verified",
                    workerView = "Workers see you completed basic verification",
                    trustLevel = "Medium trust - workers may apply but with caution",
                    color = Color(0xFF3B82F6)
                ),
                BadgeEffect(
                    badge = "⭐ Trusted",
                    workerView = "Workers see you have a proven track record",
                    trustLevel = "High trust - workers prefer your jobs",
                    color = Color(0xFFF59E0B)
                ),
                BadgeEffect(
                    badge = "🏢 Business",
                    workerView = "Workers see you're a registered business",
                    trustLevel = "Highest trust - workers feel most secure",
                    color = Color(0xFF8B5CF6)
                )
            )
            
            badgeEffects.forEach { effect ->
                BadgeEffectItem(effect)
                if (effect != badgeEffects.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFBFDBFE))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key insight
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF3B82F6).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Higher trust badges = More applications from quality workers. Workers actively filter jobs by employer trust level!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF1E40AF),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

private data class BadgeEffect(
    val badge: String,
    val workerView: String,
    val trustLevel: String,
    val color: Color
)

@Composable
private fun BadgeEffectItem(effect: BadgeEffect) {
    Column {
        Text(
            text = effect.badge,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = effect.color
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = effect.workerView,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF374151))
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = effect.trustLevel,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun CurrentBadgeCard(
    trustTier: String,
    completedJobsCount: Int,
    averageRating: Float,
    isGstVerified: Boolean
) {
    val tier = parseTrustTier(trustTier)
    val tierInfo = tier.getDisplayInfo()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(tierInfo.backgroundColor))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Current Badge",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(tierInfo.color).copy(alpha = 0.8f)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TrustBadge(
                tier = tier,
                size = TrustBadgeSize.LARGE,
                showLabel = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = tierInfo.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(tierInfo.color),
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Work,
                    value = completedJobsCount.toString(),
                    label = stringResource(R.string.jobs_done),
                    color = Color(tierInfo.color)
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = if (averageRating > 0) String.format("%.1f", averageRating) else "-",
                    label = stringResource(R.string.rating_label),
                    color = Color(tierInfo.color)
                )
                StatItem(
                    icon = Icons.Default.Verified,
                    value = if (isGstVerified) "Yes" else "No",
                    label = stringResource(R.string.gst),
                    color = Color(tierInfo.color)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun BadgeExplanationCard(
    tier: EmployerTrustTier,
    isCurrentTier: Boolean,
    requirements: List<String>,
    benefits: List<String>,
    progress: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val tierInfo = tier.getDisplayInfo()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTier) Color(tierInfo.backgroundColor) else Color.White
        ),
        border = if (isCurrentTier) null else androidx.compose.foundation.BorderStroke(
            1.dp, Color(0xFFE5E7EB)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TrustBadge(
                    tier = tier,
                    size = TrustBadgeSize.MEDIUM,
                    showLabel = true
                )
                
                if (isCurrentTier) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(tierInfo.color).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(tierInfo.color),
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Requirements
            Text(
                text = "Requirements",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            requirements.forEach { req ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(tierInfo.color),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = req,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4B5563)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Benefits
            Text(
                text = "Benefits",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4B5563)
                        )
                    )
                }
            }
            
            // Progress indicator
            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            // Action button
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(tierInfo.color)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(actionText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TrustTipsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tips to Build Trust",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF065F46)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val tips = listOf(
                "Pay workers on time to get good ratings",
                "Respond quickly to applications",
                "Provide clear job descriptions",
                "Be respectful and professional",
                "Complete jobs as promised"
            )
            
            tips.forEach { tip ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "💡",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF065F46)
                        )
                    )
                }
            }
        }
    }
}
