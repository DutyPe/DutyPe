package com.example.partimes.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.partimes.models.JobSummary
import com.example.partimes.utils.JobCardHelpers.getCategoryAnimationForJob
//import com.example.partimes.utils.JobCardHelpers.getCategoryImageForJob
import com.example.partimes.utils.JobCardHelpers.getPreferenceIcon

@Composable
fun CompactJobCard(
    job: JobSummary,
    onClick: (String) -> Unit, // Only jobId needed here
) {
    var isFavorited by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val animationRes = getCategoryAnimationForJob(job.title)


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp)
            .clickable { onClick(job.jobId) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(53.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
//                    val categoryImageRes = getCategoryImageForJob(job.title)

                    if (animationRes != null) {
                        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(animationRes))
                        LottieAnimation(
                            composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier.size(53.dp)
                        )
                    } else {
                        Text(
                            text = job.title.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1,overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 5.dp))
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(job.wage, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("• ${job.timing}", fontSize = 13.sp, color = Color.Gray)
                    }
                }

                // Share and Favorite Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Check out this job: ${job.title} - ${job.wage} -with ${job.company} at ${job.specificLocation} (${job.locationNearby}) ")
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app available to share.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.Gray,
                            modifier = Modifier.size(25.dp)
                        )
                    }

                    IconButton(onClick = { isFavorited = !isFavorited }) {
                        Icon(
                            imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorited) Color.Red else Color.Gray,
                            modifier = Modifier.size(27.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Company",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = job.company, fontSize = 13.sp, color = Color.Black)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF2979FF),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    "${job.specificLocation} (${job.locationNearby})",
                    fontSize = 13.sp,
//                    color = Color.Gray,
                    modifier = Modifier.padding(start = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${job.vacancies} Vacancies", fontSize = 12.sp, color = Color(0xFF43A047))
                }
                job.preferences.forEach { preference ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("${getPreferenceIcon(preference)} $preference", fontSize = 11.sp, color = Color.DarkGray)
                    }
                }

            }
            Spacer(modifier = Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val whatsappUri = Uri.parse("https://wa.me/${job.phoneNumber}")
                        val intent = Intent(Intent.ACTION_VIEW, whatsappUri)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2962FF))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat", color = Color.White, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${job.phoneNumber}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}
