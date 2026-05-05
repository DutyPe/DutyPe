package com.example.dutype.worker.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.AnnouncementList
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.components.GuestWelcomeBonusCard
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.WorkerHomeShimmer
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.models.JobListing
import com.example.dutype.models.JobVacancyStatus
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.models.WorkerJobRequest
import com.example.dutype.navigation.Routes
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal fun formatWorkerHomeLocation(location: com.example.dutype.models.LocationData): String {
    val rawAddress = when {
        location.address.isNotBlank() -> location.address
        else -> listOfNotNull(
            location.area?.takeIf { it.isNotBlank() },
            location.city?.takeIf { it.isNotBlank() },
            location.state?.takeIf { it.isNotBlank() },
            location.country?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }

    val normalizedParts = rawAddress
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .fold(mutableListOf<String>()) { acc, part ->
            if (acc.none { it.equals(part, ignoreCase = true) }) {
                acc.add(part)
            }
            acc
        }

    return normalizedParts.joinToString(", ").ifBlank { "Select Your Location" }
}


@Composable
internal fun LoadingContent() {
    WorkerHomeShimmer()
}

@Composable
internal fun WorkerHomeBackdropDecor(modifier: Modifier = Modifier) {
    // Intentionally empty: design rule forbids gradient halos. The role
    // theme handles the screen surface; this composable is kept so existing
    // call-sites continue to compile.
    Box(modifier = modifier)
}


private val WorkerEmptyHumorMessages = listOf(
    "Even the local chai stall is on a break right now.",
    "A tumbleweed just rolled by. Let\u2019s shake things up \u2014 try a new area.",
    "This zone is on silent mode. Switch the location and turn the volume up.",
    "Crickets. Just crickets. Time to scout a livelier spot.",
    "Your skills are sharper than this neighborhood deserves. Explore wider."
)

private val WorkerAppliedAllHumorMessages = listOf(
    "You\u2019ve applied to literally everything. Save some jobs for the rest of us.",
    "Inbox: empty. Hustle: legendary. \uD83D\uDCAA",
    "Local jobs: cleared. Boss-level unlocked. Try a new area.",
    "You\u2019re moving faster than the jobs are. Widen the radius."
)

private val WorkerEmptyEmojiCast = listOf(
    "\uD83E\uDD14",  // thinking — "hmm where are the jobs"
    "\uD83D\uDD0D",  // searching
    "\uD83D\uDE34",  // sleepy area
    "\uD83E\uDD37",  // shrug
    "\uD83C\uDF35"   // desert/empty
)

private val WorkerAppliedAllEmojiCast = listOf(
    "\uD83C\uDF89",  // celebration
    "\uD83D\uDE0E",  // cool
    "\uD83D\uDCAA",  // strong
    "\uD83D\uDE4C",  // praise
    "\uD83D\uDE80"   // rocket
)

@Composable
fun EmptyJobsState(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentLocationName: String? = null,
    isAppliedAllVariant: Boolean = false,
    @Suppress("UNUSED_PARAMETER") suggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    @Suppress("UNUSED_PARAMETER") onCitySelected: (TopCityChips.CityLocationChip) -> Unit = {}
) {
    val locationLabel = currentLocationName?.let { "near \"$it\"" } ?: "in your area"
    val humorPool = if (isAppliedAllVariant) WorkerAppliedAllHumorMessages else WorkerEmptyHumorMessages
    // Pick a stable random message per location so it does not flicker on recomposition.
    val humorMessage = remember(currentLocationName, isAppliedAllVariant) {
        humorPool.random()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            JobHuntingIllustration(isAppliedAllVariant = isAppliedAllVariant)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isAppliedAllVariant) "You are on top of it!" else "No jobs $locationLabel",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = humorMessage,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Animated empty-state illustration: a soft pulsing gradient blob, a cycling
 * cast of expressive emoji that bob and tilt, and orbiting sparkles.
 *
 * Pure Compose, zero dependencies, runs offline at 60fps.
 */
@Composable
private fun JobHuntingIllustration(
    isAppliedAllVariant: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "empty-state")

    // Gentle vertical bob for the emoji
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    // Subtle tilt
    val tilt by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tilt"
    )
    // Backdrop pulse
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    // Orbit angle for sparkles
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    // Cycle through expressive emojis every ~2.4s for the comedy reveal.
    val cast = if (isAppliedAllVariant) WorkerAppliedAllEmojiCast else WorkerEmptyEmojiCast
    var castIndex by remember(isAppliedAllVariant) { mutableStateOf(0) }
    LaunchedEffect(isAppliedAllVariant) {
        while (true) {
            kotlinx.coroutines.delay(2400)
            castIndex = (castIndex + 1) % cast.size
        }
    }
    val currentEmoji = cast[castIndex]

    val accent = if (isAppliedAllVariant) Color(0xFF10B981) else Color(0xFF6366F1)
    val accentSoft = accent.copy(alpha = 0.18f)
    val accentFaint = accent.copy(alpha = 0.08f)

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft pulsing gradient blob backdrop
        Canvas(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentSoft, accentFaint, Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.minDimension / 2f
                )
            )
        }

        // Orbiting sparkle dots
        Canvas(modifier = Modifier.size(160.dp)) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.minDimension * 0.42f
            val sparkles = listOf(
                Triple(0f, 4f, accent),
                Triple(120f, 3f, accent.copy(alpha = 0.7f)),
                Triple(240f, 5f, accent.copy(alpha = 0.85f))
            )
            sparkles.forEach { (offsetDeg, dotRadius, color) ->
                val angleRad = Math.toRadians((orbit + offsetDeg).toDouble())
                val x = centerX + radius * kotlin.math.cos(angleRad).toFloat()
                val y = centerY + radius * kotlin.math.sin(angleRad).toFloat()
                drawCircle(color = color, radius = dotRadius, center = Offset(x, y))
            }
        }

        // The emoji — cycles through a cast, bobs and tilts subtly
        AnimatedContent(
            targetState = currentEmoji,
            transitionSpec = {
                (fadeIn(tween(420)) +
                    scaleIn(initialScale = 0.6f, animationSpec = tween(420)))
                    .togetherWith(
                        fadeOut(tween(220)) +
                            scaleOut(targetScale = 1.4f, animationSpec = tween(220))
                    )
            },
            label = "emoji-cast"
        ) { emoji ->
            Text(
                text = emoji,
                fontSize = 72.sp,
                modifier = Modifier.graphicsLayer {
                    translationY = bob
                    rotationZ = tilt
                }
            )
        }
    }
}

