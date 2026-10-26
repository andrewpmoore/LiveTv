package com.freeepg.tv.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel

enum class ChannelSortOrder(val label: String) {
    PROVIDER("By provider"),
    NAME("Alphabetically"),
    NUMBER("By channel number")
}

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "freeepg_settings"
        private const val KEY_ENABLED_BROADCASTERS = "enabled_broadcasters"
        private const val KEY_MACRO_AUTO_LAUNCH = "macro_auto_launch_enabled"
        private const val KEY_CHANNEL_SORT_ORDER = "channel_sort_order"
        private const val KEY_TUNER_CONFIGURED = "tuner_configured_v1"
        private const val KEY_SHOW_ADULT_CHANNELS = "show_adult_tuner_channels"
        private const val KEY_HIDDEN_TUNER_CHANNELS = "hidden_tuner_channels"
        private const val KEY_TUNER_PLAYER_PACKAGE = "tuner_external_player_package"

        // All broadcasters enabled by default
        val DEFAULT_BROADCASTERS: Set<Broadcaster> = Broadcaster.values().toSet()
        const val DEFAULT_MACRO_AUTO_LAUNCH: Boolean = true
        const val DEFAULT_SHOW_ADULT_CHANNELS: Boolean = false

        fun filterChannels(
            allAppChannels: List<Channel>,
            selectedBroadcaster: Broadcaster?,
            enabledBroadcasters: Set<Broadcaster>,
            sortOrder: ChannelSortOrder,
            tunerChannels: List<Channel> = emptyList(),
            showAdultChannels: Boolean = DEFAULT_SHOW_ADULT_CHANNELS,
            hiddenTunerChannelIds: Set<String> = emptySet()
        ): List<Channel> {
            val allEnabledAppChannels = allAppChannels.filter { it.broadcaster in enabledBroadcasters }
            val unhiddenTunerChannels = tunerChannels.filterNot { it.id in hiddenTunerChannelIds }
            val visibleTunerChannels = if (showAdultChannels) {
                unhiddenTunerChannels
            } else {
                unhiddenTunerChannels.filterNot(::isAdultTunerChannel)
            }

            val filtered = when {
                // "TV Tuner" filter selected in top filter bar
                selectedBroadcaster == Broadcaster.TUNER -> {
                    visibleTunerChannels
                }
                // Specific broadcaster filter (BBC, ITV, Channel 4, Channel 5)
                selectedBroadcaster != null -> {
                    if (selectedBroadcaster in enabledBroadcasters) {
                        allEnabledAppChannels.filter { it.broadcaster == selectedBroadcaster }
                    } else {
                        allEnabledAppChannels
                    }
                }
                // "All Channels" view
                else -> {
                    // If TV Tuner is enabled, include tuner-exclusive channels.
                    // Where there is an overlap, the app version is already in allEnabledAppChannels,
                    // so overlapping tuner channels are omitted to avoid duplicates and prioritize app streams.
                    if (Broadcaster.TUNER in enabledBroadcasters) {
                        val tunerExclusive = visibleTunerChannels.filter { !it.isOverlappingWithApp }
                        allEnabledAppChannels + tunerExclusive
                    } else {
                        allEnabledAppChannels
                    }
                }
            }

            return when (sortOrder) {
                ChannelSortOrder.PROVIDER -> filtered
                ChannelSortOrder.NAME -> filtered.sortedBy { it.name.lowercase() }
                ChannelSortOrder.NUMBER -> filtered.sortedWith(
                    compareBy<Channel> { it.channelNumber.toIntOrNull() ?: Int.MAX_VALUE }
                        .thenBy { it.name.lowercase() }
                )
            }
        }

        private fun isAdultTunerChannel(channel: Channel): Boolean =
            channel.isTunerChannel && channel.channelNumber.toIntOrNull()?.let { it in 670..699 } == true

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
        }.toMutableSet()

        // Migration: Enable TUNER by default on upgrades from earlier app versions
        if (!prefs.contains(KEY_TUNER_CONFIGURED)) {
            set.add(Broadcaster.TUNER)
            saveEnabledBroadcasters(set)
            prefs.edit().putBoolean(KEY_TUNER_CONFIGURED, true).apply()
        }

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

    fun getChannelSortOrder(): ChannelSortOrder {
        val storedValue = prefs.getString(KEY_CHANNEL_SORT_ORDER, ChannelSortOrder.PROVIDER.name)
        return ChannelSortOrder.values().firstOrNull { it.name == storedValue }
            ?: ChannelSortOrder.PROVIDER
    }

    fun setChannelSortOrder(sortOrder: ChannelSortOrder) {
        prefs.edit().putString(KEY_CHANNEL_SORT_ORDER, sortOrder.name).apply()
    }

    fun isAdultChannelsEnabled(): Boolean =
        prefs.getBoolean(KEY_SHOW_ADULT_CHANNELS, DEFAULT_SHOW_ADULT_CHANNELS)

    fun setAdultChannelsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ADULT_CHANNELS, enabled).apply()
    }

    fun getHiddenTunerChannelIds(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN_TUNER_CHANNELS, emptySet())?.toSet() ?: emptySet()

    fun setTunerChannelHidden(channelId: String, hidden: Boolean) {
        val ids = getHiddenTunerChannelIds().toMutableSet()
        if (hidden) ids.add(channelId) else ids.remove(channelId)
        prefs.edit().putStringSet(KEY_HIDDEN_TUNER_CHANNELS, ids).apply()
    }

    fun getTunerExternalPlayerPackage(): String? =
        prefs.getString(KEY_TUNER_PLAYER_PACKAGE, null)

    fun setTunerExternalPlayerPackage(packageName: String?) {
        prefs.edit().putString(KEY_TUNER_PLAYER_PACKAGE, packageName).apply()
    }

    fun getFilteredChannels(
        selectedBroadcaster: Broadcaster?,
        sortOrder: ChannelSortOrder = getChannelSortOrder(),
        tunerChannels: List<Channel> = emptyList()
    ): List<Channel> {
        return filterChannels(
            allAppChannels = Channel.ALL_CHANNELS,
            selectedBroadcaster = selectedBroadcaster,
            enabledBroadcasters = getEnabledBroadcasters(),
            sortOrder = sortOrder,
            tunerChannels = tunerChannels,
            showAdultChannels = isAdultChannelsEnabled(),
            hiddenTunerChannelIds = getHiddenTunerChannelIds()
        )
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
