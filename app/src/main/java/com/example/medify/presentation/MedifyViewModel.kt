package com.example.medify.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.medify.data.parser.DailyHabitJsonParser
import com.example.medify.data.remote.MedifyAiService
import com.example.medify.data.repository.WellnessAiRepositoryImpl
import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.WellnessInsight
import com.example.medify.domain.repository.WellnessAiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel governing Medify's wellness habits and AI insight generation.
 */
class MedifyViewModel(
    private val repository: WellnessAiRepository,
    private val aiService: MedifyAiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedifyUiState())
    val uiState: StateFlow<MedifyUiState> = _uiState.asStateFlow()

    init {
        // Populate initial raw JSON matching default habits
        val initialJson = DailyHabitJsonParser.toJson(_uiState.value.currentHabits)
        _uiState.update { it.copy(rawJsonInput = initialJson) }

        // Automatically synthesize initial welcome insight so the screen isn't empty upon initial launch
        generateInsight()
    }

    fun setInputMode(mode: InputMode) {
        _uiState.update { current ->
            if (mode == InputMode.JSON_EDITOR) {
                // Sync current structured habits to JSON editor
                val json = DailyHabitJsonParser.toJson(current.currentHabits)
                current.copy(activeInputMode = mode, rawJsonInput = json)
            } else {
                // When switching back to structured controls, attempt to parse JSON
                try {
                    val parsed = DailyHabitJsonParser.parse(current.rawJsonInput)
                    current.copy(activeInputMode = mode, currentHabits = parsed)
                } catch (_: Exception) {
                    current.copy(activeInputMode = mode)
                }
            }
        }
    }

    fun updateMeditationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 120)
        _uiState.update {
            val updated = it.currentHabits.copy(meditationMinutes = clamped)
            it.copy(
                currentHabits = updated,
                rawJsonInput = DailyHabitJsonParser.toJson(updated)
            )
        }
    }

    fun toggleYogaPose(pose: String) {
        _uiState.update { current ->
            val list = current.currentHabits.yogaPosesAttempted.toMutableList()
            if (list.contains(pose)) {
                if (list.size > 1) { // Keep at least one pose
                    list.remove(pose)
                }
            } else {
                list.add(pose)
            }
            val updated = current.currentHabits.copy(yogaPosesAttempted = list)
            current.copy(
                currentHabits = updated,
                rawJsonInput = DailyHabitJsonParser.toJson(updated)
            )
        }
    }

    fun addCustomYogaPose(pose: String) {
        val trimmed = pose.trim()
        if (trimmed.isBlank()) return
        _uiState.update { current ->
            val list = current.currentHabits.yogaPosesAttempted.toMutableList()
            if (!list.contains(trimmed)) {
                list.add(trimmed)
            }
            val updated = current.currentHabits.copy(yogaPosesAttempted = list)
            current.copy(
                currentHabits = updated,
                rawJsonInput = DailyHabitJsonParser.toJson(updated)
            )
        }
    }

    fun selectAmbientAudio(audio: String) {
        _uiState.update {
            val updated = it.currentHabits.copy(ambientAudio = audio)
            it.copy(
                currentHabits = updated,
                rawJsonInput = DailyHabitJsonParser.toJson(updated)
            )
        }
    }

    fun updateMoodScore(score: Int) {
        val clamped = score.coerceIn(1, 5)
        _uiState.update {
            val updated = it.currentHabits.copy(moodScore = clamped)
            it.copy(
                currentHabits = updated,
                rawJsonInput = DailyHabitJsonParser.toJson(updated)
            )
        }
    }

    fun updateRawJsonInput(json: String) {
        _uiState.update { it.copy(rawJsonInput = json) }
    }

    fun toggleOfflineConstraintSimulation(enabled: Boolean) {
        _uiState.update { it.copy(simulateOfflineConstraint = enabled) }
    }

    fun toggleAudioPreview() {
        _uiState.update { it.copy(isAudioPreviewPlaying = !it.isAudioPreviewPlaying) }
    }

    /**
     * Dispatches insight generation depending on whether structured or raw JSON mode is active.
     */
    fun generateInsight() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }

            val currentState = _uiState.value
            val isSimulation = currentState.simulateOfflineConstraint

            val result: Result<WellnessInsight> = if (isSimulation) {
                // Directly trigger fallback simulation path to test offline / rate-limit resilience
                val simulatedInsight = aiService.generateWellnessSummary(
                    data = currentState.currentHabits,
                    forceSimulateFailure = true
                )
                Result.success(simulatedInsight)
            } else if (currentState.activeInputMode == InputMode.JSON_EDITOR) {
                repository.generateInsightFromJson(currentState.rawJsonInput)
            } else {
                repository.generateInsight(currentState.currentHabits)
            }

            result.fold(
                onSuccess = { insight ->
                    _uiState.update { state ->
                        val history = listOf(insight) + state.historyInsights.take(4)
                        state.copy(
                            isLoading = false,
                            latestInsight = insight,
                            historyInsights = history,
                            currentHabits = insight.habitSnapshot
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            statusMessage = "Unable to generate: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val apiKey = try {
                    BuildConfig.GEMINI_API_KEY
                } catch (_: Exception) {
                    ""
                }
                val aiService = MedifyAiService(apiKey = apiKey)
                val repository = WellnessAiRepositoryImpl(aiService = aiService)
                return MedifyViewModel(repository = repository, aiService = aiService) as T
            }
        }
    }
}
