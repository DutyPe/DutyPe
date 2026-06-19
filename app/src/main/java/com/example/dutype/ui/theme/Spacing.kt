package com.example.dutype.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * DutyPe spacing scale.
 *
 * A single 4dp-based rhythm so every screen breathes the same way. Replaces the
 * ad-hoc 10/12/14dp paddings scattered across screens with named, consistent
 * steps. Use these instead of raw `.dp` values for padding, gaps and margins.
 *
 * Usage: `Modifier.padding(Spacing.md)` or `Spacer(Modifier.height(Spacing.sm))`.
 */
object Spacing {
    /** 2dp – hairline gaps between tightly related items. */
    val xxs: Dp = 2.dp

    /** 4dp – icon-to-label gaps, chip internal padding. */
    val xs: Dp = 4.dp

    /** 8dp – default gap between related rows/elements. */
    val sm: Dp = 8.dp

    /** 12dp – inner card padding, compact list spacing. */
    val md: Dp = 12.dp

    /** 16dp – standard screen horizontal padding (the app default). */
    val lg: Dp = 16.dp

    /** 20dp – generous card padding, section inner spacing. */
    val xl: Dp = 20.dp

    /** 24dp – spacing between major sections. */
    val xxl: Dp = 24.dp

    /** 32dp – hero/top spacing on landing and auth screens. */
    val xxxl: Dp = 32.dp
}

/**
 * Standard corner radii so cards, buttons and sheets share one rounded language
 * instead of mixing 8/10/12/16dp at random.
 */
object Radius {
    /** 8dp – chips, small badges. */
    val sm: Dp = 8.dp

    /** 12dp – buttons, text fields. */
    val md: Dp = 12.dp

    /** 16dp – cards, list items. */
    val lg: Dp = 16.dp

    /** 24dp – bottom sheets, large surfaces. */
    val xl: Dp = 24.dp

    /** 999dp – fully pill-shaped elements. */
    val pill: Dp = 999.dp
}
