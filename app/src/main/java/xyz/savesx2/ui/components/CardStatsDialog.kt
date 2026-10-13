package xyz.savesx2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.savesx2.core.CardStats
import xyz.savesx2.core.Ps2SuperBlock

@Composable
fun CardStatsDialog(
    superBlock: Ps2SuperBlock? = null,
    stats: CardStats,
    isPs1Card: Boolean = false,
    onDismiss: () -> Unit,
    onOpenHex: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (isPs1Card) "PS1 Memory Card Diagnostics" else "Memory Card Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isPs1Card) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatRow("Format Version", "PlayStation 1 (PSX / PS1)")
                            StatRow("Card Architecture", "128 KB EEPROM (16 Frames)")
                            StatRow("Total Capacity", "128 KB (131,072 bytes)")
                            StatRow("Frame Size", "8,192 bytes (8 KB)")
                            StatRow("Directory Frame", "Frame 0 (64 blocks × 128 bytes)")
                            StatRow("Payload Frames", "Frames 1..15 (8 KB each)")
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatRow("Total Slots", "15 Slots")
                            StatRow("Used Slots", "${stats.allocatedClusters} Slots")
                            StatRow("Free Slots", "${stats.freeClusters} Slots")
                            StatRow("Free Space", "${stats.freeSpaceKb} KB")
                            StatRow("Integrity Verification", "XOR Block Checksums (0..126)")
                        }
                    }
                } else if (superBlock != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatRow("Format Version", superBlock.version)
                            StatRow("Card Type", if (superBlock.cardType == 2) "PlayStation 2 (Standard)" else "Type ${superBlock.cardType}")
                            StatRow("Total Capacity", "${stats.totalSpaceKb / 1024} MB (${stats.totalSpaceKb} KB)")
                            StatRow("Page Size", "${stats.pageSize} bytes (${if (stats.hasEcc) "ECC Enabled" else "RAW"})")
                            StatRow("Cluster Size", "${stats.clusterSize} bytes (${superBlock.pagesPerCluster} pages)")
                            StatRow("Erase Block Size", "${superBlock.pagesPerBlock} pages (${superBlock.pagesPerBlock * superBlock.pageLen} bytes)")
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatRow("Allocatable Clusters", "${stats.allocatableClusters}")
                            StatRow("Used Clusters", "${stats.allocatedClusters}")
                            StatRow("Free Clusters", "${stats.freeClusters}")
                            StatRow("Free Space", "${stats.freeSpaceKb} KB")
                            StatRow("Bad Blocks", "${stats.badBlocksCount}")
                            StatRow("Backup Blocks", "Block ${superBlock.backupBlock1} & ${superBlock.backupBlock2}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onOpenHex != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onOpenHex()
                        }
                    ) {
                        Text("Raw Hex")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
