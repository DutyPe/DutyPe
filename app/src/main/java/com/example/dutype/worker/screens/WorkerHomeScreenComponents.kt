package com.example.dutype.worker.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.components.AnnouncementList
import com.example.dutype.components.BirthdayBanner
import com.example.dutype.components.CategoryIcon
import com.example.dutype.components.GuestWelcomeBonusCard
import com.example.dutype.components.NotificationPermissionBottomSheet
import com.example.dutype.components.OfflineBanner
import com.example.dutype.components.ReusableSearchBar
import com.example.dutype.components.ScrollAwareLazyColumn
import com.example.dutype.components.WorkerHomeShimmer
import com.example.dutype.components.openNotificationSettings
import com.example.dutype.models.JobListing
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.WorkerAvailability
import com.example.dutype.navigation.Routes
import com.example.dutype.navigation.WorkerBottomRoutes
import com.example.dutype.location.TopCityChips
import com.example.dutype.services.BirthdayInfo
import com.example.dutype.services.BirthdayService
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.ui.theme.isAppInDarkTheme
import com.example.dutype.utils.DeepLinkHandler
import com.example.dutype.utils.GeoUtils
import com.example.dutype.utils.CategoryDetector
import com.example.dutype.utils.NotificationPermissionManager
import com.example.dutype.utils.ScrollStateManager
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.viewmodels.SmartJobApplicationViewModel
import com.example.dutype.viewmodels.SavedJobsViewModel
import com.example.dutype.worker.components.JobCard
import com.example.dutype.worker.components.WorkerHomeJobCard

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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EmptyJobsState(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    currentLocationName: String? = null,
    isAppliedAllVariant: Boolean = false,
    suggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onCitySelected: (TopCityChips.CityLocationChip) -> Unit = {},
    onHelpDesk: (() -> Unit)? = null,
    onBrowseAll: (() -> Unit)? = null
) {
    if (isAppliedAllVariant) {
        // Applied to everything nearby: a friendly pat on the back, not a dead end.
        val humorMessage = remember(currentLocationName) { WorkerAppliedAllHumorMessages.random() }
        com.example.dutype.components.DutyPeEmptyState(
            icon = Icons.Default.CheckCircle,
            art = com.example.dutype.components.EmptyArt.WAITING,
            title = stringResource(R.string.worker_you_are_on_top),
            message = humorMessage,
            primary = onBrowseAll?.let {
                com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.home_browse_all_jobs), onClick = it)
            },
            modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        return
    }
    val place = currentLocationName?.substringBefore(",")?.trim().orEmpty()
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        com.example.dutype.components.DutyPeEmptyState(
            icon = Icons.Default.Search,
            art = com.example.dutype.components.EmptyArt.SEARCH,
            title = if (place.isNotBlank()) stringResource(R.string.home_no_jobs_title_place, place) else stringResource(R.string.home_no_jobs_title),
            message = stringResource(R.string.home_no_jobs_body),
            primary = onBrowseAll?.let {
                com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.home_browse_all_jobs), onClick = it)
            },
            secondary = onHelpDesk?.let {
                com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.home_need_help), onClick = it)
            }
        )
        if (suggestedCities.isNotEmpty()) {
            Text(
                text = stringResource(R.string.home_try_other_cities),
                color = WorkerColors.TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                suggestedCities.take(6).forEach { city ->
                    Text(
                        text = city.city,
                        color = WorkerColors.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, WorkerColors.Border, RoundedCornerShape(999.dp))
                            .clickable { onCitySelected(city) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
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

    val accent = if (isAppliedAllVariant) WorkerColors.Success else Color(0xFF6366F1)
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
    // [error] is technical (exception text); people see a plain message instead.
    com.example.dutype.components.DutyPeEmptyScreen(
        icon = androidx.compose.material.icons.Icons.Outlined.ErrorOutline,
        art = com.example.dutype.components.EmptyArt.OFFLINE,
        title = stringResource(R.string.auto_oops_something_went_wrong),
        message = stringResource(R.string.job_load_failed_body),
        primary = com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.try_again), onClick = onRetry)
    )
}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeSectionsContent(
    jobListings: List<JobListing>,
    urgentJobs: List<JobListing> = emptyList(),
    isLoadingJobs: Boolean = false,
    navController: NavController,
    rootNavController: NavController,
    savedJobsViewModel: SavedJobsViewModel,
    currentLocation: com.example.dutype.models.LocationData? = null,
    onLocationChipSelected: (TopCityChips.CityLocationChip) -> Unit = {},
    hasLocationPermission: Boolean = false,
    context: android.content.Context,
    scrollStateManager: ScrollStateManager? = null,
    onJobClick: (String) -> Unit,
    onNavigateToJob: (String) -> Unit,
    userName: String = "",
    userEmail: String = "",
    userSkills: List<String> = emptyList(),
    onScrollOffsetChange: (Float) -> Unit = {},
    onLocationBarAlphaChange: (Float) -> Unit = {},
    headerHeightDp: androidx.compose.ui.unit.Dp = 180.dp,
    showEmptyJobsState: Boolean = false,
    jobsFailed: Boolean = false,
    onRetryJobs: () -> Unit = {},
    emptyJobsIsAppliedAllVariant: Boolean = false,
    emptyJobsCurrentLocationName: String? = null,
    emptyJobsSuggestedCities: List<TopCityChips.CityLocationChip> = emptyList(),
    onEmptyJobsCitySelected: (TopCityChips.CityLocationChip) -> Unit = {},
    showGuestWelcomeCard: Boolean = false,
    guestWelcomeTitle: String = "",
    guestWelcomeMessage: String = "",
    guestWelcomeButtonText: String = "",
    onGuestWelcomeClick: () -> Unit = {},
    announcements: List<com.example.dutype.models.Announcement> = emptyList(),
    onDismissAnnouncement: (String) -> Unit = {},
    birthdayService: BirthdayService,
    instantRequests: List<InstantRequest> = emptyList(),
    updatingInstantRequestId: String? = null,
    isLoadingInstantRequests: Boolean = false,
    instantHelpError: String? = null,
    showOnlineToggle: Boolean = false,
    isOnline: Boolean = false,
    isSavingOnline: Boolean = false,
    onOnlineChange: (Boolean) -> Unit = {},
    onApplyInstantRequest: (InstantRequest) -> Unit = {},
    onCallInstantRequest: (InstantRequest) -> Unit = {},
    todayEarningsAmount: Double = 0.0,
    todayJobsDone: Int = 0,
    thisWeekEarningsAmount: Double = 0.0,
    weekJobsDone: Int = 0,
    ratingValue: Float = 0f,
    reviewCount: Int = 0,
    appliedJobsCount: Int = 0,
    promoBannerUrl: String = "",
    onRequestLocationPermission: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onCategoryTap: (String) -> Unit = {},
    headerContent: @Composable () -> Unit = {}
) {

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
    val availableJobs = jobListings

    // Memoize skill-matched jobs - prioritize jobs matching worker skills, then by distance
    // Instant / urgent jobs (urgency == HIGH), nearest-first, shown in their own section on top.
    val urgentPreview = remember(urgentJobs) { urgentJobs.take(3) }
    val skillMatchedJobs = remember(availableJobs, urgentPreview, userSkills) {
        // Jobs are already distance-enriched and sorted nearest-first by the ViewModel.
        // Keep home preview concise; full list is available in All Jobs.
        // Jobs already shown in the urgent section are not repeated right below it.
        // The 5 nearest jobs are always shown, even when they also appear in the urgent section.
        availableJobs.take(5)
    }

    // Hidden for now: the "jobs near you · view map" hero card (the map tab is hidden too).
    // val newInLastHour = remember(availableJobs) {
    //     val cutoff = System.currentTimeMillis() - 60L * 60L * 1000L
    //     availableJobs.count { it.createdAt >= cutoff }
    // }
    val showHeroAndCategories = hasLocationPermission && !showEmptyJobsState

    // Track scroll offset for location bar visibility
    val listState = rememberLazyListState()

    // Calculate scroll offset
    val scrollOffset = remember {
        derivedStateOf {
            listState.firstVisibleItemIndex.toFloat() * 1000f + listState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    // Keep location bar always visible during scroll - do not fade it
    val targetAlpha = remember {
        derivedStateOf {
            1f  // Always fully visible
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
        // Scrollable content - header and all body sections scroll together seamlessly
        ScrollAwareLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent), // Transparent to show background
            state = listState,
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            item(key = "worker_home_header_item") {
                headerContent()
            }
            if (FirebaseAuth.getInstance().currentUser != null) {
                item(key = "worker_home_partner_entry") {
                    com.example.dutype.homeservices.PartnerEntryCard(
                        onClick = { rootNavController.navigate(com.example.dutype.navigation.Routes.PARTNER) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            if (showOnlineToggle) {
                item(key = "worker_home_online_toggle") {
                    com.example.dutype.worker.components.OnlineToggleCard(
                        isOnline = isOnline,
                        isSaving = isSavingOnline,
                        onChange = onOnlineChange,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

        if (promoBannerUrl.isNotBlank() || announcements.isNotEmpty()) {
            item(key = "worker_home_announcements") {
                AnnouncementList(
                    announcements = announcements,
                    promoBannerUrl = promoBannerUrl,
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

            // Shortcuts only once there is something to browse: hidden while there are no jobs here
            // and while location is off, so the empty / permission state is the clear next step.
            if (hasLocationPermission && !showEmptyJobsState && !jobsFailed && availableJobs.isNotEmpty()) item(key = "worker_home_quick_actions") {
                HomeQuickActionsGrid(
                    onBrowseJobs = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                    onAppliedJobs = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.MY_JOBS) },
                    onEarnings = { navController.navigate(Routes.WORKER_EARNINGS) },
                    onHelpDesk = { navController.navigate(Routes.HELP) }
                )
            }

            // Hidden for now: the "jobs near you · view map" hero card (the map tab is hidden too).
            // if (showHeroAndCategories) {
            //     item(key = "worker_home_hero") {
            //         HomeHeroSection(
            //             jobsCount = availableJobs.size,
            //             newInLastHour = newInLastHour,
            //             hasLocation = currentLocation != null,
            //             onMapClick = onMapClick
            //         )
            //     }
            // }

            if (
                (instantRequests.isNotEmpty() || isLoadingInstantRequests || !instantHelpError.isNullOrBlank())
            ) {
                item {
                    InstantRequestSection(
                        requests = instantRequests,
                        isLoading = isLoadingInstantRequests,
                        updatingRequestId = updatingInstantRequestId,
                        error = instantHelpError,
                        onApply = onApplyInstantRequest,
                        onCall = onCallInstantRequest
                    )
                }
            }

            if (hasLocationPermission && urgentPreview.isNotEmpty()) {
                item(key = "worker_home_urgent_jobs") {
                    HomeUrgentJobsSection(
                        jobs = urgentPreview,
                        savedJobsViewModel = savedJobsViewModel,
                        onViewAllClick = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                        onNavigateToJob = onNavigateToJob
                    )
                }
            }

            //  Birthday Banner - Shows if today is user's birthday
            if (showBirthdayBanner && birthdayInfo != null) {
                item {
                    BirthdayBanner(
                        userName = birthdayInfo!!.userName,
                        onDismiss = {
                            showBirthdayBanner = false
                            FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                                birthdayService.markWishedToday(context, userId)
                            }
                        }
                    )
                }
            }



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

        if (!hasLocationPermission) {
            // Location permission not granted -> show friendly empty state requiring location
            item {
                com.example.dutype.components.LocationPermissionRequiredState(
                    onRequestPermissionClick = onRequestLocationPermission
                )
            }
        } else if (jobsFailed) {
            // Could not load: the header and location stay; the message sits where jobs would be.
            item(key = "worker_home_jobs_error") {
                com.example.dutype.components.DutyPeEmptyState(
                    icon = androidx.compose.material.icons.Icons.Outlined.ErrorOutline,
                    art = com.example.dutype.components.EmptyArt.OFFLINE,
                    title = stringResource(R.string.auto_oops_something_went_wrong),
                    message = stringResource(R.string.job_load_failed_body),
                    primary = com.example.dutype.components.EmptyStateAction(label = stringResource(R.string.try_again), onClick = onRetryJobs),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        } else if (showEmptyJobsState) {
            item {
                EmptyJobsState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    navController = rootNavController,
                    currentLocationName = emptyJobsCurrentLocationName,
                    isAppliedAllVariant = emptyJobsIsAppliedAllVariant,
                    suggestedCities = emptyJobsSuggestedCities,
                    onCitySelected = onEmptyJobsCitySelected,
                    onHelpDesk = { navController.navigate(Routes.HELP) },
                    onBrowseAll = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) }
                )
            }
        } else if (skillMatchedJobs.isEmpty()) {
            // Jobs still loading (urgent jobs may already be on screen): skeleton, not a blank gap.
            if (isLoadingJobs) {
                item(key = "worker_home_jobs_skeleton") {
                    HomeJobsSkeleton()
                }
            }
        } else {
            // Section 3: Jobs For You (skill-matched) - transparent to show gradient
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    RecommendedJobsSection(
                        jobs = skillMatchedJobs,
                        onViewAllClick = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                        savedJobsViewModel = savedJobsViewModel,
                        onNavigateToJob = onNavigateToJob,
                        // Nothing within 20 km: say where these jobs are ("More jobs in Khammam district").
                        sectionTitle = skillMatchedJobs.first().let { first ->
                            val section = first.feedSection
                            when {
                                section != null && !section.isNearby -> com.example.dutype.worker.components
                                    .feedSectionTitle(section, first.district, first.state)
                                skillMatchedJobs.any { it.distance != null } -> stringResource(R.string.worker_nearest_jobs)
                                else -> stringResource(R.string.jobs_for_you)
                            }
                        }
                    )
                    if (isLoadingJobs && skillMatchedJobs.size < 5) {
                        HomeJobsSkeleton()
                    }
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
private fun AppliedJobsSummaryCard(
    appliedJobsCount: Int,
    onOpenMyJobs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(WorkerColors.ChipBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = WorkerColors.Primary.fg(),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.applied_jobs),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = stringResource(R.string.worker_applied_jobs_tracked, appliedJobsCount),
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(onClick = onOpenMyJobs) {
                Text(stringResource(R.string.my_jobs), color = WorkerColors.Primary.fg())
            }
        }
    }
}

@Composable
private fun InstantRequestSection(
    requests: List<InstantRequest>,
    isLoading: Boolean,
    updatingRequestId: String?,
    error: String?,
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
                    text = stringResource(R.string.worker_instant_work_close_employers),
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary),
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
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.Error)
            )
        }

        requests.take(5).forEach { request ->
            InstantRequestCard(
                request = request,
                isUpdating = updatingRequestId == request.requestId,
                onApply = { onApply(request) },
                onCall = { onCall(request) }
            )
        }
    }
}

@Composable
private fun InstantRequestCard(
    request: InstantRequest,
    isUpdating: Boolean,
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
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
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
                        .background(Color(0xFFFFEDD5).bg(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFFEA580C).fg(),
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
                                color = Color(0xFFFFEDD5).fg(),
                                shape = RoundedCornerShape(20.dp)
                            )
                        ) {
                            Text(
                                text = instantNeedTypeLabel(request.needType),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF9A3412).fg(),
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
                            style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (request.addressText.isNotBlank()) {
                        Text(
                            text = request.addressText,
                            style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569).fg()),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasWorkerResponded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5).bg(), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF15803D).fg(),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = instantWorkerResponseMessage(responseStatus),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF166534).fg(),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            run {
                Button(
                    onClick = onApply,
                    enabled = !isUpdating && !hasWorkerResponded,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C).bg(),
                        disabledContainerColor = if (hasWorkerResponded) Color(0xFFDCFCE7).bg() else Color(0xFFE5E7EB).bg(),
                        disabledContentColor = if (hasWorkerResponded) Color(0xFF15803D).fg() else Color(0xFF9CA3AF).fg()
                    )
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = WorkerColors.CardBackground)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg())
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
    else -> stringResource(R.string.urgent_offer_accept)
}

