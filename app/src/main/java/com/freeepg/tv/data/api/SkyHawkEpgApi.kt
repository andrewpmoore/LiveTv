package com.freeepg.tv.data.api

import com.freeepg.tv.data.model.SkyHawkEvent
import com.freeepg.tv.data.model.SkyHawkScheduleRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class SkyHawkEpgApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    companion object {
        private const val BASE_URL = "https://awk.epgsky.com/hawk/linear/schedule"
    }

    /**
     * Fetches the broadcast schedule for a given Sky service ID (SID) on a specific date (YYYYMMDD).
     */
    suspend fun getScheduleForChannel(dateStr: String, sid: String): List<SkyHawkEvent> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/$dateStr/$sid"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android TV; FreeEPG/1.0)")
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val parsed = json.decodeFromString<SkyHawkScheduleRoot>(bodyString)
                val channelSchedule = parsed.schedule.firstOrNull { it.sid == sid }
                    ?: parsed.schedule.firstOrNull()
                channelSchedule?.events ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
