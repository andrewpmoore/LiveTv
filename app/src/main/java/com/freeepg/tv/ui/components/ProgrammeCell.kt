package com.freeepg.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.AccentLive
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary

private val CellShape = RoundedCornerShape(6.dp)
private val FocusedCardBrush = Brush.verticalGradient(
    listOf(Color(0xFF2C3C5C), Color(0xFF1E2B44))
)
private val IdleCardBrush = Brush.verticalGradient(
    listOf(Color(0xFF161E2E), Color(0xFF111724))
)
private val FocusedBorderBrush = Brush.horizontalGradient(
    listOf(Color(0xFF00E5FF), Color(0xFFFFFFFF), Color(0xFF80D8FF))
)
private val LiveBorderColor = AccentLive.copy(alpha = 0.5f)
private val IdleBorderColor = Color(0x1FFFFFFF)

@Composable
fun ProgrammeCell(
    programme: Programme,
    channel: Channel,
    channelIndex: Int,
    channelsCount: Int,
    isNowProgramme: Boolean,
    nowFocusRequester: FocusRequester?,
    pendingFocusChannelId: String?,
    onClearPendingFocus: () -> Unit,
    onNavigateVerticalFromNow: (targetChannelIndex: Int) -> Unit,
    currentTimeSeconds: Long,
    cellWidth: Dp,
    visibleStartSeconds: Long,
    visibleEndSeconds: Long,
    height: Dp = ROW_HEIGHT_CHANNEL,
    onFocused: (Programme, Channel) -> Unit,
    onClick: (Programme, Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Notify parent hero pane whenever this cell gains focus
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocused(programme, channel)
        }
    }

    // When navigating vertically across channels on "NOW", claim focus as soon as composed
    LaunchedEffect(pendingFocusChannelId) {
        if (isNowProgramme && pendingFocusChannelId == channel.id) {
            for (i in 1..8) {
                try {
                    nowFocusRequester?.requestFocus()
                    break
                } catch (_: Exception) {
                    kotlinx.coroutines.delay(35L)
                }
            }
            onClearPendingFocus()
        }
    }

    val isLive = programme.isLiveNow(currentTimeSeconds)
    val visibleDuration = (visibleEndSeconds - visibleStartSeconds).coerceAtLeast(1L)
    val visibleElapsed = (currentTimeSeconds - visibleStartSeconds).coerceIn(0L, visibleDuration)
    val visibleProgress = if (isLive) (visibleElapsed.toFloat() / visibleDuration.toFloat()).coerceIn(0f, 1f) else 0f

    val backgroundBrush = if (isFocused) FocusedCardBrush else IdleCardBrush

    val borderModifier = if (isFocused) {
        Modifier.border(width = 2.5.dp, brush = FocusedBorderBrush, shape = CellShape)
    } else if (isLive) {
        Modifier.border(width = 0.5.dp, color = LiveBorderColor, shape = CellShape)
    } else {
        Modifier.border(width = 0.5.dp, color = IdleBorderColor, shape = CellShape)
    }

    val focusRequesterModifier = if (isNowProgramme && nowFocusRequester != null) {
        Modifier.focusRequester(nowFocusRequester)
    } else {
        Modifier
    }

    // Lock D-pad Up and Down navigation directly to other channels' NOW programmes
    val keyEventModifier = if (isNowProgramme) {
        Modifier.onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                if (keyEvent.key == Key.DirectionDown && channelIndex < channelsCount - 1) {
                    onNavigateVerticalFromNow(channelIndex + 1)
                    return@onPreviewKeyEvent true
                } else if (keyEvent.key == Key.DirectionUp && channelIndex > 0) {
                    onNavigateVerticalFromNow(channelIndex - 1)
                    return@onPreviewKeyEvent true
                }
            }
            false
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .width(cellWidth)
            .height(height)
            .padding(1.dp)
            .then(focusRequesterModifier)
            .then(keyEventModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onClick(programme, channel) }
            )
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundBrush)
            .then(borderModifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (cellWidth < 60.dp) 5.dp else 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Title and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If Live, show red play badge
                if (isLive && cellWidth >= 50.dp) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AccentLive,
                        modifier = Modifier
                            .size(13.dp)
                            .padding(end = 2.dp)
                    )
                }

                Text(
                    text = programme.title,
                    color = if (isFocused) Color.White else TextPrimary.copy(alpha = 0.95f),
                    fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = if (cellWidth < 60.dp) 11.sp else 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (programme.isHd && cellWidth >= 75.dp) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HD",
                        color = AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.5.sp,
                        modifier = Modifier
                            .background(AccentCyan.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 0.5.dp)
                    )
                }
            }

            // Middle row: Timing & Episode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val timeLabel = when {
                    cellWidth >= 70.dp -> "${programme.formattedStartTime()} - ${programme.formattedEndTime()}"
                    cellWidth >= 38.dp -> programme.formattedStartTime()
                    else -> ""
                }

                if (timeLabel.isNotEmpty()) {
                    Text(
                        text = timeLabel,
                        color = if (isLive) AccentLive else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal
                    )
                }

                if (cellWidth >= 120.dp) {
                    programme.formattedEpisodeInfo()?.let { ep ->
                        Text(
                            text = ep,
                            color = TextTertiary,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom row: Edge-to-edge live progress bar mathematically aligned with NOW needle
        if (isLive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0x33000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = visibleProgress)
                        .background(
                            Brush.horizontalGradient(channel.broadcaster.brandGradient)
                        )
                )
            }
        }
    }
}
