package com.example.dutype.worker.screens.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.example.dutype.models.JobListingSummary
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

private val CenterAnchor = Offset(0.5f, 0.5f)

/** All job markers: pills for single jobs, dark bubbles for clusters. */
@Composable
@GoogleMapComposable
internal fun MapMarkersLayer(
    items: List<MapItem>,
    activeId: String?,
    onJobClick: (JobListingSummary) -> Unit,
    onClusterClick: (MapCluster) -> Unit
) {
    for (item in items) {
        when (item) {
            is MapSingle -> JobPillMarker(item.job, item.job.id == activeId, onJobClick)
            is MapCluster -> ClusterMarker(item, onClusterClick)
        }
    }
}

@Composable
@GoogleMapComposable
private fun JobPillMarker(
    job: JobListingSummary,
    selected: Boolean,
    onClick: (JobListingSummary) -> Unit
) {
    val context = LocalContext.current
    val urgent = job.isUrgent
    val icon = remember(job.id, job.payAmount, job.payType, selected, urgent) {
        MapMarkerIcons.pill(context, mapPayShort(job), selected, urgent)
    }
    val state = remember(job.id, job.lat, job.lng) { MarkerState(LatLng(job.lat, job.lng)) }
    Marker(
        state = state,
        icon = icon,
        anchor = CenterAnchor,
        zIndex = if (selected) 100f else if (urgent) 50f else 10f,
        onClick = {
            onClick(job)
            true
        }
    )
}

@Composable
@GoogleMapComposable
private fun ClusterMarker(cluster: MapCluster, onClick: (MapCluster) -> Unit) {
    val context = LocalContext.current
    val icon = remember(cluster.count) { MapMarkerIcons.cluster(context, cluster.count) }
    val state = remember(cluster.key) { MarkerState(LatLng(cluster.lat, cluster.lng)) }
    Marker(
        state = state,
        icon = icon,
        anchor = CenterAnchor,
        zIndex = 20f,
        onClick = {
            onClick(cluster)
            true
        }
    )
}

/** Blue "you are here" dot. */
@Composable
@GoogleMapComposable
internal fun UserLocationMarker(position: LatLng) {
    val context = LocalContext.current
    val icon = remember { MapMarkerIcons.userDot(context) }
    val state = remember(position.latitude, position.longitude) { MarkerState(position) }
    Marker(state = state, icon = icon, anchor = CenterAnchor, zIndex = 5f)
}
