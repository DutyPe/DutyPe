package com.example.dutype.common.chat.help

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dutype.app.R
import com.example.dutype.components.CommonHeader
import com.example.dutype.worker.components.EnhancedNavigationRow

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Common Header - consistent across all screens
        CommonHeader(
            title = "Help & Support",
            navController = navController
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
                .padding(start = 13.dp)
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
