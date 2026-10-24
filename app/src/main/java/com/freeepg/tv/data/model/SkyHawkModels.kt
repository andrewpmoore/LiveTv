package com.freeepg.tv.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkyHawkScheduleRoot(
    @SerialName("date") val date: String? = null,
    @SerialName("schedule") val schedule: List<SkyHawkChannelSchedule> = emptyList()
)

@Serializable
data class SkyHawkChannelSchedule(
    @SerialName("sid") val sid: String,
    @SerialName("events") val events: List<SkyHawkEvent> = emptyList()
)

@Serializable
data class SkyHawkEvent(
    @SerialName("eid") val eid: String? = null,
    @SerialName("programmeuuid") val programmeUuid: String? = null,
    /**
     * Start time in Unix epoch seconds
     */
    @SerialName("st") val startTime: Long,
    /**
     * Duration in seconds
     */
    @SerialName("d") val duration: Long,
    /**
     * Programme title
     */
    @SerialName("t") val title: String,
    /**
     * Programme synopsis / description
     */
    @SerialName("sy") val synopsis: String? = null,
    @SerialName("seasonnumber") val seasonNumber: Int? = null,
    @SerialName("episodenumber") val episodeNumber: Int? = null,
    @SerialName("hd") val hd: Boolean = false,
    @SerialName("new") val isNew: Boolean = false,
    /**
     * Subtitles available
     */
    @SerialName("s") val subtitles: Boolean = false,
    /**
     * Audio Description available
     */
    @SerialName("ad") val audioDescription: Boolean = false,
    @SerialName("r") val rating: String? = null
)
