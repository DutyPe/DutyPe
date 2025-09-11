package com.example.partimes.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.partimes.utils.ScrollStateManager

@Composable
fun ScrollAwareLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollStateManager: ScrollStateManager? = null,
    content: LazyListScope.() -> Unit
) {
    val firstVisibleItemIndex by remember {
        derivedStateOf { state.firstVisibleItemIndex }
    }
    
    val firstVisibleItemScrollOffset by remember {
        derivedStateOf { state.firstVisibleItemScrollOffset }
    }

    LaunchedEffect(firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        scrollStateManager?.onScroll(
            offset = firstVisibleItemIndex.toFloat() + (firstVisibleItemScrollOffset / 1000f)
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = contentPadding
    ) {
        content()
    }
}