@Composable
internal fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
//            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = "Error",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(IconSizes.ExtraLarge) // Material Design 3: 48dp
                )

                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                )

                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center
                    )
                )

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizes.Small) // Material Design 3: 20dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.try_again))
                }
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeSectionsContent(
    jobListings: List<JobListing>,
    navController: NavController,
    rootNavController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    currentLocation: com.example.dutype.models.LocationData? = null,
    onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit = {},
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    jobVacancyStatuses: Map<String, JobVacancyStatus> = emptyMap(),
    scrollStateManager: ScrollStateManager? = null,
    onJobClick: (String) -> Unit,
    onNavigateToJob: (String) -> Unit,
    userName: String = "",
    userEmail: String = "",
    userSkills: List<String> = emptyList(),
    onScrollOffsetChange: (Float) -> Unit = {},
    onLocationBarAlphaChange: (Float) -> Unit = {},
    showEmptyJobsState: Boolean = false,
    emptyJobsIsAppliedAllVariant: Boolean = false,
    emptyJobsCurrentLocationName: String? = null,
    emptyJobsSuggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onEmptyJobsCitySelected: (TopCityChips.CityLocationChip) -> Unit = {},
    showGuestWelcomeCard: Boolean = false,
    guestWelcomeTitle: String = "",
    guestWelcomeMessage: String = "",
    guestWelcomeButtonText: String = "",
    onGuestWelcomeClick: () -> Unit = {},
    referralRewardAmount: Int = 20,
    onReferEarnClick: () -> Unit = {},
    announcements: List<com.example.dutype.models.Announcement> = emptyList(),
    onDismissAnnouncement: (String) -> Unit = {},
    birthdayService: BirthdayService,
    workerJobRequests: List<WorkerJobRequest> = emptyList(),
    updatingWorkerJobRequestId: String? = null,
    workerAvailability: WorkerAvailability = WorkerAvailability(),
    instantRequests: List<InstantRequest> = emptyList(),
    updatingInstantRequestId: String? = null,
    isLoadingInstantRequests: Boolean = false,
    instantHelpError: String? = null,
    onTurnOnAvailability: () -> Unit = {},
    onApplyInstantRequest: (InstantRequest) -> Unit = {},
    onCallInstantRequest: (InstantRequest) -> Unit = {},
    onAcceptWorkerJobRequest: (WorkerJobRequest) -> Unit = {},
    onRejectWorkerJobRequest: (WorkerJobRequest) -> Unit = {},
    onOpenWorkerJobRequest: (WorkerJobRequest) -> Unit = {}
,
    todayEarningsAmount: Double = 0.0,
    todayJobsDone: Int = 0,
    thisWeekEarningsAmount: Double = 0.0,
    weekJobsDone: Int = 0,
    ratingValue: Float = 0f,
    reviewCount: Int = 0
) {
    val workerHomeViewModel: com.example.dutype.viewmodels.WorkerHomeViewModel = hiltViewModel()
    val recentHires by workerHomeViewModel.uiState.collectAsState()
    
    // Birthday state
    var birthdayInfo by remember { mutableStateOf<BirthdayInfo?>(null) }
    var showBirthdayBanner by remember { mutableStateOf(false) }
    
    // Check birthday on init
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            if (!birthdayService.hasWishedToday(context, userId)) {
                val bday = birthdayService.checkIfBirthday(userId)
                if (bday != null) {
                    birthdayInfo = bday
                    showBirthdayBanner = true
                }
            }
        }
    }
    
    // Memoize filtered jobs to avoid recomputation on every recomposition
    val availableJobs = remember(jobListings, jobVacancyStatuses) {
        jobListings.filter { job ->
            jobVacancyStatuses[job.id] != JobVacancyStatus.FILLED
        }
    }
    
    // Memoize skill-matched jobs - prioritize jobs matching worker skills, then by distance
    val skillMatchedJobs = remember(availableJobs, userSkills) {
        // Jobs are already distance-enriched and sorted by ViewModel/engine.
        // Keep home preview concise; full list is available in All Jobs.
        availableJobs.take(5)
    }
    
    // Track scroll offset for location bar visibility
    val listState = rememberLazyListState()
    
    // Calculate scroll offset
    val scrollOffset = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.toFloat() * 1000f + listState.firstVisibleItemScrollOffset.toFloat()
        }
    }
    
    // Gradual fade: Location bar starts fading at 30px, fully hidden at 180px
    val targetAlpha = remember {
        derivedStateOf {
            val offset = scrollOffset.value
            when {
                offset < 30f -> 1f
                offset > 180f -> 0f
                else -> 1f - ((offset - 30f) / 150f)
            }
        }
    }
    
    // Animate alpha for smooth transition
    val locationBarAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetAlpha.value,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 150,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "locationBarAlpha"
    )
    
    // P1 FIX: Use snapshotFlow to debounce scroll offset changes (was firing 60x/sec)
    LaunchedEffect(Unit) {
        snapshotFlow { scrollOffset.value }
            .collect { offset -> onScrollOffsetChange(offset) }
    }

    // Notify parent about alpha for gradual fade
    LaunchedEffect(locationBarAlpha) {
        onLocationBarAlphaChange(locationBarAlpha)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrollable content - scrolls over the header
        ScrollAwareLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent), // Transparent to show purple background
            state = listState,
            contentPadding = PaddingValues(
                top = 160.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            if (showGuestWelcomeCard) {
                item {
                    GuestWelcomeBonusCard(
                        title = guestWelcomeTitle,
                        message = guestWelcomeMessage,
                        buttonText = stringResource(R.string.guest_welcome_claim_gift),
                        onClick = onGuestWelcomeClick,
                        onVariantImpression = { variant ->
                            timber.log.Timber.d("Welcome gift impression (worker) variant=%s", variant.name)
                        },
                        onVariantClick = { variant ->
                            timber.log.Timber.d("Welcome gift click (worker) variant=%s", variant.name)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                ReferEarnStripCard(
                    rewardAmount = referralRewardAmount,
                    onInviteClick = onReferEarnClick

                            item {
                                WorkerEarningsSummarySection(
                                    todayEarningsAmount = todayEarningsAmount,
                                    todayJobsDone = todayJobsDone,
                                    thisWeekEarningsAmount = thisWeekEarningsAmount,
                                    weekJobsDone = weekJobsDone,
                                    ratingValue = ratingValue,
                                    reviewCount = reviewCount
                                )
                            }
                )
            }

            if (announcements.isNotEmpty()) {
                item {
                    AnnouncementList(
                        announcements = announcements,
                        onDismiss = { announcementId ->
                            onDismissAnnouncement(announcementId)
                        },
                        onAction = { announcement ->
                            announcement.actionRoute?.let { route: String ->
                                DeepLinkHandler.handleAnnouncementAction(route, navController, context)
                            }
                        }
                    )
                }
            }

            if (
                (instantRequests.isNotEmpty() || isLoadingInstantRequests || !instantHelpError.isNullOrBlank())
            ) {
                item {
                    InstantRequestSection(
                        requests = instantRequests,
                        isAvailabilityOn = workerAvailability.isAvailable,
                        isLoading = isLoadingInstantRequests,
                        updatingRequestId = updatingInstantRequestId,
                        error = instantHelpError,
                        onGoOnline = onTurnOnAvailability,
                        onApply = onApplyInstantRequest,
                        onCall = onCallInstantRequest
                    )
                }
            }

            //  Birthday Banner - Shows if today is user's birthday
            if (showBirthdayBanner && birthdayInfo != null) {
                item {
                    BirthdayBanner(
                        userName = birthdayInfo!!.userName,
                        onDismiss = { showBirthdayBanner = false }
                    )
                }
            }

        if (workerJobRequests.isNotEmpty()) {
            item {
                WorkerJobRequestSection(
                    requests = workerJobRequests,
                    updatingRequestId = updatingWorkerJobRequestId,
                    onAccept = onAcceptWorkerJobRequest,
                    onReject = onRejectWorkerJobRequest,
                    onOpen = onOpenWorkerJobRequest
                )
            }
        }
        
        //  Recently Hired - Single centered chip with auto-scroll (no elevation)
        // COMMENTED OUT - User requested to hide this section
        /*
        if (recentHires.recentHires.isNotEmpty()) {
            item {
                val hiresList = remember(recentHires.recentHires) { recentHires.recentHires }
                var currentIndex by remember { mutableStateOf(0) }
                
                // Auto-scroll animation (like announcements)
                LaunchedEffect(hiresList.size) {
                    if (hiresList.isNotEmpty()) {
                        while (true) {
                            kotlinx.coroutines.delay(3000) // 3 seconds per hire
                            currentIndex = (currentIndex + 1) % hiresList.size
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(600)
                            ) + fadeIn(animationSpec = tween(600)) with
                            slideOutHorizontally(
                                targetOffsetX = { -it },
                                animationSpec = tween(600)
                            ) + fadeOut(animationSpec = tween(600))
                        },
                        label = "recently_hired_animation"
                    ) { index ->
                        val hire = hiresList.getOrNull(index)
                        if (hire != null) {
                            // Simple chip without elevation
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                        .background(com.example.dutype.ui.theme.WorkerColors.CardBackground)
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${hire.workerName} got ${hire.jobTitle}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        */
        
        // Section 1: Browse Categories (at the top) - transparent to show gradient
        // Location chips removed per design requirement
        /*
        item {
            TopLocationChipsSection(
                currentLocation = currentLocation,
                onLocationChipSelected = onLocationChipSelected
            )
        }
        */

        if (showEmptyJobsState) {
            item {
                EmptyJobsState(
                    modifier = Modifier.fillParentMaxHeight(0.65f),
                    navController = rootNavController,
                    currentLocationName = emptyJobsCurrentLocationName,
                    isAppliedAllVariant = emptyJobsIsAppliedAllVariant,
                    suggestedCities = emptyJobsSuggestedCities,
                    onCitySelected = onEmptyJobsCitySelected
                )
            }
        }

        if (!showEmptyJobsState) {
            // Section 3: Jobs For You (skill-matched) - transparent to show gradient
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)  // Transparent to show gradient background
                        .padding(vertical = 16.dp)
                ) {
                    RecommendedJobsSection(
                        jobs = skillMatchedJobs,
                        onViewAllClick = { navController.navigate("${Routes.WORKER_ALL_JOBS}?filter=All Jobs") },
                        savedJobsViewModel = savedJobsViewModel,
                        onNavigateToJob = onNavigateToJob,
                        sectionTitle = when {
                            workerAvailability.isAvailable -> stringResource(R.string.normal_vacancy_jobs)
                            userSkills.isNotEmpty() -> stringResource(R.string.jobs_for_you)
                            skillMatchedJobs.any { it.distance != null } -> stringResource(R.string.jobs_near_you)
                            else -> null
                        }
                    )
                }
            }
        }
        
        // Section 4: DutyPe Promise Carousel (at the bottom after jobs)
        item {
            DutyPePromiseCarousel()
        }

        // Footer: Made with love in Bharat (always shown).
        item {
            com.example.dutype.components.MadeWithLoveFooter()
        }
        
    }
    }
}

