package com.example.dutype.employer.screens.profilescreen

import com.dutype.app.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.models.EmployerSubscription
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.ui.theme.LocalRoleColors
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

private val EmpNavy = Color(0xFF0F172A)
private val EmpCobalt = Color(0xFF2563EB)
private val EmpBorder = Color(0xFFE2E8F0)
private val EmpDivider = Color(0xFFF1F5F9)
private val EmpSlate = Color(0xFF94A3B8)
private val EmpAmber = Color(0xFFF59E0B)
private const val EmpReferAmount = 100

private class EmpProfileUi(
    val businessName: String,
    val contact: String,
    val phone: String,
    val category: String,
    val gstVerified: Boolean,
    val rating: Double,
    val imageModel: String?,
    val uploading: Boolean,
    val activeJobs: Int,
    val applicants: Int,
    val locCount: Int,
    val city: String,
    val pct: Int,
    val hint: String,
    val pill: String?,
    val isLoggedIn: Boolean
)

private class EmpProfileActions(
    val onHelp: () -> Unit,
    val onEdit: () -> Unit,
    val onAvatar: () -> Unit,
    val onLogin: () -> Unit,
    val onCompany: () -> Unit,
    val onLocations: () -> Unit,
    val onSubscription: () -> Unit,
    val onHistory: () -> Unit,
    val onRefer: () -> Unit,
    val onSwitch: () -> Unit,
    val onLogout: () -> Unit
)

private fun empInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "B"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> ("" + parts[0].first() + parts[1].first()).uppercase()
    }
}

private fun empFormatPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length >= 10) {
        val d = digits.takeLast(10)
        "+91 " + d.take(5) + " " + d.drop(5)
    } else raw
}

private fun empCityFrom(address: String): String {
    val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() && it.none { c -> c.isDigit() } }
    return parts.takeLast(2).joinToString(", ")
}

private fun empPillText(sub: EmployerSubscription, planName: String?): String? {
    if (!sub.isActive) return null
    var name: String = planName ?: ""
    if (name.isBlank()) {
        name = if (sub.status == "TRIAL") "Trial" else sub.planId.replaceFirstChar { it.uppercase() }
    }
    if (name.isBlank()) return null
    if (sub.expiryDate > 0L) {
        val left = Math.ceil((sub.expiryDate - System.currentTimeMillis()) / 86400000.0).toInt()
        if (left >= 0) return name + " (" + left + "d left)"
    }
    return name
}

