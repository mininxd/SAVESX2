package xyz.savesx2.core

import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * High-accuracy PlayStation 1 (PS1 / PSX) Memory Card Parser, Slot Manager, and Container Bridge.
 *
 * Fully supports:
 * - Standard 128 KB raw memory cards (.mcr, .mcd, .srm, .mc, raw)
 * - PSP / PS Vita virtual memory card images (.vmp, 128-byte header wrapper)
 * - DexDrive save files (.gme, 3,904-byte header wrapper)
 * - Single savegame container formats (.mcs, .psx)
 * - Inter-card export / import with PlayStation 2 memory cards (.ps2)
 *
 * Standard PS1 Card Architecture:
 * - Total size: Exactly 131,072 bytes (128 KB) = 16 frames of 8,192 bytes (8 KB).
 * - Frame 0 (Bytes 0..8,191): Directory Frame (64 blocks of 128 bytes).
 *   - Block 0: Header magic "MC" (0x4D, 0x43), XOR checksum at byte 127 = 0x0E.
 *   - Blocks 1..15: Directory entries for save slots 1..15.
 *     - Byte 0: Block allocation state:
 *       - 0x51: First block of active save
 *       - 0x52: Middle block of multi-block save
 *       - 0x53: Last block of multi-block save
 *       - 0xA0: Free / unallocated block
 *       - 0xA1..0xA3: Deleted blocks
 *     - Bytes 4..7: File size in bytes (32-bit LE uint, typically 8192 * numBlocks)
 *     - Bytes 8..9: Next frame pointer (16-bit LE uint, 0xFFFF for EOF)
 *     - Bytes 10..73: Save identifier / disc ID (ASCII string, e.g. "BASLUS-00592-000")
 *     - Byte 127: XOR checksum of bytes 0..126
 *   - Blocks 16..19: Broken sector replacement list
 *   - Blocks 20..63: Unused directory space
 * - Frames 1..15 (Bytes 8,192 * i..): 8 KB save slot frames.
 *   - Frame starts with Save Header: Magic "SC" (0x53, 0x43), icon type at 0x02,
 *     Shift-JIS title at 0x04..0x43, 16-color CLUT at 0x60..0x7F, 16x16 icon at 0x80..0xFF.
 */
