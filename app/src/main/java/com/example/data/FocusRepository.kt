package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    val currentXp: Int = 0,
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

data class TokenRedemptionTier(
    val minutes: Int,
    val tokenCost: Int
)

object FocusRepository {
    private const val PREFS_NAME = "system_focus_hud_prefs"
    private const val KEY_LEVEL = "player_level"
    private const val KEY_CURRENT_XP = "player_current_xp"
    private const val KEY_STREAK = "player_streak"
    private const val KEY_COMPLETED_DUNGEONS = "player_completed_dungeons"
    private const val KEY_FOCUS_MINUTES = "focus_duration_minutes"
    private const val KEY_REWARD_TOKENS = "player_reward_tokens"
    private const val KEY_BOUND_QUEST_ID = "bound_quest_id"
    private const val KEY_IS_BLOCKING_ACTIVE = "is_blocking_active"
    private const val KEY_FORCE_DUNGEON_LOCK = "force_dungeon_lock_test_mode"
    private const val KEY_SEAL_SHORTS = "seal_youtube_shorts"
    private const val KEY_SEAL_REELS = "seal_instagram_reels"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_UNSEALED_APPS_JSON = "unsealed_apps_expiry_json"
    private const val KEY_QUESTS_JSON = "daily_quests_json_v12"
    private const val KEY_LAST_QUEST_DATE = "daily_quests_last_date"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key_custom"
    private const val KEY_LAST_ARBITER_VERDICT_JSON = "last_arbiter_verdict_json"

    // Minimum scientific focus threshold: 5 minutes
    const val MIN_FOCUS_THRESHOLD_MINUTES = 5
    // 10 Tokens = 1 minute of app usability; 35m focus = 100 Tokens = 10m app usability
    const val TOKENS_PER_APP_MINUTE = 10

    val TOKEN_REDEMPTION_TIERS: List<TokenRedemptionTier> = listOf(
        TokenRedemptionTier(minutes = 10, tokenCost = 100),
        TokenRedemptionTier(minutes = 20, tokenCost = 200),
        TokenRedemptionTier(minutes = 30, tokenCost = 300),
        TokenRedemptionTier(minutes = 40, tokenCost = 400),
        TokenRedemptionTier(minutes = 50, tokenCost = 500),
        TokenRedemptionTier(minutes = 60, tokenCost = 600)
    )

    private var prefs: SharedPreferences? = null

    private val defaultBlockedPackages = setOf(
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.facebook.katana",
        "com.snapchat.android"
    )

    private val _playerStats = MutableStateFlow(PlayerStatsState())
    val playerStats: StateFlow<PlayerStatsState> = _playerStats.asStateFlow()

    private val _rewardTokens = MutableStateFlow(0)
    val rewardTokens: StateFlow<Int> = _rewardTokens.asStateFlow()

    private val _focusDurationMinutes = MutableStateFlow(35)
    val focusDurationMinutes: StateFlow<Int> = _focusDurationMinutes.asStateFlow()

    private val _boundQuestId = MutableStateFlow<String?>(null)
    val boundQuestId: StateFlow<String?> = _boundQuestId.asStateFlow()

