package com.example.dutype.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

data class SearchSuggestion(
    val text: String,
    val icon: ImageVector? = null,
    val isRecent: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReusableSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search for opportunities...",
    height: Int = 58,
    showClearButton: Boolean = true,
    searchIconColor: Color = Color(0xFF3B82F6),
    textColor: Color = Color(0xFF1E293B),
    placeholderColor: Color = Color(0xFF9CA3AF),
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFFE2E8F0),
    focusedBorderColor: Color = Color(0xFF3B82F6),
    cornerRadius: Int = 16,
    fontSize: Int = 16,
    onSearch: (() -> Unit)? = null,
    suggestions: List<SearchSuggestion> = emptyList(),
    onSuggestionClick: ((SearchSuggestion) -> Unit)? = null,
    isLoading: Boolean = false,
    leadingIcon: ImageVector = Icons.Default.Search,
    enabled: Boolean = true,
    maxSuggestions: Int = 5,
    showShadow: Boolean = true,
    animationDuration: Int = 300
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isFocused by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    // Enhanced Animations with better spring physics
    val animatedElevation by animateDpAsState(
        targetValue = if (isFocused) 16.dp else 4.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ), label = ""
    )

    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.5.dp else 1.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = ""
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isFocused) backgroundColor else backgroundColor.copy(alpha = 0.98f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ), label = ""
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) focusedBorderColor else borderColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = ""
    )

    val loadingRotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = tween(1000), label = ""
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = ""
    )

    // Handle suggestion visibility
    LaunchedEffect(query, isFocused, suggestions) {
        delay(100)
        showSuggestions = isFocused && suggestions.isNotEmpty() && query.isNotBlank()
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Enhanced Search Bar Container with modern styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .then(
                    if (showShadow) {
                        Modifier.shadow(
                            elevation = animatedElevation,
                            shape = RoundedCornerShape(cornerRadius.dp),
                            clip = false,
                            spotColor = if (isFocused) focusedBorderColor.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                        )
                    } else Modifier
                )
                .background(
                    brush = if (isFocused) {
                        Brush.radialGradient(
                            colors = listOf(
                                animatedBackgroundColor,
                                animatedBackgroundColor.copy(alpha = 0.95f),
                                animatedBackgroundColor.copy(alpha = 0.9f)
                            ),
                            radius = 200f
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                animatedBackgroundColor,
                                animatedBackgroundColor.copy(alpha = 0.98f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
                .border(
                    width = animatedBorderWidth,
                    color = animatedBorderColor,
                    shape = RoundedCornerShape(cornerRadius.dp)
                )
                .clip(RoundedCornerShape(cornerRadius.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Enhanced Leading Icon with better animations
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(loadingRotation),
                            strokeWidth = 3.dp,
                            color = searchIconColor,
                            trackColor = searchIconColor.copy(alpha = 0.2f)
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (isFocused) searchIconColor.copy(alpha = 0.1f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = "Search",
                                tint = if (isFocused) searchIconColor else searchIconColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer(
                                        scaleX = iconScale,
                                        scaleY = iconScale
                                    )
                            )
                        }
                    }
                }

                // Custom Text Field with Better Placeholder Handling
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = fontSize.sp,
                            color = textColor,
                            fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(searchIconColor),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onSearch?.invoke()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Enhanced Placeholder with better positioning
                                if (query.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        color = placeholderColor.copy(
                                            alpha = if (isFocused) 0.8f else 0.6f
                                        ),
                                        fontSize = fontSize.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                // Enhanced Clear Button with better styling
                AnimatedVisibility(
                    visible = showClearButton && query.isNotEmpty() && enabled,
                    enter = fadeIn(animationSpec = tween(animationDuration)) +
                           androidx.compose.animation.scaleIn(
                               animationSpec = spring(
                                   dampingRatio = Spring.DampingRatioMediumBouncy,
                                   stiffness = Spring.StiffnessMedium
                               )
                           ),
                    exit = fadeOut(animationSpec = tween(animationDuration / 2)) +
                          androidx.compose.animation.scaleOut(
                              animationSpec = tween(animationDuration / 2)
                          )
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF6B7280).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onQueryChange("")
                                focusRequester.requestFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Enhanced Suggestions Dropdown with modern styling
        AnimatedVisibility(
            visible = showSuggestions && enabled,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(
                animationSpec = tween(animationDuration)
            ),
            exit = shrinkVertically(
                animationSpec = tween(animationDuration)
            ) + fadeOut(
                animationSpec = tween(animationDuration)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .zIndex(1000f),
                shape = RoundedCornerShape(cornerRadius.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = backgroundColor
                )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = suggestions.take(maxSuggestions),
                        key = { it.text }
                    ) { suggestion ->
                        EnhancedSuggestionItem(
                            suggestion = suggestion,
                            onSuggestionClick = { selectedSuggestion ->
                                onSuggestionClick?.invoke(selectedSuggestion)
                                onQueryChange(selectedSuggestion.text)
                                showSuggestions = false
                                onSearch?.invoke()
                            },
                            textColor = textColor,
                            query = query
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedSuggestionItem(
    suggestion: SearchSuggestion,
    onSuggestionClick: (SearchSuggestion) -> Unit,
    textColor: Color,
    query: String
) {
    var isPressed by remember { mutableStateOf(false) }

    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFF8FAFC) else Color.Transparent,
        animationSpec = tween(200), label = ""
    )

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = { onSuggestionClick(suggestion) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = animatedBackgroundColor,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enhanced Suggestion Icon
            Icon(
                imageVector = suggestion.icon ?: if (suggestion.isRecent) {
                    Icons.Default.History
                } else {
                    Icons.Default.Search
                },
                contentDescription = null,
                tint = if (suggestion.isRecent) Color(0xFF9CA3AF) else Color(0xFF6B7280),
                modifier = Modifier.size(20.dp)
            )

            // Enhanced Suggestion Text
            Text(
                text = suggestion.text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (suggestion.isRecent) FontWeight.Normal else FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Recent Indicator with Better Styling
            if (suggestion.isRecent) {
                Text(
                    text = "Recent",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF9CA3AF).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Enhanced Arrow Icon
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = "Use suggestion",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
