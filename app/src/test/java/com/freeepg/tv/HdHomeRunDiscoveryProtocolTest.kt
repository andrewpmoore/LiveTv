package com.freeepg.tv

import com.freeepg.tv.tuner.HdHomeRunDiscoveryProtocol
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HdHomeRunDiscoveryProtocolTest {
    @Test
    fun requestMatchesWildcardTunerDiscoveryPacket() {
        val expected = "0002000c0104000000010204ffffffff4e507f35"
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertArrayEquals(expected, HdHomeRunDiscoveryProtocol.request())
    }

    @Test
    fun acceptsTunerReplyAndSkipsUnknownTags() {
        val packet = response(byteArrayOf(99, 3, 10, 20, 30, 1, 4, 0, 0, 0, 1))
        assertTrue(HdHomeRunDiscoveryProtocol.isTunerResponse(packet, packet.size))
        // Datagram buffers are larger than the received packet.
        assertTrue(HdHomeRunDiscoveryProtocol.isTunerResponse(packet.copyOf(2048), packet.size))
    }

    @Test
    fun acceptsTwoByteTagLengths() {
        val payload = byteArrayOf(99, 0x80.toByte(), 1) + ByteArray(128) +
            byteArrayOf(1, 4, 0, 0, 0, 1)
        val packet = response(payload)
        assertTrue(HdHomeRunDiscoveryProtocol.isTunerResponse(packet, packet.size))
    }

    @Test
    fun rejectsStorageDevicesAndRequests() {
        val storage = response(byteArrayOf(1, 4, 0, 0, 0, 5))
        assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(storage, storage.size))
        val request = HdHomeRunDiscoveryProtocol.request()
        assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(request, request.size))
    }

    @Test
    fun rejectsCorruptAndTruncatedPackets() {
        val valid = response(byteArrayOf(1, 4, 0, 0, 0, 1))
        val corrupt = valid.copyOf().apply { this[8] = 2 }
        assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(corrupt, corrupt.size))
        for (length in 0 until valid.size) {
            assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(valid, length))
        }
        assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(valid, valid.size + 1))
    }

    @Test
    fun rejectsInvalidTagLengthsEvenWithValidCrc() {
        for (payload in listOf(
            byteArrayOf(1, 4, 0, 0),
            byteArrayOf(1, 0x80.toByte()),
            byteArrayOf(1, 4, 0, 0, 0, 1, 99)
        )) {
            val packet = response(payload)
            assertFalse(HdHomeRunDiscoveryProtocol.isTunerResponse(packet, packet.size))
        }
    }

    private fun response(payload: ByteArray): ByteArray {
        val frame = ByteBuffer.allocate(4 + payload.size).order(ByteOrder.BIG_ENDIAN)
            .putShort(3).putShort(payload.size.toShort()).put(payload).array()
        return ByteBuffer.allocate(frame.size + 4).order(ByteOrder.LITTLE_ENDIAN)
            .put(frame).putInt(CRC32().apply { update(frame) }.value.toInt()).array()
    }
}