@Composable
private fun instantWorkerResponseMessage(status: String): String = when (status) {
    "called" -> stringResource(R.string.contact_shared_no_apply)
    "accepted" -> stringResource(R.string.employer_selected_urgent)
    "completed" -> stringResource(R.string.urgent_work_completed)
    else -> stringResource(R.string.urgent_work_applied_no_apply)
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
            text = stringResource(R.string.auto_top_locations),
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

/**
 * Instant / urgent jobs (urgency == HIGH) nearest-first. Reuses the standard home job card so the
 * URGENT tag renders exactly like everywhere else.
 */
@Composable
private fun HomeUrgentJobsSection(
    jobs: List<JobListing>,
    savedJobsViewModel: SavedJobsViewModel,
    onViewAllClick: () -> Unit,
    onNavigateToJob: (String) -> Unit
) {
    RecommendedJobsSection(
        jobs = jobs,
        onViewAllClick = onViewAllClick,
        savedJobsViewModel = savedJobsViewModel,
        onNavigateToJob = onNavigateToJob,
        sectionTitle = stringResource(R.string.urgent_work)
    )
}

/** Lightweight placeholder cards shown while the first jobs page loads. */
@Composable
internal fun HomeJobsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) { com.example.dutype.components.JobCardShimmer() }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle ?: stringResource(R.string.jobs_near_you),
                color = HomeInk.fg(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.auto_see_all),
                color = HomeEmerald.fg(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onViewAllClick() }
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            jobs.forEach { job ->
                WorkerHomeJobCard(
                    job = job,
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
                    text = stringResource(R.string.auto_see_all),
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
                color = Color.White,
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
                title = stringResource(R.string.worker_promise_free),
                subtitle = stringResource(R.string.no_charges),
                iconColor = WorkerColors.Success,
                textColor = WorkerColors.TextPrimary
            )

            PromiseItemWithIcon(
                icon = Icons.Default.Verified,
                title = stringResource(R.string.verified_label),
                subtitle = stringResource(R.string.safe_jobs),
                iconColor = Color(0xFF3B82F6).fg(),
                textColor = WorkerColors.TextPrimary
            )

            PromiseItemWithIcon(
                icon = Icons.Default.Headset,
                title = stringResource(R.string.support),
                subtitle = stringResource(R.string.worker_promise_help_24_7),
                iconColor = Color(0xFFF59E0B).fg(),
                textColor = WorkerColors.TextPrimary
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
    textColor: Color = WorkerColors.TextPrimary
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
                    color = WorkerColors.TextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val HomeInk = Color(0xFF0F0F0F)
private val HomeNavy = Color(0xFF0F172A)
private val HomeSlate = Color(0xFF64748B)
private val HomeMuted = Color(0xFF94A3B8)
private val HomeBorder = Color(0xFFE2E8F0)
private val HomeEmerald = Color(0xFF10B981)
private val HomeRed = Color(0xFFDC2626)

// Home preview radius used by FirestoreJobViewModel.loadJobsSummaryForHome (50 km when a location is known).
private const val WORKER_HOME_RADIUS_KM = 50

/** DutyPe brand wordmark: "Duty" in ink + "Pe" in emerald. */
@Composable
private fun HomeBrandWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "DutyPe",
        color = Color.Black.fg(),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.sp,
        maxLines = 1,
        modifier = modifier
    )
}

