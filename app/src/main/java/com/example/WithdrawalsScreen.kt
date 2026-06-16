package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment

@Composable
fun WithdrawalsScreen(viewModel: AdminViewModel) {
    val withdrawals by viewModel.pendingWithdrawals.collectAsState()

    if (withdrawals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending withdrawals")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(withdrawals) { withdrawal ->
                WithdrawalCard(
                    withdrawal = withdrawal,
                    onApprove = { viewModel.approveWithdrawal(withdrawal) },
                    onReject = { reason -> 
                        viewModel.rejectWithdrawal(withdrawal, reason)
                    }
                )
            }
        }
    }
}

@Composable
fun WithdrawalCard(
    withdrawal: Withdrawal,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    var quickResponse by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check, // Fallback icon
                        contentDescription = "Withdrawal",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    val dateString = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(withdrawal.timestamp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("User ${withdrawal.userId.take(6)}...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("₦${withdrawal.amount}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text("Ref: ${withdrawal.id.takeLast(8)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }

            // Body
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                
                // Bank Details Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("BANK DETAILS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(withdrawal.bankName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(withdrawal.bankAccountNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "QUICK RESPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                OutlinedTextField(
                    value = quickResponse,
                    onValueChange = { quickResponse = it },
                    placeholder = { Text("Type a custom message to user...") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Actions Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onReject(quickResponse.takeIf { it.isNotBlank() } ?: "Invalid bank account details.") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("REJECT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("APPROVE & FUND", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

