package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerStatsState
import com.example.data.SystemAlertBanner
import com.example.data.TimerPhase
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudGridLine
import com.example.ui.theme.HudTextMuted
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MonarchShadowPurple
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SovereignRewardGold
import com.example.ui.theme.SystemManaCyan
import com.example.ui.theme.SystemWindowSurface
import com.example.ui.theme.SystemWindowSurfaceElevated
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun SystemWindowCard(
    modifier: Modifier = Modifier,
    borderStartColor: Color = SystemManaCyan,
    borderEndColor: Color = RoyalSystemBlue,
    surfaceColor: Color = SystemWindowSurface.copy(alpha = 0.94f),
    cornerCut: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = CutCornerShape(topStart = cornerCut, bottomEnd = cornerCut)
    val borderBrush = Brush.linearGradient(
        colors = listOf(borderStartColor, borderEndColor, borderStartColor.copy(alpha = 0.75f))
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor, shape)
            .border(width = 1.5.dp, brush = borderBrush, shape = shape)
            .drawBehind {
                val stepPx = 6.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = borderStartColor.copy(alpha = 0.035f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += stepPx
                }

                val bracketLen = 14.dp.toPx()
                val stroke = 2.dp.toPx()
                val inset = 4.dp.toPx()

                // Top-Right bracket
                drawLine(
                    color = borderStartColor.copy(alpha = 0.85f),
                    start = Offset(size.width - bracketLen - inset, inset),
                    end = Offset(size.width - inset, inset),
                    strokeWidth = stroke
                )
                drawLine(
                    color = borderStartColor.copy(alpha = 0.85f),
                    start = Offset(size.width - inset, inset),
                    end = Offset(size.width - inset, inset + bracketLen),
                    strokeWidth = stroke
                )

                // Bottom-Left bracket
                drawLine(
                    color = borderStartColor.copy(alpha = 0.85f),
                    start = Offset(inset, size.height - bracketLen - inset),
                    end = Offset(inset, size.height - inset),
                    strokeWidth = stroke
                )
                drawLine(
                    color = borderStartColor.copy(alpha = 0.85f),
                    start = Offset(inset, size.height - inset),
                    end = Offset(inset + bracketLen, size.height - inset),
                    strokeWidth = stroke
                )
            }
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SystemBackgroundScanlines(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 36.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = HudGridLine.copy(alpha = 0.05f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSpacing
        }

        val scanlineSpacing = 8.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = SystemManaCyan.copy(alpha = 0.025f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += scanlineSpacing
        }
    }
}

