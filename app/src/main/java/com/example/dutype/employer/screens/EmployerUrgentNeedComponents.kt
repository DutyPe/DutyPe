package com.example.dutype.employer.screens

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.material.icons.filled.Bolt
import com.dutype.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dutype.models.MatchedWorker
import com.example.dutype.viewmodels.MatchedWorkersUiState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.utils.DateTimeUtils
import java.util.Locale

@Composable
internal fun EmployerUrgentNeedSummarySection(
    requests: List<InstantRequest>,
    responsesByRequestId: Map<String, List<InstantResponse>>,
    isLoading: Boolean,
    onViewAll: () -> Unit,
    onOpenRequest: (InstantRequest) -> Unit = {},
    onPostUrgentNeed: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    UrgentIcon(Icons.Default.Schedule)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.instant_hiring_rooms),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.instant_hiring_room_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onViewAll) { Text(stringResource(R.string.view_all)) }
                }
            }

            when {
                requests.isEmpty() && !isLoading -> {
                    Text(
                        text = stringResource(R.string.urgent_empty_summary),
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary)
                    )
                }
                else -> {
                    requests.take(2).forEach { request ->
                        EmployerUrgentNeedMiniRow(
                            request = request,
                            responseCount = responsesByRequestId[request.requestId].orEmpty().size,
                            onClick = { onOpenRequest(request) }
                        )
                    }
                }
            }

            if (requests.isEmpty() && !isLoading) {
                Button(
                    onClick = onPostUrgentNeed,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                ) {
                    Text(stringResource(R.string.post_urgent_need_title))
                }
            }
        }
    }
}

