package com.example.dutype.employer.screens.profilescreen

import com.example.dutype.components.cascadeEnter
import com.example.dutype.components.bouncyClickable

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val rating: Double,
    val ratingCount: Int,
    val memberSinceMillis: Long,
    val imageModel: String?,
    val uploading: Boolean,
    val activeJobs: Int,
    val applicants: Int,
    val locCount: Int,
    val city: String,
    val pct: Int,
    val hint: String,
    val pill: String?,
    val isLoggedIn: Boolean,
    val walletBalanceRupees: Long = 0L
)

private class EmpProfileActions(
    val onHelp: () -> Unit,
    val onEdit: () -> Unit,
    val onAvatar: () -> Unit,
    val onLogin: () -> Unit,
    val onCompany: () -> Unit,
    val onMyJobs: () -> Unit = {},
    val onLocations: () -> Unit,
    val onSubscription: () -> Unit,
    val onHistory: () -> Unit,
    val onRefer: () -> Unit,
    val onWallet: () -> Unit = {},
    val onGuidelines: () -> Unit = {},
    val onRateApp: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onPrivacy: () -> Unit = {},
    val onTerms: () -> Unit = {},
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
        name = if (sub.isUnlimitedCampaign) "Unlimited" else sub.planId.substringBefore('_').replaceFirstChar { it.uppercase() }
    }
    if (name.isBlank()) return null
    if (sub.expiresAt > 0L) {
        val left = Math.ceil((sub.expiresAt - System.currentTimeMillis()) / 86400000.0).toInt()
        if (left >= 0) return name + " (" + left + "d left)"
    }
    return name
}

@Composable
private fun EmpTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.emp_profile_account),
            color = EmpNavy.fg(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Same avatar as the worker profile: 72 dp circle with a 3 dp ring, photo or initials. */
@Composable
private fun EmpAvatar(ui: EmpProfileUi, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 54.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFFF8FAFC).bg())
            .border(1.5.dp, Color(0xFFE2E8F0).bd(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            ui.uploading -> com.example.dutype.components.ThreeDotsBouncingLoader(
                dotSize = 5.dp,
                spacing = 3.dp,
                dotColor = EmpCobalt.fg()
            )
            !ui.imageModel.isNullOrBlank() -> com.example.dutype.components.OptimizedProfileImage(
                imageUrl = ui.imageModel,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize()
            )
            ui.businessName.isNotBlank() -> Text(
                text = empInitials(ui.businessName),
                style = profileTextStyle(26.sp, FontWeight.Bold, EmpCobalt.fg())
            )
            else -> Icon(Icons.Filled.Business, contentDescription = null, tint = EmpCobalt.fg(), modifier = Modifier.size(32.dp))
        }
    }
}

/**
 * Flat, centred hero — the worker profile's layout: avatar, name, one subtitle line, then
 * rating · member since. Guests get the same avatar with a log-in button.
 */
