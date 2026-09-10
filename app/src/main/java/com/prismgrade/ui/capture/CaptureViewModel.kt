package com.prismgrade.ui.capture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prismgrade.data.image.ImageProcessor
import com.prismgrade.data.image.PreparedImage
import com.prismgrade.data.remote.ClaudeGradingService
import com.prismgrade.data.repository.InspectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which of the two photo slots an action refers to. */
enum class CardSide(val slug: String) {
    FRONT("front"),
    BACK("back"),
}

/** The two ways in: photograph the card, or describe it. */
enum class InspectionMode {
    PHOTOS, DESCRIPTION,
}

data class CaptureUiState(
    val mode: InspectionMode = InspectionMode.PHOTOS,
    val front: PreparedImage? = null,
    val back: PreparedImage? = null,
    val frontError: String? = null,
    val backError: String? = null,
    val cardDescription: String = "",
    val conditionDescription: String = "",
    val isPreparingImage: Boolean = false,
    val isInspecting: Boolean = false,
    val hasApiKey: Boolean = true,
    val errorMessage: String? = null,
    /** Set when an inspection finishes, so the screen can navigate to it. */
    val completedInspectionId: Long? = null,
) {
    val canRunPhotoInspection: Boolean
        get() = front != null && !isInspecting && !isPreparingImage && hasApiKey

    val canRunDescriptionInspection: Boolean
        get() = cardDescription.isNotBlank() && !isInspecting && hasApiKey
}

class CaptureViewModel(
    private val repository: InspectionRepository,
    private val imageProcessor: ImageProcessor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    init {
        refreshApiKeyState()
    }

    /** Re-checked on every return to the screen — the key may have just been added. */
    fun refreshApiKeyState() {
        viewModelScope.launch {
            val present = repository.hasApiKey()
            _uiState.update { it.copy(hasApiKey = present) }
        }
    }

    fun setMode(mode: InspectionMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    /** A temp file for the camera app to write into, exposed via FileProvider. */
    fun newCaptureFile() = imageProcessor.newCaptureFile()

    fun onImagePicked(side: CardSide, uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPreparingImage = true,
                    frontError = if (side == CardSide.FRONT) null else it.frontError,
                    backError = if (side == CardSide.BACK) null else it.backError,
                )
            }
            try {
                val prepared = repository.prepareImage(uri, side.slug)
                _uiState.update { state ->
                    when (side) {
                        CardSide.FRONT -> state.copy(front = prepared, frontError = null)
                        CardSide.BACK -> state.copy(back = prepared, backError = null)
                    }.copy(isPreparingImage = false, errorMessage = null)
                }
            } catch (e: ImageProcessor.UnreadableImageException) {
                val message = "That photo couldn't be read. Try another shot."
                _uiState.update { state ->
                    when (side) {
                        CardSide.FRONT -> state.copy(frontError = message)
                        CardSide.BACK -> state.copy(backError = message)
                    }.copy(isPreparingImage = false)
                }
            }
        }
    }

    fun clearImage(side: CardSide) {
        _uiState.update { state ->
            when (side) {
                CardSide.FRONT -> state.copy(front = null, frontError = null)
                CardSide.BACK -> state.copy(back = null, backError = null)
            }
        }
    }

    fun onCardDescriptionChange(value: String) {
        _uiState.update { it.copy(cardDescription = value) }
    }

    fun onConditionDescriptionChange(value: String) {
        _uiState.update { it.copy(conditionDescription = value) }
    }

    fun runInspection() {
        val state = _uiState.value
        if (state.isInspecting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isInspecting = true, errorMessage = null) }
            try {
                val saved = when (state.mode) {
                    InspectionMode.PHOTOS -> {
                        val front = state.front ?: return@launch
                        repository.inspectPhotos(front, state.back)
                    }

                    InspectionMode.DESCRIPTION -> repository.inspectDescription(
                        card = state.cardDescription,
                        condition = state.conditionDescription,
                    )
                }
                _uiState.update {
                    it.copy(isInspecting = false, completedInspectionId = saved.id)
                }
            } catch (e: ClaudeGradingService.GradingException) {
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = e.message,
                        hasApiKey = e !is ClaudeGradingService.GradingException.MissingApiKey,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInspecting = false,
                        errorMessage = "The inspection didn't complete. Try again in a moment.",
                    )
                }
            }
        }
    }

    /** Called once the result screen has been opened. */
    fun onNavigatedToResult() {
        _uiState.update { it.copy(completedInspectionId = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Start a fresh inspection, keeping the chosen mode. */
    fun reset() {
        _uiState.update {
            CaptureUiState(mode = it.mode, hasApiKey = it.hasApiKey)
        }
    }

    class Factory(
        private val repository: InspectionRepository,
        private val imageProcessor: ImageProcessor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CaptureViewModel(repository, imageProcessor) as T
    }
}
