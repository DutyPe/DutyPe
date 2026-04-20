package com.example.dutype.worker.screens.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dutype.app.BuildConfig
import com.example.dutype.models.JobListing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

// Urgency colors for pulsing markers
private val UrgentRed = Color(0xFFEF4444)
private val UrgentOrange = Color(0xFFF59E0B)
private val AvailableGreen = Color(0xFF10B981)
private val FilledBlue = Color(0xFF3B82F6)

/**
 * Basic Google Maps View for Jobs Near You
 */
@Composable
fun GoogleMapView(
    modifier: Modifier = Modifier,
    jobs: List<JobListing>,
    userLatitude: Double?,
    userLongitude: Double?,
    initialLatitude: Double = 17.385044,
    initialLongitude: Double = 78.486671,
    initialZoom: Float = 14f,
    onMarkerClick: (JobListing) -> Unit = {},
    onMapReady: () -> Unit = {}
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(userLatitude ?: initialLatitude, userLongitude ?: initialLongitude),
            initialZoom
        )
    }
    
    LaunchedEffect(userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLatitude, userLongitude),
                    initialZoom
                )
            )
        }
    }
    
    val mapProperties by remember(userLatitude, userLongitude) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = userLatitude != null && userLongitude != null,
                mapType = MapType.NORMAL
            )
        )
    }
    
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false
            )
        )
    }
    
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapLoaded = { onMapReady() }
    ) {
        jobs.forEach { job ->
            val position = LatLng(job.lat, job.lng)
            
            val markerColor = when {
                job.urgency.equals("HIGH", ignoreCase = true) -> BitmapDescriptorFactory.HUE_RED
                job.distance != null && job.distance!! < 1.0 -> BitmapDescriptorFactory.HUE_GREEN
                else -> BitmapDescriptorFactory.HUE_AZURE
            }
            
            Marker(
                state = MarkerState(position = position),
                title = job.title,
                snippet = job.companyName,
                icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                onClick = {
                    onMarkerClick(job)
                    true
                }
            )
        }
    }
}

/**
 * Uber-Style Enhanced Google Maps View
 * Features:
 * - Auto-zoom based on nearest job distance
 * - Actual navigation route using Google Directions API
 * - Auto-show route to nearest job on load
 * - Job info chips with name + vacancy (vertical layout)
 */
