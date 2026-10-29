package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DailyQuest
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
fun DailyQuestsScreen(
    quests: List<DailyQuest>,
    onToggleQuest: (String) -> Unit,
    onAddQuest: (String, String) -> Unit,
    onDeleteQuest: (String) -> Unit,
    onResetDailyQuests: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var newQuestTitle by rememberSaveable { mutableStateOf("") }
    var newQuestDetail by rememberSaveable { mutableStateOf("") }

    val completedCount = quests.count { it.isCompleted }
    val totalCount = quests.size
    val allCleared = totalCount > 0 && completedCount == totalCount

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Solo Leveling [ ! QUEST INFO ] Popup Window Frame
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("daily_quests_info_window"),
            borderStartColor = if (allCleared) SovereignRewardGold else SystemManaCyan,
            borderEndColor = if (allCleared) SystemManaCyan else RoyalSystemBlue,
            contentPadding = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header: [ ! QUEST INFO ] & [ DAILY QUEST: THE PLAYER'S PREPARATION ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "[ ! QUEST INFO ]",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (allCleared) SovereignRewardGold else SystemManaCyan,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.daily_quest_header),
                            style = MaterialTheme.typography.titleMedium,
                            color = HudTextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetDailyQuests()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reset_daily_quests_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Daily Quests",
                            tint = SystemManaCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Goal Progress Indicator Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .background(AbyssalBackground.copy(alpha = 0.85f))
                        .border(
                            width = 1.dp,
                            color = if (allCleared) SovereignRewardGold else SystemManaCyan.copy(alpha = 0.5f),
                            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GOAL COMPLETION STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = HudTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (allCleared) {
                            "[ ALL CLEARED: $completedCount/$totalCount ]"
                        } else {
                            "[ INCOMPLETE: $completedCount/$totalCount ]"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allCleared) SovereignRewardGold else SystemManaCyan,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Custom Study / Work Goal Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newQuestTitle,
                        onValueChange = { newQuestTitle = it },
                        placeholder = {
                            Text(
                                text = "Assign study / work quest (+50 XP)...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudTextMuted
                            )
                        },
                        singleLine = true,
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemManaCyan,
                            unfocusedBorderColor = RoyalSystemBlue.copy(alpha = 0.6f),
                            focusedContainerColor = AbyssalBackground,
                            unfocusedContainerColor = AbyssalBackground,
                            focusedTextColor = HudTextPrimary,
                            unfocusedTextColor = HudTextPrimary,
                            cursorColor = SystemManaCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_quest_input")
                    )

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(52.dp)
                            .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                            .background(SystemManaCyan.copy(alpha = 0.2f))
                            .border(
                                width = 1.5.dp,
                                color = SystemManaCyan,
                                shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                            )
                            .clickable {
                                if (newQuestTitle.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onAddQuest(newQuestTitle, newQuestDetail)
                                    newQuestTitle = ""
                                    newQuestDetail = ""
                                }
                            }
                            .testTag("add_quest_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Daily Quest",
                            tint = SystemManaCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Quests List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("daily_quests_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = quests, key = { it.id }) { quest ->
                        DailyQuestItemRow(
                            quest = quest,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleQuest(quest.id)
                            },
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDeleteQuest(quest.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mandatory Bottom System Warning Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp))
                        .background(PenaltyDangerRed.copy(alpha = 0.14f))
                        .border(
                            width = 1.5.dp,
                            color = PenaltyDangerRed,
                            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
                        )
                        .padding(12.dp)
                        .testTag("daily_quest_warning_box")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Penalty Warning",
                            tint = PenaltyDangerRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.daily_quest_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PenaltyDangerRed,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestItemRow(
    quest: DailyQuest,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (quest.isCompleted) 0.985f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quest_row_spring"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (quest.isCompleted) 1.12f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quest_check_spring"
    )

    val borderColor by animateColorAsState(
        targetValue = if (quest.isCompleted) SovereignRewardGold else SystemManaCyan.copy(alpha = 0.6f),
        label = "quest_border_color"
    )

    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shape)
            .background(
                if (quest.isCompleted) {
                    AbyssalBackground.copy(alpha = 0.9f)
                } else {
                    SystemWindowSurfaceElevated.copy(alpha = 0.85f)
                },
                shape
            )
            .border(width = 1.2.dp, color = borderColor, shape = shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("quest_item_${quest.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Chamfered System Checkbox
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(28.dp)
                    .scale(checkScale)
                    .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                    .background(
                        if (quest.isCompleted) {
                            SovereignRewardGold.copy(alpha = 0.25f)
                        } else {
                            AbyssalBackground
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (quest.isCompleted) SovereignRewardGold else SystemManaCyan,
                        shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (quest.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Quest Completed",
                        tint = SovereignRewardGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (quest.isCompleted) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        }
                    ),
                    color = if (quest.isCompleted) HudTextSecondary else HudTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (quest.isCompleted) "[ CLEARED ]" else "[ INCOMPLETE ]",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quest.isCompleted) SovereignRewardGold else SystemManaCyan,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "+${quest.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MonarchShadowPurple,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("delete_quest_${quest.id}")
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete Quest",
                tint = HudTextMuted
            )
        }
    }
}
