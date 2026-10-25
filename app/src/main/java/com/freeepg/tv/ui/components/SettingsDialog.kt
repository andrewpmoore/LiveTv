package com.freeepg.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.repository.ChannelSortOrder
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.AccentLive
import com.freeepg.tv.ui.theme.BorderFocused
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary

@Composable
fun SettingsDialog(
    enabledBroadcasters: Set<Broadcaster>,
    channelSortOrder: ChannelSortOrder,
    showAdultChannels: Boolean = false,
    focusRequestId: Int,
    isMacroEnabled: Boolean,
    isAccessibilityServiceActive: Boolean,
    tunerChannelsCount: Int = 0,
    tunerName: String? = null,
    tunerIp: String? = null,
    isScanningTuner: Boolean = false,
    onToggleBroadcaster: (Broadcaster) -> Unit,
    onChannelSortOrderSelected: (ChannelSortOrder) -> Unit,
    onToggleAdultChannels: () -> Unit = {},
    onToggleMacro: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onScanTuner: () -> Unit = {},
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val selectedSortOptionFocusRequester = remember { FocusRequester() }
    val accessibilitySettingsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        for (attempt in 1..10) {
            kotlinx.coroutines.delay(60)
            try {
                selectedSortOptionFocusRequester.requestFocus()
                break
            } catch (_: Exception) {
                // Retry while the dialog's first option is being composed.
            }
        }
    }

    LaunchedEffect(focusRequestId) {
        if (focusRequestId > 0) {
            for (attempt in 1..10) {
                kotlinx.coroutines.delay(100)
                try {
                    accessibilitySettingsFocusRequester.requestFocus()
                    break
                } catch (_: Exception) {
                    // Retry while the dialog window and accessibility row regain focusability.
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC070B14))
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Back || event.key == Key.Escape || event.key == Key.Backspace)
                    ) {
                        onDismiss()
                        true
                    } else false
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // Dialog Modal Card
            Box(
                modifier = Modifier
                    .width(620.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF141C2C), Color(0xFF0D131F))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Color(0x44FFFFFF), Color(0x15FFFFFF))
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Intercept clicks inside card
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E283C), Color(0xFF111827))
                                    )
                                )
                                .border(0.75.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preferences & App Integration",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Configure streaming services and auto-navigation macros",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Settings List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "CHANNEL ORDER",
                                color = AccentCyan.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(ChannelSortOrder.values().toList()) { sortOrder ->
                            ChannelSortOptionRow(
                                sortOrder = sortOrder,
                                isSelected = sortOrder == channelSortOrder,
                                onSelect = { onChannelSortOrderSelected(sortOrder) },
                                modifier = if (sortOrder == channelSortOrder) {
                                    Modifier.focusRequester(selectedSortOptionFocusRequester)
                                } else Modifier
                            )
                        }

                        // Section 1 Header: Broadcasters
                        item {
                            Text(
                                text = "BROADCASTERS & CHANNELS",
                                color = AccentCyan.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        // Broadcaster Options List
                        items(Broadcaster.values()) { broadcaster ->
                            val isEnabled = broadcaster in enabledBroadcasters
                            val channelsCount = if (broadcaster == Broadcaster.TUNER) {
                                tunerChannelsCount
                            } else {
                                Channel.ALL_CHANNELS.count { it.broadcaster == broadcaster }
                            }

                            BroadcasterToggleRow(
                                broadcaster = broadcaster,
                                isEnabled = isEnabled,
                                channelsCount = channelsCount,
                                onToggle = { onToggleBroadcaster(broadcaster) },
                            )
                        }

                        // Section: HDHomeRun TV Tuner
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "HDHOMERUN TV TUNER",
                                color = Color(0xFF00B0FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        item {
                            HdHomeRunStatusRow(
                                tunerName = tunerName,
                                tunerIp = tunerIp,
                                channelsCount = tunerChannelsCount,
                                isScanning = isScanningTuner,
                                onScan = onScanTuner
                            )
                        }

                        item {
                            AdultChannelsToggleRow(
                                isEnabled = showAdultChannels,
                                onToggle = onToggleAdultChannels
                            )
                        }

                        // Section 2 Header: Auto-Stream Launcher Macro
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AUTOMATED LIVE STREAM LAUNCHER (MACRO)",
                                color = Color(0xFFFF9E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }

                        // Macro Enable / Disable Toggle Row
                        item {
                            MacroToggleRow(
                                isEnabled = isMacroEnabled && isAccessibilityServiceActive,
                                isServiceActive = isAccessibilityServiceActive,
                                onToggle = onToggleMacro
                            )
                        }

                        // Accessibility Settings Action Row
                        item {
                            AccessibilitySetupRow(
                                isServiceActive = isAccessibilityServiceActive,
                                onClick = onOpenAccessibilitySettings,
                                focusRequester = accessibilitySettingsFocusRequester
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Preferences persist automatically across app launches.",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Press [OK] to toggle",
                            color = AccentCyan.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultChannelsToggleRow(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val accent = Color(0xFFFF5C8A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource)
            .background(
                if (isFocused) Color(0xFF262033) else Color(0xFF111726),
                RoundedCornerShape(10.dp)
            )
            .border(
                if (isFocused) 1.5.dp else 1.dp,
                if (isFocused) accent else Color(0x1FFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = if (isEnabled) 0.9f else 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text("18+", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "Adult channels (670–699)",
                color = if (isEnabled) TextPrimary else TextSecondary.copy(alpha = 0.75f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Text(
                text = if (isEnabled) "Included in the tuner guide" else "Hidden from the tuner guide",
                color = TextTertiary,
                fontSize = 10.5.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (isEnabled) accent.copy(alpha = 0.2f) else Color(0x1FFFFFFF))
                .border(1.dp, if (isEnabled) accent.copy(alpha = 0.75f) else Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEnabled) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = if (isEnabled) "ENABLED" else "DISABLED",
                    color = if (isEnabled) accent else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun ChannelSortOptionRow(
    sortOrder: ChannelSortOrder,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val outline = if (isFocused) BorderFocused else Color(0x1FFFFFFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color(0xFF1E283C) else Color(0xFF111726))
            .border(1.5.dp, outline, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sortOrder.label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AccentCyan,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BroadcasterToggleRow(
    broadcaster: Broadcaster,
    isEnabled: Boolean,
    channelsCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val backgroundModifier = if (isFocused) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(
                    broadcaster.brandColor.copy(alpha = 0.25f),
                    Color(0xFF1E283C)
                )
            ),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF111726),
            RoundedCornerShape(10.dp)
        )
    }

    val borderModifier = if (isFocused) {
        Modifier.border(
            1.5.dp,
            Brush.horizontalGradient(
                listOf(BorderFocused, broadcaster.brandColor)
            ),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.border(
            1.5.dp,
            Color(0x1FFFFFFF),
            RoundedCornerShape(10.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .focusable(interactionSource = interactionSource)
            .then(backgroundModifier)
            .then(borderModifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Broadcaster Brand Badge
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    broadcaster.brandColor.copy(alpha = if (isEnabled) 1.0f else 0.35f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = broadcaster.badgeText,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Channels Count
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = broadcaster.displayName,
                color = if (isEnabled) TextPrimary else TextSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "$channelsCount linear channels available",
                color = TextTertiary,
                fontSize = 10.5.sp
            )
        }

        // Toggle Capsule Button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isEnabled) {
                        broadcaster.brandColor.copy(alpha = 0.25f)
                    } else {
                        Color(0x1FFFFFFF)
                    }
                )
                .border(
                    1.dp,
                    if (isEnabled) broadcaster.brandColor.copy(alpha = 0.8f) else Color(0x33FFFFFF),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = broadcaster.brandColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isEnabled) "ENABLED" else "DISABLED",
                    color = if (isEnabled) broadcaster.brandColor else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun MacroToggleRow(
    isEnabled: Boolean,
    isServiceActive: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val backgroundModifier = if (isFocused) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF262C3E), Color(0xFF1E283C))
            ),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF111726),
            RoundedCornerShape(10.dp)
        )
    }

    val borderModifier = if (isFocused) {
        Modifier.border(
            1.5.dp,
            BorderFocused,
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.border(
            1.5.dp,
            Color(0x1FFFFFFF),
            RoundedCornerShape(10.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .focusable(interactionSource = interactionSource)
            .then(backgroundModifier)
            .then(borderModifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Macro Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isEnabled && isServiceActive) Color(0xFFFF9E0B).copy(alpha = 0.2f) else Color(0x1FFFFFFF)
                )
                .border(
                    0.5.dp,
                    if (isEnabled && isServiceActive) Color(0xFFFF9E0B).copy(alpha = 0.6f) else Color(0x22FFFFFF),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isEnabled && isServiceActive) Color(0xFFFF9E0B) else TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Description
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Auto-Navigate Live Streams",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Service status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isServiceActive) Color(0x2200E676) else Color(0x22FF9E0B)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isServiceActive) "SERVICE ACTIVE" else "SETUP NEEDED",
                        color = if (isServiceActive) Color(0xFF00E676) else Color(0xFFFF9E0B),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isServiceActive)
                    "Navigates broadcaster apps to the requested live channel when a direct link is unavailable"
                else
                    "Auto-navigation is off until Accessibility Service is enabled in settings",
                color = if (isServiceActive) TextTertiary else Color(0xFFFFB74D),
                fontSize = 10.5.sp
            )
        }

        // Toggle Switch Capsule
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isEnabled && isServiceActive) Color(0x2200E676) else Color(0x1FFFFFFF)
                )
                .border(
                    1.dp,
                    if (isEnabled && isServiceActive) Color(0xFF00E676).copy(alpha = 0.8f) else Color(0x33FFFFFF),
                    RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEnabled && isServiceActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (isEnabled && isServiceActive) "ENABLED" else if (!isServiceActive) "OFF" else "DISABLED",
                    color = if (isEnabled && isServiceActive) Color(0xFF00E676) else TextTertiary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun AccessibilitySetupRow(
    isServiceActive: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val backgroundModifier = if (isFocused) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF1E283C), Color(0xFF141C2C))
            ),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF0D121F),
            RoundedCornerShape(10.dp)
        )
    }

    val borderModifier = if (isFocused) {
        Modifier.border(
            1.5.dp,
            BorderFocused,
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.border(
            1.5.dp,
            if (!isServiceActive) Color(0x44FF9E0B) else Color(0x1FFFFFFF),
            RoundedCornerShape(10.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(backgroundModifier)
            .then(borderModifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Android TV Accessibility Settings",
                color = AccentCyan,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = if (isServiceActive)
                    "Live TV Service is active in system settings"
                else
                    "Click to open Settings > System > Accessibility and enable Live TV",
                color = if (!isServiceActive) Color(0xFFFFB74D) else TextTertiary,
                fontSize = 10.5.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AccentCyan.copy(alpha = if (isFocused) 0.3f else 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "Open Settings \u2192",
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
@Composable
private fun HdHomeRunStatusRow(
    tunerName: String?,
    tunerIp: String?,
    channelsCount: Int,
    isScanning: Boolean,
    onScan: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val backgroundModifier = if (isFocused) {
        Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF00B0FF).copy(alpha = 0.25f), Color(0xFF1E283C))
            ),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.background(
            Color(0xFF111726),
            RoundedCornerShape(10.dp)
        )
    }

    val borderModifier = if (isFocused) {
        Modifier.border(
            1.5.dp,
            BorderFocused,
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier.border(
            1.5.dp,
            Color(0x1FFFFFFF),
            RoundedCornerShape(10.dp)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onScan
            )
            .focusable(interactionSource = interactionSource)
            .then(backgroundModifier)
            .then(borderModifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tuner Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00B0FF).copy(alpha = 0.2f))
                .border(0.5.dp, Color(0xFF00B0FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color(0xFF00B0FF),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Description
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tunerName ?: "HDHomeRun Tuner",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (!tunerIp.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x2200E676))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tunerIp,
                            color = Color(0xFF00E676),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Broadcaster apps are preferred when available; tuner streams cover other channels • $channelsCount channels loaded",
                color = TextTertiary,
                fontSize = 10.5.sp
            )
        }

        // Action button to scan/refresh
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF00B0FF).copy(alpha = if (isFocused) 0.35f else 0.18f))
                .border(1.dp, Color(0xFF00B0FF).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (isScanning) "Scanning..." else "Scan LAN \u21bb",
                color = Color(0xFF00B0FF),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
