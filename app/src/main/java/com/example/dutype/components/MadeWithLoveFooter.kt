package com.example.dutype.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.ui.theme.fg
import timber.log.Timber

const val DUTYPE_WHATSAPP_URL = "https://chat.whatsapp.com/DYj1FQZfHrW0haVcML5eU3?s=cl&p=a&iam=2"
const val DUTYPE_INSTAGRAM_URL = "https://www.instagram.com/dutype.in?vrfl=Z3JnYzZjaGZybmd4"

/**
 * Flat menu row displaying "Follow us" with gray Instagram and WhatsApp icons.
 */
@Composable
fun ProfileFollowUsRow(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = com.example.dutype.ui.theme.LocalDarkMode.current
    val itemTextColor = if (isDark) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF222222)
    val itemIconTint = if (isDark) com.example.dutype.ui.theme.DarkMap.Text else Color(0xFF2B2B2B)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.5.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile_globe),
                contentDescription = null,
                tint = itemIconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Follow us",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal,
                color = itemTextColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Instagram icon in gray color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DUTYPE_INSTAGRAM_URL)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to open Instagram")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_instagram),
                    contentDescription = "Instagram",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(19.dp)
                )
            }

            // WhatsApp icon in gray color
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DUTYPE_WHATSAPP_URL)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to open WhatsApp")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = "WhatsApp",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Shared signature brand footer for DutyPe.
 * Displays "India's #1" and "local jobs app" with very light gray text and blue heart.
 * Featured prominently at the bottom of Profile screens.
 */
@Composable
fun MadeWithLoveFooter(
    modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 35.dp)
) {
    val isDark = com.example.dutype.ui.theme.LocalDarkMode.current
    val brandTextColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "India's #1",
            fontSize = 35.sp,
            fontWeight = FontWeight.ExtraBold,
            color = brandTextColor,
            lineHeight = 39.sp,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "local jobs app",
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                color = brandTextColor,
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.width(9.dp))
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "DutyPe Blue Heart",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(31.dp)
            )
        }
    }
}
