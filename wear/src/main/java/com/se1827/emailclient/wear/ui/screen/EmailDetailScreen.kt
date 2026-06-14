package com.se1827.emailclient.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import com.se1827.emailclient.wear.data.model.EmailItem

private const val DRAFT_PREVIEW_LIMIT = 140

@Composable
fun EmailDetailScreen(
    emailItem: EmailItem,
    isProcessing: Boolean = false,
    onSend: () -> Unit,
    onSkip: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val draftBody = emailItem.fullDraftBody
    val isLong = draftBody.length > DRAFT_PREVIEW_LIMIT
    val displayBody = when {
        expanded || !isLong -> draftBody
        else -> "${draftBody.take(DRAFT_PREVIEW_LIMIT)}…"
    }

    val priorityColor = when (emailItem.priority.lowercase()) {
        "critical" -> Color(0xFFEF5350)
        "high"     -> Color(0xFFE87900)
        "low"      -> Color(0xFF4CAF50)
        else       -> MaterialTheme.colors.primary
    }

    if (isProcessing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Priority badge chip
        item {
            Box(
                modifier = Modifier
                    .background(
                        color = priorityColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when (emailItem.priority.lowercase()) {
                            "critical" -> Icons.Filled.PriorityHigh
                            "high"     -> Icons.Filled.Warning
                            else       -> Icons.Filled.PriorityHigh
                        },
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = emailItem.priority.uppercase(),
                        style = MaterialTheme.typography.caption2,
                        color = priorityColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sender + category
        item {
            Text(
                text = emailItem.senderDisplayName,
                style = MaterialTheme.typography.title2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        item {
            Text(
                text = emailItem.category.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Draft body
        item {
            Text(
                text = displayBody,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.85f)
            )
        }

        // "Show more" toggle
        if (isLong) {
            item {
                Button(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.fillMaxWidth(0.75f)
                ) {
                    Text(if (expanded) "Show less" else "Show more")
                }
            }
        }

        // Send full-width chip
        item {
            Chip(
                onClick = onSend,
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = MaterialTheme.colors.primary
                ),
                label = { Text("Send", fontWeight = FontWeight.SemiBold) },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Skip full-width chip
        item {
            Chip(
                onClick = onSkip,
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text("Skip") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
