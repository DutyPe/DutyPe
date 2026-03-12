package com.example.dutype.employer.screens.profile

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.TrustBadge
import com.example.dutype.components.TrustBadgeSize
import com.example.dutype.models.parseTrustTier
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

// Premium colors for the visiting card
private val CardBackgroundColor = Color.White
private val CardBorderColor = Color(0xFFE5E7EB)
private val PrimaryTextColor = Color(0xFF1F2937)
private val SecondaryTextColor = Color(0xFF6B7280)
private val AccentBlue = Color(0xFF3B82F6)
private val ChipBackgroundColor = Color(0xFFF3F4F6)

@Composable
fun EmployerDigitalVisitingCardScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {},
    onBottomBarVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    
    // Employer profile data
    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var industry by remember { mutableStateOf("") }
    var companySize by remember { mutableStateOf("") }
    var trustTier by remember { mutableStateOf("NEW") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var postedJobsCount by remember { mutableStateOf(0) }
    var companyRating by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var isSharing by remember { mutableStateOf(false) }
    
    // Profile completion check
    var isProfileComplete by remember { mutableStateOf(false) }
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    
    // Load employer profile data
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        onBottomBarVisibilityChange(false)
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    companyName = userDoc.getString("companyName") 
                        ?: userDoc.getString("fullName") 
                        ?: ""
                    companyPhone = userDoc.getString("phone") 
                        ?: userDoc.getString("phoneNumber") 
                        ?: ""
                    industry = userDoc.getString("industry") ?: ""
                    companySize = userDoc.getString("companySize") ?: ""
                    trustTier = userDoc.getString("trustTier") ?: "NEW"
                    profileImageUrl = userDoc.getString("profileImageUrl")
                    postedJobsCount = (userDoc.getLong("postedJobsCount") ?: 0).toInt()
                    companyRating = (userDoc.getDouble("companyRating") ?: 0.0).toFloat()
                    
                    // Fallback: Get phone from Firebase Auth if not in Firestore
                    if (companyPhone.isBlank()) {
                        currentUser.phoneNumber?.let { authPhone ->
                            companyPhone = authPhone
                            Timber.d("📱 Employer Card - Phone from Auth: $companyPhone")
                        }
                    }
                    
                    // Check profile completion - LIGHTWEIGHT approach
                    profileCompletionPercentage = (userDoc.getLong("profileCompletionPercentage") ?: 0L).toInt()
                    
                    if (profileCompletionPercentage == 0) {
                        // Quick check: profile is complete if essential fields are filled
                        val hasName = companyName.isNotBlank()
                        val hasPhone = companyPhone.isNotBlank()
                        isProfileComplete = hasName && hasPhone
                    } else {
                        // Use stored percentage - 80% is the threshold
                        isProfileComplete = profileCompletionPercentage >= 80
                    }
                    
                    Timber.d("📱 Employer Card - Data loaded: company=$companyName, phone=$companyPhone, industry=$industry, size=$companySize, profileComplete=$isProfileComplete ($profileCompletionPercentage%)")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading employer profile for visiting card")
                // On error, mark as incomplete to show the setup screen
                isProfileComplete = false
            }
        } else {
            // No user logged in - mark as incomplete
            isProfileComplete = false
        }
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "My Business Card",
            onBackClick = { navController.popBackStack() },
            backgroundColor = EmployerColors.CardBackground
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (!isProfileComplete) {
            // Show profile incomplete message
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
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
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
                            text = "Your business card will be available once you complete your profile. Add your company name and phone number to create your professional identity.",
                            style = AppTypography.bodyMedium.copy(
                                color = SecondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        )
                        
                        if (profileCompletionPercentage > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                navController.navigate(Routes.EMPLOYER_PROFILE_SETUP)
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
                Text(
                    text = "Your Business Identity 🏢",
                    style = AppTypography.pageTitle.copy(color = PrimaryTextColor)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // The Business Card
                EmployerVisitingCard(
                    companyName = companyName,
                    phone = companyPhone,
                    industry = industry,
                    companySize = companySize,
                    trustTier = trustTier,
                    profileImageUrl = profileImageUrl,
                    postedJobsCount = postedJobsCount,
                    companyRating = companyRating
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Share Your Card",
                    style = AppTypography.sectionHeader.copy(color = PrimaryTextColor)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Share Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            scope.launch {
                                isSharing = true
                                shareEmployerCard(
                                    context = context,
                                    companyName = companyName,
                                    trustTier = trustTier,
                                    platform = "whatsapp"
                                )
                                isSharing = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
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
                            shareEmployerCard(
                                context = context,
                                companyName = companyName,
                                trustTier = trustTier,
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
                
                // Tips Section
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
                            text = "• Share this card on WhatsApp Status to reach more workers!\n" +
                                   "• Share on WhatsApp Status daily for more visibility\n" +
                                   "• Send to local communities & worker groups\n" +
                                   "• The more you share, the more quality applicants you get!",
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
fun EmployerVisitingCard(
    companyName: String,
    phone: String,
    industry: String,
    companySize: String,
    trustTier: String,
    profileImageUrl: String?,
    postedJobsCount: Int,
    companyRating: Float,
    modifier: Modifier = Modifier
) {
    // Safe display values with fallbacks
    val safeCompanyName = companyName.ifBlank { "Company" }
    val displayPhone = if (phone.isNotBlank()) {
        if (phone.startsWith("+91")) phone else "+91 $phone"
    } else ""
    val safeTrustTier = trustTier.ifBlank { "NEW" }
    val displayIndustry = industry.ifBlank { "Not specified" }
    val displayCompanySize = companySize.ifBlank { "Not specified" }
    
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
            // Top Section: Logo + Company Name + Trust Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Company Logo
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
                            contentDescription = "Company Logo",
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
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = SecondaryTextColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Company Name and Trust Badge
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = safeCompanyName,
                        style = AppTypography.pageTitle.copy(color = PrimaryTextColor),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Trust Badge
                    TrustBadge(
                        tier = parseTrustTier(safeTrustTier),
                        size = TrustBadgeSize.SMALL,
                        showLabel = true
                    )
                    
                    // Industry and Company Size
                    if (industry.isNotBlank() || companySize.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (industry.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = displayIndustry,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF3B82F6),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                            if (companySize.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFF0FDF4), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = displayCompanySize,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
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
                // Phone Number
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
                    if (postedJobsCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = SecondaryTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$postedJobsCount jobs",
                                style = AppTypography.labelMedium.copy(color = SecondaryTextColor)
                            )
                        }
                    }
                    
                    if (companyRating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", companyRating),
                                style = AppTypography.labelMedium.copy(color = SecondaryTextColor)
                            )
                        }
                    }
                }
            }
            
        }
    }
}

