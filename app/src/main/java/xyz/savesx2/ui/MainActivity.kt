package xyz.savesx2.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import xyz.savesx2.core.BatchExportFormat
import xyz.savesx2.core.ExportFilenameFormat
import xyz.savesx2.core.MemcardFormatter
import xyz.savesx2.core.Ps2Save
import xyz.savesx2.core.FolderMemcardHandler
import xyz.savesx2.core.RecentCard
import xyz.savesx2.core.StoragePermissionHelper
import java.io.File
import xyz.savesx2.ui.components.AppHeader
import xyz.savesx2.ui.components.BatchExportDialog
import xyz.savesx2.ui.components.CardStatsDialog
import xyz.savesx2.ui.components.CreateCardDialog
import xyz.savesx2.ui.components.EditTimestampsDialog
import xyz.savesx2.ui.components.FormatCardDialog
import xyz.savesx2.ui.components.HexViewerDialog
import xyz.savesx2.ui.components.Icon3dViewerDialog
import xyz.savesx2.ui.components.MultiSelectHeader
import xyz.savesx2.ui.components.ResizeCardDialog
import xyz.savesx2.ui.components.SaveDetailModal
import xyz.savesx2.ui.components.SettingsDialog
import xyz.savesx2.ui.components.SwipeDismissNotification
import xyz.savesx2.ui.screens.EmptyStateScreen
import xyz.savesx2.ui.screens.MainScreen
import xyz.savesx2.ui.theme.PS2MemcardTheme

private data class PendingCreateCard(
    val name: String,
    val sizeInMB: Int,
    val useEcc: Boolean,
    val isFormatted: Boolean
)

class MainActivity : ComponentActivity() {

    private val viewModel: MemcardViewModel by viewModels()

    private var pendingExportPsuBytes: ByteArray? = null
    private var pendingExportMaxBytes: ByteArray? = null
    private var pendingExportCbsBytes: ByteArray? = null
    private var pendingExportXpsBytes: ByteArray? = null
    private var pendingExportZipBytes: ByteArray? = null
    private var pendingBatchExportFormat: BatchExportFormat? = null
    private var pendingBatchExportSaves: List<Ps2Save>? = null
    private var pendingCreateCard: PendingCreateCard? = null
    private var pendingActionAfterSave: (() -> Unit)? = null
    private var isWaitingForActivityResult = false

    private val snackbarHostState = SnackbarHostState()

    /**
     * Shows an in-app notification / toast message with swipe-to-dismiss (slide to hide) support.
     */
    private fun showToast(message: String, isLong: Boolean = false) {
        lifecycleScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (isLong) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }

