package com.prismgrade.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prismgrade.data.repository.InspectionRepository
import com.prismgrade.data.repository.SavedInspection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultViewModel(
    private val repository: InspectionRepository,
) : ViewModel() {

    private val _inspection = MutableStateFlow<SavedInspection?>(null)
    val inspection: StateFlow<SavedInspection?> = _inspection.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _inspection.value = repository.findById(id)
        }
    }

    class Factory(private val repository: InspectionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ResultViewModel(repository) as T
    }
}