@Composable
internal fun EmployerUrgentNeedHistoryContent(
    requests: List<InstantRequest>,
    responsesByRequestId: Map<String, List<InstantResponse>>,
    isLoading: Boolean,
    updatingRequestId: String?,
    updatingResponseId: String?,
    ratedResponseIds: Set<String>,
    onOpenRequest: (InstantRequest) -> Unit,
    onOpenWorkerProfile: (InstantResponse) -> Unit,
    onCallWorker: (com.example.dutype.models.InstantResponse) -> Unit,
    onSelectResponse: (InstantResponse) -> Unit,
    onCompleteResponse: (InstantResponse) -> Unit,
    onNoShowResponse: (InstantResponse) -> Unit,
    onRateResponse: (InstantResponse) -> Unit,
    onMarkRequestFilled: (InstantRequest) -> Unit,
    onCancelRequest: (InstantRequest) -> Unit,
    onDeleteRequest: ((InstantRequest) -> Unit)? = null,
    onPostUrgentNeed: () -> Unit
) {
    when {
        isLoading -> {
            com.example.dutype.components.DutyPeLoadingList(rows = 3)
        }
        requests.isEmpty() && !com.example.dutype.components.rememberOnline().value -> {
            com.example.dutype.components.DutyPeIssueState(com.example.dutype.components.LoadIssue.OFFLINE, onRetry = null)
        }
        requests.isEmpty() -> {
            com.example.dutype.components.DutyPeEmptyScreen(
                icon = Icons.Filled.Bolt,
                tone = com.example.dutype.components.EmptyTone.ORANGE,
                title = stringResource(R.string.urgent_empty_title),
                message = stringResource(R.string.urgent_empty_body),
                primary = com.example.dutype.components.EmptyStateAction(
                    label = stringResource(R.string.post_urgent_need_title),
                    icon = Icons.Filled.Bolt,
                    onClick = onPostUrgentNeed
                )
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(requests, key = { index, request -> "urgent_${request.requestId.ifBlank { "req" }}_$index" }) { _, request ->
                    EmployerUrgentNeedCard(
                        request = request,
                        responses = responsesByRequestId[request.requestId].orEmpty(),
                        isRequestUpdating = updatingRequestId == request.requestId,
                        updatingResponseId = updatingResponseId,
                        ratedResponseIds = ratedResponseIds,
                        onOpenRequest = onOpenRequest,
                        onOpenWorkerProfile = onOpenWorkerProfile,
                        onCallWorker = onCallWorker,
                        onSelectResponse = onSelectResponse,
                        onCompleteResponse = onCompleteResponse,
                        onNoShowResponse = onNoShowResponse,
                        onRateResponse = onRateResponse,
                        onMarkRequestFilled = onMarkRequestFilled,
                        onCancelRequest = onCancelRequest,
                        onDeleteRequest = onDeleteRequest
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmployerUrgentNeedDetailContent(
    request: InstantRequest?,
    responses: List<InstantResponse>,
    isLoading: Boolean,
    isRequestUpdating: Boolean,
    updatingResponseId: String?,
    ratedResponseIds: Set<String>,
    matchedWorkersState: MatchedWorkersUiState = MatchedWorkersUiState(),
    onRefreshMatchedWorkers: () -> Unit = {},
    onCallMatchedWorker: (MatchedWorker) -> Unit = {},
    onChatMatchedWorker: (MatchedWorker) -> Unit = {},
    onOpenMatchedWorkerProfile: (MatchedWorker) -> Unit = {},
    onOpenWorkerProfile: (InstantResponse) -> Unit,
    onCallWorker: (com.example.dutype.models.InstantResponse) -> Unit,
    onSelectResponse: (InstantResponse) -> Unit,
    onCompleteResponse: (InstantResponse) -> Unit,
    onNoShowResponse: (InstantResponse) -> Unit,
    onRateResponse: (InstantResponse) -> Unit,
    onMarkRequestFilled: (InstantRequest) -> Unit,
    onCancelRequest: (InstantRequest) -> Unit,
    onDeleteRequest: ((InstantRequest) -> Unit)? = null,
    onPostUrgentNeed: () -> Unit
) {
    when {
        isLoading -> {
            com.example.dutype.components.DutyPeLoadingList(rows = 4)
        }
        request == null && !com.example.dutype.components.rememberOnline().value -> {
            com.example.dutype.components.DutyPeIssueState(com.example.dutype.components.LoadIssue.OFFLINE, onRetry = null)
        }
        request == null -> {
            com.example.dutype.components.DutyPeEmptyScreen(
                icon = Icons.Default.Schedule,
                tone = com.example.dutype.components.EmptyTone.ORANGE,
                art = com.example.dutype.components.EmptyArt.SEARCH,
                title = stringResource(R.string.urgent_request_not_found),
                message = stringResource(R.string.state_notfound_body),
                primary = com.example.dutype.components.EmptyStateAction(
                    label = stringResource(R.string.post_urgent_need_title),
                    icon = Icons.Filled.Bolt,
                    onClick = onPostUrgentNeed
                )
            )
        }
        else -> {
            var selectedTab by remember { mutableIntStateOf(if (responses.isNotEmpty()) 1 else 0) }
            val responseSelectedCount = responses.count { it.status.equals("accepted", ignoreCase = true) || it.status.equals("completed", ignoreCase = true) }
            val selectedCount = maxOf(request.selectedWorkerIds.size, responseSelectedCount)
            val requiredWorkers = request.workersNeeded.coerceAtLeast(1)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Request details summary card
                item(key = "urgent_detail_${request.requestId}") {
                    EmployerUrgentNeedCard(
                        request = request,
                        responses = responses,
                        isRequestUpdating = isRequestUpdating,
                        updatingResponseId = updatingResponseId,
                        ratedResponseIds = ratedResponseIds,
                        onOpenRequest = {},
                        onOpenWorkerProfile = onOpenWorkerProfile,
                        onCallWorker = onCallWorker,
                        onSelectResponse = onSelectResponse,
                        onCompleteResponse = onCompleteResponse,
                        onNoShowResponse = onNoShowResponse,
                        onRateResponse = onRateResponse,
                        onMarkRequestFilled = onMarkRequestFilled,
                        onCancelRequest = onCancelRequest,
                        onDeleteRequest = onDeleteRequest,
                        showDetailButton = false,
                        showResponses = false
                    )
                }

                // 2. Tabs: Nearby Workers vs Responses
                item(key = "urgent_tabs") {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = EmployerColors.CardBackground,
                        contentColor = EmployerColors.TextPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = stringResource(R.string.nearby_matches_format, matchedWorkersState.workers.size),
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = { Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = stringResource(R.string.ready_workers_format, responses.size),
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                // 3. Tab Content
                if (selectedTab == 0) {
                    // TAB 0: NEARBY MATCHED WORKERS
                    item(key = "nearby_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.available_workers_near_you),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmployerColors.TextPrimary
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.nearest_workers_ready_desc),
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                                )
                            }
                            IconButton(onClick = onRefreshMatchedWorkers) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_workers), tint = EmployerColors.Primary.fg())
                            }
                        }
                    }

                    if (matchedWorkersState.isLoading) {
                        items(3, key = { "nearby_loading_$it" }) {
                            com.example.dutype.components.ApplicationListItemShimmer()
                        }
                    } else if (matchedWorkersState.hasError) {
                        item(key = "nearby_error") {
                            com.example.dutype.components.DutyPeErrorState(
                                message = matchedWorkersState.error,
                                onRetry = onRefreshMatchedWorkers,
                                compact = true
                            )
                        }
                    } else if (matchedWorkersState.workers.isEmpty()) {
                        item(key = "nearby_empty") {
                            com.example.dutype.components.DutyPeEmptyState(
                                icon = Icons.Filled.PersonSearch,
                                tone = com.example.dutype.components.EmptyTone.BLUE,
                                compact = true,
                                title = stringResource(R.string.finding_nearby_workers),
                                message = stringResource(R.string.finding_nearby_workers_desc),
                                secondary = com.example.dutype.components.EmptyStateAction(
                                    label = stringResource(R.string.refresh_nearby_workers),
                                    icon = Icons.Filled.Refresh,
                                    onClick = onRefreshMatchedWorkers
                                ),
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC).bg(), RoundedCornerShape(14.dp))
                            )
                        }
                    } else {
                        itemsIndexed(matchedWorkersState.workers, key = { index, w -> "matched_${w.workerId}_$index" }) { _, worker ->
                            UrgentMatchedWorkerCard(
                                worker = worker,
                                onCallWorker = { onCallMatchedWorker(worker) },
                                onChatWorker = { onChatMatchedWorker(worker) },
                                onOpenProfile = { onOpenMatchedWorkerProfile(worker) }
                            )
                        }
                    }
                } else {
                    // TAB 1: WORKER RESPONSES
                    if (responses.isEmpty()) {
                        item(key = "responses_empty") {
                            com.example.dutype.components.DutyPeEmptyState(
                                icon = Icons.Filled.Schedule,
                                badge = Icons.Filled.Bolt,
                                tone = com.example.dutype.components.EmptyTone.ORANGE,
                                compact = true,
                                title = stringResource(R.string.no_responses_received_yet),
                                message = stringResource(R.string.no_responses_received_desc),
                                primary = com.example.dutype.components.EmptyStateAction(
                                    label = stringResource(R.string.view_call_nearby_workers),
                                    icon = Icons.Filled.Verified,
                                    onClick = { selectedTab = 0 }
                                ),
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC).bg(), RoundedCornerShape(14.dp))
                            )
                        }
                    } else {
                        itemsIndexed(responses, key = { _, r -> "resp_${r.responseId}" }) { _, response ->
                            InstantResponseRow(
                                response = response,
                                isUpdating = updatingResponseId == response.responseId,
                                hasAlreadyRated = response.responseId in ratedResponseIds,
                                canSelectMore = selectedCount < requiredWorkers || response.workerId in request.selectedWorkerIds,
                                onOpenWorkerProfile = onOpenWorkerProfile,
                                onCallWorker = onCallWorker,
                                onSelectResponse = onSelectResponse,
                                onCompleteResponse = onCompleteResponse,
                                onNoShowResponse = onNoShowResponse,
                                onRateResponse = onRateResponse
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UrgentMatchedWorkerCard(
    worker: MatchedWorker,
    onCallWorker: () -> Unit,
    onChatWorker: (() -> Unit)? = null,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, EmployerColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9).bg()),
                    contentAlignment = Alignment.Center
                ) {
                    if (worker.profileImageUrl.isNotBlank()) {
                        com.example.dutype.components.OptimizedProfileImage(
                            imageUrl = worker.profileImageUrl,
                            contentDescription = worker.fullName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = EmployerColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = worker.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmployerColors.TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = stringResource(R.string.verified_label),
                            tint = EmployerColors.Primary.fg(),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (worker.isAvailable) stringResource(R.string.available_now) else stringResource(R.string.active_recently),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (worker.isAvailable) Color(0xFF16A34A).fg() else EmployerColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF).bg()
                ) {
                    Text(
                        text = stringResource(R.string.match_percent_format, worker.matchScore),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmployerColors.Primary.fg(),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metrics row: Distance, Rating, Completed jobs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4).bg()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF16A34A).fg(),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = worker.distanceKm?.let { stringResource(R.string.distance_km_format, it) } ?: stringResource(R.string.nearby),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF16A34A).fg()
                            ),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB).bg()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706).fg(),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.ROOT, "%.1f", worker.rating),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E).fg()
                            ),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC).bg()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmployerColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.worker_stat_jobs_count, worker.completedJobs),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EmployerColors.TextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            // Skills chips
            if (worker.skills.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(worker.skills.take(4)) { skill ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmployerColors.ChipBackground
                        ) {
                            Text(
                                text = skill.replaceFirstChar { it.titlecase(Locale.ROOT) },
                                style = MaterialTheme.typography.labelSmall.copy(color = EmployerColors.TextSecondary),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action: call or chat with the worker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCallWorker,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg())
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.call_worker))
                }
                if (onChatWorker != null) {
                    OutlinedButton(
                        onClick = onChatWorker,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.chat), color = Color(0xFF25D366), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployerUrgentNeedMiniRow(
    request: InstantRequest,
    responseCount: Int,
    onClick: () -> Unit
) {
    val selectedCount = request.selectedWorkerIds.size.coerceAtLeast(if (request.selectedWorkerId.isNotBlank()) 1 else 0)
    val hasReadyWorkers = responseCount > 0
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (hasReadyWorkers) Color(0xFFF0FDF4).bg() else Color(0xFFFFFBEB).bg(),
        border = if (hasReadyWorkers) BorderStroke(1.dp, Color(0xFF86EFAC).bd()) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgentIcon(if (hasReadyWorkers) Icons.Default.FlashOn else Icons.Default.Work, size = 38)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EmployerColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (hasReadyWorkers) stringResource(R.string.ready_open_hiring_room_format, responseCount) else stringResource(R.string.urgent_mini_meta, request.category, selectedCount, request.workersNeeded, responseCount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (hasReadyWorkers) Color(0xFF15803D).fg() else EmployerColors.TextSecondary,
                        fontWeight = if (hasReadyWorkers) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            UrgentStatusBadge(request.status)
        }
    }
}

