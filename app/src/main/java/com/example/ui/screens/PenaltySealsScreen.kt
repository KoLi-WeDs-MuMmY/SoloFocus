package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Token
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.data.FocusRepository
import com.example.ui.InstalledAppItem
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
fun PenaltySealsScreen(
    isAccessibilityEnabled: Boolean,
    canDrawOverlays: Boolean,
    hasNotificationPermission: Boolean,
    rewardTokens: Int,
    unsealedAppsExpiryMap: Map<String, Long>,
    currentTimeMillis: Long,
    isBlockingActive: Boolean,
    forceDungeonLockTestMode: Boolean,
    sealYoutubeShorts: Boolean,
    sealInstagramReels: Boolean,
    blockedPackages: Set<String>,
    installedApps: List<InstalledAppItem>,
    searchQuery: String,
    lastBlockedEventDescription: String?,
    onRefreshAccessibilityStatus: () -> Unit,
    onRequestNotificationPermissionAndSendBar: () -> Unit,
    onOpenPermissionSetupDialog: () -> Unit,
    onRedeemTokensForApp: (String, String, Int) -> Boolean,
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

    val sealedAppItems = installedApps.filter { blockedPackages.contains(it.packageName) }
    var selectedExchangePkg by rememberSaveable(sealedAppItems.map { it.packageName }) {
        mutableStateOf(sealedAppItems.firstOrNull()?.packageName.orEmpty())
    }
    var selectedRedemptionMinutes by rememberSaveable { mutableStateOf(10) }

    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val allAuthoritiesGranted = isAccessibilityEnabled && canDrawOverlays && hasNotificationPermission

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("penalty_seals_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Glowing [ SYSTEM AUTHORITY: ACCESSIBILITY, OVERLAY & NOTIFICATIONS ] Status Card
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_authority_card"),
                borderStartColor = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                borderEndColor = if (allAuthoritiesGranted) RoyalSystemBlue else MonarchShadowPurple,
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
                                imageVector = if (allAuthoritiesGranted) {
                                    Icons.Default.GppGood
                                } else {
                                    Icons.Default.GppBad
                                },
                                contentDescription = "Accessibility Authority Status",
                                tint = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.system_authority_header),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = buildString {
                            append("NOTIFICATIONS: ")
                            append(if (hasNotificationPermission) "ONLINE" else "LOCKED")
                            append(" • OVERLAY: ")
                            append(if (canDrawOverlays) "ONLINE" else "LOCKED")
                            append(" • ACCESSIBILITY: ")
                            append(if (isAccessibilityEnabled) "ONLINE" else "LOCKED")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "On Android 13+, side-loaded APKs lock Accessibility & Overlay behind 'Restricted settings'. Tap [ 1. NOTIFICATION ACCESS BAR ] and [ 2. UNLOCK RESTRICTED SETTINGS (APP INFO) ] (top-right ⋮ -> 'Allow restricted settings'), then enable Overlay and Accessibility.",
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
                        text = if (hasNotificationPermission) {
                            "[ 1. SEND PERMISSION ACCESS NOTIFICATION ]"
                        } else {
                            "[ 1. GRANT NOTIFICATIONS FOR PERMISSION ACCESS ]"
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRequestNotificationPermissionAndSendBar()
                        },
                        primaryColor = if (hasNotificationPermission) SystemManaCyan else SovereignRewardGold,
                        secondaryColor = RoyalSystemBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("notification_permission_button")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SystemHudButton(
                        text = "[ 2. UNLOCK RESTRICTED SETTINGS (APP INFO ⋮) ]",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        primaryColor = MonarchShadowPurple,
                        secondaryColor = SystemManaCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unlock_restricted_settings_button")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SystemHudButton(
                        text = if (canDrawOverlays) {
                            "[ 3. DISPLAY OVER OTHER APPS: ONLINE ]"
                        } else {
                            "[ 3. GRANT DISPLAY OVER OTHER APPS ]"
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        primaryColor = if (canDrawOverlays) SystemManaCyan else SovereignRewardGold,
                        secondaryColor = MonarchShadowPurple,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("overlay_settings_button")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SystemHudButton(
                        text = if (isAccessibilityEnabled) {
                            "[ 4. ACCESSIBILITY SERVICE: ONLINE ]"
                        } else {
                            "[ 4. GRANT ACCESSIBILITY SERVICE ]"
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

                    Spacer(modifier = Modifier.height(8.dp))

                    SystemHudButton(
                        text = "[ OPEN FULL PERMISSION CHECKLIST MODAL ]",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenPermissionSetupDialog()
                        },
                        primaryColor = SystemManaCyan,
                        secondaryColor = RoyalSystemBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_permission_modal_button")
                    )
                }
            }
        }

        // 2. [ TOKEN EXCHANGE: PER-APP TEMPORARY UNSEAL ] System Window
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("token_exchange_card"),
                borderStartColor = SovereignRewardGold,
                borderEndColor = SystemManaCyan,
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
                                contentDescription = "Token Exchange",
                                tint = SovereignRewardGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ TOKEN EXCHANGE: APP UNSEAL ]",
                                style = MaterialTheme.typography.titleMedium,
                                color = SovereignRewardGold,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "VAULT: $rewardTokens TOKENS",
                            style = MaterialTheme.typography.labelLarge,
                            color = SovereignRewardGold,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "Fixed Exchange Rate: 10 Tokens = 1 Minute (100T = 10m up to 600T = 60m). Unseals ONLY the selected app while all other sealed apps remain locked. Repeat transactions stack with no cap.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )

                    if (sealedAppItems.isEmpty()) {
                        Text(
                            text = "No apps are currently sealed. Seal apps below to manage per-app Token passes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HudTextMuted
                        )
                    } else {
                        Text(
                            text = "1. SELECT SEALED TARGET APP TO UNSEAL:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sealedAppItems.forEach { app ->
                                val isTargetSelected = selectedExchangePkg == app.packageName
                                val expiryMs = unsealedAppsExpiryMap[app.packageName] ?: 0L
                                val remainingMs = (expiryMs - currentTimeMillis).coerceAtLeast(0L)
                                val isCurrentlyUnsealed = remainingMs > 0L
                                val remMins = (remainingMs / 60_000L).toInt()
                                val remSecs = ((remainingMs % 60_000L) / 1000L).toInt()

                                val rowShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(rowShape)
                                        .background(
                                            when {
                                                isCurrentlyUnsealed -> SovereignRewardGold.copy(alpha = 0.16f)
                                                isTargetSelected -> SystemManaCyan.copy(alpha = 0.16f)
                                                else -> AbyssalBackground.copy(alpha = 0.8f)
                                            },
                                            rowShape
                                        )
                                        .border(
                                            width = if (isTargetSelected || isCurrentlyUnsealed) 1.5.dp else 1.dp,
                                            color = when {
                                                isCurrentlyUnsealed -> SovereignRewardGold
                                                isTargetSelected -> SystemManaCyan
                                                else -> HudTextMuted.copy(alpha = 0.4f)
                                            },
                                            shape = rowShape
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedExchangePkg = app.packageName
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("exchange_select_app_${app.packageName}"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.appName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isTargetSelected) SystemManaCyan else HudTextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HudTextSecondary
                                        )
                                    }

                                    Text(
                                        text = if (isCurrentlyUnsealed) {
                                            "UNSEALED (%02d:%02d)".format(remMins, remSecs)
                                        } else {
                                            "SEALED"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCurrentlyUnsealed) SovereignRewardGold else PenaltyDangerRed,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "2. SELECT FIXED PASS DURATION (10m MULTIPLES UP TO 60m):",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Bold
                        )

                        val tiersRow1 = FocusRepository.TOKEN_REDEMPTION_TIERS.take(3)
                        val tiersRow2 = FocusRepository.TOKEN_REDEMPTION_TIERS.drop(3)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tiersRow1.forEach { tier ->
                                SystemDurationChip(
                                    label = "${tier.minutes}m",
                                    subLabel = "${tier.tokenCost} TOKENS",
                                    isSelected = selectedRedemptionMinutes == tier.minutes,
                                    enabled = true,
                                    activeColor = SovereignRewardGold,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedRedemptionMinutes = tier.minutes
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("token_tier_${tier.minutes}m")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tiersRow2.forEach { tier ->
                                SystemDurationChip(
                                    label = "${tier.minutes}m",
                                    subLabel = "${tier.tokenCost} TOKENS",
                                    isSelected = selectedRedemptionMinutes == tier.minutes,
                                    enabled = true,
                                    activeColor = SovereignRewardGold,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedRedemptionMinutes = tier.minutes
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("token_tier_${tier.minutes}m")
                                )
                            }
                        }

                        val targetAppObj = sealedAppItems.find { it.packageName == selectedExchangePkg }
                            ?: sealedAppItems.first()
                        val requiredCost = selectedRedemptionMinutes * FocusRepository.TOKENS_PER_APP_MINUTE
                        val canAfford = rewardTokens >= requiredCost

                        SystemHudButton(
                            text = if (canAfford) {
                                "[ UNSEAL ${targetAppObj.appName.uppercase()} FOR ${selectedRedemptionMinutes}m (-$requiredCost TOKENS) ]"
                            } else {
                                "[ NEED $requiredCost TOKENS TO UNSEAL ${targetAppObj.appName.uppercase()} (${selectedRedemptionMinutes}m) ]"
                            },
                            enabled = canAfford,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRedeemTokensForApp(
                                    targetAppObj.packageName,
                                    targetAppObj.appName,
                                    selectedRedemptionMinutes
                                )
                            },
                            primaryColor = SovereignRewardGold,
                            secondaryColor = SystemManaCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("redeem_tokens_unseal_button")
                        )
                    }
                }
            }
        }

        // 3. Short-Form Video & [ FORCE DUNGEON LOCK (TEST MODE) ] Seals Card
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

                    SystemSealToggleRow(
                        title = "[ FORCE DUNGEON LOCK (TEST MODE) ]",
                        subtitle = "Immediately arm Dungeon Lock to test the 3-second Crimson Overlay kick-out right now.",
                        checked = forceDungeonLockTestMode || isBlockingActive,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleForceDungeonLockTestMode(it)
                        },
                        testTag = "force_dungeon_lock_switch"
                    )

                    SystemSealToggleRow(
                        title = stringResource(id = R.string.seal_youtube_shorts),
                        subtitle = "Detects 'reel_player' / 'shorts_' and triggers GLOBAL_ACTION_BACK + Crimson Overlay.",
                        checked = sealYoutubeShorts,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSealYoutubeShorts(it)
                        },
                        testTag = "seal_youtube_shorts_switch"
                    )

                    SystemSealToggleRow(
                        title = stringResource(id = R.string.seal_instagram_reels),
                        subtitle = "Detects 'clips_viewer' / 'reel_viewer' and triggers GLOBAL_ACTION_BACK + Crimson Overlay.",
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

        // 4. Installed-App Blocker List Header & Search
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
                        text = "Sealed apps are blocked during Dungeon Lock unless individually unsealed in the Token Exchange above.",
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

        // 5. Installed-App Blocker Rows
        items(items = filteredApps, key = { it.packageName }) { appItem ->
            val isSealed = blockedPackages.contains(appItem.packageName)
            val expiryMs = unsealedAppsExpiryMap[appItem.packageName] ?: 0L
            val remainingMs = (expiryMs - currentTimeMillis).coerceAtLeast(0L)
            val isTemporarilyUnsealed = isSealed && remainingMs > 0L
            val remMins = (remainingMs / 60_000L).toInt()
            val remSecs = ((remainingMs % 60_000L) / 1000L).toInt()

            val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(
                        when {
                            isTemporarilyUnsealed -> SovereignRewardGold.copy(alpha = 0.14f)
                            isSealed -> PenaltyDangerRed.copy(alpha = 0.12f)
                            else -> SystemWindowSurfaceElevated.copy(alpha = 0.85f)
                        },
                        shape
                    )
                    .border(
                        width = 1.2.dp,
                        color = when {
                            isTemporarilyUnsealed -> SovereignRewardGold
                            isSealed -> PenaltyDangerRed
                            else -> RoyalSystemBlue.copy(alpha = 0.45f)
                        },
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
                                color = when {
                                    isTemporarilyUnsealed -> SovereignRewardGold
                                    isSealed -> PenaltyDangerRed
                                    else -> SystemManaCyan
                                },
                                shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSealed && !isTemporarilyUnsealed) {
                                Icons.Default.Lock
                            } else {
                                Icons.Default.LockOpen
                            },
                            contentDescription = if (isSealed) "App Sealed" else "App Unsealed",
                            tint = when {
                                isTemporarilyUnsealed -> SovereignRewardGold
                                isSealed -> PenaltyDangerRed
                                else -> SystemManaCyan
                            },
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
                            text = if (isTemporarilyUnsealed) {
                                "TOKEN PASS ACTIVE: %02d:%02d REMAINING".format(remMins, remSecs)
                            } else {
                                appItem.packageName
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isTemporarilyUnsealed) SovereignRewardGold else HudTextSecondary
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
