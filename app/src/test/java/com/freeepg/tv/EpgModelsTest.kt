package com.freeepg.tv

import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.data.model.SkyHawkEvent
import com.freeepg.tv.data.model.SkyHawkScheduleRoot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgModelsTest {

    @Test
    fun testAllBroadcastersCovered() {
        val broadcasters = Channel.ALL_CHANNELS.map { it.broadcaster }.distinct()
        assertTrue(broadcasters.contains(Broadcaster.BBC))
        assertTrue(broadcasters.contains(Broadcaster.ITV))
        assertTrue(broadcasters.contains(Broadcaster.CHANNEL_4))
        assertTrue(broadcasters.contains(Broadcaster.CHANNEL_5))
        assertEquals(4, broadcasters.size)
    }

    @Test
    fun testChannelCounts() {
        val bbcChannels = Channel.ALL_CHANNELS.filter { it.broadcaster == Broadcaster.BBC }
        val itvChannels = Channel.ALL_CHANNELS.filter { it.broadcaster == Broadcaster.ITV }
        val c4Channels = Channel.ALL_CHANNELS.filter { it.broadcaster == Broadcaster.CHANNEL_4 }
        val c5Channels = Channel.ALL_CHANNELS.filter { it.broadcaster == Broadcaster.CHANNEL_5 }

        assertTrue("Expected at least 5 BBC channels", bbcChannels.size >= 5)
        assertTrue("Expected at least 4 ITV channels", itvChannels.size >= 4)
        assertTrue("Expected at least 4 Channel 4 channels", c4Channels.size >= 4)
        assertTrue("Expected at least 4 Channel 5 channels", c5Channels.size >= 4)
    }

    @Test
    fun testProgrammeLiveCalculation() {
        val nowSec = 1730000000L
        val prog = Programme(
            id = "test_1",
            channelId = "bbc_one",
            title = "Strictly Come Dancing",
            synopsis = "Live ballroom dancing.",
            startTimeEpochSeconds = nowSec - 1800, // started 30 min ago
            durationSeconds = 3600 // 1 hour long
        )

        assertTrue(prog.isLiveNow(nowSec))
        assertFalse(prog.isUpcoming(nowSec))
        assertEquals(0.5f, prog.progress(nowSec), 0.01f)
        assertEquals(60L, prog.durationMinutes)
    }

    @Test
    fun testSkyHawkScheduleParsing() {
        val jsonStr = """
            {
                "date": "20261024",
                "schedule": [
                    {
                        "sid": "3000",
                        "events": [
                            {
                                "eid": "E123",
                                "st": 1792791600,
                                "d": 3000,
                                "t": "The Graham Norton Show",
                                "sy": "Celebrity interviews.",
                                "seasonnumber": 34,
                                "episodenumber": 5,
                                "hd": true,
                                "new": false,
                                "s": true,
                                "ad": false
                            }
                        ]
                    }
                ]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val root = json.decodeFromString<SkyHawkScheduleRoot>(jsonStr)

        assertEquals("20261024", root.date)
        assertEquals(1, root.schedule.size)
        val channelSched = root.schedule[0]
        assertEquals("3000", channelSched.sid)
        assertEquals(1, channelSched.events.size)
        val event = channelSched.events[0]
        assertEquals("The Graham Norton Show", event.title)
        assertEquals(34, event.seasonNumber)
        assertEquals(5, event.episodeNumber)
        assertTrue(event.hd)
        assertTrue(event.subtitles)
    }
}
