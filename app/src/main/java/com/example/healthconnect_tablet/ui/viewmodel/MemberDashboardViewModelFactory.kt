package com.example.healthconnect_tablet.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory for creating MemberDashboardViewModel with required parameters
 */
class MemberDashboardViewModelFactory(
    private val context: Context,
    private val userId: String,
    private val familyName: String
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemberDashboardViewModel::class.java)) {
            return MemberDashboardViewModel(userId, familyName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 