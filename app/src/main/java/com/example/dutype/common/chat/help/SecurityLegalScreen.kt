package com.example.dutype.common.chat.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader
import com.example.dutype.navigation.Routes
import com.example.dutype.ui.theme.WorkerColors

@Composable
fun SecurityLegalScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        onStatusBarColorChange(Color.White)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WorkerColors.ScreenBackground)
    ) {
        CommonHeader(
            title = "Security & Legal",
            navController = navController,
            backgroundColor = WorkerColors.CardBackground
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Legal Section Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // Privacy Policy
                        FlatMenuItem(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "How we collect, use, and protect your data",
                            onClick = { navController.navigate(Routes.PRIVACY) }
                        )
                        
                        MenuDivider()
                        
                        // Terms & Conditions
                        FlatMenuItem(
                            icon = Icons.Outlined.Gavel,
                            title = "Terms & Conditions",
                            subtitle = "Rules and guidelines for using DutyPe",
                            onClick = { navController.navigate(Routes.TERMS) }
                        )
                        
                        MenuDivider()
                        
                        // Security & Safety
                        FlatMenuItem(
                            icon = Icons.Outlined.Security,
                            title = "Security & Safety",
                            subtitle = "Tips to keep your account safe",
                            onClick = { navController.navigate(Routes.SECURITY) }
                        )
                        
                        MenuDivider()
                        
                        // Refund Policy
                        FlatMenuItem(
                            icon = Icons.Outlined.MoneyOff,
                            title = "Refund Policy",
                            subtitle = "Cancellation and refund information",
                            onClick = { navController.navigate(Routes.CANCELLATION_REFUND) }
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun FlatMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF1F2937),
            modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1F2937)
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(com.example.dutype.ui.theme.IconSizes.Standard)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 44.dp),
        thickness = 0.5.dp,
        color = Color(0xFFE5E7EB)
    )
}
