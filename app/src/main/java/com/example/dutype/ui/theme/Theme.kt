@file:Suppress("DEPRECATION")

package com.example.dutype.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.dutype.data.ThemeMode
import com.example.dutype.data.ThemePreferenceStore
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.flowOf

// Every Material role is set: the unset ones fall back to Material's lavender/pink baseline, which
// tinted default cards, sheets, chips and shimmers pink. Neutral slate in light, deep slate in dark.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF2B3548),
    onPrimaryContainer = Color(0xFFF1F5F9),
    inversePrimary = Color(0xFF0F0F0F),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0B1220),
    secondaryContainer = Color(0xFF222B3D),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF1F1300),
    tertiaryContainer = Color(0xFF3A2410),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1A2233),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF222B3D),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = Color(0xFF1A2233),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    error = Color(0xFFF87171),
    onError = Color(0xFF2A0A0A),
    errorContainer = Color(0xFF3A1518),
    onErrorContainer = Color(0xFFFECACA),
    outline = Color(0xFF3B4558),
    outlineVariant = Color(0xFF2B3548),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2B3548),
    surfaceDim = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF070C16),
    surfaceContainerLow = Color(0xFF131B2B),
    surfaceContainer = Color(0xFF1A2233),
    surfaceContainerHigh = Color(0xFF222B3D),
    surfaceContainerHighest = Color(0xFF2B3548),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F0F0F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = Color(0xFF111827),
    inversePrimary = Color(0xFFE5E7EB),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = TertiaryGold,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = SurfaceLight,
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE5E7EB),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E7EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFB),
    surfaceContainer = Color(0xFFF5F6F8),
    surfaceContainerHigh = Color(0xFFF1F2F4),
    surfaceContainerHighest = Color(0xFFECEEF1),
)

/**
 * Hilt entry-point so `dutypeTheme` (a top-level @Composable, not a Hilt
 * component) can pull the singleton [ThemePreferenceStore] off the
 * application context.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemePrefEntryPoint {
    fun themePreferenceStore(): ThemePreferenceStore
}

/**
 * CompositionLocal exposing the user's current [ThemeMode] so any screen
 * (e.g. settings) can read or mutate it without re-injecting the store.
 */
val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }

@Composable
fun dutypeTheme(
    // Disabled by default: dynamic colour pulls from the device wallpaper
    // and overrides our brand palette. Keep our own palette consistent.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val store = remember(context) {
        runCatching {
            EntryPointAccessors
                .fromApplication(context.applicationContext, ThemePrefEntryPoint::class.java)
                .themePreferenceStore()
        }.getOrNull()
    }
    val themeModeFlow = remember(store) {
        store?.themeMode ?: flowOf(ThemeMode.SYSTEM)
    }
    val themeMode by themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalThemeMode provides themeMode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
        ) {
            // Text and icons without an explicit colour follow the theme. Without this, screens built
            // from plain Box / Column (no Scaffold or Surface) fall back to black text in dark mode.
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colorScheme.onBackground,
                content = content
            )
        }
    }
}

/**
 * Forces a light Material theme + light token palette for screens that should
 * NEVER follow the global dark/light setting (e.g. first-run onboarding,
 * language picker, role selection). All `WorkerColors` / `EmployerColors`
 * tokens read inside [content] return their light values, and the
 * MaterialTheme color scheme is locked to [LightColorScheme]. The status bar
 * is also forced to white with dark icons while in this scope.
 */
@Composable
fun ForceLightTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    val outerDark = LocalDarkMode.current
    // Leaving a forced-light screen: give the bars back to the app theme (white icons in dark mode).
    androidx.compose.runtime.DisposableEffect(outerDark) {
        onDispose {
            val window = (view.context as? Activity)?.window ?: return@onDispose
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !outerDark
            controller.isAppearanceLightNavigationBars = !outerDark
        }
    }
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.White.toArgb()
            window.navigationBarColor = LightColorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }
    CompositionLocalProvider(LocalDarkMode provides false) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
