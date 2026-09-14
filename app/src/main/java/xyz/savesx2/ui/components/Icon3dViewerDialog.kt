package xyz.savesx2.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.savesx2.core.Ps2Icon3dSession
import xyz.savesx2.core.Ps2IconDecoder
import kotlin.math.roundToInt

private const val DEFAULT_PITCH_DEG = 15.0f
private const val DEFAULT_YAW_DEG = -25.0f
private const val DEFAULT_ZOOM = 1.0f

/**
 * Ultra-efficient, zero-idle-cost 3D Icon Viewer Dialog.
 *
 * Performance guarantees:
 * 1. Event-driven strictly on-demand: Frames are rendered ONLY in direct response to touch
 *    drag/pinch events. Zero background loop / 0% CPU and 0% GPU when idle.
 * 2. Mesh pre-parsing: Geometry, UVs, normals, and RLE textures are parsed only once and cached.
 * 3. OpenGL ES 2.0 Pbuffer offscreen rendering on Dispatchers.Default avoids UI thread contention.
 */
@Composable
fun Icon3dViewerDialog(
    session: Ps2Icon3dSession,
    onDismiss: () -> Unit
) {
    var pitchDeg by remember { mutableFloatStateOf(DEFAULT_PITCH_DEG) }
    var yawDeg by remember { mutableFloatStateOf(DEFAULT_YAW_DEG) }
    var zoomScale by remember { mutableFloatStateOf(DEFAULT_ZOOM) }

    // Strictly on-demand offscreen rendering: only triggered when pitch, yaw, or zoom changes
    val iconBitmap by produceState<Bitmap?>(initialValue = null, pitchDeg, yawDeg, zoomScale) {
        value = withContext(Dispatchers.Default) {
            Ps2IconDecoder.render3dIconMesh(
                mesh = session.mesh,
                iconSys = session.iconSys,
                pitchDeg = pitchDeg,
                yawDeg = yawDeg,
                scaleMultiplier = zoomScale
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (session.subtitle.isNotBlank()) {
                                Text(
                                    text = session.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive 3D Canvas
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E2638),
                                    Color(0xFF0F131D)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                yawDeg = (yawDeg + pan.x * 0.75f) % 360f
                                pitchDeg = (pitchDeg - pan.y * 0.75f).coerceIn(-85f, 85f)
                                zoomScale = (zoomScale * zoom).coerceIn(0.5f, 2.5f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = iconBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "3D Save Icon",
                            filterQuality = FilterQuality.Medium,
                            modifier = Modifier.fillMaxSize(0.92f)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp
                        )
                    }

                    // Reset button overlay
                    IconButton(
                        onClick = {
                            pitchDeg = DEFAULT_PITCH_DEG
                            yawDeg = DEFAULT_YAW_DEG
                            zoomScale = DEFAULT_ZOOM
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Angle",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Drag to rotate • Pinch to zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats and Controls Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Angle readout
                        Column {
                            Text(
                                text = "Pitch: ${pitchDeg.roundToInt()}°  |  Yaw: ${yawDeg.roundToInt()}°",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${session.mesh.vertexCount / 3} Polys  •  ${(zoomScale * 100).roundToInt()}% Zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Zoom buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { zoomScale = (zoomScale - 0.15f).coerceIn(0.5f, 2.5f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = { zoomScale = (zoomScale + 0.15f).coerceIn(0.5f, 2.5f) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
