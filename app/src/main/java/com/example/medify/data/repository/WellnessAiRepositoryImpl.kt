package com.example.medify.data.repository

import com.example.medify.data.fallback.WellnessFallbackEngine
import com.example.medify.data.parser.DailyHabitJsonParser
import com.example.medify.data.remote.MedifyAiService
import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.InsightSource
import com.example.medify.domain.model.WellnessInsight
import com.example.medify.domain.repository.WellnessAiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [WellnessAiRepository].
 * Coordinates between the remote AI service, the JSON parser, and the fallback engine.
 */
class WellnessAiRepositoryImpl(
    private val aiService: MedifyAiService
) : WellnessAiRepository {

    override suspend fun generateInsight(habitData: DailyHabitData): Result<WellnessInsight> =
        withContext(Dispatchers.IO) {
            try {
                val insight = aiService.generateWellnessSummary(habitData)
                Result.success(insight)
            } catch (e: Exception) {
                // Guaranteed safety net fallback
                val fallback = WellnessFallbackEngine.generateFallback(
                    data = habitData,
                    source = InsightSource.FALLBACK_OFFLINE,
                    errorDetail = "Repository caught: ${e.message}"
                )
                Result.success(fallback)
            }
        }

    override suspend fun generateInsightFromJson(jsonString: String): Result<WellnessInsight> =
        withContext(Dispatchers.IO) {
            try {
                if (jsonString.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("JSON payload cannot be empty"))
                }

                val habitData = DailyHabitJsonParser.parse(jsonString)
                generateInsight(habitData)
            } catch (e: Exception) {
                // If JSON parsing itself failed, construct safe default habit and return fallback with informative error
                val safeDefault = DailyHabitData(
                    meditationMinutes = 15,
                    yogaPosesAttempted = listOf("Vrikshasana / Tree Pose", "Balasana / Child's Pose"),
                    ambientAudio = "Rainforest",
                    moodScore = 2
                )
                val fallback = WellnessFallbackEngine.generateFallback(
                    data = safeDefault,
                    source = InsightSource.FALLBACK_OFFLINE,
                    errorDetail = "JSON parsing failed: ${e.message}. Displaying fallback for default session."
                )
                Result.success(fallback)
            }
        }
}
