package com.example.partimes.utils

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.edgeToEdgePadding(): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    return this.padding(
        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        start = WindowInsets.systemBars.asPaddingValues().calculateStartPadding(layoutDirection),
        end = WindowInsets.systemBars.asPaddingValues().calculateEndPadding(layoutDirection)
    )
}

@Composable
fun Modifier.safeAreaPadding(): Modifier {
    return this.padding(
        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    )
}

@Composable
fun Modifier.horizontalSafeAreaPadding(): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    return this.padding(
        start = WindowInsets.systemBars.asPaddingValues().calculateStartPadding(layoutDirection),
        end = WindowInsets.systemBars.asPaddingValues().calculateEndPadding(layoutDirection)
    )
}

@Composable
fun Modifier.verticalSafeAreaPadding(): Modifier {
    return this.padding(
        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(),
        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    )
}

@Composable
fun Modifier.statusBarPadding(): Modifier {
    return this.padding(
        top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    )
}

@Composable
fun Modifier.navigationBarPadding(): Modifier {
    return this.padding(
        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    )
}

@Composable
fun Modifier.noSystemBarsPadding(): Modifier {
    return this.padding(0.dp)
}
