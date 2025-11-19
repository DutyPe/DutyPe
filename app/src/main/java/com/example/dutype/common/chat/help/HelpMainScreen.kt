package com.example.dutype.common.chat.help

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.parttime.dutype.R
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
    ) {
        // Clean Header - matching the About Us screen style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "Help & Support",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 20.sp
                )
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 80.dp)
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
