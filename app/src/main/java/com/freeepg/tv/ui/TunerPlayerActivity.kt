package com.freeepg.tv.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.TimestampAdjuster
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.extractor.ts.TsPayloadReader
import androidx.media3.ui.PlayerView
import com.freeepg.tv.deeplink.DeepLinkManager
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
    val deepLinkManager = remember { DeepLinkManager(context) }

    var playerError by remember { mutableStateOf<String?>(null) }
    var hasCodecSupportError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var bufferingTimedOut by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var nowSeconds by remember { mutableStateOf(System.currentTimeMillis() / 1000L) }

    val externalPlayer = remember {
        val pm = context.packageManager
        val players = listOf(
            "org.videolan.vlc" to "VLC",
            "com.silicondust.view" to "HDHomeRun View",
            "com.brouken.player" to "Just Player",
            "org.courville.nova" to "Nova Video Player"
        )
        players.firstOrNull { (pkg, _) ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun launchExternal() {
        if (deepLinkManager.launchExternalTunerStream(streamUrl)) {
            onBack()
        } else {
            deepLinkManager.openPlayStore("org.videolan.vlc")
        }
    }

    val player = remember(streamUrl) {
        val extractorsFactory = ExtractorsFactory {
            val defaultFactory = DefaultTsPayloadReaderFactory(
                DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM
            )
            var seenAudio = false
            val tsPayloadReaderFactory = object : TsPayloadReader.Factory {
                override fun createInitialPayloadReaders(): android.util.SparseArray<TsPayloadReader> {
                    return defaultFactory.createInitialPayloadReaders()
                }

                override fun createPayloadReader(
                    streamType: Int,
                    esInfo: TsPayloadReader.EsInfo
                ): TsPayloadReader? {
                    // Ignore DVB subtitles, teletext, and private data streams that cause
                    // ProgressiveMediaPeriod to wait indefinitely for format headers
                    if (streamType == 89 || streamType == 6 || streamType == 5 || streamType == 11) {
                        return null
                    }
                    // Only register the primary audio stream. Secondary/Audio Description
                    // tracks rarely transmit samples and cause perpetual buffering.
                    if (streamType == 3 || streamType == 4 || streamType == 15 || streamType == 17 || streamType == 129 || streamType == 135) {
                        if (seenAudio) return null
                        seenAudio = true
                    }
                    return defaultFactory.createPayloadReader(streamType, esInfo)
                }
            }
            @Suppress("DEPRECATION")
            arrayOf(
                TsExtractor(
                    TsExtractor.MODE_SINGLE_PMT,
                    TimestampAdjuster(0),
                    tsPayloadReaderFactory
                )
            )
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(streamUrl)
                        .setMimeType(MimeTypes.VIDEO_MP2T)
                        .build()
                )
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                        if (playbackState == Player.STATE_READY) {
                            bufferingTimedOut = false
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        val videoGroup = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
                        val audioGroup = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO }
                        val hasVideo = videoGroup != null
                        val hasAudio = audioGroup != null
                        val isVideoSupported = videoGroup?.let { g -> (0 until g.length).any(g::isTrackSupported) } ?: false
                        val isAudioSupported = audioGroup?.let { g -> (0 until g.length).any(g::isTrackSupported) } ?: false

                        if ((hasVideo && !isVideoSupported) || (hasAudio && !isAudioSupported)) {
                            hasCodecSupportError = true
                            playerError = "This TV lacks an MPEG-2/MP2 broadcast decoder. Use an external player (e.g. VLC) to play."
                            isBuffering = false
                            showDetails = true
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        val cause = error.cause?.message ?: error.message ?: ""
                        playerError = when {
                            cause.contains("404") ->
                                "Channel unavailable or weak antenna signal on HDHomeRun (Error 404)."
                            cause.contains("Decoder") || cause.contains("MediaCodec") ->
                                "This TV does not support MPEG-2/MP2 broadcast decoding."
                            else ->
                                "Tuner playback error: ${error.localizedMessage ?: "Unable to stream"}"
                        }
                        isBuffering = false
                        showDetails = true
                    }
                })
                prepare()
            }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Buffering timeout watcher: If tuning takes over 7s, antenna signal is likely missing
    LaunchedEffect(isBuffering, playerError) {
        if (isBuffering && playerError == null) {
            kotlinx.coroutines.delay(7_000)
            if (isBuffering && playerError == null) {
                bufferingTimedOut = true
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
                            if (playerError != null || hasCodecSupportError || bufferingTimedOut) {
                                launchExternal()
                                true
                            } else {
                                showDetails = true
                                interaction++
                                true
                            }
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

        // Center connecting / buffering notice
        if (isBuffering && playerError == null) {
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
                        text = "Antenna signal may be weak or missing. Press [OK] to open in external player.",
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
                if (playerError != null || hasCodecSupportError || bufferingTimedOut) {
                    Spacer(Modifier.height(12.dp))
                    playerError?.let {
                        Text(it, color = Color(0xFFFF8A80), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(10.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        val playerActionLabel = externalPlayer?.let { "Play in ${it.second}" } ?: "Install VLC Player"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00B0FF),
                            modifier = Modifier
                                .clickable { launchExternal() }
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "▶  $playerActionLabel",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier
                                .clickable { onBack() }
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "Back to Guide",
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
