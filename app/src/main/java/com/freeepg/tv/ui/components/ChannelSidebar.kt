package com.freeepg.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary
import com.freeepg.tv.ui.theme.TimelineDividerColor

val ROW_HEIGHT_CHANNEL: Dp = 68.dp

@Composable
fun ChannelSidebarItem(
    channel: Channel,
    width: Dp = 160.dp,
    height: Dp = ROW_HEIGHT_CHANNEL,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF131A29),
                        Color(0xFF0F1522)
                    )
                )
            )
            .border(0.5.dp, TimelineDividerColor)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Broadcaster brand indicator strip
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(channel.broadcaster.brandGradient)
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Channel Logo if present
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x18FFFFFF))
                        .padding(2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Channel number pill
                    Text(
                        text = channel.channelNumber,
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = channel.broadcaster.shortName,
                        color = channel.broadcaster.brandColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Channel name
                Text(
                    text = channel.shortName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
