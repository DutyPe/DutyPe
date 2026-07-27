package com.example.dutype.employer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.dutype.components.AboutDutyPeOverview
import com.example.dutype.components.AboutFooter
import com.example.dutype.components.AboutHero
import com.example.dutype.components.AboutParagraph
import com.example.dutype.components.AboutSectionCard
import com.example.dutype.components.CommonHeader
import com.example.dutype.utils.appVersionName
import androidx.compose.ui.res.stringResource
import com.dutype.app.R

@Composable
fun EmployerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    val appVersion = LocalContext.current.appVersionName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = stringResource(R.string.about_us),
            navController = navController,
            backgroundColor = Color.White
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AboutHero(
                title = stringResource(R.string.welcome_to_dutype),
                subtitle = stringResource(R.string.dutype_tagline),
                badgeEmoji = "🏢"
            )

            AboutDutyPeOverview()

            AboutSectionCard(
                title = stringResource(R.string.our_mission),
                icon = "🎯"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_employer_mission)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.our_vision),
                icon = "🔭"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_employer_vision)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.key_features_employers),
                icon = "✨"
            ) {
                AboutBullet(stringResource(R.string.about_emp_feat_post_jobs))
                AboutBullet(stringResource(R.string.about_emp_feat_talent_pool))
                AboutBullet(stringResource(R.string.about_emp_feat_gps_attendance))
                AboutBullet(stringResource(R.string.about_emp_feat_verified_workers))
                AboutBullet(stringResource(R.string.about_emp_feat_flexible_hiring))
                AboutBullet(stringResource(R.string.about_emp_feat_realtime_alerts))
                AboutBullet(stringResource(R.string.about_emp_feat_manage_postings))
                AboutBullet(stringResource(R.string.about_emp_feat_track_performance))
            }

            AboutFooter(version = appVersion)
        }
    }
}
