package com.example.healthconnect_tablet.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect_tablet.data.model.HealthData
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModel
import com.example.healthconnect_tablet.ui.viewmodel.MemberDashboardViewModelFactory
import com.example.healthconnect_tablet.ui.viewmodel.ViewMode
import com.example.healthconnect_tablet.ui.components.ChatbotDialog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Member Dashboard Screen
 * Shows individual member health data with day/week toggle and chat functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        key = "member_dashboard_$userId", // Unique key per member
        factory = MemberDashboardViewModelFactory(context, userId, familyName)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    
    // Sidebar state
    var sidebarExpanded by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (sidebarExpanded) 400.dp else 64.dp,
        animationSpec = tween(300), label = "sidebar width"
    )

    LaunchedEffect(userId, familyName) {
        // Ensure ViewModel is updated when member changes
        viewModel.updateMember(userId, familyName)
        viewModel.loadWeeklyData()
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Main content (left)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // Top bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$userName's Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (uiState.viewMode) {
                                ViewMode.DAY -> "Today's Activity"
                                ViewMode.WEEK -> "Last 7 Days"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Content based on UI state
            when {
                uiState.isLoading -> {
                    LoadingScreen()
                }
                uiState.error != null -> {
                    ErrorScreen(
                        error = uiState.error!!,
                        onRetry = { viewModel.refreshData() }
                    )
                }
                uiState.weeklyData.isNotEmpty() && uiState.weeklyData.any { 
                    it.steps > 0 || it.heartRate > 0 || it.sleepHours > 0 
                } -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Profile Card
                        ProfileCard(
                            userName = userName,
                            familyName = familyName
                        )
                        
                        // Day/Week Toggle
                        ViewModeToggle(
                            currentMode = uiState.viewMode,
                            onModeChange = { viewModel.setViewMode(it) }
                        )
                        
                        // Filter data based on view mode
                        val displayData = when (uiState.viewMode) {
                            ViewMode.DAY -> {
                                // For today mode, find today's data specifically (not just most recent)
                                val today = java.time.LocalDate.now().toString()
                                val todayData = uiState.weeklyData.find { it.date == today }
                                if (todayData != null) listOf(todayData) else emptyList()
                            }
                            ViewMode.WEEK -> uiState.weeklyData
                        }
                        
                        // Check if we have any meaningful data
                        val hasAnyData = displayData.any { 
                            it.steps > 0 || it.heartRate > 0 || it.sleepHours > 0 
                        }
                        
                        if (hasAnyData) {
                            // Health Metrics Cards
                            StepsCard(
                                data = displayData,
                                viewMode = uiState.viewMode
                            )
                            
                            HeartRateCard(
                                data = displayData,
                                viewMode = uiState.viewMode
                            )
                            
                            SleepCard(
                                data = displayData,
                                viewMode = uiState.viewMode
                            )
                        } else {
                            // Show no data message if all values are 0
                            NoDataForPeriod(
                                userName = userName,
                                period = if (uiState.viewMode == ViewMode.DAY) "today" else "this week"
                            )
                        }
                    }
                }
                else -> {
                    NoDataScreen(userName = userName)
                }
            }
        }
        
        // Enhanced Sidebar (right) - EXACT COPY FROM FAMILY DASHBOARD
        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = if (sidebarExpanded) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxHeight()
                .width(sidebarWidth)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (sidebarExpanded) {
                    // Expanded sidebar with enhanced chat content
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Chat content
                        ChatbotDialog(
                            messages = chatMessages,
                            onSendMessage = { viewModel.sendChatMessage(it) },
                            onClearHistory = { viewModel.clearChatHistory() },
                            providerInfo = viewModel.getLLMProviderInfo(),
                            isTyping = isChatLoading, // Show typing indicator when chat is loading
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    
                    // Enhanced toggle button positioned away from status bar
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 80.dp, end = 12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        shadowElevation = 4.dp,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = { sidebarExpanded = !sidebarExpanded },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Collapse Chatbot",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Enhanced collapsed sidebar
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Enhanced toggle button with chat icon
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            tonalElevation = 3.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = { sidebarExpanded = !sidebarExpanded },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = "🦊",
                                    fontSize = 20.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Ally text indicator
                        Text(
                            text = "Ally",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    familyName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Member of $familyName Family",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onModeChange(ViewMode.DAY) },
                    label = { Text("Today") },
                    selected = currentMode == ViewMode.DAY,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                
                FilterChip(
                    onClick = { onModeChange(ViewMode.WEEK) },
                    label = { Text("7 Days") },
                    selected = currentMode == ViewMode.WEEK,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun StepsCard(
    data: List<HealthData>,
    viewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    val stepsData = data.map { it.steps.toFloat() }
    val totalSteps = data.sumOf { it.steps }
    val avgSteps = if (data.isNotEmpty()) totalSteps / data.size else 0
    val goalAchieved = data.count { it.steps >= 10000 }
    
    HealthMetricCard(
        title = if (viewMode == ViewMode.DAY) "Today's Steps" else "Steps This Week",
        icon = Icons.Default.DirectionsWalk,
        color = Color(0xFF4CAF50),
        value = if (viewMode == ViewMode.DAY) totalSteps.toString() else "$totalSteps total",
        unit = "steps",
        modifier = modifier
    ) {
        if (totalSteps > 0) {
            if (viewMode == ViewMode.DAY) {
                // For day view, show today's step data as a simple progress indicator
                DailyStepsDisplay(
                    todaySteps = totalSteps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            } else {
                // For week view, show actual weekly chart from Firebase data with dates
                // Reverse order so most recent is on left
                val reversedData = stepsData.reversed()
                val reversedDateLabels = data.map { formatDateForChart(it.date) }.reversed()
                SimpleChart(
                    data = reversedData,
                    color = Color(0xFF4CAF50),
                    chartType = ChartType.BAR,
                    dateLabels = reversedDateLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Increased height for date labels
                )
            }
        } else {
            NoDataIndicator("No steps data available for this period")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (viewMode == ViewMode.DAY) {
                MetricSummary("Current", String.format("%,d", totalSteps))
                MetricSummary("Goal Progress", "${(totalSteps * 100 / 10000).coerceAtMost(100)}%")
                MetricSummary("Target", "10,000")
            } else {
                MetricSummary("Average", String.format("%,d", avgSteps))
                MetricSummary("Goals Met", "$goalAchieved/${data.size}")
                MetricSummary("Target", "10,000")
            }
        }
    }
}

@Composable
private fun HeartRateCard(
    data: List<HealthData>,
    viewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    val heartRateData = data.map { it.heartRate.toFloat() }
    val validData = data.filter { it.heartRate > 0 }
    val avgHeartRate = if (validData.isNotEmpty()) validData.sumOf { it.heartRate } / validData.size else 0
    val maxHeartRate = validData.maxOfOrNull { it.heartRate } ?: 0
    val currentHeartRate = data.lastOrNull()?.heartRate ?: 0
    
    HealthMetricCard(
        title = if (viewMode == ViewMode.DAY) "Today's Heart Rate" else "Heart Rate Trend",
        icon = Icons.Default.Favorite,
        color = Color(0xFFE91E63),
        value = if (viewMode == ViewMode.DAY) currentHeartRate.toString() else avgHeartRate.toString(),
        unit = "BPM",
        modifier = modifier
    ) {
        if (validData.isNotEmpty()) {
            if (viewMode == ViewMode.DAY && currentHeartRate > 0) {
                // For day view, show current heart rate with simple visualization
                DailyHeartRateDisplay(
                    currentHeartRate = currentHeartRate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            } else if (viewMode == ViewMode.WEEK) {
                // For week view, show heart rate trend with dates
                // Reverse order so most recent is on left
                val reversedHeartRateData = heartRateData.reversed()
                val reversedDateLabels = data.map { formatDateForChart(it.date) }.reversed()
                SimpleChart(
                    data = reversedHeartRateData,
                    color = Color(0xFFE91E63),
                    chartType = ChartType.LINE,
                    dateLabels = reversedDateLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Increased height for date labels
                )
            } else {
                NoDataIndicator("No heart rate data available for today")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricSummary("Average", "$avgHeartRate BPM")
                MetricSummary("Max", "$maxHeartRate BPM")
                MetricSummary("Current", "$currentHeartRate BPM")
            }
        } else {
            NoDataIndicator("No heart rate data available")
        }
    }
}

@Composable
private fun SleepCard(
    data: List<HealthData>,
    viewMode: ViewMode,
    modifier: Modifier = Modifier
) {
    val sleepData = data.map { it.sleepHours }
    val validData = data.filter { it.sleepHours > 0 }
    val avgSleep = if (validData.isNotEmpty()) validData.sumOf { it.sleepHours.toDouble() } / validData.size else 0.0
    val totalSleep = validData.sumOf { it.sleepHours.toDouble() }
    val goodSleepNights = validData.count { it.sleepHours >= 7 }
    val currentSleep = data.lastOrNull()?.sleepHours ?: 0f
    
    HealthMetricCard(
        title = if (viewMode == ViewMode.DAY) "Last Night's Sleep" else "Sleep This Week",
        icon = Icons.Default.Bedtime,
        color = Color(0xFF9C27B0),
        value = if (viewMode == ViewMode.DAY) String.format("%.1f", currentSleep) else String.format("%.1f", avgSleep),
        unit = "hours",
        modifier = modifier
    ) {
        if (validData.isNotEmpty()) {
            if (viewMode == ViewMode.DAY && currentSleep > 0) {
                // For day view, show sleep quality indicator
                DailySleepDisplay(
                    sleepHours = currentSleep,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            } else if (viewMode == ViewMode.WEEK) {
                // For week view, show actual weekly chart from Firebase data with dates
                // Reverse order so most recent is on left
                val reversedSleepData = sleepData.reversed()
                val reversedDateLabels = data.map { formatDateForChart(it.date) }.reversed()
                SimpleChart(
                    data = reversedSleepData,
                    color = Color(0xFF9C27B0),
                    chartType = ChartType.BAR,
                    dateLabels = reversedDateLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp) // Increased height for date labels
                )
            } else {
                NoDataIndicator("No sleep data available for today")  
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (viewMode == ViewMode.DAY) {
                    MetricSummary("Total Sleep", String.format("%.1fh", currentSleep))
                    MetricSummary("Quality", if (currentSleep >= 7) "Good" else "Poor")
                    MetricSummary("Target", "7-8h")
                } else {
                    MetricSummary("Average", String.format("%.1fh", avgSleep))
                    MetricSummary("Good Sleep", "$goodSleepNights nights")
                    MetricSummary("Target", "7-8h")
                }
            }
        } else {
            NoDataIndicator("No sleep data available")
        }
    }
}

@Composable
private fun HealthMetricCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

enum class ChartType { LINE, BAR }

/**
 * Helper function to format dates for chart labels (e.g., "12/25" or "Dec 25")
 */
private fun formatDateForChart(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        "${date.monthValue}/${date.dayOfMonth}"
    } catch (e: Exception) {
        dateString.takeLast(5) // Fallback to last 5 chars
    }
}

@Composable
private fun SimpleChart(
    data: List<Float>,
    color: Color,
    chartType: ChartType,
    modifier: Modifier = Modifier,
    dateLabels: List<String> = emptyList()
) {
    Column(modifier = modifier) {
        // Chart area
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
        if (data.isEmpty()) return@Canvas
        
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOfOrNull { it } ?: 0f  
        val range = maxValue - minValue
        
        // Handle all zero data case
        if (maxValue == 0f) {
            // Draw empty state with dashed baseline
            val dashLength = 10.dp.toPx()
            val gapLength = 5.dp.toPx()
            var currentX = 0f
            val baselineY = size.height * 0.9f
            
            while (currentX < size.width) {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(currentX, baselineY),
                    end = Offset((currentX + dashLength).coerceAtMost(size.width), baselineY),
                    strokeWidth = 2.dp.toPx()
                )
                currentX += dashLength + gapLength
            }
            return@Canvas
        }
        
        if (range == 0f) {
            // All values are the same (but not zero)
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(0f, size.height * 0.5f),
                end = Offset(size.width, size.height * 0.5f),
                strokeWidth = 4.dp.toPx()
            )
            return@Canvas
        }
        
        when (chartType) {
            ChartType.BAR -> {
                val barWidth = size.width / data.size
                val padding = barWidth * 0.1f
                val actualBarWidth = barWidth - padding
                
                data.forEachIndexed { index, value ->
                    val barHeight = ((value - minValue) / range) * size.height * 0.8f
                    val left = index * barWidth + padding / 2
                    val top = size.height - barHeight
                    
                    // Draw the bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(actualBarWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                    
                    // Draw value label on top of bar if value > 0
                    if (value > 0) {
                        drawIntoCanvas { canvas ->
                            val frameworkPaint = android.graphics.Paint().apply {
                                this.color = color.toArgb()
                                textSize = 12.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                            }
                            
                            val displayValue = if (value >= 1000) {
                                String.format("%.1fk", value / 1000)
                            } else if (value % 1 == 0f) {
                                value.toInt().toString()
                            } else {
                                String.format("%.1f", value)
                            }
                            
                            val centerX = left + actualBarWidth / 2
                            val labelY = (top - 8.dp.toPx()).coerceAtLeast(16.dp.toPx())
                            
                            canvas.nativeCanvas.drawText(
                                displayValue,
                                centerX,
                                labelY,
                                frameworkPaint
                            )
                        }
                    }
                }
            }
            ChartType.LINE -> {
                if (data.size < 2) {
                    drawCircle(
                        color = color,
                        radius = 6.dp.toPx(),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                    return@Canvas
                }
                
                val stepX = size.width / (data.size - 1)
                val path = Path()
                
                data.forEachIndexed { index, value ->
                    val x = index * stepX
                    val y = size.height - ((value - minValue) / range) * size.height * 0.8f
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    // Draw points
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                
                // Draw line
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        }
        
        // Date labels for weekly charts
        if (dateLabels.isNotEmpty() && dateLabels.size == data.size) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dateLabels.forEach { dateLabel ->
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricSummary(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoDataIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "No data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading health data...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to load data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun DailyStepsDisplay(
    todaySteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Today's Step Goal Progress",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Progress bar showing goal completion
        val goalSteps = 10000
        val progress = (todaySteps.toFloat() / goalSteps).coerceAtMost(1f)
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Color(0xFF4CAF50),
                            RoundedCornerShape(10.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%,d", todaySteps)} steps",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "Goal: ${String.format("%,d", goalSteps)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Goal achievement status
            val remainingSteps = goalSteps - todaySteps
            if (remainingSteps > 0) {
                Text(
                    text = "${String.format("%,d", remainingSteps)} steps to reach goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "🎉 Goal achieved! +${String.format("%,d", -remainingSteps)} extra steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DailySleepDisplay(
    sleepHours: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Sleep Quality Assessment",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Sleep quality visualization
        val recommendedSleep = 8f
        val sleepQuality = when {
            sleepHours >= 8f -> SleepQuality.EXCELLENT
            sleepHours >= 7f -> SleepQuality.GOOD
            sleepHours >= 6f -> SleepQuality.FAIR
            else -> SleepQuality.POOR
        }
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sleep duration bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sleep bars representing hours
                repeat(10) { hour ->
                    val isSlept = hour < sleepHours.toInt()
                    val isPartial = hour == sleepHours.toInt() && sleepHours % 1 > 0
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .padding(horizontal = 1.dp)
                            .background(
                                color = when {
                                    isSlept -> sleepQuality.color
                                    isPartial -> sleepQuality.color.copy(alpha = (sleepHours % 1))
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sleep info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%.1f", sleepHours)} hours",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = sleepQuality.color
                )
                Text(
                    text = "Recommended: 7-9h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Sleep quality status
            Text(
                text = sleepQuality.message,
                style = MaterialTheme.typography.bodySmall,
                color = sleepQuality.color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DailyHeartRateDisplay(
    currentHeartRate: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Current Heart Rate Status",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Heart rate zone visualization
        val heartRateZone = when {
            currentHeartRate >= 150 -> HeartRateZone.HIGH
            currentHeartRate >= 120 -> HeartRateZone.ELEVATED
            currentHeartRate >= 90 -> HeartRateZone.NORMAL
            currentHeartRate >= 60 -> HeartRateZone.RESTING
            else -> HeartRateZone.LOW
        }
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Heart rate visualization with pulse effect
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart rate zones visualization
                val zones = listOf(
                    Triple("Rest", 60f, Color(0xFF4CAF50)),
                    Triple("Normal", 90f, Color(0xFF8BC34A)), 
                    Triple("Elevated", 120f, Color(0xFFFFC107)),
                    Triple("High", 150f, Color(0xFFFF5722))
                )
                
                zones.forEach { (label, threshold, color) ->
                    val isActive = currentHeartRate >= threshold
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(16.dp)
                            .padding(horizontal = 1.dp)
                            .background(
                                color = if (isActive) color else color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Current heart rate info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentHeartRate BPM",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = heartRateZone.color
                )
                Text(
                    text = heartRateZone.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Heart rate status message
            Text(
                text = heartRateZone.message,
                style = MaterialTheme.typography.bodySmall,
                color = heartRateZone.color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

enum class HeartRateZone(val color: Color, val message: String, val zoneName: String) {
    LOW(Color(0xFF9E9E9E), "⚠️ Heart rate seems low. Consider consulting a doctor.", "Low"),
    RESTING(Color(0xFF4CAF50), "😌 Resting heart rate. You're relaxed.", "Resting"),
    NORMAL(Color(0xFF8BC34A), "👍 Normal heart rate. All good!", "Normal"),
    ELEVATED(Color(0xFFFFC107), "🏃‍♂️ Elevated heart rate. Active or stressed?", "Elevated"),
    HIGH(Color(0xFFFF5722), "💪 High heart rate. Intense activity or check if okay.", "High")
}

enum class SleepQuality(val color: Color, val message: String) {
    EXCELLENT(Color(0xFF4CAF50), "😴 Excellent sleep! You're well rested."),
    GOOD(Color(0xFF8BC34A), "😊 Good sleep duration. Keep it up!"),
    FAIR(Color(0xFFFFC107), "😐 Adequate sleep, but aim for 7-8 hours."),
    POOR(Color(0xFFF44336), "😵 Too little sleep. Prioritize rest tonight.")
}

@Composable
private fun NoDataScreen(userName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No health data available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No health data found for $userName in the last 7 days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoDataForPeriod(
    userName: String,
    period: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingDown,
                contentDescription = "No data",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Activity Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$userName has no recorded health data for $period.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try checking back later or ensure health tracking is enabled.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

 