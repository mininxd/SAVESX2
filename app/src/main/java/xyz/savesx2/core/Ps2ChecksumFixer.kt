package xyz.savesx2.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.Adler32
import java.util.zip.CRC32

/**
 * Supported checksum and hashing algorithms for PS2 savegame verification and repair.
 */
enum class ChecksumAlgorithm(val displayName: String, val byteSize: Int) {
    SUM32_LE("Sum32 (Little Endian)", 4),
    SUM32_BE("Sum32 (Big Endian)", 4),
    SUM16_LE("Sum16 (Little Endian)", 2),
    SUM16_BE("Sum16 (Big Endian)", 2),
    CRC32("CRC-32 (IEEE 802.3)", 4),
    CRC16_CCITT("CRC-16-CCITT", 2),
    XOR32("XOR-32", 4),
    ADLER32("Adler-32", 4)
}

/**
 * Inspection and verification result for an auto-detected PS2 game save checksum.
 */
data class DetectedGameProfile(
    val gameName: String,
    val fileName: String,
    val algorithm: ChecksumAlgorithm,
    val checksumOffset: Int,
    val checksumLength: Int,
    val rangeStart: Int,
    val rangeLength: Int,
    val storedChecksum: Long,
    val expectedChecksum: Long,
    val isValid: Boolean,
    val description: String
)

/**
 * Comprehensive hash and checksum report for an arbitrary save file or sector.
 */
data class FileChecksumAnalysis(
    val crc32Hex: String,
    val sum32LeHex: String,
    val adler32Hex: String,
    val md5Hex: String,
    val sha1Hex: String,
    val detectedProfile: DetectedGameProfile?
)

/**
 * High-performance PS2 savegame checksum validator, repair engine, and hash calculator.
 * Supports auto-detection and repair for Grand Theft Auto series (GTA III, VC, SA, LCS, VCS)
 * and generic PS2 save checksum patching (CRC32, Sum32, Sum16, Adler32, MD5, SHA1).
 */
object Ps2ChecksumFixer {

    // --- Checksum Computations ---

