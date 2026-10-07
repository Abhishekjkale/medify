package com.example.medify.domain.repository

import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.WellnessInsight

/**
 * Clean architecture boundary for generating empathetic daily wellness insights.
 */
interface WellnessAiRepository {
    /**
     * Synthesizes an empathetic 3-sentence summary from structured habit data.
     */
    suspend fun generateInsight(habitData: DailyHabitData): Result<WellnessInsight>

    /**
     * Synthesizes an empathetic 3-sentence summary from a JSON string payload.
     * Expects fields:
     * - meditation_minutes / meditationMinutes
     * - yoga_poses / yogaPosesAttempted
     * - ambient_audio / ambientAudio
     * - mood_score / moodScore
     */
    suspend fun generateInsightFromJson(jsonString: String): Result<WellnessInsight>
}
