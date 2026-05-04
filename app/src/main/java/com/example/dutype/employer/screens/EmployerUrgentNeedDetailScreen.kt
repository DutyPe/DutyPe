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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
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
            targetName = pendingRatingResponse!!.workerName.ifBlank { "this worker" },
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
                                Toast.makeText(context, error.message ?: "Failed to submit rating", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
        )
    }

    pendingCompletionResponse?.let { response ->
        ReasonDialog(
            title = "Complete urgent work",
            message = "Add a short completion note. This keeps reviews and history clear.",
            placeholder = "Work completed in person",
            confirmLabel = "Mark done",
            onDismiss = { pendingCompletionResponse = null },
            onConfirm = { note ->
                instantHelpViewModel.completeEmployerInstantResponse(response, note)
                pendingCompletionResponse = null
            }
        )
    }

    pendingNoShowResponse?.let { response ->
        ReasonDialog(
            title = "Mark no show",
            message = "Add why this worker could not complete the urgent work.",
            placeholder = "Worker did not arrive",
            confirmLabel = "No show",
            onDismiss = { pendingNoShowResponse = null },
            onConfirm = { reason ->
                instantHelpViewModel.markEmployerInstantResponseNoShow(response, reason)
                pendingNoShowResponse = null
            }
        )
    }

    pendingCancelRequest?.let { requestToCancel ->
        ReasonDialog(
            title = "Cancel urgent request",
            message = "Workers will stop seeing this urgent need.",
            placeholder = "Need cancelled by employer",
            confirmLabel = "Cancel request",
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
            title = "Urgent request",
            subtitle = request?.title ?: "Worker responses and actions",
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
            onWhatsAppWorker = { phone -> openWhatsApp(context, phone) },
            onSelectResponse = { response -> instantHelpViewModel.acceptEmployerInstantResponse(response) },
            onCompleteResponse = { response -> pendingCompletionResponse = response },
            onNoShowResponse = { response -> pendingNoShowResponse = response },
            onRateResponse = { response ->
                pendingRatingResponse = response
                showRatingSheet = true
            },
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
                Text("Close")
            }
        }
    )
}

private fun openDialer(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }.onFailure {
        Toast.makeText(context, "Unable to open dialer", Toast.LENGTH_SHORT).show()
    }
}

private fun openWhatsApp(context: android.content.Context, phone: String) {
    val digits = phone.filter { it.isDigit() }
    if (digits.isBlank()) return
    val normalized = if (digits.startsWith("91")) digits else "91$digits"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized")))
    }.onFailure {
        Toast.makeText(context, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
