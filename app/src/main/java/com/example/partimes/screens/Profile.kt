package com.example.partimes.screens
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.partimes.R
import com.example.partimes.components.NavigationRow

@Composable
fun ProfileScreen(rootNavController: NavController) {
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var userName by remember { mutableStateOf("VAMSI B") }
    var showEditDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            profileImageUri = uri
        }

    Column(
        modifier = Modifier
            .background(Color.White)
            .padding(top = 53.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Profile Image
                Image(
                    painter = if (profileImageUri != null)
                        rememberAsyncImagePainter(profileImageUri)
                    else
                        painterResource(id = R.drawable.user),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                                .clickable { showEditDialog = true },
                            tint = Color(0xFF0066FF) // Blue tint
                        )
                    }

                    Text(
                        text = "vamsib298@gmail.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
                // Right arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Spacer(modifier = Modifier.height(29.dp))
        // Profile Options with new design matching the image UI
        NavigationRow(
            imageResId = R.drawable.location, // Make sure the name matches the resource
            title = "Location & Availability",
            onClick = { rootNavController.navigate("help") }
        )

        NavigationRow(
            imageResId = R.drawable.settings, // Make sure the name matches the resource
            title = "Terms & Conditions",
            onClick = { rootNavController.navigate("terms") }
        )

        NavigationRow(
            imageResId = R.drawable.prefence, // Make sure the name matches the resource
            title = "Work Preferences",
            onClick = { rootNavController.navigate("privacy") }
        )

        Spacer(modifier = Modifier.height(8.dp))
        NavigationRow(
            imageResId = R.drawable.security, // Make sure the name matches the resource
            title = "Experience & Skills",
            onClick = { rootNavController.navigate("security") }
        )


        NavigationRow(
            imageResId = R.drawable.support, // Make sure the name matches the resource
            title = "Help & Support",
            onClick = { rootNavController.navigate("help") }
        )
        NavigationRow (
            imageResId = R.drawable.info, // Make sure the name matches the resource
            title = "About Us",
            onClick = { rootNavController.navigate("aboutUs") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        NavigationRow (
            imageResId = R.drawable.log_out, // Make sure the name matches the resource
            title = "Log Out",
            onClick = { rootNavController.navigate("logout") }
        )

    }

    if (showEditDialog) {
        var newName by remember { mutableStateOf(userName) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    userName = newName
                    showEditDialog = false
                }) {
                    Text("Save", color = Color(0xFF0066FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Enter new name") },
                    singleLine = true
                )
            }
        )
    }
}