private fun shareEmployerCard(
    context: Context,
    companyName: String,
    trustTier: String,
    platform: String
) {
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"
    
    // Safe company name with fallback
    val safeCompanyName = companyName.ifBlank { "Our Company" }
    
    // Get current user ID for employer profile deep link
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val employerProfileLink = if (currentUserId != null) {
        "https://dutype.in/employer/$currentUserId"
    } else {
        playStoreUrl
    }
    
    val trustBadgeText = when (trustTier) {
        "GOLD" -> "🏆 Gold Verified"
        "SILVER" -> "🥈 Silver Verified"
        "BRONZE" -> "🥉 Bronze Verified"
        else -> "✅ Verified"
    }
    
    val shareText = """
🏢 *$safeCompanyName*
$trustBadgeText Employer

Looking for skilled workers? We're hiring!

👉 View our company profile & apply:
$employerProfileLink

📲 Download DutyPe App:
$playStoreUrl

#DutyPe #Hiring #Jobs #LocalJobs
    """.trimIndent()
    
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        
        when (platform) {
            "whatsapp" -> setPackage("com.whatsapp")
        }
    }
    
    try {
        if (platform == "general") {
            context.startActivity(Intent.createChooser(intent, "Share your business card"))
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
        context.startActivity(Intent.createChooser(fallbackIntent, "Share your business card"))
    }
}
