package com.example.dutype.worker.screens.profile

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import com.example.dutype.components.UserReviewsBottomSheet
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.ui.unit.TextUnit
import com.example.dutype.navigation.WorkerBottomRoutes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dutype.components.ProfessionalLogoutDialog
import com.example.dutype.components.ProfileShimmer
import com.example.dutype.data.ApplicationFormDataStore
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.LocaleHelper
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.utils.findActivity
import com.example.dutype.viewmodels.ProfileCompletionViewModel
import com.example.dutype.worker.models.PersonalInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    rootNavController: NavController,
    localNavController: NavController? = null,
    onStatusBarColorChange: (Color) -> Unit = {},
    scrollStateManager: ScrollStateManager? = null,
    dataStore: ApplicationFormDataStore
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileCompletionViewModel: ProfileCompletionViewModel = hiltViewModel()
    val workerProfile by profileCompletionViewModel.profileStore.worker.collectAsState()
    // Services accessed via ProfileCompletionViewModel (proper DI pattern)
    val authManager = profileCompletionViewModel.authManager
    val profileCompletionService = profileCompletionViewModel.profileCompletionService
    var currentUserId by remember { mutableStateOf("") }

    // Auth validation - ensure unauthenticated users cannot access profile actions
    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Timber.w("Worker Profile - User not authenticated, staying in guest profile mode")
        } else {
            Timber.i("Worker Profile - User authenticated: ${currentUser.uid}")
        }
    }

    // Profile completion state
    var profileCompletionPercentage by remember { mutableStateOf(0) }
    var isProfileCompleted by remember { mutableStateOf(false) }

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    // var showThemeBottomSheet by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Guest mode - Login bottom sheet state
    var showLoginBottomSheet by remember { mutableStateOf(false) }
    var pendingMenuAction by remember { mutableStateOf<String?>(null) }

    // LIGHTWEIGHT PROFILE: Use metadata for basic profile info (name, phone, image)
    // Full profile data loads only in profile details screen

    // Get profile data from dataStore - using state with LaunchedEffect for suspend functions
    var personalInfo by remember { mutableStateOf(com.example.dutype.worker.models.PersonalInfo()) }
    var experience by remember { mutableStateOf<List<com.example.dutype.models.WorkExperience>>(emptyList()) }
    var skills by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverLetter by remember { mutableStateOf("") }
    var isFormCompleted by remember { mutableStateOf(false) }

    // Load dataStore data in coroutine (lightweight - local storage only)
    LaunchedEffect(Unit) {
        personalInfo = dataStore.getPersonalInfo()
        isFormCompleted = dataStore.isFormCompleted()
        // NOTE: experience, skills, coverLetter removed from main profile screen
        // These load only in profile details screen for performance
    }

    // DON'T load full profile from Firebase on main profile screen
    // Use metadata instead for lightweight display

    // Use backend profile data if available, otherwise fallback to dataStore
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var profileSetupStatus by remember { mutableStateOf<com.example.dutype.state.ProfileSetupStatus?>(null) }
    var profileCompletion by remember { mutableStateOf(0) }

    // Firebase profile data state for reactive updates
    var firebaseProfileData by remember { mutableStateOf<Map<String, Any?>?>(null) }

    // Hero / completeness data (visual layer only; read-only loads)
    var profileSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var profileCity by remember { mutableStateOf("") }
    var profileRating by remember { mutableStateOf(0.0) }
    var profileReviewCount by remember { mutableStateOf(0) }
    var memberSinceMillis by remember { mutableStateOf(0L) }
    var completionPercent by remember { mutableStateOf<Int?>(null) }

    // Hero data from the live own profile (shared listener, no extra reads)
    LaunchedEffect(workerProfile) {
        workerProfile?.let { p ->
            profileSkills = p.skills.map { com.example.dutype.employer.models.JobCategory.fromKey(it).displayName }
            profileCity = p.area
            memberSinceMillis = p.createdAt
            completionPercent = p.completionPercent
            if (p.photoUrl.isNotBlank() && profileImageUri == null) profileImageUrl = p.photoUrl
        }
    }

    // Rating lives on the public worker card (one read)
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            profileCompletionService.workerCard(currentUserId)?.let { card ->
                profileRating = card.rating
                profileReviewCount = card.ratingCount
            }
        }
    }

    var showReviewsSheet by remember { mutableStateOf(false) }
    var isReviewsLoading by remember { mutableStateOf(false) }
    var workerReviews by remember { mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList()) }
    var workerGivenReviews by remember { mutableStateOf<List<com.example.dutype.services.Rating>>(emptyList()) }
    val ratingService = remember { com.example.dutype.di.ratingServiceFromHilt(context) }

    fun openReviewsSheet() {
        if (currentUserId.isNotBlank()) {
            showReviewsSheet = true
            scope.launch {
                isReviewsLoading = true
                try {
                    workerReviews = ratingService.getUserRatings(currentUserId)
                    workerGivenReviews = ratingService.getRatingsGivenByUser(currentUserId)
                } catch (e: Exception) {
                    Timber.e(e, "Error loading ratings")
                } finally {
                    isReviewsLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            currentUserId = currentUser.uid
            profileCompletionViewModel.profileStore.start(com.example.dutype.firestore.FirestoreSchema.Values.Role.WORKER)
        }
        isLoadingProfile = false
    }

    // Update userName from metadata (lightweight)
    LaunchedEffect(workerProfile, personalInfo) {
        userName = when {
            !workerProfile?.name.isNullOrBlank() -> workerProfile!!.name
            personalInfo.fullName.isNotBlank() -> personalInfo.fullName
            else -> "User"
        }
    }

    // Status bar color - White for profile screen
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color(0xFFF8FAFC))
        delay(200)
        isVisible = true
    }

    var showPhotoOptionSheet by remember { mutableStateOf(false) }

    val handleImageUpload: (Uri) -> Unit = { selectedUri ->
        Timber.d(" WORKER PROFILE: Selected image URI: $selectedUri")
        profileImageUri = selectedUri
        isUploadingImage = true

        // Upload image to Firebase Storage and update profile
        scope.launch {
            try {
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                Timber.d(" WORKER PROFILE: Current user: ${currentUser?.uid}")
                if (currentUser != null) {
                    // Upload to Firebase Storage
                    Timber.d(" WORKER PROFILE: Starting upload...")
                    val uploadResult = profileCompletionViewModel.uploadProfileImage(selectedUri, currentUser.uid, "worker")
                    uploadResult.fold(
                        onSuccess = { imageUrl ->
                            profileImageUrl = imageUrl
                            Timber.i(" WORKER PROFILE: ✅ Profile image uploaded: $imageUrl")
                            android.widget.Toast.makeText(context, context.getString(R.string.profile_photo_updated), android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            Timber.e(exception, "Failed to upload profile image")
                            profileImageUri = null // Reset the local preview
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
                Timber.e(e, "Error uploading profile image")
                profileImageUri = null // Reset the local preview
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

    // Play Store URL constant
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.dutype.app"

    // WhatsApp sharing function
    val shareToWhatsApp = {
        val packageManager = context.packageManager

        try {
            // Try to open WhatsApp directly
            val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (whatsappIntent != null) {
                // Create sharing intent for WhatsApp
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT,
                        "Check out this amazing job app! Download DutyPe and find your dream job.\n\n" +
                        "Download link: $playStoreUrl"
                    )
                    setPackage("com.whatsapp")
                }
                context.startActivity(shareIntent)
            } else {
                // WhatsApp not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback to browser
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://wa.me/?text=Check%20out%20this%20amazing%20job%20app!%20Download%20DutyPe%20and%20find%20your%20dream%20job.%20Download%20link:%20$playStoreUrl")
            )
            context.startActivity(browserIntent)
        }
    }

    // Instagram sharing function
    val shareToInstagram = {
        val packageManager = context.packageManager

        try {
            // Try to open Instagram Stories or Feed
            val instagramIntent = packageManager.getLaunchIntentForPackage("com.instagram.android")
            if (instagramIntent != null) {
                // Create sharing intent for Instagram
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT,
                        " Found an amazing job app! DutyPe helps you find your dream job easily.\n\n" +
                        " Download now: $playStoreUrl\n\n" +
                        "#DutyPe #Jobs #Career #Hiring"
                    )
                    setPackage("com.instagram.android")
                }
                context.startActivity(shareIntent)
            } else {
                // Instagram not installed, open in browser
                val browserIntent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.instagram.com/")
                )
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Fallback - open Play Store link
            val browserIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(playStoreUrl)
            )
            context.startActivity(browserIntent)
        }
    }

    // Settings-style layout with Meesho-style background
    // Show shimmer while loading, then show actual content
    if (isLoadingProfile) {
        ProfileShimmer()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Flat, fully white page (no cards) — also behind the status bar.
                .background(Color.White.bg())
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Offline banner at the very top
            val connectivityViewModel: com.example.dutype.viewmodels.ConnectivityViewModel = hiltViewModel()
            val isOnline by connectivityViewModel.isOnline.collectAsState()
            com.example.dutype.components.OfflineBanner(isOffline = !isOnline)

            val isLoggedIn = currentUserId.isNotEmpty()
            val hasPhoto = profileImageUri != null || !profileImageUrl.isNullOrBlank()
            val openProfileDetails: () -> Unit = {
                if (isLoggedIn) {
                    rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS)
                } else {
                    pendingMenuAction = "profile"
                    showLoginBottomSheet = true
                }
            }
            val startImagePicker: () -> Unit = {
                if (isLoggedIn) showPhotoOptionSheet = true
                else { pendingMenuAction = "profile"; showLoginBottomSheet = true }
            }
            val whatsappSupport: () -> Unit = {
                val whatsappNumber = "918019151847" // DutyPe support number
                val message = "Hello DutyPe Team! I am a worker on DutyPe and I need help with the app."
                val encodedMessage = runCatching { java.net.URLEncoder.encode(message, "UTF-8") }.getOrDefault("")
                val whatsappUrl = "https://wa.me/$whatsappNumber?text=$encodedMessage"
                runCatching {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(whatsappUrl)).apply {
                        setPackage("com.whatsapp")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }.onFailure {
                    runCatching {
                        val browserIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(whatsappUrl)
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browserIntent)
                    }.onFailure {
                        android.widget.Toast.makeText(context, "DutyPe Support WhatsApp: +91 85007 17800", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }

            // Top bar: Profile title with Language switcher and WhatsApp support
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(R.string.profile),
                    color = androidx.compose.ui.graphics.Color(0xFF0F172A).fg(),
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showLanguageSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.translate_indic_24),
                        contentDescription = "Language",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = whatsappSupport,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp Support",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 0.dp, top = 4.dp, end = 0.dp, bottom = 100.dp)
            ) {
                // HERO CARD: profile image on left, name & phone beside it, trailing chevron on right
                item {
                    val authPhone = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                    val userPhone = workerProfile?.phone.orEmpty().ifBlank { personalInfo.phone }.ifBlank { authPhone }
                    val hasName = userName.isNotBlank() && userName != "User"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable {
                                if (isLoggedIn) openProfileDetails()
                                else rootNavController.navigate("${Routes.ENHANCED_LOGIN}?role=WORKER")
                            },
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
                            // Circular Profile Image on the left
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9).bg())
                                    .border(1.dp, Color(0xFFE2E8F0).bd(), CircleShape)
                                    .clickable { startImagePicker() },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isUploadingImage -> {
                                        com.example.dutype.components.ThreeDotsBouncingLoader(
                                            dotSize = 4.dp,
                                            spacing = 2.dp,
                                            dotColor = Color(0xFF10B981).fg()
                                        )
                                    }
                                    profileImageUri != null -> {
                                        com.example.dutype.components.OptimizedProfileImage(
                                            imageUrl = profileImageUri.toString(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    !profileImageUrl.isNullOrBlank() -> {
                                        com.example.dutype.components.OptimizedProfileImage(
                                            imageUrl = profileImageUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Default Profile",
                                            tint = Color(0xFF94A3B8).fg(),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Name & Phone Beside Profile Image
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoggedIn) {
                                    val nameText = when {
                                        hasName -> userName
                                        userPhone.isNotBlank() -> userPhone
                                        else -> stringResource(R.string.profile_set_up_profile)
                                    }
                                    Text(
                                        text = nameText,
                                        style = profileTextStyle(18.sp, FontWeight.SemiBold, Color(0xFF0F172A).fg()),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (userPhone.isNotBlank() && hasName) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = userPhone,
                                            style = profileTextStyle(14.sp, FontWeight.Normal, Color(0xFF64748B).fg()),
                                            maxLines = 1
                                        )
                                    } else if (!hasName) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = stringResource(R.string.profile_tap_add_details),
                                            style = profileTextStyle(13.sp, FontWeight.Normal, Color(0xFF94A3B8).fg()),
                                            maxLines = 1
                                        )
                                    }
                                } else {
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

                // COMPLETENESS CARD
                val pct = completionPercent
                if (isLoggedIn && pct != null && pct < 100) {
                    item {
                        ProfileCompletenessCard(
                            percent = pct.coerceIn(0, 100),
                            hint = when {
                                !hasPhoto -> stringResource(R.string.worker_profile_hint_photo)
                                profileSkills.isEmpty() -> stringResource(R.string.worker_profile_hint_skills)
                                profileCity.isBlank() -> stringResource(R.string.worker_profile_hint_city)
                                else -> stringResource(R.string.worker_profile_hint_complete)
                            },
                            onAdd = openProfileDetails
                        )
                    }
                }

                // SETTINGS LIST CARD (mockup: exactly 5 rows)
                item {
                    ProfileSettingsCard(
                        onEditProfile = openProfileDetails,
                        onWorkHistory = {
                            if (isLoggedIn) {
                                localNavController?.navigate(Routes.WORKER_HISTORY) ?: rootNavController.navigate(Routes.WORKER_HISTORY)
                            } else {
                                pendingMenuAction = "applications"
                                showLoginBottomSheet = true
                            }
                        },
                        onEarnings = {
                            if (isLoggedIn) {
                                localNavController?.navigate(Routes.WORKER_EARNINGS) ?: rootNavController.navigate(Routes.WORKER_EARNINGS)
                            } else {
                                pendingMenuAction = "earnings"
                                showLoginBottomSheet = true
                            }
                        },
                        onReferEarn = {
                            if (isLoggedIn) {
                                localNavController?.navigate(Routes.WORKER_REFER_EARN) ?: rootNavController.navigate(Routes.WORKER_REFER_EARN)
                            } else {
                                pendingMenuAction = "refer_earn"
                                showLoginBottomSheet = true
                            }
                        },
                        onBookServices = {
                            rootNavController.navigate(Routes.SERVICES)
                        },
                        onServicesBookings = {
                            rootNavController.navigate(Routes.SERVICES_BOOKINGS)
                        },
                        onGuidelines = {
                            val route = Routes.guidelinesRoute(com.example.dutype.guidelines.GuidelineRole.WORKER)
                            localNavController?.navigate(route) ?: rootNavController.navigate(route)
                        },
                        onRateApp = { openPlayStoreListing(context) },
                        onJoinCommunity = { openWhatsAppCommunity(context) },
                        onHelp = {
                            localNavController?.navigate(Routes.HELP) ?: rootNavController.navigate(Routes.HELP)
                        },
                        onSettings = { rootNavController.navigate(Routes.SETTINGS) },
                        onPrivacy = { rootNavController.navigate(Routes.PRIVACY_POLICY) },
                        onTerms = { rootNavController.navigate(Routes.TERMS_OF_SERVICE) }
                    )
                }



                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
    }
    } // End of else block for loading check

    // Dialogs
    if (showEditDialog) {
        ModernEditDialog(
            userName = userName,
            userEmail = userEmail,
            personalInfo = personalInfo,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail, updatedPersonalInfo ->
                scope.launch {
                    try {
                        // Update local variables
                        userName = newName
                        userEmail = newEmail
                        personalInfo = updatedPersonalInfo

                        // Save to DataStore
                        dataStore.savePersonalInfo(updatedPersonalInfo)

                        // Save to Firebase
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        if (currentUser != null) {
                            profileCompletionViewModel.saveWorker(
                                mapOf(com.example.dutype.firestore.FirestoreSchema.WorkerProfiles.NAME to newName.trim())
                            ).onFailure { Timber.e(it, "Worker name update failed") }
                        }

                        showEditDialog = false
                    } catch (e: Exception) {
                        Timber.e(e, "Error updating worker profile")
                        // Still close dialog even if Firebase save fails
                        showEditDialog = false
                    }
                }
            }
        )
    }

    if (showAccountDeletionDialog) {
        com.example.dutype.components.AccountDeletionDialog(
            isVisible = showAccountDeletionDialog,
            onDismiss = { showAccountDeletionDialog = false },
            navController = rootNavController,
            userRole = "Worker",
            authManager = authManager,
            profileCompletionViewModel = profileCompletionViewModel,
            scope = scope
        )
    }

    // Feedback Bottom Sheet
    com.example.dutype.components.FeedbackBottomSheet(
        isVisible = showFeedbackSheet,
        onDismiss = { showFeedbackSheet = false },
        userRole = "worker"
    )

    // if (showThemeBottomSheet) {
    //     val themeSheetState = androidx.compose.material3.rememberModalBottomSheetState(
    //         skipPartiallyExpanded = true
    //     )
    //     com.example.dutype.components.ThemeModeBottomSheet(
    //         sheetState = themeSheetState,
    //         onDismiss = { showThemeBottomSheet = false },
    //     )
    // }

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
                "profile" -> rootNavController.navigate(Routes.WORKER_PROFILE_DETAILS)
                "applications" -> localNavController?.navigate(Routes.WORKER_HISTORY) ?: rootNavController.navigate(Routes.WORKER_HISTORY)
                "earnings" -> localNavController?.navigate(Routes.WORKER_EARNINGS) ?: rootNavController.navigate(Routes.WORKER_EARNINGS)
                "refer_earn" -> localNavController?.navigate(Routes.WORKER_REFER_EARN) ?: rootNavController.navigate(Routes.WORKER_REFER_EARN)
            }
            pendingMenuAction = null
        },
        role = com.example.dutype.models.UserRole.WORKER,
        navController = rootNavController,
        title = stringResource(R.string.login_required),
        subtitle = when (pendingMenuAction) {
            "profile" -> stringResource(R.string.login_to_view_profile)
            "applications" -> stringResource(R.string.login_to_view_applications)
            "earnings" -> stringResource(R.string.login_to_view_earnings)
            "refer_earn" -> stringResource(R.string.login_to_refer_earn)
            else -> stringResource(R.string.login_to_access_feature)
        }
    )

    if (showLanguageSheet) {
        com.example.dutype.components.LanguageSelectionBottomSheet(
            onDismiss = { showLanguageSheet = false }
        )
    }

    UserReviewsBottomSheet(
        isVisible = showReviewsSheet,
        title = stringResource(R.string.my_ratings_reviews),
        averageRating = profileRating.toFloat(),
        totalRatings = profileReviewCount,
        reviews = workerReviews,
        givenReviews = workerGivenReviews,
        isLoading = isReviewsLoading,
        isGivenLoading = isReviewsLoading,
        receivedTabTitle = stringResource(R.string.employers_rated_you),
        givenTabTitle = stringResource(R.string.you_rated_employers),
        onDismiss = { showReviewsSheet = false }
    )
}