@Composable
fun PlayerStatusHeaderCard(
    stats: PlayerStatsState,
    rewardTokens: Int,
    canDrawOverlays: Boolean,
    onRequestOverlayPermission: () -> Unit,
    activeBanner: SystemAlertBanner?,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpFraction = (stats.currentXp.toFloat() / stats.xpToNextLevel.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val animatedXpProgress by animateFloatAsState(
        targetValue = xpFraction,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "xp_bar_progress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("player_status_header_card"),
            borderStartColor = SystemManaCyan,
            borderEndColor = MonarchShadowPurple,
            contentPadding = 14.dp
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
                        Box(
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MonarchShadowPurple.copy(alpha = 0.35f),
                                            RoyalSystemBlue.copy(alpha = 0.35f)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = SystemManaCyan,
                                    shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = stats.rank.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = SystemManaCyan,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "[ PLAYER STATUS ]",
                                style = MaterialTheme.typography.labelSmall,
                                color = HudTextSecondary
                            )
                            Text(
                                text = "LV.${stats.level} • ${stats.rank.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = HudTextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Reward Token Vault Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                                .background(AbyssalBackground.copy(alpha = 0.9f))
                                .border(
                                    width = 1.dp,
                                    color = SovereignRewardGold,
                                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("header_reward_tokens_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Token,
                                contentDescription = "Reward Tokens",
                                tint = SovereignRewardGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$rewardTokens TOKENS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignRewardGold,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Streak Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp))
                                .background(AbyssalBackground.copy(alpha = 0.8f))
                                .border(
                                    width = 1.dp,
                                    color = SystemManaCyan.copy(alpha = 0.7f),
                                    shape = CutCornerShape(topStart = 6.dp, bottomEnd = 6.dp)
                                )
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak Days",
                                tint = SystemManaCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${stats.streakDays}D",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemManaCyan,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Glowing Cyan/Purple XP Bar rendered with Canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANA / EXP GAUGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemManaCyan
                    )
                    Text(
                        text = "${stats.currentXp} / ${stats.xpToNextLevel} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MonarchShadowPurple,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .testTag("player_xp_bar")
                ) {
                    val barHeight = size.height
                    val barWidth = size.width

                    drawRect(
                        color = AbyssalBackground,
                        size = Size(barWidth, barHeight)
                    )

                    val fillWidth = (barWidth * animatedXpProgress).coerceAtLeast(0f)
                    if (fillWidth > 0f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(SystemManaCyan, RoyalSystemBlue, MonarchShadowPurple)
                            ),
                            size = Size(fillWidth, barHeight)
                        )
                    }

                    val segments = 10
                    val segmentStep = barWidth / segments
                    for (i in 1 until segments) {
                        val x = i * segmentStep
                        drawLine(
                            color = AbyssalBackground.copy(alpha = 0.85f),
                            start = Offset(x, 0f),
                            end = Offset(x, barHeight),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    drawRect(
                        color = SystemManaCyan.copy(alpha = 0.55f),
                        size = Size(barWidth, barHeight),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // If SYSTEM_ALERT_WINDOW permission is missing, display quick-grant button right in the header
                if (!canDrawOverlays) {
                    Spacer(modifier = Modifier.height(10.dp))
                    SystemHudButton(
                        text = "[ GRANT OVERLAY SYSTEM AUTHORITY ]",
                        onClick = onRequestOverlayPermission,
                        primaryColor = PenaltyDangerRed,
                        secondaryColor = MonarchShadowPurple,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("header_grant_overlay_permission_button")
                    )
                }
            }
        }

        // Animated [ LEVEL UP! ] or [ DUNGEON CLEARED ] alert banner
        AnimatedVisibility(
            visible = activeBanner != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (activeBanner != null) {
                val accentColor = when {
                    activeBanner.isRewardUnlock -> SovereignRewardGold
                    activeBanner.isLevelUp -> MonarchShadowPurple
                    else -> SystemManaCyan
                }
                val secondaryAccent = when {
                    activeBanner.isRewardUnlock -> SystemManaCyan
                    activeBanner.isLevelUp -> SystemManaCyan
                    else -> RoyalSystemBlue
                }

                Spacer(modifier = Modifier.height(8.dp))
                SystemWindowCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("system_alert_banner"),
                    borderStartColor = accentColor,
                    borderEndColor = secondaryAccent,
                    surfaceColor = AbyssalBackground.copy(alpha = 0.96f),
                    contentPadding = 12.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (activeBanner.isLevelUp || activeBanner.isRewardUnlock) {
                                    Icons.Default.AutoAwesome
                                } else {
                                    Icons.Default.Shield
                                },
                                contentDescription = "System Notification",
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = activeBanner.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accentColor,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = activeBanner.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HudTextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismissBanner,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dismiss_banner_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss System Notification",
                                tint = HudTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManaCoreTimerDial(
    remainingSeconds: Int,
    totalSeconds: Int,
    timerPhase: TimerPhase,
    isTimerRunning: Boolean,
    isBlockingActive: Boolean,
    expectedTokenYield: Int,
    boundQuestTitle: String?,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "mana_core_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mana_core_rotation")
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring_rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val primaryDialColor = when {
        timerPhase == TimerPhase.REWARD_UNLOCK -> SovereignRewardGold
        timerPhase == TimerPhase.FOCUS_LOCK || isBlockingActive -> PenaltyDangerRed
        else -> SystemManaCyan
    }

    val secondaryDialColor = when {
        timerPhase == TimerPhase.REWARD_UNLOCK -> SystemManaCyan
        timerPhase == TimerPhase.FOCUS_LOCK || isBlockingActive -> MonarchShadowPurple
        else -> RoyalSystemBlue
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    val statusSubLabel = when (timerPhase) {
        TimerPhase.FOCUS_LOCK -> "[ DUNGEON LOCK: ACTIVE ]"
        TimerPhase.REWARD_UNLOCK -> "[ TOKENS DEPOSITED ]"
        TimerPhase.IDLE -> if (isBlockingActive) "[ DUNGEON LOCK: ARMED ]" else "[ MANA CORE: STANDBY ]"
    }

    Box(
        modifier = modifier.testTag("mana_core_timer_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = diameter * 0.45f
            val innerRingRadius = diameter * 0.36f
            val hexRadius = diameter * 0.27f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryDialColor.copy(alpha = if (isTimerRunning) 0.22f * pulseAlpha else 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius * 1.1f
                ),
                radius = outerRadius * 1.1f,
                center = center
            )

            val tickCount = 60
            val activeTicks = (tickCount * animatedProgress).toInt()
            for (i in 0 until tickCount) {
                val angleDeg = (i * (360f / tickCount)) - 90f
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajorTick = i % 5 == 0
                val tickInner = if (isMajorTick) outerRadius * 0.90f else outerRadius * 0.94f
                val tickOuter = outerRadius
                val start = Offset(
                    x = center.x + (tickInner * cos(angleRad)).toFloat(),
                    y = center.y + (tickInner * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    x = center.x + (tickOuter * cos(angleRad)).toFloat(),
                    y = center.y + (tickOuter * sin(angleRad)).toFloat()
                )
                val isLit = i <= activeTicks
                drawLine(
                    color = if (isLit) {
                        primaryDialColor.copy(alpha = if (isMajorTick) 1f else 0.78f)
                    } else {
                        HudTextMuted.copy(alpha = 0.25f)
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (isMajorTick) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Square
                )
            }

            val arcSize = Size(innerRingRadius * 2f, innerRingRadius * 2f)
            val arcTopLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius)

            drawArc(
                color = HudTextMuted.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Butt)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryDialColor, secondaryDialColor, primaryDialColor),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Square)
            )

            rotate(degrees = if (isTimerRunning) rotationDegrees else 0f, pivot = center) {
                val hexPath = Path()
                for (side in 0 until 6) {
                    val angleRad = Math.toRadians((60.0 * side) - 30.0)
                    val px = center.x + (hexRadius * cos(angleRad)).toFloat()
                    val py = center.y + (hexRadius * sin(angleRad)).toFloat()
                    if (side == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
                }
                hexPath.close()

                drawPath(
                    path = hexPath,
                    color = primaryDialColor.copy(alpha = 0.45f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            val reticleInset = diameter * 0.06f
            val reticleLength = 16.dp.toPx()
            val reticleStroke = 2.dp.toPx()
            val left = center.x - outerRadius - reticleInset
            val right = center.x + outerRadius + reticleInset
            val top = center.y - outerRadius - reticleInset
            val bottom = center.y + outerRadius + reticleInset

            drawLine(primaryDialColor, Offset(left, top), Offset(left + reticleLength, top), reticleStroke)
            drawLine(primaryDialColor, Offset(left, top), Offset(left, top + reticleLength), reticleStroke)
            drawLine(primaryDialColor, Offset(right - reticleLength, top), Offset(right, top), reticleStroke)
            drawLine(primaryDialColor, Offset(right, top), Offset(right, top + reticleLength), reticleStroke)
            drawLine(primaryDialColor, Offset(left, bottom - reticleLength), Offset(left, bottom), reticleStroke)
            drawLine(primaryDialColor, Offset(left, bottom), Offset(left + reticleLength, bottom), reticleStroke)
            drawLine(primaryDialColor, Offset(right, bottom - reticleLength), Offset(right, bottom), reticleStroke)
            drawLine(primaryDialColor, Offset(right - reticleLength, bottom), Offset(right, bottom), reticleStroke)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = statusSubLabel,
                style = MaterialTheme.typography.labelSmall,
                color = primaryDialColor,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = HudTextPrimary,
                modifier = Modifier.testTag("timer_countdown_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "YIELD: +$expectedTokenYield TOKENS & +100 XP",
                style = MaterialTheme.typography.labelSmall,
                color = SovereignRewardGold,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            if (!boundQuestTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "BOUND: $boundQuestTitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemManaCyan,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SystemDurationChip(
    label: String,
    subLabel: String? = null,
    isSelected: Boolean,
    enabled: Boolean,
    activeColor: Color = SystemManaCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    val borderColor = if (isSelected) activeColor else HudTextMuted.copy(alpha = 0.5f)
    val bgColor = if (isSelected) {
        activeColor.copy(alpha = 0.18f)
    } else {
        SystemWindowSurfaceElevated.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(52.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) activeColor else HudTextSecondary,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
            )
            if (!subLabel.isNullOrBlank()) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) SovereignRewardGold else HudTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SystemHudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = SystemManaCyan,
    secondaryColor: Color = RoyalSystemBlue,
    enabled: Boolean = true
) {
    val shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp)
    val borderBrush = Brush.linearGradient(listOf(primaryColor, secondaryColor))
    val backgroundBrush = Brush.horizontalGradient(
        colors = if (enabled) {
            listOf(primaryColor.copy(alpha = 0.25f), secondaryColor.copy(alpha = 0.25f))
        } else {
            listOf(HudTextMuted.copy(alpha = 0.12f), HudTextMuted.copy(alpha = 0.12f))
        }
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(50.dp)
            .clip(shape)
            .background(backgroundBrush, shape)
            .border(
                width = 1.5.dp,
                brush = if (enabled) borderBrush else Brush.linearGradient(listOf(HudTextMuted, HudTextMuted)),
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) primaryColor else HudTextMuted,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}
