package com.example.dutype.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Stable
class ScrollStateManager {
    // Always keep bottom bar visible - no hiding on scroll
    private val _isBottomBarVisible = mutableStateOf(true)
    val isBottomBarVisible: State<Boolean> = derivedStateOf { _isBottomBarVisible.value }

    private val _isScrollingUp = mutableStateOf(false)
    val isScrollingUp: State<Boolean> = derivedStateOf { _isScrollingUp.value }

    private var lastScrollOffset = 0f
    private var scrollOffset = 0f

    fun onScroll(offset: Float) {
        val oldOffset = scrollOffset
        scrollOffset = offset

        // Track scrolling direction but don't hide bottom bar
        val isScrollingUp = offset > oldOffset
        _isScrollingUp.value = isScrollingUp
        
        // Always keep bottom bar visible
        _isBottomBarVisible.value = true
    }

    fun showBottomBar() {
        _isBottomBarVisible.value = true
    }

    fun hideBottomBar() {
        // Don't actually hide - keep it visible
        _isBottomBarVisible.value = true
    }

    fun reset() {
        _isBottomBarVisible.value = true
        _isScrollingUp.value = false
        lastScrollOffset = 0f
        scrollOffset = 0f
    }
}

@Composable
fun rememberScrollStateManager(): ScrollStateManager {
    return remember { ScrollStateManager() }
}