@Composable
private fun ModernEditDialog(
    userName: String,
    userEmail: String,
    personalInfo: PersonalInfo,
    onDismiss: () -> Unit,
    onSave: (String, String, PersonalInfo) -> Unit
) {
    var newName by remember { mutableStateOf(userName) }
    var newEmail by remember { mutableStateOf(userEmail) }
    var newPhone by remember { mutableStateOf(personalInfo.phone) }
    var newAddress by remember { mutableStateOf(personalInfo.address) }
    var newDateOfBirth by remember { mutableStateOf(personalInfo.dateOfBirth) }
    var newGender by remember { mutableStateOf(personalInfo.gender) }
    fun formatDob(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(8)
        return buildString {
            digits.forEachIndexed { idx, c ->
                if (idx == 2 || idx == 4) append('/')
                append(c)
            }
        }
    }
    var dobInput by remember {
        mutableStateOf(TextFieldValue(newDateOfBirth, selection = TextRange(newDateOfBirth.length)))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_edit_profile),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = com.example.dutype.ui.theme.WorkerColors.IconSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(R.string.full_name)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.dutype.ui.theme.WorkerColors.BorderFocused,
                                focusedLabelColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                focusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                unfocusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { /* Phone cannot be changed */ },
                            label = { Text(stringResource(R.string.phone_number)) },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF666666).fg(),
                                disabledBorderColor = Color(0xFFE0E0E0).bd(),
                                disabledLabelColor = Color(0xFF999999).fg()
                            ),
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444).fg(),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            label = { Text(stringResource(R.string.address)) },
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.dutype.ui.theme.WorkerColors.BorderFocused,
                                focusedLabelColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                focusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                unfocusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = dobInput,
                            onValueChange = {
                                val formatted = formatDob(it.text)
                                newDateOfBirth = formatted
                                dobInput = TextFieldValue(formatted, selection = TextRange(formatted.length))
                            },
                            label = { Text(stringResource(R.string.date_of_birth)) },
                            placeholder = { Text(stringResource(R.string.dob_placeholder)) },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.dutype.ui.theme.WorkerColors.BorderFocused,
                                focusedLabelColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                focusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                unfocusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newGender,
                            onValueChange = { newGender = it },
                            label = { Text(stringResource(R.string.gender)) },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = com.example.dutype.ui.theme.WorkerColors.BorderFocused,
                                focusedLabelColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                focusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                unfocusedTextColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Text(stringResource(R.string.cancel), color = com.example.dutype.ui.theme.WorkerColors.TextSecondary)
                    }

                    Button(
                        onClick = {
                            val updatedPersonalInfo = personalInfo.copy(
                                fullName = newName,
                                email = newEmail,
                                phone = newPhone,
                                address = newAddress,
                                dateOfBirth = newDateOfBirth,
                                gender = newGender
                            )
                            onSave(newName, newEmail, updatedPersonalInfo)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937).bg()
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save_changes))
                    }
                }
            }
        }
    }
}

