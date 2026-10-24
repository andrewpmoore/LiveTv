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
                deepLinkUris = listOf(
                    "https://www.live.bbctvapps.co.uk/bbcone",
                    "https://www.live.bbctvapps.co.uk/stream/bbcone",
                    "https://www.live.bbctvapps.co.uk",
                    "bbc://iplayer/playback/bbc_one_hd",
                    "bbciplayer://live/bbcone",
                    "https://www.bbc.co.uk/iplayer/live/bbcone"
                ),
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
                deepLinkUris = listOf(
                    "https://www.live.bbctvapps.co.uk/bbctwo",
                    "https://www.live.bbctvapps.co.uk/stream/bbctwo",
                    "https://www.live.bbctvapps.co.uk",
                    "bbc://iplayer/playback/bbc_two_hd",
                    "bbciplayer://live/bbctwo",
                    "https://www.bbc.co.uk/iplayer/live/bbctwo"
                ),
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
                deepLinkUris = listOf(
                    "bbc://iplayer/playback/bbc_three_hd",
                    "bbciplayer://live/bbcthree",
                    "https://www.bbc.co.uk/iplayer/live/bbcthree"
                ),
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
                deepLinkUris = listOf(
                    "bbc://iplayer/playback/bbc_four_hd",
                    "bbciplayer://live/bbcfour",
                    "https://www.bbc.co.uk/iplayer/live/bbcfour"
                ),
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
                deepLinkUris = listOf(
                    "bbc://iplayer/playback/bbc_news_channel_hd",
                    "bbciplayer://live/bbcnews",
                    "https://www.bbc.co.uk/iplayer/live/bbcnews"
                ),
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
                deepLinkUris = listOf(
                    "bbc://iplayer/playback/cbbc_hd",
                    "bbciplayer://live/cbbc",
                    "https://www.bbc.co.uk/iplayer/live/cbbc"
                ),
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
                deepLinkUris = listOf(
                    "bbc://iplayer/playback/cbeebies_hd",
                    "bbciplayer://live/cbeebies",
                    "https://www.bbc.co.uk/iplayer/live/cbeebies"
                ),
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
                deepLinkUris = listOf(
                    "https://www.itv.com/watch/live/itv1",
                    "itvx://watch/live/itv1",
                    "https://www.itv.com/watch"
                ),
                webLiveUrl = "https://www.itv.com/watch/live/itv1"
            ),
            Channel(
                id = "itv2",
                skySid = "6532",
                channelNumber = "118",
                name = "ITV2 HD",
                shortName = "ITV2",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-2-uk.png",
                deepLinkUris = listOf(
                    "https://www.itv.com/watch/live/itv2",
                    "itvx://watch/live/itv2",
                    "https://www.itv.com/watch"
                ),
                webLiveUrl = "https://www.itv.com/watch/live/itv2"
            ),
            Channel(
                id = "itv3",
                skySid = "6533",
                channelNumber = "119",
                name = "ITV3 HD",
                shortName = "ITV3",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-3-uk.png",
                deepLinkUris = listOf(
                    "https://www.itv.com/watch/live/itv3",
                    "itvx://watch/live/itv3",
                    "https://www.itv.com/watch"
                ),
                webLiveUrl = "https://www.itv.com/watch/live/itv3"
            ),
            Channel(
                id = "itv4",
                skySid = "6534",
                channelNumber = "120",
                name = "ITV4 HD",
                shortName = "ITV4",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-4-uk.png",
                deepLinkUris = listOf(
                    "https://www.itv.com/watch/live/itv4",
                    "itvx://watch/live/itv4",
                    "https://www.itv.com/watch"
                ),
                webLiveUrl = "https://www.itv.com/watch/live/itv4"
            ),
            Channel(
                id = "itv_be",
                skySid = "3026",
                channelNumber = "131",
                name = "ITVBe",
                shortName = "ITVBe",
                broadcaster = Broadcaster.ITV,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/itv-be-uk.png",
                deepLinkUris = listOf(
                    "https://www.itv.com/watch/live/itvbe",
                    "itvx://watch/live/itvbe"
                ),
                webLiveUrl = "https://www.itv.com/watch/live/itvbe"
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
                deepLinkUris = listOf(
                    "https://www.channel5.com/channel/5",
                    "my5://live/5",
                    "https://www.channel5.com"
                ),
                webLiveUrl = "https://www.channel5.com"
            ),
            Channel(
                id = "c5_action",
                skySid = "1036",
                channelNumber = "153",
                name = "5ACTION",
                shortName = "5ACTION",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-action-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel5.com/channel/5action",
                    "my5://live/5action"
                ),
                webLiveUrl = "https://www.channel5.com"
            ),
            Channel(
                id = "c5_star",
                skySid = "3023",
                channelNumber = "128",
                name = "5STAR",
                shortName = "5STAR",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-star-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel5.com/channel/5star",
                    "my5://live/5star"
                ),
                webLiveUrl = "https://www.channel5.com"
            ),
            Channel(
                id = "c5_usa",
                skySid = "3022",
                channelNumber = "141",
                name = "5 USA",
                shortName = "5 USA",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/5-usa-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel5.com/channel/5usa",
                    "my5://live/5usa"
                ),
                webLiveUrl = "https://www.channel5.com"
            ),
            Channel(
                id = "c5_drama",
                skySid = "3028",
                channelNumber = "150",
                name = "5DRAMA",
                shortName = "5DRAMA",
                broadcaster = Broadcaster.CHANNEL_5,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/channel-5-uk.png",
                deepLinkUris = listOf(
                    "https://www.channel5.com/channel/5drama",
                    "my5://live/5drama"
                ),
                webLiveUrl = "https://www.channel5.com"
            ),

            // === U (UKTV Play) ===
            Channel(
                id = "u_dave",
                skySid = "3809",
                channelNumber = "111",
                name = "U&Dave HD",
                shortName = "U&Dave",
                broadcaster = Broadcaster.U,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/dave-uk.png",
                deepLinkUris = listOf(
                    "https://uktvplay.uktv.co.uk/channel/dave",
                    "uktvplay://channel/dave",
                    "https://uktvplay.uktv.co.uk"
                ),
                webLiveUrl = "https://uktvplay.uktv.co.uk/channel/dave"
            ),
            Channel(
                id = "u_drama",
                skySid = "2612",
                channelNumber = "143",
                name = "U&Drama",
                shortName = "U&Drama",
                broadcaster = Broadcaster.U,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/drama-uk.png",
                deepLinkUris = listOf(
                    "https://uktvplay.uktv.co.uk/channel/drama",
                    "uktvplay://channel/drama"
                ),
                webLiveUrl = "https://uktvplay.uktv.co.uk/channel/drama"
            ),
            Channel(
                id = "u_yesterday",
                skySid = "2305",
                channelNumber = "155",
                name = "U&Yesterday",
                shortName = "U&Yesterday",
                broadcaster = Broadcaster.U,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/yesterday-uk.png",
                deepLinkUris = listOf(
                    "https://uktvplay.uktv.co.uk/channel/yesterday",
                    "uktvplay://channel/yesterday"
                ),
                webLiveUrl = "https://uktvplay.uktv.co.uk/channel/yesterday"
            ),
            Channel(
                id = "u_w",
                skySid = "2617",
                channelNumber = "132",
                name = "U&W",
                shortName = "U&W",
                broadcaster = Broadcaster.U,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/u-and-w-uk.png",
                deepLinkUris = listOf(
                    "https://uktvplay.uktv.co.uk/channel/w",
                    "uktvplay://channel/w"
                ),
                webLiveUrl = "https://uktvplay.uktv.co.uk/channel/w"
            ),
            Channel(
                id = "u_alibi",
                skySid = "3825",
                channelNumber = "109",
                name = "U&Alibi HD",
                shortName = "U&Alibi",
                broadcaster = Broadcaster.U,
                logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/united-kingdom/alibi-uk.png",
                deepLinkUris = listOf(
                    "https://uktvplay.uktv.co.uk/channel/alibi",
                    "uktvplay://channel/alibi"
                ),
                webLiveUrl = "https://uktvplay.uktv.co.uk/channel/alibi"
            )
        )
    }
}
