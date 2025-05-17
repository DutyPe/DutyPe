package com.example.partimes.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedSearchBar(
    onSearchQueryChanged: (String) -> Unit = {}
) {
    val hints = listOf("Delivery Boy", "Maid", "Store Assistant", "Shopkeeper", "Painter")
    var currentHintIndex by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Cycle hint text
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            currentHintIndex = (currentHintIndex + 1) % hints.size
        }
    }

    // Debounced search
    LaunchedEffect(searchText) {
        delay(300) // debounce
        onSearchQueryChanged(searchText)
    }

    val hintText = hints[currentHintIndex]

    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .padding(horizontal = 3.dp),
        placeholder = {
            AnimatedContent(
                targetState = hintText,
                transitionSpec = {
                    fadeIn(tween(250)) with fadeOut(tween(250))
                }
            ) { hint ->
                Box(
                    modifier = Modifier.fillMaxWidth(), // Ensures it takes full width
                    contentAlignment = Alignment.CenterStart // Align text to start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Search ",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = hint,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray,
                            fontSize = 14.sp,
                            maxLines = 1,  // Prevent cutting off text
                            overflow = TextOverflow.Ellipsis // Ensure text is truncated with ellipsis if too long
                        )
                    }
                }
            }
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(22.dp),
                tint = Color.Gray
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(
                    onClick = { searchText = "" },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            Toast.makeText(context, "Voice input coming soon!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    )
}
