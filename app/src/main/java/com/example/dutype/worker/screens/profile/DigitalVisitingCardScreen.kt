package com.example.dutype.worker.screens.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

// Premium colors for the visiting card - White theme for consistency
private val CardBackgroundColor = Color.White
private val CardBorderColor = Color(0xFFE5E7EB)
private val PrimaryTextColor = Color(0xFF1F2937)
private val SecondaryTextColor = Color(0xFF6B7280)
private val AccentBlue = Color(0xFF3B82F6)
private val LightBlueBackground = Color(0xFFEDF8FF)
private val VerifiedGreen = Color(0xFF10B981)
private val ChipBackgroundColor = Color(0xFFF3F4F6)

@Composable
fun DigitalVisitingCardScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}  // Hide bottom bar
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Worker profile data
    var workerName by remember { mutableStateOf("") }
    var workerPhone by remember { mutableStateOf("") }
    var workerSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var workerExperience by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isVerified by remember { mutableStateOf(true) }
    var completedJobs by remember { mutableStateOf(0) }
    var rating by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isSharing by remember { mutableStateOf(false) }
    
    // LIGHTWEIGHT: Profile completion check using metadata approach
    var isProfileComplete by remember { mutableStateOf(false) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    
    // Load worker profile data
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        onBottomBarVisibilityChange(false)  // Hide bottom bar on this screen
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    workerName = userDoc.getString("fullName") ?: userDoc.getString("name") ?: "Worker"
                    // Check both "phone" and "phoneNumber" fields for backward compatibility
                    workerPhone = userDoc.getString("phone") 
                        ?: userDoc.getString("phoneNumber") 
                        ?: ""
                    Timber.d("📱 Visiting Card - Phone loaded: $workerPhone")
                    val skillsString = userDoc.getString("skills") ?: ""
                    workerSkills = skillsString.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
                    workerExperience = userDoc.getString("experience") ?: ""
                    profileImageUrl = userDoc.getString("profileImageUrl")
                    isVerified = userDoc.getBoolean("isVerified") ?: true
                    completedJobs = (userDoc.getLong("completedJobsCount") ?: 0).toInt()
                    rating = (userDoc.getDouble("averageRating") ?: 0.0).toFloat()
                    
                    // LIGHTWEIGHT: Check profile completion from stored percentage (metadata approach)
                    // This avoids heavy recalculation - just read the stored value
                    profileCompletionPercentage = (userDoc.getLong("profileCompletionPercentage") ?: 0L).toInt()
                    
                    // If no stored percentage, do a quick lightweight check
                    if (profileCompletionPercentage == 0) {
                        // Quick check: profile is complete if essential fields are filled
                        val hasName = workerName.isNotBlank()
                        val hasPhone = workerPhone.isNotBlank()
                        val hasSkills = workerSkills.isNotEmpty()
                        isProfileComplete = hasName && hasPhone && hasSkills
                    } else {
                        // Use stored percentage - 80% is the threshold
                        isProfileComplete = profileCompletionPercentage >= 80
                    }
                    
                    Timber.d("📱 Visiting Card - Data loaded: name=$workerName, phone=$workerPhone, skills=$workerSkills, profileComplete=$isProfileComplete ($profileCompletionPercentage%)")
                }
                
                // Fallback: If phone is still empty, try to get from Firebase Auth
                if (workerPhone.isBlank()) {
                    currentUser.phoneNumber?.let { authPhone ->
                        workerPhone = authPhone
                        Timber.d("📱 Visiting Card - Phone from Auth: $workerPhone")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading worker profile for visiting card")
            }
        }
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "My Visiting Card",
            onBackClick = { navController.popBackStack() },
            backgroundColor = WorkerColors.CardBackground
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (!isProfileComplete) {
            // Show profile incomplete message instead of visiting card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Text(
                            text = "Complete Your Profile",
                            style = AppTypography.pageTitle.copy(color = PrimaryTextColor)
                        )
                        
                        Text(
                            text = "Your visiting card will be available once you complete your profile. Add your name, phone number, and skills to create your professional identity.",
                            style = AppTypography.bodyMedium.copy(
                                color = SecondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        )
                        
                        if (profileCompletionPercentage > 0) {
                            // Show progress
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Profile: $profileCompletionPercentage% complete",
                                    style = AppTypography.labelMedium.copy(color = SecondaryTextColor)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { profileCompletionPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = AccentBlue,
                                    trackColor = Color(0xFFE5E7EB)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                navController.navigate(Routes.PROFILE_SETUP)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Complete Profile",
                                style = AppTypography.buttonMedium.copy(color = Color.White)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title only - subtitle moved to Pro Tips
                Text(
                    text = "Your Professional Identity 🌟",
                    style = AppTypography.pageTitle.copy(color = PrimaryTextColor)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // The Visiting Card
                DigitalVisitingCard(
                    name = workerName,
                    phone = workerPhone,
                    skills = workerSkills,
                    experience = workerExperience,
                    profileImageUrl = profileImageUrl,
                    isVerified = isVerified,
                    completedJobs = completedJobs,
                    rating = rating
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Share Buttons
                Text(
                    text = "Share Your Card",
                    style = AppTypography.sectionHeader.copy(color = PrimaryTextColor)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Two Share Buttons - WhatsApp and General Share (half-half)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            scope.launch {
                                isSharing = true
                                shareVisitingCard(
                                    context = context,
                                    name = workerName,
                                    skills = workerSkills,
                                    platform = "whatsapp"
                                )
                                isSharing = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WhatsApp Green
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isSharing
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WhatsApp",
                                style = AppTypography.buttonMedium.copy(color = Color.White)
                            )
                        }
                    }
                    
                    // General Share Button
                    OutlinedButton(
                        onClick = {
                            shareVisitingCard(
                                context = context,
                                name = workerName,
                                skills = workerSkills,
                                platform = "general"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = PrimaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share",
                            style = AppTypography.buttonMedium.copy(color = PrimaryTextColor)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Tips Section - White background with black text
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro Tips",
                                style = AppTypography.sectionHeader.copy(color = PrimaryTextColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Share this card on WhatsApp Status to get more job offers!\n" +
                                   "• Share on WhatsApp Status daily for more visibility\n" +
                                   "• Send to local shop owners & businesses\n" +
                                   "• The more you share, the more jobs you get!",
                            style = AppTypography.bodySmall.copy(
                                color = SecondaryTextColor,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun DigitalVisitingCard(
    name: String,
    phone: String,
    skills: List<String>,
    experience: String,
    profileImageUrl: String?,
    isVerified: Boolean,
    completedJobs: Int,
    rating: Float,
    modifier: Modifier = Modifier
) {
    // Get primary skill for title
    val primarySkill = skills.firstOrNull() ?: "Professional Worker"
    
    // Format phone for display
    val displayPhone = if (phone.isNotBlank()) {
        if (phone.startsWith("+91")) phone else "+91 $phone"
    } else ""
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Section: Photo + Name + Verified Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Profile Photo with black border
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(
                            width = 2.dp,
                            color = PrimaryTextColor,
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    if (profileImageUrl != null) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = profileImageUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(ChipBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SecondaryTextColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Name and Title
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name.ifBlank { "Worker" },
                        style = AppTypography.pageTitle.copy(color = PrimaryTextColor),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = primarySkill,
                        style = AppTypography.bodyMedium.copy(color = SecondaryTextColor, fontWeight = FontWeight.Medium),
                        maxLines = 1
                    )
                    
                    // Verified Badge
                    if (isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(VerifiedGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = VerifiedGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DutyPe Verified",
                                style = AppTypography.labelSmall.copy(color = VerifiedGreen, fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
            
            // Skills Section - Black chips
            if (skills.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.take(3).forEach { skill ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = skill,
                                style = AppTypography.labelMedium.copy(color = PrimaryTextColor)
                            )
                        }
                    }
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CardBorderColor)
            )
            
            // Bottom Section: Phone + Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phone Number - gray background like chips
                if (displayPhone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(ChipBackgroundColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = SecondaryTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayPhone,
                            style = AppTypography.bodyMedium.copy(color = PrimaryTextColor, fontWeight = FontWeight.SemiBold)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                // Stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (completedJobs > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkHistory,
                                contentDescription = null,
                                tint = SecondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$completedJobs jobs",
                                style = AppTypography.labelMedium.copy(color = SecondaryTextColor)
                            )
                        }
                    }
                    
                    if (rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                style = AppTypography.labelMedium.copy(color = SecondaryTextColor)
                            )
                        }
                    }
                }
            }
            
        }
    }
}

private fun shareVisitingCard(
    context: Context,
    name: String,
    skills: List<String>,
    platform: String
) {
    val skillsText = if (skills.isNotEmpty()) skills.joinToString(", ") else "Professional Worker"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    
    // Get current user ID for worker profile deep link
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val workerProfileLink = if (currentUserId != null) {
        com.example.dutype.utils.DeepLinkHandler.generateWorkerWebLink(currentUserId)
    } else {
        playStoreUrl
    }
    
    val shareText = """
🌟 *$name*
Professional $skillsText

✅ Verified by DutyPe

👉 View my profile & book me:
$workerProfileLink

📲 Download DutyPe App:
$playStoreUrl

#DutyPe #HireMe #LocalJobs #Professional
    """.trimIndent()
    
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        
        when (platform) {
            "whatsapp" -> setPackage("com.whatsapp")
            "instagram" -> setPackage("com.instagram.android")
        }
    }
    
    try {
        if (platform == "general") {
            context.startActivity(Intent.createChooser(intent, "Share your visiting card"))
        } else {
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback to general share
        val fallbackIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Share your visiting card"))
    }
}