@Composable
fun EnhancedGoogleMapView(
    modifier: Modifier = Modifier,
    jobs: List<JobListing>,
    userLatitude: Double?,
    userLongitude: Double?,
    selectedJob: JobListing? = null,
    initialLatitude: Double = 17.385044,
    initialLongitude: Double = 78.486671,
    initialZoom: Float = 14f,
    onMarkerClick: (JobListing) -> Unit = {},
    onMapReady: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Find nearest job for auto-route
    val nearestJob = remember(jobs, userLatitude, userLongitude) {
        if (userLatitude != null && userLongitude != null && jobs.isNotEmpty()) {
            jobs.minByOrNull { it.distance ?: Double.MAX_VALUE }
        } else null
    }
    
    // Job to show route for (selected or nearest)
    val routeJob = selectedJob ?: nearestJob
    
    // Route points from Directions API
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    
    // Fetch actual navigation route when job changes
    LaunchedEffect(routeJob, userLatitude, userLongitude) {
        if (routeJob != null && userLatitude != null && userLongitude != null) {
            isLoadingRoute = true
            try {
                val points = fetchDirectionsRoute(
                    originLat = userLatitude,
                    originLng = userLongitude,
                    destLat = routeJob.lat,
                    destLng = routeJob.lng
                )
                routePoints = points
                Timber.d("📍 Route fetched with ${points.size} points")
            } catch (e: Exception) {
                Timber.e(e, "📍 Failed to fetch route, using straight line")
                // Fallback to straight line
                routePoints = listOf(
                    LatLng(userLatitude, userLongitude),
                    LatLng(routeJob.lat, routeJob.lng)
                )
            } finally {
                isLoadingRoute = false
            }
        } else {
            routePoints = emptyList()
        }
    }
    
    // Calculate smart zoom based on nearest job distance
    val smartZoom = remember(nearestJob, userLatitude, userLongitude) {
        if (nearestJob != null && userLatitude != null && userLongitude != null) {
            val distanceKm = nearestJob.distance ?: 5.0
            when {
                distanceKm < 0.3 -> 17f
                distanceKm < 0.5 -> 16.5f
                distanceKm < 1.0 -> 16f
                distanceKm < 2.0 -> 15f
                distanceKm < 5.0 -> 14f
                else -> 13f
            }
        } else initialZoom
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(userLatitude ?: initialLatitude, userLongitude ?: initialLongitude),
            smartZoom
        )
    }
    
    // Auto-zoom to user location on first load
    var hasZoomedToUser by remember { mutableStateOf(false) }
    LaunchedEffect(userLatitude, userLongitude, smartZoom) {
        if (userLatitude != null && userLongitude != null && !hasZoomedToUser) {
            hasZoomedToUser = true
            delay(500)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLatitude, userLongitude),
                    smartZoom
                ),
                durationMs = 1000
            )
        }
    }
    
    // Zoom to show route when job is selected - Uber-style close view
    LaunchedEffect(selectedJob) {
        if (selectedJob != null && userLatitude != null && userLongitude != null) {
            val bounds = LatLngBounds.builder()
                .include(LatLng(userLatitude, userLongitude))
                .include(LatLng(selectedJob.lat, selectedJob.lng))
                .build()
            
            delay(300)
            // Use larger padding for closer view like Uber
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 180),
                durationMs = 800
            )
        }
    }
    
    val mapProperties by remember(userLatitude, userLongitude) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = false,
                mapType = MapType.NORMAL
            )
        )
    }
    
    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true,
                mapToolbarEnabled = false,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = false,
                scrollGesturesEnabled = true,
                zoomGesturesEnabled = true
            )
        )
    }
    
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapLoaded = {
            Timber.d("📍 EnhancedGoogleMap: Map loaded with ${jobs.size} jobs")
            onMapReady()
        }
    ) {
        // User location marker (blue dot)
        if (userLatitude != null && userLongitude != null) {
            val userPosition = LatLng(userLatitude, userLongitude)
            
            Marker(
                state = MarkerState(position = userPosition),
                title = stringResource(R.string.you_are_here),
                icon = createUserLocationMarker(),
                anchor = Offset(0.5f, 0.5f),
                zIndex = 100f
            )
        }
        
        // Navigation route polyline (Uber-style dark route)
        if (routePoints.isNotEmpty()) {
            // White border/shadow for visibility
            Polyline(
                points = routePoints,
                color = Color(0xFFFFFFFF),
                width = 16f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 0f
            )
            
            // Main dark route line (Uber-style black)
            Polyline(
                points = routePoints,
                color = Color(0xFF1A1A1A),
                width = 8f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 1f
            )
        }
        
        // Job markers with info chips
        jobs.forEach { job ->
            val position = LatLng(job.lat, job.lng)
            val isUrgent = job.urgency.equals("HIGH", ignoreCase = true)
            val isNearby = job.distance != null && job.distance!! < 1.0
            val isSelected = selectedJob?.id == job.id
            val isRouteTarget = routeJob?.id == job.id
            
            // Custom marker with job info chip
            val markerIcon = createJobMarkerChip(
                context = context,
                jobTitle = job.title.take(14) + if (job.title.length > 14) ".." else "",
                vacancy = 1, // vacancies not in schema — show 1 as default
                isUrgent = isUrgent,
                isNearby = isNearby,
                isSelected = isSelected || isRouteTarget
            )
            
            Marker(
                state = MarkerState(position = position),
                icon = markerIcon,
                anchor = Offset(0.5f, 1f),
                zIndex = if (isSelected || isRouteTarget) 99f else if (isUrgent) 50f else 10f,
                onClick = {
                    Timber.d("📍 Marker clicked: ${job.title}")
                    onMarkerClick(job)
                    true
                }
            )
        }
    }
}

