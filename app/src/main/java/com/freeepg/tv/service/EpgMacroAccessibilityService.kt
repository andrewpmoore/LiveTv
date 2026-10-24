package com.freeepg.tv.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.freeepg.tv.macro.MacroManager
import com.freeepg.tv.data.model.Broadcaster

/**
 * High-performance Accessibility Service that inspects broadcaster app UI
 * and automatically clicks through navigation menus to launch live streams.
 */
class EpgMacroAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "EpgMacroService"
        private const val MIN_ACTION_INTERVAL_MS = 650L
    }

    private var lastActionTimeMs: Long = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "EpgMacroAccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) {
            return
        }

        val pending = MacroManager.getPendingMacro() ?: return
        val eventPackage = event.packageName?.toString() ?: ""

        // Verify that this event belongs to the broadcaster package we are automating
        val matchesPackage = eventPackage == pending.targetPackage ||
                pending.channel.broadcaster.allPackages.contains(eventPackage)

        if (!matchesPackage) return

        // Throttle actions to allow TV UI transitions and animations to complete
        val now = System.currentTimeMillis()
        if (now - lastActionTimeMs < MIN_ACTION_INTERVAL_MS) {
            return
        }

        val rootNode = rootInActiveWindow ?: return

        try {
            // 1. Auto-dismiss common consent / welcome dialogs if present
            if (dismissConsentOrDialogIfPresent(rootNode)) {
                lastActionTimeMs = System.currentTimeMillis()
                return
            }

            // BBC iPlayer pauses at its profile chooser before channel navigation. Select the
            // first existing profile once, then let the pending channel steps continue.
            if (pending.channel.broadcaster == Broadcaster.BBC &&
                handleBbcProfileChooser(rootNode, pending)
            ) {
                lastActionTimeMs = System.currentTimeMillis()
                return
            }

            // 2. Execute current macro step
            val currentStep = pending.currentStep ?: return
            Log.d(TAG, "Executing macro step for ${pending.channel.name}: ${currentStep.description}")

            if (pending.channel.broadcaster == Broadcaster.CHANNEL_5 &&
                pending.targetPackage == "com.channel5.my5"
            ) {
                when (navigateChannel5(rootNode, pending)) {
                    Channel5StepResult.ACTION_ADVANCED -> {
                        lastActionTimeMs = System.currentTimeMillis()
                        return
                    }
                    Channel5StepResult.ACTION_WAITING -> {
                        lastActionTimeMs = System.currentTimeMillis()
                        return
                    }
                    Channel5StepResult.NOT_HANDLED -> Unit
                }
            }

            var actionExecuted = false
            for (term in currentStep.searchTerms) {
                val matchedNode = findMatchingNode(rootNode, term)
                if (matchedNode != null) {
                    val clickableNode = findClickableTarget(matchedNode)
                    if (clickableNode != null) {
                        Log.i(TAG, "Found matching target for '$term' -> clicking ${clickableNode.className}")
                        actionExecuted = performClickOnNode(clickableNode)
                        clickableNode.recycle()
                    } else {
                        // Fallback: Dispatch gesture tap at node center
                        Log.i(TAG, "Found target for '$term' with no clickable container -> dispatching tap gesture")
                        actionExecuted = performTapGesture(matchedNode)
                    }

                    matchedNode.recycle()

                    if (actionExecuted) {
                        lastActionTimeMs = System.currentTimeMillis()
                        MacroManager.advanceStep()
                        break
                    }
                }
            }
        } finally {
            rootNode.recycle()
        }
    }

    private enum class Channel5StepResult { ACTION_ADVANCED, ACTION_WAITING, NOT_HANDLED }

    /** Returns true while the BBC profile chooser is visible so channel matching waits. */
    private fun handleBbcProfileChooser(
        root: AccessibilityNodeInfo,
        pending: com.freeepg.tv.macro.PendingMacro
    ): Boolean {
        val packageId = pending.targetPackage
        val containers = root.findAccessibilityNodeInfosByViewId(
            "$packageId:id/user-switcher_container"
        ) ?: emptyList()
        if (containers.isEmpty()) return false

        val profileList = containers.first()
        containers.drop(1).forEach { it.recycle() }

        if (!pending.firstBbcProfileSelectionRequested) {
            val firstProfileCard = profileList.getChild(0)
            val profileButton = firstProfileCard?.let(::findFirstClickableDescendant)
            firstProfileCard?.recycle()

            if (profileButton != null) {
                val clicked = performClickOnNode(profileButton)
                profileButton.recycle()
                if (clicked) {
                    pending.firstBbcProfileSelectionRequested = true
                    Log.i(TAG, "Selected the first BBC iPlayer profile")
                }
            }
        }

        profileList.recycle()
        return true
    }

    private fun findFirstClickableDescendant(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val viewId = node.viewIdResourceName.orEmpty()
        if (node.isVisibleToUser && node.isClickable &&
            !viewId.contains("avatar-add-") && !viewId.startsWith("edit-")
        ) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val target = findFirstClickableDescendant(child)
            child.recycle()
            if (target != null) return target
        }
        return null
    }

    /**
     * The current 5 TV app omits labels from its Live TV channel tiles. Use its visible
     * channel list order and view IDs instead of guessing from unrelated programme text.
     */
    private fun navigateChannel5(
        root: AccessibilityNodeInfo,
        pending: com.freeepg.tv.macro.PendingMacro
    ): Channel5StepResult {
        val packageId = pending.targetPackage

        if (pending.currentStepIndex == 0) {
            val menuItems = root.findAccessibilityNodeInfosByViewId(
                "$packageId:id/left_navigation_menu_item"
            ) ?: emptyList()
            val playerContainers = root.findAccessibilityNodeInfosByViewId(
                "$packageId:id/c5PlayerParent"
            ) ?: emptyList()
            val livePlayer = playerContainers.isNotEmpty()
            playerContainers.forEach { it.recycle() }

            if (menuItems.isNotEmpty()) {
                val liveTvMenuItem = menuItems.getOrNull(3)
                menuItems.forEach { if (it !== liveTvMenuItem) it.recycle() }
                if (liveTvMenuItem != null) {
                    val clicked = performClickOnNode(liveTvMenuItem)
                    liveTvMenuItem.recycle()
                    if (clicked) {
                        MacroManager.advanceStep()
                        Log.i(TAG, "Opened 5 Live TV menu")
                        return Channel5StepResult.ACTION_ADVANCED
                    }
                }
            }

            if (livePlayer) {
                MacroManager.advanceStep()
                return Channel5StepResult.ACTION_ADVANCED
            }
            return Channel5StepResult.NOT_HANDLED
        }

        if (pending.currentStepIndex == 1) {
            val channelLists = root.findAccessibilityNodeInfosByViewId(
                "$packageId:id/live_channels_list"
            ) ?: emptyList()
            val channelIndex = when (pending.channel.id) {
                "c5" -> 0
                "c5_usa" -> 1
                "c5_star" -> 2
                "c5_action" -> 3
                "c5_drama" -> 4
                else -> -1
            }

            if (channelLists.isNotEmpty() && channelIndex >= 0) {
                val channelList = channelLists.first()
                channelLists.drop(1).forEach { it.recycle() }
                val channelTile = channelList.getChild(channelIndex)
                channelList.recycle()
                if (channelTile != null) {
                    val clicked = performClickOnNode(channelTile)
                    channelTile.recycle()
                    if (clicked) {
                        MacroManager.advanceStep()
                        Log.i(TAG, "Selected 5 live channel index $channelIndex for ${pending.channel.id}")
                        return Channel5StepResult.ACTION_ADVANCED
                    }
                }
                return Channel5StepResult.ACTION_WAITING
            }
            channelLists.forEach { it.recycle() }

            // A tap on the live player reveals the labelled stream cards. Wait for its UI event
            // and then select the channel tile on the next accessibility callback.
            val playerViews = root.findAccessibilityNodeInfosByViewId("$packageId:id/video_view")
                ?: emptyList()
            val playerView = playerViews.firstOrNull { view ->
                val bounds = Rect()
                view.getBoundsInScreen(bounds)
                bounds.width() > 500 && bounds.height() > 500
            }
            playerViews.forEach { if (it !== playerView) it.recycle() }
            if (playerView != null) {
                val tapped = performTapGesture(playerView)
                playerView.recycle()
                if (tapped) return Channel5StepResult.ACTION_WAITING
            }
        }

        return Channel5StepResult.NOT_HANDLED
    }

    /**
     * Recursively searches for a node whose text or contentDescription contains [term] (case-insensitive).
     */
    private fun findMatchingNode(node: AccessibilityNodeInfo, term: String): AccessibilityNodeInfo? {
        return findMatchingNode(node, term, exact = true)
            ?: findMatchingNode(node, term, exact = false)
    }

    private fun findMatchingNode(
        node: AccessibilityNodeInfo,
        term: String,
        exact: Boolean
    ): AccessibilityNodeInfo? {
        if (!node.isVisibleToUser) return null

        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val matches = if (exact) {
            text.equals(term, ignoreCase = true) || contentDesc.equals(term, ignoreCase = true)
        } else {
            text.contains(term, ignoreCase = true) || contentDesc.contains(term, ignoreCase = true)
        }

        if (matches) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val result = findMatchingNode(child, term, exact)
            child.recycle()
            if (result != null) return result
        }

        return null
    }

    /**
     * Traverses upwards to find the nearest clickable or focusable container.
     */
    private fun findClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = AccessibilityNodeInfo.obtain(node)
        var levels = 0

        while (current != null && levels < 5) {
            if (current.isClickable) {
                return current
            }
            val parent = current.parent
            current.recycle()
            current = parent
            levels++
        }

        current?.recycle()
        return null
    }

    /**
     * Attempts standard click and focus actions on the given node.
     */
    private fun performClickOnNode(node: AccessibilityNodeInfo): Boolean {
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        var success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        if (!success) {
            // Fallback to tap gesture at center bounds
            success = performTapGesture(node)
        }
        return success
    }

    /**
     * Dispatches a simulated screen tap at the center coordinate of the node.
     */
    private fun performTapGesture(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (rect.isEmpty || rect.width() <= 0 || rect.height() <= 0) return false

        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 60)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Checks for and dismisses generic modal prompts (e.g. cookie consent, "Accept All", "Dismiss", "Skip").
     */
    private fun dismissConsentOrDialogIfPresent(root: AccessibilityNodeInfo): Boolean {
        val consentTerms = listOf("Accept all", "Accept cookies", "Agree", "Dismiss", "Skip")
        for (term in consentTerms) {
            val node = findMatchingNode(root, term)
            if (node != null) {
                val clickable = findClickableTarget(node)
                if (clickable != null) {
                    Log.i(TAG, "Dismissing popup dialog with '$term'")
                    performClickOnNode(clickable)
                    clickable.recycle()
                    node.recycle()
                    return true
                }
                node.recycle()
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.w(TAG, "EpgMacroAccessibilityService interrupted")
        MacroManager.clearPendingMacro()
    }
}
