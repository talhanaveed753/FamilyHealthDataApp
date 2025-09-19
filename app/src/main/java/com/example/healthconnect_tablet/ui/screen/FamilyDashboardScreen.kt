package com.example.healthconnect_tablet.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthconnect_tablet.data.model.FamilyDashboardData
import com.example.healthconnect_tablet.data.model.FamilyHealthStats
import com.example.healthconnect_tablet.ui.components.ChatbotDialog
import com.example.healthconnect_tablet.ui.components.FamilyMemberCard
import com.example.healthconnect_tablet.ui.viewmodel.FamilyDashboardViewModel
import com.example.healthconnect_tablet.ui.viewmodel.FamilyDashboardViewModelFactory
import androidx.compose.foundation.horizontalScroll

/**
 * Main Family Dashboard Screen
 * Displays health data for all family members in a tablet-optimized layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDashboardScreen(
    viewModel: FamilyDashboardViewModel = viewModel(
        factory = FamilyDashboardViewModelFactory(LocalContext.current)
    ),
    onSwitchFamily: (() -> Unit)? = null,
    onMemberClick: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var sidebarExpanded by remember { mutableStateOf(false) }
    val sidebarWidth: Dp by animateDpAsState(
        targetValue = if (sidebarExpanded) 320.dp else 56.dp,
        animationSpec = tween(durationMillis = 300), label = "sidebarWidth"
    )
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Row(modifier = modifier.fillMaxSize()) {
        // Main Dashboard (left)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Family Health Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.familyData?.family?.name?.let { familyName ->
                            Text(
                                text = familyName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                        IconButton(onClick = { onSwitchFamily?.invoke() }) {
                        Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = "Switch Family"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
            modifier = Modifier.weight(1f)
    ) { paddingValues ->
            Column(
                modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                    .padding(16.dp)
        ) {
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
                uiState.familyData != null -> {
                        val members = uiState.familyData!!.membersHealthData
                        val cardCount = members.size
                        // Banner
                        FamilyStatsCard(
                            familyStats = uiState.familyData!!.familyStats,
                            modifier = Modifier.fillMaxWidth(),
                            memberCount = cardCount
                        )
                        Spacer(modifier = Modifier.height(32.dp)) // Increased spacing to prevent overlap
                        // Responsive card layout: always fit all cards, no vertical scroll
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 250.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
                        ) {
                            items(members) { member ->
                                FamilyMemberCard(
                                    userHealthSummary = member,
                                    onCardClick = {
                                        onMemberClick?.invoke(member.user.id, member.user.name)
                                    },
                                    onColorChange = { newColor ->
                                        viewModel.updateUserColor(member.user.id, newColor)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        // Other dashboard content (graphs, stats, etc.)
                        // ... add more dashboard content here ...
                    }
                }
            }
        }
        // Enhanced Sidebar (right)
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
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyDashboardContent(
    familyData: FamilyDashboardData,
    isLandscape: Boolean,
    onMemberClick: ((String, String) -> Unit)? = null,
    onColorChange: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Family Statistics Summary
        FamilyStatsCard(
            familyStats = familyData.familyStats,
            memberCount = familyData.membersHealthData.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Family Members Section
        Text(
            text = "Family Members",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Adaptive layout based on screen orientation and size
        if (isLandscape) {
            // Grid layout for landscape mode
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(450.dp)
            ) {
                items(familyData.membersHealthData) { memberData ->
                    FamilyMemberCard(
                        userHealthSummary = memberData,
                    onCardClick = {
                        onMemberClick?.invoke(memberData.user.id, memberData.user.name)
                    },
                        onColorChange = { newColor ->
                            onColorChange?.invoke(memberData.user.id, newColor)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            // Horizontal scroll for portrait mode
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(familyData.membersHealthData) { memberData ->
                    FamilyMemberCard(
                        userHealthSummary = memberData,
                        onCardClick = {
                            onMemberClick?.invoke(memberData.user.id, memberData.user.name)
                        },
                        onColorChange = { newColor ->
                            onColorChange?.invoke(memberData.user.id, newColor)
                        },
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Additional insights or recommendations section
        InsightsSection(
            familyStats = familyData.familyStats,
            memberCount = familyData.membersHealthData.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Add bottom padding for FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun FamilyStatsCard(
    familyStats: FamilyHealthStats,
    memberCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 5.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp) // More prominent rounding for main banner
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Family Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "$memberCount members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Total Steps",
                    value = String.format("%,d", familyStats.totalSteps),
                    color = Color(0xFF4CAF50) // Health green for steps
                )
                
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Total Calories",
                    value = String.format("%,d", familyStats.totalCalories),
                    color = Color(0xFFFF7043) // Energy orange for calories
                )
                
                StatItem(
                    icon = Icons.Default.EmojiEvents,
                    label = "Goal Achievers",
                    value = "${familyStats.goalAchievers}/$memberCount",
                    color = Color(0xFFFFB300) // Achievement gold
                )
            }
            
            if (familyStats.mostActiveToday.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Use Surface only around the content, not full width
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Most Active",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Most active today: ${familyStats.mostActiveToday}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsightsSection(
    familyStats: FamilyHealthStats,
    memberCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Insights",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Family Health Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val insights = generateInsights(familyStats, memberCount)
            insights.forEach { insight ->
                Text(
                    text = "• $insight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
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
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading family health data...",
                style = MaterialTheme.typography.bodyLarge
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
                text = "Oops! Something went wrong",
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
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
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

private fun generateInsights(familyStats: FamilyHealthStats, memberCount: Int): List<String> {
    val insights = mutableListOf<String>()
    
    val avgSteps = if (memberCount > 0) familyStats.totalSteps / memberCount else 0
    when {
        avgSteps > 12000 -> insights.add("Your family is very active! Great job maintaining high step counts.")
        avgSteps > 8000 -> insights.add("Good activity levels! Consider setting a family challenge to reach 10,000 steps each.")
        avgSteps > 4000 -> insights.add("Room for improvement! Try family walks after dinner to boost activity.")
        else -> insights.add("Let's get moving! Set small, achievable goals to increase daily activity.")
    }
    
    if (familyStats.goalAchievers > 0) {
        val percentage = (familyStats.goalAchievers * 100) / memberCount
        insights.add("$percentage% of family members reached their step goal today!")
    }
    
    if (familyStats.totalSleepHours > 0) {
        val avgSleep = familyStats.totalSleepHours / memberCount
        when {
            avgSleep >= 8 -> insights.add("Excellent sleep habits! Your family is well-rested.")
            avgSleep >= 7 -> insights.add("Good sleep patterns. Aim for 8+ hours for optimal health.")
            else -> insights.add("Consider establishing better sleep routines for the whole family.")
        }
    }
    
    return insights
} 