/**
 * Fetch actual navigation route from Google Directions API
 */
private suspend fun fetchDirectionsRoute(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double
): List<LatLng> = withContext(Dispatchers.IO) {
    try {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY_HERE") {
            Timber.w("📍 No valid Maps API key, using straight line")
            return@withContext listOf(
                LatLng(originLat, originLng),
                LatLng(destLat, destLng)
            )
        }
        
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$originLat,$originLng" +
                "&destination=$destLat,$destLng" +
                "&mode=driving" +
                "&key=$apiKey"
        
        val response = URL(url).readText()
        val json = JSONObject(response)
        
        val status = json.getString("status")
        if (status != "OK") {
            Timber.w("📍 Directions API status: $status")
            return@withContext listOf(
                LatLng(originLat, originLng),
                LatLng(destLat, destLng)
            )
        }
        
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) {
            return@withContext listOf(
                LatLng(originLat, originLng),
                LatLng(destLat, destLng)
            )
        }
        
        val route = routes.getJSONObject(0)
        val overviewPolyline = route.getJSONObject("overview_polyline")
        val encodedPoints = overviewPolyline.getString("points")
        
        // Decode polyline
        decodePolyline(encodedPoints)
    } catch (e: Exception) {
        Timber.e(e, "📍 Error fetching directions")
        listOf(
            LatLng(originLat, originLng),
            LatLng(destLat, destLng)
        )
    }
}

/**
 * Decode Google's encoded polyline format
 */
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        
        poly.add(LatLng(lat / 1E5, lng / 1E5))
    }
    
    return poly
}

/**
 * Create custom user location marker (blue dot)
 */
@Composable
private fun createUserLocationMarker(): BitmapDescriptor {
    return remember {
        val size = 56
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // White outer circle
        val outerPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            setShadowLayer(6f, 0f, 2f, 0x40000000)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, outerPaint)
        
        // Blue inner circle
        val innerPaint = Paint().apply {
            color = 0xFF4285F4.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 10, innerPaint)
        
        // White center dot
        val centerPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, 6f, centerPaint)
        
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

/**
 * Create job marker chip with vertical layout: Job Name on top, Vacancy below
 */
