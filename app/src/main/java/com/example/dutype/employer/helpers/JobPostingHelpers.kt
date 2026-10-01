package com.example.dutype.employer.helpers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dutype.employer.models.PayType

object JobPostingHelpers {
    fun getPayTypeIcon(payType: PayType): ImageVector = when (payType) {
        PayType.HOURLY -> Icons.Default.AccessTime
        PayType.DAILY -> Icons.Default.CalendarToday
        PayType.WEEKLY -> Icons.Default.DateRange
        PayType.MONTHLY -> Icons.Default.Work
        PayType.NEGOTIABLE -> Icons.Default.Handshake
    }
}
