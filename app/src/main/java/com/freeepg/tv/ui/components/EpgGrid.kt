package com.freeepg.tv.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.AccentLive
import com.freeepg.tv.ui.theme.BgDark
import com.freeepg.tv.ui.theme.BgSurface
import com.freeepg.tv.ui.theme.BorderUnfocused
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TimelineDividerColor
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.ceil

const val SLOT_DURATION_SECONDS: Long = 1800L // 30 minutes
val SLOT_WIDTH_30MIN: Dp = 120.dp

private val HeaderBackgroundBrush = Brush.verticalGradient(
    listOf(Color(0xFF141C2C), Color(0xFF0F1624))
)
private val NowBadgeBrush = Brush.horizontalGradient(
    listOf(AccentLive, Color(0xFFFF5252))
)

@Composable
fun EpgGrid(
    firstChannelFocusRequester: FocusRequester,
    guideFocusRequestId: Int,
    channels: List<Channel>,
    scheduleMap: Map<String, List<Programme>>,
    currentTimeSeconds: Long,
    timelineStartSeconds: Long,
    totalSlots: Int = 52, // 26 hours of 30-min slots
    horizontalScrollState: ScrollState,
    onProgrammeFocused: (Programme, Channel) -> Unit,
    onChannelFocused: (Channel) -> Unit,
    onProgrammeClick: (Programme, Channel) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val channelColumnWidth = 165.dp
    val verticalListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val slotWidthPx = with(density) { SLOT_WIDTH_30MIN.toPx().toInt().coerceAtLeast(1) }
    val firstVisibleSlot by remember(horizontalScrollState, slotWidthPx) {
        derivedStateOf { horizontalScrollState.value / slotWidthPx }
    }
    val visibleSlotCount = ceil(
        (screenWidthDp.dp - channelColumnWidth).coerceAtLeast(0.dp).value / SLOT_WIDTH_30MIN.value
    ).toInt()
    val renderWindowStartSeconds = timelineStartSeconds +
        (max(0, firstVisibleSlot - 2) * SLOT_DURATION_SECONDS)
    val renderWindowEndSeconds = timelineStartSeconds +
        ((firstVisibleSlot + visibleSlotCount + 2).coerceAtMost(totalSlots) * SLOT_DURATION_SECONDS)

    // One focus target per channel, attached to its NOW programme or off-air cell.
    // The first row changes with the broadcaster filter. Rebind its entry requester
    // instead of retaining the requester assigned when this channel was a later row.
    val firstChannelId = channels.firstOrNull()?.id
    val channelFocusRequesters = remember(firstChannelId, firstChannelFocusRequester) {
        mutableMapOf<String, FocusRequester>()
    }
    channels.forEach { ch ->
        if (!channelFocusRequesters.containsKey(ch.id)) {
            channelFocusRequesters[ch.id] = if (ch.id == firstChannelId) firstChannelFocusRequester else FocusRequester()
        }
    }

    // Pending channel target for vertical navigation between NOW programmes (starts on first channel)
    var pendingFocusChannelId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(guideFocusRequestId) {
        if (guideFocusRequestId == 0 || channels.isEmpty()) return@LaunchedEffect
        // The first row may have been recycled after scrolling or changing filters.
        pendingFocusChannelId = null
        verticalListState.scrollToItem(0)
        repeat(20) {
            androidx.compose.runtime.withFrameNanos { }
            try {
                firstChannelFocusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: IllegalStateException) {
                // Wait for LazyColumn to compose the row and attach its requester.
            }
            kotlinx.coroutines.delay(50)
        }
    }

    val onNavigateVerticalFromNow: (Int) -> Unit = { targetIndex ->
        val targetChannel = channels.getOrNull(targetIndex)
        if (targetChannel != null) {
            pendingFocusChannelId = targetChannel.id
            coroutineScope.launch {
                // Ensure target row is visible in LazyColumn
                verticalListState.animateScrollToItem(targetIndex)
                try {
                    channelFocusRequesters[targetChannel.id]?.requestFocus()
                } catch (e: Exception) {
                    // Handled by pendingFocusChannelId LaunchedEffect when composed
                }
            }
        }
    }

    // Total width of the horizontal timeline
    val totalTimelineWidth = (SLOT_WIDTH_30MIN.value * totalSlots).dp

    // Calculate NOW needle horizontal offset in Dp
    val nowElapsedSeconds = currentTimeSeconds - timelineStartSeconds
    val nowOffsetDp: Dp = if (nowElapsedSeconds >= 0) {
        ((nowElapsedSeconds.toFloat() / SLOT_DURATION_SECONDS.toFloat()) * SLOT_WIDTH_30MIN.value).dp
    } else {
        (-100).dp
    }

    val slotLabels = remember(timelineStartSeconds, totalSlots) {
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.UK)
        List(totalSlots) { i ->
            val slotTime = timelineStartSeconds + (i * SLOT_DURATION_SECONDS)
            formatter.format(java.util.Date(slotTime * 1000))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        if (channels.isEmpty() && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading UK TV schedules...",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            }
            return@Box
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Synchronized Header (Channel Column Label + 30-min Time slots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(HeaderBackgroundBrush)
            ) {
                // Fixed Channels label on top-left
                Box(
                    modifier = Modifier
                        .width(channelColumnWidth)
                        .fillMaxHeight()
                        .background(Color(0xFF131B2A))
                        .border(0.5.dp, TimelineDividerColor)
                        .padding(start = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "CHANNELS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Horizontally scrollable timeline header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    Row(modifier = Modifier.width(totalTimelineWidth)) {
                        slotLabels.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .width(SLOT_WIDTH_30MIN)
                                    .height(38.dp)
                                    .border(0.5.dp, TimelineDividerColor.copy(alpha = 0.4f))
                                    .padding(start = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = label,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Top "NOW" indicator badge
                    if (nowOffsetDp >= 0.dp && nowOffsetDp <= totalTimelineWidth) {
                        Box(
                            modifier = Modifier
                                .offset(x = nowOffsetDp - 19.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(NowBadgeBrush)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                .align(Alignment.CenterStart)
                        ) {
                            Text(
                                text = "NOW",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Main EPG Grid Body (LazyColumn of Channels)
            LazyColumn(
                state = verticalListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                    val timelineEndSeconds = timelineStartSeconds + (totalSlots * SLOT_DURATION_SECONDS)
                    val programmes = (scheduleMap[channel.id] ?: emptyList()).filter {
                        it.endTimeEpochSeconds > timelineStartSeconds && it.startTimeEpochSeconds < timelineEndSeconds
                    }

                    // Identify the designated "NOW" programme for this channel
                    val nowProgId = remember(programmes, currentTimeSeconds) {
                        programmes.firstOrNull { it.isLiveNow(currentTimeSeconds) }?.id
                            ?: programmes.minByOrNull { kotlin.math.abs(it.startTimeEpochSeconds - currentTimeSeconds) }?.id
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT_CHANNEL)
                    ) {
                        // Channel sidebar item with logo and badge
                        ChannelSidebarItem(
                            channel = channel,
                            width = channelColumnWidth,
                            height = ROW_HEIGHT_CHANNEL
                        )

                        // Channel's horizontal programme row (synchronized with master scroll state)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ROW_HEIGHT_CHANNEL)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            // Row of programme blocks
                            Row(
                                modifier = Modifier
                                    .width(totalTimelineWidth)
                                    .height(ROW_HEIGHT_CHANNEL)
                            ) {
                                if (programmes.isEmpty()) {
                                    OffAirCell(
                                        channel = channel,
                                        focusRequester = channelFocusRequesters.getValue(channel.id),
                                        pendingFocusChannelId = pendingFocusChannelId,
                                        onClearPendingFocus = { pendingFocusChannelId = null },
                                        channelIndex = index,
                                        channelsCount = channels.size,
                                        onNavigateVertical = onNavigateVerticalFromNow,
                                        onFocused = onChannelFocused,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp)
                                    )
                                } else {
                                    var previousEndSeconds = timelineStartSeconds

                                    for (prog in programmes) {
                                        // Ignore programmes outside visible window
                                        if (prog.endTimeEpochSeconds <= timelineStartSeconds || prog.startTimeEpochSeconds >= timelineEndSeconds) {
                                            continue
                                        }

                                        val visibleStartSeconds = maxOf(timelineStartSeconds, prog.startTimeEpochSeconds)
                                        val visibleEndSeconds = minOf(timelineEndSeconds, prog.endTimeEpochSeconds)
                                        val visibleDurationSeconds = visibleEndSeconds - visibleStartSeconds
                                        if (visibleDurationSeconds <= 0) continue

                                        // If there is a gap before this programme, render placeholder
                                        if (visibleStartSeconds > previousEndSeconds) {
                                            val gapSeconds = visibleStartSeconds - previousEndSeconds
                                            val gapWidth = ((gapSeconds.toFloat() / SLOT_DURATION_SECONDS.toFloat()) * SLOT_WIDTH_30MIN.value).dp
                                            if (gapWidth.value > 2f) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(gapWidth)
                                                        .height(ROW_HEIGHT_CHANNEL)
                                                        .border(0.5.dp, BorderUnfocused.copy(alpha = 0.25f))
                                                )
                                            }
                                        }

                                        // Calculate programme cell width strictly proportional to visible duration
                                        val cellWidth = ((visibleDurationSeconds.toFloat() / SLOT_DURATION_SECONDS.toFloat()) * SLOT_WIDTH_30MIN.value).dp
                                        if (cellWidth.value < 2f) continue

                                        val isNow = (prog.id == nowProgId)

                                        ProgrammeCell(
                                            programme = prog,
                                            channel = channel,
                                            channelIndex = index,
                                            channelsCount = channels.size,
                                            isNowProgramme = isNow,
                                            nowFocusRequester = if (isNow) channelFocusRequesters[channel.id] else null,
                                            pendingFocusChannelId = pendingFocusChannelId,
                                            onClearPendingFocus = { pendingFocusChannelId = null },
                                            onNavigateVerticalFromNow = onNavigateVerticalFromNow,
                                            currentTimeSeconds = currentTimeSeconds,
                                            cellWidth = cellWidth,
                                            visibleStartSeconds = visibleStartSeconds,
                                            visibleEndSeconds = visibleEndSeconds,
                                            isWithinRenderWindow = visibleEndSeconds > renderWindowStartSeconds &&
                                                    visibleStartSeconds < renderWindowEndSeconds,
                                            height = ROW_HEIGHT_CHANNEL,
                                            onFocused = onProgrammeFocused,
                                            onClick = onProgrammeClick
                                        )

                                        previousEndSeconds = maxOf(previousEndSeconds, visibleEndSeconds)
                                    }
                                }
                            }

                            // Glowing red vertical NOW timeline needle in this row
                            if (nowOffsetDp >= 0.dp && nowOffsetDp <= totalTimelineWidth) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = nowOffsetDp - 1.dp)
                                        .width(3.dp)
                                        .fillMaxHeight()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    AccentLive.copy(alpha = 0.4f),
                                                    AccentLive,
                                                    AccentLive.copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                        .zIndex(2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OffAirCell(
    channel: Channel,
    focusRequester: FocusRequester,
    pendingFocusChannelId: String?,
    onClearPendingFocus: () -> Unit,
    channelIndex: Int,
    channelsCount: Int,
    onNavigateVertical: (Int) -> Unit,
    onFocused: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        if (isFocused) onFocused(channel)
    }

    LaunchedEffect(pendingFocusChannelId) {
        if (pendingFocusChannelId == channel.id) {
            for (attempt in 1..8) {
                try {
                    focusRequester.requestFocus()
                } catch (_: Exception) {
                    // Retry while the row is being composed after vertical navigation.
                }
                kotlinx.coroutines.delay(35L)
            }
            onClearPendingFocus()
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = 1.dp)
            .then(if (isFocused) Modifier.border(2.dp, AccentCyan, RoundedCornerShape(6.dp)) else Modifier)
            .background(
                if (isFocused) Color(0xFF202D45) else Color(0xFF111724),
                RoundedCornerShape(6.dp)
            )
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.key == Key.DirectionDown && channelIndex < channelsCount - 1 -> {
                            onNavigateVertical(channelIndex + 1)
                            true
                        }
                        event.key == Key.DirectionUp && channelIndex > 0 -> {
                            onNavigateVertical(channelIndex - 1)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null) { },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Off air",
            color = if (isFocused) TextPrimary else TextSecondary.copy(alpha = 0.75f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}
