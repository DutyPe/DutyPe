package com.example.dutype.employer.ai

import com.example.dutype.ui.theme.bd
import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dutype.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AiPicksState {
    data object Idle : AiPicksState
    data object Loading : AiPicksState
    data class Ready(val shortlist: AiShortlist) : AiPicksState
    data object Failed : AiPicksState
}

@HiltViewModel
class AiTopPicksViewModel @Inject constructor(private val service: AiHiringService) : ViewModel() {
    private val _state = MutableStateFlow<AiPicksState>(AiPicksState.Idle)
    val state: StateFlow<AiPicksState> = _state.asStateFlow()
    private var loadedKey = ""

    /** Re-asks only when the applicant set changes (the server also caches per applicant set). */
    fun load(jobId: String, applicantKey: String) {
        val key = "$jobId|$applicantKey"
        if (key == loadedKey && _state.value !is AiPicksState.Failed) return
        loadedKey = key
        _state.value = AiPicksState.Loading
        viewModelScope.launch {
            _state.value = service.shortlist(jobId).fold({ AiPicksState.Ready(it) }, { AiPicksState.Failed })
        }
    }
}

private val Purple = Color(0xFF6D28D9)
private val Ink = Color(0xFF0F172A)
private val Muted = Color(0xFF64748B)

/**
 * "AI top picks": the best 3 applicants with a one-line reason each, and one-tap Call / Hire. The
 * AI never rejects anyone; the full applicant list is still below.
 */
@Composable
fun AiTopPicksCard(
    jobId: String,
    applicantIds: List<String>,
    onCall: (applicationId: String) -> Unit,
    onHire: (applicationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (applicantIds.size < 2) return
    val viewModel: AiTopPicksViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(jobId, applicantIds) { viewModel.load(jobId, applicantIds.sorted().joinToString(",")) }
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF5FF).bg(), shape)
            .border(1.dp, Color(0xFFE9D5FF).bd(), shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Purple.fg(), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.ai_top_picks_title), color = Ink.fg(), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            (state as? AiPicksState.Ready)?.let {
                Text(stringResource(R.string.ai_top_picks_of, it.shortlist.considered), color = Muted.fg(), fontSize = 12.sp)
            }
        }
        when (val s = state) {
            AiPicksState.Idle, AiPicksState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Purple.fg(), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.ai_top_picks_loading), color = Muted.fg(), fontSize = 13.sp)
            }
            AiPicksState.Failed -> Text(stringResource(R.string.ai_top_picks_failed), color = Muted.fg(), fontSize = 13.sp)
            is AiPicksState.Ready -> {
                s.shortlist.picks.forEachIndexed { index, pick ->
                    PickRow(rank = index + 1, pick = pick, onCall = { onCall(pick.applicationId) }, onHire = { onHire(pick.applicationId) })
                }
                if (s.shortlist.aiLocked) Text(stringResource(R.string.dutype_ai_plan_needed), color = Purple.fg(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(stringResource(R.string.ai_top_picks_note), color = Muted.fg(), fontSize = 11.sp)
    }
}

@Composable
private fun PickRow(rank: Int, pick: AiPick, onCall: () -> Unit, onHire: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.bg())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(Color(0xFFEDE9FE).bg(), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (pick.photoUrl.isNotBlank()) {
                    com.example.dutype.components.OptimizedProfileImage(imageUrl = pick.photoUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
                } else {
                    Text(pick.name.trim().take(1).uppercase(), color = Purple.fg(), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("$rank. ${pick.name}", color = Ink.fg(), fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (pick.facts.isNotEmpty()) Text(pick.facts.joinToString(" · "), color = Muted.fg(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (pick.reason.isNotBlank()) Text(pick.reason, color = Ink.fg(), fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCall, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).height(44.dp)) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp), tint = Ink.fg())
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ai_top_picks_call), color = Ink.fg(), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onHire,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A).bg(), contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) { Text(stringResource(R.string.ai_top_picks_hire), fontWeight = FontWeight.Bold) }
        }
    }
}
