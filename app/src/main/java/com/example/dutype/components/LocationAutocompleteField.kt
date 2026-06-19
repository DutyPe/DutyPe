package com.example.dutype.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dutype.ui.theme.WorkerColors
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Reusable Location Autocomplete TextField Component
 * Shows location suggestions as user types
 * 
 * @param value Current location text
 * @param onValueChange Callback when text changes
 * @param onLocationSelected Callback when location is selected with coordinates
 * @param locationService LocationService instance for searching places
 * @param label Label for the text field
 * @param placeholder Placeholder text
 * @param modifier Modifier for the component
 * @param enabled Whether the field is enabled
 * @param singleLine Whether to show single line
 * @param maxLines Maximum number of lines
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (address: String, latitude: Double, longitude: Double) -> Unit,
    locationService: com.example.dutype.utils.LocationService,
    label: String = "Location",
    placeholder: String = "Search location",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    showCurrentLocationButton: Boolean = false,
    onCurrentLocationClick: (() -> Unit)? = null
) {
    // Autocomplete state
    var placeSuggestions by remember { mutableStateOf<List<com.example.dutype.models.PlaceSuggestion>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var selectedSuggestionText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(value, enabled, showSuggestions, selectedSuggestionText) {
        if (!enabled || value.length < 3 || !showSuggestions || value == selectedSuggestionText) {
            isSearching = false
            placeSuggestions = emptyList()
            return@LaunchedEffect
        }

        delay(300)
        isSearching = true
        try {
            val suggestions = locationService.searchPlaces(value)
            placeSuggestions = suggestions
            Timber.d("LocationAutocomplete: Found ${suggestions.size} suggestions for: $value")
        } catch (e: Exception) {
            Timber.e(e, "LocationAutocomplete: Failed to search places")
            placeSuggestions = emptyList()
        } finally {
            isSearching = false
        }
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { query ->
                selectedSuggestionText = null
                onValueChange(query)
                showSuggestions = query.length >= 3
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = WorkerColors.TextSecondary
                    )
                }
            },
            trailingIcon = if (showCurrentLocationButton && onCurrentLocationClick != null) {
                {
                    IconButton(onClick = onCurrentLocationClick) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Use Current Location",
                            tint = WorkerColors.Primary
                        )
                    }
                }
            } else null,
            colors = colors
        )
        
        // Autocomplete suggestions dropdown
        if (showSuggestions && placeSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.dutype.ui.theme.WorkerColors.CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(placeSuggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // User selected a suggestion
                                    val selectedAddress = suggestion.description
                                    selectedSuggestionText = selectedAddress
                                    showSuggestions = false
                                    placeSuggestions = emptyList()
                                    onValueChange(selectedAddress)
                                    onLocationSelected(
                                        selectedAddress,
                                        suggestion.latitude,
                                        suggestion.longitude
                                    )
                                    Timber.d("LocationAutocomplete: Selected - $selectedAddress")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = WorkerColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.example.dutype.ui.theme.WorkerColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
