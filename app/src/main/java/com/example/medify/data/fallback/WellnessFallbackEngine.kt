package com.example.medify.data.fallback

import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.InsightSource
import com.example.medify.domain.model.WellnessInsight

/**
 * Intelligent local fallback generator designed specifically for Medify.
 * When network constraints, rate limits, or missing API credentials occur,
 * this engine produces context-aware, empathetic, and actionable 3-sentence
 * wellness summaries that preserve the user experience without interruption.
 */
object WellnessFallbackEngine {

    fun generateFallback(
        data: DailyHabitData,
        source: InsightSource,
        errorDetail: String? = null
    ): WellnessInsight {
        val posesJoined = when {
            data.yogaPosesAttempted.isEmpty() -> "gentle stretches"
            data.yogaPosesAttempted.size == 1 -> data.yogaPosesAttempted.first()
            data.yogaPosesAttempted.size == 2 -> "${data.yogaPosesAttempted[0]} and ${data.yogaPosesAttempted[1]}"
            else -> "${data.yogaPosesAttempted.take(2).joinToString(", ")} and others"
        }

        val audio = data.ambientAudio.ifBlank { "Rainforest" }
        val minutes = data.meditationMinutes

        val (s1, s2, s3) = when (data.moodScore) {
            1 -> {
                // Highly Stressed
                Triple(
                    "Carving out $minutes minutes of stillness while navigating intense professional pressure is an act of genuine courage and self-preservation.",
                    "Surrendering into $posesJoined beneath the sheltering sounds of $audio allows your nervous system to gently decelerate from the relentless cognitive demands of your day.",
                    "Before opening your next email or meeting invite, drop your shoulders away from your ears, take two slow diaphragmatic breaths, and remember that urgency is rarely a true crisis."
                )
            }
            2 -> {
                // Restless / Taxed
                Triple(
                    "When demanding deadlines leave your mind buzzing with endless tasks, simply showing up on your mat is a quiet victory worth honoring.",
                    "Your practice of $posesJoined, accompanied by the gentle rhythm of $audio, is actively coaxing accumulated tension out of your spine and jaw.",
                    "Give yourself permission to transition slowly into your afternoon by taking a brief, screen-free walk to let your focus naturally settle."
                )
            }
            3 -> {
                // Centered / Balanced
                Triple(
                    "Balancing the rapid pace of modern work with intentional presence is an art, and you held graceful space for yourself today.",
                    "Anchoring your breath in $minutes minutes of meditation and flowing through $posesJoined to $audio provided the steady pause your mind needed to recalibrate.",
                    "Carry this grounded equilibrium forward by pausing for three mindful breaths between your major work blocks."
                )
            }
            4 -> {
                // Calm / Focused
                Triple(
                    "Your deliberate dedication to nurturing calm amidst high responsibilities is creating a resilient harbor of clarity within you.",
                    "The synergy of $posesJoined and the harmonious ambiance of $audio has softened physical fatigue and renewed your mental vitality.",
                    "Protect this hard-earned tranquility by tackling your priorities with calm discernment and setting peaceful boundaries around your evening hours."
                )
            }
            5 -> {
                // At Peace / Restored
                Triple(
                    "Cultivating profound inner peace amidst a demanding career is a sacred milestone that radiates strength into everything you touch.",
                    "Immersing in $minutes minutes of stillness and grounding into $posesJoined with $audio has replenished your spirit like sunlight through deep forest canopies.",
                    "Let this expansive serenity guide your interactions today, answering each challenge from a place of unshakeable ease and quiet confidence."
                )
            }
            else -> {
                Triple(
                    "Taking time for mindful reflection amidst demanding routines is the highest form of self-respect.",
                    "Moving through $posesJoined accompanied by the calming tones of $audio creates space for restoration and inner renewal.",
                    "Anchor yourself in this peaceful moment and carry one gentle, unhurried breath into every task that awaits."
                )
            }
        }

        val fullText = "$s1 $s2 $s3"

        return WellnessInsight(
            fullSummary = fullText,
            validationSentence = s1,
            connectionSentence = s2,
            actionableSentence = s3,
            source = source,
            habitSnapshot = data,
            timestamp = System.currentTimeMillis(),
            rawPrompt = "[Fallback Generated Locally]",
            errorMessage = errorDetail
        )
    }
}
