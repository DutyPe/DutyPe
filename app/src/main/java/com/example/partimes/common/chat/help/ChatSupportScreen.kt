package com.example.partimes.common.chat.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.partimes.components.BackNavigationTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSupportScreen(navController: NavController) {
    Scaffold(
        topBar = {
            BackNavigationTopBar(title = "Chat Support", navController = navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text("Select an issue:", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            val issues = listOf(
                "I didn’t get OTP",
                "My job post is not showing",
                "I want to delete my account"
            )

            issues.forEach { issue ->
                OutlinedButton(
                    onClick = { /* Handle auto-reply or route to agent */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(issue)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Live support coming soon...", color = Color.Gray)
        }
    }
}
