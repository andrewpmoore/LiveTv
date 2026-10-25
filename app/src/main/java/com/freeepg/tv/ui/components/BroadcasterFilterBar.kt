package com.freeepg.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.res.painterResource
import com.freeepg.tv.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.BgDark
import com.freeepg.tv.ui.theme.BgSurface
import com.freeepg.tv.ui.theme.BgSurfaceElevated
import com.freeepg.tv.ui.theme.BorderFocused
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BroadcasterFilterBar(
    onNavigateToGuide: () -> Unit,
    selectedBroadcaster: Broadcaster?,
    enabledBroadcasters: Set<Broadcaster>,
    onSelectBroadcaster: (Broadcaster?) -> Unit,
    onRefreshClick: () -> Unit,
    onJumpToNowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    isLoading: Boolean,
    currentTimeSeconds: Long,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm • EEE d MMM", Locale.UK) }
    val formattedClock = remember(currentTimeSeconds) {
        timeFormat.format(Date(currentTimeSeconds * 1000))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Icon / Logo in the top-left
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Live TV",
            modifier = Modifier
                .padding(end = 20.dp)
                .height(28.dp)
                .width(37.dp)
        )

        // Broadcaster Filter Tabs
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterTab(
                text = "All Channels",
                isSelected = selectedBroadcaster == null,
                accentColor = AccentCyan,
                isDefaultFocus = true,
                onNavigateToGuide = onNavigateToGuide,
                onClick = { onSelectBroadcaster(null) }
            )

            Broadcaster.values()
                .filter { it in enabledBroadcasters }
                .forEach { broadcaster ->
                    FilterTab(
                        text = broadcaster.displayName,
                        isSelected = selectedBroadcaster == broadcaster,
                        accentColor = broadcaster.brandColor,
                        onNavigateToGuide = onNavigateToGuide,
                        onClick = { onSelectBroadcaster(broadcaster) }
                    )
                }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action: Jump to Now (moved before Settings)
        TvActionButton(
            icon = Icons.Default.Schedule,
            label = "Now",
            onNavigateToGuide = onNavigateToGuide,
            onClick = onJumpToNowClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Action: Settings Dialog Button (just a cog icon)
        TvActionButton(
            icon = Icons.Default.Settings,
            label = null,
            contentDescription = "Settings",
            onNavigateToGuide = onNavigateToGuide,
            onClick = onOpenSettingsClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Action: Refresh Button (just an icon)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF131A28), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
            }
        } else {
            TvActionButton(
                icon = Icons.Default.Refresh,
                label = null,
                contentDescription = "Refresh Guide",
                onNavigateToGuide = onNavigateToGuide,
                onClick = onRefreshClick
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Real-time Clock
        Text(
            text = formattedClock,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    isDefaultFocus: Boolean = false,
    onNavigateToGuide: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(isDefaultFocus) {
        if (isDefaultFocus) {
            kotlinx.coroutines.delay(100)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val backgroundModifier = when {
        isFocused -> Modifier.background(
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.15f))
            ),
            RoundedCornerShape(8.dp)
        )
        isSelected -> Modifier.background(
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(accentColor.copy(alpha = 0.25f), Color(0xFF1E283C))
            ),
            RoundedCornerShape(8.dp)
        )
        else -> Modifier.background(
            Color(0xFF131A28),
            RoundedCornerShape(8.dp)
        )
    }

    val borderModifier = when {
        isFocused -> Modifier.border(
            1.5.dp,
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(accentColor, Color(0xFF80D8FF))
            ),
            RoundedCornerShape(8.dp)
        )
        isSelected -> Modifier.border(1.5.dp, accentColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
        else -> Modifier.border(1.5.dp, Color(0x1FFFFFFF), RoundedCornerShape(8.dp))
    }

    val textColor = when {
        isFocused || isSelected -> TextPrimary
        else -> TextSecondary
    }

    Box(
        modifier = Modifier
            .then(if (isDefaultFocus) Modifier.focusRequester(focusRequester) else Modifier)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 && event.key == Key.DirectionDown) {
                    onNavigateToGuide()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .then(backgroundModifier)
            .then(borderModifier)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun TvActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String? = null,
    contentDescription: String? = label,
    onNavigateToGuide: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 && event.key == Key.DirectionDown) {
                    onNavigateToGuide()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .background(
                if (isFocused) AccentCyan.copy(alpha = 0.25f) else Color(0xFF131A28),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.5.dp,
                if (isFocused) BorderFocused else Color(0x1FFFFFFF),
                RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = if (label != null) 11.dp else 8.dp,
                vertical = 6.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused) AccentCyan else TextSecondary,
            modifier = Modifier.size(16.dp)
        )
        if (label != null) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = if (isFocused) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}
