package com.example.healthconnect_tablet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthconnect_tablet.data.model.*
import com.example.healthconnect_tablet.data.repository.FirebaseRepository
import com.example.healthconnect_tablet.data.repository.HealthConnectRepository
import com.example.healthconnect_tablet.data.repository.FamilyManager
import com.example.healthconnect_tablet.data.repository.CardColorManager
import com.example.healthconnect_tablet.data.service.LLMService
import com.example.healthconnect_tablet.data.service.ClaudeService
import com.example.healthconnect_tablet.BuildConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * ViewModel for the Family Dashboard screen
 * Manages family health data aggregation and real-time updates
 */
class FamilyDashboardViewModel(
    context: Context
) : ViewModel() {
    
    private val firebaseRepository = FirebaseRepository()
    private val healthConnectRepository = HealthConnectRepository(context)
    private val familyManager = FamilyManager(context)
    private val cardColorManager = CardColorManager(context)
    
    
    private val llmService: LLMService = ClaudeService(BuildConfig.CLAUDE_API_KEY)
    
    private val _uiState = MutableStateFlow(FamilyDashboardUiState())
    val uiState: StateFlow<FamilyDashboardUiState> = _uiState.asStateFlow()
    
    // Chat state for LLM integration (NEW)
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    init {
        val selectedFamily = familyManager.getSelectedFamily()
        if (selectedFamily != null) {
            loadFamilyData(selectedFamily)
        }
    }
    
    /**
     * Load family data for the selected family
     */
    fun loadFamilyData(familyName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val usersResult = firebaseRepository.getAllUsersFromFamily(familyName)
            if (usersResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = usersResult.exceptionOrNull()?.message ?: "Failed to load family data"
                )
                return@launch
            }
            val users = usersResult.getOrNull() ?: emptyList()
            
            // Auto-assign colors for new users
            cardColorManager.autoAssignColors(users.map { it.id })
            
            val membersHealthData = users.map { user ->
                val healthResult = firebaseRepository.getLatestDailyLogFromFamily(user.id, familyName)
                val todayData = healthResult.getOrNull()
                
                // Apply local card color
                val localColor = cardColorManager.getCardColor(user.id)
                val userWithLocalColor = user.copy(cardColor = localColor)
                
                UserHealthSummary(
                    user = userWithLocalColor,
                    todayData = todayData
                )
            }
            val familyStats = calculateFamilyStats(membersHealthData)
            val family = Family(
                id = familyName,
                name = familyName,
                adminUserId = users.firstOrNull()?.id ?: "",
                memberIds = users.map { it.id }
            )
            val familyData = FamilyDashboardData(
                family = family,
                membersHealthData = membersHealthData,
                familyStats = familyStats
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                familyData = familyData,
                error = null
            )
        }
    }
    
    /**
     * Load all users and their latest health data (treat all users as a family)
     */
    private fun loadAllUsersAndHealthData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val usersResult = firebaseRepository.getAllUsers()
            if (usersResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = usersResult.exceptionOrNull()?.message ?: "Failed to load users"
                )
                return@launch
            }
            val users = usersResult.getOrNull() ?: emptyList()
            
            // Auto-assign colors for new users
            cardColorManager.autoAssignColors(users.map { it.id })
            
            val membersHealthData = users.map { user ->
                val healthResult = firebaseRepository.getLatestDailyLog(user.id)
                val todayData = healthResult.getOrNull()
                
                // Apply local card color
                val localColor = cardColorManager.getCardColor(user.id)
                val userWithLocalColor = user.copy(cardColor = localColor)
                
                UserHealthSummary(
                    user = userWithLocalColor,
                    todayData = todayData
                )
            }
            val familyStats = calculateFamilyStats(membersHealthData)
            val family = Family(
                id = "demo_family",
                name = "All Users",
                adminUserId = users.firstOrNull()?.id ?: "",
                memberIds = users.map { it.id }
            )
            val familyData = FamilyDashboardData(
                family = family,
                membersHealthData = membersHealthData,
                familyStats = familyStats
            )
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                familyData = familyData,
                error = null
            )
        }
    }
    
    /**
     * Load current user and their family data (original Firebase implementation)
     */
    private fun loadCurrentUser() {
        // This is now disabled for UI demo
        // Will be re-enabled when Firebase is connected
    }
    
    /**
     * Load family dashboard data with real-time updates (original Firebase implementation)
     */
    private fun loadFamilyDashboard(familyId: String) {
        // This is now disabled for UI demo
        // Will be re-enabled when Firebase is connected
    }
    
    /**
     * Load health data for a specific family member (original Firebase implementation)
     */
    private fun loadMemberHealthData(user: User): UserHealthSummary {
        // This is now disabled for UI demo
        // Will be re-enabled when Firebase is connected
        return UserHealthSummary(user = user, todayData = null)
    }
    
    /**
     * Calculate family statistics from member health data
     */
    private fun calculateFamilyStats(membersData: List<UserHealthSummary>): FamilyHealthStats {
        if (membersData.isEmpty()) return FamilyHealthStats()
        
        val membersWithData = membersData.mapNotNull { member ->
            member.todayData?.let { data -> member to data }
        }
        
        if (membersWithData.isEmpty()) return FamilyHealthStats()
        
        // Calculate totals and averages
        val totalSteps = membersWithData.sumOf { it.second.steps.toLong() }
        val totalSleepHours = membersWithData.sumOf { it.second.sleepHours.toDouble() }.toFloat()
        val totalCalories = membersWithData.sumOf { it.second.caloriesBurned.toLong() }
        val totalDistance = membersWithData.sumOf { it.second.distance.toDouble() }.toFloat()
        
        val averageHeartRate = if (membersWithData.isNotEmpty()) {
            membersWithData.map { it.second.heartRate }.average().toFloat()
        } else 0f
        
        // Find most active member (by steps)
        val mostActiveToday = membersWithData.maxByOrNull { it.second.steps }?.first?.user?.name ?: ""
        
        // Calculate goal achievers (assuming 10,000 steps as goal)
        val stepGoal = 10000
        val goalAchievers = membersWithData.count { it.second.steps >= stepGoal }
        
        return FamilyHealthStats(
            totalSteps = totalSteps,
            averageHeartRate = averageHeartRate,
            totalSleepHours = totalSleepHours,
            totalCalories = totalCalories,
            totalDistance = totalDistance,
            mostActiveToday = mostActiveToday,
            goalAchievers = goalAchievers
        )
    }
    
    /**
     * Calculate average metrics from health data list (original implementation)
     */
    private fun calculateAverageMetrics(healthDataList: List<HealthData>): HealthMetrics {
        // This is now disabled for UI demo
        // Will be re-enabled when Firebase is connected
        return HealthMetrics()
    }
    
    /**
     * Refresh data for the selected family
     */
    fun refreshData() {
        val selectedFamily = familyManager.getSelectedFamily()
        if (selectedFamily != null) {
            loadFamilyData(selectedFamily)
        }
    }
    
    /**
     * Switch to a different family
     */
    fun switchFamily(familyName: String) {
        familyManager.setSelectedFamily(familyName)
        loadFamilyData(familyName)
    }
    
    /**
     * Reset all card colors to default
     */
    fun resetAllCardColors() {
        cardColorManager.clearAllColors()
        // Refresh the current family data to apply default colors
        val selectedFamily = familyManager.getSelectedFamily()
        if (selectedFamily != null) {
            loadFamilyData(selectedFamily)
        }
    }
    
    /**
     * Get available card colors
     */
    fun getAvailableColors(): List<String> {
        return CardColorManager.AVAILABLE_COLORS
    }
    
    /**
     * Get the currently selected family
     */
    fun getSelectedFamily(): String? {
        return familyManager.getSelectedFamily()
    }
    
    /**
     * Sync health data (placeholder for now)
     */
    fun syncHealthData() {
        // Placeholder - will be implemented when Health Connect is connected
        viewModelScope.launch {
            // Simulate sync
            kotlinx.coroutines.delay(500)
        }
    }
    
    /**
     * Test Firebase connection and data structure (placeholder for now)
     */
    fun testFirebaseConnection() {
        viewModelScope.launch {
            println("Firebase connection test disabled - using mock data for UI demo")
        }
    }
    
    /**
     * Update a user's card color using local persistence
     */
    fun updateUserColor(userId: String, newColor: String) {
        // Save color locally - no need for coroutines as it's synchronous
        cardColorManager.setCardColor(userId, newColor)
        
        // Update the UI state immediately for responsive UI
        val currentFamilyData = _uiState.value.familyData
        if (currentFamilyData != null) {
            val updatedMembersData = currentFamilyData.membersHealthData.map { member ->
                if (member.user.id == userId) {
                    val updatedUser = member.user.copy(cardColor = newColor)
                    member.copy(user = updatedUser)
                } else {
                    member
                }
            }
            
            val updatedFamilyData = currentFamilyData.copy(
                membersHealthData = updatedMembersData
            )
            
            _uiState.value = _uiState.value.copy(familyData = updatedFamilyData)
        }
    }
    
    // ============ LLM CHAT FUNCTIONALITY  ============
    
    /**
     * Send a message to the LLM chatbot
     * Uses existing family data as context for better responses
     */
    fun sendChatMessage(message: String) {
        viewModelScope.launch {
            // Add user message to chat
            val userMessage = ChatMessage(
                id = System.currentTimeMillis().toString(),
                content = message,
                role = ChatMessage.Role.USER,
                timestamp = System.currentTimeMillis()
            )
            _chatMessages.value = _chatMessages.value + userMessage
            
            // Set chat loading state
            _isChatLoading.value = true

            // Generate comprehensive system prompt with all available family health context
            val familyData = _uiState.value.familyData
            val systemPrompt = buildString {
                append("You are a helpful health assistant for a family health monitoring app. ")
                append("Provide insights, recommendations, and answer questions about family health data. ")
                append("Be encouraging, informative, and focus on wellness. Always provide specific, personalized advice. ")
                append("You can see all the data that's displayed on the user's screen and should reference it when answering questions.")
                append("Remember that you are talking to families, whether that's adults or children, so tailor your response to in a warm, family-friendly tone, keep your responses brief and to the point.  ")
                
                familyData?.let { data ->
                    // Family Overview
                    append("\n\n=== FAMILY OVERVIEW ===")
                    append("\nFamily: ${data.family.name}")
                    append("\nTotal Members: ${data.membersHealthData.size}")
                    
                    // Family Statistics (Banner Data)
                    append("\n\n=== TODAY'S FAMILY STATS (displayed in banner) ===")
                    append("\nTotal Steps: ${data.familyStats.totalSteps}")
                    append("\nGoal Achievers: ${data.familyStats.goalAchievers}/${data.membersHealthData.size} members")
                    append("\nAverage Heart Rate: ${data.familyStats.averageHeartRate.toInt()} BPM")
                    append("\nTotal Sleep: ${data.familyStats.totalSleepHours} hours")
                    append("\nTotal Calories Burned: ${data.familyStats.totalCalories}")
                    append("\nTotal Distance: ${String.format("%.2f", data.familyStats.totalDistance / 1000)} km")
                    append("\nMost Active Today: ${data.familyStats.mostActiveToday}")
                    
                    // Individual Member Details
                    append("\n\n=== INDIVIDUAL MEMBER DETAILS ===")
                    data.membersHealthData.forEach { memberSummary ->
                        append("\n\n${memberSummary.user.name}:")
                        append("\n  Age: ${memberSummary.user.age} years")
                        if (memberSummary.user.height > 0) append(", Height: ${memberSummary.user.height}cm")
                        if (memberSummary.user.weight > 0) append(", Weight: ${memberSummary.user.weight}kg")
                        
                        memberSummary.todayData?.let { health ->
                            append("\n  Today's Activity:")
                            append("\n    • Steps: ${health.steps} ${if (health.steps >= 10000) "✅ (Goal achieved!)" else "❌ (${10000 - health.steps} steps to goal)"}")
                            append("\n    • Heart Rate: ${health.heartRate} BPM")
                            append("\n    • Sleep: ${health.sleepHours} hours ${when {
                                health.sleepHours >= 8 -> "✅ (Excellent)"
                                health.sleepHours >= 7 -> "🟡 (Good)"
                                else -> "❌ (Needs improvement)"
                            }}")
                            append("\n    • Calories Burned: ${health.caloriesBurned}")
                            append("\n    • Distance: ${String.format("%.2f", health.distance / 1000)} km")
                        } ?: append("\n  No health data available for today")
                    }
                    
                    // Health Insights & Comparisons
                    append("\n\n=== FAMILY HEALTH INSIGHTS ===")
                    val membersWithData = data.membersHealthData.mapNotNull { it.todayData?.let { health -> it.user.name to health } }
                    
                    if (membersWithData.isNotEmpty()) {
                        // Most/least active
                        val mostSteps = membersWithData.maxByOrNull { it.second.steps }
                        val leastSteps = membersWithData.minByOrNull { it.second.steps }
                        append("\nMost Active: ${mostSteps?.first} (${mostSteps?.second?.steps} steps)")
                        append("\nLeast Active: ${leastSteps?.first} (${leastSteps?.second?.steps} steps)")
                        
                        // Sleep analysis
                        val bestSleep = membersWithData.maxByOrNull { it.second.sleepHours }
                        val worstSleep = membersWithData.minByOrNull { it.second.sleepHours }
                        append("\nBest Sleep: ${bestSleep?.first} (${bestSleep?.second?.sleepHours} hours)")
                        append("\nNeeds More Sleep: ${worstSleep?.first} (${worstSleep?.second?.sleepHours} hours)")
                        
                        // Goal achievement analysis
                        val achievers = membersWithData.filter { it.second.steps >= 10000 }
                        val nonAchievers = membersWithData.filter { it.second.steps < 10000 }
                        
                        if (achievers.isNotEmpty()) {
                            append("\nGoal Achievers Today: ${achievers.joinToString(", ") { it.first }}")
                        }
                        if (nonAchievers.isNotEmpty()) {
                            append("\nNeed Encouragement: ${nonAchievers.joinToString(", ") { "${it.first} (${10000 - it.second.steps} steps needed)" }}")
                        }
                    }
                    
                    // Current Screen Context
                    append("\n\n=== CURRENT SCREEN CONTEXT ===")
                    append("\nUser is viewing: Family Dashboard for ${data.family.name}")
                    append("\nDisplayed data: Today's health metrics and family statistics")
                    append("\nBanner shows: Aggregated family totals and averages")
                    append("\nIndividual cards show: Each family member's personal health data")
                    append("\nChat interface: Available for health questions and recommendations")
                }
                
                append("\n\nIMPORTANT: Reference specific data points and member names when answering questions. ")
                append("Provide actionable, personalized health advice based on the actual data shown. ")
                append("If asked about what's displayed on screen, describe the specific numbers and member details visible to the user.")
            }

            // Send to LLM service
            llmService.sendMessage(
                messages = _chatMessages.value,
                systemPrompt = systemPrompt
            ).onSuccess { response ->
                _chatMessages.value = _chatMessages.value + response
                _isChatLoading.value = false
            }.onFailure { error ->
                // Add error message to chat
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    content = "Sorry, I encountered an error: ${error.message}. Please try again.",
                    role = ChatMessage.Role.ASSISTANT,
                    timestamp = System.currentTimeMillis()
                )
                _chatMessages.value = _chatMessages.value + errorMessage
                _isChatLoading.value = false
            }
        }
    }
    
    /**
     * Clear chat history
     */
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }
    
    /**
     * Get LLM provider info for UI display
     */
    fun getLLMProviderInfo(): String {
        return "${llmService.getProviderName()} - ${llmService.getModelName()}"
    }
}

/**
 * UI State for Family Dashboard
 */
data class FamilyDashboardUiState(
    val isLoading: Boolean = false,
    val familyData: FamilyDashboardData? = null,
    val error: String? = null
)

/**
 * Factory for creating FamilyDashboardViewModel
 */
class FamilyDashboardViewModelFactory(
    private val context: Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyDashboardViewModel::class.java)) {
            return FamilyDashboardViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 