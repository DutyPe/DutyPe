package com.example.partimes.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch

@Stable
class ScrollStateManager {
    private val _isBottomBarVisible = mutableStateOf(true)
    val isBottomBarVisible: State<Boolean> = derivedStateOf { _isBottomBarVisible.value }

    private val _isScrollingUp = mutableStateOf(false)
    val isScrollingUp: State<Boolean> = derivedStateOf { _isScrollingUp.value }

    private var lastScrollOffset = 0f
    private var scrollOffset = 0f

    fun onScroll(offset: Float) {
        val oldOffset = scrollOffset
        scrollOffset = offset

        // When scrolling up (offset increasing), hide bottom bar
        // When scrolling down (offset decreasing), show bottom bar
        val isScrollingUp = offset > oldOffset
        _isScrollingUp.value = isScrollingUp
        
        // Hide bottom bar when scrolling up, show when scrolling down or at top
        _isBottomBarVisible.value = !isScrollingUp || offset <= 0f
    }

    fun showBottomBar() {
        _isBottomBarVisible.value = true
    }

    fun hideBottomBar() {
        _isBottomBarVisible.value = false
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
