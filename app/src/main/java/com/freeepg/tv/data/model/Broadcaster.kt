package com.freeepg.tv.data.model

import androidx.compose.ui.graphics.Color

enum class Broadcaster(
    val id: String,
    val displayName: String,
    val shortName: String,
    val badgeText: String,
    val brandColor: Color,
    val brandGradient: List<Color>,
    val primaryPackage: String,
    val alternativePackages: List<String>,
    val defaultWebBaseUrl: String
) {
    BBC(
        id = "bbc",
        displayName = "BBC iPlayer",
        shortName = "iPlayer",
        badgeText = "BBC",
        brandColor = Color(0xFFFF1970), // Vibrant BBC iPlayer pink, distinctly contrasting with red NOW needle
        brandGradient = listOf(Color(0xFFFF3385), Color(0xFFD60055)),
        primaryPackage = "bbc.iplayer.android",
        alternativePackages = listOf("uk.co.bbc.iplayer", "com.nvidia.bbciplayer", "uk.co.bbc.iplayertv", "uk.co.bbc.android.iplayer"),
        defaultWebBaseUrl = "https://www.bbc.co.uk/iplayer"
    ),
    ITV(
        id = "itv",
        displayName = "ITVX",
        shortName = "ITVX",
        badgeText = "ITVX",
        brandColor = Color(0xFFFFD500),
        brandGradient = listOf(Color(0xFFFFE033), Color(0xFFFF9E00)),
        primaryPackage = "air.ITVMobilePlayer",
        alternativePackages = listOf(
            "com.itv.tenft.itvhub",
            "com.itv.itvx",
            "com.itv.itvplayer",
            "com.itv.hub",
            "com.itv.androidtv",
            "com.itv.trinty",
            "com.itv.ukplayer",
            "air.itv.hub"
        ),
        defaultWebBaseUrl = "https://www.itv.com"
    ),
    CHANNEL_4(
        id = "channel4",
        displayName = "Channel 4",
        shortName = "C4",
        badgeText = "C4",
        brandColor = Color(0xFF00E676),
        brandGradient = listOf(Color(0xFF26FF91), Color(0xFF00B050)),
        primaryPackage = "com.channel4.ondemand",
        alternativePackages = listOf("com.channel4.all4.androidtv", "uk.c4.white"),
        defaultWebBaseUrl = "https://www.channel4.com"
    ),
    CHANNEL_5(
        id = "channel5",
        displayName = "Channel 5",
        shortName = "My5",
        badgeText = "My5",
        brandColor = Color(0xFFD500F9),
        brandGradient = listOf(Color(0xFFEA39FF), Color(0xFFAA00FF)),
        primaryPackage = "com.channel5.my5",
        alternativePackages = listOf(
            "com.mobileiq.demand5",
            "com.channel5.my5.androidtv",
            "com.my5.tv",
            "com.my5.androidtv"
        ),
        defaultWebBaseUrl = "https://www.channel5.com"
    ),
    TUNER(
        id = "tuner",
        displayName = "Tuner",
        shortName = "Tuner",
        badgeText = "TUNER",
        brandColor = Color(0xFF00B0FF),
        brandGradient = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
        primaryPackage = "com.silicondust.view",
        alternativePackages = listOf(
            "org.videolan.vlc",
            "com.brouken.player",
            "org.courville.nova",
            "com.mxtech.videoplayer.ad"
        ),
        defaultWebBaseUrl = "http://api.hdhomerun.com"
    );

    val allPackages: List<String>
        get() = listOf(primaryPackage) + alternativePackages
}
