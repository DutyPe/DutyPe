package com.example.dutype.worker.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.navigation.Routes
import com.example.dutype.utils.appVersionName

private val AboutBg = Color(0xFFF8FAFC)
private val AboutInk = Color(0xFF0F0F0F)
private val AboutBorder = Color(0xFFE2E8F0)
private val AboutEmerald = Color(0xFF10B981)
private val AboutSlate400 = Color(0xFF94A3B8)
private val AboutSlate500 = Color(0xFF64748B)
private val AboutSlate600 = Color(0xFF475569)
private val AboutPill = Color(0xFFF1F5F9)

private const val ABOUT_PACKAGE = "com.dutype.app"
private const val ABOUT_WHATSAPP_URL = "https://chat.whatsapp.com/ITnhw0jk2G0I9TNlDCaNQI?s=cl&p=a&ilr=4"

private class AboutNavItem(
    @DrawableRes val icon: Int,
    val title: String,
    val onClick: () -> Unit
)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
    }
}

private fun openPlayStore(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$ABOUT_PACKAGE")))
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$ABOUT_PACKAGE")
    } catch (_: Exception) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$ABOUT_PACKAGE")
    }
}

private fun safeNavigate(navController: NavController, route: String) {
    try {
        navController.navigate(route)
    } catch (_: Exception) {
    }
}

@Composable
fun WorkerAboutScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange(AboutBg)
    }

    val context = LocalContext.current
    val versionText = "Version " + context.appVersionName() + " (Stable Build)"

    val items = listOf(
        AboutNavItem(R.drawable.ic_about_shield, "Safety & Fair Work Guidelines") {
            safeNavigate(navController, Routes.HELP)
        },
        AboutNavItem(R.drawable.ic_about_scale, "Worker Rights & Zero-Exploitation Policy") {
            safeNavigate(navController, Routes.TERMS_OF_SERVICE)
        },
        AboutNavItem(R.drawable.ic_about_star, "Rate Us on Google Play Store") {
            openPlayStore(context)
        },
        AboutNavItem(R.drawable.ic_about_message, "Join DutyPe WhatsApp Community") {
            openUrl(context, ABOUT_WHATSAPP_URL)
        },
        AboutNavItem(R.drawable.ic_about_file, "Open Source Licenses & Legal") {
            safeNavigate(navController, Routes.PRIVACY_POLICY)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AboutBg)
            .statusBarsPadding()
    ) {
        AboutTopBar(onBack = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AboutBrandCard(versionText)
            AboutPromiseCard()
            AboutNavCard(items)
            Text(
                text = "DutyPe Technologies Private Limited · Hyderabad, India",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                fontSize = 11.sp,
                color = AboutSlate400,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AboutTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_about_back),
            contentDescription = "Back",
            tint = Color.Unspecified,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBack() }
        )
        Text(
            text = "About DutyPe",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AboutInk
        )
    }
}

@Composable
private fun AboutCardBox(
    padding: Int,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, AboutBorder, shape)
            .padding(padding.dp)
    ) {
        content()
    }
}

@Composable
private fun AboutBrandCard(versionText: String) {
    AboutCardBox(padding = 24) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DutyPe",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AboutInk
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AboutEmerald)
            )
            Text(
                text = versionText,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AboutPill)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AboutSlate500
            )
            Text(
                text = "Empowering India's Blue-Collar Workforce with Dignity, Direct Access & 100% Fair Pay.",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = AboutSlate600,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Made with pride in India",
                modifier = Modifier.padding(top = 10.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AboutEmerald
            )
        }
    }
}

@Composable
private fun AboutPromiseCard() {
    AboutCardBox(padding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "OUR WORKER PROMISE",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AboutSlate400,
                letterSpacing = 0.6.sp
            )
            AboutPromiseRow(
                "0% Commission:",
                " You keep 100% of your earnings. DutyPe never takes cuts from worker wages."
            )
            AboutPromiseRow(
                "Direct Contact:",
                " Speak directly to the business owner or employer with zero middlemen."
            )
            AboutPromiseRow(
                "Verified Gigs:",
                " Employers must complete mobile & business verification before posting."
            )
        }
    }
}

@Composable
private fun AboutPromiseRow(lead: String, rest: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_about_check),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = AboutInk, fontWeight = FontWeight.SemiBold)) {
                    append(lead)
                }
                append(rest)
            },
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = AboutSlate600
        )
    }
}

@Composable
private fun AboutNavCard(items: List<AboutNavItem>) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White)
            .border(1.dp, AboutBorder, shape)
    ) {
        items.forEachIndexed { index, item ->
            AboutNavRow(item, showDivider = index < items.size - 1)
        }
    }
}

@Composable
private fun AboutNavRow(item: AboutNavItem, showDivider: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { item.onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = item.title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = AboutInk
            )
            Text(text = "›", fontSize = 16.sp, color = AboutSlate400)
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AboutPill)
            )
        }
    }
}
