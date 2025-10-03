@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthconnect_tablet.tokens.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mood
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.healthconnect_tablet.tokens.model.TokenAllowance
import com.example.healthconnect_tablet.tokens.model.TokenCounts
import com.example.healthconnect_tablet.tokens.nfc.TokenNfcReaderActivity
import com.example.healthconnect_tablet.tokens.ui.components.MoodOption
import com.example.healthconnect_tablet.tokens.ui.components.MoodOptions
import com.example.healthconnect_tablet.tokens.ui.components.MoodTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.PhysicalActivityTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.SleepTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.TokenPalette
import com.example.healthconnect_tablet.ui.theme.HealthConnect_TabletTheme
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            val coroutineScope = rememberCoroutineScope()
            val showMoodDialog = remember { mutableStateOf(false) }
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
                            onBack = { finish() },
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
                        LoadingState(paddingValues)
                    } else {
                        TokenDashboardContent(
                            paddingValues = paddingValues,
                            userName = userName,
                            uiState = uiState,
                            onScanToken = {
                                val intent = Intent(this@TokenDashboardActivity, TokenNfcReaderActivity::class.java).apply {
                                    putExtra(TokenNfcReaderActivity.EXTRA_USER_ID, userId)
                                    putExtra(TokenNfcReaderActivity.EXTRA_FAMILY_NAME, familyName)
                                    putExtra(TokenNfcReaderActivity.EXTRA_STEPS_LIMIT, uiState.allowance.activityTokens)
                                    putExtra(TokenNfcReaderActivity.EXTRA_SLEEP_LIMIT, uiState.allowance.sleepTokens)
                                }
                                launcher.launch(intent)
                            },
                            onAddMood = { showMoodDialog.value = true },
                            onViewHistory = {
                                val intent = Intent(this@TokenDashboardActivity, TokenHistoryActivity::class.java).apply {
                                    putExtra(TokenHistoryActivity.EXTRA_USER_ID, userId)
                                    putExtra(TokenHistoryActivity.EXTRA_USER_NAME, userName)
                                    putExtra(TokenHistoryActivity.EXTRA_FAMILY_NAME, familyName)
                                }
                                startActivity(intent)
                            },
                            onErrorMessage = { error ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(error) }
                            }
                        )
                    }
                }

                if (showMoodDialog.value) {
                    MoodTokenDialog(
                        onDismiss = { showMoodDialog.value = false },
                        onScanMood = {
                            showMoodDialog.value = false
                            val intent = Intent(this@TokenDashboardActivity, TokenNfcReaderActivity::class.java).apply {
                                putExtra(TokenNfcReaderActivity.EXTRA_USER_ID, userId)
                                putExtra(TokenNfcReaderActivity.EXTRA_FAMILY_NAME, familyName)
                                putExtra(TokenNfcReaderActivity.EXTRA_STEPS_LIMIT, uiState.allowance.activityTokens)
                                putExtra(TokenNfcReaderActivity.EXTRA_SLEEP_LIMIT, uiState.allowance.sleepTokens)
                            }
                            launcher.launch(intent)
                        }
                    )
                }

                resultMessage.value?.let { message ->
                    AlertDialog(
                        onDismissRequest = { resultMessage.value = null },
                        title = { Text(text = "Token recorded") },
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

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(text = "Loading token data...")
        }
    }
}

@Composable
private fun TokenDashboardContent(
    paddingValues: PaddingValues,
    userName: String,
    uiState: TokenDashboardUiState,
    onScanToken: () -> Unit,
    onAddMood: () -> Unit,
    onViewHistory: () -> Unit,
    onErrorMessage: (String) -> Unit
) {
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { onErrorMessage(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        uiState.errorMessage?.let { ErrorCard(it) }

        TodayHeader(userName = userName, allowance = uiState.allowance, scanned = uiState.scanned)

        TokenSummaryCard(allowance = uiState.allowance, scanned = uiState.scanned, remaining = uiState.remaining)

        ScanTokenCard(onScan = onScanToken)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddMood,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Mood, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add Mood Token")
            }
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "View History")
            }
        }

        TokenLegendCard()
        MoodPaletteCard()
    }
}

@Composable
private fun TokenDashboardTopBar(
    userName: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onHistoryClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(text = "$userName's Tokens", style = MaterialTheme.typography.titleLarge)
                Text(text = "Translate today's health data into tokens", style = MaterialTheme.typography.bodySmall)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
private fun TodayHeader(userName: String, allowance: TokenAllowance, scanned: TokenCounts) {
    val now = LocalDateTime.now()
    val dayLabel = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val timeLabel = now.format(DateTimeFormatter.ofPattern("h:mm a"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "$dayLabel • $timeLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "$userName is ready to build today's board.",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TokenStatRow(
                    label = "Physical Activity",
                    total = allowance.activityTokens,
                    collected = scanned.activity,
                    icon = {
                        PhysicalActivityTokenIcon(
                            color = TokenPalette.Red,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                )
                TokenStatRow(
                    label = "Sleep",
                    total = allowance.sleepTokens,
                    collected = scanned.sleep,
                    icon = {
                        SleepTokenIcon(
                            color = TokenPalette.Blue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun TokenStatRow(
    label: String,
    total: Int,
    collected: Int,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = "$collected collected", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "of $total",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TokenSummaryCard(allowance: TokenAllowance, scanned: TokenCounts, remaining: TokenCounts) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Today's allowance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryRow(label = "Physical activity tokens", total = allowance.activityTokens, scanned = scanned.activity, remaining = remaining.activity)
            SummaryRow(label = "Sleep tokens", total = allowance.sleepTokens, scanned = scanned.sleep, remaining = remaining.sleep)
            Text(
                text = "Mood tokens can be scanned at any time to capture how everyone feels.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, total: Int, scanned: Int, remaining: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleSmall)
        Text(text = "$scanned scanned • $remaining remaining of $total", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ScanTokenCard(onScan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onScan
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .drawBehind {
                    val strokeWidth = 4.dp.toPx()
                    val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))
                    drawRoundRect(
                        color = MaterialTheme.colorScheme.primary,
                        style = Stroke(width = strokeWidth, pathEffect = dash),
                        cornerRadius = CornerRadius(32.dp.toPx())
                    )
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Default.Token, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Text(text = "Tap to scan a token", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Place the physical token on the scanner area to record it.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TokenLegendCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Token legend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LegendRow(
                title = "Physical Activity",
                description = "Hexagon tokens represent movement goals.",
                content = {
                    PhysicalActivityTokenIcon(color = TokenPalette.Red, modifier = Modifier.size(48.dp))
                }
            )
            LegendRow(
                title = "Sleep",
                description = "Circle tokens record nightly rest.",
                content = {
                    SleepTokenIcon(color = TokenPalette.Blue, modifier = Modifier.size(48.dp))
                }
            )
        }
    }
}

@Composable
private fun LegendRow(title: String, description: String, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) { content() }
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MoodPaletteCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Mood colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoodOptions.forEach { option ->
                    MoodLegendItem(option = option)
                }
            }
        }
    }
}

@Composable
private fun MoodLegendItem(option: MoodOption) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MoodTokenIcon(color = option.color, modifier = Modifier.size(44.dp))
        Text(text = option.feelings, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MoodTokenDialog(onDismiss: () -> Unit, onScanMood: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "How are you feeling today?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select the mood color and scan the matching token to capture it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                MoodOptions.forEach { option ->
                    MoodLegendItem(option = option)
                }
            }
        },
        confirmButton = {
            Button(onClick = onScanMood) {
                Text(text = "Scan mood token")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}
