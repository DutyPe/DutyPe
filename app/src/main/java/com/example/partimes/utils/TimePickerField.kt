package com.example.partimes.utils

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import androidx.compose.foundation.clickable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import java.util.*

@Composable
fun TimePickerField(
    context: Context,
    label: String,
    timeValue: String,
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    OutlinedTextField(
        value = timeValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = androidx.compose.ui.Modifier
            .clickable {
                TimePickerDialog(context, { _: TimePicker, h: Int, m: Int ->
                    val formatted = String.format("%02d:%02d", h, m)
                    onTimeSelected(formatted)
                }, hour, minute, false).show()
            }
    )
}
