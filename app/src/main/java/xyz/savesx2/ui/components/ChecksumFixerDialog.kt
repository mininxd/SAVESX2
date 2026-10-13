package xyz.savesx2.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.savesx2.core.ChecksumAlgorithm
import xyz.savesx2.core.Ps2ChecksumFixer
import java.util.Locale

@Composable
fun ChecksumFixerDialog(
    fileName: String,
    dirName: String,
    data: ByteArray,
    onDismiss: () -> Unit,
    isReadOnly: Boolean = false,
    onApplyPatch: ((offset: Int, newBytes: ByteArray) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var analysis by remember(data) {
        mutableStateOf(Ps2ChecksumFixer.analyzeFile(dirName, fileName, data))
    }

    var selectedAlgorithm by remember { mutableStateOf(ChecksumAlgorithm.SUM32_LE) }
    var algorithmDropdownExpanded by remember { mutableStateOf(false) }

    var offsetMode by remember { mutableStateOf("EOF") } // "EOF", "CUSTOM"
    var customOffsetText by remember { mutableStateOf("") }
    var showCustomInjector by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, value: String) {
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Checksum & Hashes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$fileName • ${data.size} bytes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Detected Game Section
                val profile = analysis.detectedProfile
                if (profile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (profile.isValid) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (profile.isValid) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (profile.isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (profile.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = profile.gameName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profile.isValid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Text(
                                    text = if (profile.isValid) "VALID" else "CORRUPTED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Stored:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "0x%08X".format(profile.storedChecksum),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Expected:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "0x%08X".format(profile.expectedChecksum),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (profile.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            if (!profile.isValid && onApplyPatch != null && !isReadOnly) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val fixedData = Ps2ChecksumFixer.fixDetectedProfile(data, profile)
                                        val newBytes = fixedData.copyOfRange(
                                            profile.checksumOffset,
                                            profile.checksumOffset + profile.checksumLength
                                        )
                                        onApplyPatch(profile.checksumOffset, newBytes)
                                        analysis = Ps2ChecksumFixer.analyzeFile(dirName, fileName, fixedData)
                                        Toast.makeText(context, "Checksum repaired successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fix Checksum Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Hashes List Card
                Text(
                    text = "File Checksums & Hashes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        HashRow(label = "CRC-32", value = analysis.crc32Hex, onCopy = { copyToClipboard("CRC-32", analysis.crc32Hex) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HashRow(label = "Sum-32 (LE)", value = analysis.sum32LeHex, onCopy = { copyToClipboard("Sum-32", analysis.sum32LeHex) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HashRow(label = "Adler-32", value = analysis.adler32Hex, onCopy = { copyToClipboard("Adler-32", analysis.adler32Hex) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HashRow(label = "MD5", value = analysis.md5Hex, onCopy = { copyToClipboard("MD5", analysis.md5Hex) })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        HashRow(label = "SHA-1", value = analysis.sha1Hex, onCopy = { copyToClipboard("SHA-1", analysis.sha1Hex) })
                    }
                }

                // Custom Checksum Patch Section
                if (!isReadOnly && onApplyPatch != null) {
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showCustomInjector = !showCustomInjector },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (showCustomInjector) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showCustomInjector) "Hide Custom Checksum Patch" else "Custom Checksum Patch")
                    }

                    AnimatedVisibility(visible = showCustomInjector) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Algorithm",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Box {
                                        FilledTonalButton(
                                            onClick = { algorithmDropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(selectedAlgorithm.displayName)
                                        }

                                        DropdownMenu(
                                            expanded = algorithmDropdownExpanded,
                                            onDismissRequest = { algorithmDropdownExpanded = false }
                                        ) {
                                            ChecksumAlgorithm.values().forEach { algo ->
                                                DropdownMenuItem(
                                                    text = { Text(algo.displayName) },
                                                    onClick = {
                                                        selectedAlgorithm = algo
                                                        algorithmDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Target Offset",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = { offsetMode = "EOF" },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (offsetMode == "EOF") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (offsetMode == "EOF") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Text("End of File (size - ${selectedAlgorithm.byteSize})")
                                        }

                                        FilledTonalButton(
                                            onClick = { offsetMode = "CUSTOM" },
                                            modifier = Modifier.weight(0.7f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (offsetMode == "CUSTOM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (offsetMode == "CUSTOM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Text("Custom Offset")
                                        }
                                    }

                                    if (offsetMode == "CUSTOM") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = customOffsetText,
                                            onValueChange = { customOffsetText = it },
                                            label = { Text("Offset (hex e.g. 0x1A or dec)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            val targetOffset = if (offsetMode == "EOF") {
                                                maxOf(0, data.size - selectedAlgorithm.byteSize)
                                            } else {
                                                val clean = customOffsetText.trim().removePrefix("0x").removePrefix("0X")
                                                clean.toIntOrNull(16) ?: clean.toIntOrNull() ?: 0
                                            }

                                            if (targetOffset + selectedAlgorithm.byteSize > data.size || targetOffset < 0) {
                                                Toast.makeText(context, "Offset out of bounds", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }

                                            val rangeStart = 0
                                            val rangeLength = if (targetOffset > 0) targetOffset else data.size
                                            val patched = Ps2ChecksumFixer.patchChecksum(
                                                data,
                                                targetOffset,
                                                rangeStart,
                                                rangeLength,
                                                selectedAlgorithm
                                            )
                                            val newBytes = patched.copyOfRange(targetOffset, targetOffset + selectedAlgorithm.byteSize)
                                            onApplyPatch(targetOffset, newBytes)
                                            analysis = Ps2ChecksumFixer.analyzeFile(dirName, fileName, patched)
                                            Toast.makeText(context, "Injected ${selectedAlgorithm.displayName} at 0x${targetOffset.toString(16).uppercase()}", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Calculate & Inject Checksum")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun HashRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
