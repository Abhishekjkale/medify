package com.example.medify.data.remote

import android.util.Log
import com.example.medify.data.fallback.WellnessFallbackEngine
import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.InsightSource
import com.example.medify.domain.model.WellnessInsight
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Backend-facing AI Module for Medify, utilizing the official Google GenAI SDK for Android
 * (com.google.ai.client.generativeai).
 *
 * Designed with Clean Architecture principles:
 * - Thread-safe, non-blocking coroutines running on Dispatchers.IO.
 * - Deeply tailored prompt engineering for high-stress professionals and a nature/green wellness vibe.
 * - Multi-tiered fallback architecture to guarantee graceful degradation against network outages,
 *   rate limits (HTTP 429), or missing keys.
 */
class MedifyAiService(
    private val apiKey: String,
    private val modelName: String = "gemini-2.5-flash"
) {

    companion object {
        private const val TAG = "MedifyAiService"
        private const val PLACEHOLDER_KEY = "MY_GEMINI_API_KEY"

        private const val SYSTEM_PERSONA_INSTRUCTION = """
You are the empathetic, grounded wellness guide for "Medify", a mindfulness, yoga, and meditation sanctuary.
Your user is a high-stress professional navigating demanding careers, cognitive overload, and intense work deadlines.

Tone Guidelines:
- Calming, serene, warm, and nature-inspired (greenery, deep forest, gentle stillness, grounding earth).
- Highly empathetic without being overly clinical, sterile, or robotic.
- Validates the reality of work stress while opening space for restorative peace.

Core Output Mandate:
Generate a concise, deeply empathetic summary consisting of EXACTLY THREE (3) SENTENCES:
1. Sentence 1 (Validation & Empathy): Validate their present stress or emotional state with warmth, recognizing the effort it took to pause amid pressing demands.
2. Sentence 2 (Mind-Body Connection): Articulate the physiological and emotional harmony nurtured by their specific yoga poses, meditation stillness, or ambient audio choice.
3. Sentence 3 (Actionable Guidance): Provide one concrete, micro-grounding action they can carry with ease into their remaining workday.

Formatting: Output ONLY the 3 sentences in a single continuous paragraph. Do NOT use bullet points, greetings, or sign-offs.
"""
    }

    /**
     * Lazily initialized GenerativeModel from Google GenAI SDK.
     */
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey.trim(),
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 350
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE)
            ),
            systemInstruction = content {
                text(SYSTEM_PERSONA_INSTRUCTION)
            }
        )
    }

    /**
     * Generates a 3-sentence wellness summary using suspend coroutine execution.
     *
     * @param data User's daily habit parameters.
     * @param forceSimulateFailure Optional flag for testing fallback robustness in UI/tests.
     * @return [WellnessInsight] containing the 3-sentence summary and metadata.
     */
    suspend fun generateWellnessSummary(
        data: DailyHabitData,
        forceSimulateFailure: Boolean = false
    ): WellnessInsight = withContext(Dispatchers.IO) {

        // Check for manual safety simulation
        if (forceSimulateFailure) {
            Log.d(TAG, "Simulated fallback triggered for verification")
            return@withContext WellnessFallbackEngine.generateFallback(
                data = data,
                source = InsightSource.SIMULATED_TEST,
                errorDetail = "Simulated network interruption for QA verification"
            )
        }

        // Validate API Key existence
        if (apiKey.isBlank() || apiKey == PLACEHOLDER_KEY) {
            Log.w(TAG, "API key missing or placeholder. Activating local intelligence fallback.")
            return@withContext WellnessFallbackEngine.generateFallback(
                data = data,
                source = InsightSource.FALLBACK_NO_KEY,
                errorDetail = "Google GenAI API key not configured. Using local wellness engine."
            )
        }

        val prompt = buildPrompt(data)

        try {
            Log.d(TAG, "Dispatching GenAI request to model $modelName")
            val response = generativeModel.generateContent(prompt)
            val responseText = response.text?.trim().orEmpty()

            if (responseText.isBlank()) {
                Log.w(TAG, "Empty response from Gemini. Invoking fallback.")
                return@withContext WellnessFallbackEngine.generateFallback(
                    data = data,
                    source = InsightSource.FALLBACK_OFFLINE,
                    errorDetail = "Model returned empty response content"
                )
            }

            // Parse response into structured 3-sentence components
            val sentences = splitIntoSentences(responseText)
            val s1 = sentences.getOrElse(0) { responseText }
            val s2 = sentences.getOrElse(1) { "" }
            val s3 = sentences.getOrElse(2) { "" }

            Log.i(TAG, "Successfully generated wellness insight with ${sentences.size} sentences.")
            WellnessInsight(
                fullSummary = responseText,
                validationSentence = s1,
                connectionSentence = s2,
                actionableSentence = s3,
                source = InsightSource.AI_LIVE,
                habitSnapshot = data,
                timestamp = System.currentTimeMillis(),
                rawPrompt = prompt,
                errorMessage = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during GenAI request: ${e.javaClass.simpleName} - ${e.message}", e)
            handleExceptionFallback(e, data)
        }
    }

    /**
     * Determines the specific cause of failure (Rate limit vs Network vs General)
     * and delegates to the appropriate fallback state.
     */
    private fun handleExceptionFallback(e: Exception, data: DailyHabitData): WellnessInsight {
        val message = e.message.orEmpty().lowercase()
        val isRateLimit = message.contains("429") ||
                message.contains("quota") ||
                message.contains("resource_exhausted") ||
                message.contains("rate limit")

        val isNetwork = e is IOException ||
                e is SocketTimeoutException ||
                e is UnknownHostException ||
                message.contains("unable to resolve host") ||
                message.contains("timeout") ||
                message.contains("connect")

        val source = when {
            isRateLimit -> InsightSource.FALLBACK_RATE_LIMIT
            isNetwork -> InsightSource.FALLBACK_OFFLINE
            else -> InsightSource.FALLBACK_OFFLINE
        }

        return WellnessFallbackEngine.generateFallback(
            data = data,
            source = source,
            errorDetail = "${e.javaClass.simpleName}: ${e.message}"
        )
    }

    /**
     * Constructs the structured prompt containing daily data points.
     */
    private fun buildPrompt(data: DailyHabitData): String {
        val poses = if (data.yogaPosesAttempted.isEmpty()) {
            "Gentle stretching"
        } else {
            data.yogaPosesAttempted.joinToString(", ")
        }

        return """
User Daily Wellness Session Log:
- Meditation Stillness: ${data.meditationMinutes} minutes
- Yoga Asanas Attempted: $poses
- Ambient Audio Selected: ${data.ambientAudio}
- Self-Reported Mood Score: ${data.moodScore} out of 5 (${data.moodDescription})

Persona Context:
The user is a hard-working professional facing high cognitive load.
Provide the tailored 3-sentence summary now according to the calming tone and green wellness guidelines.
""".trimIndent()
    }

    /**
     * Helper to split text into distinct sentences for individual UI highlight cards.
     */
    private fun splitIntoSentences(text: String): List<String> {
        val rawSentences = text
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawSentences.size <= 3) {
            return rawSentences
        }

        // If the model produced more than 3, group the first, middle, and rest into 3 coherent parts
        val s1 = rawSentences[0]
        val s2 = rawSentences[1]
        val s3 = rawSentences.drop(2).joinToString(" ")
        return listOf(s1, s2, s3)
    }
}
