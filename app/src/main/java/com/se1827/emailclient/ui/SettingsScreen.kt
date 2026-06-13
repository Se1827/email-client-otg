package com.se1827.emailclient.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWipeConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = { viewModel.loadSettings() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (state.error != null) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
                                Text(state.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    // Navigation Section
                    item {
                        Text("Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                OutlinedButton(onClick = onNavigateToAccounts, modifier = Modifier.fillMaxWidth()) {
                                    Text("Manage Accounts")
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.fillMaxWidth()) {
                                    Text("View Calendar")
                                }
                            }
                        }
                    }

                    // Graph Section
                    item {
                        Text("Microsoft Graph / Outlook", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val mode = state.graphStatus?.mode ?: "unknown"
                                val modeColor = when (mode) {
                                    "live" -> Color.Green
                                    "mock" -> Color(0xFFFFA000)
                                    "error" -> MaterialTheme.colorScheme.error
                                    else -> Color.Gray
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = modeColor.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)) {
                                        Text(mode.uppercase(), color = modeColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(state.graphStatus?.userEmail ?: "Not connected", style = MaterialTheme.typography.bodyMedium)
                                }
                                state.graphStatus?.tip?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Storage Section
                    item {
                        Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    MetricChip("Emails", state.storageStats?.emails?.toString() ?: "0")
                                    MetricChip("Classified", state.storageStats?.classifications?.toString() ?: "0")
                                    MetricChip("Drafts", state.storageStats?.drafts?.toString() ?: "0")
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    MetricChip("Events", state.storageStats?.events?.toString() ?: "0")
                                    MetricChip("Accounts", state.storageStats?.accounts?.toString() ?: "0")
                                    MetricChip("Notifs", state.storageStats?.notifications?.toString() ?: "0")
                                }
                            }
                        }
                    }

                    // Danger Zone
                    item {
                        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Wipe all local data and synced emails. This action cannot be undone.", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { showWipeConfirm = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Wipe All Storage")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Storage") },
            text = { Text("Are you sure you want to wipe all storage? This will delete all emails and clear the database on the backend.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.wipeAllStorage(); showWipeConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !state.isWiping
                ) {
                    if (state.isWiping) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    else Text("Wipe")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