    fun calculateCrc32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0L
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value
    }

    fun calculateAdler32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0L
        val adler = Adler32()
        adler.update(data, offset, length)
        return adler.value
    }

    fun calculateSum32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0L
        var sum = 0L
        val end = offset + length
        for (i in offset until end) {
            sum = (sum + (data[i].toInt() and 0xFF)) and 0xFFFFFFFFL
        }
        return sum
    }

    fun calculateSum16(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0
        var sum = 0
        val end = offset + length
        for (i in offset until end) {
            sum = (sum + (data[i].toInt() and 0xFF)) and 0xFFFF
        }
        return sum
    }

    fun calculateCrc16Ccitt(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0
        var crc = 0xFFFF
        val end = offset + length
        for (i in offset until end) {
            val b = data[i].toInt() and 0xFF
            for (j in 0 until 8) {
                val bit = ((b shr (7 - j)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = (crc shl 1) and 0xFFFF
                if (c15 xor bit) {
                    crc = crc xor 0x1021
                }
            }
        }
        return crc and 0xFFFF
    }

    fun calculateXor32(data: ByteArray, offset: Int = 0, length: Int = data.size): Long {
        if (offset < 0 || length <= 0 || offset + length > data.size) return 0L
        var result = 0L
        val end = offset + length
        var i = offset
        while (i + 4 <= end) {
            val chunk = readUInt32LE(data, i)
            result = result xor chunk
            i += 4
        }
        if (i < end) {
            var tail = 0L
            var shift = 0
            while (i < end) {
                tail = tail or ((data[i].toLong() and 0xFF) shl shift)
                shift += 8
                i++
            }
            result = result xor tail
        }
        return result and 0xFFFFFFFFL
    }

    fun calculateMd5(data: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(data)
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            "N/A"
        }
    }

    fun calculateSha1(data: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(data)
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Throwable) {
            "N/A"
        }
    }

    // --- Byte Reading / Writing Helpers ---

    fun readUInt32LE(data: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + 4 > data.size) return 0L
        return ((data[offset].toLong() and 0xFF)) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    fun readUInt32BE(data: ByteArray, offset: Int): Long {
        if (offset < 0 || offset + 4 > data.size) return 0L
        return ((data[offset].toLong() and 0xFF) shl 24) or
                ((data[offset + 1].toLong() and 0xFF) shl 16) or
                ((data[offset + 2].toLong() and 0xFF) shl 8) or
                ((data[offset + 3].toLong() and 0xFF))
    }

    fun readUInt16LE(data: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 2 > data.size) return 0
        return ((data[offset].toInt() and 0xFF)) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun readUInt16BE(data: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 2 > data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or
                ((data[offset + 1].toInt() and 0xFF))
    }

    fun writeUInt32LE(data: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + 4 > data.size) return
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    fun writeUInt32BE(data: ByteArray, offset: Int, value: Long) {
        if (offset < 0 || offset + 4 > data.size) return
        data[offset] = ((value shr 24) and 0xFF).toByte()
        data[offset + 1] = ((value shr 16) and 0xFF).toByte()
        data[offset + 2] = ((value shr 8) and 0xFF).toByte()
        data[offset + 3] = (value and 0xFF).toByte()
    }

    fun writeUInt16LE(data: ByteArray, offset: Int, value: Int) {
        if (offset < 0 || offset + 2 > data.size) return
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    fun writeUInt16BE(data: ByteArray, offset: Int, value: Int) {
        if (offset < 0 || offset + 2 > data.size) return
        data[offset] = ((value shr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    // --- Game Profile Auto-Detection ---

    /**
     * Inspects a save directory name, file name, and file payload to detect known PS2 save structures.
     */
    fun detectGameProfile(dirName: String, fileName: String, data: ByteArray): DetectedGameProfile? {
        val lowerFile = fileName.lowercase(Locale.US)
        val upperDir = dirName.uppercase(Locale.US)

        // 1. GTA San Andreas
        if (lowerFile.matches(Regex("""gtasasf\d+\.b""")) ||
            (upperDir.contains("GTA") && lowerFile.endsWith(".b") && data.size in 200000..210000) ||
            upperDir.contains("BASLUS-20946") || upperDir.contains("BESLES-52541") || upperDir.contains("SLPM-65719")
        ) {
            if (data.size >= 4 && lowerFile.endsWith(".b")) {
                val offset = data.size - 4
                val stored = readUInt32LE(data, offset)
                val expected = calculateSum32(data, 0, offset)
                return DetectedGameProfile(
                    gameName = "Grand Theft Auto: San Andreas",
                    fileName = fileName,
                    algorithm = ChecksumAlgorithm.SUM32_LE,
                    checksumOffset = offset,
                    checksumLength = 4,
                    rangeStart = 0,
                    rangeLength = offset,
                    storedChecksum = stored,
                    expectedChecksum = expected,
                    isValid = (stored == expected),
                    description = "32-bit little-endian sum of preceding payload at end-of-file (offset 0x${offset.toString(16).uppercase()})."
                )
            }
        }

        // 2. GTA Vice City
        if (lowerFile.matches(Regex("""gtavcsf\d+\.b""")) ||
            upperDir.contains("BASLUS-20552") || upperDir.contains("BESLES-51061") || upperDir.contains("SLPM-65495")
        ) {
            if (data.size >= 4 && lowerFile.endsWith(".b")) {
                val offset = data.size - 4
                val stored = readUInt32LE(data, offset)
                val expected = calculateSum32(data, 0, offset)
                return DetectedGameProfile(
                    gameName = "Grand Theft Auto: Vice City",
                    fileName = fileName,
                    algorithm = ChecksumAlgorithm.SUM32_LE,
                    checksumOffset = offset,
                    checksumLength = 4,
                    rangeStart = 0,
                    rangeLength = offset,
                    storedChecksum = stored,
                    expectedChecksum = expected,
                    isValid = (stored == expected),
                    description = "32-bit little-endian additive sum at end-of-file (offset 0x${offset.toString(16).uppercase()})."
                )
            }
        }

        // 3. GTA III
        if (lowerFile.matches(Regex("""gta3sf\d+\.b""")) ||
            upperDir.contains("BASLUS-20062") || upperDir.contains("BESLES-50330") || upperDir.contains("SLPM-65494")
        ) {
            if (data.size >= 4 && lowerFile.endsWith(".b")) {
                val offset = data.size - 4
                val stored = readUInt32LE(data, offset)
                val expected = calculateSum32(data, 0, offset)
                return DetectedGameProfile(
                    gameName = "Grand Theft Auto III",
                    fileName = fileName,
                    algorithm = ChecksumAlgorithm.SUM32_LE,
                    checksumOffset = offset,
                    checksumLength = 4,
                    rangeStart = 0,
                    rangeLength = offset,
                    storedChecksum = stored,
                    expectedChecksum = expected,
                    isValid = (stored == expected),
                    description = "32-bit little-endian additive sum at end-of-file (offset 0x${offset.toString(16).uppercase()})."
                )
            }
        }

        // 4. GTA Liberty City Stories
        if (lowerFile.matches(Regex("""gtalcssf\d+\.b"""))) {
            if (data.size >= 4) {
                val offset = data.size - 4
                val stored = readUInt32LE(data, offset)
                val expected = calculateSum32(data, 0, offset)
                return DetectedGameProfile(
                    gameName = "Grand Theft Auto: Liberty City Stories",
                    fileName = fileName,
                    algorithm = ChecksumAlgorithm.SUM32_LE,
                    checksumOffset = offset,
                    checksumLength = 4,
                    rangeStart = 0,
                    rangeLength = offset,
                    storedChecksum = stored,
                    expectedChecksum = expected,
                    isValid = (stored == expected),
                    description = "32-bit little-endian additive sum at end-of-file."
                )
            }
        }

        // 5. GTA Vice City Stories
        if (lowerFile.matches(Regex("""gtavcssf\d+\.b"""))) {
            if (data.size >= 4) {
                val offset = data.size - 4
                val stored = readUInt32LE(data, offset)
                val expected = calculateSum32(data, 0, offset)
                return DetectedGameProfile(
                    gameName = "Grand Theft Auto: Vice City Stories",
                    fileName = fileName,
                    algorithm = ChecksumAlgorithm.SUM32_LE,
                    checksumOffset = offset,
                    checksumLength = 4,
                    rangeStart = 0,
                    rangeLength = offset,
                    storedChecksum = stored,
                    expectedChecksum = expected,
                    isValid = (stored == expected),
                    description = "32-bit little-endian additive sum at end-of-file."
                )
            }
        }

        return null
    }

    /**
     * Performs a full cryptographic and algorithmic hash analysis of a file payload.
     */
    fun analyzeFile(dirName: String, fileName: String, data: ByteArray): FileChecksumAnalysis {
        val crc = "%08X".format(calculateCrc32(data))
        val sum32 = "%08X".format(calculateSum32(data))
        val adler = "%08X".format(calculateAdler32(data))
        val md5 = calculateMd5(data)
        val sha1 = calculateSha1(data)
        val profile = detectGameProfile(dirName, fileName, data)

        return FileChecksumAnalysis(
            crc32Hex = crc,
            sum32LeHex = sum32,
            adler32Hex = adler,
            md5Hex = md5,
            sha1Hex = sha1,
            detectedProfile = profile
        )
    }

    /**
     * Repairs an auto-detected game profile checksum on the data buffer and returns the modified array.
     */
    fun fixDetectedProfile(data: ByteArray, profile: DetectedGameProfile): ByteArray {
        val copy = data.copyOf()
        val calculated = calculateChecksum(copy, profile.rangeStart, profile.rangeLength, profile.algorithm)
        writeChecksumValue(copy, profile.checksumOffset, calculated, profile.algorithm)
        return copy
    }

    /**
     * Calculates a checksum over [rangeStart] until [rangeStart + rangeLength] with [algorithm].
     */
    fun calculateChecksum(data: ByteArray, rangeStart: Int, rangeLength: Int, algorithm: ChecksumAlgorithm): Long {
        return when (algorithm) {
            ChecksumAlgorithm.SUM32_LE, ChecksumAlgorithm.SUM32_BE -> calculateSum32(data, rangeStart, rangeLength)
            ChecksumAlgorithm.SUM16_LE, ChecksumAlgorithm.SUM16_BE -> calculateSum16(data, rangeStart, rangeLength).toLong()
            ChecksumAlgorithm.CRC32 -> calculateCrc32(data, rangeStart, rangeLength)
            ChecksumAlgorithm.CRC16_CCITT -> calculateCrc16Ccitt(data, rangeStart, rangeLength).toLong()
            ChecksumAlgorithm.XOR32 -> calculateXor32(data, rangeStart, rangeLength)
            ChecksumAlgorithm.ADLER32 -> calculateAdler32(data, rangeStart, rangeLength)
        }
    }

    /**
     * Writes [value] into [data] at [targetOffset] according to [algorithm].
     */
    fun writeChecksumValue(data: ByteArray, targetOffset: Int, value: Long, algorithm: ChecksumAlgorithm) {
        when (algorithm) {
            ChecksumAlgorithm.SUM32_LE -> writeUInt32LE(data, targetOffset, value)
            ChecksumAlgorithm.SUM32_BE -> writeUInt32BE(data, targetOffset, value)
            ChecksumAlgorithm.SUM16_LE -> writeUInt16LE(data, targetOffset, value.toInt())
            ChecksumAlgorithm.SUM16_BE -> writeUInt16BE(data, targetOffset, value.toInt())
            ChecksumAlgorithm.CRC32 -> writeUInt32LE(data, targetOffset, value)
            ChecksumAlgorithm.CRC16_CCITT -> writeUInt16LE(data, targetOffset, value.toInt())
            ChecksumAlgorithm.XOR32 -> writeUInt32LE(data, targetOffset, value)
            ChecksumAlgorithm.ADLER32 -> writeUInt32BE(data, targetOffset, value)
        }
    }

    /**
     * Calculates and writes a checksum to a target offset.
     */
    fun patchChecksum(
        data: ByteArray,
        targetOffset: Int,
        rangeStart: Int,
        rangeLength: Int,
        algorithm: ChecksumAlgorithm
    ): ByteArray {
        val copy = data.copyOf()
        val calculated = calculateChecksum(copy, rangeStart, rangeLength, algorithm)
        writeChecksumValue(copy, targetOffset, calculated, algorithm)
        return copy
    }
}
