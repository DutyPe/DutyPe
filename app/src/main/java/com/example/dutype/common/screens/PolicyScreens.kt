package com.example.dutype.common.screens

import com.dutype.app.R
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dutype.components.CommonHeader

private val BrandBlue = Color(0xFF2563EB)
private val Ink900 = Color(0xFF0F172A)
private val Ink600 = Color(0xFF475569)
private val Ink400 = Color(0xFF94A3B8)

/**
 * Enterprise Terms of Service Screen
 */
@Composable
fun TermsOfServiceScreen(navController: NavController) {
    PolicyMainContainer(
        navController = navController,
        initialTabIndex = 0
    )
}

/**
 * Enterprise Privacy Policy Screen
 */
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    PolicyMainContainer(
        navController = navController,
        initialTabIndex = 1
    )
}

@Composable
private fun PolicyMainContainer(
    navController: NavController,
    initialTabIndex: Int
) {
    var selectedTab by remember { mutableIntStateOf(initialTabIndex) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CommonHeader(
            title = if (selectedTab == 0) stringResource(R.string.terms_of_service) else stringResource(R.string.privacy_policy),
            onBackClick = { navController.popBackStack() },
            backgroundColor = Color.White
        )

        // Policy Toggle Tabs - Black styled titles
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Ink900,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Ink900,
                        height = 3.dp
                    )
                }
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = stringResource(R.string.terms_of_service),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = Ink900
                        )
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = stringResource(R.string.privacy_policy),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = Ink900
                        )
                    )
                }
            )
        }

        // Seamless single-page scrollable document layout (no boxes)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (selectedTab == 0) {
                TermsOfServiceContent()
            } else {
                PrivacyPolicyContent()
            }

            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))

            // Grievance Contact & Legal Footer Section (No cards, plain clean text)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        tint = Ink900,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_dutype_technologies),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Ink900
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.policy_grievance_contact),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Ink600, lineHeight = 20.sp)
                )

                // Official Available Emails
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypein@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "DutyPe Support & Policy Query")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "dutypein@gmail.com",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dutypefeedback@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "DutyPe Feedback")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "dutypefeedback@gmail.com",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Last Updated Text (No top box, placed cleanly at bottom)
                Text(
                    text = stringResource(R.string.policy_last_updated),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Ink400,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = stringResource(R.string.policy_compliance),
                    style = MaterialTheme.typography.labelSmall.copy(color = Ink400)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TermsOfServiceContent() {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        PolicySectionItem(
            number = "1",
            icon = Icons.Default.VerifiedUser,
            title = stringResource(R.string.policy_terms_s1_title),
            content = stringResource(R.string.policy_terms_s1_content)
        )

        PolicySectionItem(
            number = "2",
            icon = Icons.Default.Phone,
            title = stringResource(R.string.policy_terms_s2_title),
            content = stringResource(R.string.policy_terms_s2_content)
        )

        PolicySectionItem(
            number = "3",
            icon = Icons.Default.Payments,
            title = stringResource(R.string.policy_terms_s3_title),
            content = stringResource(R.string.policy_terms_s3_content)
        )

        PolicySectionItem(
            number = "4",
            icon = Icons.Default.AssignmentTurnedIn,
            title = stringResource(R.string.policy_terms_s4_title),
            content = stringResource(R.string.policy_terms_s4_content)
        )

        PolicySectionItem(
            number = "5",
            icon = Icons.Default.CheckCircle,
            title = stringResource(R.string.policy_terms_s5_title),
            content = stringResource(R.string.policy_terms_s5_content)
        )

        PolicySectionItem(
            number = "6",
            icon = Icons.Default.Gavel,
            title = stringResource(R.string.policy_terms_s6_title),
            content = stringResource(R.string.policy_terms_s6_content)
        )

        PolicySectionItem(
            number = "7",
            icon = Icons.Default.Security,
            title = stringResource(R.string.policy_terms_s7_title),
            content = stringResource(R.string.policy_terms_s7_content)
        )
    }
}

@Composable
private fun PrivacyPolicyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
        PolicySectionItem(
            number = "1",
            icon = Icons.Default.Info,
            title = stringResource(R.string.policy_privacy_s1_title),
            content = stringResource(R.string.policy_privacy_s1_content)
        )

        PolicySectionItem(
            number = "2",
            icon = Icons.Default.Policy,
            title = stringResource(R.string.policy_privacy_s2_title),
            content = stringResource(R.string.policy_privacy_s2_content)
        )

        PolicySectionItem(
            number = "3",
            icon = Icons.Default.Share,
            title = stringResource(R.string.policy_privacy_s3_title),
            content = stringResource(R.string.policy_privacy_s3_content)
        )

        PolicySectionItem(
            number = "4",
            icon = Icons.Default.Lock,
            title = stringResource(R.string.policy_privacy_s4_title),
            content = stringResource(R.string.policy_privacy_s4_content)
        )

        PolicySectionItem(
            number = "5",
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.policy_privacy_s5_title),
            content = stringResource(R.string.policy_privacy_s5_content)
        )

        PolicySectionItem(
            number = "6",
            icon = Icons.Default.Shield,
            title = stringResource(R.string.policy_privacy_s6_title),
            content = stringResource(R.string.policy_privacy_s6_content)
        )
    }
}

@Composable
private fun PolicySectionItem(
    number: String,
    icon: ImageVector,
    title: String,
    content: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Ink900,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$number. $title",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Ink900
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Ink600,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(start = 44.dp)
        )
    }
}
