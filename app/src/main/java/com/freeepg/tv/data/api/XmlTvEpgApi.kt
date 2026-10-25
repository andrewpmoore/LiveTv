package com.freeepg.tv.data.api

import android.content.Context
import android.util.Log
import android.util.Xml
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/** XMLTV backup for the Sky Hawk schedule endpoint. */
class XmlTvEpgApi(
    context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "XmlTvEpgApi"
        private const val FEED_URL = "https://epgshare01.online/epgshare01/epg_ripper_UK1.xml.gz"
        private const val CACHE_FILE_NAME = "epgshare_uk.xml.gz"
        private const val CACHE_VALIDITY_MS = 6 * 60 * 60 * 1000L
        private const val WINDOW_BEFORE_SECONDS = 2 * 60 * 60L
        private const val WINDOW_AFTER_SECONDS = 26 * 60 * 60L

        // EPGshare UK1 IDs are stable XMLTV IDs. ITVBe has no entry in this feed.
        private val xmlTvChannelIds = mapOf(
            "bbc_one" to "BBC.One.Lon.HD.uk",
            "bbc_two" to "BBC.Two.HD.uk",
            "bbc_three" to "BBC.Three.HD.uk",
            "bbc_four" to "BBC.Four.HD.uk",
            "bbc_news" to "BBC.NEWS.HD.uk",
            "cbbc" to "CBBC.HD.uk",
            "cbeebies" to "CBeebies.HD.uk",
            "itv1" to "ITV1.HD.uk",
            "itv2" to "ITV2.HD.uk",
            "itv3" to "ITV3.HD.uk",
            "itv4" to "ITV4.HD.uk",
            "c4" to "Channel.4.HD.uk",
            "e4" to "E4.HD.uk",
            "more4" to "More4.HD.uk",
            "film4" to "Film4.HD.uk",
            "4seven" to "4seven.uk",
            "c5" to "Channel.5.HD.uk",
            "c5_action" to "5ACTION.uk",
            "c5_star" to "5STAR.uk",
            "c5_usa" to "5.USA.uk",
            "c5_drama" to "5SELECT.uk"
        )
    }

    private val feedCache = File(context.cacheDir, CACHE_FILE_NAME)
    private val feedMutex = Mutex()

    suspend fun getSchedules(
        channels: List<Channel>,
        nowSeconds: Long
    ): Map<String, List<Programme>> = withContext(Dispatchers.IO) {
        val requestedByXmlTvId = channels.mapNotNull { channel ->
            val mappedAppChannelId = if (channel.id in xmlTvChannelIds) {
                channel.id
            } else {
                channel.overlappingAppChannelId?.takeIf { it in xmlTvChannelIds }
            } ?: return@mapNotNull null
            xmlTvChannelIds[mappedAppChannelId]?.let { it to channel.id }
        }.toMap()

        if (requestedByXmlTvId.isEmpty()) return@withContext emptyMap()
        val file = feedMutex.withLock { getFeedFile() }
            ?: return@withContext emptyMap()

        try {
            parseFeed(file, requestedByXmlTvId, nowSeconds)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse XMLTV backup: ${e.message}")
            emptyMap()
        }
    }

    private fun getFeedFile(): File? {
        if (feedCache.exists() && System.currentTimeMillis() - feedCache.lastModified() < CACHE_VALIDITY_MS) {
            return feedCache
        }

        val temporaryFile = File(feedCache.parentFile, "$CACHE_FILE_NAME.tmp")
        try {
            val request = Request.Builder()
                .url(FEED_URL)
                .header("Accept", "application/gzip")
                .header("User-Agent", "Mozilla/5.0 (Android TV; FreeEPG/1.0)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body ?: return@use
                    body.byteStream().use { input ->
                        FileOutputStream(temporaryFile).use { output -> input.copyTo(output) }
                    }
                    if (temporaryFile.length() > 1024) {
                        if (feedCache.exists()) feedCache.delete()
                        if (temporaryFile.renameTo(feedCache)) return feedCache
                    }
                } else {
                    Log.w(TAG, "XMLTV backup returned HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not download XMLTV backup: ${e.message}")
        } finally {
            if (temporaryFile.exists()) temporaryFile.delete()
        }

        // Keep the last downloaded guide useful when the network is temporarily down.
        return feedCache.takeIf { it.exists() }
    }

    private fun parseFeed(
        file: File,
        requestedByXmlTvId: Map<String, String>,
        nowSeconds: Long
    ): Map<String, List<Programme>> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
        GZIPInputStream(file.inputStream().buffered()).use { input ->
            parser.setInput(input, "UTF-8")
            val result = mutableMapOf<String, MutableList<Programme>>()
            val earliestStart = nowSeconds - WINDOW_BEFORE_SECONDS
            val latestStart = nowSeconds + WINDOW_AFTER_SECONDS

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.name != "programme") continue

                val xmlTvId = parser.getAttributeValue(null, "channel")
                val appChannelId = requestedByXmlTvId[xmlTvId]
                val start = parseXmlTvTime(parser.getAttributeValue(null, "start"))
                val stop = parseXmlTvTime(parser.getAttributeValue(null, "stop"))
                if (appChannelId == null || start == null || start > latestStart ||
                    (stop != null && stop < earliestStart)
                ) {
                    skipElement(parser)
                    continue
                }

                var title: String? = null
                var description: String? = null
                var hasSubtitles = false
                while (parser.next() != XmlPullParser.END_TAG || parser.name != "programme") {
                    if (parser.eventType != XmlPullParser.START_TAG) continue
                    when (parser.name) {
                        "title" -> if (title == null) title = parser.nextText().trim()
                        "desc" -> if (description == null) description = parser.nextText().trim()
                        "subtitles" -> {
                            hasSubtitles = true
                            skipElement(parser)
                        }
                        else -> skipElement(parser)
                    }
                }

                val cleanTitle = title?.takeIf { it.isNotBlank() } ?: continue
                val end = (stop ?: (start + 30 * 60)).coerceAtLeast(start + 60)
                val programme = Programme(
                    id = "${appChannelId}_$start",
                    channelId = appChannelId,
                    title = cleanTitle,
                    synopsis = description?.takeIf { it.isNotBlank() } ?: "No description available.",
                    startTimeEpochSeconds = start,
                    durationSeconds = end - start,
                    isHd = xmlTvId.orEmpty().contains(".HD."),
                    isNew = false,
                    hasSubtitles = hasSubtitles,
                    hasAudioDescription = false
                )
                result.getOrPut(appChannelId) { mutableListOf() }.add(programme)
            }

            return result.mapValues { (_, programmes) ->
                programmes.distinctBy { it.startTimeEpochSeconds }.sortedBy { it.startTimeEpochSeconds }
            }
        }
    }

    private fun skipElement(parser: XmlPullParser) {
        val startDepth = parser.depth
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
        check(parser.depth == startDepth)
    }

    private fun parseXmlTvTime(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmm Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                    set2DigitYearStart(java.util.GregorianCalendar(2000, 0, 1).time)
                }
                parser.parse(value.trim())?.let { return it.time / 1000 }
            } catch (_: IllegalArgumentException) {
                // Try the next accepted XMLTV timestamp form.
            }
        }
        return null
    }
}
