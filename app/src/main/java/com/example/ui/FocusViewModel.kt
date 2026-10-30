package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArbiterVerdictResult
import com.example.data.DailyQuest
import com.example.data.FocusRepository
import com.example.data.PlayerStatsState
import com.example.data.SystemAlertBanner
import com.example.data.SystemArbiterClient
import com.example.data.TimerPhase
import com.example.service.FocusBlockerService
import com.example.util.SystemAuthorityNotificationHelper
import kotlinx.coroutines.Dispatchers
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
        FocusRepository.init(application)
        SystemAuthorityNotificationHelper.ensureChannelCreated(application)
    }

    val playerStats: StateFlow<PlayerStatsState> = FocusRepository.playerStats
    val rewardTokens: StateFlow<Int> = FocusRepository.rewardTokens
    val focusDurationMinutes: StateFlow<Int> = FocusRepository.focusDurationMinutes
    val boundQuestId: StateFlow<String?> = FocusRepository.boundQuestId
    val timerPhase: StateFlow<TimerPhase> = FocusRepository.timerPhase
    val remainingSeconds: StateFlow<Int> = FocusRepository.remainingSeconds
    val totalPhaseSeconds: StateFlow<Int> = FocusRepository.totalPhaseSeconds
    val isTimerRunning: StateFlow<Boolean> = FocusRepository.isTimerRunning
    val isBlockingActive: StateFlow<Boolean> = FocusRepository.isBlockingActive
    val forceDungeonLockTestMode: StateFlow<Boolean> = FocusRepository.forceDungeonLockTestMode
    val sealYoutubeShorts: StateFlow<Boolean> = FocusRepository.sealYoutubeShorts
    val sealInstagramReels: StateFlow<Boolean> = FocusRepository.sealInstagramReels
    val blockedPackages: StateFlow<Set<String>> = FocusRepository.blockedPackages
    val unsealedAppsExpiryMap: StateFlow<Map<String, Long>> = FocusRepository.unsealedAppsExpiryMap
    val dailyQuests: StateFlow<List<DailyQuest>> = FocusRepository.dailyQuests
    val geminiApiKey: StateFlow<String> = FocusRepository.geminiApiKey
    val activeBanner: StateFlow<SystemAlertBanner?> = FocusRepository.activeBanner
    val lastBlockedEventDescription: StateFlow<String?> = FocusRepository.lastBlockedEventDescription

    private val _selectedTab = MutableStateFlow(SystemHudTab.STATUS_TIMER)
    val selectedTab: StateFlow<SystemHudTab> = _selectedTab.asStateFlow()

    private val _isAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAccessibilityServiceEnabled: StateFlow<Boolean> = _isAccessibilityServiceEnabled.asStateFlow()

    private val _canDrawOverlays = MutableStateFlow(true)
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _hasUsageStatsPermission = MutableStateFlow(false)
    val hasUsageStatsPermission: StateFlow<Boolean> = _hasUsageStatsPermission.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _appSearchQuery = MutableStateFlow("")
    val appSearchQuery: StateFlow<String> = _appSearchQuery.asStateFlow()

    private val _isArbiterEvaluating = MutableStateFlow(false)
    val isArbiterEvaluating: StateFlow<Boolean> = _isArbiterEvaluating.asStateFlow()

    private val _latestArbiterVerdict = MutableStateFlow<ArbiterVerdictResult?>(null)
    val latestArbiterVerdict: StateFlow<ArbiterVerdictResult?> = _latestArbiterVerdict.asStateFlow()

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
        observeTimerAndUnsealClock()
    }

    private fun observeTimerAndUnsealClock() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _currentTimeMillis.value = System.currentTimeMillis()
                FocusRepository.pruneExpiredUnseals(_currentTimeMillis.value)
                if (isTimerRunning.value) {
                    FocusRepository.tickOneSecond()
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

    fun bindQuestToTimer(questId: String?) {
        FocusRepository.bindQuestToTimer(questId)
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

    fun redeemTokensForApp(packageName: String, appName: String, minutes: Int): Boolean {
        val result = FocusRepository.redeemTokensForApp(packageName, appName, minutes)
        _currentTimeMillis.value = System.currentTimeMillis()
        return result
    }

    fun toggleQuest(questId: String) {
        FocusRepository.toggleQuestCompletion(questId)
    }

    fun saveGeminiApiKey(apiKey: String) {
        FocusRepository.saveGeminiApiKey(apiKey)
    }

    fun submitQuestToArbiter(rawQuestTitle: String) {
        val trimmed = rawQuestTitle.trim()
        if (trimmed.isEmpty() || _isArbiterEvaluating.value) return

        viewModelScope.launch {
            _isArbiterEvaluating.value = true
            try {
                val result = SystemArbiterClient.evaluateQuestSingleRequest(
                    rawQuestInput = trimmed,
                    apiKey = geminiApiKey.value
                )
                FocusRepository.addApprovedArbiterQuest(result)
                _latestArbiterVerdict.value = result
            } finally {
                _isArbiterEvaluating.value = false
            }
        }
    }

    fun dismissArbiterVerdictPopup() {
        _latestArbiterVerdict.value = null
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

    fun setForceDungeonLockTestMode(active: Boolean) {
        FocusRepository.setForceDungeonLockTestMode(active)
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

    fun sendPermissionAccessNotificationNow() {
        val context = getApplication<Application>()
        refreshAccessibilityStatus()
        SystemAuthorityNotificationHelper.postPermissionAccessNotification(
            context = context,
            isAccessibilityEnabled = _isAccessibilityServiceEnabled.value,
            canDrawOverlays = _canDrawOverlays.value
        )
    }

    fun refreshAccessibilityStatus() {
        val context = getApplication<Application>()
        FocusRepository.init(context)
        val a11yEnabled = checkAccessibilityEnabled(context)
        val overlayEnabled = checkOverlayPermissionEnabled(context)
        val notifEnabled = SystemAuthorityNotificationHelper.hasNotificationPermission(context)
        val usageEnabled = checkUsageStatsEnabled(context)

        _isAccessibilityServiceEnabled.value = a11yEnabled
        _canDrawOverlays.value = overlayEnabled
        _hasNotificationPermission.value = notifEnabled
        _hasUsageStatsPermission.value = usageEnabled
        _currentTimeMillis.value = System.currentTimeMillis()

        if (notifEnabled) {
            if (!a11yEnabled || !overlayEnabled) {
                SystemAuthorityNotificationHelper.postPermissionAccessNotification(
                    context = context,
                    isAccessibilityEnabled = a11yEnabled,
                    canDrawOverlays = overlayEnabled
                )
            } else {
                SystemAuthorityNotificationHelper.dismissPermissionNotification(context)
            }
        }
    }

    private fun checkUsageStatsEnabled(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private fun checkOverlayPermissionEnabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
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
