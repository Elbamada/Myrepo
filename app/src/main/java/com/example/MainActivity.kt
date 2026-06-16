package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AdminViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        AdminApp(viewModel)
                    } else {
                        LoginScreen(viewModel = viewModel, onLoginSuccess = {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminApp(viewModel: AdminViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "deposits"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Admin Console", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text("INVESTMENT PROTOCOL V4.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Deposits") },
                    label = { Text("Deposits") },
                    selected = currentRoute == "deposits",
                    onClick = {
                        navController.navigate("deposits") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Withdrawals") },
                    label = { Text("Withdrawals") },
                    selected = currentRoute == "withdrawals",
                    onClick = {
                        navController.navigate("withdrawals") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Users") },
                    label = { Text("Users") },
                    selected = currentRoute == "users",
                    onClick = {
                        navController.navigate("users") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            val deposits by viewModel.pendingDeposits.collectAsState()
            val withdrawals by viewModel.pendingWithdrawals.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("PENDING DEPOSITS", "${deposits.size}", modifier = Modifier.weight(1f))
                val withdrawalTotal = withdrawals.sumOf { it.amount }
                StatCard("WITHDRAW REQUESTS", "₦${withdrawalTotal}", modifier = Modifier.weight(1f))
            }

            NavHost(
                navController = navController,
                startDestination = "deposits",
                modifier = Modifier.weight(1f) // Ensure NavHost takes remaining space
            ) {
                composable("deposits") { DepositsScreen(viewModel) }
                composable("withdrawals") { WithdrawalsScreen(viewModel) }
                composable("users") { UsersScreen(viewModel) }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp 
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Light
            )
        }
    }
}
