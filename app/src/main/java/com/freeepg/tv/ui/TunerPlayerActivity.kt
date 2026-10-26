package com.freeepg.tv.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.freeepg.tv.data.repository.SettingsRepository
import com.freeepg.tv.ui.theme.FreeEpgTheme
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TunerPlayerActivity : ComponentActivity() {
    companion object {
        const val EXTRA_STREAM_URL = "tuner_stream_url"
        const val EXTRA_CHANNEL_ID = "tuner_channel_id"
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
                    channelId = intent.getStringExtra(EXTRA_CHANNEL_ID).orEmpty(),
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
    channelId: String,
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
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var playerError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasStartedPlayback by remember { mutableStateOf(false) }
    var bufferingTimedOut by remember { mutableStateOf(false) }
    var weakSignalDetected by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var nowSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1000L) }

    val libVlc = remember(streamUrl) {
        LibVLC(context.applicationContext, arrayListOf("--network-caching=1200", "--clock-jitter=0"))
    }
    val player = remember(libVlc) {
        MediaPlayer(libVlc).apply {
            setEventListener { event ->
                mainHandler.post {
                    when (event.type) {
                        MediaPlayer.Event.Opening, MediaPlayer.Event.Buffering -> {
                            if (!hasStartedPlayback) isBuffering = true
                        }
                        MediaPlayer.Event.Playing, MediaPlayer.Event.Vout -> {
                            hasStartedPlayback = true
                            isBuffering = false
                            bufferingTimedOut = false
                            playerError = null
                        }
                        MediaPlayer.Event.EncounteredError -> {
                            playerError = "Unable to play this tuner stream. Check the antenna signal, or try an external player."
                            isBuffering = false
                            weakSignalDetected = true
                            showDetails = true
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.stop()
            player.detachViews()
            player.setEventListener(null)
            player.release()
            libVlc.release()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Buffering timeout watcher: If tuning takes over 7s, antenna signal is likely missing
    LaunchedEffect(isBuffering, hasStartedPlayback, playerError) {
        if (isBuffering && !hasStartedPlayback && playerError == null) {
            kotlinx.coroutines.delay(7_000)
            if (isBuffering && !hasStartedPlayback && playerError == null) {
                bufferingTimedOut = true
                weakSignalDetected = true
                showDetails = true
            }
        } else {
            bufferingTimedOut = false
        }
    }

    LaunchedEffect(showDetails, interaction, playerError, bufferingTimedOut) {
        if (showDetails && playerError == null && !bufferingTimedOut) {
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
                VLCVideoLayout(viewContext).also { layout ->
                    player.attachViews(layout, null, true, false)
                    val media = Media(libVlc, Uri.parse(streamUrl))
                    player.setMedia(media)
                    media.release()
                    player.play()
                }
            },
            update = {}
        )

        // Center connecting / buffering notice
        if (isBuffering && !hasStartedPlayback && playerError == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connecting to ${channelName.ifBlank { "TV channel" }}…",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
                if (bufferingTimedOut) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Antenna signal may be weak or missing. Press Back to return to the guide.",
                        color = Color(0xFFFFCC80),
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Overlay with Programme info, Status, and Player actions
        if (showDetails || playerError != null || bufferingTimedOut) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE6101420), Color(0xF20A0D14))
                        )
                    )
                    .padding(start = 56.dp, end = 56.dp, top = 44.dp, bottom = 36.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFFF1744), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = listOf(channelNumber, channelName).filter { it.isNotBlank() }.joinToString("  •  "),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = programmeTitle.ifBlank { channelName.ifBlank { "Live TV" } },
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                if (synopsis.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        synopsis,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 16.sp,
                        maxLines = 2
                    )
                }
                if (programmeStart > 0 && programmeDuration > 0) {
                    Spacer(Modifier.height(12.dp))
                    val progress = ((nowSeconds - programmeStart).toFloat() / programmeDuration)
                        .coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFFFF315B),
                        trackColor = Color.White.copy(alpha = 0.26f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatClock(programmeStart), color = Color.White.copy(alpha = 0.76f), fontSize = 14.sp)
                        Text(formatClock(programmeStart + programmeDuration), color = Color.White.copy(alpha = 0.76f), fontSize = 14.sp)
                    }
                }

                // Error or external player call-to-action
                if (playerError != null || bufferingTimedOut) {
                    Spacer(Modifier.height(12.dp))
                    playerError?.let {
                        Text(it, color = Color(0xFFFF8A80), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (weakSignalDetected && channelId.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF8B2635),
                            modifier = Modifier
                                .clickable {
                                    SettingsRepository(context).setTunerChannelHidden(channelId, true)
                                    onBack()
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Hide this channel from the guide",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }
    }

}

private fun formatClock(epochSeconds: Long): String =
    SimpleDateFormat("HH:mm", Locale.UK).format(Date(epochSeconds * 1000L))
