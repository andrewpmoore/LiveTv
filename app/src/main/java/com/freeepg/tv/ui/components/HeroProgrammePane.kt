package com.freeepg.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.AccentLive
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary

@Composable
fun HeroProgrammePane(
    programme: Programme?,
    channel: Channel?,
    currentTimeSeconds: Long,
    onWatchLiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandColor = channel?.broadcaster?.brandColor ?: AccentCyan

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {

        if (programme != null && channel != null) {
            val isLive = programme.isLiveNow(currentTimeSeconds)
            val progress = programme.progress(currentTimeSeconds)
            val thumbUrl = programme.imageUrl()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Programme Metadata, Title, Synopsis, and Action Controls
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top row: Channel Logo/Badge, Live Pill, Time Info, Metadata Tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Channel Logo / Badge
                        if (!channel.logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = channel.logoUrl,
                                contentDescription = channel.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .height(26.dp)
                                    .width(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x22FFFFFF))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(channel.broadcaster.brandColor, channel.broadcaster.brandColor.copy(alpha = 0.8f))
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = channel.shortName,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Channel name & number
                        Text(
                            text = "${channel.name} • ${channel.channelNumber}",
                            color = TextPrimary.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Live Pill
                        if (isLive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = AccentLive.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, AccentLive.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentLive)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "LIVE NOW",
                                    color = AccentLive,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        // Formatted Time
                        Text(
                            text = programme.formattedTimeRange(),
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Episode info tag
                        programme.formattedEpisodeInfo()?.let { epInfo ->
                            TagPill(text = epInfo, color = TextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // HD tag
                        if (programme.isHd) {
                            TagPill(text = "HD", color = AccentCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // NEW tag
                        if (programme.isNew) {
                            TagPill(text = "NEW", color = Color(0xFFFFD500))
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Subtitles & AD
                        if (programme.hasSubtitles) {
                            TagPill(text = "SUB", color = TextTertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (programme.hasAudioDescription) {
                            TagPill(text = "AD", color = TextTertiary)
                        }
                    }

                    // Middle Section: Title & Synopsis
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = programme.title,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = programme.synopsis,
                            color = TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Bottom Row: Watch Live Gradient Button & Progress Bar
                    if (isLive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Small Circular Play Button
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                AccentLive,
                                                Color(0xFFFF4060)
                                            )
                                        )
                                    )
                                    .border(0.75.dp, Color(0x66FFFFFF), CircleShape)
                                    .clickable { onWatchLiveClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch Live on ${channel.broadcaster.displayName}",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Progress Bar & Time Tracker
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${programme.formattedStartTime()} (Started)",
                                        color = TextTertiary,
                                        fontSize = 10.5.sp
                                    )
                                    Text(
                                        text = "${programme.formattedEndTime()} (Ends)",
                                        color = TextTertiary,
                                        fontSize = 10.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0x22FFFFFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = progress)
                                            .background(
                                                Brush.horizontalGradient(channel.broadcaster.brandGradient)
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = "Starts at ${programme.formattedStartTime()}",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Right Column: 16:9 Programme Thumbnail Card
                Box(
                    modifier = Modifier
                        .width(210.dp)
                        .height(118.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color(0x44FFFFFF), Color(0x11FFFFFF))
                            ),
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1B2436), Color(0xFF101724))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = programme.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Sleek fallback thumbnail when image is not provided
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            channel.broadcaster.brandColor.copy(alpha = 0.25f),
                                            Color(0xFF0F1626)
                                        )
                                    )
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LiveTv,
                                contentDescription = null,
                                tint = channel.broadcaster.brandColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = channel.shortName,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // If live, display discreet red corner tag on thumbnail
                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(AccentLive, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Centered subtle circular play badge on thumbnail
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x77000000))
                                .border(1.dp, Color(0x66FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Placeholder when no programme has focus
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Live TV • UK Guide",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Navigate with D-Pad to explore programmes across BBC iPlayer, ITVX, Channel 4, Channel 5, and U. Select any currently-playing programme to launch into live stream.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp
        )
    }
}