@Composable
private fun InstantRequestSection(
    requests: List<InstantRequest>,
    isAvailabilityOn: Boolean,
    isLoading: Boolean,
    updatingRequestId: String?,
    error: String?,
    onGoOnline: () -> Unit,
    onApply: (InstantRequest) -> Unit,
    onCall: (InstantRequest) -> Unit
) {
    if (!isLoading && requests.isEmpty() && error.isNullOrBlank()) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.instant_works_near_you),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = stringResource(R.string.instant_works_switch_on),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626))
            )
        }

        requests.take(5).forEach { request ->
            InstantRequestCard(
                request = request,
                isAvailabilityOn = isAvailabilityOn,
                isUpdating = updatingRequestId == request.requestId,
                onGoOnline = onGoOnline,
                onApply = { onApply(request) },
                onCall = { onCall(request) }
            )
        }
    }
}

@Composable
private fun InstantRequestCard(
    request: InstantRequest,
    isAvailabilityOn: Boolean,
    isUpdating: Boolean,
    onGoOnline: () -> Unit,
    onApply: () -> Unit,
    onCall: () -> Unit
) {
    val responseStatus = request.workerResponseStatus.trim().lowercase()
    val hasWorkerResponded = responseStatus in setOf("applied", "called", "accepted", "completed")
    val applyLabel = instantWorkerApplyButtonLabel(responseStatus)
    val workersNeededLabel = stringResource(R.string.urgent_workers_needed_count, request.workersNeeded)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFFFEDD5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = WorkerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.background(
                                color = Color(0xFFFFEDD5),
                                shape = RoundedCornerShape(20.dp)
                            )
                        ) {
                            Text(
                                text = instantNeedTypeLabel(request.needType),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9A3412),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Text(
                            text = buildString {
                                request.distanceKm?.let { append("%.1f km".format(it)) }
                                if (request.budgetText.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(request.budgetText)
                                }
                                if (request.workersNeeded > 1) {
                                    if (isNotEmpty()) append(" • ")
                                    append(workersNeededLabel)
                                }
                                if (isEmpty()) append(request.category)
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (request.addressText.isNotBlank()) {
                        Text(
                            text = request.addressText,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasWorkerResponded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = instantWorkerResponseMessage(responseStatus),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (!isAvailabilityOn) {
                OutlinedButton(
                    onClick = onGoOnline,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB))
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.go_online_accept_jobs),
                        color = Color(0xFF2563EB)
                    )
                }
            } else {
                Button(
                    onClick = onApply,
                    enabled = !isUpdating && !hasWorkerResponded,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C),
                        disabledContainerColor = if (hasWorkerResponded) Color(0xFFDCFCE7) else Color(0xFFE5E7EB),
                        disabledContentColor = if (hasWorkerResponded) Color(0xFF15803D) else Color(0xFF9CA3AF)
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        if (hasWorkerResponded) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(applyLabel)
                    }
                }

                Button(
                    onClick = onCall,
                    enabled = request.employerPhone.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.call))
                }
            }
        }
    }
}

