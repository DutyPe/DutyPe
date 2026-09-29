package com.example.dutype.worker.screens.map

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.dutype.models.JobListingSummary
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.GeoUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

private const val SEARCH_CHIP_MIN_MOVE_KM = 0.4
private const val CLUSTER_STOP_ZOOM = 16f

/**
 * Worker Map tab: flat, clean map of open jobs around the visible area.
 * Data comes from [WorkerMapViewModel] (viewport-based geohash loading, cached, capped).
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun JobMapScreen(
    navController: NavController,
    rootNavController: NavController? = null,
    viewModel: WorkerMapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = rememberMapColors()
    val state by viewModel.uiState.collectAsState()
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val currentLocation by locationRepository.userLocation.collectAsState()
    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    val lastKnown = remember { viewModel.lastKnownLatLng() }
    var userLatLng by remember { mutableStateOf(lastKnown?.let { LatLng(it.first, it.second) }) }
    val camera = rememberCameraPositionState {
        val start = lastKnown ?: (MAP_DEFAULT_LAT to MAP_DEFAULT_LNG)
        position = CameraPosition.fromLatLngZoom(LatLng(start.first, start.second), MAP_DEFAULT_ZOOM)
    }

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var viewport by remember { mutableStateOf<MapViewport?>(null) }
    var searchAnchor by remember { mutableStateOf<LatLng?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }
    val carouselState = rememberLazyListState()

    val filtered = remember(state.jobs, category) { state.jobs.filter { matchesMapCategory(it, category) } }
    val carouselJobs = remember(filtered, viewport) { buildCarouselJobs(filtered, viewport) }
    val extraCategories = remember(state.jobs) { extraMapCategories(state.jobs) }
    val activeId = selectedId?.takeIf { id -> carouselJobs.any { it.id == id } } ?: carouselJobs.firstOrNull()?.id

    MapLocationEffects(
        viewModel = viewModel,
        camera = camera,
        granted = permission.status.isGranted,
        lastKnown = lastKnown,
        onRequestPermission = { permission.launchPermissionRequest() },
        onUserLocation = {
            userLatLng = it
            searchAnchor = null
        }
    )
    MapViewportEffect(
        camera = camera,
        mapLoaded = mapLoaded,
        onViewport = { vp ->
            viewport = vp
            if (searchAnchor == null) searchAnchor = LatLng(vp.centerLat, vp.centerLng)
            viewModel.onCameraIdle(vp)
        }
    )
    MapSelectionEffect(
        selectedId = selectedId,
        allJobs = state.jobs,
        carouselJobs = carouselJobs,
        camera = camera,
        listState = carouselState
    )

    val showSearchChip = remember(viewport, searchAnchor, camera.isMoving) {
        val vp = viewport
        val anchor = searchAnchor
        vp != null && anchor != null && !camera.isMoving &&
            GeoUtils.calculateHaversineDistance(anchor.latitude, anchor.longitude, vp.centerLat, vp.centerLng) >
            SEARCH_CHIP_MIN_MOVE_KM
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarReserve = 96.dp
    val carouselReserve = 150.dp
    val mapBottomPadding = navBottom + bottomBarReserve + carouselReserve

    Box(modifier = Modifier.fillMaxSize()) {
        MapCanvas(
            camera = camera,
            colors = colors,
            contentPadding = PaddingValues(top = 120.dp, bottom = mapBottomPadding),
            jobs = filtered,
            zoom = viewport?.zoom ?: MAP_DEFAULT_ZOOM,
            activeId = activeId,
            userLatLng = userLatLng,
            onLoaded = { mapLoaded = true },
            onJobClick = { job ->
                selectedId = job.id
                searchAnchor = null
            },
            onClusterClick = { cluster ->
                searchAnchor = null
                scope.launch { zoomIntoCluster(camera, cluster) { selectedId = it.id } }
            }
        )

        MapTopBar(
            areaLabel = currentLocation?.getShortAddress() ?: "Jobs near you",
            isLoading = state.isLoading,
            colors = colors,
            category = category,
            extraCategories = extraCategories,
            showMore = showMore,
            onCategory = { category = it },
            onToggleMore = { showMore = !showMore },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )

        SearchThisAreaChip(
            visible = showSearchChip,
            colors = colors,
            onClick = {
                viewport?.let { vp ->
                    searchAnchor = LatLng(vp.centerLat, vp.centerLng)
                    viewModel.searchThisArea(vp)
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 108.dp)
        )

        MapBottomPanel(
            jobs = carouselJobs,
            activeId = activeId,
            listState = carouselState,
            colors = colors,
            showEmpty = state.hasLoaded && !state.isLoading && carouselJobs.isEmpty(),
            onViewList = { showList = true },
            onSelect = {
                selectedId = it.id
                searchAnchor = null
            },
            onOpen = { job -> navController.navigate(Routes.jobDetailRoute(job.id)) },
            onMyLocation = {
                val target = userLatLng
                searchAnchor = null
                if (target != null) {
                    scope.launch { camera.animate(CameraUpdateFactory.newLatLngZoom(target, 15f), 500) }
                } else if (!permission.status.isGranted) {
                    permission.launchPermissionRequest()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = bottomBarReserve + 4.dp)
        )
    }

    if (showList && carouselJobs.isNotEmpty()) {
        MapJobsListSheet(
            jobs = carouselJobs,
            colors = colors,
            onDismiss = { showList = false },
            onOpen = { job ->
                showList = false
                navController.navigate(Routes.jobDetailRoute(job.id))
            }
        )
    }
}

/** Google map with the light/dark style, job markers (clustered when zoomed out) and user dot. */
@Composable
private fun MapCanvas(
    camera: CameraPositionState,
    colors: MapColors,
    contentPadding: PaddingValues,
    jobs: List<JobListingSummary>,
    zoom: Float,
    activeId: String?,
    userLatLng: LatLng?,
    onLoaded: () -> Unit,
    onJobClick: (JobListingSummary) -> Unit,
    onClusterClick: (MapCluster) -> Unit
) {
    val properties = remember(colors.dark) {
        MapProperties(
            mapType = MapType.NORMAL,
            mapStyleOptions = runCatching {
                MapStyleOptions(if (colors.dark) MAP_STYLE_DARK else MAP_STYLE_LIGHT)
            }.getOrNull()
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false
        )
    }
    val items = remember(jobs, zoom, activeId) { MapClusterer.cluster(jobs, zoom, activeId) }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = camera,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = contentPadding,
        onMapLoaded = onLoaded
    ) {
        userLatLng?.let { UserLocationMarker(it) }
        MapMarkersLayer(items, activeId, onJobClick, onClusterClick)
    }
}

