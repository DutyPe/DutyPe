package com.example.dutype.worker.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R

/**
 * P2 PERFORMANCE FIX: Extracted HomeHeader composable
 * 
 * Reduces recomposition scope - only this component recomposes when
 * location or notification state changes.
 * 
 * @author DutyPe Engineering Team
 * @since 2.4.0
 */
@Composable
fun HomeHeader(
    locationText: String,
    isLocationLoading: Boolean,
    unreadNotificationCount: Int,
    onLocationClick: () -> Unit,
    onMapViewClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - DutyPe text and location
            Column {
                Text(
                    text = "DutyPe",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = com.example.dutype.ui.theme.WorkerColors.TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                )
                // Location directly below DutyPe
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onLocationClick() }
                ) {
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isLocationLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Right side - Map and Notification buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Map View chip button
                Surface(
                    onClick = onMapViewClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.location_view),
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.map_view),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF374151)
                            )
                        )
                    }
                }
                
                // Notification icon with badge
                NotificationButton(
                    unreadCount = unreadNotificationCount,
                    onClick = onNotificationClick
                )
            }
        }
    }
}

@Composable
private fun NotificationButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }
        
        // Red dot badge when there are unread notifications
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 6.dp)
                    .background(
                        color = Color(0xFFDC2626),
                        shape = CircleShape
                    )
            )
        }
    }
}
