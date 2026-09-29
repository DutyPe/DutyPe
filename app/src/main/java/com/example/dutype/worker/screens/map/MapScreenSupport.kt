package com.example.dutype.worker.screens.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.dutype.models.JobListingSummary

internal const val MAP_DEFAULT_LAT = 17.385044
internal const val MAP_DEFAULT_LNG = 78.486671
internal const val MAP_DEFAULT_ZOOM = 13f
internal const val MAP_CAROUSEL_LIMIT = 100

internal val MapEmerald = Color(0xFF10B981)
internal val MapUrgentRed = Color(0xFFDC2626)

/** Palette that follows the app's light/dark surface. */
internal data class MapColors(
    val surface: Color,
    val border: Color,
    val ink: Color,
    val onInk: Color,
    val muted: Color,
    val tile: Color,
    val dark: Boolean
)

@Composable
internal fun rememberMapColors(): MapColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            MapColors(
                surface = Color(0xFF171717),
                border = Color(0xFF2E2E2E),
                ink = Color(0xFFF5F5F5),
                onInk = Color(0xFF0F0F0F),
                muted = Color(0xFF94A3B8),
                tile = Color(0xFF262626),
                dark = true
            )
        } else {
            MapColors(
                surface = Color.White,
                border = Color(0xFFE2E8F0),
                ink = Color(0xFF0F0F0F),
                onInk = Color.White,
                muted = Color(0xFF64748B),
                tile = Color(0xFFF1F5F9),
                dark = false
            )
        }
    }
}

/** Flat, low-contrast light map style (no POI / transit clutter). */
internal const val MAP_STYLE_LIGHT = """
[
  {"elementType":"geometry","stylers":[{"color":"#f5f6f8"}]},
  {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#94a3b8"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#ffffff"}]},
  {"featureType":"administrative","elementType":"geometry","stylers":[{"visibility":"off"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#e2e8f0"}]},
  {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#b6c0cf"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#dbe3ee"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"landscape","elementType":"geometry","stylers":[{"color":"#f5f6f8"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#e0ebf7"}]}
]
"""

/** Matching dark style for dark theme. */
internal const val MAP_STYLE_DARK = """
[
  {"elementType":"geometry","stylers":[{"color":"#1b1d21"}]},
  {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
  {"elementType":"labels.text.fill","stylers":[{"color":"#7c8595"}]},
  {"elementType":"labels.text.stroke","stylers":[{"color":"#1b1d21"}]},
  {"featureType":"administrative","elementType":"geometry","stylers":[{"visibility":"off"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#2a2d33"}]},
  {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#33373e"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#14171b"}]}
]
"""

/** Quick-filter categories shown before "More...". */
internal val MapQuickCategories = listOf("Electrician", "Plumber", "Driver", "Cook")

internal fun matchesMapCategory(job: JobListingSummary, category: String?): Boolean {
    if (category == null) return true
    return job.jobType.contains(category, ignoreCase = true) ||
        job.title.contains(category, ignoreCase = true)
}

/** Most common job types in the loaded jobs (excluding the quick chips), for the "More" row. */
internal fun extraMapCategories(jobs: List<JobListingSummary>): List<String> {
    return jobs.asSequence()
        .map { it.jobType.trim() }
        .filter { it.isNotEmpty() && MapQuickCategories.none { q -> q.equals(it, ignoreCase = true) } }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(12)
        .map { it.key }
        .toList()
}

/** Jobs inside the viewport, keeping the (nearest-first) order, capped for a light carousel. */
internal fun buildCarouselJobs(jobs: List<JobListingSummary>, viewport: MapViewport?): List<JobListingSummary> {
    val inside = if (viewport == null) jobs else jobs.filter { viewport.contains(it.lat, it.lng) }
    return inside.take(MAP_CAROUSEL_LIMIT)
}
