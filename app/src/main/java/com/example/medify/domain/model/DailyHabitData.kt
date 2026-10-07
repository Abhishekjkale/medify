package com.example.medify.domain.model

/**
 * Structured domain entity representing a user's daily habits logged in Medify.
 *
 * @property meditationMinutes Minutes spent in mindful meditation or breathwork.
 * @property yogaPosesAttempted List of yoga asanas practiced (e.g. "Vrikshasana / Tree Pose").
 * @property ambientAudio Selected calming soundscape (e.g. "Rainforest", "Tibetan Bowls").
 * @property moodScore Self-reported emotional wellbeing on a 1-5 scale (1: Highly Stressed, 5: At Peace).
 */
data class DailyHabitData(
    val meditationMinutes: Int,
    val yogaPosesAttempted: List<String>,
    val ambientAudio: String,
    val moodScore: Int
) {
    init {
        require(meditationMinutes >= 0) { "Meditation duration cannot be negative" }
        require(moodScore in 1..5) { "Mood score must be between 1 and 5" }
    }

    /**
     * Human-readable mood description for context and display.
     */
    val moodDescription: String
        get() = when (moodScore) {
            1 -> "Highly Stressed & Overwhelmed"
            2 -> "Restless & Taxed"
            3 -> "Balanced & Grounded"
            4 -> "Calm & Focused"
            5 -> "At Peace & Restored"
            else -> "Mindful"
        }

    val moodEmoji: String
        get() = when (moodScore) {
            1 -> "🌧️"
            2 -> "⛅"
            3 -> "🌿"
            4 -> "🌸"
            5 -> "☀️"
            else -> "🧘"
        }
}
