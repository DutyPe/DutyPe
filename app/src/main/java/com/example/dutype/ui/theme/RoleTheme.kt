package com.example.dutype.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Role-based color palette exposed via [LocalRoleColors] so every composable
 * inside a worker/employer flow can read the correct background, card colour,
 * status-bar tint, etc. without hard-coding values per screen.
 *
 * Now dark-mode aware: every field reads from [WorkerColors]/[EmployerColors]
 * which themselves switch on [LocalDarkMode], so flipping the dark-mode flag
 * automatically repaints every screen wrapped in DutyPeWorkerTheme /
 * DutyPeEmployerTheme.
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
 * Worker palette built from [WorkerColors]. Reads dark-mode-aware tokens, so
 * worker screens flip to a deep-slate surface in dark mode automatically.
 */
@Composable
@ReadOnlyComposable
fun workerRoleColors(): RoleColorScheme = RoleColorScheme(
    screenBackground = WorkerColors.ScreenBackground,
    cardBackground = WorkerColors.CardBackground,
    secondaryBackground = WorkerColors.ScreenBackground,
    statusBar = WorkerColors.StatusBarColor,
    navigationBar = WorkerColors.BottomNavBackground,
    primary = WorkerColors.Primary,
    onPrimary = Color.White,
    textPrimary = WorkerColors.TextPrimary,
    textSecondary = WorkerColors.TextSecondary,
    divider = WorkerColors.Divider,
    border = WorkerColors.Border,
)

/**
 * Employer palette built from [EmployerColors]. Reads dark-mode-aware tokens.
 */
@Composable
@ReadOnlyComposable
fun employerRoleColors(): RoleColorScheme = RoleColorScheme(
    screenBackground = EmployerColors.ScreenBackground,
    cardBackground = EmployerColors.CardBackground,
    secondaryBackground = EmployerColors.ScreenBackground,
    statusBar = EmployerColors.StatusBarColor,
    navigationBar = EmployerColors.BottomNavBackground,
    primary = EmployerColors.Primary,
    onPrimary = Color.White,
    textPrimary = EmployerColors.TextPrimary,
    textSecondary = EmployerColors.TextSecondary,
    divider = EmployerColors.Divider,
    border = EmployerColors.Border,
)

/**
 * Static fallback (light worker palette) so any composable rendered outside
 * a role wrapper still gets sensible defaults instead of crashing. Real
 * screens are wrapped in DutyPeWorkerTheme/DutyPeEmployerTheme below which
 * publish a dark-mode-aware scheme.
 */
private val FallbackWorkerRoleColors = RoleColorScheme(
    screenBackground = Color(0xFFFFFFFF),
    cardBackground = Color(0xFFFFFFFF),
    secondaryBackground = Color(0xFFFFFFFF),
    statusBar = Color(0xFFFFFFFF),
    navigationBar = Color(0xFFFFFFFF),
    primary = Color(0xFF570DF8),
    onPrimary = Color.White,
    textPrimary = Color(0xFF1F2937),
    textSecondary = Color(0xFF6B7280),
    divider = Color(0xFFF3F4F6),
    border = Color(0xFFE5E7EB),
)

/**
 * CompositionLocal for the active role colour scheme. Falls back to the
 * worker palette so any composable rendered outside a role wrapper still
 * gets sensible defaults rather than crashing.
 */
val LocalRoleColors = staticCompositionLocalOf { FallbackWorkerRoleColors }

/**
 * Wrap any worker-side subtree to publish the worker palette via
 * [LocalRoleColors]. Used by `WorkerMainScreen` so every nested screen and
 * card can pull its background / card / divider colour from the theme.
 *
 * Dark-mode aware: rebuilds on every theme change so screens repaint
 * instantly when the user toggles light/dark.
 */
@Composable
fun DutyPeWorkerTheme(content: @Composable () -> Unit) {
    val scheme = workerRoleColors()
    CompositionLocalProvider(LocalRoleColors provides scheme, content = content)
}

/**
 * Wrap any employer-side subtree to publish the employer palette via
 * [LocalRoleColors].
 */
@Composable
fun DutyPeEmployerTheme(content: @Composable () -> Unit) {
    val scheme = employerRoleColors()
    CompositionLocalProvider(LocalRoleColors provides scheme, content = content)
}
