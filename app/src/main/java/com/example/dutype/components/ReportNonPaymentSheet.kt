package com.example.dutype.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dutype.app.R
import com.example.dutype.services.ReportResult
import com.example.dutype.services.ReportingService
import com.example.dutype.ui.theme.WorkerColors
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lets a worker report that an employer never paid for completed work.
 *
 * Kept separate from [ReportJobSheet] because a payment claim needs the amount owed
 * and the date the work happened, which support needs to act on it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportNonPaymentSheet(
    jobTitle: String,
    companyName: String,
    onDismiss: () -> Unit,
    onReport: suspend (amount: Double, workedOn: Timestamp, description: String) -> Result<ReportResult>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var workedOnMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val amount = amountText.toDoubleOrNull()
    val canSubmit = amount != null &&
        amount > 0 &&
        amount <= ReportingService.MAX_CLAIM_AMOUNT &&
        !isSubmitting

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = workedOnMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { workedOnMillis = it }
                    showDatePicker = false
                }) { Text(stringResource(R.string.done_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MoneyOff,
                    contentDescription = null,
                    tint = WorkerColors.Error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.report_non_payment_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = WorkerColors.TextPrimary
                    )
                    Text(
                        text = if (companyName.isBlank()) jobTitle else "$jobTitle • $companyName",
                        style = MaterialTheme.typography.bodySmall,
                        color = WorkerColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = WorkerColors.CardBackground),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Text(
                    text = stringResource(R.string.report_non_payment_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = WorkerColors.TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    amountText = input.filter { it.isDigit() || it == '.' }
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.report_non_payment_amount_label)) },
                placeholder = { Text(stringResource(R.string.report_non_payment_amount_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = amountText.isNotBlank() && amount == null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        R.string.report_non_payment_work_date,
                        dateFormat.format(Date(workedOnMillis))
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 1000) description = it },
                label = { Text(stringResource(R.string.report_non_payment_details_label)) },
                placeholder = { Text(stringResource(R.string.report_non_payment_details_hint)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = WorkerColors.Error
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }

                Button(
                    onClick = {
                        val value = amount ?: return@Button
                        isSubmitting = true
                        errorMessage = null
                        scope.launch {
                            val result = onReport(
                                value,
                                Timestamp(Date(workedOnMillis)),
                                description
                            )
                            isSubmitting = false
                            result.fold(
                                onSuccess = { onDismiss() },
                                onFailure = { errorMessage = it.message }
                            )
                        }
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = WorkerColors.Error),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.report_non_payment_submit))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
