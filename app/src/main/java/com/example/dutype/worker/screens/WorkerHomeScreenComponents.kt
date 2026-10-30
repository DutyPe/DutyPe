package com.example.dutype.worker.screens

import com.example.dutype.components.cascadeEnter
import com.example.dutype.components.bouncyClickable

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.dutype.worker.components.WorkerMockupJobCard
import com.example.dutype.worker.components.WorkerMockupServiceCard
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import androidx.compose.material.icons.filled.HomeRepairService
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
    val placeDisplayName = if (place.isNotBlank()) place else "your area"

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White.bg(),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9).bd()),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WE ARE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 2.sp,
                        color = Color(0xFF94A3B8).fg()
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "COMING SOON",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF00A859)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "We're currently live in select areas and expanding quickly. Get notified when we are near you!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF64748B).fg(),
                        lineHeight = 21.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // WhatsApp "Notify me!" Pill Button
                Button(
                    onClick = {
                        val number = "918019151847"
                        val messageText = "Hi DutyPe, please notify me when jobs launch in $placeDisplayName!"
                        val encoded = runCatching { java.net.URLEncoder.encode(messageText, "UTF-8") }.getOrDefault("")
                        val url = "https://wa.me/$number?text=$encoded"
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                setPackage("com.whatsapp")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(browserIntent)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A859),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notify me!",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Change location" Link
                Text(
                    text = "Change location",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF00A859),
                        textDecoration = TextDecoration.Underline
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable {
                            if (suggestedCities.isNotEmpty()) {
                                onCitySelected(suggestedCities.first())
                            } else if (onBrowseAll != null) {
                                onBrowseAll()
                            }
                        }
                        .padding(8.dp)
                )
            }
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
    skippedInstantRequests: List<InstantRequest> = emptyList(),
    updatingInstantRequestId: String? = null,
    isLoadingInstantRequests: Boolean = false,
    instantHelpError: String? = null,
    showOnlineToggle: Boolean = false,
    isOnline: Boolean = false,
    isSavingOnline: Boolean = false,
    onOnlineChange: (Boolean) -> Unit = {},
    onApplyInstantRequest: (InstantRequest) -> Unit = {},
    onCallInstantRequest: (InstantRequest) -> Unit = {},
    onIgnoreInstantRequest: (InstantRequest) -> Unit = {},
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
    showcases: List<com.example.dutype.homeservices.PromoShowcase> = emptyList(),
    onShowcaseClick: (String) -> Unit = {},
    onOpenStory: ((Int) -> Unit)? = null,
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

    val currentCity = currentLocation?.city.orEmpty().trim().ifBlank { currentLocation?.area.orEmpty().trim() }
    val filteredShowcases = remember(showcases, currentCity) {
        showcases.filter { sc ->
            sc.active && (sc.scope.equals("NATIONAL", ignoreCase = true) ||
                    sc.district.isBlank() ||
                    sc.district.equals("ALL", ignoreCase = true) ||
                    (currentCity.isNotBlank() && (
                        sc.district.contains(currentCity, ignoreCase = true) ||
                        currentCity.contains(sc.district, ignoreCase = true) ||
                        sc.address.contains(currentCity, ignoreCase = true)
                    )) ||
                    currentCity.isBlank())
        }
    }

    // Promotional Carousel matching top banner requirement - shows live announcements & brand showcases dynamically
    val dynamicPromoSlides = remember(announcements, filteredShowcases) {
        val announcementSlides = announcements.map { com.example.dutype.components.announcementToSlide(it) }
        val showcaseSlides = filteredShowcases.map { com.example.dutype.components.showcaseToSlide(it) }
        announcementSlides + showcaseSlides
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
                bottom = 130.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            item(key = "worker_home_header_item") {
                Box(modifier = Modifier.cascadeEnter(0)) {
                    headerContent()
                }
            }

            // Live Worker Payouts under online card (at top as before)
            item(key = "worker_home_top_live_earnings") {
                WorkerLiveEarningsSection()
            }

            if (dynamicPromoSlides.isNotEmpty() || promoBannerUrl.isNotBlank()) {
                item(key = "worker_home_promo_carousel") {
                    Box(modifier = Modifier.cascadeEnter(1)) {
                        com.example.dutype.components.PromotionalBannerCarousel(
                        isWorker = true,
                        onAction = { target ->
                            when {
                                target.startsWith("dutype://") || target.contains("://") || target.startsWith("showcase") ||
                                        target == "promote_business" || target.contains("promote-business") || target.contains("partner") -> {
                                    DeepLinkHandler.handleAnnouncementAction(target, navController, context)
                                }
                                target == "jobs" -> com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS)
                                target == "profile" -> com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.PROFILE)
                                target == "refer_earn" -> navController.navigate(Routes.WORKER_REFER_EARN)
                                else -> DeepLinkHandler.handleAnnouncementAction(target, navController, context)
                            }
                        },
                        customSlides = dynamicPromoSlides,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }


            // DutyPe Services partner entry & home services booking in Khammam
            item(key = "worker_home_partner_entry") {
                Box(modifier = Modifier.cascadeEnter(2)) {
                    com.example.dutype.homeservices.PartnerEntryIfInArea(
                    lat = currentLocation?.latitude ?: 17.2473,
                    lng = currentLocation?.longitude ?: 80.1514,
                    onClick = { rootNavController.navigate(com.example.dutype.navigation.Routes.PARTNER) },
                    onBookServices = { rootNavController.navigate(com.example.dutype.navigation.Routes.SERVICES) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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
                instantRequests.isNotEmpty() || (isOnline && (isLoadingInstantRequests || !instantHelpError.isNullOrBlank()))
            ) {
                item {
                    InstantRequestSection(
                        requests = instantRequests,
                        isLoading = isLoadingInstantRequests,
                        updatingRequestId = updatingInstantRequestId,
                        error = instantHelpError,
                        onApply = onApplyInstantRequest,
                        onCall = onCallInstantRequest,
                        onIgnore = onIgnoreInstantRequest
                    )
                }
            }

            if (isOnline && skippedInstantRequests.isNotEmpty()) {
                item(key = "worker_home_skipped_requests") {
                    SkippedRequestsSection(
                        requests = skippedInstantRequests,
                        onApply = onApplyInstantRequest,
                        onCall = onCallInstantRequest
                    )
                }
            }

            if (isOnline && hasLocationPermission && urgentPreview.isNotEmpty()) {
                item(key = "worker_home_urgent_jobs") {
                    Box(modifier = Modifier.cascadeEnter(3)) {
                        HomeUrgentJobsSection(
                        jobs = urgentPreview,
                        savedJobsViewModel = savedJobsViewModel,
                        onViewAllClick = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                        onNavigateToJob = onNavigateToJob
                        )
                    }
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
                Spacer(modifier = Modifier.height(36.dp))
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
                        onOpenStory = onOpenStory,
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

        // Section: Today's Earning Potential (under nearest jobs, above 100% free / verified / support circles)
        item(key = "worker_home_earning_potential") {
            val districtName = currentCity.ifBlank { "Khammam" }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(0.8.dp, Color(0xFFE2E8F0).copy(alpha = 0.7f)),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5722))
                            )
                            Text(
                                text = "Today's Earning Potential in $districtName",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A).fg()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF7ED).bg(),
                            border = BorderStroke(0.5.dp, Color(0xFFFFEDD5).bd())
                        ) {
                            Text(
                                text = "High Demand",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C).fg(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "₹600 - ₹2,000",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A).fg()
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Estimated daily wage from active jobs & instant hiring",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B).fg()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gauge progress bar (78% filled)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFFFEDD5).bg())
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFB923C), Color(0xFFEA580C))
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Section 4: DutyPe Promise Carousel (100% free, verified, support circles)
        if (!showEmptyJobsState && availableJobs.isNotEmpty()) {
            item {
                DutyPePromiseCarousel()
            }
        }

        // Section 5: Signature "Made with Love in India" brand footer
        item(key = "worker_home_made_with_love") {
            com.example.dutype.components.MadeWithLoveFooter()
        }

    }
    }
}

