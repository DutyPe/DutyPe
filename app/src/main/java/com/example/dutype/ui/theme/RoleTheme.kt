package com.example.dutype.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Role-based color palette exposed via [LocalRoleColors] so every composable
 * inside a worker/employer flow can read the correct background, card colour,
 * status-bar tint, etc. without hard-coding values per screen.
 *
 * Design rules (do not violate):
 *  - One single SOLID `screenBackground` per role. No gradients for general
 *    screens. Gradients are reserved for the onboarding / role-selection
 *    promotional surfaces only.
 *  - One single SOLID `cardBackground` per role used by every Card / Surface
 *    that wants to show as an elevated container.
 *  - `secondaryBackground` is the subtle bg for chip rails, search bars,
 *    secondary sections — also solid.
 *
 * Worker visual identity  : clean white surface, dark text, purple accents.
 * Employer visual identity : faint sky-blue surface, slate text, blue accents.
 */
@Immutable
data class RoleColorScheme(
    val screenBackground: Color,
    val cardBackground: Color,
    val secondaryBackground: Color,
    val statusBar: Color,
    val navigationBar: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val border: Color,
)

/**
 * Worker palette — keep the worker side feeling like a clean, premium
 * marketplace (Meesho / Apna-style). Background stays pure white so the
 * dark hero header on the Home screen stands out without adding gradients
 * elsewhere.
 */
val WorkerRoleColors: RoleColorScheme = RoleColorScheme(
    // Soft cool grey with a faint lavender tint that complements the worker
    // purple accent. Cards on top are pure white so they pop noticeably.
    screenBackground = Color(0xFFF5F6FA),
    cardBackground = Color(0xFFFFFFFF),
    secondaryBackground = Color(0xFFEEF0F6),
    statusBar = Color(0xFFF5F6FA),
    navigationBar = Color(0xFFFFFFFF),
    primary = WorkerColors.Primary,
    onPrimary = Color.White,
    textPrimary = WorkerColors.TextPrimary,
    textSecondary = WorkerColors.TextSecondary,
    divider = WorkerColors.Divider,
    border = WorkerColors.Border,
)

/**
 * Employer palette — uses a subtle, intentional light-blue surface so the
 * employer flow feels visually distinct from the worker flow at a glance,
 * while still reading as part of the same brand. Solid, no gradient.
 */
val EmployerRoleColors: RoleColorScheme = RoleColorScheme(
    // Soft sky tint that's clearly distinguishable from the worker grey at a
    // glance, while staying calm and professional. White cards on top.
    screenBackground = Color(0xFFEAF3FB),
    cardBackground = Color(0xFFFFFFFF),
    secondaryBackground = Color(0xFFDFEBF6),
    statusBar = Color(0xFFEAF3FB),
    navigationBar = Color(0xFFFFFFFF),
    primary = EmployerColors.Primary,
    onPrimary = Color.White,
    textPrimary = EmployerColors.TextPrimary,
    textSecondary = EmployerColors.TextSecondary,
    divider = EmployerColors.Divider,
    border = EmployerColors.Border,
)

/**
 * CompositionLocal for the active role colour scheme. Falls back to the
 * worker palette so any composable rendered outside a role wrapper still
 * gets sensible defaults rather than crashing.
 */
val LocalRoleColors = staticCompositionLocalOf { WorkerRoleColors }

/**
 * Wrap any worker-side subtree to publish [WorkerRoleColors] via
 * [LocalRoleColors]. Used by `WorkerMainScreen` so every nested screen and
 * card can pull its background / card / divider colour from the theme.
 */
@Composable
fun DutyPeWorkerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRoleColors provides WorkerRoleColors, content = content)
}

/**
 * Wrap any employer-side subtree to publish [EmployerRoleColors] via
 * [LocalRoleColors]. Used by `EmployerMainScreen` so every nested screen and
 * card can pull its background / card / divider colour from the theme.
 */
@Composable
fun DutyPeEmployerTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRoleColors provides EmployerRoleColors, content = content)
}
