package com.example.dutype.worker.components

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R

/**
 * The worker's Online switch, like a delivery partner's: Online = urgent jobs nearby ring on this
 * phone; Offline = no urgent offers (normal jobs are still listed).
 */
@Composable
fun OnlineToggleCard(isOnline: Boolean, isSaving: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val green = Color(0xFF16A34A).fg()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isOnline) Color(0xFFF0FDF4).bg() else Color.White.bg(), shape)
            .border(1.dp, if (isOnline) Color(0xFFBBF7D0).bd() else Color(0xFFE2E8F0).bd(), shape)
            .clickable(enabled = !isSaving) { onChange(!isOnline) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(if (isOnline) green else Color(0xFF94A3B8).bg(), CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (isOnline) R.string.worker_online_title else R.string.worker_offline_title),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) green else Color(0xFF0F172A).fg()
            )
            Text(
                text = stringResource(if (isOnline) R.string.worker_online_body else R.string.worker_offline_body),
                fontSize = 13.sp,
                color = Color(0xFF64748B).fg()
            )
        }
        Switch(
            checked = isOnline,
            onCheckedChange = { if (!isSaving) onChange(it) },
            enabled = !isSaving,
            colors = SwitchDefaults.colors(checkedTrackColor = green, checkedThumbColor = Color.White),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
