package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DailyQuest
import com.example.data.FocusRepository
import com.example.data.TimerPhase
import com.example.ui.components.ManaCoreTimerDial
import com.example.ui.components.SystemDurationChip
import com.example.ui.components.SystemHudButton
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MonarchShadowPurple
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SovereignRewardGold
import com.example.ui.theme.SystemManaCyan
import com.example.ui.theme.SystemWindowSurfaceElevated

@Composable
fun StatusTimerScreen(
    focusDurationMinutes: Int,
    boundQuestId: String?,
    activeDailyQuests: List<DailyQuest>,
    timerPhase: TimerPhase,
    remainingSeconds: Int,
    totalPhaseSeconds: Int,
    isTimerRunning: Boolean,
    isBlockingActive: Boolean,
    completedDungeons: Int,
    blockedPackageCount: Int,
    onSelectFocusDuration: (Int) -> Unit,
    onBindQuestToTimer: (String?) -> Unit,
    onStartOrToggleTimer: () -> Unit,
    onAbortTimer: () -> Unit,
    onInstantClearQuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusRow1 = listOf(5, 15, 25)
    val focusRow2 = listOf(35, 50, 70)

    val incompleteQuests = activeDailyQuests.filter { !it.isCompleted }
    val boundQuest = incompleteQuests.find { it.id == boundQuestId }
    val expectedTokens = FocusRepository.calculateTokensForFocusMinutes(focusDurationMinutes)

    val isLockActive = timerPhase == TimerPhase.FOCUS_LOCK || isBlockingActive
    val phaseBorderStart = if (isLockActive) PenaltyDangerRed else SystemManaCyan
    val phaseBorderEnd = if (isLockActive) MonarchShadowPurple else RoyalSystemBlue

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------------------------------------------------------
        // CARD 1: STATUS & ACTIVE FOCUS SESSION CARD
        // ---------------------------------------------------------
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mana_core_system_window"),
            borderStartColor = phaseBorderStart,
            borderEndColor = phaseBorderEnd,
            contentPadding = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Focus State Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (timerPhase) {
                                TimerPhase.FOCUS_LOCK -> "[ DUNGEON LOCK: ACTIVE ]"
                                TimerPhase.REWARD_UNLOCK -> "[ TOKENS DEPOSITED ]"
                                TimerPhase.IDLE -> "[ SYSTEM READY: FOCUS GATE ]"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = phaseBorderStart,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (isLockActive) {
                                "Enforcing Lock on $blockedPackageCount Sealed Apps"
                            } else {
                                "$blockedPackageCount Apps Sealed • Ready for Focus Session"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = HudTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                            .background(AbyssalBackground)
                            .border(
                                width = 1.dp,
                                color = phaseBorderStart.copy(alpha = 0.7f),
                                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock State",
                                tint = phaseBorderStart,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLockActive) "LOCKED" else "STANDBY",
                                style = MaterialTheme.typography.labelSmall,
                                color = phaseBorderStart,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Centerpiece Custom Canvas Mana Core Timer Dial
                ManaCoreTimerDial(
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalPhaseSeconds,
                    timerPhase = timerPhase,
                    isTimerRunning = isTimerRunning,
                    isBlockingActive = isBlockingActive,
                    expectedTokenYield = expectedTokens,
                    boundQuestTitle = boundQuest?.title,
                    modifier = Modifier
                        .size(240.dp)
                        .padding(4.dp)
                )

                // Primary Action Controls (Start / Pause / End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val primaryButtonLabel = when {
                        timerPhase == TimerPhase.IDLE -> "[ ENTER FOCUS DUNGEON ]"
                        isTimerRunning -> "[ PAUSE MANA CORE ]"
                        else -> "[ RESUME MANA CORE ]"
                    }

                    SystemHudButton(
                        text = primaryButtonLabel,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStartOrToggleTimer()
                        },
                        primaryColor = phaseBorderStart,
                        secondaryColor = phaseBorderEnd,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_focus_button")
                    )

                    if (timerPhase != TimerPhase.IDLE) {
                        SystemHudButton(
                            text = "[ END / CLAIM ]",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAbortTimer()
                            },
                            primaryColor = PenaltyDangerRed,
                            secondaryColor = MonarchShadowPurple,
                            modifier = Modifier.testTag("abort_timer_button")
                        )
                    }
                }

                // Test Shortcut: Instant Clear
                SystemHudButton(
                    text = "[ TEST INSTANT CLEAR (00:00 -> +$expectedTokens TOKENS & XP) ]",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onInstantClearQuest()
                    },
                    primaryColor = SovereignRewardGold,
                    secondaryColor = SystemManaCyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("instant_clear_quest_button")
                )
            }
        }

        // ---------------------------------------------------------
        // CARD 2: FOCUS DURATION SELECTORS & TOKEN REWARD FORMULA
        // ---------------------------------------------------------
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("duration_selectors_card"),
            borderStartColor = SystemManaCyan,
            borderEndColor = RoyalSystemBlue,
            contentPadding = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Token,
                            contentDescription = "Token Reward",
                            tint = SovereignRewardGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[ FOCUS DURATION & TOKEN REWARD ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "CLEARED: $completedDungeons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MonarchShadowPurple,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "Ratio: 35m Focus = 100 Reward Tokens = 10m Unsealed App Time (10 Tokens = 1 min). Minimum 5m Focus threshold required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextSecondary
                )

                // Row 1 Duration Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    focusRow1.forEach { mins ->
                        val tokens = FocusRepository.calculateTokensForFocusMinutes(mins)
                        SystemDurationChip(
                            label = "${mins}m",
                            subLabel = "+${tokens}T",
                            isSelected = focusDurationMinutes == mins,
                            enabled = timerPhase == TimerPhase.IDLE,
                            activeColor = SystemManaCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFocusDuration(mins)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2 Duration Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    focusRow2.forEach { mins ->
                        val tokens = FocusRepository.calculateTokensForFocusMinutes(mins)
                        SystemDurationChip(
                            label = "${mins}m",
                            subLabel = "+${tokens}T",
                            isSelected = focusDurationMinutes == mins,
                            enabled = timerPhase == TimerPhase.IDLE,
                            activeColor = if (mins == 35) SovereignRewardGold else SystemManaCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFocusDuration(mins)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // CARD 3: TIMER-QUEST BINDING CARD
        // ---------------------------------------------------------
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("timer_quest_binding_card"),
            borderStartColor = if (boundQuest != null) SovereignRewardGold else MonarchShadowPurple,
            borderEndColor = SystemManaCyan,
            contentPadding = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (boundQuest != null) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Bind Quest",
                            tint = if (boundQuest != null) SovereignRewardGold else MonarchShadowPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "[ BIND ACTIVE DAILY QUEST ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (boundQuest != null) SovereignRewardGold else MonarchShadowPurple,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (boundQuest != null) {
                        Text(
                            text = "BOUND (+${boundQuest.xpReward} XP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SovereignRewardGold,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = "Bind an active Daily Quest to this timer. Reaching 00:00 will auto-complete the quest, grant its XP, and deposit your Reward Tokens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextSecondary
                )

                // Unbound Option
                QuestBindOptionRow(
                    title = "[ NONE — FREE FOCUS DUNGEON ]",
                    subtitle = "Earn +$expectedTokens Reward Tokens & +100 XP without binding a specific quest",
                    isSelected = boundQuest == null,
                    accentColor = SystemManaCyan,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBindQuestToTimer(null)
                    },
                    testTag = "bind_quest_none"
                )

                if (incompleteQuests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                            .background(AbyssalBackground.copy(alpha = 0.75f))
                            .border(
                                width = 1.dp,
                                color = HudTextMuted.copy(alpha = 0.4f),
                                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "No incomplete Daily Quests found today. Add one in [ DAILY QUESTS ] to bind it here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudTextMuted
                        )
                    }
                } else {
                    incompleteQuests.forEach { quest ->
                        QuestBindOptionRow(
                            title = quest.title,
                            subtitle = "Auto-completes at 00:00 -> +${quest.xpReward} XP + $expectedTokens Tokens",
                            isSelected = boundQuest?.id == quest.id,
                            accentColor = SovereignRewardGold,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBindQuestToTimer(quest.id)
                            },
                            testTag = "bind_quest_${quest.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestBindOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    testTag: String
) {
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(
                if (isSelected) accentColor.copy(alpha = 0.16f)
                else SystemWindowSurfaceElevated.copy(alpha = 0.75f),
                shape
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else HudTextMuted.copy(alpha = 0.4f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isSelected) accentColor else HudTextPrimary,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = HudTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Link,
            contentDescription = if (isSelected) "Bound" else "Bind Quest",
            tint = if (isSelected) accentColor else HudTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
