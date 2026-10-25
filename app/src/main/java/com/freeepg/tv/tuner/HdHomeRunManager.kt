package com.freeepg.tv.tuner

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.net.Inet4Address
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class HdHomeRunManager(private val context: Context) {

    companion object {
        private const val TAG = "HdHomeRunManager"
        private const val PREFS_NAME = "hdhomerun_prefs"
        private const val KEY_TUNER_IP = "tuner_ip"
        private const val KEY_TUNER_NAME = "tuner_name"
        private const val KEY_TUNER_DEVICE_ID = "tuner_device_id"
        private const val KEY_XMLTV_NEXT_REFRESH_AT = "xmltv_next_refresh_at"
        private const val LINEUP_CACHE_FILE = "hdhomerun_lineup.json"
        private const val XMLTV_CACHE_FILE = "hdhomerun_guide.xml"
        private const val XMLTV_CACHE_MAX_AGE_MS = 20 * 60 * 60 * 1000L
        private const val DISCOVER_URL = "https://ipv4-api.hdhomerun.com/discover"
        private const val XMLTV_URL = "https://api.hdhomerun.com/api/xmltv"

        /**
         * Converts HDHomeRun lineup items into standard Channels.
         * Identifies overlapping channels so that playback can automatically prioritize the broadcaster app version
         * per user preference ("where there's an overlap in channels use the app version as my tvtuner signal is poor").
         */
        fun mapLineupToChannels(
            items: List<HdHomeRunLineupItem>,
            tunerIp: String
        ): List<Channel> {
            val appChannels = Channel.ALL_CHANNELS

            return items.map { item ->
                val overlappingAppChannel = findOverlappingAppChannel(item, appChannels)
                val skySid = overlappingAppChannel?.skySid ?: lookupTunerExclusiveSkySid(item.guideNumber, item.guideName)

                Channel(
                    id = "tuner_${item.guideNumber}",
                    skySid = skySid,
                    channelNumber = item.guideNumber,
                    name = item.guideName,
                    shortName = overlappingAppChannel?.shortName ?: item.guideName,
                    broadcaster = Broadcaster.TUNER,
                    logoUrl = overlappingAppChannel?.logoUrl ?: lookupTunerLogo(item.guideName),
                    deepLinkUris = overlappingAppChannel?.deepLinkUris ?: emptyList(),
                    webLiveUrl = overlappingAppChannel?.webLiveUrl ?: item.url,
                    streamUrl = normalizeTunerStreamUrl(item.url, tunerIp),
                    overlappingAppChannelId = overlappingAppChannel?.id
                )
            }
        }

        private fun normalizeTunerStreamUrl(streamUrl: String, tunerIp: String): String {
            return runCatching {
                val uri = Uri.parse(streamUrl)
                val host = uri.host.orEmpty()
                if (!host.endsWith(".local", ignoreCase = true) || tunerIp.isBlank() || ':' in tunerIp) {
                    streamUrl
                } else {
                    val authority = if (uri.port >= 0) "$tunerIp:${uri.port}" else tunerIp
                    uri.buildUpon()
                        .encodedAuthority(authority)
                        .build()
                        .toString()
                }
            }.getOrDefault(streamUrl)
        }

        /**
         * Matches a tuner channel to an existing broadcaster app channel based on
         * UK Freeview numbering (SD & HD) or normalized name.
         */
        fun findOverlappingAppChannel(
            item: HdHomeRunLineupItem,
            appChannels: List<Channel>
        ): Channel? {
            val num = item.guideNumber.trim()
            val nameNorm = item.guideName.lowercase().replace("[^a-z0-9]".toRegex(), "")

            // 1. Direct channel number match (e.g. 101, 102, 103, 104, 105, 118, etc.)
            val directMatch = appChannels.firstOrNull { it.channelNumber == num }
            if (directMatch != null) return directMatch

            // 2. UK Freeview SD to HD number mapping
            val freeviewSdMapping = mapOf(
                "1" to "bbc_one",
                "2" to "bbc_two",
                "3" to "itv1",
                "4" to "c4",
                "5" to "c5",
                "6" to "itv2",
                "7" to "itv3",
                "10" to "itv3",
                "9" to "bbc_four",
                "14" to "film4",
                "18" to "more4",
                "23" to "bbc_three",
                "25" to "itv4",
                "26" to "c5_star",
                "28" to "e4",
                "31" to "c5_usa",
                "32" to "c5_action",
                "33" to "itv_be",
                "45" to "c5_drama",
                "47" to "4seven",
                "201" to "cbbc",
                "202" to "cbeebies",
                "231" to "bbc_news"
            )

            freeviewSdMapping[num]?.let { targetId ->
                val match = appChannels.firstOrNull { it.id == targetId }
                if (match != null) return match
            }

            // 3. Normalized name matching
            return appChannels.firstOrNull { appCh ->
                val appNorm = appCh.name.lowercase().replace("[^a-z0-9]".toRegex(), "")
                val appShortNorm = appCh.shortName.lowercase().replace("[^a-z0-9]".toRegex(), "")
                nameNorm == appNorm || nameNorm == appShortNorm ||
                    nameNorm.startsWith(appShortNorm) || appNorm.startsWith(nameNorm)
            }
        }

        fun lookupTunerExclusiveSkySid(channelNumber: String, guideName: String): String {
            val name = guideName.lowercase()
            return when {
                name.contains("dave") -> "3809"
                name.contains("drama") -> "2612"
                name.contains("yesterday") -> "2305"
                name.contains("sky mix") -> "1831"
                name.contains("sky news") -> "4050"
                name.contains("talking picture") -> "5252"
                name.contains("gb news") -> "1196"
                name.contains("quest") -> "2510"
                name.contains("dmax") -> "2512"
                channelNumber == "11" -> "1831" // Sky Mix
                channelNumber == "19" -> "3809" // Dave
                channelNumber == "20" -> "2612" // Drama
                channelNumber == "26" -> "2305" // Yesterday
                channelNumber == "82" -> "5252" // Talking Pictures
                channelNumber == "233" -> "4050" // Sky News
                channelNumber == "236" -> "1196" // GB News
                else -> "0"
            }
        }

        fun lookupTunerLogo(guideName: String): String? {
            val name = guideName.lowercase()
            val baseUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom"
            return when {
                name.contains("dave") -> "$baseUrl/u-dave-uk.png"
                name.contains("drama") -> "$baseUrl/u-drama-uk.png"
                name.contains("yesterday") -> "$baseUrl/u-yesterday-uk.png"
                name.contains("sky mix") -> "$baseUrl/sky-mix-uk.png"
                name.contains("sky news") -> "$baseUrl/sky-news-uk.png"
                name.contains("talking") -> "$baseUrl/talking-pictures-tv-uk.png"
                name.contains("gb news") -> "$baseUrl/gb-news-uk.png"
                name.contains("quest") -> "$baseUrl/quest-uk.png"
                else -> null
            }
        }

        /**
         * Default UK Freeview channels for HDHomeRun tuners when scanning or offline.
         */
        fun getDefaultFreeviewLineup(tunerIp: String): List<HdHomeRunLineupItem> {
            val baseStream = "http://$tunerIp:5004/auto/v"
            return listOf(
                HdHomeRunLineupItem("1", "BBC One", "${baseStream}1", hd = 0),
                HdHomeRunLineupItem("2", "BBC Two", "${baseStream}2", hd = 0),
                HdHomeRunLineupItem("3", "ITV1", "${baseStream}3", hd = 0),
                HdHomeRunLineupItem("4", "Channel 4", "${baseStream}4", hd = 0),
                HdHomeRunLineupItem("5", "Channel 5", "${baseStream}5", hd = 0),
                HdHomeRunLineupItem("6", "ITV2", "${baseStream}6", hd = 0),
                HdHomeRunLineupItem("9", "BBC Four", "${baseStream}9", hd = 0),
                HdHomeRunLineupItem("10", "ITV3", "${baseStream}10", hd = 0),
                HdHomeRunLineupItem("11", "Sky Mix", "${baseStream}11", hd = 0),
                HdHomeRunLineupItem("14", "Film4", "${baseStream}14", hd = 0),
                HdHomeRunLineupItem("18", "More4", "${baseStream}18", hd = 0),
                HdHomeRunLineupItem("19", "U&Dave", "${baseStream}19", hd = 0),
                HdHomeRunLineupItem("20", "U&Drama", "${baseStream}20", hd = 0),
                HdHomeRunLineupItem("23", "BBC Three", "${baseStream}23", hd = 0),
                HdHomeRunLineupItem("25", "ITV4", "${baseStream}25", hd = 0),
                HdHomeRunLineupItem("26", "U&Yesterday", "${baseStream}26", hd = 0),
                HdHomeRunLineupItem("28", "E4", "${baseStream}28", hd = 0),
                HdHomeRunLineupItem("82", "Talking Pictures TV", "${baseStream}82", hd = 0),
                HdHomeRunLineupItem("101", "BBC One HD", "${baseStream}101", hd = 1),
                HdHomeRunLineupItem("102", "BBC Two HD", "${baseStream}102", hd = 1),
                HdHomeRunLineupItem("103", "ITV1 HD", "${baseStream}103", hd = 1),
                HdHomeRunLineupItem("104", "Channel 4 HD", "${baseStream}104", hd = 1),
                HdHomeRunLineupItem("105", "5 HD", "${baseStream}105", hd = 1),
                HdHomeRunLineupItem("233", "Sky News", "${baseStream}233", hd = 0),
                HdHomeRunLineupItem("236", "GB News", "${baseStream}236", hd = 0)
            )
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun getStoredTunerIp(): String? = prefs.getString(KEY_TUNER_IP, null)
    fun getStoredTunerName(): String? = prefs.getString(KEY_TUNER_NAME, null)

    /**
     * Loads the free HDHomeRun XMLTV guide and maps it to this tuner's lineup.
     * SiliconDust asks clients to refresh at randomized 20–28 hour intervals;
     * this cache uses a randomized 20–28 hour refresh interval.
     */
    suspend fun getTunerGuide(channels: List<Channel>): Map<String, List<Programme>> =
        withContext(Dispatchers.IO) {
            val tunerIp = getStoredTunerIp() ?: return@withContext emptyMap()
            val cacheFile = File(context.cacheDir, XMLTV_CACHE_FILE)
            val cachedXml = runCatching { cacheFile.takeIf { it.exists() }?.readText() }.getOrNull()

            val now = System.currentTimeMillis()
            val nextRefreshAt = prefs.getLong(KEY_XMLTV_NEXT_REFRESH_AT, 0L)
            val xml = if (cachedXml != null && now < nextRefreshAt) {
                cachedXml
            } else {
                val updatedXml = fetchXmltv(tunerIp)
                if (updatedXml != null) {
                    runCatching { cacheFile.writeText(updatedXml) }
                    val randomizedDelay = XMLTV_CACHE_MAX_AGE_MS + Random.nextLong(0, 8 * 60 * 60 * 1000L)
                    prefs.edit().putLong(KEY_XMLTV_NEXT_REFRESH_AT, now + randomizedDelay).apply()
                    updatedXml
                } else {
                    val retryDelay = XMLTV_CACHE_MAX_AGE_MS + Random.nextLong(0, 8 * 60 * 60 * 1000L)
                    prefs.edit().putLong(KEY_XMLTV_NEXT_REFRESH_AT, now + retryDelay).apply()
                    cachedXml
                }
            }

            if (xml.isNullOrBlank()) emptyMap() else parseXmltvForLineup(xml, channels)
        }

    private fun fetchXmltv(tunerIp: String): String? {
        val deviceInfo = try {
            val request = Request.Builder()
                .url("http://$tunerIp/discover.json")
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                json.decodeFromString<HdHomeRunDiscoverDevice>(response.body?.string().orEmpty())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read tuner guide authorization: ${e.message}")
            return null
        }
        val deviceAuth = deviceInfo.deviceAuth?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val url = XMLTV_URL.toHttpUrl().newBuilder()
                .addQueryParameter("DeviceAuth", deviceAuth)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/xml, text/xml")
                .build()
            httpClient.newBuilder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
                .newCall(request)
                .execute()
                .use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "HDHomeRun XMLTV request failed with HTTP ${response.code}")
                        return null
                    }
                    response.body?.string()?.takeIf { it.contains("<tv") }
                }
        } catch (e: Exception) {
            Log.w(TAG, "HDHomeRun XMLTV request failed: ${e.message}")
            null
        }
    }

    private data class XmltvProgramme(
        val channelId: String,
        val start: Long,
        val stop: Long,
        val title: String,
        val description: String
    )

    private fun parseXmltvForLineup(xml: String, channels: List<Channel>): Map<String, List<Programme>> {
        return try {
            val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(StringReader(xml))
            }
            val channelNames = mutableMapOf<String, MutableList<String>>()
            val listings = mutableListOf<XmltvProgramme>()
            var channelId: String? = null
            var programmeChannel: String? = null
            var programmeStart = 0L
            var programmeStop = 0L
            var programmeTitle = ""
            var programmeDescription = ""
            var capture: String? = null
            var capturedText = StringBuilder()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "channel" -> channelId = parser.getAttributeValue(null, "id")
                        "display-name" -> if (channelId != null) {
                            capture = "channel-name"
                            capturedText = StringBuilder()
                        }
                        "programme" -> {
                            programmeChannel = parser.getAttributeValue(null, "channel")
                            programmeStart = parseXmltvTime(parser.getAttributeValue(null, "start"))
                            programmeStop = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                            programmeTitle = ""
                            programmeDescription = ""
                        }
                        "title" -> if (programmeChannel != null) {
                            capture = "title"
                            capturedText = StringBuilder()
                        }
                        "desc" -> if (programmeChannel != null) {
                            capture = "description"
                            capturedText = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (capture != null) {
                        capturedText.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "display-name" -> if (capture == "channel-name" && channelId != null) {
                            channelNames.getOrPut(channelId!!) { mutableListOf() }
                                .add(capturedText.toString().trim())
                            capture = null
                        }
                        "title" -> if (capture == "title") {
                            programmeTitle = capturedText.toString().trim()
                            capture = null
                        }
                        "desc" -> if (capture == "description") {
                            programmeDescription = capturedText.toString().trim()
                            capture = null
                        }
                        "programme" -> {
                            val id = programmeChannel
                            if (id != null && programmeStart > 0 && programmeStop > programmeStart && programmeTitle.isNotBlank()) {
                                listings.add(XmltvProgramme(id, programmeStart, programmeStop, programmeTitle, programmeDescription))
                            }
                            programmeChannel = null
                            capture = null
                        }
                        "channel" -> channelId = null
                    }
                }
            }

            val byXmltvId = listings.groupBy { normalizeGuideToken(it.channelId) }
            val byXmltvName = channelNames.flatMap { (id, names) ->
                names.map { normalizeGuideName(it) to normalizeGuideToken(id) }
            }.filter { it.first.isNotBlank() }.groupBy({ it.first }, { it.second })

            channels.filter { it.isTunerChannel }.mapNotNull { channel ->
                val numberKey = normalizeGuideToken(channel.channelNumber)
                val nameKey = normalizeGuideName(channel.name)
                val matchedNumberListings = byXmltvId[numberKey]
                val xmltvIds = byXmltvName[nameKey]
                    ?: channelNames.filterValues { names -> names.any { normalizeGuideName(it) == nameKey } }
                        .keys.map { normalizeGuideToken(it) }
                val programmes = matchedNumberListings ?: xmltvIds.flatMap { byXmltvId[it].orEmpty() }
                    .distinctBy { it.start }
                    .sortedBy { it.start }
                val mappedProgrammes = programmes.map { item ->
                    Programme(
                        id = "${channel.id}_${item.start}",
                        channelId = channel.id,
                        title = item.title,
                        synopsis = item.description.ifBlank { "No description available." },
                        startTimeEpochSeconds = item.start,
                        durationSeconds = item.stop - item.start
                    )
                }
                if (mappedProgrammes.isNotEmpty()) channel.id to mappedProgrammes else null
            }.toMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse HDHomeRun XMLTV guide: ${e.message}")
            emptyMap()
        }
    }

    private fun parseXmltvTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val normalized = value.trim().replace(Regex("^(\\d{14})([+-]\\d{4})$"), "\$1 \$2")
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                return format.parse(normalized)?.time?.div(1000) ?: 0L
            } catch (_: Exception) {
                // Try the next supported XMLTV timestamp form.
            }
        }
        return 0L
    }

    private fun normalizeGuideToken(value: String): String =
        value.lowercase(Locale.US).replace("[^a-z0-9]".toRegex(), "")

    private fun normalizeGuideName(value: String): String =
        normalizeGuideToken(value).removeSuffix("hd").removeSuffix("highdefinition")

    fun setManualTunerIp(ip: String) {
        prefs.edit().putString(KEY_TUNER_IP, ip.trim()).apply()
    }

    /**
     * Finds an HDHomeRun locally first, then falls back to SiliconDust's online discovery service.
     */
    suspend fun discoverTuner(): HdHomeRunDiscoverDevice? = withContext(Dispatchers.IO) {
        val primary = discoverOnLocalNetwork() ?: discoverThroughCloud()
        if (primary != null) {
            prefs.edit()
                .putString(KEY_TUNER_IP, primary.localIp)
                .putString(KEY_TUNER_NAME, primary.friendlyName ?: primary.modelNumber ?: "HDHomeRun")
                .putString(KEY_TUNER_DEVICE_ID, primary.deviceId)
                .apply()
            Log.i(TAG, "Discovered HDHomeRun: ${primary.friendlyName} at ${primary.localIp}")
        }
        primary
    }

    private fun discoverOnLocalNetwork(): HdHomeRunDiscoverDevice? {
        val host = "hdhomerun.local"
        return try {
            val request = Request.Builder()
                .url("http://$host/discover.json")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val device = json.decodeFromString<HdHomeRunDiscoverDevice>(body)
                val ipAddress = device.localIp
                    ?.takeIf { it.isNotBlank() }
                    ?: resolveIpv4(host)
                    ?: device.baseUrl?.let { baseUrl ->
                        runCatching { resolveIpv4(baseUrl.toHttpUrl().host) }.getOrNull()
                    }
                if (ipAddress.isNullOrBlank()) return null
                device.copy(localIp = ipAddress)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Local HDHomeRun discovery unavailable: ${e.message}")
            null
        }
    }

    private fun discoverThroughCloud(): HdHomeRunDiscoverDevice? {
        return try {
            val request = Request.Builder()
                .url(DISCOVER_URL)
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null

                val devices = try {
                    json.decodeFromString<List<HdHomeRunDiscoverDevice>>(body)
                } catch (_: Exception) {
                    try {
                        listOf(json.decodeFromString<HdHomeRunDiscoverDevice>(body))
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                devices.firstOrNull { !it.localIp.isNullOrBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "HDHomeRun discovery failed: ${e.message}")
            null
        }
    }

    private fun resolveIpv4(host: String): String? = runCatching {
        InetAddress.getAllByName(host)
            .firstOrNull { it is Inet4Address }
            ?.hostAddress
    }.getOrNull()

    /**
     * Loads the channel lineup from the tuner or local cache / fallback.
     */
    suspend fun getTunerChannels(forceRefresh: Boolean = false): List<Channel> = withContext(Dispatchers.IO) {
        val tunerIp = getStoredTunerIp()

        if (forceRefresh || tunerIp != null) {
            val freshLineup = fetchLineupFromTuner(tunerIp)
            if (freshLineup.isNotEmpty()) {
                saveLineupCache(freshLineup)
                return@withContext mapLineupToChannels(freshLineup, tunerIp ?: "127.0.0.1")
            }
        }

        // Try reading cached lineup
        val cached = readLineupCache()
        if (cached.isNotEmpty()) {
            return@withContext mapLineupToChannels(cached, tunerIp ?: "127.0.0.1")
        }

        // Do not invent a tuner at a placeholder IP. That created channels that
        // could neither provide guide data nor play. A tuner must be discovered,
        // configured, or have a previously fetched lineup cached.
        emptyList()
    }

    private fun fetchLineupFromTuner(tunerIp: String?): List<HdHomeRunLineupItem> {
        if (tunerIp.isNullOrBlank()) return emptyList()
        val url = "http://$tunerIp/lineup.json"
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                json.decodeFromString<List<HdHomeRunLineupItem>>(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch lineup from $url: ${e.message}")
            emptyList()
        }
    }

    private fun saveLineupCache(items: List<HdHomeRunLineupItem>) {
        try {
            val file = File(context.cacheDir, LINEUP_CACHE_FILE)
            val jsonStr = json.encodeToString(items)
            file.writeText(jsonStr)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache lineup: ${e.message}")
        }
    }

    private fun readLineupCache(): List<HdHomeRunLineupItem> {
        return try {
            val file = File(context.cacheDir, LINEUP_CACHE_FILE)
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<HdHomeRunLineupItem>>(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }
}
