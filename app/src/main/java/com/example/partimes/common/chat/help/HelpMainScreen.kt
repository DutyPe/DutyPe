package com.example.partimes.common.chat.help

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.example.partimes.R
import com.example.partimes.worker.components.EnhancedNavigationRow
import com.example.partimes.utils.BackNavigationTopBar

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.Black)
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Help & Support", navController = navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            EnhancedNavigationRow(
                imageResId = R.drawable.helpsupport,
                title = "FAQs",
                subtitle = "Common questions answered",
                onClick = { navController.navigate("faq") }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.chat,
                title = "Chat Support",
                subtitle = "Live or automated replies",
                onClick = { navController.navigate("chat_support") }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.whatsapp,
                title = "WhatsApp Support",
                subtitle = "Talk to a support agent",
                onClick = { navController.navigate("call_support") }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.report,
                title = "Report a Problem",
                subtitle = "Tell us what's wrong",
                onClick = { navController.navigate("report") }
            )

            EnhancedNavigationRow(
                imageResId = R.drawable.tutorial,
                title = "How to Use the App",
                subtitle = "Voice + graphic tutorials",
                onClick = { navController.navigate("tutorial") }
            )
        }
    }
}
