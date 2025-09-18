package com.example.partimes.employer.screens.referral

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestReferralSystem(
    onNewReferral: (ReferralItem) -> Unit,
    onStatusUpdate: (String) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var testCount by remember { mutableStateOf(0) }
    var lastTestTime by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Test referral names and positions
    val testNames = listOf(
        "Alex Thompson", "Maria Garcia", "James Wilson", "Lisa Chen", "Robert Taylor",
        "Jennifer Lee", "Michael Brown", "Sarah Davis", "David Miller", "Emily Johnson",
        "Chris Anderson", "Jessica White", "Daniel Martinez", "Amanda Taylor", "Kevin Brown"
    )
    
    val testPositions = listOf(
        "Software Engineer", "Marketing Manager", "Data Analyst", "UX Designer", 
        "Product Manager", "Sales Director", "HR Manager", "Finance Analyst",
        "DevOps Engineer", "Business Analyst", "Content Writer", "Project Manager"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Color(0xFF2193b0),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Test Referral System",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Test stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Tests Run",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF666666)
                        )
                    )
                    Text(
                        text = testCount.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2193b0)
                        )
                    )
                }
                
                Column {
                    Text(
                        text = "Last Test",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF666666)
                        )
                    )
                    Text(
                        text = lastTestTime.ifEmpty { "Never" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Single test button
                OutlinedButton(
                    onClick = {
                        val newName = testNames.random()
                        val newPosition = testPositions.random()
                        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                        
                        val newReferral = ReferralItem(
                            name = newName,
                            position = newPosition,
                            date = currentDate,
                            status = "Pending",
                            earnings = 0.0
                        )
                        
                        onNewReferral(newReferral)
                        testCount++
                        lastTestTime = dateFormat.format(Date())
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2193b0)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Referral")
                }
                
                // Auto test button
                Button(
                    onClick = {
                        isRunning = !isRunning
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFE53E3E) else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Stop Auto" else "Auto Test")
                }
            }
            
            // Auto test description
            if (isRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFF4CAF50),
                                CircleShape
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Auto-testing every 5 seconds",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
    
    // Auto test functionality
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                delay(5000) // 5 seconds
                
                if (isRunning) {
                    val newName = testNames.random()
                    val newPosition = testPositions.random()
                    val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                    
                    val newReferral = ReferralItem(
                        name = newName,
                        position = newPosition,
                        date = currentDate,
                        status = "Pending",
                        earnings = 0.0
                    )
                    
                    onNewReferral(newReferral)
                    testCount++
                    lastTestTime = dateFormat.format(Date())
                }
            }
        }
    }
}

// Status update test system
@Composable
fun TestStatusUpdateSystem(
    onStatusUpdate: (String) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var updateCount by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Test Status Updates",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Simulates pending referrals becoming completed",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF666666)
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    onStatusUpdate("test")
                    updateCount++
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Status Update")
            }
        }
    }
}
