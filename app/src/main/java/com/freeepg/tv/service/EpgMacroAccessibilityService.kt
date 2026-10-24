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

            // 2. Execute current macro step
            val currentStep = pending.currentStep ?: return
            Log.d(TAG, "Executing macro step for ${pending.channel.name}: ${currentStep.description}")

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

    /**
     * Recursively searches for a node whose text or contentDescription contains [term] (case-insensitive).
     */
    private fun findMatchingNode(node: AccessibilityNodeInfo, term: String): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        if (text.contains(term, ignoreCase = true) || contentDesc.contains(term, ignoreCase = true)) {
            return AccessibilityNodeInfo.obtain(node)
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val result = findMatchingNode(child, term)
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
