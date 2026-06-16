package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(viewModel: AdminViewModel) {
    val users by viewModel.users.collectAsState()
    var selectedUser by remember { mutableStateOf<User?>(null) }
    
    if (selectedUser != null) {
        SendNotificationDialog(
            user = selectedUser!!,
            onDismiss = { selectedUser = null },
            onSend = { title, message, actionType ->
                viewModel.sendNotification(selectedUser!!.id, title, message, actionType)
                selectedUser = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedUser = user },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Balance: ₦${user.balance}", style = MaterialTheme.typography.bodyLarge)
                    Text("Total Invested: ₦${user.totalInvested}", style = MaterialTheme.typography.bodyLarge)
                    Text("Bank: ${user.bankName} - ${user.bankAccountNumber}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SendNotificationDialog(
    user: User,
    onDismiss: () -> Unit,
    onSend: (title: String, message: String, actionType: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var actionType by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notify ${user.fullName}") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = actionType,
                    onValueChange = { actionType = it },
                    label = { Text("Action Type (e.g. RESUBMIT_DEPOSIT)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(title, message, actionType) }) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
