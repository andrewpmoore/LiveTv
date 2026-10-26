package com.freeepg.tv.tuner

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/** HDHomeRun's UDP discovery framing, independent of Android and hostname resolution. */
internal object HdHomeRunDiscoveryProtocol {
    const val PORT = 65001

    fun request(): ByteArray {
        // Discover request: tuner device type (1), wildcard device ID (0xffffffff).
        val frame = byteArrayOf(
            0, 2, 0, 12,
            1, 4, 0, 0, 0, 1,
            2, 4, -1, -1, -1, -1
        )
        return ByteBuffer.allocate(frame.size + 4).order(ByteOrder.LITTLE_ENDIAN)
            .put(frame)
            .putInt(CRC32().apply { update(frame) }.value.toInt())
            .array()
    }

    fun isTunerResponse(data: ByteArray, length: Int): Boolean {
        if (length < 8 || length > data.size) return false
        val packet = ByteBuffer.wrap(data, 0, length).order(ByteOrder.BIG_ENDIAN)
        if (packet.short.toInt() != 3) return false
        val payloadLength = packet.short.toInt() and 0xffff
        if (payloadLength + 8 != length) return false
        val payloadEnd = length - 4
        val expectedCrc = ByteBuffer.wrap(data, payloadEnd, 4)
            .order(ByteOrder.LITTLE_ENDIAN).int
        if (CRC32().apply { update(data, 0, payloadEnd) }.value.toInt() != expectedCrc) {
            return false
        }

        var tunerFound = false
        while (packet.position() < payloadEnd) {
            if (payloadEnd - packet.position() < 2) return false
            val tag = packet.get().toInt() and 0xff
            var size = packet.get().toInt() and 0xff
            if (size and 0x80 != 0) {
                if (packet.position() >= payloadEnd) return false
                size = (size and 0x7f) or ((packet.get().toInt() and 0xff) shl 7)
            }
            if (size > payloadEnd - packet.position()) return false
            val next = packet.position() + size
            if (tag == 1 && size == 4 && packet.int == 1) tunerFound = true
            packet.position(next)
        }
        return tunerFound
    }
}
