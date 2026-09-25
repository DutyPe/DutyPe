package com.example.dutype.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dutype.utils.InAppReviewManager

@Composable
fun PlayStoreRatingPrompt(
    reviewManager: InAppReviewManager
) {
    val showPrompt by reviewManager.showRatingPromptFlow.collectAsStateWithLifecycle()
    if (!showPrompt) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isTelugu = remember(configuration) {
        configuration.locales[0]?.language.equals("te", ignoreCase = true)
    }

    val title = if (isTelugu) "డ్యూటీపే మీకు నచ్చిందా?" else "Enjoying DutyPe?"
    val message = if (isTelugu) {
        "గూగుల్ ప్లే స్టోర్‌లో మీ 5-స్టార్ రేటింగ్ ఎక్కువ మంది కార్మికులకు పనులు మరియు యజమానులకు నమ్మకమైన సిబ్బందిని పొందడానికి సహాయపడుతుంది. దయచేసి 5 సెకన్లలో రేటింగ్ ఇవ్వండి!"
    } else {
        "Your 5-star rating on Google Play helps local workers find daily jobs and employers hire reliable staff. Please take 5 seconds to rate us!"
    }
    val rateButtonText = if (isTelugu) "⭐⭐⭐⭐⭐ ప్లే స్టోర్‌లో రేట్ చేయండి" else "⭐⭐⭐⭐⭐ Rate 5 Stars on Play Store"
    val laterButtonText = if (isTelugu) "తర్వాత" else "Maybe Later"

    Dialog(
        onDismissRequest = { reviewManager.dismissRatingPrompt() },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gold Star Icon header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFF8E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating Star",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 5 Stars Display
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Rate Button
                Button(
                    onClick = {
                        reviewManager.rateOnPlayStore(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    )
                ) {
                    Text(
                        text = rateButtonText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss Button
                TextButton(
                    onClick = { reviewManager.dismissRatingPrompt() }
                ) {
                    Text(
                        text = laterButtonText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
