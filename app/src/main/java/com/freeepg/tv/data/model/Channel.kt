package com.freeepg.tv.data.model

data class Channel(
    val id: String,
    val skySid: String,
    val channelNumber: String,
    val name: String,
    val shortName: String,
    val broadcaster: Broadcaster,
    val logoUrl: String? = null,
    /**
     * Specific deep link URIs to launch directly into the live broadcast stream on Android TV.
     * Ordered from highest priority (specific live intent) to generic fallback.
     */
    val deepLinkUris: List<String>,
    val webLiveUrl: String
) {
    companion object {
        val ALL_CHANNELS: List<Channel> = listOf(
            // === BBC iPlayer ===
            Channel(
                id = "bbc_one",
                skySid = "3000",
                channelNumber = "101",
                name = "BBC One HD",
                shortName = "BBC One",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-one-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/bbcone"
            ),
            Channel(
                id = "bbc_two",
                skySid = "2075",
                channelNumber = "102",
                name = "BBC Two HD",
                shortName = "BBC Two",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-two-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/bbctwo"
            ),
            Channel(
                id = "bbc_three",
                skySid = "2080",
                channelNumber = "115",
                name = "BBC Three HD",
                shortName = "BBC Three",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-three-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/bbcthree"
            ),
            Channel(
                id = "bbc_four",
                skySid = "2086",
                channelNumber = "116",
                name = "BBC Four HD",
                shortName = "BBC Four",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-four-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/bbcfour"
            ),
            Channel(
                id = "bbc_news",
                skySid = "2085",
                channelNumber = "503",
                name = "BBC News HD",
                shortName = "BBC News",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-news-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/bbcnews"
            ),
            Channel(
                id = "cbbc",
                skySid = "2078",
                channelNumber = "607",
                name = "CBBC HD",
                shortName = "CBBC",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-cbbc-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/cbbc"
            ),
            Channel(
                id = "cbeebies",
                skySid = "2088",
                channelNumber = "608",
                name = "CBeebies HD",
                shortName = "CBeebies",
                broadcaster = Broadcaster.BBC,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/bbc-cbeebies-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.bbc.co.uk/iplayer/live/cbeebies"
            ),

            // === ITVX ===
            Channel(
                id = "itv1",
                skySid = "6504",
                channelNumber = "103",
                name = "ITV1 HD",
                shortName = "ITV1",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-1-uk.png",
                deepLinkUris = listOf("https://www.itv.com/watch?channel=itv"),
                webLiveUrl = "https://www.itv.com/watch?channel=itv"
            ),
            Channel(
                id = "itv2",
                skySid = "6532",
                channelNumber = "118",
                name = "ITV2 HD",
                shortName = "ITV2",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-2-uk.png",
                deepLinkUris = listOf("https://www.itv.com/watch?channel=itv2"),
                webLiveUrl = "https://www.itv.com/watch?channel=itv2"
            ),
            Channel(
                id = "itv3",
                skySid = "6533",
                channelNumber = "119",
                name = "ITV3 HD",
                shortName = "ITV3",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-3-uk.png",
                deepLinkUris = listOf("https://www.itv.com/watch?channel=itv3"),
                webLiveUrl = "https://www.itv.com/watch?channel=itv3"
            ),
            Channel(
                id = "itv4",
                skySid = "6534",
                channelNumber = "120",
                name = "ITV4 HD",
                shortName = "ITV4",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-4-uk.png",
                deepLinkUris = listOf("https://www.itv.com/watch?channel=itv4"),
                webLiveUrl = "https://www.itv.com/watch?channel=itv4"
            ),
            Channel(
                id = "itv_be",
                skySid = "3026",
                channelNumber = "131",
                name = "ITVBe",
                shortName = "ITVBe",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-be-uk.png",
                // ITVBe's web route falls back to another live feed in the current ITVX TV app.
                // Use menu navigation instead of claiming this unverified route selects ITVBe.
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.itv.com/watch?channel=itvbe"
            ),

            // === Channel 4 ===
            Channel(
                id = "c4",
                skySid = "4075",
                channelNumber = "104",
                name = "Channel 4 HD",
                shortName = "Channel 4",
                broadcaster = Broadcaster.CHANNEL_4,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/channel-4-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel4.com/now/C4",
                    "all4://now/C4",
                    "channel4://now/C4",
                    "https://www.channel4.com"
                ),
                webLiveUrl = "https://www.channel4.com/now/C4"
            ),
            Channel(
                id = "e4",
                skySid = "4076",
                channelNumber = "135",
                name = "E4 HD",
                shortName = "E4",
                broadcaster = Broadcaster.CHANNEL_4,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/e-4-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel4.com/now/E4",
                    "all4://now/E4",
                    "channel4://now/E4"
                ),
                webLiveUrl = "https://www.channel4.com/now/E4"
            ),
            Channel(
                id = "more4",
                skySid = "4043",
                channelNumber = "136",
                name = "More4 HD",
                shortName = "More4",
                broadcaster = Broadcaster.CHANNEL_4,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/4-more-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel4.com/now/M4",
                    "all4://now/M4"
                ),
                webLiveUrl = "https://www.channel4.com/now/M4"
            ),
            Channel(
                id = "film4",
                skySid = "4044",
                channelNumber = "313",
                name = "Film4 HD",
                shortName = "Film4",
                broadcaster = Broadcaster.CHANNEL_4,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/film-4-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel4.com/now/F4",
                    "all4://now/F4"
                ),
                webLiveUrl = "https://www.channel4.com/now/F4"
            ),
            Channel(
                id = "4seven",
                skySid = "3150",
                channelNumber = "137",
                name = "4seven",
                shortName = "4seven",
                broadcaster = Broadcaster.CHANNEL_4,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/4-seven-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel4.com/now/4S",
                    "all4://now/4S"
                ),
                webLiveUrl = "https://www.channel4.com/now/4S"
            ),

            // === Channel 5 (My5) ===
            Channel(
                id = "c5",
                skySid = "4058",
                channelNumber = "105",
                name = "5 HD",
                shortName = "Channel 5",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/channel-5-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.my5.tv/live/channel-5"
            ),
            Channel(
                id = "c5_action",
                skySid = "1036",
                channelNumber = "153",
                name = "5ACTION",
                shortName = "5ACTION",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-action-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.my5.tv/live/5action"
            ),
            Channel(
                id = "c5_star",
                skySid = "3023",
                channelNumber = "128",
                name = "5STAR",
                shortName = "5STAR",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-star-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.my5.tv/live/5star"
            ),
            Channel(
                id = "c5_usa",
                skySid = "3022",
                channelNumber = "141",
                name = "5 USA",
                shortName = "5 USA",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-usa-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.my5.tv/live/5usa"
            ),
            Channel(
                id = "c5_drama",
                skySid = "3028",
                channelNumber = "150",
                name = "5DRAMA",
                shortName = "5DRAMA",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/channel-5-uk.png",
                deepLinkUris = emptyList(),
                webLiveUrl = "https://www.my5.tv/live/5drama"
            )
        )
    }
}
