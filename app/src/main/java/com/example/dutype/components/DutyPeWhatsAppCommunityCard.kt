package com.example.dutype.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Official DutyPe WhatsApp Community Join Card
 * Prominently displayed in Worker and Employer profile screens for emergency hiring & job updates.
 */
@Composable
fun DutyPeWhatsAppCommunityCard(
    modifier: Modifier = Modifier,
    whatsAppGroupUrl: String = "https://whatsapp.com/channel/0029VbBdNOQ1iUxZMmvg8t2G"
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl)).apply {
                        setPackage("com.whatsapp")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    // Fallback to browser if WhatsApp app is not installed
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsAppGroupUrl))
                        context.startActivity(browserIntent)
                    } catch (_: Exception) {
                        android.widget.Toast.makeText(context, "Unable to open WhatsApp link", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF075E54) // WhatsApp Teal Dark
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF25D366), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "DutyPe WhatsApp Community",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DutyPe WhatsApp Community",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

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
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