@Composable
private fun EmpHero(ui: EmpProfileUi, actions: EmpProfileActions) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .bouncyClickable(onClick = if (ui.isLoggedIn) actions.onEdit else actions.onLogin),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).bd()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmpAvatar(ui = ui, onClick = if (ui.isLoggedIn) actions.onAvatar else actions.onLogin, size = 54.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (!ui.isLoggedIn) {
                    Text(
                        text = stringResource(R.string.profile_login_signup),
                        style = profileTextStyle(17.sp, FontWeight.SemiBold, Color(0xFF0F172A).fg()),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.profile_view_update_data),
                        style = profileTextStyle(13.sp, FontWeight.Normal, Color(0xFF64748B).fg()),
                        maxLines = 1
                    )
                } else {
                    val hasName = ui.businessName.isNotBlank() && ui.businessName != "Employer" && ui.businessName != "Business"
                    val displayName = when {
                        hasName -> ui.businessName
                        ui.phone.isNotBlank() -> ui.phone
                        else -> stringResource(R.string.profile_set_up_profile)
                    }
                    Text(
                        text = displayName,
                        style = profileTextStyle(18.sp, FontWeight.SemiBold, Color(0xFF0F172A).fg()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (ui.phone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = empFormatPhone(ui.phone),
                            style = profileTextStyle(14.sp, FontWeight.Normal, Color(0xFF64748B).fg()),
                            maxLines = 1
                        )
                    }
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_airbnb_chevron_right),
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The worker profile's completeness block: title, thin progress bar, hint and "Add →". */
@Composable
private fun EmpCompletenessCard(ui: EmpProfileUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 8.dp)
    ) {
        Text(text = stringResource(R.string.emp_profile_complete_pct, ui.pct), style = profileTextStyle(14.sp, FontWeight.Medium, Color(0xFF0F0F0F).fg()))
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE2E8F0).bg())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ui.pct.coerceIn(0, 100) / 100f)
                    .fillMaxHeight()
                    .background(EmpCobalt.bg())
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.emp_profile_reach_100, ui.hint),
                style = profileTextStyle(12.sp, FontWeight.Normal, Color(0xFF64748B).fg()),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.emp_profile_add_action),
                style = profileTextStyle(12.sp, FontWeight.SemiBold, EmpCobalt.fg()),
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
    }
}

@Composable
private fun profileTextStyle(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, color: Color) =
    MaterialTheme.typography.bodyMedium.copy(
        fontSize = size,
        fontWeight = weight,
        color = color,
        lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = 0.sp
    )

@Composable
private fun ProfileRowDivider() {
    HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFF0F0F0))
}

@Composable
private fun EmpMenuRow(
    iconRes: Int,
    title: String,
    pill: String? = null,
    onClick: () -> Unit,
    showStars: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = profileTextStyle(13.5.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showStars) {
            Text(
                text = "\u2605\u2605\u2605\u2605\u2605",
                style = profileTextStyle(11.sp, FontWeight.Normal, EmpAmber.fg())
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (!pill.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFEFF6FF).bg())
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = pill,
                    style = profileTextStyle(11.sp, FontWeight.SemiBold, Color(0xFF2563EB).fg()),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_airbnb_chevron_right),
            contentDescription = null,
            tint = Color(0xFF717171),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmpMenuCard(ui: EmpProfileUi, actions: EmpProfileActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Section 1: Hiring & Management
        Text(
            text = "Hiring & Management",
            style = profileTextStyle(13.sp, FontWeight.Medium, Color(0xFF64748B).fg()),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        EmpMenuRow(
            iconRes = R.drawable.myjobs,
            title = stringResource(R.string.emp_profile_my_posted_jobs),
            pill = null,
            onClick = actions.onMyJobs
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_wallet,
            title = "Services Wallet",
            pill = "₹${ui.walletBalanceRupees}",
            onClick = actions.onWallet
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_wallet,
            title = stringResource(R.string.emp_profile_subscription_credits),
            pill = null,
            onClick = actions.onSubscription
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_globe,
            title = stringResource(R.string.emp_profile_work_locations),
            pill = null,
            onClick = actions.onLocations
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFEBEBEB))
        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Support & Referral
        Text(
            text = "Support & Referral",
            style = profileTextStyle(13.sp, FontWeight.Medium, Color(0xFF64748B).fg()),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_help,
            title = stringResource(R.string.emp_profile_help_support),
            pill = null,
            onClick = actions.onHelp
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_gift,
            title = stringResource(R.string.emp_profile_refer_employer, EmpReferAmount),
            pill = null,
            onClick = actions.onRefer
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFEBEBEB))
        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Settings & About
        Text(
            text = "Settings & About",
            style = profileTextStyle(13.sp, FontWeight.Medium, Color(0xFF64748B).fg()),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_settings,
            title = stringResource(R.string.settings),
            pill = null,
            onClick = actions.onSettings
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_help,
            title = stringResource(R.string.guide_entry),
            pill = null,
            onClick = actions.onGuidelines
        )
        ProfileRowDivider()
        EmpMenuRow(
            iconRes = R.drawable.ic_profile_star,
            title = stringResource(R.string.about_nav_rate_playstore),
            pill = null,
            onClick = actions.onRateApp,
            showStars = true
        )
        ProfileRowDivider()
    }
}

