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
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.flags = AccessibilityServiceInfo.DEFAULT or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            info.notificationTimeout = 50L
            info.packageNames = null // Listen globally across all installed packages
            serviceInfo = info
        } catch (_: Exception) {
            // Fallback to XML configuration if OEM restricts runtime update
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val eventPkg = event.packageName?.toString()?.trim()
        if (eventPkg.isNullOrBlank()) return

        // 1. RECURSION GUARD: If the event originates from our own application or BlockerActivity,
        // NEVER trigger blocking!
        val ownPackage = applicationContext.packageName
        if (eventPkg == ownPackage ||
            eventPkg == "com.example" ||
            eventPkg.startsWith("com.aistudio.systemfocus") ||
            eventPkg.contains("systemfocus", ignoreCase = true)
        ) {
            return
        }

        // 2. SYSTEM GUARDS: Never block System UI, Settings, Permission Controller, Keyboard, or Home Launcher
        if (isExemptSystemOrSelfPackage(eventPkg)) {
            return
        }

        // Ensure live SharedPreferences state is loaded
        FocusRepository.init(applicationContext)

        val now = System.currentTimeMillis()
        val isBlockingActive = FocusRepository.isBlockingActive.value
        val blockedPackages = FocusRepository.blockedPackages.value

        // 3. FAST-PATH APP BLOCKING ON WINDOW STATE CHANGED (New Activity / App Foregrounded):
        if (isBlockingActive && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (blockedPackages.contains(eventPkg) &&
                !FocusRepository.isPackageTemporarilyUnsealed(eventPkg, now)
            ) {
                // Debounce check so we don't spam activity launches
                if (now - lastBlockActionTime < 1000L) {
                    return
                }
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("Sealed App Intercepted: $eventPkg")

                // Launch full-screen BlockerActivity with NEW_TASK | CLEAR_TOP
                launchBlockerActivity(
                    targetPackage = eventPkg,
                    reason = "SEALED_APP"
                )

                // Also trigger transient penalty banner
                triggerSystemPenaltyOverlayOrToast(
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ! SYSTEM PENALTY: DUNGEON LOCK ACTIVE ]",
                    detail = "TARGET SEALED: $eventPkg\nSpend Reward Tokens to unseal this app or complete your Focus session."
                )
                return
            }
        }

        // 4. REAL SHORTS & REELS BLOCKING (Multi-Window Node Scanner)
        val sealShorts = FocusRepository.sealYoutubeShorts.value
        val sealReels = FocusRepository.sealInstagramReels.value

        if (!sealShorts && !sealReels) {
            return
        }

        // Respect per-app unseal for YouTube or Instagram
        if (FocusRepository.isPackageTemporarilyUnsealed(eventPkg, now)) {
            return
        }

        // Only scan within YouTube or Instagram packages
        val isYoutube = eventPkg.equals("com.google.android.youtube", ignoreCase = true) ||
            eventPkg.contains("youtube", ignoreCase = true)
        val isInstagram = eventPkg.equals("com.instagram.android", ignoreCase = true) ||
            eventPkg.contains("instagram", ignoreCase = true)

        if (!isYoutube && !isInstagram) {
            return
        }

        val rootNodes = collectActiveRootNodes(event)
        if (rootNodes.isEmpty()) return

        for (rootNode in rootNodes) {
            val nodePkg = rootNode.packageName?.toString()?.takeIf { it.isNotBlank() } ?: eventPkg
            if (FocusRepository.isPackageTemporarilyUnsealed(nodePkg, now)) {
                continue
            }

            if (sealShorts && isYoutube && isYoutubeShortsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 1200L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> Kick-Back")
                performGlobalAction(GLOBAL_ACTION_BACK)

                launchBlockerActivity(
                    targetPackage = nodePkg,
                    reason = "SHORTS_REELS"
                )
                triggerSystemPenaltyOverlayOrToast(
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ! SYSTEM PENALTY: SHORTS SEALED ]",
                    detail = "YOUTUBE SHORTS FEED SEALED BY THE SYSTEM.\nShort-form scrolling is forbidden."
                )
                return
            }

            if (sealReels && isInstagram && isInstagramReelsDetected(nodePkg, rootNode)) {
                if (now - lastBlockActionTime < 1200L) return
                lastBlockActionTime = now

                FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> Kick-Back")
                performGlobalAction(GLOBAL_ACTION_BACK)

                launchBlockerActivity(
                    targetPackage = nodePkg,
                    reason = "SHORTS_REELS"
                )
                triggerSystemPenaltyOverlayOrToast(
                    headerTitle = "[ ! SYSTEM PENALTY: ACCESS DENIED ]",
                    subtitle = "[ ! SYSTEM PENALTY: REELS SEALED ]",
                    detail = "INSTAGRAM REELS FEED SEALED BY THE SYSTEM.\nShort-form scrolling is forbidden."
                )
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
            // Fallback to sending home if activity launching has OEM restriction
            performGlobalAction(GLOBAL_ACTION_HOME)
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
            pkg.contains("launcher", ignoreCase = true) ||
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
                    setBackgroundColor(Color.parseColor("#EB050811"))
                    setPadding(64, 64, 64, 64)
                }

                val cardDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#F51A050D"))
                    setStroke(6, Color.parseColor("#FFFF1744"))
                    cornerRadius = 12f
                }

                val innerBox = LinearLayout(applicationContext).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = cardDrawable
                    setPadding(56, 64, 56, 64)
                }

                val alertHeaderView = TextView(applicationContext).apply {
                    text = headerTitle
                    setTextColor(Color.parseColor("#FFFF1744"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    gravity = Gravity.CENTER
                    letterSpacing = 0.1f
                }

                val subHeaderView = TextView(applicationContext).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#FFFFD600"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    gravity = Gravity.CENTER
                    setPadding(0, 24, 0, 24)
                }

                val detailView = TextView(applicationContext).apply {
                    text = detail
                    setTextColor(Color.parseColor("#FFE8F6FF"))
                    typeface = Typeface.MONOSPACE
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    gravity = Gravity.CENTER
                }

                val countdownNoteView = TextView(applicationContext).apply {
                    text = "[ ENFORCING SYSTEM LOCKOUT... ]"
                    setTextColor(Color.parseColor("#FF00E5FF"))
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    gravity = Gravity.CENTER
                    setPadding(0, 36, 0, 0)
                }

                innerBox.addView(alertHeaderView)
                innerBox.addView(subHeaderView)
                innerBox.addView(detailView)
                innerBox.addView(countdownNoteView)
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
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                }

                wm.addView(overlayContainer, params)
                activePenaltyOverlayView = overlayContainer

                mainHandler.removeCallbacks(dismissOverlayRunnable)
                mainHandler.postDelayed(dismissOverlayRunnable, 2500L)
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