/** Header row: DutyPe wordmark on the left, location chip + bell on the right. */
@Composable
internal fun WorkerHomeGreetingHeader(
    areaName: String,
    isLocationLoading: Boolean,
    unreadNotificationCount: Int,
    onNotificationClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeScreenBackground.bg())
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeBrandWordmark(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(10.dp))
            HomeLocationChip(
                areaName = areaName,
                isLoading = isLocationLoading,
                onClick = onLocationClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            HomeBellButton(
                hasUnread = unreadNotificationCount > 0,
                onClick = onNotificationClick
            )
        }
    }
}

private val HomeScreenBackground = Color(0xFFF8FAFC)

@Composable
private fun HomeLocationChip(areaName: String, isLoading: Boolean, onClick: () -> Unit) {
    val chipShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .height(36.dp)
            .widthIn(max = 170.dp)
            .clip(chipShape)
            .background(Color.White.bg())
            .border(1.dp, HomeBorder.bd(), chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(13.dp),
                strokeWidth = 1.5.dp,
                color = HomeEmerald.fg()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = HomeEmerald.fg(),
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = areaName,
            color = HomeInk.fg(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = HomeSlate.fg(),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun HomeBellButton(hasUnread: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.bg())
            .border(1.dp, HomeBorder.bd(), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = HomeInk.fg(),
            modifier = Modifier.size(18.dp)
        )
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 5.dp)
                    .size(10.dp)
                    .background(Color.White.bg(), CircleShape)
                    .padding(1.5.dp)
                    .background(HomeRed.bg(), CircleShape)
            )
        }
    }
}

