package com.example.dutype.navigation

import android.net.Uri
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P2-4: Replaces the `LocalBroadcastManager`-based deep-link relay between
 * [com.example.dutype.MainActivity] and [MainNavGraph].
 *
 * Activity emits via [emit] from `onCreate` / `onNewIntent`; the nav graph
 * collects [events] inside a `LaunchedEffect` and routes through
 * [com.example.dutype.utils.DeepLinkHandler]. One indirection instead of three,
 * no Android intent broadcasting machinery, and no stale receiver lifetimes.
 *
 * Replay = 1 so a deep-link that arrives while the nav graph is still
 * resolving its start destination is not lost; extraBufferCapacity allows
 * back-to-back emits without suspending the activity.
 */
@Singleton
class DeepLinkBus @Inject constructor() {
    private val _events = MutableSharedFlow<Uri>(
        replay = 1,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<Uri> = _events.asSharedFlow()

    fun emit(uri: Uri) {
        _events.tryEmit(uri)
    }

    /**
     * Drops any replayed value so the next collector starts fresh. Call from
     * the nav graph after a deep link has been routed and you want to avoid
     * re-handling it on a subsequent recomposition / activity recreation.
     */
    fun clearReplay() {
        _events.resetReplayCache()
    }
}
