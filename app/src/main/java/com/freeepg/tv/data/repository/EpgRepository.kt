package com.freeepg.tv.data.repository

import android.content.Context
import android.util.Log
import com.freeepg.tv.data.api.SkyHawkEpgApi
import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.data.model.SkyHawkEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EpgRepository(
    private val context: Context,
    private val api: SkyHawkEpgApi = SkyHawkEpgApi()
) {
    companion object {
        private const val TAG = "EpgRepository"
        private const val CACHE_FILE_NAME = "epg_24h_cache.json"
        private const val BUNDLED_ASSET_FILE = "bundled_epg.json"
        private const val CACHE_VALIDITY_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val inMemoryCache = mutableMapOf<String, List<Programme>>()

    fun getAllChannels(): List<Channel> = Channel.ALL_CHANNELS

    fun getChannelsForBroadcaster(broadcaster: Broadcaster?): List<Channel> {
        return if (broadcaster == null) {
            Channel.ALL_CHANNELS
        } else {
            Channel.ALL_CHANNELS.filter { it.broadcaster == broadcaster }
        }
    }

    /**
     * Loads 24-hour programme guide data.
     * Checks in-memory cache, then persistent file cache, bundled assets, and fetches latest from API.
     */
    suspend fun loadSchedule(
        forceRefresh: Boolean = false,
        channels: List<Channel> = Channel.ALL_CHANNELS
    ): Map<String, List<Programme>> = withContext(Dispatchers.IO) {
        val nowSeconds = System.currentTimeMillis() / 1000

        if (!forceRefresh && inMemoryCache.isNotEmpty()) {
            return@withContext inMemoryCache
        }

        // 1. Try reading disk cache first
        if (!forceRefresh) {
            val diskCached = readDiskCache()
            if (diskCached.isNotEmpty()) {
                inMemoryCache.putAll(diskCached)
                val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
                if (System.currentTimeMillis() - cacheFile.lastModified() < CACHE_VALIDITY_MS) {
                    return@withContext diskCached
                }
            }
        }

        // 2. Fall back to pre-bundled asset cache for instant 0ms startup
        if (inMemoryCache.isEmpty()) {
            val assetCached = readAssetCache()
            if (assetCached.isNotEmpty()) {
                inMemoryCache.putAll(assetCached)
            }
        }

        // If we already have valid cached/bundled data, return it immediately so the UI is instantaneous
        val cachedSnapshot = if (inMemoryCache.isNotEmpty()) HashMap(inMemoryCache) else null
        if (cachedSnapshot != null && !forceRefresh) {
            // Trigger background refresh with semaphore so UI is never blocked
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val freshData = fetchScheduleFromNetwork(channels, nowSeconds)
                    if (freshData.isNotEmpty()) {
                        inMemoryCache.putAll(freshData)
                        saveDiskCache(inMemoryCache)
                        Log.d(TAG, "Background schedule refresh completed: ${freshData.size} channels")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Background schedule refresh failed: ${e.message}")
                }
            }
            return@withContext cachedSnapshot
        }

        // Fetch fresh schedule data from API for today and tomorrow (guarantees next 24 hours)
        val freshData = fetchScheduleFromNetwork(channels, nowSeconds)
        if (freshData.isNotEmpty()) {
            inMemoryCache.putAll(freshData)
            saveDiskCache(inMemoryCache)
            return@withContext inMemoryCache
        }

        return@withContext inMemoryCache
    }

    private suspend fun fetchScheduleFromNetwork(
        channels: List<Channel>,
        nowSeconds: Long
    ): Map<String, List<Programme>> = coroutineScope {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.UK)
        val calendar = Calendar.getInstance()
        val todayStr = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = dateFormat.format(calendar.time)

        val resultMap = mutableMapOf<String, List<Programme>>()
        val semaphore = Semaphore(4) // Throttle to 4 concurrent network requests to prevent emulator ANR

        val deferredList = channels.map { channel ->
            async {
                semaphore.withPermit {
                    val todayEvents = api.getScheduleForChannel(todayStr, channel.skySid)
                    val tomorrowEvents = api.getScheduleForChannel(tomorrowStr, channel.skySid)
                    val combined = (todayEvents + tomorrowEvents)
                        .distinctBy { it.startTime }
                        .sortedBy { it.startTime }

                    val mappedProgrammes = combined.map { event ->
                        mapToProgramme(event, channel.id)
                    }.filter { programme ->
                        programme.endTimeEpochSeconds >= (nowSeconds - 7200) &&
                                programme.startTimeEpochSeconds <= (nowSeconds + 26 * 3600)
                    }
                    channel.id to mappedProgrammes
                }
            }
        }

        deferredList.awaitAll().forEach { (channelId, programmes) ->
            if (programmes.isNotEmpty()) {
                resultMap[channelId] = programmes
            }
        }

        resultMap
    }

    private fun mapToProgramme(event: SkyHawkEvent, channelId: String): Programme {
        val titleClean = event.title.replace("\\*\\*Visually Signed\\*\\*\\s*".toRegex(), "")
        return Programme(
            id = "${channelId}_${event.startTime}",
            channelId = channelId,
            title = titleClean,
            synopsis = event.synopsis ?: "No description available.",
            startTimeEpochSeconds = event.startTime,
            durationSeconds = event.duration,
            seasonNumber = event.seasonNumber,
            episodeNumber = event.episodeNumber,
            isHd = event.hd,
            isNew = event.isNew,
            hasSubtitles = event.subtitles,
            hasAudioDescription = event.audioDescription,
            rating = event.rating?.takeIf { it != "--" },
            programmeUuid = event.programmeUuid
        )
    }

    private fun saveDiskCache(data: Map<String, List<Programme>>) {
        try {
            val root = JSONObject()
            root.put("timestamp", System.currentTimeMillis())
            val channelsJson = JSONObject()

            for ((chId, programmes) in data) {
                val arr = JSONArray()
                for (p in programmes) {
                    val pObj = JSONObject().apply {
                        put("id", p.id)
                        put("channelId", p.channelId)
                        put("title", p.title)
                        put("synopsis", p.synopsis)
                        put("st", p.startTimeEpochSeconds)
                        put("d", p.durationSeconds)
                        p.seasonNumber?.let { put("s", it) }
                        p.episodeNumber?.let { put("ep", it) }
                        put("hd", p.isHd)
                        put("new", p.isNew)
                        put("sub", p.hasSubtitles)
                        put("ad", p.hasAudioDescription)
                        p.programmeUuid?.let { put("uuid", it) }
                    }
                    arr.put(pObj)
                }
                channelsJson.put(chId, arr)
            }
            root.put("channels", channelsJson)

            val file = File(context.cacheDir, CACHE_FILE_NAME)
            file.writeText(root.toString())
            Log.d(TAG, "Cached EPG schedule to disk: ${file.length()} bytes")
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving disk cache: ${e.message}")
        }
    }

    private fun readDiskCache(): Map<String, List<Programme>> {
        val file = File(context.cacheDir, CACHE_FILE_NAME)
        if (!file.exists()) return emptyMap()

        return try {
            parseEpgJson(file.readText())
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading disk cache: ${e.message}")
            emptyMap()
        }
    }

    private fun readAssetCache(): Map<String, List<Programme>> {
        return try {
            val text = context.assets.open(BUNDLED_ASSET_FILE).bufferedReader().use { it.readText() }
            parseEpgJson(text)
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading bundled asset cache: ${e.message}")
            emptyMap()
        }
    }

    private fun parseEpgJson(text: String): Map<String, List<Programme>> {
        val root = JSONObject(text)
        val channelsJson = root.getJSONObject("channels")
        val result = mutableMapOf<String, List<Programme>>()

        val keys = channelsJson.keys()
        while (keys.hasNext()) {
            val chId = keys.next()
            val arr = channelsJson.getJSONArray(chId)
            val list = mutableListOf<Programme>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Programme(
                        id = obj.getString("id"),
                        channelId = obj.getString("channelId"),
                        title = obj.getString("title"),
                        synopsis = obj.getString("synopsis"),
                        startTimeEpochSeconds = obj.getLong("st"),
                        durationSeconds = obj.getLong("d"),
                        seasonNumber = if (obj.has("s")) obj.getInt("s") else null,
                        episodeNumber = if (obj.has("ep")) obj.getInt("ep") else null,
                        isHd = obj.optBoolean("hd", false),
                        isNew = obj.optBoolean("new", false),
                        hasSubtitles = obj.optBoolean("sub", false),
                        hasAudioDescription = obj.optBoolean("ad", false),
                        programmeUuid = if (obj.has("uuid")) obj.getString("uuid") else null
                    )
                )
            }
            result[chId] = list
        }
        return result
    }
}