@Composable
private fun EmpAccountCard(ui: EmpProfileUi, actions: EmpProfileActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EmpProfileBody(ui: EmpProfileUi, actions: EmpProfileActions, modifier: Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.cascadeEnter(index = 0)) {
            EmpHero(ui = ui, actions = actions)
        }
        if (ui.isLoggedIn && ui.pct < 100) {
            Box(modifier = Modifier.cascadeEnter(index = 1)) {
                EmpCompletenessCard(ui = ui, onClick = actions.onCompany)
            }
        }
        Box(modifier = Modifier.cascadeEnter(index = 2)) {
            EmpMenuCard(ui = ui, actions = actions)
        }
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
    val homeServicesViewModel: com.example.dutype.homeservices.HomeServicesViewModel = hiltViewModel()
    val wallet by homeServicesViewModel.wallet.collectAsState()
    val walletBalanceRupees = (wallet?.balancePaise ?: 0L) / 100
    var showWalletTopupSheet by remember { mutableStateOf(false) }
    val subState by subscriptionViewModel.activeSubscription.collectAsState()
    val plans by subscriptionViewModel.plans.collectAsState()
    val context = LocalContext.current
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager

    // Live own profile from the shared store (one listener for the whole app)
    val employerProfile by profileCompletionViewModel.profileStore.employer.collectAsState()
    var openJobsCount by remember { mutableStateOf(0) }
    var waitingApplicants by remember { mutableStateOf(0) }

    var companyName by remember { mutableStateOf("") }
    var companyPhone by remember { mutableStateOf("") }
    var employerType by remember { mutableStateOf("COMPANY") }
    var profCompany by remember { mutableStateOf("") }
    var profContact by remember { mutableStateOf("") }
    var profAddress by remember { mutableStateOf("") }
    var profRating by remember { mutableStateOf(0.0) }
    var profRatingCount by remember { mutableStateOf(0) }
    var profCreatedAt by remember { mutableStateOf(0L) }
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

    fun applyEmployer(p: com.example.dutype.profile.EmployerProfile) {
        employerType = p.employerType
        companyName = p.displayName
        profCompany = p.displayName
        profContact = p.ownerName
        profAddress = p.address
        profPhotoUrl = p.photoUrl
        profRating = p.rating
        profRatingCount = p.ratingCount
        profCreatedAt = p.createdAt
        storedPct = p.completionPercent
        companyPhone = p.phone.ifBlank {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber.orEmpty()
        }
        if (p.photoUrl.isNotBlank() && profileImageUri == null) profileImageUrl = p.photoUrl
    }

    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null || currentUser.isAnonymous) {
            isLoadingProfile = false
            return@LaunchedEffect
        }
        currentUserId = currentUser.uid
        profileCompletionViewModel.profileStore.start(com.example.dutype.firestore.FirestoreSchema.Values.Role.EMPLOYER)
        subscriptionViewModel.loadPlans()
        if (employerProfile == null) {
            profileCompletionViewModel.getEmployer(currentUser.uid).getOrNull()?.let { applyEmployer(it) }
        }
        isLoadingProfile = false
        val (open, waiting) = profileCompletionViewModel.profileCompletionService.employerCounts(currentUser.uid)
        openJobsCount = open
        waitingApplicants = waiting
    }

    LaunchedEffect(employerProfile) {
        employerProfile?.let { applyEmployer(it) }
    }

    var showPhotoOptionSheet by remember { mutableStateOf(false) }

    val handleImageUpload: (Uri) -> Unit = { selectedUri ->
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

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { handleImageUpload(it) }
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
        if (profAddress.isBlank()) missingHints.add("Add business address")
        if (photoUrl.isBlank()) missingHints.add("Add shop photo")
        val computedPct = (4 - missingHints.size) * 100 / 4
        val pct = if (storedPct in 1..100) storedPct else computedPct
        val hint = if (missingHints.isNotEmpty()) missingHints[0] else "Complete your profile"

        val planName = plans.firstOrNull { it.id == subState.planId }?.name
        val contact = if (profContact.isNotBlank() && !profContact.equals(displayName, ignoreCase = true)) profContact else ""

        val ui = EmpProfileUi(
            businessName = displayName,
            contact = contact,
            phone = empFormatPhone(companyPhone),
            rating = profRating,
            ratingCount = profRatingCount,
            memberSinceMillis = profCreatedAt,
            imageModel = if (profileImageUri != null) profileImageUri.toString() else photoUrl.ifBlank { null },
            uploading = isUploadingImage,
            activeJobs = openJobsCount,
            applicants = waitingApplicants,
            locCount = if (profAddress.isNotBlank()) 1 else 0,
            city = if (profAddress.isNotBlank()) empCityFrom(profAddress) else "Add a work location",
            pct = pct,
            hint = hint,
            pill = empPillText(subState, planName),
            isLoggedIn = isLoggedIn,
            walletBalanceRupees = walletBalanceRupees
        )

        val actions = EmpProfileActions(
            onHelp = {
                try {
                    nav.navigate(Routes.EMPLOYER_HELP)
                } catch (e: Exception) {
                    Timber.e(e, "Error navigating to help")
                    android.widget.Toast.makeText(context, "Unable to open Help Desk.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onEdit = { go(Routes.EMPLOYER_COMPANY_DETAILS, "profile") },
            onAvatar = {
                if (isLoggedIn) {
                    showPhotoOptionSheet = true
                } else {
                    pendingMenuAction = "profile"
                    showLoginBottomSheet = true
                }
            },
            onLogin = { rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=EMPLOYER") },
            onCompany = { go(Routes.EMPLOYER_COMPANY_DETAILS, "profile") },
            onMyJobs = { go(Routes.EMPLOYER_MY_JOBS, "my_jobs") },
            onLocations = { go(Routes.EMPLOYER_MANAGE_ADDRESSES, "locations") },
            onSubscription = { go(Routes.EMPLOYER_SUBSCRIPTION, null) },
            onHistory = { go(Routes.EMPLOYER_HISTORY, "job_posts") },
            onRefer = { go(Routes.EMPLOYER_REFER_EARN, null) },
            onWallet = { showWalletTopupSheet = true },
            onGuidelines = { nav.navigate(Routes.guidelinesRoute(com.example.dutype.guidelines.GuidelineRole.EMPLOYER)) },
            onRateApp = { openPlayStoreListing(context) },
            onSettings = { rootNavController.navigate(Routes.SETTINGS) },
            onPrivacy = { rootNavController.navigate(Routes.PRIVACY_POLICY) },
            onTerms = { rootNavController.navigate(Routes.TERMS_OF_SERVICE) },
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

            EmpTopBar()
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

    // DutyPe Micro-Wallet Deposit Bottom Sheet
    if (showWalletTopupSheet) {
        com.example.dutype.homeservices.MicroWalletTopupSheet(
            currentBalanceRupees = walletBalanceRupees,
            requiredFeeRupees = 9,
            onDismiss = { showWalletTopupSheet = false },
            onTopupSuccess = { showWalletTopupSheet = false },
            viewModel = homeServicesViewModel
        )
    }

    com.example.dutype.components.ImagePickerBottomSheet(
        isVisible = showPhotoOptionSheet,
        onDismiss = { showPhotoOptionSheet = false },
        onImageSelected = { uri -> handleImageUpload(uri) }
    )

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

private const val PLAY_STORE_PACKAGE = "com.dutype.app"

private fun openPlayStoreListing(context: android.content.Context) {
    try {
        val playStoreIntent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("market://details?id=$PLAY_STORE_PACKAGE")
        ).apply {
            setPackage("com.android.vending")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(playStoreIntent)
    } catch (e: Exception) {
        try {
            val webIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE")
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            timber.log.Timber.e(e2, "Unable to open Play Store")
        }
    }
}
