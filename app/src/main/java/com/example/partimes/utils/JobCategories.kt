package com.example.partimes.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow

@Composable
fun ExpandableCategorySection() {
    val categories = listOf(
        "Animal Care" to "🐶",
        "Bar Staff" to "🍹",
        "Barista" to "☕",
        "Chef & Cook" to "🍕",
        "Cleaner" to "🧽",
        "Customer Service" to "📞",
        "Data Entry" to "💻",
        "Delivery Driver" to "🚚",
        "Farm Worker" to "🚜",
        "Gardening" to "🌱",
        "Healthcare" to "🏥",
        "Hotel Staff" to "🏨",
        "Kitchen Hand" to "🍽️",
        "Laborer" to "🔨",
        "Office Work" to "📋",
        "Retail" to "🛍️",
        "Security" to "🛡️",
        "Tutor" to "📚",
        "Waiter/Waitress" to "🍽️",
        "Warehouse" to "📦"
    )

    var isExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Job Categories",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                                 else Icons.Default.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
        }

        if (isExpanded) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                categories.forEach { (category, emoji) ->
                    CategoryChip(
                        text = category,
                        emoji = emoji,
                        onClick = { /* Handle category selection */ }
                    )
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.take(6)) { (category, emoji) ->
                    CategoryChip(
                        text = category,
                        emoji = emoji,
                        onClick = { /* Handle category selection */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    emoji: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFF3F4F6)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = emoji, fontSize = 14.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
            )
        }
    }
}
