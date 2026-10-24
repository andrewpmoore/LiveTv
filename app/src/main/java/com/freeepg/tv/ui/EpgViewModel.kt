package com.freeepg.tv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.data.repository.EpgRepository
import com.freeepg.tv.data.repository.ChannelSortOrder
import com.freeepg.tv.data.repository.SettingsRepository
import com.freeepg.tv.deeplink.DeepLinkManager
import com.freeepg.tv.deeplink.LaunchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class EpgUiState(
    val selectedBroadcaster: Broadcaster? = null,
    val enabledBroadcasters: Set<Broadcaster> = SettingsRepository.DEFAULT_BROADCASTERS,
    val channelSortOrder: ChannelSortOrder = ChannelSortOrder.NUMBER,
    val channels: List<Channel> = Channel.ALL_CHANNELS,
    val scheduleMap: Map<String, List<Programme>> = emptyMap(),
    val focusedProgramme: Programme? = null,
    val focusedChannel: Channel? = null,
    val currentTimeSeconds: Long = System.currentTimeMillis() / 1000,
    val timelineStartSeconds: Long = computeTimelineStart(System.currentTimeMillis() / 1000),
    val totalSlots: Int = 52, // 26 hours of 30-min slots
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val settingsFocusRequestId: Int = 0,
    val isMacroEnabled: Boolean = true,
    val isAccessibilityServiceActive: Boolean = false,
    val notInstalledDialog: LaunchResult.AppNotInstalled? = null
) {
    companion object {
        fun computeTimelineStart(nowSeconds: Long): Long {
            // Start 2 hours before now, rounded down to nearest 30 minutes (1800s)
            val twoHoursAgo = nowSeconds - 7200
            return (twoHoursAgo / 1800) * 1800
        }
    }
}

class EpgViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EpgRepository(application)
    val settingsRepository = SettingsRepository(application)
    val deepLinkManager = DeepLinkManager(application)

    private val _uiState = MutableStateFlow(
        EpgUiState(
            enabledBroadcasters = settingsRepository.getEnabledBroadcasters(),
            channelSortOrder = settingsRepository.getChannelSortOrder(),
            channels = settingsRepository.getFilteredChannels(null)
        )
    )
    val uiState: StateFlow<EpgUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null

    init {
        startClockTicker()
        loadEpgData()
    }

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000) // Update every 30 seconds to minimize recomposition churn on TV chips
                val nowSec = System.currentTimeMillis() / 1000
                _uiState.update { it.copy(currentTimeSeconds = nowSec) }
            }
        }
    }

    fun loadEpgData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val activeChannels = settingsRepository.getFilteredChannels(null)
                val schedule = repository.loadSchedule(
                    forceRefresh = forceRefresh,
                    channels = activeChannels
                )

                // Select initial focused programme on the first enabled channel
                val firstChannel = _uiState.value.channels.firstOrNull()
                val liveProg = firstChannel?.let { ch ->
                    schedule[ch.id]?.firstOrNull { it.isLiveNow(_uiState.value.currentTimeSeconds) }
                        ?: schedule[ch.id]?.firstOrNull()
                }

                _uiState.update { current ->
                    current.copy(
                        scheduleMap = schedule,
                        isLoading = false,
                        focusedProgramme = current.focusedProgramme ?: liveProg,
                        focusedChannel = current.focusedChannel ?: firstChannel
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load TV guide: ${e.message}"
                    )
                }
            }
        }
    }

    fun selectBroadcaster(broadcaster: Broadcaster?) {
        val filtered = settingsRepository.getFilteredChannels(broadcaster)
        _uiState.update { current ->
            val firstChannel = filtered.firstOrNull()
            val liveProg = firstChannel?.let { ch ->
                current.scheduleMap[ch.id]?.firstOrNull { it.isLiveNow(current.currentTimeSeconds) }
            }
            current.copy(
                selectedBroadcaster = broadcaster,
                channels = filtered,
                focusedChannel = firstChannel ?: current.focusedChannel,
                focusedProgramme = liveProg ?: current.focusedProgramme
            )
        }
    }

    fun setChannelSortOrder(sortOrder: ChannelSortOrder) {
        settingsRepository.setChannelSortOrder(sortOrder)
        val current = _uiState.value
        val sortedChannels = settingsRepository.getFilteredChannels(current.selectedBroadcaster, sortOrder)
        val firstChannel = sortedChannels.firstOrNull()
        val firstProgramme = firstChannel?.let { channel ->
            current.scheduleMap[channel.id]?.firstOrNull { it.isLiveNow(current.currentTimeSeconds) }
        }

        _uiState.update {
            it.copy(
                channelSortOrder = sortOrder,
                channels = sortedChannels,
                focusedChannel = firstChannel ?: it.focusedChannel,
                focusedProgramme = firstProgramme
            )
        }
    }

    fun openSettings() {
        refreshAccessibilityStatus(showDialog = true)
    }

    fun refreshAccessibilityStatus(showDialog: Boolean? = null) {
        val isServiceActive = com.freeepg.tv.macro.MacroManager.isAccessibilityServiceEnabled(getApplication())
        val isMacroPref = settingsRepository.isMacroAutoLaunchEnabled()
        _uiState.update {
            it.copy(
                showSettingsDialog = showDialog ?: it.showSettingsDialog,
                settingsFocusRequestId = if (showDialog == null && it.showSettingsDialog) {
                    it.settingsFocusRequestId + 1
                } else {
                    it.settingsFocusRequestId
                },
                isAccessibilityServiceActive = isServiceActive,
                isMacroEnabled = isMacroPref && isServiceActive
            )
        }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun toggleMacroAutoLaunch() {
        val isServiceActive = com.freeepg.tv.macro.MacroManager.isAccessibilityServiceEnabled(getApplication())
        if (!isServiceActive) {
            openAccessibilitySettings()
            return
        }
        val newState = settingsRepository.toggleMacroAutoLaunch()
        _uiState.update { it.copy(isMacroEnabled = newState) }
    }

    fun openAccessibilitySettings() {
        com.freeepg.tv.macro.MacroManager.openAccessibilitySettings(getApplication())
    }

    fun toggleBroadcaster(broadcaster: Broadcaster) {
        val updatedBroadcasters = settingsRepository.toggleBroadcaster(broadcaster)
        val selectedBroadcaster = if (_uiState.value.selectedBroadcaster in updatedBroadcasters) {
            _uiState.value.selectedBroadcaster
        } else {
            null
        }

        val updatedChannels = settingsRepository.getFilteredChannels(selectedBroadcaster)
        val currentFocusedChannel = _uiState.value.focusedChannel
        val newFocusedChannel = if (currentFocusedChannel != null && currentFocusedChannel in updatedChannels) {
            currentFocusedChannel
        } else {
            updatedChannels.firstOrNull()
        }

        val liveProg = newFocusedChannel?.let { ch ->
            _uiState.value.scheduleMap[ch.id]?.firstOrNull { it.isLiveNow(_uiState.value.currentTimeSeconds) }
        }

        _uiState.update { current ->
            current.copy(
                enabledBroadcasters = updatedBroadcasters,
                selectedBroadcaster = selectedBroadcaster,
                channels = updatedChannels,
                focusedChannel = newFocusedChannel,
                focusedProgramme = liveProg ?: current.focusedProgramme
            )
        }

        // Trigger schedule refresh for any newly enabled channels
        loadEpgData(forceRefresh = false)
    }

    fun onProgrammeFocused(programme: Programme, channel: Channel) {
        _uiState.update {
            it.copy(
                focusedProgramme = programme,
                focusedChannel = channel
            )
        }
    }

    fun onChannelFocused(channel: Channel) {
        _uiState.update {
            it.copy(
                focusedProgramme = null,
                focusedChannel = channel
            )
        }
    }

    /**
     * Handles programme click:
     * Immediately fires the deep-link into the broadcaster app / live stream.
     */
    fun onProgrammeClick(programme: Programme, channel: Channel) {
        val result = deepLinkManager.launchLiveProgramme(channel, programme)

        when (result) {
            is LaunchResult.Success -> {
                // Successfully launched directly into broadcaster app stream
            }
            is LaunchResult.AppNotInstalled -> {
                // Show installation prompt
                _uiState.update { it.copy(notInstalledDialog = result) }
            }
            is LaunchResult.Failure -> {
                _uiState.update { it.copy(errorMessage = result.reason) }
                android.widget.Toast.makeText(
                    getApplication(),
                    result.reason,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun dismissNotInstalledDialog() {
        _uiState.update { it.copy(notInstalledDialog = null) }
    }

    fun installApp(packageName: String) {
        deepLinkManager.openPlayStore(packageName)
        dismissNotInstalledDialog()
    }

    fun openWebLive(url: String) {
        deepLinkManager.openWebLiveStream(url)
        dismissNotInstalledDialog()
    }
}
