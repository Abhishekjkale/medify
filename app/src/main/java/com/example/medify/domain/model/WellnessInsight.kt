package com.example.medify.domain.model

/**
 * Indicates where the generated summary originated from.
 */
enum class InsightSource(val label: String, val badgeColorHex: Long) {
    AI_LIVE("Gemini AI Live", 0xFF2E6B4F),
    FALLBACK_OFFLINE("Offline Fallback (Network Constraint)", 0xFF4D6353),
    FALLBACK_RATE_LIMIT("Rate Limit Safe Fallback", 0xFF8A6D3B),
    FALLBACK_NO_KEY("Local Intelligence Engine", 0xFF3B6570),
    SIMULATED_TEST("Safety Simulation", 0xFF5D7A68)
}

/**
 * Result data representing the empathetic 3-sentence summary and wellness breakdown.
 *
 * @property fullSummary The complete 3-sentence summary text.
 * @property validationSentence Sentence 1: Empathy and validation for the stressed professional persona.
 * @property connectionSentence Sentence 2: Mind-body observation linking yoga, breath, and audio.
 * @property actionableSentence Sentence 3: Concrete micro-grounding guidance for the day.
 * @property source The origin source of the insight (AI live or fallback).
 * @property habitSnapshot The daily habit parameters this insight was created from.
 * @property timestamp Unix timestamp when the insight was synthesized.
 * @property rawPrompt The prompt sent to the LLM (for transparency / debugging).
 * @property errorMessage Any diagnostic error message if a fallback was triggered.
 */
data class WellnessInsight(
    val fullSummary: String,
    val validationSentence: String,
    val connectionSentence: String,
    val actionableSentence: String,
    val source: InsightSource,
    val habitSnapshot: DailyHabitData,
    val timestamp: Long = System.currentTimeMillis(),
    val rawPrompt: String = "",
    val errorMessage: String? = null
) {
    val sentences: List<String>
        get() = listOf(validationSentence, connectionSentence, actionableSentence)
            .filter { it.isNotBlank() }
}
