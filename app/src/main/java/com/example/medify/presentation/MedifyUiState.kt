package com.example.medify.presentation

import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.WellnessInsight

enum class InputMode {
    STRUCTURED_CONTROLS,
    JSON_EDITOR
}

data class MedifyUiState(
    val currentHabits: DailyHabitData = DailyHabitData(
        meditationMinutes = 20,
        yogaPosesAttempted = listOf("Vrikshasana / Tree Pose", "Adho Mukha Svanasana / Downward Dog"),
        ambientAudio = "Rainforest",
        moodScore = 2 // Realistic high-stress initial score (2 = Restless & Taxed)
    ),
    val rawJsonInput: String = "",
    val activeInputMode: InputMode = InputMode.STRUCTURED_CONTROLS,
    val isLoading: Boolean = false,
    val latestInsight: WellnessInsight? = null,
    val historyInsights: List<WellnessInsight> = emptyList(),
    val simulateOfflineConstraint: Boolean = false,
    val isAudioPreviewPlaying: Boolean = false,
    val statusMessage: String? = null
)
