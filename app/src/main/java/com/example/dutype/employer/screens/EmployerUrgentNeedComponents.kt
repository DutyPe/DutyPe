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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                            text = stringResource(R.string.urgent_needs_posted),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = EmployerColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.urgent_track_workers_today),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        showDetailButton = false
                    )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgentIcon(Icons.Default.Work, size = 38)
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
                    text = stringResource(R.string.urgent_mini_meta, request.category, selectedCount, request.workersNeeded, responseCount),
                    style = MaterialTheme.typography.bodySmall.copy(color = EmployerColors.TextSecondary),
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
    showDetailButton: Boolean = true
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
                    OutlinedButton(
                        onClick = { onOpenRequest(request) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.view_details))
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
                    OutlinedButton(
                        onClick = { onCallWorker(response.workerPhone) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.call))
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
                            Text(stringResource(R.string.select))
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