@Composable
private fun EmpTopBar(onHelp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Account",
            color = EmpNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, EmpCobalt, RoundedCornerShape(16.dp))
                .clickable(onClick = onHelp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Help Desk",
                color = EmpCobalt,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmpChip(text: String, bg: Color, fg: Color, icon: ImageVector) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun EmpAvatar(ui: EmpProfileUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(shape)
            .border(2.dp, EmpAmber, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (ui.uploading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else if (!ui.imageModel.isNullOrBlank()) {
            com.example.dutype.components.OptimizedProfileImage(
                imageUrl = ui.imageModel,
                contentDescription = "Company Logo",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = empInitials(ui.businessName),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmpHeroTexts(ui: EmpProfileUi, onLogin: () -> Unit, modifier: Modifier) {
    Column(modifier = modifier) {
        if (ui.isLoggedIn) {
            Text(
                text = ui.businessName.ifEmpty { "Your Company" },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (ui.contact.isNotBlank()) {
                Text(
                    text = ui.contact + " (Owner)",
                    color = EmpSlate,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (ui.phone.isNotBlank()) {
                Text(
                    text = ui.phone,
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        } else {
            Text(
                text = "Log in / Sign up",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onLogin)
            )
            Text(
                text = "View and update your profile data",
                color = EmpSlate,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun EmpHeroCard(ui: EmpProfileUi, actions: EmpProfileActions) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EmpNavy)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmpAvatar(ui = ui, onClick = actions.onAvatar)
            Spacer(modifier = Modifier.width(12.dp))
            EmpHeroTexts(ui = ui, onLogin = actions.onLogin, modifier = Modifier.weight(1f))
            if (ui.isLoggedIn) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.Top)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                        .clickable(onClick = actions.onEdit)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
        if (ui.isLoggedIn && (ui.gstVerified || ui.rating > 0.0 || ui.category.isNotBlank())) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (ui.gstVerified) {
                    EmpChip("GST Verified", Color(0xFF064E3B), Color(0xFF34D399), Icons.Filled.Check)
                }
                if (ui.rating > 0.0) {
                    EmpChip(String.format(java.util.Locale.US, "%.1f Employer Score", ui.rating), Color(0xFF451A03), Color(0xFFFBBF24), Icons.Filled.Star)
                }
                if (ui.category.isNotBlank()) {
                    EmpChip(ui.category, Color(0xFF1E3A8A), Color(0xFF60A5FA), Icons.Filled.Business)
                }
            }
        }
    }
}

@Composable
private fun EmpSnapshotCard(
    iconRes: Int,
    title: String,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White)
            .border(1.dp, EmpBorder, shape)
            .padding(14.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = EmpCobalt,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = title,
            color = EmpNavy,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Box(modifier = Modifier.padding(top = 3.dp)) { content() }
    }
}

@Composable
private fun EmpSnapshotRow(ui: EmpProfileUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EmpSnapshotCard(
            iconRes = R.drawable.ic_emp_briefcase,
            title = ui.activeJobs.toString() + (if (ui.activeJobs == 1) " Active Job" else " Active Jobs"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = ui.applicants.toString() + " Applicants waiting",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        EmpSnapshotCard(
            iconRes = R.drawable.ic_emp_pin,
            title = ui.locCount.toString() + (if (ui.locCount == 1) " Work Location" else " Work Locations"),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                text = ui.city,
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmpCompletenessCard(ui: EmpProfileUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, EmpBorder, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_emp_bolt),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Profile " + ui.pct + "% Complete · " + ui.hint,
            color = EmpNavy,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Complete →",
            color = EmpCobalt,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmpMenuRow(
    iconRes: Int,
    title: String,
    pill: String?,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = EmpNavy,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!pill.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = pill,
                        color = EmpCobalt,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = "›", color = EmpSlate, fontSize = 16.sp)
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EmpDivider)
            )
        }
    }
}

@Composable
private fun EmpMenuCard(ui: EmpProfileUi, actions: EmpProfileActions) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, EmpBorder, shape)
    ) {
        EmpMenuRow(R.drawable.ic_emp_building, "Company Details & Photos", null, true, actions.onCompany)
        EmpMenuRow(R.drawable.ic_emp_pin, "Work Locations & Job Sites", null, true, actions.onLocations)
        EmpMenuRow(R.drawable.ic_emp_card, "Subscription & Credits", ui.pill, true, actions.onSubscription)
        EmpMenuRow(R.drawable.ic_emp_archive, "Hiring History & Closed Posts", null, true, actions.onHistory)
        EmpMenuRow(R.drawable.ic_emp_gift, "Refer an Employer (Earn ₹" + EmpReferAmount + ")", null, true, actions.onRefer)
        EmpMenuRow(R.drawable.ic_emp_headset, "Help & Support", null, false, actions.onHelp)
    }
}

@Composable
private fun EmpAccountRow(text: String, color: Color, showDivider: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EmpDivider)
            )
        }
    }
}

@Composable
private fun EmpAccountCard(ui: EmpProfileUi, actions: EmpProfileActions) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, EmpBorder, shape)
    ) {
        EmpAccountRow("Switch to Worker Mode", EmpCobalt, ui.isLoggedIn, actions.onSwitch)
        if (ui.isLoggedIn) {
            EmpAccountRow("Log Out", Color(0xFFEF4444), false, actions.onLogout)
        }
    }
}