@Composable
private fun instantNeedTypeLabel(value: String): String = when (value) {
    "urgent_now" -> stringResource(R.string.urgent_now)
    "today" -> stringResource(R.string.today)
    "scheduled" -> stringResource(R.string.urgent_tomorrow)
    else -> stringResource(R.string.today)
}

@Composable
private fun instantWorkerApplyButtonLabel(status: String): String = when (status) {
    "applied" -> stringResource(R.string.applied)
    "called" -> stringResource(R.string.contacted)
    "accepted" -> stringResource(R.string.accepted)
    "completed" -> stringResource(R.string.completed)
    else -> stringResource(R.string.apply)
}

@Composable
private fun instantWorkerResponseMessage(status: String): String = when (status) {
    "called" -> stringResource(R.string.contact_shared_no_apply)
    "accepted" -> stringResource(R.string.employer_selected_urgent)
    "completed" -> stringResource(R.string.urgent_work_completed)
    else -> stringResource(R.string.urgent_work_applied_no_apply)
}

@Composable
private fun ReferEarnStripCard(
    rewardAmount: Int,
    onInviteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F3FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = null,
                tint = Color(0xFF6D28D9),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.refer_earn_amount, rewardAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF312E81),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = stringResource(R.string.refer_invite_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                )
            }

            Button(
                onClick = onInviteClick,
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B21B6)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.invite_now),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WorkerJobRequestSection(
    requests: List<WorkerJobRequest>,
    updatingRequestId: String?,
    onAccept: (WorkerJobRequest) -> Unit,
    onReject: (WorkerJobRequest) -> Unit,
    onOpen: (WorkerJobRequest) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.urgent_employer_requests),
            style = MaterialTheme.typography.titleMedium.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        requests.take(3).forEach { request ->
            WorkerJobRequestCard(
                request = request,
                isUpdating = updatingRequestId == request.requestId,
                onAccept = { onAccept(request) },
                onReject = { onReject(request) },
                onOpen = { onOpen(request) }
            )
        }
    }
}

