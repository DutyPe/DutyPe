package com.example.partimes.components
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
        "Child Care" to "🧸",
        "Cleaning" to "🧽",
        "Construction & Trades" to "🛠",
        "Customer Service" to "📞",
        "Driver & Delivery" to "🛵",
        "Events & Promotion" to "🎪",
        "Fast Food" to "🍟",
        "Healthcare (Assistants/Nurses)" to "💊",
        "Host & Hostess" to "🛎",
        "Housekeeping & Maid" to "🧹",
        "Painter & Decorator" to "🎨",
        "Security Guard / Bouncer" to "🛡️",
        "Electrician & Plumber Assistant" to "🔌",
        "Gardener / Mali" to "🌿",
        "Receptionist & Front Desk" to "📋",
        "Waiter/Waitress (Cafe/Events)" to "🍽️",
        "Tiffin Service Helper" to "🍱",
        "DJ Setup & Sound Rental" to "🎧",
        "Wedding/Event Helper" to "💃",
        "Part-time Tutors" to "📚",
        "Cook (Home/Small Business)" to "👨‍🍳",
        "AC/Fridge Repair Helper" to "❄️",
        "Garment Tailor/Helper" to "🧵",
        "Beauty Parlour Assistant" to "💅",
        "Bike/Car Wash Helper" to "🚿",
        "Pet Care/Dog Walker" to "🐾",
        "Temple Helper (Pujari Assistant)" to "🛕",
        "Milk & Newspaper Delivery" to "📰",
        "Furniture Painter/Polish Helper" to "🪑",
        "Stage Setup/Lighting Crew" to "🎬"
    )

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = "Toggle categories"
                )
            }

        }
        if (!isExpanded) {
            Column(modifier = Modifier.padding(bottom = 10.dp)) { // Add bottom padding here
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories.take(5)) { (label, emoji) ->
                        CategoryChip(label = label, emoji = emoji)
                    }
                    item {
                        Text(
                            text = "Show More",
                            color = Color(0xFF2979FF),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { isExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        else {
            FlowRow(
                mainAxisSpacing = 5.dp,
                crossAxisSpacing = 5.dp,
                modifier = Modifier.padding(top = 10.dp)

            ) {
                categories.forEach { (label, emoji) ->
                    CategoryChip(label = label, emoji = emoji)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, emoji: String, onClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF2F2F2),
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