    private val _timerPhase = MutableStateFlow(TimerPhase.IDLE)
    val timerPhase: StateFlow<TimerPhase> = _timerPhase.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(35 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalPhaseSeconds = MutableStateFlow(35 * 60)
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

    // Per-app independent unseal expiration map: packageName -> expirationEpochMillis
    private val _unsealedAppsExpiryMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val unsealedAppsExpiryMap: StateFlow<Map<String, Long>> = _unsealedAppsExpiryMap.asStateFlow()

    // Daily Quests start completely empty (0 quests) and reset at midnight
    private val _dailyQuests = MutableStateFlow<List<DailyQuest>>(emptyList())
    val dailyQuests: StateFlow<List<DailyQuest>> = _dailyQuests.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _activeBanner = MutableStateFlow<SystemAlertBanner?>(null)
    val activeBanner: StateFlow<SystemAlertBanner?> = _activeBanner.asStateFlow()

    private val _lastBlockedEventDescription = MutableStateFlow<String?>(null)
    val lastBlockedEventDescription: StateFlow<String?> = _lastBlockedEventDescription.asStateFlow()

    /**
     * Scientifically tuned Reward Token formula:
     * - Under 5 minutes of focus: 0 Tokens.
     * - 5+ minutes of focus: 35m Focus = 100 Tokens (10m of app usability at 10 Tokens/min).
     *   Rounded to the nearest 5 Tokens for clean RPG HUD numbers.
     */
    fun calculateTokensForFocusMinutes(minutes: Int): Int {
        if (minutes < MIN_FOCUS_THRESHOLD_MINUTES) return 0
        val rawTokens = (minutes * 100.0) / 35.0
        return ((rawTokens / 5.0).roundToInt() * 5).coerceAtLeast(10)
    }

    fun init(context: Context) {
        val sharedPrefs = prefs ?: context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .also { prefs = it }
        loadFromPrefs(sharedPrefs)
        checkAndEnforceMidnightQuestReset(sharedPrefs)
        pruneExpiredUnseals()
    }

    fun initialize(context: Context) = init(context)

    private fun currentLocalDayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun checkAndEnforceMidnightQuestReset(sharedPrefs: SharedPreferences? = prefs) {
        val sp = sharedPrefs ?: return
        val todayKey = currentLocalDayKey()
        val savedDayKey = sp.getString(KEY_LAST_QUEST_DATE, null)
        if (savedDayKey == null) {
            sp.edit().putString(KEY_LAST_QUEST_DATE, todayKey).apply()
        } else if (savedDayKey != todayKey) {
            // Midnight passed: clear Daily Quests log to 0 quests for the new day
            _dailyQuests.value = emptyList()
            _boundQuestId.value = null
            sp.edit()
                .putString(KEY_LAST_QUEST_DATE, todayKey)
                .putString(KEY_QUESTS_JSON, "[]")
                .remove(KEY_BOUND_QUEST_ID)
                .apply()
        }
    }

    private fun loadFromPrefs(sharedPrefs: SharedPreferences) {
        val level = sharedPrefs.getInt(KEY_LEVEL, 1).coerceAtLeast(1)
        val xp = sharedPrefs.getInt(KEY_CURRENT_XP, 0).coerceAtLeast(0)
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

        _rewardTokens.value = sharedPrefs.getInt(KEY_REWARD_TOKENS, 0).coerceAtLeast(0)

        val focusMins = sharedPrefs.getInt(KEY_FOCUS_MINUTES, 35)
        _focusDurationMinutes.value = focusMins
        if (_timerPhase.value == TimerPhase.IDLE && !_isTimerRunning.value) {
            _remainingSeconds.value = focusMins * 60
            _totalPhaseSeconds.value = focusMins * 60
        }

        _boundQuestId.value = sharedPrefs.getString(KEY_BOUND_QUEST_ID, null)?.takeIf { it.isNotBlank() }

        val forceLock = sharedPrefs.getBoolean(KEY_FORCE_DUNGEON_LOCK, false)
        val blockingActive = sharedPrefs.getBoolean(KEY_IS_BLOCKING_ACTIVE, false) || forceLock
        _forceDungeonLockTestMode.value = forceLock
        _isBlockingActive.value = blockingActive

        _sealYoutubeShorts.value = sharedPrefs.getBoolean(KEY_SEAL_SHORTS, true)
        _sealInstagramReels.value = sharedPrefs.getBoolean(KEY_SEAL_REELS, true)

        val savedPackages = sharedPrefs.getStringSet(KEY_BLOCKED_PACKAGES, null)
        if (savedPackages != null) {
            _blockedPackages.value = savedPackages.toSet()
        }

        val unsealedJson = sharedPrefs.getString(KEY_UNSEALED_APPS_JSON, null)
        if (!unsealedJson.isNullOrBlank()) {
            _unsealedAppsExpiryMap.value = parseUnsealedAppsJson(unsealedJson)
        } else {
            _unsealedAppsExpiryMap.value = emptyMap()
        }

        val savedApiKey = sharedPrefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()
        _geminiApiKey.value = savedApiKey

        val questsJson = sharedPrefs.getString(KEY_QUESTS_JSON, null)
        if (!questsJson.isNullOrBlank()) {
            _dailyQuests.value = parseQuestsJson(questsJson)
        } else {
            _dailyQuests.value = emptyList()
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

    fun bindQuestToTimer(questId: String?) {
        _boundQuestId.value = questId
        prefs?.edit()?.apply {
            if (questId == null) remove(KEY_BOUND_QUEST_ID)
            else putString(KEY_BOUND_QUEST_ID, questId)
        }?.apply()
    }

    fun startFocusDungeon() {
        val totalSecs = _focusDurationMinutes.value * 60
        _timerPhase.value = TimerPhase.FOCUS_LOCK
        _totalPhaseSeconds.value = totalSecs
        _remainingSeconds.value = totalSecs
        _isTimerRunning.value = true
        _isBlockingActive.value = true
        prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, true)?.apply()

        val boundQuest = _dailyQuests.value.find { it.id == _boundQuestId.value && !it.isCompleted }
        val expectedTokens = calculateTokensForFocusMinutes(_focusDurationMinutes.value)
        _activeBanner.value = SystemAlertBanner(
            title = "[ DUNGEON LOCK: ACTIVE ]",
            message = if (boundQuest != null) {
                "Bound Quest: \"${boundQuest.title}\" • Clear for +$expectedTokens Tokens & +${100 + boundQuest.xpReward} XP."
            } else {
                "Distraction Seals armed for ${_focusDurationMinutes.value}m. Clear for +$expectedTokens Reward Tokens."
            }
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

    /**
     * If the Player focused for at least the 5-minute threshold before ending early,
     * awards proportional Reward Tokens for the elapsed focused minutes.
     */
    fun abortTimerToIdle() {
        val elapsedSeconds = (_totalPhaseSeconds.value - _remainingSeconds.value).coerceAtLeast(0)
        val elapsedMinutes = elapsedSeconds / 60
        val earnedPartialTokens = if (_timerPhase.value == TimerPhase.FOCUS_LOCK &&
            elapsedMinutes >= MIN_FOCUS_THRESHOLD_MINUTES
        ) {
            calculateTokensForFocusMinutes(elapsedMinutes)
        } else {
            0
        }

        _isTimerRunning.value = false
        _timerPhase.value = TimerPhase.IDLE
        val focusSecs = _focusDurationMinutes.value * 60
        _totalPhaseSeconds.value = focusSecs
        _remainingSeconds.value = focusSecs
        val shouldBlock = _forceDungeonLockTestMode.value
        _isBlockingActive.value = shouldBlock
        prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, shouldBlock)?.apply()

        if (earnedPartialTokens > 0) {
            depositRewardTokens(earnedPartialTokens)
            _activeBanner.value = SystemAlertBanner(
                title = "[ THRESHOLD PASSED: +$earnedPartialTokens TOKENS ]",
                message = "Focused for ${elapsedMinutes}m (>= ${MIN_FOCUS_THRESHOLD_MINUTES}m threshold). +$earnedPartialTokens Reward Tokens deposited."
            )
        }
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
                message = "Test Mode engaged. Sealed apps (unless Token-unsealed) and Shorts/Reels will trigger the Crimson Overlay."
            )
        } else {
            _activeBanner.value = SystemAlertBanner(
                title = "[ ! SYSTEM NOTIFICATION ]",
                message = "Force Dungeon Lock (Test Mode) disengaged."
            )
        }
    }

    fun tickOneSecond() {
        pruneExpiredUnseals()
        if (!_isTimerRunning.value) return
        val current = _remainingSeconds.value
        if (current > 1) {
            _remainingSeconds.value = current - 1
        } else {
            onPhaseReachedZero()
        }
    }

    fun onPhaseReachedZero() {
        if (_timerPhase.value != TimerPhase.FOCUS_LOCK) return

        val focusMins = _focusDurationMinutes.value
        val earnedTokens = calculateTokensForFocusMinutes(focusMins)
        depositRewardTokens(earnedTokens)

        // Check if a Daily Quest was bound to this Focus Timer
        val boundId = _boundQuestId.value
        val boundQuest = _dailyQuests.value.find { it.id == boundId && !it.isCompleted }
        var totalXpGranted = 100
        var boundQuestCompletedTitle: String? = null

        if (boundQuest != null) {
            val updatedQuests = _dailyQuests.value.map { q ->
                if (q.id == boundQuest.id) q.copy(isCompleted = true) else q
            }
            _dailyQuests.value = updatedQuests
            saveQuestsToPrefs(updatedQuests)
            totalXpGranted += boundQuest.xpReward
            boundQuestCompletedTitle = boundQuest.title
            bindQuestToTimer(null)
        }

        // Return timer to IDLE and keep seals armed if any daily quests remain incomplete or force lock is on
        _isTimerRunning.value = false
        _timerPhase.value = TimerPhase.IDLE
        val focusSecs = focusMins * 60
        _totalPhaseSeconds.value = focusSecs
        _remainingSeconds.value = focusSecs
        val hasIncompleteQuests = _dailyQuests.value.any { !it.isCompleted }
        val keepLocked = _forceDungeonLockTestMode.value || hasIncompleteQuests
        _isBlockingActive.value = keepLocked
        prefs?.edit()?.putBoolean(KEY_IS_BLOCKING_ACTIVE, keepLocked)?.apply()

        val leveledUp = addXpInternal(totalXpGranted, incrementDungeonCount = true)

        _activeBanner.value = SystemAlertBanner(
            title = "[ DUNGEON CLEARED: +$earnedTokens TOKENS ]",
            message = buildString {
                append("+$totalXpGranted XP & +$earnedTokens Reward Tokens deposited! ")
                if (boundQuestCompletedTitle != null) {
                    append("Bound Quest \"$boundQuestCompletedTitle\" auto-completed! ")
                }
                if (leveledUp) {
                    append("[ LEVEL UP! ] ")
                }
                append("Redeem Tokens in [ PENALTY SEALS ] to unseal individual apps.")
            },
            isLevelUp = leveledUp,
            isRewardUnlock = true
        )
    }

    private fun depositRewardTokens(tokensToAdd: Int) {
        if (tokensToAdd <= 0) return
        val updated = (_rewardTokens.value + tokensToAdd).coerceAtLeast(0)
        _rewardTokens.value = updated
        prefs?.edit()?.putInt(KEY_REWARD_TOKENS, updated)?.apply()
    }

    /**
     * Redeems fixed-price Reward Tokens (10m..60m in 10m multiples; 10 Tokens = 1m)
     * to temporarily unseal ONLY the specified package. Stackable with no cap.
     */
    fun redeemTokensForApp(packageName: String, appName: String, minutes: Int): Boolean {
        val cost = minutes * TOKENS_PER_APP_MINUTE
        if (minutes <= 0 || _rewardTokens.value < cost) {
            _activeBanner.value = SystemAlertBanner(
                title = "[ ! INSUFFICIENT REWARD TOKENS ]",
                message = "Requires $cost Tokens for ${minutes}m unseal on $appName (Available: ${_rewardTokens.value} Tokens)."
            )
            return false
        }

        val now = System.currentTimeMillis()
        val newBalance = (_rewardTokens.value - cost).coerceAtLeast(0)
        _rewardTokens.value = newBalance
        prefs?.edit()?.putInt(KEY_REWARD_TOKENS, newBalance)?.apply()

        val currentMap = _unsealedAppsExpiryMap.value.toMutableMap()
        val existingExpiry = currentMap[packageName]?.takeIf { it > now } ?: now
        val newExpiry = existingExpiry + (minutes * 60_000L)
        currentMap[packageName] = newExpiry
        _unsealedAppsExpiryMap.value = currentMap
        saveUnsealedAppsToPrefs(currentMap)

        val totalRemainingMins = ((newExpiry - now) / 60_000L).coerceAtLeast(1L)
        _activeBanner.value = SystemAlertBanner(
            title = "[ SEAL LIFTED: ${appName.uppercase()} ]",
            message = "Spent $cost Tokens (+${minutes}m). $appName is unsealed for ${totalRemainingMins}m while all other sealed apps remain locked!",
            isRewardUnlock = true
        )
        return true
    }

    fun isPackageTemporarilyUnsealed(packageName: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val expiry = _unsealedAppsExpiryMap.value[packageName] ?: return false
        return expiry > nowMs
    }

    fun pruneExpiredUnseals(nowMs: Long = System.currentTimeMillis()) {
        val current = _unsealedAppsExpiryMap.value
        if (current.isEmpty()) return
        val activeOnly = current.filterValues { it > nowMs }
        if (activeOnly.size != current.size) {
            _unsealedAppsExpiryMap.value = activeOnly
            saveUnsealedAppsToPrefs(activeOnly)
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

    /**
     * Fixes the Quest Checkbox Exploit: Unchecking a completed quest deducts the exact XP
     * it originally awarded and de-levels the Player if XP drops below 0.
     */
    fun deductXp(amount: Int, reason: String) {
        if (amount <= 0) return
        var stats = _playerStats.value
        var newXp = stats.currentXp - amount
        var newLevel = stats.level
        var deLeveled = false

        while (newXp < 0 && newLevel > 1) {
            newLevel -= 1
            val prevLevelCap = xpRequiredForLevel(newLevel)
            newXp += prevLevelCap
            deLeveled = true
        }
        if (newLevel <= 1 && newXp < 0) {
            newLevel = 1
            newXp = 0
        }

        val xpNeeded = xpRequiredForLevel(newLevel)
        val newRank = HunterRank.forLevel(newLevel)

        stats = stats.copy(
            level = newLevel,
            currentXp = newXp,
            xpToNextLevel = xpNeeded,
            rank = newRank
        )
        _playerStats.value = stats

        prefs?.edit()
            ?.putInt(KEY_LEVEL, newLevel)
            ?.putInt(KEY_CURRENT_XP, newXp)
            ?.apply()

        _activeBanner.value = SystemAlertBanner(
            title = if (deLeveled) "[ ! PENALTY: LEVEL DEMOTION ]" else "[ ! XP REVERSED ]",
            message = "-$amount XP DEDUCTED — $reason (Current: LV.$newLevel • $newXp/$xpNeeded XP)"
        )
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
        checkAndEnforceMidnightQuestReset()
        val currentList = _dailyQuests.value
        val target = currentList.find { it.id == questId } ?: return
        val becomingCompleted = !target.isCompleted

        val updatedList = currentList.map { quest ->
            if (quest.id == questId) quest.copy(isCompleted = becomingCompleted) else quest
        }
        _dailyQuests.value = updatedList
        saveQuestsToPrefs(updatedList)

        if (becomingCompleted) {
            if (_boundQuestId.value == questId) {
                bindQuestToTimer(null)
            }
            addXp(target.xpReward, "Quest Completed: ${target.title}")
        } else {
            // Subtract exact XP when unchecked to prevent XP farming exploit
            deductXp(target.xpReward, "Quest Unchecked: ${target.title}")
        }
    }

    fun addApprovedArbiterQuest(result: ArbiterVerdictResult) {
        checkAndEnforceMidnightQuestReset()
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

    fun deleteQuest(questId: String) {
        if (_boundQuestId.value == questId) {
            bindQuestToTimer(null)
        }
        val updated = _dailyQuests.value.filterNot { it.id == questId }
        _dailyQuests.value = updated
        saveQuestsToPrefs(updated)
    }

    fun resetDailyQuests() {
        val currentList = _dailyQuests.value
        // Deduct XP for any completed quests being reset to incomplete so reset cannot be exploited
        val xpToDeduct = currentList.filter { it.isCompleted }.sumOf { it.xpReward }
        val resetList = currentList.map { it.copy(isCompleted = false) }
        _dailyQuests.value = resetList
        saveQuestsToPrefs(resetList)
        if (xpToDeduct > 0) {
            deductXp(xpToDeduct, "Daily Quests Reset to Incomplete")
        }
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

    private fun saveUnsealedAppsToPrefs(map: Map<String, Long>) {
        val obj = JSONObject()
        val now = System.currentTimeMillis()
        map.forEach { (pkg, expiry) ->
            if (expiry > now) {
                obj.put(pkg, expiry)
            }
        }
        prefs?.edit()?.putString(KEY_UNSEALED_APPS_JSON, obj.toString())?.apply()
    }

    private fun parseUnsealedAppsJson(json: String): Map<String, Long> {
        return try {
            val obj = JSONObject(json)
            val now = System.currentTimeMillis()
            val result = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val expiry = obj.optLong(key, 0L)
                if (expiry > now) {
                    result[key] = expiry
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
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
        prefs?.edit()
            ?.putString(KEY_QUESTS_JSON, array.toString())
            ?.putString(KEY_LAST_QUEST_DATE, currentLocalDayKey())
            ?.apply()
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
            emptyList()
        }
    }
}
