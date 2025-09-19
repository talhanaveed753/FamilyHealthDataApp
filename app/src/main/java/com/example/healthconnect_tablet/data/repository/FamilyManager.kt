package com.example.healthconnect_tablet.data.repository

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages family selection and persistence
 */
class FamilyManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "family_preferences", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_SELECTED_FAMILY = "selected_family"
        private const val KEY_FAMILY_PASSWORDS = "family_passwords"
    }
    
    /**
     * Get the currently selected family
     */
    fun getSelectedFamily(): String? {
        return sharedPreferences.getString(KEY_SELECTED_FAMILY, null)
    }
    
    /**
     * Set the selected family
     */
    fun setSelectedFamily(familyName: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_FAMILY, familyName)
            .apply()
    }
    
    /**
     * Clear the selected family (for logout/switch)
     */
    fun clearSelectedFamily() {
        sharedPreferences.edit()
            .remove(KEY_SELECTED_FAMILY)
            .apply()
    }
    
    /**
     * Check if a family is selected
     */
    fun hasSelectedFamily(): Boolean {
        return getSelectedFamily() != null
    }
    
    /**
     * Validate family password
     */
    fun validateFamilyPassword(familyName: String, password: String): Boolean {
        val familyPasswords = mapOf(
            "family1" to "pass1",
            "family2" to "pass2", 
            "family3" to "pass3",
            "family4" to "pass4",
            "family5" to "pass5",
            "family6" to "pass6",
            "family7" to "pass7",
            "family8" to "pass8",
            "family9" to "pass9",
            "family10" to "pass10"
        )
        
        return familyPasswords[familyName] == password
    }
    
    /**
     * Get all available families
     */
    fun getAvailableFamilies(): List<String> {
        return listOf(
            "family1", "family2", "family3", "family4", "family5",
            "family6", "family7", "family8", "family9", "family10"
        )
    }
} 