package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.InstalledAppItem
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
fun PenaltySealsScreen(
    isAccessibilityEnabled: Boolean,
    isBlockingActive: Boolean,
    forceDungeonLockTestMode: Boolean,
    sealYoutubeShorts: Boolean,
    sealInstagramReels: Boolean,
    blockedPackages: Set<String>,
    installedApps: List<InstalledAppItem>,
    searchQuery: String,
    lastBlockedEventDescription: String?,
    onRefreshAccessibilityStatus: () -> Unit,
    onToggleForceDungeonLockTestMode: (Boolean) -> Unit,
    onToggleSealYoutubeShorts: (Boolean) -> Unit,
    onToggleSealInstagramReels: (Boolean) -> Unit,
    onToggleBlockedPackage: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("penalty_seals_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Glowing [ SYSTEM AUTHORITY: ACCESSIBILITY SERVICE ] Status Card
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_authority_card"),
                borderStartColor = if (isAccessibilityEnabled) SystemManaCyan else PenaltyDangerRed,
                borderEndColor = if (isAccessibilityEnabled) RoyalSystemBlue else MonarchShadowPurple,
                contentPadding = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isAccessibilityEnabled) {
                                    Icons.Default.GppGood
                                } else {
                                    Icons.Default.GppBad
                                },
                                contentDescription = "Accessibility Authority Status",
                                tint = if (isAccessibilityEnabled) SystemManaCyan else PenaltyDangerRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.system_authority_header),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAccessibilityEnabled) SystemManaCyan else PenaltyDangerRed,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isAccessibilityEnabled) {
                            "STATUS: ONLINE • FocusBlockerService has active System Authority to enforce app & Shorts/Reels seals even when the app UI is closed."
                        } else {
                            "STATUS: OFFLINE • Enable 'System Authority: Focus & Shorts Blocker' in Android Accessibility Settings to enforce real app and Shorts/Reels blocking."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextPrimary
                    )

                    if (!lastBlockedEventDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LAST INTERCEPTION: $lastBlockedEventDescription",
                            style = MaterialTheme.typography.labelSmall,
                            color = SovereignRewardGold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SystemHudButton(
                        text = if (isAccessibilityEnabled) {
                            "[ CONFIGURE ACCESSIBILITY AUTHORITY ]"
                        } else {
                            "[ GRANT SYSTEM AUTHORITY (OPEN SETTINGS) ]"
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        primaryColor = if (isAccessibilityEnabled) SystemManaCyan else PenaltyDangerRed,
                        secondaryColor = RoyalSystemBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accessibility_settings_button")
                    )
                }
            }
        }

        // 2. Short-Form Video & [ FORCE DUNGEON LOCK (TEST MODE) ] Seals Card
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("short_form_seals_card"),
                borderStartColor = PenaltyDangerRed,
                borderEndColor = MonarchShadowPurple,
                contentPadding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Short-Form Video Seals",
                            tint = PenaltyDangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ SHORT-FORM FEED & DUNGEON SEALS ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = PenaltyDangerRed,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // [ FORCE DUNGEON LOCK (TEST MODE) ]
                    SystemSealToggleRow(
                        title = "[ FORCE DUNGEON LOCK (TEST MODE) ]",
                        subtitle = "Immediately lock all sealed apps and Shorts/Reels right now without waiting for the timer.",
                        checked = forceDungeonLockTestMode || isBlockingActive,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleForceDungeonLockTestMode(it)
                        },
                        testTag = "force_dungeon_lock_switch"
                    )

                    // [ SEAL YOUTUBE SHORTS ]
                    SystemSealToggleRow(
                        title = stringResource(id = R.string.seal_youtube_shorts),
                        subtitle = "Multi-window node scanner detects 'reel_player', 'shorts_', or selected 'Shorts' tab and triggers GLOBAL_ACTION_BACK.",
                        checked = sealYoutubeShorts,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSealYoutubeShorts(it)
                        },
                        testTag = "seal_youtube_shorts_switch"
                    )

                    // [ SEAL INSTAGRAM REELS ]
                    SystemSealToggleRow(
                        title = stringResource(id = R.string.seal_instagram_reels),
                        subtitle = "Multi-window node scanner detects 'clips_viewer', 'reel_viewer', or active 'Reels' tab and triggers GLOBAL_ACTION_BACK.",
                        checked = sealInstagramReels,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSealInstagramReels(it)
                        },
                        testTag = "seal_instagram_reels_switch"
                    )
                }
            }
        }

        // 3. Installed-App Blocker List Header & Search
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("installed_apps_header_card"),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Sealed Apps",
                                tint = SystemManaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ INSTALLED APP PENALTY SEALS ]",
                                style = MaterialTheme.typography.titleMedium,
                                color = SystemManaCyan,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "${blockedPackages.size} SEALED",
                            style = MaterialTheme.typography.labelSmall,
                            color = PenaltyDangerRed,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sealed apps trigger GLOBAL_ACTION_HOME + '[ ! SYSTEM PENALTY: ACCESS DENIED UNTIL FOCUS COMPLETE ]' Toast when opened while locked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Installed Apps",
                                tint = SystemManaCyan
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Filter installed apps or package names...",
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
                            .fillMaxWidth()
                            .testTag("installed_apps_search_input")
                    )
                }
            }
        }

        // 4. Installed-App Blocker Rows
        items(items = filteredApps, key = { it.packageName }) { appItem ->
            val isSealed = blockedPackages.contains(appItem.packageName)
            val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        if (isSealed) {
                            PenaltyDangerRed.copy(alpha = 0.12f)
                        } else {
                            SystemWindowSurfaceElevated.copy(alpha = 0.85f)
                        },
                        shape
                    )
                    .border(
                        width = 1.2.dp,
                        color = if (isSealed) PenaltyDangerRed else RoyalSystemBlue.copy(alpha = 0.45f),
                        shape = shape
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleBlockedPackage(appItem.packageName)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("app_seal_row_${appItem.packageName}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                            .background(AbyssalBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSealed) PenaltyDangerRed else SystemManaCyan,
                                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSealed) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isSealed) "App Sealed" else "App Unsealed",
                            tint = if (isSealed) PenaltyDangerRed else SystemManaCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appItem.appName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = HudTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appItem.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudTextSecondary
                        )
                    }
                }

                Switch(
                    checked = isSealed,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggleBlockedPackage(appItem.packageName)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AbyssalBackground,
                        checkedTrackColor = PenaltyDangerRed,
                        uncheckedThumbColor = SystemManaCyan,
                        uncheckedTrackColor = AbyssalBackground,
                        uncheckedBorderColor = SystemManaCyan
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("app_seal_switch_${appItem.packageName}")
                )
            }
        }
    }
}

@Composable
private fun SystemSealToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AbyssalBackground.copy(alpha = 0.85f), shape)
            .border(
                width = 1.2.dp,
                color = if (checked) PenaltyDangerRed else RoyalSystemBlue.copy(alpha = 0.5f),
                shape = shape
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (checked) PenaltyDangerRed else SystemManaCyan,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = HudTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AbyssalBackground,
                checkedTrackColor = PenaltyDangerRed,
                uncheckedThumbColor = SystemManaCyan,
                uncheckedTrackColor = AbyssalBackground,
                uncheckedBorderColor = SystemManaCyan
            ),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        )
    }
}
