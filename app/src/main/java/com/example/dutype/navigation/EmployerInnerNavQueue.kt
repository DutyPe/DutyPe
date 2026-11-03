package com.example.dutype.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Reactive and one-shot handoff queue from the outer [MainNavGraph] to the
 * nested [EmployerMainScreen] NavHost.
 */
object EmployerInnerNavQueue {
    @Volatile
    private var pendingRoute: String? = null

    private val _events = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun setPending(route: String) {
        pendingRoute = route
        _events.tryEmit(route)
    }

    fun consume(): String? {
        val route = pendingRoute
        pendingRoute = null
        _events.resetReplayCache()
        return route
    }
}
