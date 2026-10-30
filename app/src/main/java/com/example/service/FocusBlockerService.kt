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
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.MainActivity
import com.example.data.FocusRepository
import com.example.ui.BlockerActivity

class FocusBlockerService : AccessibilityService() {

    companion object {
        private var instanceRef: FocusBlockerService? = null

        fun triggerBlockFromAnywhere(context: Context, targetPackage: String) {
            val service = instanceRef
            if (service != null) {
                service.handleAppInterception(targetPackage)
            } else {
                // If service instance is temporarily unbound, fallback to standalone overlay
                showStandaloneOverlay(context.applicationContext, targetPackage)
            }
        }

        private fun showStandaloneOverlay(context: Context, targetPackage: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            Handler(Looper.getMainLooper()).post {
                try {
                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                    val appLabel = resolveAppLabel(context, targetPackage)

                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundColor(Color.parseColor("#F4050811"))
                        setPadding(48, 48, 48, 48)
                        isClickable = true
                        isFocusable = true
                    }

                    val card = GradientDrawable().apply {
                        setColor(Color.parseColor("#FA1A050D"))
                        setStroke(5, Color.parseColor("#FFFF1744"))
                        cornerRadius = 14f
                    }

                    val inner = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = card
                        setPadding(40, 48, 40, 48)
                    }

                    val title = TextView(context).apply {
                        text = "[ ! SYSTEM PENALTY: ACCESS DENIED ]"
                        setTextColor(Color.parseColor("#FFFF1744"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        gravity = Gravity.CENTER
                    }

                    val sub = TextView(context).apply {
                        text = "[ DUNGEON LOCK ENFORCED: $appLabel ]"
                        setTextColor(Color.parseColor("#FFFFD600"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        gravity = Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }

                    val timerText = TextView(context).apply {
                        text = "System Kick-Out in 3s..."
                        setTextColor(Color.parseColor("#FF00E5FF"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, 16)
                    }

                    val homeBtn = TextView(context).apply {
                        text = "[ RETURN TO HOME SCREEN ]"
                        setTextColor(Color.parseColor("#FFFF1744"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor("#33FF1744"))
                            setStroke(2, Color.parseColor("#FFFF1744"))
                            cornerRadius = 8f
                        }
                        setPadding(28, 18, 28, 18)
                        setOnClickListener {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(homeIntent)
                            try { wm.removeView(container) } catch (_: Exception) {}
                        }
                    }

                    inner.addView(title)
                    inner.addView(sub)
                    inner.addView(timerText)
                    inner.addView(homeBtn)
                    container.addView(inner)

                    val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    }
                    val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        flag,
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    wm.addView(container, params)

                    // Auto kick-out after 3 seconds
                    object : CountDownTimer(3000L, 1000L) {
                        override fun onTick(millisUntilFinished: Long) {
                            val secs = (millisUntilFinished / 1000L).toInt() + 1
                            timerText.text = "System Kick-Out in ${secs}s..."
                        }

                        override fun onFinish() {
                            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(homeIntent)
                            try { wm.removeView(container) } catch (_: Exception) {}
                        }
                    }.start()
                } catch (_: Exception) {
                }
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

    private var lastBlockActionTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cachedHomePackage: String? = null
    private var activePenaltyOverlayView: View? = null
    private var activeCountdownTimer: CountDownTimer? = null
    private val dismissOverlayRunnable = Runnable { removePenaltyOverlay() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = this
        FocusRepository.init(applicationContext)
        cachedHomePackage = resolveDefaultHomeLauncherPackage()

        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            info.notificationTimeout = 30L
            info.packageNames = null // Global coverage across all packages
            serviceInfo = info
        } catch (_: Exception) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val ownPackage = applicationContext.packageName
        val eventPkg = event.packageName?.toString()?.trim()

        // 1. RECURSION GUARD: If event originates from our own package or BlockerActivity, NEVER block
        if (!eventPkg.isNullOrBlank() && (
                eventPkg == ownPackage ||
                eventPkg == "com.example" ||
                eventPkg.startsWith("com.aistudio.systemfocus") ||
                eventPkg.contains("systemfocus", ignoreCase = true)
            )
        ) {
            return
        }

        // Target package to inspect
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

        // Ensure live repository state
        FocusRepository.init(applicationContext)

        val now = System.currentTimeMillis()
        val isBlockingActive = FocusRepository.isDungeonLockActive(now)
        val blockedPackages = FocusRepository.blockedPackages.value

        // 3. REAL-TIME APP BLOCKING (Runs whether our app is backgrounded, minimized, or closed)
        if (isBlockingActive &&
            blockedPackages.contains(targetPkg) &&
            !FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)
        ) {
            handleAppInterception(targetPkg)
            return
        }

        // 4. SHORTS & REELS FEED SCANNER
        val sealShorts = FocusRepository.sealYoutubeShorts.value
        val sealReels = FocusRepository.sealInstagramReels.value
        if (!sealShorts && !sealReels) return

        if (FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)) return

        val isYoutube = targetPkg.equals("com.google.android.youtube", ignoreCase = true) ||
            targetPkg.contains("youtube", ignoreCase = true)
        val isInstagram = targetPkg.equals("com.instagram.android", ignoreCase = true) ||
            targetPkg.contains("instagram", ignoreCase = true)

        if (!isYoutube && !isInstagram) return

        val rootNodes = collectActiveRootNodes(event)
        if (rootNodes.isEmpty()) return

        for (rootNode in rootNodes) {
            val nodePkg = rootNode.packageName?.toString()?.takeIf { it.isNotBlank() } ?: targetPkg
            if (FocusRepository.isPackageTemporarilyUnsealed(nodePkg, now)) continue

            if (sealShorts && isYoutube && isYoutubeShortsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 800L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> Kick-Out")
                performGlobalAction(GLOBAL_ACTION_BACK)
                displaySystemPenaltyOverlayInsideApp(
                    targetPackage = nodePkg,
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ SHORT-FORM FEED SEALED BY SYSTEM ]",
                    isFeedShield = true
                )
                return
            }

            if (sealReels && isInstagram && isInstagramReelsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 800L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> Kick-Out")
                performGlobalAction(GLOBAL_ACTION_BACK)
                displaySystemPenaltyOverlayInsideApp(
                    targetPackage = nodePkg,
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ SHORT-FORM FEED SEALED BY SYSTEM ]",
                    isFeedShield = true
                )
                return
            }
        }
    }