/** Count pill (opens the list) + my-location button row above the job carousel. */
@Composable
private fun MapBottomPanel(
    jobs: List<JobListingSummary>,
    activeId: String?,
    listState: LazyListState,
    colors: MapColors,
    showEmpty: Boolean,
    onViewList: () -> Unit,
    onSelect: (JobListingSummary) -> Unit,
    onOpen: (JobListingSummary) -> Unit,
    onMyLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            if (jobs.isNotEmpty()) JobsCountPill(jobs.size, colors, onViewList) else Box {}
            MyLocationButton(colors, onMyLocation)
        }
        if (jobs.isNotEmpty()) {
            MapJobCarousel(jobs, activeId, listState, colors, onSelect, onOpen)
        } else if (showEmpty) {
            EmptyAreaCard(colors, Modifier.padding(start = 16.dp, end = 16.dp))
        }
    }
}

/** Last-known location first, then a fresh fix; handles the permission prompt. */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapLocationEffects(
    viewModel: WorkerMapViewModel,
    camera: CameraPositionState,
    granted: Boolean,
    lastKnown: Pair<Double, Double>?,
    onRequestPermission: () -> Unit,
    onUserLocation: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val locationRepository = remember { com.example.dutype.di.locationRepositoryFromHilt(context) }
    val latestOnUser by rememberUpdatedState(onUserLocation)

    // Instant map: load around the last-known (or default) centre without waiting for GPS.
    LaunchedEffect(Unit) {
        val start = lastKnown ?: (MAP_DEFAULT_LAT to MAP_DEFAULT_LNG)
        viewModel.loadInitial(start.first, start.second)
    }

    LaunchedEffect(granted) {
        if (!granted) {
            onRequestPermission()
            return@LaunchedEffect
        }
        try {
            val fix = locationRepository.getHighAccuracy(timeoutMs = 10000L, minAccuracyMeters = 50f)
            if (fix != null) {
                val target = LatLng(fix.latitude, fix.longitude)
                latestOnUser(target)
                viewModel.loadInitial(fix.latitude, fix.longitude)
                camera.animate(CameraUpdateFactory.newLatLngZoom(target, MAP_DEFAULT_ZOOM), 800)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Map: error getting location")
        }
    }
}

/** Publishes the visible viewport every time the camera settles. */
@Composable
private fun MapViewportEffect(
    camera: CameraPositionState,
    mapLoaded: Boolean,
    onViewport: (MapViewport) -> Unit
) {
    val latest by rememberUpdatedState(onViewport)
    LaunchedEffect(camera, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        snapshotFlow { camera.isMoving }.filter { !it }.collect {
            val bounds = camera.projection?.visibleRegion?.latLngBounds
            if (bounds != null) {
                latest(
                    MapViewport(
                        south = bounds.southwest.latitude,
                        west = bounds.southwest.longitude,
                        north = bounds.northeast.latitude,
                        east = bounds.northeast.longitude,
                        zoom = camera.position.zoom
                    )
                )
            }
        }
    }
}

/** Selected job: centre the map on it and scroll the carousel to its card. */
@Composable
private fun MapSelectionEffect(
    selectedId: String?,
    allJobs: List<JobListingSummary>,
    carouselJobs: List<JobListingSummary>,
    camera: CameraPositionState,
    listState: LazyListState
) {
    val latestAll by rememberUpdatedState(allJobs)
    val latestCarousel by rememberUpdatedState(carouselJobs)
    LaunchedEffect(selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        val index = latestCarousel.indexOfFirst { it.id == id }
        if (index >= 0 && listState.firstVisibleItemIndex != index) listState.animateScrollToItem(index)
        val job = latestAll.firstOrNull { it.id == id } ?: return@LaunchedEffect
        camera.animate(CameraUpdateFactory.newLatLng(LatLng(job.lat, job.lng)), 400)
    }
}

/** Cluster tap: zoom to its bounds, or select the first job when already at street level. */
private suspend fun zoomIntoCluster(
    camera: CameraPositionState,
    cluster: MapCluster,
    onSelect: (JobListingSummary) -> Unit
) {
    if (camera.position.zoom >= CLUSTER_STOP_ZOOM) {
        cluster.jobs.firstOrNull()?.let(onSelect)
        return
    }
    val builder = LatLngBounds.builder()
    cluster.jobs.forEach { builder.include(LatLng(it.lat, it.lng)) }
    try {
        camera.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 140), 450)
    } catch (e: IllegalStateException) {
        camera.animate(CameraUpdateFactory.zoomIn(), 450)
    }
}
