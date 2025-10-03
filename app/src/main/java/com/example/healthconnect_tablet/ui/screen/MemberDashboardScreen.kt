@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthconnect_tablet.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect_tablet.data.model.HealthData
import com.example.healthconnect_tablet.tokens.model.TokenAllowance
import com.example.healthconnect_tablet.tokens.model.TokenCounts
import com.example.healthconnect_tablet.tokens.model.TokenRules
import com.example.healthconnect_tablet.tokens.nfc.TokenNfcReaderActivity
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import com.example.healthconnect_tablet.tokens.ui.TokenDashboardActivity
import com.example.healthconnect_tablet.tokens.ui.components.MoodOptions
import com.example.healthconnect_tablet.tokens.ui.components.MoodTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.PhysicalActivityTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.SleepTokenIcon
import com.example.healthconnect_tablet.tokens.ui.components.TokenPalette
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModel
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModelFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MemberDashboardScreen(
    userId: String,
    userName: String,
    familyName: String,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: MemberDashboardViewModel = viewModel(
        key = "member_dashboard_$userId",
        factory = MemberDashboardViewModelFactory(context, userId, familyName)
    )
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val showMoodDialog = remember { mutableStateOf(false) }
    val resultMessage = rememberSaveable { mutableStateOf<String?>(null) }
    val scannedCountsState = remember { mutableStateOf(TokenCounts()) }

    fun refreshScannedCounts() {
        val counts = TokenCounts(
            activity = PrefsStorage.getTodayAutomatedCount(context, userId, "steps"),
            sleep = PrefsStorage.getTodayAutomatedCount(context, userId, "sleep")
        )
        scannedCountsState.value = counts
    }

    LaunchedEffect(userId) { refreshScannedCounts() }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = result.data?.getStringExtra(TokenNfcReaderActivity.EXTRA_SCAN_RESULT)
            if (!message.isNullOrBlank()) {
                resultMessage.value = message
            }
            refreshScannedCounts()
        }
    }

    LaunchedEffect(userId, familyName) {
        viewModel.updateMember(userId, familyName)
    }

    Scaffold(
        topBar = {
            MemberTopBar(
                userName = userName,
                onBack = onBackPressed,
                onRefresh = {
                    viewModel.refreshData()
                    refreshScannedCounts()
                },
                onOpenTokens = {
                    val intent = Intent(context, TokenDashboardActivity::class.java).apply {
                        putExtra(TokenDashboardActivity.EXTRA_USER_ID, userId)
                        putExtra(TokenDashboardActivity.EXTRA_USER_NAME, userName)
                        putExtra(TokenDashboardActivity.EXTRA_FAMILY_NAME, familyName)
                    }
                    context.startActivity(intent)
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingContent(paddingValues)
            uiState.error != null -> ErrorContent(
                paddingValues = paddingValues,
                message = uiState.error ?: "",
                onRetry = {
                    viewModel.refreshData()
                    refreshScannedCounts()
                }
            )
            else -> {
                val todayAllowance = calculateTodayAllowance(uiState.weeklyData)
                MemberContent(
                    paddingValues = paddingValues,
                    userName = userName,
                    familyName = familyName,
                    allowance = todayAllowance,
                    scannedCounts = scannedCountsState.value,
                    weeklyData = uiState.weeklyData,
                    onScanToken = {
                        val intent = Intent(context, TokenNfcReaderActivity::class.java).apply {
                            putExtra(TokenNfcReaderActivity.EXTRA_USER_ID, userId)
                            putExtra(TokenNfcReaderActivity.EXTRA_FAMILY_NAME, familyName)
                            putExtra(TokenNfcReaderActivity.EXTRA_STEPS_LIMIT, todayAllowance.activityTokens)
                            putExtra(TokenNfcReaderActivity.EXTRA_SLEEP_LIMIT, todayAllowance.sleepTokens)
                        }
                        scannerLauncher.launch(intent)
                    },
                    onAddMood = { showMoodDialog.value = true },
                    onOpenDashboard = {
                        val intent = Intent(context, TokenDashboardActivity::class.java).apply {
                            putExtra(TokenDashboardActivity.EXTRA_USER_ID, userId)
                            putExtra(TokenDashboardActivity.EXTRA_USER_NAME, userName)
                            putExtra(TokenDashboardActivity.EXTRA_FAMILY_NAME, familyName)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }

    if (showMoodDialog.value) {
        MoodDialog(
            onDismiss = { showMoodDialog.value = false },
            onScanMood = {
                showMoodDialog.value = false
                val intent = Intent(context, TokenNfcReaderActivity::class.java).apply {
                    val allowance = calculateTodayAllowance(uiState.weeklyData)
                    putExtra(TokenNfcReaderActivity.EXTRA_USER_ID, userId)
                    putExtra(TokenNfcReaderActivity.EXTRA_FAMILY_NAME, familyName)
                    putExtra(TokenNfcReaderActivity.EXTRA_STEPS_LIMIT, allowance.activityTokens)
                    putExtra(TokenNfcReaderActivity.EXTRA_SLEEP_LIMIT, allowance.sleepTokens)
                }
                scannerLauncher.launch(intent)
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

@Composable
private fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(paddingValues: PaddingValues, message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text(text = "Retry") }
        }
    }
}

@Composable
private fun MemberTopBar(
    userName: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTokens: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(text = "$userName's Activity", style = MaterialTheme.typography.titleLarge)
                Text(text = "Today's token-ready metrics", style = MaterialTheme.typography.bodySmall)
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onOpenTokens) {
                Icon(imageVector = Icons.Default.Token, contentDescription = "Token Dashboard")
            }
        }
    )
}

@Composable
private fun MemberContent(
    paddingValues: PaddingValues,
    userName: String,
    familyName: String,
    allowance: TokenAllowance,
    scannedCounts: TokenCounts,
    weeklyData: List<HealthData>,
    onScanToken: () -> Unit,
    onAddMood: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    val todayEntry = weeklyData.find { it.date == LocalDate.now().toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        MemberSummaryCard(
            userName = userName,
            familyName = familyName,
            allowance = allowance,
            scannedCounts = scannedCounts
        )

        ScanAreaCard(onScan = onScanToken)

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
            Button(
                onClick = onOpenDashboard,
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Token, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Open Token Tools")
            }
        }

        TodayDetailsCard(todayEntry = todayEntry)
    }
}

@Composable
private fun MemberSummaryCard(
    userName: String,
    familyName: String,
    allowance: TokenAllowance,
    scannedCounts: TokenCounts
) {
    val now = LocalDateTime.now()
    val dayLabel = now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val timeLabel = now.format(DateTimeFormatter.ofPattern("h:mm a"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "$dayLabel • $timeLabel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "$userName • $familyName", style = MaterialTheme.typography.bodyMedium)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryRow(
                    label = "Physical Activity",
                    total = allowance.activityTokens,
                    scanned = scannedCounts.activity,
                    icon = {
                        PhysicalActivityTokenIcon(color = TokenPalette.Red, modifier = Modifier.size(36.dp))
                    }
                )
                SummaryRow(
                    label = "Sleep",
                    total = allowance.sleepTokens,
                    scanned = scannedCounts.sleep,
                    icon = {
                        SleepTokenIcon(color = TokenPalette.Blue, modifier = Modifier.size(36.dp))
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    total: Int,
    scanned: Int,
    icon: @Composable () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = "$scanned of $total tokens collected", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ScanAreaCard(onScan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .drawBehind {
                        val strokeWidth = 4.dp.toPx()
                        val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))
                        drawRoundRect(
                            color = MaterialTheme.colorScheme.primary,
                            style = Stroke(width = strokeWidth, pathEffect = dash),
                            cornerRadius = CornerRadius(32.dp.toPx())
                        )
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(imageVector = Icons.Default.Token, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Text(text = "Scan token", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Tap below and hold the token over the tablet's center.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onScan) { Text(text = "Start Scan") }
        }
    }
}

@Composable
private fun TodayDetailsCard(todayEntry: HealthData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Today's snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (todayEntry == null) {
                Text(text = "No synced data yet for today.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "Tokens are calculated from synced steps and sleep. You're ready to build today's board!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MoodDialog(onDismiss: () -> Unit, onScanMood: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "How are you feeling?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Choose a mood color and scan the matching token to add it to the board.",
                    style = MaterialTheme.typography.bodyMedium
                )
                MoodOptions.forEach { option ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        MoodTokenIcon(color = option.color, modifier = Modifier.size(40.dp))
                        Text(text = option.feelings, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onScanMood) { Text(text = "Scan mood token") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

private fun calculateTodayAllowance(weeklyData: List<HealthData>): TokenAllowance {
    val today = LocalDate.now().toString()
    val entry = weeklyData.find { it.date == today }
    val steps = entry?.steps ?: 0
    val sleepMinutes = ((entry?.sleepHours ?: 0f) * 60).toInt()
    return TokenAllowance(
        activityTokens = TokenRules.activityTokensForSteps(steps),
        sleepTokens = TokenRules.sleepTokensForMinutes(sleepMinutes)
    )
}
