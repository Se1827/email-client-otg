package com.se1827.emailclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.CalendarViewModel
import com.se1827.emailclient.network.CreateEventRequest
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<String?>(null) }

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
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = { viewModel.syncCalendar() }, enabled = !state.isSyncing) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = "Sync")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New Event")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadEvents() }) { Text("Retry") }
                    }
                }
                state.events.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("No upcoming events", style = MaterialTheme.typography.titleMedium)
                        Text("Sync your calendar to load events", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.events) { event ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(event.color))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                            
                            val startFormatted = try {
                                val dt = OffsetDateTime.parse(event.start)
                                dt.format(DateTimeFormatter.ofPattern("EEE d MMM · h:mm a"))
                            } catch (e: Exception) {
                                event.start
                            }

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                                    Box(modifier = Modifier.width(8.dp).fillMaxHeight().background(color))
                                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                                        Text(event.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(startFormatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                        if (event.location.isNotEmpty()) {
                                            Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (event.attendees.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                                                Text("${event.attendees.size} attendees", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    IconButton(onClick = { eventToDelete = event.id }, modifier = Modifier.align(Alignment.Top)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("") }
        var end by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start (ISO)") }, placeholder = { Text("2026-06-14T15:30:00Z") })
                    OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End (ISO)") })
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createEvent(CreateEventRequest(
                        title = title, start = start, end = end, location = location, description = desc
                    ))
                    showCreateDialog = false
                }, enabled = title.isNotBlank() && start.isNotBlank()) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete this event?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteEvent(eventToDelete!!)
                    eventToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
