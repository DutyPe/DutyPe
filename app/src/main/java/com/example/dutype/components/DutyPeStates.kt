package com.example.dutype.components

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dutype.app.R
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import kotlinx.coroutines.delay

/**
 * Why a screen could not show its data. Each one gets its own picture, words and action, so the
 * user knows whether to check their internet, wait, or try again — never a blank screen or an
 * "empty" message when the real problem is the network.
 */
enum class LoadIssue { OFFLINE, SLOW, SERVER, DENIED, NOT_FOUND }

/** Picks the issue from an error message (Firestore, Functions, OkHttp) and the connection. */
fun loadIssueOf(message: String?, online: Boolean): LoadIssue {
    if (!online) return LoadIssue.OFFLINE
    val m = message.orEmpty().lowercase()
    return when {
        listOf("unable to resolve host", "client is offline", "network", "unavailable", "failed to connect", "no address")
            .any { it in m } -> LoadIssue.OFFLINE
        listOf("timeout", "timed out", "deadline").any { it in m } -> LoadIssue.SLOW
        listOf("permission", "unauthenticated", "not authenticated", "denied").any { it in m } -> LoadIssue.DENIED
        listOf("not found", "not_found", "no longer").any { it in m } -> LoadIssue.NOT_FOUND
        else -> LoadIssue.SERVER
    }
}

fun isOnlineNow(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** Live "has internet" for the composition; flips as soon as the phone loses / regains it. */
@Composable
fun rememberOnline(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(isOnlineNow(context)) }
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = true }
            override fun onLost(network: Network) { state.value = isOnlineNow(context) }
            override fun onUnavailable() { state.value = false }
        }
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm?.unregisterNetworkCallback(callback) } }
    }
    return state
}

/**
 * Full-screen (or [compact]) state for a load problem, with the matching pet picture:
 * offline → the pup with the unplugged cable, slow → the pup waiting by the bowl,
 * not found → the pup searching, server error → "Oops" with the unplugged pup.
 */
@Composable
fun DutyPeIssueState(
    issue: LoadIssue,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    secondary: EmptyStateAction? = null
) {
    val context = LocalContext.current
    val retry = onRetry?.let { EmptyStateAction(stringResource(R.string.state_retry), Icons.Filled.Refresh, it) }
    val (title, message) = when (issue) {
        LoadIssue.OFFLINE -> stringResource(R.string.state_offline_title) to stringResource(R.string.state_offline_body)
        LoadIssue.SLOW -> stringResource(R.string.state_slow_title) to stringResource(R.string.state_slow_body)
        LoadIssue.SERVER -> stringResource(R.string.state_error_title) to stringResource(R.string.state_error_body)
        LoadIssue.DENIED -> stringResource(R.string.state_denied_title) to stringResource(R.string.state_denied_body)
        LoadIssue.NOT_FOUND -> stringResource(R.string.state_notfound_title) to stringResource(R.string.state_notfound_body)
    }
    val settings = if (issue == LoadIssue.OFFLINE && secondary == null) {
        EmptyStateAction(stringResource(R.string.state_open_settings), Icons.Filled.Settings) {
            runCatching { context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    } else secondary
    DutyPeEmptyState(
        icon = when (issue) {
            LoadIssue.OFFLINE -> Icons.Filled.WifiOff
            LoadIssue.SLOW -> Icons.Filled.HourglassTop
            LoadIssue.SERVER -> Icons.Filled.ErrorOutline
            LoadIssue.DENIED -> Icons.Filled.Lock
            LoadIssue.NOT_FOUND -> Icons.Filled.SearchOff
        },
        badge = if (issue == LoadIssue.OFFLINE) Icons.Filled.CloudOff else null,
        tone = when (issue) {
            LoadIssue.OFFLINE, LoadIssue.SERVER -> EmptyTone.ORANGE
            LoadIssue.SLOW -> EmptyTone.BLUE
            LoadIssue.DENIED -> EmptyTone.PURPLE
            LoadIssue.NOT_FOUND -> EmptyTone.BLUE
        },
        art = when (issue) {
            LoadIssue.OFFLINE, LoadIssue.SERVER -> EmptyArt.OFFLINE
            LoadIssue.SLOW -> EmptyArt.WAITING
            LoadIssue.NOT_FOUND -> EmptyArt.SEARCH
            LoadIssue.DENIED -> null
        },
        title = title,
        message = message,
        primary = retry,
        secondary = settings,
        compact = compact,
        modifier = if (compact) modifier.fillMaxWidth() else modifier.fillMaxSize()
    )
}

/** [DutyPeIssueState] from an error message; re-checks the connection so "offline" is exact. */
@Composable
fun DutyPeErrorState(message: String?, onRetry: (() -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val online by rememberOnline()
    DutyPeIssueState(loadIssueOf(message, online), onRetry, modifier, compact)
}

/**
 * Skeleton list while loading. After [slowAfterMs] a gentle "slow internet" note appears, and
 * when the phone is offline it says so instead of shimmering forever.
 */
@Composable
fun DutyPeLoadingList(
    modifier: Modifier = Modifier,
    rows: Int = 5,
    slowAfterMs: Long = 8_000,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    row: @Composable () -> Unit = { ApplicationListItemShimmer() }
) {
    val online by rememberOnline()
    var slow by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(slowAfterMs)
        slow = true
    }
    Column(modifier = modifier) {
        AnimatedVisibility(visible = slow || !online, enter = fadeIn(), exit = fadeOut()) {
            StatusNote(
                text = stringResource(if (online) R.string.state_slow_hint else R.string.state_offline_hint),
                color = if (online) Color(0xFF2563EB) else Color(0xFFEA580C)
            )
        }
        LazyColumn(
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) {
            items(rows) { row() }
        }
    }
}

/** Thin coloured note at the top of a list ("You are offline — showing saved data"). */
@Composable
fun StatusNote(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(color.copy(alpha = 0.08f).bg(), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, color = color.fg(), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Shows [StatusNote] while offline over content that is still visible (cached data). */
@Composable
fun OfflineCachedNote(modifier: Modifier = Modifier) {
    val online by rememberOnline()
    AnimatedVisibility(visible = !online, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        StatusNote(stringResource(R.string.state_offline_cached), Color(0xFFEA580C))
    }
}