/** Dark hero card: real nearby-jobs count + "View map" pill. */
@Composable
private fun HomeHeroCard(
    jobsCount: Int,
    newInLastHour: Int,
    hasLocation: Boolean,
    onMapClick: () -> Unit
) {
    val countText = if (jobsCount == 1) {
        stringResource(R.string.worker_stat_job_single)
    } else {
        stringResource(R.string.worker_stat_jobs_count, jobsCount)
    }
    val newText = if (newInLastHour > 0) stringResource(R.string.worker_new_in_last_hour, newInLastHour) else ""
    val withinText = if (hasLocation) stringResource(R.string.worker_within_radius, WORKER_HOME_RADIUS_KM) else ""
    val openAreaText = stringResource(R.string.worker_jobs_open_area)
    val subline = buildString {
        if (newText.isNotBlank()) append(newText)
        if (withinText.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(withinText)
        }
        if (isEmpty()) append(openAreaText)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HomeInk.bg())
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.worker_near_you_today), color = HomeMuted.fg(), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = countText,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subline, color = HomeEmerald.fg(), fontSize = 12.sp, maxLines = 2)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(HomeEmerald.bg())
                .clickable(onClick = onMapClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.worker_view_map),
                color = HomeInk.fg(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = HomeInk.fg(),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** Hero card (one LazyColumn item). */
@Composable
private fun HomeHeroSection(
    jobsCount: Int,
    newInLastHour: Int,
    hasLocation: Boolean,
    onMapClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp)
    ) {
        HomeHeroCard(
            jobsCount = jobsCount,
            newInLastHour = newInLastHour,
            hasLocation = hasLocation,
            onMapClick = onMapClick
        )
    }
}

