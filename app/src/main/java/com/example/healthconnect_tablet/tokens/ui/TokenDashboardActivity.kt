package com.example.healthconnect_tablet.tokens.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.healthconnect_tablet.tokens.model.TokenAllowance
import com.example.healthconnect_tablet.tokens.model.TokenCounts
import com.example.healthconnect_tablet.tokens.nfc.TokenNfcReaderActivity
import com.example.healthconnect_tablet.ui.theme.HealthConnect_TabletTheme
import kotlinx.coroutines.launch

class TokenDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: run {
            finish(); return
        }
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Family Member"
        val familyName = intent.getStringExtra(EXTRA_FAMILY_NAME) ?: run {
            finish(); return
        }

        val viewModel = ViewModelProvider(
            this,
            TokenDashboardViewModelFactory(applicationContext, userId, familyName)
        )[TokenDashboardViewModel::class.java]

        @OptIn(ExperimentalMaterial3Api::class)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val resultMessage = rememberSaveable { mutableStateOf<String?>(null) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val message = result.data?.getStringExtra(TokenNfcReaderActivity.EXTRA_SCAN_RESULT)
                    if (!message.isNullOrBlank()) {
                        resultMessage.value = message
                    }
                    viewModel.refreshScannedCounts()
                }
            }

            HealthConnect_TabletTheme {
                Scaffold(
                    topBar = {
                        TokenDashboardTopBar(
                            userName = userName,
                            onRefresh = { viewModel.refreshAllowances() },
                            onHistoryClick = {
                                val intent = Intent(this@TokenDashboardActivity, TokenHistoryActivity::class.java).apply {
                                    putExtra(TokenHistoryActivity.EXTRA_USER_ID, userId)
                                    putExtra(TokenHistoryActivity.EXTRA_USER_NAME, userName)
                                    putExtra(TokenHistoryActivity.EXTRA_FAMILY_NAME, familyName)
                                }
                                startActivity(intent)
                            }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    if (uiState.isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Loading token data...")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            uiState.errorMessage?.let { error ->
                                ErrorCard(message = error) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(error)
                                    }
                                }
                            }

                            TokenSummaryCard(
                                allowance = uiState.allowance,
                                scanned = uiState.scanned,
                                remaining = uiState.remaining
                            )

                            Button(
                                onClick = {
                                    val intent = Intent(this@TokenDashboardActivity, TokenNfcReaderActivity::class.java).apply {
                                        putExtra(TokenNfcReaderActivity.EXTRA_USER_ID, userId)
                                        putExtra(TokenNfcReaderActivity.EXTRA_FAMILY_NAME, familyName)
                                        putExtra(TokenNfcReaderActivity.EXTRA_STEPS_LIMIT, uiState.allowance.stepsTokens)
                                        putExtra(TokenNfcReaderActivity.EXTRA_SLEEP_LIMIT, uiState.allowance.sleepTokens)
                                        putExtra(TokenNfcReaderActivity.EXTRA_HEART_LIMIT, uiState.allowance.heartTokens)
                                    }
                                    launcher.launch(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Token, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Scan NFC Token")
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(this@TokenDashboardActivity, TokenHistoryActivity::class.java).apply {
                                        putExtra(TokenHistoryActivity.EXTRA_USER_ID, userId)
                                        putExtra(TokenHistoryActivity.EXTRA_USER_NAME, userName)
                                        putExtra(TokenHistoryActivity.EXTRA_FAMILY_NAME, familyName)
                                    }
                                    startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.History, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "View History")
                            }
                        }
                    }
                }

                resultMessage.value?.let { message ->
                    AlertDialog(
                        onDismissRequest = { resultMessage.value = null },
                        title = { Text(text = "Scan Result") },
                        text = { Text(text = message) },
                        confirmButton = {
                            Button(onClick = { resultMessage.value = null }) {
                                Text(text = "OK")
                            }
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "token_user_id"
        const val EXTRA_USER_NAME = "token_user_name"
        const val EXTRA_FAMILY_NAME = "token_family_name"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenDashboardTopBar(
    userName: String,
    onRefresh: () -> Unit,
    onHistoryClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(text = "$userName's Tokens", style = MaterialTheme.typography.titleLarge)
                Text(text = "Manage today's allowances", style = MaterialTheme.typography.bodySmall)
            }
        },
        actions = {
            IconButton(onClick = onHistoryClick) {
                Icon(imageVector = Icons.Default.History, contentDescription = "History")
            }
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    )
}

@Composable
private fun TokenSummaryCard(
    allowance: TokenAllowance,
    scanned: TokenCounts,
    remaining: TokenCounts
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Today's Allowances", style = MaterialTheme.typography.titleMedium)
            TokenRow(label = "Steps", total = allowance.stepsTokens, scanned = scanned.steps, remaining = remaining.steps)
            TokenRow(label = "Sleep", total = allowance.sleepTokens, scanned = scanned.sleep, remaining = remaining.sleep)
            TokenRow(label = "Heart", total = allowance.heartTokens, scanned = scanned.heart, remaining = remaining.heart)
            Text(
                text = "Mood tokens can be scanned without limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TokenRow(label: String, total: Int, scanned: Int, remaining: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = "Scanned: $scanned", style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "Total: $total", style = MaterialTheme.typography.bodySmall)
            Text(text = "Remaining: $remaining", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onShown: () -> Unit) {
    LaunchedEffect(message) {
        onShown()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}
