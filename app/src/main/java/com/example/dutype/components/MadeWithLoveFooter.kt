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
 * Displays "India's #1" and "local jobs app" with blue heart/emoji.
 * Featured prominently at the bottom of Profile screens.
 */
@Composable
fun MadeWithLoveFooter(
    modifier: Modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "India's #1",
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
                text = "local jobs app",
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
