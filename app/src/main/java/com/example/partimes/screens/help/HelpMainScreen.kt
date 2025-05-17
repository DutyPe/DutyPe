package com.example.partimes.screens.help

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
import androidx.navigation.NavController
import com.example.partimes.R
import com.example.partimes.components.BackNavigationTopBar
import com.example.partimes.components.NavigationRow

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(rootNavController: NavController) {
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Help & Support", navController = rootNavController)
        }
    ) { paddingValues ->  // This will get the scaffold's content padding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply dynamic padding
                .verticalScroll(rememberScrollState()) // Scrollable Column
        ) {
            NavigationRow(
                imageResId = R.drawable.helpsupport, // Make sure the name matches the resource
                title = "FAQs",
                subtitle = "Common questions answered",
                onClick = { rootNavController.navigate("faq") }
            )

            NavigationRow(
                imageResId = R.drawable.chat, // Make sure the name matches the resource
                title = "Chat Support",
                subtitle = "Live or automated replies",
                onClick = { rootNavController.navigate("chat_support") }
            )

            NavigationRow(
                imageResId = R.drawable.whatsapp, // Make sure the name matches the resource
                title = "WhatsApp Support",
                subtitle = "Talk to a support agent",
                onClick = { rootNavController.navigate("call_support") }
            )

            NavigationRow(
                imageResId = R.drawable.report, // Make sure the name matches the resource
                title = "Report a Problem",
                subtitle = "Tell us what’s wrong",
                onClick = { rootNavController.navigate("report") }
            )

            NavigationRow(
                imageResId = R.drawable.tutorial, // Make sure the name matches the resource
                title = "How to Use the App",
                subtitle = "Voice + graphic tutorials",
                onClick = { rootNavController.navigate("tutorial") }
            )

        }
    }
}
