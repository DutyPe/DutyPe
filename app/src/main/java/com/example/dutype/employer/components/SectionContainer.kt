package com.example.dutype.employer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Set to true by a parent that has already drawn a card-shaped container
 * around its children, so individual section composables can skip their own
 * outer Card chrome and render flat. Use [SectionContainer] inside section
 * composables to honor this signal.
 */
val LocalSectionInGroup = compositionLocalOf { false }

/**
 * Wraps a section's content. When the section is rendered standalone, draws
 * a white rounded card. When rendered inside a grouped parent (i.e.
 * [LocalSectionInGroup] is true), renders the children flat with only the
 * inner padding so the parent card is the only visible chrome.
 */
@Composable
fun SectionContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    val inGroup = LocalSectionInGroup.current
    if (inGroup) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.EmployerColors.CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
    }
}
