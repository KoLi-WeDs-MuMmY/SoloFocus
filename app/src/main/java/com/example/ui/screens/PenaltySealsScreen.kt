package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    hasUsageStatsPermission: Boolean,
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
    onRefreshPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
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
                onRefreshPermissions()
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
    var selectedRedemptionMinutes by rememberSaveable { mutableIntStateOf(10) }

    val filteredApps = if (searchQuery.isBlank()) {
        installedApps
    } else {
        installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val allAuthoritiesGranted = isAccessibilityEnabled && canDrawOverlays && hasNotificationPermission && hasUsageStatsPermission

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("penalty_seals_list"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------------------------------------------------------
        // CARD 1: PERMISSIONS & SYSTEM AUTHORITY SETUP
        // ---------------------------------------------------------
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("system_authority_card"),
                borderStartColor = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                borderEndColor = if (allAuthoritiesGranted) RoyalSystemBlue else MonarchShadowPurple,
                contentPadding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card Title Row
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
                                imageVector = Icons.Default.Shield,
                                contentDescription = "System Authority",
                                tint = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ SYSTEM AUTHORITY & PERMISSIONS ]",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Status Pill
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    if (allAuthoritiesGranted) SystemManaCyan.copy(alpha = 0.15f)
                                    else PenaltyDangerRed.copy(alpha = 0.15f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                    shape = CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (allAuthoritiesGranted) "ALL ONLINE" else "ACTION REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (allAuthoritiesGranted) SystemManaCyan else PenaltyDangerRed,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Text(
                        text = "Grant required Android permissions for real-time app interception, penalty HUD overlay, and foreground monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )

                    // 1. Accessibility Service Row
                    PermissionIndicatorRow(
                        icon = Icons.Default.Security,
                        title = "Accessibility Service",
                        subtitle = "Real-time app intercept & Shorts/Reels shield",
                        isGranted = isAccessibilityEnabled,
                        actionLabel = "ENABLE",
                        onClickAction = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        testTag = "enable_accessibility_button"
                    )

                    // 2. Display Over Other Apps (Overlay) Row
                    PermissionIndicatorRow(
                        icon = Icons.Default.Layers,
                        title = "Display Over Other Apps",
                        subtitle = "Full-screen penalty HUD over restricted apps",
                        isGranted = canDrawOverlays,
                        actionLabel = "ENABLE",
                        onClickAction = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        testTag = "enable_overlay_button"
                    )

                    // 3. Usage Access Row
                    PermissionIndicatorRow(
                        icon = Icons.Default.QueryStats,
                        title = "Usage Access",
                        subtitle = "Foreground application telemetry monitor",
                        isGranted = hasUsageStatsPermission,
                        actionLabel = "ENABLE",
                        onClickAction = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        testTag = "enable_usage_stats_button"
                    )

                    // 4. Notifications Row
                    PermissionIndicatorRow(
                        icon = Icons.Default.Notifications,
                        title = "Notification Authority",
                        subtitle = "Lock status bar & background alert updates",
                        isGranted = hasNotificationPermission,
                        actionLabel = "ENABLE",
                        onClickAction = onRequestNotificationPermission,
                        testTag = "enable_notifications_button"
                    )

                    // Android 13+ Restricted Settings Helper
                    if (!isAccessibilityEnabled || !canDrawOverlays) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                                .background(MonarchShadowPurple.copy(alpha = 0.12f))
                                .border(
                                    width = 1.dp,
                                    color = MonarchShadowPurple.copy(alpha = 0.4f),
                                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Android 13+ Note: If toggles are grayed out, open App Info -> tap top-right menu (⋮) -> 'Allow restricted settings'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HudTextPrimary
                                )
                                SystemHudButton(
                                    text = "[ OPEN APP INFO TO UNLOCK RESTRICTED SETTINGS ]",
                                    onClick = {
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
                                        .testTag("app_info_restricted_button")
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // CARD 2: FEED SHIELDS & TEST CONTROLS
        // ---------------------------------------------------------
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("content_shields_card"),
                borderStartColor = SystemManaCyan,
                borderEndColor = MonarchShadowPurple,
                contentPadding = 16.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Content Shields",
                            tint = SystemManaCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ SHORT-FORM FEED SHIELDS & TEST ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // YouTube Shorts Shield
                    FeedShieldToggleRow(
                        title = "YouTube Shorts Shield",
                        description = "Intercepts Shorts player and kicks back to feed",
                        isChecked = sealYoutubeShorts,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSealYoutubeShorts(it)
                        },
                        activeColor = SystemManaCyan,
                        testTag = "youtube_shorts_switch"
                    )

                    // Instagram Reels Shield
                    FeedShieldToggleRow(
                        title = "Instagram Reels Shield",
                        description = "Intercepts Reels player and kicks back to feed",
                        isChecked = sealInstagramReels,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleSealInstagramReels(it)
                        },
                        activeColor = MonarchShadowPurple,
                        testTag = "instagram_reels_switch"
                    )

                    // Force Dungeon Lock (Test Mode)
                    FeedShieldToggleRow(
                        title = "Force Dungeon Lock (Test Mode)",
                        description = "Enforces instant app blocking immediately without active timer",
                        isChecked = forceDungeonLockTestMode,
                        onCheckedChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleForceDungeonLockTestMode(it)
                        },
                        activeColor = PenaltyDangerRed,
                        testTag = "force_dungeon_lock_switch"
                    )
                }
            }
        }

        // ---------------------------------------------------------
        // CARD 3: REWARD TOKEN EXCHANGE TERMINAL
        // ---------------------------------------------------------
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
                                text = "[ REWARD TOKEN EXCHANGE ]",
                                style = MaterialTheme.typography.titleMedium,
                                color = SovereignRewardGold,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "VAULT: $rewardTokens TOKENS",
                            style = MaterialTheme.typography.labelMedium,
                            color = SovereignRewardGold,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "Exchange Rate: 10 Tokens = 1 Minute of App Usability. Unseals ONLY the selected app while all other sealed apps remain locked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )

                    if (sealedAppItems.isEmpty()) {
                        Text(
                            text = "No apps currently sealed. Toggle packages below to enable per-app Token passes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HudTextMuted
                        )
                    } else {
                        // App selection chips
                        Text(
                            text = "SELECT APP TO UNSEAL:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sealedAppItems.take(3).forEach { app ->
                                val isSelected = selectedExchangePkg == app.packageName
                                val shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(shape)
                                        .background(
                                            if (isSelected) SovereignRewardGold.copy(alpha = 0.2f)
                                            else SystemWindowSurfaceElevated
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) SovereignRewardGold else HudTextMuted.copy(alpha = 0.3f),
                                            shape = shape
                                        )
                                        .clickable { selectedExchangePkg = app.packageName }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) SovereignRewardGold else HudTextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Duration Tier Chips
                        Text(
                            text = "SELECT UNSEAL DURATION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FocusRepository.TOKEN_REDEMPTION_TIERS.take(4).forEach { tier ->
                                SystemDurationChip(
                                    label = "${tier.minutes}m",
                                    subLabel = "${tier.tokenCost}T",
                                    isSelected = selectedRedemptionMinutes == tier.minutes,
                                    enabled = true,
                                    activeColor = SovereignRewardGold,
                                    onClick = { selectedRedemptionMinutes = tier.minutes },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        val selectedTier = FocusRepository.TOKEN_REDEMPTION_TIERS.find { it.minutes == selectedRedemptionMinutes }
                            ?: FocusRepository.TOKEN_REDEMPTION_TIERS.first()
                        val canAfford = rewardTokens >= selectedTier.tokenCost

                        SystemHudButton(
                            text = if (canAfford) {
                                "[ UNSEAL FOR ${selectedTier.minutes}m (${selectedTier.tokenCost} TOKENS) ]"
                            } else {
                                "[ INSUFFICIENT TOKENS (NEED ${selectedTier.tokenCost}T) ]"
                            },
                            onClick = {
                                if (selectedExchangePkg.isNotBlank() && canAfford) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val targetApp = sealedAppItems.find { it.packageName == selectedExchangePkg }
                                    onRedeemTokensForApp(
                                        selectedExchangePkg,
                                        targetApp?.appName ?: selectedExchangePkg,
                                        selectedTier.minutes
                                    )
                                }
                            },
                            primaryColor = if (canAfford) SovereignRewardGold else HudTextMuted,
                            secondaryColor = SystemManaCyan,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("redeem_tokens_button")
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // CARD 4: APP SELECTOR & RULE MANAGER (SEALED PACKAGES LIST)
        // ---------------------------------------------------------
        item {
            SystemWindowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_selector_manager_card"),
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
                        Text(
                            text = "[ APP SELECTOR & RULE MANAGER ]",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemManaCyan,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "SEALED: ${blockedPackages.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (blockedPackages.isNotEmpty()) PenaltyDangerRed else HudTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Toggle target applications to seal during Focus Dungeons. Intercepted apps trigger the full-screen System Penalty HUD.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextSecondary
                    )

                    // Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_field"),
                        placeholder = {
                            Text(
                                "Search installed apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HudTextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = SystemManaCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = HudTextMuted
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemManaCyan,
                            unfocusedBorderColor = HudTextMuted.copy(alpha = 0.4f),
                            focusedTextColor = HudTextPrimary,
                            unfocusedTextColor = HudTextPrimary,
                            cursorColor = SystemManaCyan
                        ),
                        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                    )
                }
            }
        }

        // Installed App Items
        items(
            items = filteredApps,
            key = { it.packageName }
        ) { app ->
            val isSealed = blockedPackages.contains(app.packageName)
            val expiryMs = unsealedAppsExpiryMap[app.packageName] ?: 0L
            val remainingMs = (expiryMs - currentTimeMillis).coerceAtLeast(0L)
            val isCurrentlyUnsealed = remainingMs > 0L
            val remMins = (remainingMs / 60_000L).toInt()

            AppBlockerItemRow(
                app = app,
                isSealed = isSealed,
                isCurrentlyUnsealed = isCurrentlyUnsealed,
                unsealedMinutesLeft = remMins,
                onToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleBlockedPackage(app.packageName)
                }
            )
        }
    }
}

