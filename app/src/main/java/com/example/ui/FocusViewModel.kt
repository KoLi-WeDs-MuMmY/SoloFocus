package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyQuest
import com.example.data.FocusRepository
import com.example.data.PlayerStatsState
import com.example.data.SystemAlertBanner
import com.example.data.TimerPhase
import com.example.service.FocusBlockerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SystemHudTab(val title: String, val shortLabel: String) {
    STATUS_TIMER("[ STATUS & TIMER ]", "[ STATUS ]"),
    DAILY_QUESTS("[ DAILY QUESTS ]", "[ QUESTS ]"),
    PENALTY_SEALS("[ PENALTY SEALS ]", "[ SEALS ]")
}

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isPresetTarget: Boolean = false
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    init {
        FocusRepository.initialize(application)
    }

    val playerStats: StateFlow<PlayerStatsState> = FocusRepository.playerStats
    val focusDurationMinutes: StateFlow<Int> = FocusRepository.focusDurationMinutes
    val rewardDurationMinutes: StateFlow<Int> = FocusRepository.rewardDurationMinutes
    val timerPhase: StateFlow<TimerPhase> = FocusRepository.timerPhase
    val remainingSeconds: StateFlow<Int> = FocusRepository.remainingSeconds
    val totalPhaseSeconds: StateFlow<Int> = FocusRepository.totalPhaseSeconds
    val isTimerRunning: StateFlow<Boolean> = FocusRepository.isTimerRunning
    val isBlockingActive: StateFlow<Boolean> = FocusRepository.isBlockingActive
    val sealYoutubeShorts: StateFlow<Boolean> = FocusRepository.sealYoutubeShorts
    val sealInstagramReels: StateFlow<Boolean> = FocusRepository.sealInstagramReels
    val blockedPackages: StateFlow<Set<String>> = FocusRepository.blockedPackages
    val dailyQuests: StateFlow<List<DailyQuest>> = FocusRepository.dailyQuests
    val activeBanner: StateFlow<SystemAlertBanner?> = FocusRepository.activeBanner
    val lastBlockedEventDescription: StateFlow<String?> = FocusRepository.lastBlockedEventDescription

    private val _selectedTab = MutableStateFlow(SystemHudTab.STATUS_TIMER)
    val selectedTab: StateFlow<SystemHudTab> = _selectedTab.asStateFlow()

    private val _isAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAccessibilityServiceEnabled: StateFlow<Boolean> = _isAccessibilityServiceEnabled.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private var timerJob: Job? = null

    private val curatedFallbackTargets = listOf(
        InstalledAppItem("com.google.android.youtube", "YouTube", isPresetTarget = true),
        InstalledAppItem("com.instagram.android", "Instagram", isPresetTarget = true),
        InstalledAppItem("com.zhiliaoapp.musically", "TikTok", isPresetTarget = true),
        InstalledAppItem("com.twitter.android", "X / Twitter", isPresetTarget = true),
        InstalledAppItem("com.reddit.frontpage", "Reddit", isPresetTarget = true),
        InstalledAppItem("com.snapchat.android", "Snapchat", isPresetTarget = true),
        InstalledAppItem("com.discord", "Discord", isPresetTarget = true),
        InstalledAppItem("com.netflix.mediaclient", "Netflix", isPresetTarget = true)
    )

    init {
        refreshAccessibilityStatus()
        loadInstalledApps()
        observeTimerLoop()
    }

    private fun observeTimerLoop() {
        viewModelScope.launch {
            isTimerRunning.collect { running ->
                timerJob?.cancel()
                if (running) {
                    timerJob = viewModelScope.launch {
                        while (isTimerRunning.value) {
                            delay(1000L)
                            FocusRepository.tickOneSecond()
                        }
                    }
                }
            }
        }
    }

    fun selectTab(tab: SystemHudTab) {
        _selectedTab.value = tab
    }

    fun selectFocusDuration(minutes: Int) {
        FocusRepository.setFocusDuration(minutes)
    }

    fun selectRewardDuration(minutes: Int) {
        FocusRepository.setRewardDuration(minutes)
    }

    fun startOrToggleDungeonTimer() {
        FocusRepository.togglePauseResumeTimer()
    }

    fun abortTimer() {
        FocusRepository.abortTimerToIdle()
    }

    fun simulateTimerZeroForInstantClear() {
        if (FocusRepository.timerPhase.value == TimerPhase.IDLE) {
            FocusRepository.startFocusDungeon()
        }
        FocusRepository.onPhaseReachedZero()
    }

    fun toggleQuest(questId: String) {
        FocusRepository.toggleQuestCompletion(questId)
    }

    fun addQuest(title: String, subtitle: String) {
        FocusRepository.addCustomQuest(title, subtitle)
    }

    fun deleteQuest(questId: String) {
        FocusRepository.deleteQuest(questId)
    }

    fun resetDailyQuests() {
        FocusRepository.resetDailyQuests()
    }

    fun setSealYoutubeShorts(enabled: Boolean) {
        FocusRepository.setSealYoutubeShorts(enabled)
    }

    fun setSealInstagramReels(enabled: Boolean) {
        FocusRepository.setSealInstagramReels(enabled)
    }

    fun setManualDungeonLock(active: Boolean) {
        FocusRepository.setManualDungeonLock(active)
    }

    fun toggleBlockedPackage(packageName: String) {
        FocusRepository.toggleBlockedPackage(packageName)
    }

    fun updateAppSearchQuery(query: String) {
        _appSearchQuery.value = query
    }

    fun dismissBanner() {
        FocusRepository.dismissBanner()
    }

    fun refreshAccessibilityStatus() {
        val context = getApplication<Application>()
        _isAccessibilityServiceEnabled.value = checkAccessibilityEnabled(context)
    }

    private fun checkAccessibilityEnabled(context: Context): Boolean {
        return try {
            val expectedComponentName = "${context.packageName}/${FocusBlockerService::class.java.canonicalName}"
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                if (componentNameString.equals(expectedComponentName, ignoreCase = true) ||
                    componentNameString.endsWith(FocusBlockerService::class.java.simpleName, ignoreCase = true)
                ) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val ownPkg = context.packageName

            val discovered = try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                resolveInfos.mapNotNull { info ->
                    val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == ownPkg || pkg == "com.android.settings") return@mapNotNull null
                    val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                    InstalledAppItem(
                        packageName = pkg,
                        appName = label,
                        isPresetTarget = curatedFallbackTargets.any { it.packageName == pkg }
                    )
                }.distinctBy { it.packageName }
            } catch (_: Exception) {
                emptyList()
            }

            // Merge curated targets with discovered installed apps so the user always sees
            // common distraction apps alongside all installed launcher apps on the device
            val mergedMap = linkedMapOf<String, InstalledAppItem>()
            curatedFallbackTargets.forEach { mergedMap[it.packageName] = it }
            discovered.forEach { mergedMap[it.packageName] = it }

            val sortedList = mergedMap.values.sortedWith(
                compareByDescending<InstalledAppItem> { it.isPresetTarget }
                    .thenBy { it.appName.lowercase() }
            )

            withContext(Dispatchers.Main) {
                _installedApps.value = sortedList
            }
        }
    }
}
