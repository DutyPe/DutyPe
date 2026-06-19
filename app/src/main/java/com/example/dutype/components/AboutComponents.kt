package com.example.dutype.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.dutype.app.R
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.WorkerColors

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
 * Hero block at the top of an About screen — colored badge + title + tagline.
 */
@Composable
fun AboutHero(
    title: String,
    subtitle: String,
    accentColor: Color,
    badgeEmoji: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF0F0F0F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_splash_logo_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = AppTypography.pageTitle.copy(
                        fontWeight = FontWeight.Bold,
                        color = WorkerColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = AppTypography.bodyMedium.copy(color = WorkerColors.TextSecondary)
                )
            }
        }
    }
}

/**
 * Section card with a small accent bar, icon, title, and slot for body content.
 */
@Composable
fun AboutSectionCard(
    title: String,
    accentColor: Color,
    icon: String,
    body: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = aboutIconFor(icon),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = AppTypography.sectionHeader.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WorkerColors.TextPrimary
                        )
                    )
                }
                body()
            }
        }
    }
}

/**
 * A bullet row used inside About sections — small accent dot + text.
 */
@Composable
fun AboutBullet(text: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .background(accentColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(color = WorkerColors.TextPrimary)
        )
    }
}

/**
 * Plain paragraph block used for mission/vision style copy in About screens.
 */
@Composable
fun AboutParagraph(text: String) {
    Text(
        text = text,
        style = AppTypography.bodyMedium.copy(color = WorkerColors.TextPrimary)
    )
}

/**
 * Footer used at the end of every About screen — "Made with love" + version.
 */
@Composable
fun AboutFooter(version: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.about_made_in_bharat),
            style = AppTypography.bodyMedium.copy(
                color = WorkerColors.TextSecondary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_version_format, version),
            style = AppTypography.bodySmall.copy(color = WorkerColors.TextTertiary),
            textAlign = TextAlign.Center
        )
    }
}
