package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainActivity
import com.example.data.FocusRepository
import com.example.ui.components.SystemBackgroundScanlines
import com.example.ui.components.SystemHudButton
import com.example.ui.components.SystemWindowCard
import com.example.ui.theme.AbyssalBackground
import com.example.ui.theme.HudTextPrimary
import com.example.ui.theme.HudTextSecondary
import com.example.ui.theme.MonarchShadowPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PenaltyDangerRed
import com.example.ui.theme.RoyalSystemBlue
import com.example.ui.theme.SovereignRewardGold
import com.example.ui.theme.SystemManaCyan
import kotlinx.coroutines.delay

class BlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_BLOCK_REASON = "extra_block_reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FocusRepository.init(applicationContext)
        enableEdgeToEdge()

        val blockedPkg = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        val reason = intent.getStringExtra(EXTRA_BLOCK_REASON) ?: "SEALED_APP"

        setContent {
            MyApplicationTheme {
                BlockerScreen(
                    blockedPackage = blockedPkg,
                    blockReason = reason,
                    onNavigateHome = { navigateHomeAndFinish() },
                    onOpenAppHud = { openMainHudAndFinish() },
                    onQuickUnseal = { pkg, appName ->
                        quickUnsealAndLaunch(pkg, appName)
                    }
                )
            }
        }
    }

    private fun navigateHomeAndFinish() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        } catch (_: Exception) {
        }
        finish()
    }

    private fun openMainHudAndFinish() {
        try {
            val hudIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(hudIntent)
        } catch (_: Exception) {
        }
        finish()
    }

    private fun quickUnsealAndLaunch(packageName: String, appName: String) {
        val success = FocusRepository.redeemTokensForApp(
            packageName = packageName,
            appName = appName,
            minutes = 10
        )
        if (success) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            } catch (_: Exception) {
            }
            finish()
        }
    }
}

@Composable
fun BlockerScreen(
    blockedPackage: String,
    blockReason: String,
    onNavigateHome: () -> Unit,
    onOpenAppHud: () -> Unit,
    onQuickUnseal: (packageName: String, appName: String) -> Unit
) {
    val context = LocalContext.current
    val rewardTokens by FocusRepository.rewardTokens.collectAsStateWithLifecycle()

    // Resolving application label
    val appName = remember(blockedPackage) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(blockedPackage, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            if (blockedPackage.isNotBlank()) blockedPackage else "Target Application"
        }
    }

    // 5-second mindful cooldown timer (standard in One Sec / Freedom / Regain)
    var countdownSeconds by remember { mutableIntStateOf(5) }
    val progressAnimation = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        progressAnimation.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
    }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds -= 1
        }
    }

    // Intercept Back button: always return to Home, never expose the blocked app
    BackHandler {
        onNavigateHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AbyssalBackground)
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        SystemBackgroundScanlines()

        SystemWindowCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blocker_friction_card"),
            borderStartColor = PenaltyDangerRed,
            borderEndColor = MonarchShadowPurple,
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Alert
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Penalty Alert",
                        tint = PenaltyDangerRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                        style = MaterialTheme.typography.titleMedium,
                        color = PenaltyDangerRed,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = when (blockReason) {
                        "SHORTS_REELS" -> "[ SHORT-FORM FEED SEALED BY SYSTEM ]"
                        else -> "[ ACTIVE DUNGEON LOCK ENFORCED ]"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = SovereignRewardGold,
                    fontWeight = FontWeight.Bold
                )

                // Blocked App Target Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                        .background(PenaltyDangerRed.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = PenaltyDangerRed.copy(alpha = 0.6f),
                            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = appName.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                        if (blockedPackage.isNotBlank()) {
                            Text(
                                text = blockedPackage,
                                style = MaterialTheme.typography.labelSmall,
                                color = HudTextSecondary
                            )
                        }
                    }
                }

                // 5-second Mindful Cooldown Meter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(68.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnimation.value },
                            modifier = Modifier.fillMaxSize(),
                            color = PenaltyDangerRed,
                            trackColor = PenaltyDangerRed.copy(alpha = 0.2f),
                            strokeWidth = 5.dp
                        )
                        Text(
                            text = if (countdownSeconds > 0) "${countdownSeconds}s" else "PAUSE",
                            style = MaterialTheme.typography.titleMedium,
                            color = SovereignRewardGold,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "Take a breath. Protect your Hunter streak and focus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HudTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Action: Return to Home
                SystemHudButton(
                    text = "[ RETURN TO HOME SCREEN ]",
                    onClick = onNavigateHome,
                    primaryColor = PenaltyDangerRed,
                    secondaryColor = MonarchShadowPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocker_return_home_button")
                )

                // Secondary Action: Open Focus App
                SystemHudButton(
                    text = "[ OPEN SYSTEM FOCUS HUD ]",
                    onClick = onOpenAppHud,
                    primaryColor = SystemManaCyan,
                    secondaryColor = RoyalSystemBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blocker_open_hud_button")
                )

                // Quick Token Unseal Shortcut (if user has >= 100 Reward Tokens)
                if (rewardTokens >= 100 && blockedPackage.isNotBlank()) {
                    SystemHudButton(
                        text = "[ UNSEAL 10 MIN (COST: 100 / $rewardTokens TOKENS) ]",
                        onClick = { onQuickUnseal(blockedPackage, appName) },
                        primaryColor = SovereignRewardGold,
                        secondaryColor = SystemManaCyan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blocker_quick_unseal_button")
                    )
                }
            }
        }
    }
}
