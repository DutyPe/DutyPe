package com.example.dutype.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.dutype.app.R

// =============================================================================
// LEGACY MATERIAL TOKENS (kept for compatibility with old Material 3 setup)
// =============================================================================
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val AppStatusBarBlue = Color(0xFF0066FF)
val AppNavigationBarBlack = Color(0xFF000000)

val PrimaryBlue = Color(0xFF2563EB)
val SecondaryTeal = Color(0xFF0D9488)
val TertiaryGold = Color(0xFFD97706)

val DarkBackground = Color(0xFF0F172A)
val LightBackground = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceLight = Color(0xFFFFFFFF)

// =============================================================================
// DARK MODE COMPOSITION LOCAL
// =============================================================================
/**
 * `true` when the app should render in dark mode. Provided by `dutypeTheme`
 * based on the user's stored ThemeMode preference (System / Light / Dark).
 *
 * Reading this inside any @Composable is the single switch that drives the
 * entire palette: WorkerColors, EmployerColors, RoleColors, status bars and
 * Material 3 colorScheme all branch off it.
 */
val LocalDarkMode = compositionLocalOf { false }

@Composable
@ReadOnlyComposable
internal fun isAppInDarkTheme(): Boolean = LocalDarkMode.current

// =============================================================================
// WORKER PALETTE
// =============================================================================
/**
 * Worker colour tokens. Each property is a @Composable getter that returns
 * the dark-mode value when [LocalDarkMode] is true, otherwise the original
 * light value. Existing call sites remain unchanged.
 */
object WorkerColors {
    val HomeGradientStart: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF000000) else Color(0xFF000000)
    val HomeGradientMiddle: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF111111) else Color(0xFF1A1A1A)
    val HomeGradientEnd: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1F1F1F) else Color(0xFF2D2D2D)

    val StatusBarColor: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFF8FAFC)

    val ScreenBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFF8FAFC)
    val CardBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1A2233) else Color(0xFFFFFFFF)

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFB6C0CC) else Color(0xFF475569)
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF8794A4) else Color(0xFF64748B)
    val TextDisabled: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF4B5563) else Color(0xFFD1D5DB)

    val Primary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF2E2E33) else Color(0xFF0F0F0F)
    val PrimaryLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF26262B) else Color(0xFFEDEDEF)
    val Secondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFFF8585) else Color(0xFFFF6B6B)
    val Accent: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981)

    val Success: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981)
    val SuccessLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    val Warning: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    val WarningLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF4A2E05) else Color(0xFFFEF3C7)
    val Error: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)
    val ErrorLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF4C1212) else Color(0xFFFEE2E2)
    val Info: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF3B82F6)
    val InfoLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0F2A4D) else Color(0xFFDBEAFE)

    val Divider: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1F2937) else Color(0xFFF3F4F6)
    val Border: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF334155) else Color(0xFFE5E7EB)
    val BorderFocused: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE6E6EA) else Color(0xFF0F0F0F)

    val IconPrimary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE5E7EB) else Color(0xFF374151)
    val IconSecondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF8794A4) else Color(0xFF9CA3AF)
    val IconAccent: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE6E6EA) else Color(0xFF0F0F0F)

    val BadgeNew: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val BadgeHot: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)
    val BadgeVerified: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981)

    val ChipBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1F2937) else Color(0xFFF3F4F6)
    val ChipSelectedBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0F2A4D) else Color(0xFFDBEAFE)
    val ChipText: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE5E7EB) else Color(0xFF374151)
    val ChipSelectedText: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFBFDBFE) else Color(0xFF1D4ED8)

    val ShimmerBase: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1A2233) else Color(0xFFF5F7FA)
    val ShimmerHighlight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF263247) else Color(0xFFFFFFFF)

    val BottomNavBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF141416) else Color(0xFFFFFFFF)
    val BottomNavSelected: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF0F0F0F)
    val BottomNavUnselected: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF7A7D82) else Color(0xFF9AA0A6)

    val CardShadow: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0x40000000) else Color(0x0D000000)
}

// =============================================================================
// EMPLOYER PALETTE
// =============================================================================
/**
 * Employer colour tokens. Same dark-mode-aware pattern as [WorkerColors].
 */
object EmployerColors {
    val HomeGradientStart: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFFFFFFF)
    val HomeGradientMiddle: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFFFFFFF)
    val HomeGradientEnd: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFFFFFFF)

    val StatusBarColor: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFFFFFFF)

    val ScreenBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0B1220) else Color(0xFFFFFFFF)
    val CardBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1A2233) else Color(0xFFFFFFFF)

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFB6C0CC) else Color(0xFF64748B)
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF8794A4) else Color(0xFF94A3B8)
    val TextDisabled: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF475569) else Color(0xFFCBD5E1)

    val Primary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF2E2E33) else Color(0xFF0F0F0F)
    val PrimaryLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF26262B) else Color(0xFFEDEDEF)
    val Secondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF38BDF8) else Color(0xFF0EA5E9)
    val SecondaryLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0C2A3D) else Color(0xFFE0F2FE)
    val Accent: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF38BDF8) else Color(0xFF0284C7)

    val Success: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981)
    val SuccessLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF064E3B) else Color(0xFFD1FAE5)
    val Warning: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFF59E0B)
    val WarningLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF4A2E05) else Color(0xFFFEF3C7)
    val Error: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)
    val ErrorLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF4C1212) else Color(0xFFFEE2E2)
    val Info: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF38BDF8) else Color(0xFF0EA5E9)
    val InfoLight: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0C2A3D) else Color(0xFFE0F2FE)

    val Divider: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1F2937) else Color(0xFFF1F5F9)
    val Border: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF334155) else Color(0xFFE2E8F0)
    val BorderFocused: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE6E6EA) else Color(0xFF0F0F0F)

    val IconPrimary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE2E8F0) else Color(0xFF334155)
    val IconSecondary: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF8794A4) else Color(0xFF94A3B8)
    val IconAccent: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE6E6EA) else Color(0xFF0F0F0F)

    val BadgeNew: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val BadgeHot: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFF87171) else Color(0xFFEF4444)
    val BadgeVerified: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF34D399) else Color(0xFF10B981)

    val ChipBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF1F2937) else Color(0xFFF1F5F9)
    val ChipSelectedBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF0F2A4D) else Color(0xFFDBEAFE)
    val ChipText: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFE2E8F0) else Color(0xFF334155)
    val ChipSelectedText: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF93C5FD) else Color(0xFF2563EB)

    val BottomNavBackground: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF141416) else Color(0xFFFFFFFF)
    val BottomNavSelected: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFFFFFFFF) else Color(0xFF0F0F0F)
    val BottomNavUnselected: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0xFF7A7D82) else Color(0xFF9AA0A6)

    val CardShadow: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) Color(0x40000000) else Color(0x0D000000)
}