private class HomeQuickAction(
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val container: Color,
    val tint: Color
)

/** 2x2 grid of quick-action cards (two Rows, 12dp gaps, 20dp side padding). */
@Composable
private fun HomeQuickActionsGrid(
    onBrowseJobs: () -> Unit,
    onAppliedJobs: () -> Unit,
    onEarnings: () -> Unit,
    onHelpDesk: () -> Unit
) {
    val browseAction = HomeQuickAction(
        label = stringResource(R.string.worker_action_browse_jobs),
        subtitle = stringResource(R.string.worker_action_browse_jobs_sub),
        icon = Icons.Default.Work,
        container = Color(0xFFF0FDF4),
        tint = Color(0xFF16A34A).fg()
    )
    val appliedAction = HomeQuickAction(
        label = stringResource(R.string.worker_action_applied_jobs),
        subtitle = stringResource(R.string.worker_action_applied_jobs_sub),
        icon = Icons.Default.CheckCircle,
        container = Color(0xFFEFF6FF),
        tint = Color(0xFF2563EB).fg()
    )
    val earningsAction = HomeQuickAction(
        label = stringResource(R.string.worker_action_earnings),
        subtitle = stringResource(R.string.worker_action_earnings_sub),
        icon = Icons.Default.AccountBalanceWallet,
        container = Color(0xFFFEF3C7),
        tint = Color(0xFFD97706).fg()
    )
    val helpAction = HomeQuickAction(
        label = stringResource(R.string.worker_action_help),
        subtitle = stringResource(R.string.worker_action_help_sub),
        icon = Icons.Default.Headset,
        container = Color(0xFFF1F5F9),
        tint = Color(0xFF0F172A).fg()
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeQuickActionCard(browseAction, onBrowseJobs, Modifier.weight(1f))
            HomeQuickActionCard(appliedAction, onAppliedJobs, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeQuickActionCard(earningsAction, onEarnings, Modifier.weight(1f))
            HomeQuickActionCard(helpAction, onHelpDesk, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeQuickActionCard(action: HomeQuickAction, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clip(cardShape)
            .background(Color.White.bg())
            .border(1.dp, HomeBorder.bd(), cardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(action.container.bg(), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = action.label,
                color = HomeInk.fg(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = action.subtitle,
                color = HomeSlate.fg(),
                fontSize = 12.sp,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    color = Color(0xFF4B5563).fg(),
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = WorkerColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(color = WorkerColors.TextSecondary)
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
            text = stringResource(R.string.auto_your_earnings),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A).fg(),
                fontSize = 18.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WorkerStatCard(
                title = stringResource(R.string.worker_stat_today),
                value = formatRupeeCompact(todayEarningsAmount),
                subtitle = if (todayJobsDone == 1) {
                    stringResource(R.string.worker_stat_job_single)
                } else {
                    stringResource(R.string.worker_stat_jobs_count, todayJobsDone)
                },
                icon = Icons.Default.Work,
                color = Color(0xFF16A34A).fg(),
                modifier = Modifier.weight(1f)
            )
            WorkerStatCard(
                title = stringResource(R.string.worker_stat_this_week),
                value = formatRupeeCompact(thisWeekEarningsAmount),
                subtitle = if (weekJobsDone == 1) {
                    stringResource(R.string.worker_stat_job_single)
                } else {
                    stringResource(R.string.worker_stat_jobs_count, weekJobsDone)
                },
                icon = Icons.Default.CalendarToday,
                color = Color(0xFF2563EB).fg(),
                modifier = Modifier.weight(1f)
            )
            WorkerStatCard(
                title = stringResource(R.string.worker_stat_rating),
                value = String.format("%.1f", ratingValue),
                subtitle = if (reviewCount == 1) {
                    stringResource(R.string.worker_stat_review_single)
                } else {
                    stringResource(R.string.worker_stat_reviews_count, reviewCount)
                },
                icon = Icons.Default.Star,
                color = Color(0xFFF59E0B).fg(),
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
        colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground)
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
                    color = Color(0xFF0F172A).fg(),
                    fontSize = 20.sp
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WorkerColors.TextSecondary,
                    fontSize = 12.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8).fg(),
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
