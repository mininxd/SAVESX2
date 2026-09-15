package xyz.savesx2.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handler for exporting and importing PlayStation 2 game saves in ZIP format (.zip).
 * Supports standard SavesX2 zip exports, PCSX2 folder saves, and zip archives containing
 * raw PS2 save directories or savegame files (.psu, .max, .cbs, .xps).
 */
object ZipSaveHandler {

    data class ZipItem(
        val fullPath: String,
        val data: ByteArray,
        val timeMs: Long
    )

    fun isZip(data: ByteArray): Boolean {
        if (data.size < 4) return false
        if (data[0] != 0x50.toByte() || data[1] != 0x4B.toByte()) return false
        val b2 = data[2]
        val b3 = data[3]
        return (b2 == 0x03.toByte() && b3 == 0x04.toByte()) ||
               (b2 == 0x05.toByte() && b3 == 0x06.toByte()) ||
               (b2 == 0x07.toByte() && b3 == 0x08.toByte())
    }

    /**
     * Checks if 4-byte header matches Sony PS2 icon.sys magic "PS2D".
     */
    fun isPs2dHeader(data: ByteArray): Boolean {
        if (data.size < 4) return false
        return data[0] == 'P'.code.toByte() &&
               data[1] == 'S'.code.toByte() &&
               data[2] == '2'.code.toByte() &&
               data[3] == 'D'.code.toByte()
    }

    /**
     * Extracts a PS2 save directory ID from a folder or file name candidate.
     * E.g. "[BASLUS-21447] Persona 4" -> "BASLUS-21447", "BISLPM-66675" -> "BISLPM-66675"
     */
    fun extractPs2SaveName(candidate: String): String? {
        val trimmed = candidate.trim()
        if (trimmed.isBlank()) return null

        // 1. Bracketed ID: [BASLUS-21447], [SLUS-20002], etc.
        val bracketMatch = Regex("""\[([A-Za-z0-9_-]{4,32})\]""").find(trimmed)
        if (bracketMatch != null) {
            val id = bracketMatch.groupValues[1].trim()
            if (id.length in 4..32 && !id.contains(' ')) return id
        }

        // 2. Standard PS2 save code pattern (e.g. BASLUS-21447, BISLPM-66675, BESLES-50000, BADATA-SYSTEM)
        val codeMatch = Regex("""\b(B[A-Za-z0-9_]{3,7}-\d{3,6}[A-Za-z0-9_]*)\b""", RegexOption.IGNORE_CASE).find(trimmed)
        if (codeMatch != null) {
            return codeMatch.groupValues[1]
        }

        // 3. Generic PS2 game code pattern (e.g. SLUS-20002, SLES-50000, SCES-50000, SLPS-25000)
        val genericMatch = Regex("""\b([A-Za-z]{4}-\d{5}[A-Za-z0-9_]*)\b""", RegexOption.IGNORE_CASE).find(trimmed)
        if (genericMatch != null) {
            return genericMatch.groupValues[1]
        }

        return null
    }

    /**
     * Sanitizes a string to be a valid PS2 directory/file name (ASCII, no spaces, max 31 chars).
     */
    fun sanitizeSaveName(name: String): String {
        val clean = name.trim().filter { c ->
            (c in 'A'..'Z') || (c in 'a'..'z') || (c in '0'..'9') || c == '_' || c == '-' || c == '.'
        }
        return clean.take(31).ifBlank { "SAVE" }
    }

