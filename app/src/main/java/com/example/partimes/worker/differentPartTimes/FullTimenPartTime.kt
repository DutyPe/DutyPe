package com.example.partimes.worker.differentPartTimes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FullTimePartTimersList() {
//    val list = dummyPartTimers.filter { it.type == "Full-time" || it.type == "Part-time" }
    val starColor = Color(0xFFFFAA00)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
//        list.forEach { pt ->
//            Surface(
//                shape = RoundedCornerShape(16.dp),
//                color = Color.White,
//                shadowElevation = 2.dp,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 12.dp)
//            ) {
//                Row(
//                    modifier = Modifier.padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Surface(
//                        shape = RoundedCornerShape(12.dp),
//                        color = Color(0xFFFBBC05),
//                        modifier = Modifier.size(80.dp)
//                    ) {
//                        Box(modifier = Modifier.fillMaxSize())
//                    }
//
//                    Spacer(modifier = Modifier.width(12.dp))
//
//                    Column {
//                        Text(pt.name, fontSize = 18.sp)
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            repeat(5) {
//                                Icon(
//                                    imageVector = Icons.Default.Star,
//                                    contentDescription = null,
//                                    tint = starColor,
//                                    modifier = Modifier.size(16.dp)
//                                )
//                            }
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("${pt.rating}", fontSize = 14.sp)
//                        }
//                        Text(pt.role, fontSize = 14.sp, color = Color.DarkGray)
//                        Text("₹${pt.hourlyRate} • ${pt.distanceKm} km away", fontSize = 14.sp, color = Color.Gray)
//                    }
//                }
//            }
//        }
    }
}
