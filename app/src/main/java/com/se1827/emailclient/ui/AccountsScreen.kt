package com.se1827.emailclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.se1827.emailclient.AccountViewModel
import com.se1827.emailclient.network.AccountConfigDto
import com.se1827.emailclient.network.AccountConfigRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(viewModel: AccountViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEditDialog by remember { mutableStateOf<AccountConfigDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Accounts") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.PersonAdd, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadAccounts() }) { Text("Retry") }
                    }
                }
                state.accounts.isEmpty() -> {
                    Text("No accounts found", modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.accounts) { account ->
                            val providerColor = when (account.provider.lowercase()) {
                                "imap" -> Color.Blue
                                "gmail" -> Color.Red
                                "outlook" -> Color.Magenta
                                else -> Color.Gray
                            }
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(12.dp).clip(CircleShape).background(providerColor)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(account.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            if (account.isActive) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                                            }
                                        }
                                        Text(account.email, style = MaterialTheme.typography.bodyMedium)
                                        Text(account.provider.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { showEditDialog = account }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                                    }
                                    IconButton(onClick = { accountToDelete = account.id }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || showEditDialog != null) {
        val isEdit = showEditDialog != null
        val editAcc = showEditDialog

        var name by remember { mutableStateOf(editAcc?.name ?: "") }
        var email by remember { mutableStateOf(editAcc?.email ?: "") }
        var provider by remember { mutableStateOf(editAcc?.provider ?: "imap") }
        var imapHost by remember { mutableStateOf(editAcc?.imapHost ?: "") }
        var imapPort by remember { mutableStateOf(editAcc?.imapPort?.toString() ?: "993") }
        var imapUser by remember { mutableStateOf(editAcc?.imapUser ?: "") }
        var smtpHost by remember { mutableStateOf(editAcc?.smtpHost ?: "") }
        var smtpPort by remember { mutableStateOf(editAcc?.smtpPort?.toString() ?: "587") }
        var smtpUser by remember { mutableStateOf(editAcc?.smtpUser ?: "") }
        var password by remember { mutableStateOf("") }
        var isActive by remember { mutableStateOf(editAcc?.isActive ?: true) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false; showEditDialog = null },
            title = { Text(if (isEdit) "Edit Account" else "Add Account") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    // Provider simple text for now
                    OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Provider (imap/gmail/outlook/mock)") })
                    
                    Text("IMAP Config", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = imapHost, onValueChange = { imapHost = it }, label = { Text("IMAP Host") })
                    OutlinedTextField(value = imapPort, onValueChange = { imapPort = it }, label = { Text("IMAP Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = imapUser, onValueChange = { imapUser = it }, label = { Text("IMAP User") })

                    Text("SMTP Config", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = smtpHost, onValueChange = { smtpHost = it }, label = { Text("SMTP Host") })
                    OutlinedTextField(value = smtpPort, onValueChange = { smtpPort = it }, label = { Text("SMTP Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = smtpUser, onValueChange = { smtpUser = it }, label = { Text("SMTP User") })

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isEdit) "Password (leave blank to keep)" else "Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Active")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val req = AccountConfigRequest(
                        name = name, email = email, provider = provider,
                        imapHost = imapHost, imapPort = imapPort.toIntOrNull() ?: 993, imapUser = imapUser,
                        smtpHost = smtpHost, smtpPort = smtpPort.toIntOrNull() ?: 587, smtpUser = smtpUser,
                        password = password.ifEmpty { null }, isActive = isActive
                    )
                    if (isEdit) viewModel.updateAccount(editAcc!!.id, req) else viewModel.createAccount(req)
                    showCreateDialog = false; showEditDialog = null
                }, enabled = !state.isSaving) {
                    Text(if (isEdit) "Save" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; showEditDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete this account?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAccount(accountToDelete!!); accountToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