    /**
     * Inspects the contents of a ZIP byte array to check if it contains a valid PS2 savegame.
     * Checks inside the zip for:
     * 1. icon.sys starting with "PS2D" magic
     * 2. Valid save archives (.psu, .max, .cbs, .xps)
     * 3. Valid _pcsx2_index with save files
     */
    fun isZipSave(data: ByteArray, fileName: String? = null): Boolean {
        if (!isZip(data)) return false

        try {
            ZipInputStream(ByteArrayInputStream(data)).use { zis ->
                var entry = zis.nextEntry
                var hasValidIconSys = false
                var hasArchiveSave = false
                var hasValidIndex = false
                var otherFilesCount = 0

                while (entry != null) {
                    val rawPath = entry.name.replace('\\', '/').trimStart('/')
                    val lower = rawPath.lowercase()
                    val isJunk = lower.startsWith("__macosx/") ||
                                 lower.contains("/__macosx/") ||
                                 lower.endsWith("/.ds_store") ||
                                 lower == ".ds_store" ||
                                 lower.endsWith("/thumbs.db") ||
                                 lower == "thumbs.db" ||
                                 lower.split('/').any { it.startsWith("._") }

                    if (!entry.isDirectory && !isJunk && rawPath.isNotBlank()) {
                        val baseName = rawPath.substringAfterLast('/')
                        val baseLower = baseName.lowercase()

                        when {
                            baseLower == "icon.sys" -> {
                                val header = ByteArray(4)
                                val read = zis.read(header)
                                if (read == 4 && isPs2dHeader(header)) {
                                    hasValidIconSys = true
                                }
                            }
                            baseLower.endsWith(".psu") ||
                            baseLower.endsWith(".max") ||
                            baseLower.endsWith(".cbs") ||
                            baseLower.endsWith(".xps") -> {
                                val entryBytes = zis.readBytes()
                                if (isArchiveSaveData(entryBytes, baseLower)) {
                                    hasArchiveSave = true
                                }
                            }
                            baseLower == FolderMemcardHandler.INDEX_FILENAME -> {
                                val entryBytes = zis.readBytes()
                                val parsed = FolderMemcardHandler.parseIndexContent(String(entryBytes, Charsets.UTF_8))
                                if (parsed.fileCreated.isNotEmpty() || parsed.fileOrder.isNotEmpty()) {
                                    hasValidIndex = true
                                }
                            }
                            else -> {
                                otherFilesCount++
                            }
                        }
                    }
                    zis.closeEntry()
                    if (hasValidIconSys || hasArchiveSave) {
                        return true
                    }
                    entry = zis.nextEntry
                }

                return hasValidIconSys || hasArchiveSave || (hasValidIndex && otherFilesCount > 0)
            }
        } catch (_: Throwable) {
            return false
        }
    }

    private fun isArchiveSaveData(data: ByteArray, fileNameLower: String): Boolean {
        return when {
            fileNameLower.endsWith(".max") -> MaxHandler.isMax(data)
            fileNameLower.endsWith(".cbs") -> CbsHandler.isCbs(data)
            fileNameLower.endsWith(".xps") -> XpsHandler.isXps(data)
            fileNameLower.endsWith(".psu") -> Ps2FileDetector.isPsuData(data)
            else -> MaxHandler.isMax(data) || CbsHandler.isCbs(data) || XpsHandler.isXps(data) || Ps2FileDetector.isPsuData(data)
        }
    }

