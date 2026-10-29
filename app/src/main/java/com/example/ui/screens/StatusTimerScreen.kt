package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TimerPhase
import com.example.ui.components.ManaCoreTimerDial
import com.example.ui.components.SystemDurationChip
import com.example.ui.components.SystemHudButton
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MonarchShadowPurple
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SovereignRewardGold
import com.example.ui.theme.SystemManaCyan

@Composable
fun StatusTimerScreen(
    focusDurationMinutes: Int,
    rewardDurationMinutes: Int,
    timerPhase: TimerPhase,
    remainingSeconds: Int,
    totalPhaseSeconds: Int,
    isTimerRunning: Boolean,
    isBlockingActive: Boolean,
    completedDungeons: Int,
    onSelectFocusDuration: (Int) -> Unit,
    onSelectRewardDuration: (Int) -> Unit,
    onStartOrToggleTimer: () -> Unit,
    onAbortTimer: () -> Unit,
    onInstantClearQuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusOptions = listOf(25, 45, 60, 90)
    val rewardOptions = listOf(10, 15, 30)

    val phaseBorderStart = when {
        timerPhase == TimerPhase.REWARD_UNLOCK -> SovereignRewardGold
        timerPhase == TimerPhase.FOCUS_LOCK || isBlockingActive -> PenaltyDangerRed
        else -> SystemManaCyan
    }
    val phaseBorderEnd = when {
        timerPhase == TimerPhase.REWARD_UNLOCK -> SystemManaCyan
        timerPhase == TimerPhase.FOCUS_LOCK || isBlockingActive -> MonarchShadowPurple
        else -> RoyalSystemBlue
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Main Mana Core Timer System Window
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // System Header Bar inside the Timer Window
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (timerPhase) {
                            TimerPhase.FOCUS_LOCK -> "[ DUNGEON LOCK: ACTIVE ]"
                            TimerPhase.REWARD_UNLOCK -> "[ QUEST CLEARED: REWARD TIME UNLOCKED ]"
                            TimerPhase.IDLE -> "[ ! SYSTEM NOTIFICATION: FOCUS GATE ]"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = phaseBorderStart,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
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
                        Icon(
                            imageVector = if (timerPhase == TimerPhase.REWARD_UNLOCK) {
                                Icons.Default.LockOpen
                            } else {
                                Icons.Default.Lock
                            },
                            contentDescription = "Seal Lock Status",
                            tint = phaseBorderStart,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (timerPhase) {
                                TimerPhase.FOCUS_LOCK -> "SEALED"
                                TimerPhase.REWARD_UNLOCK -> "UNSEALED"
                                TimerPhase.IDLE -> if (isBlockingActive) "SEALED" else "STANDBY"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = phaseBorderStart,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Centerpiece Custom Canvas Mana Core Timer
                ManaCoreTimerDial(
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalPhaseSeconds,
                    timerPhase = timerPhase,
                    isTimerRunning = isTimerRunning,
                    isBlockingActive = isBlockingActive,
                    modifier = Modifier
                        .size(256.dp)
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val primaryButtonLabel = when {
                        timerPhase == TimerPhase.IDLE -> "[ ENTER DUNGEON ]"
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
                            text = "[ ABORT ]",
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

                Spacer(modifier = Modifier.height(10.dp))

                // Instant Clear / Boss Raid Complete Action (triggers 00:00 transition & +100 XP)
                SystemHudButton(
                    text = if (timerPhase == TimerPhase.REWARD_UNLOCK) {
                        "[ END REWARD WINDOW EARLY ]"
                    } else {
                        "[ INSTANT CLEAR QUEST (00:00 -> +100 XP) ]"
                    },
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

        // Duration Selectors System Window
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("duration_selectors_card"),
            borderStartColor = SystemManaCyan,
            borderEndColor = RoyalSystemBlue,
            contentPadding = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ FOCUS QUEST TIME ]",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemManaCyan,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "CLEARED GATES: $completedDungeons",
                        style = MaterialTheme.typography.labelSmall,
                        color = MonarchShadowPurple,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Locks distracting apps & short-form feeds. Grants +100 XP upon reaching 00:00.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    focusOptions.forEach { mins ->
                        SystemDurationChip(
                            label = "${mins}m",
                            isSelected = focusDurationMinutes == mins,
                            enabled = timerPhase == TimerPhase.IDLE,
                            activeColor = SystemManaCyan,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFocusDuration(mins)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("focus_duration_${mins}m")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Reward Time",
                        tint = SovereignRewardGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[ CLEARED REWARD TIME ]",
                        style = MaterialTheme.typography.titleMedium,
                        color = SovereignRewardGold,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically unseals blocked apps when Focus Timer hits 00:00.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rewardOptions.forEach { mins ->
                        SystemDurationChip(
                            label = "${mins}m",
                            isSelected = rewardDurationMinutes == mins,
                            enabled = timerPhase != TimerPhase.REWARD_UNLOCK,
                            activeColor = SovereignRewardGold,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectRewardDuration(mins)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reward_duration_${mins}m")
                        )
                    }
                }
            }
        }
    }
}