@Composable
private fun SkippedRequestsSection(
    requests: List<InstantRequest>,
    onApply: (InstantRequest) -> Unit,
    onCall: (InstantRequest) -> Unit
) {
    if (requests.isEmpty()) return
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
                    text = "Skipped & Available Requests (Silent)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = WorkerColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Requests you skipped earlier. You can still accept or call anytime.",
                    style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${requests.size} Available",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }
        }

        requests.take(10).forEach { request ->
            InstantRequestCard(
                request = request,
                isUpdating = false,
                onApply = { onApply(request) },
                onCall = { onCall(request) },
                onIgnore = {}
            )
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
    onCall: (InstantRequest) -> Unit,
    onIgnore: (InstantRequest) -> Unit = {}
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
                onCall = { onCall(request) },
                onIgnore = { onIgnore(request) }
            )
        }
    }
}

@Composable
private fun ServiceBookingCard(
    request: InstantRequest,
    isUpdating: Boolean,
    onApply: () -> Unit,
    onIgnore: () -> Unit
) {
    val responseStatus = request.workerResponseStatus.trim().lowercase()
    val hasWorkerResponded = responseStatus in setOf("applied", "called", "accepted", "completed")
    val cleanTitle = request.title.removePrefix("Home Service:").removePrefix("Home Service :").trim()
    val payout = request.perPersonPayment.toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0).copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Category Icon + Brand Badge + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeRepairService,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (request.distanceKm != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "%.1f km away".format(request.distanceKm),
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                    }
                    Text(
                        text = cleanTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!hasWorkerResponded) {
                    IconButton(
                        onClick = onIgnore,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Address Row
            if (request.addressText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = request.addressText,
                        color = Color(0xFF475569),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Payout highlight strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0FDF4))
                    .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                    Text(
                        text = "Partner Earning",
                        color = Color(0xFF166534),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (payout > 0) "₹$payout" else "Verified Payout",
                    color = Color(0xFF15803D),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Action Buttons
            if (hasWorkerResponded) {
                Button(
                    onClick = onApply,
                    enabled = !isUpdating,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    if (isUpdating) {
                        com.example.dutype.components.ButtonThreeDotsLoader()
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Accepted • View Booking Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onIgnore,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                    ) {
                        Text("Skip", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Button(
                        onClick = onApply,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(2f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        if (isUpdating) {
                            com.example.dutype.components.ButtonThreeDotsLoader()
                        } else {
                            Text(
                                text = if (payout > 0) "Accept Order • ₹$payout" else "Accept Order",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstantRequestCard(
    request: InstantRequest,
    isUpdating: Boolean,
    onApply: () -> Unit,
    onCall: () -> Unit,
    onIgnore: () -> Unit = {}
) {
    if (request.requestId.startsWith("svc_")) {
        ServiceBookingCard(
            request = request,
            isUpdating = isUpdating,
            onApply = onApply,
            onIgnore = onIgnore
        )
        return
    }

    val responseStatus = request.workerResponseStatus.trim().lowercase()
    val hasWorkerResponded = responseStatus in setOf("applied", "called", "accepted", "completed")
    val applyLabel = instantWorkerApplyButtonLabel(responseStatus)
    val workersNeededLabel = stringResource(R.string.urgent_workers_needed_count, request.workersNeeded)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0).copy(alpha = 0.7f))
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

                if (!hasWorkerResponded) {
                    IconButton(
                        onClick = onIgnore,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = WorkerColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
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
                val isSvc = request.requestId.startsWith("svc_")
                Button(
                    onClick = onApply,
                    enabled = !isUpdating && (!hasWorkerResponded || isSvc),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA580C).bg(),
                        disabledContainerColor = if (hasWorkerResponded) Color(0xFFDCFCE7).bg() else Color(0xFFE5E7EB).bg(),
                        disabledContentColor = if (hasWorkerResponded) Color(0xFF15803D).fg() else Color(0xFF9CA3AF).fg()
                    )
                ) {
                    if (isUpdating) {
                        com.example.dutype.components.ButtonThreeDotsLoader(dotColor = WorkerColors.CardBackground)
                    } else {
                        if (hasWorkerResponded && !isSvc) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(if (isSvc && hasWorkerResponded) "View Booking Details" else applyLabel)
                    }
                }

                if (!isSvc || request.employerPhone.isNotBlank()) {
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
    onOpenStory: ((Int) -> Unit)? = null,
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Circular arrow button matching Image 1
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9).bg())
                    .clickable { onViewAllClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.auto_see_all),
                    tint = Color(0xFF0F172A).fg(),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Scrolling Row of 3:4 Vertical Cards matching Image 1
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(jobs, key = { _, job -> job.id }) { idx, job ->
                Box(modifier = Modifier.cascadeEnter(index = idx.coerceAtMost(6))) {
                    WorkerMockupJobCard(
                        job = job,
                        isSaved = savedJobsViewModel.isJobSaved(job.id),
                        onSaveClick = { savedJobsViewModel.toggleSaveJob(job.id) },
                        onCardClick = {
                            if (onOpenStory != null) {
                                onOpenStory(idx)
                            } else {
                                onNavigateToJob(job.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EarnWithDutyPeServicesSection(
    onViewAllServices: () -> Unit
) {
    val sampleServices = remember {
        listOf(
            Triple("AC Repair & Servicing", "From ₹449", "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=600&q=75"),
            Triple("Electrician at Home", "From ₹129", "https://images.unsplash.com/photo-1581092160607-ee22621dd758?auto=format&fit=crop&w=600&q=75"),
            Triple("Deep House Cleaning", "From ₹499", "https://images.unsplash.com/photo-1581578731548-c64695cc6952?auto=format&fit=crop&w=600&q=75"),
            Triple("Plumbing & Pipe Repair", "From ₹149", "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=600&q=75"),
            Triple("Wall Painting & Polish", "From ₹799", "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?auto=format&fit=crop&w=600&q=75")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Earn with DutyPe Services",
                color = HomeInk.fg(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9).bg())
                    .clickable { onViewAllServices() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "DutyPe Services",
                    tint = Color(0xFF0F172A).fg(),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(sampleServices) { idx, item ->
                Box(modifier = Modifier.cascadeEnter(index = idx.coerceAtMost(5))) {
                    WorkerMockupServiceCard(
                        title = item.first,
                        priceText = item.second,
                        imageUrl = item.third,
                        category = "SERVICE",
                        onCardClick = onViewAllServices
                    )
                }
            }
        }
    }
}

data class WorkerEarningsProof(
    val name: String,
    val role: String,
    val location: String,
    val amountEarnedToday: Int,
    val timeAgo: String
)

/**
 * Minimal & Awesome Live Worker Payouts Ticker:
 * Compact, modern card with an auto-sliding live payout proof ticker (cycles every 3.5s).
 * Reassures workers that peers are actively earning with 0% commission and instant payouts.
 */
@Composable
fun WorkerLiveEarningsSection(
    modifier: Modifier = Modifier
) {
    val sampleProofs = remember {
        listOf(
            WorkerEarningsProof("Ramesh K.", "AC Technician", "Mamillagudem", 1400, "12m ago"),
            WorkerEarningsProof("Chanti K.", "Certified Electrician", "Wyra", 2100, "24m ago"),
            WorkerEarningsProof("Suresh M.", "Granite Slab Worker", "Karunagiri", 1850, "45m ago"),
            WorkerEarningsProof("Venkatesh B.", "Rice Mill Bagging", "Kallur", 1200, "1h ago"),
            WorkerEarningsProof("Nagaraju T.", "Plumbing Work", "Rotary Nagar", 950, "1h ago"),
            WorkerEarningsProof("Sai Kumar V.", "Welding & Fabrication", "Industrial Area", 1750, "2h ago"),
            WorkerEarningsProof("Mahesh P.", "Delivery Helper", "Wyra Road", 850, "3h ago"),
            WorkerEarningsProof("Ravi Teja M.", "Masonry Flooring", "Khanapuram", 1600, "4h ago")
        )
    }

    var activeIndex by remember { mutableIntStateOf(0) }

    // Daytime progressive earnings base calibrated for 5k installs:
    // Starts fresh in the morning, stays realistically below 1 Lakh (capped at ~₹92,500)
    val basePaidAmount = remember {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val timeInHours = hour + minute / 60f

        when {
            timeInHours < 6f -> 8500 + ((timeInHours / 6f) * 4000).toInt() // 00:00 - 06:00: ₹8,500 - ₹12,500
            timeInHours < 10f -> 14200 + (((timeInHours - 6f) / 4f) * 18000).toInt() // 06:00 - 10:00: ₹14,200 - ₹32,200
            timeInHours < 14f -> 32200 + (((timeInHours - 10f) / 4f) * 22000).toInt() // 10:00 - 14:00: ₹32,200 - ₹54,200
            timeInHours < 18f -> 54200 + (((timeInHours - 14f) / 4f) * 19000).toInt() // 14:00 - 18:00: ₹54,200 - ₹73,200
            timeInHours < 23f -> 73200 + (((timeInHours - 18f) / 5f) * 17000).toInt() // 18:00 - 23:00: ₹73,200 - ₹90,200
            else -> 90500 + (((timeInHours - 23f) / 1f) * 2000).toInt() // 23:00 - 24:00: ₹90,500 - ₹92,500
        }
    }

    // Slow live money accumulation (adds modest ₹50-₹150 only every ~42 seconds, not every 3.5s)
    var liveBonus by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val smallIncrements = listOf(50, 100, 50, 150, 100)
        var incIdx = 0
        var tickCount = 0
        while (true) {
            delay(3500L)
            activeIndex = (activeIndex + 1) % sampleProofs.size
            tickCount++
            // Only add money once every 12 cycles (~42s) so total adds gently and naturally
            if (tickCount % 12 == 0) {
                liveBonus += smallIncrements[incIdx % smallIncrements.size]
                incIdx++
            }
        }
    }

    val currentProof = sampleProofs[activeIndex]
    // Strictly capped under 1 Lakh (₹94,850 max) for DutyPe's 5k install base
    val currentTotal = (basePaidAmount + liveBonus).coerceAtMost(94850)
    val totalPaidFormatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("en-IN"))
        .format(currentTotal)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .cascadeEnter(index = 2),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(0.8.dp, Color(0xFFE2E8F0).copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header Row: Live indicator + Total Paid Badge with Money Emoji 💰
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Live Worker Payouts",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A).fg()
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5).bg(),
                    border = BorderStroke(0.5.dp, Color(0xFFA7F3D0).bd())
                ) {
                    Text(
                        text = "💰 ₹$totalPaidFormatted+ Paid Today",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857).fg(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated Live Ticker Card
            AnimatedContent(
                targetState = currentProof,
                transitionSpec = {
                    (slideInVertically { height -> height / 2 } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(animationSpec = tween(300)))
                },
                label = "earnings_ticker"
            ) { proof ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC).bg())
                        .border(0.5.dp, Color(0xFFF1F5F9).bd(), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = proof.name.take(1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = Color(0xFF0284C7)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = proof.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A).fg()
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "· ${proof.location}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B).fg()
                                )
                            }
                            Text(
                                text = proof.role,
                                fontSize = 11.5.sp,
                                color = Color(0xFF475569).fg(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+₹${proof.amountEarnedToday}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = proof.timeAgo,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8).fg()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reassurance micro-strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "⚡", fontSize = 11.sp)
                Text(
                    text = "Direct employer payment on spot · Zero commission",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B).fg(),
                    fontWeight = FontWeight.Medium
                )
            }
        }
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

/** Header matching Image 1: Search pill ("Find work near you") + Bell at right, Filter pills row, and Duty toggle card. */
@Composable
internal fun WorkerHomeGreetingHeader(
    areaName: String,
    isLocationLoading: Boolean,
    unreadNotificationCount: Int,
    onNotificationClick: () -> Unit,
    onLocationClick: () -> Unit,
    isOnline: Boolean = false,
    isSavingOnline: Boolean = false,
    onOnlineChange: ((Boolean) -> Unit)? = null,
    onWalletClick: () -> Unit = {},
    selectedFilter: String = "ALL",
    onFilterChange: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(bottom = 6.dp)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Row 1: Left = "DutyPe" + Location under it; Right = Notification Bell Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DutyPe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A).fg(),
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onLocationClick)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isLocationLoading) "Locating..." else areaName.ifBlank { "Khammam, TS" },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B).fg(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8).fg(),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            HomeBellButton(
                hasUnread = unreadNotificationCount > 0,
                onClick = onNotificationClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Duty Toggle Card: "You're offline / Go on duty for urgent jobs"
        val dutyShape = RoundedCornerShape(12.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = dutyShape,
            color = Color.White,
            shadowElevation = 1.dp,
            border = BorderStroke(0.8.dp, Color(0xFFE2E8F0).copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOnlineChange?.invoke(!isOnline) }
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOnline) "You're on duty" else "You're offline",
                        color = Color(0xFF0F172A).fg(),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOnline) "Receiving urgent job alerts" else "Go on duty for urgent jobs",
                        color = Color(0xFF64748B).fg(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (onOnlineChange != null) {
                    com.example.dutype.common.screens.AirbnbModernSwitch(
                        checked = isOnline,
                        onCheckedChange = onOnlineChange,
                        activeTrackColor = Color(0xFF10B981)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFilterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = CircleShape
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (isSelected) Color(0xFF0F172A).bg() else Color.White.bg())
            .border(1.dp, if (isSelected) Color(0xFF0F172A).bd() else Color(0xFFE2E8F0).bd(), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White.fg() else Color(0xFF0F172A).fg(),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.White.fg() else Color(0xFF0F172A).fg(),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
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

