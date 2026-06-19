package com.example.dutype.employer.screens

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
import com.example.dutype.ui.theme.EmployerColors
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

    val accent = EmployerColors.Primary
    val appVersion = LocalContext.current.appVersionName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
    ) {
        CommonHeader(
            title = stringResource(R.string.about_us),
            navController = navController
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
                subtitle = stringResource(R.string.dutype_tagline),
                accentColor = accent,
                badgeEmoji = "\uD83C\uDFE2"
            )

            AboutSectionCard(
                title = stringResource(R.string.our_mission),
                accentColor = EmployerColors.Success,
                icon = "\uD83C\uDFAF"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_employer_mission)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.our_vision),
                accentColor = EmployerColors.Warning,
                icon = "\uD83D\uDD2D"
            ) {
                AboutParagraph(
                    text = stringResource(R.string.about_employer_vision)
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.key_features_employers),
                accentColor = EmployerColors.Primary,
                icon = "\u2728"
            ) {
                AboutBullet(stringResource(R.string.about_emp_feat_post_jobs), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_talent_pool), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_gps_attendance), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_verified_workers), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_flexible_hiring), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_realtime_alerts), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_manage_postings), accent)
                AboutBullet(stringResource(R.string.about_emp_feat_track_performance), accent)
            }

            AboutSectionCard(
                title = stringResource(R.string.hire_for_any_role),
                accentColor = EmployerColors.Error,
                icon = "\uD83D\uDC65"
            ) {
                AboutBullet(stringResource(R.string.about_emp_cat_delivery), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_kitchen), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_housekeeping), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_shop), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_childcare), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_maintenance_w), EmployerColors.Error)
                AboutBullet(stringResource(R.string.about_emp_cat_event), EmployerColors.Error)
            }

            AboutSectionCard(
                title = stringResource(R.string.why_choose_dutype),
                accentColor = Color(0xFF8B5CF6),
                icon = "\uD83D\uDC8E"
            ) {
                AboutBullet(stringResource(R.string.about_emp_why_quick_hiring), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_emp_why_verified_db), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_emp_why_cost_effective), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_emp_why_24_7), Color(0xFF8B5CF6))
                AboutBullet(stringResource(R.string.about_emp_why_support), Color(0xFF8B5CF6))
            }

            AboutSectionCard(
                title = stringResource(R.string.our_core_values),
                accentColor = Color(0xFF0EA5E9),
                icon = "\uD83E\uDD1D"
            ) {
                AboutBullet(stringResource(R.string.about_emp_val_efficiency), Color(0xFF0EA5E9))
                AboutBullet(stringResource(R.string.about_emp_val_reliability), Color(0xFF0EA5E9))
                AboutBullet(stringResource(R.string.about_emp_val_transparency), Color(0xFF0EA5E9))
                AboutBullet(stringResource(R.string.about_emp_val_empowerment), Color(0xFF0EA5E9))
            }

            Spacer(modifier = Modifier.height(8.dp))

            AboutFooter(version = appVersion)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
