package com.example.healthconnect_tablet.tokens.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import com.example.healthconnect_tablet.ui.theme.HealthConnect_TabletTheme

class TokenHistoryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra(EXTRA_USER_ID) ?: run { finish(); return }
        val userName = intent.getStringExtra(EXTRA_USER_NAME) ?: "Family Member"
        val familyName = intent.getStringExtra(EXTRA_FAMILY_NAME) ?: run { finish(); return }

        val viewModel = ViewModelProvider(
            this,
            TokenHistoryViewModelFactory(applicationContext, userId, familyName)
        )[TokenHistoryViewModel::class.java]

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            LaunchedEffect(uiState.event) {
                when (val event = uiState.event) {
                    is TokenHistoryEvent.Message -> {
                        if (event.message.isNotBlank()) {
                            snackbarHostState.showSnackbar(event.message)
                            viewModel.consumeEvent()
                        }
                    }
                    is TokenHistoryEvent.Error -> {
                        snackbarHostState.showSnackbar(event.message)
                        viewModel.consumeEvent()
                    }
                    TokenHistoryEvent.None -> {}
                }
            }

            HealthConnect_TabletTheme {
                Scaffold(
                    topBar = {
                        HistoryTopBar(
                            userName = userName,
                            onBack = { finish() },
                            onClearAll = { viewModel.clearAll() },
                            onClearUser = { viewModel.clearUserHistory() },
                            onClearToday = { viewModel.clearToday() }
                        )
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    HistoryContent(
                        paddingValues = paddingValues,
                        isLoading = uiState.isLoading,
                        records = uiState.records
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_USER_ID = "history_user_id"
        const val EXTRA_USER_NAME = "history_user_name"
        const val EXTRA_FAMILY_NAME = "history_family_name"
    }
}

@Composable
private fun HistoryTopBar(
    userName: String,
    onBack: () -> Unit,
    onClearAll: () -> Unit,
    onClearUser: () -> Unit,
    onClearToday: () -> Unit
) {
    val expanded = remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Column {
                Text(text = "$userName's Token History", style = MaterialTheme.typography.titleLarge)
                Text(text = "All scans recorded on this device", style = MaterialTheme.typography.bodySmall)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { expanded.value = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Clear This User") },
                    onClick = {
                        expanded.value = false
                        onClearUser()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear Today's Scans") },
                    onClick = {
                        expanded.value = false
                        onClearToday()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Clear ALL") },
                    onClick = {
                        expanded.value = false
                        onClearAll()
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun HistoryContent(
    paddingValues: PaddingValues,
    isLoading: Boolean,
    records: List<PrefsStorage.ScanRecord>
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No scans recorded yet.")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(records) { record ->
                HistoryRow(record)
            }
        }
    }
}

@Composable
private fun HistoryRow(record: PrefsStorage.ScanRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = when (record.type) {
            "automated" -> record.category?.replaceFirstChar { it.uppercase() } ?: "Automated"
            "mood" -> "Mood"
            else -> record.type
        }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = when (record.type) {
                "automated" -> "Amount: ${record.amount}"
                "mood" -> "Mood: ${record.mood}"
                else -> "Amount: ${record.amount}"
            },
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = PrefsStorage.formatTimestamp(record.timestamp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
