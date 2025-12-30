package com.example.dutype.common.chat.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

@Composable
fun CancellationRefundScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = "Cancellation & Refund",
            navController = navController
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 0.dp)
                .padding(start = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Cancellation & Refund Policy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "This policy explains how cancellations and refunds work for DutyPe subscription plans.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Last updated: December 28, 2025",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 1
            Text(
                text = "1. SUBSCRIPTION CANCELLATION",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "You can cancel your subscription at any time from the app settings. Upon cancellation:\n\n" +
                       "• Your subscription will remain active until the end of the current billing period\n" +
                       "• You will not be charged for the next billing cycle\n" +
                       "• You can continue using premium features until the subscription expires\n" +
                       "• After expiration, your account will revert to the Free plan",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 2
            Text(
                text = "2. REFUND POLICY",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Refunds are available under the following conditions:\n\n" +
                       "• Full refund within 7 days of purchase if you haven't used any premium features\n" +
                       "• Pro-rated refund for technical issues that prevent you from using the service\n" +
                       "• No refund for partial month usage after the 7-day period\n" +
                       "• Refunds are processed within 5-7 business days to the original payment method\n\n" +
                       "Cancellation/Refund Request Time: Within 7 days of purchase\n" +
                       "Refund Processing Time: 5-7 business days",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 3
            Text(
                text = "3. HOW TO REQUEST A REFUND",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "To request a refund:\n\n" +
                       "1. Email us at dutypein@gmail.com with subject 'Refund Request'\n" +
                       "2. Include your registered phone number or email\n" +
                       "3. Provide the reason for refund request\n" +
                       "4. Include your payment transaction ID (if available)\n\n" +
                       "Our team will review your request and respond within 48 hours.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 4
            Text(
                text = "4. NON-REFUNDABLE CASES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Refunds will NOT be provided in the following cases:\n\n" +
                       "• Account suspended due to violation of terms of service\n" +
                       "• Request made after 7 days of purchase (unless technical issues)\n" +
                       "• Premium features have been used (job posts created, applications viewed)\n" +
                       "• Promotional or discounted subscriptions (unless specified)",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section 5
            Text(
                text = "5. AUTO-RENEWAL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Subscriptions auto-renew by default:\n\n" +
                       "• You will be charged automatically at the end of each billing cycle\n" +
                       "• We send a reminder email 3 days before renewal\n" +
                       "• You can disable auto-renewal from subscription settings\n" +
                       "• Disabling auto-renewal does not cancel your current subscription",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Contact
            Text(
                text = "Questions about refunds?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Contact us at dutypein@gmail.com",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.Black,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.padding(bottom = 40.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
