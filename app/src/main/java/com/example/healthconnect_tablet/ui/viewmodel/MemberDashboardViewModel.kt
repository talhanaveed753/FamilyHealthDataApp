package com.example.healthconnect_tablet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthconnect_tablet.data.model.HealthData
import com.example.healthconnect_tablet.data.model.ChatMessage
import com.example.healthconnect_tablet.data.model.User
import com.example.healthconnect_tablet.data.repository.FirebaseRepository
import com.example.healthconnect_tablet.data.service.LLMService
import com.example.healthconnect_tablet.data.service.ClaudeService
import com.example.healthconnect_tablet.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * View mode for displaying health data
 */
enum class ViewMode {
    DAY,    // Show single day data
    WEEK    // Show 7-day trend data
}

/**
 * ViewModel for Member Dashboard Screen
 * Manages health data for a specific family member over the last 7 days
 * with day/week view modes and individual chat integration
 */
class MemberDashboardViewModel(
    private var userId: String,
    private var familyName: String
) : ViewModel() {
    
    private val firebaseRepository = FirebaseRepository()
    private val llmService: LLMService = ClaudeService(BuildConfig.CLAUDE_API_KEY)
    
    private val _uiState = MutableStateFlow(MemberDashboardUiState())
    val uiState: StateFlow<MemberDashboardUiState> = _uiState.asStateFlow()
    
    // Chat state for individual member LLM integration - isolated per ViewModel instance
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()
    
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()
    
    // Static companion object to store chat history across ViewModel instances
    companion object {
        private val memberChatHistory = mutableMapOf<String, List<ChatMessage>>()
    }
    
    init {
        android.util.Log.d("MemberDashboardViewModel", "Created new instance for userId: $userId, familyName: $familyName")
        loadUserProfileAndWeeklyData()
        loadMemberChatHistory()
    }
    
    /**
     * Load chat history for the current member from static storage
     */
    private fun loadMemberChatHistory() {
        val currentMessages = memberChatHistory[userId] ?: emptyList()
        _chatMessages.value = currentMessages
        android.util.Log.d("MemberDashboardViewModel", "Loaded chat history for userId: $userId, messages count: ${currentMessages.size}")
    }
    
    /**
     * Load user profile and 7-day health data for the member
     */
    fun loadUserProfileAndWeeklyData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            android.util.Log.d("MemberDashboardViewModel", "Loading profile and data for user: $userId, family: $familyName")
            
            // Load user profile first
            val profileResult = firebaseRepository.getUserProfileFromFamily(userId, familyName)
            if (profileResult.isFailure) {
                val errorMessage = profileResult.exceptionOrNull()?.message ?: "Failed to load user profile"
                android.util.Log.e("MemberDashboardViewModel", "Error loading profile: $errorMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                return@launch
            }
            
            val userProfile = profileResult.getOrNull()
            android.util.Log.d("MemberDashboardViewModel", "Loaded user profile: ${userProfile?.name}, age: ${userProfile?.age}, gender: ${userProfile?.gender}")
            
            // Load weekly health data
            val healthResult = firebaseRepository.getLast7DaysFromFamily(userId, familyName)
            
            if (healthResult.isSuccess) {
                val weeklyData = healthResult.getOrNull() ?: emptyList()
                
                android.util.Log.d("MemberDashboardViewModel", "Loaded ${weeklyData.size} days of data")
                weeklyData.forEachIndexed { index, data ->
                    android.util.Log.d("MemberDashboardViewModel", "Day ${index + 1}: steps=${data.steps}, heartRate=${data.heartRate}, sleep=${data.sleepHours}")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = userProfile,
                    weeklyData = weeklyData,
                    error = null
                )
            } else {
                val errorMessage = healthResult.exceptionOrNull()?.message ?: "Failed to load health data"
                android.util.Log.e("MemberDashboardViewModel", "Error loading health data: $errorMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userProfile = userProfile, // Still set profile even if health data fails
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Load 7-day health data for the member (legacy method)
     */
    fun loadWeeklyData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            android.util.Log.d("MemberDashboardViewModel", "Loading data for user: $userId, family: $familyName")
            
            val result = firebaseRepository.getLast7DaysFromFamily(userId, familyName)
            
            if (result.isSuccess) {
                val weeklyData = result.getOrNull() ?: emptyList()
                
                android.util.Log.d("MemberDashboardViewModel", "Loaded ${weeklyData.size} days of data")
                weeklyData.forEachIndexed { index, data ->
                    android.util.Log.d("MemberDashboardViewModel", "Day ${index + 1}: steps=${data.steps}, heartRate=${data.heartRate}, sleep=${data.sleepHours}")
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    weeklyData = weeklyData,
                    error = null
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Failed to load health data"
                android.util.Log.e("MemberDashboardViewModel", "Error loading data: $errorMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    /**
     * Update the member whose data we're viewing and refresh
     */
    fun updateMember(newUserId: String, newFamilyName: String) {
        android.util.Log.d("MemberDashboardViewModel", "updateMember called: current=$userId, new=$newUserId")
        if (userId != newUserId || familyName != newFamilyName) {
            android.util.Log.d("MemberDashboardViewModel", "Switching from userId: $userId to $newUserId")
            userId = newUserId
            familyName = newFamilyName
            
            // Load the new member's chat history (or empty if they have none)
            loadMemberChatHistory()
            
            // Load data for the new member
            loadUserProfileAndWeeklyData()
        } else {
            android.util.Log.d("MemberDashboardViewModel", "No change needed, staying with userId: $userId")
        }
    }
    
    /**
     * Refresh the health data
     */
    fun refreshData() {
        loadUserProfileAndWeeklyData()
    }
    
    /**
     * Set the view mode for displaying health data
     */
    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }
    
    /**
     * Get filtered data based on current view mode
     */
    fun getFilteredData(): List<HealthData> {
        val currentData = _uiState.value.weeklyData
        return when (_uiState.value.viewMode) {
            ViewMode.DAY -> currentData.takeLast(1) // Show today only (most recent day)
            ViewMode.WEEK -> currentData // Show all 7 days
        }
    }
    
    // ============ INDIVIDUAL CHAT FUNCTIONALITY ============
    
    /**
     * Send a message to the individual member's health assistant
     * Creates personalized context based on their health data only
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
            
            // Update both the static storage and the current display
            val currentMemberMessages = memberChatHistory[userId] ?: emptyList()
            val updatedMemberMessages = currentMemberMessages + userMessage
            
            android.util.Log.d("MemberDashboardViewModel", "Adding user message for userId: $userId, total messages: ${updatedMemberMessages.size}")
            
            memberChatHistory[userId] = updatedMemberMessages
            _chatMessages.value = updatedMemberMessages
            
            // Set chat loading state
            _isChatLoading.value = true

            // Generate personalized system prompt with individual health context
            val weeklyData = _uiState.value.weeklyData
            val userProfile = _uiState.value.userProfile
            val systemPrompt = buildString {
                append("You are Ally, a personal health assistant focused on this individual family member's health and wellness. ")
                append("Provide personalized, encouraging advice based on their health data patterns. ")
                append("Be warm, supportive, and focus on achievable improvements. ")
                append("Keep responses brief and actionable, suitable for family members of all ages.")
                
                // Personal Profile Information
                userProfile?.let { profile ->
                    append("\n\n=== MEMBER PROFILE ===")
                    append("\nName: ${profile.name}")
                    
                    // Only include age if it's provided (not 0)
                    if (profile.age > 0) {
                        append("\nAge: ${profile.age} years old")
                    }
                    
                    // Only include gender if provided
                    if (profile.gender.isNotEmpty()) {
                        append("\nGender: ${profile.gender}")
                    }
                    
                    // Only include height if provided
                    if (profile.height > 0) {
                        append("\nHeight: ${profile.height}cm")
                    }
                    
                    // Only include weight if provided
                    if (profile.weight > 0) {
                        append("\nWeight: ${profile.weight}kg")
                    }
                    
                    // If no demographic info is available, note it
                    if (profile.age <= 0 && profile.gender.isEmpty() && profile.height <= 0 && profile.weight <= 0) {
                        append("\nProfile Info: Limited demographic information available - provide general health advice based on activity data.")
                    }
                    
                    // Address them by name for personalization
                    append("\n\nIMPORTANT: Address this person as ${profile.name} in your responses to make them feel personally connected.")
                } ?: append("\n\n=== MEMBER PROFILE ===\nName: Individual family member\nProfile Info: No demographic information available - provide general health advice based on activity data.")
                
                if (weeklyData.isNotEmpty()) {
                    // Individual Health Overview
                    append("\n\n=== INDIVIDUAL HEALTH DATA (Last 7 Days) ===")
                    userProfile?.let { 
                        append("\nMember: ${it.name} (${it.age} years old)")
                    } ?: append("\nMember: Individual family member")
                    append("\nData Period: ${weeklyData.size} days of health tracking")
                    
                    // Daily breakdown
                    append("\n\n=== DAILY HEALTH BREAKDOWN ===")
                    weeklyData.forEachIndexed { index, day ->
                        val dayLabel = when (index) {
                            weeklyData.size - 1 -> "Today"
                            weeklyData.size - 2 -> "Yesterday"
                            else -> "${weeklyData.size - index - 1} days ago"
                        }
                        append("\n$dayLabel (${day.date}):")
                        append("\n  • Steps: ${day.steps} ${if (day.steps >= 10000) "✅ (Goal achieved!)" else "❌ (${10000 - day.steps} steps to goal)"}")
                        append("\n  • Heart Rate: ${if (day.heartRate > 0) "${day.heartRate} BPM" else "No data"}")
                        append("\n  • Sleep: ${if (day.sleepHours > 0) "${day.sleepHours} hours ${when {
                            day.sleepHours >= 8 -> "✅ (Excellent)"
                            day.sleepHours >= 7 -> "🟡 (Good)"
                            else -> "❌ (Needs improvement)"
                        }}" else "No data"}")
                    }
                    
                    // Weekly analysis and trends
                    val validStepsData = weeklyData.filter { it.steps > 0 }
                    val validHeartRateData = weeklyData.filter { it.heartRate > 0 }
                    val validSleepData = weeklyData.filter { it.sleepHours > 0 }
                    
                    append("\n\n=== WEEKLY HEALTH ANALYSIS ===")
                    
                    // Steps analysis
                    if (validStepsData.isNotEmpty()) {
                        val totalSteps = validStepsData.sumOf { it.steps }
                        val avgSteps = totalSteps / validStepsData.size
                        val bestDay = validStepsData.maxByOrNull { it.steps }
                        val goalDays = validStepsData.count { it.steps >= 10000 }
                        
                        append("\nSteps Summary:")
                        append("\n  • Total this week: ${String.format("%,d", totalSteps)} steps")
                        append("\n  • Daily average: ${String.format("%,d", avgSteps)} steps")
                        append("\n  • Best day: ${bestDay?.steps} steps")
                        append("\n  • Goal achieved: $goalDays out of ${validStepsData.size} days")
                        
                        // Step trend analysis
                        if (validStepsData.size >= 2) {
                            val recent = validStepsData.takeLast(3).map { it.steps }.average()
                            val earlier = validStepsData.take(validStepsData.size - 2).map { it.steps }.average()
                            if (recent > earlier) {
                                append("\n  • Trend: ⬆️ Improving activity levels")
                            } else if (recent < earlier) {
                                append("\n  • Trend: ⬇️ Activity decreasing, needs encouragement")
                            } else {
                                append("\n  • Trend: ➡️ Consistent activity levels")
                            }
                        }
                    }
                    
                    // Sleep analysis
                    if (validSleepData.isNotEmpty()) {
                        val avgSleep = validSleepData.map { it.sleepHours }.average()
                        val bestSleep = validSleepData.maxByOrNull { it.sleepHours }
                        val goodSleepDays = validSleepData.count { it.sleepHours >= 7 }
                        
                        append("\n\nSleep Summary:")
                        append("\n  • Average sleep: ${String.format("%.1f", avgSleep)} hours per night")
                        append("\n  • Best night: ${bestSleep?.sleepHours} hours")
                        append("\n  • Good sleep (7+ hours): $goodSleepDays out of ${validSleepData.size} nights")
                        append("\n  • Sleep quality: ${when {
                            avgSleep >= 8 -> "✅ Excellent"
                            avgSleep >= 7 -> "🟡 Good"
                            avgSleep >= 6 -> "⚠️ Needs improvement"
                            else -> "❌ Poor, needs attention"
                        }}")
                    }
                    
                    // Heart rate analysis (if available)
                    if (validHeartRateData.isNotEmpty()) {
                        val avgHeartRate = validHeartRateData.map { it.heartRate }.average()
                        append("\n\nHeart Rate Summary:")
                        append("\n  • Average heart rate: ${avgHeartRate.toInt()} BPM")
                        append("\n  • Data available: ${validHeartRateData.size} out of ${weeklyData.size} days")
                    }
                    
                    // Health insights and recommendations
                    append("\n\n=== PERSONALIZED INSIGHTS ===")
                    
                    // Activity insights
                    if (validStepsData.isNotEmpty()) {
                        val avgSteps = validStepsData.sumOf { it.steps } / validStepsData.size
                        val userName = userProfile?.name ?: "you"
                        val ageContext = userProfile?.age?.takeIf { it > 0 }?.let { age ->
                            when {
                                age < 18 -> " Great for someone your age!"
                                age < 30 -> " Perfect for a young adult!"
                                age < 50 -> " Excellent for maintaining health at ${age}!"
                                else -> " Outstanding activity level at ${age}!"
                            }
                        } ?: ""
                        
                        when {
                            avgSteps >= 12000 -> append("\nActivity: 🌟 ${userName.capitalize()}, you're very active! Great job maintaining high activity levels.${ageContext}")
                            avgSteps >= 10000 -> append("\nActivity: ✅ ${userName.capitalize()}, you're meeting step goals consistently. Keep up the excellent work!${ageContext}")
                            avgSteps >= 7000 -> append("\nActivity: 🟡 ${userName.capitalize()}, good activity level. Try adding 1000 more steps per day to reach the goal.")
                            avgSteps >= 5000 -> append("\nActivity: ⚠️ ${userName.capitalize()}, moderate activity. Consider short walks after meals to boost daily steps.")
                            else -> append("\nActivity: ❌ ${userName.capitalize()}, low activity detected. Start with small goals like 5000 steps per day.")
                        }
                    }
                    
                    // Sleep insights
                    if (validSleepData.isNotEmpty()) {
                        val avgSleep = validSleepData.map { it.sleepHours }.average()
                        val userName = userProfile?.name ?: "you"
                        val ageRecommendation = userProfile?.age?.takeIf { it > 0 }?.let { age ->
                            when {
                                age < 18 -> " Teens need 8-10 hours for proper growth and development."
                                age < 30 -> " Adults your age should aim for 7-9 hours nightly."
                                age < 65 -> " At ${age}, 7-8 hours is ideal for maintaining health."
                                else -> " Older adults typically need 7-8 hours of quality sleep."
                            }
                        } ?: " Most adults should aim for 7-9 hours of quality sleep nightly."
                        
                        when {
                            avgSleep >= 8 -> append("\nSleep: 🌟 ${userName.capitalize()}, excellent sleep habits! You're getting optimal rest.${ageRecommendation}")
                            avgSleep >= 7 -> append("\nSleep: ✅ ${userName.capitalize()}, good sleep duration. Try to maintain this consistent schedule.${ageRecommendation}")
                            avgSleep >= 6 -> append("\nSleep: 🟡 ${userName.capitalize()}, adequate sleep but could improve. Aim for 7-8 hours nightly.${ageRecommendation}")
                            else -> append("\nSleep: ❌ ${userName.capitalize()}, insufficient sleep detected. Prioritize a consistent bedtime routine.${ageRecommendation}")
                        }
                    }
                    
                    // Current screen context
                    append("\n\n=== CURRENT VIEW CONTEXT ===")
                    append("\nScreen: Individual member health dashboard")
                    append("\nView Mode: ${_uiState.value.viewMode.name}")
                    append("\nDisplayed Data: ${if (_uiState.value.viewMode == ViewMode.DAY) "Single day focus" else "7-day trends and patterns"}")
                    append("\nAvailable Features: Day/Week toggle, health charts, personal chat")
                }
                
                append("\n\nREMEMBER: Provide specific, actionable advice based on the actual data shown. ")
                append("Reference specific numbers and trends when answering questions. ")
                append("Be encouraging and focus on achievable next steps for better health. ")
                userProfile?.name?.let { name ->
                    append("Always address the person as ${name} to make responses feel personal and engaging.")
                }
            }

            // Send to LLM service
            llmService.sendMessage(
                messages = _chatMessages.value,
                systemPrompt = systemPrompt
            ).onSuccess { response ->
                // Update both the static storage and the current display
                val currentMemberMessages = memberChatHistory[userId] ?: emptyList()
                val updatedMemberMessages = currentMemberMessages + response
                
                memberChatHistory[userId] = updatedMemberMessages
                _chatMessages.value = updatedMemberMessages
                _isChatLoading.value = false
            }.onFailure { error ->
                // Add error message to chat
                val errorMessage = ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    content = "Sorry, I encountered an error: ${error.message}. Please try again.",
                    role = ChatMessage.Role.ASSISTANT,
                    timestamp = System.currentTimeMillis()
                )
                
                // Update both the static storage and the current display
                val currentMemberMessages = memberChatHistory[userId] ?: emptyList()
                val updatedMemberMessages = currentMemberMessages + errorMessage
                
                memberChatHistory[userId] = updatedMemberMessages
                _chatMessages.value = updatedMemberMessages
                _isChatLoading.value = false
            }
        }
    }
    
    /**
     * Clear individual chat history for the current member only
     */
    fun clearChatHistory() {
        // Remove the current member's chat history from static storage
        memberChatHistory.remove(userId)
        
        // Clear the current display
        _chatMessages.value = emptyList()
        
        android.util.Log.d("MemberDashboardViewModel", "Cleared chat history for userId: $userId")
    }
    
    /**
     * Get LLM provider info for UI display
     */
    fun getLLMProviderInfo(): String {
        return "${llmService.getProviderName()} - ${llmService.getModelName()}"
    }
}

/**
 * UI State for Member Dashboard
 */
data class MemberDashboardUiState(
    val isLoading: Boolean = false,
    val userProfile: User? = null,
    val weeklyData: List<HealthData> = emptyList(),
    val viewMode: ViewMode = ViewMode.WEEK, // Default to week view
    val error: String? = null
) 