@Composable
private fun EmpProfileBody(ui: EmpProfileUi, actions: EmpProfileActions, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EmpHeroCard(ui = ui, actions = actions)
        if (ui.isLoggedIn) {
            EmpSnapshotRow(ui = ui)
            if (ui.pct < 100) {
                EmpCompletenessCard(ui = ui, onClick = actions.onCompany)
            }
        }
        EmpMenuCard(ui = ui, actions = actions)
        EmpAccountCard(ui = ui, actions = actions)
        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
fun EmployerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: ((Color) -> Unit)? = null
) {
    val screenBg = com.example.dutype.ui.theme.EmployerColors.ScreenBackground
    LaunchedEffect(screenBg) {
        onStatusBarColorChange?.invoke(screenBg)
    }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val subscriptionViewModel: com.example.dutype.viewmodels.SubscriptionViewModel = hiltViewModel()
    val subState by subscriptionViewModel.activeSubscription.collectAsState()
    val plans by subscriptionViewModel.plans.collectAsState()
    val context = LocalContext.current
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager

    // LIGHTWEIGHT PROFILE: Use metadata for basic profile info (name, phone, image)
    val userStats by profileCompletionViewModel.metadataManager.userMetadata.userStats.collectAsState()
    val employerStats by profileCompletionViewModel.metadataManager.userMetadata.employerStats.collectAsState()

    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var employerType by remember { mutableStateOf("COMPANY") }
    var profCompany by remember { mutableStateOf("") }
    var profContact by remember { mutableStateOf("") }
    var profIndustry by remember { mutableStateOf("") }
    var profGstin by remember { mutableStateOf("") }
    var profAddress by remember { mutableStateOf("") }
    var profRating by remember { mutableStateOf(0.0) }
    var profPhotoUrl by remember { mutableStateOf("") }
    var storedPct by remember { mutableStateOf(0) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLanguageBottomSheet by remember { mutableStateOf(false) }
    // var showThemeBottomSheet by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingMenuAction by remember { mutableStateOf<String?>(null) }

    // LIGHTWEIGHT: Load only basic profile info using metadata
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            try {
                // LIGHTWEIGHT: Only load basic profile (name, phone, image) - no heavy stats
                profileCompletionViewModel.metadataManager.userMetadata.loadBasicProfile()

                // Use metadata for profile image URL
                profileImageUrl = userStats.profileImageUrl.ifEmpty { null }

                val employerData = profileCompletionViewModel.getEmployerProfileData(currentUser.uid)
                employerData.onSuccess { data ->
                    employerType = data["employerType"] as? String ?: "COMPANY"
                    profCompany = data["companyName"] as? String ?: ""
                    profContact = data["fullName"] as? String ?: ""
                    profIndustry = data["industry"] as? String ?: ""
                    profGstin = data["gstin"] as? String ?: ""
                    profAddress = data["businessAddress"] as? String ?: ""
                    profPhotoUrl = data["profileImageUrl"] as? String ?: ""
                    profRating = (data["rating"] as? Number)?.toDouble()
                        ?: (data["averageRating"] as? Number)?.toDouble() ?: 0.0
                }
                storedPct = try {
                    profileCompletionViewModel.getCompletionPercentage(com.example.dutype.models.UserRole.EMPLOYER)
                } catch (e: Exception) {
                    0
                }

                Timber.i("Employer profile (lightweight) - Name: ${userStats.fullName}, Phone: ${userStats.phone}, Type: $employerType")
            } catch (e: Exception) {
                Timber.e("Error loading lightweight profile: ${e.message}")
            }
        }
        isLoadingProfile = false
    }

    // Update company name and phone from metadata (lightweight)
    // Also get phone from Firebase Auth as fallback for new users
    LaunchedEffect(userStats) {
        companyName = userStats.companyName.ifBlank { userStats.fullName }
        // Get phone from metadata, fallback to Firebase Auth
        val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
        companyPhone = userStats.phone.ifBlank { authPhone }
        if (userStats.profileImageUrl.isNotBlank() && profileImageUrl == null) {
            profileImageUrl = userStats.profileImageUrl
        }
    }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                profileImageUri = selectedUri
                isUploadingImage = true
                scope.launch {
                    try {
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "employer")
                            uploadResult.fold(
                                onSuccess = { imageUrl ->
                                    profileImageUrl = imageUrl
                                    profPhotoUrl = imageUrl
                                    android.widget.Toast.makeText(context, context.getString(R.string.profile_photo_updated), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { exception ->
                                    profileImageUri = null
                                    // Show user-friendly error message
                                    val errorMessage = when {
                                        exception.message?.contains("quota", ignoreCase = true) == true ||
                                        exception.message?.contains("billing", ignoreCase = true) == true ||
                                        exception.message?.contains("storage", ignoreCase = true) == true ->
                                            context.getString(R.string.photo_upload_unavailable)
                                        exception.message?.contains("network", ignoreCase = true) == true ->
                                            context.getString(R.string.network_error_check_connection)
                                        else -> context.getString(R.string.photo_upload_failed)
                                    }
                                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        profileImageUri = null
                        android.widget.Toast.makeText(context, context.getString(R.string.photo_upload_failed), android.widget.Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploadingImage = false
                    }
                }
            }
        }

    if (isLoadingProfile) {
        ProfileShimmer()
    } else {
        val isLoggedIn = currentUserId.isNotEmpty()
        val nav = localNavController ?: rootNavController

        fun go(route: String, pendingKey: String?) {
            if (!isLoggedIn) {
                pendingMenuAction = pendingKey
                showLoginBottomSheet = true
                return
            }
            try {
                nav.navigate(route)
            } catch (e: Exception) {
                Timber.e(e, "Error navigating to $route")
                android.widget.Toast.makeText(context, "Unable to open. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Profile completeness: stored service percentage, else computed from loaded data
        val displayName = profCompany.ifBlank { companyName }
        val photoUrl = profPhotoUrl.ifBlank { profileImageUrl ?: "" }
        val missingHints = ArrayList<String>()
        if (displayName.isBlank()) missingHints.add("Add business name")
        if (companyPhone.isBlank()) missingHints.add("Add phone number")
        if (profIndustry.isBlank()) missingHints.add("Add business category")
        if (profAddress.isBlank()) missingHints.add("Add business address")
        if (photoUrl.isBlank()) missingHints.add("Add shop photo")
        val computedPct = (5 - missingHints.size) * 100 / 5
        val pct = if (storedPct in 1..100) storedPct else computedPct
        val hint = if (missingHints.isNotEmpty()) missingHints[0] else "Complete your profile"

        val planName = plans.firstOrNull { it.id == subState.planId }?.name
        val contact = if (profContact.isNotBlank() && !profContact.equals(displayName, ignoreCase = true)) profContact else ""

        val ui = EmpProfileUi(
            businessName = displayName,
            contact = contact,
            phone = empFormatPhone(companyPhone),
            category = profIndustry,
            gstVerified = profGstin.isNotBlank(),
            rating = profRating,
            imageModel = if (profileImageUri != null) profileImageUri.toString() else photoUrl.ifBlank { null },
            uploading = isUploadingImage,
            activeJobs = employerStats.activeJobs,
            applicants = (employerStats.totalApplicationsReceived - employerStats.totalHires).coerceAtLeast(0),
            locCount = if (profAddress.isNotBlank()) 1 else 0,
            city = if (profAddress.isNotBlank()) empCityFrom(profAddress) else "Add a work location",
            pct = pct,
            hint = hint,
            pill = empPillText(subState, planName),
            isLoggedIn = isLoggedIn
        )

        val actions = EmpProfileActions(
            onHelp = { go(Routes.EMPLOYER_HELP, null) },
            onEdit = { go(Routes.EMPLOYER_COMPANY_DETAILS, "profile") },
            onAvatar = {
                if (isLoggedIn) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    pendingMenuAction = "profile"
                    showLoginBottomSheet = true
                }
            },
            onLogin = { rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER") },
            onCompany = { go(Routes.EMPLOYER_COMPANY_DETAILS, "profile") },
            onLocations = { go(Routes.EMPLOYER_MANAGE_ADDRESSES, "locations") },
            onSubscription = { go(Routes.EMPLOYER_SUBSCRIPTION, null) },
            onHistory = { go(Routes.EMPLOYER_HISTORY, "job_posts") },
            onRefer = { go(Routes.EMPLOYER_REFER_EARN, null) },
            onSwitch = {
                rootNavController.navigate(Routes.SELECT_ROLE) {
                    launchSingleTop = true
                }
            },
            onLogout = { showLogoutDialog = true }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                // Solid role background — no gradient.
                .background(LocalRoleColors.current.screenBackground)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)

            EmpTopBar(onHelp = actions.onHelp)
            EmpProfileBody(ui = ui, actions = actions, modifier = Modifier.weight(1f))
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        ProfessionalLogoutDialog(
            isVisible = showLogoutDialog,
            onDismiss = { showLogoutDialog = false },
            navController = rootNavController,
            userRole = "Employer",
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }

    if (showAccountDeletionDialog) {
        com.example.dutype.components.AccountDeletionDialog(
            isVisible = showAccountDeletionDialog,
            onDismiss = { showAccountDeletionDialog = false },
            navController = rootNavController,
            userRole = "Employer",
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }

    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "employer"
    )

    // Language Selection Bottom Sheet
    if (showLanguageBottomSheet) {
        com.example.dutype.components.LanguageSelectionBottomSheet(
            onDismiss = { showLanguageBottomSheet = false }
        )
    }

    // if (showThemeBottomSheet) {
    //     val themeSheetState = androidx.compose.material3.rememberModalBottomSheetState(
    //         skipPartiallyExpanded = true
    //     )
    //     com.example.dutype.components.ThemeModeBottomSheet(
    //         sheetState = themeSheetState,
    //         onDismiss = { showThemeBottomSheet = false },
    //     )
    // }

    // Guest Mode - Login Bottom Sheet
    com.example.dutype.components.LoginBottomSheet(
        isVisible = showLoginBottomSheet,
        onDismiss = {
            showLoginBottomSheet = false
            pendingMenuAction = null
        },
        onLoginSuccess = {
            showLoginBottomSheet = false
            // Execute the pending action after successful login
            when (pendingMenuAction) {
                "profile" -> localNavController?.navigate(Routes.EMPLOYER_COMPANY_DETAILS) ?: rootNavController.navigate(Routes.EMPLOYER_COMPANY_DETAILS)
                "job_posts" -> localNavController?.navigate(Routes.EMPLOYER_HISTORY) ?: rootNavController.navigate(Routes.EMPLOYER_HISTORY)
                "locations" -> runCatching {
                    localNavController?.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES) ?: rootNavController.navigate(Routes.EMPLOYER_MANAGE_ADDRESSES)
                }.onFailure {
                    timber.log.Timber.e(it, "Error navigating to pending locations action")
                }
                // "refer_earn" -> localNavController?.navigate(Routes.EMPLOYER_REFER_EARN) ?: rootNavController.navigate(Routes.EMPLOYER_REFER_EARN)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.EMPLOYER,
        navController = rootNavController,
        title = androidx.compose.ui.res.stringResource(R.string.login_required),
        subtitle = when (pendingMenuAction) {
            "profile" -> androidx.compose.ui.res.stringResource(R.string.login_company_profile)
            "job_posts" -> androidx.compose.ui.res.stringResource(R.string.login_job_posts)
            "locations" -> androidx.compose.ui.res.stringResource(R.string.login_manage_work_locations)
            // "refer_earn" -> stringResource(R.string.login_refer_earn)
            else -> androidx.compose.ui.res.stringResource(R.string.login_access_feature)
        }
    )
}
