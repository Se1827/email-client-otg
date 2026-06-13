package com.se1827.emailclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.EmailDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailDetailScreen(
    emailId: String,
    onBack: () -> Unit,
    viewModel: EmailDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(emailId) {
        viewModel.loadEmail(emailId)
    }

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
                title = {
                    Text(
                        text = state.email?.subject ?: "Loading...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.email != null) {
                        // We would use Icons.Filled.Star but it's not strictly required in standard library, let's just use Text or a standard Icon
                        // For simplicity, let's skip the filled star check and use standard icon if available, or just a generic icon
                        IconButton(onClick = { viewModel.toggleStar() }) {
                            // Using a text indicator for star since we don't have all icons guaranteed
                            Text(if (state.email!!.isStarred) "★" else "☆", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.email != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.classify(force = true) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isClassifying
                        ) {
                            if (state.isClassifying) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Classify")
                            }
                        }

                        var showQualityMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showQualityMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isDrafting
                            ) {
                                if (state.isDrafting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text("Draft Reply")
                                }
                            }
                            DropdownMenu(
                                expanded = showQualityMenu,
                                onDismissRequest = { showQualityMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("Quick") }, onClick = { showQualityMenu = false; viewModel.generateDraft("quick") })
                                DropdownMenuItem(text = { Text("Balanced") }, onClick = { showQualityMenu = false; viewModel.generateDraft("balanced") })
                                DropdownMenuItem(text = { Text("Thorough") }, onClick = { showQualityMenu = false; viewModel.generateDraft("thorough") })
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadEmail(emailId) }) {
                            Text("Retry")
                        }
                    }
                }
                state.email != null -> {
                    val email = state.email!!
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Sender Info
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = email.sender.firstOrNull()?.toString()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = email.sender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "To: ${email.recipients.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = email.timestamp.substringBefore("T"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Classification
                        email.classification?.let { cls ->
                            item {
                                val priorityColor = when (cls.priority.lowercase()) {
                                    "critical" -> MaterialTheme.colorScheme.errorContainer
                                    "high" -> Color(0xFFFFE0B2) // Orange-ish
                                    "normal" -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color(0xFFC8E6C9) // Green-ish
                                }
                                val onPriorityColor = when (cls.priority.lowercase()) {
                                    "critical" -> MaterialTheme.colorScheme.onErrorContainer
                                    "high" -> Color(0xFFE65100)
                                    "normal" -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> Color(0xFF1B5E20)
                                }

                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(color = priorityColor, shape = RoundedCornerShape(16.dp)) {
                                                Text(cls.priority.uppercase(), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = onPriorityColor, fontWeight = FontWeight.Bold)
                                            }
                                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
                                                Text(cls.category, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                            Spacer(Modifier.weight(1f))
                                            Text("${(cls.confidence * 100).toInt()}% conf", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(cls.reasoning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Body
                        item {
                            Text(
                                text = email.body,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Draft Reply
                        email.draftReply?.let { draft ->
                            item {
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("AI Draft Reply", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.weight(1f))
                                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                                                Text("${draft.tone} • ${draft.quality}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        Text(draft.body, style = MaterialTheme.typography.bodyMedium)
                                        Button(
                                            onClick = { viewModel.approveDraft() },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = !state.isApproving
                                        ) {
                                            if (state.isApproving) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                            } else {
                                                Text("Approve & Send")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Thread
                        if (state.thread.size > 1) {
                            item {
                                var expanded by remember { mutableStateOf(false) }
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Thread (${state.thread.size} messages)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    if (expanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            state.thread.filter { it.id != emailId }.forEach { threadMsg ->
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(threadMsg.sender, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                                        Text(threadMsg.body, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
