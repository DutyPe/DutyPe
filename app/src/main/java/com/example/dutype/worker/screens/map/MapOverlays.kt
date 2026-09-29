package com.example.dutype.worker.screens.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top overlay: area pill, then category chips. Map is a bottom-bar tab, so there is no
 * back button here (the old arrow just popped to Home and fought the tab bar).
 */
@Composable
internal fun MapTopBar(
    areaLabel: String,
    isLoading: Boolean,
    colors: MapColors,
    category: String?,
    extraCategories: List<String>,
    showMore: Boolean,
    onCategory: (String?) -> Unit,
    onToggleMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapAreaPill(areaLabel, isLoading, colors, Modifier.fillMaxWidth())
        MapCategoryRow(colors, category, extraCategories, showMore, onCategory, onToggleMore)
    }
}

@Composable
private fun MapAreaPill(label: String, isLoading: Boolean, colors: MapColors, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.LocationOn, null, tint = MapEmerald, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MapEmerald
                )
            }
        }
    }
}

@Composable
private fun MapCategoryRow(
    colors: MapColors,
    category: String?,
    extraCategories: List<String>,
    showMore: Boolean,
    onCategory: (String?) -> Unit,
    onToggleMore: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "all") { MapChip("All", category == null, colors) { onCategory(null) } }
        items(MapQuickCategories, key = { "q$it" }) { name ->
            MapChip(name, category.equals(name, ignoreCase = true), colors) {
                onCategory(if (category.equals(name, ignoreCase = true)) null else name)
            }
        }
        if (showMore) {
            items(extraCategories, key = { "x$it" }) { name ->
                MapChip(name, category.equals(name, ignoreCase = true), colors) {
                    onCategory(if (category.equals(name, ignoreCase = true)) null else name)
                }
            }
        }
        if (extraCategories.isNotEmpty()) {
            item(key = "more") { MapChip(if (showMore) "Less" else "More…", false, colors, onToggleMore) }
        }
    }
}

/** 36dp pill chip. */
@Composable
private fun MapChip(label: String, selected: Boolean, colors: MapColors, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.ink else colors.surface,
        border = BorderStroke(1.dp, if (selected) colors.ink else colors.border)
    ) {
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) colors.onInk else colors.ink,
                maxLines = 1
            )
        }
    }
}

/** "Search this area" chip that appears after panning. */
@Composable
internal fun SearchThisAreaChip(
    visible: Boolean,
    colors: MapColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(18.dp),
            color = colors.ink,
            shadowElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = colors.onInk, modifier = Modifier.size(16.dp))
                Text("Search this area", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onInk)
            }
        }
    }
}

/** Floating 48dp white "my location" button. */
@Composable
internal fun MyLocationButton(colors: MapColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    MapRoundButton(onClick = onClick, colors = colors, size = 48, modifier = modifier) {
        Icon(Icons.Default.MyLocation, "My location", tint = colors.ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MapRoundButton(
    onClick: () -> Unit,
    colors: MapColors,
    size: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** "N jobs in this area · List" pill above the carousel; tapping opens the full list sheet. */
@Composable
internal fun JobsCountPill(count: Int, colors: MapColors, onViewList: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onViewList,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (count == 1) "1 job in this area" else "$count jobs in this area",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink
            )
            Surface(shape = RoundedCornerShape(14.dp), color = colors.ink, modifier = Modifier.height(26.dp)) {
                Row(
                    modifier = Modifier.padding(start = 8.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = colors.onInk, modifier = Modifier.size(14.dp))
                    Text("List", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onInk)
                }
            }
        }
    }
}

/** Shown in the carousel slot when the current area has no jobs. */
@Composable
internal fun EmptyAreaCard(colors: MapColors, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("No jobs in this area", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Zoom out or move the map to explore more jobs.",
                fontSize = 12.sp,
                color = colors.muted
            )
        }
    }
}
