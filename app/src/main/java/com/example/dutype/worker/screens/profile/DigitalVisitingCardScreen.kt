package com.example.dutype.worker.screens.profile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.AppTypography
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

// Premium colors for the visiting card
private val CardGradientStart = Color(0xFF1E3A8A) // Dark Blue
private val CardGradientMid = Color(0xFF3B82F6)   // Blue
private val CardGradientEnd = Color(0xFF8B5CF6)   // Purple
private val GoldAccent = Color(0xFFFFD700)
private val VerifiedGreen = Color(0xFF10B981)

@Composable
fun DigitalVisitingCardScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
    
    // Load worker profile data
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        
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
                    
                    Timber.d("📱 Visiting Card - Data loaded: name=$workerName, phone=$workerPhone, skills=$workerSkills")
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
            .background(Color(0xFFF8FAFC))
    ) {
        CommonHeader(
            title = "My Visiting Card",
            onBackClick = { navController.popBackStack() }
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CardGradientMid)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Emotional Hook Text
                Text(
                    text = "Your Professional Identity 🌟",
                    style = AppTypography.sectionHeader.copy(
                        color = CardGradientStart,
                        fontSize = 20.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Share this card on WhatsApp Status to get more job offers!",
                    style = AppTypography.bodyMedium.copy(
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
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
                    style = AppTypography.sectionHeader.copy(color = Color.Black)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // WhatsApp Share Button (Primary)
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
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366) // WhatsApp Green
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSharing
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Share on WhatsApp Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Other Share Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instagram Share
                    OutlinedButton(
                        onClick = {
                            shareVisitingCard(
                                context = context,
                                name = workerName,
                                skills = workerSkills,
                                platform = "instagram"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF))
                            )
                        )
                    ) {
                        Text(
                            text = "Instagram",
                            color = Color(0xFFDD2A7B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // General Share
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
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardGradientMid)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = CardGradientMid,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "More",
                            color = CardGradientMid,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Tips Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pro Tips",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Share on WhatsApp Status daily for more visibility\n" +
                                   "• Add to your Instagram Story\n" +
                                   "• Send to local shop owners & businesses\n" +
                                   "• The more you share, the more jobs you get!",
                            style = AppTypography.bodySmall.copy(
                                color = Color(0xFF92400E),
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
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    
    // Get primary skill for title
    val primarySkill = skills.firstOrNull() ?: "Professional Worker"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f) // Business card ratio
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = CardGradientMid.copy(alpha = 0.3f),
                spotColor = CardGradientEnd.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CardGradientStart, CardGradientMid, CardGradientEnd),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Decorative elements
            // Top-right circle
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            // Bottom-left circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
                    .background(
                        Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
            )
            
            // Shimmer effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            start = Offset(shimmerOffset * 1000f - 300f, 0f),
                            end = Offset(shimmerOffset * 1000f + 100f, 500f)
                        )
                    )
            )
            
            // Card Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Top Row: Photo + Name + Verified Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Profile Photo with gold border
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(
                                width = 3.dp,
                                brush = Brush.linearGradient(
                                    listOf(GoldAccent, Color(0xFFFFA500), GoldAccent)
                                ),
                                shape = CircleShape
                            )
                            .padding(3.dp)
                    ) {
                        if (profileImageUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUrl),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Name and Title
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "Professional $primarySkill",
                            color = GoldAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        
                        // Verified Badge
                        if (isVerified) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        VerifiedGreen.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
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
                                    text = "Verified by DutyPe",
                                    color = VerifiedGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Skills Tags
                if (skills.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        skills.take(3).forEach { skill ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = skill,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (completedJobs > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkHistory,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$completedJobs Jobs Done",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    if (rating > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f Rating", rating),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bottom: Phone + CTA - Stack vertically on small cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Phone - Full width row
                    if (phone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = phone,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                    
                    // CTA - The Viral Hook! - Aligned to end
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "📲 Book me on DutyPe",
                                color = CardGradientStart,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
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
    
    val shareText = """
🌟 *$name*
Professional $skillsText

✅ Verified by DutyPe
📲 Book me on DutyPe App

Download DutyPe: $playStoreUrl

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
