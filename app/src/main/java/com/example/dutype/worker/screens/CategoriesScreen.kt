package com.example.dutype.worker.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.OfflineBanner
import com.example.dutype.viewmodels.ConnectivityViewModel
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.CategoriesViewModel
import com.example.dutype.worker.components.JobCard
import kotlinx.coroutines.launch
import timber.log.Timber
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

/**
 * Categories Screen - Blinkit/Zepto style category browser
 * Left sidebar: All categories (only "All Jobs" shows count)
 * Right side: Jobs for selected category with infinite scroll pagination (15 at a time)
 */
@Composable
fun CategoriesScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    initialCategory: String? = null,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val viewModel: CategoriesViewModel = hiltViewModel()
    val savedJobsViewModel: com.example.dutype.viewmodels.SavedJobsViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLocation by viewModel.locationPreferences.currentLocation.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val normalizedInitialCategory = remember(initialCategory) {
        if (initialCategory.equals("All Jobs", ignoreCase = true)) "All" else initialCategory
    }
    
    // Selected category state - use initial category if provided
    var selectedCategory by remember { mutableStateOf(normalizedInitialCategory ?: "All") }
    
    // Track if initial load has been done
    var initialLoadDone by remember { mutableStateOf(false) }
    
    // Load initial jobs when screen opens
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        
        // 🚀 FAST LOADING: Use cached location, load jobs immediately
        val savedLocation = viewModel.locationPreferences.getSavedLocationIfFresh()
        if (savedLocation != null) {
            Timber.d("📍 CategoriesScreen: Using cached location - lat=${savedLocation.latitude}, lon=${savedLocation.longitude}")
            viewModel.setUserLocation(savedLocation.latitude, savedLocation.longitude)
        } else {
            Timber.d("📍 CategoriesScreen: No cached location - jobs will load without distance")
        }
        
        // Load jobs immediately (don't wait for location)
        val categoryToLoad = normalizedInitialCategory ?: "All"
        Timber.d("📦 CategoriesScreen: Loading category: $categoryToLoad")
        viewModel.loadJobsForCategory(categoryToLoad)
        initialLoadDone = true

        if (!viewModel.locationPreferences.isManualLocationLocked()) {
            launch {
                try {
                    // Avoid repeated GPS work when cached location is still fresh.
                    if (!viewModel.locationPreferences.isLocationFresh(5 * 60 * 1000L)) {
                        locationRepository.refresh { freshLocation ->
                            if (freshLocation != null) {
                                Timber.d("📍 CategoriesScreen: Fresh location received - re-sorting jobs by distance")
                                viewModel.setUserLocation(freshLocation.latitude, freshLocation.longitude)
                            }
                        }
                    } else {
                        Timber.d("📍 CategoriesScreen: Skipping GPS refresh - using fresh cached location")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "📍 CategoriesScreen: Failed to refresh location")
                }
            }
        } else {
            Timber.d("📍 CategoriesScreen: Manual location lock active - skipping background GPS refresh")
        }
    }
    
    // Load jobs when category changes (after initial load)
    LaunchedEffect(selectedCategory) {
        if (initialLoadDone) {
            Timber.d("📦 CategoriesScreen: Category changed to: $selectedCategory")
            viewModel.loadJobsForCategory(selectedCategory)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        // Offline banner at the very top
        val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
        val isOnline by connectivityViewModel.isOnline.collectAsState()
        OfflineBanner(isOffline = !isOnline)
        
        // Header
        CommonHeader(
                title = stringResource(R.string.all_categories),
                onBackClick = { navController.popBackStack() },
                showBackButton = true,
                backgroundColor = WorkerColors.CardBackground
            )
            
            // Main content - Split view
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Sidebar - Categories
                CategorySidebar(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFE5E7EB))
                )
                
                // Right Side - Jobs List
                JobsListSection(
                    jobs = uiState.jobs,
                    selectedCategory = selectedCategory,
                    navController = navController,
                    viewModel = viewModel,
                    savedJobsViewModel = savedJobsViewModel,
                    hasMore = uiState.hasMore,
                    isLoading = uiState.isLoading,
                    isLoadingMore = uiState.isLoadingMore,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
    }
}

