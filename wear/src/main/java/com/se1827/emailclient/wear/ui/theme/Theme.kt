package com.se1827.emailclient.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

private val EmailAgentColors = Colors(
    primary = Color(0xFF8EA7FF),
    primaryVariant = Color(0xFF6378D8),
    secondary = Color(0xFFB7C4FF),
    background = Color(0xFF080A10),
    surface = Color(0xFF171A25),
    error = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF10152A),
    onSecondary = Color(0xFF10152A),
    onBackground = Color(0xFFE6E8F2),
    onSurface = Color(0xFFE6E8F2),
    onError = Color(0xFF3B0905)
)

@Composable
fun EmailAgentWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = EmailAgentColors,
        typography = Typography(),
        content = content
    )
}
