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
import com.example.dutype.ui.theme.WorkerColors

@Composable
fun WorkerAboutScreen(
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
            title = "About Us",
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
                title = "Welcome to DutyPe",
                subtitle = "Your gateway to reliable local jobs \u2014 daily, hourly, full-time.",
                accentColor = accent,
                badgeEmoji = "\uD83D\uDC4B"
            )

            AboutSectionCard(
                title = "Our Mission",
                accentColor = Color(0xFF10B981),
                icon = "\uD83C\uDFAF"
            ) {
                AboutParagraph(
                    text = "To empower workers by providing easy access to local job opportunities. Everyone deserves a chance to earn, grow, and succeed without complicated applications or lengthy processes."
                )
            }

            AboutSectionCard(
                title = "Our Vision",
                accentColor = Color(0xFFF59E0B),
                icon = "\uD83D\uDD2D"
            ) {
                AboutParagraph(
                    text = "To become India\u2019s most trusted platform for local employment, where every worker can find meaningful work that fits their skills, schedule, and location."
                )
            }

            AboutSectionCard(
                title = "Key Features",
                accentColor = Color(0xFF2563EB),
                icon = "\u2728"
            ) {
                AboutBullet("Quick one-tap job applications", accent)
                AboutBullet("Jobs near your location", accent)
                AboutBullet("Real-time job notifications", accent)
                AboutBullet("Save jobs for later", accent)
                AboutBullet("Track your applications", accent)
                AboutBullet("Build your work profile", accent)
            }

            AboutSectionCard(
                title = "Job Categories",
                accentColor = Color(0xFFEF4444),
                icon = "\uD83D\uDEE0\uFE0F"
            ) {
                AboutBullet("Delivery & Logistics", Color(0xFFEF4444))
                AboutBullet("Food Service & Cooking", Color(0xFFEF4444))
                AboutBullet("Housekeeping & Cleaning", Color(0xFFEF4444))
                AboutBullet("Shop & Retail Help", Color(0xFFEF4444))
                AboutBullet("Childcare & Eldercare", Color(0xFFEF4444))
                AboutBullet("Maintenance & Repairs", Color(0xFFEF4444))
            }

            AboutSectionCard(
                title = "Why Choose DutyPe?",
                accentColor = Color(0xFF8B5CF6),
                icon = "\uD83D\uDC8E"
            ) {
                AboutBullet("No resume required", Color(0xFF8B5CF6))
                AboutBullet("Verified employers", Color(0xFF8B5CF6))
                AboutBullet("Transparent pay information", Color(0xFF8B5CF6))
                AboutBullet("Flexible work options", Color(0xFF8B5CF6))
                AboutBullet("Safe and secure platform", Color(0xFF8B5CF6))
            }

            Spacer(modifier = Modifier.height(8.dp))

            AboutFooter(version = BuildConfig.VERSION_NAME)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
