package com.freeepg.tv.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.freeepg.tv.ui.components.AppNotInstalledDialog
import com.freeepg.tv.ui.components.BroadcasterFilterBar
import com.freeepg.tv.ui.components.EpgGrid
import com.freeepg.tv.ui.components.HeroProgrammePane
import com.freeepg.tv.ui.components.SLOT_DURATION_SECONDS
import com.freeepg.tv.ui.components.SLOT_WIDTH_30MIN
import com.freeepg.tv.ui.components.SettingsDialog
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.BgDark
import com.freeepg.tv.ui.theme.FreeEpgTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: EpgViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FreeEpgTheme {
                MainEpgScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAccessibilityStatus()
    }
}

@Composable
fun MainEpgScreen(viewModel: EpgViewModel) {
    val state by viewModel.uiState.collectAsState()
    val horizontalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val firstChannelFocusRequester = remember { FocusRequester() }

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Helper function to scroll timeline to "NOW"
    fun scrollToNow(animate: Boolean = false) {
        val nowElapsedSeconds = state.currentTimeSeconds - state.timelineStartSeconds
        if (nowElapsedSeconds > 0) {
            val slotWidthPx = with(density) { SLOT_WIDTH_30MIN.toPx() }
            val targetPx = ((nowElapsedSeconds.toFloat() / SLOT_DURATION_SECONDS.toFloat()) * slotWidthPx).toInt()
            val leadOffsetPx = with(density) { 160.dp.roundToPx() }
            val scrollDestination = maxOf(0, targetPx - leadOffsetPx)
            coroutineScope.launch {
                if (animate) {
                    horizontalScrollState.animateScrollTo(scrollDestination)
                } else {
                    horizontalScrollState.scrollTo(scrollDestination)
                }
            }
        }
    }

    // Scroll to Now on initial load once schedules arrive
    LaunchedEffect(state.scheduleMap.isNotEmpty()) {
        if (state.scheduleMap.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            scrollToNow(animate = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Unified Top Area (Filter bar + Program detail section seamlessly sharing the gradient)
        val brandColor = state.focusedChannel?.broadcaster?.brandColor ?: AccentCyan

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color(0xFF162032), // Luminous navy gradient top
                            Color(0xFF0F1523), // Mid gradient
                            Color(0xFF090D14)  // Base gradient blending into grid
                        )
                    )
                )
        ) {
            // Ambient broadcaster brand glow seamlessly expanding across the entire top & detail area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                brandColor.copy(alpha = 0.22f),
                                brandColor.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(260f, 90f),
                            radius = 650f
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Broadcaster Filter & App Header
                BroadcasterFilterBar(
                    onNavigateToGuide = {
                        coroutineScope.launch {
                            repeat(20) {
                                try {
                                    firstChannelFocusRequester.requestFocus()
                                    return@launch
                                } catch (_: IllegalStateException) {
                                }
                                kotlinx.coroutines.delay(50)
                            }
                        }
                    },
                    selectedBroadcaster = state.selectedBroadcaster,
                    enabledBroadcasters = state.enabledBroadcasters,
                    onSelectBroadcaster = { broadcaster ->
                        viewModel.selectBroadcaster(broadcaster)
                    },
                    onRefreshClick = {
                        viewModel.loadEpgData(forceRefresh = true)
                    },
                    onJumpToNowClick = {
                        scrollToNow(animate = true)
                    },
                    onOpenSettingsClick = {
                        viewModel.openSettings()
                    },
                    isLoading = state.isLoading,
                    currentTimeSeconds = state.currentTimeSeconds
                )

                // 2. Dynamic Hero Programme Detail Pane (Updates as you move around with D-pad)
                HeroProgrammePane(
                    programme = state.focusedProgramme,
                    channel = state.focusedChannel,
                    currentTimeSeconds = state.currentTimeSeconds,
                    onWatchLiveClick = {
                        val prog = state.focusedProgramme
                        val ch = state.focusedChannel
                        if (prog != null && ch != null) {
                            viewModel.onProgrammeClick(prog, ch)
                        }
                    }
                )
            }
        }

        // 3. EPG Guide Grid with synchronized time header and channels column
        EpgGrid(
            firstChannelFocusRequester = firstChannelFocusRequester,
            channels = state.channels,
            scheduleMap = state.scheduleMap,
            currentTimeSeconds = state.currentTimeSeconds,
            timelineStartSeconds = state.timelineStartSeconds,
            totalSlots = state.totalSlots,
            horizontalScrollState = horizontalScrollState,
            onProgrammeFocused = { prog, ch ->
                viewModel.onProgrammeFocused(prog, ch)
            },
            onChannelFocused = { channel ->
                viewModel.onChannelFocused(channel)
            },
            onProgrammeClick = { prog, ch ->
                viewModel.onProgrammeClick(prog, ch)
            },
            isLoading = state.isLoading,
            modifier = Modifier.weight(1f)
        )
    }

    // Modal Settings Dialog
    if (state.showSettingsDialog) {
        SettingsDialog(
            enabledBroadcasters = state.enabledBroadcasters,
            channelSortOrder = state.channelSortOrder,
            showAdultChannels = state.showAdultChannels,
            focusRequestId = state.settingsFocusRequestId,
            isMacroEnabled = state.isMacroEnabled,
            isAccessibilityServiceActive = state.isAccessibilityServiceActive,
            tunerChannelsCount = state.tunerChannels.size,
            tunerName = state.tunerName,
            tunerIp = state.tunerIp,
            isScanningTuner = state.isScanningTuner,
            onToggleBroadcaster = { broadcaster ->
                viewModel.toggleBroadcaster(broadcaster)
            },
            onChannelSortOrderSelected = { sortOrder ->
                viewModel.setChannelSortOrder(sortOrder)
            },
            onToggleAdultChannels = {
                viewModel.toggleAdultChannels()
            },
            onToggleMacro = {
                viewModel.toggleMacroAutoLaunch()
            },
            onOpenAccessibilitySettings = {
                viewModel.openAccessibilitySettings()
            },
            onScanTuner = {
                viewModel.scanForTuner(silent = false)
            },
            onDismiss = {
                viewModel.closeSettings()
            }
        )
    }

    // Modal dialog when broadcaster app is not installed
    state.notInstalledDialog?.let { request ->
        AppNotInstalledDialog(
            request = request,
            onInstallClick = { pkg ->
                viewModel.installApp(pkg)
            },
            onDismiss = {
                viewModel.dismissNotInstalledDialog()
            }
        )
    }
}
