package com.example.partimes.common.chat.help

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.components.BackNavigationTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSupportScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "CallSupport Screen", navController = navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Support available: 10AM – 7PM", fontSize = 16.sp)

            Button(
                onClick = {
                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+911234567890"))
                    context.startActivity(callIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📞 Call Us")
            }

            Button(
                onClick = {
                    val whatsappUrl = "https://wa.me/911234567890"
                    val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                    context.startActivity(whatsappIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("💬 WhatsApp Us")
            }
        }
    }
}
