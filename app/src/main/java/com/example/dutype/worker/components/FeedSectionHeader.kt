package com.example.dutype.worker.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dutype.app.R
import com.example.dutype.models.FeedSection
import com.example.dutype.ui.theme.WorkerColors

/** Title of a feed section; [district] / [state] name the place for the fallback sections. */
@Composable
fun feedSectionTitle(section: FeedSection, district: String, state: String): String = when (section) {
    FeedSection.KM_5 -> stringResource(R.string.feed_section_km_5)
    FeedSection.KM_10 -> stringResource(R.string.feed_section_km_10)
    FeedSection.KM_15 -> stringResource(R.string.feed_section_km_15)
    FeedSection.KM_20 -> stringResource(R.string.feed_section_km_20)
    FeedSection.DISTRICT -> if (district.isNotBlank()) stringResource(R.string.feed_section_district, district)
        else stringResource(R.string.feed_section_district_plain)
    FeedSection.STATE -> if (state.isNotBlank()) stringResource(R.string.feed_section_state, state)
        else stringResource(R.string.feed_section_state_plain)
    FeedSection.ANYWHERE -> stringResource(R.string.feed_section_anywhere)
}

/**
 * Header shown where the feed moves to a new section. When the very first section is already the
 * district or state, it also says there was nothing within 20 km.
 */
@Composable
fun FeedSectionHeader(
    section: FeedSection,
    district: String,
    state: String,
    isFirst: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = if (isFirst) 0.dp else 8.dp, bottom = 2.dp)) {
        if (isFirst && !section.isNearby && section != FeedSection.ANYWHERE) {
            Text(
                text = stringResource(R.string.feed_no_jobs_within_20km),
                style = MaterialTheme.typography.bodySmall.copy(color = WorkerColors.TextSecondary)
            )
        }
        Text(
            text = feedSectionTitle(section, district, state),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = WorkerColors.TextPrimary)
        )
    }
}