@Composable
private fun PermissionIndicatorRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionLabel: String,
    onClickAction: () -> Unit,
    testTag: String
) {
    val rowShape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(
                if (isGranted) SystemManaCyan.copy(alpha = 0.08f)
                else PenaltyDangerRed.copy(alpha = 0.08f)
            )
            .border(
                width = 1.dp,
                color = if (isGranted) SystemManaCyan.copy(alpha = 0.4f)
                else PenaltyDangerRed.copy(alpha = 0.4f),
                shape = rowShape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) SystemManaCyan else PenaltyDangerRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = HudTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = HudTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CutCornerShape(topStart = 4.dp, bottomEnd = 4.dp))
                    .background(SystemManaCyan.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = SystemManaCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ONLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemManaCyan,
                    fontWeight = FontWeight.Black
                )
            }
        } else {
            SystemHudButton(
                text = actionLabel,
                onClick = onClickAction,
                primaryColor = PenaltyDangerRed,
                secondaryColor = MonarchShadowPurple,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun FeedShieldToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color,
    testTag: String
) {
    val shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SystemWindowSurfaceElevated.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = if (isChecked) activeColor.copy(alpha = 0.6f) else HudTextMuted.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isChecked) activeColor else HudTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = HudTextSecondary
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = activeColor,
                checkedTrackColor = activeColor.copy(alpha = 0.3f),
                uncheckedThumbColor = HudTextMuted,
                uncheckedTrackColor = AbyssalBackground
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun AppBlockerItemRow(
    app: InstalledAppItem,
    isSealed: Boolean,
    isCurrentlyUnsealed: Boolean,
    unsealedMinutesLeft: Int,
    onToggle: () -> Unit
) {
    val cardShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(cardShape)
            .background(
                if (isSealed) PenaltyDangerRed.copy(alpha = 0.08f)
                else SystemWindowSurfaceElevated.copy(alpha = 0.6f)
            )
            .border(
                width = if (isSealed) 1.2.dp else 1.dp,
                color = if (isSealed) PenaltyDangerRed.copy(alpha = 0.6f)
                else HudTextMuted.copy(alpha = 0.25f),
                shape = cardShape
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("app_row_${app.packageName}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Monogram
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                .background(
                    if (isSealed) PenaltyDangerRed.copy(alpha = 0.2f)
                    else SystemManaCyan.copy(alpha = 0.15f)
                )
                .border(
                    width = 1.dp,
                    color = if (isSealed) PenaltyDangerRed else SystemManaCyan,
                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isSealed) PenaltyDangerRed else SystemManaCyan,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSealed) Color.White else HudTextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (app.isPresetTarget) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp))
                            .background(MonarchShadowPurple.copy(alpha = 0.25f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRESET",
                            style = MaterialTheme.typography.labelSmall,
                            color = MonarchShadowPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = HudTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isCurrentlyUnsealed) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "UNSEALED PASS: ${unsealedMinutesLeft}m remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = SovereignRewardGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = isSealed,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PenaltyDangerRed,
                checkedTrackColor = PenaltyDangerRed.copy(alpha = 0.35f),
                uncheckedThumbColor = HudTextMuted,
                uncheckedTrackColor = AbyssalBackground
            ),
            modifier = Modifier.testTag("switch_${app.packageName}")
        )
    }
}
