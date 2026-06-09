package com.se1827.emailclient.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.se1827.emailclient.wear.viewmodel.EmailViewModel

@Composable
fun EmailListScreen(
    viewModel: EmailViewModel,
    onEmailClick: (String) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "AI drafts",
                style = MaterialTheme.typography.title1,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(viewModel.emails, key = { it.id }) { email ->
            Chip(
                onClick = { onEmailClick(email.id) },
                label = { Text(email.senderDisplayName) },
                secondaryLabel = { Text("${email.priority} • ${email.category}") }
            )
        }
    }
}