// ============================================
// MEESHO-STYLE COMPONENTS
// ============================================

@Composable
private fun profileTextStyle(size: TextUnit, weight: FontWeight, color: Color) =
    MaterialTheme.typography.bodyMedium.copy(
        fontSize = size,
        fontWeight = weight,
        color = color,
        lineHeight = TextUnit.Unspecified,
        letterSpacing = 0.sp
    )

@Composable
private fun ProfileListRow(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconRes: Int? = null,
    showStars: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222),
                modifier = Modifier.size(25.dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222),
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = profileTextStyle(14.5.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showStars) {
            Text(
                text = "\u2605\u2605\u2605\u2605\u2605",
                style = profileTextStyle(11.sp, FontWeight.Normal, Color(0xFFF59E0B).fg())
            )
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
private fun ProfileCompletenessCard(percent: Int, hint: String, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.emp_profile_complete_pct, percent),
            style = profileTextStyle(14.sp, FontWeight.Medium, Color(0xFF0F0F0F).fg())
        )
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
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .background(Color(0xFF10B981).bg())
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hint,
                style = profileTextStyle(12.sp, FontWeight.Normal, Color(0xFF64748B).fg()),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.emp_profile_add_action),
                style = profileTextStyle(12.sp, FontWeight.SemiBold, Color(0xFF10B981).fg()),
                modifier = Modifier.clickable(onClick = onAdd)
            )
        }
    }
}

