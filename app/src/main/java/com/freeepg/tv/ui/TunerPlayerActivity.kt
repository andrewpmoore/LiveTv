package com.freeepg.tv.ui

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.freeepg.tv.ui.theme.FreeEpgTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TunerPlayerActivity : ComponentActivity() {
    companion object {
        const val EXTRA_STREAM_URL = "tuner_stream_url"
        const val EXTRA_CHANNEL_NAME = "tuner_channel_name"
        const val EXTRA_CHANNEL_NUMBER = "tuner_channel_number"
        const val EXTRA_PROGRAMME_TITLE = "tuner_programme_title"
        const val EXTRA_PROGRAMME_SYNOPSIS = "tuner_programme_synopsis"
        const val EXTRA_PROGRAMME_START = "tuner_programme_start"
        const val EXTRA_PROGRAMME_DURATION = "tuner_programme_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        if (streamUrl.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            FreeEpgTheme {
                TunerPlayerScreen(
                    streamUrl = streamUrl,
                    channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME).orEmpty(),
                    channelNumber = intent.getStringExtra(EXTRA_CHANNEL_NUMBER).orEmpty(),
                    programmeTitle = intent.getStringExtra(EXTRA_PROGRAMME_TITLE).orEmpty(),
                    synopsis = intent.getStringExtra(EXTRA_PROGRAMME_SYNOPSIS).orEmpty(),
                    programmeStart = intent.getLongExtra(EXTRA_PROGRAMME_START, 0L),
                    programmeDuration = intent.getLongExtra(EXTRA_PROGRAMME_DURATION, 0L),
                    onBack = ::finish
                )
            }
        }
    }
}

@Composable
private fun TunerPlayerScreen(
    streamUrl: String,
    channelName: String,
    channelNumber: String,
    programmeTitle: String,
    synopsis: String,
    programmeStart: Long,
    programmeDuration: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var showDetails by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var nowSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1000L) }
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                }

                override fun onPlayerError(error: PlaybackException) {
                    playerError = error.localizedMessage ?: "The tuner stream could not be played."
                    isBuffering = false
                    showDetails = true
                }
            })
            prepare()
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(showDetails, interaction, playerError) {
        if (showDetails && playerError == null) {
            kotlinx.coroutines.delay(5_000)
            showDetails = false
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            nowSeconds = System.currentTimeMillis() / 1000L
            kotlinx.coroutines.delay(1_000)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter -> {
                            showDetails = true
                            interaction++
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.player = player
                }
            },
            update = { it.player = player }
        )

        if (isBuffering && playerError == null) {
            Text(
                text = "Connecting to ${channelName.ifBlank { "TV channel" }}…",
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (showDetails || playerError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE6101420), Color(0xF20A0D14))
                        )
                    )
                    .padding(start = 56.dp, end = 56.dp, top = 44.dp, bottom = 40.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFFF1744), shape = RoundedCornerShape(4.dp)) {
                        Text("LIVE", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = listOf(channelNumber, channelName).filter { it.isNotBlank() }.joinToString("  •  "),
                        color = Color.White.copy(alpha = 0.82f), fontSize = 17.sp, fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = programmeTitle.ifBlank { channelName.ifBlank { "Live TV" } },
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (synopsis.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(synopsis, color = Color.White.copy(alpha = 0.78f), fontSize = 17.sp, maxLines = 2)
                }
                if (programmeStart > 0 && programmeDuration > 0) {
                    Spacer(Modifier.height(13.dp))
                    val progress = ((nowSeconds - programmeStart).toFloat() / programmeDuration)
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFFFF315B),
                        trackColor = Color.White.copy(alpha = 0.26f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatClock(programmeStart), color = Color.White.copy(alpha = 0.76f), fontSize = 14.sp)
                        Text(formatClock(programmeStart + programmeDuration), color = Color.White.copy(alpha = 0.76f), fontSize = 14.sp)
                    }
                }
                playerError?.let {
                    Spacer(Modifier.height(10.dp))
                    Text("Playback error: $it", color = Color(0xFFFF9AA9), fontSize = 15.sp)
                }
            }
        }
    }
}

private fun formatClock(epochSeconds: Long): String =
    SimpleDateFormat("HH:mm", Locale.UK).format(Date(epochSeconds * 1000L))
