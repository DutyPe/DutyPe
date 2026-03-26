package com.example.dutype.common.screens.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Us") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Need Help?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "We're here to help. Choose how you'd like to reach us.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@dutypeapp.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, "DutyPe Support Request")
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Email Support", fontWeight = FontWeight.SemiBold)
                        Text("support@dutypeapp.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            ElevatedCard(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("WhatsApp Support", fontWeight = FontWeight.SemiBold)
                        Text("Message us on WhatsApp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpMainScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onStatusBarColorChange(androidx.compose.ui.graphics.Color.White)
    }
    var expandedGuideIndex by remember { mutableStateOf(-1) }
    var expandedFaqIndex by remember { mutableStateOf(-1) }

    val guideItems = listOf(
        HelpExpandableItem(
            title = "Getting Started",
            content = "1. Complete your worker profile with skills and location\n2. Keep your phone and profile photo updated\n3. Turn on notifications to avoid missing jobs"
        ),
        HelpExpandableItem(
            title = "Finding Jobs Faster",
            content = "1. Use category tabs and filters\n2. Keep location access on for nearby jobs\n3. Save jobs to revisit quickly"
        ),
        HelpExpandableItem(
            title = "Applying & Work Start",
            content = "1. Open job details and apply\n2. Track status in My Jobs\n3. Use QR verification when starting work"
        ),
        HelpExpandableItem(
            title = "Building Reputation",
            content = "1. Complete jobs on time\n2. Keep communication professional\n3. Maintain high ratings and profile completeness"
        )
    )

    val faqItems = listOf(
        HelpExpandableItem(
            title = "Why am I not seeing enough jobs?",
            content = "Enable location permissions, set the correct city, and check different tabs (Hourly/Daily/Part-time)."
        ),
        HelpExpandableItem(
            title = "How do I track my application status?",
            content = "Open My Jobs or Applied Jobs. You can see pending, shortlisted, accepted, or rejected status there."
        ),
        HelpExpandableItem(
            title = "How do I contact support?",
            content = "Use Contact Us from this section to reach us by email/WhatsApp and include screenshots for faster help."
        ),
        HelpExpandableItem(
            title = "How do I improve trust and visibility?",
            content = "Complete profile details, keep work history accurate, and collect good ratings from completed jobs."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
    ) {
        CommonHeader(
            title = "Help & FAQs",
            navController = navController
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "How can we help you today?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Find quick guides and FAQs for common worker-side issues.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Worker Guide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    guideItems.forEachIndexed { index, item ->
                        ExpandableHelpRow(
                            item = item,
                            expanded = expandedGuideIndex == index,
                            onClick = {
                                expandedGuideIndex = if (expandedGuideIndex == index) -1 else index
                            }
                        )
                        if (index < guideItems.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    faqItems.forEachIndexed { index, item ->
                        ExpandableHelpRow(
                            item = item,
                            expanded = expandedFaqIndex == index,
                            onClick = {
                                expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                            }
                        )
                        if (index < faqItems.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableHelpRow(
    item: HelpExpandableItem,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
            )
        }
    }
}

private data class HelpExpandableItem(
    val title: String,
    val content: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportProblemScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    var reportText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Problem") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (submitted) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                Text("Report Submitted", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Thank you for reporting. We'll look into this.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Go Back") }
            } else {
                Text("Report a Problem", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Describe the issue you're facing and we'll investigate.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = reportText,
                    onValueChange = { reportText = it },
                    label = { Text("Describe the problem") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
                Button(
                    onClick = { submitted = true },
                    enabled = reportText.length >= 10,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Submit Report") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    navController: NavController,
    onStatusBarColorChange: (androidx.compose.ui.graphics.Color) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorial") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Getting Started with DutyPe",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TutorialStep("1", "Create Your Profile", "Sign up with your phone number and complete your profile with your skills and experience.")
            TutorialStep("2", "Browse Jobs", "Search for jobs near you by category, location, or keyword.")
            TutorialStep("3", "Apply with One Tap", "Found a job you like? Apply instantly or contact the employer via WhatsApp.")
            TutorialStep("4", "Stay Safe", "We use AI to detect scam jobs and protect you — look for safety badges!")
            TutorialStep("5", "Build Your Reputation", "Complete jobs, earn ratings, and unlock trust badges.")
        }
    }
}

@Composable
private fun TutorialStep(number: String, title: String, description: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(number, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
