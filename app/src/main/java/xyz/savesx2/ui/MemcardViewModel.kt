package xyz.savesx2.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import xyz.savesx2.core.CardStats
import xyz.savesx2.core.ExportFilenameFormat
import xyz.savesx2.core.MemcardFormatter
import xyz.savesx2.core.Ps2DirectoryEntry
import xyz.savesx2.core.Ps2Icon3dSession
import xyz.savesx2.core.Ps2IconDecoder
import xyz.savesx2.core.Ps2Memcard
import xyz.savesx2.core.Ps2Save
import xyz.savesx2.core.Ps2Timestamp
import xyz.savesx2.core.PsuHandler
import xyz.savesx2.core.FolderMemcardHandler
import xyz.savesx2.core.Ps2FileDetector
import xyz.savesx2.core.Ps2FileType
import xyz.savesx2.core.Ps2SuperBlock
import xyz.savesx2.core.RecentCard
import xyz.savesx2.core.RecentCardsManager
import xyz.savesx2.core.UpdateChecker
import xyz.savesx2.core.UpdateStatus
import xyz.savesx2.core.ZipSaveHandler
import java.io.File
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface CardUiState {
    data object Empty : CardUiState
    data class Loading(val message: String = "Loading memory card...") : CardUiState
    data class Loaded(
        val cardName: String,
        val cardUri: Uri?,
        val memcard: Ps2Memcard,
        val saves: List<Ps2Save>,
        val stats: CardStats,
        val isFolderCard: Boolean = false,
        val folderPath: String? = null
    ) : CardUiState
    data class Error(val message: String) : CardUiState
}

enum class FilterType {
    ALL,
    PS2_ONLY,
    PS1_ONLY,
    PROTECTED
}

enum class SortBy {
    NAME_ASC,
    DATE_DESC,
    SIZE_DESC
}

data class HexEditorSession(
    val title: String,
    val data: ByteArray,
    val saveName: String? = null,
    val fileName: String? = null,
    val isReadOnly: Boolean = false,
    val isRawCard: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HexEditorSession) return false
        return title == other.title &&
                saveName == other.saveName &&
                fileName == other.fileName &&
                isReadOnly == other.isReadOnly &&
                isRawCard == other.isRawCard &&
                data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (saveName?.hashCode() ?: 0)
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + isReadOnly.hashCode()
        result = 31 * result + isRawCard.hashCode()
        return result
    }
}

class CardSnapshot private constructor(
    private val rawOrCompressedData: ByteArray,
    val uncompressedSize: Int,
    val isCompressed: Boolean,
    val actionDescription: String
) {
    val compressedSizeBytes: Int get() = rawOrCompressedData.size

    val data: ByteArray
        get() = if (isCompressed) decompress(rawOrCompressedData, uncompressedSize) else rawOrCompressedData.copyOf()

    companion object {
        fun create(data: ByteArray, actionDescription: String): CardSnapshot {
            if (data.isEmpty()) return CardSnapshot(ByteArray(0), 0, false, actionDescription)
            val deflater = Deflater(Deflater.BEST_SPEED)
            return try {
                deflater.setInput(data)
                deflater.finish()
                val bos = ByteArrayOutputStream(minOf(data.size, 64 * 1024))
                val buffer = ByteArray(32 * 1024)
                while (!deflater.finished()) {
                    val count = deflater.deflate(buffer)
                    bos.write(buffer, 0, count)
                }
                CardSnapshot(bos.toByteArray(), data.size, true, actionDescription)
            } catch (_: Throwable) {
                CardSnapshot(data.copyOf(), data.size, false, actionDescription)
            } finally {
                deflater.end()
            }
        }

        private fun decompress(compressed: ByteArray, uncompressedSize: Int): ByteArray {
            if (uncompressedSize == 0 || compressed.isEmpty()) return ByteArray(0)
            val inflater = Inflater()
            return try {
                inflater.setInput(compressed)
                val result = ByteArray(uncompressedSize)
                var offset = 0
                while (!inflater.finished() && offset < uncompressedSize) {
                    val count = inflater.inflate(result, offset, uncompressedSize - offset)
                    if (count == 0) break
                    offset += count
                }
                result
            } catch (_: Throwable) {
                ByteArray(uncompressedSize)
            } finally {
                inflater.end()
            }
        }
    }
}

class MemcardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CardUiState>(CardUiState.Empty)
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME_ASC)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _selectedSave = MutableStateFlow<Ps2Save?>(null)
    val selectedSave: StateFlow<Ps2Save?> = _selectedSave.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _showFormatDialog = MutableStateFlow(false)
    val showFormatDialog: StateFlow<Boolean> = _showFormatDialog.asStateFlow()

    private val _showStatsDialog = MutableStateFlow(false)
    val showStatsDialog: StateFlow<Boolean> = _showStatsDialog.asStateFlow()

    private val _showResizeDialog = MutableStateFlow(false)
    val showResizeDialog: StateFlow<Boolean> = _showResizeDialog.asStateFlow()

    private val _icon3dSession = MutableStateFlow<Ps2Icon3dSession?>(null)
    val icon3dSession: StateFlow<Ps2Icon3dSession?> = _icon3dSession.asStateFlow()

    private val _hexEditorSession = MutableStateFlow<HexEditorSession?>(null)
    val hexEditorSession: StateFlow<HexEditorSession?> = _hexEditorSession.asStateFlow()

    private val _hexViewerData = MutableStateFlow<Pair<String, ByteArray>?>(null)
    val hexViewerData: StateFlow<Pair<String, ByteArray>?> = _hexViewerData.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun getMaxUndoHistory(cardSizeBytes: Int): Int {
        val sizeMb = cardSizeBytes / (1024 * 1024)
        return when {
            sizeMb <= 8 -> 10
            sizeMb <= 16 -> 6
            sizeMb <= 32 -> 4
            sizeMb <= 64 -> 2
            else -> 1
        }
    }

    private fun pruneHistoryLocked(cardSizeBytes: Int) {
        val maxCount = getMaxUndoHistory(cardSizeBytes)
        val maxBudget = when {
            cardSizeBytes <= 8 * 1024 * 1024 -> 30 * 1024 * 1024
            cardSizeBytes <= 32 * 1024 * 1024 -> 40 * 1024 * 1024
            else -> 50 * 1024 * 1024
        }

        while (undoStack.size > maxCount) {
            undoStack.removeFirst()
        }
        while (redoStack.size > maxCount) {
            redoStack.removeFirst()
        }

        fun totalSize(): Int = undoStack.sumOf { it.compressedSizeBytes } + redoStack.sumOf { it.compressedSizeBytes }

        while (totalSize() > maxBudget && undoStack.size > 1) {
            undoStack.removeFirst()
        }
        while (totalSize() > maxBudget && redoStack.isNotEmpty()) {
            redoStack.removeFirst()
        }
    }

    private val undoStack = ArrayDeque<CardSnapshot>()
    private val redoStack = ArrayDeque<CardSnapshot>()
    private var savedCardCrc: Long? = null
    private var baselineCardData: ByteArray? = null
    private val historyLock = Any()
    private val historyMutex = Mutex()

    private val _customDirectoryUri = MutableStateFlow<Uri?>(null)
    val customDirectoryUri: StateFlow<Uri?> = _customDirectoryUri.asStateFlow()

    private val _customDirectoryName = MutableStateFlow<String?>(null)
    val customDirectoryName: StateFlow<String?> = _customDirectoryName.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _recentCards = MutableStateFlow<List<RecentCard>>(emptyList())
    val recentCards: StateFlow<List<RecentCard>> = _recentCards.asStateFlow()

    private val _exportFilenameFormat = MutableStateFlow(ExportFilenameFormat.GAME_NAME_AND_PRODUCT_ID)
    val exportFilenameFormat: StateFlow<ExportFilenameFormat> = _exportFilenameFormat.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(true)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _hasLegacyApp = MutableStateFlow(false)
    val hasLegacyApp: StateFlow<Boolean> = _hasLegacyApp.asStateFlow()
    private var legacyAppDismissed = false

    fun checkLegacyApp(context: Context) {
        val isInstalled = try {
            context.packageManager.getPackageInfo("xyz.mininxd.ps2memcards", 0)
            true
        } catch (_: Exception) {
            false
        }
        if (!isInstalled) {
            _hasLegacyApp.value = false
        } else if (!legacyAppDismissed) {
            _hasLegacyApp.value = true
        }
    }

    fun dismissLegacyAppPrompt() {
        legacyAppDismissed = true
        _hasLegacyApp.value = false
    }

    fun initSettings(context: Context) {
        _recentCards.value = RecentCardsManager.getRecentCards(context)
        _exportFilenameFormat.value = ExportFilenameFormat.getSavedFormat(context)
        checkLegacyApp(context)
    }

    fun addRecentCard(context: Context, recent: RecentCard) {
        RecentCardsManager.addRecentCard(context, recent)
        _recentCards.value = RecentCardsManager.getRecentCards(context)
    }

    fun removeRecentCard(context: Context, uriString: String) {
        RecentCardsManager.removeRecentCard(context, uriString)
        _recentCards.value = RecentCardsManager.getRecentCards(context)
    }

    fun clearRecentCards(context: Context) {
        RecentCardsManager.clearAll(context)
        _recentCards.value = emptyList()
    }

    fun setExportFilenameFormat(context: Context, format: ExportFilenameFormat) {
        _exportFilenameFormat.value = format
        ExportFilenameFormat.saveFormat(context, format)
    }

    fun setShowSettingsDialog(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun updateStoragePermission(isGranted: Boolean) {
        _hasStoragePermission.value = isGranted
    }

    fun checkUpdate(currentVersion: String) {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.Checking
            val result = UpdateChecker.checkUpdate(currentVersion)
            _updateStatus.value = result
        }
    }

    private var currentLoadedCard: CardUiState.Loaded? = null
    private var iconPreloadJob: Job? = null

    private fun setLoadedState(loaded: CardUiState.Loaded, triggerIconPreload: Boolean = true) {
        currentLoadedCard = loaded
        _uiState.value = loaded
        if (triggerIconPreload) {
            startIconPreloading(loaded.saves, loaded.memcard)
        }
    }

    private fun startIconPreloading(saves: List<Ps2Save>, card: Ps2Memcard) {
        iconPreloadJob?.cancel()
        val savesNeedingIcons = saves.filter { it.iconBitmap == null }
        if (savesNeedingIcons.isEmpty()) return

        iconPreloadJob = viewModelScope.launch(Dispatchers.Default) {
            val batch = mutableMapOf<String, Bitmap>()
            var lastFlushTime = System.currentTimeMillis()

            for (save in savesNeedingIcons) {
                if (!isActive) break
                val bmp = card.decodeSaveIcon(save)
                if (bmp != null) {
                    batch[save.directoryName] = bmp
                }
                val now = System.currentTimeMillis()
                if (batch.size >= 3 || (now - lastFlushTime >= 200 && batch.isNotEmpty())) {
                    val batchToApply = batch.toMap()
                    batch.clear()
                    lastFlushTime = now
                    withContext(Dispatchers.Main.immediate) {
                        applyIconBatchToState(batchToApply)
                    }
                }
                delay(16)
            }
            if (batch.isNotEmpty()) {
                val remaining = batch.toMap()
                withContext(Dispatchers.Main.immediate) {
                    applyIconBatchToState(remaining)
                }
            }
        }
    }

    private fun applyIconBatchToState(icons: Map<String, Bitmap>) {
        if (icons.isEmpty()) return
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return
        val updatedSaves = current.saves.map { s ->
            val bmp = icons[s.directoryName]
            if (bmp != null && s.iconBitmap == null) {
                s.copy(iconBitmap = bmp, iconImageBitmap = bmp.asImageBitmap())
            } else {
                s
            }
        }
        val updatedLoaded = current.copy(saves = updatedSaves)
        setLoadedState(updatedLoaded, triggerIconPreload = false)

        val sel = _selectedSave.value
        if (sel != null && icons.containsKey(sel.directoryName)) {
            val bmp = icons[sel.directoryName]
            if (bmp != null && sel.iconBitmap == null) {
                _selectedSave.value = sel.copy(iconBitmap = bmp, iconImageBitmap = bmp.asImageBitmap())
            }
        }
    }

    suspend fun loadSaveIcon(save: Ps2Save): Bitmap? = withContext(Dispatchers.Default) {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withContext null
        val bmp = current.memcard.decodeSaveIcon(save)
        if (bmp != null && save.iconBitmap == null) {
            withContext(Dispatchers.Main.immediate) {
                applyIconBatchToState(mapOf(save.directoryName to bmp))
            }
        }
        bmp
    }

    fun setCustomDirectory(uri: Uri?, name: String?) {
        _customDirectoryUri.value = uri
        _customDirectoryName.value = name
    }

    fun markCardSaved(savedUri: Uri? = null, savedName: String? = null, context: Context? = null) {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return
        savedCardCrc = calculateCrc(current.memcard.getRawDataDirect())
        baselineCardData = current.memcard.getRawDataDirect().copyOf()
        clearUndoRedoHistory()
        _hasUnsavedChanges.value = false
        val updated = current.copy(
            cardName = savedName ?: current.cardName,
            cardUri = savedUri ?: current.cardUri
        )
        setLoadedState(updated)
        if (context != null && savedUri != null) {
            val name = savedName ?: updated.cardName
            addRecentCard(
                context,
                RecentCard(
                    uriString = savedUri.toString(),
                    fileName = name,
                    sizeBytes = updated.memcard.getRawDataDirect().size.toLong(),
                    saveCount = updated.saves.size,
                    lastOpened = System.currentTimeMillis(),
                    isFolder = updated.isFolderCard
                )
            )
        }
    }

    private fun calculateCrc(bytes: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(bytes)
        return crc.value
    }

    private fun pushUndoSnapshot(snapshot: ByteArray, actionDescription: String) {
        val cardSnapshot = CardSnapshot.create(snapshot, actionDescription)
        synchronized(historyLock) {
            undoStack.addLast(cardSnapshot)
            redoStack.clear()
            pruneHistoryLocked(snapshot.size)
            _canUndo.value = true
            _canRedo.value = false
        }
    }

    private fun clearUndoRedoHistory() {
        synchronized(historyLock) {
            undoStack.clear()
            redoStack.clear()
            _canUndo.value = false
            _canRedo.value = false
        }
    }

    fun undo() {
        if (!_canUndo.value) return
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                withContext(Dispatchers.Default) {
                    try {
                        val currentBytes = current.memcard.getRawDataDirect().copyOf()
                        val snapshotToRestore: CardSnapshot = synchronized(historyLock) {
                            if (undoStack.isEmpty()) {
                                _canUndo.value = false
                                return@withContext
                            }
                            val snap = undoStack.removeLast()
                            val redoSnapshot = CardSnapshot.create(currentBytes, snap.actionDescription)
                            redoStack.addLast(redoSnapshot)
                            pruneHistoryLocked(currentBytes.size)
                            _canUndo.value = undoStack.isNotEmpty()
                            _canRedo.value = true
                            snap
                        }

                        val restoredData = snapshotToRestore.data
                        val card = Ps2Memcard.open(restoredData) ?: return@withContext
                        val saves = card.listSaves()
                        val stats = card.getStats()
                        val isAtSavedBaseline = savedCardCrc != null && calculateCrc(restoredData) == savedCardCrc
                        _hasUnsavedChanges.value = if (current.cardUri == null) true else !isAtSavedBaseline
                        val updated = current.copy(
                            memcard = card,
                            saves = saves,
                            stats = stats
                        )
                        setLoadedState(updated)
                        val selName = _selectedSave.value?.directoryName
                        _selectedSave.value = if (selName != null) saves.firstOrNull { it.directoryName == selName } else null
                        _snackbarMessage.value = "Undo: ${snapshotToRestore.actionDescription}"
                    } catch (t: Throwable) {
                        _snackbarMessage.value = "Undo error: ${t.message ?: "Failed to restore state"}"
                    }
                }
            }
        }
    }

    fun redo() {
        if (!_canRedo.value) return
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                withContext(Dispatchers.Default) {
                    try {
                        val currentBytes = current.memcard.getRawDataDirect().copyOf()
                        val snapshotToRestore: CardSnapshot = synchronized(historyLock) {
                            if (redoStack.isEmpty()) {
                                _canRedo.value = false
                                return@withContext
                            }
                            val snap = redoStack.removeLast()
                            val undoSnapshot = CardSnapshot.create(currentBytes, snap.actionDescription)
                            undoStack.addLast(undoSnapshot)
                            pruneHistoryLocked(currentBytes.size)
                            _canUndo.value = true
                            _canRedo.value = redoStack.isNotEmpty()
                            snap
                        }

                        val restoredData = snapshotToRestore.data
                        val card = Ps2Memcard.open(restoredData) ?: return@withContext
                        val saves = card.listSaves()
                        val stats = card.getStats()
                        val isAtSavedBaseline = savedCardCrc != null && calculateCrc(restoredData) == savedCardCrc
                        _hasUnsavedChanges.value = if (current.cardUri == null) true else !isAtSavedBaseline
                        val updated = current.copy(
                            memcard = card,
                            saves = saves,
                            stats = stats
                        )
                        setLoadedState(updated)
                        val selName = _selectedSave.value?.directoryName
                        _selectedSave.value = if (selName != null) saves.firstOrNull { it.directoryName == selName } else null
                        _snackbarMessage.value = "Redo: ${snapshotToRestore.actionDescription}"
                    } catch (t: Throwable) {
                        _snackbarMessage.value = "Redo error: ${t.message ?: "Failed to restore state"}"
                    }
                }
            }
        }
    }

    fun cancelEdit() {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                val baseline = baselineCardData ?: synchronized(historyLock) {
                    undoStack.firstOrNull()?.data
                }
                clearUndoRedoHistory()
                if (baseline != null) {
                    withContext(Dispatchers.Default) {
                        try {
                            val card = Ps2Memcard.open(baseline.copyOf()) ?: return@withContext
                            val saves = card.listSaves()
                            val stats = card.getStats()
                            val isAtSavedBaseline = savedCardCrc != null && calculateCrc(baseline) == savedCardCrc
                            _hasUnsavedChanges.value = if (current.cardUri == null) true else !isAtSavedBaseline
                            val updated = current.copy(
                                memcard = card,
                                saves = saves,
                                stats = stats
                            )
                            setLoadedState(updated)
                            val selName = _selectedSave.value?.directoryName
                            _selectedSave.value = if (selName != null) saves.firstOrNull { it.directoryName == selName } else null
                            _snackbarMessage.value = "Editing cancelled"
                        } catch (t: Throwable) {
                            _snackbarMessage.value = "Cancel edit error: ${t.message ?: "Failed"}"
                        }
                    }
                } else {
                    _hasUnsavedChanges.value = false
                    _snackbarMessage.value = "Editing cancelled"
                }
            }
        }
    }

    fun setLoading(message: String) {
        _uiState.value = CardUiState.Loading(message)
    }

    fun setError(message: String) {
        _uiState.value = CardUiState.Error(message)
    }

    fun clearLoading() {
        if (_uiState.value is CardUiState.Loading) {
            _uiState.value = currentLoadedCard ?: CardUiState.Empty
        }
    }

    fun getRawCardData(): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.getRawDataDirect()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: FilterType) {
        _filterType.value = filter
    }

    fun setSortBy(sort: SortBy) {
        _sortBy.value = sort
    }

    fun selectSave(save: Ps2Save?) {
        _selectedSave.value = save
    }

    fun setShowCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }

    fun setShowFormatDialog(show: Boolean) {
        _showFormatDialog.value = show
    }

    fun setShowStatsDialog(show: Boolean) {
        _showStatsDialog.value = show
    }

    fun setShowResizeDialog(show: Boolean) {
        val loaded = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard
        if (show && loaded?.isFolderCard == true) {
            _snackbarMessage.value = "Folder-based memory cards do not have a fixed capacity and cannot be resized."
            return
        }
        _showResizeDialog.value = show
    }

    fun open3dIconViewer(save: Ps2Save) {
        viewModelScope.launch {
            val loaded = (uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@launch
            val iconFileName = save.iconSys?.iconFile?.trim()?.ifBlank { null }
                ?: save.files.firstOrNull { it.name.endsWith(".icn", ignoreCase = true) || it.name.endsWith(".ico", ignoreCase = true) }?.name
                ?: return@launch

            val iconBytes = withContext(Dispatchers.Default) {
                loaded.memcard.getSaveFileBytes(save.directoryName, iconFileName)
            } ?: return@launch

            val mesh = withContext(Dispatchers.Default) {
                Ps2IconDecoder.parseIconMesh(iconBytes)
            } ?: run {
                _snackbarMessage.value = "Failed to parse 3D icon mesh."
                return@launch
            }

            _icon3dSession.value = Ps2Icon3dSession(
                title = save.displayTitle,
                subtitle = save.displaySubtitle,
                mesh = mesh,
                iconSys = save.iconSys
            )
        }
    }

    fun close3dIconViewer() {
        _icon3dSession.value = null
    }

    fun openHexEditor(
        title: String,
        data: ByteArray,
        saveName: String? = null,
        fileName: String? = null,
        isReadOnly: Boolean = false,
        isRawCard: Boolean = false
    ) {
        _hexViewerData.value = Pair(title, data)
        _hexEditorSession.value = HexEditorSession(
            title = title,
            data = data,
            saveName = saveName,
            fileName = fileName,
            isReadOnly = isReadOnly,
            isRawCard = isRawCard
        )
    }

    fun openHexViewer(title: String, data: ByteArray) {
        openHexEditor(title, data)
    }

    fun closeHexViewer() {
        _hexViewerData.value = null
        _hexEditorSession.value = null
    }

    fun closeHexEditor() {
        closeHexViewer()
    }

    fun saveHexEditedFile(
        saveName: String?,
        fileName: String?,
        newData: ByteArray,
        isRawCard: Boolean = false
    ) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        if (isRawCard) {
                            val newCard = Ps2Memcard.open(newData)
                            if (newCard != null) {
                                pushUndoSnapshot(snapshotBefore, "Raw hex edit on ${current.cardName}")
                                val saves = newCard.listSaves()
                                val stats = newCard.getStats()
                                _hasUnsavedChanges.value = true
                                setLoadedState(current.copy(memcard = newCard, saves = saves, stats = stats))
                                _snackbarMessage.value = "Updated raw card data"
                                _hexViewerData.value = null
                                _hexEditorSession.value = null
                            } else {
                                _snackbarMessage.value = "Failed: Invalid memory card structure"
                            }
                            return@withContext
                        }

                        if (saveName != null && fileName != null) {
                            val ok = current.memcard.updateSaveFile(saveName, fileName, newData)
                            if (ok) {
                                pushUndoSnapshot(snapshotBefore, "Edit $fileName in $saveName")
                                val saves = current.memcard.listSaves()
                                val stats = current.memcard.getStats()
                                _hasUnsavedChanges.value = true
                                val updatedSave = saves.firstOrNull { it.directoryName == saveName }
                                setLoadedState(current.copy(saves = saves, stats = stats))
                                if (_selectedSave.value?.directoryName == saveName) {
                                    _selectedSave.value = updatedSave
                                }
                                _snackbarMessage.value = "Saved changes to $fileName"
                                _hexViewerData.value = null
                                _hexEditorSession.value = null
                            } else {
                                _snackbarMessage.value = "Failed to update $fileName"
                            }
                        }
                    } catch (t: Throwable) {
                        _snackbarMessage.value = "Save file edit error: ${t.message ?: "Failed"}"
                    }
                }
            }
        }
    }

    fun setSaveProtection(saveName: String, isProtected: Boolean) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                val currentSave = current.saves.firstOrNull { it.directoryName == saveName }
                if (currentSave != null && currentSave.isProtected == isProtected) {
                    return@withLock
                }

                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val ok = current.memcard.setSaveProtection(saveName, isProtected)
                        if (ok) {
                            val actionDesc = if (isProtected) "Protect $saveName" else "Unprotect $saveName"
                            pushUndoSnapshot(snapshotBefore, actionDesc)
                            val saves = current.memcard.listSaves()
                            val stats = current.memcard.getStats()
                            _hasUnsavedChanges.value = true
                            val updatedSave = saves.firstOrNull { it.directoryName == saveName }
                            setLoadedState(current.copy(saves = saves, stats = stats))
                            if (_selectedSave.value?.directoryName == saveName) {
                                _selectedSave.value = updatedSave
                            }
                            _snackbarMessage.value = if (isProtected) {
                                "Marked $saveName as copy-protected"
                            } else {
                                "Removed copy-protection from $saveName"
                            }
                        }
                    } catch (t: Throwable) {
                        _snackbarMessage.value = "Protection update error: ${t.message ?: "Failed"}"
                    }
                }
            }
        }
    }

    fun updateSaveTimestamps(saveName: String, created: Ps2Timestamp, modified: Ps2Timestamp) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val ok = current.memcard.updateSaveTimestamps(saveName, created, modified)
                        if (ok) {
                            pushUndoSnapshot(snapshotBefore, "Edit timestamps for $saveName")
                            val saves = current.memcard.listSaves()
                            val stats = current.memcard.getStats()
                            _hasUnsavedChanges.value = true
                            val updatedSave = saves.firstOrNull { it.directoryName == saveName }
                            setLoadedState(current.copy(saves = saves, stats = stats))
                            if (_selectedSave.value?.directoryName == saveName) {
                                _selectedSave.value = updatedSave
                            }
                            _snackbarMessage.value = "Updated timestamps for $saveName"
                        }
                    } catch (t: Throwable) {
                        _snackbarMessage.value = "Timestamp update error: ${t.message ?: "Failed"}"
                    }
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun reloadCard(contentResolver: android.content.ContentResolver, uri: Uri, context: Context? = null) {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard
        val fileName = current?.cardName ?: "MemoryCard.ps2"
        loadCardFromUri(contentResolver, uri, fileName, context)
    }

    fun loadFolderCard(folder: File, context: Context? = null) {
        viewModelScope.launch {
            _uiState.value = CardUiState.Loading("Loading folder card ${folder.name}...")
            withContext(Dispatchers.IO) {
                try {
                    val detection = Ps2FileDetector.detect(folder)
                    if (detection.fileType != Ps2FileType.PS2_FOLDER_MEMCARD) {
                        _uiState.value = CardUiState.Error("Folder '${folder.name}' is not a valid PCSX2 folder memory card (missing or invalid _pcsx2_superblock).")
                        return@withContext
                    }
                    val card = FolderMemcardHandler.loadFolderMemcard(folder)
                    if (card != null) {
                        val cardName = folder.name
                        val saves = card.listSaves()
                        val stats = card.getStats()
                        savedCardCrc = calculateCrc(card.getRawDataDirect())
                        baselineCardData = card.getRawDataDirect().copyOf()
                        clearUndoRedoHistory()
                        _hasUnsavedChanges.value = false
                        val uri = Uri.fromFile(File(folder, FolderMemcardHandler.SUPERBLOCK_FILENAME))
                        val loaded = CardUiState.Loaded(
                            cardName = cardName,
                            cardUri = uri,
                            memcard = card,
                            saves = saves,
                            stats = stats,
                            isFolderCard = true,
                            folderPath = folder.absolutePath
                        )
                        setLoadedState(loaded)
                        if (context != null) {
                            addRecentCard(
                                context,
                                RecentCard(
                                    uriString = uri.toString(),
                                    fileName = cardName,
                                    sizeBytes = card.totalCapacityBytes,
                                    saveCount = saves.size,
                                    lastOpened = System.currentTimeMillis(),
                                    isFolder = true
                                )
                            )
                        }
                        _snackbarMessage.value = "Loaded folder card $cardName (${saves.size} saves)"
                    } else {
                        _uiState.value = CardUiState.Error("Failed to load PCSX2 folder memory card from ${folder.name}.")
                    }
                } catch (t: Throwable) {
                    _uiState.value = CardUiState.Error("Failed to load folder card: ${t.message ?: "Unknown error"}")
                }
            }
        }
    }

    fun loadCardFromUri(
        contentResolver: android.content.ContentResolver,
        uri: Uri,
        fileName: String,
        context: Context? = null
    ) {
        viewModelScope.launch {
            _uiState.value = CardUiState.Loading("Reading $fileName...")
            withContext(Dispatchers.IO) {
                try {
                    val detection = Ps2FileDetector.detect(contentResolver, uri, fileName, context)
                    when (detection.fileType) {
                        Ps2FileType.PS2_FOLDER_MEMCARD -> {
                            val isSuperblockName = fileName.equals(FolderMemcardHandler.SUPERBLOCK_FILENAME, ignoreCase = true) ||
                                    fileName.endsWith(FolderMemcardHandler.SUPERBLOCK_FILENAME, ignoreCase = true)
                            if (!isSuperblockName) {
                                _uiState.value = CardUiState.Error("Folder memory cards must be opened by selecting the '_pcsx2_superblock' file.")
                                return@withContext
                            }
                            val folder = detection.folderDir ?: detection.resolvedFile?.parentFile
                            if (folder != null && folder.isDirectory) {
                                val card = FolderMemcardHandler.loadFolderMemcard(folder)
                                if (card != null) {
                                    val cardName = folder.name
                                    val saves = card.listSaves()
                                    val stats = card.getStats()
                                    savedCardCrc = calculateCrc(card.getRawDataDirect())
                                    baselineCardData = card.getRawDataDirect().copyOf()
                                    clearUndoRedoHistory()
                                    _hasUnsavedChanges.value = false
                                    val loaded = CardUiState.Loaded(
                                        cardName = cardName,
                                        cardUri = uri,
                                        memcard = card,
                                        saves = saves,
                                        stats = stats,
                                        isFolderCard = true,
                                        folderPath = folder.absolutePath
                                    )
                                    setLoadedState(loaded)
                                    if (context != null) {
                                        addRecentCard(
                                            context,
                                            RecentCard(
                                                uriString = uri.toString(),
                                                fileName = cardName,
                                                sizeBytes = card.totalCapacityBytes,
                                                saveCount = saves.size,
                                                lastOpened = System.currentTimeMillis(),
                                                isFolder = true
                                            )
                                        )
                                    }
                                    _snackbarMessage.value = "Loaded folder card $cardName (${saves.size} saves)"
                                } else {
                                    _uiState.value = CardUiState.Error("Failed to load PCSX2 folder memory card from ${folder.name}.")
                                }
                            } else {
                                _uiState.value = CardUiState.Error("Could not access memory card folder for $fileName.")
                            }
                        }
                        Ps2FileType.PS2_MEMCARD_IMAGE -> {
                            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            if (bytes == null) {
                                _uiState.value = CardUiState.Error("Could not read file from storage.")
                                return@withContext
                            }
                            val card = Ps2Memcard.open(bytes)
                            if (card != null) {
                                val saves = card.listSaves()
                                val stats = card.getStats()
                                savedCardCrc = calculateCrc(bytes)
                                baselineCardData = bytes.copyOf()
                                clearUndoRedoHistory()
                                _hasUnsavedChanges.value = false
                                val loaded = CardUiState.Loaded(
                                    cardName = fileName,
                                    cardUri = uri,
                                    memcard = card,
                                    saves = saves,
                                    stats = stats,
                                    isFolderCard = false,
                                    folderPath = null
                                )
                                setLoadedState(loaded)
                                if (context != null) {
                                    addRecentCard(
                                        context,
                                        RecentCard(
                                            uriString = uri.toString(),
                                            fileName = fileName,
                                            sizeBytes = bytes.size.toLong(),
                                            saveCount = saves.size,
                                            lastOpened = System.currentTimeMillis()
                                        )
                                    )
                                }
                                _snackbarMessage.value = "Loaded $fileName (${saves.size} saves)"
                            } else {
                                _uiState.value = CardUiState.Error("Invalid PS2 Memory Card image format.")
                            }
                        }
                        Ps2FileType.SAVEGAME_PSU,
                        Ps2FileType.SAVEGAME_MAX,
                        Ps2FileType.SAVEGAME_CBS,
                        Ps2FileType.SAVEGAME_XPS,
                        Ps2FileType.SAVEGAME_ZIP -> {
                            _uiState.value = CardUiState.Error("'$fileName' is a savegame file, not a memory card. Please open or create a memory card first, then import this save.")
                        }
                        Ps2FileType.SAVEGAME_FOLDER -> {
                            _uiState.value = CardUiState.Error("'$fileName' is a savegame folder, not a memory card. Please open or create a memory card first, then import this save.")
                        }
                        Ps2FileType.INVALID -> {
                            _uiState.value = CardUiState.Error("Selected file '$fileName' is not a valid PS2 memory card.")
                        }
                    }
                } catch (t: Throwable) {
                    _uiState.value = CardUiState.Error("Failed to open card: ${t.message ?: "Out of memory"}")
                }
            }
        }
    }

    fun loadCardFromBytes(name: String, bytes: ByteArray, uri: Uri? = null) {
        viewModelScope.launch {
            _uiState.value = CardUiState.Loading("Reading $name...")
            withContext(Dispatchers.Default) {
                try {
                    val card = Ps2Memcard.open(bytes)
                    if (card != null) {
                        val saves = card.listSaves()
                        val stats = card.getStats()
                        savedCardCrc = calculateCrc(bytes)
                        baselineCardData = bytes.copyOf()
                        clearUndoRedoHistory()
                        _hasUnsavedChanges.value = false
                        val loaded = CardUiState.Loaded(
                            cardName = name,
                            cardUri = uri,
                            memcard = card,
                            saves = saves,
                            stats = stats
                        )
                        setLoadedState(loaded)
                        _snackbarMessage.value = "Loaded $name (${saves.size} saves)"
                    } else {
                        _uiState.value = CardUiState.Error("Invalid PS2 Memory Card image format.")
                    }
                } catch (t: Throwable) {
                    _uiState.value = CardUiState.Error("Failed to open card: ${t.message ?: "Out of memory"}")
                }
            }
        }
    }

    fun createNewCard(name: String, sizeInMB: Int, useEcc: Boolean, formatted: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = CardUiState.Loading("Creating $name (${sizeInMB}MB)...")
            withContext(Dispatchers.Default) {
                try {
                    val bytes = if (formatted) {
                        MemcardFormatter.format(sizeInMB, useEcc)
                    } else {
                        MemcardFormatter.createUnformatted(sizeInMB, useEcc)
                    }
                    val card = Ps2Memcard.open(bytes)
                    if (card != null) {
                        val saves = card.listSaves()
                        val stats = card.getStats()
                        savedCardCrc = null
                        baselineCardData = bytes.copyOf()
                        clearUndoRedoHistory()
                        _hasUnsavedChanges.value = true
                        val loaded = CardUiState.Loaded(
                            cardName = name,
                            cardUri = null,
                            memcard = card,
                            saves = saves,
                            stats = stats
                        )
                        setLoadedState(loaded)
                        _snackbarMessage.value = "Created $name successfully!"
                    } else {
                        _uiState.value = CardUiState.Error("Failed to initialize memory card.")
                    }
                } catch (t: Throwable) {
                    _uiState.value = CardUiState.Error("Creation error: ${t.message ?: "Out of memory"}")
                }
            }
        }
    }

    fun formatCurrentCard() {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                _uiState.value = CardUiState.Loading("Formatting card...")
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val sizeInMB = current.memcard.totalCapacityMb.toInt()
                        val bytes = MemcardFormatter.format(sizeInMB, current.memcard.hasEcc)
                        val card = Ps2Memcard.open(bytes)
                        if (card != null) {
                            pushUndoSnapshot(snapshotBefore, "Format card")
                            _hasUnsavedChanges.value = true
                            val loaded = CardUiState.Loaded(
                                cardName = current.cardName,
                                cardUri = current.cardUri,
                                memcard = card,
                                saves = emptyList(),
                                stats = card.getStats()
                            )
                            setLoadedState(loaded)
                            _snackbarMessage.value = "Memory card formatted successfully."
                        }
                    } catch (t: Throwable) {
                        _uiState.value = CardUiState.Error("Format error: ${t.message ?: "Out of memory"}")
                    }
                }
            }
        }
    }

    fun resizeCurrentCard(newSizeMb: Int) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                if (current.isFolderCard) {
                    _snackbarMessage.value = "Folder-based memory cards do not have a fixed capacity and cannot be resized."
                    return@withLock
                }
                val currentMb = (current.memcard.totalCapacityMb + 0.5).toInt()
                if (newSizeMb <= currentMb) {
                    _snackbarMessage.value = "Target size (${newSizeMb}MB) must be greater than current size (${currentMb}MB)."
                    return@withLock
                }

                _uiState.value = CardUiState.Loading("Resizing memory card to ${newSizeMb}MB...")
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val success = current.memcard.resize(newSizeMb)
                        if (success) {
                            pushUndoSnapshot(snapshotBefore, "Resize card to ${newSizeMb}MB")
                            _hasUnsavedChanges.value = true
                            val saves = current.memcard.listSaves(forceRefresh = true)
                            val stats = current.memcard.getStats()
                            val loaded = CardUiState.Loaded(
                                cardName = current.cardName,
                                cardUri = current.cardUri,
                                memcard = current.memcard,
                                saves = saves,
                                stats = stats
                            )
                            setLoadedState(loaded)
                            _snackbarMessage.value = "Memory card expanded to ${newSizeMb}MB successfully."
                        } else {
                            val reloaded = CardUiState.Loaded(
                                cardName = current.cardName,
                                cardUri = current.cardUri,
                                memcard = current.memcard,
                                saves = current.memcard.listSaves(),
                                stats = current.memcard.getStats()
                            )
                            setLoadedState(reloaded)
                            _snackbarMessage.value = "Failed to resize memory card."
                        }
                    } catch (t: Throwable) {
                        _uiState.value = CardUiState.Error("Resize error: ${t.message ?: "Failed"}")
                    }
                }
            }
        }
    }

    fun deleteSave(saveName: String) {
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val success = current.memcard.deleteSave(saveName)
                        if (success) {
                            pushUndoSnapshot(snapshotBefore, "Delete save $saveName")
                            val saves = current.memcard.listSaves()
                            val stats = current.memcard.getStats()
                            _selectedSave.value = null
                            _hasUnsavedChanges.value = true
                            val loaded = current.copy(saves = saves, stats = stats)
                            setLoadedState(loaded)
                            _snackbarMessage.value = "Deleted save $saveName"
                        } else {
                            _snackbarMessage.value = "Failed to delete save $saveName"
                        }
                    } catch (e: Throwable) {
                        _snackbarMessage.value = "Delete error: ${e.message}"
                    }
                }
            }
        }
    }

    fun importSave(saveBytes: ByteArray, fileName: String? = null) {
        val currentCheck = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return
        if (!currentCheck.stats.isFormatted) {
            _snackbarMessage.value = "Card must be formatted before importing saves."
            return
        }
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                _uiState.value = CardUiState.Loading("Importing Savegame...")
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val success = current.memcard.importSave(saveBytes, fileName)
                        if (success) {
                            val saves = current.memcard.listSaves()
                            val stats = current.memcard.getStats()
                            val newSave = saves.firstOrNull { old -> current.saves.none { it.directoryName == old.directoryName } }
                            val actionDesc = if (newSave != null) "Import save ${newSave.directoryName}" else "Import save"
                            pushUndoSnapshot(snapshotBefore, actionDesc)
                            _hasUnsavedChanges.value = true
                            val loaded = current.copy(saves = saves, stats = stats)
                            setLoadedState(loaded)
                            _snackbarMessage.value = if (newSave != null) "Imported save '${newSave.directoryName}' successfully!" else "Imported save successfully!"
                        } else {
                            setLoadedState(current)
                            _snackbarMessage.value = "Failed to import save (insufficient space or invalid format)."
                        }
                    } catch (e: Throwable) {
                        setLoadedState(current)
                        _snackbarMessage.value = "Import error: ${e.message}"
                    }
                }
            }
        }
    }

    fun importSaveWithValidation(bytes: ByteArray, fileName: String) {
        val type = Ps2FileDetector.detect(bytes, fileName)
        when (type) {
            Ps2FileType.PS2_FOLDER_MEMCARD -> {
                _snackbarMessage.value = "'$fileName' is a memory card superblock, not a savegame. Use 'Open Card' to open it."
            }
            Ps2FileType.PS2_MEMCARD_IMAGE -> {
                _snackbarMessage.value = "'$fileName' is a PS2 memory card image, not a savegame. Use 'Open Card' to open it."
            }
            Ps2FileType.INVALID -> {
                if (ZipSaveHandler.isZip(bytes) || fileName.endsWith(".zip", ignoreCase = true)) {
                    _snackbarMessage.value = "ZIP archive '$fileName' does not contain a valid PS2 savegame (missing or invalid icon.sys)."
                } else {
                    _snackbarMessage.value = "'$fileName' is not a valid PS2 savegame (.psu, .max, .cbs, .xps, .zip)."
                }
            }
            Ps2FileType.SAVEGAME_PSU,
            Ps2FileType.SAVEGAME_MAX,
            Ps2FileType.SAVEGAME_CBS,
            Ps2FileType.SAVEGAME_XPS,
            Ps2FileType.SAVEGAME_ZIP -> {
                importSave(bytes, fileName)
            }
            Ps2FileType.SAVEGAME_FOLDER -> {
                _snackbarMessage.value = "Use folder import to import savegame directories."
            }
        }
    }

    fun importSaveFolder(saveDir: File) {
        val currentCheck = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return
        if (!currentCheck.stats.isFormatted) {
            _snackbarMessage.value = "Card must be formatted before importing saves."
            return
        }
        if (!FolderMemcardHandler.isValidSaveFolder(saveDir)) {
            _snackbarMessage.value = "Folder '${saveDir.name}' is not a valid PS2 savegame folder (missing or invalid icon.sys)."
            return
        }
        viewModelScope.launch {
            historyMutex.withLock {
                val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return@withLock
                _uiState.value = CardUiState.Loading("Importing Savegame Folder ${saveDir.name}...")
                withContext(Dispatchers.Default) {
                    try {
                        val snapshotBefore = current.memcard.getRawDataDirect().copyOf()
                        val success = FolderMemcardHandler.importSaveFolder(current.memcard, saveDir)
                        if (success) {
                            val saves = current.memcard.listSaves()
                            val stats = current.memcard.getStats()
                            val actionDesc = "Import save ${saveDir.name}"
                            pushUndoSnapshot(snapshotBefore, actionDesc)
                            _hasUnsavedChanges.value = true
                            val loaded = current.copy(saves = saves, stats = stats)
                            setLoadedState(loaded)
                            _snackbarMessage.value = "Imported save ${saveDir.name} successfully!"
                        } else {
                            setLoadedState(current)
                            _snackbarMessage.value = "Failed to import save folder (insufficient space or invalid format)."
                        }
                    } catch (e: Throwable) {
                        setLoadedState(current)
                        _snackbarMessage.value = "Import error: ${e.message}"
                    }
                }
            }
        }
    }

    fun importPsu(psuBytes: ByteArray) = importSave(psuBytes)

    fun importZip(zipBytes: ByteArray, fileName: String? = null) = importSave(zipBytes, fileName)

    fun exportPsu(saveName: String): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.exportSaveAsPsu(saveName)
    }

    fun exportMax(saveName: String): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.exportSaveAsMax(saveName)
    }

    fun exportCbs(saveName: String): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.exportSaveAsCbs(saveName)
    }

    fun exportXps(saveName: String): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.exportSaveAsXps(saveName)
    }

    fun exportZip(saveName: String): ByteArray? {
        val current = (_uiState.value as? CardUiState.Loaded) ?: currentLoadedCard ?: return null
        return current.memcard.exportSaveAsZip(saveName)
    }



    fun closeCard() {
        iconPreloadJob?.cancel()
        currentLoadedCard = null
        _uiState.value = CardUiState.Empty
        savedCardCrc = null
        baselineCardData = null
        clearUndoRedoHistory()
        _hasUnsavedChanges.value = false
        _selectedSave.value = null
        _searchQuery.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        iconPreloadJob?.cancel()
        clearUndoRedoHistory()
    }
}