class Ps1Memcard private constructor(
    private var cardData: ByteArray,
    val headerOffset: Int = 0,
    val isVmp: Boolean = false,
    val isGme: Boolean = false,
    val originalHeaderBytes: ByteArray? = null
) {
    companion object {
        const val RAW_CARD_SIZE = 131072 // 128 KB = 16 frames * 8192 bytes
        const val FRAME_SIZE = 8192
        const val BLOCK_SIZE = 128
        const val TOTAL_SLOTS = 15
        const val VMP_HEADER_SIZE = 128
        const val VMP_TOTAL_SIZE = 131200
        const val GME_HEADER_SIZE = 3904
        const val GME_TOTAL_SIZE = 134976

        // Block Allocation States
        const val STATUS_IN_USE_FIRST: Byte = 0x51.toByte()
        const val STATUS_IN_USE_MIDDLE: Byte = 0x52.toByte()
        const val STATUS_IN_USE_LAST: Byte = 0x53.toByte()
        const val STATUS_FREE: Byte = 0xA0.toByte()
        const val STATUS_DELETED_FIRST: Byte = 0xA1.toByte()
        const val STATUS_DELETED_MIDDLE: Byte = 0xA2.toByte()
        const val STATUS_DELETED_LAST: Byte = 0xA3.toByte()

        /**
         * Validates whether a byte array represents a PlayStation 1 memory card image.
         */
        fun isPs1Memcard(data: ByteArray, fileName: String? = null): Boolean {
            if (data.isEmpty()) return false
            val nameLower = fileName?.lowercase(Locale.US) ?: ""

            // 1. PSP / PS Vita .vmp (131,200 bytes, magic "VMP\0")
            if (data.size == VMP_TOTAL_SIZE &&
                data[0] == 0x56.toByte() && data[1] == 0x4D.toByte() && data[2] == 0x50.toByte() && data[3] == 0x00.toByte()
            ) {
                return true
            }

            // 2. DexDrive .gme (134,976 bytes, magic "123-456-STD")
            if (data.size >= GME_TOTAL_SIZE &&
                data[0] == 0x31.toByte() && data[1] == 0x32.toByte() && data[2] == 0x33.toByte()
            ) {
                return true
            }

            // 3. Standard raw 128 KB image (.mcr, .mcd, .srm, .mc, raw)
            if (data.size == RAW_CARD_SIZE) {
                // Formatted with "MC" header magic
                if (data[0] == 0x4D.toByte() && data[1] == 0x43.toByte()) {
                    return true
                }
                // Unformatted card with PS1 filename extension
                if (nameLower.endsWith(".mcr") || nameLower.endsWith(".srm") ||
                    nameLower.endsWith(".mcd") || nameLower.endsWith(".mc")
                ) {
                    return true
                }
                // Check if directory frame has at least one valid slot status or checksum
                var validSlots = 0
                for (slot in 1..TOTAL_SLOTS) {
                    val status = data[slot * BLOCK_SIZE]
                    if (status == STATUS_IN_USE_FIRST || status == STATUS_FREE ||
                        status in STATUS_DELETED_FIRST..STATUS_DELETED_LAST
                    ) {
                        validSlots++
                    }
                }
                if (validSlots >= 8) return true
            }

            // 4. File extension hints with matching ~128KB size
            if ((nameLower.endsWith(".mcr") || nameLower.endsWith(".srm") || nameLower.endsWith(".vmp")) &&
                data.size in RAW_CARD_SIZE..(RAW_CARD_SIZE + 4096)
            ) {
                return true
            }

            return false
        }

        /**
         * Validates whether a byte array is a PlayStation 1 single save container (.mcs).
         */
        fun isMcsSave(data: ByteArray, fileName: String? = null): Boolean {
            val nameLower = fileName?.lowercase(Locale.US) ?: ""
            if (data.size < BLOCK_SIZE + FRAME_SIZE) return false
            if (nameLower.endsWith(".mcs")) return true

            // Size is 128 + N * 8192
            val payloadSize = data.size - BLOCK_SIZE
            if (payloadSize > 0 && payloadSize % FRAME_SIZE == 0) {
                if (data[0] == STATUS_IN_USE_FIRST) {
                    // Check if payload starts with "SC"
                    if (data[BLOCK_SIZE] == 0x53.toByte() && data[BLOCK_SIZE + 1] == 0x43.toByte()) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Validates whether a byte array is a raw single PS1 savegame (.psx).
         */
        fun isPsxSave(data: ByteArray, fileName: String? = null): Boolean {
            val nameLower = fileName?.lowercase(Locale.US) ?: ""
            if (data.size < 256) return false
            if (data[0] == 0x53.toByte() && data[1] == 0x43.toByte()) {
                if (nameLower.endsWith(".psx") || nameLower.endsWith(".raw") || (data.size % FRAME_SIZE == 0)) {
                    return true
                }
            }
            return false
        }

        /**
         * Opens and parses a PlayStation 1 memory card from raw bytes.
         */
        fun open(data: ByteArray): Ps1Memcard? {
            if (!isPs1Memcard(data)) return null

            // PSP / Vita .vmp
            if (data.size == VMP_TOTAL_SIZE &&
                data[0] == 0x56.toByte() && data[1] == 0x4D.toByte() && data[2] == 0x50.toByte()
            ) {
                val header = data.copyOfRange(0, VMP_HEADER_SIZE)
                val raw = data.copyOfRange(VMP_HEADER_SIZE, VMP_HEADER_SIZE + RAW_CARD_SIZE)
                return Ps1Memcard(raw, headerOffset = VMP_HEADER_SIZE, isVmp = true, originalHeaderBytes = header)
            }

            // DexDrive .gme
            if (data.size >= GME_TOTAL_SIZE &&
                data[0] == 0x31.toByte() && data[1] == 0x32.toByte() && data[2] == 0x33.toByte()
            ) {
                val header = data.copyOfRange(0, GME_HEADER_SIZE)
                val raw = data.copyOfRange(GME_HEADER_SIZE, GME_HEADER_SIZE + RAW_CARD_SIZE)
                return Ps1Memcard(raw, headerOffset = GME_HEADER_SIZE, isGme = true, originalHeaderBytes = header)
            }

            // Standard raw 128 KB
            if (data.size == RAW_CARD_SIZE) {
                return Ps1Memcard(data.copyOf())
            }

            // Over-sized raw image with padding
            if (data.size > RAW_CARD_SIZE && data[0] == 0x4D.toByte() && data[1] == 0x43.toByte()) {
                return Ps1Memcard(data.copyOfRange(0, RAW_CARD_SIZE))
            }

            return null
        }

        /**
         * Creates a fresh, fully formatted 128 KB PlayStation 1 memory card.
         */
        fun createFormattedCard(): ByteArray {
            val data = ByteArray(RAW_CARD_SIZE)

            // Frame 0 Block 0: MC header
            data[0] = 0x4D.toByte() // 'M'
            data[1] = 0x43.toByte() // 'C'
            data[127] = 0x0E.toByte() // Checksum of 'M' and 'C'

            // Blocks 1..15: Directory entries (all 0xA0 free)
            for (i in 1..TOTAL_SLOTS) {
                val offset = i * BLOCK_SIZE
                data[offset] = STATUS_FREE
                data[offset + 8] = 0xFF.toByte()
                data[offset + 9] = 0xFF.toByte()
                data[offset + 127] = calculateBlockChecksum(data, offset)
            }

            // Blocks 16..19: Broken sector markers (0xFF)
            for (b in 16..19) {
                val offset = b * BLOCK_SIZE
                for (j in 0 until 127) {
                    data[offset + j] = 0xFF.toByte()
                }
                data[offset + 127] = calculateBlockChecksum(data, offset)
            }

            return data
        }

        /**
         * Computes the standard XOR checksum of bytes 0..126 within a 128-byte block.
         */
        fun calculateBlockChecksum(data: ByteArray, offset: Int): Byte {
            var xor = 0
            for (i in 0 until 127) {
                xor = xor xor (data[offset + i].toInt() and 0xFF)
            }
            return xor.toByte()
        }

        private fun readNullTerminatedAscii(bytes: ByteArray): String {
            val nullIdx = bytes.indexOf(0.toByte())
            val len = if (nullIdx >= 0) nullIdx else bytes.size
            return String(bytes, 0, len, Charsets.US_ASCII).trim()
        }
    }

    /**
     * Checks if this memory card is formatted.
     */
    fun isFormatted(): Boolean {
        if (cardData.size < BLOCK_SIZE) return false
        return cardData[0] == 0x4D.toByte() && cardData[1] == 0x43.toByte()
    }

    /**
     * Returns the full memory card byte array including any original format wrapper (.vmp, .gme).
     */
    fun getRawDataDirect(): ByteArray {
        val orig = originalHeaderBytes
        return if (orig != null && orig.isNotEmpty()) {
            orig + cardData
        } else {
            cardData
        }
    }

    /**
     * Lists all active PlayStation 1 saves stored across the 15 card slots.
     */
    fun listSaves(): List<Ps2Save> {
        val saves = mutableListOf<Ps2Save>()
        val visitedSlots = BooleanArray(TOTAL_SLOTS + 1)

        for (slot in 1..TOTAL_SLOTS) {
            if (visitedSlots[slot]) continue
            val offset = slot * BLOCK_SIZE
            val status = cardData[offset]

            if (status == STATUS_IN_USE_FIRST) {
                visitedSlots[slot] = true

                // 32-bit little endian file size
                val rawSize = (cardData[offset + 4].toLong() and 0xFF) or
                        ((cardData[offset + 5].toLong() and 0xFF) shl 8) or
                        ((cardData[offset + 6].toLong() and 0xFF) shl 16) or
                        ((cardData[offset + 7].toLong() and 0xFF) shl 24)

                val nextFrame = (cardData[offset + 8].toInt() and 0xFF) or
                        ((cardData[offset + 9].toInt() and 0xFF) shl 8)

                // Save identifier / disc ID (ASCII string)
                val idBytes = cardData.copyOfRange(offset + 10, offset + 74)
                val idString = readNullTerminatedAscii(idBytes).ifBlank { "PS1_SLOT_$slot" }

                // Follow next frame chain for multi-frame saves
                val frameIndices = mutableListOf<Int>()
                frameIndices.add(slot)
                var currNext = nextFrame
                var loopGuard = 0
                while (currNext in 1..TOTAL_SLOTS && !visitedSlots[currNext] && currNext != 0xFFFF && loopGuard++ < TOTAL_SLOTS) {
                    visitedSlots[currNext] = true
                    frameIndices.add(currNext)
                    val nextOffset = currNext * BLOCK_SIZE
                    currNext = (cardData[nextOffset + 8].toInt() and 0xFF) or
                            ((cardData[nextOffset + 9].toInt() and 0xFF) shl 8)
                }

                val frameCount = frameIndices.size
                val totalPayload = ByteArray(frameCount * FRAME_SIZE)
                for (idx in frameIndices.indices) {
                    val f = frameIndices[idx]
                    val srcOffset = f * FRAME_SIZE
                    if (srcOffset + FRAME_SIZE <= cardData.size) {
                        System.arraycopy(cardData, srcOffset, totalPayload, idx * FRAME_SIZE, FRAME_SIZE)
                    }
                }

                // Decode PS1 Header ("SC")
                var shiftJisTitle: String? = null
                var isPocketStation = false
                if (totalPayload.size >= 68 && totalPayload[0] == 0x53.toByte() && totalPayload[1] == 0x43.toByte()) {
                    shiftJisTitle = Ps2IconDecoder.extractPs1Title(totalPayload)
                    val iconType = totalPayload[2].toInt() and 0xFF
                    isPocketStation = (iconType in 0x11..0x13)
                }

                val cacheKey = "ps1_card:$idString:${totalPayload.size}"
                val iconBitmap = if (totalPayload.size >= 256 && totalPayload[0] == 0x53.toByte() && totalPayload[1] == 0x43.toByte()) {
                    Ps2IconDecoder.decodePs1Icon(totalPayload, cacheKey)
                } else null

                // Look up canonical game metadata
                val gameInfo = Ps2SerialDatabase.lookupGame(idString)
                val canonicalTitle = gameInfo?.title
                val region = gameInfo?.region ?: Ps2Region.detect(idString, shiftJisTitle ?: "")

                val finalTitle = when {
                    canonicalTitle != null && (shiftJisTitle.isNullOrBlank() || shiftJisTitle.equals(idString, true) || Ps2SerialDatabase.isRawSerialTitle(shiftJisTitle)) -> canonicalTitle
                    !shiftJisTitle.isNullOrBlank() -> shiftJisTitle
                    canonicalTitle != null -> canonicalTitle
                    else -> idString
                }

                val finalSubtitle = when {
                    canonicalTitle != null && !shiftJisTitle.isNullOrBlank() && !shiftJisTitle.equals(canonicalTitle, true) && !shiftJisTitle.equals(idString, true) -> shiftJisTitle
                    frameCount > 1 -> "PS1 Save ($frameCount Blocks)"
                    else -> "PS1 Save (Slot $slot)"
                }

                val actualSize = if (rawSize in 1..(frameCount * FRAME_SIZE.toLong())) rawSize else (frameCount * FRAME_SIZE.toLong())

                val dirEntry = Ps2DirectoryEntry(
                    mode = Ps2DirectoryEntry.DF_FILE or Ps2DirectoryEntry.DF_EXISTS or Ps2DirectoryEntry.DF_RWX or Ps2DirectoryEntry.DF_PSX or Ps2DirectoryEntry.DF_0400,
                    length = actualSize,
                    created = Ps2Timestamp(),
                    cluster = slot.toLong(),
                    dirEntry = 0L,
                    modified = Ps2Timestamp(),
                    attr = 0,
                    name = idString
                )

                val files = listOf(
                    Ps2SaveFile(
                        name = idString,
                        sizeInBytes = actualSize,
                        modifiedDate = "Slot $slot",
                        dirEntry = dirEntry,
                        data = totalPayload
                    )
                )

                saves.add(
                    Ps2Save(
                        directoryName = idString,
                        title = finalTitle,
                        subtitle = finalSubtitle,
                        sizeInBytes = actualSize,
                        createdDate = "Slot $slot",
                        modifiedDate = if (frameCount > 1) "$frameCount Blocks" else "1 Block",
                        isProtected = false,
                        isHidden = false,
                        isPocketStation = isPocketStation,
                        isPsx = true,
                        dirEntry = dirEntry,
                        files = files,
                        iconSys = null,
                        iconBitmap = iconBitmap,
                        iconImageBitmap = iconBitmap?.asImageBitmap(),
                        gameInfo = gameInfo,
                        region = region
                    )
                )
            }
        }
        return saves
    }

    /**
     * Deletes a PlayStation 1 savegame by clearing its directory blocks and zeroing payload frames.
     */
    fun deleteSave(saveName: String): Boolean {
        val targetSlot = findSaveSlot(saveName) ?: return false

        // Trace all frames in the chain
        val slotsToDelete = mutableListOf<Int>()
        var curr = targetSlot
        var loopGuard = 0
        while (curr in 1..TOTAL_SLOTS && loopGuard++ < TOTAL_SLOTS) {
            slotsToDelete.add(curr)
            val off = curr * BLOCK_SIZE
            val next = (cardData[off + 8].toInt() and 0xFF) or ((cardData[off + 9].toInt() and 0xFF) shl 8)
            if (next in 1..TOTAL_SLOTS && next != 0xFFFF) {
                curr = next
            } else {
                break
            }
        }

        // Mark blocks as free and zero frame data
        for (slot in slotsToDelete) {
            val off = slot * BLOCK_SIZE
            cardData[off] = STATUS_FREE
            cardData[off + 4] = 0
            cardData[off + 5] = 0
            cardData[off + 6] = 0
            cardData[off + 7] = 0
            cardData[off + 8] = 0xFF.toByte()
            cardData[off + 9] = 0xFF.toByte()
            for (j in 10 until 74) cardData[off + j] = 0
            cardData[off + 127] = calculateBlockChecksum(cardData, off)

            // Zero out frame payload
            val frameOffset = slot * FRAME_SIZE
            if (frameOffset + FRAME_SIZE <= cardData.size) {
                cardData.fill(0, frameOffset, frameOffset + FRAME_SIZE)
            }
        }

        return true
    }

    /**
     * Imports a PlayStation 1 savegame (.mcs, .psx, or raw frames) onto this memory card.
     */
    fun importSave(saveData: ByteArray, defaultName: String? = null): Boolean {
        if (saveData.isEmpty()) return false

        val isMcs = isMcsSave(saveData)
        val payload: ByteArray
        val saveName: String

        if (isMcs) {
            val dirBlock = saveData.copyOfRange(0, BLOCK_SIZE)
            payload = saveData.copyOfRange(BLOCK_SIZE, saveData.size)
            val rawName = readNullTerminatedAscii(dirBlock.copyOfRange(10, 74))
            saveName = rawName.ifBlank { defaultName ?: "PS1_IMPORT" }
        } else {
            payload = saveData
            saveName = defaultName?.ifBlank { "PS1_IMPORT" } ?: "PS1_IMPORT"
        }

        val neededFrames = maxOf(1, (payload.size + FRAME_SIZE - 1) / FRAME_SIZE)

        // Delete existing save with matching name if present
        deleteSave(saveName)

        // Find available free slots
        val freeSlots = mutableListOf<Int>()
        for (slot in 1..TOTAL_SLOTS) {
            val status = cardData[slot * BLOCK_SIZE]
            if (status == STATUS_FREE || status in STATUS_DELETED_FIRST..STATUS_DELETED_LAST) {
                freeSlots.add(slot)
            }
        }

        if (freeSlots.size < neededFrames) {
            return false // Insufficient free blocks
        }

        val allocatedSlots = freeSlots.take(neededFrames)

        // Write payload frames and directory entries
        for (idx in allocatedSlots.indices) {
            val slot = allocatedSlots[idx]
            val chunkOffset = idx * FRAME_SIZE
            val chunkLen = minOf(FRAME_SIZE, payload.size - chunkOffset)

            // Write payload chunk to frame
            val frameOffset = slot * FRAME_SIZE
            cardData.fill(0, frameOffset, frameOffset + FRAME_SIZE)
            if (chunkLen > 0) {
                System.arraycopy(payload, chunkOffset, cardData, frameOffset, chunkLen)
            }

            // Write directory block
            val off = slot * BLOCK_SIZE
            val status = when {
                idx == 0 -> STATUS_IN_USE_FIRST
                idx == allocatedSlots.lastIndex -> STATUS_IN_USE_LAST
                else -> STATUS_IN_USE_MIDDLE
            }
            cardData[off] = status

            // 32-bit file size
            val size = payload.size
            cardData[off + 4] = (size and 0xFF).toByte()
            cardData[off + 5] = ((size shr 8) and 0xFF).toByte()
            cardData[off + 6] = ((size shr 16) and 0xFF).toByte()
            cardData[off + 7] = ((size shr 24) and 0xFF).toByte()

            // Next frame pointer
            val nextSlot = if (idx < allocatedSlots.lastIndex) allocatedSlots[idx + 1] else 0xFFFF
            cardData[off + 8] = (nextSlot and 0xFF).toByte()
            cardData[off + 9] = ((nextSlot shr 8) and 0xFF).toByte()

            // Save identifier
            val nameBytes = saveName.toByteArray(Charsets.US_ASCII)
            for (j in 10 until 74) cardData[off + j] = 0
            System.arraycopy(nameBytes, 0, cardData, off + 10, minOf(nameBytes.size, 64))

            // Checksum
            cardData[off + 127] = calculateBlockChecksum(cardData, off)
        }

        return true
    }

    /**
     * Exports a savegame as a standard PlayStation 1 single save file (.mcs).
     */
    fun exportSaveAsMcs(saveName: String): ByteArray? {
        val slot = findSaveSlot(saveName) ?: return null
        val dirBlock = cardData.copyOfRange(slot * BLOCK_SIZE, (slot + 1) * BLOCK_SIZE)
        val payload = getSaveBytes(saveName) ?: return null
        return dirBlock + payload
    }

    /**
     * Exports a savegame as raw save frames (.psx).
     */
    fun exportSaveAsPsx(saveName: String): ByteArray? {
        return getSaveBytes(saveName)
    }

    /**
     * Exports a savegame as a ZIP archive containing both .mcs and .psx files.
     */
    fun exportSaveAsZip(saveName: String): ByteArray? {
        val mcsBytes = exportSaveAsMcs(saveName) ?: return null
        val psxBytes = exportSaveAsPsx(saveName) ?: return null

        return try {
            val bos = ByteArrayOutputStream()
            ZipOutputStream(bos).use { zos ->
                val mcsEntry = ZipEntry("$saveName.mcs")
                zos.putNextEntry(mcsEntry)
                zos.write(mcsBytes)
                zos.closeEntry()

                val psxEntry = ZipEntry("$saveName.psx")
                zos.putNextEntry(psxEntry)
                zos.write(psxBytes)
                zos.closeEntry()
            }
            bos.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Retrieves the concatenated payload bytes for a save across all its allocated frames.
     */
    fun getSaveBytes(saveName: String): ByteArray? {
        val startSlot = findSaveSlot(saveName) ?: return null
        val slots = mutableListOf<Int>()
        var curr = startSlot
        var loopGuard = 0
        while (curr in 1..TOTAL_SLOTS && loopGuard++ < TOTAL_SLOTS) {
            slots.add(curr)
            val off = curr * BLOCK_SIZE
            val next = (cardData[off + 8].toInt() and 0xFF) or ((cardData[off + 9].toInt() and 0xFF) shl 8)
            if (next in 1..TOTAL_SLOTS && next != 0xFFFF) {
                curr = next
            } else {
                break
            }
        }

        val totalBytes = ByteArray(slots.size * FRAME_SIZE)
        for (i in slots.indices) {
            val s = slots[i]
            System.arraycopy(cardData, s * FRAME_SIZE, totalBytes, i * FRAME_SIZE, FRAME_SIZE)
        }
        return totalBytes
    }

    /**
     * Updates the payload data for an existing save file (used by hex editor).
     */
    fun updateSaveFile(saveName: String, newData: ByteArray): Boolean {
        val startSlot = findSaveSlot(saveName) ?: return false
        val slots = mutableListOf<Int>()
        var curr = startSlot
        var loopGuard = 0
        while (curr in 1..TOTAL_SLOTS && loopGuard++ < TOTAL_SLOTS) {
            slots.add(curr)
            val off = curr * BLOCK_SIZE
            val next = (cardData[off + 8].toInt() and 0xFF) or ((cardData[off + 9].toInt() and 0xFF) shl 8)
            if (next in 1..TOTAL_SLOTS && next != 0xFFFF) {
                curr = next
            } else {
                break
            }
        }

        var offset = 0
        for (slot in slots) {
            val chunkLen = minOf(FRAME_SIZE, newData.size - offset)
            if (chunkLen > 0) {
                System.arraycopy(newData, offset, cardData, slot * FRAME_SIZE, chunkLen)
                offset += chunkLen
            }
        }
        return true
    }

    /**
     * Formats the memory card to a standard blank PS1 layout.
     */
    fun format(): Boolean {
        cardData = createFormattedCard()
        return true
    }

    /**
     * Computes statistics for this PS1 memory card.
     */
    fun getStats(): CardStats {
        var usedSlots = 0
        for (slot in 1..TOTAL_SLOTS) {
            val status = cardData[slot * BLOCK_SIZE]
            if (status == STATUS_IN_USE_FIRST || status == STATUS_IN_USE_MIDDLE || status == STATUS_IN_USE_LAST) {
                usedSlots++
            }
        }
        val freeSlots = TOTAL_SLOTS - usedSlots
        val usedSpaceBytes = usedSlots * FRAME_SIZE.toLong()
        val freeSpaceBytes = freeSlots * FRAME_SIZE.toLong()

        return CardStats(
            totalClusters = 16L,
            allocatableClusters = TOTAL_SLOTS.toLong(),
            allocatedClusters = usedSlots.toLong(),
            freeClusters = freeSlots.toLong(),
            totalSpaceBytes = RAW_CARD_SIZE.toLong(),
            usedSpaceBytes = usedSpaceBytes,
            freeSpaceBytes = freeSpaceBytes,
            badBlocksCount = 0,
            hasEcc = false,
            pageSize = BLOCK_SIZE,
            clusterSize = FRAME_SIZE,
            isFormatted = isFormatted()
        )
    }

    private fun findSaveSlot(saveName: String): Int? {
        for (slot in 1..TOTAL_SLOTS) {
            val off = slot * BLOCK_SIZE
            val status = cardData[off]
            if (status == STATUS_IN_USE_FIRST) {
                val name = readNullTerminatedAscii(cardData.copyOfRange(off + 10, off + 74))
                if (name == saveName || saveName == "PS1_SLOT_$slot") {
                    return slot
                }
            }
        }
        return null
    }
}
