package com.prismgrade.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prismgrade.data.local.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKeyDraft: String = "",
    val hasStoredKey: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = settingsStore.apiKey.first()
            _uiState.update {
                it.copy(apiKeyDraft = stored, hasStoredKey = stored.isNotBlank())
            }
        }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKeyDraft = value, isSaved = false) }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsStore.setApiKey(_uiState.value.apiKeyDraft)
            _uiState.update { it.copy(isSaving = false, isSaved = true, hasStoredKey = true) }
        }
    }

    fun clear() {
        viewModelScope.launch {
            settingsStore.clearApiKey()
            _uiState.update {
                it.copy(apiKeyDraft = "", hasStoredKey = false, isSaved = false)
            }
        }
    }

    class Factory(private val settingsStore: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(settingsStore) as T
    }
}
