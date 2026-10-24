package com.freeepg.tv.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Programme(
    val id: String,
    val channelId: String,
    val title: String,
    val synopsis: String,
    val startTimeEpochSeconds: Long,
    val durationSeconds: Long,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val isHd: Boolean = false,
    val isNew: Boolean = false,
    val hasSubtitles: Boolean = false,
    val hasAudioDescription: Boolean = false,
    val genre: String? = null,
    val rating: String? = null,
    val programmeUuid: String? = null
) {
    fun imageUrl(): String? {
        val uuid = programmeUuid?.takeIf { it.isNotBlank() } ?: return null
        return "https://imageservice.sky.com/pd-image/$uuid/16-9/500?territory=GB&provider=SKY&proposition=SKYQ"
    }

    val endTimeEpochSeconds: Long
        get() = startTimeEpochSeconds + durationSeconds

    val durationMinutes: Long
        get() = (durationSeconds + 59) / 60

    fun isLiveNow(currentTimeEpochSeconds: Long): Boolean {
        return currentTimeEpochSeconds in startTimeEpochSeconds until endTimeEpochSeconds
    }

    fun isUpcoming(currentTimeEpochSeconds: Long): Boolean {
        return startTimeEpochSeconds > currentTimeEpochSeconds
    }

    fun progress(currentTimeEpochSeconds: Long): Float {
        if (durationSeconds <= 0) return 0f
        if (currentTimeEpochSeconds < startTimeEpochSeconds) return 0f
        if (currentTimeEpochSeconds >= endTimeEpochSeconds) return 1f
        return (currentTimeEpochSeconds - startTimeEpochSeconds).toFloat() / durationSeconds.toFloat()
    }

    fun formattedTimeRange(): String {
        val format = SimpleDateFormat("HH:mm", Locale.UK)
        val startStr = format.format(Date(startTimeEpochSeconds * 1000))
        val endStr = format.format(Date(endTimeEpochSeconds * 1000))
        val durationStr = if (durationMinutes >= 60) {
            val hrs = durationMinutes / 60
            val mins = durationMinutes % 60
            if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
        } else {
            "${durationMinutes}m"
        }
        return "$startStr - $endStr ($durationStr)"
    }

    fun formattedStartTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.UK)
        return format.format(Date(startTimeEpochSeconds * 1000))
    }

    fun formattedEndTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.UK)
        return format.format(Date(endTimeEpochSeconds * 1000))
    }

    fun formattedEpisodeInfo(): String? {
        return when {
            seasonNumber != null && episodeNumber != null -> "Series $seasonNumber Ep $episodeNumber"
            episodeNumber != null -> "Ep $episodeNumber"
            seasonNumber != null -> "Series $seasonNumber"
            else -> null
        }
    }
}
