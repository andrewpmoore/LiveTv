package com.freeepg.tv

import com.freeepg.tv.data.model.Broadcaster
import com.freeepg.tv.data.model.Channel
import com.freeepg.tv.data.model.Programme
import com.freeepg.tv.data.model.SkyHawkEvent
import com.freeepg.tv.data.model.SkyHawkScheduleRoot
import com.freeepg.tv.data.repository.ChannelSortOrder
import com.freeepg.tv.data.repository.SettingsRepository
import com.freeepg.tv.tuner.HdHomeRunLineupItem
import com.freeepg.tv.tuner.HdHomeRunManager
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun testBroadcasterTunerEnum() {
        val tuner = Broadcaster.TUNER
        assertEquals("tuner", tuner.id)
        assertEquals("TV Tuner", tuner.displayName)
        assertEquals("Tuner", tuner.shortName)
        assertTrue(Broadcaster.values().contains(Broadcaster.TUNER))
    }

    @Test
    fun testHdHomeRunLineupOverlapMapping() {
        val testLineup = listOf(
            HdHomeRunLineupItem("1", "BBC One", "http://192.168.1.50:5004/auto/v1", hd = 0),
            HdHomeRunLineupItem("2", "BBC Two", "http://192.168.1.50:5004/auto/v2", hd = 0),
            HdHomeRunLineupItem("3", "ITV1", "http://192.168.1.50:5004/auto/v3", hd = 0),
            HdHomeRunLineupItem("4", "Channel 4", "http://192.168.1.50:5004/auto/v4", hd = 0),
            HdHomeRunLineupItem("5", "Channel 5", "http://192.168.1.50:5004/auto/v5", hd = 0),
            HdHomeRunLineupItem("101", "BBC One HD", "http://192.168.1.50:5004/auto/v101", hd = 1),
            HdHomeRunLineupItem("102", "BBC Two HD", "http://192.168.1.50:5004/auto/v102", hd = 1),
            HdHomeRunLineupItem("103", "ITV1 HD", "http://192.168.1.50:5004/auto/v103", hd = 1)
        )

        val mappedChannels = HdHomeRunManager.mapLineupToChannels(testLineup, "192.168.1.50")
        assertEquals(testLineup.size, mappedChannels.size)

        for (ch in mappedChannels) {
            assertTrue("Expected channel ${ch.name} (${ch.channelNumber}) to be marked as overlapping", ch.isOverlappingWithApp)
            assertNotNull("Expected overlappingAppChannelId to be set for ${ch.name}", ch.overlappingAppChannelId)
            assertEquals(Broadcaster.TUNER, ch.broadcaster)
            assertTrue("Expected isTunerChannel to be true", ch.isTunerChannel)
            assertNotNull("Expected streamUrl to be set", ch.streamUrl)
        }

        val bbcOne = mappedChannels.first { it.channelNumber == "1" }
        assertEquals("bbc_one", bbcOne.overlappingAppChannelId)

        val itv1 = mappedChannels.first { it.channelNumber == "3" }
        assertEquals("itv1", itv1.overlappingAppChannelId)

        val bbcOneHd = mappedChannels.first { it.channelNumber == "101" }
        assertEquals("bbc_one", bbcOneHd.overlappingAppChannelId)
    }

    @Test
    fun testHdHomeRunTunerExclusiveChannels() {
        val testLineup = listOf(
            HdHomeRunLineupItem("11", "Sky Mix", "http://192.168.1.50:5004/auto/v11", hd = 0),
            HdHomeRunLineupItem("19", "U&Dave", "http://192.168.1.50:5004/auto/v19", hd = 0),
            HdHomeRunLineupItem("20", "U&Drama", "http://192.168.1.50:5004/auto/v20", hd = 0),
            HdHomeRunLineupItem("82", "Talking Pictures TV", "http://192.168.1.50:5004/auto/v82", hd = 0),
            HdHomeRunLineupItem("233", "Sky News", "http://192.168.1.50:5004/auto/v233", hd = 0)
        )

        val mappedChannels = HdHomeRunManager.mapLineupToChannels(testLineup, "192.168.1.50")

        for (ch in mappedChannels) {
            assertFalse("Expected tuner-exclusive channel ${ch.name} to not overlap", ch.isOverlappingWithApp)
            assertNull("Expected overlappingAppChannelId to be null for ${ch.name}", ch.overlappingAppChannelId)
            assertTrue("Expected non-zero Sky SID for EPG data for ${ch.name}", ch.skySid != "0")
        }

        val skyMix = mappedChannels.first { it.channelNumber == "11" }
        assertEquals("1831", skyMix.skySid)

        val dave = mappedChannels.first { it.channelNumber == "19" }
        assertEquals("3809", dave.skySid)

        val tptv = mappedChannels.first { it.channelNumber == "82" }
        assertEquals("5252", tptv.skySid)
    }

    @Test
    fun testFilterChannelsTunerPriorityAndDeduplication() {
        val defaultLineup = HdHomeRunManager.getDefaultFreeviewLineup("192.168.1.50")
        val tunerChannels = HdHomeRunManager.mapLineupToChannels(defaultLineup, "192.168.1.50")

        // 1. "All Channels" view with TUNER enabled:
        // Overlapping tuner channels must be omitted in favour of the app versions
        val allChannels = SettingsRepository.filterChannels(
            allAppChannels = Channel.ALL_CHANNELS,
            selectedBroadcaster = null,
            enabledBroadcasters = Broadcaster.values().toSet(),
            sortOrder = ChannelSortOrder.NUMBER,
            tunerChannels = tunerChannels
        )

        // App versions of overlapping channels must be present
        val appBbcOne = allChannels.firstOrNull { it.id == "bbc_one" }
        assertNotNull("BBC One app channel must be present in All Channels", appBbcOne)
        assertEquals(Broadcaster.BBC, appBbcOne?.broadcaster)

        // Tuner duplicate versions (e.g. tuner_1, tuner_101) must NOT be present
        val duplicateTunerOne = allChannels.firstOrNull { it.id == "tuner_1" || it.id == "tuner_101" }
        assertNull("Overlapping tuner channels must be excluded from All Channels", duplicateTunerOne)

        // Tuner-exclusive channels (e.g. tuner_11 Sky Mix, tuner_19 Dave) must be present
        val skyMix = allChannels.firstOrNull { it.id == "tuner_11" }
        assertNotNull("Tuner-exclusive channel Sky Mix must appear in All Channels", skyMix)
        assertEquals(Broadcaster.TUNER, skyMix?.broadcaster)

        val dave = allChannels.firstOrNull { it.id == "tuner_19" }
        assertNotNull("Tuner-exclusive channel Dave must appear in All Channels", dave)

        val tptv = allChannels.firstOrNull { it.id == "tuner_82" }
        assertNotNull("Tuner-exclusive channel Talking Pictures TV must appear in All Channels", tptv)

        // 2. "TV Tuner" filter view:
        // Must show all tuner channels (including overlapping ones)
        val tunerFilterChannels = SettingsRepository.filterChannels(
            allAppChannels = Channel.ALL_CHANNELS,
            selectedBroadcaster = Broadcaster.TUNER,
            enabledBroadcasters = Broadcaster.values().toSet(),
            sortOrder = ChannelSortOrder.NUMBER,
            tunerChannels = tunerChannels
        )

        assertEquals("Tuner filter tab must show all tuner channels", tunerChannels.size, tunerFilterChannels.size)
        assertTrue(tunerFilterChannels.any { it.id == "tuner_1" })
        assertTrue(tunerFilterChannels.any { it.id == "tuner_11" })

        // 3. Broadcaster filter (e.g. BBC):
        // Must show only BBC channels
        val bbcFilterChannels = SettingsRepository.filterChannels(
            allAppChannels = Channel.ALL_CHANNELS,
            selectedBroadcaster = Broadcaster.BBC,
            enabledBroadcasters = Broadcaster.values().toSet(),
            sortOrder = ChannelSortOrder.NUMBER,
            tunerChannels = tunerChannels
        )

        assertTrue(bbcFilterChannels.all { it.broadcaster == Broadcaster.BBC })
        assertFalse(bbcFilterChannels.any { it.broadcaster == Broadcaster.TUNER })

        // 4. Tuner broadcaster disabled:
        // In "All Channels", no tuner channels must be present
        val enabledWithoutTuner = Broadcaster.values().filter { it != Broadcaster.TUNER }.toSet()
        val channelsWithoutTuner = SettingsRepository.filterChannels(
            allAppChannels = Channel.ALL_CHANNELS,
            selectedBroadcaster = null,
            enabledBroadcasters = enabledWithoutTuner,
            sortOrder = ChannelSortOrder.NUMBER,
            tunerChannels = tunerChannels
        )

        assertFalse(channelsWithoutTuner.any { it.broadcaster == Broadcaster.TUNER })
    }
}