    /**
     * Unpacks all valid PS2 saves contained inside the ZIP archive.
     */
    fun unpackZip(data: ByteArray, defaultSaveName: String? = null): List<PsuHandler.UnpackedPsu> {
        val items = mutableListOf<ZipItem>()
        try {
            ZipInputStream(ByteArrayInputStream(data)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val rawPath = entry.name.replace('\\', '/').trimStart('/')
                    val lower = rawPath.lowercase()
                    val isJunk = lower.startsWith("__macosx/") ||
                                 lower.contains("/__macosx/") ||
                                 lower.endsWith("/.ds_store") ||
                                 lower == ".ds_store" ||
                                 lower.endsWith("/thumbs.db") ||
                                 lower == "thumbs.db" ||
                                 lower.split('/').any { it.startsWith("._") }

                    if (!entry.isDirectory && !isJunk && rawPath.isNotBlank()) {
                        val bytes = zis.readBytes()
                        items.add(ZipItem(rawPath, bytes, entry.time))
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (_: Throwable) {
            return emptyList()
        }

        if (items.isEmpty()) return emptyList()

        // 1. Check for standalone archive files (.psu, .max, .cbs, .xps) inside the zip
        val archiveItems = items.filter { item ->
            val l = item.fullPath.lowercase()
            l.endsWith(".psu") || l.endsWith(".max") || l.endsWith(".cbs") || l.endsWith(".xps")
        }
        if (archiveItems.isNotEmpty()) {
            val archives = mutableListOf<PsuHandler.UnpackedPsu>()
            for (item in archiveItems) {
                val l = item.fullPath.lowercase()
                val unpacked = when {
                    l.endsWith(".max") || MaxHandler.isMax(item.data) -> MaxHandler.unpackMax(item.data)
                    l.endsWith(".cbs") || CbsHandler.isCbs(item.data) -> CbsHandler.unpackCbs(item.data)
                    l.endsWith(".xps") || XpsHandler.isXps(item.data) -> XpsHandler.unpackXps(item.data)
                    else -> PsuHandler.unpackPsu(item.data)
                }
                if (unpacked != null) {
                    archives.add(unpacked)
                }
            }
            if (archives.isNotEmpty()) return archives
        }

        // 2. Check for save folders with valid icon.sys starting with PS2D magic
        val validIconSysItems = items.filter { item ->
            val baseName = item.fullPath.substringAfterLast('/')
            baseName.equals("icon.sys", ignoreCase = true) && isPs2dHeader(item.data)
        }

        val resultList = mutableListOf<PsuHandler.UnpackedPsu>()

        if (validIconSysItems.isNotEmpty()) {
            val processedPrefixes = mutableSetOf<String>()
            for (iconItem in validIconSysItems) {
                val dirPrefix = iconItem.fullPath.substringBeforeLast('/', "")
                if (!processedPrefixes.add(dirPrefix)) continue

                val saveFiles = if (dirPrefix.isEmpty()) {
                    items.filter { !it.fullPath.contains('/') }
                } else {
                    items.filter { it.fullPath.startsWith("$dirPrefix/") }
                }

                val rawFolder = if (dirPrefix.isNotEmpty()) dirPrefix.substringAfterLast('/') else ""
                var saveName = extractPs2SaveName(rawFolder)
                if (saveName == null) {
                    for (f in saveFiles) {
                        val name = f.fullPath.substringAfterLast('/')
                        val extracted = extractPs2SaveName(name)
                        if (extracted != null) {
                            saveName = extracted
                            break
                        }
                    }
                }
                if (saveName == null && !defaultSaveName.isNullOrBlank()) {
                    saveName = extractPs2SaveName(defaultSaveName)
                }
                if (saveName == null && rawFolder.isNotBlank()) {
                    saveName = sanitizeSaveName(rawFolder)
                }
                if (saveName == null && !defaultSaveName.isNullOrBlank()) {
                    saveName = sanitizeSaveName(defaultSaveName.substringBeforeLast('.'))
                }
                val finalName = saveName ?: "SAVE"

                val unpacked = createUnpackedPsuFromFiles(finalName, saveFiles)
                if (unpacked != null) {
                    resultList.add(unpacked)
                }
            }
        } else {
            // 3. Check for _pcsx2_index
            val indexItems = items.filter {
                it.fullPath.substringAfterLast('/').equals(FolderMemcardHandler.INDEX_FILENAME, ignoreCase = true)
            }
            if (indexItems.isNotEmpty()) {
                val processedPrefixes = mutableSetOf<String>()
                for (indexItem in indexItems) {
                    val dirPrefix = indexItem.fullPath.substringBeforeLast('/', "")
                    if (!processedPrefixes.add(dirPrefix)) continue

                    val saveFiles = if (dirPrefix.isEmpty()) {
                        items.filter { !it.fullPath.contains('/') }
                    } else {
                        items.filter { it.fullPath.startsWith("$dirPrefix/") }
                    }

                    val rawFolder = if (dirPrefix.isNotEmpty()) dirPrefix.substringAfterLast('/') else ""
                    val saveName = extractPs2SaveName(rawFolder)
                        ?: (if (!defaultSaveName.isNullOrBlank()) extractPs2SaveName(defaultSaveName) else null)
                        ?: (if (rawFolder.isNotBlank()) sanitizeSaveName(rawFolder) else null)
                        ?: (if (!defaultSaveName.isNullOrBlank()) sanitizeSaveName(defaultSaveName.substringBeforeLast('.')) else null)
                        ?: "SAVE"

                    val unpacked = createUnpackedPsuFromFiles(saveName, saveFiles)
                    if (unpacked != null) {
                        resultList.add(unpacked)
                    }
                }
            }
        }

        return resultList
    }

    private fun createUnpackedPsuFromFiles(
        saveName: String,
        files: List<ZipItem>
    ): PsuHandler.UnpackedPsu? {
        val indexItem = files.firstOrNull {
            it.fullPath.substringAfterLast('/').equals(FolderMemcardHandler.INDEX_FILENAME, ignoreCase = true)
        }
        val indexData = indexItem?.let {
            FolderMemcardHandler.parseIndexContent(String(it.data, Charsets.UTF_8))
        }

        val filteredFiles = files.filter {
            val base = it.fullPath.substringAfterLast('/')
            !base.equals(FolderMemcardHandler.INDEX_FILENAME, ignoreCase = true) &&
            !base.equals(FolderMemcardHandler.SUPERBLOCK_FILENAME, ignoreCase = true) &&
            !base.startsWith("_pcsx2_deleted_") &&
            !base.startsWith(".")
        }
        if (filteredFiles.isEmpty()) return null

        val sortedFiles = if (indexData != null && indexData.fileOrder.isNotEmpty()) {
            filteredFiles.sortedWith { f1, f2 ->
                val n1 = f1.fullPath.substringAfterLast('/')
                val n2 = f2.fullPath.substringAfterLast('/')
                val o1 = indexData.fileOrder[n1] ?: Int.MAX_VALUE
                val o2 = indexData.fileOrder[n2] ?: Int.MAX_VALUE
                if (o1 != o2) o1.compareTo(o2) else n1.compareTo(n2)
            }
        } else {
            filteredFiles.sortedWith { f1, f2 ->
                val n1 = f1.fullPath.substringAfterLast('/')
                val n2 = f2.fullPath.substringAfterLast('/')
                when {
                    n1.equals("icon.sys", ignoreCase = true) -> -1
                    n2.equals("icon.sys", ignoreCase = true) -> 1
                    else -> n1.compareTo(n2)
                }
            }
        }

        val psuEntries = mutableListOf<PsuHandler.PsuEntry>()
        for (item in sortedFiles) {
            val fName = item.fullPath.substringAfterLast('/').take(31)
            if (fName.isBlank() || fName == "." || fName == "..") continue

            val fCreated = indexData?.fileCreated?.get(fName)
                ?: (if (item.timeMs > 0) Ps2Timestamp.fromEpochSeconds(item.timeMs / 1000L) else Ps2Timestamp.now())
            val fModified = indexData?.fileModified?.get(fName)
                ?: (if (item.timeMs > 0) Ps2Timestamp.fromEpochSeconds(item.timeMs / 1000L) else Ps2Timestamp.now())

            psuEntries.add(
                PsuHandler.PsuEntry(
                    dirEntry = Ps2DirectoryEntry(
                        mode = Ps2DirectoryEntry.DF_FILE or Ps2DirectoryEntry.DF_EXISTS or
                                Ps2DirectoryEntry.DF_RWX or Ps2DirectoryEntry.DF_0400,
                        length = item.data.size.toLong(),
                        created = fCreated,
                        cluster = 0,
                        dirEntry = 0,
                        modified = fModified,
                        attr = 0,
                        name = fName
                    ),
                    data = item.data
                )
            )
        }

        if (psuEntries.isEmpty()) return null

        val rootCreated = indexData?.rootCreated
            ?: psuEntries.firstOrNull()?.dirEntry?.created
            ?: Ps2Timestamp.now()
        val rootModified = indexData?.rootModified
            ?: psuEntries.firstOrNull()?.dirEntry?.modified
            ?: Ps2Timestamp.now()

        val dirEntry = Ps2DirectoryEntry(
            mode = Ps2DirectoryEntry.DF_DIRECTORY or Ps2DirectoryEntry.DF_EXISTS or
                    Ps2DirectoryEntry.DF_RWX or Ps2DirectoryEntry.DF_0400,
            length = (psuEntries.size + 2).toLong(),
            created = rootCreated,
            cluster = 0,
            dirEntry = 0,
            modified = rootModified,
            attr = 0,
            name = saveName
        )

        return PsuHandler.UnpackedPsu(dirEntry, psuEntries)
    }

    /**
     * Exports a save directory and all its files from a [Ps2Memcard] as a ZIP archive byte array.
     */
    fun exportZip(memcard: Ps2Memcard, saveName: String): ByteArray? {
        val saves = memcard.listSaves()
        val save = saves.firstOrNull { it.directoryName == saveName } ?: return null

        val bos = ByteArrayOutputStream()
        val zos = ZipOutputStream(bos)

        for (f in save.files) {
            val data = f.data ?: memcard.getSaveFileBytes(saveName, f.name) ?: continue
            val entry = ZipEntry("${save.directoryName}/${f.name}")
            val time = f.dirEntry.modified.toEpochSeconds() * 1000L
            if (time > 0) {
                entry.time = time
            }
            zos.putNextEntry(entry)
            zos.write(data)
            zos.closeEntry()
        }
        zos.close()
        return bos.toByteArray()
    }
}
