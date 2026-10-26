package com.example.dutype.worker.screens

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
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            scrollStateManager = scrollStateManager
        ) {
            item(key = "worker_home_header_item") {
                headerContent()
            }

            if (dynamicPromoSlides.isNotEmpty() || promoBannerUrl.isNotBlank()) {
                item(key = "worker_home_promo_carousel") {
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


            // DutyPe Services partner entry & home services booking in Khammam
            item(key = "worker_home_partner_entry") {
                com.example.dutype.homeservices.PartnerEntryIfInArea(
                    lat = currentLocation?.latitude ?: 17.2473,
                    lng = currentLocation?.longitude ?: 80.1514,
                    onClick = { rootNavController.navigate(com.example.dutype.navigation.Routes.PARTNER) },
                    onBookServices = { rootNavController.navigate(com.example.dutype.navigation.Routes.SERVICES) },
                    modifier = Modifier.padding(horizontal = 16.dp)
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

        // Section 4: DutyPe Promise Carousel (only shown when jobs are available, hidden on empty state)
        if (!showEmptyJobsState && availableJobs.isNotEmpty()) {
            item {
                DutyPePromiseCarousel()
            }
        }

        // Footer: Made with love in Bharat (always shown).
        item {
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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

/** Header row: DutyPe wordmark + location on the left, Duty ON/OFF switch (green when ON) + bell on the right. */
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.bg())
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 8.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: DutyPe wordmark + small location text underneath
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onLocationClick)
            ) {
                HomeBrandWordmark()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (isLocationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.2.dp,
                            color = HomeEmerald.fg()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = HomeEmerald.fg(),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = areaName.ifBlank { "Select Location" },
                        color = Color(0xFF64748B),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Duty ON/OFF switch (Green when ON) + Bell icon (No wallet icon)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOnlineChange != null) {
                    com.example.dutype.common.screens.AirbnbModernSwitch(
                        checked = isOnline,
                        onCheckedChange = onOnlineChange,
                        activeTrackColor = Color(0xFF10B981)
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

