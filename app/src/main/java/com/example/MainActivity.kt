package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ArbiterVerdictResult
import com.example.data.FocusRepository
import com.example.ui.FocusViewModel
import com.example.ui.SystemHudTab
import com.example.ui.components.PlayerStatusHeaderCard
import com.example.ui.components.SystemBackgroundScanlines
import com.example.ui.components.SystemHudButton
import com.example.ui.components.SystemWindowCard
import com.example.ui.screens.DailyQuestsScreen
import com.example.ui.screens.PenaltySealsScreen
import com.example.ui.screens.StatusTimerScreen
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MonarchShadowPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SovereignRewardGold
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val rewardTokens by viewModel.rewardTokens.collectAsStateWithLifecycle()
    val focusDurationMinutes by viewModel.focusDurationMinutes.collectAsStateWithLifecycle()
    val boundQuestId by viewModel.boundQuestId.collectAsStateWithLifecycle()
    val timerPhase by viewModel.timerPhase.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val totalPhaseSeconds by viewModel.totalPhaseSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()
    val isBlockingActive by viewModel.isBlockingActive.collectAsStateWithLifecycle()
    val forceDungeonLockTestMode by viewModel.forceDungeonLockTestMode.collectAsStateWithLifecycle()
    val sealYoutubeShorts by viewModel.sealYoutubeShorts.collectAsStateWithLifecycle()
    val sealInstagramReels by viewModel.sealInstagramReels.collectAsStateWithLifecycle()
    val blockedPackages by viewModel.blockedPackages.collectAsStateWithLifecycle()
    val unsealedAppsExpiryMap by viewModel.unsealedAppsExpiryMap.collectAsStateWithLifecycle()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsStateWithLifecycle()
    val dailyQuests by viewModel.dailyQuests.collectAsStateWithLifecycle()
    val geminiApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()
    val isArbiterEvaluating by viewModel.isArbiterEvaluating.collectAsStateWithLifecycle()
    val latestArbiterVerdict by viewModel.latestArbiterVerdict.collectAsStateWithLifecycle()
    val activeBanner by viewModel.activeBanner.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isAccessibilityEnabled by viewModel.isAccessibilityServiceEnabled.collectAsStateWithLifecycle()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsStateWithLifecycle()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    val hasUsageStatsPermission by viewModel.hasUsageStatsPermission.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val appSearchQuery by viewModel.appSearchQuery.collectAsStateWithLifecycle()
    val lastBlockedEventDescription by viewModel.lastBlockedEventDescription.collectAsStateWithLifecycle()

    var hasDismissedStartupPermissionModal by rememberSaveable { mutableStateOf(false) }
    var forceShowPermissionModal by rememberSaveable { mutableStateOf(false) }

    // Runtime permission launcher for Android 13+ POST_NOTIFICATIONS
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshAccessibilityStatus()
        viewModel.sendPermissionAccessNotificationNow()
    }

    val requestNotificationsAndPostAccessBar = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!hasNotificationPermission) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            viewModel.sendPermissionAccessNotificationNow()
        }
    }

    // Automatically request POST_NOTIFICATIONS on first launch so the System can post the
    // permission access notification bar
    LaunchedEffect(Unit) {
        viewModel.refreshAccessibilityStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val missingAnyPermission = !isAccessibilityEnabled || !canDrawOverlays || !hasNotificationPermission
    if ((missingAnyPermission && !hasDismissedStartupPermissionModal) || forceShowPermissionModal) {
        SystemPermissionSetupDialog(
            hasNotificationPermission = hasNotificationPermission,
            canDrawOverlays = canDrawOverlays,
            isAccessibilityEnabled = isAccessibilityEnabled,
            onRequestNotificationPermission = requestNotificationsAndPostAccessBar,
            onOpenAppInfoRestrictedSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onOpenOverlaySettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onOpenAccessibilitySettings = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onDismiss = {
                hasDismissedStartupPermissionModal = true
                forceShowPermissionModal = false
            }
        )
    }

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
                            rewardTokens = rewardTokens,
                            canDrawOverlays = canDrawOverlays && isAccessibilityEnabled,
                            onRequestOverlayPermission = { forceShowPermissionModal = true },
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
                            rewardTokens = rewardTokens,
                            focusDurationMinutes = focusDurationMinutes,
                            boundQuestId = boundQuestId,
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
                            canDrawOverlays = canDrawOverlays,
                            hasNotificationPermission = hasNotificationPermission,
                            hasUsageStatsPermission = hasUsageStatsPermission,
                            sealYoutubeShorts = sealYoutubeShorts,
                            sealInstagramReels = sealInstagramReels,
                            blockedPackages = blockedPackages,
                            unsealedAppsExpiryMap = unsealedAppsExpiryMap,
                            currentTimeMillis = currentTimeMillis,
                            installedApps = installedApps,
                            appSearchQuery = appSearchQuery,
                            onRequestNotificationPermissionAndSendBar = requestNotificationsAndPostAccessBar,
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
                        rewardTokens = rewardTokens,
                        canDrawOverlays = canDrawOverlays && isAccessibilityEnabled,
                        onRequestOverlayPermission = { forceShowPermissionModal = true },
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
                            rewardTokens = rewardTokens,
                            focusDurationMinutes = focusDurationMinutes,
                            boundQuestId = boundQuestId,
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
                            canDrawOverlays = canDrawOverlays,
                            hasNotificationPermission = hasNotificationPermission,
                            hasUsageStatsPermission = hasUsageStatsPermission,
                            sealYoutubeShorts = sealYoutubeShorts,
                            sealInstagramReels = sealInstagramReels,
                            blockedPackages = blockedPackages,
                            unsealedAppsExpiryMap = unsealedAppsExpiryMap,
                            currentTimeMillis = currentTimeMillis,
                            installedApps = installedApps,
                            appSearchQuery = appSearchQuery,
                            onRequestNotificationPermissionAndSendBar = requestNotificationsAndPostAccessBar,
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
private fun SystemPermissionSetupDialog(
    hasNotificationPermission: Boolean,
    canDrawOverlays: Boolean,
    isAccessibilityEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenAppInfoRestrictedSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("system_permission_setup_dialog"),
            borderStartColor = PenaltyDangerRed,
            borderEndColor = SystemManaCyan,
            surfaceColor = AbyssalBackground.copy(alpha = 0.98f),
            contentPadding = 18.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "[ ! SYSTEM AUTHORITY SETUP ]",
                    style = MaterialTheme.typography.titleMedium,
                    color = PenaltyDangerRed,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Grant Display Over Other Apps, Accessibility, and Notification Access. If Android locks Accessibility/Overlay ('Restricted setting'), unlock it via Step 2 or from the Notification Shade:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextPrimary
                )

                SystemHudButton(
                    text = if (hasNotificationPermission) {
                        "[ 1. NOTIFICATIONS: ONLINE (SEND ACCESS BAR) ]"
                    } else {
                        "[ 1. ALLOW NOTIFICATIONS (FOR ACCESS BAR) ]"
                    },
                    onClick = onRequestNotificationPermission,
                    primaryColor = if (hasNotificationPermission) SystemManaCyan else SovereignRewardGold,
                    secondaryColor = RoyalSystemBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                SystemHudButton(
                    text = "[ 2. UNLOCK LOCKED PERMISSIONS (APP INFO ⋮) ]",
                    onClick = onOpenAppInfoRestrictedSettings,
                    primaryColor = MonarchShadowPurple,
                    secondaryColor = SystemManaCyan,
                    modifier = Modifier.fillMaxWidth()
                )

                SystemHudButton(
                    text = if (canDrawOverlays) {
                        "[ 3. DISPLAY OVER OTHER APPS: ONLINE ]"
                    } else {
                        "[ 3. GRANT DISPLAY OVER OTHER APPS ]"
                    },
                    onClick = onOpenOverlaySettings,
                    primaryColor = if (canDrawOverlays) SystemManaCyan else SovereignRewardGold,
                    secondaryColor = MonarchShadowPurple,
                    modifier = Modifier.fillMaxWidth()
                )

                SystemHudButton(
                    text = if (isAccessibilityEnabled) {
                        "[ 4. ACCESSIBILITY BLOCKER: ONLINE ]"
                    } else {
                        "[ 4. GRANT ACCESSIBILITY SERVICE ]"
                    },
                    onClick = onOpenAccessibilitySettings,
                    primaryColor = if (isAccessibilityEnabled) SystemManaCyan else PenaltyDangerRed,
                    secondaryColor = RoyalSystemBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                SystemHudButton(
                    text = "[ CONTINUE TO SYSTEM HUD ]",
                    onClick = onDismiss,
                    primaryColor = SystemManaCyan,
                    secondaryColor = RoyalSystemBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_permission_modal_button")
                )
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    selectedTab: SystemHudTab,
    rewardTokens: Int,
    focusDurationMinutes: Int,
    boundQuestId: String?,
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
    canDrawOverlays: Boolean,
    hasNotificationPermission: Boolean,
    hasUsageStatsPermission: Boolean,
    sealYoutubeShorts: Boolean,
    sealInstagramReels: Boolean,
    blockedPackages: Set<String>,
    unsealedAppsExpiryMap: Map<String, Long>,
    currentTimeMillis: Long,
    installedApps: List<com.example.ui.InstalledAppItem>,
    appSearchQuery: String,
    onRequestNotificationPermissionAndSendBar: () -> Unit,
    viewModel: FocusViewModel
) {
    when (selectedTab) {
        SystemHudTab.STATUS_TIMER -> {
            StatusTimerScreen(
                focusDurationMinutes = focusDurationMinutes,
                boundQuestId = boundQuestId,
                activeDailyQuests = dailyQuests,
                timerPhase = timerPhase,
                remainingSeconds = remainingSeconds,
                totalPhaseSeconds = totalPhaseSeconds,
                isTimerRunning = isTimerRunning,
                isBlockingActive = isBlockingActive,
                completedDungeons = completedDungeons,
                blockedPackageCount = blockedPackages.size,
                onSelectFocusDuration = viewModel::selectFocusDuration,
                onBindQuestToTimer = viewModel::bindQuestToTimer,
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
                canDrawOverlays = canDrawOverlays,
                hasNotificationPermission = hasNotificationPermission,
                hasUsageStatsPermission = hasUsageStatsPermission,
                rewardTokens = rewardTokens,
                unsealedAppsExpiryMap = unsealedAppsExpiryMap,
                currentTimeMillis = currentTimeMillis,
                isBlockingActive = isBlockingActive,
                forceDungeonLockTestMode = forceDungeonLockTestMode,
                sealYoutubeShorts = sealYoutubeShorts,
                sealInstagramReels = sealInstagramReels,
                blockedPackages = blockedPackages,
                installedApps = installedApps,
                searchQuery = appSearchQuery,
                onRefreshPermissions = viewModel::refreshAccessibilityStatus,
                onRequestNotificationPermission = onRequestNotificationPermissionAndSendBar,
                onRedeemTokensForApp = viewModel::redeemTokensForApp,
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
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
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
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
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
