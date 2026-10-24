package com.freeepg.tv.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    channels: List<Channel>,
    scheduleMap: Map<String, List<Programme>>,
    currentTimeSeconds: Long,
    timelineStartSeconds: Long,
    totalSlots: Int = 52, // 26 hours of 30-min slots
    horizontalScrollState: ScrollState,
    onProgrammeFocused: (Programme, Channel) -> Unit,
    onProgrammeClick: (Programme, Channel) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val channelColumnWidth = 165.dp
    val verticalListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Map of FocusRequesters for each channel's "NOW" programme
    val nowFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    channels.forEach { ch ->
        if (!nowFocusRequesters.containsKey(ch.id)) {
            nowFocusRequesters[ch.id] = FocusRequester()
        }
    }

    // Pending channel target for vertical navigation between NOW programmes (starts on first channel)
    var pendingFocusChannelId by remember { mutableStateOf<String?>(channels.firstOrNull()?.id) }

    val onNavigateVerticalFromNow: (Int) -> Unit = { targetIndex ->
        val targetChannel = channels.getOrNull(targetIndex)
        if (targetChannel != null) {
            pendingFocusChannelId = targetChannel.id
            coroutineScope.launch {
                // Ensure target row is visible in LazyColumn
                verticalListState.animateScrollToItem(targetIndex)
                try {
                    nowFocusRequesters[targetChannel.id]?.requestFocus()
                } catch (e: Exception) {
                    // Handled by pendingFocusChannelId LaunchedEffect when composed
                }
            }
        }
    }

    // Set initial focus to the first channel's NOW programme on load
    LaunchedEffect(scheduleMap.isNotEmpty(), channels) {
        if (scheduleMap.isNotEmpty() && channels.isNotEmpty()) {
            val firstChannel = channels.first()
            pendingFocusChannelId = firstChannel.id
            val requester = nowFocusRequesters[firstChannel.id]
            for (attempt in 1..25) {
                kotlinx.coroutines.delay(50L)
                try {
                    requester?.requestFocus()
                    break
                } catch (_: Exception) {
                    // Keep retrying until first NOW cell is fully measured, laid out, and attached
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
                    val programmes = scheduleMap[channel.id] ?: emptyList()

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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = if (isLoading) "Loading schedule..." else "No guide data available",
                                            color = TextSecondary.copy(alpha = 0.5f),
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    val timelineEndSeconds = timelineStartSeconds + (totalSlots * SLOT_DURATION_SECONDS)
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
                                            nowFocusRequester = if (isNow) nowFocusRequesters[channel.id] else null,
                                            pendingFocusChannelId = pendingFocusChannelId,
                                            onClearPendingFocus = { pendingFocusChannelId = null },
                                            onNavigateVerticalFromNow = onNavigateVerticalFromNow,
                                            currentTimeSeconds = currentTimeSeconds,
                                            cellWidth = cellWidth,
                                            visibleStartSeconds = visibleStartSeconds,
                                            visibleEndSeconds = visibleEndSeconds,
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
