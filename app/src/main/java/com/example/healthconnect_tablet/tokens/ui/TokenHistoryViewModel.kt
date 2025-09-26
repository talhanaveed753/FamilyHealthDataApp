package com.example.healthconnect_tablet.tokens.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthconnect_tablet.tokens.data.TokenRemoteLogger
import com.example.healthconnect_tablet.tokens.storage.PrefsStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TokenHistoryEvent {
    object None : TokenHistoryEvent()
    data class Error(val message: String) : TokenHistoryEvent()
    data class Message(val message: String) : TokenHistoryEvent()
}

data class TokenHistoryUiState(
    val isLoading: Boolean = true,
    val records: List<PrefsStorage.ScanRecord> = emptyList(),
    val event: TokenHistoryEvent = TokenHistoryEvent.None
)

class TokenHistoryViewModel(
    private val context: Context,
    private val userId: String,
    private val familyName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokenHistoryUiState())
    val uiState: StateFlow<TokenHistoryUiState> = _uiState.asStateFlow()

    init {
        refreshHistory()
    }

    fun refreshHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = context.applicationContext
            val records = PrefsStorage.getAllScans(ctx, userId)
            _uiState.update {
                it.copy(isLoading = false, records = records, event = TokenHistoryEvent.None)
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = context.applicationContext
            PrefsStorage.clearAllScans(ctx)
            val synced = trySyncRemote { TokenRemoteLogger.clearUserScans(familyName, userId) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    records = PrefsStorage.getAllScans(ctx, userId),
                    event = if (synced) {
                        TokenHistoryEvent.Message("Cleared all local scans.")
                    } else {
                        TokenHistoryEvent.Error("Cleared locally but failed to sync with Firestore.")
                    }
                )
            }
        }
    }

    fun clearUserHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = context.applicationContext
            PrefsStorage.clearUserScans(ctx, userId)
            val synced = trySyncRemote { TokenRemoteLogger.clearUserScans(familyName, userId) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    records = PrefsStorage.getAllScans(ctx, userId),
                    event = if (synced) {
                        TokenHistoryEvent.Message("Cleared scans for this user.")
                    } else {
                        TokenHistoryEvent.Error("Cleared locally but failed to sync with Firestore.")
                    }
                )
            }
        }
    }

    fun clearToday() {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = context.applicationContext
            PrefsStorage.clearTodayScansForUser(ctx, userId)
            val synced = trySyncRemote { TokenRemoteLogger.clearTodayScans(familyName, userId) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    records = PrefsStorage.getAllScans(ctx, userId),
                    event = if (synced) {
                        TokenHistoryEvent.Message("Cleared today's scans.")
                    } else {
                        TokenHistoryEvent.Error("Cleared locally but failed to sync with Firestore.")
                    }
                )
            }
        }
    }

    fun consumeEvent() {
        _uiState.update { it.copy(event = TokenHistoryEvent.None) }
    }

    private suspend fun trySyncRemote(block: suspend () -> Unit): Boolean = try {
        block()
        true
    } catch (_: Exception) {
        false
    }
}

class TokenHistoryViewModelFactory(
    private val context: Context,
    private val userId: String,
    private val familyName: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TokenHistoryViewModel::class.java)) {
            return TokenHistoryViewModel(context, userId, familyName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