@Composable
private fun EmployerUrgentNeedCard(
    request: InstantRequest,
    responses: List<InstantResponse>,
    isRequestUpdating: Boolean,
    updatingResponseId: String?,
    ratedResponseIds: Set<String>,
    onOpenRequest: (InstantRequest) -> Unit,
    onOpenWorkerProfile: (InstantResponse) -> Unit,
    onCallWorker: (com.example.dutype.models.InstantResponse) -> Unit,
    onSelectResponse: (InstantResponse) -> Unit,
    onCompleteResponse: (InstantResponse) -> Unit,
    onNoShowResponse: (InstantResponse) -> Unit,
    onRateResponse: (InstantResponse) -> Unit,
    onMarkRequestFilled: (InstantRequest) -> Unit,
    onCancelRequest: (InstantRequest) -> Unit,
    onDeleteRequest: ((InstantRequest) -> Unit)? = null,
    showDetailButton: Boolean = true,
    showResponses: Boolean = true
) {
    val normalizedStatus = request.status.lowercase(Locale.ROOT)
    val canCancel = normalizedStatus in setOf("open", "filled")
    val responseSelectedCount = responses.count { it.status.equals("accepted", ignoreCase = true) || it.status.equals("completed", ignoreCase = true) }
    val selectedCount = maxOf(request.selectedWorkerIds.size, responseSelectedCount)
    val completedCount = maxOf(request.completedWorkerIds.size, responses.count { it.status.equals("completed", ignoreCase = true) })
    val requiredWorkers = request.workersNeeded.coerceAtLeast(1)
    val canMarkFilled = normalizedStatus in setOf("open", "failed") && selectedCount > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, EmployerColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                UrgentIcon(Icons.Default.Schedule)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        UrgentStatusBadge(request.status)
                    }
                    Text(
                        text = "${request.category} • ${needTypeLabel(request.needType)} • ${DateTimeUtils.formatRelativeTime(request.createdAt)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (request.addressText.isNotBlank()) {
                        Text(
                            text = request.addressText,
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { InfoPill(Icons.Default.Group, stringResource(R.string.urgent_need_count, requiredWorkers)) }
                item { InfoPill(Icons.Default.CheckCircle, stringResource(R.string.urgent_selected_count, selectedCount, requiredWorkers)) }
                // Dispatch progress: offers go to online workers 5 → 10 → 15 → 20 km until filled.
                if (normalizedStatus == "open" && request.dispatchRadiusKm > 0) {
                    item { InfoPill(Icons.Default.Schedule, stringResource(R.string.urgent_offers_sent_within, request.dispatchRadiusKm)) }
                }
                item { InfoPill(Icons.Default.Group, stringResource(R.string.urgent_responses_count, responses.size)) }
                if (completedCount > 0) {
                    item { InfoPill(Icons.Default.DoneAll, stringResource(R.string.urgent_done_count, completedCount)) }
                }
                if (request.budgetText.isNotBlank()) {
                    item { InfoPill(Icons.Default.Work, request.budgetText) }
                }
                if (request.notifiedWorkerCount > 0) {
                    item { InfoPill(Icons.Default.Schedule, stringResource(R.string.urgent_notified_count, request.notifiedWorkerCount)) }
                }
            }

            if (request.description.isNotBlank()) {
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary),
                    maxLines = if (showDetailButton) 2 else 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showResponses) {
                if (responses.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC).bg()
                    ) {
                        Text(
                            text = stringResource(R.string.urgent_no_worker_responses),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                        )
                    }
                } else {
                    responses.forEach { response ->
                        InstantResponseRow(
                            response = response,
                            isUpdating = updatingResponseId == response.responseId,
                            hasAlreadyRated = response.responseId in ratedResponseIds,
                            canSelectMore = selectedCount < requiredWorkers || response.workerId in request.selectedWorkerIds,
                        onOpenWorkerProfile = onOpenWorkerProfile,
                        onCallWorker = onCallWorker,
                        onSelectResponse = onSelectResponse,
                        onCompleteResponse = onCompleteResponse,
                        onNoShowResponse = onNoShowResponse,
                        onRateResponse = onRateResponse
                    )
                }
            }
        }

            if (request.failureReason.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F2).bg()
                ) {
                    Text(
                        text = request.failureReason,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBE123C).fg())
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showDetailButton) {
                    Button(
                        onClick = { onOpenRequest(request) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB).bg())
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (responses.isNotEmpty()) stringResource(R.string.open_instant_hiring_room_ready, responses.size) else stringResource(R.string.open_instant_hiring_room),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                if (canMarkFilled || canCancel) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canMarkFilled) {
                            Button(
                                onClick = { onMarkRequestFilled(request) },
                                enabled = !isRequestUpdating,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg())
                            ) {
                                if (isRequestUpdating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.mark_urgent_job_filled))
                                }
                            }
                        }
                        if (canCancel) {
                            OutlinedButton(
                                onClick = { onCancelRequest(request) },
                                enabled = !isRequestUpdating,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isRequestUpdating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.stop_urgent_job))
                                }
                            }
                        }
                    }
                }
                
                // Add Delete Button if request is cancelled, failed, completed, or expired!
                val isInactive = normalizedStatus in setOf("completed", "expired", "cancelled", "failed")
                if (isInactive && onDeleteRequest != null) {
                    OutlinedButton(
                        onClick = { onDeleteRequest(request) },
                        enabled = !isRequestUpdating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmployerColors.Error),
                        border = BorderStroke(1.dp, EmployerColors.Error)
                    ) {
                        if (isRequestUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EmployerColors.Error, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmployerColors.Error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.delete_post))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstantResponseRow(
    response: InstantResponse,
    isUpdating: Boolean,
    hasAlreadyRated: Boolean,
    canSelectMore: Boolean,
    onOpenWorkerProfile: (InstantResponse) -> Unit,
    onCallWorker: (com.example.dutype.models.InstantResponse) -> Unit,
    onSelectResponse: (InstantResponse) -> Unit,
    onCompleteResponse: (InstantResponse) -> Unit,
    onNoShowResponse: (InstantResponse) -> Unit,
    onRateResponse: (InstantResponse) -> Unit
) {
    val status = response.status.lowercase(Locale.ROOT)
    val canSelect = status in setOf("viewed", "applied", "interested", "called") && canSelectMore
    val canComplete = status == "accepted"
    val canRate = status == "completed" && !hasAlreadyRated

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC).bg()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = response.workerName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = EmployerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = response.workerSkills.take(3).joinToString(", ").ifBlank { stringResource(R.string.worker_profile_fallback) },
                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ResponseStatusBadge(response.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenWorkerProfile(response) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.profile))
                }
                if (response.status != "rejected") {
                    Button(
                        onClick = { onCallWorker(response) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg())
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.call_worker_btn), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isUpdating -> {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                    canSelect -> {
                        Button(
                            onClick = { onSelectResponse(response) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.hire_worker_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                    !canSelectMore && status in setOf("viewed", "applied", "interested", "called") -> {
                        InfoPill(Icons.Default.CheckCircle, stringResource(R.string.required_workers_selected))
                    }
                    canComplete -> {
                        Button(
                            onClick = { onCompleteResponse(response) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg())
                        ) {
                            Text(stringResource(R.string.mark_done))
                        }
                        OutlinedButton(
                            onClick = { onNoShowResponse(response) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Report, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.no_show))
                        }
                    }
                }
            }
            // Accepted but not needed any more (e.g. the other worker is bringing a friend): remove with a reason.
            if (canComplete && !isUpdating) {
                RemoveUrgentWorkerButton(response)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                when {
                    canRate -> {
                        Button(
                            onClick = { onRateResponse(response) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B).bg())
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.rate))
                        }
                    }
                    status == "completed" && hasAlreadyRated -> {
                        InfoPill(Icons.Default.DoneAll, stringResource(R.string.rated))
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveUrgentWorkerButton(response: InstantResponse) {
    val viewModel: com.example.dutype.viewmodels.InstantHelpViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var open by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val presets = listOf(
        stringResource(R.string.urgent_remove_reason_friend),
        stringResource(R.string.urgent_remove_reason_not_needed),
        stringResource(R.string.urgent_remove_reason_late)
    )
    TextButton(onClick = { open = true }) {
        Text(stringResource(R.string.urgent_remove_worker), color = Color(0xFFDC2626).fg(), fontSize = 13.sp)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(R.string.urgent_remove_title, response.workerName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.urgent_remove_body), fontSize = 13.sp)
                    presets.forEach { p ->
                        FilterChip(selected = reason == p, onClick = { reason = p }, label = { Text(p, fontSize = 12.sp) })
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(200) },
                        label = { Text(stringResource(R.string.urgent_remove_reason_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeEmployerUrgentWorker(response, reason)
                        open = false
                    },
                    enabled = reason.isNotBlank()
                ) { Text(stringResource(R.string.urgent_remove_confirm), color = Color(0xFFDC2626).fg()) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun UrgentIcon(icon: ImageVector, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFFFFEDD5).bg(), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFEA580C).fg(),
            modifier = Modifier.size((size / 2).dp)
        )
    }
}

@Composable
private fun UrgentStatusBadge(status: String) {
    val normalized = status.lowercase(Locale.ROOT)
    val color = when (normalized) {
        "open" -> Color(0xFF16A34A)
        "filled" -> Color(0xFF2563EB)
        "completed" -> Color(0xFF16A34A)
        "expired" -> Color(0xFF64748B)
        "cancelled", "failed" -> Color(0xFFDC2626)
        else -> Color(0xFF64748B)
    }
    val label = when (normalized) {
        "open" -> stringResource(R.string.open_status)
        "filled" -> stringResource(R.string.filled)
        "completed" -> stringResource(R.string.completed)
        "expired" -> stringResource(R.string.history_expired)
        "cancelled" -> stringResource(R.string.status_cancelled)
        "failed" -> stringResource(R.string.status_failed)
        else -> normalized.ifBlank { stringResource(R.string.open_status) }
    }
    StatusBadge(text = label, color = color)
}

@Composable
private fun ResponseStatusBadge(status: String) {
    val normalized = status.lowercase(Locale.ROOT)
    val color = when (normalized) {
        "applied", "interested", "called" -> Color(0xFFEA580C)
        "accepted" -> Color(0xFF2563EB)
        "completed" -> Color(0xFF16A34A)
        "busy", "rejected", "cancelled", "no_show" -> Color(0xFFDC2626)
        else -> Color(0xFF64748B)
    }
    val label = when (normalized) {
        "applied" -> stringResource(R.string.applied)
        "interested" -> stringResource(R.string.status_interested)
        "called" -> stringResource(R.string.status_called)
        "accepted" -> stringResource(R.string.status_accepted)
        "completed" -> stringResource(R.string.completed)
        "busy" -> stringResource(R.string.status_busy)
        "rejected" -> stringResource(R.string.status_rejected)
        "cancelled" -> stringResource(R.string.status_cancelled)
        "no_show" -> stringResource(R.string.no_show)
        else -> if (normalized.isBlank()) stringResource(R.string.viewed) else normalized.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    StatusBadge(text = label, color = color)
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                color = color,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InfoPill(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1F5F9).bg()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF475569).fg(), modifier = Modifier.size(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF475569).fg(),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun needTypeLabel(value: String): String = when (value) {
    "urgent_now" -> stringResource(R.string.urgent_now)
    "today" -> stringResource(R.string.today)
    "scheduled" -> stringResource(R.string.urgent_tomorrow)
    else -> stringResource(R.string.urgent)
}