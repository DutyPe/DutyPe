package com.example.dutype.worker.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.employer.models.JobCategory
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.viewmodels.CategoriesViewModel
import com.example.dutype.worker.components.JobCard
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Categories Screen - Blinkit/Zepto style category browser
 * Left sidebar: All categories (only "All Jobs" shows count)
 * Right side: Jobs for selected category with infinite scroll pagination (15 at a time)
 */
@Composable
fun CategoriesScreen(
    navController: NavController,
    initialCategory: String? = null,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    val viewModel: CategoriesViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Selected category state - use initial category if provided
    var selectedCategory by remember { mutableStateOf(initialCategory ?: "All") }
    
    // Track if initial load has been done
    var initialLoadDone by remember { mutableStateOf(false) }
    
    // Load initial jobs when screen opens
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
        // Load the initial category (either from navigation or "All")
        val categoryToLoad = initialCategory ?: "All"
        Timber.d("📦 CategoriesScreen: Initial load for category: $categoryToLoad")
        viewModel.loadJobsForCategory(categoryToLoad)
        initialLoadDone = true
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
            .background(WorkerColors.ScreenBackground)
    ) {
        // Header
        CommonHeader(
            title = "All Categories",
            onBackClick = { navController.popBackStack() },
            showBackButton = true,
            backgroundColor = WorkerColors.CardBackground
        )
        
        // Main content - Split view
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Sidebar - Categories
            CategorySidebar(
                selectedCategory = selectedCategory,
                totalJobCount = uiState.totalJobsInDb,
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
    totalJobCount: Int,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // All categories - only "All" shows job count
    val categories = listOf(
        CategoryDisplayItem("All", "📋", totalJobCount, showCount = true)
    ) + JobCategory.entries.map { cat ->
        CategoryDisplayItem(cat.displayName, cat.icon, 0, showCount = false)
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
        
        // Job count badge - only show for "All" category
        if (category.showCount && category.jobCount > 0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "(${category.jobCount})",
                style = AppTypography.labelSmall.copy(
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
                    fontSize = 10.sp
                )
            )
        }
        
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
    hasMore: Boolean,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Infinite scroll - load more when near end
    LaunchedEffect(listState, hasMore, isLoadingMore, selectedCategory) {
        snapshotFlow { 
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3
        }.distinctUntilChanged().collect { shouldLoadMore ->
            if (shouldLoadMore && hasMore && !isLoadingMore && !isLoading) {
                Timber.d("📦 Categories: Loading more jobs for $selectedCategory...")
                viewModel.loadMoreJobs()
            }
        }
    }
    
    // Reset scroll position when category changes
    LaunchedEffect(selectedCategory) {
        listState.scrollToItem(0)
    }
    
    Column(modifier = modifier.background(WorkerColors.ScreenBackground)) {
        // Category header with job count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedCategory,
                style = AppTypography.sectionHeader.copy(
                    color = Color(0xFF1F2937),
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Job count chip
            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${jobs.size}${if (hasMore) "+" else ""} jobs",
                        style = AppTypography.labelSmall.copy(
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        
        // Divider
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
                        text = if (selectedCategory == "All") "No jobs available" else "No jobs in $selectedCategory",
                        style = AppTypography.bodyMedium.copy(
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check back later for new opportunities",
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
                items(jobs, key = { it.id }) { job ->
                    JobCard(
                        job = job,
                        onCardClick = { jobId ->
                            navController.navigate(Routes.jobDetailRoute(jobId))
                        },
                        onSaveClick = { /* Handle save */ }
                    )
                }
                
                // Loading indicator when loading more
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1F2937),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
                
                // End of list indicator
                if (!hasMore && jobs.isNotEmpty() && !isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You've seen all ${jobs.size} jobs",
                                style = AppTypography.labelSmall.copy(color = Color(0xFF9CA3AF))
                            )
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
    val icon: String,
    val jobCount: Int,
    val showCount: Boolean = false
)
