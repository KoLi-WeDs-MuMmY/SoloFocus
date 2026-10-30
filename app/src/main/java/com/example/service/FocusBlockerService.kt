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
import com.example.data.FocusRepository
import com.example.ui.BlockerActivity

class FocusBlockerService : AccessibilityService() {

    private var lastBlockActionTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cachedHomePackage: String? = null
    private var activePenaltyOverlayView: View? = null
    private val dismissOverlayRunnable = Runnable { removePenaltyOverlay() }

    override fun onServiceConnected() {
        super.onServiceConnected()
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
            info.notificationTimeout = 40L
            info.packageNames = null // Monitor all applications globally
            serviceInfo = info
        } catch (_: Exception) {
            // Fallback to XML configuration if OEM restricts runtime update
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val ownPackage = applicationContext.packageName
        val eventPkg = event.packageName?.toString()?.trim()

        // 1. RECURSION GUARD: If the event originates from our own application or BlockerActivity,
        // NEVER trigger blocking!
        if (!eventPkg.isNullOrBlank() && (
                eventPkg == ownPackage ||
                eventPkg == "com.example" ||
                eventPkg.startsWith("com.aistudio.systemfocus") ||
                eventPkg.contains("systemfocus", ignoreCase = true)
            )
        ) {
            return
        }

        // Also verify rootInActiveWindow: if our own app is currently in foreground, abort
        val activeWindowPkg = try {
            rootInActiveWindow?.packageName?.toString()?.trim()
        } catch (_: Exception) {
            null
        }

        if (!activeWindowPkg.isNullOrBlank() && (
                activeWindowPkg == ownPackage ||
                activeWindowPkg == "com.example" ||
                activeWindowPkg.startsWith("com.aistudio.systemfocus") ||
                activeWindowPkg.contains("systemfocus", ignoreCase = true)
            )
        ) {
            return
        }

        val targetPkg = eventPkg ?: activeWindowPkg ?: return

        // 2. SYSTEM GUARDS: Never block System UI, Settings, Permission Controller, Keyboard, or Home Launcher
        if (isExemptSystemOrSelfPackage(targetPkg)) {
            return
        }

        // Ensure live SharedPreferences and session state is loaded
        FocusRepository.init(applicationContext)

        val now = System.currentTimeMillis()
        val isBlockingActive = FocusRepository.isDungeonLockActive(now)
        val blockedPackages = FocusRepository.blockedPackages.value

        // 3. ROCK-SOLID BACKGROUND APP BLOCKING:
        // Works seamlessly whether our app is in background, minimized, or killed
        if (isBlockingActive &&
            blockedPackages.contains(targetPkg) &&
            !FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)
        ) {
            if (now - lastBlockActionTime < 500L) {
                return
            }
            lastBlockActionTime = now

            FocusRepository.recordBlockedInterception("Sealed App Intercepted: $targetPkg")

            // CRITICAL STEP 1: AccessibilityService system-level kick to Android Home screen!
            // This is universally permitted by Android OS and works instantly from the background.
            performGlobalAction(GLOBAL_ACTION_HOME)

            // CRITICAL STEP 2: Display the full-screen Crimson System Penalty Overlay
            triggerSystemPenaltyOverlayOrToast(
                headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                subtitle = "[ ! SYSTEM PENALTY: DUNGEON LOCK ACTIVE ]",
                detail = "TARGET SEALED: $targetPkg\nSpend Reward Tokens to unseal this app or complete your Focus session."
            )

            // CRITICAL STEP 3: Attempt to launch BlockerActivity from Home screen
            mainHandler.postDelayed({
                launchBlockerActivity(
                    targetPackage = targetPkg,
                    reason = "SEALED_APP"
                )
            }, 120L)
            return
        }

        // 4. REAL SHORTS & REELS BLOCKING (Multi-Window Node Scanner)
        val sealShorts = FocusRepository.sealYoutubeShorts.value
        val sealReels = FocusRepository.sealInstagramReels.value

        if (!sealShorts && !sealReels) {
            return
        }

        // Respect per-app unseal for YouTube or Instagram
        if (FocusRepository.isPackageTemporarilyUnsealed(targetPkg, now)) {
            return
        }

        val isYoutube = targetPkg.equals("com.google.android.youtube", ignoreCase = true) ||
            targetPkg.contains("youtube", ignoreCase = true)
        val isInstagram = targetPkg.equals("com.instagram.android", ignoreCase = true) ||
            targetPkg.contains("instagram", ignoreCase = true)

        if (!isYoutube && !isInstagram) {
            return
        }

        val rootNodes = collectActiveRootNodes(event)
        if (rootNodes.isEmpty()) return