@Composable
private fun WorkerJobRequestCard(
    request: WorkerJobRequest,
    isUpdating: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.employerName.ifBlank { request.companyName },
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = request.jobTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = WorkerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            if (request.salary.isNotBlank()) append(request.salary)
                            if (request.salaryType.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(request.salaryType.lowercase().replaceFirstChar { it.titlecase() })
                            }
                            if (request.distanceKm != null) {
                                if (isNotEmpty()) append(" • ")
                                append("%.1f km".format(request.distanceKm))
                            }
                            if (isEmpty()) append("Tap to view job")
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF6B7280)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.reject))
                }

                Button(
                    onClick = onAccept,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.accept))
                }
            }
        }
    }
}

@Composable
internal fun TopLocationChipsSection(
    currentLocation: com.example.dutype.models.LocationData?,
    onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit
) {
    val chips = remember(currentLocation) {
        TopCityChips.buildTopLocationChips(currentLocation)
    }

    if (chips.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Top locations",
            style = MaterialTheme.typography.titleSmall.copy(
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chips) { chip ->
                FilterChip(
                    selected = false,
                    onClick = { onLocationChipSelected(chip) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(chip.label())
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground,
                        labelColor = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun RecommendedJobsSection(
    jobs: List<JobListing>,
    onViewAllClick: () -> Unit,
    savedJobsViewModel: SavedJobsViewModel,
    onNavigateToJob: (String) -> Unit,
    sectionTitle: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Section Header with "See all" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onViewAllClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle ?: stringResource(R.string.jobs_near_you),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                    fontSize = 17.sp
                )
            )
            
            // Arrow button - clean minimal style
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View All",
                tint = com.example.dutype.ui.theme.WorkerColors.IconPrimary,
                modifier = Modifier
                    .size(IconSizes.Standard)
                    .clickable { onViewAllClick() }
                    .padding(4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Job Cards - Show only 3, using regular JobCard (ad shows on back from JobDescription)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            jobs.forEach { job ->
                val id = job.id.ifEmpty { job.id }
                JobCard(
                    job = job,
                    isSaved = job.isSaved,
                    onSaveClick = {
                        if (job.isSaved) {
                            savedJobsViewModel.unsaveJob(job.id)
                        } else {
                            savedJobsViewModel.saveJob(job.id)
                        }
                    },
                    onCardClick = { onNavigateToJob(it) }
                )
            }
        }
    }
}

@Composable
fun BrowseCategoriesSection(
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    getCategoryBadge: (String) -> String? = { null }
) {
    // Categories with emojis - all use same light gray background
    val categories = listOf(
        CategoryItem("Delivery", "\uD83D\uDEB4"),
        CategoryItem("Shop Helper", "\uD83C\uDFEA"),
        CategoryItem("Housekeeping", "\uD83E\uDDF9"),
        CategoryItem("Construction", "\uD83D\uDC77"),
        CategoryItem("Events", "\uD83C\uDFAA"),
        CategoryItem("Kitchen", "\uD83C\uDF73"),
        CategoryItem("Driver", "\uD83D\uDE97"),
        CategoryItem("Security", "\uD83D\uDC82"),
        CategoryItem("Electrician", "\uD83D\uDCA1"),
        CategoryItem("Plumber", "\uD83D\uDD27"),
        CategoryItem("Sales", "🛍️"),
        CategoryItem("Telecaller", "📞"),
        CategoryItem("Teacher", "📚"),
        CategoryItem("Office Staff", "🗂️"),
        CategoryItem("Customer Support", "🎧"),
        CategoryItem("Field Work", "🧭"),
        CategoryItem("Finance", "🏦"),
        CategoryItem("Data Entry", "⌨️"),
        CategoryItem("Healthcare", "⚕️"),
        CategoryItem("Beautician", "💇")
    )
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Categories header with "See all" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onViewAllClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.categories),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )
            // "See all" text with arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = com.example.dutype.ui.theme.WorkerColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View All Categories",
                    tint = com.example.dutype.ui.theme.WorkerColors.IconPrimary,
                    modifier = Modifier.size(IconSizes.Standard)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Home preview keeps categories compact: 2 rows, then "See all".
        val visibleCategories = categories.take(10)
        val chunkedCategories = visibleCategories.chunked(5)
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            chunkedCategories.forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowCategories.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategoryClick(category.name) }
                        )
                    }
                    // Fill empty spaces if row has less than 5 items
                    repeat(5 - rowCategories.size) {
                        Spacer(modifier = Modifier.width(68.dp))
                    }
                }
            }
        }
    }
}

