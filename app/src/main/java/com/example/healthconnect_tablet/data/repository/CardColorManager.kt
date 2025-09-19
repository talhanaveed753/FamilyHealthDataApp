package com.example.healthconnect_tablet.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages card color persistence locally using SharedPreferences
 * Replaces Firebase storage for better performance and offline capability
 */
class CardColorManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "card_colors_preferences", 
        Context.MODE_PRIVATE
    )
    
    // In-memory cache for reactive updates
    private val _cardColors = MutableStateFlow<Map<String, String>>(mutableMapOf())
    val cardColors: StateFlow<Map<String, String>> = _cardColors.asStateFlow()
    
    companion object {
        private const val KEY_PREFIX = "card_color_"
        const val DEFAULT_COLOR = "#FFE0E0E0" // Medium Light Gray
        
        // Available card colors
        val AVAILABLE_COLORS = listOf(
            "#FFE0E0E0", // Medium Light Gray (default)
            "#FFE8F5E8", // Soft Green
            "#FFF3E0F0", // Soft Pink
            "#FFFFF2E0", // Soft Orange
            "#FFE3F2FD", // Soft Blue
            "#FFEDE7F6", // Soft Purple
            "#FFE0F2F1", // Soft Teal
            "#FFFCE4EC"  // Soft Rose
        )
    }
    
    init {
        loadAllColors()
    }
    
    /**
     * Load all stored colors into memory cache
     */
    private fun loadAllColors() {
        val allPrefs = sharedPreferences.all
        val colorMap = mutableMapOf<String, String>()
        
        allPrefs.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX) && value is String) {
                val userId = key.removePrefix(KEY_PREFIX)
                colorMap[userId] = value
            }
        }
        
        _cardColors.value = colorMap
    }
    
    /**
     * Get card color for a specific user
     * Returns default color if not set
     */
    fun getCardColor(userId: String): String {
        return _cardColors.value[userId] 
            ?: sharedPreferences.getString(getKeyForUser(userId), DEFAULT_COLOR) 
            ?: DEFAULT_COLOR
    }
    
    /**
     * Set card color for a specific user
     * Updates both persistent storage and memory cache
     */
    fun setCardColor(userId: String, color: String) {
        // Validate color is in available colors
        val validColor = if (AVAILABLE_COLORS.contains(color)) color else DEFAULT_COLOR
        
        // Save to SharedPreferences
        sharedPreferences.edit()
            .putString(getKeyForUser(userId), validColor)
            .apply()
        
        // Update memory cache
        val currentColors = _cardColors.value.toMutableMap()
        currentColors[userId] = validColor
        _cardColors.value = currentColors
    }
    
    /**
     * Remove card color for a specific user (revert to default)
     */
    fun removeCardColor(userId: String) {
        // Remove from SharedPreferences
        sharedPreferences.edit()
            .remove(getKeyForUser(userId))
            .apply()
        
        // Update memory cache
        val currentColors = _cardColors.value.toMutableMap()
        currentColors.remove(userId)
        _cardColors.value = currentColors
    }
    
    /**
     * Clear all card colors
     */
    fun clearAllColors() {
        val editor = sharedPreferences.edit()
        
        // Remove all card color keys
        _cardColors.value.keys.forEach { userId ->
            editor.remove(getKeyForUser(userId))
        }
        
        editor.apply()
        
        // Clear memory cache
        _cardColors.value = emptyMap()
    }
    
    /**
     * Get all stored colors as a map
     */
    fun getAllColors(): Map<String, String> {
        return _cardColors.value
    }
    
    /**
     * Check if a user has a custom color set
     */
    fun hasCustomColor(userId: String): Boolean {
        return _cardColors.value.containsKey(userId)
    }
    
    /**
     * Get next available color for auto-assignment
     * Returns a color that's not currently used by other users
     */
    fun getNextAvailableColor(excludeUserIds: List<String> = emptyList()): String {
        val usedColors = _cardColors.value
            .filterKeys { !excludeUserIds.contains(it) }
            .values
            .toSet()
        
        // Find first available color that's not used
        val availableColor = AVAILABLE_COLORS.find { !usedColors.contains(it) }
        return availableColor ?: AVAILABLE_COLORS.random()
    }
    
    /**
     * Auto-assign colors to users who don't have one
     */
    fun autoAssignColors(userIds: List<String>) {
        userIds.forEach { userId ->
            if (!hasCustomColor(userId)) {
                val nextColor = getNextAvailableColor(userIds.filter { it != userId })
                setCardColor(userId, nextColor)
            }
        }
    }
    
    /**
     * Generate the SharedPreferences key for a user
     */
    private fun getKeyForUser(userId: String): String {
        return "$KEY_PREFIX$userId"
    }
    
    /**
     * Export colors for backup/sync (if needed in future)
     */
    fun exportColors(): Map<String, String> {
        return getAllColors()
    }
    
    /**
     * Import colors from backup/sync (if needed in future)
     */
    fun importColors(colors: Map<String, String>) {
        val editor = sharedPreferences.edit()
        
        colors.forEach { (userId, color) ->
            if (AVAILABLE_COLORS.contains(color)) {
                editor.putString(getKeyForUser(userId), color)
            }
        }
        
        editor.apply()
        loadAllColors() // Refresh memory cache
    }
} 