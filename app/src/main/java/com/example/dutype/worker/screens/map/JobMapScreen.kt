package com.example.dutype.worker.screens.map

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.models.JobListing
import com.example.dutype.navigation.Routes
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.example.dutype.utils.LocationService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.compose.ui.res.stringResource
import com.dutype.app.R


/**
 * HYPER-LOCAL RADAR MAP - Jobs Near You
 * 
 * Premium Features:
 * - Uber-style pulsing animated markers based on urgency
 * - Distance filter (500m, 1km, 2km, 5km, All)
 * - Job count badge showing jobs in selected radius
 * - Dark gradient header for premium feel
 * - Enhanced job preview card with quick actions
 * - Category filter chips
 * - Walking/cycling distance indicators
 * 
 * REFACTORED: Removed ServiceProvider anti-pattern
 * LocationService is now injected via Hilt composable parameter
 * 
 * Based on DutyPe Feature Documentation
 */

// Distance filter options
enum class DistanceFilter(val meters: Int, val label: String, val icon: String) {
    WALKING_500M(500, "500m", "🚶"),
    WALKING_1KM(1000, "1km", "🚶"),
    CYCLING_2KM(2000, "2km", "🚴"),
    NEARBY_5KM(5000, "5km", "📍"),
    WITHIN_10KM(10000, "10km", "🚗"),
    ALL(Int.MAX_VALUE, "All", "🌍")
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JobMapScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    viewModel: FirestoreJobViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // LocationService accessed via FirestoreJobViewModel (proper DI pattern)
    val locationService = viewModel.locationService
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    val filteredJobs by viewModel.filteredJobs.collectAsState()
    var selectedJob by remember { mutableStateOf<JobListing?>(null) }
    var userLatitude by remember { mutableStateOf<Double?>(null) }
    var userLongitude by remember { mutableStateOf<Double?>(null) }
    var isLoadingLocation by remember { mutableStateOf(true) }
    var isMapReady by remember { mutableStateOf(false) }
    var selectedDistanceFilter by remember { mutableStateOf(DistanceFilter.NEARBY_5KM) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    
    // Camera position state for Google Maps
    val cameraPositionState = rememberCameraPositionState()
    
    // Location permission
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Default to Hyderabad if no location
    val defaultLatitude = 17.385044
    val defaultLongitude = 78.486671
    
    // Colors
    val primaryBlue = Color(0xFF2563EB)
    val urgentRed = Color(0xFFEF4444)
    val availableGreen = Color(0xFF10B981)
    val warningOrange = Color(0xFFF59E0B)
    
    // Get user location on launch
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            isLoadingLocation = true
            try {
                val location = locationRepository.getHighAccuracy(
                    timeoutMs = 10000L,
                    minAccuracyMeters = 50f
                )
                if (location != null) {
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    Timber.d("📍 Map: User location set to ${location.latitude}, ${location.longitude}")
                }
            } catch (e: Exception) {
                Timber.e(e, "📍 Map: Error getting location")
            } finally {
                isLoadingLocation = false
            }
        } else {
            locationPermissionState.launchPermissionRequest()
            isLoadingLocation = false
        }
    }
    
    // Load jobs
    LaunchedEffect(Unit) {
        viewModel.loadJobs()
    }
    
    // P0 FIX: Sort jobs by nearest-first using NearestJobsEngine
    // Then filter by category and distance radius
    val jobsWithCoordinates = remember(filteredJobs, selectedDistanceFilter, selectedCategory, userLatitude, userLongitude) {
        // Step 1: Get jobs sorted by nearest using NearestJobsEngine
        val sortedByDistance = if (userLatitude != null && userLongitude != null) {
            com.example.dutype.engine.NearestJobsEngine.getNearbyJobs(
                filteredJobs.filter { it.lat != 0.0 && it.lng != 0.0 },
                userLatitude!!,
                userLongitude!!
            )
        } else {
            filteredJobs.filter { it.lat != 0.0 && it.lng != 0.0 }
        }
        
        // Step 2: Apply category and distance filters (maintain sort order)
        sortedByDistance
            .filter { job -> selectedCategory == null || job.getCategory() == selectedCategory }
            .filter { job -> 
                val distanceMeters = (job.distance ?: 999.0) * 1000
                distanceMeters <= selectedDistanceFilter.meters
            }
    }
    
    // Get unique categories from jobs
    val categories = remember(filteredJobs) {
        filteredJobs.map { it.getCategory() }.distinct().sorted()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding() // Use system bars padding instead of fixed bottom padding
    ) {
        // Google Maps View with enhanced markers
        EnhancedGoogleMapView(
            modifier = Modifier.fillMaxSize(),
            jobs = jobsWithCoordinates,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            selectedJob = selectedJob,
            initialLatitude = userLatitude ?: defaultLatitude,
            initialLongitude = userLongitude ?: defaultLongitude,
            initialZoom = when (selectedDistanceFilter) {
                DistanceFilter.WALKING_500M -> 16f
                DistanceFilter.WALKING_1KM -> 15f
                DistanceFilter.CYCLING_2KM -> 14f
                DistanceFilter.NEARBY_5KM -> 13f
                DistanceFilter.WITHIN_10KM -> 12f
                DistanceFilter.ALL -> 11f
            },
            onMarkerClick = { job -> selectedJob = job },
            onMapReady = {
                isMapReady = true
                Timber.d("📍 Map: Google Maps ready")
            }
        )

        
        // Premium Header with gradient
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Main header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E293B)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.jobs_near_you_map),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsing indicator
                                PulsingDot(color = availableGreen, size = 8.dp)
                                Text(
                                    text = "${jobsWithCoordinates.size} ${stringResource(R.string.jobs_within)} ${selectedDistanceFilter.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                        
                        // Filter button
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Badge(
                                containerColor = if (selectedCategory != null) primaryBlue else Color.Transparent
                            ) {
                                Icon(
                                    Icons.Outlined.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (showFilters) primaryBlue else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                    
                    // Distance filter chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(DistanceFilter.entries) { filter ->
                            DistanceFilterChip(
                                filter = filter,
                                isSelected = selectedDistanceFilter == filter,
                                jobCount = jobsWithCoordinates.count { job ->
                                    val distMeters = (job.distance ?: Double.MAX_VALUE) * 1000
                                    distMeters <= filter.meters
                                },
                                onClick = { selectedDistanceFilter = filter }
                            )
                        }
                    }

                    
                    // Category filter (expandable)
                    AnimatedVisibility(visible = showFilters) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.filter_by_category),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text(stringResource(R.string.all)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF374151),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { 
                                            selectedCategory = if (selectedCategory == category) null else category 
                                        },
                                        label = { Text(category) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF374151),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        // My Location FAB
        FloatingActionButton(
            onClick = {
                userLatitude?.let { lat ->
                    userLongitude?.let { lng ->
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f),
                                durationMs = 500
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (selectedJob != null) 220.dp else 16.dp),
            containerColor = Color.White,
            contentColor = primaryBlue,
            shape = CircleShape
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
        }
        
        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val currentZoom = cameraPositionState.position.zoom
                        cameraPositionState.animate(
                            CameraUpdateFactory.zoomTo(currentZoom + 1f),
                            durationMs = 300
                        )
                    }
                },
                modifier = Modifier.size(44.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            }
            
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val currentZoom = cameraPositionState.position.zoom
                        cameraPositionState.animate(
                            CameraUpdateFactory.zoomTo(currentZoom - 1f),
                            durationMs = 300
                        )
                    }
                },
                modifier = Modifier.size(44.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard))
            }
        }
        
        // Selected Job Card (Enhanced)
        AnimatedVisibility(
            visible = selectedJob != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedJob?.let { job ->
                EnhancedJobMapCard(
                    job = job,
                    onViewDetails = {
                        val id = job.id.ifEmpty { job.id }
                        // Navigate directly to job details - ad shows on back from JobDescriptionScreen
                        navController.navigate(Routes.jobDetailRoute(id))
                    },
                    onCall = {
                        val phone = job.contactNumber
                        if (phone.isNotEmpty()) {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:$phone")
                            }
                            context.startActivity(intent)
                        }
                    },
                    onDismiss = { selectedJob = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        
        // Loading indicator
        if (uiState.isLoading || isLoadingLocation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = primaryBlue,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isLoadingLocation) stringResource(R.string.fetching_your_location) else stringResource(R.string.loading_nearby_jobs),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // No jobs message
        if (!uiState.isLoading && !isLoadingLocation && jobsWithCoordinates.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_jobs_within_distance, selectedDistanceFilter.label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.try_expanding_search),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val locationNavController = rootNavController ?: navController
                                kotlin.runCatching {
                                    locationNavController.navigate(Routes.MANUAL_LOCATION_ROUTE)
                                }.onFailure {
                                    Timber.e(it, "JobMapScreen: Failed to navigate to manual location route")
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.change_location))
                        }

                        Button(
                            onClick = { selectedDistanceFilter = DistanceFilter.ALL },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.show_all_jobs))
                        }
                    }
                }
            }
        }
    }
}


