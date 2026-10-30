package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

enum class HunterRank(val label: String, val title: String, val minLevel: Int) {
    E_RANK("E-RANK", "NOVICE AWAKENED", 1),
    D_RANK("D-RANK", "GATE RAIDER", 3),
    C_RANK("C-RANK", "STRIKE SQUAD VANGUARD", 5),
    B_RANK("B-RANK", "ELITE GUILD STRIKER", 8),
    A_RANK("A-RANK", "HIGH RANKER", 11),
    S_RANK("S-RANK", "SHADOW MONARCH", 15);

    companion object {
        fun forLevel(level: Int): HunterRank {
            return entries.lastOrNull { level >= it.minLevel } ?: E_RANK
        }
    }
}

enum class TimerPhase {
    IDLE,
    FOCUS_LOCK,
    REWARD_UNLOCK
}

data class DailyQuest(
    val id: String,
    val title: String,
    val subtitle: String,
    val xpReward: Int = 50,
    val isCompleted: Boolean = false,
    val isStarter: Boolean = false,
    val arbiterVerdict: String? = null
)

data class PlayerStatsState(
    val level: Int = 1,
    val currentXp: Int = 40,
    val xpToNextLevel: Int = 200,
    val rank: HunterRank = HunterRank.E_RANK,
    val streakDays: Int = 1,
    val completedDungeons: Int = 0
)

data class SystemAlertBanner(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val isLevelUp: Boolean = false,
    val isRewardUnlock: Boolean = false
)

object FocusRepository {
    private const val PREFS_NAME = "system_focus_hud_prefs"
    private const val KEY_LEVEL = "player_level"
    private const val KEY_CURRENT_XP = "player_current_xp"
    private const val KEY_STREAK = "player_streak"
    private const val KEY_COMPLETED_DUNGEONS = "player_completed_dungeons"
    private const val KEY_FOCUS_MINUTES = "focus_duration_minutes"
    private const val KEY_REWARD_MINUTES = "reward_duration_minutes"
    private const val KEY_IS_BLOCKING_ACTIVE = "is_blocking_active"
    private const val KEY_FORCE_DUNGEON_LOCK = "force_dungeon_lock_test_mode"
    private const val KEY_SEAL_SHORTS = "seal_youtube_shorts"
    private const val KEY_SEAL_REELS = "seal_instagram_reels"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_QUESTS_JSON = "daily_quests_json"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key_custom"
    private const val KEY_LAST_ARBITER_VERDICT_JSON = "last_arbiter_verdict_json"

    private var prefs: SharedPreferences? = null