@Composable
private fun ProfileSettingsCard(
    onEditProfile: () -> Unit,
    onWorkHistory: () -> Unit,
    onEarnings: () -> Unit,
    onReferEarn: () -> Unit,
    onBookServices: () -> Unit = {},
    onServicesBookings: () -> Unit = {},
    onGuidelines: () -> Unit = {},
    onRateApp: () -> Unit,
    onJoinCommunity: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit = {},
    onTerms: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Section 1: Work & Earnings
        Text(
            text = "Work & Earnings",
            style = profileTextStyle(15.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        ProfileListRow(
            title = stringResource(R.string.work_history),
            onClick = onWorkHistory,
            iconRes = R.drawable.ic_profile_history
        )
        ProfileRowDivider()
        ProfileListRow(
            title = stringResource(R.string.my_earnings),
            onClick = onEarnings,
            iconRes = R.drawable.ic_profile_wallet
        )
        ProfileRowDivider()
        ProfileListRow(
            title = stringResource(R.string.emp_profile_help_desk),
            onClick = onHelp,
            iconRes = R.drawable.ic_profile_help
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFEBEBEB))
        Spacer(modifier = Modifier.height(16.dp))

        // Section 2: Referral
        Text(
            text = "Referral",
            style = profileTextStyle(15.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable(onClick = onReferEarn)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile_gift),
                contentDescription = null,
                tint = if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222),
                modifier = Modifier.size(25.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.refer_earn),
                style = profileTextStyle(14.5.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFFEF3C7).bg())
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "₹100",
                    style = profileTextStyle(11.sp, FontWeight.Bold, Color(0xFFD97706).fg())
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_airbnb_chevron_right),
                contentDescription = null,
                tint = Color(0xFF717171),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFEBEBEB))
        Spacer(modifier = Modifier.height(16.dp))

        // Section 3: Settings & About
        Text(
            text = "Settings & About",
            style = profileTextStyle(15.sp, FontWeight.Normal, if (com.example.dutype.ui.theme.LocalDarkMode.current) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ProfileListRow(
            title = stringResource(R.string.settings),
            onClick = onSettings,
            iconRes = R.drawable.ic_profile_settings
        )
        ProfileRowDivider()
        ProfileListRow(
            title = stringResource(R.string.guide_entry),
            onClick = onGuidelines,
            iconRes = R.drawable.ic_profile_help
        )
        ProfileRowDivider()
        ProfileListRow(
            title = stringResource(R.string.about_nav_rate_playstore),
            onClick = onRateApp,
            iconRes = R.drawable.ic_profile_star,
            showStars = true
        )
        ProfileRowDivider()
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// TODO: replace with the real community invite if this one changes.
private const val WHATSAPP_COMMUNITY_URL = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"
private const val PLAY_STORE_PACKAGE = "com.dutype.app"

private fun openPlayStoreListing(context: android.content.Context) {
    try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PLAY_STORE_PACKAGE")).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(playStoreIntent)
    } catch (e: Exception) {
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PLAY_STORE_PACKAGE")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e2: Exception) {
            Timber.e(e2, "Unable to open Play Store")
        }
    }
}

private fun openWhatsAppCommunity(context: android.content.Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_COMMUNITY_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Timber.e(e, "Unable to open WhatsApp community")
    }
}

@Composable
private fun ProfileRowDivider() {
    HorizontalDivider(thickness = 0.75.dp, color = Color(0xFFF0F0F0))
}
