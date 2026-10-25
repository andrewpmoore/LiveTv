package com.freeepg.tv.tuner

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HdHomeRunDiscoverDevice(
    @SerialName("DeviceID") val deviceId: String? = null,
    @SerialName("LocalIP") val localIp: String? = null,
    @SerialName("BaseURL") val baseUrl: String? = null,
    @SerialName("DiscoverURL") val discoverUrl: String? = null,
    @SerialName("LineupURL") val lineupUrl: String? = null,
    @SerialName("ModelNumber") val modelNumber: String? = null,
    @SerialName("FriendlyName") val friendlyName: String? = null,
    @SerialName("DeviceAuth") val deviceAuth: String? = null
)

@Serializable
data class HdHomeRunLineupItem(
    @SerialName("GuideNumber") val guideNumber: String,
    @SerialName("GuideName") val guideName: String,
    @SerialName("URL") val url: String,
    @SerialName("HD") val hd: Int? = null,
    @SerialName("Favorite") val favorite: Int? = null
)
