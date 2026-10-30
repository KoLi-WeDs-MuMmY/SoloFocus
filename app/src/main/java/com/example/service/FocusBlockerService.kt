package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.data.FocusRepository
import kotlin.math.ceil

class FocusBlockerService : AccessibilityService() {

    companion object {
        @Volatile
        private var instanceRef: FocusBlockerService? = null

        private val mainHandler = Handler(Looper.getMainLooper())

        @Volatile
        private var isOverlayShowing: Boolean = false

        @Volatile
        private var postKickCooldownUntil: Long = 0L

        private var activePenaltyOverlayView: View? = null
        private var activeCountdownTimer: CountDownTimer? = null
        private var activeOverlayPackage: String? = null

        fun isOverlayCurrentlyShowing(): Boolean {
            return isOverlayShowing || System.currentTimeMillis() < postKickCooldownUntil
        }

        fun triggerBlockFromAnywhere(context: Context, targetPackage: String) {
            val appCtx = context.applicationContext
            if (isOverlayCurrentlyShowing()) return

            val service = instanceRef
            if (service != null) {
                service.handleAppInterception(targetPackage)
            } else {
                showUnifiedPenaltyOverlay(
                    context = appCtx,
                    targetPackage = targetPackage,
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ACTIVE DUNGEON LOCK ENFORCED ]",
                    isFeedShield = false
                )
            }
        }

        fun showUnifiedPenaltyOverlay(
            context: Context,
            targetPackage: String,
            headerTitle: String,
            subtitle: String,
            isFeedShield: Boolean
        ) {
            val appCtx = context.applicationContext
            val now = System.currentTimeMillis()
            if (isOverlayShowing || now < postKickCooldownUntil) {
                return
            }

            val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(appCtx)
            } else {
                true
            }

