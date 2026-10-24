package com.freeepg.tv.macro

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.service.EpgMacroAccessibilityService

/**
 * Defines a step in the automation macro.
 * The service will search for any matching text or content description in the UI hierarchy.
 */
data class MacroStep(
    val searchTerms: List<String>,
    val description: String,
    val delayAfterMs: Long = 700L
)

/**
 * Holds a pending macro instruction to be executed by [EpgMacroAccessibilityService].
 */
data class PendingMacro(
    val channel: Channel,
    val targetPackage: String,
    val steps: List<MacroStep>,
    val timestampMs: Long = System.currentTimeMillis(),
    val ttlMs: Long = 45_000L
) {
    var currentStepIndex: Int = 0
    var firstBbcProfileSelectionRequested: Boolean = false

    val isExpired: Boolean
        get() = (System.currentTimeMillis() - timestampMs) > ttlMs

    val currentStep: MacroStep?
        get() = steps.getOrNull(currentStepIndex)
}

object MacroManager {

    private const val TAG = "MacroManager"

    @Volatile
    private var pendingMacro: PendingMacro? = null

    /**
     * Checks whether FreeEPG's Accessibility Service is currently enabled in Android Settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val expectedComponentName = ComponentName(context, EpgMacroAccessibilityService::class.java)
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledComponent = ComponentName.unflattenFromString(componentNameString)
                if (enabledComponent != null && enabledComponent == expectedComponentName) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status: ${e.message}")
            false
        }
    }

    /**
     * Opens the Android TV Accessibility Settings screen so the user can enable the service.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
        }
    }

    /**
     * Prepares and arms a macro sequence for the given channel and target package.
     */
    fun armMacro(channel: Channel, targetPackage: String) {
        val steps = buildMacroStepsForChannel(channel)
        pendingMacro = PendingMacro(
            channel = channel,
            targetPackage = targetPackage,
            steps = steps
        )
        Log.i(TAG, "Armed macro for ${channel.name} in package $targetPackage with ${steps.size} steps")
    }

    fun getPendingMacro(): PendingMacro? {
        val current = pendingMacro ?: return null
        if (current.isExpired) {
            Log.d(TAG, "Pending macro for ${current.channel.name} expired")
            pendingMacro = null
            return null
        }
        return current
    }

    fun advanceStep() {
        val current = pendingMacro ?: return
        current.currentStepIndex++
        if (current.currentStepIndex >= current.steps.size) {
            Log.i(TAG, "Completed all macro steps for ${current.channel.name}")
            pendingMacro = null
        } else {
            Log.d(TAG, "Advanced to step ${current.currentStepIndex + 1}/${current.steps.size}: ${current.currentStep?.description}")
        }
    }

    fun clearPendingMacro() {
        pendingMacro = null
    }

    /**
     * Builds tailored macro navigation steps for each UK broadcaster app.
     */
    private fun buildMacroStepsForChannel(channel: Channel): List<MacroStep> {
        val steps = mutableListOf<MacroStep>()

        val channelSearchTerms = listOf(
            channel.shortName,
            channel.shortName.replace(" ", ""),
            channel.name.replace(" HD", ""),
            channel.name
        ).distinct()

        when (channel.broadcaster) {
            Broadcaster.BBC -> {
                // Go to the channel list before searching: channel names also appear in home rails.
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Channels"),
                        description = "Open BBC iPlayer channels"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = channelSearchTerms,
                        description = "Select '${channel.shortName}' tile"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Watch Live", "Watch live now", "Play live"),
                        description = "Confirm 'Watch Live'"
                    )
                )
            }

            Broadcaster.ITV -> {
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Live TV", "Channels"),
                        description = "Open ITVX Live TV"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = channelSearchTerms,
                        description = "Select '${channel.shortName}'"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Watch Live", "Watch live now", "Play live"),
                        description = "Confirm 'Watch Live'"
                    )
                )
            }

            Broadcaster.CHANNEL_4 -> {
                // Channel 4 normally launches directly. Keep these steps for app versions that do not.
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Live TV", "Channels"),
                        description = "Navigate to Live or '${channel.shortName}'"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = channelSearchTerms,
                        description = "Select '${channel.shortName}'"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = listOf("Watch Live", "Play", "Watch"),
                        description = "Confirm Live Playback"
                    )
                )
            }

            Broadcaster.CHANNEL_5 -> {
                steps.add(
                    MacroStep(
                        searchTerms = emptyList(),
                        description = "Open 5 live channels"
                    )
                )
                steps.add(
                    MacroStep(
                        searchTerms = emptyList(),
                        description = "Tune to '${channel.shortName}'"
                    )
                )
            }
        }

        return steps
    }
}
