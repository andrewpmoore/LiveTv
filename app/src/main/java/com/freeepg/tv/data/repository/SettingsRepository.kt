package com.freeepg.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "freeepg_settings"
        private const val KEY_ENABLED_BROADCASTERS = "enabled_broadcasters"
        private const val KEY_MACRO_AUTO_LAUNCH = "macro_auto_launch_enabled"

        // All broadcasters enabled by default
        val DEFAULT_BROADCASTERS: Set<Broadcaster> = Broadcaster.values().toSet()
        const val DEFAULT_MACRO_AUTO_LAUNCH: Boolean = true
    }

    fun getEnabledBroadcasters(): Set<Broadcaster> {
        val storedNames = prefs.getStringSet(KEY_ENABLED_BROADCASTERS, null)
            ?: return DEFAULT_BROADCASTERS

        val set = storedNames.mapNotNull { name ->
            try {
                Broadcaster.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()

        // Ensure at least one broadcaster remains enabled so the guide is never blank
        return if (set.isEmpty()) DEFAULT_BROADCASTERS else set
    }

    fun isBroadcasterEnabled(broadcaster: Broadcaster): Boolean {
        return getEnabledBroadcasters().contains(broadcaster)
    }

    fun setBroadcasterEnabled(broadcaster: Broadcaster, enabled: Boolean): Set<Broadcaster> {
        val current = getEnabledBroadcasters().toMutableSet()
        if (enabled) {
            current.add(broadcaster)
        } else {
            // Guard: don't allow disabling the last broadcaster
            if (current.size > 1) {
                current.remove(broadcaster)
            }
        }
        saveEnabledBroadcasters(current)
        return current
    }

    fun toggleBroadcaster(broadcaster: Broadcaster): Set<Broadcaster> {
        val isEnabled = isBroadcasterEnabled(broadcaster)
        return setBroadcasterEnabled(broadcaster, !isEnabled)
    }

    private fun saveEnabledBroadcasters(broadcasters: Set<Broadcaster>) {
        val names = broadcasters.map { it.name }.toSet()
        prefs.edit().putStringSet(KEY_ENABLED_BROADCASTERS, names).apply()
    }

    fun getFilteredChannels(selectedBroadcaster: Broadcaster?): List<Channel> {
        val enabledBroadcasters = getEnabledBroadcasters()
        val allEnabledChannels = Channel.ALL_CHANNELS.filter { it.broadcaster in enabledBroadcasters }
        return if (selectedBroadcaster != null && selectedBroadcaster in enabledBroadcasters) {
            allEnabledChannels.filter { it.broadcaster == selectedBroadcaster }
        } else {
            allEnabledChannels
        }
    }

    fun isMacroAutoLaunchEnabled(): Boolean {
        return prefs.getBoolean(KEY_MACRO_AUTO_LAUNCH, DEFAULT_MACRO_AUTO_LAUNCH)
    }

    fun setMacroAutoLaunchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MACRO_AUTO_LAUNCH, enabled).apply()
    }

    fun toggleMacroAutoLaunch(): Boolean {
        val newState = !isMacroAutoLaunchEnabled()
        setMacroAutoLaunchEnabled(newState)
        return newState
    }
}