    fun handleAppInterception(targetPackage: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockActionTime < 450L) {
            return
        }
        lastBlockActionTime = now

        FocusRepository.recordBlockedInterception("Sealed App Intercepted: $targetPackage")

        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(applicationContext)
        } else {
            true
        }

        if (canOverlay) {
            // Display the full-screen System Penalty HUD overlay DIRECTLY ON TOP OF THE BLOCKED APP!
            displaySystemPenaltyOverlayInsideApp(
                targetPackage = targetPackage,
                headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                subtitle = "[ ACTIVE DUNGEON LOCK ENFORCED ]",
                isFeedShield = false
            )
        } else {
            // Fallback if Display Over Other Apps permission is missing
            performGlobalAction(GLOBAL_ACTION_HOME)
            Toast.makeText(
                applicationContext,
                "[ ! SYSTEM PENALTY: ACCESS DENIED ] Please grant Display Over Other Apps",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Draws an interactive full-screen WindowManager overlay directly over the blocked app.
     * Contains 5s mindful pause, Return to Home, and Token Unseal actions.
     */
    private fun displaySystemPenaltyOverlayInsideApp(
        targetPackage: String,
        headerTitle: String,
        subtitle: String,
        isFeedShield: Boolean
    ) {
        mainHandler.post {
            try {
                removePenaltyOverlay()
                val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post
                val appLabel = resolveAppLabel(applicationContext, targetPackage)
                val vaultTokens = FocusRepository.rewardTokens.value

                val overlayContainer = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#F5050811"))
                    setPadding(40, 48, 40, 48)
                    isClickable = true
                    isFocusable = true
                }

                val cardBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#FA1A050D"))
                    setStroke(5, Color.parseColor("#FFFF1744"))
                    cornerRadius = 14f
                }

                val innerBox = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg
                    setPadding(40, 48, 40, 48)
                }

                val headerView = TextView(applicationContext).apply {
                    text = headerTitle
                    setTextColor(Color.parseColor("#FFFF1744"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                    gravity = Gravity.CENTER
                    letterSpacing = 0.08f
                }

                val subHeaderView = TextView(applicationContext).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#FFFFD600"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    setPadding(0, 16, 0, 16)
                }

                // App Target Capsule
                val targetCapsule = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#26FF1744"))
                        setStroke(2, Color.parseColor("#80FF1744"))
                        cornerRadius = 8f
                    }
                    setPadding(32, 16, 32, 16)
                }

                val targetAppNameView = TextView(applicationContext).apply {
                    text = appLabel.uppercase()
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    gravity = Gravity.CENTER
                }

                val targetPkgView = TextView(applicationContext).apply {
                    text = targetPackage
                    setTextColor(Color.parseColor("#FF8FA3BF"))
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    gravity = Gravity.CENTER
                }

                targetCapsule.addView(targetAppNameView)
                targetCapsule.addView(targetPkgView)

                // 5-second countdown meter
                val countdownView = TextView(applicationContext).apply {
                    text = "Breathe... Stay disciplined (5s)"
                    setTextColor(Color.parseColor("#FF00E5FF"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    setPadding(0, 20, 0, 20)
                }

                // Action Button 1: Return to Home
                val returnHomeBtn = TextView(applicationContext).apply {
                    text = "[ RETURN TO HOME SCREEN ]"
                    setTextColor(Color.parseColor("#FFFF1744"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#33FF1744"))
                        setStroke(2, Color.parseColor("#FFFF1744"))
                        cornerRadius = 8f
                    }
                    setPadding(32, 18, 32, 18)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        removePenaltyOverlay()
                    }
                }

                // Action Button 2: Quick Token Unseal (if user has >= 100 tokens)
                var quickUnsealBtn: TextView? = null
                if (vaultTokens >= 100 && !isFeedShield) {
                    quickUnsealBtn = TextView(applicationContext).apply {
                        text = "[ UNSEAL 10 MIN (100 / $vaultTokens TOKENS) ]"
                        setTextColor(Color.parseColor("#FFFFD600"))
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        gravity = Gravity.CENTER
                        background = GradientDrawable().apply {
                            setColor(Color.parseColor("#26FFD600"))
                            setStroke(2, Color.parseColor("#FFFFD600"))
                            cornerRadius = 8f
                        }
                        setPadding(28, 16, 28, 16)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            FocusRepository.redeemTokensForApp(targetPackage, appLabel, 10)
                            removePenaltyOverlay()
                            Toast.makeText(
                                applicationContext,
                                "$appLabel unsealed for 10 minutes",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // Action Button 3: Open Focus HUD
                val openHudBtn = TextView(applicationContext).apply {
                    text = "[ OPEN SYSTEM FOCUS HUD ]"
                    setTextColor(Color.parseColor("#FF00E5FF"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#2600E5FF"))
                        setStroke(2, Color.parseColor("#FF00E5FF"))
                        cornerRadius = 8f
                    }
                    setPadding(28, 14, 28, 14)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        val hudIntent = Intent(applicationContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        applicationContext.startActivity(hudIntent)
                        removePenaltyOverlay()
                    }
                }

                val spacer1 = View(applicationContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)
                }
                val spacer2 = View(applicationContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16)
                }
                val spacer3 = View(applicationContext).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 12)
                }

                innerBox.addView(headerView)
                innerBox.addView(subHeaderView)
                innerBox.addView(targetCapsule)
                innerBox.addView(countdownView)
                innerBox.addView(returnHomeBtn)
                if (quickUnsealBtn != null) {
                    innerBox.addView(spacer2)
                    innerBox.addView(quickUnsealBtn)
                }
                innerBox.addView(spacer3)
                innerBox.addView(openHudBtn)

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
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                wm.addView(overlayContainer, params)
                activePenaltyOverlayView = overlayContainer

                // Auto kick-out countdown timer for 3 seconds
                activeCountdownTimer?.cancel()
                activeCountdownTimer = object : CountDownTimer(3000L, 1000L) {
                    override fun onTick(millisUntilFinished: Long) {
                        val secs = (millisUntilFinished / 1000L).toInt() + 1
                        countdownView.text = "System Kick-Out in ${secs}s..."
                    }

                    override fun onFinish() {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        removePenaltyOverlay()
                    }
                }.start()

            } catch (_: Exception) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun removePenaltyOverlay() {
        activeCountdownTimer?.cancel()
        activeCountdownTimer = null
        val view = activePenaltyOverlayView ?: return
        activePenaltyOverlayView = null
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeView(view)
        } catch (_: Exception) {
        }
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
            (homePkg != null && pkg == homePkg)
    }

    private fun collectActiveRootNodes(event: AccessibilityEvent?): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        try {
            rootInActiveWindow?.let { nodes.add(it) }
        } catch (_: Exception) {
        }
        try {
            event?.source?.let { src ->
                if (nodes.none { it == src }) {
                    nodes.add(src)
                }
            }
        } catch (_: Exception) {
        }
        try {
            windows?.forEach { windowInfo ->
                val winRoot = windowInfo.root
                if (winRoot != null && nodes.none { it == winRoot }) {
                    nodes.add(winRoot)
                }
            }
        } catch (_: Exception) {
        }
        return nodes
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
        if (node == null || depth > 24) return false

        val viewId = node.viewIdResourceName.orEmpty().lowercase()
        if (viewId.isNotEmpty() && viewIdSignals.any { signal -> viewId.contains(signal) }) {
            return true
        }

        val textStr = node.text?.toString()?.trim().orEmpty()
        val descStr = node.contentDescription?.toString()?.trim().orEmpty()

        if (node.isSelected) {
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
        mainHandler.removeCallbacks(dismissOverlayRunnable)
        removePenaltyOverlay()
        super.onDestroy()
    }

    override fun onInterrupt() {
        removePenaltyOverlay()
    }
}