            if (!canOverlay) {
                postKickCooldownUntil = now + 1500L
                navigateToHomeLauncher(appCtx)
                mainHandler.post {
                    Toast.makeText(
                        appCtx,
                        "[ ! SYSTEM PENALTY: ACCESS DENIED ] Grant Display Over Other Apps for HUD Popup",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }

            // Lock overlay state immediately so concurrent polls never recreate or reset the popup
            isOverlayShowing = true
            activeOverlayPackage = targetPackage

            mainHandler.post {
                try {
                    // Clean up any stale view reference just in case
                    cleanupOverlayViewsOnly(appCtx)

                    val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    if (wm == null) {
                        isOverlayShowing = false
                        navigateToHomeLauncher(appCtx)
                        return@post
                    }

                    val appLabel = resolveAppLabel(appCtx, targetPackage)
                    val vaultTokens = FocusRepository.rewardTokens.value
                    val density = appCtx.resources.displayMetrics.density
                    fun dp(value: Int): Int = (value * density).toInt()

                    // Custom LinearLayout that intercepts Back key / system Back gesture cleanly
                    val overlayContainer = object : LinearLayout(appCtx) {
                        override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
                            if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
                                if (event.action == KeyEvent.ACTION_UP) {
                                    postKickCooldownUntil = System.currentTimeMillis() + 1800L
                                    dismissPenaltyOverlay(appCtx)
                                    navigateToHomeLauncher(appCtx)
                                }
                                return true
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundColor(Color.parseColor("#F2050811"))
                        setPadding(dp(20), dp(24), dp(20), dp(24))
                        isClickable = true
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }

                    val cardBg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.parseColor("#FA12060D"))
                        setStroke(dp(2).coerceAtLeast(3), Color.parseColor("#FFFF1744"))
                        cornerRadius = dp(12).toFloat()
                    }

                    val innerBox = LinearLayout(appCtx).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = cardBg
                        setPadding(dp(22), dp(26), dp(22), dp(26))
                        isClickable = true
                    }

                    val headerView = TextView(appCtx).apply {
                        text = headerTitle
                        setTextColor(Color.parseColor("#FFFF1744"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        gravity = Gravity.CENTER
                        letterSpacing = 0.05f
                    }

                    val subHeaderView = TextView(appCtx).apply {
                        text = subtitle
                        setTextColor(Color.parseColor("#FFFFD600"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(8), 0, dp(12))
                    }

                    // App Target Capsule
                    val targetCapsule = LinearLayout(appCtx).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor("#29FF1744"))
                            setStroke(dp(1).coerceAtLeast(2), Color.parseColor("#99FF1744"))
                            cornerRadius = dp(8).toFloat()
                        }
                        setPadding(dp(20), dp(12), dp(20), dp(12))
                    }

                    val targetAppNameView = TextView(appCtx).apply {
                        text = appLabel.uppercase()
                        setTextColor(Color.WHITE)
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        gravity = Gravity.CENTER
                    }

                    val targetPkgView = TextView(appCtx).apply {
                        text = targetPackage
                        setTextColor(Color.parseColor("#FF8FA3BF"))
                        typeface = Typeface.MONOSPACE
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        gravity = Gravity.CENTER
                    }

                    targetCapsule.addView(targetAppNameView)
                    targetCapsule.addView(targetPkgView)

                    // 3-second live countdown badge
                    val countdownBadge = TextView(appCtx).apply {
                        text = "3s"
                        setTextColor(Color.parseColor("#FFFFD600"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(14), 0, dp(2))
                    }

                    val countdownView = TextView(appCtx).apply {
                        text = "System Kick-Out in 3s..."
                        setTextColor(Color.parseColor("#FF00E5FF"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, dp(16))
                    }

                    // Helper to create responsive button background with pressed state
                    fun createHudButtonDrawable(fillHex: String, pressedHex: String, strokeHex: String): StateListDrawable {
                        val normal = GradientDrawable().apply {
                            setColor(Color.parseColor(fillHex))
                            setStroke(dp(1).coerceAtLeast(2), Color.parseColor(strokeHex))
                            cornerRadius = dp(8).toFloat()
                        }
                        val pressed = GradientDrawable().apply {
                            setColor(Color.parseColor(pressedHex))
                            setStroke(dp(2).coerceAtLeast(3), Color.WHITE)
                            cornerRadius = dp(8).toFloat()
                        }
                        return StateListDrawable().apply {
                            addState(intArrayOf(android.R.attr.state_pressed), pressed)
                            addState(intArrayOf(), normal)
                        }
                    }

                    // Action Button 1: Return to Safe Zone (Home)
                    val returnHomeBtn = TextView(appCtx).apply {
                        text = "[ RETURN TO SAFE ZONE (HOME) ]"
                        setTextColor(Color.WHITE)
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        gravity = Gravity.CENTER
                        minHeight = dp(48)
                        background = createHudButtonDrawable(
                            fillHex = "#4DFF1744",
                            pressedHex = "#80FF1744",
                            strokeHex = "#FFFF1744"
                        )
                        setPadding(dp(20), dp(14), dp(20), dp(14))
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            postKickCooldownUntil = System.currentTimeMillis() + 1800L
                            dismissPenaltyOverlay(appCtx)
                            navigateToHomeLauncher(appCtx)
                        }
                    }

                    // Action Button 2: Quick Token Unseal (if user has >= 100 tokens)
                    var quickUnsealBtn: TextView? = null
                    if (vaultTokens >= 100 && !isFeedShield) {
                        quickUnsealBtn = TextView(appCtx).apply {
                            text = "[ UNSEAL 10 MIN (100 / $vaultTokens TOKENS) ]"
                            setTextColor(Color.parseColor("#FFFFD600"))
                            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            gravity = Gravity.CENTER
                            minHeight = dp(48)
                            background = createHudButtonDrawable(
                                fillHex = "#33FFD600",
                                pressedHex = "#66FFD600",
                                strokeHex = "#FFFFD600"
                            )
                            setPadding(dp(18), dp(13), dp(18), dp(13))
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                postKickCooldownUntil = System.currentTimeMillis() + 1500L
                                FocusRepository.redeemTokensForApp(targetPackage, appLabel, 10)
                                dismissPenaltyOverlay(appCtx)
                                Toast.makeText(
                                    appCtx,
                                    "$appLabel unsealed for 10 minutes",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    // Action Button 3: Open System Focus HUD
                    val openHudBtn = TextView(appCtx).apply {
                        text = "[ VIEW DAILY QUESTS IN SYSTEM HUD ]"
                        setTextColor(Color.parseColor("#FF00E5FF"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        gravity = Gravity.CENTER
                        minHeight = dp(48)
                        background = createHudButtonDrawable(
                            fillHex = "#2900E5FF",
                            pressedHex = "#5900E5FF",
                            strokeHex = "#FF00E5FF"
                        )
                        setPadding(dp(18), dp(13), dp(18), dp(13))
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            postKickCooldownUntil = System.currentTimeMillis() + 1800L
                            dismissPenaltyOverlay(appCtx)
                            try {
                                val hudIntent = Intent(appCtx, MainActivity::class.java).apply {
                                    putExtra("EXTRA_OPEN_TAB", "DAILY_QUESTS")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                appCtx.startActivity(hudIntent)
                            } catch (_: Exception) {
                            }
                        }
                    }

                    val fullWidthParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    val buttonTopMarginParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(10)
                    }

                    innerBox.addView(headerView, fullWidthParams)
                    innerBox.addView(subHeaderView, fullWidthParams)
                    innerBox.addView(targetCapsule, fullWidthParams)
                    innerBox.addView(countdownBadge, fullWidthParams)
                    innerBox.addView(countdownView, fullWidthParams)
                    innerBox.addView(returnHomeBtn, fullWidthParams)
                    if (quickUnsealBtn != null) {
                        innerBox.addView(quickUnsealBtn, buttonTopMarginParams)
                    }
                    innerBox.addView(openHudBtn, buttonTopMarginParams)

                    overlayContainer.addView(
                        innerBox,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )

                    val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }

                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        layoutFlag,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    wm.addView(overlayContainer, params)
                    overlayContainer.requestFocus()
                    activePenaltyOverlayView = overlayContainer

                    // Single uninterrupted 3-second countdown timer (150ms tick interval for crisp updates)
                    activeCountdownTimer?.cancel()
                    activeCountdownTimer = object : CountDownTimer(3000L, 150L) {
                        override fun onTick(millisUntilFinished: Long) {
                            val secs = ceil(millisUntilFinished / 1000.0).toInt().coerceIn(1, 3)
                            countdownBadge.text = "${secs}s"
                            countdownView.text = "System Kick-Out in ${secs}s..."
                        }

                        override fun onFinish() {
                            countdownBadge.text = "0s"
                            countdownView.text = "Enforcing System Kick-Out..."
                            postKickCooldownUntil = System.currentTimeMillis() + 1800L
                            dismissPenaltyOverlay(appCtx)
                            navigateToHomeLauncher(appCtx)
                        }
                    }.start()
                } catch (_: Exception) {
                    isOverlayShowing = false
                    postKickCooldownUntil = System.currentTimeMillis() + 1500L
                    navigateToHomeLauncher(appCtx)
                }
            }
        }

        private fun cleanupOverlayViewsOnly(context: Context) {
            activeCountdownTimer?.cancel()
            activeCountdownTimer = null
            val view = activePenaltyOverlayView ?: return
            activePenaltyOverlayView = null
            try {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                wm?.removeViewImmediate(view)
            } catch (_: Exception) {
                try {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    wm?.removeView(view)
                } catch (_: Exception) {
                }
            }
        }

        fun dismissPenaltyOverlay(context: Context) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                cleanupOverlayViewsOnly(context)
                activeOverlayPackage = null
                isOverlayShowing = false
            } else {
                mainHandler.post {
                    cleanupOverlayViewsOnly(context)
                    activeOverlayPackage = null
                    isOverlayShowing = false
                }
            }
        }

