package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dutype.ui.theme.WorkerColors

/**
 * Shared "Made with [heart] in Bharat" footer used at the bottom of both
 * worker and employer home screens. Rendered large, left-aligned, with a
 * brand-blue heart so it reads as a signature rather than fine print.
 */
@Composable
fun MadeWithLoveFooter(modifier: Modifier = Modifier) {
    val labelColor = WorkerColors.TextPrimary
    val heartColor = Color(0xFF2563EB)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 64.dp, bottom = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = stringResource(R.string.auto_made_with),
            style = MaterialTheme.typography.titleLarge.copy(
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = heartColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.auto_in_bharat),
            style = MaterialTheme.typography.titleLarge.copy(
                color = labelColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp
            )
        )
    }
}
