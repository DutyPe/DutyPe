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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import com.example.dutype.utils.SalaryFormatter
import com.example.dutype.worker.components.jobCategoryEmoji
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
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

    // Memoize nearest jobs (top 5) and skill matched jobs (at least 5)
    val urgentPreview = remember(urgentJobs) { urgentJobs.take(3) }
    val nearestJobs = remember(availableJobs, urgentJobs) {
        val pool = (availableJobs + urgentJobs).distinctBy { it.id }
        if (pool.size >= 5) pool.take(12) else pool
    }
    val skillMatchedJobs = remember(availableJobs, userSkills) {
        val normalizedSkills = userSkills.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val matched = if (normalizedSkills.isNotEmpty()) {
            availableJobs.filter { job ->
                val cat = job.category.lowercase()
                val title = job.title.lowercase()
                normalizedSkills.any { s -> cat.contains(s) || title.contains(s) || s.contains(cat) }
            }
        } else emptyList()
        // Ensure at least 5 jobs: prioritize skill matches, then diverse jobs from the feed
        val remaining = availableJobs.filterNot { nearestJobs.contains(it) }
        val combined = (matched + remaining).distinctBy { it.id }
        if (combined.size >= 5) {
            combined.take(8)
        } else if (availableJobs.size > 5) {
            availableJobs.drop(5).take(5)
        } else {
            availableJobs
        }
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

            // User requirement: Until jobs or empty job state loads, do not show announcements or any feed content.
            val isContentReady = !isLoadingJobs && (jobListings.isNotEmpty() || showEmptyJobsState || jobsFailed)

            if (!isContentReady) {
                item(key = "worker_home_jobs_skeleton_initial") {
                    HomeJobsSkeleton()
                }
            } else {
            // Promotional / announcement carousel: rendered at top of feed matching Employer Home
            item(key = "worker_home_promo_carousel") {
                Box(modifier = Modifier.cascadeEnter(0)) {
                    com.example.dutype.components.PromotionalBannerCarousel(
                        isWorker = true,
                        customSlides = dynamicPromoSlides,
                        onAction = { target ->
                            when {
                                target == "partner" || target == "partner_services" || target.contains("partner") -> {
                                    rootNavController.navigate(com.example.dutype.navigation.Routes.PARTNER)
                                }
                                target == "services" -> {
                                    rootNavController.navigate(com.example.dutype.navigation.Routes.SERVICES)
                                }
                                target.startsWith("dutype://") || target.contains("://") || target.startsWith("showcase") ||
                                        target == "promote_business" || target.contains("promote-business") -> {
                                    DeepLinkHandler.handleAnnouncementAction(target, navController, context)
                                }
                                target == "jobs" -> com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS)
                                target == "profile" -> com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.PROFILE)
                                target == "refer_earn" -> navController.navigate(Routes.WORKER_REFER_EARN)
                                target == "spin_and_win" || target == "spin" -> navController.navigate(com.example.dutype.navigation.Routes.SPIN_AND_WIN)
                                else -> DeepLinkHandler.handleAnnouncementAction(target, navController, context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Sleek Single-Line Floating Ticker: 🟢 Ramesh K. received ₹1,400 (AC Technician · Mamillagudem) 12m ago
            item(key = "worker_home_payout_ticker") {
                Box(modifier = Modifier.cascadeEnter(index = 2)) {
                    WorkerPayoutTickerPill(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
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
        } else if (nearestJobs.isEmpty()) {
            // Jobs still loading (urgent jobs may already be on screen): skeleton, not a blank gap.
            if (isLoadingJobs) {
                item(key = "worker_home_jobs_skeleton") {
                    HomeJobsSkeleton()
                }
            }
        } else {
            // Section: Nearest Jobs
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    // Section: Nearest Jobs (Story Highlight viewing cards with cascade entrance)
                    Box(modifier = Modifier.cascadeEnter(index = 3)) {
                        RecommendedJobsSection(
                            jobs = nearestJobs,
                            onViewAllClick = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                            savedJobsViewModel = savedJobsViewModel,
                            onNavigateToJob = onNavigateToJob,
                            onOpenStory = onOpenStory,
                            sectionTitle = stringResource(R.string.worker_nearest_jobs)
                        )
                    }
                    // Section: Best Skill-Based Matching Card (matching Image 5 Cook card with cascade entrance)
                    val highlightedJobs = if (skillMatchedJobs.isNotEmpty()) skillMatchedJobs else nearestJobs
                    if (highlightedJobs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Box(modifier = Modifier.cascadeEnter(index = 4)) {
                            SkillMatchedJobsSection(
                                jobs = highlightedJobs,
                                onViewAllClick = { com.example.dutype.components.navigateToWorkerTab(navController, WorkerBottomRoutes.JOBS) },
                                savedJobsViewModel = savedJobsViewModel,
                                onNavigateToJob = onNavigateToJob,
                                sectionTitle = "Jobs Matching Your Skills"
                            )
                        }
                    }
                    if (isLoadingJobs && nearestJobs.size < 5) {
                        HomeJobsSkeleton()
                    }
                }
            }
        }

        // Section: Today's Earning Potential (High Demand work card)
        // User rule: Only show after location permission given AND when there are jobs in their location
        if (hasLocationPermission && !showEmptyJobsState && availableJobs.isNotEmpty()) {
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
        }

        // Section 4: DutyPe Promise Carousel (100% free, verified, support circles)
        if (!showEmptyJobsState && availableJobs.isNotEmpty()) {
            item {
                DutyPePromiseCarousel()
            }
        }

        // India's #1 local jobs app brand footer on Worker Home
        item(key = "worker_home_brand_footer") {
            com.example.dutype.components.MadeWithLoveFooter(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )
        }
        }

        item(key = "worker_home_bottom_spacer") {
            Spacer(modifier = Modifier.height(64.dp))
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
    if (jobs.isEmpty()) return

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
            // Circular arrow button
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

        // Horizontal Scrolling Row of 3:4 Vertical Story Highlight Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(jobs, key = { _, job -> job.id }) { idx, job ->
                Box(modifier = Modifier.cascadeEnter(index = idx.coerceAtMost(6))) {
                    com.example.dutype.worker.components.WorkerMockupJobCard(
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

/**
 * Best Skill-Based Matching Section: Shows the highlighted card exactly matching Image 5 (Cook card style)
 */
@Composable
fun SkillMatchedJobsSection(
    jobs: List<JobListing>,
    onViewAllClick: () -> Unit,
    savedJobsViewModel: SavedJobsViewModel,
    onNavigateToJob: (String) -> Unit,
    sectionTitle: String = "Jobs Matching Your Skills"
) {
    if (jobs.isEmpty()) return
    val displayedJobs = jobs.take(5)

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
                text = sectionTitle,
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

        Spacer(modifier = Modifier.height(12.dp))

        // Stacked vertically one after another (at least 5 jobs matching skills)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            displayedJobs.forEach { job ->
                HighlightedWorkerJobCard(
                    job = job,
                    isSaved = savedJobsViewModel.isJobSaved(job.id),
                    onSaveClick = { savedJobsViewModel.toggleSaveJob(job.id) },
                    onCardClick = { onNavigateToJob(job.id) }
                )
            }
        }
    }
}

/**
 * Highlighted Job Card matching Image 1 mockup:
 * - Circle avatar on left with category emoji or photo
 * - Title + Subtitle ("Gopi Restaurant · 15h · Verified ✓")
 * - Large bold rate ("₹500/hour paid hourly")
 * - Red location pin + distance
 * - 3 soft pill badges (Vacancy, Part-time, Urgent) + Black Apply Now button
 */
@Composable
fun HighlightedWorkerJobCard(
    job: JobListing,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payAmount = job.payAmount
    val payTypeShort = when (job.payType.uppercase()) {
        "MONTHLY" -> "month"
        "DAILY", "DAY" -> "day"
        "HOURLY" -> "hour"
        "WEEKLY" -> "week"
        else -> "month"
    }
    val payFrequency = when (job.payType.uppercase()) {
        "MONTHLY" -> "monthly"
        "DAILY", "DAY" -> "after shift"
        "HOURLY" -> "hourly"
        "WEEKLY" -> "weekly"
        else -> "monthly"
    }
    val timeAgo = remember(job.createdAt) {
        if (job.createdAt > 0L) com.example.dutype.utils.DateTimeUtils.formatRelativeTime(job.createdAt) else "15h"
    }
    val categoryEmoji = remember(job.category, job.title) {
        val cat = jobCategoryEmoji(job.category)
        if (cat != "🏢") cat else jobCategoryEmoji(job.title)
    }
    val locationDisplay = remember(job.area, job.distance) {
        val loc = job.area.ifBlank { job.district }.ifBlank { "Hyderabad" }
        if (job.distance != null) "$loc · ${String.format("%.1f", job.distance)} km away" else loc
    }
    val vacanciesCount = if (job.vacancies > 0) job.vacancies else 1
    val vacanciesText = "$vacanciesCount ${if (vacanciesCount == 1) "Vacancy" else "Vacancies"}"
    val workType = job.employmentType.ifBlank { "Part-time" }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Left Circle Avatar + Title & Verified Subtitle + Save Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Category / Company Avatar
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!job.photoUrl.isNullOrBlank()) {
                        com.example.dutype.components.OptimizedJobImage(
                            imageUrl = job.photoUrl,
                            contentDescription = job.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = categoryEmoji, fontSize = 25.sp)
                    }
                }

                Spacer(Modifier.width(12.dp))

                // Title + Subtitle Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.title,
                        color = Color(0xFF0F172A),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${job.companyName.ifBlank { "DutyPe Partner" }} · $timeAgo",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Heart Save Button with scale pulse (1.0f -> 1.35f -> 1.0f) and color morph
                val heartScale = remember { androidx.compose.animation.core.Animatable(1f) }
                LaunchedEffect(isSaved) {
                    if (isSaved) {
                        heartScale.snapTo(1f)
                        heartScale.animateTo(
                            targetValue = 1.35f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )
                        )
                        heartScale.animateTo(
                            targetValue = 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                }
                val heartColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSaved) Color(0xFFFF385C) else Color(0xFF94A3B8),
                    animationSpec = androidx.compose.animation.core.tween(250),
                    label = "heartColor"
                )

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Save job",
                        tint = heartColor,
                        modifier = Modifier
                            .size(21.5.dp)
                            .graphicsLayer {
                                scaleX = heartScale.value
                                scaleY = heartScale.value
                            }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 2: Large Bold Pay: ₹500/hour paid hourly
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "₹${SalaryFormatter.amount(payAmount)}/$payTypeShort",
                    color = Color(0xFF0F172A),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "paid $payFrequency",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Row 3: Red Location Pin + Address
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = locationDisplay,
                    color = Color(0xFF64748B),
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            // Row 4: Badges Row + Black Apply Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Soft Pill Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vacancy Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(
                            text = vacanciesText,
                            color = Color(0xFF1D4ED8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    // Part-time / Full-time Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = workType,
                            color = Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    // Urgent Pill (if urgent)
                    if (job.isUrgent) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "Urgent",
                                color = Color(0xFFB45309),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Black Apply Now Pill Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.clickable { onCardClick() }
                ) {
                    Text(
                        text = "Apply Now",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
 * Sleek Single-Line Floating Ticker Pill:
 * 🟢 Ramesh K. received ₹1,400 (AC Technician · Mamillagudem) 12m ago
 * Rotates every 4 seconds. Tap opens "Recent Direct Payouts" bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPayoutTickerPill(
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
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L) // Rotates every 4 seconds
            activeIndex = (activeIndex + 1) % sampleProofs.size
        }
    }

    val currentProof = sampleProofs[activeIndex]

    // Unboxed ticker: strictly single-line height, zero excess vertical space, smoothly sliding names in place
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(horizontal = 20.dp, vertical = 0.dp)
            .clickable { showBottomSheet = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing Green Live Dot
            Box(contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "payoutDotPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1100, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "payoutPulseScale"
                )
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = 0.35f
                        }
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Box(
                    modifier = Modifier
                        .size(6.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }

            // Rotating 1-line Marquee - Unboxed and strictly constrained to single-line height
            AnimatedContent(
                targetState = currentProof,
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn(animationSpec = tween(300)))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(300)))
                },
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds(),
                label = "payout_marquee"
            ) { proof ->
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))) {
                            append("${proof.name} received ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))) {
                            append("₹${java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("en-IN")).format(proof.amountEarnedToday)} ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF64748B))) {
                            append("(${proof.role} · ${proof.location}) ")
                        }
                        withStyle(SpanStyle(color = Color(0xFF94A3B8), fontSize = 10.5.sp)) {
                            append(proof.timeAgo)
                        }
                    },
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "View all payouts",
            tint = Color(0xFF94A3B8),
            modifier = Modifier
                .size(13.dp)
                .padding(start = 4.dp)
        )
    }

    // Recent Direct Payouts Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "💰 Recent Direct Payouts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(
                            text = "Live verified settlements • 100% Direct UPI • ₹0 Cut",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = "Verified ✓",
                            color = Color(0xFF047857),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // List of Recent Payouts
                sampleProofs.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(
                                    text = "${item.name} · ${item.location}",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "${item.role} • Direct UPI Transfer",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+₹${item.amountEarnedToday}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF059669)
                            )
                            Text(text = item.timeAgo, fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                }

                Spacer(Modifier.height(16.dp))

                // Bottom Guarantee Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "DutyPe Guarantee: Employers settle wages directly via UPI or cash on spot. Zero commission taken.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF475569),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpinAndWinEntryBanner(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onPlayClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Color(0xFFFBBF24), Color(0xFFF59E0B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎡", fontSize = 22.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Spin & Win Up to ₹100",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = "FREE",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Daily lucky wheel · Guaranteed coin prizes",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF10B981),
                    modifier = Modifier.clickable(onClick = onPlayClick)
                ) {
                    Text(
                        text = "Spin ▶",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
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
    onSwitchRoleClick: () -> Unit = {},
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOnlineChange != null) {
                    HomeOnlineDutySwitch(
                        isOnline = isOnline,
                        isSaving = isSavingOnline,
                        onCheckedChange = onOnlineChange
                    )
                }
                HomeBellButton(
                    hasUnread = unreadNotificationCount > 0,
                    onClick = onNotificationClick
                )
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
private fun HomeOnlineDutySwitch(
    isOnline: Boolean,
    isSaving: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isOnline) Color(0xFF10B981) else Color(0xFFE2E8F0),
        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "switchTrack"
    )
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isOnline) 22.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "switchThumb"
    )

    Box(
        modifier = Modifier
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg.bg())
            .border(1.dp, if (isOnline) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onCheckedChange(!isOnline) }
            .padding(2.5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(25.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(0.5.dp, Color(0xFFE2E8F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    strokeWidth = 2.dp,
                    color = if (isOnline) Color(0xFF10B981) else Color(0xFF64748B)
                )
            } else if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF94A3B8), CircleShape)
                )
            }
        }
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
            painter = painterResource(id = R.drawable.ic_bell_outline),
            contentDescription = "Notifications",
            tint = HomeInk.fg(),
            modifier = Modifier.size(20.dp)
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

