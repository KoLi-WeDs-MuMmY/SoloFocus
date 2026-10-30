package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Key
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.ArbiterVerdictResult
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
    savedApiKey: String,
    isArbiterEvaluating: Boolean,
    latestArbiterVerdict: ArbiterVerdictResult?,
    onSaveApiKey: (String) -> Unit,
    onSubmitQuestToArbiter: (String) -> Unit,
    onDismissArbiterVerdict: () -> Unit,
    onToggleQuest: (String) -> Unit,
    onDeleteQuest: (String) -> Unit,
    onResetDailyQuests: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var newQuestTitle by rememberSaveable { mutableStateOf("") }
    var isApiKeyConfigExpanded by rememberSaveable { mutableStateOf(false) }
    var apiKeyInput by rememberSaveable(savedApiKey) { mutableStateOf(savedApiKey) }
    var showApiKeyPlaintext by rememberSaveable { mutableStateOf(false) }

    val completedCount = quests.count { it.isCompleted }
    val totalCount = quests.size
    val allCleared = totalCount > 0 && completedCount == totalCount

    // Glowing [ ! SYSTEM ARBITER VERDICT ] Popup Window
    if (latestArbiterVerdict != null) {
        SystemArbiterVerdictDialog(
            verdictResult = latestArbiterVerdict,
            onDismiss = onDismissArbiterVerdict
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Sleek Collapsible [ API KEY CONFIG ] Box at Top of [ DAILY QUESTS ]
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("api_key_config_box"),
            borderStartColor = if (savedApiKey.isNotBlank()) SystemManaCyan else MonarchShadowPurple,
            borderEndColor = RoyalSystemBlue,
            contentPadding = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isApiKeyConfigExpanded = !isApiKeyConfigExpanded
                        }
                        .testTag("api_key_config_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key Config",
                            tint = if (savedApiKey.isNotBlank()) SystemManaCyan else MonarchShadowPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ API KEY CONFIG ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (savedApiKey.isNotBlank()) SystemManaCyan else MonarchShadowPurple,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (savedApiKey.isNotBlank()) "• KEY LINKED" else "• OFFLINE FALLBACK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (savedApiKey.isNotBlank()) SovereignRewardGold else HudTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = if (isApiKeyConfigExpanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                        contentDescription = "Expand or Collapse API Key Config",
                        tint = SystemManaCyan
                    )
                }

                AnimatedVisibility(
                    visible = isApiKeyConfigExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Paste your free Gemini API Key for single-request System Arbiter judgments (gemini-2.5-flash). Stored strictly in local SharedPreferences.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudTextSecondary
                        )

                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            placeholder = {
                                Text(
                                    text = "AIzaSy... (Free Gemini API Key)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudTextMuted
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (showApiKeyPlaintext) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                Text(
                                    text = if (showApiKeyPlaintext) "HIDE" else "SHOW",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemManaCyan,
                                    modifier = Modifier
                                        .clickable { showApiKeyPlaintext = !showApiKeyPlaintext }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            },
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
                                .fillMaxWidth()
                                .testTag("gemini_api_key_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SystemHudButton(
                                text = "[ SAVE KEY TO SYSTEM ]",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSaveApiKey(apiKeyInput)
                                    isApiKeyConfigExpanded = false
                                },
                                primaryColor = SystemManaCyan,
                                secondaryColor = RoyalSystemBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_api_key_button")
                            )

                            if (apiKeyInput.isNotBlank()) {
                                SystemHudButton(
                                    text = "[ CLEAR ]",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        apiKeyInput = ""
                                        onSaveApiKey("")
                                    },
                                    primaryColor = PenaltyDangerRed,
                                    secondaryColor = MonarchShadowPurple
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Solo Leveling [ ! QUEST INFO ] Popup Window Frame
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

                // Single-Request Gemini System Arbiter Quest Input + Submit Button
                OutlinedTextField(
                    value = newQuestTitle,
                    onValueChange = { newQuestTitle = it },
                    placeholder = {
                        Text(
                            text = "Propose a study/work quest to the System Arbiter...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudTextMuted
                        )
                    },
                    singleLine = true,
                    enabled = !isArbiterEvaluating,
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
                        .fillMaxWidth()
                        .testTag("add_quest_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                SystemHudButton(
                    text = if (isArbiterEvaluating) {
                        "[ SYSTEM ARBITER JUDGING... ]"
                    } else {
                        "[ SUBMIT QUEST TO SYSTEM ]"
                    },
                    enabled = newQuestTitle.isNotBlank() && !isArbiterEvaluating,
                    onClick = {
                        if (newQuestTitle.isNotBlank() && !isArbiterEvaluating) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val submitted = newQuestTitle
                            newQuestTitle = ""
                            onSubmitQuestToArbiter(submitted)
                        }
                    },
                    primaryColor = SystemManaCyan,
                    secondaryColor = MonarchShadowPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_quest_to_system_button")
                )

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(10.dp))

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
                        .padding(10.dp)
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
                            modifier = Modifier.size(20.dp)
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
private fun SystemArbiterVerdictDialog(
    verdictResult: ArbiterVerdictResult,
    onDismiss: () -> Unit
) {
    // Cyan for COMPLY, Monarch Purple for MODIFY, Crimson Red for DECLINE
    val accentColor = when (verdictResult.verdict) {
        "COMPLY" -> SystemManaCyan
        "MODIFY" -> MonarchShadowPurple
        else -> PenaltyDangerRed
    }
    val secondaryColor = when (verdictResult.verdict) {
        "COMPLY" -> RoyalSystemBlue
        "MODIFY" -> SystemManaCyan
        else -> MonarchShadowPurple
    }

    Dialog(onDismissRequest = onDismiss) {
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("system_arbiter_verdict_dialog"),
            borderStartColor = accentColor,
            borderEndColor = secondaryColor,
            surfaceColor = AbyssalBackground.copy(alpha = 0.98f),
            contentPadding = 18.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (verdictResult.verdict == "DECLINE") {
                            Icons.Default.Gavel
                        } else {
                            Icons.Default.AutoAwesome
                        },
                        contentDescription = "System Arbiter Verdict",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "[ ! SYSTEM ARBITER VERDICT ]",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Black
                    )
                }

                // Verdict & XP Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(
                                width = 1.5.dp,
                                color = accentColor,
                                shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "VERDICT: ${verdictResult.verdict}",
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (verdictResult.verdict != "DECLINE") {
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                                .background(SovereignRewardGold.copy(alpha = 0.16f))
                                .border(
                                    width = 1.dp,
                                    color = SovereignRewardGold,
                                    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+${verdictResult.xpReward} XP REWARD",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignRewardGold,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                if (verdictResult.verdict == "MODIFY") {
                    Text(
                        text = "SUBMITTED: \"${verdictResult.submittedQuestTitle}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )
                }

                // Final Approved / Modified / Rejected Quest Title Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .background(SystemWindowSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.6f),
                            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = when (verdictResult.verdict) {
                                "COMPLY" -> "REGISTERED QUEST OBJECTIVE:"
                                "MODIFY" -> "ESCALATED SYSTEM OBJECTIVE:"
                                else -> "REJECTED INPUT:"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = verdictResult.finalQuestTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HudTextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Text(
                    text = verdictResult.systemMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HudTextPrimary
                )

                SystemHudButton(
                    text = if (verdictResult.verdict == "DECLINE") {
                        "[ ACKNOWLEDGE PENALTY ]"
                    } else {
                        "[ ACCEPT SYSTEM DIRECTIVE ]"
                    },
                    onClick = onDismiss,
                    primaryColor = accentColor,
                    secondaryColor = secondaryColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("arbiter_verdict_confirm_button")
                )
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
        targetValue = when {
            quest.isCompleted -> SovereignRewardGold
            quest.arbiterVerdict == "MODIFY" -> MonarchShadowPurple
            else -> SystemManaCyan.copy(alpha = 0.6f)
        },
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
                    if (quest.arbiterVerdict != null) {
                        Text(
                            text = "[ ARBITER: ${quest.arbiterVerdict} ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (quest.arbiterVerdict == "MODIFY") MonarchShadowPurple else SystemManaCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