// Pulsing dot animation component
@Composable
private fun PulsingDot(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// Distance filter chip
@Composable
private fun DistanceFilterChip(
    filter: DistanceFilter,
    isSelected: Boolean,
    jobCount: Int,
    onClick: () -> Unit
) {
    val selectedColor = Color(0xFF374151) // Dark gray for selected state
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedColor else Color.White,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(filter.icon, fontSize = 14.sp)
            Text(
                text = filter.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
            if (jobCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "$jobCount",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}


// Enhanced job card with quick actions
@Composable
private fun EnhancedJobMapCard(
    job: JobListing,
    onViewDetails: () -> Unit,
    onCall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF2563EB)
    val urgentRed = Color(0xFFEF4444)
    val successGreen = Color(0xFF10B981)
    
    val isUrgent = job.urgency.equals("HIGH", ignoreCase = true)
    
    // Format salary display from schema fields
    val salaryDisplay = remember(job.salary, job.salaryType) {
        val amount = if (job.salary == job.salary.toLong().toDouble())
            job.salary.toLong().toString() else job.salary.toString()
        val period = when (job.salaryType.uppercase()) {
            "HOURLY" -> "/hour"
            "MONTHLY" -> "/month"
            else -> "/day"
        }
        "₹$amount$period"
    }
    
    Card(
        modifier = modifier.shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.LocalRoleColors.current.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with urgency badge and close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUrgent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = urgentRed.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            PulsingDot(color = urgentRed, size = 8.dp)
                            Text(
                                text = "URGENT - Hiring Today!",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = urgentRed
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = successGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "✓ Available",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = successGreen
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Job title and company
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pay
                InfoChip(
                    icon = "💰",
                    text = salaryDisplay,
                    backgroundColor = Color(0xFFF0FDF4),
                    textColor = Color(0xFF166534)
                )
                
                // Distance
                job.distance?.let { dist ->
                    InfoChip(
                        icon = if (dist < 1) "🚶" else "📍",
                        text = if (dist < 1) "${(dist * 1000).toInt()}m away" else "${"%.1f".format(dist)}km away",
                        backgroundColor = Color(0xFFF0F9FF),
                        textColor = Color(0xFF0369A1)
                    )
                }
                
                // Job type
                if (job.jobType.isNotEmpty()) {
                    InfoChip(
                        icon = "📅",
                        text = job.jobType,
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFF92400E)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Navigate button - Opens Google Maps for directions (driving default)
                OutlinedButton(
                    onClick = {
                        // Open Google Maps directions with driving as default mode
                        val uri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.lat},${job.lng}&travelmode=driving")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback to browser if Google Maps not installed
                            val browserUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${job.lat},${job.lng}&travelmode=driving")
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, browserUri))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = primaryBlue
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryBlue)
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.navigate), fontWeight = FontWeight.SemiBold)
                }
                
                // View details button
                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Text(stringResource(R.string.view_details), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Info chip component
@Composable
private fun InfoChip(
    icon: String,
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 12.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