        fun navigateToHomeLauncher(context: Context) {
            try {
                instanceRef?.performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {
            }
            try {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(homeIntent)
            } catch (_: Exception) {
            }
        }

        fun resolveAppLabel(context: Context, packageName: String): String {
            return try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName
            }
        }
    }

    private var lastFeedScanTime: Long = 0L
    private var cachedHomePackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = this
        FocusRepository.init(applicationContext)
        cachedHomePackage = resolveDefaultHomeLauncherPackage()

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            info.notificationTimeout = 100L
            info.packageNames = null // Global coverage across all packages
            serviceInfo = info
        } catch (_: Exception) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // If penalty overlay is already on screen or cooling down after kick-out, ignore events
        if (isOverlayCurrentlyShowing()) return

        val ownPackage = applicationContext.packageName
        val eventPkg = event.packageName?.toString()?.trim()

        // 1. RECURSION GUARD: If event originates from our own package, NEVER block
        if (!eventPkg.isNullOrBlank() && (
                eventPkg == ownPackage ||
                eventPkg == "com.example" ||
                eventPkg.startsWith("com.aistudio.systemfocus") ||
                eventPkg.contains("systemfocus", ignoreCase = true)
            )
        ) {
            return
        }

        val targetPkg = eventPkg
            ?: try { rootInActiveWindow?.packageName?.toString()?.trim() } catch (_: Exception) { null }
            ?: return

        if (targetPkg == ownPackage ||
            targetPkg == "com.example" ||
            targetPkg.startsWith("com.aistudio.systemfocus") ||
            targetPkg.contains("systemfocus", ignoreCase = true)
        ) {
            return
        }

        // 2. SYSTEM GUARDS: Never block System UI, Settings, Permission Controller, Keyboard, or Home Launcher
        if (isExemptSystemOrSelfPackage(targetPkg)) {
            return
        }

        if (!FocusRepository.isReady()) {
            FocusRepository.init(applicationContext)
        }

        val now = System.currentTimeMillis()
        val isBlockingActive = FocusRepository.isDungeonLockActive(now)
        val blockedPackages = FocusRepository.blockedPackages.value

        // 3. REAL-TIME APP BLOCKING
        if (isBlockingActive &&
            blockedPackages.contains(targetPkg) &&
            !FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)
        ) {
            handleAppInterception(targetPkg)
            return
        }

        // 4. SHORTS & REELS FEED SCANNER (strictly scoped to YouTube & Instagram and throttled)
        val sealShorts = FocusRepository.sealYoutubeShorts.value
        val sealReels = FocusRepository.sealInstagramReels.value
        if (!sealShorts && !sealReels) return

        val isYoutube = targetPkg.equals("com.google.android.youtube", ignoreCase = true) ||
            targetPkg.contains("youtube", ignoreCase = true)
        val isInstagram = targetPkg.equals("com.instagram.android", ignoreCase = true) ||
            targetPkg.contains("instagram", ignoreCase = true)

        if (!isYoutube && !isInstagram) return
        if (now - lastFeedScanTime < 350L) return
        lastFeedScanTime = now

        if (FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)) return

        val rootNode = try { rootInActiveWindow } catch (_: Exception) { null } ?: return
        val nodePkg = rootNode.packageName?.toString()?.takeIf { it.isNotBlank() } ?: targetPkg
        if (FocusRepository.isPackageTemporarilyUnsealed(nodePkg, now)) return

        if (sealShorts && isYoutube && isYoutubeShortsDetected(nodePkg, rootNode)) {
            FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> Kick-Out")
            showUnifiedPenaltyOverlay(
                context = applicationContext,
                targetPackage = nodePkg,
                headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                subtitle = "[ SHORT-FORM FEED SEALED BY SYSTEM ]",
                isFeedShield = true
            )
            return
        }

        if (sealReels && isInstagram && isInstagramReelsDetected(nodePkg, rootNode)) {
            FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> Kick-Out")
            showUnifiedPenaltyOverlay(
                context = applicationContext,
                targetPackage = nodePkg,
                headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                subtitle = "[ SHORT-FORM FEED SEALED BY SYSTEM ]",
                isFeedShield = true
            )
            return
        }
    }

    fun handleAppInterception(targetPackage: String) {
        if (isOverlayCurrentlyShowing()) return

        FocusRepository.recordBlockedInterception("Sealed App Intercepted: $targetPackage")
        showUnifiedPenaltyOverlay(
            context = applicationContext,
            targetPackage = targetPackage,
            headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
            subtitle = "[ ACTIVE DUNGEON LOCK ENFORCED ]",
            isFeedShield = false
        )
    }

    private fun isExemptSystemOrSelfPackage(pkg: String): Boolean {
        val ownPkg = applicationContext.packageName
        if (pkg == ownPkg ||
            pkg == "com.example" ||
            pkg.startsWith("com.aistudio.systemfocus")
        ) {
            return true
        }

        val homePkg = cachedHomePackage ?: resolveDefaultHomeLauncherPackage().also {
            cachedHomePackage = it
        }

        return pkg == "com.android.systemui" ||
            pkg == "com.android.settings" ||
            pkg.contains("inputmethod", ignoreCase = true) ||
            pkg.contains("permissioncontroller", ignoreCase = true) ||
            pkg.contains("launcher", ignoreCase = true) ||
            (homePkg != null && pkg == homePkg)
    }

    private fun isYoutubeShortsDetected(
        packageName: String,
        root: AccessibilityNodeInfo
    ): Boolean {
        val isYoutubePkg = packageName.equals("com.google.android.youtube", ignoreCase = true) ||
            packageName.contains("youtube", ignoreCase = true)
        if (!isYoutubePkg) return false

        return scanNodeTreeForSignals(
            node = root,
            viewIdSignals = listOf(
                "reel_player",
                "shorts_",
                "reel_recycler",
                "reel_watch",
                "reel_progress",
                "reel_multi_format"
            ),
            selectedTabLabels = listOf("Shorts")
        )
    }

    private fun isInstagramReelsDetected(
        packageName: String,
        root: AccessibilityNodeInfo
    ): Boolean {
        val isInstagramPkg = packageName.equals("com.instagram.android", ignoreCase = true) ||
            packageName.contains("instagram", ignoreCase = true)
        if (!isInstagramPkg) return false

        return scanNodeTreeForSignals(
            node = root,
            viewIdSignals = listOf(
                "clips_viewer",
                "reel_viewer",
                "clips_video",
                "clips_swipe",
                "clips_item"
            ),
            selectedTabLabels = listOf("Reels")
        )
    }

    private fun scanNodeTreeForSignals(
        node: AccessibilityNodeInfo?,
        viewIdSignals: List<String>,
        selectedTabLabels: List<String>,
        depth: Int = 0
    ): Boolean {
        if (node == null || depth > 16) return false

        val viewId = node.viewIdResourceName.orEmpty().lowercase()
        if (viewId.isNotEmpty() && viewIdSignals.any { signal -> viewId.contains(signal) }) {
            return true
        }

        if (node.isSelected) {
            val textStr = node.text?.toString()?.trim().orEmpty()
            val descStr = node.contentDescription?.toString()?.trim().orEmpty()
            for (tabLabel in selectedTabLabels) {
                if (textStr.equals(tabLabel, ignoreCase = true) ||
                    descStr.equals(tabLabel, ignoreCase = true) ||
                    descStr.startsWith("$tabLabel,", ignoreCase = true)
                ) {
                    return true
                }
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Exception) {
                null
            } ?: continue
            if (scanNodeTreeForSignals(child, viewIdSignals, selectedTabLabels, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun resolveDefaultHomeLauncherPackage(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    override fun onDestroy() {
        instanceRef = null
        dismissPenaltyOverlay(applicationContext)
        super.onDestroy()
    }

    override fun onInterrupt() {
        dismissPenaltyOverlay(applicationContext)
    }
}
