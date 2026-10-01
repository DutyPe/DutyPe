package com.example.dutype.worker.screens.map

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.fg
import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.models.JobListingSummary
import com.example.dutype.utils.toJobListing
import kotlinx.coroutines.launch

/** Horizontally snapping carousel of compact job cards (next card peeks in). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MapJobCarousel(
    jobs: List<JobListingSummary>,
    activeId: String?,
    listState: LazyListState,
    colors: MapColors,
    onSelect: (JobListingSummary) -> Unit,
    onOpen: (JobListingSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardWidth = (LocalConfiguration.current.screenWidthDp - 56).coerceAtLeast(240).dp
    CarouselUserSelectEffect(listState, jobs, onSelect)
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp)
    ) {
        items(jobs, key = { it.id }) { job ->
            MapJobCard(
                job = job,
                selected = job.id == activeId,
                colors = colors,
                onClick = { onSelect(job) },
                onOpen = { onOpen(job) },
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}

/** Reports the settled card only when the user dragged the carousel (not programmatic scrolls). */
@Composable
private fun CarouselUserSelectEffect(
    state: LazyListState,
    jobs: List<JobListingSummary>,
    onSelect: (JobListingSummary) -> Unit
) {
    val latestJobs by rememberUpdatedState(jobs)
    val latestSelect by rememberUpdatedState(onSelect)
    LaunchedEffect(state) {
        var userDriven = false
        launch {
            state.interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Start) userDriven = true
            }
        }
        androidx.compose.runtime.snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && userDriven) {
                userDriven = false
                latestJobs.getOrNull(state.firstVisibleItemIndex)?.let { latestSelect(it) }
            }
        }
    }
}

/** Compact flat job card: company tile, title, company - distance, pay, small View button. */
@Composable
private fun MapJobCard(
    job: JobListingSummary,
    selected: Boolean,
    colors: MapColors,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MapEmerald.bd() else colors.border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompanyTile(job, colors)
            JobCardInfo(job, colors, Modifier.weight(1f))
            ViewButton(colors, onOpen)
        }
    }
}

/** Same category icon tile as the job cards and the Find Jobs rail. */
@Composable
private fun CompanyTile(job: JobListingSummary, colors: MapColors) {
    com.example.dutype.worker.components.JobCategoryIconTile(
        category = job.category,
        title = job.title,
        size = 48.dp,
        cornerRadius = 12.dp
    )
}

@Composable
private fun JobCardInfo(job: JobListingSummary, colors: MapColors, modifier: Modifier = Modifier) {
    val distance = mapDistanceLabel(job.distance)
    val fallbackCompany = stringResource(R.string.hiring_now)
    val company = job.companyName.ifBlank { fallbackCompany }
    val subtitle = if (distance != null) "$company · $distance" else company
    val urgent = job.isUrgent
    Column(modifier = modifier) {
        Text(
            text = job.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = mapPayFull(job),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (urgent) {
                Text(stringResource(R.string.urgent), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MapUrgentRed.fg())
            }
        }
    }
}

@Composable
private fun ViewButton(colors: MapColors, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.ink
    ) {
        Box(modifier = Modifier.padding(start = 14.dp, end = 14.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.view_action), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onInk)
        }
    }
}

/**
 * Full list of the jobs in the visible map area, nearest first. Uses the same
 * [com.example.dutype.worker.components.WorkerHomeJobCard] as Home and Find Jobs.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun MapJobsListSheet(
    jobs: List<JobListingSummary>,
    colors: MapColors,
    onDismiss: () -> Unit,
    onOpen: (JobListingSummary) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listings = androidx.compose.runtime.remember(jobs) {
        jobs.map { it to it.toJobListing() }
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Text(
            text = if (jobs.size == 1) stringResource(R.string.map_job_count_single) else stringResource(R.string.map_job_count_plural, jobs.size),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.nearest_first),
            fontSize = 12.sp,
            color = colors.muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(listings, key = { it.first.id }) { (summary, listing) ->
                com.example.dutype.worker.components.WorkerHomeJobCard(
                    job = listing,
                    onCardClick = { onOpen(summary) }
                )
            }
        }
    }
}
