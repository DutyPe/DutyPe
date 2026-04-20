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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dutype.app.BuildConfig
import com.example.dutype.components.AboutBullet
import com.example.dutype.components.AboutFooter
import com.example.dutype.components.AboutHero
import com.example.dutype.components.AboutParagraph
import com.example.dutype.components.AboutSectionCard
import com.example.dutype.components.CommonHeader
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

    val accent = Color(0xFF2563EB)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.dutype.ui.theme.LocalRoleColors.current.screenBackground)
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
                accentColor = Color(0xFF10B981),
                icon = "\uD83C\uDFAF"
            ) {
                AboutParagraph(
                    text = "To empower businesses by providing a seamless, efficient, and reliable platform to connect with a flexible workforce \u2014 so you can focus on growing your business."
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.our_vision),
                accentColor = Color(0xFFF59E0B),
                icon = "\uD83D\uDD2D"
            ) {
                AboutParagraph(
                    text = "To become the leading platform for on-demand employment in India, where businesses thrive with the right talent and workers find meaningful opportunities."
                )
            }

            AboutSectionCard(
                title = stringResource(R.string.key_features_employers),
                accentColor = Color(0xFF2563EB),
                icon = "\u2728"
            ) {
                AboutBullet("Post jobs in minutes", accent)
                AboutBullet("Access a large talent pool", accent)
                AboutBullet("GPS-based attendance tracking", accent)
                AboutBullet("Verified worker profiles", accent)
                AboutBullet("Flexible hiring options", accent)
                AboutBullet("Real-time application alerts", accent)
                AboutBullet("Manage multiple postings", accent)
                AboutBullet("Track worker performance", accent)
            }

            AboutSectionCard(
                title = stringResource(R.string.hire_for_any_role),
                accentColor = Color(0xFFEF4444),
                icon = "\uD83D\uDC65"
            ) {
                AboutBullet("Delivery Personnel", Color(0xFFEF4444))
                AboutBullet("Kitchen & Cooking Staff", Color(0xFFEF4444))
                AboutBullet("Housekeeping & Cleaning", Color(0xFFEF4444))
                AboutBullet("Shop Assistants & Retail", Color(0xFFEF4444))
                AboutBullet("Childcare & Eldercare", Color(0xFFEF4444))
                AboutBullet("Maintenance Workers", Color(0xFFEF4444))
                AboutBullet("Event & Catering Staff", Color(0xFFEF4444))
            }

            AboutSectionCard(
                title = stringResource(R.string.why_choose_dutype),
                accentColor = Color(0xFF8B5CF6),
                icon = "\uD83D\uDC8E"
            ) {
                AboutBullet("Quick hiring process", Color(0xFF8B5CF6))
                AboutBullet("Verified worker database", Color(0xFF8B5CF6))
                AboutBullet("Cost-effective solutions", Color(0xFF8B5CF6))
                AboutBullet("24/7 platform access", Color(0xFF8B5CF6))
                AboutBullet("Dedicated support team", Color(0xFF8B5CF6))
            }

            AboutSectionCard(
                title = stringResource(R.string.our_core_values),
                accentColor = Color(0xFF0EA5E9),
                icon = "\uD83E\uDD1D"
            ) {
                AboutBullet("Efficiency in hiring", Color(0xFF0EA5E9))
                AboutBullet("Reliability you can trust", Color(0xFF0EA5E9))
                AboutBullet("Transparency in all dealings", Color(0xFF0EA5E9))
                AboutBullet("Empowerment for businesses", Color(0xFF0EA5E9))
            }

            Spacer(modifier = Modifier.height(8.dp))

            AboutFooter(version = BuildConfig.VERSION_NAME)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
