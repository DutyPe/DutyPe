package com.example.dutype.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared signature brand footer for DutyPe.
 * Displays the large bold signature statement followed by the blue heart icon inline on line 2,
 * exactly matching the reference design layout.
 * Used across Worker Home, Hire Workers, and Customer Services screens.
 */
@Composable
fun MadeWithLoveFooter(
    modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "India's local",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF94A3B8),
            lineHeight = 36.sp,
            letterSpacing = (-0.8).sp
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "jobs & service app",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                letterSpacing = (-0.8).sp
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "DutyPe Blue Heart",
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
