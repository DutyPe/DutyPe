package com.example.dutype.employer.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.components.RatingBottomSheet
import com.example.dutype.models.InstantRequest
import com.example.dutype.models.InstantResponse
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.EmployerColors
import com.example.dutype.viewmodels.InstantHelpViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerUrgentNeedDetailScreen(
    navController: NavController,
    requestId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val instantHelpViewModel: InstantHelpViewModel = hiltViewModel()
    val instantHelpState by instantHelpViewModel.uiState.collectAsStateWithLifecycle()
    val ratingService = remember {
        com.example.dutype.services.RatingService(
            com.example.dutype.di.firestoreFromHilt(context),
            com.example.dutype.di.authFromHilt(context)
        )
    }

    var pendingRatingResponse by remember { mutableStateOf<InstantResponse?>(null) }
    var pendingCompletionResponse by remember { mutableStateOf<InstantResponse?>(null) }
    var pendingNoShowResponse by remember { mutableStateOf<InstantResponse?>(null) }
    var pendingCancelRequest by remember { mutableStateOf<InstantRequest?>(null) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var ratedResponseIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(requestId) {
        instantHelpViewModel.loadEmployerUrgentNeeds()
    }

    val request = instantHelpState.employerInstantRequests.firstOrNull { it.requestId == requestId }
    val responses = instantHelpState.employerInstantResponses[requestId].orEmpty()

    LaunchedEffect(responses) {
        ratedResponseIds = responses
            .filter { it.status.equals("completed", ignoreCase = true) }
            .mapNotNull { response ->
                if (ratingService.hasRated(response.requestId, response.workerId)) response.responseId else null
            }
            .toSet()
    }

    if (showRatingSheet && pendingRatingResponse != null) {
        RatingBottomSheet(
            isVisible = showRatingSheet,
            targetName = pendingRatingResponse!!.workerName.ifBlank { stringResource(R.string.this_worker) },
            targetRole = "WORKER",
            onDismiss = {
                showRatingSheet = false
                pendingRatingResponse = null
            },
            onSubmit = { rating, review, tags ->
                pendingRatingResponse?.let { response ->
                    scope.launch {
                        ratingService.submitRating(
                            jobId = response.requestId,
                            targetUserId = response.workerId,
                            rating = rating,
                            review = review,
                            tags = tags,
                            targetRole = "WORKER"
                        ).fold(
                            onSuccess = { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                if (result.success) {
                                    ratedResponseIds = ratedResponseIds + response.responseId
                                    showRatingSheet = false
                                    pendingRatingResponse = null
                                }
                            },
                            onFailure = { error ->
                                Toast.makeText(context, error.message ?: context.getString(R.string.failed_submit_rating), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        )
    }

    pendingCompletionResponse?.let { response ->
        ReasonDialog(
            title = stringResource(R.string.complete_urgent_work),
            message = stringResource(R.string.urgent_complete_note_message),
            placeholder = stringResource(R.string.urgent_complete_note_hint),
            confirmLabel = stringResource(R.string.mark_done),
            onDismiss = { pendingCompletionResponse = null },
            onConfirm = { note ->
                instantHelpViewModel.completeEmployerInstantResponse(response, note)
                pendingCompletionResponse = null
            }
        )
    }

    pendingNoShowResponse?.let { response ->
        ReasonDialog(
            title = stringResource(R.string.mark_no_show),
            message = stringResource(R.string.urgent_no_show_message),
            placeholder = stringResource(R.string.urgent_no_show_hint),
            confirmLabel = stringResource(R.string.no_show),
            onDismiss = { pendingNoShowResponse = null },
            onConfirm = { reason ->
                instantHelpViewModel.markEmployerInstantResponseNoShow(response, reason)
                pendingNoShowResponse = null
            }
        )
    }

    pendingCancelRequest?.let { requestToCancel ->
        ReasonDialog(
            title = stringResource(R.string.cancel_urgent_request),
            message = stringResource(R.string.urgent_cancel_message),
            placeholder = stringResource(R.string.urgent_cancel_hint),
            confirmLabel = stringResource(R.string.cancel_request),
            onDismiss = { pendingCancelRequest = null },
            onConfirm = { reason ->
                instantHelpViewModel.cancelEmployerInstantRequest(requestToCancel, reason)
                pendingCancelRequest = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmployerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.urgent_request_title),
            subtitle = request?.title ?: stringResource(R.string.urgent_responses_actions_subtitle),
            navController = navController,
            backgroundColor = EmployerColors.ScreenBackground
        )

        EmployerUrgentNeedDetailContent(
            request = request,
            responses = responses,
            isLoading = instantHelpState.isLoadingEmployerUrgentNeeds,
            isRequestUpdating = instantHelpState.updatingEmployerRequestId == requestId,
            updatingResponseId = instantHelpState.updatingEmployerResponseId,
            ratedResponseIds = ratedResponseIds,
            onOpenWorkerProfile = { response -> navController.navigate(Routes.workerProfileViewRoute(response.workerId)) },
            onCallWorker = { phone -> openDialer(context, phone) },
            onSelectResponse = { response -> instantHelpViewModel.acceptEmployerInstantResponse(response) },
            onCompleteResponse = { response -> pendingCompletionResponse = response },
            onNoShowResponse = { response -> pendingNoShowResponse = response },
            onRateResponse = { response ->
                pendingRatingResponse = response
                showRatingSheet = true
            },
            onMarkRequestFilled = { requestToFill -> instantHelpViewModel.markEmployerInstantRequestFilled(requestToFill) },
            onCancelRequest = { requestToCancel -> pendingCancelRequest = requestToCancel },
            onPostUrgentNeed = { navController.navigate(Routes.EMPLOYER_POST_URGENT_NEED) }
        )
    }
}

@Composable
private fun ReasonDialog(
    title: String,
    message: String,
    placeholder: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(placeholder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(300) },
                    placeholder = { Text(placeholder) },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun openDialer(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.unable_to_open_dialer), Toast.LENGTH_SHORT).show()
    }
}
