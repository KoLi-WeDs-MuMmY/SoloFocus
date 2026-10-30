package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.data.FocusRepository
import com.example.data.TimerPhase

class FocusBlockerService : AccessibilityService() {

    private var lastBlockActionTime: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cachedHomePackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusRepository.init(applicationContext)
        cachedHomePackage = resolveDefaultHomeLauncherPackage()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Ensure live SharedPreferences and StateFlow sync even if main UI is closed
        FocusRepository.init(applicationContext)

        val eventPackage = event?.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: return

        val homePkg = cachedHomePackage ?: resolveDefaultHomeLauncherPackage().also {
            cachedHomePackage = it
        }

        // Never block our own app, System UI, Settings, or the default Home Launcher
        if (eventPackage == applicationContext.packageName ||
            eventPackage == "com.android.systemui" ||
            eventPackage == "com.android.settings" ||
            (homePkg != null && eventPackage == homePkg)
        ) {
            return
        }

        val now = System.currentTimeMillis()
        // 800ms cooldown debounce check so performGlobalAction does not spam-loop
        if (now - lastBlockActionTime < 800L) {
            return
        }

        val isRewardUnlocked = FocusRepository.timerPhase.value == TimerPhase.REWARD_UNLOCK &&
            !FocusRepository.forceDungeonLockTestMode.value
        if (isRewardUnlocked) {
            return
        }

        val isBlockingActive = FocusRepository.isBlockingActive.value
        val blockedPackages = FocusRepository.blockedPackages.value

        // 1. REAL APP BLOCKING: Immediately send user Home and show System Penalty Toast
        if (isBlockingActive && blockedPackages.contains(eventPackage)) {
            lastBlockActionTime = now
            FocusRepository.recordBlockedInterception("Sealed App Blocked: $eventPackage")
            performGlobalAction(GLOBAL_ACTION_HOME)
            showSystemPenaltyToast("[ ! SYSTEM PENALTY: ACCESS DENIED UNTIL FOCUS COMPLETE ]")
            return
        }

        // 2. REAL SHORTS & REELS BLOCKING (Multi-Window & Multi-Signal Node Scanner)
        val sealShorts = FocusRepository.sealYoutubeShorts.value
        val sealReels = FocusRepository.sealInstagramReels.value

        if (!sealShorts && !sealReels) {
            return
        }

        val rootNodes = collectActiveRootNodes(event)
        if (rootNodes.isEmpty()) return

        for (rootNode in rootNodes) {
            val nodePkg = rootNode.packageName?.toString() ?: eventPackage

            if (sealShorts && isYoutubeShortsDetected(nodePkg, rootNode)) {
                lastBlockActionTime = now
                FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> GLOBAL_ACTION_BACK")
                performGlobalAction(GLOBAL_ACTION_BACK)
                showSystemPenaltyToast("[ ! SYSTEM SEAL: YOUTUBE SHORTS RESTRICTED ]")
                return
            }

            if (sealReels && isInstagramReelsDetected(nodePkg, rootNode)) {
                lastBlockActionTime = now
                FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> GLOBAL_ACTION_BACK")
                performGlobalAction(GLOBAL_ACTION_BACK)
                showSystemPenaltyToast("[ ! SYSTEM SEAL: INSTAGRAM REELS RESTRICTED ]")
                return
            }
        }
    }

    private fun collectActiveRootNodes(event: AccessibilityEvent?): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { nodes.add(it) }
        event?.source?.let { src ->
            if (nodes.none { it == src }) {
                nodes.add(src)
            }
        }
        try {
            windows?.forEach { windowInfo ->
                val winRoot = windowInfo.root
                if (winRoot != null && nodes.none { it == winRoot }) {
                    nodes.add(winRoot)
                }
            }
        } catch (_: Exception) {
            // Ignore if windows list is unavailable on specific OEM builds
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
            viewIdSignals = listOf("reel_player", "shorts_", "reel_recycler", "reel_watch"),
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
            viewIdSignals = listOf("clips_viewer", "reel_viewer", "clips_video", "clips_swipe"),
            selectedTabLabels = listOf("Reels")
        )
    }

    private fun scanNodeTreeForSignals(
        node: AccessibilityNodeInfo?,
        viewIdSignals: List<String>,
        selectedTabLabels: List<String>,
        depth: Int = 0
    ): Boolean {
        if (node == null || depth > 22) return false

        if (node.isVisibleToUser) {
            val viewId = node.viewIdResourceName.orEmpty().lowercase()
            if (viewId.isNotEmpty() && viewIdSignals.any { signal -> viewId.contains(signal) }) {
                return true
            }

            val textStr = node.text?.toString()?.trim().orEmpty()
            val descStr = node.contentDescription?.toString()?.trim().orEmpty()

            if (node.isSelected) {
                for (tabLabel in selectedTabLabels) {
                    if (textStr.equals(tabLabel, ignoreCase = true) ||
                        descStr.equals(tabLabel, ignoreCase = true)
                    ) {
                        return true
                    }
                }
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            if (scanNodeTreeForSignals(child, viewIdSignals, selectedTabLabels, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun showSystemPenaltyToast(message: String) {
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
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

    override fun onInterrupt() {
        // System Accessibility Service interrupted
    }
}
