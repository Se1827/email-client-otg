package com.se1827.emailclient.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.ComposeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    onBack: () -> Unit,
    viewModel: ComposeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var to by remember { mutableStateOf("") }
    var cc by remember { mutableStateOf("") }
    var bcc by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var showCcBcc by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(state.sendSuccess) {
        if (state.sendSuccess) {
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("New Email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.send(to, cc, bcc, subject, body) },
                        enabled = !state.isSending
                    ) {
                        if (state.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            val selected = state.accounts.find { it.id == state.selectedAccountId }
                            Text(selected?.email ?: "Select Account")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            state.accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.email) },
                                    onClick = { viewModel.selectAccount(acc.id); expanded = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showAiDialog = true }) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Compose", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = to,
                onValueChange = { to = it },
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            HorizontalDivider()

            if (showCcBcc) {
                OutlinedTextField(
                    value = cc,
                    onValueChange = { cc = it },
                    label = { Text("CC") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = bcc,
                    onValueChange = { bcc = it },
                    label = { Text("BCC") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
                )
                HorizontalDivider()
            } else {
                TextButton(
                    onClick = { showCcBcc = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Cc/Bcc")
                }
                HorizontalDivider()
            }

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
            HorizontalDivider()

            if (state.aiDraft.isNotEmpty() && !state.sendSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AI Suggestion", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(state.aiDraft, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { body = state.aiDraft }) {
                            Text("Use This")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).defaultMinSize(minHeight = 200.dp),
                placeholder = { Text("Compose email") },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
        }
    }

    if (showAiDialog) {
        var prompt by remember { mutableStateOf("") }
        var quality by remember { mutableStateOf("balanced") }
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Compose") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("What should the email say?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Quality", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = quality == "quick", onClick = { quality = "quick" }, label = { Text("Quick") })
                        FilterChip(selected = quality == "balanced", onClick = { quality = "balanced" }, label = { Text("Balanced") })
                        FilterChip(selected = quality == "thorough", onClick = { quality = "thorough" }, label = { Text("Thorough") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.aiCompose(prompt, quality)
                    showAiDialog = false
                }) { Text("Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("Cancel") }
            }
        )
    }
}
