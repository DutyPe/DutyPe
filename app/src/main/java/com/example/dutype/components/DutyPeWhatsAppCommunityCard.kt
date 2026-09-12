package com.example.dutype.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.WorkerColors

/**
 * Official DutyPe WhatsApp Jobs Group / Community Flat Menu Item
 * Features a dynamic running gradient border with smooth multicolor rotation.
 */
@Composable
fun DutyPeWhatsAppCommunityCard(
    modifier: Modifier = Modifier,
    title: String = "Join DutyPe Jobs Group",
    subtitle: String = "Get daily job updates & direct hiring on WhatsApp",
    whatsAppGroupUrl: String = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"
) {
    val context = LocalContext.current
    val isDark = com.example.dutype.ui.theme.isAppInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl)).apply {
                        setPackage("com.whatsapp")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl))
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Unable to open WhatsApp link", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // WhatsApp Vibrant Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "WhatsApp Group",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WorkerColors.TextPrimary,
                            fontSize = 14.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF25D366))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WorkerColors.TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Join Button Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF25D366)
            ) {
                Text(
                    text = "JOIN",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
