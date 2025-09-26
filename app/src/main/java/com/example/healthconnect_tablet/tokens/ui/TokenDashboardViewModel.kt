package com.example.healthconnect_tablet.tokens.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthconnect_tablet.data.repository.FirebaseRepository
import com.example.healthconnect_tablet.tokens.model.TokenAllowance
import com.example.healthconnect_tablet.tokens.model.TokenCounts
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TokenDashboardUiState(
    val isLoading: Boolean = true,
    val allowance: TokenAllowance = TokenAllowance(),
    val scanned: TokenCounts = TokenCounts(),
    val errorMessage: String? = null
) {
    val remaining: TokenCounts
        get() = scanned.remainingFrom(allowance)
}

class TokenDashboardViewModel(
    private val context: Context,
    private val userId: String,
    private val familyName: String
) : ViewModel() {

    private val repository = FirebaseRepository()

    private val _uiState = MutableStateFlow(TokenDashboardUiState())
    val uiState: StateFlow<TokenDashboardUiState> = _uiState.asStateFlow()

    init {
        refreshAllowances()
    }

    fun refreshAllowances() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getTodayTokenAllowance(userId, familyName)
            if (result.isSuccess) {
                val allowance = result.getOrDefault(TokenAllowance())
                val scanned = loadScannedCounts()
                _uiState.value = TokenDashboardUiState(
                    isLoading = false,
                    allowance = allowance,
                    scanned = scanned,
                    errorMessage = null
                )
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Unable to load token allowance"
                    )
                }
            }
        }
    }

    fun refreshScannedCounts() {
        _uiState.update { state ->
            state.copy(scanned = loadScannedCounts())
        }
    }

    private fun loadScannedCounts(): TokenCounts {
        val ctx = context.applicationContext
        return TokenCounts(
            steps = PrefsStorage.getTodayAutomatedCount(ctx, userId, "steps"),
            sleep = PrefsStorage.getTodayAutomatedCount(ctx, userId, "sleep"),
            heart = PrefsStorage.getTodayAutomatedCount(ctx, userId, "heart")
        )
    }
}

class TokenDashboardViewModelFactory(
    private val context: Context,
    private val userId: String,
    private val familyName: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TokenDashboardViewModel::class.java)) {
            return TokenDashboardViewModel(context, userId, familyName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
