package com.example

import com.example.medify.data.fallback.WellnessFallbackEngine
import com.example.medify.data.parser.DailyHabitJsonParser
import com.example.medify.data.remote.MedifyAiService
import com.example.medify.domain.model.DailyHabitData
import com.example.medify.domain.model.InsightSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MedifyAiModuleTest {

    @Test
    fun testJsonParser_parsesSnakeCaseSuccessfully() {
        val json = """
            {
              "meditation_minutes": 25,
              "yoga_poses_attempted": ["Vrikshasana / Tree Pose", "Adho Mukha Svanasana / Downward Dog"],
              "ambient_audio_selected": "Tibetan Bowls",
              "mood_score": 1
            }
        """.trimIndent()

        val parsed = DailyHabitJsonParser.parse(json)
        assertEquals(25, parsed.meditationMinutes)
        assertEquals(2, parsed.yogaPosesAttempted.size)
        assertEquals("Vrikshasana / Tree Pose", parsed.yogaPosesAttempted[0])
        assertEquals("Tibetan Bowls", parsed.ambientAudio)
        assertEquals(1, parsed.moodScore)
    }

    @Test
    fun testJsonParser_parsesCamelCaseSuccessfully() {
        val json = """
            {
              "meditationMinutes": 10,
              "yogaPoses": ["Balasana / Child's Pose"],
              "ambientAudio": "Rainforest",
              "moodScore": 4
            }
        """.trimIndent()

        val parsed = DailyHabitJsonParser.parse(json)
        assertEquals(10, parsed.meditationMinutes)
        assertEquals(1, parsed.yogaPosesAttempted.size)
        assertEquals("Balasana / Child's Pose", parsed.yogaPosesAttempted[0])
        assertEquals("Rainforest", parsed.ambientAudio)
        assertEquals(4, parsed.moodScore)
    }

    @Test
    fun testFallbackEngine_generatesThreeSentencesForHighStressPersona() {
        val habit = DailyHabitData(
            meditationMinutes = 20,
            yogaPosesAttempted = listOf("Vrikshasana / Tree Pose", "Balasana / Child's Pose"),
            ambientAudio = "Rainforest",
            moodScore = 1
        )

        val fallback = WellnessFallbackEngine.generateFallback(
            data = habit,
            source = InsightSource.FALLBACK_OFFLINE
        )

        assertNotNull(fallback.fullSummary)
        assertTrue("Summary should contain validation sentence", fallback.validationSentence.isNotBlank())
        assertTrue("Summary should contain connection sentence", fallback.connectionSentence.isNotBlank())
        assertTrue("Summary should contain actionable sentence", fallback.actionableSentence.isNotBlank())

        // Validate sentence count
        assertEquals(3, fallback.sentences.size)

        // Validate nature/wellness keywords
        assertTrue(fallback.fullSummary.contains("Rainforest") || fallback.connectionSentence.contains("Rainforest"))
    }

    @Test
    fun testAiService_simulatedFailureReturnsAppropriateFallback() = runBlocking {
        val service = MedifyAiService(apiKey = "TEST_KEY")
        val habit = DailyHabitData(
            meditationMinutes = 15,
            yogaPosesAttempted = listOf("Adho Mukha Svanasana / Downward Dog"),
            ambientAudio = "Tibetan Bowls",
            moodScore = 2
        )

        val result = service.generateWellnessSummary(
            data = habit,
            forceSimulateFailure = true
        )

        assertEquals(InsightSource.SIMULATED_TEST, result.source)
        assertEquals(3, result.sentences.size)
        assertTrue(result.actionableSentence.isNotBlank())
    }
}