data class CategoryItem(
    val name: String,
    val emoji: String
)

@Composable
internal fun CategoryChip(
    category: CategoryItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(68.dp)
    ) {
        // Icon container - white background with subtle shadow
        Card(
            modifier = Modifier.size(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.emoji,
                    fontSize = 26.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Category name
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall.copy(
                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun DutyPePromiseCarousel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PromiseItemWithIcon(
                icon = Icons.Default.CheckCircle,
                title = "100% Free",
                subtitle = stringResource(R.string.no_charges),
                iconColor = Color(0xFF10B981),
                textColor = Color(0xFF111827)
            )
            
            PromiseItemWithIcon(
                icon = Icons.Default.Verified,
                title = stringResource(R.string.verified_label),
                subtitle = stringResource(R.string.safe_jobs),
                iconColor = Color(0xFF3B82F6),
                textColor = Color(0xFF111827)
            )
            
            PromiseItemWithIcon(
                icon = Icons.Default.Headset,
                title = stringResource(R.string.support),
                subtitle = "24/7 help",
                iconColor = Color(0xFFF59E0B),
                textColor = Color(0xFF111827)
            )
        }
    }
}

@Composable
fun PromiseItemWithIcon(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    iconColor: Color,
    textColor: Color = Color(0xFF111827)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(90.dp)
    ) {
        // Simple icon with solid color background
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = iconColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(IconSizes.Standard) // Material Design 3: 24dp
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Title text
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 13.sp
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        
        // Subtitle text
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun DynamicHeader(
    locationText: String,
    locationBarAlpha: Float,
    isLocationLoading: Boolean = false,
    unreadNotificationCount: Int = 0,
    isInstantAvailable: Boolean = false,
    isInstantAvailabilitySaving: Boolean = false,
    todayEarningsAmount: Double = 0.0,
    todayJobsDone: Int = 0,
    thisWeekEarningsAmount: Double = 0.0,
    weekJobsDone: Int = 0,
    ratingValue: Float = 0f,
    reviewCount: Int = 0,
    onInstantAvailabilityChange: (Boolean) -> Unit = {},
    onMapClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WorkerHomeHeaderTopColor,
                        WorkerHomeHeaderMidColor,
                        Color(0xFF5E86FF)
                    )
                ),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "DutyPe",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = Color.White
                        )
                    )

                    if (locationBarAlpha > 0.05f) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onLocationClick)
                                .padding(end = 6.dp)
                                .graphicsLayer { alpha = locationBarAlpha.coerceIn(0f, 1f) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLocationLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(9.dp),
                                    strokeWidth = 1.2.dp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            Text(
                                text = locationText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-3).dp, y = 7.dp)
                                .background(
                                    if (locationBarAlpha > 0.05f) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .graphicsLayer {
                                                    alpha = locationBarAlpha.coerceIn(0f, 1f)
                                                    translationY = -12f * (1f - locationBarAlpha)
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isInstantAvailable) Color(0xFF16A34A) else Color(0xFFE5E7EB)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(
                                                                if (isInstantAvailable) Color(0xFFD1FAE5) else Color(0xFFD1D5DB),
                                                                CircleShape
                                                            )
                                                    )
                                                    Column {
                                                        Text(
                                                            text = stringResource(R.string.instant_works_near_you),
                                                            style = MaterialTheme.typography.titleSmall.copy(
                                                                color = if (isInstantAvailable) Color.White else Color(0xFF374151),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        )
                                                        Text(
                                                            text = if (isInstantAvailable) stringResource(R.string.nearby_jobs_instantly) else stringResource(R.string.turn_on_to_get_jobs),
                                                            style = MaterialTheme.typography.bodySmall.copy(
                                                                color = if (isInstantAvailable) Color(0xFFDCFCE7) else Color(0xFF6B7280)
                                                            )
                                                        )
                                                    }
                                                }

                                                OutlinedButton(
                                                    onClick = { onInstantAvailabilityChange(!isInstantAvailable) },
                                                    enabled = !isInstantAvailabilitySaving,
                                                    shape = RoundedCornerShape(999.dp),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isInstantAvailable) Color(0xFFE2E8F0) else Color(0xFF9CA3AF)
                                                    ),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = if (isInstantAvailable) Color.White else Color(0xFF1F2937)
                                                    )
                                                ) {
                                                    if (isInstantAvailabilitySaving) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp,
                                                            color = if (isInstantAvailable) Color.White else Color(0xFF1F2937)
                                                        )
                                                    } else {
                                                        Text(if (isInstantAvailable) stringResource(R.string.go_offline) else stringResource(R.string.go_online))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.PowerSettingsNew,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerTopMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF4B5563),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF111827),
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6B7280))
        )
    }
}

