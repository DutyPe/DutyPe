package com.example.dutype.worker.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.components.AboutBullet
import com.example.dutype.components.AboutFooter
import com.example.dutype.components.AboutHero
import com.example.dutype.components.AboutParagraph
import com.example.dutype.components.AboutSectionCard
import com.example.dutype.components.CommonHeader
import com.example.dutype.ui.theme.WorkerColors
import com.example.dutype.utils.appVersionName
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    val accent = Color(0xFF2563EB)
    val appVersion = LocalContext.current.appVersionName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.about_us),
            navController = navController,
            backgroundColor = WorkerColors.CardBackground
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AboutHero(
                title = stringResource(R.string.welcome_to_dutype),
                subtitle = stringResource(R.string.about_worker_subtitle),
                accentColor = accent,
                badgeEmoji = "\uD83D\uDC4B"
            )

            AboutSectionCard(
                title = stringResource(R.string.our_mission),
                accentColor = Color(0xFF10B981),
                icon = "\uD83C\uDFAF"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_worker_mission)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.our_vision),
                accentColor = Color(0xFFF59E0B),
                icon = "\uD83D\uDD2D"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_worker_vision)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.key_features),
                accentColor = Color(0xFF2563EB),
                icon = "\u2728"
            ) {
                AboutBullet(stringResource(R.string.about_feat_quick_apply), accent)
                AboutBullet(stringResource(R.string.about_feat_jobs_near), accent)
                AboutBullet(stringResource(R.string.about_feat_realtime_notif), accent)
                AboutBullet(stringResource(R.string.about_feat_save_jobs), accent)
                AboutBullet(stringResource(R.string.about_feat_track_apps), accent)
                AboutBullet(stringResource(R.string.about_feat_build_profile), accent)
            }

            AboutSectionCard(
                title = stringResource(R.string.job_categories),
                accentColor = Color(0xFFEF4444),
                icon = "\uD83D\uDEE0\uFE0F"
            ) {
                AboutBullet(stringResource(R.string.about_cat_delivery), Color(0xFFEF4444))
                AboutBullet(stringResource(R.string.about_cat_food), Color(0xFFEF4444))
                AboutBullet(stringResource(R.string.about_cat_housekeeping), Color(0xFFEF4444))
                AboutBullet(stringResource(R.string.about_cat_shop), Color(0xFFEF4444))
                AboutBullet(stringResource(R.string.about_cat_childcare), Color(0xFFEF4444))
                AboutBullet(stringResource(R.string.about_cat_maintenance), Color(0xFFEF4444))
            }

            AboutSectionCard(
                title = stringResource(R.string.why_choose_dutype),
                accentColor = Color(0xFF8B5CF6),
                icon = "\uD83D\uDC8E"
            ) {
                AboutBullet(stringResource(R.string.about_why_no_resume), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_why_verified_employers), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_why_transparent_pay), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_why_flexible_work), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_why_safe_platform), Color(0xFF8B5CF6))
            }

            Spacer(modifier = Modifier.height(8.dp))

            AboutFooter(version = appVersion)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
