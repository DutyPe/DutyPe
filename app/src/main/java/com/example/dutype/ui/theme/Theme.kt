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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2E2E33),
    secondary = Color(0xFF38BDF8),
    tertiary = TertiaryGold,
    background = Color(0xFF0B1220),
    surface = Color(0xFF1A2233),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F0F0F),
    secondary = SecondaryTeal,
    tertiary = TertiaryGold,
    background = LightBackground,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
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
            content = content,
        )
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
