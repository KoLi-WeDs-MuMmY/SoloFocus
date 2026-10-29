package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.FocusRepository
import com.example.data.TimerPhase

class FocusBlockerService : AccessibilityService() {

    private var lastActionTimestampMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusRepository.initialize(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Never block our own System HUD app or system UI / settings
        if (packageName == applicationContext.packageName ||
            packageName == "com.android.systemui" ||
            packageName == "com.android.settings"
        ) {
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastActionTimestampMs < 650L) {
            return
        }

        val isRewardUnlocked = FocusRepository.timerPhase.value == TimerPhase.REWARD_UNLOCK
        val isBlockingActive = FocusRepository.isBlockingActive.value
        val blockedApps = FocusRepository.blockedPackages.value

        // 1. Full App Blocking during active Dungeon Lock (disabled during unlocked Reward Time)
        if (isBlockingActive && !isRewardUnlocked && blockedApps.contains(packageName)) {
            lastActionTimestampMs = now
            FocusRepository.recordBlockedInterception("Sealed App Intercepted: $packageName")
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        // 2. YouTube Shorts & Instagram Reels node-tree inspection
        if (!isRewardUnlocked) {
            val sealShorts = FocusRepository.sealYoutubeShorts.value
            val sealReels = FocusRepository.sealInstagramReels.value

            if ((sealShorts || sealReels)) {
                val rootNode = rootInActiveWindow ?: event.source
                if (rootNode != null) {
                    try {
                        if (sealShorts && isYoutubeShortsNodePresent(packageName, rootNode)) {
                            lastActionTimestampMs = now
                            FocusRepository.recordBlockedInterception("YouTube Shorts Sealed -> Back triggered")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            return
                        }
                        if (sealReels && isInstagramReelsNodePresent(packageName, rootNode)) {
                            lastActionTimestampMs = now
                            FocusRepository.recordBlockedInterception("Instagram Reels Sealed -> Back triggered")
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            return
                        }
                    } finally {
                        // Safe node cleanup handled by GC on modern Android API levels
                    }
                }
            }
        }
    }

    private fun isYoutubeShortsNodePresent(
        packageName: String,
        root: AccessibilityNodeInfo
    ): Boolean {
        val isYoutube = packageName.contains("youtube", ignoreCase = true)
        if (!isYoutube && !containsViewIdOrText(root, "reel_player", null)) {
            return false
        }
        return containsViewIdOrText(
            node = root,
            viewIdKeyword = "reel_player",
            textOrDescKeyword = "Shorts"
        )
    }

    private fun isInstagramReelsNodePresent(
        packageName: String,
        root: AccessibilityNodeInfo
    ): Boolean {
        val isInstagram = packageName.contains("instagram", ignoreCase = true)
        if (!isInstagram && !containsViewIdOrText(root, "clips_viewer", null)) {
            return false
        }
        return containsViewIdOrText(
            node = root,
            viewIdKeyword = "clips_viewer",
            textOrDescKeyword = "Reels"
        )
    }

    private fun containsViewIdOrText(
        node: AccessibilityNodeInfo?,
        viewIdKeyword: String,
        textOrDescKeyword: String?,
        depth: Int = 0
    ): Boolean {
        if (node == null || depth > 18) return false

        val viewId = node.viewIdResourceName.orEmpty()
        if (viewId.contains(viewIdKeyword, ignoreCase = true)) {
            return true
        }

        if (textOrDescKeyword != null) {
            val contentDesc = node.contentDescription?.toString().orEmpty()
            val nodeText = node.text?.toString().orEmpty()
            if (node.isSelected &&
                (contentDesc.equals(textOrDescKeyword, ignoreCase = true) ||
                    nodeText.equals(textOrDescKeyword, ignoreCase = true))
            ) {
                return true
            }
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            if (containsViewIdOrText(child, viewIdKeyword, textOrDescKeyword, depth + 1)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        // System Accessibility Service interrupted
    }
}
