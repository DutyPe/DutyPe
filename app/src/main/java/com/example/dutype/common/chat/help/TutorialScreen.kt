package com.example.dutype.common.chat.help

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import com.example.dutype.components.CommonHeader
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White) // White theme color to match worker screens
    
    // Clean Header - matching the about us page style
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Common header - used across all help screens
        CommonHeader(
            title = "How to Use the App",
            navController = navController
        )
        
        // Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            val tutorials = listOf(
                "🎥 How to apply for a job",
                "📢 How to post a job", 
                "🎤 How to upload a voice intro"
            )
            
            tutorials.forEach { tutorial ->
                TutorialCard(title = tutorial)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
@Composable
fun TutorialCard(title: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap to view video tutorial (Coming soon)",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Black.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        )
    }
}
