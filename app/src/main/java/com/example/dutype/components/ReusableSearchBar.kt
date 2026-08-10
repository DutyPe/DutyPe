package com.example.dutype.components

import com.dutype.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.dutype.ui.theme.WorkerColors
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
    searchIconColor: Color = WorkerColors.Primary,
    textColor: Color = WorkerColors.TextPrimary,
    placeholderColor: Color = WorkerColors.TextTertiary,
    backgroundColor: Color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.Transparent else WorkerColors.CardBackground,
    borderColor: Color = WorkerColors.Border,
    focusedBorderColor: Color = WorkerColors.Primary,
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

    // Handle focus cleanup when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            if (isFocused) {
                focusManager.clearFocus()
                isFocused = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Clicking outside search bar clears focus
                if (isFocused) {
                    focusManager.clearFocus()
                    isFocused = false
                }
            }
    ) {
        // Enhanced Search Bar Container with modern styling
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Allow clicking on search bar to focus, but this won't unfocus
                }
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
                                color = WorkerColors.TextSecondary.copy(alpha = 0.1f),
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
                            tint = WorkerColors.TextSecondary,
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
                                isFocused = false
                                focusManager.clearFocus()
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
        targetValue = if (isPressed) WorkerColors.ChipBackground else Color.Transparent,
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
                tint = if (suggestion.isRecent) WorkerColors.TextTertiary else WorkerColors.TextSecondary,
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
                    text = stringResource(R.string.auto_recent),
                    color = WorkerColors.TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            color = WorkerColors.TextTertiary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Enhanced Arrow Icon
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = "Use suggestion",
                tint = WorkerColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
