package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ArbiterVerdictResult
import com.example.data.FocusRepository
import com.example.ui.FocusViewModel
import com.example.ui.SystemHudTab
import com.example.ui.components.PlayerStatusHeaderCard
import com.example.ui.components.SystemBackgroundScanlines
import com.example.ui.components.SystemWindowCard
import com.example.ui.screens.DailyQuestsScreen
import com.example.ui.screens.PenaltySealsScreen
import com.example.ui.screens.StatusTimerScreen
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SystemManaCyan
import com.example.ui.theme.SystemWindowSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SystemFocusHudApp()
            }
        }
    }
}

@Composable
fun SystemFocusHudApp(
    viewModel: FocusViewModel = viewModel()
) {
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val focusDurationMinutes by viewModel.focusDurationMinutes.collectAsStateWithLifecycle()
    val rewardDurationMinutes by viewModel.rewardDurationMinutes.collectAsStateWithLifecycle()
    val timerPhase by viewModel.timerPhase.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val totalPhaseSeconds by viewModel.totalPhaseSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isBlockingActive by viewModel.isBlockingActive.collectAsStateWithLifecycle()
    val forceDungeonLockTestMode by viewModel.forceDungeonLockTestMode.collectAsStateWithLifecycle()
    val sealYoutubeShorts by viewModel.sealYoutubeShorts.collectAsStateWithLifecycle()
    val sealInstagramReels by viewModel.sealInstagramReels.collectAsStateWithLifecycle()
    val blockedPackages by viewModel.blockedPackages.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val isArbiterEvaluating by viewModel.isArbiterEvaluating.collectAsStateWithLifecycle()
    val latestArbiterVerdict by viewModel.latestArbiterVerdict.collectAsStateWithLifecycle()
    val activeBanner by viewModel.activeBanner.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityServiceEnabled.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val appSearchQuery by viewModel.appSearchQuery.collectAsStateWithLifecycle()
    val lastBlockedEventDescription by viewModel.lastBlockedEventDescription.collectAsStateWithLifecycle()

    // Return to primary [ STATUS & TIMER ] screen on back press from secondary tabs
    BackHandler(enabled = selectedTab != SystemHudTab.STATUS_TIMER) {
        viewModel.selectTab(SystemHudTab.STATUS_TIMER)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssalBackground)
            .safeDrawingPadding()
    ) {
        SystemBackgroundScanlines()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val isExpandedConsole = maxWidth >= 600.dp

            if (isExpandedConsole) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PlayerStatusHeaderCard(
                            stats = playerStats,
                            activeBanner = activeBanner,
                            onDismissBanner = viewModel::dismissBanner
                        )

                        SystemConsoleSideRail(
                            selectedTab = selectedTab,
                            isBlockingActive = isBlockingActive,
                            onSelectTab = viewModel::selectTab,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ActiveTabContent(
                            selectedTab = selectedTab,
                            focusDurationMinutes = focusDurationMinutes,
                            rewardDurationMinutes = rewardDurationMinutes,
                            timerPhase = timerPhase,
                            remainingSeconds = remainingSeconds,
                            totalPhaseSeconds = totalPhaseSeconds,
                            isTimerRunning = isTimerRunning,
                            isBlockingActive = isBlockingActive,
                            forceDungeonLockTestMode = forceDungeonLockTestMode,
                            completedDungeons = playerStats.completedDungeons,
                            dailyQuests = dailyQuests,
                            geminiApiKey = geminiApiKey,
                            isArbiterEvaluating = isArbiterEvaluating,
                            latestArbiterVerdict = latestArbiterVerdict,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            sealYoutubeShorts = sealYoutubeShorts,
                            sealInstagramReels = sealInstagramReels,
                            blockedPackages = blockedPackages,
                            installedApps = installedApps,
                            appSearchQuery = appSearchQuery,
                            lastBlockedEventDescription = lastBlockedEventDescription,
                            viewModel = viewModel
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayerStatusHeaderCard(
                        stats = playerStats,
                        activeBanner = activeBanner,
                        onDismissBanner = viewModel::dismissBanner
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        ActiveTabContent(
                            selectedTab = selectedTab,
                            focusDurationMinutes = focusDurationMinutes,
                            rewardDurationMinutes = rewardDurationMinutes,
                            timerPhase = timerPhase,
                            remainingSeconds = remainingSeconds,
                            totalPhaseSeconds = totalPhaseSeconds,
                            isTimerRunning = isTimerRunning,
                            isBlockingActive = isBlockingActive,
                            forceDungeonLockTestMode = forceDungeonLockTestMode,
                            completedDungeons = playerStats.completedDungeons,
                            dailyQuests = dailyQuests,
                            geminiApiKey = geminiApiKey,
                            isArbiterEvaluating = isArbiterEvaluating,
                            latestArbiterVerdict = latestArbiterVerdict,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            sealYoutubeShorts = sealYoutubeShorts,
                            sealInstagramReels = sealInstagramReels,
                            blockedPackages = blockedPackages,
                            installedApps = installedApps,
                            appSearchQuery = appSearchQuery,
                            lastBlockedEventDescription = lastBlockedEventDescription,
                            viewModel = viewModel
                        )
                    }

                    SystemBottomHudDock(
                        selectedTab = selectedTab,
                        onSelectTab = viewModel::selectTab
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    selectedTab: SystemHudTab,
    focusDurationMinutes: Int,
    rewardDurationMinutes: Int,
    timerPhase: com.example.data.TimerPhase,
    remainingSeconds: Int,
    totalPhaseSeconds: Int,
    isTimerRunning: Boolean,
    isBlockingActive: Boolean,
    forceDungeonLockTestMode: Boolean,
    completedDungeons: Int,
    dailyQuests: List<com.example.data.DailyQuest>,
    geminiApiKey: String,
    isArbiterEvaluating: Boolean,
    latestArbiterVerdict: ArbiterVerdictResult?,
    isAccessibilityEnabled: Boolean,
    sealYoutubeShorts: Boolean,
    sealInstagramReels: Boolean,
    blockedPackages: Set<String>,
    installedApps: List<com.example.ui.InstalledAppItem>,
    appSearchQuery: String,
    lastBlockedEventDescription: String?,
    viewModel: FocusViewModel
) {
    when (selectedTab) {
        SystemHudTab.STATUS_TIMER -> {
            StatusTimerScreen(
                focusDurationMinutes = focusDurationMinutes,
                rewardDurationMinutes = rewardDurationMinutes,
                timerPhase = timerPhase,
                remainingSeconds = remainingSeconds,
                totalPhaseSeconds = totalPhaseSeconds,
                isTimerRunning = isTimerRunning,
                isBlockingActive = isBlockingActive,
                completedDungeons = completedDungeons,
                onSelectFocusDuration = viewModel::selectFocusDuration,
                onSelectRewardDuration = viewModel::selectRewardDuration,
                onStartOrToggleTimer = viewModel::startOrToggleDungeonTimer,
                onAbortTimer = viewModel::abortTimer,
                onInstantClearQuest = viewModel::simulateTimerZeroForInstantClear
            )
        }
        SystemHudTab.DAILY_QUESTS -> {
            DailyQuestsScreen(
                quests = dailyQuests,
                savedApiKey = geminiApiKey,
                isArbiterEvaluating = isArbiterEvaluating,
                latestArbiterVerdict = latestArbiterVerdict,
                onSaveApiKey = viewModel::saveGeminiApiKey,
                onSubmitQuestToArbiter = viewModel::submitQuestToArbiter,
                onDismissArbiterVerdict = viewModel::dismissArbiterVerdictPopup,
                onToggleQuest = viewModel::toggleQuest,
                onDeleteQuest = viewModel::deleteQuest,
                onResetDailyQuests = viewModel::resetDailyQuests
            )
        }
        SystemHudTab.PENALTY_SEALS -> {
            PenaltySealsScreen(
                isAccessibilityEnabled = isAccessibilityEnabled,
                isBlockingActive = isBlockingActive,
                forceDungeonLockTestMode = forceDungeonLockTestMode,
                sealYoutubeShorts = sealYoutubeShorts,
                sealInstagramReels = sealInstagramReels,
                blockedPackages = blockedPackages,
                installedApps = installedApps,
                searchQuery = appSearchQuery,
                lastBlockedEventDescription = lastBlockedEventDescription,
                onRefreshAccessibilityStatus = viewModel::refreshAccessibilityStatus,
                onToggleForceDungeonLockTestMode = viewModel::setForceDungeonLockTestMode,
                onToggleSealYoutubeShorts = viewModel::setSealYoutubeShorts,
                onToggleSealInstagramReels = viewModel::setSealInstagramReels,
                onToggleBlockedPackage = viewModel::toggleBlockedPackage,
                onSearchQueryChange = viewModel::updateAppSearchQuery
            )
        }
    }
}

@Composable
private fun SystemBottomHudDock(
    selectedTab: SystemHudTab,
    onSelectTab: (SystemHudTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SystemWindowSurface.copy(alpha = 0.96f), shape)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(SystemManaCyan, RoyalSystemBlue)),
                shape = shape
            )
            .padding(6.dp)
            .testTag("system_bottom_hud_dock"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SystemHudTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val tabShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
            val testTag = when (tab) {
                SystemHudTab.STATUS_TIMER -> "tab_status_timer"
                SystemHudTab.DAILY_QUESTS -> "tab_daily_quests"
                SystemHudTab.PENALTY_SEALS -> "tab_penalty_seals"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .height(50.dp)
                    .clip(tabShape)
                    .background(
                        if (isSelected) {
                            SystemManaCyan.copy(alpha = 0.2f)
                        } else {
                            AbyssalBackground.copy(alpha = 0.7f)
                        },
                        tabShape
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) SystemManaCyan else RoyalSystemBlue.copy(alpha = 0.35f),
                        shape = tabShape
                    )
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 4.dp)
                    .testTag(testTag),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.title,
                        tint = if (isSelected) SystemManaCyan else HudTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) SystemManaCyan else HudTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemConsoleSideRail(
    selectedTab: SystemHudTab,
    isBlockingActive: Boolean,
    onSelectTab: (SystemHudTab) -> Unit,
    modifier: Modifier = Modifier
) {
    SystemWindowCard(
        modifier = modifier.testTag("system_console_side_rail"),
        borderStartColor = SystemManaCyan,
        borderEndColor = RoyalSystemBlue,
        contentPadding = 14.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "[ HUNTER COMMAND CONSOLE ]",
                style = MaterialTheme.typography.labelLarge,
                color = SystemManaCyan,
                fontWeight = FontWeight.Black
            )

            SystemHudTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val tabShape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
                val testTag = when (tab) {
                    SystemHudTab.STATUS_TIMER -> "tab_status_timer"
                    SystemHudTab.DAILY_QUESTS -> "tab_daily_quests"
                    SystemHudTab.PENALTY_SEALS -> "tab_penalty_seals"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .height(52.dp)
                        .clip(tabShape)
                        .background(
                            if (isSelected) {
                                SystemManaCyan.copy(alpha = 0.22f)
                            } else {
                                AbyssalBackground.copy(alpha = 0.8f)
                            },
                            tabShape
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) SystemManaCyan else RoyalSystemBlue.copy(alpha = 0.4f),
                            shape = tabShape
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 14.dp)
                        .testTag(testTag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = tab.title,
                        tint = if (isSelected) SystemManaCyan else HudTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) SystemManaCyan else HudTextPrimary,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBlockingActive) {
                    "STATUS: [ DUNGEON LOCK: ACTIVE ]"
                } else {
                    "STATUS: [ SYSTEM READY ]"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isBlockingActive) PenaltyDangerRed else SystemManaCyan,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun SystemHudTab.icon(): ImageVector {
    return when (this) {
        SystemHudTab.STATUS_TIMER -> Icons.Default.Timer
        SystemHudTab.DAILY_QUESTS -> Icons.Default.Checklist
        SystemHudTab.PENALTY_SEALS -> Icons.Default.Security
    }
}