@Composable
internal fun WorkerEarningsSummarySection(
    todayEarningsAmount: Double = 0.0,
    todayJobsDone: Int = 0,
    thisWeekEarningsAmount: Double = 0.0,
    weekJobsDone: Int = 0,
    ratingValue: Float = 0f,
    reviewCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Your Earnings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                fontSize = 18.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkerStatCard(
                title = "Today",
                value = formatRupeeCompact(todayEarningsAmount),
                subtitle = if (todayJobsDone == 1) "1 job" else "$todayJobsDone jobs",
                icon = Icons.Default.Work,
                color = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
            WorkerStatCard(
                title = "This Week",
                value = formatRupeeCompact(thisWeekEarningsAmount),
                subtitle = if (weekJobsDone == 1) "1 job" else "$weekJobsDone jobs",
                icon = Icons.Default.CalendarToday,
                color = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            WorkerStatCard(
                title = "Rating",
                value = String.format("%.1f", ratingValue),
                subtitle = if (reviewCount == 1) "1 review" else "$reviewCount reviews",
                icon = Icons.Default.Star,
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WorkerStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 20.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun formatRupeeCompact(amount: Double): String {
    return "₹${String.format("%.0f", amount.coerceAtLeast(0.0))}"
}


//@Preview(showBackground = true)
//@Composable
//fun WorkerHomeScreenPreview() {
//    WorkerHomeScreen(
//        navController = NavController(LocalContext.current),
//        onStatusBarColorChange = {}
//    )
//}
