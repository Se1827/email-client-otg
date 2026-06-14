package com.se1827.emailclient.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.se1827.emailclient.wear.viewmodel.EmailViewModel
import com.se1827.emailclient.wear.viewmodel.UiState

@Composable
fun EmailListScreen(
    viewModel: EmailViewModel,
    onEmailClick: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchPendingDrafts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.NetworkError -> ErrorScreen(message = state.message, onRetry = { viewModel.fetchPendingDrafts() })
        is UiState.AuthError -> ErrorScreen(message = state.message, onRetry = { viewModel.fetchPendingDrafts() })
        is UiState.BackendUnavailable -> ErrorScreen(message = state.message, onRetry = { viewModel.fetchPendingDrafts() })
        is UiState.UnknownError -> ErrorScreen(message = state.message, onRetry = { viewModel.fetchPendingDrafts() })
        is UiState.Success -> {
            val emails = state.emails
            val totalPending = emails.size
            val criticalCount = emails.count { it.priority.equals("critical", ignoreCase = true) }
            val highCount = emails.count { it.priority.equals("high", ignoreCase = true) }
            val normalCount = emails.count { it.priority.equals("normal", ignoreCase = true) }
            val lowCount = emails.count { it.priority.equals("low", ignoreCase = true) }

            val timeAgo = android.text.format.DateUtils.getRelativeTimeSpanString(
                state.lastUpdated,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS
            ).toString()

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Pending Reviews", style = MaterialTheme.typography.caption1, color = androidx.compose.ui.graphics.Color.Gray)
                        Text(text = totalPending.toString(), style = MaterialTheme.typography.display1)
                    }
                }

                if (totalPending == 0) {
                    item {
                        Text(
                            text = "Yay! All Caught Up.",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    item {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(text = "Priority Breakdown", style = MaterialTheme.typography.caption1, color = androidx.compose.ui.graphics.Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                            if (criticalCount > 0) Text("🔴 Critical: $criticalCount", color = androidx.compose.ui.graphics.Color.Red, style = MaterialTheme.typography.body2)
                            if (highCount > 0) Text("🟠 High: $highCount", color = androidx.compose.ui.graphics.Color(0xFFFFA500), style = MaterialTheme.typography.body2)
                            if (normalCount > 0) Text("🔵 Normal: $normalCount", color = androidx.compose.ui.graphics.Color.Blue, style = MaterialTheme.typography.body2)
                            if (lowCount > 0) Text("🟢 Low: $lowCount", color = androidx.compose.ui.graphics.Color.Green, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                item {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Last Updated", style = MaterialTheme.typography.caption1, color = androidx.compose.ui.graphics.Color.Gray)
                        Text(text = if (System.currentTimeMillis() - state.lastUpdated < 60000) "Just now" else timeAgo, style = MaterialTheme.typography.body2)
                    }
                }

                if (totalPending > 0) {
                    item {
                        Text(
                            text = "Review Queue",
                            style = MaterialTheme.typography.title2,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(emails, key = { it.id }) { email ->
                        Chip(
                            onClick = { onEmailClick(email.id) },
                            label = { Text(email.senderDisplayName) },
                            secondaryLabel = { Text("${email.priority} • ${email.category}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