    private val openCardLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
            loadCardFromUri(it)
        }
    }

    private val selectSaveDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        isWaitingForActivityResult = false
        if (uri == null) {
            pendingCreateCard = null
            pendingActionAfterSave = null
            return@registerForActivityResult
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        val docDir = DocumentFile.fromTreeUri(this, uri)
        val dirName = docDir?.name ?: uri.lastPathSegment ?: "Selected Folder"
        val prefs = getSharedPreferences("memcard_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("custom_dir_uri", uri.toString())
            .putString("custom_dir_name", dirName)
            .apply()
        viewModel.setCustomDirectory(uri, dirName)

        val createParams = pendingCreateCard
        if (createParams != null) {
            pendingCreateCard = null
            createAndSaveCard(uri, createParams.name, createParams.sizeInMB, createParams.useEcc, createParams.isFormatted)
            return@registerForActivityResult
        }

        saveLoadedCardToDirectory(uri, dirName)
    }

    private fun createAndSaveCard(dirUri: Uri, name: String, sizeInMB: Int, useEcc: Boolean, formatted: Boolean) {
        lifecycleScope.launch {
            viewModel.setLoading("Creating $name (${sizeInMB}MB)...")
            withContext(Dispatchers.IO) {
                try {
                    val tree = DocumentFile.fromTreeUri(this@MainActivity, dirUri)
                    val targetFile = tree?.findFile(name) ?: tree?.createFile("application/octet-stream", name)
                    if (targetFile == null) {
                        withContext(Dispatchers.Main) {
                            showToast("Could not create file in selected directory", isLong = true)
                            viewModel.clearLoading()
                        }
                        return@withContext
                    }

                    val bytes = if (formatted) {
                        MemcardFormatter.format(sizeInMB, useEcc)
                    } else {
                        MemcardFormatter.createUnformatted(sizeInMB, useEcc)
                    }

                    writeBytesToSafUriAtomically(targetFile.uri, bytes)

                    withContext(Dispatchers.Main) {
                        viewModel.loadCardFromBytes(name, bytes, targetFile.uri)
                        viewModel.addRecentCard(
                            this@MainActivity,
                            RecentCard(
                                uriString = targetFile.uri.toString(),
                                fileName = name,
                                sizeBytes = bytes.size.toLong(),
                                saveCount = 0,
                                lastOpened = System.currentTimeMillis()
                            )
                        )
                        showToast("Created and saved $name successfully!")
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to create card: ${t.message ?: "Out of memory"}", isLong = true)
                        viewModel.setError("Failed to create card: ${t.message ?: "Out of memory"}")
                    }
                }
            }
        }
    }

    private fun writeBytesToSafUriAtomically(targetUri: Uri, data: ByteArray) {
        val tempFile = File.createTempFile("savesx2_card_atomic_", ".tmp", cacheDir)
        try {
            tempFile.outputStream().use { fos ->
                fos.write(data)
                fos.flush()
            }
            val out = try {
                contentResolver.openOutputStream(targetUri, "wt")
            } catch (_: Exception) {
                contentResolver.openOutputStream(targetUri, "w")
            } ?: throw java.io.IOException("Could not open output stream for writing")

            out.use { stream ->
                tempFile.inputStream().use { fis ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        stream.write(buffer, 0, bytesRead)
                    }
                    stream.flush()
                }
            }
        } finally {
            tempFile.delete()
        }
    }

    private fun saveLoadedCardToDirectory(dirUri: Uri, dirName: String) {
        val loaded = viewModel.uiState.value as? CardUiState.Loaded ?: return
        val rawData = loaded.memcard.getRawDataDirect()
        val cardName = loaded.cardName
        lifecycleScope.launch {
            viewModel.setLoading("Saving $cardName...")
            withContext(Dispatchers.IO) {
                try {
                    val docDir = DocumentFile.fromTreeUri(this@MainActivity, dirUri)
                    val targetFile = docDir?.findFile(cardName) ?: docDir?.createFile("application/octet-stream", cardName)
                    if (targetFile != null) {
                        writeBytesToSafUriAtomically(targetFile.uri, rawData)
                        withContext(Dispatchers.Main) {
                            viewModel.markCardSaved(targetFile.uri, cardName, this@MainActivity)
                            showToast("Saved $cardName to $dirName successfully!")
                            val action = pendingActionAfterSave
                            pendingActionAfterSave = null
                            action?.invoke()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Could not create $cardName in $dirName", isLong = true)
                            pendingActionAfterSave = null
                            viewModel.clearLoading()
                        }
                    }
                } catch (t: Throwable) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to save card: ${t.message ?: "Error"}", isLong = true)
                        pendingActionAfterSave = null
                        viewModel.clearLoading()
                    }
                }
            }
        }
    }

    private val importSaveLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let {
            val fileName = queryFileName(it) ?: "save"
            val file = StoragePermissionHelper.resolveFileFromUri(this, it)
            if (file != null && file.isDirectory) {
                viewModel.importSaveFolder(file)
                return@registerForActivityResult
            }
            try {
                contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    viewModel.importSaveWithValidation(bytes, fileName)
                }
            } catch (e: Exception) {
                showToast("Failed to read save file: ${e.message}", isLong = true)
            }
        }
    }

    private val exportPsuLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let { targetUri ->
            pendingExportPsuBytes?.let { bytes ->
                try {
                    contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bytes)
                    }
                    showToast("PSU save exported successfully!")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}", isLong = true)
                }
            }
        }
        pendingExportPsuBytes = null
    }

    private val exportMaxLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let { targetUri ->
            pendingExportMaxBytes?.let { bytes ->
                try {
                    contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bytes)
                    }
                    showToast("Action Replay MAX save exported successfully!")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}", isLong = true)
                }
            }
        }
        pendingExportMaxBytes = null
    }

    private val exportCbsLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let { targetUri ->
            pendingExportCbsBytes?.let { bytes ->
                try {
                    contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bytes)
                    }
                    showToast("CodeBreaker (.cbs) save exported successfully!")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}", isLong = true)
                }
            }
        }
        pendingExportCbsBytes = null
    }

    private val exportXpsLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let { targetUri ->
            pendingExportXpsBytes?.let { bytes ->
                try {
                    contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bytes)
                    }
                    showToast("SharkPort / X-Port (.xps) save exported successfully!")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}", isLong = true)
                }
            }
        }
        pendingExportXpsBytes = null
    }

    private val exportZipLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri: Uri? ->
        isWaitingForActivityResult = false
        uri?.let { targetUri ->
            pendingExportZipBytes?.let { bytes ->
                try {
                    contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bytes)
                    }
                    showToast("ZIP archive exported successfully!")
                } catch (e: Exception) {
                    showToast("Export failed: ${e.message}", isLong = true)
                }
            }
        }
        pendingExportZipBytes = null
    }

    private val batchExportDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        isWaitingForActivityResult = false
        val format = pendingBatchExportFormat
        val saves = pendingBatchExportSaves
        pendingBatchExportFormat = null
        pendingBatchExportSaves = null
        if (uri == null || format == null || saves.isNullOrEmpty()) {
            return@registerForActivityResult
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        executeBatchExport(uri, format, saves)
    }

    private val manageStorageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isWaitingForActivityResult = false
        val granted = StoragePermissionHelper.hasStoragePermission(this)
        viewModel.updateStoragePermission(granted)
        if (granted) {
            showToast("Storage access granted")
        } else {
            showToast("Storage access is required to manage memory cards", isLong = false)
        }
    }

    private val requestLegacyStorageLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        isWaitingForActivityResult = false
        val granted = StoragePermissionHelper.hasStoragePermission(this)
        viewModel.updateStoragePermission(granted)
        if (granted) {
            showToast("Storage access granted")
        } else {
            showToast("Storage access is required to manage memory cards", isLong = false)
        }
    }

    private fun redirectToStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isWaitingForActivityResult = true
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    isWaitingForActivityResult = false
                    showToast("Could not open storage settings: ${e.message}", isLong = true)
                }
            }
        } else {
            isWaitingForActivityResult = true
            requestLegacyStorageLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun launchOpenCard() {
        isWaitingForActivityResult = true
        openCardLauncher.launch(arrayOf("*/*"))
    }

    private fun launchSelectSaveDirectory() {
        isWaitingForActivityResult = true
        selectSaveDirectoryLauncher.launch(null)
    }

    private fun launchImportSave() {
        isWaitingForActivityResult = true
        importSaveLauncher.launch(arrayOf("*/*"))
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasStoragePermission = StoragePermissionHelper.hasStoragePermission(this)
        viewModel.updateStoragePermission(hasStoragePermission)
        if (!hasStoragePermission) {
            redirectToStorageSettings()
        }

        viewModel.initSettings(this)

        // Load persisted custom directory
        val prefs = getSharedPreferences("memcard_prefs", Context.MODE_PRIVATE)
        val savedDirUri = prefs.getString("custom_dir_uri", null)
        val savedDirName = prefs.getString("custom_dir_name", null)
        if (savedDirUri != null) {
            viewModel.setCustomDirectory(Uri.parse(savedDirUri), savedDirName)
        }

        // Handle opening file from external intent
        intent?.data?.let { uri ->
            loadCardFromUri(uri)
        }

        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.7.3"
        } catch (_: Exception) {
            "1.7.3"
        }
        viewModel.checkUpdate(appVersion)

        setContent {
            PS2MemcardTheme {
                val uiState by viewModel.uiState.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val filterType by viewModel.filterType.collectAsState()
                val sortBy by viewModel.sortBy.collectAsState()
                val selectedSave by viewModel.selectedSave.collectAsState()
                val selectedSaveNames by viewModel.selectedSaveNames.collectAsState()
                val showCreateDialog by viewModel.showCreateDialog.collectAsState()
                val showFormatDialog by viewModel.showFormatDialog.collectAsState()
                val showStatsDialog by viewModel.showStatsDialog.collectAsState()
                val showResizeDialog by viewModel.showResizeDialog.collectAsState()
                val icon3dSession by viewModel.icon3dSession.collectAsState()
                val hexSession by viewModel.hexEditorSession.collectAsState()
                val snackbarMessage by viewModel.snackbarMessage.collectAsState()
                val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
                val customDirectoryName by viewModel.customDirectoryName.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                val recentCards by viewModel.recentCards.collectAsState()
                val exportFilenameFormat by viewModel.exportFilenameFormat.collectAsState()
                val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                val canUndo by viewModel.canUndo.collectAsState()
                val canRedo by viewModel.canRedo.collectAsState()
                val hasLegacyApp by viewModel.hasLegacyApp.collectAsState()

                var showBatchExportDialog by remember { mutableStateOf(false) }
                var showBatchDeleteConfirmDialog by remember { mutableStateOf(false) }

                LaunchedEffect(snackbarMessage) {
                    snackbarMessage?.let { msg ->
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSnackbar()
                    }
                }

                val currentCardName = (uiState as? CardUiState.Loaded)?.cardName
                val isInMemoryOnly = (uiState as? CardUiState.Loaded)?.cardUri == null
                var showUnsavedChangesDialog by remember { mutableStateOf(false) }
                var showReloadConfirmDialog by remember { mutableStateOf(false) }
                var editingTimestampsSave by remember { mutableStateOf<Ps2Save?>(null) }

                BackHandler(enabled = selectedSaveNames.isNotEmpty()) {
                    viewModel.clearSelection()
                }

                BackHandler(enabled = (uiState is CardUiState.Loaded) && selectedSave == null) {
                    if (canUndo || canRedo) {
                        viewModel.cancelEdit()
                    } else if (hasUnsavedChanges || isInMemoryOnly) {
                        showUnsavedChangesDialog = true
                    } else {
                        viewModel.closeCard()
                    }
                }

                if (showUnsavedChangesDialog) {
                    AlertDialog(
                        onDismissRequest = { showUnsavedChangesDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Text(
                                text = "Save Changes?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "You have unsaved changes on this memory card. Do you want to save before closing?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showUnsavedChangesDialog = false
                                    pendingActionAfterSave = { viewModel.closeCard() }
                                    triggerSaveCurrentCard()
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            Row {
                                TextButton(
                                    onClick = {
                                        showUnsavedChangesDialog = false
                                        viewModel.closeCard()
                                    }
                                ) {
                                    Text("Discard")
                                }
                                TextButton(
                                    onClick = { showUnsavedChangesDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    )
                }

                if (showReloadConfirmDialog) {
                    val loaded = uiState as? CardUiState.Loaded
                    AlertDialog(
                        onDismissRequest = { showReloadConfirmDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Text(
                                text = "Discard Changes & Reload?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "You have unsaved changes on this memory card. Reloading will discard all unsaved edits and re-read from storage.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showReloadConfirmDialog = false
                                    loaded?.cardUri?.let { uri ->
                                        viewModel.reloadCard(contentResolver, uri)
                                    }
                                }
                            ) {
                                Text("Discard & Reload")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReloadConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        if (selectedSaveNames.isNotEmpty()) {
                            val loaded = uiState as? CardUiState.Loaded
                            val allSaves = loaded?.saves ?: emptyList()
                            val allVisibleSelected = allSaves.isNotEmpty() && allSaves.all { it.directoryName in selectedSaveNames }
                            MultiSelectHeader(
                                selectedCount = selectedSaveNames.size,
                                isAllSelected = allVisibleSelected,
                                onToggleSelectAll = {
                                    if (allVisibleSelected) {
                                        viewModel.clearSelection()
                                    } else {
                                        viewModel.selectAllSaves(allSaves.map { it.directoryName })
                                    }
                                },
                                onExport = {
                                    showBatchExportDialog = true
                                },
                                onDelete = {
                                    showBatchDeleteConfirmDialog = true
                                },
                                onClose = {
                                    viewModel.clearSelection()
                                }
                            )
                        } else {
                            AppHeader(
                                cardName = currentCardName,
                                hasUnsavedChanges = hasUnsavedChanges,
                                isInMemoryOnly = isInMemoryOnly,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() },
                                onOpenCard = { launchOpenCard() },
                                onCreateCard = { viewModel.setShowCreateDialog(true) },
                                onSaveCard = { triggerSaveCurrentCard() },
                                onSaveCardAs = { triggerSaveCardAs() },
                                onFormatCard = { viewModel.setShowFormatDialog(true) },
                                onShowStats = { viewModel.setShowStatsDialog(true) },
                                onResizeCard = { viewModel.setShowResizeDialog(true) },
                                isFolderCard = (uiState as? CardUiState.Loaded)?.isFolderCard ?: false,
                                onOpenRawHex = {
                                    (uiState as? CardUiState.Loaded)?.let { loaded ->
                                        viewModel.openHexEditor(
                                            title = "${loaded.cardName} (Raw Card)",
                                            data = loaded.memcard.getRawDataDirect(),
                                            isRawCard = true
                                        )
                                    }
                                },
                                onCancelEdit = { viewModel.cancelEdit() },
                                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
                            )
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(snackbarHostState) { data ->
                            key(data) {
                                SwipeDismissNotification(
                                    onDismiss = { data.dismiss() }
                                ) {
                                    Snackbar(
                                        snackbarData = data,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (val state = uiState) {
                            is CardUiState.Empty -> {
                                EmptyStateScreen(
                                    onOpenCard = { launchOpenCard() },
                                    onCreateCard = { viewModel.setShowCreateDialog(true) },
                                    recentCards = recentCards,
                                    onOpenRecentCard = { recent ->
                                        loadCardFromUri(Uri.parse(recent.uriString))
                                    },
                                    onRemoveRecentCard = { uriStr ->
                                        viewModel.removeRecentCard(this@MainActivity, uriStr)
                                    },
                                    onClearRecentCards = {
                                        viewModel.clearRecentCards(this@MainActivity)
                                    },
                                    onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                                    updateStatus = updateStatus,
                                    onCheckUpdate = {
                                         val version = try {
                                             packageManager.getPackageInfo(packageName, 0).versionName ?: "1.7.3"
                                         } catch (e: Exception) {
                                             "1.7.3"
                                         }
                                        viewModel.checkUpdate(version)
                                    }
                                )
                            }
                            is CardUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            is CardUiState.Loaded -> {
                                val onSaveClick = remember(viewModel) { { save: Ps2Save -> viewModel.selectSave(save) } }
                                val onExportPsu = remember { { save: Ps2Save -> triggerExportPsu(save) } }
                                val onExportZip = remember { { save: Ps2Save -> triggerExportZip(save) } }
                                val onDeleteSave = remember(viewModel) { { save: Ps2Save -> viewModel.deleteSave(save.directoryName) } }
                                val onSearchChange = remember(viewModel) { { q: String -> viewModel.setSearchQuery(q) } }
                                val onFilterChange = remember(viewModel) { { f: FilterType -> viewModel.setFilterType(f) } }
                                val onSortChange = remember(viewModel) { { s: SortBy -> viewModel.setSortBy(s) } }
                                val onImportPsu = remember { { launchImportSave() } }
                                val onSaveCard = remember { { triggerSaveCurrentCard() } }
                                val onFormatCard = remember(viewModel) { { viewModel.setShowFormatDialog(true) } }
                                val onRefresh = remember(state.cardUri, hasUnsavedChanges) {
                                    {
                                        val uri = state.cardUri
                                        if (uri == null) {
                                            showToast("Card is stored in memory only (not saved to file)")
                                        } else if (hasUnsavedChanges) {
                                            showReloadConfirmDialog = true
                                        } else {
                                            viewModel.reloadCard(contentResolver, uri)
                                        }
                                    }
                                }

                                MainScreen(
                                    saves = state.saves,
                                    stats = state.stats,
                                    searchQuery = searchQuery,
                                    onSearchChange = onSearchChange,
                                    filterType = filterType,
                                    onFilterChange = onFilterChange,
                                    sortBy = sortBy,
                                    onSortChange = onSortChange,
                                    selectedSaveNames = selectedSaveNames,
                                    onToggleSelectSave = { save -> viewModel.toggleSaveSelection(save.directoryName) },
                                    onStartMultiSelect = { save -> viewModel.selectSaveForMultiSelect(save.directoryName) },
                                    onExportSelected = { showBatchExportDialog = true },
                                    onSaveClick = onSaveClick,
                                    onExportPsu = onExportPsu,
                                    onExportZip = onExportZip,
                                    onDeleteSave = onDeleteSave,
                                    onImportPsu = onImportPsu,
                                    hasUnsavedChanges = hasUnsavedChanges,
                                    isInMemoryOnly = isInMemoryOnly,
                                    onSaveCard = onSaveCard,
                                    onFormatCard = onFormatCard,
                                    isRefreshing = isRefreshing,
                                    onRefresh = onRefresh,
                                    onLoadIcon = { viewModel.loadSaveIcon(it) }
                                )
                            }
                            is CardUiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { launchOpenCard() }) {
                                            Text("Open Another Card")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modals & Dialogs
                selectedSave?.let { save ->
                    SaveDetailModal(
                        save = save,
                        onDismiss = { viewModel.selectSave(null) },
                        onExportPsu = {
                            viewModel.selectSave(null)
                            triggerExportPsu(save)
                        },
                        onExportMax = {
                            viewModel.selectSave(null)
                            triggerExportMax(save)
                        },
                        onExportCbs = {
                            viewModel.selectSave(null)
                            triggerExportCbs(save)
                        },
                        onExportXps = {
                            viewModel.selectSave(null)
                            triggerExportXps(save)
                        },
                        onExportZip = {
                            viewModel.selectSave(null)
                            triggerExportZip(save)
                        },
                        onDelete = {
                            viewModel.selectSave(null)
                            viewModel.deleteSave(save.directoryName)
                        },
                        onToggleProtection = { isProtected ->
                            viewModel.setSaveProtection(save.directoryName, isProtected)
                        },
                        onEditTimestamps = {
                            editingTimestampsSave = save
                        },
                        onInspectFileHex = { file ->
                            val data = file.data ?: (uiState as? CardUiState.Loaded)?.memcard?.getSaveFileBytes(save.directoryName, file.name) ?: ByteArray(0)
                            viewModel.openHexEditor(
                                title = file.name,
                                data = data,
                                saveName = save.directoryName,
                                fileName = file.name
                            )
                        },
                        onView3dIcon = {
                            viewModel.open3dIconViewer(save)
                        },
                        onLoadIcon = { viewModel.loadSaveIcon(it) }
                    )
                }

                editingTimestampsSave?.let { saveToEdit ->
                    EditTimestampsDialog(
                        saveTitle = saveToEdit.displayTitle,
                        initialCreated = saveToEdit.dirEntry.created,
                        initialModified = saveToEdit.dirEntry.modified,
                        onDismiss = { editingTimestampsSave = null },
                        onSave = { newCreated, newModified ->
                            editingTimestampsSave = null
                            viewModel.updateSaveTimestamps(saveToEdit.directoryName, newCreated, newModified)
                        }
                    )
                }

                if (showCreateDialog) {
                    CreateCardDialog(
                        customDirectoryName = customDirectoryName,
                        onSelectCustomDirectory = { launchSelectSaveDirectory() },
                        onDismiss = { viewModel.setShowCreateDialog(false) },
                        onSaveCard = { name, size, ecc, formatted ->
                            viewModel.setShowCreateDialog(false)
                            val customDirUri = viewModel.customDirectoryUri.value
                            if (customDirUri != null) {
                                createAndSaveCard(customDirUri, name, size, ecc, formatted)
                            } else {
                                pendingCreateCard = PendingCreateCard(name, size, ecc, formatted)
                                launchSelectSaveDirectory()
                            }
                        }
                    )
                }

                if (showFormatDialog) {
                    FormatCardDialog(
                        cardName = currentCardName ?: "Memory Card",
                        onDismiss = { viewModel.setShowFormatDialog(false) },
                        onConfirmFormat = {
                            viewModel.setShowFormatDialog(false)
                            viewModel.formatCurrentCard()
                        }
                    )
                }

                if (showResizeDialog) {
                    (uiState as? CardUiState.Loaded)?.let { loaded ->
                        if (!loaded.isFolderCard) {
                            val currentMb = (loaded.memcard.totalCapacityMb + 0.5).toInt()
                            ResizeCardDialog(
                                cardName = loaded.cardName,
                                currentCapacityMb = currentMb,
                                onDismiss = { viewModel.setShowResizeDialog(false) },
                                onConfirmResize = { targetMb ->
                                    viewModel.setShowResizeDialog(false)
                                    viewModel.resizeCurrentCard(targetMb)
                                }
                            )
                        } else {
                            viewModel.setShowResizeDialog(false)
                        }
                    }
                }

                icon3dSession?.let { session ->
                    Icon3dViewerDialog(
                        session = session,
                        onDismiss = { viewModel.close3dIconViewer() }
                    )
                }

                if (showStatsDialog) {
                    (uiState as? CardUiState.Loaded)?.let { loaded ->
                        CardStatsDialog(
                            superBlock = loaded.memcard.superBlock,
                            stats = loaded.stats,
                            onDismiss = { viewModel.setShowStatsDialog(false) },
                            onOpenHex = {
                                viewModel.openHexEditor(
                                    title = "${loaded.cardName} (Raw Card)",
                                    data = loaded.memcard.getRawDataDirect(),
                                    isRawCard = true
                                )
                            }
                        )
                    }
                }

                hexSession?.let { session ->
                    HexViewerDialog(
                        title = session.title,
                        data = session.data,
                        isReadOnly = session.isReadOnly,
                        onSave = if (!session.isReadOnly && (session.saveName != null || session.isRawCard)) {
                            { modifiedBytes ->
                                viewModel.saveHexEditedFile(
                                    saveName = session.saveName,
                                    fileName = session.fileName,
                                    newData = modifiedBytes,
                                    isRawCard = session.isRawCard
                                )
                            }
                        } else null,
                        onDismiss = { viewModel.closeHexEditor() }
                    )
                }

                if (showSettingsDialog) {
                    SettingsDialog(
                        currentFormat = exportFilenameFormat,
                        onFormatSelected = { viewModel.setExportFilenameFormat(this@MainActivity, it) },
                        hasRecentCards = recentCards.isNotEmpty(),
                        onClearRecentCards = { viewModel.clearRecentCards(this@MainActivity) },
                        onDismiss = { viewModel.setShowSettingsDialog(false) },
                        versionName = try {
                            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.7.3"
                        } catch (_: Exception) {
                            "1.7.3"
                        }
                    )
                }

                if (showBatchExportDialog) {
                    val loadedSaves = (uiState as? CardUiState.Loaded)?.saves ?: emptyList()
                    val selectedSavesList = loadedSaves.filter { it.directoryName in selectedSaveNames }
                    BatchExportDialog(
                        saveCount = selectedSavesList.size,
                        onDismiss = { showBatchExportDialog = false },
                        onSelectFormat = { format ->
                            showBatchExportDialog = false
                            launchBatchExport(format, selectedSavesList)
                        }
                    )
                }

                if (showBatchDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatchDeleteConfirmDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        title = {
                            Text(
                                text = "Delete ${selectedSaveNames.size} Saves?",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = "Are you sure you want to delete the selected ${selectedSaveNames.size} save(s) from this memory card?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showBatchDeleteConfirmDialog = false
                                    viewModel.deleteSaves(selectedSaveNames)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatchDeleteConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (hasLegacyApp) {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissLegacyAppPrompt() },
                        shape = RoundedCornerShape(24.dp),
                        title = {
                            Text(
                                text = "Older App Detected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "An older version of this app (xyz.mininxd.ps2memcards) was detected on your device.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "To avoid duplicate file associations and confusion, it is recommended to uninstall it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.dismissLegacyAppPrompt()
                                    val legacyPkg = "xyz.mininxd.ps2memcards"
                                    try {
                                        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                            data = Uri.parse("package:$legacyPkg")
                                            putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                        }
                                        startActivity(uninstallIntent)
                                    } catch (_: Exception) {
                                        try {
                                            @Suppress("DEPRECATION")
                                            val fallbackIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                                                data = Uri.parse("package:$legacyPkg")
                                                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                            }
                                            startActivity(fallbackIntent)
                                        } catch (_: Exception) {
                                            try {
                                                val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.parse("package:$legacyPkg")
                                                }
                                                startActivity(appDetailsIntent)
                                            } catch (e: Exception) {
                                                showToast("Could not launch uninstaller: ${e.message}")
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Uninstall")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.dismissLegacyAppPrompt() }) {
                                Text("Later")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.checkLegacyApp(this)
        if (!isWaitingForActivityResult) {
            val granted = StoragePermissionHelper.hasStoragePermission(this)
            viewModel.updateStoragePermission(granted)
            if (!granted) {
                redirectToStorageSettings()
            }
        }
        isWaitingForActivityResult = false
    }

    override fun onResume() {
        super.onResume()
        val granted = StoragePermissionHelper.hasStoragePermission(this)
        viewModel.updateStoragePermission(granted)
        viewModel.checkLegacyApp(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            loadCardFromUri(uri)
        }
    }

    private fun triggerSaveCurrentCard() {
        val loaded = viewModel.uiState.value as? CardUiState.Loaded ?: return

        // If card is a folder card and has a folder path on disk
        if (loaded.isFolderCard && loaded.folderPath != null) {
            val folder = File(loaded.folderPath)
            lifecycleScope.launch {
                viewModel.setLoading("Saving ${loaded.cardName} folder...")
                withContext(Dispatchers.IO) {
                    try {
                        val ok = FolderMemcardHandler.convertFileToFolder(loaded.memcard, folder)
                        if (ok) {
                            withContext(Dispatchers.Main) {
                                viewModel.markCardSaved(loaded.cardUri, loaded.cardName, this@MainActivity)
                                showToast("Saved ${loaded.cardName} folder successfully!")
                                val action = pendingActionAfterSave
                                pendingActionAfterSave = null
                                action?.invoke()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                showToast("Failed to save folder memory card", isLong = true)
                                viewModel.clearLoading()
                            }
                        }
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            showToast("Failed to save folder card: ${t.message ?: "Error"}", isLong = true)
                            viewModel.clearLoading()
                        }
                    }
                }
            }
            return
        }

        // If card already has a file URI, save directly to it
        if (loaded.cardUri != null) {
            val cardUri = loaded.cardUri
            val cardName = loaded.cardName
            val rawData = loaded.memcard.getRawDataDirect()
            lifecycleScope.launch {
                viewModel.setLoading("Saving $cardName...")
                withContext(Dispatchers.IO) {
                    try {
                        writeBytesToSafUriAtomically(cardUri, rawData)
                        withContext(Dispatchers.Main) {
                            viewModel.markCardSaved(cardUri, cardName, this@MainActivity)
                            showToast("Saved $cardName successfully!")
                            val action = pendingActionAfterSave
                            pendingActionAfterSave = null
                            action?.invoke()
                        }
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            // If writing to original URI failed (e.g. permission lost), fall back to folder picker
                            viewModel.clearLoading()
                            triggerSaveCardAs()
                        }
                    }
                }
            }
            return
        }

        // If custom directory is set, save to that directory
        val customDirUri = viewModel.customDirectoryUri.value
        if (customDirUri != null) {
            saveLoadedCardToDirectory(customDirUri, viewModel.customDirectoryName.value ?: "Custom Directory")
            return
        }

        // Otherwise prompt folder picker
        triggerSaveCardAs()
    }

    private fun triggerSaveCardAs() {
        viewModel.clearLoading()
        if (viewModel.uiState.value is CardUiState.Loaded) {
            launchSelectSaveDirectory()
        }
    }

    private fun loadCardFromUri(uri: Uri) {
        val fileName = queryFileName(uri) ?: "MemoryCard.ps2"
        viewModel.loadCardFromUri(contentResolver, uri, fileName, this)
    }

    private fun triggerExportPsu(save: Ps2Save) {
        val bytes = viewModel.exportPsu(save.directoryName)
        if (bytes != null) {
            pendingExportPsuBytes = bytes
            val filename = ExportFilenameFormat.generateFilename(save, "psu", viewModel.exportFilenameFormat.value)
            isWaitingForActivityResult = true
            exportPsuLauncher.launch(filename)
        } else {
            showToast("Failed to export PSU")
        }
    }

    private fun triggerExportMax(save: Ps2Save) {
        val bytes = viewModel.exportMax(save.directoryName)
        if (bytes != null) {
            pendingExportMaxBytes = bytes
            val filename = ExportFilenameFormat.generateFilename(save, "max", viewModel.exportFilenameFormat.value)
            isWaitingForActivityResult = true
            exportMaxLauncher.launch(filename)
        } else {
            showToast("Failed to export Action Replay MAX save")
        }
    }

    private fun triggerExportCbs(save: Ps2Save) {
        val bytes = viewModel.exportCbs(save.directoryName)
        if (bytes != null) {
            pendingExportCbsBytes = bytes
            val filename = ExportFilenameFormat.generateFilename(save, "cbs", viewModel.exportFilenameFormat.value)
            isWaitingForActivityResult = true
            exportCbsLauncher.launch(filename)
        } else {
            showToast("Failed to export CodeBreaker save")
        }
    }

    private fun triggerExportXps(save: Ps2Save) {
        val bytes = viewModel.exportXps(save.directoryName)
        if (bytes != null) {
            pendingExportXpsBytes = bytes
            val filename = ExportFilenameFormat.generateFilename(save, "xps", viewModel.exportFilenameFormat.value)
            isWaitingForActivityResult = true
            exportXpsLauncher.launch(filename)
        } else {
            showToast("Failed to export SharkPort / X-Port save")
        }
    }

    private fun triggerExportZip(save: Ps2Save) {
        val bytes = viewModel.exportZip(save.directoryName)
        if (bytes != null) {
            pendingExportZipBytes = bytes
            val filename = ExportFilenameFormat.generateFilename(save, "zip", viewModel.exportFilenameFormat.value)
            isWaitingForActivityResult = true
            exportZipLauncher.launch(filename)
        } else {
            showToast("Failed to export ZIP")
        }
    }

    private fun launchBatchExport(format: BatchExportFormat, saves: List<Ps2Save>) {
        if (saves.isEmpty()) return
        pendingBatchExportFormat = format
        pendingBatchExportSaves = saves
        isWaitingForActivityResult = true
        batchExportDirectoryLauncher.launch(null)
    }

    private fun executeBatchExport(dirUri: Uri, format: BatchExportFormat, selectedSaves: List<Ps2Save>) {
        lifecycleScope.launch {
            viewModel.setLoading("Exporting ${selectedSaves.size} saves as ${format.title}...")
            val result = withContext(Dispatchers.IO) {
                try {
                    val targetDir = DocumentFile.fromTreeUri(this@MainActivity, dirUri)
                    if (targetDir == null || !targetDir.isDirectory) {
                        return@withContext Triple(0, selectedSaves.size, "Could not access selected directory")
                    }
                    var successCount = 0
                    var failCount = 0
                    val dirName = targetDir.name ?: "selected folder"

                    for (save in selectedSaves) {
                        val bytes = viewModel.exportSave(save.directoryName, format)
                        if (bytes == null) {
                            failCount++
                            continue
                        }
                        val filename = ExportFilenameFormat.generateFilename(
                            save,
                            format.extension,
                            viewModel.exportFilenameFormat.value
                        )
                        val targetFile = targetDir.findFile(filename) ?: targetDir.createFile("application/octet-stream", filename)
                        if (targetFile != null) {
                            try {
                                writeBytesToSafUriAtomically(targetFile.uri, bytes)
                                successCount++
                            } catch (_: Exception) {
                                failCount++
                            }
                        } else {
                            failCount++
                        }
                    }
                    Triple(successCount, failCount, dirName)
                } catch (e: Exception) {
                    Triple(0, selectedSaves.size, e.message ?: "Unknown error")
                }
            }
            viewModel.clearLoading()
            viewModel.clearSelection()
            if (result.second == 0) {
                showToast("Exported ${result.first} saves as ${format.title} to ${result.third}!", isLong = true)
            } else if (result.first > 0) {
                showToast("Exported ${result.first} saves (${result.second} failed) to ${result.third}", isLong = true)
            } else {
                showToast("Batch export failed: ${result.third}", isLong = true)
            }
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index)
                    }
                }
            }
        }
        return uri.lastPathSegment
    }
}