@Composable
private fun CategorySidebar(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // All categories - no job counts shown
    val categories = listOf(
        CategoryDisplayItem("All", "📋")
    ) + JobCategory.entries.map { cat ->
        CategoryDisplayItem(cat.displayName, cat.icon)
    }
    
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(categories) { category ->
            CategoryItemView(
                category = category,
                isSelected = selectedCategory == category.name,
                onClick = { onCategorySelected(category.name) }
            )
        }
    }
}

@Composable
private fun CategoryItemView(
    category: CategoryDisplayItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFEDF8FF) else Color.Transparent,
        animationSpec = tween(200),
        label = "bgColor"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1F2937) else Color(0xFF6B7280),
        animationSpec = tween(200),
        label = "textColor"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji icon
        Text(
            text = category.icon,
            fontSize = 24.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Category name
        Text(
            text = category.name,
            style = AppTypography.labelSmall.copy(
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        // Selection indicator
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3B82F6))
            )
        }
    }
}


@Composable
private fun JobsListSection(
    jobs: List<JobListing>,
    selectedCategory: String,
    navController: NavController,
    viewModel: CategoriesViewModel,
    savedJobsViewModel: com.example.dutype.viewmodels.SavedJobsViewModel,
    hasMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var lastLoadTriggerToken by remember { mutableStateOf<String?>(null) }
    val shouldLoadMore by remember(jobs.size, hasMore, isLoading, isLoadingMore, selectedCategory) {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            jobs.isNotEmpty() &&
                listState.isScrollInProgress &&
                hasMore &&
                !isLoading &&
                !isLoadingMore &&
                lastVisibleItemIndex >= jobs.lastIndex
        }
    }
    
    // Reset scroll position when category changes
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(shouldLoadMore, jobs.size, selectedCategory) {
        if (shouldLoadMore) {
            val nextLoadToken = "$selectedCategory:${jobs.size}"
            if (nextLoadToken != lastLoadTriggerToken) {
                lastLoadTriggerToken = nextLoadToken
                Timber.d("📦 Categories: user reached last visible job for '$selectedCategory', requesting next page (token=$nextLoadToken)")
                viewModel.loadMoreJobs()
            }
        }
    }
    
    Column(modifier = modifier.background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)) {
        // Divider at top
        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
        
        // Loading state
        if (isLoading && jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color(0xFF1F2937),
                    strokeWidth = 2.dp
                )
            }
        }
        // Empty state
        else if (!isLoading && jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = Color(0xFFD1D5DB),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedCategory == "All") "No nearby jobs right now" else "No $selectedCategory jobs nearby",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We checked 10km and 15km around your location. Try another area to unlock more jobs.",
                        style = AppTypography.bodySmall.copy(
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
        // Jobs list
        else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = jobs,
                    key = { job -> "${selectedCategory}_${job.id}" } // CRITICAL FIX: Include category in key to prevent conflicts
                ) { job ->
                    JobCard(
                        job = job,
                        isSaved = job.isSaved,
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { jobId ->
                            val currentlySaved = job.isSaved
                            if (currentlySaved) {
                                savedJobsViewModel.unsaveJob(jobId)
                            } else {
                                savedJobsViewModel.saveJob(jobId)
                            }
                        }
                    )
                }

                if (hasMore && jobs.isNotEmpty()) {
                    item(key = "categories_load_more_sentinel_$selectedCategory") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF1F2937),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Loading more jobs...",
                                    style = AppTypography.bodySmall.copy(color = Color(0xFF6B7280))
                                )
                            }
                        }
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Data class for category display
private data class CategoryDisplayItem(
    val name: String,
    val icon: String
)
