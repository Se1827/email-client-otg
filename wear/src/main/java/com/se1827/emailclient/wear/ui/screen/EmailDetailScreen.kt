package com.se1827.emailclient.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.se1827.emailclient.wear.data.model.EmailItem

@Composable
fun EmailDetailScreen(
    emailItem: EmailItem,
    onSend: () -> Unit,
    onSkip: () -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emailItem.senderDisplayName, style = MaterialTheme.typography.title2)
                Text(emailItem.fullDraftBody, style = MaterialTheme.typography.body1)
            }
        }
        item {
            Button(onClick = onSend) {
                Text("Send")
            }
        }
        item {
            Button(onClick = onSkip, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Skip")
            }
        }
    }
}
