package com.example.dutype.common.chat.help

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

data class TutorialSection(
    val title: String,
    val emoji: String,
    val steps: List<TutorialStep>
)

data class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onStatusBarColorChange: (Color) -> Unit
) {
    onStatusBarColorChange(Color.White)
    
    val workerTutorials = listOf(
        TutorialSection(
            title = "Find Jobs Near You",
            emoji = "🔍",
            steps = listOf(
                TutorialStep(1, "Open the App", "Launch DutyPe and allow location access to see jobs near you"),
                TutorialStep(2, "Browse Jobs", "Scroll through available jobs on the home screen or use the map view"),
                TutorialStep(3, "Filter Jobs", "Use category filters like Delivery, Cleaning, Helper to find specific work"),
                TutorialStep(4, "Check Distance", "Each job shows how far it is from your current location")
            )
        ),
        TutorialSection(
            title = "Apply for a Job",
            emoji = "📝",
            steps = listOf(
                TutorialStep(1, "Tap on Job Card", "Select any job that interests you to see full details"),
                TutorialStep(2, "Review Details", "Check salary, timing, location, and job requirements"),
                TutorialStep(3, "Click Apply", "Tap the 'Apply Now' button at the bottom"),
                TutorialStep(4, "Complete Profile", "If first time, fill your basic details like name and phone"),
                TutorialStep(5, "Submit Application", "Review and submit your application to the employer")
            )
        ),
        TutorialSection(
            title = "Track Your Applications",
            emoji = "📋",
            steps = listOf(
                TutorialStep(1, "Go to My Jobs", "Tap 'My Jobs' in the bottom navigation bar"),
                TutorialStep(2, "View Status", "See all your applications with their current status"),
                TutorialStep(3, "Check Updates", "Applications show Pending, Accepted, or Rejected status"),
                TutorialStep(4, "Contact Employer", "Once accepted, you can call or message the employer")
            )
        ),
        TutorialSection(
            title = "Complete Your Profile",
            emoji = "👤",
            steps = listOf(
                TutorialStep(1, "Go to Profile", "Tap 'Profile' in the bottom navigation"),
                TutorialStep(2, "Add Photo", "Upload a clear photo of yourself"),
                TutorialStep(3, "Fill Details", "Add your skills, experience, and preferred job types"),
                TutorialStep(4, "Add Documents", "Upload Aadhaar or other ID for verification (optional)"),
                TutorialStep(5, "Get Verified", "Complete profile gets more job responses!")
            )
        ),
        TutorialSection(
            title = "Start Work & Get Paid",
            emoji = "💰",
            steps = listOf(
                TutorialStep(1, "Reach Location", "Go to the job location at the scheduled time"),
                TutorialStep(2, "Show QR Code", "Open your accepted job and show QR to employer"),
                TutorialStep(3, "Complete Work", "Do your best work as per job requirements"),
                TutorialStep(4, "Get Payment", "Receive payment directly from employer after work")
            )
        )
    )
    
    val employerTutorials = listOf(
        TutorialSection(
            title = "Post a New Job",
            emoji = "📢",
            steps = listOf(
                TutorialStep(1, "Tap Post Job", "Click the '+' button or 'Post Job' on dashboard"),
                TutorialStep(2, "Select Category", "Choose job type like Delivery, Cleaning, Helper, etc."),
                TutorialStep(3, "Add Details", "Enter job title, description, and requirements"),
                TutorialStep(4, "Set Pay & Timing", "Specify salary (per day/hour) and work schedule"),
                TutorialStep(5, "Add Location", "Set the exact work location for workers to find"),
                TutorialStep(6, "Publish Job", "Review and post your job to start receiving applications")
            )
        ),
        TutorialSection(
            title = "Review Applications",
            emoji = "👥",
            steps = listOf(
                TutorialStep(1, "Go to My Jobs", "View all your posted jobs on the dashboard"),
                TutorialStep(2, "Check Applicants", "Tap on a job to see who has applied"),
                TutorialStep(3, "View Profiles", "Check worker profiles, experience, and ratings"),
                TutorialStep(4, "Accept or Reject", "Select the best candidates for your job"),
                TutorialStep(5, "Contact Worker", "Call or message accepted workers directly")
            )
        ),
        TutorialSection(
            title = "Verify Work Start",
            emoji = "✅",
            steps = listOf(
                TutorialStep(1, "Worker Arrives", "When worker reaches, they'll show a QR code"),
                TutorialStep(2, "Scan QR", "Use the app to scan and verify work start"),
                TutorialStep(3, "Track Progress", "Monitor work completion through the app"),
                TutorialStep(4, "Complete & Pay", "Mark work as done and pay the worker")
            )
        ),
        TutorialSection(
            title = "Build Trust & Get More Workers",
            emoji = "⭐",
            steps = listOf(
                TutorialStep(1, "Complete Profile", "Add company details and verification documents"),
                TutorialStep(2, "Pay on Time", "Always pay workers promptly after work"),
                TutorialStep(3, "Get Good Reviews", "Treat workers well to earn positive ratings"),
                TutorialStep(4, "Earn Trust Badges", "Verified employers get more applications!")
            )
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        CommonHeader(
            title = "How to Use DutyPe",
            navController = navController,
            backgroundColor = Color.White
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // For Workers Section
            item {
                SectionHeader(
                    title = "For Job Seekers",
                    subtitle = "Find and apply for daily wage jobs",
                    color = Color(0xFF10B981)
                )
            }
            
            items(workerTutorials) { tutorial ->
                ExpandableTutorialCard(tutorial = tutorial, accentColor = Color(0xFF10B981))
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // For Employers Section
            item {
                SectionHeader(
                    title = "For Employers",
                    subtitle = "Post jobs and hire workers",
                    color = Color(0xFF3B82F6)
                )
            }
            
            items(employerTutorials) { tutorial ->
                ExpandableTutorialCard(tutorial = tutorial, accentColor = Color(0xFF3B82F6))
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    color: Color
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF6B7280)
            )
        )
    }
}

@Composable
private fun ExpandableTutorialCard(
    tutorial: TutorialSection,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tutorial.emoji,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tutorial.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF111827)
                            )
                        )
                        Text(
                            text = "${tutorial.steps.size} steps",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF)
                            )
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF9CA3AF)
                )
            }
            
            // Expanded Content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                tutorial.steps.forEach { step ->
                    StepItem(step = step, accentColor = accentColor)
                    if (step != tutorial.steps.last()) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    step: TutorialStep,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // Step Number Circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${step.stepNumber}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151)
                )
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp
                )
            )
        }
    }
}
