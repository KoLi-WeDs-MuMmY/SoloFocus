package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ArbiterVerdictResult(
    val verdict: String, // "COMPLY" | "MODIFY" | "DECLINE"
    val submittedQuestTitle: String,
    val finalQuestTitle: String,
    val xpReward: Int,
    val systemMessage: String,
    val usedOfflineFallback: Boolean = false
)

object SystemArbiterClient {

    private const val GEMINI_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    /**
     * Strictly executes a SINGLE HTTPS request on Dispatchers.IO ONLY when the user explicitly
     * taps [ SUBMIT QUEST TO SYSTEM ]. Falls back gracefully to the offline heuristic Arbiter
     * if no API key is set or if network/quota is unavailable.
     */
    suspend fun evaluateQuestSingleRequest(
        rawQuestInput: String,
        apiKey: String
    ): ArbiterVerdictResult = withContext(Dispatchers.IO) {
        val cleanedInput = rawQuestInput.trim()
        if (cleanedInput.isEmpty()) {
            return@withContext ArbiterVerdictResult(
                verdict = "DECLINE",
                submittedQuestTitle = rawQuestInput,
                finalQuestTitle = "",
                xpReward = 0,
                systemMessage = "[ ! SYSTEM ERROR ]: Empty quest input detected. Enter a valid training or study objective.",
                usedOfflineFallback = true
            )
        }

        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty() || trimmedKey == "MY_GEMINI_API_KEY") {
            return@withContext evaluateWithOfflineHeuristic(
                cleanedInput,
                reasonPrefix = "[ LOCAL SYSTEM ARBITER ]: "
            )
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("$GEMINI_ENDPOINT?key=$trimmedKey")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 15000
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val promptText = """
                You are the cold, authoritative "System Arbiter" from Solo Leveling.
                Evaluate the Player's proposed daily quest: "$cleanedInput"
                
                Return strictly a single JSON object with these exact keys:
                - "verdict": Choose "COMPLY" if the quest is specific, productive, and worthy of a Hunter (study, deep work, coding, fitness, reading, discipline). Choose "MODIFY" if the quest is productive in intent but too vague, short, or easy (and rewrite "finalQuestTitle" to be concrete, measurable, and challenging). Choose "DECLINE" if the quest is lazy, unproductive, entertainment-binging, or trivial nonsense.
                - "finalQuestTitle": String. The approved quest title (for COMPLY), the upgraded specific quest title (for MODIFY), or the rejected title (for DECLINE).
                - "xpReward": Integer between 50 and 150 based on difficulty (0 if DECLINE).
                - "systemMessage": String. A short, cold, dramatic Solo Leveling System judgment message (1-2 sentences max).
            """.trimIndent()

            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", promptText))
                        )
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.3)
                    }
                )
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            if (responseCode in 200..299 && responseBody.isNotBlank()) {
                return@withContext parseGeminiJsonResponse(cleanedInput, responseBody)
            } else {
                return@withContext evaluateWithOfflineHeuristic(
                    cleanedInput,
                    reasonPrefix = "[ OFFLINE ARBITER FALLBACK (HTTP $responseCode) ]: "
                )
            }
        } catch (_: Exception) {
            return@withContext evaluateWithOfflineHeuristic(
                cleanedInput,
                reasonPrefix = "[ OFFLINE ARBITER FALLBACK ]: "
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseGeminiJsonResponse(
        submittedTitle: String,
        rawResponse: String
    ): ArbiterVerdictResult {
        return try {
            val root = JSONObject(rawResponse)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

            val cleanJson = textPart
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val arbiterJson = JSONObject(cleanJson)
            val rawVerdict = arbiterJson.optString("verdict", "COMPLY").uppercase()
            val verdict = when (rawVerdict) {
                "COMPLY", "MODIFY", "DECLINE" -> rawVerdict
                else -> "COMPLY"
            }

            val finalTitle = arbiterJson.optString("finalQuestTitle", submittedTitle)
                .trim()
                .ifEmpty { submittedTitle }

            val rawXp = arbiterJson.optInt("xpReward", 75)
            val clampedXp = if (verdict == "DECLINE") 0 else rawXp.coerceIn(50, 150)

            val systemMessage = arbiterJson.optString(
                "systemMessage",
                "The System has evaluated your preparation protocol."
            ).trim()

            ArbiterVerdictResult(
                verdict = verdict,
                submittedQuestTitle = submittedTitle,
                finalQuestTitle = finalTitle,
                xpReward = clampedXp,
                systemMessage = systemMessage,
                usedOfflineFallback = false
            )
        } catch (_: Exception) {
            evaluateWithOfflineHeuristic(
                submittedTitle,
                reasonPrefix = "[ HEURISTIC ARBITER ]: "
            )
        }
    }

    private fun evaluateWithOfflineHeuristic(
        input: String,
        reasonPrefix: String
    ): ArbiterVerdictResult {
        val lower = input.lowercase()
        val lazyKeywords = listOf(
            "scroll tiktok", "watch reels", "watch shorts", "binge", "nap all day",
            "play games all day", "procrastinate", "do nothing", "skip study", "lazy"
        )

        if (lazyKeywords.any { lower.contains(it) } || input.length < 3) {
            return ArbiterVerdictResult(
                verdict = "DECLINE",
                submittedQuestTitle = input,
                finalQuestTitle = input,
                xpReward = 0,
                systemMessage = "${reasonPrefix}Unproductive distraction detected. The System refuses to register weak-willed objectives.",
                usedOfflineFallback = true
            )
        }

        val wordCount = input.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val hasNumbersOrDuration = input.any { it.isDigit() }

        return if (wordCount <= 2 && !hasNumbersOrDuration) {
            val upgradedTitle = "$input — 45m Deep Focus & Deliberate Practice [0/1]"
            ArbiterVerdictResult(
                verdict = "MODIFY",
                submittedQuestTitle = input,
                finalQuestTitle = upgradedTitle,
                xpReward = 90,
                systemMessage = "${reasonPrefix}Objective was too vague for Hunter progression. Parameters have been escalated to enforce measurable growth.",
                usedOfflineFallback = true
            )
        } else {
            val xp = when {
                hasNumbersOrDuration && wordCount >= 5 -> 125
                hasNumbersOrDuration || wordCount >= 4 -> 100
                else -> 75
            }
            ArbiterVerdictResult(
                verdict = "COMPLY",
                submittedQuestTitle = input,
                finalQuestTitle = input,
                xpReward = xp,
                systemMessage = "${reasonPrefix}Objective verified. Complete this preparation quest without retreat to claim +$xp XP.",
                usedOfflineFallback = true
            )
        }
    }
}