private fun createJobMarkerChip(
    context: android.content.Context,
    jobTitle: String,
    vacancy: Int,
    isUrgent: Boolean,
    isNearby: Boolean,
    isSelected: Boolean
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    
    // Dimensions
    val padding = (10 * density).toInt()
    val cornerRadius = (12 * density)
    val pinHeight = (20 * density).toInt()
    
    // Text paints
    val titlePaint = Paint().apply {
        textSize = 11 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val vacancyPaint = Paint().apply {
        textSize = 9 * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    
    // Measure text
    val titleWidth = titlePaint.measureText(jobTitle)
    val vacancyText = "$vacancy vacancy"
    val vacancyWidth = vacancyPaint.measureText(vacancyText)
    
    val chipWidth = (maxOf(titleWidth, vacancyWidth) + padding * 2).toInt()
    val lineHeight = (14 * density).toInt()
    val chipHeight = (lineHeight * 2 + padding * 1.5f).toInt()
    val totalWidth = chipWidth.coerceAtLeast((60 * density).toInt())
    val totalHeight = chipHeight + pinHeight
    
    val bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Colors based on state - NOT black for markers
    val bgColor = when {
        isSelected -> 0xFF2563EB.toInt()  // Blue for selected
        isUrgent -> 0xFFDC2626.toInt()    // Red for urgent
        isNearby -> 0xFF059669.toInt()    // Green for nearby
        else -> 0xFFFFFFFF.toInt()        // White for normal
    }
    val titleColor = if (bgColor == 0xFFFFFFFF.toInt()) 0xFF1F2937.toInt() else android.graphics.Color.WHITE
    val vacancyColor = if (bgColor == 0xFFFFFFFF.toInt()) 0xFF6B7280.toInt() else 0xDDFFFFFF.toInt()
    
    // Draw shadow
    val shadowPaint = Paint().apply {
        color = 0x25000000
        style = Paint.Style.FILL
        isAntiAlias = true
        maskFilter = android.graphics.BlurMaskFilter(4 * density, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    val shadowRect = RectF(
        (totalWidth - chipWidth) / 2f + 2,
        3f,
        (totalWidth + chipWidth) / 2f + 2,
        chipHeight.toFloat() + 3
    )
    canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)
    
    // Draw chip background
    val chipPaint = Paint().apply {
        color = bgColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val chipRect = RectF(
        (totalWidth - chipWidth) / 2f,
        0f,
        (totalWidth + chipWidth) / 2f,
        chipHeight.toFloat()
    )
    canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, chipPaint)
    
    // Draw border for white chips
    if (bgColor == 0xFFFFFFFF.toInt()) {
        val borderPaint = Paint().apply {
            color = 0xFFE5E7EB.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            isAntiAlias = true
        }
        canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, borderPaint)
    }
    
    // Draw job title (top line)
    titlePaint.color = titleColor
    val titleX = chipRect.left + (chipWidth - titleWidth) / 2
    val titleY = padding + lineHeight * 0.8f
    canvas.drawText(jobTitle, titleX, titleY, titlePaint)
    
    // Draw vacancy (bottom line)
    vacancyPaint.color = vacancyColor
    val vacancyX = chipRect.left + (chipWidth - vacancyWidth) / 2
    val vacancyY = titleY + lineHeight
    canvas.drawText(vacancyText, vacancyX, vacancyY, vacancyPaint)
    
    // Draw pin pointer
    val pinPaint = Paint().apply {
        color = bgColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val pinPath = android.graphics.Path().apply {
        moveTo(totalWidth / 2f - 8 * density, chipHeight.toFloat() - 1)
        lineTo(totalWidth / 2f, (chipHeight + pinHeight - 4 * density))
        lineTo(totalWidth / 2f + 8 * density, chipHeight.toFloat() - 1)
        close()
    }
    canvas.drawPath(pinPath, pinPaint)
    
    // Draw pin dot
    canvas.drawCircle(totalWidth / 2f, totalHeight - 4 * density, 4 * density, pinPaint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Pulsing Radar Overlay - Uber-style animated radar effect
 * Shows pulsing circles emanating from user location
 */
@Composable
fun PulsingRadarOverlay(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    color: Color = AvailableGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_alpha"
    )
    
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_scale"
    )
    
    if (isActive) {
        Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = minOf(size.width, size.height) / 2
            
            // Draw pulsing circles
            drawCircle(
                color = color.copy(alpha = radarAlpha),
                radius = maxRadius * radarScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Enhanced Google Map with Pulsing Markers
 * Combines the existing EnhancedGoogleMapView with pulsing animation overlay
 */
@Composable
fun GoogleMapWithPulsingMarkers(
    modifier: Modifier = Modifier,
    jobs: List<JobListing>,
    userLatitude: Double?,
    userLongitude: Double?,
    selectedJob: JobListing? = null,
    showPulsingOverlay: Boolean = true,
    onMarkerClick: (JobListing) -> Unit = {},
    onMapReady: () -> Unit = {}
) {
    Box(modifier = modifier) {
        // Main map
        EnhancedGoogleMapView(
            modifier = Modifier.fillMaxSize(),
            jobs = jobs,
            userLatitude = userLatitude,
            userLongitude = userLongitude,
            selectedJob = selectedJob,
            onMarkerClick = onMarkerClick,
            onMapReady = onMapReady
        )
        
        // Pulsing radar overlay (optional)
        if (showPulsingOverlay && userLatitude != null && userLongitude != null) {
            PulsingRadarOverlay(
                modifier = Modifier.fillMaxSize(),
                isActive = true,
                color = AvailableGreen
            )
        }
    }
}
