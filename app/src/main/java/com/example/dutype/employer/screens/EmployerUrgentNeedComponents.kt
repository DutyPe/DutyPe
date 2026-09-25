package com.example.dutype.employer.screens

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
                            text = "⚡ Instant Hiring Rooms",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Call workers directly & hire instantly",
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
    onCallWorker: (String) -> Unit,
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmployerColors.Primary)
            }
        }
        requests.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    UrgentIcon(Icons.Default.Schedule, size = 64)
                    Text(
                        text = stringResource(R.string.urgent_empty_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = EmployerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.urgent_empty_body),
                        style = MaterialTheme.typography.bodyMedium.copy(color = EmployerColors.TextSecondary),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onPostUrgentNeed,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                    ) {
                        Text(stringResource(R.string.post_urgent_need_title))
                    }
                }
            }
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
    onRequestMatchedWorker: (MatchedWorker) -> Unit = {},
    onOpenMatchedWorkerProfile: (MatchedWorker) -> Unit = {},
    onOpenWorkerProfile: (InstantResponse) -> Unit,
    onCallWorker: (String) -> Unit,
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmployerColors.Primary)
            }
        }
        request == null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    UrgentIcon(Icons.Default.Schedule, size = 64)
                    Text(
                        text = stringResource(R.string.urgent_request_not_found),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = EmployerColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onPostUrgentNeed,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                    ) {
                        Text(stringResource(R.string.post_urgent_need_title))
                    }
                }
            }
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
                                    text = "Nearby Matches (${matchedWorkersState.workers.size})",
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
                                    text = "Ready Workers (${responses.size})",
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
                                    text = "Available Workers Near You",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmployerColors.TextPrimary
                                    )
                                )
                                Text(
                                    text = "Nearest workers ready for instant work. Tap 'Call Worker' to connect directly.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary)
                                )
                            }
                            IconButton(onClick = onRefreshMatchedWorkers) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh workers", tint = EmployerColors.Primary)
                            }
                        }
                    }

                    if (matchedWorkersState.isLoading) {
                        item(key = "nearby_loading") {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = EmployerColors.Primary, modifier = Modifier.size(32.dp))
                            }
                        }
                    } else if (matchedWorkersState.workers.isEmpty()) {
                        item(key = "nearby_empty") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = EmployerColors.TextSecondary, modifier = Modifier.size(40.dp))
                                    Text(
                                        text = "Finding Nearby Workers...",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmployerColors.TextPrimary)
                                    )
                                    Text(
                                        text = "Workers in your area have been broadcast notified of this urgent post. You can tap Refresh to check for newly active workers.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                    OutlinedButton(onClick = onRefreshMatchedWorkers, shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Refresh Nearby Workers")
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(matchedWorkersState.workers, key = { index, w -> "matched_${w.workerId}_$index" }) { _, worker ->
                            UrgentMatchedWorkerCard(
                                worker = worker,
                                isRequesting = matchedWorkersState.requestingWorkerId == worker.workerId,
                                isRequested = worker.requestStatus.equals("pending", ignoreCase = true) || worker.requestStatus.equals("accepted", ignoreCase = true),
                                onCallWorker = { onCallMatchedWorker(worker) },
                                onRequestWorker = { onRequestMatchedWorker(worker) },
                                onOpenProfile = { onOpenMatchedWorkerProfile(worker) }
                            )
                        }
                    }
                } else {
                    // TAB 1: WORKER RESPONSES
                    if (responses.isEmpty()) {
                        item(key = "responses_empty") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = EmployerColors.TextSecondary, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "No responses received yet",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmployerColors.TextPrimary)
                                    )
                                    Text(
                                        text = "Workers have been notified and responses will show here once accepted. For immediate hiring, you can directly call nearby workers from the 'Nearby Workers' tab.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { selectedTab = 0 },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmployerColors.Primary)
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View & Call Nearby Workers")
                                    }
                                }
                            }
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
    isRequesting: Boolean,
    isRequested: Boolean,
    onCallWorker: () -> Unit,
    onRequestWorker: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenProfile() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        .background(Color(0xFFF1F5F9)),
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
                            contentDescription = "Verified",
                            tint = EmployerColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (worker.isAvailable) "Available now" else "Active recently",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (worker.isAvailable) Color(0xFF16A34A) else EmployerColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        text = "${worker.matchScore}% Match",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmployerColors.Primary,
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
                    color = Color(0xFFF0FDF4)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = worker.distanceKm?.let { String.format(Locale.ROOT, "%.1f km", it) } ?: "Nearby",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF16A34A)
                            ),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = String.format(Locale.ROOT, "%.1f", worker.rating),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            ),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC)
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
                            text = "${worker.completedJobs} jobs",
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

            // Action buttons: Request & Call
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRequestWorker,
                    enabled = !isRequested && !isRequesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (isRequested) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRequested) "Requested" else "Request")
                    }
                }

                Button(
                    onClick = onCallWorker,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call Worker")
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
        color = if (hasReadyWorkers) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
        border = if (hasReadyWorkers) BorderStroke(1.dp, Color(0xFF86EFAC)) else null
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
                    text = if (hasReadyWorkers) "🟢 $responseCount Ready! Tap to Open Hiring Room →" else stringResource(R.string.urgent_mini_meta, request.category, selectedCount, request.workersNeeded, responseCount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (hasReadyWorkers) Color(0xFF15803D) else EmployerColors.TextSecondary,
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
    onCallWorker: (String) -> Unit,
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EmployerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        color = Color(0xFFF8FAFC)
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
                    color = Color(0xFFFFF1F2)
                ) {
                    Text(
                        text = request.failureReason,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFBE123C))
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (responses.isNotEmpty()) "⚡ Open Instant Hiring Room (${responses.size} Ready)" else "⚡ Open Instant Hiring Room",
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
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
                            Text("Delete Post")
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
    onCallWorker: (String) -> Unit,
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
        color = Color(0xFFF8FAFC)
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
                if (response.workerPhone.isNotBlank()) {
                    Button(
                        onClick = { onCallWorker(response.workerPhone) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📞 Call Worker", fontWeight = FontWeight.Bold, color = Color.White)
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
                            Text("✅ Hire Worker", fontWeight = FontWeight.Bold)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
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
                    canRate -> {
                        Button(
                            onClick = { onRateResponse(response) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
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
private fun UrgentIcon(icon: ImageVector, size: Int = 44) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(Color(0xFFFFEDD5), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFEA580C),
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
    StatusBadge(text = normalized.ifBlank { "open" }, color = color)
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
    val label = if (normalized == "no_show") stringResource(R.string.no_show) else normalized.ifBlank { "viewed" }
    StatusBadge(text = label, color = color)
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text.replace('_', ' ').replaceFirstChar { it.uppercase() },
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
        color = Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF475569),
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