        for (rootNode in rootNodes) {
            val nodePkg = rootNode.packageName?.toString()?.takeIf { it.isNotBlank() } ?: targetPkg
            if (FocusRepository.isPackageTemporarilyUnsealed(nodePkg, now)) {
                continue
            }

            if (sealShorts && isYoutube && isYoutubeShortsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 800L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> Kick-Out")

                // If YouTube itself is on the blocked apps list, kick out to Home, otherwise back to normal feed
                if (isBlockingActive && blockedPackages.contains(nodePkg)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }

                triggerSystemPenaltyOverlayOrToast(
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ! SYSTEM PENALTY: SHORTS SEALED ]",
                    detail = "YOUTUBE SHORTS FEED SEALED BY THE SYSTEM.\nShort-form scrolling is forbidden."
                )

                mainHandler.postDelayed({
                    launchBlockerActivity(
                        targetPackage = nodePkg,
                        reason = "SHORTS_REELS"
                    )
                }, 120L)
                return
            }

            if (sealReels && isInstagram && isInstagramReelsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 800L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> Kick-Out")

                if (isBlockingActive && blockedPackages.contains(nodePkg)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                } else {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }

                triggerSystemPenaltyOverlayOrToast(
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ! SYSTEM PENALTY: REELS SEALED ]",
                    detail = "INSTAGRAM REELS FEED SEALED BY THE SYSTEM.\nShort-form scrolling is forbidden."
                )

                mainHandler.postDelayed({
                    launchBlockerActivity(
                        targetPackage = nodePkg,
                        reason = "SHORTS_REELS"
                    )
                }, 120L)
                return
            }
        }
    }

    private fun launchBlockerActivity(targetPackage: String, reason: String) {
        try {
            val intent = Intent(applicationContext, BlockerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(BlockerActivity.EXTRA_BLOCKED_PACKAGE, targetPackage)
                putExtra(BlockerActivity.EXTRA_BLOCK_REASON, reason)
            }
            applicationContext.startActivity(intent)
        } catch (_: Exception) {
            // If background activity start is restricted by OEM, the user is already on the Home screen
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

    private fun triggerSystemPenaltyOverlayOrToast(
        headerTitle: String,
        subtitle: String,
        detail: String
    ) {
        mainHandler.post {
            com.example.util.SystemAuthorityNotificationHelper.postPenaltyAlertNotification(
                context = applicationContext,
                title = headerTitle,
                detail = detail
            )

            val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(applicationContext)
            } else {
                true
            }

            if (!canOverlay) {
                Toast.makeText(
                    applicationContext,
                    "$headerTitle • $subtitle",
                    Toast.LENGTH_SHORT
                ).show()
                return@post
            }

            try {
                removePenaltyOverlay()
                val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

                val overlayContainer = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#F0050811"))
                    setPadding(56, 56, 56, 56)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        removePenaltyOverlay()
                    }
                }

                val cardDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#F51A050D"))
                    setStroke(5, Color.parseColor("#FFFF1744"))
                    cornerRadius = 12f
                }

                val innerBox = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = cardDrawable
                    setPadding(48, 56, 48, 56)
                }

                val alertHeaderView = TextView(applicationContext).apply {
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
                    setPadding(0, 20, 0, 20)
                }

                val detailView = TextView(applicationContext).apply {
                    text = detail
                    setTextColor(Color.parseColor("#FFE8F6FF"))
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                }

                val returnHomeBtn = TextView(applicationContext).apply {
                    text = "[ RETURN TO HOME SCREEN ]"
                    setTextColor(Color.parseColor("#FFFF1744"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                    val btnBg = GradientDrawable().apply {
                        setColor(Color.parseColor("#33FF1744"))
                        setStroke(3, Color.parseColor("#FFFF1744"))
                        cornerRadius = 8f
                    }
                    background = btnBg
                    setPadding(32, 20, 32, 20)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        removePenaltyOverlay()
                    }
                }

                val spacer = View(applicationContext).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        32
                    )
                }

                innerBox.addView(alertHeaderView)
                innerBox.addView(subHeaderView)
                innerBox.addView(detailView)
                innerBox.addView(spacer)
                innerBox.addView(returnHomeBtn)

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
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                wm.addView(overlayContainer, params)
                activePenaltyOverlayView = overlayContainer

                mainHandler.removeCallbacks(dismissOverlayRunnable)
                mainHandler.postDelayed(dismissOverlayRunnable, 3000L)
            } catch (_: Exception) {
                Toast.makeText(
                    applicationContext,
                    "$headerTitle • $subtitle",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removePenaltyOverlay() {
        val view = activePenaltyOverlayView ?: return
        activePenaltyOverlayView = null
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            wm?.removeViewImmediate(view)
        } catch (_: Exception) {
            // View already detached
        }
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
        mainHandler.removeCallbacks(dismissOverlayRunnable)
        removePenaltyOverlay()
        super.onDestroy()
    }

    override fun onInterrupt() {
        removePenaltyOverlay()
    }
}