    private val defaultBlockedPackages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.snapchat.android"
    )

    private val defaultStarterQuests = listOf(
        DailyQuest(
            id = "quest_deep_work",
            title = "Complete 1 Deep Work Dungeon Session",
            subtitle = "Zero distraction interruptions [0/1]",
            xpReward = 50,
            isCompleted = false,
            isStarter = true
        ),
        DailyQuest(
            id = "quest_code_study",
            title = "Algorithm & Core Skill Training (45m)",
            subtitle = "Intensive problem solving & review",
            xpReward = 75,
            isCompleted = false,
            isStarter = true
        ),
        DailyQuest(
            id = "quest_reading",
            title = "Read 20 Pages of Technical / Study Material",
            subtitle = "Intelligence stat conditioning",
            xpReward = 50,
            isCompleted = false,
            isStarter = true
        ),
        DailyQuest(
            id = "quest_no_scroll",
            title = "Abstain from Short-Form Video Feeds",
            subtitle = "Keep YouTube Shorts & Reels sealed",
            xpReward = 50,
            isCompleted = false,
            isStarter = true
        )
    )

    private val _playerStats = MutableStateFlow(PlayerStatsState())
    val playerStats: StateFlow<PlayerStatsState> = _playerStats.asStateFlow()

    private val _focusDurationMinutes = MutableStateFlow(25)
    val focusDurationMinutes: StateFlow<Int> = _focusDurationMinutes.asStateFlow()

    private val _rewardDurationMinutes = MutableStateFlow(10)
    val rewardDurationMinutes: StateFlow<Int> = _rewardDurationMinutes.asStateFlow()

    private val _timerPhase = MutableStateFlow(TimerPhase.IDLE)
    val timerPhase: StateFlow<TimerPhase> = _timerPhase.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalPhaseSeconds = MutableStateFlow(25 * 60)
    val totalPhaseSeconds: StateFlow<Int> = _totalPhaseSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _isBlockingActive = MutableStateFlow(false)
    val isBlockingActive: StateFlow<Boolean> = _isBlockingActive.asStateFlow()

    private val _forceDungeonLockTestMode = MutableStateFlow(false)
    val forceDungeonLockTestMode: StateFlow<Boolean> = _forceDungeonLockTestMode.asStateFlow()

    private val _sealYoutubeShorts = MutableStateFlow(true)
    val sealYoutubeShorts: StateFlow<Boolean> = _sealYoutubeShorts.asStateFlow()

    private val _sealInstagramReels = MutableStateFlow(true)
    val sealInstagramReels: StateFlow<Boolean> = _sealInstagramReels.asStateFlow()

    private val _blockedPackages = MutableStateFlow(defaultBlockedPackages)
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _dailyQuests = MutableStateFlow(defaultStarterQuests)
    val dailyQuests: StateFlow<List<DailyQuest>> = _dailyQuests.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _activeBanner = MutableStateFlow<SystemAlertBanner?>(null)
    val activeBanner: StateFlow<SystemAlertBanner?> = _activeBanner.asStateFlow()

    private val _lastBlockedEventDescription = MutableStateFlow<String?>(null)
    val lastBlockedEventDescription: StateFlow<String?> = _lastBlockedEventDescription.asStateFlow()

    /**
     * Safe to call from both MainActivity and FocusBlockerService.onServiceConnected() /
     * onAccessibilityEvent() so the AccessibilityService always reads live state even when
     * the main UI is closed or backgrounded.
     */
    fun init(context: Context) {
        val sharedPrefs = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }
        loadFromPrefs(sharedPrefs)
    }

    fun initialize(context: Context) = init(context)

    private fun loadFromPrefs(sharedPrefs: SharedPreferences) {
        val level = sharedPrefs.getInt(KEY_LEVEL, 1).coerceAtLeast(1)
        val xp = sharedPrefs.getInt(KEY_CURRENT_XP, 40).coerceAtLeast(0)
        val streak = sharedPrefs.getInt(KEY_STREAK, 1).coerceAtLeast(1)
        val dungeons = sharedPrefs.getInt(KEY_COMPLETED_DUNGEONS, 0).coerceAtLeast(0)
        val xpNeeded = xpRequiredForLevel(level)

        _playerStats.value = PlayerStatsState(
            level = level,
            currentXp = xp,
            xpToNextLevel = xpNeeded,
            rank = HunterRank.forLevel(level),
            streakDays = streak,
            completedDungeons = dungeons
        )

        val focusMins = sharedPrefs.getInt(KEY_FOCUS_MINUTES, 25)
        val rewardMins = sharedPrefs.getInt(KEY_REWARD_MINUTES, 10)
        _focusDurationMinutes.value = focusMins
        _rewardDurationMinutes.value = rewardMins
        if (_timerPhase.value == TimerPhase.IDLE && !_isTimerRunning.value) {
            _remainingSeconds.value = focusMins * 60
            _totalPhaseSeconds.value = focusMins * 60
        }

        val forceLock = sharedPrefs.getBoolean(KEY_FORCE_DUNGEON_LOCK, false)
        val blockingActive = sharedPrefs.getBoolean(KEY_IS_BLOCKING_ACTIVE, false) || forceLock
        _forceDungeonLockTestMode.value = forceLock
        if (_timerPhase.value != TimerPhase.REWARD_UNLOCK || forceLock) {
            _isBlockingActive.value = blockingActive
        }

        _sealYoutubeShorts.value = sharedPrefs.getBoolean(KEY_SEAL_SHORTS, true)
        _sealInstagramReels.value = sharedPrefs.getBoolean(KEY_SEAL_REELS, true)

        val savedPackages = sharedPrefs.getStringSet(KEY_BLOCKED_PACKAGES, null)
        if (savedPackages != null) {
            _blockedPackages.value = savedPackages.toSet()
        }

        val savedApiKey = sharedPrefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()
        _geminiApiKey.value = savedApiKey

        val questsJson = sharedPrefs.getString(KEY_QUESTS_JSON, null)
        if (!questsJson.isNullOrBlank()) {
            val parsed = parseQuestsJson(questsJson)
            if (parsed.isNotEmpty()) {
                _dailyQuests.value = parsed
            }
        }
    }

    fun xpRequiredForLevel(level: Int): Int {
        return 150 + (level * 50)
    }

    fun saveGeminiApiKey(apiKey: String) {
        val cleaned = apiKey.trim()
        _geminiApiKey.value = cleaned
        prefs?.edit()?.putString(KEY_GEMINI_API_KEY, cleaned)?.apply()
        _activeBanner.value = SystemAlertBanner(
            title = "[ ! SYSTEM NOTIFICATION ]",
            message = if (cleaned.isNotEmpty()) {
                "Gemini API Key stored in local System memory."
            } else {
                "Gemini API Key cleared. Offline Heuristic Arbiter active."
            }
        )
    }

    fun setFocusDuration(minutes: Int) {
        _focusDurationMinutes.value = minutes
        prefs?.edit()?.putInt(KEY_FOCUS_MINUTES, minutes)?.apply()
        if (_timerPhase.value == TimerPhase.IDLE) {
            _remainingSeconds.value = minutes * 60
            _totalPhaseSeconds.value = minutes * 60
        }
    }

    fun setRewardDuration(minutes: Int) {
        _rewardDurationMinutes.value = minutes
        prefs?.edit()?.putInt(KEY_REWARD_MINUTES, minutes)?.apply()
    }

    fun startFocusDungeon() {
        val totalSecs = _focusDurationMinutes.value * 60
        _timerPhase.value = TimerPhase.FOCUS_LOCK
        _totalPhaseSeconds.value = totalSecs
        _remainingSeconds.value = totalSecs
        _isTimerRunning.value = true
        _isBlockingActive.value = true
        prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, true)?.apply()
        _activeBanner.value = SystemAlertBanner(
            title = "[ DUNGEON LOCK: ACTIVE ]",
            message = "Distraction Seals engaged for ${_focusDurationMinutes.value}m. Do not retreat."
        )
    }

    fun togglePauseResumeTimer() {
        if (_timerPhase.value == TimerPhase.IDLE) {
            startFocusDungeon()
            return
        }
        val nextRunning = !_isTimerRunning.value
        _isTimerRunning.value = nextRunning
        if (_timerPhase.value == TimerPhase.FOCUS_LOCK) {
            val shouldBlock = nextRunning || _forceDungeonLockTestMode.value
            _isBlockingActive.value = shouldBlock
            prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, shouldBlock)?.apply()
        }
    }

    fun abortTimerToIdle() {
        _isTimerRunning.value = false
        _timerPhase.value = TimerPhase.IDLE
        val focusSecs = _focusDurationMinutes.value * 60
        _totalPhaseSeconds.value = focusSecs
        _remainingSeconds.value = focusSecs
        val shouldBlock = _forceDungeonLockTestMode.value
        _isBlockingActive.value = shouldBlock
        prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, shouldBlock)?.apply()
    }

    fun setForceDungeonLockTestMode(active: Boolean) {
        _forceDungeonLockTestMode.value = active
        _isBlockingActive.value = active || (_timerPhase.value == TimerPhase.FOCUS_LOCK && _isTimerRunning.value)
        prefs?.edit()
            ?.putBoolean(KEY_FORCE_DUNGEON_LOCK, active)
            ?.putBoolean(KEY_IS_BLOCKING_ACTIVE, _isBlockingActive.value)
            ?.apply()

        if (active) {
            _activeBanner.value = SystemAlertBanner(
                title = "[ FORCE DUNGEON LOCK: ACTIVE ]",
                message = "Test Mode engaged. Blocked apps, YouTube Shorts, and Instagram Reels are immediately sealed."
            )
        } else {
            _activeBanner.value = SystemAlertBanner(
                title = "[ ! SYSTEM NOTIFICATION ]",
                message = "Force Dungeon Lock (Test Mode) disengaged."
            )
        }
    }

    fun setManualDungeonLock(active: Boolean) = setForceDungeonLockTestMode(active)

    fun tickOneSecond() {
        if (!_isTimerRunning.value) return
        val current = _remainingSeconds.value
        if (current > 1) {
            _remainingSeconds.value = current - 1
        } else {
            onPhaseReachedZero()
        }
    }

    fun onPhaseReachedZero() {
        when (_timerPhase.value) {
            TimerPhase.FOCUS_LOCK -> {
                val rewardSecs = _rewardDurationMinutes.value * 60
                _timerPhase.value = TimerPhase.REWARD_UNLOCK
                _totalPhaseSeconds.value = rewardSecs
                _remainingSeconds.value = rewardSecs
                _isTimerRunning.value = true
                _forceDungeonLockTestMode.value = false
                _isBlockingActive.value = false
                prefs?.edit()
                    ?.putBoolean(KEY_FORCE_DUNGEON_LOCK, false)
                    ?.putBoolean(KEY_IS_BLOCKING_ACTIVE, false)
                    ?.apply()

                val leveledUp = addXpInternal(100, incrementDungeonCount = true)
                _activeBanner.value = SystemAlertBanner(
                    title = "[ QUEST CLEARED: REWARD TIME UNLOCKED ]",
                    message = if (leveledUp) {
                        "+100 XP GRANTED • [ LEVEL UP! ] • Distraction Seals temporarily lifted for ${_rewardDurationMinutes.value}m!"
                    } else {
                        "+100 XP GRANTED • Distraction Seals temporarily lifted for ${_rewardDurationMinutes.value}m!"
                    },
                    isLevelUp = leveledUp,
                    isRewardUnlock = true
                )
            }
            TimerPhase.REWARD_UNLOCK -> {
                _isTimerRunning.value = false
                _timerPhase.value = TimerPhase.IDLE
                val focusSecs = _focusDurationMinutes.value * 60
                _totalPhaseSeconds.value = focusSecs
                _remainingSeconds.value = focusSecs
                val hasIncompleteQuests = _dailyQuests.value.any { !it.isCompleted }
                _isBlockingActive.value = hasIncompleteQuests
                prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, hasIncompleteQuests)?.apply()
                _activeBanner.value = SystemAlertBanner(
                    title = "[ ! SYSTEM NOTIFICATION ]",
                    message = if (hasIncompleteQuests) {
                        "Reward window expired. Incomplete Daily Quests detected — Distraction Seals re-locked."
                    } else {
                        "Reward window complete. System awaiting next Dungeon command."
                    }
                )
            }
            TimerPhase.IDLE -> Unit
        }
    }

    fun addXp(amount: Int, reason: String) {
        val leveledUp = addXpInternal(amount, incrementDungeonCount = false)
        if (leveledUp) {
            val stats = _playerStats.value
            _activeBanner.value = SystemAlertBanner(
                title = "[ LEVEL UP! ]",
                message = "Promoted to LV.${stats.level} (${stats.rank.label}: ${stats.rank.title})! ($reason)",
                isLevelUp = true
            )
        } else {
            _activeBanner.value = SystemAlertBanner(
                title = "[ ! SYSTEM NOTIFICATION ]",
                message = "+$amount XP ACQUIRED — $reason",
                isLevelUp = false
            )
        }
    }

    private fun addXpInternal(amount: Int, incrementDungeonCount: Boolean): Boolean {
        var stats = _playerStats.value
        var newXp = stats.currentXp + amount
        var newLevel = stats.level
        var xpNeeded = stats.xpToNextLevel
        var leveledUp = false

        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel += 1
            xpNeeded = xpRequiredForLevel(newLevel)
            leveledUp = true
        }

        val newDungeons = if (incrementDungeonCount) stats.completedDungeons + 1 else stats.completedDungeons
        val newStreak = if (incrementDungeonCount) stats.streakDays + 1 else stats.streakDays
        val newRank = HunterRank.forLevel(newLevel)

        stats = stats.copy(
            level = newLevel,
            currentXp = newXp,
            xpToNextLevel = xpNeeded,
            rank = newRank,
            streakDays = newStreak,
            completedDungeons = newDungeons
        )
        _playerStats.value = stats

        prefs?.edit()
            ?.putInt(KEY_LEVEL, newLevel)
            ?.putInt(KEY_CURRENT_XP, newXp)
            ?.putInt(KEY_STREAK, newStreak)
            ?.putInt(KEY_COMPLETED_DUNGEONS, newDungeons)
            ?.apply()

        return leveledUp
    }

    fun toggleQuestCompletion(questId: String) {
        val currentList = _dailyQuests.value
        val target = currentList.find { it.id == questId } ?: return
        val becomingCompleted = !target.isCompleted

        val updatedList = currentList.map { quest ->
            if (quest.id == questId) quest.copy(isCompleted = becomingCompleted) else quest
        }
        _dailyQuests.value = updatedList
        saveQuestsToPrefs(updatedList)

        if (becomingCompleted) {
            addXp(target.xpReward, "Quest Completed: ${target.title}")
        }
    }

    fun addApprovedArbiterQuest(result: ArbiterVerdictResult) {
        saveLastArbiterVerdictToPrefs(result)
        if (result.verdict == "DECLINE") return

        val finalTitle = result.finalQuestTitle.trim()
        if (finalTitle.isEmpty()) return

        val newQuest = DailyQuest(
            id = "quest_${System.currentTimeMillis()}",
            title = finalTitle,
            subtitle = result.systemMessage,
            xpReward = result.xpReward.coerceIn(50, 150),
            isCompleted = false,
            isStarter = false,
            arbiterVerdict = result.verdict
        )
        val updated = listOf(newQuest) + _dailyQuests.value
        _dailyQuests.value = updated
        saveQuestsToPrefs(updated)
    }

    fun addCustomQuest(
        title: String,
        subtitle: String = "Player-assigned Daily Preparation",
        xpReward: Int = 50,
        arbiterVerdict: String? = null
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val newQuest = DailyQuest(
            id = "quest_${System.currentTimeMillis()}",
            title = trimmed,
            subtitle = subtitle.trim().ifEmpty { "Player-assigned Daily Preparation" },
            xpReward = xpReward.coerceIn(50, 150),
            isCompleted = false,
            isStarter = false,
            arbiterVerdict = arbiterVerdict
        )
        val updated = _dailyQuests.value + newQuest
        _dailyQuests.value = updated
        saveQuestsToPrefs(updated)
    }

    fun deleteQuest(questId: String) {
        val updated = _dailyQuests.value.filterNot { it.id == questId }
        _dailyQuests.value = updated
        saveQuestsToPrefs(updated)
    }

    fun resetDailyQuests() {
        val resetList = _dailyQuests.value.map { it.copy(isCompleted = false) }
        _dailyQuests.value = resetList
        saveQuestsToPrefs(resetList)
    }

    fun setSealYoutubeShorts(enabled: Boolean) {
        _sealYoutubeShorts.value = enabled
        prefs?.edit()?.putBoolean(KEY_SEAL_SHORTS, enabled)?.apply()
    }

    fun setSealInstagramReels(enabled: Boolean) {
        _sealInstagramReels.value = enabled
        prefs?.edit()?.putBoolean(KEY_SEAL_REELS, enabled)?.apply()
    }

    fun toggleBlockedPackage(packageName: String) {
        _blockedPackages.update { current ->
            val next = if (current.contains(packageName)) {
                current - packageName
            } else {
                current + packageName
            }
            prefs?.edit()?.putStringSet(KEY_BLOCKED_PACKAGES, next)?.apply()
            next
        }
    }

    fun recordBlockedInterception(description: String) {
        _lastBlockedEventDescription.value = description
    }

    fun dismissBanner() {
        _activeBanner.value = null
    }

    private fun saveLastArbiterVerdictToPrefs(result: ArbiterVerdictResult) {
        val obj = JSONObject().apply {
            put("verdict", result.verdict)
            put("submittedQuestTitle", result.submittedQuestTitle)
            put("finalQuestTitle", result.finalQuestTitle)
            put("xpReward", result.xpReward)
            put("systemMessage", result.systemMessage)
            put("timestamp", System.currentTimeMillis())
        }
        prefs?.edit()?.putString(KEY_LAST_ARBITER_VERDICT_JSON, obj.toString())?.apply()
    }

    private fun saveQuestsToPrefs(quests: List<DailyQuest>) {
        val array = JSONArray()
        quests.forEach { quest ->
            val obj = JSONObject().apply {
                put("id", quest.id)
                put("title", quest.title)
                put("subtitle", quest.subtitle)
                put("xpReward", quest.xpReward)
                put("isCompleted", quest.isCompleted)
                put("isStarter", quest.isStarter)
                if (quest.arbiterVerdict != null) {
                    put("arbiterVerdict", quest.arbiterVerdict)
                }
            }
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_QUESTS_JSON, array.toString())?.apply()
    }

    private fun parseQuestsJson(json: String): List<DailyQuest> {
        return try {
            val array = JSONArray(json)
            val result = mutableListOf<DailyQuest>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val verdictStr = obj.optString("arbiterVerdict", "").takeIf { it.isNotBlank() }
                result.add(
                    DailyQuest(
                        id = obj.optString("id", "quest_$i"),
                        title = obj.optString("title", "Daily Quest"),
                        subtitle = obj.optString("subtitle", "Player Preparation"),
                        xpReward = obj.optInt("xpReward", 50),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        isStarter = obj.optBoolean("isStarter", false),
                        arbiterVerdict = verdictStr
                    )
                )
            }
            result
        } catch (_: Exception) {
            defaultStarterQuests
        }
    }
}
