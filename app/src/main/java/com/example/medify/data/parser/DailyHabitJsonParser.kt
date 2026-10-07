package com.example.medify.data.parser

import com.example.medify.domain.model.DailyHabitData
import org.json.JSONArray
import org.json.JSONObject

/**
 * Resilient JSON parser supporting varied naming conventions (camelCase, snake_case)
 * for daily habit inputs.
 */
object DailyHabitJsonParser {

    fun parse(jsonString: String): DailyHabitData {
        val root = JSONObject(jsonString)

        val meditationMinutes = root.optInt("meditation_minutes", -1)
            .takeIf { it >= 0 }
            ?: root.optInt("meditationMinutes", -1)
                .takeIf { it >= 0 }
            ?: root.optInt("meditation_duration", -1)
                .takeIf { it >= 0 }
            ?: root.optInt("durationMinutes", 15)

        val posesList = mutableListOf<String>()
        val posesArray: JSONArray? = when {
            root.has("yoga_poses_attempted") -> root.optJSONArray("yoga_poses_attempted")
            root.has("yogaPosesAttempted") -> root.optJSONArray("yogaPosesAttempted")
            root.has("yoga_poses") -> root.optJSONArray("yoga_poses")
            root.has("yogaPoses") -> root.optJSONArray("yogaPoses")
            root.has("poses") -> root.optJSONArray("poses")
            else -> null
        }

        if (posesArray != null) {
            for (i in 0 until posesArray.length()) {
                val pose = posesArray.optString(i)?.trim()
                if (!pose.isNullOrBlank()) {
                    posesList.add(pose)
                }
            }
        } else {
            // Check if string representation was passed
            val singlePoseStr = root.optString("yoga_poses", "")
                .ifBlank { root.optString("yogaPoses", "") }
            if (singlePoseStr.isNotBlank()) {
                posesList.addAll(singlePoseStr.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }

        if (posesList.isEmpty()) {
            posesList.add("Balasana / Child's Pose")
        }

        val ambientAudio = root.optString("ambient_audio_selected", "")
            .ifBlank { root.optString("ambientAudioSelected", "") }
            .ifBlank { root.optString("ambient_audio", "") }
            .ifBlank { root.optString("ambientAudio", "") }
            .ifBlank { root.optString("ambient_sound", "") }
            .ifBlank { "Rainforest" }

        val rawMood = root.optInt("mood_score", -1)
            .takeIf { it in 1..5 }
            ?: root.optInt("moodScore", -1)
                .takeIf { it in 1..5 }
            ?: root.optInt("mood", 3)

        val moodScore = rawMood.coerceIn(1, 5)

        return DailyHabitData(
            meditationMinutes = meditationMinutes.coerceAtLeast(0),
            yogaPosesAttempted = posesList,
            ambientAudio = ambientAudio,
            moodScore = moodScore
        )
    }

    /**
     * Converts DailyHabitData into a beautifully formatted JSON string.
     */
    fun toJson(data: DailyHabitData, indent: Int = 2): String {
        val root = JSONObject()
        root.put("meditation_minutes", data.meditationMinutes)
        val posesArray = JSONArray()
        data.yogaPosesAttempted.forEach { posesArray.put(it) }
        root.put("yoga_poses_attempted", posesArray)
        root.put("ambient_audio_selected", data.ambientAudio)
        root.put("mood_score", data.moodScore)
        return root.toString(indent)
    }
}
