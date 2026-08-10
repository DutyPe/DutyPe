package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.AppTypography

private val Ink900 = Color(0xFF0F172A)
private val Ink600 = Color(0xFF475569)

private fun aboutIconFor(symbol: String): ImageVector = when (symbol) {
    "💰" -> Icons.Default.Payments
    "❤️" -> Icons.Default.Favorite
    "🚀" -> Icons.Default.TrendingUp
    "🛡️", "🔒" -> Icons.Default.Security
    "👥", "🤝" -> Icons.Default.Groups
    "🏢" -> Icons.Default.Business
    "🎯" -> Icons.Default.Flag
    "💡" -> Icons.Default.Lightbulb
    "✅" -> Icons.Default.CheckCircle
    else -> Icons.Default.Info
}

/**
 * Clean Single-Page Hero block for About Us featuring DutyPe launcher icon.
 */
@Composable
fun AboutHero(
    title: String,
    subtitle: String,
    accentColor: Color = Ink900,
    badgeEmoji: String = "👋"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "DutyPe Logo",
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = AppTypography.pageTitle.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Ink900
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = AppTypography.bodyMedium.copy(color = Ink600)
            )
        }
    }
}

/**
 * Professional "What is DutyPe?" Overview Block.
 */
@Composable
fun AboutDutyPeOverview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Ink900,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.auto_what_is_dutype),
                style = AppTypography.sectionHeader.copy(
                    fontWeight = FontWeight.Bold,
                    color = Ink900
                )
            )
        }
        Text(
            text = stringResource(R.string.auto_dutype_is_india_s_premier_instant_workforc),
            style = AppTypography.bodyMedium.copy(
                color = Ink600,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(start = 44.dp)
        )
    }
}

/**
 * Clean seamless section item (no box cards, no colorful lines).
 */
@Composable
fun AboutSectionCard(
    title: String,
    accentColor: Color = Ink900,
    icon: String,
    body: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = aboutIconFor(icon),
                    contentDescription = null,
                    tint = Ink900,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = AppTypography.sectionHeader.copy(
                    fontWeight = FontWeight.Bold,
                    color = Ink900
                )
            )
        }
        body()
    }
}

/**
 * A bullet row — uniform black bullet point (`•`), no colorful dots.
 */
@Composable
fun AboutBullet(text: String, accentColor: Color = Ink900) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Ink900
            ),
            modifier = Modifier.padding(end = 10.dp)
        )
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = Ink900,
                lineHeight = 20.sp
            )
        )
    }
}

/**
 * Plain paragraph block.
 */
@Composable
fun AboutParagraph(text: String) {
    Text(
        text = text,
        style = AppTypography.bodyMedium.copy(
            color = Ink600,
            lineHeight = 22.sp
        )
    )
}

/**
 * Footer at the end of About screen.
 */
@Composable
fun AboutFooter(version: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = stringResource(R.string.about_made_in_bharat),
            style = AppTypography.bodyMedium.copy(
                color = Ink600,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_version_format, version),
            style = AppTypography.bodySmall.copy(color = Color(0xFF94A3B8)),
            textAlign = TextAlign.Center
        )
    }
}
