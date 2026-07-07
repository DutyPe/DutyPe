package com.example.dutype.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.example.dutype.viewmodels.AppConfigViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DutyPeLoader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color(0xFF8B5CF6)
) {
    val appConfigViewModel: AppConfigViewModel = hiltViewModel()
    val config by appConfigViewModel.dynamicFeaturesConfig.collectAsState()
    val lottieUrl = config.lottieLoadingUrl

    if (lottieUrl.isNotBlank()) {
        val compositionResult = rememberLottieComposition(LottieCompositionSpec.Url(lottieUrl))
        val composition = compositionResult.value
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(size)
                )
            } else {
                CircularProgressIndicator(
                    color = color,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    } else {
        CircularProgressIndicator(
            color = color,
            modifier = modifier.size(size)
        )
    }
}
