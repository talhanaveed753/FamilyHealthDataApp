package com.example.healthconnect_tablet.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect_tablet.data.model.HealthData
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModel
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModelFactory
import com.example.healthconnect_tablet.ui.viewmodel.ViewMode
import com.example.healthconnect_tablet.tokens.ui.TokenDashboardActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

    LaunchedEffect(userId, familyName) {
        viewModel.updateMember(userId, familyName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "$userName's Activity", style = MaterialTheme.typography.titleLarge)
                        Text(text = familyName, style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        val intent = Intent(context, TokenDashboardActivity::class.java).apply {
                            putExtra(TokenDashboardActivity.EXTRA_USER_ID, userId)
                            putExtra(TokenDashboardActivity.EXTRA_USER_NAME, userName)
                            putExtra(TokenDashboardActivity.EXTRA_FAMILY_NAME, familyName)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(imageVector = Icons.Default.Token, contentDescription = "Token Dashboard")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingContent(paddingValues)
            uiState.error != null -> ErrorContent(
                paddingValues = paddingValues,
                message = uiState.error ?: "",
                onRetry = { viewModel.refreshData() }
            )
            else -> MemberContent(
                paddingValues = paddingValues,
                userName = userName,
                familyName = familyName,
                viewMode = uiState.viewMode,
                onModeChange = { viewModel.setViewMode(it) },
                weeklyData = uiState.weeklyData
            )
        }
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
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(text = "Retry") }
        }
    }
}

@Composable
private fun MemberContent(
    paddingValues: PaddingValues,
    userName: String,
    familyName: String,
    viewMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    weeklyData: List<HealthData>
) {
    val displayData = when (viewMode) {
        ViewMode.DAY -> {
            val today = LocalDate.now().toString()
            weeklyData.find { it.date == today }?.let { listOf(it) } ?: emptyList()
        }
        ViewMode.WEEK -> weeklyData.sortedByDescending { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SummaryCard(userName = userName, familyName = familyName, weeklyData = weeklyData)

        ModeSelector(currentMode = viewMode, onModeChange = onModeChange)

        if (displayData.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No health data available for the selected period.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayData) { entry ->
                    DailyEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(userName: String, familyName: String, weeklyData: List<HealthData>) {
    val recent = weeklyData.lastOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "$userName • $familyName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (recent != null) {
                Text(text = "Steps today: ${recent.steps}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Sleep hours: ${"%.1f".format(recent.sleepHours)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Average heart rate: ${recent.heartRate} bpm", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "No data synced yet.", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = "Use the token dashboard to translate these metrics into physical tokens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ModeSelector(currentMode: ViewMode, onModeChange: (ViewMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "View", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ViewModeChip(
                label = "Today",
                selected = currentMode == ViewMode.DAY,
                onClick = { onModeChange(ViewMode.DAY) }
            )
            ViewModeChip(
                label = "Last 7 days",
                selected = currentMode == ViewMode.WEEK,
                onClick = { onModeChange(ViewMode.WEEK) }
            )
        }
    }
}

@Composable
private fun ViewModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun DailyEntryCard(entry: HealthData) {
    val dateLabel = try {
        LocalDate.parse(entry.date)
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (_: Exception) {
        entry.date
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Steps: ${entry.steps}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Sleep: ${"%.1f".format(entry.sleepHours)} hours", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Heart rate: ${entry.heartRate} bpm", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
