package com.example.dutype.worker.screens.myJobs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dutype.worker.helpers.WorkerHelpers
import com.example.dutype.worker.helpers.WorkerHelpers.getCategoryImageForJob
import com.example.dutype.worker.helpers.WorkerHelpers.getStatusEmoji
import com.example.dutype.worker.models.AppliedJob
import com.example.dutype.models.JobListing

@Composable
fun AppliedJobCard(
    appliedJob: AppliedJob,
    onClick: (JobListing) -> Unit
) {
    val statusColor = WorkerHelpers.getStatusColor(appliedJob.status)
    val statusEmoji = getStatusEmoji(appliedJob.status)
    val context = LocalContext.current
    var isFavorited by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable { onClick(appliedJob.jobListing) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // --- Application Status Badge ---
            Box(
                modifier = Modifier
                    .background(color = statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.Start)
            ) {
                Text(
                    text = "$statusEmoji ${appliedJob.status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            // --- Job Main Info ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                val categoryImageRes = getCategoryImageForJob(appliedJob.jobListing.title)

                if (categoryImageRes != null) {
                    Icon(
                        painter = painterResource(id = categoryImageRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    AsyncImage(
                        model = appliedJob.jobListing.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appliedJob.jobListing.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = appliedJob.jobListing.payAmount,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = appliedJob.jobListing.company,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${appliedJob.jobListing.specificLocation} (${appliedJob.jobListing.locationNearby})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                // --- Share and Favorite Icons ---
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val interactionSource = remember { MutableInteractionSource() }

                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Check out this job: ${appliedJob.jobListing.title} - ${appliedJob.jobListing.payAmount}")
                                    `package` = "com.whatsapp"
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, "WhatsA pp not installed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    )

                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorited) Color.Red else Color.Gray,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                isFavorited = !isFavorited
                            }
                    )
                }
            }
        }
    }
}
