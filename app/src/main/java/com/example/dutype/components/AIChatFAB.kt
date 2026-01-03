package com.example.dutype.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AI Chat Floating Action Button
 * 
 * Provides quick access to AI chatbot from any screen.
 * Different variants for workers and employers.
 */

@Composable
fun WorkerAIChatFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF1A237E),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI Assistant"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Help",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun EmployerAIChatFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF3B82F6),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = "Business Assistant"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Help",
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Compact AI Chat FAB (icon only)
 */
@Composable
fun CompactAIChatFAB(
    onClick: () -> Unit,
    isWorker: Boolean = true,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = if (isWorker) Color(0xFF1A237E) else Color(0xFF3B82F6),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isWorker) Icons.Default.SmartToy else Icons.Default.Business,
                contentDescription = "AI Assistant"
            )
        }
    }
}

/**
 * AI Chat FAB with badge showing unread count
 */
@Composable
fun AIChatFABWithBadge(
    onClick: () -> Unit,
    unreadCount: Int = 0,
    isWorker: Boolean = true,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(
                        containerColor = Color(0xFFEF4444)
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = if (isWorker) Color(0xFF1A237E) else Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isWorker) Icons.Default.SmartToy else Icons.Default.Business,
                    contentDescription = "AI Assistant"
                )
            }
        }
    }
}
