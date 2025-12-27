package com.example.dutype.worker.screens.map

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.dutype.utils.LocationService
import com.example.dutype.viewmodels.FirestoreJobViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ACCESSIBILITY FEATURE: Map-First Interface
 * 
 * Full-screen map showing job pins for workers who navigate by landmarks.
 * Workers recognize landmarks ("Oh, a job near the big temple") better than street names.
 * 
 * Uses Google Maps for native Android map experience.
 * 
 * Implemented: December 27, 2025
 * Updated to Google Maps: December 28, 2025
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JobMapScreen(
    navController: NavController,
    viewModel: FirestoreJobViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    
    // UI State
    val uiState by viewModel.uiState.collectAsState()
    var selectedJob by remember { mutableStateOf<JobListing?>(null) }
    var userLatitude by remember { mutableStateOf<Double?>(null) }
    var userLongitude by remember { mutableStateOf<Double?>(null) }
    var isLoadingLocation by remember { mutableStateOf(true) }
    var isMapReady by remember { mutableStateOf(false) }
    
    // Camera position state for Google Maps
    val cameraPositionState = rememberCameraPositionState()
    
    // Location permission
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    
    // Default to Hyderabad if no location
    val defaultLatitude = 17.385044
    val defaultLongitude = 78.486671
    
    // Get user location on launch
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            isLoadingLocation = true
            try {
                val location = locationService.getHighAccuracyLocation(
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
    
    // Debug: Log jobs with coordinates
    LaunchedEffect(uiState.jobs) {
        Timber.d("📍 JobMapScreen: Total jobs loaded: ${uiState.jobs.size}")
        uiState.jobs.forEach { job ->
            Timber.d("📍 JobMapScreen: Job '${job.title}' - lat=${job.latitude}, lng=${job.longitude}")
        }
    }
    
    // Filter jobs with valid coordinates
    val jobsWithCoordinates = uiState.jobs.filter { 
        it.latitude != 0.0 && it.longitude != 0.0 
    }
    
    // Debug: Log filtered jobs
    LaunchedEffect(jobsWithCoordinates) {
        Timber.d("📍 JobMapScreen: Jobs with coordinates: ${jobsWithCoordinates.size}")
    }
    
    // Colors
    val primaryBlue = Color(0xFF2563EB)
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Maps View
        GoogleMapView(
            modifier = Modifier.fillMaxSize(),
            jobs = jobsWithCoordinates,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            initialLatitude = userLatitude ?: defaultLatitude,
            initialLongitude = userLongitude ?: defaultLongitude,
            initialZoom = 14f,
            onMarkerClick = { job ->
                selectedJob = job
            },
            onMapReady = {
                isMapReady = true
                Timber.d("📍 Map: Google Maps ready")
            }
        )
        
        // Top Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.White.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jobs Near You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${jobsWithCoordinates.size} jobs on map",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
                
                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF10B981), label = "Available")
                    LegendItem(color = Color(0xFFEF4444), label = "Urgent")
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
                .padding(end = 16.dp, bottom = if (selectedJob != null) 220.dp else 100.dp)
                .navigationBarsPadding(),
            containerColor = Color.White,
            contentColor = primaryBlue
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
                modifier = Modifier.size(48.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
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
                modifier = Modifier.size(48.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
        }
        
        // Selected Job Card (Bottom Sheet style)
        AnimatedVisibility(
            visible = selectedJob != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedJob?.let { job ->
                JobMapCard(
                    job = job,
                    onViewDetails = {
                        navController.navigate(Routes.jobDetailRoute(job.id.ifEmpty { job.jobId }))
                    },
                    onDismiss = { selectedJob = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                )
            }
        }
        
        // Loading indicator
        if (uiState.isLoading || isLoadingLocation) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = primaryBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isLoadingLocation) "Getting your location..." else "Loading jobs...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // No jobs message
        if (!uiState.isLoading && jobsWithCoordinates.isEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📍", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No jobs with location data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try the list view to see all jobs",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6B7280)
        )
    }
}

/**
 * Job card shown when a marker is selected
 */
@Composable
private fun JobMapCard(
    job: JobListing,
    onViewDetails: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryBlue = Color(0xFF2563EB)
    
    Card(
        modifier = modifier.shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Urgency badge
                if (job.urgency == "URGENT") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = "🔥 URGENT",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Job title
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Company name
            Text(
                text = job.companyName.ifEmpty { job.company },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pay
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = job.payAmount.ifEmpty { "Negotiable" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
                
                // Distance (if available)
                job.distance?.let { dist ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📍", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (dist < 1) "${(dist * 1000).toInt()}m" else "${String.format("%.1f", dist)}km",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }
            
            // Landmark info (if available)
            val landmark = job.locationNearby.ifEmpty { job.area ?: "" }
            if (landmark.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏛️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Near: $landmark",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // View Details button
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Text("View Details", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
